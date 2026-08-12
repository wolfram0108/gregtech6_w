/**
 * Copyright (c) 2021 GregTech-6 Team
 *
 * This file is part of GregTech.
 *
 * GregTech is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * GregTech is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with GregTech. If not, see <http://www.gnu.org/licenses/>.
 *
 * Modified in 2026 for the GregTech 6 NeoForge port
 * (https://github.com/wolfram0108/gregtech6_w): ported from Minecraft 1.7.10 / Forge
 * to Minecraft 26.1.2 / NeoForge.
 */

package gregapi.network;

import java.util.ArrayList;
import java.util.Locale;
import java.util.UUID;
import java.util.function.Supplier;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;

import gregapi.util.UT;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

/**
 * F7 центральный переходник — СЕТЬ (ветка бэкпорта 1.20.1).
 *
 * <p><b>Что заменено.</b> Ветка 26.x стояла на payload-системе NeoForge ({@code CustomPacketPayload} +
 * {@code PayloadRegistrar} на {@code RegisterPayloadHandlersEvent} + {@code StreamCodec} над
 * {@code RegistryFriendlyByteBuf}). В 1.20.1 такой системы нет вовсе; канал строится
 * {@code NetworkRegistry.newSimpleChannel(name, версия, clientAccepted, serverAccepted)}
 * ({@code forge-1201-decompiled/net/minecraftforge/network/NetworkRegistry.java:102}), сообщение
 * регистрируется {@code SimpleChannel.registerMessage(index, class, encoder, decoder, consumer)}
 * ({@code simple/SimpleChannel.java:71}), приём идёт через {@code NetworkEvent.Context}
 * ({@code NetworkEvent.java:151-232}), отправка — через {@code PacketDistributor}
 * ({@code PacketDistributor.java:39-87}).
 *
 * <p><b>Центр остался один и вернулся к форме Грегориуса.</b> На канал регистрируется РОВНО ОДИН тип
 * сообщения — байт-конверт {@link GT6Payload}; идентичность пакета внутри несёт первый байт
 * ({@link IPacket#getPacketID()}) и таблица {@code mPacketTypes[256]}. Это дословно схема 1.7.10:
 * там транспорт тоже видел один {@code FMLProxyPacket} с сырым {@code byte[]}, а разбор делал сам мод
 * (оригинал {@code NetworkHandler.encode/decode:80-93}). Схема «именованный {@code Type<>} на каждый
 * пакет», которую пришлось завести в 26.x, здесь не нужна — числовые ID снова работают, и вместе с ними
 * вернулась родная проверка версии протокола (сообщение «Your Version … does not match»), усиленная
 * протокольной строкой самого канала.
 *
 * <p><b>Момент создания канала — конструктор</b>, как в оригинале ({@code NetworkRegistry.INSTANCE.newChannel}
 * там стоит в том же конструкторе). Это допустимо: реестр каналов Forge запирается только в фазе
 * {@code COMPLETE} ({@code ForgeStatesProvider.java:27} — {@code NETLOCK}), а GT6 строит хендлеры на
 * {@code FMLConstructModEvent}; живой образец той же версии — AE2 ({@code AppEngBase.java:204}).
 * Отдельной подписки на событие регистрации (как было в 26.x) больше не существует.
 *
 * @author Gregorius Techneticies
 */
public final class NetworkHandler implements INetworkHandler {
	/** Протокольная строка канала: стороны обязаны совпасть, иначе Forge не пустит подключение
	 *  ({@code NetworkRegistry.java:102} — предикаты clientAccepted/serverAccepted). Прямой наследник
	 *  1.7.10-проверки версии, которую GT6 делал сам в {@link #decode}. */
	private static final String NETWORK_VERSION = "1";

	private final IPacket[] mPacketTypes;
	private final String mModID;
	private final String mChannelName;
	private final SimpleChannel mChannel;

	/**
	 * Just instantiate your Network Handler once with this simple Constructor and everything else should be done.
	 *
	 * For usage keep that instance in a Variable somewhere so you can send Packets.
	 *
	 * For an example look into the Main File (GT_API), where I initialise the API Network Handler.
	 *
	 * @param aModID the ID of your Mod.
	 * @param aChannelName Name of your Channel (use 4 Characters or less, we do not want to Lag out the Connection), the GT Channel is called "GREG" and the API Channel is called "GAPI".
	 * @param aPacketTypes An Array of your Packet Types (an empty instance of every Packet you want to use for decoding). Remember that "getPacketID" must return a for your Handler individual Number. All 256 Byte Values are possible. Yes I mean the negative ones.
	 */
	public NetworkHandler(String aModID, String aChannelName, IPacket... aPacketTypes) {
		mModID = aModID;
		mChannelName = aChannelName;
		if (aChannelName.length() > 4) throw new IllegalArgumentException("String for Channel Name must contain 4 Characters or less!");
		mPacketTypes = new IPacket[256];
		for (int i = 0; i < aPacketTypes.length; i++) {
			int tID = UT.Code.unsignB(aPacketTypes[i].getPacketID());
			if (mPacketTypes[tID] == null) mPacketTypes[tID] = aPacketTypes[i]; else throw new IllegalArgumentException("Duplicate Packet ID! " + tID);
		}
		// Канал заводится ПРЯМО В КОНСТРУКТОРЕ — как в 1.7.10 (см. javadoc класса: NETLOCK стоит в фазе COMPLETE).
		mChannel = NetworkRegistry.newSimpleChannel(new ResourceLocation(identifierPart(aModID), "network/" + identifierPart(aChannelName)), () -> NETWORK_VERSION, NETWORK_VERSION::equals, NETWORK_VERSION::equals);
		// Один тип сообщения на канал, двусторонний (направление не задаём) — форма FMLEmbeddedChannel оригинала.
		mChannel.registerMessage(0, GT6Payload.class, GT6Payload::write, GT6Payload::read, this::handlePayload);
	}

	// [GT6-SYNCDIAG] BUG-094 (снять при уборке фазы): клиент — счётчики приёма GT6-пакетов
	private static final java.util.concurrent.atomic.AtomicLong sDiagReceived = new java.util.concurrent.atomic.AtomicLong(), sDiagQueued = new java.util.concurrent.atomic.AtomicLong(), sDiagProcessed = new java.util.concurrent.atomic.AtomicLong();

	private void handlePayload(GT6Payload aPayload, Supplier<NetworkEvent.Context> aContextSupplier) {
		NetworkEvent.Context aContext = aContextSupplier.get();
		// 1.20.1 требует явной отметки: неотмеченный пакет Forge считает необработанным и пишет в лог
		// (NetworkEvent.Context.setPacketHandled, NetworkEvent.java:196). В 26.x отметки не было — там её вёл движок.
		aContext.setPacketHandled(true);
		IPacket tPacket = decode(aPayload.data());
		if (tPacket == null) {
			if (gregapi.data.CS.probeFlag("gt6syncdiag.flag")) gregapi.data.CS.OUT.println("[GT6-SYNCDIAG-NET] decode=null (канал " + mChannelName + ")");
			return;
		}
		if (gregapi.data.CS.probeFlag("gt6syncdiag.flag")) {
			long tN = sDiagReceived.incrementAndGet();
			if (tN <= 10 || tN % 200 == 0) gregapi.data.CS.OUT.println("[GT6-SYNCDIAG-NET] принят #" + tN + " " + tPacket.getClass().getSimpleName() + " (канал " + mChannelName + ")");
		}
		aContext.enqueueWork(() -> {
			BlockGetter tWorld = getProcessingWorld(aContext);
			// НАДЁЖНЫЙ МОСТ (репорт игрока: worldgen-MTE невидимы в стартовой области при входе): даже на
			// ChunkWatchEvent.Sent координатный GT6-пакет может обгонять чанк при логин-очереди (chunk-sender
			// троттлит бандл, payload-канал — нет) → блока ещё нет → пакет молча терялся → клиент-BE не создавался.
			// Вместо гонки — буфер: пакет в незагруженный чанк откладывается и доигрывается по тикам (processPending).
			if (tWorld instanceof Level tLevel && tLevel.isClientSide() && tPacket instanceof gregapi.network.packets.PacketCoordinates tPC
			 && !tLevel.hasChunkAt(new BlockPos(tPC.mX, tPC.mY, tPC.mZ))) {
				queuePending(tPC, this);
				if (gregapi.data.CS.probeFlag("gt6syncdiag.flag")) {
					long tQ = sDiagQueued.incrementAndGet();
					if (tQ <= 10 || tQ % 200 == 0) gregapi.data.CS.OUT.println("[GT6-SYNCDIAG-NET] отложен (чанка нет) #" + tQ + " @" + tPC.mX + "," + tPC.mY + "," + tPC.mZ);
				}
				return;
			}
			tPacket.process(tWorld, this);
			if (gregapi.data.CS.probeFlag("gt6syncdiag.flag")) {
				long tP = sDiagProcessed.incrementAndGet();
				if (tP <= 10 || tP % 200 == 0) gregapi.data.CS.OUT.println("[GT6-SYNCDIAG-NET] обработан #" + tP + " " + tPacket.getClass().getSimpleName() + " world=" + (tWorld == null ? "null" : tWorld.getClass().getSimpleName()));
			}
		});
	}

	// ---- Клиентский буфер отложенных координатных пакетов (пакет обогнал чанк) ----
	private static final class PendingPacket {
		final gregapi.network.packets.PacketCoordinates mPacket; final NetworkHandler mHandler; int mTTL = 600; // ~30с
		PendingPacket(gregapi.network.packets.PacketCoordinates aPacket, NetworkHandler aHandler) {mPacket = aPacket; mHandler = aHandler;}
	}
	private static final java.util.ArrayDeque<PendingPacket> PENDING = new java.util.ArrayDeque<>();
	private static void queuePending(gregapi.network.packets.PacketCoordinates aPacket, NetworkHandler aHandler) {
		synchronized (PENDING) {if (PENDING.size() < 8192) PENDING.add(new PendingPacket(aPacket, aHandler));}
	}
	/** Доигрывание отложенных пакетов (зовёт клиент-тик GT_API_Proxy_Client); aWorld — текущий клиент-Level. */
	public static void processPending(Level aWorld) {
		if (aWorld == null) {synchronized (PENDING) {PENDING.clear();} return;}
		java.util.List<PendingPacket> tReady = null;
		synchronized (PENDING) {
			for (java.util.Iterator<PendingPacket> it = PENDING.iterator(); it.hasNext();) {
				PendingPacket tP = it.next();
				if (aWorld.hasChunkAt(new BlockPos(tP.mPacket.mX, tP.mPacket.mY, tP.mPacket.mZ))) {
					if (tReady == null) tReady = new ArrayList<>();
					tReady.add(tP); it.remove();
				} else if (--tP.mTTL <= 0) it.remove();
			}
		}
		if (tReady != null) for (PendingPacket tP : tReady) try {tP.mPacket.process(aWorld, tP.mHandler);} catch (Throwable e) {e.printStackTrace(gregapi.data.CS.ERR);}
	}

	/** 1:1 с оригиналом: серверный приёмник отдавал {@code null} ({@code HandlerServer.channelRead0}),
	 *  клиентский — мир игрока ({@code Minecraft.getMinecraft().thePlayer.worldObj}). Игрок берётся ЧЕРЕЗ ЦЕНТР
	 *  side-разделения мода ({@code GT_API_Proxy.getThePlayer}: сервер отдаёт null, клиентский прокси —
	 *  {@code Minecraft.getInstance().player}), а не прямым обращением к клиентскому классу из общего кода —
	 *  тот же запрет, на котором ловили BUG-084. */
	private BlockGetter getProcessingWorld(NetworkEvent.Context aContext) {
		if (aContext.getDirection().getReceptionSide() != LogicalSide.CLIENT) return null;
		Player tPlayer = gregapi.GT_API.api_proxy.getThePlayer();
		return tPlayer == null ? null : tPlayer.level();
	}

	private GT6Payload payload(IPacket aPacket) {
		ByteArrayDataOutput rOut = ByteStreams.newDataOutput();
		rOut.writeByte(aPacket.getPacketID());
		rOut.write(aPacket.encode().toByteArray());
		return new GT6Payload(rOut.toByteArray());
	}

	private IPacket decode(byte[] aData) {
		if (aData == null || aData.length <= 0) return null;
		ByteArrayDataInput tData = ByteStreams.newDataInput(aData);
		int tID = UT.Code.unsignB(tData.readByte());
		if (mPacketTypes[tID] == null) {
			gregapi.data.CS.ERR.println("Your Version of '" + mModID + "' definetly does not match the Version installed on the Server you joined! Do not report this as a Bug! You failed to install/update the proper Version of '" + mModID + "' all by yourself!");
			return null;
		}
		return mPacketTypes[tID].decode(tData);
	}

	@Override
	public void sendToServer(IPacket aPacket) {
		if (aPacket == null) return;
		mChannel.sendToServer(payload(aPacket));
	}

	@Override
	public void sendToPlayer(IPacket aPacket, ServerPlayer aPlayer) {
		if (aPacket == null || aPlayer == null) return;
		mChannel.send(PacketDistributor.PLAYER.with(() -> aPlayer), payload(aPacket));
	}

	@Override
	public void sendToAllAround(IPacket aPacket, TargetPoint aPosition) {
		if (aPacket == null || aPosition == null || aPosition.mLevel == null) return;
		PacketDistributor.TargetPoint tTarget = new PacketDistributor.TargetPoint(aPosition.mExcluded, aPosition.mX, aPosition.mY, aPosition.mZ, aPosition.mRange, aPosition.mLevel.dimension());
		mChannel.send(PacketDistributor.NEAR.with(() -> tTarget), payload(aPacket));
	}

	@Override public void sendToAllPlayersInRange(IPacket aPacket, Level aWorld, BlockPos aCoords) {sendToAllPlayersInRange(aPacket, aWorld, aCoords.getX(), aCoords.getZ());}
	@Override public void sendToAllPlayersInRange(IPacket aPacket, Level aWorld, int aX, int aZ) {
		if (aPacket == null) return;
		ServerLevel tWorld = serverWorld(aWorld);
		if (tWorld == null) return;
		// TRACKING_CHUNK рассылает ровно тем, кто ЧАНК ВИДИТ ({@code PacketDistributor.java:238-243} —
		// chunkMap.getPlayers(chunk.getPos(), false)); это и есть проверка isPlayerWatchingChunk оригинала.
		ChunkPos tChunk = chunk(aX, aZ);
		mChannel.send(PacketDistributor.TRACKING_CHUNK.with(() -> tWorld.getChunk(tChunk.x, tChunk.z)), payload(aPacket));
	}

	@Override public void sendToPlayerIfInRange(IPacket aPacket, UUID aPlayer, Level aWorld, BlockPos aCoords) {sendToPlayerIfInRange(aPacket, aPlayer, aWorld, aCoords.getX(), aCoords.getZ());}
	@Override public void sendToPlayerIfInRange(IPacket aPacket, UUID aPlayer, Level aWorld, int aX, int aZ) {
		if (aPacket == null || aPlayer == null) return;
		ServerLevel tWorld = serverWorld(aWorld);
		if (tWorld == null) return;
		ChunkPos tChunk = chunk(aX, aZ);
		for (ServerPlayer tPlayer : tWorld.getChunkSource().chunkMap.getPlayers(tChunk, false)) if (aPlayer.equals(tPlayer.getUUID())) {
			mChannel.send(PacketDistributor.PLAYER.with(() -> tPlayer), payload(aPacket));
			return;
		}
	}

	@Override public void sendToAllPlayersInRangeExcept(IPacket aPacket, UUID aPlayer, Level aWorld, BlockPos aCoords) {sendToAllPlayersInRangeExcept(aPacket, aPlayer, aWorld, aCoords.getX(), aCoords.getZ());}
	@Override public void sendToAllPlayersInRangeExcept(IPacket aPacket, UUID aPlayer, Level aWorld, int aX, int aZ) {
		if (aPacket == null) return;
		ServerLevel tWorld = serverWorld(aWorld);
		if (tWorld == null) return;
		ChunkPos tChunk = chunk(aX, aZ);
		GT6Payload tPayload = payload(aPacket);
		for (ServerPlayer tPlayer : tWorld.getChunkSource().chunkMap.getPlayers(tChunk, false)) if (aPlayer == null || !aPlayer.equals(tPlayer.getUUID())) mChannel.send(PacketDistributor.PLAYER.with(() -> tPlayer), tPayload);
	}

	/** 1.7.10 отдавал ОТДЕЛЬНЫЙ {@code FMLEmbeddedChannel} на сторону; в 1.20.1 канал один на обе стороны,
	 *  поэтому аргумент остаётся ради совместимости сигнатуры и на выбор не влияет. */
	@Override
	public SimpleChannel getChannel(Dist aSide) {
		return mChannel;
	}

	public String getChannelName() {
		return mChannelName;
	}

	private static ServerLevel serverWorld(Level aWorld) {
		return aWorld instanceof ServerLevel ? (ServerLevel)aWorld : null;
	}

	private static ChunkPos chunk(int aX, int aZ) {
		return new ChunkPos(new BlockPos(aX, 0, aZ)); // 1.20.1: конструктор от BlockPos (ChunkPos.java:32) — форма 1.7.10 getChunkFromBlockCoords
	}

	private static String identifierPart(String aName) {
		String tName = aName == null ? "gt6" : aName.toLowerCase(Locale.ROOT);
		StringBuilder rName = new StringBuilder(tName.length());
		for (int i = 0; i < tName.length(); i++) {
			char tChar = tName.charAt(i);
			rName.append((tChar >= 'a' && tChar <= 'z') || (tChar >= '0' && tChar <= '9') || tChar == '_' || tChar == '-' || tChar == '.' ? tChar : '_');
		}
		return rName.length() <= 0 ? "gt6" : rName.toString();
	}

	/** Байт-конверт GT6: ровно то, чем в 1.7.10 был {@code FMLProxyPacket} — сырой {@code byte[]}, первый байт
	 *  которого есть {@link IPacket#getPacketID()}. Своей структуры не несёт, разбор делает сам мод ({@link #decode}). */
	public record GT6Payload(byte[] data) {
		public static GT6Payload read(FriendlyByteBuf aBuffer) {
			return new GT6Payload(aBuffer.readByteArray());
		}

		public void write(FriendlyByteBuf aBuffer) {
			aBuffer.writeByteArray(data);
		}
	}

	public static final class TargetPoint {
		public final ServerLevel mLevel;
		public final ServerPlayer mExcluded;
		public final double mX, mY, mZ, mRange;

		public TargetPoint(ServerLevel aLevel, double aX, double aY, double aZ, double aRange) {
			this(aLevel, null, aX, aY, aZ, aRange);
		}

		public TargetPoint(ServerLevel aLevel, ServerPlayer aExcluded, double aX, double aY, double aZ, double aRange) {
			mLevel = aLevel;
			mExcluded = aExcluded;
			mX = aX;
			mY = aY;
			mZ = aZ;
			mRange = aRange;
		}
	}
}

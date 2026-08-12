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
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;

import gregapi.util.UT;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.minecraftforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * @author Gregorius Techneticies
 */
public final class NetworkHandler implements INetworkHandler {
	private static final String NETWORK_VERSION = "1";
	private static final List<NetworkHandler> HANDLERS = new ArrayList<>();

	private final IPacket[] mPacketTypes;
	private final String mModID;
	private final String mChannelName;
	private final CustomPacketPayload.Type<GT6Payload> mPayloadType;
	private final StreamCodec<RegistryFriendlyByteBuf, GT6Payload> mPayloadCodec;

	/**
	 * Just instantiate your Network Handler once with this simple Constructor and everything else should be done.
	 *
	 * For usage keep that instance in a Variable somewhere so you can send Packets.
	 *
	 * For an example look into the Main File (GT_API), where I initialise the API Network Handler.
	 *
	 * @param aModID the ID of your Mod.
	 * @param aChannelName Name of your Channel (use 4 Characters or less, we don't want to Lag out the Connection), the GT Channel is called "GREG" and the API Channel is called "GAPI".
	 * @param aPacketTypes An Array of your Packet Types (an empty instance of every Packet you want to use for decoding). Remember that "getPacketID" must return a for your Handler individual Number. All 256 Byte Values are possible. Yes I mean the negative ones.
	 */
	public NetworkHandler(String aModID, String aChannelName, IPacket... aPacketTypes) {
		mModID = aModID;
		mChannelName = aChannelName;
		if (aChannelName.length() > 4) throw new IllegalArgumentException("String for Channel Name must contain 4 Characters or less!");
		mPayloadType = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(identifierPart(aModID), "network/" + identifierPart(aChannelName)));
		mPayloadCodec = GT6Payload.codec(mPayloadType);
		mPacketTypes = new IPacket[256];
		for (int i = 0; i < aPacketTypes.length; i++) {
			int tID = UT.Code.unsignB(aPacketTypes[i].getPacketID());
			if (mPacketTypes[tID] == null) mPacketTypes[tID] = aPacketTypes[i]; else throw new IllegalArgumentException("Duplicate Packet ID! " + tID);
		}
		synchronized(HANDLERS) {
			HANDLERS.add(this);
		}
	}

	public static void registerPayloadHandlers(RegisterPayloadHandlersEvent aEvent) {
		List<NetworkHandler> tHandlers;
		synchronized(HANDLERS) {
			tHandlers = new ArrayList<>(HANDLERS);
		}
		PayloadRegistrar tRegistrar = aEvent.registrar(NETWORK_VERSION);
		for (NetworkHandler tHandler : tHandlers) tHandler.registerPayload(tRegistrar);
		// F7-lifecycle (boot-подтверждено: NetworkHandler создаются вовремя)
	}

	private void registerPayload(PayloadRegistrar aRegistrar) {
		aRegistrar.playBidirectional(mPayloadType, mPayloadCodec, this::handlePayload, this::handlePayload);
	}

	// [GT6-SYNCDIAG] BUG-094 (снять при уборке фазы): клиент — счётчики приёма GT6-пакетов
	private static final java.util.concurrent.atomic.AtomicLong sDiagReceived = new java.util.concurrent.atomic.AtomicLong(), sDiagQueued = new java.util.concurrent.atomic.AtomicLong(), sDiagProcessed = new java.util.concurrent.atomic.AtomicLong();

	private void handlePayload(GT6Payload aPayload, IPayloadContext aContext) {
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

	private BlockGetter getProcessingWorld(IPayloadContext aContext) {
		if (aContext.flow() != PacketFlow.CLIENTBOUND) return null;
		Player tPlayer = aContext.player();
		return tPlayer == null ? null : tPlayer.level();
	}

	private GT6Payload payload(IPacket aPacket) {
		ByteArrayDataOutput rOut = ByteStreams.newDataOutput();
		rOut.writeByte(aPacket.getPacketID());
		rOut.write(aPacket.encode().toByteArray());
		return new GT6Payload(mPayloadType, rOut.toByteArray());
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
		ClientPacketDistributor.sendToServer(payload(aPacket));
	}

	@Override
	public void sendToPlayer(IPacket aPacket, ServerPlayer aPlayer) {
		if (aPacket == null || aPlayer == null) return;
		PacketDistributor.sendToPlayer(aPlayer, payload(aPacket));
	}

	@Override
	public void sendToAllAround(IPacket aPacket, TargetPoint aPosition) {
		if (aPacket == null || aPosition == null || aPosition.mLevel == null) return;
		PacketDistributor.sendToPlayersNear(aPosition.mLevel, aPosition.mExcluded, aPosition.mX, aPosition.mY, aPosition.mZ, aPosition.mRange, payload(aPacket));
	}

	@Override public void sendToAllPlayersInRange(IPacket aPacket, Level aWorld, BlockPos aCoords) {sendToAllPlayersInRange(aPacket, aWorld, aCoords.getX(), aCoords.getZ());}
	@Override public void sendToAllPlayersInRange(IPacket aPacket, Level aWorld, int aX, int aZ) {
		if (aPacket == null) return;
		ServerLevel tWorld = serverWorld(aWorld);
		if (tWorld == null) return;
		PacketDistributor.sendToPlayersTrackingChunk(tWorld, chunk(aX, aZ), payload(aPacket));
	}

	@Override public void sendToPlayerIfInRange(IPacket aPacket, UUID aPlayer, Level aWorld, BlockPos aCoords) {sendToPlayerIfInRange(aPacket, aPlayer, aWorld, aCoords.getX(), aCoords.getZ());}
	@Override public void sendToPlayerIfInRange(IPacket aPacket, UUID aPlayer, Level aWorld, int aX, int aZ) {
		if (aPacket == null || aPlayer == null) return;
		ServerLevel tWorld = serverWorld(aWorld);
		if (tWorld == null) return;
		ChunkPos tChunk = chunk(aX, aZ);
		for (ServerPlayer tPlayer : tWorld.getChunkSource().chunkMap.getPlayers(tChunk, false)) if (aPlayer.equals(tPlayer.getUUID())) {
			PacketDistributor.sendToPlayer(tPlayer, payload(aPacket));
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
		for (ServerPlayer tPlayer : tWorld.getChunkSource().chunkMap.getPlayers(tChunk, false)) if (aPlayer == null || !aPlayer.equals(tPlayer.getUUID())) PacketDistributor.sendToPlayer(tPlayer, tPayload);
	}

	@Override
	public CustomPacketPayload.Type<GT6Payload> getChannel(Dist aSide) {
		return mPayloadType;
	}

	public String getChannelName() {
		return mChannelName;
	}

	private static ServerLevel serverWorld(Level aWorld) {
		return aWorld instanceof ServerLevel ? (ServerLevel)aWorld : null;
	}

	private static ChunkPos chunk(int aX, int aZ) {
		return ChunkPos.containing(new BlockPos(aX, 0, aZ));
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

	public record GT6Payload(CustomPacketPayload.Type<GT6Payload> type, byte[] data) implements CustomPacketPayload {
		public static StreamCodec<RegistryFriendlyByteBuf, GT6Payload> codec(CustomPacketPayload.Type<GT6Payload> aType) {
			return StreamCodec.ofMember(GT6Payload::write, aBuffer -> read(aType, aBuffer));
		}

		private static GT6Payload read(CustomPacketPayload.Type<GT6Payload> aType, RegistryFriendlyByteBuf aBuffer) {
			return new GT6Payload(aType, aBuffer.readByteArray());
		}

		public void write(RegistryFriendlyByteBuf aBuffer) {
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

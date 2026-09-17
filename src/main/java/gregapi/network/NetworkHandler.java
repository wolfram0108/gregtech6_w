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

/** 1.20.1 has no payload-system at all; the channel is built via NetworkRegistry.newSimpleChannel with registerMessage,
 *  one message type per channel (a byte envelope whose first byte is the packet id) -- 1.7.10's own FMLProxyPacket scheme.
 *  @author Gregorius Techneticies */
public final class NetworkHandler implements INetworkHandler {
	/** Fallback only for a build whose own metadata cannot be read; a fixed string would let any build in. */
	private static final String NETWORK_VERSION_UNKNOWN = "unknown";

	/** Channel version follows the MOD version: both sides must match or Forge refuses the connection
	 *  (NetworkRegistry.java:102, predicates clientAccepted/serverAccepted), so a client of another
	 *  build cannot join and silently desync on a changed packet format. */
	public static String networkVersion() {
		String rVersion = net.minecraftforge.fml.ModList.get() == null ? null
			: net.minecraftforge.fml.ModList.get().getModContainerById(gregapi.data.MD.GT.mID)
				.map(tContainer -> tContainer.getModInfo().getVersion().toString()).orElse(null);
		if (rVersion == null || rVersion.isBlank()) {
			gregapi.data.CS.ERR.println("GT_API: mod version unreadable, network channel falls back to '" + NETWORK_VERSION_UNKNOWN + "'.");
			return NETWORK_VERSION_UNKNOWN;
		}
		return rVersion;
	}

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
		// The channel is created directly in the constructor, as in 1.7.10; the registry only locks in a later phase.
		final String tVersion = networkVersion();
		mChannel = NetworkRegistry.newSimpleChannel(new ResourceLocation(identifierPart(aModID), "network/" + identifierPart(aChannelName)), () -> tVersion, tVersion::equals, tVersion::equals);
		// One message type per channel, bidirectional, matching the original FMLEmbeddedChannel's own form.
		mChannel.registerMessage(0, GT6Payload.class, GT6Payload::write, GT6Payload::read, this::handlePayload);
	}

	private void handlePayload(GT6Payload aPayload, Supplier<NetworkEvent.Context> aContextSupplier) {
		NetworkEvent.Context aContext = aContextSupplier.get();
		// 1.20.1 requires marking a packet handled explicitly, or Forge logs it unhandled; newer versions tracked that themselves.
		aContext.setPacketHandled(true);
		IPacket tPacket = decode(aPayload.data());
		if (tPacket == null) return;
		aContext.enqueueWork(() -> {
			BlockGetter tWorld = getProcessingWorld(aContext);
			// A coordinate packet can outrace its chunk during login, since the chunk sender throttles but this payload
			// channel doesn't; the block isn't there yet, so the packet is buffered and replayed on tick instead.
			if (tWorld instanceof Level tLevel && tLevel.isClientSide() && tPacket instanceof gregapi.network.packets.PacketCoordinates tPC
			 && !tLevel.hasChunkAt(new BlockPos(tPC.mX, tPC.mY, tPC.mZ))) {
				queuePending(tPC, this);
				return;
			}
			tPacket.process(tWorld, this);
		});
	}

	private static final class PendingPacket {
		final gregapi.network.packets.PacketCoordinates mPacket; final NetworkHandler mHandler; int mTTL = 600; // Roughly 30 seconds before this pending packet is given up on.
		PendingPacket(gregapi.network.packets.PacketCoordinates aPacket, NetworkHandler aHandler) {mPacket = aPacket; mHandler = aHandler;}
	}
	private static final java.util.ArrayDeque<PendingPacket> PENDING = new java.util.ArrayDeque<>();
	private static void queuePending(gregapi.network.packets.PacketCoordinates aPacket, NetworkHandler aHandler) {
		synchronized (PENDING) {if (PENDING.size() < 8192) PENDING.add(new PendingPacket(aPacket, aHandler));}
	}
	/** Replays deferred packets; called from the client tick, aWorld is the current client Level. */
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

	/** 1:1 with the original: the server side returns null, the client side returns the player's world, fetched through
	 *  the mod's own side-split center rather than referencing a client-only class directly from shared code. */
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
		// Broadcasts to whoever is watching the chunk, asked by position rather than chunk object, because TRACKING_CHUNK's API
		// demands an actual chunk object -- fetching one mid-worldgen can deadlock the server against its own generation.
		ChunkPos tChunk = chunk(aX, aZ);
		GT6Payload tPayload = payload(aPacket);
		for (ServerPlayer tPlayer : tWorld.getChunkSource().chunkMap.getPlayers(tChunk, false)) mChannel.send(PacketDistributor.PLAYER.with(() -> tPlayer), tPayload);
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

	/** 1.7.10 gave each side its own channel; on 1.20.1 there's just one for both, so this argument no longer changes anything. */
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
		return new ChunkPos(new BlockPos(aX, 0, aZ)); // This BlockPos constructor is the 1.20.1 form of 1.7.10's getChunkFromBlockCoords.
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

	/** Exactly what 1.7.10's FMLProxyPacket was: a raw byte[] whose first byte is the packet id, structure-free by design. */
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

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
 */

package gregapi.network;

import java.util.EnumMap;
import java.util.List;
import java.util.UUID;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;

import cpw.mods.fml.common.FMLCommonHandler;
import net.neoforged.fml.Logging;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.network.PacketDistributor;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.api.distmarker.Dist;
import gregapi.util.UT;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.MessageToMessageCodec;
import net.minecraft.client.Minecraft;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;

/**
 * @author Gregorius Techneticies
 */
@Sharable
public final class NetworkHandler extends MessageToMessageCodec<CustomPacketPayload, IPacket> implements INetworkHandler {
	private final EnumMap<Dist, PayloadRegistrar> mChannel;
	private final IPacket[] mPacketTypes;
	private final String mModID;
	
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
		if (aChannelName.length() > 4) throw new IllegalArgumentException("String for Channel Name must contain 4 Characters or less!");
		mChannel = PayloadRegistrar.INSTANCE.newChannel(aChannelName, this, FMLCommonHandler.instance().getSide()==Dist.CLIENT?new HandlerClient(this):new HandlerServer(this));
		mPacketTypes = new IPacket[256];
		for (int i = 0; i < aPacketTypes.length; i++) {
			int tID = UT.Code.unsignB(aPacketTypes[i].getPacketID());
			if (mPacketTypes[tID] == null) mPacketTypes[tID] = aPacketTypes[i]; else throw new IllegalArgumentException("Duplicate Packet ID! " + tID);
		}
	}
	
	@Override
	protected void encode(ChannelHandlerContext aContext, IPacket aPacket, List<Object> aOutput) throws Exception {
		aOutput.add(new CustomPacketPayload(Unpooled.buffer().writeByte(aPacket.getPacketID()).writeBytes(aPacket.encode().toByteArray()), aContext.channel().attr(PayloadRegistrar.FML_CHANNEL).get()));
	}
	
	@Override
	protected void decode(ChannelHandlerContext aContext, CustomPacketPayload aPacket, List<Object> aOutput) throws Exception {
		ByteArrayDataInput aData = ByteStreams.newDataInput(aPacket.payload().array());
		int aID = UT.Code.unsignB(aData.readByte());
		if (mPacketTypes[aID] == null) {
			Logging.warning("Your Version of '" + mModID + "' definetly does not match the Version installed on the Server you joined! Do not report this as a Bug! You failed to install/update the proper Version of '" + mModID + "' all by yourself!");
		} else {
			aOutput.add(mPacketTypes[aID].decode(aData));
		}
	}
	
	@Override
	public void sendToServer(IPacket aPacket) {
		if (aPacket == null) return;
		PayloadRegistrar tChannel = getChannel(Dist.CLIENT);
		tChannel.attr(PacketDistributor.FML_MESSAGETARGET).set(PacketDistributor.OutboundTarget.TOSERVER);
		tChannel.writeAndFlush(aPacket);
	}
	
	@Override
	public void sendToPlayer(IPacket aPacket, ServerPlayer aPlayer) {
		if (aPacket == null) return;
		PayloadRegistrar tChannel = getChannel(Dist.SERVER);
		tChannel.attr(PacketDistributor.FML_MESSAGETARGET).set(PacketDistributor.OutboundTarget.PLAYER);
		tChannel.attr(PacketDistributor.FML_MESSAGETARGETARGS).set(aPlayer);
		tChannel.writeAndFlush(aPacket);
	}
	
	@Override
	public void sendToAllAround(IPacket aPacket, PacketDistributor aPosition) {
		if (aPacket == null) return;
		PayloadRegistrar tChannel = getChannel(Dist.SERVER);
		tChannel.attr(PacketDistributor.FML_MESSAGETARGET).set(PacketDistributor.OutboundTarget.ALLAROUNDPOINT);
		tChannel.attr(PacketDistributor.FML_MESSAGETARGETARGS).set(aPosition);
		tChannel.writeAndFlush(aPacket);
	}
	
	@Override public void sendToAllPlayersInRange(IPacket aPacket, Level aWorld, BlockPos aCoords) {sendToAllPlayersInRange(aPacket, aWorld, aCoords.posX, aCoords.posZ);}
	@Override public void sendToAllPlayersInRange(IPacket aPacket, Level aWorld, int aX, int aZ) {
		if (aPacket == null) return;
		if (aWorld != null && !aWorld.isRemote) for (Object tObject : aWorld.playerEntities) {
			if (tObject instanceof ServerPlayer) {
				ServerPlayer tPlayer = (ServerPlayer)tObject;
				LevelChunk tChunk = aWorld.getChunkFromBlockCoords(aX, aZ);
				if (tPlayer.getServerForPlayer().getPlayerManager().isPlayerWatchingChunk(tPlayer, tChunk.xPosition, tChunk.zPosition)) sendToPlayer(aPacket, tPlayer);
			} else return;
		}
	}
	
	@Override public void sendToPlayerIfInRange(IPacket aPacket, UUID aPlayer, Level aWorld, BlockPos aCoords) {sendToPlayerIfInRange(aPacket, aPlayer, aWorld, aCoords.posX, aCoords.posZ);}
	@Override public void sendToPlayerIfInRange(IPacket aPacket, UUID aPlayer, Level aWorld, int aX, int aZ) {
		if (aPacket == null) return;
		if (aWorld != null && !aWorld.isRemote) for (Object tObject : aWorld.playerEntities) {
			if (tObject instanceof ServerPlayer) {
				ServerPlayer tPlayer = (ServerPlayer)tObject;
				if (tPlayer.getUniqueID().equals(aPlayer)) {
					LevelChunk tChunk = aWorld.getChunkFromBlockCoords(aX, aZ);
					if (tPlayer.getServerForPlayer().getPlayerManager().isPlayerWatchingChunk(tPlayer, tChunk.xPosition, tChunk.zPosition)) {
						sendToPlayer(aPacket, tPlayer);
					}
					return;
				}
			} else return;
		}
	}
	
	@Override public void sendToAllPlayersInRangeExcept(IPacket aPacket, UUID aPlayer, Level aWorld, BlockPos aCoords) {sendToAllPlayersInRangeExcept(aPacket, aPlayer, aWorld, aCoords.posX, aCoords.posZ);}
	@Override public void sendToAllPlayersInRangeExcept(IPacket aPacket, UUID aPlayer, Level aWorld, int aX, int aZ) {
		if (aPacket == null) return;
		if (aWorld != null && !aWorld.isRemote) for (Object tObject : aWorld.playerEntities) {
			if (tObject instanceof ServerPlayer) {
				ServerPlayer tPlayer = (ServerPlayer)tObject;
				if (!tPlayer.getUniqueID().equals(aPlayer)) {
					LevelChunk tChunk = aWorld.getChunkFromBlockCoords(aX, aZ);
					if (tPlayer.getServerForPlayer().getPlayerManager().isPlayerWatchingChunk(tPlayer, tChunk.xPosition, tChunk.zPosition)) {
						sendToPlayer(aPacket, tPlayer);
					}
				}
			} else return;
		}
	}
	
	@Override
	public PayloadRegistrar getChannel(Dist aSide) {
		return mChannel.get(aSide);
	}
	
	@Sharable
	static final class HandlerClient extends SimpleChannelInboundHandler<IPacket> {
		public final INetworkHandler mNetworkHandler;
		
		public HandlerClient(INetworkHandler aNetworkHandler) {
			mNetworkHandler = aNetworkHandler;
		}
		
		@Override
		protected void channelRead0(ChannelHandlerContext ctx, IPacket aPacket) throws Exception {
			aPacket.process(Minecraft.getMinecraft().thePlayer == null ? null : Minecraft.getMinecraft().thePlayer.worldObj, mNetworkHandler);
//          DEB.println(aPacket.getClass().getName());
//          if (aPacket instanceof PacketCoordinates) DEB.println(" X: " + ((PacketCoordinates)aPacket).mX + " - Y: " + ((PacketCoordinates)aPacket).mY + " - Z: " + ((PacketCoordinates)aPacket).mZ);
		}
	}
	
	@Sharable
	static final class HandlerServer extends SimpleChannelInboundHandler<IPacket> {
		public final INetworkHandler mNetworkHandler;
		
		public HandlerServer(INetworkHandler aNetworkHandler) {
			mNetworkHandler = aNetworkHandler;
		}
		
		@Override
		protected void channelRead0(ChannelHandlerContext ctx, IPacket aPacket) throws Exception {
			aPacket.process(null, mNetworkHandler);
		}
	}
}

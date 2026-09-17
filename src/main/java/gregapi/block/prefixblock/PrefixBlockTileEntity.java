/**
 * Copyright (c) 2025 GregTech-6 Team
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

package gregapi.block.prefixblock;

import gregapi.network.INetworkHandler;
import gregapi.network.packets.data.PacketSyncDataName;
import gregapi.network.packets.data.PacketSyncDataShort;
import gregapi.render.IRenderedBlockObject;
import gregapi.render.IRenderedBlockObjectSideCheck;
import gregapi.render.ITexture;
import gregapi.tileentity.ITileEntityScheduledUpdate;
import gregapi.tileentity.ITileEntitySpecificPlacementBehavior;
import gregapi.tileentity.ITileEntitySynchronising;
import gregapi.tileentity.base.TileEntityBase01Root;
import gregapi.util.WD;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.BlockGetter;

import static gregapi.data.CS.*;

/**
 * @author Gregorius Techneticies
 */
public class PrefixBlockTileEntity extends TileEntityBase01Root implements IRenderedBlockObject, IRenderedBlockObjectSideCheck, ITileEntityScheduledUpdate, ITileEntitySynchronising, ITileEntitySpecificPlacementBehavior {
	public short mMetaData = W;
	public boolean mBlocked = T;
	public CompoundTag mItemNBT = null;
	
	public PrefixBlockTileEntity() {super(F);}
	public PrefixBlockTileEntity(net.minecraft.core.BlockPos aPos) {super(F, aPos);}
	public PrefixBlockTileEntity(net.minecraft.core.BlockPos aPos, net.minecraft.world.level.block.state.BlockState aState) {super(F, aPos, aState);}
	
	@Override public String getTileEntityName() {return "gt.MetaBlockTileEntity";}
	
	// getUpdateTag defaults to empty, so the client never received the ore's material on chunk load and rendered it as blank;
	// this sends it through saveCustomOnly instead, applied client-side through the normal update-tag chain.
	@Override public net.minecraft.nbt.CompoundTag getUpdateTag() {return saveWithoutMetadata();}

	// GT6's old optimization of skipping sync for occluded ore is incompatible with neo, which needs the tile data on
	// chunk load regardless, or the ore renders grey; this always returns the standard neo update packet instead now.
	@Override public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getUpdatePacket() {
		mBlocked = WD.visOcc(level, getBlockPos().getX(), getBlockPos().getY(), getBlockPos().getZ(), F, T);
		return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
	}
	
	@Override
	public void onScheduledUpdate() {
		if (!(mBlocked = WD.visOcc(level, getBlockPos().getX(), getBlockPos().getY(), getBlockPos().getZ(), F, T))) {
			NW_API.sendToAllPlayersInRange(new PacketSyncDataShort(getCoords(), mMetaData), level, getCoords());
			if (mItemNBT != null && mItemNBT.contains("display")) NW_API.sendToAllPlayersInRange(new PacketSyncDataName(getCoords(), mItemNBT.getCompound("display").getString("Name")), level, getCoords());
		}
	}
	
	@Override
	public void onAdjacentBlockChange(int aX, int aY, int aZ) {
		if (!level.isClientSide() && mBlocked) {
			mBlocked = F;
			NW_API.sendToAllPlayersInRange(new PacketSyncDataShort(getCoords(), mMetaData), level, getCoords());
			if (mItemNBT != null && mItemNBT.contains("display")) NW_API.sendToAllPlayersInRange(new PacketSyncDataName(getCoords(), mItemNBT.getCompound("display").getString("Name")), level, getCoords());
		}
	}
	
	@Override
	public void sendUpdateToPlayer(ServerPlayer aPlayer) {
		if (!(mBlocked = WD.visOcc(level, getBlockPos().getX(), getBlockPos().getY(), getBlockPos().getZ(), T, T))) {
			NW_API.sendToPlayer(new PacketSyncDataShort(getCoords(), mMetaData), aPlayer);
			if (mItemNBT != null && mItemNBT.contains("display")) NW_API.sendToPlayer(new PacketSyncDataName(getCoords(), mItemNBT.getCompound("display").getString("Name")), aPlayer);
		}
	}
	
	private ITexture mTexture;

	/** Clears the cached texture whenever the synced material arrives, or the mesh would stay stuck on "no material". */
	public void receiveMetaData(short aMetaData) {mMetaData = aMetaData; mTexture = null;}

	/** A worldgen stub tile entity arrives with a placeholder material, re-read from the chunk-wide map here at setLevel, since
	 *  that stub can occasionally survive until the next load instead of being replaced immediately. */
	@Override public void setLevel(net.minecraft.world.level.Level aLevel) {
		super.setLevel(aLevel);
		if (mMetaData == W && aLevel != null && getBlockState().getBlock() instanceof PrefixBlock)
			mMetaData = PrefixBlock.getOreMeta(aLevel, getBlockPos().getX(), getBlockPos().getY(), getBlockPos().getZ());
	}

	@Override
	public ITexture getTexture(Block aBlock, int aRenderPass, byte aSide, boolean[] aShouldSideBeRendered) {
		if (!aShouldSideBeRendered[aSide]) return null;
		if (mTexture == null) mTexture = (aBlock instanceof PrefixBlock ? ((PrefixBlock)aBlock).getTexture(mMetaData, level != null) : null);
		return mTexture;
	}
	
	// RenderBlocks itself was removed, so the parameter is a neutral Object, the same holder used elsewhere.
	@Override public boolean renderItem(Block aBlock, Object aRenderer) {return F;}
	@Override public boolean renderBlock(Block aBlock, Object aRenderer, BlockGetter aWorld, int aX, int aY, int aZ) {return F;}
	@Override public boolean setBlockBounds(Block aBlock, int aRenderPass, boolean[] aShouldSideBeRendered) {return F;}
	@Override public int getRenderPasses(Block aBlock, boolean[] aShouldSideBeRendered) {return 1;}
	@Override public void readFromNBT(CompoundTag aNBT) {super.readFromNBT(aNBT); mMetaData = aNBT.getShort("m"); if (aNBT.contains("gt.nbt.drop")) mItemNBT = aNBT.getCompound("gt.nbt.drop"); mTexture = null;/* rebuild the texture after the material loads or syncs */}
	@Override public void writeToNBT(CompoundTag aNBT) {super.writeToNBT(aNBT); aNBT.putShort("m", mMetaData); if (mItemNBT != null && !mItemNBT.isEmpty()) aNBT.put("gt.nbt.drop", mItemNBT);}
	@Override public void processPacket(INetworkHandler aNetworkHandler) {/**/}
	@Override public Object getGUIClient(int aGUIID, Player aPlayer) {return null;}
	@Override public Object getGUIServer(int aGUIID, Player aPlayer) {return null;}
}

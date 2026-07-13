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
import net.minecraft.client.renderer.RenderBlocks;
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
	
	@Override public String getTileEntityName() {return "gt.MetaBlockTileEntity";}
	
	// @Override
	public net.minecraft.network.Packet getDescriptionPacket() {
		if (!(mBlocked = WD.visOcc(level, getBlockPos().getX(), getBlockPos().getY(), getBlockPos().getZ(), F, T))) {
			NW_API.sendToAllPlayersInRange(new PacketSyncDataShort(getCoords(), mMetaData), level, getCoords());
			if (mItemNBT != null && mItemNBT.contains("display")) NW_API.sendToAllPlayersInRange(new PacketSyncDataName(getCoords(), mItemNBT.getCompoundOrEmpty("display").getString("Name")), level, getCoords());
		}
		return null;
	}
	
	@Override
	public void onScheduledUpdate() {
		if (!(mBlocked = WD.visOcc(level, getBlockPos().getX(), getBlockPos().getY(), getBlockPos().getZ(), F, T))) {
			NW_API.sendToAllPlayersInRange(new PacketSyncDataShort(getCoords(), mMetaData), level, getCoords());
			if (mItemNBT != null && mItemNBT.contains("display")) NW_API.sendToAllPlayersInRange(new PacketSyncDataName(getCoords(), mItemNBT.getCompoundOrEmpty("display").getString("Name")), level, getCoords());
		}
	}
	
	@Override
	public void onAdjacentBlockChange(int aX, int aY, int aZ) {
		if (!level.isClientSide() && mBlocked) {
			mBlocked = F;
			NW_API.sendToAllPlayersInRange(new PacketSyncDataShort(getCoords(), mMetaData), level, getCoords());
			if (mItemNBT != null && mItemNBT.contains("display")) NW_API.sendToAllPlayersInRange(new PacketSyncDataName(getCoords(), mItemNBT.getCompoundOrEmpty("display").getString("Name")), level, getCoords());
		}
	}
	
	@Override
	public void sendUpdateToPlayer(ServerPlayer aPlayer) {
		if (!(mBlocked = WD.visOcc(level, getBlockPos().getX(), getBlockPos().getY(), getBlockPos().getZ(), T, T))) {
			NW_API.sendToPlayer(new PacketSyncDataShort(getCoords(), mMetaData), aPlayer);
			if (mItemNBT != null && mItemNBT.contains("display")) NW_API.sendToPlayer(new PacketSyncDataName(getCoords(), mItemNBT.getCompoundOrEmpty("display").getString("Name")), aPlayer);
		}
	}
	
	private ITexture mTexture;
	
	@Override
	public ITexture getTexture(Block aBlock, int aRenderPass, byte aSide, boolean[] aShouldSideBeRendered) {
		if (!aShouldSideBeRendered[aSide]) return null;
		if (mTexture == null) mTexture = (aBlock instanceof PrefixBlock ? ((PrefixBlock)aBlock).getTexture(mMetaData, level != null) : null);
		return mTexture;
	}
	
	@Override public boolean renderItem(Block aBlock, RenderBlocks aRenderer) {return F;}
	@Override public boolean renderBlock(Block aBlock, RenderBlocks aRenderer, BlockGetter aWorld, int aX, int aY, int aZ) {return F;}
	@Override public boolean setBlockBounds(Block aBlock, int aRenderPass, boolean[] aShouldSideBeRendered) {return F;}
	@Override public int getRenderPasses(Block aBlock, boolean[] aShouldSideBeRendered) {return 1;}
	@Override public void readFromNBT(CompoundTag aNBT) {super.readFromNBT(aNBT); mMetaData = aNBT.getShort("m"); if (aNBT.contains("gt.nbt.drop")) mItemNBT = aNBT.getCompoundOrEmpty("gt.nbt.drop");}
	@Override public void writeToNBT(CompoundTag aNBT) {super.writeToNBT(aNBT); aNBT.putShort("m", mMetaData); if (mItemNBT != null && !mItemNBT.isEmpty()) aNBT.put("gt.nbt.drop", mItemNBT);}
	@Override public void processPacket(INetworkHandler aNetworkHandler) {/**/}
	@Override public Object getGUIClient(int aGUIID, Player aPlayer) {return null;}
	@Override public Object getGUIServer(int aGUIID, Player aPlayer) {return null;}
}

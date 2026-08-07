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

package gregtech.tileentity.inventories;

import static gregapi.data.CS.*;

import java.util.List;

import gregapi.block.multitileentity.IMultiTileEntity.IMTE_HasMultiBlockMachineRelevantData;
import gregapi.code.HashSetNoNulls;
import gregapi.data.LH;
import gregapi.data.LH.Chat;
import gregapi.data.TD;
import gregapi.old.Textures;
import gregapi.render.BlockTextureDefault;
import gregapi.render.BlockTextureMulti;
import gregapi.render.IIconContainer;
import gregapi.render.ITexture;
import gregapi.tileentity.ITileEntityMachineBlockUpdateable;
import gregapi.tileentity.base.TileEntityBase09FacingSingle;
import gregapi.tileentity.delegate.DelegatorTileEntity;
import gregapi.tileentity.delegate.ITileEntityCanDelegate;
import gregapi.tileentity.machines.ITileEntitySwitchableOnOff;
import gregapi.util.UT;
import gregapi.util.WD;
import gregtech.blocks.tool.BlockLongDistPipe;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.Container;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.core.BlockPos;

public class MultiTileEntityLongDistancePipelineItem extends TileEntityBase09FacingSingle implements IMTE_HasMultiBlockMachineRelevantData, ITileEntityCanDelegate, ITileEntityMachineBlockUpdateable, ITileEntitySwitchableOnOff {
	protected boolean mStopped = F;
	protected MultiTileEntityLongDistancePipelineItem mTarget = null, mSender = null;
	protected BlockPos mTargetPos = null;
	
	@Override
	public void readFromNBT2(CompoundTag aNBT) {
		super.readFromNBT2(aNBT);
		if (aNBT.contains(NBT_STOPPED)) mStopped = aNBT.getBooleanOr(NBT_STOPPED, false);
		if (aNBT.contains(NBT_TARGET)) {mTargetPos = new BlockPos(UT.Code.bindInt(aNBT.getLongOr(NBT_TARGET_X, 0L)), UT.Code.bindInt(aNBT.getLongOr(NBT_TARGET_Y, 0L)), UT.Code.bindInt(aNBT.getLongOr(NBT_TARGET_Z, 0L)));}
	}
	
	@Override
	public void writeToNBT2(CompoundTag aNBT) {
		super.writeToNBT2(aNBT);
		if (mTargetPos != null && mTarget != this) {
		UT.NBT.setBoolean(aNBT, NBT_TARGET, T);
		UT.NBT.setNumber(aNBT, NBT_TARGET_X, mTargetPos.getX());
		UT.NBT.setNumber(aNBT, NBT_TARGET_Y, mTargetPos.getY());
		UT.NBT.setNumber(aNBT, NBT_TARGET_Z, mTargetPos.getZ());
		}
		UT.NBT.setBoolean(aNBT, NBT_STOPPED, mStopped);
	}
	
	@Override
	public void addToolTips(List<String> aList, ItemStack aStack, boolean aF3_H) {
		super.addToolTips(aList, aStack, aF3_H);
		aList.add(Chat.DGRAY + LH.get(LH.TOOL_TO_RESET_SOFT_HAMMER));
		aList.add(Chat.DGRAY + LH.get(LH.TOOL_TO_DETAIL_MAGNIFYINGGLASS));
	}
	
	@Override
	public long onToolClick2(String aTool, long aRemainingDurability, long aQuality, Entity aPlayer, List<String> aChatReturn, Container aPlayerInventory, boolean aSneaking, ItemStack aStack, byte aSide, float aHitX, float aHitY, float aHitZ) {
		long rReturn = super.onToolClick2(aTool, aRemainingDurability, aQuality, aPlayer, aChatReturn, aPlayerInventory, aSneaking, aStack, aSide, aHitX, aHitY, aHitZ);
		if (rReturn > 0) return rReturn;
		
		if (isClientSide()) return 0;
		
		if (aTool.equals(TOOL_softhammer)) {
			scanPipes();
			return 10000;
		}
		if (aTool.equals(TOOL_magnifyingglass)) {
			if (aChatReturn != null) {
				if (mSender != null && !mSender.isDead() && mSender.mTarget == this) {
					aChatReturn.add("Is the Target");
					aChatReturn.add("Sender is at: X: " + mSender.getBlockPos().getX() + " Y: " + mSender.getBlockPos().getY() + " Z: " + mSender.getBlockPos().getZ());
				} else {
					aChatReturn.add(checkTarget() ? "Has Target" : "Has no loaded Target");
					if (mTargetPos != null) aChatReturn.add("Target should be around: X: " + mTargetPos.getX() + " Y: " + mTargetPos.getY() + " Z: " + mTargetPos.getZ());
				}
			}
			return 1;
		}
		return 0;
	}
	
	public boolean checkTarget() {
		if (mStopped || isClientSide()) return F;
		if (mTargetPos == null) {
			scanPipes();
		} else if (mTarget == null || mTarget.isDead()) {
			mTarget = null;
			if (WD.exists(level, mTargetPos.getX(), mTargetPos.getY(), mTargetPos.getZ())) {
				BlockEntity tTileEntity = WD.te(level, mTargetPos, T);
				if (tTileEntity instanceof MultiTileEntityLongDistancePipelineItem) {
					mTarget = (MultiTileEntityLongDistancePipelineItem)tTileEntity;
				} else {
					if (tTileEntity != null) mTargetPos = null;
				}
			}
		}
		if (mTarget == null || mTarget == this) return F;
		if (mTarget.mSender == null || mTarget.mSender.isDead() || mTarget.mSender.mTarget == null || mTarget.mSender.mTarget.isDead()) mTarget.mSender = this;
		return mTarget.mSender == this;
	}
	
	private void scanPipes() {
		if (mSender != null && !mSender.isDead() && mSender.mTarget == this) return;
		mIgnoreUnloadedChunks = F;
		mTargetPos = getCoords();
		mTarget = this;
		mSender = null;
		
		Block aBlock = getBlockAtSide(OPOS[mFacing]);
		byte aMetaData = getMetaDataAtSide(OPOS[mFacing]);
		if (aBlock instanceof BlockLongDistPipe) {
			
			if (((BlockLongDistPipe)aBlock).mTemperatures[aMetaData] >= 0) return;
			HashSetNoNulls<BlockPos>
			tNewChecks  = new HashSetNoNulls<>(),
			tOldChecks  = new HashSetNoNulls<>(F, getCoords()),
			tToCheck    = new HashSetNoNulls<>(F, getOffsetN(mFacing, 1)),
			tWires      = new HashSetNoNulls<>();
			
			while (!tToCheck.isEmpty()) {
				for (BlockPos aCoords : tToCheck) {
					if (getBlock(aCoords) == aBlock && getMetaData(aCoords) == aMetaData) {
						tWires.add(aCoords);
						BlockPos tCoords;
						if (tOldChecks.add(tCoords = new BlockPos(aCoords.getX() + 1, aCoords.getY(), aCoords.getZ()))) tNewChecks.add(tCoords);
						if (tOldChecks.add(tCoords = new BlockPos(aCoords.getX() - 1, aCoords.getY(), aCoords.getZ()))) tNewChecks.add(tCoords);
						if (tOldChecks.add(tCoords = new BlockPos(aCoords.getX(), aCoords.getY() + 1, aCoords.getZ()))) tNewChecks.add(tCoords);
						if (tOldChecks.add(tCoords = new BlockPos(aCoords.getX(), aCoords.getY() - 1, aCoords.getZ()))) tNewChecks.add(tCoords);
						if (tOldChecks.add(tCoords = new BlockPos(aCoords.getX(), aCoords.getY(), aCoords.getZ() + 1))) tNewChecks.add(tCoords);
						if (tOldChecks.add(tCoords = new BlockPos(aCoords.getX(), aCoords.getY(), aCoords.getZ() - 1))) tNewChecks.add(tCoords);
					} else {
						BlockEntity tTileEntity = getTileEntity(aCoords);
						if (tTileEntity != this && tTileEntity instanceof MultiTileEntityLongDistancePipelineItem) {
							if (tWires.contains(((MultiTileEntityLongDistancePipelineItem)tTileEntity).getOffset(((MultiTileEntityLongDistancePipelineItem)tTileEntity).mFacing, 1))) {
								mTarget = (MultiTileEntityLongDistancePipelineItem)tTileEntity;
								mTargetPos = mTarget.getCoords();
								mIgnoreUnloadedChunks = T;
								return;
							}
							tOldChecks.remove(aCoords);
						}
					}
				}
				tToCheck.clear();
				tToCheck.addAll(tNewChecks);
				tNewChecks.clear();
			}
		}
		mIgnoreUnloadedChunks = T;
	}
	
	@Override public boolean setStateOnOff(boolean aOnOff) {mStopped = !aOnOff; return !mStopped;}
	@Override public boolean getStateOnOff() {return !mStopped;}
	
	@Override public void onCoordinateChange() {super.onCoordinateChange(); mTargetPos = null; mSender = null;}
	@Override public void onMachineBlockUpdate(BlockPos aCoords, Block aBlock, byte aMeta, boolean aRemoved) {if (aBlock instanceof BlockLongDistPipe) {mTargetPos = null; mSender = null;}}
	@Override public boolean hasMultiBlockMachineRelevantData() {return T;}
	
	@Override public boolean canDrop(int aInventorySlot) {return F;}
	@Override public boolean isExtender(byte aSide) {return F;}
	
	@Override
	public ItemStack removeItem(int aSlot, int aDecrement) {
		if (checkTarget()) {
			DelegatorTileEntity<Container> tTileEntity = mTarget.getAdjacentInventory(OPOS[mTarget.mFacing]);
			if (tTileEntity.mTileEntity != null) return tTileEntity.mTileEntity.removeItem(aSlot, aDecrement);
		}
		return null;
	}
	@Override
	public ItemStack removeItemNoUpdate(int aSlot) {
		if (checkTarget()) {
			DelegatorTileEntity<Container> tTileEntity = mTarget.getAdjacentInventory(OPOS[mTarget.mFacing]);
			if (tTileEntity.mTileEntity != null) return tTileEntity.mTileEntity.removeItemNoUpdate(aSlot);
		}
		return null;
	}
	@Override
	public ItemStack getItem(int aSlot) {
		if (checkTarget()) {
			DelegatorTileEntity<Container> tTileEntity = mTarget.getAdjacentInventory(OPOS[mTarget.mFacing]);
			if (tTileEntity.mTileEntity != null) return tTileEntity.mTileEntity.getItem(aSlot);
		}
		return null;
	}
	@Override
	public String getInventoryName() {
		if (checkTarget()) {
			DelegatorTileEntity<Container> tTileEntity = mTarget.getAdjacentInventory(OPOS[mTarget.mFacing]);
		}
		return super.getInventoryName();
	}
	@Override
	public int getContainerSize() {
		if (checkTarget()) {
			DelegatorTileEntity<Container> tTileEntity = mTarget.getAdjacentInventory(OPOS[mTarget.mFacing]);
			if (tTileEntity.mTileEntity != null) return tTileEntity.mTileEntity.getContainerSize();
		}
		return 0;
	}
	@Override
	public int getMaxStackSize() {
		if (checkTarget()) {
			DelegatorTileEntity<Container> tTileEntity = mTarget.getAdjacentInventory(OPOS[mTarget.mFacing]);
			if (tTileEntity.mTileEntity != null) return tTileEntity.mTileEntity.getMaxStackSize();
		}
		return 0;
	}
	@Override
	public void setItem(int aSlot, ItemStack aStack) {
		if (checkTarget()) {
			DelegatorTileEntity<Container> tTileEntity = mTarget.getAdjacentInventory(OPOS[mTarget.mFacing]);
			if (tTileEntity.mTileEntity != null) tTileEntity.mTileEntity.setItem(aSlot, aStack);
		}
	}
	@Override
	public boolean hasCustomInventoryName() {
		if (checkTarget()) {
			DelegatorTileEntity<Container> tTileEntity = mTarget.getAdjacentInventory(OPOS[mTarget.mFacing]);
		}
		return getCustomName() != null;
	}
	@Override
	public boolean canPlaceItem(int aSlot, ItemStack aStack) {
		if (checkTarget()) {
			DelegatorTileEntity<Container> tTileEntity = mTarget.getAdjacentInventory(OPOS[mTarget.mFacing]);
			if (tTileEntity.mTileEntity != null) return tTileEntity.mTileEntity.canPlaceItem(aSlot, aStack);
		}
		return F;
	}
	
	// Relay Sided Inventories
	
	@Override
	public int[] getAccessibleSlotsFromSide2(byte aSide) {
		if (checkTarget()) {
			DelegatorTileEntity<Container> tTileEntity = mTarget.getAdjacentInventory(OPOS[mTarget.mFacing]);
			if (tTileEntity.mTileEntity instanceof WorldlyContainer) return ((WorldlyContainer)tTileEntity.mTileEntity).getSlotsForFace(FORGE_DIR[tTileEntity.mSideOfTileEntity]);
			if (tTileEntity.mTileEntity != null) return UT.Code.getAscendingArray(tTileEntity.mTileEntity.getContainerSize());
		}
		return ZL_INTEGER;
	}
	@Override
	public boolean canInsertItem2(int aSlot, ItemStack aStack, byte aSide) {
		if (checkTarget()) {
			DelegatorTileEntity<Container> tTileEntity = mTarget.getAdjacentInventory(OPOS[mTarget.mFacing]);
			if (tTileEntity.mTileEntity instanceof WorldlyContainer) return ((WorldlyContainer)tTileEntity.mTileEntity).canPlaceItemThroughFace(aSlot, aStack, FORGE_DIR[tTileEntity.mSideOfTileEntity]);
			if (tTileEntity.mTileEntity != null) return T;
		}
		return F;
	}
	@Override
	public boolean canExtractItem2(int aSlot, ItemStack aStack, byte aSide) {
		return F;
	}
	
	@Override
	public ITexture getTexture2(Block aBlock, int aRenderPass, byte aSide, boolean[] aShouldSideBeRendered) {
		if (!aShouldSideBeRendered[aSide]) return null;
		int aIndex = aSide==mFacing?0:aSide==OPOS[mFacing]?1:2;
		return BlockTextureMulti.get(BlockTextureDefault.get(sColoreds[aIndex], mRGBa, mMaterial.contains(TD.Properties.GLOWING)), BlockTextureDefault.get(sOverlays[aIndex]));
	}
	
	// Icons
	public static IIconContainer sColoreds[] = new IIconContainer[] {
		new Textures.BlockIcons.CustomIcon("machines/pipelines/item/colored/front"),
		new Textures.BlockIcons.CustomIcon("machines/pipelines/item/colored/back"),
		new Textures.BlockIcons.CustomIcon("machines/pipelines/item/colored/side"),
	}, sOverlays[] = new IIconContainer[] {
		new Textures.BlockIcons.CustomIcon("machines/pipelines/item/overlay/front"),
		new Textures.BlockIcons.CustomIcon("machines/pipelines/item/overlay/back"),
		new Textures.BlockIcons.CustomIcon("machines/pipelines/item/overlay/side"),
	};
	
	@Override public String getTileEntityName() {return "gt.multitileentity.pipelines.item";}
}

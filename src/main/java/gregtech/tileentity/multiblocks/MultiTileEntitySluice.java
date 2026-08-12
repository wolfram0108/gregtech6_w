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

package gregtech.tileentity.multiblocks;

import gregapi.data.LH;
import gregapi.data.LH.Chat;
import gregapi.tileentity.delegate.DelegatorTileEntity;
import gregapi.tileentity.energy.ITileEntityEnergy;
import gregapi.tileentity.machines.ITileEntityAdjacentOnOff;
import gregapi.tileentity.multiblocks.ITileEntityMultiBlockController;
import gregapi.tileentity.multiblocks.MultiTileEntityMultiBlockPart;
import gregapi.tileentity.multiblocks.TileEntityBase10MultiBlockMachine;
import gregapi.util.WD;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.fluids.capability.IFluidHandler;

import java.util.List;

import static gregapi.data.CS.*;

/**
 * @author Gregorius Techneticies
 */
public class MultiTileEntitySluice extends TileEntityBase10MultiBlockMachine {
	@Override
	public boolean checkStructure2(BlockPos aCoordinates, Entity aPlayer, Container aInventory) {
		int
		tMinX = getBlockPos().getX()-(SIDE_X_NEG==mFacing?0:SIDE_X_POS==mFacing?6:1),
		tMinZ = getBlockPos().getZ()-(SIDE_Z_NEG==mFacing?0:SIDE_Z_POS==mFacing?6:1),
		tMaxX = getBlockPos().getX()+(SIDE_X_POS==mFacing?0:SIDE_X_NEG==mFacing?6:1),
		tMaxZ = getBlockPos().getZ()+(SIDE_Z_POS==mFacing?0:SIDE_Z_NEG==mFacing?6:1),
		tD = (mActive?mFacing+2:mFacing-2);
		
		if (WD.exists(level, tMinX, getBlockPos().getY(), tMinZ) && WD.exists(level, tMaxX, getBlockPos().getY()+2, tMaxZ)) {
			boolean tSuccess = T;
			for (int tX = tMinX; tX <= tMaxX; tX++) for (int tZ = tMinZ; tZ <= tMaxZ; tZ++) {
				if (SIDES_AXIS_X[mFacing] ? tX == getBlockPos().getX() : tZ == getBlockPos().getZ()) {
				if (!ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX, getBlockPos().getY()  , tZ, 18006, getMultiTileEntityRegistryID(), 1, MultiTileEntityMultiBlockPart.ONLY_ITEM_FLUID_OUT, aCoordinates, aPlayer, aInventory)) tSuccess = F;
				} else {
				if (!ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX, getBlockPos().getY()  , tZ, 18006, getMultiTileEntityRegistryID(), 0, MultiTileEntityMultiBlockPart.NOTHING, aCoordinates, aPlayer, aInventory)) tSuccess = F;
				}
				
				if (SIDES_AXIS_X[mFacing] ? Math.abs(tX-getBlockPos().getX())==5 && tZ != getBlockPos().getZ() : Math.abs(tZ-getBlockPos().getZ())==5 && tX != getBlockPos().getX()) {
				if (!ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX, getBlockPos().getY()+1, tZ, 18006, getMultiTileEntityRegistryID(), 3, MultiTileEntityMultiBlockPart.ONLY_ENERGY_IN, aCoordinates, aPlayer, aInventory)) tSuccess = F;
				} else {
				if (!ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX, getBlockPos().getY()+1, tZ, 18006, getMultiTileEntityRegistryID(), 0, MultiTileEntityMultiBlockPart.NOTHING, aCoordinates, aPlayer, aInventory)) tSuccess = F;
				}
				
				if (SIDES_AXIS_X[mFacing] ? Math.abs(tX-getBlockPos().getX())==6 : Math.abs(tZ-getBlockPos().getZ())==6) {
				if (!ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX, getBlockPos().getY()+2, tZ, 18106, getMultiTileEntityRegistryID(),tD, MultiTileEntityMultiBlockPart.ONLY_ITEM_FLUID_IN, aCoordinates, aPlayer, aInventory)) tSuccess = F;
				} else {
				if (!ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX, getBlockPos().getY()+2, tZ, 18106, getMultiTileEntityRegistryID(),tD, MultiTileEntityMultiBlockPart.NOTHING, aCoordinates, aPlayer, aInventory)) tSuccess = F;
				}
			}
			return tSuccess;
		}
		return mStructureOkay;
	}
	
	static {
		LH.add("gt.tooltip.multiblock.sluice.1", "Two 3x7 Layers of Titanium Walls");
		LH.add("gt.tooltip.multiblock.sluice.2", "3x7 Layer of Sluice Parts ontop of that");
		LH.add("gt.tooltip.multiblock.sluice.3", "Main Block centered on Slim-Side-Bottom and facing outwards");
		LH.add("gt.tooltip.multiblock.sluice.4", "Input only at the Top of the Far Side");
		LH.add("gt.tooltip.multiblock.sluice.5", "Output only at the Bottom of the Close Side");
	}
	
	@Override
	public void addToolTips(List<String> aList, ItemStack aStack, boolean aF3_H) {
		aList.add(Chat.CYAN     + LH.get(LH.STRUCTURE) + ":");
		aList.add(Chat.WHITE    + LH.get("gt.tooltip.multiblock.sluice.1"));
		aList.add(Chat.WHITE    + LH.get("gt.tooltip.multiblock.sluice.2"));
		aList.add(Chat.WHITE    + LH.get("gt.tooltip.multiblock.sluice.3"));
		aList.add(Chat.WHITE    + LH.get("gt.tooltip.multiblock.sluice.4"));
		aList.add(Chat.WHITE    + LH.get("gt.tooltip.multiblock.sluice.5"));
		super.addToolTips(aList, aStack, aF3_H);
	}
	
	@Override
	public boolean isInsideStructure(int aX, int aY, int aZ) {
		return
		aX >= getBlockPos().getX()-(SIDE_X_NEG==mFacing?0:SIDE_X_POS==mFacing?6:1) &&
		aY >= getBlockPos().getY()   &&
		aZ >= getBlockPos().getZ()-(SIDE_Z_NEG==mFacing?0:SIDE_Z_POS==mFacing?6:1) &&
		aX <= getBlockPos().getX()+(SIDE_X_POS==mFacing?0:SIDE_X_NEG==mFacing?6:1) &&
		aY <= getBlockPos().getY()+2 &&
		aZ <= getBlockPos().getZ()+(SIDE_Z_POS==mFacing?0:SIDE_Z_NEG==mFacing?6:1);
	}
	
	@Override
	public void updateAdjacentToggleableEnergySources() {
		DelegatorTileEntity<BlockEntity> tDelegator;
		if (SIDES_AXIS_X[mFacing]) {
			tDelegator = WD.te(level, getOffsetXN(mFacing, 5), getBlockPos().getY()+1, getBlockPos().getZ() - 2, SIDE_Z_POS, F);
			if (tDelegator.mTileEntity instanceof ITileEntityAdjacentOnOff && tDelegator.mTileEntity instanceof ITileEntityEnergy && ((ITileEntityEnergy)tDelegator.mTileEntity).isEnergyEmittingTo(mEnergyTypeAccepted, tDelegator.mSideOfTileEntity, T)) ((ITileEntityAdjacentOnOff)tDelegator.mTileEntity).setAdjacentOnOff(getStateOnOff());
			tDelegator = WD.te(level, getOffsetXN(mFacing, 5), getBlockPos().getY()+1, getBlockPos().getZ() + 2, SIDE_Z_NEG, F);
			if (tDelegator.mTileEntity instanceof ITileEntityAdjacentOnOff && tDelegator.mTileEntity instanceof ITileEntityEnergy && ((ITileEntityEnergy)tDelegator.mTileEntity).isEnergyEmittingTo(mEnergyTypeAccepted, tDelegator.mSideOfTileEntity, T)) ((ITileEntityAdjacentOnOff)tDelegator.mTileEntity).setAdjacentOnOff(getStateOnOff());
		} else {
			tDelegator = WD.te(level, getBlockPos().getX() - 2, getBlockPos().getY()+1, getOffsetZN(mFacing, 5), SIDE_X_POS, F);
			if (tDelegator.mTileEntity instanceof ITileEntityAdjacentOnOff && tDelegator.mTileEntity instanceof ITileEntityEnergy && ((ITileEntityEnergy)tDelegator.mTileEntity).isEnergyEmittingTo(mEnergyTypeAccepted, tDelegator.mSideOfTileEntity, T)) ((ITileEntityAdjacentOnOff)tDelegator.mTileEntity).setAdjacentOnOff(getStateOnOff());
			tDelegator = WD.te(level, getBlockPos().getX() + 2, getBlockPos().getY()+1, getOffsetZN(mFacing, 5), SIDE_X_NEG, F);
			if (tDelegator.mTileEntity instanceof ITileEntityAdjacentOnOff && tDelegator.mTileEntity instanceof ITileEntityEnergy && ((ITileEntityEnergy)tDelegator.mTileEntity).isEnergyEmittingTo(mEnergyTypeAccepted, tDelegator.mSideOfTileEntity, T)) ((ITileEntityAdjacentOnOff)tDelegator.mTileEntity).setAdjacentOnOff(getStateOnOff());
		}
	}
	
	@Override
	public DelegatorTileEntity<IFluidHandler> getFluidOutputTarget(byte aSide, Fluid aOutput) {
		return getAdjacentTank(SIDE_BOTTOM);
	}
	
	@Override
	public DelegatorTileEntity<BlockEntity> getItemOutputTarget(byte aSide) {
		return getAdjacentTileEntity(SIDE_BOTTOM);
	}
	
	@Override public DelegatorTileEntity<Container> getItemInputTarget(byte aSide) {return null;}
	@Override public DelegatorTileEntity<IFluidHandler> getFluidInputTarget(byte aSide) {return null;}
	
	@Override public boolean refreshStructureOnActiveStateChange() {return T;}
	
	@Override public String getTileEntityName() {return "gt.multitileentity.multiblock.sluice";}
}

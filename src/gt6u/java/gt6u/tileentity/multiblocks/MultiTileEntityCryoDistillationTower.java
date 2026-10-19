/**
 * Copyright (c) 2020 Gregtech6-Unofficial Team
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
 * Modified in 2026 for the GregTech 6 NeoForge port (https://github.com/wolfram0108/gregtech6_w):
 * ported from GregTech6-Unofficial for Minecraft 1.7.10 / Forge to an addon for Minecraft 26.1.2 / NeoForge.
 */

package gt6u.tileentity.multiblocks;

import gregapi.data.LH;
import gregapi.tileentity.multiblocks.ITileEntityMultiBlockController;
import gregapi.tileentity.multiblocks.MultiTileEntityMultiBlockPart;
import gregapi.util.WD;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;

import static gregapi.data.CS.*;

/** GT6U's Cryo Distillation Tower (GT6U MultiTileEntityCryoDistillationTower.java): built from Frost Proof Machine Casings. */
public class MultiTileEntityCryoDistillationTower extends gregtech.tileentity.multiblocks.MultiTileEntityCryoDistillationTower {
	@Override
public boolean checkStructure2(BlockPos aCoordinates, Entity aPlayer, Container aInventory) {
		int tX = getOffsetXN(mFacing), tY = getBlockPos().getY(), tZ = getOffsetZN(mFacing);
		if (WD.exists(level, tX-1, tY, tZ-1) && WD.exists(level, tX+1, tY, tZ-1) && WD.exists(level, tX-1, tY, tZ+1) && WD.exists(level, tX+1, tY, tZ+1)) {
			boolean tSuccess = T;
			
			if (!ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX-1, tY-1, tZ-1, 18101, getMultiTileEntityRegistryID(), 0, MultiTileEntityMultiBlockPart.ONLY_ENERGY_IN, aCoordinates, aPlayer, aInventory)) tSuccess = F;
			if (!ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX  , tY-1, tZ-1, 18101, getMultiTileEntityRegistryID(), 0, MultiTileEntityMultiBlockPart.ONLY_ENERGY_IN, aCoordinates, aPlayer, aInventory)) tSuccess = F;
			if (!ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX+1, tY-1, tZ-1, 18101, getMultiTileEntityRegistryID(), 0, MultiTileEntityMultiBlockPart.ONLY_ENERGY_IN, aCoordinates, aPlayer, aInventory)) tSuccess = F;
			if (!ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX-1, tY-1, tZ  , 18101, getMultiTileEntityRegistryID(), 0, MultiTileEntityMultiBlockPart.ONLY_ENERGY_IN, aCoordinates, aPlayer, aInventory)) tSuccess = F;
			if (!ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX  , tY-1, tZ  , 18101, getMultiTileEntityRegistryID(), 0, MultiTileEntityMultiBlockPart.ONLY_ENERGY_IN, aCoordinates, aPlayer, aInventory)) tSuccess = F;
			if (!ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX+1, tY-1, tZ  , 18101, getMultiTileEntityRegistryID(), 0, MultiTileEntityMultiBlockPart.ONLY_ENERGY_IN, aCoordinates, aPlayer, aInventory)) tSuccess = F;
			if (!ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX-1, tY-1, tZ+1, 18101, getMultiTileEntityRegistryID(), 0, MultiTileEntityMultiBlockPart.ONLY_ENERGY_IN, aCoordinates, aPlayer, aInventory)) tSuccess = F;
			if (!ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX  , tY-1, tZ+1, 18101, getMultiTileEntityRegistryID(), 0, MultiTileEntityMultiBlockPart.ONLY_ENERGY_IN, aCoordinates, aPlayer, aInventory)) tSuccess = F;
			if (!ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX+1, tY-1, tZ+1, 18101, getMultiTileEntityRegistryID(), 0, MultiTileEntityMultiBlockPart.ONLY_ENERGY_IN, aCoordinates, aPlayer, aInventory)) tSuccess = F;
			
			if (!ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX-1, tY  , tZ-1, 18112, getMultiTileEntityRegistryID(),                              0, MultiTileEntityMultiBlockPart.ONLY_ITEM_FLUID, aCoordinates, aPlayer, aInventory)) tSuccess = F;
			if (!ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX  , tY  , tZ-1, 18112, getMultiTileEntityRegistryID(),  mFacing == SIDE_Z_POS ? 1 : 0, MultiTileEntityMultiBlockPart.ONLY_ITEM_FLUID, aCoordinates, aPlayer, aInventory)) tSuccess = F;
			if (!ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX+1, tY  , tZ-1, 18112, getMultiTileEntityRegistryID(),                              0, MultiTileEntityMultiBlockPart.ONLY_ITEM_FLUID, aCoordinates, aPlayer, aInventory)) tSuccess = F;
			if (!ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX-1, tY  , tZ  , 18112, getMultiTileEntityRegistryID(),  mFacing == SIDE_X_POS ? 1 : 0, MultiTileEntityMultiBlockPart.ONLY_ITEM_FLUID, aCoordinates, aPlayer, aInventory)) tSuccess = F;
			if (!ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX  , tY  , tZ  , 18112, getMultiTileEntityRegistryID(),                              0, MultiTileEntityMultiBlockPart.ONLY_ITEM_FLUID, aCoordinates, aPlayer, aInventory)) tSuccess = F;
			if (!ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX+1, tY  , tZ  , 18112, getMultiTileEntityRegistryID(),  mFacing == SIDE_X_NEG ? 1 : 0, MultiTileEntityMultiBlockPart.ONLY_ITEM_FLUID, aCoordinates, aPlayer, aInventory)) tSuccess = F;
			if (!ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX-1, tY  , tZ+1, 18112, getMultiTileEntityRegistryID(),                              0, MultiTileEntityMultiBlockPart.ONLY_ITEM_FLUID, aCoordinates, aPlayer, aInventory)) tSuccess = F;
			if (!ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX  , tY  , tZ+1, 18112, getMultiTileEntityRegistryID(),  mFacing == SIDE_Z_NEG ? 1 : 0, MultiTileEntityMultiBlockPart.ONLY_ITEM_FLUID, aCoordinates, aPlayer, aInventory)) tSuccess = F;
			if (!ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX+1, tY  , tZ+1, 18112, getMultiTileEntityRegistryID(),                              0, MultiTileEntityMultiBlockPart.ONLY_ITEM_FLUID, aCoordinates, aPlayer, aInventory)) tSuccess = F;
			
			for (int i = 1; i < 8; i++) {
			if (!ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX-1, tY+i, tZ-1, 18112, getMultiTileEntityRegistryID(),                              0, MultiTileEntityMultiBlockPart.ONLY_FLUID_OUT, aCoordinates, aPlayer, aInventory)) tSuccess = F;
			if (!ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX  , tY+i, tZ-1, 18112, getMultiTileEntityRegistryID(),  mFacing == SIDE_Z_POS ? 1 : 0, MultiTileEntityMultiBlockPart.ONLY_FLUID_OUT, aCoordinates, aPlayer, aInventory)) tSuccess = F;
			if (!ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX+1, tY+i, tZ-1, 18112, getMultiTileEntityRegistryID(),                              0, MultiTileEntityMultiBlockPart.ONLY_FLUID_OUT, aCoordinates, aPlayer, aInventory)) tSuccess = F;
			if (!ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX-1, tY+i, tZ  , 18112, getMultiTileEntityRegistryID(),  mFacing == SIDE_X_POS ? 1 : 0, MultiTileEntityMultiBlockPart.ONLY_FLUID_OUT, aCoordinates, aPlayer, aInventory)) tSuccess = F;
			if (!ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX  , tY+i, tZ  , 18112, getMultiTileEntityRegistryID(),                              0, MultiTileEntityMultiBlockPart.ONLY_FLUID_OUT, aCoordinates, aPlayer, aInventory)) tSuccess = F;
			if (!ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX+1, tY+i, tZ  , 18112, getMultiTileEntityRegistryID(),  mFacing == SIDE_X_NEG ? 1 : 0, MultiTileEntityMultiBlockPart.ONLY_FLUID_OUT, aCoordinates, aPlayer, aInventory)) tSuccess = F;
			if (!ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX-1, tY+i, tZ+1, 18112, getMultiTileEntityRegistryID(),                              0, MultiTileEntityMultiBlockPart.ONLY_FLUID_OUT, aCoordinates, aPlayer, aInventory)) tSuccess = F;
			if (!ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX  , tY+i, tZ+1, 18112, getMultiTileEntityRegistryID(),  mFacing == SIDE_Z_NEG ? 1 : 0, MultiTileEntityMultiBlockPart.ONLY_FLUID_OUT, aCoordinates, aPlayer, aInventory)) tSuccess = F;
			if (!ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX+1, tY+i, tZ+1, 18112, getMultiTileEntityRegistryID(),                              0, MultiTileEntityMultiBlockPart.ONLY_FLUID_OUT, aCoordinates, aPlayer, aInventory)) tSuccess = F;
			}
			
			return tSuccess;
		}
		return mStructureOkay;
	}

	static {
		LH.add("gt.tooltip.multiblock.cryodistillationtower.1", "3x3 Base of Heat Transmitters. They can also transfer Cold)");
		LH.add("gt.tooltip.multiblock.cryodistillationtower.2", "3x3x8 of Frost Proof Machine Casing");
	}
}

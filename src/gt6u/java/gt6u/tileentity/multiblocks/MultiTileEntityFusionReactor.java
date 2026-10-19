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
import gregapi.tileentity.energy.ITileEntityEnergy;

import static gregapi.data.CS.*;

/** GT6U's Fusion Reactor (GT6U MultiTileEntityFusionReactor.java): its Electric Interfaces take energy in, none goes out of them. */
public class MultiTileEntityFusionReactor extends gregtech.tileentity.multiblocks.MultiTileEntityFusionReactor {
	@Override
public boolean checkStructure2(BlockPos aCoordinates, Entity aPlayer, Container aInventory) {
		int tX = getOffsetXN(mFacing, 2), tY = getBlockPos().getY(), tZ = getOffsetZN(mFacing, 2);
		if (WD.exists(level, tX-9, tY, tZ-9) && WD.exists(level, tX+9, tY, tZ-9) && WD.exists(level, tX-9, tY, tZ+9) && WD.exists(level, tX+9, tY, tZ+9)) {
			boolean tSuccess = T;
			
			int tVersatile = 3, tLogic = 12, tControl = 12;
			
			for (int i = -2; i <= 2; i++) for (int j = -2; j <= 2; j++) for (int k = -2; k <= 2; k++) {
				if (i*i + j*j + k*k < 4) {
					if (ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX+i, tY+j, tZ+k, 18200, getMultiTileEntityRegistryID(), 0, MultiTileEntityMultiBlockPart.NOTHING, aCoordinates, aPlayer, aInventory)) {
						tVersatile--;
					} else if (ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX+i, tY+j, tZ+k, 18201, getMultiTileEntityRegistryID(), 0, MultiTileEntityMultiBlockPart.NOTHING, aCoordinates, aPlayer, aInventory)) {
						tLogic--;
					} else if (ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX+i, tY+j, tZ+k, 18202, getMultiTileEntityRegistryID(), 0, MultiTileEntityMultiBlockPart.NOTHING, aCoordinates, aPlayer, aInventory)) {
						tControl--;
					} else {
						tSuccess = F;
					}
				} else if (i*i + j*j + k*k > 6 || (j == 0 && (((i == -2 || i == 2) && k == 0) || (((k == -2 || k == 2) && i == 0))))) {
					if (!ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX+i, tY+j, tZ+k, 18008, getMultiTileEntityRegistryID(), 0, MultiTileEntityMultiBlockPart.NOTHING, aCoordinates, aPlayer, aInventory)) tSuccess = F;
				} else {
					if (!ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX+i, tY+j, tZ+k, 18299, getMultiTileEntityRegistryID(), 0, MultiTileEntityMultiBlockPart.NOTHING, aCoordinates, aPlayer, aInventory)) tSuccess = F;
				}
			}
			
			if (tVersatile > 0 || tLogic > 0 || tControl > 0) tSuccess = F;
			
			if (mFacing != SIDE_X_NEG) {
				if (!ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX-3, tY, tZ  , 18008, getMultiTileEntityRegistryID(), 0, MultiTileEntityMultiBlockPart.NOTHING, aCoordinates, aPlayer, aInventory)) tSuccess = F;
				if (!ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX-4, tY, tZ  , 18008, getMultiTileEntityRegistryID(), 0, MultiTileEntityMultiBlockPart.NOTHING, aCoordinates, aPlayer, aInventory)) tSuccess = F;
			}
			if (mFacing != SIDE_X_POS) {
				if (!ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX+3, tY, tZ  , 18008, getMultiTileEntityRegistryID(), 0, MultiTileEntityMultiBlockPart.NOTHING, aCoordinates, aPlayer, aInventory)) tSuccess = F;
				if (!ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX+4, tY, tZ  , 18008, getMultiTileEntityRegistryID(), 0, MultiTileEntityMultiBlockPart.NOTHING, aCoordinates, aPlayer, aInventory)) tSuccess = F;
			}
			if (mFacing != SIDE_Z_NEG) {
				if (!ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX  , tY, tZ-3, 18008, getMultiTileEntityRegistryID(), 0, MultiTileEntityMultiBlockPart.NOTHING, aCoordinates, aPlayer, aInventory)) tSuccess = F;
				if (!ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX  , tY, tZ-4, 18008, getMultiTileEntityRegistryID(), 0, MultiTileEntityMultiBlockPart.NOTHING, aCoordinates, aPlayer, aInventory)) tSuccess = F;
			}
			if (mFacing != SIDE_Z_POS) {
				if (!ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX  , tY, tZ+3, 18008, getMultiTileEntityRegistryID(), 0, MultiTileEntityMultiBlockPart.NOTHING, aCoordinates, aPlayer, aInventory)) tSuccess = F;
				if (!ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX  , tY, tZ+4, 18008, getMultiTileEntityRegistryID(), 0, MultiTileEntityMultiBlockPart.NOTHING, aCoordinates, aPlayer, aInventory)) tSuccess = F;
			}
			
			tX -= 9; tZ -= 9;
			
			for (int i = 0; i < 19; i++) for (int j = 0; j < 19; j++) {
				if (OCTAGONS[0][i][j]) {
					if (!ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX+i, tY-1, tZ+j, 18003, getMultiTileEntityRegistryID(), 0, MultiTileEntityMultiBlockPart.ONLY_ITEM_FLUID, aCoordinates, aPlayer, aInventory)) tSuccess = F;
					if ((i == 9 && (j == 0 || j == 18)) || (j == 9 && (i == 0 || i == 18))) {
					if (!ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX+i, tY  , tZ+j, 18003, getMultiTileEntityRegistryID(), 2, MultiTileEntityMultiBlockPart.ONLY_ENERGY_IN, aCoordinates, aPlayer, aInventory)) tSuccess = F;
					} else {
					if (!ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX+i, tY  , tZ+j, 18003, getMultiTileEntityRegistryID(), mActive ? 6 : 5, MultiTileEntityMultiBlockPart.ONLY_ENERGY_IN, aCoordinates, aPlayer, aInventory)) tSuccess = F;
					}
					if (!ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX+i, tY+1, tZ+j, 18003, getMultiTileEntityRegistryID(), 0, MultiTileEntityMultiBlockPart.ONLY_ITEM_FLUID, aCoordinates, aPlayer, aInventory)) tSuccess = F;
				}
				if (OCTAGONS[1][i][j]) {
					if (!ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX+i, tY-2, tZ+j, 18003, getMultiTileEntityRegistryID(), 0, MultiTileEntityMultiBlockPart.ONLY_ITEM_FLUID, aCoordinates, aPlayer, aInventory)) tSuccess = F;
					
					if (!ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX+i, tY-1, tZ+j, 18003, getMultiTileEntityRegistryID(), 0, MultiTileEntityMultiBlockPart.NOTHING, aCoordinates, aPlayer, aInventory)) tSuccess = F;
					
					if (!ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX+i, tY  , tZ+j, 18045, getMultiTileEntityRegistryID(), 0, MultiTileEntityMultiBlockPart.NOTHING, aCoordinates, aPlayer, aInventory)) tSuccess = F;
					
					if (!ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX+i, tY+1, tZ+j, 18003, getMultiTileEntityRegistryID(), 0, MultiTileEntityMultiBlockPart.NOTHING, aCoordinates, aPlayer, aInventory)) tSuccess = F;
					
					if (!ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX+i, tY+2, tZ+j, 18003, getMultiTileEntityRegistryID(), 0, MultiTileEntityMultiBlockPart.ONLY_ITEM_FLUID, aCoordinates, aPlayer, aInventory)) tSuccess = F;
				}
				if (OCTAGONS[2][i][j]) {
					if (!ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX+i, tY-2, tZ+j, 18003, getMultiTileEntityRegistryID(), 0, MultiTileEntityMultiBlockPart.ONLY_ITEM_FLUID, aCoordinates, aPlayer, aInventory)) tSuccess = F;
					
					if (!ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX+i, tY-1, tZ+j, 18045, getMultiTileEntityRegistryID(), 0, MultiTileEntityMultiBlockPart.NOTHING, aCoordinates, aPlayer, aInventory)) tSuccess = F;
					
					if (!ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX+i, tY  , tZ+j, 18002, getMultiTileEntityRegistryID(), 0, MultiTileEntityMultiBlockPart.NOTHING, aCoordinates, aPlayer, aInventory)) tSuccess = F;
					
					if (!ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX+i, tY+1, tZ+j, 18045, getMultiTileEntityRegistryID(), 0, MultiTileEntityMultiBlockPart.NOTHING, aCoordinates, aPlayer, aInventory)) tSuccess = F;
					
					if (!ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX+i, tY+2, tZ+j, 18003, getMultiTileEntityRegistryID(), 0, MultiTileEntityMultiBlockPart.ONLY_ITEM_FLUID, aCoordinates, aPlayer, aInventory)) tSuccess = F;
				}
			}
			return tSuccess;
		}
		return mStructureOkay;
	}

	// GT6U drops GT6's output through the Electric Interfaces; the reactor emits as any basic machine (MultiTileEntityBasicMachine.doOutputEnergy).
	@Override
	public void doOutputEnergy() {
		ITileEntityEnergy.Util.emitEnergyToSide(mEnergyTypeEmitted, FACING_TO_SIDE[mFacing][mEnergyOutput], mOutputEnergy, 1, this);
	}

	static {
		LH.add("gt.tooltip.multiblock.fusionreactor.5", "Input energy for start. Then for process");
	}
}

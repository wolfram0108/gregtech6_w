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

import gregapi.util.WD;
import gregapi.data.LH;
import gregapi.data.LH.Chat;
import gregapi.tileentity.delegate.DelegatorTileEntity;
import gregapi.tileentity.multiblocks.ITileEntityMultiBlockController;
import gregapi.tileentity.multiblocks.MultiTileEntityMultiBlockPart;
import gregapi.tileentity.multiblocks.TileEntityBase10MultiBlockMachine;
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
public class MultiTileEntityAutoclave extends TileEntityBase10MultiBlockMachine {
	@Override
	public boolean checkStructure2(BlockPos aCoordinates, Entity aPlayer, Container aInventory) {
		int tX = getOffsetXN(mFacing), tY = getBlockPos().getY()+1, tZ = getOffsetZN(mFacing);
		if (WD.exists(level, tX-1, tY, tZ-1) && WD.exists(level, tX+1, tY, tZ-1) && WD.exists(level, tX-1, tY, tZ+1) && WD.exists(level, tX+1, tY, tZ+1)) {
			boolean tSuccess = T;
			for (int i = -1; i <= 1; i++) for (int j = -1; j <= 1; j++) for (int k = -1; k <= 1; k++) {
				if (i == 0 && j == 0 && k == 0) {
					if (getAir(tX+i, tY+j, tZ+k)) WD.set(level, tX+i, tY+j, tZ+k, NB, 0, 3); else tSuccess = F;
				} else {
					if (!ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX+i, tY+j, tZ+k, 18022, getMultiTileEntityRegistryID(), 0, MultiTileEntityMultiBlockPart.ONLY_ITEM_FLUID_ENERGY, aCoordinates, aPlayer, aInventory)) tSuccess = F;
				}
			}
			return tSuccess;
		}
		return mStructureOkay;
	}
	
	static {
		LH.add("gt.tooltip.multiblock.autoclave.1", "3x3x3 Hollow of Dense Stainless Steel Walls");
		LH.add("gt.tooltip.multiblock.autoclave.2", "Main Block centered on Side-Bottom and facing outwards");
		LH.add("gt.tooltip.multiblock.autoclave.3", "Input and Output at any Blocks");
	}
	
	@Override
	public void addToolTips(List<String> aList, ItemStack aStack, boolean aF3_H) {
		aList.add(Chat.CYAN     + LH.get(LH.STRUCTURE) + ":");
		aList.add(Chat.WHITE    + LH.get("gt.tooltip.multiblock.autoclave.1"));
		aList.add(Chat.WHITE    + LH.get("gt.tooltip.multiblock.autoclave.2"));
		aList.add(Chat.WHITE    + LH.get("gt.tooltip.multiblock.autoclave.3"));
		super.addToolTips(aList, aStack, aF3_H);
	}
	
	@Override
	public boolean isInsideStructure(int aX, int aY, int aZ) {
		int tX = getOffsetXN(mFacing), tY = getBlockPos().getY(), tZ = getOffsetZN(mFacing);
		return aX >= tX - 1 && aY >= tY && aZ >= tZ - 1 && aX <= tX + 1 && aY <= tY + 2 && aZ <= tZ + 1;
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
	
	@Override public String getTileEntityName() {return "gt.multitileentity.multiblock.autoclave";}
}

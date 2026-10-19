/**
 * Copyright (c) 2019 Gregorius Techneticies
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

import gregapi.util.WD;

import gregapi.data.LH;
import gregapi.data.LH.Chat;
import gregapi.tileentity.delegate.DelegatorTileEntity;
import gregapi.tileentity.multiblocks.ITileEntityMultiBlockController;
import gregapi.tileentity.multiblocks.MultiTileEntityMultiBlockPart;
import gregapi.tileentity.multiblocks.TileEntityBase10MultiBlockMachine;
import net.minecraft.world.level.block.Block;
import gregapi.block.Material;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.fluids.capability.IFluidHandler;

import java.util.List;

import static gregapi.data.CS.*;
/**
 * @author Gregorius Techneticies
 */
public class MultiTileEntityAquaticFarm extends TileEntityBase10MultiBlockMachine {
	@Override
	public boolean checkStructure2() {
		int
				tMinX = getBlockPos().getX()-(SIDE_X_NEG==mFacing?0:SIDE_X_POS==mFacing?6:2),
				tMinZ = getBlockPos().getZ()-(SIDE_Z_NEG==mFacing?0:SIDE_Z_POS==mFacing?6:2),
				tMaxX = getBlockPos().getX()+(SIDE_X_POS==mFacing?0:SIDE_X_NEG==mFacing?6:2),
				tMaxZ = getBlockPos().getZ()+(SIDE_Z_POS==mFacing?0:SIDE_Z_NEG==mFacing?6:2);
		int
				tD = mActive?2:mRunning?1:0;

		if (WD.exists(level, tMinX, getBlockPos().getY(), tMinZ) && WD.exists(level, tMaxX, getBlockPos().getY()+2, tMaxZ)) {
			boolean tSuccess = T;
			for (int tX = tMinX; tX <= tMaxX; tX++) for (int tZ = tMinZ; tZ <= tMaxZ; tZ++) {

				// Conditions
				boolean isSides = tX == tMinX || tX == tMaxX || tZ == tMinZ || tZ == tMaxZ;
				boolean isCorners = tX == tMinX && tZ == tMinZ || tX == tMinX && tZ == tMaxZ || tX == tMaxX && tZ == tMinZ || tX == tMaxX && tZ == tMaxZ;
				boolean isNotSides = tX != tMinX && tX != tMaxX && tZ != tMinZ && tZ != tMaxZ;

				// Layer 1
				if (!ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX, getBlockPos().getY(), tZ, isSides ? 18002 : 18298, getMultiTileEntityRegistryID(), 0, MultiTileEntityMultiBlockPart.ONLY_ENERGY)) tSuccess = F;

				// Layer 2
				if (isNotSides) {
					if (getWater(tX, getBlockPos().getY() + 1, tZ)) WD.set(level, tX, getBlockPos().getY() + 1, tZ, Blocks.WATER, 0, 3); else tSuccess = F;
				} else {
					if (!ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX, getBlockPos().getY() + 1, tZ, isCorners ? 18002 : 18298, getMultiTileEntityRegistryID(), isCorners ? 0 : tD, MultiTileEntityMultiBlockPart.ONLY_ITEM_FLUID)) tSuccess = F;
				}

				// Layer 3
				if (isNotSides) {
					if (getWater(tX, getBlockPos().getY() + 2, tZ)) WD.set(level, tX, getBlockPos().getY() + 2, tZ, Blocks.WATER, 0, 3); else tSuccess = F;
				} else {
					if (!ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX, getBlockPos().getY() + 2, tZ, 18002, getMultiTileEntityRegistryID(), 0, MultiTileEntityMultiBlockPart.ONLY_ITEM_FLUID)) tSuccess = F;
				}

			}
			return tSuccess;
		}
		return mStructureOkay;
		
	}

	private boolean getWater(int aX, int aY, int aZ) {
		if (level == null) return T;
		if (mIgnoreUnloadedChunks && crossedChunkBorder(aX, aZ) && !WD.exists(level, aX, aY, aZ)) return T;
		// GT6U asked the block's material, so GT6's river, ocean and swamp waters fill the farm as well.
		return WD.getMaterial(WD.block(level, aX, aY, aZ)) == Material.water;
	}

	
	static {
		LH.add("gt.tooltip.multiblock.aquaticfarm.1", "5(Width)x3(Height)x7(Length) Hollow Cuboid with top side opened");
		LH.add("gt.tooltip.multiblock.aquaticfarm.2", "Main Block centered on 5x3-Side-Bottom and facing outwards");
		LH.add("gt.tooltip.multiblock.aquaticfarm.3", "The Edges of the Cuboid are Stainless Steel Walls");
		LH.add("gt.tooltip.multiblock.aquaticfarm.4", "The Faces of the Cuboid are Aquatic Farm Blocks");
		LH.add("gt.tooltip.multiblock.aquaticfarm.5", "Fill the Hollow space with Still Water");
		LH.add("gt.tooltip.multiblock.aquaticfarm.6", "Stuff can go in and out at middle and top layer");
		LH.add("gt.tooltip.multiblock.aquaticfarm.7", "Energy input at the bottom layer");
	}
	
	@Override
	public void addToolTips(List<String> aList, ItemStack aStack, boolean aF3_H) {
		aList.add(Chat.CYAN  + LH.get(LH.STRUCTURE) + ":");
		aList.add(Chat.WHITE + LH.get("gt.tooltip.multiblock.aquaticfarm.1"));
		aList.add(Chat.WHITE + LH.get("gt.tooltip.multiblock.aquaticfarm.2"));
		aList.add(Chat.WHITE + LH.get("gt.tooltip.multiblock.aquaticfarm.3"));
		aList.add(Chat.WHITE + LH.get("gt.tooltip.multiblock.aquaticfarm.4"));
		aList.add(Chat.WHITE + LH.get("gt.tooltip.multiblock.aquaticfarm.5"));
		aList.add(Chat.WHITE + LH.get("gt.tooltip.multiblock.aquaticfarm.6"));
		aList.add(Chat.WHITE + LH.get("gt.tooltip.multiblock.aquaticfarm.7"));
		super.addToolTips(aList, aStack, aF3_H);
	}


	@Override
	public boolean isInsideStructure(int aX, int aY, int aZ) {
		return
				aX >= getBlockPos().getX() - (SIDE_X_NEG == mFacing ? 0 : SIDE_X_POS == mFacing ? 6 : 2) &&
						aY >= getBlockPos().getY() &&
						aZ >= getBlockPos().getZ() - (SIDE_Z_NEG == mFacing ? 0 : SIDE_Z_POS == mFacing ? 6 : 2) &&
						aX <= getBlockPos().getX() + (SIDE_X_POS == mFacing ? 0 : SIDE_X_NEG == mFacing ? 6 : 2) &&
						aY <= getBlockPos().getY() + 2 &&
						aZ <= getBlockPos().getZ() + (SIDE_Z_POS == mFacing ? 0 : SIDE_Z_NEG == mFacing ? 6 : 2);
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
	
	@Override public String getTileEntityName() {return "gt.multitileentity.multiblock.aquaticfarm";}
}

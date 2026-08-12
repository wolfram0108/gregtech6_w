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
public class MultiTileEntityMatterFabricator extends TileEntityBase10MultiBlockMachine {
	@Override
	public boolean checkStructure2(BlockPos aCoordinates, Entity aPlayer, Container aInventory) {
		int tX = getOffsetXN(mFacing, 2)-2, tY = getBlockPos().getY(), tZ = getOffsetZN(mFacing, 2)-2;
		if (WD.exists(level, tX, tY, tZ) && WD.exists(level, tX+4, tY, tZ) && WD.exists(level, tX, tY, tZ+4) && WD.exists(level, tX+4, tY, tZ+4)) {
			boolean tSuccess = T;
			
			if (!ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX  , tY  , tZ  , 18031, getMultiTileEntityRegistryID(), 0, MultiTileEntityMultiBlockPart.ONLY_ITEM_FLUID_ENERGY, aCoordinates, aPlayer, aInventory)) tSuccess = F;
			if (!ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX+1, tY  , tZ  , 18031, getMultiTileEntityRegistryID(), 0, MultiTileEntityMultiBlockPart.ONLY_ITEM_FLUID_ENERGY, aCoordinates, aPlayer, aInventory)) tSuccess = F;
			if (!ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX+2, tY  , tZ  , 18031, getMultiTileEntityRegistryID(), 0, MultiTileEntityMultiBlockPart.ONLY_ITEM_FLUID_ENERGY, aCoordinates, aPlayer, aInventory)) tSuccess = F;
			if (!ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX+3, tY  , tZ  , 18031, getMultiTileEntityRegistryID(), 0, MultiTileEntityMultiBlockPart.ONLY_ITEM_FLUID_ENERGY, aCoordinates, aPlayer, aInventory)) tSuccess = F;
			if (!ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX+4, tY  , tZ  , 18031, getMultiTileEntityRegistryID(), 0, MultiTileEntityMultiBlockPart.ONLY_ITEM_FLUID_ENERGY, aCoordinates, aPlayer, aInventory)) tSuccess = F;
			if (!ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX  , tY  , tZ+1, 18031, getMultiTileEntityRegistryID(), 0, MultiTileEntityMultiBlockPart.ONLY_ITEM_FLUID_ENERGY, aCoordinates, aPlayer, aInventory)) tSuccess = F;
			if (!ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX+1, tY  , tZ+1, 18031, getMultiTileEntityRegistryID(), 0, MultiTileEntityMultiBlockPart.ONLY_ITEM_FLUID_ENERGY, aCoordinates, aPlayer, aInventory)) tSuccess = F;
			if (!ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX+2, tY  , tZ+1, 18031, getMultiTileEntityRegistryID(), 0, MultiTileEntityMultiBlockPart.ONLY_ITEM_FLUID_ENERGY, aCoordinates, aPlayer, aInventory)) tSuccess = F;
			if (!ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX+3, tY  , tZ+1, 18031, getMultiTileEntityRegistryID(), 0, MultiTileEntityMultiBlockPart.ONLY_ITEM_FLUID_ENERGY, aCoordinates, aPlayer, aInventory)) tSuccess = F;
			if (!ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX+4, tY  , tZ+1, 18031, getMultiTileEntityRegistryID(), 0, MultiTileEntityMultiBlockPart.ONLY_ITEM_FLUID_ENERGY, aCoordinates, aPlayer, aInventory)) tSuccess = F;
			if (!ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX  , tY  , tZ+2, 18031, getMultiTileEntityRegistryID(), 0, MultiTileEntityMultiBlockPart.ONLY_ITEM_FLUID_ENERGY, aCoordinates, aPlayer, aInventory)) tSuccess = F;
			if (!ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX+1, tY  , tZ+2, 18031, getMultiTileEntityRegistryID(), 0, MultiTileEntityMultiBlockPart.ONLY_ITEM_FLUID_ENERGY, aCoordinates, aPlayer, aInventory)) tSuccess = F;
			if (!ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX+2, tY  , tZ+2, 18031, getMultiTileEntityRegistryID(), 0, MultiTileEntityMultiBlockPart.ONLY_ITEM_FLUID_ENERGY, aCoordinates, aPlayer, aInventory)) tSuccess = F;
			if (!ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX+3, tY  , tZ+2, 18031, getMultiTileEntityRegistryID(), 0, MultiTileEntityMultiBlockPart.ONLY_ITEM_FLUID_ENERGY, aCoordinates, aPlayer, aInventory)) tSuccess = F;
			if (!ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX+4, tY  , tZ+2, 18031, getMultiTileEntityRegistryID(), 0, MultiTileEntityMultiBlockPart.ONLY_ITEM_FLUID_ENERGY, aCoordinates, aPlayer, aInventory)) tSuccess = F;
			if (!ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX  , tY  , tZ+3, 18031, getMultiTileEntityRegistryID(), 0, MultiTileEntityMultiBlockPart.ONLY_ITEM_FLUID_ENERGY, aCoordinates, aPlayer, aInventory)) tSuccess = F;
			if (!ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX+1, tY  , tZ+3, 18031, getMultiTileEntityRegistryID(), 0, MultiTileEntityMultiBlockPart.ONLY_ITEM_FLUID_ENERGY, aCoordinates, aPlayer, aInventory)) tSuccess = F;
			if (!ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX+2, tY  , tZ+3, 18031, getMultiTileEntityRegistryID(), 0, MultiTileEntityMultiBlockPart.ONLY_ITEM_FLUID_ENERGY, aCoordinates, aPlayer, aInventory)) tSuccess = F;
			if (!ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX+3, tY  , tZ+3, 18031, getMultiTileEntityRegistryID(), 0, MultiTileEntityMultiBlockPart.ONLY_ITEM_FLUID_ENERGY, aCoordinates, aPlayer, aInventory)) tSuccess = F;
			if (!ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX+4, tY  , tZ+3, 18031, getMultiTileEntityRegistryID(), 0, MultiTileEntityMultiBlockPart.ONLY_ITEM_FLUID_ENERGY, aCoordinates, aPlayer, aInventory)) tSuccess = F;
			if (!ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX  , tY  , tZ+4, 18031, getMultiTileEntityRegistryID(), 0, MultiTileEntityMultiBlockPart.ONLY_ITEM_FLUID_ENERGY, aCoordinates, aPlayer, aInventory)) tSuccess = F;
			if (!ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX+1, tY  , tZ+4, 18031, getMultiTileEntityRegistryID(), 0, MultiTileEntityMultiBlockPart.ONLY_ITEM_FLUID_ENERGY, aCoordinates, aPlayer, aInventory)) tSuccess = F;
			if (!ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX+2, tY  , tZ+4, 18031, getMultiTileEntityRegistryID(), 0, MultiTileEntityMultiBlockPart.ONLY_ITEM_FLUID_ENERGY, aCoordinates, aPlayer, aInventory)) tSuccess = F;
			if (!ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX+3, tY  , tZ+4, 18031, getMultiTileEntityRegistryID(), 0, MultiTileEntityMultiBlockPart.ONLY_ITEM_FLUID_ENERGY, aCoordinates, aPlayer, aInventory)) tSuccess = F;
			if (!ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX+4, tY  , tZ+4, 18031, getMultiTileEntityRegistryID(), 0, MultiTileEntityMultiBlockPart.ONLY_ITEM_FLUID_ENERGY, aCoordinates, aPlayer, aInventory)) tSuccess = F;
			
			if (!ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX  , tY+1, tZ  , 18031, getMultiTileEntityRegistryID(), 0, MultiTileEntityMultiBlockPart.ONLY_ITEM_FLUID_ENERGY, aCoordinates, aPlayer, aInventory)) tSuccess = F;
			if (!ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX+1, tY+1, tZ  , 18031, getMultiTileEntityRegistryID(), 0, MultiTileEntityMultiBlockPart.ONLY_ITEM_FLUID_ENERGY, aCoordinates, aPlayer, aInventory)) tSuccess = F;
			if (!ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX+2, tY+1, tZ  , 18031, getMultiTileEntityRegistryID(), 0, MultiTileEntityMultiBlockPart.ONLY_ITEM_FLUID_ENERGY, aCoordinates, aPlayer, aInventory)) tSuccess = F;
			if (!ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX+3, tY+1, tZ  , 18031, getMultiTileEntityRegistryID(), 0, MultiTileEntityMultiBlockPart.ONLY_ITEM_FLUID_ENERGY, aCoordinates, aPlayer, aInventory)) tSuccess = F;
			if (!ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX+4, tY+1, tZ  , 18031, getMultiTileEntityRegistryID(), 0, MultiTileEntityMultiBlockPart.ONLY_ITEM_FLUID_ENERGY, aCoordinates, aPlayer, aInventory)) tSuccess = F;
			if (!ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX  , tY+1, tZ+1, 18031, getMultiTileEntityRegistryID(), 0, MultiTileEntityMultiBlockPart.ONLY_ITEM_FLUID_ENERGY, aCoordinates, aPlayer, aInventory)) tSuccess = F;
			if (!ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX+1, tY+1, tZ+1, 18044, getMultiTileEntityRegistryID(), 0, MultiTileEntityMultiBlockPart.ONLY_ITEM_FLUID_ENERGY, aCoordinates, aPlayer, aInventory)) tSuccess = F;
			if (!ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX+2, tY+1, tZ+1, 18044, getMultiTileEntityRegistryID(), 0, MultiTileEntityMultiBlockPart.ONLY_ITEM_FLUID_ENERGY, aCoordinates, aPlayer, aInventory)) tSuccess = F;
			if (!ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX+3, tY+1, tZ+1, 18044, getMultiTileEntityRegistryID(), 0, MultiTileEntityMultiBlockPart.ONLY_ITEM_FLUID_ENERGY, aCoordinates, aPlayer, aInventory)) tSuccess = F;
			if (!ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX+4, tY+1, tZ+1, 18031, getMultiTileEntityRegistryID(), 0, MultiTileEntityMultiBlockPart.ONLY_ITEM_FLUID_ENERGY, aCoordinates, aPlayer, aInventory)) tSuccess = F;
			if (!ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX  , tY+1, tZ+2, 18031, getMultiTileEntityRegistryID(), 0, MultiTileEntityMultiBlockPart.ONLY_ITEM_FLUID_ENERGY, aCoordinates, aPlayer, aInventory)) tSuccess = F;
			if (!ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX+1, tY+1, tZ+2, 18044, getMultiTileEntityRegistryID(), 0, MultiTileEntityMultiBlockPart.ONLY_ITEM_FLUID_ENERGY, aCoordinates, aPlayer, aInventory)) tSuccess = F;
			if (!ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX+2, tY+1, tZ+2, 18044, getMultiTileEntityRegistryID(), 0, MultiTileEntityMultiBlockPart.ONLY_ITEM_FLUID_ENERGY, aCoordinates, aPlayer, aInventory)) tSuccess = F;
			if (!ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX+3, tY+1, tZ+2, 18044, getMultiTileEntityRegistryID(), 0, MultiTileEntityMultiBlockPart.ONLY_ITEM_FLUID_ENERGY, aCoordinates, aPlayer, aInventory)) tSuccess = F;
			if (!ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX+4, tY+1, tZ+2, 18031, getMultiTileEntityRegistryID(), 0, MultiTileEntityMultiBlockPart.ONLY_ITEM_FLUID_ENERGY, aCoordinates, aPlayer, aInventory)) tSuccess = F;
			if (!ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX  , tY+1, tZ+3, 18031, getMultiTileEntityRegistryID(), 0, MultiTileEntityMultiBlockPart.ONLY_ITEM_FLUID_ENERGY, aCoordinates, aPlayer, aInventory)) tSuccess = F;
			if (!ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX+1, tY+1, tZ+3, 18044, getMultiTileEntityRegistryID(), 0, MultiTileEntityMultiBlockPart.ONLY_ITEM_FLUID_ENERGY, aCoordinates, aPlayer, aInventory)) tSuccess = F;
			if (!ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX+2, tY+1, tZ+3, 18044, getMultiTileEntityRegistryID(), 0, MultiTileEntityMultiBlockPart.ONLY_ITEM_FLUID_ENERGY, aCoordinates, aPlayer, aInventory)) tSuccess = F;
			if (!ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX+3, tY+1, tZ+3, 18044, getMultiTileEntityRegistryID(), 0, MultiTileEntityMultiBlockPart.ONLY_ITEM_FLUID_ENERGY, aCoordinates, aPlayer, aInventory)) tSuccess = F;
			if (!ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX+4, tY+1, tZ+3, 18031, getMultiTileEntityRegistryID(), 0, MultiTileEntityMultiBlockPart.ONLY_ITEM_FLUID_ENERGY, aCoordinates, aPlayer, aInventory)) tSuccess = F;
			if (!ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX  , tY+1, tZ+4, 18031, getMultiTileEntityRegistryID(), 0, MultiTileEntityMultiBlockPart.ONLY_ITEM_FLUID_ENERGY, aCoordinates, aPlayer, aInventory)) tSuccess = F;
			if (!ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX+1, tY+1, tZ+4, 18031, getMultiTileEntityRegistryID(), 0, MultiTileEntityMultiBlockPart.ONLY_ITEM_FLUID_ENERGY, aCoordinates, aPlayer, aInventory)) tSuccess = F;
			if (!ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX+2, tY+1, tZ+4, 18031, getMultiTileEntityRegistryID(), 0, MultiTileEntityMultiBlockPart.ONLY_ITEM_FLUID_ENERGY, aCoordinates, aPlayer, aInventory)) tSuccess = F;
			if (!ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX+3, tY+1, tZ+4, 18031, getMultiTileEntityRegistryID(), 0, MultiTileEntityMultiBlockPart.ONLY_ITEM_FLUID_ENERGY, aCoordinates, aPlayer, aInventory)) tSuccess = F;
			if (!ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX+4, tY+1, tZ+4, 18031, getMultiTileEntityRegistryID(), 0, MultiTileEntityMultiBlockPart.ONLY_ITEM_FLUID_ENERGY, aCoordinates, aPlayer, aInventory)) tSuccess = F;
			
			if (!ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX  , tY+2, tZ  , 18031, getMultiTileEntityRegistryID(), 0, MultiTileEntityMultiBlockPart.ONLY_ITEM_FLUID_ENERGY, aCoordinates, aPlayer, aInventory)) tSuccess = F;
			if (!ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX+1, tY+2, tZ  , 18031, getMultiTileEntityRegistryID(), 0, MultiTileEntityMultiBlockPart.ONLY_ITEM_FLUID_ENERGY, aCoordinates, aPlayer, aInventory)) tSuccess = F;
			if (!ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX+2, tY+2, tZ  , 18031, getMultiTileEntityRegistryID(), 0, MultiTileEntityMultiBlockPart.ONLY_ITEM_FLUID_ENERGY, aCoordinates, aPlayer, aInventory)) tSuccess = F;
			if (!ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX+3, tY+2, tZ  , 18031, getMultiTileEntityRegistryID(), 0, MultiTileEntityMultiBlockPart.ONLY_ITEM_FLUID_ENERGY, aCoordinates, aPlayer, aInventory)) tSuccess = F;
			if (!ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX+4, tY+2, tZ  , 18031, getMultiTileEntityRegistryID(), 0, MultiTileEntityMultiBlockPart.ONLY_ITEM_FLUID_ENERGY, aCoordinates, aPlayer, aInventory)) tSuccess = F;
			if (!ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX  , tY+2, tZ+1, 18031, getMultiTileEntityRegistryID(), 0, MultiTileEntityMultiBlockPart.ONLY_ITEM_FLUID_ENERGY, aCoordinates, aPlayer, aInventory)) tSuccess = F;
			if (!ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX+1, tY+2, tZ+1, 18044, getMultiTileEntityRegistryID(), 0, MultiTileEntityMultiBlockPart.ONLY_ITEM_FLUID_ENERGY, aCoordinates, aPlayer, aInventory)) tSuccess = F;
			if (!ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX+2, tY+2, tZ+1, 18044, getMultiTileEntityRegistryID(), 0, MultiTileEntityMultiBlockPart.ONLY_ITEM_FLUID_ENERGY, aCoordinates, aPlayer, aInventory)) tSuccess = F;
			if (!ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX+3, tY+2, tZ+1, 18044, getMultiTileEntityRegistryID(), 0, MultiTileEntityMultiBlockPart.ONLY_ITEM_FLUID_ENERGY, aCoordinates, aPlayer, aInventory)) tSuccess = F;
			if (!ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX+4, tY+2, tZ+1, 18031, getMultiTileEntityRegistryID(), 0, MultiTileEntityMultiBlockPart.ONLY_ITEM_FLUID_ENERGY, aCoordinates, aPlayer, aInventory)) tSuccess = F;
			if (!ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX  , tY+2, tZ+2, 18031, getMultiTileEntityRegistryID(), 0, MultiTileEntityMultiBlockPart.ONLY_ITEM_FLUID_ENERGY, aCoordinates, aPlayer, aInventory)) tSuccess = F;
			if (!ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX+1, tY+2, tZ+2, 18044, getMultiTileEntityRegistryID(), 0, MultiTileEntityMultiBlockPart.ONLY_ITEM_FLUID_ENERGY, aCoordinates, aPlayer, aInventory)) tSuccess = F;
			if (getAir(tX+2, tY+2, tZ+2)) WD.set(level, tX+2, tY+2, tZ+2, NB, 0, 3); else tSuccess = F;
			if (!ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX+3, tY+2, tZ+2, 18044, getMultiTileEntityRegistryID(), 0, MultiTileEntityMultiBlockPart.ONLY_ITEM_FLUID_ENERGY, aCoordinates, aPlayer, aInventory)) tSuccess = F;
			if (!ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX+4, tY+2, tZ+2, 18031, getMultiTileEntityRegistryID(), 0, MultiTileEntityMultiBlockPart.ONLY_ITEM_FLUID_ENERGY, aCoordinates, aPlayer, aInventory)) tSuccess = F;
			if (!ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX  , tY+2, tZ+3, 18031, getMultiTileEntityRegistryID(), 0, MultiTileEntityMultiBlockPart.ONLY_ITEM_FLUID_ENERGY, aCoordinates, aPlayer, aInventory)) tSuccess = F;
			if (!ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX+1, tY+2, tZ+3, 18044, getMultiTileEntityRegistryID(), 0, MultiTileEntityMultiBlockPart.ONLY_ITEM_FLUID_ENERGY, aCoordinates, aPlayer, aInventory)) tSuccess = F;
			if (!ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX+2, tY+2, tZ+3, 18044, getMultiTileEntityRegistryID(), 0, MultiTileEntityMultiBlockPart.ONLY_ITEM_FLUID_ENERGY, aCoordinates, aPlayer, aInventory)) tSuccess = F;
			if (!ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX+3, tY+2, tZ+3, 18044, getMultiTileEntityRegistryID(), 0, MultiTileEntityMultiBlockPart.ONLY_ITEM_FLUID_ENERGY, aCoordinates, aPlayer, aInventory)) tSuccess = F;
			if (!ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX+4, tY+2, tZ+3, 18031, getMultiTileEntityRegistryID(), 0, MultiTileEntityMultiBlockPart.ONLY_ITEM_FLUID_ENERGY, aCoordinates, aPlayer, aInventory)) tSuccess = F;
			if (!ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX  , tY+2, tZ+4, 18031, getMultiTileEntityRegistryID(), 0, MultiTileEntityMultiBlockPart.ONLY_ITEM_FLUID_ENERGY, aCoordinates, aPlayer, aInventory)) tSuccess = F;
			if (!ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX+1, tY+2, tZ+4, 18031, getMultiTileEntityRegistryID(), 0, MultiTileEntityMultiBlockPart.ONLY_ITEM_FLUID_ENERGY, aCoordinates, aPlayer, aInventory)) tSuccess = F;
			if (!ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX+2, tY+2, tZ+4, 18031, getMultiTileEntityRegistryID(), 0, MultiTileEntityMultiBlockPart.ONLY_ITEM_FLUID_ENERGY, aCoordinates, aPlayer, aInventory)) tSuccess = F;
			if (!ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX+3, tY+2, tZ+4, 18031, getMultiTileEntityRegistryID(), 0, MultiTileEntityMultiBlockPart.ONLY_ITEM_FLUID_ENERGY, aCoordinates, aPlayer, aInventory)) tSuccess = F;
			if (!ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX+4, tY+2, tZ+4, 18031, getMultiTileEntityRegistryID(), 0, MultiTileEntityMultiBlockPart.ONLY_ITEM_FLUID_ENERGY, aCoordinates, aPlayer, aInventory)) tSuccess = F;
			
			if (!ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX  , tY+3, tZ  , 18031, getMultiTileEntityRegistryID(), 0, MultiTileEntityMultiBlockPart.ONLY_ITEM_FLUID_ENERGY, aCoordinates, aPlayer, aInventory)) tSuccess = F;
			if (!ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX+1, tY+3, tZ  , 18031, getMultiTileEntityRegistryID(), 0, MultiTileEntityMultiBlockPart.ONLY_ITEM_FLUID_ENERGY, aCoordinates, aPlayer, aInventory)) tSuccess = F;
			if (!ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX+2, tY+3, tZ  , 18031, getMultiTileEntityRegistryID(), 0, MultiTileEntityMultiBlockPart.ONLY_ITEM_FLUID_ENERGY, aCoordinates, aPlayer, aInventory)) tSuccess = F;
			if (!ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX+3, tY+3, tZ  , 18031, getMultiTileEntityRegistryID(), 0, MultiTileEntityMultiBlockPart.ONLY_ITEM_FLUID_ENERGY, aCoordinates, aPlayer, aInventory)) tSuccess = F;
			if (!ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX+4, tY+3, tZ  , 18031, getMultiTileEntityRegistryID(), 0, MultiTileEntityMultiBlockPart.ONLY_ITEM_FLUID_ENERGY, aCoordinates, aPlayer, aInventory)) tSuccess = F;
			if (!ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX  , tY+3, tZ+1, 18031, getMultiTileEntityRegistryID(), 0, MultiTileEntityMultiBlockPart.ONLY_ITEM_FLUID_ENERGY, aCoordinates, aPlayer, aInventory)) tSuccess = F;
			if (!ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX+1, tY+3, tZ+1, 18044, getMultiTileEntityRegistryID(), 0, MultiTileEntityMultiBlockPart.ONLY_ITEM_FLUID_ENERGY, aCoordinates, aPlayer, aInventory)) tSuccess = F;
			if (!ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX+2, tY+3, tZ+1, 18044, getMultiTileEntityRegistryID(), 0, MultiTileEntityMultiBlockPart.ONLY_ITEM_FLUID_ENERGY, aCoordinates, aPlayer, aInventory)) tSuccess = F;
			if (!ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX+3, tY+3, tZ+1, 18044, getMultiTileEntityRegistryID(), 0, MultiTileEntityMultiBlockPart.ONLY_ITEM_FLUID_ENERGY, aCoordinates, aPlayer, aInventory)) tSuccess = F;
			if (!ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX+4, tY+3, tZ+1, 18031, getMultiTileEntityRegistryID(), 0, MultiTileEntityMultiBlockPart.ONLY_ITEM_FLUID_ENERGY, aCoordinates, aPlayer, aInventory)) tSuccess = F;
			if (!ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX  , tY+3, tZ+2, 18031, getMultiTileEntityRegistryID(), 0, MultiTileEntityMultiBlockPart.ONLY_ITEM_FLUID_ENERGY, aCoordinates, aPlayer, aInventory)) tSuccess = F;
			if (!ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX+1, tY+3, tZ+2, 18044, getMultiTileEntityRegistryID(), 0, MultiTileEntityMultiBlockPart.ONLY_ITEM_FLUID_ENERGY, aCoordinates, aPlayer, aInventory)) tSuccess = F;
			if (!ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX+2, tY+3, tZ+2, 18044, getMultiTileEntityRegistryID(), 0, MultiTileEntityMultiBlockPart.ONLY_ITEM_FLUID_ENERGY, aCoordinates, aPlayer, aInventory)) tSuccess = F;
			if (!ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX+3, tY+3, tZ+2, 18044, getMultiTileEntityRegistryID(), 0, MultiTileEntityMultiBlockPart.ONLY_ITEM_FLUID_ENERGY, aCoordinates, aPlayer, aInventory)) tSuccess = F;
			if (!ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX+4, tY+3, tZ+2, 18031, getMultiTileEntityRegistryID(), 0, MultiTileEntityMultiBlockPart.ONLY_ITEM_FLUID_ENERGY, aCoordinates, aPlayer, aInventory)) tSuccess = F;
			if (!ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX  , tY+3, tZ+3, 18031, getMultiTileEntityRegistryID(), 0, MultiTileEntityMultiBlockPart.ONLY_ITEM_FLUID_ENERGY, aCoordinates, aPlayer, aInventory)) tSuccess = F;
			if (!ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX+1, tY+3, tZ+3, 18044, getMultiTileEntityRegistryID(), 0, MultiTileEntityMultiBlockPart.ONLY_ITEM_FLUID_ENERGY, aCoordinates, aPlayer, aInventory)) tSuccess = F;
			if (!ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX+2, tY+3, tZ+3, 18044, getMultiTileEntityRegistryID(), 0, MultiTileEntityMultiBlockPart.ONLY_ITEM_FLUID_ENERGY, aCoordinates, aPlayer, aInventory)) tSuccess = F;
			if (!ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX+3, tY+3, tZ+3, 18044, getMultiTileEntityRegistryID(), 0, MultiTileEntityMultiBlockPart.ONLY_ITEM_FLUID_ENERGY, aCoordinates, aPlayer, aInventory)) tSuccess = F;
			if (!ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX+4, tY+3, tZ+3, 18031, getMultiTileEntityRegistryID(), 0, MultiTileEntityMultiBlockPart.ONLY_ITEM_FLUID_ENERGY, aCoordinates, aPlayer, aInventory)) tSuccess = F;
			if (!ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX  , tY+3, tZ+4, 18031, getMultiTileEntityRegistryID(), 0, MultiTileEntityMultiBlockPart.ONLY_ITEM_FLUID_ENERGY, aCoordinates, aPlayer, aInventory)) tSuccess = F;
			if (!ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX+1, tY+3, tZ+4, 18031, getMultiTileEntityRegistryID(), 0, MultiTileEntityMultiBlockPart.ONLY_ITEM_FLUID_ENERGY, aCoordinates, aPlayer, aInventory)) tSuccess = F;
			if (!ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX+2, tY+3, tZ+4, 18031, getMultiTileEntityRegistryID(), 0, MultiTileEntityMultiBlockPart.ONLY_ITEM_FLUID_ENERGY, aCoordinates, aPlayer, aInventory)) tSuccess = F;
			if (!ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX+3, tY+3, tZ+4, 18031, getMultiTileEntityRegistryID(), 0, MultiTileEntityMultiBlockPart.ONLY_ITEM_FLUID_ENERGY, aCoordinates, aPlayer, aInventory)) tSuccess = F;
			if (!ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX+4, tY+3, tZ+4, 18031, getMultiTileEntityRegistryID(), 0, MultiTileEntityMultiBlockPart.ONLY_ITEM_FLUID_ENERGY, aCoordinates, aPlayer, aInventory)) tSuccess = F;
			
			if (!ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX  , tY+4, tZ  , 18031, getMultiTileEntityRegistryID(), 0, MultiTileEntityMultiBlockPart.ONLY_ITEM_FLUID_ENERGY, aCoordinates, aPlayer, aInventory)) tSuccess = F;
			if (!ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX+1, tY+4, tZ  , 18031, getMultiTileEntityRegistryID(), 0, MultiTileEntityMultiBlockPart.ONLY_ITEM_FLUID_ENERGY, aCoordinates, aPlayer, aInventory)) tSuccess = F;
			if (!ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX+2, tY+4, tZ  , 18031, getMultiTileEntityRegistryID(), 0, MultiTileEntityMultiBlockPart.ONLY_ITEM_FLUID_ENERGY, aCoordinates, aPlayer, aInventory)) tSuccess = F;
			if (!ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX+3, tY+4, tZ  , 18031, getMultiTileEntityRegistryID(), 0, MultiTileEntityMultiBlockPart.ONLY_ITEM_FLUID_ENERGY, aCoordinates, aPlayer, aInventory)) tSuccess = F;
			if (!ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX+4, tY+4, tZ  , 18031, getMultiTileEntityRegistryID(), 0, MultiTileEntityMultiBlockPart.ONLY_ITEM_FLUID_ENERGY, aCoordinates, aPlayer, aInventory)) tSuccess = F;
			if (!ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX  , tY+4, tZ+1, 18031, getMultiTileEntityRegistryID(), 0, MultiTileEntityMultiBlockPart.ONLY_ITEM_FLUID_ENERGY, aCoordinates, aPlayer, aInventory)) tSuccess = F;
			if (!ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX+1, tY+4, tZ+1, 18031, getMultiTileEntityRegistryID(), 0, MultiTileEntityMultiBlockPart.ONLY_ITEM_FLUID_ENERGY, aCoordinates, aPlayer, aInventory)) tSuccess = F;
			if (!ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX+2, tY+4, tZ+1, 18031, getMultiTileEntityRegistryID(), 0, MultiTileEntityMultiBlockPart.ONLY_ITEM_FLUID_ENERGY, aCoordinates, aPlayer, aInventory)) tSuccess = F;
			if (!ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX+3, tY+4, tZ+1, 18031, getMultiTileEntityRegistryID(), 0, MultiTileEntityMultiBlockPart.ONLY_ITEM_FLUID_ENERGY, aCoordinates, aPlayer, aInventory)) tSuccess = F;
			if (!ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX+4, tY+4, tZ+1, 18031, getMultiTileEntityRegistryID(), 0, MultiTileEntityMultiBlockPart.ONLY_ITEM_FLUID_ENERGY, aCoordinates, aPlayer, aInventory)) tSuccess = F;
			if (!ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX  , tY+4, tZ+2, 18031, getMultiTileEntityRegistryID(), 0, MultiTileEntityMultiBlockPart.ONLY_ITEM_FLUID_ENERGY, aCoordinates, aPlayer, aInventory)) tSuccess = F;
			if (!ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX+1, tY+4, tZ+2, 18031, getMultiTileEntityRegistryID(), 0, MultiTileEntityMultiBlockPart.ONLY_ITEM_FLUID_ENERGY, aCoordinates, aPlayer, aInventory)) tSuccess = F;
			if (!ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX+2, tY+4, tZ+2, 18031, getMultiTileEntityRegistryID(), 0, MultiTileEntityMultiBlockPart.ONLY_ITEM_FLUID_ENERGY, aCoordinates, aPlayer, aInventory)) tSuccess = F;
			if (!ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX+3, tY+4, tZ+2, 18031, getMultiTileEntityRegistryID(), 0, MultiTileEntityMultiBlockPart.ONLY_ITEM_FLUID_ENERGY, aCoordinates, aPlayer, aInventory)) tSuccess = F;
			if (!ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX+4, tY+4, tZ+2, 18031, getMultiTileEntityRegistryID(), 0, MultiTileEntityMultiBlockPart.ONLY_ITEM_FLUID_ENERGY, aCoordinates, aPlayer, aInventory)) tSuccess = F;
			if (!ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX  , tY+4, tZ+3, 18031, getMultiTileEntityRegistryID(), 0, MultiTileEntityMultiBlockPart.ONLY_ITEM_FLUID_ENERGY, aCoordinates, aPlayer, aInventory)) tSuccess = F;
			if (!ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX+1, tY+4, tZ+3, 18031, getMultiTileEntityRegistryID(), 0, MultiTileEntityMultiBlockPart.ONLY_ITEM_FLUID_ENERGY, aCoordinates, aPlayer, aInventory)) tSuccess = F;
			if (!ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX+2, tY+4, tZ+3, 18031, getMultiTileEntityRegistryID(), 0, MultiTileEntityMultiBlockPart.ONLY_ITEM_FLUID_ENERGY, aCoordinates, aPlayer, aInventory)) tSuccess = F;
			if (!ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX+3, tY+4, tZ+3, 18031, getMultiTileEntityRegistryID(), 0, MultiTileEntityMultiBlockPart.ONLY_ITEM_FLUID_ENERGY, aCoordinates, aPlayer, aInventory)) tSuccess = F;
			if (!ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX+4, tY+4, tZ+3, 18031, getMultiTileEntityRegistryID(), 0, MultiTileEntityMultiBlockPart.ONLY_ITEM_FLUID_ENERGY, aCoordinates, aPlayer, aInventory)) tSuccess = F;
			if (!ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX  , tY+4, tZ+4, 18031, getMultiTileEntityRegistryID(), 0, MultiTileEntityMultiBlockPart.ONLY_ITEM_FLUID_ENERGY, aCoordinates, aPlayer, aInventory)) tSuccess = F;
			if (!ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX+1, tY+4, tZ+4, 18031, getMultiTileEntityRegistryID(), 0, MultiTileEntityMultiBlockPart.ONLY_ITEM_FLUID_ENERGY, aCoordinates, aPlayer, aInventory)) tSuccess = F;
			if (!ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX+2, tY+4, tZ+4, 18031, getMultiTileEntityRegistryID(), 0, MultiTileEntityMultiBlockPart.ONLY_ITEM_FLUID_ENERGY, aCoordinates, aPlayer, aInventory)) tSuccess = F;
			if (!ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX+3, tY+4, tZ+4, 18031, getMultiTileEntityRegistryID(), 0, MultiTileEntityMultiBlockPart.ONLY_ITEM_FLUID_ENERGY, aCoordinates, aPlayer, aInventory)) tSuccess = F;
			if (!ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX+4, tY+4, tZ+4, 18031, getMultiTileEntityRegistryID(), 0, MultiTileEntityMultiBlockPart.ONLY_ITEM_FLUID_ENERGY, aCoordinates, aPlayer, aInventory)) tSuccess = F;
			
			if (!ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX  , tY+5, tZ  , 18299, getMultiTileEntityRegistryID(), 0, MultiTileEntityMultiBlockPart.NOTHING, aCoordinates, aPlayer, aInventory)) tSuccess = F;
			if (!ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX+1, tY+5, tZ  , 18299, getMultiTileEntityRegistryID(), 0, MultiTileEntityMultiBlockPart.NOTHING, aCoordinates, aPlayer, aInventory)) tSuccess = F;
			if (!ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX+2, tY+5, tZ  , 18299, getMultiTileEntityRegistryID(), 0, MultiTileEntityMultiBlockPart.NOTHING, aCoordinates, aPlayer, aInventory)) tSuccess = F;
			if (!ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX+3, tY+5, tZ  , 18299, getMultiTileEntityRegistryID(), 0, MultiTileEntityMultiBlockPart.NOTHING, aCoordinates, aPlayer, aInventory)) tSuccess = F;
			if (!ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX+4, tY+5, tZ  , 18299, getMultiTileEntityRegistryID(), 0, MultiTileEntityMultiBlockPart.NOTHING, aCoordinates, aPlayer, aInventory)) tSuccess = F;
			if (!ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX  , tY+5, tZ+1, 18299, getMultiTileEntityRegistryID(), 0, MultiTileEntityMultiBlockPart.NOTHING, aCoordinates, aPlayer, aInventory)) tSuccess = F;
			if (!ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX+4, tY+5, tZ+1, 18299, getMultiTileEntityRegistryID(), 0, MultiTileEntityMultiBlockPart.NOTHING, aCoordinates, aPlayer, aInventory)) tSuccess = F;
			if (!ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX  , tY+5, tZ+2, 18299, getMultiTileEntityRegistryID(), 0, MultiTileEntityMultiBlockPart.NOTHING, aCoordinates, aPlayer, aInventory)) tSuccess = F;
			if (!ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX+4, tY+5, tZ+2, 18299, getMultiTileEntityRegistryID(), 0, MultiTileEntityMultiBlockPart.NOTHING, aCoordinates, aPlayer, aInventory)) tSuccess = F;
			if (!ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX  , tY+5, tZ+3, 18299, getMultiTileEntityRegistryID(), 0, MultiTileEntityMultiBlockPart.NOTHING, aCoordinates, aPlayer, aInventory)) tSuccess = F;
			if (!ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX+4, tY+5, tZ+3, 18299, getMultiTileEntityRegistryID(), 0, MultiTileEntityMultiBlockPart.NOTHING, aCoordinates, aPlayer, aInventory)) tSuccess = F;
			if (!ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX  , tY+5, tZ+4, 18299, getMultiTileEntityRegistryID(), 0, MultiTileEntityMultiBlockPart.NOTHING, aCoordinates, aPlayer, aInventory)) tSuccess = F;
			if (!ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX+1, tY+5, tZ+4, 18299, getMultiTileEntityRegistryID(), 0, MultiTileEntityMultiBlockPart.NOTHING, aCoordinates, aPlayer, aInventory)) tSuccess = F;
			if (!ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX+2, tY+5, tZ+4, 18299, getMultiTileEntityRegistryID(), 0, MultiTileEntityMultiBlockPart.NOTHING, aCoordinates, aPlayer, aInventory)) tSuccess = F;
			if (!ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX+3, tY+5, tZ+4, 18299, getMultiTileEntityRegistryID(), 0, MultiTileEntityMultiBlockPart.NOTHING, aCoordinates, aPlayer, aInventory)) tSuccess = F;
			if (!ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX+4, tY+5, tZ+4, 18299, getMultiTileEntityRegistryID(), 0, MultiTileEntityMultiBlockPart.NOTHING, aCoordinates, aPlayer, aInventory)) tSuccess = F;
			
			if (!ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX+2, tY+5, tZ+2, 18200, getMultiTileEntityRegistryID(), 0, MultiTileEntityMultiBlockPart.NOTHING, aCoordinates, aPlayer, aInventory)) tSuccess = F;
			
			int tCountA = 0, tCountB = 0;
			
			if ( ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX+1, tY+5, tZ+1, 18202, getMultiTileEntityRegistryID(), 0, MultiTileEntityMultiBlockPart.NOTHING, aCoordinates, aPlayer, aInventory)) tCountA++;
			if ( ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX+2, tY+5, tZ+1, 18202, getMultiTileEntityRegistryID(), 0, MultiTileEntityMultiBlockPart.NOTHING, aCoordinates, aPlayer, aInventory)) tCountA++;
			if ( ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX+3, tY+5, tZ+1, 18202, getMultiTileEntityRegistryID(), 0, MultiTileEntityMultiBlockPart.NOTHING, aCoordinates, aPlayer, aInventory)) tCountA++;
			if ( ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX+1, tY+5, tZ+2, 18202, getMultiTileEntityRegistryID(), 0, MultiTileEntityMultiBlockPart.NOTHING, aCoordinates, aPlayer, aInventory)) tCountA++;
			if ( ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX+3, tY+5, tZ+2, 18202, getMultiTileEntityRegistryID(), 0, MultiTileEntityMultiBlockPart.NOTHING, aCoordinates, aPlayer, aInventory)) tCountA++;
			if ( ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX+1, tY+5, tZ+3, 18202, getMultiTileEntityRegistryID(), 0, MultiTileEntityMultiBlockPart.NOTHING, aCoordinates, aPlayer, aInventory)) tCountA++;
			if ( ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX+2, tY+5, tZ+3, 18202, getMultiTileEntityRegistryID(), 0, MultiTileEntityMultiBlockPart.NOTHING, aCoordinates, aPlayer, aInventory)) tCountA++;
			if ( ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX+3, tY+5, tZ+3, 18202, getMultiTileEntityRegistryID(), 0, MultiTileEntityMultiBlockPart.NOTHING, aCoordinates, aPlayer, aInventory)) tCountA++;
			
			if ( ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX+1, tY+5, tZ+1, 18204, getMultiTileEntityRegistryID(), 0, MultiTileEntityMultiBlockPart.NOTHING, aCoordinates, aPlayer, aInventory)) tCountB++;
			if ( ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX+2, tY+5, tZ+1, 18204, getMultiTileEntityRegistryID(), 0, MultiTileEntityMultiBlockPart.NOTHING, aCoordinates, aPlayer, aInventory)) tCountB++;
			if ( ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX+3, tY+5, tZ+1, 18204, getMultiTileEntityRegistryID(), 0, MultiTileEntityMultiBlockPart.NOTHING, aCoordinates, aPlayer, aInventory)) tCountB++;
			if ( ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX+1, tY+5, tZ+2, 18204, getMultiTileEntityRegistryID(), 0, MultiTileEntityMultiBlockPart.NOTHING, aCoordinates, aPlayer, aInventory)) tCountB++;
			if ( ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX+3, tY+5, tZ+2, 18204, getMultiTileEntityRegistryID(), 0, MultiTileEntityMultiBlockPart.NOTHING, aCoordinates, aPlayer, aInventory)) tCountB++;
			if ( ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX+1, tY+5, tZ+3, 18204, getMultiTileEntityRegistryID(), 0, MultiTileEntityMultiBlockPart.NOTHING, aCoordinates, aPlayer, aInventory)) tCountB++;
			if ( ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX+2, tY+5, tZ+3, 18204, getMultiTileEntityRegistryID(), 0, MultiTileEntityMultiBlockPart.NOTHING, aCoordinates, aPlayer, aInventory)) tCountB++;
			if ( ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX+3, tY+5, tZ+3, 18204, getMultiTileEntityRegistryID(), 0, MultiTileEntityMultiBlockPart.NOTHING, aCoordinates, aPlayer, aInventory)) tCountB++;
			
			if (tCountA < 4 || tCountB < 4) tSuccess = F;
			
			return tSuccess;
		}
		return mStructureOkay;
	}
	
	static {
		LH.add("gt.tooltip.multiblock.matterfabricator.1", "5x5x5 'Hollow' of 97 Dense Lead Walls");
		LH.add("gt.tooltip.multiblock.matterfabricator.2", "3x3x3 'Hollow' with 26 Osmium Coils inside the 5x5x5");
		LH.add("gt.tooltip.multiblock.matterfabricator.3", "5x5 Ring of 16 Ventilation Units ontop");
		LH.add("gt.tooltip.multiblock.matterfabricator.4", "4 Control Quadcore Processor Units in that Ring");
		LH.add("gt.tooltip.multiblock.matterfabricator.5", "4 Conversion Quadcore Processor Units in that Ring");
		LH.add("gt.tooltip.multiblock.matterfabricator.6", "1 Versatile Quadcore Processor Unit in the center of that Ring");
		LH.add("gt.tooltip.multiblock.matterfabricator.7", "Main Block centered on Side-Bottom and facing outwards");
		LH.add("gt.tooltip.multiblock.matterfabricator.8", "Stuff can go in and out on any of the Dense Lead Walls");
	}
	
	@Override
	public void addToolTips(List<String> aList, ItemStack aStack, boolean aF3_H) {
		aList.add(Chat.CYAN  + LH.get(LH.STRUCTURE) + ":");
		aList.add(Chat.WHITE + LH.get("gt.tooltip.multiblock.matterfabricator.1"));
		aList.add(Chat.WHITE + LH.get("gt.tooltip.multiblock.matterfabricator.2"));
		aList.add(Chat.WHITE + LH.get("gt.tooltip.multiblock.matterfabricator.3"));
		aList.add(Chat.WHITE + LH.get("gt.tooltip.multiblock.matterfabricator.4"));
		aList.add(Chat.WHITE + LH.get("gt.tooltip.multiblock.matterfabricator.5"));
		aList.add(Chat.WHITE + LH.get("gt.tooltip.multiblock.matterfabricator.6"));
		aList.add(Chat.WHITE + LH.get("gt.tooltip.multiblock.matterfabricator.7"));
		aList.add(Chat.WHITE + LH.get("gt.tooltip.multiblock.matterfabricator.8"));
		super.addToolTips(aList, aStack, aF3_H);
	}
	
	@Override
	public boolean isInsideStructure(int aX, int aY, int aZ) {
		int tX = getOffsetXN(mFacing, 2), tY = getBlockPos().getY(), tZ = getOffsetZN(mFacing, 2);
		return aX >= tX - 2 && aY >= tY && aZ >= tZ - 2 && aX <= tX + 2 && aY <= tY + 6 && aZ <= tZ + 2;
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
	
	@Override public String getTileEntityName() {return "gt.multitileentity.multiblock.matterfabricator";}
}

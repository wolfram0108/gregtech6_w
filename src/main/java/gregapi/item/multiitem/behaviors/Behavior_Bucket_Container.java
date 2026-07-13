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

package gregapi.item.multiitem.behaviors;

import static gregapi.data.CS.*;

import gregapi.data.CS.BlocksGT;
import gregapi.data.FL;
import gregapi.item.multiitem.MultiItem;
import gregapi.item.multiitem.behaviors.IBehavior.AbstractBehaviorDefault;
import gregapi.util.WD;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidBlock;

public class Behavior_Bucket_Container extends AbstractBehaviorDefault {
	public static final IBehavior<MultiItem> INSTANCE = new Behavior_Bucket_Container();
	
	@Override
	public ItemStack onItemRightClick(MultiItem aItem, ItemStack aStack, Level aWorld, Player aPlayer) {
		HitResult tPosition = WD.getMOP(aWorld, aPlayer, T);
		if (tPosition == null || tPosition.typeOfHit != HitResult.MovingObjectType.BLOCK) return aStack;
		if (!aWorld.canMineBlock(aPlayer, tPosition.getBlockPos().getX(), tPosition.getBlockPos().getY(), tPosition.getBlockPos().getZ())) return aStack;
		
		Block tBlock = WD.block(aWorld, tPosition.getBlockPos().getX(), tPosition.getBlockPos().getY(), tPosition.getBlockPos().getZ());
		if (tBlock == Blocks.water || tBlock == Blocks.flowing_water) {
			if (WD.meta(aWorld, tPosition.getBlockPos().getX(), tPosition.getBlockPos().getY(), tPosition.getBlockPos().getZ()) == 0 && aItem.fill(aStack, FL.Water.make(1000), F) == 1000) {
				WD.set(aWorld, tPosition.getBlockPos().getX(), tPosition.getBlockPos().getY(), tPosition.getBlockPos().getZ(), NB, 0, 3);
				aItem.fill(aStack, FL.Water.make(1000), T);
			}
			return aStack;
		}
		if (tBlock == Blocks.lava || tBlock == Blocks.flowing_lava) {
			if (WD.meta(aWorld, tPosition.getBlockPos().getX(), tPosition.getBlockPos().getY(), tPosition.getBlockPos().getZ()) == 0 && aItem.fill(aStack, FL.Lava.make(1000), F) == 1000) {
				WD.set(aWorld, tPosition.getBlockPos().getX(), tPosition.getBlockPos().getY(), tPosition.getBlockPos().getZ(), NB, 0, 3);
				aItem.fill(aStack, FL.Lava.make(1000), T);
			}
			return aStack;
		}
		if (tBlock == BlocksGT.River) {
			aItem.fill(aStack, FL.Water.make(1000), T);
			return aStack;
		}
		if (tBlock == BlocksGT.Ocean) {
			aItem.fill(aStack, FL.Ocean.make(1000), T);
			return aStack;
		}
		if (tBlock == BlocksGT.Swamp) {
			aItem.fill(aStack, FL.Dirty_Water.make(1000), T);
			return aStack;
		}
		if (tBlock instanceof IFluidBlock) {
			FluidStack tDrained = ((IFluidBlock)tBlock).drain(aWorld, tPosition.getBlockPos().getX(), tPosition.getBlockPos().getY(), tPosition.getBlockPos().getZ(), F);
			if (tDrained != null && tDrained.getAmount() > 0 && aItem.fill(aStack, tDrained, F) == tDrained.getAmount()) {
				// Forge fucked up the Fluid Draining Function, meaning if you insert true for doDrain it will ALWAYS return a null Fluid for the finite Fluid Blocks. That's why I take the result from the simulation instead of the actual draining.
				aItem.fill(aStack, tDrained, T);
				((IFluidBlock)tBlock).drain(aWorld, tPosition.getBlockPos().getX(), tPosition.getBlockPos().getY(), tPosition.getBlockPos().getZ(), T);
			}
			return aStack;
		}
		
		tPosition.getBlockPos().getX()+=OFFX[tPosition.sideHit];
		tPosition.getBlockPos().getY()+=OFFY[tPosition.sideHit];
		tPosition.getBlockPos().getZ()+=OFFZ[tPosition.sideHit];
		tBlock = WD.block(aWorld, tPosition.getBlockPos().getX(), tPosition.getBlockPos().getY(), tPosition.getBlockPos().getZ());
		
		if (tBlock instanceof IFluidBlock) {
			FluidStack tDrained = ((IFluidBlock)tBlock).drain(aWorld, tPosition.getBlockPos().getX(), tPosition.getBlockPos().getY(), tPosition.getBlockPos().getZ(), F);
			if (tDrained != null && tDrained.getAmount() > 0 && aItem.fill(aStack, tDrained, F) == tDrained.getAmount()) {
				// Forge fucked up the Fluid Draining Function, meaning if you insert true for doDrain it will ALWAYS return a null Fluid for the finite Fluid Blocks. That's why I take the result from the simulation instead of the actual draining.
				aItem.fill(aStack, tDrained, T);
				((IFluidBlock)tBlock).drain(aWorld, tPosition.getBlockPos().getX(), tPosition.getBlockPos().getY(), tPosition.getBlockPos().getZ(), T);
			}
			return aStack;
		}
		
		return aStack;
	}
}

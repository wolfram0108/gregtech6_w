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
import net.minecraft.world.phys.BlockHitResult;

import net.minecraft.core.BlockPos;
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
		if (tPosition == null || tPosition.getType() != HitResult.Type.BLOCK) return aStack;
		if (!aWorld.mayInteract(aPlayer, ((BlockHitResult)tPosition).getBlockPos())) return aStack; // F-item-use: 1.7.10 World.canMineBlock(player,x,y,z) -> neo Level.mayInteract(Entity,BlockPos) (Level.java:887).
		
		Block tBlock = WD.block(aWorld, ((BlockHitResult)tPosition).getBlockPos().getX(), ((BlockHitResult)tPosition).getBlockPos().getY(), ((BlockHitResult)tPosition).getBlockPos().getZ());
		if (tBlock == Blocks.WATER || tBlock == Blocks.WATER) {
			if (WD.meta(aWorld, ((BlockHitResult)tPosition).getBlockPos().getX(), ((BlockHitResult)tPosition).getBlockPos().getY(), ((BlockHitResult)tPosition).getBlockPos().getZ()) == 0 && aItem.fill(aStack, FL.Water.make(1000), F) == 1000) {
				WD.set(aWorld, ((BlockHitResult)tPosition).getBlockPos().getX(), ((BlockHitResult)tPosition).getBlockPos().getY(), ((BlockHitResult)tPosition).getBlockPos().getZ(), NB, 0, 3);
				aItem.fill(aStack, FL.Water.make(1000), T);
			}
			return aStack;
		}
		if (tBlock == Blocks.LAVA || tBlock == Blocks.LAVA) {
			if (WD.meta(aWorld, ((BlockHitResult)tPosition).getBlockPos().getX(), ((BlockHitResult)tPosition).getBlockPos().getY(), ((BlockHitResult)tPosition).getBlockPos().getZ()) == 0 && aItem.fill(aStack, FL.Lava.make(1000), F) == 1000) {
				WD.set(aWorld, ((BlockHitResult)tPosition).getBlockPos().getX(), ((BlockHitResult)tPosition).getBlockPos().getY(), ((BlockHitResult)tPosition).getBlockPos().getZ(), NB, 0, 3);
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
			FluidStack tDrained = ((IFluidBlock)tBlock).drain(aWorld, ((BlockHitResult)tPosition).getBlockPos().getX(), ((BlockHitResult)tPosition).getBlockPos().getY(), ((BlockHitResult)tPosition).getBlockPos().getZ(), F);
			if (tDrained != null && tDrained.getAmount() > 0 && aItem.fill(aStack, tDrained, F) == tDrained.getAmount()) {
				// Forge fucked up the Fluid Draining Function, meaning if you insert true for doDrain it will ALWAYS return a null Fluid for the finite Fluid Blocks. That's why I take the result from the simulation instead of the actual draining.
				aItem.fill(aStack, tDrained, T);
				((IFluidBlock)tBlock).drain(aWorld, ((BlockHitResult)tPosition).getBlockPos().getX(), ((BlockHitResult)tPosition).getBlockPos().getY(), ((BlockHitResult)tPosition).getBlockPos().getZ(), T);
			}
			return aStack;
		}
		
		// было tPosition.blockX/Y/Z += OFFX/Y/Z[sideHit] (сдвиг на соседний блок по стороне удара); neo BlockPos immutable -> переприсвоить BlockHitResult на relative(getDirection())
		tPosition = new BlockHitResult(tPosition.getLocation(), ((BlockHitResult)tPosition).getDirection(), ((BlockHitResult)tPosition).getBlockPos().relative(((BlockHitResult)tPosition).getDirection()), ((BlockHitResult)tPosition).isInside());
		tBlock = WD.block(aWorld, ((BlockHitResult)tPosition).getBlockPos().getX(), ((BlockHitResult)tPosition).getBlockPos().getY(), ((BlockHitResult)tPosition).getBlockPos().getZ());
		
		if (tBlock instanceof IFluidBlock) {
			FluidStack tDrained = ((IFluidBlock)tBlock).drain(aWorld, ((BlockHitResult)tPosition).getBlockPos().getX(), ((BlockHitResult)tPosition).getBlockPos().getY(), ((BlockHitResult)tPosition).getBlockPos().getZ(), F);
			if (tDrained != null && tDrained.getAmount() > 0 && aItem.fill(aStack, tDrained, F) == tDrained.getAmount()) {
				// Forge fucked up the Fluid Draining Function, meaning if you insert true for doDrain it will ALWAYS return a null Fluid for the finite Fluid Blocks. That's why I take the result from the simulation instead of the actual draining.
				aItem.fill(aStack, tDrained, T);
				((IFluidBlock)tBlock).drain(aWorld, ((BlockHitResult)tPosition).getBlockPos().getX(), ((BlockHitResult)tPosition).getBlockPos().getY(), ((BlockHitResult)tPosition).getBlockPos().getZ(), T);
			}
			return aStack;
		}
		
		return aStack;
	}
}

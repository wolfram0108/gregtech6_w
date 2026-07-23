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
 */

package gregapi.item;

import static gregapi.data.CS.*;

import gregapi.data.CS.FluidsGT;
import gregapi.data.FL;
import gregapi.data.IL;
import gregapi.util.ST;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidContainerItem;

/**
 * @author Gregorius Techneticies
 */
public interface IItemRottable {
	public ItemStack getRotten(ItemStack aStack);
	public ItemStack getRotten(ItemStack aStack, Level aWorld, int aX, int aY, int aZ);
	
	public static class RottingUtil {
		public static ItemStack rotting(ItemStack aStack) {
			if (aStack.getItem() == Items.MILK_BUCKET) return IL.ENVM_Spoiled_Milk_Bucket.exists()?IL.ENVM_Spoiled_Milk_Bucket.get(aStack.getCount()):ST.make(Items.BUCKET, aStack.getCount(), 0);
			if (aStack.getItem() instanceof IItemRottable) return ((IItemRottable)aStack.getItem()).getRotten(aStack);
			if (aStack.getItem() instanceof IFluidContainerItem) return rotting(aStack, (IFluidContainerItem)aStack.getItem());
			return aStack;
		}

		public static ItemStack rotting(ItemStack aStack, Level aWorld, int aX, int aY, int aZ) {
			if (aStack.getItem() == Items.MILK_BUCKET) return IL.ENVM_Spoiled_Milk_Bucket.exists()?IL.ENVM_Spoiled_Milk_Bucket.get(aStack.getCount()):ST.make(Items.BUCKET, aStack.getCount(), 0);
			if (aStack.getItem() instanceof IItemRottable) return ((IItemRottable)aStack.getItem()).getRotten(aStack, aWorld, aX, aY, aZ);
			if (aStack.getItem() instanceof IFluidContainerItem) return rotting(aStack, (IFluidContainerItem)aStack.getItem());
			return aStack;
		}

		// F5/BUG-045: восстановлено 1:1 с оригиналом (:56-73) на живом compat-mirror IFluidContainerItem
		// (ItemStack-arg методы, null-семантика пустоты) — per-stack мутации идут в NBT самого aStack.
		public static ItemStack rotting(ItemStack aStack, IFluidContainerItem aItem) {
			FluidStack tFluid = aItem.getFluid(aStack);
			if (tFluid != null) {
				if (FL.Milk_Spoiled.is(tFluid) || FL.Rotten_Drink.is(tFluid) || FL.Dirty_Water.is(tFluid) || FL.Swampwater.is(tFluid) || FL.Stagnant_Water.is(tFluid)) {
					//
				} else if (FluidsGT.WATER.contains(FL.regName(tFluid.getFluid()))) {
					aItem.drain(aStack, Integer.MAX_VALUE, T);
					aItem.fill(aStack, FL.Dirty_Water.make(tFluid.getAmount()), T);
				} else if (FluidsGT.MILK.contains(FL.regName(tFluid.getFluid()))) {
					aItem.drain(aStack, Integer.MAX_VALUE, T);
					aItem.fill(aStack, FL.Milk_Spoiled.make(tFluid.getAmount()), T);
				} else if (FluidsGT.FOOD.contains(FL.regName(tFluid.getFluid()))) {
					aItem.drain(aStack, Integer.MAX_VALUE, T);
					aItem.fill(aStack, FL.Rotten_Drink.make(tFluid.getAmount()), T);
				}
			}
			return aStack;
		}
	}
}

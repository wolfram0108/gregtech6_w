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
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;

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
			if (aStack.getItem() instanceof IFluidHandlerItem) return rotting(aStack, (IFluidHandlerItem)aStack.getItem());
			return aStack;
		}
		
		public static ItemStack rotting(ItemStack aStack, Level aWorld, int aX, int aY, int aZ) {
			if (aStack.getItem() == Items.MILK_BUCKET) return IL.ENVM_Spoiled_Milk_Bucket.exists()?IL.ENVM_Spoiled_Milk_Bucket.get(aStack.getCount()):ST.make(Items.BUCKET, aStack.getCount(), 0);
			if (aStack.getItem() instanceof IItemRottable) return ((IItemRottable)aStack.getItem()).getRotten(aStack, aWorld, aX, aY, aZ);
			if (aStack.getItem() instanceof IFluidHandlerItem) return rotting(aStack, (IFluidHandlerItem)aStack.getItem());
			return aStack;
		}
		
		public static ItemStack rotting(ItemStack aStack, IFluidHandlerItem aItem) {
			// F5: neo IFluidHandlerItem (extends IFluidHandler) — getFluid(ItemStack) удалён -> getFluidInTank(0)
			// (IFluidHandler.java:81, ёмкость item-обёртки = танк 0). F15: getFluidInTank даёт FluidStack.EMPTY,
			// не null (1.7.10 getFluid отдавал null) -> проверка !isEmpty(), НЕ !=null (иначе всегда true).
			// F5 functional-adapted (getFluidInTank-обёртка, runtime-привязка к aStack — паритет-каверз, компилятор не судит): aItem здесь — каст Item, не stack-bound capability-обёртка
			// (aStack.getCapability(Capabilities.FluidHandler.ITEM)); мутации drain/fill применяются к обёртке —
			// runtime-привязка к aStack требует паритет-проверки (компилятор это не судит).
			FluidStack tFluid = aItem.getFluidInTank(0);
			if (!tFluid.isEmpty()) {
				if (FL.Milk_Spoiled.is(tFluid) || FL.Rotten_Drink.is(tFluid) || FL.Dirty_Water.is(tFluid) || FL.Swampwater.is(tFluid) || FL.Stagnant_Water.is(tFluid)) {
					//
				} else if (FluidsGT.WATER.contains(FL.regName(tFluid.getFluid()))) {
					aItem.drain(Integer.MAX_VALUE, FluidAction.EXECUTE);
					aItem.fill(FL.Dirty_Water.make(tFluid.getAmount()), FluidAction.EXECUTE);
				} else if (FluidsGT.MILK.contains(FL.regName(tFluid.getFluid()))) {
					aItem.drain(Integer.MAX_VALUE, FluidAction.EXECUTE);
					aItem.fill(FL.Milk_Spoiled.make(tFluid.getAmount()), FluidAction.EXECUTE);
				} else if (FluidsGT.FOOD.contains(FL.regName(tFluid.getFluid()))) {
					aItem.drain(Integer.MAX_VALUE, FluidAction.EXECUTE);
					aItem.fill(FL.Rotten_Drink.make(tFluid.getAmount()), FluidAction.EXECUTE);
				}
			}
			return aStack;
		}
	}
}

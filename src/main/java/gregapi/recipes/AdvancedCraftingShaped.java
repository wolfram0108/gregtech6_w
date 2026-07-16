/**
 * Copyright (c) 2023 GregTech-6 Team
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

package gregapi.recipes;
import gregapi.code.ItemNBT;

import gregapi.code.TagData;
import gregapi.item.IItemEnergy;
import gregapi.item.IItemGTContainerTool;
import gregapi.item.multiitem.MultiItemTool;
import gregapi.util.ST;
import gregapi.util.UT;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import gregapi.recipes.ShapedOreRecipe;

import static gregapi.data.CS.*;

/**
 * @author Gregorius Techneticies
 */
public class AdvancedCraftingShaped extends ShapedOreRecipe implements ICraftingRecipeGT {
	public final boolean mDismantleable, mRemovableByGT, mAutoCraftable, mKeepingNBT;
	private final net.minecraft.resources.ResourceKey<net.minecraft.world.item.enchantment.Enchantment>[] mEnchantmentsAdded;
	private final int[] mEnchantmentLevelsAdded;
	
	public AdvancedCraftingShaped(ItemStack aResult, boolean aDismantleAble, boolean aRemovableByGT, boolean aKeepingNBT, boolean aAutoCraftable, net.minecraft.resources.ResourceKey<net.minecraft.world.item.enchantment.Enchantment>[] aEnchantmentsAdded, int[] aEnchantmentLevelsAdded, Object... aRecipe) {
		super(aResult, aRecipe);
		mEnchantmentsAdded = aEnchantmentsAdded;
		mEnchantmentLevelsAdded = aEnchantmentLevelsAdded;
		mRemovableByGT = aRemovableByGT;
		mKeepingNBT = aKeepingNBT;
		mDismantleable = aDismantleAble;
		mAutoCraftable = aAutoCraftable;
	}
	
	@Override
	public boolean matches(CraftingInput aGrid, Level aWorld) {
		if (mKeepingNBT) {
			ItemStack tStack = null;
			// F11: Forge InventoryCrafting.getSizeInventory()/getStackInSlot(i) удалены; neo-эквивалент —
			// CraftingInput.size()/getItem(i). getItem(i) никогда не null (пустой слот = ItemStack.EMPTY), поэтому
			// занятость слота проверяется ST.valid(...), а не сравнением с null (CraftingInput.java:85-96).
			for (int i = 0; i < aGrid.size(); i++) {
				ItemStack tSlot = aGrid.getItem(i);
				if (ST.valid(tSlot) && (ItemNBT.get(tSlot) != null)) {
					if (tStack != null) {
						if (((ItemNBT.get(tStack) != null) != (ItemNBT.get(tSlot) != null)) || ((ItemNBT.get(tStack) != null) && !ItemNBT.get(tStack).equals(ItemNBT.get(tSlot)))) return F;
					}
					tStack = tSlot;
				}
			}
		}
		return super.matches(aGrid, aWorld);
	}
	
	@Override
	public ItemStack getCraftingResult(CraftingInput aGrid) {
		ItemStack rStack = super.getCraftingResult(aGrid);
		if (rStack != null) {
			// Update the Stack
			ST.update(rStack);
			
			// Keeping NBT
			// F11: CraftingInput.size()/getItem(i) (getStackInSlot/getSizeInventory удалены); ST.valid(...)
			// заменяет "!= null" (getItem(i) всегда non-null, пустой слот = ItemStack.EMPTY).
			if (mKeepingNBT) for (int i = 0; i < aGrid.size(); i++) {
				ItemStack tSlot = aGrid.getItem(i);
				if (ST.valid(tSlot) && (ItemNBT.get(tSlot) != null)) {
					UT.NBT.set(rStack, (CompoundTag)ItemNBT.get(tSlot).copy());
					break;
				}
			}

			// GT Charge Values
			if (rStack.getItem() instanceof IItemEnergy) {
				for (TagData tEnergyType : ((IItemEnergy)rStack.getItem()).getEnergyTypes(rStack)) {
					long tCharge = 0;
					for (int i = 0; i < aGrid.size(); i++) {
						ItemStack tSlot = aGrid.getItem(i);
						if (ST.valid(tSlot) && tSlot.getItem() instanceof IItemEnergy && !(tSlot.getItem() instanceof IItemGTContainerTool)) {
							tCharge += ((IItemEnergy)tSlot.getItem()).getEnergyStored(tEnergyType, tSlot);
						}
					}
					((IItemEnergy)rStack.getItem()).setEnergyStored(tEnergyType, rStack, tCharge);
				}
			}

			// Saving Ingredients inside the Item.
			if (mDismantleable) {
				CompoundTag rNBT = ItemNBT.get(rStack), tNBT = UT.NBT.make();
				if (rNBT == null) rNBT = UT.NBT.make();
				// F11 (АДАПТИРОВАНО): 1.7.10 InventoryCrafting фикс-9 (3x3); neo CraftingInput подрезается до габарита
				// (F11-crafting-recipe.md §7) — Math.min(9,size()) сохраняет 1:1 для полной 3x3 и не переполняется на меньшей. Не заглушка.
				for (int i = 0, j = Math.min(9, aGrid.size()); i < j; i++) {
					ItemStack tStack = aGrid.getItem(i);
					if (ST.valid(tStack) && ST.container(tStack, T) == null && !(tStack.getItem() instanceof MultiItemTool)) {
						tStack = ST.amount(1, tStack);
						tNBT.put(""+i, ST.save(tStack));
					}
				}
				rNBT.put(NBT_RECYCLING_COMPS, tNBT);
				UT.NBT.set(rStack, rNBT);
			}
			
			// Add Enchantments
			for (int i = 0; i < mEnchantmentsAdded.length; i++) UT.NBT.addEnchantment(rStack, mEnchantmentsAdded[i], UT.NBT.getEnchantmentLevel(mEnchantmentsAdded[i], rStack) + mEnchantmentLevelsAdded[i]);
			
			// Update the Stack again
			ST.update(rStack);
		}
		return rStack;
	}
	
	@Override
	public boolean isRemovableByGT() {
		return mRemovableByGT;
	}
	
	@Override
	public boolean isAutocraftableByGT() {
		return mAutoCraftable;
	}
}

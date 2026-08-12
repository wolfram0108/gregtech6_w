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
 *
 * Modified in 2026 for the GregTech 6 NeoForge port
 * (https://github.com/wolfram0108/gregtech6_w): ported from Minecraft 1.7.10 / Forge
 * to Minecraft 26.1.2 / NeoForge.
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
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import gregapi.recipes.ShapelessOreRecipe;

import static gregapi.data.CS.*;

/**
 * @author Gregorius Techneticies
 */
public class AdvancedCraftingShapeless extends ShapelessOreRecipe implements ICraftingRecipeGT {
	public final boolean mDismantleable, mRemovableByGT, mAutoCraftable, mKeepingNBT;
	private final net.minecraft.world.item.enchantment.Enchantment[] mEnchantmentsAdded;
	private final int[] mEnchantmentLevelsAdded;
	
	public AdvancedCraftingShapeless(ItemStack aResult, boolean aDismantleAble, boolean aRemovableByGT, boolean aKeepingNBT, boolean aAutoCraftable, net.minecraft.world.item.enchantment.Enchantment[] aEnchantmentsAdded, int[] aEnchantmentLevelsAdded, Object... aRecipe) {
		super(aResult, aRecipe);
		mEnchantmentsAdded = aEnchantmentsAdded;
		mEnchantmentLevelsAdded = aEnchantmentLevelsAdded;
		mRemovableByGT = aRemovableByGT;
		mKeepingNBT = aKeepingNBT;
		mDismantleable = aDismantleAble;
		mAutoCraftable = aAutoCraftable;
	}
	
	@Override
	public boolean matches(CraftingContainer aGrid, Level aWorld) {
		if (mKeepingNBT) {
			ItemStack tStack = null, tMainInput = ((getInput().get(0) instanceof ItemStack) ? (ItemStack)getInput().get(0) : null);
			// F11: Forge InventoryCrafting.getSizeInventory()/getStackInSlot(i) удалены; neo-эквивалент —
			// CraftingContainer.size()/getItem(i). Занятость слота — ST.valid(...) (getItem(i) всегда non-null).
			for (int i = 0; i < aGrid.getContainerSize(); i++) {
				ItemStack tSlot = aGrid.getItem(i);
				if (ST.valid(tSlot)) {
					if (tMainInput == null) {
						if (getInput().get(0) instanceof Iterable) for (Object tObject : (Iterable)getInput().get(0)) if (tObject instanceof ItemStack) {
							if (ST.equal(tSlot, (ItemStack)tObject, T)) {
								tMainInput = ST.amount(1, tSlot);
							}
						}
					} else {
						if (ST.equal_(tSlot, tMainInput, T)) {
							if (tStack != null && !ST.equal_(tStack, tSlot, F)) return F;
							tStack = tSlot;
						}
					}

				}
			}
		}
		return super.matches(aGrid, aWorld);
	}
	
	@Override
	public ItemStack getCraftingResult(CraftingContainer aGrid) {
		ItemStack rStack = super.getCraftingResult(aGrid);
		if (rStack != null) {
			// Update the Stack
			ST.update(rStack);
			
			// Keeping NBT
			// F11: CraftingContainer.size()/getItem(i) (getStackInSlot/getSizeInventory удалены); ST.valid(...)
			// заменяет "!= null" (getItem(i) всегда non-null, пустой слот = ItemStack.EMPTY).
			if (mKeepingNBT) {
				ItemStack tMainInput = ((getInput().get(0) instanceof ItemStack) ? (ItemStack)getInput().get(0) : null);
				for (int i = 0; i < aGrid.getContainerSize(); i++) {
					ItemStack tSlot = aGrid.getItem(i);
					if (ST.valid(tSlot) && (ItemNBT.get(tSlot) != null) && (tMainInput == null || ST.equal_(tSlot, tMainInput, T))) {
						UT.NBT.set(rStack, (CompoundTag)ItemNBT.get(tSlot).copy());
						break;
					}
				}
			}

			// GT Charge Values
			if (rStack.getItem() instanceof IItemEnergy) {
				for (TagData tEnergyType : ((IItemEnergy)rStack.getItem()).getEnergyTypes(rStack)) {
					long tCharge = 0;
					for (int i = 0; i < aGrid.getContainerSize(); i++) {
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
				// См. AdvancedCraftingShaped: Math.min(9, getContainerSize()) — граница меньшей сетки (инвентарь игрока 2x2)
				// (F11-crafting-recipe.md §7), 1:1 для полного 3x3. Не заглушка.
				for (int i = 0, j = Math.min(9, aGrid.getContainerSize()); i < j; i++) {
					ItemStack tStack = aGrid.getItem(i);
					if (ST.valid(tStack) && ST.container(tStack, true) == null && !(tStack.getItem() instanceof MultiItemTool)) {
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

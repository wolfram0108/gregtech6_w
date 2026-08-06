/**
 * Copyright (c) 2020 GregTech-6 Team
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

import gregapi.oredict.OreDictionary;
import gregapi.util.ST;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static gregapi.data.CS.*;

/**
 * F11 ПЕРЕХОДНИК — замена Forge {@code net.minecraftforge.oredict.ShapelessOreRecipe} (см.
 * {@code decisions/F11-crafting-recipe.md}). Дословный аналог Forge-семантики бесформенного ore-рецепта,
 * но сетка — neo {@code CraftingInput} ({@code getItem}/{@code size}), сравнение стеков — GT6 {@code ST.equal}
 * (учитывает материал-компонент, F1), ore-имена — F4 {@code OreDictionary.getOres}.
 *
 * <p>Каждый вход — {@code ItemStack} (точный) либо {@code List<ItemStack>} (ore-альтернативы). Это тот же
 * формат, что читает {@code AdvancedCraftingShapeless.matches} через {@code getInput()}
 * ({@code instanceof ItemStack} / {@code instanceof Iterable}).</p>
 */
public class ShapelessOreRecipe implements ICraftingRecipeGT {
	protected final ItemStack mOutput;
	/** Каждый элемент: {@code ItemStack} (точный вход) или {@code List<ItemStack>} (ore-альтернативы). */
	protected final List<Object> mInput = new ArrayList<>();
	/** F4 роль-C: ore-версия ванильного датапак-рецепта (см. {@code ShapedOreRecipe.mVanillaReplacement}). */
	public boolean mVanillaReplacement = F;
	public net.minecraft.resources.ResourceKey<net.minecraft.world.item.crafting.Recipe<?>> mSourceId = null;

	public ShapelessOreRecipe(ItemStack aResult, Object... aRecipe) {
		mOutput = ST.copy(aResult);
		for (Object tObject : aRecipe) {
			if (tObject instanceof ItemStack) {
				mInput.add(ST.copy((ItemStack)tObject));
			} else if (tObject instanceof List) {
				mInput.add(tObject);
			} else if (tObject instanceof String) {
				mInput.add(OreDictionary.getOres((String)tObject));
			} else {
				throw new IllegalArgumentException("Invalid shapeless ore recipe ingredient: " + tObject);
			}
		}
	}

	/** Совпадение стека сетки с входом (точный {@code ItemStack} или любой из ore-списка). */
	protected static boolean ingredientMatches(ItemStack aStack, Object aTarget) {
		if (aTarget instanceof ItemStack) return ST.equal(aStack, (ItemStack)aTarget, T);
		if (aTarget instanceof List) for (Object tAlt : (List<?>)aTarget) if (tAlt instanceof ItemStack && ST.equal(aStack, (ItemStack)tAlt, T)) return T;
		return F;
	}

	@Override
	public boolean matches(CraftingInput aGrid, Level aWorld) {
		List<Object> tRequired = new ArrayList<>(mInput);
		for (int i = 0; i < aGrid.size(); i++) {
			ItemStack tSlot = aGrid.getItem(i);
			if (!tSlot.isEmpty()) {
				boolean tFound = F;
				for (Iterator<Object> tIt = tRequired.iterator(); tIt.hasNext();) {
					if (ingredientMatches(tSlot, tIt.next())) {tIt.remove(); tFound = T; break;}
				}
				if (!tFound) return F;
			}
		}
		return tRequired.isEmpty();
	}

	@Override
	public ItemStack getCraftingResult(CraftingInput aGrid) {return ST.copy(mOutput);}

	/** @return список входов ({@code ItemStack} / {@code List<ItemStack>}) — как у Forge-{@code getInput()}. */
	public List<Object> getInput() {return mInput;}

	@Override public int getRecipeSize() {return mInput.size();}
	@Override public ItemStack getRecipeOutput() {return mOutput;}
	@Override public boolean isRemovableByGT() {return T;}
	@Override public boolean isAutocraftableByGT() {return T;}
}

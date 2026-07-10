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
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static gregapi.data.CS.*;

/**
 * F11 ПЕРЕХОДНИК — замена Forge {@code net.minecraftforge.oredict.ShapedOreRecipe} (см.
 * {@code decisions/F11-crafting-recipe.md}). Дословный аналог Forge-семантики фигурного ore-рецепта:
 * разбор паттерна (строки + {@code Character}→ингредиент), матчинг сетки со сдвигом/зеркалом. Сетка —
 * neo {@code CraftingInput} (уже подрезан до габарита, {@code getItem(x,y)}/{@code width}/{@code height}),
 * сравнение стеков — GT6 {@code ST.equal} (материал-компонент F1), ore-имена — F4 {@code OreDictionary.getOres}.
 */
public class ShapedOreRecipe implements ICraftingRecipeGT {
	protected final ItemStack mOutput;
	/** Ячейки row-major, размер {@code mWidth*mHeight}. Каждая: {@code null} (пусто), {@code ItemStack} или {@code List<ItemStack>}. */
	protected final Object[] mInput;
	protected final int mWidth, mHeight;
	protected boolean mMirrored = F;

	public ShapedOreRecipe(ItemStack aResult, Object... aRecipe) {
		mOutput = ST.copy(aResult);
		int tIdx = 0;

		// Forge-формат: ведущий Boolean = зеркало; если следом Object[] — это и есть реальный recipe.
		if (aRecipe.length > 0 && aRecipe[0] instanceof Boolean) {
			mMirrored = (Boolean)aRecipe[0];
			if (aRecipe.length > 1 && aRecipe[1] instanceof Object[]) aRecipe = (Object[])aRecipe[1]; else tIdx = 1;
		}

		// Паттерн: либо String[] (строки-ряды), либо последовательность String.
		StringBuilder tShape = new StringBuilder();
		int tWidth = 0, tHeight = 0;
		if (aRecipe[tIdx] instanceof String[]) {
			for (String tRow : (String[])aRecipe[tIdx++]) {tWidth = tRow.length(); tShape.append(tRow); tHeight++;}
		} else {
			while (tIdx < aRecipe.length && aRecipe[tIdx] instanceof String) {String tRow = (String)aRecipe[tIdx++]; tWidth = tRow.length(); tShape.append(tRow); tHeight++;}
		}
		mWidth = tWidth; mHeight = tHeight;

		// Карта символ→ингредиент из оставшихся пар (Character, ingredient).
		Map<Character, Object> tMap = new HashMap<>();
		for (; tIdx < aRecipe.length; tIdx += 2) {
			Character tChar = (Character)aRecipe[tIdx];
			Object tIn = aRecipe[tIdx+1];
			if (tIn instanceof ItemStack) tMap.put(tChar, ST.copy((ItemStack)tIn));
			else if (tIn instanceof List) tMap.put(tChar, tIn);
			else if (tIn instanceof String) tMap.put(tChar, OreDictionary.getOres((String)tIn));
			else if (tIn instanceof ItemLike) tMap.put(tChar, new ItemStack((ItemLike)tIn));
			else throw new IllegalArgumentException("Invalid shaped ore recipe ingredient: " + tIn);
		}

		mInput = new Object[mWidth * mHeight];
		char[] tChars = tShape.toString().toCharArray();
		for (int i = 0; i < tChars.length; i++) mInput[i] = tMap.get(tChars[i]); // ' ' отсутствует в карте → null (пусто)
	}

	@Override
	public boolean matches(CraftingInput aGrid, Level aWorld) {
		if (aGrid.width() != mWidth || aGrid.height() != mHeight) return F;
		return checkMatch(aGrid, F) || (mMirrored && checkMatch(aGrid, T));
	}

	protected boolean checkMatch(CraftingInput aGrid, boolean aMirror) {
		for (int y = 0; y < mHeight; y++) for (int x = 0; x < mWidth; x++) {
			Object tTarget = mInput[(aMirror ? (mWidth-1-x) : x) + y*mWidth];
			ItemStack tActual = aGrid.getItem(x, y);
			if (tTarget == null) {if (!tActual.isEmpty()) return F;}
			else if (!ShapelessOreRecipe.ingredientMatches(tActual, tTarget)) return F;
		}
		return T;
	}

	@Override
	public ItemStack getCraftingResult(CraftingInput aGrid) {return ST.copy(mOutput);}

	/** Forge {@code setMirrored}: разрешить зеркальное совпадение. Возвращает себя для цепочки. */
	public ShapedOreRecipe setMirrored(boolean aMirrored) {mMirrored = aMirrored; return this;}

	/** @return входы ячеек ({@code null}/{@code ItemStack}/{@code List<ItemStack>}) — как у Forge-{@code getInput()} ({@code Object[]}). */
	public Object[] getInput() {return mInput;}

	@Override public int getRecipeSize() {return mWidth * mHeight;}
	@Override public ItemStack getRecipeOutput() {return mOutput;}
	@Override public boolean isRemovableByGT() {return T;}
	@Override public boolean isAutocraftableByGT() {return T;}
}

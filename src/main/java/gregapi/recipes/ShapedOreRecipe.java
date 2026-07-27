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

		Object[] tCells = new Object[tWidth * tHeight];
		char[] tChars = tShape.toString().toCharArray();
		for (int i = 0; i < tChars.length && i < tCells.length; i++) tCells[i] = tMap.get(tChars[i]); // ' ' отсутствует в карте → null (пусто)

		// F11 (BUG-058) — ПУСТЫЕ КРАЯ ПАТТЕРНА ОБРЕЗАЮТСЯ ТЕМ ЖЕ ПРИЁМОМ, ЧТО ДВИЖОК ПРИМЕНЯЕТ К СЕТКЕ.
		//
		// 1.7.10: matches звался на ПОЛНОЙ сетке 3×3, и Forge искал паттерн ПЕРЕБОРОМ СМЕЩЕНИЙ —
		//   `for (x = 0; x <= 3-width; x++) for (y = 0; y <= 3-height; y++) checkMatch(inv, x, y, …)`
		//   (Forge-1.7.10 ShapedOreRecipe.java:174-190, checkMatch:196-248: вне окна паттерна слот обязан быть пуст).
		//   Поэтому объявление с пустым краем — например `"  "`/`" S"` (Loader_Recipes_Woods.java:134,
		//   конверсия любой деревянной палки в ванильную) — совпадало нормально.
		// neo: движок отдаёт в matches УЖЕ ОБРЕЗАННУЮ сетку — bounding box непустых слотов
		//   (CraftingInput.ofPositioned, neo-decompiled/.../CraftingInput.java:36-77: newWidth = right-left+1),
		//   а смещение (left/top) остаётся снаружи и рецепту не передаётся. Перебор смещений здесь
		//   невоспроизводим и не нужен — его роль исполняет сама обрезка.
		// Следствие без этой нормализации: паттерн 2×2 с одним ингредиентом сравнивался с сеткой 1×1 и
		//   `aGrid.width() != mWidth` отсекал рецепт НАВСЕГДА. Замер судьёй самораскладки: таких паттернов
		//   в буфере 2983, из них 898 проверяемых давали FAIL.
		// Приём взят у самого движка (философия §4 «адаптируем на уровне движка, централизованно»), правка —
		//   в ЕДИНСТВЕННОМ центре shaped-крафта GT6: AdvancedCraftingShaped наследует этот класс.
		int tLeft = tWidth, tRight = -1, tTop = tHeight, tBottom = -1;
		for (int y = 0; y < tHeight; y++) for (int x = 0; x < tWidth; x++) if (tCells[x + y*tWidth] != null) {
			if (x < tLeft  ) tLeft   = x;
			if (x > tRight ) tRight  = x;
			if (y < tTop   ) tTop    = y;
			if (y > tBottom) tBottom = y;
		}
		if (tRight < 0 || (tRight-tLeft+1 == tWidth && tBottom-tTop+1 == tHeight)) {
			// паттерн целиком пуст либо краёв нет — оставляем как объявлено
			mWidth = tWidth; mHeight = tHeight; mInput = tCells;
		} else {
			int tNewWidth = tRight-tLeft+1, tNewHeight = tBottom-tTop+1;
			Object[] tCropped = new Object[tNewWidth * tNewHeight];
			for (int y = 0; y < tNewHeight; y++) for (int x = 0; x < tNewWidth; x++) tCropped[x + y*tNewWidth] = tCells[(x+tLeft) + (y+tTop)*tWidth];
			mWidth = tNewWidth; mHeight = tNewHeight; mInput = tCropped;
		}
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

	/** Read-only геттер на {@link #mWidth} (Ф1.3-crafting-jei: ширина сетки нужна JEI-раскладке; не ломает F11-шов, шире протокол не меняет). */
	public int getWidth() {return mWidth;}
	/** Read-only геттер на {@link #mHeight} (Ф1.3-crafting-jei: высота сетки нужна JEI-раскладке; не ломает F11-шов, шире протокол не меняет). */
	public int getHeight() {return mHeight;}

	@Override public int getRecipeSize() {return mWidth * mHeight;}
	@Override public ItemStack getRecipeOutput() {return mOutput;}
	@Override public boolean isRemovableByGT() {return T;}
	@Override public boolean isAutocraftableByGT() {return T;}
}

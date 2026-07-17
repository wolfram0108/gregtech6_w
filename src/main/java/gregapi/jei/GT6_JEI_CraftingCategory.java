/**
 * Copyright (c) 2026 GregTech-6 Team
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

package gregapi.jei;

import gregapi.data.MD;
import gregapi.recipes.ICraftingRecipeGT;
import gregapi.recipes.ShapedOreRecipe;
import gregapi.recipes.ShapelessOreRecipe;
import gregapi.util.ST;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.ICraftingGridHelper;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.AbstractRecipeCategory;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;

import java.util.ArrayList;
import java.util.List;

import static gregapi.data.CS.*;

/**
 * ЕДИНЫЙ центр JEI-показа крафт-верстака GT6 (Ф1.3-crafting-jei, decisions/F11-crafting-recipe.md). В 1.7.10
 * NEI показывал крафт GT6 сам: {@code ICraftingRecipeGT extends IRecipe}, GT6 регистрировал конкретные
 * реализации forge {@code ShapedOreRecipe}/{@code ShapelessOreRecipe} через {@code GameRegistry.addRecipe}
 * (см. {@code CR.shaped}/{@code CR.shapeless} тех лет), и встроенный (не GT6-код, часть самого NEI)
 * {@code ShapedRecipeHandler}/{@code ShapelessRecipeHandler} узнавал их по классу и рисовал сам — GT6 даже
 * явно ОТКЛЮЧАЛ свой цикл {@code GuiCraftingRecipe.craftinghandlers} (gregapi/NEI_GT_API_Config.java:59,
 * закомментировано), полагаясь целиком на автоматику NEI.
 *
 * <p>В neo GT6-крафт (F11) — СОБСТВЕННЫЙ буфер {@code CR.BUFFER}/{@code ICraftingRecipeGT}, диспетчер
 * {@code CustomRecipe} читает его в рантайме верстака; сами рецепты НЕ neo {@code CraftingRecipe}
 * (ADR F11 §"почему не Recipe"), поэтому встроенная JEI-категория {@code RecipeTypes.CRAFTING}
 * ({@code IRecipeHolderType<RecipeHolder<CraftingRecipe>>} — требует codec/serializer в
 * {@code RecipeManager}, см. ADR §Ф1.3-crafting-jei) их не видит и не может: делать GT6-рецепты настоящим
 * {@code CraftingRecipe} означало бы переоткрыть закрытый F11-шов ради витрины. Поэтому — СОБСТВЕННАЯ
 * {@code IRecipeCategory} на {@link ICraftingRecipeGT}, читающая тот же {@code CR.list()} (F11-буфер) 1:1,
 * но раскладка сетки — через штатный JEI {@link ICraftingGridHelper} (тот же общий JEI-механизм, что и
 * встроенная категория): 3×3-сетка по 18px/слот, выходной слот (95,19), размер 116×54, иконка
 * {@code Blocks.CRAFTING_TABLE} — 1:1 скопировано (декомпилировано javap) из
 * {@code mezz.jei.library.plugins.vanilla.crafting.CraftingRecipeCategory} (сама эта константа не в наших
 * трёх neo-корнях — реализация JEI закрыта, но она читается через javap как часть приёмки данной задачи;
 * контракт {@link ICraftingGridHelper} в {@code jei-26.1.2-common-api}), — визуально неотличима от
 * нативного крафт-рендера JEI.
 *
 * <p>Показываются ТОЛЬКО {@link ShapedOreRecipe}/{@link ShapelessOreRecipe}-наследники F11-буфера
 * (в т.ч. {@code AdvancedCraftingShaped}/{@code AdvancedCraftingShapeless}/{@code AdvancedCraftingTool}) —
 * 1:1 с тем, что показывал NEI: {@code AdvancedCrafting1ToY}/{@code AdvancedCraftingXToY} реализуют
 * {@code ICraftingRecipeGT} НАПРЯМУЮ (не наследуют forge {@code ShapedOreRecipe}/{@code ShapelessOreRecipe})
 * и их {@code getRecipeOutput()} возвращает {@code ERROR_OUTPUT}-заглушку (не настоящий выход) — они и в
 * 1.7.10 не попадали под NEI-хендлер и NЕ отображались (не регрессия, тот же самый пробел).
 */
public final class GT6_JEI_CraftingCategory extends AbstractRecipeCategory<ICraftingRecipeGT> {
	public static final RecipeType<ICraftingRecipeGT> TYPE = RecipeType.create(MD.GT.mID, "crafting_gt6", ICraftingRecipeGT.class);

	private final ICraftingGridHelper mGridHelper;

	public GT6_JEI_CraftingCategory(IGuiHelper aGuiHelper) {
		super(TYPE, Component.literal("Crafting Table"), aGuiHelper.createDrawableItemLike(Blocks.CRAFTING_TABLE), 116, 54);
		mGridHelper = aGuiHelper.createCraftingGridHelper();
	}

	@Override
	public void setRecipe(IRecipeLayoutBuilder aBuilder, ICraftingRecipeGT aRecipe, IFocusGroup aFocuses) {
		try {
			if (aRecipe instanceof ShapedOreRecipe) {
				ShapedOreRecipe tShaped = (ShapedOreRecipe)aRecipe;
				mGridHelper.createAndSetInputs(aBuilder, cells(tShaped.getInput()), tShaped.getWidth(), tShaped.getHeight());
			} else if (aRecipe instanceof ShapelessOreRecipe) {
				List<Object> tInput = ((ShapelessOreRecipe)aRecipe).getInput();
				mGridHelper.createAndSetInputs(aBuilder, cells(tInput.toArray()), 0, 0);
			}
			ItemStack tOutput = aRecipe.getRecipeOutput();
			if (ST.valid(tOutput)) mGridHelper.createAndSetOutputs(aBuilder, List.of(tOutput));
		} catch (Throwable e) {
			ERR.println("JEI: GT6 crafting recipe failed to lay out, skipping its slots.");
			e.printStackTrace(ERR);
		}
	}

	/** Ячейка {@code null}/{@code ItemStack}/{@code List<ItemStack>} (см. {@link ShapedOreRecipe#getInput()}
	 *  и {@link ShapelessOreRecipe#getInput()}) -> {@code List<ItemStack>} для {@link ICraftingGridHelper}. */
	private static List<List<ItemStack>> cells(Object[] aCells) {
		List<List<ItemStack>> rCells = new ArrayList<>(aCells.length);
		for (Object tCell : aCells) {
			if (tCell instanceof ItemStack) {
				ItemStack tStack = (ItemStack)tCell;
				rCells.add(ST.valid(tStack) ? List.of(tStack) : List.of());
			} else if (tCell instanceof List) {
				List<ItemStack> tAlts = new ArrayList<>();
				for (Object tAlt : (List<?>)tCell) if (tAlt instanceof ItemStack && ST.valid((ItemStack)tAlt)) tAlts.add((ItemStack)tAlt);
				rCells.add(tAlts);
			} else {
				rCells.add(List.of());
			}
		}
		return rCells;
	}
}

/**
 * Copyright (c) 2026 wolfram0108
 *
 * Written in 2026 for the GregTech 6 NeoForge port
 * (https://github.com/wolfram0108/gregtech6_w). Not part of the original GregTech 6
 * by Gregorius Techneticies; distributed under the same licence as the work it extends.
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
 * <p>BUG-099 (требование пользователя «рецепты обязаны быть в витрине на 100%»): показываются ЧЕТЫРЕ типа —
 * {@link ShapedOreRecipe}/{@link ShapelessOreRecipe}-наследники (в т.ч. {@code AdvancedCraftingShaped}/
 * {@code AdvancedCraftingShapeless}/{@code AdvancedCraftingTool}) плюс оба самостоятельных:
 * {@code AdvancedCrafting1ToY} (один предмет, продукт выбирается КЛЕТКОЙ) и {@code AdvancedCraftingXToY}
 * (X предметов префикса → Y выхода). Последние два реализуют {@code ICraftingRecipeGT} напрямую, а их
 * {@code getRecipeOutput()} отдаёт {@code ERROR_OUTPUT}-заглушку — поэтому их не рисовал и NEI в 1.7.10
 * (тот узнавал рецепты по классу). Здесь витрина ИДЁТ ДАЛЬШЕ оригинала: раскладку и выход строим из полей
 * самого рецепта (префикс входа/выхода, количество, номер клетки), так что игрок видит и что получится,
 * и куда класть.</p>
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
			} else if (aRecipe instanceof gregapi.recipes.AdvancedCrafting1ToY t1ToY) {
				// BUG-099: продукт выбирается КЛЕТКОЙ — предмет кладётся так, чтобы перед ним стояло ровно
				// mEmpty пустых (то же число, по которому рецепт себя опознаёт, AdvancedCrafting1ToY:161).
				// Рисуем настоящую сетку 3x3 с предметом в этой клетке: игрок видит не только ЧТО получится,
				// но и КУДА положить — без этого механику из витрины не узнать.
				List<ItemStack> tIn = new java.util.ArrayList<>(), tOut = new java.util.ArrayList<>();
				materialPairs(t1ToY, t1ToY.mInput, t1ToY.mOutput, t1ToY.mOutputCount, m -> t1ToY.hasOutputFor(m), tIn, tOut);
				if (!tIn.isEmpty() && t1ToY.mEmpty < 9) {
					List<List<ItemStack>> tCells = new java.util.ArrayList<>();
					for (int i = 0; i < 9; i++) tCells.add(i == t1ToY.mEmpty ? tIn : List.of());
					mGridHelper.createAndSetInputs(aBuilder, tCells, 3, 3);
					mGridHelper.createAndSetOutputs(aBuilder, tOut);
				}
				return;
			} else if (aRecipe instanceof gregapi.recipes.AdvancedCraftingXToY tXToY) {
				// BUG-099: X одинаковых предметов префикса → выход. Позиции не важны (рецепт считает только
				// количество) — показываем плотной раскладкой, как их и кладут.
				List<ItemStack> tIn = new java.util.ArrayList<>(), tOut = new java.util.ArrayList<>();
				materialPairs(tXToY, tXToY.mInput, tXToY.mOutput, tXToY.mOutputCount, m -> tXToY.hasOutputFor(m), tIn, tOut);
				int tN = tXToY.mInputCount;
				if (!tIn.isEmpty() && tN > 0 && tN <= 9) {
					int tW = tN >= 9 ? 3 : (tN >= 4 ? 2 : tN), tH = (tN + tW - 1) / tW;
					List<List<ItemStack>> tCells = new java.util.ArrayList<>();
					for (int i = 0; i < tW * tH; i++) tCells.add(i < tN ? tIn : List.of());
					mGridHelper.createAndSetInputs(aBuilder, tCells, tW, tH);
					mGridHelper.createAndSetOutputs(aBuilder, tOut);
				}
				return;
			}
			ItemStack tOutput = aRecipe.getRecipeOutput();
			if (ST.valid(tOutput)) mGridHelper.createAndSetOutputs(aBuilder, List.of(tOutput));
		} catch (Throwable e) {
			ERR.println("JEI: GT6 crafting recipe failed to lay out, skipping its slots.");
			e.printStackTrace(ERR);
		}
	}

	/** BUG-099: рецепт задан на ПРЕФИКС, а витрина показывает конкретные предметы — собираем пары «вход↔выход»
	 *  по всем материалам, для которых у рецепта есть выход. Списки одной длины и одного порядка: JEI листает
	 *  альтернативы слотов в такт, поэтому вход и выход всегда показывают ОДИН материал.
	 *  <p>Материалы берём из {@code MATERIAL_MAP} (существующие), а не из разреженного массива на 32767 слотов:
	 *  раскладка строится для каждого рецепта, и обход массива стоил 14 секунд на регистрации плагина —
	 *  за это время интегрированный сервер успевал отвалиться (замер по логу JEI-стартера).</p> */
	private static final java.util.Map<Object, List<List<ItemStack>>> PAIRS_CACHE = new java.util.WeakHashMap<>();

	/** Пары для рецепта, считаются один раз (JEI может перестраивать раскладку). */
	private static void materialPairs(Object aRecipe, gregapi.oredict.OreDictPrefix aInput, gregapi.oredict.OreDictPrefix aOutput, int aOutputCount
	, java.util.function.Predicate<gregapi.oredict.OreDictMaterial> aHasOutput, List<ItemStack> rInputs, List<ItemStack> rOutputs) {
		List<List<ItemStack>> tCached = PAIRS_CACHE.get(aRecipe);
		if (tCached != null) {rInputs.addAll(tCached.get(0)); rOutputs.addAll(tCached.get(1)); return;}
		materialPairs(aInput, aOutput, aOutputCount, aHasOutput, rInputs, rOutputs);
		PAIRS_CACHE.put(aRecipe, List.of(List.copyOf(rInputs), List.copyOf(rOutputs)));
	}

	private static void materialPairs(gregapi.oredict.OreDictPrefix aInput, gregapi.oredict.OreDictPrefix aOutput, int aOutputCount
	, java.util.function.Predicate<gregapi.oredict.OreDictMaterial> aHasOutput, List<ItemStack> rInputs, List<ItemStack> rOutputs) {
		for (gregapi.oredict.OreDictMaterial tMaterial : new java.util.LinkedHashSet<>(gregapi.oredict.OreDictMaterial.MATERIAL_MAP.values())) {
			if (tMaterial == null || !aHasOutput.test(tMaterial)) continue;
			ItemStack tIn = aInput.mat(tMaterial, 1);
			if (!ST.valid(tIn)) continue;
			ItemStack tOut = aOutput.mat(tMaterial, aOutputCount);
			if (!ST.valid(tOut)) continue;
			rInputs.add(tIn); rOutputs.add(tOut);
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

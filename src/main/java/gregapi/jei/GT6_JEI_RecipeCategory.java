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

import gregapi.recipes.Recipe;
import gregapi.recipes.Recipe.RecipeMap;
import gregapi.util.ST;
import gregapi.util.UT;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.AbstractRecipeCategory;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import java.util.ArrayList;
import java.util.List;

import static gregapi.data.CS.*;

/**
 * ОДНА generic JEI-категория, инстанциируемая на каждую видимую {@link RecipeMap} (см.
 * {@link GT6_JEI_Plugin#registerCategories}). Раскладка слотов портирует 1:1 логику
 * {@code gregapi.NEI_RecipeMap.CachedDefaultRecipe} (gregapi/NEI_RecipeMap.java:147-394) — тот же
 * switch по {@code mInputItemsCount}/{@code mOutputItemsCount}, те же пиксельные координаты слотов
 * (сдвинутые на {@link #OFFSET_X}/{@link #OFFSET_Y}, как делал {@code FixedPositionedStack}, т.к. NEI
 * координировал слоты от левого угла оверлея, а JEI — от левого угла своего собственного виджета),
 * тот же алгоритм текста длительности/энергии/спецзначения ({@code drawExtras},
 * gregapi/NEI_RecipeMap.java:687-724), тут — через {@link IRecipeExtrasBuilder#addText}.
 */
public final class GT6_JEI_RecipeCategory extends AbstractRecipeCategory<Recipe> {
	/** См. {@code gregapi.NEI_RecipeMap.sOffsetX/sOffsetY} (gregapi/NEI_RecipeMap.java:66). */
	private static final int OFFSET_X = 5, OFFSET_Y = 11;
	private static final int WIDTH = 176, HEIGHT = 161;

	private final RecipeMap mMap;
	/** Система координат ЗАЯКОРЕНА ПО ПИКСЕЛЯМ (замер canner.png: рамка входа-1 @текстуры (34,24) ⇒ предмет (35,25)
	 *  = сырые NEI-числа слотов) → JEI-координата = NEI-число БЕЗ офсета, GUI-текстура рисуется @(0,3) с v=3
	 *  (текстур-пиксель == JEI-пиксель); NEI.png сдвинут на 11 вверх относительно GUI-текстуры
	 *  (сведение систем 1.7.10: машина @(−5,−8,v3), NEI @(−5,−16)) → клип v=11, высота 155 @(0,0). */
	private final IDrawable mBackNEI, mBackGui;

	public GT6_JEI_RecipeCategory(RecipeMap aMap, RecipeType<Recipe> aType, IGuiHelper aGuiHelper) {
		super(aType, Component.literal(aMap.mNameLocal), icon(aMap, aGuiHelper), WIDTH, HEIGHT);
		mMap = aMap;
		IDrawable tNEI = null, tGui = null;
		try {
			tNEI = aGuiHelper.createDrawable(net.minecraft.resources.ResourceLocation.parse((RES_PATH_GUI + "machines/NEI.png").toLowerCase(java.util.Locale.ROOT)), 0, 5, 176, 161);
			String tGuiPath = gregapi.util.UT.Code.stringValid(aMap.mGUIPath) ? aMap.mGUIPath : RES_PATH_GUI + aMap.mNameInternal + ".png";
			tGui = aGuiHelper.createDrawable(net.minecraft.resources.ResourceLocation.parse(tGuiPath.toLowerCase(java.util.Locale.ROOT)), 0, 3, 176, 79);
		} catch (Throwable e) {ERR.println("JEI: фон категории '" + aMap.mNameInternal + "' не собрался: " + e);}
		mBackNEI = tNEI; mBackGui = tGui;
	}

	@Override
	public void draw(Recipe aRecipe, mezz.jei.api.gui.ingredient.IRecipeSlotsView aSlotsView, net.minecraft.client.gui.GuiGraphicsExtractor aGraphics, double aMouseX, double aMouseY) {
		if (mBackNEI != null) mBackNEI.draw(aGraphics, 0, 0);
		if (mBackGui != null) mBackGui.draw(aGraphics, 0, 3);
	}

	private static IDrawable icon(RecipeMap aMap, IGuiHelper aGuiHelper) {
		return aMap.mRecipeMachineList.isEmpty() ? aGuiHelper.createBlankDrawable(16, 16) : aGuiHelper.createDrawableItemStack(aMap.mRecipeMachineList.get(0));
	}

	@Override
	public void setRecipe(IRecipeLayoutBuilder aBuilder, Recipe aRecipe, IFocusGroup aFocuses) {
		try {
			int tStartIndex = 0;

			// Портировано 1:1 из gregapi/NEI_RecipeMap.java:173-278 (input-switch по mInputItemsCount).
			switch (mMap.mInputItemsCount) {
			case  0:
				break;
			case  1:
				tStartIndex = in(aBuilder, aRecipe, tStartIndex, 53, mMap.mInputFluidCount>6? 7:25);
				break;
			case  2:
				tStartIndex = in(aBuilder, aRecipe, tStartIndex, 35, mMap.mInputFluidCount>6? 7:25);
				tStartIndex = in(aBuilder, aRecipe, tStartIndex, 53, mMap.mInputFluidCount>6? 7:25);
				break;
			case  3:
				tStartIndex = in(aBuilder, aRecipe, tStartIndex, 17, mMap.mInputFluidCount>6? 7:25);
				tStartIndex = in(aBuilder, aRecipe, tStartIndex, 35, mMap.mInputFluidCount>6? 7:25);
				tStartIndex = in(aBuilder, aRecipe, tStartIndex, 53, mMap.mInputFluidCount>6? 7:25);
				break;
			case  4:
				tStartIndex = in(aBuilder, aRecipe, tStartIndex, 35, mMap.mInputFluidCount>3? 7:16);
				tStartIndex = in(aBuilder, aRecipe, tStartIndex, 53, mMap.mInputFluidCount>3? 7:16);
				tStartIndex = in(aBuilder, aRecipe, tStartIndex, 35, mMap.mInputFluidCount>3?25:34);
				tStartIndex = in(aBuilder, aRecipe, tStartIndex, 53, mMap.mInputFluidCount>3?25:34);
				break;
			case  5:
				tStartIndex = in(aBuilder, aRecipe, tStartIndex, 17, mMap.mInputFluidCount>3? 7:16);
				tStartIndex = in(aBuilder, aRecipe, tStartIndex, 35, mMap.mInputFluidCount>3? 7:16);
				tStartIndex = in(aBuilder, aRecipe, tStartIndex, 53, mMap.mInputFluidCount>3? 7:16);
				tStartIndex = in(aBuilder, aRecipe, tStartIndex, 35, mMap.mInputFluidCount>3?25:34);
				tStartIndex = in(aBuilder, aRecipe, tStartIndex, 53, mMap.mInputFluidCount>3?25:34);
				break;
			case  6:
				tStartIndex = in(aBuilder, aRecipe, tStartIndex, 17, mMap.mInputFluidCount>3? 7:16);
				tStartIndex = in(aBuilder, aRecipe, tStartIndex, 35, mMap.mInputFluidCount>3? 7:16);
				tStartIndex = in(aBuilder, aRecipe, tStartIndex, 53, mMap.mInputFluidCount>3? 7:16);
				tStartIndex = in(aBuilder, aRecipe, tStartIndex, 17, mMap.mInputFluidCount>3?25:34);
				tStartIndex = in(aBuilder, aRecipe, tStartIndex, 35, mMap.mInputFluidCount>3?25:34);
				tStartIndex = in(aBuilder, aRecipe, tStartIndex, 53, mMap.mInputFluidCount>3?25:34);
				break;
			case  7:
				tStartIndex = in(aBuilder, aRecipe, tStartIndex, 17,  7);
				tStartIndex = in(aBuilder, aRecipe, tStartIndex, 35,  7);
				tStartIndex = in(aBuilder, aRecipe, tStartIndex, 53,  7);
				tStartIndex = in(aBuilder, aRecipe, tStartIndex, 17, 25);
				tStartIndex = in(aBuilder, aRecipe, tStartIndex, 35, 25);
				tStartIndex = in(aBuilder, aRecipe, tStartIndex, 53, 25);
				tStartIndex = in(aBuilder, aRecipe, tStartIndex, 17, 43);
				break;
			case  8:
				tStartIndex = in(aBuilder, aRecipe, tStartIndex, 17,  7);
				tStartIndex = in(aBuilder, aRecipe, tStartIndex, 35,  7);
				tStartIndex = in(aBuilder, aRecipe, tStartIndex, 53,  7);
				tStartIndex = in(aBuilder, aRecipe, tStartIndex, 17, 25);
				tStartIndex = in(aBuilder, aRecipe, tStartIndex, 35, 25);
				tStartIndex = in(aBuilder, aRecipe, tStartIndex, 53, 25);
				tStartIndex = in(aBuilder, aRecipe, tStartIndex, 17, 43);
				tStartIndex = in(aBuilder, aRecipe, tStartIndex, 35, 43);
				break;
			case  9:
				tStartIndex = in(aBuilder, aRecipe, tStartIndex, 17,  7);
				tStartIndex = in(aBuilder, aRecipe, tStartIndex, 35,  7);
				tStartIndex = in(aBuilder, aRecipe, tStartIndex, 53,  7);
				tStartIndex = in(aBuilder, aRecipe, tStartIndex, 17, 25);
				tStartIndex = in(aBuilder, aRecipe, tStartIndex, 35, 25);
				tStartIndex = in(aBuilder, aRecipe, tStartIndex, 53, 25);
				tStartIndex = in(aBuilder, aRecipe, tStartIndex, 17, 43);
				tStartIndex = in(aBuilder, aRecipe, tStartIndex, 35, 43);
				tStartIndex = in(aBuilder, aRecipe, tStartIndex, 53, 43);
				break;
			case 10:
				tStartIndex = in(aBuilder, aRecipe, tStartIndex, 17,  7);
				tStartIndex = in(aBuilder, aRecipe, tStartIndex, 35,  7);
				tStartIndex = in(aBuilder, aRecipe, tStartIndex, 53,  7);
				tStartIndex = in(aBuilder, aRecipe, tStartIndex, 17, 25);
				tStartIndex = in(aBuilder, aRecipe, tStartIndex, 35, 25);
				tStartIndex = in(aBuilder, aRecipe, tStartIndex, 53, 25);
				tStartIndex = in(aBuilder, aRecipe, tStartIndex, 17, 43);
				tStartIndex = in(aBuilder, aRecipe, tStartIndex, 35, 43);
				tStartIndex = in(aBuilder, aRecipe, tStartIndex, 53, 43);
				tStartIndex = in(aBuilder, aRecipe, tStartIndex, 53, 61);
				break;
			case 11:
				tStartIndex = in(aBuilder, aRecipe, tStartIndex, 17,  7);
				tStartIndex = in(aBuilder, aRecipe, tStartIndex, 35,  7);
				tStartIndex = in(aBuilder, aRecipe, tStartIndex, 53,  7);
				tStartIndex = in(aBuilder, aRecipe, tStartIndex, 17, 25);
				tStartIndex = in(aBuilder, aRecipe, tStartIndex, 35, 25);
				tStartIndex = in(aBuilder, aRecipe, tStartIndex, 53, 25);
				tStartIndex = in(aBuilder, aRecipe, tStartIndex, 17, 43);
				tStartIndex = in(aBuilder, aRecipe, tStartIndex, 35, 43);
				tStartIndex = in(aBuilder, aRecipe, tStartIndex, 53, 43);
				tStartIndex = in(aBuilder, aRecipe, tStartIndex, 35, 61);
				tStartIndex = in(aBuilder, aRecipe, tStartIndex, 53, 61);
				break;
			default:
				tStartIndex = in(aBuilder, aRecipe, tStartIndex, 17,  7);
				tStartIndex = in(aBuilder, aRecipe, tStartIndex, 35,  7);
				tStartIndex = in(aBuilder, aRecipe, tStartIndex, 53,  7);
				tStartIndex = in(aBuilder, aRecipe, tStartIndex, 17, 25);
				tStartIndex = in(aBuilder, aRecipe, tStartIndex, 35, 25);
				tStartIndex = in(aBuilder, aRecipe, tStartIndex, 53, 25);
				tStartIndex = in(aBuilder, aRecipe, tStartIndex, 17, 43);
				tStartIndex = in(aBuilder, aRecipe, tStartIndex, 35, 43);
				tStartIndex = in(aBuilder, aRecipe, tStartIndex, 53, 43);
				tStartIndex = in(aBuilder, aRecipe, tStartIndex, 17, 61);
				tStartIndex = in(aBuilder, aRecipe, tStartIndex, 35, 61);
				tStartIndex = in(aBuilder, aRecipe, tStartIndex, 53, 61);
				break;
			}

			// gregapi/NEI_RecipeMap.java:280-281.
			if (aRecipe.mSpecialItems instanceof ItemStack && ST.valid((ItemStack)aRecipe.mSpecialItems)) {
				aBuilder.addInputSlot(80, 43).addItemStack((ItemStack)aRecipe.mSpecialItems);
			}
			if (!mMap.mRecipeMachineList.isEmpty()) {
				aBuilder.addInputSlot(152, 83).addItemStacks(mMap.mRecipeMachineList);
			}

			tStartIndex = 0;

			// Портировано 1:1 из gregapi/NEI_RecipeMap.java:285-390 (output-switch по mOutputItemsCount).
			switch (mMap.mOutputItemsCount) {
			case  0:
				break;
			case  1:
				tStartIndex = out(aBuilder, aRecipe, tStartIndex, 107, mMap.mOutputFluidCount>6? 7:25);
				break;
			case  2:
				tStartIndex = out(aBuilder, aRecipe, tStartIndex, 107, mMap.mOutputFluidCount>6? 7:25);
				tStartIndex = out(aBuilder, aRecipe, tStartIndex, 125, mMap.mOutputFluidCount>6? 7:25);
				break;
			case  3:
				tStartIndex = out(aBuilder, aRecipe, tStartIndex, 107, mMap.mOutputFluidCount>6? 7:25);
				tStartIndex = out(aBuilder, aRecipe, tStartIndex, 125, mMap.mOutputFluidCount>6? 7:25);
				tStartIndex = out(aBuilder, aRecipe, tStartIndex, 143, mMap.mOutputFluidCount>6? 7:25);
				break;
			case  4:
				tStartIndex = out(aBuilder, aRecipe, tStartIndex, 107, mMap.mOutputFluidCount>3? 7:16);
				tStartIndex = out(aBuilder, aRecipe, tStartIndex, 125, mMap.mOutputFluidCount>3? 7:16);
				tStartIndex = out(aBuilder, aRecipe, tStartIndex, 107, mMap.mOutputFluidCount>3?25:34);
				tStartIndex = out(aBuilder, aRecipe, tStartIndex, 125, mMap.mOutputFluidCount>3?25:34);
				break;
			case  5:
				tStartIndex = out(aBuilder, aRecipe, tStartIndex, 107, mMap.mOutputFluidCount>3? 7:16);
				tStartIndex = out(aBuilder, aRecipe, tStartIndex, 125, mMap.mOutputFluidCount>3? 7:16);
				tStartIndex = out(aBuilder, aRecipe, tStartIndex, 143, mMap.mOutputFluidCount>3? 7:16);
				tStartIndex = out(aBuilder, aRecipe, tStartIndex, 107, mMap.mOutputFluidCount>3?25:34);
				tStartIndex = out(aBuilder, aRecipe, tStartIndex, 125, mMap.mOutputFluidCount>3?25:34);
				break;
			case  6:
				tStartIndex = out(aBuilder, aRecipe, tStartIndex, 107, mMap.mOutputFluidCount>3? 7:16);
				tStartIndex = out(aBuilder, aRecipe, tStartIndex, 125, mMap.mOutputFluidCount>3? 7:16);
				tStartIndex = out(aBuilder, aRecipe, tStartIndex, 143, mMap.mOutputFluidCount>3? 7:16);
				tStartIndex = out(aBuilder, aRecipe, tStartIndex, 107, mMap.mOutputFluidCount>3?25:34);
				tStartIndex = out(aBuilder, aRecipe, tStartIndex, 125, mMap.mOutputFluidCount>3?25:34);
				tStartIndex = out(aBuilder, aRecipe, tStartIndex, 143, mMap.mOutputFluidCount>3?25:34);
				break;
			case  7:
				tStartIndex = out(aBuilder, aRecipe, tStartIndex, 107,  7);
				tStartIndex = out(aBuilder, aRecipe, tStartIndex, 125,  7);
				tStartIndex = out(aBuilder, aRecipe, tStartIndex, 143,  7);
				tStartIndex = out(aBuilder, aRecipe, tStartIndex, 107, 25);
				tStartIndex = out(aBuilder, aRecipe, tStartIndex, 125, 25);
				tStartIndex = out(aBuilder, aRecipe, tStartIndex, 143, 25);
				tStartIndex = out(aBuilder, aRecipe, tStartIndex, 107, 43);
				break;
			case  8:
				tStartIndex = out(aBuilder, aRecipe, tStartIndex, 107,  7);
				tStartIndex = out(aBuilder, aRecipe, tStartIndex, 125,  7);
				tStartIndex = out(aBuilder, aRecipe, tStartIndex, 143,  7);
				tStartIndex = out(aBuilder, aRecipe, tStartIndex, 107, 25);
				tStartIndex = out(aBuilder, aRecipe, tStartIndex, 125, 25);
				tStartIndex = out(aBuilder, aRecipe, tStartIndex, 143, 25);
				tStartIndex = out(aBuilder, aRecipe, tStartIndex, 107, 43);
				tStartIndex = out(aBuilder, aRecipe, tStartIndex, 125, 43);
				break;
			case  9:
				tStartIndex = out(aBuilder, aRecipe, tStartIndex, 107,  7);
				tStartIndex = out(aBuilder, aRecipe, tStartIndex, 125,  7);
				tStartIndex = out(aBuilder, aRecipe, tStartIndex, 143,  7);
				tStartIndex = out(aBuilder, aRecipe, tStartIndex, 107, 25);
				tStartIndex = out(aBuilder, aRecipe, tStartIndex, 125, 25);
				tStartIndex = out(aBuilder, aRecipe, tStartIndex, 143, 25);
				tStartIndex = out(aBuilder, aRecipe, tStartIndex, 107, 43);
				tStartIndex = out(aBuilder, aRecipe, tStartIndex, 125, 43);
				tStartIndex = out(aBuilder, aRecipe, tStartIndex, 143, 43);
				break;
			case 10:
				tStartIndex = out(aBuilder, aRecipe, tStartIndex, 107,  7);
				tStartIndex = out(aBuilder, aRecipe, tStartIndex, 125,  7);
				tStartIndex = out(aBuilder, aRecipe, tStartIndex, 143,  7);
				tStartIndex = out(aBuilder, aRecipe, tStartIndex, 107, 25);
				tStartIndex = out(aBuilder, aRecipe, tStartIndex, 125, 25);
				tStartIndex = out(aBuilder, aRecipe, tStartIndex, 143, 25);
				tStartIndex = out(aBuilder, aRecipe, tStartIndex, 107, 43);
				tStartIndex = out(aBuilder, aRecipe, tStartIndex, 125, 43);
				tStartIndex = out(aBuilder, aRecipe, tStartIndex, 143, 43);
				tStartIndex = out(aBuilder, aRecipe, tStartIndex, 143, 61);
				break;
			case 11:
				tStartIndex = out(aBuilder, aRecipe, tStartIndex, 107,  7);
				tStartIndex = out(aBuilder, aRecipe, tStartIndex, 125,  7);
				tStartIndex = out(aBuilder, aRecipe, tStartIndex, 143,  7);
				tStartIndex = out(aBuilder, aRecipe, tStartIndex, 107, 25);
				tStartIndex = out(aBuilder, aRecipe, tStartIndex, 125, 25);
				tStartIndex = out(aBuilder, aRecipe, tStartIndex, 143, 25);
				tStartIndex = out(aBuilder, aRecipe, tStartIndex, 107, 43);
				tStartIndex = out(aBuilder, aRecipe, tStartIndex, 125, 43);
				tStartIndex = out(aBuilder, aRecipe, tStartIndex, 143, 43);
				tStartIndex = out(aBuilder, aRecipe, tStartIndex, 125, 61);
				tStartIndex = out(aBuilder, aRecipe, tStartIndex, 143, 61);
				break;
			default:
				tStartIndex = out(aBuilder, aRecipe, tStartIndex, 107,  7);
				tStartIndex = out(aBuilder, aRecipe, tStartIndex, 125,  7);
				tStartIndex = out(aBuilder, aRecipe, tStartIndex, 143,  7);
				tStartIndex = out(aBuilder, aRecipe, tStartIndex, 107, 25);
				tStartIndex = out(aBuilder, aRecipe, tStartIndex, 125, 25);
				tStartIndex = out(aBuilder, aRecipe, tStartIndex, 143, 25);
				tStartIndex = out(aBuilder, aRecipe, tStartIndex, 107, 43);
				tStartIndex = out(aBuilder, aRecipe, tStartIndex, 125, 43);
				tStartIndex = out(aBuilder, aRecipe, tStartIndex, 143, 43);
				tStartIndex = out(aBuilder, aRecipe, tStartIndex, 107, 61);
				tStartIndex = out(aBuilder, aRecipe, tStartIndex, 125, 61);
				tStartIndex = out(aBuilder, aRecipe, tStartIndex, 143, 61);
				break;
			}

			// gregapi/NEI_RecipeMap.java:392-393 (флюидные слоты; проверка "!= null" — как остальной
			// FluidStack[]-код этого порта, gregapi/recipes/Recipe.java:373/382 и др.: пустых слотов
			// это FluidStack[]-хранилище не EMPTY-заполняет, а оставляет настоящим Java null — в отличие
			// от ItemStack, где F15 заменил null на EMPTY).
			// BUG-082: жидкость подаётся ТЕМ ЖЕ приёмом, что в 1.7.10 — предметом-дисплеем GT6, а не родным
			// ингредиентом JEI. Прежняя подача (addIngredient(FLUID_STACK, ...)) теряла ВСЁ, что несёт дисплей:
			// замер живой витрины дал в слоте тултип из ОДНОЙ строки «fluid.steam» (сырой ключ локализации!)
			// против 11 строк дисплея — имя, Amount, Worth, формула, Temperature, State, Density, Viscosity,
			// описание. Заодно исчезал объём: JEI рисует FluidStack долей от чужой ёмкости, поэтому малое
			// количество выглядело «неполным», тогда как у дисплея объём — ЧИСЛО стопки (FL.java:751).
			// Аргументы 1:1 с оригиналом, включая ведёрный масштаб КАЖДОЙ карты (mUseBucketSizeIn/Out).
			for (int i = 0; i < aRecipe.mFluidInputs.length && i < mMap.mInputFluidCount; i++) {
				FluidStack tFluid = aRecipe.mFluidInputs[i];
				if (tFluid == null) continue;
				ItemStack tDisplay = gregapi.data.FL.display(tFluid, true, false, mMap.mUseBucketSizeIn);
				if (tDisplay != null) aBuilder.addInputSlot(53 - (i%3)*18, 63 - (i/3)*18).addItemStack(tDisplay);
			}
			for (int i = 0; i < aRecipe.mFluidOutputs.length && i < mMap.mOutputFluidCount; i++) {
				FluidStack tFluid = aRecipe.mFluidOutputs[i];
				if (tFluid == null) continue;
				ItemStack tDisplay = gregapi.data.FL.display(tFluid, true, false, mMap.mUseBucketSizeOut);
				if (tDisplay != null) aBuilder.addOutputSlot(107 + (i%3)*18, 63 - (i/3)*18).addItemStack(tDisplay);
			}
		} catch (Throwable e) {
			ERR.println("JEI: RecipeMap '" + mMap.mNameInternal + "' failed to lay out a recipe, skipping its slots.");
			e.printStackTrace(ERR);
		}
	}

	/** gregapi/NEI_RecipeMap.java:177 и аналоги: добавляет входной предмет-слот, если он есть, и возвращает следующий индекс. */
	private static int in(IRecipeLayoutBuilder aBuilder, Recipe aRecipe, int aIndex, int aX, int aY) {
		ItemStack tStack = aRecipe.getRepresentativeInput(aIndex);
		if (tStack != null) aBuilder.addInputSlot(aX, aY).addItemStack(tStack);
		return aIndex + 1;
	}

	/** gregapi/NEI_RecipeMap.java:289 и аналоги: добавляет выходной предмет-слот с шансом (как {@code handleItemTooltip}, gregapi/NEI_RecipeMap.java:664-684), возвращает следующий индекс. */
	private static int out(IRecipeLayoutBuilder aBuilder, Recipe aRecipe, int aIndex, int aX, int aY) {
		ItemStack tStack = aRecipe.getOutput(aIndex);
		if (tStack != null) {
			IRecipeSlotBuilder tSlot = aBuilder.addOutputSlot(aX, aY).addItemStack(tStack);
			int tChance = aRecipe.getOutputChance(aIndex), tMax = aRecipe.getMaxChance(aIndex);
			if (tChance > 0 && tChance != tMax) {
				long tPercent = UT.Code.units(tChance, tMax, 10000, F);
				String tSuffix = tStack.getCount() > 1 ? " each" : "";
				tSlot.addRichTooltipCallback((aSlotView, aTooltip) -> aTooltip.add(Component.literal(
					"Chance: " + (tPercent/100) + "." + ((tPercent%100)<10 ? "0"+(tPercent%100) : String.valueOf(tPercent%100)) + "%" + tSuffix)));
			}
		}
		return aIndex + 1;
	}

	/** Портировано 1:1 из gregapi/NEI_RecipeMap.java:687-724 ({@code drawExtras}) — тот же расчёт строк,
	 *  выведенный через нативный текстовый виджет JEI ({@link IRecipeExtrasBuilder#addText}) вместо
	 *  мёртвого F3-superseded {@code drawText} (gregapi/NEI_RecipeMap.java:640-646, no-op). */
	@Override
	public void createRecipeExtras(IRecipeExtrasBuilder aBuilder, Recipe aRecipe, IFocusGroup aFocuses) {
		try {
			List<FormattedText> tLines = new ArrayList<>();
			long tGUt = aRecipe.mEUt, tDuration = aRecipe.mDuration;
			if (tGUt == 0) {
				if (mMap.mShowVoltageAmperageInNEI) tLines.add(Component.literal("Tier: unspecified"));
			} else if (tGUt > 0) {
				tLines.add(Component.literal("Costs: " + UT.Code.makeString(tGUt * tDuration) + " GU"));
				if (mMap.mShowVoltageAmperageInNEI) {
					if (!mMap.mCombinePower) tLines.add(Component.literal("Usage: " + UT.Code.makeString(tGUt) + " GU/t"));
					tLines.add(Component.literal("Tier: " + UT.Code.makeString(tGUt / mMap.mPower) + " GU"));
					tLines.add(Component.literal("Power: " + UT.Code.makeString(mMap.mPower)));
				} else {
					if (tGUt != 1 && !mMap.mCombinePower) tLines.add(Component.literal("Usage: " + UT.Code.makeString(tGUt) + " GU/t"));
				}
			} else {
				long tAbs = -tGUt;
				tLines.add(Component.literal("Gain: " + UT.Code.makeString(tAbs * tDuration) + " GU"));
				if (mMap.mShowVoltageAmperageInNEI) {
					if (!mMap.mCombinePower) tLines.add(Component.literal("Output: " + UT.Code.makeString(tAbs) + " GU/t"));
					tLines.add(Component.literal("Tier: " + UT.Code.makeString(tAbs / mMap.mPower) + " GU"));
					tLines.add(Component.literal("Power: " + UT.Code.makeString(mMap.mPower)));
				} else {
					if (tAbs != 1 && !mMap.mCombinePower) tLines.add(Component.literal("Output: " + UT.Code.makeString(tAbs) + " GU/t"));
				}
			}
			if (tDuration > 0) tLines.add(Component.literal("Time: " + (tDuration < 1200 ? UT.Code.makeString(tDuration) + " ticks" : tDuration < 36000 ? UT.Code.makeString(tDuration/20) + " secs" : UT.Code.makeString(tDuration/1200) + " mins")));
			if (UT.Code.stringValid(mMap.mNEISpecialValuePre) || UT.Code.stringValid(mMap.mNEISpecialValuePost))
				tLines.add(Component.literal(mMap.mNEISpecialValuePre + UT.Code.makeString(aRecipe.mSpecialValue * mMap.mNEISpecialValueMultiplier) + mMap.mNEISpecialValuePost));
			// NEI drawText @(10,73..123) шаг 10, чёрный без тени; addText(w,h)+setPosition (было (4,96) — 4px ширины
			// давали текст-СТОЛБИК по букве, «Costs: 2…» вертикально)
			if (!tLines.isEmpty()) aBuilder.addText(tLines, WIDTH - (10) - 4, 60)
				.setPosition(15, 84).setColor(0xFF000000).setShadow(false).setLineSpacing(2);
		} catch (Throwable e) {
			ERR.println("JEI: RecipeMap '" + mMap.mNameInternal + "' failed to build its info text, skipping.");
			e.printStackTrace(ERR);
		}
	}
}

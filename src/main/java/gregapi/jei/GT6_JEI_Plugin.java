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
import gregapi.recipes.Recipe;
import gregapi.recipes.Recipe.RecipeMap;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static gregapi.data.CS.*;

/**
 * ЕДИНЫЙ центр JEI-совместимости GT6 (Ф1.3, decisions/ROADMAP.md §Ф1). Заменяет старый NEI-мост
 * ({@link gregapi.NEI_RecipeMap}/{@link gregapi.NEI_GT_API_Config}, codechicken.nei, недоступен на neo)
 * той же ролью: читает ЕДИНСТВЕННЫЙ центр рецептов GT6 — {@link RecipeMap#RECIPE_MAP_LIST} — и выводит
 * его в JEI. Никаких параллельных данных: категория на карту, рецепты — прямо из
 * {@link RecipeMap#getNEIAllRecipes()} (тот же метод, что дёргал старый NEI-обработчик,
 * gregapi/NEI_RecipeMap.java:523), катализаторы — из {@link RecipeMap#mRecipeMachineList}.
 *
 * Client-only: обнаруживается самой JEI по аннотации {@link JeiPlugin} (ServiceLoader/ASM-скан модов,
 * без ручной регистрации на mod-bus — см. mezz.jei.api.IModPlugin, JEI сама решает, на какой стороне
 * инстанциировать плагин; выделен в отдельный пакет {@code gregapi.jei}, чтобы не тянуть JEI-типы в
 * общий код).
 *
 * PORT-TODO(F11, Ф1.3-crafting-jei): крафт-рецепты GT6 ({@code CR.BUFFER}/{@code ICraftingRecipeGT},
 * F11-диспетчер {@code decisions/F11-crafting-recipe.md}) в JEI НЕ выведены. Причина: они не являются
 * neo {@code CraftingRecipe} (см. javadoc {@code ICraftingRecipeGT}: "F11 — собственный крафт-контракт
 * ... НЕ neo Recipe"), поэтому не попадают в {@code RecipeTypes.CRAFTING} автоматически; а собственная
 * generic-категория (по образцу {@link GT6_JEI_RecipeCategory}) требует ширины/высоты сетки, которых
 * {@code ShapedOreRecipe}/{@code ShapelessOreRecipe} (единственные конкретные реализации с публичным
 * {@code getInput()}) НЕ выставляют публично (`gregapi/recipes/ShapedOreRecipe.java`: {@code mWidth}/
 * {@code mHeight} — protected; `gregapi/recipes/ICraftingRecipeGT.java` — общий контракт вовсе без
 * {@code getInput()}). Требует либо нового public-геттера на F11-файлах (вне периметра этой задачи —
 * F11 уже закрытый шов, decisions/F11-crafting-recipe.md), либо полноценного {@code CraftingRecipe}-
 * адаптера. Машинные рецепты (RecipeMap→JEI) выше не затронуты и работают независимо.
 */
@JeiPlugin
public final class GT6_JEI_Plugin implements IModPlugin {
	public static final Identifier PLUGIN_UID = Identifier.fromNamespaceAndPath(MD.GT.mID, "jei_plugin");

	/** RecipeMap -> её уникальный JEI-RecipeType. Заполняется в {@link #registerCategories}, читается в {@link #registerRecipes}/{@link #registerRecipeCatalysts}. */
	private final Map<RecipeMap, RecipeType<Recipe>> mTypes = new LinkedHashMap<>();
	/** RecipeMap -> её видимые рецепты ({@code mEnabled && !mHidden}, gregapi/recipes/Recipe.java:564), посчитанные ОДИН раз в {@link #registerCategories} и переиспользуемые в {@link #registerRecipes} (не плодим параллельный пересчёт). */
	private final Map<RecipeMap, List<Recipe>> mRecipes = new LinkedHashMap<>();

	@Override
	public Identifier getPluginUid() {
		return PLUGIN_UID;
	}

	@Override
	public void registerCategories(IRecipeCategoryRegistration aRegistration) {
		mTypes.clear();
		mRecipes.clear();
		IGuiHelper tGuiHelper = aRegistration.getJeiHelpers().getGuiHelper();
		List<IRecipeCategory<?>> tCategories = new ArrayList<>();
		// Единственный центр рецептов GT6 (как старый NEI_GT_API_Config.java:70): каждая карта с
		// mNEIAllowed==true — своя JEI-категория, но только если у неё реально есть видимые рецепты
		// (ROADMAP.md 1.3: "JEI-категорий == непустых карт").
		for (RecipeMap tMap : RecipeMap.RECIPE_MAP_LIST) {
			if (!tMap.mNEIAllowed) continue;
			try {
				List<Recipe> tRecipeList = tMap.getNEIAllRecipes();
				if (tRecipeList.isEmpty()) continue;
				RecipeType<Recipe> tType = RecipeType.create(MD.GT.mID, tMap.mNameNEI, Recipe.class);
				mTypes.put(tMap, tType);
				mRecipes.put(tMap, tRecipeList);
				tCategories.add(new GT6_JEI_RecipeCategory(tMap, tType, tGuiHelper));
			} catch (Throwable e) {
				ERR.println("JEI: RecipeMap '" + tMap.mNameInternal + "' failed to register as a category, skipping.");
				e.printStackTrace(ERR);
			}
		}
		aRegistration.addRecipeCategories(tCategories.toArray(new IRecipeCategory<?>[0]));
	}

	@Override
	public void registerRecipes(IRecipeRegistration aRegistration) {
		for (Map.Entry<RecipeMap, RecipeType<Recipe>> tEntry : mTypes.entrySet()) {
			try {
				aRegistration.addRecipes(tEntry.getValue(), mRecipes.get(tEntry.getKey()));
			} catch (Throwable e) {
				ERR.println("JEI: RecipeMap '" + tEntry.getKey().mNameInternal + "' failed to register its recipes, skipping.");
				e.printStackTrace(ERR);
			}
		}
	}

	@Override
	public void registerRecipeCatalysts(IRecipeCatalystRegistration aRegistration) {
		for (Map.Entry<RecipeMap, RecipeType<Recipe>> tEntry : mTypes.entrySet()) {
			try {
				List<ItemStack> tMachines = tEntry.getKey().mRecipeMachineList;
				if (!tMachines.isEmpty()) aRegistration.addCraftingStation(tEntry.getValue(), tMachines.toArray(new ItemStack[0]));
			} catch (Throwable e) {
				ERR.println("JEI: RecipeMap '" + tEntry.getKey().mNameInternal + "' failed to register its catalysts, skipping.");
				e.printStackTrace(ERR);
			}
		}
	}
}

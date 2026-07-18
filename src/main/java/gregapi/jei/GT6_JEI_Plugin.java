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
import gregapi.recipes.Recipe;
import gregapi.recipes.Recipe.RecipeMap;
import gregapi.recipes.ShapedOreRecipe;
import gregapi.recipes.ShapelessOreRecipe;
import gregapi.util.CR;
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
import net.minecraft.world.level.block.Blocks;

import java.util.ArrayList;
import java.util.Collections;
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
 * <p>Крафт-рецепты GT6 (F11-буфер {@code CR.BUFFER}/{@code ICraftingRecipeGT}, диспетчер
 * {@code CustomRecipe}, decisions/F11-crafting-recipe.md §Ф1.3-crafting-jei) — тот же {@code CR.list()},
 * своя категория {@link GT6_JEI_CraftingCategory} (см. её javadoc: почему не встроенная
 * {@code RecipeTypes.CRAFTING}, и как она переиспользует нативный JEI {@code ICraftingGridHelper}).</p>
 */
@JeiPlugin
public final class GT6_JEI_Plugin implements IModPlugin {
	public static final Identifier PLUGIN_UID = Identifier.fromNamespaceAndPath(MD.GT.mID, "jei_plugin");

	/** RecipeMap -> её уникальный JEI-RecipeType. Заполняется в {@link #registerCategories}, читается в {@link #registerRecipes}/{@link #registerRecipeCatalysts}. */
	private final Map<RecipeMap, RecipeType<Recipe>> mTypes = new LinkedHashMap<>();
	/** RecipeMap -> её видимые рецепты ({@code mEnabled && !mHidden}, gregapi/recipes/Recipe.java:564), посчитанные ОДИН раз в {@link #registerCategories} и переиспользуемые в {@link #registerRecipes} (не плодим параллельный пересчёт). */
	private final Map<RecipeMap, List<Recipe>> mRecipes = new LinkedHashMap<>();
	/** Ф1.3-crafting-jei: F11-буфер {@code CR.list()}, отфильтрованный до {@link ShapedOreRecipe}/{@link ShapelessOreRecipe}-наследников
	 *  (1:1 с тем, что показывал NEI — см. {@link GT6_JEI_CraftingCategory} javadoc), посчитан ОДИН раз в {@link #registerCategories}. */
	private List<ICraftingRecipeGT> mCraftingRecipes = Collections.emptyList();

	@Override
	public Identifier getPluginUid() {
		return PLUGIN_UID;
	}

	/** F1-jei: JEI различает варианты предмета только по ЗАЯВЛЕННЫМ компонентам (registerFromDataComponentTypes);
	 *  без заявки SUBTYPE все процедурные варианты (itemDamage 1.7.10) схлопываются в один предмет
	 *  (лог-улика «289 duplicate items», ingredient-лист пуст по моду). Заявляем SUBTYPE всем gt-предметам. */
	@Override
	public void registerItemSubtypes(mezz.jei.api.registration.ISubtypeRegistration aRegistration) {
		if (!gregapi.GT_API.SUBTYPE.isBound()) return;
		net.minecraft.core.component.DataComponentType<?> tSubtype = gregapi.GT_API.SUBTYPE.get();
		int tCount = 0;
		for (net.minecraft.world.item.Item tItem : net.minecraft.core.registries.BuiltInRegistries.ITEM) {
			Identifier tKey = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(tItem);
			if (tKey == null) continue;
			String tNs = tKey.getNamespace();
			if (!tNs.equals(ModIDs.GT) && !tNs.equals("gregtech") && !tNs.equals("gregapi")) continue;
			try {aRegistration.registerFromDataComponentTypes(tItem, tSubtype); tCount++;} catch (Throwable e) {/**/}
		}
		OUT.println("[GT6-JEI] SUBTYPE-подтипы заявлены для " + tCount + " предметов.");
	}

	@Override
	public void registerCategories(IRecipeCategoryRegistration aRegistration) {
		mTypes.clear();
		mRecipes.clear();
		mCraftingRecipes = Collections.emptyList();
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

		// Ф1.3-crafting-jei: крафт-верстак GT6 (F11-буфер) — своя категория, см. GT6_JEI_CraftingCategory
		// javadoc. Показываются только Shaped/ShapelessOreRecipe-наследники (1:1 с тем, что NEI мог отрисовать).
		try {
			List<ICraftingRecipeGT> tCraftingList = new ArrayList<>();
			for (ICraftingRecipeGT tRecipe : CR.list()) {
				if (tRecipe == null) continue;
				if (tRecipe instanceof ShapedOreRecipe || tRecipe instanceof ShapelessOreRecipe) tCraftingList.add(tRecipe);
			}
			if (!tCraftingList.isEmpty()) {
				mCraftingRecipes = tCraftingList;
				tCategories.add(new GT6_JEI_CraftingCategory(tGuiHelper));
			}
		} catch (Throwable e) {
			ERR.println("JEI: GT6 crafting-table category failed to register, skipping.");
			e.printStackTrace(ERR);
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

		if (!mCraftingRecipes.isEmpty()) {
			try {
				aRegistration.addRecipes(GT6_JEI_CraftingCategory.TYPE, mCraftingRecipes);
			} catch (Throwable e) {
				ERR.println("JEI: GT6 crafting-table recipes failed to register, skipping.");
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

		if (!mCraftingRecipes.isEmpty()) {
			try {
				aRegistration.addCraftingStation(GT6_JEI_CraftingCategory.TYPE, Blocks.CRAFTING_TABLE);
			} catch (Throwable e) {
				ERR.println("JEI: GT6 crafting-table category failed to register its catalyst, skipping.");
				e.printStackTrace(ERR);
			}
		}
	}
}

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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static gregapi.data.CS.*;

/** The single center of JEI compatibility, replacing the old NEI bridge with the same role: it reads GT6's one recipe
 *  center and feeds JEI, with no parallel data of its own. Client-only, discovered by JEI's own plugin scan. */
@JeiPlugin
public final class GT6_JEI_Plugin implements IModPlugin {
	public static final ResourceLocation PLUGIN_UID = new ResourceLocation(MD.GT.mID, "jei_plugin");

	/** Maps each RecipeMap to its own JEI RecipeType; filled in registerCategories, read by the two methods below. */
	private final Map<RecipeMap, RecipeType<Recipe>> mTypes = new LinkedHashMap<>();

	/** The live JEI runtime is the only door to opening the recipe screen; JEI hands it over once via onRuntimeAvailable.
	 *  Held here rather than in a second holder, since this class already is the JEI-compat center. */
	public static volatile mezz.jei.api.runtime.IJeiRuntime sRuntime = null;

	/** What the crafting showcase shows/hides is fixed at the moment categories are built, since it remembers cell contents
	 *  from then on, not live -- see GT6_JEI_CraftingCategory. */
	public static int sShownAtRegistration = -1, sHiddenAtRegistration = -1;

	public static final List<String> sHiddenExamples = new ArrayList<>();
	/** The same key 1.7.10 used to open NEI now opens the matching JEI category; static, since the caller can't see the plugin. */
	private static final Map<String, RecipeType<Recipe>> sTypesByName = new java.util.concurrent.ConcurrentHashMap<>();
	/** Returns false if JEI never came up or no such category exists -- the same semantics the old, now-dead NEI call had. */
	public static boolean showRecipeCategory(String aNameNEI) {
		mezz.jei.api.runtime.IJeiRuntime tRuntime = sRuntime;
		if (tRuntime == null || aNameNEI == null) return F;
		RecipeType<Recipe> tType = sTypesByName.get(aNameNEI);
		if (tType == null) return F;
		try {tRuntime.getRecipesGui().showTypes(java.util.List.of(tType)); return T;}
		catch (Throwable e) {ERR.println("JEI: не удалось открыть категорию '" + aNameNEI + "'"); e.printStackTrace(ERR); return F;}
	}
	/** RecipeMap to its visible recipes, counted once in registerCategories and reused rather than recomputed. */
	private final Map<RecipeMap, List<Recipe>> mRecipes = new LinkedHashMap<>();
	/** CR.list() filtered to ShapedOreRecipe/ShapelessOreRecipe descendants, 1:1 with what NEI used to show, counted once. */
	private List<ICraftingRecipeGT> mCraftingRecipes = Collections.emptyList();

	@Override
	public ResourceLocation getPluginUid() {
		return PLUGIN_UID;
	}

	/** GT6's item subtype lives in damage again on 1.20.1, so JEI needs its own string interpreter or every procedural
	 *  variant collapses into one item; the string is meta plus the NBT tag, the same two features NEI used. */
	@Override
	public void registerItemSubtypes(mezz.jei.api.registration.ISubtypeRegistration aRegistration) {
		int tCount = 0, tMetaOnly = 0;
		for (net.minecraft.world.item.Item tItem : net.minecraft.core.registries.BuiltInRegistries.ITEM) {
			ResourceLocation tKey = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(tItem);
			if (tKey == null) continue;
			String tNs = tKey.getNamespace();
			if (!tNs.equals(ModIDs.GT) && !tNs.equals("gregtech") && !tNs.equals("gregapi")) continue;
			try {
				// The identity rule isn't this class's to make: it asks the central ST, keeping no copy of the rule here.
				final boolean tWithNBT = gregapi.util.ST.identityIncludesNBT(tItem);
				aRegistration.registerSubtypeInterpreter(tItem, (aStack, aContext) -> {
					String rID = Short.toString(gregapi.util.ST.meta_(aStack));
					if (tWithNBT && aStack.hasTag()) rID = rID + "|" + aStack.getTag();
					return rID;
				});
				if (tWithNBT) tCount++; else tMetaOnly++;
			} catch (Throwable e) {/**/}
		}
		OUT.println("[GT6-JEI] подтипы мета+NBT заявлены для " + tCount + " предметов, только мета — для " + tMetaOnly + " (инструменты).");
	}


	@Override
	public void registerCategories(IRecipeCategoryRegistration aRegistration) {
		mTypes.clear();
		sTypesByName.clear();
		mRecipes.clear();
		mCraftingRecipes = Collections.emptyList();
		IGuiHelper tGuiHelper = aRegistration.getJeiHelpers().getGuiHelper();
		List<IRecipeCategory<?>> tCategories = new ArrayList<>();
		// Each recipe map with mNEIAllowed==true gets its own JEI category, but only when it actually has visible recipes.
		for (RecipeMap tMap : RecipeMap.RECIPE_MAP_LIST) {
			if (!tMap.mNEIAllowed) continue;
			try {
				List<Recipe> tRecipeList = tMap.getNEIAllRecipes();
				if (tRecipeList.isEmpty()) continue;
				RecipeType<Recipe> tType = RecipeType.create(MD.GT.mID, tMap.mNameNEI, Recipe.class);
				mTypes.put(tMap, tType);
				sTypesByName.put(tMap.mNameNEI, tType); // The same key that used to open NEI in 1.7.10.
				mRecipes.put(tMap, tRecipeList);
				tCategories.add(new GT6_JEI_RecipeCategory(tMap, tType, tGuiHelper));
			} catch (Throwable e) {
				ERR.println("JEI: RecipeMap '" + tMap.mNameInternal + "' failed to register as a category, skipping.");
				e.printStackTrace(ERR);
			}
		}

		// Set once categories are actually built, not by checking ModList: the recipe viewer being present in the mod list doesn't
		// mean there's anything to open, exactly the same distinction 1.7.10 drew for its own NEI plugin flag.
		gregapi.data.CS.NEI = T;

		// GT6's own crafting types (AdvancedCrafting1ToY/XToY) get their own category too, since even the old recipe viewer
		// only ever drew shaped/shapeless; showing cell placement here is necessary, or the mechanic wouldn't be visible at all.
		try {
			List<ICraftingRecipeGT> tCraftingList = new ArrayList<>();
			int tHidden = 0;
			for (ICraftingRecipeGT tRecipe : CR.list()) {
				if (tRecipe == null) continue;
				if (tRecipe instanceof ShapedOreRecipe || tRecipe instanceof ShapelessOreRecipe
				 || tRecipe instanceof gregapi.recipes.AdvancedCrafting1ToY || tRecipe instanceof gregapi.recipes.AdvancedCraftingXToY) {
					// A recipe with an empty variant list in some cell was never drawn by the old viewer either; only the display is hidden,
					// the recipe itself stays live in the buffer and the crafting bench.
					if (!GT6_JEI_CraftingCategory.showable(tRecipe)) {
						tHidden++;
						if (sHiddenExamples.size() < 12) try {
							ItemStack tOut = tRecipe.getRecipeOutput();
							sHiddenExamples.add(tRecipe.getClass().getSimpleName() + " -> " + (tOut == null ? "null" : gregapi.util.ST.identityKey(tOut)));
						} catch (Throwable e) {/**/}
						continue;
					}
					tCraftingList.add(tRecipe);
				}
			}
			sShownAtRegistration = tCraftingList.size();
			sHiddenAtRegistration = tHidden;
			OUT.println("[GT6-JEI] крафт-верстак: показываем " + tCraftingList.size() + " рецептов, скрыто как в NEI 1.7.10 (нечем нарисовать ячейку) — " + tHidden);
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

	/** JEI registers every source fluid under its own native ingredient type too, doubling up with GT6's own fluid-display
	 *  items; the old recipe viewer never had that type, so its native fluid layer is removed here, 1:1 with the old view. */
	@Override
	public void onRuntimeAvailable(mezz.jei.api.runtime.IJeiRuntime aRuntime) {
		sRuntime = aRuntime; // The only door to the recipe screen; see showRecipeCategory.
		try {
			mezz.jei.api.runtime.IIngredientManager tManager = aRuntime.getIngredientManager();
			java.util.Collection<net.minecraftforge.fluids.FluidStack> tFluids = new java.util.ArrayList<>(tManager.getAllIngredients(mezz.jei.api.forge.ForgeTypes.FLUID_STACK));
			if (!tFluids.isEmpty()) tManager.removeIngredientsAtRuntime(mezz.jei.api.forge.ForgeTypes.FLUID_STACK, tFluids);
			OUT.println("[GT6-JEI] родной FLUID_STACK-пласт снят из панели: было " + tFluids.size() + ", осталось " + tManager.getAllIngredients(mezz.jei.api.forge.ForgeTypes.FLUID_STACK).size() + " (жидкости показывает GT6-дисплей, как NEI 1.7.10)");
		} catch (Throwable e) {
			ERR.println("JEI: не удалось снять родной FLUID_STACK-пласт (дубль жидкостей останется в панели).");
			e.printStackTrace(ERR);
		}
	}

	@Override
	public void registerRecipeCatalysts(IRecipeCatalystRegistration aRegistration) {
		for (Map.Entry<RecipeMap, RecipeType<Recipe>> tEntry : mTypes.entrySet()) {
			try {
				List<ItemStack> tMachines = tEntry.getKey().mRecipeMachineList;
				if (!tMachines.isEmpty()) aRegistration.addRecipeCatalysts(tEntry.getValue(), tMachines.toArray(new ItemStack[0]));
			} catch (Throwable e) {
				ERR.println("JEI: RecipeMap '" + tEntry.getKey().mNameInternal + "' failed to register its catalysts, skipping.");
				e.printStackTrace(ERR);
			}
		}

		if (!mCraftingRecipes.isEmpty()) {
			try {
				aRegistration.addRecipeCatalysts(GT6_JEI_CraftingCategory.TYPE, Blocks.CRAFTING_TABLE);
			} catch (Throwable e) {
				ERR.println("JEI: GT6 crafting-table category failed to register its catalyst, skipping.");
				e.printStackTrace(ERR);
			}
		}
	}
}

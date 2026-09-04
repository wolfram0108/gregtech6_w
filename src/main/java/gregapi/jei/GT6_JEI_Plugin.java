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
 * The SINGLE center of GT6's JEI compatibility (phase 1 of the roadmap, decisions/ROADMAP.md). Replaces the old NEI bridge
 * ({@link gregapi.NEI_RecipeMap}/{@link gregapi.NEI_GT_API_Config}, codechicken.nei, unavailable on neo)
 * with the same role: reads the ONE-AND-ONLY GT6 recipe center — {@link RecipeMap#RECIPE_MAP_LIST} — and exposes
 * it to JEI. No parallel data: a category per map, recipes straight from
 * {@link RecipeMap#getNEIAllRecipes()} (the same method the old NEI handler called,
 * gregapi/NEI_RecipeMap.java:523), catalysts from {@link RecipeMap#mRecipeMachineList}.
 *
 * Client-only: discovered by JEI itself via the {@link JeiPlugin} annotation (ServiceLoader/ASM mod scan,
 * no manual mod-bus registration — see mezz.jei.api.IModPlugin, JEI decides on which side to
 * instantiate the plugin; split into its own {@code gregapi.jei} package so JEI types don't leak into
 * the common code).
 *
 * <p>GT6 crafting recipes (F11 buffer {@code CR.BUFFER}/{@code ICraftingRecipeGT}, the
 * {@code CustomRecipe} dispatcher, decisions/F11-crafting-recipe.md, section 10) — the same {@code CR.list()},
 * its own category {@link GT6_JEI_CraftingCategory} (see its javadoc: why not the built-in
 * {@code RecipeTypes.CRAFTING}, and how it reuses the native JEI {@code ICraftingGridHelper}).</p>
 */
@JeiPlugin
public final class GT6_JEI_Plugin implements IModPlugin {
	public static final Identifier PLUGIN_UID = Identifier.fromNamespaceAndPath(MD.GT.mID, "jei_plugin");

	/** RecipeMap -> its unique JEI RecipeType. Filled in {@link #registerCategories}, read in {@link #registerRecipes}/{@link #registerRecipeCatalysts}. */
	private final Map<RecipeMap, RecipeType<Recipe>> mTypes = new LinkedHashMap<>();

	/** BUG-056: the live JEI runtime — the only door through which the recipe screen can be OPENED.
	 *  JEI hands it over once ({@link #onRuntimeAvailable}); kept here because this class is
	 *  the JEI compatibility center (see the class docstring), so we don't add a second holder. */
	public static volatile mezz.jei.api.runtime.IJeiRuntime sRuntime = null;

	/** Crafting showcase guard: what was shown and what was hidden AT THE MOMENT categories were built (the
	 *  moment matters — the showcase is built once and remembers cell contents, see GT6_JEI_CraftingCategory). */
	public static int sShownAtRegistration = -1, sHiddenAtRegistration = -1;

	public static final List<String> sHiddenExamples = new ArrayList<>();
	/** BUG-056: {@code RecipeMap.mNameNEI} -> category type. The same key 1.7.10 used to call NEI
	 *  ({@code GuiCraftingRecipe.openRecipeGui(mNameNEI)}) now opens the JEI category. Static,
	 *  because the caller ({@code RecipeMap.openNEI}) never sees the plugin instance. */
	private static final Map<String, RecipeType<Recipe>> sTypesByName = new java.util.concurrent.ConcurrentHashMap<>();
	/** BUG-056: open the recipe screen by map name. Returns {@code false} if JEI didn't come up or
	 *  no such category exists — exactly the same semantics as the dead NEI call (it also returned false). */
	public static boolean showRecipeCategory(String aNameNEI) {
		mezz.jei.api.runtime.IJeiRuntime tRuntime = sRuntime;
		if (tRuntime == null || aNameNEI == null) return F;
		RecipeType<Recipe> tType = sTypesByName.get(aNameNEI);
		if (tType == null) return F;
		try {tRuntime.getRecipesGui().showTypes(java.util.List.of(tType)); return T;}
		catch (Throwable e) {ERR.println("JEI: could not open category '" + aNameNEI + "'"); e.printStackTrace(ERR); return F;}
	}
	/** RecipeMap -> its visible recipes ({@code mEnabled && !mHidden}, gregapi/recipes/Recipe.java:564), computed ONCE in {@link #registerCategories} and reused in {@link #registerRecipes} (no parallel recomputation). */
	private final Map<RecipeMap, List<Recipe>> mRecipes = new LinkedHashMap<>();
	/** Crafting recipes in JEI: the F11 buffer {@code CR.list()}, filtered down to {@link ShapedOreRecipe}/{@link ShapelessOreRecipe} descendants
	 *  (1:1 with what NEI used to show — see the {@link GT6_JEI_CraftingCategory} javadoc), computed ONCE in {@link #registerCategories}. */
	private List<ICraftingRecipeGT> mCraftingRecipes = Collections.emptyList();

	@Override
	public Identifier getPluginUid() {
		return PLUGIN_UID;
	}

	/** F1-jei: JEI tells item variants apart only by DECLARED components (registerFromDataComponentTypes);
	 *  without a SUBTYPE declaration all procedural variants (1.7.10 itemDamage) collapse into one item
	 *  (log evidence: "289 duplicate items", the ingredient list is empty for the mod). Declare SUBTYPE for every gt item. */
	@Override
	public void registerItemSubtypes(mezz.jei.api.registration.ISubtypeRegistration aRegistration) {
		if (!gregapi.GT_API.SUBTYPE.isBound()) return;
		net.minecraft.core.component.DataComponentType<?> tSubtype = gregapi.GT_API.SUBTYPE.get();
		int tCount = 0, tMetaOnly = 0;
		for (net.minecraft.world.item.Item tItem : net.minecraft.core.registries.BuiltInRegistries.ITEM) {
			Identifier tKey = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(tItem);
			if (tKey == null) continue;
			String tNs = tKey.getNamespace();
			if (!tNs.equals(ModIDs.GT) && !tNs.equals("gregtech") && !tNs.equals("gregapi")) continue;
			// SUBTYPE (1.7.10 meta) + CUSTOM_DATA (F8 ItemNBT center: coins/batteries/chests differ by NBT material,
			// not meta — 1.7.10 NEI told them apart by NBT; without declaring it — "389 duplicate items" for Coins)
			try {
				// the identity rule isn't ours: we ask the ST center (BUG-079), the showcase keeps no copy of its own
				if (gregapi.util.ST.identityIncludesNBT(tItem)) {aRegistration.registerFromDataComponentTypes(tItem, tSubtype, net.minecraft.core.component.DataComponents.CUSTOM_DATA); tCount++;}
				else {aRegistration.registerFromDataComponentTypes(tItem, tSubtype); tMetaOnly++;}
			} catch (Throwable e) {/**/}
		}
		OUT.println("[GT6-JEI] SUBTYPE+CUSTOM_DATA subtypes declared for " + tCount + " items, SUBTYPE only for " + tMetaOnly + " (tools).");
	}


	@Override
	public void registerCategories(IRecipeCategoryRegistration aRegistration) {
		mTypes.clear();
		sTypesByName.clear();
		mRecipes.clear();
		mCraftingRecipes = Collections.emptyList();
		IGuiHelper tGuiHelper = aRegistration.getJeiHelpers().getGuiHelper();
		List<IRecipeCategory<?>> tCategories = new ArrayList<>();
		// GT6's single recipe center (like the old NEI_GT_API_Config.java:70): every map with
		// mNEIAllowed==true gets its own JEI category, but only if it actually has visible recipes
		// (ROADMAP.md 1.3: "JEI categories == non-empty maps").
		for (RecipeMap tMap : RecipeMap.RECIPE_MAP_LIST) {
			if (!tMap.mNEIAllowed) continue;
			try {
				List<Recipe> tRecipeList = tMap.getNEIAllRecipes();
				if (tRecipeList.isEmpty()) continue;
				RecipeType<Recipe> tType = RecipeType.create(MD.GT.mID, tMap.mNameNEI, Recipe.class);
				mTypes.put(tMap, tType);
				sTypesByName.put(tMap.mNameNEI, tType); // BUG-056: the same key 1.7.10 used to call NEI
				mRecipes.put(tMap, tRecipeList);
				tCategories.add(new GT6_JEI_RecipeCategory(tMap, tType, tGuiHelper));
			} catch (Throwable e) {
				ERR.println("JEI: RecipeMap '" + tMap.mNameInternal + "' failed to register as a category, skipping.");
				e.printStackTrace(ERR);
			}
		}

		// BUG-056: a recipe viewer EXISTS — so the "show recipes" icon can be drawn.
		// In 1.7.10 the same flag was set by GT6's NEI plugin (NEI_GT_API_Config.loadConfig/run) because NEI was
		// an OPTIONAL mod; in 26.1.2 JEI is just as optional, and the fact that "the plugin loaded"
		// is the exact analogue. Set it here rather than from ModList: what matters is not the mod's presence in
		// the list, but that categories were actually built and there is something to open.
		gregapi.data.CS.NEI = T;

		// Crafting recipes in JEI: the GT6 crafting table (F11 buffer) gets its own category, see the GT6_JEI_CraftingCategory javadoc.
		// BUG-099 (user requirement "recipes must be 100% present in the showcase"): BOTH of GT6's own standalone types
		// are added alongside Shaped/Shapeless — AdvancedCrafting1ToY (one item, the product is chosen by CELL) and
		// AdvancedCraftingXToY (X prefix items → Y output). Even 1.7.10's NEI never showed these: it only knew how to
		// draw shaped/shapeless, while these implement ICraftingRecipeGT directly. Here the showcase GOES BEYOND
		// the original — it also shows the layout, i.e. which cell to place what in, otherwise the mechanic can't be seen at all.
		try {
			List<ICraftingRecipeGT> tCraftingList = new ArrayList<>();
			int tHidden = 0;
			for (ICraftingRecipeGT tRecipe : CR.list()) {
				if (tRecipe == null) continue;
				if (tRecipe instanceof ShapedOreRecipe || tRecipe instanceof ShapelessOreRecipe
				 || tRecipe instanceof gregapi.recipes.AdvancedCrafting1ToY || tRecipe instanceof gregapi.recipes.AdvancedCraftingXToY) {
					// The show rule isn't ours, it's 1.7.10's: the showcase of that time never drew a recipe with an
					// EMPTY variant list in a cell (see GT6_JEI_CraftingCategory.showable). The mechanic is untouched:
					// the recipe stays in CR.BUFFER and on the crafting table, only its display is hidden.
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
			OUT.println("[GT6-JEI] crafting table: showing " + tCraftingList.size() + " recipes, hidden as in NEI 1.7.10 (nothing to draw the cell with) — " + tHidden);
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

	/** BUG-030 v2 (player report: "the old fluid layer stayed as a duplicate"): JEI itself registers ALL source
	 *  fluids of the registry under the native FLUID_STACK ingredient type ({@code FluidStackListFactory.create}: Registry.listElements →
	 *  filter isSource — the same ~679 set as the GT6 displays) → the ingredient panel showed fluids TWICE.
	 *  1.7.10's NEI had NO fluid ingredient type at all — there was only one layer, the GT6 displays (with a rich tooltip,
	 *  ItemFluidDisplay.addInformation). We remove JEI's native layer entirely — 1:1 with the NEI look; GT6's recipe
	 *  categories show fluids as display items ({@code FL.display}), they don't need FLUID_STACK ingredients. */
	@Override
	public void onRuntimeAvailable(mezz.jei.api.runtime.IJeiRuntime aRuntime) {
		sRuntime = aRuntime; // BUG-056: the only door to the recipe screen, see showRecipeCategory
		try {
			mezz.jei.api.runtime.IIngredientManager tManager = aRuntime.getIngredientManager();
			java.util.Collection<net.neoforged.neoforge.fluids.FluidStack> tFluids = new java.util.ArrayList<>(tManager.getAllIngredients(mezz.jei.api.neoforge.NeoForgeTypes.FLUID_STACK));
			if (!tFluids.isEmpty()) tManager.removeIngredientsAtRuntime(mezz.jei.api.neoforge.NeoForgeTypes.FLUID_STACK, tFluids);
			OUT.println("[GT6-JEI] native FLUID_STACK layer removed from the panel: was " + tFluids.size() + ", now " + tManager.getAllIngredients(mezz.jei.api.neoforge.NeoForgeTypes.FLUID_STACK).size() + " (fluids are shown by the GT6 display, as in NEI 1.7.10)");
		} catch (Throwable e) {
			ERR.println("JEI: could not remove the native FLUID_STACK layer (a fluid duplicate will remain in the panel).");
			e.printStackTrace(ERR);
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

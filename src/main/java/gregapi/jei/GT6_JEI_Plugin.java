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
 * it to JEI. No parallel data: a category per map, recipes asked per lookup exactly as the old NEI handler asked them
 * ({@link RecipeMap#getNEIRecipesFor}/{@link RecipeMap#getNEIUsagesFor}/{@link RecipeMap#getNEIAllRecipes}, see
 * {@link #registerAdvanced}), catalysts from {@link RecipeMap#mRecipeMachineList}.
 *
 * Client-only: discovered by JEI itself via the {@link JeiPlugin} annotation (ServiceLoader/ASM mod scan,
 * no manual mod-bus registration — see mezz.jei.api.IModPlugin, JEI decides on which side to
 * instantiate the plugin; split into its own {@code gregapi.jei} package so JEI types don't leak into
 * the common code).
 *
 * <p>GT6 crafting recipes (F11 buffer {@code CR.BUFFER}/{@code ICraftingRecipeGT}, the
 * {@code CustomRecipe} dispatcher, decisions/F11-crafting-recipe.md, section 10) — the same {@code CR.list()},
 * shown as ordinary crafting recipes of JEI's native crafting category: {@link GT6_JEI_CraftingRecipes} turns
 * them into display-only crafting recipe entries, and the vanilla originals of the F4 role-C replacements are
 * hidden by id, so the player sees one page per recipe as in 1.7.10 (issue #45).</p>
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

	/** Crafting showcase guard: how many buffer recipes the viewer shows and how many it hides by the 1.7.10 rule,
	 *  counted once when the categories are registered. */
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
	/** Crafting recipes in JEI: the F11 buffer {@code CR.list()}, filtered down to the four shown types (1:1 with what
	 *  NEI used to show — see {@link GT6_JEI_CraftingRecipes}), computed ONCE in {@link #registerCategories}. */
	private List<ICraftingRecipeGT> mCraftingRecipes = Collections.emptyList();
	/** JEI's ingredient manager, handed over at recipe registration; the crafting lookup keys items with it. */
	private volatile mezz.jei.api.runtime.IIngredientManager mIngredients = null;

	@Override
	public Identifier getPluginUid() {
		return PLUGIN_UID;
	}

	/** Отдельные Item различаются реестром; оставшиеся компоненты идентичности определяет ST. */
	@Override
	public void registerItemSubtypes(mezz.jei.api.registration.ISubtypeRegistration aRegistration) {
		if (gregapi.GT_API.DATA_TYPE == null) return;
		int tLegacy = 0, tDataOnly = 0, tRegistryOnly = 0;
		for (net.minecraft.world.item.Item tItem : net.minecraft.core.registries.BuiltInRegistries.ITEM) {
			Identifier tKey = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(tItem);
			if (tKey == null) continue;
			if (!ModIDs.isGregNamespace(tKey.getNamespace())) continue;
			try {
				var tComponents = gregapi.util.ST.identityComponents(tItem);
				if (tComponents.length == 0) {tRegistryOnly++; continue;}
				aRegistration.registerFromDataComponentTypes(tItem, tComponents);
				if (gregapi.util.ST.usesSubtypeComponent(tItem)) tLegacy++; else tDataOnly++;
			} catch (Throwable e) {/**/}
		}
		OUT.println("[GT6-JEI] legacy=" + tLegacy + ", data-only=" + tDataOnly + ", registry-only=" + tRegistryOnly);
	}


	@Override
	public void registerCategories(IRecipeCategoryRegistration aRegistration) {
		mTypes.clear();
		sTypesByName.clear();
		mCraftingRecipes = Collections.emptyList();
		IGuiHelper tGuiHelper = aRegistration.getJeiHelpers().getGuiHelper();
		List<IRecipeCategory<?>> tCategories = new ArrayList<>();
		// GT6's single recipe center, 1:1 with NEI_GT_API_Config.java:62: every map with mNEIAllowed==true gets its own
		// category. A map's recipes may exist only on demand, so emptiness is left to JEI, which hides a category without recipes.
		for (RecipeMap tMap : RecipeMap.RECIPE_MAP_LIST) {
			if (!tMap.mNEIAllowed) continue;
			try {
				// NEI let several handlers share one id (Furnace and Microwave are both "smelting"); a JEI uid must be unique,
				// so a map whose NEI id is taken falls back to its own internal name, and the NEI id keeps opening the first map.
				boolean tTaken = sTypesByName.containsKey(tMap.mNameNEI);
				RecipeType<Recipe> tType = RecipeType.create(MD.GT.mID, tTaken ? tMap.mNameInternal : tMap.mNameNEI, Recipe.class);
				mTypes.put(tMap, tType);
				if (!tTaken) sTypesByName.put(tMap.mNameNEI, tType); // BUG-056: the same key 1.7.10 used to call NEI
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

		// Crafting recipes in JEI: GT6's buffer becomes ordinary crafting recipes of the native category (issue #45).
		// In 1.7.10 they were IRecipes of the one CraftingManager, so NEI drew them in its own crafting handler; a
		// parallel GT6 category showed the same table twice and kept foreign crafting integrations from seeing them.
		try {
			List<ICraftingRecipeGT> tCraftingList = new ArrayList<>();
			int tHidden = 0;
			for (ICraftingRecipeGT tRecipe : CR.list()) {
				if (tRecipe == null) continue;
				if (tRecipe instanceof ShapedOreRecipe || tRecipe instanceof ShapelessOreRecipe
				 || tRecipe instanceof gregapi.recipes.AdvancedCrafting1ToY || tRecipe instanceof gregapi.recipes.AdvancedCraftingXToY) {
					// The show rule isn't ours, it's 1.7.10's: the showcase of that time never drew a recipe with an
					// EMPTY variant list in a cell (see GT6_JEI_CraftingRecipes.showable). The mechanic is untouched:
					// the recipe stays in CR.BUFFER and on the crafting table, only its display is hidden.
					if (!GT6_JEI_CraftingRecipes.showable(tRecipe)) {
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
			mCraftingRecipes = tCraftingList;
			OUT.println("[GT6-JEI] crafting table: showing " + tCraftingList.size() + " recipes, hidden as in NEI 1.7.10 (nothing to draw the cell with) — " + tHidden);
		} catch (Throwable e) {
			ERR.println("JEI: GT6 crafting recipes failed to register, skipping.");
			e.printStackTrace(ERR);
		}

		aRegistration.addRecipeCategories(tCategories.toArray(new IRecipeCategory<?>[0]));
	}

	/** Nothing is registered as a list: GT6 generates many recipes only when asked, and NEI 1.7.10 looked them up per
	 *  click, so maps and crafting answer JEI per lookup instead ({@link #registerAdvanced}). */
	@Override
	public void registerRecipes(IRecipeRegistration aRegistration) {
		mIngredients = aRegistration.getIngredientManager();
		removeNativeFluidLayer(aRegistration.getIngredientManager());
	}

	/** NEI 1.7.10 had no fluid ingredients, only GT6's fluid displays, so JEI's own fluid layer goes; removed before the
	 *  panel is built, the panel never re-sorts for it. */
	private static void removeNativeFluidLayer(mezz.jei.api.runtime.IIngredientManager aManager) {
		try {
			java.util.Collection<net.neoforged.neoforge.fluids.FluidStack> tFluids = new java.util.ArrayList<>(aManager.getAllIngredients(mezz.jei.api.neoforge.NeoForgeTypes.FLUID_STACK));
			if (!tFluids.isEmpty()) aManager.removeIngredientsAtRuntime(mezz.jei.api.neoforge.NeoForgeTypes.FLUID_STACK, tFluids);
			OUT.println("[GT6-JEI] native FLUID_STACK layer removed before the panel is built: was " + tFluids.size() + ", now " + aManager.getAllIngredients(mezz.jei.api.neoforge.NeoForgeTypes.FLUID_STACK).size() + " (fluids are shown by the GT6 display, as in NEI 1.7.10)");
		} catch (Throwable e) {
			ERR.println("JEI: could not remove the native FLUID_STACK layer (a fluid duplicate will remain in the panel).");
			e.printStackTrace(ERR);
		}
	}

	/** JEI's own uid prefix for its tag-information categories (mezz.jei.common.recipes.TagRecipeUtil). */
	private static final String JEI_TAG_RECIPES = "tag_recipes/";

	/** The one place JEI's own showcase is cut back to NEI 1.7.10's: JEI adds these itself, so its live runtime is the earliest
	 *  point a mod reaches them; hidden in memory only, the player's config is untouched. */
	private static void hideNativeArtifacts(mezz.jei.api.recipe.IRecipeManager aManager) {
		try {
			// NEI 1.7.10 had no tag handler (codechicken RecipeInfo.java:93-104).
			int tHidden = 0;
			for (IRecipeCategory<?> tCategory : aManager.createRecipeCategoryLookup().includeHidden().get().toList()) {
				if (!tCategory.getRecipeType().getUid().getPath().startsWith(JEI_TAG_RECIPES)) continue;
				aManager.hideRecipeCategory(tCategory.getRecipeType());
				tHidden++;
			}
			OUT.println("[GT6-JEI] tag-information categories hidden (NEI 1.7.10 had no tag handler): " + tHidden);
		} catch (Throwable e) {
			ERR.println("JEI: could not hide the tag-information categories (they stay in the recipe lookups).");
			e.printStackTrace(ERR);
		}
		try {
			// NEI showed each smelting pair once with its real output (FurnaceRecipeHandler.java:96-119), as the gregtech:smelting tab
			// does; a dispatcher card (an input list over a placeholder output) is an adaptation artifact 1.7.10 never showed.
			// A dispatcher card is any card carrying a dispatcher's recipe id: the vanilla tab's and a mod's wrapper of it (Jumbo Furnace).
			java.util.Set<Object> tIds = new java.util.HashSet<>();
			// The client's synced smelting recipes, as JEI holds them (no handled-filter in this JEI version).
			aManager.createRecipeLookup(mezz.jei.api.constants.RecipeTypes.SMELTING).includeHidden().get()
				.filter(r -> cookingArtifact(r.value())).forEach(r -> tIds.add(r.id().identifier()));
			aManager.createRecipeLookup(mezz.jei.api.constants.RecipeTypes.SMOKING).includeHidden().get().filter(r -> cookingArtifact(r.value())).forEach(r -> tIds.add(r.id().identifier()));
			aManager.createRecipeLookup(mezz.jei.api.constants.RecipeTypes.BLASTING).includeHidden().get().filter(r -> cookingArtifact(r.value())).forEach(r -> tIds.add(r.id().identifier()));
			long tStart = System.nanoTime();
			int tHidden = 0;
			// GT6's own tabs never hold a vanilla recipe, and listing them all would run their on-demand generation.
			for (IRecipeCategory<?> tCategory : aManager.createRecipeCategoryLookup().includeHidden().get().toList())
				if (!MD.GT.mID.equals(tCategory.getRecipeType().getUid().getNamespace())) tHidden += hideById(aManager, tCategory, tIds);
			OUT.println("[GT6-JEI] GT6 smelting dispatcher cards hidden (the gregtech:smelting tab shows the pairs): " + tHidden + " cards of " + tIds.size() + " dispatchers, " + (System.nanoTime() - tStart) / 1000000 + " ms");
		} catch (Throwable e) {
			ERR.println("JEI: could not hide the GT6 smelting dispatcher cards (they stay in the smelting tabs).");
			e.printStackTrace(ERR);
		}
	}

	/** Hides the category's cards whose recipe id is one of aIds; a category that fails to list stays as it is. */
	private static <T> int hideById(mezz.jei.api.recipe.IRecipeManager aManager, IRecipeCategory<T> aCategory, java.util.Set<Object> aIds) {
		try {
			java.util.List<T> tHit = aManager.createRecipeLookup(aCategory.getRecipeType()).includeHidden().get().filter(r -> aIds.contains(aCategory.getIdentifier(r))).toList();
			if (!tHit.isEmpty()) aManager.hideRecipes(aCategory.getRecipeType(), tHit);
			return tHit.size();
		} catch (Throwable e) {
			ERR.println("JEI: category " + aCategory.getRecipeType().getUid() + " could not be checked for GT6 smelting dispatcher cards.");
			e.printStackTrace(ERR);
			return 0;
		}
	}

	@Override
	public void onRuntimeAvailable(mezz.jei.api.runtime.IJeiRuntime aRuntime) {
		hideNativeArtifacts(aRuntime.getRecipeManager());
		hideReplacedVanilla(aRuntime.getRecipeManager());
		sRuntime = aRuntime; // BUG-056: the only door to the recipe screen, see showRecipeCategory
	}

	/** Each map answers per lookup, as NEI 1.7.10 made a fresh handler per click; the lookup runs GT6's on-demand generation. */
	@Override
	public void registerAdvanced(mezz.jei.api.registration.IAdvancedRegistration aRegistration) {
		for (Map.Entry<RecipeMap, RecipeType<Recipe>> tEntry : mTypes.entrySet()) {
			try {
				aRegistration.addSimpleRecipeManagerPlugin(tEntry.getValue(), new MapLookup(tEntry.getKey()));
			} catch (Throwable e) {
				ERR.println("JEI: RecipeMap '" + tEntry.getKey().mNameInternal + "' failed to register its lookup, skipping.");
				e.printStackTrace(ERR);
			}
		}
		aRegistration.addSimpleRecipeManagerPlugin(mezz.jei.api.constants.RecipeTypes.SMOKING, new CookingLookup<>(gregapi.recipes.FurnaceRecipes.smoking(), (aIn, aOut) -> cookingView(aIn, aOut, true)));
		aRegistration.addSimpleRecipeManagerPlugin(mezz.jei.api.constants.RecipeTypes.BLASTING, new CookingLookup<>(gregapi.recipes.FurnaceRecipes.blasting(), (aIn, aOut) -> blastingView(aIn, aOut)));

		if (!mCraftingRecipes.isEmpty()) {
			try {
				// The native crafting type is shared with the engine's own recipes: JEI asks every plugin per lookup, so
				// GT6 answers where its recipes are and the vanilla ones keep coming from JEI's own index.
				aRegistration.addSimpleRecipeManagerPlugin(mezz.jei.api.constants.RecipeTypes.CRAFTING, new CraftingLookup(mCraftingRecipes, () -> mIngredients));
			} catch (Throwable e) {
				ERR.println("JEI: GT6 crafting recipes failed to register their lookup, skipping.");
				e.printStackTrace(ERR);
			}
		}
	}

	/** The item of a JEI focus, or null for anything else. */
	private static ItemStack focusStack(mezz.jei.api.ingredients.ITypedIngredient<?> aIngredient) {
		ItemStack rStack = aIngredient == null ? null : aIngredient.getItemStack().orElse(null);
		return rStack == null || rStack.isEmpty() ? null : rStack;
	}

	/** Crafting recipes answered per lookup, keyed by JEI's own recipe uid so a lookup matches exactly what its index would. */
	private static final class CraftingLookup implements mezz.jei.api.recipe.advanced.ISimpleRecipeManagerPlugin<net.minecraft.world.item.crafting.RecipeHolder<net.minecraft.world.item.crafting.CraftingRecipe>> {
		private final List<ICraftingRecipeGT> mRecipes;
		private final java.util.function.Supplier<mezz.jei.api.runtime.IIngredientManager> mIngredients;
		private mezz.jei.api.ingredients.IIngredientHelper<ItemStack> mHelper = null;
		private Map<Object, List<net.minecraft.world.item.crafting.RecipeHolder<net.minecraft.world.item.crafting.CraftingRecipe>>> mByInput = null, mByOutput = null;
		private List<net.minecraft.world.item.crafting.RecipeHolder<net.minecraft.world.item.crafting.CraftingRecipe>> mAll = null;
		CraftingLookup(List<ICraftingRecipeGT> aRecipes, java.util.function.Supplier<mezz.jei.api.runtime.IIngredientManager> aIngredients) {mRecipes = aRecipes; mIngredients = aIngredients;}

		private Object uid(ItemStack aStack) {
			if (mHelper == null) mHelper = mIngredients.get().getIngredientHelper(aStack);
			// A copy: JEI's subtype interpreters may write into the stack (1.20.1 painting: getOrCreateTagElement), and these are the recipes' own stacks.
			return mHelper.getUid(aStack.copy(), mezz.jei.api.ingredients.subtypes.UidContext.Recipe);
		}

		private synchronized void index() {
			if (mAll != null) return;
			long tStart = System.nanoTime();
			Map<Object, List<net.minecraft.world.item.crafting.RecipeHolder<net.minecraft.world.item.crafting.CraftingRecipe>>> tIn = new java.util.HashMap<>(), tOut = new java.util.HashMap<>();
			List<net.minecraft.world.item.crafting.RecipeHolder<net.minecraft.world.item.crafting.CraftingRecipe>> tAll = new ArrayList<>();
			for (int i = 0; i < mRecipes.size(); i++) {
				List<GT6_JEI_CraftingRecipes.Adapter> tAdapters;
				try {tAdapters = GT6_JEI_CraftingRecipes.adapters(mRecipes.get(i), i);} catch (Throwable e) {continue;}
				for (GT6_JEI_CraftingRecipes.Adapter tAdapter : tAdapters) {
					tAll.add(tAdapter.holder());
					put(tIn , tAdapter.inputs(), tAdapter.holder());
					put(tOut, List.of(tAdapter.output()), tAdapter.holder());
				}
			}
			mAll = tAll; mByInput = tIn; mByOutput = tOut;
			OUT.println("[GT6-JEI] crafting table lookup indexed on first use: " + mRecipes.size() + " recipes -> " + tAll.size() + " cards, " + tIn.size() + " input keys, " + tOut.size() + " output keys, " + (System.nanoTime() - tStart) / 1000000 + " ms");
		}

		private void put(Map<Object, List<net.minecraft.world.item.crafting.RecipeHolder<net.minecraft.world.item.crafting.CraftingRecipe>>> aIndex, List<ItemStack> aStacks, net.minecraft.world.item.crafting.RecipeHolder<net.minecraft.world.item.crafting.CraftingRecipe> aHolder) {
			java.util.Set<Object> tSeen = new java.util.HashSet<>();
			for (ItemStack tStack : aStacks) {
				Object tKey = uid(tStack);
				if (tSeen.add(tKey)) aIndex.computeIfAbsent(tKey, k -> new ArrayList<>()).add(aHolder);
			}
		}

		private List<net.minecraft.world.item.crafting.RecipeHolder<net.minecraft.world.item.crafting.CraftingRecipe>> find(boolean aInput, mezz.jei.api.ingredients.ITypedIngredient<?> aFocus) {
			ItemStack tStack = focusStack(aFocus);
			if (tStack == null) return Collections.emptyList();
			try {
				index();
				List<net.minecraft.world.item.crafting.RecipeHolder<net.minecraft.world.item.crafting.CraftingRecipe>> rList = (aInput ? mByInput : mByOutput).get(uid(tStack));
				return rList == null ? Collections.emptyList() : rList;
			} catch (Throwable e) {e.printStackTrace(ERR); return Collections.emptyList();}
		}

		@Override public boolean isHandledInput (mezz.jei.api.ingredients.ITypedIngredient<?> aInput ) {return focusStack(aInput ) != null;}
		@Override public boolean isHandledOutput(mezz.jei.api.ingredients.ITypedIngredient<?> aOutput) {return focusStack(aOutput) != null;}
		@Override public List<net.minecraft.world.item.crafting.RecipeHolder<net.minecraft.world.item.crafting.CraftingRecipe>> getRecipesForInput (mezz.jei.api.ingredients.ITypedIngredient<?> aInput ) {return find(T, aInput );}
		@Override public List<net.minecraft.world.item.crafting.RecipeHolder<net.minecraft.world.item.crafting.CraftingRecipe>> getRecipesForOutput(mezz.jei.api.ingredients.ITypedIngredient<?> aOutput) {return find(F, aOutput);}
		@Override public List<net.minecraft.world.item.crafting.RecipeHolder<net.minecraft.world.item.crafting.CraftingRecipe>> getAllRecipes() {index(); return mAll;}
	}

	/** Procedural furnace pairs queried on demand from the same reader servicing the machine. */
	private static final class CookingLookup<R> implements mezz.jei.api.recipe.advanced.ISimpleRecipeManagerPlugin<R> {
		private final gregapi.recipes.FurnaceRecipes mChannel;
		private final java.util.function.BiFunction<ItemStack, ItemStack, R> mFactory;
		CookingLookup(gregapi.recipes.FurnaceRecipes aChannel, java.util.function.BiFunction<ItemStack, ItemStack, R> aFactory) {mChannel = aChannel; mFactory = aFactory;}
		private List<R> find(ItemStack aFocus, boolean aInput) {
			gregapi.recipes.FurnaceRecipes.Answers tAnswers = mChannel.snapshot();
			List<R> rRecipes = new ArrayList<>();
			java.util.Set<net.minecraft.world.item.ItemStack> tSeen = net.minecraft.world.item.ItemStackLinkedSet.createTypeAndComponentsSet();
			for (ItemStack tKey : tAnswers.candidates()) {
				ItemStack tIn = gregapi.util.ST.amount(1, tKey), tOut = tAnswers.result(tIn);
				if (gregapi.util.ST.invalid(tIn) || gregapi.util.ST.invalid(tOut) || !tSeen.add(tIn)) continue;
				if (aFocus != null && !gregapi.util.ST.equal(aFocus, aInput ? tIn : tOut, true)) continue;
				rRecipes.add(mFactory.apply(tIn, tOut));
			}
			return rRecipes;
		}
		@Override public boolean isHandledInput(mezz.jei.api.ingredients.ITypedIngredient<?> aFocus) {return focusStack(aFocus) != null;}
		@Override public boolean isHandledOutput(mezz.jei.api.ingredients.ITypedIngredient<?> aFocus) {return focusStack(aFocus) != null;}
		@Override public List<R> getRecipesForInput(mezz.jei.api.ingredients.ITypedIngredient<?> aFocus) {return find(focusStack(aFocus), true);}
		@Override public List<R> getRecipesForOutput(mezz.jei.api.ingredients.ITypedIngredient<?> aFocus) {return find(focusStack(aFocus), false);}
		@Override public List<R> getAllRecipes() {return find(null, true);}
	}
	private static String cookingViewPath(ItemStack aInput, boolean aSmoking) {
		var tJSON = ItemStack.CODEC.encodeStart(net.minecraft.resources.RegistryOps.create(com.mojang.serialization.JsonOps.INSTANCE, net.minecraft.client.Minecraft.getInstance().level.registryAccess()), gregapi.util.ST.amount(1, aInput)).result().orElseThrow(() -> new IllegalStateException("Codec failed to serialize furnace showcase input"));
		return "cooking_view/" + (aSmoking ? "smoking/" : "blasting/") + java.util.UUID.nameUUIDFromBytes(canonicalCookingJSON(tJSON).toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
	}
	/** Codec stores components; only object keys are sorted, array order is preserved. */
	private static com.google.gson.JsonElement canonicalCookingJSON(com.google.gson.JsonElement aJSON) {
		if (aJSON.isJsonObject()) {
			var rObject = new com.google.gson.JsonObject();
			aJSON.getAsJsonObject().keySet().stream().sorted().forEach(tKey -> rObject.add(tKey, canonicalCookingJSON(aJSON.getAsJsonObject().get(tKey))));
			return rObject;
		}
		if (aJSON.isJsonArray()) {
			var rArray = new com.google.gson.JsonArray();
			for (var tElement : aJSON.getAsJsonArray()) rArray.add(canonicalCookingJSON(tElement));
			return rArray;
		}
		return aJSON;
	}

	private static net.minecraft.world.item.crafting.RecipeHolder<net.minecraft.world.item.crafting.SmokingRecipe> cookingView(ItemStack aIn, ItemStack aOut, boolean aSmoking) {
		net.minecraft.world.item.crafting.Ingredient tInput = new gregapi.recipes.FurnaceRecipes.RuntimeIngredient(java.util.List.of(aIn)).toVanilla();
		var tRecipe = new net.minecraft.world.item.crafting.SmokingRecipe(new net.minecraft.world.item.crafting.Recipe.CommonInfo(false), new net.minecraft.world.item.crafting.AbstractCookingRecipe.CookingBookInfo(net.minecraft.world.item.crafting.CookingBookCategory.FOOD, ""), tInput, net.minecraft.world.item.ItemStackTemplate.fromNonEmptyStack(aOut), gregapi.recipes.FurnaceRecipes.smoking().engineExperience(aOut), 100);
		return new net.minecraft.world.item.crafting.RecipeHolder<>(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.RECIPE, net.minecraft.resources.Identifier.fromNamespaceAndPath("gregapi", cookingViewPath(aIn, true))), tRecipe);
	}
	private static net.minecraft.world.item.crafting.RecipeHolder<net.minecraft.world.item.crafting.BlastingRecipe> blastingView(ItemStack aIn, ItemStack aOut) {
		net.minecraft.world.item.crafting.Ingredient tInput = new gregapi.recipes.FurnaceRecipes.RuntimeIngredient(java.util.List.of(aIn)).toVanilla();
		var tRecipe = new net.minecraft.world.item.crafting.BlastingRecipe(new net.minecraft.world.item.crafting.Recipe.CommonInfo(false), new net.minecraft.world.item.crafting.AbstractCookingRecipe.CookingBookInfo(net.minecraft.world.item.crafting.CookingBookCategory.MISC, ""), tInput, net.minecraft.world.item.ItemStackTemplate.fromNonEmptyStack(aOut), gregapi.recipes.FurnaceRecipes.blasting().engineExperience(aOut), 100);
		return new net.minecraft.world.item.crafting.RecipeHolder<>(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.RECIPE, net.minecraft.resources.Identifier.fromNamespaceAndPath("gregapi", cookingViewPath(aIn, false))), tRecipe);
	}
	private static boolean cookingArtifact(Object aRecipe) {
		if (gregapi.recipes.FurnaceRecipes.dispatcher(aRecipe)) return true;
		return aRecipe instanceof net.minecraft.world.item.crafting.AbstractCookingRecipe tCooking && tCooking.input().getCustomIngredient() instanceof gregapi.recipes.FurnaceRecipes.RuntimeIngredient tInput && !tInput.hasVisibleInputs();
	}

	/** One map's lookup. Errors are swallowed like the NEI handler did: JEI permanently disables a plugin that throws,
	 *  and the integrated server may grow the same map concurrently while the client reads it. */
	private static final class MapLookup implements mezz.jei.api.recipe.advanced.ISimpleRecipeManagerPlugin<Recipe> {
		private final RecipeMap mMap;
		MapLookup(RecipeMap aMap) {mMap = aMap;}

		@Override public boolean isHandledInput (mezz.jei.api.ingredients.ITypedIngredient<?> aInput ) {return focusStack(aInput ) != null;}
		@Override public boolean isHandledOutput(mezz.jei.api.ingredients.ITypedIngredient<?> aOutput) {return focusStack(aOutput) != null;}

		@Override public List<Recipe> getRecipesForInput(mezz.jei.api.ingredients.ITypedIngredient<?> aInput) {
			ItemStack tStack = focusStack(aInput);
			if (tStack == null) return Collections.emptyList();
			try {return mMap.getNEIUsagesFor(tStack);} catch (Throwable e) {e.printStackTrace(ERR); return Collections.emptyList();}
		}

		@Override public List<Recipe> getRecipesForOutput(mezz.jei.api.ingredients.ITypedIngredient<?> aOutput) {
			ItemStack tStack = focusStack(aOutput);
			if (tStack == null) return Collections.emptyList();
			try {return mMap.getNEIRecipesFor(tStack);} catch (Throwable e) {e.printStackTrace(ERR); return Collections.emptyList();}
		}

		/** The "all recipes" click of NEI_RecipeMap.loadCraftingRecipes(String), answered as JEI consumes it: see {@link AllRecipes}. */
		@Override public List<Recipe> getAllRecipes() {
			return new AllRecipes(mMap);
		}
	}

	/** JEI streams "all recipes" at start only to see whether a category is empty; a recipe the map already holds answers
	 *  that, so the on-demand generation (NEI's explicit "all recipes" click) starts only when the stream goes further. */
	private static final class AllRecipes extends java.util.AbstractList<Recipe> {
		private final RecipeMap mMap;
		private List<Recipe> mAll = null;
		AllRecipes(RecipeMap aMap) {mMap = aMap;}

		private List<Recipe> all() {
			if (mAll == null) try {mAll = mMap.getNEIAllRecipes();} catch (Throwable e) {e.printStackTrace(ERR); mAll = Collections.emptyList();}
			return mAll;
		}


		// Index and size need the whole list; JEI only streams it (PluginManager.getRecipes: .stream()).
		@Override public Recipe get(int aIndex) {return all().get(aIndex);}
		@Override public int size() {return all().size();}
		// The default Collection spliterator asks size() before the first element, which would run the generation.
		@Override public java.util.Spliterator<Recipe> spliterator() {return new Walk();}
		@Override public java.util.Iterator<Recipe> iterator() {return java.util.Spliterators.iterator(spliterator());}
		@Override public java.util.stream.Stream<Recipe> stream() {return java.util.stream.StreamSupport.stream(spliterator(), false);}

		private final class Walk implements java.util.Spliterator<Recipe> {
			private Recipe mEarly = null;
			private int mIndex = -1;

			@Override public boolean tryAdvance(java.util.function.Consumer<? super Recipe> aAction) {
				if (mIndex < 0) {
					mIndex = 0;
					if (mAll == null && (mEarly = GT6_JEI_StartMaps.firstReady(mMap)) != null) {aAction.accept(mEarly); return true;}
				}
				List<Recipe> tAll = all();
				while (mIndex < tAll.size()) {
					Recipe tRecipe = tAll.get(mIndex++);
					if (tRecipe != mEarly) {aAction.accept(tRecipe); return true;}
				}
				return false;
			}
			@Override public java.util.Spliterator<Recipe> trySplit() {return null;}
			@Override public long estimateSize() {return Long.MAX_VALUE;}
			@Override public int characteristics() {return ORDERED | NONNULL;}
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

	/** One page per replaced vanilla recipe, as in 1.7.10: Forge replaced the recipe in the one CraftingManager, here the
	 *  GT6 ore version is a separate card, so the superseded datapack original is hidden by its own id. */
	private void hideReplacedVanilla(mezz.jei.api.recipe.IRecipeManager aManager) {
		if (mCraftingRecipes == null || mCraftingRecipes.isEmpty()) return;
		try {
			java.util.Set<net.minecraft.resources.Identifier> tReplaced = GT6_JEI_CraftingRecipes.replacedSources(mCraftingRecipes);
			if (tReplaced.isEmpty()) return;
			java.util.List<net.minecraft.world.item.crafting.RecipeHolder<net.minecraft.world.item.crafting.CraftingRecipe>> tHit = aManager.createRecipeLookup(mezz.jei.api.constants.RecipeTypes.CRAFTING).includeHidden().get()
				.filter(tHolder -> tReplaced.contains(tHolder.id().identifier())).toList();
			if (!tHit.isEmpty()) aManager.hideRecipes(mezz.jei.api.constants.RecipeTypes.CRAFTING, tHit);
			OUT.println("[GT6-JEI] replaced vanilla recipes hidden (one page per recipe, as in 1.7.10): " + tHit.size() + " of " + tReplaced.size());
		} catch (Throwable e) {
			ERR.println("JEI: could not hide the replaced vanilla recipes (the player will see two pages for them).");
			e.printStackTrace(ERR);
		}
	}
}

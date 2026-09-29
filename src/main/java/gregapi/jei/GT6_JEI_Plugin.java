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
	/** Crafting recipes in JEI: the F11 buffer {@code CR.list()}, filtered down to {@link ShapedOreRecipe}/{@link ShapelessOreRecipe} descendants
	 *  (1:1 with what NEI used to show — see the {@link GT6_JEI_CraftingCategory} javadoc), computed ONCE in {@link #registerCategories}. */
	private List<ICraftingRecipeGT> mCraftingRecipes = Collections.emptyList();
	/** JEI's ingredient manager, handed over at recipe registration; the crafting lookup keys items with it. */
	private volatile mezz.jei.api.runtime.IIngredientManager mIngredients = null;

	@Override
	public Identifier getPluginUid() {
		return PLUGIN_UID;
	}

	/** F1-jei: JEI tells item variants apart only by DECLARED components (registerFromDataComponentTypes);
	 *  without a SUBTYPE declaration all procedural variants (1.7.10 itemDamage) collapse into one item
	 *  (log evidence: "289 duplicate items", the ingredient list is empty for the mod). Declare SUBTYPE for every gt item. */
	@Override
	public void registerItemSubtypes(mezz.jei.api.registration.ISubtypeRegistration aRegistration) {
		if (gregapi.GT_API.SUBTYPE_TYPE == null) return;
		net.minecraft.core.component.DataComponentType<?> tSubtype = gregapi.GT_API.SUBTYPE_TYPE;
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

	/** Issue #5: map recipes are NOT registered as a snapshot. GT6 generates many of them only when asked
	 *  (IRecipeMapHandler), which a list taken once at world join can never contain; see {@link #registerAdvanced}.
	 *  Crafting recipes are not handed over either: JEI indexing every slot of ~65k of them cost ~1.6 s of the frame
	 *  that starts JEI, so they are answered per lookup too ({@link CraftingLookup}), as NEI 1.7.10 did. */
	@Override
	public void registerRecipes(IRecipeRegistration aRegistration) {
		mIngredients = aRegistration.getIngredientManager();
		removeNativeFluidLayer(aRegistration.getIngredientManager());
	}

	/** BUG-030 v2 (player report: "the old fluid layer stayed as a duplicate"): JEI itself registers ALL source
	 *  fluids of the registry under the native FLUID_STACK ingredient type ({@code FluidStackListFactory.create}: Registry.listElements →
	 *  filter isSource — the same ~679 set as the GT6 displays) → the ingredient panel showed fluids TWICE.
	 *  1.7.10's NEI had NO fluid ingredient type at all — there was only one layer, the GT6 displays (with a rich tooltip,
	 *  ItemFluidDisplay.addInformation). We remove JEI's native layer entirely — 1:1 with the NEI look; GT6's recipe
	 *  categories show fluids as display items ({@code FL.display}), they don't need FLUID_STACK ingredients.
	 *  JEI offers no way to keep its own layer from being registered, so it is removed at recipe registration: the panel's
	 *  list and filter are built later (registerRuntime), so they never see it and do not re-sort once per removed fluid. */
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

	@Override
	public void onRuntimeAvailable(mezz.jei.api.runtime.IJeiRuntime aRuntime) {
		sRuntime = aRuntime; // BUG-056: the only door to the recipe screen, see showRecipeCategory
	}

	/** Issue #5: each map answers JEI per lookup, the way NEI 1.7.10 created a fresh handler per click
	 *  (TemplateRecipeHandler.getRecipeHandler) — the lookup also runs GT6's on-demand recipe generation. */
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
		if (!mCraftingRecipes.isEmpty()) {
			try {
				aRegistration.addSimpleRecipeManagerPlugin(GT6_JEI_CraftingCategory.TYPE, new CraftingLookup(mCraftingRecipes, () -> mIngredients));
			} catch (Throwable e) {
				ERR.println("JEI: GT6 crafting-table recipes failed to register their lookup, skipping.");
				e.printStackTrace(ERR);
			}
		}
	}

	/** The item of a JEI focus, or null for anything else. */
	private static ItemStack focusStack(mezz.jei.api.ingredients.ITypedIngredient<?> aIngredient) {
		ItemStack rStack = aIngredient == null ? null : aIngredient.getItemStack().orElse(null);
		return rStack == null || rStack.isEmpty() ? null : rStack;
	}

	/** Crafting-table recipes answered per lookup. Items are keyed by JEI's own recipe uid ({@code getUid(stack,
	 *  UidContext.Recipe)}), the key JEI indexes list-filled slots with, so a lookup matches exactly what JEI's own index
	 *  matched; the index is built on the first lookup from the same slot contents the card shows. */
	private static final class CraftingLookup implements mezz.jei.api.recipe.advanced.ISimpleRecipeManagerPlugin<ICraftingRecipeGT> {
		private final List<ICraftingRecipeGT> mRecipes;
		private final java.util.function.Supplier<mezz.jei.api.runtime.IIngredientManager> mIngredients;
		private mezz.jei.api.ingredients.IIngredientHelper<ItemStack> mHelper = null;
		private Map<Object, List<ICraftingRecipeGT>> mByInput = null, mByOutput = null;
		CraftingLookup(List<ICraftingRecipeGT> aRecipes, java.util.function.Supplier<mezz.jei.api.runtime.IIngredientManager> aIngredients) {mRecipes = aRecipes; mIngredients = aIngredients;}

		private Object uid(ItemStack aStack) {
			if (mHelper == null) mHelper = mIngredients.get().getIngredientHelper(aStack);
			// A copy: JEI's subtype interpreters may write into the stack (1.20.1 painting: getOrCreateTagElement), and these are the recipes' own stacks.
			return mHelper.getUid(aStack.copy(), mezz.jei.api.ingredients.subtypes.UidContext.Recipe);
		}

		private synchronized void index() {
			if (mByOutput != null) return;
			long tStart = System.nanoTime();
			Map<Object, List<ICraftingRecipeGT>> tIn = new java.util.HashMap<>(), tOut = new java.util.HashMap<>();
			List<ItemStack> tInputs = new ArrayList<>(), tOutputs = new ArrayList<>();
			for (ICraftingRecipeGT tRecipe : mRecipes) {
				tInputs.clear(); tOutputs.clear();
				try {GT6_JEI_CraftingCategory.slotStacks(tRecipe, tInputs, tOutputs);} catch (Throwable e) {continue;}
				put(tIn , tInputs , tRecipe);
				put(tOut, tOutputs, tRecipe);
			}
			mByInput = tIn; mByOutput = tOut;
			OUT.println("[GT6-JEI] crafting table lookup indexed on first use: " + mRecipes.size() + " recipes, " + tIn.size() + " input keys, " + tOut.size() + " output keys, " + (System.nanoTime() - tStart) / 1000000 + " ms");
		}

		private void put(Map<Object, List<ICraftingRecipeGT>> aIndex, List<ItemStack> aStacks, ICraftingRecipeGT aRecipe) {
			java.util.Set<Object> tSeen = new java.util.HashSet<>();
			for (ItemStack tStack : aStacks) {
				Object tKey = uid(tStack);
				if (tSeen.add(tKey)) aIndex.computeIfAbsent(tKey, k -> new ArrayList<>()).add(aRecipe);
			}
		}

		private List<ICraftingRecipeGT> find(boolean aInput, mezz.jei.api.ingredients.ITypedIngredient<?> aFocus) {
			ItemStack tStack = focusStack(aFocus);
			if (tStack == null) return Collections.emptyList();
			try {
				index();
				List<ICraftingRecipeGT> rList = (aInput ? mByInput : mByOutput).get(uid(tStack));
				return rList == null ? Collections.emptyList() : rList;
			} catch (Throwable e) {e.printStackTrace(ERR); return Collections.emptyList();}
		}

		@Override public boolean isHandledInput (mezz.jei.api.ingredients.ITypedIngredient<?> aInput ) {return focusStack(aInput ) != null;}
		@Override public boolean isHandledOutput(mezz.jei.api.ingredients.ITypedIngredient<?> aOutput) {return focusStack(aOutput) != null;}
		@Override public List<ICraftingRecipeGT> getRecipesForInput (mezz.jei.api.ingredients.ITypedIngredient<?> aInput ) {return find(T, aInput );}
		@Override public List<ICraftingRecipeGT> getRecipesForOutput(mezz.jei.api.ingredients.ITypedIngredient<?> aOutput) {return find(F, aOutput);}
		@Override public List<ICraftingRecipeGT> getAllRecipes() {return mRecipes;}
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

	/** One map's "all recipes", produced lazily. JEI asks every category for it at GUI start only to see whether the
	 *  category is empty (a findAny over the stream); a recipe the map already holds answers that, and getNEIAllRecipes,
	 *  which runs the on-demand generation and which NEI 1.7.10 ran only on the explicit "all recipes" click, starts
	 *  only when the stream is walked past it. A map with nothing yet generates at once, so no category drops out.
	 *  A full walk yields the same recipes as getNEIAllRecipes, each once (the early one is skipped by identity). */
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

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
	/** CR.list() filtered to ShapedOreRecipe/ShapelessOreRecipe descendants, 1:1 with what NEI used to show, counted once. */
	private List<ICraftingRecipeGT> mCraftingRecipes = Collections.emptyList();
	/** JEI's ingredient manager, handed over at recipe registration; the crafting lookup keys items with it. */
	private volatile mezz.jei.api.runtime.IIngredientManager mIngredients = null;

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
		mCraftingRecipes = Collections.emptyList();
		IGuiHelper tGuiHelper = aRegistration.getJeiHelpers().getGuiHelper();
		List<IRecipeCategory<?>> tCategories = new ArrayList<>();
		// 1:1 with NEI_GT_API_Config.java:62: every map with mNEIAllowed==true gets its own category. A map's recipes may
		// exist only on demand, so emptiness is left to JEI, which hides a category without recipes.
		for (RecipeMap tMap : RecipeMap.RECIPE_MAP_LIST) {
			if (!tMap.mNEIAllowed) continue;
			try {
				// NEI let several handlers share one id (Furnace and Microwave are both "smelting"); a JEI uid must be unique,
				// so a map whose NEI id is taken falls back to its own internal name, and the NEI id keeps opening the first map.
				boolean tTaken = sTypesByName.containsKey(tMap.mNameNEI);
				RecipeType<Recipe> tType = RecipeType.create(MD.GT.mID, tTaken ? tMap.mNameInternal : tMap.mNameNEI, Recipe.class);
				mTypes.put(tMap, tType);
				if (!tTaken) sTypesByName.put(tMap.mNameNEI, tType); // The same key that used to open NEI in 1.7.10.
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
			java.util.Collection<net.minecraftforge.fluids.FluidStack> tFluids = new java.util.ArrayList<>(aManager.getAllIngredients(mezz.jei.api.forge.ForgeTypes.FLUID_STACK));
			if (!tFluids.isEmpty()) aManager.removeIngredientsAtRuntime(mezz.jei.api.forge.ForgeTypes.FLUID_STACK, tFluids);
			OUT.println("[GT6-JEI] native FLUID_STACK layer removed before the panel is built: was " + tFluids.size() + ", now " + aManager.getAllIngredients(mezz.jei.api.forge.ForgeTypes.FLUID_STACK).size() + " (fluids are shown by the GT6 display, as in NEI 1.7.10)");
		} catch (Throwable e) {
			ERR.println("JEI: could not remove the native FLUID_STACK layer (a fluid duplicate will remain in the panel).");
			e.printStackTrace(ERR);
		}
	}

	@Override
	public void onRuntimeAvailable(mezz.jei.api.runtime.IJeiRuntime aRuntime) {
		sRuntime = aRuntime; // The only door to the recipe screen; see showRecipeCategory.
	}

	/** Each map answers per lookup, as NEI 1.7.10 made a fresh handler per click; the lookup runs GT6's on-demand generation. */
	@Override
	public void registerAdvanced(mezz.jei.api.registration.IAdvancedRegistration aRegistration) {
		for (Map.Entry<RecipeMap, RecipeType<Recipe>> tEntry : mTypes.entrySet()) {
			try {
				aRegistration.addTypedRecipeManagerPlugin(tEntry.getValue(), new MapLookup(tEntry.getKey()));
			} catch (Throwable e) {
				ERR.println("JEI: RecipeMap '" + tEntry.getKey().mNameInternal + "' failed to register its lookup, skipping.");
				e.printStackTrace(ERR);
			}
		}
		if (!mCraftingRecipes.isEmpty()) {
			try {
				aRegistration.addTypedRecipeManagerPlugin(GT6_JEI_CraftingCategory.TYPE, new CraftingLookup(mCraftingRecipes, () -> mIngredients));
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

	/** Crafting recipes answered per lookup, keyed by JEI's own recipe uid so a lookup matches exactly what its index would. */
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

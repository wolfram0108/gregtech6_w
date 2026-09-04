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

import gregapi.data.LH;
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
 * The SINGLE center for JEI display of the GT6 crafting bench (F1.3-crafting-jei, decisions/F11-crafting-recipe.md). In 1.7.10
 * NEI displayed GT6 crafting itself: {@code ICraftingRecipeGT extends IRecipe}, GT6 registered concrete
 * forge implementations {@code ShapedOreRecipe}/{@code ShapelessOreRecipe} via {@code GameRegistry.addRecipe}
 * (see the {@code CR.shaped}/{@code CR.shapeless} of that era), and the built-in (not GT6 code, part of NEI itself)
 * {@code ShapedRecipeHandler}/{@code ShapelessRecipeHandler} recognized them by class and drew them itself — GT6 even
 * explicitly DISABLED its own {@code GuiCraftingRecipe.craftinghandlers} loop (gregapi/NEI_GT_API_Config.java:59,
 * commented out), relying entirely on NEI's automation.
 *
 * <p>In neo, GT6 crafting (F11) is its OWN buffer {@code CR.BUFFER}/{@code ICraftingRecipeGT}, a dispatcher
 * {@code CustomRecipe} reads it at bench runtime; the recipes themselves are NOT neo {@code CraftingRecipe}
 * (ADR F11 §"why not Recipe"), so the built-in JEI category {@code RecipeTypes.CRAFTING}
 * ({@code IRecipeHolderType<RecipeHolder<CraftingRecipe>>} — requires a codec/serializer in
 * {@code RecipeManager}, see ADR §F1.3-crafting-jei) cannot see them and cannot: making GT6 recipes a real
 * {@code CraftingRecipe} would mean reopening the closed F11 seam just for the display. Hence — a DEDICATED
 * {@code IRecipeCategory} on {@link ICraftingRecipeGT}, reading the same {@code CR.list()} (the F11 buffer) 1:1,
 * but the grid layout goes through the standard JEI {@link ICraftingGridHelper} (the same shared JEI mechanism as the
 * built-in category): a 3×3 grid at 18px/slot, output slot at (95,19), size 116×54, icon
 * {@code Blocks.CRAFTING_TABLE} — copied 1:1 (decompiled via javap) from
 * {@code mezz.jei.library.plugins.vanilla.crafting.CraftingRecipeCategory} (this constant itself is not in our
 * three neo reference roots — the JEI implementation is closed source, but it was read via javap as part of this
 * task's acceptance; the {@link ICraftingGridHelper} contract lives in {@code jei-26.1.2-common-api}) — visually
 * indistinguishable from JEI's native crafting render.
 *
 * <p>BUG-099 (user requirement "recipes must be 100% shown in the display"): FOUR types are shown —
 * {@link ShapedOreRecipe}/{@link ShapelessOreRecipe} descendants (incl. {@code AdvancedCraftingShaped}/
 * {@code AdvancedCraftingShapeless}/{@code AdvancedCraftingTool}) plus both standalone ones:
 * {@code AdvancedCrafting1ToY} (one item, the product is chosen by CELL) and {@code AdvancedCraftingXToY}
 * (X prefix items → Y output). The last two implement {@code ICraftingRecipeGT} directly, and their
 * {@code getRecipeOutput()} returns an {@code ERROR_OUTPUT} stub — which is why NEI never drew them in 1.7.10 either
 * (it recognized recipes by class). Here the display GOES FURTHER than the original: the layout and output are built from
 * the recipe's own fields (input/output prefix, count, cell number), so the player sees both what they'll get
 * and where to place items.</p>
 */
public final class GT6_JEI_CraftingCategory extends AbstractRecipeCategory<ICraftingRecipeGT> {
	public static final RecipeType<ICraftingRecipeGT> TYPE = RecipeType.create(MD.GT.mID, "crafting_gt6", ICraftingRecipeGT.class);

	private final ICraftingGridHelper mGridHelper;

	public GT6_JEI_CraftingCategory(IGuiHelper aGuiHelper) {
		super(TYPE, Component.literal(LH.tt("Crafting Table")), aGuiHelper.createDrawableItemLike(Blocks.CRAFTING_TABLE), 116, 54);
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
				// The product is chosen by the CELL the item sits in, so the card must show that cell.
				List<ItemStack> tIn = new java.util.ArrayList<>(), tOut = new java.util.ArrayList<>();
				materialPairs(t1ToY, t1ToY.mInput, t1ToY.mOutput, t1ToY.mOutputCount, m -> t1ToY.hasOutputFor(m), tIn, tOut);
				if (!tIn.isEmpty() && t1ToY.mEmpty < 9) {
					boolean[] tFilled = new boolean[9];
					tFilled[t1ToY.mEmpty] = T;
					family(aBuilder, tFilled, tIn, tOut);
				}
				return;
			} else if (aRecipe instanceof gregapi.recipes.AdvancedCraftingXToY tXToY) {
				// Positions carry no meaning here (the recipe counts items), so fill the first N cells.
				List<ItemStack> tIn = new java.util.ArrayList<>(), tOut = new java.util.ArrayList<>();
				materialPairs(tXToY, tXToY.mInput, tXToY.mOutput, tXToY.mOutputCount, m -> tXToY.hasOutputFor(m), tIn, tOut);
				int tN = tXToY.mInputCount;
				if (!tIn.isEmpty() && tN > 0 && tN <= 9) {
					boolean[] tFilled = new boolean[9];
					for (int i = 0; i < tN; i++) tFilled[i] = T;
					family(aBuilder, tFilled, tIn, tOut);
				}
				return;
			}
			ItemStack tOutput = aRecipe.getRecipeOutput();
			if (ST.valid(tOutput)) mGridHelper.createAndSetOutputs(aBuilder, List.of(tOutput));
		} catch (Throwable e) {
			sLayoutFailures++;
			ERR.println("JEI: GT6 crafting recipe failed to lay out, skipping its slots.");
			e.printStackTrace(ERR);
		}
	}

	/** BUG-121: layout guard — how many recipes the display failed to lay out this run.
	 *  Zero is mandatory: a failed layout leaves the card WITHOUT slots (the output is placed after the inputs). */
	public static int sLayoutFailures = 0;

	/** The only place a family card (prefix recipe, paired material lists) becomes slots.
	 *  The grid stays 3x3 because only there JEI's cell-to-slot mapping is the identity, so no index is ever guessed. */
	private void family(IRecipeLayoutBuilder aBuilder, boolean[] aFilled, List<ItemStack> aInputs, List<ItemStack> aOutputs) {
		List<List<ItemStack>> tCells = new ArrayList<>(9);
		for (int i = 0; i < 9; i++) tCells.add(aFilled[i] ? aInputs : List.of());
		List<mezz.jei.api.gui.builder.IRecipeSlotBuilder> tSlots = mGridHelper.createAndSetInputs(aBuilder, tCells, 3, 3);
		mezz.jei.api.gui.builder.IRecipeSlotBuilder tOutSlot = mGridHelper.createAndSetOutputs(aBuilder, aOutputs);
		List<mezz.jei.api.gui.builder.IIngredientAcceptor<?>> tLinked = new ArrayList<>();
		for (int i = 0; i < 9; i++) if (aFilled[i]) tLinked.add(tSlots.get(i));
		tLinked.add(tOutSlot);
		// Without the link a focused output leaves the input cycling through every material.
		aBuilder.createFocusLink(tLinked.toArray(new mezz.jei.api.gui.builder.IIngredientAcceptor<?>[0]));
	}

	/** ⛔ 1.7.10 DISPLAY RULE, restored verbatim (player report "parts of some recipes
	 *  are missing"): a recipe where EVEN ONE cell is an empty list of options (an ore name
	 *  under which no item exists) was NOT shown AT ALL in NEI —
	 *  {@code reference/mods/NotEnoughItems-1.7.10/src/codechicken/nei/recipe/ShapedRecipeHandler.java:157-158}
	 *  ({@code if (item instanceof List && ((List<?>)item).isEmpty()) return null; //ore handler, no ores})
	 *  and the same in {@code ShapelessRecipeHandler.java:156-157}.
	 *
	 *  <p>Such entries are legitimate in GT6 and exist in the ORIGINAL itself: prefix templates substitute symbols
	 *  unconditionally ({@code Loader_OreProcessing.OreProcessing_CraftFrom.onOreRegistration}), so
	 *  a "gem ring" is born even for lead, for which no gem exists. The recipe is dead
	 *  (nothing to match) — the 1.7.10 display did not show it either. We keep the data 1:1 with the original
	 *  (reference {@code reference/oracle/crafting.jsonl}), and the filter is not data but a display rule.</p>
	 *
	 *  <p>For the two standalone types ({@code AdvancedCrafting1ToY}/{@code AdvancedCraftingXToY},
	 *  BUG-099) the rule is the same in spirit: if there is no material with both an input and an output,
	 *  there is nothing to show — the card would be empty.</p> */
	public static boolean showable(ICraftingRecipeGT aRecipe) {
		try {
			if (aRecipe instanceof ShapedOreRecipe tShaped) return noEmptyChoice(tShaped.getInput());
			if (aRecipe instanceof ShapelessOreRecipe tShapeless) return noEmptyChoice(tShapeless.getInput().toArray());
			if (aRecipe instanceof gregapi.recipes.AdvancedCrafting1ToY t1ToY) return t1ToY.mEmpty < 9 && hasPairs(t1ToY, t1ToY.mInput, t1ToY.mOutput, t1ToY.mOutputCount, m -> t1ToY.hasOutputFor(m));
			if (aRecipe instanceof gregapi.recipes.AdvancedCraftingXToY tXToY) return tXToY.mInputCount > 0 && tXToY.mInputCount <= 9 && hasPairs(tXToY, tXToY.mInput, tXToY.mOutput, tXToY.mOutputCount, m -> tXToY.hasOutputFor(m));
		} catch (Throwable e) {
			// NEI also returned null on a parse exception, i.e. it did not show it either (ShapedRecipeHandler.java:161-164)
			return F;
		}
		return T;
	}

	/** No cell is an EMPTY list of options. The cell format is 1:1 with 1.7.10
	 *  ({@code null} / {@code ItemStack} / {@code List<ItemStack>}), the list is live
	 *  ({@code OreDictionary.getOres} returns the same reference), so the check is correct at the moment of display. */
	private static boolean noEmptyChoice(Object[] aCells) {
		if (aCells == null) return F;
		for (Object tCell : aCells) if (tCell instanceof List && ((List<?>)tCell).isEmpty()) return F;
		return T;
	}

	/** Whether there is at least one material giving BOTH an input AND an output (otherwise nothing to show). */
	private static boolean hasPairs(Object aRecipe, gregapi.oredict.OreDictPrefix aInput, gregapi.oredict.OreDictPrefix aOutput
	, int aOutputCount, java.util.function.Predicate<gregapi.oredict.OreDictMaterial> aHasOutput) {
		List<ItemStack> tIn = new ArrayList<>(), tOut = new ArrayList<>();
		materialPairs(aRecipe, aInput, aOutput, aOutputCount, aHasOutput, tIn, tOut);
		return !tIn.isEmpty();
	}

	/** BUG-099: the recipe is defined on a PREFIX, but the display shows concrete items — we collect "input↔output"
	 *  pairs across all materials for which the recipe has an output. The lists are the same length and order: JEI cycles
	 *  through slot alternatives in lockstep, so the input and output always show the SAME material.
	 *  <p>Materials are taken from {@code MATERIAL_MAP} (the existing ones), not from the sparse 32767-slot array:
	 *  the layout is built for every recipe, and walking the array cost 14 seconds at plugin registration —
	 *  long enough for the integrated server to time out (measured from the JEI-starter log).</p> */
	private static final java.util.Map<Object, List<List<ItemStack>>> PAIRS_CACHE = new java.util.WeakHashMap<>();

	/** Pairs for the recipe, computed once (JEI may rebuild the layout). */
	private static void materialPairs(Object aRecipe, gregapi.oredict.OreDictPrefix aInput, gregapi.oredict.OreDictPrefix aOutput, int aOutputCount
	, java.util.function.Predicate<gregapi.oredict.OreDictMaterial> aHasOutput, List<ItemStack> rInputs, List<ItemStack> rOutputs) {
		List<List<ItemStack>> tCached = PAIRS_CACHE.get(aRecipe);
		if (tCached != null) {rInputs.addAll(tCached.get(0)); rOutputs.addAll(tCached.get(1)); return;}
		materialPairs(aInput, aOutput, aOutputCount, aHasOutput, rInputs, rOutputs);
		PAIRS_CACHE.put(aRecipe, List.of(List.copyOf(rInputs), List.copyOf(rOutputs)));
	}

	private static void materialPairs(gregapi.oredict.OreDictPrefix aInput, gregapi.oredict.OreDictPrefix aOutput, int aOutputCount
	, java.util.function.Predicate<gregapi.oredict.OreDictMaterial> aHasOutput, List<ItemStack> rInputs, List<ItemStack> rOutputs) {
		java.util.Set<String> tSeen = new java.util.HashSet<>();
		for (gregapi.oredict.OreDictMaterial tMaterial : new java.util.LinkedHashSet<>(gregapi.oredict.OreDictMaterial.MATERIAL_MAP.values())) {
			if (tMaterial == null) continue;
			ItemStack tIn = aInput.mat(tMaterial, 1);
			if (!ST.valid(tIn)) continue;
			// The recipe judges the STACK, and the dictionary may unify it into another material or prefix,
			// so the pair is built from what the stack really is — never from the material key asked for.
			gregapi.oredict.OreDictItemData tData = gregapi.util.OM.anydata_(tIn);
			if (tData == null || tData.mPrefix != aInput || tData.mMaterial == null) continue;
			gregapi.oredict.OreDictMaterial tActual = tData.mMaterial.mMaterial;
			if (tActual == null || !aHasOutput.test(tActual)) continue;
			ItemStack tOut = aOutput.mat(tActual, aOutputCount);
			if (!ST.valid(tOut) || !tSeen.add(ST.identityKey(tIn))) continue;
			rInputs.add(tIn); rOutputs.add(tOut);
		}
	}

	/** A cell {@code null}/{@code ItemStack}/{@code List<ItemStack>} (see {@link ShapedOreRecipe#getInput()}
	 *  and {@link ShapelessOreRecipe#getInput()}) -> {@code List<ItemStack>} for {@link ICraftingGridHelper}. */
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

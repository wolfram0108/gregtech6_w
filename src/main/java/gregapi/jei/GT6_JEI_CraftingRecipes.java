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
import gregapi.oredict.OreDictItemData;
import gregapi.oredict.OreDictMaterial;
import gregapi.oredict.OreDictPrefix;
import gregapi.recipes.AdvancedCrafting1ToY;
import gregapi.recipes.AdvancedCraftingXToY;
import gregapi.recipes.GT6CraftingDisplay;
import gregapi.recipes.ICraftingRecipeGT;
import gregapi.recipes.ShapedOreRecipe;
import gregapi.recipes.ShapelessOreRecipe;
import gregapi.util.OM;
import gregapi.util.ST;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

/**
 * The SINGLE center that shows GT6 crafting in JEI (issue #45). In 1.7.10 the recipes were ordinary {@code IRecipe}s
 * of the one CraftingManager, so NEI's own crafting handler drew them; in the port they live in GT6's buffer (F11)
 * and a foreign viewer cannot see that buffer. This class turns a buffer recipe into the display-only
 * {@link GT6CraftingDisplay}, which JEI's native crafting category and AE2's pattern encoder accept as crafting.
 *
 * <p>One display per recipe, except the prefix families ({@code AdvancedCrafting1ToY}/{@code XToY}): their output
 * depends on the material, so each material pair becomes its own card (the owner's requirement, 2026-10-30).</p>
 */
public final class GT6_JEI_CraftingRecipes {
	private GT6_JEI_CraftingRecipes() {}

	/** A display recipe with the stacks it shows, so the JEI lookup can index it without building its layout. */
	public record Adapter(CraftingRecipe recipe, List<ItemStack> inputs, ItemStack output) {}

	/** 1.7.10 display rule (BUG-121): a recipe with an EMPTY variant list in a cell was not drawn by NEI either
	 *  ({@code NotEnoughItems ShapedRecipeHandler:157-158}). */
	public static boolean showable(ICraftingRecipeGT aRecipe) {
		try {
			if (aRecipe instanceof ShapedOreRecipe tShaped) return noEmptyChoice(tShaped.getInput());
			if (aRecipe instanceof ShapelessOreRecipe tShapeless) return noEmptyChoice(tShapeless.getInput().toArray());
			return !pairs(aRecipe).isEmpty();
		} catch (Throwable e) {
			// NEI also returned null on a parse exception, i.e. it did not show it either (ShapedRecipeHandler:161-164)
			return false;
		}
	}

	/** The display cards of one buffer recipe; {@code aIndex} only makes their ids unique and stable within a run. */
	public static List<Adapter> adapters(ICraftingRecipeGT aRecipe, int aIndex) {
		List<Adapter> rAdapters = new ArrayList<>(1);
		if (aRecipe instanceof ShapedOreRecipe tShaped) {
			ItemStack tOutput = tShaped.getRecipeOutput();
			if (!ST.valid(tOutput)) return rAdapters;
			List<List<ItemStack>> tCells = cells(tShaped.getInput());
			rAdapters.add(adapter(aIndex, "", new GT6CraftingDisplay(id(aIndex, ""), false, tShaped.getWidth(), tShaped.getHeight(), tCells, ST.copy(tOutput)), tCells, tOutput));
			return rAdapters;
		}
		if (aRecipe instanceof ShapelessOreRecipe tShapeless) {
			ItemStack tOutput = tShapeless.getRecipeOutput();
			if (!ST.valid(tOutput)) return rAdapters;
			List<List<ItemStack>> tCells = cells(tShapeless.getInput().toArray());
			rAdapters.add(adapter(aIndex, "", new GT6CraftingDisplay(id(aIndex, ""), true, 0, 0, tCells, ST.copy(tOutput)), tCells, tOutput));
			return rAdapters;
		}
		// Prefix families: the output follows the material, so the card must too — one card per material pair.
		List<ItemStack[]> tPairs = pairs(aRecipe);
		for (int i = 0; i < tPairs.size(); i++) {
			ItemStack tInput = tPairs.get(i)[0], tOutput = tPairs.get(i)[1];
			List<List<ItemStack>> tCells = new ArrayList<>();
			boolean tShapeless;
			int tWidth = 0, tHeight = 0;
			if (aRecipe instanceof AdvancedCrafting1ToY tOne && tOne.mEmpty < 9) {
				// The product is chosen by the cell the item sits in; a 3x3 grid keeps that cell, as in 1.7.10.
				for (int c = 0; c < 9; c++) tCells.add(c == tOne.mEmpty ? List.of(tInput) : List.of());
				tShapeless = false; tWidth = 3; tHeight = 3;
			} else if (aRecipe instanceof AdvancedCraftingXToY tMany && tMany.mInputCount > 0 && tMany.mInputCount <= 9) {
				// Counted, not placed: a flat list of the same item, which is what the recipe matches.
				for (int c = 0; c < tMany.mInputCount; c++) tCells.add(List.of(tInput));
				tShapeless = true;
			} else {
				// A cell index beyond the grid (or a malformed count): still show the material pair, without the layout.
				tCells.add(List.of(tInput));
				tShapeless = true;
			}
			String tVariant = "/" + i;
			rAdapters.add(adapter(aIndex, tVariant, new GT6CraftingDisplay(id(aIndex, tVariant), tShapeless, tWidth, tHeight, tCells, ST.copy(tOutput)), tCells, tOutput));
		}
		return rAdapters;
	}

	/** Ids of vanilla recipes that a SHOWN GT6 recipe replaces (F4 role-C): the viewer must keep one page, as in 1.7.10. */
	public static Set<ResourceLocation> replacedSources(Iterable<ICraftingRecipeGT> aShown) {
		Set<ResourceLocation> rSources = new HashSet<>();
		for (ICraftingRecipeGT tRecipe : aShown) {
			ResourceLocation tSource = null;
			if (tRecipe instanceof ShapedOreRecipe tShaped && tShaped.mVanillaReplacement) tSource = tShaped.mSourceId;
			else if (tRecipe instanceof ShapelessOreRecipe tShapeless && tShapeless.mVanillaReplacement) tSource = tShapeless.mSourceId;
			if (tSource != null) rSources.add(tSource);
		}
		return rSources;
	}

	private static ResourceLocation id(int aIndex, String aVariant) {
		return new ResourceLocation(MD.GAPI.mID, "gt6_crafting/" + aIndex + aVariant);
	}

	private static Adapter adapter(int aIndex, String aVariant, GT6CraftingDisplay aDisplay, List<List<ItemStack>> aCells, ItemStack aOutput) {
		return new Adapter(aDisplay, flatten(aCells), ST.copy(aOutput));
	}

	private static List<ItemStack> flatten(List<List<ItemStack>> aCells) {
		List<ItemStack> rStacks = new ArrayList<>();
		for (List<ItemStack> tCell : aCells) rStacks.addAll(tCell);
		return rStacks;
	}

	/** No cell is an EMPTY list of options (the 1.7.10 display rule above). */
	private static boolean noEmptyChoice(Object[] aCells) {
		if (aCells == null) return false;
		for (Object tCell : aCells) if (tCell instanceof List && ((List<?>)tCell).isEmpty()) return false;
		return true;
	}

	/** A cell {@code null}/{@code ItemStack}/{@code List<ItemStack>} -> the stacks a JEI slot shows. */
	private static List<List<ItemStack>> cells(Object[] aCells) {
		List<List<ItemStack>> rCells = new ArrayList<>(aCells.length);
		for (Object tCell : aCells) {
			List<ItemStack> tStacks = new ArrayList<>();
			if (tCell instanceof ItemStack tStack) {if (ST.valid(tStack)) tStacks.add(ST.copy(tStack));}
			else if (tCell instanceof List<?> tList) for (Object tAlt : tList) if (tAlt instanceof ItemStack tStack && ST.valid(tStack)) tStacks.add(ST.copy(tStack));
			rCells.add(tStacks);
		}
		return rCells;
	}

	/** The material pairs of a prefix family, input first: the same loop the cycling card used, now one pair per card. */
	private static List<ItemStack[]> pairs(ICraftingRecipeGT aRecipe) {
		List<ItemStack[]> rPairs = new ArrayList<>();
		OreDictPrefix tIn, tOut;
		int tCount;
		Predicate<OreDictMaterial> tHasOutput;
		if (aRecipe instanceof AdvancedCrafting1ToY tOne) {tIn = tOne.mInput; tOut = tOne.mOutput; tCount = tOne.mOutputCount; tHasOutput = tOne::hasOutputFor;}
		else if (aRecipe instanceof AdvancedCraftingXToY tMany) {tIn = tMany.mInput; tOut = tMany.mOutput; tCount = tMany.mOutputCount; tHasOutput = tMany::hasOutputFor;}
		else return rPairs;
		Set<String> tSeen = new HashSet<>();
		for (OreDictMaterial tMaterial : new LinkedHashSet<>(OreDictMaterial.MATERIAL_MAP.values())) {
			if (tMaterial == null) continue;
			ItemStack tInput = tIn.mat(tMaterial, 1);
			if (!ST.valid(tInput)) continue;
			// The recipe judges the STACK, and the dictionary may unify it into another material or prefix,
			// so the pair is built from what the stack really is — never from the material key asked for.
			OreDictItemData tData = OM.anydata_(tInput);
			if (tData == null || tData.mPrefix != tIn || tData.mMaterial == null) continue;
			OreDictMaterial tActual = tData.mMaterial.mMaterial;
			if (tActual == null || !tHasOutput.test(tActual)) continue;
			ItemStack tOutput = tOut.mat(tActual, tCount);
			if (!ST.valid(tOutput) || !tSeen.add(ST.identityKey(tInput))) continue;
			rPairs.add(new ItemStack[] {tInput, tOutput});
		}
		return rPairs;
	}
}

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

package gregapi.recipes;

import static gregapi.data.CS.*;

import java.util.HashMap;
import java.util.Map;

import gregapi.util.ST;
import net.minecraft.world.item.ItemStack;

/** Vanilla's mutable FurnaceRecipes singleton is gone (smelting is now immutable datapack data), but GT6
 *  still mutates smelting at runtime, so this reproduces the old API 1:1 over GT6's own mutable storage. */
public class FurnaceRecipes {
	private static final FurnaceRecipes INSTANCE = new FurnaceRecipes();

	/** 1.7.10's FurnaceRecipes.smelting() singleton accessor. */
	public static FurnaceRecipes smelting() {return INSTANCE;}

	private final Map<ItemStack, ItemStack> mSmeltingList   = new HashMap<>();
	private final Map<ItemStack, Float>     mExperienceList = new HashMap<>();

	/** 1.7.10's getSmeltingResult: first output whose input matches, wildcard-aware, same as GT6's own iteration. */
	public ItemStack getSmeltingResult(ItemStack aInput) {
		if (ST.invalid(aInput)) return NI;
		for (Map.Entry<ItemStack, ItemStack> tEntry : mSmeltingList.entrySet()) if (ST.equal(aInput, tEntry.getKey(), T)) return tEntry.getValue();
		return NI;
	}

	/** 1.7.10 func_151394_a = addSmeltingRecipe(input, output, experience). */
	public void func_151394_a(ItemStack aInput, ItemStack aOutput, float aExperience) {
		if (ST.invalid(aInput) || ST.invalid(aOutput)) return;
		mSmeltingList.put(aInput, aOutput);
		mExperienceList.put(aOutput, aExperience);
	}

	/** 1.7.10's getSmeltingList: a mutable map GT6 iterates and removes from directly. */
	public Map<ItemStack, ItemStack> getSmeltingList() {return mSmeltingList;}

	/** Matches the original rule: the result item's own hook is asked first and its non-(-1) answer overrides
	 *  the map; a type that doesn't implement the hook gets the vanilla default of -1 (ask the map), else 0. */
	public float func_151398_b(ItemStack aOutput) {
		if (ST.invalid(aOutput)) return 0.0F;
		if (aOutput.getItem() instanceof gregapi.item.IItemSmeltingExperience tItem) {
			float tXP = tItem.getSmeltingExperience(aOutput);
			if (tXP != -1) return tXP;
		}
		for (Map.Entry<ItemStack, Float> tEntry : mExperienceList.entrySet()) if (ST.equal(aOutput, tEntry.getKey(), T)) return tEntry.getValue();
		return 0.0F;
	}

	private boolean mShowcaseFilled = false;

	/** The vanilla half of this list and the furnace showcase, from the smelting recipes at hand: the server's at its start,
	 *  a dedicated-server client's on receiving them. 1.7.10 filled the showcase on the client, once per process. */
	public void restoreVanilla(Iterable<? extends net.minecraft.world.item.crafting.RecipeHolder<?>> aSmelting) {
		int tImported = importVanilla(aSmelting);
		if (mShowcaseFilled) {OUT.println("[GT6] F11-smelting: vanilla smeltings ported into the GT6 registry: " + tImported + "; furnace showcase already filled this session"); return;}
		mShowcaseFilled = true;
		// Counted by the map's size growing: its indexing hook returns null by design (RecipeMapNonGTRecipes), not by failure.
		int tBefore = gregapi.data.RM.Furnace.mRecipeListSize;
		for (Map.Entry<ItemStack, ItemStack> tEntry : new java.util.ArrayList<>(mSmeltingList.entrySet())) {
			if (ST.invalid(tEntry.getKey())) continue;
			gregapi.recipes.Recipe tRecipe = gregapi.data.RM.Furnace.findRecipe(null, null, F, Long.MAX_VALUE, NI, ZL_FS, ST.array(ST.copy(tEntry.getKey())));
			if (tRecipe != null) gregapi.data.RM.Furnace.addFakeRecipe(F, tRecipe);
		}
		OUT.println("[GT6] F11-smelting: vanilla smeltings ported into the GT6 registry: " + tImported + "; furnace showcase (JEI) filled: " + (gregapi.data.RM.Furnace.mRecipeListSize - tBefore) + " recipes");
	}

	/** Restores the vanilla half of this list, lost when the class became GT6's own storage instead of vanilla's;
	 *  by user decision every vanilla recipe is imported, including items that did not exist in 1.7.10. */
	public int importVanilla(Iterable<? extends net.minecraft.world.item.crafting.RecipeHolder<?>> aSmelting) {
		int rAdded = 0;
		try {
			// The smelting recipes also hold GT6's own dispatcher entry, a bridge into this same list rather than data.
			for (net.minecraft.world.item.crafting.RecipeHolder<?> tHolder : aSmelting) {
				if (tHolder.value() instanceof GT6SmeltingDispatcher) continue;
				if (!(tHolder.value() instanceof net.minecraft.world.item.crafting.SmeltingRecipe tRecipe)) continue;
				for (net.minecraft.core.Holder<net.minecraft.world.item.Item> tItem : tRecipe.input().items().toList()) {
					ItemStack tIn = new ItemStack(tItem);
					if (ST.invalid(tIn)) continue;
					// A GT6 recipe already registered for this input takes priority and is not overwritten.
					if (ST.valid(getSmeltingResult(tIn))) continue;
					ItemStack tOut = tRecipe.assemble(new net.minecraft.world.item.crafting.SingleRecipeInput(tIn));
					if (ST.invalid(tOut)) continue;
					func_151394_a(tIn, ST.copy(tOut), tRecipe.experience());
					rAdded++;
				}
			}
		} catch (Throwable e) {
			e.printStackTrace(gregapi.data.CS.ERR);
		}
		return rAdded;
	}
}

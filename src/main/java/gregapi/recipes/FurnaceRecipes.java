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
import net.minecraft.world.item.crafting.Ingredient;

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

	/** Restores the vanilla half of this list, lost when the class became GT6's own storage instead of vanilla's;
	 *  by user decision every vanilla recipe is imported, including items that did not exist in 1.7.10. */
	public int importVanilla(net.minecraft.server.MinecraftServer aServer) {
		if (aServer == null) return 0;
		int rAdded = 0;
		try {
			net.minecraft.server.level.ServerLevel tLevel = aServer.overworld();
			if (tLevel == null) return 0;
			// The SMELTING registry also holds GT6's own bridge dispatcher, which relays into this same registry rather than
			// carrying data; since it's now a SmeltingRecipe itself, filtering by class can't exclude it, so it's excluded by name.
			// (forge-1201-decompiled RecipeManager.java:96-98).
			for (net.minecraft.world.item.crafting.Recipe<?> tAny
				: tLevel.getRecipeManager().getAllRecipesFor(net.minecraft.world.item.crafting.RecipeType.SMELTING)) {
				if (tAny instanceof GT6SmeltingDispatcher) continue;
				if (!(tAny instanceof net.minecraft.world.item.crafting.SmeltingRecipe tRecipe)) continue;
				for (Ingredient tIngredient : tRecipe.getIngredients()) for (ItemStack tItem : tIngredient.getItems()) {
					ItemStack tIn = ST.amount(1, tItem);
					if (ST.invalid(tIn)) continue;
					// A GT6 recipe already registered for this input takes priority and is not overwritten.
					if (ST.valid(getSmeltingResult(tIn))) continue;
					ItemStack tOut = tRecipe.assemble(new net.minecraft.world.SimpleContainer(tIn), tLevel.registryAccess());
					if (ST.invalid(tOut)) continue;
					func_151394_a(tIn, ST.copy(tOut), tRecipe.getExperience());
					rAdded++;
				}
			}
		} catch (Throwable e) {
			e.printStackTrace(gregapi.data.CS.ERR);
		}
		return rAdded;
	}
}

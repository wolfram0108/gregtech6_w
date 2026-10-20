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

package gt6u;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import gregapi.oredict.OreDictManager;
import gregapi.oredict.event.IOreDictListenerEvent;
import gregapi.recipes.ICraftingRecipeGT;
import gregapi.recipes.IRecipeMapHandler;
import gregapi.recipes.Recipe;
import gregapi.recipes.RecipeCapture;
import gregapi.recipes.Recipe.RecipeMap;
import gregapi.util.CR;
import gregapi.util.ST;

import static gregapi.data.CS.*;

/** Takes back GT6 Recipes GregTech6-Unofficial dropped: GT6's own statement runs with the recipe capture open, and the Recipes it
 *  would make are taken out of GT6's maps and crafting list again. */
public class GT6U_Recipes {
	/** Takes back the GT6 Recipes, Recipe Handlers and OreDict listeners these restated GT6 statements make; what is not found is
	 *  listed in GT6's log under aSource. */
	public static void takeBack(String aSource, Runnable aGT6Statements) {
		RecipeCapture.Captured tCaptured = RecipeCapture.capture(aGT6Statements);
		int tMissing = 0;
		for (Map.Entry<RecipeMap, Recipe> tEntry : tCaptured.mMachine) if (tEntry.getKey().removeSame(tEntry.getValue()) <= 0) {tMissing++; ERR.println("GT6U take back, not found in " + aSource + ": " + tEntry.getKey().mNameInternal + " " + describe(tEntry.getValue()));}
		for (ICraftingRecipeGT tRecipe : tCaptured.mCrafting) if (CR.removeSame(tRecipe) <= 0) {tMissing++; ERR.println("GT6U take back, not found in " + aSource + ": crafting " + tRecipe.getClass().getSimpleName() + " -> " + ST.configNames(tRecipe.getRecipeOutput()));}
		for (Map.Entry<RecipeMap, IRecipeMapHandler> tEntry : tCaptured.mHandlers) if (tEntry.getKey().removeSameHandler(tEntry.getValue()) <= 0) {tMissing++; ERR.println("GT6U take back, not found in " + aSource + ": handler " + tEntry.getKey().mNameInternal + " " + tEntry.getValue().getClass().getSimpleName());}
		// One pass per Map that lost something: its tank sizes only rise on add, so they are rebuilt from what is left.
		LinkedHashSet<RecipeMap> tChanged = new LinkedHashSet<>();
		for (Map.Entry<RecipeMap, Recipe> tEntry : tCaptured.mMachine) tChanged.add(tEntry.getKey());
		for (Map.Entry<RecipeMap, IRecipeMapHandler> tEntry : tCaptured.mHandlers) tChanged.add(tEntry.getKey());
		for (RecipeMap tMap : tChanged) tMap.recalculateFluidSizes();
		// A dropped OreDict listener goes whole, from every name it listens to, or later registrations would make its Recipes again.
		Map<IOreDictListenerEvent, List<String>> tListeners = new LinkedHashMap<>();
		for (Map.Entry<String, IOreDictListenerEvent> tEntry : tCaptured.mListeners) tListeners.computeIfAbsent(tEntry.getValue(), k -> new ArrayList<>()).add(tEntry.getKey());
		for (Map.Entry<IOreDictListenerEvent, List<String>> tEntry : tListeners.entrySet()) {
			int tRemoved = OreDictManager.INSTANCE.removeSameListeners(tEntry.getValue(), tEntry.getKey());
			if (tRemoved < 0) ERR.println("GT6U take back, cannot be told apart in " + aSource + ": OreDict listener of " + tEntry.getValue() + ", nothing is registered under these names");
			else if (tRemoved == 0) {tMissing++; ERR.println("GT6U take back, not found in " + aSource + ": OreDict listener of " + tEntry.getValue());}
		}
		if (tMissing > 0) ERR.println("GT6U: " + tMissing + " of " + (tCaptured.mMachine.size() + tCaptured.mCrafting.size() + tCaptured.mHandlers.size() + tCaptured.mListeners.size()) + " GT6 Recipes and listeners to take back were not found in " + aSource);
	}
	
	/** Before a statement GT6U put ahead of GT6's statements aLaterGT6 in the same code: a GT6 Recipe of theirs it collides with
	 *  gives way, as in GT6U. null after the statement ends it and lists what gave way in GT6's log. */
	public static void first(Runnable aLaterGT6) {
		RecipeCapture.first(aLaterGT6);
		if (aLaterGT6 != null) return;
		// A Recipe that gave way is a removal like a take back: rebuild the tank sizes of its Map once, from what is left.
		LinkedHashSet<RecipeMap> tChanged = new LinkedHashSet<>();
		for (Map.Entry<RecipeMap, Recipe> tEntry : RecipeCapture.gaveWay()) {
			ERR.println("GT6U first, gave way: " + tEntry.getKey().mNameInternal + " " + describe(tEntry.getValue()));
			tChanged.add(tEntry.getKey());
		}
		for (RecipeMap tMap : tChanged) tMap.recalculateFluidSizes();
	}

	private static String describe(Recipe aRecipe) {
		return ST.configNames(aRecipe.mInputs) + " + " + java.util.Arrays.toString(aRecipe.mFluidInputs) + " -> " + ST.configNames(aRecipe.mOutputs) + " + " + java.util.Arrays.toString(aRecipe.mFluidOutputs) + " @" + aRecipe.mEUt + "x" + aRecipe.mDuration;
	}
}

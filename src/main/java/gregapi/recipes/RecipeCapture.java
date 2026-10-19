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

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import gregapi.recipes.Recipe.RecipeMap;

/** Restating GT6 statements without their effect on GT6's Recipes: Recipe Maps (Recipes and Recipe Handlers), the crafting list and
 *  OreDict listeners hand what they would add to the capture instead. An addon takes back a GT6 Recipe this way; GT6 never opens it. */
public final class RecipeCapture {
	private RecipeCapture() {}

	private static List<Map.Entry<RecipeMap, Recipe>> sMachine = null;
	private static List<ICraftingRecipeGT> sCrafting = null;
	private static List<Map.Entry<RecipeMap, IRecipeMapHandler>> sHandlers = null;
	private static List<Map.Entry<String, gregapi.oredict.event.IOreDictListenerEvent>> sListeners = null;

	/** Recipes a restatement made, none of them added. */
	public static final class Captured {
		public final List<Map.Entry<RecipeMap, Recipe>> mMachine = new ArrayList<>();
		public final List<ICraftingRecipeGT> mCrafting = new ArrayList<>();
		public final List<Map.Entry<RecipeMap, IRecipeMapHandler>> mHandlers = new ArrayList<>();
		/** OreDict listeners the restatement made, by name: replayed on what is registered, they are not kept to hear more. */
		public final List<Map.Entry<String, gregapi.oredict.event.IOreDictListenerEvent>> mListeners = new ArrayList<>();

		public boolean isEmpty() {return mMachine.isEmpty() && mCrafting.isEmpty() && mHandlers.isEmpty();}

		/** Made the same Recipes: equal machine Recipes in equal maps and equal crafting Recipes, as many of each. */
		public boolean sameRecipes(Captured aOther) {
			if (mMachine.size() != aOther.mMachine.size() || mCrafting.size() != aOther.mCrafting.size()) return false;
			List<Map.Entry<RecipeMap, Recipe>> tLeft = new ArrayList<>(aOther.mMachine);
			for (Map.Entry<RecipeMap, Recipe> tEntry : mMachine) {
				Map.Entry<RecipeMap, Recipe> tMatch = null;
				for (Map.Entry<RecipeMap, Recipe> tOther : tLeft) if (tOther.getKey() == tEntry.getKey() && tOther.getValue().sameAs(tEntry.getValue())) {tMatch = tOther; break;}
				if (tMatch == null) return false;
				tLeft.remove(tMatch);
			}
			List<ICraftingRecipeGT> tLeftCrafting = new ArrayList<>(aOther.mCrafting);
			for (ICraftingRecipeGT tRecipe : mCrafting) {
				ICraftingRecipeGT tMatch = null;
				for (ICraftingRecipeGT tOther : tLeftCrafting) if (gregapi.util.CR.sameRecipe(tOther, tRecipe)) {tMatch = tOther; break;}
				if (tMatch == null) return false;
				tLeftCrafting.remove(tMatch);
			}
			return true;
		}
	}

	public static boolean open() {return sMachine != null;}

	private static List<Map.Entry<RecipeMap, Recipe>> sFirst = null, sGaveWay = new ArrayList<>();

	/** Before an addon's statement that its sources put before these GT6 statements (aLaterGT6, restated under the capture): a
	 *  Recipe of theirs it collides with gives way, as one of theirs never got in where the addon's statement came first. null
	 *  ends it. */
	public static void first(Runnable aLaterGT6) {sFirst = aLaterGT6 == null ? null : capture(aLaterGT6).mMachine;}

	/** Called by a Recipe Map whose new Recipe collides with aOld: true when aOld gave way and is out of the map. */
	public static boolean yields(RecipeMap aMap, Recipe aOld) {
		if (sFirst == null) return false;
		for (Map.Entry<RecipeMap, Recipe> tLater : sFirst) if (tLater.getKey() == aMap && tLater.getValue().sameAs(aOld)) {
			if (!aMap.remove(aOld)) return false;
			sGaveWay.add(new AbstractMap.SimpleEntry<>(aMap, aOld));
			return true;
		}
		return false;
	}

	/** The Recipes that gave way since the last call. */
	public static List<Map.Entry<RecipeMap, Recipe>> gaveWay() {
		List<Map.Entry<RecipeMap, Recipe>> rGaveWay = sGaveWay;
		sGaveWay = new ArrayList<>();
		return rGaveWay;
	}

	public static Captured capture(Runnable aRestatement) {
		Captured rCaptured = new Captured();
		List<Map.Entry<RecipeMap, Recipe>> tMachine = sMachine;
		List<ICraftingRecipeGT> tCrafting = sCrafting;
		List<Map.Entry<RecipeMap, IRecipeMapHandler>> tHandlers = sHandlers;
		List<Map.Entry<String, gregapi.oredict.event.IOreDictListenerEvent>> tListeners = sListeners;
		sMachine = rCaptured.mMachine; sCrafting = rCaptured.mCrafting; sHandlers = rCaptured.mHandlers; sListeners = rCaptured.mListeners;
		try {aRestatement.run();} finally {sMachine = tMachine; sCrafting = tCrafting; sHandlers = tHandlers; sListeners = tListeners;}
		return rCaptured;
	}

	/** Called by a Recipe Map about to add a Recipe: true when the capture took it instead. */
	public static boolean take(RecipeMap aMap, Recipe aRecipe) {
		if (sMachine == null) return false;
		sMachine.add(new AbstractMap.SimpleEntry<>(aMap, aRecipe));
		return true;
	}

	/** Called by a Recipe Map about to add a Recipe Handler: true when the capture took it instead. */
	public static boolean take(RecipeMap aMap, IRecipeMapHandler aHandler) {
		if (sHandlers == null) return false;
		sHandlers.add(new AbstractMap.SimpleEntry<>(aMap, aHandler));
		return true;
	}

	/** Called by an OreDict Names listener adding a listener of one name: the capture notes it. */
	public static void take(String aOreDictName, gregapi.oredict.event.IOreDictListenerEvent aListener) {
		if (sListeners != null) sListeners.add(new AbstractMap.SimpleEntry<>(aOreDictName, aListener));
	}

	/** Called by the crafting list about to add a Recipe: true when the capture took it instead. */
	public static boolean take(ICraftingRecipeGT aRecipe) {
		if (sCrafting == null) return false;
		sCrafting.add(aRecipe);
		return true;
	}
}

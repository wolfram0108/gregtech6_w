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

import static gregapi.data.CS.*;

import gregapi.recipes.Recipe;
import gregapi.recipes.Recipe.RecipeMap;

/**
 * What JEI's start asks of GT6's recipe maps, kept free of JEI types so it loads without JEI.
 * <p>At its start JEI asks every category whether it is empty; a map answers with the first recipe it already shows, and
 * a map with none has to run its on-demand generation first. {@link #warmEmptyMaps} runs that generation on the
 * integrated server as it starts, so it is done before the client joins and JEI starts on the render thread.</p>
 */
public final class GT6_JEI_StartMaps {
	private GT6_JEI_StartMaps() {}

	/** The first recipe the map already shows, or null. The integrated server may grow the map while the client reads it. */
	public static Recipe firstReady(RecipeMap aMap) {
		try {for (Recipe tRecipe : aMap.mRecipeList) if (tRecipe != null && tRecipe.mEnabled && !tRecipe.mHidden) return tRecipe;} catch (Throwable e) {/* the caller falls back to the full list */}
		return null;
	}

	/** The same getNEIAllRecipes call JEI's emptiness check would make, for every shown map that has nothing ready yet. */
	public static void warmEmptyMaps() {
		if (!net.minecraftforge.fml.ModList.get().isLoaded("jei")) return;
		long tStart = System.nanoTime();
		int tWarmed = 0;
		for (RecipeMap tMap : RecipeMap.RECIPE_MAP_LIST) {
			if (!tMap.mNEIAllowed || firstReady(tMap) != null) continue;
			try {tMap.getNEIAllRecipes(); tWarmed++;} catch (Throwable e) {e.printStackTrace(ERR);}
		}
		OUT.println("[GT6-JEI] maps with nothing ready generated before JEI starts: " + tWarmed + ", " + (System.nanoTime() - tStart) / 1000000 + " ms on the server thread");
	}
}

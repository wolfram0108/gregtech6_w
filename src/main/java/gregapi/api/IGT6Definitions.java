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

package gregapi.api;

import java.util.function.Function;
import java.util.function.Supplier;

import gregapi.oredict.OreDictItemData;
import gregapi.oredict.OreDictMaterial;
import gregapi.recipes.Recipe.RecipeMap;
import net.minecraft.world.item.ItemStack;

/** An addon's changes to GT6's static definitions and recipe helpers, asked while GT6 runs them, before any addon code would run
 *  otherwise. Registered as a service in META-INF/services/gregapi.api.IGT6Definitions; a null answer keeps GT6's own. */
public interface IGT6Definitions {
	/** The mod this addon is: GT6 runs its answers as that mod's code, so the texts they write are the addon's. */
	String modID();
	/** The Recipe Map GT6 keeps under this internal name, or null for GT6's own; aGT6 builds GT6's own from GT6's English aNameLocal. */
	default RecipeMap recipeMap(String aNameInternal, String aNameLocal, Function<String, RecipeMap> aGT6) {return null;}
	/** The material GT6 defines under this name, or null for GT6's own; aGT6 defines GT6's own when called. */
	default OreDictMaterial material(String aName, Supplier<OreDictMaterial> aGT6) {return null;}
	/** True when the addon drops GT6's definition of this name: GT6 then does not make it at all. */
	default boolean drops(String aName) {return false;}
	/** The number GT6 keeps under this name, or null for GT6's own aGT6. */
	default Long number(String aName, long aGT6) {return null;}
	/** Makes the Fermenter's biomass Recipes of this item in place of RM.biomass and returns its result, or null to leave them to GT6. */
	default Boolean biomass(ItemStack aBiomass, long aSpeed) {return null;}
	/** The data a MultiItem registers for this item of its addItem, or null for GT6's own aGT6. */
	default OreDictItemData itemData(ItemStack aStack, OreDictItemData aGT6) {return null;}
}

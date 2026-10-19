/**
 * Copyright (c) 2020 Gregtech6-Unofficial Team
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
 *
 * Modified in 2026 for the GregTech 6 NeoForge port (https://github.com/wolfram0108/gregtech6_w):
 * the GregTech6-Unofficial redefinitions of four recipe maps of gregapi/data/RM.java, given to GT6 as an addon service.
 */

package gt6u;

import java.util.function.Function;
import java.util.function.Supplier;

import gregapi.api.GT6Definitions;
import gregapi.api.IGT6Definitions;
import gregapi.data.FL;
import gregapi.data.RM;
import gregapi.oredict.OreDictItemData;
import gregapi.oredict.OreDictMaterial;
import gregapi.recipes.Recipe.RecipeMap;
import gregapi.recipes.maps.RecipeMapFormingPress;
import gregapi.util.ST;
import gt6u.data.FL_GT6U;
import gt6u.data.MT_GT6U;
import gt6u.items.MultiItemTechnological_Recipes;
import net.minecraft.world.item.ItemStack;

import static gregapi.data.CS.*;

/** GregTech6-Unofficial's changes to GT6's static definitions and recipe helpers, asked by GT6 while it runs them (META-INF/services). */
public class GT6U_Definitions implements IGT6Definitions {
	@Override public String modID() {return GT6U_Main.MOD_ID;}

	@Override
	public RecipeMap recipeMap(String aNameInternal, String aNameLocal, Function<String, RecipeMap> aGT6) {
		switch (aNameInternal) {
		case "gt.recipe.heatmixer": return heatMixer();
		case "gt.recipe.fermenter": return fermenter();
		case "gt.recipe.press"    : return press();
		case "gt.recipe.assembler": return assembler();
		default: return null;
		}
	}
	
	@Override
	public OreDictMaterial material(String aName, Supplier<OreDictMaterial> aGT6) {
		switch (aName) {
		case GT6Definitions.MATERIAL_SAND       : return MT_GT6U.InMT.sand();
		case GT6Definitions.MATERIAL_SLUICE_SAND: return MT_GT6U.InMT.sluiceSand();
		case GT6Definitions.MATERIAL_NEUTRONIUM : return MT_GT6U.InMT.neutronium();
		case GT6Definitions.MATERIAL_TRITANIUM  : return MT_GT6U.InMT.tritanium();
		case GT6Definitions.MATERIAL_DURANIUM   : return MT_GT6U.InMT.duranium();
		default: return null;
		}
	}
	
	/** GT6U has no unknown(...) elements (GT6U MT.java, GT6 6.14.17 MT.java:523-806), no Steam gas binding and no "kerosene" fluid:
	 *  it names that fluid "kerosine" (GT6U Loader_Fluids.java:101, made by the addon's Loader_Fluids). */
	@Override
	public boolean drops(String aName) {
		switch (aName) {
		case GT6Definitions.UNKNOWN_ELEMENTS: case GT6Definitions.STEAM_GAS: case GT6Definitions.FLUID_KEROSENE: return T;
		default: return F;
		}
	}
	
	@Override
	public Long number(String aName, long aGT6) {
		switch (aName) {
		case GT6Definitions.REPLICATOR_POWER_PER_NUCLEON: return 512L; // GT6U RecipeMapReplicator.java:91
		default: return null;
		}
	}
	
	/** GT6U's data of the three Circuit Plates it rewrote in GT6's MultiItem (GT6U MultiItemTechnological.java), registered in their
	 *  place, as GT6U registered them: the recycling Recipes are made from them. */
	@Override
	public OreDictItemData itemData(ItemStack aStack, OreDictItemData aGT6) {
		return aStack.getItem() == ItemsGT.TECH ? MultiItemTechnological_Recipes.itemData(ST.meta_(aStack)) : null;
	}
	
	/** GT6U's RM.biomass (GT6U RM.java): circuit 5 for the plain Recipes, circuit 6 for those sped up by Biomass Flora, no rotten
	 *  drink or mushroom soup. GT6U's statement stands where GT6 6.17 changed the same one; the mineral water Recipe, which GT6
	 *  added after 6.14.17, stays as GT6 makes it. */
	@Override
	public Boolean biomass(ItemStack aBiomass, long aSpeed) {
		if (ST.invalid(aBiomass)) return F;
		int tSize = aBiomass.getCount();
		long tBiomassFloraAcc = 4;
		
		if (tSize <= 0) return F;
		aBiomass = ST.amount(1, aBiomass);
		for (String tFluid : FluidsGT.WATER) if (FL.exists(tFluid) && !"riverwater".equals(tFluid)) {
			RM.Fermenter.addRecipe2(F, 16, (aSpeed * 4) / tSize, ST.tag(5), aBiomass, FL.make(tFluid, 1080 / tSize), FL.BiomassIC2.make(1080 / tSize), ZL_IS);
			
			RM.Fermenter.addRecipe2(F, 16, (aSpeed * 4 / tBiomassFloraAcc) / tSize, ST.tag(6), aBiomass, FL.array(FL.make(tFluid, 1080 / tSize), FL_GT6U.BiomassFlora.make(1080 / tSize / 8)), FL.BiomassIC2.make(1080 * 2 / tSize), ZL_IS);
		}
		RM.Fermenter.addRecipe1(F, 16, (aSpeed * 4) / tSize, aBiomass, FL.MnWtr.make(                     1080 / tSize), FL.BiomassIC2.make(1080   / tSize, FL.Biomass), ZL_IS);
		for (String tFluid : FluidsGT.MILK ) if (FL.exists(tFluid)) {
			RM.Fermenter.addRecipe2(F, 16, (aSpeed * 3) / tSize, ST.tag(5), aBiomass, FL.make(tFluid, 1080 / tSize), FL.BiomassIC2.make(2160 / tSize), ZL_IS);
			
			RM.Fermenter.addRecipe2(F, 16, (aSpeed * 3 / tBiomassFloraAcc) / tSize, ST.tag(6), aBiomass, FL.array(FL.make(tFluid, 1080 / tSize), FL_GT6U.BiomassFlora.make(1080 / tSize / 8)), FL.BiomassIC2.make(2160 * 2   / tSize), ZL_IS);
		}
		for (String tFluid : FluidsGT.JUICE) if (FL.exists(tFluid) && !"potion.idunsapplejuice".equals(tFluid) && !"potion.goldenapplejuice".equals(tFluid) && !"goldencarrotjuice".equals(tFluid)) {
			RM.Fermenter.addRecipe2(F, 16, (aSpeed * 3) / tSize, ST.tag(5), aBiomass, FL.make(tFluid, 1080 / tSize), FL.BiomassIC2.make(3240 / tSize), ZL_IS);
			RM.Fermenter.addRecipe2(F, 16, aSpeed / tSize, ST.tag(5), aBiomass, FL.make("potion.idunsapplejuice", 209952 / tSize), FL.BiomassIC2.make(2099520 / tSize), ZL_IS);
			RM.Fermenter.addRecipe2(F, 16, aSpeed / tSize, ST.tag(5), aBiomass, FL.make("potion.goldenapplejuice", 23328 / tSize), FL.BiomassIC2.make(233280 / tSize), ZL_IS);
			RM.Fermenter.addRecipe2(F, 16, aSpeed / tSize, ST.tag(5), aBiomass, FL.make("goldencarrotjuice", 2592 / tSize), FL.BiomassIC2.make(25920 / tSize), ZL_IS);
			
			RM.Fermenter.addRecipe2(F, 16, (aSpeed * 3 / tBiomassFloraAcc) / tSize, ST.tag(6), aBiomass, FL.array(FL.make(tFluid, 1080 / tSize), FL_GT6U.BiomassFlora.make(1080 / tSize / 8)), FL.BiomassIC2.make(3240 * 2  / tSize), ZL_IS);
		}
		for (String tFluid : FluidsGT.HONEY) if (FL.exists(tFluid)) {
			RM.Fermenter.addRecipe2(F, 16, (aSpeed * 3) / tSize, ST.tag(5), aBiomass, FL.make(tFluid, 1080 / tSize), FL.BiomassIC2.make(3240 / tSize), ZL_IS);
			RM.Fermenter.addRecipe2(F, 16, (aSpeed * 2) / tSize, ST.tag(5), aBiomass, FL.Honeydew.make(1080 / tSize), FL.BiomassIC2.make(3240 / tSize), ZL_IS);
			RM.Fermenter.addRecipe2(F, 16, (aSpeed * 2) / tSize, ST.tag(5), aBiomass, FL.RoyalJelly.make(1080 / tSize), FL.BiomassIC2.make(32400 / tSize), ZL_IS);
			
			RM.Fermenter.addRecipe2(F, 16, (aSpeed * 3 / tBiomassFloraAcc) / tSize, ST.tag(6), aBiomass, FL.array(FL.make(tFluid, 1080 / tSize), FL_GT6U.BiomassFlora.make(1080 / tSize / 8)), FL.BiomassIC2.make(3240 * 2   / tSize), ZL_IS);
		}
		return T;
	}
	
	/** GT6U's definitions of four GT6 maps (GT6U RM.java:106-150); they differ from GT6's only where GT6U changed them. */
	private static RecipeMap heatMixer() {return new RecipeMap                     (null, "gt.recipe.heatmixer"                    , "Heat Mixer"                      , null, 0, 1, RES_PATH_GUI+"machines/HeatMixer"                 ,/*IN-OUT-MIN-ITEM=*/ 6, 6, 0,/*IN-OUT-MIN-FLUID=*/ 6, 6, 0,/*MIN*/ 2,/*AMP=*/ 1, ""                    ,    1, ""      , T, T, T, T, F);}
	private static RecipeMap fermenter() {return new RecipeMap                     (null, "gt.recipe.fermenter"                    , "Fermenter"                       , null, 0, 1, RES_PATH_GUI+"machines/Fermenter"                 ,/*IN-OUT-MIN-ITEM=*/ 2, 1, 1,/*IN-OUT-MIN-FLUID=*/ 2, 1, 0,/*MIN*/ 1,/*AMP=*/ 1, ""                    ,    1, ""      , T, T, T, T, F, T, T);}
	private static RecipeMap press    () {return new RecipeMapFormingPress         (null, "gt.recipe.press"                        , "Press"                           , null, 0, 1, RES_PATH_GUI+"machines/Press"                     ,/*IN-OUT-MIN-ITEM=*/ 3, 1, 2,/*IN-OUT-MIN-FLUID=*/ 1, 1, 0,/*MIN*/ 0,/*AMP=*/ 1, ""                    ,    1, ""      , T, T, T, T, F, T, T);}
	private static RecipeMap assembler() {return new RecipeMap                     (null, "gt.recipe.assembler"                    , "Circuit Assembler"               , null, 0, 1, RES_PATH_GUI+"machines/Assembler"                 ,/*IN-OUT-MIN-ITEM=*/ 9, 1, 1,/*IN-OUT-MIN-FLUID=*/ 1, 0, 0,/*MIN*/ 1,/*AMP=*/ 1, ""                    ,    1, ""      , T, T, T, T);}
}

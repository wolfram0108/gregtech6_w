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
 * the GregTech6-Unofficial entries of gregtech/loaders/a/Loader_Fluids.java moved into an addon registry.
 */

package gt6u.loaders;


import gt6u.data.MT_GT6U;

import static gregapi.data.CS.*;
import gregapi.data.CS.FluidsGT;
import gregapi.data.CS.IconsGT;
import gregapi.data.CS.PotionsGT;
import gregapi.data.FL;
import gregapi.data.IL;
import gregapi.data.LH;
import gregapi.data.MD;
import gregapi.data.MT;
import gregapi.data.TD;
import gregapi.data.AM;
import gregapi.fluid.FluidGT;
import gregapi.item.multiitem.food.FoodStatDrink;
import gregapi.old.Textures;
import gregapi.oredict.OreDictMaterial;
import gregapi.render.IIconContainer;
import gregapi.render.TextureSet;
import gregapi.util.ST;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.UseAnim;

import net.minecraftforge.fluids.FluidStack;
import gt6u.GT6U_Recipes;

/** GregTech6-Unofficial's edits of gregtech/loaders/a/Loader_Fluids.java, as statements: what GT6U added runs in run(), what it dropped is taken back in
 *  takeBack(): restated as GT6 makes them, their Recipes are taken out of GT6's maps again. */
public class Loader_Fluids {
	public static void run() {
		run_run();
		// GT6U made these in GT6's Loader_Fluids, before its loop over the materials' own fluids: the loop runs again after them.
		gregtech.loaders.a.Loader_Fluids.createMaterialFluids();
	}
	
	public static void takeBack() {
	}
	
	/** GT6U's additions to Loader_Fluids.run (GT6U gregtech/loaders/a/Loader_Fluids.java). */
	static void run_run() {
		// GT6 6.17 makes these two before this loader, with 10000 (gregtech/loaders/a/Loader_Fluids.java:57-58); GT6U's edit
		// lowered that very line to 4000 (GT6U Loader_Fluids.java:48-49). A second FL.create cannot carry that over: a fluid
		// keeps the temperature it was first given (6.17.08 FL.java:1113), so GT6U's value is set on the fluid GT6 made, in the
		// holder FL.create itself looks up (FL.java:1303); its Fluid object only resolves at RegisterEvent, after this loader.
		FluidGT tHeliumPlasma = FluidGT.BY_NAME.get("heliumplasma"), tNitrogenPlasma = FluidGT.BY_NAME.get("nitrogenplasma");
		if (tHeliumPlasma   != null) tHeliumPlasma  .setTemperature(4000);
		if (tNitrogenPlasma != null) tNitrogenPlasma.setTemperature(4000);
		FL.create("OxygenPlasma" , "Oxygen Plasma" , MT.O , 3, 1000, 4000).setLuminosity(15);
		FL.create("IronPlasma" , "Iron Plasma" , MT.Fe , 3, 1000, 4000).setLuminosity(15);
		FL.create("ProtonPlasma", "Proton Plasma", MT.p, 3, 1000, 4000).setLuminosity(15);
		FL.create("ElectronPlasma", "Electron Plasma", MT.e, 3, 1000, 4000).setLuminosity(15);
		FL.create("PositronPlasma", "Positron Plasma", AM.e, 3, 1000, 4000).setLuminosity(15);
		FL.create("AntiProtonPlasma", "Anti Proton Plasma", AM.p, 3, 1000, 4000).setLuminosity(15);
		FL.create("IonizedHydrogen", "Ionized Hydrogen", MT_GT6U.H_Ion, 3, 1000, 273).setLuminosity(15);
		FL.create("IonizedHelium", "Ionized Helium", MT_GT6U.He_Ion, 3, 1000, 273).setLuminosity(15);
		FL.create("Anti-Nitrogen", "Anti-Nitrogen", AM.N, 2, 1000, 273);
		FL.create("Anti-Hydrogen", "Anti-Hydrogen", AM.H, 2, 1000, 273);
		FL.create("Anti-Deuterium", "Anti-Deuterium", AM.D, 2, 1000, 273);
		FL.create("Anti-Tritium", "Anti-Tritium", AM.T, 2, 1000, 273);
		FL.create("Anti-Helium", "Anti-Helium", AM.He, 2, 1000, 273);
		FL.create("Anti-Helium_3", "Anti-Helium_3", AM.He_3, 2, 1000, 273);
		FL.create("Anti-Carbon_13", "Anti-Carbon_13", AM.C_13, 2, 1000, 2000);
		FL.create("sfuel" , "Fuel Oil With Sulfur", null , 1);
		FL.create("sdiesel" , "Diesel With Sulfur" , null , 1);
		FL.create("spetrol" , "Petrol With Sulfur" , null , 1);
		FL.create("skerosine" , "Kerosine With Sulfur", null , 1);
		FL.create("snaphtha" , "Naphtha With Sulfur" , null , 1);
		FL.create("gasoil" , "Gas Oil" , null , 1);
		FL.create("sgasoil" , "Gas Oil With Sulfur" , null , 1);
		FL.create("lubricant" , "Lubricant" , MT.Lubricant , 1);
		FL.create("Propane" , "Propane" , MT.Propane , 2).setDensity(-1000);
		FL.create("Butane" , "Butane" , MT.Butane , 2).setDensity(-1000);
		FL.create("Ethylene" , "Ethylene" , MT.Ethylene , 2);
		FL.create("Propylene" , "Propylene" , MT.Propylene , 2);
		FL.create("benzene" , "Benzene" , null , 1);
		FL.create("lpg" , "Liquefied Petroleum Gas" , null , 1);
		FL.create("phenol" , "Phenol" , null , 1);
		FL.create("acetone" , "Acetone" , null , 1);
		FL.create("Bisphenol" , "Bisphenol" , null , 1);
		FL.create("Epichlorohydrin" , "Epichlorohydrin" , null , 1);
		FL.create("isoprene" , "Isoprene" , null , 1);
		FL.create("dichloroisopropanol" , "Dichloroisopropanol" , null , 1);
		FL.create("cumene" , "Cumene" , null , 1);
		FL.create("silane" , "Silane" , null , 1);
		FL.create("allylchloride" , "Allyl Chloride" , null , 1);
		FL.create("acrylicacid" , "Acrylic Acid" , null , 1);
		FL.create("ethylacrylate" , "Ethyl Acrylate" , null , 1);
		FL.create("styrene" , "Styrene" , null , 1);
		FL.create("vinylchloride" , "Vinyl Chloride" , null , 1);
		FL.create("ethyne" , "Ethyne" , null , 2);
		FL.create("photoresist" , "Photoresist" , null , 1);
		FL.create("monochlorodifluoromethane", "Monochlorodifluoromethane",null , 2);
		FL.create("trichloromethane" , "Trichloromethane" , null , 2);
		FL.create("tetrafluoroethylene" , "Tetrafluoroethylene" , null , 2);
		FL.create("solutionrichptpa" , "Rich Platinum Palladium Solution" , null , 1);
		FL.create("solutionrichpa" , "Rich Palladium Solution" , null , 1);
		FL.create("moltensodiumbisulfate" , "Molten Sodium Bisulfate" , MT.NaHSO4 , 1);
		FL.create("rhodiumsulfidesolution" , "Rhodium Sulfide Solution" , null , 1);
		FL.create("pregnantsolutionosru" , "Rich Osmium Ruthenium Pregnant Solution" , null , 1);
		FL.create("absorptionliquidosru" , "Rich Osmium Ruthenium Absorption Liquid" , null , 1);
		FL.create("absorptionliquidru" , "Rich Ruthenium Absorption Liquid" , null , 1);
		FL.create("rutheniumtetroxide" , "Ruthenium Tetroxide" , null , 2);
		FL.create("osmiumtetroxide" , "Osmium Tetroxide" , null , 2);
		FL.create("chlororhodiumacid" , "Chlororhodium Acid" , null , 1);
		FL.create("evaporatedliquidos" , "Rich Osmium Evaporated Liquid" , null , 1);
		FL.create("sodiumosmiumhexachloride" , "Ammonium Hexachloroosmium Solution" , null , 1);
		FL.create("biowaste" , "Bio Waste" , null , 1);
		FL.create("agar" , "Agar" , null , 1);
		FL.create("biomass_flora" , "Biomass Flora" , null , 1);
		FL.create("biomass_diluted" , "Diluted Biomass" , null , 1);
		FL.create("bacteria_medium" , "Bacteria Culture Medium" , null , 1);
		FL.create("yeast" , "Yeast" , null , 1);
		FL.create("methanogens" , "Methanogens" , null , 1);
		FL.create("bacillus" , "Bacillus" , null , 1);
		FL.create("clostridia" , "Clostridia" , null , 1);
		FL.create("mscbasalmedium" , "MSC Basal Medium" , null , 1);
		FL.create("mscsupplement" , "MSC Supplement" , null , 1);
		FL.create("msc" , "MSC(Mesenchymal Stem Cell)" , null , 1);
		FL.create("bonemarrow" , "Bone Marrow" , null , 1);
		FL.create("spss" , "Stroke-physiological saline solution" , null , 1);
		FL.create("amylumsolution" , "Amylum Solution" , null , 1);
		FL.create("vitaminsolution" , "Vitamin Solution" , null , 1);
		FL.create("hormonesolution" , "Hormone Solution" , null , 1);
		FL.create("differentiationpromotingproteinneuron" , "Differentiation Promoting Protein Neuron" , null , 1);
		FL.create("methanol" , "Methanol" , null , 1);
		FL.create("coconutoil" , "Coconut Oil" , null , 1);
		FL.create("methyllaurate" , "Methyl Laurate" , null , 1);
		FL.create("methylhydrolaurate" , "Methyl Hydrolaurate" , null , 1);
		FL.create("tris_hcl" , "Tris-HCl Buffer" , null , 1);
		FL.create("aceticacid" , "Acetic Acid" , null , 1);
		FL.create("edta" , "Ethylenediaminetetraacetic acid" , null , 1);
		FL.create("animaltissuehomogenate" , "Animal Tissue Homogenate" , null , 1);
		FL.create("dnaextract" , "DNA Extract" , null , 1);
		FL.create("dnasolution" , "DNA Solution" , null , 1);
		FL.create("bacteria_differentiationpromotingproteinneuron" , "Bacteria(Differentiation Promoting Protein Neuron)" , null , 1);
		FL.create("protein" , "Protein" , null , 1);
		FL.create("dodecane" , "Dodecane" , null , 1);
		FL.create("dodecanechloride" , "Dodecane Chloride" , null , 1);
		FL.create("monochlorodecane" , "Monochlorodecane" , null , 1);
		FL.create("deoxyribonucleotide" , "Deoxyribonucleotide" , null , 1);
		FL.create("primer" , "Primer" , null , 1);
		FL.create("taqdnapolymerase" , "taq DNA polymerase" , null , 1);
		FL.create("escherichiacoli" , "Escherichia coli" , null , 1);
		FL.create("restrictionendonuclease" , "Restriction endonuclease", null , 1);
		FL.create("differentiationpromotingprotein_nervecell_gene", "Differentiation promoting protein (nerve cell) gene", null, 1);
		FL.create("laurylalcohol" , "Lauryl alcohol" , null , 1);
		FL.create("polyvinylchloride" , "Molten Polyvinylchloride" , MT_GT6U.PVC , 1,1000,423);
		FL.create("polytetrafluoroethylene" , "Molten Polytetrafluoroethylene" , MT_GT6U.PTFE , 1,1000,423);
		FL.create("epoxid" , "Molten Epoxid" , MT_GT6U.Epoxid , 1,1000,423);
		FL.create("liquidoxygen" , "Liquid Oxygen" , MT.O , 1, 1000, 85);
		FL.create("liquidhydrogen" , "Liquid Hydrogen" , MT.H , 1, 1000, 20);
		FL.create("electro_etching_solution" , "Electro-Etching Solution", MT_GT6U.ElectroEtchingSolution , 1, 1000, 273);
		FL.create("peroxydisulfuricacid" , "Peroxydisulfuric Acid", MT_GT6U.H2S2O8 , 1, 1000, 273);
		FL.create("mixedester" , "Ester Mixture", null, 1, 1000, 273);
		FL.create("aquaticwaste" , "Aquatic Waste Water", null, 1, 1000, 273);
		FL.create("aquaticnutrition" , "Aquatic Nutrition Water", null, 1, 1000, 273);
		FL.create("ic2biomass" , "Biomass" , MT.Biomass , 1);
		MT.Lubricant.liquid(FL.LubRoCant.make(1000));
		FL.create("Lubricant" , null , MT.Lubricant , 1);
	}

	// GT6U's FL.create of an existing fluid reconfigures it in place (FL.create), which covers the plasmas and gases it re-made.
	// GT6U's dropped MT.Steam.gas(...) and FL.create("kerosene", ...) are not made: GT6U_Definitions.drops.
}

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
 * the GregTech6-Unofficial entries of gregtech/loaders/c/Loader_Recipes_Chem.java moved into an addon registry.
 */

package gt6u.loaders;


import gt6u.data.MT_GT6U;
import gt6u.data.FL_GT6U;
import gt6u.data.RM_GT6U;

import static gregapi.data.CS.*;
import static gregapi.data.OP.*;
import gregapi.data.ANY;
import gregapi.data.CS.FluidsGT;
import gregapi.data.FL;
import gregapi.data.MT;
import gregapi.data.OP;
import gregapi.data.RM;
import gregapi.oredict.OreDictMaterial;
import gregapi.util.OM;
import gregapi.util.ST;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;
import gt6u.GT6U_Recipes;

/** GregTech6-Unofficial's edits of gregtech/loaders/c/Loader_Recipes_Chem.java, as statements: what GT6U added runs in run(), what it dropped is taken back in
 *  takeBack(): restated as GT6 makes them, their Recipes are taken out of GT6's maps again. */
public class Loader_Recipes_Chem {
	public static void run() {
		run_run();
	}
	
	public static void takeBack() {
		takeBack_run();
	}
	
	/** GT6U's additions to Loader_Recipes_Chem.run (GT6U gregtech/loaders/c/Loader_Recipes_Chem.java). */
	static void run_run() {
		RM.Electrolyzer .addRecipe1(T, 16, 800, ST.tag(0), FL.array(MT.H2SO4.fluid(U*14, T)), FL.array(MT.H.fluid(U*2, F), MT_GT6U.H2S2O8.fluid( U*12, F)), NI);
		RM.Mixer .addRecipe1(T, 16, 256, ST.tag(0), FL.array(MT_GT6U.H2S2O8.fluid(U*12, T), MT.H2O.fluid(U*6, T)), FL.array(MT.H2SO4.fluid(U*14, F), MT.H2O2.fluid( U*4, F)), NI);
		RM.Distillery .addRecipe1(T, 16, 24, ST.tag(0), FL.Biomass .make(40), FL.Reikanol.make(12, FL.BioEthanol), FL.DistW.make(20));
		RM.DistillationTower.addRecipe0(F, 64, 16 , FL.array(FL.Biomass .make( 80)), FL.array(FL.BioEthanol.make(20), MT.Glycerol.liquid(U50, F), FL.Methane.make(4), FL.DistW.make(50)), ZL_IS);
		RM.DistillationTower.addRecipe0(F, 64, 16 , FL.array(FL.BiomassIC2 .make( 80)), FL.array(FL.BioEthanol.make(20), MT.Glycerol.liquid(U50, F), FL.Methane.make(4), FL.DistW.make(50)), ZL_IS);
		RM.DistillationTower.addRecipe0(F, 64, 256, new long[]{5000, 5000}, FL.array(FL.Oil_ExtraHeavy.make(25)), FL.array(FL_GT6U.SFuel.make(35), FL_GT6U.SDiesel.make(25), FL_GT6U.SKerosine.make(25), FL_GT6U.SPetrol.make(20), FL_GT6U.SNaphtha.make(25), FL_GT6U.SGasoil.make(15), FL.Propane.make(5), FL.Butane.make(5), FL.lube(50)), dustTiny.mat(MT.WaxParaffin, 1), dustTiny.mat(MT.Asphalt, 1));
		RM.DistillationTower.addRecipe0(F, 64, 196, new long[]{4000, 4000}, FL.array(FL.Oil_Heavy.make(25)), FL.array(FL_GT6U.SFuel.make(30), FL_GT6U.SDiesel.make(20), FL_GT6U.SKerosine.make(20), FL_GT6U.SPetrol.make(15), FL_GT6U.SNaphtha.make(20), FL_GT6U.SGasoil.make(20), FL.Propane.make(10), FL.Butane.make(10), FL.lube(40)), dustTiny.mat(MT.WaxParaffin, 1), dustTiny.mat(MT.Asphalt, 1));
		if(FL.Oil_Heavy2.exists())RM.DistillationTower.addRecipe0(F, 64, 196, new long[]{4000, 4000}, FL.array(FL.Oil_Heavy2.make(25)), FL.array(FL_GT6U.SFuel.make(30), FL_GT6U.SDiesel.make(20), FL_GT6U.SKerosine.make(20), FL_GT6U.SPetrol.make(15), FL_GT6U.SNaphtha.make(20), FL_GT6U.SGasoil.make(20), FL.Propane.make(10), FL.Butane.make(10), FL.lube(40)), dustTiny.mat(MT.WaxParaffin, 1), dustTiny.mat(MT.Asphalt, 1));
		RM.DistillationTower.addRecipe0(F, 64, 128, new long[]{3000, 3000}, FL.array(FL.Oil_Medium.make(25)), FL.array(FL_GT6U.SFuel.make(25), FL_GT6U.SDiesel.make(15), FL_GT6U.SKerosine.make(15), FL_GT6U.SPetrol.make(15), FL_GT6U.SNaphtha.make(25), FL_GT6U.SGasoil.make(20), FL.Propane.make(15), FL.Butane.make(15), FL.lube(25)), dustTiny.mat(MT.WaxParaffin, 1), dustTiny.mat(MT.Asphalt, 1));
		RM.DistillationTower.addRecipe0(F, 64, 128, new long[]{3000, 3000}, FL.array(FL.Oil_Normal.make(25)), FL.array(FL_GT6U.SFuel.make(25), FL_GT6U.SDiesel.make(15), FL_GT6U.SKerosine.make(15), FL_GT6U.SPetrol.make(15), FL_GT6U.SNaphtha.make(20), FL_GT6U.SGasoil.make(25), FL.Propane.make(15), FL.Butane.make(15), FL.lube(25)), dustTiny.mat(MT.WaxParaffin, 1), dustTiny.mat(MT.Asphalt, 1));
		if(FL.Oil_HotCrude.exists())RM.DistillationTower.addRecipe0(F, 64, 64, new long[]{3000, 3000}, FL.array(FL.Oil_HotCrude.make(25)), FL.array(FL_GT6U.SFuel.make(25), FL_GT6U.SDiesel.make(15), FL_GT6U.SKerosine.make(15), FL_GT6U.SPetrol.make(15), FL_GT6U.SNaphtha.make(15), FL_GT6U.SGasoil.make(20), FL.Propane.make(15), FL.Butane.make(15), FL.lube(25)), dustTiny.mat(MT.WaxParaffin, 1), dustTiny.mat(MT.Asphalt, 1));
		RM.DistillationTower.addRecipe0(F, 64, 64, new long[]{2000, 2000}, FL.array(FL.Oil_Light.make(25)), FL.array(FL_GT6U.SFuel.make(15), FL_GT6U.SDiesel.make(10), FL_GT6U.SKerosine.make(10), FL_GT6U.SPetrol.make(10), FL_GT6U.SNaphtha.make(20), FL_GT6U.SGasoil.make(30), FL.Propane.make(25), FL.Butane.make(25), FL.lube(15)), dustTiny.mat(MT.WaxParaffin, 1), dustTiny.mat(MT.Asphalt, 1));
		if(FL.Oil_Light2.exists())RM.DistillationTower.addRecipe0(F, 64, 64, new long[]{2000, 2000}, FL.array(FL.Oil_Light2.make(25)), FL.array(FL_GT6U.SFuel.make(15), FL_GT6U.SDiesel.make(10), FL_GT6U.SKerosine.make(10), FL_GT6U.SPetrol.make(10), FL_GT6U.SNaphtha.make(20), FL_GT6U.SGasoil.make(30), FL.Propane.make(25), FL.Butane.make(25), FL.lube(15)), dustTiny.mat(MT.WaxParaffin, 1), dustTiny.mat(MT.Asphalt, 1));
		RM.DistillationTower.addRecipe0(F, 64, 64, new long[]{1000, 1000}, FL.array(FL.Oil_Soulsand.make(25)), FL.array(FL_GT6U.SFuel.make(10), FL_GT6U.SDiesel.make(5), FL_GT6U.SKerosine.make(5), FL_GT6U.SPetrol.make(5), FL_GT6U.SNaphtha.make(10), FL_GT6U.SGasoil.make(25), FL.Propane.make(5), FL.Butane.make(5), FL.lube(40)), dustTiny.mat(MT.WaxParaffin, 1), dustTiny.mat(MT.Asphalt, 1));
		RM.Mixer .addRecipe0(F, 16, 16, FL.array(FL_GT6U.SFuel.make(16), FL.Hydrogen.make(2)), FL.array(FL.Fuel.make(12), MT.H2S.gas(U1000, T)), ZL_IS);
		RM.Mixer .addRecipe0(F, 16, 16, FL.array(FL_GT6U.SDiesel.make(16), FL.Hydrogen.make(2)), FL.array(FL.Diesel.make(12), MT.H2S.gas(U1000, T)), ZL_IS);
		RM.Mixer .addRecipe0(F, 16, 16, FL.array(FL_GT6U.SPetrol.make(16), FL.Hydrogen.make(2)), FL.array(FL.Petrol.make(12), MT.H2S.gas(U1000, T)), ZL_IS);
		RM.Mixer .addRecipe0(F, 16, 16, FL.array(FL_GT6U.SKerosine.make(16), FL.Hydrogen.make(2)), FL.array(FL.Kerosine.make(12), MT.H2S.gas(U1000, T)), ZL_IS);
		RM.Mixer .addRecipe0(F, 16, 16, FL.array(FL_GT6U.SNaphtha.make(16), FL.Hydrogen.make(2)), FL.array(FL_GT6U.Naphtha.make(12), MT.H2S.gas(U1000, T)), ZL_IS);
		RM.Mixer .addRecipe0(F, 16, 16, FL.array(FL_GT6U.SGasoil.make(16), FL.Hydrogen.make(2)), FL.array(FL_GT6U.Gasoil.make(12), MT.H2S.gas(U1000, T)), ZL_IS);
		RM_GT6U.OilCleaner .addRecipe0(F, 16, 8, FL.array(FL_GT6U.SFuel.make(16), FL.Hydrogen.make(1)), FL.array(FL.Fuel.make(16), MT.H2S.gas(U1000, T)), ZL_IS);
		RM_GT6U.OilCleaner .addRecipe0(F, 16, 8, FL.array(FL_GT6U.SDiesel.make(16), FL.Hydrogen.make(1)), FL.array(FL.Diesel.make(16), MT.H2S.gas(U1000, T)), ZL_IS);
		RM_GT6U.OilCleaner .addRecipe0(F, 16, 8, FL.array(FL_GT6U.SPetrol.make(16), FL.Hydrogen.make(1)), FL.array(FL.Petrol.make(16), MT.H2S.gas(U1000, T)), ZL_IS);
		RM_GT6U.OilCleaner .addRecipe0(F, 16, 8, FL.array(FL_GT6U.SKerosine.make(16), FL.Hydrogen.make(1)), FL.array(FL.Kerosine.make(16), MT.H2S.gas(U1000, T)), ZL_IS);
		RM_GT6U.OilCleaner .addRecipe0(F, 16, 8, FL.array(FL_GT6U.SNaphtha.make(16), FL.Hydrogen.make(1)), FL.array(FL_GT6U.Naphtha.make(16), MT.H2S.gas(U1000, T)), ZL_IS);
		RM_GT6U.OilCleaner .addRecipe0(F, 16, 8, FL.array(FL_GT6U.SGasoil.make(16), FL.Hydrogen.make(1)), FL.array(FL_GT6U.Gasoil.make(16), MT.H2S.gas(U1000, T)), ZL_IS);
		RM.SteamCracking .addRecipe0(F, 16, 384, FL.array(FL.Steam.make(1000), FL.Propane.make(100)), FL.array(FL.Hydrogen.make( 2), FL.Methane.make(27), FL.Ethylene.make(42), FL.Propylene.make(19)), ZL_IS);
		RM.SteamCracking .addRecipe0(F, 16, 384, FL.array(FL.Steam.make(1000), FL.Butane .make(100)), FL.array(FL.Hydrogen.make( 5), FL.Methane.make( 9), FL.Ethylene.make(78), FL.Propylene.make( 3)), ZL_IS);
		RM.CatalyticCracking.addRecipe1(F, 16, 384, OP.dust.mat(MT.Pt, 0), FL.array(FL.Hydrogen.make(100), FL.Petrol.make(100)), FL.array(FL.Ethylene.make(30), FL.Propylene.make(20)), ZL_IS);
		RM.CatalyticCracking.addRecipe1(F, 16, 384, OP.dust.mat(MT.Pt, 0), FL.array(FL.Hydrogen.make(100), FL.Fuel .make(100)), FL.array(FL.Ethylene.make(40), FL.Propylene.make(10)), ZL_IS);
		RM_GT6U.CrackingTower .addRecipe0(F,128, 108, FL.array(FL.Steam.make(1000), FL.Butane .make(1000)), FL.array(FL.Methane.make(550), FL.Ethylene.make(800), FL.Propylene.make(400)), dustTiny.mat(MT.C, 1));
		RM_GT6U.CrackingTower .addRecipe0(F,128, 108, FL.array(FL.Hydrogen.make(1000), FL.Butane .make(1000)), FL.array(FL.Methane.make(2000), FL.Propane.make(1000)), ZL_IS);
		RM_GT6U.CrackingTower .addRecipe0(F,128, 108, FL.array(FL.Steam.make(1000), FL.Propane .make(1000)), FL.array(FL.Methane.make(1000), FL.Propylene.make(500)), dustTiny.mat(MT.C, 1));
		RM_GT6U.CrackingTower .addRecipe0(F,128, 108, FL.array(FL.Hydrogen.make(1000), FL.Propane .make(1000)), FL.array(FL.Methane.make(1500)), ZL_IS);
		RM_GT6U.CrackingTower .addRecipe0(F,128, 108, FL.array(FL.Steam.make(1000), FL.Petrol .make(1000)), FL.array(FL_GT6U.Naphtha.make(100), FL_GT6U.benzene.make(300), FL.Methane.make(450), FL.Ethylene.make(450), FL.Propylene.make(450), FL.Fuel.make(100)), dustTiny.mat(MT.C, 1));
		RM_GT6U.CrackingTower .addRecipe0(F,128, 108, FL.array(FL.Hydrogen.make(1000), FL.Petrol .make(1000)), FL.array(FL.Methane.make(500), FL.Butane.make(400), FL_GT6U.Naphtha.make(800), FL.Propane.make(400)), ZL_IS);
		RM_GT6U.CrackingTower .addRecipe0(F,128, 108, FL.array(FL.Steam.make(1000), FL_GT6U.Naphtha .make(1000)), FL.array(FL.Fuel.make(100), FL.Petrol.make(100), FL_GT6U.benzene.make(400), FL.Methane.make(600), FL.Ethylene.make(600), FL.Propylene.make(600)), dustTiny.mat(MT.C, 1));
		RM_GT6U.CrackingTower .addRecipe0(F,128, 108, FL.array(FL.Hydrogen.make(1000), FL_GT6U.Naphtha .make(1000)), FL.array(FL.Methane.make(200), FL.Butane.make(400), FL_GT6U.Naphtha.make(800), FL.Propane.make(400)), ZL_IS);
		RM_GT6U.CrackingTower .addRecipe0(F,128, 108, FL.array(FL.Steam.make(1000), FL.Fuel .make(1000)), FL.array(FL.Petrol.make(100), FL_GT6U.benzene.make(200), FL.Methane.make(500), FL.Ethylene.make(300), FL.Propylene.make(400)), dustTiny.mat(MT.C, 1));
		RM_GT6U.CrackingTower .addRecipe0(F,128, 108, FL.array(FL.Hydrogen.make(1000), FL.Fuel .make(1000)), FL.array(FL.Petrol.make(800), FL.Butane.make(100), FL_GT6U.Naphtha.make(400), FL.Propane.make(100), FL.Methane.make(150)), ZL_IS);
		RM_GT6U.CrackingTower .addRecipe0(F,128, 108, FL.array(FL.Steam.make(1000), FL_GT6U.Gasoil .make(1000)), FL.array(FL.Methane.make(800), FL.Ethylene.make(200), FL.Propylene.make(150)), dustTiny.mat(MT.C, 1));
		RM_GT6U.CrackingTower .addRecipe0(F,128, 108, FL.array(FL.Hydrogen.make(1000), FL_GT6U.Gasoil .make(1000)), FL.array(FL.Methane.make(2000)), ZL_IS);
		RM.Centrifuge .addRecipe0(F, 64, 48, FL.array(FL.Gas_Natural.make(320)), FL.array(FL.LPG .make(290)), ZL_IS);
		RM.Mixer .addRecipe1(T, 16, 16, OP.dust.mat(MT.MgCl2, 0), FL.array(MT.TiCl4.liquid(U1000, T), FL.Ethylene .make(144)), ZL_FS, OP.dust.mat(MT.Plastic, 1));
		RM.Mixer .addRecipe1(T, 16, 16, OP.dust.mat(MT.MgCl2, 0), FL.array(MT.TiCl4.liquid(U1000, T), FL.Propylene.make(144)), ZL_FS, OP.dust.mat(MT.Plastic, 1));
		RM.Mixer .addRecipe1(T, 16, 144, ST.tag(0), FL.array(FL.Propylene.make(2000)), FL.array(FL_GT6U.Isoprene.make(1000), FL.Methane.make(1000)), ZL_IS);
		RM.Mixer .addRecipe1(T, 16, 144, ST.tag(1), FL.array(FL.Propylene.make(1000), FL.Ethylene .make(1000)), FL.array(FL_GT6U.Isoprene.make(1000), FL.Methane.make(1000)), ZL_IS);
		RM.Mixer .addRecipe1(T, 16, 144, ST.tag(2), FL.array(FL.Propylene.make(1000), FL.Ethylene .make(1000)), FL.array(FL_GT6U.Isoprene.make(1000), MT.H .gas (2*U , F)), ZL_IS);
		RM.Mixer .addRecipe1(T, 16, 108, ST.tag(2), FL.array(FL_GT6U.Isoprene.make(288), MT.O.gas (2*U, F)), ZL_FS, OP.dust.mat(MT.Rubber, 3));
		RM.HeatMixer .addRecipe1(T, 64, 144, ST.tag(0), FL.array(FL.Ethylene.make(1000), MT.Cl.gas(U*1, T)), FL.array(FL_GT6U.Vinyl_Chloride.make(1000), MT.HCl.liquid(U*1, F)), ZL_IS);
		RM.HeatMixer .addRecipe1(T, 64, 144, OP.dust.mat(MT.MgCl2, 0), FL.array(FL_GT6U.Vinyl_Chloride.make(500), MT.TiCl4.liquid(U1000*5, T)), ZL_FS, OP.dust.mat(MT_GT6U.PVC, 1));
		RM.Roasting .addRecipe1(T, 256, 288, OP.dust.mat(MT.K2SO4, 1), FL.array(MT.SO3.gas(U*1, T)), ZL_FS, OP.dust.mat(MT.K2S2O7, 1));
		RM.HeatMixer .addRecipe1(T, 256, 144, ST.tag(0), FL.array(FL.Methane.make(1000), MT.Cl.gas(U*3, T)), FL.array(FL_GT6U.Trichloromethane.make(1000), MT.HCl.liquid(U*3, F)), ZL_IS);
		RM.HeatMixer .addRecipe1(T, 256, 144, ST.tag(0), FL.array(FL_GT6U.Trichloromethane.make(1000), MT.HF.gas(U*2, T)), FL.array(FL_GT6U.Monochlorodifluoromethane.make(1000), MT.HCl.liquid(U*2, F)), ZL_IS);
		RM.HeatMixer .addRecipe1(T, 256, 288, OP.dust.mat(MT.Pt, 0), FL.array(FL_GT6U.Monochlorodifluoromethane.make(2000)), FL.array(FL_GT6U.Tetrafluoroethylene.make(1000), MT.HCl.liquid(U*2, F)), ZL_IS);
		RM.HeatMixer .addRecipe1(T, 256, 288, OP.dust.mat(MT.K2S2O7, 0), FL.array(FL_GT6U.Tetrafluoroethylene.make(144), FL.DistW.make(144)), ZL_FS, OP.dust.mat(MT_GT6U.PTFE, 1));
		RM.HeatMixer .addRecipeX(T, 16, 500, ST.array(OP.dust.mat(MT.Al2O3, 5), OP.dust.mat(MT.C, 3)), FL.array(MT.Cl.fluid(U*6, T)), MT.CO.fluid(U*6, T), OP.dust.mat(MT_GT6U.AlCl3, 8));
		RM.HeatMixer .addRecipe1(T, 16, 500, OP.dust.mat(MT.PO4, 1), FL.array(MT.HCl.fluid(U*3, T)), FL.array(MT_GT6U.H3PO4.liquid(U*1, F), MT.Cl.gas(U*1, F)), ZL_IS);
		RM.HeatMixer .addRecipe1(T, 128, 400, OP.dust.mat(MT.MgCl2, 0), FL.array(MT.Glycerol.liquid(U*1, T), MT.HCl.fluid(U*3, T)), FL.array(FL_GT6U.Dichloroisopropanol.make(1000), MT.Cl.gas(U*1, T)), ZL_IS);
		RM.HeatMixer .addRecipe1(T, 128, 400, OP.dust.mat(MT.KOH, 0), FL.array(FL_GT6U.Dichloroisopropanol.make(1000)), FL.array(FL_GT6U.epichlorohydrin.make(500)), ZL_IS);
		RM.HeatMixer .addRecipe1(T, 128, 400, OP.dust.mat(MT_GT6U.AlCl3, 0), FL.array(FL_GT6U.benzene.make(1000), FL.Propylene.make(1000)), FL.array(FL_GT6U.Cumene.make(1000)), ZL_IS);
		RM.HeatMixer .addRecipe0(T, 128, 400, FL.array(FL_GT6U.Cumene.make(1000), MT.H2SO4.liquid(U*1, T), MT.O.gas(U*1, T)), FL.array(FL_GT6U.phenol.make(500), FL_GT6U.acetone.make(500)), ZL_IS);
		RM.HeatMixer .addRecipe0(T, 128, 400, FL.array(FL_GT6U.phenol.make(1000), FL_GT6U.acetone.make(1000)), FL.array(FL_GT6U.bisphenol.make(2000)), ZL_IS);
		RM.HeatMixer .addRecipe1(T, 128, 400, OP.dust.mat(MT.NaOH, 4), FL.array(FL_GT6U.epichlorohydrin.make(1000), FL_GT6U.bisphenol.make(1000), MT_GT6U.H3PO4.liquid(U100*5, T)), ZL_FS, OP.dust.mat(MT_GT6U.Epoxid, 3));
		RM.HeatMixer .addRecipe0(T, 128, 200, FL.array(FL.Propylene.make(1000), MT.Cl.gas(U*1, T)), FL.array(FL_GT6U.Allyl_Chloride.make(1000), MT.HCl.liquid(U*1, F)), ZL_IS);
		RM.HeatMixer .addRecipeX(T, 128, 100, ST.array(ST.tag(0), OP.dust.mat(MT.Ag , 0), OP.dust.mat(MT.NaOH, 1)), FL.array(FL_GT6U.Allyl_Chloride.make(1000), MT.H2O.liquid(U*1, T), MT.O.gas(U*1, T)), FL.array(FL_GT6U.Acrylic_Acid.make(1000)), OP.dust.mat(MT.NaCl, 1));
		RM.HeatMixer .addRecipe0(T, 128, 400, FL.array(FL_GT6U.Acrylic_Acid.make(1000), FL.BioEthanol.make(1000), MT.H2SO4.liquid(U*1, T)), FL.array(FL_GT6U.Ethyl_Acrylate.make(1000), MT.H2SO4.liquid(U10*8, F)), ZL_IS);
		RM.HeatMixer .addRecipe1(T, 128, 200, OP.dust.mat(MT.Pt, 0), FL.array(FL.Ethylene.make(1000), FL_GT6U.benzene.make(1000)), FL.array(FL_GT6U.Styrene.make(1000)), ZL_IS);
		RM.HeatMixer .addRecipe1(T, 128, 400, OP.dust.mat(MT.V2O5, 0), FL.array(FL_GT6U.Ethyl_Acrylate.make(1000), FL_GT6U.Styrene.make(1000), MT.TiCl4.liquid(U100, T)), FL.array(FL_GT6U.Photoresist.make(2000)), ZL_IS);
		RM.HeatMixer .addRecipe2(T, 16, 288, OM.dust(MT.OREMATS.Chromite, U*28), OP.dust.mat(MT.Na2CO3, 48), FL.array(MT.O.gas(U*14, T)), FL.array(MT.CO2.gas(U*24, F)), OP.dust.mat(MT_GT6U.Na2CrO4, 56), OM.dust(MT.OREMATS.Hematite, U*10));
		RM.HeatMixer .addRecipe1(T, 16, 288, OP.dust.mat(MT_GT6U.Na2CrO4, 14), FL.array(MT.H2SO4.liquid(U*7, T)), FL.array(MT.H2O.liquid(U*3, F)), OP.dust.mat(MT_GT6U.Na2Cr2O7, 11), OP.dust.mat(MT.Na2SO4, 7));
		RM.HeatMixer .addRecipe2(T, 16, 288, OP.dust.mat(MT_GT6U.Na2Cr2O7, 11), OP.dust.mat(MT.C, 2), ZL_FS, FL.array(MT.CO.gas(U*2, F)), OP.dust.mat(MT_GT6U.Cr2O3, 5), OP.dust.mat(MT.Na2CO3, 6));
		RM.BurnMixer .addRecipe2(T, 16, 144, OP.dust.mat(MT_GT6U.Cr2O3, 5), OP.dust.mat(MT.Al, 2), ZL_FS, FL.array(MT.Cr.liquid(U*2, F)), OP.dust.mat(MT.Al2O3, 5));
		RM.HeatMixer .addRecipe1(T, 64, 400, OP.dust.mat(MT.CaF2, 15), FL.array(MT_GT6U.H3PO4.fluid(U*16, F)), FL.array(MT_GT6U.PF5.fluid(U*12, F), MT.H2O.fluid(U*9, F)), OP.dust.mat(MT_GT6U.CaO, 10));
		RM.HeatMixer .addRecipe1(T, 64, 300, OP.dust.mat(MT.PO4, 20), FL.array(MT.HF.fluid(U*40, F)), FL.array(MT_GT6U.PF5.fluid(U*24, F), MT.H2O.fluid(U*30, F), MT.O.fluid(U*6, F)), NI);
		RM.Bath .addRecipe1(T, 0, 200, OP.dust.mat(MT.LiOH, 3), FL.array(MT.HF.fluid(U*2, F)), FL.array(MT.H2O.fluid(U*3, F)), OP.dust.mat(MT_GT6U.LiF, 2));
		RM.Drying .addRecipe1(T, 16, 4000, OP.dust.mat(MT_GT6U.LiF, 2), FL.array(MT_GT6U.PF5.fluid(U*6, F)), ZL_FS, OP.dust.mat(MT_GT6U.LiPF6, 8));
		RM.Lightning .addRecipe0(T, 256, 300, FL.array(MT.CO.fluid(U*2, T), MT.Cl.fluid(U*2, T)), FL.array(MT_GT6U.COCl2.gas(U*4, F)), ZL_IS);
		RM.Mixer .addRecipe0(T, 16, 400, FL.array(MT_GT6U.COCl2.gas(U*4, T), FL.BioEthanol.make(2000)), FL.array(MT_GT6U.C5H10O3.liquid(U*18, F), MT.HCl.fluid(U*4, F)), ZL_IS);
		RM.Mixer .addRecipe0(T, 16, 400, FL.array(MT_GT6U.C5H10O3.liquid(U*72, T), FL_GT6U.Methanol.make(6000)), FL.array(FL_GT6U.MixedEster.make(78000)), ZL_IS);
		RM.DistillationTower.addRecipe0(T, 64, 1000, FL.array(FL_GT6U.MixedEster.make(78000)), FL.array(MT_GT6U.C3H6O3.liquid(U*12, F), MT_GT6U.C5H10O3.liquid(U*18, F), MT_GT6U.C4H8O3.liquid(U*30, F), FL_GT6U.Methanol.make(2000), FL.BioEthanol.make(4000)), ZL_IS);
		GT6U_Recipes.first(Loader_Recipes_Chem::later_run_0); RM.Roasting .addRecipe1(T, 16, 512, OM.dust(MT.Pyrite ), MT. O .gas( 4*U3 , T), MT.SO2.gas( 6*U3 , F), OM.dust(MT.Fe, U3 )); GT6U_Recipes.first(null);
		GT6U_Recipes.first(Loader_Recipes_Chem::later_run_1); RM.Roasting .addRecipe1(T, 16, 512, OM.dust(MT.OREMATS.Sphalerite ), MT. O .gas( 2*U2 , T), MT.SO2.gas( 3*U2 , F), OM.dust(MT.Zn, U2 )); GT6U_Recipes.first(null);
		GT6U_Recipes.first(Loader_Recipes_Chem::later_run_2); RM.Roasting .addRecipe1(T, 16, 512, OM.dust(MT.OREMATS.Molybdenite ), MT. O .gas( 4*U3 , T), MT.SO2.gas( 6*U3 , F), OM.dust(MT.Mo, U3 )); GT6U_Recipes.first(null);
		GT6U_Recipes.first(Loader_Recipes_Chem::later_run_3); RM.Roasting .addRecipe1(T, 16, 512, OM.dust(MT.OREMATS.Stibnite ), MT. O .gas( 6*U5 , T), MT.SO2.gas( 9*U5 , F), OM.dust(MT.Sb, 2*U5 )); GT6U_Recipes.first(null);
		GT6U_Recipes.first(Loader_Recipes_Chem::later_run_4); RM.Roasting .addRecipe1(T, 16, 512, OM.dust(MT.OREMATS.Pentlandite ), MT. O .gas(16*U17, T), MT.SO2.gas(24*U17, F), OM.dust(MT.Ni, 9*U17)); GT6U_Recipes.first(null);
		GT6U_Recipes.first(Loader_Recipes_Chem::later_run_5); RM.Roasting .addRecipe1(T, 16, 512, OM.dust(MT.OREMATS.Chalcopyrite ), MT. O .gas( 4*U4 , T), MT.SO2.gas( 6*U4 , F), OM.dust(MT.Fe, U4 ), OM.dust(MT.Cu, U4)); GT6U_Recipes.first(null);
		GT6U_Recipes.first(Loader_Recipes_Chem::later_run_6); RM.Roasting .addRecipe1(T, 16, 512, OM.dust(MT.OREMATS.Arsenopyrite ), MT. O .gas( 2*U3 , T), MT.SO2.gas( 3*U3 , F), OM.dust(MT.Fe, U3 ), OM.dust(MT.As, U3)); GT6U_Recipes.first(null);
		GT6U_Recipes.first(Loader_Recipes_Chem::later_run_7); RM.Roasting .addRecipe1(T, 16, 512, OM.dust(MT.OREMATS.Cobaltite ), MT. O .gas( 2*U3 , T), MT.SO2.gas( 3*U3 , F), OM.dust(MT.Co, U3 ), OM.dust(MT.As, U3)); GT6U_Recipes.first(null);
		GT6U_Recipes.first(Loader_Recipes_Chem::later_run_8); RM.Roasting .addRecipe1(T, 16, 512, OM.dust(MT.OREMATS.Galena ), MT. O .gas( 4*U8 , T), MT.SO2.gas( 6*U8 , F), OM.dust(MT.Ag, 3*U8 ), OM.dust(MT.Pb, 3*U8)); GT6U_Recipes.first(null);
		GT6U_Recipes.first(Loader_Recipes_Chem::later_run_9); RM.Roasting .addRecipe1(T, 16, 512, OM.dust(MT.OREMATS.Cooperite ), MT. O .gas( 2*U6 , T), MT.SO2.gas( 3*U6 , F), OM.dust(MT.Pt, 3*U6 ), OM.dust(MT.Ni, 1*U6), OM.dust(MT.Pd, 1*U6)); GT6U_Recipes.first(null);
		GT6U_Recipes.first(Loader_Recipes_Chem::later_run_10); RM.Roasting .addRecipe1(T, 16, 512, OM.dust(MT.OREMATS.Tetrahedrite ), MT. O .gas( 6*U8 , T), MT.SO2.gas( 9*U8 , F), OM.dust(MT.Cu, 3*U8 ), OM.dust(MT.Sb, 1*U8), OM.dust(MT.Fe, 1*U8)); GT6U_Recipes.first(null);
		GT6U_Recipes.first(Loader_Recipes_Chem::later_run_11); RM.Roasting .addRecipe1(T, 16, 512, OM.dust(MT.OREMATS.Stannite ), MT. O .gas( 8*U8 , T), MT.SO2.gas(12*U8 , F), OM.dust(MT.Cu, 2*U8 ), OM.dust(MT.Fe, 1*U8), OM.dust(MT.Sn, 1*U8)); GT6U_Recipes.first(null);
		GT6U_Recipes.first(Loader_Recipes_Chem::later_run_12); RM.Roasting .addRecipe1(T, 16, 512, OM.dust(MT.OREMATS.Kesterite ), MT. O .gas( 8*U8 , T), MT.SO2.gas(12*U8 , F), OM.dust(MT.Cu, 2*U8 ), OM.dust(MT.Zn, 1*U8), OM.dust(MT.Sn, 1*U8)); GT6U_Recipes.first(null);
		GT6U_Recipes.first(Loader_Recipes_Chem::later_run_13); RM.Roasting .addRecipe1(T, 16, 128, OM.dust(MT.Li ), MT. O .gas( U2 , T), NF , OM.dust(MT.Li2O, 3*U2)); GT6U_Recipes.first(null);
		GT6U_Recipes.first(Loader_Recipes_Chem::later_run_14); RM.Roasting .addRecipe1(T, 16, 128, OM.dust(MT.V ), MT. O .gas( 5*U2 , T), NF , OM.dust(MT.V2O5, 7*U2)); GT6U_Recipes.first(null);
		GT6U_Recipes.first(Loader_Recipes_Chem::later_run_15); RM.Roasting .addRecipe1(T, 16, 128, OM.dust(MT.Cr ), MT. O .gas( 2*U , T), NF , OM.dust(MT.CrO2, U )); GT6U_Recipes.first(null);
		GT6U_Recipes.first(Loader_Recipes_Chem::later_run_16); RM.Roasting .addRecipe1(T, 16, 16, OM.dust(MT.S ), MT. O .gas( 2*U , T), MT.SO2.gas( 3*U , F), ZL_IS); GT6U_Recipes.first(null);
		GT6U_Recipes.first(Loader_Recipes_Chem::later_run_17); for (OreDictMaterial tMat : ANY.C.mToThis) if (tMat != MT.Graphene) RM.Roasting .addRecipe1(T, 16, 16, OM.dust(tMat ), MT. O .gas( 2*U , T), MT.CO2.gas( 3*U , F), ZL_IS); GT6U_Recipes.first(null);
		GT6U_Recipes.first(Loader_Recipes_Chem::later_run_18); RM.Roasting .addRecipe1(T, 16, 16, OM.dust(MT.Charcoal ), MT. O .gas( 2*U , T), MT.CO2.gas( 3*U , F), ZL_IS); GT6U_Recipes.first(null);
		GT6U_Recipes.first(Loader_Recipes_Chem::later_run_19); RM.Roasting .addRecipe1(T, 16, 16, OM.dust(MT.Coal ), MT. O .gas( 4*U , T), MT.CO2.gas( 6*U , F), ZL_IS); GT6U_Recipes.first(null);
		GT6U_Recipes.first(Loader_Recipes_Chem::later_run_20); RM.Roasting .addRecipe1(T, 16, 16, OM.dust(MT.CoalCoke ), MT. O .gas( 4*U , T), MT.CO2.gas( 6*U , F), ZL_IS); GT6U_Recipes.first(null);
		GT6U_Recipes.first(Loader_Recipes_Chem::later_run_21); for (OreDictMaterial tMat : ANY.Diamond.mToThis) RM.Roasting .addRecipe1(T, 16, 16, OM.dust(tMat ), MT. O .gas( 8*U , T), MT.CO2.gas(12*U , F), ZL_IS); GT6U_Recipes.first(null);
	}

	/** GT6 statements GT6U put a statement of run_run ahead of: what they make gives way to it. */
	static void later_run_0() {
		for (String tOxygen : FluidsGT.OXYGEN) if (FL.exists(tOxygen)) {
			RM.Roasting .addRecipe1(T, 16, 512, OM.dust(MT.Pyrite ), FL.make(tOxygen, 1834), MT.SO2.gas( 6*U3 , F), OM.dust(MT.Fe2O3, 5*U6 ));
		}
		final long[] tChances = new long[] {8000, 8000, 8000};
		for (String tAir : FluidsGT.AIR) if (FL.exists(tAir)) {
			RM.Roasting .addRecipe1(T, 16, 512, tChances, OM.dust(MT.Pyrite ), FL.make(tAir, 8000), MT.SO2.gas( 6*U3 , F), OM.dust(MT.Fe2O3, 5*U6 ));
		}
	}

	/** GT6 statements GT6U put a statement of run_run ahead of: what they make gives way to it. */
	static void later_run_1() {
		for (String tOxygen : FluidsGT.OXYGEN) if (FL.exists(tOxygen)) {
			RM.Roasting .addRecipe1(T, 16, 512, OM.dust(MT.OREMATS.Sphalerite ), FL.make(tOxygen, 1000), MT.SO2.gas( 3*U2 , F), OM.dust(MT.Zn , U2 ));
		}
		final long[] tChances = new long[] {8000, 8000, 8000};
		for (String tAir : FluidsGT.AIR) if (FL.exists(tAir)) {
			RM.Roasting .addRecipe1(T, 16, 512, tChances, OM.dust(MT.OREMATS.Sphalerite ), FL.make(tAir, 4000), MT.SO2.gas( 3*U2 , F), OM.dust(MT.Zn , U2 ));
		}
	}

	/** GT6 statements GT6U put a statement of run_run ahead of: what they make gives way to it. */
	static void later_run_2() {
		for (String tOxygen : FluidsGT.OXYGEN) if (FL.exists(tOxygen)) {
			RM.Roasting .addRecipe1(T, 16, 512, OM.dust(MT.OREMATS.Molybdenite ), FL.make(tOxygen, 1334), MT.SO2.gas( 6*U3 , F), OM.dust(MT.Mo , U3 ));
		}
		final long[] tChances = new long[] {8000, 8000, 8000};
		for (String tAir : FluidsGT.AIR) if (FL.exists(tAir)) {
			RM.Roasting .addRecipe1(T, 16, 512, tChances, OM.dust(MT.OREMATS.Molybdenite ), FL.make(tAir, 6000), MT.SO2.gas( 6*U3 , F), OM.dust(MT.Mo , U3 ));
		}
	}

	/** GT6 statements GT6U put a statement of run_run ahead of: what they make gives way to it. */
	static void later_run_3() {
		for (String tOxygen : FluidsGT.OXYGEN) if (FL.exists(tOxygen)) {
			RM.Roasting .addRecipe1(T, 16, 512, OM.dust(MT.OREMATS.Stibnite ), FL.make(tOxygen, 1200), MT.SO2.gas( 9*U5 , F), OM.dust(MT.Sb , 2*U5 ));
		}
		final long[] tChances = new long[] {8000, 8000, 8000};
		for (String tAir : FluidsGT.AIR) if (FL.exists(tAir)) {
			RM.Roasting .addRecipe1(T, 16, 512, tChances, OM.dust(MT.OREMATS.Stibnite ), FL.make(tAir, 5000), MT.SO2.gas( 9*U5 , F), OM.dust(MT.Sb , 2*U5 ));
		}
	}

	/** GT6 statements GT6U put a statement of run_run ahead of: what they make gives way to it. */
	static void later_run_4() {
		for (String tOxygen : FluidsGT.OXYGEN) if (FL.exists(tOxygen)) {
			RM.Roasting .addRecipe1(T, 16, 512, OM.dust(MT.OREMATS.Pentlandite ), FL.make(tOxygen, 942), MT.SO2.gas(24*U17, F), OM.dust(MT.Ni , 9*U17));
		}
		final long[] tChances = new long[] {8000, 8000, 8000};
		for (String tAir : FluidsGT.AIR) if (FL.exists(tAir)) {
			RM.Roasting .addRecipe1(T, 16, 512, tChances, OM.dust(MT.OREMATS.Pentlandite ), FL.make(tAir, 4000), MT.SO2.gas(24*U17, F), OM.dust(MT.Ni , 9*U17));
		}
	}

	/** GT6 statements GT6U put a statement of run_run ahead of: what they make gives way to it. */
	static void later_run_5() {
		for (String tOxygen : FluidsGT.OXYGEN) if (FL.exists(tOxygen)) {
			RM.Roasting .addRecipe1(T, 16, 512, OM.dust(MT.OREMATS.Chalcopyrite ), FL.make(tOxygen, 1375), MT.SO2.gas( 6*U4 , F), OM.dust(MT.Fe2O3, 5*U8 ), OM.dust(MT.Cu, U4));
		}
		final long[] tChances = new long[] {8000, 8000, 8000};
		for (String tAir : FluidsGT.AIR) if (FL.exists(tAir)) {
			RM.Roasting .addRecipe1(T, 16, 512, tChances, OM.dust(MT.OREMATS.Chalcopyrite), FL.make(tAir, 6000), MT.SO2.gas( 6*U4 , F), OM.dust(MT.Fe2O3, 5*U8 ), OM.dust(MT.Cu, U4));
		}
	}

	/** GT6 statements GT6U put a statement of run_run ahead of: what they make gives way to it. */
	static void later_run_6() {
		for (String tOxygen : FluidsGT.OXYGEN) if (FL.exists(tOxygen)) {
			RM.Roasting .addRecipe1(T, 16, 512, OM.dust(MT.OREMATS.Arsenopyrite ), FL.make(tOxygen, 1167), MT.SO2.gas( 3*U3 , F), OM.dust(MT.Fe2O3, 5*U6 ), OM.dust(MT.As, U3));
		}
		final long[] tChances = new long[] {8000, 8000, 8000};
		for (String tAir : FluidsGT.AIR) if (FL.exists(tAir)) {
			RM.Roasting .addRecipe1(T, 16, 512, tChances, OM.dust(MT.OREMATS.Arsenopyrite), FL.make(tAir, 5000), MT.SO2.gas( 3*U3 , F), OM.dust(MT.Fe2O3, 5*U6 ), OM.dust(MT.As, U3));
		}
	}

	/** GT6 statements GT6U put a statement of run_run ahead of: what they make gives way to it. */
	static void later_run_7() {
		for (String tOxygen : FluidsGT.OXYGEN) if (FL.exists(tOxygen)) {
			RM.Roasting .addRecipe1(T, 16, 512, OM.dust(MT.OREMATS.Cobaltite ), FL.make(tOxygen, 667), MT.SO2.gas( 3*U3 , F), OM.dust(MT.Co , U3 ), OM.dust(MT.As, U3));
		}
		final long[] tChances = new long[] {8000, 8000, 8000};
		for (String tAir : FluidsGT.AIR) if (FL.exists(tAir)) {
			RM.Roasting .addRecipe1(T, 16, 512, tChances, OM.dust(MT.OREMATS.Cobaltite ), FL.make(tAir, 3000), MT.SO2.gas( 3*U3 , F), OM.dust(MT.Co , U3 ), OM.dust(MT.As, U3));
		}
	}

	/** GT6 statements GT6U put a statement of run_run ahead of: what they make gives way to it. */
	static void later_run_8() {
		for (String tOxygen : FluidsGT.OXYGEN) if (FL.exists(tOxygen)) {
			RM.Roasting .addRecipe1(T, 16, 512, OM.dust(MT.OREMATS.Galena ), FL.make(tOxygen, 500), MT.SO2.gas( 6*U8 , F), OM.dust(MT.Ag , 3*U8 ), OM.dust(MT.Pb, 3*U8));
		}
		final long[] tChances = new long[] {8000, 8000, 8000};
		for (String tAir : FluidsGT.AIR) if (FL.exists(tAir)) {
			RM.Roasting .addRecipe1(T, 16, 512, tChances, OM.dust(MT.OREMATS.Galena ), FL.make(tAir, 2000), MT.SO2.gas( 6*U8 , F), OM.dust(MT.Ag , 3*U8 ), OM.dust(MT.Pb, 3*U8));
		}
	}

	/** GT6 statements GT6U put a statement of run_run ahead of: what they make gives way to it. */
	static void later_run_9() {
		for (String tOxygen : FluidsGT.OXYGEN) if (FL.exists(tOxygen)) {
			RM.Roasting .addRecipe1(T, 16, 512, OM.dust(MT.OREMATS.Cooperite ), FL.make(tOxygen, 334), MT.SO2.gas( 3*U6 , F), OM.dust(MT.Pt , 3*U6 ), OM.dust(MT.Ni, 1*U6), OM.dust(MT.Pd , 1*U6));
		}
		final long[] tChances = new long[] {8000, 8000, 8000};
		for (String tAir : FluidsGT.AIR) if (FL.exists(tAir)) {
			RM.Roasting .addRecipe1(T, 16, 512, tChances, OM.dust(MT.OREMATS.Cooperite ), FL.make(tAir, 2000), MT.SO2.gas( 3*U6 , F), OM.dust(MT.Pt , 3*U6 ), OM.dust(MT.Ni, 1*U6), OM.dust(MT.Pd , 1*U6));
		}
	}

	/** GT6 statements GT6U put a statement of run_run ahead of: what they make gives way to it. */
	static void later_run_10() {
		for (String tOxygen : FluidsGT.OXYGEN) if (FL.exists(tOxygen)) {
			RM.Roasting .addRecipe1(T, 16, 512, OM.dust(MT.OREMATS.Tetrahedrite ), FL.make(tOxygen, 938), MT.SO2.gas( 9*U8 , F), OM.dust(MT.Cu , 3*U8 ), OM.dust(MT.Sb, 1*U8), OM.dust(MT.Fe2O3, 5*U16));
		}
		final long[] tChances = new long[] {8000, 8000, 8000};
		for (String tAir : FluidsGT.AIR) if (FL.exists(tAir)) {
			RM.Roasting .addRecipe1(T, 16, 512, tChances, OM.dust(MT.OREMATS.Tetrahedrite), FL.make(tAir, 4000), MT.SO2.gas( 9*U8 , F), OM.dust(MT.Cu , 3*U8 ), OM.dust(MT.Sb, 1*U8), OM.dust(MT.Fe2O3, 5*U16));
		}
	}

	/** GT6 statements GT6U put a statement of run_run ahead of: what they make gives way to it. */
	static void later_run_11() {
		for (String tOxygen : FluidsGT.OXYGEN) if (FL.exists(tOxygen)) {
			RM.Roasting .addRecipe1(T, 16, 512, OM.dust(MT.OREMATS.Stannite ), FL.make(tOxygen, 1188), MT.SO2.gas(12*U8 , F), OM.dust(MT.Cu , 2*U8 ), OM.dust(MT.Sn, 1*U8), OM.dust(MT.Fe2O3, 5*U16));
		}
		final long[] tChances = new long[] {8000, 8000, 8000};
		for (String tAir : FluidsGT.AIR) if (FL.exists(tAir)) {
			RM.Roasting .addRecipe1(T, 16, 512, tChances, OM.dust(MT.OREMATS.Stannite ), FL.make(tAir, 5000), MT.SO2.gas(12*U8 , F), OM.dust(MT.Cu , 2*U8 ), OM.dust(MT.Sn, 1*U8), OM.dust(MT.Fe2O3, 5*U16));
		}
	}

	/** GT6 statements GT6U put a statement of run_run ahead of: what they make gives way to it. */
	static void later_run_12() {
		for (String tOxygen : FluidsGT.OXYGEN) if (FL.exists(tOxygen)) {
			RM.Roasting .addRecipe1(T, 16, 512, OM.dust(MT.OREMATS.Kesterite ), FL.make(tOxygen, 1000), MT.SO2.gas(12*U8 , F), OM.dust(MT.Cu , 2*U8 ), OM.dust(MT.Zn, 1*U8), OM.dust(MT.Sn , 1*U8));
		}
		final long[] tChances = new long[] {8000, 8000, 8000};
		for (String tAir : FluidsGT.AIR) if (FL.exists(tAir)) {
			RM.Roasting .addRecipe1(T, 16, 512, tChances, OM.dust(MT.OREMATS.Kesterite ), FL.make(tAir, 4000), MT.SO2.gas(12*U8 , F), OM.dust(MT.Cu , 2*U8 ), OM.dust(MT.Zn, 1*U8), OM.dust(MT.Sn , 1*U8));
		}
	}

	/** GT6 statements GT6U put a statement of run_run ahead of: what they make gives way to it. */
	static void later_run_13() {
		for (String tOxygen : FluidsGT.OXYGEN) if (FL.exists(tOxygen)) {
			RM.Roasting .addRecipe1(T, 16, 128, OM.dust(MT.Li ), FL.make(tOxygen, 500), NF , OM.dust(MT.Li2O, 3*U2));
		}
	}

	/** GT6 statements GT6U put a statement of run_run ahead of: what they make gives way to it. */
	static void later_run_14() {
		for (String tOxygen : FluidsGT.OXYGEN) if (FL.exists(tOxygen)) {
			RM.Roasting .addRecipe1(T, 16, 128, OM.dust(MT.V ), FL.make(tOxygen, 2500), NF , OM.dust(MT.V2O5, 7*U2));
		}
	}

	/** GT6 statements GT6U put a statement of run_run ahead of: what they make gives way to it. */
	static void later_run_15() {
		for (String tOxygen : FluidsGT.OXYGEN) if (FL.exists(tOxygen)) {
			RM.Roasting .addRecipe1(T, 16, 128, OM.dust(MT.Cr ), FL.make(tOxygen, 2000), NF , OM.dust(MT.CrO2, U ));
		}
	}

	/** GT6 statements GT6U put a statement of run_run ahead of: what they make gives way to it. */
	static void later_run_16() {
		for (String tOxygen : FluidsGT.OXYGEN) if (FL.exists(tOxygen)) {
			RM.Roasting .addRecipe1(T, 16, 16, OP.dust.mat(MT.S , 1), FL.make(tOxygen, 2000), MT.SO2.gas( 3*U , F), ZL_IS);
			RM.Roasting.addRecipe1(T, 16, 144, OP.blockDust.mat(MT.S, 1), FL.make(tOxygen, 18000), MT.SO2.gas(27*U, F), ZL_IS);
		}
		for (String tAir : FluidsGT.AIR) if (FL.exists(tAir)) {
			RM.Roasting .addRecipe1(T, 16, 16, OP.dust.mat(MT.S , 1), FL.make(tAir, 8000), MT.SO2.gas( 3*U, F), ZL_IS);
			RM.Roasting.addRecipe1(T, 16, 144, OP.blockDust.mat(MT.S, 1), FL.make(tAir, 72000), MT.SO2.gas(27*U, F), ZL_IS);
		}
	}

	/** GT6 statements GT6U put a statement of run_run ahead of: what they make gives way to it. */
	static void later_run_17() {
		for (OreDictMaterial tMat : ANY.C.mToThis) if (tMat != MT.Graphene) RM.Roasting .addRecipe1(T, 16, 16, OM.dust(tMat ), MT.CO2.gas( 3*U , T), MT.CO .gas( 4*U , F), ZL_IS);
		for (OreDictMaterial tMat : ANY.Diamond.mToThis) RM.Roasting .addRecipe1(T, 16, 16, OM.dust(tMat ), MT.CO2.gas(12*U , T), MT.CO .gas(16*U , F), ZL_IS);
		for (String tOxygen : FluidsGT.OXYGEN) if (FL.exists(tOxygen)) {
			for (OreDictMaterial tMat : ANY.C.mToThis) if (tMat != MT.Graphene) RM.Roasting .addRecipe1(T, 16, 16, OM.dust(tMat ), FL.make(tOxygen, 2000), MT.CO2.gas( 3*U , F), ZL_IS);
			for (OreDictMaterial tMat : ANY.Diamond.mToThis) RM.Roasting .addRecipe1(T, 16, 16, OM.dust(tMat ), FL.make(tOxygen, 8000), MT.CO2.gas(12*U , F), ZL_IS);
		}
	}

	/** GT6 statements GT6U put a statement of run_run ahead of: what they make gives way to it. */
	static void later_run_18() {
		RM.Roasting .addRecipe1(T, 16, 16, OM.dust(MT.Charcoal ), MT.CO2.gas( 3*U , T), MT.CO .gas( 4*U , F), ZL_IS);
		for (String tOxygen : FluidsGT.OXYGEN) if (FL.exists(tOxygen)) {
			RM.Roasting .addRecipe1(T, 16, 16, OM.dust(MT.Charcoal ), FL.make(tOxygen, 2000), MT.CO2.gas( 3*U , F), ZL_IS);
		}
	}

	/** GT6 statements GT6U put a statement of run_run ahead of: what they make gives way to it. */
	static void later_run_19() {
		RM.Roasting .addRecipe1(T, 16, 16, OM.dust(MT.Coal ), MT.CO2.gas( 6*U , T), MT.CO .gas( 8*U , F), ZL_IS);
		for (String tOxygen : FluidsGT.OXYGEN) if (FL.exists(tOxygen)) {
			RM.Roasting .addRecipe1(T, 16, 16, OM.dust(MT.Coal ), FL.make(tOxygen, 4000), MT.CO2.gas( 6*U , F), ZL_IS);
		}
	}

	/** GT6 statements GT6U put a statement of run_run ahead of: what they make gives way to it. */
	static void later_run_20() {
		RM.Roasting .addRecipe1(T, 16, 16, OM.dust(MT.CoalCoke ), MT.CO2.gas( 6*U , T), MT.CO .gas( 8*U , F), ZL_IS);
		for (String tOxygen : FluidsGT.OXYGEN) if (FL.exists(tOxygen)) {
			RM.Roasting .addRecipe1(T, 16, 16, OM.dust(MT.CoalCoke ), FL.make(tOxygen, 4000), MT.CO2.gas( 6*U , F), ZL_IS);
		}
	}

	/** GT6 statements GT6U put a statement of run_run ahead of: what they make gives way to it. */
	static void later_run_21() {
		for (OreDictMaterial tMat : ANY.C.mToThis) if (tMat != MT.Graphene) RM.Roasting .addRecipe1(T, 16, 16, OM.dust(tMat ), MT.CO2.gas( 3*U , T), MT.CO .gas( 4*U , F), ZL_IS);
		for (OreDictMaterial tMat : ANY.Diamond.mToThis) RM.Roasting .addRecipe1(T, 16, 16, OM.dust(tMat ), MT.CO2.gas(12*U , T), MT.CO .gas(16*U , F), ZL_IS);
		for (String tOxygen : FluidsGT.OXYGEN) if (FL.exists(tOxygen)) {
			for (OreDictMaterial tMat : ANY.C.mToThis) if (tMat != MT.Graphene) RM.Roasting .addRecipe1(T, 16, 16, OM.dust(tMat ), FL.make(tOxygen, 2000), MT.CO2.gas( 3*U , F), ZL_IS);
			for (OreDictMaterial tMat : ANY.Diamond.mToThis) RM.Roasting .addRecipe1(T, 16, 16, OM.dust(tMat ), FL.make(tOxygen, 8000), MT.CO2.gas(12*U , F), ZL_IS);
		}
	}

	/** GT6's statements in Loader_Recipes_Chem.run that GT6U dropped, as the port has them: their Recipes are taken back. */
	static void takeBack_run() {
		GT6U_Recipes.takeBack("Loader_Recipes_Chem.run", () -> {
			RM.DistillationTower.addRecipe0(F, 64, 16, new long[] { 500, 500, 500}, FL.array(FL.Biomass .make( 80)), FL.array(FL.Reikanol.make(20, FL.BioEthanol), MT.Glycerol.liquid(U50, F), FL.Methane.make(4), FL.DistW.make(50)), ZL_IS);
			RM.DistillationTower.addRecipe0(F, 64, 16, new long[] { 500, 500, 500}, FL.array(FL.BiomassIC2 .make( 80)), FL.array(FL.Reikanol.make(20, FL.BioEthanol), MT.Glycerol.liquid(U50, F), FL.Methane.make(4), FL.DistW.make(50)), ZL_IS);
			RM.DistillationTower.addRecipe0(F, 64, 256, new long[]{5000, 5000, 5000}, FL.array(FL.Oil_ExtraHeavy.make(25)), FL.array(FL.Fuel.make(35), FL.Diesel.make(25), FL.Kerosine.make(25), FL.Petrol.make(20), FL.Propane.make(5), FL.Butane.make(5), FL.lube(50)), dustTiny.mat(MT.WaxParaffin, 1), dustTiny.mat(MT.Asphalt, 1), dustTiny.mat(MT.PetCoke, 1));
			RM.DistillationTower.addRecipe0(F, 64, 196, new long[]{4000, 4000, 4000}, FL.array(FL.Oil_Heavy.make(25)), FL.array(FL.Fuel.make(30), FL.Diesel.make(20), FL.Kerosine.make(20), FL.Petrol.make(15), FL.Propane.make(10), FL.Butane.make(10), FL.lube(40)), dustTiny.mat(MT.WaxParaffin, 1), dustTiny.mat(MT.Asphalt, 1), dustTiny.mat(MT.PetCoke, 1));
			if(FL.Oil_Heavy2.exists())RM.DistillationTower.addRecipe0(F, 64, 196, new long[]{4000, 4000, 4000}, FL.array(FL.Oil_Heavy2.make(25)), FL.array(FL.Fuel.make(30), FL.Diesel.make(20), FL.Kerosine.make(20), FL.Petrol.make(15), FL.Propane.make(10), FL.Butane.make(10), FL.lube(40)), dustTiny.mat(MT.WaxParaffin, 1), dustTiny.mat(MT.Asphalt, 1), dustTiny.mat(MT.PetCoke, 1));
			RM.DistillationTower.addRecipe0(F, 64, 128, new long[]{3000, 3000, 3000}, FL.array(FL.Oil_Medium.make(25)), FL.array(FL.Fuel.make(25), FL.Diesel.make(15), FL.Kerosine.make(15), FL.Petrol.make(15), FL.Propane.make(15), FL.Butane.make(15), FL.lube(25)), dustTiny.mat(MT.WaxParaffin, 1), dustTiny.mat(MT.Asphalt, 1), dustTiny.mat(MT.PetCoke, 1));
			RM.DistillationTower.addRecipe0(F, 64, 128, new long[]{3000, 3000, 3000}, FL.array(FL.Oil_Normal.make(25)), FL.array(FL.Fuel.make(25), FL.Diesel.make(15), FL.Kerosine.make(15), FL.Petrol.make(15), FL.Propane.make(15), FL.Butane.make(15), FL.lube(25)), dustTiny.mat(MT.WaxParaffin, 1), dustTiny.mat(MT.Asphalt, 1), dustTiny.mat(MT.PetCoke, 1));
			if(FL.Oil_HotCrude.exists())RM.DistillationTower.addRecipe0(F, 64, 64, new long[]{3000, 3000, 3000}, FL.array(FL.Oil_HotCrude.make(25)), FL.array(FL.Fuel.make(25), FL.Diesel.make(15), FL.Kerosine.make(15), FL.Petrol.make(15), FL.Propane.make(15), FL.Butane.make(15), FL.lube(25)), dustTiny.mat(MT.WaxParaffin, 1), dustTiny.mat(MT.Asphalt, 1), dustTiny.mat(MT.PetCoke, 1));
			RM.DistillationTower.addRecipe0(F, 64, 64, new long[]{2000, 2000, 2000}, FL.array(FL.Oil_Light.make(25)), FL.array(FL.Fuel.make(15), FL.Diesel.make(10), FL.Kerosine.make(10), FL.Petrol.make(10), FL.Propane.make(25), FL.Butane.make(25), FL.lube(15)), dustTiny.mat(MT.WaxParaffin, 1), dustTiny.mat(MT.Asphalt, 1), dustTiny.mat(MT.PetCoke, 1));
			if(FL.Oil_Light2.exists())RM.DistillationTower.addRecipe0(F, 64, 64, new long[]{2000, 2000, 2000}, FL.array(FL.Oil_Light2.make(25)), FL.array(FL.Fuel.make(15), FL.Diesel.make(10), FL.Kerosine.make(10), FL.Petrol.make(10), FL.Propane.make(25), FL.Butane.make(25), FL.lube(15)), dustTiny.mat(MT.WaxParaffin, 1), dustTiny.mat(MT.Asphalt, 1), dustTiny.mat(MT.PetCoke, 1));
			RM.DistillationTower.addRecipe0(F, 64, 64, new long[]{1000, 1000, 1000}, FL.array(FL.Oil_Soulsand.make(25)), FL.array(FL.Fuel.make(10), FL.Diesel.make(5), FL.Kerosine.make(5), FL.Petrol.make(5), FL.Propane.make(5), FL.Butane.make(5), FL.lube(40)), dustTiny.mat(MT.WaxParaffin, 1), dustTiny.mat(MT.Asphalt, 1), dustTiny.mat(MT.PetCoke, 1));
			RM.SteamCracking .addRecipe0(F, 16, 64, FL.array(FL.Steam.make(1000), FL.Propane.make(100)), FL.array(FL.Hydrogen.make( 2), FL.Methane.make(27), FL.Ethylene.make(42), FL.Propylene.make(19)), ZL_IS);
			RM.SteamCracking .addRecipe0(F, 16, 64, FL.array(FL.Steam.make(1000), FL.Butane .make(100)), FL.array(FL.Hydrogen.make( 5), FL.Methane.make( 9), FL.Ethylene.make(78), FL.Propylene.make( 3)), ZL_IS);
			RM.CatalyticCracking.addRecipe1(F, 16, 64, OP.dust.mat(MT.Pt, 0), FL.array(FL.Hydrogen.make(100), FL.Petrol.make(100)), FL.array(FL.Ethylene.make(30), FL.Propylene.make(20)), ZL_IS);
			RM.CatalyticCracking.addRecipe1(F, 16, 64, OP.dust.mat(MT.Pt, 0), FL.array(FL.Hydrogen.make(100), FL.Fuel .make(100)), FL.array(FL.Ethylene.make(40), FL.Propylene.make(10)), ZL_IS);
			RM.Mixer .addRecipe1(T, 16, 16, OP.dust.mat(MT.MgCl2, 0), FL.array(MT.TiCl4.liquid(U1000, T), FL.Ethylene .make(100)), ZL_FS, OP.dust.mat(MT.Plastic, 1));
			RM.Mixer .addRecipe1(T, 16, 16, OP.dust.mat(MT.MgCl2, 0), FL.array(MT.TiCl4.liquid(U1000, T), FL.Propylene.make(100)), ZL_FS, OP.dust.mat(MT.Plastic, 1));
		});
	}
}

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
 * the GregTech6-Unofficial entries of gregtech/loaders/c/Loader_Recipes_Other.java moved into an addon registry.
 */

package gt6u.loaders;


import gt6u.data.FL_GT6U;
import gt6u.data.IL_GT6U;
import gt6u.data.RM_GT6U;

import static gregapi.data.CS.*;
import static gregapi.data.OP.*;
import gregapi.block.metatype.BlockMetaType;
import gregapi.block.multitileentity.MultiTileEntityRegistry;
import gregapi.data.*;
import gregapi.data.CS.BlocksGT;
import gregapi.data.CS.FluidsGT;
import gregapi.oredict.OreDictMaterial;
import gregapi.oredict.OreDictPrefix;
import gregapi.oredict.event.IOreDictListenerEvent;
import gregapi.oredict.event.OreDictListenerEvent_Names;
import gregapi.util.CR;
import gregapi.util.OM;
import gregapi.util.ST;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;
import gt6u.GT6U_Recipes;

/** GregTech6-Unofficial's edits of gregtech/loaders/c/Loader_Recipes_Other.java, as statements: what GT6U added runs in run(), what it dropped is taken back in
 *  takeBack(): restated as GT6 makes them, their Recipes are taken out of GT6's maps again. */
public class Loader_Recipes_Other {
	public static void run() {
		run_run();
	}
	
	public static void takeBack() {
		takeBack_run();
	}
	
	/** GT6U's additions to Loader_Recipes_Other.run (GT6U gregtech/loaders/c/Loader_Recipes_Other.java). */
	static void run_run() {
		new OreDictListenerEvent_Names() {
			@Override public void addAllListeners() {
				addListener(DYE_OREDICTS_LENS[DYE_INDEX_Red], new IOreDictListenerEvent(){@Override public void onOreRegistration(OreDictRegistrationContainer aEvent){for(OreDictMaterial tMat:ANY.Cu.mToThis)RM.LaserEngraver.addRecipe2(T, 16, 64, foil.mat(tMat, 4), ST.amount(0, aEvent.mStack), IL.Circuit_Wire_Copper.get(1));RM.LaserEngraver.addRecipe2(T, 16, 64, foil.mat(MT.Au, 4), ST.amount(0, aEvent.mStack), IL.Circuit_Wire_Gold.get(1));RM_GT6U.MaskAligner.addRecipe2(T, 16, 64, foil.mat(MT.Pt, 4), ST.amount(0, aEvent.mStack), IL.Circuit_Wire_Platinum.get(1));RM.LaserEngraver.addRecipe2(T, 16, 64, foil.mat(MT.Thaumium, 4), ST.amount(0, aEvent.mStack), IL.Circuit_Wire_Magic.get(1));RM.LaserEngraver.addRecipe2(T, 16, 64, foil.mat(MT.Manasteel, 4), ST.amount(0, aEvent.mStack), IL.Circuit_Wire_Magic.get(1));RM.LaserEngraver.addRecipe2(T, 16, 64, foil.mat(MT.Mithril, 4), ST.amount(0, aEvent.mStack), IL.Circuit_Wire_Magic.get(1));RM.LaserEngraver.addRecipe2(T, 16, 64, foil.mat(MT.Netherite, 4), ST.amount(0, aEvent.mStack), IL.Circuit_Wire_Magic.get(1));RM.LaserEngraver.addRecipe2(T, 16, 64, foil.mat(MT.Enderium, 4), ST.amount(0, aEvent.mStack), IL.Circuit_Wire_Enderium.get(1));RM.LaserEngraver.addRecipe2(T, 16, 64, foil.mat(MT.Signalum, 4), ST.amount(0, aEvent.mStack), IL.Circuit_Wire_Signalum.get(1));}});
				addListener(OD.itemResin, new IOreDictListenerEvent(){@Override public void onOreRegistration(OreDictRegistrationContainer aEvent){RM.rem_smelting(aEvent.mStack);if(MD.TiC.owns(aEvent.mStack))return;RM.biomass(ST.amount(6, aEvent.mStack), 32);RM.ic2_extractor(aEvent.mStack, OM.ingot(MT.Rubber, 7*U9));RM.Juicer.addRecipe1(T, 16, 64, aEvent.mStack, NF, FL.Latex.make(L/2), NI);RM.Squeezer.addRecipe1(T, 16, 64, aEvent.mStack, NF, FL.Latex.make(L), NI);RM.Centrifuge.addRecipe1(T, 16, 64, aEvent.mStack, NF, FL.Latex.make(L), FL.Glue.make(250));for(FluidStack tWater:FL.waters(250))RM.Mixer.addRecipe1(T, 16, 16, aEvent.mStack, tWater, FL.Glue.make(250), ZL_IS);RM.Laminator.addRecipe2(T, 16, 16, aEvent.mStack, ST.make(Blocks.PISTON, 1, W), ST.make(Blocks.STICKY_PISTON, 1, 0));for(OreDictMaterial tMat:ANY.Wood.mToThis){ItemStack tDust=OP.dust.mat(tMat, 4);if(ST.valid(tDust))RM.Press.addRecipeX(T, F, F, F, T, 16, 64, ST.array(aEvent.mStack, tDust, OP.foil.mat(MT.Cu, 1)), IL_GT6U.Circuit_Plate_Empty_Phenolic.get(1));}}});
			}
		};
		for (ItemStack tTNT : ST.array(ST.make(Blocks.TNT, 8, W), IL.IC2_ITNT.get(4), IL.Boomstick.get(12), IL.Dynamite.get(2), IL.Dynamite_Strong.get(1))) if (ST.valid(tTNT)) {
			for (OreDictMaterial tMat : ANY.Diamond.mToThis) {
				OreDictMaterial tOutput = MT.DiamondIndustrial;
				GT6U_Recipes.first(Loader_Recipes_Other::later_run_0); RM.ImplosionCompressor.addRecipeX(T, 16, 256, ST.array(dust.mat(tMat, 1), tTNT , ST.tag(0)), plateGem .mat(tOutput, 1)); GT6U_Recipes.first(null);
				GT6U_Recipes.first(Loader_Recipes_Other::later_run_1); RM.ImplosionCompressor.addRecipeX(T, 16, 256, ST.array(dust.mat(tMat, 1), tTNT , ST.tag(1)), gem .mat(tOutput, 1)); GT6U_Recipes.first(null);
				GT6U_Recipes.first(Loader_Recipes_Other::later_run_2); RM.ImplosionCompressor.addRecipeX(T, 16, 256, ST.array(dust.mat(tMat, 2), ST.mul(4, tTNT), ST.tag(2)), gemFlawless .mat(tOutput, 1)); GT6U_Recipes.first(null);
				GT6U_Recipes.first(Loader_Recipes_Other::later_run_3); RM.ImplosionCompressor.addRecipeX(T, 16, 256, ST.array(dust.mat(tMat, 4), ST.mul(8, tTNT), ST.tag(3)), gemExquisite.mat(tOutput, 1)); GT6U_Recipes.first(null);
			}
			for (OreDictMaterial tMat : ANY.Sapphire.mToThis) {
				OreDictMaterial tOutput = MT.Sapphire;
				GT6U_Recipes.first(Loader_Recipes_Other::later_run_4); RM.ImplosionCompressor.addRecipeX(T, 16, 256, ST.array(dust.mat(tMat, 1), tTNT , ST.tag(0)), plateGem .mat(tOutput, 1)); GT6U_Recipes.first(null);
				GT6U_Recipes.first(Loader_Recipes_Other::later_run_5); RM.ImplosionCompressor.addRecipeX(T, 16, 256, ST.array(dust.mat(tMat, 1), tTNT , ST.tag(1)), gem .mat(tOutput, 1)); GT6U_Recipes.first(null);
				GT6U_Recipes.first(Loader_Recipes_Other::later_run_6); RM.ImplosionCompressor.addRecipeX(T, 16, 256, ST.array(dust.mat(tMat, 2), ST.mul(4, tTNT), ST.tag(2)), gemFlawless .mat(tOutput, 1)); GT6U_Recipes.first(null);
				GT6U_Recipes.first(Loader_Recipes_Other::later_run_7); RM.ImplosionCompressor.addRecipeX(T, 16, 256, ST.array(dust.mat(tMat, 4), ST.mul(8, tTNT), ST.tag(3)), gemExquisite.mat(tOutput, 1)); GT6U_Recipes.first(null);
			}
			for (OreDictMaterial tMat : ANY.Garnet.mToThis) {
				OreDictMaterial tOutput = tMat;
				GT6U_Recipes.first(Loader_Recipes_Other::later_run_8); RM.ImplosionCompressor.addRecipeX(T, 16, 256, ST.array(dust.mat(tMat, 1), tTNT , ST.tag(0)), plateGem .mat(tOutput, 1)); GT6U_Recipes.first(null);
				GT6U_Recipes.first(Loader_Recipes_Other::later_run_9); RM.ImplosionCompressor.addRecipeX(T, 16, 256, ST.array(dust.mat(tMat, 1), tTNT , ST.tag(1)), gem .mat(tOutput, 1)); GT6U_Recipes.first(null);
				GT6U_Recipes.first(Loader_Recipes_Other::later_run_10); RM.ImplosionCompressor.addRecipeX(T, 16, 256, ST.array(dust.mat(tMat, 2), ST.mul(4, tTNT), ST.tag(2)), gemFlawless .mat(tOutput, 1)); GT6U_Recipes.first(null);
				GT6U_Recipes.first(Loader_Recipes_Other::later_run_11); RM.ImplosionCompressor.addRecipeX(T, 16, 256, ST.array(dust.mat(tMat, 4), ST.mul(8, tTNT), ST.tag(3)), gemExquisite.mat(tOutput, 1)); GT6U_Recipes.first(null);
			}
			for (OreDictMaterial tMat : ANY.Jasper.mToThis) {
				OreDictMaterial tOutput = tMat;
				GT6U_Recipes.first(Loader_Recipes_Other::later_run_12); RM.ImplosionCompressor.addRecipeX(T, 16, 256, ST.array(dust.mat(tMat, 1), tTNT , ST.tag(0)), plateGem .mat(tOutput, 1)); GT6U_Recipes.first(null);
				GT6U_Recipes.first(Loader_Recipes_Other::later_run_13); RM.ImplosionCompressor.addRecipeX(T, 16, 256, ST.array(dust.mat(tMat, 1), tTNT , ST.tag(1)), gem .mat(tOutput, 1)); GT6U_Recipes.first(null);
				GT6U_Recipes.first(Loader_Recipes_Other::later_run_14); RM.ImplosionCompressor.addRecipeX(T, 16, 256, ST.array(dust.mat(tMat, 2), ST.mul(4, tTNT), ST.tag(2)), gemFlawless .mat(tOutput, 1)); GT6U_Recipes.first(null);
				GT6U_Recipes.first(Loader_Recipes_Other::later_run_15); RM.ImplosionCompressor.addRecipeX(T, 16, 256, ST.array(dust.mat(tMat, 4), ST.mul(8, tTNT), ST.tag(3)), gemExquisite.mat(tOutput, 1)); GT6U_Recipes.first(null);
			}
			for (OreDictMaterial tMat : ANY.TigerEye.mToThis) {
				OreDictMaterial tOutput = tMat;
				GT6U_Recipes.first(Loader_Recipes_Other::later_run_16); RM.ImplosionCompressor.addRecipeX(T, 16, 256, ST.array(dust.mat(tMat, 1), tTNT , ST.tag(0)), plateGem .mat(tOutput, 1)); GT6U_Recipes.first(null);
				GT6U_Recipes.first(Loader_Recipes_Other::later_run_17); RM.ImplosionCompressor.addRecipeX(T, 16, 256, ST.array(dust.mat(tMat, 1), tTNT , ST.tag(1)), gem .mat(tOutput, 1)); GT6U_Recipes.first(null);
				GT6U_Recipes.first(Loader_Recipes_Other::later_run_18); RM.ImplosionCompressor.addRecipeX(T, 16, 256, ST.array(dust.mat(tMat, 2), ST.mul(4, tTNT), ST.tag(2)), gemFlawless .mat(tOutput, 1)); GT6U_Recipes.first(null);
				GT6U_Recipes.first(Loader_Recipes_Other::later_run_19); RM.ImplosionCompressor.addRecipeX(T, 16, 256, ST.array(dust.mat(tMat, 4), ST.mul(8, tTNT), ST.tag(3)), gemExquisite.mat(tOutput, 1)); GT6U_Recipes.first(null);
			}
			for (OreDictMaterial tMat : ANY.Aventurine.mToThis) {
				OreDictMaterial tOutput = tMat;
				GT6U_Recipes.first(Loader_Recipes_Other::later_run_20); RM.ImplosionCompressor.addRecipeX(T, 16, 256, ST.array(dust.mat(tMat, 1), tTNT , ST.tag(0)), plateGem .mat(tOutput, 1)); GT6U_Recipes.first(null);
				GT6U_Recipes.first(Loader_Recipes_Other::later_run_21); RM.ImplosionCompressor.addRecipeX(T, 16, 256, ST.array(dust.mat(tMat, 1), tTNT , ST.tag(1)), gem .mat(tOutput, 1)); GT6U_Recipes.first(null);
				GT6U_Recipes.first(Loader_Recipes_Other::later_run_22); RM.ImplosionCompressor.addRecipeX(T, 16, 256, ST.array(dust.mat(tMat, 2), ST.mul(4, tTNT), ST.tag(2)), gemFlawless .mat(tOutput, 1)); GT6U_Recipes.first(null);
				GT6U_Recipes.first(Loader_Recipes_Other::later_run_23); RM.ImplosionCompressor.addRecipeX(T, 16, 256, ST.array(dust.mat(tMat, 4), ST.mul(8, tTNT), ST.tag(3)), gemExquisite.mat(tOutput, 1)); GT6U_Recipes.first(null);
			}
		}
		RM.Massfab .addRecipe2(T, 16, 144 , ST.tag(0), OP.dustDiv72.mat(MT.AncientDebris, 1), NF, FL.Ender.make(L ), ZL_IS);
		RM.Massfab .addRecipe2(T, 16, 144* 8, ST.tag(0), OP.dustTiny .mat(MT.AncientDebris, 1), NF, FL.Ender.make(L* 8), ZL_IS);
		RM.Massfab .addRecipe2(T, 16, 144* 18, ST.tag(0), OP.dustSmall.mat(MT.AncientDebris, 1), NF, FL.Ender.make(L* 18), ZL_IS);
		RM.Massfab .addRecipe2(T, 16, 144* 72, ST.tag(0), OP.dust .mat(MT.AncientDebris, 1), NF, FL.Ender.make(L* 72), ZL_IS);
		RM.Massfab .addRecipe2(T, 16, 144* 72, ST.tag(0), OP.ingot .mat(MT.AncientDebris, 1), NF, FL.Ender.make(L* 72), ZL_IS);
		RM.Massfab .addRecipe2(T, 16, 144*648, ST.tag(0), OP.blockDust.mat(MT.AncientDebris, 1), NF, FL.Ender.make(L*648), ZL_IS);
		RM.Replicator.addRecipe1(T, 16, 144 , ST.tag(0), FL.Ender.make(L), NF, OP.gem.mat(MT.EnderPearl, 1));
		RM.Lightning .addRecipe1(T, 512, 512, OP.blockSolid.mat(MT.Netherite, 1), FL.array(FL.Ender.make(L*4), FL.Oil_Soulsand.make(5000)), ZL_FS, OP.gem.mat(MT.NetherStar, 1));
		RM.Fusion.addRecipe1(T, 2048, 196, ST.tag(2), FL.array(MT.D .gas (U*2, T), MT.H .gas (U*1 , T)), FL.array(MT.He_3 .gas ( U*1, F) ), ZL_IS ).setSpecialNumber( 2000L*20000L*1L);
		RM.Fusion.addRecipe1(T, 2048, 196, ST.tag(1), FL.array(MT.T .gas (U*2, T) ), FL.array(MT.He .gas ( U*1 , F) ), ZL_IS ).setSpecialNumber( 2000L*20000L*1L);
		RM.Fusion.addRecipe1(T, 2048, 196, ST.tag(2), FL.array(MT.D .gas (U*1, T), MT.T .gas (U*1 , T)), FL.array(MT.He .plasma( U*1 , F) ), ZL_IS ).setSpecialNumber( 2000L*3000L*10L);
		RM.Fusion.addRecipe1(T, 2048, 196, ST.tag(2), FL.array(MT.He_3 .gas (U*1, T), MT.D .gas (U*1 , T)), FL.array(MT.He .plasma( U*1 , F) ), ZL_IS ).setSpecialNumber( 2000L*3000L*10L);
		RM.Fusion.addRecipe1(T, 2048, 196, ST.tag(1), FL.array(MT.He_3 .gas (U*2, T) ), FL.array(MT.He .plasma( U*1 , F) ), ZL_IS ).setSpecialNumber( 2000L*3000L*10L);
		RM.Fusion.addRecipe1(T, 2048, 196, ST.tag(2), FL.array(MT.He .gas (U*1, T), MT.D .gas (U*1 , T)), FL.array(MT.Li .liquid( U*1 , F) ), ZL_IS ).setSpecialNumber( 2000L*3000L*10L);
		RM.Fusion.addRecipe1(T, 2048, 196, ST.tag(2), FL.array(MT.He_3 .gas (U*1, T), MT.T .gas (U*1 , T)), FL.array(MT.Li .liquid( U*1 , F) ), ZL_IS ).setSpecialNumber( 2000L*3000L*10L);
		RM.Fusion.addRecipe1(T, 2048, 196, ST.tag(1), FL.array(MT.He .gas (U*2, T) ), FL.array(MT.Be_8 .liquid( U*1 , F) ), ZL_IS ).setSpecialNumber( 2000L*3000L*10L);
		RM.Fusion.addRecipe1(T, 2048, 196, ST.tag(2), FL.array(MT.He_3 .gas (U*1, T), MT.He .gas (U*1 ,T)), FL.array(MT.Be_7 .liquid( U*1 , F) ), ZL_IS ).setSpecialNumber( 2000L*3000L*10L);
		RM.Fusion.addRecipe1(T, 2048, 196, ST.tag(2), FL.array(MT.He .gas (U*1, T), MT.Be_8.liquid(U*1 ,T)), FL.array(MT.C .liquid( U*1 , F) ), ZL_IS ).setSpecialNumber( 2000L*3000L*10L);
		RM.Fusion.addRecipe1(T, 2048, 196, ST.tag(2), FL.array(MT.H .gas (U*1, T), MT.C .liquid(U*1 ,T)), FL.array(MT.C_13 .liquid( U*1 , F) ), ZL_IS ).setSpecialNumber( 2000L*3000L*10L);
		RM.Fusion.addRecipe1(T, 2048, 196, ST.tag(2), FL.array(MT.D .gas (U*1, T), MT.C_13.liquid(U*1 ,T)), FL.array(MT.N .plasma( U*1 , F) ), ZL_IS ).setSpecialNumber( 2000L*3000L*10L);
		RM.Fusion.addRecipe1(T, 2048, 196, ST.tag(2), FL.array(MT.T .gas (U*1, T), MT.C .liquid(U*1 ,T)), FL.array(MT.N .plasma( U*1 , F) ), ZL_IS ).setSpecialNumber( 2000L*3000L*10L);
		RM.Fusion.addRecipe1(T, 2048, 196, ST.tag(1), FL.array(MT.Be_8 .liquid (U*2, T) ), FL.array(MT.O .plasma( U*1 , F) ), ZL_IS ).setSpecialNumber( 2000L*3000L*10L);
		RM.Fusion.addRecipe1(T, 2048, 196, ST.tag(2), FL.array(MT.He .gas (U*1, T), MT.C .liquid(U*1 ,T)), FL.array(MT.O .plasma( U*1 , F) ), ZL_IS ).setSpecialNumber( 2000L*3000L*10L);
		RM.Fusion.addRecipe1(T, 2048, 196, ST.tag(2), FL.array(MT.D .gas (U*1, T), MT.N .gas (U*1 ,T)), FL.array(MT.O .plasma( U*1 , F) ), ZL_IS ).setSpecialNumber( 2000L*3000L*10L);
		RM.Fusion.addRecipe1(T, 2048, 196, ST.tag(2), FL.array(MT.D .gas (U*1, T), MT.O .gas (U*1 ,T)), FL.array(MT.F .gas( U*1 , F) ), ZL_IS ).setSpecialNumber( 2000L*3000L*10L);
		RM.Fusion.addRecipe1(T, 2048, 196, ST.tag(2), FL.array(MT.He .gas (U*1, T), MT.O .gas (U*1 ,T)), FL.array(MT.Ne .gas( U*1 , F) ), ZL_IS ).setSpecialNumber( 2000L*3000L*10L);
		RM.Fusion.addRecipe1(T, 2048, 196, ST.tag(1), FL.array(MT.N .gas (U*2, T) ), FL.array(MT.Si .liquid( U*1 , F) ), ZL_IS ).setSpecialNumber( 2000L*3000L*10L);
		RM.Fusion.addRecipe1(T, 2048, 196, ST.tag(1), FL.array(MT.O .gas (U*2, T) ), FL.array(MT.S .liquid( U*1 , F) ), ZL_IS ).setSpecialNumber( 2000L*3000L*10L);
		RM.Fusion.addRecipe1(T, 2048, 196, ST.tag(2), FL.array(MT.HF .gas (U*1, T), MT.N .gas (U*1 ,T)), FL.array(MT.Cl .gas( U*1 , F) ), ZL_IS ).setSpecialNumber( 2000L*3000L*10L);
		RM.Fusion.addRecipe1(T, 2048, 196, ST.tag(1), FL.array(MT.Ne .gas (U*2, T) ), FL.array(MT.Ar .gas( U*1 , F) ), ZL_IS ).setSpecialNumber( 2000L*3000L*10L);
		RM.Fusion.addRecipe1(T, 4096, 196, ST.tag(2), FL.array(MT.Al .liquid (U*1, T), MT.Na .liquid(U*1 ,T)), FL.array(MT.Ti .liquid( U*1 , F) ), ZL_IS ).setSpecialNumber( 2000L*3000L*10L);
		RM.Fusion.addRecipe1(T, 4096, 196, ST.tag(2), FL.array(MT.Ti .liquid (U*1, T), MT.T .gas(U*1 ,T)), FL.array(MT.V .liquid( U*1 , F) ), ZL_IS ).setSpecialNumber( 2000L*3000L*10L);
		RM.Fusion.addRecipe1(T, 4096, 196, ST.tag(2), FL.array(MT.V .liquid (U*1, T), MT.H .gas(U*1 ,T)), FL.array(MT.Cr .liquid( U*1 , F) ), ZL_IS ).setSpecialNumber( 2000L*3000L*10L);
		RM.Fusion.addRecipe1(T, 4096, 196, ST.tag(2), FL.array(MT.Cr .liquid (U*1, T), MT.T .gas(U*1 ,T)), FL.array(MT.Mn .liquid( U*1 , F) ), ZL_IS ).setSpecialNumber( 2000L*3000L*10L);
		RM.Fusion.addRecipe1(T, 4096, 196, ST.tag(2), FL.array(MT.Mn .liquid (U*1, T), MT.H .gas(U*1 ,T)), FL.array(MT.Fe .liquid( U*1 , F) ), ZL_IS ).setSpecialNumber( 2000L*3000L*10L);
		RM.Fusion.addRecipe1(T, 4096, 196, ST.tag(2), FL.array(MT.Cr .liquid (U*1, T), MT.He .gas(U*1 ,T)), FL.array(MT.Fe .liquid( U*1 , F) ), ZL_IS ).setSpecialNumber( 2000L*3000L*10L);
		RM.Fusion.addRecipe1(T, 4096, 196, ST.tag(2), FL.array(MT.Ti .liquid (U*1, T), MT.Be .liquid(U*1 ,T)), FL.array(MT.Fe .liquid( U*1 , F) ), ZL_IS ).setSpecialNumber( 2000L*3000L*10L);
		RM.Fusion.addRecipe1(T, 4096, 196, ST.tag(2), FL.array(MT.Ar .gas (U*1, T), MT.O .gas(U*1 ,T)), FL.array(MT.Fe .plasma( U*1 , F) ), ZL_IS ).setSpecialNumber( 2000L*3000L*10L);
		RM.Fusion.addRecipe1(T, 4096, 196, ST.tag(1), FL.array(MT.Si .liquid (U*2, T) ), FL.array(MT.Fe .plasma( U*1 , F) ), ZL_IS ).setSpecialNumber( 2000L*3000L*10L);
		RM.Fusion.addRecipe1(T, 4096, 196, ST.tag(2), FL.array(MT.Mg .liquid (U*1, T), MT.S .liquid(U*1 ,T)), FL.array(MT.Fe .plasma( U*1 , F) ), ZL_IS ).setSpecialNumber( 2000L*3000L*10L);
		RM.Fusion.addRecipe1(T, 8192, 196, ST.tag(2), FL.array(MT.Fe .liquid (U*1, T), MT.V .liquid(U*1 ,T)), FL.array(MT.Ru .liquid( U*1 , F) ), ZL_IS ).setSpecialNumber( 2000L*3000L*10L);
		RM.Fusion.addRecipe1(T, 8192, 196, ST.tag(2), FL.array(MT.Cr .liquid (U*1, T), MT.Ar .gas(U*1 ,T)), FL.array(MT.Y .liquid( U*1 , F) ), ZL_IS ).setSpecialNumber( 2000L*3000L*10L);
		RM.Fusion.addRecipe1(T, 8192, 196, ST.tag(2), FL.array(MT.Y .liquid (U*1, T), MT.Ru .liquid(U*1 ,T)), FL.array(MT.Ta .liquid( U*1 , F) ), ZL_IS ).setSpecialNumber( 2000L*3000L*10L);
		RM.Fusion.addRecipe1(T, 8192, 196, ST.tag(2), FL.array(MT.Ta .liquid (U*1, T), MT.T .gas(U*1 ,T)), FL.array(MT.W .liquid( U*1 , F) ), ZL_IS ).setSpecialNumber( 2000L*3000L*10L);
		RM.Fusion.addRecipe1(T, 8192, 196, ST.tag(2), FL.array(MT.Hf .liquid (U*1, T), MT.He .gas(U*1 ,T)), FL.array(MT.W .liquid( U*1 , F) ), ZL_IS ).setSpecialNumber( 2000L*3000L*10L);
		RM.Fusion.addRecipe1(T, 8192, 196, ST.tag(2), FL.array(MT.W .liquid (U*1, T), MT.T .gas(U*2 ,T)), FL.array(MT.Os .liquid( U*1 , F) ), ZL_IS ).setSpecialNumber( 2000L*3000L*10L);
		RM.Fusion.addRecipe1(T, 8192, 196, ST.tag(2), FL.array(MT.W .liquid (U*1, T), MT.Li_6.liquid(U*1 ,T)), FL.array(MT.Ir .liquid( U*1 , F) ), ZL_IS ).setSpecialNumber( 2000L*3000L*10L);
		RM.Fusion.addRecipe1(T, 8192, 196, ST.tag(2), FL.array(MT.Ir .liquid (U*1, T), MT.T .gas(U*1 ,T)), FL.array(MT.Pt .liquid( U*1 , F) ), ZL_IS ).setSpecialNumber( 2000L*3000L*10L);
		RM.Fusion.addRecipe1(T, 8192, 196, ST.tag(2), FL.array(MT.Pt .liquid (U*1, T), MT.H .gas(U*1 ,T)), FL.array(MT.Au .liquid( U*1 , F) ), ZL_IS ).setSpecialNumber( 2000L*3000L*10L);
		RM.Fusion.addRecipe1(T, 8192, 196, ST.tag(2), FL.array(MT.Os .liquid (U*1, T), MT.Li_6.liquid(U*1 ,T)), FL.array(MT.Hg .liquid( U*1 , F) ), ZL_IS ).setSpecialNumber( 2000L*3000L*10L);
		RM.Fusion.addRecipe1(T, 8192, 196, ST.tag(2), FL.array(MT.Hg .liquid (U*1, T), MT.Au .liquid(U*1 ,T)), FL.array(MT.Rh .liquid( U*1 , F) ), ZL_IS ).setSpecialNumber( 2000L*3000L*10L);
		RM.Fusion.addRecipe1(T, 16384, 196, ST.tag(2), FL.array(MT.Hg .liquid (U*1, T), MT.Mg .liquid(U*1 ,T)), FL.array(MT.U_238 .liquid( U*1 , F), MT.H .gas(U*1, F) ), ZL_IS ).setSpecialNumber( 2000L*3000L*10L);
		RM.Fusion.addRecipe1(T, 16384, 196, ST.tag(2), FL.array(MT.Au .liquid (U*1, T), MT.Al .liquid(U*1 ,T)), FL.array(MT.U_238 .liquid( U*1 , F), MT.H .gas(U*1, F) ), ZL_IS ).setSpecialNumber( 2000L*3000L*10L);
		RM.Fusion.addRecipe1(T, 16384, 196, ST.tag(2), FL.array(MT.U_238 .liquid (U*1, T), MT.He .gas(U*1 ,T)), FL.array(MT.Pu .liquid( U*1 , F), MT.H .gas(U*1, F) ), ZL_IS ).setSpecialNumber( 2000L*3000L*10L);
		RM.Fusion.addRecipe1(T, 32768, 196, ST.tag(2), FL.array(MT.Ir .liquid (U*1, T), MT.Ar .gas(U*1 ,T)), FL.array(MT.Am .liquid( U*1 , F), MT.He .plasma(U*1, F) ), ZL_IS ).setSpecialNumber( 2000L*3000L*10L);
		RM.Fusion.addRecipe1(T, 32768, 196, ST.tag(2), FL.array(MT.Nq_528.liquid (U100*3, T), MT.Rn .gas(U100*25 ,T)), FL.array(MT.Nq_522.liquid( U100*1 , F) ), ZL_IS ).setSpecialNumber( 2000L*3000L*10L);
		RM.Fusion.addRecipe1(T,16384, 1024, ST.tag(2), FL.array(MT.Ad .liquid(U*1, T), MT.Be_7.liquid(U*1 , T)), FL.array(MT.W .liquid( U*1 , F), MT.He .gas (16*U, F), MT.He_3 .gas (U*24, F), MT.T .gas (U*24, F)), OP.dust.mat(MT.Vb, 1)).setSpecialNumber(60000L*20000L*12L);
		RM.Fusion.addRecipeX(T, 131072, 196, ST.array(ST.tag(2), dustTiny.mat(MT.Vb, 1)), FL.array(MT.Nq_522.liquid (U100*16, T), MT.Am .liquid(U100*16 ,T)), FL.array(MT.Neutronium.liquid( U100*1 , F) ), ZL_IS ).setSpecialNumber( 2000L*3000L*10L);
		RM_GT6U.ParticleCollider.addRecipe1(T, 8192, 20, new long[] {100, 100, 10, 200, 200, 50}, ST.tag(1), FL_GT6U.HydrogenI.make(10), FL.Hydrogen.make(5), IL_GT6U.Proton.get(1), IL_GT6U.Anti_Proton.get(1), IL_GT6U.Neutron.get(1), IL_GT6U.Electron.get(1), IL_GT6U.Positron.get(1), IL_GT6U.Neutrino.get(1)).setSpecialNumber(10000000);
		RM_GT6U.ParticleCollider.addRecipe1(T, 8192, 40, new long[] {80, 80, 8, 160, 160, 30}, ST.tag(1), FL_GT6U.HeliumI.make(10), FL.Helium.make(6), IL_GT6U.Proton.get(1), IL_GT6U.Anti_Proton.get(1), IL_GT6U.Neutron.get(1), IL_GT6U.Electron.get(1), IL_GT6U.Positron.get(1), IL_GT6U.Neutrino.get(1)).setSpecialNumber(10000000);
		RM_GT6U.ParticleCollider.addRecipeX(T, 32768, 20, new long[] {200, 200, 20, 5, 100, 1}, ST.array(IL_GT6U.Electron.get(10),IL_GT6U.Positron.get(10)), FL.array(MT.Xe.gas(U, T)), FL.array(MT.Xe.gas(U, T)), IL_GT6U.Proton.get(10), IL_GT6U.Anti_Proton.get(10), IL_GT6U.Neutron.get(10), IL_GT6U.Higgs_Boson.get(10), IL_GT6U.Neutrino.get(10), IL_GT6U.Kerr_Blackhole.get(10)).setSpecialNumber(10000000);
		RM_GT6U.ParticleCollider.addRecipeX(T, 32768, 40, new long[] {300, 300, 15, 5, 75, 1}, ST.array(ST.tag(1), IL_GT6U.Proton.get(20)), FL.array(MT.Xe.gas(U, T)), FL.array(MT.Xe.gas(U, T)), IL_GT6U.Electron.get(10), IL_GT6U.Positron.get(10), IL_GT6U.Neutron.get(10), IL_GT6U.Higgs_Boson.get(10), IL_GT6U.Neutrino.get(10), IL_GT6U.Kerr_Blackhole.get(10)).setSpecialNumber(10000000);
		RM.Welder.addRecipeX(T, 64, 40, ST.array(pipeTiny.mat(MT.TungstenSteel, 1), wireGt01.mat(MT.Nq, 3), IL.Electric_Pump_LV.get(2)), FL.array(FL.Nitrogen.make(2000)), ZL_FS, wireGt01.mat(MT.Superconductor, 3));
		RM.Welder.addRecipeX(T, 64, 40, ST.array(pipeTiny.mat(MT.TungstenSteel, 1), wireGt01.mat(MT.Nq, 3), IL.Electric_Pump_LV.get(2)), FL.array(FL.Helium.make(2000)), ZL_FS, wireGt01.mat(MT.Superconductor, 3));
		RM.Welder.addRecipeX(T, 64, 40, ST.array(pipeTiny.mat(MT.TungstenSteel, 1), wireGt01.mat(MT.Nq, 3), IL.Electric_Pump_LV.get(2)), FL.array(FL.Hydrogen.make(2000)), ZL_FS, wireGt01.mat(MT.Superconductor, 3));
		RM.Welder.addRecipeX(T, 64, 40, ST.array(pipeTiny.mat(MT.TungstenSteel, 1), wireGt01.mat(MT.YttriumBariumCuprate, 3), IL.Electric_Pump_LV.get(2)), FL.array(FL.Helium.make(2000)), ZL_FS, wireGt01.mat(MT.Superconductor, 3));
		RM.Welder.addRecipeX(T, 64, 40, ST.array(pipeTiny.mat(MT.TungstenSteel, 1), wireGt01.mat(MT.YttriumBariumCuprate, 3), IL.Electric_Pump_LV.get(2)), FL.array(FL.Hydrogen.make(2000)), ZL_FS, wireGt01.mat(MT.Superconductor, 3));
		RM.Welder.addRecipeX(T, 64, 40, ST.array(pipeTiny.mat(MT.TungstenSteel, 1), wireGt01.mat(MT.NiobiumTitanium, 3), IL.Electric_Pump_LV.get(2)), FL.array(FL.Helium.make(2000)), ZL_FS, wireGt01.mat(MT.Superconductor, 3));
		RM_GT6U.Ionizer.addRecipe0(T, 64, 2000, FL.Hydrogen.make(50), FL_GT6U.HydrogenI.make(50), ZL_IS);
		RM_GT6U.Ionizer.addRecipe0(T, 64, 2000, FL.Helium.make(50), FL_GT6U.HeliumI.make(50), ZL_IS);
		RM_GT6U.Well.addRecipe1(T, 16, 1800, ST.tag(1), ZL_FS, MT.H2O.liquid(U*2, F), ZL_IS);
		RM.Extruder.addRecipe2(T, 512, 512, OP.dust.mat(MT.Graphite, 1), IL.Shape_Extruder_Plate.get(0), OP.plate.mat(MT.Graphite, 1));
		RM.Mixer.addRecipe1(T, 16, 200, OP.dust.mat(MT.MeatRaw, 1), FL.Water.make(500), FL_GT6U.AquaticNutrition.make(500), ZL_IS);
		RM.Mixer.addRecipe1(T, 16, 200, OP.dust.mat(MT.MeatRotten, 1), FL.Water.make(500), FL_GT6U.AquaticNutrition.make(500), ZL_IS);
		RM.Centrifuge.addRecipe1(T, 64, 200, new long[] {500}, NI, FL_GT6U.AquaticWaste.make(1000), FL.Water.make(1000), ST.make(Blocks.SAND, 1, 0));
		RM_GT6U.AquaticFarm.addRecipe1(T, 256,200, new long[] {1500, 1500, 1500, 1500}, ST.make(Items.COD, 1, 0), FL.Water.make(50), FL_GT6U.AquaticWaste.make(50), ST.array(ST.make(Items.COD, 6, 0), ST.make(Items.COD, 6, 0), ST.make(Items.COD, 6, 0), ST.make(Items.COD, 6, 0)));
		RM_GT6U.AquaticFarm.addRecipe1(T, 256,200, new long[] {1500, 1500, 1500, 1500}, ST.make(Items.COD, 1, 1), FL.Water.make(50), FL_GT6U.AquaticWaste.make(50), ST.array(ST.make(Items.COD, 4, 1), ST.make(Items.COD, 4, 1), ST.make(Items.COD, 4, 1), ST.make(Items.COD, 4, 1)));
		RM_GT6U.AquaticFarm.addRecipe1(T, 256,200, new long[] {1500, 1500, 1500, 1500}, ST.make(Items.COD, 1, 2), FL.Water.make(50), FL_GT6U.AquaticWaste.make(50), ST.array(ST.make(Items.COD, 4, 2), ST.make(Items.COD, 4, 2), ST.make(Items.COD, 4, 2), ST.make(Items.COD, 4, 2)));
		RM_GT6U.AquaticFarm.addRecipe1(T, 256,200, new long[] {1500, 1500, 1500, 1500}, ST.make(Items.COD, 1, 3), FL.Water.make(50), FL_GT6U.AquaticWaste.make(50), ST.array(ST.make(Items.COD, 3, 3), ST.make(Items.COD, 3, 3), ST.make(Items.COD, 3, 3), ST.make(Items.COD, 3, 3)));
		RM_GT6U.AquaticFarm.addRecipe1(T, 256,200, new long[] {3000, 3000, 3000, 3000}, ST.make(Items.COD, 1, 0), FL_GT6U.AquaticNutrition.make(50), FL_GT6U.AquaticWaste.make(50), ST.array(ST.make(Items.COD, 6, 0), ST.make(Items.COD, 6, 0), ST.make(Items.COD, 6, 0), ST.make(Items.COD, 6, 0)));
		RM_GT6U.AquaticFarm.addRecipe1(T, 256,200, new long[] {3000, 3000, 3000, 3000}, ST.make(Items.COD, 1, 1), FL_GT6U.AquaticNutrition.make(50), FL_GT6U.AquaticWaste.make(50), ST.array(ST.make(Items.COD, 4, 1), ST.make(Items.COD, 4, 1), ST.make(Items.COD, 4, 1), ST.make(Items.COD, 4, 1)));
		RM_GT6U.AquaticFarm.addRecipe1(T, 256,200, new long[] {3000, 3000, 3000, 3000}, ST.make(Items.COD, 1, 2), FL_GT6U.AquaticNutrition.make(50), FL_GT6U.AquaticWaste.make(50), ST.array(ST.make(Items.COD, 4, 2), ST.make(Items.COD, 4, 2), ST.make(Items.COD, 4, 2), ST.make(Items.COD, 4, 2)));
		RM_GT6U.AquaticFarm.addRecipe1(T, 256,200, new long[] {3000, 3000, 3000, 3000}, ST.make(Items.COD, 1, 3), FL_GT6U.AquaticNutrition.make(50), FL_GT6U.AquaticWaste.make(50), ST.array(ST.make(Items.COD, 3, 3), ST.make(Items.COD, 3, 3), ST.make(Items.COD, 3, 3), ST.make(Items.COD, 3, 3)));
	}

	/** GT6 statements GT6U put a statement of run_run ahead of: what they make gives way to it. */
	static void later_run_0() {
		for (ItemStack tTNT : ST.array(ST.make(Blocks.TNT, 8, W), IL.IC2_ITNT.get(4), IL.Boomstick.get(12), IL.Dynamite.get(2), IL.Dynamite_Strong.get(1))) if (ST.valid(tTNT)) {
			for (OreDictMaterial tMat : ANY.Emerald.mToThis) {
				OreDictMaterial tOutput = MT.Emerald;
				RM.ImplosionCompressor.addRecipeX(T, 0, 256, ST.array(dust.mat(tMat, 1), tTNT , ST.tag(0)), plateGem .mat(tOutput, 1));
				RM.ImplosionCompressor.addRecipeX(T, 0, 256, ST.array(dust.mat(tMat, 1), tTNT , ST.tag(1)), gem .mat(tOutput, 1));
				RM.ImplosionCompressor.addRecipeX(T, 0, 256, ST.array(dust.mat(tMat, 2), ST.mul(4, tTNT), ST.tag(2)), gemFlawless .mat(tOutput, 1));
				RM.ImplosionCompressor.addRecipeX(T, 0, 256, ST.array(dust.mat(tMat, 4), ST.mul(8, tTNT), ST.tag(3)), gemExquisite.mat(tOutput, 1));
			}
			for (OreDictMaterial tMat : ANY.Amethyst.mToThis) {
				OreDictMaterial tOutput = tMat;
				RM.ImplosionCompressor.addRecipeX(T, 0, 256, ST.array(dust.mat(tMat, 1), tTNT , ST.tag(0)), plateGem .mat(tOutput, 1));
				RM.ImplosionCompressor.addRecipeX(T, 0, 256, ST.array(dust.mat(tMat, 1), tTNT , ST.tag(1)), gem .mat(tOutput, 1));
				RM.ImplosionCompressor.addRecipeX(T, 0, 256, ST.array(dust.mat(tMat, 2), ST.mul(4, tTNT), ST.tag(2)), gemFlawless .mat(tOutput, 1));
				RM.ImplosionCompressor.addRecipeX(T, 0, 256, ST.array(dust.mat(tMat, 4), ST.mul(8, tTNT), ST.tag(3)), gemExquisite.mat(tOutput, 1));
			}
			for (OreDictMaterial tMat : new OreDictMaterial[] {MT.Spinel, MT.BalasRuby, MT.Topaz, MT.BlueTopaz, MT.Tanzanite, MT.Amazonite, MT.Alexandrite, MT.Opal, MT.OnyxRed, MT.OnyxBlack, MT.Olivine, MT.Dioptase, MT.Craponite}) {
				OreDictMaterial tOutput = tMat;
				RM.ImplosionCompressor.addRecipeX(T, 0, 256, ST.array(dust.mat(tMat, 1), tTNT , ST.tag(0)), plateGem .mat(tOutput, 1));
				RM.ImplosionCompressor.addRecipeX(T, 0, 256, ST.array(dust.mat(tMat, 1), tTNT , ST.tag(1)), gem .mat(tOutput, 1));
				RM.ImplosionCompressor.addRecipeX(T, 0, 256, ST.array(dust.mat(tMat, 2), ST.mul(4, tTNT), ST.tag(2)), gemFlawless .mat(tOutput, 1));
				RM.ImplosionCompressor.addRecipeX(T, 0, 256, ST.array(dust.mat(tMat, 4), ST.mul(8, tTNT), ST.tag(3)), gemExquisite.mat(tOutput, 1));
			}
		}
	}

	/** GT6 statements GT6U put a statement of run_run ahead of: what they make gives way to it. */
	static void later_run_1() {
		for (ItemStack tTNT : ST.array(ST.make(Blocks.TNT, 8, W), IL.IC2_ITNT.get(4), IL.Boomstick.get(12), IL.Dynamite.get(2), IL.Dynamite_Strong.get(1))) if (ST.valid(tTNT)) {
			for (OreDictMaterial tMat : ANY.Emerald.mToThis) {
				OreDictMaterial tOutput = MT.Emerald;
				RM.ImplosionCompressor.addRecipeX(T, 0, 256, ST.array(dust.mat(tMat, 1), tTNT , ST.tag(0)), plateGem .mat(tOutput, 1));
				RM.ImplosionCompressor.addRecipeX(T, 0, 256, ST.array(dust.mat(tMat, 1), tTNT , ST.tag(1)), gem .mat(tOutput, 1));
				RM.ImplosionCompressor.addRecipeX(T, 0, 256, ST.array(dust.mat(tMat, 2), ST.mul(4, tTNT), ST.tag(2)), gemFlawless .mat(tOutput, 1));
				RM.ImplosionCompressor.addRecipeX(T, 0, 256, ST.array(dust.mat(tMat, 4), ST.mul(8, tTNT), ST.tag(3)), gemExquisite.mat(tOutput, 1));
			}
			for (OreDictMaterial tMat : ANY.Amethyst.mToThis) {
				OreDictMaterial tOutput = tMat;
				RM.ImplosionCompressor.addRecipeX(T, 0, 256, ST.array(dust.mat(tMat, 1), tTNT , ST.tag(0)), plateGem .mat(tOutput, 1));
				RM.ImplosionCompressor.addRecipeX(T, 0, 256, ST.array(dust.mat(tMat, 1), tTNT , ST.tag(1)), gem .mat(tOutput, 1));
				RM.ImplosionCompressor.addRecipeX(T, 0, 256, ST.array(dust.mat(tMat, 2), ST.mul(4, tTNT), ST.tag(2)), gemFlawless .mat(tOutput, 1));
				RM.ImplosionCompressor.addRecipeX(T, 0, 256, ST.array(dust.mat(tMat, 4), ST.mul(8, tTNT), ST.tag(3)), gemExquisite.mat(tOutput, 1));
			}
			for (OreDictMaterial tMat : new OreDictMaterial[] {MT.Spinel, MT.BalasRuby, MT.Topaz, MT.BlueTopaz, MT.Tanzanite, MT.Amazonite, MT.Alexandrite, MT.Opal, MT.OnyxRed, MT.OnyxBlack, MT.Olivine, MT.Dioptase, MT.Craponite}) {
				OreDictMaterial tOutput = tMat;
				RM.ImplosionCompressor.addRecipeX(T, 0, 256, ST.array(dust.mat(tMat, 1), tTNT , ST.tag(0)), plateGem .mat(tOutput, 1));
				RM.ImplosionCompressor.addRecipeX(T, 0, 256, ST.array(dust.mat(tMat, 1), tTNT , ST.tag(1)), gem .mat(tOutput, 1));
				RM.ImplosionCompressor.addRecipeX(T, 0, 256, ST.array(dust.mat(tMat, 2), ST.mul(4, tTNT), ST.tag(2)), gemFlawless .mat(tOutput, 1));
				RM.ImplosionCompressor.addRecipeX(T, 0, 256, ST.array(dust.mat(tMat, 4), ST.mul(8, tTNT), ST.tag(3)), gemExquisite.mat(tOutput, 1));
			}
		}
	}

	/** GT6 statements GT6U put a statement of run_run ahead of: what they make gives way to it. */
	static void later_run_2() {
		for (ItemStack tTNT : ST.array(ST.make(Blocks.TNT, 8, W), IL.IC2_ITNT.get(4), IL.Boomstick.get(12), IL.Dynamite.get(2), IL.Dynamite_Strong.get(1))) if (ST.valid(tTNT)) {
			for (OreDictMaterial tMat : ANY.Emerald.mToThis) {
				OreDictMaterial tOutput = MT.Emerald;
				RM.ImplosionCompressor.addRecipeX(T, 0, 256, ST.array(dust.mat(tMat, 1), tTNT , ST.tag(0)), plateGem .mat(tOutput, 1));
				RM.ImplosionCompressor.addRecipeX(T, 0, 256, ST.array(dust.mat(tMat, 1), tTNT , ST.tag(1)), gem .mat(tOutput, 1));
				RM.ImplosionCompressor.addRecipeX(T, 0, 256, ST.array(dust.mat(tMat, 2), ST.mul(4, tTNT), ST.tag(2)), gemFlawless .mat(tOutput, 1));
				RM.ImplosionCompressor.addRecipeX(T, 0, 256, ST.array(dust.mat(tMat, 4), ST.mul(8, tTNT), ST.tag(3)), gemExquisite.mat(tOutput, 1));
			}
			for (OreDictMaterial tMat : ANY.Amethyst.mToThis) {
				OreDictMaterial tOutput = tMat;
				RM.ImplosionCompressor.addRecipeX(T, 0, 256, ST.array(dust.mat(tMat, 1), tTNT , ST.tag(0)), plateGem .mat(tOutput, 1));
				RM.ImplosionCompressor.addRecipeX(T, 0, 256, ST.array(dust.mat(tMat, 1), tTNT , ST.tag(1)), gem .mat(tOutput, 1));
				RM.ImplosionCompressor.addRecipeX(T, 0, 256, ST.array(dust.mat(tMat, 2), ST.mul(4, tTNT), ST.tag(2)), gemFlawless .mat(tOutput, 1));
				RM.ImplosionCompressor.addRecipeX(T, 0, 256, ST.array(dust.mat(tMat, 4), ST.mul(8, tTNT), ST.tag(3)), gemExquisite.mat(tOutput, 1));
			}
			for (OreDictMaterial tMat : new OreDictMaterial[] {MT.Spinel, MT.BalasRuby, MT.Topaz, MT.BlueTopaz, MT.Tanzanite, MT.Amazonite, MT.Alexandrite, MT.Opal, MT.OnyxRed, MT.OnyxBlack, MT.Olivine, MT.Dioptase, MT.Craponite}) {
				OreDictMaterial tOutput = tMat;
				RM.ImplosionCompressor.addRecipeX(T, 0, 256, ST.array(dust.mat(tMat, 1), tTNT , ST.tag(0)), plateGem .mat(tOutput, 1));
				RM.ImplosionCompressor.addRecipeX(T, 0, 256, ST.array(dust.mat(tMat, 1), tTNT , ST.tag(1)), gem .mat(tOutput, 1));
				RM.ImplosionCompressor.addRecipeX(T, 0, 256, ST.array(dust.mat(tMat, 2), ST.mul(4, tTNT), ST.tag(2)), gemFlawless .mat(tOutput, 1));
				RM.ImplosionCompressor.addRecipeX(T, 0, 256, ST.array(dust.mat(tMat, 4), ST.mul(8, tTNT), ST.tag(3)), gemExquisite.mat(tOutput, 1));
			}
		}
	}

	/** GT6 statements GT6U put a statement of run_run ahead of: what they make gives way to it. */
	static void later_run_3() {
		for (ItemStack tTNT : ST.array(ST.make(Blocks.TNT, 8, W), IL.IC2_ITNT.get(4), IL.Boomstick.get(12), IL.Dynamite.get(2), IL.Dynamite_Strong.get(1))) if (ST.valid(tTNT)) {
			for (OreDictMaterial tMat : ANY.Emerald.mToThis) {
				OreDictMaterial tOutput = MT.Emerald;
				RM.ImplosionCompressor.addRecipeX(T, 0, 256, ST.array(dust.mat(tMat, 1), tTNT , ST.tag(0)), plateGem .mat(tOutput, 1));
				RM.ImplosionCompressor.addRecipeX(T, 0, 256, ST.array(dust.mat(tMat, 1), tTNT , ST.tag(1)), gem .mat(tOutput, 1));
				RM.ImplosionCompressor.addRecipeX(T, 0, 256, ST.array(dust.mat(tMat, 2), ST.mul(4, tTNT), ST.tag(2)), gemFlawless .mat(tOutput, 1));
				RM.ImplosionCompressor.addRecipeX(T, 0, 256, ST.array(dust.mat(tMat, 4), ST.mul(8, tTNT), ST.tag(3)), gemExquisite.mat(tOutput, 1));
			}
			for (OreDictMaterial tMat : ANY.Amethyst.mToThis) {
				OreDictMaterial tOutput = tMat;
				RM.ImplosionCompressor.addRecipeX(T, 0, 256, ST.array(dust.mat(tMat, 1), tTNT , ST.tag(0)), plateGem .mat(tOutput, 1));
				RM.ImplosionCompressor.addRecipeX(T, 0, 256, ST.array(dust.mat(tMat, 1), tTNT , ST.tag(1)), gem .mat(tOutput, 1));
				RM.ImplosionCompressor.addRecipeX(T, 0, 256, ST.array(dust.mat(tMat, 2), ST.mul(4, tTNT), ST.tag(2)), gemFlawless .mat(tOutput, 1));
				RM.ImplosionCompressor.addRecipeX(T, 0, 256, ST.array(dust.mat(tMat, 4), ST.mul(8, tTNT), ST.tag(3)), gemExquisite.mat(tOutput, 1));
			}
			for (OreDictMaterial tMat : new OreDictMaterial[] {MT.Spinel, MT.BalasRuby, MT.Topaz, MT.BlueTopaz, MT.Tanzanite, MT.Amazonite, MT.Alexandrite, MT.Opal, MT.OnyxRed, MT.OnyxBlack, MT.Olivine, MT.Dioptase, MT.Craponite}) {
				OreDictMaterial tOutput = tMat;
				RM.ImplosionCompressor.addRecipeX(T, 0, 256, ST.array(dust.mat(tMat, 1), tTNT , ST.tag(0)), plateGem .mat(tOutput, 1));
				RM.ImplosionCompressor.addRecipeX(T, 0, 256, ST.array(dust.mat(tMat, 1), tTNT , ST.tag(1)), gem .mat(tOutput, 1));
				RM.ImplosionCompressor.addRecipeX(T, 0, 256, ST.array(dust.mat(tMat, 2), ST.mul(4, tTNT), ST.tag(2)), gemFlawless .mat(tOutput, 1));
				RM.ImplosionCompressor.addRecipeX(T, 0, 256, ST.array(dust.mat(tMat, 4), ST.mul(8, tTNT), ST.tag(3)), gemExquisite.mat(tOutput, 1));
			}
		}
	}

	/** GT6 statements GT6U put a statement of run_run ahead of: what they make gives way to it. */
	static void later_run_4() {
		for (ItemStack tTNT : ST.array(ST.make(Blocks.TNT, 8, W), IL.IC2_ITNT.get(4), IL.Boomstick.get(12), IL.Dynamite.get(2), IL.Dynamite_Strong.get(1))) if (ST.valid(tTNT)) {
			for (OreDictMaterial tMat : ANY.Emerald.mToThis) {
				OreDictMaterial tOutput = MT.Emerald;
				RM.ImplosionCompressor.addRecipeX(T, 0, 256, ST.array(dust.mat(tMat, 1), tTNT , ST.tag(0)), plateGem .mat(tOutput, 1));
				RM.ImplosionCompressor.addRecipeX(T, 0, 256, ST.array(dust.mat(tMat, 1), tTNT , ST.tag(1)), gem .mat(tOutput, 1));
				RM.ImplosionCompressor.addRecipeX(T, 0, 256, ST.array(dust.mat(tMat, 2), ST.mul(4, tTNT), ST.tag(2)), gemFlawless .mat(tOutput, 1));
				RM.ImplosionCompressor.addRecipeX(T, 0, 256, ST.array(dust.mat(tMat, 4), ST.mul(8, tTNT), ST.tag(3)), gemExquisite.mat(tOutput, 1));
			}
			for (OreDictMaterial tMat : ANY.Amethyst.mToThis) {
				OreDictMaterial tOutput = tMat;
				RM.ImplosionCompressor.addRecipeX(T, 0, 256, ST.array(dust.mat(tMat, 1), tTNT , ST.tag(0)), plateGem .mat(tOutput, 1));
				RM.ImplosionCompressor.addRecipeX(T, 0, 256, ST.array(dust.mat(tMat, 1), tTNT , ST.tag(1)), gem .mat(tOutput, 1));
				RM.ImplosionCompressor.addRecipeX(T, 0, 256, ST.array(dust.mat(tMat, 2), ST.mul(4, tTNT), ST.tag(2)), gemFlawless .mat(tOutput, 1));
				RM.ImplosionCompressor.addRecipeX(T, 0, 256, ST.array(dust.mat(tMat, 4), ST.mul(8, tTNT), ST.tag(3)), gemExquisite.mat(tOutput, 1));
			}
			for (OreDictMaterial tMat : new OreDictMaterial[] {MT.Spinel, MT.BalasRuby, MT.Topaz, MT.BlueTopaz, MT.Tanzanite, MT.Amazonite, MT.Alexandrite, MT.Opal, MT.OnyxRed, MT.OnyxBlack, MT.Olivine, MT.Dioptase, MT.Craponite}) {
				OreDictMaterial tOutput = tMat;
				RM.ImplosionCompressor.addRecipeX(T, 0, 256, ST.array(dust.mat(tMat, 1), tTNT , ST.tag(0)), plateGem .mat(tOutput, 1));
				RM.ImplosionCompressor.addRecipeX(T, 0, 256, ST.array(dust.mat(tMat, 1), tTNT , ST.tag(1)), gem .mat(tOutput, 1));
				RM.ImplosionCompressor.addRecipeX(T, 0, 256, ST.array(dust.mat(tMat, 2), ST.mul(4, tTNT), ST.tag(2)), gemFlawless .mat(tOutput, 1));
				RM.ImplosionCompressor.addRecipeX(T, 0, 256, ST.array(dust.mat(tMat, 4), ST.mul(8, tTNT), ST.tag(3)), gemExquisite.mat(tOutput, 1));
			}
		}
	}

	/** GT6 statements GT6U put a statement of run_run ahead of: what they make gives way to it. */
	static void later_run_5() {
		for (ItemStack tTNT : ST.array(ST.make(Blocks.TNT, 8, W), IL.IC2_ITNT.get(4), IL.Boomstick.get(12), IL.Dynamite.get(2), IL.Dynamite_Strong.get(1))) if (ST.valid(tTNT)) {
			for (OreDictMaterial tMat : ANY.Emerald.mToThis) {
				OreDictMaterial tOutput = MT.Emerald;
				RM.ImplosionCompressor.addRecipeX(T, 0, 256, ST.array(dust.mat(tMat, 1), tTNT , ST.tag(0)), plateGem .mat(tOutput, 1));
				RM.ImplosionCompressor.addRecipeX(T, 0, 256, ST.array(dust.mat(tMat, 1), tTNT , ST.tag(1)), gem .mat(tOutput, 1));
				RM.ImplosionCompressor.addRecipeX(T, 0, 256, ST.array(dust.mat(tMat, 2), ST.mul(4, tTNT), ST.tag(2)), gemFlawless .mat(tOutput, 1));
				RM.ImplosionCompressor.addRecipeX(T, 0, 256, ST.array(dust.mat(tMat, 4), ST.mul(8, tTNT), ST.tag(3)), gemExquisite.mat(tOutput, 1));
			}
			for (OreDictMaterial tMat : ANY.Amethyst.mToThis) {
				OreDictMaterial tOutput = tMat;
				RM.ImplosionCompressor.addRecipeX(T, 0, 256, ST.array(dust.mat(tMat, 1), tTNT , ST.tag(0)), plateGem .mat(tOutput, 1));
				RM.ImplosionCompressor.addRecipeX(T, 0, 256, ST.array(dust.mat(tMat, 1), tTNT , ST.tag(1)), gem .mat(tOutput, 1));
				RM.ImplosionCompressor.addRecipeX(T, 0, 256, ST.array(dust.mat(tMat, 2), ST.mul(4, tTNT), ST.tag(2)), gemFlawless .mat(tOutput, 1));
				RM.ImplosionCompressor.addRecipeX(T, 0, 256, ST.array(dust.mat(tMat, 4), ST.mul(8, tTNT), ST.tag(3)), gemExquisite.mat(tOutput, 1));
			}
			for (OreDictMaterial tMat : new OreDictMaterial[] {MT.Spinel, MT.BalasRuby, MT.Topaz, MT.BlueTopaz, MT.Tanzanite, MT.Amazonite, MT.Alexandrite, MT.Opal, MT.OnyxRed, MT.OnyxBlack, MT.Olivine, MT.Dioptase, MT.Craponite}) {
				OreDictMaterial tOutput = tMat;
				RM.ImplosionCompressor.addRecipeX(T, 0, 256, ST.array(dust.mat(tMat, 1), tTNT , ST.tag(0)), plateGem .mat(tOutput, 1));
				RM.ImplosionCompressor.addRecipeX(T, 0, 256, ST.array(dust.mat(tMat, 1), tTNT , ST.tag(1)), gem .mat(tOutput, 1));
				RM.ImplosionCompressor.addRecipeX(T, 0, 256, ST.array(dust.mat(tMat, 2), ST.mul(4, tTNT), ST.tag(2)), gemFlawless .mat(tOutput, 1));
				RM.ImplosionCompressor.addRecipeX(T, 0, 256, ST.array(dust.mat(tMat, 4), ST.mul(8, tTNT), ST.tag(3)), gemExquisite.mat(tOutput, 1));
			}
		}
	}

	/** GT6 statements GT6U put a statement of run_run ahead of: what they make gives way to it. */
	static void later_run_6() {
		for (ItemStack tTNT : ST.array(ST.make(Blocks.TNT, 8, W), IL.IC2_ITNT.get(4), IL.Boomstick.get(12), IL.Dynamite.get(2), IL.Dynamite_Strong.get(1))) if (ST.valid(tTNT)) {
			for (OreDictMaterial tMat : ANY.Emerald.mToThis) {
				OreDictMaterial tOutput = MT.Emerald;
				RM.ImplosionCompressor.addRecipeX(T, 0, 256, ST.array(dust.mat(tMat, 1), tTNT , ST.tag(0)), plateGem .mat(tOutput, 1));
				RM.ImplosionCompressor.addRecipeX(T, 0, 256, ST.array(dust.mat(tMat, 1), tTNT , ST.tag(1)), gem .mat(tOutput, 1));
				RM.ImplosionCompressor.addRecipeX(T, 0, 256, ST.array(dust.mat(tMat, 2), ST.mul(4, tTNT), ST.tag(2)), gemFlawless .mat(tOutput, 1));
				RM.ImplosionCompressor.addRecipeX(T, 0, 256, ST.array(dust.mat(tMat, 4), ST.mul(8, tTNT), ST.tag(3)), gemExquisite.mat(tOutput, 1));
			}
			for (OreDictMaterial tMat : ANY.Amethyst.mToThis) {
				OreDictMaterial tOutput = tMat;
				RM.ImplosionCompressor.addRecipeX(T, 0, 256, ST.array(dust.mat(tMat, 1), tTNT , ST.tag(0)), plateGem .mat(tOutput, 1));
				RM.ImplosionCompressor.addRecipeX(T, 0, 256, ST.array(dust.mat(tMat, 1), tTNT , ST.tag(1)), gem .mat(tOutput, 1));
				RM.ImplosionCompressor.addRecipeX(T, 0, 256, ST.array(dust.mat(tMat, 2), ST.mul(4, tTNT), ST.tag(2)), gemFlawless .mat(tOutput, 1));
				RM.ImplosionCompressor.addRecipeX(T, 0, 256, ST.array(dust.mat(tMat, 4), ST.mul(8, tTNT), ST.tag(3)), gemExquisite.mat(tOutput, 1));
			}
			for (OreDictMaterial tMat : new OreDictMaterial[] {MT.Spinel, MT.BalasRuby, MT.Topaz, MT.BlueTopaz, MT.Tanzanite, MT.Amazonite, MT.Alexandrite, MT.Opal, MT.OnyxRed, MT.OnyxBlack, MT.Olivine, MT.Dioptase, MT.Craponite}) {
				OreDictMaterial tOutput = tMat;
				RM.ImplosionCompressor.addRecipeX(T, 0, 256, ST.array(dust.mat(tMat, 1), tTNT , ST.tag(0)), plateGem .mat(tOutput, 1));
				RM.ImplosionCompressor.addRecipeX(T, 0, 256, ST.array(dust.mat(tMat, 1), tTNT , ST.tag(1)), gem .mat(tOutput, 1));
				RM.ImplosionCompressor.addRecipeX(T, 0, 256, ST.array(dust.mat(tMat, 2), ST.mul(4, tTNT), ST.tag(2)), gemFlawless .mat(tOutput, 1));
				RM.ImplosionCompressor.addRecipeX(T, 0, 256, ST.array(dust.mat(tMat, 4), ST.mul(8, tTNT), ST.tag(3)), gemExquisite.mat(tOutput, 1));
			}
		}
	}

	/** GT6 statements GT6U put a statement of run_run ahead of: what they make gives way to it. */
	static void later_run_7() {
		for (ItemStack tTNT : ST.array(ST.make(Blocks.TNT, 8, W), IL.IC2_ITNT.get(4), IL.Boomstick.get(12), IL.Dynamite.get(2), IL.Dynamite_Strong.get(1))) if (ST.valid(tTNT)) {
			for (OreDictMaterial tMat : ANY.Emerald.mToThis) {
				OreDictMaterial tOutput = MT.Emerald;
				RM.ImplosionCompressor.addRecipeX(T, 0, 256, ST.array(dust.mat(tMat, 1), tTNT , ST.tag(0)), plateGem .mat(tOutput, 1));
				RM.ImplosionCompressor.addRecipeX(T, 0, 256, ST.array(dust.mat(tMat, 1), tTNT , ST.tag(1)), gem .mat(tOutput, 1));
				RM.ImplosionCompressor.addRecipeX(T, 0, 256, ST.array(dust.mat(tMat, 2), ST.mul(4, tTNT), ST.tag(2)), gemFlawless .mat(tOutput, 1));
				RM.ImplosionCompressor.addRecipeX(T, 0, 256, ST.array(dust.mat(tMat, 4), ST.mul(8, tTNT), ST.tag(3)), gemExquisite.mat(tOutput, 1));
			}
			for (OreDictMaterial tMat : ANY.Amethyst.mToThis) {
				OreDictMaterial tOutput = tMat;
				RM.ImplosionCompressor.addRecipeX(T, 0, 256, ST.array(dust.mat(tMat, 1), tTNT , ST.tag(0)), plateGem .mat(tOutput, 1));
				RM.ImplosionCompressor.addRecipeX(T, 0, 256, ST.array(dust.mat(tMat, 1), tTNT , ST.tag(1)), gem .mat(tOutput, 1));
				RM.ImplosionCompressor.addRecipeX(T, 0, 256, ST.array(dust.mat(tMat, 2), ST.mul(4, tTNT), ST.tag(2)), gemFlawless .mat(tOutput, 1));
				RM.ImplosionCompressor.addRecipeX(T, 0, 256, ST.array(dust.mat(tMat, 4), ST.mul(8, tTNT), ST.tag(3)), gemExquisite.mat(tOutput, 1));
			}
			for (OreDictMaterial tMat : new OreDictMaterial[] {MT.Spinel, MT.BalasRuby, MT.Topaz, MT.BlueTopaz, MT.Tanzanite, MT.Amazonite, MT.Alexandrite, MT.Opal, MT.OnyxRed, MT.OnyxBlack, MT.Olivine, MT.Dioptase, MT.Craponite}) {
				OreDictMaterial tOutput = tMat;
				RM.ImplosionCompressor.addRecipeX(T, 0, 256, ST.array(dust.mat(tMat, 1), tTNT , ST.tag(0)), plateGem .mat(tOutput, 1));
				RM.ImplosionCompressor.addRecipeX(T, 0, 256, ST.array(dust.mat(tMat, 1), tTNT , ST.tag(1)), gem .mat(tOutput, 1));
				RM.ImplosionCompressor.addRecipeX(T, 0, 256, ST.array(dust.mat(tMat, 2), ST.mul(4, tTNT), ST.tag(2)), gemFlawless .mat(tOutput, 1));
				RM.ImplosionCompressor.addRecipeX(T, 0, 256, ST.array(dust.mat(tMat, 4), ST.mul(8, tTNT), ST.tag(3)), gemExquisite.mat(tOutput, 1));
			}
		}
	}

	/** GT6 statements GT6U put a statement of run_run ahead of: what they make gives way to it. */
	static void later_run_8() {
		for (ItemStack tTNT : ST.array(ST.make(Blocks.TNT, 8, W), IL.IC2_ITNT.get(4), IL.Boomstick.get(12), IL.Dynamite.get(2), IL.Dynamite_Strong.get(1))) if (ST.valid(tTNT)) {
			for (OreDictMaterial tMat : new OreDictMaterial[] {MT.Spinel, MT.BalasRuby, MT.Topaz, MT.BlueTopaz, MT.Tanzanite, MT.Amazonite, MT.Alexandrite, MT.Opal, MT.OnyxRed, MT.OnyxBlack, MT.Olivine, MT.Dioptase, MT.Craponite}) {
				OreDictMaterial tOutput = tMat;
				RM.ImplosionCompressor.addRecipeX(T, 0, 256, ST.array(dust.mat(tMat, 1), tTNT , ST.tag(0)), plateGem .mat(tOutput, 1));
				RM.ImplosionCompressor.addRecipeX(T, 0, 256, ST.array(dust.mat(tMat, 1), tTNT , ST.tag(1)), gem .mat(tOutput, 1));
				RM.ImplosionCompressor.addRecipeX(T, 0, 256, ST.array(dust.mat(tMat, 2), ST.mul(4, tTNT), ST.tag(2)), gemFlawless .mat(tOutput, 1));
				RM.ImplosionCompressor.addRecipeX(T, 0, 256, ST.array(dust.mat(tMat, 4), ST.mul(8, tTNT), ST.tag(3)), gemExquisite.mat(tOutput, 1));
			}
		}
	}

	/** GT6 statements GT6U put a statement of run_run ahead of: what they make gives way to it. */
	static void later_run_9() {
		for (ItemStack tTNT : ST.array(ST.make(Blocks.TNT, 8, W), IL.IC2_ITNT.get(4), IL.Boomstick.get(12), IL.Dynamite.get(2), IL.Dynamite_Strong.get(1))) if (ST.valid(tTNT)) {
			for (OreDictMaterial tMat : new OreDictMaterial[] {MT.Spinel, MT.BalasRuby, MT.Topaz, MT.BlueTopaz, MT.Tanzanite, MT.Amazonite, MT.Alexandrite, MT.Opal, MT.OnyxRed, MT.OnyxBlack, MT.Olivine, MT.Dioptase, MT.Craponite}) {
				OreDictMaterial tOutput = tMat;
				RM.ImplosionCompressor.addRecipeX(T, 0, 256, ST.array(dust.mat(tMat, 1), tTNT , ST.tag(0)), plateGem .mat(tOutput, 1));
				RM.ImplosionCompressor.addRecipeX(T, 0, 256, ST.array(dust.mat(tMat, 1), tTNT , ST.tag(1)), gem .mat(tOutput, 1));
				RM.ImplosionCompressor.addRecipeX(T, 0, 256, ST.array(dust.mat(tMat, 2), ST.mul(4, tTNT), ST.tag(2)), gemFlawless .mat(tOutput, 1));
				RM.ImplosionCompressor.addRecipeX(T, 0, 256, ST.array(dust.mat(tMat, 4), ST.mul(8, tTNT), ST.tag(3)), gemExquisite.mat(tOutput, 1));
			}
		}
	}

	/** GT6 statements GT6U put a statement of run_run ahead of: what they make gives way to it. */
	static void later_run_10() {
		for (ItemStack tTNT : ST.array(ST.make(Blocks.TNT, 8, W), IL.IC2_ITNT.get(4), IL.Boomstick.get(12), IL.Dynamite.get(2), IL.Dynamite_Strong.get(1))) if (ST.valid(tTNT)) {
			for (OreDictMaterial tMat : new OreDictMaterial[] {MT.Spinel, MT.BalasRuby, MT.Topaz, MT.BlueTopaz, MT.Tanzanite, MT.Amazonite, MT.Alexandrite, MT.Opal, MT.OnyxRed, MT.OnyxBlack, MT.Olivine, MT.Dioptase, MT.Craponite}) {
				OreDictMaterial tOutput = tMat;
				RM.ImplosionCompressor.addRecipeX(T, 0, 256, ST.array(dust.mat(tMat, 1), tTNT , ST.tag(0)), plateGem .mat(tOutput, 1));
				RM.ImplosionCompressor.addRecipeX(T, 0, 256, ST.array(dust.mat(tMat, 1), tTNT , ST.tag(1)), gem .mat(tOutput, 1));
				RM.ImplosionCompressor.addRecipeX(T, 0, 256, ST.array(dust.mat(tMat, 2), ST.mul(4, tTNT), ST.tag(2)), gemFlawless .mat(tOutput, 1));
				RM.ImplosionCompressor.addRecipeX(T, 0, 256, ST.array(dust.mat(tMat, 4), ST.mul(8, tTNT), ST.tag(3)), gemExquisite.mat(tOutput, 1));
			}
		}
	}

	/** GT6 statements GT6U put a statement of run_run ahead of: what they make gives way to it. */
	static void later_run_11() {
		for (ItemStack tTNT : ST.array(ST.make(Blocks.TNT, 8, W), IL.IC2_ITNT.get(4), IL.Boomstick.get(12), IL.Dynamite.get(2), IL.Dynamite_Strong.get(1))) if (ST.valid(tTNT)) {
			for (OreDictMaterial tMat : new OreDictMaterial[] {MT.Spinel, MT.BalasRuby, MT.Topaz, MT.BlueTopaz, MT.Tanzanite, MT.Amazonite, MT.Alexandrite, MT.Opal, MT.OnyxRed, MT.OnyxBlack, MT.Olivine, MT.Dioptase, MT.Craponite}) {
				OreDictMaterial tOutput = tMat;
				RM.ImplosionCompressor.addRecipeX(T, 0, 256, ST.array(dust.mat(tMat, 1), tTNT , ST.tag(0)), plateGem .mat(tOutput, 1));
				RM.ImplosionCompressor.addRecipeX(T, 0, 256, ST.array(dust.mat(tMat, 1), tTNT , ST.tag(1)), gem .mat(tOutput, 1));
				RM.ImplosionCompressor.addRecipeX(T, 0, 256, ST.array(dust.mat(tMat, 2), ST.mul(4, tTNT), ST.tag(2)), gemFlawless .mat(tOutput, 1));
				RM.ImplosionCompressor.addRecipeX(T, 0, 256, ST.array(dust.mat(tMat, 4), ST.mul(8, tTNT), ST.tag(3)), gemExquisite.mat(tOutput, 1));
			}
		}
	}

	/** GT6 statements GT6U put a statement of run_run ahead of: what they make gives way to it. */
	static void later_run_12() {
		for (ItemStack tTNT : ST.array(ST.make(Blocks.TNT, 8, W), IL.IC2_ITNT.get(4), IL.Boomstick.get(12), IL.Dynamite.get(2), IL.Dynamite_Strong.get(1))) if (ST.valid(tTNT)) {
			for (OreDictMaterial tMat : new OreDictMaterial[] {MT.Spinel, MT.BalasRuby, MT.Topaz, MT.BlueTopaz, MT.Tanzanite, MT.Amazonite, MT.Alexandrite, MT.Opal, MT.OnyxRed, MT.OnyxBlack, MT.Olivine, MT.Dioptase, MT.Craponite}) {
				OreDictMaterial tOutput = tMat;
				RM.ImplosionCompressor.addRecipeX(T, 0, 256, ST.array(dust.mat(tMat, 1), tTNT , ST.tag(0)), plateGem .mat(tOutput, 1));
				RM.ImplosionCompressor.addRecipeX(T, 0, 256, ST.array(dust.mat(tMat, 1), tTNT , ST.tag(1)), gem .mat(tOutput, 1));
				RM.ImplosionCompressor.addRecipeX(T, 0, 256, ST.array(dust.mat(tMat, 2), ST.mul(4, tTNT), ST.tag(2)), gemFlawless .mat(tOutput, 1));
				RM.ImplosionCompressor.addRecipeX(T, 0, 256, ST.array(dust.mat(tMat, 4), ST.mul(8, tTNT), ST.tag(3)), gemExquisite.mat(tOutput, 1));
			}
		}
	}

	/** GT6 statements GT6U put a statement of run_run ahead of: what they make gives way to it. */
	static void later_run_13() {
		for (ItemStack tTNT : ST.array(ST.make(Blocks.TNT, 8, W), IL.IC2_ITNT.get(4), IL.Boomstick.get(12), IL.Dynamite.get(2), IL.Dynamite_Strong.get(1))) if (ST.valid(tTNT)) {
			for (OreDictMaterial tMat : new OreDictMaterial[] {MT.Spinel, MT.BalasRuby, MT.Topaz, MT.BlueTopaz, MT.Tanzanite, MT.Amazonite, MT.Alexandrite, MT.Opal, MT.OnyxRed, MT.OnyxBlack, MT.Olivine, MT.Dioptase, MT.Craponite}) {
				OreDictMaterial tOutput = tMat;
				RM.ImplosionCompressor.addRecipeX(T, 0, 256, ST.array(dust.mat(tMat, 1), tTNT , ST.tag(0)), plateGem .mat(tOutput, 1));
				RM.ImplosionCompressor.addRecipeX(T, 0, 256, ST.array(dust.mat(tMat, 1), tTNT , ST.tag(1)), gem .mat(tOutput, 1));
				RM.ImplosionCompressor.addRecipeX(T, 0, 256, ST.array(dust.mat(tMat, 2), ST.mul(4, tTNT), ST.tag(2)), gemFlawless .mat(tOutput, 1));
				RM.ImplosionCompressor.addRecipeX(T, 0, 256, ST.array(dust.mat(tMat, 4), ST.mul(8, tTNT), ST.tag(3)), gemExquisite.mat(tOutput, 1));
			}
		}
	}

	/** GT6 statements GT6U put a statement of run_run ahead of: what they make gives way to it. */
	static void later_run_14() {
		for (ItemStack tTNT : ST.array(ST.make(Blocks.TNT, 8, W), IL.IC2_ITNT.get(4), IL.Boomstick.get(12), IL.Dynamite.get(2), IL.Dynamite_Strong.get(1))) if (ST.valid(tTNT)) {
			for (OreDictMaterial tMat : new OreDictMaterial[] {MT.Spinel, MT.BalasRuby, MT.Topaz, MT.BlueTopaz, MT.Tanzanite, MT.Amazonite, MT.Alexandrite, MT.Opal, MT.OnyxRed, MT.OnyxBlack, MT.Olivine, MT.Dioptase, MT.Craponite}) {
				OreDictMaterial tOutput = tMat;
				RM.ImplosionCompressor.addRecipeX(T, 0, 256, ST.array(dust.mat(tMat, 1), tTNT , ST.tag(0)), plateGem .mat(tOutput, 1));
				RM.ImplosionCompressor.addRecipeX(T, 0, 256, ST.array(dust.mat(tMat, 1), tTNT , ST.tag(1)), gem .mat(tOutput, 1));
				RM.ImplosionCompressor.addRecipeX(T, 0, 256, ST.array(dust.mat(tMat, 2), ST.mul(4, tTNT), ST.tag(2)), gemFlawless .mat(tOutput, 1));
				RM.ImplosionCompressor.addRecipeX(T, 0, 256, ST.array(dust.mat(tMat, 4), ST.mul(8, tTNT), ST.tag(3)), gemExquisite.mat(tOutput, 1));
			}
		}
	}

	/** GT6 statements GT6U put a statement of run_run ahead of: what they make gives way to it. */
	static void later_run_15() {
		for (ItemStack tTNT : ST.array(ST.make(Blocks.TNT, 8, W), IL.IC2_ITNT.get(4), IL.Boomstick.get(12), IL.Dynamite.get(2), IL.Dynamite_Strong.get(1))) if (ST.valid(tTNT)) {
			for (OreDictMaterial tMat : new OreDictMaterial[] {MT.Spinel, MT.BalasRuby, MT.Topaz, MT.BlueTopaz, MT.Tanzanite, MT.Amazonite, MT.Alexandrite, MT.Opal, MT.OnyxRed, MT.OnyxBlack, MT.Olivine, MT.Dioptase, MT.Craponite}) {
				OreDictMaterial tOutput = tMat;
				RM.ImplosionCompressor.addRecipeX(T, 0, 256, ST.array(dust.mat(tMat, 1), tTNT , ST.tag(0)), plateGem .mat(tOutput, 1));
				RM.ImplosionCompressor.addRecipeX(T, 0, 256, ST.array(dust.mat(tMat, 1), tTNT , ST.tag(1)), gem .mat(tOutput, 1));
				RM.ImplosionCompressor.addRecipeX(T, 0, 256, ST.array(dust.mat(tMat, 2), ST.mul(4, tTNT), ST.tag(2)), gemFlawless .mat(tOutput, 1));
				RM.ImplosionCompressor.addRecipeX(T, 0, 256, ST.array(dust.mat(tMat, 4), ST.mul(8, tTNT), ST.tag(3)), gemExquisite.mat(tOutput, 1));
			}
		}
	}

	/** GT6 statements GT6U put a statement of run_run ahead of: what they make gives way to it. */
	static void later_run_16() {
		for (ItemStack tTNT : ST.array(ST.make(Blocks.TNT, 8, W), IL.IC2_ITNT.get(4), IL.Boomstick.get(12), IL.Dynamite.get(2), IL.Dynamite_Strong.get(1))) if (ST.valid(tTNT)) {
			for (OreDictMaterial tMat : new OreDictMaterial[] {MT.Spinel, MT.BalasRuby, MT.Topaz, MT.BlueTopaz, MT.Tanzanite, MT.Amazonite, MT.Alexandrite, MT.Opal, MT.OnyxRed, MT.OnyxBlack, MT.Olivine, MT.Dioptase, MT.Craponite}) {
				OreDictMaterial tOutput = tMat;
				RM.ImplosionCompressor.addRecipeX(T, 0, 256, ST.array(dust.mat(tMat, 1), tTNT , ST.tag(0)), plateGem .mat(tOutput, 1));
				RM.ImplosionCompressor.addRecipeX(T, 0, 256, ST.array(dust.mat(tMat, 1), tTNT , ST.tag(1)), gem .mat(tOutput, 1));
				RM.ImplosionCompressor.addRecipeX(T, 0, 256, ST.array(dust.mat(tMat, 2), ST.mul(4, tTNT), ST.tag(2)), gemFlawless .mat(tOutput, 1));
				RM.ImplosionCompressor.addRecipeX(T, 0, 256, ST.array(dust.mat(tMat, 4), ST.mul(8, tTNT), ST.tag(3)), gemExquisite.mat(tOutput, 1));
			}
		}
	}

	/** GT6 statements GT6U put a statement of run_run ahead of: what they make gives way to it. */
	static void later_run_17() {
		for (ItemStack tTNT : ST.array(ST.make(Blocks.TNT, 8, W), IL.IC2_ITNT.get(4), IL.Boomstick.get(12), IL.Dynamite.get(2), IL.Dynamite_Strong.get(1))) if (ST.valid(tTNT)) {
			for (OreDictMaterial tMat : new OreDictMaterial[] {MT.Spinel, MT.BalasRuby, MT.Topaz, MT.BlueTopaz, MT.Tanzanite, MT.Amazonite, MT.Alexandrite, MT.Opal, MT.OnyxRed, MT.OnyxBlack, MT.Olivine, MT.Dioptase, MT.Craponite}) {
				OreDictMaterial tOutput = tMat;
				RM.ImplosionCompressor.addRecipeX(T, 0, 256, ST.array(dust.mat(tMat, 1), tTNT , ST.tag(0)), plateGem .mat(tOutput, 1));
				RM.ImplosionCompressor.addRecipeX(T, 0, 256, ST.array(dust.mat(tMat, 1), tTNT , ST.tag(1)), gem .mat(tOutput, 1));
				RM.ImplosionCompressor.addRecipeX(T, 0, 256, ST.array(dust.mat(tMat, 2), ST.mul(4, tTNT), ST.tag(2)), gemFlawless .mat(tOutput, 1));
				RM.ImplosionCompressor.addRecipeX(T, 0, 256, ST.array(dust.mat(tMat, 4), ST.mul(8, tTNT), ST.tag(3)), gemExquisite.mat(tOutput, 1));
			}
		}
	}

	/** GT6 statements GT6U put a statement of run_run ahead of: what they make gives way to it. */
	static void later_run_18() {
		for (ItemStack tTNT : ST.array(ST.make(Blocks.TNT, 8, W), IL.IC2_ITNT.get(4), IL.Boomstick.get(12), IL.Dynamite.get(2), IL.Dynamite_Strong.get(1))) if (ST.valid(tTNT)) {
			for (OreDictMaterial tMat : new OreDictMaterial[] {MT.Spinel, MT.BalasRuby, MT.Topaz, MT.BlueTopaz, MT.Tanzanite, MT.Amazonite, MT.Alexandrite, MT.Opal, MT.OnyxRed, MT.OnyxBlack, MT.Olivine, MT.Dioptase, MT.Craponite}) {
				OreDictMaterial tOutput = tMat;
				RM.ImplosionCompressor.addRecipeX(T, 0, 256, ST.array(dust.mat(tMat, 1), tTNT , ST.tag(0)), plateGem .mat(tOutput, 1));
				RM.ImplosionCompressor.addRecipeX(T, 0, 256, ST.array(dust.mat(tMat, 1), tTNT , ST.tag(1)), gem .mat(tOutput, 1));
				RM.ImplosionCompressor.addRecipeX(T, 0, 256, ST.array(dust.mat(tMat, 2), ST.mul(4, tTNT), ST.tag(2)), gemFlawless .mat(tOutput, 1));
				RM.ImplosionCompressor.addRecipeX(T, 0, 256, ST.array(dust.mat(tMat, 4), ST.mul(8, tTNT), ST.tag(3)), gemExquisite.mat(tOutput, 1));
			}
		}
	}

	/** GT6 statements GT6U put a statement of run_run ahead of: what they make gives way to it. */
	static void later_run_19() {
		for (ItemStack tTNT : ST.array(ST.make(Blocks.TNT, 8, W), IL.IC2_ITNT.get(4), IL.Boomstick.get(12), IL.Dynamite.get(2), IL.Dynamite_Strong.get(1))) if (ST.valid(tTNT)) {
			for (OreDictMaterial tMat : new OreDictMaterial[] {MT.Spinel, MT.BalasRuby, MT.Topaz, MT.BlueTopaz, MT.Tanzanite, MT.Amazonite, MT.Alexandrite, MT.Opal, MT.OnyxRed, MT.OnyxBlack, MT.Olivine, MT.Dioptase, MT.Craponite}) {
				OreDictMaterial tOutput = tMat;
				RM.ImplosionCompressor.addRecipeX(T, 0, 256, ST.array(dust.mat(tMat, 1), tTNT , ST.tag(0)), plateGem .mat(tOutput, 1));
				RM.ImplosionCompressor.addRecipeX(T, 0, 256, ST.array(dust.mat(tMat, 1), tTNT , ST.tag(1)), gem .mat(tOutput, 1));
				RM.ImplosionCompressor.addRecipeX(T, 0, 256, ST.array(dust.mat(tMat, 2), ST.mul(4, tTNT), ST.tag(2)), gemFlawless .mat(tOutput, 1));
				RM.ImplosionCompressor.addRecipeX(T, 0, 256, ST.array(dust.mat(tMat, 4), ST.mul(8, tTNT), ST.tag(3)), gemExquisite.mat(tOutput, 1));
			}
		}
	}

	/** GT6 statements GT6U put a statement of run_run ahead of: what they make gives way to it. */
	static void later_run_20() {
		for (ItemStack tTNT : ST.array(ST.make(Blocks.TNT, 8, W), IL.IC2_ITNT.get(4), IL.Boomstick.get(12), IL.Dynamite.get(2), IL.Dynamite_Strong.get(1))) if (ST.valid(tTNT)) {
			for (OreDictMaterial tMat : new OreDictMaterial[] {MT.Spinel, MT.BalasRuby, MT.Topaz, MT.BlueTopaz, MT.Tanzanite, MT.Amazonite, MT.Alexandrite, MT.Opal, MT.OnyxRed, MT.OnyxBlack, MT.Olivine, MT.Dioptase, MT.Craponite}) {
				OreDictMaterial tOutput = tMat;
				RM.ImplosionCompressor.addRecipeX(T, 0, 256, ST.array(dust.mat(tMat, 1), tTNT , ST.tag(0)), plateGem .mat(tOutput, 1));
				RM.ImplosionCompressor.addRecipeX(T, 0, 256, ST.array(dust.mat(tMat, 1), tTNT , ST.tag(1)), gem .mat(tOutput, 1));
				RM.ImplosionCompressor.addRecipeX(T, 0, 256, ST.array(dust.mat(tMat, 2), ST.mul(4, tTNT), ST.tag(2)), gemFlawless .mat(tOutput, 1));
				RM.ImplosionCompressor.addRecipeX(T, 0, 256, ST.array(dust.mat(tMat, 4), ST.mul(8, tTNT), ST.tag(3)), gemExquisite.mat(tOutput, 1));
			}
		}
	}

	/** GT6 statements GT6U put a statement of run_run ahead of: what they make gives way to it. */
	static void later_run_21() {
		for (ItemStack tTNT : ST.array(ST.make(Blocks.TNT, 8, W), IL.IC2_ITNT.get(4), IL.Boomstick.get(12), IL.Dynamite.get(2), IL.Dynamite_Strong.get(1))) if (ST.valid(tTNT)) {
			for (OreDictMaterial tMat : new OreDictMaterial[] {MT.Spinel, MT.BalasRuby, MT.Topaz, MT.BlueTopaz, MT.Tanzanite, MT.Amazonite, MT.Alexandrite, MT.Opal, MT.OnyxRed, MT.OnyxBlack, MT.Olivine, MT.Dioptase, MT.Craponite}) {
				OreDictMaterial tOutput = tMat;
				RM.ImplosionCompressor.addRecipeX(T, 0, 256, ST.array(dust.mat(tMat, 1), tTNT , ST.tag(0)), plateGem .mat(tOutput, 1));
				RM.ImplosionCompressor.addRecipeX(T, 0, 256, ST.array(dust.mat(tMat, 1), tTNT , ST.tag(1)), gem .mat(tOutput, 1));
				RM.ImplosionCompressor.addRecipeX(T, 0, 256, ST.array(dust.mat(tMat, 2), ST.mul(4, tTNT), ST.tag(2)), gemFlawless .mat(tOutput, 1));
				RM.ImplosionCompressor.addRecipeX(T, 0, 256, ST.array(dust.mat(tMat, 4), ST.mul(8, tTNT), ST.tag(3)), gemExquisite.mat(tOutput, 1));
			}
		}
	}

	/** GT6 statements GT6U put a statement of run_run ahead of: what they make gives way to it. */
	static void later_run_22() {
		for (ItemStack tTNT : ST.array(ST.make(Blocks.TNT, 8, W), IL.IC2_ITNT.get(4), IL.Boomstick.get(12), IL.Dynamite.get(2), IL.Dynamite_Strong.get(1))) if (ST.valid(tTNT)) {
			for (OreDictMaterial tMat : new OreDictMaterial[] {MT.Spinel, MT.BalasRuby, MT.Topaz, MT.BlueTopaz, MT.Tanzanite, MT.Amazonite, MT.Alexandrite, MT.Opal, MT.OnyxRed, MT.OnyxBlack, MT.Olivine, MT.Dioptase, MT.Craponite}) {
				OreDictMaterial tOutput = tMat;
				RM.ImplosionCompressor.addRecipeX(T, 0, 256, ST.array(dust.mat(tMat, 1), tTNT , ST.tag(0)), plateGem .mat(tOutput, 1));
				RM.ImplosionCompressor.addRecipeX(T, 0, 256, ST.array(dust.mat(tMat, 1), tTNT , ST.tag(1)), gem .mat(tOutput, 1));
				RM.ImplosionCompressor.addRecipeX(T, 0, 256, ST.array(dust.mat(tMat, 2), ST.mul(4, tTNT), ST.tag(2)), gemFlawless .mat(tOutput, 1));
				RM.ImplosionCompressor.addRecipeX(T, 0, 256, ST.array(dust.mat(tMat, 4), ST.mul(8, tTNT), ST.tag(3)), gemExquisite.mat(tOutput, 1));
			}
		}
	}

	/** GT6 statements GT6U put a statement of run_run ahead of: what they make gives way to it. */
	static void later_run_23() {
		for (ItemStack tTNT : ST.array(ST.make(Blocks.TNT, 8, W), IL.IC2_ITNT.get(4), IL.Boomstick.get(12), IL.Dynamite.get(2), IL.Dynamite_Strong.get(1))) if (ST.valid(tTNT)) {
			for (OreDictMaterial tMat : new OreDictMaterial[] {MT.Spinel, MT.BalasRuby, MT.Topaz, MT.BlueTopaz, MT.Tanzanite, MT.Amazonite, MT.Alexandrite, MT.Opal, MT.OnyxRed, MT.OnyxBlack, MT.Olivine, MT.Dioptase, MT.Craponite}) {
				OreDictMaterial tOutput = tMat;
				RM.ImplosionCompressor.addRecipeX(T, 0, 256, ST.array(dust.mat(tMat, 1), tTNT , ST.tag(0)), plateGem .mat(tOutput, 1));
				RM.ImplosionCompressor.addRecipeX(T, 0, 256, ST.array(dust.mat(tMat, 1), tTNT , ST.tag(1)), gem .mat(tOutput, 1));
				RM.ImplosionCompressor.addRecipeX(T, 0, 256, ST.array(dust.mat(tMat, 2), ST.mul(4, tTNT), ST.tag(2)), gemFlawless .mat(tOutput, 1));
				RM.ImplosionCompressor.addRecipeX(T, 0, 256, ST.array(dust.mat(tMat, 4), ST.mul(8, tTNT), ST.tag(3)), gemExquisite.mat(tOutput, 1));
			}
		}
	}

	/** GT6's statements in Loader_Recipes_Other.run that GT6U dropped, as the port has them: their Recipes are taken back. */
	static void takeBack_run() {
		GT6U_Recipes.takeBack("Loader_Recipes_Other.run", () -> {
			new OreDictListenerEvent_Names() {
				@Override public void addAllListeners() {
					addListener(DYE_OREDICTS_LENS[DYE_INDEX_Red], new IOreDictListenerEvent(){@Override public void onOreRegistration(OreDictRegistrationContainer aEvent){for(OreDictMaterial tMat:ANY.Cu.mToThis)RM.LaserEngraver.addRecipe2(T, 16, 64, foil.mat(tMat, 4), ST.amount(0, aEvent.mStack), IL.Circuit_Wire_Copper.get(1));RM.LaserEngraver.addRecipe2(T, 16, 64, foil.mat(MT.Au, 4), ST.amount(0, aEvent.mStack), IL.Circuit_Wire_Gold.get(1));RM.LaserEngraver.addRecipe2(T, 16, 64, foil.mat(MT.Pt, 4), ST.amount(0, aEvent.mStack), IL.Circuit_Wire_Platinum.get(1));RM.LaserEngraver.addRecipe2(T, 16, 64, foil.mat(MT.Thaumium, 4), ST.amount(0, aEvent.mStack), IL.Circuit_Wire_Magic.get(1));RM.LaserEngraver.addRecipe2(T, 16, 64, foil.mat(MT.Manasteel, 4), ST.amount(0, aEvent.mStack), IL.Circuit_Wire_Magic.get(1));RM.LaserEngraver.addRecipe2(T, 16, 64, foil.mat(MT.Mithril, 4), ST.amount(0, aEvent.mStack), IL.Circuit_Wire_Magic.get(1));RM.LaserEngraver.addRecipe2(T, 16, 64, foil.mat(MT.Netherite, 4), ST.amount(0, aEvent.mStack), IL.Circuit_Wire_Magic.get(1));RM.LaserEngraver.addRecipe2(T, 16, 64, foil.mat(MT.Enderium, 4), ST.amount(0, aEvent.mStack), IL.Circuit_Wire_Enderium.get(1));RM.LaserEngraver.addRecipe2(T, 16, 64, foil.mat(MT.Signalum, 4), ST.amount(0, aEvent.mStack), IL.Circuit_Wire_Signalum.get(1));}});
					addListener(OD.itemResin, new IOreDictListenerEvent(){@Override public void onOreRegistration(OreDictRegistrationContainer aEvent){RM.rem_smelting(aEvent.mStack);if(MD.TiC.owns(aEvent.mStack))return;RM.biomass(ST.amount(6, aEvent.mStack), 32);RM.ic2_extractor(aEvent.mStack, OM.ingot(MT.Rubber, 7*U9));RM.Juicer.addRecipe1(T, 16, 64, aEvent.mStack, NF, FL.Latex.make(L/2), NI);RM.Squeezer.addRecipe1(T, 16, 64, aEvent.mStack, NF, FL.Latex.make(L), NI);RM.Centrifuge.addRecipe1(T, 16, 64, aEvent.mStack, NF, FL.Latex.make(L), FL.Glue.make(250));for(FluidStack tWater:FL.waters(250))RM.Mixer.addRecipe1(T, 16, 16, aEvent.mStack, tWater, FL.Glue.make(250), ZL_IS);RM.Laminator.addRecipe2(T, 16, 16, aEvent.mStack, ST.make(Blocks.PISTON, 1, W), ST.make(Blocks.STICKY_PISTON, 1, 0));}});
				}
			};
			for (ItemStack tTNT : ST.array(ST.make(Blocks.TNT, 8, W), IL.IC2_ITNT.get(4), IL.Boomstick.get(12), IL.Dynamite.get(2), IL.Dynamite_Strong.get(1))) if (ST.valid(tTNT)) {
				for (OreDictMaterial tMat : ANY.Diamond.mToThis) {
					OreDictMaterial tOutput = MT.DiamondIndustrial;
					RM.ImplosionCompressor.addRecipeX(T, 0, 256, ST.array(dust.mat(tMat, 1), tTNT , ST.tag(0)), plateGem .mat(tOutput, 1));
					RM.ImplosionCompressor.addRecipeX(T, 0, 256, ST.array(dust.mat(tMat, 1), tTNT , ST.tag(1)), gem .mat(tOutput, 1));
					RM.ImplosionCompressor.addRecipeX(T, 0, 256, ST.array(dust.mat(tMat, 2), ST.mul(4, tTNT), ST.tag(2)), gemFlawless .mat(tOutput, 1));
					RM.ImplosionCompressor.addRecipeX(T, 0, 256, ST.array(dust.mat(tMat, 4), ST.mul(8, tTNT), ST.tag(3)), gemExquisite.mat(tOutput, 1));
				}
				for (OreDictMaterial tMat : ANY.Sapphire.mToThis) {
					OreDictMaterial tOutput = MT.Sapphire;
					RM.ImplosionCompressor.addRecipeX(T, 0, 256, ST.array(dust.mat(tMat, 1), tTNT , ST.tag(0)), plateGem .mat(tOutput, 1));
					RM.ImplosionCompressor.addRecipeX(T, 0, 256, ST.array(dust.mat(tMat, 1), tTNT , ST.tag(1)), gem .mat(tOutput, 1));
					RM.ImplosionCompressor.addRecipeX(T, 0, 256, ST.array(dust.mat(tMat, 2), ST.mul(4, tTNT), ST.tag(2)), gemFlawless .mat(tOutput, 1));
					RM.ImplosionCompressor.addRecipeX(T, 0, 256, ST.array(dust.mat(tMat, 4), ST.mul(8, tTNT), ST.tag(3)), gemExquisite.mat(tOutput, 1));
				}
				for (OreDictMaterial tMat : ANY.Garnet.mToThis) {
					OreDictMaterial tOutput = tMat;
					RM.ImplosionCompressor.addRecipeX(T, 0, 256, ST.array(dust.mat(tMat, 1), tTNT , ST.tag(0)), plateGem .mat(tOutput, 1));
					RM.ImplosionCompressor.addRecipeX(T, 0, 256, ST.array(dust.mat(tMat, 1), tTNT , ST.tag(1)), gem .mat(tOutput, 1));
					RM.ImplosionCompressor.addRecipeX(T, 0, 256, ST.array(dust.mat(tMat, 2), ST.mul(4, tTNT), ST.tag(2)), gemFlawless .mat(tOutput, 1));
					RM.ImplosionCompressor.addRecipeX(T, 0, 256, ST.array(dust.mat(tMat, 4), ST.mul(8, tTNT), ST.tag(3)), gemExquisite.mat(tOutput, 1));
				}
				for (OreDictMaterial tMat : ANY.Jasper.mToThis) {
					OreDictMaterial tOutput = tMat;
					RM.ImplosionCompressor.addRecipeX(T, 0, 256, ST.array(dust.mat(tMat, 1), tTNT , ST.tag(0)), plateGem .mat(tOutput, 1));
					RM.ImplosionCompressor.addRecipeX(T, 0, 256, ST.array(dust.mat(tMat, 1), tTNT , ST.tag(1)), gem .mat(tOutput, 1));
					RM.ImplosionCompressor.addRecipeX(T, 0, 256, ST.array(dust.mat(tMat, 2), ST.mul(4, tTNT), ST.tag(2)), gemFlawless .mat(tOutput, 1));
					RM.ImplosionCompressor.addRecipeX(T, 0, 256, ST.array(dust.mat(tMat, 4), ST.mul(8, tTNT), ST.tag(3)), gemExquisite.mat(tOutput, 1));
				}
				for (OreDictMaterial tMat : ANY.TigerEye.mToThis) {
					OreDictMaterial tOutput = tMat;
					RM.ImplosionCompressor.addRecipeX(T, 0, 256, ST.array(dust.mat(tMat, 1), tTNT , ST.tag(0)), plateGem .mat(tOutput, 1));
					RM.ImplosionCompressor.addRecipeX(T, 0, 256, ST.array(dust.mat(tMat, 1), tTNT , ST.tag(1)), gem .mat(tOutput, 1));
					RM.ImplosionCompressor.addRecipeX(T, 0, 256, ST.array(dust.mat(tMat, 2), ST.mul(4, tTNT), ST.tag(2)), gemFlawless .mat(tOutput, 1));
					RM.ImplosionCompressor.addRecipeX(T, 0, 256, ST.array(dust.mat(tMat, 4), ST.mul(8, tTNT), ST.tag(3)), gemExquisite.mat(tOutput, 1));
				}
				for (OreDictMaterial tMat : ANY.Aventurine.mToThis) {
					OreDictMaterial tOutput = tMat;
					RM.ImplosionCompressor.addRecipeX(T, 0, 256, ST.array(dust.mat(tMat, 1), tTNT , ST.tag(0)), plateGem .mat(tOutput, 1));
					RM.ImplosionCompressor.addRecipeX(T, 0, 256, ST.array(dust.mat(tMat, 1), tTNT , ST.tag(1)), gem .mat(tOutput, 1));
					RM.ImplosionCompressor.addRecipeX(T, 0, 256, ST.array(dust.mat(tMat, 2), ST.mul(4, tTNT), ST.tag(2)), gemFlawless .mat(tOutput, 1));
					RM.ImplosionCompressor.addRecipeX(T, 0, 256, ST.array(dust.mat(tMat, 4), ST.mul(8, tTNT), ST.tag(3)), gemExquisite.mat(tOutput, 1));
				}
			}
			RM.Massfab .addRecipe2(T, 16, 144 , ST.tag(0), dustDiv72.mat(MT.AncientDebris, 1), NF, FL.Ender.make(L ), ZL_IS);
			RM.Massfab .addRecipe2(T, 16, 144* 8, ST.tag(0), dustTiny .mat(MT.AncientDebris, 1), NF, FL.Ender.make(L* 8), ZL_IS);
			RM.Massfab .addRecipe2(T, 16, 144* 18, ST.tag(0), dustSmall.mat(MT.AncientDebris, 1), NF, FL.Ender.make(L* 18), ZL_IS);
			RM.Massfab .addRecipe2(T, 16, 144* 72, ST.tag(0), dust .mat(MT.AncientDebris, 1), NF, FL.Ender.make(L* 72), ZL_IS);
			RM.Massfab .addRecipe2(T, 16, 144* 72, ST.tag(0), ingot .mat(MT.AncientDebris, 1), NF, FL.Ender.make(L* 72), ZL_IS);
			RM.Massfab .addRecipe2(T, 16, 144*648, ST.tag(0), blockDust.mat(MT.AncientDebris, 1), NF, FL.Ender.make(L*648), ZL_IS);
			RM.Replicator.addRecipe1(T, 16, 144 , ST.tag(0), FL.Ender.make(L), NF, gem.mat(MT.EnderPearl, 1));
			RM.Lightning .addRecipe1(T, 512, 512, blockSolid.mat(MT.Netherite, 1), FL.array(FL.Ender.make(L*4), FL.Oil_Soulsand.make(5000)), ZL_FS, gem.mat(MT.NetherStar, 1));
			RM.Fusion.addRecipe1(F, -8192, 730, ST.tag(1), FL.array(MT.D .gas (U*2, T) ), FL.array(MT.He_3 .gas ( U2, F), MT.T .gas ( U2, F) ), ZL_IS ).setSpecialNumber( 730L*8192L*16L);
			RM.Fusion.addRecipe1(F, -8192, 1130, ST.tag(1), FL.array(MT.T .gas (U*2, T) ), FL.array(MT.He .gas ( U , F) ), ZL_IS ).setSpecialNumber( 1130L*8192L*16L);
			RM.Fusion.addRecipe1(F, -8192, 1290, ST.tag(1), FL.array(MT.He_3 .gas (U*2, T) ), FL.array(MT.He .gas ( U , F) ), ZL_IS ).setSpecialNumber( 1290L*8192L*16L);
			RM.Fusion.addRecipe1(F, 0, 1890, ST.tag(1), FL.array(MT.He .gas (U*2, T) ), FL.array(MT.Be_8 .liquid( U , F) ), ZL_IS ).setSpecialNumber( 1890L*8192L*16L);
			RM.Fusion.addRecipe1(F, 0, 3214, ST.tag(1), FL.array(MT.Be_8 .liquid(U*2, T) ), FL.array(MT.O .gas ( U , F) ), ZL_IS ).setSpecialNumber( 3214L*8192L*16L);
			RM.Fusion.addRecipe1(F, -8192, 546, ST.tag(2), FL.array(MT.H .gas (U , T), MT.B_11.liquid(U , T)), FL.array(MT.He .gas (3*U , F) ), ZL_IS ).setSpecialNumber( 8469L*8192L*16L);
			RM.Fusion.addRecipe1(F, -8192, 315, ST.tag(2), FL.array(MT.H .gas (U , T), MT.C .liquid(U , T)), FL.array(MT.C_13 .liquid( U , F) ), ZL_IS ).setSpecialNumber( 315L*8192L*16L);
			RM.Fusion.addRecipe1(F, -8192, 754, ST.tag(2), FL.array(MT.H .gas (U , T), MT.C_13.liquid(U , T)), FL.array(MT.N .gas ( U , F) ), ZL_IS ).setSpecialNumber( 754L*8192L*16L);
			RM.Fusion.addRecipe1(F, -8192, 1404, ST.tag(2), FL.array(MT.H .gas (U*2, T), MT.N .gas (U , T)), FL.array(MT.He .gas ( U2, F), MT.C .liquid( U2, F), MT.O .gas ( U2, F) ), ZL_IS ).setSpecialNumber( 1404L*8192L*16L);
			RM.Fusion.addRecipe1(F, -8192, 455, ST.tag(2), FL.array(MT.H .gas (U*2, T), MT.O .gas (U , T)), FL.array(MT.He .gas ( U2, F), MT.F .gas ( U2, F), MT.N .gas ( U2, F) ), ZL_IS ).setSpecialNumber( 455L*8192L*16L);
			RM.Fusion.addRecipe1(F, -8192, 1760, ST.tag(2), FL.array(MT.D .gas (U , T), MT.T .gas (U , T)), FL.array(MT.He .gas ( U , F) ), ZL_IS ).setSpecialNumber( 1760L*8192L*16L);
			RM.Fusion.addRecipe1(F, -8192, 1830, ST.tag(2), FL.array(MT.D .gas (U , T), MT.He_3.gas (U , T)), FL.array(MT.He .gas ( U , F) ), ZL_IS ).setSpecialNumber( 1830L*8192L*16L);
			RM.Fusion.addRecipe1(F, -8192, 2640, ST.tag(2), FL.array(MT.T .gas (U , T), MT.He_3.gas (U , T)), FL.array(MT.He .gas (3*U4, F), MT.D .gas ( U4, F) ), ZL_IS ).setSpecialNumber( 2640L*8192L*16L);
			RM.Fusion.addRecipe1(F, -8192, 3336, ST.tag(2), FL.array(MT.D .gas (U , T), MT.Li_6.liquid(U , T)), FL.array(MT.He .gas (3*U8, F), MT.He_3 .gas ( U8, F), MT.Li .liquid( U8, F), MT.Be_7 .liquid( U8, F)), ZL_IS ).setSpecialNumber( 3336L*8192L*16L);
			RM.Fusion.addRecipe1(F, -8192, 1690, ST.tag(2), FL.array(MT.He_3 .gas (U , T), MT.Li_6.liquid(U , T)), FL.array(MT.He .gas (2*U , F) ), ZL_IS ).setSpecialNumber( 1690L*8192L*16L);
			RM.Fusion.addRecipe1(F, -8192, 736, ST.tag(2), FL.array(MT.He .gas (U , T), MT.Be_8.liquid(U , T)), FL.array(MT.C .liquid( U , F) ), ZL_IS ).setSpecialNumber( 736L*8192L*16L);
			RM.Fusion.addRecipe1(F, -8192, 716, ST.tag(2), FL.array(MT.He .gas (U , T), MT.C .liquid(U , T)), FL.array(MT.O .gas ( U , F) ), ZL_IS ).setSpecialNumber( 716L*8192L*16L);
			RM.Fusion.addRecipe1(F, -8192, 1956, ST.tag(2), FL.array(MT.Ad .liquid(U , T), MT.Be_7.liquid(U , T)), FL.array(MT.W .liquid( U , F), MT.He .gas (16*U, F), MT.He_3 .gas (24*U, F), MT.T .gas (24*U, F)), dust.mat(MT.Vb, 1)).setSpecialNumber(94956L*8192L*16L);
		});
	}
}

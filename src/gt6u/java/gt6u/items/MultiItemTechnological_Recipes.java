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
 * the GregTech6-Unofficial entries of gregtech/items/MultiItemTechnological.java moved into an addon registry.
 */

package gt6u.items;


import gt6u.data.MT_GT6U;
import gt6u.data.IL_GT6U;

import static gregapi.data.CS.*;
import gregapi.code.ItemStackContainer;
import gregapi.cover.covers.*;
import gregapi.data.*;
import gregapi.data.CS.BooksGT;
import gregapi.data.CS.ItemsGT;
import gregapi.item.CreativeTab;
import gregapi.item.multiitem.MultiItemRandom;
import gregapi.item.multiitem.behaviors.Behavior_Turn_Into;
import gregapi.oredict.OreDictItemData;
import gregapi.oredict.OreDictMaterial;
import gregapi.util.CR;
import gregapi.util.OM;
import gregapi.util.ST;
import gregtech.items.behaviors.Behavior_DataStorage;
import gregtech.items.behaviors.Behavior_DataStorage16;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import gt6mirror.net.minecraftforge.fluids.FluidContainerRegistry.FluidContainerData;
import gregapi.data.LH;
import gt6u.GT6U_Recipes;

/** GregTech6-Unofficial's edits of gregtech/items/MultiItemTechnological.java, as statements: what GT6U added runs in run(), what it dropped is taken back in
 *  takeBack(): restated as GT6 makes them, their Recipes are taken out of GT6's maps again. */
public class MultiItemTechnological_Recipes {
	/** A GT6 item's name and tooltip as GT6U has them: addItem would take the id again, so the language entries it makes are set. */
	private static void change(MultiItemRandom aItem, int aID, String aEnglish, String aToolTip) {
		ItemStack tStack = ST.make(aItem, 1, aID);
		LH.add(aItem.getUnlocalizedName(tStack), aEnglish);
		LH.add(aItem.getUnlocalizedName(tStack) + ".tooltip", aToolTip);
	}
	
	/** GT6U's data of the GT6 items it rewrote, which GT6 registers in place of its own (GT6Definitions.itemData); null for the rest. */
	public static OreDictItemData itemData(int aID) {
		switch (aID) {
		case 30000: return new OreDictItemData(ANY.Cu, U100*25, ANY.Plastic, U);
		case 30002: return new OreDictItemData(MT.Cu, U, ANY.SiO2, U);
		case 30012: return new OreDictItemData(ANY.SiO2, U);
		default: return null;
		}
	}
	
	public static void run() {
		run_addItems();
	}
	
	public static void takeBack() {
		takeBack_addItems();
	}
	
	/** GT6U's additions to MultiItemTechnological.addItems (GT6U gregtech/items/MultiItemTechnological.java). */
	static void run_addItems() {
		int tLastID = 0;
		CR.shaped(IL.Comp_Laser_Gas_Empty.get(1), CR.DEF_REV_NCC, "CWM", "WGx", "MTd", 'W', MT.DATA.CABLES_01[2], 'C', OD_CIRCUITS[1], 'M', OP.plate.dat(MT.Ag), 'T', OP.screw.dat(MT.StainlessSteel), 'G', Blocks.GLASS);
		change(ItemsGT.TECH, 30000, "Plastic Circuit Plate", "Needs Circuit Wiring");
		CR.shaped(IL_GT6U.Perforated_Bushing.get(1), CR.DEF_NAC , "ShS", "PPP", "SfS", 'P', OP.plateDouble.dat(ANY.Steel), 'S', OP.bolt.dat(ANY.Steel));
		RM.Bath.addRecipe1(T, 0, 200, IL_GT6U.Perforated_Bushing.get(0), MT.Glass.liquid(U10, T), NF, IL_GT6U.Glass_Fiber.get(10));
		RM.Mixer.addRecipe1(T, 16, 300, ST.tag(0), FL.array(MT.HCl.fluid(U, T), MT.H2O2.fluid(U*2, T), MT.H2O.fluid(U*3, T)), MT_GT6U.ElectroEtchingSolution.fluid(U*6, T), NI);
		for (OreDictMaterial tMat : ANY.SiO2.mToThis) {
			ItemStack tDust = OP.dust.mat(tMat, 1);
			if (ST.valid(tDust)) RM.Press.addRecipeX(T, F, F, F, T, 16, 64, ST.array(OP.plate.mat(MT.Plastic, 1), tDust, OP.foil.mat(MT.Cu, 1)), IL.Circuit_Plate_Empty.get(1));
		}
		RM.Press.addRecipeX(T, F, F, F, T, 16, 100, ST.array(OP.plate.mat(MT_GT6U.Epoxid, 1), IL_GT6U.Glass_Fiber.get(4), OP.foil.mat(MT.Cu, 4)), IL_GT6U.Circuit_Plate_Empty_Epoxid.get(1));
		RM.Press.addRecipeX(T, F, F, F, T, 16, 100, ST.array(OP.plate.mat(MT_GT6U.Epoxid, 1), OP.wireFine.mat(MT.Graphene, 4), OP.foil.mat(MT.Cu, 4)), IL_GT6U.Circuit_Plate_Empty_Epoxid.get(1));
		RM.Press.addRecipeX(T, F, F, F, T, 16, 160, ST.array(OP.foil.mat(MT_GT6U.Epoxid, 4), IL_GT6U.Glass_Fiber.get(8), OP.foil.mat(MT.Cu, 8)), IL_GT6U.Circuit_Plate_Empty_Multilayer.get(1));
		RM.Press.addRecipeX(T, F, F, F, T, 16, 160, ST.array(OP.foil.mat(MT_GT6U.Epoxid, 4), OP.wireFine.mat(MT.Graphene, 8), OP.foil.mat(MT.Cu, 8)), IL_GT6U.Circuit_Plate_Empty_Multilayer.get(1));
		RM.Press.addRecipeX(T, F, F, F, T, 64, 256, ST.array(OP.foil.mat(MT_GT6U.Epoxid, 8), OP.foil.mat(MT.Al, 4), IL_GT6U.Glass_Fiber.get(16)), IL_GT6U.Circuit_plate_Empty_Crystal.get(1));
		RM.Press.addRecipeX(T, F, F, F, T, 64, 256, ST.array(OP.foil.mat(MT_GT6U.Epoxid, 8), OP.foil.mat(MT.Al, 4), OP.wireFine.mat(MT.Graphene, 16)), IL_GT6U.Circuit_plate_Empty_Crystal.get(1));
		change(ItemsGT.TECH, 30002, "Circuit Plate (Copper)", "Needs Circuit Parts");
		change(ItemsGT.TECH, 30012, "Circuit Plate (Magic)", "Needs Circuit Parts");
		RM.Press.addRecipe2(T, F, F, F, T, 16, 64, IL_GT6U.Circuit_Plate_Empty_Phenolic.get(1), IL.Circuit_Wire_Copper .get(1), MT_GT6U.ElectroEtchingSolution.fluid(U10, T), NF, IL.Circuit_Plate_Copper .get(1));
		RM.Press.addRecipe2(T, F, F, F, T, 16, 64, IL.Circuit_Plate_Empty.get(1), IL.Circuit_Wire_Copper .get(1), MT_GT6U.ElectroEtchingSolution.fluid(U10, T), NF, IL.Circuit_Plate_Copper .get(1));
		RM.Press.addRecipe2(T, F, F, F, T, 16, 64, IL_GT6U.Circuit_Plate_Empty_Epoxid.get(1), IL.Circuit_Wire_Gold .get(1), MT_GT6U.ElectroEtchingSolution.fluid(U10, T), NF, IL.Circuit_Plate_Gold .get(1));
		RM.Press.addRecipe2(T, F, F, F, T, 16, 64, IL_GT6U.Circuit_Plate_Empty_Multilayer.get(1), IL.Circuit_Wire_Platinum .get(1), MT_GT6U.ElectroEtchingSolution.fluid(U10, T), NF, IL.Circuit_Plate_Platinum .get(1));
		RM.Press.addRecipe2(T, F, F, F, T, 16, 64, IL_GT6U.Circuit_Plate_Empty_Phenolic.get(1), IL.Circuit_Wire_Magic .get(1), MT_GT6U.ElectroEtchingSolution.fluid(U10, T), NF, IL.Circuit_Plate_Magic .get(1));
		RM.Press.addRecipe2(T, F, F, F, T, 16, 64, IL.Circuit_Plate_Empty.get(1), IL.Circuit_Wire_Magic .get(1), MT_GT6U.ElectroEtchingSolution.fluid(U10, T), NF, IL.Circuit_Plate_Magic .get(1));
		RM.Press.addRecipe2(T, F, F, F, T, 16, 64, IL.Circuit_Plate_Empty.get(1), IL.Circuit_Wire_Enderium .get(1), MT_GT6U.ElectroEtchingSolution.fluid(U10, T), NF, IL.Circuit_Plate_Enderium .get(1));
		RM.Press.addRecipe2(T, F, F, F, T, 16, 64, IL.Circuit_Plate_Empty.get(1), IL.Circuit_Wire_Signalum .get(1), MT_GT6U.ElectroEtchingSolution.fluid(U10, T), NF, IL.Circuit_Plate_Signalum .get(1));
		RM.Assembler.addRecipeX(T, F, F, F, T, 32, 16, ST.array(OP.wireFine.mat(MT.Cu, 1), OP.wireFine.mat(MT.Signalum, 1), OP.plateGemTiny.mat(MT.Si, 1)), IL.Circuit_Part_Good.get(1));
		RM.Assembler.addRecipeX(T, F, F, F, T, 32, 16, ST.array(OP.wireFine.mat(MT.AnnealedCopper, 1), OP.wireFine.mat(MT.Signalum, 1), OP.plateGemTiny.mat(MT.Si, 1)), IL.Circuit_Part_Good.get(1));
		RM.Assembler.addRecipeX(T, F, F, F, T, 32, 16, ST.array(OP.wireFine.mat(MT.Cu, 1), OP.wireFine.mat(MT.RedAlloy, 1), OP.plateGemTiny.mat(MT.Si, 1)), IL.Circuit_Part_Good.get(1));
		RM.Assembler.addRecipeX(T, F, F, F, T, 32, 16, ST.array(OP.wireFine.mat(MT.AnnealedCopper, 1), OP.wireFine.mat(MT.RedAlloy, 1), OP.plateGemTiny.mat(MT.Si, 1)), IL.Circuit_Part_Good.get(1));
		RM.Assembler.addRecipeX(T, F, F, F, T, 32, 16, ST.array(OP.wireFine.mat(MT.Cu, 1), OP.wireFine.mat(MT.Signalum, 1), OP.plateGemTiny.mat(MT.Ge, 1)), IL.Circuit_Part_Good.get(1));
		RM.Assembler.addRecipeX(T, F, F, F, T, 32, 16, ST.array(OP.wireFine.mat(MT.AnnealedCopper, 1), OP.wireFine.mat(MT.Signalum, 1), OP.plateGemTiny.mat(MT.Ge, 1)), IL.Circuit_Part_Good.get(1));
		RM.Assembler.addRecipeX(T, F, F, F, T, 32, 16, ST.array(OP.wireFine.mat(MT.Cu, 1), OP.wireFine.mat(MT.RedAlloy, 1), OP.plateGemTiny.mat(MT.Ge, 1)), IL.Circuit_Part_Good.get(1));
		RM.Assembler.addRecipeX(T, F, F, F, T, 32, 16, ST.array(OP.wireFine.mat(MT.AnnealedCopper, 1), OP.wireFine.mat(MT.RedAlloy, 1), OP.plateGemTiny.mat(MT.Ge, 1)), IL.Circuit_Part_Good.get(1));
		RM.Assembler.addRecipeX(T, F, F, F, T, 32, 16, ST.array(OP.wireFine.mat(MT.Cu, 1), OP.wireFine.mat(MT.Signalum, 1), OP.plateGemTiny.mat(MT.RedstoneAlloy, 1)), IL.Circuit_Part_Good.get(1));
		RM.Assembler.addRecipeX(T, F, F, F, T, 32, 16, ST.array(OP.wireFine.mat(MT.AnnealedCopper, 1), OP.wireFine.mat(MT.Signalum, 1), OP.plateGemTiny.mat(MT.RedstoneAlloy, 1)), IL.Circuit_Part_Good.get(1));
		RM.Assembler.addRecipeX(T, F, F, F, T, 32, 16, ST.array(OP.wireFine.mat(MT.Cu, 1), OP.wireFine.mat(MT.RedAlloy, 1), OP.plateGemTiny.mat(MT.RedstoneAlloy, 1)), IL.Circuit_Part_Good.get(1));
		RM.Assembler.addRecipeX(T, F, F, F, T, 32, 16, ST.array(OP.wireFine.mat(MT.AnnealedCopper, 1), OP.wireFine.mat(MT.RedAlloy, 1), OP.plateGemTiny.mat(MT.RedstoneAlloy, 1)), IL.Circuit_Part_Good.get(1));
		RM.Assembler.addRecipeX(T, F, F, F, T, 128, 48, ST.array(OP.wireFine.mat(MT.Au , 1), OP.wireFine.mat(MT.Signalum, 1), OP.plateGemTiny.mat(MT.Si , 1)), IL.Circuit_Part_Advanced .get(1));
		RM.Assembler.addRecipeX(T, F, F, F, T, 128, 48, ST.array(OP.wireFine.mat(MT.Au , 1), OP.wireFine.mat(MT.Signalum, 1), OP.plateGemTiny.mat(MT.Ge , 1)), IL.Circuit_Part_Advanced .get(1));
		RM.Assembler.addRecipeX(T, F, F, F, T, 128, 48, ST.array(OP.wireFine.mat(MT.Au , 1), OP.wireFine.mat(MT.Signalum, 1), OP.plateGemTiny.mat(MT.RedstoneAlloy, 1)), IL.Circuit_Part_Elite .get(1));
		RM.Assembler.addRecipeX(T, F, F, F, T,128, 192, ST.array(OP.wireFine.mat(MT.Pt , 1), OP.wireFine.mat(MT.Signalum, 1), OP.plateGemTiny.mat(MT.Si , 1)), IL.Circuit_Part_Master .get(1));
		RM.Assembler.addRecipeX(T, F, F, F, T,128, 192, ST.array(OP.wireFine.mat(MT.Pt , 1), OP.wireFine.mat(MT.Signalum, 1), OP.plateGemTiny.mat(MT.Ge , 1)), IL.Circuit_Part_Master .get(1));
		RM.Assembler.addRecipeX(T, F, F, F, T,128, 192, ST.array(OP.wireFine.mat(MT.Pt , 1), OP.wireFine.mat(MT.Signalum, 1), OP.plateGemTiny.mat(MT.RedstoneAlloy, 1)), IL.Circuit_Part_Ultimate .get(1));
		RM.Assembler.addRecipe2(T, F, F, F, T, 32, 128, IL.Circuit_Plate_Copper .get(1), IL.Circuit_Part_Good .get(4), IL.Circuit_Board_Good .get(1));
		RM.Assembler.addRecipe2(T, F, F, F, T, 32, 128, IL.Circuit_Plate_Copper .get(1), IL.Circuit_Part_Advanced .get(4), IL.Circuit_Board_Good .get(1));
		RM.Assembler.addRecipe2(T, F, F, F, T, 32, 128, IL.Circuit_Plate_Copper .get(1), IL.Circuit_Part_Elite .get(4), IL.Circuit_Board_Good .get(1));
		RM.Assembler.addRecipe2(T, F, F, F, T, 32, 128, IL.Circuit_Plate_Copper .get(1), IL.Circuit_Part_Master .get(4), IL.Circuit_Board_Good .get(1));
		RM.Assembler.addRecipe2(T, F, F, F, T, 32, 128, IL.Circuit_Plate_Copper .get(1), IL.Circuit_Part_Ultimate .get(4), IL.Circuit_Board_Good .get(1));
		RM.Assembler.addRecipe2(T, F, F, F, T, 32, 128, IL.Circuit_Plate_Gold .get(1), IL.Circuit_Part_Good .get(4), IL.Circuit_Board_Good .get(1));
		RM.Assembler.addRecipe2(T, F, F, F, T,32, 512, IL.Circuit_Plate_Gold .get(1), IL.Circuit_Part_Advanced .get(4), IL.Circuit_Board_Advanced .get(1));
		RM.Assembler.addRecipe2(T, F, F, F, T,128, 256, IL.Circuit_Plate_Gold .get(1), IL.Circuit_Part_Elite .get(4), IL.Circuit_Board_Elite .get(1));
		RM.Assembler.addRecipe2(T, F, F, F, T,128, 256, IL.Circuit_Plate_Gold .get(1), IL.Circuit_Part_Master .get(4), IL.Circuit_Board_Elite .get(1));
		RM.Assembler.addRecipe2(T, F, F, F, T,128, 256, IL.Circuit_Plate_Gold .get(1), IL.Circuit_Part_Ultimate .get(4), IL.Circuit_Board_Elite .get(1));
		RM.Assembler.addRecipe2(T, F, F, F, T, 32, 128, IL.Circuit_Plate_Platinum .get(1), IL.Circuit_Part_Good .get(4), IL.Circuit_Board_Good .get(1));
		RM.Assembler.addRecipe2(T, F, F, F, T,32, 512, IL.Circuit_Plate_Platinum .get(1), IL.Circuit_Part_Advanced .get(4), IL.Circuit_Board_Advanced .get(1));
		RM.Assembler.addRecipe2(T, F, F, F, T,128, 256, IL.Circuit_Plate_Platinum .get(1), IL.Circuit_Part_Elite .get(4), IL.Circuit_Board_Elite .get(1));
		RM.Assembler.addRecipe2(T, F, F, F, T,128, 512, IL.Circuit_Plate_Platinum .get(1), IL.Circuit_Part_Master .get(4), IL.Circuit_Board_Master .get(1));
		RM.Assembler.addRecipe2(T, F, F, F, T,128, 1024, IL.Circuit_Plate_Platinum .get(1), IL.Circuit_Part_Ultimate .get(4), IL.Circuit_Board_Ultimate .get(1));
		RM.Assembler.addRecipe2(T, F, F, F, T, 16, 64, IL.Circuit_Plate_Magic .get(1), IL.Circuit_Part_Magic .get(4), IL.Circuit_Board_Magic .get(1));
		RM.Assembler.addRecipe2(T, F, F, F, T, 16, 64, IL.Circuit_Plate_Enderium .get(1), IL.Circuit_Part_Enderium .get(4), IL.Circuit_Board_Enderium .get(1));
		RM.Assembler.addRecipe2(T, F, F, F, T, 16, 64, IL.Circuit_Plate_Signalum .get(1), IL.Circuit_Part_Signalum .get(4), IL.Circuit_Board_Signalum .get(1));
		RM.Assembler.addRecipe2(T, F, F, F, T, 16, 64, IL.Circuit_Plate_HSLA .get(1), IL.Circuit_Part_EnderPearl .get(4), IL.Circuit_Board_HSLA_Circuit .get(1));
		RM.Assembler.addRecipe2(T, F, F, F, T, 16, 64, IL.Circuit_Plate_HSLA .get(1), IL.Circuit_Part_EnderEye .get(4), IL.Circuit_Board_Power_Module .get(1));
		RM.Bath.addRecipe1(T, F, F, F, T, 0, 120, IL.Circuit_Board_Good.get(1), MT.Sn.liquid(U2, T), NF, IL.Circuit_Good.get(1));
		RM.Bath.addRecipe1(T, F, F, F, T, 0, 120, IL.Circuit_Board_Good.get(1), MT.SolderingAlloy.liquid(U2, T), NF, IL.Circuit_Good.get(1));
		RM.Bath.addRecipe1(T, F, F, F, T, 0, 120, IL.Circuit_Board_Advanced.get(1), MT.Sn.liquid(U2, T), NF, IL.Circuit_Good.get(1));
		RM.Bath.addRecipe1(T, F, F, F, T, 0, 120, IL.Circuit_Board_Advanced.get(1), MT.SolderingAlloy.liquid(U2, T), NF, IL.Circuit_Advanced.get(1));
		RM.Bath.addRecipe1(T, F, F, F, T, 0, 120, IL.Circuit_Board_Elite.get(1), MT.Pb.liquid(U2, T), NF, IL.Circuit_Good.get(1));
		RM.Bath.addRecipe1(T, F, F, F, T, 0, 120, IL.Circuit_Board_Elite.get(1), MT.Sn.liquid(U2, T), NF, IL.Circuit_Advanced.get(1));
		RM.Bath.addRecipe1(T, F, F, F, T, 0, 120, IL.Circuit_Board_Elite.get(1), MT.SolderingAlloy.liquid(U2, T), NF, IL.Circuit_Elite.get(1));
		RM.Bath.addRecipe1(T, F, F, F, T, 0, 120, IL.Circuit_Board_Master.get(1), MT.Pb.liquid(U2, T), NF, IL.Circuit_Advanced.get(1));
		RM.Bath.addRecipe1(T, F, F, F, T, 0, 120, IL.Circuit_Board_Master.get(1), MT.Sn.liquid(U2, T), NF, IL.Circuit_Elite.get(1));
		RM.Bath.addRecipe1(T, F, F, F, T, 0, 120, IL.Circuit_Board_Master.get(1), MT.SolderingAlloy.liquid(U2, T), NF, IL.Circuit_Master.get(1));
		RM.Bath.addRecipe1(T, F, F, F, T, 0, 120, IL.Circuit_Board_Ultimate.get(1), MT.Pb.liquid(U2, T), NF, IL.Circuit_Elite.get(1));
		RM.Bath.addRecipe1(T, F, F, F, T, 0, 120, IL.Circuit_Board_Ultimate.get(1), MT.Sn.liquid(U2, T), NF, IL.Circuit_Master.get(1));
		RM.Bath.addRecipe1(T, F, F, F, T, 0, 120, IL.Circuit_Board_Ultimate.get(1), MT.SolderingAlloy.liquid(U2, T), NF, IL.Circuit_Ultimate.get(1));
		RM.Assembler.addRecipe1(T, F, F, F, T, 32, 64, IL.Circuit_Board_Magic.get(1), MT.SolderingAlloy.liquid(U2, T), NF, IL.Circuit_Magic.get(1));
		RM.Assembler.addRecipe1(T, F, F, F, T, 32, 64, IL.Circuit_Board_Enderium.get(1), MT.SolderingAlloy.liquid(U2, T), NF, IL.Circuit_Enderium.get(1));
		RM.Assembler.addRecipe1(T, F, F, F, T, 32, 64, IL.Circuit_Board_Signalum.get(1), MT.Pb.liquid(U2, T), NF, IL.Circuit_Signalum.get(1));
		RM.Assembler.addRecipe1(T, F, F, F, T, 32, 64, IL.Circuit_Board_Signalum.get(1), MT.Sn.liquid(U2, T), NF, IL.Circuit_Signalum.get(1));
		RM.Assembler.addRecipe1(T, F, F, F, T, 32, 64, IL.Circuit_Board_Signalum.get(1), MT.SolderingAlloy.liquid(U2, T), NF, IL.Circuit_Signalum.get(1));
		RM.Assembler.addRecipe1(T, F, F, F, T, 32, 64, IL.Circuit_Board_BC_Redstone.get(1), MT.SolderingAlloy.liquid(U2, T), NF, IL.Circuit_BC_Redstone.get(1));
		RM.Assembler.addRecipe1(T, F, F, F, T, 32, 64, IL.Circuit_Board_BC_Iron.get(1), MT.SolderingAlloy.liquid(U2, T), NF, IL.Circuit_BC_Iron.get(1));
		RM.Assembler.addRecipe1(T, F, F, F, T, 32, 64, IL.Circuit_Board_BC_Gold.get(1), MT.SolderingAlloy.liquid(U2, T), NF, IL.Circuit_BC_Gold.get(1));
		RM.Assembler.addRecipe1(T, F, F, F, T, 32, 64, IL.Circuit_Board_BC_Diamond.get(1), MT.SolderingAlloy.liquid(U2, T), NF, IL.Circuit_BC_Diamond.get(1));
		RM.Assembler.addRecipe1(T, F, F, F, T, 32, 64, IL.Circuit_Board_BC_Ender.get(1), MT.SolderingAlloy.liquid(U2, T), NF, IL.Circuit_BC_Ender.get(1));
		RM.Assembler.addRecipe1(T, F, F, F, T, 32, 64, IL.Circuit_Board_BC_Quartz.get(1), MT.SolderingAlloy.liquid(U2, T), NF, IL.Circuit_BC_Quartz.get(1));
		RM.Assembler.addRecipe1(T, F, F, F, T, 32, 64, IL.Circuit_Board_BC_Comparator.get(1), MT.SolderingAlloy.liquid(U2, T), NF, IL.Circuit_BC_Comparator.get(1));
		RM.Assembler.addRecipe1(T, F, F, F, T, 32, 64, IL.Circuit_Board_BC_Emerald.get(1), MT.SolderingAlloy.liquid(U2, T), NF, IL.Circuit_BC_Emerald.get(1));
		CR.shaped(IL.Processor_Crystal_Empty.get(1), CR.DEF_REV, "CLC", "LBL", "CLC", 'C', OD_CIRCUITS[6], 'B', IL_GT6U.Circuit_plate_Empty_Crystal, 'L', IL.Comp_Laser_Gas_HeNe);
		RM.Assembler.addRecipe2(T, F, F, F, T, 480, 144, IL.Processor_Crystal_Empty.get(1), IL.Circuit_Crystal_Diamond .get(1), IL.Processor_Crystal_Diamond .get(1));
		RM.Assembler.addRecipe2(T, F, F, F, T, 480, 144, IL.Processor_Crystal_Empty.get(1), IL.Circuit_Crystal_Ruby .get(1), IL.Processor_Crystal_Ruby .get(1));
		RM.Assembler.addRecipe2(T, F, F, F, T, 480, 144, IL.Processor_Crystal_Empty.get(1), IL.Circuit_Crystal_Emerald .get(1), IL.Processor_Crystal_Emerald .get(1));
		RM.Assembler.addRecipe2(T, F, F, F, T, 480, 144, IL.Processor_Crystal_Empty.get(1), IL.Circuit_Crystal_Sapphire.get(1), IL.Processor_Crystal_Sapphire.get(1));
	}

	// The three GT6 item registrations GT6U rewrote: their names by change() in run_addItems, their data by itemData;
	// its own items are registered by the addon's MultiItemTechnological.

	/** GT6's statements in MultiItemTechnological.addItems that GT6U dropped, as the port has them: their Recipes are taken back. */
	static void takeBack_addItems() {
		GT6U_Recipes.takeBack("MultiItemTechnological_Recipes.addItems", () -> {
			CR.shaped(IL.Comp_Laser_Gas_Empty.get(1), CR.DEF_REV_NCC, "CWM", "WGx", "MTd", 'W', MT.DATA.CABLES_01[2], 'C', OD_CIRCUITS[2], 'M', OP.plate.dat(MT.Ag), 'T', OP.screw.dat(MT.StainlessSteel), 'G', OD.blockGlassColorless);
			for (OreDictMaterial tMat : ANY.SiO2.mToThis) {
				ItemStack tDust = OP.dust.mat(tMat, 1);
				if (ST.valid(tDust)) for (OreDictMaterial tMt2 : ANY.Plastic.mToThis) {
					RM.Press.addRecipe2(T, F, F, F, T, 16, 64, OP.plate.mat(tMt2, 1), tDust, IL.Circuit_Plate_Empty.get(1));
				}
			}
			RM.Press.addRecipe2(T, F, F, F, T, 16, 64, IL.Circuit_Plate_Empty.get(1), IL.Circuit_Wire_Copper .get(1), IL.Circuit_Plate_Copper .get(1));
			RM.Press.addRecipe2(T, F, F, F, T, 16, 64, IL.Circuit_Plate_Empty.get(1), IL.Circuit_Wire_Gold .get(1), IL.Circuit_Plate_Gold .get(1));
			RM.Press.addRecipe2(T, F, F, F, T, 16, 64, IL.Circuit_Plate_Empty.get(1), IL.Circuit_Wire_Platinum .get(1), IL.Circuit_Plate_Platinum .get(1));
			RM.Press.addRecipe2(T, F, F, F, T, 16, 64, IL.Circuit_Plate_Empty.get(1), IL.Circuit_Wire_Magic .get(1), IL.Circuit_Plate_Magic .get(1));
			RM.Press.addRecipe2(T, F, F, F, T, 16, 64, IL.Circuit_Plate_Empty.get(1), IL.Circuit_Wire_Enderium .get(1), IL.Circuit_Plate_Enderium .get(1));
			RM.Press.addRecipe2(T, F, F, F, T, 16, 64, IL.Circuit_Plate_Empty.get(1), IL.Circuit_Wire_Signalum .get(1), IL.Circuit_Plate_Signalum .get(1));
			for (OreDictMaterial tMat : ANY.Cu.mToThis) {
				RM.Press.addRecipeX(T, F, F, F, T, 16, 16, ST.array(OP.wireFine.mat(tMat , 1), OP.wireFine.mat(MT.Signalum, 1), OP.plateGemTiny.mat(MT.Si , 1)), IL.Circuit_Part_Good .get(1));
			}
			for (OreDictMaterial tMat : ANY.Cu.mToThis) {
				RM.Press.addRecipeX(T, F, F, F, T, 16, 16, ST.array(OP.wireFine.mat(tMat , 1), OP.wireFine.mat(MT.RedAlloy, 1), OP.plateGemTiny.mat(MT.Si , 1)), IL.Circuit_Part_Good .get(1));
			}
			for (OreDictMaterial tMat : ANY.Cu.mToThis) {
				RM.Press.addRecipeX(T, F, F, F, T, 16, 16, ST.array(OP.wireFine.mat(tMat , 1), OP.wireFine.mat(MT.Signalum, 1), OP.plateGemTiny.mat(MT.Ge , 1)), IL.Circuit_Part_Good .get(1));
			}
			for (OreDictMaterial tMat : ANY.Cu.mToThis) {
				RM.Press.addRecipeX(T, F, F, F, T, 16, 16, ST.array(OP.wireFine.mat(tMat , 1), OP.wireFine.mat(MT.RedAlloy, 1), OP.plateGemTiny.mat(MT.Ge , 1)), IL.Circuit_Part_Good .get(1));
			}
			for (OreDictMaterial tMat : ANY.Cu.mToThis) {
				RM.Press.addRecipeX(T, F, F, F, T, 16, 16, ST.array(OP.wireFine.mat(tMat , 1), OP.wireFine.mat(MT.Signalum, 1), OP.plateGemTiny.mat(MT.RedstoneAlloy, 1)), IL.Circuit_Part_Good .get(1));
			}
			for (OreDictMaterial tMat : ANY.Cu.mToThis) {
				RM.Press.addRecipeX(T, F, F, F, T, 16, 16, ST.array(OP.wireFine.mat(tMat , 1), OP.wireFine.mat(MT.RedAlloy, 1), OP.plateGemTiny.mat(MT.RedstoneAlloy, 1)), IL.Circuit_Part_Good .get(1));
			}
			RM.Press.addRecipeX(T, F, F, F, T, 16, 16, ST.array(OP.wireFine.mat(MT.Au , 1), OP.wireFine.mat(MT.Signalum, 1), OP.plateGemTiny.mat(MT.Si , 1)), IL.Circuit_Part_Advanced .get(1));
			RM.Press.addRecipeX(T, F, F, F, T, 16, 16, ST.array(OP.wireFine.mat(MT.Au , 1), OP.wireFine.mat(MT.Signalum, 1), OP.plateGemTiny.mat(MT.Ge , 1)), IL.Circuit_Part_Advanced .get(1));
			RM.Press.addRecipeX(T, F, F, F, T, 16, 16, ST.array(OP.wireFine.mat(MT.Au , 1), OP.wireFine.mat(MT.Signalum, 1), OP.plateGemTiny.mat(MT.RedstoneAlloy, 1)), IL.Circuit_Part_Elite .get(1));
			RM.Press.addRecipeX(T, F, F, F, T, 16, 16, ST.array(OP.wireFine.mat(MT.Pt , 1), OP.wireFine.mat(MT.Signalum, 1), OP.plateGemTiny.mat(MT.Si , 1)), IL.Circuit_Part_Master .get(1));
			RM.Press.addRecipeX(T, F, F, F, T, 16, 16, ST.array(OP.wireFine.mat(MT.Pt , 1), OP.wireFine.mat(MT.Signalum, 1), OP.plateGemTiny.mat(MT.Ge , 1)), IL.Circuit_Part_Master .get(1));
			RM.Press.addRecipeX(T, F, F, F, T, 16, 16, ST.array(OP.wireFine.mat(MT.Pt , 1), OP.wireFine.mat(MT.Signalum, 1), OP.plateGemTiny.mat(MT.RedstoneAlloy, 1)), IL.Circuit_Part_Ultimate .get(1));
			RM.Press.addRecipe2(T, F, F, F, T, 16, 64, IL.Circuit_Plate_Copper .get(1), IL.Circuit_Part_Good .get(4), IL.Circuit_Board_Good .get(1));
			RM.Press.addRecipe2(T, F, F, F, T, 16, 64, IL.Circuit_Plate_Copper .get(1), IL.Circuit_Part_Advanced .get(4), IL.Circuit_Board_Good .get(1));
			RM.Press.addRecipe2(T, F, F, F, T, 16, 64, IL.Circuit_Plate_Copper .get(1), IL.Circuit_Part_Elite .get(4), IL.Circuit_Board_Good .get(1));
			RM.Press.addRecipe2(T, F, F, F, T, 16, 64, IL.Circuit_Plate_Copper .get(1), IL.Circuit_Part_Master .get(4), IL.Circuit_Board_Good .get(1));
			RM.Press.addRecipe2(T, F, F, F, T, 16, 64, IL.Circuit_Plate_Copper .get(1), IL.Circuit_Part_Ultimate .get(4), IL.Circuit_Board_Good .get(1));
			RM.Press.addRecipe2(T, F, F, F, T, 16, 64, IL.Circuit_Plate_Gold .get(1), IL.Circuit_Part_Good .get(4), IL.Circuit_Board_Good .get(1));
			RM.Press.addRecipe2(T, F, F, F, T, 16, 64, IL.Circuit_Plate_Gold .get(1), IL.Circuit_Part_Advanced .get(4), IL.Circuit_Board_Advanced .get(1));
			RM.Press.addRecipe2(T, F, F, F, T, 16, 64, IL.Circuit_Plate_Gold .get(1), IL.Circuit_Part_Elite .get(4), IL.Circuit_Board_Elite .get(1));
			RM.Press.addRecipe2(T, F, F, F, T, 16, 64, IL.Circuit_Plate_Gold .get(1), IL.Circuit_Part_Master .get(4), IL.Circuit_Board_Elite .get(1));
			RM.Press.addRecipe2(T, F, F, F, T, 16, 64, IL.Circuit_Plate_Gold .get(1), IL.Circuit_Part_Ultimate .get(4), IL.Circuit_Board_Elite .get(1));
			RM.Press.addRecipe2(T, F, F, F, T, 16, 64, IL.Circuit_Plate_Platinum .get(1), IL.Circuit_Part_Good .get(4), IL.Circuit_Board_Good .get(1));
			RM.Press.addRecipe2(T, F, F, F, T, 16, 64, IL.Circuit_Plate_Platinum .get(1), IL.Circuit_Part_Advanced .get(4), IL.Circuit_Board_Advanced .get(1));
			RM.Press.addRecipe2(T, F, F, F, T, 16, 64, IL.Circuit_Plate_Platinum .get(1), IL.Circuit_Part_Elite .get(4), IL.Circuit_Board_Elite .get(1));
			RM.Press.addRecipe2(T, F, F, F, T, 16, 64, IL.Circuit_Plate_Platinum .get(1), IL.Circuit_Part_Master .get(4), IL.Circuit_Board_Master .get(1));
			RM.Press.addRecipe2(T, F, F, F, T, 16, 64, IL.Circuit_Plate_Platinum .get(1), IL.Circuit_Part_Ultimate .get(4), IL.Circuit_Board_Ultimate .get(1));
			RM.Press.addRecipe2(T, F, F, F, T, 16, 64, IL.Circuit_Plate_Magic .get(1), IL.Circuit_Part_Magic .get(4), IL.Circuit_Board_Magic .get(1));
			RM.Press.addRecipe2(T, F, F, F, T, 16, 64, IL.Circuit_Plate_Enderium .get(1), IL.Circuit_Part_Enderium .get(4), IL.Circuit_Board_Enderium .get(1));
			RM.Press.addRecipe2(T, F, F, F, T, 16, 64, IL.Circuit_Plate_Signalum .get(1), IL.Circuit_Part_Signalum .get(4), IL.Circuit_Board_Signalum .get(1));
			RM.Press.addRecipe2(T, F, F, F, T, 16, 64, IL.Circuit_Plate_HSLA .get(1), IL.Circuit_Part_EnderPearl .get(4), IL.Circuit_Board_HSLA_Circuit .get(1));
			RM.Press.addRecipe2(T, F, F, F, T, 16, 64, IL.Circuit_Plate_HSLA .get(1), IL.Circuit_Part_EnderEye .get(4), IL.Circuit_Board_Power_Module .get(1));
			RM.Bath.addRecipe1(T, F, F, F, T, 0, 64, IL.Circuit_Board_Good.get(1), MT.Sn.liquid(U2, T), NF, IL.Circuit_Good.get(1));
			RM.Bath.addRecipe1(T, F, F, F, T, 0, 64, IL.Circuit_Board_Good.get(1), MT.SolderingAlloy.liquid(U2, T), NF, IL.Circuit_Good.get(1));
			RM.Bath.addRecipe1(T, F, F, F, T, 0, 64, IL.Circuit_Board_Advanced.get(1), MT.Sn.liquid(U2, T), NF, IL.Circuit_Good.get(1));
			RM.Bath.addRecipe1(T, F, F, F, T, 0, 64, IL.Circuit_Board_Advanced.get(1), MT.SolderingAlloy.liquid(U2, T), NF, IL.Circuit_Advanced.get(1));
			RM.Bath.addRecipe1(T, F, F, F, T, 0, 64, IL.Circuit_Board_Elite.get(1), MT.Pb.liquid(U2, T), NF, IL.Circuit_Good.get(1));
			RM.Bath.addRecipe1(T, F, F, F, T, 0, 64, IL.Circuit_Board_Elite.get(1), MT.Sn.liquid(U2, T), NF, IL.Circuit_Advanced.get(1));
			RM.Bath.addRecipe1(T, F, F, F, T, 0, 64, IL.Circuit_Board_Elite.get(1), MT.SolderingAlloy.liquid(U2, T), NF, IL.Circuit_Elite.get(1));
			RM.Bath.addRecipe1(T, F, F, F, T, 0, 64, IL.Circuit_Board_Master.get(1), MT.Pb.liquid(U2, T), NF, IL.Circuit_Advanced.get(1));
			RM.Bath.addRecipe1(T, F, F, F, T, 0, 64, IL.Circuit_Board_Master.get(1), MT.Sn.liquid(U2, T), NF, IL.Circuit_Elite.get(1));
			RM.Bath.addRecipe1(T, F, F, F, T, 0, 64, IL.Circuit_Board_Master.get(1), MT.SolderingAlloy.liquid(U2, T), NF, IL.Circuit_Master.get(1));
			RM.Bath.addRecipe1(T, F, F, F, T, 0, 64, IL.Circuit_Board_Ultimate.get(1), MT.Pb.liquid(U2, T), NF, IL.Circuit_Elite.get(1));
			RM.Bath.addRecipe1(T, F, F, F, T, 0, 64, IL.Circuit_Board_Ultimate.get(1), MT.Sn.liquid(U2, T), NF, IL.Circuit_Master.get(1));
			RM.Bath.addRecipe1(T, F, F, F, T, 0, 64, IL.Circuit_Board_Ultimate.get(1), MT.SolderingAlloy.liquid(U2, T), NF, IL.Circuit_Ultimate.get(1));
			RM.Bath.addRecipe1(T, F, F, F, T, 0, 64, IL.Circuit_Board_Magic.get(1), MT.SolderingAlloy.liquid(U2, T), NF, IL.Circuit_Magic.get(1));
			RM.Bath.addRecipe1(T, F, F, F, T, 0, 64, IL.Circuit_Board_Enderium.get(1), MT.SolderingAlloy.liquid(U2, T), NF, IL.Circuit_Enderium.get(1));
			RM.Bath.addRecipe1(T, F, F, F, T, 0, 64, IL.Circuit_Board_Signalum.get(1), MT.Pb.liquid(U2, T), NF, IL.Circuit_Signalum.get(1));
			RM.Bath.addRecipe1(T, F, F, F, T, 0, 64, IL.Circuit_Board_Signalum.get(1), MT.Sn.liquid(U2, T), NF, IL.Circuit_Signalum.get(1));
			RM.Bath.addRecipe1(T, F, F, F, T, 0, 64, IL.Circuit_Board_Signalum.get(1), MT.SolderingAlloy.liquid(U2, T), NF, IL.Circuit_Signalum.get(1));
			RM.Bath.addRecipe1(T, F, F, F, T, 0, 64, IL.Circuit_Board_BC_Redstone.get(1), MT.SolderingAlloy.liquid(U2, T), NF, IL.Circuit_BC_Redstone.get(1));
			RM.Bath.addRecipe1(T, F, F, F, T, 0, 64, IL.Circuit_Board_BC_Iron.get(1), MT.SolderingAlloy.liquid(U2, T), NF, IL.Circuit_BC_Iron.get(1));
			RM.Bath.addRecipe1(T, F, F, F, T, 0, 64, IL.Circuit_Board_BC_Gold.get(1), MT.SolderingAlloy.liquid(U2, T), NF, IL.Circuit_BC_Gold.get(1));
			RM.Bath.addRecipe1(T, F, F, F, T, 0, 64, IL.Circuit_Board_BC_Diamond.get(1), MT.SolderingAlloy.liquid(U2, T), NF, IL.Circuit_BC_Diamond.get(1));
			RM.Bath.addRecipe1(T, F, F, F, T, 0, 64, IL.Circuit_Board_BC_Ender.get(1), MT.SolderingAlloy.liquid(U2, T), NF, IL.Circuit_BC_Ender.get(1));
			RM.Bath.addRecipe1(T, F, F, F, T, 0, 64, IL.Circuit_Board_BC_Quartz.get(1), MT.SolderingAlloy.liquid(U2, T), NF, IL.Circuit_BC_Quartz.get(1));
			RM.Bath.addRecipe1(T, F, F, F, T, 0, 64, IL.Circuit_Board_BC_Comparator.get(1), MT.SolderingAlloy.liquid(U2, T), NF, IL.Circuit_BC_Comparator.get(1));
			RM.Bath.addRecipe1(T, F, F, F, T, 0, 64, IL.Circuit_Board_BC_Emerald.get(1), MT.SolderingAlloy.liquid(U2, T), NF, IL.Circuit_BC_Emerald.get(1));
			CR.shaped(IL.Processor_Crystal_Empty.get(1), CR.DEF_REV, "CLC", "LBL", "CLC", 'C', OD_CIRCUITS[6], 'B', IL.Circuit_Plate_Platinum, 'L', IL.Comp_Laser_Gas_HeNe);
			RM.Press.addRecipe2(T, F, F, F, T, 16, 16, IL.Processor_Crystal_Empty.get(1), IL.Circuit_Crystal_Diamond .get(1), IL.Processor_Crystal_Diamond .get(1));
			RM.Press.addRecipe2(T, F, F, F, T, 16, 16, IL.Processor_Crystal_Empty.get(1), IL.Circuit_Crystal_Ruby .get(1), IL.Processor_Crystal_Ruby .get(1));
			RM.Press.addRecipe2(T, F, F, F, T, 16, 16, IL.Processor_Crystal_Empty.get(1), IL.Circuit_Crystal_Emerald .get(1), IL.Processor_Crystal_Emerald .get(1));
			RM.Press.addRecipe2(T, F, F, F, T, 16, 16, IL.Processor_Crystal_Empty.get(1), IL.Circuit_Crystal_Sapphire.get(1), IL.Processor_Crystal_Sapphire.get(1));
		});
	}
}

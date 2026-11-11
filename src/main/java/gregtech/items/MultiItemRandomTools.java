/**
 * Copyright (c) 2025 GregTech-6 Team
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
 * Modified in 2026 for the GregTech 6 NeoForge port
 * (https://github.com/wolfram0108/gregtech6_w): ported from Minecraft 1.7.10 / Forge
 * to Minecraft 26.1.2 / NeoForge.
 */

package gregtech.items;

import gregapi.GT_API;
import gregapi.code.IItemContainer;
import gregapi.code.ItemNBT;
import gregapi.cover.covers.CoverTextureCanvas;
import gregapi.data.*;
import gregapi.item.CreativeTab;
import gregapi.item.IItemRottable;
import gregapi.item.IPrefixItem;
import gregapi.item.multiitem.MultiItemRandomWithCompat;
import gregapi.item.multiitem.behaviors.*;
import gregapi.item.multiitem.energy.EnergyStat;
import gregapi.item.multiitem.energy.EnergyStatDebug;
import gregapi.old.Textures;
import gregapi.oredict.OreDictItemData;
import gregapi.oredict.OreDictMaterial;
import gregapi.render.BlockTextureDefault;
import gregapi.util.CR;
import gregapi.util.OM;
import gregapi.util.ST;
import gregapi.util.UT;
import gregtech.items.behaviors.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.Random;

import static gregapi.data.CS.*;
import static gregapi.data.OP.chunkGt;

public class MultiItemRandomTools extends MultiItemRandomWithCompat implements IItemRottable {
	private MultiItemRandomTools(MultiItemRandomTools aFamily, short aMeta) {super(aFamily, aMeta);}
	@Override protected gregapi.item.multiitem.MultiItemRandom createVariant(short aMeta) {return new MultiItemRandomTools(this, aMeta);}
	public MultiItemRandomTools(String aModID, String aUnlocalized) {
		super(aModID, aUnlocalized);
		new gregapi.item.CreativeTab(getUnlocalizedName(), "GregTech: Equipment", this, (short)5008); // F16 creative tab: own GT tab (icon+displayItems), registered by CreativeTabsGT on RegisterEvent<CreativeModeTab>. 1:1.
	}
	
	protected void defineItems(gregapi.item.ItemTypeDefinitions aItems) {
		aItems.add(0, aID -> {IL.Compound_Bronze       .set(addItem(  aID, "Bronze Compound"        , "Put in Furnace to smelt it" , new OreDictItemData(MT.Bronze       , U9), TC.stack(TC.METALLUM, 1)));});
		aItems.add(1, aID -> {IL.Compound_Brass        .set(addItem(  aID, "Brass Compound"         , "Put in Furnace to smelt it" , new OreDictItemData(MT.Brass        , U9), TC.stack(TC.METALLUM, 1)));});
		aItems.add(2, aID -> {IL.Compound_BismuthBronze.set(addItem(  aID, "Bismuth Bronze Compound", "Put in Furnace to smelt it" , new OreDictItemData(MT.BismuthBronze, U9), TC.stack(TC.METALLUM, 1)));});
		aItems.add(3, aID -> {IL.Compound_ArsenicCopper.set(addItem(  aID, "Arsenic Copper Compound", "Put in Furnace to smelt it" , new OreDictItemData(MT.ArsenicCopper, U9), TC.stack(TC.METALLUM, 1)));});
		aItems.add(4, aID -> {
			IL.Compound_ArsenicBronze.set(addItem(  aID, "Arsenic Bronze Compound", "Put in Furnace to smelt it" , new OreDictItemData(MT.ArsenicBronze, U9), TC.stack(TC.METALLUM, 1)));
		
		RM.add_smelting(IL.Compound_Bronze       .get(1), OP.nugget.mat(MT.Bronze       , 1), F, F, T);
		RM.add_smelting(IL.Compound_Brass        .get(1), OP.nugget.mat(MT.Brass        , 1), F, F, T);
		RM.add_smelting(IL.Compound_BismuthBronze.get(1), OP.nugget.mat(MT.BismuthBronze, 1), F, F, T);
		RM.add_smelting(IL.Compound_ArsenicCopper.get(1), OP.nugget.mat(MT.ArsenicCopper, 1), F, F, T);
		RM.add_smelting(IL.Compound_ArsenicBronze.get(1), OP.nugget.mat(MT.ArsenicBronze, 1), F, F, T);
		});
		
		aItems.add(899, aID -> {
			IL.Porcelain_Cup_Raw               .set(addItem(aID, "Modeled Porcelain Cup"    , "Put in Furnace to harden", new OreDictItemData(MT.Porcelain, U), TC.stack(TC.TERRA, 2), TC.stack(TC.VACUOS, 1))); CR.shapeless(OP.dust.mat(MT.Porcelain, 1), CR.DEF_NCC, new Object[] {last()});
		
		CR.shaped(IL.Porcelain_Cup_Raw   .get(1), CR.DEF_NCC, "kPR"              , 'P', OP.dust.dat(MT.Porcelain), 'R', OreDictToolNames.rollingpin);
		});

		aItems.add(900, aID -> {IL.Ceramic_Ingot_Mold_Raw          .set(addItem(aID, "Clay Ingot Mold"          , "Put in Furnace to harden", new OreDictItemData(MT.Clay, U*5), TC.stack(TC.TERRA, 2), TC.stack(TC.GELUM     , 1))); CR.shapeless(ST.make(Items.CLAY_BALL, 5, 0), CR.DEF_NCC, new Object[] {last()});});
		aItems.add(901, aID -> {IL.Ceramic_Chunk_Mold_Raw          .set(addItem(aID, "Clay Chunk Mold"          , "Put in Furnace to harden", new OreDictItemData(MT.Clay, U*5), TC.stack(TC.TERRA, 2), TC.stack(TC.GELUM     , 1))); CR.shapeless(ST.make(Items.CLAY_BALL, 5, 0), CR.DEF_NCC, new Object[] {last()});});
		aItems.add(902, aID -> {IL.Ceramic_Plate_Mold_Raw          .set(addItem(aID, "Clay Plate Mold"          , "Put in Furnace to harden", new OreDictItemData(MT.Clay, U*5), TC.stack(TC.TERRA, 2), TC.stack(TC.GELUM     , 1))); CR.shapeless(ST.make(Items.CLAY_BALL, 5, 0), CR.DEF_NCC, new Object[] {last()});});
		aItems.add(903, aID -> {IL.Ceramic_Tiny_Plate_Mold_Raw     .set(addItem(aID, "Clay Tiny Plate Mold"     , "Put in Furnace to harden", new OreDictItemData(MT.Clay, U*5), TC.stack(TC.TERRA, 2), TC.stack(TC.GELUM     , 1))); CR.shapeless(ST.make(Items.CLAY_BALL, 5, 0), CR.DEF_NCC, new Object[] {last()});});
		aItems.add(904, aID -> {IL.Ceramic_Bolt_Mold_Raw           .set(addItem(aID, "Clay Bolt Mold"           , "Put in Furnace to harden", new OreDictItemData(MT.Clay, U*5), TC.stack(TC.TERRA, 2), TC.stack(TC.GELUM     , 1))); CR.shapeless(ST.make(Items.CLAY_BALL, 5, 0), CR.DEF_NCC, new Object[] {last()});});
		aItems.add(905, aID -> {IL.Ceramic_Rod_Mold_Raw            .set(addItem(aID, "Clay Rod Mold"            , "Put in Furnace to harden", new OreDictItemData(MT.Clay, U*5), TC.stack(TC.TERRA, 2), TC.stack(TC.GELUM     , 1))); CR.shapeless(ST.make(Items.CLAY_BALL, 5, 0), CR.DEF_NCC, new Object[] {last()});});
		aItems.add(906, aID -> {IL.Ceramic_Long_Rod_Mold_Raw       .set(addItem(aID, "Clay Long Rod Mold"       , "Put in Furnace to harden", new OreDictItemData(MT.Clay, U*5), TC.stack(TC.TERRA, 2), TC.stack(TC.GELUM     , 1))); CR.shapeless(ST.make(Items.CLAY_BALL, 5, 0), CR.DEF_NCC, new Object[] {last()});});
		aItems.add(907, aID -> {IL.Ceramic_Item_Casing_Mold_Raw    .set(addItem(aID, "Clay Item Casing Mold"    , "Put in Furnace to harden", new OreDictItemData(MT.Clay, U*5), TC.stack(TC.TERRA, 2), TC.stack(TC.GELUM     , 1))); CR.shapeless(ST.make(Items.CLAY_BALL, 5, 0), CR.DEF_NCC, new Object[] {last()});});
		aItems.add(908, aID -> {IL.Ceramic_Ring_Mold_Raw           .set(addItem(aID, "Clay Ring Mold"           , "Put in Furnace to harden", new OreDictItemData(MT.Clay, U*5), TC.stack(TC.TERRA, 2), TC.stack(TC.GELUM     , 1))); CR.shapeless(ST.make(Items.CLAY_BALL, 5, 0), CR.DEF_NCC, new Object[] {last()});});
		aItems.add(909, aID -> {IL.Ceramic_Gear_Mold_Raw           .set(addItem(aID, "Clay Gear Mold"           , "Put in Furnace to harden", new OreDictItemData(MT.Clay, U*5), TC.stack(TC.TERRA, 2), TC.stack(TC.GELUM     , 1))); CR.shapeless(ST.make(Items.CLAY_BALL, 5, 0), CR.DEF_NCC, new Object[] {last()});});
		aItems.add(910, aID -> {IL.Ceramic_Small_Gear_Mold_Raw     .set(addItem(aID, "Clay Small Gear Mold"     , "Put in Furnace to harden", new OreDictItemData(MT.Clay, U*5), TC.stack(TC.TERRA, 2), TC.stack(TC.GELUM     , 1))); CR.shapeless(ST.make(Items.CLAY_BALL, 5, 0), CR.DEF_NCC, new Object[] {last()});});
		aItems.add(911, aID -> {IL.Ceramic_Sword_Mold_Raw          .set(addItem(aID, "Clay Sword Mold"          , "Put in Furnace to harden", new OreDictItemData(MT.Clay, U*5), TC.stack(TC.TERRA, 2), TC.stack(TC.GELUM     , 1))); CR.shapeless(ST.make(Items.CLAY_BALL, 5, 0), CR.DEF_NCC, new Object[] {last()});});
		aItems.add(912, aID -> {IL.Ceramic_Pickaxe_Mold_Raw        .set(addItem(aID, "Clay Pickaxe Mold"        , "Put in Furnace to harden", new OreDictItemData(MT.Clay, U*5), TC.stack(TC.TERRA, 2), TC.stack(TC.GELUM     , 1))); CR.shapeless(ST.make(Items.CLAY_BALL, 5, 0), CR.DEF_NCC, new Object[] {last()});});
		aItems.add(913, aID -> {IL.Ceramic_Spade_Mold_Raw          .set(addItem(aID, "Clay Spade Mold"          , "Put in Furnace to harden", new OreDictItemData(MT.Clay, U*5), TC.stack(TC.TERRA, 2), TC.stack(TC.GELUM     , 1))); CR.shapeless(ST.make(Items.CLAY_BALL, 5, 0), CR.DEF_NCC, new Object[] {last()});});
		aItems.add(914, aID -> {IL.Ceramic_Shovel_Mold_Raw         .set(addItem(aID, "Clay Shovel Mold"         , "Put in Furnace to harden", new OreDictItemData(MT.Clay, U*5), TC.stack(TC.TERRA, 2), TC.stack(TC.GELUM     , 1))); CR.shapeless(ST.make(Items.CLAY_BALL, 5, 0), CR.DEF_NCC, new Object[] {last()});});
		aItems.add(915, aID -> {IL.Ceramic_Universal_Spade_Mold_Raw.set(addItem(aID, "Clay Universal Spade Mold", "Put in Furnace to harden", new OreDictItemData(MT.Clay, U*5), TC.stack(TC.TERRA, 2), TC.stack(TC.GELUM     , 1))); CR.shapeless(ST.make(Items.CLAY_BALL, 5, 0), CR.DEF_NCC, new Object[] {last()});});
		aItems.add(916, aID -> {IL.Ceramic_Axe_Mold_Raw            .set(addItem(aID, "Clay Axe Mold"            , "Put in Furnace to harden", new OreDictItemData(MT.Clay, U*5), TC.stack(TC.TERRA, 2), TC.stack(TC.GELUM     , 1))); CR.shapeless(ST.make(Items.CLAY_BALL, 5, 0), CR.DEF_NCC, new Object[] {last()});});
		aItems.add(917, aID -> {IL.Ceramic_Double_Axe_Mold_Raw     .set(addItem(aID, "Clay Double Axe Mold"     , "Put in Furnace to harden", new OreDictItemData(MT.Clay, U*5), TC.stack(TC.TERRA, 2), TC.stack(TC.GELUM     , 1))); CR.shapeless(ST.make(Items.CLAY_BALL, 5, 0), CR.DEF_NCC, new Object[] {last()});});
		aItems.add(918, aID -> {IL.Ceramic_Saw_Mold_Raw            .set(addItem(aID, "Clay Saw Mold"            , "Put in Furnace to harden", new OreDictItemData(MT.Clay, U*5), TC.stack(TC.TERRA, 2), TC.stack(TC.GELUM     , 1))); CR.shapeless(ST.make(Items.CLAY_BALL, 5, 0), CR.DEF_NCC, new Object[] {last()});});
		aItems.add(919, aID -> {IL.Ceramic_Hammer_Mold_Raw         .set(addItem(aID, "Clay Hammer Mold"         , "Put in Furnace to harden", new OreDictItemData(MT.Clay, U*5), TC.stack(TC.TERRA, 2), TC.stack(TC.GELUM     , 1))); CR.shapeless(ST.make(Items.CLAY_BALL, 5, 0), CR.DEF_NCC, new Object[] {last()});});
		aItems.add(920, aID -> {IL.Ceramic_File_Mold_Raw           .set(addItem(aID, "Clay File Mold"           , "Put in Furnace to harden", new OreDictItemData(MT.Clay, U*5), TC.stack(TC.TERRA, 2), TC.stack(TC.GELUM     , 1))); CR.shapeless(ST.make(Items.CLAY_BALL, 5, 0), CR.DEF_NCC, new Object[] {last()});});
		aItems.add(921, aID -> {IL.Ceramic_Screwdriver_Mold_Raw    .set(addItem(aID, "Clay Screwdriver Mold"    , "Put in Furnace to harden", new OreDictItemData(MT.Clay, U*5), TC.stack(TC.TERRA, 2), TC.stack(TC.GELUM     , 1))); CR.shapeless(ST.make(Items.CLAY_BALL, 5, 0), CR.DEF_NCC, new Object[] {last()});});
		aItems.add(922, aID -> {IL.Ceramic_Chisel_Mold_Raw         .set(addItem(aID, "Clay Chisel Mold"         , "Put in Furnace to harden", new OreDictItemData(MT.Clay, U*5), TC.stack(TC.TERRA, 2), TC.stack(TC.GELUM     , 1))); CR.shapeless(ST.make(Items.CLAY_BALL, 5, 0), CR.DEF_NCC, new Object[] {last()});});
		aItems.add(923, aID -> {IL.Ceramic_Arrow_Mold_Raw          .set(addItem(aID, "Clay Arrow Mold"          , "Put in Furnace to harden", new OreDictItemData(MT.Clay, U*5), TC.stack(TC.TERRA, 2), TC.stack(TC.GELUM     , 1))); CR.shapeless(ST.make(Items.CLAY_BALL, 5, 0), CR.DEF_NCC, new Object[] {last()});});
		aItems.add(924, aID -> {IL.Ceramic_Hoe_Mold_Raw            .set(addItem(aID, "Clay Hoe Mold"            , "Put in Furnace to harden", new OreDictItemData(MT.Clay, U*5), TC.stack(TC.TERRA, 2), TC.stack(TC.GELUM     , 1))); CR.shapeless(ST.make(Items.CLAY_BALL, 5, 0), CR.DEF_NCC, new Object[] {last()});});
		aItems.add(925, aID -> {IL.Ceramic_Sense_Mold_Raw          .set(addItem(aID, "Clay Sense Mold"          , "Put in Furnace to harden", new OreDictItemData(MT.Clay, U*5), TC.stack(TC.TERRA, 2), TC.stack(TC.GELUM     , 1))); CR.shapeless(ST.make(Items.CLAY_BALL, 5, 0), CR.DEF_NCC, new Object[] {last()});});
		aItems.add(926, aID -> {IL.Ceramic_Plow_Mold_Raw           .set(addItem(aID, "Clay Plow Mold"           , "Put in Furnace to harden", new OreDictItemData(MT.Clay, U*5), TC.stack(TC.TERRA, 2), TC.stack(TC.GELUM     , 1))); CR.shapeless(ST.make(Items.CLAY_BALL, 5, 0), CR.DEF_NCC, new Object[] {last()});});
		aItems.add(927, aID -> {IL.Ceramic_Builderwand_Mold_Raw    .set(addItem(aID, "Clay Builder's Wand Mold" , "Put in Furnace to harden", new OreDictItemData(MT.Clay, U*5), TC.stack(TC.TERRA, 2), TC.stack(TC.GELUM     , 1))); CR.shapeless(ST.make(Items.CLAY_BALL, 5, 0), CR.DEF_NCC, new Object[] {last()});});
		aItems.add(928, aID -> {IL.Ceramic_Nugget_Mold_Raw         .set(addItem(aID, "Clay Nugget Mold"         , "Put in Furnace to harden", new OreDictItemData(MT.Clay, U*5), TC.stack(TC.TERRA, 2), TC.stack(TC.GELUM     , 1))); CR.shapeless(ST.make(Items.CLAY_BALL, 5, 0), CR.DEF_NCC, new Object[] {last()});});
		aItems.add(929, aID -> {IL.Ceramic_Billet_Mold_Raw         .set(addItem(aID, "Clay Billet Mold"         , "Put in Furnace to harden", new OreDictItemData(MT.Clay, U*5), TC.stack(TC.TERRA, 2), TC.stack(TC.GELUM     , 1))); CR.shapeless(ST.make(Items.CLAY_BALL, 5, 0), CR.DEF_NCC, new Object[] {last()});});

		aItems.add(987, aID -> {IL.Ceramic_Tap_Raw                 .set(addItem(aID, "Clay Tap"                 , "Put in Furnace to harden", new OreDictItemData(MT.Clay, U*3), TC.stack(TC.TERRA, 2), TC.stack(TC.MOTUS     , 1))); CR.shapeless(ST.make(Items.CLAY_BALL, 3, 0), CR.DEF_NCC, new Object[] {last()});});
		aItems.add(988, aID -> {IL.Ceramic_Funnel_Raw              .set(addItem(aID, "Clay Funnel"              , "Put in Furnace to harden", new OreDictItemData(MT.Clay, U*3), TC.stack(TC.TERRA, 2), TC.stack(TC.MOTUS     , 1))); CR.shapeless(ST.make(Items.CLAY_BALL, 3, 0), CR.DEF_NCC, new Object[] {last()});});
		aItems.add(989, aID -> {IL.Ceramic_Crucible_Raw            .set(addItem(aID, "Clay Crucible"            , "Put in Furnace to harden", new OreDictItemData(MT.Clay, U*7), TC.stack(TC.TERRA, 2), TC.stack(TC.IGNIS     , 1))); CR.shapeless(ST.make(Items.CLAY_BALL, 7, 0), CR.DEF_NCC, new Object[] {last()});});
		aItems.add(990, aID -> {IL.Ceramic_Basin_Raw               .set(addItem(aID, "Clay Basin"               , "Put in Furnace to harden", new OreDictItemData(MT.Clay, U*5), TC.stack(TC.TERRA, 2), TC.stack(TC.GELUM     , 1))); CR.shapeless(ST.make(Items.CLAY_BALL, 5, 0), CR.DEF_NCC, new Object[] {last()});});
		aItems.add(991, aID -> {IL.Ceramic_Mold_Raw                .set(addItem(aID, "Clay Mold"                , "Put in Furnace to harden", new OreDictItemData(MT.Clay, U*5), TC.stack(TC.TERRA, 2), TC.stack(TC.GELUM     , 1))); CR.shapeless(ST.make(Items.CLAY_BALL, 5, 0), CR.DEF_NCC, new Object[] {last()});});
		aItems.add(992, aID -> {IL.Ceramic_Faucet_Raw              .set(addItem(aID, "Clay Faucet"              , "Put in Furnace to harden", new OreDictItemData(MT.Clay, U*3), TC.stack(TC.TERRA, 2), TC.stack(TC.MOTUS     , 1))); CR.shapeless(ST.make(Items.CLAY_BALL, 3, 0), CR.DEF_NCC, new Object[] {last()});});
		aItems.add(993, aID -> {IL.Ceramic_Crossing_Raw            .set(addItem(aID, "Clay Crossing"            , "Put in Furnace to harden", new OreDictItemData(MT.Clay, U*5), TC.stack(TC.TERRA, 2), TC.stack(TC.ITER      , 1))); CR.shapeless(ST.make(Items.CLAY_BALL, 5, 0), CR.DEF_NCC, new Object[] {last()});});
		aItems.add(994, aID -> {IL.Juicer_Raw                      .set(addItem(aID, "Clay Juicer"              , "Put in Furnace to harden", new OreDictItemData(MT.Clay, U*4), TC.stack(TC.TERRA, 2), TC.stack(TC.LIMUS     , 1))); CR.shapeless(ST.make(Items.CLAY_BALL, 4, 0), CR.DEF_NCC, new Object[] {last()});});
		aItems.add(995, aID -> {IL.Ceramic_Bowl_Raw                .set(addItem(aID, "Clay Bowl"                , "Put in Furnace to harden", new OreDictItemData(MT.Clay, U*5), TC.stack(TC.TERRA, 2), TC.stack(TC.PERMUTATIO, 1))); CR.shapeless(ST.make(Items.CLAY_BALL, 5, 0), CR.DEF_NCC, new Object[] {last()});});
		aItems.add(996, aID -> {IL.Ceramic_Jug_Raw                 .set(addItem(aID, "Clay Jug"                 , "Put in Furnace to harden", new OreDictItemData(MT.Clay, U*6), TC.stack(TC.TERRA, 2), TC.stack(TC.VACUOS    , 1))); CR.shapeless(ST.make(Items.CLAY_BALL, 6, 0), CR.DEF_NCC, new Object[] {last()});});
		aItems.add(997, aID -> {
			IL.Measuring_Pot_Raw               .set(addItem(aID, "Clay Measuring Pot"       , "Put in Furnace to harden", new OreDictItemData(MT.Clay, U*4), TC.stack(TC.TERRA, 2), TC.stack(TC.VACUOS    , 1))); CR.shapeless(ST.make(Items.CLAY_BALL, 4, 0), CR.DEF_NCC, new Object[] {last()});
		
		CR.shaped(IL.Ceramic_Tap_Raw     .get(1), CR.DEF_NCC, "CCR", "kC "       , 'C', OD.itemClay, 'R', OreDictToolNames.rollingpin);
		CR.shaped(IL.Ceramic_Funnel_Raw  .get(1), CR.DEF_NCC, "CRC", "kC "       , 'C', OD.itemClay, 'R', OreDictToolNames.rollingpin);
		CR.shaped(IL.Ceramic_Crucible_Raw.get(1), CR.DEF_NCC, "CkC", "CRC", "CCC", 'C', OD.itemClay, 'R', OreDictToolNames.rollingpin);
		CR.shaped(IL.Ceramic_Basin_Raw   .get(1), CR.DEF_NCC, "CkC", "CRC", " C ", 'C', OD.itemClay, 'R', OreDictToolNames.rollingpin);
		CR.shaped(IL.Ceramic_Mold_Raw    .get(1), CR.DEF_NCC, "C C", "CCC", "k R", 'C', OD.itemClay, 'R', OreDictToolNames.rollingpin);
		CR.shaped(IL.Ceramic_Faucet_Raw  .get(1), CR.DEF_NCC, "C C", "kCR"       , 'C', OD.itemClay, 'R', OreDictToolNames.rollingpin);
		CR.shaped(IL.Ceramic_Crossing_Raw.get(1), CR.DEF_NCC, "kCR", "CCC", " C ", 'C', OD.itemClay, 'R', OreDictToolNames.rollingpin);
		CR.shaped(IL.Ceramic_Crossing_Raw.get(1), CR.DEF_NCC, " C ", "CCC", "kCR", 'C', OD.itemClay, 'R', OreDictToolNames.rollingpin);
		CR.shaped(IL.Juicer_Raw          .get(1), CR.DEF_NCC, "kCR", "CCC"       , 'C', OD.itemClay, 'R', OreDictToolNames.rollingpin);
		CR.shaped(IL.Ceramic_Bowl_Raw    .get(1), CR.DEF_NCC, "k R", "C C", "CCC", 'C', OD.itemClay, 'R', OreDictToolNames.rollingpin);
		CR.shaped(IL.Ceramic_Jug_Raw     .get(1), CR.DEF_NCC, "kCR", "C C", "CCC", 'C', OD.itemClay, 'R', OreDictToolNames.rollingpin);
		CR.shaped(IL.Measuring_Pot_Raw   .get(1), CR.DEF_NCC, "CkC", "CCR"       , 'C', OD.itemClay, 'R', OreDictToolNames.rollingpin);
		
		CR.shapeless(IL.Ceramic_Ingot_Mold_Raw          .get(1), CR.DEF_NCC, new Object[] {IL.Ceramic_Mold_Raw, OD.itemMudBrick});
		CR.shapeless(IL.Ceramic_Ingot_Mold_Raw          .get(1), CR.DEF_NCC, new Object[] {IL.Ceramic_Mold_Raw, OP.ingot.dat(MT.Brick)});
		CR.shapeless(IL.Ceramic_Plate_Mold_Raw          .get(1), CR.DEF_NCC, new Object[] {IL.Ceramic_Mold_Raw, OD.paneGlass});
		CR.shapeless(IL.Ceramic_Plate_Mold_Raw          .get(1), CR.DEF_NCC, new Object[] {IL.Ceramic_Mold_Raw, OD.plankAnyWood});
		CR.shapeless(IL.Ceramic_Arrow_Mold_Raw          .get(1), CR.DEF_NCC, new Object[] {IL.Ceramic_Mold_Raw, OD.itemFlint});
		CR.shapeless(IL.Ceramic_Arrow_Mold_Raw          .get(1), CR.DEF_NCC, new Object[] {IL.Ceramic_Mold_Raw, Items.ARROW});
		CR.shapeless(IL.Ceramic_Sword_Mold_Raw          .get(1), CR.DEF_NCC, new Object[] {IL.Ceramic_Mold_Raw, Items.WOODEN_SWORD});
		CR.shapeless(IL.Ceramic_Pickaxe_Mold_Raw        .get(1), CR.DEF_NCC, new Object[] {IL.Ceramic_Mold_Raw, Items.WOODEN_PICKAXE});
		CR.shapeless(IL.Ceramic_Shovel_Mold_Raw         .get(1), CR.DEF_NCC, new Object[] {IL.Ceramic_Mold_Raw, Items.WOODEN_SHOVEL});
		CR.shapeless(IL.Ceramic_Axe_Mold_Raw            .get(1), CR.DEF_NCC, new Object[] {IL.Ceramic_Mold_Raw, Items.WOODEN_AXE});
		CR.shapeless(IL.Ceramic_Hoe_Mold_Raw            .get(1), CR.DEF_NCC, new Object[] {IL.Ceramic_Mold_Raw, Items.WOODEN_HOE});
		CR.shapeless(IL.Ceramic_Sword_Mold_Raw          .get(1), CR.DEF_NCC, new Object[] {IL.Ceramic_Mold_Raw, Items.STONE_SWORD});
		CR.shapeless(IL.Ceramic_Pickaxe_Mold_Raw        .get(1), CR.DEF_NCC, new Object[] {IL.Ceramic_Mold_Raw, Items.STONE_PICKAXE});
		CR.shapeless(IL.Ceramic_Shovel_Mold_Raw         .get(1), CR.DEF_NCC, new Object[] {IL.Ceramic_Mold_Raw, Items.STONE_SHOVEL});
		CR.shapeless(IL.Ceramic_Axe_Mold_Raw            .get(1), CR.DEF_NCC, new Object[] {IL.Ceramic_Mold_Raw, Items.STONE_AXE});
		CR.shapeless(IL.Ceramic_Hoe_Mold_Raw            .get(1), CR.DEF_NCC, new Object[] {IL.Ceramic_Mold_Raw, Items.STONE_HOE});
		CR.shapeless(IL.Ceramic_File_Mold_Raw           .get(1), CR.DEF_NCC, new Object[] {IL.Ceramic_Mold_Raw, OD.paneGlass, OD.paneGlass});
		CR.shapeless(IL.Ceramic_File_Mold_Raw           .get(1), CR.DEF_NCC, new Object[] {IL.Ceramic_Mold_Raw, OD.plankAnyWood, OD.plankAnyWood});
		CR.shapeless(IL.Ceramic_File_Mold_Raw           .get(1), CR.DEF_NCC, new Object[] {IL.Ceramic_Mold_Raw, ToolsGT.sMetaTool.make(ToolsGT.FILE)});
		CR.shapeless(IL.Ceramic_Sword_Mold_Raw          .get(1), CR.DEF_NCC, new Object[] {IL.Ceramic_Mold_Raw, ToolsGT.sMetaTool.make(ToolsGT.SWORD)});
		CR.shapeless(IL.Ceramic_Pickaxe_Mold_Raw        .get(1), CR.DEF_NCC, new Object[] {IL.Ceramic_Mold_Raw, ToolsGT.sMetaTool.make(ToolsGT.PICKAXE)});
		CR.shapeless(IL.Ceramic_Pickaxe_Mold_Raw        .get(1), CR.DEF_NCC, new Object[] {IL.Ceramic_Mold_Raw, ToolsGT.sMetaTool.make(ToolsGT.GEM_PICK)});
		CR.shapeless(IL.Ceramic_Pickaxe_Mold_Raw        .get(1), CR.DEF_NCC, new Object[] {IL.Ceramic_Mold_Raw, ToolsGT.sMetaTool.make(ToolsGT.CONSTRUCTION_PICK)});
		CR.shapeless(IL.Ceramic_Spade_Mold_Raw          .get(1), CR.DEF_NCC, new Object[] {IL.Ceramic_Mold_Raw, ToolsGT.sMetaTool.make(ToolsGT.SPADE)});
		CR.shapeless(IL.Ceramic_Shovel_Mold_Raw         .get(1), CR.DEF_NCC, new Object[] {IL.Ceramic_Mold_Raw, ToolsGT.sMetaTool.make(ToolsGT.SHOVEL)});
		CR.shapeless(IL.Ceramic_Universal_Spade_Mold_Raw.get(1), CR.DEF_NCC, new Object[] {IL.Ceramic_Mold_Raw, ToolsGT.sMetaTool.make(ToolsGT.UNIVERSALSPADE)});
		CR.shapeless(IL.Ceramic_Axe_Mold_Raw            .get(1), CR.DEF_NCC, new Object[] {IL.Ceramic_Mold_Raw, ToolsGT.sMetaTool.make(ToolsGT.AXE)});
		CR.shapeless(IL.Ceramic_Double_Axe_Mold_Raw     .get(1), CR.DEF_NCC, new Object[] {IL.Ceramic_Mold_Raw, ToolsGT.sMetaTool.make(ToolsGT.DOUBLE_AXE)});
		CR.shapeless(IL.Ceramic_Saw_Mold_Raw            .get(1), CR.DEF_NCC, new Object[] {IL.Ceramic_Mold_Raw, ToolsGT.sMetaTool.make(ToolsGT.SAW)});
		CR.shapeless(IL.Ceramic_Hammer_Mold_Raw         .get(1), CR.DEF_NCC, new Object[] {IL.Ceramic_Mold_Raw, ToolsGT.sMetaTool.make(ToolsGT.SOFTHAMMER)});
		CR.shapeless(IL.Ceramic_Hammer_Mold_Raw         .get(1), CR.DEF_NCC, new Object[] {IL.Ceramic_Mold_Raw, ToolsGT.sMetaTool.make(ToolsGT.HARDHAMMER)});
		CR.shapeless(IL.Ceramic_Screwdriver_Mold_Raw    .get(1), CR.DEF_NCC, new Object[] {IL.Ceramic_Mold_Raw, ToolsGT.sMetaTool.make(ToolsGT.SCREWDRIVER)});
		CR.shapeless(IL.Ceramic_Chisel_Mold_Raw         .get(1), CR.DEF_NCC, new Object[] {IL.Ceramic_Mold_Raw, ToolsGT.sMetaTool.make(ToolsGT.CHISEL)});
		CR.shapeless(IL.Ceramic_Hoe_Mold_Raw            .get(1), CR.DEF_NCC, new Object[] {IL.Ceramic_Mold_Raw, ToolsGT.sMetaTool.make(ToolsGT.HOE)});
		CR.shapeless(IL.Ceramic_Sense_Mold_Raw          .get(1), CR.DEF_NCC, new Object[] {IL.Ceramic_Mold_Raw, ToolsGT.sMetaTool.make(ToolsGT.SENSE)});
		CR.shapeless(IL.Ceramic_Plow_Mold_Raw           .get(1), CR.DEF_NCC, new Object[] {IL.Ceramic_Mold_Raw, ToolsGT.sMetaTool.make(ToolsGT.PLOW)});
		CR.shapeless(IL.Ceramic_Builderwand_Mold_Raw    .get(1), CR.DEF_NCC, new Object[] {IL.Ceramic_Mold_Raw, ToolsGT.sMetaTool.make(ToolsGT.BUILDERWAND)});
		
		
		for (IPrefixItem tItem : OP.toolHeadSword            .mRegisteredPrefixItems) CR.shapeless(IL.Ceramic_Sword_Mold_Raw          .get(1), CR.DEF_NCC, new Object[] {IL.Ceramic_Mold_Raw, tItem});
		for (IPrefixItem tItem : OP.toolHeadRawSword         .mRegisteredPrefixItems) CR.shapeless(IL.Ceramic_Sword_Mold_Raw          .get(1), CR.DEF_NCC, new Object[] {IL.Ceramic_Mold_Raw, tItem});
		for (IPrefixItem tItem : OP.toolHeadPickaxe          .mRegisteredPrefixItems) CR.shapeless(IL.Ceramic_Pickaxe_Mold_Raw        .get(1), CR.DEF_NCC, new Object[] {IL.Ceramic_Mold_Raw, tItem});
		for (IPrefixItem tItem : OP.toolHeadRawPickaxe       .mRegisteredPrefixItems) CR.shapeless(IL.Ceramic_Pickaxe_Mold_Raw        .get(1), CR.DEF_NCC, new Object[] {IL.Ceramic_Mold_Raw, tItem});
		for (IPrefixItem tItem : OP.toolHeadSpade            .mRegisteredPrefixItems) CR.shapeless(IL.Ceramic_Spade_Mold_Raw          .get(1), CR.DEF_NCC, new Object[] {IL.Ceramic_Mold_Raw, tItem});
		for (IPrefixItem tItem : OP.toolHeadRawSpade         .mRegisteredPrefixItems) CR.shapeless(IL.Ceramic_Spade_Mold_Raw          .get(1), CR.DEF_NCC, new Object[] {IL.Ceramic_Mold_Raw, tItem});
		for (IPrefixItem tItem : OP.toolHeadShovel           .mRegisteredPrefixItems) CR.shapeless(IL.Ceramic_Shovel_Mold_Raw         .get(1), CR.DEF_NCC, new Object[] {IL.Ceramic_Mold_Raw, tItem});
		for (IPrefixItem tItem : OP.toolHeadRawShovel        .mRegisteredPrefixItems) CR.shapeless(IL.Ceramic_Shovel_Mold_Raw         .get(1), CR.DEF_NCC, new Object[] {IL.Ceramic_Mold_Raw, tItem});
		for (IPrefixItem tItem : OP.toolHeadUniversalSpade   .mRegisteredPrefixItems) CR.shapeless(IL.Ceramic_Universal_Spade_Mold_Raw.get(1), CR.DEF_NCC, new Object[] {IL.Ceramic_Mold_Raw, tItem});
		for (IPrefixItem tItem : OP.toolHeadRawUniversalSpade.mRegisteredPrefixItems) CR.shapeless(IL.Ceramic_Universal_Spade_Mold_Raw.get(1), CR.DEF_NCC, new Object[] {IL.Ceramic_Mold_Raw, tItem});
		for (IPrefixItem tItem : OP.toolHeadAxe              .mRegisteredPrefixItems) CR.shapeless(IL.Ceramic_Axe_Mold_Raw            .get(1), CR.DEF_NCC, new Object[] {IL.Ceramic_Mold_Raw, tItem});
		for (IPrefixItem tItem : OP.toolHeadRawAxe           .mRegisteredPrefixItems) CR.shapeless(IL.Ceramic_Axe_Mold_Raw            .get(1), CR.DEF_NCC, new Object[] {IL.Ceramic_Mold_Raw, tItem});
		for (IPrefixItem tItem : OP.toolHeadAxe              .mRegisteredPrefixItems) CR.shapeless(IL.Ceramic_Double_Axe_Mold_Raw     .get(1), CR.DEF_NCC, new Object[] {IL.Ceramic_Mold_Raw, tItem, tItem});
		for (IPrefixItem tItem : OP.toolHeadRawAxe           .mRegisteredPrefixItems) CR.shapeless(IL.Ceramic_Double_Axe_Mold_Raw     .get(1), CR.DEF_NCC, new Object[] {IL.Ceramic_Mold_Raw, tItem, tItem});
		for (IPrefixItem tItem : OP.toolHeadAxeDouble        .mRegisteredPrefixItems) CR.shapeless(IL.Ceramic_Double_Axe_Mold_Raw     .get(1), CR.DEF_NCC, new Object[] {IL.Ceramic_Mold_Raw, tItem});
		for (IPrefixItem tItem : OP.toolHeadRawAxeDouble     .mRegisteredPrefixItems) CR.shapeless(IL.Ceramic_Double_Axe_Mold_Raw     .get(1), CR.DEF_NCC, new Object[] {IL.Ceramic_Mold_Raw, tItem});
		for (IPrefixItem tItem : OP.toolHeadSaw              .mRegisteredPrefixItems) CR.shapeless(IL.Ceramic_Saw_Mold_Raw            .get(1), CR.DEF_NCC, new Object[] {IL.Ceramic_Mold_Raw, tItem});
		for (IPrefixItem tItem : OP.toolHeadRawSaw           .mRegisteredPrefixItems) CR.shapeless(IL.Ceramic_Saw_Mold_Raw            .get(1), CR.DEF_NCC, new Object[] {IL.Ceramic_Mold_Raw, tItem});
		for (IPrefixItem tItem : OP.toolHeadHammer           .mRegisteredPrefixItems) CR.shapeless(IL.Ceramic_Hammer_Mold_Raw         .get(1), CR.DEF_NCC, new Object[] {IL.Ceramic_Mold_Raw, tItem});
		for (IPrefixItem tItem : OP.toolHeadFile             .mRegisteredPrefixItems) CR.shapeless(IL.Ceramic_File_Mold_Raw           .get(1), CR.DEF_NCC, new Object[] {IL.Ceramic_Mold_Raw, tItem});
		for (IPrefixItem tItem : OP.toolHeadScrewdriver      .mRegisteredPrefixItems) CR.shapeless(IL.Ceramic_Screwdriver_Mold_Raw    .get(1), CR.DEF_NCC, new Object[] {IL.Ceramic_Mold_Raw, tItem});
		for (IPrefixItem tItem : OP.toolHeadChisel           .mRegisteredPrefixItems) CR.shapeless(IL.Ceramic_Chisel_Mold_Raw         .get(1), CR.DEF_NCC, new Object[] {IL.Ceramic_Mold_Raw, tItem});
		for (IPrefixItem tItem : OP.toolHeadRawChisel        .mRegisteredPrefixItems) CR.shapeless(IL.Ceramic_Chisel_Mold_Raw         .get(1), CR.DEF_NCC, new Object[] {IL.Ceramic_Mold_Raw, tItem});
		for (IPrefixItem tItem : OP.toolHeadArrow            .mRegisteredPrefixItems) CR.shapeless(IL.Ceramic_Arrow_Mold_Raw          .get(1), CR.DEF_NCC, new Object[] {IL.Ceramic_Mold_Raw, tItem});
		for (IPrefixItem tItem : OP.toolHeadRawArrow         .mRegisteredPrefixItems) CR.shapeless(IL.Ceramic_Arrow_Mold_Raw          .get(1), CR.DEF_NCC, new Object[] {IL.Ceramic_Mold_Raw, tItem});
		for (IPrefixItem tItem : OP.arrowGtWood              .mRegisteredPrefixItems) CR.shapeless(IL.Ceramic_Arrow_Mold_Raw          .get(1), CR.DEF_NCC, new Object[] {IL.Ceramic_Mold_Raw, tItem});
		for (IPrefixItem tItem : OP.arrowGtPlastic           .mRegisteredPrefixItems) CR.shapeless(IL.Ceramic_Arrow_Mold_Raw          .get(1), CR.DEF_NCC, new Object[] {IL.Ceramic_Mold_Raw, tItem});
		for (IPrefixItem tItem : OP.toolHeadHoe              .mRegisteredPrefixItems) CR.shapeless(IL.Ceramic_Hoe_Mold_Raw            .get(1), CR.DEF_NCC, new Object[] {IL.Ceramic_Mold_Raw, tItem});
		for (IPrefixItem tItem : OP.toolHeadRawHoe           .mRegisteredPrefixItems) CR.shapeless(IL.Ceramic_Hoe_Mold_Raw            .get(1), CR.DEF_NCC, new Object[] {IL.Ceramic_Mold_Raw, tItem});
		for (IPrefixItem tItem : OP.toolHeadSense            .mRegisteredPrefixItems) CR.shapeless(IL.Ceramic_Sense_Mold_Raw          .get(1), CR.DEF_NCC, new Object[] {IL.Ceramic_Mold_Raw, tItem});
		for (IPrefixItem tItem : OP.toolHeadRawSense         .mRegisteredPrefixItems) CR.shapeless(IL.Ceramic_Sense_Mold_Raw          .get(1), CR.DEF_NCC, new Object[] {IL.Ceramic_Mold_Raw, tItem});
		for (IPrefixItem tItem : OP.toolHeadPlow             .mRegisteredPrefixItems) CR.shapeless(IL.Ceramic_Plow_Mold_Raw           .get(1), CR.DEF_NCC, new Object[] {IL.Ceramic_Mold_Raw, tItem});
		for (IPrefixItem tItem : OP.toolHeadRawPlow          .mRegisteredPrefixItems) CR.shapeless(IL.Ceramic_Plow_Mold_Raw           .get(1), CR.DEF_NCC, new Object[] {IL.Ceramic_Mold_Raw, tItem});
		for (IPrefixItem tItem : OP.toolHeadBuilderwand      .mRegisteredPrefixItems) CR.shapeless(IL.Ceramic_Builderwand_Mold_Raw    .get(1), CR.DEF_NCC, new Object[] {IL.Ceramic_Mold_Raw, tItem});
		
		
		for (OreDictMaterial tMat : new OreDictMaterial[] {ANY.WoodPlastic, ANY.Stone, MT.Glass, ANY.Wax, ANY.Iron, ANY.Cu, MT.Sn, MT.Zn, MT.Pb, MT.Bi, MT.Brass, MT.Bronze, MT.ArsenicCopper, MT.ArsenicBronze, MT.BismuthBronze, MT.Au}) {
		CR.shapeless(IL.Ceramic_Ingot_Mold_Raw          .get(1), CR.DEF_NCC, new Object[] {IL.Ceramic_Mold_Raw, OP.ingot.dat(tMat)});
		CR.shapeless(IL.Ceramic_Billet_Mold_Raw         .get(1), CR.DEF_NCC, new Object[] {IL.Ceramic_Mold_Raw, OP.billet.dat(tMat)});
		CR.shapeless(IL.Ceramic_Chunk_Mold_Raw          .get(1), CR.DEF_NCC, new Object[] {IL.Ceramic_Mold_Raw, OP.chunkGt.dat(tMat)});
		CR.shapeless(IL.Ceramic_Nugget_Mold_Raw         .get(1), CR.DEF_NCC, new Object[] {IL.Ceramic_Mold_Raw, OP.nugget.dat(tMat)});
		CR.shapeless(IL.Ceramic_Plate_Mold_Raw          .get(1), CR.DEF_NCC, new Object[] {IL.Ceramic_Mold_Raw, OP.plate.dat(tMat)});
		CR.shapeless(IL.Ceramic_Plate_Mold_Raw          .get(1), CR.DEF_NCC, new Object[] {IL.Ceramic_Mold_Raw, OP.plateGem.dat(tMat)});
		CR.shapeless(IL.Ceramic_Tiny_Plate_Mold_Raw     .get(1), CR.DEF_NCC, new Object[] {IL.Ceramic_Mold_Raw, OP.plateTiny.dat(tMat)});
		CR.shapeless(IL.Ceramic_Tiny_Plate_Mold_Raw     .get(1), CR.DEF_NCC, new Object[] {IL.Ceramic_Mold_Raw, OP.plateGemTiny.dat(tMat)});
		CR.shapeless(IL.Ceramic_Bolt_Mold_Raw           .get(1), CR.DEF_NCC, new Object[] {IL.Ceramic_Mold_Raw, OP.bolt.dat(tMat)});
		CR.shapeless(IL.Ceramic_Rod_Mold_Raw            .get(1), CR.DEF_NCC, new Object[] {IL.Ceramic_Mold_Raw, OP.stick.dat(tMat)});
		CR.shapeless(IL.Ceramic_Long_Rod_Mold_Raw       .get(1), CR.DEF_NCC, new Object[] {IL.Ceramic_Mold_Raw, OP.stick.dat(tMat), OP.stick.dat(tMat)});
		CR.shapeless(IL.Ceramic_Long_Rod_Mold_Raw       .get(1), CR.DEF_NCC, new Object[] {IL.Ceramic_Mold_Raw, OP.stickLong.dat(tMat)});
		CR.shapeless(IL.Ceramic_Item_Casing_Mold_Raw    .get(1), CR.DEF_NCC, new Object[] {IL.Ceramic_Mold_Raw, OP.casingSmall.dat(tMat)});
		CR.shapeless(IL.Ceramic_Ring_Mold_Raw           .get(1), CR.DEF_NCC, new Object[] {IL.Ceramic_Mold_Raw, OP.ring.dat(tMat)});
		CR.shapeless(IL.Ceramic_Gear_Mold_Raw           .get(1), CR.DEF_NCC, new Object[] {IL.Ceramic_Mold_Raw, OP.gearGt.dat(tMat)});
		CR.shapeless(IL.Ceramic_Small_Gear_Mold_Raw     .get(1), CR.DEF_NCC, new Object[] {IL.Ceramic_Mold_Raw, OP.gearGtSmall.dat(tMat)});
		CR.shapeless(IL.Ceramic_File_Mold_Raw           .get(1), CR.DEF_NCC, new Object[] {IL.Ceramic_Mold_Raw, OP.plate.dat(tMat), OP.plate.dat(tMat)});
		CR.shapeless(IL.Ceramic_Chisel_Mold_Raw         .get(1), CR.DEF_NCC, new Object[] {IL.Ceramic_Mold_Raw, OD.paneGlass, OP.stick.dat(tMat)});
		CR.shapeless(IL.Ceramic_Chisel_Mold_Raw         .get(1), CR.DEF_NCC, new Object[] {IL.Ceramic_Mold_Raw, OD.plankAnyWood, OP.stick.dat(tMat)});
		CR.shapeless(IL.Ceramic_Chisel_Mold_Raw         .get(1), CR.DEF_NCC, new Object[] {IL.Ceramic_Mold_Raw, OP.plate.dat(tMat), OP.stick.dat(tMat)});
		}
		});
		
		aItems.add(998, aID -> {IL.Food_Can_Empty.set(addItem(aID, "Empty Food Can" , "Used for canning Food" , new OreDictItemData(MT.TinAlloy, OP.plateCurved.mAmount), TC.stack(TC.VACUOS, 1), TC.stack(TC.FABRICO, 1)));});
		aItems.add(999, aID -> {
			IL.Spray_Empty   .set(addItem(aID, "Empty Spray Can", "Used for making Sprays", new OreDictItemData(MT.Sn, OP.plateCurved.mAmount, MT.Redstone, OP.dust.mAmount), TC.stack(TC.VACUOS, 1), TC.stack(TC.MOTUS, 1)));
		
		RM.RollBender.addRecipe1(T, 16, 64, OP.plateCurved.mat(MT.TinAlloy, 1), IL.Food_Can_Empty.get(1));
// TODO RA.addAssemblerRecipe(OP.dust.mat(MT.Redstone, 1), OP.cell.mat(MT.Empty, 1), IL.Spray_Empty.get(1), 800, 1);
		CR.shaped(IL.Food_Can_Empty      .get(1), CR.DEF_NCC, "fh" , "oP"        , 'P', OP.plateCurved.dat(MT.TinAlloy));
		CR.shaped(IL.Spray_Empty         .get(1), CR.DEF_NCC, "Rf" , "Cs"        , 'R', OD.itemRedstone, 'C', OP.plateCurved.dat(MT.Sn));
		});
		
		for (byte i = 0; i < 16; i++) {
			final byte tIndex = i;
			aItems.addAll(new int[] {1000+2*tIndex, (1000+2*tIndex)+1, 1100+2*tIndex, (1100+2*tIndex)+1, 1132+2*tIndex, (1132+2*tIndex)+1}, aIDs -> {
			IL.SPRAY_CAN_DYES[tIndex]               .set(addItem( aIDs[0], "Spray Paint ("+DYE_NAMES[tIndex]+")", "Full", TC.stack(TC.SENSUS, 4)));
		IL.SPRAY_CAN_DYES_USED[tIndex]          .set(addItem(aIDs[1], "Spray Paint ("+DYE_NAMES[tIndex]+")", "Used", TC.stack(TC.SENSUS, 3), TD.Creative.HIDDEN));
		addItemBehavior(new Behavior_Spray_Color(IL.Spray_Empty.get(1), IL.SPRAY_CAN_DYES_USED[tIndex].get(1), IL.SPRAY_CAN_DYES[tIndex].get(1), 512, tIndex), mLastID, mLastID-1);
		RM.Canner.addRecipe1(T, 16, 256, IL.Spray_Empty.get(1), FL.mul(DYE_FLUIDS_CHEMICAL[tIndex], 16), NF, IL.SPRAY_CAN_DYES[tIndex].get(1));
		ItemsGT.addNEIRedirects(IL.SPRAY_CAN_DYES_USED[tIndex].get(1), IL.SPRAY_CAN_DYES[tIndex].get(1));
		RM.DidYouKnow.addFakeRecipe(F, ST.array(IL.SPRAY_CAN_DYES[tIndex].get(1), IL.SPRAY_CAN_DYES_USED[tIndex].get(1), ST.make(Blocks.WHITE_WOOL, 1, 0), ST.make(Blocks.GLASS_PANE, 1, 0), ST.make(Blocks.GLASS, 1, 0), ST.make(Blocks.TERRACOTTA, 1, 0)), ST.array(NI, NI, ST.make(Blocks.WHITE_WOOL, 1, 15-tIndex), ST.make(Blocks.WHITE_STAINED_GLASS_PANE, 1, 15-tIndex), ST.make(Blocks.WHITE_STAINED_GLASS, 1, 15-tIndex), ST.make(Blocks.WHITE_TERRACOTTA, 1, 15-tIndex)), null, ZL_LONG, ZL_FS, ZL_FS, 0, 0, 0);


		IL.SPRAY_CAN_FOAM[tIndex]               .set(addItem( aIDs[2], "C-Foam Spray ("+DYE_NAMES[tIndex]+")", "Full", TC.stack(TC.TERRA, 2), TC.stack(TC.FABRICO, 2)));
		IL.SPRAY_CAN_FOAM_USED[tIndex]          .set(addItem(aIDs[3], "C-Foam Spray ("+DYE_NAMES[tIndex]+")", "Used", TC.stack(TC.TERRA, 1), TC.stack(TC.FABRICO, 1), TD.Creative.HIDDEN));
		addItemBehavior(new Behavior_Spray_Foam(IL.Spray_Empty.get(1), IL.SPRAY_CAN_FOAM_USED[tIndex].get(1), IL.SPRAY_CAN_FOAM[tIndex].get(1), 256, tIndex, F), mLastID, mLastID-1);
		RM.Canner.addRecipe1(T, 16, 256, IL.Spray_Empty.get(1), FL.mul(DYED_C_FOAMS[tIndex], 256), NF, IL.SPRAY_CAN_FOAM[tIndex].get(1));
		ItemsGT.addNEIRedirects(IL.SPRAY_CAN_FOAM_USED[tIndex].get(1), IL.SPRAY_CAN_FOAM[tIndex].get(1), ST.make(BlocksGT.CFoamFresh, 1, tIndex), ST.make(BlocksGT.CFoam, 1, tIndex));
		RM.DidYouKnow.addFakeRecipe(F, ST.array(IL.SPRAY_CAN_FOAM[tIndex].get(1), IL.SPRAY_CAN_FOAM_USED[tIndex].get(1)), ST.array(ST.make(BlocksGT.CFoamFresh, 1, tIndex), ST.make(BlocksGT.CFoam, 1, tIndex)), null, ZL_LONG, ZL_FS, ZL_FS, 0, 0, 0);


		IL.SPRAY_CAN_FOAM_OWNED[tIndex]         .set(addItem( aIDs[4], "Advanced C-Foam Spray ("+DYE_NAMES[tIndex]+")", "Full (C-Foam only breakable by Owner once dry)", TC.stack(TC.TERRA, 2), TC.stack(TC.FABRICO, 2), TC.stack(TC.SPIRITUS, 2)));
		IL.SPRAY_CAN_FOAM_OWNED_USED[tIndex]    .set(addItem(aIDs[5], "Advanced C-Foam Spray ("+DYE_NAMES[tIndex]+")", "Used (C-Foam only breakable by Owner once dry)", TC.stack(TC.TERRA, 1), TC.stack(TC.FABRICO, 1), TC.stack(TC.SPIRITUS, 1), TD.Creative.HIDDEN));
		addItemBehavior(new Behavior_Spray_Foam(IL.Spray_Empty.get(1), IL.SPRAY_CAN_FOAM_OWNED_USED[tIndex].get(1), IL.SPRAY_CAN_FOAM_OWNED[tIndex].get(1), 256, tIndex, T), mLastID, mLastID-1);
		RM.Canner.addRecipe1(T, 16, 256, IL.Spray_Empty.get(1), FL.mul(DYED_C_FOAMS_OWNED[tIndex], 256), NF, IL.SPRAY_CAN_FOAM_OWNED[tIndex].get(1));
		ItemsGT.addNEIRedirects(IL.SPRAY_CAN_FOAM_OWNED_USED[tIndex].get(1), IL.SPRAY_CAN_FOAM_OWNED[tIndex].get(1));
		RM.DidYouKnow.addFakeRecipe(F, ST.array(IL.SPRAY_CAN_FOAM_OWNED[tIndex].get(1), IL.SPRAY_CAN_FOAM_OWNED_USED[tIndex].get(1)), ST.array(ST.make(BlocksGT.CFoamFresh, 1, tIndex, "Player-Owned C-Foam"), ST.make(BlocksGT.CFoam, 1, tIndex, "Player-Owned C-Foam")), null, ZL_LONG, ZL_FS, ZL_FS, 0, 0, 0);
		});
		}
		
		
		
		aItems.add(1096, aID -> {IL.Spray_Color_Remover             .set(addItem(aID, "Paint Removal Spray", "Full", TC.stack(TC.SENSUS, 2), TC.stack(TC.PERDITIO, 2)));});
		aItems.add(1097, aID -> {
			IL.Spray_Color_Remover_Used        .set(addItem(aID, "Paint Removal Spray", "Used", TC.stack(TC.SENSUS, 1), TC.stack(TC.PERDITIO, 1), TD.Creative.HIDDEN));
		addItemBehavior(new Behavior_Spray_Color_Remover(IL.Spray_Empty.get(1), IL.Spray_Color_Remover_Used.get(1), IL.Spray_Color_Remover.get(1), 256), mLastID, mLastID-1);
		RM.Canner.addRecipe1(T, 16, 256, IL.Spray_Empty.get(1), MT.Cl.fluid(16*U, T), NF, IL.Spray_Color_Remover.get(1));
		ItemsGT.addNEIRedirects(IL.Spray_Color_Remover_Used.get(1), IL.Spray_Color_Remover.get(1));
		});
		
		
		
		aItems.add(1196, aID -> {IL.Spray_Foam_Remover              .set(addItem(aID, "C-Foam Removal Spray", "Full", TC.stack(TC.TERRA, 2), TC.stack(TC.PERDITIO, 2)));});
		aItems.add(1197, aID -> {
			IL.Spray_Foam_Remover_Used         .set(addItem(aID, "C-Foam Removal Spray", "Used", TC.stack(TC.TERRA, 1), TC.stack(TC.PERDITIO, 1), TD.Creative.HIDDEN));
		addItemBehavior(new Behavior_Spray_Foam_Remover(IL.Spray_Empty.get(1), IL.Spray_Foam_Remover_Used.get(1), IL.Spray_Foam_Remover.get(1), 256), mLastID, mLastID-1);
		RM.Canner.addRecipe1(T, 16, 256, IL.Spray_Empty.get(1), MT.H2SO4.fluid(16*U, T), NF, IL.Spray_Foam_Remover.get(1));
		RM.Canner.addRecipe1(T, 16, 256, IL.Spray_Empty.get(1), MT.HNO3 .fluid(16*U, T), NF, IL.Spray_Foam_Remover.get(1));
		ItemsGT.addNEIRedirects(IL.Spray_Foam_Remover_Used.get(1), IL.Spray_Foam_Remover.get(1));
		});
		
		
		
		aItems.add(1198, aID -> {IL.Spray_Foam_Hardener             .set(addItem(aID, "Hardening Spray", "Full", TC.stack(TC.TERRA, 2), TC.stack(TC.TUTAMEN, 2)));});
		aItems.add(1199, aID -> {
			IL.Spray_Foam_Hardener_Used        .set(addItem(aID, "Hardening Spray", "Used", TC.stack(TC.TERRA, 1), TC.stack(TC.TUTAMEN, 1), TD.Creative.HIDDEN));
		addItemBehavior(new Behavior_Spray_Foam_Hardener(IL.Spray_Empty.get(1), IL.Spray_Foam_Hardener_Used.get(1), IL.Spray_Foam_Hardener.get(1), 256), mLastID, mLastID-1);
		RM.Canner.addRecipe2(T, 16, 256, ST.make(Blocks.SAND, 7, 0), IL.Spray_Empty.get(1), IL.Spray_Foam_Hardener.get(1));
		for (OreDictMaterial tMat : ANY.SiO2.mToThis) for (ItemStack tDust : ST.array(OP.dust.mat(tMat, 63), OP.blockDust.mat(tMat, 7))) if (ST.valid(tDust))
		RM.Canner.addRecipe2(T, 16, 256, tDust, IL.Spray_Empty.get(1), IL.Spray_Foam_Hardener.get(1));
		ItemsGT.addNEIRedirects(IL.Spray_Foam_Hardener_Used.get(1), IL.Spray_Foam_Hardener.get(1));
		});
		
		
		
		aItems.add(1998, aID -> {IL.Spray_Extinguisher              .set(addItem(aID, "Fire Extinguisher (CO2)", "Full", TC.stack(TC.PERDITIO, 2), TC.stack(TC.IGNIS, 2)));});
		aItems.add(1999, aID -> {
			IL.Spray_Extinguisher_Used         .set(addItem(aID, "Fire Extinguisher (CO2)", "Used", TC.stack(TC.PERDITIO, 1), TC.stack(TC.IGNIS, 1), TD.Creative.HIDDEN));
		addItemBehavior(new Behavior_Spray_Extinguisher(IL.Spray_Empty.get(1), IL.Spray_Extinguisher_Used.get(1), IL.Spray_Extinguisher.get(1), 256), mLastID, mLastID-1);
		RM.Canner.addRecipe1(T, 16, 256, IL.Spray_Empty.get(1), MT.CO2.fluid(16*U, T), NF, IL.Spray_Extinguisher.get(1));
		ItemsGT.addNEIRedirects(IL.Spray_Extinguisher_Used.get(1), IL.Spray_Extinguisher.get(1));
		});
		
		
		
		OreDictMaterial[] tBucketMaterials = new OreDictMaterial[] {ANY.Cu, MT.Sn, MT.Zn, MT.Pb, MT.Bi, MT.Brass, MT.Bronze, MT.BismuthBronze, MT.Au};
		for (int i = 0; i < 9; i++) {
			final int tIndex = i;
			aItems.addAll(new int[] {2000 + tIndex*100, (2000 + tIndex*100)+1, (2000 + tIndex*100)+2, (2000 + tIndex*100)+3, (2000 + tIndex*100)+4, (2000 + tIndex*100)+5, (2000 + tIndex*100)+6, (2000 + tIndex*100)+7, (2000 + tIndex*100)+8, (2000 + tIndex*100)+9, (2000 + tIndex*100)+10, (2000 + tIndex*100)+11, (2000 + tIndex*100)+12, (2000 + tIndex*100)+13, (2000 + tIndex*100)+14, (2000 + tIndex*100)+15, (2000 + tIndex*100)+16, (2000 + tIndex*100)+17, (2000 + tIndex*100)+18}, aIDs -> {
			OreDictItemData tData = new OreDictItemData(ANY.Wood, U*3, OM.stack(tBucketMaterials[tIndex], U*1));
			ItemStack tBucket = addItem(aIDs[0], "Wooden Bucket", "Empty", TC.stack(TC.ARBOR, 2), TC.stack(TC.VACUOS, 2), Behavior_Bucket_Simple.INSTANCE, tData);
			CR.shaped(tBucket, CR.DEF_NCC, "WPW", " Wh", 'P', OP.plate.dat(tBucketMaterials[tIndex]), 'W', OD.plankAnyWood);
			ItemsGT.addNEIRedirects(tBucket
			, addItem(aIDs[1], "Wooden Bucket", "Water"                                  , TC.stack(TC.ARBOR, 2), TC.stack(TC.AQUA     , 2), new Behavior_Bucket_Simple(ST.make(Items.WATER_BUCKET, 1, 0))                       , tData.copy(), FL.Water.make(1000), FL.DistW.make(1000), FL.River_Water.make(1000), FL.MnWtr.make(1000), FL.Mineralsoda.make(1000), FL.Soda.make(1000), FL.Water_Hot.make(1000), FL.Water_Boiling.make(1000), FL.Water_Geothermal.make(1000), OD.container1000water)
			, addItem(aIDs[2], "Wooden Bucket", "Milk (you cannot drink out of Buckets!)", TC.stack(TC.ARBOR, 2), TC.stack(TC.SANO     , 2), new Behavior_Bucket_Simple(ST.make(Items.MILK_BUCKET, 1, 0))                        , tData.copy(), FL.Milk.make(1000), FL.MilkGrC.make(1000)                                                                                                                                                                                    , OD.container1000milk)
			, addItem(aIDs[3], "Wooden Bucket", "Latex"                                  , TC.stack(TC.ARBOR, 2), TC.stack(TC.LIMUS    , 2), Behavior_Bucket_Simple.INSTANCE                                                     , tData.copy(), FL.Latex.make(1000), FL.make("molten.latex", 1000)                                                                                                                                                                           , OD.container1000latex)
			, addItem(aIDs[4], "Wooden Bucket", "Creosote Oil"                           , TC.stack(TC.ARBOR, 2), TC.stack(TC.IGNIS    , 2), new Behavior_Bucket_Simple(IL.RC_Creosote_Bucket.get(1))                            , tData.copy(), FL.Oil_Creosote.make(1000)                                                                                                                                                                                                   , OD.container1000creosote)
			, addItem(aIDs[5], "Wooden Bucket", "Sea Water (you cannot place this!)"     , TC.stack(TC.ARBOR, 2), TC.stack(TC.TEMPESTAS, 2), new Behavior_Switch_Metadata(2000 + tIndex*100, F, F)                                    , tData.copy(), FL.Ocean.make(1000), FL.OceanGrC.make(1000), FL.Tropics_Water.make(1000)                                                                                                                                                     )
			, addItem(aIDs[6], "Wooden Bucket", "Rubber Tree Sap"                        , TC.stack(TC.ARBOR, 2), TC.stack(TC.LIMUS    , 2), Behavior_Bucket_Simple.INSTANCE                                                     , tData.copy(), FL.Resin_Rubber.make(1000)                                                                                                                                                                                                   , OD.container1000rubbertreesap)
			, addItem(aIDs[7], "Wooden Bucket", "Spruce Tree Resin"                      , TC.stack(TC.ARBOR, 3), TC.stack(TC.LIMUS    , 1), Behavior_Bucket_Simple.INSTANCE                                                     , tData.copy(), FL.Resin_Spruce.make(1000)                                                                                                                                                                                                   , OD.container1000spruceresin)
			, addItem(aIDs[8], "Wooden Bucket", "Honey"                                  , TC.stack(TC.ARBOR, 3), TC.stack(TC.BESTIA   , 1), new Behavior_Bucket_Simple(ST.make(MD.ERE, "bucketHoney", 1, 0, IL.FR_Honey_Bucket)), tData.copy(), FL.Honey.make(1000), FL.HoneyGrC.make(1000), FL.HoneyBoP.make(1000)                                                                                                                                                          , OD.container1000honey)
			, addItem(aIDs[9], "Wooden Bucket", "Dirty Water (you cannot place this!)"   , TC.stack(TC.ARBOR, 2), TC.stack(TC.AQUA     , 1), TC.stack(TC.VENEMUM, 1), new Behavior_Switch_Metadata(2000 + tIndex*100, F, F)           , tData.copy(), FL.Dirty_Water.make(1000)                                                                                                                                                                                                    )
			, addItem(aIDs[10], "Wooden Bucket", "Lubricant"                              , TC.stack(TC.ARBOR, 2), TC.stack(TC.LIMUS    , 2), Behavior_Bucket_Simple.INSTANCE                                                     , tData.copy(), FL.LubRoCant.make(1000), FL.Lubricant.make(1000)                                                                                                                                                                             , OD.container1000lubricant)
			, addItem(aIDs[11], "Wooden Bucket", "Milk (you cannot drink out of Buckets!)", TC.stack(TC.ARBOR, 2), TC.stack(TC.VENEMUM  , 2), Behavior_Bucket_Simple.INSTANCE                                                     , tData.copy(), FL.Milk_Spoiled.make(1000)                                                                                                                                                                                                   )
			, addItem(aIDs[12], "Wooden Bucket", "Maple Sap"                              , TC.stack(TC.ARBOR, 2), TC.stack(TC.FAMES    , 2), Behavior_Bucket_Simple.INSTANCE                                                     , tData.copy(), FL.Sap_Maple.make(1000)                                                                                                                                                                                                      , OD.container1000maplesap)
			, addItem(aIDs[13], "Wooden Bucket", "Rainbow Sap"                            , TC.stack(TC.ARBOR, 2), TC.stack(TC.AURAM    , 2), Behavior_Bucket_Simple.INSTANCE                                                     , tData.copy(), FL.Sap_Rainbow.make(1000)                                                                                                                                                                                                    , OD.container1000rainbowsap)
			, addItem(aIDs[14], "Wooden Bucket", "Soy Milk"                               , TC.stack(TC.ARBOR, 2), TC.stack(TC.HERBA    , 2), Behavior_Bucket_Simple.INSTANCE                                                     , tData.copy(), FL.MilkSoy.make(1000)                                                                                                                                                                                                        , OD.container1000soymilk)
			, addItem(aIDs[15], "Wooden Bucket", "Glue"                                   , TC.stack(TC.ARBOR, 2), TC.stack(TC.LIMUS    , 2), Behavior_Bucket_Simple.INSTANCE                                                     , tData.copy(), FL.Glue.make(1000)                                                                                                                                                                                                           , OD.container1000glue)
			, addItem(aIDs[16], "Wooden Bucket", "Spectral Dew"                           , TC.stack(TC.ARBOR, 2), TC.stack(TC.SPIRITUS , 2), new Behavior_Bucket_Simple(IL.NeLi_Bucket_Spectral_Dew.get(1))                      , tData.copy(), FL.SpDew.make(1000)                                                                                                                                                                                                          , OD.container1000water)
			, addItem(aIDs[17], "Wooden Bucket", "Seed Oil"                               , TC.stack(TC.ARBOR, 3), TC.stack(TC.GRANUM   , 1), new Behavior_Bucket_Simple(IL.FR_SeedOil_Bucket.get(1))                             , tData.copy(), FL.Oil_Seed.make(1000), FL.Oil_Lin.make(1000), FL.Oil_Hemp.make(1000), FL.Oil_Olive.make(1000), FL.Oil_Sunflower.make(1000), FL.Oil_Nut.make(1000)                                                                           , OD.container1000seedoil)
			, addItem(aIDs[18], "Wooden Bucket", "Poison"                                 , TC.stack(TC.ARBOR, 3), TC.stack(TC.VENEMUM  , 2), Behavior_Bucket_Simple.INSTANCE                                                     , tData.copy(), FL.Poison.make(1000), FL.Potion_Poison_2.make(1000)                                                                                                                                                                          , OD.container1000poison)
			);
		});
		}
		
		aItems.then(() -> {
			IL.Wooden_Bucket_Copper       .set(this, 2000);
		IL.Wooden_Bucket_Tin          .set(this, 2100);
		IL.Wooden_Bucket_Zinc         .set(this, 2200);
		IL.Wooden_Bucket_Lead         .set(this, 2300);
		IL.Wooden_Bucket_Bismuth      .set(this, 2400);
		IL.Wooden_Bucket_Brass        .set(this, 2500);
		IL.Wooden_Bucket_Bronze       .set(this, 2600);
		IL.Wooden_Bucket_BismuthBronze.set(this, 2700);
		IL.Wooden_Bucket_Gold         .set(this, 2800);
		});
		
		
		
		aItems.add(5000, aID -> {
			IL.Tool_Matches                    .set(addItem(aID, "Match"                            , ""                                            , new Behavior_Lighter(9000), TC.stack(TC.IGNIS, 1), TC.stack(TC.VACUOS, 1), OD.craftingFirestarter));
		for (OreDictMaterial tWood : ANY.Wood.mToThis) {
		RM.Assembler.addRecipe2(T, 16, 16, OP.bolt.mat(tWood, 1), OP.dustSmall.mat(MT.P       , 1), IL.Tool_Matches.get(1));
		RM.Assembler.addRecipe2(T, 16, 64, OP.bolt.mat(tWood, 4), OP.dust     .mat(MT.P              , 1), IL.Tool_Matches.get(4));
		for (OreDictMaterial tPhosphorus : ANY.Phosphorus.mToThis) {
		RM.Assembler.addRecipe2(T, 16, 16, OP.bolt.mat(tWood, 1), OP.dustSmall.mat(tPhosphorus, 1), IL.Tool_Matches.get(1));
		RM.Assembler.addRecipe2(T, 16, 64, OP.bolt.mat(tWood, 4), OP.dust     .mat(tPhosphorus, 1), IL.Tool_Matches.get(4));
		}
		}
		CR.shaped(IL.Tool_Matches.get(1), CR.DEF, "P", "S", 'P', OP.dustSmall.dat(MT.P                ), 'S', OP.bolt.dat(ANY.Wood));
		CR.shaped(IL.Tool_Matches.get(1), CR.DEF, "P", "S", 'P', OP.dustSmall.dat(ANY.Phosphorus      ), 'S', OP.bolt.dat(ANY.Wood));
		CR.shaped(IL.Tool_Matches.get(4), CR.DEF, " S ", "SPS", " S ", 'P', OP.dust.dat(MT.P          ), 'S', OP.bolt.dat(ANY.Wood));
		CR.shaped(IL.Tool_Matches.get(4), CR.DEF, " S ", "SPS", " S ", 'P', OP.dust.dat(ANY.Phosphorus), 'S', OP.bolt.dat(ANY.Wood));
		});
		aItems.add(5002, aID -> {IL.Tool_MatchBox_Used              .set(addItem(aID, "Match Box"                        , "This is not a Car"                           , TC.stack(TC.IGNIS, 2), TC.stack(TC.POTENTIA, 1), OD.craftingFirestarter, TD.Creative.HIDDEN));});
		aItems.add(5003, aID -> {
			IL.Tool_MatchBox_Full              .set(addItem(aID, "Match Box (Full)"                 , "This is not a Car"                           , TC.stack(TC.IGNIS, 1), TC.stack(TC.POTENTIA, 2), OD.craftingFirestarter));
		addItemBehavior(new Behavior_Lighter(null, IL.Tool_MatchBox_Used.get(1), IL.Tool_MatchBox_Full.get(1), 64, 9000), mLastID, mLastID-1);;
		ItemsGT.addNEIRedirects(IL.Tool_Matches.get(1), IL.Tool_MatchBox_Used.get(1), IL.Tool_MatchBox_Full.get(1));
		RM.Boxinator.addRecipe2(T, 16, 64, IL.Tool_Matches.get(64), OP.plateDouble.mat(MT.Paper, 1), IL.Tool_MatchBox_Full.get(1));
		RM.Unboxinator.addRecipe1(T, 16, 32, IL.Tool_MatchBox_Full.get(1), IL.Tool_Matches.get(64), OP.scrapGt.mat(MT.Paper, 16));
		});
		
		
		
		aItems.add(5004, aID -> {IL.Tool_Lighter_Invar_Empty        .set(addItem(aID, "Lighter (Empty)"                  , "Requires Canning Machine to be filled"       , new OreDictItemData(MT.Invar, OP.plateCurved.mAmount * 2), TC.stack(TC.IGNIS, 1), TC.stack(TC.VACUOS, 1)));});
		aItems.add(5005, aID -> {IL.Tool_Lighter_Invar_Used         .set(addItem(aID, "Lighter"                          , ""                                            , new OreDictItemData(MT.Invar, OP.plateCurved.mAmount * 2), TC.stack(TC.IGNIS, 2), TC.stack(TC.POTENTIA, 1), OD.craftingFirestarter, TD.Creative.HIDDEN));});
		aItems.add(5006, aID -> {
			IL.Tool_Lighter_Invar_Full         .set(addItem(aID, "Lighter (Full)"                   , ""                                            , new OreDictItemData(MT.Invar, OP.plateCurved.mAmount * 2), TC.stack(TC.IGNIS, 1), TC.stack(TC.POTENTIA, 2), OD.craftingFirestarter));
		addItemBehavior(new Behavior_Lighter(IL.Tool_Lighter_Invar_Empty.get(1), IL.Tool_Lighter_Invar_Used.get(1), IL.Tool_Lighter_Invar_Full.get(1), 100, 10000), mLastID, mLastID-1);;
		RM.Canner.addRecipe1(T, 16, 16, IL.Tool_Lighter_Invar_Empty.get(1), FL.Butane.make(100), NF, IL.Tool_Lighter_Invar_Full.get(1));
		ItemsGT.addNEIRedirects(IL.Tool_Lighter_Invar_Empty.get(1), IL.Tool_Lighter_Invar_Used.get(1), IL.Tool_Lighter_Invar_Full.get(1));
		});
		
		
		
		aItems.add(5007, aID -> {IL.Tool_Lighter_Platinum_Empty     .set(addItem(aID, "Shiny Lighter (Empty)"            , "Requires Canning Machine to be filled"       , new OreDictItemData(MT.Pt, OP.plateCurved.mAmount * 2), TC.stack(TC.IGNIS, 1), TC.stack(TC.NEBRISUM, 1), TC.stack(TC.VACUOS, 1)));});
		aItems.add(5008, aID -> {IL.Tool_Lighter_Platinum_Used      .set(addItem(aID, "Shiny Lighter"                    , "A known Master of Pranks is engraved on it"  , new OreDictItemData(MT.Pt, OP.plateCurved.mAmount * 2), TC.stack(TC.IGNIS, 2), TC.stack(TC.NEBRISUM, 1), TC.stack(TC.POTENTIA, 1), OD.craftingFirestarter, TD.Creative.HIDDEN));});
		aItems.add(5009, aID -> {
			IL.Tool_Lighter_Platinum_Full      .set(addItem(aID, "Shiny Lighter (Full)"             , "A known Master of Pranks is engraved on it"  , new OreDictItemData(MT.Pt, OP.plateCurved.mAmount * 2), TC.stack(TC.IGNIS, 1), TC.stack(TC.NEBRISUM, 1), TC.stack(TC.POTENTIA, 2), OD.craftingFirestarter));
		addItemBehavior(new Behavior_Lighter(IL.Tool_Lighter_Platinum_Empty.get(1), IL.Tool_Lighter_Platinum_Used.get(1), IL.Tool_Lighter_Platinum_Full.get(1), 1000, 10000), mLastID, mLastID-1);;
		RM.Canner.addRecipe1(T, 16, 64, IL.Tool_Lighter_Platinum_Empty.get(1), FL.Butane.make(1000), NF, IL.Tool_Lighter_Platinum_Full.get(1));
		ItemsGT.addNEIRedirects(IL.Tool_Lighter_Platinum_Empty.get(1), IL.Tool_Lighter_Platinum_Used.get(1), IL.Tool_Lighter_Platinum_Full.get(1));
		});
		
		
		
		aItems.add(5010, aID -> {IL.Tool_Lighter_Plastic_Empty      .set(addItem(aID, "Plastic Lighter (Empty)"          , "Requires Canning Machine to be filled"       , new OreDictItemData(ANY.Plastic, OP.plateCurved.mAmount * 2, ANY.Fe, OP.screw.mAmount), TC.stack(TC.IGNIS, 1), TC.stack(TC.VACUOS, 1)));});
		aItems.add(5011, aID -> {IL.Tool_Lighter_Plastic_Used       .set(addItem(aID, "Plastic Lighter"                  , ""                                            , new OreDictItemData(ANY.Plastic, OP.plateCurved.mAmount * 2, ANY.Fe, OP.screw.mAmount), TC.stack(TC.IGNIS, 2), TC.stack(TC.POTENTIA, 1), OD.craftingFirestarter, TD.Creative.HIDDEN));});
		aItems.add(5012, aID -> {IL.Tool_Lighter_Plastic_Full       .set(addItem(aID, "Plastic Lighter (Full)"           , ""                                            , new OreDictItemData(ANY.Plastic, OP.plateCurved.mAmount * 2, ANY.Fe, OP.screw.mAmount), TC.stack(TC.IGNIS, 1), TC.stack(TC.POTENTIA, 2), OD.craftingFirestarter));});
		aItems.add(5013, aID -> {
			IL.Tool_Lighter_Plastic_Broken     .set(addItem(aID, "Plastic Lighter (Broken)"         , ""                                            , new OreDictItemData(ANY.Plastic, OP.plateCurved.mAmount * 2, ANY.Fe, OP.screw.mAmount), TC.stack(TC.IGNIS, 1), TC.stack(TC.PERDITIO, 1), TD.Creative.HIDDEN));
		addItemBehavior(new Behavior_Lighter(IL.Tool_Lighter_Plastic_Broken.get(1), IL.Tool_Lighter_Plastic_Used.get(1), IL.Tool_Lighter_Plastic_Full.get(1), 100, 9000), mLastID-2, mLastID-1);;
		CR.shaped(IL.Tool_Lighter_Plastic_Empty.get(1), CR.DEF_NCC, "IF", "dP", "xP", 'F', OD.itemFlint, 'P', OP.plateCurved.dat(ANY.Plastic), 'I', OP.screw.dat(ANY.Iron));
		RM.Canner.addRecipe1(T, 16, 16, IL.Tool_Lighter_Plastic_Empty.get(1), FL.Butane.make(100), NF, IL.Tool_Lighter_Plastic_Full.get(1));
		ItemsGT.addNEIRedirects(IL.Tool_Lighter_Plastic_Empty.get(1), IL.Tool_Lighter_Plastic_Used.get(1), IL.Tool_Lighter_Plastic_Full.get(1), IL.Tool_Lighter_Plastic_Broken.get(1));
		});
		
		
		
		aItems.add(5014, aID -> {
			IL.Tool_Fire_Starter               .set(addItem(aID, "Fire Starter"                     , "(Made with Dry Grass)"                       , new OreDictItemData(ANY.Wood, U), new Behavior_Lighter(5000), TC.stack(TC.IGNIS, 1), TC.stack(TC.ARBOR, 1), TC.stack(TC.HERBA, 1), OD.craftingFirestarter));
		CR.shaped(IL.Tool_Fire_Starter.get(1)       , CR.DEF_NCC_MIR, "S ", "GS", 'S', OP.stick.dat(ANY.Wood), 'G', OD.itemGrassDry);
		});
		
		aItems.add(5015, aID -> {
			IL.Tool_Fire_Starter_Bark          .set(addItem(aID, "Fire Starter"                     , "(Made with Dry Tree Bark)"                   , new OreDictItemData(ANY.Wood, U), new Behavior_Lighter(5500), TC.stack(TC.IGNIS, 1), TC.stack(TC.ARBOR, 2), OD.craftingFirestarter));
		CR.shaped(IL.Tool_Fire_Starter_Bark.get(1)  , CR.DEF_NCC_MIR, "S ", "GS", 'S', OP.stick.dat(ANY.Wood), 'G', OD.itemBarkDry);

		// ADAPT-001 (adaptation, NOT 1:1): another tinder source — vanilla 26.1.2 dry leaf litter,
		// absent in 1.7.10. No new item: the two existing variants differ only by ignition chance
		// (grass 5000 = 50%, bark 5500 = 55%), i.e. material quality; litter is the same light
		// plant-tinder class as grass, so it yields the same item — a third ID would just duplicate
		// the entity under a different label. The 2x2 shape follows a direct player request (2 sticks
		// + 2 leaves); the grass/bark recipes use a diagonal because they have only two ingredients.
		CR.shaped(IL.Tool_Fire_Starter.get(1)       , CR.DEF_NCC_MIR, "SL", "LS", 'S', OP.stick.dat(ANY.Wood), 'L', OD.itemLeafLitter);
		});
		
		
		aItems.add(5999, aID -> {
			IL.Pellet_Wood                     .set(addItem(aID, "Wood Pellet"                      , ""                                            , new OreDictItemData(ANY.Wood, U), TICKS_PER_SMELT, TC.stack(TC.POTENTIA, 1)));
		RM.Mixer.addRecipe1(T, 16, 16, OM.dust(MT.LiveRoot   , U2), FL.Glue.make( 25), NF, IL.Pellet_Wood.get(1));
		RM.Mixer.addRecipe1(T, 16, 32, OM.dust(MT.LiveRoot       ), FL.Glue.make( 50), NF, IL.Pellet_Wood.get(2));
		RM.Mixer.addRecipe1(T, 16, 16, OM.dust(MT.Livingwood , U2), FL.Glue.make( 25), NF, IL.Pellet_Wood.get(1));
		RM.Mixer.addRecipe1(T, 16, 32, OM.dust(MT.Livingwood     ), FL.Glue.make( 50), NF, IL.Pellet_Wood.get(2));
		RM.Mixer.addRecipe1(T, 16, 16, OM.dust(MT.Greatwood  , U2), FL.Glue.make( 25), NF, IL.Pellet_Wood.get(1));
		RM.Mixer.addRecipe1(T, 16, 32, OM.dust(MT.Greatwood      ), FL.Glue.make( 50), NF, IL.Pellet_Wood.get(2));
		RM.Mixer.addRecipe1(T, 16, 16, OM.dust(MT.Dreamwood  , U4), FL.Glue.make( 25), NF, IL.Pellet_Wood.get(1));
		RM.Mixer.addRecipe1(T, 16, 64, OM.dust(MT.Dreamwood      ), FL.Glue.make(100), NF, IL.Pellet_Wood.get(4));
		RM.Mixer.addRecipe1(T, 16, 16, OM.dust(MT.Shimmerwood, U4), FL.Glue.make( 25), NF, IL.Pellet_Wood.get(1));
		RM.Mixer.addRecipe1(T, 16, 64, OM.dust(MT.Shimmerwood    ), FL.Glue.make(100), NF, IL.Pellet_Wood.get(4));
		RM.Mixer.addRecipe1(T, 16, 16, OM.dust(MT.Silverwood , U4), FL.Glue.make( 25), NF, IL.Pellet_Wood.get(1));
		RM.Mixer.addRecipe1(T, 16, 64, OM.dust(MT.Silverwood     ), FL.Glue.make(100), NF, IL.Pellet_Wood.get(4));
		for (OreDictMaterial tWood : ANY.Wood.mToThis)
		RM.Mixer.addRecipe1(T, 16, 16, OM.dust(tWood             ), FL.Glue.make( 25), NF, IL.Pellet_Wood.get(1));
		RM.Mixer.addRecipe1(T, 16, 16, OM.dust(MT.Bark           ), FL.Glue.make( 25), NF, IL.Pellet_Wood.get(1));
		RM.Compressor.addRecipe1(T, 16, 16, IL.Pellet_Wood.get(2), ST.make(BlocksGT.Planks, 1, 8));
		RM.CokeOven.addRecipe1(T, 0, 900, IL.Pellet_Wood.get(1), NF, FL.Oil_Creosote.make(25), chunkGt.mat(MT.Charcoal, 1));
		});
		
		
		aItems.add(6000, aID -> {IL.Module_Stone_Generator          .set(addItem(aID, "Stone Generator Module"           , "Generates Stone for Recipes, also known as a Cobble Generator", TC.stack(TC.MACHINA, 1), TC.stack(TC.FABRICO, 1), TC.stack(TC.IGNIS, 1), TC.stack(TC.TERRA, 1), TC.stack(TC.AQUA, 1)));});
		aItems.add(6001, aID -> {IL.Module_Basalt_Generator         .set(addItem(aID, "Basalt Generator Module"          , "Generates Basalt for Recipes"                                 , TC.stack(TC.MACHINA, 1), TC.stack(TC.FABRICO, 1), TC.stack(TC.IGNIS, 1), TC.stack(TC.TERRA, 1), TC.stack(TC.AQUA, 1)));});
		aItems.add(6002, aID -> {
			IL.Module_Blackstone_Generator     .set(addItem(aID, "Blackstone Generator Module"      , "Generates Blackstone for Recipes"                             , TC.stack(TC.MACHINA, 1), TC.stack(TC.FABRICO, 1), TC.stack(TC.IGNIS, 1), TC.stack(TC.TERRA, 1), TC.stack(TC.AQUA, 1)));
		CR.shaped(IL.Module_Stone_Generator     .get(1), CR.DEF_REV_NCC, "CPC", "LMW", "COC", 'M', OP.casingMachine.dat(MT.SteelGalvanized), 'O', IL.Shape_Extruder_Block, 'C', OD_CIRCUITS[4], 'P', OD.craftingPiston, 'L', OD.container1000lava, 'W', OD.container1000water);
		CR.shaped(IL.Module_Basalt_Generator    .get(1), CR.DEF_REV_NCC, "S", "M", "I", 'M', IL.Module_Stone_Generator, 'S', OD.soulsand, 'I', Blocks.PACKED_ICE);
		CR.shaped(IL.Module_Blackstone_Generator.get(1), CR.DEF_REV_NCC, "S", "M", "O", 'M', IL.Module_Stone_Generator, 'S', OD.soulsand, 'O', OD.obsidian);
		});
		
		
		aItems.add(7002, aID -> {IL.Paper_Printed_Pages             .set(addItem(aID, "Printed Pages"                    , ""                                                            , Behavior_PrintedPages.INSTANCE, new OreDictItemData(MT.Paper, U*3), TC.stack(TC.COGNITIO, 3)));});
		aItems.add(7003, aID -> {
			IL.Paper_Printed_Pages_Many        .set(addItem(aID, "Many Printed Pages"               , ""                                                            , Behavior_PrintedPages.INSTANCE, new OreDictItemData(MT.Paper, U*6), TC.stack(TC.COGNITIO, 6)));
		BooksGT.BOOK_REGISTER.put(IL.Paper_Printed_Pages     , (byte)26);
		BooksGT.BOOK_REGISTER.put(IL.Paper_Printed_Pages_Many, (byte)26);
		});
		
		aItems.add(7010, aID -> {IL.Paper_Blueprint_Empty           .set(addItem(aID, "Empty Blueprint"                  , "Place in Blueprint Slot and Shiftclick it, to assign Recipe" , new OreDictItemData(MT.Paper, U), TC.stack(TC.COGNITIO, 1)));});
		aItems.add(7011, aID -> {
			IL.Paper_Blueprint_Used            .set(addItem(aID, "Blueprint"                        , "Blueprint containing a Crafting Recipe"                      , new OreDictItemData(MT.Paper, U), TC.stack(TC.COGNITIO, 2), "gt:autocrafterblueprintitem"));
		for (FluidStack tDye : DYE_FLUIDS[DYE_INDEX_Blue])
		RM.Bath.addRecipe1(T, 0, 16, ST.make(Items.PAPER, 1, W), tDye, NF, IL.Paper_Blueprint_Empty.get(1));
		BooksGT.BOOK_REGISTER.put(IL.Paper_Blueprint_Empty, (byte)25);
		BooksGT.BOOK_REGISTER.put(IL.Paper_Blueprint_Used , (byte)28);
		ItemsGT.addNEIRedirects(IL.Paper_Blueprint_Empty.get(1), IL.Paper_Blueprint_Used.get(1));
		});
		
		
		for (int i = 0; i < 16; i++)            {
			final int tIndex = i;
			aItems.add(tIndex       +  7030, aID -> {addItem(aID, DYE_NAMES[tIndex] + " Canvas"           , "Can be used together with the Obscurator"    , "gt:canvas", new CoverTextureCanvas(BlockTextureDefault.get("machines/covers/canvas", DYES[tIndex])), new OreDictItemData(MT.Paper, U), TC.stack(TC.PANNUS, 1));});
		}
		aItems.then(() -> {
			for (FluidStack tDye : DYE_FLUIDS[DYE_INDEX_White])
		RM.Bath.addRecipe1(T, 0, 16, ST.make(Items.PAPER, 1, W), tDye, NF, make(7030+DYE_INDEX_White));
		for (int i = 0; i < 16; i++) if (i != DYE_INDEX_White) for (FluidStack tDye : DYE_FLUIDS[i])
		RM.Bath.addRecipe1(T, 0, 16, make(7030+DYE_INDEX_White), tDye, NF, make(7030+i));
		for (int i = 0; i < 16; i++)
		BooksGT.BOOK_REGISTER.put(this, 7030+i, (byte)27);
		});
		
		
		aItems.add(7990, aID -> {IL.Paper_Magic_Research_0          .set(addItem(aID, "Magic Research Paper (Introduction)", "", new OreDictItemData(MT.Paper, U), TC.TERRA   .get(8), TC.AQUA        .get(8), TC.AER     .get(8), TC.IGNIS   .get(8), TC.PERDITIO .get(8), TC.ORDO      .get(8)));});
		aItems.add(7991, aID -> {IL.Paper_Magic_Research_1          .set(addItem(aID, "Magic Research Paper (Page 1 / 8)"  , "", new OreDictItemData(MT.Paper, U), TC.LUX     .get(8), TC.MOTUS       .get(8), TC.VACUOS  .get(8), TC.POTENTIA.get(8), TC.VITREUS  .get(8), TC.VICTUS    .get(8)));});
		aItems.add(7992, aID -> {IL.Paper_Magic_Research_2          .set(addItem(aID, "Magic Research Paper (Page 2 / 8)"  , "", new OreDictItemData(MT.Paper, U), TC.VOLATUS .get(8), TC.ITER        .get(8), TC.MORTUUS .get(8), TC.BESTIA  .get(8), TC.TENEBRAE .get(8), TC.PRAECANTIO.get(8)));});
		aItems.add(7993, aID -> {IL.Paper_Magic_Research_3          .set(addItem(aID, "Magic Research Paper (Page 3 / 8)"  , "", new OreDictItemData(MT.Paper, U), TC.ALIENIS .get(8), TC.EXAMINIS    .get(8), TC.SPIRITUS.get(8), TC.VITIUM  .get(8), TC.METALLUM .get(8), TC.GELUM     .get(8)));});
		aItems.add(7994, aID -> {IL.Paper_Magic_Research_4          .set(addItem(aID, "Magic Research Paper (Page 4 / 8)"  , "", new OreDictItemData(MT.Paper, U), TC.COGNITIO.get(8), TC.SENSUS      .get(8), TC.HERBA   .get(8), TC.SANO    .get(8), TC.LIMUS    .get(8), TC.TEMPESTAS .get(8)));});
		aItems.add(7995, aID -> {IL.Paper_Magic_Research_5          .set(addItem(aID, "Magic Research Paper (Page 5 / 8)"  , "", new OreDictItemData(MT.Paper, U), TC.HUMANUS .get(8), TC.VINCULUM    .get(8), TC.ARBOR   .get(8), TC.FAMES   .get(8), TC.CORPUS   .get(8), TC.AURAM     .get(8)));});
		aItems.add(7996, aID -> {IL.Paper_Magic_Research_6          .set(addItem(aID, "Magic Research Paper (Page 6 / 8)"  , "", new OreDictItemData(MT.Paper, U), TC.PERFODIO.get(8), TC.INSTRUMENTUM.get(8), TC.MESSIS  .get(8), TC.LUCRUM  .get(8), TC.VENEMUM  .get(8), TC.PERMUTATIO.get(8)));});
		aItems.add(7997, aID -> {IL.Paper_Magic_Research_7          .set(addItem(aID, "Magic Research Paper (Page 7 / 8)"  , "", new OreDictItemData(MT.Paper, U), TC.TELUM   .get(8), TC.TUTAMEN     .get(8), TC.PANNUS  .get(8), TC.FABRICO .get(8), TC.METO     .get(8), TC.MACHINA   .get(8)));});
		aItems.add(7998, aID -> {
			IL.Paper_Magic_Research_8          .set(addItem(aID, "Magic Research Paper (Page 8 / 8)"  , "", new OreDictItemData(MT.Paper, U), TC.ELECTRUM.get(8), TC.RADIO       .get(8), TC.MAGNETO .get(8), TC.NEBRISUM.get(8), TC.STRONTIO .get(8), TC.REFLEXIO  .get(8)));
		
		for (int i = 0; i < IL.MAGIC_RESEARCH_PAPERS.length; i++) {
			BooksGT.BOOK_REGISTER.put(IL.MAGIC_RESEARCH_PAPERS[i], (byte)18);
			if (IL.TC_Knowledge_Fragment.exists()) {
				RM.generify(IL.MAGIC_RESEARCH_PAPERS[i].get(1), IL.TC_Knowledge_Fragment.get(Math.max(1, i)));
				CR.shapeless(IL.TC_Knowledge_Fragment.get(Math.max(1, i)), CR.DEF, new Object[] {IL.MAGIC_RESEARCH_PAPERS[i]});
			}
		}
		});

		aItems.add(8000, aID -> {IL.Robot_Tip_Wrench                .set(addItem(aID, "Robot Arm Wrench Tip"         , "Infinitely usable inside an Autocrafter", OreDictToolNames.wrench                                                          , "gt:autocrafterinfinite", TC.stack(TC.INSTRUMENTUM, 4)));});
		aItems.add(8001, aID -> {IL.Robot_Tip_Screwdriver           .set(addItem(aID, "Robot Arm Screwdriver Tip"    , "Infinitely usable inside an Autocrafter", OreDictToolNames.screwdriver                                                     , "gt:autocrafterinfinite", TC.stack(TC.INSTRUMENTUM, 4)));});
		aItems.add(8002, aID -> {IL.Robot_Tip_Saw                   .set(addItem(aID, "Robot Arm Saw Tip"            , "Infinitely usable inside an Autocrafter", OreDictToolNames.saw                                                             , "gt:autocrafterinfinite", TC.stack(TC.INSTRUMENTUM, 4)));});
		aItems.add(8003, aID -> {IL.Robot_Tip_Hammer                .set(addItem(aID, "Robot Arm Hammer Tip"         , "Infinitely usable inside an Autocrafter", OreDictToolNames.hammer, OreDictToolNames.hac_mortar                             , "gt:autocrafterinfinite", TC.stack(TC.INSTRUMENTUM, 4)));});
		aItems.add(8004, aID -> {IL.Robot_Tip_Cutter                .set(addItem(aID, "Robot Arm Cutter Tip"         , "Infinitely usable inside an Autocrafter", OreDictToolNames.wirecutter, OreDictToolNames.scissors                           , "gt:autocrafterinfinite", TC.stack(TC.INSTRUMENTUM, 4)));});
		aItems.add(8005, aID -> {IL.Robot_Tip_Chisel                .set(addItem(aID, "Robot Arm Chisel Tip"         , "Infinitely usable inside an Autocrafter", OreDictToolNames.chisel                                                          , "gt:autocrafterinfinite", TC.stack(TC.INSTRUMENTUM, 4)));});
		aItems.add(8006, aID -> {IL.Robot_Tip_Rubber                .set(addItem(aID, "Robot Arm Rubber Hammer Tip"  , "Infinitely usable inside an Autocrafter", OreDictToolNames.softhammer                                                      , "gt:autocrafterinfinite", TC.stack(TC.INSTRUMENTUM, 4)));});
		aItems.add(8007, aID -> {IL.Robot_Tip_Blade                 .set(addItem(aID, "Robot Arm Blade Tip"          , "Infinitely usable inside an Autocrafter", OreDictToolNames.sword, OreDictToolNames.blade, OreDictToolNames.hac_cuttingboard, "gt:autocrafterinfinite", TC.stack(TC.INSTRUMENTUM, 4)));});
		aItems.add(8008, aID -> {IL.Robot_Tip_Drill                 .set(addItem(aID, "Robot Arm Drill Tip"          , "Infinitely usable inside an Autocrafter", OreDictToolNames.drill, OreDictToolNames.handdrill                               , "gt:autocrafterinfinite", TC.stack(TC.INSTRUMENTUM, 4)));});
		aItems.add(8009, aID -> {
			IL.Robot_Tip_File                  .set(addItem(aID, "Robot Arm File Tip"           , "Infinitely usable inside an Autocrafter", OreDictToolNames.file                                                            , "gt:autocrafterinfinite", TC.stack(TC.INSTRUMENTUM, 4)));
		
		CR.shaped(IL.Robot_Tip_Wrench      .get(1), CR.DEF_REV, "wPh", "CMC", " X ", 'P', OP.plateCurved.dat(MT.SteelGalvanized), 'C', OD_CIRCUITS[3], 'M', IL.MOTORS   [3], 'X', OP.toolHeadWrench.dat(MT.Cr));
		CR.shaped(IL.Robot_Tip_Screwdriver .get(1), CR.DEF_REV, "wPh", "CMC", " X ", 'P', OP.plateCurved.dat(MT.SteelGalvanized), 'C', OD_CIRCUITS[3], 'M', IL.MOTORS   [3], 'X', OP.toolHeadScrewdriver.dat(MT.StainlessSteel));
		CR.shaped(IL.Robot_Tip_Saw         .get(1), CR.DEF_REV, "wPh", "CMC", "DXd", 'P', OP.plateCurved.dat(MT.SteelGalvanized), 'C', OD_CIRCUITS[3], 'M', IL.MOTORS   [3], 'X', OP.toolHeadBuzzSaw.dat(MT.CobaltBrass), 'D', OP.dust.dat(ANY.Diamond));
		CR.shaped(IL.Robot_Tip_Hammer      .get(1), CR.DEF_REV, "wPh", "CMC", " X ", 'P', OP.plateCurved.dat(MT.SteelGalvanized), 'C', OD_CIRCUITS[3], 'M', IL.PISTONS  [3], 'X', OP.toolHeadHammer.dat(MT.TungstenCarbide));
		CR.shaped(IL.Robot_Tip_Cutter      .get(1), CR.DEF_REV, "wPh", "CMC", "XfX", 'P', OP.plateCurved.dat(MT.SteelGalvanized), 'C', OD_CIRCUITS[3], 'M', IL.MOTORS   [3], 'X', OP.plate.dat(MT.StainlessSteel));
		CR.shaped(IL.Robot_Tip_Chisel      .get(1), CR.DEF_REV, "wPh", "CMC", " X ", 'P', OP.plateCurved.dat(MT.SteelGalvanized), 'C', OD_CIRCUITS[3], 'M', IL.PISTONS  [3], 'X', OP.toolHeadChisel.dat(MT.TungstenSteel));
		CR.shaped(IL.Robot_Tip_Rubber      .get(1), CR.DEF_REV, "wPh", "CMC", " X ", 'P', OP.plateCurved.dat(MT.SteelGalvanized), 'C', OD_CIRCUITS[3], 'M', IL.PISTONS  [3], 'X', OP.toolHeadHammer.dat(ANY.Rubber));
		CR.shaped(IL.Robot_Tip_Blade       .get(1), CR.DEF_REV, "wPh", "CMC", " X ", 'P', OP.plateCurved.dat(MT.SteelGalvanized), 'C', OD_CIRCUITS[3], 'M', IL.PISTONS  [3], 'X', OP.toolHeadSword.dat(MT.Bronze));
		CR.shaped(IL.Robot_Tip_Drill       .get(1), CR.DEF_REV, "wPh", "CMC", "fX ", 'P', OP.plateCurved.dat(MT.SteelGalvanized), 'C', OD_CIRCUITS[3], 'M', IL.MOTORS   [3], 'X', OP.stick.dat(ANY.Steel));
		CR.shaped(IL.Robot_Tip_File        .get(1), CR.DEF_REV, "wPh", "CMC", " X ", 'P', OP.plateCurved.dat(MT.SteelGalvanized), 'C', OD_CIRCUITS[3], 'M', IL.CONVEYERS[3], 'X', OP.dust.dat(ANY.Diamond));
		});

		aItems.add(8500, aID -> {IL.Tool_Token_Wrench               .set(addItem(aID, "Single Use Wrench"       , "Non-Functional-Tool for Crafting only", OreDictToolNames.wrench                                                          , TC.stack(TC.INSTRUMENTUM, 1)));});
		aItems.add(8501, aID -> {IL.Tool_Token_Screwdriver          .set(addItem(aID, "Single Use Screwdriver"  , "Non-Functional-Tool for Crafting only", OreDictToolNames.screwdriver                                                     , TC.stack(TC.INSTRUMENTUM, 1)));});
		aItems.add(8502, aID -> {IL.Tool_Token_Saw                  .set(addItem(aID, "Single Use Saw"          , "Non-Functional-Tool for Crafting only", OreDictToolNames.saw                                                             , TC.stack(TC.INSTRUMENTUM, 1)));});
		aItems.add(8503, aID -> {IL.Tool_Token_Hammer               .set(addItem(aID, "Single Use Hammer"       , "Non-Functional-Tool for Crafting only", OreDictToolNames.hammer, OreDictToolNames.hac_mortar                             , TC.stack(TC.INSTRUMENTUM, 1)));});
		aItems.add(8504, aID -> {IL.Tool_Token_Cutter               .set(addItem(aID, "Single Use Cutter"       , "Non-Functional-Tool for Crafting only", OreDictToolNames.wirecutter, OreDictToolNames.scissors                           , TC.stack(TC.INSTRUMENTUM, 1)));});
		aItems.add(8505, aID -> {IL.Tool_Token_Chisel               .set(addItem(aID, "Single Use Chisel"       , "Non-Functional-Tool for Crafting only", OreDictToolNames.chisel                                                          , TC.stack(TC.INSTRUMENTUM, 1)));});
		aItems.add(8506, aID -> {IL.Tool_Token_Rubber               .set(addItem(aID, "Single Use Rubber Hammer", "Non-Functional-Tool for Crafting only", OreDictToolNames.softhammer                                                      , TC.stack(TC.INSTRUMENTUM, 1)));});
		aItems.add(8507, aID -> {IL.Tool_Token_Blade                .set(addItem(aID, "Single Use Blade"        , "Non-Functional-Tool for Crafting only", OreDictToolNames.sword, OreDictToolNames.blade, OreDictToolNames.hac_cuttingboard, TC.stack(TC.INSTRUMENTUM, 1)));});
		aItems.add(8508, aID -> {IL.Tool_Token_Drill                .set(addItem(aID, "Single Use Drill"        , "Non-Functional-Tool for Crafting only", OreDictToolNames.drill, OreDictToolNames.handdrill                               , TC.stack(TC.INSTRUMENTUM, 1)));});
		aItems.add(8509, aID -> {
			IL.Tool_Token_File                 .set(addItem(aID, "Single Use File"         , "Non-Functional-Tool for Crafting only", OreDictToolNames.file                                                            , TC.stack(TC.INSTRUMENTUM, 1)));
		
		RM.Boxinator.addRecipe2(T, 16, 16* 2, OP.plateTiny.mat(MT.Steel , 1), IL.Robot_Tip_Wrench     .get(0), IL.Tool_Token_Wrench     .get( 2));
		RM.Boxinator.addRecipe2(T, 16, 16*32, OP.plateTiny.mat(MT.Steel , 1), IL.Robot_Tip_Screwdriver.get(0), IL.Tool_Token_Screwdriver.get(32));
		RM.Boxinator.addRecipe2(T, 16, 16*32, OP.plateTiny.mat(MT.Steel , 1), IL.Robot_Tip_Saw        .get(0), IL.Tool_Token_Saw        .get(32));
		RM.Boxinator.addRecipe2(T, 16, 16* 2, OP.plateTiny.mat(MT.Steel , 1), IL.Robot_Tip_Hammer     .get(0), IL.Tool_Token_Hammer     .get( 2));
		RM.Boxinator.addRecipe2(T, 16, 16* 4, OP.plateTiny.mat(MT.Steel , 1), IL.Robot_Tip_Cutter     .get(0), IL.Tool_Token_Cutter     .get( 4));
		RM.Boxinator.addRecipe2(T, 16, 16* 9, OP.plateTiny.mat(MT.Steel , 1), IL.Robot_Tip_Chisel     .get(0), IL.Tool_Token_Chisel     .get( 9));
		RM.Boxinator.addRecipe2(T, 16, 16* 4, OP.nugget   .mat(MT.Rubber, 1), IL.Robot_Tip_Rubber     .get(0), IL.Tool_Token_Rubber     .get( 4));
		RM.Boxinator.addRecipe2(T, 16, 16*32, OP.plateTiny.mat(MT.Steel , 1), IL.Robot_Tip_Blade      .get(0), IL.Tool_Token_Blade      .get(32));
		RM.Boxinator.addRecipe2(T, 16, 16*32, OP.plateTiny.mat(MT.Steel , 1), IL.Robot_Tip_Drill      .get(0), IL.Tool_Token_Drill      .get(32));
		RM.Boxinator.addRecipe2(T, 16, 16* 9, OP.plateTiny.mat(MT.Steel , 1), IL.Robot_Tip_File       .get(0), IL.Tool_Token_File       .get( 9));
		});
		
		
		aItems.add(9000, aID -> {IL.Tool_Remote_Activator           .set(addItem(aID, "Remote Activator"         , "", Behavior_Remote.INSTANCE, TC.stack(TC.MOTUS, 1), TC.stack(TC.PERMUTATIO, 1)));});
		aItems.add(9001, aID -> {IL.Tool_Cheat                      .set(addItem(aID, "Debug Scanner"            , "", Behavior_Cropnalyzer.INSTANCE, ItemsGT.DEBUG_ITEMS, ItemsGT.ILLEGAL_DROPS, GarbageGT.BLACKLIST, new Behavior_Scanner(Integer.MAX_VALUE), EnergyStatDebug.INSTANCE, TC.stack(TC.SENSUS,10), TC.stack(TC.INSTRUMENTUM,10)));});
		aItems.add(9002, aID -> {IL.Tool_Scanner                    .set(addItem(aID, "Portable Scanner"         , "", Behavior_Cropnalyzer.INSTANCE, new Behavior_Scanner(2), EnergyStat.makeTool(TD.Energy.EU, V[3]*8000, V[3], 64, next()), TC.stack(TC.SENSUS,10), TC.stack(TC.INSTRUMENTUM,10)));});
		aItems.add(9003, aID -> {IL.Tool_Cropnalyzer                .set(addItem(aID, "Portable Cropnalyzer"     , "", Behavior_Cropnalyzer.INSTANCE, EnergyStat.makeTool(TD.Energy.EU, V[2]*8000, V[2], 64, next()), TC.stack(TC.SENSUS, 5), TC.stack(TC.INSTRUMENTUM, 5), TC.stack(TC.HERBA, 5)));});
		aItems.add(9998, aID -> {IL.Tool_Chunk_Remover              .set(addItem(aID, "Chunk Eraser"             , "", Behavior_Chunk_Remover.INSTANCE, ItemsGT.DEBUG_ITEMS, ItemsGT.ILLEGAL_DROPS, GarbageGT.BLACKLIST, TC.stack(TC.TERRA,10), TC.stack(TC.VACUOS,10), TC.stack(TC.INSTRUMENTUM,10)));});
		aItems.add(9999, aID -> {
			IL.Tool_Worldgen_Debugger          .set(addItem(aID, "Worldgen Debug Wand"      , "", Behavior_Worldgen_Debugger.INSTANCE, ItemsGT.DEBUG_ITEMS, ItemsGT.ILLEGAL_DROPS, GarbageGT.BLACKLIST, TC.stack(TC.TERRA,10), TC.stack(TC.PRAECANTIO,10), TC.stack(TC.INSTRUMENTUM,10)));
		
		CR.shaped(IL.Tool_Remote_Activator      .get(1), CR.DEF_REV, "TPE", "BCd", "xPT", 'P', OP.plate.dat(MT.Cr), 'T', OP.screw.dat(MT.Cr), 'C', OD_CIRCUITS[4], 'E', IL.EMITTERS[4], 'B', OD.button);
		BooksGT.BOOK_REGISTER.put(IL.Tool_Remote_Activator, (byte)1);
		});
//      Moved to the Battery Section of the MTE Loader.
//      CR.shaped(IL.Tool_Scanner               .get(1), CR.DEF_REV, "EXR", "CPU", "BXB", 'B', IL.Battery_Alkaline_HV, 'X', OP.plate.dat(MT.Cr), 'U', OD_USB_STICKS[3], 'C', OD_USB_CABLES[3], 'E', IL.EMITTERS[4], 'R', IL.SENSORS[4], 'P', IL.Processor_Crystal_Sapphire);
//      CR.shaped(IL.Tool_Cropnalyzer           .get(1), CR.DEF_REV, "EXR", "CPU", "BXB", 'B', IL.Battery_Alkaline_MV, 'X', OP.plate.dat(MT.Al), 'U', OD_USB_STICKS[1], 'C', OD_USB_CABLES[1], 'E', IL.EMITTERS[2], 'R', IL.SENSORS[2], 'P', OD_CIRCUITS[6]);
		
		aItems.add(10000, aID -> {IL.Thermometer_Quicksilver         .set(addItem(aID, "Quicksilver Thermometer"  , "Measures Temperature"                   , new Behavior_Tool(TOOL_thermometer  ), TC.stack(TC.SENSUS, 2), TC.stack(TC.IGNIS, 2), TC.stack(TC.GELUM, 2)));});
		aItems.add(10001, aID -> {IL.Geiger_Counter_Empty            .set(addItem(aID, "Geiger Counter (Empty)"   , "Fill with proper inert Gas"                                                    , TC.stack(TC.SENSUS, 1), TC.stack(TC.RADIO, 1)));});
		aItems.add(10002, aID -> {IL.Geiger_Counter                  .set(addItem(aID, "Geiger Counter"           , "Measures Neutron Count"                 , new Behavior_Tool(TOOL_geigercounter), TC.stack(TC.SENSUS, 5), TC.stack(TC.RADIO, 5)));});
		aItems.add(10003, aID -> {IL.Electro_Meter                   .set(addItem(aID, "Electrometer"             , "Measures Electricity"                   , new Behavior_Tool(TOOL_electrometer ), TC.stack(TC.SENSUS, 5), TC.stack(TC.ELECTRUM, 5)));});
		aItems.add(10004, aID -> {
			IL.Tacho_Meter                     .set(addItem(aID, "Tachometer"               , "Measures Rotation Speed"                , new Behavior_Tool(TOOL_tachometer   ), TC.stack(TC.SENSUS, 5), TC.stack(TC.MOTUS, 5)));
		
		CR.shaped(IL.Thermometer_Quicksilver    .get(1), CR.DEF_REV, " GD", "GQG", "PG ", 'P', OP.plate.dat(ANY.Cu), 'G', OP.plate.dat(MT.Glass), 'D', DYE_OREDICTS[DYE_INDEX_Red], 'Q', OD.itemQuicksilver);
		CR.shaped(IL.Geiger_Counter_Empty       .get(1), CR.DEF_REV, "TXT", "PCP", "TdT", 'X', OP.capcellcon .dat(MT.Al   ), 'P', OP.plate.dat(MT.Al             ), 'T', OP.screw.dat(MT.Al             ), 'C', OD_CIRCUITS[1]);
		CR.shaped(IL.Electro_Meter              .get(1), CR.DEF_REV, "TXT", "PCP", "TdT", 'X', OP.wireFine   .dat(ANY.Cu  ), 'P', OP.plate.dat(MT.SteelGalvanized), 'T', OP.screw.dat(MT.SteelGalvanized), 'C', OD_CIRCUITS[1]);
		CR.shaped(IL.Tacho_Meter                .get(1), CR.DEF_REV, "TXT", "PCP", "TdT", 'X', OP.gearGtSmall.dat(MT.Brass), 'P', OP.plate.dat(MT.Brass          ), 'T', OP.screw.dat(MT.Brass          ), 'C', OD.itemRedstone);
		
		OM.data(IL.Geiger_Counter.get(1), OM.data(IL.Geiger_Counter_Empty.get(1)));
		
		RM.Canner.addRecipe1(T, 16, 64, IL.Geiger_Counter_Empty.get(1), FL.Helium.make(1000), NF, IL.Geiger_Counter.get(1));
		RM.Canner.addRecipe1(T, 16, 64, IL.Geiger_Counter_Empty.get(1), FL.Neon  .make(1000), NF, IL.Geiger_Counter.get(1));
		RM.Canner.addRecipe1(T, 16, 64, IL.Geiger_Counter_Empty.get(1), FL.Argon .make(1000), NF, IL.Geiger_Counter.get(1));
		});
		
		
		aItems.add(11000, aID -> {IL.Compass_North                   .set(addItem(aID, "Compass (NORTH)" , "Points to Zed Negative"                             , OD.itemCompass, new Behavior_Switch_Metadata(11001, T, T), OM.data(ST.make(Items.COMPASS, 1, 0)), TC.stack(TC.METALLUM, 2), TC.stack(TC.MAGNETO                                              , 2))); CR.shapeless(ST.make(Items.COMPASS, 1, 0), CR.DEF, new Object[] {last()});});
		aItems.add(11001, aID -> {IL.Compass_Face                    .set(addItem(aID, "Compass (FACE)"  , "Points into the direction you look"                 , OD.itemCompass, new Behavior_Switch_Metadata(11002, T, T), OM.data(ST.make(Items.COMPASS, 1, 0)), TD.Creative.HIDDEN, TC.stack(TC.METALLUM, 2), TC.stack(TC.MAGNETO, 1), TC.stack(TC.COGNITIO, 1))); CR.shapeless(ST.make(Items.COMPASS, 1, 0), CR.DEF, new Object[] {last()});});
		aItems.add(11002, aID -> {IL.Compass_Spawn                   .set(addItem(aID, "Compass (SPAWN)" , "Points to the World Spawn like a normal Compass"    , OD.itemCompass, new Behavior_Switch_Metadata(11003, T, T), OM.data(ST.make(Items.COMPASS, 1, 0)), TD.Creative.HIDDEN, TC.stack(TC.METALLUM, 2), TC.stack(TC.MAGNETO, 1), TC.stack(TC.HUMANUS , 1))); CR.shapeless(ST.make(Items.COMPASS, 1, 0), CR.DEF, new Object[] {last()});});
		aItems.add(11003, aID -> {IL.Compass_Center                  .set(addItem(aID, "Compass (CENTER)", "Points towards the middle of the World"             , OD.itemCompass, new Behavior_Switch_Metadata(11004, T, T), OM.data(ST.make(Items.COMPASS, 1, 0)), TD.Creative.HIDDEN, TC.stack(TC.METALLUM, 2), TC.stack(TC.MAGNETO, 1), TC.stack(TC.TERRA   , 1))); CR.shapeless(ST.make(Items.COMPASS, 1, 0), CR.DEF, new Object[] {last()});});
		aItems.add(11004, aID -> {
			IL.Compass_Death                   .set(addItem(aID, "Compass (DEATH)" , "Points to your last Death, if you did not Rage Quit", OD.itemCompass, new Behavior_Switch_Metadata(11000, T, T), OM.data(ST.make(Items.COMPASS, 1, 0)), TD.Creative.HIDDEN, TC.stack(TC.METALLUM, 2), TC.stack(TC.MAGNETO, 1), TC.stack(TC.MORTUUS , 1))); CR.shapeless(ST.make(Items.COMPASS, 1, 0), CR.DEF, new Object[] {last()});
		ItemsGT.addNEIRedirects(IL.Compass_North.get(1), IL.Compass_Face.get(1), IL.Compass_Spawn.get(1), IL.Compass_Center.get(1), IL.Compass_Death.get(1));
		CR.shapeless(IL.Compass_North.get(1), CR.DEF, new Object[] {Items.COMPASS});
		});
		
		
		aItems.add(12000, aID -> {IL.Tape                            .set(addItem(aID, "Tape", "Full Roll", TC.stack(TC.PANNUS, 1), TC.stack(TC.LIMUS, 1)));});
		aItems.add(12001, aID -> {
			IL.Tape_Used                       .set(addItem(aID, "Tape", "Used Roll", TC.stack(TC.PANNUS, 1), TC.stack(TC.LIMUS, 1)));
		CR.shaped   (IL.Tape.get(1), CR.DEF, "PPP", " G ", 'P', OD.paperEmpty, 'G', OD.itemGlue);
		CR.shapeless(IL.Tape.get(1), CR.DEF, new Object[] {IL.Tape_Used, IL.Tape_Used, IL.Tape_Used, IL.Tape_Used});
		addItemBehavior(new Behavior_Duct_Tape(null, IL.Tape_Used.get(1), IL.Tape.get(1), 0, 10000), mLastID, mLastID-1);
		ItemsGT.addNEIRedirects(IL.Tape.get(1), IL.Tape_Used.get(1));
		BooksGT.BOOK_REGISTER.put(IL.Tape           , (byte)57);
		BooksGT.BOOK_REGISTER.put(IL.Tape_Used      , (byte)57);
		});
		
		aItems.add(12002, aID -> {IL.Duct_Tape                       .set(addItem(aID, "Duct Tape", "Full Roll", TC.stack(TC.FABRICO, 1), TC.stack(TC.LIMUS, 1), OD.craftingDuctTape));});
		aItems.add(12003, aID -> {
			IL.Duct_Tape_Used                  .set(addItem(aID, "Duct Tape", "Used Roll", TC.stack(TC.FABRICO, 1), TC.stack(TC.LIMUS, 1)));
		CR.shaped   (IL.Duct_Tape.get(1), CR.DEF, "PPP", " G ", 'P', OP.foil.dat(ANY.Plastic), 'G', OD.itemGlue);
		CR.shapeless(IL.Duct_Tape.get(1), CR.DEF, new Object[] {IL.Duct_Tape_Used, IL.Duct_Tape_Used, IL.Duct_Tape_Used, IL.Duct_Tape_Used});
		addItemBehavior(new Behavior_Duct_Tape(null, IL.Duct_Tape_Used.get(1), IL.Duct_Tape.get(1), 1, 100000), mLastID, mLastID-1);
		ItemsGT.addNEIRedirects(IL.Duct_Tape.get(1), IL.Duct_Tape_Used.get(1));
		BooksGT.BOOK_REGISTER.put(IL.Duct_Tape      , (byte)58);
		BooksGT.BOOK_REGISTER.put(IL.Duct_Tape_Used , (byte)58);
		});
		
		aItems.add(12008, aID -> {IL.Brain_Tape                      .set(addItem(aID, "BrainTech Aerospace Advanced Reinforced Duct Tape FAL-84", "Full Roll", TC.stack(TC.TUTAMEN, 1), TC.stack(TC.LIMUS, 1), OD.craftingDuctTape));});
		aItems.add(12009, aID -> {
			IL.Brain_Tape_Used                 .set(addItem(aID, "BrainTech Aerospace Advanced Reinforced Duct Tape FAL-84", "Used Roll", TC.stack(TC.TUTAMEN, 1), TC.stack(TC.LIMUS, 1), OD.craftingDuctTape));
		CR.shaped   (IL.Brain_Tape.get(1), CR.DEF, "PPP", " G ", 'P', OP.foil.dat(ANY.W), 'G', OD.itemGlue);
		CR.shapeless(IL.Brain_Tape.get(1), CR.DEF, new Object[] {IL.Brain_Tape_Used, IL.Brain_Tape_Used, IL.Brain_Tape_Used, IL.Brain_Tape_Used});
		addItemBehavior(new Behavior_Duct_Tape(null, IL.Brain_Tape_Used.get(1), IL.Brain_Tape.get(1), 2, 10000000), mLastID, mLastID-1);
		ItemsGT.addNEIRedirects(IL.Brain_Tape.get(1), IL.Brain_Tape_Used.get(1));
		BooksGT.BOOK_REGISTER.put(IL.Brain_Tape     , (byte)59);
		BooksGT.BOOK_REGISTER.put(IL.Brain_Tape_Used, (byte)59);
		});
		
		
		aItems.add(13000, aID -> {IL.Bag_Loot_Sapling                .set(addItem(aID, "Bagged Sapling", "Loot: A Sapling"  , OP.treeSapling, TC.stack(TC.LUCRUM, 1), TC.stack(TC.ARBOR   , 2), new Behavior_Drop_Loot("gt.saplings")));});
		aItems.add(13001, aID -> {IL.Bag_Loot_Seeds                  .set(addItem(aID, "Seed Pouch"    , "Loot: Some random Seeds"          , TC.stack(TC.LUCRUM, 1), TC.stack(TC.HERBA   , 2), new Behavior_Drop_Loot("gt.seeds")));});
		aItems.add(13002, aID -> {IL.Bag_Loot_Gems                   .set(addItem(aID, "Gem Pouch"     , "Loot: Gems, one of which Flawless", TC.stack(TC.LUCRUM, 3), TC.stack(TC.VITREUS , 3), new Behavior_Drop_Loot("gt.flawless", "gt.gems", "gt.gems")));});
		aItems.add(13003, aID -> {IL.Bag_Loot_Misc                   .set(addItem(aID, "Loot Pouch"    , "Loot: Misc Items"                 , TC.stack(TC.LUCRUM, 3), TC.stack(TC.NEBRISUM, 3), new Behavior_Drop_Loot("gt.misc")));});
		
		
		aItems.add(30000, aID -> {IL.Key_Iron                        .set(addItem(aID, "Iron Key"    , "", OD.itemKey, new OreDictItemData(ANY.Iron   , U4), Behavior_Key.INSTANCE, TC.stack(TC.MACHINA, 1))); CR.shaped(IL.Key_Iron    .get(3), CR.DEF_NCC, "fPx", 'P', OP.plate.dat(ANY.Iron));});
		aItems.add(30001, aID -> {IL.Key_Gold                        .set(addItem(aID, "Gold Key"    , "", OD.itemKey, new OreDictItemData(MT.Au      , U4), Behavior_Key.INSTANCE, TC.stack(TC.MACHINA, 1))); CR.shaped(IL.Key_Gold    .get(3), CR.DEF_NCC, "fPx", 'P', OP.plate.dat(MT.Au));});
		aItems.add(30002, aID -> {IL.Key_Copper                      .set(addItem(aID, "Copper Key"  , "", OD.itemKey, new OreDictItemData(ANY.Cu     , U4), Behavior_Key.INSTANCE, TC.stack(TC.MACHINA, 1))); CR.shaped(IL.Key_Copper  .get(3), CR.DEF_NCC, "fPx", 'P', OP.plate.dat(ANY.Cu));});
		aItems.add(30003, aID -> {IL.Key_Tin                         .set(addItem(aID, "Tin Key"     , "", OD.itemKey, new OreDictItemData(MT.Sn      , U4), Behavior_Key.INSTANCE, TC.stack(TC.MACHINA, 1))); CR.shaped(IL.Key_Tin     .get(3), CR.DEF_NCC, "fPx", 'P', OP.plate.dat(MT.Sn));});
		aItems.add(30004, aID -> {IL.Key_Bronze                      .set(addItem(aID, "Bronze Key"  , "", OD.itemKey, new OreDictItemData(MT.Bronze  , U4), Behavior_Key.INSTANCE, TC.stack(TC.MACHINA, 1))); CR.shaped(IL.Key_Bronze  .get(3), CR.DEF_NCC, "fPx", 'P', OP.plate.dat(MT.Bronze));});
		aItems.add(30005, aID -> {IL.Key_Brass                       .set(addItem(aID, "Brass Key"   , "", OD.itemKey, new OreDictItemData(MT.Brass   , U4), Behavior_Key.INSTANCE, TC.stack(TC.MACHINA, 1))); CR.shaped(IL.Key_Brass   .get(3), CR.DEF_NCC, "fPx", 'P', OP.plate.dat(MT.Brass));});
		aItems.add(30006, aID -> {IL.Key_Silver                      .set(addItem(aID, "Silver Key"  , "", OD.itemKey, new OreDictItemData(MT.Ag      , U4), Behavior_Key.INSTANCE, TC.stack(TC.MACHINA, 1))); CR.shaped(IL.Key_Silver  .get(3), CR.DEF_NCC, "fPx", 'P', OP.plate.dat(MT.Ag));});
		aItems.add(30007, aID -> {IL.Key_Platinum                    .set(addItem(aID, "Platinum Key", "", OD.itemKey, new OreDictItemData(MT.Pt      , U4), Behavior_Key.INSTANCE, TC.stack(TC.MACHINA, 1))); CR.shaped(IL.Key_Platinum.get(3), CR.DEF_NCC, "fPx", 'P', OP.plate.dat(MT.Pt));});
		aItems.add(30008, aID -> {IL.Key_Lead                        .set(addItem(aID, "Lead Key"    , "", OD.itemKey, new OreDictItemData(MT.Pb      , U4), Behavior_Key.INSTANCE, TC.stack(TC.MACHINA, 1))); CR.shaped(IL.Key_Lead    .get(3), CR.DEF_NCC, "fPx", 'P', OP.plate.dat(MT.Pb));});
		aItems.add(30009, aID -> {
			IL.Key_Plastic                     .set(addItem(aID, "Plastic Key" , "", OD.itemKey, new OreDictItemData(ANY.Plastic, U4), Behavior_Key.INSTANCE, TC.stack(TC.MACHINA, 1))); CR.shaped(IL.Key_Plastic .get(3), CR.DEF_NCC, "fPx", 'P', OP.plate.dat(ANY.Plastic));
		
		// In order to be able to hide Keys inside Bookshelves
		for (IItemContainer tContainer : IL.KEYS) BooksGT.BOOK_REGISTER.put(tContainer, (byte)1);
		});
	}
	
	public int getRenderPasses(int aMetaData) {return UT.Code.inside(11000, 11004, aMetaData) ? 2 : 1;}
	public boolean requiresMultipleRenderPasses() {return T;}
	
	@Override
	public Identifier getIcon(ItemStack aStack, int aRenderPass) {
		if (aRenderPass == 0) return getIconIndex(aStack);
		Player aPlayer = GT_API.api_proxy.getThePlayer();
		if (aPlayer == null) return getIconIndex(aStack);
		BlockPos aTarget;
		switch(ST.meta_(aStack)) {
		case 11000: return Textures.ItemIcons.COMPASS[UT.Code.roundDown(0.5+Textures.ItemIcons.COMPASS.length*(361260+aPlayer.getYRot())/360)%Textures.ItemIcons.COMPASS.length].getIcon(0);
		case 11001: return Textures.ItemIcons.COMPASS[UT.Code.roundDown(0.5+Textures.ItemIcons.COMPASS.length*(361260-aPlayer.getYRot())/360)%Textures.ItemIcons.COMPASS.length].getIcon(0);
		case 11003: return Textures.ItemIcons.COMPASS[UT.Code.roundDown(0.5+Textures.ItemIcons.COMPASS.length*(361170+aPlayer.getYRot()-Math.atan2(-aPlayer.getZ(), -aPlayer.getX())*180/Math.PI)/360)%Textures.ItemIcons.COMPASS.length].getIcon(0);
		case 11002: aTarget = aPlayer.level().getRespawnData().pos(); break;
		case 11004: aTarget = LAST_DEATH_OF_THE_PLAYER; break;
		default: return getIconIndex(aStack);
		}
		// Spin to Win!
		if (aTarget   == null) return Textures.ItemIcons.COMPASS[(int)(CLIENT_TIME % Textures.ItemIcons.COMPASS.length)].getIcon(0);
		double tDistance = aTarget.distToLowCornerSqr(UT.Code.roundDown(aPlayer.getX()), aTarget.getY(), UT.Code.roundDown(aPlayer.getZ()));
		if (tDistance <     1) return Textures.ItemIcons.COMPASS[(int)(CLIENT_TIME % Textures.ItemIcons.COMPASS.length)].getIcon(0);
		// Point to Target, but with a little jiggle for getting closer!
		int tIndex = Textures.ItemIcons.COMPASS.length + (UT.Code.roundDown(0.5+Textures.ItemIcons.COMPASS.length*(361170+aPlayer.getYRot()-Math.atan2(aTarget.getZ()+0.5-aPlayer.getZ(), aTarget.getX()+0.5-aPlayer.getX())*180/Math.PI)/360)%Textures.ItemIcons.COMPASS.length);
		if (tDistance <     4) return Textures.ItemIcons.COMPASS[(tIndex + (                            new Random(CLIENT_TIME   ).nextInt(3)-1)) % Textures.ItemIcons.COMPASS.length].getIcon(0);
		if (tDistance <    16) return Textures.ItemIcons.COMPASS[(tIndex + (CLIENT_TIME %  2 != 0 ? 0 : new Random(CLIENT_TIME/ 2).nextInt(3)-1)) % Textures.ItemIcons.COMPASS.length].getIcon(0);
		if (tDistance <   256) return Textures.ItemIcons.COMPASS[(tIndex + (CLIENT_TIME %  3 != 0 ? 0 : new Random(CLIENT_TIME/ 3).nextInt(3)-1)) % Textures.ItemIcons.COMPASS.length].getIcon(0);
		if (tDistance <  1024) return Textures.ItemIcons.COMPASS[(tIndex + (CLIENT_TIME %  4 != 0 ? 0 : new Random(CLIENT_TIME/ 4).nextInt(3)-1)) % Textures.ItemIcons.COMPASS.length].getIcon(0);
		if (tDistance <  4096) return Textures.ItemIcons.COMPASS[(tIndex + (CLIENT_TIME %  5 != 0 ? 0 : new Random(CLIENT_TIME/ 5).nextInt(3)-1)) % Textures.ItemIcons.COMPASS.length].getIcon(0);
		if (tDistance < 16384) return Textures.ItemIcons.COMPASS[(tIndex + (CLIENT_TIME % 10 != 0 ? 0 : new Random(CLIENT_TIME/10).nextInt(3)-1)) % Textures.ItemIcons.COMPASS.length].getIcon(0);
		if (tDistance < 65536) return Textures.ItemIcons.COMPASS[(tIndex + (CLIENT_TIME % 20 != 0 ? 0 : new Random(CLIENT_TIME/20).nextInt(3)-1)) % Textures.ItemIcons.COMPASS.length].getIcon(0);
		// Point straight to Target!
		return Textures.ItemIcons.COMPASS[tIndex % Textures.ItemIcons.COMPASS.length].getIcon(0);
	}
	
	@Override
	public Identifier getIconFromDamageForRenderPass(int aMetaData, int aRenderPass) {
		if (aRenderPass == 0) return getIconFromDamage(aMetaData);
		Player aPlayer = GT_API.api_proxy.getThePlayer();
		if (aPlayer == null) return getIconFromDamage(aMetaData);
		BlockPos aTarget;
		switch(aMetaData) {
		case 11000: return Textures.ItemIcons.COMPASS[UT.Code.roundDown(0.5+Textures.ItemIcons.COMPASS.length*(361260+aPlayer.getYRot())/360)%Textures.ItemIcons.COMPASS.length].getIcon(0);
		case 11001: return Textures.ItemIcons.COMPASS[UT.Code.roundDown(0.5+Textures.ItemIcons.COMPASS.length*(361260-aPlayer.getYRot())/360)%Textures.ItemIcons.COMPASS.length].getIcon(0);
		case 11003: return Textures.ItemIcons.COMPASS[UT.Code.roundDown(0.5+Textures.ItemIcons.COMPASS.length*(361170+aPlayer.getYRot()-Math.atan2(-aPlayer.getZ(), -aPlayer.getX())*180/Math.PI)/360)%Textures.ItemIcons.COMPASS.length].getIcon(0);
		case 11002: aTarget = aPlayer.level().getRespawnData().pos(); break;
		case 11004: aTarget = LAST_DEATH_OF_THE_PLAYER; break;
		default: return getIconFromDamage(aMetaData);
		}
		// Spin to Win!
		if (aTarget   == null) return Textures.ItemIcons.COMPASS[(int)(CLIENT_TIME % Textures.ItemIcons.COMPASS.length)].getIcon(0);
		double tDistance = aTarget.distToLowCornerSqr(UT.Code.roundDown(aPlayer.getX()), aTarget.getY(), UT.Code.roundDown(aPlayer.getZ()));
		if (tDistance <     1) return Textures.ItemIcons.COMPASS[(int)(CLIENT_TIME % Textures.ItemIcons.COMPASS.length)].getIcon(0);
		// Point to Target, but with a little jiggle for getting closer!
		int tIndex = Textures.ItemIcons.COMPASS.length + (UT.Code.roundDown(0.5+Textures.ItemIcons.COMPASS.length*(361170+aPlayer.getYRot()-Math.atan2(aTarget.getZ()+0.5-aPlayer.getZ(), aTarget.getX()+0.5-aPlayer.getX())*180/Math.PI)/360)%Textures.ItemIcons.COMPASS.length);
		if (tDistance <     4) return Textures.ItemIcons.COMPASS[(tIndex + (                            new Random(CLIENT_TIME   ).nextInt(3)-1)) % Textures.ItemIcons.COMPASS.length].getIcon(0);
		if (tDistance <    16) return Textures.ItemIcons.COMPASS[(tIndex + (CLIENT_TIME %  2 != 0 ? 0 : new Random(CLIENT_TIME/ 2).nextInt(3)-1)) % Textures.ItemIcons.COMPASS.length].getIcon(0);
		if (tDistance <   256) return Textures.ItemIcons.COMPASS[(tIndex + (CLIENT_TIME %  3 != 0 ? 0 : new Random(CLIENT_TIME/ 3).nextInt(3)-1)) % Textures.ItemIcons.COMPASS.length].getIcon(0);
		if (tDistance <  1024) return Textures.ItemIcons.COMPASS[(tIndex + (CLIENT_TIME %  4 != 0 ? 0 : new Random(CLIENT_TIME/ 4).nextInt(3)-1)) % Textures.ItemIcons.COMPASS.length].getIcon(0);
		if (tDistance <  4096) return Textures.ItemIcons.COMPASS[(tIndex + (CLIENT_TIME %  5 != 0 ? 0 : new Random(CLIENT_TIME/ 5).nextInt(3)-1)) % Textures.ItemIcons.COMPASS.length].getIcon(0);
		if (tDistance < 16384) return Textures.ItemIcons.COMPASS[(tIndex + (CLIENT_TIME % 10 != 0 ? 0 : new Random(CLIENT_TIME/10).nextInt(3)-1)) % Textures.ItemIcons.COMPASS.length].getIcon(0);
		if (tDistance < 65536) return Textures.ItemIcons.COMPASS[(tIndex + (CLIENT_TIME % 20 != 0 ? 0 : new Random(CLIENT_TIME/20).nextInt(3)-1)) % Textures.ItemIcons.COMPASS.length].getIcon(0);
		// Point straight to Target!
		return Textures.ItemIcons.COMPASS[tIndex % Textures.ItemIcons.COMPASS.length].getIcon(0);
	}
	
	@Override
	public ItemStack getContainerItemDefault(ItemStack aStack) {
		short aMeta = ST.meta_(aStack);
		if (aMeta >=  1000 && aMeta <=  1999) return make(  999);
		if (aMeta >   2000 && aMeta <=  2099) return make( 2000);
		if (aMeta >   2100 && aMeta <=  2199) return make( 2100);
		if (aMeta >   2200 && aMeta <=  2299) return make( 2200);
		if (aMeta >   2300 && aMeta <=  2399) return make( 2300);
		if (aMeta >   2400 && aMeta <=  2499) return make( 2400);
		if (aMeta >   2500 && aMeta <=  2599) return make( 2500);
		if (aMeta >   2600 && aMeta <=  2699) return make( 2600);
		if (aMeta >   2700 && aMeta <=  2799) return make( 2700);
		if (aMeta >   2800 && aMeta <=  2899) return make( 2800);
		if (aMeta >   2900 && aMeta <=  2999) return make( 2900);
		if (aMeta ==  5005 || aMeta ==  5008) return make(aMeta - 1);
		if (aMeta ==  5006 || aMeta ==  5009) return make(aMeta - 2);
		if (aMeta ==  5011) return make( 5013);
		if (aMeta ==  5012) return make( 5010);
		return super.getContainerItemDefault(aStack);
	}
	
	@Override
	public boolean canBeStoredInToolbox(ItemStack aStack) {
		short aMeta = ST.meta_(aStack);
		return aMeta >= 9000 || aMeta == 6000 || aMeta == 6001 || aMeta == 6002 || (aMeta >= 1000 && aMeta < 5999);
	}
	
	@Override
	public int getDefaultStackLimit(ItemStack aStack) {
		short aMeta = ST.meta_(aStack);
		switch (aMeta) {
		case 5002: case 5005: case 5008: case 5011: case 5014: case 5015: case 9001: case 9002: return 1;
		default: return (aMeta >= 2000 && aMeta < 3000) ? 1 : 64;
		}
	}
	
	@Override
	public ItemStack getRotten(ItemStack aStack) {
		switch(ST.meta_(aStack)) {
		case 2002: return ST.make(metaFamily(), aStack.getCount(), 2011, ItemNBT.get(aStack));
		case 2102: return ST.make(metaFamily(), aStack.getCount(), 2111, ItemNBT.get(aStack));
		case 2202: return ST.make(metaFamily(), aStack.getCount(), 2211, ItemNBT.get(aStack));
		case 2302: return ST.make(metaFamily(), aStack.getCount(), 2311, ItemNBT.get(aStack));
		case 2402: return ST.make(metaFamily(), aStack.getCount(), 2411, ItemNBT.get(aStack));
		case 2502: return ST.make(metaFamily(), aStack.getCount(), 2511, ItemNBT.get(aStack));
		case 2602: return ST.make(metaFamily(), aStack.getCount(), 2611, ItemNBT.get(aStack));
		case 2702: return ST.make(metaFamily(), aStack.getCount(), 2711, ItemNBT.get(aStack));
		case 2802: return ST.make(metaFamily(), aStack.getCount(), 2811, ItemNBT.get(aStack));
		case 2902: return ST.make(metaFamily(), aStack.getCount(), 2911, ItemNBT.get(aStack));
		}
		return aStack;
	}
	
	@Override public ItemStack getRotten(ItemStack aStack, Level aWorld, int aX, int aY, int aZ) {return getRotten(aStack);}
}

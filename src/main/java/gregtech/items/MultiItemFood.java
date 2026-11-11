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

import gregapi.code.ItemNBT;
import gregapi.data.*;
import gregapi.item.CreativeTab;
import gregapi.item.IItemRottable;
import gregapi.item.multiitem.MultiItemRandomWithCompat;
import gregapi.item.multiitem.behaviors.*;
import gregapi.item.multiitem.food.FoodStat;
import gregapi.oredict.OreDictItemData;
import gregapi.oredict.OreDictManager;
import gregapi.oredict.OreDictMaterial;
import gregapi.util.CR;
import gregapi.util.OM;
import gregapi.util.ST;
import gregapi.util.UT;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;

import static gregapi.data.CS.*;

/** Potion/MobEffect static instances are gone entirely in neo, so the numeric ids below are literal 1.7.10 values checked
 *  against the oracle decompile; UT.Entities.applyPotion maps each through VANILLA_POTION_IDS to a real effect. */
public class MultiItemFood extends MultiItemRandomWithCompat implements IItemRottable {
	private MultiItemFood(MultiItemFood aFamily, short aMeta) {super(aFamily, aMeta);}
	@Override protected gregapi.item.multiitem.MultiItemRandom createVariant(short aMeta) {return new MultiItemFood(this, aMeta);}
	public MultiItemFood(String aModID, String aUnlocalized) {
		super(aModID, aUnlocalized);
		// Its own creative tab, registered by CreativeTabsGT on RegisterEvent<CreativeModeTab>, 1:1 with the original.
		new gregapi.item.CreativeTab(getUnlocalizedName(), "GregTech: Nature & Foods", this, (short)12000);
	}
	
	protected void defineItems(gregapi.item.ItemTypeDefinitions aItems) {
		aItems.add(12000, aID -> {IL.Grass       .set(addItem(aID, "Grass"       , "Make 9 of this into a Bale in order to dry it", Behavior_FeedGrass.INSTANCE, OD.itemGrass         , TICKS_PER_SMELT / 4, TC.stack(TC.MESSIS, 1), TC.stack(TC.HERBA, 1)));});
		aItems.add(12001, aID -> {IL.Grass_Dry   .set(addItem(aID, "Dry Grass"   , "Useful for making a simple Fire Starter"      , Behavior_FeedGrass.INSTANCE, OD.itemGrassDry      , TICKS_PER_SMELT / 2, TC.stack(TC.MESSIS, 1)));});
		aItems.add(12002, aID -> {IL.Grass_Moldy .set(addItem(aID, "Moldy Grass" , ""                                                                          , OD.itemGrassMoldy    , TICKS_PER_SMELT / 4, TC.stack(TC.MESSIS, 1)));});
		aItems.add(12003, aID -> {IL.Grass_Rotten.set(addItem(aID, "Rotten Grass", ""                                                                          , OD.itemGrassRotten   , TICKS_PER_SMELT / 4, TC.stack(TC.MESSIS, 1)));});
		aItems.add(12004, aID -> {IL.Crop_Rye    .set(addItem(aID, "Rye"         , ""                                             , Behavior_FeedGrass.INSTANCE, "cropRye"            , TICKS_PER_SMELT / 4, TC.stack(TC.MESSIS, 1)));});
		aItems.add(12005, aID -> {IL.Crop_Oats   .set(addItem(aID, "Oats"        , ""                                             , Behavior_FeedGrass.INSTANCE, "cropOats"           , TICKS_PER_SMELT / 4, TC.stack(TC.MESSIS, 1)));});
		aItems.add(12006, aID -> {IL.Crop_Barley .set(addItem(aID, "Barley"      , ""                                             , Behavior_FeedGrass.INSTANCE, "cropBarley"         , TICKS_PER_SMELT / 4, TC.stack(TC.MESSIS, 1)));});
		aItems.add(12007, aID -> {
			IL.Crop_Rice   .set(addItem(aID, "Rice"        , ""                                             , Behavior_FeedGrass.INSTANCE, "cropRice"           , TICKS_PER_SMELT / 4, TC.stack(TC.MESSIS, 1)));
		RM.replicateOrganic( 1,  2, IL.Grass      .get(1));
		RM.replicateOrganic( 1,  3, IL.Crop_Wheat .get(1));
		RM.replicateOrganic( 1,  4, IL.Crop_Rye   .get(1));
		RM.replicateOrganic( 1,  5, IL.Crop_Oats  .get(1));
		RM.replicateOrganic( 1,  6, IL.Crop_Barley.get(1));
		RM.replicateOrganic( 1,  7, IL.Crop_Rice  .get(1));
		CR.shaped(IL.Bale           .get(1), CR.DEF_NCC, "XXX", "XXX", "XXX", 'X', OD.itemGrass);
		CR.shaped(IL.Bale_Dry       .get(1), CR.DEF_NCC, "XXX", "XXX", "XXX", 'X', OD.itemGrassDry);
		CR.shaped(IL.Bale_Moldy     .get(1), CR.DEF_NCC, "XXX", "XXX", "XXX", 'X', OD.itemGrassMoldy);
		CR.shaped(IL.Bale_Rotten    .get(1), CR.DEF_NCC, "XXX", "XXX", "XXX", 'X', OD.itemGrassRotten);
		CR.shaped(IL.Bale_Rye       .get(1), CR.DEF_NCC, "XXX", "XXX", "XXX", 'X', "cropRye");
		CR.shaped(IL.Bale_Oats      .get(1), CR.DEF_NCC, "XXX", "XXX", "XXX", 'X', "cropOats");
		CR.shaped(IL.Bale_Barley    .get(1), CR.DEF_NCC, "XXX", "XXX", "XXX", 'X', "cropBarley");
		CR.shaped(IL.Bale_Rice      .get(1), CR.DEF_NCC, "XXX", "XXX", "XXX", 'X', "cropRice");
		CR.shapeless(IL.Grass       .get(1), CR.DEF_NCC, new Object[] {OD.itemGrassTall});
		CR.shapeless(IL.Grass       .get(9), CR.DEF_NCC, new Object[] {OD.baleGrass});
		CR.shapeless(IL.Grass_Dry   .get(9), CR.DEF_NCC, new Object[] {OD.baleGrassDry});
		CR.shapeless(IL.Grass_Moldy .get(9), CR.DEF_NCC, new Object[] {OD.baleGrassMoldy});
		CR.shapeless(IL.Grass_Rotten.get(9), CR.DEF_NCC, new Object[] {OD.baleGrassRotten});
		CR.shapeless(IL.Crop_Rye    .get(9), CR.DEF_NCC, new Object[] {"baleRye"});
		CR.shapeless(IL.Crop_Oats   .get(9), CR.DEF_NCC, new Object[] {"baleOats"});
		CR.shapeless(IL.Crop_Barley .get(9), CR.DEF_NCC, new Object[] {"baleBarley"});
		CR.shapeless(IL.Crop_Rice   .get(9), CR.DEF_NCC, new Object[] {"baleRice"});
		ItemsGT.addNEIRedirects(IL.Bale       .get(1), IL.Grass       .get(1));
		ItemsGT.addNEIRedirects(IL.Bale_Dry   .get(1), IL.Grass_Dry   .get(1));
		ItemsGT.addNEIRedirects(IL.Bale_Moldy .get(1), IL.Grass_Moldy .get(1));
		ItemsGT.addNEIRedirects(IL.Bale_Rotten.get(1), IL.Grass_Rotten.get(1));
		ItemsGT.addNEIRedirects(IL.Bale_Rye   .get(1), IL.Crop_Rye    .get(1));
		ItemsGT.addNEIRedirects(IL.Bale_Oats  .get(1), IL.Crop_Oats   .get(1));
		ItemsGT.addNEIRedirects(IL.Bale_Barley.get(1), IL.Crop_Barley .get(1));
		ItemsGT.addNEIRedirects(IL.Bale_Rice  .get(1), IL.Crop_Rice   .get(1));
		RM.DidYouKnow.addFakeRecipe(F, ST.array(ST.make(ToolsGT.sMetaTool, 1, ToolsGT.KNIFE, "Just cut Tall Grass with this"), ST.make(ToolsGT.sMetaTool, 1, ToolsGT.SENSE, "Or cut Tall Grass with this"), ST.make(Blocks.SHORT_GRASS, 1, 0)), ST.array(IL.Grass.get(1), IL.Bale.get(1)), null, ZL_LONG, ZL_FS, ZL_FS, 0, 0, 0);
		RM.DidYouKnow.addFakeRecipe(F, ST.array(IL.Bale.getWithName(1, "Put the Bale Outside")), ST.array(IL.Bale_Dry.getWithName(1, "When its dry Outside"), IL.Bale_Moldy.getWithName(1, "When its wet Outside"), IL.Bale_Rotten.getWithName(1, "When its wet Outside and more time passed"), IL.Grass_Dry.getWithName(1, "When its dry Outside"), IL.Grass_Moldy.getWithName(1, "When its wet Outside"), IL.Grass_Rotten.getWithName(1, "When its wet Outside and more time passed")), null, ZL_LONG, ZL_FS, ZL_FS, 0, 0, 0);
		});
		
		
		
		
		aItems.add(12010, aID -> {IL.Cerublossom     .set(addItem(aID, "Cerublossom"      , "Used for magical Purposes"              , new Behavior_Turn_Into(IL.ARS_Cerublossom ), IL.ARS_Cerublossom.exists() ? TD.Creative.HIDDEN : "flowerCerublossom", TC.stack(TC.HERBA, 1), TC.stack(TC.PRAECANTIO, 1), TC.stack(TC.LUX, 1)));});
		aItems.add(12011, aID -> {
			IL.DesertNova      .set(addItem(aID, "Desert Nova"      , "Used for magical Purposes"              , new Behavior_Turn_Into(IL.ARS_DesertNova  ), IL.ARS_DesertNova .exists() ? TD.Creative.HIDDEN : "flowerDesertNova" , TC.stack(TC.HERBA, 1), TC.stack(TC.PRAECANTIO, 1), TC.stack(TC.LUX, 1)));
		RM.replicateOrganic( 2,  3, IL.ARS_Cerublossom.exists() ? IL.ARS_Cerublossom.get(1) : IL.Cerublossom.get(1));
		RM.replicateOrganic( 2,  4, IL.ARS_DesertNova .exists() ? IL.ARS_DesertNova .get(1) : IL.DesertNova .get(1));
		});
		
		
		aItems.add(12050, aID -> {
			IL.Resin           .set(addItem(aID, "Rubber Resin"     , ""                                       , new Behavior_Turn_Into(IL.IC2_Resin       ), IL.IC2_Resin      .exists() ? TD.Creative.HIDDEN : OD.itemResin       , TC.stack(TC.LIMUS, 1), TICKS_PER_SMELT / 2));
		RM.replicateOrganic( 2,  5, IL.IC2_Resin.exists() ? IL.IC2_Resin.get(1) : IL.Resin.get(1));
		});
		
		
		aItems.add(12098, aID -> {IL.Sticky_Goo      .set(addItem(aID, "Sticky Goo"       , "Not Vegan at all"                       , OD.slimeball, OD.slimeballAnimal, TC.stack(TC.LIMUS, 2)));});
		aItems.add(12099, aID -> {
			IL.Slimeball_Borax .set(addItem(aID, "Slimeball"        , "Borax mixed with Glue"                  , OD.slimeball, OD.slimeballBorax , TC.stack(TC.LIMUS, 2)));
		RM.Mixer.addRecipe1(T, 16, 16, OM.dust(MT.OREMATS.Borax), FL.Glue.make(250), NF, IL.Slimeball_Borax.get(1));
		});
		
		
		aItems.add(12100, aID -> {IL.Remains_Plant   .set(addItem(aID, "Plant Remains"    , ""                                       , OD.itemPlantRemains, TICKS_PER_SMELT / 4, TC.stack(TC.HERBA, 1)));});
		aItems.add(12101, aID -> {IL.Remains_Fruit   .set(addItem(aID, "Fruit Remains"    , ""                                       , OD.itemPlantRemains, TICKS_PER_SMELT / 4, TC.stack(TC.HERBA, 1)));});
		aItems.add(12102, aID -> {IL.Remains_Veggie  .set(addItem(aID, "Vegetable Remains", ""                                       , OD.itemPlantRemains, TICKS_PER_SMELT / 4, TC.stack(TC.HERBA, 1)));});
		aItems.add(12103, aID -> {IL.Remains_Nut     .set(addItem(aID, "Nut Remains"      , ""                                       , OD.itemPlantRemains, TICKS_PER_SMELT / 2, TC.stack(TC.HERBA, 1)));});
		
		
		aItems.add(12201, aID -> {
			IL.Bark_Dry        .set(addItem(aID, "Dry Bark"         , "Useful for making a simple Fire Starter", OD.itemBarkDry, TICKS_PER_SMELT / 4, TC.stack(TC.ARBOR, 1), new OreDictItemData(MT.Bark, U2)));
		RM.replicateOrganic( 2,  6, IL.Bark_Dry.get(1));
		});
		
		
		aItems.add(12300, aID -> {IL.Mud_Ball        .set(addItem(aID, "Mud"              , ""                                       , OD.itemMud , TC.stack(TC.TERRA, 1)                                         )); if (COMPAT_FR != null) COMPAT_FR.addToBackpacks("digger", last());});
		aItems.add(12310, aID -> {IL.Clay_Ball_Brown .set(addItem(aID, "Brown Clay"       , "Contains small amounts of Lithium"      , OD.itemClay, TC.stack(TC.TERRA, 1), new OreDictItemData(MT.ClayBrown   , U))); if (COMPAT_FR != null) COMPAT_FR.addToBackpacks("digger", last());});
		aItems.add(12311, aID -> {IL.Clay_Ball_Red   .set(addItem(aID, "Red Clay"         , "Perfectly Balanced With No Exploits!"   , OD.itemClay, TC.stack(TC.TERRA, 1), new OreDictItemData(MT.ClayRed     , U))); if (COMPAT_FR != null) COMPAT_FR.addToBackpacks("digger", last());});
		aItems.add(12312, aID -> {IL.Clay_Ball_Yellow.set(addItem(aID, "Yellow Clay"      , "Bentonite Clay"                         , OD.itemClay, TC.stack(TC.TERRA, 1), new OreDictItemData(MT.Bentonite   , U))); if (COMPAT_FR != null) COMPAT_FR.addToBackpacks("digger", last());});
		aItems.add(12313, aID -> {IL.Clay_Ball_Blue  .set(addItem(aID, "Blue Clay"        , "Palygorskite Clay / Fullers Earth"      , OD.itemClay, TC.stack(TC.TERRA, 1), new OreDictItemData(MT.Palygorskite, U))); if (COMPAT_FR != null) COMPAT_FR.addToBackpacks("digger", last());});
		aItems.add(12314, aID -> {
			IL.Clay_Ball_White .set(addItem(aID, "White Clay"       , "Kaolinite Clay / Porcelain Clay"        , OD.itemClay, TC.stack(TC.TERRA, 1), new OreDictItemData(MT.Kaolinite   , U))); if (COMPAT_FR != null) COMPAT_FR.addToBackpacks("digger", last());
		CR.remove(ST.make(Items.CLAY_BALL, 1, 0), ST.make(Items.CLAY_BALL, 1, 0), NI, ST.make(Items.CLAY_BALL, 1, 0), ST.make(Items.CLAY_BALL, 1, 0));
		RM.generify(IL.Clay_Ball_Brown .get(1), ST.make(Items.CLAY_BALL, 1, 0));
		RM.generify(IL.Clay_Ball_Red   .get(1), ST.make(Items.CLAY_BALL, 1, 0));
		RM.generify(IL.Clay_Ball_Yellow.get(1), ST.make(Items.CLAY_BALL, 1, 0));
		RM.generify(IL.Clay_Ball_Blue  .get(1), ST.make(Items.CLAY_BALL, 1, 0));
		RM.generify(IL.Clay_Ball_White .get(1), ST.make(Items.CLAY_BALL, 1, 0));
		RM.add_smelting(IL.Clay_Ball_Brown .get(1), ST.make(Items.BRICK, 1, 0), F, F, T);
		RM.add_smelting(IL.Clay_Ball_Red   .get(1), ST.make(Items.BRICK, 1, 0), F, F, T);
		RM.add_smelting(IL.Clay_Ball_Yellow.get(1), ST.make(Items.BRICK, 1, 0), F, F, T);
		RM.add_smelting(IL.Clay_Ball_Blue  .get(1), ST.make(Items.BRICK, 1, 0), F, F, T);
		RM.add_smelting(IL.Clay_Ball_White .get(1), ST.make(Items.BRICK, 1, 0), F, F, T);
		CR.shaped    (OP.plate.mat(MT.Clay         , 1), CR.DEF_NCC, "R", "C", 'R', OreDictToolNames.rollingpin, 'C', Items.CLAY_BALL);
		CR.shaped    (OP.plate.mat(MT.ClayBrown    , 1), CR.DEF_NCC, "R", "C", 'R', OreDictToolNames.rollingpin, 'C', IL.Clay_Ball_Brown);
		CR.shaped    (OP.plate.mat(MT.ClayRed      , 1), CR.DEF_NCC, "R", "C", 'R', OreDictToolNames.rollingpin, 'C', IL.Clay_Ball_Red);
		CR.shaped    (OP.plate.mat(MT.Bentonite    , 1), CR.DEF_NCC, "R", "C", 'R', OreDictToolNames.rollingpin, 'C', IL.Clay_Ball_Yellow);
		CR.shaped    (OP.plate.mat(MT.Palygorskite , 1), CR.DEF_NCC, "R", "C", 'R', OreDictToolNames.rollingpin, 'C', IL.Clay_Ball_Blue);
		CR.shaped    (OP.plate.mat(MT.Kaolinite    , 1), CR.DEF_NCC, "R", "C", 'R', OreDictToolNames.rollingpin, 'C', IL.Clay_Ball_White);
		CR.shaped    (ST.make(Blocks.MUD, 1, 0), CR.DEF_NCC, "XX", "XX", 'X', IL.Mud_Ball);
		CR.shaped    (ST.make(BlocksGT.Diggables, 1, 1), CR.DEF_NCC, "XX", "XX", 'X', IL.Clay_Ball_Brown);
		CR.shaped    (ST.make(BlocksGT.Diggables, 1, 3), CR.DEF_NCC, "XX", "XX", 'X', IL.Clay_Ball_Red);
		CR.shaped    (ST.make(BlocksGT.Diggables, 1, 4), CR.DEF_NCC, "XX", "XX", 'X', IL.Clay_Ball_Yellow);
		CR.shaped    (ST.make(BlocksGT.Diggables, 1, 5), CR.DEF_NCC, "XX", "XX", 'X', IL.Clay_Ball_Blue);
		CR.shaped    (ST.make(BlocksGT.Diggables, 1, 6), CR.DEF_NCC, "XX", "XX", 'X', IL.Clay_Ball_White);
		CR.shaped    (ST.make(Blocks.CLAY       , 1, 0), CR.DEF_NCC, "XX", "XX", 'X', Items.CLAY_BALL);
		CR.shapeless (IL.Mud_Ball               .get(4), CR.DEF_NCC, new Object[] {ST.make(Blocks.MUD, 1, 0)});
		CR.shapeless (IL.Clay_Ball_Brown        .get(4), CR.DEF_NCC, new Object[] {ST.make(BlocksGT.Diggables, 1, 1)});
		CR.shapeless (IL.Clay_Ball_Red          .get(4), CR.DEF_NCC, new Object[] {ST.make(BlocksGT.Diggables, 1, 3)});
		CR.shapeless (IL.Clay_Ball_Yellow       .get(4), CR.DEF_NCC, new Object[] {ST.make(BlocksGT.Diggables, 1, 4)});
		CR.shapeless (IL.Clay_Ball_Blue         .get(4), CR.DEF_NCC, new Object[] {ST.make(BlocksGT.Diggables, 1, 5)});
		CR.shapeless (IL.Clay_Ball_White        .get(4), CR.DEF_NCC, new Object[] {ST.make(BlocksGT.Diggables, 1, 6)});
		CR.shapeless (ST.make(Items.CLAY_BALL   , 4, 0), CR.DEF_NCC, new Object[] {Blocks.CLAY});
		RM.compactunpack(IL.Mud_Ball            .get(4), ST.make(Blocks.MUD, 1, 0));
		RM.compactunpack(IL.Clay_Ball_Brown     .get(4), ST.make(BlocksGT.Diggables, 1, 1));
		RM.compactunpack(IL.Clay_Ball_Red       .get(4), ST.make(BlocksGT.Diggables, 1, 3));
		RM.compactunpack(IL.Clay_Ball_Yellow    .get(4), ST.make(BlocksGT.Diggables, 1, 4));
		RM.compactunpack(IL.Clay_Ball_Blue      .get(4), ST.make(BlocksGT.Diggables, 1, 5));
		RM.compactunpack(IL.Clay_Ball_White     .get(4), ST.make(BlocksGT.Diggables, 1, 6));
		RM.compactunpack(ST.make(Items.CLAY_BALL, 4, 0), ST.make(Blocks.CLAY       , 1, 0));
		RM.RollingMill.addRecipe1(T, 16, 32, ST.make(Items.CLAY_BALL, 1, 0), OP.plate.mat(MT.Clay        , 1));
		RM.RollingMill.addRecipe1(T, 16, 32, IL.Clay_Ball_Brown     .get(1), OP.plate.mat(MT.ClayBrown   , 1));
		RM.RollingMill.addRecipe1(T, 16, 32, IL.Clay_Ball_Red       .get(1), OP.plate.mat(MT.ClayRed     , 1));
		RM.RollingMill.addRecipe1(T, 16, 32, IL.Clay_Ball_Yellow    .get(1), OP.plate.mat(MT.Bentonite   , 1));
		RM.RollingMill.addRecipe1(T, 16, 32, IL.Clay_Ball_Blue      .get(1), OP.plate.mat(MT.Palygorskite, 1));
		RM.RollingMill.addRecipe1(T, 16, 32, IL.Clay_Ball_White     .get(1), OP.plate.mat(MT.Kaolinite   , 1));
		RM.Compressor .addRecipe1(T, 16, 16, OM.dust(MT.Clay        ), ST.make(Items.CLAY_BALL, 1, 0));
		RM.Compressor .addRecipe1(T, 16, 16, OM.dust(MT.ClayBrown   ), IL.Clay_Ball_Brown     .get(1));
		RM.Compressor .addRecipe1(T, 16, 16, OM.dust(MT.ClayRed     ), IL.Clay_Ball_Red       .get(1));
		RM.Compressor .addRecipe1(T, 16, 16, OM.dust(MT.Bentonite   ), IL.Clay_Ball_Yellow    .get(1));
		RM.Compressor .addRecipe1(T, 16, 16, OM.dust(MT.Palygorskite), IL.Clay_Ball_Blue      .get(1));
		RM.Compressor .addRecipe1(T, 16, 16, OM.dust(MT.Kaolinite   ), IL.Clay_Ball_White     .get(1));
		RM.mortarize(1, ST.make(Items.CLAY_BALL   , 1, W), OM.dust(MT.Clay));
		RM.mortarize(1, IL.Clay_Ball_Brown        .get(1), OM.dust(MT.ClayBrown));
		RM.mortarize(1, IL.Clay_Ball_Red          .get(1), OM.dust(MT.ClayRed));
		RM.mortarize(1, IL.Clay_Ball_Yellow       .get(1), OM.dust(MT.Bentonite));
		RM.mortarize(1, IL.Clay_Ball_Blue         .get(1), OM.dust(MT.Palygorskite));
		RM.mortarize(1, IL.Clay_Ball_White        .get(1), OM.dust(MT.Kaolinite));
		RM.mortarize(4, ST.make(Blocks.CLAY       , 1, W), OM.dust(MT.Clay, U*4));
		RM.mortarize(4, ST.make(BlocksGT.Diggables, 1, 1), OM.dust(MT.ClayBrown, U*4));
		RM.mortarize(4, ST.make(BlocksGT.Diggables, 1, 3), OM.dust(MT.ClayRed, U*4));
		RM.mortarize(4, ST.make(BlocksGT.Diggables, 1, 4), OM.dust(MT.Bentonite, U*4));
		RM.mortarize(4, ST.make(BlocksGT.Diggables, 1, 5), OM.dust(MT.Palygorskite, U*4));
		RM.mortarize(4, ST.make(BlocksGT.Diggables, 1, 6), OM.dust(MT.Kaolinite, U*4));
		for (FluidStack tWater : FL.waters(125, 100)) {
		RM.Bath       .addRecipe1(T,  0, 16, OM.dust(MT.Ceramic      ), FL.mul(tWater, 5), NF, ST.make(Items.CLAY_BALL, 1, 0));
		RM.Bath       .addRecipe1(T,  0, 16, OM.dust(MT.Clay         ),        tWater    , NF, ST.make(Items.CLAY_BALL, 1, 0));
		RM.Bath       .addRecipe1(T,  0, 16, OM.dust(MT.ClayRed      ),        tWater    , NF, IL.Clay_Ball_Red.get(1));
		RM.Bath       .addRecipe1(T,  0, 16, OM.dust(MT.ClayBrown    ),        tWater    , NF, IL.Clay_Ball_Brown.get(1));
		RM.Bath       .addRecipe1(T,  0, 16, OM.dust(MT.Bentonite    ),        tWater    , NF, IL.Clay_Ball_Yellow.get(1));
		RM.Bath       .addRecipe1(T,  0, 16, OM.dust(MT.Palygorskite ),        tWater    , NF, IL.Clay_Ball_Blue.get(1));
		RM.Bath       .addRecipe1(T,  0, 16, OM.dust(MT.Kaolinite    ),        tWater    , NF, IL.Clay_Ball_White.get(1));
		}
		});
		
		aItems.add(12400, aID -> {IL.Tusk         .set(addItem(aID, "Tusk"         , ""                 , OD.itemTusk              , TC.stack(TC.BESTIA, 2), TC.stack(TC.FAMES, 2))); if (COMPAT_FR != null) COMPAT_FR.addToBackpacks("hunter", last());});
		aItems.add(12401, aID -> {IL.Tusk_Hoglin  .set(addItem(aID, "Hoglin Tusk"  , "not golden"       , OD.itemTusk              , TC.stack(TC.BESTIA, 2), TC.stack(TC.FAMES, 2))); if (COMPAT_FR != null) COMPAT_FR.addToBackpacks("hunter", last());});
		aItems.add(12402, aID -> {IL.Tusk_Boar    .set(addItem(aID, "Boar Tusk"    , ""                 , OD.itemTusk              , TC.stack(TC.BESTIA, 2), TC.stack(TC.FAMES, 2))); if (COMPAT_FR != null) COMPAT_FR.addToBackpacks("hunter", last());});
		aItems.add(12403, aID -> {IL.Tusk_Elephant.set(addItem(aID, "Elephant Tusk", "Ivory"            , OD.itemTusk, OD.itemIvory, TC.stack(TC.BESTIA, 3), TC.stack(TC.FAMES, 3), TC.stack(TC.LUCRUM, 3))); if (COMPAT_FR != null) COMPAT_FR.addToBackpacks("hunter", last());});
		
		aItems.add(12500, aID -> {IL.Hoof         .set(addItem(aID, "Hoof"         , ""                 , OD.itemHoof              , TC.stack(TC.BESTIA, 2), TC.stack(TC.ITER, 2))); if (COMPAT_FR != null) COMPAT_FR.addToBackpacks("hunter", last());});
		aItems.add(12501, aID -> {IL.Hoof_Cow     .set(addItem(aID, "Cow Hoof"     , ""                 , OD.itemHoof              , TC.stack(TC.BESTIA, 2), TC.stack(TC.ITER, 2))); if (COMPAT_FR != null) COMPAT_FR.addToBackpacks("hunter", last());});
		aItems.add(12502, aID -> {IL.Hoof_Horse   .set(addItem(aID, "Horse Hoof"   , "not race-worthy"  , OD.itemHoof              , TC.stack(TC.BESTIA, 2), TC.stack(TC.ITER, 2))); if (COMPAT_FR != null) COMPAT_FR.addToBackpacks("hunter", last());});
		aItems.add(12503, aID -> {IL.Hoof_Mule    .set(addItem(aID, "Mule Hoof"    , ""                 , OD.itemHoof              , TC.stack(TC.BESTIA, 2), TC.stack(TC.ITER, 2))); if (COMPAT_FR != null) COMPAT_FR.addToBackpacks("hunter", last());});
		aItems.add(12504, aID -> {IL.Hoof_Donkey  .set(addItem(aID, "Donkey Hoof"  , ""                 , OD.itemHoof              , TC.stack(TC.BESTIA, 2), TC.stack(TC.ITER, 2))); if (COMPAT_FR != null) COMPAT_FR.addToBackpacks("hunter", last());});
		aItems.add(12505, aID -> {IL.Hoof_Deer    .set(addItem(aID, "Deer Hoof"    , ""                 , OD.itemHoof              , TC.stack(TC.BESTIA, 2), TC.stack(TC.ITER, 2))); if (COMPAT_FR != null) COMPAT_FR.addToBackpacks("hunter", last());});
		
		aItems.add(12600, aID -> {IL.Horn         .set(addItem(aID, "Horn"         , ""                 , OD.itemHorn              , TC.stack(TC.BESTIA, 2), TC.stack(TC.TELUM, 2))); if (COMPAT_FR != null) COMPAT_FR.addToBackpacks("hunter", last());});
		aItems.add(12601, aID -> {IL.Horn_Cow     .set(addItem(aID, "Cow Horn"     , ""                 , OD.itemHorn              , TC.stack(TC.BESTIA, 2), TC.stack(TC.TELUM, 2))); if (COMPAT_FR != null) COMPAT_FR.addToBackpacks("hunter", last());});
		aItems.add(12602, aID -> {IL.Horn_Sheep   .set(addItem(aID, "Sheep Horn"   , "from horned Sheep", OD.itemHorn              , TC.stack(TC.BESTIA, 2), TC.stack(TC.TELUM, 2))); if (COMPAT_FR != null) COMPAT_FR.addToBackpacks("hunter", last());});
		
		aItems.add(12700, aID -> {IL.Antler       .set(addItem(aID, "Antler"       , ""                 , OD.itemAntler            , TC.stack(TC.BESTIA, 2), TC.stack(TC.INSTRUMENTUM, 2))); if (COMPAT_FR != null) COMPAT_FR.addToBackpacks("hunter", last());});
		aItems.add(12701, aID -> {IL.Antler_Deer  .set(addItem(aID, "Deer Antler"  , "oh dear..."       , OD.itemAntler            , TC.stack(TC.BESTIA, 2), TC.stack(TC.INSTRUMENTUM, 2))); if (COMPAT_FR != null) COMPAT_FR.addToBackpacks("hunter", last());});
		
		
		
		
		
		
		
		aItems.add(30000, aID -> {IL.Comb_Honey   .set(addItem(aID, "Honey Comb"   , "", OD.beeComb, OD.materialHoneycomb, "foodFilledhoneycomb", TC.stack(TC.LIMUS, 1), TC.stack(TC.FAMES, 1), TC.stack(TC.SANO, 1), new FoodStat( 1, 0.1F, 20, C+37, 0.50F,  0,  0,  0, 20,  0, ItemUseAnimation.EAT, null, F, T, F, T))); Sandwiches.INGREDIENTS.put(last(), (byte)22);});
		aItems.add(30001, aID -> {IL.Comb_Water   .set(addItem(aID, "Water Comb"   , "", OD.beeComb, TC.stack(TC.AQUA, 2), new FoodStat( 1, 0.1F, 20, C+37, 0.50F,  0,  0,  0, 20,  0, ItemUseAnimation.EAT, null, F, T, F, T))); Sandwiches.INGREDIENTS.put(last(), (byte)22);});
		aItems.add(30002, aID -> {IL.Comb_Magic   .set(addItem(aID, "Magic Comb"   , "", OD.beeComb, TC.stack(TC.PRAECANTIO, 2)));});
		aItems.add(30003, aID -> {IL.Comb_Nether  .set(addItem(aID, "Nether Comb"  , "", OD.beeComb, TC.stack(TC.IGNIS, 2)));});
		aItems.add(30004, aID -> {IL.Comb_End     .set(addItem(aID, "End Comb"     , "", OD.beeComb, TC.stack(TC.ALIENIS, 2)));});
		aItems.add(30005, aID -> {IL.Comb_Rock    .set(addItem(aID, "Rock Comb"    , "", OD.beeComb, TC.stack(TC.TERRA, 2)));});
		aItems.add(30006, aID -> {IL.Comb_Jungle  .set(addItem(aID, "Jungle Comb"  , "", OD.beeComb, TC.stack(TC.HERBA, 2), new FoodStat( 1, 0.1F, 20, C+37, 0.50F,  0,  0,  0, 20,  0, ItemUseAnimation.EAT, null, F, T, F, T))); Sandwiches.INGREDIENTS.put(last(), (byte)22);});
		aItems.add(30007, aID -> {IL.Comb_Frozen  .set(addItem(aID, "Frozen Comb"  , "", OD.beeComb, TC.stack(TC.GELUM, 2)));});
		aItems.add(30008, aID -> {IL.Comb_Shroom  .set(addItem(aID, "Shroomy Comb" , "", OD.beeComb, TC.stack(TC.VITIUM, 1), TC.stack(TC.HERBA, 1), new FoodStat( 1, 0.1F, 20, C+37, 0.50F,  0,  0,  0, 20,  0, ItemUseAnimation.EAT, null, F, T, F, T))); Sandwiches.INGREDIENTS.put(last(), (byte)22);});
		aItems.add(30009, aID -> {IL.Comb_Sandy   .set(addItem(aID, "Sandy Comb"   , "", OD.beeComb, TC.stack(TC.TERRA, 1), TC.stack(TC.IGNIS, 1)));});
		
		aItems.add(30100, aID -> {IL.Comb_Clay    .set(addItem(aID, "Clay Comb"    , "", OD.beeComb, OD.beeCombCrossbred, TC.stack(TC.TERRA, 1), TC.stack(TC.AQUA, 1)));});
		aItems.add(30101, aID -> {IL.Comb_Sticky  .set(addItem(aID, "Sticky Comb"  , "", OD.beeComb, OD.beeCombCrossbred, TC.stack(TC.LIMUS, 2)));});
		aItems.add(30102, aID -> {IL.Comb_Royal   .set(addItem(aID, "Royal Comb"   , "", OD.beeComb, OD.beeCombCrossbred, TC.stack(TC.LIMUS, 1), TC.stack(TC.SANO, 1), new FoodStat( 1, 0.1F, 20, C+37, 0.50F,  0,  0,  0, 20,  0, ItemUseAnimation.EAT, null, F, T, F, T))); Sandwiches.INGREDIENTS.put(last(), (byte)22);});
		aItems.add(30103, aID -> {IL.Comb_Soul    .set(addItem(aID, "Soul Comb"    , "", OD.beeComb, OD.beeCombCrossbred, TC.stack(TC.SPIRITUS, 2)));});
		aItems.add(30104, aID -> {IL.Comb_Amnesic .set(addItem(aID, "Amnesic Comb" , "", OD.beeComb, OD.beeCombCrossbred, TC.stack(TC.LIMUS, 1), TC.stack(TC.STRONTIO, 1)));});
		aItems.add(30105, aID -> {IL.Comb_Military.set(addItem(aID, "Military Comb", "", OD.beeComb, OD.beeCombCrossbred, TC.stack(TC.TELUM, 2)));});
		
		aItems.add(30200, aID -> {IL.Comb_Pyro    .set(addItem(aID, "Pyro Comb"    , "", OD.beeComb, OD.beeCombCrossbred, TC.stack(TC.IGNIS, 2)));});
		aItems.add(30201, aID -> {IL.Comb_Cryo    .set(addItem(aID, "Cryo Comb"    , "", OD.beeComb, OD.beeCombCrossbred, TC.stack(TC.GELUM, 2)));});
		aItems.add(30202, aID -> {IL.Comb_Aero    .set(addItem(aID, "Aero Comb"    , "", OD.beeComb, OD.beeCombCrossbred, TC.stack(TC.AER  , 2)));});
		aItems.add(30203, aID -> {
			IL.Comb_Tera    .set(addItem(aID, "Tera Comb"    , "", OD.beeComb, OD.beeCombCrossbred, TC.stack(TC.TERRA, 2)));
		
		
		
		RM.Centrifuge.addRecipe1(T, 16, 64, new long[] {10000,  1000}                  , IL.Comb_Honey   .get(1), NF   , FL.Honey                                .make( 100), OM.dust(MT.WaxBee)                , IL.FR_Propolis.get(1));
		RM.Centrifuge.addRecipe1(T, 16, 64, new long[] {10000,  1000}                  , IL.Comb_Water   .get(1), NF   , FL.Water                                .make(1000), OM.dust(MT.WaxBee)                );
		RM.Centrifuge.addRecipe1(T, 16, 64, new long[] {10000,  1000}                  , IL.Comb_Magic   .get(1), NF   , FL.Ambrosia                             .make( 100), OM.dust(MT.WaxMagic)              );
		RM.Centrifuge.addRecipe1(T, 16, 64, new long[] {10000,  1000}                  , IL.Comb_Nether  .get(1), NF   , FL.Blaze                                .make(   L), OM.dust(MT.WaxRefractory)         );
		RM.Centrifuge.addRecipe1(T, 16, 64, new long[] {10000,  1000,  1000}           , IL.Comb_End     .get(1), NF   , FL.Dragon_Breath                        .make( 125), OM.dust(MT.Endstone)              , IL.FR_Propolis_Pulsating.get(1), IL.EtFu_Chorus_Fruit.get(1));
		RM.Centrifuge.addRecipe1(T, 16, 64, new long[] {10000,  1000}                  , IL.Comb_Rock    .get(1), NF   , FL.Concrete                             .make(   L), OM.dust(MT.Stone)                 );
		RM.Centrifuge.addRecipe1(T, 16, 64, new long[] {10000,  1000}                  , IL.Comb_Jungle  .get(1), NF   , MT.Chocolate                          .liquid(U, T), OM.dust(MT.Cocoa)                 , IL.FR_Propolis_Silky.get(1, ST.make(Items.STRING, 1, 0)));
		RM.Centrifuge.addRecipe1(T, 16, 64, new long[] {10000,  1000}                  , IL.Comb_Frozen  .get(1), NF   , FL.Ice                                  .make(1000), OM.dust(MT.Ice)                   );
		RM.Centrifuge.addRecipe1(T, 16, 64, new long[] { 6000,  6000}                  , IL.Comb_Shroom  .get(1), NF   , FL.Soup_Mushroom                        .make(1000), ST.make(Blocks.RED_MUSHROOM_BLOCK, 1, 0), ST.make(Blocks.BROWN_MUSHROOM_BLOCK, 1, 0));
		RM.Centrifuge.addRecipe1(T, 16, 64, new long[] {10000,  1000}                  , IL.Comb_Sandy   .get(1), NF   , FL.Juice_Cactus                         .make( 100), ST.make(Blocks.SAND, 1, 0)        );
		RM.Centrifuge.addRecipe1(T, 16, 64, new long[] { 2000,2000,2000,2000,2000,2000}, IL.Comb_Clay    .get(1), NF   , FL.Concrete                             .make(   L), OM.dust(MT.Clay)                  , OM.dust(MT.ClayBrown), OM.dust(MT.ClayRed), OM.dust(MT.Bentonite), OM.dust(MT.Palygorskite), OM.dust(MT.Kaolinite));
		RM.Centrifuge.addRecipe1(T, 16, 64, new long[] {10000,  3000}                  , IL.Comb_Sticky  .get(1), NF   , FL.Latex                                .make(   L), OM.dust(MT.WaxBee)                , IL.FR_Propolis_Sticky.get(1, IL.IC2_Resin.get(1, IL.Resin.get(1))));
		RM.Centrifuge.addRecipe1(T, 16, 64, new long[] {10000}                         , IL.Comb_Royal   .get(1), ZL_FS, FL.array(FL.Honey.make(50), FL.RoyalJelly.make(10)), OM.dust(MT.WaxBee)                );
		RM.Centrifuge.addRecipe1(T, 16, 64, new long[] {10000,  9000}                  , IL.Comb_Soul    .get(1), NF   , FL.Oil_Soulsand                         .make(  50), OM.dust(MT.WaxSoulful)            , OM.dust(MT.SoulSand));
		RM.Centrifuge.addRecipe1(T, 16, 64, new long[] {10000,  1000}                  , IL.Comb_Amnesic .get(1), NF   , FL                                      .lube(1000), OM.dust(MT.WaxAmnesic)            );
		RM.Centrifuge.addRecipe1(T, 16, 64, new long[] {10000,   500,   500,   250}    , IL.Comb_Military.get(1), NF   , FL.Potion_Harm_1                        .make(  50), OM.dust(MT.Bone)                  , ST.make(Items.BONE, 1, 0), ST.make(Items.ROTTEN_FLESH, 1, 0), ST.make(Items.SPIDER_EYE, 1, 0));
		RM.Centrifuge.addRecipe1(T, 16, 64, new long[] {10000,  1000}                  , IL.Comb_Pyro    .get(1), NF   , FL.Blaze                                .make( L/2), OM.dust(MT.Blaze, U9)             , OP.stick.mat(MT.Blaze, 1));
		RM.Centrifuge.addRecipe1(T, 16, 64, new long[] {10000,  1000}                  , IL.Comb_Cryo    .get(1), NF   , FL.Ice                                  .make( 500), OM.dust(MT.Blizz, U9)             , OP.stick.mat(MT.Blizz, 1));
		RM.Centrifuge.addRecipe1(T, 16, 64, new long[] {10000,   500,   500}           , IL.Comb_Aero    .get(1), NF   , FL.Dragon_Breath                        .make(  50), OM.dust(MT.Blitz, U9)             , OP.stick.mat(MT.Blitz, 1), OP.stick.mat(MT.Breeze, 1));
		RM.Centrifuge.addRecipe1(T, 16, 64, new long[] {10000,  1000}                  , IL.Comb_Tera    .get(1), NF   , FL.Concrete                             .make(   L), OM.dust(MT.Basalz, U9)            , OP.stick.mat(MT.Basalz, 1));
		});
		
		
		aItems.add(0, aID -> {IL.Food_Lemon                          .set(addItem(    aID, "Lemon"                                    , "Don't make Lemonade"         , "cropLemon"                   , new FoodStat( 1, 0.600F,   0, C+36,  0.30F,   0,   0,   0,   4,   0, ItemUseAnimation.EAT, null                                 , F, T, F, T, PotionsGT.ID_CONDUCTIVE, 300, 0, 70), TC.stack(TC.MESSIS, 1), TC.stack(TC.HERBA, 1), TC.stack(TC.FAMES, 1)));});
		aItems.add(1, aID -> {
			IL.Food_Lemon_Sliced                   .set(addItem(    aID, "Lemon Slice"                              , "Ideal to put on your Drink"                                  , new FoodStat( 0, 0.150F,   0, C+36,  0.30F,   0,   0,   0,   1,   0, ItemUseAnimation.EAT, null                                 , F, T, F, T, PotionsGT.ID_CONDUCTIVE, 300, 0, 70), TC.stack(TC.HERBA, 1))); Sandwiches.INGREDIENTS.put(last(), (byte)28);
		RM.replicateOrganic( 3,  4, IL.Food_Lemon.get(1));
		CR.shaped(IL.Food_Lemon_Sliced.get(4), CR.DEF_NCC, "kX", 'X', "cropLemon");
		});


		aItems.add(10, aID -> {IL.Food_Tomato                         .set(addItem(   aID, "Tomato"                                   , "Solid Ketchup"               , "cropTomato"                  , new FoodStat( 1, 0.600F,   0, C+36,  0.30F,   0,   0,   0,   4,   0, ItemUseAnimation.EAT, null                                 , F, T, F, T), TC.stack(TC.MESSIS, 1), TC.stack(TC.HERBA, 1), TC.stack(TC.FAMES, 1)));});
		aItems.add(11, aID -> {
			IL.Food_Tomato_Sliced                  .set(addItem(   aID, "Tomato Slice"                             , "Solid Ketchup"                                               , new FoodStat( 0, 0.150F,   0, C+36,  0.30F,   0,   0,   0,   1,   0, ItemUseAnimation.EAT, null                                 , F, T, F, T), TC.stack(TC.HERBA, 1))); Sandwiches.INGREDIENTS.put(last(), (byte)36);
		RM.replicateOrganic( 4,  5, IL.Food_Tomato.get(1));
		CR.shaped(IL.Food_Tomato_Sliced.get(4), CR.DEF_NCC, "kX", 'X', "cropTomato");
		});
		
		
		aItems.add(20, aID -> {
			IL.Food_MTomato                        .set(addItem(   aID, "Maxim Tomato"                             , "Ten Hearts in one Tomato"    , "cropTomato"                  , new FoodStat( 9, 1.000F,  50, C+36,  0.30F,   0,   0,   0,  10,   0, ItemUseAnimation.EAT, null                                 , F, T, F, T, /*regeneration*/10, 60, 4, 100), TC.stack(TC.MESSIS, 1), TC.stack(TC.SANO, 3), TC.stack(TC.FAMES, 1)));
		RM.replicateOrganic( 4,  6, IL.Food_MTomato.get(1));
		RM.food_can(IL.Food_MTomato.get(1),10, "Canned Max Tomato", IL.CANS_VEGGIE);
		});
		
		
		aItems.add(30, aID -> {IL.Food_Onion                          .set(addItem(   aID, "Onion"                                    , "Taking over the whole Taste" , "cropOnion"                   , new FoodStat( 1, 1.200F,   0, C+36,  0.30F,   0,   0,   0,   4,   0, ItemUseAnimation.EAT, null                                 , F, T, F, T), TC.stack(TC.MESSIS, 1), TC.stack(TC.HERBA, 1), TC.stack(TC.FAMES, 1)));});
		aItems.add(31, aID -> {
			IL.Food_Onion_Sliced                   .set(addItem(   aID, "Onion Slice"                              , "ONIONS, UNITE!"                                              , new FoodStat( 0, 0.300F,   0, C+36,  0.30F,   0,   0,   0,   1,   0, ItemUseAnimation.EAT, null                                 , F, T, F, T), TC.stack(TC.HERBA, 1))); Sandwiches.INGREDIENTS.put(last(), (byte)31);
		RM.replicateOrganic( 4,  7, IL.Food_Onion.get(1));
		CR.shaped(IL.Food_Onion_Sliced.get(4), CR.DEF_NCC, "kX", 'X', "cropOnion");
		});
		
		
		aItems.add(40, aID -> {IL.Food_Cucumber                       .set(addItem(   aID, "Cucumber"                              , "Is it a type of Melon or a type of Pumpkin?", "cropCucumber"    , new FoodStat( 1, 1.200F,   0, C+36,  0.30F,   0,   0,   0,   4,   0, ItemUseAnimation.EAT, null                                 , F, T, F, T), TC.stack(TC.MESSIS, 1), TC.stack(TC.HERBA, 1), TC.stack(TC.FAMES, 1)));});
		aItems.add(41, aID -> {IL.Food_Cucumber_Sliced                .set(addItem(   aID, "Cucumber Slice"                        , "Not a sliced Sea Cucumber!"                                     , new FoodStat( 0, 0.300F,   0, C+36,  0.30F,   0,   0,   0,   1,   0, ItemUseAnimation.EAT, null                                 , F, T, F, T), TC.stack(TC.HERBA, 1))); Sandwiches.INGREDIENTS.put(last(), (byte)23);});
		aItems.add(42, aID -> {IL.Food_Pickle                         .set(addItem(   aID, "Pickle"                                , "Not a Sea Pickle! Not Rick either!", "cropPickle", "foodPickles", new FoodStat( 1, 1.200F,   0, C+36,  0.30F,   4,   0,   0,   4,   0, ItemUseAnimation.EAT, null                                 , F, T, F, T), TC.stack(TC.MESSIS, 1), TC.stack(TC.HERBA, 1), TC.stack(TC.VENEMUM, 1)));});
		aItems.add(43, aID -> {
			IL.Food_Pickle_Sliced                  .set(addItem(   aID, "Pickle Slice"                          , "You seem to be in a Pickle."                                    , new FoodStat( 0, 0.300F,   0, C+36,  0.30F,   1,   0,   0,   1,   0, ItemUseAnimation.EAT, null                                 , F, T, F, T), TC.stack(TC.HERBA, 1))); Sandwiches.INGREDIENTS.put(last(), (byte)41);
		RM.replicateOrganic( 4,  8, IL.Food_Cucumber.get(1));
		RM.replicateOrganic( 4, 13, IL.Food_Pickle.get(1));
		CR.shaped(IL.Food_Cucumber_Sliced.get(4), CR.DEF_NCC, "kX", 'X', "cropCucumber");
		CR.shaped(IL.Food_Pickle_Sliced.get(4), CR.DEF_NCC, "kX", 'X', "cropPickle");
		CR.shaped(IL.Food_Pickle_Sliced.get(4), CR.DEF_NCC, "kX", 'X', "foodPickles");
		});
		
		
		aItems.add(50, aID -> {
			IL.Food_Chili_Pepper                   .set(addItem(   aID, "Chili Pepper"                             , "It is red and hot"           , "cropChilipepper"             , new FoodStat( 1, 1.200F, -10, C+40,  0.50F,   0,   0,  10,   0,   0, ItemUseAnimation.EAT, null                                 , F, T, F, T, PotionsGT.ID_FLAMMABLE, 300, 0, 70, /*confusion*/9, 200, 1, 40), TC.stack(TC.MESSIS, 1), TC.stack(TC.IGNIS, 1), TC.stack(TC.FAMES, 1))); Sandwiches.INGREDIENTS.put(last(), (byte)20);
		RM.replicateOrganic( 4,  9, IL.Food_Chili_Pepper.get(1));
		});
		
		
		aItems.add(60, aID -> {IL.Food_Grapes_Green                   .set(addItem(   aID, "Green Grapes"                             , "Source of Wine"              , "cropGrapeGreen"              , new FoodStat( 1, 0.600F,   0, C+36,  0.30F,   0,   0,   0,  12,   0, ItemUseAnimation.EAT, null                                 , F, T, F, T), TC.stack(TC.MESSIS, 1), TC.stack(TC.HERBA, 1), TC.stack(TC.FAMES, 1)));});
		aItems.add(61, aID -> {
			IL.Food_Raisins_Green                  .set(addItem(   aID, "Green Raisins"                            , "Dried Grapes"                , "foodRaisins"                 , new FoodStat( 2, 0.600F,   0, C+37,  0.20F,   0,   0,   0,  12,   0, ItemUseAnimation.EAT, null                                 , F, T, F, T), TC.stack(TC.MESSIS, 1), TC.stack(TC.VACUOS, 1), TC.stack(TC.FAMES, 1)));
		RM.replicateOrganic( 3,  5, IL.Food_Grapes_Green.get(1));
		});
		
		aItems.add(63, aID -> {IL.Food_Grapes_White                   .set(addItem(   aID, "White Grapes"                             , "Source of Wine"              , "cropGrapeWhite"              , new FoodStat( 1, 0.600F,   0, C+36,  0.30F,   0,   0,   0,  12,   0, ItemUseAnimation.EAT, null                                 , F, T, F, T), TC.stack(TC.MESSIS, 1), TC.stack(TC.HERBA, 1), TC.stack(TC.FAMES, 1)));});
		aItems.add(64, aID -> {
			IL.Food_Raisins_White                  .set(addItem(   aID, "White Raisins"                            , "Dried Grapes"                , "foodRaisins"                 , new FoodStat( 2, 0.600F,   0, C+37,  0.20F,   0,   0,   0,  12,   0, ItemUseAnimation.EAT, null                                 , F, T, F, T), TC.stack(TC.MESSIS, 1), TC.stack(TC.VACUOS, 1), TC.stack(TC.FAMES, 1)));
		RM.replicateOrganic( 3,  6, IL.Food_Grapes_White.get(1));
		});
		
		aItems.add(66, aID -> {IL.Food_Grapes_Red                     .set(addItem(   aID, "Red Grapes"                               , "Source of Wine"              , "cropGrapeRed"                , new FoodStat( 1, 0.600F,   0, C+36,  0.30F,   0,   0,   0,  12,   0, ItemUseAnimation.EAT, null                                 , F, T, F, T), TC.stack(TC.MESSIS, 1), TC.stack(TC.HERBA, 1), TC.stack(TC.FAMES, 1)));});
		aItems.add(67, aID -> {
			IL.Food_Raisins_Red                    .set(addItem(   aID, "Red Raisins"                              , "Dried Grapes"                , "foodRaisins"                 , new FoodStat( 2, 0.600F,   0, C+37,  0.20F,   0,   0,   0,  12,   0, ItemUseAnimation.EAT, null                                 , F, T, F, T), TC.stack(TC.MESSIS, 1), TC.stack(TC.VACUOS, 1), TC.stack(TC.FAMES, 1)));
		RM.replicateOrganic( 3,  7, IL.Food_Grapes_Red.get(1));
		});
		
		aItems.add(70, aID -> {IL.Food_Grapes_Purple                  .set(addItem(   aID, "Purple Grapes"                            , "Source of Wine"              , "cropGrapePurple"             , new FoodStat( 1, 0.600F,   0, C+36,  0.30F,   0,   0,   0,  12,   0, ItemUseAnimation.EAT, null                                 , F, T, F, T), TC.stack(TC.MESSIS, 1), TC.stack(TC.HERBA, 1), TC.stack(TC.FAMES, 1)));});
		aItems.add(71, aID -> {
			IL.Food_Raisins_Purple                 .set(addItem(   aID, "Purple Raisins"                           , "Dried Grapes"                , "foodRaisins"                 , new FoodStat( 2, 0.600F,   0, C+37,  0.20F,   0,   0,   0,  12,   0, ItemUseAnimation.EAT, null                                 , F, T, F, T), TC.stack(TC.MESSIS, 1), TC.stack(TC.VACUOS, 1), TC.stack(TC.FAMES, 1)));
		RM.replicateOrganic( 3,  8, IL.Food_Grapes_Purple.get(1));
		});
		
		aItems.add(72, aID -> {
			IL.Food_Raisins_Chocolate              .set(addItem(   aID, "Chocolate Raisins"                        , "Dried Grapes coated in Chocolate", "foodChocolateraisins"    , new FoodStat( 3, 1.200F,   0, C+37,  0.20F,   0,   0,   0,  40,   0, ItemUseAnimation.EAT, null                                 , F, T, F, T), TC.stack(TC.MESSIS, 1), TC.stack(TC.VACUOS, 1), TC.stack(TC.SANO, 1)));
		
		
		IL.Food_Carrot                         .set(ST.make(Items.CARROT, 1, 0)); FoodsGT.put(ST.make(Items.CARROT, 1, W), 0, 0, 0, 8, 0);
		});//                                            , new FoodStat( 5, 1.200F,   0, C+37,  0.10F,   0,   0,   0,   8,   0, ItemUseAnimation.EAT, null                                 , F, T, F, T), TC.stack(TC.HERBA, 1), TC.stack(TC.FAMES, 1), TC.stack(TC.IGNIS, 1), Behavior_FeedPig.INSTANCE));
		aItems.add(81, aID -> {
			IL.Food_Carrot_Sliced                  .set(addItem(   aID, "Carrot Slice"                             , "Sliced Goku"                                                 , new FoodStat( 0, 0.300F,   0, C+37,  0.30F,   0,   0,   0,   2,   0, ItemUseAnimation.EAT, null                                 , F, T, F, T), TC.stack(TC.HERBA, 1))); Sandwiches.INGREDIENTS.put(last(), (byte)18);
		RM.replicateOrganic( 4,  9, IL.Food_Carrot.get(1));
		CR.shaped(IL.Food_Carrot_Sliced.get(4), CR.DEF_NCC, "kX", 'X', "cropCarrot");
		
		
		IL.Food_Potato_Poisonous               .set(ST.make(Items.POISONOUS_POTATO, 1, 0)); FoodsGT.put(ST.make(Items.POISONOUS_POTATO, 1, W), 0, 0, 0, 4, 0);
		RM.replicateOrganic( 4, 11, IL.Food_Potato_Poisonous.get(1));
		
		
		IL.Food_Potato                         .set(ST.make(Items.POTATO, 1, 0)); FoodsGT.put(ST.make(Items.POTATO, 1, W), 0, 0, 0, 4, 0);
		});
		aItems.add(32700, aID -> {
			IL.Food_Potato_On_Stick                .set(addItem(aID, "Potato on a Stick"        , "Totally looks like a Crab Claw", new OreDictItemData(MT.Potato,U,ANY.Wood,U2), new FoodStat( 1, 0.600F,   0, C+37,  0.30F,   0,   0,   0,   4,   0, ItemUseAnimation.EAT, IL.Stick.get(1), F, T, F, T), TC.stack(TC.FAMES, 1), TC.stack(TC.MESSIS, 1), TC.stack(TC.ARBOR, 1)));
		RM.replicateOrganic( 4, 10, IL.Food_Potato.get(1));
		CR.shapeless(IL.Food_Potato_On_Stick.get(1), CR.DEF, new Object[] {OP.stick.dat(ANY.Wood), "cropPotato"});
		
		
		IL.Food_Potato_Baked                   .set(ST.make(Items.BAKED_POTATO, 1, 0)); FoodsGT.put(ST.make(Items.BAKED_POTATO, 1, W), 0, 0, 0, 4, 0);
		});
		aItems.add(32701, aID -> {
			IL.Food_Potato_On_Stick_Roasted        .set(addItem(aID, "Roasted Potato on a Stick", "Still looks like a Crab Claw"  , new OreDictItemData(MT.Potato,U,ANY.Wood,U2), new FoodStat( 5, 1.200F,   0, C+38,  0.50F,   0,   0,   0,   4,   0, ItemUseAnimation.EAT, IL.Stick.get(1), F, T, F, T), TC.stack(TC.FAMES, 1), TC.stack(TC.MESSIS, 1), TC.stack(TC.ARBOR, 1), TC.stack(TC.IGNIS, 1)));
		RM.add_smelting(IL.Food_Potato_On_Stick.get(1), IL.Food_Potato_On_Stick_Roasted.get(1), F, T, F);
		CR.shapeless(IL.Food_Potato_On_Stick_Roasted.get(1), CR.DEF, new Object[] {OP.stick.dat(ANY.Wood), IL.Food_Potato_Baked});
		for (OreDictMaterial tMat : ANY.Wood.mToThis) {ItemStack tStick = OP.stick.mat(tMat, 1); if (ST.valid(tStick))
		RM.Boxinator.addRecipe2(T, 16, 16, IL.Food_Potato_Baked.get(1), tStick, IL.Food_Potato_On_Stick_Roasted.get(1));
		}
		});
		
		aItems.add(8000, aID -> {IL.Food_Fries_Raw                      .set(addItem( aID, "Potato Strips"                            , "Long Potatoes"                                               , new FoodStat( 1, 1.200F,   0, C+37,  0.10F,   0,   0,   0,   5,   0, ItemUseAnimation.EAT, null                                 , F, T, F, T), TC.stack(TC.FAMES, 1), TC.stack(TC.MESSIS, 1), TC.stack(TC.HERBA, 1), new OreDictItemData(MT.Potato, U))); setFluidContainerStats(mLastID, 0, 8);});
		aItems.add(8010, aID -> {IL.Food_Fries                          .set(addItem( aID, "Fries"                                    , "Not to be confused with that Futurama Guy", "foodFries"      , new FoodStat( 7, 1.200F,   0, C+38,  0.50F,   0,   0,  10,  10,   0, ItemUseAnimation.EAT, null                                 , F, T, F, T), TC.stack(TC.FAMES, 1), TC.stack(TC.MESSIS, 1), TC.stack(TC.HERBA, 1), TC.stack(TC.IGNIS, 1)));             setFluidContainerStats(mLastID, 0, 8);});
		aItems.add(8011, aID -> {
			IL.Food_Fries_Packaged                 .set(addItem( aID, "Fries"                                    , "Ketchup not included"                                        , new FoodStat( 7, 1.200F,   0, C+38,  0.50F,   0,   0,  10,  10,   0, ItemUseAnimation.EAT, OP.scrapGt.mat(MT.Paper, 16)         , F, T, F, T), TC.stack(TC.FAMES, 1), TC.stack(TC.MESSIS, 1), TC.stack(TC.HERBA, 1), TC.stack(TC.IGNIS, 1)));
		RM.Bath.addRecipe1(T,  0,   16, IL.Food_Fries_Raw.get(1), MT.FryingOilHot.liquid(U/100, T), NF, IL.Food_Fries.get(1));
		CR.shaped(IL.Food_Fries_Raw.get(1), CR.DEF_NCC, "k", "X", 'X', "cropPotato");
		CR.shapeless(IL.Food_Fries_Packaged.get(1), CR.DEF_NCC, new Object[] {OP.plateDouble.dat(MT.Paper), IL.Food_Fries});
		RM.Boxinator.addRecipe2(T, 16, 16, IL.Food_Fries.get(1), OP.plateDouble.mat(MT.Paper, 1), IL.Food_Fries_Packaged.get(1));
		RM.Unboxinator.addRecipe1(T, 16, 16, IL.Food_Fries_Packaged.get(1), IL.Food_Fries.get(1), OP.scrapGt.mat(MT.Paper, 16));
		});
		
		
		aItems.add(9000, aID -> {IL.Food_PotatoChips_Raw                .set(addItem( aID, "Potato Chips (Raw)"                       , "Just like a Potato"                                          , new FoodStat( 1, 1.200F,   0, C+37,  0.10F,   0,   0,   0,   0,   0, ItemUseAnimation.EAT, null                                 , F, T, F, T), TC.stack(TC.FAMES, 1), TC.stack(TC.MESSIS, 1), TC.stack(TC.HERBA, 1), new OreDictItemData(MT.Potato, U))); setFluidContainerStats(mLastID, 0, 8);});
		aItems.add(9010, aID -> {IL.Food_PotatoChips                    .set(addItem( aID, "Potato Chips"                             , "Crunchy"                                                     , new FoodStat( 7, 1.200F,   0, C+37,  0.10F,   0,   0,  10,  10,   0, ItemUseAnimation.EAT, null                                 , F, T, F, T), TC.stack(TC.FAMES, 1), TC.stack(TC.MESSIS, 1), TC.stack(TC.HERBA, 1), TC.stack(TC.IGNIS, 1)));             setFluidContainerStats(mLastID, 0, 8);});
		aItems.add(9011, aID -> {
			IL.Food_PotatoChips_Packaged           .set(addItem( aID, "Bag of Potato Chips"                      , "Full of delicious Air"                                       , new FoodStat( 7, 1.200F,   0, C+37,  0.10F,   0,   0,  10,  10,   0, ItemUseAnimation.EAT, OP.scrapGt.mat(MT.Al, 2)             , F, T, F, T), TC.stack(TC.FAMES, 1), TC.stack(TC.MESSIS, 1), TC.stack(TC.HERBA, 1), TC.stack(TC.IGNIS, 1)));
		RM.add_smelting(IL.Food_PotatoChips_Raw.get(1), IL.Food_PotatoChips.get(1), F, T, F);
		CR.shaped(IL.Food_PotatoChips_Raw.get(1), CR.DEF_NCC, "kX", 'X', "cropPotato");
		CR.shapeless(IL.Food_PotatoChips_Packaged.get(1), CR.DEF_NCC, new Object[] {OP.foil.dat(MT.Al), IL.Food_PotatoChips});
		RM.Boxinator.addRecipe2(T, 16, 16, IL.Food_PotatoChips.get(1), OP.foil.mat(MT.Al, 1), IL.Food_PotatoChips_Packaged.get(1));
		RM.Unboxinator.addRecipe1(T, 16, 16, IL.Food_PotatoChips_Packaged.get(1), IL.Food_PotatoChips.get(1), OP.scrapGt.mat(MT.Al, 2));
		});
		
		
		aItems.add(9020, aID -> {IL.Food_ChiliChips                     .set(addItem( aID, "Chili Chips"                              , "Spicy"                                                       , new FoodStat( 7, 1.200F, -10, C+40,  0.30F,   0,   0,  30,  10,   0, ItemUseAnimation.EAT, null                                 , F, T, F, T, PotionsGT.ID_FLAMMABLE, 300, 0, 70), TC.stack(TC.FAMES, 1), TC.stack(TC.MESSIS, 1), TC.stack(TC.HERBA, 1), TC.stack(TC.IGNIS, 1))); setFluidContainerStats(mLastID, 0, 8);});
		aItems.add(9021, aID -> {
			IL.Food_ChiliChips_Packaged            .set(addItem( aID, "Bag of Chili Chips"                       , "Stop making noises Baj!"                                     , new FoodStat( 7, 1.200F, -10, C+40,  0.30F,   0,   0,  30,  10,   0, ItemUseAnimation.EAT, OP.scrapGt.mat(MT.Al, 2)             , F, T, F, T, PotionsGT.ID_FLAMMABLE, 300, 0, 70), TC.stack(TC.FAMES, 1), TC.stack(TC.MESSIS, 1), TC.stack(TC.HERBA, 1), TC.stack(TC.IGNIS, 1)));
		CR.shapeless(IL.Food_ChiliChips_Packaged.get(1), CR.DEF_NCC, new Object[] {OP.foil.dat(MT.Al), IL.Food_ChiliChips});
		RM.Boxinator.addRecipe2(T, 16, 16, IL.Food_ChiliChips.get(1), OP.foil.mat(MT.Al, 1), IL.Food_ChiliChips_Packaged.get(1));
		RM.Unboxinator.addRecipe1(T, 16, 16, IL.Food_ChiliChips_Packaged.get(1), IL.Food_ChiliChips.get(1), OP.scrapGt.mat(MT.Al, 2));
		
		
		RM.replicateOrganic( 4, 12, ST.make(Blocks.PUMPKIN, 1, 0));
		});
		
		
		aItems.add(90, aID -> {IL.Food_Banana                         .set(addItem(   aID, "Banana"                                   , "For Scale"                   , "cropBanana"                  , new FoodStat( 1, 0.600F,   0, C+36,  0.30F,   0,   0,   0,   8,   0,4,ItemUseAnimation.EAT, null                                , F, T, F, T, PotionsGT.ID_SLIPPERY, 300, 1, 70), TC.stack(TC.MESSIS, 1), TC.stack(TC.HERBA, 1), TC.stack(TC.FAMES, 1)));});
		aItems.add(91, aID -> {
			IL.Food_Banana_Sliced                  .set(addItem(   aID, "Banana Slice"                             , "Food for Minions"                                            , new FoodStat( 0, 0.150F,   0, C+36,  0.30F,   0,   0,   0,   2,   0,1,ItemUseAnimation.EAT, null                                , F, T, F, T, PotionsGT.ID_SLIPPERY, 300, 1, 70), TC.stack(TC.HERBA, 1))); Sandwiches.INGREDIENTS.put(last(), (byte)17);
		RM.replicateOrganic( 3,  9, IL.Food_Banana.get(1));
		CR.shaped(IL.Food_Banana_Sliced.get(4), CR.DEF_NCC, "kX", 'X', "cropBanana");
		});
		
		
		aItems.add(100, aID -> {IL.Food_Pomegranate                    .set(addItem(  aID, "Pomegranate"                              , "A seeded Apple"              , "cropPomegranate"             , new FoodStat( 1, 0.600F,   0, C+36,  0.30F,   0,   0,   0,   8,   0, ItemUseAnimation.EAT, null                                 , F, T, F, T), TC.stack(TC.MESSIS, 1), TC.stack(TC.HERBA, 1), TC.stack(TC.BESTIA, 1)));});
		aItems.add(101, aID -> {
			IL.Food_Pomeraisins                    .set(addItem(  aID, "Pomeraisins"                              , "Lesser Dogs favourite Food"  , "foodRaisins"                 , new FoodStat( 2, 0.600F,   0, C+37,  0.20F,   0,   0,   0,   2,   0, ItemUseAnimation.EAT, null                                 , F, T, F, T), TC.stack(TC.MESSIS, 1), TC.stack(TC.VACUOS, 1), TC.stack(TC.BESTIA, 1), Behavior_FeedDog.INSTANCE));
		RM.replicateOrganic( 3, 10, IL.Food_Pomegranate.get(1));
		});
		
		
		aItems.add(110, aID -> {
			IL.Food_Blueberry                      .set(addItem(  aID, "Blueberry"                                , ""                            , "cropBlueberry"               , new FoodStat( 1, 0.600F,   0, C+36,  0.30F,   0,   0,   0,   8,   0, ItemUseAnimation.EAT, null                                 , F, T, F, T), TC.stack(TC.MESSIS, 1), TC.stack(TC.HERBA, 1), TC.stack(TC.SENSUS, 1)));
		RM.replicateOrganic( 5,  6, IL.Food_Blueberry.get(1));
		BushesGT.put(IL.Food_Blueberry.get(1), 0x22ff22, 0xffcccc, 0x6666dd, 0x0000ff);
		});
		
		aItems.add(120, aID -> {
			IL.Food_Gooseberry                     .set(addItem(  aID, "Gooseberry"                               , ""                            , "cropGooseberry"              , new FoodStat( 1, 0.600F,   0, C+36,  0.30F,   0,   0,   0,   8,   0, ItemUseAnimation.EAT, null                                 , F, T, F, T), TC.stack(TC.MESSIS, 1), TC.stack(TC.HERBA, 1), TC.stack(TC.FAMES, 1)));
		RM.replicateOrganic( 5,  7, IL.Food_Gooseberry.get(1));
		});
		// Grows on Trees
		
		aItems.add(130, aID -> {
			IL.Food_Candleberry                    .set(addItem(  aID, "Candleberry"                              , ""                            , "cropCandleberry"             , new FoodStat( 1, 0.600F,   0, C+36,  0.30F,   0,   0,   0,   8,   0, ItemUseAnimation.EAT, null                                 , F, T, F, T, PotionsGT.ID_FLAMMABLE, 300, 0, 30), TC.stack(TC.MESSIS, 1), TC.stack(TC.HERBA, 1), TC.stack(TC.GELUM, 1)));
		RM.replicateOrganic( 5,  8, IL.Food_Candleberry.get(1));
		BushesGT.put(IL.Food_Candleberry.get(1), 0x44ff44, 0xccffcc, 0xaaffaa, 0xccffcc);
		});
		
		aItems.add(140, aID -> {
			IL.Food_Cranberry                      .set(addItem(  aID, "Cranberry"                                , ""                            , "cropCranberry"               , new FoodStat( 1, 0.600F,   0, C+36,  0.30F,   0,   0,   0,   8,   0, ItemUseAnimation.EAT, null                                 , F, T, F, T), TC.stack(TC.MESSIS, 1), TC.stack(TC.HERBA, 1), TC.stack(TC.FAMES, 1)));
		RM.replicateOrganic( 5,  9, IL.Food_Cranberry.get(1));
		BushesGT.put(IL.Food_Cranberry.get(1), 0x00dd00, 0xffcccc, 0x66ff66, 0xff0000);
		});
		
		aItems.add(150, aID -> {
			IL.Food_Currants_Black                 .set(addItem(  aID, "Black Currants"                           , ""                            , "cropCurrantsBlack"           , new FoodStat( 1, 0.600F,   0, C+36,  0.30F,   0,   0,   0,   6,   0, ItemUseAnimation.EAT, null                                 , F, T, F, T), TC.stack(TC.MESSIS, 1), TC.stack(TC.HERBA, 1), TC.stack(TC.TENEBRAE, 1)));
		RM.replicateOrganic( 5, 10, IL.Food_Currants_Black.get(1));
		BushesGT.put(IL.Food_Currants_Black.get(1), 0x33ff33, 0xaaaaaa, 0x66ff66, 0x111111);
		});
		
		aItems.add(160, aID -> {
			IL.Food_Currants_White                 .set(addItem(  aID, "White Currants"                           , ""                            , "cropCurrantsWhite"           , new FoodStat( 1, 0.600F,   0, C+36,  0.30F,   0,   0,   0,   8,   0, ItemUseAnimation.EAT, null                                 , F, T, F, T), TC.stack(TC.MESSIS, 1), TC.stack(TC.HERBA, 1), TC.stack(TC.ORDO, 1)));
		RM.replicateOrganic( 5, 11, IL.Food_Currants_White.get(1));
		BushesGT.put(IL.Food_Currants_White.get(1), 0x33ff33, 0xaaaaaa, 0x66ff66, 0xeeeedd);
		});
		
		aItems.add(170, aID -> {
			IL.Food_Currants_Red                   .set(addItem(  aID, "Red Currants"                             , ""                            , "cropCurrantsRed"             , new FoodStat( 1, 0.600F,   0, C+36,  0.30F,   0,   0,   0,   8,   0, ItemUseAnimation.EAT, null                                 , F, T, F, T), TC.stack(TC.MESSIS, 1), TC.stack(TC.HERBA, 1), TC.stack(TC.SANO, 1)));
		RM.replicateOrganic( 5, 12, IL.Food_Currants_Red.get(1));
		BushesGT.put(IL.Food_Currants_Red.get(1), 0x33ff33, 0xaaaaaa, 0x66ff66, 0xee0000);
		});
		
		aItems.add(180, aID -> {
			IL.Food_Blackberry                     .set(addItem(  aID, "Blackberry"                               , ""                            , "cropBlackberry"              , new FoodStat( 1, 0.600F,   0, C+36,  0.30F,   0,   0,   0,   8,   0, ItemUseAnimation.EAT, null                                 , F, T, F, T), TC.stack(TC.MESSIS, 1), TC.stack(TC.HERBA, 1), TC.stack(TC.TENEBRAE, 1)));
		RM.replicateOrganic( 5, 13, IL.Food_Blackberry.get(1));
		BushesGT.put(IL.Food_Blackberry.get(1), 0x11ff11, 0xffcccc, 0x663333, 0x331111);
		});
		
		aItems.add(190, aID -> {
			IL.Food_Raspberry                      .set(addItem(  aID, "Raspberry"                                , ""                            , "cropRaspberry"               , new FoodStat( 1, 0.600F,   0, C+36,  0.30F,   0,   0,   0,   8,   0, ItemUseAnimation.EAT, null                                 , F, T, F, T), TC.stack(TC.MESSIS, 1), TC.stack(TC.HERBA, 1), TC.stack(TC.SANO, 1)));
		RM.replicateOrganic( 5, 14, IL.Food_Raspberry.get(1));
		BushesGT.put(IL.Food_Raspberry.get(1), 0x11ff11, 0xffcccc, 0x664444, 0xffaaaa);
		});
		
		aItems.add(200, aID -> {
			IL.Food_Strawberry                     .set(addItem(  aID, "Strawberry"                               , ""                            , "cropStrawberry"              , new FoodStat( 1, 0.600F,   0, C+36,  0.30F,   0,   0,   0,   8,   0, ItemUseAnimation.EAT, null                                 , F, T, F, T), TC.stack(TC.MESSIS, 1), TC.stack(TC.HERBA, 1), TC.stack(TC.SANO, 1)));
		RM.replicateOrganic( 5, 15, IL.Food_Strawberry.get(1));
		});
		// Grows on Weeds
		
		
		aItems.add(210, aID -> {IL.Food_Apple_Green                    .set(addItem(  aID, "Apple"            , ""                                                    , "cropAppleGreen"              , new FoodStat( 4, 0.400F,  10, C+36,  0.30F,   0,   0,   0,   4,   0, ItemUseAnimation.EAT, ST.make(this, 1, 212)                , F, T, F, T), TC.stack(TC.MESSIS, 2), TC.stack(TC.FAMES, 1), Behavior_FeedPig.INSTANCE));});
		aItems.add(211, aID -> {IL.Food_Apple_Green_Sliced             .set(addItem(  aID, "Apple Slice"      , ""                                                                                    , new FoodStat( 1, 0.400F,  10, C+36,  0.30F,   0,   0,   0,   1,   0, ItemUseAnimation.EAT, null                                 , F, T, F, T), TC.stack(TC.HERBA, 1)));});
		aItems.add(212, aID -> {
			IL.Food_Apple_Green_Core               .set(addItem(  aID, "Apple Core"       , "Not to be confused with the Mod"                     , "itemPlantRemains"            , TICKS_PER_SMELT / 4, TC.stack(TC.HERBA, 1), Behavior_FeedPig.INSTANCE));
		RM.replicateOrganic( 3, 11, IL.Food_Apple_Green.get(1));
		CR.shaped(IL.Food_Apple_Green_Sliced    .get(4), CR.DEF_NCC, "kX", 'X', "cropAppleGreen");
		});
		
		
		aItems.add(220, aID -> {IL.Food_Apple_Yellow                   .set(addItem(  aID, "Apple"            , ""                                                    , "cropAppleYellow"             , new FoodStat( 5, 0.300F,   5, C+36,  0.30F,   0,   0,   0,   8,   0, ItemUseAnimation.EAT, ST.make(this, 1, 222)                , F, T, F, T), TC.stack(TC.MESSIS, 2), TC.stack(TC.FAMES, 1), Behavior_FeedPig.INSTANCE));});
		aItems.add(221, aID -> {IL.Food_Apple_Yellow_Sliced            .set(addItem(  aID, "Apple Slice"      , ""                                                                                    , new FoodStat( 1, 0.300F,   5, C+36,  0.30F,   0,   0,   0,   2,   0, ItemUseAnimation.EAT, null                                 , F, T, F, T), TC.stack(TC.HERBA, 1)));});
		aItems.add(222, aID -> {
			IL.Food_Apple_Yellow_Core              .set(addItem(  aID, "Apple Core"       , "Not to be confused with the Mod"                     , "itemPlantRemains"            , TICKS_PER_SMELT / 4, TC.stack(TC.HERBA, 1), Behavior_FeedPig.INSTANCE));
		RM.replicateOrganic( 3, 12, IL.Food_Apple_Yellow.get(1));
		CR.shaped(IL.Food_Apple_Yellow_Sliced   .get(4), CR.DEF_NCC, "kX", 'X', "cropAppleYellow");
		
		
		IL.Food_Apple_Red                      .set(ST.make(Items.APPLE, 1, 0)); FoodsGT.put(ST.make(Items.APPLE, 1, W), 0, 0, 0, 8, 0);
		});//    , "cropAppleRed"                , new FoodStat( 4, 0.300F,   0, C+37,  0.30F,   0,   0,   0,   8,   0, ItemUseAnimation.EAT, ST.make(this, 1, 232)                , F, T, F, T), TC.stack(TC.MESSIS, 2), TC.stack(TC.FAMES, 1), Behavior_FeedPig.INSTANCE));
		aItems.add(231, aID -> {IL.Food_Apple_Red_Sliced               .set(addItem(  aID, "Apple Slice"      , ""                                                                                    , new FoodStat( 1, 0.300F,   0, C+37,  0.30F,   0,   0,   0,   2,   0, ItemUseAnimation.EAT, null                                 , F, T, F, T), TC.stack(TC.HERBA, 1)));});
		aItems.add(232, aID -> {
			IL.Food_Apple_Red_Core                 .set(addItem(  aID, "Apple Core"       , "Not to be confused with the Mod"                     , "itemPlantRemains"            , TICKS_PER_SMELT / 4, TC.stack(TC.HERBA, 1), Behavior_FeedPig.INSTANCE));
		RM.replicateOrganic( 3, 13, IL.Food_Apple_Red.get(1));
		CR.shaped(IL.Food_Apple_Red_Sliced      .get(4), CR.DEF_NCC, "kX", 'X', "cropAppleRed");
		});
		
		
		aItems.add(240, aID -> {IL.Food_Apple_DarkRed                  .set(addItem(  aID, "Apple"            , ""                                                    , "cropAppleDarkRed"            , new FoodStat( 5, 0.400F,   5, C+37,  0.30F,   0,   0,   0,  12,   0, ItemUseAnimation.EAT, ST.make(this, 1, 242)                , F, T, F, T), TC.stack(TC.MESSIS, 2), TC.stack(TC.FAMES, 1), Behavior_FeedPig.INSTANCE));});
		aItems.add(241, aID -> {IL.Food_Apple_DarkRed_Sliced           .set(addItem(  aID, "Apple Slice"      , ""                                                                                    , new FoodStat( 1, 0.400F,   5, C+37,  0.30F,   0,   0,   0,   3,   0, ItemUseAnimation.EAT, null                                 , F, T, F, T), TC.stack(TC.HERBA, 1)));});
		aItems.add(242, aID -> {
			IL.Food_Apple_DarkRed_Core             .set(addItem(  aID, "Apple Core"       , "Not to be confused with the Mod"                     , "itemPlantRemains"            , TICKS_PER_SMELT / 4, TC.stack(TC.HERBA, 1), Behavior_FeedPig.INSTANCE));
		RM.replicateOrganic( 3, 14, IL.Food_Apple_DarkRed.get(1));
		CR.shaped(IL.Food_Apple_DarkRed_Sliced  .get(4), CR.DEF_NCC, "kX", 'X', "cropAppleDarkRed");
		
		
		RM.replicateOrganic( 3, 15, ST.make(Blocks.MELON, 1, 0));
		});
		
		
		aItems.add(250, aID -> {IL.Food_Peanut                         .set(addItem(  aID, "Peanut"           , "Deez Nutz"                                           , "cropPeanut"                  , new FoodStat( 2, 0.300F,   0, C+37,  0.10F,   0,   0,   0,   0,  16, ItemUseAnimation.EAT, null                                 , F, T, F, T), TC.stack(TC.MESSIS, 1), TC.stack(TC.GRANUM, 1), TC.stack(TC.COGNITIO, 1)));});
		aItems.add(251, aID -> {
			addItem(aID, "", "", new Behavior_Turn_Into(IL.Food_Coconut), TD.Creative.HIDDEN); // Migrating a GT6U Mistake when assigning IDs. There are 10-ID-wide gaps for a reason.
		RM.replicateOrganic( 6,  7, IL.Food_Peanut.get(1));
		});
		
		
		aItems.add(260, aID -> {
			IL.Food_Hazelnut                       .set(addItem(  aID, "Hazelnut"         , ""                                                    , "cropHazelnut"                , new FoodStat( 2, 0.300F,   0, C+37,  0.10F,   0,   0,   0,   0,  16, ItemUseAnimation.EAT, null                                 , F, T, F, T), TC.stack(TC.MESSIS, 1), TC.stack(TC.GRANUM, 1), TC.stack(TC.PERFODIO, 1)));
		RM.replicateOrganic( 6,  8, IL.Food_Hazelnut.get(1));
		});
		
		
		aItems.add(270, aID -> {IL.Food_Ananas                         .set(addItem(  aID, "Ananas"           , "Who lives in a Pineapple under the the Sea?"         , "cropAnanas"                  , new FoodStat( 4, 0.300F,  10, C+36,  0.30F,   0,   0,   0,   8,   0, ItemUseAnimation.EAT, null                                 , F, T, F, T), TC.stack(TC.MESSIS, 1), TC.stack(TC.ARBOR, 1), TC.stack(TC.FAMES, 1)));});
		aItems.add(271, aID -> {
			IL.Food_Ananas_Sliced                  .set(addItem(  aID, "Ananas Slice"     , "Did Ted ever find out about the Mystery Pineapple?"                                  , new FoodStat( 1, 0.300F,  10, C+36,  0.30F,   0,   0,   0,   2,   0, ItemUseAnimation.EAT, null                                 , F, T, F, T), TC.stack(TC.ARBOR, 1))); Sandwiches.INGREDIENTS.put(last(), (byte)16);
		RM.replicateOrganic( 2,  8, IL.Food_Ananas.get(1));
		CR.shaped(IL.Food_Ananas_Sliced.get(4), CR.DEF_NCC, "kX", 'X', "cropAnanas");
		});
		
		
		aItems.add(280, aID -> {
			IL.Food_Cinnamon                       .set(addItem(  aID, "Cinnamon Bark"    , "Don't let anyone Challenge you!"                     , "cropCinnamon"                , new FoodStat( 2, 0.300F,   0, C+37,  0.10F,   0,   0,   0,   0,   0, ItemUseAnimation.EAT, null                                 , F, T, F, T), TC.stack(TC.ARBOR, 1), TC.stack(TC.FAMES, 1)));
		RM.replicateOrganic( 2,  7, IL.Food_Cinnamon.get(1));
		});
		
		// Couldn't resist to put Lyrics from the Donkey Kong Rap there.
		aItems.add(290, aID -> {
			IL.Food_Coconut                        .set(addItem(  aID, "Coconut"          , "His Coconut Gun can fire in spurts. If he shoots ya, it's gonna hurt!", "cropCoconut", new FoodStat( 2, 0.300F,   0, C+37,  0.10F,   0,   0,   0,   8,   8, ItemUseAnimation.EAT, null                                 , F, T, F, T), TC.stack(TC.MESSIS, 1), TC.stack(TC.GRANUM, 1), TC.stack(TC.TUTAMEN, 1)));
		RM.replicateOrganic( 2,  9, IL.Food_Coconut.get(1));
		});
		
		
		aItems.add(1000, aID -> {IL.Food_Cheese                         .set(addItem( aID, "Cheese"           , "Click the Cheese"                                    , "foodCheese"                  , new FoodStat( 2, 1.200F,   0, C+37,  0.10F,   0,   0,   8,   0,   8, ItemUseAnimation.EAT, null                                 , F, T, F, T), TC.stack(TC.FAMES, 2)));});
		aItems.add(1001, aID -> {
			IL.Food_Cheese_Sliced                  .set(addItem( aID, "Cheese Slice"     , "ALIEN ATTACK!!!, throw the CHEEEEESE!!!"                                             , new FoodStat( 1, 0.600F,   0, C+37,  0.10F,   0,   0,   2,   0,   2, ItemUseAnimation.EAT, null                                 , F, T, F, T), TC.stack(TC.FAMES, 1), new OreDictItemData(MT.Cheese, U4))); Sandwiches.INGREDIENTS.put(last(), (byte)19);
		CR.shaped(IL.Food_Cheese_Sliced.get(4), CR.DEF_NCC, "kX", 'X', "foodCheese");
		
		
		IL.Food_Brown_Egg                      .set(ST.make(Items.EGG, 1, 0));
		});
		aItems.add(1051, aID -> {IL.Food_White_Egg                      .set(addItem( aID, "Egg"              , "The Egg came before the Chicken!"                    , OD.itemEgg                    , TC.stack(TC.BESTIA, 1), TC.stack(TC.LIMUS, 1), TC.stack(TC.VICTUS, 1)));});
		aItems.add(1060, aID -> {IL.Food_Brown_Egg_Boiled               .set(addItem( aID, "Boiled Egg"       , "Did you expect this to look different after boiling?", "foodBoiledegg"               , new FoodStat( 2, 1.200F,   0, C+37,  0.50F,   0,   0,   0,   4,   8, ItemUseAnimation.EAT, null                                 , F, T, F, T                                                                    ), TC.stack(TC.FAMES, 2)));});
		aItems.add(1061, aID -> {IL.Food_White_Egg_Boiled               .set(addItem( aID, "Boiled Egg"       , "Did you expect this to look different after boiling?", "foodBoiledegg"               , new FoodStat( 2, 1.200F,   0, C+37,  0.50F,   0,   0,   0,   4,   8, ItemUseAnimation.EAT, null                                 , F, T, F, T                                                                    ), TC.stack(TC.FAMES, 2)));});
		aItems.add(1070, aID -> {IL.Food_Egg_Fried                      .set(addItem( aID, "Fried Egg"        , ""                                                    , "foodFriedegg"                , new FoodStat( 2, 1.200F,   0, C+37,  0.50F,   0,   0,   0,   4,   8, ItemUseAnimation.EAT, null                                 , F, T, F, T                                                                    ), TC.stack(TC.FAMES, 2))); Sandwiches.INGREDIENTS.put(last(), (byte)43);});
		aItems.add(1071, aID -> {IL.Food_Egg_Scrambled                  .set(addItem( aID, "Scrambled Egg"    , ""                                                    , "foodScrambledegg"            , new FoodStat( 2, 1.200F,   0, C+37,  0.10F,   0,   0,   0,   4,   8, ItemUseAnimation.EAT, null                                 , F, T, F, T, PotionsGT.ID_SLIPPERY, 300, 0, 30, PotionsGT.ID_STICKY, 300, 0, 30), TC.stack(TC.FAMES, 2))); Sandwiches.INGREDIENTS.put(last(), (byte)(200+DYE_INDEX_Yellow));});
		aItems.add(1072, aID -> {IL.Food_Egg_Sliced                     .set(addItem( aID, "Sliced Egg"       , "Eggcellent!"                                                                         , new FoodStat( 1, 0.600F,   0, C+37,  0.50F,   0,   0,   0,   1,   2, ItemUseAnimation.EAT, null                                 , F, T, F, T                                                                    ), TC.stack(TC.FAMES, 1))); Sandwiches.INGREDIENTS.put(last(), (byte)44);});
		aItems.add(1073, aID -> {IL.Food_Egg_Yolk                       .set(addItem( aID, "Egg Yolk"         , "That's all, Yolks!"                                                                  , new FoodStat( 1, 1.200F,   0, C+37,  0.50F,   0,   0,   0,   2,   4, ItemUseAnimation.EAT, null                                 , F, T, F, T, PotionsGT.ID_SLIPPERY, 300, 0, 30                                 ), TC.stack(TC.FAMES, 1))); Sandwiches.INGREDIENTS.put(last(), (byte)(200+DYE_INDEX_Yellow));});
		aItems.add(1074, aID -> {
			IL.Food_Egg_White                      .set(addItem( aID, "Egg White"        , ""                                                                                    , new FoodStat( 1, 1.200F,   0, C+37,  0.50F,   0,   0,   0,   2,   4, ItemUseAnimation.EAT, null                                 , F, T, F, T, PotionsGT.ID_STICKY  , 300, 0, 30                                 ), TC.stack(TC.FAMES, 1))); Sandwiches.INGREDIENTS.put(last(), (byte)(200+DYE_INDEX_White));
		CR.shaped(IL.Food_Egg_Sliced.get(4), CR.DEF_NCC, "kX", 'X', "foodBoiledegg");
		RM.generify(IL.Food_White_Egg.get(1), ST.make(Items.EGG, 1, 0));
		RM.Mixer.addRecipe2(T, 16, 16, IL.Food_Egg_White.get(1), IL.Food_Egg_Yolk.get(1), IL.Food_Egg_Scrambled.get(1));
		for (String tCookingOil : FluidsGT.COOKING_OIL) if (FL.exists(tCookingOil)) {
		for (String tFluid : FluidsGT.VINEGAR) if (FL.exists(tFluid))
		RM.Mixer.addRecipe1(T, 16, 16, IL.Food_Egg_Yolk.get(1), FL.array(FL.make(tCookingOil, 100), FL.make(tFluid    , 100)), FL.Mayo.make(250), ZL_IS);
		RM.Mixer.addRecipe1(T, 16, 16, IL.Food_Egg_Yolk.get(1), FL.array(FL.make(tCookingOil, 100), FL.Juice_Lemon.make(100)), FL.Mayo.make(250), ZL_IS);
		RM.Mixer.addRecipe1(T, 16, 16, IL.Food_Egg_Yolk.get(1), FL.array(FL.make(tCookingOil, 100), FL.Juice_Lime .make(100)), FL.Mayo.make(250), ZL_IS);
		}
		
		FoodsGT.put(ST.make(Items.COD           , 1, W), 0, 0, 0, 0,12); Sandwiches.INGREDIENTS.put(ST.make(Items.COD           , 1, W), (byte)24);
		FoodsGT.put(ST.make(Items.COOKED_COD  , 1, W), 0, 0, 0, 0,12); Sandwiches.INGREDIENTS.put(ST.make(Items.COOKED_COD  , 1, W), (byte)25);
		
		FoodsGT.put(ST.make(Items.BEEF           , 1, W), 0, 0, 0, 0,16); Sandwiches.INGREDIENTS.put(ST.make(Items.BEEF           , 1, W), (byte)29);
		FoodsGT.put(ST.make(Items.COOKED_BEEF    , 1, W), 0, 0, 0, 0,16); Sandwiches.INGREDIENTS.put(ST.make(Items.COOKED_BEEF    , 1, W), (byte)30);
		
		FoodsGT.put(ST.make(Items.CHICKEN        , 1, W), 0, 0, 0, 0,12); Sandwiches.INGREDIENTS.put(ST.make(Items.CHICKEN        , 1, W), (byte)29);
		FoodsGT.put(ST.make(Items.COOKED_CHICKEN , 1, W), 0, 0, 0, 0,12); Sandwiches.INGREDIENTS.put(ST.make(Items.COOKED_CHICKEN , 1, W), (byte)30);
		
		FoodsGT.put(ST.make(Items.PORKCHOP       , 1, W), 0, 0, 0, 0,16); Sandwiches.INGREDIENTS.put(ST.make(Items.PORKCHOP       , 1, W), (byte)29);
		FoodsGT.put(ST.make(Items.COOKED_PORKCHOP, 1, W), 0, 0, 0, 0,16); Sandwiches.INGREDIENTS.put(ST.make(Items.COOKED_PORKCHOP, 1, W), (byte)30);
		
		FoodsGT.put(ST.make(Items.ROTTEN_FLESH   , 1, W),10, 0, 0, 0, 8); Sandwiches.INGREDIENTS.put(ST.make(Items.ROTTEN_FLESH   , 1, W), (byte)29);
		});
		
		
		aItems.add(1100, aID -> {IL.Food_Ham_Raw                        .set(addItem( aID, "Raw Ham"                                  , "Dropped by Pigs and Boars"   , "foodHamraw"                  , new FoodStat( 3, 0.600F,   0, C+37,  0.10F,   0,   0,   0,   0,  24, ItemUseAnimation.EAT, null                                 , F, T, F, T), TC.stack(TC.FAMES, 1), TC.stack(TC.CORPUS, 2), new OreDictItemData(MT.MeatRaw, U*2, MT.Bone, U4)));});
		aItems.add(1101, aID -> {
			IL.Food_Ham_Cooked                     .set(addItem( aID, "Cooked Ham"                               , ""                            , "foodHamcooked"               , new FoodStat(10, 1.600F,   0, C+38,  0.50F,   0,   0,   0,   0,  24, ItemUseAnimation.EAT, null                                 , F, T, F, T), TC.stack(TC.FAMES, 1), TC.stack(TC.CORPUS, 2), new OreDictItemData(MT.MeatCooked, U*2, MT.Bone, U4)));
		RM.add_smelting(IL.Food_Ham_Raw.get(1), IL.Food_Ham_Cooked.get(1), F, T, F);
		});
		
		
		aItems.add(1102, aID -> {IL.Food_Ham_Slice_Raw                  .set(addItem( aID, "Raw Ham Slice"                            , ""                                                            , new FoodStat( 1, 0.600F,   0, C+37,  0.10F,   0,   0,   0,   0,   6, ItemUseAnimation.EAT, null                                 , F, T, F, T), TC.stack(TC.CORPUS, 1), new OreDictItemData(MT.MeatRaw, U2))); Sandwiches.INGREDIENTS.put(last(), (byte)26);});
		aItems.add(1103, aID -> {
			IL.Food_Ham_Slice_Cooked               .set(addItem( aID, "Cooked Ham Slice"                         , ""                                                            , new FoodStat( 3, 1.600F,   0, C+38,  0.50F,   0,   0,   0,   0,   6, ItemUseAnimation.EAT, null                                 , F, T, F, T), TC.stack(TC.CORPUS, 1), new OreDictItemData(MT.MeatCooked, U2))); Sandwiches.INGREDIENTS.put(last(), (byte)27);
		RM.add_smelting(IL.Food_Ham_Slice_Raw.get(1), IL.Food_Ham_Slice_Cooked.get(1), F, T, F);
		CR.shaped(IL.Food_Ham_Slice_Raw.get(4), CR.DEF_NCC, "kX", 'X', "foodHamraw");
		CR.shaped(IL.Food_Ham_Slice_Cooked.get(4), CR.DEF_NCC, "kX", 'X', "foodHamcooked");
		});
		
		
		aItems.add(1112, aID -> {IL.Food_Bacon_Raw                      .set(addItem( aID, "Raw Bacon"                                , "Dropped by Pigs and Boars"       , "foodBaconraw"            , new FoodStat( 1, 0.900F,   0, C+37,  0.10F,   0,   0,   0,   0,   6, ItemUseAnimation.EAT, null                                 , F, T, F, T), TC.stack(TC.CORPUS, 1), new OreDictItemData(MT.MeatRaw, U2))); Sandwiches.INGREDIENTS.put(last(), (byte)37);});
		aItems.add(1113, aID -> {
			IL.Food_Bacon_Cooked                   .set(addItem( aID, "Grilled Bacon"                            , ""                                , "foodBaconcooked"         , new FoodStat( 3, 1.800F,   0, C+38,  0.50F,   0,   0,   0,   0,   6, ItemUseAnimation.EAT, null                                 , F, T, F, T), TC.stack(TC.CORPUS, 1), new OreDictItemData(MT.MeatCooked, U2))); Sandwiches.INGREDIENTS.put(last(), (byte)38);
		RM.add_smelting(IL.Food_Bacon_Raw.get(1), IL.Food_Bacon_Cooked.get(1), F, T, F);
		});
		
		
		aItems.add(1200, aID -> {IL.Food_Rib_Raw                        .set(addItem( aID, "Raw Ribs"                                 , "Dropped by large Animals"        , "foodRibraw"              , new FoodStat( 3, 0.600F,   0, C+37,  0.10F,   0,   0,   0,   0,  24, ItemUseAnimation.EAT, null                                 , F, T, F, T), TC.stack(TC.FAMES, 1), TC.stack(TC.CORPUS, 2), new OreDictItemData(MT.MeatRaw, U*2, MT.Bone, U), Behavior_FeedDog.INSTANCE)); Sandwiches.INGREDIENTS.put(last(), (byte)29);});
		aItems.add(1201, aID -> {IL.Food_Rib_Cooked                     .set(addItem( aID, "Grilled Ribs"                             , ""                                , "foodRibcooked"           , new FoodStat(10, 1.600F,   0, C+38,  0.50F,   0,   0,   0,   0,  24, ItemUseAnimation.EAT, null                                 , F, T, F, T), TC.stack(TC.FAMES, 1), TC.stack(TC.CORPUS, 2), new OreDictItemData(MT.MeatCooked, U*2, MT.Bone, U))); Sandwiches.INGREDIENTS.put(last(), (byte)30);});
		aItems.add(1202, aID -> {
			IL.Food_Rib_BBQ                        .set(addItem( aID, "Barbecue Ribs"                            , "High Quality Video Game Ribs"    , "foodRibbbq"              , new FoodStat(10, 1.600F,   0, C+38,  0.50F,   0,   0,   0,  30,  24, ItemUseAnimation.EAT, null                                 , F, T, F, T, /*heal*/6, 1, 0, 10, /*moveSpeed*/1, 200, 0, 20), TC.stack(TC.FAMES, 2), TC.stack(TC.CORPUS, 2), new OreDictItemData(MT.MeatCooked, U*2, MT.Bone, U))); Sandwiches.INGREDIENTS.put(last(), (byte)30);
		RM.add_smelting(IL.Food_Rib_Raw.get(1), IL.Food_Rib_Cooked.get(1), F, T, F);
		CR.shapeless(IL.Food_Rib_BBQ.get(1), CR.DEF_NCC, new Object[] {"foodRibcooked", "foodBarbecuesauce"});
		});
		
		
		aItems.add(1210, aID -> {IL.Food_RibEyeSteak_Raw                .set(addItem( aID, "Raw Rib Eye Steak"                        , "Dropped by large Animals"        , "foodRibeyesteakraw"      , new FoodStat( 3, 0.600F,   0, C+37,  0.10F,   0,   0,   0,   0,  24, ItemUseAnimation.EAT, null                                 , F, T, F, T), TC.stack(TC.FAMES, 1), TC.stack(TC.CORPUS, 3), new OreDictItemData(MT.MeatRaw, U*3, MT.Bone, U4), Behavior_FeedDog.INSTANCE)); Sandwiches.INGREDIENTS.put(last(), (byte)32);});
		aItems.add(1211, aID -> {
			IL.Food_RibEyeSteak_Cooked             .set(addItem( aID, "Grilled Rib Eye Steak"                    , "It is staring at you"            , "foodRibeyesteakcooked"   , new FoodStat(10, 1.600F,   0, C+38,  0.50F,   0,   0,   0,   0,  24, ItemUseAnimation.EAT, null                                 , F, T, F, T), TC.stack(TC.FAMES, 1), TC.stack(TC.CORPUS, 3), new OreDictItemData(MT.MeatCooked, U*3, MT.Bone, U4))); Sandwiches.INGREDIENTS.put(last(), (byte)33);
		RM.add_smelting(IL.Food_RibEyeSteak_Raw.get(1), IL.Food_RibEyeSteak_Cooked.get(1), F, T, F);
		});
		
		
		aItems.add(1300, aID -> {IL.Food_DogMeat_Raw                    .set(addItem( aID, "Dogmeat"                                  , "Why didn't you use [MERCY]?"     , "foodDograw"              , new FoodStat( 2, 0.600F,   0, C+37,  0.10F,   0,   0,   0,   0,  12, ItemUseAnimation.EAT, null                                 , F, T, F, T), TC.stack(TC.FAMES, 1), TC.stack(TC.BESTIA, 1), TC.stack(TC.CORPUS, 2), new OreDictItemData(MT.MeatRaw, U*2, MT.Bone, U9), Behavior_FeedDog.INSTANCE)); Sandwiches.INGREDIENTS.put(last(), (byte)29);});
		aItems.add(1301, aID -> {
			IL.Food_DogMeat_Cooked                 .set(addItem( aID, "Grilled Dogmeat"                          , "You monster!"                    , "foodDogcooked"           , new FoodStat( 8, 1.600F,   0, C+38,  0.50F,   0,   0,   0,   0,  12, ItemUseAnimation.EAT, null                                 , F, T, F, T), TC.stack(TC.FAMES, 1), TC.stack(TC.BESTIA, 1), TC.stack(TC.CORPUS, 2), new OreDictItemData(MT.MeatCooked, U*2, MT.Bone, U9))); Sandwiches.INGREDIENTS.put(last(), (byte)30);
		RM.add_smelting(IL.Food_DogMeat_Raw.get(1), IL.Food_DogMeat_Cooked.get(1), F, T, F);
		});
		
		
		aItems.add(1400, aID -> {IL.Food_Mutton_Raw                     .set(addItem( aID, "Mutton"           , "Beep Beep, I'm a Sheep, I said: Beep Beep Imma Sheep"    , "foodMuttonraw"           , new FoodStat( 2, 0.600F,   0, C+37,  0.10F,   0,   0,   0,   0,  12, ItemUseAnimation.EAT, null                                 , F, T, F, T), TC.stack(TC.FAMES, 1), TC.stack(TC.CORPUS, 3), new OreDictItemData(MT.MeatRaw, U*2, MT.Bone, U4), Behavior_FeedDog.INSTANCE)); Sandwiches.INGREDIENTS.put(last(), (byte)29);});
		aItems.add(1401, aID -> {
			IL.Food_Mutton_Cooked                  .set(addItem( aID, "Grilled Mutton"                           , ""                                , "foodMuttoncooked"        , new FoodStat( 7, 2.000F,   0, C+38,  0.50F,   0,   0,   0,   0,  12, ItemUseAnimation.EAT, null                                 , F, T, F, T), TC.stack(TC.FAMES, 1), TC.stack(TC.CORPUS, 3), new OreDictItemData(MT.MeatCooked, U*2, MT.Bone, U4))); Sandwiches.INGREDIENTS.put(last(), (byte)30);
		RM.add_smelting(IL.Food_Mutton_Raw.get(1), IL.Food_Mutton_Cooked.get(1), F, T, F);
		});
		
		
		aItems.add(1500, aID -> {IL.Food_Horse_Raw                      .set(addItem( aID, "Horse Meat"                               , ""                                , "foodHorseraw"            , new FoodStat( 2, 0.600F,   0, C+37,  0.10F,   0,   0,   0,   0,   8, ItemUseAnimation.EAT, null                                 , F, T, F, T), TC.stack(TC.FAMES, 1), TC.stack(TC.BESTIA, 1), TC.stack(TC.CORPUS, 2), new OreDictItemData(MT.MeatRaw, U*2, MT.Bone, U4), Behavior_FeedDog.INSTANCE)); Sandwiches.INGREDIENTS.put(last(), (byte)29);});
		aItems.add(1501, aID -> {
			IL.Food_Horse_Cooked                   .set(addItem( aID, "Grilled Horse Meat"                       , ""                                , "foodHorsecooked"         , new FoodStat( 8, 1.600F,   0, C+38,  0.50F,   0,   0,   0,   0,   8, ItemUseAnimation.EAT, null                                 , F, T, F, T), TC.stack(TC.FAMES, 1), TC.stack(TC.BESTIA, 1), TC.stack(TC.CORPUS, 2), new OreDictItemData(MT.MeatCooked, U*2, MT.Bone, U4))); Sandwiches.INGREDIENTS.put(last(), (byte)30);
		RM.add_smelting(IL.Food_Horse_Raw.get(1), IL.Food_Horse_Cooked.get(1), F, T, F);
		});
		
		aItems.add(1510, aID -> {IL.Food_Mule_Raw                       .set(addItem( aID, "Mule Meat"                                , ""                                , "foodMuleraw"             , new FoodStat( 3, 0.800F,   0, C+37,  0.10F,   0,   0,   0,   0,   8, ItemUseAnimation.EAT, null                                 , F, T, F, T), TC.stack(TC.FAMES, 1), TC.stack(TC.BESTIA, 1), TC.stack(TC.CORPUS, 2), new OreDictItemData(MT.MeatRaw, 5*U2, MT.Bone, U4), Behavior_FeedDog.INSTANCE)); Sandwiches.INGREDIENTS.put(last(), (byte)29);});
		aItems.add(1511, aID -> {
			IL.Food_Mule_Cooked                    .set(addItem( aID, "Grilled Mule Meat"                        , ""                                , "foodMulecooked"          , new FoodStat(10, 1.800F,   0, C+38,  0.50F,   0,   0,   0,   0,   8, ItemUseAnimation.EAT, null                                 , F, T, F, T), TC.stack(TC.FAMES, 1), TC.stack(TC.BESTIA, 1), TC.stack(TC.CORPUS, 2), new OreDictItemData(MT.MeatCooked, 5*U2, MT.Bone, U4))); Sandwiches.INGREDIENTS.put(last(), (byte)30);
		RM.add_smelting(IL.Food_DogMeat_Raw.get(1), IL.Food_DogMeat_Cooked.get(1), F, T, F);
		});
		
		aItems.add(1520, aID -> {IL.Food_Donkey_Raw                     .set(addItem( aID, "Donkey Meat"                              , ""                                , "foodDonkeyraw"           , new FoodStat( 2, 0.600F,   0, C+37,  0.10F,   0,   0,   0,   0,  12, ItemUseAnimation.EAT, null                                 , F, T, F, T), TC.stack(TC.FAMES, 1), TC.stack(TC.BESTIA, 1), TC.stack(TC.CORPUS, 2), new OreDictItemData(MT.MeatRaw, 5*U2, MT.Bone, U9), Behavior_FeedDog.INSTANCE)); Sandwiches.INGREDIENTS.put(last(), (byte)29);});
		aItems.add(1521, aID -> {
			IL.Food_Donkey_Cooked                  .set(addItem( aID, "Grilled Donkey Meat"                      , ""                                , "foodDonkeycooked"        , new FoodStat( 8, 1.600F,   0, C+38,  0.50F,   0,   0,   0,   0,  12, ItemUseAnimation.EAT, null                                 , F, T, F, T), TC.stack(TC.FAMES, 1), TC.stack(TC.BESTIA, 1), TC.stack(TC.CORPUS, 2), new OreDictItemData(MT.MeatCooked, 5*U2, MT.Bone, U9))); Sandwiches.INGREDIENTS.put(last(), (byte)30);
		RM.add_smelting(IL.Food_Donkey_Raw.get(1), IL.Food_Donkey_Cooked.get(1), F, T, F);
		});
		
		
		aItems.add(1998, aID -> {IL.Food_Scrap_Meat                     .set(addItem( aID, "Scrap Meat"                               , "Parts people usually don't eat"  , "foodScrapmeat"           , new FoodStat( 2, 0.600F,   0, C+37,  0.10F,   0,   0,   4,   0,  12, ItemUseAnimation.EAT, null                                 , F, T, F, T, /*hunger*/17, 300, 0, 70, /*confusion*/9, 300, 0, 10), TC.stack(TC.CORPUS, 2), new OreDictItemData(MT.MeatRaw, U*2), Behavior_FeedDog.INSTANCE)); Sandwiches.INGREDIENTS.put(last(), (byte)29);});
		
		aItems.add(32000, aID -> {IL.Food_Dough                          .set(addItem(aID, "Dough"                                , "For making Bread"                        , "foodDough"           , new FoodStat( 1, 1.000F,   0, C+37,  0.10F,   0,   0,   0,   0,   0, ItemUseAnimation.EAT, null                                 , F, T, F, T), TC.stack(TC.HERBA, 1), TC.stack(TC.FAMES, 1)));});
		aItems.add(32001, aID -> {IL.Food_Dough_Sugar                    .set(addItem(aID, "Sugary Dough"                         , "Don't eat the Dough before it is baken"  , "foodSugarDough"      , new FoodStat( 1, 1.000F,   0, C+37,  0.10F,   0,   0,   0,  20,   0, ItemUseAnimation.EAT, null                                 , F, T, F, T), TC.stack(TC.HERBA, 1), TC.stack(TC.FAMES, 1)));});
		aItems.add(32002, aID -> {IL.Food_Dough_Chocolate                .set(addItem(aID, "Chocolate Dough"                      , "I said don't eat the Dough!"             , "foodChocolateDough"  , new FoodStat( 1, 1.000F,   0, C+37,  0.10F,   0,   0,   0,  20,   0, ItemUseAnimation.EAT, null                                 , F, T, F, T), TC.stack(TC.HERBA, 1), TC.stack(TC.FAMES, 1)));});
		aItems.add(32003, aID -> {IL.Food_Dough_Egg                      .set(addItem(aID, "Egg Dough"                            , "For making Pasta"                        , "foodEggDough"        , new FoodStat( 1, 1.000F,   0, C+37,  0.10F,   0,   0,   0,  10,   0, ItemUseAnimation.EAT, null                                 , F, T, F, T), TC.stack(TC.HERBA, 1), TC.stack(TC.FAMES, 1)));});
		aItems.add(32004, aID -> {IL.Food_Dough_Sugar_Raisins            .set(addItem(aID, "Sugary Raisin Dough"                  , "Don't eat the Dough before it is baken"  , "foodSugarDough"      , new FoodStat( 1, 1.000F,   0, C+37,  0.10F,   0,   0,   0,  30,   0, ItemUseAnimation.EAT, null                                 , F, T, F, T), TC.stack(TC.HERBA, 1), TC.stack(TC.FAMES, 1)));});
		aItems.add(32005, aID -> {IL.Food_Dough_Sugar_Chocolate_Raisins  .set(addItem(aID, "Sugary Chocolate Raisin Dough"        , "Almost looks like Chocolate Chips"       , "foodSugarDough"      , new FoodStat( 1, 1.000F,   0, C+37,  0.10F,   0,   0,   0,  40,   0, ItemUseAnimation.EAT, null                                 , F, T, F, T), TC.stack(TC.HERBA, 1), TC.stack(TC.FAMES, 1)));});
		aItems.add(32006, aID -> {
			IL.Food_Dough_Abyssal                  .set(addItem(aID, "Abyssal Dough"                        , "For practicing netherlicious Bakery"     , "foodAbyssalDough"    , new FoodStat( 1, 1.000F,   0, C+37,  0.10F,   0,   0,   0,   0,   0, ItemUseAnimation.EAT, null                                 , F, T, F, T), TC.stack(TC.HERBA, 1), TC.stack(TC.FAMES, 1)));
		if (IL.NeLi_Bread.exists()) RM.add_smelting(IL.Food_Dough_Abyssal.get(1), IL.NeLi_Bread.get(1), F, T, F);
		});
		
		
		
		aItems.add(10000, aID -> {IL.Food_Chum                           .set(addItem(aID, "Chum"                                     , "Chum is Fum!"                            , "foodChum"        , new FoodStat( 5, 1.600F,   0, C+37,  0.10F,   0,   0,  20,   0,   0, ItemUseAnimation.EAT, null                                 , T, F, T, T, /*hunger*/17, 1000, 4, 100, /*confusion*/9, 300, 1, 80), TC.stack(TC.FAMES, 1), TC.stack(TC.CORPUS, 1))); Sandwiches.INGREDIENTS.put(last(), (byte)39);});
		
		
		
		aItems.add(2000, aID -> {
			IL.Food_Cookie_Raw                     .set(addItem( aID, "Cookie shaped Dough"                      , "For baking Cookies"                                          , new FoodStat( 1, 0.200F,   0, C+37,  0.10F,   0,   0,   0,   5,   0, ItemUseAnimation.EAT, null                                 , F, T, F, T), TC.stack(TC.HERBA, 1), TC.stack(TC.FAMES, 1)));
		RM.add_smelting(IL.Food_Cookie_Raw.get(1), ST.make(Items.COOKIE, 1, 0), F, T, F); FoodsGT.put(ST.make(Items.COOKIE, 1, W), 0, 0, 0,10, 0);
		RM.food_can(ST.make(Items.COOKIE, 6, W), 12, "Cookie Tin", IL.CANS_COOKIES);
		CR.shaped(IL.Food_Cookie_Raw.get(4), CR.DEF_NCC, "kX", 'X', "foodChocolateDough");
		RM.Slicer.addRecipe2(T, 16, 16, IL.Food_Dough_Chocolate.get(1), IL.Shape_Slicer_Flat.get(0), IL.Food_Cookie_Raw.get(4));
		});
		
		
		aItems.add(2002, aID -> {IL.Food_Cookie_Raisins_Raw             .set(addItem( aID, "Cookie shaped Raisin Dough"               , "For baking Raisin Cookies"                                   , new FoodStat( 1, 0.200F,   0, C+37,  0.10F,   0,   0,   0,  10,   0, ItemUseAnimation.EAT, null                                 , F, T, F, T), TC.stack(TC.HERBA, 1), TC.stack(TC.FAMES, 1)));});
		aItems.add(2003, aID -> {
			IL.Food_Cookie_Raisins                 .set(addItem( aID, "Raisin Cookie"                   , "You don't like it? I don't care! It's delicious!", "foodRaisincookies", new FoodStat( 2, 0.200F,   0, C+37,  0.10F,   0,   0,   0,  15,   0, ItemUseAnimation.EAT, null                                 , F, T, F, T), TC.stack(TC.HERBA, 1), TC.stack(TC.FAMES, 1)));
		RM.add_smelting(IL.Food_Cookie_Raisins_Raw.get(1), IL.Food_Cookie_Raisins.get(1), F, T, F);
		RM.food_can(IL.Food_Cookie_Raisins.get(6), 12, "Raisin Cookie Tin", IL.CANS_COOKIES);
		CR.shaped(IL.Food_Cookie_Raisins_Raw.get(4), CR.DEF_NCC, "kX", 'X', IL.Food_Dough_Sugar_Raisins);
		RM.Slicer.addRecipe2(T, 16, 16, IL.Food_Dough_Sugar_Raisins.get(1), IL.Shape_Slicer_Flat.get(0), IL.Food_Cookie_Raisins_Raw.get(4));
		});
		
		
		aItems.add(2004, aID -> {IL.Food_Cookie_Chocolate_Raisins_Raw   .set(addItem( aID, "Cookie shaped Chocolate Raisin Dough"     , "Almost looks like a regular Chocolate Chip Cookie >:D"       , new FoodStat( 1, 0.200F,   0, C+37,  0.10F,   0,   0,   0,  20,   0, ItemUseAnimation.EAT, null                                 , F, T, F, T), TC.stack(TC.HERBA, 1), TC.stack(TC.FAMES, 1)));});
		aItems.add(2005, aID -> {
			IL.Food_Cookie_Chocolate_Raisins       .set(addItem( aID, "Cookie"                                   , ""                                       , "foodRaisincookies", new FoodStat( 2, 0.200F,   0, C+37,  0.10F,   0,   0,   0,  25,   0, ItemUseAnimation.EAT, null                                 , F, T, F, T), TC.stack(TC.HERBA, 1), TC.stack(TC.FAMES, 1)));
		RM.add_smelting(IL.Food_Cookie_Chocolate_Raisins_Raw.get(1), IL.Food_Cookie_Chocolate_Raisins.get(1), F, T, F);
		RM.food_can(IL.Food_Cookie_Chocolate_Raisins.get(6), 12, "Chocolate Raisin Cookie Tin", IL.CANS_COOKIES);
		CR.shaped(IL.Food_Cookie_Chocolate_Raisins_Raw.get(4), CR.DEF_NCC, "kX", 'X', IL.Food_Dough_Sugar_Chocolate_Raisins);
		RM.Slicer.addRecipe2(T, 16, 16, IL.Food_Dough_Sugar_Chocolate_Raisins.get(1), IL.Shape_Slicer_Flat.get(0), IL.Food_Cookie_Chocolate_Raisins_Raw.get(4));
		});
		
		
		aItems.add(2006, aID -> {
			IL.Food_Cookie_Abyssal_Raw             .set(addItem( aID, "Cookie shaped Abyssal Dough"              , "For baking netherlicious Cookies"                            , new FoodStat( 1, 0.200F,   0, C+37,  0.10F,   0,   0,   0,   5,   0, ItemUseAnimation.EAT, null                                 , F, T, F, T), TC.stack(TC.HERBA, 1), TC.stack(TC.FAMES, 1)));
		if (IL.NeLi_Cookie.exists()) {
		RM.add_smelting(IL.Food_Cookie_Abyssal_Raw.get(1), IL.NeLi_Cookie.get(1), F, T, F); FoodsGT.put(IL.NeLi_Cookie.get(1), 0, 0, 0,10, 0);
		RM.food_can(IL.NeLi_Cookie.get(6), 12, "Abyssal Cookie Tin", IL.CANS_COOKIES);
		} else {
		RM.add_smelting(IL.Food_Cookie_Abyssal_Raw.get(1), ST.make(Items.COOKIE, 1, 0), F, T, F);
		}
		CR.shaped(IL.Food_Cookie_Abyssal_Raw.get(4), CR.DEF_NCC, "kX", 'X', "foodAbyssalDough");
		RM.Slicer.addRecipe2(T, 16, 16, IL.Food_Dough_Abyssal.get(1), IL.Shape_Slicer_Flat.get(0), IL.Food_Cookie_Abyssal_Raw.get(4));
		});
		
		
		aItems.add(3000, aID -> {IL.Food_CakeBottom_Raw                 .set(addItem( aID, "Raw Cake Bottom"                          , "For making Cake"                                             , new FoodStat( 2, 0.200F,   0, C+37,  0.10F,   0,   0,   0,  20,   0, ItemUseAnimation.EAT, null                                 , F, T, F, T), TC.stack(TC.HERBA, 1), TC.stack(TC.FAMES, 1)));});
		aItems.add(3001, aID -> {
			IL.Food_CakeBottom                     .set(addItem( aID, "Cake Bottom"                              , "I know I promised you an actual Cake, but well..."           , new FoodStat( 3, 0.200F,   0, C+37,  0.10F,   0,   0,   0,  20,   0, ItemUseAnimation.EAT, null                                 , F, T, F, T), TC.stack(TC.HERBA, 1), TC.stack(TC.FAMES, 1)));
		RM.add_smelting(IL.Food_CakeBottom_Raw.get(1), IL.Food_CakeBottom.get(1), F, T, F);
		CR.shapeless(IL.Food_CakeBottom_Raw.get(1), CR.DEF_NCC, new Object[] {"foodSugarDough", "foodSugarDough", "foodSugarDough", "foodSugarDough"});
		CR.shaped(ST.make(Items.CAKE, 1, 0), CR.DEF_NCC, "C", "Z", 'Z', IL.Food_CakeBottom, 'C', "foodHeavycream");
		CR.delate(ST.make(Items.CAKE, 1, 0));
		});
		
		
		aItems.add(4000, aID -> {IL.Food_Dough_Flat                     .set(addItem( aID, "Flattened Dough"                          , "For making Pizza"                                            , new FoodStat( 1, 0.200F,   0, C+37,  0.10F,   0,   0,   0,   0,   0, ItemUseAnimation.EAT, null                                 , F, T, F, T), TC.stack(TC.HERBA, 1), TC.stack(TC.FAMES, 1)));});
		aItems.add(4002, aID -> {
			IL.Food_Dough_Flat_Ketchup             .set(addItem( aID, "Flat Dough with Ketchup"                  , "For making Pizza"                                            , new FoodStat( 2, 0.200F,   0, C+37,  0.10F,   0,   0,   0,   0,   0, ItemUseAnimation.EAT, null                                 , F, T, F, T), TC.stack(TC.HERBA, 1), TC.stack(TC.FAMES, 1)));
		CR.shapeless(IL.Food_Dough_Flat.get(1), CR.DEF, new Object[] {"foodDough", OreDictToolNames.rollingpin});
		CR.shapeless(IL.Food_Dough_Flat_Ketchup.get(1), CR.DEF, new Object[] {"foodKetchup", IL.Food_Dough_Flat});
		CR.shapeless(IL.Food_Dough_Flat_Ketchup.get(2), CR.DEF, new Object[] {"foodKetchup", IL.Food_Dough_Flat, IL.Food_Dough_Flat});
		CR.shapeless(IL.Food_Dough_Flat_Ketchup.get(3), CR.DEF, new Object[] {"foodKetchup", IL.Food_Dough_Flat, IL.Food_Dough_Flat, IL.Food_Dough_Flat});
		CR.shapeless(IL.Food_Dough_Flat_Ketchup.get(4), CR.DEF, new Object[] {"foodKetchup", IL.Food_Dough_Flat, IL.Food_Dough_Flat, IL.Food_Dough_Flat, IL.Food_Dough_Flat});
		CR.shapeless(IL.Food_Dough_Flat_Ketchup.get(5), CR.DEF, new Object[] {"foodKetchup", IL.Food_Dough_Flat, IL.Food_Dough_Flat, IL.Food_Dough_Flat, IL.Food_Dough_Flat, IL.Food_Dough_Flat});
		});
		
		
		aItems.add(4010, aID -> {IL.Food_Pizza_Cheese_Raw               .set(addItem( aID, "Raw Pizza Margherita"                     , "Into the Oven with it!"                                      , new FoodStat( 2, 0.300F,   0, C+37,  0.10F,   0,   0,   8,   0,  12, ItemUseAnimation.EAT, null                                 , F, T, F, T), TC.stack(TC.HERBA, 1), TC.stack(TC.FAMES, 2), TC.stack(TC.IGNIS, 1)));});
		aItems.add(4011, aID -> {
			IL.Food_Pizza_Cheese                   .set(addItem( aID, "Pizza Margherita"                         , "Cheese Pizza"                                                , new FoodStat( 6, 1.200F,   0, C+38,  0.50F,   0,   0,   8,   0,  12, ItemUseAnimation.EAT, null                                 , F, T, F, T), TC.stack(TC.HERBA, 1), TC.stack(TC.FAMES, 2), TC.stack(TC.IGNIS, 1)));
		CR.shapeless(IL.Food_Pizza_Cheese_Raw.get(1), CR.DEF, new Object[] {IL.Food_Dough_Flat_Ketchup, IL.Food_Cheese_Sliced, IL.Food_Cheese_Sliced, IL.Food_Cheese_Sliced});
		RM.add_smelting(IL.Food_Pizza_Cheese_Raw.get(1), IL.Food_Pizza_Cheese.get(1), F, T, F);
		});
		
		
		aItems.add(4012, aID -> {IL.Food_Pizza_Meat_Raw                 .set(addItem( aID, "Raw Mince Meat Pizza"                     , "Into the Oven with it!"                                      , new FoodStat( 2, 0.300F,   0, C+37,  0.10F,   0,   0,   4,   0,  16, ItemUseAnimation.EAT, null                                 , F, T, F, T), TC.stack(TC.HERBA, 1), TC.stack(TC.FAMES, 1), TC.stack(TC.CORPUS, 1)));});
		aItems.add(4013, aID -> {
			IL.Food_Pizza_Meat                     .set(addItem( aID, "Mince Meat Pizza"                         , "Emo Pizza, it cuts itself!"                                  , new FoodStat( 7, 1.200F,   0, C+38,  0.50F,   0,   0,   4,   0,  16, ItemUseAnimation.EAT, null                                 , F, T, F, T), TC.stack(TC.HERBA, 1), TC.stack(TC.FAMES, 1), TC.stack(TC.CORPUS, 1)));
		CR.shapeless(IL.Food_Pizza_Meat_Raw.get(1), CR.DEF, new Object[] {IL.Food_Dough_Flat_Ketchup, OP.dust.dat(MT.MeatCooked)});
		RM.add_smelting(IL.Food_Pizza_Meat_Raw.get(1), IL.Food_Pizza_Meat.get(1), F, T, F);
		});
		
		
		aItems.add(4014, aID -> {IL.Food_Pizza_Veggie_Raw               .set(addItem( aID, "Raw Veggie Pizza"                         , "Into the Oven with it!"                                      , new FoodStat( 1, 0.300F,   5, C+35,  0.30F,   0,   0,   0,  16,   0, ItemUseAnimation.EAT, null                                 , F, T, F, T), TC.stack(TC.HERBA, 2), TC.stack(TC.FAMES, 1), TC.stack(TC.IGNIS, 1)));});
		aItems.add(4015, aID -> {
			IL.Food_Pizza_Veggie                   .set(addItem( aID, "Veggie Pizza"                             , "The next they want is Gluten Free Pizzas..."                 , new FoodStat( 5, 1.200F,   5, C+38,  0.50F,   0,   0,   0,  16,   0, ItemUseAnimation.EAT, null                                 , F, T, F, T), TC.stack(TC.HERBA, 2), TC.stack(TC.FAMES, 1), TC.stack(TC.IGNIS, 1)));
		CR.shapeless(IL.Food_Pizza_Veggie_Raw.get(1), CR.DEF, new Object[] {IL.Food_Dough_Flat_Ketchup, IL.Food_Cucumber_Sliced, IL.Food_Tomato_Sliced, IL.Food_Onion_Sliced});
		RM.add_smelting(IL.Food_Pizza_Veggie_Raw.get(1), IL.Food_Pizza_Veggie.get(1), F, T, F);
		});
		
		
		aItems.add(4016, aID -> {IL.Food_Pizza_Ananas_Raw               .set(addItem( aID, "Raw Pizza Hawaii"                         , "Did you seriously just put Pineapple on a Pizza?"            , new FoodStat( 2, 0.300F,   5, C+37,  0.10F,   0,   0,   4,   8,   8, ItemUseAnimation.EAT, null                                 , F, T, F, T), TC.stack(TC.HERBA, 1), TC.stack(TC.FAMES, 1), TC.stack(TC.ARBOR, 1)));});
		aItems.add(4017, aID -> {
			IL.Food_Pizza_Ananas                   .set(addItem( aID, "Pizza Hawaii"                             , "This is an Abomination! Who puts Pineapple on a Pizza!?"     , new FoodStat( 7, 1.200F,   5, C+38,  0.50F,   0,   0,   4,   8,   8, ItemUseAnimation.EAT, null                                 , F, T, F, T), TC.stack(TC.HERBA, 1), TC.stack(TC.FAMES, 1), TC.stack(TC.ARBOR, 1)));
		CR.shapeless(IL.Food_Pizza_Ananas_Raw.get(1), CR.DEF, new Object[] {IL.Food_Dough_Flat_Ketchup, IL.Food_Ananas_Sliced, IL.Food_Ananas_Sliced, IL.Food_Ham_Slice_Cooked, IL.Food_Cheese_Sliced});
		RM.add_smelting(IL.Food_Pizza_Ananas_Raw.get(1), IL.Food_Pizza_Ananas.get(1), F, T, F);
		});
		
		
		
		aItems.add(5000, aID -> {IL.Food_Bun_Raw                        .set(addItem( aID, "Dough (Bun)"                              , "In Bun Shape"                                                , new FoodStat( 1, 0.600F,   0, C+37,  0.10F,   0,   0,   0,   0,   0, ItemUseAnimation.EAT, null                                 , F, T, F, T), TC.stack(TC.HERBA, 1), TC.stack(TC.FAMES, 1)));});
		aItems.add(5001, aID -> {IL.Food_Bun                            .set(addItem( aID, "Bun"                                      , ""                                                            , new FoodStat( 2, 1.200F,   0, C+37,  0.10F,   0,   0,   0,   0,   0, ItemUseAnimation.EAT, null                                 , F, T, F, T), TC.stack(TC.HERBA, 1), TC.stack(TC.FAMES, 1), TC.stack(TC.IGNIS, 1)));});
		aItems.add(5002, aID -> {IL.Food_Bun_Sliced                     .set(addItem( aID, "Sliced Bun"                               , "Just half a Bun"                                             , new FoodStat( 1, 1.200F,   0, C+37,  0.10F,   0,   0,   0,   0,   0, ItemUseAnimation.EAT, null                                 , F, T, F, T), TC.stack(TC.HERBA, 1), TC.stack(TC.FAMES, 1), TC.stack(TC.IGNIS, 1)));});
		aItems.add(5003, aID -> {
			IL.Food_Buns_Sliced                    .set(addItem( aID, "Buns"                                     , "Pre Sliced"                                                  , new FoodStat( 2, 1.200F,   0, C+37,  0.10F,   0,   0,   0,   0,   0, ItemUseAnimation.EAT, null                                 , F, T, F, T), TC.stack(TC.HERBA, 1), TC.stack(TC.FAMES, 1), TC.stack(TC.IGNIS, 1)));
		RM.add_smelting(IL.Food_Bun_Raw.get(1), IL.Food_Bun.get(1), F, T, F);
		RM.food_can(IL.Food_Bun                .get(1), 2, "Canned Bread", IL.CANS_BREAD);
		RM.food_can(IL.Food_Bun_Sliced         .get(2), 2, "Canned Bread", IL.CANS_BREAD);
		RM.food_can(IL.Food_Buns_Sliced        .get(1), 2, "Canned Bread", IL.CANS_BREAD);
		CR.shaped(IL.Food_Bun_Sliced.get(2), CR.DEF_NCC, "kX", 'X', IL.Food_Bun);
		CR.shapeless(IL.Food_Bun_Raw.get(1), CR.DEF, new Object[] {"foodDough"});
		CR.shapeless(IL.Food_Buns_Sliced.get(1), CR.DEF, new Object[] {IL.Food_Bun_Sliced, IL.Food_Bun_Sliced});
		CR.shapeless(IL.Food_Bun_Sliced.get(2), CR.DEF, new Object[] {IL.Food_Buns_Sliced});
		RM.packunpack(IL.Food_Bun_Sliced.get(2), IL.Food_Buns_Sliced.get(1));
		RM.Slicer.addRecipe2(T, 16, 16, IL.Food_Bun.get(1), IL.Shape_Slicer_Split.get(0), IL.Food_Bun_Sliced.get(2));
		});

		aItems.add(5010, aID -> {IL.Food_Burger_Veggie                  .set(addItem( aID, "Veggie Burger"                            , "No matter how you call this, this is NOT a Burger!"          , new FoodStat( 4, 1.200F,   0, C+35,  0.30F,   0,   0,   0,  12,   0, ItemUseAnimation.EAT, null                                 , F, T, F, T), TC.stack(TC.HERBA, 2), TC.stack(TC.FAMES, 1)));});
		aItems.add(5011, aID -> {IL.Food_Burger_Cheese                  .set(addItem( aID, "Cheese Burger"                            , "Cheesy!"                                                     , new FoodStat( 4, 1.400F,   0, C+38,  0.50F,   0,   0,   8,   0,   8, ItemUseAnimation.EAT, null                                 , F, T, F, T), TC.stack(TC.HERBA, 1), TC.stack(TC.FAMES, 2)));});
		aItems.add(5012, aID -> {IL.Food_Burger_Meat                    .set(addItem( aID, "Hamburger"                                , "The Mc Burger Queen Burger"                                  , new FoodStat( 6, 1.600F,   0, C+38,  0.50F,   0,   0,   4,   0,  12, ItemUseAnimation.EAT, null                                 , F, T, F, T), TC.stack(TC.HERBA, 1), TC.stack(TC.FAMES, 1), TC.stack(TC.CORPUS, 1)));});
		aItems.add(5013, aID -> {IL.Food_Burger_Chum                    .set(addItem( aID, "Chum Burger"                              , "Fum is Chum!"                                                , new FoodStat( 6, 1.600F,   0, C+37,  0.10F,   0,   0,  20,   0,   0, ItemUseAnimation.EAT, null                                 , T, F, T, T, /*hunger*/17, 1000, 4, 100, /*confusion*/9, 300, 1, 80), TC.stack(TC.HERBA, 1), TC.stack(TC.FAMES, 1), TC.stack(TC.CORPUS, 1)));});
		aItems.add(5016, aID -> {IL.Food_Burger_Tofu                    .set(addItem( aID, "Tofu Burger"                              , "Just a white thingy inside Buns"                             , new FoodStat( 4, 1.400F,   0, C+37,  0.10F,   0,   0,   4,   4,   0, ItemUseAnimation.EAT, null                                 , F, T, F, T), TC.stack(TC.HERBA, 2), TC.stack(TC.FAMES, 1)));});
		aItems.add(5017, aID -> {IL.Food_Burger_Soylent                 .set(addItem( aID, "Soylent Burger"                           , "Don't forget your Soylent Salad with Soylent Cola!"          , new FoodStat( 5, 1.400F,   0, C+37,  0.10F,   0,   0,   4,   0,  12, ItemUseAnimation.EAT, null                                 , F, T, F, T), TC.stack(TC.HERBA, 1), TC.stack(TC.FAMES, 1), TC.stack(TC.CORPUS, 1)));});
		aItems.add(5018, aID -> {
			IL.Food_Burger_Fish                    .set(addItem( aID, "Fish Burger"                              , "Smells Fishy"                                                , new FoodStat( 6, 1.600F,   0, C+38,  0.50F,   0,   0,   3,   0,   8, ItemUseAnimation.EAT, null                                 , F, T, F, T), TC.stack(TC.HERBA, 1), TC.stack(TC.FAMES, 1), TC.stack(TC.CORPUS, 1)));
		CR.shapeless(IL.Food_Burger_Veggie.get(1)   , CR.DEF, new Object[] {IL.Food_Buns_Sliced, IL.Food_Cucumber_Sliced, IL.Food_Tomato_Sliced, IL.Food_Onion_Sliced});
		CR.shapeless(IL.Food_Burger_Veggie.get(1)   , CR.DEF, new Object[] {IL.Food_Bun_Sliced, IL.Food_Bun_Sliced, IL.Food_Cucumber_Sliced, IL.Food_Tomato_Sliced, IL.Food_Onion_Sliced});
		CR.shapeless(IL.Food_Burger_Cheese.get(1)   , CR.DEF, new Object[] {IL.Food_Buns_Sliced, IL.Food_Cheese_Sliced, IL.Food_Cheese_Sliced, IL.Food_Cheese_Sliced});
		CR.shapeless(IL.Food_Burger_Cheese.get(1)   , CR.DEF, new Object[] {IL.Food_Bun_Sliced, IL.Food_Bun_Sliced, IL.Food_Cheese_Sliced, IL.Food_Cheese_Sliced, IL.Food_Cheese_Sliced});
		CR.shapeless(IL.Food_Burger_Meat.get(1)     , CR.DEF, new Object[] {IL.Food_Buns_Sliced, OP.ingot.dat(MT.MeatCooked)});
		CR.shapeless(IL.Food_Burger_Meat.get(1)     , CR.DEF, new Object[] {IL.Food_Bun_Sliced, IL.Food_Bun_Sliced, OP.ingot.dat(MT.MeatCooked)});
		CR.shapeless(IL.Food_Burger_Tofu.get(1)     , CR.DEF, new Object[] {IL.Food_Buns_Sliced, OP.ingot.dat(MT.Tofu)});
		CR.shapeless(IL.Food_Burger_Tofu.get(1)     , CR.DEF, new Object[] {IL.Food_Bun_Sliced, IL.Food_Bun_Sliced, OP.ingot.dat(MT.Tofu)});
		CR.shapeless(IL.Food_Burger_Soylent.get(1)  , CR.DEF, new Object[] {IL.Food_Buns_Sliced, OP.ingot.dat(MT.SoylentGreen)});
		CR.shapeless(IL.Food_Burger_Soylent.get(1)  , CR.DEF, new Object[] {IL.Food_Bun_Sliced, IL.Food_Bun_Sliced, OP.ingot.dat(MT.SoylentGreen)});
		CR.shapeless(IL.Food_Burger_Fish.get(1)     , CR.DEF, new Object[] {IL.Food_Buns_Sliced, OP.ingot.dat(MT.FishCooked)});
		CR.shapeless(IL.Food_Burger_Fish.get(1)     , CR.DEF, new Object[] {IL.Food_Bun_Sliced, IL.Food_Bun_Sliced, OP.ingot.dat(MT.FishCooked)});
		CR.shapeless(IL.Food_Burger_Chum.get(1)     , CR.DEF, new Object[] {IL.Food_Buns_Sliced, "foodChum"});
		CR.shapeless(IL.Food_Burger_Chum.get(1)     , CR.DEF, new Object[] {IL.Food_Bun_Sliced, IL.Food_Bun_Sliced, "foodChum"});
		});
		
		
		
		aItems.add(6000, aID -> {
			IL.Food_Bread_Raw                      .set(addItem( aID, "Dough (Bread)"                            , "In Bread Shape"                                              , new FoodStat( 1, 0.600F,   0, C+37,  0.10F,   0,   0,   0,   0,   0, ItemUseAnimation.EAT, null                                 , F, T, F, T), TC.stack(TC.HERBA, 1), TC.stack(TC.FAMES, 1)));
		IL.Food_Bread                          .set(ST.make(Items.BREAD, 1, 0));
		}); // My OCD told me to do this, it is not my fault! XD                                                   , new FoodStat( 5, 1.200F,   0, C+37,  0.10F,   0,   0,   0,   0,   0, ItemUseAnimation.EAT, null                                 , F, T, F, T), TC.stack(TC.HERBA, 1), TC.stack(TC.FAMES, 1), TC.stack(TC.IGNIS, 1)));
		aItems.add(6002, aID -> {IL.Food_Bread_Sliced                   .set(addItem( aID, "Sliced Bread"                             , ""                                                            , new FoodStat( 2, 1.200F,   0, C+37,  0.10F,   0,   0,   0,   0,   0, ItemUseAnimation.EAT, null                                 , F, T, F, T), TC.stack(TC.HERBA, 1), TC.stack(TC.FAMES, 1), TC.stack(TC.IGNIS, 1)));});
		aItems.add(6003, aID -> {
			IL.Food_Breads_Sliced                  .set(addItem( aID, "Breads"                                   , "Pre Sliced"                                                  , new FoodStat( 5, 1.200F,   0, C+37,  0.10F,   0,   0,   0,   0,   0, ItemUseAnimation.EAT, null                                 , F, T, F, T), TC.stack(TC.HERBA, 1), TC.stack(TC.FAMES, 1), TC.stack(TC.IGNIS, 1)));
		RM.add_smelting(IL.Food_Bread_Raw.get(1), IL.Food_Bread.get(1), F, T, F);
		RM.food_can(IL.Food_Bread              .get(1), 4, "Canned Bread", IL.CANS_BREAD);
		RM.food_can(IL.Food_Bread_Sliced       .get(1), 2, "Canned Bread", IL.CANS_BREAD);
		RM.food_can(IL.Food_Breads_Sliced      .get(1), 4, "Canned Bread", IL.CANS_BREAD);
		CR.shaped(IL.Food_Bread_Sliced.get(2), CR.DEF_NCC, "kX", 'X', IL.Food_Bread);
		CR.shapeless(IL.Food_Bread_Raw.get(1), CR.DEF, new Object[] {"foodDough", "foodDough"});
		CR.shapeless(IL.Food_Breads_Sliced.get(1), CR.DEF, new Object[] {IL.Food_Bread_Sliced, IL.Food_Bread_Sliced});
		CR.shapeless(IL.Food_Bread_Sliced.get(2), CR.DEF, new Object[] {IL.Food_Breads_Sliced});
		RM.packunpack(IL.Food_Bread_Sliced.get(2), IL.Food_Breads_Sliced.get(1));
		RM.Slicer.addRecipe2(T, 16, 16, IL.Food_Bread.get(1), IL.Shape_Slicer_Split.get(0), IL.Food_Bread_Sliced.get(2));
		});
		
		aItems.add(6010, aID -> {IL.Food_Sandwich_Veggie                .set(addItem( aID, "Veggie Sandwich"                          , "It's Canon, Guys! Season 4, Vegan Morty!"                    , new FoodStat( 7, 1.200F,   0, C+35,  0.30F,   0,   0,   0,  24,   0, ItemUseAnimation.EAT, null                                 , F, T, F, T), TC.stack(TC.HERBA, 2), TC.stack(TC.FAMES, 1)));                                                  setFluidContainerStats(mLastID, 0, 32);});
		aItems.add(6011, aID -> {IL.Food_Sandwich_Cheese                .set(addItem( aID, "Cheese Sandwich"                          , "Say Cheese!"                                                 , new FoodStat( 7, 1.400F,   0, C+38,  0.50F,   0,   0,  16,   0,  16, ItemUseAnimation.EAT, null                                 , F, T, F, T), TC.stack(TC.HERBA, 1), TC.stack(TC.FAMES, 2)));                                                  setFluidContainerStats(mLastID, 0, 32);});
		aItems.add(6014, aID -> {IL.Food_Sandwich_Bacon                 .set(addItem( aID, "Bacon Sandwich"                           , "The best Sandwich ever!"                                     , new FoodStat(10, 1.800F,   0, C+38,  0.50F,   0,   0,  12,   0,  18, ItemUseAnimation.EAT, null                                 , F, T, F, T), TC.stack(TC.HERBA, 1), TC.stack(TC.FAMES, 1), TC.stack(TC.CORPUS, 1)));                          setFluidContainerStats(mLastID, 0, 32);});
		aItems.add(6015, aID -> {
			IL.Food_Sandwich_Steak                 .set(addItem( aID, "Steak Sandwich"                           , "Not a 'Steam Sandwich'"                                      , new FoodStat(10, 1.600F,   0, C+38,  0.50F,   0,   0,   8,   0,  16, ItemUseAnimation.EAT, null                                 , F, T, F, T), TC.stack(TC.HERBA, 1), TC.stack(TC.FAMES, 1), TC.stack(TC.CORPUS, 1)));                          setFluidContainerStats(mLastID, 0, 32);
		CR.shapeless(IL.Food_Sandwich_Veggie.get(1) , CR.DEF, new Object[] {IL.Food_Breads_Sliced, IL.Food_Cucumber_Sliced, IL.Food_Cucumber_Sliced, IL.Food_Tomato_Sliced, IL.Food_Tomato_Sliced, IL.Food_Onion_Sliced});
		CR.shapeless(IL.Food_Sandwich_Veggie.get(1) , CR.DEF, new Object[] {IL.Food_Bread_Sliced, IL.Food_Bread_Sliced, IL.Food_Cucumber_Sliced, IL.Food_Cucumber_Sliced, IL.Food_Tomato_Sliced, IL.Food_Tomato_Sliced, IL.Food_Onion_Sliced});
		CR.shapeless(IL.Food_Sandwich_Cheese.get(1) , CR.DEF, new Object[] {IL.Food_Breads_Sliced, IL.Food_Cheese_Sliced, IL.Food_Cheese_Sliced, IL.Food_Cheese_Sliced, IL.Food_Cheese_Sliced, IL.Food_Cheese_Sliced});
		CR.shapeless(IL.Food_Sandwich_Cheese.get(1) , CR.DEF, new Object[] {IL.Food_Bread_Sliced, IL.Food_Bread_Sliced, IL.Food_Cheese_Sliced, IL.Food_Cheese_Sliced, IL.Food_Cheese_Sliced, IL.Food_Cheese_Sliced, IL.Food_Cheese_Sliced});
		CR.shapeless(IL.Food_Sandwich_Bacon.get(1)  , CR.DEF, new Object[] {IL.Food_Breads_Sliced, "foodBaconcooked", "foodBaconcooked", "foodBaconcooked"});
		CR.shapeless(IL.Food_Sandwich_Bacon.get(1)  , CR.DEF, new Object[] {IL.Food_Bread_Sliced, IL.Food_Bread_Sliced, "foodBaconcooked", "foodBaconcooked", "foodBaconcooked"});
		CR.shapeless(IL.Food_Sandwich_Steak.get(1)  , CR.DEF, new Object[] {IL.Food_Breads_Sliced, Items.COOKED_BEEF});
		CR.shapeless(IL.Food_Sandwich_Steak.get(1)  , CR.DEF, new Object[] {IL.Food_Bread_Sliced, IL.Food_Bread_Sliced, Items.COOKED_BEEF});
		});
		
		
		
		aItems.add(7000, aID -> {IL.Food_Baguette_Raw                   .set(addItem( aID, "Dough (Baguette)"                         , "In Baguette Shape"                                           , new FoodStat( 1, 0.600F,   0, C+37,  0.10F,   0,   0,   0,   0,   0, ItemUseAnimation.EAT, null                                 , F, T, F, T), TC.stack(TC.HERBA, 1), TC.stack(TC.FAMES, 1)));});
		aItems.add(7001, aID -> {IL.Food_Baguette                       .set(addItem( aID, "Baguette"                                 , "I teleported nothing BUT Bread!!!"                           , new FoodStat( 8, 1.200F,   0, C+37,  0.10F,   0,   0,   0,   0,   0, ItemUseAnimation.EAT, null                                 , F, T, F, T), TC.stack(TC.HERBA, 1), TC.stack(TC.FAMES, 1), TC.stack(TC.IGNIS, 1)));});
		aItems.add(7002, aID -> {IL.Food_Baguette_Sliced                .set(addItem( aID, "Sliced Baguette"                          , "Just half a Baguette"                                        , new FoodStat( 4, 1.200F,   0, C+37,  0.10F,   0,   0,   0,   0,   0, ItemUseAnimation.EAT, null                                 , F, T, F, T), TC.stack(TC.HERBA, 1), TC.stack(TC.FAMES, 1), TC.stack(TC.IGNIS, 1)));});
		aItems.add(7003, aID -> {
			IL.Food_Baguettes_Sliced               .set(addItem( aID, "Baguettes"                                , "Pre Sliced"                                                  , new FoodStat( 8, 1.200F,   0, C+37,  0.10F,   0,   0,   0,   0,   0, ItemUseAnimation.EAT, null                                 , F, T, F, T), TC.stack(TC.HERBA, 1), TC.stack(TC.FAMES, 1), TC.stack(TC.IGNIS, 1)));
		RM.add_smelting(IL.Food_Baguette_Raw.get(1), IL.Food_Baguette.get(1), F, T, F);
		RM.food_can(IL.Food_Baguette           .get(1), 8, "Canned Pain", IL.CANS_BREAD);
		RM.food_can(IL.Food_Baguette_Sliced    .get(1), 4, "Canned Pain", IL.CANS_BREAD);
		RM.food_can(IL.Food_Baguettes_Sliced   .get(1), 8, "Canned Pain", IL.CANS_BREAD);
		CR.shaped(IL.Food_Baguette_Sliced.get(2), CR.DEF_NCC, "kX", 'X', IL.Food_Baguette);
		CR.shapeless(IL.Food_Baguette_Raw.get(1), CR.DEF, new Object[] {"foodDough", "foodDough", "foodDough"});
		CR.shapeless(IL.Food_Baguettes_Sliced.get(1), CR.DEF, new Object[] {IL.Food_Baguette_Sliced, IL.Food_Baguette_Sliced});
		CR.shapeless(IL.Food_Baguette_Sliced.get(2), CR.DEF, new Object[] {IL.Food_Baguettes_Sliced});
		RM.packunpack(IL.Food_Baguette_Sliced.get(2), IL.Food_Baguette_Sliced.get(1));
		RM.Slicer.addRecipe2(T, 16, 16, IL.Food_Baguette.get(1), IL.Shape_Slicer_Split.get(0), IL.Food_Baguette_Sliced.get(2));
		});
		
		aItems.add(7010, aID -> {IL.Food_Large_Sandwich_Veggie          .set(addItem( aID, "Large Veggie Sandwich"                    , "Meatless"                                                    , new FoodStat(15, 2.200F,   0, C+35,  0.30F,   0,   0,   0,  36,   0, ItemUseAnimation.EAT, null                                 , F, T, F, T), TC.stack(TC.HERBA, 3), TC.stack(TC.FAMES, 1)));                                                  setFluidContainerStats(mLastID, 0, 16);});
		aItems.add(7011, aID -> {IL.Food_Large_Sandwich_Cheese          .set(addItem( aID, "Large Cheese Sandwich"                    , "I need another cheesy tooltip for this"                      , new FoodStat(15, 2.400F,   0, C+38,  0.50F,   0,   0,  24,   0,  24, ItemUseAnimation.EAT, null                                 , F, T, F, T), TC.stack(TC.HERBA, 1), TC.stack(TC.FAMES, 3)));                                                  setFluidContainerStats(mLastID, 0, 16);});
		aItems.add(7014, aID -> {IL.Food_Large_Sandwich_Bacon           .set(addItem( aID, "Large Bacon Sandwich"                     , "For Men! (and manly Women)"                                  , new FoodStat(20, 2.800F,   0, C+38,  0.50F,   0,   0,  16,   0,  36, ItemUseAnimation.EAT, null                                 , F, T, F, T), TC.stack(TC.HERBA, 2), TC.stack(TC.FAMES, 1), TC.stack(TC.CORPUS, 2)));                          setFluidContainerStats(mLastID, 0, 16);});
		aItems.add(7015, aID -> {
			IL.Food_Large_Sandwich_Steak           .set(addItem( aID, "Large Steak Sandwich"                     , "Yes, I once accidentially called it 'Steam Sandwich'"        , new FoodStat(20, 2.600F,   0, C+38,  0.50F,   0,   0,  12,   0,  32, ItemUseAnimation.EAT, null                                 , F, T, F, T), TC.stack(TC.HERBA, 2), TC.stack(TC.FAMES, 1), TC.stack(TC.CORPUS, 2)));                          setFluidContainerStats(mLastID, 0, 16);
		CR.shapeless(IL.Food_Large_Sandwich_Veggie.get(1)   , CR.DEF, new Object[] {IL.Food_Baguettes_Sliced, IL.Food_Cucumber_Sliced, IL.Food_Cucumber_Sliced, IL.Food_Cucumber_Sliced, IL.Food_Tomato_Sliced, IL.Food_Tomato_Sliced, IL.Food_Tomato_Sliced, IL.Food_Onion_Sliced});
		CR.shapeless(IL.Food_Large_Sandwich_Veggie.get(1)   , CR.DEF, new Object[] {IL.Food_Baguette_Sliced, IL.Food_Baguette_Sliced, IL.Food_Cucumber_Sliced, IL.Food_Cucumber_Sliced, IL.Food_Cucumber_Sliced, IL.Food_Tomato_Sliced, IL.Food_Tomato_Sliced, IL.Food_Tomato_Sliced, IL.Food_Onion_Sliced});
		CR.shapeless(IL.Food_Large_Sandwich_Cheese.get(1)   , CR.DEF, new Object[] {IL.Food_Baguettes_Sliced, IL.Food_Cheese_Sliced, IL.Food_Cheese_Sliced, IL.Food_Cheese_Sliced, IL.Food_Cheese_Sliced, IL.Food_Cheese_Sliced, IL.Food_Cheese_Sliced, IL.Food_Cheese_Sliced});
		CR.shapeless(IL.Food_Large_Sandwich_Cheese.get(1)   , CR.DEF, new Object[] {IL.Food_Baguette_Sliced, IL.Food_Baguette_Sliced, IL.Food_Cheese_Sliced, IL.Food_Cheese_Sliced, IL.Food_Cheese_Sliced, IL.Food_Cheese_Sliced, IL.Food_Cheese_Sliced, IL.Food_Cheese_Sliced, IL.Food_Cheese_Sliced});
		CR.shapeless(IL.Food_Large_Sandwich_Bacon.get(1)    , CR.DEF, new Object[] {IL.Food_Baguettes_Sliced, "foodBaconcooked", "foodBaconcooked", "foodBaconcooked", "foodBaconcooked", "foodBaconcooked", "foodBaconcooked"});
		CR.shapeless(IL.Food_Large_Sandwich_Bacon.get(1)    , CR.DEF, new Object[] {IL.Food_Baguette_Sliced, IL.Food_Baguette_Sliced, "foodBaconcooked", "foodBaconcooked", "foodBaconcooked", "foodBaconcooked", "foodBaconcooked", "foodBaconcooked"});
		CR.shapeless(IL.Food_Large_Sandwich_Steak.get(1)    , CR.DEF, new Object[] {IL.Food_Baguettes_Sliced, Items.COOKED_BEEF, Items.COOKED_BEEF});
		CR.shapeless(IL.Food_Large_Sandwich_Steak.get(1)    , CR.DEF, new Object[] {IL.Food_Baguette_Sliced, IL.Food_Baguette_Sliced, Items.COOKED_BEEF, Items.COOKED_BEEF});
		});
		
		
		
		aItems.add(14000, aID -> {IL.Food_Toast_Raw                       .set(addItem(aID, "Dough (Toast Loaf)"                       , "Shape of a Toast Loaf"                                       , new FoodStat( 1, 0.600F,   0, C+37,  0.10F,   0,   0,   0,   0,   0, ItemUseAnimation.EAT, null                                 , F, T, F, T), TC.stack(TC.HERBA, 1), TC.stack(TC.FAMES, 1)));});
		aItems.add(14001, aID -> {IL.Food_Toast                           .set(addItem(aID, "Loaf of Toast"                            , "Do not teleport Bread!"                                      , new FoodStat( 8, 1.200F,   0, C+37,  0.10F,   0,   0,   0,   0,   0, ItemUseAnimation.EAT, null                                 , F, T, F, T), TC.stack(TC.HERBA, 1), TC.stack(TC.FAMES, 1), TC.stack(TC.IGNIS, 1)));});
		aItems.add(14002, aID -> {IL.Food_Toast_Sliced                    .set(addItem(aID, "Toast"                                    , "Best thing since sliced Bread, oh wait..."                   , new FoodStat( 1, 1.200F,   0, C+37,  0.10F,   0,   0,   0,   0,   0, ItemUseAnimation.EAT, null                                 , F, T, F, T), TC.stack(TC.HERBA, 1), TC.stack(TC.FAMES, 1), TC.stack(TC.IGNIS, 1))); Sandwiches.INGREDIENTS.put(last(), (byte)254);});
		aItems.add(14003, aID -> {
			IL.Food_Toasted_Sliced                  .set(addItem(aID, "Toasted Toast"                            , ""                                                            , new FoodStat( 1, 1.200F,   0, C+39,  0.10F,   0,   0,   0,   0,   0, ItemUseAnimation.EAT, null                                 , F, T, F, T), TC.stack(TC.HERBA, 1), TC.stack(TC.FAMES, 1), TC.stack(TC.IGNIS, 2))); Sandwiches.INGREDIENTS.put(last(), (byte)253);
		RM.add_smelting(IL.Food_Toast_Raw.get(1), IL.Food_Toast.get(1), F, T, F);
		RM.add_smelting(IL.Food_Toast_Sliced.get(1), IL.Food_Toasted_Sliced.get(1), F, T, F);
		RM.food_can(IL.Food_Toast_Raw           .get(1), 8, "Canned Bread", IL.CANS_BREAD);
		RM.food_can(IL.Food_Toast               .get(1), 8, "Canned Bread", IL.CANS_BREAD);
		RM.food_can(IL.Food_Toast_Sliced        .get(1), 1, "Canned Bread", IL.CANS_BREAD);
		CR.shaped(IL.Food_Toast_Sliced.get(8), CR.DEF_NCC, "kX", 'X', IL.Food_Toast);
		CR.shapeless(IL.Food_Toast_Raw.get(1), CR.DEF, new Object[] {"foodDough", "foodDough", "foodDough", "foodDough"});
		RM.Slicer.addRecipe2(T, 16, 16, IL.Food_Toast.get(1), IL.Shape_Slicer_Flat.get(0), IL.Food_Toast_Sliced.get(8));
		
		
		
		
		
		
		
		FoodsGT.put(ST.make(Items.MUSHROOM_STEW     , 1, W), 0,10, 0, 5, 0);
		});
		
		
		
		
		aItems.add(10010, aID -> {
			IL.Food_Chum_On_Stick                  .set(addItem(aID, "Chum on a Stick"                          , "Don't forget to try our Chum-balaya"                         , new FoodStat( 5, 1.600F,   0, C+37,  0.10F,   0,   0,  20,   0,   0, ItemUseAnimation.EAT, IL.Stick.get(1)                      , T, F, T, T, PotionsGT.ID_STICKY, 300, 0, 90, /*hunger*/17, 1000, 4, 100, /*confusion*/9, 300, 1, 80), TC.stack(TC.FAMES, 1), TC.stack(TC.CORPUS, 1), new OreDictItemData(ANY.Wood, U2)));
		CR.shapeless(IL.Food_Chum_On_Stick.get(1), CR.DEF_NCC, new Object[] {OP.stick.dat(ANY.Wood), "foodChum"});
		});
		
		
		
		aItems.add(11000, aID -> {
			IL.Food_Dough_Egg_Flat                 .set(addItem(aID, "Flattened Egg Dough"                      , "For making Pasta"                                            , new FoodStat( 1, 0.600F,   0, C+37,  0.10F,   0,   0,   0,   5,   5, ItemUseAnimation.EAT, null                                 , F, T, F, T), TC.stack(TC.HERBA, 1), TC.stack(TC.FAMES, 1)));
		CR.shapeless(IL.Food_Dough_Egg_Flat.get(1), CR.DEF, new Object[] {IL.Food_Dough_Egg, OreDictToolNames.rollingpin});
		RM.RollingMill.addRecipe1(T, 16,   16, IL.Food_Dough_Egg.get(1), IL.Food_Dough_Egg_Flat.get(1));
		});



		aItems.add(13000, aID -> {IL.Food_Ice_Cream                      .set(addItem(aID, "Ice Cream"                                , "Basic Milk Gelato"                               , "foodIcecream"                    , new FoodStat( 1, 0.600F,   0, C+35,  0.50F,   0,   0,   5,  10,   0, ItemUseAnimation.EAT, null                                 , F, T, F, T), TC.stack(TC.GELUM, 1), TC.stack(TC.FAMES, 1)));});
		aItems.add(13001, aID -> {IL.Food_Ice_Cream_Stracciatella        .set(addItem(aID, "Stracciatella Ice Cream"                  , "Fine Chocolate Chip Gelato"                      , "foodStracciatellaicecream"       , new FoodStat( 1, 0.600F,   0, C+35,  0.50F,   0,   0,   5,  20,   0, ItemUseAnimation.EAT, null                                 , F, T, F, T), TC.stack(TC.GELUM, 1), TC.stack(TC.FAMES, 1)));});
		aItems.add(13002, aID -> {IL.Food_Ice_Cream_Raisin               .set(addItem(aID, "Raisin Ice Cream"                         , "Dry and Grapey Gelato"                           , "foodRaisinicecream"              , new FoodStat( 1, 0.600F,   0, C+35,  0.50F,   0,   0,   5,  20,   0, ItemUseAnimation.EAT, null                                 , F, T, F, T), TC.stack(TC.GELUM, 1), TC.stack(TC.FAMES, 1)));});
		aItems.add(13003, aID -> {IL.Food_Ice_Cream_Vanilla              .set(addItem(aID, "Vanilla Ice Cream"                        , "Standard Vanilla Gelato"                         , "foodVanillaicecream"             , new FoodStat( 1, 0.600F,   0, C+35,  0.50F,   0,   0,   5,  10,   0, ItemUseAnimation.EAT, null                                 , F, T, F, T), TC.stack(TC.GELUM, 1), TC.stack(TC.FAMES, 1)));});
		aItems.add(13004, aID -> {IL.Food_Ice_Cream_Chocolate            .set(addItem(aID, "Chocolate Ice Cream"                      , "Standard Chocolate Gelato"                       , "foodChocolateicecream"           , new FoodStat( 1, 0.600F,   0, C+35,  0.50F,   0,   0,   5,  20,   0, ItemUseAnimation.EAT, null                                 , F, T, F, T), TC.stack(TC.GELUM, 1), TC.stack(TC.FAMES, 1)));});
		aItems.add(13005, aID -> {IL.Food_Ice_Cream_Mocha                .set(addItem(aID, "Mocha Ice Cream"                          , "Coffee based Gelato"                             , "foodMochaicecream"               , new FoodStat( 1, 0.600F,   0, C+35,  0.50F,   0,  20,   5,  10,   0, ItemUseAnimation.EAT, null                                 , F, T, F, T), TC.stack(TC.GELUM, 1), TC.stack(TC.FAMES, 1)));});
		aItems.add(13006, aID -> {IL.Food_Ice_Cream_Caramel              .set(addItem(aID, "Caramel Ice Cream"                        , "Sugary Gelato"                                   , "foodCaramelicecream"             , new FoodStat( 1, 0.600F,   0, C+35,  0.50F,   0,   0,   5,  30,   0, ItemUseAnimation.EAT, null                                 , F, T, F, T), TC.stack(TC.GELUM, 1), TC.stack(TC.FAMES, 1)));});
		aItems.add(13007, aID -> {IL.Food_Ice_Cream_Mint                 .set(addItem(aID, "Mint Ice Cream"                           , "Fresh smelling Gelato"                           , "foodMinticecream"                , new FoodStat( 1, 0.600F,   0, C+35,  0.50F,   0,   0,   5,  10,   0, ItemUseAnimation.EAT, null                                 , F, T, F, T), TC.stack(TC.GELUM, 1), TC.stack(TC.FAMES, 1)));});
		aItems.add(13008, aID -> {IL.Food_Ice_Cream_Mint_Chocolate_Chip  .set(addItem(aID, "Mint Chocolate Chip Ice Cream"            , "Minty Chocolate Chip Gelato"                     , "foodMintchocolatechipicecream"   , new FoodStat( 1, 0.600F,   0, C+35,  0.50F,   0,   0,   5,  20,   0, ItemUseAnimation.EAT, null                                 , F, T, F, T), TC.stack(TC.GELUM, 1), TC.stack(TC.FAMES, 1)));});
		aItems.add(13009, aID -> {IL.Food_Ice_Cream_Strawberry           .set(addItem(aID, "Strawberry Ice Cream"                     , "Fruity Red Gelato"                               , "foodStrawberryicecream"          , new FoodStat( 1, 0.600F,   5, C+35,  0.50F,   0,   0,   5,  15,   0, ItemUseAnimation.EAT, null                                 , F, T, F, T), TC.stack(TC.GELUM, 1), TC.stack(TC.FAMES, 1)));});
		aItems.add(13010, aID -> {IL.Food_Ice_Cream_Cherry               .set(addItem(aID, "Cherry Ice Cream"                         , "Fruity Red Gelato"                               , "foodCherryicecream"              , new FoodStat( 1, 0.600F,   5, C+35,  0.50F,   0,   0,   5,  15,   0, ItemUseAnimation.EAT, null                                 , F, T, F, T), TC.stack(TC.GELUM, 1), TC.stack(TC.FAMES, 1)));});
		aItems.add(13011, aID -> {IL.Food_Ice_Cream_Blueberry            .set(addItem(aID, "Blueberry Ice Cream"                      , "Smurfy Blue Gelato"                              , "foodBlueberryicecream"           , new FoodStat( 1, 0.600F,   5, C+35,  0.50F,   0,   0,   5,  15,   0, ItemUseAnimation.EAT, null                                 , F, T, F, T), TC.stack(TC.GELUM, 1), TC.stack(TC.FAMES, 1)));});
		aItems.add(13012, aID -> {IL.Food_Ice_Cream_Currant              .set(addItem(aID, "Currant Ice Cream"                        , "Most Current Gelato"                             , "foodCurranticecream"             , new FoodStat( 1, 0.600F,   5, C+35,  0.50F,   0,   0,   5,  15,   0, ItemUseAnimation.EAT, null                                 , F, T, F, T), TC.stack(TC.GELUM, 1), TC.stack(TC.FAMES, 1)));});
		aItems.add(13013, aID -> {IL.Food_Ice_Cream_Blackberry           .set(addItem(aID, "Blackberry Ice Cream"                     , "Fruity Dark Gelato"                              , "foodBlackberryicecream"          , new FoodStat( 1, 0.600F,   5, C+35,  0.50F,   0,   0,   5,  15,   0, ItemUseAnimation.EAT, null                                 , F, T, F, T), TC.stack(TC.GELUM, 1), TC.stack(TC.FAMES, 1)));});
		aItems.add(13014, aID -> {IL.Food_Ice_Cream_Raspberry            .set(addItem(aID, "Raspberry Ice Cream"                      , "Fruity Red Gelato"                               , "foodRaspberryicecream"           , new FoodStat( 1, 0.600F,   5, C+35,  0.50F,   0,   0,   5,  15,   0, ItemUseAnimation.EAT, null                                 , F, T, F, T), TC.stack(TC.GELUM, 1), TC.stack(TC.FAMES, 1)));});
		aItems.add(13015, aID -> {IL.Food_Ice_Cream_Cranberry            .set(addItem(aID, "Cranberry Ice Cream"                      , "Fruity Red Gelato"                               , "foodCranberryicecream"           , new FoodStat( 1, 0.600F,   5, C+35,  0.50F,   0,   0,   5,  15,   0, ItemUseAnimation.EAT, null                                 , F, T, F, T), TC.stack(TC.GELUM, 1), TC.stack(TC.FAMES, 1)));});
		aItems.add(13016, aID -> {IL.Food_Ice_Cream_Gooseberry           .set(addItem(aID, "Gooseberry Ice Cream"                     , "Fruity Yellow Gelato"                            , "foodGooseberryicecream"          , new FoodStat( 1, 0.600F,   5, C+35,  0.50F,   0,   0,   5,  15,   0, ItemUseAnimation.EAT, null                                 , F, T, F, T), TC.stack(TC.GELUM, 1), TC.stack(TC.FAMES, 1)));});
		aItems.add(13017, aID -> {IL.Food_Ice_Cream_Nutella              .set(addItem(aID, "Nutella Ice Cream"                        , "Perfectly flavoured Gelato"                      , "foodNutellaicecream"             , new FoodStat( 1, 0.600F,   0, C+35,  0.50F,   0,   0,   5,  15,  15, ItemUseAnimation.EAT, null                                 , F, T, F, T), TC.stack(TC.GELUM, 1), TC.stack(TC.FAMES, 1)));});
		aItems.add(13018, aID -> {IL.Food_Ice_Cream_Lemon                .set(addItem(aID, "Lemon Ice Cream"                          , "Sour Yellow Gelato"                              , "foodLemonicecream"               , new FoodStat( 1, 0.600F,   5, C+35,  0.50F,   0,   0,   5,  15,   0, ItemUseAnimation.EAT, null                                 , F, T, F, T), TC.stack(TC.GELUM, 1), TC.stack(TC.FAMES, 1)));});
		aItems.add(13019, aID -> {IL.Food_Ice_Cream_Maple                .set(addItem(aID, "Maple Ice Cream"                          , "Maple flavoured Gelato"                          , "foodMapleicecream"               , new FoodStat( 1, 0.600F,   0, C+35,  0.50F,   0,   0,   5,  30,   0, ItemUseAnimation.EAT, null                                 , F, T, F, T), TC.stack(TC.GELUM, 1), TC.stack(TC.FAMES, 1)));});
		aItems.add(13020, aID -> {IL.Food_Ice_Cream_Pistachio            .set(addItem(aID, "Pistachio Ice Cream"                      , "Nut flavoured Gelato"                            , "foodPistachioicecream"           , new FoodStat( 1, 0.600F,   0, C+35,  0.50F,   0,   0,   5,  10,  10, ItemUseAnimation.EAT, null                                 , F, T, F, T), TC.stack(TC.GELUM, 1), TC.stack(TC.FAMES, 1)));});
		aItems.add(13021, aID -> {IL.Food_Ice_Cream_Bacon                .set(addItem(aID, "Bacon Ice Cream"                          , "Manly Gelato"                                    , "foodBaconicecream"               , new FoodStat( 1, 0.600F,   0, C+35,  0.50F,   0,   0,   5,  20,   0, ItemUseAnimation.EAT, null                                 , F, T, F, T), TC.stack(TC.GELUM, 1), TC.stack(TC.CORPUS, 1)));});
		aItems.add(13022, aID -> {IL.Food_Ice_Cream_Kiwi                 .set(addItem(aID, "Kiwi Ice Cream"                           , "Cute Green Gelato"                               , "foodKiwiicecream"                , new FoodStat( 1, 0.600F,   5, C+35,  0.50F,   0,   0,   5,  15,   0, ItemUseAnimation.EAT, null                                 , F, T, F, T), TC.stack(TC.GELUM, 1), TC.stack(TC.FAMES, 1)));});
		aItems.add(13023, aID -> {IL.Food_Ice_Cream_Melon                .set(addItem(aID, "Melon Ice Cream"                          , "Watery Red Gelato"                               , "foodMelonicecream"               , new FoodStat( 1, 0.600F,  10, C+35,  0.50F,   0,   0,   5,  15,   0, ItemUseAnimation.EAT, null                                 , F, T, F, T), TC.stack(TC.GELUM, 1), TC.stack(TC.FAMES, 1)));});
		aItems.add(13024, aID -> {IL.Food_Ice_Cream_Banana               .set(addItem(aID, "Banana Ice Cream"                         , "Minion Gelato"                                   , "foodBananaicecream"              , new FoodStat( 1, 0.600F,   5, C+35,  0.50F,   0,   0,   5,  15, 0,2, ItemUseAnimation.EAT, null                                 , F, T, F, T), TC.stack(TC.GELUM, 1), TC.stack(TC.FAMES, 1)));});
		aItems.add(13025, aID -> {IL.Food_Ice_Cream_Grape                .set(addItem(aID, "Grape Ice Cream"                          , "Watery Green Gelato"                             , "foodGrapeicecream"               , new FoodStat( 1, 0.600F,  10, C+35,  0.50F,   0,   0,   5,  20,   0, ItemUseAnimation.EAT, null                                 , F, T, F, T), TC.stack(TC.GELUM, 1), TC.stack(TC.FAMES, 1)));});
		aItems.add(13026, aID -> {IL.Food_Ice_Cream_Apple                .set(addItem(aID, "Apple Ice Cream"                          , "Fruity Yellow Gelato"                            , "foodAppleicecream"               , new FoodStat( 1, 0.600F,   5, C+35,  0.50F,   0,   0,   5,  15,   0, ItemUseAnimation.EAT, null                                 , F, T, F, T), TC.stack(TC.GELUM, 1), TC.stack(TC.FAMES, 1)));});
		aItems.add(13027, aID -> {IL.Food_Ice_Cream_Peanut_Butter        .set(addItem(aID, "Peanut Butter Ice Cream"                  , "It's Peanut Butter Ice Cream Time!"              , "foodPeanutbuttericecream"        , new FoodStat( 1, 0.600F,   0, C+35,  0.50F,   0,   0,   5,  15,  15, ItemUseAnimation.EAT, null                                 , F, T, F, T), TC.stack(TC.GELUM, 1), TC.stack(TC.FAMES, 1)));});
		aItems.add(13028, aID -> {IL.Food_Ice_Cream_Ananas               .set(addItem(aID, "Ananas Ice Cream"                         , "Pineapple Gelato"                                , "foodPineappleicecream"           , new FoodStat( 1, 0.600F,   5, C+35,  0.50F,   0,   0,   5,  15,   0, ItemUseAnimation.EAT, null                                 , F, T, F, T), TC.stack(TC.GELUM, 1), TC.stack(TC.ARBOR, 1)));});
		aItems.add(13029, aID -> {IL.Food_Ice_Cream_Chum                 .set(addItem(aID, "Chum Ice Cream"                           , "Fum is Fum!"                                     , "foodChumicecream"                , new FoodStat( 1, 0.600F,   0, C+35,  0.50F,   0,   0,  20,  10,   0, ItemUseAnimation.EAT, null                                 , T, F, T, T, /*hunger*/17, 250, 4, 100, /*confusion*/9, 100, 1, 80), TC.stack(TC.GELUM, 1), TC.stack(TC.CORPUS, 1)));});
		aItems.add(13030, aID -> {IL.Food_Ice_Cream_Honey                .set(addItem(aID, "Honey Ice Cream"                          , "Bee the Gelato"                                  , "foodHoneyicecream"               , new FoodStat( 1, 0.600F,   5, C+35,  0.50F,   0,   0,   5,  25,   0, ItemUseAnimation.EAT, null                                 , F, T, F, T), TC.stack(TC.GELUM, 1), TC.stack(TC.BESTIA, 1)));});
		aItems.add(13989, aID -> {IL.Food_Ice_Cream_Bear                 .set(addItem(aID, "Bear Ice Cream"                           , "Gelato for Bears!"                               , "foodBearicecream"                , new FoodStat( 2, 1.200F,   0, C+35,  0.50F,   0,   0,   5,  10,   5, ItemUseAnimation.EAT, null                                 , F, T, F, T), TC.stack(TC.GELUM, 1), TC.stack(TC.BESTIA, 1)));});
		aItems.add(13995, aID -> {IL.Food_Ice_Cream_Neapolitan           .set(addItem(aID, "Neapolitan Ice Cream"                     , "Gelato mix of Strawberry, Vanilla and Chocolate" , "foodNeapolitanicecream"          , new FoodStat( 1, 0.600F,   2, C+35,  0.50F,   0,   0,   5,  15,   0, ItemUseAnimation.EAT, null                                 , F, T, F, T), TC.stack(TC.GELUM, 1), TC.stack(TC.FAMES, 1)));});
		aItems.add(13996, aID -> {IL.Food_Ice_Cream_Spumoni_Chocolate    .set(addItem(aID, "Spumoni Chocolate Ice Cream"              , "Gelato mix of Cherry, Pistachio and Chocolate"   , "foodSpumoniicecream"             , new FoodStat( 1, 0.600F,   2, C+35,  0.50F,   0,   0,   5,  18,   0, ItemUseAnimation.EAT, null                                 , F, T, F, T), TC.stack(TC.GELUM, 1), TC.stack(TC.FAMES, 1)));});
		aItems.add(13997, aID -> {IL.Food_Ice_Cream_Spumoni_Vanilla      .set(addItem(aID, "Spumoni Vanilla Ice Cream"                , "Gelato mix of Cherry, Pistachio and Vanilla"     , "foodSpumoniicecream"             , new FoodStat( 1, 0.600F,   2, C+35,  0.50F,   0,   0,   5,  15,   0, ItemUseAnimation.EAT, null                                 , F, T, F, T), TC.stack(TC.GELUM, 1), TC.stack(TC.FAMES, 1)));});
		aItems.add(13998, aID -> {IL.Food_Ice_Cream_Superman             .set(addItem(aID, "Superman Ice Cream"                       , "Gelato that saves the Day!"                      , "foodSupermanicecream"            , new FoodStat( 1, 0.600F,   5, C+35,  0.50F,   0,   0,   5,  15,   0, ItemUseAnimation.EAT, null                                 , F, T, F, T), TC.stack(TC.GELUM, 1), TC.stack(TC.FAMES, 1)));});
		aItems.add(13999, aID -> {
			IL.Food_Ice_Cream_Rainbow              .set(addItem(aID, "Rainbow Ice Cream"                        , "Gelato of Friendship and Magic"                  , "foodRainbowicecream"             , new FoodStat( 1, 0.600F,   0, C+35,  0.50F,   0,   0,   5,  15,   0, ItemUseAnimation.EAT, null                                 , F, T, F, T, /*regeneration*/10, 10, 4, 100, /*digSpeed*/3, 200, 2, 100), TC.stack(TC.GELUM, 1), TC.stack(TC.AURAM, 1)));
		
		for (FluidStack tWater : FL.waters(250)) {
		RM.CryoMixer.addRecipe1(T, 16, 16, OM.dust(MT.NaCl, U4), FL.array(tWater           , FL.Cream.make( 250)), ZL_FS, IL.Food_Ice_Cream.get(1));
		RM.CryoMixer.addRecipe1(T, 16, 64, OM.dust(MT.NaCl, U ), FL.array(FL.mul(tWater, 4), FL.Cream.make(1000)), ZL_FS, IL.Food_Ice_Cream.get(4));
		}
		
		RM.Mixer.addRecipe1(T, 16, 16, IL.Food_Ice_Cream.get(1), FL.Juice_Lemon             .make( 50), NF, IL.Food_Ice_Cream_Lemon.get(1));
		RM.Mixer.addRecipe1(T, 16, 16, IL.Food_Ice_Cream.get(1), FL.Juice_Banana            .make( 50), NF, IL.Food_Ice_Cream_Banana.get(1));
		RM.Mixer.addRecipe1(T, 16, 16, IL.Food_Ice_Cream.get(1), FL.Juice_Grape_Red         .make( 50), NF, IL.Food_Ice_Cream_Grape.get(1));
		RM.Mixer.addRecipe1(T, 16, 16, IL.Food_Ice_Cream.get(1), FL.Juice_Grape_White       .make( 50), NF, IL.Food_Ice_Cream_Grape.get(1));
		RM.Mixer.addRecipe1(T, 16, 16, IL.Food_Ice_Cream.get(1), FL.Juice_Grape_Green       .make( 50), NF, IL.Food_Ice_Cream_Grape.get(1));
		RM.Mixer.addRecipe1(T, 16, 16, IL.Food_Ice_Cream.get(1), FL.Juice_Grape_Purple      .make( 50), NF, IL.Food_Ice_Cream_Grape.get(1));
		RM.Mixer.addRecipe1(T, 16, 16, IL.Food_Ice_Cream.get(1), FL.Juice_Apple             .make( 50), NF, IL.Food_Ice_Cream_Apple.get(1));
		RM.Mixer.addRecipe1(T, 16, 16, IL.Food_Ice_Cream.get(1), FL.Juice_AppleGrC          .make( 50), NF, IL.Food_Ice_Cream_Apple.get(1));
		RM.Mixer.addRecipe1(T, 16, 16, IL.Food_Ice_Cream.get(1), FL.Juice_Ananas            .make( 50), NF, IL.Food_Ice_Cream_Ananas.get(1));
		RM.Mixer.addRecipe1(T, 16, 16, IL.Food_Ice_Cream.get(1), FL.Juice_Cherry            .make( 50), NF, IL.Food_Ice_Cream_Cherry.get(1));
		RM.Mixer.addRecipe1(T, 16, 16, IL.Food_Ice_Cream.get(1), FL.Juice_Cranberry         .make( 50), NF, IL.Food_Ice_Cream_Cranberry.get(1));
		RM.Mixer.addRecipe1(T, 16, 16, IL.Food_Ice_Cream.get(1), FL.Juice_Strawberry        .make( 50), NF, IL.Food_Ice_Cream_Strawberry.get(1));
		RM.Mixer.addRecipe1(T, 16, 16, IL.Food_Ice_Cream.get(1), FL.Juice_Kiwi              .make( 50), NF, IL.Food_Ice_Cream_Kiwi.get(1));
		RM.Mixer.addRecipe1(T, 16, 16, IL.Food_Ice_Cream.get(1), FL.Juice_Melon             .make( 50), NF, IL.Food_Ice_Cream_Melon.get(1));
		RM.Mixer.addRecipe1(T, 16, 16, IL.Food_Ice_Cream.get(1), FL.Juice_Currant           .make( 50), NF, IL.Food_Ice_Cream_Currant.get(1));
		RM.Mixer.addRecipe1(T, 16, 16, IL.Food_Ice_Cream.get(1), FL.Juice_Raspberry         .make( 50), NF, IL.Food_Ice_Cream_Raspberry.get(1));
		RM.Mixer.addRecipe1(T, 16, 16, IL.Food_Ice_Cream.get(1), FL.Juice_Blackberry        .make( 50), NF, IL.Food_Ice_Cream_Blackberry.get(1));
		RM.Mixer.addRecipe1(T, 16, 16, IL.Food_Ice_Cream.get(1), FL.Juice_Blueberry         .make( 50), NF, IL.Food_Ice_Cream_Blueberry.get(1));
		RM.Mixer.addRecipe1(T, 16, 16, IL.Food_Ice_Cream.get(1), FL.Juice_Gooseberry        .make( 50), NF, IL.Food_Ice_Cream_Gooseberry.get(1));
		RM.Mixer.addRecipe1(T, 16, 16, IL.Food_Ice_Cream.get(1), FL.RoyalJelly              .make(  5), NF, IL.Food_Ice_Cream_Honey.get(1));
		RM.Mixer.addRecipe1(T, 16, 16, IL.Food_Ice_Cream.get(1), FL.Honey                   .make( 50), NF, IL.Food_Ice_Cream_Honey.get(1));
		RM.Mixer.addRecipe1(T, 16, 16, IL.Food_Ice_Cream.get(1), FL.HoneyGrC                .make( 50), NF, IL.Food_Ice_Cream_Honey.get(1));
		RM.Mixer.addRecipe1(T, 16, 16, IL.Food_Ice_Cream.get(1), FL.HoneyBoP                .make( 50), NF, IL.Food_Ice_Cream_Honey.get(1));
		RM.Mixer.addRecipe1(T, 16, 16, IL.Food_Ice_Cream.get(1), MT.Chocolate           .liquid(U4, T), NF, IL.Food_Ice_Cream_Chocolate.get(1));
		RM.Mixer.addRecipe1(T, 16, 16, IL.Food_Ice_Cream.get(1), FL.Syrup_Maple             .make( 50), NF, IL.Food_Ice_Cream_Maple.get(1));
		RM.Mixer.addRecipe1(T, 16, 16, IL.Food_Ice_Cream.get(1), FL.Cream_Nutella           .make( 50), NF, IL.Food_Ice_Cream_Nutella.get(1));
		RM.Mixer.addRecipe1(T, 16, 16, IL.Food_Ice_Cream.get(1), FL.Nutbutter_Peanut        .make( 50), NF, IL.Food_Ice_Cream_Peanut_Butter.get(1));
		RM.Mixer.addRecipe1(T, 16, 16, IL.Food_Ice_Cream.get(1), FL.Sap_Rainbow             .make( 50), NF, IL.Food_Ice_Cream_Rainbow.get(1));
		
		RM.Mixer.addRecipe2(T, 16, 16, IL.Food_Ice_Cream.get(1), OM.dust(MT.Chocolate   , U4), IL.Food_Ice_Cream_Stracciatella.get(1));
		RM.Mixer.addRecipe2(T, 16, 64, IL.Food_Ice_Cream.get(4), OM.dust(MT.Chocolate   , U ), IL.Food_Ice_Cream_Stracciatella.get(4));
		RM.Mixer.addRecipe2(T, 16, 16, IL.Food_Ice_Cream.get(1), OM.dust(MT.Vanilla     , U4), IL.Food_Ice_Cream_Vanilla.get(1));
		RM.Mixer.addRecipe2(T, 16, 64, IL.Food_Ice_Cream.get(4), OM.dust(MT.Vanilla     , U ), IL.Food_Ice_Cream_Vanilla.get(4));
		RM.Mixer.addRecipe2(T, 16, 16, IL.Food_Ice_Cream.get(1), OM.dust(MT.Coffee      , U4), IL.Food_Ice_Cream_Mocha.get(1));
		RM.Mixer.addRecipe2(T, 16, 64, IL.Food_Ice_Cream.get(4), OM.dust(MT.Coffee      , U ), IL.Food_Ice_Cream_Mocha.get(4));
		RM.Mixer.addRecipe2(T, 16, 16, IL.Food_Ice_Cream.get(1), OM.dust(MT.Mint        , U4), IL.Food_Ice_Cream_Mint.get(1));
		RM.Mixer.addRecipe2(T, 16, 64, IL.Food_Ice_Cream.get(4), OM.dust(MT.Mint        , U ), IL.Food_Ice_Cream_Mint.get(4));
		RM.Mixer.addRecipe2(T, 16, 16, IL.Food_Ice_Cream.get(1), OM.dust(MT.Pistachio   , U4), IL.Food_Ice_Cream_Pistachio.get(1));
		RM.Mixer.addRecipe2(T, 16, 64, IL.Food_Ice_Cream.get(4), OM.dust(MT.Pistachio   , U ), IL.Food_Ice_Cream_Pistachio.get(4));
		RM.Mixer.addRecipe2(T, 16, 16, IL.Food_Ice_Cream.get(1), OM.dust(MT.MeatCooked  , U4), IL.Food_Ice_Cream_Bear.get(1));
		RM.Mixer.addRecipe2(T, 16, 64, IL.Food_Ice_Cream.get(4), OM.dust(MT.MeatCooked  , U ), IL.Food_Ice_Cream_Bear.get(4));
		
		RM.Mixer.addRecipe2(T, 16, 16, IL.Food_Ice_Cream_Mint.get(1), OM.dust(MT.Chocolate  , U4), IL.Food_Ice_Cream_Mint_Chocolate_Chip.get(1));
		RM.Mixer.addRecipe2(T, 16, 64, IL.Food_Ice_Cream_Mint.get(4), OM.dust(MT.Chocolate  , U ), IL.Food_Ice_Cream_Mint_Chocolate_Chip.get(4));
		
		RM.Mixer.addRecipeX(T, 16, 48, ST.array(IL.Food_Ice_Cream_Strawberry.get(1), IL.Food_Ice_Cream_Vanilla  .get(1), IL.Food_Ice_Cream_Chocolate.get(1)), IL.Food_Ice_Cream_Neapolitan.get(3));
		RM.Mixer.addRecipeX(T, 16, 48, ST.array(IL.Food_Ice_Cream_Cherry    .get(1), IL.Food_Ice_Cream_Pistachio.get(1), IL.Food_Ice_Cream_Vanilla  .get(1)), IL.Food_Ice_Cream_Spumoni_Vanilla.get(3));
		RM.Mixer.addRecipeX(T, 16, 48, ST.array(IL.Food_Ice_Cream_Cherry    .get(1), IL.Food_Ice_Cream_Pistachio.get(1), IL.Food_Ice_Cream_Chocolate.get(1)), IL.Food_Ice_Cream_Spumoni_Chocolate.get(3));
		RM.Mixer.addRecipeX(T, 16, 48, ST.array(IL.Food_Ice_Cream_Cherry    .get(1), IL.Food_Ice_Cream_Blueberry.get(1), IL.Food_Ice_Cream_Lemon    .get(1)), IL.Food_Ice_Cream_Superman.get(3));
		});
		
		
		
		
		
		
		
		
		
		
		aItems.add(31000, aID -> {IL.Pill_Empty                          .set(addItem(aID, "Empty Wax Pill"                           , "Placebo"                                                     , new FoodStat( 0, 0.000F,   0, C+37,  0.00F,   0,   0,   0,   0,   0,   0, ItemUseAnimation.EAT, null                                 , T, F, F, T                                     ), TC.stack(TC.SANO, 1), TC.stack(TC.VACUOS , 1))); Sandwiches.INGREDIENTS.put(last(), (byte)42);});
		aItems.add(31001, aID -> {IL.Pill_Iodine                         .set(addItem(aID, "Radaway"                                  , "Pill that cures Effects of Radiation"                        , new FoodStat( 0, 0.000F,   0, C+37,  0.00F,   0,   0,   0,   0,   0, -50, ItemUseAnimation.EAT, null                                 , T, F, F, T, PotionsGT.ID_RADIATION, -1, -1, 100), TC.stack(TC.SANO, 3)                         )); Sandwiches.INGREDIENTS.put(last(), (byte)42);});
		aItems.add(31002, aID -> {IL.Pill_Mint                           .set(addItem(aID, "Peppermint"                               , "Take a fresh energetic Breath"                               , new FoodStat( 0, 0.000F,   0, C+37,  0.00F,   0,   0,   0,   0,   0,   0, ItemUseAnimation.EAT, null                                 , T, F, F, T, /*moveSlowdown*/2, -1, -1, 100), TC.stack(TC.SANO, 1), TC.stack(TC.HERBA  , 1))); Sandwiches.INGREDIENTS.put(last(), (byte)42);});
		aItems.add(31003, aID -> {IL.Pill_Blue                           .set(addItem(aID, "Blue Pill"                                , "Ignore the nauseating Reality"                               , new FoodStat( 0, 0.000F,   0, C+37,  0.00F,   0,   0,   0,   0,   0,   0, ItemUseAnimation.EAT, null                                 , T, F, F, T, /*confusion*/9, -1, -1, 100), TC.stack(TC.SANO, 1), TC.stack(TC.CORPUS , 1))); Sandwiches.INGREDIENTS.put(last(), (byte)42);});
		aItems.add(31004, aID -> {IL.Pill_Red                            .set(addItem(aID, "Red Pill"                                 , "Open your Eyes to the Truth"                                 , new FoodStat( 0, 0.000F,   0, C+37,  0.00F,   0,   0,   0,   0,   0,   0, ItemUseAnimation.EAT, null                                 , T, F, F, T, /*blindness*/15, -1, -1, 100), TC.stack(TC.SANO, 1), TC.stack(TC.SENSUS , 1))); Sandwiches.INGREDIENTS.put(last(), (byte)42);});
		aItems.add(31005, aID -> {
			IL.Pill_Antidote                       .set(addItem(aID, "Antidote"                                 , "Pill that cures Poison"                                      , new FoodStat( 0, 0.000F,   0, C+37,  0.00F,   0,   0,   0,   0,   0,   0, ItemUseAnimation.EAT, null                                 , T, F, F, T, /*poison*/19, -1, -1, 100), TC.stack(TC.SANO, 1), TC.stack(TC.VENEMUM, 1))); Sandwiches.INGREDIENTS.put(last(), (byte)42);
		RM.Boxinator    .addRecipe2(T, 16,   16, OM.dust(MT.I)                          , IL.Pill_Empty.get(1), IL.Pill_Iodine.get(1));
		RM.Boxinator    .addRecipe2(T, 16,   16, OM.dust(MT.Mint)                       , IL.Pill_Empty.get(1), IL.Pill_Mint.get(1));
		RM.Boxinator    .addRecipe2(T, 16,   16, ST.make(Blocks.BROWN_MUSHROOM, 1, W)   , IL.Pill_Empty.get(1), IL.Pill_Antidote.get(1));
		});
		
		aItems.add(31999, aID -> {IL.Pill_Cure_All                       .set(addItem(aID, "Cure All"                                 , "Cures everything you could imagine*"                         , new FoodStat( 0, 0.000F,   0, C+37,  1.00F,-999,-999,-999,-999,-999,-999, ItemUseAnimation.EAT, null                                 , T, F, F, T, PotionsGT.ID_RADIATION, -1, -1, 100, PotionsGT.ID_HYPOTHERMIA, -1, -1, 100, PotionsGT.ID_HEATSTROKE, -1, -1, 100, PotionsGT.ID_FROSTBITE, -1, -1, 100, PotionsGT.ID_DEHYDRATION, -1, -1, 100, PotionsGT.ID_INSANITY, -1, -1, 100, PotionsGT.ID_FLAMMABLE, -1, -1, 100, PotionsGT.ID_SLIPPERY, -1, -1, 100, PotionsGT.ID_CONDUCTIVE, -1, -1, 100, PotionsGT.ID_STICKY, -1, -1, 100, /*digSlowdown*/4, -1, -1, 100, /*moveSlowdown*/2, -1, -1, 100, /*hunger*/17, -1, -1, 100, /*harm*/7, -1, -1, 100, /*confusion*/9, -1, -1, 100, /*blindness*/15, -1, -1, 100, /*weakness*/18, -1, -1, 100, /*poison*/19, -1, -1, 100, /*wither*/20, -1, -1, 100, /*regeneration*/10, 100, 100, 100, /*saturation, field_76443_y*/23, 100, 100, 100).setMilk().setExtinguish(), new Behavior_CureZombie(500, F), TC.stack(TC.SANO, 10), TD.Creative.HIDDEN)); Sandwiches.INGREDIENTS.put(last(), (byte)42);});
		
		
		
		aItems.add(32101, aID -> {IL.Food_Tofu                           .set(addItem(aID, "Tofu Bar"                                 , "Alternative for Meat"                , OP.ingot.dat(MT.Tofu)         , new FoodStat( 2, 1.400F,   0, C+37,  0.10F,   0,   0,   4,   4,   0, ItemUseAnimation.EAT, null                         , F, T, F, T), TC.stack(TC.HERBA, 1), TC.stack(TC.FAMES, 1))); Sandwiches.INGREDIENTS.put(last(), (byte)35);});
		aItems.add(32103, aID -> {IL.Food_SoylentGreen                   .set(addItem(aID, "Emerald Green Bar"                        , "Emerald Green is Villagers!"         , OP.ingot.dat(MT.SoylentGreen) , new FoodStat( 3, 1.400F,   0, C+37,  0.10F,   0,   0,   4,   0,  12, ItemUseAnimation.EAT, null                         , F, T, F, T), TC.stack(TC.CORPUS, 1), TC.stack(TC.FAMES, 1))); Sandwiches.INGREDIENTS.put(last(), (byte)34);});
		aItems.add(32105, aID -> {IL.Food_Meat_Raw                       .set(addItem(aID, "Raw Meat Bar"                             , "Don't eat raw Mince Meat"            , OP.ingot.dat(MT.MeatRaw)      , new FoodStat( 2, 0.600F,   0, C+37,  0.10F,   0,   0,   4,   0,  12, ItemUseAnimation.EAT, null                         , F, T, F, T, /*hunger*/17, 300, 0, 50), TC.stack(TC.CORPUS, 1), TC.stack(TC.FAMES, 1))); Sandwiches.INGREDIENTS.put(last(), (byte)29);});
		aItems.add(32107, aID -> {IL.Food_Meat                           .set(addItem(aID, "Cooked Meat Bar"                          , "Compressed Meat"                     , OP.ingot.dat(MT.MeatCooked)   , new FoodStat( 2, 1.600F,   0, C+38,  0.50F,   0,   0,   4,   0,   4, ItemUseAnimation.EAT, null                         , F, T, F, T), TC.stack(TC.CORPUS, 1), TC.stack(TC.IGNIS, 1), TC.stack(TC.FAMES, 1))); Sandwiches.INGREDIENTS.put(last(), (byte)30);});
		aItems.add(32109, aID -> {IL.Food_Chocolate                      .set(addItem(aID, "Chocolate Bar"                            , "Not for Canines or Felines!"         , OP.ingot.dat(MT.Chocolate)    , new FoodStat( 4, 0.800F,   0, C+37,  0.10F,   0,   0,   0,  40,   0, ItemUseAnimation.EAT, null                         , F, T, F, T), TC.stack(TC.HERBA, 1), TC.stack(TC.MOTUS, 1), TC.stack(TC.FAMES, 1), Behavior_FeedChocolate.INSTANCE)); OreDictManager.INSTANCE.setTarget_(OP.ingot, MT.Chocolate, last()); Sandwiches.INGREDIENTS.put(last(), (byte)21);});
		aItems.add(32111, aID -> {IL.Food_Cheese_Bar                     .set(addItem(aID, "Cheese Bar"                               , "Compact Cheese"                      , OP.ingot.dat(MT.Cheese)       , new FoodStat( 2, 1.200F,   0, C+37,  0.10F,   0,   0,   8,   0,   8, ItemUseAnimation.EAT, null                         , F, T, F, T), TC.stack(TC.FAMES, 2)));});
		aItems.add(32113, aID -> {IL.Food_Fish_Raw                       .set(addItem(aID, "Raw Fish Bar"                             , "Compressed Fish"                     , OP.ingot.dat(MT.FishRaw)      , new FoodStat( 2, 0.600F,   0, C+37,  0.10F,   0,   0,   3,   0,   8, ItemUseAnimation.EAT, null                         , F, T, F, T, /*hunger*/17, 300, 0, 50), TC.stack(TC.CORPUS, 1), TC.stack(TC.FAMES, 1))); Sandwiches.INGREDIENTS.put(last(), (byte)24);});
		aItems.add(32115, aID -> {IL.Food_Fish                           .set(addItem(aID, "Cooked Fish Bar"                          , "Like Fish Sticks? You're a gay Fish" , OP.ingot.dat(MT.FishCooked)   , new FoodStat( 2, 1.600F,   0, C+38,  0.50F,   0,   0,   3,   0,   8, ItemUseAnimation.EAT, null                         , F, T, F, T), TC.stack(TC.CORPUS, 1), TC.stack(TC.IGNIS, 1), TC.stack(TC.FAMES, 1))); Sandwiches.INGREDIENTS.put(last(), (byte)25);});
		aItems.add(32117, aID -> {IL.Food_Butter                         .set(addItem(aID, "Butter"                                   , "A chunk of pure Fat"                 , OP.ingot.dat(MT.Butter)       , new FoodStat( 1, 4.000F,   0, C+37,  0.10F,   0,   0,   0,   0,  80, ItemUseAnimation.EAT, null                         , F, T, F, T, PotionsGT.ID_SLIPPERY, 300, 1, 90), TC.stack(TC.FAMES, 3))); OreDictManager.INSTANCE.setTarget_(OP.ingot, MT.Butter      , last()); Sandwiches.INGREDIENTS.put(last(), DYE_INDEX_Yellow);});
		aItems.add(32119, aID -> {
			IL.Food_Butter_Salted                  .set(addItem(aID, "Salted Butter"                            , "As if it wasn't unhealthy already"   , OP.ingot.dat(MT.ButterSalted) , new FoodStat( 1, 4.000F,   0, C+37,  0.10F,   0,   0,  40,   0,  80, ItemUseAnimation.EAT, null                         , F, T, F, T, PotionsGT.ID_SLIPPERY, 300, 1, 90), TC.stack(TC.FAMES, 3))); OreDictManager.INSTANCE.setTarget_(OP.ingot, MT.ButterSalted, last()); Sandwiches.INGREDIENTS.put(last(), DYE_INDEX_Yellow);
		RM.replicateOrganic( 7,  8, IL.Food_Tofu.get(1));
		RM.replicateOrganic( 7,  9, IL.Food_SoylentGreen.get(1));
		RM.replicateOrganic( 7, 10, IL.Food_Meat_Raw.get(1));
		RM.replicateOrganic( 7, 11, IL.Food_Chocolate.get(1));
		RM.replicateOrganic( 7, 12, IL.Food_Cheese_Bar.get(1));
		RM.replicateOrganic( 7, 13, IL.Food_Fish_Raw.get(1));
		RM.replicateOrganic( 7, 14, IL.Food_Butter.get(1));
		
		RM.replicateOrganic( 8,  9, ST.make(Blocks.BROWN_MUSHROOM_BLOCK, 1, 0));
		RM.replicateOrganic( 8, 10, ST.make(Blocks.RED_MUSHROOM_BLOCK, 1, 0));
		RM.replicateOrganic( 8, 11, ST.make(Items.NETHER_WART, 1, 0));
		RM.replicateOrganic( 8, 12, OP.plantGtWart.mat(MT.Milk, 1));
		RM.replicateOrganic( 8, 13, OP.plantGtWart.mat(MT.Glowstone, 1));
		
		RM.replicateOrganic( 9, 10, OP.plantGtBlossom.mat(MT.Tea, 1));
		RM.replicateOrganic( 9, 11, OP.plantGtBlossom.mat(MT.Mint, 1));
		RM.replicateOrganic( 9, 12, OP.plantGtBlossom.mat(MT.Indigo, 1));
		
		RM.replicateOrganic(10, 11, IL.Food_White_Egg.get(1));
		RM.replicateOrganic(10, 12, ST.make(Items.FEATHER, 1, 0));
		RM.replicateOrganic(10, 13, ST.make(Items.LEATHER, 1, 0));
		RM.replicateOrganic(10, 14, ST.make(Blocks.COBWEB, 1, 0));
		RM.replicateOrganic(10, 15, ST.make(Items.BONE, 1, 0));
		RM.replicateOrganic(10, 16, ST.make(Items.SLIME_BALL, 1, 0));
		RM.replicateOrganic(10, 17, ST.make(Items.SPIDER_EYE, 1, 0));
		RM.replicateOrganic(10, 18, ST.make(Items.GHAST_TEAR, 1, 0));
		RM.replicateOrganic(10, 19, ST.make(Items.SUGAR_CANE, 1, 0));
		RM.replicateOrganic(10, 20, ST.make(Blocks.CACTUS, 1, 0));
		RM.replicateOrganic(10, 21, ST.make(Blocks.VINE, 1, 0));
		RM.replicateOrganic(10, 22, ST.make(Blocks.LILY_PAD, 1, 0));
		RM.replicateOrganic(10, 23, IL.Dye_SquidInk.get(1));
		RM.replicateOrganic(10, 24, IL.Dye_Cocoa.get(1));
		});
	}
	
	@Override
	public ItemStack getRotten(ItemStack aStack) {
		short aMeta = ST.meta_(aStack);
		if (UT.Code.inside(    0,   999, aMeta)) return (IL.ENVM_Rotten_Food.exists()?IL.ENVM_Rotten_Food:IL.Remains_Plant).get(aStack.getCount());
		if (UT.Code.inside( 1100,  1999, aMeta)) return ST.make(Items.ROTTEN_FLESH, aStack.getCount(), 0);
		if (UT.Code.inside(13000, 13999, aMeta)) return null;
		if (UT.Code.inside(31000, 31999, aMeta)) return aStack;
		
		switch(aMeta) {
		case 12000: return ST.make(metaFamily(), aStack.getCount(), 12002, ItemNBT.get(aStack));
		case 12001: return ST.make(metaFamily(), aStack.getCount(), 12002, ItemNBT.get(aStack));
		case 12002: return ST.make(metaFamily(), aStack.getCount(), 12003, ItemNBT.get(aStack));
		case 12004: return ST.make(metaFamily(), aStack.getCount(), 12002, ItemNBT.get(aStack));
		case 12005: return ST.make(metaFamily(), aStack.getCount(), 12002, ItemNBT.get(aStack));
		case 12006: return ST.make(metaFamily(), aStack.getCount(), 12002, ItemNBT.get(aStack));
		case 12007: return ST.make(metaFamily(), aStack.getCount(), 12002, ItemNBT.get(aStack));
		case 32700: return (IL.ENVM_Rotten_Food.exists()?IL.ENVM_Rotten_Food:IL.Remains_Plant).get(aStack.getCount());
		case 32701: return (IL.ENVM_Rotten_Food.exists()?IL.ENVM_Rotten_Food:IL.Remains_Plant).get(aStack.getCount());
		case 32105: case 32107: return OP.ingot.mat(MT.MeatRotten, aStack.getCount());
		case 32113: case 32115: return OP.ingot.mat(MT.FishRotten, aStack.getCount());
		
		default: return ST.food(aStack) > 0 ? IL.ENVM_Rotten_Food.exists() ? IL.ENVM_Rotten_Food.get(aStack.getCount()) : null : aStack;
		}
	}
	
	@Override public ItemStack getRotten(ItemStack aStack, Level aWorld, int aX, int aY, int aZ) {return getRotten(aStack);}
}

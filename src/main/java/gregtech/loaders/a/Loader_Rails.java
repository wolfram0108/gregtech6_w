/**
 * Copyright (c) 2023 GregTech-6 Team
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

package gregtech.loaders.a;

import gregapi.GT_API;
import gregapi.block.misc.BlockBaseRail;
import gregapi.data.*;
import gregapi.data.CS.BlocksGT;
import gregapi.old.Textures;
import gregapi.util.CR;
import gregapi.util.ST;
import gregtech.blocks.BlockRailRoad;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import static gregapi.data.CS.F;
import static gregapi.data.CS.T;
import static gregapi.util.CR.*;

public class Loader_Rails implements Runnable {
	@Override
	public void run() {
		GT_API.registerBlockLazy(gregapi.data.CS.ModIDs.GT, "gt.block.rail.road", () -> {BlockRailRoad b = new BlockRailRoad(null, "gt.block.rail.road"                    , "Road Stripe"                         , 0.50F, 20,                                 0, Textures.BlockIcons.RAIL_ROAD_STRIPE             , Textures.BlockIcons.RAIL_ROAD_REFLECTOR); BlocksGT.RailRoad = b; return b;});
		
		GT_API.registerBlockLazy(gregapi.data.CS.ModIDs.GT, "gt.block.rail.aluminium", () -> {BlockBaseRail b = new BlockBaseRail(null, "gt.block.rail.aluminium"               , "Aluminium Track"               , F, F, 0.20F,  6, MT.Al             .mToolQuality-1, Textures.BlockIcons.RAIL_STRAIGHT_ALUMINIUM      , Textures.BlockIcons.RAIL_TURNED_ALUMINIUM); BlocksGT.RailAluminium = b; return b;});
		GT_API.registerBlockLazy(gregapi.data.CS.ModIDs.GT, "gt.block.rail.bronze", () -> {BlockBaseRail b = new BlockBaseRail(null, "gt.block.rail.bronze"                  , "Bronze Track"                  , F, F, 0.30F,  8, MT.Bronze         .mToolQuality-1, Textures.BlockIcons.RAIL_STRAIGHT_BRONZE         , Textures.BlockIcons.RAIL_TURNED_BRONZE); BlocksGT.RailBronze = b; return b;});
		GT_API.registerBlockLazy(gregapi.data.CS.ModIDs.GT, "gt.block.rail.magnalium", () -> {BlockBaseRail b = new BlockBaseRail(null, "gt.block.rail.magnalium"               , "Magnalium Track"               , F, F, 0.60F, 12, MT.Magnalium      .mToolQuality-1, Textures.BlockIcons.RAIL_STRAIGHT_MAGNALIUM      , Textures.BlockIcons.RAIL_TURNED_MAGNALIUM); BlocksGT.RailMagnalium = b; return b;});
		GT_API.registerBlockLazy(gregapi.data.CS.ModIDs.GT, "gt.block.rail.steel", () -> {BlockBaseRail b = new BlockBaseRail(null, "gt.block.rail.steel"                   , "Steel Track"                   , F, F, 0.60F, 12, MT.Steel          .mToolQuality-1, Textures.BlockIcons.RAIL_STRAIGHT_STEEL          , Textures.BlockIcons.RAIL_TURNED_STEEL); BlocksGT.RailSteel = b; return b;});
		GT_API.registerBlockLazy(gregapi.data.CS.ModIDs.GT, "gt.block.rail.stainlesssteel", () -> {BlockBaseRail b = new BlockBaseRail(null, "gt.block.rail.stainlesssteel"          , "Stainless Steel Track"         , F, F, 0.80F, 10, MT.StainlessSteel .mToolQuality-1, Textures.BlockIcons.RAIL_STRAIGHT_STAINLESSSTEEL , Textures.BlockIcons.RAIL_TURNED_STAINLESSSTEEL); BlocksGT.RailStainlessSteel = b; return b;});
		GT_API.registerBlockLazy(gregapi.data.CS.ModIDs.GT, "gt.block.rail.tungsten", () -> {BlockBaseRail b = new BlockBaseRail(null, "gt.block.rail.tungsten"                , "Tungsten Track"                , F, F, 1.00F, 20, MT.W              .mToolQuality-1, Textures.BlockIcons.RAIL_STRAIGHT_TUNGSTEN       , Textures.BlockIcons.RAIL_TURNED_TUNGSTEN); BlocksGT.RailTungsten = b; return b;});
		GT_API.registerBlockLazy(gregapi.data.CS.ModIDs.GT, "gt.block.rail.titanium", () -> {BlockBaseRail b = new BlockBaseRail(null, "gt.block.rail.titanium"                , "Titanium Track"                , F, F, 1.20F, 16, MT.Ti             .mToolQuality-1, Textures.BlockIcons.RAIL_STRAIGHT_TITANIUM       , Textures.BlockIcons.RAIL_TURNED_TITANIUM); BlocksGT.RailTitanium = b; return b;});
		GT_API.registerBlockLazy(gregapi.data.CS.ModIDs.GT, "gt.block.rail.tungstensteel", () -> {BlockBaseRail b = new BlockBaseRail(null, "gt.block.rail.tungstensteel"           , "Tungstensteel Track"           , F, F, 1.40F, 20, MT.TungstenSteel  .mToolQuality-1, Textures.BlockIcons.RAIL_STRAIGHT_TUNGSTENSTEEL  , Textures.BlockIcons.RAIL_TURNED_TUNGSTENSTEEL); BlocksGT.RailTungstenSteel = b; return b;});
		GT_API.registerBlockLazy(gregapi.data.CS.ModIDs.GT, "gt.block.rail.tungstencarbide", () -> {BlockBaseRail b = new BlockBaseRail(null, "gt.block.rail.tungstencarbide"         , "Tungstencarbide Track"         , F, F, 1.60F, 24, MT.TungstenCarbide.mToolQuality-1, Textures.BlockIcons.RAIL_STRAIGHT_TUNGSTENCARBIDE, Textures.BlockIcons.RAIL_TURNED_TUNGSTENCARBIDE); BlocksGT.RailTungstenCarbide = b; return b;});
		GT_API.registerBlockLazy(gregapi.data.CS.ModIDs.GT, "gt.block.rail.adamantium", () -> {BlockBaseRail b = new BlockBaseRail(null, "gt.block.rail.adamantium"              , "Adamantium Track"              , F, F, 4.00F,100, MT.Ad             .mToolQuality-1, Textures.BlockIcons.RAIL_STRAIGHT_ADAMANTIUM     , Textures.BlockIcons.RAIL_TURNED_ADAMANTIUM); BlocksGT.RailAdamantium = b; return b;});
		
		GT_API.registerBlockLazy(gregapi.data.CS.ModIDs.GT, "gt.block.rail.booster.aluminium", () -> {BlockBaseRail b = new BlockBaseRail(null, "gt.block.rail.booster.aluminium"       , "Aluminium Booster Track"       , T, F, 0.20F,  6, MT.Al             .mToolQuality-1, Textures.BlockIcons.RAIL_BOOSTER_ALUMINIUM       , Textures.BlockIcons.RAIL_BOOSTER_ACTIVE_ALUMINIUM); BlocksGT.RailAluminiumBooster = b; return b;});
		GT_API.registerBlockLazy(gregapi.data.CS.ModIDs.GT, "gt.block.rail.booster.bronze", () -> {BlockBaseRail b = new BlockBaseRail(null, "gt.block.rail.booster.bronze"          , "Bronze Booster Track"          , T, F, 0.30F,  8, MT.Bronze         .mToolQuality-1, Textures.BlockIcons.RAIL_BOOSTER_BRONZE          , Textures.BlockIcons.RAIL_BOOSTER_ACTIVE_BRONZE); BlocksGT.RailBronzeBooster = b; return b;});
		GT_API.registerBlockLazy(gregapi.data.CS.ModIDs.GT, "gt.block.rail.booster.magnalium", () -> {BlockBaseRail b = new BlockBaseRail(null, "gt.block.rail.booster.magnalium"       , "Magnalium Booster Track"       , T, F, 0.60F, 12, MT.Magnalium      .mToolQuality-1, Textures.BlockIcons.RAIL_BOOSTER_MAGNALIUM       , Textures.BlockIcons.RAIL_BOOSTER_ACTIVE_MAGNALIUM); BlocksGT.RailMagnaliumBooster = b; return b;});
		GT_API.registerBlockLazy(gregapi.data.CS.ModIDs.GT, "gt.block.rail.booster.steel", () -> {BlockBaseRail b = new BlockBaseRail(null, "gt.block.rail.booster.steel"           , "Steel Booster Track"           , T, F, 0.60F, 12, MT.Steel          .mToolQuality-1, Textures.BlockIcons.RAIL_BOOSTER_STEEL           , Textures.BlockIcons.RAIL_BOOSTER_ACTIVE_STEEL); BlocksGT.RailSteelBooster = b; return b;});
		GT_API.registerBlockLazy(gregapi.data.CS.ModIDs.GT, "gt.block.rail.booster.stainlesssteel", () -> {BlockBaseRail b = new BlockBaseRail(null, "gt.block.rail.booster.stainlesssteel"  , "Stainless Steel Booster Track" , T, F, 0.80F, 10, MT.StainlessSteel .mToolQuality-1, Textures.BlockIcons.RAIL_BOOSTER_STAINLESSSTEEL  , Textures.BlockIcons.RAIL_BOOSTER_ACTIVE_STAINLESSSTEEL); BlocksGT.RailStainlessSteelBooster = b; return b;});
		GT_API.registerBlockLazy(gregapi.data.CS.ModIDs.GT, "gt.block.rail.booster.tungsten", () -> {BlockBaseRail b = new BlockBaseRail(null, "gt.block.rail.booster.tungsten"        , "Tungsten Booster Track"        , T, F, 1.00F, 20, MT.W              .mToolQuality-1, Textures.BlockIcons.RAIL_BOOSTER_TUNGSTEN        , Textures.BlockIcons.RAIL_BOOSTER_ACTIVE_TUNGSTEN); BlocksGT.RailTungstenBooster = b; return b;});
		GT_API.registerBlockLazy(gregapi.data.CS.ModIDs.GT, "gt.block.rail.booster.titanium", () -> {BlockBaseRail b = new BlockBaseRail(null, "gt.block.rail.booster.titanium"        , "Titanium Booster Track"        , T, F, 1.20F, 16, MT.Ti             .mToolQuality-1, Textures.BlockIcons.RAIL_BOOSTER_TITANIUM        , Textures.BlockIcons.RAIL_BOOSTER_ACTIVE_TITANIUM); BlocksGT.RailTitaniumBooster = b; return b;});
		GT_API.registerBlockLazy(gregapi.data.CS.ModIDs.GT, "gt.block.rail.booster.tungstensteel", () -> {BlockBaseRail b = new BlockBaseRail(null, "gt.block.rail.booster.tungstensteel"   , "Tungstensteel Booster Track"   , T, F, 1.40F, 20, MT.TungstenSteel  .mToolQuality-1, Textures.BlockIcons.RAIL_BOOSTER_TUNGSTENSTEEL   , Textures.BlockIcons.RAIL_BOOSTER_ACTIVE_TUNGSTENSTEEL); BlocksGT.RailTungstenSteelBooster = b; return b;});
		GT_API.registerBlockLazy(gregapi.data.CS.ModIDs.GT, "gt.block.rail.booster.tungstencarbide", () -> {BlockBaseRail b = new BlockBaseRail(null, "gt.block.rail.booster.tungstencarbide" , "Tungstencarbide Booster Track" , T, F, 1.60F, 24, MT.TungstenCarbide.mToolQuality-1, Textures.BlockIcons.RAIL_BOOSTER_TUNGSTENCARBIDE , Textures.BlockIcons.RAIL_BOOSTER_ACTIVE_TUNGSTENCARBIDE); BlocksGT.RailTungstenCarbideBooster = b; return b;});
		GT_API.registerBlockLazy(gregapi.data.CS.ModIDs.GT, "gt.block.rail.booster.adamantium", () -> {BlockBaseRail b = new BlockBaseRail(null, "gt.block.rail.booster.adamantium"      , "Adamantium Booster Track"      , T, F, 4.00F,100, MT.Ad             .mToolQuality-1, Textures.BlockIcons.RAIL_BOOSTER_ADAMANTIUM      , Textures.BlockIcons.RAIL_BOOSTER_ACTIVE_ADAMANTIUM); BlocksGT.RailAdamantiumBooster = b; return b;});
		
		GT_API.registerBlockLazy(gregapi.data.CS.ModIDs.GT, "gt.block.rail.detector.aluminium", () -> {BlockBaseRail b = new BlockBaseRail(null, "gt.block.rail.detector.aluminium"      , "Aluminium Detector Track"      , F, T, 0.20F,  6, MT.Al             .mToolQuality-1, Textures.BlockIcons.RAIL_DETECTOR_ALUMINIUM      , Textures.BlockIcons.RAIL_DETECTOR_ACTIVE_ALUMINIUM); BlocksGT.RailAluminiumDetector = b; return b;});
		GT_API.registerBlockLazy(gregapi.data.CS.ModIDs.GT, "gt.block.rail.detector.bronze", () -> {BlockBaseRail b = new BlockBaseRail(null, "gt.block.rail.detector.bronze"         , "Bronze Detector Track"         , F, T, 0.30F,  8, MT.Bronze         .mToolQuality-1, Textures.BlockIcons.RAIL_DETECTOR_BRONZE         , Textures.BlockIcons.RAIL_DETECTOR_ACTIVE_BRONZE); BlocksGT.RailBronzeDetector = b; return b;});
		GT_API.registerBlockLazy(gregapi.data.CS.ModIDs.GT, "gt.block.rail.detector.magnalium", () -> {BlockBaseRail b = new BlockBaseRail(null, "gt.block.rail.detector.magnalium"      , "Magnalium Detector Track"      , F, T, 0.60F, 12, MT.Magnalium      .mToolQuality-1, Textures.BlockIcons.RAIL_DETECTOR_MAGNALIUM      , Textures.BlockIcons.RAIL_DETECTOR_ACTIVE_MAGNALIUM); BlocksGT.RailMagnaliumDetector = b; return b;});
		GT_API.registerBlockLazy(gregapi.data.CS.ModIDs.GT, "gt.block.rail.detector.steel", () -> {BlockBaseRail b = new BlockBaseRail(null, "gt.block.rail.detector.steel"          , "Steel Detector Track"          , F, T, 0.60F, 12, MT.Steel          .mToolQuality-1, Textures.BlockIcons.RAIL_DETECTOR_STEEL          , Textures.BlockIcons.RAIL_DETECTOR_ACTIVE_STEEL); BlocksGT.RailSteelDetector = b; return b;});
		GT_API.registerBlockLazy(gregapi.data.CS.ModIDs.GT, "gt.block.rail.detector.stainlesssteel", () -> {BlockBaseRail b = new BlockBaseRail(null, "gt.block.rail.detector.stainlesssteel" , "Stainless Steel Detector Track", F, T, 0.80F, 10, MT.StainlessSteel .mToolQuality-1, Textures.BlockIcons.RAIL_DETECTOR_STAINLESSSTEEL , Textures.BlockIcons.RAIL_DETECTOR_ACTIVE_STAINLESSSTEEL); BlocksGT.RailStainlessSteelDetector = b; return b;});
		GT_API.registerBlockLazy(gregapi.data.CS.ModIDs.GT, "gt.block.rail.detector.tungsten", () -> {BlockBaseRail b = new BlockBaseRail(null, "gt.block.rail.detector.tungsten"       , "Tungsten Detector Track"       , F, T, 1.00F, 20, MT.W              .mToolQuality-1, Textures.BlockIcons.RAIL_DETECTOR_TUNGSTEN       , Textures.BlockIcons.RAIL_DETECTOR_ACTIVE_TUNGSTEN); BlocksGT.RailTungstenDetector = b; return b;});
		GT_API.registerBlockLazy(gregapi.data.CS.ModIDs.GT, "gt.block.rail.detector.titanium", () -> {BlockBaseRail b = new BlockBaseRail(null, "gt.block.rail.detector.titanium"       , "Titanium Detector Track"       , F, T, 1.20F, 16, MT.Ti             .mToolQuality-1, Textures.BlockIcons.RAIL_DETECTOR_TITANIUM       , Textures.BlockIcons.RAIL_DETECTOR_ACTIVE_TITANIUM); BlocksGT.RailTitaniumDetector = b; return b;});
		GT_API.registerBlockLazy(gregapi.data.CS.ModIDs.GT, "gt.block.rail.detector.tungstensteel", () -> {BlockBaseRail b = new BlockBaseRail(null, "gt.block.rail.detector.tungstensteel"  , "Tungstensteel Detector Track"  , F, T, 1.40F, 20, MT.TungstenSteel  .mToolQuality-1, Textures.BlockIcons.RAIL_DETECTOR_TUNGSTENSTEEL  , Textures.BlockIcons.RAIL_DETECTOR_ACTIVE_TUNGSTENSTEEL); BlocksGT.RailTungstenSteelDetector = b; return b;});
		GT_API.registerBlockLazy(gregapi.data.CS.ModIDs.GT, "gt.block.rail.detector.tungstencarbide", () -> {BlockBaseRail b = new BlockBaseRail(null, "gt.block.rail.detector.tungstencarbide", "Tungstencarbide Detector Track", F, T, 1.60F, 24, MT.TungstenCarbide.mToolQuality-1, Textures.BlockIcons.RAIL_DETECTOR_TUNGSTENCARBIDE, Textures.BlockIcons.RAIL_DETECTOR_ACTIVE_TUNGSTENCARBIDE); BlocksGT.RailTungstenCarbideDetector = b; return b;});
		GT_API.registerBlockLazy(gregapi.data.CS.ModIDs.GT, "gt.block.rail.detector.adamantium", () -> {BlockBaseRail b = new BlockBaseRail(null, "gt.block.rail.detector.adamantium"     , "Adamantium Detector Track"     , F, T, 4.00F,100, MT.Ad             .mToolQuality-1, Textures.BlockIcons.RAIL_DETECTOR_ADAMANTIUM     , Textures.BlockIcons.RAIL_DETECTOR_ACTIVE_ADAMANTIUM); BlocksGT.RailAdamantiumDetector = b; return b;});
		
		// F12-followup (block-split): рецепты рельсов — deferItemInit (server-start): поля BlocksGT.RailX заселены
		// RegisterEvent'ом (registerBlockLazy выше) + компоненты связаны (ST.make).
		gregapi.GT_API.deferItemInit(() -> {
		if (MD.RC.mLoaded) {
			CR.shaped(ST.make((Block)BlocksGT.RailSteel                     , 12, 0), CR.DEF_REV_NCC, "R R", "RBR", "R R", 'R', OP.railGt.dat(ANY.Steel         ), 'B', IL.RC_Bed_Wood);
			CR.shaped(ST.make((Block)BlocksGT.RailAluminium                 , 12, 0), CR.DEF_REV_NCC, "R R", "RBR", "R R", 'R', OP.railGt.dat(MT.Al             ), 'B', IL.RC_Bed_Wood);
			CR.shaped(ST.make((Block)BlocksGT.RailMagnalium                 , 12, 0), CR.DEF_REV_NCC, "R R", "RBR", "R R", 'R', OP.railGt.dat(MT.Magnalium      ), 'B', IL.RC_Bed_Wood);
			CR.shaped(ST.make((Block)BlocksGT.RailBronze                    , 12, 0), CR.DEF_REV_NCC, "R R", "RBR", "R R", 'R', OP.railGt.dat(MT.Bronze         ), 'B', IL.RC_Bed_Wood);
			CR.shaped(ST.make((Block)BlocksGT.RailStainlessSteel            , 12, 0), CR.DEF_REV_NCC, "R R", "RBR", "R R", 'R', OP.railGt.dat(MT.StainlessSteel ), 'B', IL.RC_Bed_Wood);
			CR.shaped(ST.make((Block)BlocksGT.RailTitanium                  , 12, 0), CR.DEF_REV_NCC, "R R", "RBR", "R R", 'R', OP.railGt.dat(MT.Ti             ), 'B', IL.RC_Bed_Wood);
			CR.shaped(ST.make((Block)BlocksGT.RailTungsten                  , 12, 0), CR.DEF_REV_NCC, "R R", "RBR", "R R", 'R', OP.railGt.dat(ANY.W             ), 'B', IL.RC_Bed_Wood);
			CR.shaped(ST.make((Block)BlocksGT.RailTungstenSteel             , 12, 0), CR.DEF_REV_NCC, "R R", "RBR", "R R", 'R', OP.railGt.dat(MT.TungstenSteel  ), 'B', IL.RC_Bed_Wood);
			CR.shaped(ST.make((Block)BlocksGT.RailTungstenCarbide           , 12, 0), CR.DEF_REV_NCC, "R R", "RBR", "R R", 'R', OP.railGt.dat(MT.TungstenCarbide), 'B', IL.RC_Bed_Wood);
			CR.shaped(ST.make((Block)BlocksGT.RailAdamantium                , 12, 0), CR.DEF_REV_NCC, "R R", "RBR", "R R", 'R', OP.railGt.dat(MT.Ad             ), 'B', IL.RC_Bed_Wood);
			
			CR.shaped(ST.make((Block)BlocksGT.RailSteelBooster              , 12, 0), CR.DEF_REV_NCC, "RDR", "GBG", "RDR", 'R', OP.railGt.dat(ANY.Steel         ), 'B', IL.RC_Bed_Wood, 'D', OD.itemRedstone, 'G', OP.railGt.dat(MT.Au));
			CR.shaped(ST.make((Block)BlocksGT.RailAluminiumBooster          , 12, 0), CR.DEF_REV_NCC, "RDR", "GBG", "RDR", 'R', OP.railGt.dat(MT.Al             ), 'B', IL.RC_Bed_Wood, 'D', OD.itemRedstone, 'G', OP.railGt.dat(MT.Ag));
			CR.shaped(ST.make((Block)BlocksGT.RailMagnaliumBooster          , 12, 0), CR.DEF_REV_NCC, "RDR", "GBG", "RDR", 'R', OP.railGt.dat(MT.Magnalium      ), 'B', IL.RC_Bed_Wood, 'D', OD.itemRedstone, 'G', OP.railGt.dat(MT.Ag));
			CR.shaped(ST.make((Block)BlocksGT.RailBronzeBooster             , 12, 0), CR.DEF_REV_NCC, "RDR", "GBG", "RDR", 'R', OP.railGt.dat(MT.Bronze         ), 'B', IL.RC_Bed_Wood, 'D', OD.itemRedstone, 'G', OP.railGt.dat(MT.Ag));
			CR.shaped(ST.make((Block)BlocksGT.RailStainlessSteelBooster     , 12, 0), CR.DEF_REV_NCC, "RDR", "GBG", "RDR", 'R', OP.railGt.dat(MT.StainlessSteel ), 'B', IL.RC_Bed_Wood, 'D', OD.itemRedstone, 'G', OP.railGt.dat(MT.Au));
			CR.shaped(ST.make((Block)BlocksGT.RailTitaniumBooster           , 12, 0), CR.DEF_REV_NCC, "RDR", "GBG", "RDR", 'R', OP.railGt.dat(MT.Ti             ), 'B', IL.RC_Bed_Wood, 'D', OD.itemRedstone, 'G', OP.railGt.dat(MT.Electrum));
			CR.shaped(ST.make((Block)BlocksGT.RailTungstenBooster           , 12, 0), CR.DEF_REV_NCC, "RDR", "GBG", "RDR", 'R', OP.railGt.dat(ANY.W             ), 'B', IL.RC_Bed_Wood, 'D', OD.itemRedstone, 'G', OP.railGt.dat(MT.Electrum));
			CR.shaped(ST.make((Block)BlocksGT.RailTungstenSteelBooster      , 12, 0), CR.DEF_REV_NCC, "RDR", "GBG", "RDR", 'R', OP.railGt.dat(MT.TungstenSteel  ), 'B', IL.RC_Bed_Wood, 'D', OD.itemRedstone, 'G', OP.railGt.dat(MT.Pt));
			CR.shaped(ST.make((Block)BlocksGT.RailTungstenCarbideBooster    , 12, 0), CR.DEF_REV_NCC, "RDR", "GBG", "RDR", 'R', OP.railGt.dat(MT.TungstenCarbide), 'B', IL.RC_Bed_Wood, 'D', OD.itemRedstone, 'G', OP.railGt.dat(MT.Pt));
			CR.shaped(ST.make((Block)BlocksGT.RailAdamantiumBooster         , 12, 0), CR.DEF_REV_NCC, "RDR", "GBG", "RDR", 'R', OP.railGt.dat(MT.Ad             ), 'B', IL.RC_Bed_Wood, 'D', OD.itemRedstone, 'G', OP.railGt.dat(MT.Os));
			
			CR.shaped(ST.make((Block)BlocksGT.RailSteelDetector             , 12, 0), CR.DEF_REV_NCC, "RBR", "RPR", "RDR", 'R', OP.railGt.dat(ANY.Steel         ), 'B', IL.RC_Bed_Wood, 'D', OD.itemRedstone, 'P', OD.pressurePlateStone);
			CR.shaped(ST.make((Block)BlocksGT.RailAluminiumDetector         , 12, 0), CR.DEF_REV_NCC, "RBR", "RPR", "RDR", 'R', OP.railGt.dat(MT.Al             ), 'B', IL.RC_Bed_Wood, 'D', OD.itemRedstone, 'P', OD.pressurePlateStone);
			CR.shaped(ST.make((Block)BlocksGT.RailMagnaliumDetector         , 12, 0), CR.DEF_REV_NCC, "RBR", "RPR", "RDR", 'R', OP.railGt.dat(MT.Magnalium      ), 'B', IL.RC_Bed_Wood, 'D', OD.itemRedstone, 'P', OD.pressurePlateStone);
			CR.shaped(ST.make((Block)BlocksGT.RailBronzeDetector            , 12, 0), CR.DEF_REV_NCC, "RBR", "RPR", "RDR", 'R', OP.railGt.dat(MT.Bronze         ), 'B', IL.RC_Bed_Wood, 'D', OD.itemRedstone, 'P', OD.pressurePlateStone);
			CR.shaped(ST.make((Block)BlocksGT.RailStainlessSteelDetector    , 12, 0), CR.DEF_REV_NCC, "RBR", "RPR", "RDR", 'R', OP.railGt.dat(MT.StainlessSteel ), 'B', IL.RC_Bed_Wood, 'D', OD.itemRedstone, 'P', OD.pressurePlateStone);
			CR.shaped(ST.make((Block)BlocksGT.RailTitaniumDetector          , 12, 0), CR.DEF_REV_NCC, "RBR", "RPR", "RDR", 'R', OP.railGt.dat(MT.Ti             ), 'B', IL.RC_Bed_Wood, 'D', OD.itemRedstone, 'P', OD.pressurePlateStone);
			CR.shaped(ST.make((Block)BlocksGT.RailTungstenDetector          , 12, 0), CR.DEF_REV_NCC, "RBR", "RPR", "RDR", 'R', OP.railGt.dat(ANY.W             ), 'B', IL.RC_Bed_Wood, 'D', OD.itemRedstone, 'P', OD.pressurePlateStone);
			CR.shaped(ST.make((Block)BlocksGT.RailTungstenSteelDetector     , 12, 0), CR.DEF_REV_NCC, "RBR", "RPR", "RDR", 'R', OP.railGt.dat(MT.TungstenSteel  ), 'B', IL.RC_Bed_Wood, 'D', OD.itemRedstone, 'P', OD.pressurePlateStone);
			CR.shaped(ST.make((Block)BlocksGT.RailTungstenCarbideDetector   , 12, 0), CR.DEF_REV_NCC, "RBR", "RPR", "RDR", 'R', OP.railGt.dat(MT.TungstenCarbide), 'B', IL.RC_Bed_Wood, 'D', OD.itemRedstone, 'P', OD.pressurePlateStone);
			CR.shaped(ST.make((Block)BlocksGT.RailAdamantiumDetector        , 12, 0), CR.DEF_REV_NCC, "RBR", "RPR", "RDR", 'R', OP.railGt.dat(MT.Ad             ), 'B', IL.RC_Bed_Wood, 'D', OD.itemRedstone, 'P', OD.pressurePlateStone);
		} else {
			CR.shaped(ST.make((Block)BlocksGT.RailSteel                     ,  4, 0), CR.DEF_REV_NCC, "RSR", "RSR", "RSR", 'R', OP.railGt.dat(ANY.Steel         ), 'S', OP.stick.dat(ANY.WoodTreated));
			CR.shaped(ST.make((Block)BlocksGT.RailAluminium                 ,  4, 0), CR.DEF_REV_NCC, "RSR", "RSR", "RSR", 'R', OP.railGt.dat(MT.Al             ), 'S', OP.stick.dat(ANY.WoodTreated));
			CR.shaped(ST.make((Block)BlocksGT.RailMagnalium                 ,  4, 0), CR.DEF_REV_NCC, "RSR", "RSR", "RSR", 'R', OP.railGt.dat(MT.Magnalium      ), 'S', OP.stick.dat(ANY.WoodTreated));
			CR.shaped(ST.make((Block)BlocksGT.RailBronze                    ,  4, 0), CR.DEF_REV_NCC, "RSR", "RSR", "RSR", 'R', OP.railGt.dat(MT.Bronze         ), 'S', OP.stick.dat(ANY.WoodTreated));
			CR.shaped(ST.make((Block)BlocksGT.RailStainlessSteel            ,  4, 0), CR.DEF_REV_NCC, "RSR", "RSR", "RSR", 'R', OP.railGt.dat(MT.StainlessSteel ), 'S', OP.stick.dat(ANY.WoodTreated));
			CR.shaped(ST.make((Block)BlocksGT.RailTitanium                  ,  4, 0), CR.DEF_REV_NCC, "RSR", "RSR", "RSR", 'R', OP.railGt.dat(MT.Ti             ), 'S', OP.stick.dat(ANY.WoodTreated));
			CR.shaped(ST.make((Block)BlocksGT.RailTungsten                  ,  4, 0), CR.DEF_REV_NCC, "RSR", "RSR", "RSR", 'R', OP.railGt.dat(ANY.W             ), 'S', OP.stick.dat(ANY.WoodTreated));
			CR.shaped(ST.make((Block)BlocksGT.RailTungstenSteel             ,  4, 0), CR.DEF_REV_NCC, "RSR", "RSR", "RSR", 'R', OP.railGt.dat(MT.TungstenSteel  ), 'S', OP.stick.dat(ANY.WoodTreated));
			CR.shaped(ST.make((Block)BlocksGT.RailTungstenCarbide           ,  4, 0), CR.DEF_REV_NCC, "RSR", "RSR", "RSR", 'R', OP.railGt.dat(MT.TungstenCarbide), 'S', OP.stick.dat(ANY.WoodTreated));
			CR.shaped(ST.make((Block)BlocksGT.RailAdamantium                ,  4, 0), CR.DEF_REV_NCC, "RSR", "RSR", "RSR", 'R', OP.railGt.dat(MT.Ad             ), 'S', OP.stick.dat(ANY.WoodTreated));
			
			CR.shaped(ST.make((Block)BlocksGT.RailSteelBooster              ,  4, 0), CR.DEF_REV_NCC, "RSR", "GDG", "RSR", 'R', OP.railGt.dat(ANY.Steel         ), 'S', OP.stick.dat(ANY.WoodTreated), 'D', OD.itemRedstone, 'G', OP.railGt.dat(MT.Au));
			CR.shaped(ST.make((Block)BlocksGT.RailAluminiumBooster          ,  4, 0), CR.DEF_REV_NCC, "RSR", "GDG", "RSR", 'R', OP.railGt.dat(MT.Al             ), 'S', OP.stick.dat(ANY.WoodTreated), 'D', OD.itemRedstone, 'G', OP.railGt.dat(MT.Ag));
			CR.shaped(ST.make((Block)BlocksGT.RailMagnaliumBooster          ,  4, 0), CR.DEF_REV_NCC, "RSR", "GDG", "RSR", 'R', OP.railGt.dat(MT.Magnalium      ), 'S', OP.stick.dat(ANY.WoodTreated), 'D', OD.itemRedstone, 'G', OP.railGt.dat(MT.Ag));
			CR.shaped(ST.make((Block)BlocksGT.RailBronzeBooster             ,  4, 0), CR.DEF_REV_NCC, "RSR", "GDG", "RSR", 'R', OP.railGt.dat(MT.Bronze         ), 'S', OP.stick.dat(ANY.WoodTreated), 'D', OD.itemRedstone, 'G', OP.railGt.dat(MT.Ag));
			CR.shaped(ST.make((Block)BlocksGT.RailStainlessSteelBooster     ,  4, 0), CR.DEF_REV_NCC, "RSR", "GDG", "RSR", 'R', OP.railGt.dat(MT.StainlessSteel ), 'S', OP.stick.dat(ANY.WoodTreated), 'D', OD.itemRedstone, 'G', OP.railGt.dat(MT.Au));
			CR.shaped(ST.make((Block)BlocksGT.RailTitaniumBooster           ,  4, 0), CR.DEF_REV_NCC, "RSR", "GDG", "RSR", 'R', OP.railGt.dat(MT.Ti             ), 'S', OP.stick.dat(ANY.WoodTreated), 'D', OD.itemRedstone, 'G', OP.railGt.dat(MT.Electrum));
			CR.shaped(ST.make((Block)BlocksGT.RailTungstenBooster           ,  4, 0), CR.DEF_REV_NCC, "RSR", "GDG", "RSR", 'R', OP.railGt.dat(ANY.W             ), 'S', OP.stick.dat(ANY.WoodTreated), 'D', OD.itemRedstone, 'G', OP.railGt.dat(MT.Electrum));
			CR.shaped(ST.make((Block)BlocksGT.RailTungstenSteelBooster      ,  4, 0), CR.DEF_REV_NCC, "RSR", "GDG", "RSR", 'R', OP.railGt.dat(MT.TungstenSteel  ), 'S', OP.stick.dat(ANY.WoodTreated), 'D', OD.itemRedstone, 'G', OP.railGt.dat(MT.Pt));
			CR.shaped(ST.make((Block)BlocksGT.RailTungstenCarbideBooster    ,  4, 0), CR.DEF_REV_NCC, "RSR", "GDG", "RSR", 'R', OP.railGt.dat(MT.TungstenCarbide), 'S', OP.stick.dat(ANY.WoodTreated), 'D', OD.itemRedstone, 'G', OP.railGt.dat(MT.Pt));
			CR.shaped(ST.make((Block)BlocksGT.RailAdamantiumBooster         ,  4, 0), CR.DEF_REV_NCC, "RSR", "GDG", "RSR", 'R', OP.railGt.dat(MT.Ad             ), 'S', OP.stick.dat(ANY.WoodTreated), 'D', OD.itemRedstone, 'G', OP.railGt.dat(MT.Os));
			
			CR.shaped(ST.make((Block)BlocksGT.RailSteelDetector             ,  4, 0), CR.DEF_REV_NCC, "RSR", "RPR", "RDR", 'R', OP.railGt.dat(ANY.Steel         ), 'S', OP.stick.dat(ANY.WoodTreated), 'D', OD.itemRedstone, 'P', OD.pressurePlateStone);
			CR.shaped(ST.make((Block)BlocksGT.RailAluminiumDetector         ,  4, 0), CR.DEF_REV_NCC, "RSR", "RPR", "RDR", 'R', OP.railGt.dat(MT.Al             ), 'S', OP.stick.dat(ANY.WoodTreated), 'D', OD.itemRedstone, 'P', OD.pressurePlateStone);
			CR.shaped(ST.make((Block)BlocksGT.RailMagnaliumDetector         ,  4, 0), CR.DEF_REV_NCC, "RSR", "RPR", "RDR", 'R', OP.railGt.dat(MT.Magnalium      ), 'S', OP.stick.dat(ANY.WoodTreated), 'D', OD.itemRedstone, 'P', OD.pressurePlateStone);
			CR.shaped(ST.make((Block)BlocksGT.RailBronzeDetector            ,  4, 0), CR.DEF_REV_NCC, "RSR", "RPR", "RDR", 'R', OP.railGt.dat(MT.Bronze         ), 'S', OP.stick.dat(ANY.WoodTreated), 'D', OD.itemRedstone, 'P', OD.pressurePlateStone);
			CR.shaped(ST.make((Block)BlocksGT.RailStainlessSteelDetector    ,  4, 0), CR.DEF_REV_NCC, "RSR", "RPR", "RDR", 'R', OP.railGt.dat(MT.StainlessSteel ), 'S', OP.stick.dat(ANY.WoodTreated), 'D', OD.itemRedstone, 'P', OD.pressurePlateStone);
			CR.shaped(ST.make((Block)BlocksGT.RailTitaniumDetector          ,  4, 0), CR.DEF_REV_NCC, "RSR", "RPR", "RDR", 'R', OP.railGt.dat(MT.Ti             ), 'S', OP.stick.dat(ANY.WoodTreated), 'D', OD.itemRedstone, 'P', OD.pressurePlateStone);
			CR.shaped(ST.make((Block)BlocksGT.RailTungstenDetector          ,  4, 0), CR.DEF_REV_NCC, "RSR", "RPR", "RDR", 'R', OP.railGt.dat(ANY.W             ), 'S', OP.stick.dat(ANY.WoodTreated), 'D', OD.itemRedstone, 'P', OD.pressurePlateStone);
			CR.shaped(ST.make((Block)BlocksGT.RailTungstenSteelDetector     ,  4, 0), CR.DEF_REV_NCC, "RSR", "RPR", "RDR", 'R', OP.railGt.dat(MT.TungstenSteel  ), 'S', OP.stick.dat(ANY.WoodTreated), 'D', OD.itemRedstone, 'P', OD.pressurePlateStone);
			CR.shaped(ST.make((Block)BlocksGT.RailTungstenCarbideDetector   ,  4, 0), CR.DEF_REV_NCC, "RSR", "RPR", "RDR", 'R', OP.railGt.dat(MT.TungstenCarbide), 'S', OP.stick.dat(ANY.WoodTreated), 'D', OD.itemRedstone, 'P', OD.pressurePlateStone);
			CR.shaped(ST.make((Block)BlocksGT.RailAdamantiumDetector        ,  4, 0), CR.DEF_REV_NCC, "RSR", "RPR", "RDR", 'R', OP.railGt.dat(MT.Ad             ), 'S', OP.stick.dat(ANY.WoodTreated), 'D', OD.itemRedstone, 'P', OD.pressurePlateStone);
			
			CR.shaped(ST.make(Blocks.RAIL          ,  4, 0), DEF_REV_NCC | DEL_OTHER_SHAPED_RECIPES, "RSR", "RSR", "RSR", 'R', OP.railGt.dat(ANY.Fe), 'S', OP.stick.dat(ANY.WoodTreated));
			CR.shaped(ST.make(Blocks.POWERED_RAIL   ,  4, 0), DEF_REV_NCC | DEL_OTHER_SHAPED_RECIPES, "RSR", "GDG", "RSR", 'R', OP.railGt.dat(ANY.Fe), 'S', OP.stick.dat(ANY.WoodTreated), 'D', OD.itemRedstone, 'G', OP.railGt.dat(MT.Au));
			CR.shaped(ST.make(Blocks.DETECTOR_RAIL ,  4, 0), DEF_REV_NCC | DEL_OTHER_SHAPED_RECIPES, "RSR", "RPR", "RDR", 'R', OP.railGt.dat(ANY.Fe), 'S', OP.stick.dat(ANY.WoodTreated), 'D', OD.itemRedstone, 'P', OD.pressurePlateStone);
			
			CR.shaped(ST.make(Blocks.ACTIVATOR_RAIL,  1, 0), DEF | DEL_OTHER_SHAPED_RECIPES, "RSR", "RTR", "RSR", 'R', OP.railGt.dat(MT.Al             ), 'S', OP.stick.dat(ANY.WoodTreated), 'T', OD.craftingRedstoneTorch);
			CR.shaped(ST.make(Blocks.ACTIVATOR_RAIL,  1, 0), DEF | DEL_OTHER_SHAPED_RECIPES, "RSR", "RTR", "RSR", 'R', OP.railGt.dat(MT.Magnalium      ), 'S', OP.stick.dat(ANY.WoodTreated), 'T', OD.craftingRedstoneTorch);
			CR.shaped(ST.make(Blocks.ACTIVATOR_RAIL,  1, 0), DEF | DEL_OTHER_SHAPED_RECIPES, "RSR", "RTR", "RSR", 'R', OP.railGt.dat(MT.Bronze         ), 'S', OP.stick.dat(ANY.WoodTreated), 'T', OD.craftingRedstoneTorch);
			CR.shaped(ST.make(Blocks.ACTIVATOR_RAIL,  2, 0), DEF | DEL_OTHER_SHAPED_RECIPES, "RSR", "RTR", "RSR", 'R', OP.railGt.dat(ANY.Fe            ), 'S', OP.stick.dat(ANY.WoodTreated), 'T', OD.craftingRedstoneTorch);
			CR.shaped(ST.make(Blocks.ACTIVATOR_RAIL,  3, 0), DEF | DEL_OTHER_SHAPED_RECIPES, "RSR", "RTR", "RSR", 'R', OP.railGt.dat(ANY.Steel         ), 'S', OP.stick.dat(ANY.WoodTreated), 'T', OD.craftingRedstoneTorch);
			CR.shaped(ST.make(Blocks.ACTIVATOR_RAIL,  3, 0), DEF | DEL_OTHER_SHAPED_RECIPES, "RSR", "RTR", "RSR", 'R', OP.railGt.dat(MT.HSLA           ), 'S', OP.stick.dat(ANY.WoodTreated), 'T', OD.craftingRedstoneTorch);
			CR.shaped(ST.make(Blocks.ACTIVATOR_RAIL,  4, 0), DEF | DEL_OTHER_SHAPED_RECIPES, "RSR", "RTR", "RSR", 'R', OP.railGt.dat(MT.StainlessSteel ), 'S', OP.stick.dat(ANY.WoodTreated), 'T', OD.craftingRedstoneTorch);
			CR.shaped(ST.make(Blocks.ACTIVATOR_RAIL,  6, 0), DEF | DEL_OTHER_SHAPED_RECIPES, "RSR", "RTR", "RSR", 'R', OP.railGt.dat(MT.Ti             ), 'S', OP.stick.dat(ANY.WoodTreated), 'T', OD.craftingRedstoneTorch);
			CR.shaped(ST.make(Blocks.ACTIVATOR_RAIL,  6, 0), DEF | DEL_OTHER_SHAPED_RECIPES, "RSR", "RTR", "RSR", 'R', OP.railGt.dat(ANY.W             ), 'S', OP.stick.dat(ANY.WoodTreated), 'T', OD.craftingRedstoneTorch);
			CR.shaped(ST.make(Blocks.ACTIVATOR_RAIL, 12, 0), DEF | DEL_OTHER_SHAPED_RECIPES, "RSR", "RTR", "RSR", 'R', OP.railGt.dat(MT.TungstenSteel  ), 'S', OP.stick.dat(ANY.WoodTreated), 'T', OD.craftingRedstoneTorch);
			CR.shaped(ST.make(Blocks.ACTIVATOR_RAIL, 12, 0), DEF | DEL_OTHER_SHAPED_RECIPES, "RSR", "RTR", "RSR", 'R', OP.railGt.dat(MT.TungstenCarbide), 'S', OP.stick.dat(ANY.WoodTreated), 'T', OD.craftingRedstoneTorch);
			CR.shaped(ST.make(Blocks.ACTIVATOR_RAIL, 64, 0), DEF | DEL_OTHER_SHAPED_RECIPES, "RSR", "RTR", "RSR", 'R', OP.railGt.dat(MT.Ad             ), 'S', OP.stick.dat(ANY.WoodTreated), 'T', OD.craftingRedstoneTorch);
		}
		});
	}
}

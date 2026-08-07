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
 *
 * Modified in 2026 for the GregTech 6 NeoForge port
 * (https://github.com/wolfram0108/gregtech6_w): ported from Minecraft 1.7.10 / Forge
 * to Minecraft 26.1.2 / NeoForge.
 */

package gregtech.loaders.a;

import gregapi.data.MD;
import gregapi.data.MT;
import gregapi.data.OP;
import gregapi.data.TD;
import gregapi.GT_API;
import gregapi.item.prefixitem.PrefixItem;
import gregapi.item.prefixitem.PrefixItemChain;
import gregapi.item.prefixitem.PrefixItemProjectile;
import gregapi.item.prefixitem.PrefixItemRing;
import gregapi.util.ST;
import gregtech.entities.projectiles.EntityArrow_Material;
import gregtech.items.*;

import static gregapi.data.CS.*;

public class Loader_Items implements Runnable {
	@Override
	public void run() {
		
		GT_API.registerItemLazy(MD.GT.mID, "gt.multiitem.technological", () -> ItemsGT.TECH = new MultiItemTechnological(MD.GT.mID, "gt.multiitem.technological"));
		GT_API.registerItemLazy(MD.GT.mID, "gt.multiitem.randomtools", () -> ItemsGT.TOOLS = new MultiItemRandomTools  (MD.GT.mID, "gt.multiitem.randomtools"));
		GT_API.registerItemLazy(MD.GT.mID, "gt.multiitem.cans", () -> ItemsGT.CANS = new MultiItemCans         (MD.GT.mID, "gt.multiitem.cans"));
		GT_API.registerItemLazy(MD.GT.mID, "gt.multiitem.food", () -> ItemsGT.FOOD = new MultiItemFood         (MD.GT.mID, "gt.multiitem.food"));
		GT_API.registerItemLazy(MD.GT.mID, "gt.multiitem.bottles", () -> ItemsGT.BOTTLES = new MultiItemBottles      (MD.GT.mID, "gt.multiitem.bottles"));
		GT_API.registerItemLazy(MD.GT.mID, "gt.multiitem.books", () -> ItemsGT.BOOKS = new MultiItemBooks        (MD.GT.mID, "gt.multiitem.books"));
		GT_API.registerItemLazy(MD.GT.mID, "gt.multiitem.bumblebee", () -> ItemsGT.BUMBLEBEES = new MultiItemBumbles      (MD.GT.mID, "gt.multiitem.bumblebee"));
		
		gregapi.GT_API.deferItemInit(() -> {
		ItemsGT.ALL_MULTI_ITEMS[0] = ItemsGT.TECH;
		ItemsGT.ALL_MULTI_ITEMS[1] = ItemsGT.TOOLS;
		ItemsGT.ALL_MULTI_ITEMS[2] = ItemsGT.CANS;
		ItemsGT.ALL_MULTI_ITEMS[3] = ItemsGT.FOOD;
		ItemsGT.ALL_MULTI_ITEMS[4] = ItemsGT.BOTTLES;
		ItemsGT.ALL_MULTI_ITEMS[5] = ItemsGT.BOOKS;
		ItemsGT.ALL_MULTI_ITEMS[6] = ItemsGT.BUMBLEBEES;
		});
		
		GT_API.registerItemLazy(MD.GT.mID, "gt.meta.dust", () -> {PrefixItem p = new PrefixItem(MD.GT, "gt.meta.dust"                        , OP.dust                           ); if (COMPAT_FR != null) gregapi.GT_API.deferItemInit(() -> COMPAT_FR.addToBackpacks("miner", ST.make(p, 1, W))); return p;});
		GT_API.registerItemLazy(MD.GT.mID, "gt.meta.dustSmall", () -> {PrefixItem p = new PrefixItem(MD.GT, "gt.meta.dustSmall"                   , OP.dustSmall                      ); if (COMPAT_FR != null) gregapi.GT_API.deferItemInit(() -> COMPAT_FR.addToBackpacks("miner", ST.make(p, 1, W))); return p;});
		GT_API.registerItemLazy(MD.GT.mID, "gt.meta.dustTiny", () -> {PrefixItem p = new PrefixItem(MD.GT, "gt.meta.dustTiny"                    , OP.dustTiny                       ); if (COMPAT_FR != null) gregapi.GT_API.deferItemInit(() -> COMPAT_FR.addToBackpacks("miner", ST.make(p, 1, W))); return p;});
		GT_API.registerItemLazy(MD.GT.mID, "gt.meta.dustDiv72", () -> {PrefixItem p = new PrefixItem(MD.GT, "gt.meta.dustDiv72"                   , OP.dustDiv72                      ); if (COMPAT_FR != null) gregapi.GT_API.deferItemInit(() -> COMPAT_FR.addToBackpacks("miner", ST.make(p, 1, W))); return p;});
		GT_API.registerItemLazy(MD.GT.mID, "gt.meta.dustImpure", () -> {PrefixItem p = new PrefixItem(MD.GT, "gt.meta.dustImpure"                  , OP.dustImpure                     ); if (COMPAT_FR != null) gregapi.GT_API.deferItemInit(() -> COMPAT_FR.addToBackpacks("miner", ST.make(p, 1, W))); return p;});
		GT_API.registerItemLazy(MD.GT.mID, "gt.meta.crushed", () -> {PrefixItem p = new PrefixItem(MD.GT, "gt.meta.crushed"                     , OP.crushed                        ); if (COMPAT_FR != null) gregapi.GT_API.deferItemInit(() -> COMPAT_FR.addToBackpacks("miner", ST.make(p, 1, W))); return p;});
		GT_API.registerItemLazy(MD.GT.mID, "gt.meta.crushedTiny", () -> {PrefixItem p = new PrefixItem(MD.GT, "gt.meta.crushedTiny"                 , OP.crushedTiny                    ); if (COMPAT_FR != null) gregapi.GT_API.deferItemInit(() -> COMPAT_FR.addToBackpacks("miner", ST.make(p, 1, W))); return p;});
		GT_API.registerItemLazy(MD.GT.mID, "gt.meta.crushedPurified", () -> {PrefixItem p = new PrefixItem(MD.GT, "gt.meta.crushedPurified"             , OP.crushedPurified                ); if (COMPAT_FR != null) gregapi.GT_API.deferItemInit(() -> COMPAT_FR.addToBackpacks("miner", ST.make(p, 1, W))); return p;});
		GT_API.registerItemLazy(MD.GT.mID, "gt.meta.crushedPurifiedTiny", () -> {PrefixItem p = new PrefixItem(MD.GT, "gt.meta.crushedPurifiedTiny"         , OP.crushedPurifiedTiny            ); if (COMPAT_FR != null) gregapi.GT_API.deferItemInit(() -> COMPAT_FR.addToBackpacks("miner", ST.make(p, 1, W))); return p;});
		GT_API.registerItemLazy(MD.GT.mID, "gt.meta.crushedCentrifuged", () -> {PrefixItem p = new PrefixItem(MD.GT, "gt.meta.crushedCentrifuged"          , OP.crushedCentrifuged             ); if (COMPAT_FR != null) gregapi.GT_API.deferItemInit(() -> COMPAT_FR.addToBackpacks("miner", ST.make(p, 1, W))); return p;});
		GT_API.registerItemLazy(MD.GT.mID, "gt.meta.crushedCentrifugedTiny", () -> {PrefixItem p = new PrefixItem(MD.GT, "gt.meta.crushedCentrifugedTiny"      , OP.crushedCentrifugedTiny         ); if (COMPAT_FR != null) gregapi.GT_API.deferItemInit(() -> COMPAT_FR.addToBackpacks("miner", ST.make(p, 1, W))); return p;});
		
		GT_API.registerItemLazy(MD.GT.mID, "gt.meta.gemChipped", () -> {PrefixItem p = new PrefixItem(MD.GT, "gt.meta.gemChipped"                  , OP.gemChipped                     ); if (COMPAT_FR != null) gregapi.GT_API.deferItemInit(() -> COMPAT_FR.addToBackpacks("miner", ST.make(p, 1, W))); return p;});
		GT_API.registerItemLazy(MD.GT.mID, "gt.meta.gemFlawed", () -> {PrefixItem p = new PrefixItem(MD.GT, "gt.meta.gemFlawed"                   , OP.gemFlawed                      ); if (COMPAT_FR != null) gregapi.GT_API.deferItemInit(() -> COMPAT_FR.addToBackpacks("miner", ST.make(p, 1, W))); return p;});
		GT_API.registerItemLazy(MD.GT.mID, "gt.meta.gem", () -> {PrefixItem p = new PrefixItem(MD.GT, "gt.meta.gem"                         , OP.gem                            ); if (COMPAT_FR != null) gregapi.GT_API.deferItemInit(() -> COMPAT_FR.addToBackpacks("miner", ST.make(p, 1, W))); return p;});
		GT_API.registerItemLazy(MD.GT.mID, "gt.meta.gemFlawless", () -> {PrefixItem p = new PrefixItem(MD.GT, "gt.meta.gemFlawless"                 , OP.gemFlawless                    ); if (COMPAT_FR != null) gregapi.GT_API.deferItemInit(() -> COMPAT_FR.addToBackpacks("miner", ST.make(p, 1, W))); return p;});
		GT_API.registerItemLazy(MD.GT.mID, "gt.meta.gemExquisite", () -> {PrefixItem p = new PrefixItem(MD.GT, "gt.meta.gemExquisite"                , OP.gemExquisite                   ); if (COMPAT_FR != null) gregapi.GT_API.deferItemInit(() -> COMPAT_FR.addToBackpacks("miner", ST.make(p, 1, W))); return p;});
		GT_API.registerItemLazy(MD.GT.mID, "gt.meta.gemLegendary", () -> {PrefixItem p = new PrefixItem(MD.GT, "gt.meta.gemLegendary"                , OP.gemLegendary                   ); if (COMPAT_FR != null) gregapi.GT_API.deferItemInit(() -> COMPAT_FR.addToBackpacks("miner", ST.make(p, 1, W))); return p;});
		GT_API.registerItemLazy(MD.GT.mID, "gt.meta.boule", () -> new PrefixItem(MD.GT, "gt.meta.boule"                       , OP.bouleGt                        ));
		
		GT_API.registerItemLazy(MD.GT.mID, "gt.meta.nugget", () -> {PrefixItem p = new PrefixItem(MD.GT, "gt.meta.nugget"                      , OP.nugget                         ); if (COMPAT_FR != null) gregapi.GT_API.deferItemInit(() -> COMPAT_FR.addToBackpacks("miner", ST.make(p, 1, W))); return p;});
		GT_API.registerItemLazy(MD.GT.mID, "gt.meta.chunkGt", () -> {PrefixItem p = new PrefixItem(MD.GT, "gt.meta.chunkGt"                     , OP.chunkGt                        ); if (COMPAT_FR != null) gregapi.GT_API.deferItemInit(() -> COMPAT_FR.addToBackpacks("miner", ST.make(p, 1, W))); return p;});
		GT_API.registerItemLazy(MD.GT.mID, "gt.meta.billet", () -> {PrefixItem p = new PrefixItem(MD.GT, "gt.meta.billet"                      , OP.billet                         ); if (COMPAT_FR != null) gregapi.GT_API.deferItemInit(() -> COMPAT_FR.addToBackpacks("miner", ST.make(p, 1, W))); return p;});
		GT_API.registerItemLazy(MD.GT.mID, "gt.meta.ingot", () -> {PrefixItem p = new PrefixItem(MD.GT, "gt.meta.ingot"                       , OP.ingot                          ); if (COMPAT_FR != null) gregapi.GT_API.deferItemInit(() -> COMPAT_FR.addToBackpacks("miner", ST.make(p, 1, W))); return p;});
		GT_API.registerItemLazy(MD.GT.mID, "gt.meta.ingotHot", () -> new PrefixItem(MD.GT, "gt.meta.ingotHot"                    , OP.ingotHot                       ));
		GT_API.registerItemLazy(MD.GT.mID, "gt.meta.ingotDouble", () -> new PrefixItem(MD.GT, "gt.meta.ingotDouble"                 , OP.ingotDouble                    ));
		GT_API.registerItemLazy(MD.GT.mID, "gt.meta.ingotTriple", () -> new PrefixItem(MD.GT, "gt.meta.ingotTriple"                 , OP.ingotTriple                    ));
		GT_API.registerItemLazy(MD.GT.mID, "gt.meta.ingotQuadruple", () -> new PrefixItem(MD.GT, "gt.meta.ingotQuadruple"              , OP.ingotQuadruple                 ));
		GT_API.registerItemLazy(MD.GT.mID, "gt.meta.ingotQuintuple", () -> new PrefixItem(MD.GT, "gt.meta.ingotQuintuple"              , OP.ingotQuintuple                 ));
		
		GT_API.registerItemLazy(MD.GT.mID, "gt.meta.plateGemTiny", () -> {PrefixItem p = new PrefixItem(MD.GT, "gt.meta.plateGemTiny"                , OP.plateGemTiny                   ); if (COMPAT_FR != null) gregapi.GT_API.deferItemInit(() -> COMPAT_FR.addToBackpacks("builder", ST.make(p, 1, W))); return p;});
		GT_API.registerItemLazy(MD.GT.mID, "gt.meta.plateGem", () -> {PrefixItem p = new PrefixItem(MD.GT, "gt.meta.plateGem"                    , OP.plateGem                       ); if (COMPAT_FR != null) gregapi.GT_API.deferItemInit(() -> COMPAT_FR.addToBackpacks("builder", ST.make(p, 1, W))); return p;});
		GT_API.registerItemLazy(MD.GT.mID, "gt.meta.plateTiny", () -> {PrefixItem p = new PrefixItem(MD.GT, "gt.meta.plateTiny"                   , OP.plateTiny                      ); if (COMPAT_FR != null) gregapi.GT_API.deferItemInit(() -> COMPAT_FR.addToBackpacks("builder", ST.make(p, 1, W))); return p;});
		GT_API.registerItemLazy(MD.GT.mID, "gt.meta.plate", () -> {PrefixItem p = new PrefixItem(MD.GT, "gt.meta.plate"                       , OP.plate                          ); if (COMPAT_FR != null) gregapi.GT_API.deferItemInit(() -> COMPAT_FR.addToBackpacks("builder", ST.make(p, 1, W))); return p;});
		GT_API.registerItemLazy(MD.GT.mID, "gt.meta.plateDouble", () -> {PrefixItem p = new PrefixItem(MD.GT, "gt.meta.plateDouble"                 , OP.plateDouble                    ); if (COMPAT_FR != null) gregapi.GT_API.deferItemInit(() -> COMPAT_FR.addToBackpacks("builder", ST.make(p, 1, W))); return p;});
		GT_API.registerItemLazy(MD.GT.mID, "gt.meta.plateTriple", () -> {PrefixItem p = new PrefixItem(MD.GT, "gt.meta.plateTriple"                 , OP.plateTriple                    ); if (COMPAT_FR != null) gregapi.GT_API.deferItemInit(() -> COMPAT_FR.addToBackpacks("builder", ST.make(p, 1, W))); return p;});
		GT_API.registerItemLazy(MD.GT.mID, "gt.meta.plateQuadruple", () -> {PrefixItem p = new PrefixItem(MD.GT, "gt.meta.plateQuadruple"              , OP.plateQuadruple                 ); if (COMPAT_FR != null) gregapi.GT_API.deferItemInit(() -> COMPAT_FR.addToBackpacks("builder", ST.make(p, 1, W))); return p;});
		GT_API.registerItemLazy(MD.GT.mID, "gt.meta.plateQuintuple", () -> {PrefixItem p = new PrefixItem(MD.GT, "gt.meta.plateQuintuple"              , OP.plateQuintuple                 ); if (COMPAT_FR != null) gregapi.GT_API.deferItemInit(() -> COMPAT_FR.addToBackpacks("builder", ST.make(p, 1, W))); return p;});
		GT_API.registerItemLazy(MD.GT.mID, "gt.meta.plateDense", () -> {PrefixItem p = new PrefixItem(MD.GT, "gt.meta.plateDense"                  , OP.plateDense                     ); if (COMPAT_FR != null) gregapi.GT_API.deferItemInit(() -> COMPAT_FR.addToBackpacks("builder", ST.make(p, 1, W))); return p;});
		GT_API.registerItemLazy(MD.GT.mID, "gt.meta.plateCurved", () -> {PrefixItem p = new PrefixItem(MD.GT, "gt.meta.plateCurved"                 , OP.plateCurved                    ); if (COMPAT_FR != null) gregapi.GT_API.deferItemInit(() -> COMPAT_FR.addToBackpacks("builder", ST.make(p, 1, W))); return p;});
		
		GT_API.registerItemLazy(MD.GT.mID, "gt.meta.scrapGt", () -> new PrefixItem(MD.GT, "gt.meta.scrapGt"                     , OP.scrapGt                        ));
		GT_API.registerItemLazy(MD.GT.mID, "gt.meta.rockGt", () -> {PrefixItem p = new PrefixItem(MD.GT, "gt.meta.rockGt"                      , OP.rockGt                         ); if (COMPAT_FR != null) gregapi.GT_API.deferItemInit(() -> COMPAT_FR.addToBackpacks("miner", ST.make(p, 1, W))); return p;});
		GT_API.registerItemLazy(MD.GT.mID, "gt.meta.oreRaw", () -> {PrefixItem p = new PrefixItem(MD.GT, "gt.meta.oreRaw"                      , OP.oreRaw                         ); if (COMPAT_FR != null) gregapi.GT_API.deferItemInit(() -> COMPAT_FR.addToBackpacks("miner", ST.make(p, 1, W))); return p;});
		
		GT_API.registerItemLazy(MD.GT.mID, "gt.meta.gearGtSmall", () -> new PrefixItem(MD.GT, "gt.meta.gearGtSmall"                 , OP.gearGtSmall                    ));
		GT_API.registerItemLazy(MD.GT.mID, "gt.meta.gearGt", () -> new PrefixItem(MD.GT, "gt.meta.gearGt"                      , OP.gearGt                         ));
		GT_API.registerItemLazy(MD.GT.mID, "gt.meta.rotor", () -> new PrefixItem(MD.GT, "gt.meta.rotor"                       , OP.rotor                          ));
		GT_API.registerItemLazy(MD.GT.mID, "gt.meta.stick", () -> {PrefixItem p = new PrefixItem(MD.GT, "gt.meta.stick"                       , OP.stick                          ); if (COMPAT_FR != null) gregapi.GT_API.deferItemInit(() -> COMPAT_FR.addToBackpacks("forester", ST.make(p, 1, W))); return p;});
		GT_API.registerItemLazy(MD.GT.mID, "gt.meta.stickLong", () -> {PrefixItem p = new PrefixItem(MD.GT, "gt.meta.stickLong"                   , OP.stickLong                      ); if (COMPAT_FR != null) gregapi.GT_API.deferItemInit(() -> COMPAT_FR.addToBackpacks("forester", ST.make(p, 1, W))); return p;});
		GT_API.registerItemLazy(MD.GT.mID, "gt.meta.springSmall", () -> new PrefixItem(MD.GT, "gt.meta.springSmall"                 , OP.springSmall                    ));
		GT_API.registerItemLazy(MD.GT.mID, "gt.meta.spring", () -> new PrefixItem(MD.GT, "gt.meta.spring"                      , OP.spring                         ));
		GT_API.registerItemLazy(MD.GT.mID, "gt.meta.lens", () -> new PrefixItem(MD.GT, "gt.meta.lens"                        , OP.lens                           ));
		GT_API.registerItemLazy(MD.GT.mID, "gt.meta.round", () -> new PrefixItem(MD.GT, "gt.meta.round"                       , OP.round                          ));
		GT_API.registerItemLazy(MD.GT.mID, "gt.meta.bolt", () -> new PrefixItem(MD.GT, "gt.meta.bolt"                        , OP.bolt                           ));
		GT_API.registerItemLazy(MD.GT.mID, "gt.meta.screw", () -> new PrefixItem(MD.GT, "gt.meta.screw"                       , OP.screw                          ));
		GT_API.registerItemLazy(MD.GT.mID, "gt.meta.ring", () -> new PrefixItemRing(MD.GT, "gt.meta.ring"                    , OP.ring                           ));
		GT_API.registerItemLazy(MD.GT.mID, "gt.meta.chain", () -> new PrefixItemChain(MD.GT, "gt.meta.chain"                  , OP.chain                          ));
		GT_API.registerItemLazy(MD.GT.mID, "gt.meta.foil", () -> {PrefixItem p = new PrefixItem(MD.GT, "gt.meta.foil"                        , OP.foil                           ); if (COMPAT_FR != null) gregapi.GT_API.deferItemInit(() -> COMPAT_FR.addToBackpacks("builder", ST.make(p, 1, W))); return p;});
		GT_API.registerItemLazy(MD.GT.mID, "gt.meta.casingSmall", () -> new PrefixItem(MD.GT, "gt.meta.casingSmall"                 , OP.casingSmall                    ));
		GT_API.registerItemLazy(MD.GT.mID, "gt.meta.wireFine", () -> new PrefixItem(MD.GT, "gt.meta.wireFine"                    , OP.wireFine                       ));
		GT_API.registerItemLazy(MD.GT.mID, "gt.meta.minecartWheels", () -> new PrefixItem(MD.GT, "gt.meta.minecartWheels"              , OP.minecartWheels                 ));
		GT_API.registerItemLazy(MD.GT.mID, "gt.meta.railGt", () -> new PrefixItem(MD.GT, "gt.meta.railGt"                      , OP.railGt                         ));
		
		GT_API.registerItemLazy(MD.GT.mID, "gt.meta.plantGtBerry", () -> new PrefixItem(MD.GT, "gt.meta.plantGtBerry"                , OP.plantGtBerry                   ));
		GT_API.registerItemLazy(MD.GT.mID, "gt.meta.plantGtBlossom", () -> new PrefixItem(MD.GT, "gt.meta.plantGtBlossom"              , OP.plantGtBlossom                 ));
		GT_API.registerItemLazy(MD.GT.mID, "gt.meta.plantGtFiber", () -> new PrefixItem(MD.GT, "gt.meta.plantGtFiber"                , OP.plantGtFiber                   ));
		GT_API.registerItemLazy(MD.GT.mID, "gt.meta.plantGtTwig", () -> new PrefixItem(MD.GT, "gt.meta.plantGtTwig"                 , OP.plantGtTwig                    ));
		GT_API.registerItemLazy(MD.GT.mID, "gt.meta.plantGtWart", () -> new PrefixItem(MD.GT, "gt.meta.plantGtWart"                 , OP.plantGtWart                    ));
		
		GT_API.registerItemLazy(MD.GT.mID, "gt.meta.chemtube", () -> {PrefixItem p = new PrefixItem(MD.GT, "gt.meta.chemtube", OP.chemtube); p.mCraftingSound = SFX.IC_TREETAP; gregapi.GT_API.deferItemInit(() -> OP.chemtube.mContainerItem = ST.make(p, 1, MT.Empty.mID)); return p;});
		
		
		GT_API.registerItemLazy(MD.GT.mID, "gt.meta.toolHeadRawSword", () -> new PrefixItem(MD.GT, "gt.meta.toolHeadRawSword"            , OP.toolHeadRawSword               ));
		GT_API.registerItemLazy(MD.GT.mID, "gt.meta.toolHeadSword", () -> {PrefixItem p = new PrefixItem(MD.GT, "gt.meta.toolHeadSword"               , OP.toolHeadSword                  ); if (COMPAT_FR != null) gregapi.GT_API.deferItemInit(() -> COMPAT_FR.addToBackpacks("hunter", ST.make(p, 1, W))); return p;});
		GT_API.registerItemLazy(MD.GT.mID, "gt.meta.toolHeadRawPickaxe", () -> new PrefixItem(MD.GT, "gt.meta.toolHeadRawPickaxe"          , OP.toolHeadRawPickaxe             ));
		GT_API.registerItemLazy(MD.GT.mID, "gt.meta.toolHeadPickaxe", () -> {PrefixItem p = new PrefixItem(MD.GT, "gt.meta.toolHeadPickaxe"             , OP.toolHeadPickaxe                ); if (COMPAT_FR != null) gregapi.GT_API.deferItemInit(() -> COMPAT_FR.addToBackpacks("miner", ST.make(p, 1, W))); return p;});
		GT_API.registerItemLazy(MD.GT.mID, "gt.meta.toolHeadPickaxeGem", () -> {PrefixItem p = new PrefixItem(MD.GT, "gt.meta.toolHeadPickaxeGem"          , OP.toolHeadPickaxeGem             ); if (COMPAT_FR != null) gregapi.GT_API.deferItemInit(() -> COMPAT_FR.addToBackpacks("miner", ST.make(p, 1, W))); return p;});
		GT_API.registerItemLazy(MD.GT.mID, "gt.meta.toolHeadConstructionPickaxe", () -> {PrefixItem p = new PrefixItem(MD.GT, "gt.meta.toolHeadConstructionPickaxe" , OP.toolHeadConstructionPickaxe    ); if (COMPAT_FR != null) gregapi.GT_API.deferItemInit(() -> COMPAT_FR.addToBackpacks("builder", ST.make(p, 1, W))); return p;});
		GT_API.registerItemLazy(MD.GT.mID, "gt.meta.toolHeadBuilderwand", () -> {PrefixItem p = new PrefixItem(MD.GT, "gt.meta.toolHeadBuilderwand"         , OP.toolHeadBuilderwand            ); if (COMPAT_FR != null) gregapi.GT_API.deferItemInit(() -> COMPAT_FR.addToBackpacks("builder", ST.make(p, 1, W))); return p;});
		GT_API.registerItemLazy(MD.GT.mID, "gt.meta.toolHeadRawShovel", () -> new PrefixItem(MD.GT, "gt.meta.toolHeadRawShovel"           , OP.toolHeadRawShovel              ));
		GT_API.registerItemLazy(MD.GT.mID, "gt.meta.toolHeadShovel", () -> {PrefixItem p = new PrefixItem(MD.GT, "gt.meta.toolHeadShovel"              , OP.toolHeadShovel                 ); if (COMPAT_FR != null) gregapi.GT_API.deferItemInit(() -> COMPAT_FR.addToBackpacks("digger", ST.make(p, 1, W))); return p;});
		GT_API.registerItemLazy(MD.GT.mID, "gt.meta.toolHeadRawSpade", () -> new PrefixItem(MD.GT, "gt.meta.toolHeadRawSpade"            , OP.toolHeadRawSpade               ));
		GT_API.registerItemLazy(MD.GT.mID, "gt.meta.toolHeadSpade", () -> {PrefixItem p = new PrefixItem(MD.GT, "gt.meta.toolHeadSpade"               , OP.toolHeadSpade                  ); if (COMPAT_FR != null) gregapi.GT_API.deferItemInit(() -> COMPAT_FR.addToBackpacks("digger", ST.make(p, 1, W))); return p;});
		GT_API.registerItemLazy(MD.GT.mID, "gt.meta.toolHeadRawAxe", () -> new PrefixItem(MD.GT, "gt.meta.toolHeadRawAxe"              , OP.toolHeadRawAxe                 ));
		GT_API.registerItemLazy(MD.GT.mID, "gt.meta.toolHeadAxe", () -> {PrefixItem p = new PrefixItem(MD.GT, "gt.meta.toolHeadAxe"                 , OP.toolHeadAxe                    ); if (COMPAT_FR != null) gregapi.GT_API.deferItemInit(() -> COMPAT_FR.addToBackpacks("forester", ST.make(p, 1, W))); return p;});
		GT_API.registerItemLazy(MD.GT.mID, "gt.meta.toolHeadRawAxeDouble", () -> new PrefixItem(MD.GT, "gt.meta.toolHeadRawAxeDouble"        , OP.toolHeadRawAxeDouble           ));
		GT_API.registerItemLazy(MD.GT.mID, "gt.meta.toolHeadAxeDouble", () -> {PrefixItem p = new PrefixItem(MD.GT, "gt.meta.toolHeadAxeDouble"           , OP.toolHeadAxeDouble              ); if (COMPAT_FR != null) gregapi.GT_API.deferItemInit(() -> COMPAT_FR.addToBackpacks("forester", ST.make(p, 1, W))); return p;});
		GT_API.registerItemLazy(MD.GT.mID, "gt.meta.toolHeadRawHoe", () -> new PrefixItem(MD.GT, "gt.meta.toolHeadRawHoe"              , OP.toolHeadRawHoe                 ));
		GT_API.registerItemLazy(MD.GT.mID, "gt.meta.toolHeadHoe", () -> {PrefixItem p = new PrefixItem(MD.GT, "gt.meta.toolHeadHoe"                 , OP.toolHeadHoe                    ); if (COMPAT_FR != null) gregapi.GT_API.deferItemInit(() -> COMPAT_FR.addToBackpacks("forester", ST.make(p, 1, W))); return p;});
		GT_API.registerItemLazy(MD.GT.mID, "gt.meta.toolHeadHammer", () -> {PrefixItem p = new PrefixItem(MD.GT, "gt.meta.toolHeadHammer"              , OP.toolHeadHammer                 ); if (COMPAT_FR != null) gregapi.GT_API.deferItemInit(() -> COMPAT_FR.addToBackpacks("builder", ST.make(p, 1, W))); return p;});
		GT_API.registerItemLazy(MD.GT.mID, "gt.meta.toolHeadFile", () -> {PrefixItem p = new PrefixItem(MD.GT, "gt.meta.toolHeadFile"                , OP.toolHeadFile                   ); if (COMPAT_FR != null) gregapi.GT_API.deferItemInit(() -> COMPAT_FR.addToBackpacks("builder", ST.make(p, 1, W))); return p;});
		GT_API.registerItemLazy(MD.GT.mID, "gt.meta.toolHeadRawChisel", () -> new PrefixItem(MD.GT, "gt.meta.toolHeadRawChisel"           , OP.toolHeadRawChisel              ));
		GT_API.registerItemLazy(MD.GT.mID, "gt.meta.toolHeadChisel", () -> {PrefixItem p = new PrefixItem(MD.GT, "gt.meta.toolHeadChisel"              , OP.toolHeadChisel                 ); if (COMPAT_FR != null) gregapi.GT_API.deferItemInit(() -> COMPAT_FR.addToBackpacks("builder", ST.make(p, 1, W))); return p;});
		GT_API.registerItemLazy(MD.GT.mID, "gt.meta.toolHeadRawSaw", () -> new PrefixItem(MD.GT, "gt.meta.toolHeadRawSaw"              , OP.toolHeadRawSaw                 ));
		GT_API.registerItemLazy(MD.GT.mID, "gt.meta.toolHeadSaw", () -> {PrefixItem p = new PrefixItem(MD.GT, "gt.meta.toolHeadSaw"                 , OP.toolHeadSaw                    ); if (COMPAT_FR != null) gregapi.GT_API.deferItemInit(() -> COMPAT_FR.addToBackpacks("forester", ST.make(p, 1, W))); return p;});
		GT_API.registerItemLazy(MD.GT.mID, "gt.meta.toolHeadDrill", () -> {PrefixItem p = new PrefixItem(MD.GT, "gt.meta.toolHeadDrill"               , OP.toolHeadDrill                  ); if (COMPAT_FR != null) gregapi.GT_API.deferItemInit(() -> COMPAT_FR.addToBackpacks("miner", ST.make(p, 1, W))); return p;});
		GT_API.registerItemLazy(MD.GT.mID, "gt.meta.toolHeadChainsaw", () -> {PrefixItem p = new PrefixItem(MD.GT, "gt.meta.toolHeadChainsaw"            , OP.toolHeadChainsaw               ); if (COMPAT_FR != null) gregapi.GT_API.deferItemInit(() -> COMPAT_FR.addToBackpacks("forester", ST.make(p, 1, W))); return p;});
		GT_API.registerItemLazy(MD.GT.mID, "gt.meta.toolHeadWrench", () -> {PrefixItem p = new PrefixItem(MD.GT, "gt.meta.toolHeadWrench"              , OP.toolHeadWrench                 ); if (COMPAT_FR != null) gregapi.GT_API.deferItemInit(() -> COMPAT_FR.addToBackpacks("builder", ST.make(p, 1, W))); return p;});
		GT_API.registerItemLazy(MD.GT.mID, "gt.meta.toolHeadScrewdriver", () -> {PrefixItem p = new PrefixItem(MD.GT, "gt.meta.toolHeadScrewdriver"         , OP.toolHeadScrewdriver            ); if (COMPAT_FR != null) gregapi.GT_API.deferItemInit(() -> COMPAT_FR.addToBackpacks("builder", ST.make(p, 1, W))); return p;});
		GT_API.registerItemLazy(MD.GT.mID, "gt.meta.toolHeadRawUniversalSpade", () -> new PrefixItem(MD.GT, "gt.meta.toolHeadRawUniversalSpade"   , OP.toolHeadRawUniversalSpade      ));
		GT_API.registerItemLazy(MD.GT.mID, "gt.meta.toolHeadUniversalSpade", () -> {PrefixItem p = new PrefixItem(MD.GT, "gt.meta.toolHeadUniversalSpade"      , OP.toolHeadUniversalSpade         ); if (COMPAT_FR != null) gregapi.GT_API.deferItemInit(() -> COMPAT_FR.addToBackpacks("digger", ST.make(p, 1, W))); return p;});
		GT_API.registerItemLazy(MD.GT.mID, "gt.meta.toolHeadRawSense", () -> new PrefixItem(MD.GT, "gt.meta.toolHeadRawSense"            , OP.toolHeadRawSense               ));
		GT_API.registerItemLazy(MD.GT.mID, "gt.meta.toolHeadSense", () -> {PrefixItem p = new PrefixItem(MD.GT, "gt.meta.toolHeadSense"               , OP.toolHeadSense                  ); if (COMPAT_FR != null) gregapi.GT_API.deferItemInit(() -> COMPAT_FR.addToBackpacks("forester", ST.make(p, 1, W))); return p;});
		GT_API.registerItemLazy(MD.GT.mID, "gt.meta.toolHeadRawPlow", () -> new PrefixItem(MD.GT, "gt.meta.toolHeadRawPlow"             , OP.toolHeadRawPlow                ));
		GT_API.registerItemLazy(MD.GT.mID, "gt.meta.toolHeadPlow", () -> {PrefixItem p = new PrefixItem(MD.GT, "gt.meta.toolHeadPlow"                , OP.toolHeadPlow                   ); if (COMPAT_FR != null) gregapi.GT_API.deferItemInit(() -> COMPAT_FR.addToBackpacks("digger", ST.make(p, 1, W))); return p;});
		GT_API.registerItemLazy(MD.GT.mID, "gt.meta.toolHeadBuzzSaw", () -> {PrefixItem p = new PrefixItem(MD.GT, "gt.meta.toolHeadBuzzSaw"             , OP.toolHeadBuzzSaw                ); if (COMPAT_FR != null) gregapi.GT_API.deferItemInit(() -> COMPAT_FR.addToBackpacks("forester", ST.make(p, 1, W))); return p;});
		GT_API.registerItemLazy(MD.GT.mID, "gt.meta.toolHeadRawArrow", () -> {PrefixItem p = new PrefixItem(MD.GT, "gt.meta.toolHeadRawArrow"            , OP.toolHeadRawArrow               ); if (COMPAT_FR != null) gregapi.GT_API.deferItemInit(() -> COMPAT_FR.addToBackpacks("hunter", ST.make(p, 1, W))); return p;});
		GT_API.registerItemLazy(MD.GT.mID, "gt.meta.toolHeadArrow", () -> {PrefixItem p = new PrefixItem(MD.GT, "gt.meta.toolHeadArrow"               , OP.toolHeadArrow                  ); if (COMPAT_FR != null) gregapi.GT_API.deferItemInit(() -> COMPAT_FR.addToBackpacks("hunter", ST.make(p, 1, W))); return p;});
		GT_API.registerItemLazy(MD.GT.mID, "gt.meta.arrowGtWood", () -> {PrefixItem p = new PrefixItemProjectile(MD.GT, "gt.meta.arrowGtWood"       , OP.arrowGtWood        , TD.Projectiles.ARROW          , EntityArrow_Material.class, 1.00F, 6.00F, T, T, F).setLootingMultiplier(1); if (COMPAT_FR != null) gregapi.GT_API.deferItemInit(() -> COMPAT_FR.addToBackpacks("hunter", ST.make(p, 1, W))); return p;});
		GT_API.registerItemLazy(MD.GT.mID, "gt.meta.arrowGtPlastic", () -> {PrefixItem p = new PrefixItemProjectile(MD.GT, "gt.meta.arrowGtPlastic"    , OP.arrowGtPlastic     , TD.Projectiles.ARROW          , EntityArrow_Material.class, 1.50F, 6.00F, T, T, F).setLootingMultiplier(2); if (COMPAT_FR != null) gregapi.GT_API.deferItemInit(() -> COMPAT_FR.addToBackpacks("hunter", ST.make(p, 1, W))); return p;});
		GT_API.registerItemLazy(MD.GT.mID, "gt.meta.bulletGtSmall", () -> {PrefixItem p = new PrefixItemProjectile(MD.GT, "gt.meta.bulletGtSmall"     , OP.bulletGtSmall      , TD.Projectiles.BULLET_SMALL   , EntityArrow_Material.class, 1.50F, 3.00F, F, F, T).setLootingMultiplier(1); if (COMPAT_FR != null) gregapi.GT_API.deferItemInit(() -> COMPAT_FR.addToBackpacks("hunter", ST.make(p, 1, W))); return p;});
		GT_API.registerItemLazy(MD.GT.mID, "gt.meta.bulletGtMedium", () -> {PrefixItem p = new PrefixItemProjectile(MD.GT, "gt.meta.bulletGtMedium"    , OP.bulletGtMedium     , TD.Projectiles.BULLET_MEDIUM  , EntityArrow_Material.class, 1.75F, 2.50F, F, F, T).setLootingMultiplier(2); if (COMPAT_FR != null) gregapi.GT_API.deferItemInit(() -> COMPAT_FR.addToBackpacks("hunter", ST.make(p, 1, W))); return p;});
		GT_API.registerItemLazy(MD.GT.mID, "gt.meta.bulletGtLarge", () -> {PrefixItem p = new PrefixItemProjectile(MD.GT, "gt.meta.bulletGtLarge"     , OP.bulletGtLarge      , TD.Projectiles.BULLET_LARGE   , EntityArrow_Material.class, 2.00F, 2.00F, F, F, T).setLootingMultiplier(3); if (COMPAT_FR != null) gregapi.GT_API.deferItemInit(() -> COMPAT_FR.addToBackpacks("hunter", ST.make(p, 1, W))); return p;});
	}
}

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
import gregapi.item.multiitem.behaviors.Behavior_CureZombie;
import gregapi.item.multiitem.behaviors.Behavior_Drop_Loot;
import gregapi.item.multiitem.food.FoodStatFluid;
import gregapi.oredict.OreDictItemData;
import gregapi.util.CR;
import gregapi.util.ST;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import static gregapi.data.CS.*;

public class MultiItemBottles extends MultiItemRandomWithCompat implements IItemRottable {
	private MultiItemBottles(MultiItemBottles aFamily, short aMeta) {super(aFamily, aMeta);}
	@Override protected gregapi.item.multiitem.MultiItemRandom createVariant(short aMeta) {return new MultiItemBottles(this, aMeta);}
	public MultiItemBottles(String aModID, String aUnlocalized) {
		super(aModID, aUnlocalized);
		new gregapi.item.CreativeTab(getUnlocalizedName(), "GregTech: Bottles", this, (short)1600); // F16 creative tab: own GT tab (icon+displayItems), registered by CreativeTabsGT on RegisterEvent<CreativeModeTab>. 1:1.
	}
	
	protected void defineItems(gregapi.item.ItemTypeDefinitions aItems) {
		aItems.add(0, aID -> {addItem(    aID, "Mineral Water"                , ""                                    , FL.MnWtr                  .make(250), TC.stack(TC.VITREUS, 1), TC.stack(TC.AQUA           , 2), TC.stack(TC.SANO      , 1), FoodStatFluid.INSTANCE);});
		aItems.add(1, aID -> {addItem(    aID, "Sea Water"                    , ""                                    , FL.Ocean                  .make(250), TC.stack(TC.VITREUS, 1), TC.stack(TC.TEMPESTAS      , 2), TC.stack(TC.FAMES     , 1), FoodStatFluid.INSTANCE, FL.OceanGrC.make(250), FL.Tropics_Water.make(250));});
		aItems.add(2, aID -> {addItem(    aID, "Soda"                         , "", "foodBubblywater"                 , FL.Soda                   .make(250), TC.stack(TC.VITREUS, 1), TC.stack(TC.TEMPESTAS      , 2), TC.stack(TC.AQUA      , 1), FoodStatFluid.INSTANCE);});
		aItems.add(3, aID -> {addItem(    aID, "Mineral Soda"                 , ""                                    , FL.Mineralsoda            .make(250), TC.stack(TC.VITREUS, 1), TC.stack(TC.TEMPESTAS      , 2), TC.stack(TC.SANO      , 1), FoodStatFluid.INSTANCE);});
		aItems.add(4, aID -> {addItem(    aID, "Ice Water"                    , ""                                    , FL.Ice                    .make(250), TC.stack(TC.VITREUS, 1), TC.stack(TC.GELUM          , 2), TC.stack(TC.AQUA      , 1), FoodStatFluid.INSTANCE);});
		aItems.add(5, aID -> {addItem(    aID, "Dirty Water"                  , ""                                    , FL.Dirty_Water            .make(250), TC.stack(TC.VITREUS, 1), TC.stack(TC.AQUA           , 2), TC.stack(TC.VENEMUM   , 1), FoodStatFluid.INSTANCE);});
		aItems.add(6, aID -> {addItem(    aID, "Swampwater"                   , ""                                    , FL.Swampwater             .make(250), TC.stack(TC.VITREUS, 1), TC.stack(TC.AQUA           , 2), TC.stack(TC.MORTUUS   , 1), FoodStatFluid.INSTANCE, TD.Creative.HIDDEN);});
		aItems.add(7, aID -> {addItem(    aID, "Stagnant Water"               , ""                                    , FL.Stagnant_Water         .make(250), TC.stack(TC.VITREUS, 1), TC.stack(TC.AQUA           , 2), TC.stack(TC.MORTUUS   , 1), FoodStatFluid.INSTANCE, TD.Creative.HIDDEN);});
		aItems.add(8, aID -> {addItem(    aID, "Spectral Dew"                 , "", "listAllwater"                    , FL.SpDew                  .make(250), TC.stack(TC.VITREUS, 1), TC.stack(TC.AQUA           , 2), TC.stack(TC.SPIRITUS  , 1), FoodStatFluid.INSTANCE, TD.Creative.HIDDEN);});


		aItems.add(100, aID -> {addItem(  aID, "Grape Juice"                  , "", "foodGrapejuice"                  , FL.Juice_Grape_White      .make(250), TC.stack(TC.VITREUS, 1), TC.stack(TC.AQUA           , 1), TC.stack(TC.SANO      , 1), FoodStatFluid.INSTANCE, OD.container250juice);});
		aItems.add(101, aID -> {addItem(  aID, "White Wine"                   , "", "foodGrapewine"                   , FL.Wine_Grape_White       .make(250), TC.stack(TC.VITREUS, 1), TC.stack(TC.VENEMUM        , 1), TC.stack(TC.SANO      , 1), FoodStatFluid.INSTANCE);});
		aItems.add(102, aID -> {addItem(  aID, "Grape Vinegar"                , "", "foodVinegar"                     , FL.Vinegar_Grape          .make(250), TC.stack(TC.VITREUS, 1), TC.stack(TC.VENEMUM        , 2), TC.stack(TC.SANO      , 1), FoodStatFluid.INSTANCE); Sandwiches.INGREDIENTS.put(last(), DYE_INDEX_Lime);});
		aItems.add(103, aID -> {addItem(  aID, "Grape Juice"                  , "", "foodGrapejuice"                  , FL.Juice_Grape_Red        .make(250), TC.stack(TC.VITREUS, 1), TC.stack(TC.AQUA           , 1), TC.stack(TC.SANO      , 1), FoodStatFluid.INSTANCE, OD.container250juice);});
		aItems.add(104, aID -> {addItem(  aID, "Red Wine"                     , "", "foodGrapewine"                   , FL.Wine_Grape_Red         .make(250), TC.stack(TC.VITREUS, 1), TC.stack(TC.VENEMUM        , 1), TC.stack(TC.SANO      , 1), FoodStatFluid.INSTANCE);});
		aItems.add(105, aID -> {addItem(  aID, "Grape Smoothie"               , "", "foodGrapesmoothie"               , FL.Smoothie_Grape_White   .make(250), TC.stack(TC.VITREUS, 1), TC.stack(TC.GELUM          , 1), TC.stack(TC.SANO      , 1), FoodStatFluid.INSTANCE);});
		aItems.add(106, aID -> {addItem(  aID, "Grape Smoothie"               , "", "foodGrapesmoothie"               , FL.Smoothie_Grape_Red     .make(250), TC.stack(TC.VITREUS, 1), TC.stack(TC.GELUM          , 1), TC.stack(TC.SANO      , 1), FoodStatFluid.INSTANCE);});
		aItems.add(107, aID -> {addItem(  aID, "Grape Smoothie"               , "", "foodGrapesmoothie"               , FL.Smoothie_Grape_Green   .make(250), TC.stack(TC.VITREUS, 1), TC.stack(TC.GELUM          , 1), TC.stack(TC.SANO      , 1), FoodStatFluid.INSTANCE);});
		aItems.add(108, aID -> {addItem(  aID, "Grape Smoothie"               , "", "foodGrapesmoothie"               , FL.Smoothie_Grape_Purple  .make(250), TC.stack(TC.VITREUS, 1), TC.stack(TC.GELUM          , 1), TC.stack(TC.SANO      , 1), FoodStatFluid.INSTANCE);});
		aItems.add(109, aID -> {addItem(  aID, "Grape Juice"                  , "", "foodGrapejuice"                  , FL.Juice_Grape_Green      .make(250), TC.stack(TC.VITREUS, 1), TC.stack(TC.AQUA           , 1), TC.stack(TC.SANO      , 1), FoodStatFluid.INSTANCE, OD.container250juice);});
		aItems.add(110, aID -> {addItem(  aID, "Grape Juice"                  , "", "foodGrapejuice"                  , FL.Juice_Grape_Purple     .make(250), TC.stack(TC.VITREUS, 1), TC.stack(TC.AQUA           , 1), TC.stack(TC.SANO      , 1), FoodStatFluid.INSTANCE, OD.container250juice);});
		aItems.add(111, aID -> {addItem(  aID, "Wine"                         , "", "foodGrapewine"                   , FL.Wine_Grape_Green       .make(250), TC.stack(TC.VITREUS, 1), TC.stack(TC.VENEMUM        , 1), TC.stack(TC.SANO      , 1), FoodStatFluid.INSTANCE);});
		aItems.add(112, aID -> {addItem(  aID, "Ricardo Sanchez"              , "", "foodGrapewine"                   , FL.Wine_Grape_Purple      .make(250), TC.stack(TC.VITREUS, 1), TC.stack(TC.VENEMUM        , 1), TC.stack(TC.COGNITIO  , 1), FoodStatFluid.INSTANCE);});


		aItems.add(200, aID -> {addItem(  aID, "Lemon Juice"                  , "", "foodLemonjuice"                  , FL.Juice_Lemon            .make(250), TC.stack(TC.VITREUS, 1), TC.stack(TC.AQUA           , 1), TC.stack(TC.PERFODIO  , 1), FoodStatFluid.INSTANCE, OD.container250juice);});
		aItems.add(201, aID -> {addItem(  aID, "Lemonade"                     , "", "foodLemonade"                    , FL.Lemonade               .make(250), TC.stack(TC.VITREUS, 1), TC.stack(TC.AQUA           , 1), TC.stack(TC.PERFODIO  , 1), FoodStatFluid.INSTANCE);});
		aItems.add(202, aID -> {addItem(  aID, "Limoncello"                   , "", "foodLemonwine"                   , FL.Wine_Lemon             .make(250), TC.stack(TC.VITREUS, 1), TC.stack(TC.VENEMUM        , 1), TC.stack(TC.PERFODIO  , 1), FoodStatFluid.INSTANCE);});
		aItems.add(203, aID -> {addItem(  aID, "Alcopops"                     , "", "listAllbeverage"                 , FL.Alcopops               .make(250), TC.stack(TC.VITREUS, 1), TC.stack(TC.VENEMUM        , 1), TC.stack(TC.VINCULUM  , 1), FoodStatFluid.INSTANCE, TD.Creative.HIDDEN);});
		aItems.add(204, aID -> {addItem(  aID, "Cave Johnson's Grenade Juice" , ""                                    , FL.Grenade_Juice          .make(250), TC.stack(TC.VITREUS, 1), TC.stack(TC.MORTUUS        , 1), TC.stack(TC.PERDITIO  , 1), FoodStatFluid.INSTANCE, TD.Creative.HIDDEN, OD.container250juice); Sandwiches.INGREDIENTS.put(last(), DYE_INDEX_Yellow);});
		aItems.add(205, aID -> {addItem(  aID, "Lemon Smoothie"               , "", "foodLemonsmoothie"               , FL.Smoothie_Lemon         .make(250), TC.stack(TC.VITREUS, 1), TC.stack(TC.GELUM          , 1), TC.stack(TC.PERFODIO  , 1), FoodStatFluid.INSTANCE);});


		aItems.add(300, aID -> {addItem(  aID, "Potato Juice"                 , ""                                    , FL.Juice_Potato           .make(250), TC.stack(TC.VITREUS, 1), TC.stack(TC.AQUA           , 1), TC.stack(TC.MESSIS    , 1), FoodStatFluid.INSTANCE, OD.container250juice);});
		aItems.add(301, aID -> {addItem(  aID, "Vodka"                        , "", "foodVodka"                       , FL.Vodka                  .make(250), TC.stack(TC.VITREUS, 1), TC.stack(TC.VENEMUM        , 1), TC.stack(TC.TELUM     , 1), FoodStatFluid.INSTANCE);});
		aItems.add(302, aID -> {addItem(  aID, "Leninade"                     , "", "listAllbeverage"                 , FL.Leninade               .make(250), TC.stack(TC.VITREUS, 1), TC.stack(TC.VENEMUM        , 2), TC.stack(TC.TELUM     , 2), FoodStatFluid.INSTANCE, TD.Creative.HIDDEN);});


		aItems.add(400, aID -> {addItem(  aID, "Reedwater"                    , ""                                    , FL.Juice_Reed             .make(250), TC.stack(TC.VITREUS, 1), TC.stack(TC.AQUA           , 1), TC.stack(TC.HERBA     , 1), FoodStatFluid.INSTANCE, OD.container250juice);});
		aItems.add(401, aID -> {addItem(  aID, "Rum"                          , "", "foodRum"                         , FL.Rum_White              .make(250), TC.stack(TC.VITREUS, 1), TC.stack(TC.VENEMUM        , 1), TC.stack(TC.LUCRUM    , 1), FoodStatFluid.INSTANCE);});
		aItems.add(402, aID -> {addItem(  aID, "Pirate Brew"                  , "", "foodRum"                         , FL.Rum_Dark               .make(250), TC.stack(TC.VITREUS, 1), TC.stack(TC.VENEMUM        , 2), TC.stack(TC.LUCRUM    , 2), FoodStatFluid.INSTANCE, TD.Creative.HIDDEN);});
		aItems.add(403, aID -> {addItem(  aID, "Cane Vinegar"                 , "", "foodVinegar"                     , FL.Vinegar_Cane           .make(250), TC.stack(TC.VITREUS, 1), TC.stack(TC.VENEMUM        , 2), TC.stack(TC.LUCRUM    , 1), FoodStatFluid.INSTANCE);});
		aItems.add(404, aID -> {addItem(  aID, "Piña Colada"                  , ""                                    , FL.Pina_Colada            .make(250), TC.stack(TC.VITREUS, 1), TC.stack(TC.VENEMUM        , 1), TC.stack(TC.HERBA     , 2), FoodStatFluid.INSTANCE);});


		aItems.add(500, aID -> {addItem(  aID, "Hops Mash"                    , ""                                    , FL.Mash_Hops              .make(250), TC.stack(TC.VITREUS, 1), TC.stack(TC.AQUA           , 1), TC.stack(TC.MESSIS    , 1), FoodStatFluid.INSTANCE);});
		aItems.add(501, aID -> {addItem(  aID, "Dark Beer"                    , "", "foodBeer"                        , FL.Beer_Dark              .make(250), TC.stack(TC.VITREUS, 1), TC.stack(TC.VENEMUM        , 1), TC.stack(TC.POTENTIA  , 1), FoodStatFluid.INSTANCE);});
		aItems.add(502, aID -> {addItem(  aID, "Dragon Blood"                 , "", "foodBeer"                        , FL.Beer_Dragonblood       .make(250), TC.stack(TC.VITREUS, 1), TC.stack(TC.VENEMUM        , 2), TC.stack(TC.POTENTIA  , 2), FoodStatFluid.INSTANCE, TD.Creative.HIDDEN);});


		aItems.add(600, aID -> {addItem(  aID, "Wheaty Mash"                  , ""                                    , FL.Mash_Wheat             .make(250), TC.stack(TC.VITREUS, 1), TC.stack(TC.AQUA           , 1), TC.stack(TC.MESSIS    , 1), FoodStatFluid.INSTANCE);});
		aItems.add(601, aID -> {addItem(  aID, "Scotch"                       , "", "foodWhiskey"                     , FL.Whiskey_Scotch         .make(250), TC.stack(TC.VITREUS, 1), TC.stack(TC.VENEMUM        , 1), TC.stack(TC.TUTAMEN   , 1), FoodStatFluid.INSTANCE);});
		aItems.add(602, aID -> {addItem(  aID, "Glen McKenner"                , "", "foodWhiskey"                     , FL.Whiskey_GlenMcKenner   .make(250), TC.stack(TC.VITREUS, 1), TC.stack(TC.VENEMUM        , 2), TC.stack(TC.TUTAMEN   , 2), FoodStatFluid.INSTANCE, TD.Creative.HIDDEN);});


		aItems.add(700, aID -> {addItem(  aID, "Wheaty Hops Mash"             , ""                                    , FL.Mash_WheatHops         .make(250), TC.stack(TC.VITREUS, 1), TC.stack(TC.AQUA           , 1), TC.stack(TC.MESSIS    , 2), FoodStatFluid.INSTANCE);});
		aItems.add(701, aID -> {
			IL.Bottle_Beer.set(
		addItem(  aID, "Beer"                         , "", "foodBeer"                        , FL.Beer                   .make(250), TC.stack(TC.VITREUS, 1), TC.stack(TC.VENEMUM        , 1), TC.stack(TC.PERFODIO  , 1), FoodStatFluid.INSTANCE));
		});


		aItems.add(800, aID -> {addItem(  aID, "Chili Sauce"                  , "", "foodHotsauce"                    , FL.Sauce_Chili            .make(250), TC.stack(TC.VITREUS, 1), TC.stack(TC.VENEMUM        , 1), TC.stack(TC.IGNIS     , 1), FoodStatFluid.INSTANCE); Sandwiches.INGREDIENTS.put(last(), DYE_INDEX_Red);});
		aItems.add(801, aID -> {addItem(  aID, "Hot Sauce"                    , "", "foodHotsauce"                    , FL.Sauce_Hot              .make(250), TC.stack(TC.VITREUS, 1), TC.stack(TC.VENEMUM        , 1), TC.stack(TC.IGNIS     , 2), FoodStatFluid.INSTANCE); Sandwiches.INGREDIENTS.put(last(), DYE_INDEX_Red);});
		aItems.add(802, aID -> {addItem(  aID, "Diabolo Sauce"                , "", "foodHotsauce"                    , FL.Sauce_Diabolo          .make(250), TC.stack(TC.VITREUS, 1), TC.stack(TC.VENEMUM        , 2), TC.stack(TC.IGNIS     , 3), FoodStatFluid.INSTANCE, TD.Creative.HIDDEN); Sandwiches.INGREDIENTS.put(last(), DYE_INDEX_Red);});
		aItems.add(803, aID -> {addItem(  aID, "Diablo Sauce"                 , "", "foodHotsauce"                    , FL.Sauce_Diablo           .make(250), TC.stack(TC.VITREUS, 1), TC.stack(TC.VENEMUM        , 2), TC.stack(TC.IGNIS     , 4), FoodStatFluid.INSTANCE, TD.Creative.HIDDEN); Sandwiches.INGREDIENTS.put(last(), DYE_INDEX_Red);});
		aItems.add(804, aID -> {addItem(  aID, "There is no Cow Sauce"        , "", "foodHotsauce"                    , FL.Sauce_Cow_Level        .make(250), TC.stack(TC.VITREUS, 1), TC.stack(TC.VENEMUM        , 3), TC.stack(TC.IGNIS     , 5), FoodStatFluid.INSTANCE, TD.Creative.HIDDEN); Sandwiches.INGREDIENTS.put(last(), DYE_INDEX_Red);});
		aItems.add(805, aID -> {addItem(  aID, "Barbecue Sauce"               , "", "foodBarbecuesauce"               , FL.Sauce_BBQ              .make(250), TC.stack(TC.VITREUS, 1), TC.stack(TC.LIMUS          , 1), TC.stack(TC.FAMES     , 1), FoodStatFluid.INSTANCE); Sandwiches.INGREDIENTS.put(last(), DYE_INDEX_Brown);});


		aItems.add(900, aID -> {addItem(  aID, "Apple Juice"                  , "", "foodApplejuice"                  , FL.Juice_Apple            .make(250), TC.stack(TC.VITREUS, 1), TC.stack(TC.AQUA           , 1), TC.stack(TC.HERBA     , 1), FoodStatFluid.INSTANCE, FL.Juice_AppleGrC.make(250), OD.container250juice);});
		aItems.add(901, aID -> {addItem(  aID, "Cider"                        , "", "foodApplecider"                  , FL.Cider_Apple            .make(250), TC.stack(TC.VITREUS, 1), TC.stack(TC.VENEMUM        , 1), TC.stack(TC.TUTAMEN   , 1), FoodStatFluid.INSTANCE);});
		aItems.add(902, aID -> {addItem(  aID, "Apple Cider Vinegar"          , "", "foodVinegar"                     , FL.Vinegar_Apple          .make(250), TC.stack(TC.VITREUS, 1), TC.stack(TC.VENEMUM        , 2), TC.stack(TC.TUTAMEN   , 1), FoodStatFluid.INSTANCE); Sandwiches.INGREDIENTS.put(last(), DYE_INDEX_Lime);});
		aItems.add(905, aID -> {addItem(  aID, "Apple Smoothie"               , "", "foodApplesmoothie"               , FL.Smoothie_Apple         .make(250), TC.stack(TC.VITREUS, 1), TC.stack(TC.GELUM          , 1), TC.stack(TC.HERBA     , 1), FoodStatFluid.INSTANCE);});


		aItems.add(1000, aID -> {addItem( aID, "Olive Oil"         , "Cooking Oil", OP.bottle.dat(MT.OliveOil)        , FL.Oil_Olive              .make(250), TC.stack(TC.VITREUS, 1), TC.stack(TC.AQUA           , 1), TC.stack(TC.LUX       , 1), FoodStatFluid.INSTANCE, OD.container250seedoil); Sandwiches.INGREDIENTS.put(last(), DYE_INDEX_Lime);});
		aItems.add(1001, aID -> {addItem( aID, "Sunflower Oil"     , "Cooking Oil", OP.bottle.dat(MT.SunflowerOil)    , FL.Oil_Sunflower          .make(250), TC.stack(TC.VITREUS, 1), TC.stack(TC.AQUA           , 1), TC.stack(TC.LUX       , 1), FoodStatFluid.INSTANCE, OD.container250seedoil); Sandwiches.INGREDIENTS.put(last(), DYE_INDEX_Yellow);});
		aItems.add(1002, aID -> {addItem( aID, "Nut Oil"           , "Cooking Oil", OP.bottle.dat(MT.NutOil)          , FL.Oil_Nut                .make(250), TC.stack(TC.VITREUS, 1), TC.stack(TC.AQUA           , 1), TC.stack(TC.LUX       , 1), FoodStatFluid.INSTANCE, OD.container250seedoil); Sandwiches.INGREDIENTS.put(last(), DYE_INDEX_Yellow);});
		aItems.add(1003, aID -> {addItem( aID, "Seed Oil"          , "Cooking Oil", OP.bottle.dat(MT.SeedOil)         , FL.Oil_Seed               .make(250), TC.stack(TC.VITREUS, 1), TC.stack(TC.AQUA           , 1), TC.stack(TC.LUX       , 1), FoodStatFluid.INSTANCE, OD.container250seedoil); Sandwiches.INGREDIENTS.put(last(), DYE_INDEX_Lime);});
		aItems.add(1004, aID -> {addItem( aID, "Hemp Oil"          , "Cooking Oil", OP.bottle.dat(MT.HempOil)         , FL.Oil_Hemp               .make(250), TC.stack(TC.VITREUS, 1), TC.stack(TC.AQUA           , 1), TC.stack(TC.LUX       , 1), FoodStatFluid.INSTANCE, OD.container250seedoil); Sandwiches.INGREDIENTS.put(last(), DYE_INDEX_Lime);});
		aItems.add(1005, aID -> {addItem( aID, "Lin Oil"           , "Cooking Oil", OP.bottle.dat(MT.LinOil)          , FL.Oil_Lin                .make(250), TC.stack(TC.VITREUS, 1), TC.stack(TC.AQUA           , 1), TC.stack(TC.LUX       , 1), FoodStatFluid.INSTANCE, OD.container250seedoil); Sandwiches.INGREDIENTS.put(last(), DYE_INDEX_Lime);});
		aItems.add(1006, aID -> {addItem( aID, "Fish Oil"          , "Cooking Oil", OP.bottle.dat(MT.FishOil)         , FL.Oil_Fish               .make(250), TC.stack(TC.VITREUS, 1), TC.stack(TC.AQUA           , 1), TC.stack(TC.LUX       , 1), FoodStatFluid.INSTANCE); Sandwiches.INGREDIENTS.put(last(), DYE_INDEX_Yellow);});
		aItems.add(1007, aID -> {
			addItem( aID, "Whale Oil"         , "Cooking Oil", OP.bottle.dat(MT.WhaleOil)        , FL.Oil_Whale              .make(250), TC.stack(TC.VITREUS, 1), TC.stack(TC.AQUA           , 1), TC.stack(TC.LUX       , 1), FoodStatFluid.INSTANCE); Sandwiches.INGREDIENTS.put(last(), DYE_INDEX_Brown);
		
		CR.shapeless(make(4, 1003), CR.DEF, new Object[] {OD.container1000seedoil, OP.bottle.dat(MT.Empty), OP.bottle.dat(MT.Empty), OP.bottle.dat(MT.Empty), OP.bottle.dat(MT.Empty)});
		CR.shapeless(make(3, 1003), CR.DEF, new Object[] {OD.container1000seedoil, OP.bottle.dat(MT.Empty), OP.bottle.dat(MT.Empty), OP.bottle.dat(MT.Empty)});
		CR.shapeless(make(2, 1003), CR.DEF, new Object[] {OD.container1000seedoil, OP.bottle.dat(MT.Empty), OP.bottle.dat(MT.Empty)});
		CR.shapeless(make(1, 1003), CR.DEF, new Object[] {OD.container1000seedoil, OP.bottle.dat(MT.Empty)});
		});
		
		aItems.add(1020, aID -> {addItem( aID, "Mayo"                         , "", "foodMayo"                        , FL.Mayo                   .make(250), TC.stack(TC.VITREUS, 1), TC.stack(TC.LIMUS          , 1), TC.stack(TC.FAMES     , 1), FoodStatFluid.INSTANCE); Sandwiches.INGREDIENTS.put(last(), DYE_INDEX_White);});
		aItems.add(1021, aID -> {addItem( aID, "Dressing"                     , "", "foodSaladdressing"               , FL.Dressing               .make(250), TC.stack(TC.VITREUS, 1), TC.stack(TC.HERBA          , 1), TC.stack(TC.FAMES     , 1), FoodStatFluid.INSTANCE); Sandwiches.INGREDIENTS.put(last(), DYE_INDEX_Lime);});
		
		aItems.add(1100, aID -> {
			IL.Bottle_Milk.set(
		addItem( aID, "Milk"                         , "", OP.bottle.dat(MT.Milk)            , FL.Milk                   .make(250), TC.stack(TC.VITREUS, 1), TC.stack(TC.AQUA           , 1), TC.stack(TC.SANO      , 1), FoodStatFluid.INSTANCE, FL.MilkGrC.make(250)));
		});
		aItems.add(1101, aID -> {addItem( aID, "Heavy Cream"                  , "", "bottleCream"                     , FL.Cream                  .make(250), TC.stack(TC.VITREUS, 1), TC.stack(TC.LIMUS          , 1), TC.stack(TC.POTENTIA  , 1), FoodStatFluid.INSTANCE); Sandwiches.INGREDIENTS.put(last(), DYE_INDEX_White);});
		aItems.add(1102, aID -> {
			IL.Bottle_Milk_Spoiled.set(
		addItem( aID, "Milk"                         , ""                                    , FL.Milk_Spoiled           .make(250), TC.stack(TC.VITREUS, 1), TC.stack(TC.AQUA           , 1), TC.stack(TC.VENEMUM   , 1), FoodStatFluid.INSTANCE, TD.Creative.HIDDEN));
		
		CR.shapeless(make(4, 1100), CR.DEF, new Object[] {OD.container1000milk, OP.bottle.dat(MT.Empty), OP.bottle.dat(MT.Empty), OP.bottle.dat(MT.Empty), OP.bottle.dat(MT.Empty)});
		CR.shapeless(make(3, 1100), CR.DEF, new Object[] {OD.container1000milk, OP.bottle.dat(MT.Empty), OP.bottle.dat(MT.Empty), OP.bottle.dat(MT.Empty)});
		CR.shapeless(make(2, 1100), CR.DEF, new Object[] {OD.container1000milk, OP.bottle.dat(MT.Empty), OP.bottle.dat(MT.Empty)});
		CR.shapeless(make(1, 1100), CR.DEF, new Object[] {OD.container1000milk, OP.bottle.dat(MT.Empty)});
		});
		
		aItems.add(1200, aID -> {
			IL.Bottle_Milk_Soy.set(
		addItem( aID, "Soy Milk"                     , "", "foodSoymilk"                     , FL.MilkSoy                .make(250), TC.stack(TC.VITREUS, 1), TC.stack(TC.HERBA          , 1), TC.stack(TC.SANO      , 1), FoodStatFluid.INSTANCE));
		
		CR.shapeless(make(4, 1200), CR.DEF, new Object[] {OD.container1000soymilk, OP.bottle.dat(MT.Empty), OP.bottle.dat(MT.Empty), OP.bottle.dat(MT.Empty), OP.bottle.dat(MT.Empty)});
		CR.shapeless(make(3, 1200), CR.DEF, new Object[] {OD.container1000soymilk, OP.bottle.dat(MT.Empty), OP.bottle.dat(MT.Empty), OP.bottle.dat(MT.Empty)});
		CR.shapeless(make(2, 1200), CR.DEF, new Object[] {OD.container1000soymilk, OP.bottle.dat(MT.Empty), OP.bottle.dat(MT.Empty)});
		CR.shapeless(make(1, 1200), CR.DEF, new Object[] {OD.container1000soymilk, OP.bottle.dat(MT.Empty)});
		});

		aItems.add(1300, aID -> {
			IL.Bottle_Honey.set(
		addItem( aID, "Honey", "Why does this Bottle look like a Bear and not a Bee?"        , FL.Honey                  .make(250), TC.stack(TC.VITREUS, 1), TC.stack(TC.BESTIA         , 1), TC.stack(TC.SANO      , 1), FoodStatFluid.INSTANCE, FL.HoneyGrC.make(250), FL.HoneyBoP.make(250), OP.bottle.dat(MT.Honey), OD.container250honey)); Sandwiches.INGREDIENTS.put(last(), DYE_INDEX_Yellow);
		});
		aItems.add(1301, aID -> {addItem( aID, "Honeydew"                     , "", OP.bottle.dat(MT.Honeydew)        , FL.Honeydew               .make(250), TC.stack(TC.VITREUS, 1), TC.stack(TC.BESTIA         , 1), TC.stack(TC.SANO      , 1), FoodStatFluid.INSTANCE);});
		aItems.add(1302, aID -> {addItem( aID, "Royal Jelly"                  , ""                                    , FL.RoyalJelly             .make(250), TC.stack(TC.VITREUS, 1), TC.stack(TC.BESTIA         , 2), TC.stack(TC.SANO      , 1), FoodStatFluid.INSTANCE); Sandwiches.INGREDIENTS.put(last(), DYE_INDEX_Yellow);});
		aItems.add(1303, aID -> {addItem( aID, "Ambrosia"                     , "", "foodAmbrosia"                    , FL.Ambrosia               .make(250), TC.stack(TC.VITREUS, 1), TC.stack(TC.BESTIA         , 1), TC.stack(TC.SANO      , 2), FoodStatFluid.INSTANCE); Sandwiches.INGREDIENTS.put(last(), DYE_INDEX_Purple);});
		aItems.add(1304, aID -> {addItem( aID, "Short Mead"                   , "", "foodMead"                        , FL.ShortMead              .make(250), TC.stack(TC.VITREUS, 1), TC.stack(TC.VENEMUM        , 1), TC.stack(TC.SANO      , 1), FoodStatFluid.INSTANCE);});
		aItems.add(1305, aID -> {
			addItem( aID, "Mead"                         , "", "foodMead"                        , FL.Mead                   .make(250), TC.stack(TC.VITREUS, 1), TC.stack(TC.VENEMUM        , 1), TC.stack(TC.SANO      , 1), FoodStatFluid.INSTANCE);
		
		CR.shapeless(make(4, 1300), CR.DEF, new Object[] {OD.container1000honey, OP.bottle.dat(MT.Empty), OP.bottle.dat(MT.Empty), OP.bottle.dat(MT.Empty), OP.bottle.dat(MT.Empty)});
		CR.shapeless(make(3, 1300), CR.DEF, new Object[] {OD.container1000honey, OP.bottle.dat(MT.Empty), OP.bottle.dat(MT.Empty), OP.bottle.dat(MT.Empty)});
		CR.shapeless(make(2, 1300), CR.DEF, new Object[] {OD.container1000honey, OP.bottle.dat(MT.Empty), OP.bottle.dat(MT.Empty)});
		CR.shapeless(make(1, 1300), CR.DEF, new Object[] {OD.container1000honey, OP.bottle.dat(MT.Empty)});
		
		CR.shapeless(make(1, 1300), CR.DEF, new Object[] {OP.bottle.dat(MT.Empty)                         , OD.dropHoney     , OD.dropHoney     , OD.dropHoney     });
		CR.shapeless(make(2, 1300), CR.DEF, new Object[] {OP.bottle.dat(MT.Empty), OP.bottle.dat(MT.Empty), OD.dropHoney     , OD.dropHoney     , OD.dropHoney     , OD.dropHoney     , OD.dropHoney     });
		CR.shapeless(make(1, 1301), CR.DEF, new Object[] {OP.bottle.dat(MT.Empty)                         , OD.dropHoneydew  , OD.dropHoneydew  , OD.dropHoneydew  });
		CR.shapeless(make(2, 1301), CR.DEF, new Object[] {OP.bottle.dat(MT.Empty), OP.bottle.dat(MT.Empty), OD.dropHoneydew  , OD.dropHoneydew  , OD.dropHoneydew  , OD.dropHoneydew  , OD.dropHoneydew  });
		CR.shapeless(make(1, 1302), CR.DEF, new Object[] {OP.bottle.dat(MT.Empty)                         , OD.dropRoyalJelly, OD.dropRoyalJelly, OD.dropRoyalJelly});
		CR.shapeless(make(2, 1302), CR.DEF, new Object[] {OP.bottle.dat(MT.Empty), OP.bottle.dat(MT.Empty), OD.dropRoyalJelly, OD.dropRoyalJelly, OD.dropRoyalJelly, OD.dropRoyalJelly, OD.dropRoyalJelly});
		});

		aItems.add(1400, aID -> {addItem( aID, "Golden Apple Juice"           , "", "foodApplejuice"      , FL.make("potion.goldenapplejuice"  , 250), TC.stack(TC.VITREUS, 1), TC.stack(TC.AQUA           , 1), TC.stack(TC.SANO      , 1), FoodStatFluid.INSTANCE, TD.Creative.HIDDEN, OD.container250juice);});
		aItems.add(1401, aID -> {addItem( aID, "Golden Cider"                 , "", "foodApplecider"      , FL.make("potion.goldencider"       , 250), TC.stack(TC.VITREUS, 1), TC.stack(TC.VENEMUM        , 1), TC.stack(TC.TUTAMEN   , 1), FoodStatFluid.INSTANCE, TD.Creative.HIDDEN);});

		aItems.add(1500, aID -> {addItem( aID, "Idun's Apple Juice"           , "", "foodApplejuice"      , FL.make("potion.idunsapplejuice"   , 250), TC.stack(TC.VITREUS, 1), TC.stack(TC.AQUA           , 1), TC.stack(TC.NEBRISUM  , 9), FoodStatFluid.INSTANCE, TD.Creative.HIDDEN, OD.container250juice);});
		aItems.add(1501, aID -> {addItem( aID, "Notches Brew"                 , "", "foodApplecider"      , FL.make("potion.notchesbrew"       , 250), TC.stack(TC.VITREUS, 1), TC.stack(TC.VENEMUM        , 2), TC.stack(TC.NEBRISUM  , 9), FoodStatFluid.INSTANCE, TD.Creative.HIDDEN);});

		aItems.add(1600, aID -> {
			IL.Bottle_Purple_Drink.set(
		addItem( aID, "Purple Drink"                 , ""                                    , FL.Purple_Drink           .make(250), TC.stack(TC.VITREUS, 1), TC.stack(TC.AQUA           , 1), TC.stack(TC.VINCULUM  , 1), FoodStatFluid.INSTANCE));
		});
		aItems.add(1601, aID -> {
			IL.Bottle_Rotten_Drink.set(
		addItem( aID, "Rotten Drink"                 , ""                                    , FL.Rotten_Drink           .make(250), TC.stack(TC.VITREUS, 1), TC.stack(TC.VENEMUM        , 1), TC.stack(TC.MORTUUS   , 1), FoodStatFluid.INSTANCE, TD.Creative.HIDDEN));
		});

		aItems.add(1700, aID -> {
			IL.Bottle_Holy_Water.set(
		addItem( aID, "Holy Water"                   , "", OP.bottle.dat(MT.HolyWater)       , MT.HolyWater          .liquid(U4, F), TC.stack(TC.VITREUS, 1), TC.stack(TC.AQUA           , 1), TC.stack(TC.AURAM     , 1), FoodStatFluid.INSTANCE, new Behavior_CureZombie(500, T)));
		});

		aItems.add(1800, aID -> {addItem( aID, "Rice Water"                   , ""                                    , FL.Mash_Rice              .make(250), TC.stack(TC.VITREUS, 1), TC.stack(TC.AQUA           , 1), TC.stack(TC.HERBA     , 1), FoodStatFluid.INSTANCE);});
		aItems.add(1801, aID -> {addItem( aID, "Sake"                         , "", "foodSake"                        , FL.Sake                   .make(250), TC.stack(TC.VITREUS, 1), TC.stack(TC.VENEMUM        , 1), TC.stack(TC.TEMPESTAS , 1), FoodStatFluid.INSTANCE);});
		aItems.add(1802, aID -> {addItem( aID, "Rice Vinegar"                 , "", "foodVinegar"                     , FL.Vinegar_Rice           .make(250), TC.stack(TC.VITREUS, 1), TC.stack(TC.VENEMUM        , 2), TC.stack(TC.TEMPESTAS , 1), FoodStatFluid.INSTANCE); Sandwiches.INGREDIENTS.put(last(), DYE_INDEX_Lime);});

		aItems.add(1900, aID -> {addItem( aID, "Chocolate Cream"              , "", "foodChocolatecream"              , FL.Cream_Chocolate        .make(250), TC.stack(TC.VITREUS, 1), TC.stack(TC.LIMUS          , 1), TC.stack(TC.SANO      , 1), FoodStatFluid.INSTANCE); Sandwiches.INGREDIENTS.put(last(), DYE_INDEX_Brown);});
		aItems.add(1901, aID -> {addItem( aID, "Nutella"                      , "", "foodNutella"                     , FL.Cream_Nutella          .make(250), TC.stack(TC.VITREUS, 1), TC.stack(TC.LIMUS          , 1), TC.stack(TC.FAMES     , 1), FoodStatFluid.INSTANCE); Sandwiches.INGREDIENTS.put(last(), DYE_INDEX_Brown);});

		aItems.add(2000, aID -> {addItem( aID, "Strawberry Juice"             , "", "foodStrawberryjuice"             , FL.Juice_Strawberry       .make(250), TC.stack(TC.VITREUS, 1), TC.stack(TC.AQUA           , 1), TC.stack(TC.HERBA     , 1), FoodStatFluid.INSTANCE, OD.container250juice);});
		aItems.add(2005, aID -> {addItem( aID, "Strawberry Smoothie"          , "", "foodStrawberrysmoothie"          , FL.Smoothie_Strawberry    .make(250), TC.stack(TC.VITREUS, 1), TC.stack(TC.GELUM          , 1), TC.stack(TC.HERBA     , 1), FoodStatFluid.INSTANCE);});

		aItems.add(2100, aID -> {addItem( aID, "Banana Juice"                 , "", "foodBananajuice"                 , FL.Juice_Banana           .make(250), TC.stack(TC.VITREUS, 1), TC.stack(TC.AQUA           , 1), TC.stack(TC.HERBA     , 1), FoodStatFluid.INSTANCE, OD.container250juice);});
		aItems.add(2105, aID -> {addItem( aID, "Banana Smoothie"              , "", "foodBananasmoothie"              , FL.Smoothie_Banana        .make(250), TC.stack(TC.VITREUS, 1), TC.stack(TC.GELUM          , 1), TC.stack(TC.HERBA     , 1), FoodStatFluid.INSTANCE);});

		aItems.add(2200, aID -> {
			IL.Bottle_Slime_Green.set(
		addItem( aID, "Green Slime Bottle"         , "Can be used as Glue too", OD.container250slimegreen, FL.Slime_Green.make(250), TC.stack(TC.VITREUS, 1), TC.stack(TC.AQUA           , 1), TC.stack(TC.LIMUS     , 1), FoodStatFluid.INSTANCE)); Sandwiches.INGREDIENTS.put(last(), DYE_INDEX_Lime);
		});
		aItems.add(2201, aID -> {
			IL.Bottle_Slime_Pink.set(
		addItem( aID, "Pink Slime Bottle"          , "Can be used as Glue too", OD.container250slimepink , FL.Slime_Pink .make(250), TC.stack(TC.VITREUS, 1), TC.stack(TC.CORPUS         , 1), TC.stack(TC.LIMUS     , 1), FoodStatFluid.INSTANCE)); Sandwiches.INGREDIENTS.put(last(), DYE_INDEX_Pink);
		});
		aItems.add(2202, aID -> {
			IL.Bottle_Slime_Blue.set(
		addItem( aID, "Blue Slime Bottle"          , "Can be used as Glue too", OD.container250slimeblue , FL.Slime_Blue .make(250), TC.stack(TC.VITREUS, 1), TC.stack(TC.AQUA           , 1), TC.stack(TC.LIMUS     , 1), FoodStatFluid.INSTANCE)); Sandwiches.INGREDIENTS.put(last(), DYE_INDEX_Blue);
		});
		aItems.add(2210, aID -> {
			addItem( aID, "BAWLS"                      , ""                                                  , FL.BAWLS      .make(250), TC.stack(TC.VITREUS, 1), TC.stack(TC.TEMPESTAS      , 1), TC.stack(TC.LIMUS     , 1), FoodStatFluid.INSTANCE);
		
		CR.shapeless(make(4, 2200), CR.DEF, new Object[] {OD.container1000slimegreen, OP.bottle.dat(MT.Empty), OP.bottle.dat(MT.Empty), OP.bottle.dat(MT.Empty), OP.bottle.dat(MT.Empty)});
		CR.shapeless(make(3, 2200), CR.DEF, new Object[] {OD.container1000slimegreen, OP.bottle.dat(MT.Empty), OP.bottle.dat(MT.Empty), OP.bottle.dat(MT.Empty)});
		CR.shapeless(make(2, 2200), CR.DEF, new Object[] {OD.container1000slimegreen, OP.bottle.dat(MT.Empty), OP.bottle.dat(MT.Empty)});
		CR.shapeless(make(1, 2200), CR.DEF, new Object[] {OD.container1000slimegreen, OP.bottle.dat(MT.Empty)});
		
		CR.shapeless(make(4, 2201), CR.DEF, new Object[] {OD.container1000slimepink, OP.bottle.dat(MT.Empty), OP.bottle.dat(MT.Empty), OP.bottle.dat(MT.Empty), OP.bottle.dat(MT.Empty)});
		CR.shapeless(make(3, 2201), CR.DEF, new Object[] {OD.container1000slimepink, OP.bottle.dat(MT.Empty), OP.bottle.dat(MT.Empty), OP.bottle.dat(MT.Empty)});
		CR.shapeless(make(2, 2201), CR.DEF, new Object[] {OD.container1000slimepink, OP.bottle.dat(MT.Empty), OP.bottle.dat(MT.Empty)});
		CR.shapeless(make(1, 2201), CR.DEF, new Object[] {OD.container1000slimepink, OP.bottle.dat(MT.Empty)});
		
		CR.shapeless(make(4, 2202), CR.DEF, new Object[] {OD.container1000slimeblue, OP.bottle.dat(MT.Empty), OP.bottle.dat(MT.Empty), OP.bottle.dat(MT.Empty), OP.bottle.dat(MT.Empty)});
		CR.shapeless(make(3, 2202), CR.DEF, new Object[] {OD.container1000slimeblue, OP.bottle.dat(MT.Empty), OP.bottle.dat(MT.Empty), OP.bottle.dat(MT.Empty)});
		CR.shapeless(make(2, 2202), CR.DEF, new Object[] {OD.container1000slimeblue, OP.bottle.dat(MT.Empty), OP.bottle.dat(MT.Empty)});
		CR.shapeless(make(1, 2202), CR.DEF, new Object[] {OD.container1000slimeblue, OP.bottle.dat(MT.Empty)});
		});
		
		aItems.add(2300, aID -> {addItem( aID, "Melon Juice"                  , "", "foodMelonjuice"                  , FL.Juice_Melon            .make(250), TC.stack(TC.VITREUS, 1), TC.stack(TC.AQUA           , 2), FoodStatFluid.INSTANCE, OD.container250juice);});
		aItems.add(2305, aID -> {addItem( aID, "Melon Smoothie"               , "", "foodMelonsmoothie"               , FL.Smoothie_Melon         .make(250), TC.stack(TC.VITREUS, 1), TC.stack(TC.GELUM          , 2), FoodStatFluid.INSTANCE);});
		
		aItems.add(2400, aID -> {addItem( aID, "Juice"                        , "", "foodJuice"                       , FL.Juice                  .make(250), TC.stack(TC.VITREUS, 1), TC.stack(TC.AQUA           , 1), TC.stack(TC.HERBA     , 1), FoodStatFluid.INSTANCE, OD.container250juice);});
		aItems.add(2405, aID -> {
			addItem( aID, "Froot Smoothie"               , "", "foodFruitsmoothie"               , FL.Smoothie_Fruit         .make(250), TC.stack(TC.VITREUS, 1), TC.stack(TC.GELUM          , 1), TC.stack(TC.HERBA     , 1), FoodStatFluid.INSTANCE);
		
		CR.shapeless(make(4, 2400), CR.DEF, new Object[] {OD.container1000juice, OP.bottle.dat(MT.Empty), OP.bottle.dat(MT.Empty), OP.bottle.dat(MT.Empty), OP.bottle.dat(MT.Empty)});
		CR.shapeless(make(3, 2400), CR.DEF, new Object[] {OD.container1000juice, OP.bottle.dat(MT.Empty), OP.bottle.dat(MT.Empty), OP.bottle.dat(MT.Empty)});
		CR.shapeless(make(2, 2400), CR.DEF, new Object[] {OD.container1000juice, OP.bottle.dat(MT.Empty), OP.bottle.dat(MT.Empty)});
		CR.shapeless(make(1, 2400), CR.DEF, new Object[] {OD.container1000juice, OP.bottle.dat(MT.Empty)});
		CR.shapeless(make(1, 2400), CR.DEF, new Object[] {OD.container250juice , OP.bottle.dat(MT.Empty)});
		});
		
		aItems.add(2500, aID -> {addItem( aID, "Kiwi Juice"                   , "", "foodKiwijuice"                   , FL.Juice_Kiwi             .make(250), TC.stack(TC.VITREUS, 1), TC.stack(TC.AQUA           , 1), TC.stack(TC.PERFODIO  , 1), FoodStatFluid.INSTANCE, OD.container250juice);});
		aItems.add(2505, aID -> {addItem( aID, "Kiwi Smoothie"                , "", "foodKiwismoothie"                , FL.Smoothie_Kiwi          .make(250), TC.stack(TC.VITREUS, 1), TC.stack(TC.GELUM          , 1), TC.stack(TC.PERFODIO  , 1), FoodStatFluid.INSTANCE);});
		
		aItems.add(2600, aID -> {addItem( aID, "Raspberry Juice"              , "", "foodRaspberryjuice"              , FL.Juice_Raspberry        .make(250), TC.stack(TC.VITREUS, 1), TC.stack(TC.AQUA           , 1), TC.stack(TC.FAMES     , 1), FoodStatFluid.INSTANCE, OD.container250juice);});
		aItems.add(2605, aID -> {addItem( aID, "Raspberry Smoothie"           , "", "foodRaspberrysmoothie"           , FL.Smoothie_Raspberry     .make(250), TC.stack(TC.VITREUS, 1), TC.stack(TC.GELUM          , 1), TC.stack(TC.FAMES     , 1), FoodStatFluid.INSTANCE);});
		
		aItems.add(2700, aID -> {addItem( aID, "Blackberry Juice"             , "", "foodBlackberryjuice"             , FL.Juice_Blackberry       .make(250), TC.stack(TC.VITREUS, 1), TC.stack(TC.AQUA           , 1), TC.stack(TC.FAMES     , 1), FoodStatFluid.INSTANCE, OD.container250juice);});
		aItems.add(2705, aID -> {addItem( aID, "Blackberry Smoothie"          , "", "foodBlackberrysmoothie"          , FL.Smoothie_Blackberry    .make(250), TC.stack(TC.VITREUS, 1), TC.stack(TC.GELUM          , 1), TC.stack(TC.FAMES     , 1), FoodStatFluid.INSTANCE);});
		
		aItems.add(2800, aID -> {addItem( aID, "Blueberry Juice"              , "", "foodBlueberryjuice"              , FL.Juice_Blueberry        .make(250), TC.stack(TC.VITREUS, 1), TC.stack(TC.AQUA           , 1), TC.stack(TC.FAMES     , 1), FoodStatFluid.INSTANCE, OD.container250juice, DYE_OREDICTS[DYE_INDEX_Blue]);});
		aItems.add(2805, aID -> {addItem( aID, "Blueberry Smoothie"           , "", "foodBlueberrysmoothie"           , FL.Smoothie_Blueberry     .make(250), TC.stack(TC.VITREUS, 1), TC.stack(TC.GELUM          , 1), TC.stack(TC.FAMES     , 1), FoodStatFluid.INSTANCE);});
		
		aItems.add(2900, aID -> {addItem( aID, "Cranberry Juice"              , "", "foodCranberryjuice"              , FL.Juice_Cranberry        .make(250), TC.stack(TC.VITREUS, 1), TC.stack(TC.AQUA           , 1), TC.stack(TC.FAMES     , 1), FoodStatFluid.INSTANCE, OD.container250juice);});
		aItems.add(2905, aID -> {addItem( aID, "Cranberry Smoothie"           , "", "foodCranberrysmoothie"           , FL.Smoothie_Cranberry     .make(250), TC.stack(TC.VITREUS, 1), TC.stack(TC.GELUM          , 1), TC.stack(TC.FAMES     , 1), FoodStatFluid.INSTANCE);});
		
		aItems.add(3000, aID -> {addItem( aID, "Gooseberry Juice"             , "", "foodGooseberryjuice"             , FL.Juice_Gooseberry       .make(250), TC.stack(TC.VITREUS, 1), TC.stack(TC.AQUA           , 1), TC.stack(TC.FAMES     , 1), FoodStatFluid.INSTANCE, OD.container250juice);});
		aItems.add(3005, aID -> {addItem( aID, "Gooseberry Smoothie"          , "", "foodGooseberrysmoothie"          , FL.Smoothie_Gooseberry    .make(250), TC.stack(TC.VITREUS, 1), TC.stack(TC.GELUM          , 1), TC.stack(TC.FAMES     , 1), FoodStatFluid.INSTANCE);});
		
		aItems.add(3100, aID -> {addItem( aID, "Tomato Juice"                 , "", "foodTomatojuice"                 , FL.Juice_Tomato           .make(250), TC.stack(TC.VITREUS, 1), TC.stack(TC.AQUA           , 1), TC.stack(TC.HERBA     , 1), FoodStatFluid.INSTANCE, OD.container250juice);});
		aItems.add(3101, aID -> {addItem( aID, "Tomato Ketchup"               , "", "foodKetchup"                     , FL.Ketchup                .make(250), TC.stack(TC.VITREUS, 1), TC.stack(TC.LIMUS          , 1), TC.stack(TC.FAMES     , 1), FoodStatFluid.INSTANCE); Sandwiches.INGREDIENTS.put(last(), DYE_INDEX_Red);});
		
		aItems.add(3200, aID -> {addItem( aID, "Golden Carrot Juice"          , "", "foodCarrotjuice"                 , FL.make("goldencarrotjuice"   , 250), TC.stack(TC.VITREUS, 1), TC.stack(TC.AQUA           , 1), TC.stack(TC.SENSUS    , 2), FoodStatFluid.INSTANCE, OD.container250juice, TD.Creative.HIDDEN);});
		
		aItems.add(3300, aID -> {addItem( aID, "Carrot Juice"                 , "", "foodCarrotjuice"                 , FL.Juice_Carrot           .make(250), TC.stack(TC.VITREUS, 1), TC.stack(TC.AQUA           , 1), TC.stack(TC.SENSUS    , 1), FoodStatFluid.INSTANCE, OD.container250juice);});
		
		aItems.add(3400, aID -> {addItem( aID, "Cactus Water"                 , ""                                    , FL.Juice_Cactus           .make(250), TC.stack(TC.VITREUS, 1), TC.stack(TC.AQUA           , 1), TC.stack(TC.HERBA     , 1), FoodStatFluid.INSTANCE, OD.container250juice);});
		
		aItems.add(3500, aID -> {addItem( aID, "Maple Sap"                    , ""                                    , FL.Sap_Maple              .make(250), TC.stack(TC.VITREUS, 1), TC.stack(TC.AQUA           , 1), TC.stack(TC.MOTUS     , 1), FoodStatFluid.INSTANCE, OD.container250maplesap);});
		aItems.add(3501, aID -> {
			addItem( aID, "Maple Syrup"                  , "", "foodMaplesyrup"                  , FL.Syrup_Maple            .make(250), TC.stack(TC.VITREUS, 1), TC.stack(TC.LIMUS          , 1), TC.stack(TC.POTENTIA  , 1), FoodStatFluid.INSTANCE); Sandwiches.INGREDIENTS.put(last(), DYE_INDEX_Brown);
		
		CR.shapeless(make(4, 3500), CR.DEF, new Object[] {OD.container1000maplesap, OP.bottle.dat(MT.Empty), OP.bottle.dat(MT.Empty), OP.bottle.dat(MT.Empty), OP.bottle.dat(MT.Empty)});
		CR.shapeless(make(3, 3500), CR.DEF, new Object[] {OD.container1000maplesap, OP.bottle.dat(MT.Empty), OP.bottle.dat(MT.Empty), OP.bottle.dat(MT.Empty)});
		CR.shapeless(make(2, 3500), CR.DEF, new Object[] {OD.container1000maplesap, OP.bottle.dat(MT.Empty), OP.bottle.dat(MT.Empty)});
		CR.shapeless(make(1, 3500), CR.DEF, new Object[] {OD.container1000maplesap, OP.bottle.dat(MT.Empty)});
		});
		
		aItems.add(3601, aID -> {addItem( aID, "Peanut Butter"                , "", "foodPeanutbutter"                , FL.Nutbutter_Peanut       .make(250), TC.stack(TC.VITREUS, 1), TC.stack(TC.LIMUS          , 1), TC.stack(TC.FAMES     , 1), FoodStatFluid.INSTANCE); Sandwiches.INGREDIENTS.put(last(), DYE_INDEX_Brown);});
		
		aItems.add(3700, aID -> {
			addItem( aID, "Rainbow Sap", "Friendship in a Bottle, definitely not blood of a Tree", FL.Sap_Rainbow            .make(250), TC.stack(TC.VITREUS, 1), TC.stack(TC.AQUA           , 1), TC.stack(TC.AURAM     , 1), FoodStatFluid.INSTANCE, OD.container250rainbowsap, "foodRainbowsap", TD.Creative.HIDDEN); Sandwiches.INGREDIENTS.put(last(), (byte)40);
		
		CR.shapeless(make(4, 3700), CR.DEF, new Object[] {OD.container1000rainbowsap, OP.bottle.dat(MT.Empty), OP.bottle.dat(MT.Empty), OP.bottle.dat(MT.Empty), OP.bottle.dat(MT.Empty)});
		CR.shapeless(make(3, 3700), CR.DEF, new Object[] {OD.container1000rainbowsap, OP.bottle.dat(MT.Empty), OP.bottle.dat(MT.Empty), OP.bottle.dat(MT.Empty)});
		CR.shapeless(make(2, 3700), CR.DEF, new Object[] {OD.container1000rainbowsap, OP.bottle.dat(MT.Empty), OP.bottle.dat(MT.Empty)});
		CR.shapeless(make(1, 3700), CR.DEF, new Object[] {OD.container1000rainbowsap, OP.bottle.dat(MT.Empty)});
		});
		
		aItems.add(3800, aID -> {addItem( aID, "Cherry Juice"                 , "", "foodCherryjuice"                 , FL.Juice_Cherry           .make(250), TC.stack(TC.VITREUS, 1), TC.stack(TC.AQUA           , 1), TC.stack(TC.HERBA     , 1), FoodStatFluid.INSTANCE, OD.container250juice);});
		aItems.add(3805, aID -> {addItem( aID, "Cherry Smoothie"              , "", "foodCherrysmoothie"              , FL.Smoothie_Cherry        .make(250), TC.stack(TC.VITREUS, 1), TC.stack(TC.GELUM          , 1), TC.stack(TC.HERBA     , 1), FoodStatFluid.INSTANCE);});
		
		aItems.add(3900, aID -> {addItem( aID, "Ananas Juice"                 , "", "foodAnanasjuice"                 , FL.Juice_Ananas           .make(250), TC.stack(TC.VITREUS, 1), TC.stack(TC.AQUA           , 1), TC.stack(TC.ARBOR     , 1), FoodStatFluid.INSTANCE, OD.container250juice);});
		aItems.add(3901, aID -> {addItem( aID, "Ananas Cider"                 , "", "foodAnanascider"                 , FL.Cider_Ananas           .make(250), TC.stack(TC.VITREUS, 1), TC.stack(TC.VENEMUM        , 1), TC.stack(TC.TUTAMEN   , 1), FoodStatFluid.INSTANCE);});
		aItems.add(3905, aID -> {addItem( aID, "Ananas Smoothie"              , "", "foodAnanassmoothie"              , FL.Smoothie_Ananas        .make(250), TC.stack(TC.VITREUS, 1), TC.stack(TC.GELUM          , 1), TC.stack(TC.ARBOR     , 1), FoodStatFluid.INSTANCE);});
		
		aItems.add(4000, aID -> {addItem( aID, "Currant Juice"                , "", "foodCurrantjuice"                , FL.Juice_Currant          .make(250), TC.stack(TC.VITREUS, 1), TC.stack(TC.AQUA           , 1), TC.stack(TC.HERBA     , 1), FoodStatFluid.INSTANCE, OD.container250juice);});
		aItems.add(4005, aID -> {addItem( aID, "Currant Smoothie"             , "", "foodCurrantsmoothie"             , FL.Smoothie_Currant       .make(250), TC.stack(TC.VITREUS, 1), TC.stack(TC.GELUM          , 1), TC.stack(TC.HERBA     , 1), FoodStatFluid.INSTANCE);});
		
		aItems.add(4100, aID -> {addItem( aID, "Plum Juice"                   , "", "foodPlumjuice"                   , FL.Juice_Plum             .make(250), TC.stack(TC.VITREUS, 1), TC.stack(TC.AQUA           , 1), TC.stack(TC.SANO      , 1), FoodStatFluid.INSTANCE, OD.container250juice);});
		aItems.add(4105, aID -> {addItem( aID, "Plum Smoothie"                , "", "foodPlumsmoothie"                , FL.Smoothie_Plum          .make(250), TC.stack(TC.VITREUS, 1), TC.stack(TC.GELUM          , 1), TC.stack(TC.SANO      , 1), FoodStatFluid.INSTANCE);});
		
		aItems.add(4200, aID -> {addItem( aID, "Peach Juice"                  , "", "foodPeachjuice"                  , FL.Juice_Peach            .make(250), TC.stack(TC.VITREUS, 1), TC.stack(TC.AQUA           , 1), TC.stack(TC.VICTUS    , 1), FoodStatFluid.INSTANCE, OD.container250juice);});
		aItems.add(4205, aID -> {addItem( aID, "Peach Smoothie"               , "", "foodPeachsmoothie"               , FL.Smoothie_Peach         .make(250), TC.stack(TC.VITREUS, 1), TC.stack(TC.GELUM          , 1), TC.stack(TC.VICTUS    , 1), FoodStatFluid.INSTANCE);});
		
		aItems.add(4300, aID -> {addItem( aID, "Elderberry Juice"             , "", "foodElderberryjuice"             , FL.Juice_Elderberry       .make(250), TC.stack(TC.VITREUS, 1), TC.stack(TC.AQUA           , 1), TC.stack(TC.STRONTIO  , 1), FoodStatFluid.INSTANCE, OD.container250juice);});
		aItems.add(4305, aID -> {addItem( aID, "Elderberry Smoothie"          , "", "foodElderberrysmoothie"          , FL.Smoothie_Elderberry    .make(250), TC.stack(TC.VITREUS, 1), TC.stack(TC.GELUM          , 1), TC.stack(TC.STRONTIO  , 1), FoodStatFluid.INSTANCE);});
		
		aItems.add(4400, aID -> {addItem( aID, "Grapefruit Juice"             , "", "foodGrapefruitjuice"             , FL.Juice_Grapefruit       .make(250), TC.stack(TC.VITREUS, 1), TC.stack(TC.AQUA           , 1), TC.stack(TC.FAMES     , 1), FoodStatFluid.INSTANCE, OD.container250juice);});
		aItems.add(4405, aID -> {addItem( aID, "Grapefruit Smoothie"          , "", "foodGrapefruitsmoothie"          , FL.Smoothie_Grapefruit    .make(250), TC.stack(TC.VITREUS, 1), TC.stack(TC.GELUM          , 1), TC.stack(TC.FAMES     , 1), FoodStatFluid.INSTANCE);});
		
		aItems.add(4500, aID -> {addItem( aID, "Lime Juice"                   , "", "foodLimejuice"                   , FL.Juice_Lime             .make(250), TC.stack(TC.VITREUS, 1), TC.stack(TC.AQUA           , 1), TC.stack(TC.PERFODIO  , 1), FoodStatFluid.INSTANCE, OD.container250juice);});
		aItems.add(4505, aID -> {addItem( aID, "Lime Smoothie"                , "", "foodLimesmoothie"                , FL.Smoothie_Lime          .make(250), TC.stack(TC.VITREUS, 1), TC.stack(TC.GELUM          , 1), TC.stack(TC.PERFODIO  , 1), FoodStatFluid.INSTANCE);});
		
		aItems.add(4600, aID -> {addItem( aID, "Orange Juice"                 , "", "foodOrangejuice"                 , FL.Juice_Orange           .make(250), TC.stack(TC.VITREUS, 1), TC.stack(TC.AQUA           , 1), TC.stack(TC.PERFODIO  , 1), FoodStatFluid.INSTANCE, OD.container250juice);});
		aItems.add(4605, aID -> {addItem( aID, "Orange Smoothie"              , "", "foodOrangesmoothie"              , FL.Smoothie_Orange        .make(250), TC.stack(TC.VITREUS, 1), TC.stack(TC.GELUM          , 1), TC.stack(TC.PERFODIO  , 1), FoodStatFluid.INSTANCE);});
		
		aItems.add(4700, aID -> {addItem( aID, "Apricot Juice"                , "", "foodApricotjuice"                , FL.Juice_Apricot          .make(250), TC.stack(TC.VITREUS, 1), TC.stack(TC.AQUA           , 1), TC.stack(TC.HERBA     , 1), FoodStatFluid.INSTANCE, OD.container250juice);});
		aItems.add(4705, aID -> {addItem( aID, "Apricot Smoothie"             , "", "foodApricotsmoothie"             , FL.Smoothie_Apricot       .make(250), TC.stack(TC.VITREUS, 1), TC.stack(TC.GELUM          , 1), TC.stack(TC.HERBA     , 1), FoodStatFluid.INSTANCE);});
		
		aItems.add(4800, aID -> {addItem( aID, "Pear Juice"                   , "", "foodPearjuice"                   , FL.Juice_Pear             .make(250), TC.stack(TC.VITREUS, 1), TC.stack(TC.AQUA           , 1), TC.stack(TC.HERBA     , 1), FoodStatFluid.INSTANCE, OD.container250juice);});
		aItems.add(4805, aID -> {addItem( aID, "Pear Smoothie"                , "", "foodPearsmoothie"                , FL.Smoothie_Pear          .make(250), TC.stack(TC.VITREUS, 1), TC.stack(TC.GELUM          , 1), TC.stack(TC.HERBA     , 1), FoodStatFluid.INSTANCE);});
		
		aItems.add(4900, aID -> {addItem( aID, "Pumpkin Juice"                , "", "foodPumpkinjuice"                , FL.Juice_Pumpkin          .make(250), TC.stack(TC.VITREUS, 1), TC.stack(TC.AQUA           , 1), TC.stack(TC.HERBA     , 1), FoodStatFluid.INSTANCE, OD.container250juice);});
		
		aItems.add(5000, aID -> {addItem( aID, "Persimmon Juice"              , "", "foodPersimmonjuice"              , FL.Juice_Persimmon        .make(250), TC.stack(TC.VITREUS, 1), TC.stack(TC.AQUA           , 1), TC.stack(TC.PERFODIO  , 1), FoodStatFluid.INSTANCE, OD.container250juice);});
		aItems.add(5005, aID -> {addItem( aID, "Persimmon Smoothie"           , "", "foodPersimmonsmoothie"           , FL.Smoothie_Persimmon     .make(250), TC.stack(TC.VITREUS, 1), TC.stack(TC.GELUM          , 1), TC.stack(TC.PERFODIO  , 1), FoodStatFluid.INSTANCE);});
		
		aItems.add(5100, aID -> {addItem( aID, "Starfruit Juice"              , "", "foodStarfruitjuice"              , FL.Juice_Starfruit        .make(250), TC.stack(TC.VITREUS, 1), TC.stack(TC.AQUA           , 1), TC.stack(TC.HERBA     , 1), FoodStatFluid.INSTANCE, OD.container250juice);});
		aItems.add(5105, aID -> {addItem( aID, "Starfruit Smoothie"           , "", "foodStarfruitsmoothie"           , FL.Smoothie_Starfruit     .make(250), TC.stack(TC.VITREUS, 1), TC.stack(TC.GELUM          , 1), TC.stack(TC.HERBA     , 1), FoodStatFluid.INSTANCE);});
		
		aItems.add(5200, aID -> {addItem( aID, "Fig Juice"                    , "", "foodFigjuice"                    , FL.Juice_Fig              .make(250), TC.stack(TC.VITREUS, 1), TC.stack(TC.AQUA           , 1), TC.stack(TC.GRANUM    , 1), FoodStatFluid.INSTANCE, OD.container250juice);});
		aItems.add(5205, aID -> {addItem( aID, "Fig Smoothie"                 , "", "foodFigsmoothie"                 , FL.Smoothie_Fig           .make(250), TC.stack(TC.VITREUS, 1), TC.stack(TC.GELUM          , 1), TC.stack(TC.GRANUM    , 1), FoodStatFluid.INSTANCE);});
		
		aItems.add(5300, aID -> {addItem( aID, "Pomegranate Juice"            , "", "foodPomegranatejuice"            , FL.Juice_Pomegranate      .make(250), TC.stack(TC.VITREUS, 1), TC.stack(TC.AQUA           , 1), TC.stack(TC.GRANUM    , 1), FoodStatFluid.INSTANCE, OD.container250juice);});
		aItems.add(5305, aID -> {addItem( aID, "Pomegranate Smoothie"         , "", "foodPomegranatesmoothie"         , FL.Smoothie_Pomegranate   .make(250), TC.stack(TC.VITREUS, 1), TC.stack(TC.GELUM          , 1), TC.stack(TC.GRANUM    , 1), FoodStatFluid.INSTANCE);});
		
		aItems.add(5400, aID -> {addItem( aID, "Mango Juice"                  , "", "foodMangojuice"                  , FL.Juice_Mango            .make(250), TC.stack(TC.VITREUS, 1), TC.stack(TC.AQUA           , 1), TC.stack(TC.HERBA     , 1), FoodStatFluid.INSTANCE, OD.container250juice);});
		aItems.add(5405, aID -> {addItem( aID, "Mango Smoothie"               , "", "foodMangosmoothie"               , FL.Smoothie_Mango         .make(250), TC.stack(TC.VITREUS, 1), TC.stack(TC.GELUM          , 1), TC.stack(TC.HERBA     , 1), FoodStatFluid.INSTANCE);});
		
		aItems.add(5500, aID -> {addItem( aID, "Papaya Juice"                 , "", "foodPapayajuice"                 , FL.Juice_Papaya           .make(250), TC.stack(TC.VITREUS, 1), TC.stack(TC.AQUA           , 1), TC.stack(TC.HERBA     , 1), FoodStatFluid.INSTANCE, OD.container250juice);});
		aItems.add(5505, aID -> {addItem( aID, "Papaya Smoothie"              , "", "foodPapayasmoothie"              , FL.Smoothie_Papaya        .make(250), TC.stack(TC.VITREUS, 1), TC.stack(TC.GELUM          , 1), TC.stack(TC.HERBA     , 1), FoodStatFluid.INSTANCE);});
		
		aItems.add(5600, aID -> {addItem( aID, "Coconut Milk"      , "Cooking Oil", "foodCoconutmilk"                 , FL.Juice_Coconut          .make(250), TC.stack(TC.VITREUS, 1), TC.stack(TC.AQUA           , 1), TC.stack(TC.SANO      , 1), FoodStatFluid.INSTANCE, OD.container250juice);});
		aItems.add(5604, aID -> {addItem( aID, "Coconut Cream"                , "", "foodCoconutcream"                , FL.Cream_Coconut          .make(250), TC.stack(TC.VITREUS, 1), TC.stack(TC.LIMUS          , 1), TC.stack(TC.POTENTIA  , 1), FoodStatFluid.INSTANCE); Sandwiches.INGREDIENTS.put(last(), DYE_INDEX_White);});
		aItems.add(5605, aID -> {addItem( aID, "Coconut Smoothie"             , "", "foodCoconutsmoothie"             , FL.Smoothie_Coconut       .make(250), TC.stack(TC.VITREUS, 1), TC.stack(TC.GELUM          , 1), TC.stack(TC.SANO      , 1), FoodStatFluid.INSTANCE);});
		
		aItems.add(5700, aID -> {addItem( aID, "Beet Juice"                   , "", "foodBeetjuice"                   , FL.Juice_Beet             .make(250), TC.stack(TC.VITREUS, 1), TC.stack(TC.AQUA           , 1), TC.stack(TC.HERBA     , 1), FoodStatFluid.INSTANCE, OD.container250juice);});
		
		aItems.add(30000, aID -> {addItem(aID, "Medicine"                     , ""                                    , FL.Med_Heal               .make(250), TC.stack(TC.VITREUS, 1), TC.stack(TC.SANO           , 2), FoodStatFluid.INSTANCE);});
		aItems.add(30001, aID -> {addItem(aID, "Laxative"                     , ""                                    , FL.Med_Laxative           .make(250), TC.stack(TC.VITREUS, 1), TC.stack(TC.SANO           , 1), TC.stack(TC.FAMES     , 1), FoodStatFluid.INSTANCE);});
		
		aItems.add(32000, aID -> {
			IL.Bottle_Ink.set(
		addItem(aID  , "Ink Bottle"           , "Color: " + DYE_NAMES[DYE_INDEX_Black ], DYE_OREDICTS[DYE_INDEX_Black ], TC.stack(TC.VITREUS, 1), TC.stack(TC.SENSUS, 1), FL.make("squidink", L)));
		});
		
		aItems.add(32001, aID -> {
			IL.Bottle_Indigo.set(
		addItem(aID  , "Bottled Indigo Dye"   , "Color: " + DYE_NAMES[DYE_INDEX_Blue  ], DYE_OREDICTS[DYE_INDEX_Blue  ], TC.stack(TC.VITREUS, 1), TC.stack(TC.SENSUS, 1), FL.make("indigo", L)));
		});
		
		for (byte i = 0; i < 16; i++) {
			final byte tIndex = i;
			aItems.addAll(new int[] {32100+tIndex, 32116+tIndex, 32132+tIndex}, aIDs -> {
			addItem(aIDs[0], "Bottled Water Dye"    , "Color: " + DYE_NAMES[tIndex               ], DYE_OREDICTS[tIndex               ], TC.stack(TC.VITREUS, 1), TC.stack(TC.SENSUS, 1), DYE_FLUIDS_WATER[tIndex]);
		addItem(aIDs[1], "Bottled Chemical Dye" , "Color: " + DYE_NAMES[tIndex               ], DYE_OREDICTS[tIndex               ], TC.stack(TC.VITREUS, 1), TC.stack(TC.SENSUS, 1), DYE_FLUIDS_CHEMICAL[tIndex]);
		addItem(aIDs[2], "Bottled Flower Dye"   , "Color: " + DYE_NAMES[tIndex               ], DYE_OREDICTS[tIndex               ], TC.stack(TC.VITREUS, 1), TC.stack(TC.SENSUS, 1), DYE_FLUIDS_FLOWER[tIndex]);
		});
		}
		
		aItems.add(32760, aID -> {
			IL.Bottle_Poison.set(
		addItem(aID, "Bottle of Poison", "", "bottlePoison", OD.container250poison, FL.Poison.make(250), TC.stack(TC.VITREUS, 1), TC.stack(TC.VENEMUM, 2), FoodStatFluid.INSTANCE)); Sandwiches.INGREDIENTS.put(last(), DYE_INDEX_Purple);
		
		CR.shapeless(last(4), CR.DEF, new Object[] {OD.container1000poison, OP.bottle.dat(MT.Empty), OP.bottle.dat(MT.Empty), OP.bottle.dat(MT.Empty), OP.bottle.dat(MT.Empty)});
		CR.shapeless(last(3), CR.DEF, new Object[] {OD.container1000poison, OP.bottle.dat(MT.Empty), OP.bottle.dat(MT.Empty), OP.bottle.dat(MT.Empty)});
		CR.shapeless(last(2), CR.DEF, new Object[] {OD.container1000poison, OP.bottle.dat(MT.Empty), OP.bottle.dat(MT.Empty)});
		CR.shapeless(last(1), CR.DEF, new Object[] {OD.container1000poison, OP.bottle.dat(MT.Empty)});
		CR.shapeless(last(2), CR.DEF, new Object[] {OD.container500poison , OP.bottle.dat(MT.Empty), OP.bottle.dat(MT.Empty)});
		CR.shapeless(last(1), CR.DEF, new Object[] {OD.container500poison , OP.bottle.dat(MT.Empty)});
		});
		
		aItems.add(32761, aID -> {
			IL.Bottle_Loot.set(
		addItem(aID, "Clouded Bottle", "Loot: A random Bottle", TC.stack(TC.VITREUS, 1), TC.stack(TC.LUCRUM, 1), new Behavior_Drop_Loot("gt.bottles")));
		});
		
		aItems.add(32762, aID -> {
			IL.Bottle_Tar.set(
		addItem(aID, "Tar Bottle", "Can be used as Glue too", "bottleTar", DYE_OREDICTS[DYE_INDEX_Black ], FL.Tar.make(250), FL.make("tarfluid", 250), TC.stack(TC.VITREUS, 1), TC.stack(TC.LIMUS, 1), TC.stack(TC.IGNIS, 1), FoodStatFluid.INSTANCE));
		
		CR.shapeless(last(4), CR.DEF, new Object[] {OD.container1000tar, OP.bottle.dat(MT.Empty), OP.bottle.dat(MT.Empty), OP.bottle.dat(MT.Empty), OP.bottle.dat(MT.Empty)});
		CR.shapeless(last(3), CR.DEF, new Object[] {OD.container1000tar, OP.bottle.dat(MT.Empty), OP.bottle.dat(MT.Empty), OP.bottle.dat(MT.Empty)});
		CR.shapeless(last(2), CR.DEF, new Object[] {OD.container1000tar, OP.bottle.dat(MT.Empty), OP.bottle.dat(MT.Empty)});
		CR.shapeless(last(1), CR.DEF, new Object[] {OD.container1000tar, OP.bottle.dat(MT.Empty)});
		
		CR.shapeless(last(1), CR.DEF, new Object[] {OP.bottle.dat(MT.Empty), OD.itemTar});
		});
		
		aItems.add(32763, aID -> {
			IL.Bottle_Blood.set(
		addItem(aID, "Bottle o'Blood", "", "bottleBlood", OD.container250blood, FL.Blood.make(250), TC.stack(TC.VITREUS, 1), TC.stack(TC.CORPUS, 2), FoodStatFluid.INSTANCE)); Sandwiches.INGREDIENTS.put(last(), DYE_INDEX_Red);
		
		CR.shapeless(last(4), CR.DEF, new Object[] {OD.container1000blood, OP.bottle.dat(MT.Empty), OP.bottle.dat(MT.Empty), OP.bottle.dat(MT.Empty), OP.bottle.dat(MT.Empty)});
		CR.shapeless(last(3), CR.DEF, new Object[] {OD.container1000blood, OP.bottle.dat(MT.Empty), OP.bottle.dat(MT.Empty), OP.bottle.dat(MT.Empty)});
		CR.shapeless(last(2), CR.DEF, new Object[] {OD.container1000blood, OP.bottle.dat(MT.Empty), OP.bottle.dat(MT.Empty)});
		CR.shapeless(last(1), CR.DEF, new Object[] {OD.container1000blood, OP.bottle.dat(MT.Empty)});
		
		CR.shapeless(last(1), CR.DEF, new Object[] {OP.bottle.dat(MT.Empty), OD.itemBlood});
		});
		
		aItems.add(32764, aID -> {
			IL.Bottle_Lubricant.set(
		addItem(aID, "Lubricant Bottle", "", OP.bottle.dat(MT.Lubricant), FL.LubRoCant.make(250), FL.Lubricant.make(250), TC.stack(TC.VITREUS, 1), TC.stack(TC.LIMUS, 2), FoodStatFluid.INSTANCE));
		
		CR.shapeless(last(4), CR.DEF, new Object[] {OD.container1000lubricant, OP.bottle.dat(MT.Empty), OP.bottle.dat(MT.Empty), OP.bottle.dat(MT.Empty), OP.bottle.dat(MT.Empty)});
		CR.shapeless(last(3), CR.DEF, new Object[] {OD.container1000lubricant, OP.bottle.dat(MT.Empty), OP.bottle.dat(MT.Empty), OP.bottle.dat(MT.Empty)});
		CR.shapeless(last(2), CR.DEF, new Object[] {OD.container1000lubricant, OP.bottle.dat(MT.Empty), OP.bottle.dat(MT.Empty)});
		CR.shapeless(last(1), CR.DEF, new Object[] {OD.container1000lubricant, OP.bottle.dat(MT.Empty)});
		});
		
		aItems.add(32765, aID -> {
			IL.Bottle_Mercury.set(
		addItem(aID, "Mercury Bottle", "Also called Quicksilver", new OreDictItemData(MT.Hg, U, MT.Glass, U), OP.bottle.dat(MT.Hg).toString() /* <- yes that toString() is needed! */, OD.itemQuicksilver, MT.Hg.liquid( U, T), TC.stack(TC.VITREUS, 1), TC.stack(TC.VENEMUM, 2), FoodStatFluid.INSTANCE));
		
		CR.shapeless(last(1), CR.DEF, new Object[] {OD.itemQuicksilver, OP.bottle.dat(MT.Empty)});
		CR.shapeless(last(1), CR.DEF, new Object[] {OP.chunkGt.dat(MT.Hg), OP.chunkGt.dat(MT.Hg), OP.chunkGt.dat(MT.Hg), OP.chunkGt.dat(MT.Hg), OP.bottle.dat(MT.Empty)});
		CR.shapeless(last(1), CR.DEF, new Object[] {OP.billet.dat(MT.Hg), OP.nugget.dat(MT.Hg), OP.nugget.dat(MT.Hg), OP.nugget.dat(MT.Hg), OP.bottle.dat(MT.Empty)});
		CR.shapeless(last(2), CR.DEF, new Object[] {OP.billet.dat(MT.Hg), OP.billet.dat(MT.Hg), OP.billet.dat(MT.Hg), OP.bottle.dat(MT.Empty), OP.bottle.dat(MT.Empty)});
		CR.shapeless(last(1), CR.DEF, new Object[] {IL.HBM_Mercury_Drop, IL.HBM_Mercury_Drop, IL.HBM_Mercury_Drop, IL.HBM_Mercury_Drop, IL.HBM_Mercury_Drop, IL.HBM_Mercury_Drop, IL.HBM_Mercury_Drop, IL.HBM_Mercury_Drop, OP.bottle.dat(MT.Empty)});
		});
		
		aItems.add(32766, aID -> {
			IL.Bottle_Glue.set(
		addItem(aID, "Glue Bottle", "", OP.bottle.dat(MT.Glue), FL.Glue.make(250), TC.stack(TC.VITREUS, 1), TC.stack(TC.LIMUS, 2), FoodStatFluid.INSTANCE));
		
		CR.shapeless(last(4), CR.DEF, new Object[] {OD.container1000glue, OP.bottle.dat(MT.Empty), OP.bottle.dat(MT.Empty), OP.bottle.dat(MT.Empty), OP.bottle.dat(MT.Empty)});
		CR.shapeless(last(3), CR.DEF, new Object[] {OD.container1000glue, OP.bottle.dat(MT.Empty), OP.bottle.dat(MT.Empty), OP.bottle.dat(MT.Empty)});
		CR.shapeless(last(2), CR.DEF, new Object[] {OD.container1000glue, OP.bottle.dat(MT.Empty), OP.bottle.dat(MT.Empty)});
		CR.shapeless(last(1), CR.DEF, new Object[] {OD.container1000glue, OP.bottle.dat(MT.Empty)});
		});
		
		// TODO foodChocolatemilk
	}
	
	@Override
	public ItemStack getContainerItemDefault(ItemStack aStack) {
		return IL.Bottle_Empty.get(1);
	}
	
	@Override
	public int getDefaultStackLimit(ItemStack aStack) {
		return ST.meta_(aStack) >= 32000 ? 64 : OP.bottle.mDefaultStackSize;
	}
	
	@Override
	public ItemStack getRotten(ItemStack aStack) {
		short aMeta = ST.meta_(aStack);
		
		switch(aMeta) {
		case    5: case    6: case    7: case 1102: case 1601: case 1700: return aStack;
		case    0: case    1: case    2: case    3: case    4: return ST.make(metaFamily(), aStack.getCount(), 5, ItemNBT.get(aStack));
		case 1100: case 1200: return    ST.make(metaFamily(), aStack.getCount(), 1102, ItemNBT.get(aStack));
		default: return aMeta < 30000 ? ST.make(metaFamily(), aStack.getCount(), 1601, ItemNBT.get(aStack)) : aStack;
		}
	}
	
	@Override public ItemStack getRotten(ItemStack aStack, Level aWorld, int aX, int aY, int aZ) {return getRotten(aStack);}
}

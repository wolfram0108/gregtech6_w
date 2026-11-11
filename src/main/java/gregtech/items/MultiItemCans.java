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
import gregapi.item.multiitem.behaviors.Behavior_FeedCat;
import gregapi.item.multiitem.behaviors.Behavior_FeedDog;
import gregapi.item.multiitem.food.FoodStat;
import gregapi.util.CR;
import gregapi.util.ST;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.level.Level;

import static gregapi.data.CS.*;

public class MultiItemCans extends MultiItemRandomWithCompat implements IItemRottable {
	private MultiItemCans(MultiItemCans aFamily, short aMeta) {super(aFamily, aMeta);}
	@Override protected gregapi.item.multiitem.MultiItemRandom createVariant(short aMeta) {return new MultiItemCans(this, aMeta);}
	public MultiItemCans(String aModID, String aUnlocalized) {
		super(aModID, aUnlocalized);
		new gregapi.item.CreativeTab(getUnlocalizedName(), "GregTech: Cans", this, (short)74); // F16 creative tab: own GT tab (icon+displayItems), registered by CreativeTabsGT on RegisterEvent<CreativeModeTab>. 1:1.
	}
	
	protected void defineItems(gregapi.item.ItemTypeDefinitions aItems) {
		aItems.add(1, aID -> {IL.Food_Can_Undefined_1.set(addItem(    aID, "Tiny Food Can (Unknown)"      , "", new FoodStat( 2, 0.1F,  5, C+37, 0.10F,  0,  0,  0,  0,  0, ItemUseAnimation.EAT, NI, F, F, F, T), TC.stack(TC.FAMES, 1), TC.stack(TC.FABRICO, 1))); CR.remove(last());});
		aItems.add(2, aID -> {IL.Food_Can_Undefined_2.set(addItem(    aID, "Small Food Can (Unknown)"     , "", new FoodStat( 4, 0.2F, 10, C+37, 0.10F,  0,  0,  0,  0,  0, ItemUseAnimation.EAT, NI, F, F, F, T), TC.stack(TC.FAMES, 1), TC.stack(TC.FABRICO, 1))); CR.remove(last());});
		aItems.add(3, aID -> {IL.Food_Can_Undefined_3.set(addItem(    aID, "Tall Food Can (Unknown)"      , "", new FoodStat( 6, 0.3F, 15, C+37, 0.10F,  0,  0,  0,  0,  0, ItemUseAnimation.EAT, NI, F, F, F, T), TC.stack(TC.FAMES, 2), TC.stack(TC.FABRICO, 1))); CR.remove(last());});
		aItems.add(4, aID -> {IL.Food_Can_Undefined_4.set(addItem(    aID, "Wide Food Can (Unknown)"      , "", new FoodStat( 8, 0.4F, 20, C+37, 0.10F,  0,  0,  0,  0,  0, ItemUseAnimation.EAT, NI, F, F, F, T), TC.stack(TC.FAMES, 2), TC.stack(TC.FABRICO, 1))); CR.remove(last());});
		aItems.add(5, aID -> {IL.Food_Can_Undefined_5.set(addItem(    aID, "Large Food Can (Unknown)"     , "", new FoodStat(10, 0.5F, 25, C+37, 0.10F,  0,  0,  0,  0,  0, ItemUseAnimation.EAT, NI, F, F, F, T), TC.stack(TC.FAMES, 3), TC.stack(TC.FABRICO, 1))); CR.remove(last());});
		aItems.add(6, aID -> {IL.Food_Can_Undefined_6.set(addItem(    aID, "Huge Food Can (Unknown)"      , "", new FoodStat(12, 0.6F, 30, C+37, 0.10F,  0,  0,  0,  0,  0, ItemUseAnimation.EAT, NI, F, F, F, T), TC.stack(TC.FAMES, 3), TC.stack(TC.FABRICO, 1))); CR.remove(last());});
		
		aItems.add(11, aID -> {IL.Food_Can_Rotten_1   .set(addItem(   aID, "Tiny Food Can (Rotten)"       , "", new FoodStat( 2, 0.1F,  5, C+37, 0.10F,  4,  0,  4,  4,  0, ItemUseAnimation.EAT, NI, F, F, T, T, /*hunger*/17, 300, 0, 40), TC.stack(TC.FAMES, 1), TC.stack(TC.FABRICO, 1), TD.Creative.HIDDEN)); CR.remove(last()); Sandwiches.INGREDIENTS.put(last(), (byte)29);});
		aItems.add(12, aID -> {IL.Food_Can_Rotten_2   .set(addItem(   aID, "Small Food Can (Rotten)"      , "", new FoodStat( 4, 0.2F, 10, C+37, 0.10F,  8,  0,  8,  8,  0, ItemUseAnimation.EAT, NI, F, F, T, T, /*hunger*/17, 300, 0, 50), TC.stack(TC.FAMES, 1), TC.stack(TC.FABRICO, 1), TD.Creative.HIDDEN)); CR.remove(last()); Sandwiches.INGREDIENTS.put(last(), (byte)29);});
		aItems.add(13, aID -> {IL.Food_Can_Rotten_3   .set(addItem(   aID, "Tall Food Can (Rotten)"       , "", new FoodStat( 6, 0.3F, 15, C+37, 0.10F, 12,  0, 12, 12,  0, ItemUseAnimation.EAT, NI, F, F, T, T, /*hunger*/17, 300, 0, 60), TC.stack(TC.FAMES, 2), TC.stack(TC.FABRICO, 1), TD.Creative.HIDDEN)); CR.remove(last()); Sandwiches.INGREDIENTS.put(last(), (byte)29);});
		aItems.add(14, aID -> {IL.Food_Can_Rotten_4   .set(addItem(   aID, "Wide Food Can (Rotten)"       , "", new FoodStat( 8, 0.4F, 20, C+37, 0.10F, 16,  0, 16, 16,  0, ItemUseAnimation.EAT, NI, F, F, T, T, /*hunger*/17, 300, 0, 70), TC.stack(TC.FAMES, 2), TC.stack(TC.FABRICO, 1), TD.Creative.HIDDEN)); CR.remove(last()); Sandwiches.INGREDIENTS.put(last(), (byte)29);});
		aItems.add(15, aID -> {IL.Food_Can_Rotten_5   .set(addItem(   aID, "Large Food Can (Rotten)"      , "", new FoodStat(10, 0.5F, 25, C+37, 0.10F, 20,  0, 20, 20,  0, ItemUseAnimation.EAT, NI, F, F, T, T, /*hunger*/17, 300, 0, 80), TC.stack(TC.FAMES, 3), TC.stack(TC.FABRICO, 1), TD.Creative.HIDDEN)); CR.remove(last()); Sandwiches.INGREDIENTS.put(last(), (byte)29);});
		aItems.add(16, aID -> {IL.Food_Can_Rotten_6   .set(addItem(   aID, "Huge Food Can (Rotten)"       , "", new FoodStat(12, 0.6F, 30, C+37, 0.10F, 24,  0, 24, 24,  0, ItemUseAnimation.EAT, NI, F, F, T, T, /*hunger*/17, 300, 0, 90), TC.stack(TC.FAMES, 3), TC.stack(TC.FABRICO, 1), TD.Creative.HIDDEN)); CR.remove(last()); Sandwiches.INGREDIENTS.put(last(), (byte)29);});
		
		aItems.add(21, aID -> {IL.Food_Can_Veggie_1   .set(addItem(   aID, "Tiny Food Can (Vegetables)"   , "", new FoodStat( 2, 0.1F, 10, C+37, 0.10F,  0,  0,  0,  5,  0, ItemUseAnimation.EAT, NI, F, F, F, T), TC.stack(TC.HERBA, 1), TC.stack(TC.FABRICO, 1), "listAllveggie")); CR.remove(last());});
		aItems.add(22, aID -> {IL.Food_Can_Veggie_2   .set(addItem(   aID, "Small Food Can (Vegetables)"  , "", new FoodStat( 4, 0.2F, 20, C+37, 0.10F,  0,  0,  0, 10,  0, ItemUseAnimation.EAT, NI, F, F, F, T), TC.stack(TC.HERBA, 1), TC.stack(TC.FABRICO, 1), "listAllveggie")); CR.remove(last());});
		aItems.add(23, aID -> {IL.Food_Can_Veggie_3   .set(addItem(   aID, "Tall Food Can (Vegetables)"   , "", new FoodStat( 6, 0.3F, 30, C+37, 0.10F,  0,  0,  0, 15,  0, ItemUseAnimation.EAT, NI, F, F, F, T), TC.stack(TC.HERBA, 2), TC.stack(TC.FABRICO, 1), "listAllveggie")); CR.remove(last());});
		aItems.add(24, aID -> {IL.Food_Can_Veggie_4   .set(addItem(   aID, "Wide Food Can (Vegetables)"   , "", new FoodStat( 8, 0.4F, 40, C+37, 0.10F,  0,  0,  0, 20,  0, ItemUseAnimation.EAT, NI, F, F, F, T), TC.stack(TC.HERBA, 2), TC.stack(TC.FABRICO, 1), "listAllveggie")); CR.remove(last());});
		aItems.add(25, aID -> {IL.Food_Can_Veggie_5   .set(addItem(   aID, "Large Food Can (Vegetables)"  , "", new FoodStat(10, 0.5F, 50, C+37, 0.10F,  0,  0,  0, 25,  0, ItemUseAnimation.EAT, NI, F, F, F, T), TC.stack(TC.HERBA, 3), TC.stack(TC.FABRICO, 1), "listAllveggie")); CR.remove(last());});
		aItems.add(26, aID -> {IL.Food_Can_Veggie_6   .set(addItem(   aID, "Huge Food Can (Vegetables)"   , "", new FoodStat(12, 0.6F, 60, C+37, 0.10F,  0,  0,  0, 30,  0, ItemUseAnimation.EAT, NI, F, F, F, T), TC.stack(TC.HERBA, 3), TC.stack(TC.FABRICO, 1), "listAllveggie")); CR.remove(last());});
		
		aItems.add(31, aID -> {IL.Food_Can_Fruit_1    .set(addItem(   aID, "Tiny Food Can (Fruits)"       , "", new FoodStat( 2, 0.1F, 15, C+37, 0.30F,  0,  0,  0, 10,  0, ItemUseAnimation.EAT, NI, F, F, F, T), TC.stack(TC.HERBA, 1), TC.stack(TC.FABRICO, 1), "listAllfruit")); CR.remove(last());});
		aItems.add(32, aID -> {IL.Food_Can_Fruit_2    .set(addItem(   aID, "Small Food Can (Fruits)"      , "", new FoodStat( 4, 0.2F, 30, C+37, 0.30F,  0,  0,  0, 20,  0, ItemUseAnimation.EAT, NI, F, F, F, T), TC.stack(TC.HERBA, 1), TC.stack(TC.FABRICO, 1), "listAllfruit")); CR.remove(last());});
		aItems.add(33, aID -> {IL.Food_Can_Fruit_3    .set(addItem(   aID, "Tall Food Can (Fruits)"       , "", new FoodStat( 6, 0.3F, 45, C+37, 0.30F,  0,  0,  0, 30,  0, ItemUseAnimation.EAT, NI, F, F, F, T), TC.stack(TC.HERBA, 2), TC.stack(TC.FABRICO, 1), "listAllfruit")); CR.remove(last());});
		aItems.add(34, aID -> {IL.Food_Can_Fruit_4    .set(addItem(   aID, "Wide Food Can (Fruits)"       , "", new FoodStat( 8, 0.4F, 60, C+37, 0.30F,  0,  0,  0, 40,  0, ItemUseAnimation.EAT, NI, F, F, F, T), TC.stack(TC.HERBA, 2), TC.stack(TC.FABRICO, 1), "listAllfruit")); CR.remove(last());});
		aItems.add(35, aID -> {IL.Food_Can_Fruit_5    .set(addItem(   aID, "Large Food Can (Fruits)"      , "", new FoodStat(10, 0.5F, 75, C+37, 0.30F,  0,  0,  0, 50,  0, ItemUseAnimation.EAT, NI, F, F, F, T), TC.stack(TC.HERBA, 3), TC.stack(TC.FABRICO, 1), "listAllfruit")); CR.remove(last());});
		aItems.add(36, aID -> {IL.Food_Can_Fruit_6    .set(addItem(   aID, "Huge Food Can (Fruits)"       , "", new FoodStat(12, 0.6F, 90, C+37, 0.30F,  0,  0,  0, 60,  0, ItemUseAnimation.EAT, NI, F, F, F, T), TC.stack(TC.HERBA, 3), TC.stack(TC.FABRICO, 1), "listAllfruit")); CR.remove(last());});
		
		aItems.add(41, aID -> {IL.Food_Can_Bread_1    .set(addItem(   aID, "Tiny Food Can (Bread)"        , "", new FoodStat( 2, 0.1F,  2, C+37, 0.10F,  0,  0,  0,  0,  0, ItemUseAnimation.EAT, NI, F, F, F, T), TC.stack(TC.HERBA, 1), TC.stack(TC.FABRICO, 1))); CR.remove(last());});
		aItems.add(42, aID -> {IL.Food_Can_Bread_2    .set(addItem(   aID, "Small Food Can (Bread)"       , "", new FoodStat( 4, 0.2F,  4, C+37, 0.10F,  0,  0,  0,  0,  0, ItemUseAnimation.EAT, NI, F, F, F, T), TC.stack(TC.HERBA, 1), TC.stack(TC.FABRICO, 1))); CR.remove(last());});
		aItems.add(43, aID -> {IL.Food_Can_Bread_3    .set(addItem(   aID, "Tall Food Can (Bread)"        , "", new FoodStat( 6, 0.3F,  6, C+37, 0.10F,  0,  0,  0,  0,  0, ItemUseAnimation.EAT, NI, F, F, F, T), TC.stack(TC.HERBA, 2), TC.stack(TC.FABRICO, 1))); CR.remove(last());});
		aItems.add(44, aID -> {IL.Food_Can_Bread_4    .set(addItem(   aID, "Wide Food Can (Bread)"        , "", new FoodStat( 8, 0.4F,  8, C+37, 0.10F,  0,  0,  0,  0,  0, ItemUseAnimation.EAT, NI, F, F, F, T), TC.stack(TC.HERBA, 2), TC.stack(TC.FABRICO, 1))); CR.remove(last());});
		aItems.add(45, aID -> {IL.Food_Can_Bread_5    .set(addItem(   aID, "Large Food Can (Bread)"       , "", new FoodStat(10, 0.5F, 10, C+37, 0.10F,  0,  0,  0,  0,  0, ItemUseAnimation.EAT, NI, F, F, F, T), TC.stack(TC.HERBA, 3), TC.stack(TC.FABRICO, 1))); CR.remove(last());});
		aItems.add(46, aID -> {IL.Food_Can_Bread_6    .set(addItem(   aID, "Huge Food Can (Bread)"        , "", new FoodStat(12, 0.6F, 12, C+37, 0.10F,  0,  0,  0,  0,  0, ItemUseAnimation.EAT, NI, F, F, F, T), TC.stack(TC.HERBA, 3), TC.stack(TC.FABRICO, 1))); CR.remove(last());});
		
		aItems.add(51, aID -> {IL.Food_Can_Meat_1     .set(addItem(   aID, "Tiny Food Can (Meat)"         , "", new FoodStat( 2, 0.1F,  5, C+37, 0.10F,  0,  0,  4,  0,  4, ItemUseAnimation.EAT, NI, F, F, F, T), TC.stack(TC.CORPUS, 1), TC.stack(TC.FABRICO, 1), Behavior_FeedCat.INSTANCE, Behavior_FeedDog.INSTANCE, "listAllmeatraw", "listAllmeatcooked")); CR.remove(last()); Sandwiches.INGREDIENTS.put(last(), (byte)29);});
		aItems.add(52, aID -> {IL.Food_Can_Meat_2     .set(addItem(   aID, "Small Food Can (Meat)"        , "", new FoodStat( 4, 0.2F, 10, C+37, 0.10F,  0,  0,  8,  0,  8, ItemUseAnimation.EAT, NI, F, F, F, T), TC.stack(TC.CORPUS, 1), TC.stack(TC.FABRICO, 1), Behavior_FeedCat.INSTANCE, Behavior_FeedDog.INSTANCE, "listAllmeatraw", "listAllmeatcooked")); CR.remove(last()); Sandwiches.INGREDIENTS.put(last(), (byte)29);});
		aItems.add(53, aID -> {IL.Food_Can_Meat_3     .set(addItem(   aID, "Tall Food Can (Meat)"         , "", new FoodStat( 6, 0.3F, 15, C+37, 0.10F,  0,  0, 12,  0, 12, ItemUseAnimation.EAT, NI, F, F, F, T), TC.stack(TC.CORPUS, 2), TC.stack(TC.FABRICO, 1), Behavior_FeedCat.INSTANCE, Behavior_FeedDog.INSTANCE, "listAllmeatraw", "listAllmeatcooked")); CR.remove(last()); Sandwiches.INGREDIENTS.put(last(), (byte)29);});
		aItems.add(54, aID -> {IL.Food_Can_Meat_4     .set(addItem(   aID, "Wide Food Can (Meat)"         , "", new FoodStat( 8, 0.4F, 20, C+37, 0.10F,  0,  0, 16,  0, 16, ItemUseAnimation.EAT, NI, F, F, F, T), TC.stack(TC.CORPUS, 2), TC.stack(TC.FABRICO, 1), Behavior_FeedCat.INSTANCE, Behavior_FeedDog.INSTANCE, "listAllmeatraw", "listAllmeatcooked")); CR.remove(last()); Sandwiches.INGREDIENTS.put(last(), (byte)29);});
		aItems.add(55, aID -> {IL.Food_Can_Meat_5     .set(addItem(   aID, "Large Food Can (Meat)"        , "", new FoodStat(10, 0.5F, 25, C+37, 0.10F,  0,  0, 20,  0, 20, ItemUseAnimation.EAT, NI, F, F, F, T), TC.stack(TC.CORPUS, 3), TC.stack(TC.FABRICO, 1), Behavior_FeedCat.INSTANCE, Behavior_FeedDog.INSTANCE, "listAllmeatraw", "listAllmeatcooked")); CR.remove(last()); Sandwiches.INGREDIENTS.put(last(), (byte)29);});
		aItems.add(56, aID -> {IL.Food_Can_Meat_6     .set(addItem(   aID, "Huge Food Can (Meat)"         , "", new FoodStat(12, 0.6F, 30, C+37, 0.10F,  0,  0, 24,  0, 24, ItemUseAnimation.EAT, NI, F, F, F, T), TC.stack(TC.CORPUS, 3), TC.stack(TC.FABRICO, 1), Behavior_FeedCat.INSTANCE, Behavior_FeedDog.INSTANCE, "listAllmeatraw", "listAllmeatcooked")); CR.remove(last()); Sandwiches.INGREDIENTS.put(last(), (byte)29);});
		
		aItems.add(61, aID -> {IL.Food_Can_Fish_1     .set(addItem(   aID, "Tiny Food Can (Fish)"         , "", new FoodStat( 2, 0.1F,  5, C+37, 0.10F,  0,  0,  2,  0,  2, ItemUseAnimation.EAT, NI, F, F, F, T), TC.stack(TC.CORPUS, 1), TC.stack(TC.FABRICO, 1), Behavior_FeedCat.INSTANCE, "listAllfishraw", "listAllfishcooked")); CR.remove(last()); Sandwiches.INGREDIENTS.put(last(), (byte)25);});
		aItems.add(62, aID -> {IL.Food_Can_Fish_2     .set(addItem(   aID, "Small Food Can (Fish)"        , "", new FoodStat( 4, 0.2F, 10, C+37, 0.10F,  0,  0,  4,  0,  4, ItemUseAnimation.EAT, NI, F, F, F, T), TC.stack(TC.CORPUS, 1), TC.stack(TC.FABRICO, 1), Behavior_FeedCat.INSTANCE, "listAllfishraw", "listAllfishcooked")); CR.remove(last()); Sandwiches.INGREDIENTS.put(last(), (byte)25);});
		aItems.add(63, aID -> {IL.Food_Can_Fish_3     .set(addItem(   aID, "Tall Food Can (Fish)"         , "", new FoodStat( 6, 0.3F, 15, C+37, 0.10F,  0,  0,  8,  0,  8, ItemUseAnimation.EAT, NI, F, F, F, T), TC.stack(TC.CORPUS, 2), TC.stack(TC.FABRICO, 1), Behavior_FeedCat.INSTANCE, "listAllfishraw", "listAllfishcooked")); CR.remove(last()); Sandwiches.INGREDIENTS.put(last(), (byte)25);});
		aItems.add(64, aID -> {IL.Food_Can_Fish_4     .set(addItem(   aID, "Wide Food Can (Fish)"         , "", new FoodStat( 8, 0.4F, 20, C+37, 0.10F,  0,  0, 10,  0, 10, ItemUseAnimation.EAT, NI, F, F, F, T), TC.stack(TC.CORPUS, 2), TC.stack(TC.FABRICO, 1), Behavior_FeedCat.INSTANCE, "listAllfishraw", "listAllfishcooked")); CR.remove(last()); Sandwiches.INGREDIENTS.put(last(), (byte)25);});
		aItems.add(65, aID -> {IL.Food_Can_Fish_5     .set(addItem(   aID, "Large Food Can (Fish)"        , "", new FoodStat(10, 0.5F, 25, C+37, 0.10F,  0,  0, 12,  0, 12, ItemUseAnimation.EAT, NI, F, F, F, T), TC.stack(TC.CORPUS, 3), TC.stack(TC.FABRICO, 1), Behavior_FeedCat.INSTANCE, "listAllfishraw", "listAllfishcooked")); CR.remove(last()); Sandwiches.INGREDIENTS.put(last(), (byte)25);});
		aItems.add(66, aID -> {IL.Food_Can_Fish_6     .set(addItem(   aID, "Huge Food Can (Fish)"         , "", new FoodStat(12, 0.6F, 30, C+37, 0.10F,  0,  0, 14,  0, 14, ItemUseAnimation.EAT, NI, F, F, F, T), TC.stack(TC.CORPUS, 3), TC.stack(TC.FABRICO, 1), Behavior_FeedCat.INSTANCE, "listAllfishraw", "listAllfishcooked")); CR.remove(last()); Sandwiches.INGREDIENTS.put(last(), (byte)25);});
		
		aItems.add(71, aID -> {IL.Food_Can_Chum_1     .set(addItem(   aID, "Tiny Food Can (Chum)"         , "", new FoodStat( 2, 0.1F,  5, C+37, 0.10F,  0,  0, 10,  0,  0, ItemUseAnimation.EAT, NI, F, F, T, T, /*hunger*/17, 1000, 4, 100, /*confusion*/9, 300, 1, 80), TC.stack(TC.CORPUS, 1), TC.stack(TC.FABRICO, 1), TD.Creative.HIDDEN)); CR.remove(last()); Sandwiches.INGREDIENTS.put(last(), (byte)39);});
		aItems.add(72, aID -> {IL.Food_Can_Chum_2     .set(addItem(   aID, "Small Food Can (Chum)"        , "", new FoodStat( 4, 0.2F, 10, C+37, 0.10F,  0,  0, 20,  0,  0, ItemUseAnimation.EAT, NI, F, F, T, T, /*hunger*/17, 1000, 4, 100, /*confusion*/9, 300, 1, 80), TC.stack(TC.CORPUS, 1), TC.stack(TC.FABRICO, 1), TD.Creative.HIDDEN)); CR.remove(last()); Sandwiches.INGREDIENTS.put(last(), (byte)39);});
		aItems.add(73, aID -> {IL.Food_Can_Chum_3     .set(addItem(   aID, "Tall Food Can (Chum)"         , "", new FoodStat( 6, 0.3F, 15, C+37, 0.10F,  0,  0, 30,  0,  0, ItemUseAnimation.EAT, NI, F, F, T, T, /*hunger*/17, 1000, 4, 100, /*confusion*/9, 300, 1, 80), TC.stack(TC.CORPUS, 2), TC.stack(TC.FABRICO, 1), TD.Creative.HIDDEN)); CR.remove(last()); Sandwiches.INGREDIENTS.put(last(), (byte)39);});
		aItems.add(74, aID -> {IL.Food_Can_Chum_4     .set(addItem(   aID, "Wide Food Can (Chum)"         , "", new FoodStat( 8, 0.4F, 20, C+37, 0.10F,  0,  0, 40,  0,  0, ItemUseAnimation.EAT, NI, F, F, T, T, /*hunger*/17, 1000, 4, 100, /*confusion*/9, 300, 1, 80), TC.stack(TC.CORPUS, 2), TC.stack(TC.FABRICO, 1), TD.Creative.HIDDEN)); CR.remove(last()); Sandwiches.INGREDIENTS.put(last(), (byte)39);});
		aItems.add(75, aID -> {IL.Food_Can_Chum_5     .set(addItem(   aID, "Large Food Can (Chum)"        , "", new FoodStat(10, 0.5F, 25, C+37, 0.10F,  0,  0, 50,  0,  0, ItemUseAnimation.EAT, NI, F, F, T, T, /*hunger*/17, 1000, 4, 100, /*confusion*/9, 300, 1, 80), TC.stack(TC.CORPUS, 3), TC.stack(TC.FABRICO, 1), TD.Creative.HIDDEN)); CR.remove(last()); Sandwiches.INGREDIENTS.put(last(), (byte)39);});
		aItems.add(76, aID -> {IL.Food_Can_Chum_6     .set(addItem(   aID, "Huge Food Can (Chum)"         , "", new FoodStat(12, 0.6F, 30, C+37, 0.10F,  0,  0, 60,  0,  0, ItemUseAnimation.EAT, NI, F, F, T, T, /*hunger*/17, 1000, 4, 100, /*confusion*/9, 300, 1, 80), TC.stack(TC.CORPUS, 3), TC.stack(TC.FABRICO, 1), TD.Creative.HIDDEN)); CR.remove(last()); Sandwiches.INGREDIENTS.put(last(), (byte)39);});
		
		aItems.add(81, aID -> {IL.Food_Can_Cookies_1  .set(addItem(   aID, "Tiny Food Can (Cookies)"      , "", new FoodStat( 2, 0.1F,  2, C+37, 0.10F,  0,  0,  0,  0,  0, ItemUseAnimation.EAT, NI, F, F, F, T), TC.stack(TC.FAMES, 1), TC.stack(TC.FABRICO, 1))); CR.remove(last());});
		aItems.add(82, aID -> {IL.Food_Can_Cookies_2  .set(addItem(   aID, "Small Food Can (Cookies)"     , "", new FoodStat( 4, 0.2F,  4, C+37, 0.10F,  0,  0,  0,  0,  0, ItemUseAnimation.EAT, NI, F, F, F, T), TC.stack(TC.FAMES, 1), TC.stack(TC.FABRICO, 1))); CR.remove(last());});
		aItems.add(83, aID -> {IL.Food_Can_Cookies_3  .set(addItem(   aID, "Tall Food Can (Cookies)"      , "", new FoodStat( 6, 0.3F,  6, C+37, 0.10F,  0,  0,  0,  0,  0, ItemUseAnimation.EAT, NI, F, F, F, T), TC.stack(TC.FAMES, 2), TC.stack(TC.FABRICO, 1))); CR.remove(last());});
		aItems.add(84, aID -> {IL.Food_Can_Cookies_4  .set(addItem(   aID, "Wide Food Can (Cookies)"      , "", new FoodStat( 8, 0.4F,  8, C+37, 0.10F,  0,  0,  0,  0,  0, ItemUseAnimation.EAT, NI, F, F, F, T), TC.stack(TC.FAMES, 2), TC.stack(TC.FABRICO, 1))); CR.remove(last());});
		aItems.add(85, aID -> {IL.Food_Can_Cookies_5  .set(addItem(   aID, "Large Food Can (Cookies)"     , "", new FoodStat(10, 0.5F, 10, C+37, 0.10F,  0,  0,  0,  0,  0, ItemUseAnimation.EAT, NI, F, F, F, T), TC.stack(TC.FAMES, 3), TC.stack(TC.FABRICO, 1))); CR.remove(last());});
		aItems.add(86, aID -> {IL.Food_Can_Cookies_6  .set(addItem(   aID, "Huge Food Can (Cookies)"      , "", new FoodStat(12, 0.6F, 12, C+37, 0.10F,  0,  0,  0,  0,  0, ItemUseAnimation.EAT, NI, F, F, F, T), TC.stack(TC.FAMES, 3), TC.stack(TC.FABRICO, 1))); CR.remove(last());});
		
		aItems.add(32764, aID -> {IL.Food_Can_Air_End    .set(addItem(aID, "Canned Space Air", "Naturally Venting, Air-Free Space Air (for sus Underwater)", new FoodStat( 0, 0.0F,  0, C+30, 0.20F,  0,  0,  0,  0,  0, ItemUseAnimation.DRINK, NI, T, F, F, T).setRebreathe(400), TC.stack(TC.AER, 4), TC.stack(TC.ALIENIS, 1), TC.stack(TC.FABRICO, 1))); CR.remove(last());});
		aItems.add(32765, aID -> {IL.Food_Can_Air_Nether .set(addItem(aID, "Canned Hot Air"  , "Naturally Smoldering, Water-Free Air (for use Underwater)" , new FoodStat( 0, 0.0F,  0, C+40, 0.50F,  0,  0,  0,  0,  0, ItemUseAnimation.DRINK, NI, T, F, F, T).setRebreathe(400), TC.stack(TC.AER, 4), TC.stack(TC.IGNIS  , 1), TC.stack(TC.FABRICO, 1))); CR.remove(last());});
		aItems.add(32766, aID -> {
			IL.Food_Can_Air        .set(addItem(aID, "Canned Air"      , "Naturally Sparkling, Salt-Free Air (for use Underwater)"   , new FoodStat( 0, 0.0F,  0, C+36, 0.20F,  0,  0,  0,  0,  0, ItemUseAnimation.DRINK, NI, T, F, F, T).setRebreathe(400), TC.stack(TC.AER, 4), TC.stack(TC.VOLATUS, 1), TC.stack(TC.FABRICO, 1))); CR.remove(last());
		
		for (String tAir : FluidsGT.AIR) if (!FL.Air_End.is(tAir) && !FL.Air_Nether.is(tAir))
		RM.Canner.addRecipe1(F, 16, 64, IL.Food_Can_Empty.get(1), FL     .make(tAir, 16000), NF, IL.Food_Can_Air       .get(1));
		RM.Canner.addRecipe1(F, 16, 64, IL.Food_Can_Empty.get(1), FL.Air_Nether.make(16000), NF, IL.Food_Can_Air_Nether.get(1));
		RM.Canner.addRecipe1(F, 16, 64, IL.Food_Can_Empty.get(1), FL.Air_End   .make(16000), NF, IL.Food_Can_Air_End   .get(1));
		
		RM.Canner.addRecipe1(F, 16, 16, IL.Food_Can_Air       .get(1), NF, FL.Air       .make(16000), IL.Food_Can_Empty.get(1));
		RM.Canner.addRecipe1(F, 16, 16, IL.Food_Can_Air_Nether.get(1), NF, FL.Air_Nether.make(16000), IL.Food_Can_Empty.get(1));
		RM.Canner.addRecipe1(F, 16, 16, IL.Food_Can_Air_End   .get(1), NF, FL.Air_End   .make(16000), IL.Food_Can_Empty.get(1));
		});
	}
	
	@Override
	public ItemStack getContainerItemDefault(ItemStack aStack) {
		return IL.Food_Can_Empty.get(1);
	}
	
	@Override
	public ItemStack getRotten(ItemStack aStack) {
		short tMeta = ST.meta_(aStack);
		return tMeta < 20 || tMeta >= 32000 ? aStack : ST.make(metaFamily(), aStack.getCount(), 10+(tMeta%10), ItemNBT.get(aStack));
	}
	
	@Override public ItemStack getRotten(ItemStack aStack, Level aWorld, int aX, int aY, int aZ) {return getRotten(aStack);}
}

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
 * the GregTech6-Unofficial entries of gregtech/items/MultiItemFood.java moved into an addon registry.
 */

package gt6u.items;


import static gregapi.data.CS.*;
import gregapi.data.*;
import gregapi.data.CS.BlocksGT;
import gregapi.data.CS.BushesGT;
import gregapi.data.CS.FoodsGT;
import gregapi.data.CS.ItemsGT;
import gregapi.data.CS.OreDictToolNames;
import gregapi.data.CS.PotionsGT;
import gregapi.data.CS.ToolsGT;
import gregapi.item.CreativeTab;
import gregapi.item.IItemRottable;
import gregapi.item.multiitem.MultiItemRandom;
import gregapi.item.multiitem.behaviors.Behavior_CureZombie;
import gregapi.item.multiitem.behaviors.Behavior_FeedChocolate;
import gregapi.item.multiitem.behaviors.Behavior_FeedDog;
import gregapi.item.multiitem.behaviors.Behavior_FeedGrass;
import gregapi.item.multiitem.behaviors.Behavior_FeedPig;
import gregapi.item.multiitem.behaviors.Behavior_Turn_Into;
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
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.ItemStack;

import net.minecraft.world.level.Level;
import net.minecraftforge.fluids.FluidStack;
import gt6u.GT6U_Recipes;

/** GregTech6-Unofficial's edits of gregtech/items/MultiItemFood.java, as statements: what GT6U added runs in run(), what it dropped is taken back in
 *  takeBack(): restated as GT6 makes them, their Recipes are taken out of GT6's maps again. */
public class MultiItemFood_Recipes {
	public static void run() {
		run_addItems();
	}
	
	public static void takeBack() {
		takeBack_addItems();
	}
	
	/** GT6U's additions to MultiItemFood.addItems (GT6U gregtech/items/MultiItemFood.java). */
	static void run_addItems() {
		// Banana (90) and Banana Slice (91): GT6U's first copies are overwritten by GT6's own later in the same method.
		int tLastID = 0;
		CR.shaped(IL.Food_Carrot_Sliced.get(4), CR.DEF_NCC, "kX", 'X', "cropCarrot");
		CR.shapeless(IL.Food_Sandwich_Steak.get(1) , CR.DEF, new Object[] {IL.Food_Breads_Sliced, ST.make(Items.COOKED_BEEF, 1, W)});
		CR.shapeless(IL.Food_Sandwich_Steak.get(1) , CR.DEF, new Object[] {IL.Food_Bread_Sliced, IL.Food_Bread_Sliced, ST.make(Items.COOKED_BEEF, 1, W)});
		FoodsGT.put(ST.make(Items.PUMPKIN_PIE , 1, W), 0, 0, 0,15, 0);
		IL.Food_Fries_Raw.set( ItemsGT.FOOD.addItem(tLastID = 8000, "Potato Strips" , "It's Potato in Stripe Form" , new FoodStat( 1, 1.200F, 0, C+37, 0.10F, 0, 0, 0, 5, 0, UseAnim.EAT, null , F, T, F, T), TC.stack(TC.FAMES, 1), TC.stack(TC.MESSIS, 1), TC.stack(TC.HERBA, 1)));
		ItemsGT.FOOD.setFluidContainerStats(tLastID, 0, 8);
		IL.Food_Fries.set( ItemsGT.FOOD.addItem(tLastID = 8010, "Fries" , "Not to confuse with Fry the Delivery Boy", "foodFries" , new FoodStat( 7, 1.200F, 0, C+38, 0.50F, 0, 0, 10, 10, 0, UseAnim.EAT, null , F, T, F, T), TC.stack(TC.FAMES, 1), TC.stack(TC.MESSIS, 1), TC.stack(TC.HERBA, 1), TC.stack(TC.IGNIS, 1)));
		ItemsGT.FOOD.setFluidContainerStats(tLastID, 0, 8);
		IL.Food_Fries_Packaged.set( ItemsGT.FOOD.addItem(tLastID = 8011, "Fries" , "Ketchup not included" , new FoodStat( 7, 1.200F, 0, C+38, 0.50F, 0, 0, 10, 10, 0, UseAnim.EAT, OP.scrapGt.mat(MT.Paper, 16) , F, T, F, T), TC.stack(TC.FAMES, 1), TC.stack(TC.MESSIS, 1), TC.stack(TC.HERBA, 1), TC.stack(TC.IGNIS, 1)));
		RM.Bath.addRecipe1(T, 0, 16, IL.Food_Fries_Raw.get(1), MT.FryingOilHot.liquid(U/100, T), NF, IL.Food_Fries.get(1));
		CR.shaped(IL.Food_Fries_Raw.get(1), CR.DEF_NAC_NCC, "k", "X", 'X', "cropPotato");
		CR.shapeless(IL.Food_Fries_Packaged.get(1), CR.DEF_NAC_NCC, new Object[] {OP.plateDouble.dat(MT.Paper), IL.Food_Fries});
		RM.Boxinator.addRecipe2(T, 16, 16, IL.Food_Fries.get(1), OP.plateDouble.mat(MT.Paper, 1), IL.Food_Fries_Packaged.get(1));
		RM.Unboxinator.addRecipe1(T, 16, 16, IL.Food_Fries_Packaged.get(1), IL.Food_Fries.get(1), OP.scrapGt.mat(MT.Paper, 16));
		IL.Food_PotatoChips_Raw.set( ItemsGT.FOOD.addItem(tLastID = 9000, "Potato Chips (Raw)" , "Just like a Potato" , new FoodStat( 1, 1.200F, 0, C+37, 0.10F, 0, 0, 0, 0, 0, UseAnim.EAT, null , F, T, F, T), TC.stack(TC.FAMES, 1), TC.stack(TC.MESSIS, 1), TC.stack(TC.HERBA, 1)));
		ItemsGT.FOOD.setFluidContainerStats(tLastID, 0, 8);
		IL.Food_PotatoChips.set( ItemsGT.FOOD.addItem(tLastID = 9010, "Potato Chips" , "Crunchy" , new FoodStat( 7, 1.200F, 0, C+37, 0.10F, 0, 0, 10, 10, 0, UseAnim.EAT, null , F, T, F, T), TC.stack(TC.FAMES, 1), TC.stack(TC.MESSIS, 1), TC.stack(TC.HERBA, 1), TC.stack(TC.IGNIS, 1)));
		ItemsGT.FOOD.setFluidContainerStats(tLastID, 0, 8);
		IL.Food_PotatoChips_Packaged.set( ItemsGT.FOOD.addItem(tLastID = 9011, "Bag of Potato Chips" , "Full of delicious Air" , new FoodStat( 7, 1.200F, 0, C+37, 0.10F, 0, 0, 10, 10, 0, UseAnim.EAT, OP.scrapGt.mat(MT.Al, 2) , F, T, F, T), TC.stack(TC.FAMES, 1), TC.stack(TC.MESSIS, 1), TC.stack(TC.HERBA, 1), TC.stack(TC.IGNIS, 1)));
		RM.add_smelting(IL.Food_PotatoChips_Raw.get(1), IL.Food_PotatoChips.get(1));
		CR.shaped(IL.Food_PotatoChips_Raw.get(1), CR.DEF_NAC_NCC, "kX", 'X', "cropPotato");
		CR.shapeless(IL.Food_PotatoChips_Packaged.get(1), CR.DEF_NAC_NCC, new Object[] {OP.foil.dat(MT.Al), IL.Food_PotatoChips});
		RM.Boxinator.addRecipe2(T, 16, 16, IL.Food_PotatoChips.get(1), OP.foil.mat(MT.Al, 1), IL.Food_PotatoChips_Packaged.get(1));
		RM.Unboxinator.addRecipe1(T, 16, 16, IL.Food_PotatoChips_Packaged.get(1), IL.Food_PotatoChips.get(1), OP.scrapGt.mat(MT.Al, 2));
		IL.Food_ChiliChips.set( ItemsGT.FOOD.addItem(tLastID = 9020, "Chili Chips" , "Spicy" , new FoodStat( 7, 1.200F, -10, C+40, 0.30F, 0, 0, 30, 10, 0, UseAnim.EAT, null , F, T, F, T), TC.stack(TC.FAMES, 1), TC.stack(TC.MESSIS, 1), TC.stack(TC.HERBA, 1), TC.stack(TC.IGNIS, 1)));
		ItemsGT.FOOD.setFluidContainerStats(tLastID, 0, 8);
		IL.Food_ChiliChips_Packaged.set( ItemsGT.FOOD.addItem(tLastID = 9021, "Bag of Chili Chips" , "Stop making noises Baj!" , new FoodStat( 7, 1.200F, -10, C+40, 0.30F, 0, 0, 30, 10, 0, UseAnim.EAT, OP.scrapGt.mat(MT.Al, 2) , F, T, F, T), TC.stack(TC.FAMES, 1), TC.stack(TC.MESSIS, 1), TC.stack(TC.HERBA, 1), TC.stack(TC.IGNIS, 1)));
		CR.shapeless(IL.Food_ChiliChips_Packaged.get(1), CR.DEF_NAC_NCC, new Object[] {OP.foil.dat(MT.Al), IL.Food_ChiliChips});
		RM.Boxinator.addRecipe2(T, 16, 16, IL.Food_ChiliChips.get(1), OP.foil.mat(MT.Al, 1), IL.Food_ChiliChips_Packaged.get(1));
		RM.Unboxinator.addRecipe1(T, 16, 16, IL.Food_ChiliChips_Packaged.get(1), IL.Food_ChiliChips.get(1), OP.scrapGt.mat(MT.Al, 2));
	}

	/** GT6's statements in MultiItemFood.addItems that GT6U dropped, as the port has them: their Recipes are taken back. */
	static void takeBack_addItems() {
		GT6U_Recipes.takeBack("MultiItemFood_Recipes.addItems", () -> {
			RM.packunpack(IL.Food_Bun_Sliced.get(2), IL.Food_Buns_Sliced.get(1));
			RM.packunpack(IL.Food_Bread_Sliced.get(2), IL.Food_Breads_Sliced.get(1));
			CR.shapeless(IL.Food_Sandwich_Steak.get(1) , CR.DEF, new Object[] {IL.Food_Breads_Sliced, Items.COOKED_BEEF});
			CR.shapeless(IL.Food_Sandwich_Steak.get(1) , CR.DEF, new Object[] {IL.Food_Bread_Sliced, IL.Food_Bread_Sliced, Items.COOKED_BEEF});
			RM.packunpack(IL.Food_Baguette_Sliced.get(2), IL.Food_Baguette_Sliced.get(1));
		});
	}
}

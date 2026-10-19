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
 * the GregTech6-Unofficial entries of gregtech/loaders/c/Loader_Recipes_Vanilla_OreDict.java moved into an addon registry.
 */

package gt6u.loaders;

import static gregapi.data.CS.*;
import static gregapi.data.OP.*;
import gregapi.block.metatype.BlockStones;
import gregapi.code.ItemStackContainer;
import gregapi.config.ConfigCategories;
import gregapi.data.CS.BlocksGT;
import gregapi.data.CS.BooksGT;
import gregapi.data.CS.ConfigsGT;
import gregapi.data.FL;
import gregapi.data.IL;
import gregapi.data.MT;
import gregapi.data.OD;
import gregapi.data.OP;
import gregapi.data.RM;
import gregapi.oredict.OreDictMaterial;
import gregapi.oredict.event.IOreDictListenerEvent;
import gregapi.oredict.event.OreDictListenerEvent_Names;
import gregapi.util.CR;
import gregapi.util.OM;
import gregapi.util.ST;
import gregapi.wooddict.WoodDictionary;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import gt6u.GT6U_Recipes;

/** GregTech6-Unofficial's edits of gregtech/loaders/c/Loader_Recipes_Vanilla_OreDict.java, as statements: what GT6U added runs in run(), what it dropped is taken back in
 *  takeBack(): restated as GT6 makes them, their Recipes are taken out of GT6's maps again. */
/** GT6 6.17 keeps these listeners in another loader; a listener of its own carries GT6U's and restates GT6's for taking back. */
public class Loader_Recipes_Vanilla_OreDict {
	public static void run() {
		run_addAllListeners();
	}
	
	public static void takeBack() {
		takeBack_addAllListeners();
	}
	
	/** GT6U's additions to Loader_Recipes_Vanilla_OreDict.addAllListeners (GT6U gregtech/loaders/c/Loader_Recipes_Vanilla_OreDict.java). */
	static void run_addAllListeners() {
		new OreDictListenerEvent_Names() {@Override public void addAllListeners() {
			addListener(OD.plankSkyroot, OD.plankWeedwood, OD.plankWood, new IOreDictListenerEvent(){@Override public void onOreRegistration(OreDictRegistrationContainer aEvent){RM.generify(aEvent.mStack, IL.Plank.get(1));RM.Welder.addRecipe2(T, 16, 32, ST.amount(8, aEvent.mStack), dust.mat(MT.Redstone, 1), ST.make(Blocks.NOTE_BLOCK, 1, 0));RM.Welder.addRecipe2(T, 16, 64, ST.amount(8, aEvent.mStack), gem.mat(MT.Diamond, 1), ST.make(Blocks.JUKEBOX, 1, 0));RM.Welder.addRecipe2(T, 16, 16, aEvent.mStack, ST.tag(1), ST.make(Blocks.OAK_BUTTON, 1, 0));RM.Welder.addRecipe2(T, 16, 32, ST.amount(2, aEvent.mStack), ST.tag(2), ST.make(Blocks.OAK_PRESSURE_PLATE, 1, 0));RM.Welder.addRecipe2(T, 16, 48, ST.amount(3, aEvent.mStack), ST.tag(3), ST.make(Blocks.OAK_TRAPDOOR, 1, 0));RM.Welder.addRecipe2(T, 16, 64, ST.amount(4, aEvent.mStack), ST.tag(4), ST.make(Blocks.CRAFTING_TABLE, 1, 0));RM.Welder.addRecipe2(T, 16, 96, ST.amount(2, aEvent.mStack), ST.tag(6), ST.make(Items.OAK_DOOR, 1, 0));RM.Welder.addRecipe2(T, 16, 128, ST.amount(8, aEvent.mStack), ST.tag(8), ST.make(Blocks.CHEST, 1, 0));RM.Welder.addRecipe2(T, 16, 64, ST.amount(6, aEvent.mStack), ST.make(Items.BOOK, 3, 0), ST.make(Blocks.BOOKSHELF, 1, 0));if(WoodDictionary.IGNORED_OREDICT_REGISTRATIONS.contains(ST.item_(aEvent.mStack)))return;if(IL.MaCu_Polished_Planks.exists()){RM.Bath.addRecipe1(T, 0, 144, aEvent.mStack, FL.Oil_Fish.make(1000), NF, IL.MaCu_Polished_Planks.get(1));RM.Bath.addRecipe1(T, 0, 144, aEvent.mStack, FL.Oil_Whale.make(500), NF, IL.MaCu_Polished_Planks.get(1));}ItemStack tTreated=IL.IE_Treated_Planks.get(1, IL.Treated_Planks.get(1));RM.Bath.addRecipe1(T, 0, 144, aEvent.mStack, FL.Oil_Seed.make(100), NF, tTreated);RM.Bath.addRecipe1(T, 0, 144, aEvent.mStack, FL.Oil_Lin.make(100), NF, tTreated);RM.Bath.addRecipe1(T, 0, 144, aEvent.mStack, FL.Oil_Hemp.make(100), NF, tTreated);RM.Bath.addRecipe1(T, 0, 144, aEvent.mStack, FL.Oil_Nut.make(100), NF, tTreated);RM.Bath.addRecipe1(T, 0, 144, aEvent.mStack, FL.Oil_Olive.make(100), NF, tTreated);RM.Bath.addRecipe1(T, 0, 144, aEvent.mStack, FL.Oil_Sunflower.make(100), NF, tTreated);RM.Bath.addRecipe1(T, 0, 144, aEvent.mStack, FL.Oil_Creosote.make(100), NF, tTreated);}});
		}};
	}

	/** GT6's statements in Loader_Recipes_Vanilla_OreDict.addAllListeners that GT6U dropped, as the port has them: their Recipes are taken back. */
	static void takeBack_addAllListeners() {
		GT6U_Recipes.takeBack("Loader_Recipes_Vanilla_OreDict.addAllListeners", () -> {
			new OreDictListenerEvent_Names() {@Override public void addAllListeners() {
				addListener(OD.plankSkyroot, OD.plankWeedwood, OD.plankWood, new IOreDictListenerEvent(){@Override public void onOreRegistration(OreDictRegistrationContainer aEvent){RM.generify(aEvent.mStack, IL.Plank.get(1));RM.Assembler.addRecipe2(T, 16, 32, ST.amount(8, aEvent.mStack), dust.mat(MT.Redstone, 1), ST.make(Blocks.NOTE_BLOCK, 1, 0));RM.Assembler.addRecipe2(T, 16, 64, ST.amount(8, aEvent.mStack), gem.mat(MT.Diamond, 1), ST.make(Blocks.JUKEBOX, 1, 0));RM.Assembler.addRecipe2(T, 16, 16, aEvent.mStack, ST.tag(1), ST.make(Blocks.OAK_BUTTON, 1, 0));RM.Assembler.addRecipe2(T, 16, 32, ST.amount(2, aEvent.mStack), ST.tag(2), ST.make(Blocks.OAK_PRESSURE_PLATE, 1, 0));RM.Assembler.addRecipe2(T, 16, 48, ST.amount(3, aEvent.mStack), ST.tag(3), ST.make(Blocks.OAK_TRAPDOOR, 1, 0));RM.Assembler.addRecipe2(T, 16, 64, ST.amount(4, aEvent.mStack), ST.tag(4), ST.make(Blocks.CRAFTING_TABLE, 1, 0));RM.Assembler.addRecipe2(T, 16, 96, ST.amount(2, aEvent.mStack), ST.tag(6), ST.make(Items.OAK_DOOR, 1, 0));RM.Assembler.addRecipe2(T, 16, 128, ST.amount(8, aEvent.mStack), ST.tag(8), ST.make(Blocks.CHEST, 1, 0));RM.Assembler.addRecipe2(T, 16, 64, ST.amount(6, aEvent.mStack), ST.make(Items.BOOK, 3, 0), ST.make(Blocks.BOOKSHELF, 1, 0));if(WoodDictionary.IGNORED_OREDICT_REGISTRATIONS.contains(ST.item_(aEvent.mStack)))return;if(IL.MaCu_Polished_Planks.exists()){RM.Bath.addRecipe1(T, 0, 144, aEvent.mStack, FL.Oil_Fish.make(1000), NF, IL.MaCu_Polished_Planks.get(1));RM.Bath.addRecipe1(T, 0, 144, aEvent.mStack, FL.Oil_Whale.make(500), NF, IL.MaCu_Polished_Planks.get(1));}ItemStack tTreated=IL.IE_Treated_Planks.get(1, IL.Treated_Planks.get(1));RM.Bath.addRecipe1(T, 0, 144, aEvent.mStack, FL.Oil_Seed.make(100), NF, tTreated);RM.Bath.addRecipe1(T, 0, 144, aEvent.mStack, FL.Oil_Lin.make(100), NF, tTreated);RM.Bath.addRecipe1(T, 0, 144, aEvent.mStack, FL.Oil_Hemp.make(100), NF, tTreated);RM.Bath.addRecipe1(T, 0, 144, aEvent.mStack, FL.Oil_Nut.make(100), NF, tTreated);RM.Bath.addRecipe1(T, 0, 144, aEvent.mStack, FL.Oil_Olive.make(100), NF, tTreated);RM.Bath.addRecipe1(T, 0, 144, aEvent.mStack, FL.Oil_Sunflower.make(100), NF, tTreated);RM.Bath.addRecipe1(T, 0, 144, aEvent.mStack, FL.Oil_Creosote.make(100), NF, tTreated);}});
			}};
		});
	}
}

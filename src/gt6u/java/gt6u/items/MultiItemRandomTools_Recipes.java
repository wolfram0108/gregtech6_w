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
 * the GregTech6-Unofficial entries of gregtech/items/MultiItemRandomTools.java moved into an addon registry.
 */

package gt6u.items;


import static gregapi.data.CS.*;
import static gregapi.data.OP.*;
import gregapi.GT_API;
import gregapi.code.IItemContainer;
import gregapi.cover.covers.CoverTextureCanvas;
import gregapi.data.*;
import gregapi.data.CS.BlocksGT;
import gregapi.data.CS.BooksGT;
import gregapi.data.CS.GarbageGT;
import gregapi.data.CS.ItemsGT;
import gregapi.data.CS.OreDictToolNames;
import gregapi.data.CS.ToolsGT;
import gregapi.item.CreativeTab;
import gregapi.item.IItemRottable;
import gregapi.item.multiitem.MultiItem;
import gregapi.item.multiitem.MultiItemRandom;
import gregapi.item.multiitem.behaviors.Behavior_Bucket_Simple;
import gregapi.item.multiitem.behaviors.Behavior_PrintedPages;
import gregapi.item.multiitem.behaviors.Behavior_Switch_Metadata;
import gregapi.item.multiitem.behaviors.Behavior_Tool;
import gregapi.item.multiitem.behaviors.IBehavior;
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
import net.minecraft.world.level.Level;
import net.minecraftforge.fluids.FluidStack;
import gt6u.GT6U_Recipes;

/** GregTech6-Unofficial's edits of gregtech/items/MultiItemRandomTools.java, as statements: what GT6U added runs in run(), what it dropped is taken back in
 *  takeBack(): restated as GT6 makes them, their Recipes are taken out of GT6's maps again. */
public class MultiItemRandomTools_Recipes {
	public static void run() {
		run_addItems();
	}
	
	public static void takeBack() {
		takeBack_addItems();
	}
	
	/** GT6U's additions to MultiItemRandomTools.addItems (GT6U gregtech/items/MultiItemRandomTools.java). */
	static void run_addItems() {
		for (OreDictMaterial tWood : ANY.Wood.mToThis) {
			RM.Welder.addRecipe2(T, 16, 16, OP.bolt.mat(tWood, 1), OP.dustSmall.mat(MT.P , 1), IL.Tool_Matches.get(1));
			RM.Welder.addRecipe2(T, 16, 16, OP.bolt.mat(tWood, 1), OP.dustSmall.mat(MT.Phosphorus, 1), IL.Tool_Matches.get(1));
			RM.Welder.addRecipe2(T, 16, 64, OP.bolt.mat(tWood, 4), OP.dust .mat(MT.P , 1), IL.Tool_Matches.get(4));
			RM.Welder.addRecipe2(T, 16, 64, OP.bolt.mat(tWood, 4), OP.dust.mat(MT.Phosphorus, 1), IL.Tool_Matches.get(4));
		}
	}

	/** GT6's statements in MultiItemRandomTools.addItems that GT6U dropped, as the port has them: their Recipes are taken back. */
	static void takeBack_addItems() {
		GT6U_Recipes.takeBack("MultiItemRandomTools_Recipes.addItems", () -> {
			for (OreDictMaterial tWood : ANY.Wood.mToThis) {
				RM.Assembler.addRecipe2(T, 16, 16, OP.bolt.mat(tWood, 1), OP.dustSmall.mat(MT.P , 1), IL.Tool_Matches.get(1));
				for (OreDictMaterial tPhosphorus : ANY.Phosphorus.mToThis) {
					RM.Assembler.addRecipe2(T, 16, 16, OP.bolt.mat(tWood, 1), OP.dustSmall.mat(tPhosphorus, 1), IL.Tool_Matches.get(1));
				}
				RM.Assembler.addRecipe2(T, 16, 64, OP.bolt.mat(tWood, 4), OP.dust .mat(MT.P , 1), IL.Tool_Matches.get(4));
				for (OreDictMaterial tPhosphorus : ANY.Phosphorus.mToThis) {
					RM.Assembler.addRecipe2(T, 16, 64, OP.bolt.mat(tWood, 4), OP.dust .mat(tPhosphorus, 1), IL.Tool_Matches.get(4));
				}
			}
		});
	}
}

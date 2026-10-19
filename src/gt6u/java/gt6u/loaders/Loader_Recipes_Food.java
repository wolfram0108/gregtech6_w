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
 * the GregTech6-Unofficial entries of gregtech/loaders/c/Loader_Recipes_Food.java moved into an addon registry.
 */

package gt6u.loaders;


import gt6u.data.FL_GT6U;

import static gregapi.data.CS.*;
import static gregapi.data.OP.*;
import gregapi.data.ANY;
import gregapi.data.CS.FluidsGT;
import gregapi.data.CS.FoodsGT;
import gregapi.data.FL;
import gregapi.data.IL;
import gregapi.data.MT;
import gregapi.data.OD;
import gregapi.data.OP;
import gregapi.data.RM;
import gregapi.data.TD;
import gregapi.item.multiitem.MultiItemRandom;
import gregapi.oredict.OreDictItemData;
import gregapi.oredict.OreDictManager;
import gregapi.oredict.OreDictMaterial;
import gregapi.oredict.OreDictMaterialStack;
import gregapi.oredict.OreDictPrefix;
import gregapi.oredict.event.IOreDictListenerEvent;
import gregapi.oredict.event.OreDictListenerEvent_Names;
import gregapi.oredict.event.OreDictListenerEvent_TwoNames;
import gregapi.util.CR;
import gregapi.util.OM;
import gregapi.util.ST;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;
import gt6u.GT6U_Recipes;

/** GregTech6-Unofficial's edits of gregtech/loaders/c/Loader_Recipes_Food.java, as statements: what GT6U added runs in run(), what it dropped is taken back in
 *  takeBack(): restated as GT6 makes them, their Recipes are taken out of GT6's maps again. */
public class Loader_Recipes_Food {
	public static void run() {
		run_run();
	}
	
	public static void takeBack() {
		takeBack_run();
	}
	
	/** GT6U's additions to Loader_Recipes_Food.run (GT6U gregtech/loaders/c/Loader_Recipes_Food.java). */
	static void run_run() {
		for (FluidStack tWater : FL.array(FL.Water.make(1000), FL.SpDew.make(1000), FL.DistW.make(1000))) {
			GT6U_Recipes.first(Loader_Recipes_Food::later_run_0); for (OreDictMaterial tMat : ANY.Flour.mToThis) RM.Mixer .addRecipe1(T, 16, 16, OM.dust(tMat), FL.mul(tWater, 1, 4, T), FL_GT6U.AmylumSolution.make(1000), IL.Food_Dough.get(2)); GT6U_Recipes.first(null);
		}
		RM.Centrifuge.addRecipe1(T, 16, 64, IL.Food_Coconut.get(1), ZL_FS, FL_GT6U.CoconutOil.make(100), ZL_IS);
	}

	/** GT6 statements GT6U put a statement of run_run ahead of: what they make gives way to it. */
	static void later_run_0() {
		for (OreDictMaterial tMat : ANY.FlourGrains.mToThis) {
			RM.Mixer .addRecipe2(T, 16, 16, OM.dust(tMat), OM.dust(MT.MeatRaw), OM.ingot(MT.MeatRaw));
			RM.Mixer .addRecipe2(T, 16, 16, OM.dust(tMat), OM.dust(MT.FishRaw), OM.ingot(MT.FishRaw));
			RM.Mixer .addRecipe2(T, 16, 16, OM.dust(tMat), OM.dust(MT.MeatRotten), OM.ingot(MT.MeatRotten));
			RM.Mixer .addRecipe2(T, 16, 16, OM.dust(tMat), OM.dust(MT.FishRotten), OM.ingot(MT.FishRotten));
		}
	}

	/** GT6's statements in Loader_Recipes_Food.run that GT6U dropped, as the port has them: their Recipes are taken back. */
	static void takeBack_run() {
		GT6U_Recipes.takeBack("Loader_Recipes_Food.run", () -> {
			for (FluidStack tWater : FL.array(FL.Water.make(1000), FL.SpDew.make(1000), FL.DistW.make(1000))) {
				for (OreDictMaterial tMat : ANY.Flour.mToThis) RM.Mixer .addRecipe1(T, 16, 16, OM.dust(tMat ), FL.mul(tWater, 1, 4, T), NF, IL.Food_Dough.get(2));
			}
		});
	}
}

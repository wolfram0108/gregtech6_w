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
 * the GregTech6-Unofficial entries of gregtech/loaders/c/Loader_Recipes_Handlers.java moved into an addon registry.
 */

package gt6u.loaders;


import gt6u.data.RM_GT6U;
import gt6u.data.OP_GT6U;

import static gregapi.data.CS.*;
import static gregapi.data.OP.*;
import static gt6u.data.OP_GT6U.*;
import static gregapi.data.TD.Atomic.*;
import static gregapi.data.TD.Compounds.*;
import static gregapi.data.TD.Prefix.*;
import static gregapi.data.TD.Processing.*;
import static gregapi.data.TD.Properties.*;
import static gregapi.oredict.OreDictMaterialCondition.*;

import gregapi.code.ICondition;
import gregapi.code.ICondition.And;
import gregapi.code.ICondition.Nor;
import gregapi.code.ICondition.Or;
import gregapi.data.ANY;
import gregapi.data.FL;
import gregapi.data.IL;
import gregapi.data.MT;
import gregapi.data.OP;
import gregapi.data.RM;
import gregapi.data.TD;
import gregapi.oredict.OreDictManager;
import gregapi.oredict.OreDictMaterial;
import gregapi.oredict.OreDictPrefix;
import gregapi.recipes.AdvancedCrafting1ToY;
import gregapi.recipes.AdvancedCraftingXToY;
import gregapi.recipes.handlers.RecipeMapHandlerCrushing;
import gregapi.recipes.handlers.RecipeMapHandlerMaterial;
import gregapi.recipes.handlers.RecipeMapHandlerPrefix;
import gregapi.recipes.handlers.RecipeMapHandlerPrefixForging;
import gregapi.recipes.handlers.RecipeMapHandlerPrefixShredding;
import gregapi.util.ST;
import gregapi.util.UT;
import gregtech.loaders.b.Loader_OreProcessing.OreProcessing_CraftFrom;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;
import gt6u.GT6U_Recipes;

/** GregTech6-Unofficial's edits of gregtech/loaders/c/Loader_Recipes_Handlers.java, as statements: what GT6U added runs in run(), what it dropped is taken back in
 *  takeBack(): restated as GT6 makes them, their Recipes are taken out of GT6's maps again. */
public class Loader_Recipes_Handlers {
	public static void run() {
		run_run();
	}
	
	public static void takeBack() {
		takeBack_run();
	}
	
	/** GT6U's additions to Loader_Recipes_Handlers.run (GT6U gregtech/loaders/c/Loader_Recipes_Handlers.java). */
	static void run_run() {
		RM.Wiremill.add(new RecipeMapHandlerPrefix(wireFine, 4, NF, 16, 0, 196, NF, wireLPGt01, 1, NI, NI, T, F, F, new And(ANTIMATTER.NOT, COATED.NOT, SMITHABLE)));
		RM.RollBender .add(new RecipeMapHandlerPrefix(wireFine , 2, NF, 16, 0, 256, NF, springSmall , 1, NI, NI, T, F, F, new And(ANTIMATTER.NOT, SMITHABLE)));
		RM.Loom .add(new RecipeMapHandlerPrefix(wireLPGt01 , 2, NF, 16, 0, 64, NF, wireLPGt02 , 1, ST.tag( 2), NI, F, F, F, ANTIMATTER.NOT));
		RM.Loom .add(new RecipeMapHandlerPrefix(wireLPGt01 , 4, NF, 16, 0, 64, NF, wireLPGt04 , 1, ST.tag( 4), NI, F, F, F, ANTIMATTER.NOT));
		RM.Loom .add(new RecipeMapHandlerPrefix(wireLPGt01 , 8, NF, 16, 0, 64, NF, wireLPGt08 , 1, ST.tag( 8), NI, F, F, F, ANTIMATTER.NOT));
		RM.Loom .add(new RecipeMapHandlerPrefix(wireLPGt01 ,12, NF, 16, 0, 64, NF, wireLPGt12 , 1, ST.tag(12), NI, F, F, F, ANTIMATTER.NOT));
		RM.Loom .add(new RecipeMapHandlerPrefix(wireLPGt01 ,16, NF, 16, 0, 64, NF, wireLPGt16 , 1, ST.tag(16), NI, F, F, F, ANTIMATTER.NOT));
		RM.Loom .add(new RecipeMapHandlerPrefix(wireLPGt02 , 2, NF, 16, 0, 64, NF, wireLPGt04 , 1, ST.tag( 4), NI, F, F, F, ANTIMATTER.NOT));
		RM.Loom .add(new RecipeMapHandlerPrefix(wireLPGt02 , 4, NF, 16, 0, 64, NF, wireLPGt08 , 1, ST.tag( 8), NI, F, F, F, ANTIMATTER.NOT));
		RM.Loom .add(new RecipeMapHandlerPrefix(wireLPGt02 , 6, NF, 16, 0, 64, NF, wireLPGt12 , 1, ST.tag(12), NI, F, F, F, ANTIMATTER.NOT));
		RM.Loom .add(new RecipeMapHandlerPrefix(wireLPGt02 , 8, NF, 16, 0, 64, NF, wireLPGt16 , 1, ST.tag(16), NI, F, F, F, ANTIMATTER.NOT));
		RM.Loom .add(new RecipeMapHandlerPrefix(wireLPGt04 , 2, NF, 16, 0, 64, NF, wireLPGt08 , 1, ST.tag( 8), NI, F, F, F, ANTIMATTER.NOT));
		RM.Loom .add(new RecipeMapHandlerPrefix(wireLPGt04 , 3, NF, 16, 0, 64, NF, wireLPGt12 , 1, ST.tag(12), NI, F, F, F, ANTIMATTER.NOT));
		RM.Loom .add(new RecipeMapHandlerPrefix(wireLPGt04 , 4, NF, 16, 0, 64, NF, wireLPGt16 , 1, ST.tag(16), NI, F, F, F, ANTIMATTER.NOT));
		RM.Loom .add(new RecipeMapHandlerPrefix(wireLPGt08 , 2, NF, 16, 0, 64, NF, wireLPGt16 , 1, ST.tag(16), NI, F, F, F, ANTIMATTER.NOT));
		FluidStack[] tFluids = FL.array(FL.Water.make(1000), FL.SpDew.make(1000), FL.DistW.make(1000), FL.Lubricant.make(1000), FL.LubRoCant.make(1000));
		long[] tMultiplier = new long[] {4, 4, 3, 1, 1};
		for (int i = 0; i < 4; i++) if (tFluids[i] != null) {
			RM_GT6U.LaserCutter .add(new RecipeMapHandlerPrefix(blockSolid , 1, NF, 16,tMultiplier[i] * 7 * 32, 0, NF, plate , 9, NI, NI, F, F, F, ANTIMATTER.NOT));
			RM_GT6U.LaserCutter .add(new RecipeMapHandlerPrefix(stickLong , 1, NF, 16,tMultiplier[i] * 32, 0, NF, stick , 2, NI, NI, F, F, F, ANTIMATTER.NOT));
			RM_GT6U.LaserCutter .add(new RecipeMapHandlerPrefix(stick , 1, NF, 16,tMultiplier[i] * 3 * 32, 0, NF, bolt , 4, NI, NI, F, F, F, ANTIMATTER.NOT));
			RM_GT6U.LaserCutter .add(new RecipeMapHandlerPrefix(plate , 1, NF, 16,tMultiplier[i] * 4 * 32, 0, NF, plateTiny , 9, NI, NI, F, F, F, ANTIMATTER.NOT));
			RM_GT6U.LaserCutter .add(new RecipeMapHandlerPrefix(plateGem , 1, NF, 16,tMultiplier[i] * 4 * 32, 0, NF, plateGemTiny , 9, NI, NI, F, F, F, ANTIMATTER.NOT));
			RM_GT6U.LaserCutter .add(new RecipeMapHandlerPrefix(gemChipped , 1, NF, 16,tMultiplier[i] * 32, 0, NF, plateGemTiny , 2, NI, NI, F, F, F, ANTIMATTER.NOT));
			RM_GT6U.LaserCutter .add(new RecipeMapHandlerPrefix(gemFlawed , 1, NF, 16,tMultiplier[i] * 2 * 32, 0, NF, plateGemTiny , 4, NI, NI, F, F, F, ANTIMATTER.NOT));
			RM_GT6U.LaserCutter .add(new RecipeMapHandlerPrefix(gem , 1, NF, 48,tMultiplier[i] * 32, 0, NF, plateGem , 1, NI, NI, F, F, F, ANTIMATTER.NOT));
			RM_GT6U.LaserCutter .add(new RecipeMapHandlerPrefix(gemFlawless , 1, NF, 48,tMultiplier[i] * 32, 0, NF, plateGem , 2, NI, NI, F, F, F, ANTIMATTER.NOT));
			RM_GT6U.LaserCutter .add(new RecipeMapHandlerPrefix(gemExquisite , 1, NF, 48,tMultiplier[i] * 3 * 32, 0, NF, plateGem , 4, NI, NI, F, F, F, ANTIMATTER.NOT));
			RM_GT6U.LaserCutter .add(new RecipeMapHandlerPrefix(gemLegendary , 1, NF, 48,tMultiplier[i] * 7 * 32, 0, NF, plateGem , 8, NI, NI, F, F, F, ANTIMATTER.NOT));
			RM_GT6U.LaserCutter .add(new RecipeMapHandlerPrefix(bouleGt , 1, NF, 16,tMultiplier[i] * 3 * 32, 0, NF, plateGem , 6, NI, NI, F, F, F, ANTIMATTER.NOT));
			RM_GT6U.LaserCutter .add(new RecipeMapHandlerPrefix(ingotDouble , 1, NF, 16,tMultiplier[i] * 32, 0, NF, ingot , 2, NI, NI, F, F, F, ANTIMATTER.NOT));
			RM_GT6U.LaserCutter .add(new RecipeMapHandlerPrefix(ingotTriple , 1, NF, 16,tMultiplier[i] * 2 * 32, 0, NF, ingot , 3, NI, NI, F, F, F, ANTIMATTER.NOT));
			RM_GT6U.LaserCutter .add(new RecipeMapHandlerPrefix(ingotQuadruple , 1, NF, 16,tMultiplier[i] * 3 * 32, 0, NF, ingot , 4, NI, NI, F, F, F, ANTIMATTER.NOT));
			RM_GT6U.LaserCutter .add(new RecipeMapHandlerPrefix(ingotQuintuple , 1, NF, 16,tMultiplier[i] * 4 * 32, 0, NF, ingot , 5, NI, NI, F, F, F, ANTIMATTER.NOT));
		}
	}

	/** GT6's statements in Loader_Recipes_Handlers.run that GT6U dropped, as the port has them: their Recipes are taken back. */
	static void takeBack_run() {
		GT6U_Recipes.takeBack("Loader_Recipes_Handlers.run", () -> {
			Or tEasyWorkable = new Or<>(FURNACE, SOFT), tEasyHeatable = new Or<>(FURNACE);
			RM.RollBender .add(new RecipeMapHandlerPrefix(plate , 1, NF, 16, 0, 256, NF, plateCurved , 1, NI, NI, T, F, F, new And(ANTIMATTER.NOT, SMITHABLE, tEasyWorkable.NOT)));
			RM.RollBender .add(new RecipeMapHandlerPrefix(plate , 1, NF, 16, 16 , 0, NF, plateCurved , 1, NI, NI, T, F, F, new And(ANTIMATTER.NOT, SMITHABLE, tEasyWorkable)));
		});
	}
}

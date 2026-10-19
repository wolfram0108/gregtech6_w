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
 * the GregTech6-Unofficial entries of gregtech/loaders/a/Loader_Tools.java moved into an addon registry.
 */

package gt6u.loaders;

import gregapi.GT_API;
import gregapi.code.ICondition.And;
import gregapi.config.ConfigCategories;
import gregapi.data.IL;
import gregapi.data.MT;
import gregapi.oredict.OreDictManager;
import gregtech.loaders.a.Loader_Tools.OreProcessing_Tool;
import gt6u.data.CS_GT6U;
import net.minecraft.world.item.ItemStack;

import static gregapi.data.CS.*;
import static gregapi.data.OP.*;
import static gregapi.data.TD.Atomic.ANTIMATTER;
import static gregapi.data.TD.Properties.*;
import static gregapi.oredict.OreDictMaterialCondition.*;
import static gt6u.data.CS_GT6U.*;

/** GregTech6-Unofficial's part of gregtech/loaders/a/Loader_Tools.java: its EV electric tools assembled from their heads
 *  (GT6U Loader_Tools.java:382-388). GT6U never adds these tools to the MetaTool, so the listeners find no tool to make, as in GT6U. */
public class Loader_Tools {
	public static void run() {
		// Queued as GT6 queues its own electric tool recipes (gregtech/loaders/a/Loader_Tools.java): battery lookups need the post-bind window.
		GAPI.mBeforePostInit.add(() -> GT_API.deferItemInit(() -> {
			final String tCategory = ConfigCategories.Recipes.gregtechtools + ".";
			for (ItemStack tBattery : OreDictManager.getOres("gt:re-battery3", F)) {
			toolHeadWrench              .addListener(new OreProcessing_Tool(WRENCH_EV               , tCategory + "WrenchEV"                ,F,T, -1,V[4], MT.DATA.Electric_T[4]    , new String[][] {{"dAT", "XWX", "XVX"}}                                                    , null                                                          , plateCurved.dat(MT.DATA.Electric_T[4]), null, null, tBattery, IL.MOTORS[4], new And(ANTIMATTER.NOT, WOOD.NOT, BOUNCY.NOT, STRETCHY.NOT, typemin(3), qualmin(1))));
			toolHeadDrill               .addListener(new OreProcessing_Tool(MININGDRILL_EV          , tCategory + "MiningDrillEV"           ,F,T, -1,V[4], MT.DATA.Electric_T[4]    , new String[][] {{"dAT", "XWX", "XVX"}}                                                    , null                                                          , plateCurved.dat(MT.DATA.Electric_T[4]), null, null, tBattery, IL.MOTORS[4], new And(ANTIMATTER.NOT, WOOD.NOT, BOUNCY.NOT, STRETCHY.NOT, typemin(3), qualmin(1))));
			toolHeadChainsaw            .addListener(new OreProcessing_Tool(CHAINSAW_EV             , tCategory + "ChainsawEV"              ,F,T, -1,V[4], MT.DATA.Electric_T[4]    , new String[][] {{"dAT", "XWX", "XVX"}}                                                    , null                                                          , plateCurved.dat(MT.DATA.Electric_T[4]), null, null, tBattery, IL.MOTORS[4], new And(ANTIMATTER.NOT, WOOD.NOT, BOUNCY.NOT, STRETCHY.NOT, typemin(3), qualmin(1))));
			toolHeadDrill               .addListener(new OreProcessing_Tool(JACKHAMMER_EV_Normal    , tCategory + "JackhammerEV"            ,F,T, -1,V[4], MT.DATA.Electric_T[4]    , new String[][] {{"SVS", "XWX", "YSY"}}                                                    , null                                                          , plateCurved.dat(MT.DATA.Electric_T[4]), spring.dat(MT.DATA.Electric_T[4]), null, tBattery, IL.PISTONS[4], new And(ANTIMATTER.NOT, WOOD.NOT, BOUNCY.NOT, STRETCHY.NOT, typemin(3), qualmin(1))));
			}
		}));
	}
}

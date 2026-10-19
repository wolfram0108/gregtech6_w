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
 * the GregTech6-Unofficial entries of gregtech/items/MultiItemTechnological.java moved into an addon registry.
 */

package gt6u.items;

import gregapi.data.ANY;
import gregapi.data.MT;
import gregapi.data.TC;
import gregapi.item.CreativeTab;
import gregapi.item.multiitem.MultiItemRandom;
import gregapi.oredict.OreDictItemData;
import gt6u.GT6U_Main;
import gt6u.data.IL_GT6U;
import gt6u.data.MT_GT6U;

import static gregapi.data.CS.*;

/** The technological items GregTech6-Unofficial added to GT6's MultiItem (GT6U MultiItemTechnological.java), in the addon's own
 *  MultiItem under GT6U's ids; their recipes are in MultiItemTechnological_Recipes. */
public class MultiItemTechnological extends MultiItemRandom {
	public MultiItemTechnological() {
		super(GT6U_Main.MOD_ID, "gt.multiitem.technological");
		new CreativeTab(getUnlocalizedName(), "GregTech: Technology (GT6U)", this, (short)30017);
	}
	
	@Override
	public void addItems() {
		int tLastID = 0;
		IL_GT6U.Circuit_Plate_Empty_Phenolic.set( addItem(tLastID = 30017, "Phenolic Circuit Plate" , "Needs Circuit Wiring" , new OreDictItemData(ANY.Cu, U100*25, MT.Wood, U*4), TC.stack(TC.FABRICO, 1)));
		IL_GT6U.Circuit_Plate_Empty_Epoxid.set( addItem(tLastID = 30018, "Epoxid Circuit Plate" , "Needs Circuit Wiring" , new OreDictItemData(ANY.Cu, U, MT_GT6U.Epoxid, U), TC.stack(TC.FABRICO, 1)));
		IL_GT6U.Circuit_Plate_Empty_Multilayer.set( addItem(tLastID = 30019, "Multilayer Epoxid Circuit Plate" , "Needs Circuit Wiring" , new OreDictItemData(ANY.Cu, U*2, MT_GT6U.Epoxid, U), TC.stack(TC.FABRICO, 1)));
		IL_GT6U.Circuit_plate_Empty_Crystal.set( addItem(tLastID = 30020, "Aluminium Printed Circuit Plate" , "Needs Circuit Wiring" , new OreDictItemData(ANY.Cu, U*2, MT_GT6U.Epoxid, U*2), TC.stack(TC.FABRICO, 1)));
		IL_GT6U.Perforated_Bushing.set( addItem(tLastID = 30021, "Perforated Bushing" , "Used to manufacture glass fiber" , new OreDictItemData(ANY.Steel, U10*65), TC.stack(TC.FABRICO, 1)));
		IL_GT6U.Glass_Fiber.set( addItem(tLastID = 30022, "Fibreglass" , "About 1/20 of your hair's diameter" , new OreDictItemData(MT.Glass, U100), TC.stack(TC.FABRICO, 1)));
	}
}

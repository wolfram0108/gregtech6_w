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
 * the GregTech6-Unofficial entries of gregtech/loaders/a/Loader_Blocks.java moved into an addon registry.
 */

package gt6u.loaders;


import static gregapi.data.CS.*;

import gregapi.block.MaterialGas;
import gregapi.block.MaterialOil;
import gregapi.block.fluid.BlockBaseFluid;
import gregapi.config.ConfigCategories;
import gregapi.data.ANY;
import gregapi.data.CS.BlocksGT;
import gregapi.data.CS.ItemsGT;
import gregapi.data.FL;
import gregapi.data.MD;
import gregapi.data.MT;
import gregapi.data.OP;
import gregapi.data.TC;
import gregapi.old.Textures;
import gregapi.util.CR;
import gregapi.util.ST;
import gregapi.util.UT;
import gregtech.blocks.*;
import gregtech.blocks.fluids.BlockOcean;
import gregtech.blocks.fluids.BlockRiver;
import gregtech.blocks.fluids.BlockSwamp;
import gregtech.blocks.plants.BlockFlowersA;
import gregtech.blocks.plants.BlockFlowersB;
import gregtech.blocks.plants.BlockGlowtus;
import gregtech.blocks.stone.BlockCrystalOres;
import gregtech.blocks.stone.BlockRockOres;
import gregtech.blocks.stone.BlockVanillaOresA;
import gregtech.blocks.tool.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;

import gt6u.GT6U_Recipes;

/** GregTech6-Unofficial's edits of gregtech/loaders/a/Loader_Blocks.java, as statements: what GT6U added runs in run(), what it dropped is taken back in
 *  takeBack(): restated as GT6 makes them, their Recipes are taken out of GT6's maps again. */
public class Loader_Blocks {
	public static void run() {
		run_run();
	}
	
	public static void takeBack() {
		takeBack_run();
	}
	
	/** GT6U's additions to Loader_Blocks.run (GT6U gregtech/loaders/a/Loader_Blocks.java). */
	static void run_run() {
		int long_num = ConfigsGT.RECIPES.get("longdistcrafting", "long-dist-wire-craft-out-num", 16);
		CR.shaped(ST.make(BlocksGT.LongDistWire01, long_num, 0), CR.DEF_REV_NCC, "RSR", "PWP", "RSR", 'R', OP.plate.dat(ANY.Rubber), 'P', OP.plateCurved.dat(ANY.Cu), 'S', OP.plateCurved.dat(MT.Al), 'W', OP.wireGt16.dat(MT.Sn));
		CR.shaped(ST.make(BlocksGT.LongDistWire01, long_num, 1), CR.DEF_REV_NCC, "RSR", "PWP", "RSR", 'R', OP.plate.dat(ANY.Rubber), 'P', OP.plateCurved.dat(ANY.Cu), 'S', OP.plateCurved.dat(MT.Al), 'W', OP.wireGt16.dat(MT.Pb));
		CR.shaped(ST.make(BlocksGT.LongDistWire01, long_num, 2), CR.DEF_REV_NCC, "RSR", "PWP", "RSR", 'R', OP.plate.dat(ANY.Rubber), 'P', OP.plateCurved.dat(ANY.Cu), 'S', OP.plateCurved.dat(MT.Al), 'W', OP.wireGt16.dat(ANY.Cu));
		CR.shaped(ST.make(BlocksGT.LongDistWire01, long_num, 3), CR.DEF_REV_NCC, "RSR", "PWP", "RSR", 'R', OP.plate.dat(ANY.Rubber), 'P', OP.plateCurved.dat(ANY.Cu), 'S', OP.plateCurved.dat(MT.Al), 'W', OP.wireGt16.dat(MT.Ag));
		CR.shaped(ST.make(BlocksGT.LongDistWire01, long_num, 4), CR.DEF_REV_NCC, "RSR", "PWP", "RSR", 'R', OP.plate.dat(ANY.Rubber), 'P', OP.plateCurved.dat(ANY.Cu), 'S', OP.plateCurved.dat(MT.Al), 'W', OP.wireGt16.dat(MT.Au));
		CR.shaped(ST.make(BlocksGT.LongDistWire01, long_num, 5), CR.DEF_REV_NCC, "RSR", "PWP", "RSR", 'R', OP.plate.dat(ANY.Rubber), 'P', OP.plateCurved.dat(ANY.Cu), 'S', OP.plateCurved.dat(MT.Al), 'W', OP.wireGt16.dat(MT.Electrum));
		CR.shaped(ST.make(BlocksGT.LongDistWire01, long_num, 6), CR.DEF_REV_NCC, "RSR", "PWP", "RSR", 'R', OP.plate.dat(ANY.Rubber), 'P', OP.plateCurved.dat(ANY.Cu), 'S', OP.plateCurved.dat(MT.Al), 'W', OP.wireGt16.dat(MT.BlueAlloy));
		CR.shaped(ST.make(BlocksGT.LongDistWire01, long_num, 7), CR.DEF_REV_NCC, "RSR", "PWP", "RSR", 'R', OP.plate.dat(ANY.Rubber), 'P', OP.plateCurved.dat(ANY.Cu), 'S', OP.plateCurved.dat(MT.Al), 'W', OP.wireGt16.dat(MT.ElectrotineAlloy));
		CR.shaped(ST.make(BlocksGT.LongDistWire01, long_num, 8), CR.DEF_REV_NCC, "RSR", "PWP", "RSR", 'R', OP.plate.dat(ANY.Rubber), 'P', OP.plateCurved.dat(ANY.Cu), 'S', OP.plateCurved.dat(MT.Al), 'W', OP.wireGt16.dat(ANY.Steel));
		CR.shaped(ST.make(BlocksGT.LongDistWire01, long_num, 9), CR.DEF_REV_NCC, "RSR", "PWP", "RSR", 'R', OP.plate.dat(ANY.Rubber), 'P', OP.plateCurved.dat(ANY.Cu), 'S', OP.plateCurved.dat(MT.Al), 'W', OP.wireGt16.dat(MT.Al));
		CR.shaped(ST.make(BlocksGT.LongDistWire01, long_num, 10), CR.DEF_REV_NCC, "RSR", "PWP", "RSR", 'R', OP.plate.dat(ANY.Rubber), 'P', OP.plateCurved.dat(ANY.Cu), 'S', OP.plateCurved.dat(MT.Al), 'W', OP.wireGt16.dat(ANY.W));
		CR.shaped(ST.make(BlocksGT.LongDistWire01, long_num, 11), CR.DEF_REV_NCC, "RSR", "PWP", "RSR", 'R', OP.plate.dat(ANY.Rubber), 'P', OP.plateCurved.dat(ANY.Cu), 'S', OP.plateCurved.dat(MT.Al), 'W', OP.wireGt16.dat(MT.TungstenSteel));
		CR.shaped(ST.make(BlocksGT.LongDistWire01, long_num, 12), CR.DEF_REV_NCC, "RSR", "PWP", "RSR", 'R', OP.plate.dat(ANY.Rubber), 'P', OP.plateCurved.dat(ANY.Cu), 'S', OP.plateCurved.dat(MT.Al), 'W', OP.wireGt16.dat(MT.Os));
		CR.shaped(ST.make(BlocksGT.LongDistWire01, long_num, 13), CR.DEF_REV_NCC, "RSR", "PWP", "RSR", 'R', OP.plate.dat(ANY.Rubber), 'P', OP.plateCurved.dat(ANY.Cu), 'S', OP.plateCurved.dat(MT.Al), 'W', OP.wireGt16.dat(MT.Pt));
		CR.shaped(ST.make(BlocksGT.LongDistWire01, long_num, 14), CR.DEF_REV_NCC, "RSR", "PWP", "RSR", 'R', OP.plate.dat(ANY.Rubber), 'P', OP.plateCurved.dat(ANY.Cu), 'S', OP.plateCurved.dat(MT.Al), 'W', OP.wireGt16.dat(MT.Nq));
		CR.shaped(ST.make(BlocksGT.LongDistWire01, long_num, 15), CR.DEF_REV_NCC, "RSR", "PWP", "RSR", 'R', OP.plate.dat(ANY.Rubber), 'P', OP.plateCurved.dat(ANY.Cu), 'S', OP.plateCurved.dat(MT.Al), 'W', OP.wireGt16.dat(MT.Graphene));
		long_num = ConfigsGT.RECIPES.get("longdistcrafting", "long-dist-pipe-craft-out-num", 16);
		CR.shaped(ST.make(BlocksGT.LongDistPipe01, long_num, 0), CR.DEF_REV_NCC, "SPS", "PwP", "SPS", 'P', OP.pipeMedium.dat(MT.Electrum), 'S', OP.plate.dat(ANY.Plastic));
		CR.shaped(ST.make(BlocksGT.LongDistPipe01, long_num, 1), CR.DEF_REV_NCC, "SPS", "PwP", "SPS", 'P', OP.pipeMedium.dat(MT.StainlessSteel), 'S', OP.plate.dat(ANY.Plastic));
		CR.shaped(ST.make(BlocksGT.LongDistPipe01, long_num, 2), CR.DEF_REV_NCC, "SPS", "PwP", "SPS", 'P', OP.pipeMedium.dat(ANY.W), 'S', OP.plate.dat(ANY.Plastic));
		CR.shaped(ST.make(BlocksGT.LongDistPipe01, long_num, 3), CR.DEF_REV_NCC, "SPS", "PwP", "SPS", 'P', OP.pipeMedium.dat(MT.Ad), 'S', OP.plate.dat(ANY.Plastic));
		CR.shaped(ST.make(BlocksGT.LongDistPipe01, long_num, 4), CR.DEF_REV_NCC, "SPS", "PwP", "SPS", 'P', OP.pipeMedium.dat(MT.Draconium), 'S', OP.plate.dat(ANY.Plastic));
	}

	/** GT6's statements in Loader_Blocks.run that GT6U dropped, as the port has them: their Recipes are taken back. */
	static void takeBack_run() {
		GT6U_Recipes.takeBack("Loader_Blocks.run", () -> {
			CR.shaped(ST.make(BlocksGT.LongDistWire01, 1, 0), CR.DEF_REV_NCC, "RSR", "PWP", "RSR", 'R', OP.plate.dat(ANY.Rubber), 'P', OP.plateCurved.dat(ANY.Cu), 'S', OP.plateCurved.dat(MT.Al), 'W', OP.wireGt16.dat(MT.Sn));
			CR.shaped(ST.make(BlocksGT.LongDistWire01, 1, 1), CR.DEF_REV_NCC, "RSR", "PWP", "RSR", 'R', OP.plate.dat(ANY.Rubber), 'P', OP.plateCurved.dat(ANY.Cu), 'S', OP.plateCurved.dat(MT.Al), 'W', OP.wireGt16.dat(MT.Pb));
			CR.shaped(ST.make(BlocksGT.LongDistWire01, 1, 2), CR.DEF_REV_NCC, "RSR", "PWP", "RSR", 'R', OP.plate.dat(ANY.Rubber), 'P', OP.plateCurved.dat(ANY.Cu), 'S', OP.plateCurved.dat(MT.Al), 'W', OP.wireGt16.dat(ANY.Cu));
			CR.shaped(ST.make(BlocksGT.LongDistWire01, 1, 3), CR.DEF_REV_NCC, "RSR", "PWP", "RSR", 'R', OP.plate.dat(ANY.Rubber), 'P', OP.plateCurved.dat(ANY.Cu), 'S', OP.plateCurved.dat(MT.Al), 'W', OP.wireGt16.dat(MT.Ag));
			CR.shaped(ST.make(BlocksGT.LongDistWire01, 1, 4), CR.DEF_REV_NCC, "RSR", "PWP", "RSR", 'R', OP.plate.dat(ANY.Rubber), 'P', OP.plateCurved.dat(ANY.Cu), 'S', OP.plateCurved.dat(MT.Al), 'W', OP.wireGt16.dat(MT.Au));
			CR.shaped(ST.make(BlocksGT.LongDistWire01, 1, 5), CR.DEF_REV_NCC, "RSR", "PWP", "RSR", 'R', OP.plate.dat(ANY.Rubber), 'P', OP.plateCurved.dat(ANY.Cu), 'S', OP.plateCurved.dat(MT.Al), 'W', OP.wireGt16.dat(MT.Electrum));
			CR.shaped(ST.make(BlocksGT.LongDistWire01, 1, 6), CR.DEF_REV_NCC, "RSR", "PWP", "RSR", 'R', OP.plate.dat(ANY.Rubber), 'P', OP.plateCurved.dat(ANY.Cu), 'S', OP.plateCurved.dat(MT.Al), 'W', OP.wireGt16.dat(MT.BlueAlloy));
			CR.shaped(ST.make(BlocksGT.LongDistWire01, 1, 7), CR.DEF_REV_NCC, "RSR", "PWP", "RSR", 'R', OP.plate.dat(ANY.Rubber), 'P', OP.plateCurved.dat(ANY.Cu), 'S', OP.plateCurved.dat(MT.Al), 'W', OP.wireGt16.dat(MT.ElectrotineAlloy));
			CR.shaped(ST.make(BlocksGT.LongDistWire01, 1, 8), CR.DEF_REV_NCC, "RSR", "PWP", "RSR", 'R', OP.plate.dat(ANY.Rubber), 'P', OP.plateCurved.dat(ANY.Cu), 'S', OP.plateCurved.dat(MT.Al), 'W', OP.wireGt16.dat(ANY.Steel));
			CR.shaped(ST.make(BlocksGT.LongDistWire01, 1, 9), CR.DEF_REV_NCC, "RSR", "PWP", "RSR", 'R', OP.plate.dat(ANY.Rubber), 'P', OP.plateCurved.dat(ANY.Cu), 'S', OP.plateCurved.dat(MT.Al), 'W', OP.wireGt16.dat(MT.Al));
			CR.shaped(ST.make(BlocksGT.LongDistWire01, 1, 10), CR.DEF_REV_NCC, "RSR", "PWP", "RSR", 'R', OP.plate.dat(ANY.Rubber), 'P', OP.plateCurved.dat(ANY.Cu), 'S', OP.plateCurved.dat(MT.Al), 'W', OP.wireGt16.dat(ANY.W));
			CR.shaped(ST.make(BlocksGT.LongDistWire01, 1, 11), CR.DEF_REV_NCC, "RSR", "PWP", "RSR", 'R', OP.plate.dat(ANY.Rubber), 'P', OP.plateCurved.dat(ANY.Cu), 'S', OP.plateCurved.dat(MT.Al), 'W', OP.wireGt16.dat(MT.TungstenSteel));
			CR.shaped(ST.make(BlocksGT.LongDistWire01, 1, 12), CR.DEF_REV_NCC, "RSR", "PWP", "RSR", 'R', OP.plate.dat(ANY.Rubber), 'P', OP.plateCurved.dat(ANY.Cu), 'S', OP.plateCurved.dat(MT.Al), 'W', OP.wireGt16.dat(MT.Os));
			CR.shaped(ST.make(BlocksGT.LongDistWire01, 1, 13), CR.DEF_REV_NCC, "RSR", "PWP", "RSR", 'R', OP.plate.dat(ANY.Rubber), 'P', OP.plateCurved.dat(ANY.Cu), 'S', OP.plateCurved.dat(MT.Al), 'W', OP.wireGt16.dat(MT.Pt));
			CR.shaped(ST.make(BlocksGT.LongDistWire01, 1, 14), CR.DEF_REV_NCC, "RSR", "PWP", "RSR", 'R', OP.plate.dat(ANY.Rubber), 'P', OP.plateCurved.dat(ANY.Cu), 'S', OP.plateCurved.dat(MT.Al), 'W', OP.wireGt16.dat(MT.Nq));
			CR.shaped(ST.make(BlocksGT.LongDistWire01, 1, 15), CR.DEF_REV_NCC, "RSR", "PWP", "RSR", 'R', OP.plate.dat(ANY.Rubber), 'P', OP.plateCurved.dat(ANY.Cu), 'S', OP.plateCurved.dat(MT.Al), 'W', OP.wireGt16.dat(MT.Graphene));
			CR.shaped(ST.make(BlocksGT.LongDistPipe01, 1, 0), CR.DEF_REV_NCC, "SPS", "PwP", "SPS", 'P', OP.pipeMedium.dat(MT.Electrum), 'S', OP.plate.dat(ANY.Plastic));
			CR.shaped(ST.make(BlocksGT.LongDistPipe01, 1, 1), CR.DEF_REV_NCC, "SPS", "PwP", "SPS", 'P', OP.pipeMedium.dat(MT.StainlessSteel), 'S', OP.plate.dat(ANY.Plastic));
			CR.shaped(ST.make(BlocksGT.LongDistPipe01, 1, 2), CR.DEF_REV_NCC, "SPS", "PwP", "SPS", 'P', OP.pipeMedium.dat(ANY.W), 'S', OP.plate.dat(ANY.Plastic));
			CR.shaped(ST.make(BlocksGT.LongDistPipe01, 1, 3), CR.DEF_REV_NCC, "SPS", "PwP", "SPS", 'P', OP.pipeMedium.dat(MT.Ad), 'S', OP.plate.dat(ANY.Plastic));
			CR.shaped(ST.make(BlocksGT.LongDistPipe01, 1, 4), CR.DEF_REV_NCC, "SPS", "PwP", "SPS", 'P', OP.pipeMedium.dat(MT.Draconium), 'S', OP.plate.dat(ANY.Plastic));
		});
	}
}

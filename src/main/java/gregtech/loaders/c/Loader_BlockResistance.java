/**
 * Copyright (c) 2022 GregTech-6 Team
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
 */

package gregtech.loaders.c;

import gregapi.util.WD;
import gregapi.block.MaterialAdventure;
import gregapi.data.IL;
import gregapi.data.MD;
import gregapi.util.ST;
import gregapi.util.UT;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import static gregapi.data.CS.*;

public class Loader_BlockResistance implements Runnable {
	@Override
	public void run() {
		WD.setResistance(Blocks.STONE, 10);
		WD.setResistance(Blocks.COBBLESTONE, 10);
		WD.setResistance(Blocks.STONE_BRICKS, 10);
		WD.setResistance(Blocks.BRICKS, 20);
		WD.setResistance(Blocks.TERRACOTTA, 15);
		WD.setResistance(Blocks.WHITE_TERRACOTTA, 15);
		WD.setResistance(Blocks.IRON_BLOCK, 30);
		WD.setResistance(Blocks.DIAMOND_BLOCK, 60);
		WD.setResistance(Blocks.OBSIDIAN, 60);
		WD.setResistance(Blocks.ENCHANTING_TABLE, 60);
		WD.setResistance(Blocks.ENDER_CHEST, 60);
		WD.setResistance(Blocks.ANVIL, 60);
		WD.setResistance(Blocks.WATER, 30);
		WD.setResistance(Blocks.WATER, 30);
		WD.setResistance(Blocks.LAVA, 30);
		
		if (MD.SD.mLoaded) {
			Block
			tBlock = ST.block(MD.SD, "fullDrawers1"); if (tBlock != NB) {UT.Reflection.setFieldContent(Block.class, tBlock, "field_149764_J", MaterialAdventure.WOOD, T, F); UT.Reflection.setFieldContent(Block.class, tBlock, "blockMaterial", MaterialAdventure.WOOD, T, F);}
			tBlock = ST.block(MD.SD, "fullDrawers2"); if (tBlock != NB) {UT.Reflection.setFieldContent(Block.class, tBlock, "field_149764_J", MaterialAdventure.WOOD, T, F); UT.Reflection.setFieldContent(Block.class, tBlock, "blockMaterial", MaterialAdventure.WOOD, T, F);}
			tBlock = ST.block(MD.SD, "fullDrawers4"); if (tBlock != NB) {UT.Reflection.setFieldContent(Block.class, tBlock, "field_149764_J", MaterialAdventure.WOOD, T, F); UT.Reflection.setFieldContent(Block.class, tBlock, "blockMaterial", MaterialAdventure.WOOD, T, F);}
			tBlock = ST.block(MD.SD, "halfDrawers2"); if (tBlock != NB) {UT.Reflection.setFieldContent(Block.class, tBlock, "field_149764_J", MaterialAdventure.WOOD, T, F); UT.Reflection.setFieldContent(Block.class, tBlock, "blockMaterial", MaterialAdventure.WOOD, T, F);}
			tBlock = ST.block(MD.SD, "halfDrawers4"); if (tBlock != NB) {UT.Reflection.setFieldContent(Block.class, tBlock, "field_149764_J", MaterialAdventure.WOOD, T, F); UT.Reflection.setFieldContent(Block.class, tBlock, "blockMaterial", MaterialAdventure.WOOD, T, F);}
			tBlock = ST.block(MD.SD, "fullCustom1" ); if (tBlock != NB) {UT.Reflection.setFieldContent(Block.class, tBlock, "field_149764_J", MaterialAdventure.WOOD, T, F); UT.Reflection.setFieldContent(Block.class, tBlock, "blockMaterial", MaterialAdventure.WOOD, T, F);}
			tBlock = ST.block(MD.SD, "fullCustom2" ); if (tBlock != NB) {UT.Reflection.setFieldContent(Block.class, tBlock, "field_149764_J", MaterialAdventure.WOOD, T, F); UT.Reflection.setFieldContent(Block.class, tBlock, "blockMaterial", MaterialAdventure.WOOD, T, F);}
			tBlock = ST.block(MD.SD, "fullCustom4" ); if (tBlock != NB) {UT.Reflection.setFieldContent(Block.class, tBlock, "field_149764_J", MaterialAdventure.WOOD, T, F); UT.Reflection.setFieldContent(Block.class, tBlock, "blockMaterial", MaterialAdventure.WOOD, T, F);}
			tBlock = ST.block(MD.SD, "halfCustom2" ); if (tBlock != NB) {UT.Reflection.setFieldContent(Block.class, tBlock, "field_149764_J", MaterialAdventure.WOOD, T, F); UT.Reflection.setFieldContent(Block.class, tBlock, "blockMaterial", MaterialAdventure.WOOD, T, F);}
			tBlock = ST.block(MD.SD, "halfCustom4" ); if (tBlock != NB) {UT.Reflection.setFieldContent(Block.class, tBlock, "field_149764_J", MaterialAdventure.WOOD, T, F); UT.Reflection.setFieldContent(Block.class, tBlock, "blockMaterial", MaterialAdventure.WOOD, T, F);}
		}
		
		Block
		tBlock = IL.EtFu_Obsidian      .block(); if (tBlock != null && tBlock != NB) WD.setResistance(tBlock, 60);
		tBlock = IL.NeLi_Obsidian      .block(); if (tBlock != null && tBlock != NB) WD.setResistance(tBlock, 60);
		tBlock = IL.NePl_Obsidian      .block(); if (tBlock != null && tBlock != NB) WD.setResistance(tBlock, 60);
		tBlock = IL.NePl_Ancient_Debris.block(); if (tBlock != null && tBlock != NB) WD.setResistance(tBlock, 60);
		tBlock = IL.EtFu_Ancient_Debris.block(); if (tBlock != null && tBlock != NB) WD.setResistance(tBlock, 60);
	}
}

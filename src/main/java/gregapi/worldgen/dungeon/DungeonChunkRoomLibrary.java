/**
 * Copyright (c) 2021 GregTech-6 Team
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

package gregapi.worldgen.dungeon;

import static gregapi.data.CS.*;

import gregapi.data.FL;
import gregapi.data.MD;
import gregapi.util.ST;
import gregapi.util.UT;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.nbt.ListTag;
import net.minecraftforge.common.ChestGenHooks;

/**
 * @author Gregorius Techneticies
 */
public class DungeonChunkRoomLibrary extends DungeonChunkRoomEmpty {
	@Override
	public boolean generate(DungeonData aData) {
		if (!super.generate(aData)) return F;
		aData.mTags.add(WorldgenDungeonGT.TAG_LIBRARY);
		
		int tPlank = aData.next(6), tSlab = tPlank + 8, tShelf = 7000 + tPlank;
		
		String[] tLoots = new String[] {ChestGenHooks.STRONGHOLD_LIBRARY, ChestGenHooks.STRONGHOLD_CORRIDOR, ChestGenHooks.STRONGHOLD_CROSSING, ChestGenHooks.PYRAMID_DESERT_CHEST, ChestGenHooks.PYRAMID_JUNGLE_CHEST, ChestGenHooks.VILLAGE_BLACKSMITH, ChestGenHooks.MINESHAFT_CORRIDOR, ChestGenHooks.DUNGEON_CHEST, ChestGenHooks.BONUS_CHEST};
		
		for (int tX = 2; tX <= 13; tX++) for (int tZ = 2; tZ <= 13; tZ++) {
			aData.set(tX, 1, tZ, Blocks.WHITE_CARPET, aData.mColorInversed);
		}
		
		for (int tX = 1; tX <= 14; tX++) for (int tZ = 1; tZ <= 14; tZ++) {
			aData.set(tX, 7, tZ, Blocks.OAK_PLANKS, tPlank);
			aData.set(tX, 8, tZ, Blocks.OAK_SLAB, tPlank);
			if (tX == 1 || tX == 14 || tZ == 1 || tZ == 14) {
				aData.set(tX, 6, tZ, Blocks.OAK_PLANKS, tPlank);
				aData.set(tX, 5, tZ, Blocks.OAK_SLAB, tSlab);
			} else if ((tX == 3 || tX == 6 || tX == 9 || tX == 12) && (tZ == 3 || tZ == 6 || tZ == 9 || tZ == 12)) {
				aData.lamp(tX, 6, tZ, +1);
			} else {
				aData.set(tX, 6, tZ, Blocks.OAK_SLAB, tSlab);
			}
		}
		
		aData.set( 2, 3,  4, Blocks.OAK_SLAB, tSlab);
		aData.set( 2, 3,  3, Blocks.OAK_SLAB, tSlab);
		aData.set( 2, 3,  2, Blocks.OAK_PLANKS, tPlank);
		aData.set( 3, 3,  2, Blocks.OAK_SLAB, tSlab);
		aData.set( 4, 3,  2, Blocks.OAK_SLAB, tSlab);
		
		aData.set(13, 3,  4, Blocks.OAK_SLAB, tSlab);
		aData.set(13, 3,  3, Blocks.OAK_SLAB, tSlab);
		aData.set(13, 3,  2, Blocks.OAK_PLANKS, tPlank);
		aData.set(12, 3,  2, Blocks.OAK_SLAB, tSlab);
		aData.set(11, 3,  2, Blocks.OAK_SLAB, tSlab);
		
		aData.set( 2, 3, 11, Blocks.OAK_SLAB, tSlab);
		aData.set( 2, 3, 12, Blocks.OAK_SLAB, tSlab);
		aData.set( 2, 3, 13, Blocks.OAK_PLANKS, tPlank);
		aData.set( 3, 3, 13, Blocks.OAK_SLAB, tSlab);
		aData.set( 4, 3, 13, Blocks.OAK_SLAB, tSlab);
		
		aData.set(13, 3, 11, Blocks.OAK_SLAB, tSlab);
		aData.set(13, 3, 12, Blocks.OAK_SLAB, tSlab);
		aData.set(13, 3, 13, Blocks.OAK_PLANKS, tPlank);
		aData.set(12, 3, 13, Blocks.OAK_SLAB, tSlab);
		aData.set(11, 3, 13, Blocks.OAK_SLAB, tSlab);
		
		for (int tY = 1; tY <= 5; tY++) {
			aData.set( 1, tY,  1, Blocks.OAK_PLANKS, tPlank);
			aData.set(14, tY,  1, Blocks.OAK_PLANKS, tPlank);
			aData.set( 1, tY, 14, Blocks.OAK_PLANKS, tPlank);
			aData.set(14, tY, 14, Blocks.OAK_PLANKS, tPlank);
			
			if (tY == 3) {
				aData.set( 2, tY,  1, Blocks.OAK_PLANKS, tPlank);
				aData.set( 3, tY,  1, Blocks.OAK_PLANKS, tPlank);
				aData.set( 4, tY,  1, Blocks.OAK_PLANKS, tPlank);
				aData.set(11, tY,  1, Blocks.OAK_PLANKS, tPlank);
				aData.set(12, tY,  1, Blocks.OAK_PLANKS, tPlank);
				aData.set(13, tY,  1, Blocks.OAK_PLANKS, tPlank);
				
				aData.set(14, tY,  2, Blocks.OAK_PLANKS, tPlank);
				aData.set(14, tY,  3, Blocks.OAK_PLANKS, tPlank);
				aData.set(14, tY,  4, Blocks.OAK_PLANKS, tPlank);
				aData.set(14, tY, 11, Blocks.OAK_PLANKS, tPlank);
				aData.set(14, tY, 12, Blocks.OAK_PLANKS, tPlank);
				aData.set(14, tY, 13, Blocks.OAK_PLANKS, tPlank);
				
				aData.set( 2, tY, 14, Blocks.OAK_PLANKS, tPlank);
				aData.set( 3, tY, 14, Blocks.OAK_PLANKS, tPlank);
				aData.set( 4, tY, 14, Blocks.OAK_PLANKS, tPlank);
				aData.set(11, tY, 14, Blocks.OAK_PLANKS, tPlank);
				aData.set(12, tY, 14, Blocks.OAK_PLANKS, tPlank);
				aData.set(13, tY, 14, Blocks.OAK_PLANKS, tPlank);
				
				aData.set( 1, tY,  2, Blocks.OAK_PLANKS, tPlank);
				aData.set( 1, tY,  3, Blocks.OAK_PLANKS, tPlank);
				aData.set( 1, tY,  4, Blocks.OAK_PLANKS, tPlank);
				aData.set( 1, tY, 11, Blocks.OAK_PLANKS, tPlank);
				aData.set( 1, tY, 12, Blocks.OAK_PLANKS, tPlank);
				aData.set( 1, tY, 13, Blocks.OAK_PLANKS, tPlank);
			} else {
				int tIndex = aData.next(24), tKeyIndex = aData.next(3)+aData.next(3);
				ListTag tList = null;
				if (tKeyIndex < aData.mGeneratedKeys.length) {
					aData.mGeneratedKeys[tKeyIndex] = T;
					tList = new ListTag();
					tList.add(UT.NBT.makeShort(ST.save(aData.mKeyStacks[tKeyIndex]), "s", (short)aData.next(14)));
				}
				
				aData.shelf( 2, tY,  1, tShelf, SIDE_Z_POS, tLoots, tIndex--==0?tList:null);
				aData.shelf( 3, tY,  1, tShelf, SIDE_Z_POS, tLoots, tIndex--==0?tList:null);
				aData.shelf( 4, tY,  1, tShelf, SIDE_Z_POS, tLoots, tIndex--==0?tList:null);
				aData.shelf(11, tY,  1, tShelf, SIDE_Z_POS, tLoots, tIndex--==0?tList:null);
				aData.shelf(12, tY,  1, tShelf, SIDE_Z_POS, tLoots, tIndex--==0?tList:null);
				aData.shelf(13, tY,  1, tShelf, SIDE_Z_POS, tLoots, tIndex--==0?tList:null);
				
				aData.shelf(14, tY,  2, tShelf, SIDE_X_NEG, tLoots, tIndex--==0?tList:null);
				aData.shelf(14, tY,  3, tShelf, SIDE_X_NEG, tLoots, tIndex--==0?tList:null);
				aData.shelf(14, tY,  4, tShelf, SIDE_X_NEG, tLoots, tIndex--==0?tList:null);
				aData.shelf(14, tY, 11, tShelf, SIDE_X_NEG, tLoots, tIndex--==0?tList:null);
				aData.shelf(14, tY, 12, tShelf, SIDE_X_NEG, tLoots, tIndex--==0?tList:null);
				aData.shelf(14, tY, 13, tShelf, SIDE_X_NEG, tLoots, tIndex--==0?tList:null);
				
				aData.shelf( 2, tY, 14, tShelf, SIDE_Z_NEG, tLoots, tIndex--==0?tList:null);
				aData.shelf( 3, tY, 14, tShelf, SIDE_Z_NEG, tLoots, tIndex--==0?tList:null);
				aData.shelf( 4, tY, 14, tShelf, SIDE_Z_NEG, tLoots, tIndex--==0?tList:null);
				aData.shelf(11, tY, 14, tShelf, SIDE_Z_NEG, tLoots, tIndex--==0?tList:null);
				aData.shelf(12, tY, 14, tShelf, SIDE_Z_NEG, tLoots, tIndex--==0?tList:null);
				aData.shelf(13, tY, 14, tShelf, SIDE_Z_NEG, tLoots, tIndex--==0?tList:null);
				
				aData.shelf( 1, tY,  2, tShelf, SIDE_X_POS, tLoots, tIndex--==0?tList:null);
				aData.shelf( 1, tY,  3, tShelf, SIDE_X_POS, tLoots, tIndex--==0?tList:null);
				aData.shelf( 1, tY,  4, tShelf, SIDE_X_POS, tLoots, tIndex--==0?tList:null);
				aData.shelf( 1, tY, 11, tShelf, SIDE_X_POS, tLoots, tIndex--==0?tList:null);
				aData.shelf( 1, tY, 12, tShelf, SIDE_X_POS, tLoots, tIndex--==0?tList:null);
				aData.shelf( 1, tY, 13, tShelf, SIDE_X_POS, tLoots, tIndex--==0?tList:null);
			}
		}
		
		Block
		tHexoriumColor  = ST.block(MD.HEX, UT.Code.select(aData.mColor, "blockEnergizedHexoriumMonolithRainbow", HEXORIUM_MONOLITHS)),
		tHexoriumRandom = ST.block(MD.HEX, UT.Code.select(              "blockEnergizedHexoriumMonolithRainbow", HEXORIUM_MONOLITHS));
		
		switch(aData.next(4)) {
		case 0:
			aData.set( 3, 1,  3, Blocks.ENCHANTING_TABLE);
			aData.set( 3, 1, 12, Blocks.CRAFTING_TABLE);
			aData.set(12, 1,  3, Blocks.JUKEBOX);
			aData.set(12, 1, 12, Blocks.ENDER_CHEST);
			aData.cup( 3, 2, 12, FL.Potion_NightVision_1L, aData.next1in2() ? tHexoriumColor : tHexoriumRandom, aData.next1in2() ? 1 : 9);
			break;
		case 1:
			aData.set( 3, 1,  3, Blocks.ENDER_CHEST);
			aData.set( 3, 1, 12, Blocks.ENCHANTING_TABLE);
			aData.set(12, 1,  3, Blocks.CRAFTING_TABLE);
			aData.set(12, 1, 12, Blocks.JUKEBOX);
			aData.cup(12, 2,  3, FL.Potion_NightVision_1L, aData.next1in2() ? tHexoriumColor : tHexoriumRandom, aData.next1in2() ? 1 : 9);
			break;
		case 2:
			aData.set( 3, 1,  3, Blocks.JUKEBOX);
			aData.set( 3, 1, 12, Blocks.ENDER_CHEST);
			aData.set(12, 1,  3, Blocks.ENCHANTING_TABLE);
			aData.set(12, 1, 12, Blocks.CRAFTING_TABLE);
			aData.cup(12, 2, 12, FL.Potion_NightVision_1L, aData.next1in2() ? tHexoriumColor : tHexoriumRandom, aData.next1in2() ? 1 : 9);
			break;
		case 3:
			aData.set( 3, 1,  3, Blocks.CRAFTING_TABLE);
			aData.set( 3, 1, 12, Blocks.JUKEBOX);
			aData.set(12, 1,  3, Blocks.ENDER_CHEST);
			aData.set(12, 1, 12, Blocks.ENCHANTING_TABLE);
			aData.cup( 3, 2,  3, FL.Potion_NightVision_1L, aData.next1in2() ? tHexoriumColor : tHexoriumRandom, aData.next1in2() ? 1 : 9);
			break;
		}
		
		return T;
	}
}

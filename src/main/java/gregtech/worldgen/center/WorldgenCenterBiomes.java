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
 */

package gregtech.worldgen.center;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.WorldGenLevel;

import static gregapi.data.CS.*;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.Set;

import gregapi.block.multitileentity.MultiTileEntityRegistry;
import gregapi.data.CS.BlocksGT;
import gregapi.data.IL;
import gregapi.util.ST;
import gregapi.util.WD;
import gregapi.worldgen.WorldgenObject;
import gregtech.blocks.fluids.BlockRiver;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.LevelChunk;

/**
 * @author Gregorius Techneticies
 */
public class WorldgenCenterBiomes extends WorldgenObject {
	public int mHeight = 66;
	
	@SafeVarargs
	public WorldgenCenterBiomes(String aName, boolean aDefault, List<WorldgenObject>... aLists) {
		super(aName, aDefault, aLists);
		mHeight = getConfigFile().get(mCategory, "Height", WD.waterLevel()+4);
		GENERATE_BIOMES = mEnabled;
	}
	
	@Override
	public boolean enabled(WorldGenLevel aWorld, int aDimType) {
		return GENERATE_BIOMES && WD.dimensionId(aWorld) == DIM_OVERWORLD;
	}
	
	@Override
	public void reset(WorldGenLevel aWorld, ChunkAccess aChunk, int aDimType, int aMinX, int aMinZ, int aMaxX, int aMaxZ, Random aRandom, Biome[][] aBiomes, Set<String> aBiomeNames) {
		if (GENERATE_BIOMES && aDimType == DIM_OVERWORLD && aMinX >= -96 && aMinX <= 80 && aMinZ >= -96 && aMinZ <= 80) GENERATING_SPECIAL = T;
	}
	
	@Override
	public boolean generate(WorldGenLevel aWorld, ChunkAccess aChunk, int aDimType, int aMinX, int aMinZ, int aMaxX, int aMaxZ, Random aRandom, Biome[][] aBiomes, Set<String> aBiomeNames) {
		if (aMinX >= -96 && aMinX <= 80 && aMinZ >= -96 && aMinZ <= 80) {
			if (GENERATE_STREETS && aMinX >= -32 && aMinX <= 16 && aMinZ >= -32 && aMinZ <= 16) {
				WD.setBiomes(aWorld, aChunk, net.minecraft.world.level.biome.Biomes.RIVER);
				return T;
			}
			if (GENERATE_NEXUS && aMinX == 16 && aMinZ == -48) {
				WD.setBiomes(aWorld, aChunk, net.minecraft.world.level.biome.Biomes.PLAINS); /* FORCED-ADAPTATION: биом обнулён прежним проходом (removed/modded в neo) -> PLAINS-дефолт */
				return T;
			}
			if (GENERATE_TESTING && (aMinX == 32 || aMinX == 48) && (aMinZ == -32 || aMinZ == -48)) {
				WD.setBiomes(aWorld, aChunk, net.minecraft.world.level.biome.Biomes.PLAINS); /* FORCED-ADAPTATION: биом обнулён прежним проходом (removed/modded в neo) -> PLAINS-дефолт */
				return T;
			}
			if (aMinX == -16 || aMinX == 0 || aMinZ == -16 || aMinZ == 0) {
				WD.setBiomes(aWorld, aChunk, net.minecraft.world.level.biome.Biomes.RIVER);
				BlockRiver.PLACEMENT_ALLOWED = T;
				for (int i = 0; i < 16; i++) for (int j = 0; j < 16; j++) {
					for (int k = -3; k < 64; k++) WD.set(aChunk, i, mHeight+k, j, NB, 0);
					WD.set(aChunk, i, mHeight-4, j, BlocksGT.River, 0);
					WD.set(aChunk, i, mHeight-5, j, BlocksGT.River, 0);
					WD.set(aChunk, i, mHeight-6, j, BlocksGT.River, 0);
					WD.set(aChunk, i, mHeight-7, j, BlocksGT.Sands, aMinX < 0 ? aMinZ < 0 ? 0 : 1 : aMinZ < 0 ? 2 : 0);
					WD.set(aChunk, i, mHeight-8, j, Blocks.GRAVEL, 1);
					WD.set(aChunk, i, mHeight-9, j, Blocks.CLAY, 0);
					WD.set(aChunk, i, mHeight-10, j, Blocks.CLAY, 0);
					for (int k = 1; k < mHeight-10; k++) WD.set(aChunk, i, k, j, Blocks.STONE, 1);
				}
				BlockRiver.PLACEMENT_ALLOWED = F;
				WD.setSpawnLocation(aWorld, 0, mHeight+5, 0);
				return T;
			}
			if (aMinX < 0) {
				if (aMinZ < 0) {
					if ((aMinX == -80 || aMinX == -64) && (aMinZ == -80 || aMinZ == -64)) {
						WD.setBiomes(aWorld, aChunk, net.minecraft.world.level.biome.Biomes.SNOWY_PLAINS);
						for (int i = 0; i < 16; i++) for (int j = 0; j < 16; j++) {
							for (int k = 1; k < 64; k++) WD.set(aChunk, i, mHeight+k, j, NB, 0);
							WD.set(aChunk, i, mHeight  , j, Blocks.ICE, 0);
							WD.set(aChunk, i, mHeight-1, j, Blocks.PACKED_ICE, 0);
							WD.set(aChunk, i, mHeight-2, j, Blocks.PACKED_ICE, 0);
							WD.set(aChunk, i, mHeight-3, j, Blocks.PACKED_ICE, 0);
							WD.set(aChunk, i, mHeight-4, j, Blocks.PACKED_ICE, 0);
							WD.set(aChunk, i, mHeight-5, j, Blocks.PACKED_ICE, 0);
							for (int k = 1; k < mHeight-5; k++) WD.set(aChunk, i, k, j, k < 32 ? BlocksGT.SchistGreen : BlocksGT.SchistBlue, aRandom.nextBoolean()?2:0);
						}
					} else {
						WD.setBiomes(aWorld, aChunk, net.minecraft.world.level.biome.Biomes.SNOWY_TAIGA);
						for (int i = 0; i < 16; i++) for (int j = 0; j < 16; j++) {
							for (int k = 2; k < 64; k++) WD.set(aChunk, i, mHeight+k, j, NB, 0);
							WD.set(aChunk, i, mHeight+1, j, Blocks.SNOW, aRandom.nextInt(2));
							WD.set(aChunk, i, mHeight  , j, Blocks.DIRT, 2);
							WD.set(aChunk, i, mHeight-1, j, Blocks.DIRT, 2);
							WD.set(aChunk, i, mHeight-2, j, Blocks.DIRT, 2);
							WD.set(aChunk, i, mHeight-3, j, Blocks.DIRT, 2);
							WD.set(aChunk, i, mHeight-4, j, Blocks.DIRT, 2);
							WD.set(aChunk, i, mHeight-5, j, Blocks.DIRT, 2);
							for (int k = 1; k < mHeight-5; k++) WD.set(aChunk, i, k, j, Blocks.MOSSY_COBBLESTONE, 0);
						}
						
						WD.set(aChunk,  4, mHeight+1,  4, NB, aRandom.nextInt(2));
						WD.set(aChunk, 12, mHeight+1,  4, NB, aRandom.nextInt(2));
						WD.set(aChunk,  4, mHeight+1, 12, NB, aRandom.nextInt(2));
						WD.set(aChunk, 12, mHeight+1, 12, NB, aRandom.nextInt(2));
						
						WD.placeTree(aWorld, aMinX+ 4, mHeight+1, aMinZ+ 4);
						WD.placeTree(aWorld, aMinX+12, mHeight+1, aMinZ+ 4);
						WD.placeTree(aWorld, aMinX+ 4, mHeight+1, aMinZ+12);
						WD.placeTree(aWorld, aMinX+12, mHeight+1, aMinZ+12);
					}
				} else {
					if ((aMinX == -80 || aMinX == -64) && (aMinZ == 48 || aMinZ == 64)) {
						WD.setBiomes(aWorld, aChunk, net.minecraft.world.level.biome.Biomes.FOREST);
						for (int i = 0; i < 16; i++) for (int j = 0; j < 16; j++) {
							for (int k = 1; k < 64; k++) WD.set(aChunk, i, mHeight+k, j, NB, 0);
							WD.set(aChunk, i, mHeight  , j, Blocks.GRASS_BLOCK, 0);
							WD.set(aChunk, i, mHeight-1, j, Blocks.DIRT, 0);
							WD.set(aChunk, i, mHeight-2, j, Blocks.DIRT, 0);
							WD.set(aChunk, i, mHeight-3, j, Blocks.DIRT, 0);
							WD.set(aChunk, i, mHeight-4, j, Blocks.DIRT, 0);
							WD.set(aChunk, i, mHeight-5, j, Blocks.DIRT, 0);
							for (int k = 1; k < mHeight-5; k++) WD.set(aChunk, i, k, j, k < 32 ? BlocksGT.Kimberlite : BlocksGT.Quartzite, aRandom.nextBoolean()?2:0);
						}
						WD.set(aChunk,  6, mHeight+1,  6, Blocks.PUMPKIN, 0);
						WD.set(aChunk, 10, mHeight+1,  6, Blocks.PUMPKIN, 0);
						WD.set(aChunk,  6, mHeight+1, 10, Blocks.PUMPKIN, 0);
						WD.set(aChunk, 10, mHeight+1, 10, Blocks.PUMPKIN, 0);
						
						WD.placeTree(aWorld, aMinX+ 4, mHeight+1, aMinZ+ 4);
						WD.placeTree(aWorld, aMinX+12, mHeight+1, aMinZ+ 4);
						WD.placeTree(aWorld, aMinX+ 4, mHeight+1, aMinZ+12);
						WD.placeTree(aWorld, aMinX+12, mHeight+1, aMinZ+12);
					} else {
						MultiTileEntityRegistry tRegistry = MultiTileEntityRegistry.getRegistry("gt.multitileentity");
						WD.setBiomes(aWorld, aChunk, net.minecraft.world.level.biome.Biomes.PLAINS); /* FORCED-ADAPTATION: биом обнулён прежним проходом (removed/modded в neo) -> PLAINS-дефолт */
						for (int i = 0; i < 16; i++) for (int j = 0; j < 16; j++) {
							for (int k = 1; k < 64; k++) WD.set(aChunk, i, mHeight+k, j, NB, 0);
							WD.set(aChunk, i, mHeight  , j, Blocks.GRASS_BLOCK, 0);
							WD.set(aChunk, i, mHeight-1, j, Blocks.DIRT, 0);
							WD.set(aChunk, i, mHeight-2, j, BlocksGT.Diggables, 1);
							WD.set(aChunk, i, mHeight-3, j, BlocksGT.Diggables, 3);
							WD.set(aChunk, i, mHeight-4, j, BlocksGT.Diggables, 4);
							WD.set(aChunk, i, mHeight-5, j, BlocksGT.Diggables, 5);
							WD.set(aChunk, i, mHeight-6, j, BlocksGT.Diggables, 6);
							for (int k = 1; k < mHeight-6; k++) WD.set(aChunk, i, k, j, k < 32 ? BlocksGT.Limestone : BlocksGT.Marble, aRandom.nextBoolean()?2:0);
							switch(aRandom.nextInt(60)) {
							case  0: case  1: case  2: if (tRegistry != null) tRegistry.mBlock.placeBlock(aWorld, aMinX+i, mHeight+1, aMinZ+j, SIDE_UNKNOWN, (short)32757, ST.save(NBT_VALUE, ST.make(Items.FLINT, 1, 0)), F, T); break;
							case  3: case  4: case  5: if (tRegistry != null) tRegistry.mBlock.placeBlock(aWorld, aMinX+i, mHeight+1, aMinZ+j, SIDE_UNKNOWN, (short)32757, null, F, T); break;
							case  6: case  7: case  8: if (tRegistry != null) tRegistry.mBlock.placeBlock(aWorld, aMinX+i, mHeight+1, aMinZ+j, SIDE_UNKNOWN, (short)32756, null, F, T); break;
							case  9: case 10: case 11: case 12: case 13: case 14: WD.set(aChunk, i, mHeight+1, j, Blocks.DEAD_BUSH, 1); break;
							case 15: WD.set(aChunk, i, mHeight+1, j, Blocks.DANDELION, 0); break;
							case 16: WD.set(aChunk, i, mHeight+1, j, Blocks.POPPY, 0); break;
							case 17: WD.set(aChunk, i, mHeight+1, j, Blocks.POPPY, 1); break;
							case 18: WD.set(aChunk, i, mHeight+1, j, Blocks.POPPY, 2); break;
							case 19: WD.set(aChunk, i, mHeight+1, j, Blocks.POPPY, 3); break;
							case 20: WD.set(aChunk, i, mHeight+1, j, Blocks.POPPY, 4); break;
							case 21: WD.set(aChunk, i, mHeight+1, j, Blocks.POPPY, 5); break;
							case 22: WD.set(aChunk, i, mHeight+1, j, Blocks.POPPY, 6); break;
							case 23: WD.set(aChunk, i, mHeight+1, j, Blocks.POPPY, 7); break;
							case 24: WD.set(aChunk, i, mHeight+1, j, Blocks.POPPY, 8); break;
							}
						}
					}
				}
			} else {
				if (aMinZ < 0) {
					if ((aMinX == 48 || aMinX == 64) && (aMinZ == -80 || aMinZ == -64)) {
						WD.setBiomes(aWorld, aChunk, net.minecraft.world.level.biome.Biomes.BADLANDS);
						for (int i = 0; i < 16; i++) for (int j = 0; j < 16; j++) {
							for (int k = 1; k < 64; k++) WD.set(aChunk, i, mHeight+k, j, NB, 0);
							WD.set(aChunk, i, mHeight  , j, Blocks.SAND, 1);
							WD.set(aChunk, i, mHeight-1, j, Blocks.SAND, 1);
							WD.set(aChunk, i, mHeight-2, j, Blocks.SAND, 1);
							WD.set(aChunk, i, mHeight-3, j, Blocks.SAND, 1);
							WD.set(aChunk, i, mHeight-4, j, Blocks.SAND, 1);
							WD.set(aChunk, i, mHeight-5, j, Blocks.SAND, 1);
							for (int k = 1; k < mHeight-5; k++) WD.set(aChunk, i, k, j, Blocks.TERRACOTTA, 0);
						}
						for (int i = 1; i <= 3; i++) {
							WD.set(aChunk,  4, mHeight+i,  4, Blocks.CACTUS, 0);
							WD.set(aChunk, 12, mHeight+i,  4, Blocks.CACTUS, 0);
							WD.set(aChunk,  4, mHeight+i, 12, Blocks.CACTUS, 0);
							WD.set(aChunk, 12, mHeight+i, 12, Blocks.CACTUS, 0);
						}
					} else {
						WD.setBiomes(aWorld, aChunk, net.minecraft.world.level.biome.Biomes.DESERT);
						for (int i = 0; i < 16; i++) for (int j = 0; j < 16; j++) {
							for (int k = 1; k < 64; k++) WD.set(aChunk, i, mHeight+k, j, NB, 0);
							WD.set(aChunk, i, mHeight  , j, Blocks.SAND, 0);
							WD.set(aChunk, i, mHeight-1, j, Blocks.SAND, 0);
							WD.set(aChunk, i, mHeight-2, j, Blocks.SAND, 0);
							WD.set(aChunk, i, mHeight-3, j, Blocks.SAND, 0);
							WD.set(aChunk, i, mHeight-4, j, Blocks.SAND, 0);
							WD.set(aChunk, i, mHeight-5, j, Blocks.SAND, 0);
							for (int k = 1; k < mHeight-5; k++) WD.set(aChunk, i, k, j, Blocks.SANDSTONE, 0);
						}
					}
				} else {
					if ((aMinX == 48 || aMinX == 64) && (aMinZ == 48 || aMinZ == 64)) {
						WD.setBiomes(aWorld, aChunk, net.minecraft.world.level.biome.Biomes.SWAMP);
						for (int i = 0; i < 16; i++) for (int j = 0; j < 16; j++) {
							for (int k = 1; k < 64; k++) WD.set(aChunk, i, mHeight+k, j, NB, 0);
							WD.set(aChunk, i, mHeight  , j, Blocks.WATER, 0);
							WD.set(aChunk, i, mHeight-1, j, BlocksGT.Diggables, 0);
							WD.set(aChunk, i, mHeight-2, j, BlocksGT.Diggables, 0);
							WD.set(aChunk, i, mHeight-3, j, BlocksGT.Diggables, 2);
							WD.set(aChunk, i, mHeight-4, j, BlocksGT.Diggables, 2);
							WD.set(aChunk, i, mHeight-5, j, BlocksGT.Diggables, 2);
							for (int k = 1; k < mHeight-5; k++) WD.set(aChunk, i, k, j, k < 32 ? BlocksGT.GraniteRed : BlocksGT.GraniteBlack, aRandom.nextBoolean()?2:0);
							if (aRandom.nextInt(8) == 0) WD.set(aChunk, i, mHeight+1, j, BlocksGT.Glowtus, aRandom.nextInt(16));
						}
						WD.set(aChunk,  4, mHeight+1,  4, Blocks.LILY_PAD, 0);
						WD.set(aChunk, 12, mHeight+1,  4, Blocks.LILY_PAD, 0);
						WD.set(aChunk,  4, mHeight+1, 12, Blocks.LILY_PAD, 0);
						WD.set(aChunk, 12, mHeight+1, 12, Blocks.LILY_PAD, 0);
					} else {
						WD.setBiomes(aWorld, aChunk, net.minecraft.world.level.biome.Biomes.JUNGLE);
						if (IL.EtFu_Dirt.exists()) {
							Block tBlock = IL.EtFu_Dirt.block();
							for (int i = 0; i < 16; i++) for (int j = 0; j < 16; j++) {
								for (int k = 1; k < 64; k++) WD.set(aChunk, i, mHeight+k, j, NB, 0);
								WD.set(aChunk, i, mHeight  , j, Blocks.GRASS_BLOCK, 0);
								WD.set(aChunk, i, mHeight-1, j, tBlock, 0);
								WD.set(aChunk, i, mHeight-2, j, tBlock, 0);
								WD.set(aChunk, i, mHeight-3, j, tBlock, 0);
								WD.set(aChunk, i, mHeight-4, j, tBlock, 0);
								WD.set(aChunk, i, mHeight-5, j, tBlock, 0);
								for (int k = 1; k < mHeight-5; k++) WD.set(aChunk, i, k, j, k < 32 ? BlocksGT.Komatiite : BlocksGT.Basalt, aRandom.nextBoolean()?2:0);
							}
						} else {
							for (int i = 0; i < 16; i++) for (int j = 0; j < 16; j++) {
								for (int k = 1; k < 64; k++) WD.set(aChunk, i, mHeight+k, j, NB, 0);
								WD.set(aChunk, i, mHeight  , j, Blocks.GRASS_BLOCK, 0);
								WD.set(aChunk, i, mHeight-1, j, Blocks.DIRT, 1);
								WD.set(aChunk, i, mHeight-2, j, Blocks.DIRT, 1);
								WD.set(aChunk, i, mHeight-3, j, Blocks.DIRT, 1);
								WD.set(aChunk, i, mHeight-4, j, Blocks.DIRT, 1);
								WD.set(aChunk, i, mHeight-5, j, Blocks.DIRT, 1);
								for (int k = 1; k < mHeight-5; k++) WD.set(aChunk, i, k, j, k < 32 ? BlocksGT.Komatiite : BlocksGT.Basalt, aRandom.nextBoolean()?2:0);
							}
						}
						WD.set(aChunk, 6+aRandom.nextInt(4), mHeight+1, 6+aRandom.nextInt(4), Blocks.MELON, 0);
						
						WD.placeTree(aWorld, aMinX+ 4, mHeight+1, aMinZ+ 4);
						WD.placeTree(aWorld, aMinX+12, mHeight+1, aMinZ+ 4);
						WD.placeTree(aWorld, aMinX+ 4, mHeight+1, aMinZ+12);
						WD.placeTree(aWorld, aMinX+12, mHeight+1, aMinZ+12);
					}
				}
			}
			return T;
		}
		return F;
	}
}

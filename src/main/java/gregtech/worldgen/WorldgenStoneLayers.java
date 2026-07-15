/**
 * Copyright (c) 2025 GregTech-6 Team
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

package gregtech.worldgen;

import gregapi.block.metatype.BlockStones;
import gregapi.block.multitileentity.MultiTileEntityRegistry;
import gregapi.data.MT;
import gregapi.data.OP;
import gregapi.oredict.OreDictMaterial;
import gregapi.util.ST;
import gregapi.util.UT;
import gregapi.util.WD;
import gregapi.worldgen.StoneLayer;
import gregapi.worldgen.StoneLayerOres;
import gregapi.worldgen.WorldgenObject;
import net.minecraft.world.level.block.Block;
import gregapi.block.Material;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;

import java.util.List;
import java.util.Random;
import java.util.Set;

import static gregapi.data.CS.*;

/**
 * @author Gregorius Techneticies
 */
public class WorldgenStoneLayers extends WorldgenObject {
	@SafeVarargs
	public WorldgenStoneLayers(String aName, boolean aDefault, List<WorldgenObject>... aLists) {
		super(aName, aDefault, aLists);
		GENERATE_STONE = mEnabled;
	}
	
	@Override
	public boolean generate(Level aWorld, LevelChunk aChunk, int aDimType, int aMinX, int aMinZ, int aMaxX, int aMaxZ, Random aRandom, Biome[][] aBiomes, Set<String> aBiomeNames) {
		if (GENERATING_SPECIAL) return F;
		
		//final boolean tSlime = (aChunk.getRandomWithSeed(987234911L).nextInt(10) == 0);
		final NoiseGenerator tNoise = new NoiseGenerator(aWorld);
		final LevelChunkSection[] aStorages = aChunk.getSections();
		final int tListSize = StoneLayer.LAYERS.size(), tMaxHeight = aChunk.getTopFilledSegment()+15;
		final StoneLayer[] tScan = new StoneLayer[7];
		final byte tScanMinusOne = (byte)(tScan.length-1);
		
		MultiTileEntityRegistry tRegistry = MultiTileEntityRegistry.getRegistry("gt.multitileentity");
		Block tLastReplaced = Blocks.STONE;
		
		for (int i = 0; i < 16; i++) for (int j = 0; j < 16; j++) {
			final int tX = aMinX+i, tZ = aMinZ+j;
			final Biome aBiome = aBiomes[i][j];
			
			for (int k = 0; k < tScan.length; k++) {
				tScan[k] = (StoneLayer.LAYERS.get(tNoise.get(tX, k-2, tZ, tListSize)));
				if (tScan[k].mNoDeep) tScan[k] = StoneLayer.DEEPSLATE;
			}
			
			boolean tCanPlaceRocks = F;
			OreDictMaterial tLastRock = MT.STONES.Deepslate, tLastOre = null;
			
			for (int tY = 1; tY < tMaxHeight; tY++) {
				final LevelChunkSection aStorage = aStorages[tY >> 4];
				final Block aBlock = (aStorage == null ? NB : aStorage.getBlockState(i, tY & 15, j).getBlock());
				assert aStorage != null;
				// Just mark as Opaque Ground.
				if (aBlock == Blocks.BEDROCK) {
					tCanPlaceRocks = T;
				// Place Rock if on Opaque Surface.
				} else if (aBlock == NB) {
					if (tCanPlaceRocks && aRandom.nextInt(128) == 0) tRegistry.mBlock.placeBlock(aWorld, tX, tY, tZ, SIDE_UNKNOWN, (short)32757, ST.save(NBT_VALUE, OP.rockGt.mat(aRandom.nextBoolean()&&tLastOre!=null?tLastOre.mTargetCrushing.mMaterial:tLastRock, 1)), F, T);
					tLastOre = null;
					tCanPlaceRocks = F;
				// Stone and Ore Generation in vanilla Stone.
				} else if (aBlock == Blocks.STONE || (aBlock == Blocks.INFESTED_STONE && aStorage.getExtBlockMetadata(i, tY & 15, j) == 0)) {
					tCanPlaceRocks = T;
					boolean temp = T;
					if (tScan[5] == tScan[1]) {
						for (StoneLayerOres tOres : tScan[3].mOres) if (tOres.mMaterial.mID > 0 && tOres.check(tScan[3], aWorld, tX, tY, tZ, aBiome, aRandom) && (tScan[6] == tScan[0] ? tOres.normal(tScan[3], aWorld, tX, tY, tZ, aBiome) : tOres.small(tScan[3], aWorld, tX, tY, tZ, aBiome))) {
							if (tOres.mGenerateIndicators) tLastOre = tOres.mMaterial;
							temp = F;
							break;
						}
					} else {
						for (StoneLayerOres tOres : StoneLayer.get(tScan[5], tScan[1])) if (tOres.mMaterial.mID > 0 && tOres.check(tScan[3], aWorld, tX, tY, tZ, aBiome, aRandom) && tOres.set(tScan[3], aWorld, tX, tY, tZ, aBiome, aRandom)) {
							if (tOres.mGenerateIndicators) tLastOre = tOres.mMaterial;
							temp = F;
							break;
						}
					}
					if (temp && tScan[4] != tScan[2] && tScan[3].mOreSmall != null && !StoneLayer.RANDOM_SMALL_GEM_ORES.isEmpty() && aRandom.nextInt(100) == 0) {
						if (tScan[3].mOreSmall.placeBlock(aWorld, tX, tY, tZ, SIDE_UNKNOWN, UT.Code.select(MT.Emerald, StoneLayer.RANDOM_SMALL_GEM_ORES).mID, null, F, T)) {
							temp = F;
						}
					}
					if (temp) {
						tLastRock = tScan[3].mMaterialSurface;
						if (aBlock != tScan[3].mStone) {
							WD.set(aChunk, i, tY, j, tScan[3].mStone, tScan[3].mMetaStone);
						}
					}
				// Cobblestone Generation.
				} else if (aBlock == Blocks.COBBLESTONE) {
					tCanPlaceRocks = T;
					if (tScan[3].mCobble != null) {
						tLastRock = tScan[3].mMaterialSurface;
						if (aBlock != tScan[3].mCobble) {
							WD.set(aChunk, i, tY, j, tScan[3].mCobble, tScan[3].mMetaCobble);
						}
					}
				// Mossy Cobblestone Generation.
				} else if (aBlock == Blocks.MOSSY_COBBLESTONE) {
					tCanPlaceRocks = T;
					if (tScan[3].mMossy != null) {
						tLastRock = tScan[3].mMaterialSurface;
						if (aBlock != tScan[3].mMossy) {
							WD.set(aChunk, i, tY, j, tScan[3].mMossy, tScan[3].mMetaMossy);
						}
					}
				// Check for the GT6 Stone being natural. Unlikely case due to GT6 Stone being the thing that is supposed to generate this very moment and not before. But Villages would otherwise see their House Materials replaed.
				} else if (aBlock instanceof BlockStones) {
					tCanPlaceRocks = (aStorage.getExtBlockMetadata(i, tY & 15, j) < 3);
				// Stone and Ore Generation in replaceable Blocks.
				} else if (aBlock == tLastReplaced || StoneLayer.REPLACEABLE_BLOCKS.contains(aBlock)) {
					tLastReplaced = aBlock;
					tCanPlaceRocks = T;
					boolean temp = T;
					if (tScan[5] == tScan[1]) {
						for (StoneLayerOres tOres : tScan[3].mOres) if (tOres.mMaterial.mID > 0 && tOres.check(tScan[3], aWorld, tX, tY, tZ, aBiome, aRandom) && (tScan[6] == tScan[0] ? tOres.normal(tScan[3], aWorld, tX, tY, tZ, aBiome) : tOres.small(tScan[3], aWorld, tX, tY, tZ, aBiome))) {
							if (tOres.mGenerateIndicators) tLastOre = tOres.mMaterial;
							temp = F;
							break;
						}
					} else {
						for (StoneLayerOres tOres : StoneLayer.get(tScan[5], tScan[1])) if (tOres.mMaterial.mID > 0 && tOres.check(tScan[3], aWorld, tX, tY, tZ, aBiome, aRandom) && tOres.set(tScan[3], aWorld, tX, tY, tZ, aBiome, aRandom)) {
							if (tOres.mGenerateIndicators) tLastOre = tOres.mMaterial;
							temp = F;
							break;
						}
					}
					if (temp && tScan[4] != tScan[2] && tScan[3].mOreSmall != null && !StoneLayer.RANDOM_SMALL_GEM_ORES.isEmpty() && aRandom.nextInt(100) == 0) {
						if (tScan[3].mOreSmall.placeBlock(aWorld, tX, tY, tZ, SIDE_UNKNOWN, UT.Code.select(MT.Emerald, StoneLayer.RANDOM_SMALL_GEM_ORES).mID, null, F, T)) {
							temp = F;
						}
					}
					if (temp) {
						tLastRock = tScan[3].mMaterialSurface;
						if (aBlock != tScan[3].mStone) {
							WD.set(aChunk, i, tY, j, tScan[3].mStone, tScan[3].mMetaStone);
						}
					}
				// Place Rock if on Opaque Surface.
				} else if (WD.easyRep(aWorld, tX, tY, tZ, aBlock)) {
					if (tCanPlaceRocks && !WD.getMaterial(aBlock).isLiquid() && aRandom.nextInt(128) == 0) tRegistry.mBlock.placeBlock(aWorld, tX, tY, tZ, SIDE_UNKNOWN, (short)32757, ST.save(NBT_VALUE, OP.rockGt.mat(aRandom.nextBoolean()&&tLastOre!=null?tLastOre.mTargetCrushing.mMaterial:tLastRock, 1)), F, T);
					tLastOre = null;
					tCanPlaceRocks = F;
				// Just check if the last Block was Opaque and of the right kind of Material.
				} else {
					if (WD.opaque(aBlock)) {
						tCanPlaceRocks = (WD.getMaterial(aBlock) == Material.clay || WD.getMaterial(aBlock) == Material.sand || WD.getMaterial(aBlock) == Material.grass || WD.getMaterial(aBlock) == Material.ground);
					} else {
						tLastOre = null;
						tCanPlaceRocks = F;
					}
				}
				
				// And scan for next Block on the Stone Layer Type.
				for (int t = 0; t < tScanMinusOne; t++) tScan[t] = tScan[t+1];
				tScan[tScanMinusOne] = StoneLayer.LAYERS.get(tNoise.get(tX, tY-2+tScanMinusOne, tZ, tListSize));
				// Ores that should not generate too deeply will be replaced by (Deep)Slate. This prevents flammable Ores near Lava in most cases.
				if (tY-2+tScanMinusOne < 24 && tScan[tScanMinusOne].mNoDeep) tScan[tScanMinusOne] = StoneLayer.DEEPSLATE;
			}
		}
		return T;
	}
	
	@Override public boolean enabled(Level aWorld, int aDimType) {return GENERATE_STONE;}
}

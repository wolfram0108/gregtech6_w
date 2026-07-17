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

package gregtech.worldgen;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.WorldGenLevel;

import static gregapi.data.CS.*;

import java.util.List;
import java.util.Random;
import java.util.Set;

import gregapi.data.CS.BlocksGT;
import gregapi.util.WD;
import gregapi.worldgen.WorldgenObject;
import gregtech.blocks.fluids.BlockOcean;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;

/**
 * @author Gregorius Techneticies
 */
public class WorldgenOcean extends WorldgenObject {
	public int mHeight = WD.waterLevel();
	
	@SafeVarargs
	public WorldgenOcean(String aName, boolean aDefault, List<WorldgenObject>... aLists) {
		super(aName, aDefault, aLists);
		mHeight = getConfigFile().get(mCategory, "Height", mHeight);
	}
	
	@Override
	public boolean generate(WorldGenLevel aWorld, ChunkAccess aChunk, int aDimType, int aMinX, int aMinZ, int aMaxX, int aMaxZ, Random aRandom, Biome[][] aBiomes, Set<String> aBiomeNames) {
		boolean temp = T;
		for (String tName : aBiomeNames) if (BIOMES_OCEAN.contains(tName)) {temp = F; break;}
		if (temp) return F;
		// F6-Y-scale: старт скана = реальная поверхность воды (MC26 море getSeaLevel()=63, GT6-дефолт waterLevel=62 → иначе верхний слой не замещался); нижняя граница minY (был жёсткий 0); индекс секции getSectionIndex (getSections()[0]=мин-секция при min-Y!=0), НЕ tY>>4 (эталон WorldgenStoneLayers d6dc0f2d).
		int tHeight = Math.max(WD.waterLevel(aWorld, mHeight), aWorld.getSeaLevel());
		final LevelChunkSection[] tStorages = aChunk.getSections();
		final int tMinY = WD.minY(aWorld);
		for (int tX = 0; tX < 16; tX++) for (int tZ = 0; tZ < 16; tZ++) {
			boolean tPlacedNone = T;
			for (int tY = tHeight; tY > tMinY; tY--) {
				final int tSectionIndex = aChunk.getSectionIndex(tY);
				final LevelChunkSection tStorage = (tSectionIndex >= 0 && tSectionIndex < tStorages.length) ? tStorages[tSectionIndex] : null;
				if (tStorage == null) continue;
				final Block tBlock = tStorage.getBlockState(tX, tY & 15, tZ).getBlock();
				if (WD.opaque(tBlock)) break;
				if (tBlock != Blocks.WATER && tBlock != Blocks.WATER) continue;
				
				if (tPlacedNone) {
					tPlacedNone = F;
					BlockOcean.UPDATE_TICK = (aBiomeNames.size() > 1);
					BlockOcean.PLACEMENT_ALLOWED = T;
					if (!WD.set(aWorld, aMinX+tX, tY, aMinZ+tZ, BlocksGT.Ocean, 0, 0)) {
						WD.set(aWorld, aMinX+tX, tY, aMinZ+tZ, Blocks.WATER, 0, 0);
						aChunk.markUnsaved();
						return F;
					}
					BlockOcean.PLACEMENT_ALLOWED = F;
				} else {
					tStorage.setBlockState(tX, tY & 15, tZ, BlocksGT.Ocean.defaultBlockState());
				}
				temp = T;
			}
		}
		return temp;
	}
}

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
import gregtech.blocks.fluids.BlockSwamp;
import gregtech.blocks.fluids.BlockWaterlike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;

/**
 * @author Gregorius Techneticies
 */
public class WorldgenSwamp extends WorldgenObject {
	public int mHeight = WD.waterLevel();
	
	@SafeVarargs
	public WorldgenSwamp(String aName, boolean aDefault, List<WorldgenObject>... aLists) {
		super(aName, aDefault, aLists);
		mHeight = getConfigFile().get(mCategory, "Height", mHeight);
	}
	
	@Override
	public boolean generate(WorldGenLevel aWorld, ChunkAccess aChunk, int aDimType, int aMinX, int aMinZ, int aMaxX, int aMaxZ, Random aRandom, Biome[][] aBiomes, Set<String> aBiomeNames) {
		boolean temp = T;
		for (String tName : aBiomeNames) if (BIOMES_SWAMP.contains(tName)) {temp = F; break;}
		if (temp) return F;
		// F6-Y-scale: старт скана = реальная поверхность воды (getSeaLevel), нижняя граница minY, индекс секции getSectionIndex (эталон WorldgenStoneLayers d6dc0f2d).
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
				if (tBlock != Blocks.WATER && tBlock != Blocks.WATER && !(tBlock instanceof BlockWaterlike && BIOMES_SWAMP.contains(aBiomes[tX][tZ]))) continue;
				
				if (tPlacedNone) {
					tPlacedNone = F;
					
					BlockSwamp.PLACEMENT_ALLOWED = T;
					if (!WD.set(aWorld, aMinX+tX, tY, aMinZ+tZ, BlocksGT.Swamp, 0, 0)) {
						WD.set(aWorld, aMinX+tX, tY, aMinZ+tZ, Blocks.WATER, 0, 0);
						aChunk.markUnsaved();
						return F;
					}
					BlockSwamp.PLACEMENT_ALLOWED = F;
					// Стартовый тик 1:1 onBlockAdded (10+rand(90)): в 1.7.10 populate шёл по живому миру и
					// onBlockAdded планировал его сам; neo прото-чанк колбэков не даёт → планируем явно
					// (тик персистится в ProtoChunkTicks). Отсюда каскад вниз (updateTick тикает aY-1) —
					// конверсия грязи болотом и вся над-логика вод оживают без соседского события.
					aWorld.scheduleTick(new net.minecraft.core.BlockPos(aMinX+tX, tY, aMinZ+tZ), BlocksGT.Swamp, 10+RNGSUS.nextInt(90));
				} else {
					tStorage.setBlockState(tX, tY & 15, tZ, BlocksGT.Swamp.defaultBlockState());
				}
				temp = T;
			}
		}
		return temp;
	}
}

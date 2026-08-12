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
 *
 * Modified in 2026 for the GregTech 6 NeoForge port
 * (https://github.com/wolfram0108/gregtech6_w): ported from Minecraft 1.7.10 / Forge
 * to Minecraft 26.1.2 / NeoForge.
 */

package gregtech.worldgen;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.WorldGenLevel;

import gregapi.util.WD;
import gregapi.worldgen.WorldgenObject;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import gregapi.block.Material;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.List;
import java.util.Random;
import java.util.Set;

import static gregapi.data.CS.*;

/**
 * @author Gregorius Techneticies
 */
public class WorldgenBlackSand extends WorldgenObject {
	@SafeVarargs
	public WorldgenBlackSand(String aName, boolean aDefault, List<WorldgenObject>... aLists) {
		super(aName, aDefault, aLists);
	}
	
	@Override
	public boolean generate(WorldGenLevel aWorld, ChunkAccess aChunk, int aDimType, int aMinX, int aMinZ, int aMaxX, int aMaxZ, Random aRandom, Biome[][] aBiomes, Set<String> aBiomeNames) {
		if (aRandom.nextInt(64) > 0 || checkForMajorWorldgen(aWorld, aMinX, aMinZ, aMaxX, aMaxZ)) return F;
		for (String tName : aBiomeNames) if (BIOMES_OCEAN_BEACH.contains(tName) || BIOMES_SWAMP.contains(tName)) return F;
		boolean temp = T;
		for (String tName : aBiomeNames) if (BIOMES_RIVER.contains(tName)) {temp = F; break;}
		if (temp) return F;
		
		int tX = aMinX-16, tZ = aMinZ-16, tUpperBound = WD.waterLevel(aWorld)+1, tLowerBound = WD.waterLevel(aWorld)-12, aMeta = new NoiseGenerator(aWorld).get(aMinX/4, 360, aMinZ/4, 3);
		for (int i = 0; i < 48; i++) for (int j = 0; j < 48; j++) if (WorldgenPit.SHAPE[i][j]) {
			Block tBlock = NB, tLastBlock = WD.block(aWorld, tX+i, tUpperBound+1, tZ+j);
			for (int tY = tUpperBound, tGenerated = 0; tY >= tLowerBound && tGenerated < 2; tY--, tLastBlock = tBlock) {
				tBlock = WD.block(aWorld, tX+i, tY, tZ+j);
				byte tMeta = WD.meta(aWorld, tX+i, tY, tZ+j);
				if (tBlock == BlocksGT.Sands && tMeta == aMeta) {tGenerated++; continue;}
				// F6: было `!tBlock.isOpaqueCube()` (1.7.10 Block-уровневый метод, удалён движком). 1:1-замена —
				// `BlockState.isSolidRender(aWorld, tPos)` (объявление `BlockBehaviour.java:654`, поле `solidRender` там же
				// строится как "occlusion shape — полный куб" (`BlockBehaviour.java:499`) — тот же смысл, что у
				// старого isOpaqueCube: цельный непрозрачный для рендера/освещения блок). Старый метод был
				// block-уровневым, без учёта состояния/меты — берём `defaultBlockState()` (тоже без учёта меты,
				// как и раньше).
				if (!tBlock.defaultBlockState().isSolidRender(aWorld, new net.minecraft.core.BlockPos(tX+i, tY, tZ+j))) {if (tGenerated > 0) break; continue;}
				// F6: `Blocks.DIRT/gravel/sand/clay` — старые 1.7.10-имена полей (нижний регистр); реальные
				// поля neo — `Blocks.DIRT/GRAVEL/SAND/CLAY` (`Blocks.java:85,322,342,2099`), то же переименование,
				// что применено волной 1 по всему остальному дереву.
				if ((tBlock == Blocks.DIRT || tBlock == Blocks.COARSE_DIRT) || tBlock == Blocks.GRAVEL || tBlock == Blocks.SAND || tBlock == Blocks.CLAY || tBlock == BlocksGT.oreSmallGravel || tBlock == BlocksGT.oreGravel || tBlock == BlocksGT.oreSmallSand || tBlock == BlocksGT.oreSand || tBlock == BlocksGT.oreSmallRedSand || tBlock == BlocksGT.oreRedSand) {
					// F6 (1:1): не забирать дёрн под деревьями/кустами/растениями. WD.getMaterial(Block) РЕАЛИЗОВАН
					// (vanilla-классификация, WD.java:474) — стух-тег снят.
					if (tGenerated <= 0 && (WD.getMaterial(tLastBlock) == gregapi.block.Material.wood || WD.getMaterial(tLastBlock) == gregapi.block.Material.gourd)) continue;
				} else {
					if (tGenerated > 0) {
						// F6 (1:1): не сверлить второй слой сквозь неопознанный НЕ-камень. WD.getMaterial реализован — стух-тег снят.
						if (WD.getMaterial(tBlock) != gregapi.block.Material.rock) break;
					} else {
						continue;
					}
				}
				// F6 (1:1 оригинала :72 `aWorld.setBlock(tX+i, tY, tZ+j, BlocksGT.Sands, aMeta, 3)`): ставим ЧЕРЕЗ ЦЕНТР
				// WD.set(...,Block,meta,flags) — он и доставляет мету в BlockState (BlockBaseMeta несёт её свойством
				// META, BlockBaseMeta:47-48, и реализует IBlockExtendedMetaData). Тем же вызовом ставит этот же блок
				// сосед по ворлдгену — WorldgenCenterBiomes:91. Прежний прямой setBlock(defaultBlockState()) шёл мимо
				// центра и ронял мету в 0: из трёх вариантов (0 Magnetite / 1 BasalticMineralSand / 2 GraniticMineralSand)
				// генерировался только первый, хотя aMeta считается шумом выше (:57) и читается при сверке (:63).
				WD.set(aWorld, tX+i, tY, tZ+j, BlocksGT.Sands, aMeta, 3);
				tGenerated++;
			}
		}
		return temp;
	}
}

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

package gregtech.worldgen;

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
	public boolean generate(Level aWorld, LevelChunk aChunk, int aDimType, int aMinX, int aMinZ, int aMaxX, int aMaxZ, Random aRandom, Biome[][] aBiomes, Set<String> aBiomeNames) {
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
				// `BlockState.isSolidRender()` (объявление `BlockBehaviour.java:654`, поле `solidRender` там же
				// строится как "occlusion shape — полный куб" (`BlockBehaviour.java:499`) — тот же смысл, что у
				// старого isOpaqueCube: цельный непрозрачный для рендера/освещения блок). Старый метод был
				// block-уровневым, без учёта состояния/меты — берём `defaultBlockState()` (тоже без учёта меты,
				// как и раньше).
				if (!tBlock.defaultBlockState().isSolidRender()) {if (tGenerated > 0) break; continue;}
				// F6: `Blocks.dirt/gravel/sand/clay` — старые 1.7.10-имена полей (нижний регистр); реальные
				// поля neo — `Blocks.DIRT/GRAVEL/SAND/CLAY` (`Blocks.java:85,322,342,2099`), то же переименование,
				// что применено волной 1 по всему остальному дереву.
				if ((tBlock == Blocks.DIRT && tMeta < 2) || tBlock == Blocks.GRAVEL || tBlock == Blocks.SAND || tBlock == Blocks.CLAY || tBlock == BlocksGT.oreSmallGravel || tBlock == BlocksGT.oreGravel || tBlock == BlocksGT.oreSmallSand || tBlock == BlocksGT.oreSand || tBlock == BlocksGT.oreSmallRedSand || tBlock == BlocksGT.oreRedSand) {
					// PORT-TODO(F6, block-behavior: getMaterial()==wood/gourd): было "Don't take away the Dirt
					// Block below Trees, Bushes and other Plants" — `if (tGenerated<=0 && (tLastBlock.getMaterial()
					// == Material.wood || == Material.gourd)) continue;`. `Block.getMaterial()` удалён движком, F9
					// block-material shim ещё не готов (та же болезнь класса, что уже задокументирована в
					// `WorldgenOresLarge.java` §C5, метка `F6, block-behavior: getMaterial()==grass/ground/sand/
					// rock`) — не выдумываю замену. Гейт временно ОПУЩЕН (не проверяется): реже, чем в оригинале,
					// сохраняется дёрн под деревом/кустом; сама генерация чёрного песка (эта ветка) не отключена.
				} else {
					if (tGenerated > 0) {
						// PORT-TODO(F6, block-behavior: getMaterial()!=rock): было `if (tBlock.getMaterial() !=
						// Material.rock) break;` (не сверлить второй слой сквозь неопознанный НЕ-камень).
						// `Block.getMaterial()` удалён, F9 ещё не готов (см. PORT-TODO выше) — гейт временно
						// ОПУЩЕН: второй слой чёрного песка ставится независимо от материала нижнего блока.
					} else {
						continue;
					}
				}
				// F6: было `aWorld.setBlock(x,y,z,Block,int meta,int flags)` (1.7.10-сигнатура, удалена движком).
				// Реальный neo — `Level.setBlock(BlockPos,BlockState,int flags)` (`Level.java:217`). PORT-TODO(F6,
				// block-material: BlockBaseMeta→BlockState): `BlocksGT.Sands` — метаданные-блок (`BlockSands
				// extends BlockBaseMeta`, 3 варианта: Magnetite/BasalticMineralSand/GraniticMineralSand по мете
				// 0/1/2), но meta→BlockState-Property шим для `BlockBaseMeta` в дереве ещё не существует (grep
				// `IntegerProperty`/`BlockStateProperties` по `gregapi/block` = 0 совпадений) — не выдумываю.
				// Временно `defaultBlockState()` (вариант меты 0 = Magnetite) вместо `aMeta`-варианта — теряется
				// выбор конкретного варианта чёрного песка (честно отложено; сам факт размещения BlocksGT.Sands и
				// алгоритм формы/высоты пита не тронуты).
				aWorld.setBlock(new BlockPos(tX+i, tY, tZ+j), BlocksGT.Sands.defaultBlockState(), 3);
				tGenerated++;
			}
		}
		return temp;
	}
}

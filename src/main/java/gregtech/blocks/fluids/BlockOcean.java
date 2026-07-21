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

package gregtech.blocks.fluids;

import gregapi.code.ArrayListNoNulls;
import gregapi.util.WD;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.material.Fluid;

import java.util.Random;

import static gregapi.data.CS.*;

/**
 * @author Gregorius Techneticies
 *
 * F5 форс движка (decisions/F5-fluids.md §5): движковые хуки (onBlockAdded/onNeighborBlockChange/updateTick)
 * больше НЕ реальные {@code @Override} — neo {@code Block}-тик-точка иная сигнатура
 * (BlockBehaviour.tick(BlockState,ServerLevel,BlockPos,RandomSource)), рантайм-мост — F3 (как и у
 * {@link gregapi.block.BlockBase#updateTick}, тот же приём: `// @Override` вместо `@Override`).
 */
public class BlockOcean extends BlockWaterlike {
	public static boolean PLACEMENT_ALLOWED = F, FLOWS_OUT = T, SPREAD_TO_AIR = F, UPDATE_TICK = T;

	public BlockOcean(String aName, Fluid aFluid) {
		super(aName, aFluid, FLOWS_OUT, T);
		tickRate = 20;
	}

	// @Override
	public void onBlockAdded(Level aWorld, int aX, int aY, int aZ) {
		if (PLACEMENT_ALLOWED) {
			if (UPDATE_TICK) aWorld.scheduleTick(new BlockPos(aX, aY, aZ), this, 10+RNGSUS.nextInt(90)); // было scheduleBlockUpdate(x,y,z,block,delay)
		} else {
			WD.set(aWorld, aX, aY, aZ, NB, 0, 2);
		}
	}

	// @Override
	public void onNeighborBlockChange(Level aWorld, int aX, int aY, int aZ, Block aBlock) {
		if (aBlock == Blocks.DIRT && WD.block(aWorld, aX, aY-1, aZ) == Blocks.GRASS_BLOCK) WD.set(aWorld, aX, aY-1, aZ, Blocks.DIRT, 1, 2);
		super.onNeighborBlockChange(aWorld, aX, aY, aZ, aBlock);
	}

	// @Override
	public void updateTick(Level aWorld, int aX, int aY, int aZ, Random aRandom) {
		PLACEMENT_ALLOWED = UPDATE_TICK = T;

		if (aWorld.hasChunksAt(aX-33, aY-33, aZ-33, aX+33, aY+33, aZ+33)) { // было doChunksNearChunkExist(x,y,z,33) — LevelReader.hasChunksAt(x0,y0,z0,x1,y1,z1) тот же checkChunksExist-инлайн
			aWorld.getLightEngine().checkBlock(new BlockPos(aX, aY, aZ)); // было func_147451_t(x,y,z) (relight Sky+Block) — LevelLightEngine.checkBlock(BlockPos)
			WD.update(aWorld, aX, aY, aZ);
			if (aY > WD.minY(aWorld)) { // F6-Y-scale: было aY > 0, дно neo = getMinY()
				if (WD.block(aWorld, aX, aY-1, aZ) == this) {
					aWorld.scheduleTick(new BlockPos(aX, aY-1, aZ), this, tickRate);
				} else {
					aWorld.getLightEngine().checkBlock(new BlockPos(aX, aY-1, aZ));
					WD.update(aWorld, aX, aY-1, aZ);
				}
			}
		} else {
			aWorld.scheduleTick(new BlockPos(aX, aY, aZ), this, Math.max(600, tickRate));
			PLACEMENT_ALLOWED = F;
			return;
		}
		
		if (aY <= WD.minY(aWorld)) { // F6-Y-scale: было aY <= 0, дно neo = getMinY()
			updateFlow(aWorld, aX, aY, aZ, aRandom);
			PLACEMENT_ALLOWED = F;
			return;
		}
		
		Block tBlock;

		Holder<Biome> tBiome = aWorld.getBiome(new BlockPos(aX, aY, aZ)); // было getBiomeGenForCoords(x,z) (2D) — LevelReader.getBiome(BlockPos) (F6-центр, см. WD.java envTemp/infiniteWater)

		boolean tHasNoOceanAround = T, tHasOceanBiome = BIOMES_OCEAN_BEACH.contains(tBiome); // было tBiome.biomeName — BiomeNameSet.contains(Holder<Biome>) резолвит идентичность сам
		byte tOceanCounter = 0;
		ArrayListNoNulls<BlockPos> tList = new ArrayListNoNulls<>();
		for (byte tSide : ALL_SIDES_HORIZONTAL) {
			tBlock = WD.block(aWorld, aX, aY, aZ, tSide);
			if (tBlock == this) {
				tHasNoOceanAround = F;
				if (tHasOceanBiome || WD.meta(aWorld, aX, aY, aZ, tSide) == 0) tOceanCounter++;
			} else if (tBlock == BlocksGT.River) {
				tList.add(new BlockPos(aX+OFFX[tSide], aY+OFFY[tSide], aZ+OFFZ[tSide]));
				tOceanCounter++;
			} else if (WD.water(tBlock)) {
				tList.add(new BlockPos(aX+OFFX[tSide], aY+OFFY[tSide], aZ+OFFZ[tSide]));
				if (tHasOceanBiome || WD.meta(aWorld, aX, aY, aZ, tSide) == 0) tOceanCounter++;
			} else if (tHasOceanBiome && tBlock instanceof BlockWaterlike) {
				tList.add(new BlockPos(aX+OFFX[tSide], aY+OFFY[tSide], aZ+OFFZ[tSide]));
				tOceanCounter++;
			}
		}
		
		tBlock = WD.block(aWorld, aX, aY-1, aZ);
		if (tBlock == this) {
			tHasNoOceanAround = F;
			if (WD.meta(aWorld, aX, aY-1, aZ) == 0) tOceanCounter++;
		} else if (WD.anywater(tBlock)) {
			tHasNoOceanAround = F;
			if (WD.set(aWorld, aX, aY-1, aZ, this, 0, WATER_UPDATE_FLAGS)) tOceanCounter++;
		}
		
		if (tHasNoOceanAround && WD.block(aWorld, aX, aY+1, aZ) != this) {
			WD.set(aWorld, aX, aY, aZ, NB, 0, WATER_UPDATE_FLAGS);
			PLACEMENT_ALLOWED = F;
			return;
		}
		
		if (WD.meta(aWorld, aX, aY, aZ) != 0) {
			if (tOceanCounter >= 2 || (SPREAD_TO_AIR && tHasOceanBiome) || (WD.block(aWorld, aX, aY+1, aZ) == this && WD.meta(aWorld, aX, aY+1, aZ) == 0)) {
				WD.set(aWorld, aX, aY, aZ, this, 0, WATER_UPDATE_FLAGS);
			}
		}
		
		if (BIOMES_RIVER_LAKE.contains(tBiome)) {
			tOceanCounter = 0;
			for (int i = -1; i < 2; i++) for (int j = -1; j < 2; j++) if (i != 0 && j != 0) {
				if (WD.block(aWorld, aX+i, aY, aZ+j) == this && WD.meta(aWorld, aX+i, aY, aZ+j) == 0) {
					tOceanCounter++;
				}
			}
			if (tOceanCounter < 3) {
				updateFlow(aWorld, aX, aY, aZ, aRandom);
				PLACEMENT_ALLOWED = F;
				return;
			}
		}
		
		for (BlockPos tCoords : tList) {
			if (WD.set(aWorld, tCoords.getX(), tCoords.getY(), tCoords.getZ(), this, 0, WATER_UPDATE_FLAGS)) for (int i = -1; i < 2; i++) for (int j = -1; j < 2; j++) {
				if (WD.exists(aWorld, tCoords.getX()+i, tCoords.getY(), tCoords.getZ()+j)) {
					tBlock = WD.block(aWorld, tCoords.getX()+i, tCoords.getY(), tCoords.getZ()+j);
					if (tBlock == this) aWorld.scheduleTick(new BlockPos(tCoords.getX()+i, tCoords.getY(), tCoords.getZ()+j), this, tickRate);
				}
			}
		}
		
		updateFlow(aWorld, aX, aY, aZ, aRandom);
		PLACEMENT_ALLOWED = F;
		return;
	}
	
	// @Override
	public int getLightOpacity(BlockGetter aWorld, int aX, int aY, int aZ) {
		// TODO FIX THIS SHIT
		// PORT-TODO(F3, arbitrary-block light opacity): было WD.block(...).getLightOpacity(world,x,y,z) —
		// 1.7.10 Forge-хук на ЛЮБОМ Block, у neo per-instance getLightOpacity(BlockGetter,pos) на произвольном
		// Block нет (F9-центр WD не покрывает; см. decisions/F5-fluids.md). Дефолт вместо вычисления.
		return LIGHT_OPACITY_NONE;
	}

	@Override public net.minecraft.resources.Identifier getIcon(int aSide, int aMeta) {throw new UnsupportedOperationException("PORT-TODO(F3, fluid icon): было Blocks.water.getIcon — neo BakedModel-рендер, crash-only per /goal");} // см. BlockWaterlike.getIcon
	@Override public int getRenderColor(int aMeta) {return 0x00c0c0c0;}
	@Override public int colorMultiplier(BlockGetter aWorld, int aX, int aY, int aZ) {return 0x00c0c0c0;}
}

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
 *
 * Modified in 2026 for the GregTech 6 NeoForge port
 * (https://github.com/wolfram0108/gregtech6_w): ported from Minecraft 1.7.10 / Forge
 * to Minecraft 26.1.2 / NeoForge.
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

/** @author Gregorius Techneticies
 *  neo's block tick hook has a different signature, so these are no longer real overrides; the runtime
 *  bridge uses the same '// @Override' trick as {@link gregapi.block.BlockBase#updateTick}. */
public class BlockOcean extends BlockWaterlike {
	public static boolean PLACEMENT_ALLOWED = F, FLOWS_OUT = T, SPREAD_TO_AIR = F, UPDATE_TICK = T;

	public BlockOcean(String aName, Fluid aFluid) {
		super(aName, aFluid, FLOWS_OUT, T);
		tickRate = 20;
	}

	// @Override
	public void onBlockAdded(Level aWorld, int aX, int aY, int aZ) {
		if (PLACEMENT_ALLOWED) {
			if (UPDATE_TICK) aWorld.scheduleTick(new BlockPos(aX, aY, aZ), this, 10+RNGSUS.nextInt(90)); // was scheduleBlockUpdate(x,y,z,block,delay)
		} else {
			WD.set(aWorld, aX, aY, aZ, NB, 0, 2);
		}
	}

	// @Override
	public void onNeighborBlockChange(Level aWorld, int aX, int aY, int aZ, Block aBlock) {
		if (gregapi.data.CS.Flattened.headOf(aBlock) == Blocks.DIRT && WD.block(aWorld, aX, aY-1, aZ) == Blocks.GRASS_BLOCK) WD.set(aWorld, aX, aY-1, aZ, Blocks.DIRT, 1, 2);
		super.onNeighborBlockChange(aWorld, aX, aY, aZ, aBlock);
	}

	// @Override
	public void updateTick(Level aWorld, int aX, int aY, int aZ, Random aRandom) {
		PLACEMENT_ALLOWED = UPDATE_TICK = T;

		if (aWorld.hasChunksAt(aX-33, aY-33, aZ-33, aX+33, aY+33, aZ+33)) { // was doChunksNearChunkExist(x,y,z,33) -> LevelReader.hasChunksAt, the same inlined check
			// Idle re-light plus a forced client update on every tick of every water block are removed: cheap in 1.7.10,
			// they cost 2 light-enqueues and a section-update packet each in neo, remeshing 150-260 sections/sec near the sea.
			if (aY > WD.minY(aWorld)) { // Was aY > 0; neo's world floor is getMinY() instead of 0.
				if (WD.block(aWorld, aX, aY-1, aZ) == this) {
					aWorld.scheduleTick(new BlockPos(aX, aY-1, aZ), this, tickRate);
				}
			}
		} else {
			aWorld.scheduleTick(new BlockPos(aX, aY, aZ), this, Math.max(600, tickRate));
			PLACEMENT_ALLOWED = F;
			return;
		}
		
		if (aY <= WD.minY(aWorld)) { // Was aY <= 0; neo's world floor is getMinY() instead of 0.
			updateFlow(aWorld, aX, aY, aZ, aRandom);
			PLACEMENT_ALLOWED = F;
			return;
		}
		
		Block tBlock;

		Holder<Biome> tBiome = aWorld.getBiome(new BlockPos(aX, aY, aZ)); // Was getBiomeGenForCoords(x,z) (2D); LevelReader.getBiome(BlockPos) is the shared biome-lookup center (see WD.java).

		boolean tHasNoOceanAround = T, tHasOceanBiome = BIOMES_OCEAN_BEACH.contains(tBiome); // was tBiome.biomeName; BiomeNameSet.contains(Holder<Biome>) resolves identity itself
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
			// Claiming water below itself is ocean's second conversion branch, unrestricted in 1.7.10 since large water was itself a
			// biome; ocean's own territory is the same BIOMES_OCEAN_BEACH it already checks above.
			if (canClaim(aWorld, aX, aY-1, aZ) && WD.set(aWorld, aX, aY-1, aZ, this, 0, WATER_UPDATE_FLAGS)) tOceanCounter++;
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
		
		// The claim itself lives in BlockWaterlike: ocean, river and swamp all take foreign water the same way,
		// each stopped by its own canClaim territory (in mc26 the water sits in land biomes, not in its own).
		claimWater(aWorld, tList);
		
		updateFlow(aWorld, aX, aY, aZ, aRandom);
		PLACEMENT_ALLOWED = F;
		return;
	}
	
	// Light dampening now comes from the shared BlockFluidBaseGT center instead of a dead method the engine
	// never called; ocean's own neighbor-dependent 1.7.10 hack can't port since dampening only sees the block's own state.

	// getIcon isn't overridden here since the original body is verbatim the base class's own; only the tint below is ocean's
	// own.
	@Override public int getRenderColor(int aMeta) {return 0x00c0c0c0;}
	@Override public int colorMultiplier(BlockGetter aWorld, int aX, int aY, int aZ) {return 0x00c0c0c0;}

	/** Ocean's own territory is the same ocean/beach biome set the tick already checks; the original's separate
	 *  BIOMES_RIVER_LAKE limiter expresses a different rule (solid-front claiming in rivers/lakes) and is left untouched. */
	@Override
	public boolean canClaim(Level aWorld, int aX, int aY, int aZ) {
		return BIOMES_OCEAN_BEACH.contains(aWorld.getBiome(new BlockPos(aX, aY, aZ)));
	}
}

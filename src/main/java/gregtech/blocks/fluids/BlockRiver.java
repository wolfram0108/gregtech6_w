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

import gregapi.data.FL;
import gregapi.util.UT;
import gregapi.util.WD;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.Random;

import static gregapi.data.CS.*;

/**
 * @author Gregorius Techneticies
 *
 * F5 форс движка (decisions/F5-fluids.md §5): движковые хуки — см. javadoc {@link BlockOcean}.
 */
public class BlockRiver extends BlockWaterlike {
	public static boolean PLACEMENT_ALLOWED = F, FLOWS_OUT = T;

	public BlockRiver(String aName, Fluid aFluid) {
		super(aName, aFluid, FLOWS_OUT, T);
		tickRate = 20;
	}

	// @Override
	public void onBlockAdded(Level aWorld, int aX, int aY, int aZ) {
		if (PLACEMENT_ALLOWED) {
			aWorld.scheduleTick(new BlockPos(aX, aY, aZ), this, 10+RNGSUS.nextInt(90)); // было scheduleBlockUpdate(x,y,z,block,delay)
		} else {
			WD.set(aWorld, aX, aY, aZ, NB, 0, 3);
		}
	}

	// @Override
	public void onNeighborBlockChange(Level aWorld, int aX, int aY, int aZ, Block aBlock) {
		if (WD.block(aWorld, aX, aY-1, aZ) == Blocks.GRASS_BLOCK) WD.set(aWorld, aX, aY-1, aZ, Blocks.DIRT, 1, 2);
		super.onNeighborBlockChange(aWorld, aX, aY, aZ, aBlock);
	}

	// @Override
	public void updateTick(Level aWorld, int aX, int aY, int aZ, Random aRandom) {
		PLACEMENT_ALLOWED = T;

		if (aWorld.hasChunksAt(aX-33, aY-33, aZ-33, aX+33, aY+33, aZ+33)) { // было doChunksNearChunkExist(x,y,z,33) — см. BlockOcean
			// ADAPT-009: холостые re-light + клиент-апдейт каждого тика воды сняты — обоснование см. BlockOcean.updateTick
			if (aY > WD.minY(aWorld)) { // F6-Y-scale: было aY > 0, дно neo = getMinY()
				if (WD.block(aWorld, aX, aY-1, aZ) == this) {
					aWorld.scheduleTick(new BlockPos(aX, aY-1, aZ), this, tickRate);
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
		if (WD.meta(aWorld, aX, aY, aZ) != 0) {
			byte tRiverCounter = 0;
			for (byte tSide : ALL_SIDES_HORIZONTAL) if (WD.block(aWorld, aX, aY, aZ, tSide) == this && WD.meta(aWorld, aX, aY, aZ, tSide) == 0) tRiverCounter++;
			if (tRiverCounter >= 2) WD.set(aWorld, aX, aY, aZ, this, 0, WATER_UPDATE_FLAGS);
		}
		updateFlow(aWorld, aX, aY, aZ, aRandom);
		PLACEMENT_ALLOWED = F;
		return;
	}
	
	@Override
	public FluidStack drain(Level aWorld, int aX, int aY, int aZ, boolean aDoDrain) {
		if (!isSourceBlock(aWorld, aX, aY, aZ)) return null;
		if (aDoDrain) WD.set(aWorld, aX, aY, aZ, NB, 0, 3);
		return FL.Water.make(1000);
	}
	
	@Override
	public int colorMultiplier(BlockGetter aWorld, int aX, int aY, int aZ) {
		// было aWorld.getBiomeGenForCoords(x,z) (2D, IBlockAccess) — BlockGetter самого getBiome не несёт
		// (LevelReader.getBiome(BlockPos), F6-центр); рендер вызывает colorMultiplier всегда с реальным Level
		// (тот же приём — WD.te(BlockGetter,...) instanceof Level, WD.java:379-380), F3-safe дефолт иначе.
		if (!(aWorld instanceof Level)) return 0x00ffffff;
		Level aLevel = (Level)aWorld;
		int rR = 0, rG = 0, rB = 0;
		for (int tX = -1; tX <= 1; tX++) for (int tZ = -1; tZ <= 1; tZ++) {
			int tRGB = aLevel.getBiome(new BlockPos(aX+tX, aY, aZ+tZ)).value().getWaterColor(); // было .getWaterColorMultiplier() — Biome.getWaterColor() (Biome.java:259)
			rR += UT.Code.getR(tRGB);
			rG += UT.Code.getG(tRGB);
			rB += UT.Code.getB(tRGB);
		}
		return UT.Code.getRGBInt(rR/9, rG/9, rB/9);
	}
}

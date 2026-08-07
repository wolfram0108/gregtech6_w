/**
 * Copyright (c) 2020 GregTech-6 Team
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

package gregtech.worldgen.nether;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.WorldGenLevel;

import static gregapi.data.CS.*;

import java.util.List;
import java.util.Random;
import java.util.Set;

import gregapi.data.CS.BlocksGT;
import gregapi.util.WD;
import gregapi.worldgen.WorldgenObject;
import gregtech.worldgen.NoiseGenerator;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.LevelChunk;

/**
 * @author Gregorius Techneticies
 */
public class WorldgenNetherQuartz extends WorldgenObject {
	@SafeVarargs
	public WorldgenNetherQuartz(String aName, boolean aDefault, List<WorldgenObject>... aLists) {
		super(aName, aDefault, aLists);
	}
	
	@Override
	public boolean generate(WorldGenLevel aWorld, ChunkAccess aChunk, int aDimType, int aMinX, int aMinZ, int aMaxX, int aMaxZ, Random aRandom, Biome[][] aBiomes, Set<String> aBiomeNames) {
		NoiseGenerator tNoise = new NoiseGenerator(aWorld);
		for (int i = 0; i < 16; i++) for (int j = 0; j < 16; j++) {
			int
			tY = 40+tNoise.get(aMinX+i,  0, aMinZ+j, 200);
			if (WD.block(aChunk, i, tY, j) == Blocks.NETHERRACK) WD.set(aChunk, i, tY, j, BlocksGT.RockOres, 8);
			tY = 40+tNoise.get(aMinX+i, 64, aMinZ+j, 200);
			if (WD.block(aChunk, i, tY, j) == Blocks.NETHERRACK) WD.set(aChunk, i, tY, j, BlocksGT.RockOres, 8);
		}
		return T;
	}
}

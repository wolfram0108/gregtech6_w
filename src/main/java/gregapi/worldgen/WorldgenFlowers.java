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

package gregapi.worldgen;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.WorldGenLevel;

import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.Set;

import gregapi.util.UT;
import gregapi.util.WD;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.common.IPlantable;

/**
 * @author Gregorius Techneticies
 */
public class WorldgenFlowers extends WorldgenOnSurface {
	public final int mBlockMeta;
	public final Block mBlock;
	public final Collection<String> mBiomeList;
	
	@SafeVarargs
	public WorldgenFlowers(String aName, boolean aDefault, Block aBlock, int aBlockMeta, int aAmount, int aProbability, Collection<String> aBiomeList, List<WorldgenObject>... aLists) {
		super(aName, aDefault, aAmount, aProbability, aLists);
		mBlock     = (aBlock instanceof IPlantable ? aBlock : Blocks.DANDELION);
		mBlockMeta = UT.Code.bind4(aBlockMeta);
		mBiomeList = aBiomeList;
	}
	
	@Override
	public int canGenerate(WorldGenLevel aWorld, ChunkAccess aChunk, int aDimType, int aMinX, int aMinZ, int aMaxX, int aMaxZ, Random aRandom, Biome[][] aBiomes, Set<String> aBiomeNames) {
		if (checkForMajorWorldgen(aWorld, aMinX, aMinZ, aMaxX, aMaxZ)) return 0;
		for (String tName : aBiomeNames) if (mBiomeList.contains(tName)) return mAmount;
		return 0;
	}
	
	@Override
	public boolean tryPlaceStuff(WorldGenLevel aWorld, int aX, int aY, int aZ, Random aRandom, Block aContact) {
		return WD.easyRep(aWorld, aX, aY+1, aZ) && mBlock.defaultBlockState().canSurvive(aWorld, new net.minecraft.core.BlockPos(aX, aY+1, aZ)) && WD.set(aWorld, aX, aY+1, aZ, mBlock, mBlockMeta, 2); // F-plant: canBlockStay -> BlockState.canSurvive (BlockBehaviour.java:827).
	}
}

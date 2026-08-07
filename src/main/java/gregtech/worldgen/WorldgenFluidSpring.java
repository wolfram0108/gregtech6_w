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

package gregtech.worldgen;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.WorldGenLevel;

import gregapi.data.IL;
import gregapi.util.ST;
import gregapi.util.UT;
import gregapi.util.WD;
import gregapi.worldgen.WorldgenObject;
import gregapi.worldgen.WorldgenOresBedrock;
import gregtech.tileentity.misc.MultiTileEntityFluidSpring;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.List;
import java.util.Random;
import java.util.Set;

import static gregapi.data.CS.*;

/**
 * @author Gregorius Techneticies
 */
public class WorldgenFluidSpring extends WorldgenObject {
	public final int mMeta, mProbability, mIndicatorType;
	public final FluidStack mSpringFluid;
	public final Block mBlock;
	
	@SafeVarargs public WorldgenFluidSpring(String aName, boolean aDefault, Block aBlock, int aMeta, int aProbability, FluidStack aSpringFluid, List<WorldgenObject>... aLists) {this(aName, aDefault, aBlock, aMeta, aProbability, 0, aSpringFluid, aLists);}
	@SafeVarargs public WorldgenFluidSpring(String aName, boolean aDefault, Block aBlock, int aMeta, int aProbability, int aIndicatorType, FluidStack aSpringFluid, List<WorldgenObject>... aLists) {
		super(aName, aDefault, aLists);
		mSpringFluid   = aSpringFluid;
		mBlock         = ST.valid(aBlock)?aBlock:Blocks.WATER;
		mMeta          = UT.Code.bind4(aMeta);
		mIndicatorType = aIndicatorType;
		mProbability   = getConfigFile().get(mCategory, "Probability", aProbability);
	}
	
	@Override
	public boolean generate(WorldGenLevel aWorld, ChunkAccess aChunk, int aDimType, int aMinX, int aMinZ, int aMaxX, int aMaxZ, Random aRandom, Biome[][] aBiomes, Set<String> aBiomeNames) {
		if (GENERATING_SPECIAL || !WorldgenOresBedrock.generatedNoBedrockOre() || !WorldgenOresBedrock.canGenerateBedrockOre() || aRandom.nextInt(mProbability) != 0) return F;

		WorldgenOresBedrock.setCanGenerateBedrockOre(F);
		
		// F6-Y-scale: бедрок MC26 на getMinY() (был Y=0) — родник и слои жидкости якорятся к дну мира (tMinY).
		final int tMinY = WD.minY(aWorld);
		Block tBlock = WD.block(aWorld, aMinX+8, tMinY, aMinZ+8);
		if (tBlock != BlocksGT.oreBedrock && tBlock != BlocksGT.oreSmallBedrock && !WD.bedrock(tBlock)) return F;
		
		tBlock = (aDimType == DIM_NETHER ? Blocks.NETHERRACK : IL.EtFu_Deepslate.block());
		if (ST.invalid(tBlock)) tBlock = Blocks.STONE;
		
		for (int i = 0; i <= 6; i++) for (int tX = aMinX+i; tX <= aMaxX-i; tX++) for (int tZ = aMinZ+i; tZ <= aMaxZ-i; tZ++) {
			if (!WD.opq(aWorld, tX, tMinY+i+1, tZ, F, T)) WD.set(aWorld, tX, tMinY+i+1, tZ, tBlock, 0, 0);

			if (i > 0) WD.set(aWorld, tX, tMinY+i, tZ, mBlock, mMeta, 0);

			if (mSpringFluid != null && i > 2 && aRandom.nextInt(16) == 0 && WD.bedrock(aWorld, tX, tMinY, tZ)) {
				MultiTileEntityFluidSpring.setBlock(aWorld, tX, tMinY, tZ, mSpringFluid);
			}
		}
		
		switch (mIndicatorType) {
		// Yellow or Brown Grass.
		case  1: case  2: case  3:
			// F6-Y-scale: no-arg getHeight()=COUNT в MC26 → WD.topY (maxY+1 = старая getHeight()).
			int tMinHeight = Math.min(WD.topY(aWorld)-2, WD.waterLevel(aWorld)-1)
			,   tMaxHeight = Math.min(WD.topY(aWorld)-1, tMinHeight * 2 + 16);
			for (int i = 0; i < 6; i++) {
				int tX = aMinX+4+aRandom.nextInt(8), tZ = aMinZ+4+aRandom.nextInt(8);
				for (int tY = tMaxHeight; tY > tMinHeight; tY--) {
					Block tContact = WD.block(aWorld, tX, tY, tZ);
					if (WD.getMaterial(tContact).isLiquid() || tContact == Blocks.FARMLAND) break;
					if (!WD.opaque(tContact) || WD.wood(tContact, aWorld, tX, tY, tZ) || WD.leaves(tContact, aWorld, tX, tY, tZ)) continue;
					if (!BlocksGT.plantableGrass.contains(tContact)) break;
					for (int a = -1; a <= 1; a++) for (int b = -1; b <= 1; b++) if (aRandom.nextBoolean()) if (BlocksGT.plantableGrass.contains(WD.block(aWorld, tX+a, tY, tZ+b))) {
						WD.set(aWorld, tX+a, tY, tZ+b, BlocksGT.Grass, mIndicatorType == 3 ? 0 : 3+mIndicatorType, 0);
					}
					break;
				}
			}
			break;
		default:
			break;
		}
		
		return T;
	}
}

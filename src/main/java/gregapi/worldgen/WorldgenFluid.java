/**
 * Copyright (c) 2019 Gregorius Techneticies
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

package gregapi.worldgen;

import static gregapi.data.CS.*;

import java.util.Collection;
import java.util.List;
import java.util.Random;

import gregapi.util.WD;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.Level;

/**
 * @author Gregorius Techneticies
 */
public class WorldgenFluid extends WorldgenBlob {
	@SafeVarargs
	public WorldgenFluid(String aName, boolean aDefault, Block aBlock, int aBlockMeta, int aAmount, int aSize, int aProbability, int aMinY, int aMaxY, Collection<String> aBiomeList, boolean aAllowToGenerateinVoid, List<WorldgenObject>... aLists) {
		super(aName, aDefault, aBlock, aBlockMeta, aAmount, aSize, aProbability, aMinY, aMaxY, aBiomeList, aAllowToGenerateinVoid, aLists);
	}
	
	@Override
	public boolean tryPlaceStuff(Level aWorld, int aX, int aY, int aZ, Random aRandom) {
		Block tTargetedBlock = WD.block(aWorld, aX, aY, aZ);
		if (tTargetedBlock == mBlock && WD.meta(aWorld, aX, aY, aZ) == mBlockMeta) {
			return T;
		}
		if (WD.bedrock(aWorld, aX, aY, aZ, tTargetedBlock)) {
			return aY >= 1 && aY <= 4 ? WD.set(aWorld, aX, aY, aZ, mBlock, mBlockMeta, 0) : doBedrockStuff(aWorld, aX, aY, aZ, aRandom);
		}
		if (tTargetedBlock == NB || WD.air(aWorld, aX, aY, aZ, tTargetedBlock)) {
			return mAllowToGenerateinVoid && WD.set(aWorld, aX, aY, aZ, mBlock, mBlockMeta, 0);
		}
		if (tTargetedBlock == Blocks.DIRT || tTargetedBlock == Blocks.SOUL_SAND || WD.ore_stone(tTargetedBlock, (byte)WD.meta(aWorld, aX, aY, aZ))) {
			return WD.set(aWorld, aX, aY, aZ, mBlock, mBlockMeta, 0);
		}
		return F;
	}
	
	public boolean doBedrockStuff(Level aWorld, int aX, int aY, int aZ, Random aRandom) {
		return F;
	}
}

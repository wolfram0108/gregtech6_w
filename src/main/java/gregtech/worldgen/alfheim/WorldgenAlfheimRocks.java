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
 */

package gregtech.worldgen.alfheim;

import static gregapi.data.CS.*;

import java.util.List;
import java.util.Random;
import java.util.Set;

import gregapi.block.multitileentity.MultiTileEntityRegistry;
import gregapi.util.ST;
import gregapi.util.UT;
import gregapi.util.WD;
import gregapi.worldgen.WorldgenObject;
import gregapi.worldgen.WorldgenOnSurface;
import net.minecraft.world.level.block.Block;
import gregapi.block.Material;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.LevelChunk;

/**
 * @author Gregorius Techneticies
 */
public class WorldgenAlfheimRocks extends WorldgenOnSurface {
	@SafeVarargs
	public WorldgenAlfheimRocks(String aName, boolean aDefault, int aAmount, int aProbability, List<WorldgenObject>... aLists) {
		super(aName, aDefault, aAmount, aProbability, aLists);
	}
	
	@Override
	public int canGenerate(Level aWorld, LevelChunk aChunk, int aDimType, int aMinX, int aMinZ, int aMaxX, int aMaxZ, Random aRandom, Biome[][] aBiomes, Set<String> aBiomeNames) {
		if (checkForMajorWorldgen(aWorld, aMinX, aMinZ, aMaxX, aMaxZ)) return 0;
		return mAmount;
	}
	
	@Override
	public boolean tryPlaceStuff(Level aWorld, int aX, int aY, int aZ, Random aRandom, Block aContact) {
		if (WD.getMaterial(aContact) != Material.grass && WD.getMaterial(aContact) != Material.ground && WD.getMaterial(aContact) != Material.sand) return F;
		MultiTileEntityRegistry tRegistry = MultiTileEntityRegistry.getRegistry("gt.multitileentity");
		if (tRegistry == null) return F;
		if (WD.easyRep(aWorld, aX, aY+1, aZ)) return tRegistry.mBlock.placeBlock(aWorld, aX, aY+1, aZ, SIDE_UNKNOWN, (short)32757, aRandom.nextInt(5)==0?ST.save(UT.NBT.make(), NBT_VALUE, Items.FLINT):null, F, T);
		return F;
	}
}

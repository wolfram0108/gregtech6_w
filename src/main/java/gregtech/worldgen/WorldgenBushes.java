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

package gregtech.worldgen;

import gregapi.block.multitileentity.MultiTileEntityRegistry;
import gregapi.code.ItemStackContainer;
import gregapi.data.MD;
import gregapi.util.ST;
import gregapi.util.UT;
import gregapi.util.WD;
import gregapi.worldgen.WorldgenObject;
import gregapi.worldgen.WorldgenOnSurface;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
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
public class WorldgenBushes extends WorldgenOnSurface {
	@SafeVarargs
	public WorldgenBushes(String aName, boolean aDefault, int aAmount, int aProbability, List<WorldgenObject>... aLists) {
		super(aName, aDefault, aAmount, aProbability, aLists);
	}
	
	@Override
	public int canGenerate(Level aWorld, LevelChunk aChunk, int aDimType, int aMinX, int aMinZ, int aMaxX, int aMaxZ, Random aRandom, Biome[][] aBiomes, Set<String> aBiomeNames) {
		if (checkForMajorWorldgen(aWorld, aMinX, aMinZ, aMaxX, aMaxZ)) return 0;
		for (String tName : aBiomeNames) if (!BIOMES_FROZEN.contains(tName) && (BIOMES_PLAINS.contains(tName) || BIOMES_WOODS.contains(tName))) return mAmount;
		return 0;
	}
	
	@Override
	public boolean tryPlaceStuff(Level aWorld, int aX, int aY, int aZ, Random aRandom, Block aContact) {
		if (!BlocksGT.plantableGreens.contains(aContact) || !WD.easyRep(aWorld, aX, aY+1, aZ)) return F;
		MultiTileEntityRegistry tRegistry = MultiTileEntityRegistry.getRegistry("gt.multitileentity");
		if (tRegistry == null) return F;
		
		ItemStack tBerry = UT.Code.select(new NoiseGenerator(aWorld).get(aX/2, 300, aZ/2, BushesGT.MAP.size()), new ItemStackContainer(Items.STRING, 1, 0), BushesGT.MAP.keySet().toArray(ZL_ISC)).toStack();
		
		// Change Minecraft String to Harvestcraft Cotton, because it's in the static initializer as a default.
		if (MD.HaC.mLoaded && ST.item(tBerry) == Items.STRING) tBerry = ST.make(MD.HaC, "cottonItem", 1, 0, tBerry);
		
		if (placeBushCore(aWorld, aX, aY, aZ, tRegistry, tBerry, 3)) {
			if (aRandom.nextBoolean() && placeBushCore(aWorld, aX-1, aY, aZ  , tRegistry, tBerry, 3)) placeBushSides(aWorld, aX-1, aY, aZ  , tRegistry, tBerry, 3);
			if (aRandom.nextBoolean() && placeBushCore(aWorld, aX+1, aY, aZ  , tRegistry, tBerry, 3)) placeBushSides(aWorld, aX+1, aY, aZ  , tRegistry, tBerry, 3);
			if (aRandom.nextBoolean() && placeBushCore(aWorld, aX  , aY, aZ-1, tRegistry, tBerry, 3)) placeBushSides(aWorld, aX  , aY, aZ-1, tRegistry, tBerry, 3);
			if (aRandom.nextBoolean() && placeBushCore(aWorld, aX  , aY, aZ+1, tRegistry, tBerry, 3)) placeBushSides(aWorld, aX  , aY, aZ+1, tRegistry, tBerry, 3);
			placeBushSides(aWorld, aX, aY, aZ, tRegistry, tBerry, 3);
			return T;
		}
		return F;
	}
	
	public boolean placeBushCore(Level aWorld, int aX, int aY, int aZ, MultiTileEntityRegistry aRegistry, ItemStack aBerry, int aStage) {
		Block tBlock = WD.block(aWorld, aX, aY, aZ);
		if (!BlocksGT.plantableGreens.contains(tBlock) || !WD.easyRep(aWorld, aX, aY+1, aZ)) return F;
		if (tBlock == Blocks.GRASS_BLOCK) WD.set(aWorld, aX, aY, aZ, Blocks.DIRT, 0, 3);
		return aRegistry.mBlock.placeBlock(aWorld, aX  , aY+1, aZ  , SIDE_UNKNOWN, (short)32759, ST.save(UT.NBT.make(NBT_FACING, SIDE_UNDEFINED, NBT_STATE, aStage), NBT_VALUE, aBerry), T, T);
	}
	
	public boolean placeBushSides(Level aWorld, int aX, int aY, int aZ, MultiTileEntityRegistry aRegistry, ItemStack aBerry, int aStage) {
		if (WD.easyRep(aWorld, aX+1, aY+1, aZ  )) aRegistry.mBlock.placeBlock(aWorld, aX+1, aY+1, aZ  , SIDE_UNKNOWN, (short)32759, ST.save(UT.NBT.make(NBT_FACING, SIDE_X_NEG    , NBT_STATE, aStage), NBT_VALUE, aBerry), T, T);
		if (WD.easyRep(aWorld, aX-1, aY+1, aZ  )) aRegistry.mBlock.placeBlock(aWorld, aX-1, aY+1, aZ  , SIDE_UNKNOWN, (short)32759, ST.save(UT.NBT.make(NBT_FACING, SIDE_X_POS    , NBT_STATE, aStage), NBT_VALUE, aBerry), T, T);
		if (WD.easyRep(aWorld, aX  , aY+1, aZ+1)) aRegistry.mBlock.placeBlock(aWorld, aX  , aY+1, aZ+1, SIDE_UNKNOWN, (short)32759, ST.save(UT.NBT.make(NBT_FACING, SIDE_Z_NEG    , NBT_STATE, aStage), NBT_VALUE, aBerry), T, T);
		if (WD.easyRep(aWorld, aX  , aY+1, aZ-1)) aRegistry.mBlock.placeBlock(aWorld, aX  , aY+1, aZ-1, SIDE_UNKNOWN, (short)32759, ST.save(UT.NBT.make(NBT_FACING, SIDE_Z_POS    , NBT_STATE, aStage), NBT_VALUE, aBerry), T, T);
		if (WD.easyRep(aWorld, aX  , aY+2, aZ  )) aRegistry.mBlock.placeBlock(aWorld, aX  , aY+2, aZ  , SIDE_UNKNOWN, (short)32759, ST.save(UT.NBT.make(NBT_FACING, SIDE_Y_NEG    , NBT_STATE, aStage), NBT_VALUE, aBerry), T, T);
		return T;
	}
}

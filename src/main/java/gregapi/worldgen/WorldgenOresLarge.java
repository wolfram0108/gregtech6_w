/**
 * Copyright (c) 2021 GregTech-6 Team
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

import static gregapi.data.CS.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import gregapi.block.multitileentity.MultiTileEntityRegistry;
import gregapi.code.ArrayListNoNulls;
import gregapi.config.Config;
import gregapi.data.CS.ConfigsGT;
import gregapi.data.OP;
import gregapi.oredict.OreDictManager;
import gregapi.oredict.OreDictMaterial;
import gregapi.util.ST;
import gregapi.util.UT;
import gregapi.util.WD;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;

/**
 * @author Gregorius Techneticies
 */
public class WorldgenOresLarge extends WorldgenObject {
	public static ArrayList<WorldgenOresLarge> sList = new ArrayListNoNulls<>();
	public final int mWeight, mDistance;
	public final short mMinY, mMaxY, mDensity, mSize;
	public final OreDictMaterial mTop, mBottom, mBetween, mSpread;
	public final boolean mIndicatorRocks;
	
	@SafeVarargs
	public WorldgenOresLarge(String aName, boolean aDefault, int aMinY, int aMaxY, int aWeight, int aDensity, int aSize, OreDictMaterial aTop, OreDictMaterial aBottom, OreDictMaterial aBetween, OreDictMaterial aSpread, List<WorldgenObject>... aLists) {
		this(aName, aDefault, T, aMinY, aMaxY, aWeight, aDensity, aSize, aTop, aBottom, aBetween, aSpread, aLists);
	}
	
	@SafeVarargs
	public WorldgenOresLarge(String aName, boolean aDefault, boolean aIndicatorRocks, int aMinY, int aMaxY, int aWeight, int aDensity, int aSize, OreDictMaterial aTop, OreDictMaterial aBottom, OreDictMaterial aBetween, OreDictMaterial aSpread, List<WorldgenObject>... aLists) {
		super(aName, aDefault, aLists);
		mMinY               = (short)Math.max(0,        getConfigFile().get(mCategory, "MinHeight"        , aMinY));
		mMaxY               = (short)Math.max(mMinY+5,  getConfigFile().get(mCategory, "MaxHeight"        , aMaxY));
		mWeight             =        Math.max(1,        getConfigFile().get(mCategory, "RandomWeight"     , aWeight));
		mDensity            = (short)Math.max(1,        getConfigFile().get(mCategory, "Density"          , aDensity));
		mDistance           =        Math.max(0,        getConfigFile().get(mCategory, "DistanceFromSpawn", 0));
		mSize               = (short)Math.max(1,        getConfigFile().get(mCategory, "Size"             , aSize));
		mIndicatorRocks     =                           getConfigFile().get(mCategory, "IndicatorRocks"   , aIndicatorRocks);
		mTop                =                           getConfigFile().get(mCategory, "OreTop"           , aTop);
		mBottom             =                           getConfigFile().get(mCategory, "OreBottom"        , aBottom);
		mBetween            =                           getConfigFile().get(mCategory, "OreBetween"       , aBetween);
		mSpread             =                           getConfigFile().get(mCategory, "OreSpread"        , aSpread);
		
		if (mEnabled) {
			if (mTop        .mID > 0) OreDictManager.INSTANCE.triggerVisibility("ore"+mTop      .mNameInternal);
			if (mBottom     .mID > 0) OreDictManager.INSTANCE.triggerVisibility("ore"+mBottom   .mNameInternal);
			if (mBetween    .mID > 0) OreDictManager.INSTANCE.triggerVisibility("ore"+mBetween  .mNameInternal);
			if (mSpread     .mID > 0) OreDictManager.INSTANCE.triggerVisibility("ore"+mSpread   .mNameInternal);
		}
		
		if (mTop        .mID <= 0) ERR.println("The OreTop Material is not valid for Ores: " + mTop);
		if (mBottom     .mID <= 0) ERR.println("The OreBottom Material is not valid for Ores: " + mBottom);
		if (mBetween    .mID <= 0) ERR.println("The OreBetween Material is not valid for Ores: " + mBetween);
		if (mSpread     .mID <= 0) ERR.println("The OreSpread Material is not valid for Ores: " + mSpread);
		
		if (mTop.mID <= 0 && mBottom.mID <= 0 && mBetween.mID <= 0 && mSpread.mID <= 0) mInvalid = T;
	}
	
	public boolean generate(WorldGenLevel aWorld, ChunkAccess aChunk, int aMinX, int aMinZ, int aMaxX, int aMaxZ, int aOriginChunkX, int aOriginChunkZ, Random aRandom) {
		if (GENERATING_SPECIAL) return F;
		if (mDistance > 0 && Math.abs(aMinX) <= mDistance && Math.abs(aMinZ) <= mDistance) return F;
		
		// The vein window [mMinY..mMaxY] (defined for the old 0..255 world) stretches sea-anchored for MC26's taller world.
		int tRMinY = WD.remapY(aWorld, mMinY), tRMaxY = WD.remapY(aWorld, mMaxY);
		// A vein is flat, occupying only 7 layers, so stretching the window while keeping one vein per chunk would
		// thin veins by the stretch factor; vein count scales with it instead, each getting its own height from the same stream.
		java.util.Random tVeinRandom = WD.random(aWorld, aOriginChunkX, aOriginChunkZ);
		int tVeins = WD.yScaleAmount(aWorld, mMinY, mMaxY, 1, tVeinRandom);
		for (int tVein = 0; tVein < tVeins; tVein++) {
		int tMinY = tRMinY + tVeinRandom.nextInt(Math.max(1, tRMaxY - tRMinY - 5));

		// Was a check against the literal vanilla Overworld id 0; checked against the real Level.OVERWORLD constant instead, as
		// elsewhere.
		if (mIndicatorRocks && (!(GENERATE_STREETS && aWorld.getLevel().dimension() == Level.OVERWORLD) || (Math.abs(aMinX) >= 64 && Math.abs(aMaxX) >= 64 && Math.abs(aMinZ) >= 64 && Math.abs(aMaxZ) >= 64))) {
			MultiTileEntityRegistry tRegistry = MultiTileEntityRegistry.getRegistry("gt.multitileentity");
			if (tRegistry != null) {
				for (int i = 0, j = 1+aRandom.nextInt(3); i < j; i++) {
					int tX = aMinX + aRandom.nextInt(16), tZ = aMinZ + aRandom.nextInt(16);
					for (int tY = Math.min(WD.topY(aWorld), tMinY+25); tY >= tMinY-10 && tY > WD.minY(aWorld); tY--) {
						// LevelChunk.getBlock(int,int,int) is gone; the real neo equivalent is LevelChunk.getBlockState(BlockPos).
						BlockState tContact = aChunk.getBlockState(new BlockPos(tX, tY, tZ));
						// WD.getMaterial(Block) is gone; the 1:1 replacement for 'material is liquid' is BlockState.liquid(), which inherits the
						// same value the old Material.isLiquid held.
						if (tContact.liquid()) break;
						// Was isOpaqueCube() for skipping non-solid blocks while descending; replaced by the central WD.opaque helper.
						if (!WD.opaque(tContact.getBlock())) continue;
						// The indicator only goes on grass/ground/sand/rock materials; WD.getMaterial(Block) is fully implemented by identity
						// and tags, so the stale marker here is removed.
						gregapi.block.Material tMat = WD.getMaterial(tContact.getBlock());
						if (tMat != gregapi.block.Material.grass && tMat != gregapi.block.Material.ground && tMat != gregapi.block.Material.sand && tMat != gregapi.block.Material.rock) break;
						if (WD.easyRepDry(aWorld, tX, tY+1, tZ)) tRegistry.mBlock.placeBlock(aWorld, tX, tY+1, tZ, SIDE_UNKNOWN, (short)32757, aRandom.nextInt(3)!=0?ST.save(NBT_VALUE, OP.rockGt.mat(UT.Code.select(mTop, mTop, mBottom, mBetween, mSpread), 1)):UT.NBT.make(), F, T);
						break;
					}
				}
			}
		}
		
		for (int cX=aOriginChunkX-aRandom.nextInt(mSize), eX=aOriginChunkX+16+aRandom.nextInt(mSize), tX=Math.max(aMinX, cX); tX<=Math.min(aMaxX, eX); tX++)
		for (int cZ=aOriginChunkZ-aRandom.nextInt(mSize), eZ=aOriginChunkZ+16+aRandom.nextInt(mSize), tZ=Math.max(aMinZ, cZ); tZ<=Math.min(aMaxZ, eZ); tZ++) {
			if (mBottom.mID > 0) for (int i=tMinY-1; i<tMinY+2; i++) {
				if (aRandom.nextInt(Math.max(1, Math.max(Math.abs(cZ-tZ), Math.abs(eZ-tZ)) / mDensity)) == 0 || aRandom.nextInt(Math.max(1, Math.max(Math.abs(cX-tX), Math.abs(eX-tX)) / mDensity)) == 0) {
					WD.setOre(aWorld, tX, i, tZ, mBottom.mID);
				}
			}
			if (mTop.mID > 0) for (int i=tMinY+3; i<tMinY+6; i++) {
				if (aRandom.nextInt(Math.max(1, Math.max(Math.abs(cZ-tZ), Math.abs(eZ-tZ)) / mDensity)) == 0 || aRandom.nextInt(Math.max(1, Math.max(Math.abs(cX-tX), Math.abs(eX-tX)) / mDensity)) == 0) {
					WD.setOre(aWorld, tX, i, tZ, mTop.mID);
				}
			}
			if (mBetween.mID > 0) if (aRandom.nextInt(Math.max(1, Math.max(Math.abs(cZ-tZ), Math.abs(eZ-tZ)) / mDensity)) == 0 || aRandom.nextInt(Math.max(1, Math.max(Math.abs(cX-tX), Math.abs(eX-tX)) / mDensity)) == 0) {
				WD.setOre(aWorld, tX, tMinY+2+aRandom.nextInt(2), tZ, mBetween.mID);
			}
			if (mSpread.mID > 0) if (aRandom.nextInt(Math.max(1, Math.max(Math.abs(cZ-tZ), Math.abs(eZ-tZ)) / mDensity)) == 0 || aRandom.nextInt(Math.max(1, Math.max(Math.abs(cX-tX), Math.abs(eX-tX)) / mDensity)) == 0) {
				WD.setOre(aWorld, tX, tMinY-1+aRandom.nextInt(7), tZ, mSpread.mID);
			}
		}
		} // End of the loop over veins (tVeins).
		return T;
	}
	
	@Override
	public Config getConfigFile() {
		return ConfigsGT.WORLDGEN_GT5;
	}
}

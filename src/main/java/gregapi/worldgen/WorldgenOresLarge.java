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
 */

package gregapi.worldgen;

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
	
	public boolean generate(Level aWorld, LevelChunk aChunk, int aMinX, int aMinZ, int aMaxX, int aMaxZ, int aOriginChunkX, int aOriginChunkZ, Random aRandom) {
		if (GENERATING_SPECIAL) return F;
		if (mDistance > 0 && Math.abs(aMinX) <= mDistance && Math.abs(aMinZ) <= mDistance) return F;
		
		int tMinY = mMinY + WD.random(aWorld, aOriginChunkX, aOriginChunkZ).nextInt(mMaxY - mMinY - 5);
		
		// F6: было `WD.dimensionId(aWorld) == 0` (буквально ванильный Overworld) — сверено на реальную
		// константу Level.OVERWORLD (Level.java:95), как и в WorldgenObject.checkForMajorWorldgen.
		if (mIndicatorRocks && (!(GENERATE_STREETS && aWorld.dimension() == Level.OVERWORLD) || (Math.abs(aMinX) >= 64 && Math.abs(aMaxX) >= 64 && Math.abs(aMinZ) >= 64 && Math.abs(aMaxZ) >= 64))) {
			MultiTileEntityRegistry tRegistry = MultiTileEntityRegistry.getRegistry("gt.multitileentity");
			if (tRegistry != null) {
				for (int i = 0, j = 1+aRandom.nextInt(3); i < j; i++) {
					int tX = aMinX + aRandom.nextInt(16), tZ = aMinZ + aRandom.nextInt(16);
					for (int tY = Math.min(aWorld.getHeight(), tMinY+25); tY >= tMinY-10 && tY > 0; tY--) {
						// F6: было `Block tContact = aChunk.getBlock(tX&15, tY, tZ&15)` — LevelChunk.getBlock(int,int,int)
						// удалён; реальный neo — LevelChunk.getBlockState(BlockPos):BlockState (LevelChunk.java:210).
						BlockState tContact = aChunk.getBlockState(new BlockPos(tX, tY, tZ));
						// F6: было `tContact.getMaterial().isLiquid()` — Block.getMaterial() удалён (§C5); 1:1-замена
						// «материал блока — жидкость» это реальный BlockState.liquid() (BlockBehaviour.java:586, поле
						// `liquid` напрямую наследует старое Material.isLiquid).
						if (tContact.liquid()) break;
						// PORT-TODO(F6, block-behavior: isOpaqueCube): было `if (!WD.opaque(tContact)) continue;`
						// (пропуск не-цельных блоков при спуске к поверхности). isOpaqueCube() удалён (§C2 — dead-list);
						// documented 1:1 нет (isSolidRender/canOcclude/isSolid близки, но не тождественны) — не выдумываю.
						// PORT-TODO(F6, block-behavior: getMaterial()==grass/ground/sand/rock): было
						// `if (tContact.getMaterial() != Material.grass && ... != Material.rock) break;` — индикатор
						// ставится только на травяной/земляной/песчаный/каменный грунт. Block.getMaterial() удалён,
						// а Material.grass/ground/sand/rock — F9 block-material shim (ещё не сделан; WD.floor/opq/
						// getMaterial в WD.java тоже не портированы). До готовности F9/F3 индикаторные камни
						// (косметика над жилой) не ставятся — гейтим `break` (ничего не размещаем), чтобы не
						// поставить индикатор в неверном месте. Сама генерация ЖИЛЫ (цикл ниже, WD.setOre) не затронута.
						break;
						/* PORT-TODO(F6): восстановить при готовности F9(block-material)+F3(block-behavior):
						if (!WD.opaque(tContact)) continue;
						if (tContact.getMaterial() != Material.grass && tContact.getMaterial() != Material.ground && tContact.getMaterial() != Material.sand && tContact.getMaterial() != Material.rock) break;
						if (WD.easyRep(aWorld, tX, tY+1, tZ)) tRegistry.mBlock.placeBlock(aWorld, tX, tY+1, tZ, SIDE_UNKNOWN, (short)32757, aRandom.nextInt(3)!=0?ST.save(NBT_VALUE, OP.rockGt.mat(UT.Code.select(mTop, mTop, mBottom, mBetween, mSpread), 1)):UT.NBT.make(), F, T);
						break;
						*/
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
		return T;
	}
	
	@Override
	public Config getConfigFile() {
		return ConfigsGT.WORLDGEN_GT5;
	}
}

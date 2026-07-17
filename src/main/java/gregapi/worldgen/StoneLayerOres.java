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

package gregapi.worldgen;
import net.minecraft.world.level.WorldGenLevel;
import gregapi.util.WD;

import gregapi.code.BiomeNameSet;
import gregapi.data.MT;
import gregapi.oredict.OreDictMaterial;
import gregapi.util.ST;
import gregapi.util.UT;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Random;

import static gregapi.data.CS.*;

public class StoneLayerOres {
	public boolean mGenerateIndicators;
	public int mMinY, mMaxY;
	public byte mMeta;
	public OreDictMaterial mMaterial;
	public Block mBlock;
	/** The Material Amount will determine the chance in the form of an X of U Chance. */
	public long mChance;
	public BiomeNameSet mTargetBiomes = new BiomeNameSet();
	/** No longer in use, did not work before anyways. */
	@Deprecated public ArrayList<String> mBiomes = new ArrayList<>();
	
	@SuppressWarnings("rawtypes")
	public StoneLayerOres(OreDictMaterial aMaterial, long aChance, int aMinY, int aMaxY, Collection... aBiomes) {
		this(aMaterial, T, aChance, aMinY, aMaxY, NB, 0, aBiomes);
	}
	@SuppressWarnings("rawtypes")
	public StoneLayerOres(OreDictMaterial aMaterial, long aChance, int aMinY, int aMaxY, Block aBlock, Collection... aBiomes) {
		this(aMaterial, T, aChance, aMinY, aMaxY, aBlock, 0, aBiomes);
	}
	@SuppressWarnings("rawtypes")
	public StoneLayerOres(OreDictMaterial aMaterial, long aChance, int aMinY, int aMaxY, Block aBlock, long aMeta, Collection... aBiomes) {
		this(aMaterial, T, aChance, aMinY, aMaxY, aBlock, aMeta, aBiomes);
	}
	@SuppressWarnings("rawtypes")
	public StoneLayerOres(OreDictMaterial aMaterial, boolean aGenerateIndicators, long aChance, int aMinY, int aMaxY, Collection... aBiomes) {
		this(aMaterial, aGenerateIndicators, aChance, aMinY, aMaxY, NB, 0, aBiomes);
	}
	@SuppressWarnings("rawtypes")
	public StoneLayerOres(OreDictMaterial aMaterial, boolean aGenerateIndicators, long aChance, int aMinY, int aMaxY, Block aBlock, Collection... aBiomes) {
		this(aMaterial, aGenerateIndicators, aChance, aMinY, aMaxY, aBlock, 0, aBiomes);
	}
	@SuppressWarnings("rawtypes")
	public StoneLayerOres(OreDictMaterial aMaterial, boolean aGenerateIndicators, long aChance, int aMinY, int aMaxY, Block aBlock, long aMeta, Collection... aBiomes) {
		this(aMaterial, aGenerateIndicators, aChance, aMinY, aMaxY, aBlock, aMeta, NB, 0, aBiomes);
	}
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public StoneLayerOres(OreDictMaterial aMaterial, boolean aGenerateIndicators, long aChance, int aMinY, int aMaxY, Block aBlock1, long aMeta1, Block aBlock2, long aMeta2, Collection... aBiomes) {
		mMaterial = (aMaterial != null && aMaterial.mID > 0 ? aMaterial : MT.Empty);
		mChance = UT.Code.bind(1, U, aChance);
		if (!ST.valid(aBlock1)) {aBlock1 = aBlock2; aMeta1 = aMeta2;}
		mBlock = (aBlock1 == NB ? null : aBlock1);
		mMeta = UT.Code.bind4(aMeta1);
		mGenerateIndicators = aGenerateIndicators;
		for (Collection aBiome : aBiomes) mTargetBiomes.addAll(aBiome);
		if (aMinY > aMaxY) {mMinY = aMaxY; mMaxY = aMinY;} else {mMinY = aMinY; mMaxY = aMaxY;}
	}
	
	// F6 §4.1 (decisions/F6-worldgen.md): окно Y руды задано в старом мире [0..255]; в MC26 (-64..319) РАСТЯГИВАЕТСЯ
	// sea-anchored (WD.remapY, море — якорь) + шанс масштабируется ОБРАТНО растяжению (сохранить исходное КОЛИЧЕСТВО
	// руды в зоне, §4.1 п.3 — иначе на удвоенной высоте плотность бы упала вдвое). Кэш по (minY,seaLevel) измерения:
	// считается один раз, а не на каждый из миллионов вызовов check() за чанк.
	private transient int mRemapKey = Integer.MIN_VALUE, mRemapMinY, mRemapMaxY;
	private transient long mRemapChance;
	private void ensureRemap(WorldGenLevel aWorld) {
		int tKey = aWorld.getMinY() * 1000003 + aWorld.getSeaLevel();
		if (tKey == mRemapKey) return;
		mRemapKey = tKey;
		mRemapMinY = WD.remapY(aWorld, mMinY);
		mRemapMaxY = WD.remapY(aWorld, mMaxY);
		long tOldSpan = Math.max(1, mMaxY - mMinY), tNewSpan = Math.max(1, mRemapMaxY - mRemapMinY);
		mRemapChance = UT.Code.bind(1, U, mChance * tOldSpan / tNewSpan);
	}

	@SuppressWarnings("unlikely-arg-type")
	public boolean check(StoneLayer aLayer, WorldGenLevel aWorld, int aX, int aY, int aZ, Biome aBiome, int aRandomNumber) {
		ensureRemap(aWorld);
		return aY >= mRemapMinY && aY <= mRemapMaxY && aRandomNumber           < mRemapChance && (mTargetBiomes.isEmpty() || mTargetBiomes.contains(aBiome));
	}
	@SuppressWarnings("unlikely-arg-type")
	public boolean check(StoneLayer aLayer, WorldGenLevel aWorld, int aX, int aY, int aZ, Biome aBiome, Random aRandom) {
		ensureRemap(aWorld);
		return aY >= mRemapMinY && aY <= mRemapMaxY && aRandom.nextInt((int)U) < mRemapChance && (mTargetBiomes.isEmpty() || mTargetBiomes.contains(aBiome));
	}
	@SuppressWarnings("unlikely-arg-type")
	public boolean check(StoneLayer aLayer, WorldGenLevel aWorld, int aX, int aY, int aZ, Biome aBiome) {
		ensureRemap(aWorld);
		return aY >= mRemapMinY && aY <= mRemapMaxY && RNGSUS .nextInt((int)U) < mRemapChance && (mTargetBiomes.isEmpty() || mTargetBiomes.contains(aBiome));
	}

	public boolean set(StoneLayer aLayer, WorldGenLevel aWorld, int aX, int aY, int aZ, Biome aBiome, Random aRandom) {
		if (mBlock != null) return WD.set(aWorld, aX, aY, aZ, mBlock, mMeta, 0);
		ensureRemap(aWorld);
		return aY == mRemapMinY || aY == mRemapMaxY || aRandom.nextBoolean() ? small(aLayer, aWorld, aX, aY, aZ, aBiome) : normal(aLayer, aWorld, aX, aY, aZ, aBiome);
	}
	public boolean set(StoneLayer aLayer, WorldGenLevel aWorld, int aX, int aY, int aZ, Biome aBiome) {
		if (mBlock != null) return WD.set(aWorld, aX, aY, aZ, mBlock, mMeta, 0);
		ensureRemap(aWorld);
		return aY == mRemapMinY || aY == mRemapMaxY || RNGSUS .nextBoolean() ? small(aLayer, aWorld, aX, aY, aZ, aBiome) : normal(aLayer, aWorld, aX, aY, aZ, aBiome);
	}
	public boolean normal(StoneLayer aLayer, WorldGenLevel aWorld, int aX, int aY, int aZ, Biome aBiome) {
		if (mBlock != null) return WD.set(aWorld, aX, aY, aZ, mBlock, mMeta, 0);
		return aLayer.mOre       != null && aLayer.mOre      .placeBlock(aWorld, aX, aY, aZ, SIDE_UNKNOWN, mMaterial.mID, null, F, T);
	}
	public boolean small(StoneLayer aLayer, WorldGenLevel aWorld, int aX, int aY, int aZ, Biome aBiome) {
		if (mBlock != null) return WD.set(aWorld, aX, aY, aZ, mBlock, mMeta, 0);
		return aLayer.mOreSmall  != null && aLayer.mOreSmall .placeBlock(aWorld, aX, aY, aZ, SIDE_UNKNOWN, mMaterial.mID, null, F, T);
	}
	public boolean broken(StoneLayer aLayer, WorldGenLevel aWorld, int aX, int aY, int aZ, Biome aBiome) {
		if (mBlock != null) return WD.set(aWorld, aX, aY, aZ, mBlock, mMeta, 0);
		return aLayer.mOreBroken != null && aLayer.mOreBroken.placeBlock(aWorld, aX, aY, aZ, SIDE_UNKNOWN, mMaterial.mID, null, F, T);
	}
}

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
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.WorldGenLevel;

import static gregapi.data.CS.*;

import java.util.List;
import java.util.Random;
import java.util.Set;

import gregapi.oredict.OreDictManager;
import gregapi.oredict.OreDictMaterial;
import gregapi.util.WD;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.LevelChunk;

/**
 * @author Gregorius Techneticies
 */
public class WorldgenOresSmall extends WorldgenObject {
	public final short mMinY, mMaxY, mAmount;
	public final OreDictMaterial mMaterial;
	
	@SafeVarargs
	public WorldgenOresSmall(String aName, boolean aDefault, int aMinY, int aMaxY, int aAmount, OreDictMaterial aPrimary, List<WorldgenObject>... aLists) {
		super(aName, aDefault, aLists);
		mMinY               = (short)                   getConfigFile().get(mCategory, "MinHeight"   , aMinY);
		mMaxY               = (short)Math.max(mMinY+1,  getConfigFile().get(mCategory, "MaxHeight"   , aMaxY));
		mAmount             = (short)Math.max(1,        getConfigFile().get(mCategory, "Amount"      , aAmount));
		mMaterial           =                           getConfigFile().get(mCategory, "Ore"         , aPrimary);
		
		if (mEnabled && mMaterial.mID > 0) OreDictManager.INSTANCE.triggerVisibility("ore"+mMaterial.mNameInternal);
		
		if (mMaterial.mID <= 0) {
			ERR.println("The Material is not valid for Ores: " + mMaterial);
			mInvalid = T;
		}
	}
	
	@Override
	public boolean generate(WorldGenLevel aWorld, ChunkAccess aChunk, int aDimType, int aMinX, int aMinZ, int aMaxX, int aMaxZ, Random aRandom, Biome[][] aBiomes, Set<String> aBiomeNames) {
		if (GENERATING_SPECIAL) return F;
		// F6 §4.1 (указание пользователя 2026-08-07): окно [mMinY..mMaxY] задано в старом мире [0..255] — растягиваем
		// sea-anchored через центр, а КОЛИЧЕСТВО домножаем на то же растяжение, чтобы плотность руды на объём осталась
		// как в 1.7.10 (окно выросло вдвое под морем → и россыпи вдвое больше). Оба ответа даёт WD, формулы здесь нет.
		int tMinY = WD.remapY(aWorld, mMinY), tMaxY = WD.remapY(aWorld, mMaxY);
		int tAmount = WD.yScaleAmount(aWorld, mMinY, mMaxY, mAmount, aRandom);
		for (int i = 0, j = Math.max(1, tAmount/2 + aRandom.nextInt(1+tAmount)/2); i < j; i++) WD.setSmallOre(aWorld, aMinX+aRandom.nextInt(16), tMinY+aRandom.nextInt(Math.max(1, tMaxY-tMinY)), aMinZ+aRandom.nextInt(16), mMaterial.mID);
		return T;
	}
}

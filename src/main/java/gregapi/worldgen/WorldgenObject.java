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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import gregapi.config.Config;
import gregapi.data.CS.ConfigsGT;
import gregapi.util.UT;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.LevelChunk;

/**
 * @author Gregorius Techneticies
 */
public abstract class WorldgenObject {
	public boolean mEnabled, mInvalid = F;
	public final String mName, mCategory;
	/**
	 * F6: было {@code Map<Integer, Boolean>} по {@code WorldProvider.dimensionId} — удалён, в neo измерение
	 * идентифицируется {@code ResourceKey<Level>} (нет числового id вообще, не только у модовых, см.
	 * NoiseGenerator.java javadoc). Ключ карты соответственно сменён на {@code ResourceKey<Level>} — сама
	 * структура (кэш "разрешено ли поколение в этом измерении") и алгоритм {@link #enabled} не изменились.
	 */
	public final Map<ResourceKey<Level>, Boolean> mDimEnabled = new HashMap<>();
	
	@SafeVarargs
	public WorldgenObject(String aName, boolean aDefault, List<WorldgenObject>... aLists) {
		if (UT.Code.stringInvalid(aName)) throw new IllegalArgumentException("The Name has to be not null and is also not allowed to be an empty String");
		mName = aName;
		mCategory = "worldgenerator."+mName;
		mEnabled = getConfigFile().get(mCategory, "Enabled", aDefault);
		for (List<WorldgenObject> aList : aLists) aList.add(this);
	}
	
	public boolean generate(Level aWorld, LevelChunk aChunk, int aDimType, int aMinX, int aMinZ, int aMaxX, int aMaxZ, Random aRandom, Biome[][] aBiomes, Set<String> aBiomeNames) {
		// Insert your WorldGen Code Here.
		return F;
	}
	
	public boolean enabled(Level aWorld, int aDimType) {
		if (mInvalid) return F;
		ResourceKey<Level> tDim = aWorld.dimension();
		Boolean tAllowed = mDimEnabled.get(tDim);
		if (tAllowed != null) return tAllowed && mEnabled;
		// F6: было `aWorld.provider.getDimensionName().replaceAll(" ", "_")` (человекочитаемое имя измерения,
		// напр. "The Nether"->"The_Nether") — WorldProvider удалён. Ключ конфига заменён на идентификатор
		// измерения ("namespace:path", напр. "minecraft:the_nether") через реальный ResourceKey.identifier()
		// (Level.java:1030, ResourceKey.java:55) — тоже стабильная человекочитаемая строка для конфиг-файла,
		// но без риска коллизии между одноимёнными измерениями разных модов.
		boolean tValue = getConfigFile().get(mCategory+".dim", tDim.identifier().toString(), T);
		mDimEnabled.put(tDim, tValue);
		return tValue && mEnabled;
	}

	public void reset(Level aWorld, LevelChunk aChunk, int aDimType, int aMinX, int aMinZ, int aMaxX, int aMaxZ, Random aRandom, Biome[][] aBiomes, Set<String> aBiomeNames) {/**/}

	public boolean checkForMajorWorldgen(Level aWorld, int aMinX, int aMinZ, int aMaxX, int aMaxZ) {
		// F6: было `aWorld.provider.dimensionId == DIM_OVERWORLD` (DIM_OVERWORLD=0, CS.java:904, буквально
		// ванильный Overworld-id) — сверено на реальную константу Level.OVERWORLD (Level.java:95).
		if (aWorld.dimension() == Level.OVERWORLD) {
			if (GENERATE_STREETS && (Math.abs(aMinX) < 64 || Math.abs(aMaxX) < 64 || Math.abs(aMinZ) < 64 || Math.abs(aMaxZ) < 64)) return T;
			if (GENERATE_BIOMES && aMinX >= -96 && aMinX <= 80 && aMinZ >= -96 && aMinZ <= 80) return T;
		}
		return F;
	}
	
	public Config getConfigFile() {
		return ConfigsGT.WORLDGEN;
	}
}

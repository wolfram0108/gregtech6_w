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

package gregapi.code;

import static gregapi.data.CS.*;

import java.util.AbstractSet;
import java.util.HashMap;
import java.util.Iterator;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;

/**
 * @author Gregorius Techneticies
 */
public class BiomeNameSet extends AbstractSet<String> {
	private transient HashMap<String, Object> map;
	private static final Object OBJECT = new Object();

	/** 1.7.10's human-readable Biome.biomeName field is gone; neo biomes are data-driven with no display-name
	 *  field of their own, so identity is the registry key instead; a bare Biome with no Holder/ResourceKey returns "". */
	public static String biomeKeyName(Object aName) {
		if (aName instanceof Holder<?> aHolder) return aHolder.unwrapKey().map(k -> k.location().toString()).orElse("");
		if (aName instanceof ResourceKey<?> aKey) return aKey.location().toString();
		if (aName instanceof Biome aBiome) return keyOfBiome(aBiome);
		return aName.toString();
	}

	/** Resolves the key directly from the live registry since a bare Biome has no back-reference; the previous
	 *  unconditional empty return silently broke every caller lacking a Holder (hay decay, bee compass, rock id). */
	private static final java.util.Map<Biome, String> BIOME_KEY_CACHE = new java.util.WeakHashMap<>();
	public static String keyOfBiome(Biome aBiome) {
		if (aBiome == null) return "";
		synchronized (BIOME_KEY_CACHE) {
			String rCached = BIOME_KEY_CACHE.get(aBiome);
			if (rCached != null) return rCached;
		}
		String rKey = "";
		try {
			net.minecraft.server.MinecraftServer tServer = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
			if (tServer != null) rKey = tServer.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.BIOME).getResourceKey(aBiome).map(k -> k.location().toString()).orElse("");
		} catch (Throwable e) {rKey = "";}
		if (!rKey.isEmpty()) synchronized (BIOME_KEY_CACHE) {BIOME_KEY_CACHE.put(aBiome, rKey);}
		return rKey;
	}

	@SafeVarargs
	public BiomeNameSet(Object... aArray) {
		map = new HashMap<>(Math.max((int)(aArray.length/.75F) + 1, 16));
		for (Object aName : aArray) add(biomeKeyName(aName));
	}

	@Override
	public boolean contains(Object aName) {
		if (aName == null) return F;
		String aString = biomeKeyName(aName);
		if (aString == null || aString.isEmpty()) return F;
		return map.containsKey(aString.toLowerCase());
	}
	
	@Override
	public boolean add(String aName) {
		if (aName == null || aName.isEmpty()) return F;
		aName = aName.toLowerCase();
		if (!aName.endsWith(" m")) add(aName + " m");
		return map.put(aName, OBJECT) == null;
	}
	
	@Override public Iterator<String> iterator() {return map.keySet().iterator();}
	@Override public boolean remove(Object aName) {return map.remove(aName)==OBJECT;}
	@Override public boolean isEmpty() {return map.isEmpty();}
	@Override public int size() {return map.size();}
	@Override public void clear() {map.clear();}
}

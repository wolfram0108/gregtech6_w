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

	/**
	 * F6: было {@code ((BiomeGenBase)aName).biomeName} — поле {@code Biome.biomeName} (человекочитаемое имя
	 * биома 1.7.10) удалено, биомы в neo data-driven и собственного display-name поля не несут. Идентичность
	 * биома в neo — реестровый ключ ({@code identifier().toString()}, "namespace:path"), тот же приём, что
	 * уже применяет диспетчер {@code GT6WorldGenerator.java:96-98,204-205}: {@code Holder<Biome>.unwrapKey()}
	 * (Holder.java:40) -> {@code ResourceKey<Biome>.location()} (ResourceKey.java:52) -> {@code
	 * Identifier.toString()} (Identifier.java:126, "namespace:path"). {@code ResourceKey<Biome>} напрямую
	 * (как датаген-константы {@code net.minecraft.world.level.biome.Biomes.RIVER/...}, используемые в
	 * {@code CS.java}-наборах {@code BIOMES_*}) резолвится тем же {@code identifier()}.
	 * F6 (документация biomeName-семантики): голый {@code Biome}-инстанс без {@code Holder}/{@code ResourceKey}
	 * (передаётся {@code StoneLayerOres.check/set(...,Biome aBiome,...)} — {@code aBiome} там берётся из
	 * {@code tBiomes[i][j]=tBiomeHolder.value()} в {@code GT6WorldGenerator.java}, т.е. Holder уже развёрнут
	 * и потерян до вызова) не несёт обратной ссылки на реестровый ключ ни в одном из 3 корней референса
	 * ({@code Biome} — {@code final class} без registry-back-ref-поля/метода); ключ взять НЕГДЕ без
	 * {@code RegistryAccess}-поиска по значению в этой точке — форс движка, возвращаем "" (не угадываем API).
	 * Протяжка {@code Holder<Biome>} до вызова ИССЛЕДОВАНА (не пропущена): {@code tBiomes[][]} — параметр
	 * {@code WorldgenObject.generate/reset(...,Biome[][] aBiomes,...)} (`gregapi/worldgen/WorldgenObject.java:
	 * 61,81`), виртуально переопределённый ~50 leaf-worldgen классами (`gregtech/worldgen/**`, grep
	 * `Biome[][]|Biome aBiome` по дереву = 55 файлов) — смена типа элемента на {@code Holder<Biome>} ломает
	 * ПУБЛИЧНУЮ сигнатуру базового класса и ВСЕ 50 override разом; тот же самый "хвост" уже сознательно
	 * отложен отдельным чекпоинтом (`STATE.md`/`DEFERRED-LEDGER.md` §F6 "Хвост F6": "~40 leaf-worldgen
	 * классов ... их generate-сигнатуры не тронуты") — не расширяем сейчас поверх границы этой задачи.
	 */
	public static String biomeKeyName(Object aName) {
		if (aName instanceof Holder<?> aHolder) return aHolder.unwrapKey().map(k -> k.location().toString()).orElse("");
		if (aName instanceof ResourceKey<?> aKey) return aKey.location().toString();
		if (aName instanceof Biome aBiome) return keyOfBiome(aBiome);
		return aName.toString();
	}

	/**
	 * КЛЮЧ ГОЛОГО {@code Biome} — поиском по живому реестру.
	 *
	 * <p>Раньше здесь стояло {@code return ""}, и это делало МЁРТВЫМИ все ветки, куда биом приходит без
	 * {@code Holder}: {@code WD.biome(...)} отдаёт {@code .value()} (WD.java:1185-1186), поэтому
	 * {@code contains(...)} возвращал false ВСЕГДА. Так молча не работали: влажность биома для гниения сена
	 * ({@code BlockBaleGrass:113}), поиск магического биома и Энда компасом пчелы
	 * ({@code MultiItemBumbles:234,266}), гены пчёл по пустыне/меса ({@code IItemBumbleBee:141}), опознание
	 * лунного, марсианского и космического камня ({@code MultiTileEntityRock:105-107,189,231}).
	 *
	 * <p>Обратной ссылки на ключ у {@code Biome} действительно нет ({@code final class} без registry-back-ref),
	 * но сам реестр её знает: {@code registryAccess().lookupOrThrow(Registries.BIOME).getResourceKey(value)}.
	 * Реестр берём у текущего сервера ({@code ServerLifecycleHooks.getCurrentServer()}); вне сервера (датаген,
	 * ранняя загрузка) отвечаем "" — прежним поведением, а не выдумкой. Результат кэшируется: вызовы идут из
	 * тиков блоков.
	 */
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
			if (tServer != null) rKey = tServer.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.BIOME).getResourceKey(aBiome).map(k -> k.location().toString()).orElse("");
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

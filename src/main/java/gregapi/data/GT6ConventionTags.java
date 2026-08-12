/**
 * Copyright (c) 2026 wolfram0108
 *
 * Written in 2026 for the GregTech 6 NeoForge port
 * (https://github.com/wolfram0108/gregtech6_w). Not part of the original GregTech 6
 * by Gregorius Techneticies; distributed under the same licence as the work it extends.
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

package gregapi.data;

import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

import gregapi.block.prefixblock.PrefixBlock;
import gregapi.item.IPrefixItem;
import gregapi.oredict.OreDictPrefix;
import gregapi.oredict.OreDictTags;

/**
 * F1-b тег-мост, ИСХОДЯЩАЯ сторона — предметы и блоки GT6 в материал-агностичных конвенционных тегах
 * {@code c:} (тот же приём, что {@link GT6HarvestTags} для добычи и {@link GT6ItemTags} для боеприпаса:
 * данные у GT6 уже есть, меняется лишь канал, которым их читает движок).
 *
 * <p><b>Почему только группы, без материала.</b> Тег вешается на ЗАПИСЬ РЕЕСТРА, а по решению F1 (модель B)
 * у GT6 ОДИН {@code Item} на префикс — материал живёт компонентом стека ({@code PrefixItem.java:118}).
 * Значит {@code c:ingots} («это слиток») про наш {@code gt.meta.ingot} — ТОЧНАЯ правда, а
 * {@code c:ingots/tin} был бы ложью про все остальные материалы того же предмета. По-материальные теги мост
 * добирает другим путём — через собственную унификацию Грега (см. {@link OreDictTags}); выдумывать их здесь
 * запрещено.
 *
 * <p><b>Перенос, а не список.</b> Провайдер обходит реестры и спрашивает у каждого GT6-предмета/блока ЕГО
 * ЖЕ префикс ({@code IPrefixItem.getPrefix}, {@code PrefixBlock.mPrefix}) — тот самый, которым его судит
 * словарь. Таблица «префикс → тег» одна и лежит в центре ({@code OreDictTags.groupItemTag/groupBlockTag});
 * второй копии в дереве быть не должно. Новый префикс попадёт в тег сам, без правки данных.
 */
public class GT6ConventionTags extends TagsProvider<Item> {
	public GT6ConventionTags(PackOutput aOutput, CompletableFuture<HolderLookup.Provider> aLookup) {
		super(aOutput, Registries.ITEM, aLookup, MD.GAPI.mID);
	}

	@Override
	public String getName() {return "GT6 Convention Tags (items)";}

	@Override
	protected void addTags(HolderLookup.Provider aProvider) {
		TreeMap<String, Integer> tCounts = new TreeMap<>();
		for (Item tItem : BuiltInRegistries.ITEM) {
			OreDictPrefix tPrefix = prefixOf(tItem);
			if (tPrefix == null) continue;
			TagKey<Item> tTag = OreDictTags.groupItemTag(tPrefix);
			if (tTag == null) continue;
			Identifier tID = BuiltInRegistries.ITEM.getKey(tItem);
			if (tID == null) continue;
			getOrCreateRawBuilder(tTag).addElement(tID);
			tCounts.merge(tTag.location().toString(), 1, Integer::sum);
		}
		CS.OUT.println("[GT6-DATAGEN] F1-b тег-мост наружу, предметы: " + tCounts);
	}

	/** Префикс предмета — ЕГО ЖЕ канал ({@code PrefixItem.java:261}, {@code PrefixBlockItem.java:224}).
	 *  Мета не участвует: у обоих носителей {@code getPrefix} от неё не зависит, а на фазе датагена стеки
	 *  создавать нельзя (то же ограничение, что записано в {@code GT6ItemTags.java:89-95}). */
	private static OreDictPrefix prefixOf(Item aItem) {
		try {
			if (aItem instanceof IPrefixItem tPrefixed) return tPrefixed.getPrefix(0);
		} catch (Throwable e) {/* предмет без канала — просто не размечаем */}
		return null;
	}

	/**
	 * Блочная половина того же моста: {@code c:ores} и {@code c:storage_blocks} существуют и для блоков
	 * ({@code Tags.java:201,280}). Отдельный класс, потому что {@code TagsProvider} типизирован реестром,
	 * а не потому, что правило другое — правило одно и лежит в {@link OreDictTags}.
	 */
	public static class Blocks extends TagsProvider<Block> {
		public Blocks(PackOutput aOutput, CompletableFuture<HolderLookup.Provider> aLookup) {
			super(aOutput, BuiltInRegistries.BLOCK.key(), aLookup, MD.GAPI.mID);
		}

		@Override
		public String getName() {return "GT6 Convention Tags (blocks)";}

		@Override
		protected void addTags(HolderLookup.Provider aProvider) {
			TreeMap<String, Integer> tCounts = new TreeMap<>();
			for (Block tBlock : BuiltInRegistries.BLOCK) {
				if (!(tBlock instanceof PrefixBlock tPrefixed)) continue;
				TagKey<Block> tTag = OreDictTags.groupBlockTag(tPrefixed.mPrefix);
				if (tTag == null) continue;
				Identifier tID = BuiltInRegistries.BLOCK.getKey(tBlock);
				if (tID == null) continue;
				getOrCreateRawBuilder(tTag).addElement(tID);
				tCounts.merge(tTag.location().toString(), 1, Integer::sum);
			}
			CS.OUT.println("[GT6-DATAGEN] F1-b тег-мост наружу, блоки: " + tCounts);
		}
	}
}

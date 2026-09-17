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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

import gregapi.block.prefixblock.PrefixBlock;
import gregapi.item.IPrefixItem;
import gregapi.oredict.OreDictPrefix;
import gregapi.oredict.OreDictTags;

/** Tag bridge, outgoing side: puts GT6 items/blocks into material-agnostic forge: convention
 *  tags. Only by group, never by material: GT6 has one Item per prefix, and a per-material tag would misname the rest. */
public class GT6ConventionTags extends TagsProvider<Item> {
	public GT6ConventionTags(PackOutput aOutput, CompletableFuture<HolderLookup.Provider> aLookup) {
		super(aOutput, Registries.ITEM, aLookup, MD.GAPI.mID, null);
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
			ResourceLocation tID = BuiltInRegistries.ITEM.getKey(tItem);
			if (tID == null) continue;
			getOrCreateRawBuilder(tTag).addElement(tID);
			tCounts.merge(tTag.location().toString(), 1, Integer::sum);
		}
		CS.OUT.println("[GT6-DATAGEN] F1-b тег-мост наружу, предметы: " + tCounts);
	}

	/** A prefix comes from the item's own channel (IPrefixItem.getPrefix/PrefixBlock.mPrefix); meta is
	 *  irrelevant since neither carrier's getPrefix depends on it, and stacks can't be built at datagen time. */
	private static OreDictPrefix prefixOf(Item aItem) {
		try {
			if (aItem instanceof IPrefixItem tPrefixed) return tPrefixed.getPrefix(0);
		} catch (Throwable e) {/* an item with no channel is simply left untagged */}
		return null;
	}

	/** Block half of the same bridge: forge:ores and forge:storage_blocks exist for blocks too; a
	 *  separate class only because TagsProvider is typed by registry, not because the rule differs. */
	public static class Blocks extends TagsProvider<Block> {
		public Blocks(PackOutput aOutput, CompletableFuture<HolderLookup.Provider> aLookup) {
			super(aOutput, BuiltInRegistries.BLOCK.key(), aLookup, MD.GAPI.mID, null);
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
				ResourceLocation tID = BuiltInRegistries.BLOCK.getKey(tBlock);
				if (tID == null) continue;
				getOrCreateRawBuilder(tTag).addElement(tID);
				tCounts.merge(tTag.location().toString(), 1, Integer::sum);
			}
			CS.OUT.println("[GT6-DATAGEN] F1-b тег-мост наружу, блоки: " + tCounts);
		}
	}
}

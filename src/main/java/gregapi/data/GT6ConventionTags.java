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
 * F1-b tag bridge, the OUTGOING side — GT6 items and blocks into material-agnostic convention tags
 * {@code c:} (the same approach as {@link GT6HarvestTags} for harvesting and {@link GT6ItemTags} for ammunition:
 * GT6 already has the data, only the channel the engine reads it through changes).
 *
 * <p><b>Why groups only, no material.</b> The tag hangs off a REGISTRY ENTRY, and per the F1 decision (model B)
 * GT6 has ONE {@code Item} per prefix — the material lives as a stack component ({@code PrefixItem.java:118}).
 * So {@code c:ingots} ("this is an ingot") about our {@code gt.meta.ingot} is EXACTLY true, while
 * {@code c:ingots/tin} would be a lie about every other material of the same item. Per-material tags are covered
 * by the bridge a different way — through Greg's own unification (see {@link OreDictTags}); inventing them
 * here is forbidden.
 *
 * <p><b>A transfer, not a list.</b> The provider walks the registries and asks each GT6 item/block for ITS
 * OWN prefix ({@code IPrefixItem.getPrefix}, {@code PrefixBlock.mPrefix}) — the very same one the ore
 * dictionary judges it by. The "prefix → tag" table is singular and lives in the center ({@code OreDictTags.groupItemTag/groupBlockTag});
 * there must be no second copy in the tree. A new prefix falls into the tag on its own, with no data edits needed.
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
		CS.OUT.println("[GT6-DATAGEN] F1-b outward tag bridge, items: " + tCounts);
	}

	/** An item's prefix — ITS OWN channel ({@code PrefixItem.java:261}, {@code PrefixBlockItem.java:224}).
	 *  Meta doesn't participate: for both carriers {@code getPrefix} doesn't depend on it, and at the datagen phase stacks
	 *  can't be created (the same restriction recorded in {@code GT6ItemTags.java:89-95}). */
	private static OreDictPrefix prefixOf(Item aItem) {
		try {
			if (aItem instanceof IPrefixItem tPrefixed) return tPrefixed.getPrefix(0);
		} catch (Throwable e) {/* an item with no channel — simply don't tag it */}
		return null;
	}

	/**
	 * The block half of the same bridge: {@code c:ores} and {@code c:storage_blocks} exist for blocks too
	 * ({@code Tags.java:201,280}). A separate class because {@code TagsProvider} is typed by the registry,
	 * not because the rule differs — the rule is singular and lives in {@link OreDictTags}.
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
			CS.OUT.println("[GT6-DATAGEN] F1-b outward tag bridge, blocks: " + tCounts);
		}
	}
}

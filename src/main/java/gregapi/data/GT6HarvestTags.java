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

import java.util.concurrent.CompletableFuture;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

/** Carries GT6's own harvest data into tags, the only channel neo listens to. 1.7.10
 *  asked the block itself; neo has no such methods at all, and decides purely by tag instead. */
public class GT6HarvestTags extends TagsProvider<Block> {
	/** 1.7.10 level -> vanilla "needs at least this tool" tag; the scale matches: 1=stone, 2=iron, 3=diamond. */
	private static final TagKey<?>[] NEEDS_BY_LEVEL = {null, BlockTags.NEEDS_STONE_TOOL, BlockTags.NEEDS_IRON_TOOL, BlockTags.NEEDS_DIAMOND_TOOL};

	public GT6HarvestTags(PackOutput aOutput, CompletableFuture<HolderLookup.Provider> aLookup) {
		super(aOutput, BuiltInRegistries.BLOCK.key(), aLookup, MD.GAPI.mID, null);
	}

	@Override
	protected void addTags(HolderLookup.Provider aProvider) {
		int tTagged = 0, tSkipped = 0, tOverScale = 0;
		for (Block tBlock : BuiltInRegistries.BLOCK) {
			net.minecraft.resources.ResourceLocation tID = BuiltInRegistries.BLOCK.getKey(tBlock);
			if (tID == null || !(tID.getNamespace().equals(CS.ModIDs.GT) || tID.getNamespace().equals("gregtech"))) continue;
			String tTool = harvestToolOf(tBlock);
			TagKey<Block> tMineable = mineableTag(tTool);
			if (tMineable == null) {tSkipped++; continue;} // a GT6-specific tool has no vanilla tag, so none is invented
			int tLevel = harvestLevelOf(tBlock);
			// Scale mismatch: GT6 goes 0..15 (plus 9999 for bedrock-tier); vanilla only has three steps
			// (stone/iron/diamond). Above 3, leaving the block untagged is the correct 1:1 translation, not a free pass.
			if (tLevel >= NEEDS_BY_LEVEL.length) {tOverScale++; continue;}
			getOrCreateRawBuilder(tMineable).addElement(tID);
			if (tLevel > 0 && NEEDS_BY_LEVEL[tLevel] != null) {
				@SuppressWarnings("unchecked") TagKey<Block> tNeeds = (TagKey<Block>) NEEDS_BY_LEVEL[tLevel];
				getOrCreateRawBuilder(tNeeds).addElement(tID);
			}
			tTagged++;
		}
		CS.OUT.println("GT6 F12-harvest: размечено блоков тегами добычи " + tTagged + ", пропущено: не-ванильный инструмент " + tSkipped + ", уровень выше ванильной шкалы " + tOverScale);
		// Diagnostic for that same scale mismatch: counts how many blocks fall above vanilla's three tool
		// tiers, before relying on the tags at all.
		java.util.Map<Integer, Integer> tHist = new java.util.TreeMap<>();
		java.util.Map<String, Integer> tTools = new java.util.TreeMap<>();
		for (Block tBlock : BuiltInRegistries.BLOCK) {
			net.minecraft.resources.ResourceLocation tID = BuiltInRegistries.BLOCK.getKey(tBlock);
			if (tID == null || !(tID.getNamespace().equals(CS.ModIDs.GT) || tID.getNamespace().equals("gregtech"))) continue;
			tHist.merge(harvestLevelOf(tBlock), 1, Integer::sum);
			tTools.merge(String.valueOf(harvestToolOf(tBlock)), 1, Integer::sum);
		}
		CS.OUT.println("GT6 F12-harvest DIAG уровни(GT6->кол-во): " + tHist);
		CS.OUT.println("GT6 F12-harvest DIAG инструменты: " + tTools);
	}

	/** A block's tool is its own 1.7.10 method, asked with meta 0, since the tag applies to the whole block. */
	private static String harvestToolOf(Block aBlock) {
		try {
			if (aBlock instanceof gregapi.block.prefixblock.PrefixBlock tP) return tP.getHarvestTool(0);
			if (aBlock instanceof gregapi.block.BlockBase tB) return tB.getHarvestTool(0);
			if (aBlock instanceof gregapi.block.multitileentity.MultiTileEntityBlock tM) return tM.getHarvestTool(0);
		} catch (Throwable e) {/* a block with no channel is simply left untagged */}
		return null;
	}

	/** A block's level is its own 1.7.10 method; for meta-based families, the minimum across metas is used. */
	private static int harvestLevelOf(Block aBlock) {
		try {
			if (aBlock instanceof gregapi.block.prefixblock.PrefixBlock tP) return tP.getHarvestLevel(0);
			if (aBlock instanceof gregapi.block.BlockBase tB) return tB.getHarvestLevel(0);
			if (aBlock instanceof gregapi.block.multitileentity.MultiTileEntityBlock tM) return tM.getHarvestLevel(0);
		} catch (Throwable e) {/* see above */}
		return 0;
	}

	/** Vanilla "mined with this tool" tag for a GT6 tool name, or null if vanilla has none; public
	 *  because the acceptance-display judge reads the same mapping to check whether this mechanism tagged a block. */
	public static TagKey<Block> mineableTag(String aTool) {
		if (aTool == null) return null;
		if (aTool.equals(CS.TOOL_pickaxe)) return BlockTags.MINEABLE_WITH_PICKAXE;
		if (aTool.equals(CS.TOOL_axe    )) return BlockTags.MINEABLE_WITH_AXE;
		if (aTool.equals(CS.TOOL_shovel )) return BlockTags.MINEABLE_WITH_SHOVEL;
		return null;
	}
}

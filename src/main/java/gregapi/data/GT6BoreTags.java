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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;

import gregapi.block.metatype.BlockBasePlanks;
import gregapi.block.metatype.BlockStones;
import gregapi.block.misc.BlockBaseFlower;
import gregapi.block.misc.BlockBaseLilyPad;
import gregapi.block.prefixblock.PrefixBlock;
import gregapi.block.tree.BlockBaseBeam;
import gregapi.block.tree.BlockBaseSapling;
import gregapi.block.tree.BlockBaseTree;
import gregtech.blocks.BlockDiggable;
import gregtech.blocks.BlockSands;
import gregtech.blocks.stone.BlockCrystalOres;
import gregtech.blocks.stone.BlockRockOres;
import gregtech.blocks.stone.BlockVanillaOresA;

/**
 * Railcraft's tunnel bore, the same carry-over as {@link GT6HarvestTags}: the data stays GT6's, the channel the engine reads it through changes.
 *
 * <p>In 1.7.10 the bore asked a list ({@code EntityTunnelBore.addMineableBlock}) that 13 GT6 block classes filled from their own constructors.
 * Railcraft Reborn has no such list: with {@code boreMinesAllBlocks=false} it mines what the block tag
 * {@code railcraft:tunnel_bore_mineable_blocks} names ({@code TunnelBore.isMineableBlock}). This provider writes GT6's part of that tag: every registered
 * instance of those 13 classes. A class is asked by {@code instanceof}, as its constructor was asked by inheritance, so a subclass and an addon's block
 * land in the tag the way they landed in the list. The file only adds ({@code replace} stays false), Reborn's own entries remain.
 *
 * <p><b>Honesty boundary.</b> A tag names a whole block, while the 1.7.10 list also named metas: {@code BlockStones} offered only COBBL, STONE and MCOBL.
 * Here the meta is a state of one block, so the whole block is tagged and its other variants (smooth, bricks and so on) become mineable by the bore too.
 * Leaving the block out would instead take the plain stones from the bore, which is the bigger loss.
 */
public class GT6BoreTags extends TagsProvider<Block> {
	/** Reborn's tag (RailcraftTags.TUNNEL_BORE_MINEABLE_BLOCKS); the namespace is the carrier id of the Railcraft table. */
	public static final net.minecraft.tags.TagKey<Block> TUNNEL_BORE_MINEABLE_BLOCKS =
		net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.BLOCK, Identifier.parse(CS.ModIDs.RC_REBORN + ":tunnel_bore_mineable_blocks"));

	/** The 13 classes whose constructors called addMineableBlock in the original. */
	private static final Class<?>[] BORE_CLASSES = {
		BlockBasePlanks.class, BlockStones.class, BlockBaseFlower.class, BlockBaseLilyPad.class, PrefixBlock.class, BlockBaseBeam.class, BlockBaseSapling.class,
		BlockBaseTree.class, BlockDiggable.class, BlockSands.class, BlockVanillaOresA.class, BlockRockOres.class, BlockCrystalOres.class};

	public GT6BoreTags(PackOutput aOutput, CompletableFuture<HolderLookup.Provider> aLookup) {
		super(aOutput, BuiltInRegistries.BLOCK.key(), aLookup, MD.GAPI.mID);
	}

	@Override
	public String getName() {return "GT6 Tunnel Bore Tags";}

	@Override
	protected void addTags(HolderLookup.Provider aProvider) {
		Map<String, Integer> tCounts = new LinkedHashMap<>();
		for (Class<?> tClass : BORE_CLASSES) tCounts.put(tClass.getSimpleName(), 0);
		net.minecraft.tags.TagBuilder tBuilder = getOrCreateRawBuilder(TUNNEL_BORE_MINEABLE_BLOCKS);
		int tTagged = 0;
		for (Block tBlock : BuiltInRegistries.BLOCK) {
			Identifier tID = BuiltInRegistries.BLOCK.getKey(tBlock);
			if (tID == null) continue;
			boolean tIn = false;
			for (Class<?> tClass : BORE_CLASSES) if (tClass.isInstance(tBlock)) {tIn = true; tCounts.merge(tClass.getSimpleName(), 1, Integer::sum);}
			if (tIn) {tBuilder.addElement(tID); tTagged++;}
		}
		// The 1.7.10 Compat listener also offered every foreign plank to the bore (OP.plank); Reborn's tag has logs, leaves and saplings but no planks.
		tBuilder.addTag(Identifier.withDefaultNamespace("planks"));
		CS.OUT.println("[GT6-DATAGEN] tunnel bore tag: GT6 blocks " + tTagged + " by class " + tCounts + ", plus #minecraft:planks");
	}
}

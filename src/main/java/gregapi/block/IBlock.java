/**
 * Copyright (c) 2019 Gregorius Techneticies
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

package gregapi.block;

import net.minecraft.world.level.block.Block;


/**
 * @author Gregorius Techneticies
 */
public interface IBlock {
	public Block getBlock();
	/** 1.7.10 mutated the block's bounding box directly for both render and collision; neo bounds are
	 *  immutable, so the block stores its own last-set copy instead, with real render use deferred to a later client pass. */
	public void setBlockBounds(float aMinX, float aMinY, float aMinZ, float aMaxX, float aMaxY, float aMaxZ);
	/** The last render-bounds set, read back by GT6BlockModel.applyBounds; a single contract shared
	 *  across every GT6 block hierarchy, since they have no common ancestor of their own. */
	public float[] getRenderBounds();
	/** 1.7.10 kept getIcon(side, meta) on vanilla Block itself, so every block had it by definition;
	 *  neo removed that along with IIcon, so this restores the contract since GT6's hierarchies share no common Block ancestor. */
	default net.minecraft.resources.ResourceLocation getIcon(int aSide, int aMeta) {return null;}
	/** Pairs with getIcon: 1.7.10 had both a per-meta tint and a per-position tint on Block itself,
	 *  both removed from neo; the default here is white, meaning no tint of its own, same as vanilla's default. */
	default int getRenderColor(int aMeta) {return 0x00ffffff;}
	default int colorMultiplier(net.minecraft.world.level.BlockGetter aWorld, int aX, int aY, int aZ) {return getRenderColor(gregapi.util.WD.meta(aWorld, aX, aY, aZ));}
	/** 1.7.10's Block.getRenderBlockPass() (0=cutout, 1=translucent) lived on vanilla Block and GT6 overrode it;
	 *  1.20.1 reads this contract from the baked model. Declared here since GT6's hierarchies share no Block ancestor. */
	default int getRenderBlockPass() {return 0;}
	/** 1.7.10 Forge kept the harvest-tool query on Block itself, answered per hierarchy from its own
	 *  fields; neo removed that point, so this restores it on the shared IBlock contract instead. */
	default String getHarvestTool(int aMeta) {return "";}
	/** Defaults to -1, not 0, matching 1.7.10's own "level not set" value exactly; callers already
	 *  clamp it or compare it the same way the original did, where -1 correctly means no requirement. */
	default int getHarvestLevel(int aMeta) {return -1;}
	/** This is an active tool-requirement rule for GT6, not decoration; the center used to select carriers by enumerating
	 *  hierarchies and missed PrefixBlock's 27 blocks, so selection now goes by this shared contract instead. */
	default Material getMaterial() {return null;}
	/** 1.7.10's block meta carried the material's tool-quality level directly; here that meta slot is occupied by something
	 *  else, so this contract restores the link, overridden by any hierarchy whose meta means something different. */
	default int getHarvestLevel(net.minecraft.world.level.BlockGetter aWorld, int aX, int aY, int aZ) {
		return getHarvestLevel(gregapi.util.WD.meta(aWorld, aX, aY, aZ));
	}
	/** A Forge per-position hardness point that GT6 hierarchies answer with their own per-meta,
	 *  per-material, or per-tile values; the default falls back to vanilla's own destroy time. */
	default float getBlockHardness(net.minecraft.world.level.Level aWorld, int aX, int aY, int aZ) {
		net.minecraft.core.BlockPos tPos = new net.minecraft.core.BlockPos(aX, aY, aZ);
		return aWorld.getBlockState(tPos).getDestroySpeed(aWorld, tPos);
	}
}

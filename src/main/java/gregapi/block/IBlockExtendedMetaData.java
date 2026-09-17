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

import net.minecraft.world.level.BlockGetter;

/**
 * @author Gregorius Techneticies
 */
public interface IBlockExtendedMetaData {
	/** The meta-to-BlockState routing for every META-property family lives here once, instead of each
	 *  carrier's separate copy; only carriers with a genuinely different layout (TE-meta, rails, fluids) override it. */
	public default void setExtendedMetaData(BlockGetter aWorld, int aX, int aY, int aZ, short aMetaData) {
		net.minecraft.core.BlockPos tPos = new net.minecraft.core.BlockPos(aX, aY, aZ);
		net.minecraft.world.level.block.state.BlockState tState = aWorld.getBlockState(tPos);
		if (tState.getBlock() != this) return;
		net.minecraft.world.level.block.state.BlockState tNew = getStateForExtendedMetaData(tState, aMetaData);
		if (tNew == null) return;
		if (aWorld instanceof net.minecraft.world.level.LevelAccessor tLA) tLA.setBlock(tPos, tNew, 3);
		else if (aWorld instanceof net.minecraft.world.level.chunk.ChunkAccess tChunk) tChunk.setBlockState(tPos, tNew, false);
	}
	public default short getExtendedMetaData(BlockGetter aWorld, int aX, int aY, int aZ) {
		net.minecraft.world.level.block.state.BlockState tState = aWorld.getBlockState(new net.minecraft.core.BlockPos(aX, aY, aZ));
		return tState.getBlock() == this ? getExtendedMetaData(tState) : 0;
	}

	/** Reads meta from the BlockState snapshot, not the world, because neo removes the block before drops run; families whose
	 *  meta lives in a different property (rails, fluids) override this, and TE-meta carriers default to 0. */
	public default short getExtendedMetaData(net.minecraft.world.level.block.state.BlockState aState) {
		return aState.hasProperty(gregapi.block.BlockBaseMeta.META) ? (short)(int)aState.getValue(gregapi.block.BlockBaseMeta.META) : 0;
	}

	/** Builds the state with metadata already set for one atomic setBlock call, since a separate two-phase write broke blocks
	 *  whose onPlace reads meta right away, like rails; null means this family's meta isn't state-expressible. */
	public default net.minecraft.world.level.block.state.BlockState getStateForExtendedMetaData(net.minecraft.world.level.block.state.BlockState aBase, short aMetaData) {
		return aBase.hasProperty(gregapi.block.BlockBaseMeta.META) ? aBase.setValue(gregapi.block.BlockBaseMeta.META, (int)gregapi.util.UT.Code.bind4(aMetaData)) : null;
	}
}

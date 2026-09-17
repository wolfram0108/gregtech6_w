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
 *
 * Modified in 2026 for the GregTech 6 NeoForge port
 * (https://github.com/wolfram0108/gregtech6_w): ported from Minecraft 1.7.10 / Forge
 * to Minecraft 26.1.2 / NeoForge.
 */

package gregapi.block.tree;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.SoundType;
import gregapi.util.WD;

import static gregapi.data.CS.*;

import gregapi.block.BlockBaseMeta;
import gregapi.data.MD;
import gregapi.render.IIconContainer;
import gregapi.util.ST;
import mods.railcraft.common.carts.EntityTunnelBore;
import net.minecraft.world.level.block.Block;
import gregapi.block.Material;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.Level;

/**
 * @author Gregorius Techneticies
 */
public abstract class BlockBaseTree extends BlockBaseMeta {
	public BlockBaseTree(Class<? extends BlockItem> aItemClass, String aNameInternal, Material aMaterial, SoundType aSoundType, long aMaxMeta, IIconContainer[] aIcons) {
		super(aItemClass, aNameInternal, aMaterial, aSoundType, aMaxMeta, aIcons);
		if (MD.RC.mLoaded) try {EntityTunnelBore.addMineableBlock(this);} catch(Throwable e) {e.printStackTrace(ERR);}
		if (COMPAT_FR != null) gregapi.GT_API.deferItemInit(() -> COMPAT_FR.addToBackpacks("forester", ST.make(this, 1, W)));
	}
	
	public abstract int getLeavesRangeSide(byte aMeta);
	public abstract int getLeavesRangeYPos(byte aMeta);
	public abstract int getLeavesRangeYNeg(byte aMeta);
	
	// Wires the "block removed, notify neighbors" channel (1.7.10's breakBlock -> neo's
	// affectNeighborsAfterRemoval); without it a felled trunk never started leaf decay, leaving leaves hanging in midair.
	@Override public void onRemove(net.minecraft.world.level.block.state.BlockState aState, Level aLevel, BlockPos aPos, net.minecraft.world.level.block.state.BlockState aNewState, boolean aMovedByPiston) {
		if (!aState.is(aNewState.getBlock())) breakBlock(aLevel, aPos.getX(), aPos.getY(), aPos.getZ(), aState.getBlock(), getExtendedMetaData(aState));
		super.onRemove(aState, aLevel, aPos, aNewState, aMovedByPiston);
	}

	// @Override
	public void breakBlock(Level aWorld, int aX, int aY, int aZ, Block aBlock, int aMeta) {
		int tRangeSide = getLeavesRangeSide((byte)aMeta)+1, tRangeYNeg = getLeavesRangeYNeg((byte)aMeta)+1, tRangeYPos = getLeavesRangeYPos((byte)aMeta)+1;
		// isAreaLoaded takes only a symmetric radius, so the max of the three original ranges is used as a safe
		// superset that never loads less area than the asymmetric check it replaces.
		if (!aWorld.isClientSide() && aWorld.isAreaLoaded(new BlockPos(aX, aY, aZ), Math.max(tRangeSide, Math.max(tRangeYNeg, tRangeYPos)))) {
			tRangeSide--; tRangeYNeg--; tRangeYPos--;
			for (int i = -tRangeSide; i <= tRangeSide; ++i) for (int j = -tRangeYNeg; j <= tRangeYPos; ++j) for (int k = -tRangeSide; k <= tRangeSide; ++k) {
				Block tBlock = WD.block(aWorld, aX + i, aY + j, aZ + k);
				// beginLeavesDecay is gone as a generic Block hook in neo; GT6 logic lives only on BlockBaseLeaves,
				// so it is routed there by instanceof while vanilla leaves keep their own tick-based decay.
				if (WD.leaves(tBlock, aWorld, aX + i, aY + j, aZ + k) && tBlock instanceof BlockBaseLeaves) ((BlockBaseLeaves)tBlock).beginLeavesDecay(aWorld, aX + i, aY + j, aZ + k);
			}
		}
	}
}

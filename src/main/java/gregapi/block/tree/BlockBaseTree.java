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
	
	// @Override
	public void breakBlock(Level aWorld, int aX, int aY, int aZ, Block aBlock, int aMeta) {
		int tRangeSide = getLeavesRangeSide((byte)aMeta)+1, tRangeYNeg = getLeavesRangeYNeg((byte)aMeta)+1, tRangeYPos = getLeavesRangeYPos((byte)aMeta)+1;
		// было World.checkChunksExist(x0,y0,z0,x1,y1,z1) (асимметричный диапазон по осям) -> ILevelReaderExtension.
		// isAreaLoaded(BlockPos,int) [ILevelReaderExtension.java:19] принимает только симметричный радиус - берём
		// максимум из трёх диапазонов (безопасный супер-набор: требует загрузки НЕ МЕНЬШЕ области, чем исходно
		// проверялась, тот же приём, что и BlockBase/PrefixBlock.checkGravity ±32).
		if (!aWorld.isClientSide() && aWorld.isAreaLoaded(new BlockPos(aX, aY, aZ), Math.max(tRangeSide, Math.max(tRangeYNeg, tRangeYPos)))) {
			tRangeSide--; tRangeYNeg--; tRangeYPos--;
			for (int i = -tRangeSide; i <= tRangeSide; ++i) for (int j = -tRangeYNeg; j <= tRangeYPos; ++j) for (int k = -tRangeSide; k <= tRangeSide; ++k) {
				Block tBlock = WD.block(aWorld, aX + i, aY + j, aZ + k);
				// было Block.beginLeavesDecay(World,x,y,z) (1.7.10, вызов полиморфно на generic Block) - метод удалён
				// из neo Block целиком (PORT-TODO(F13/F16, block-beginLeavesDecay-removed), см. MultiTileEntityBlock.java);
				// единственный держатель реальной логики в дереве классов этого файла - BlockBaseLeaves (плоский метод,
				// не движковый override) - маршрутизируем через instanceof, vanilla-листва распадается автоматически
				// (свой tick-based механизм, не нуждается в этом поке).
				if (WD.leaves(tBlock, aWorld, aX + i, aY + j, aZ + k) && tBlock instanceof BlockBaseLeaves) ((BlockBaseLeaves)tBlock).beginLeavesDecay(aWorld, aX + i, aY + j, aZ + k);
			}
		}
	}
}

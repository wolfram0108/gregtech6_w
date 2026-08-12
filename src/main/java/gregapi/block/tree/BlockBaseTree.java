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
	
	// Подключение канала «блок снят — тронь соседей» (2026-07-30, реестр мёртвых каналов).
	// 1.7.10 breakBlock(World,x,y,z,Block,meta) → neo BlockBehaviour.affectNeighborsAfterRemoval(BlockState,
	// ServerLevel,BlockPos,boolean) (BlockBehaviour.java:170; зовётся движком после снятия блока,
	// BlockStateBase:748). Без моста тело ниже не звалось НИКЕМ: срубленный ствол GT6 не запускал распад
	// листвы вокруг — листья оставались висеть в воздухе. Подтип берём тем же центром мета↔BlockState, что и
	// остальной порт (IBlockExtendedMetaData), а не своим разбором свойств.
	// Ветка 1.20.1: носитель момента «блок снят» — onRemove (BlockBehaviour.java:163). Он зовётся и на смене
	// состояния того же блока, поэтому гейт на смену БЛОКА (ванильная идиома) — иначе breakBlock срабатывал бы
	// на каждом изменении property, чего 1.7.10-хук не делал.
	@Override public void onRemove(net.minecraft.world.level.block.state.BlockState aState, Level aLevel, BlockPos aPos, net.minecraft.world.level.block.state.BlockState aNewState, boolean aMovedByPiston) {
		if (!aState.is(aNewState.getBlock())) breakBlock(aLevel, aPos.getX(), aPos.getY(), aPos.getZ(), aState.getBlock(), getExtendedMetaData(aState));
		super.onRemove(aState, aLevel, aPos, aNewState, aMovedByPiston);
	}

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
				// F13 functional-adapted: 1.7.10 Block.beginLeavesDecay(World,x,y,z) — generic Block-хук, удалён из neo. Реальную
				// GT6-логику несёт только BlockBaseLeaves (плоский метод) → маршрутизируем instanceof'ом; vanilla-листва
				// распадается своим tick-механизмом (в этом поке не нуждается). Не заглушка: GT6-логика вызывается.
				if (WD.leaves(tBlock, aWorld, aX + i, aY + j, aZ + k) && tBlock instanceof BlockBaseLeaves) ((BlockBaseLeaves)tBlock).beginLeavesDecay(aWorld, aX + i, aY + j, aZ + k);
			}
		}
	}
}

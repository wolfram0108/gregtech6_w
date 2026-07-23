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
 */

package gregapi.block;

import net.minecraft.world.level.BlockGetter;

/**
 * @author Gregorius Techneticies
 */
public interface IBlockExtendedMetaData {
	public void setExtendedMetaData(BlockGetter aWorld, int aX, int aY, int aZ, short aMetaData);
	public short getExtendedMetaData(BlockGetter aWorld, int aX, int aY, int aZ);

	/** F13-снимок (BUG-016/BUG-047): мета из BlockState без чтения мира (harvest-пути: в neo removeBlock идёт ДО дропов).
	 *  Дефолт покрывает семьи с META-свойством (BlockBaseMeta/BlockBaseFlower/BlockFluidBaseGT — их META равны по equals:
	 *  IntegerProperty "meta" 0..15); TE-мета (PrefixBlock) стейтом не выражается → 0 (как прежний хардкод WD.meta(BlockState):837).
	 *  Носители меты в ДРУГИХ свойствах (BlockBaseRail: SHAPE+POWERED) переопределяют. */
	public default short getExtendedMetaData(net.minecraft.world.level.block.state.BlockState aState) {
		return aState.hasProperty(gregapi.block.BlockBaseMeta.META) ? (short)(int)aState.getValue(gregapi.block.BlockBaseMeta.META) : 0;
	}

	/** BUG-047 атомарный канал записи: state-с-метой для ОДНОГО setBlock (1.7.10 Chunk.func_150807_a писал блок+мету
	 *  атомарно — onBlockAdded видел мету; двухфазный WD.set «default-state, потом мета» ломал этот контракт для блоков,
	 *  чей onPlace читает мету, — рельс). aBase — текущий state (если блок тот же; прочие свойства, напр. WATERLOGGED,
	 *  сохраняются — 1:1 с 1.7.10, где смена меты не трогала ничего больше) либо defaultBlockState.
	 *  null = мета стейтом не выражается (TE-мета PrefixBlock) → вызыватель идёт прежним двухфазным путём. */
	public default net.minecraft.world.level.block.state.BlockState getStateForExtendedMetaData(net.minecraft.world.level.block.state.BlockState aBase, short aMetaData) {
		return aBase.hasProperty(gregapi.block.BlockBaseMeta.META) ? aBase.setValue(gregapi.block.BlockBaseMeta.META, (int)gregapi.util.UT.Code.bind4(aMetaData)) : null;
	}
}

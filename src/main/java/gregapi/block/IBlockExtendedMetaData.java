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
	/** Консолидация (заход #39, снятие дубля-зеркала): маршрутизация мета↔BlockState для семей с META-свойством
	 *  живёт ЗДЕСЬ — один код на всех носителей приёма (BlockBaseMeta/BlockBaseBars/BlockBaseFlower; их прежние
	 *  дословные копии друг друга удалены). Переопределяют только носители ИНОЙ раскладки: TE-мета (PrefixBlock),
	 *  мета в других свойствах (BlockBaseRail: SHAPE+POWERED), жидкости (BlockFluidBaseGT: LEVEL).
	 *  Пишет Level/LevelAccessor (интерактив) ИЛИ ChunkAccess (ворлдген — WD.set(ChunkAccess):872). */
	public default void setExtendedMetaData(BlockGetter aWorld, int aX, int aY, int aZ, short aMetaData) {
		net.minecraft.core.BlockPos tPos = new net.minecraft.core.BlockPos(aX, aY, aZ);
		net.minecraft.world.level.block.state.BlockState tState = aWorld.getBlockState(tPos);
		if (tState.getBlock() != this) return;
		net.minecraft.world.level.block.state.BlockState tNew = getStateForExtendedMetaData(tState, aMetaData);
		if (tNew == null) return;
		if (aWorld instanceof net.minecraft.world.level.LevelAccessor tLA) tLA.setBlock(tPos, tNew, 3);
		else if (aWorld instanceof net.minecraft.world.level.chunk.ChunkAccess tChunk) tChunk.setBlockState(tPos, tNew, net.minecraft.world.level.block.Block.UPDATE_ALL);
	}
	public default short getExtendedMetaData(BlockGetter aWorld, int aX, int aY, int aZ) {
		net.minecraft.world.level.block.state.BlockState tState = aWorld.getBlockState(new net.minecraft.core.BlockPos(aX, aY, aZ));
		return tState.getBlock() == this ? getExtendedMetaData(tState) : 0;
	}

	/** F13-снимок (BUG-016/BUG-047): мета из BlockState без чтения мира (harvest-пути: в neo removeBlock идёт ДО дропов).
	 *  Дефолт покрывает семьи с META-свойством `IntegerProperty "meta" 0..15` (BlockBaseMeta и наследники, BlockBaseBars —
	 *  они добавляют ИМЕННО BlockBaseMeta.META); TE-мета (PrefixBlock) стейтом не выражается → 0 (как прежний хардкод
	 *  WD.meta(BlockState):837).
	 *  ⚠ УТОЧНЕНО при разборе BUG-071: у BlockFluidBaseGT свойство называется `gt6_meta` (FLUID_META) — по equals с
	 *  BlockBaseMeta.META оно НЕ совпадает, поэтому здесь жидкости дают 0. Мировой путь у них свой
	 *  (getExtendedMetaData(BlockGetter,…) читает FLUID_META), а harvest-снимок жидкостям не нужен — их не ломают
	 *  инструментом; прежняя формулировка «их META равны по equals» была неверна.
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

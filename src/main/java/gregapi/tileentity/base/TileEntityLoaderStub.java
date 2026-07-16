/**
 * Copyright (c) 2026 GregTech-6 Team
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

package gregapi.tileentity.base;

import com.mojang.serialization.MapCodec;

import gregapi.util.UT;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;

import static gregapi.data.CS.*;

/**
 * F-tileentity-construction (LOAD-путь): concrete-заглушка, которую neo создаёт на world-load для GT6-MTE, чей
 * конкретный класс НЕ выводится из блока (MTE-машины: класс = sub-ID из сохранённого NBT, недоступен в
 * {@code BlockEntityType.create(pos,state)}). Прежний supplier {@code ->null} падал NPE ({@code BlockEntity.loadStatic:206}).
 * Заглушка лишь захватывает сырой NBT в {@link #loadAdditional}; реальный MTE реконструируется через реестр и заменяет
 * заглушку на {@code ChunkEvent.Load} (там доступен {@code Level} + позиция, замена {@code setBlockEntity} безопасна).
 */
public class TileEntityLoaderStub extends TileEntityBase01Root {
	public CompoundTag mLoadedNBT = null;

	public TileEntityLoaderStub(BlockPos aPos, BlockState aState) {super(F, aPos, aState);}

	@Override public String getTileEntityName() {return "gt.te.loader";}

	// Захват сырого NBT (без прогона readFromNBT — заглушка не знает конкретный класс MTE); реконструкция — на ChunkEvent.Load.
	@Override protected void loadAdditional(ValueInput input) {
		mLoadedNBT = input.read(MapCodec.assumeMapUnsafe(CompoundTag.CODEC)).orElseGet(UT.NBT::make);
	}
}

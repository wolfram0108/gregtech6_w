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

	/** LOAD-путь БЕЗ ПОТЕРЬ (корень BUG-057): чанк может сохраниться РАНЬШЕ, чем реконструкция стаба добежит
	 *  (очередь server-tick с квотой; транзитные/граничные чанки выгружаются раньше; при save/shutdown тик не идёт).
	 *  Базовый {@code saveAdditional} прогонял GT6-мост {@code writeToNBT}, который для стаба писал только id/x/y/z —
	 *  {@code gt.mte.reg}/{@code gt.mte.id} и все данные MTE стирались с диска НАВСЕГДА (блок навсегда прозрачен).
	 *  Стаб — прозрачный переносчик: возвращает захваченный NBT на диск ровно как прочитал (идемпотентный round-trip). */
	@Override protected void saveAdditional(net.minecraft.world.level.storage.ValueOutput output) {
		if (mLoadedNBT == null) {super.saveAdditional(output); return;}
		output.store(mLoadedNBT);
	}
}

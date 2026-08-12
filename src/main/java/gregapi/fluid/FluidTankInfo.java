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

package gregapi.fluid;

import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidTank;

/**
 * F5 компат-заглушка: Forge-1.7.10 {@code net.minecraftforge.fluids.FluidTankInfo} — простая
 * неизменяемая пара (жидкость, ёмкость), которую отдавал {@code IFluidHandler.getTankInfo()}. Ни в
 * Forge 1.20.1 (греп по {@code forge-1201-decompiled/net/minecraftforge/fluids/} — класса нет: там
 * {@code IFluidHandler} отдаёт содержимое поштучно через {@code getFluidInTank}/{@code getTankCapacity}),
 * ни в 26.x прямого аналога нет — воспроизведена по фактическому использованию в дереве (2-арг конструктор,
 * поля {@code fluid}/{@code capacity}: {@code gregapi.data.CS.java:834}, {@code FluidTankGT.getInfo()}).
 * [Метка отложенности «consumer-файлы не мигрированы» СНЯТА 2026-08-06 — пережила собственный фикс:
 * в оригинале ровно 14 вызывателей {@code getTankInfo}, в порте у всех 14 есть плечо — 12 ходят через
 * ЦЕНТР шва {@code FL.getTankInfo} ({@code FL.java:944}, side-aware поверх движкового API), сенсоры
 * Fluidometer/Bucketometer/KiloBucketometer, BasicMachine:705, WD:2061, Bridge/Extender/MiniPortal/
 * LongDistancePipelineFluid; 1 — контракт {@code IMultiBlockFluidHandler} (1:1 с оригиналом :396);
 * 1 — {@code MultiTileEntityPipeFluid:506-511}, длина берётся из движкового API напрямую (документировано там).]
 */
public final class FluidTankInfo {
	public final FluidStack fluid;
	public final int capacity;

	public FluidTankInfo(FluidStack aFluid, int aCapacity) {
		fluid = aFluid;
		capacity = aCapacity;
	}

	/** Forge-1.7.10 {@code FluidTankInfo(IFluidTank)} = пара (текущая жидкость, ёмкость бака).
	 *  Forge 1.20.1 сохранил {@code IFluidTank.getFluid()}/{@code getCapacity()} дословно
	 *  ({@code forge-1201-decompiled/net/minecraftforge/fluids/IFluidTank.java}) — 1:1. */
	public FluidTankInfo(IFluidTank aTank) {
		this(aTank.getFluid(), aTank.getCapacity());
	}
}

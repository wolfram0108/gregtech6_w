/**
 * Copyright (c) 2025 GregTech-6 Team
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

import net.neoforged.neoforge.fluids.FluidStack;

/**
 * F5 компат-заглушка: Forge-1.7.10 {@code net.minecraftforge.fluids.FluidTankInfo} — простая
 * неизменяемая пара (жидкость, ёмкость), которую отдавал {@code IFluidHandler.getTankInfo()}. В neo
 * 26.1.2 прямого аналога нет (заменено transfer-API {@code ResourceHandler}, не возвращающим такую
 * пару напрямую) — воспроизведена по фактическому использованию в дереве (2-арг конструктор,
 * поля {@code fluid}/{@code capacity}: {@code gregapi.data.CS.java:834}, {@code FluidTankGT.getInfo()}).
 * // PORT-TODO(этап 6, F5): места, где это ещё нужно как часть legacy IFluidHandler.getTankInfo(),
 * не мигрированы — они относятся к consumer-файлам вне области этого переходника.
 */
public final class FluidTankInfo {
	public final FluidStack fluid;
	public final int capacity;

	public FluidTankInfo(FluidStack aFluid, int aCapacity) {
		fluid = aFluid;
		capacity = aCapacity;
	}
}

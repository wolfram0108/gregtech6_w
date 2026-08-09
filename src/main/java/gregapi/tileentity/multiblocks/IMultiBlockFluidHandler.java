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

package gregapi.tileentity.multiblocks;
import gregapi.fluid.FluidTankInfo;

import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * @author Gregorius Techneticies
 */
public interface IMultiBlockFluidHandler extends ITileEntityMultiBlockController {
	public int fill                     (MultiTileEntityMultiBlockPart aPart, byte aSide, FluidStack aFluid, boolean aDoFill);
	public FluidStack drain             (MultiTileEntityMultiBlockPart aPart, byte aSide, FluidStack aFluid, boolean aDoDrain);
	public FluidStack drain             (MultiTileEntityMultiBlockPart aPart, byte aSide, int aAmountToDrain, boolean aDoDrain);
	public boolean canFill              (MultiTileEntityMultiBlockPart aPart, byte aSide, Fluid aFluid);
	public boolean canDrain             (MultiTileEntityMultiBlockPart aPart, byte aSide, Fluid aFluid);
	public FluidTankInfo[] getTankInfo  (MultiTileEntityMultiBlockPart aPart, byte aSide);
	/** Сами танки, видимые со стороны ЭТОЙ части. Седьмой метод контракта: в 1.7.10 его не требовалось —
	 *  часть объявляла {@code IFluidHandler} и чужой мод спрашивал её напрямую шестью методами выше. В neo
	 *  наружу видно только зарегистрированную capability, а строится она из объектов танков
	 *  ({@code GT6FluidCapability.handlerOf}); без этого метода стенка танка отдавала «танков нет», и
	 *  содержимое многоблока было видно только на контроллере (репорт игрока: Jade молчит на стенках).
	 *  Реализация уже есть у всех носителей — {@code TileEntityBase01Root:847} и переопределение
	 *  {@code TileEntityBase10MultiBlockBase:212}; здесь метод только объявлен в контракте. */
	public net.neoforged.neoforge.fluids.IFluidTank[] getFluidTanks(MultiTileEntityMultiBlockPart aPart, byte aSide);
}

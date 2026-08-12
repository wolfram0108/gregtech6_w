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

import gregapi.tileentity.base.TileEntityBase01Root;
import net.minecraft.core.Direction;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

/**
 * F5-capability на Forge 1.20.1 — СТОРОННИЙ вид на танки GT6 (ветка бэкпорта).
 *
 * <p><b>Что изменилось против ветки 26.x.</b> Там наружный канал жидкости был
 * {@code ResourceHandler<FluidResource>}, объявляемый событием {@code RegisterCapabilitiesEvent}
 * ({@code registerBlock(Capabilities.Fluid.BLOCK, …)}) — ни transfer-API, ни события-регистрации в
 * 1.20.1 не существует. Здесь канал — {@code ForgeCapabilities.FLUID_HANDLER}
 * ({@code forge-1201-decompiled/net/minecraftforge/common/capabilities/ForgeCapabilities.java:21}),
 * а объявляет его сам BlockEntity через {@code getCapability(Capability, Direction)} →
 * {@code LazyOptional} ({@code ICapabilityProvider.java:26}; образец той же версии — AE2
 * {@code SkyStoneTankBlockEntity.java:66-72}, {@code ChestBlockEntity.java:740-749}).
 * Единственный вход — {@link TileEntityBase01Root#getCapability} (общий корень ВСЕЙ GT6-иерархии
 * BlockEntity), поэтому мост по-прежнему ровно один и россыпи по наследникам нет.
 *
 * <p><b>Главное: сторона вернулась в контракт — это восстановление формы 1.7.10, а не новая модель.</b>
 * В 1.7.10 GT6-TE реализовывали {@code net.minecraftforge.fluids.IFluidHandler} с ШЕСТЬЮ side-методами
 * ({@code fill(ForgeDirection,…)}/{@code drain(ForgeDirection,…)}/{@code getTankInfo(ForgeDirection)} —
 * оригинал {@code TileEntityBase01Root.java:603,612,621,630,636,642}). В 1.20.1 сам {@code IFluidHandler}
 * sideless (7 методов), но сторону несёт САМ ЗАПРОС КАПЫ: {@code getCapability(cap, side)}. Значит
 * side-aware поведение GT6 выражается связкой «сторона запроса → хендлер, привязанный к этой стороне»,
 * и вся GT6-логика стороны ({@code getFluidTankFillable/Drainable(side, …)}, ковер-оверрайды
 * {@code TileEntityBase06Covers:375}) работает ровно как у Грегориуса.
 *
 * <p><b>Своей логики танков здесь нет.</b> Все семь методов — тонкая переадресация на side-aware методы
 * самого TE. Это важнее, чем кажется: ветка 26.x строила наружный вид из СЫРОГО списка танков
 * ({@code getFluidTanksForCapability}), минуя {@code getFluidTankFillable/Drainable}, то есть мимо
 * GT6-правил «какой танк какой стороне и подо что доступен»; здесь этих правил не обходит никто.
 * Наследники, переопределяющие side-методы (ретрансляторы Bridge/Extender/MiniPortal/
 * LongDistancePipelineFluid, MultiBlockPart, PipeFluid), попадают в наружный вид сами собой.
 */
public final class GT6FluidCapability {
	private GT6FluidCapability() {}

	/** Есть ли у этой стороны что показывать наружу. Предикат тот же, что был в ветке 26.x
	 *  (танков нет → капы нет), но спрашивается через {@code getTankInfo(side)} — то есть с учётом
	 *  наследников, переопределивших именно его. */
	public static boolean hasTanks(TileEntityBase01Root aTileEntity, Direction aSide) {
		if (aTileEntity == null) return false;
		try {
			gregapi.fluid.FluidTankInfo[] tInfo = aTileEntity.getTankInfo(aSide);
			return tInfo != null && tInfo.length > 0;
		} catch (Throwable e) {return false;} // логика конкретного TE не должна ронять чужой мод, который просто спросил капу
	}

	/** Хендлер, привязанный к стороне запроса ({@code null} = sideless-запрос = родной GT6 {@code SIDE_ANY}). */
	public static IFluidHandler handlerOf(TileEntityBase01Root aTileEntity, Direction aSide) {
		return new SidedTankView(aTileEntity, aSide);
	}

	/**
	 * Side-bound вид: 1.20.1-{@code IFluidHandler} поверх side-методов GT6 — дословный эквивалент того,
	 * чем в 1.7.10 был сам TE. Исключения наружу не выпускаем по той же причине, что и в
	 * {@link #hasTanks}: чужой мод, спросивший капу, не должен падать из-за логики конкретной машины.
	 */
	private static final class SidedTankView implements IFluidHandler {
		private final TileEntityBase01Root mTileEntity;
		private final Direction mSide;

		SidedTankView(TileEntityBase01Root aTileEntity, Direction aSide) {mTileEntity = aTileEntity; mSide = aSide;}

		private gregapi.fluid.FluidTankInfo[] info() {
			try {
				gregapi.fluid.FluidTankInfo[] rInfo = mTileEntity.getTankInfo(mSide);
				return rInfo == null ? gregapi.data.CS.ZL_FLUIDTANKINFO : rInfo;
			} catch (Throwable e) {return gregapi.data.CS.ZL_FLUIDTANKINFO;}
		}

		@Override public int getTanks() {return info().length;}

		@Override public FluidStack getFluidInTank(int aTank) {
			gregapi.fluid.FluidTankInfo[] tInfo = info();
			return aTank >= 0 && aTank < tInfo.length && tInfo[aTank] != null && tInfo[aTank].fluid != null ? tInfo[aTank].fluid : FluidStack.EMPTY;
		}

		@Override public int getTankCapacity(int aTank) {
			gregapi.fluid.FluidTankInfo[] tInfo = info();
			return aTank >= 0 && aTank < tInfo.length && tInfo[aTank] != null ? tInfo[aTank].capacity : 0;
		}

		@Override public boolean isFluidValid(int aTank, FluidStack aFluid) {
			if (aFluid == null || aFluid.isEmpty()) return false;
			try {return mTileEntity.canFill(mSide, aFluid.getFluid());} catch (Throwable e) {return false;}
		}

		@Override public int fill(FluidStack aResource, FluidAction aAction) {
			if (aResource == null || aResource.isEmpty()) return 0;
			try {return mTileEntity.fill(mSide, aResource, aAction.execute());} catch (Throwable e) {return 0;}
		}

		/** {@code FluidStack.EMPTY} вместо {@code null}: контракт 1.20.1 {@code IFluidHandler.drain}
		 *  требует непустой объект-стек ({@code IFluidHandler.java:88,104} — {@code @NotNull}). */
		@Override public FluidStack drain(FluidStack aResource, FluidAction aAction) {
			if (aResource == null || aResource.isEmpty()) return FluidStack.EMPTY;
			try {FluidStack rDrained = mTileEntity.drain(mSide, aResource, aAction.execute()); return rDrained == null ? FluidStack.EMPTY : rDrained;} catch (Throwable e) {return FluidStack.EMPTY;}
		}

		@Override public FluidStack drain(int aMaxDrain, FluidAction aAction) {
			if (aMaxDrain <= 0) return FluidStack.EMPTY;
			try {FluidStack rDrained = mTileEntity.drain(mSide, aMaxDrain, aAction.execute()); return rDrained == null ? FluidStack.EMPTY : rDrained;} catch (Throwable e) {return FluidStack.EMPTY;}
		}
	}
}

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

import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.IFluidTank;

/** Compat stand-in for Forge 1.7.10's simple immutable (fluid, capacity) pair, which neo's transfer API
 *  does not return directly; reconstructed from how the mod actually uses it (a 2-arg constructor). */
public final class FluidTankInfo {
	public final FluidStack fluid;
	public final int capacity;
	/** The capacity without the cut to int, for the channels that carry a long; and whether the tank only voids what it gets, so it is no storage. */
	public final long capacityLong;
	public final boolean voids;

	public FluidTankInfo(FluidStack aFluid, int aCapacity) {
		fluid = aFluid;
		capacity = aCapacity;
		capacityLong = aCapacity;
		voids = false;
	}

	/** neo's IFluidTank kept getFluid()/getCapacity(), so this constructor carries over 1:1. */
	public FluidTankInfo(IFluidTank aTank) {
		fluid = aTank.getFluid();
		capacity = aTank.getCapacity();
		capacityLong = aTank instanceof FluidTankGT tTank ? tTank.capacity() : aTank.getCapacity();
		voids = aTank instanceof FluidTankGT tTank && tTank.voidsExcess();
	}
}

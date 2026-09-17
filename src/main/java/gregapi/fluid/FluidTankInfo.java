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

/** Forge 1.7.10's IFluidHandler.getTankInfo() returned this simple immutable (fluid, capacity) pair; no engine version now
 *  has a direct analog, so it's reproduced here from how the tree actually uses it. */
public final class FluidTankInfo {
	public final FluidStack fluid;
	public final int capacity;

	public FluidTankInfo(FluidStack aFluid, int aCapacity) {
		fluid = aFluid;
		capacity = aCapacity;
	}

	/** Forge 1.20.1 kept IFluidTank.getFluid()/getCapacity() verbatim, so this constructor stays 1:1 with 1.7.10. */
	public FluidTankInfo(IFluidTank aTank) {
		this(aTank.getFluid(), aTank.getCapacity());
	}
}

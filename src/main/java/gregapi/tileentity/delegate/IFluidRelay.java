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

package gregapi.tileentity.delegate;

import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/** A block that passes fluid on to another storage; the one place it says which, for its own fill and drain and for whoever
 *  needs to know where a move through it lands (GT6FluidCapability sizes a foreign transaction's moves by that storage). */
public interface IFluidRelay {
	/** The storage fluid entering this block's side goes on to, or null where there is none. */
	public DelegatorTileEntity<IFluidHandler> getRelayedTank(byte aSide);
}

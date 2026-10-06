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

package gregapi.tileentity.energy;

import gregapi.tileentity.base.TileEntityBase01Root;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.energy.IEnergyStorage;

import static gregapi.data.CS.*;

/**
 * The RF channel of GT6 as other mods see it today. In 1.7.10 the RF blocks of GT6 declared CoFH's IEnergyHandler
 * (ITileEntityEnergyFluxHandler) and TileEntityBase01Root answered receiveEnergy/extractEnergy for all of them; Forge 1.20.1
 * has no such interface, only ForgeCapabilities.ENERGY, so TileEntityBase01Root.getCapability hands those same blocks out
 * there, answered by the same root methods. BuildCraft's MJ joins on the same selection through ICompatBC.capability.
 */
public class GT6EnergyCapability {
	private GT6EnergyCapability() {}

	/** The block's RF side as 1.7.10 offered it: only RF handlers, and only on a side where canConnectEnergy says yes. */
	public static TileEntityBase01Root flux(BlockEntity aBlockEntity, Direction aSide) {
		if (!(aBlockEntity instanceof ITileEntityEnergyFluxHandler) || !(aBlockEntity instanceof TileEntityBase01Root tRoot) || aSide == null) return null;
		try {return tRoot.canConnectEnergy(aSide) ? tRoot : null;} catch (Throwable e) {return null;}
	}

	public static IEnergyStorage handlerOf(TileEntityBase01Root aRoot, Direction aSide) {return new FluxHandler(aRoot, aSide);}

	/** FE in and out of one GT6 RF block; the Forge 1.20.1 capability has no transactions, so calls go straight to the root. */
	private static final class FluxHandler implements IEnergyStorage {
		private final TileEntityBase01Root mRoot;
		private final Direction mSide;
		FluxHandler(TileEntityBase01Root aRoot, Direction aSide) {mRoot = aRoot; mSide = aSide;}
		@Override public int receiveEnergy(int aAmount, boolean aSimulate) {return aAmount <= 0 ? 0 : Math.max(0, mRoot.receiveEnergy(mSide, aAmount, aSimulate));}
		@Override public int extractEnergy(int aAmount, boolean aSimulate) {return aAmount <= 0 ? 0 : Math.max(0, mRoot.extractEnergy(mSide, aAmount, aSimulate));}
		@Override public int getEnergyStored() {return mRoot.getEnergyStored(mSide);}
		@Override public int getMaxEnergyStored() {return mRoot.getMaxEnergyStored(mSide);}
		@Override public boolean canReceive() {return T;}
		@Override public boolean canExtract() {return T;}
	}
}

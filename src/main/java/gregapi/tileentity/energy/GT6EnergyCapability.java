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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

import static gregapi.data.CS.*;

/** GT6's RF blocks as other mods see them today: CoFH's IEnergyHandler of 1.7.10 is gone, so Capabilities.Energy.BLOCK
 *  carries them, answered by the same root methods; a mod with its own channel joins via its compat (ICompatBC). */
public class GT6EnergyCapability {
	private GT6EnergyCapability() {}

	public static void register(IEventBus aModBus) {
		aModBus.addListener(GT6EnergyCapability::onRegisterCapabilities);
	}

	private static void onRegisterCapabilities(RegisterCapabilitiesEvent aEvent) {
		Block[] tArray = gregapi.block.multitileentity.MultiTileEntityBlock.allInRegistry();
		if (tArray.length == 0) {
			ERR.println("GT6 energy-capability: 0 MTE blocks in the registry — the RF channel was NOT registered!");
			return;
		}
		aEvent.registerBlock(Capabilities.Energy.BLOCK, (aLevel, aPos, aState, aBlockEntity, aSide) -> {
			TileEntityBase01Root tRoot = flux(aBlockEntity, aSide);
			return tRoot == null ? null : new FluxHandler(tRoot, aSide);
		}, tArray);
		OUT.println("GT6 energy-capability: RF channel registered for " + tArray.length + " MTE blocks (Capabilities.Energy.BLOCK).");
		if (COMPAT_BC != null) COMPAT_BC.registerCapabilities(aEvent, tArray);
	}

	/** The block's RF side as 1.7.10 offered it: only RF handlers, and only on a side where canConnectEnergy says yes. */
	public static TileEntityBase01Root flux(BlockEntity aBlockEntity, Direction aSide) {
		if (!(aBlockEntity instanceof ITileEntityEnergyFluxHandler) || !(aBlockEntity instanceof TileEntityBase01Root tRoot) || aSide == null) return null;
		try {return tRoot.canConnectEnergy(aSide) ? tRoot : null;} catch (Throwable e) {return null;}
	}

	/** FE in and out of one GT6 RF block: what a transaction accepts is held as pending and handed to the root on its
	 *  root commit, nothing on a revert, since GT6 moves energy at once. */
	private static final class FluxHandler implements EnergyHandler {
		private final TileEntityBase01Root mRoot;
		private final Direction mSide;
		private int mInPending = 0, mOutPending = 0;
		private final SnapshotJournal<int[]> mJournal = new SnapshotJournal<>() {
			@Override protected int[] createSnapshot() {return new int[] {mInPending, mOutPending};}
			@Override protected void revertToSnapshot(int[] aSnapshot) {mInPending = aSnapshot[0]; mOutPending = aSnapshot[1];}
			@Override protected void onRootCommit(int[] aOriginal) {
				int tIn = mInPending, tOut = mOutPending;
				mInPending = mOutPending = 0;
				if (tIn  > 0) mRoot.receiveEnergy(mSide, tIn , F);
				if (tOut > 0) mRoot.extractEnergy(mSide, tOut, F);
			}
		};

		FluxHandler(TileEntityBase01Root aRoot, Direction aSide) {mRoot = aRoot; mSide = aSide;}

		@Override public long getAmountAsLong() {return mRoot.getEnergyStored(mSide);}
		@Override public long getCapacityAsLong() {return mRoot.getMaxEnergyStored(mSide);}

		@Override
		public int insert(int aAmount, TransactionContext aTransaction) {
			if (aAmount <= 0) return 0;
			int rAccepted = Math.max(0, Math.min(aAmount, mRoot.receiveEnergy(mSide, mInPending + aAmount, T) - mInPending));
			if (rAccepted > 0) {mJournal.updateSnapshots(aTransaction); mInPending += rAccepted;}
			return rAccepted;
		}

		@Override
		public int extract(int aAmount, TransactionContext aTransaction) {
			if (aAmount <= 0) return 0;
			int rTaken = Math.max(0, Math.min(aAmount, mRoot.extractEnergy(mSide, mOutPending + aAmount, T) - mOutPending));
			if (rTaken > 0) {mJournal.updateSnapshots(aTransaction); mOutPending += rTaken;}
			return rTaken;
		}
	}
}

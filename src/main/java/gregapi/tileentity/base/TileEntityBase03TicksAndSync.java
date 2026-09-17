/**
 * Copyright (c) 2022 GregTech-6 Team
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

package gregapi.tileentity.base;

import gregapi.network.INetworkHandler;
import gregapi.network.IPacket;
import gregapi.tileentity.ITileEntitySynchronising;
import gregapi.util.WD;
import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

import static gregapi.data.CS.*;

/**
 * @author Gregorius Techneticies
 * 
 * TileEntity with Network Code
 */
public abstract class TileEntityBase03TicksAndSync extends TileEntityBase02AdjacentTEBuffer implements ITileEntitySynchronising {
	/** Gets set if/when needed. */
	public UUID mOwner = null;
	
	/** Variable for seeing if the Tick Function is called right now. */
	public boolean mIsRunningTick = F;
	
	/** Variable for updating Data to the Client */
	private boolean mSendClientData = F;
	
	/** Gets set to true when the Block received a Block Update. */
	public boolean mBlockUpdated = F;
	
	/** @return a Packet containing all Data which has to be synchronised to the Client */
	public abstract IPacket getClientDataPacket(boolean aSendAll);

	/** Must be a pure recompute of what to show the client, with no side effects; the center calls it before every snapshot.
	 *  It no longer depends on ticking: a BE can be visible but outside the simulation range and never tick at all. */
	public void updateVisualData() {/**/}

	/** Sends all Data to the Clients in Range */
	public void sendClientData(boolean aSendAll, ServerPlayer aPlayer) {
		if (mSendingClientData) return; // defense against reentrancy: the visual recompute below calls back into the sync path itself
		mSendingClientData = T;
		// The snapshot must carry the current visual state, not whatever the tick last computed, since it may not have run.
		try {updateVisualData();} catch (Throwable e) {e.printStackTrace(ERR);} // a failed visual recompute must not break the sync
		try {
		if (aPlayer == null) {
			IPacket tPacket = getClientDataPacket(aSendAll);
			if (mOwner == null) {
				getNetworkHandler().sendToAllPlayersInRange(tPacket, level, getCoords());
			} else {
				getNetworkHandler().sendToPlayerIfInRange(tPacket, mOwner, level, getCoords());
				getNetworkHandlerNonOwned().sendToAllPlayersInRangeExcept(tPacket, mOwner, level, getCoords());
			}
		} else {
			// A joining player always gets a personal snapshot now; the old gate assumed the BE would eventually tick and
			// broadcast on its own, which is false for one sitting outside the simulation range.
			IPacket tPacket = getClientDataPacket(aSendAll);
			if (mOwner == null) {
				getNetworkHandler().sendToPlayer(tPacket, aPlayer);
			} else {
				if (mOwner.equals(aPlayer.getUUID())) {
					getNetworkHandler().sendToPlayer(tPacket, aPlayer);
				} else {
					getNetworkHandlerNonOwned().sendToPlayer(tPacket, aPlayer);
				}
			}
		}
		} finally {mSendingClientData = F;}
	}

	@Override
	public void processPacket(INetworkHandler aNetworkHandler) {
		if (isClientSide()) mOwner = (aNetworkHandler == getNetworkHandlerNonOwned() ? NOT_YOU : null);
	}

	/** @return the used Network Handler. Defaults to the API Handler. */
	public INetworkHandler getNetworkHandler() {return NW_API;}
	public INetworkHandler getNetworkHandlerNonOwned() {return NW_AP2;}

	/** Reentrancy guard: {@link #sendClientData} recomputes the look, and some owners' recompute calls
	 *  {@link #updateClientData} itself. */
	private boolean mSendingClientData = F;

	/** Usually flagging is enough since the snapshot goes out on the next tick.
	 *  A BE outside the simulation range never ticks, yet can still change from player interaction, so this sends right away. */
	public void updateClientData() {
		mSendClientData = T;
		if (mSendingClientData || !isServerSide() || level == null || WD.blockTicking(this)) return;
		mSendClientData = F;
		sendClientData(T, null);
	}

	@Override public void onCoordinateChange() {super.onCoordinateChange(); updateClientData();}
	
	@Override public final net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getUpdatePacket() {return null;}
	
	@Override
	public void clearRemoved() {
		super.clearRemoved();
		updateClientData();
	}
	
	@Override
	public final void sendUpdateToPlayer(ServerPlayer aPlayer) {
		sendClientData(T, aPlayer);
	}
	
	@Override
	public boolean allowInteraction(Entity aEntity) {
		return mOwner == null || (aEntity != null && mOwner.equals(aEntity.getUUID()));
	}
	
	@Override
	public final void updateEntity() {
		mIsRunningTick = T;
		boolean tIsServerSide = isServerSide();
		try {
			if (mTimer == 0) {
				setChanged();
				WD.mark(this);
				onTickFirst(tIsServerSide);
			}
			if (!isDead()) onTickStart(mTimer, tIsServerSide);
			if (!isDead()) super.updateEntity();
			if (!isDead()) onTick(mTimer, tIsServerSide);
			if (!isDead() && tIsServerSide && mTimer > 2 && (mSendClientData || onTickCheck(mTimer))) {
				sendClientData(mSendClientData, null);
				mSendClientData = F;
				onTickChecked(mTimer);
			}
			if (!isDead()) onTickResetChecks(mTimer, tIsServerSide);
			if (!isDead()) onTickEnd(mTimer, tIsServerSide);
		} catch(Throwable e1) {
			e1.printStackTrace(ERR);
			setError((tIsServerSide?"Serverside: ":"Clientside: ") + e1);
			try {
				onTickFailed(mTimer, tIsServerSide);
			} catch(Throwable e2) {
				e2.printStackTrace(ERR);
				setError((tIsServerSide?"Serverside: ":"Clientside: ") + e2);
			}
		}
		mIsRunningTick = F;
	}
	
	/** The very first Tick happening to this TileEntity */
	public void onTickFirst(boolean aIsServerSide) {/**/}
	
	/** The first Part of the Tick. */
	public void onTickStart(long aTimer, boolean aIsServerSide) {/**/}
	
	/** The regular Tick. */
	public void onTick(long aTimer, boolean aIsServerSide) {/**/}
	
	/** Use this to check if it is required to send an update to the Clients. If you want you can call "updateClientData", but then you need to return true in order for it to work.*/
	public boolean onTickCheck(long aTimer) {return F;}
	
	/** Called when onTickCheck returns true. A super Call is important for this one! */
	public void onTickChecked(long aTimer) {/**/}
	
	/** Used to reset all Variables which have something to do with the detection of Changes. A super Call is important for this one! */
	public void onTickResetChecks(long aTimer, boolean aIsServerSide) {mBlockUpdated = F;}
	
	/** The absolutely last Part of the Tick. */
	public void onTickEnd(long aTimer, boolean aIsServerSide) {/**/}
	
	/** Gets called when there is an Exception happening during one of the Tick Functions. */
	public void onTickFailed(long aTimer, boolean aIsServerSide) {/**/}
}

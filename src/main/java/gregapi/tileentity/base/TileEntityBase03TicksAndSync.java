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

	/**
	 * ПРОИЗВОДНЫЕ ДАННЫЕ ОБЛИКА, которые едут клиенту (что лежит на наковальне, что налито в ящике бутылей,
	 * какие книги на полке). Реализация обязана быть ЧИСТЫМ пересчётом из собственного состояния —
	 * без переноса предметов, звуков и прочих побочных действий: центр зовёт её перед КАЖДОЙ сборкой снимка.
	 *
	 * <p>Зачем канал вообще нужен (Issue #1 «предмет на наковальне не отображается»). Облик считался
	 * ТОЛЬКО в тике блок-энтити ({@code onTick2} под {@code mInventoryChanged}), и оттуда же уходил клиенту.
	 * Блок-энтити НЕ ТИКАЕТ, пока чанк вне зоны симуляции ({@code ServerLevel.shouldTickBlocksAt} →
	 * {@code inBlockTickingRange}), а ВИДИМ он при этом остаётся: зоны видимости и симуляции раздельны, и
	 * видимость обычно больше. Сервер сам не знал, что рисовать, и клиент получал пустую наковальню. Теперь
	 * облик считается в момент, когда снимок собирается, и от тика не зависит вовсе.
	 */
	public void updateVisualData() {/**/}

	/** Sends all Data to the Clients in Range */
	public void sendClientData(boolean aSendAll, ServerPlayer aPlayer) {
		if (mSendingClientData) return; // защита от возврата: пересчёт облика ниже сам зовёт updateClientData
		mSendingClientData = T;
		// Снимок обязан нести АКТУАЛЬНЫЙ облик, а не тот, что успел посчитать тик (его может не быть вовсе).
		try {updateVisualData();} catch (Throwable e) {e.printStackTrace(ERR);} // пересчёт облика не должен рвать синк
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
			// ПЕРСОНАЛЬНЫЙ СНИМОК ВОШЕДШЕМУ ИГРОКУ ШЛЁТСЯ ВСЕГДА. Прежде здесь стоял гейт `!mSendClientData`
			// («раз рассылка всем и так запланирована, персонально не шлём»), и он держался на допущении, что
			// блок-энтити обязательно оттикает и разошлёт. Это неверно: у нетикающего чанка флаг
			// mSendClientData взводится при каждой привязке BE к чанку (clearRemoved → updateClientData) и
			// не сбрасывается никогда — значит вошедший игрок не получал снимок вовсе. Цена снятия — изредка
			// второй такой же пакет; снимок идемпотентен.
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

	/** Защита от возврата в отправку: {@link #sendClientData} пересчитывает облик, а пересчёт у части
	 *  носителей сам зовёт {@link #updateClientData} — без гейта это была бы рекурсия. */
	private boolean mSendingClientData = F;

	/**
	 * Called to send all Data to the close Clients.
	 *
	 * <p>Обычно достаточно взвести флаг: снимок уедет на ближайшем тике. Но у блока в чанке ВНЕ зоны
	 * симуляции тика не будет вовсе, а измениться он может — взаимодействие игрока и соседей от тика не
	 * зависит (положил слиток в наковальню в чанке, который ещё не затикал: предмет лёг, а картинка
	 * осталась прежней и ждать её было бы нечего). Для таких блоков снимок отправляется сразу.
	 */
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

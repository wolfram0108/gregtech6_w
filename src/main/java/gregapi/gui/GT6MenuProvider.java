/**
 * Copyright (c) 2026 GregTech-6 Team
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

package gregapi.gui;

import gregapi.data.LH;
import gregapi.tileentity.ITileEntityGUI;
import gregapi.tileentity.ITileEntityInventoryGUI;
import gregapi.util.WD;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import static gregapi.data.CS.T;

/**
 * @author Gregorius Techneticies (перенос архитектуры), порт — F-GUI
 *
 * F-GUI (шов «GUI/меню»): ЕДИНСТВЕННАЯ реализация {@link MenuProvider} мода — серверный центр открытия
 * GUI. 1.7.10 {@code EntityPlayer.openGui(mod,id,world,x,y,z)} диспетчерил через {@code IGuiHandler}
 * автоматически (Forge network registry); в neo такого нет — единственный путь открыть контейнер с
 * сервера — {@code Player.openMenu(MenuProvider, Consumer<RegistryFriendlyByteBuf>)}
 * (`neo-decompiled/net/minecraft/world/entity/player/Player.java:844`, реально —
 * `neo-decompiled/net/minecraft/server/level/ServerPlayer.java:1414-1461`,
 * `neoforge-decompiled/net/neoforged/neoforge/common/extensions/IPlayerExtension.java:75-77`).
 * Единственный вызыватель — {@code gregapi.tileentity.base.TileEntityBase01Root#openGUI} (единственное
 * место мода, вызывавшее {@code player.openGui(...)} в оригинале — сверено, {@code grep .openGui(} по
 * всему {@code gregtech6/src}). Маршрут ({@code id → getGUIServer}) ТОТ ЖЕ, что и клиентская реконструкция
 * контейнера ({@link ContainerCommon#createFromNetwork}) — централизация сохранена.
 */
public class GT6MenuProvider implements MenuProvider {
	private final Level mLevel;
	private final BlockPos mPos;
	private final int mGUIID;

	public GT6MenuProvider(Level aLevel, BlockPos aPos, int aGUIID) {
		mLevel = aLevel;
		mPos = aPos;
		mGUIID = aGUIID;
	}

	/** Заголовок окна для сетевого пакета открытия — источник берётся из {@code ITileEntityInventoryGUI}
	 *  (уже централизованный контракт «есть ли у инвентаря своё имя»), LOCALIZATION-центр {@code LH.get}. */
	@Override
	public Component getDisplayName() {
		BlockEntity tTileEntity = WD.te(mLevel, mPos, T);
		if (tTileEntity instanceof ITileEntityInventoryGUI) {
			ITileEntityInventoryGUI tGUI = (ITileEntityInventoryGUI)tTileEntity;
			if (tGUI.hasCustomInventoryNameGUI()) return Component.literal(LH.get(tGUI.getInventoryNameGUI()));
		}
		return Component.literal("");
	}

	/** Серверное создание контейнера — тот же маршрут {@code getGUIServer}, что и клиентская
	 *  реконструкция ({@link ContainerCommon#createFromNetwork}); windowId прокинут через мост
	 *  {@link ContainerCommon#withWindowID} (см. его javadoc про форс движка на {@code containerId}). */
	@Override
	public AbstractContainerMenu createMenu(int aWindowID, Inventory aInv, Player aPlayer) {
		BlockEntity tTileEntity = WD.te(aPlayer.level(), mPos, T);
		if (!(tTileEntity instanceof ITileEntityGUI)) return null;
		final BlockEntity fTileEntity = tTileEntity;
		Object tGUI = ContainerCommon.withWindowID(aWindowID, () -> ((ITileEntityGUI)fTileEntity).getGUIServer(mGUIID, aPlayer));
		return tGUI instanceof AbstractContainerMenu ? (AbstractContainerMenu)tGUI : null;
	}
}

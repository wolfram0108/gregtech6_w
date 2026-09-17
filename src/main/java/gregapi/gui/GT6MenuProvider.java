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

/** @author Gregorius Techneticies (architecture carried over), port by this project
 *  The only MenuProvider implementation in the mod: neo requires Player.openMenu(MenuProvider) since
 *  Forge's IGuiHandler auto-dispatch is gone; the single caller is the same method that opened GUIs before. */
public class GT6MenuProvider implements MenuProvider {
	private final Level mLevel;
	private final BlockPos mPos;
	private final int mGUIID;

	public GT6MenuProvider(Level aLevel, BlockPos aPos, int aGUIID) {
		mLevel = aLevel;
		mPos = aPos;
		mGUIID = aGUIID;
	}

	/** Window title for the open-menu packet, sourced from the inventory's own name contract via LH.get. */
	@Override
	public Component getDisplayName() {
		BlockEntity tTileEntity = WD.te(mLevel, mPos, T);
		if (tTileEntity instanceof ITileEntityInventoryGUI) {
			ITileEntityInventoryGUI tGUI = (ITileEntityInventoryGUI)tTileEntity;
			if (tGUI.hasCustomInventoryNameGUI()) return Component.literal(LH.get(tGUI.getInventoryNameGUI()));
		}
		return Component.literal("");
	}

	/** Server-side container creation uses the same getGUIServer route as client reconstruction; windowId
	 *  passes through the {@link ContainerCommon#withWindowID} bridge. */
	@Override
	public AbstractContainerMenu createMenu(int aWindowID, Inventory aInv, Player aPlayer) {
		BlockEntity tTileEntity = WD.te(aPlayer.level(), mPos, T);
		if (!(tTileEntity instanceof ITileEntityGUI)) return null;
		final BlockEntity fTileEntity = tTileEntity;
		Object tGUI = ContainerCommon.withWindowID(aWindowID, () -> ((ITileEntityGUI)fTileEntity).getGUIServer(mGUIID, aPlayer));
		return tGUI instanceof AbstractContainerMenu ? (AbstractContainerMenu)tGUI : null;
	}
}

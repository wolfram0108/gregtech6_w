/**
 * Copyright (c) 2020 GregTech-6 Team
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

package gregapi.gui;

import static gregapi.data.CS.*;

import gregapi.tileentity.ITileEntityInventoryGUI;
import gregapi.util.ST;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** @author Gregorius Techneticies
 *  remove bridges through ST.nn (the same EMPTY<->null boundary as {@link Slot_Base#remove}); do not
 *  confuse it with the 1.7.10 decrStackSizeGUI contract. */
public class Slot_Holo extends Slot_Base {
	public boolean mCanInsertItem, mCanStackItem;
	public int mMaxStacksize = 127;

	public Slot_Holo(ITileEntityInventoryGUI aInventory, int aIndex, int aX, int aY, boolean aCanInsertItem, boolean aCanStackItem, int aMaxStacksize) {
		super(aInventory, aIndex, aX, aY);
		mCanInsertItem = aCanInsertItem;
		mCanStackItem = aCanStackItem;
		mMaxStacksize = aMaxStacksize;
	}

	@Override
	public boolean mayPlace(ItemStack par1ItemStack) {
		return mCanInsertItem;
	}

	@Override
	public int getMaxStackSize() {
		return mMaxStacksize;
	}

	// The original silenced getHasStack() as its only lever against the vanilla tooltip, but pickup/place/
	// click were already blocked by slot type; lying to hasItem() only broke display, since the engine relies on it.

	@Override
	public ItemStack remove(int par1) {
		if (!mCanStackItem) return ST.nn(null); // neo's ItemStack contract forbids null, unlike the original's plain return null here.
		return super.remove(par1);
	}

	@Override
	public boolean mayPickup(Player par1EntityPlayer) {
		return F;
	}
}

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
 */

package gregapi.gui;

import static gregapi.data.CS.*;

import gregapi.tileentity.ITileEntityInventoryGUI;
import gregapi.util.ST;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * @author Gregorius Techneticies
 *
 * F-GUI: {@code isItemValid}→{@code mayPlace}, {@code getSlotStackLimit}→{@code getMaxStackSize},
 * {@code getHasStack}→{@code hasItem}, {@code decrStackSize}→{@code remove}, {@code canTakeStack}→
 * {@code mayPickup} (движок, см. {@link Slot_Base}). {@code remove} мостит через {@code ST.nn} (F15,
 * та же граница, что {@link Slot_Base#remove}) — не смешивать с 1.7.10-контрактом {@code decrStackSizeGUI}.
 */
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

	@Override
	public boolean hasItem() {
		return F;
	}

	@Override
	public ItemStack remove(int par1) {
		if (!mCanStackItem) return ST.nn(null); // F15-мост: было `return null`, neo ItemStack не бывает null
		return super.remove(par1);
	}

	@Override
	public boolean mayPickup(Player par1EntityPlayer) {
		return F;
	}
}

/**
 * Copyright (c) 2019 Gregorius Techneticies
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

package gregapi.dummies;

import static gregapi.data.CS.*;

import gregapi.util.ST;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

public class DummyInventory implements Container {
	public final ItemStack[] mInventory;
	public DummyInventory(int aSize) {mInventory = new ItemStack[aSize];}
	
	public int getSizeInventory() {return mInventory.length;}
	public ItemStack getStackInSlot(int aSlot) {return mInventory[aSlot];}
	public ItemStack decrStackSize(int aSlot, int aDecrement) {if (mInventory[aSlot] == null) return null; if (mInventory[aSlot].getCount() <= aDecrement) {ItemStack tStack = ST.copy(mInventory[aSlot]); mInventory[aSlot] = NI; return tStack;} ItemStack rStack = mInventory[aSlot].splitStack(aDecrement); if (mInventory[aSlot].getCount() <= 0) mInventory[aSlot] = NI; return rStack;}
	public ItemStack getStackInSlotOnClosing(int aSlot) {ItemStack rStack = mInventory[aSlot]; mInventory[aSlot] = null; return rStack;}
	public void setInventorySlotContents(int aSlot, ItemStack aStack) {mInventory[aSlot] = aStack;}
	public String getInventoryName() {return "DUMMY INVENTORY";}
	public boolean hasCustomInventoryName() {return F;}
	public int getInventoryStackLimit() {return 64;}
	public void markDirty() {/**/}
	public boolean isUseableByPlayer(Player p_70300_1_) {return F;}
	public void openInventory() {/**/}
	public void closeInventory() {/**/}
	public boolean isItemValidForSlot(int p_94041_1_, ItemStack p_94041_2_) {return T;}
}

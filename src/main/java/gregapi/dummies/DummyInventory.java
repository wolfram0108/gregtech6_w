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
 *
 * Modified in 2026 for the GregTech 6 NeoForge port
 * (https://github.com/wolfram0108/gregtech6_w): ported from Minecraft 1.7.10 / Forge
 * to Minecraft 26.1.2 / NeoForge.
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
	
	public int getContainerSize() {return mInventory.length;}
	public boolean isEmpty() {for (int i = 0; i < mInventory.length; i++) if (mInventory[i] != null) return F; return T;}
	// F-loot (заход данжей #39, живой тест «полки пустые»): neo-контракт Container.getItem — НИКОГДА не null
	// (движок зовёт .isEmpty() без гейта: LootTable.getAvailableSlots:206 `container.getItem(i).isEmpty()`).
	// Прежний 1.7.10-возврат null ронял NPE внутри LootTable.fill → ST.generateLoot ловил и возвращал F →
	// ВСЯ ваниль-ветка лута (полки/сундуки данжа по 1.7.10-именам пулов) молча умирала. Внутреннее хранение
	// mInventory остаётся null-able (1:1 — потребители вроде BookShelf итерируют его напрямую по контракту 1.7.10).
	public ItemStack getItem(int aSlot) {return mInventory[aSlot] == null ? ItemStack.EMPTY : mInventory[aSlot];}
	public ItemStack removeItem(int aSlot, int aDecrement) {if (mInventory[aSlot] == null) return null; if (mInventory[aSlot].getCount() <= aDecrement) {ItemStack tStack = ST.copy(mInventory[aSlot]); mInventory[aSlot] = NI; return tStack;} ItemStack rStack = mInventory[aSlot].split(aDecrement); if (mInventory[aSlot].getCount() <= 0) mInventory[aSlot] = NI; return rStack;}
	public ItemStack removeItemNoUpdate(int aSlot) {ItemStack rStack = mInventory[aSlot]; mInventory[aSlot] = null; return rStack;}
	public void setItem(int aSlot, ItemStack aStack) {mInventory[aSlot] = aStack;}
	public String getInventoryName() {return "DUMMY INVENTORY";}
	public boolean hasCustomInventoryName() {return F;}
	public int getMaxStackSize() {return 64;}
	public void setChanged() {/**/}
	public void clearContent() {for (int i = 0; i < mInventory.length; i++) mInventory[i] = NI;}
	public boolean stillValid(Player p_70300_1_) {return F;}
	public void openInventory() {/**/}
	public void closeInventory() {/**/}
	public boolean canPlaceItem(int p_94041_1_, ItemStack p_94041_2_) {return T;}
}

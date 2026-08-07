/**
 * Copyright (c) 2023 GregTech-6 Team
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

import gregapi.code.ItemStackContainer;
import gregapi.code.ItemStackSet;
import gregapi.tileentity.ITileEntityInventoryGUI;
import gregapi.util.ST;
import net.minecraft.world.item.ItemStack;

import static gregapi.data.CS.T;

/**
 * @author Gregorius Techneticies
 *
 * F-GUI: {@code isItemValid}→{@code mayPlace} (движок, см. {@link Slot_Base}).
 */
public class Slot_Whitelist extends Slot_Base {
	private ItemStackSet<ItemStackContainer> mWhiteList = ST.hashset();

	public Slot_Whitelist(ITileEntityInventoryGUI aInventory, int aIndex, int aX, int aY, ItemStack... aValidStacks) {
		super(aInventory, aIndex, aX, aY);
		for (ItemStack aStack : aValidStacks) mWhiteList.add(aStack);
	}

	@Override
	public boolean mayPlace(ItemStack aStack) {
		return super.mayPlace(aStack) && mWhiteList.contains(aStack, T);
	}
}

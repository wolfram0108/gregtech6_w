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

import gregapi.tileentity.ITileEntityInventoryGUI;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** @author Gregorius Techneticies
 *  Item.isValidArmor was removed entirely; replaced with the engine's own ItemStack.canEquip, which fills
 *  the same role. mArmorType (old 0..3 index) maps to the same FEET/LEGS/CHEST/HEAD order used elsewhere. */
public class Slot_Armor extends Slot_Base {
	private static final EquipmentSlot[] ARMOR_SLOTS = {EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD};

	final int mArmorType;
	final Player mPlayer;

	public Slot_Armor(ITileEntityInventoryGUI aInventory, int aIndex, int aX, int aY, int aArmor, Player aPlayer) {
		super(aInventory, aIndex, aX, aY);
		mArmorType = aArmor;
		mPlayer = aPlayer;
	}

	@Override
	public int getMaxStackSize() {
		return 1;
	}

	@Override
	public boolean mayPlace(ItemStack aStack) {
		return aStack != null && aStack.getItem() != null && aStack.canEquip(ARMOR_SLOTS[mArmorType], mPlayer);
	}
}

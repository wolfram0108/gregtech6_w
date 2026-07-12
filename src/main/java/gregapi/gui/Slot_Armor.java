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

import gregapi.tileentity.ITileEntityInventoryGUI;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * @author Gregorius Techneticies
 *
 * F-GUI: {@code getSlotStackLimit}→{@code getMaxStackSize}, {@code isItemValid}→{@code mayPlace} (движок,
 * см. {@link Slot_Base}). {@code Item.isValidArmor(ItemStack,int,Entity)} движок убрал целиком — заменено
 * центральным neo-эквивалентом {@code ItemStack.canEquip(EquipmentSlot,LivingEntity)}
 * (`neoforge-decompiled/net/neoforged/neoforge/common/extensions/IItemStackExtension.java:215-216`,
 * делегирует в `IItemExtension.java:264` — «Determines if the specific ItemStack can be placed in the
 * specified armor slot, for the entity», дословно та же роль); {@code mArmorType} (старый индекс 0..3) →
 * {@link #ARMOR_SLOTS} в том же порядке FEET/LEGS/CHEST/HEAD, что уже установлен центром брони
 * (`gregapi/GT_API_Proxy.java:994`, комментарий «порядок FEET/LEGS/CHEST/HEAD соответствует старому 0..3»).
 */
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

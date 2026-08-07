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

package gregapi.gui;

import static gregapi.data.CS.*;

import gregapi.tileentity.ITileEntityInventoryGUI;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * @author Gregorius Techneticies
 *
 * F-GUI: {@code putStack}→{@code set}; body deliberately bypasses the GT6 GUI-abstraction (uses the raw
 * base {@code Slot.container}/{@code inventory} field, exactly like the original, NOT {@code mInventory})
 * — {@code inventory.setInventorySlotContents}→{@code container.setItem} (движок), {@code getWorldObj().isClientSide()}
 * →{@code getLevel().isClientSide()} (движок, `neo-decompiled/net/minecraft/world/level/Level.java:163`).
 * ГРАНИЦА null↔EMPTY (доработка R8): этот {@code set} НЕ трогает GT6-инвентарь ({@code mInventory}/
 * {@code setInventorySlotContentsGUI}) вообще — пишет ТОЛЬКО в raw neo {@code container.setItem(...)}
 * (`Slot.java:65` — сам движковый {@code Container}, ждёт {@code ItemStack.EMPTY}, не {@code null}) — мост
 * EMPTY→null здесь НЕ нужен, {@code aStack} уже приходит в neo-конвенции (EMPTY) от вызывающего кода.
 */
public class Slot_Render extends Slot_Holo {
	public Slot_Render(ITileEntityInventoryGUI aInventory, int aIndex, int aX, int aY) {
		super(aInventory, aIndex, aX, aY, F, F, 0);
	}

	/**
	 * NEI has a nice and "useful" Delete-All Function, which would delete the Content of this Slot. This is here to prevent that.
	 */
	@Override
	public void set(ItemStack aStack) {
		if (container instanceof BlockEntity && ((BlockEntity)container).getLevel().isClientSide()) {
			container.setItem(getSlotIndex(), aStack);
		}
		setChanged();
	}
}

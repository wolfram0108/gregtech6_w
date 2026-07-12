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

/**
 * @author Gregorius Techneticies
 *
 * F-GUI (доработка R1/R8): оригинальный {@code putStack} (`gregtech6/.../Slot_Normal.java`) был БАЙТ-В-БАЙТ
 * идентичен {@code Slot_Base.putStack} — избыточный override уже в оригинале. Убран (не переопределяем) —
 * наследуется {@link Slot_Base#set}, который несёт мост {@code EMPTY→null} на запись в GT6-инвентарь;
 * поведение то же самое, дублирования (R1) больше нет.
 */
public class Slot_Normal extends Slot_Base {
	public Slot_Normal(ITileEntityInventoryGUI aInventory, int aIndex, int aX, int aY) {
		super(aInventory, aIndex, aX, aY);
	}
}

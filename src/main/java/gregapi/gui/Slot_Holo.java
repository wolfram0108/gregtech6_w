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

	// F-GUI ЦЕНТР ГОЛО-СЛОТА (BUG-082). Оригинал глушил getHasStack() (`Slot_Holo.java:53` → F) — в 1.7.10 это был
	// единственный рычаг, гасивший ванильный тултип содержимого (`GuiContainer.drawScreen:183` — тултип рисуется
	// ТОЛЬКО при theSlot.getHasStack()). Защитой этот рычаг не был и там: взятие запрещает mayPickup (:70, безусловно,
	// включая креатив), вставку — mayPlace (:49), а клики и перекладывание отсекаются ЯВНО по типу слота —
	// ContainerCommon.clicked:498, quickMoveStack:521, merge :561/:582 (все четыре 1:1 с оригиналом :350/:505/:538/:559).
	//
	// В neo hasItem() — не рычаг, а ФАКТ, на который движок опирается в девяти местах (тултип, подсветка-контракты,
	// quick-craft, PICKUP_ALL, CLONE, scroll-действия). Ложь здесь ломала показ и ничего не защищала: до всех клик-путей
	// движка голо-слот попросту не доходит (отсечка выше). Поэтому факт возвращается движку честным (Slot_Base:118 —
	// "есть ли стек"), и тултип содержимого собирает САМ движок своей политикой (AbstractContainerScreen:199-208:
	// getTooltipFromContainerItem + tooltip-image + TOOLTIP_STYLE + проверка предмета на курсоре) — вместо урезанной
	// копии этой политики в экране. Роль GT6-довеска в ContainerClient сузилась обратно до 1.7.10-й: подсказка ПУСТОГО
	// слота (оригинал ContainerClient.drawScreen:81 — ST.invalid(tSlot.getStack())).
	//
	// Отличие от 1.7.10 намеренное и точечное: там содержимое голо-слота показывал внешний слой (NEI, он читал
	// slot.getStack() мимо getHasStack) — в порте такого слоя нет, а функция у игрока была и подтверждена им.

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

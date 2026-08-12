/**
 * Copyright (c) 2026 wolfram0108
 *
 * Written in 2026 for the GregTech 6 NeoForge port
 * (https://github.com/wolfram0108/gregtech6_w). Not part of the original GregTech 6
 * by Gregorius Techneticies; distributed under the same licence as the work it extends.
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

package gregapi.tileentity;

import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.WorldlyContainer;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.wrapper.InvWrapper;
import net.minecraftforge.items.wrapper.SidedInvWrapper;

/**
 * СТОРОННИЙ ВИД НА ИНВЕНТАРЬ GT6 на Forge 1.20.1 — пара к {@link gregapi.fluid.GT6FluidCapability}
 * (тот же класс потери, тот же приём).
 *
 * <p><b>Что было в 1.7.10.</b> Базовые TE объявляли ванильные {@code IInventory}/{@code ISidedInventory}
 * (оригинал {@code TileEntityBase05Inventories:41}, {@code TileEntityBase06Covers:62}) — и этого хватало:
 * чужая воронка, труба, сортировщик читали инвентарь машины без единой строчки про GT6.
 *
 * <p><b>Что стало в 1.20.1.</b> Ванильные интерфейсы на месте ({@code Container}/{@code WorldlyContainer},
 * перенесены 1:1), их по-прежнему понимает ванильная воронка; модовому транспорту нужен
 * {@code ForgeCapabilities.ITEM_HANDLER} ({@code ForgeCapabilities.java:23}). Объявляет его сам
 * BlockEntity — {@link gregapi.tileentity.base.TileEntityBase01Root#getCapability}, единственный вход
 * для всей иерархии (в ветке 26.x эту роль играло событие {@code RegisterCapabilitiesEvent}, которого
 * в 1.20.1 нет).
 *
 * <p><b>Своей логики переноса предметов здесь нет.</b> Ванильный инвентарь оборачивается ШТАТНОЙ
 * обёрткой движка {@link SidedInvWrapper} (прямой аналог {@code ISidedInventory} 1.7.10) —
 * правила «что откуда можно брать» остаются целиком за GT6: обёртка спрашивает его же
 * {@code getSlotsForFace}/{@code canPlaceItemThroughFace}/{@code canTakeItemThroughFace}.
 *
 * <p><b>Класса дефекта BUG-082 в 1.20.1 не существует</b> (тот же разряд, что «протухший Holder» и
 * «ловушка иммутабельности» прошлых заходов — исчезнувший класс, а не отложенность). В 26.x запрос
 * БЕЗ стороны отдавал весь массив мимо фильтра мода, а движковая обёртка при {@code side == null}
 * не спрашивала {@code canTakeItemThroughFace} — под это в порте был заведён свой {@code SidelessView}.
 * Форжевая обёртка 1.20.1 гейтов не теряет: {@code SidedInvWrapper.getSlots/getStackInSlot} идут через
 * {@code inv.getSlotsForFace(side)} ({@code SidedInvWrapper.java:67,97}), а
 * {@code canPlaceItemThroughFace}/{@code canTakeItemThroughFace} проверяются БЕЗУСЛОВНО, в том числе при
 * {@code side == null} ({@code SidedInvWrapper.java:131,167,228}). Сторона {@code null} доезжает до
 * {@code getSlotsForFace(null)} → {@code TileEntityBase06Covers:322} → {@code getAccessibleSlotsFromSide2(6)}
 * = родной GT6 {@code SIDE_ANY}. Свой класс-надстройка снят вместе с причиной.
 */
public final class GT6ItemCapability {
	private GT6ItemCapability() {}

	/** Есть ли что показывать наружу: пустой инвентарь — не то же, что его отсутствие. */
	public static boolean hasInventory(net.minecraft.world.level.block.entity.BlockEntity aBlockEntity) {
		try {
			return aBlockEntity instanceof Container tContainer && tContainer.getContainerSize() > 0;
		} catch (Throwable e) {return false;} // логика конкретного TE не должна ронять чужой мод, который просто спросил капу
	}

	/** Хендлер, привязанный к стороне запроса ({@code null} = sideless-запрос = родной GT6 {@code SIDE_ANY}). */
	public static IItemHandler handlerOf(net.minecraft.world.level.block.entity.BlockEntity aBlockEntity, Direction aSide) {
		if (aBlockEntity instanceof WorldlyContainer tWorldly) return new SidedInvWrapper(tWorldly, aSide);
		return new InvWrapper((Container)aBlockEntity);
	}
}

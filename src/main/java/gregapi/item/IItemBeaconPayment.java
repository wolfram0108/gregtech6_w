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

package gregapi.item;

import net.minecraft.world.item.ItemStack;

/**
 * Контракт-возрождение Forge-хука 1.7.10 {@code Item.isBeaconPayment(ItemStack)} (Forge Item.java:1482,
 * удалён в neo — оплата маяка стала данными: тег {@code ItemTags.BEACON_PAYMENT_ITEMS}, ItemTags.java:135).
 * Тег стоит на Item и не видит материал в данных стека, а GT6 держит один предмет на префикс — поэтому
 * пер-стековый ответ носителя возвращается мостом: {@code GT_API_Proxy.isBeaconPayment(ItemStack)}
 * (центральный предикат: тег ИЛИ этот контракт) + подмена слота 0 ванильного {@code BeaconMenu} на
 * открытии меню (сервер {@code PlayerContainerEvent.Open}, клиент {@code ScreenEvent.Opening}).
 * Отбор носителей — по контракту, не по иерархии.
 */
public interface IItemBeaconPayment {
	/** @return true, если этим стеком можно оплатить маяк (в 1.7.10 маяк спрашивал сам предмет: TileEntityBeacon.isItemValidForSlot:409). */
	public boolean isBeaconPayment(ItemStack aStack);
}

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

package gregapi.code;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/**
 * F8 — центральный мост ItemStack↔NBT (см. `decisions/F8-nbt-data-components.md`,
 * `REMAP-RULES.md` §C3). В 1.7.10 "предметный NBT" был сырым mutable-полем `NBTTagCompound`
 * прямо на `ItemStack` (`getTagCompound`/`setTagCompound`/`hasTagCompound`). В neo 26.1.2 этих
 * методов больше нет — канал заменён на `DataComponents.CUSTOM_DATA` (обёртка `CustomData`,
 * см. `neo-decompiled/net/minecraft/world/item/component/CustomData.java`).
 * <p>
 * Этот класс воспроизводит старую 1.7.10-семантику 1:1 поверх нового канала:
 * <ul>
 *   <li>{@link #get(ItemStack)} == старое `ItemNBT.get(stack)` (null, если тега нет).</li>
 *   <li>{@link #set(ItemStack, CompoundTag)} == старое `stack.setTagCompound(nbt)`.</li>
 *   <li>{@link #has(ItemStack)} == старое `(ItemNBT.get(stack) != null)`.</li>
 * </ul>
 * <p>
 * ВАЖНО — КОНТРАКТ «мутировал → закоммить»: `CustomData` внутри иммутабельна (каждый `get`/
 * `copyTag` отдаёт КОПИЮ) — в отличие от оригинала 1.7.10, где возвращённый тег был тем же самым
 * объектом, что хранился в стеке (прямая мутация без повторного `setTagCompound` там сохранялась).
 * Любой код, который мутирует тег, полученный из {@link #get(ItemStack)} (или из транзитивных
 * обёрток `UT.NBT.get`/`getNBT`/`getOrCreate`), ОБЯЗАН записать его обратно через
 * {@link #set(ItemStack, CompoundTag)} / `UT.NBT.set` — иначе изменение потеряется. Это относится
 * и к геттероподобным именам: `getOrCreate` тоже возвращает DETACHED-копию.
 * <p>
 * Известные точки, где повторная запись логически невозможна без смены контракта вызывающего
 * (ЗАКРЫТЫ write-back ItemNBT.set — см. память gt6-stub-drops-data-lesson; заметка историческая): `MultiItemTool.setToolDamage`/`isItemStackUsable`,
 * `Behavior_Sonictron.setCurrentIndex`/`setTickTimer` — они возвращают мутируемый тег наружу, а
 * вызывающий его не коммитит. Разбор — `decisions/F8-nbt-data-components.md` §7.
 *
 * @author Gregorius Techneticies
 */
public final class ItemNBT {
	private ItemNBT() {/**/}

	public static CompoundTag get(ItemStack aStack) {
		if (aStack == null || aStack.isEmpty()) return null;
		CustomData tData = aStack.get(DataComponents.CUSTOM_DATA);
		return tData == null || tData.isEmpty() ? null : tData.copyTag();
	}

	public static void set(ItemStack aStack, CompoundTag aNBT) {
		if (aStack == null || aStack.isEmpty()) return;
		if (aNBT == null || aNBT.isEmpty()) {
			aStack.remove(DataComponents.CUSTOM_DATA);
		} else {
			aStack.set(DataComponents.CUSTOM_DATA, CustomData.of(aNBT));
		}
	}

	public static boolean has(ItemStack aStack) {
		if (aStack == null || aStack.isEmpty()) return false;
		CustomData tData = aStack.get(DataComponents.CUSTOM_DATA);
		return tData != null && !tData.isEmpty();
	}
}

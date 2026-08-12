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

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

/**
 * F8 — центральный мост ItemStack↔NBT (см. `decisions/F8-nbt-data-components.md`).
 * В 1.7.10 «предметный NBT» был сырым mutable-полем `NBTTagCompound` прямо на `ItemStack`
 * (`getTagCompound`/`setTagCompound`/`hasTagCompound`). В 26.1.2 канал был заменён на
 * `DataComponents.CUSTOM_DATA` (иммутабельная обёртка `CustomData`) — отсюда и брался этот мост.
 * <p>
 * В 1.20.1 сырой тег предмета живёт снова на самом стеке — `ItemStack.getTag()/getOrCreateTag()/
 * setTag()/hasTag()` (`forge-1201-decompiled/net/minecraft/world/item/ItemStack.java:507,512,516`),
 * то есть ровно как в оригинале. Мост сохранён (весь мод ходит через него — один центр), но тело
 * сведено к форме 1.7.10.
 * <p>
 * <b>Ловушка иммутабельности из ADR §3-bis здесь ОТСУТСТВУЕТ.</b> `getTag()` отдаёт ЖИВОЙ объект
 * тега стека (а не копию, как `CustomData.copyTag()`), поэтому прямая мутация сохраняется сама —
 * как в 1.7.10. Требование «мутировал → закоммить через {@link #set}» перестаёт быть обязательным;
 * существующие commit-вызовы по дереву остаются корректными (повторная запись того же объекта
 * безвредна), потому ни одно место вызова не правится.
 * <p>
 * Семантика 1:1 c 1.7.10 сохранена: {@link #get} отдаёт null, если тега нет или он пуст;
 * {@link #set} с null/пустым тегом снимает тег (нормализация GT6 — `UT.NBT.set` всегда стрипает
 * пустые теги); {@link #has} — есть непустой тег.
 *
 * @author Gregorius Techneticies
 */
public final class ItemNBT {
	private ItemNBT() {/**/}

	public static CompoundTag get(ItemStack aStack) {
		if (aStack == null || aStack.isEmpty()) return null;
		CompoundTag rNBT = aStack.getTag();
		return rNBT == null || rNBT.isEmpty() ? null : rNBT;
	}

	public static void set(ItemStack aStack, CompoundTag aNBT) {
		if (aStack == null || aStack.isEmpty()) return;
		aStack.setTag(aNBT == null || aNBT.isEmpty() ? null : aNBT);
	}

	public static boolean has(ItemStack aStack) {
		if (aStack == null || aStack.isEmpty()) return false;
		CompoundTag tNBT = aStack.getTag();
		return tNBT != null && !tNBT.isEmpty();
	}
}

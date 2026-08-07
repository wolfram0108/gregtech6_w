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
 * Контракт-возрождение Forge-хука 1.7.10 {@code Item.getSmeltingExperience(ItemStack)}: движок спрашивал опыт
 * плавки у ПРЕДМЕТА-РЕЗУЛЬТАТА, и его ответ ПЕРЕКРЫВАЛ карту опыта рецептов (1.7.10
 * FurnaceRecipes.func_151398_b:117-118 — ответ != -1 возвращается сразу, карта не смотрится; дефолт Item = -1
 * «спроси карту»). В neo хук удалён, опыт — поле рецепта; правило целиком восстановлено в
 * {@code gregapi.recipes.FurnaceRecipes.func_151398_b} (хук → карта → 0), а до ванильной печи его доносит
 * {@code GT6SmeltingDispatcher} (экземпляр на класс опыта). Носитель — PrefixItem (самоцвет → 1.0, иначе 0,
 * тело 1:1). Отбор носителей — по контракту, не по иерархии; не-носитель = дефолт -1.
 */
public interface IItemSmeltingExperience {
	/** @return опыт плавки за единицу этого стека-РЕЗУЛЬТАТА; -1 = «не знаю, спроси карту рецептов» (дефолт 1.7.10). */
	public float getSmeltingExperience(ItemStack aStack);
}

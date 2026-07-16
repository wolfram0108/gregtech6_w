/**
 * Copyright (c) 2023 GregTech-6 Team
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

package gregapi.enchants;

import gregapi.data.MD;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.enchantment.Enchantment;

/**
 * @author Gregorius Techneticies
 *
 * <p>Форс движка, см. {@link Enchantment_WerewolfDamage} (тот же класс изменения — record
 * {@code Enchantment}, невозможность {@code extends EnchantmentDamage}/override
 * {@code func_151367_b}). Игровая логика перенесена 1:1 в {@link EnchantmentEffect_Slime};
 * bootstrap — {@link EnchantsGT6#bootstrap}.
 *
 * F8 (1:1): материал→чара назначения (golden ctor addEnchantmentForDamage(this,N)) ПЕРЕНЕСЕНЫ в MT.init() enchant-блок
 * как addEnchantmentForDamage(KEY,N). Материалы снова несут Dissolving. Не заглушка.
 */
public class Enchantment_SlimeDamage {
	public static final ResourceKey<Enchantment> KEY =
		ResourceKey.create(Registries.ENCHANTMENT, Identifier.fromNamespaceAndPath(MD.GAPI.mID, "dissolving"));
}

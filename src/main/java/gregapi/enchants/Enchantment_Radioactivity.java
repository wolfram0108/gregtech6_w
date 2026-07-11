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
 * <p>Форс движка, см. {@link Enchantment_WerewolfDamage}. Игровая логика (делегат в
 * {@code UT.Entities.applyRadioactivity}) перенесена 1:1 в {@link EnchantmentEffect_Radioactivity};
 * bootstrap — {@link EnchantsGT6#bootstrap}.
 *
 * <p>Оригинал (`gregtech6/.../Enchantment_Radioactivity.java:58-81`) переопределял
 * {@code getMinEnchantability=Integer.MAX_VALUE}/{@code getMaxEnchantability=0} (никогда не
 * достижим через стол зачарования) и {@code canApply=false}/{@code isAllowedOnBooks=false} —
 * в data-driven модели это переносится как {@code EnchantmentDefinition} с
 * {@code Enchantment.constantCost(Integer.MAX_VALUE)}/{@code constantCost(0)} (те же литералы,
 * `EnchantsGT6.bootstrap`) и ОТСУТСТВИЕМ членства в теге {@code EnchantmentTags.IN_ENCHANTING_TABLE}
 * (не добавляется нигде в этой зоне) — функционально тот же результат «никогда не выпадает».
 *
 * PORT-TODO(F8, enchant-registry): материал-регистрация (`gregtech6/.../Enchantment_Radioactivity.java:41-54`,
 * {@code MT.Cyanite.addEnchantmentForTools(this,1).addEnchantmentForDamage(this,1)...} и далее по
 * tools/damage/ranged/armors) не перенесена — тот же регистровый тайминг-класс проблемы, что у
 * {@link Enchantment_WerewolfDamage}, решение вне зоны `gregapi/enchants`.
 */
public class Enchantment_Radioactivity {
	public static final ResourceKey<Enchantment> KEY =
		ResourceKey.create(Registries.ENCHANTMENT, Identifier.fromNamespaceAndPath(MD.GAPI.mID, "radioactivity"));
}

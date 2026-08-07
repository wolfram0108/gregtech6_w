/**
 * Copyright (c) 2025 GregTech-6 Team
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

package gregapi.enchants;

import gregapi.data.MD;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.enchantment.Enchantment;

/**
 * @author Gregorius Techneticies
 *
 * <p>Форс движка: {@code net.minecraft.world.item.enchantment.Enchantment} в neo — {@code record}
 * (final, не наследуется, {@code neo-decompiled/.../enchantment/Enchantment.java:60}); прежние
 * {@code extends net.minecraft.enchantment.EnchantmentDamage} и переопределение виртуального
 * {@code func_151367_b} физически невозможны (движок больше не диспетчерит зачарования через
 * override-колбэки — см. `DEFERRED-LEDGER.md` метку {@code ENCHANT, effect-dispatch-engine}).
 * Игровая логика (проверка {@code isWereCreature} + 2 зелья + пасхалка Bear989Sr) перенесена 1:1
 * в {@link EnchantmentEffect_Werewolf}; полная сборка объекта чара (definition/cost/slots/эффект)
 * и запись в датапак-реестр {@code Registries.ENCHANTMENT} — {@link EnchantsGT6#bootstrap}.
 *
 * <p>{@link #KEY} — стабильная modern-идентичность этого чара (заменяет прежний
 * {@code public static Enchantment_WerewolfDamage INSTANCE}, который держал сам Java-объект чара;
 * в data-driven модели чар — не Java-объект, а запись реестра, адресуемая {@code ResourceKey}).
 *
 * F8 (1:1): прежние вызовы {@code MT.Ir.addEnchantmentForDamage(this, 6)} и далее по всем материалам ПЕРЕНЕСЕНЫ
 * в {@code MT.init()} (enchant-блок) как {@code Ir.addEnchantmentForDamage(KEY, 6)}. Тайминг НЕ мешает: список хранит
 * {@code ResourceKey} (не резолвнутый Holder), разрешаемый лишь при применении чара (позже RegistryAccess готов).
 * Материалы (Ir/Osmiridium/HSSS/Ag/…) снова несут Werebane. Не заглушка.
 */
public class Enchantment_WerewolfDamage {
	public static final ResourceKey<Enchantment> KEY =
		ResourceKey.create(Registries.ENCHANTMENT, Identifier.fromNamespaceAndPath(MD.GAPI.mID, "werebane"));
}

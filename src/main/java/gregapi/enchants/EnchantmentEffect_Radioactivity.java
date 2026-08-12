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


package gregapi.enchants;

import gregapi.util.UT;
import net.minecraft.world.entity.Entity;

/**
 * @author Gregorius Techneticies
 *
 * Игровая логика чара {@code Enchantment_Radioactivity} (оригинал 1.7.10 —
 * {@code gregapi/enchants/Enchantment_Radioactivity.java:84-86}, метод {@code func_151367_b}).
 * В 26.x-версии порта тело жило в record-е {@code EnchantmentEntityEffect} (движок диспетчерил чары
 * data-driven); в 1.20.1 движковая модель чар снова императивная — {@code Enchantment.doPostHurt}
 * ({@code forge-1201-decompiled/.../enchantment/Enchantment.java:121}), поэтому носитель снова
 * обычный метод, вызываемый из {@link Enchantment_Radioactivity}. Значения не изменены.
 */
public final class EnchantmentEffect_Radioactivity {
	private EnchantmentEffect_Radioactivity() {}

	public static void apply(Entity aEntity, int aLevel) {
		UT.Entities.applyRadioactivity(aEntity, aLevel, 1);
	}
}

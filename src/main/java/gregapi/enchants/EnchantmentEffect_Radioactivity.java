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

/** @author Gregorius Techneticies
 *  1.20.1 runs enchantments imperatively via Enchantment.doPostHurt rather than a data-driven record, so this
 *  body is an ordinary method again, called from Enchantment_Radioactivity; values are unchanged. */
public final class EnchantmentEffect_Radioactivity {
	private EnchantmentEffect_Radioactivity() {}

	public static void apply(Entity aEntity, int aLevel) {
		UT.Entities.applyRadioactivity(aEntity, aLevel, 1);
	}
}

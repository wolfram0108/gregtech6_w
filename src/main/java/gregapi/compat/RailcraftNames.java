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

package gregapi.compat;

import gregapi.data.MD;

/** Railcraft's table in ForeignNames: 1.7.10 item names onto Railcraft Reborn 1.1.11 ids (reference/mods/Railcraft-Reborn).
 *  The carrier is the real mod id; MD.RC keeps the legacy id and stays unloaded, so the ~150 legacy-API uses of it stay dead. */
public final class RailcraftNames {
	private RailcraftNames() {}

	public static final ForeignNames TABLE = new ForeignNames(MD.RC).carrier(MD.RC_REBORN);

	private static final String
	  NO_DARK_ORE = "Railcraft Reborn has no abyssal diamond, emerald or lapis ore (1.7.10 ore metas 2, 3 and 4)"
	, NO_CELL = "Railcraft Reborn has no creosote cell, only creosote_bucket";

	static {
		TABLE.map("cube", 0, "railcraft:coal_coke_block");
		TABLE.map("fuel.coke", "railcraft:coal_coke");
		TABLE.map("firestone.raw", "railcraft:raw_firestone");
		TABLE.map("ore", 0, "railcraft:sulfur_ore");
		TABLE.map("ore", 1, "railcraft:saltpeter_ore");
		TABLE.gone("ore", 2, NO_DARK_ORE);
		TABLE.gone("ore", 3, NO_DARK_ORE);
		TABLE.gone("ore", 4, NO_DARK_ORE);
		TABLE.map("ore", 5, "railcraft:firestone_ore");
		TABLE.map("part.plate", 0, "railcraft:iron_plate");
		TABLE.map("part.plate", 1, "railcraft:steel_plate");
		TABLE.map("part.plate", 2, "railcraft:tin_plate");
		TABLE.map("part.plate", 3, "railcraft:copper_plate");
		TABLE.map("part.plate", 4, "railcraft:lead_plate");
		TABLE.gone("fluid.creosote.cell", 1, NO_CELL);
	}
}

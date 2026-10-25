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

package gregapi.compat.railcraft;

import gregapi.item.multiitem.MultiItemToolWithCompat;
import mods.railcraft.api.item.Crowbar;

/** Railcraft Reborn knows a crowbar by instanceof Crowbar, which may be declared only with Railcraft installed, so
 *  Loader_Tools builds the tool from this class by name then; the answers are inherited from MultiItemToolWithCompat. */
public class MultiItemToolWithCompatRC extends MultiItemToolWithCompat implements Crowbar {
	public MultiItemToolWithCompatRC(String aModID, String aUnlocalized) {
		super(aModID, aUnlocalized);
	}
}

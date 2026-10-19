/**
 * Copyright (c) 2020 Gregtech6-Unofficial Team
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
 * Modified in 2026 for the GregTech 6 NeoForge port (https://github.com/wolfram0108/gregtech6_w):
 * the GregTech6-Unofficial additions of gregapi/data/BI.java moved into an addon class.
 */

package gt6u.data;

import gregapi.data.BI;
import gregapi.old.Textures;
import gregapi.render.IIconContainer;

/** The icons GregTech6-Unofficial added to BI and Textures.BlockIcons (GT6U BI.java:113, Textures.java:41). */
public class BI_GT6U {
	public static final IIconContainer CHAR_LP = new BI.Icon("overlays/characters/lp"), FLUID_WELL = new Textures.BlockIcons.CustomIcon("iconsets/FLUID_WELL");
}

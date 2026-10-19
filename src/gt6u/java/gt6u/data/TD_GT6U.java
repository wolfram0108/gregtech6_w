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
 * the GregTech6-Unofficial additions of gregapi/data/TD.java moved into an addon class.
 */

package gt6u.data;

import gregapi.code.TagData;
import gregapi.data.LH;
import gregapi.data.TD;

/** The tags GregTech6-Unofficial added to TD (GT6U TD.java:59, :144-151, :556). */
public class TD_GT6U {
	public static final TagData
	  WIRE_OpticalFiber = TagData.createTagData("CONNECTORS.WIRE_OPTICSLFIBER", "Optical Fiber")
	/** Energy Tag for light Energy (from the sun). */
	, LP                = TagData.createTagData("ENERGY.LP", "LP", "Light Energy", LH.Chat.YELLOW)
	, OPTICALFIBERS     = TagData.createTagData("ITEMGENERATOR.OPTICALFIBERS")
	;
	
	/** GT6U lists LP among GT6's energy types right after LU, and allows it negative (GT6U TD.java:202, :212). */
	public static void addToGT6() {
		TD.Energy.ALL.add(TD.Energy.ALL.indexOf(TD.Energy.LU) + 1, LP);
		TD.Energy.ALL_NEGATIVE_ALLOWED.add(LP);
	}
}

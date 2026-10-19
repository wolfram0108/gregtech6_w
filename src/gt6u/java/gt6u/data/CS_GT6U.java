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
 * the GregTech6-Unofficial additions of gregapi/data/CS.java moved into an addon class.
 */

package gt6u.data;

import gregapi.item.multiitem.MultiItemRandom;

/** Constants GregTech6-Unofficial added to CS (GT6U CS.java:159-173, :1555, :1680). */
public class CS_GT6U {
	/** Durations in ticks from 1 second to 1 hour. */
	public static final long[] DT = {20, 100, 200, 600, 1200, 3600, 6000, 12000, 18000, 36000, 72000};
	
	/** GT6U's electric EV tools; the ids are those GT6U gave them in GT6's MetaTool, here in the addon's own. */
	public static final short MININGDRILL_EV = 106, CHAINSAW_EV = 116, WRENCH_EV = 126, JACKHAMMER_EV_Normal = 134, MONKEY_WRENCH_EV = 186;
	
	/** GT6U's MultiItems: physics and biology, and its technological items, which GT6U kept in GT6's own. */
	public static MultiItemRandom PHYS, BIO, TECH;
}

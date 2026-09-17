/**
 * Copyright (c) 2019 Gregorius Techneticies
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

package gregapi.old;

import gregapi.render.IIconContainer;

/** The immediate-mode Tessellator drawing this used is gone in 26.1.2; its replacement is GT6QuadBuilder.
 *  Nothing in the mod calls this utility any more, so the body is left as a dead no-op. */
public class GT_RenderUtil {
	public static void renderItemIcon(IIconContainer icon, double size, double z, float nx, float ny, float nz) {
		renderItemIcon(icon, 0, 0, size, size, z, nx, ny, nz);
	}

	/** Dead, see the class javadoc; body is a no-op. */
	public static void renderItemIcon(IIconContainer icon, double xStart, double yStart, double xEnd, double yEnd, double z, float nx, float ny, float nz) {
		if (icon == null) return;
		//
	}
}

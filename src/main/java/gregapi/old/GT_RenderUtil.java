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
 */

package gregapi.old;

import gregapi.render.IIconContainer;

/**
 * F3-render: 1.7.10 рисовал квад иконки immediate-mode ({@code Tessellator}) — стек удалён в 26.1.2. Замена рисования —
 * {@link gregapi.render.GT6QuadBuilder}/{@code QuadBakingVertexConsumer} (F3-render.md §8). {@code IIcon}→{@link IIconContainer}.
 * Этот util НЕ вызывается ни из одного места мода (нет ссылающихся файлов) — мёртв, тело no-op.
 */
public class GT_RenderUtil {
	public static void renderItemIcon(IIconContainer icon, double size, double z, float nx, float ny, float nz) {
		renderItemIcon(icon, 0, 0, size, size, z, nx, ny, nz);
	}

	/** F3-render: было immediate-mode рисование квада; мёртв (см. class javadoc). No-op. */
	public static void renderItemIcon(IIconContainer icon, double xStart, double yStart, double xEnd, double yEnd, double z, float nx, float ny, float nz) {
		if (icon == null) return;
		//
	}
}

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
 * PORT-TODO(F3, baked-рендер клиента): 1.7.10 рисовал квад иконки прямо в мир ({@code Tessellator}
 * immediate-mode: {@code startDrawingQuads/setNormal/addVertexWithUV/draw}) — весь стек удалён в
 * 26.1.2 (decisions/F3-render.md §1, класс {@code com.mojang.blaze3d.vertex.Tesselator} больше не
 * даёт прямого доступа к {@code instance}/immediate-режиму). Параметр {@code IIcon} (тип удалён)
 * заменён центральной поверхностью {@link IIconContainer} (см. её PORT-TODO(F3) на {@code getIcon}).
 * Реальная замена рисования — {@code CubeBuilder}/{@code QuadBakingVertexConsumer} (F3-render.md §2.2);
 * не вызывается ни из одного места мода (нет ссылающихся файлов) — тело temporарно no-op до той фазы.
 */
public class GT_RenderUtil {
	public static void renderItemIcon(IIconContainer icon, double size, double z, float nx, float ny, float nz) {
		renderItemIcon(icon, 0, 0, size, size, z, nx, ny, nz);
	}

	/** PORT-TODO(F3, baked-рендер клиента): было immediate-mode рисование квада (см. class javadoc). */
	public static void renderItemIcon(IIconContainer icon, double xStart, double yStart, double xEnd, double yEnd, double z, float nx, float ny, float nz) {
		if (icon == null) return;
		//
	}
}

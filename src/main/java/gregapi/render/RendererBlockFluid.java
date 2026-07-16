/**
 * Copyright (c) 2023 GregTech-6 Team
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

package gregapi.render;

/**
 * F3-render: в 1.7.10 рисовал жидкостный блок immediate-mode (Forge {@code BlockFluidBase}/{@code FluidRegistry}, всё удалено).
 * Геометрия жидкости в neo — baked через {@link GT6BlockModel} (fluid-блок как IRenderedBlock) ЛИБО neo fluid-система; старый
 * Forge-кастом-жидкостный рендер сюда не относится (F5 закрыт отдельно). Этот класс держит лишь серверную поверхность:
 * {@code RENDER_ID}/{@code INSTANCE} (читаются блок-классами как "id рендер-типа") — no-op-совместимость.
 */
public class RendererBlockFluid {
	public static int RENDER_ID = 0;
	public static RendererBlockFluid INSTANCE;

	public RendererBlockFluid(int aRenderID) {
		INSTANCE = this;
		RENDER_ID = aRenderID;
	}
}

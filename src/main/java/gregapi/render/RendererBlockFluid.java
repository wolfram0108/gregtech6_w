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
 * PORT-TODO(F3, baked-рендер клиента): в 1.7.10 рисовал жидкостный блок immediate-mode через
 * {@code Tesselator}/{@code RenderBlocks} по Forge-API {@code BlockFluidBase}/{@code IFluidBlock}/
 * {@code FluidRegistry} (все удалены в 26.1.2, и все — старый Forge-API кастомных жидкостей, не
 * относящийся к владельцу F5 {@code gregapi.fluid}/{@code FL} — REMAP-RULES §C4; F5 закрыт, сюда не
 * лезем). Реальная замена геометрии жидкости — {@code DynamicBlockStateModel.collectParts()} +
 * {@code CubeBuilder} по высоте потока (decisions/F3-render.md §2.1-2.2), клиентская baked-фаза.
 * Здесь — только серверная поверхность: {@code RENDER_ID}/{@code INSTANCE} (читаются внешними
 * блок-классами как "id рендер-типа жидкости"), тело рендера гатится до no-op.
 */
public class RendererBlockFluid {
	public static int RENDER_ID = 0;
	public static RendererBlockFluid INSTANCE;

	public RendererBlockFluid(int aRenderID) {
		INSTANCE = this;
		RENDER_ID = aRenderID;
	}
}

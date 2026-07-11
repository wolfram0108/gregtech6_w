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

package gregapi.render;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.minecraft.resources.Identifier;

/**
 * @author Gregorius Techneticies
 *
 * PORT-TODO(F3, серверная поверхность): 1.7.10 {@code IIcon}/{@code IIconRegister} (immediate-mode
 * атлас-стежка) удалены в 26.1.2 целиком, замены не существует до фазы baked-рендера клиента —
 * см. {@code decisions/F3-render.md} §2.3 (реальный держатель текстуры на клиенте будет
 * {@code net.minecraft.client.resources.model.Material}, разрешаемый через {@code ModelBaker.materials()}).
 * До той фазы этот интерфейс отдаёт нейтральный держатель ссылки на текстуру ({@link Identifier}),
 * достаточный чтобы весь мод (141+ мест) компилировался против ОДНОЙ центральной поверхности.
 */
public interface IIconContainer {
	/**
	 * @return держатель ссылки на текстуру для этого Render Pass.
	 * PORT-TODO(F3, baked-рендер клиента): было {@code IIcon getIcon(int)}.
	 */
	@OnlyIn(Dist.CLIENT)
	public Identifier getIcon(int aRenderPass);

	/**
	 * @return if this Render Pass uses Color Modulation.
	 */
	@OnlyIn(Dist.CLIENT)
	public boolean isUsingColorModulation(int aRenderPass);

	/**
	 * @return the Color Modulation of the Icon.
	 */
	@OnlyIn(Dist.CLIENT)
	public short[] getIconColor(int aRenderPass);

	/**
	 * @return the Amount of Render Passes for this Icon.
	 */
	@OnlyIn(Dist.CLIENT)
	public int getIconPasses();

	/**
	 * @return the Default Texture File for this Icon.
	 */
	@OnlyIn(Dist.CLIENT)
	public Identifier getTextureFile();

	/**
	 * Registers the Icon of this IconContainer.
	 * PORT-TODO(F3, baked-рендер клиента): было {@code registerIcons(IIconRegister)} (атлас-стежка
	 * 1.7.10, тип удалён). Параметр — нейтральный держатель до реальной привязки к
	 * {@code ModelBaker.materials()}/атласу; реализациям следует не-op'ить на сервере.
	 */
	@OnlyIn(Dist.CLIENT)
	public void registerIcons(Object aIconRegister);
}

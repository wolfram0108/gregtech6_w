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

package gregapi.render;

import net.neoforged.api.distmarker.Dist;
import net.minecraft.resources.ResourceLocation;

/**
 * @author Gregorius Techneticies
 *
 * F3 (держатель текстуры): 1.7.10 {@code IIcon}/{@code IIconRegister} (immediate-mode атлас-стежка) удалены
 * в 26.1.2 целиком. Фаза baked-рендера ПРОЙДЕНА — держателем стал {@link ResourceLocation}, а резолв в
 * {@code TextureAtlasSprite} централизован в {@code GT6QuadBuilder.resolveSprite}; на этом канале работают
 * {@code GT6BlockModel}/{@code GT6ItemModel} (текстуры мультиблоков — BUG-061, item-модели — BUG-068, оба
 * приняты живым тестом игрока). Долгом это больше не является: интерфейс — и есть neo-поверхность,
 * одна на весь мод (141+ мест).
 */
public interface IIconContainer {
	/**
	 * @return держатель ссылки на текстуру для этого Render Pass.
	 * F3 superseded-render (GT6BlockModel/ItemModel пайплайн; старый getIcon/immediate-mode мёртв, 0 вызовов neo): было {@code IIcon getIcon(int)}.
	 */
	public ResourceLocation getIcon(int aRenderPass);

	/**
	 * @return if this Render Pass uses Color Modulation.
	 */
	public boolean isUsingColorModulation(int aRenderPass);

	/**
	 * @return the Color Modulation of the Icon.
	 */
	public short[] getIconColor(int aRenderPass);

	/**
	 * @return the Amount of Render Passes for this Icon.
	 */
	public int getIconPasses();

	/**
	 * @return the Default Texture File for this Icon.
	 */
	public ResourceLocation getTextureFile();

	/**
	 * Registers the Icon of this IconContainer.
	 * F3 superseded-render (GT6BlockModel/ItemModel пайплайн; старый getIcon/immediate-mode мёртв, 0 вызовов neo): было {@code registerIcons(IIconRegister)} (атлас-стежка
	 * 1.7.10, тип удалён). Параметр — нейтральный держатель до реальной привязки к
	 * {@code ModelBaker.materials()}/атласу; реализациям следует не-op'ить на сервере.
	 */
	public void registerIcons(Object aIconRegister);
}

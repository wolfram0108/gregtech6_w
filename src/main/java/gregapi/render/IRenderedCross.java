/**
 * Copyright (c) 2026 GregTech-6 Team
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

import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;

/**
 * F3-render: маркер «cross-модель» (растения/цветы — X-форма из 2 диагональных плоскостей, а не куб).
 * {@link GT6BlockModel} ветвится по нему: вместо 6 граней куба зовёт {@link GT6QuadBuilder#crossFace} с иконкой,
 * которую отдаёт {@link #getCrossIcon} (per-мета через {@code WD.meta}). Наследует {@link IRenderedBlock} (чтобы
 * инъекция {@code onModifyBakingResult}, фильтрующая по {@code IRenderedBlock}, покрывала и cross-блоки), но
 * кубические методы IRenderedBlock cross-путём НЕ вызываются — здесь дефолтные заглушки для контракта.
 */
public interface IRenderedCross extends IRenderedBlock {
	/** Иконка cross-модели (растение) для позиции; per-мета — сам блок читает {@code WD.meta}. {@code aWorld==null} = item-рендер (мета 0). */
	Identifier getCrossIcon(BlockGetter aWorld, int aX, int aY, int aZ);
	/** Оттенок cross-модели (0..255 RGBa), {@code null} = белый (без тинта). */
	default short[] getCrossRGBa(BlockGetter aWorld, int aX, int aY, int aZ) {return null;}

	// --- Дефолты IRenderedBlock: cross-путь GT6BlockModel их НЕ зовёт (ветвится на getCrossIcon), нужны лишь для контракта интерфейса. ---
	@Override default ITexture getTexture(int aRenderPass, byte aSide, ItemStack aStack) {return null;}
	@Override default ITexture getTexture(int aRenderPass, byte aSide, boolean[] aShouldSideBeRendered, BlockGetter aWorld, int aX, int aY, int aZ) {return null;}
	@Override default boolean usesRenderPass(int aRenderPass, ItemStack aStack) {return aRenderPass == 0;}
	@Override default boolean usesRenderPass(int aRenderPass, BlockGetter aWorld, int aX, int aY, int aZ, boolean[] aShouldSideBeRendered) {return aRenderPass == 0;}
	@Override default boolean setBlockBounds(int aRenderPass, ItemStack aStack) {return false;}
	@Override default boolean setBlockBounds(int aRenderPass, BlockGetter aWorld, int aX, int aY, int aZ, boolean[] aShouldSideBeRendered) {return false;}
	@Override default int getRenderPasses(ItemStack aStack) {return 1;}
	@Override default int getRenderPasses(BlockGetter aWorld, int aX, int aY, int aZ, boolean[] aShouldSideBeRendered) {return 1;}
	@Override default IRenderedBlockObject passRenderingToObject(ItemStack aStack) {return null;}
	@Override default IRenderedBlockObject passRenderingToObject(BlockGetter aWorld, int aX, int aY, int aZ) {return null;}
}

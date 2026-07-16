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
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * F3-render: 1.7.10 immediate-mode GUI/overlay-помощники. Инвентарный рендер предмета в neo — через baked item-модель
 * ({@link gregapi.render.GT6ItemModel}) + {@code GuiGraphics.renderItem} на call-site; {@code renderItemIntoGUI} здесь мёртв
 * (0 вызывателей, суперседирован). {@code mRenderBlocks} — нейтральный held-объект ("передать дальше"). Единственное живое —
 * {@code drawWrenchOverlay} (косметический overlay соединений труб/проводов при наведении с гаечным ключом).
 */
public class RenderHelper {
	/** F3-render: было {@code RenderBlocks} (тип удалён); нейтральный held-объект, тело не трогается. */
	public static Object mRenderBlocks = null;

	/** F3-render: было immediate-mode RenderItem/GL11; в neo инвентарь-рендер — baked GT6ItemModel + GuiGraphics.renderItem. Мёртв (0 вызывателей). */
	public static void renderItemIntoGUI(Font aFontRenderer, TextureManager aTextureManager, ItemStack aStack, int aX, int aY, boolean aEffect) {
		//
	}

	/** PORT-TODO(F3, косметический overlay соединений — живой highlight-путь): 1.7.10 рисовал линии труб/проводов сырым GL11.
	 *  neo — линии через {@code PoseStack}+{@code MultiBufferSource} из highlight-события; косметика, не влияет на паритет/логику.
	 *  Крах сломал бы рендер каждого выделения блока → no-op до реализации overlay-геометрии. */
	public static void drawWrenchOverlay(Player aPlayer, int aX, int aY, int aZ, byte aConnections, byte aSide, float aPartialTicks) {
		//
	}
}

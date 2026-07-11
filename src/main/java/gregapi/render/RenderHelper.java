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
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * PORT-TODO(F3, baked-рендер клиента): оба метода в 1.7.10 рисовали immediate-mode через
 * {@code RenderBlocks}/{@code RenderItem}/{@code GL11} (инвентарный рендер предмета/блока и
 * wrench-overlay поверх грани) — весь стек удалён в 26.1.2 (decisions/F3-render.md §1). Реальная
 * замена инвентарного рендера — {@code ItemStackRenderState}/{@code ItemModelResolver}
 * (decisions/F3-render.md §2.5, "IItemRenderer" в таблице §3); wrench-overlay ляжет на
 * {@code SubmitNodeCollector}-путь BER/пост-рендера. {@code mRenderBlocks} держит нейтральный
 * держатель {@code Object} — используется внешними местами только как "передать дальше", своё
 * тело не трогает.
 */
@OnlyIn(Dist.CLIENT)
public class RenderHelper {
	/** PORT-TODO(F3, baked-рендер клиента): было {@code RenderBlocks} (тип удалён). */
	public static Object mRenderBlocks = null;

	/** PORT-TODO(F3, baked-рендер клиента): было immediate-mode RenderItem/RenderBlocks через GL11 (см. class javadoc). */
	public static void renderItemIntoGUI(Font aFontRenderer, TextureManager aTextureManager, ItemStack aStack, int aX, int aY, boolean aEffect) {
		//
	}

	/** PORT-TODO(F3, baked-рендер клиента): было отладочное наложение проводов/труб через сырой GL11 (см. class javadoc). */
	public static void drawWrenchOverlay(Player aPlayer, int aX, int aY, int aZ, byte aConnections, byte aSide, float aPartialTicks) {
		//
	}
}

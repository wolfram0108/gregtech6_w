/**
 * Copyright (c) 2021 GregTech-6 Team
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

import static gregapi.data.CS.*;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.resources.Identifier;

/**
 * @author Gregorius Techneticies
 *
 * PORT-TODO(F3, серверная поверхность): в 1.7.10 этот интерфейс дёргал по каждой стороне напрямую
 * immediate-mode рендерер (RenderBlocks/Tessellator/GL11/IIcon) — весь этот стек в 26.1.2 удалён
 * без замены (см. decisions/F3-render.md §1). Реальная замена —
 * {@code DynamicBlockStateModel.collectParts(level,pos,state,random,parts)} +
 * {@code CubeBuilder}/{@code QuadBakingVertexConsumer} (decisions/F3-render.md §2.1-2.2) — это
 * клиентский baked-рендер, отдельная поздняя фаза. Этот файл хранит только 1:1-структуру
 * (имена методов/форму классов) как серверную компилирующуюся поверхность: {@code RenderBlocks}
 * заменён нейтральным держателем {@code Object aRenderer}, {@code IIcon} — на {@link Identifier},
 * а всё тело AO/Tessellator (было ~700 строк) сведено к no-op заглушкам с PORT-TODO.
 */
public interface ITexture {
	public void renderXPos(Object aRenderer, Block aBlock, int aX, int aY, int aZ, int aBrightness, boolean aChangedBlockBounds);
	public void renderXNeg(Object aRenderer, Block aBlock, int aX, int aY, int aZ, int aBrightness, boolean aChangedBlockBounds);
	public void renderYPos(Object aRenderer, Block aBlock, int aX, int aY, int aZ, int aBrightness, boolean aChangedBlockBounds);
	public void renderYNeg(Object aRenderer, Block aBlock, int aX, int aY, int aZ, int aBrightness, boolean aChangedBlockBounds);
	public void renderZPos(Object aRenderer, Block aBlock, int aX, int aY, int aZ, int aBrightness, boolean aChangedBlockBounds);
	public void renderZNeg(Object aRenderer, Block aBlock, int aX, int aY, int aZ, int aBrightness, boolean aChangedBlockBounds);

	public boolean isValidTexture();

	/**
	 * PORT-TODO(F3, baked-рендер клиента): было Tessellator/GL11/RenderBlocks immediate-mode рендер
	 * (per-side UV + Ambient-Occlusion, ~700 строк). Реальная замена — CubeBuilder/QuadCollection.Builder
	 * внутри DynamicBlockStateModel.collectParts() (decisions/F3-render.md §2.1-2.2). До той фазы все
	 * методы этого держателя — no-op, сохраняющий лишь булев признак "текстура задана".
	 */
	public static class Util {
		public static boolean OPTIFINE_LOADED = F, GT_ALPHA_BLENDING = F, MC_ALPHA_BLENDING = F, IS_RENDERING_ALPHA = F;

		/** PORT-TODO(F3, baked-рендер клиента): было RenderBlocks/GL11 alpha-blending Setup вокруг многопроходного рендера. */
		public static void startRendering(Object aRenderer, Block aBlock, BlockGetter aWorld, int aX, int aY, int aZ) {
			//
		}

		/** PORT-TODO(F3, baked-рендер клиента): было RenderBlocks/GL11 alpha-blending Teardown. */
		public static void endRendering(Object aRenderer, Block aBlock, BlockGetter aWorld, int aX, int aY, int aZ) {
			//
		}

		//=============================================================================================================
		// PORT-TODO(F3, baked-рендер клиента): prepare+do+applyAmbientOcclusion Rendering (было ~700 строк
		// Tessellator.addVertexWithUV/GL11 по каждой стороне) — удалено, заменит CubeBuilder внутри collectParts().
		//=============================================================================================================

		public static boolean renderSide(byte aSide, Identifier aIcon, short[] aRGBa, boolean aAllowAlpha, boolean aUseConstantBrightness, boolean aEnableAO, Object aRenderer, Block aBlock, int aX, int aY, int aZ, int aBrightness, boolean aChangedBlockBounds) {
			if (aIcon == null) return F;
			switch(aSide) {
			case SIDE_Y_NEG: return renderYNeg(aIcon, aRGBa, aAllowAlpha, aUseConstantBrightness, aEnableAO, aRenderer, aBlock, aX, aY, aZ, aBrightness, aChangedBlockBounds);
			case SIDE_Y_POS: return renderYPos(aIcon, aRGBa, aAllowAlpha, aUseConstantBrightness, aEnableAO, aRenderer, aBlock, aX, aY, aZ, aBrightness, aChangedBlockBounds);
			case SIDE_Z_NEG: return renderZNeg(aIcon, aRGBa, aAllowAlpha, aUseConstantBrightness, aEnableAO, aRenderer, aBlock, aX, aY, aZ, aBrightness, aChangedBlockBounds);
			case SIDE_Z_POS: return renderZPos(aIcon, aRGBa, aAllowAlpha, aUseConstantBrightness, aEnableAO, aRenderer, aBlock, aX, aY, aZ, aBrightness, aChangedBlockBounds);
			case SIDE_X_NEG: return renderXNeg(aIcon, aRGBa, aAllowAlpha, aUseConstantBrightness, aEnableAO, aRenderer, aBlock, aX, aY, aZ, aBrightness, aChangedBlockBounds);
			case SIDE_X_POS: return renderXPos(aIcon, aRGBa, aAllowAlpha, aUseConstantBrightness, aEnableAO, aRenderer, aBlock, aX, aY, aZ, aBrightness, aChangedBlockBounds);
			default: return F;
			}
		}

		/** Side = 5. PORT-TODO(F3, baked-рендер клиента): было doRenderXPos+prepareRenderXPos+applyAmbientOcclusionXPos. */
		public static boolean renderXPos(Identifier aIcon, short[] aRGBa, boolean aAllowAlpha, boolean aUseConstantBrightness, boolean aEnableAO, Object aRenderer, Block aBlock, int aX, int aY, int aZ, int aBrightness, boolean aChangedBlockBounds) {
			return aIcon != null;
		}
		/** Side = 4. PORT-TODO(F3, baked-рендер клиента): было doRenderXNeg+prepareRenderXNeg+applyAmbientOcclusionXNeg. */
		public static boolean renderXNeg(Identifier aIcon, short[] aRGBa, boolean aAllowAlpha, boolean aUseConstantBrightness, boolean aEnableAO, Object aRenderer, Block aBlock, int aX, int aY, int aZ, int aBrightness, boolean aChangedBlockBounds) {
			return aIcon != null;
		}
		/** Side = 1. PORT-TODO(F3, baked-рендер клиента): было doRenderYPos+prepareRenderYPos+applyAmbientOcclusionYPos. */
		public static boolean renderYPos(Identifier aIcon, short[] aRGBa, boolean aAllowAlpha, boolean aUseConstantBrightness, boolean aEnableAO, Object aRenderer, Block aBlock, int aX, int aY, int aZ, int aBrightness, boolean aChangedBlockBounds) {
			return aIcon != null;
		}
		/** Side = 0. PORT-TODO(F3, baked-рендер клиента): было doRenderYNeg (renderFixedNegativeYFacing)+prepareRenderYNeg+applyAmbientOcclusionYNeg. */
		public static boolean renderYNeg(Identifier aIcon, short[] aRGBa, boolean aAllowAlpha, boolean aUseConstantBrightness, boolean aEnableAO, Object aRenderer, Block aBlock, int aX, int aY, int aZ, int aBrightness, boolean aChangedBlockBounds) {
			return aIcon != null;
		}
		/** Side = 3. PORT-TODO(F3, baked-рендер клиента): было doRenderZPos+prepareRenderZPos+applyAmbientOcclusionZPos. */
		public static boolean renderZPos(Identifier aIcon, short[] aRGBa, boolean aAllowAlpha, boolean aUseConstantBrightness, boolean aEnableAO, Object aRenderer, Block aBlock, int aX, int aY, int aZ, int aBrightness, boolean aChangedBlockBounds) {
			return aIcon != null;
		}
		/** Side = 2. PORT-TODO(F3, baked-рендер клиента): было doRenderZNeg+prepareRenderZNeg+applyAmbientOcclusionZNeg. */
		public static boolean renderZNeg(Identifier aIcon, short[] aRGBa, boolean aAllowAlpha, boolean aUseConstantBrightness, boolean aEnableAO, Object aRenderer, Block aBlock, int aX, int aY, int aZ, int aBrightness, boolean aChangedBlockBounds) {
			return aIcon != null;
		}
	}
}

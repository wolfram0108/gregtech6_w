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
 *
 * Modified in 2026 for the GregTech 6 NeoForge port
 * (https://github.com/wolfram0108/gregtech6_w): ported from Minecraft 1.7.10 / Forge
 * to Minecraft 26.1.2 / NeoForge.
 */

package gregapi.render;

import static gregapi.data.CS.*;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.resources.ResourceLocation;

/**
 * @author Gregorius Techneticies
 *
 * F3-render: в 1.7.10 этот интерфейс дёргал по каждой стороне immediate-mode рендерер
 * (RenderBlocks/Tessellator/GL11/IIcon) — стек удалён в 26.1.2. РЕАЛИЗОВАНА замена (decisions/F3-render.md §8):
 * {@code aRenderer} = {@link GT6QuadBuilder}, per-side {@code renderXPos/...} → {@code Util.renderSide} →
 * {@code putFace(side, ResourceLocation, RGBa)} строит BakedQuad; сборка в {@link GT6BlockModel} (BakedModel + ModelData).
 * {@code IIcon} → {@link ResourceLocation}. immediate-mode AO/Tessellator заменён декларативным baked-путём.
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
	 * F3-render: было Tessellator/GL11/RenderBlocks immediate-mode (per-side UV + AO, ~700 строк). РЕАЛИЗОВАНА замена —
	 * per-side {@code renderX/Y/Z} строят BakedQuad через {@link GT6QuadBuilder} (мост в {@code aRenderer}); сборка в
	 * {@link GT6BlockModel}. AO/яркость даёт neo baked-путь (лайтмап на рендере), поэтому alpha-blending setup/teardown не нужны.
	 */
	public static class Util {
		public static boolean OPTIFINE_LOADED = F, GT_ALPHA_BLENDING = F, MC_ALPHA_BLENDING = F, IS_RENDERING_ALPHA = F;

		/** F3-render: было GL11 alpha-blending Setup; в baked-пути не нужно (neo сам управляет blend по RenderType). No-op. */
		public static void startRendering(Object aRenderer, Block aBlock, BlockGetter aWorld, int aX, int aY, int aZ) {
			//
		}

		/** F3-render: было GL11 alpha-blending Teardown; в baked-пути не нужно. No-op. */
		public static void endRendering(Object aRenderer, Block aBlock, BlockGetter aWorld, int aX, int aY, int aZ) {
			//
		}

		//=============================================================================================================
		// F3-render: prepare+do+applyAmbientOcclusion (было ~700 строк Tessellator/GL11 по стороне) — заменено
		// декларативным baked-путём: per-side quad в GT6QuadBuilder, AO/яркость даёт neo на рендере.
		//=============================================================================================================

		public static boolean renderSide(byte aSide, ResourceLocation aIcon, short[] aRGBa, boolean aAllowAlpha, boolean aUseConstantBrightness, boolean aEnableAO, Object aRenderer, Block aBlock, int aX, int aY, int aZ, int aBrightness, boolean aChangedBlockBounds) {
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

		// F3-render: per-side мост immediate-mode → декларативный quad. aRenderer=GT6QuadBuilder аккумулирует full-cube грань
		// из (side, ResourceLocation, RGBa). Reused GT6 per-side texture-логика (BlockTextureDefault даёт icon+RGBa на сторону).
		/** Side = 5 (X_POS/EAST). */
		public static boolean renderXPos(ResourceLocation aIcon, short[] aRGBa, boolean aAllowAlpha, boolean aUseConstantBrightness, boolean aEnableAO, Object aRenderer, Block aBlock, int aX, int aY, int aZ, int aBrightness, boolean aChangedBlockBounds) {
			if (aRenderer instanceof GT6QuadBuilder tB) tB.putFace((byte)SIDE_X_POS, aIcon, aRGBa);
			return aIcon != null;
		}
		/** Side = 4 (X_NEG/WEST). */
		public static boolean renderXNeg(ResourceLocation aIcon, short[] aRGBa, boolean aAllowAlpha, boolean aUseConstantBrightness, boolean aEnableAO, Object aRenderer, Block aBlock, int aX, int aY, int aZ, int aBrightness, boolean aChangedBlockBounds) {
			if (aRenderer instanceof GT6QuadBuilder tB) tB.putFace((byte)SIDE_X_NEG, aIcon, aRGBa);
			return aIcon != null;
		}
		/** Side = 1 (Y_POS/UP). */
		public static boolean renderYPos(ResourceLocation aIcon, short[] aRGBa, boolean aAllowAlpha, boolean aUseConstantBrightness, boolean aEnableAO, Object aRenderer, Block aBlock, int aX, int aY, int aZ, int aBrightness, boolean aChangedBlockBounds) {
			if (aRenderer instanceof GT6QuadBuilder tB) tB.putFace((byte)SIDE_Y_POS, aIcon, aRGBa);
			return aIcon != null;
		}
		/** Side = 0 (Y_NEG/DOWN). */
		public static boolean renderYNeg(ResourceLocation aIcon, short[] aRGBa, boolean aAllowAlpha, boolean aUseConstantBrightness, boolean aEnableAO, Object aRenderer, Block aBlock, int aX, int aY, int aZ, int aBrightness, boolean aChangedBlockBounds) {
			if (aRenderer instanceof GT6QuadBuilder tB) tB.putFace((byte)SIDE_Y_NEG, aIcon, aRGBa);
			return aIcon != null;
		}
		/** Side = 3 (Z_POS/SOUTH). */
		public static boolean renderZPos(ResourceLocation aIcon, short[] aRGBa, boolean aAllowAlpha, boolean aUseConstantBrightness, boolean aEnableAO, Object aRenderer, Block aBlock, int aX, int aY, int aZ, int aBrightness, boolean aChangedBlockBounds) {
			if (aRenderer instanceof GT6QuadBuilder tB) tB.putFace((byte)SIDE_Z_POS, aIcon, aRGBa);
			return aIcon != null;
		}
		/** Side = 2 (Z_NEG/NORTH). */
		public static boolean renderZNeg(ResourceLocation aIcon, short[] aRGBa, boolean aAllowAlpha, boolean aUseConstantBrightness, boolean aEnableAO, Object aRenderer, Block aBlock, int aX, int aY, int aZ, int aBrightness, boolean aChangedBlockBounds) {
			if (aRenderer instanceof GT6QuadBuilder tB) tB.putFace((byte)SIDE_Z_NEG, aIcon, aRGBa);
			return aIcon != null;
		}
	}
}

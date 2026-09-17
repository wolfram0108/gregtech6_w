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

/** 1.7.10 called an immediate-mode renderer per side (RenderBlocks/Tessellator/GL11/IIcon); that stack is gone.
 *  aRenderer is now GT6QuadBuilder: renderXPos/... calls Util.renderSide -> putFace, building baked quads. */
public interface ITexture {
	public void renderXPos(Object aRenderer, Block aBlock, int aX, int aY, int aZ, int aBrightness, boolean aChangedBlockBounds);
	public void renderXNeg(Object aRenderer, Block aBlock, int aX, int aY, int aZ, int aBrightness, boolean aChangedBlockBounds);
	public void renderYPos(Object aRenderer, Block aBlock, int aX, int aY, int aZ, int aBrightness, boolean aChangedBlockBounds);
	public void renderYNeg(Object aRenderer, Block aBlock, int aX, int aY, int aZ, int aBrightness, boolean aChangedBlockBounds);
	public void renderZPos(Object aRenderer, Block aBlock, int aX, int aY, int aZ, int aBrightness, boolean aChangedBlockBounds);
	public void renderZNeg(Object aRenderer, Block aBlock, int aX, int aY, int aZ, int aBrightness, boolean aChangedBlockBounds);

	public boolean isValidTexture();

	/** Replaces ~700 lines of Tessellator/GL11 immediate-mode per-side rendering with BakedQuad building via
	 *  GT6QuadBuilder; neo's baked path already handles AO and brightness, so no manual blend setup is needed. */
	public static class Util {
		public static boolean OPTIFINE_LOADED = F, GT_ALPHA_BLENDING = F, MC_ALPHA_BLENDING = F, IS_RENDERING_ALPHA = F;

		/** Was GL11 alpha-blending setup; unneeded in the baked path since neo manages blending via RenderType. No-op. */
		public static void startRendering(Object aRenderer, Block aBlock, BlockGetter aWorld, int aX, int aY, int aZ) {
			//
		}

		/** Was GL11 alpha-blending teardown; unneeded for the same reason. No-op. */
		public static void endRendering(Object aRenderer, Block aBlock, BlockGetter aWorld, int aX, int aY, int aZ) {
			//
		}

		//=============================================================================================================
		// Replaces the old prepare+do+applyAmbientOcclusion Tessellator/GL11 sequence with a per-side quad
		// built via GT6QuadBuilder; AO and brightness now come from neo at render time.
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

		// Per-side bridge from immediate-mode calls to a declarative quad: aRenderer=GT6QuadBuilder accumulates one face.
		// Reuses GT6's per-side texture logic (BlockTextureDefault supplies icon+RGBa per side).
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

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
 *
 * Modified in 2026 for the GregTech 6 NeoForge port
 * (https://github.com/wolfram0108/gregtech6_w): ported from Minecraft 1.7.10 / Forge
 * to Minecraft 26.1.2 / NeoForge.
 */

package gregapi.render;

import gregapi.block.fluid.BlockBaseFluid;
import gregapi.block.fluid.BlockFluidBaseGT;
import gregapi.util.UT;
import gregapi.util.WD;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;

/** Same class and geometry as 1.7.10's immediate-mode renderWorldBlock, just 'draw now' turned into 'give me a
 *  quad'; RENDER_ID/INSTANCE are no-op compatibility shims read elsewhere as 'is there a renderer'. */
public class RendererBlockFluid {
	public static int RENDER_ID = 0;
	public static RendererBlockFluid INSTANCE;

	public RendererBlockFluid(int aRenderID) {
		INSTANCE = this;
		RENDER_ID = aRenderID;
	}

	/** 1:1 with the original's constants. */
	/** Must stay 0.875, not 0.8888889: the original 1.7.10 applies exactly this value in two roles at once (an
	 *  averaging threshold and a volume-fraction multiplier), confirmed against a live 1.7.10 measurement of fluid height. */
	public static final float MAX_FLUID_HEIGHT = 0.875F, RENDER_OFFSET = 0.0010000000474974513F;

	/** Was getFluidHeightAverage(float[]) in the original; body is 1:1. */
	public static float getFluidHeightAverage(float[] aFlow) {
		float total = 0, end = 0;
		int count = 0;
		for (int i = 0; i < aFlow.length; i++) {
			if (aFlow[i] >= MAX_FLUID_HEIGHT && end != 1F) end = aFlow[i];
			if (aFlow[i] >= 0) {
				total += aFlow[i];
				count++;
			}
		}
		if (end == 0) end = total / count;
		return end;
	}

	/** Was getFluidHeightForRender(...) in the original, body 1:1; the IFluidBlock check maps to
	 *  BlockFluidBaseGT for GT6 fluids, with vanilla water covered separately. */
	public static float getFluidHeightForRender(BlockGetter aWorld, int aX, int aY, int aZ, BlockFluidBaseGT aFluidBlock, Block aBlock) {
		if (aBlock == null) aBlock = WD.block(aWorld, aX, aY, aZ);
		if (aBlock == aFluidBlock) {
			Block tBlockAbove = WD.block(aWorld, aX, aY - aFluidBlock.dir(), aZ);
			if (WD.getMaterial(tBlockAbove).isLiquid() || tBlockAbove instanceof BlockFluidBaseGT) return 1;
			return UT.Code.bindF(aFluidBlock.getQuantaPercentage(aWorld, aX, aY, aZ)) * MAX_FLUID_HEIGHT;
		}
		return !WD.getMaterial(aBlock).isSolid() && WD.block(aWorld, aX, aY - aFluidBlock.dir(), aZ) == aFluidBlock ? 1 : UT.Code.bindF(aFluidBlock.getQuantaPercentage(aWorld, aX, aY, aZ)) * MAX_FLUID_HEIGHT;
	}

	/** Was renderWorldBlock in the original: same structure and vertices, emitting a quad instead of an immediate-mode vertex. */
	public static void collectFluidQuads(GT6QuadBuilder aQB, BlockGetter aWorld, int aX, int aY, int aZ, BlockBaseFluid aFluid) {
		if (!(aFluid.renderTexture() instanceof BlockTextureFluid tTex) || !tTex.isValidTexture()) return;
		ResourceLocation tIcon = tTex.icon();
		short[] tRGBa = tTex.mRGBa;
		int aDir = aFluid.dir();

		boolean renderTop = WD.block(aWorld, aX, aY - aDir, aZ) != aFluid;
		boolean renderBottom = aFluid.shouldSideBeRendered(aWorld, aX, aY + aDir, aZ, 0) && WD.block(aWorld, aX, aY + aDir, aZ) != aFluid;

		boolean[] renderSides = new boolean[] {
			aFluid.shouldSideBeRendered(aWorld, aX, aY, aZ - 1, 2),
			aFluid.shouldSideBeRendered(aWorld, aX, aY, aZ + 1, 3),
			aFluid.shouldSideBeRendered(aWorld, aX - 1, aY, aZ, 4),
			aFluid.shouldSideBeRendered(aWorld, aX + 1, aY, aZ, 5)
		};

		if (!renderTop && !renderBottom && !renderSides[0] && !renderSides[1] && !renderSides[2] && !renderSides[3]) return;

		float heightNW, heightSW, heightSE, heightNE;
		float flow11 = getFluidHeightForRender(aWorld, aX, aY, aZ, aFluid, aFluid);

		if (flow11 != 1) {
			float flow00 = getFluidHeightForRender(aWorld, aX - 1, aY, aZ - 1, aFluid, null);
			float flow01 = getFluidHeightForRender(aWorld, aX - 1, aY, aZ,     aFluid, null);
			float flow02 = getFluidHeightForRender(aWorld, aX - 1, aY, aZ + 1, aFluid, null);
			float flow10 = getFluidHeightForRender(aWorld, aX,     aY, aZ - 1, aFluid, null);
			float flow12 = getFluidHeightForRender(aWorld, aX,     aY, aZ + 1, aFluid, null);
			float flow20 = getFluidHeightForRender(aWorld, aX + 1, aY, aZ - 1, aFluid, null);
			float flow21 = getFluidHeightForRender(aWorld, aX + 1, aY, aZ,     aFluid, null);
			float flow22 = getFluidHeightForRender(aWorld, aX + 1, aY, aZ + 1, aFluid, null);

			heightNW = getFluidHeightAverage(new float[] {flow00, flow01, flow10, flow11});
			heightSW = getFluidHeightAverage(new float[] {flow01, flow02, flow12, flow11});
			heightSE = getFluidHeightAverage(new float[] {flow12, flow21, flow22, flow11});
			heightNE = getFluidHeightAverage(new float[] {flow10, flow20, flow21, flow11});
		} else {
			heightNW = flow11;
			heightSW = flow11;
			heightSE = flow11;
			heightNE = flow11;
		}

		// Top face (surface, or the lower boundary for gas mirrored from the ceiling): vertices and UV are 1:1 with the original.
		if (renderTop) {
			double flowDir = BlockFluidBaseGT.getFlowDirection(aWorld, aX, aY, aZ);

			float hNW = heightNW - RENDER_OFFSET, hSW = heightSW - RENDER_OFFSET, hSE = heightSE - RENDER_OFFSET, hNE = heightNE - RENDER_OFFSET;
			float u1, u2, u3, u4, v1, v2, v3, v4;

			if (flowDir < -999.0F) {
				u2 = 0; v2 = 0;
				u1 = u2; v1 = 16;
				u4 = 16; v4 = v1;
				u3 = u4; v3 = v2;
			} else {
				float xFlow = (float)Math.sin(flowDir) * 0.25F;
				float zFlow = (float)Math.cos(flowDir) * 0.25F;
				u2 = 8.0F + (-zFlow - xFlow) * 16.0F; v2 = 8.0F + (-zFlow + xFlow) * 16.0F;
				u1 = 8.0F + (-zFlow + xFlow) * 16.0F; v1 = 8.0F + (+zFlow + xFlow) * 16.0F;
				u4 = 8.0F + (+zFlow + xFlow) * 16.0F; v4 = 8.0F + (+zFlow - xFlow) * 16.0F;
				u3 = 8.0F + (+zFlow - xFlow) * 16.0F; v3 = 8.0F + (-zFlow - xFlow) * 16.0F;
			}

			if (aDir < 0) {
				aQB.fluidQuad(new float[][] {
					{0, hNW, 0, u2, v2}, {0, hSW, 1, u1, v1}, {1, hSE, 1, u4, v4}, {1, hNE, 0, u3, v3}
				}, Direction.UP, tIcon, tRGBa, true);
			} else {
				aQB.fluidQuad(new float[][] {
					{1, 1 - hNE, 0, u3, v3}, {1, 1 - hSE, 1, u4, v4}, {0, 1 - hSW, 1, u1, v1}, {0, 1 - hNW, 0, u2, v2}
				}, Direction.DOWN, tIcon, tRGBa, true);
			}
		}

		// Bottom face (the ceiling plane for gas): 1:1 with the original.
		if (renderBottom) {
			if (aDir < 0) {
				aQB.fluidQuad(new float[][] {
					{0, RENDER_OFFSET, 0, 0, 0}, {0, RENDER_OFFSET, 1, 0, 16}, {1, RENDER_OFFSET, 1, 16, 16}, {1, RENDER_OFFSET, 0, 16, 0}
				}, Direction.DOWN, tIcon, tRGBa, true);
			} else {
				aQB.fluidQuad(new float[][] {
					{0, 1 + RENDER_OFFSET, 0, 0, 0}, {0, 1 + RENDER_OFFSET, 1, 0, 16}, {1, 1 + RENDER_OFFSET, 1, 16, 16}, {1, 1 + RENDER_OFFSET, 0, 16, 0}
				}, Direction.UP, tIcon, tRGBa, true);
			}
		}

		// Side faces: 1:1 with the original, using a half-texture V coordinate scaled by fluid height for the flowing look.
		for (int side = 0; side < 4; ++side) {
			if (!renderSides[side]) continue;

			float ty1, ty2, tx1, tx2, tz1, tz2;
			if (side == 0) {
				ty1 = heightNW; ty2 = heightNE;
				tx1 = 0; tx2 = 1;
				tz1 = RENDER_OFFSET; tz2 = RENDER_OFFSET;
			} else if (side == 1) {
				ty1 = heightSE; ty2 = heightSW;
				tx1 = 1; tx2 = 0;
				tz1 = 1 - RENDER_OFFSET; tz2 = 1 - RENDER_OFFSET;
			} else if (side == 2) {
				ty1 = heightSW; ty2 = heightNW;
				tx1 = RENDER_OFFSET; tx2 = RENDER_OFFSET;
				tz1 = 1; tz2 = 0;
			} else {
				ty1 = heightNE; ty2 = heightSE;
				tx1 = 1 - RENDER_OFFSET; tx2 = 1 - RENDER_OFFSET;
				tz1 = 0; tz2 = 1;
			}

			float u1Flow = 0, u2Flow = 8;
			float v1Flow = (1.0F - ty1) * 16.0F * 0.5F;
			float v2Flow = (1.0F - ty2) * 16.0F * 0.5F;
			float v3Flow = 8;
			Direction tFace = side == 0 ? Direction.NORTH : side == 1 ? Direction.SOUTH : side == 2 ? Direction.WEST : Direction.EAST;

			if (aDir < 0) {
				aQB.fluidQuad(new float[][] {
					{tx1, ty1, tz1, u1Flow, v1Flow}, {tx2, ty2, tz2, u2Flow, v2Flow}, {tx2, 0, tz2, u2Flow, v3Flow}, {tx1, 0, tz1, u1Flow, v3Flow}
				}, tFace, tIcon, tRGBa, true);
			} else {
				aQB.fluidQuad(new float[][] {
					{tx1, 1, tz1, u1Flow, v3Flow}, {tx2, 1, tz2, u2Flow, v3Flow}, {tx2, 1 - ty2, tz2, u2Flow, v2Flow}, {tx1, 1 - ty1, tz1, u1Flow, v1Flow}
				}, tFace, tIcon, tRGBa, true);
			}
		}
	}
}

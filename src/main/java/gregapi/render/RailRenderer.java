/**
 * Copyright (c) 2026 wolfram0108
 *
 * Written in 2026 for the GregTech 6 NeoForge port
 * (https://github.com/wolfram0108/gregtech6_w). Not part of the original GregTech 6
 * by Gregorius Techneticies; distributed under the same licence as the work it extends.
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

import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.BlockAndTintGetter;

/** Reproduces vanilla RenderBlocks.renderBlockRail as a flat quad at y=1/16, since BlockBaseRail extends vanilla
 *  BaseRailBlock rather than IRenderedBlock and needs its own branch, with meta mapped to shape via a dedicated bridge. */
public final class RailRenderer {
	private RailRenderer() {}

	private static final float Y = 1.0F / 16.0F; // rail sits 1px above the block bottom (vanilla 0.0625).

	/** Collects a rail's quads at a position from its current meta. */
	public static void collectRailQuads(GT6QuadBuilder aQB, BlockAndTintGetter aLevel, int aX, int aY, int aZ, gregapi.block.misc.BlockBaseRail aRail) {
		int tMeta = gregapi.util.WD.meta(aLevel, aX, aY, aZ) & 0xFF;
		ResourceLocation tIcon = aRail.getIcon(0, tMeta); // primary/secondary (straight/turned or active) is picked by the block itself.
		if (tIcon == null) return;
		// Power/detector rails only encode direction (bits 0-2, no corners); a regular rail uses the full meta 0..9.
		int tShape = (aRail.mPowerRail || aRail.mDetectorRail) ? (tMeta & 7) : (tMeta & 15);
		boolean tCorner = tShape >= 6 && tShape <= 9;
		boolean tEW = (tShape == 1 || tShape == 2 || tShape == 3); // E-W track orientation.
		float[][] c = tCorner ? cornerQuad(tShape - 6) : flatQuad(tEW);
		if (tShape >= 2 && tShape <= 5) raise(c, tShape); // ascending (2-5): raise the two corners on the uphill side.
		// bothSides: a rail is visible from above and below, 1:1 with vanilla drawing both an up and a down face.
		aQB.fluidQuad(c, Direction.UP, tIcon, gregapi.data.CS.UNCOLOURED, true);
	}

	/** Flat horizontal rail quad; UV runs along Z for a north-south track or along X for east-west. */
	private static float[][] flatQuad(boolean aEW) {
		if (aEW) return new float[][]{{0,Y,0, 0,0},{1,Y,0, 0,16},{1,Y,1, 16,16},{0,Y,1, 16,0}};
		return new float[][]{{0,Y,0, 0,0},{1,Y,0, 16,0},{1,Y,1, 16,16},{0,Y,1, 0,16}};
	}

	/** The corner-icon UV rotation isn't a linear +90 cycle by shape; it's a table recovered from the original's actual
	 *  vertex layout (0,-1,+2,+1 for SE,SW,NW,NE), since a naive linear formula rotated two corners the wrong way. */
	private static final int[] CORNER_ROT = {0, 3, 2, 1}; // SE,SW,NW,NE: shift matches vanilla (0,-1,+2,+1) mod 4.

	/** Corner quad (turned icon): base position and UV rotated by {@link #CORNER_ROT}, 1:1 with vanilla's rail-corner rotation. */
	private static float[][] cornerQuad(int aRot) {
		float[][] uv  = {{0,0},{16,0},{16,16},{0,16}};
		float[][] pos = {{0,Y,0},{1,Y,0},{1,Y,1},{0,Y,1}};
		float[][] c = new float[4][5];
		int tRot = CORNER_ROT[aRot & 3];
		for (int i = 0; i < 4; i++) {
			c[i][0] = pos[i][0]; c[i][1] = pos[i][1]; c[i][2] = pos[i][2];
			float[] u = uv[(i + tRot) & 3];
			c[i][3] = u[0]; c[i][4] = u[1];
		}
		return c;
	}

	/** Ascending tilt: raises the two corners on the uphill side by +1 on Y, leaving the low edge at rail level. */
	private static void raise(float[][] c, int aShape) {
		int[] hi;
		switch (aShape) {
		case 2:  hi = new int[]{1,2}; break; // ascending east (x=1).
		case 3:  hi = new int[]{0,3}; break; // ascending west (x=0).
		case 4:  hi = new int[]{0,1}; break; // ascending north (z=0).
		default: hi = new int[]{2,3}; break; // 5, ascending south (z=1).
		}
		for (int i : hi) c[i][1] += 1.0F;
	}
}

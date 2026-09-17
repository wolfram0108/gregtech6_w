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
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.BlockGetter;

/** The old immediate-mode block/item render dispatcher is gone; its logic now lives in GT6BlockModel/GT6ItemModel.
 *  This class only keeps the server-visible surface (mRenderID/INSTANCE, checked in 11+ places) as a compat shim. */
public class RendererBlockTextured {
	public final int mRenderID;
	public static RendererBlockTextured INSTANCE;
	public static CompoundTag mUsedNBT = null;

	public RendererBlockTextured(int aRenderID) {
		INSTANCE = this;
		mRenderID = aRenderID;
	}

	/** Was immediate-mode Tesselator/GL11 drawing; the same logic is reproduced 1:1 in GT6BlockModel.collectParts. */
	public static boolean renderNegativeYFacing(BlockGetter aWorld, Object aRenderer, Block aBlock, int aX, int aY, int aZ, ITexture aIcon, boolean aFullBlock, boolean aShouldSideBeRendered, Object aRenderedBlockObject) {
		if (aIcon == null || !aIcon.isValidTexture()) return F;
		if (aWorld != null && aFullBlock && !aShouldSideBeRendered) return F;
		aIcon.renderYNeg(aRenderer, aBlock, aX, aY, aZ, 240, !aFullBlock);
		return T;
	}

	/** Was immediate-mode Tesselator/GL11 drawing; the same logic is reproduced 1:1 in GT6BlockModel.collectParts. */
	public static boolean renderPositiveYFacing(BlockGetter aWorld, Object aRenderer, Block aBlock, int aX, int aY, int aZ, ITexture aIcon, boolean aFullBlock, boolean aShouldSideBeRendered, Object aRenderedBlockObject) {
		if (aIcon == null || !aIcon.isValidTexture()) return F;
		if (aWorld != null && aFullBlock && !aShouldSideBeRendered) return F;
		aIcon.renderYPos(aRenderer, aBlock, aX, aY, aZ, 240, !aFullBlock);
		return T;
	}

	/** Was immediate-mode Tesselator/GL11 drawing; the same logic is reproduced 1:1 in GT6BlockModel.collectParts. */
	public static boolean renderNegativeZFacing(BlockGetter aWorld, Object aRenderer, Block aBlock, int aX, int aY, int aZ, ITexture aIcon, boolean aFullBlock, boolean aShouldSideBeRendered, Object aRenderedBlockObject) {
		if (aIcon == null || !aIcon.isValidTexture()) return F;
		if (aWorld != null && aFullBlock && !aShouldSideBeRendered) return F;
		aIcon.renderZNeg(aRenderer, aBlock, aX, aY, aZ, 240, !aFullBlock);
		return T;
	}

	/** Was immediate-mode Tesselator/GL11 drawing; the same logic is reproduced 1:1 in GT6BlockModel.collectParts. */
	public static boolean renderPositiveZFacing(BlockGetter aWorld, Object aRenderer, Block aBlock, int aX, int aY, int aZ, ITexture aIcon, boolean aFullBlock, boolean aShouldSideBeRendered, Object aRenderedBlockObject) {
		if (aIcon == null || !aIcon.isValidTexture()) return F;
		if (aWorld != null && aFullBlock && !aShouldSideBeRendered) return F;
		aIcon.renderZPos(aRenderer, aBlock, aX, aY, aZ, 240, !aFullBlock);
		return T;
	}

	/** Was immediate-mode Tesselator/GL11 drawing; the same logic is reproduced 1:1 in GT6BlockModel.collectParts. */
	public static boolean renderNegativeXFacing(BlockGetter aWorld, Object aRenderer, Block aBlock, int aX, int aY, int aZ, ITexture aIcon, boolean aFullBlock, boolean aShouldSideBeRendered, Object aRenderedBlockObject) {
		if (aIcon == null || !aIcon.isValidTexture()) return F;
		if (aWorld != null && aFullBlock && !aShouldSideBeRendered) return F;
		aIcon.renderXNeg(aRenderer, aBlock, aX, aY, aZ, 240, !aFullBlock);
		return T;
	}

	/** Was immediate-mode Tesselator/GL11 drawing; the same logic is reproduced 1:1 in GT6BlockModel.collectParts. */
	public static boolean renderPositiveXFacing(BlockGetter aWorld, Object aRenderer, Block aBlock, int aX, int aY, int aZ, ITexture aIcon, boolean aFullBlock, boolean aShouldSideBeRendered, Object aRenderedBlockObject) {
		if (aIcon == null || !aIcon.isValidTexture()) return F;
		if (aWorld != null && aFullBlock && !aShouldSideBeRendered) return F;
		aIcon.renderXPos(aRenderer, aBlock, aX, aY, aZ, 240, !aFullBlock);
		return T;
	}
}

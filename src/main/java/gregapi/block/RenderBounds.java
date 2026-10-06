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

package gregapi.block;

/** Render bounds of one GT6 block. 1.7.10 mutated the shared Block from its single render thread; neo meshes chunks on several,
 *  so inside a render pass (CTX) each thread keeps its own copy and only outside it the shared one moves. */
public final class RenderBounds {
	/** Set by the model for the length of a render pass on this thread. */
	public static final ThreadLocal<boolean[]> CTX = ThreadLocal.withInitial(() -> new boolean[1]);
	private volatile float[] mShared = {0, 0, 0, 1, 1, 1};
	private final ThreadLocal<float[]> mLocal = ThreadLocal.withInitial(() -> mShared.clone());

	public void set(float aMinX, float aMinY, float aMinZ, float aMaxX, float aMaxY, float aMaxZ) {
		float[] tBounds = {aMinX, aMinY, aMinZ, aMaxX, aMaxY, aMaxZ};
		mLocal.set(tBounds);
		if (!CTX.get()[0]) mShared = tBounds;
	}
	/** The bounds the current render pass of this thread set. */
	public float[] render() {return mLocal.get();}
	/** The bounds set outside rendering, which the collision and outline bridges read. */
	public float[] shared() {return mShared;}
}

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

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.geometry.QuadCollection;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.model.pipeline.QuadBakingVertexConsumer;

/** Accumulates per-side BakedQuads for the declarative {@link GT6BlockModel} in place of 1.7.10's immediate-mode
 *  Tessellator drawing, so GT6's existing per-side texture logic is reused 1:1 and only the drawing mechanism changes. */
public final class GT6QuadBuilder {
	private final QuadCollection.Builder mQuads = new QuadCollection.Builder();
	private final List<BakedQuad> mAll = new ArrayList<>();
	/** Current render bounds, updated per pass (1.7.10's RenderBlocks.setRenderBoundsFromBlock). */
	private final float[] mBounds = {0, 0, 0, 1, 1, 1};
	/** Accumulates the bounds of actually-emitted faces, since GT6 boxes are computed at runtime rather than declared
	 *  as constants; this is the only place that knows how much geometry a given MTE really occupies. */
	private final float[] mDrawn = {0, 0, 0, 1, 1, 1};
	private boolean mDrawnAny = false;
	/** Per-face UV rotation matching 1.7.10's uvRotate{Bottom,Top,...} fields; only variant 1 is implemented,
	 *  since that's the only one vanilla's renderBlockLog (PILLAR blocks) actually uses. */
	private final byte[] mUVRotate = new byte[6];

	/** Sets per-face UV rotation (order DOWN,UP,NORTH,SOUTH,WEST,EAST; 0=none, 1=variant 1 of renderBlockLog). */
	public void setUVRotate(int aDown, int aUp, int aNorth, int aSouth, int aWest, int aEast) {
		mUVRotate[0] = (byte)aDown; mUVRotate[1] = (byte)aUp; mUVRotate[2] = (byte)aNorth;
		mUVRotate[3] = (byte)aSouth; mUVRotate[4] = (byte)aWest; mUVRotate[5] = (byte)aEast;
	}
	/** Resets rotations, matching 1.7.10's renderBlockLog clearing uvRotate* after renderStandardBlock. */
	public void clearUVRotate() {java.util.Arrays.fill(mUVRotate, (byte)0);}

	/** Updates the current render bounds before a pass, since GT6BlockModel reads them right after setBlockBounds. */
	public void setBounds(float[] aBounds) {
		if (aBounds == null || aBounds.length < 6) {System.arraycopy(new float[]{0,0,0,1,1,1}, 0, mBounds, 0, 6);}
		else System.arraycopy(aBounds, 0, mBounds, 0, 6);
	}

	/** A face gets asked about neighbor culling only if it lies exactly on the cell-boundary plane; using the whole
	 *  shape's cube-ness instead left non-cubic shapes (slabs) with unwanted seams on their always-visible internal faces. */
	private boolean atCellBoundary(Direction aDir) {
		switch (aDir) {
		case DOWN : return mBounds[1] <= 0;
		case UP   : return mBounds[4] >= 1;
		case NORTH: return mBounds[2] <= 0;
		case SOUTH: return mBounds[5] >= 1;
		case WEST : return mBounds[0] <= 0;
		default   : return mBounds[3] >= 1; // EAST
		}
	}

	/** GT6 side-byte to neo Direction mapping: 0=DOWN, 1=UP, 2=NORTH, 3=SOUTH, 4=WEST, 5=EAST. */
	public void putFace(byte aSide, Identifier aIcon, short[] aRGBa) {putFace(aSide, aIcon, aRGBa, false, 0, true);}

	/** Vertex color {r, g, b, a}: alpha only where the texture allows it, as on 1.7.10's flat path (its AO path was always opaque);
	 *  an allowed zero still reads as opaque, since GT6 colors are often RGB-ints with an empty alpha byte. */
	static int[] rgba(short[] aRGBa, boolean aAllowAlpha) {
		boolean tRGB = aRGBa != null && aRGBa.length >= 3;
		return new int[] {tRGB ? aRGBa[0] & 0xFF : 255, tRGB ? aRGBa[1] & 0xFF : 255, tRGB ? aRGBa[2] & 0xFF : 255, aAllowAlpha && aRGBa != null && aRGBa.length >= 4 && (aRGBa[3] & 0xFF) != 0 ? aRGBa[3] & 0xFF : 255};
	}

	/** aAllowAlpha lets the color's alpha reach the vertex as 1.7.10's flag of that name did; aEmission is the face's own
	 *  block light (1.7.10 drew such faces at a fixed lightmap value), aAO=false takes the face out of ambient occlusion. */
	public void putFace(byte aSide, Identifier aIcon, short[] aRGBa, boolean aAllowAlpha, int aEmission, boolean aAO) {
		if (aIcon == null || aSide < 0 || aSide > 5) return;
		TextureAtlasSprite tSprite = sprite(aIcon);
		if (tSprite == null) return;
		Direction tDir = Direction.from3DDataValue(aSide);
		BakedQuad tQuad = boundedFace(tDir, tSprite, aRGBa, aAllowAlpha, aEmission, aAO);
		if (tQuad == null) return;
		// A face on the cell-boundary plane is cull-aware (engine asks the neighbor); a face inside the cell
		// (slab top, pipe side) is always visible.
		if (atCellBoundary(tDir)) mQuads.addCulledFace(tDir, tQuad); else mQuads.addUnculledFace(tQuad);
		mAll.add(tQuad);
		// Bounds accumulate only from really-emitted faces, not every declared box, so the culling frame matches
		// what's actually drawn, instead of being inflated by a pass with a missing texture.
		if (mDrawnAny) {
			for (int i = 0; i < 3; i++) if (mBounds[i] < mDrawn[i]) mDrawn[i] = mBounds[i];
			for (int i = 3; i < 6; i++) if (mBounds[i] > mDrawn[i]) mDrawn[i] = mBounds[i];
		} else {
			System.arraycopy(mBounds, 0, mDrawn, 0, 6);
			mDrawnAny = true;
		}
	}

	/** Sum of emitted-face bounds in local block coordinates, or null if no face was ever emitted. */
	public float[] drawnBounds() {return mDrawnAny ? mDrawn : null;}

	public QuadCollection build() {return mQuads.build();}
	public List<BakedQuad> quads() {return mAll;}
	public boolean isEmpty() {return mAll.isEmpty();}

	private static TextureAtlasSprite sprite(Identifier aIcon) {return resolveSprite(aIcon);}

	/** Sprite resolve from the block atlas, the default used by putFace/resolveBlockFaceIcon for block faces. */
	public static TextureAtlasSprite resolveSprite(Identifier aIcon) {return resolveSprite(aIcon, net.minecraft.data.AtlasIds.BLOCKS);}

	/** Resolves a sprite from a chosen atlas, since GT6 textures are dynamic: block faces live in BLOCKS
	 *  while item icons (material items) live in ITEMS. */
	public static TextureAtlasSprite resolveSprite(Identifier aIcon, Identifier aAtlas) {
		try {
			net.minecraft.client.renderer.texture.TextureAtlas tAtlas = Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(aAtlas);
			TextureAtlasSprite tSprite = tAtlas.getSprite(aIcon);
			// getSprite returns the MISSING sprite, not null, on failure; converting it to null lets the ITEMS->BLOCKS
			// fallback and face-skipping work, instead of drawing a purple quad.
			return tSprite == tAtlas.missingSprite() ? null : tSprite;
		} catch (Throwable e) {return null;}
	}

	/** Resolves a vanilla block's face sprite from its baked BlockStateModel, replacing the removed 1.7.10
	 *  Block.getIcon(side,meta), as the sole centralized point for 'copy another block's texture'. */
	public static Identifier resolveBlockFaceIcon(net.minecraft.world.level.block.Block aBlock, int aSide) {
		return resolveBlockFaceIcon(aBlock, aSide, 0);
	}

	/** meta-aware version, 1:1 with 1.7.10's Block.getIcon(side,meta): since Flattening 1.13 turned meta variants
	 *  into separate neo blocks, it maps (base block, meta) to the variant block instead of reading a variant meta. */
	public static Identifier resolveBlockFaceIcon(net.minecraft.world.level.block.Block aBlock, int aSide, int aMeta) {
		// A GT6 block's dynamic model gives no quads without level/pos, so the baked path below would fall to the
		// error sprite; asking IBlock#getIcon directly first (by contract, not hierarchy) resolves the real icon instead.
		if (aBlock instanceof gregapi.block.IBlock tGT6) {
			Identifier tIcon = tGT6.getIcon(aSide, aMeta);
			if (tIcon != null) return tIcon;
		}
		net.minecraft.world.level.block.Block tVariant = flattenVariant(aBlock, aMeta);
		net.minecraft.world.level.block.state.BlockState tState = (tVariant != null ? tVariant : aBlock).defaultBlockState();
		net.minecraft.client.renderer.block.BlockStateModelSet tSet = Minecraft.getInstance().getModelManager().getBlockStateModelSet();
		if (aSide >= 0 && aSide <= 5) {
			Direction tDir = Direction.from3DDataValue(aSide);
			List<net.minecraft.client.renderer.block.dispatch.BlockStateModelPart> tParts = new ArrayList<>();
			tSet.get(tState).collectParts(net.minecraft.util.RandomSource.create(42L), tParts);
			for (net.minecraft.client.renderer.block.dispatch.BlockStateModelPart tPart : tParts) {
				List<BakedQuad> tQuads = tPart.getQuads(tDir);
				if (tQuads != null && !tQuads.isEmpty()) return tQuads.get(0).materialInfo().sprite().contents().name();
			}
		}
		return tSet.getParticleMaterial(tState).sprite().contents().name();
	}

	/** Flattening 1.13 lookup: returns the variant block for (base, meta), or null for meta 0 or a non-variant
	 *  block, using only documented vanilla tables. */
	private static net.minecraft.world.level.block.Block flattenVariant(net.minecraft.world.level.block.Block aBase, int aMeta) {
		if (aMeta == 0) return null;
		net.minecraft.world.level.block.Block B = aBase;
		net.minecraft.world.level.block.Block[] Bk = null;
		if (B == net.minecraft.world.level.block.Blocks.STONE_BRICKS) Bk = new net.minecraft.world.level.block.Block[]{
			net.minecraft.world.level.block.Blocks.STONE_BRICKS, net.minecraft.world.level.block.Blocks.MOSSY_STONE_BRICKS,
			net.minecraft.world.level.block.Blocks.CRACKED_STONE_BRICKS, net.minecraft.world.level.block.Blocks.CHISELED_STONE_BRICKS};
		else if (B == net.minecraft.world.level.block.Blocks.DIRT) Bk = new net.minecraft.world.level.block.Block[]{
			net.minecraft.world.level.block.Blocks.DIRT, net.minecraft.world.level.block.Blocks.COARSE_DIRT, net.minecraft.world.level.block.Blocks.PODZOL};
		else if (B == net.minecraft.world.level.block.Blocks.SAND) Bk = new net.minecraft.world.level.block.Block[]{
			net.minecraft.world.level.block.Blocks.SAND, net.minecraft.world.level.block.Blocks.RED_SAND};
		else if (B == net.minecraft.world.level.block.Blocks.SANDSTONE) Bk = new net.minecraft.world.level.block.Block[]{ // 1.7.10 sandstone: 0 normal,1 chiseled,2 smooth
			net.minecraft.world.level.block.Blocks.SANDSTONE, net.minecraft.world.level.block.Blocks.CHISELED_SANDSTONE, net.minecraft.world.level.block.Blocks.SMOOTH_SANDSTONE};
		if (Bk != null && aMeta > 0 && aMeta < Bk.length) return Bk[aMeta];
		return null;
	}

	// Renderers lay out AO and lightmap by vertex NUMBER, taking the number as canonical (FaceInfo.java:14-48);
	// any other order rotates the shading map on the face while geometry, UV and winding stay correct.
	static final int[] EMIT_ORDER = {1, 0, 3, 2};

	/** Face from the current bounds with UV clipped to them and tint from RGBa, following AE2's QuartzGlassModel pattern. */
	private BakedQuad boundedFace(Direction aDir, TextureAtlasSprite aSprite, short[] aRGBa, boolean aAllowAlpha, int aEmission, boolean aAO) {
		int[] tColor = rgba(aRGBa, aAllowAlpha);
		float[][] c = corners(aDir, mBounds);
		if (mUVRotate[aDir.get3DDataValue()] == 1) rotateUV1(aDir, c, mBounds);
		net.minecraft.world.phys.Vec3 n = aDir.getUnitVec3();
		QuadBakingVertexConsumer tBuilder = new QuadBakingVertexConsumer();
		tBuilder.setSprite(new Material.Baked(aSprite, false));
		tBuilder.setDirection(aDir);
		if (aEmission > 0) tBuilder.setLightEmission(aEmission);
		if (!aAO) tBuilder.setAmbientOcclusion(false);
		// EMIT_ORDER is a cyclic shift of the plain reverse, so the outward winding that back-face culling
		// needs is kept while the vertex numbering becomes the canonical one.
		for (int idx = 0; idx < 4; idx++) {
			final int i = EMIT_ORDER[idx];
			tBuilder.addVertex(c[i][0], c[i][1], c[i][2]);
			tBuilder.setColor(tColor[0], tColor[1], tColor[2], tColor[3]);
			tBuilder.setNormal((float)n.x, (float)n.y, (float)n.z);
			// corners() gives UV in the 0..16 block-texture convention, but neo's getU/getV expect a 0..1 offset; without
			// dividing by 16 the sample lands 16x outside the sprite, in the atlas's transparent gaps.
			tBuilder.setUv(aSprite.getU(c[i][3] / 16f), aSprite.getV(c[i][4] / 16f));
		}
		return tBuilder.bakeQuad();
	}

	/** Fluid quad with arbitrary vertices for sloped surfaces, always unculled since visibility is decided by
	 *  RendererBlockFluid's own shouldSideBeRendered logic; aBothSides duplicates winding since 1.7.10 had no backface cull. */
	public void fluidQuad(float[][] aCorners, Direction aDir, Identifier aIcon, short[] aRGBa, boolean aBothSides) {
		if (aIcon == null || aCorners == null || aCorners.length < 4) return;
		TextureAtlasSprite tSprite = sprite(aIcon);
		if (tSprite == null) return;
		BakedQuad tQuad = vertexQuad(aCorners, tSprite, aRGBa, aDir, false);
		if (tQuad != null) {mQuads.addUnculledFace(tQuad); mAll.add(tQuad);}
		if (aBothSides) {
			BakedQuad tBack = vertexQuad(aCorners, tSprite, aRGBa, aDir.getOpposite(), true);
			if (tBack != null) {mQuads.addUnculledFace(tBack); mAll.add(tBack);}
		}
	}
	/** Computes the FaceInfo vertex canon from the geometry itself, since arbitrary sloped-surface vertices can't use
	 *  the static EMIT_ORDER table; each vertex is classified low/high on the face's two planar axes and placed accordingly.
	 *  @return the emission order; degenerate geometry (coincident corners) returns the caller's own order unchanged. */
	static int[] canonicalOrder(Direction aDir, float[][] aCorners) {
		final int tNormal = aDir.getAxis().ordinal();          // 0=X, 1=Y, 2=Z
		final int a1 = tNormal == 0 ? 1 : 0;                   // first planar axis of the face
		final int a2 = tNormal == 2 ? 1 : 2;                   // second planar axis of the face
		// A side face of a fluid must lead on its horizontal axis: the vertical one carries the slope,
		// where "above the middle" tells nothing about which corner is which.
		boolean[][] tCls = classify(aCorners, a1, a2);
		boolean tSwapped = false;
		if (tCls == null) {tCls = classify(aCorners, a2, a1); tSwapped = true;}
		if (tCls == null) return new int[]{0, 1, 2, 3};        // degenerate geometry: caller's order
		final boolean[] tHiLead = tCls[0], tHiRank = tCls[1];
		final boolean[][] tCanon = canonPattern(aDir, tSwapped ? a2 : a1, tSwapped ? a1 : a2);
		int[] rOrder = new int[4];
		for (int c = 0; c < 4; c++) {
			int tFound = -1;
			for (int i = 0; i < 4; i++) if (tHiLead[i] == tCanon[c][0] && tHiRank[i] == tCanon[c][1]) {tFound = i; break;}
			if (tFound < 0) return new int[]{0, 1, 2, 3};
			rOrder[c] = tFound;
		}
		return rOrder;
	}

	/** Classifies vertices low/high on two axes: the lead axis splits at the spread midpoint, the second
	 *  axis splits within each lead-axis pair; null means the lead axis can't split evenly. */
	private static boolean[][] classify(float[][] aCorners, int aLead, int aRank) {
		float tMin = Float.MAX_VALUE, tMax = -Float.MAX_VALUE;
		for (float[] tV : aCorners) {tMin = Math.min(tMin, tV[aLead]); tMax = Math.max(tMax, tV[aLead]);}
		if (tMax - tMin < 1e-6F) return null;
		final float tMid = (tMin + tMax) * 0.5F;
		boolean[] tHiLead = new boolean[4];
		int tCountHi = 0;
		for (int i = 0; i < 4; i++) {tHiLead[i] = aCorners[i][aLead] > tMid; if (tHiLead[i]) tCountHi++;}
		if (tCountHi != 2) return null;
		boolean[] tHiRank = new boolean[4];
		for (int s = 0; s < 2; s++) {
			boolean tSide = s == 1;
			int p = -1, q = -1;
			for (int i = 0; i < 4; i++) if (tHiLead[i] == tSide) {if (p < 0) p = i; else q = i;}
			if (p < 0 || q < 0) return null;
			if (Math.abs(aCorners[p][aRank] - aCorners[q][aRank]) < 1e-6F) return null; // pair indistinguishable on the second axis
			tHiRank[aCorners[p][aRank] > aCorners[q][aRank] ? p : q] = true;
		}
		return new boolean[][]{tHiLead, tHiRank};
	}

	/** FaceInfo's vanilla vertex canon rewritten as low/high on each face's two planar axes; the normal
	 *  axis is constant per direction and so omitted. */
	private static boolean[][] canonPattern(Direction aDir, int a1, int a2) {
		final boolean[][] rXYZ = new boolean[4][3];             // [vertex][axis] = 'high'
		final boolean n = false, x = true;
		switch (aDir) {
		case DOWN:  set(rXYZ, n,n,x,  n,n,n,  x,n,n,  x,n,x); break;
		case UP:    set(rXYZ, n,x,n,  n,x,x,  x,x,x,  x,x,n); break;
		case NORTH: set(rXYZ, x,x,n,  x,n,n,  n,n,n,  n,x,n); break;
		case SOUTH: set(rXYZ, n,x,x,  n,n,x,  x,n,x,  x,x,x); break;
		case WEST:  set(rXYZ, n,x,n,  n,n,n,  n,n,x,  n,x,x); break;
		default:    set(rXYZ, x,x,x,  x,n,x,  x,n,n,  x,x,n); break; // EAST
		}
		boolean[][] r = new boolean[4][2];
		for (int i = 0; i < 4; i++) {r[i][0] = rXYZ[i][a1]; r[i][1] = rXYZ[i][a2];}
		return r;
	}
	private static void set(boolean[][] aTable, boolean... aBits) {
		for (int i = 0; i < 12; i++) aTable[i / 3][i % 3] = aBits[i];
	}

	/** One quad from 4 vertices with tint; aReverse tells the caller this is the back side. */
	private BakedQuad vertexQuad(float[][] aCorners, TextureAtlasSprite aSprite, short[] aRGBa, Direction aDir, boolean aReverse) {
		int[] tColor = rgba(aRGBa, false); // RendererBlockFluid drew with setColorOpaque_F
		net.minecraft.world.phys.Vec3 n = aDir.getUnitVec3();
		QuadBakingVertexConsumer tBuilder = new QuadBakingVertexConsumer();
		tBuilder.setSprite(new Material.Baked(aSprite, false));
		tBuilder.setDirection(aDir);
		// The canonical order of the OPPOSITE face already walks the other way round, so the back side of a
		// two-sided surface gets its reversed winding from aDir alone.
		int[] tOrder = canonicalOrder(aDir, aCorners);
		for (int idx = 0; idx < 4; idx++) {
			int i = tOrder[idx];
			tBuilder.addVertex(aCorners[i][0], aCorners[i][1], aCorners[i][2]);
			tBuilder.setColor(tColor[0], tColor[1], tColor[2], tColor[3]);
			tBuilder.setNormal((float)n.x, (float)n.y, (float)n.z);
			tBuilder.setUv(aSprite.getU(aCorners[i][3] / 16f), aSprite.getV(aCorners[i][4] / 16f));
		}
		return tBuilder.bakeQuad();
	}

	/** Cross-model plants/flowers: an X-shape from two diagonal planes, each two-sided and unculled, with
	 *  full UV, like vanilla's block/cross model. */
	public void crossFace(Identifier aIcon, short[] aRGBa) {
		if (aIcon == null) return;
		TextureAtlasSprite tSprite = sprite(aIcon);
		if (tSprite == null) return;
		// Cross planes go straight into the always-visible bucket, so they never need the boundary-plane cull check.
		addCrossPlane(new float[][]{{0,0,0},{1,0,1},{1,1,1},{0,1,0}}, tSprite, aRGBa); // diagonal SW->NE
		addCrossPlane(new float[][]{{1,0,0},{0,0,1},{0,1,1},{1,1,0}}, tSprite, aRGBa); // diagonal SE->NW
	}
	private void addCrossPlane(float[][] aCorners, TextureAtlasSprite aSprite, short[] aRGBa) {
		for (boolean tReverse : new boolean[]{false, true}) { // front + back = plane visible from both sides
			BakedQuad tQuad = planeQuad(aCorners, aSprite, aRGBa, tReverse);
			if (tQuad != null) {mQuads.addUnculledFace(tQuad); mAll.add(tQuad);}
		}
	}
	/** One quad of an arbitrary plane with full UV and tint; aReverse gives it reverse winding for the back side. */
	private BakedQuad planeQuad(float[][] aCorners, TextureAtlasSprite aSprite, short[] aRGBa, boolean aReverse) {
		int[] tColor = rgba(aRGBa, false); // vanilla crossed squares drew with setColorOpaque_F
		float[][] tUV = {{0,16},{16,16},{16,0},{0,0}}; // bottom-left, bottom-right, top-right, top-left
		float nx = aCorners[1][2]-aCorners[0][2], nz = -(aCorners[1][0]-aCorners[0][0]); // plane normal in XZ (for lighting; cull is disabled)
		float nlen = (float)Math.sqrt(nx*nx+nz*nz); if (nlen > 0) {nx/=nlen; nz/=nlen;}
		if (aReverse) {nx = -nx; nz = -nz;}
		QuadBakingVertexConsumer tBuilder = new QuadBakingVertexConsumer();
		tBuilder.setSprite(new Material.Baked(aSprite, false));
		tBuilder.setDirection(Direction.UP);
		// 1:1 with vanilla's block/cross model: shade and AO are off, since 1.7.10 drew plants under flat, undirected light.
		tBuilder.setShade(false);
		tBuilder.setAmbientOcclusion(false);
		int[] tOrder = aReverse ? new int[]{3,2,1,0} : new int[]{0,1,2,3};
		for (int idx = 0; idx < 4; idx++) {
			int i = tOrder[idx];
			tBuilder.addVertex(aCorners[i][0], aCorners[i][1], aCorners[i][2]);
			tBuilder.setColor(tColor[0], tColor[1], tColor[2], tColor[3]);
			tBuilder.setNormal(nx, 0, nz);
			tBuilder.setUv(aSprite.getU(tUV[i][0] / 16f), aSprite.getV(tUV[i][1] / 16f));
		}
		return tBuilder.bakeQuad();
	}

	/** UV-rotation variant 1, verbatim from 1.7.10's RenderBlocks, reduced to per-vertex UV tables; note the field
	 *  names map to faces non-trivially (uvRotateEast->NORTH, West->SOUTH, North->WEST, South->EAST). */
	private static void rotateUV1(Direction aDir, float[][] c, float[] b) {
		float u0x = b[0]*16, u1x = b[3]*16, u0z = b[2]*16, u1z = b[5]*16;
		float v0y = (1-b[1])*16, v1y = (1-b[4])*16, ty0 = b[1]*16, ty1 = b[4]*16;
		switch (aDir) {
		case DOWN:  uv(c,0, 16-u0z, u0x); uv(c,1, 16-u1z, u0x); uv(c,2, 16-u1z, u1x); uv(c,3, 16-u0z, u1x); break;
		case UP:    uv(c,0, u1z, 16-u0x); uv(c,1, u0z, 16-u0x); uv(c,2, u0z, 16-u1x); uv(c,3, u1z, 16-u1x); break;
		case NORTH: uv(c,0, v1y, u1x);    uv(c,1, v0y, u1x);    uv(c,2, v0y, u0x);    uv(c,3, v1y, u0x);    break;
		case SOUTH: uv(c,0, ty1, 16-u0x); uv(c,1, ty0, 16-u0x); uv(c,2, ty0, 16-u1x); uv(c,3, ty1, 16-u1x); break;
		case WEST:  uv(c,0, ty1, 16-u0z); uv(c,1, ty0, 16-u0z); uv(c,2, ty0, 16-u1z); uv(c,3, ty1, 16-u1z); break;
		case EAST:  uv(c,0, v1y, u1z);    uv(c,1, v0y, u1z);    uv(c,2, v0y, u0z);    uv(c,3, v1y, u0z);    break;
		default: break;
		}
	}
	private static void uv(float[][] c, int i, float u, float v) {c[i][3] = u; c[i][4] = v;}

	/** 4 corners of a face from the block bounds, UV clipped to them; a full 0..1 cube reduces to the old fixed UV. */
	private static float[][] corners(Direction aDir, float[] b) {
		float x0 = b[0], y0 = b[1], z0 = b[2], x1 = b[3], y1 = b[4], z1 = b[5];
		// Bounds outside the 0..1 cube get full UV instead of interpolated, since interpolating past the cube would sample
		// neighboring atlas sprites; GT6 relies on this for turbine blades, crucible walls and other out-of-cube geometry.
		float tx0 = x0, tx1 = x1, ty0 = y0, ty1 = y1, tz0 = z0, tz1 = z1;
		if (x0 < 0 || x1 > 1) {tx0 = 0; tx1 = 1;}
		if (y0 < 0 || y1 > 1) {ty0 = 0; ty1 = 1;}
		if (z0 < 0 || z1 > 1) {tz0 = 0; tz1 = 1;}
		float u0x = tx0*16, u1x = tx1*16, u0z = tz0*16, u1z = tz1*16, v0y = (1-ty0)*16, v1y = (1-ty1)*16;
		switch (aDir) {
		case DOWN:  return new float[][]{{x0,y0,z0, u0x,u0z},{x0,y0,z1, u0x,u1z},{x1,y0,z1, u1x,u1z},{x1,y0,z0, u1x,u0z}};
		case UP:    return new float[][]{{x0,y1,z1, u0x,u1z},{x0,y1,z0, u0x,u0z},{x1,y1,z0, u1x,u0z},{x1,y1,z1, u1x,u1z}};
		case NORTH: return new float[][]{{x1,y0,z0, u0x,v0y},{x1,y1,z0, u0x,v1y},{x0,y1,z0, u1x,v1y},{x0,y0,z0, u1x,v0y}};
		case SOUTH: return new float[][]{{x0,y0,z1, u0x,v0y},{x0,y1,z1, u0x,v1y},{x1,y1,z1, u1x,v1y},{x1,y0,z1, u1x,v0y}};
		case WEST:  return new float[][]{{x0,y0,z0, u0z,v0y},{x0,y1,z0, u0z,v1y},{x0,y1,z1, u1z,v1y},{x0,y0,z1, u1z,v0y}};
		case EAST:  return new float[][]{{x1,y0,z1, u0z,v0y},{x1,y1,z1, u0z,v1y},{x1,y1,z0, u1z,v1y},{x1,y0,z0, u1z,v0y}};
		default:    return new float[][]{{x0,y0,z0, u0x,u0z},{x0,y0,z1, u0x,u1z},{x1,y0,z1, u1x,u1z},{x1,y0,z0, u1x,u0z}};
		}
	}
}

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
import java.util.Collections;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.model.pipeline.QuadBakingVertexConsumer;

/** The 'capturing renderer' fed into GT6's own per-side texture chain; instead of drawing immediately like the old
 *  Tessellator did, it accumulates BakedQuads for the declarative model, reusing GT6's per-side logic unchanged. */
public final class GT6QuadBuilder {
	/** Replaces the engine's own removed quad-collection type; holds exactly the two buckets getQuads itself needs
	 *  (culled faces versus always-visible ones), the shape the model's answer already takes, not a new abstraction. */
	public static final class QuadSet {
		private static final List<BakedQuad> EMPTY = Collections.emptyList();
		final List<BakedQuad>[] mCulled;
		final List<BakedQuad> mUnculled;
		@SuppressWarnings("unchecked")
		QuadSet(List<BakedQuad>[] aCulled, List<BakedQuad> aUnculled) {mCulled = aCulled; mUnculled = aUnculled;}
		/** A null side means always-visible faces; any real side means that side's own cull-aware faces. */
		public List<BakedQuad> getQuads(Direction aSide) {
			if (aSide == null) return mUnculled;
			List<BakedQuad> r = mCulled[aSide.get3DDataValue()];
			return r == null ? EMPTY : r;
		}
		public boolean isEmpty() {
			if (!mUnculled.isEmpty()) return false;
			for (List<BakedQuad> t : mCulled) if (t != null && !t.isEmpty()) return false;
			return true;
		}
	}

	@SuppressWarnings("unchecked")
	private final List<BakedQuad>[] mCulled = new List[6];
	private final List<BakedQuad> mUnculled = new ArrayList<>();
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

	/** Whether the engine asks a neighbor about a face is decided per FACE, by whether that face's own plane touches the
	 *  cell boundary -- not per whole shape as before, which silently broke same-material face-hiding on any non-cube shape. */
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
	public void putFace(byte aSide, ResourceLocation aIcon, short[] aRGBa) {
		if (aIcon == null || aSide < 0 || aSide > 5) return;
		TextureAtlasSprite tSprite = sprite(aIcon);
		if (tSprite == null) return;
		Direction tDir = Direction.from3DDataValue(aSide);
		BakedQuad tQuad = boundedFace(tDir, tSprite, aRGBa);
		if (tQuad == null) return;
		// A face on the cell-boundary plane is cull-aware (engine asks the neighbor); a face inside the cell
		// (slab top, pipe side) is always visible.
		if (atCellBoundary(tDir)) addCulled(tDir, tQuad); else mUnculled.add(tQuad);
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

	private void addCulled(Direction aDir, BakedQuad aQuad) {
		int i = aDir.get3DDataValue();
		if (mCulled[i] == null) mCulled[i] = new ArrayList<>();
		mCulled[i].add(aQuad);
	}

	public QuadSet build() {return new QuadSet(mCulled, mUnculled);}
	public List<BakedQuad> quads() {return mAll;}
	public boolean isEmpty() {return mAll.isEmpty();}

	private static TextureAtlasSprite sprite(ResourceLocation aIcon) {return resolveSprite(aIcon);}

	// On this engine version block icons and item icons share one atlas, with no separate ids for each; GT6's own
	// block/item split survives as a sprite-id prefix instead, or same-named icon sets would collide in one atlas.
	/** The atlas bucket for block textures, whose sprite id needs no prefix. */
	public static final int ATLAS_BLOCKS = 0;
	/** The atlas bucket for item icons, whose sprite id gets an items/ prefix to avoid colliding with block textures. */
	public static final int ATLAS_ITEMS = 1;

	/** Stands in for a real item-atlas address, which doesn't exist on this engine version at all -- item icons live in
	 *  the block atlas under a prefix instead, so this value is only a bucket marker, kept for contract completeness. */
	public static final ResourceLocation LOCATION_ITEMS = new ResourceLocation("minecraft", "textures/atlas/items.png");

	/** Sprite resolve from the block atlas, the default used by putFace/resolveBlockFaceIcon for block faces. */
	public static TextureAtlasSprite resolveSprite(ResourceLocation aIcon) {return resolveSprite(aIcon, ATLAS_BLOCKS);}

	/** Resolves a sprite from the given bucket: GT6's dynamic textures split between a block path and an item path,
	 *  and a material item specifically needs its item-version icon, or it renders as a missing-texture purple. */
	public static TextureAtlasSprite resolveSprite(ResourceLocation aIcon, int aAtlasBucket) {
		if (aIcon == null) return null;
		try {
			TextureAtlas tAtlas = Minecraft.getInstance().getModelManager().getAtlas(TextureAtlas.LOCATION_BLOCKS);
			if (tAtlas == null) return null;
			TextureAtlasSprite tSprite = tAtlas.getSprite(aAtlasBucket == ATLAS_ITEMS ? aIcon.withPrefix("items/") : aIcon);
			// getSprite returns the engine's own missing-texture sprite (not null) when absent; converting that to null here
			// lets a fallback or a skipped face take over, instead of drawing a purple quad.
			return tSprite == null || MissingTextureAtlasSprite.getLocation().equals(tSprite.contents().name()) ? null : tSprite;
		} catch (Throwable e) {return null;}
	}

	/** Replaces 1.7.10's removed Block.getIcon(side,meta) for a VANILLA block, reading its baked model's own face
	 *  sprite instead; the one point in the mod for 'copy another block's texture', used by two helper classes. */
	public static ResourceLocation resolveBlockFaceIcon(net.minecraft.world.level.block.Block aBlock, int aSide) {
		return resolveBlockFaceIcon(aBlock, aSide, 0);
	}

	/** meta-aware version, 1:1 with 1.7.10's Block.getIcon(side,meta): since Flattening 1.13 turned meta variants
	 *  into separate neo blocks, it maps (base block, meta) to the variant block instead of reading a variant meta. */
	public static ResourceLocation resolveBlockFaceIcon(net.minecraft.world.level.block.Block aBlock, int aSide, int aMeta) {
		// A GT6 block's dynamic model gives no quads without level/pos, so the baked path below would fall to the
		// error sprite; asking IBlock#getIcon directly first (by contract, not hierarchy) resolves the real icon instead.
		if (aBlock instanceof gregapi.block.IBlock tGT6) {
			ResourceLocation tIcon = tGT6.getIcon(aSide, aMeta);
			if (tIcon != null) return tIcon;
		}
		net.minecraft.world.level.block.Block tVariant = flattenVariant(aBlock, aMeta);
		net.minecraft.world.level.block.state.BlockState tState = (tVariant != null ? tVariant : aBlock).defaultBlockState();
		// The baked block model comes from BlockModelShaper; it hands back per-side quads and a particle sprite itself,
		// the same two channels a different engine branch reaches through its own separate types.
		net.minecraft.client.resources.model.BakedModel tModel = Minecraft.getInstance().getBlockRenderer().getBlockModel(tState);
		if (aSide >= 0 && aSide <= 5) {
			Direction tDir = Direction.from3DDataValue(aSide);
			List<BakedQuad> tQuads = tModel.getQuads(tState, tDir, net.minecraft.util.RandomSource.create(42L));
			if (tQuads != null && !tQuads.isEmpty()) return tQuads.get(0).getSprite().contents().name();
		}
		return tModel.getParticleIcon().contents().name();
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

	/** The corner order a cube face is emitted in, matching vanilla's own canon, since the AO corner layout depends on it. */
	static final int[] EMIT_ORDER = {1, 0, 3, 2};

	/** Face from the current bounds with UV clipped to them and tint from RGBa, following AE2's QuartzGlassModel pattern. */
	private BakedQuad boundedFace(Direction aDir, TextureAtlasSprite aSprite, short[] aRGBa) {
		int r = aRGBa != null && aRGBa.length >= 3 ? (aRGBa[0] & 0xFF) : 255;
		int g = aRGBa != null && aRGBa.length >= 3 ? (aRGBa[1] & 0xFF) : 255;
		int b = aRGBa != null && aRGBa.length >= 3 ? (aRGBa[2] & 0xFF) : 255;
		// 1.7.10's tint had no alpha channel, but GT6 colors are often RGB-int with a zero alpha byte, which neo reads
		// as fully transparent; zero is remapped to 255 to match the original opaque tint.
		int a = aRGBa != null && aRGBa.length >= 4 && (aRGBa[3] & 0xFF) != 0 ? (aRGBa[3] & 0xFF) : 255;
		float[][] c = corners(aDir, mBounds);
		if (mUVRotate[aDir.get3DDataValue()] == 1) rotateUV1(aDir, c, mBounds);
		org.joml.Vector3f n = aDir.step();
		QuadBakingVertexConsumer.Buffered tBuilder = new QuadBakingVertexConsumer.Buffered();
		tBuilder.setSprite(aSprite);
		tBuilder.setDirection(aDir);
		tBuilder.setTintIndex(-1);   // Tint is already baked into the vertices; leaving tintIndex at its default would make the engine multiply it in again.
		tBuilder.setShade(true);     // Block faces shade by direction here, 1:1 with 1.7.10's own per-face brightness multipliers.
		tBuilder.setHasAmbientOcclusion(true);
		// The engine derives a face's normal from vertex winding and places AO brightness by vertex NUMBER, treating that
		// number as canonical; this permutation keeps the fixed winding while landing each vertex on its correct AO slot.
		for (int idx = 0; idx < 4; idx++) {
			final int i = EMIT_ORDER[idx];
			tBuilder.vertex(c[i][0], c[i][1], c[i][2]);
			tBuilder.color(r, g, b, a);
			tBuilder.normal(n.x(), n.y(), n.z());
			// corners hands out UV in the 0..16 block-texture convention; 1.20.1's own getU/getV already divide by 16
			// themselves, so dividing again here (needed on a branch that expected 0..1 input) squeezed a face into a single texel.
			tBuilder.uv(aSprite.getU(c[i][3]), aSprite.getV(c[i][4]));
			tBuilder.endVertex();
		}
		return tBuilder.getQuad();
	}

	/** Fluid quad with arbitrary vertices for sloped surfaces, always unculled since visibility is decided by
	 *  RendererBlockFluid's own shouldSideBeRendered logic; aBothSides duplicates winding since 1.7.10 had no backface cull. */
	public void fluidQuad(float[][] aCorners, Direction aDir, ResourceLocation aIcon, short[] aRGBa, boolean aBothSides) {
		if (aIcon == null || aCorners == null || aCorners.length < 4) return;
		TextureAtlasSprite tSprite = sprite(aIcon);
		if (tSprite == null) return;
		BakedQuad tQuad = vertexQuad(aCorners, tSprite, aRGBa, aDir, false);
		if (tQuad != null) {mUnculled.add(tQuad); mAll.add(tQuad);}
		if (aBothSides) {
			BakedQuad tBack = vertexQuad(aCorners, tSprite, aRGBa, aDir.getOpposite(), true);
			if (tBack != null) {mUnculled.add(tBack); mAll.add(tBack);}
		}
	}
	/** The same vanilla vertex-order canon as EMIT_ORDER above, but computed from geometry instead of read from a table,
	 *  since an arbitrary quad's vertices (fluids) arrive in whatever order the caller built them, not a fixed cube-face row. */
	static int[] canonicalOrder(Direction aDir, float[][] aCorners) {
		final int tNormal = aDir.getAxis().ordinal();          // 0=X, 1=Y, 2=Z
		final int a1 = tNormal == 0 ? 1 : 0;                   // first planar axis of the face
		final int a2 = tNormal == 2 ? 1 : 2;                   // second planar axis of the face
		// The leading axis is whichever one splits vertices evenly; a fluid's side face must use the horizontal, since
		// its vertical axis carries different corner heights (a slope), where 'above the midpoint' would be wrong.
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

	/** Vanilla's own FaceInfo canon, rewritten as lesser/greater along a face's two in-plane axes; the constant normal
	 *  axis is omitted, since it never varies within one face. */
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

	/** One quad from 4 corners {x,y,z,u,v} (u,v in 0..16) plus tint; aReverse flips the winding order. */
	private BakedQuad vertexQuad(float[][] aCorners, TextureAtlasSprite aSprite, short[] aRGBa, Direction aDir, boolean aReverse) {
		int r = aRGBa != null && aRGBa.length >= 3 ? (aRGBa[0] & 0xFF) : 255;
		int g = aRGBa != null && aRGBa.length >= 3 ? (aRGBa[1] & 0xFF) : 255;
		int b = aRGBa != null && aRGBa.length >= 3 ? (aRGBa[2] & 0xFF) : 255;
		int a = aRGBa != null && aRGBa.length >= 4 && (aRGBa[3] & 0xFF) != 0 ? (aRGBa[3] & 0xFF) : 255;
		org.joml.Vector3f n = aDir.step();
		QuadBakingVertexConsumer.Buffered tBuilder = new QuadBakingVertexConsumer.Buffered();
		tBuilder.setSprite(aSprite);
		tBuilder.setDirection(aDir);
		tBuilder.setTintIndex(-1);
		tBuilder.setShade(true);
		tBuilder.setHasAmbientOcclusion(true);
		// Vertex order follows the vanilla canon (canonicalOrder), not caller order, or shading lands on the wrong face.
		// aReverse no longer picks the winding (the opposite face's canon does); fluidQuad passes it via aDir/getOpposite.
		int[] tOrder = canonicalOrder(aDir, aCorners);
		for (int idx = 0; idx < 4; idx++) {
			int i = tOrder[idx];
			tBuilder.vertex(aCorners[i][0], aCorners[i][1], aCorners[i][2]);
			tBuilder.color(r, g, b, a);
			tBuilder.normal(n.x(), n.y(), n.z());
			tBuilder.uv(aSprite.getU(aCorners[i][3]), aSprite.getV(aCorners[i][4])); // getU/getV expect UV in the 0..16 pixel range on 1.20.1, matching the note above.
			tBuilder.endVertex();
		}
		return tBuilder.getQuad();
	}

	/** Cross-model plants/flowers: an X-shape from two diagonal planes, each two-sided and unculled, with
	 *  full UV, like vanilla's block/cross model. */
	public void crossFace(ResourceLocation aIcon, short[] aRGBa) {
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
			if (tQuad != null) {mUnculled.add(tQuad); mAll.add(tQuad);}
		}
	}
	/** One quad of an arbitrary plane with full UV and tint; aReverse gives it reverse winding for the back side. */
	private BakedQuad planeQuad(float[][] aCorners, TextureAtlasSprite aSprite, short[] aRGBa, boolean aReverse) {
		int r = aRGBa != null && aRGBa.length >= 3 ? (aRGBa[0] & 0xFF) : 255;
		int g = aRGBa != null && aRGBa.length >= 3 ? (aRGBa[1] & 0xFF) : 255;
		int b = aRGBa != null && aRGBa.length >= 3 ? (aRGBa[2] & 0xFF) : 255;
		// 1.7.10's tint had no alpha channel, but GT6 colors are often RGB-int with a zero alpha byte, which neo reads
		// as fully transparent; zero is remapped to 255 to match the original opaque tint.
		int a = aRGBa != null && aRGBa.length >= 4 && (aRGBa[3] & 0xFF) != 0 ? (aRGBa[3] & 0xFF) : 255;
		float[][] tUV = {{0,16},{16,16},{16,0},{0,0}}; // bottom-left, bottom-right, top-right, top-left
		float nx = aCorners[1][2]-aCorners[0][2], nz = -(aCorners[1][0]-aCorners[0][0]); // plane normal in XZ (for lighting; cull is disabled)
		float nlen = (float)Math.sqrt(nx*nx+nz*nz); if (nlen > 0) {nx/=nlen; nz/=nlen;}
		if (aReverse) {nx = -nx; nz = -nz;}
		QuadBakingVertexConsumer.Buffered tBuilder = new QuadBakingVertexConsumer.Buffered();
		tBuilder.setSprite(aSprite);
		tBuilder.setDirection(Direction.UP);
		tBuilder.setTintIndex(-1);
		// 1:1 with vanilla's block/cross model: shade and AO are off, since 1.7.10 drew plants under flat, undirected light.
		tBuilder.setShade(false);
		tBuilder.setHasAmbientOcclusion(false);
		int[] tOrder = aReverse ? new int[]{3,2,1,0} : new int[]{0,1,2,3};
		for (int idx = 0; idx < 4; idx++) {
			int i = tOrder[idx];
			tBuilder.vertex(aCorners[i][0], aCorners[i][1], aCorners[i][2]);
			tBuilder.color(r, g, b, a);
			tBuilder.normal(nx, 0, nz);
			tBuilder.uv(aSprite.getU(tUV[i][0]), aSprite.getV(tUV[i][1])); // getU/getV expect UV in the 0..16 pixel range on 1.20.1, matching the note above.
			tBuilder.endVertex();
		}
		return tBuilder.getQuad();
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

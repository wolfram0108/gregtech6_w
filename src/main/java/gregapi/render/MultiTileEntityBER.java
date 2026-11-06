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

import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.QuadInstance;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;

import gregapi.block.multitileentity.MultiTileEntityBlock;
import gregapi.tileentity.base.TileEntityBase01Root;

/** Builds per-frame geometry only for the two MTE with a dedicated 1.7.10 renderer (chest, mass storage) and for
 *  the currently-breaking block's crack decals; every other MTE lives in the section mesh via collectParts. */
public class MultiTileEntityBER implements BlockEntityRenderer<TileEntityBase01Root, MultiTileEntityBER.MTERenderState> {

	public MultiTileEntityBER(BlockEntityRendererProvider.Context aContext) {/* per-BE geometry is built in extractRenderState; context resources aren't needed here */}

	// The render-distance override is removed: MTE geometry now lives in the section mesh and draws at full
	// distance, and the engine's 64-block default already matches 1.7.10's own TESR clipping radius.

	/** Neo clips a block entity's render by this box, defaulting to just its own cube, unlike 1.7.10 where MTE drew via
	 *  the section mesh; the box uses last frame's drawn geometry, skipping the clip for one frame while unknown. */
	@Override
	@SuppressWarnings({"rawtypes", "unchecked"})
	public net.minecraft.world.phys.AABB getRenderBoundingBox(TileEntityBase01Root aBE) {
		net.minecraft.world.phys.AABB rBox = aBE.mRenderAABB;
		BlockEntityRenderer tSpecial = SPECIAL_RENDERERS.get(aBE.getClass());
		if (tSpecial != null) try {
			net.minecraft.world.phys.AABB tSpecialBox = tSpecial.getRenderBoundingBox(aBE);
			rBox = rBox == null ? tSpecialBox : (tSpecialBox == null ? rBox : rBox.minmax(tSpecialBox));
		} catch (Throwable e) {/* a broken bounding box from one MTE must not crash the frame */}
		return rBox == null ? net.minecraft.world.phys.AABB.INFINITE : rBox;
	}

	// Since neo registers a BER per BlockEntityType and every MTE shares one type, per-class dispatch (matching
	// 1.7.10's ClientRegistry.bindTileEntitySpecialRenderer) has to live here instead, in a class->renderer registry.
	@SuppressWarnings("rawtypes")
	private static final java.util.Map<Class<?>, BlockEntityRenderer> SPECIAL_RENDERERS = new java.util.HashMap<>();
	public static void bindSpecialRenderer(Class<?> aTileEntityClass, @SuppressWarnings("rawtypes") BlockEntityRenderer aRenderer) {SPECIAL_RENDERERS.put(aTileEntityClass, aRenderer);}

	/** Diagnostic counters for the quad cache: render-object extracts, real rebuilds, and cache hits. */
	public static final java.util.concurrent.atomic.AtomicLong sQuadExtracts = new java.util.concurrent.atomic.AtomicLong(), sQuadBuilds = new java.util.concurrent.atomic.AtomicLong(), sQuadCacheHits = new java.util.concurrent.atomic.AtomicLong();
	/** Call counter for onSectionDirty, used by the gt6berstorm stand to catch an O(N) call storm. */
	public static final java.util.concurrent.atomic.AtomicLong sSectionDirtyCalls = new java.util.concurrent.atomic.AtomicLong();

	/** The quad cache is invalidated wholesale on atlas/model reshuffles (allChanged, since cached UVs would go stale)
	 *  and per-section via onSectionDirty, the same funnel the engine already uses whenever a block's appearance changes. */
	public static long sQuadEpoch = 0;

	/** setSectionDirty is cheap for the engine but called in large batches per block change, so a per-section scan on
	 *  every call was measured causing a call storm; work moved to render time, where each MTE compares stamps. */
	private static final it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap SECTION_STAMP = new it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap();

	/** Stamps a section from setSectionDirty on the main thread; O(1), touching neither the chunk nor its block entities. */
	public static void onSectionDirty(int aSectionX, int aSectionY, int aSectionZ) {
		sSectionDirtyCalls.incrementAndGet();
		SECTION_STAMP.addTo(net.minecraft.core.SectionPos.asLong(aSectionX, aSectionY, aSectionZ), 1L);
	}

	/** Stamp of the section holding this position; 0 is a legitimate value meaning the section was never marked. */
	private static long sectionStamp(BlockPos aPos) {
		return SECTION_STAMP.get(net.minecraft.core.SectionPos.asLong(
			  net.minecraft.core.SectionPos.blockToSectionCoord(aPos.getX())
			, net.minecraft.core.SectionPos.blockToSectionCoord(aPos.getY())
			, net.minecraft.core.SectionPos.blockToSectionCoord(aPos.getZ())));
	}

	/** Full reset from allChanged: the epoch invalidates every cache at once, and section stamps clear with it,
	 *  so the map doesn't grow across world switches. */
	public static void onRenderAllChanged() {sQuadEpoch++; SECTION_STAMP.clear();}
	/** Counter of crack-decal submits, used as the test judge for crack rendering. */
	public static final java.util.concurrent.atomic.AtomicLong sCrackSubmits = new java.util.concurrent.atomic.AtomicLong();

	public static boolean hasSpecialRenderer(Class<?> aTileEntityClass) {return SPECIAL_RENDERERS.containsKey(aTileEntityClass);}

	/** Item-form of TESR classes: 1.7.10 called the special renderer directly on a canonical TE built from stack NBT;
	 *  neo's carrier is the item's special-model layer, which routes here to the same registered renderer by class. */
	public static final net.minecraft.client.renderer.special.SpecialModelRenderer<net.minecraft.world.level.block.entity.BlockEntity> SPECIAL_ITEM_FORM = new net.minecraft.client.renderer.special.SpecialModelRenderer<net.minecraft.world.level.block.entity.BlockEntity>() {
		@Override
		@SuppressWarnings("unchecked")
		public void submit(net.minecraft.world.level.block.entity.BlockEntity aBE, PoseStack aPoseStack, SubmitNodeCollector aNodes, int aLight, int aOverlay, boolean aFoil, int aOutline) {
			if (aBE == null) return;
			@SuppressWarnings("rawtypes") BlockEntityRenderer tRenderer = SPECIAL_RENDERERS.get(aBE.getClass());
			if (tRenderer == null) return;
			try {
				BlockEntityRenderState tState = (BlockEntityRenderState)tRenderer.createRenderState();
				tRenderer.extractRenderState(aBE, tState, 0, Vec3.ZERO, null);
				tState.lightCoords = aLight;
				tRenderer.submit(tState, aPoseStack, aNodes, null);
			} catch (Throwable e) {/* item-form must not crash the render */}
		}
		@Override public void getExtents(java.util.function.Consumer<org.joml.Vector3fc> aOutput) {
			for (int x = 0; x <= 1; x++) for (int y = 0; y <= 1; y++) for (int z = 0; z <= 1; z++) aOutput.accept(new org.joml.Vector3f(x, y, z));
		}
		@Override public net.minecraft.world.level.block.entity.BlockEntity extractArgument(net.minecraft.world.item.ItemStack aStack) {
			try {
				if (aStack.getItem() instanceof gregapi.block.multitileentity.MultiTileEntityItemInternal tMTE) {
					gregapi.block.multitileentity.MultiTileEntityContainer tCont = tMTE.mBlock.mMultiTileEntityRegistry.getNewTileEntityContainer(aStack);
					// Second path where a detached TE is born, for items with their own special renderer (chest, mass
					// storage); facing compensation comes from the same center as the ordinary item renderer.
					if (tCont != null && tCont.mTileEntity != null && SPECIAL_RENDERERS.containsKey(tCont.mTileEntity.getClass())) return gregapi.block.multitileentity.MultiTileEntityRegistry.applyItemFacing(tCont.mTileEntity);
				}
			} catch (Throwable e) {/**/}
			return null;
		}
	};

	/** Snapshot of geometry collected on the main thread; thread-safe since submit only reads it. */
	public static class MTERenderState extends BlockEntityRenderState {
		public List<BakedQuad> mQuads;
		@SuppressWarnings("rawtypes") public BlockEntityRenderer mSpecialRenderer;
		public BlockEntityRenderState mSpecialState;
	}

	@Override public MTERenderState createRenderState() {return new MTERenderState();}

	@Override
	@SuppressWarnings("unchecked")
	public void extractRenderState(TileEntityBase01Root aBE, MTERenderState aState, float aPartialTicks, Vec3 aCameraPos, ModelFeatureRenderer.CrumblingOverlay aBreakProgress) {
		BlockEntityRenderer.super.extractRenderState(aBE, aState, aPartialTicks, aCameraPos, aBreakProgress); // base fields: blockPos, lightCoords, breakProgress
		aState.mQuads = null;
		aState.mSpecialRenderer = null; aState.mSpecialState = null;
		Block tBlock = aBE.getBlockState().getBlock();
		// Ores and stubs have no render object, so their clip box is set to a plain block cube here directly; otherwise
		// it would stay unknown forever, and an unknown box means 'don't clip' (see getRenderBoundingBox).
		if (aBE.getLevel() == null || !(aBE instanceof IRenderedBlockObject tRenderer) || !(tBlock instanceof MultiTileEntityBlock)) {
			aBE.mRenderAABB = new net.minecraft.world.phys.AABB(aBE.getBlockPos());
			return;
		}
		@SuppressWarnings("rawtypes") BlockEntityRenderer tSpecial = SPECIAL_RENDERERS.get(aBE.getClass());
		// Main gate: MTE's look is already built in the section mesh, so rebuilding it here every frame would be pure
		// double work; live geometry is needed only for the breaking block's cracks and the two per-frame renderers.
		if (tSpecial == null && aBreakProgress == null) {
			aBE.mRenderAABB = new net.minecraft.world.phys.AABB(aBE.getBlockPos());
			return;
		}
		BlockPos tPos = aBE.getBlockPos();
		// The cube clip box is set only for MTE without their own per-frame renderer; chest and mass-storage keep it
		// unknown (don't clip), matching 1.7.10's INFINITE default for TESR whose animation can extend past the block.
		if (tSpecial == null) aBE.mRenderAABB = new net.minecraft.world.phys.AABB(tPos);
		// Live geometry is built only for the currently-breaking block, so its crack decals land on the actual shape
		// (1:1 with 1.7.10's drawBlockDamageTexture); a special renderer draws its own animation independently.
		if (aBreakProgress != null) {
			sQuadExtracts.incrementAndGet();
			// A cache hit means neither the render epoch nor the own section's stamp changed since the build frame; a
			// never-marked section's stamp of 0 is legitimate since the first frame always starts as a guaranteed miss.
			long tSectionStamp = sectionStamp(tPos);
			if (aBE.mQuadCacheEpoch == sQuadEpoch && aBE.mQuadCacheSectionStamp == tSectionStamp) {
				aState.mQuads = aBE.mQuadCache;
				sQuadCacheHits.incrementAndGet();
			} else {
				GT6QuadBuilder tQB = new GT6QuadBuilder();
				try { GT6BlockModel.buildRendererQuads(tQB, tRenderer, tBlock, aBE.getLevel(), tPos.getX(), tPos.getY(), tPos.getZ()); } catch (Throwable e) {/* one MTE's render logic must not crash the frame */}
				if (!tQB.isEmpty()) aState.mQuads = tQB.breakingQuads();
				// Clip box is set to what this block entity actually drew, shifting the quads' local coordinates into world space.
				float[] tDrawn = tQB.drawnBounds();
				if (tDrawn != null) aBE.mRenderAABB = new net.minecraft.world.phys.AABB(
					  tPos.getX() + Math.min(tDrawn[0], 0F), tPos.getY() + Math.min(tDrawn[1], 0F), tPos.getZ() + Math.min(tDrawn[2], 0F)
					, tPos.getX() + Math.max(tDrawn[3], 1F), tPos.getY() + Math.max(tDrawn[4], 1F), tPos.getZ() + Math.max(tDrawn[5], 1F));
				aBE.mQuadCache = aState.mQuads; // null = 'no quads', also cached (validity is judged by epoch and section stamp).
				aBE.mQuadCacheEpoch = sQuadEpoch;
				aBE.mQuadCacheSectionStamp = tSectionStamp;
				sQuadBuilds.incrementAndGet();
			}
		}
		if (tSpecial != null) try {
			aState.mSpecialRenderer = tSpecial;
			aState.mSpecialState = (BlockEntityRenderState)tSpecial.createRenderState();
			tSpecial.extractRenderState(aBE, aState.mSpecialState, aPartialTicks, aCameraPos, aBreakProgress);
		} catch (Throwable e) {aState.mSpecialRenderer = null; aState.mSpecialState = null;}
	}

	@Override
	@SuppressWarnings("unchecked")
	public void submit(MTERenderState aState, PoseStack aPoseStack, SubmitNodeCollector aNodes, CameraRenderState aCamera) {
		final List<BakedQuad> tQuads = aState.mQuads;
		if (tQuads != null && !tQuads.isEmpty()) {
			final QuadInstance tQI = new QuadInstance(); // color=-1 (white, doesn't retint the baked quad's own color); light comes from the block's position.
			tQI.setLightCoords(aState.lightCoords);
			// The textured geometry already lives in the section mesh; submitting it here again causes coplanar
			// surfaces with different lighting. Only emit the breaking overlay from these live quads.
			// submitCustomGeometry has no built-in crumbling support, so the live quads are emitted through
			// SheetedDecalTextureGenerator so crack decals land on the actual surface, instead of a flat block face.
			// (LevelRenderer:939-945 → extractRenderState → BlockEntityRenderState.breakProgress).
			final net.minecraft.client.renderer.feature.ModelFeatureRenderer.CrumblingOverlay tBreak = aState.breakProgress;
			if (tBreak != null) {
				sCrackSubmits.incrementAndGet();
				aNodes.submitCustomGeometry(aPoseStack, net.minecraft.client.resources.model.ModelBakery.DESTROY_TYPES.get(tBreak.progress()), (tPose, tBuffer) -> {
					com.mojang.blaze3d.vertex.VertexConsumer tDecal = new com.mojang.blaze3d.vertex.SheetedDecalTextureGenerator(tBuffer, tBreak.cameraPose(), 1.0F);
					for (BakedQuad tQuad : tQuads) tDecal.putBakedQuad(tPose, tQuad, tQI);
				});
			}
		}
		if (aState.mSpecialRenderer != null && aState.mSpecialState != null)
			try {aState.mSpecialRenderer.submit(aState.mSpecialState, aPoseStack, aNodes, aCamera);} catch (Throwable e) {/* a broken special renderer must not crash the frame */}
	}
}

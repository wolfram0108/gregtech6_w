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
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;

import gregapi.block.multitileentity.MultiTileEntityBlock;
import gregapi.tileentity.base.TileEntityBase01Root;

/** MTE geometry used to be rebuilt here every frame; the chunk-mesh cache does hold a live BlockEntity after all,
 *  so the look now lives in the section mesh (GT6BlockModel), and only chest/mass-storage keep a per-frame renderer. */
public class MultiTileEntityBER implements BlockEntityRenderer<TileEntityBase01Root> {

	public MultiTileEntityBER(BlockEntityRendererProvider.Context aContext) {/* per-BE geometry is built in render(); the context's resources aren't needed here */}

	// The section mesh already renders to full view distance, so this BER needs no distance override of its own.
	// The engine's 64-block default matches 1.7.10's TESR radius, which only chest and mass storage ever used.

	// Vanilla dispatches by BlockEntityType, and every MTE shares one type, so this BER dispatches by class internally.
	@SuppressWarnings("rawtypes")
	private static final java.util.Map<Class<?>, BlockEntityRenderer> SPECIAL_RENDERERS = new java.util.HashMap<>();
	public static void bindSpecialRenderer(Class<?> aTileEntityClass, @SuppressWarnings("rawtypes") BlockEntityRenderer aRenderer) {SPECIAL_RENDERERS.put(aTileEntityClass, aRenderer);}

	/** Diagnostic counters for the quad cache: render calls, actual rebuilds, and cache hits. */
	public static final java.util.concurrent.atomic.AtomicLong sQuadExtracts = new java.util.concurrent.atomic.AtomicLong(), sQuadBuilds = new java.util.concurrent.atomic.AtomicLong(), sQuadCacheHits = new java.util.concurrent.atomic.AtomicLong();

	/** The render epoch invalidates every cached quad at once on an atlas/model reload or a distance change.
	 *  Per-section invalidation reuses the same engine signal that rebuilt the 1.7.10 mesh, so staleness can't outlive it. */
	public static long sQuadEpoch = 0;

	/** setSectionDirty used to sweep every block entity in a chunk, 27x per block change - seconds-long freezes.
	 *  Now the signal only stamps the section (O(1)); each MTE checks its own stamp against it at render time. */
	private static final it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap SECTION_STAMP = new it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap();

	/** Stamps a section from setSectionDirty on the main thread; O(1), touching neither the chunk nor its block entities. */
	public static void onSectionDirty(int aSectionX, int aSectionY, int aSectionZ) {
		SECTION_STAMP.addTo(net.minecraft.core.SectionPos.asLong(aSectionX, aSectionY, aSectionZ), 1L);
	}

	/** Stamp of the section holding this position (0 = never marked - a legitimate value, see render). */
	private static long sectionStamp(BlockPos aPos) {
		return SECTION_STAMP.get(net.minecraft.core.SectionPos.asLong(
			  net.minecraft.core.SectionPos.blockToSectionCoord(aPos.getX())
			, net.minecraft.core.SectionPos.blockToSectionCoord(aPos.getY())
			, net.minecraft.core.SectionPos.blockToSectionCoord(aPos.getZ())));
	}

	/** Full reset from allChanged: the epoch invalidates every cache at once, and section stamps clear with it,
	 *  so the map doesn't grow across world switches. */
	public static void onRenderAllChanged() {sQuadEpoch++; SECTION_STAMP.clear();}

	public static boolean hasSpecialRenderer(Class<?> aTileEntityClass) {return SPECIAL_RENDERERS.containsKey(aTileEntityClass);}

	/** 1.7.10's item render called renderTileEntityAt on a canonical TE built from the stack's NBT.
	 *  The 1.20.1 carrier is BlockEntityWithoutLevelRenderer, reached when the item model reports isCustomRenderer(). */
	/** Bridges from the common item class into client code via a lazy invokestatic.
	 *  The same pattern as MTEChestRenderer.bindFirst. */
	public static void bindItemExtensions(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> aConsumer) {
		aConsumer.accept(new net.minecraftforge.client.extensions.common.IClientItemExtensions() {
			@Override public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() {return specialItemForm();}
		});
	}

	private static SpecialItemForm sItemForm;
	/** Lazy instantiation: the constructor needs live client nodes that aren't ready yet when this class loads. */
	public static SpecialItemForm specialItemForm() {
		if (sItemForm == null) sItemForm = new SpecialItemForm();
		return sItemForm;
	}

	/** A separate class because 1.20.1 requires a real subclass of BlockEntityWithoutLevelRenderer, not an anonymous interface. */
	public static final class SpecialItemForm extends net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer {
		private SpecialItemForm() {
			super(net.minecraft.client.Minecraft.getInstance().getBlockEntityRenderDispatcher(), net.minecraft.client.Minecraft.getInstance().getEntityModels());
		}

		@Override
		@SuppressWarnings("unchecked")
		public void renderByItem(net.minecraft.world.item.ItemStack aStack, net.minecraft.world.item.ItemDisplayContext aContext, PoseStack aPoseStack, MultiBufferSource aBuffer, int aLight, int aOverlay) {
			net.minecraft.world.level.block.entity.BlockEntity tBE = extractSpecialItemForm(aStack);
			if (tBE == null) return;
			@SuppressWarnings("rawtypes") BlockEntityRenderer tRenderer = SPECIAL_RENDERERS.get(tBE.getClass());
			if (tRenderer == null) return;
			try {
				tRenderer.render(tBE, 0F, aPoseStack, aBuffer, aLight, aOverlay);
			} catch (Throwable e) {/* item-form must not crash the render */}
		}

	}

	/** Builds the same detached canonical TE that the ordinary item renderer creates, or returns null.
	 *  Kept static: the item model must know whether a special renderer is needed before BEWLR exists at all. */
	public static net.minecraft.world.level.block.entity.BlockEntity extractSpecialItemForm(net.minecraft.world.item.ItemStack aStack) {
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

	/** The engine only wraps the buffer with a decal generator when this position has destruction progress.
	 *  Reading that same signal avoids keeping a second registry of breaking blocks that could drift out of sync. */
	private static boolean isCrumbling(MultiBufferSource aBuffer) {
		return !(aBuffer instanceof MultiBufferSource.BufferSource);
	}

	@Override
	public void render(TileEntityBase01Root aBE, float aPartialTicks, PoseStack aPoseStack, MultiBufferSource aBuffer, int aLight, int aOverlay) {
		Block tBlock = aBE.getBlockState().getBlock();
		// Only render-object MTE blocks build real geometry; ores and loader stubs fall back to a plain block cube.
		// The damage-clip box for gated-out blocks must be set here, or it stays unknown, which means never clipping.
		if (aBE.getLevel() == null || !(aBE instanceof IRenderedBlockObject tRenderer) || !(tBlock instanceof MultiTileEntityBlock)) {
			aBE.mRenderAABB = new net.minecraft.world.phys.AABB(aBE.getBlockPos());
			return;
		}
		@SuppressWarnings("rawtypes") BlockEntityRenderer tSpecialRenderer = SPECIAL_RENDERERS.get(aBE.getClass());
		// The MTE look already lives in the section mesh, so rebuilding it here every frame is pure duplicate work.
		// Live geometry is built only for a block under destruction (for cracks) or one with its own per-frame renderer.
		if (tSpecialRenderer == null && !isCrumbling(aBuffer)) {
			aBE.mRenderAABB = new net.minecraft.world.phys.AABB(aBE.getBlockPos());
			return;
		}
		BlockPos tPos = aBE.getBlockPos();
		// The block-cube clip box is set only for MTEs without their own per-frame renderer.
		// Chest and mass storage keep it unknown (unclipped), matching 1.7.10's default infinite box for TESR animations.
		if (tSpecialRenderer == null) aBE.mRenderAABB = new net.minecraft.world.phys.AABB(tPos);
		// Live geometry is emitted only for a block under destruction, so the engine can overlay cracks on its real shape.
		if (isCrumbling(aBuffer)) emitLiveQuads(aBE, tRenderer, tBlock, tPos, aPoseStack, aBuffer, aLight, aOverlay);
		if (tSpecialRenderer != null) try {
			tSpecialRenderer.render(aBE, aPartialTicks, aPoseStack, aBuffer, aLight, aOverlay);
		} catch (Throwable e) {/* a broken special renderer must not crash the frame */}
	}

	/** Live MTE geometry comes from the same center that builds the section mesh; called only for a block under destruction. */
	private void emitLiveQuads(TileEntityBase01Root aBE, IRenderedBlockObject tRenderer, Block tBlock, BlockPos tPos, PoseStack aPoseStack, MultiBufferSource aBuffer, int aLight, int aOverlay) {
		sQuadExtracts.incrementAndGet();
		List<BakedQuad> tQuads;
		// A cache hit means neither the render epoch nor this section's own stamp moved since the geometry was built.
		// Stamp 0 for a never-marked section is legitimate: the first frame misses (Long.MIN_VALUE seed), then 0==0 holds.
		long tSectionStamp = sectionStamp(tPos);
		if (aBE.mQuadCacheEpoch == sQuadEpoch && aBE.mQuadCacheSectionStamp == tSectionStamp) {
			tQuads = aBE.mQuadCache;
			sQuadCacheHits.incrementAndGet();
		} else {
			aBE.mRenderAABB = new net.minecraft.world.phys.AABB(tPos);
			GT6QuadBuilder tQB = new GT6QuadBuilder();
			try { GT6BlockModel.buildRendererQuads(tQB, tRenderer, tBlock, aBE.getLevel(), tPos.getX(), tPos.getY(), tPos.getZ()); } catch (Throwable e) {/* one MTE's render logic must not crash the frame */}
			tQuads = tQB.isEmpty() ? null : tQB.quads();
			// Clip box is set to what this block entity actually drew, shifting the quads' local coordinates into world space.
			float[] tDrawn = tQB.drawnBounds();
			if (tDrawn != null) aBE.mRenderAABB = new net.minecraft.world.phys.AABB(
				  tPos.getX() + Math.min(tDrawn[0], 0F), tPos.getY() + Math.min(tDrawn[1], 0F), tPos.getZ() + Math.min(tDrawn[2], 0F)
				, tPos.getX() + Math.max(tDrawn[3], 1F), tPos.getY() + Math.max(tDrawn[4], 1F), tPos.getZ() + Math.max(tDrawn[5], 1F));
			aBE.mQuadCache = tQuads; // null = 'no quads', also cached (validity is judged by epoch and section stamp).
			aBE.mQuadCacheEpoch = sQuadEpoch;
			aBE.mQuadCacheSectionStamp = tSectionStamp;
			sQuadBuilds.incrementAndGet();
		}
		if (tQuads != null && !tQuads.isEmpty()) {
			// Quads are already in local block coordinates; cutoutBlockSheet's affectsCrumbling means the engine adds cracks itself.
			VertexConsumer tConsumer = aBuffer.getBuffer(Sheets.cutoutBlockSheet());
			PoseStack.Pose tPose = aPoseStack.last();
			// The short putBulkData overload defaults readExistingColor to false, discarding the material color baked into
			// the vertices; the long overload (true) reads it back, matching the chunk-mesh path.
			int[] tLights = {aLight, aLight, aLight, aLight};
			float[] tBrightness = {1F, 1F, 1F, 1F};
			for (BakedQuad tQuad : tQuads) tConsumer.putBulkData(tPose, tQuad, tBrightness, 1F, 1F, 1F, tLights, aOverlay, true);
		}
	}
}

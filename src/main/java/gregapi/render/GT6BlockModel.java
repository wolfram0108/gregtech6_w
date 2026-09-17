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

import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.ChunkRenderTypeSet;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.client.model.data.ModelProperty;
import net.minecraft.core.Direction;

/** One dynamic model for every GT6 block, the same centralization as Greg's own single RendererBlockTextured: the
 *  engine calls getModelData, which reuses GT6's texture logic and bakes quads instead of drawing them immediately. */
public class GT6BlockModel implements BakedModel {
	/** The 'world to model' channel: one key shared by the whole mod, not a per-block copy. */
	public static final ModelProperty<QuadContext> PROPERTY = new ModelProperty<>();

	/** Ready-made geometry for a single position, built once in getModelData; getQuads only slices it by side afterward. */
	public static final class QuadContext {
		final GT6QuadBuilder.QuadSet mQuads;
		QuadContext(GT6QuadBuilder.QuadSet aQuads) {mQuads = aQuads;}
	}

	/** The owning block, needed only where there's no world context (the engine's breaking overlay, a static model query);
	 *  null falls back to a plain cube. */
	private final Block mOwner;
	/** getParticleIcon(ModelData) is given neither state nor position on this engine version, so the crumb's meta comes
	 *  from here, through the same center GT6's own tint source already reads it from. */
	private final BlockState mOwnerState;
	/** A lazy cache of the context-free form, a pure function of the owner; the breaking overlay and JourneyMap call it often. */
	private volatile GT6QuadBuilder.QuadSet mContextFree;
	private volatile boolean mContextFreeBuilt = false;

	/** The particle sprite fallback resolves lazily: this event fires before ModelManager is ready to be read, and
	 *  getParticleIcon itself is only called later, at actual render time. */
	public GT6BlockModel() {this(null, null);}
	public GT6BlockModel(Block aOwner) {this(aOwner, null);}
	public GT6BlockModel(Block aOwner, BlockState aOwnerState) {mOwner = aOwner; mOwnerState = aOwnerState;}

	/** The fallback crumb sprite, 1:1 with the old single-particle model; no blocks/ prefix, since the atlas source adds one. */
	private static TextureAtlasSprite sErrorParticle;
	private static TextureAtlasSprite errorParticle() {
		if (sErrorParticle == null) sErrorParticle = GT6QuadBuilder.resolveSprite(new ResourceLocation("gregtech", "system/error"));
		return sErrorParticle;
	}
	/** Resets the lazy sprite whenever the atlas rebakes, alongside the item-model caches. */
	public static void invalidateParticle() {sErrorParticle = null;}

	// The BakedModel contract on this engine version starts here.

	/** Where the model actually sees the world: chunk compilation calls this right before tessellation, and only here
	 *  does GT6's own render-pass/bounds/texture chain run at all. */
	@Override
	public ModelData getModelData(BlockAndTintGetter aLevel, BlockPos aPos, BlockState aState, ModelData aModelData) {
		GT6QuadBuilder.QuadSet tQuads = collect(aLevel, aPos, aState);
		return tQuads == null ? aModelData : aModelData.derive().with(PROPERTY, new QuadContext(tQuads)).build();
	}

	@Override
	public List<BakedQuad> getQuads(BlockState aState, Direction aSide, RandomSource aRandom, ModelData aData, RenderType aRenderType) {
		QuadContext tCtx = aData == null ? null : aData.get(PROPERTY);
		return (tCtx != null ? tCtx.mQuads : contextFree()).getQuads(aSide);
	}

	/** The plain vanilla overload with no ModelData; consumers outside the chunk-compile path land here instead. */
	@Override
	public List<BakedQuad> getQuads(BlockState aState, Direction aSide, RandomSource aRandom) {
		return getQuads(aState, aSide, aRandom, ModelData.EMPTY, null);
	}

	/** 1.7.10's getRenderBlockPass() (0=cutout, 1=blend) channel; this engine version never picks a layer itself, so the
	 *  mod must, and the item side specifically asks the ITEM model for it -- without this, GUI glass rendered opaque. */
	public static ChunkRenderTypeSet renderTypesOf(BlockState aState, Block aFallback) {
		return ChunkRenderTypeSet.of(renderTypeOf(aState == null ? aFallback : aState.getBlock()));
	}

	/** The fluid-surface layer is a fourth consumer of the same formula: fluids draw through their own engine pass, which
	 *  reads its own render-layer table, so the mod's fluids register there too, using the same 1.7.10 pass value. */
	public static RenderType renderTypeOf(Block aBlock) {
		int tPass = aBlock instanceof gregapi.block.IBlock tI ? tI.getRenderBlockPass() : 0;
		return tPass > 0 ? RenderType.translucent() : RenderType.cutout();
	}

	/** The block's render layer in the world, through the same center as the item arm above. */
	@Override
	public ChunkRenderTypeSet getRenderTypes(BlockState aState, RandomSource aRandom, ModelData aData) {
		return renderTypesOf(aState, mOwner);
	}

	@Override public boolean useAmbientOcclusion() {return true;}
	@Override public boolean isGui3d() {return true;}
	@Override public boolean usesBlockLight() {return true;}
	@Override public boolean isCustomRenderer() {return false;}
	@Override public ItemOverrides getOverrides() {return ItemOverrides.EMPTY;}
	@Override public ItemTransforms getTransforms() {return ItemTransforms.NO_TRANSFORMS;}
	@Override public TextureAtlasSprite getParticleIcon() {return errorParticle();}

	// GT6's own render chain starts here.

	/** Geometry assembly for one position, split out since on this engine version it runs before getQuads, from getModelData. */
	private GT6QuadBuilder.QuadSet collect(BlockAndTintGetter aLevel, BlockPos aPos, BlockState aState) {
		// The whole render chain runs inside a bounds context (BlockBase.RENDER_BOUNDS_CTX): setBlockBounds writes a
		// thread-local copy per pass, not the shared Block fields, so passes on different threads can't clobber each other.
		boolean[] tCtx = gregapi.block.BlockBase.RENDER_BOUNDS_CTX.get(); boolean tPrevCtx = tCtx[0]; tCtx[0] = true;
		try {
			return collectParts0(aLevel, aPos, aState);
		} finally {tCtx[0] = tPrevCtx;}
	}

	/** The context-free block shape for the breaking overlay and static model queries; a pure function of the owner block. */
	private GT6QuadBuilder.QuadSet contextFree() {
		if (mContextFreeBuilt) return mContextFree;
		boolean[] tCtx = gregapi.block.BlockBase.RENDER_BOUNDS_CTX.get(); boolean tPrevCtx = tCtx[0]; tCtx[0] = true;
		try {mContextFree = buildContextFree();} catch (Throwable e) {mContextFree = new GT6QuadBuilder().build();} finally {tCtx[0] = tPrevCtx;}
		mContextFreeBuilt = true;
		return mContextFree;
	}

	/** There's no world context at all here (the breaking overlay passes ModelData.EMPTY, other readers are outside the
	 *  world entirely), so the crack shape is built by owner type, using the same geometry as the 3D inventory icon. */
	private GT6QuadBuilder.QuadSet buildContextFree() {
		GT6QuadBuilder tCrackQB = new GT6QuadBuilder();
		if (mOwner instanceof gregapi.block.multitileentity.MultiTileEntityBlock || mOwner instanceof gregapi.block.multitileentity.MultiTileEntityBlockInternal) return tCrackQB.build();
		if (mOwner != null) {
			net.minecraft.world.item.Item tOwnerItem = net.minecraft.world.item.Item.byBlock(mOwner);
			if (tOwnerItem != null && tOwnerItem != net.minecraft.world.item.Items.AIR) {
				try {buildInventoryQuads(tCrackQB, mOwner, new net.minecraft.world.item.ItemStack(tOwnerItem));} catch (Throwable e) {/* cube/cross fallback below */}
			}
		}
		if (tCrackQB.isEmpty()) {
			net.minecraft.resources.ResourceLocation tCrackIcon = null;
			try {
				if (mOwner instanceof IRenderedCross tCross) tCrackIcon = tCross.getCrossIcon(null, 0, 0, 0); // aWorld==null contract: meta is passed via aX (see buildInventoryQuads).
				else if (mOwner instanceof gregapi.block.IBlock tGT6) tCrackIcon = tGT6.getIcon(1, 0);
			} catch (Throwable e) {/* cube/cross fallback below */}
			if (tCrackIcon == null) tCrackIcon = gregapi.old.Textures.BlockIcons.CFOAM_HARDENED.getIcon(0);
			if (mOwner instanceof IRenderedCross) {
				tCrackQB.crossFace(tCrackIcon, gregapi.data.CS.UNCOLOURED);
			} else {
				tCrackQB.setBounds(mOwner instanceof gregapi.block.IBlock tIB ? tIB.getRenderBounds() : null);
				for (byte tSide = 0; tSide < 6; tSide++) tCrackQB.putFace(tSide, tCrackIcon, gregapi.data.CS.UNCOLOURED);
			}
		}
		return tCrackQB.build();
	}

	private GT6QuadBuilder.QuadSet collectParts0(BlockAndTintGetter aLevel, BlockPos aPos, BlockState aState) {
		// BlockBaseRail extends vanilla BaseRailBlock, not IRenderedBlock, so rails get their own flat quad-by-meta branch
		// here instead of going through the IRenderedBlock box-chain below.
		if (aState.getBlock() instanceof gregapi.block.misc.BlockBaseRail tRail) {
			GT6QuadBuilder tRailQB = new GT6QuadBuilder();
			RailRenderer.collectRailQuads(tRailQB, aLevel, aPos.getX(), aPos.getY(), aPos.getZ(), tRail);
			return tRailQB.build();
		}
		if (!(aState.getBlock() instanceof IRenderedBlock tRB)) return null;
		Block tBlock = aState.getBlock();
		int tX = aPos.getX(), tY = aPos.getY(), tZ = aPos.getZ();
		GT6QuadBuilder tQB = new GT6QuadBuilder();

		// Plants/flowers (IRenderedCross) get an X-shaped model from two diagonal planes, bypassing the cubic-face chain entirely.
		if (tRB instanceof IRenderedCross tCross) {
			tQB.crossFace(tCross.getCrossIcon(aLevel, tX, tY, tZ), tCross.getCrossRGBa(aLevel, tX, tY, tZ));
			return tQB.build();
		}

		// Fluid blocks (oil/gas/geo-water) get a 1:1 port of RendererBlockFluid's quanta-height rendering with neighbor-
		// averaged slopes, instead of the IRenderedBlock box path (still used for the item-form icon).
		if (tBlock instanceof gregapi.block.fluid.BlockBaseFluid tFluid) {
			RendererBlockFluid.collectFluidQuads(tQB, aLevel, tX, tY, tZ, tFluid);
			return tQB.build();
		}

		// MTEs render through the ordinary section-mesh path, like 1.7.10 did, not a per-frame redraw: the chunk-compile
		// region hands back the same live block-entity object the client level has, not a null one.

		// 1:1 port of the original's double passRenderingToObject dispatch: for an MTE the render object is the live block
		// entity itself; with no block entity it falls to the block branch, where an MTE block's own pass count is 0.
		IRenderedBlockObject tRenderer = tRB.passRenderingToObject(aLevel, tX, tY, tZ);
		if (tRenderer != null) tRenderer = tRenderer.passRenderingToObject(aLevel, tX, tY, tZ);

		if (tRenderer == null) {
			// 1:1 port of RenderBlocks.renderBlockLog: PILLAR blocks (logs/beams/bales) rotate face UVs by the stacking axis.
			if (tBlock instanceof gregapi.block.BlockBase tBB && tBB.getRenderType() == gregapi.data.CS.PILLAR_RENDER) {
				int tAxis = gregapi.util.WD.meta(aLevel, tX, tY, tZ) & gregapi.data.CS.PILLAR_BITS;
				if (tAxis == gregapi.data.CS.PILLAR_X) tQB.setUVRotate(1, 1, 1, 1, 0, 0);
				else if (tAxis == gregapi.data.CS.PILLAR_Z) tQB.setUVRotate(0, 0, 0, 0, 1, 1);
			}
			boolean[] tSides = sides(tBlock, tRB instanceof IRenderedBlockObjectSideCheck ? (IRenderedBlockObjectSideCheck)tRB : null);
			// setBlockBounds contract (1:1 with renderWorldBlock): true means re-read bounds from the block, false means a full
			// cube; ignoring the return value let a pebble's mini-box leak into unrelated blocks sharing the same Block instance.
			boolean tNeedsToSetBounds = true;
			for (int i = 0, j = tRB.getRenderPasses(aLevel, tX, tY, tZ, tSides); i < j; i++) {
				if (!tRB.usesRenderPass(i, aLevel, tX, tY, tZ, tSides)) continue;
				if (tRB.setBlockBounds(i, aLevel, tX, tY, tZ, tSides)) {tNeedsToSetBounds = true;}
				else {if (tNeedsToSetBounds) gregapi.util.WD.setBlockBounds(tBlock, 0, 0, 0, 1, 1, 1); tNeedsToSetBounds = false;}
				applyBounds(tQB, tBlock);
				for (byte s = 0; s < 6; s++) face(tQB, tBlock, s, tRB.getTexture(i, s, tSides, aLevel, tX, tY, tZ), tX, tY, tZ);
			}
			if (tNeedsToSetBounds) gregapi.util.WD.setBlockBounds(tBlock, 0, 0, 0, 1, 1, 1); // anti-leak for the shared block (1:1 :132).
			tQB.clearUVRotate(); // 1:1 renderBlockLog: reset uvRotate* after renderStandardBlock.
		} else {
			// The same crash guard the BER branch already carries: one MTE's render logic mustn't take down anything else, and
			// here an uncaught exception would crash the whole game, since it happens inside chunk compilation.
			try {buildRendererQuads(tQB, tRenderer, tBlock, aLevel, tX, tY, tZ);} catch (Throwable e) {/* one MTE must not crash the whole section mesh */}
		}
		return tQB.build();
	}

	/** Render-object branch (getRenderPasses -> setBlockBounds -> getTexture -> quads), shared between the baked
	 *  collectParts path and the live-BE MultiTileEntityBER path; renderBlock=true means the object drew itself already. */
	public static void buildRendererQuads(GT6QuadBuilder aQB, IRenderedBlockObject aRenderer, Block aBlock, net.minecraft.world.level.BlockGetter aLevel, int aX, int aY, int aZ) {
		// The bounds-context brackets apply here too, since this method is also called directly from MultiTileEntityBER.
		boolean[] tCtx = gregapi.block.BlockBase.RENDER_BOUNDS_CTX.get(); boolean tPrevCtx = tCtx[0]; tCtx[0] = true;
		try {buildRendererQuads0(aQB, aRenderer, aBlock, aLevel, aX, aY, aZ);} finally {tCtx[0] = tPrevCtx;}
	}
	private static void buildRendererQuads0(GT6QuadBuilder aQB, IRenderedBlockObject aRenderer, Block aBlock, net.minecraft.world.level.BlockGetter aLevel, int aX, int aY, int aZ) {
		if (aRenderer.renderBlock(aBlock, aQB, aLevel, aX, aY, aZ)) return;
		boolean[] tSides = sides(aBlock, aRenderer instanceof IRenderedBlockObjectSideCheck ? (IRenderedBlockObjectSideCheck)aRenderer : null);
		// setBlockBounds contract (render-object branch of renderWorldBlock): false means a full cube, not leftover bounds.
		boolean tNeedsToSetBounds = true;
		for (int i = 0, j = aRenderer.getRenderPasses(aBlock, tSides); i < j; i++) {
			if (!aRenderer.usesRenderPass(i, tSides)) continue;
			if (aRenderer.setBlockBounds(aBlock, i, tSides)) {tNeedsToSetBounds = true;}
			else {if (tNeedsToSetBounds) gregapi.util.WD.setBlockBounds(aBlock, 0, 0, 0, 1, 1, 1); tNeedsToSetBounds = false;}
			applyBounds(aQB, aBlock);
			for (byte s = 0; s < 6; s++) face(aQB, aBlock, s, aRenderer.getTexture(aBlock, i, s, tSides), aX, aY, aZ);
		}
		if (tNeedsToSetBounds) gregapi.util.WD.setBlockBounds(aBlock, 0, 0, 0, 1, 1, 1); // anti-leak for the shared block (1:1 :158).
	}

	/** Verbatim port of RendererBlockTextured.renderInventoryBlock for the item-form 3D icon: either the TE branch via
	 *  passRenderingToObject(ItemStack) for MTE, or the block-level getRenderPasses/getTexture branch for ores. */
	public static void buildInventoryQuads(GT6QuadBuilder aQB, Block aBlock, net.minecraft.world.item.ItemStack aStack) {
		// The item-form icon is built on the render thread too, so it also runs inside the bounds context.
		boolean[] tCtx = gregapi.block.BlockBase.RENDER_BOUNDS_CTX.get(); boolean tPrevCtx = tCtx[0]; tCtx[0] = true;
		try {buildInventoryQuads0(aQB, aBlock, aStack);} finally {tCtx[0] = tPrevCtx;}
	}
	private static void buildInventoryQuads0(GT6QuadBuilder aQB, Block aBlock, net.minecraft.world.item.ItemStack aStack) {
		if (!(aBlock instanceof IRenderedBlock tRB)) return;
		// Cross-block item icons use the same two crossed planes as 1.7.10's drawCrossedSquares, since IRenderedCross's
		// cubic channels are contractually null and would otherwise yield an empty quad set.
		if (tRB instanceof IRenderedCross tCross) {
			int tMeta = gregapi.util.ST.meta_(aStack);
			aQB.crossFace(tCross.getCrossIcon(null, tMeta, 0, 0), tCross.getCrossRGBa(null, tMeta, 0, 0));
			return;
		}
		boolean[] tSides = {true, true, true, true, true, true}; // SIDES_ITEM_RENDER (no neighbors, so every face is shown).
		IRenderedBlockObject tRenderer = tRB.passRenderingToObject(aStack);
		if (tRenderer != null) tRenderer = tRenderer.passRenderingToObject(aStack);
		// setBlockBounds contract (1:1 with renderInventoryBlock): false means full cube plus the anti-leak reset.
		boolean tNeedsToSetBounds = true;
		if (tRenderer != null) {
			for (int i = 0, j = tRenderer.getRenderPasses(aBlock, tSides); i < j; i++) {
				if (!tRenderer.usesRenderPass(i, tSides)) continue;
				if (tRenderer.setBlockBounds(aBlock, i, tSides)) {tNeedsToSetBounds = true;}
				else {if (tNeedsToSetBounds) gregapi.util.WD.setBlockBounds(aBlock, 0, 0, 0, 1, 1, 1); tNeedsToSetBounds = false;}
				applyBounds(aQB, aBlock);
				for (byte s = 0; s < 6; s++) face(aQB, aBlock, s, tRenderer.getTexture(aBlock, i, s, tSides), 0, 0, 0);
			}
		} else {
			for (int i = 0, j = tRB.getRenderPasses(aStack); i < j; i++) {
				if (!tRB.usesRenderPass(i, aStack)) continue;
				if (tRB.setBlockBounds(i, aStack)) {tNeedsToSetBounds = true;}
				else {if (tNeedsToSetBounds) gregapi.util.WD.setBlockBounds(aBlock, 0, 0, 0, 1, 1, 1); tNeedsToSetBounds = false;}
				applyBounds(aQB, aBlock);
				for (byte s = 0; s < 6; s++) face(aQB, aBlock, s, tRB.getTexture(i, s, aStack), 0, 0, 0);
			}
		}
		if (tNeedsToSetBounds) gregapi.util.WD.setBlockBounds(aBlock, 0, 0, 0, 1, 1, 1); // 1:1 :92
	}

	/** tSides: a SideCheck object gets renderFullBlockSide; everyone else gets all-true, since neo's
	 *  addCulledFace already handles neighbor culling. */
	private static boolean[] sides(Block aBlock, IRenderedBlockObjectSideCheck aCheck) {
		boolean[] r = {true, true, true, true, true, true};
		if (aCheck != null) for (byte s = 0; s < 6; s++) r[s] = aCheck.renderFullBlockSide(aBlock, null, s);
		return r;
	}

	/** Copies the block's current render bounds (after setBlockBounds) into the quad-builder via the shared
	 *  IBlock.getRenderBounds contract, since GT6's six Block hierarchies share no common ancestor to branch on by class. */
	private static void applyBounds(GT6QuadBuilder aQB, Block aBlock) {
		aQB.setBounds(aBlock instanceof gregapi.block.IBlock tI ? tI.getRenderBounds() : null);
	}

	/** One per-side call into ITexture, which accumulates the resulting face into GT6QuadBuilder. */
	private static void face(GT6QuadBuilder aQB, Block aBlock, byte aSide, ITexture aTex, int aX, int aY, int aZ) {
		if (aTex == null || !aTex.isValidTexture()) return;
		switch (aSide) {
		case 0: aTex.renderYNeg(aQB, aBlock, aX, aY, aZ, 240, false); break;
		case 1: aTex.renderYPos(aQB, aBlock, aX, aY, aZ, 240, false); break;
		case 2: aTex.renderZNeg(aQB, aBlock, aX, aY, aZ, 240, false); break;
		case 3: aTex.renderZPos(aQB, aBlock, aX, aY, aZ, 240, false); break;
		case 4: aTex.renderXNeg(aQB, aBlock, aX, aY, aZ, 240, false); break;
		case 5: aTex.renderXPos(aQB, aBlock, aX, aY, aZ, 240, false); break;
		}
	}

	/** Resolves the crumb icon 1:1 with 1.7.10's own particle logic, through the shared IBlock.getIcon contract both fluid
	 *  hierarchies answer, so there's one branch; meta comes from the owner's BlockState, the tint source's own center. */
	@Override
	public TextureAtlasSprite getParticleIcon(ModelData aData) {
		try {
			if (mOwner instanceof gregapi.block.IBlock tGT6) {
				int tMeta = mOwnerState != null && mOwner instanceof gregapi.block.IBlockExtendedMetaData tMetaBlock ? tMetaBlock.getExtendedMetaData(mOwnerState) : 0;
				ResourceLocation tIcon = tGT6.getIcon(0, tMeta);
				if (tIcon == null) tIcon = gregapi.old.Textures.BlockIcons.CFOAM_HARDENED.getIcon(0);
				if (tIcon != null) {
					TextureAtlasSprite tSprite = GT6QuadBuilder.resolveSprite(tIcon);
					if (tSprite != null) return tSprite;
				}
			}
		} catch (Throwable e) {/* particle sprite must not crash rendering */}
		return errorParticle();
	}
}

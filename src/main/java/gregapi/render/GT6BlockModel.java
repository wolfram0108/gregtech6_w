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

import com.mojang.serialization.MapCodec;

import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.SimpleModelWrapper;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.client.resources.model.sprite.MaterialBaker;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.model.DynamicBlockStateModel;
import net.neoforged.neoforge.client.model.block.CustomUnbakedBlockStateModel;

/** A single dynamic model for every GT6 block, mirroring the original's one RendererBlockTextured centralization:
 *  neo calls collectParts, walking each pass/side through {@link ITexture} into {@link GT6QuadBuilder}. */
public class GT6BlockModel implements DynamicBlockStateModel {
	private final Material.Baked mParticle;
	/** The owning block is needed only for the engine's breaking/crumbling path, which calls collectParts with an AIR
	 *  state where the overlay shape would otherwise be unknowable; null falls back to a plain cube. */
	private final Block mOwner;

	GT6BlockModel(MaterialBaker aBaker) {
		net.minecraft.client.resources.model.ModelDebugName tDebugName = getClass()::toString;
		// No 'blocks/' prefix: the atlas source (atlases/blocks.json) already maps textures/blocks/** with an empty prefix.
		mParticle = aBaker.get(new Material(Identifier.fromNamespaceAndPath("gregtech", "system/error")), tDebugName);
		mOwner = null;
	}

	/** For the ModifyBakingResult path the particle sprite is already resolved from the event's own textureGetter. */
	public GT6BlockModel(Material.Baked aParticle) {mParticle = aParticle; mOwner = null;}
	public GT6BlockModel(Material.Baked aParticle, Block aOwner) {mParticle = aParticle; mOwner = aOwner;}

	@Override
	public void collectParts(BlockAndTintGetter aLevel, BlockPos aPos, BlockState aState, RandomSource aRandom, List<BlockStateModelPart> aParts) {
		// The whole render chain runs inside a bounds context (BlockBase.RENDER_BOUNDS_CTX): setBlockBounds writes a
		// thread-local copy per pass, not the shared Block fields, so passes on different threads can't clobber each other.
		boolean[] tCtx = gregapi.block.BlockBase.RENDER_BOUNDS_CTX.get(); boolean tPrevCtx = tCtx[0]; tCtx[0] = true;
		try {
			collectParts0(aLevel, aPos, aState, aRandom, aParts);
		} finally {tCtx[0] = tPrevCtx;}
	}
	private void collectParts0(BlockAndTintGetter aLevel, BlockPos aPos, BlockState aState, RandomSource aRandom, List<BlockStateModelPart> aParts) {
		// The engine's breaking path calls collectParts with dummy args (EMPTY level, ZERO pos, AIR state); the dynamic
		// model gives nothing on dummies, so crack decals fall back to per-owner static geometry, gated strictly on AIR state.
		if (aState.isAir()) {
			if (mOwner instanceof gregapi.block.multitileentity.MultiTileEntityBlock || mOwner instanceof gregapi.block.multitileentity.MultiTileEntityBlockInternal) return;
			GT6QuadBuilder tCrackQB = new GT6QuadBuilder();
			// The same branch also serves context-free static model requests (JourneyMap and similar mods reading a model
			// outside the world); it reuses the item-form quads so the map sees the same colorized icon a player would.
			if (mOwner != null) {
				net.minecraft.world.item.Item tOwnerItem = net.minecraft.world.item.Item.byBlock(mOwner);
				if (tOwnerItem != null && tOwnerItem != net.minecraft.world.item.Items.AIR) {
					try {buildInventoryQuads(tCrackQB, mOwner, new net.minecraft.world.item.ItemStack(tOwnerItem));} catch (Throwable e) {/* cube/cross fallback below */}
				}
			}
			if (tCrackQB.isEmpty()) {
				net.minecraft.resources.Identifier tCrackIcon = null;
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
			if (!tCrackQB.isEmpty()) aParts.add(new SimpleModelWrapper(tCrackQB.build(), true, mParticle));
			return;
		}
		// BlockBaseRail extends vanilla BaseRailBlock, not IRenderedBlock, so rails get their own flat quad-by-meta branch
		// here instead of going through the IRenderedBlock box-chain below.
		if (aState.getBlock() instanceof gregapi.block.misc.BlockBaseRail tRail) {
			GT6QuadBuilder tRailQB = new GT6QuadBuilder();
			RailRenderer.collectRailQuads(tRailQB, aLevel, aPos.getX(), aPos.getY(), aPos.getZ(), tRail);
			if (!tRailQB.isEmpty()) aParts.add(new SimpleModelWrapper(tRailQB.build(), true, mParticle));
			return;
		}
		if (!(aState.getBlock() instanceof IRenderedBlock tRB)) return;
		Block tBlock = aState.getBlock();
		int tX = aPos.getX(), tY = aPos.getY(), tZ = aPos.getZ();
		GT6QuadBuilder tQB = new GT6QuadBuilder();

		// Plants/flowers (IRenderedCross) get an X-shaped model from two diagonal planes, bypassing the cubic-face chain entirely.
		if (tRB instanceof IRenderedCross tCross) {
			tQB.crossFace(tCross.getCrossIcon(aLevel, tX, tY, tZ), tCross.getCrossRGBa(aLevel, tX, tY, tZ));
			aParts.add(new SimpleModelWrapper(tQB.build(), true, mParticle));
			return;
		}

		// Fluid blocks (oil/gas/geo-water) get a 1:1 port of RendererBlockFluid's quanta-height rendering with neighbor-
		// averaged slopes, instead of the IRenderedBlock box path (still used for the item-form icon).
		if (tBlock instanceof gregapi.block.fluid.BlockBaseFluid tFluid) {
			RendererBlockFluid.collectFluidQuads(tQB, aLevel, tX, tY, tZ, tFluid);
			aParts.add(new SimpleModelWrapper(tQB.build(), true, mParticle));
			return;
		}

		// The chunk-compile region hands the mesher the SAME live BlockEntity object (it copies the map, not the entities),
		// so MTE geometry belongs in the section mesh like every other block, not redrawn per frame via BER.

		// 1:1 port of renderWorldBlock's double passRenderingToObject: the render-object branch is the live BlockEntity
		// itself for MTE; without a BE (stub/ore) it falls to the block branch, whose MTE getRenderPasses is 0.
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
			// Same crash-guard as the BER branch: an exception here would be wrapped into a ReportedException by section
			// compilation and crash the whole game, not just drop one block's render.
			try {buildRendererQuads(tQB, tRenderer, tBlock, aLevel, tX, tY, tZ);} catch (Throwable e) {/* one MTE must not crash the whole section mesh */}
		}
		aParts.add(new SimpleModelWrapper(tQB.build(), true, mParticle));
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
		case 0: aTex.renderYNeg(aQB, aBlock, aX, aY, aZ, ITexture.Util.WORLD_BRIGHTNESS, false); break;
		case 1: aTex.renderYPos(aQB, aBlock, aX, aY, aZ, ITexture.Util.WORLD_BRIGHTNESS, false); break;
		case 2: aTex.renderZNeg(aQB, aBlock, aX, aY, aZ, ITexture.Util.WORLD_BRIGHTNESS, false); break;
		case 3: aTex.renderZPos(aQB, aBlock, aX, aY, aZ, ITexture.Util.WORLD_BRIGHTNESS, false); break;
		case 4: aTex.renderXNeg(aQB, aBlock, aX, aY, aZ, ITexture.Util.WORLD_BRIGHTNESS, false); break;
		case 5: aTex.renderXPos(aQB, aBlock, aX, aY, aZ, ITexture.Util.WORLD_BRIGHTNESS, false); break;
		}
	}

	/** JourneyMap and similar mods hit this static overload when quads are empty (MTE crack branch is deliberately
	 *  empty); it resolves the real owner icon via the same IBlock.getIcon contract used by the pos-aware overload below. */
	@Override
	public Material.Baked particleMaterial() {
		try {
			if (mOwner instanceof gregapi.block.IBlock tGT6) {
				net.minecraft.resources.Identifier tIcon = tGT6.getIcon(1, 0);
				if (tIcon == null) tIcon = gregapi.old.Textures.BlockIcons.CFOAM_HARDENED.getIcon(0);
				if (tIcon != null) {
					net.minecraft.client.renderer.texture.TextureAtlasSprite tSprite = GT6QuadBuilder.resolveSprite(tIcon);
					if (tSprite != null) return new Material.Baked(tSprite, false);
				}
			}
		} catch (Throwable e) {/* particle sprite must not crash rendering */}
		return mParticle;
	}

	/** Break/hit particles need a real per-position icon, not the single static mParticle every GT6 block shared;
	 *  resolved 1:1 with EntityDiggingFX via block.getIcon(0,meta), falling back to mParticle for MTE/iconless blocks. */
	@Override
	public Material.Baked particleMaterial(BlockAndTintGetter aLevel, BlockPos aPos, BlockState aState) {
		try {
			Block tBlock = aState.getBlock();
			Identifier tIcon = null;
			// Asking the shared getIcon contract once covers BlockBase, both fluid hierarchies and MTE in one branch; the old
			// per-hierarchy branch missed water-like blocks (river/ocean/swamp), which fell back to CFoam instead of water.
			if (tBlock instanceof gregapi.block.IBlock tGT6) tIcon = tGT6.getIcon(0, gregapi.util.WD.meta(aLevel, aPos.getX(), aPos.getY(), aPos.getZ()));
			// 1:1 with 1.7.10's default (BlockBase and MultiTileEntityBlock both fall back to CFOAM_HARDENED):
			// MTE/iconless particles are grey CFoam crumbs, not the error texture.
			if (tIcon == null) tIcon = gregapi.old.Textures.BlockIcons.CFOAM_HARDENED.getIcon(0);
			if (tIcon != null) {
				net.minecraft.client.renderer.texture.TextureAtlasSprite tSprite = GT6QuadBuilder.resolveSprite(tIcon);
				if (tSprite != null) return new Material.Baked(tSprite, false);
			}
		} catch (Throwable e) {/* particle sprite must not crash rendering */}
		return mParticle;
	}

	@Override
	public int materialFlags() {return 0;}

	/** Unbaked model type used for registration; blockstate JSON references it by {@code "type":"gregtech:gt6block"}. */
	public record Unbaked() implements CustomUnbakedBlockStateModel {
		public static final Identifier ID = Identifier.fromNamespaceAndPath("gregtech", "gt6block");
		public static final MapCodec<Unbaked> MAP_CODEC = MapCodec.unit(Unbaked::new);
		@Override public BlockStateModel bake(ModelBaker aBaker) {return new GT6BlockModel(aBaker.materials());}
		@Override public void resolveDependencies(Resolver aResolver) {}
		@Override public MapCodec<Unbaked> codec() {return MAP_CODEC;}
	}
}

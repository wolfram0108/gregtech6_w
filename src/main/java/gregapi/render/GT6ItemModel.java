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

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.pipeline.QuadBakingVertexConsumer;

/** One item model for every GT6 item, the same centralization as the block model: per-meta icon becomes flat quads or
 *  the block's 3D form. Registered by runtime injection since the mod is procedural, with icon lookup done by reflection. */
public class GT6ItemModel implements BakedModel {

	// ================================================================================================================
	// Flat item geometry is a pure function of sprite/tint/outline, so it's cached globally; BakedQuad is immutable,
	// so sharing it across frames and layers is as safe as the engine's own baked models already do, resetting only on rebake.
	// ================================================================================================================
	private record FlatKey(TextureAtlasSprite mSprite, int mColor, boolean mSides) {}
	private record InvKey(net.minecraft.world.level.block.Block mBlock, short mMeta, CompoundTag mTag) {}
	/** Key for a resolved per-stack model: item plus GT6 meta plus NBT, the same identity used elsewhere in the mod. */
	private record ModelKey(net.minecraft.world.item.Item mItem, short mMeta, CompoundTag mTag) {}
	private static final java.util.concurrent.ConcurrentHashMap<FlatKey, List<BakedQuad>> sFlatCache = new java.util.concurrent.ConcurrentHashMap<>();
	private static final java.util.concurrent.ConcurrentHashMap<InvKey, List<BakedQuad>> sInvCache = new java.util.concurrent.ConcurrentHashMap<>();
	private static final java.util.concurrent.ConcurrentHashMap<ModelKey, Baked> sModelCache = new java.util.concurrent.ConcurrentHashMap<>();

	/** Cache reset, called from onModifyBakingResult on every model/atlas rebake. */
	public static void invalidateCaches() {sFlatCache.clear(); sInvCache.clear(); sModelCache.clear(); sSideFaceCache.clear(); sVanillaTransforms.clear(); sVanillaTransformsTried.clear(); sBarOverlayIcons = null;}

	/** Caches reflective Method lookups, since getClass().getMethod(...) was previously called on every render pass
	 *  of every visible item, every frame; a cached null means 'no such method'. */
	private static final java.util.concurrent.ConcurrentHashMap<String, java.lang.reflect.Method> sMethodCache = new java.util.concurrent.ConcurrentHashMap<>();
	private static final java.lang.reflect.Method NO_METHOD;
	static {java.lang.reflect.Method m = null; try {m = Object.class.getMethod("hashCode");} catch (Throwable e) {} NO_METHOD = m;}
	private static java.lang.reflect.Method cachedMethod(Class<?> aClass, String aName, Class<?>... aArgs) {
		String tKey = aClass.getName() + '#' + aName + '#' + aArgs.length + (aArgs.length > 0 ? aArgs[0].getSimpleName() : "");
		java.lang.reflect.Method rMethod = sMethodCache.computeIfAbsent(tKey, k -> {
			try {return aClass.getMethod(aName, aArgs);} catch (Throwable e) {return NO_METHOD;}
		});
		return rMethod == NO_METHOD ? null : rMethod;
	}

	/** Flat item geometry (front + back + optional outline) from cache, built only on a cache miss. */
	private static List<BakedQuad> flatQuads(TextureAtlasSprite aSprite, int aColor, boolean aSides) {
		if (sFlatCache.size() > 16384) sFlatCache.clear(); // size safety valve (JEI pages through thousands of items)
		return sFlatCache.computeIfAbsent(new FlatKey(aSprite, aColor, aSides), aKey -> {
			java.util.ArrayList<BakedQuad> rQuads = new java.util.ArrayList<>(aSides ? 10 : 2);
			rQuads.add(flatFace(aSprite, true, aColor));
			rQuads.add(flatFace(aSprite, false, aColor));
			if (aSides) addSideQuads(rQuads, aSprite, aColor);
			return java.util.List.copyOf(rQuads);
		});
	}

	// The root model, where 'this model depends on the stack' actually happens, starts here.

	private final ItemOverrides mOverrides = new ItemOverrides() {
		@Override
		public BakedModel resolve(BakedModel aModel, ItemStack aStack, ClientLevel aLevel, LivingEntity aEntity, int aSeed) {
			try {return modelFor(aStack);} catch (Throwable e) {return aModel;} // Render-safe: a failure resolving one item's model doesn't crash rendering as a whole.
		}
	};

	@Override public ItemOverrides getOverrides() {return mOverrides;}
	@Override public List<BakedQuad> getQuads(BlockState aState, Direction aSide, RandomSource aRandom) {return java.util.List.of();}
	@Override public boolean useAmbientOcclusion() {return false;}
	@Override public boolean isGui3d() {return false;}
	@Override public boolean usesBlockLight() {return false;}
	@Override public boolean isCustomRenderer() {return false;}
	/** The item-side arm of the render-layer channel: the engine specifically asks the ITEM model for this, not the block
	 *  model, and without answering here GUI glass rendered opaque, since the default table knows nothing about GT6 blocks. */
	@Override public net.minecraftforge.client.ChunkRenderTypeSet getRenderTypes(BlockState aState, RandomSource aRandom, net.minecraftforge.client.model.data.ModelData aData) {
		return GT6BlockModel.renderTypesOf(aState, null);
	}
	@Override public TextureAtlasSprite getParticleIcon() {return GT6QuadBuilder.resolveSprite(gregapi.old.Textures.BlockIcons.CFOAM_HARDENED.getIcon(0));}
	@Override public ItemTransforms getTransforms() {return ItemTransforms.NO_TRANSFORMS;}

	/** The center of item rendering, reproducing the original's renderInventoryBlock: a block item gets 3D geometry,
	 *  a plain item gets flat per-pass icons with per-pass tint, the same as the original's multi-pass item icon. */
	private static Baked modelFor(ItemStack aStack) {
		if (sModelCache.size() > 16384) sModelCache.clear();
		CompoundTag tTag = aStack.getTag();
		return sModelCache.computeIfAbsent(new ModelKey(aStack.getItem(), gregapi.util.ST.meta_(aStack), tTag == null ? null : tTag.copy()), aKey -> build(aStack));
	}

	private static Baked build(ItemStack aStack) {
		net.minecraft.world.item.Item tItem = aStack.getItem();
		if (tItem instanceof net.minecraft.world.item.BlockItem tRailBI && tRailBI.getBlock() instanceof gregapi.block.misc.BlockBaseRail tRail) {
			// The rail is a block-item without IRenderedBlock, so the flat-icon path can't resolve it; its straight icon
			// (meta 0) is drawn directly instead, like the vanilla rail item.
			return buildRailItem(tRail);
		}
		if (tItem instanceof net.minecraft.world.item.BlockItem tBI && tBI.getBlock() instanceof IRenderedBlock) {
			return buildBlockInventory(aStack, tBI.getBlock());
		}
		return buildFlatItem(aStack, tItem);
	}

	// The resolved per-stack model starts here.

	/** The finished model for one stack: quads, poses, and flags; mWorldVariant is the same model without GUI-only
	 *  overlays, picked by applyTransform depending on where it's being drawn. */
	private static final class Baked implements BakedModel {
		final List<BakedQuad> mQuads;
		final ItemTransforms mTransforms;
		final boolean mGui3d, mBlockLight;
		final TextureAtlasSprite mParticle;
		Baked mWorldVariant = this;
		/** 1.7.10's TESR item renderers drew a special item form directly; this engine version routes the same case through
		 *  isCustomRenderer(), into a BEWLR custom renderer instead. */
		boolean mCustomRenderer = false;

		Baked(List<BakedQuad> aQuads, ItemTransforms aTransforms, boolean aGui3d, boolean aBlockLight, TextureAtlasSprite aParticle) {
			mQuads = aQuads; mTransforms = aTransforms == null ? ItemTransforms.NO_TRANSFORMS : aTransforms;
			mGui3d = aGui3d; mBlockLight = aBlockLight; mParticle = aParticle;
		}

		@Override public List<BakedQuad> getQuads(BlockState aState, Direction aSide, RandomSource aRandom) {return aSide == null ? mQuads : java.util.List.of();}
		@Override public boolean useAmbientOcclusion() {return false;}
		@Override public boolean isGui3d() {return mGui3d;}
		@Override public boolean usesBlockLight() {return mBlockLight;}
		@Override public boolean isCustomRenderer() {return mCustomRenderer;}
		/** The second carrier of the item-side render-layer channel: the engine asks the layer of whatever model
		 *  ItemOverrides actually resolved, so leaving this unanswered would still leave cached items with opaque glass. */
		@Override public net.minecraftforge.client.ChunkRenderTypeSet getRenderTypes(BlockState aState, RandomSource aRandom, net.minecraftforge.client.model.data.ModelData aData) {
			return GT6BlockModel.renderTypesOf(aState, null);
		}
		@Override public ItemOverrides getOverrides() {return ItemOverrides.EMPTY;}
		@Override public ItemTransforms getTransforms() {return mTransforms;}
		@Override public TextureAtlasSprite getParticleIcon() {return mParticle;}

		/** Durability/charge bars draw only in inventory/GUI, a split 1.7.10's render engine made for it automatically;
		 *  this port's bridge reproduces that split centrally here, using applyTransform as the render-context carrier instead. */
		@Override
		public BakedModel applyTransform(ItemDisplayContext aContext, PoseStack aPoseStack, boolean aLeftHand) {
			mTransforms.getTransform(aContext).apply(aLeftHand, aPoseStack);
			return aContext == ItemDisplayContext.GUI ? this : mWorldVariant;
		}
	}

	// The model-assembly branches start here.

	/** Item-form of a block: 3D geometry in the inventory via {@link GT6BlockModel#buildInventoryQuads}. */
	private static Baked buildBlockInventory(ItemStack aStack, net.minecraft.world.level.block.Block aBlock) {
		// A block-item's 3D form is a pure function of block, meta, and stack NBT; the cache is global, built only on a miss.
		if (sInvCache.size() > 16384) sInvCache.clear();
		CompoundTag tTag = aStack.getTag();
		List<BakedQuad> tBuilt = sInvCache.computeIfAbsent(new InvKey(aBlock, gregapi.util.ST.meta_(aStack), tTag == null ? null : tTag.copy()), aKey -> {
			GT6QuadBuilder tQB = new GT6QuadBuilder();
			try { GT6BlockModel.buildInventoryQuads(tQB, aBlock, aStack); } catch (Throwable e) {}
			return java.util.List.copyOf(tQB.quads());
		});
		// Raw cube geometry renders front-on and dark without a display transform, which 1.7.10's engine applied automatically;
		// the canonical isometric GUI transform is read from the engine's own block/block model here, not hardcoded.
		TextureAtlasSprite tParticle = tBuilt.isEmpty() ? null : tBuilt.get(0).getSprite();
		Baked rBaked = new Baked(tBuilt, blockGuiTransforms(), true, true, tParticle);
		// 1.7.10's TESR renderers called their TE renderer directly on a canonical TE built from stack NBT; the carrier here
		// is BEWLR instead, reached once the model declares isCustomRenderer, with the same class-based dispatch as before.
		if (tBuilt.isEmpty() && MultiTileEntityBER.extractSpecialItemForm(aStack) != null) rBaked.mCustomRenderer = true;
		return rBaked;
	}

	/** The rail's inventory icon is a flat straight-track icon, like vanilla's own rail item; the icon itself lives
	 *  among block textures, which is why this resolves items to blocks rather than the other way round. */
	private static Baked buildRailItem(gregapi.block.misc.BlockBaseRail aRail) {
		ResourceLocation tIcon = aRail.getIcon(0, 0);
		TextureAtlasSprite tSprite = tIcon == null ? null : GT6QuadBuilder.resolveSprite(tIcon, GT6QuadBuilder.ATLAS_ITEMS);
		if (tSprite == null && tIcon != null) tSprite = GT6QuadBuilder.resolveSprite(tIcon, GT6QuadBuilder.ATLAS_BLOCKS);
		if (tSprite == null) return new Baked(java.util.List.of(), ItemTransforms.NO_TRANSFORMS, false, false, null);
		// The rail is a flat icon, not full-3D (1.7.10's own ItemBlock never marked it so), giving it a flat, ground-lying
		// pose; a flat item is full-bright in the GUI too, matching vanilla's own default for such icons.
		return new Baked(flatQuads(tSprite, -1, true), flatItemTransforms(false), false, false, tSprite);
	}

	/** Material/MultiItem items: one flat icon per render pass, via getIcon(stack,pass) and getColorFromItemStack(stack,pass). */
	private static Baked buildFlatItem(ItemStack aStack, net.minecraft.world.item.Item aItem) {
		java.util.ArrayList<BakedQuad> tGui = new java.util.ArrayList<>();
		java.util.ArrayList<BakedQuad> tWorld = new java.util.ArrayList<>();
		TextureAtlasSprite tParticle = null;
		boolean tHasBars = false;
		int tPasses = itemRenderPasses(aItem, aStack);
		for (int tPass = 0; tPass < tPasses; tPass++) {
			ResourceLocation tIcon = iconForPass(aItem, aStack, tPass);
			if (tIcon == null) { if (tPass == 0) break; else continue; }
			TextureAtlasSprite tSprite = GT6QuadBuilder.resolveSprite(tIcon, GT6QuadBuilder.ATLAS_ITEMS);
			if (tSprite == null) tSprite = GT6QuadBuilder.resolveSprite(tIcon, GT6QuadBuilder.ATLAS_BLOCKS);
			if (tSprite == null) continue;
			int tColor = itemColor(aItem, aStack, tPass);
			// The bar-overlay flag comes from membership in the mod's own central icon registry, not from assuming 'the last
			// two passes are always bars' -- a material item's own tint pass would otherwise be caught by that assumption.
			boolean tBar = isBarOverlayIcon(tIcon);
			// The 1px outline gives a flat item its 'thickness'; skipped for bar overlays, since 1.7.10 drew those flat.
			List<BakedQuad> tQuads = flatQuads(tSprite, tColor, !tBar);
			tGui.addAll(tQuads);
			if (tBar) tHasBars = true; else tWorld.addAll(tQuads);
			if (tParticle == null) tParticle = tSprite;
		}
		// Hand/ground/frame pose is decided by the same channel 1.7.10 used for it: isFull3D().
		ItemTransforms tTransforms = flatItemTransforms(isFull3D(aItem));
		Baked rGui = new Baked(java.util.List.copyOf(tGui), tTransforms, false, false, tParticle);
		// Bar-overlay passes are skipped anywhere outside the GUI (in hand, on the ground, in a frame), 1:1 with 1.7.10.
		if (tHasBars) rGui.mWorldVariant = new Baked(java.util.List.copyOf(tWorld), tTransforms, false, false, tParticle);
		return rGui;
	}

	// Canonical vanilla model transforms, cached by model path and read from the engine once, right after bake.
	private static final java.util.Map<String, ItemTransforms> sVanillaTransforms = new java.util.concurrent.ConcurrentHashMap<>();
	private static final java.util.Set<String> sVanillaTransformsTried = java.util.concurrent.ConcurrentHashMap.newKeySet();
	/** The ItemTransforms of vanilla's own block/block model: its GUI display is the 30/225 isometric transform at 0.625 scale. */
	private static ItemTransforms blockGuiTransforms() {return vanillaTransforms("block/block");}

	/** Same channel as 1.7.10's Item.isFull3D(), asked via the mod's base item contract, not the tool hierarchy. */
	private static boolean isFull3D(net.minecraft.world.item.Item aItem) {
		return aItem instanceof gregapi.item.ItemBase tBase && tBase.isFull3D();
	}

	/** In 1.7.10 the engine itself picked between exactly two flat-item poses by isFull3D(); the carrier here is the
	 *  model's ItemTransforms, taken from vanilla's own two matching models rather than invented angles. */
	private static ItemTransforms flatItemTransforms(boolean aFull3D) {
		return vanillaTransforms(aFull3D ? "item/handheld" : "item/generated");
	}

	/** Reads a vanilla model's own ItemTransforms through the engine's public model-bakery getter, not a hardcoded
	 *  constant; that getter is open on this engine version, where a newer branch had to reach it by reflection instead. */
	private static ItemTransforms vanillaTransforms(String aModelPath) {
		ItemTransforms rCached = sVanillaTransforms.get(aModelPath);
		if (rCached != null || sVanillaTransformsTried.contains(aModelPath)) return rCached;
		sVanillaTransformsTried.add(aModelPath);
		try {
			net.minecraft.client.resources.model.ModelBakery tBakery = net.minecraft.client.Minecraft.getInstance().getModelManager().getModelBakery();
			net.minecraft.client.resources.model.UnbakedModel tModel = tBakery.getModel(new ResourceLocation("minecraft", aModelPath));
			if (tModel instanceof net.minecraft.client.renderer.block.model.BlockModel tBlockModel) {
				ItemTransforms tTr = tBlockModel.getTransforms();
				if (tTr != null) sVanillaTransforms.put(aModelPath, tTr);
				return tTr;
			}
		} catch (Throwable e) {/* Model unavailable, so it falls back to NO_TRANSFORMS. */}
		return null;
	}

	/** Item render pass count via PrefixItem.getRenderPasses(int)=2 when present, else 1 pass. */
	private static int itemRenderPasses(Object aItem, ItemStack aStack) {
		try { java.lang.reflect.Method m = cachedMethod(aItem.getClass(), "getRenderPasses", int.class); if (m != null) { Object r = m.invoke(aItem, (int)gregapi.util.ST.meta_(aStack)); if (r instanceof Integer ri && ri > 0) return Math.min(ri, 8); } } catch (Throwable e) {}
		return 1;
	}
	/** Per-pass item icon via GT6's getIcon(stack,pass), falling back to getIconIndex/getIconFromDamage for pass 0. */
	private static ResourceLocation iconForPass(Object aItem, ItemStack aStack, int aPass) {
		try { java.lang.reflect.Method m = cachedMethod(aItem.getClass(), "getIcon", ItemStack.class, int.class); if (m != null) { Object o = m.invoke(aItem, aStack, aPass); if (o instanceof ResourceLocation id) return id; } } catch (Throwable e) {}
		if (aPass == 0) { ResourceLocation r = tryIcon(aItem, "getIconIndex", ItemStack.class, aStack); if (r == null) r = tryIcon(aItem, "getIconFromDamage", int.class, aStack.getDamageValue()); return r; }
		return null;
	}
	/** GT6's getColorFromItemStack(stack,pass) returns 0xRRGGBB: material tint on pass 0, white on every other pass. */
	private static int itemColor(Object aItem, ItemStack aStack, int aPass) {
		try { java.lang.reflect.Method m = cachedMethod(aItem.getClass(), "getColorFromItemStack", ItemStack.class, int.class); if (m != null) { Object c = m.invoke(aItem, aStack, aPass); if (c instanceof Integer ci) return ci; } } catch (Throwable e) {}
		return 0xFFFFFF;
	}

	// The mod's central set of bar-overlay icons, read from the same texture registry those icons already live in,
	// not a hardcoded list; cached lazily since icons only resolve once the atlas has finished baking.
	private static java.util.Set<ResourceLocation> sBarOverlayIcons;
	private static java.util.Set<ResourceLocation> barOverlayIcons() {
		if (sBarOverlayIcons != null) return sBarOverlayIcons;
		java.util.HashSet<ResourceLocation> tSet = new java.util.HashSet<>();
		try {
			for (gregapi.render.IIconContainer c : gregapi.old.Textures.ItemIcons.DURABILITY_BAR) { ResourceLocation i = c.getIcon(0); if (i != null) tSet.add(i); }
			for (gregapi.render.IIconContainer c : gregapi.old.Textures.ItemIcons.ENERGY_BAR)     { ResourceLocation i = c.getIcon(0); if (i != null) tSet.add(i); }
		} catch (Throwable e) {}
		if (!tSet.isEmpty()) sBarOverlayIcons = tSet;
		return tSet;
	}
	/** Is this pass icon a GUI-only durability/charge bar overlay? Decided by membership in the central icon registry. */
	private static boolean isBarOverlayIcon(ResourceLocation aIcon) { return aIcon != null && barOverlayIcons().contains(aIcon); }

	/** Resolves an item's icon through GT6's own channel; kept public for a render probe to check for missing icons. */
	public static ResourceLocation resolveIcon(ItemStack aItem) {
		Object tItem = aItem.getItem();
		ResourceLocation r = tryIcon(tItem, "getIconIndex", ItemStack.class, aItem);
		if (r == null) r = tryIcon(tItem, "getIconFromDamage", int.class, aItem.getDamageValue());
		return r;
	}

	private static ResourceLocation tryIcon(Object aTarget, String aMethod, Class<?> aArgType, Object aArg) {
		try {
			java.lang.reflect.Method m = cachedMethod(aTarget.getClass(), aMethod, aArgType);
			if (m == null) return null;
			Object o = m.invoke(aTarget, aArg);
			return o instanceof ResourceLocation tId ? tId : null;
		} catch (Throwable ignored) {return null;}
	}

	/** A flat 16x16 item face at z=8/16 from a sprite, front (+Z) or back (-Z), tinted by aColor (0xRRGGBB). */
	private static BakedQuad flatFace(TextureAtlasSprite aSprite, boolean aFront, int aColor) {
		int r=(aColor>>16)&0xFF, g=(aColor>>8)&0xFF, b8=aColor&0xFF;
		Direction tDir = aFront ? Direction.SOUTH : Direction.NORTH;
		float z = aFront ? 8.5f/16f : 7.5f/16f; // front/back are 1px apart, as ItemModelGenerator does; at z=0.5 both would z-fight and show the dark back face
		float[][] c = aFront
			? new float[][]{{0,0,z, 0,16},{0,1,z, 0,0},{1,1,z, 16,0},{1,0,z, 16,16}}
			: new float[][]{{1,0,z, 16,16},{1,1,z, 16,0},{0,1,z, 0,0},{0,0,z, 0,16}};
		org.joml.Vector3f n = tDir.step();
		QuadBakingVertexConsumer.Buffered b = new QuadBakingVertexConsumer.Buffered();
		b.setSprite(aSprite);
		b.setDirection(tDir);
		b.setTintIndex(-1); // Material tint is already baked into the vertices; a default tintIndex would make the engine multiply it in again.
		b.setShade(false);  // A flat item gets even, front-lit light, matching vanilla's own convention for such icons, with no directional shading.
		// Same canonical vertex numbering as block faces: one order for the whole mod, see GT6QuadBuilder.EMIT_ORDER.
		for (int idx = 0; idx < 4; idx++) {
			final int i = GT6QuadBuilder.EMIT_ORDER[idx];
			b.vertex(c[i][0], c[i][1], c[i][2]);
			b.color(r, g, b8, 255); // material tint (a white-swatch probe confirmed the color path works; the root cause was lighting)
			b.normal(n.x(), n.y(), n.z());
			b.uv(aSprite.getU(c[i][3]), aSprite.getV(c[i][4])); // 1.20.1's own getU/getV already divide by 16 (the 1.7.10 texture scale) internally.
			b.endVertex();
		}
		return b.getQuad();
	}

	// A literal transcription of the engine's own flat-item side-face algorithm: a 1px face is built along every
	// silhouette edge, the same 'thickness' that makes a flat item look real; the scan result is cached per sprite.

	/** The engine's own side-direction mapping (up/down/left/right to up/down/east/west), taken as canon, not reinvented. */
	private static final Direction[] SIDE_DIRS = {Direction.UP, Direction.DOWN, Direction.EAST, Direction.WEST};
	/** Pixel-scan cache: sprite name to face list {dirIdx,x,y}, unioned across all animation frames like vanilla does. */
	private static final java.util.concurrent.ConcurrentHashMap<String, int[][]> sSideFaceCache = new java.util.concurrent.ConcurrentHashMap<>();

	private static void addSideQuads(List<BakedQuad> aOut, TextureAtlasSprite aSprite, int aColor) {
		int[][] tFaces = sideFacesOf(aSprite);
		net.minecraft.client.renderer.texture.SpriteContents tC = aSprite.contents();
		float tXScale = 16.0F / tC.width(), tYScale = 16.0F / tC.height(); // bakeSideFaces:117-118
		for (int[] tFace : tFaces) {
			int tDir = tFace[0]; float x = tFace[1], y = tFace[2];
			// UV per bakeSideFaces: inset 0.1px from pixel edges, with V flipped on vertical faces.
			float u0 = x + 0.1F, u1 = x + 1.0F - 0.1F, v0, v1;
			if (tDir <= 1) {v0 = y + 0.1F; v1 = y + 1.0F - 0.1F;} else {v0 = y + 1.0F - 0.1F; v1 = y + 0.1F;} // isHorizontal = UP|DOWN
			// Geometry per bakeSideFaces: pixel-row bounds scaled and flipped from texture-space Y-down into model-space Y-up.
			float tStartX = x, tStartY = y, tEndX = x, tEndY = y;
			switch (tDir) {
				case 0: tEndX = x + 1.0F; break;                                    // UP
				case 1: tEndX = x + 1.0F; tStartY = y + 1.0F; tEndY = y + 1.0F; break; // DOWN
				case 2: tEndY = y + 1.0F; break;                                    // LEFT (EAST)
				default: tStartX = x + 1.0F; tEndX = x + 1.0F; tEndY = y + 1.0F;    // RIGHT (WEST)
			}
			tStartX *= tXScale; tEndX *= tXScale; tStartY *= tYScale; tEndY *= tYScale;
			tStartY = 16.0F - tStartY; tEndY = 16.0F - tEndY;
			float[] tFrom, tTo;
			switch (tDir) {
				case 0:  tFrom = new float[]{tStartX, tStartY, 7.5F}; tTo = new float[]{tEndX,   tStartY, 8.5F}; break; // UP
				case 1:  tFrom = new float[]{tStartX, tEndY,   7.5F}; tTo = new float[]{tEndX,   tEndY,   8.5F}; break; // DOWN
				case 2:  tFrom = new float[]{tStartX, tStartY, 7.5F}; tTo = new float[]{tStartX, tEndY,   8.5F}; break; // LEFT
				default: tFrom = new float[]{tEndX,   tStartY, 7.5F}; tTo = new float[]{tEndX,   tEndY,   8.5F}; break; // RIGHT
			}
			aOut.add(sideQuad(aSprite, SIDE_DIRS[tDir], tFrom, tTo, u0 * tXScale, v0 * tYScale, u1 * tXScale, v1 * tYScale, aColor));
		}
	}

	/** The pixel-contour scan for the side outline above, cached per sprite since sprites don't change. */
	private static int[][] sideFacesOf(TextureAtlasSprite aSprite) {
		net.minecraft.client.renderer.texture.SpriteContents tC = aSprite.contents();
		String tKey = tC.name().toString();
		int[][] tCached = sSideFaceCache.get(tKey);
		if (tCached != null) return tCached;
		java.util.LinkedHashSet<Integer> tSet = new java.util.LinkedHashSet<>();
		try {
			int w = tC.width(), h = tC.height();
			int[] tFrames = tC.getUniqueFrames().toArray(); // 1.20.1's own getUniqueFrames returns an IntStream here, not the older array form.
			for (int f = 0; f < tFrames.length; f++) {
				int tFrame = tFrames[f];
				for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) {
					if (sideTransparent(tC, tFrame, x, y, w, h)) continue;
					// checkTransition: a transparent neighbor at (x-stepX, y-stepY) means a face is needed there.
					if (sideTransparent(tC, tFrame, x,     y - 1, w, h)) tSet.add(sideKey(0, x, y)); // UP
					if (sideTransparent(tC, tFrame, x,     y + 1, w, h)) tSet.add(sideKey(1, x, y)); // DOWN
					if (sideTransparent(tC, tFrame, x - 1, y,     w, h)) tSet.add(sideKey(2, x, y)); // LEFT (EAST)
					if (sideTransparent(tC, tFrame, x + 1, y,     w, h)) tSet.add(sideKey(3, x, y)); // RIGHT (WEST)
				}
			}
		} catch (Throwable e) {tSet.clear();} // pixels unavailable -> item stays without an outline (front/back are intact)
		int[][] rFaces = new int[tSet.size()][]; int i = 0;
		for (int tKey2 : tSet) rFaces[i++] = new int[]{tKey2 >>> 28, (tKey2 >>> 14) & 0x3FFF, tKey2 & 0x3FFF};
		sSideFaceCache.put(tKey, rFaces);
		return rFaces;
	}
	private static int sideKey(int aDir, int aX, int aY) {return (aDir << 28) | (aX << 14) | aY;}
	private static boolean sideTransparent(net.minecraft.client.renderer.texture.SpriteContents aC, int aFrame, int aX, int aY, int aW, int aH) {
		return aX < 0 || aY < 0 || aX >= aW || aY >= aH || aC.isTransparent(aFrame, aX, aY); // isTransparent treats anything outside the sprite's own bounds as transparent too.
	}

	/** Follows the engine's own FaceInfo vertex order for a side quad; checked to give the same winding as the reversed
	 *  order flatFace uses elsewhere, so both stay consistent. */
	private static BakedQuad sideQuad(TextureAtlasSprite aSprite, Direction aDir, float[] aFrom, float[] aTo, float aMinU, float aMinV, float aMaxU, float aMaxV, int aColor) {
		int r = (aColor >> 16) & 0xFF, g = (aColor >> 8) & 0xFF, b8 = aColor & 0xFF;
		// FaceInfo: from/to selector per axis for each of a face's 4 vertices (1=to, 0=from).
		int[][] tSel;
		switch (aDir) {
			case UP:   tSel = new int[][]{{0,1,0},{0,1,1},{1,1,1},{1,1,0}}; break;
			case DOWN: tSel = new int[][]{{0,0,1},{0,0,0},{1,0,0},{1,0,1}}; break;
			case WEST: tSel = new int[][]{{0,1,0},{0,0,0},{0,0,1},{0,1,1}}; break;
			default:   tSel = new int[][]{{1,1,1},{1,0,1},{1,0,0},{1,1,0}}; break; // EAST
		}
		org.joml.Vector3f n = aDir.step();
		QuadBakingVertexConsumer.Buffered b = new QuadBakingVertexConsumer.Buffered();
		b.setSprite(aSprite);
		b.setDirection(aDir);
		b.setTintIndex(-1);
		b.setShade(false); // Same as flatFace: one uniform brightness for the whole flat-item model.
		for (int i = 0; i < 4; i++) {
			b.vertex((tSel[i][0] == 1 ? aTo[0] : aFrom[0]) / 16f, (tSel[i][1] == 1 ? aTo[1] : aFrom[1]) / 16f, (tSel[i][2] == 1 ? aTo[2] : aFrom[2]) / 16f);
			b.color(r, g, b8, 255);
			b.normal(n.x(), n.y(), n.z());
			b.uv(aSprite.getU(i == 0 || i == 1 ? aMinU : aMaxU), aSprite.getV(i == 0 || i == 3 ? aMinV : aMaxV)); // 1.20.1's own getU/getV already divide by 16 internally, same as the earlier note above.
			b.endVertex();
		}
		return b.getQuad();
	}
}

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

import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.model.pipeline.QuadBakingVertexConsumer;

/** A single dynamic item model for every GT6 item, mirroring {@link GT6BlockModel}'s centralization: it resolves
 *  the per-meta icon by reflection, since no single interface spans MultiItem/PrefixItem/ItemBlock. */
public class GT6ItemModel implements ItemModel {

	// ================================================================================================================
	// Item quad geometry is a pure function of (sprite, tint, outline) or (block, meta, components), so it's cached
	// globally instead of rebuilt every frame; BakedQuad is immutable and safe to share across frames and layers.
	// ================================================================================================================
	private record FlatKey(TextureAtlasSprite mSprite, int mColor, boolean mSides) {}
	private record InvKey(net.minecraft.world.level.block.Block mBlock, short mMeta, net.minecraft.core.component.DataComponentPatch mComponents) {}
	private static final java.util.concurrent.ConcurrentHashMap<FlatKey, List<BakedQuad>> sFlatCache = new java.util.concurrent.ConcurrentHashMap<>();
	private static final java.util.concurrent.ConcurrentHashMap<InvKey, List<BakedQuad>> sInvCache = new java.util.concurrent.ConcurrentHashMap<>();

	/** Cache reset, called from onModifyBakingResult on every model/atlas rebake. */
	public static void invalidateCaches() {sFlatCache.clear(); sInvCache.clear();}

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

	@Override
	public void update(ItemStackRenderState aOutput, ItemStack aItem, ItemModelResolver aResolver, ItemDisplayContext aCtx, net.minecraft.client.multiplayer.ClientLevel aLevel, net.minecraft.world.entity.ItemOwner aOwner, int aSeed) {
		aOutput.appendModelIdentityElement(this);
		// The GUI caches item pixels by getModelIdentity(), which per Mojang's own canon must include everything the
		// pixels depend on; without per-pass sprite/tint/foil here, NBT-differentiated tools would collide in one slot.
		aOutput.appendModelIdentityElement(aItem.getItem());
		aOutput.appendModelIdentityElement((int) gregapi.util.ST.meta_(aItem));
		try {
			net.minecraft.world.item.Item tItem = aItem.getItem();
			// Central item-render entry point, reproducing RendererBlockTextured.renderInventoryBlock: block items get 3D
			// geometry via buildInventoryQuads, while material/MultiItem items get flat per-pass icons with per-pass tint.
			if (tItem instanceof net.minecraft.world.item.BlockItem tRailBI && tRailBI.getBlock() instanceof gregapi.block.misc.BlockBaseRail tRail) {
				// The rail is a block-item without IRenderedBlock, so the flat-icon path can't resolve it; its straight icon
				// (meta 0) is drawn directly instead, like the vanilla rail item.
				renderRailItem(aOutput, tRail, aCtx);
			} else if (tItem instanceof net.minecraft.world.item.BlockItem tPlantBI && tPlantBI.getBlock() instanceof IRenderedCross) {
				// Saplings/flowers use their flat icon as items, as in 1.7.10; crossed planes are world geometry only.
				renderFlatItem(aOutput, aItem, tItem, aCtx);
			} else if (tItem instanceof net.minecraft.world.item.BlockItem tBI && tBI.getBlock() instanceof IRenderedBlock) {
				renderBlockInventory(aOutput, aItem, tBI.getBlock(), aCtx);
			} else {
				renderFlatItem(aOutput, aItem, tItem, aCtx);
			}
		} catch (Throwable e) {/* render-safe: one item failing must not break the whole render */}
	}

	/** Item-form of a block: 3D geometry in the inventory via {@link GT6BlockModel#buildInventoryQuads}. */
	private static void renderBlockInventory(ItemStackRenderState aOutput, ItemStack aStack, net.minecraft.world.level.block.Block aBlock, net.minecraft.world.item.ItemDisplayContext aCtx) {
		// 3D block-item shape is a pure function of (block, meta, stack components), cached globally and built only on a miss.
		if (sInvCache.size() > 16384) sInvCache.clear();
		List<BakedQuad> tBuilt = sInvCache.computeIfAbsent(new InvKey(aBlock, gregapi.util.ST.meta_(aStack), aStack.getComponentsPatch()), aKey -> {
			GT6QuadBuilder tQB = new GT6QuadBuilder();
			try { GT6BlockModel.buildInventoryQuads(tQB, aBlock, aStack); } catch (Throwable e) {}
			return java.util.List.copyOf(tQB.quads());
		});
		if (tBuilt.isEmpty()) {
			// 1.7.10's TESR item-form render (Chest/MassStorage) used a special renderer directly; the neo carrier is a
			// special-model layer that dispatches to the same registered in-world renderer by class.
			net.minecraft.world.level.block.entity.BlockEntity tArg = MultiTileEntityBER.SPECIAL_ITEM_FORM.extractArgument(aStack);
			if (tArg != null) {
				aOutput.appendModelIdentityElement("mte-special:" + tArg.getClass().getName());
				ItemStackRenderState.LayerRenderState tSpLayer = aOutput.newLayer();
				net.minecraft.client.resources.model.cuboid.ItemTransforms tSpTr = blockGuiTransforms();
				if (tSpTr != null) tSpLayer.setItemTransform(tSpTr.getTransform(aCtx));
				tSpLayer.setupSpecialModel(MultiTileEntityBER.SPECIAL_ITEM_FORM, tArg);
				aOutput.setAnimated(); // per-frame special render (lid/contents) is not for the GUI atlas cache
			}
			return;
		}
		// Per-quad sprite names go into the model identity (same canon as renderFlatItem), since otherwise stacks
		// with the same item+meta but a different look would share one cache slot.
		java.util.TreeSet<String> tIdSpr = new java.util.TreeSet<>();
		boolean tAnimated = false;
		int tColorHash = 1; // Player report 'all coins are the same color in creative/JEI': coin color comes from a vertex tint, not the sprite.
		// Coins of different materials use identical sprites, so identity built from sprites alone merged them into one
		// GuiItemAtlas cache slot; mixing in the quads' vertex colors into the identity fixes it.
		// The shape goes in too: a battery's charge bar is a box whose height follows its charge, with the same sprites
		// (MultiTileEntityBatteryAdvEU*.java:55), so stacks of other charges shared one atlas slot and showed stale bars.
		int tShapeHash = 1;
		for (BakedQuad q : tBuilt) try {
			tIdSpr.add(q.materialInfo().sprite().contents().name().toString());
			if (q.materialInfo().sprite().contents().isAnimated()) tAnimated = true;
			for (int v = 0; v < 4; v++) {
				tColorHash = 31 * tColorHash + q.bakedColors().color(v);
				org.joml.Vector3fc p = q.position(v);
				tShapeHash = 31 * (31 * (31 * tShapeHash + Float.floatToIntBits(p.x())) + Float.floatToIntBits(p.y())) + Float.floatToIntBits(p.z());
			}
		} catch (Throwable e) {}
		for (String s : tIdSpr) aOutput.appendModelIdentityElement(s);
		aOutput.appendModelIdentityElement(tColorHash);
		aOutput.appendModelIdentityElement(tShapeHash);
		// Per Mojang's canon (CuboidItemModelWrapper.update): an animated face sprite requires setAnimated,
		// or the GUI atlas caches it as a static frame.
		if (tAnimated) aOutput.setAnimated();
		ItemStackRenderState.LayerRenderState tLayer = aOutput.newLayer();
		// Without a display transform neo draws the block-item cube frontally (one dark face), since in 1.7.10 the engine
		// itself applied that isometry; the canonical block-GUI transform is read live from the engine, not hardcoded.
		net.minecraft.client.resources.model.cuboid.ItemTransforms tTr = blockGuiTransforms();
		if (tTr != null) tLayer.setItemTransform(tTr.getTransform(aCtx));
		if (aStack.hasFoil()) {
			tLayer.setFoilType(ItemStackRenderState.FoilType.STANDARD);
			aOutput.setAnimated();
			aOutput.appendModelIdentityElement(ItemStackRenderState.FoilType.STANDARD);
		}
		tLayer.prepareQuadList().addAll(tBuilt);
		tLayer.setUsesBlockLight(true);
		try { tLayer.setParticleMaterial(new Material.Baked(tBuilt.get(0).materialInfo().sprite(), false)); } catch (Throwable e) {}
	}

	/** Rail inventory icon is the flat straight sprite (meta 0), resolved from the BLOCKS atlas since rail icons
	 *  live there rather than in ITEMS, reusing the same flat front/back/outline geometry as other flat items. */
	private static void renderRailItem(ItemStackRenderState aOutput, gregapi.block.misc.BlockBaseRail aRail, ItemDisplayContext aCtx) {
		Identifier tIcon = aRail.getIcon(0, 0);
		if (tIcon == null) return;
		TextureAtlasSprite tSprite = GT6QuadBuilder.resolveSprite(tIcon, net.minecraft.data.AtlasIds.ITEMS);
		if (tSprite == null) tSprite = GT6QuadBuilder.resolveSprite(tIcon, net.minecraft.data.AtlasIds.BLOCKS);
		if (tSprite == null) return;
		aOutput.appendModelIdentityElement(tSprite.contents().name());
		if (tSprite.contents().isAnimated()) aOutput.setAnimated();
		ItemStackRenderState.LayerRenderState tLayer = aOutput.newLayer();
		// The rail icon is deliberately not full3D, since its 1.7.10 ItemBlock never called setFull3D either,
		// so it lies flat like the vanilla item.
		net.minecraft.client.resources.model.cuboid.ItemTransforms tRailTr = flatItemTransforms(false);
		if (tRailTr != null) tLayer.setItemTransform(tRailTr.getTransform(aCtx));
		tLayer.setUsesBlockLight(false); // flat item is full-bright (per ItemModelGenerator/GuiLight.FRONT canon)
		tLayer.prepareQuadList().addAll(flatQuads(tSprite, -1, true)); // geometry from cache
		tLayer.setParticleMaterial(new Material.Baked(tSprite, false));
	}

	/** Material/MultiItem items: one flat icon per render pass, via getIcon(stack,pass) and getColorFromItemStack(stack,pass). */
	private static void renderFlatItem(ItemStackRenderState aOutput, ItemStack aStack, net.minecraft.world.item.Item aItem, ItemDisplayContext aCtx) {
		// 1.7.10's engine itself skipped the durability/charge bar overlay passes outside GUI context; this adapter
		// reproduces it centrally, identifying bar passes by their icon from the central bar registry, not by index.
		boolean tSkipBars = (aCtx != ItemDisplayContext.GUI);
		int tPasses = itemRenderPasses(aItem, aStack);
		for (int tPass = 0; tPass < tPasses; tPass++) {
			Identifier tIcon = iconForPass(aItem, aStack, tPass);
			if (tIcon == null) { if (tPass == 0) return; else continue; }
			if (tSkipBars && isBarOverlayIcon(tIcon)) continue; // GUI-only durability/charge overlay, skipped in-world (hand/ground/frame)
			TextureAtlasSprite tSprite = GT6QuadBuilder.resolveSprite(tIcon, net.minecraft.data.AtlasIds.ITEMS);
			if (tSprite == null) tSprite = GT6QuadBuilder.resolveSprite(tIcon, net.minecraft.data.AtlasIds.BLOCKS);
			if (tSprite == null) continue;
			int tColor = itemColor(aItem, aStack, tPass);
			// Sprite and tint both go into the model identity per canon; otherwise tools sharing the same meta but
			// different NBT material would collide in the GuiItemAtlas cache.
			aOutput.appendModelIdentityElement(tSprite.contents().name());
			aOutput.appendModelIdentityElement(tColor);
			// Animated sprites (bees, fluids) need setAnimated, or the GUI atlas draws the slot once and freezes it.
			if (tSprite.contents().isAnimated()) aOutput.setAnimated();
			ItemStackRenderState.LayerRenderState tLayer = aOutput.newLayer();
			// Without a display transform every context renders identically; the difference channel is the same
			// as 1.7.10's isFull3D(), which picks a handheld vs. flat transform.
			net.minecraft.client.resources.model.cuboid.ItemTransforms tFlatTr = flatItemTransforms(isFull3D(aItem));
			if (tFlatTr != null) tLayer.setItemTransform(tFlatTr.getTransform(aCtx));
			tLayer.setUsesBlockLight(false); // reference ItemModelGenerator=GuiLight.FRONT: a flat GUI item is full-bright, or the layer gets block-shaded
			if (aStack.hasFoil()) { // 1:1: 1.7.10 draws the glint by hasEffect over the passes; neo canon is FoilType plus identity plus animated.
				tLayer.setFoilType(ItemStackRenderState.FoilType.STANDARD);
				aOutput.setAnimated();
				aOutput.appendModelIdentityElement(ItemStackRenderState.FoilType.STANDARD);
			}
			// The 'thickness' is a 1px silhouette outline (bar overlays never get one, since 1.7.10 drew those flat);
			// geometry is a pure function of (sprite, tint, outline), so it's cached.
			tLayer.prepareQuadList().addAll(flatQuads(tSprite, tColor, !isBarOverlayIcon(tIcon)));
			tLayer.setParticleMaterial(new Material.Baked(tSprite, false));
		}
	}

	// Canonical vanilla model transforms, cached by model path and read from the engine once, right after bake.
	private static final java.util.Map<String, net.minecraft.client.resources.model.cuboid.ItemTransforms> sVanillaTransforms = new java.util.concurrent.ConcurrentHashMap<>();
	private static final java.util.Set<String> sVanillaTransformsTried = java.util.concurrent.ConcurrentHashMap.newKeySet();
	/** Reads the vanilla block/block model's GUI transform (isometry 30/225, scale 0.625) live from the engine's
	 *  ResolvedModel instead of hardcoding it, via reflection since there's no public getter (same idiom as iconForPass). */
	private static net.minecraft.client.resources.model.cuboid.ItemTransforms blockGuiTransforms() {return vanillaTransforms("block/block");}

	/** Same channel as 1.7.10's Item.isFull3D(), asked via the mod's base item contract, not the tool hierarchy. */
	private static boolean isFull3D(net.minecraft.world.item.Item aItem) {
		return aItem instanceof gregapi.item.ItemBase tBase && tBase.isFull3D();
	}

	/** Flat item position had exactly two cases in 1.7.10, chosen by isFull3D(); in neo that same distinction lives in
	 *  the vanilla item/handheld and item/generated model transforms, read from the engine rather than hardcoded. */
	private static net.minecraft.client.resources.model.cuboid.ItemTransforms flatItemTransforms(boolean aFull3D) {
		return vanillaTransforms(aFull3D ? "item/handheld" : "item/generated");
	}

	/** Reads a vanilla model's ItemTransforms by path from the engine's ResolvedModel via reflection, since
	 *  there's no public getter and the values shouldn't be hardcoded. */
	private static net.minecraft.client.resources.model.cuboid.ItemTransforms vanillaTransforms(String aModelPath) {
		net.minecraft.client.resources.model.cuboid.ItemTransforms rCached = sVanillaTransforms.get(aModelPath);
		if (rCached != null || sVanillaTransformsTried.contains(aModelPath)) return rCached;
		sVanillaTransformsTried.add(aModelPath);
		try {
			net.minecraft.client.resources.model.ModelBakery tBakery = net.minecraft.client.Minecraft.getInstance().getModelManager().getModelBakery();
			java.lang.reflect.Field tF = net.minecraft.client.resources.model.ModelBakery.class.getDeclaredField("resolvedModels");
			tF.setAccessible(true);
			@SuppressWarnings("unchecked")
			java.util.Map<net.minecraft.resources.Identifier, net.minecraft.client.resources.model.ResolvedModel> tResolved =
				(java.util.Map<net.minecraft.resources.Identifier, net.minecraft.client.resources.model.ResolvedModel>) tF.get(tBakery);
			net.minecraft.client.resources.model.ResolvedModel tModel = tResolved.get(net.minecraft.resources.Identifier.withDefaultNamespace(aModelPath));
			if (tModel != null) {
				net.minecraft.client.resources.model.cuboid.ItemTransforms tTr = tModel.getTopTransforms();
				if (tTr != null) sVanillaTransforms.put(aModelPath, tTr);
				return tTr;
			}
		} catch (Throwable e) {/* model unavailable -> fallback NO_TRANSFORM */}
		return null;
	}

	/** Item render pass count via PrefixItem.getRenderPasses(int)=2 when present, else 1 pass. */
	private static int itemRenderPasses(Object aItem, ItemStack aStack) {
		try { java.lang.reflect.Method m = cachedMethod(aItem.getClass(), "getRenderPasses", int.class); if (m != null) { Object r = m.invoke(aItem, (int)gregapi.util.ST.meta_(aStack)); if (r instanceof Integer ri && ri > 0) return Math.min(ri, 8); } } catch (Throwable e) {}
		return 1;
	}
	/** Per-pass item icon via GT6's getIcon(stack,pass), falling back to getIconIndex/getIconFromDamage for pass 0. */
	private static Identifier iconForPass(Object aItem, ItemStack aStack, int aPass) {
		if (aItem instanceof net.minecraft.world.item.BlockItem tBI && tBI.getBlock() instanceof IRenderedCross tCross) {
			return aPass == 0 ? tCross.getCrossIcon(null, gregapi.util.ST.meta_(aStack), 0, 0) : null;
		}
		try { java.lang.reflect.Method m = cachedMethod(aItem.getClass(), "getIcon", ItemStack.class, int.class); if (m != null) { Object o = m.invoke(aItem, aStack, aPass); if (o instanceof Identifier id) return id; } } catch (Throwable e) {}
		if (aPass == 0) { Identifier r = tryIcon(aItem, "getIconIndex", ItemStack.class, aStack); if (r == null) r = tryIcon(aItem, "getIconFromDamage", int.class, aStack.getDamageValue()); return r; }
		return null;
	}
	/** GT6's getColorFromItemStack(stack,pass) returns 0xRRGGBB: material tint on pass 0, white on every other pass. */
	private static int itemColor(Object aItem, ItemStack aStack, int aPass) {
		if (aItem instanceof net.minecraft.world.item.BlockItem tBI && tBI.getBlock() instanceof IRenderedCross tCross) {
			short[] tRGBa = tCross.getCrossRGBa(null, gregapi.util.ST.meta_(aStack), 0, 0);
			return tRGBa == null ? 0xFFFFFF : (tRGBa[0] << 16) | (tRGBa[1] << 8) | tRGBa[2];
		}
		try { java.lang.reflect.Method m = cachedMethod(aItem.getClass(), "getColorFromItemStack", ItemStack.class, int.class); if (m != null) { Object c = m.invoke(aItem, aStack, aPass); if (c instanceof Integer ci) return ci; } } catch (Throwable e) {}
		return 0xFFFFFF;
	}

	// The set of bar-overlay icons comes from the mod's own central texture registry, not hardcoded strings or
	// pass-index guessing; it's built lazily since icons only resolve after the atlas bake, caching only a fully-resolved set.
	private static java.util.Set<Identifier> sBarOverlayIcons;
	private static java.util.Set<Identifier> barOverlayIcons() {
		if (sBarOverlayIcons != null) return sBarOverlayIcons;
		java.util.HashSet<Identifier> tSet = new java.util.HashSet<>();
		try {
			for (gregapi.render.IIconContainer c : gregapi.old.Textures.ItemIcons.DURABILITY_BAR) { Identifier i = c.getIcon(0); if (i != null) tSet.add(i); }
			for (gregapi.render.IIconContainer c : gregapi.old.Textures.ItemIcons.ENERGY_BAR)     { Identifier i = c.getIcon(0); if (i != null) tSet.add(i); }
		} catch (Throwable e) {}
		if (!tSet.isEmpty()) sBarOverlayIcons = tSet;
		return tSet;
	}
	/** Is this pass icon a GUI-only durability/charge bar overlay? Decided by membership in the central icon registry. */
	private static boolean isBarOverlayIcon(Identifier aIcon) { return aIcon != null && barOverlayIcons().contains(aIcon); }

	/** Resolves an item's icon via GT6's getIconIndex(ItemStack) or getIconFromDamage(int); public since the
	 *  render probe rig reuses it for its 'no purple icons' check. */
	public static Identifier resolveIcon(ItemStack aItem) {
		Object tItem = aItem.getItem();
		Identifier r = tryIcon(tItem, "getIconIndex", ItemStack.class, aItem);
		if (r == null) r = tryIcon(tItem, "getIconFromDamage", int.class, aItem.getDamageValue());
		return r;
	}

	private static Identifier tryIcon(Object aTarget, String aMethod, Class<?> aArgType, Object aArg) {
		try {
			java.lang.reflect.Method m = cachedMethod(aTarget.getClass(), aMethod, aArgType);
			if (m == null) return null;
			Object o = m.invoke(aTarget, aArg);
			return o instanceof Identifier tId ? tId : null;
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
		net.minecraft.world.phys.Vec3 n = tDir.getUnitVec3();
		QuadBakingVertexConsumer b = new QuadBakingVertexConsumer();
		b.setSprite(new Material.Baked(aSprite, false));
		b.setDirection(tDir);
		b.setLightEmission(15); // full-bright: emission forced to 15, since GUI otherwise gives dark lightCoords like the reference flat item.
		// Same canonical vertex numbering as block faces: one order for the whole mod, see GT6QuadBuilder.EMIT_ORDER.
		for (int idx = 0; idx < 4; idx++) {
			final int i = GT6QuadBuilder.EMIT_ORDER[idx];
			b.addVertex(c[i][0], c[i][1], c[i][2]);
			b.setColor(r, g, b8, 255); // material tint (a white-swatch probe confirmed the color path works; the root cause was lighting)
			b.setNormal((float)n.x, (float)n.y, (float)n.z);
			b.setUv(aSprite.getU(c[i][3] / 16f), aSprite.getV(c[i][4] / 16f));
		}
		return b.bakeQuad();
	}

	// Verbatim transcription of the engine's ItemModelGenerator.bakeSideFaces/getSideFaces/checkTransition/isTransparent:
	// a 1px outline is built per opaque pixel with a transparent neighbor, cached by sprite since it reruns every frame.

	/** Canon SideDirection mapping: UP->Direction.UP, DOWN->DOWN, LEFT->EAST, RIGHT->WEST. */
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

	/** Pixel-contour scan (getSideFaces/checkTransition/isTransparent), cached by sprite. */
	private static int[][] sideFacesOf(TextureAtlasSprite aSprite) {
		net.minecraft.client.renderer.texture.SpriteContents tC = aSprite.contents();
		String tKey = tC.name().toString();
		int[][] tCached = sSideFaceCache.get(tKey);
		if (tCached != null) return tCached;
		java.util.LinkedHashSet<Integer> tSet = new java.util.LinkedHashSet<>();
		try {
			int w = tC.width(), h = tC.height();
			it.unimi.dsi.fastutil.ints.IntList tFrames = tC.getUniqueFrames();
			for (int f = 0; f < tFrames.size(); f++) {
				int tFrame = tFrames.getInt(f);
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
		return aX < 0 || aY < 0 || aX >= aW || aY >= aH || aC.isTransparent(aFrame, aX, aY); // isTransparent: outside the sprite counts as transparent
	}

	/** Side quad follows the FaceInfo vertex canon with UV picked per vertex index; this ordering gives the
	 *  same winding as the reversed order in flatFace. */
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
		net.minecraft.world.phys.Vec3 n = aDir.getUnitVec3();
		QuadBakingVertexConsumer b = new QuadBakingVertexConsumer();
		b.setSprite(new Material.Baked(aSprite, false));
		b.setDirection(aDir);
		b.setLightEmission(15); // same as flatFace: full-bright, uniform brightness for the flat item model
		for (int i = 0; i < 4; i++) {
			b.addVertex((tSel[i][0] == 1 ? aTo[0] : aFrom[0]) / 16f, (tSel[i][1] == 1 ? aTo[1] : aFrom[1]) / 16f, (tSel[i][2] == 1 ? aTo[2] : aFrom[2]) / 16f);
			b.setColor(r, g, b8, 255);
			b.setNormal((float)n.x, (float)n.y, (float)n.z);
			b.setUv(aSprite.getU((i == 0 || i == 1 ? aMinU : aMaxU) / 16f), aSprite.getV((i == 0 || i == 3 ? aMinV : aMaxV) / 16f));
		}
		return b.bakeQuad();
	}
}

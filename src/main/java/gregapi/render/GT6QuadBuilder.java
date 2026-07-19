/**
 * Copyright (c) 2019 Gregorius Techneticies
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

/**
 * F3-render (client): «захватывающий рендерер» — объект {@code aRenderer}, который передаётся в GT6-цепочку
 * {@code ITexture.renderXPos(aRenderer,...) -> ITexture.Util.renderSide(side, Identifier, RGBa, ..., aRenderer, ...)}.
 * Вместо immediate-mode отрисовки (1.7.10 Tessellator, удалён) он АККУМУЛИРУЕТ per-side full-cube {@link BakedQuad}
 * для декларативной модели {@link GT6BlockModel}. Так GT6 per-side texture-логика (какая иконка/цвет на сторону —
 * решает сам ITexture/тайл) переиспользуется 1:1; переписан только «нарисуй сейчас» → «дай мне quad».
 * Спрайты резолвятся из атласа блоков в рантайме (текстуры GT6 динамические: материал×префикс, нельзя пре-bake).
 * Мост в {@link ITexture.Util}: если {@code aRenderer instanceof GT6QuadBuilder} → {@code putFace(side, icon, RGBa)}.
 * См. decisions/F3-render.md §2 (AE2 QuartzGlassModel/CubeBuilder-паттерн).
 */
public final class GT6QuadBuilder {
	private final QuadCollection.Builder mQuads = new QuadCollection.Builder();
	private final List<BakedQuad> mAll = new ArrayList<>();
	/** Текущие render-bounds {minX,minY,minZ,maxX,maxY,maxZ} (1.7.10 RenderBlocks.setRenderBoundsFromBlock, обновляется per-pass). */
	private final float[] mBounds = {0, 0, 0, 1, 1, 1};
	private boolean mFullCube = true;

	/** F3-render: обновить текущие render-bounds перед проходом (GT6BlockModel читает {@code BlockBase.getRenderBounds()} после setBlockBounds). */
	public void setBounds(float[] aBounds) {
		if (aBounds == null || aBounds.length < 6) {System.arraycopy(new float[]{0,0,0,1,1,1}, 0, mBounds, 0, 6);}
		else System.arraycopy(aBounds, 0, mBounds, 0, 6);
		mFullCube = mBounds[0] <= 0 && mBounds[1] <= 0 && mBounds[2] <= 0 && mBounds[3] >= 1 && mBounds[4] >= 1 && mBounds[5] >= 1;
	}

	/** GT6 side-байт → neo Direction: SIDE_Y_NEG=0=DOWN, Y_POS=1=UP, Z_NEG=2=NORTH, Z_POS=3=SOUTH, X_NEG=4=WEST, X_POS=5=EAST. */
	/** Диаг П9: имена спрайтов, НЕ найденных в атласе (грань молча пропускалась → «частично без текстур»). */
	public static final java.util.Set<String> sMissingSprites = java.util.concurrent.ConcurrentHashMap.newKeySet();

	public void putFace(byte aSide, Identifier aIcon, short[] aRGBa) {
		if (aIcon == null || aSide < 0 || aSide > 5) return;
		TextureAtlasSprite tSprite = sprite(aIcon);
		if (tSprite == null) {if (sMissingSprites.size() < 400) sMissingSprites.add(aIcon.toString()); return;}
		Direction tDir = Direction.from3DDataValue(aSide);
		BakedQuad tQuad = boundedFace(tDir, tSprite, aRGBa);
		if (tQuad == null) return;
		// full-cube грань — cull-aware (сосед скроет её); sub-cube (спайк/бар/провод) — всегда видима.
		if (mFullCube) mQuads.addCulledFace(tDir, tQuad); else mQuads.addUnculledFace(tQuad);
		mAll.add(tQuad);
	}

	public QuadCollection build() {return mQuads.build();}
	public List<BakedQuad> quads() {return mAll;}
	public boolean isEmpty() {return mAll.isEmpty();}

	private static TextureAtlasSprite sprite(Identifier aIcon) {return resolveSprite(aIcon);}

	/** Резолв спрайта из block-атласа (по умолчанию — блок-грани через putFace/resolveBlockFaceIcon). */
	public static TextureAtlasSprite resolveSprite(Identifier aIcon) {return resolveSprite(aIcon, net.minecraft.data.AtlasIds.BLOCKS);}

	/** Резолв спрайта из указанного атласа. GT6-текстуры динамические: блок-грани — в BLOCKS (atlases/blocks.json),
	 *  item-иконки — в ITEMS (atlases/items.json, textures/items/**). GT6ItemModel резолвит из ITEMS (материал-предметы
	 *  берут item-версию materialicons, а не блочную; gt.multiitem.* иначе не в атласе → пурпур). */
	public static TextureAtlasSprite resolveSprite(Identifier aIcon, Identifier aAtlas) {
		try {
			net.minecraft.client.renderer.texture.TextureAtlas tAtlas = Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(aAtlas);
			TextureAtlasSprite tSprite = tAtlas.getSprite(aIcon);
			// getSprite возвращает MISSING-спрайт (не null) при отсутствии (TextureAtlas.java:255) → приводим к null: даёт
			// работать fallback (ITEMS→BLOCKS в GT6ItemModel) и пропуск грани в putFace вместо пурпур-квада; детекция пурпура.
			return tSprite == tAtlas.missingSprite() ? null : tSprite;
		} catch (Throwable e) {return null;}
	}

	/** F3 block-icon-data: neo-замена удалённого 1.7.10 {@code Block.getIcon(side,meta)} — {@link Identifier} спрайта
	 *  грани ВАНИЛЬНОГО блока из его baked {@link net.minecraft.client.renderer.block.dispatch.BlockStateModel} (спрайт
	 *  quad'а нужной стороны; particle-спрайт — fallback). Централизация §3: единственная точка «скопировать текстуру
	 *  другого блока» — используют {@link IconContainerCopied} и {@link BlockTextureCopied}. meta 1.7.10 схлопнут в
	 *  {@code defaultBlockState} (в neo вариантные под-блоки — отдельные Block'и, в вызывателях meta практически 0).
	 *  aSide 0..5 = {@code Direction.from3DDataValue} (тот же маппинг, что {@link #putFace}); SIDE_ANY/вне диапазона → particle. */
	public static Identifier resolveBlockFaceIcon(net.minecraft.world.level.block.Block aBlock, int aSide) {
		return resolveBlockFaceIcon(aBlock, aSide, 0);
	}

	/** F3 block-icon-data (meta-aware): 1:1 к 1.7.10 {@code Block.getIcon(side,meta)}. Варианты 1.7.10 (stonebrick 0..3,
	 *  dirt 0..2, sand 0..1) в neo — ОТДЕЛЬНЫЕ блоки (Flattening 1.13, таблица Mojang), не meta одного блока → сопоставляем
	 *  (базовый-neo-блок,meta)→блок-вариант, иначе defaultBlockState базового. Централизация §3: единственная точка учёта meta
	 *  для {@link BlockTextureCopied}/{@link IconContainerCopied} (их 1.7.10-предок звал getIcon с meta). */
	public static Identifier resolveBlockFaceIcon(net.minecraft.world.level.block.Block aBlock, int aSide, int aMeta) {
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

	/** Flattening 1.13 (таблица Mojang): 1.7.10 meta-варианты ванильных блоков → отдельные neo-блоки. Возвращает
	 *  блок-вариант для (базовый,meta) либо null (meta 0 / не мульти-блок → базовый). Только достоверные vanilla-таблицы. */
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

	/** Грань по текущим bounds (4 вершины) с UV из спрайта (клип по bounds) + tint из RGBa (0..255). AE2 QuartzGlassModel.createQuad/putVertex. */
	private BakedQuad boundedFace(Direction aDir, TextureAtlasSprite aSprite, short[] aRGBa) {
		int r = aRGBa != null && aRGBa.length >= 3 ? (aRGBa[0] & 0xFF) : 255;
		int g = aRGBa != null && aRGBa.length >= 3 ? (aRGBa[1] & 0xFF) : 255;
		int b = aRGBa != null && aRGBa.length >= 3 ? (aRGBa[2] & 0xFF) : 255;
		// 1.7.10 тинт грани шёл setColorOpaque_I (альфы у тинта НЕТ); GT6-цвета часто RGB-int с нулевой альфой →
		// a=0 в neo делал грань ПРОЗРАЧНОЙ (машины «без текстур», виден только overlay). 0 → 255.
		int a = aRGBa != null && aRGBa.length >= 4 && (aRGBa[3] & 0xFF) != 0 ? (aRGBa[3] & 0xFF) : 255;
		float[][] c = corners(aDir, mBounds);
		net.minecraft.world.phys.Vec3 n = aDir.getUnitVec3();
		QuadBakingVertexConsumer tBuilder = new QuadBakingVertexConsumer();
		tBuilder.setSprite(new Material.Baked(aSprite, false));
		tBuilder.setDirection(aDir);
		// КРИТ (инвертированные нормали — 'видно блок изнутри'): neo выводит нормаль/facing грани ИЗ winding вершин
		// (FaceBakery.computeQuadNormal), а corners давал CW-порядок (нормаль внутрь) → GPU back-face-cull скрывал грань
		// снаружи. Реверсируем порядок вершин (3→0) → нормаль наружу, грань видима снаружи. UV/позиция per-vertex сохранены.
		for (int i = 3; i >= 0; i--) {
			tBuilder.addVertex(c[i][0], c[i][1], c[i][2]);
			tBuilder.setColor(r, g, b, a);
			tBuilder.setNormal((float)n.x, (float)n.y, (float)n.z);
			// КРИТ (прозрачные/мусорные блоки): corners даёт u,v в 0..16 (block-texture конвенция), а neo getU/getV(offset)
			// ждут offset 0..1 (u0+(u1-u0)*offset) → без /16 UV в 16× мимо спрайта = сэмпл вне текстуры (прозрачные щели
			// атласа) → блок прозрачный. GT6ItemModel.flatFace уже делит на 16f — блочный путь этого не делал. Нормализуем.
			tBuilder.setUv(aSprite.getU(c[i][3] / 16f), aSprite.getV(c[i][4] / 16f));
		}
		return tBuilder.bakeQuad();
	}

	/** F3-render cross-модель (растения/цветы): X-форма из 2 диагональных плоскостей, каждая ДВУСТОРОННЯЯ (unculled,
	 *  видна с обеих сторон). Текстура полная (UV 0..16 /16f, как vanilla block/cross). Используют IRenderedCross-блоки. */
	public void crossFace(Identifier aIcon, short[] aRGBa) {
		if (aIcon == null) return;
		TextureAtlasSprite tSprite = sprite(aIcon);
		if (tSprite == null) return;
		mFullCube = false; // cross никогда не куб — грани не cull-aware
		addCrossPlane(new float[][]{{0,0,0},{1,0,1},{1,1,1},{0,1,0}}, tSprite, aRGBa); // диагональ SW->NE
		addCrossPlane(new float[][]{{1,0,0},{0,0,1},{0,1,1},{1,1,0}}, tSprite, aRGBa); // диагональ SE->NW
	}
	private void addCrossPlane(float[][] aCorners, TextureAtlasSprite aSprite, short[] aRGBa) {
		for (boolean tReverse : new boolean[]{false, true}) { // front + back = плоскость видна с обеих сторон
			BakedQuad tQuad = planeQuad(aCorners, aSprite, aRGBa, tReverse);
			if (tQuad != null) {mQuads.addUnculledFace(tQuad); mAll.add(tQuad);}
		}
	}
	/** Один quad произвольной плоскости (4 вершины) с полной UV + tint. aReverse — обратная намотка (задняя сторона). */
	private BakedQuad planeQuad(float[][] aCorners, TextureAtlasSprite aSprite, short[] aRGBa, boolean aReverse) {
		int r = aRGBa != null && aRGBa.length >= 3 ? (aRGBa[0] & 0xFF) : 255;
		int g = aRGBa != null && aRGBa.length >= 3 ? (aRGBa[1] & 0xFF) : 255;
		int b = aRGBa != null && aRGBa.length >= 3 ? (aRGBa[2] & 0xFF) : 255;
		// 1.7.10 тинт грани шёл setColorOpaque_I (альфы у тинта НЕТ); GT6-цвета часто RGB-int с нулевой альфой →
		// a=0 в neo делал грань ПРОЗРАЧНОЙ (машины «без текстур», виден только overlay). 0 → 255.
		int a = aRGBa != null && aRGBa.length >= 4 && (aRGBa[3] & 0xFF) != 0 ? (aRGBa[3] & 0xFF) : 255;
		float[][] tUV = {{0,16},{16,16},{16,0},{0,0}}; // bottom-left, bottom-right, top-right, top-left
		float nx = aCorners[1][2]-aCorners[0][2], nz = -(aCorners[1][0]-aCorners[0][0]); // нормаль плоскости в XZ (для освещения; cull отключён)
		float nlen = (float)Math.sqrt(nx*nx+nz*nz); if (nlen > 0) {nx/=nlen; nz/=nlen;}
		if (aReverse) {nx = -nx; nz = -nz;}
		QuadBakingVertexConsumer tBuilder = new QuadBakingVertexConsumer();
		tBuilder.setSprite(new Material.Baked(aSprite, false));
		tBuilder.setDirection(Direction.UP);
		int[] tOrder = aReverse ? new int[]{3,2,1,0} : new int[]{0,1,2,3};
		for (int idx = 0; idx < 4; idx++) {
			int i = tOrder[idx];
			tBuilder.addVertex(aCorners[i][0], aCorners[i][1], aCorners[i][2]);
			tBuilder.setColor(r, g, b, a);
			tBuilder.setNormal(nx, 0, nz);
			tBuilder.setUv(aSprite.getU(tUV[i][0] / 16f), aSprite.getV(tUV[i][1] / 16f));
		}
		return tBuilder.bakeQuad();
	}

	/** 4 угла грани по bounds b={minX,minY,minZ,maxX,maxY,maxZ}, CCW относительно нормали; {x,y,z,u,v} (u,v в 0..16 → UV клипается по bounds).
	 *  При full-cube (0..1) сводится к прежнему поведению (u,v = 0..16). */
	private static float[][] corners(Direction aDir, float[] b) {
		float x0 = b[0], y0 = b[1], z0 = b[2], x1 = b[3], y1 = b[4], z1 = b[5];
		float u0x = x0*16, u1x = x1*16, u0z = z0*16, u1z = z1*16, v0y = (1-y0)*16, v1y = (1-y1)*16;
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

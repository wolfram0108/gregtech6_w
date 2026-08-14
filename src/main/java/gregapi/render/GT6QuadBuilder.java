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

/**
 * F3-render (client): «захватывающий рендерер» — объект {@code aRenderer}, который передаётся в GT6-цепочку
 * {@code ITexture.renderXPos(aRenderer,...) -> ITexture.Util.renderSide(side, ResourceLocation, RGBa, ..., aRenderer, ...)}.
 * Вместо immediate-mode отрисовки (1.7.10 Tessellator, удалён) он АККУМУЛИРУЕТ per-side full-cube {@link BakedQuad}
 * для декларативной модели {@link GT6BlockModel}. Так GT6 per-side texture-логика (какая иконка/цвет на сторону —
 * решает сам ITexture/тайл) переиспользуется 1:1; переписан только «нарисуй сейчас» → «дай мне quad».
 * Спрайты резолвятся из атласа блоков в рантайме (текстуры GT6 динамические: материал×префикс, нельзя пре-bake).
 * Мост в {@link ITexture.Util}: если {@code aRenderer instanceof GT6QuadBuilder} → {@code putFace(side, icon, RGBa)}.
 * См. decisions/F3-render.md §2 (AE2 QuartzGlassModel/CubeBuilder-паттерн).
 */
public final class GT6QuadBuilder {
	/** F3-render (1.20.1): носитель результата вместо удалённого движкового {@code QuadCollection}. В 1.20.1
	 *  {@code BakedModel.getQuads(state, side, rand)} сам разделяет геометрию: {@code side != null} — грани,
	 *  которые скрывает сосед (аналог {@code addCulledFace}), {@code side == null} — всегда видимые
	 *  ({@code addUnculledFace}). Держим ровно эти два ведра — форма ответа модели, не новая абстракция. */
	public static final class QuadSet {
		private static final List<BakedQuad> EMPTY = Collections.emptyList();
		final List<BakedQuad>[] mCulled;
		final List<BakedQuad> mUnculled;
		@SuppressWarnings("unchecked")
		QuadSet(List<BakedQuad>[] aCulled, List<BakedQuad> aUnculled) {mCulled = aCulled; mUnculled = aUnculled;}
		/** aSide == null → всегда видимые грани; иначе — cull-aware грани этой стороны. */
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
	/** Текущие render-bounds {minX,minY,minZ,maxX,maxY,maxZ} (1.7.10 RenderBlocks.setRenderBoundsFromBlock, обновляется per-pass). */
	private final float[] mBounds = {0, 0, 0, 1, 1, 1};
	private boolean mFullCube = true;
	/** BUG-063 (граница отрисовки): суммарные bounds ФАКТИЧЕСКИ выданных граней — единственное место, где известно,
	 *  сколько геометрии GT6 реально занимает. Считать статикой нельзя: у GT6 боксы вычисляются в рантайме
	 *  (замер: из 785 объявлений box(...) числовых лишь 8 — тигель/турбина; трубы, коннекторы и каверы задают
	 *  границы выражениями). Копится тут, читается {@link MultiTileEntityBER#getRenderBoundingBox}. */
	private final float[] mDrawn = {0, 0, 0, 1, 1, 1};
	private boolean mDrawnAny = false;
	/** F3-render PILLAR: 1.7.10 RenderBlocks.uvRotate{Bottom,Top,East,West,North,South} → per-face поворот UV;
	 *  индекс = Direction.get3DDataValue (DOWN,UP,NORTH,SOUTH,WEST,EAST). Реализован вариант 1 — единственный,
	 *  который ставит vanilla renderBlockLog (RenderBlocks:4435-4446, PILLAR-блоки GT6: брёвна/балки/тюки). */
	private final byte[] mUVRotate = new byte[6];

	/** Выставить повороты UV per-face (порядок: DOWN,UP,NORTH,SOUTH,WEST,EAST; 0=нет, 1=вариант 1 renderBlockLog). */
	public void setUVRotate(int aDown, int aUp, int aNorth, int aSouth, int aWest, int aEast) {
		mUVRotate[0] = (byte)aDown; mUVRotate[1] = (byte)aUp; mUVRotate[2] = (byte)aNorth;
		mUVRotate[3] = (byte)aSouth; mUVRotate[4] = (byte)aWest; mUVRotate[5] = (byte)aEast;
	}
	/** Сброс поворотов (1.7.10 renderBlockLog обнулял uvRotate* после renderStandardBlock). */
	public void clearUVRotate() {java.util.Arrays.fill(mUVRotate, (byte)0);}

	/** F3-render: обновить текущие render-bounds перед проходом (GT6BlockModel читает {@code BlockBase.getRenderBounds()} после setBlockBounds). */
	public void setBounds(float[] aBounds) {
		if (aBounds == null || aBounds.length < 6) {System.arraycopy(new float[]{0,0,0,1,1,1}, 0, mBounds, 0, 6);}
		else System.arraycopy(aBounds, 0, mBounds, 0, 6);
		mFullCube = mBounds[0] <= 0 && mBounds[1] <= 0 && mBounds[2] <= 0 && mBounds[3] >= 1 && mBounds[4] >= 1 && mBounds[5] >= 1;
	}

	/** GT6 side-байт → neo Direction: SIDE_Y_NEG=0=DOWN, Y_POS=1=UP, Z_NEG=2=NORTH, Z_POS=3=SOUTH, X_NEG=4=WEST, X_POS=5=EAST. */
	/** Диаг П9: имена спрайтов, НЕ найденных в атласе (грань молча пропускалась → «частично без текстур»). */
	public static final java.util.Set<String> sMissingSprites = java.util.concurrent.ConcurrentHashMap.newKeySet();

	// ВРЕМЕННАЯ ДИАГНОСТИКА (цвет мира, репорт 2026-08-13): считаем грани с цветом и без по нитям (Render thread =
	// BER/item, Worker = чанк-компиляция). Печать капнута: 2 строки на прогон. Снять после закрытия дефекта цвета.
	private static final java.util.concurrent.atomic.AtomicLong sDiagFaces = new java.util.concurrent.atomic.AtomicLong();
	private static final java.util.concurrent.ConcurrentHashMap<String, long[]> sDiagByThread = new java.util.concurrent.ConcurrentHashMap<>();
	private static final java.util.Set<String> sDiagSamples = java.util.concurrent.ConcurrentHashMap.newKeySet();
	public void putFace(byte aSide, ResourceLocation aIcon, short[] aRGBa) {
		if (aIcon == null || aSide < 0 || aSide > 5) return;
		boolean tColored = aRGBa != null && aRGBa.length >= 3 && ((aRGBa[0] & 0xFF) != 255 || (aRGBa[1] & 0xFF) != 255 || (aRGBa[2] & 0xFF) != 255);
		String tThread = Thread.currentThread().getName().startsWith("Render") ? "render" : "worker";
		long[] tCnt = sDiagByThread.computeIfAbsent(tThread, k -> new long[2]);
		synchronized (tCnt) {tCnt[tColored ? 1 : 0]++;}
		String tKind = tThread + (tColored ? "-цвет" : "-бел");
		if (sDiagSamples.stream().filter(s -> s.startsWith(tKind)).count() < 3)
			sDiagSamples.add(tKind + ":" + aIcon + (aRGBa == null ? ":null" : ":" + (aRGBa[0]&0xFF) + "," + (aRGBa[1]&0xFF) + "," + (aRGBa[2]&0xFF)));
		long tN = sDiagFaces.incrementAndGet();
		if (tN == 2000 || tN == 20000) {
			StringBuilder tSB = new StringBuilder("[GT6-COLORDIAG] faces=").append(tN);
			for (java.util.Map.Entry<String, long[]> tE : sDiagByThread.entrySet()) tSB.append(" · ").append(tE.getKey()).append(" бел/цвет=").append(tE.getValue()[0]).append("/").append(tE.getValue()[1]);
			tSB.append(" · примеры: ").append(String.join(" | ", sDiagSamples));
			gregapi.data.CS.OUT.println(tSB.toString());
		}
		TextureAtlasSprite tSprite = sprite(aIcon);
		if (tSprite == null) {if (sMissingSprites.size() < 400) sMissingSprites.add(aIcon.toString()); return;}
		Direction tDir = Direction.from3DDataValue(aSide);
		BakedQuad tQuad = boundedFace(tDir, tSprite, aRGBa);
		if (tQuad == null) return;
		// full-cube грань — cull-aware (сосед скроет её); sub-cube (спайк/бар/провод) — всегда видима.
		if (mFullCube) addCulled(tDir, tQuad); else mUnculled.add(tQuad);
		mAll.add(tQuad);
		// BUG-063: границы копим по РЕАЛЬНО выданной грани (а не по каждому объявленному боксу) — тогда рамка
		// отсечения совпадает с тем, что видит игрок, и не раздувается проходами, у которых текстуры не нашлось.
		if (mDrawnAny) {
			for (int i = 0; i < 3; i++) if (mBounds[i] < mDrawn[i]) mDrawn[i] = mBounds[i];
			for (int i = 3; i < 6; i++) if (mBounds[i] > mDrawn[i]) mDrawn[i] = mBounds[i];
		} else {
			System.arraycopy(mBounds, 0, mDrawn, 0, 6);
			mDrawnAny = true;
		}
	}

	/** BUG-063: суммарные bounds выданных граней {minX,minY,minZ,maxX,maxY,maxZ} в локальных координатах блока,
	 *  либо null, если не выдано ни одной грани. */
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

	// F3-render (1.20.1): в 1.20.1 атлас БЛОКОВ ОДИН и он же держит иконки предметов (vanilla
	// assets/minecraft/atlases/blocks.json перечисляет и block/, и item/) — раздельных AtlasIds.BLOCKS/ITEMS,
	// как в 26.x, здесь нет. Разделение GT6 (блок-текстуры textures/blocks/**, item-иконки textures/items/**)
	// сохранено ПРЕФИКСОМ sprite-id в atlas-source: blocks → prefix "" (id `gregtech:materialicons/...`, как
	// строит getIcon), items → prefix "items/" (id `gregtech:items/materialicons/...`), иначе одноимённые
	// materialicons/iconsets обеих папок столкнулись бы в одном атласе. Перевод id ↔ ведро живёт ТОЛЬКО здесь.
	/** Ведро атласа: блок-текстуры (sprite-id как есть). */
	public static final int ATLAS_BLOCKS = 0;
	/** Ведро атласа: item-иконки (sprite-id получает префикс {@code items/}). */
	public static final int ATLAS_ITEMS = 1;

	/** «Атлас предметов» как ЗНАЧЕНИЕ канала {@code IIconContainer.getTextureFile()} (1.7.10
	 *  {@code TextureMap.locationItemsTexture}, 26.x {@code gregapi.render.GT6QuadBuilder.LOCATION_ITEMS}). В 1.20.1 отдельного
	 *  items-атласа не существует — иконки предметов лежат в block-атласе под префиксом {@code items/} (см.
	 *  {@link #ATLAS_ITEMS}), поэтому значение служит МАРКЕРОМ ведра, а не адресом текстуры. Канал сохранён
	 *  ради 1:1 состава контракта: читателей у него в моде нет (греп {@code getTextureFile} — только объявления). */
	public static final ResourceLocation LOCATION_ITEMS = ResourceLocation.withDefaultNamespace("textures/atlas/items.png");

	/** Резолв спрайта из block-атласа (по умолчанию — блок-грани через putFace/resolveBlockFaceIcon). */
	public static TextureAtlasSprite resolveSprite(ResourceLocation aIcon) {return resolveSprite(aIcon, ATLAS_BLOCKS);}

	/** Резолв спрайта из указанного ведра. GT6-текстуры динамические: блок-грани — textures/blocks/**,
	 *  item-иконки — textures/items/** (материал-предметы берут item-версию materialicons, а не блочную;
	 *  gt.multiitem.* иначе не в атласе → пурпур). Оба ведра лежат в одном block-атласе 1.20.1. */
	public static TextureAtlasSprite resolveSprite(ResourceLocation aIcon, int aAtlasBucket) {
		if (aIcon == null) return null;
		try {
			TextureAtlas tAtlas = Minecraft.getInstance().getModelManager().getAtlas(TextureAtlas.LOCATION_BLOCKS);
			if (tAtlas == null) return null;
			TextureAtlasSprite tSprite = tAtlas.getSprite(aAtlasBucket == ATLAS_ITEMS ? aIcon.withPrefix("items/") : aIcon);
			// getSprite возвращает MISSING-спрайт (не null) при отсутствии (TextureAtlas.java:125-128) → приводим к null: даёт
			// работать fallback (ITEMS→BLOCKS в GT6ItemModel) и пропуск грани в putFace вместо пурпур-квада; детекция пурпура.
			return tSprite == null || MissingTextureAtlasSprite.getLocation().equals(tSprite.contents().name()) ? null : tSprite;
		} catch (Throwable e) {return null;}
	}

	/** F3 block-icon-data: neo-замена удалённого 1.7.10 {@code Block.getIcon(side,meta)} — {@link ResourceLocation} спрайта
	 *  грани ВАНИЛЬНОГО блока из его baked {@link net.minecraft.client.renderer.block.dispatch.BlockStateModel} (спрайт
	 *  quad'а нужной стороны; particle-спрайт — fallback). Централизация §3: единственная точка «скопировать текстуру
	 *  другого блока» — используют {@link IconContainerCopied} и {@link BlockTextureCopied}. meta 1.7.10 схлопнут в
	 *  {@code defaultBlockState} (в neo вариантные под-блоки — отдельные Block'и, в вызывателях meta практически 0).
	 *  aSide 0..5 = {@code Direction.from3DDataValue} (тот же маппинг, что {@link #putFace}); SIDE_ANY/вне диапазона → particle. */
	public static ResourceLocation resolveBlockFaceIcon(net.minecraft.world.level.block.Block aBlock, int aSide) {
		return resolveBlockFaceIcon(aBlock, aSide, 0);
	}

	/** F3 block-icon-data (meta-aware): 1:1 к 1.7.10 {@code Block.getIcon(side,meta)}. Варианты 1.7.10 (stonebrick 0..3,
	 *  dirt 0..2, sand 0..1) в neo — ОТДЕЛЬНЫЕ блоки (Flattening 1.13, таблица Mojang), не meta одного блока → сопоставляем
	 *  (базовый-neo-блок,meta)→блок-вариант, иначе defaultBlockState базового. Централизация §3: единственная точка учёта meta
	 *  для {@link BlockTextureCopied}/{@link IconContainerCopied} (их 1.7.10-предок звал getIcon с meta). */
	public static ResourceLocation resolveBlockFaceIcon(net.minecraft.world.level.block.Block aBlock, int aSide, int aMeta) {
		// GT6-блок-цель (LIVE-DEFECTS №5): его модель — динамический GT6BlockModel, который БЕЗ level/pos квадов не отдаёт
		// → baked-путь ниже падал в particle = system/error (жёлто-красный X у камешков на GT6-породах). Родной 1.7.10-канал
		// Block.getIcon(side,meta) сохранён на BlockBase-иерархии (BlockBaseMeta/Log/Beam/Grass/...) — спрашиваем его напрямую;
		// defensive-throw/null у классов без канала → штатный baked-путь ниже.
		// Отбор по КОНТРАКТУ (gregapi.block.IBlock#getIcon), а не по иерархии: общего Block-предка у GT6 нет,
		// а канал иконки есть у всех — BlockBase, обеих жидкостных иерархий и MTE. Носитель обязан ОТВЕЧАТЬ
		// (null = «канала нет» → baked-путь ниже), поэтому глушилка catch(Throwable) здесь больше не нужна.
		if (aBlock instanceof gregapi.block.IBlock tGT6) {
			ResourceLocation tIcon = tGT6.getIcon(aSide, aMeta);
			if (tIcon != null) return tIcon;
		}
		net.minecraft.world.level.block.Block tVariant = flattenVariant(aBlock, aMeta);
		net.minecraft.world.level.block.state.BlockState tState = (tVariant != null ? tVariant : aBlock).defaultBlockState();
		// 1.20.1: baked-модель блока берётся у BlockModelShaper (ModelManager.java:73), сама модель отдаёт
		// per-side квады (BakedModel.getQuads(state,side,rand)) и particle-спрайт (getParticleIcon) —
		// это те же два канала, что в 26.x давали BlockStateModelSet/BlockStateModelPart.
		net.minecraft.client.resources.model.BakedModel tModel = Minecraft.getInstance().getBlockRenderer().getBlockModel(tState);
		if (aSide >= 0 && aSide <= 5) {
			Direction tDir = Direction.from3DDataValue(aSide);
			List<BakedQuad> tQuads = tModel.getQuads(tState, tDir, net.minecraft.util.RandomSource.create(42L));
			if (tQuads != null && !tQuads.isEmpty()) return tQuads.get(0).getSprite().contents().name();
		}
		return tModel.getParticleIcon().contents().name();
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

	/** Порядок обхода углов {@link #corners} при выдаче грани куба: приводит вершины к КАНОНУ ванили
	 *  ({@code FaceInfo.java:10-15}), от которого зависит раскладка AO по углам. Разбор — в {@link #boundedFace}. */
	private static final int[] EMIT_ORDER = {1, 0, 3, 2};

	/** Грань по текущим bounds (4 вершины) с UV из спрайта (клип по bounds) + tint из RGBa (0..255). AE2 QuartzGlassModel.createQuad/putVertex. */
	private BakedQuad boundedFace(Direction aDir, TextureAtlasSprite aSprite, short[] aRGBa) {
		int r = aRGBa != null && aRGBa.length >= 3 ? (aRGBa[0] & 0xFF) : 255;
		int g = aRGBa != null && aRGBa.length >= 3 ? (aRGBa[1] & 0xFF) : 255;
		int b = aRGBa != null && aRGBa.length >= 3 ? (aRGBa[2] & 0xFF) : 255;
		// 1.7.10 тинт грани шёл setColorOpaque_I (альфы у тинта НЕТ); GT6-цвета часто RGB-int с нулевой альфой →
		// a=0 в neo делал грань ПРОЗРАЧНОЙ (машины «без текстур», виден только overlay). 0 → 255.
		int a = aRGBa != null && aRGBa.length >= 4 && (aRGBa[3] & 0xFF) != 0 ? (aRGBa[3] & 0xFF) : 255;
		float[][] c = corners(aDir, mBounds);
		if (mUVRotate[aDir.get3DDataValue()] == 1) rotateUV1(aDir, c, mBounds);
		org.joml.Vector3f n = aDir.step();
		QuadBakingVertexConsumer.Buffered tBuilder = new QuadBakingVertexConsumer.Buffered();
		tBuilder.setSprite(aSprite);
		tBuilder.setDirection(aDir);
		tBuilder.setTintIndex(-1);   // тинт УЖЕ запечён в вершины (RGBa GT6); tintIndex по умолчанию 0 → движок домножил бы ЕЩЁ раз через BlockColors
		tBuilder.setShade(true);     // грань блока затеняется по направлению — 1:1 1.7.10 renderStandardBlock (per-face множители яркости)
		tBuilder.setHasAmbientOcclusion(true);
		// КРИТ (инвертированные нормали — 'видно блок изнутри'): движок выводит нормаль/facing грани ИЗ winding вершин
		// (FaceBakery.computeQuadNormal), а corners давал CW-порядок (нормаль внутрь) → GPU back-face-cull скрывал грань
		// снаружи. Обмотку разворачиваем — нормаль наружу, грань видима снаружи. UV/позиция per-vertex сохранены.
		//
		// ⚠️ ПОЧЕМУ ИМЕННО {1,0,3,2}, А НЕ {3,2,1,0} (репорт игрока «тени на гранях есть, но повёрнуты»).
		// Ванильный AO-конвейер 1.20.1 кладёт посчитанные яркости и лайтмапы в квад ПО НОМЕРУ ВЕРШИНЫ,
		// молча считая номер КАНОНИЧЕСКИМ: `brightness[remap.vertN] = <яркость угла N>`
		// (ModelBlockRenderer.java:431-434, таблица AmbientVertexRemap :490-496, раздача по вершинам :154).
		// Канон порядка задаёт FaceInfo.java:10-15. Прежний обход {3,2,1,0} давал перестановку [2,3,0,1]
		// на ВСЕХ шести сторонах — это поворот карты затенения на 180°: геометрия и текстура верны (их мы
		// задаём сами, u/v лежат в той же строке `corners()`), а тень ложится не на свой угол.
		// На ветке main того же не видно не потому, что порядок другой (он там такой же), а потому, что
		// NeoForge 26.x подменяет ванильный лайтер своим, координатным: он считает яркость по ФАКТИЧЕСКИМ
		// координатам вершины и порядок игнорирует (neo .../block/ModelBlockRenderer.java:41-42 →
		// neoforge .../client/model/ao/EnhancedBlockModelLighter.java:33,103,110-119). У Forge 1.20.1 такого
		// лайтера нет вовсе — здесь канон обязателен.
		// {1,0,3,2} — ЦИКЛИЧЕСКИЙ сдвиг прежнего {3,2,1,0} на 2, поэтому обмотка (а с ней нормаль и
		// back-face-cull) не меняется: фикс «видно блок изнутри» выше остаётся в силе.
		// Стережёт статический судья: workspace/tools/quad_vertex_order_judge.py (читает ЭТОТ файл;
		// обязан давать перестановку [0,1,2,3] и «сторон с расхождением: 0 из 6»).
		for (int idx = 0; idx < 4; idx++) {
			final int i = EMIT_ORDER[idx];
			tBuilder.vertex(c[i][0], c[i][1], c[i][2]);
			tBuilder.color(r, g, b, a);
			tBuilder.normal(n.x(), n.y(), n.z());
			// КРИТ (шкала UV): corners даёт u,v в 0..16 (block-texture конвенция). getU/getV 1.20.1 САМИ делят на 16
			// (TextureAtlasSprite.java:66-69, u0+(u1-u0)*p/16 — шкала 1.7.10 getInterpolatedU); деление здесь (нужное
			// в 26.x, где offset 0..1) сжимало развёртку в 1/16 уголка спрайта — грани красились одним текселем (белые).
			tBuilder.uv(aSprite.getU(c[i][3]), aSprite.getV(c[i][4]));
			tBuilder.endVertex();
		}
		return tBuilder.getQuad();
	}

	/** F3-fluid: quad жидкости с произвольными вершинами {x,y,z,u,v} (u,v в texel-единицах 0..16, как 1.7.10
	 *  getInterpolatedU/V) — кванта-высоты/склоны поверхности. Всегда unculled (видимость решает
	 *  {@link RendererBlockFluid} 1:1-логикой shouldSideBeRendered, а не neo-cull). aBothSides — вторая обратная
	 *  намотка (1.7.10 рендерил без backface-cull и дублировал winding: поверхность видна из-под жидкости). */
	public void fluidQuad(float[][] aCorners, Direction aDir, ResourceLocation aIcon, short[] aRGBa, boolean aBothSides) {
		if (aIcon == null || aCorners == null || aCorners.length < 4) return;
		TextureAtlasSprite tSprite = sprite(aIcon);
		if (tSprite == null) {if (sMissingSprites.size() < 400) sMissingSprites.add(aIcon.toString()); return;}
		mFullCube = false;
		BakedQuad tQuad = vertexQuad(aCorners, tSprite, aRGBa, aDir, false);
		if (tQuad != null) {mUnculled.add(tQuad); mAll.add(tQuad);}
		if (aBothSides) {
			BakedQuad tBack = vertexQuad(aCorners, tSprite, aRGBa, aDir.getOpposite(), true);
			if (tBack != null) {mUnculled.add(tBack); mAll.add(tBack);}
		}
	}
	/** Один quad по 4 вершинам {x,y,z,u,v} (u,v 0..16) с tint; aReverse — обратная намотка. */
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
		int[] tOrder = aReverse ? new int[]{3,2,1,0} : new int[]{0,1,2,3};
		for (int idx = 0; idx < 4; idx++) {
			int i = tOrder[idx];
			tBuilder.vertex(aCorners[i][0], aCorners[i][1], aCorners[i][2]);
			tBuilder.color(r, g, b, a);
			tBuilder.normal(n.x(), n.y(), n.z());
			tBuilder.uv(aSprite.getU(aCorners[i][3]), aSprite.getV(aCorners[i][4])); // getU/getV 1.20.1 ждут 0..16 (см. КРИТ выше)
			tBuilder.endVertex();
		}
		return tBuilder.getQuad();
	}

	/** F3-render cross-модель (растения/цветы): X-форма из 2 диагональных плоскостей, каждая ДВУСТОРОННЯЯ (unculled,
	 *  видна с обеих сторон). Текстура полная (UV 0..16 /16f, как vanilla block/cross). Используют IRenderedCross-блоки. */
	public void crossFace(ResourceLocation aIcon, short[] aRGBa) {
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
			if (tQuad != null) {mUnculled.add(tQuad); mAll.add(tQuad);}
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
		QuadBakingVertexConsumer.Buffered tBuilder = new QuadBakingVertexConsumer.Buffered();
		tBuilder.setSprite(aSprite);
		tBuilder.setDirection(Direction.UP);
		tBuilder.setTintIndex(-1);
		// 1:1 vanilla block/cross-модель («затемнённые цветы», репорт игрока): "shade": false + "ambientocclusion": false —
		// без направленного затенения по нормали (XZ-нормаль давала ×0.6-0.8) и без AO; 1.7.10 рисовал cross ровным светом.
		tBuilder.setShade(false);
		tBuilder.setHasAmbientOcclusion(false);
		int[] tOrder = aReverse ? new int[]{3,2,1,0} : new int[]{0,1,2,3};
		for (int idx = 0; idx < 4; idx++) {
			int i = tOrder[idx];
			tBuilder.vertex(aCorners[i][0], aCorners[i][1], aCorners[i][2]);
			tBuilder.color(r, g, b, a);
			tBuilder.normal(nx, 0, nz);
			tBuilder.uv(aSprite.getU(tUV[i][0]), aSprite.getV(tUV[i][1])); // getU/getV 1.20.1 ждут 0..16 (см. КРИТ выше)
			tBuilder.endVertex();
		}
		return tBuilder.getQuad();
	}

	/** F3-render PILLAR: вариант 1 поворота UV — ДОСЛОВНО из 1.7.10 RenderBlocks (ветки uvRotate*==1 в
	 *  renderFaceYNeg:7254/YPos:7349/ZNeg:7485/ZPos:7588/XNeg:7704/XPos:7840, включая свопы d7..d10),
	 *  сведено в per-vertex UV-таблицы для порядка вершин {@link #corners}. Соответствие полей 1.7.10 → грань:
	 *  uvRotateBottom→DOWN, Top→UP, East→ZNeg(NORTH), West→ZPos(SOUTH), North→XNeg(WEST), South→XPos(EAST). */
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

	/** 4 угла грани по bounds b={minX,minY,minZ,maxX,maxY,maxZ}, CCW относительно нормали; {x,y,z,u,v} (u,v в 0..16 → UV клипается по bounds).
	 *  При full-cube (0..1) сводится к прежнему поведению (u,v = 0..16). */
	private static float[][] corners(Direction aDir, float[] b) {
		float x0 = b[0], y0 = b[1], z0 = b[2], x1 = b[3], y1 = b[4], z1 = b[5];
		// ГРАНИЦЫ ВНЕ КУБА → UV ПОЛНЫЕ, а не интерполированные. 1.7.10 делает это в КАЖДОЙ из шести renderFaceXXX
		// (RenderBlocks:7224-7234 YNeg, :7332 YPos, :7440 ZNeg, :7571-7581 ZPos, :7687 XNeg, :7803 XPos) парой
		// «if (renderMinA < 0 || renderMaxA > 1) {d = getMinU/V(); d' = getMaxU/V();}»: интерполяция за пределами
		// 0..1 увела бы координату ЗА СПРАЙТ, и движок сэмплил бы соседей по атласу. GT6 опирается на это всерьёз —
		// лопасти турбины рисуются боксом −0.999..1.999 (MultiTileEntityLargeTurbine:117-119), стенки тигля
		// −0.999..3.0 (MultiTileEntityCrucible:648-653), луч лазера −0.99..1.99, ErrorRenderer −0.25..1.25.
		// БЕЗ этой страховки было ровно то, что видел игрок (BUG-061): «турбина показывает кусок атласа вместо
		// лопастей, дыры на тиглях». Геометрию НЕ трогаем — вершины остаются на фактических границах, полными
		// становятся только UV, то есть подставляем 0..1 вместо фактических границ ТОЛЬКО в текстурные координаты.
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

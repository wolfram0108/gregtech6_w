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

import net.minecraft.client.renderer.Sheets;
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

/**
 * F3-render спец-рендеры MTE (1.7.10 {@code ClientRegistry.bindTileEntitySpecialRenderer}) — и ТОЛЬКО они.
 *
 * <p><b>BUG-138 носитель №2.</b> Прежде этот BlockEntityRenderer строил и заливал геометрию ВСЕХ MTE КАЖДЫЙ КАДР
 * (303 класса: машины, трубы, камни, кусты, покрытия) — 38,85 % рендер-потока живого клиента, из них 35,4 % на
 * заливку вершин. Обоснованием служило «регион чанк-компиляции BE не отдаёт (getBlockEntity=null 100%)»; живая
 * проба (стенд {@code gt6meshgate}, 2026-08-21) показала обратное: регион отдаёт ТОТ ЖЕ живой объект BE
 * ({@code RenderSectionRegion.getBlockEntity:73-79} → {@code SectionCopy:34,47-49} — копируется КАРТА, не сущности).
 * Поэтому облик MTE снова живёт в МЭШЕ СЕКЦИИ — его собирает {@link GT6BlockModel#collectParts} тем же центром
 * {@link GT6BlockModel#buildRendererQuads}, ровно как в 1.7.10 ({@code MultiTileEntityBlock.getRenderType()} →
 * {@code RendererBlockTextured implements ISimpleBlockRenderingHandler}, оригинал {@code :295}).
 *
 * <p><b>Что осталось покадровым — 1:1 с оригиналом.</b> В 1.7.10 у GT6 было РОВНО ДВА покадровых рендерера
 * (сундук и масс-сторадж). Здесь их держит реестр {@link #SPECIAL_RENDERERS} — диспетч по КЛАССУ внутри единого
 * BER, потому что движок регистрирует рендерер по {@code BlockEntityType}, а он у всех MTE один из двух. Всем
 * прочим MTE {@link #extractRenderState} геометрию НЕ строит.
 *
 * <p><b>Трещины разрушения.</b> Признак «по этому блоку идёт разрушение» движок кладёт прямо в аргумент
 * {@code extractRenderState} ({@code ModelFeatureRenderer.CrumblingOverlay}, {@code LevelRenderer:939-945});
 * по нему и строится живая геометрия ломаемого блока, чтобы трещины легли на его ФАКТИЧЕСКУЮ форму — 1:1 с
 * 1.7.10, где {@code RenderGlobal.drawBlockDamageTexture} звал тот же {@code RendererBlockTextured} с реальным миром.
 *
 * <p>Руды ({@code PrefixBlockTileEntity}) и стабы отсеиваются тем же гейтом, что и раньше.
 */
public class MultiTileEntityBER implements BlockEntityRenderer<TileEntityBase01Root, MultiTileEntityBER.MTERenderState> {

	public MultiTileEntityBER(BlockEntityRendererProvider.Context aContext) {/* per-BE геометрия строится в extractRenderState; ресурсы контекста тут не нужны */}

	// BUG-138: переопределение дистанции СНЯТО. Оно стояло потому, что геометрия MTE шла через этот BER и пропадала
	// за движковыми 64 блоками; теперь она в мэше секции и рисуется на всю дальность прорисовки. Дефолт движка
	// (64 блока) — ровно 1:1 с 1.7.10, где TESR резался тем же радиусом (TileEntity.getMaxRenderDistanceSquared()
	// == 4096), а покадровыми были только сундук и масс-сторадж.

	/** BUG-063 (репорт игрока: «как только центральный нижний блок выходит за границы экрана, весь тигель сразу
	 *  пропадает»): neo отсекает рисунок BE по ЭТОЙ рамке ({@code BlockEntityRenderDispatcher:90}), а её умолчание —
	 *  куб самого блока ({@code IBlockEntityRendererExtension:20-22}). У GT6 геометрия за свой блок выходит штатно
	 *  (тигель рисует всю структуру 3×3×3 из контроллера — {@code MultiTileEntityCrucible:648-653}; лопасти турбины,
	 *  коннекторы труб), а в 1.7.10 такого узла не было вовсе: MTE рисовались мэшем чанка и отсекались секцией 16³
	 *  ({@code RendererBlockTextured implements ISimpleBlockRenderingHandler}), TESR же имели дефолт INFINITE
	 *  ({@code recompSrc TileEntity:399-420}). Рамку НЕ ЗАДАЁМ константой — у GT6 боксы вычисляются в рантайме;
	 *  берём ФАКТИЧЕСКУЮ геометрию прошлого кадра ({@link gregapi.render.GT6QuadBuilder#drawnBounds}), а пока она
	 *  неизвестна — один кадр без отсечения, чтобы extract состоялся и рамка стала известна (дистанция при этом
	 *  по-прежнему режет: фрустум проверяется ДО shouldRender). Приём канонический: так же объявляют рамку маяк
	 *  (луч в небо), сундук (крышка) и поршень ({@code BeaconRenderer:221}, {@code ChestRenderer:136}, {@code PistonHeadRenderer:96}). */
	@Override
	@SuppressWarnings({"rawtypes", "unchecked"})
	public net.minecraft.world.phys.AABB getRenderBoundingBox(TileEntityBase01Root aBE) {
		net.minecraft.world.phys.AABB rBox = aBE.mRenderAABB;
		BlockEntityRenderer tSpecial = SPECIAL_RENDERERS.get(aBE.getClass());
		if (tSpecial != null) try {
			net.minecraft.world.phys.AABB tSpecialBox = tSpecial.getRenderBoundingBox(aBE);
			rBox = rBox == null ? tSpecialBox : (tSpecialBox == null ? rBox : rBox.minmax(tSpecialBox));
		} catch (Throwable e) {/* чужая рамка не должна ронять кадр */}
		return rBox == null ? net.minecraft.world.phys.AABB.INFINITE : rBox;
	}

	// F3-render спец-рендеры (1.7.10 ClientRegistry.bindTileEntitySpecialRenderer = vanilla-диспетчер по КЛАССУ TE;
	// в neo BER регистрируется по BlockEntityType, а у всех MTE он ОДИН — MTE_TYPE) → диспетч по классу живёт здесь,
	// в едином BER: реестр класс→рендерер, extract/submit делегируются. Оба живых TESR GT6 (Chest/MassStorage) идут сюда.
	@SuppressWarnings("rawtypes")
	private static final java.util.Map<Class<?>, BlockEntityRenderer> SPECIAL_RENDERERS = new java.util.HashMap<>();
	public static void bindSpecialRenderer(Class<?> aTileEntityClass, @SuppressWarnings("rawtypes") BlockEntityRenderer aRenderer) {SPECIAL_RENDERERS.put(aTileEntityClass, aRenderer);}

	/** Диаг-счётчики кэша квадов (BUG-106 №4): extract'ы рендер-объектов / реальные пересборки / кэш-хиты. */
	public static final java.util.concurrent.atomic.AtomicLong sQuadExtracts = new java.util.concurrent.atomic.AtomicLong(), sQuadBuilds = new java.util.concurrent.atomic.AtomicLong(), sQuadCacheHits = new java.util.concurrent.atomic.AtomicLong();
	/** Счётчик вызовов {@link #onSectionDirty} — судья шторма O(N) (живой стенд gt6berstorm). */
	public static final java.util.concurrent.atomic.AtomicLong sSectionDirtyCalls = new java.util.concurrent.atomic.AtomicLong();

	/** BUG-106 №4 — кэш квадов BER. Эпоха рендера: {@code allChanged()} (перешив атласа/моделей — F3+T, F3+A,
	 *  смена дистанции; кэшированные квады держат UV СТАРОГО атласа) рвёт ВСЕ кэши разом, O(1). Точечный сброс —
	 *  {@link #onSectionDirty}: та же воронка, которой движок помечает секции на перестройку, то есть ровно тот
	 *  сигнал, по которому в 1.7.10 пересобирался мэш с геометрией MTE (recompSrc RenderGlobal.markBlockForUpdate →
	 *  markBlockRangeForRenderUpdate ±1). Всё общение клиента о смене облика уже проходит через неё:
	 *  каждый receiveData*-диспетчер ({@code MultiTileEntityBlock:265-325}) кончается {@code WD.update} →
	 *  {@code ClientLevel.sendBlockUpdated:701} → {@code LevelRenderer.blockChanged:1432} → setSectionDirty;
	 *  прямые setBlock и свет — туда же ({@code viewArea.setDirty} зовётся ТОЛЬКО из setSectionDirty:1481).
	 *  Залипание кэша возможно лишь там, где залипал бы и мэш 1.7.10 — 1:1 по следствию. */
	public static long sQuadEpoch = 0;

	/** ШТАМП СЕКЦИИ — цена сигнала (волна 3 консолидации, п.2). Сигнал {@code setSectionDirty} для движка стоит
	 *  один флаг ({@code viewArea.setDirty}), поэтому он зовёт его ПАЧКАМИ: {@code setBlockDirty} крутит ±1 по
	 *  трём осям и бьёт 27 раз на ОДНО изменение блока ({@code LevelRenderer.java:1446-1460}), а приход чанка
	 *  добавляет свет ({@code ClientPacketListener.enableChunkLight} → {@code Level.setSectionRangeDirty}, 3×3×N
	 *  секций по Y). Прежняя редакция вешала на этот сигнал обход ВСЕХ блок-сущностей чанка — живой замер
	 *  (стенд gt6berstorm, полёт в непрогруженную область) дал сотни тысяч вызовов и миллионы бесполезных
	 *  итераций за десятки секунд.
	 *
	 *  <p>Работа снята с сигнала и отдана моменту рендера: сигнал лишь ПЕЧАТАЕТ секцию (инкремент, O(1)), а
	 *  валидность кэша каждый MTE проверяет сам, сверяя свой оттиск со штампом своей секции. Гранулярность и
	 *  момент инвалидации те же, что были (та же воронка, та же секция) — дешевеет только цена, поэтому 1:1
	 *  по следствию с мэшем 1.7.10 сохраняется.
	 *
	 *  <p>Смена мира карту не переживает: {@code LevelRenderer.setLevel} зовёт {@code allChanged()} →
	 *  {@link #onRenderAllChanged} чистит её тем же движковым сигналом, которым рвутся и эпохи. Отдельного
	 *  механизма выгрузки не заводим. */
	private static final it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap SECTION_STAMP = new it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap();

	/** Печать секции (зовёт MixinLevelRenderer из setSectionDirty, main-thread). O(1) — ни чанка, ни его блок-сущностей. */
	public static void onSectionDirty(int aSectionX, int aSectionY, int aSectionZ) {
		sSectionDirtyCalls.incrementAndGet();
		SECTION_STAMP.addTo(net.minecraft.core.SectionPos.asLong(aSectionX, aSectionY, aSectionZ), 1L);
	}

	/** Штамп секции, в которой лежит позиция (0 = секцию не помечали ни разу — законное значение, см. рендер). */
	private static long sectionStamp(BlockPos aPos) {
		return SECTION_STAMP.get(net.minecraft.core.SectionPos.asLong(
			  net.minecraft.core.SectionPos.blockToSectionCoord(aPos.getX())
			, net.minecraft.core.SectionPos.blockToSectionCoord(aPos.getY())
			, net.minecraft.core.SectionPos.blockToSectionCoord(aPos.getZ())));
	}

	/** Полный сброс (зовёт MixinLevelRenderer из allChanged, main-thread): эпоха рвёт все кэши разом, штампы
	 *  секций теряют смысл вместе с ней (иначе карта росла бы от мира к миру). */
	public static void onRenderAllChanged() {sQuadEpoch++; SECTION_STAMP.clear();}
	/** Счётчик crack-decal сабмитов (судья трещин). */
	public static final java.util.concurrent.atomic.AtomicLong sCrackSubmits = new java.util.concurrent.atomic.AtomicLong();

	public static boolean hasSpecialRenderer(Class<?> aTileEntityClass) {return SPECIAL_RENDERERS.containsKey(aTileEntityClass);}

	/** Item-форма TESR-классов: 1.7.10 renderItem звал renderTileEntityAt(this,0,0,0,0) на canonical-TE (данные из NBT
	 *  стека); neo-носитель — special-model слой предмета ({@code LayerRenderState.setupSpecialModel}) → ЭТОТ адаптер:
	 *  тот же зарегистрированный спец-рендерер (диспетч по классу), extract с detached-BE (fullbright по extractBase). */
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
			} catch (Throwable e) {/* item-форма не должна ронять рендер */}
		}
		@Override public void getExtents(java.util.function.Consumer<org.joml.Vector3fc> aOutput) {
			for (int x = 0; x <= 1; x++) for (int y = 0; y <= 1; y++) for (int z = 0; z <= 1; z++) aOutput.accept(new org.joml.Vector3f(x, y, z));
		}
		@Override public net.minecraft.world.level.block.entity.BlockEntity extractArgument(net.minecraft.world.item.ItemStack aStack) {
			try {
				if (aStack.getItem() instanceof gregapi.block.multitileentity.MultiTileEntityItemInternal tMTE) {
					gregapi.block.multitileentity.MultiTileEntityContainer tCont = tMTE.mBlock.mMultiTileEntityRegistry.getNewTileEntityContainer(aStack);
					// BUG-078: ВТОРОЙ путь рождения detached-TE (предметы со своим рендерером: сундук, масстораж).
					// Компенсация item-facing берётся из того же центра, что и у обычного item-рендера.
					if (tCont != null && tCont.mTileEntity != null && SPECIAL_RENDERERS.containsKey(tCont.mTileEntity.getClass())) return gregapi.block.multitileentity.MultiTileEntityRegistry.applyItemFacing(tCont.mTileEntity);
				}
			} catch (Throwable e) {/**/}
			return null;
		}
	};

	/** Снапшот геометрии, собранной на main-thread (thread-safe: submit его лишь читает). */
	public static class MTERenderState extends BlockEntityRenderState {
		public List<BakedQuad> mQuads;
		@SuppressWarnings("rawtypes") public BlockEntityRenderer mSpecialRenderer;
		public BlockEntityRenderState mSpecialState;
	}

	@Override public MTERenderState createRenderState() {return new MTERenderState();}

	@Override
	@SuppressWarnings("unchecked")
	public void extractRenderState(TileEntityBase01Root aBE, MTERenderState aState, float aPartialTicks, Vec3 aCameraPos, ModelFeatureRenderer.CrumblingOverlay aBreakProgress) {
		BlockEntityRenderer.super.extractRenderState(aBE, aState, aPartialTicks, aCameraPos, aBreakProgress); // база: blockPos/lightCoords/breakProgress
		aState.mQuads = null;
		aState.mSpecialRenderer = null; aState.mSpecialState = null;
		Block tBlock = aBE.getBlockState().getBlock();
		// Только MTE-блоки с render-объектом: руды(PrefixBlock/PrefixBlockTileEntity) и стабы(TileEntityLoaderStub, render-данных нет) → baked/пусто.
		// BUG-063: рамку отсечения гейт-отсеянным ставим ЗДЕСЬ — иначе она навсегда осталась бы неизвестной,
		// а неизвестная = «не отсекать» (см. getRenderBoundingBox). Их геометрия — обычный куб блока.
		if (aBE.getLevel() == null || !(aBE instanceof IRenderedBlockObject tRenderer) || !(tBlock instanceof MultiTileEntityBlock)) {
			aBE.mRenderAABB = new net.minecraft.world.phys.AABB(aBE.getBlockPos());
			return;
		}
		@SuppressWarnings("rawtypes") BlockEntityRenderer tSpecial = SPECIAL_RENDERERS.get(aBE.getClass());
		// BUG-138 — ГЛАВНЫЙ ГЕЙТ. Облик MTE собран в мэше секции (GT6BlockModel.collectParts), поэтому строить и
		// заливать его здесь ещё раз — чистая двойная работа каждый кадр. Живая геометрия нужна ровно двум:
		// ломаемому блоку (по ней ниже эмитятся трещины) и классам со своим покадровым рендерером (сундук,
		// масс-сторадж — те самые два TESR 1.7.10). Рамка = куб блока: рисунка BE у остальных больше нет,
		// а мэш секции отсекается своей секцией 16³ — как в 1.7.10.
		if (tSpecial == null && aBreakProgress == null) {
			aBE.mRenderAABB = new net.minecraft.world.phys.AABB(aBE.getBlockPos());
			return;
		}
		BlockPos tPos = aBE.getBlockPos();
		// Рамку куба ставим только тем, у кого своего покадрового рендерера НЕТ. У сундука и масс-стоража она
		// остаётся неизвестной (= не отсекать, см. getRenderBoundingBox) — ровно как в 1.7.10, где у TESR рамка
		// по умолчанию была БЕСКОНЕЧНОЙ (recompSrc TileEntity:399-420), а их аниматика может выходить за блок.
		if (tSpecial == null) aBE.mRenderAABB = new net.minecraft.world.phys.AABB(tPos);
		// Живая геометрия — ТОЛЬКО ломаемому блоку: по ней submit кладёт трещины на фактическую форму (1:1 с 1.7.10,
		// RenderGlobal.drawBlockDamageTexture → тот же RendererBlockTextured). Спец-рендерер свою аниматику рисует сам.
		if (aBreakProgress != null) {
			sQuadExtracts.incrementAndGet();
			// BUG-106 №4: кэш-хит — облик с кадра построения не менялся, то есть НИ эпоха рендера (перешив атласа),
			// НИ штамп СВОЕЙ секции с того кадра не сдвинулись (волна 3 консолидации, п.2). Штамп 0 у ни разу не
			// помеченной секции — законное значение: первый кадр даёт промах (оттиск заведён Long.MIN_VALUE), после
			// него 0 == 0 и кэш живёт.
			long tSectionStamp = sectionStamp(tPos);
			if (aBE.mQuadCacheEpoch == sQuadEpoch && aBE.mQuadCacheSectionStamp == tSectionStamp) {
				aState.mQuads = aBE.mQuadCache;
				sQuadCacheHits.incrementAndGet();
			} else {
				GT6QuadBuilder tQB = new GT6QuadBuilder();
				try { GT6BlockModel.buildRendererQuads(tQB, tRenderer, tBlock, aBE.getLevel(), tPos.getX(), tPos.getY(), tPos.getZ()); } catch (Throwable e) {/* render-логика конкретного MTE не должна ронять кадр */}
				if (!tQB.isEmpty()) aState.mQuads = tQB.quads();
				// BUG-063: рамка = ФАКТИЧЕСКИ нарисованное этим BE (quads строятся в локальных координатах блока → сдвигаем в мир).
				float[] tDrawn = tQB.drawnBounds();
				if (tDrawn != null) aBE.mRenderAABB = new net.minecraft.world.phys.AABB(
					  tPos.getX() + Math.min(tDrawn[0], 0F), tPos.getY() + Math.min(tDrawn[1], 0F), tPos.getZ() + Math.min(tDrawn[2], 0F)
					, tPos.getX() + Math.max(tDrawn[3], 1F), tPos.getY() + Math.max(tDrawn[4], 1F), tPos.getZ() + Math.max(tDrawn[5], 1F));
				aBE.mQuadCache = aState.mQuads; // null = «квадов нет» — тоже кэшируется (валидность судят эпоха и штамп секции)
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
			final QuadInstance tQI = new QuadInstance(); // color=-1 (белый, не перетинтит baked-цвет quad'а); light из позиции блока
			tQI.setLightCoords(aState.lightCoords);
			// quads GT6QuadBuilder — уже в локальных координатах блока 0..1 (как baked-модель); PoseStack на submit уже в позиции блока.
			aNodes.submitCustomGeometry(aPoseStack, Sheets.cutoutBlockSheet(), (tPose, tBuffer) -> {
				for (BakedQuad tQuad : tQuads) tBuffer.putBakedQuad(tPose, tQuad, tQI);
			});
			// F3-render ТРЕЩИНЫ по ЖИВОЙ геометрии (репорт игрока: «в оригинале трещины ложились прямо на поверхность
			// трубы/камня/верёвки»): submitCustomGeometry crumbling не несёт (он только у submitModel,
			// ModelFeatureRenderer:112) → эмитим сами ТЕ ЖЕ quads через SheetedDecalTextureGenerator (UV из позиции,
			// как ванильный BE-crumbling) в DESTROY_TYPES[progress]. breakProgress кладёт движок
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
			try {aState.mSpecialRenderer.submit(aState.mSpecialState, aPoseStack, aNodes, aCamera);} catch (Throwable e) {/* спец-рендер не должен ронять кадр */}
	}
}

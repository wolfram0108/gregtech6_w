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

/**
 * F3-render спец-рендеры MTE (1.7.10 {@code ClientRegistry.bindTileEntitySpecialRenderer}) — и ТОЛЬКО они.
 *
 * <p><b>BUG-138 носитель №2.</b> Прежде этот {@link BlockEntityRenderer} строил и заливал геометрию ВСЕХ MTE
 * КАЖДЫЙ КАДР (303 класса: машины, трубы, камни, кусты, покрытия) — 38,85 % рендер-потока живого клиента,
 * из них 35,4 % на заливку вершин. Обоснованием служило «регион чанк-компиляции BE не отдаёт
 * (getBlockEntity=null 100%)»; живая проба (стенд {@code gt6meshgate}, 2026-08-21) показала обратное — регион,
 * собранный движковой фабрикой {@code RenderRegionCache.createRegion}, отдаёт ТОТ ЖЕ живой объект BE
 * ({@code RenderChunkRegion.getBlockEntity:51-56} → {@code RenderChunk:24-28}: копируется КАРТА, не сущности).
 * Поэтому облик MTE снова живёт в МЭШЕ СЕКЦИИ — его собирает {@link GT6BlockModel#collectParts0} тем же центром
 * {@link GT6BlockModel#buildRendererQuads}, ровно как в 1.7.10 ({@code MultiTileEntityBlock.getRenderType()} →
 * {@code RendererBlockTextured implements ISimpleBlockRenderingHandler}, оригинал {@code :295}).
 *
 * <p><b>Что осталось покадровым — 1:1 с оригиналом.</b> В 1.7.10 у GT6 было РОВНО ДВА покадровых рендерера
 * ({@code bindTileEntitySpecialRenderer}: сундук и масс-сторадж). Здесь их держит реестр
 * {@link #SPECIAL_RENDERERS} — диспетч по КЛАССУ внутри единого BER, потому что движок регистрирует рендерер
 * по {@code BlockEntityType}, а он у всех MTE один из двух. Всем прочим MTE {@link #render} НИЧЕГО не рисует.
 *
 * <p><b>Трещины разрушения.</b> Движок оборачивает буфер блок-сущности декалью ТОЛЬКО когда по её позиции идёт
 * разрушение ({@code LevelRenderer.java:1244-1255}: {@code destructionProgress.get(pos)} непуст →
 * {@code multibuffersource1} подменяется обёрткой {@code VertexMultiConsumer} поверх
 * {@code SheetedDecalTextureGenerator}; иначе передаётся сам {@code MultiBufferSource.BufferSource}). Этот
 * признак движок кладёт нам прямо в аргумент — по нему {@link #render} и решает, строить ли живую геометрию:
 * ломаемому блоку строит (трещины ложатся на ФАКТИЧЕСКУЮ форму трубы/камня/куста — 1:1 с 1.7.10, где
 * {@code RenderGlobal.drawBlockDamageTexture} звал тот же {@code RendererBlockTextured} с реальным миром),
 * остальным — нет.
 *
 * <p>Руды ({@code PrefixBlockTileEntity}) и стабы отсеиваются тем же гейтом, что и раньше.
 */
public class MultiTileEntityBER implements BlockEntityRenderer<TileEntityBase01Root> {

	public MultiTileEntityBER(BlockEntityRendererProvider.Context aContext) {/* per-BE геометрия строится в render; ресурсы контекста тут не нужны */}

	// BUG-138: переопределение дистанции СНЯТО. Оно стояло потому, что геометрия MTE шла через этот BER и
	// пропадала за движковыми 64 блоками; теперь она в мэше секции и рисуется на всю дальность прорисовки.
	// Дефолт движка (64 блока) — ровно 1:1 с 1.7.10, где TESR резался тем же радиусом
	// (TileEntity.getMaxRenderDistanceSquared() == 4096), а покадровыми были только сундук и масс-сторадж.

	// F3-render спец-рендеры (1.7.10 ClientRegistry.bindTileEntitySpecialRenderer = vanilla-диспетчер по КЛАССУ TE;
	// здесь BER регистрируется по BlockEntityType, а у всех MTE он ОДИН — MTE_TYPE) → диспетч по классу живёт здесь,
	// в едином BER: реестр класс→рендерер, render делегируется. Оба живых TESR GT6 (Chest/MassStorage) идут сюда.
	@SuppressWarnings("rawtypes")
	private static final java.util.Map<Class<?>, BlockEntityRenderer> SPECIAL_RENDERERS = new java.util.HashMap<>();
	public static void bindSpecialRenderer(Class<?> aTileEntityClass, @SuppressWarnings("rawtypes") BlockEntityRenderer aRenderer) {SPECIAL_RENDERERS.put(aTileEntityClass, aRenderer);}

	/** Диаг-счётчики кэша квадов (BUG-106 №4): вызовы рендера рендер-объектов / реальные пересборки / кэш-хиты. */
	public static final java.util.concurrent.atomic.AtomicLong sQuadExtracts = new java.util.concurrent.atomic.AtomicLong(), sQuadBuilds = new java.util.concurrent.atomic.AtomicLong(), sQuadCacheHits = new java.util.concurrent.atomic.AtomicLong();

	/** BUG-106 №4 — кэш квадов BER. Эпоха рендера: {@code allChanged()} (перешив атласа/моделей — F3+T, F3+A,
	 *  смена дистанции; кэшированные квады держат UV СТАРОГО атласа) рвёт ВСЕ кэши разом, O(1). Точечный сброс —
	 *  {@link #onSectionDirty}: та же воронка, которой движок помечает секции на перестройку, то есть ровно тот
	 *  сигнал, по которому в 1.7.10 пересобирался мэш с геометрией MTE (recompSrc RenderGlobal.markBlockForUpdate →
	 *  markBlockRangeForRenderUpdate ±1). Всё общение клиента о смене облика уже проходит через неё:
	 *  каждый receiveData*-диспетчер ({@code MultiTileEntityBlock:265-325}) кончается {@code WD.update} →
	 *  {@code ClientLevel.sendBlockUpdated} → {@code LevelRenderer.blockChanged} → setSectionDirty;
	 *  прямые setBlock и свет — туда же. Залипание кэша возможно лишь там, где залипал бы и мэш 1.7.10 — 1:1 по следствию. */
	public static long sQuadEpoch = 0;

	/** ШТАМП СЕКЦИИ — цена сигнала. Сигнал {@code setSectionDirty} для движка стоит один флаг
	 *  ({@code viewArea.setDirty}), поэтому он зовёт его ПАЧКАМИ: {@code setBlockDirty} крутит ±1 по трём осям
	 *  и бьёт 27 раз на ОДНО изменение блока ({@code LevelRenderer.java:2385-2390}), а приход чанка добавляет
	 *  свет ({@code ClientPacketListener.enableChunkLight:718-728} + {@code readSectionList:2310-2318}). Прежняя
	 *  редакция вешала на этот сигнал обход ВСЕХ блок-сущностей чанка — а их у GT6 сотни (перепись сейва
	 *  GT6WGTest: медиана 262, p90 959, максимум 2750 на чанк), и 26 из 27 обходов были дублем одной секции.
	 *  Приход одного чанка стоил в среднем 25,4 млн итераций карты — отсюда фризы в секунды на новых чанках.
	 *
	 *  <p>Работа снята с сигнала и отдана моменту рендера: сигнал лишь ПЕЧАТАЕТ секцию (инкремент, O(1)), а
	 *  валидность кэша каждый MTE проверяет сам, сверяя свой оттиск со штампом своей секции. Гранулярность и
	 *  момент инвалидации те же, что были (та же воронка, та же секция) — дешевеет только цена, поэтому 1:1
	 *  по следствию с мэшем 1.7.10 сохраняется.
	 *
	 *  <p>Смена мира карту не переживает: {@code LevelRenderer.setLevel} зовёт {@code allChanged()}
	 *  ({@code LevelRenderer.java:667-668}) → {@link #onRenderAllChanged} чистит её тем же движковым сигналом,
	 *  которым рвутся и эпохи. Отдельного механизма выгрузки не заводим. */
	private static final it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap SECTION_STAMP = new it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap();

	/** Печать секции (зовёт MixinLevelRenderer из setSectionDirty, main-thread). O(1) — ни чанка, ни его блок-сущностей. */
	public static void onSectionDirty(int aSectionX, int aSectionY, int aSectionZ) {
		SECTION_STAMP.addTo(net.minecraft.core.SectionPos.asLong(aSectionX, aSectionY, aSectionZ), 1L);
	}

	/** Штамп секции, в которой лежит позиция (0 = секцию не помечали ни разу — законное значение, см. render). */
	private static long sectionStamp(BlockPos aPos) {
		return SECTION_STAMP.get(net.minecraft.core.SectionPos.asLong(
			  net.minecraft.core.SectionPos.blockToSectionCoord(aPos.getX())
			, net.minecraft.core.SectionPos.blockToSectionCoord(aPos.getY())
			, net.minecraft.core.SectionPos.blockToSectionCoord(aPos.getZ())));
	}

	/** Полный сброс (зовёт MixinLevelRenderer из allChanged, main-thread): эпоха рвёт все кэши разом, штампы
	 *  секций теряют смысл вместе с ней (иначе карта росла бы от мира к миру). */
	public static void onRenderAllChanged() {sQuadEpoch++; SECTION_STAMP.clear();}

	public static boolean hasSpecialRenderer(Class<?> aTileEntityClass) {return SPECIAL_RENDERERS.containsKey(aTileEntityClass);}

	/** Item-форма TESR-классов: 1.7.10 renderItem звал renderTileEntityAt(this,0,0,0,0) на canonical-TE (данные из NBT
	 *  стека); носитель 1.20.1 — {@code BlockEntityWithoutLevelRenderer}, куда движок уходит, когда item-модель
	 *  объявила {@code isCustomRenderer()} ({@code ItemRenderer.render} → {@code IClientItemExtensions.getCustomRenderer()}).
	 *  Внутри — тот же зарегистрированный спец-рендерер (диспетч по классу), рисующий detached-BE. */
	/** Мост из common-класса предмета MTE ({@code MultiTileEntityItemInternal.initializeClient}) — тот же приём, что
	 *  {@code MTEChestRenderer.bindFirst}: клиентский код живёт в клиентском файле (BUG-092), common-класс зовёт его
	 *  ленивым invokestatic. Через этот канал движок и находит BEWLR item-формы. */
	public static void bindItemExtensions(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> aConsumer) {
		aConsumer.accept(new net.minecraftforge.client.extensions.common.IClientItemExtensions() {
			@Override public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() {return specialItemForm();}
		});
	}

	private static SpecialItemForm sItemForm;
	/** Ленивая выдача BEWLR: конструктор {@code BlockEntityWithoutLevelRenderer} требует живых узлов Minecraft,
	 *  а класс BER грузится раньше их готовности — инстанс создаём при первом обращении с клиента. */
	public static SpecialItemForm specialItemForm() {
		if (sItemForm == null) sItemForm = new SpecialItemForm();
		return sItemForm;
	}

	/** BEWLR item-формы MTE со спец-рендером. Отдельный класс, потому что 1.20.1 требует наследника
	 *  {@link net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer}, а не анонимного интерфейса. */
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
			} catch (Throwable e) {/* item-форма не должна ронять рендер */}
		}

	}

	/** Стек → canonical-TE со спец-рендером (или null). Тот же центр рождения detached-TE, что у обычного item-рендера.
	 *  Статический: item-модель спрашивает «нужен ли спец-рендер» ДО того, как BEWLR понадобится. */
	public static net.minecraft.world.level.block.entity.BlockEntity extractSpecialItemForm(net.minecraft.world.item.ItemStack aStack) {
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

	/** BUG-138 — ПРИЗНАК «по этому блоку идёт разрушение», который движок кладёт нам прямо в аргумент.
	 *  {@code LevelRenderer:1244-1255}: обычному BE передаётся сам {@code MultiBufferSource.BufferSource}
	 *  ({@code renderBuffers.bufferSource()}), и ТОЛЬКО когда {@code destructionProgress.get(pos)} непуст —
	 *  вместо него лямбда-обёртка, домешивающая {@code SheetedDecalTextureGenerator} в типы с
	 *  {@code affectsCrumbling()}. Своего реестра ломаемых позиций не заводим (он был бы копией движкового и
	 *  умел бы рассинхронизироваться): условие читаем там же, где его выставил движок. */
	private static boolean isCrumbling(MultiBufferSource aBuffer) {
		return !(aBuffer instanceof MultiBufferSource.BufferSource);
	}

	@Override
	public void render(TileEntityBase01Root aBE, float aPartialTicks, PoseStack aPoseStack, MultiBufferSource aBuffer, int aLight, int aOverlay) {
		Block tBlock = aBE.getBlockState().getBlock();
		// Только MTE-блоки с render-объектом: руды(PrefixBlock/PrefixBlockTileEntity) и стабы(TileEntityLoaderStub,
		// render-данных нет) → baked/пусто. BUG-063: рамку отсечения гейт-отсеянным ставим ЗДЕСЬ — иначе она
		// навсегда осталась бы неизвестной, а неизвестная = «не отсекать». Их геометрия — обычный куб блока.
		if (aBE.getLevel() == null || !(aBE instanceof IRenderedBlockObject tRenderer) || !(tBlock instanceof MultiTileEntityBlock)) {
			aBE.mRenderAABB = new net.minecraft.world.phys.AABB(aBE.getBlockPos());
			return;
		}
		@SuppressWarnings("rawtypes") BlockEntityRenderer tSpecialRenderer = SPECIAL_RENDERERS.get(aBE.getClass());
		// BUG-138 — ГЛАВНЫЙ ГЕЙТ. Облик MTE собран в мэше секции (GT6BlockModel.collectParts0), поэтому строить и
		// заливать его здесь ещё раз — чистая двойная работа каждый кадр. Живая геометрия нужна ровно двум:
		// ломаемому блоку (движок домешивает по ней трещины) и классам со своим покадровым рендерером
		// (сундук, масс-сторадж — те самые два TESR 1.7.10). Рамка = куб блока: рисунка BE у остальных больше нет,
		// а мэш секции отсекается своей секцией 16³ — как в 1.7.10.
		if (tSpecialRenderer == null && !isCrumbling(aBuffer)) {
			aBE.mRenderAABB = new net.minecraft.world.phys.AABB(aBE.getBlockPos());
			return;
		}
		BlockPos tPos = aBE.getBlockPos();
		// Рамку куба ставим только тем, у кого своего покадрового рендерера НЕТ. У сундука и масс-стоража она
		// остаётся неизвестной (= не отсекать) — ровно как в 1.7.10, где у TESR рамка по умолчанию была
		// БЕСКОНЕЧНОЙ (recompSrc TileEntity:399-420), а их аниматика может выходить за блок.
		if (tSpecialRenderer == null) aBE.mRenderAABB = new net.minecraft.world.phys.AABB(tPos);
		// Живая геометрия — ТОЛЬКО ломаемому блоку: по ней движок кладёт трещины на фактическую форму (1:1 с 1.7.10,
		// RenderGlobal.drawBlockDamageTexture → тот же RendererBlockTextured). Спец-рендерер свою аниматику рисует сам.
		if (isCrumbling(aBuffer)) emitLiveQuads(aBE, tRenderer, tBlock, tPos, aPoseStack, aBuffer, aLight, aOverlay);
		if (tSpecialRenderer != null) try {
			tSpecialRenderer.render(aBE, aPartialTicks, aPoseStack, aBuffer, aLight, aOverlay);
		} catch (Throwable e) {/* спец-рендер не должен ронять кадр */}
	}

	/** Живая геометрия MTE, собранная тем же центром {@link GT6BlockModel#buildRendererQuads}, что и мэш секции.
	 *  Зовётся только для блока под разрушением — прочим облик даёт мэш. */
	private void emitLiveQuads(TileEntityBase01Root aBE, IRenderedBlockObject tRenderer, Block tBlock, BlockPos tPos, PoseStack aPoseStack, MultiBufferSource aBuffer, int aLight, int aOverlay) {
		sQuadExtracts.incrementAndGet();
		List<BakedQuad> tQuads;
		// BUG-106 №4: кэш-хит — облик с кадра построения не менялся, то есть НИ эпоха рендера (перешив атласа),
		// НИ штамп СВОЕЙ секции с того кадра не сдвинулись. Штамп 0 у ни разу не помеченной секции — законное
		// значение: первый кадр даёт промах (оттиск заведён Long.MIN_VALUE), после него 0 == 0 и кэш живёт.
		// mRenderAABB остаётся от кадра построения, спец-рендер (аниматика TESR) идёт живым ниже, как всегда.
		long tSectionStamp = sectionStamp(tPos);
		if (aBE.mQuadCacheEpoch == sQuadEpoch && aBE.mQuadCacheSectionStamp == tSectionStamp) {
			tQuads = aBE.mQuadCache;
			sQuadCacheHits.incrementAndGet();
		} else {
			aBE.mRenderAABB = new net.minecraft.world.phys.AABB(tPos);
			GT6QuadBuilder tQB = new GT6QuadBuilder();
			try { GT6BlockModel.buildRendererQuads(tQB, tRenderer, tBlock, aBE.getLevel(), tPos.getX(), tPos.getY(), tPos.getZ()); } catch (Throwable e) {/* render-логика конкретного MTE не должна ронять кадр */}
			tQuads = tQB.isEmpty() ? null : tQB.quads();
			// BUG-063: рамка = ФАКТИЧЕСКИ нарисованное этим BE (quads строятся в локальных координатах блока → сдвигаем в мир).
			float[] tDrawn = tQB.drawnBounds();
			if (tDrawn != null) aBE.mRenderAABB = new net.minecraft.world.phys.AABB(
				  tPos.getX() + Math.min(tDrawn[0], 0F), tPos.getY() + Math.min(tDrawn[1], 0F), tPos.getZ() + Math.min(tDrawn[2], 0F)
				, tPos.getX() + Math.max(tDrawn[3], 1F), tPos.getY() + Math.max(tDrawn[4], 1F), tPos.getZ() + Math.max(tDrawn[5], 1F));
			aBE.mQuadCache = tQuads; // null = «квадов нет» — тоже кэшируется (валидность судят эпоха и штамп секции)
			aBE.mQuadCacheEpoch = sQuadEpoch;
			aBE.mQuadCacheSectionStamp = tSectionStamp;
			sQuadBuilds.incrementAndGet();
		}
		if (tQuads != null && !tQuads.isEmpty()) {
			// quads GT6QuadBuilder — уже в локальных координатах блока 0..1 (как baked-модель); PoseStack на входе
			// уже сдвинут в позицию блока (LevelRenderer:1244). cutoutBlockSheet несёт affectsCrumbling=true —
			// трещины разрушения движок домешивает сам (см. javadoc класса), вручную их эмитить не нужно.
			VertexConsumer tConsumer = aBuffer.getBuffer(Sheets.cutoutBlockSheet());
			PoseStack.Pose tPose = aPoseStack.last();
			// КОРЕНЬ «все MTE в мире серые» (репорт игрока 2026-08-13, судья gt6colorprobe): короткая перегрузка
			// putBulkData в 1.20.1 делегирует с readExistingColor=false (VertexConsumer.java:62-64) — запечённый в
			// вершины цвет материала ОТБРАСЫВАЛСЯ. Длинная перегрузка с true читает цвет вершин (:93-99), как чанк-путь
			// (ModelBlockRenderer.putQuadData:150). Свет тем же квартетом, множители 1 — цвет уже в вершинах.
			int[] tLights = {aLight, aLight, aLight, aLight};
			float[] tBrightness = {1F, 1F, 1F, 1F};
			for (BakedQuad tQuad : tQuads) tConsumer.putBulkData(tPose, tQuad, tBrightness, 1F, 1F, 1F, tLights, aOverlay, true);
		}
	}
}

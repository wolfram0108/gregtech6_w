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
 * F3-render КОРЕНЬ прозрачности MTE (машины/трубы/декор): geometry MTE-блоков живёт на клиент-BE (IRenderedBlockObject:
 * getRenderPasses/getTexture/box), а neo section-compile регион (worker-снапшот) BE НЕ отдаёт (доказано probe:
 * getBlockEntity=null 100%), и ModelData-мост удалён в 26.x. Потому MTE рисуются НЕ через baked {@link GT6BlockModel}
 * (он их пропускает), а через ЭТОТ BlockEntityRenderer: {@link #extractRenderState} на main-thread берёт ЖИВОЙ BE и строит
 * quads той же логикой GT6 ({@link GT6BlockModel#buildRendererQuads}, переиспользование 1:1), {@link #submit} эмитит их через
 * {@code submitCustomGeometry}. Один generic BER на весь {@code MTE_TYPE} — централизация 1:1 (аналог единого GT6-рендерера).
 * Руды ({@code PrefixBlockTileEntity}) и стабы отсеиваются гейтом (их рисует baked-модель). См. память gt6-neoforge-2612 п.7.
 */
public class MultiTileEntityBER implements BlockEntityRenderer<TileEntityBase01Root, MultiTileEntityBER.MTERenderState> {

	public MultiTileEntityBER(BlockEntityRendererProvider.Context aContext) {/* per-BE геометрия строится в extractRenderState; ресурсы контекста тут не нужны */}

	/** F3-render дистанция (репорт игрока: MTE «пропадают вдалеке»): дефолт BER = 64 блока, но в 1.7.10 MTE были
	 *  chunk-геометрией и рисовались на ВСЮ дистанцию прорисовки. 1:1 по следствию: радиус = renderDistance чанков. */
	@Override public int getViewDistance() {
		return net.minecraft.client.Minecraft.getInstance().options.renderDistance().get() * 16;
	}

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

	/** Диаг-счётчики судьи П2 (спец-рендер реально вызван движком). */
	public static final java.util.concurrent.atomic.AtomicLong sSpecialExtract = new java.util.concurrent.atomic.AtomicLong(), sSpecialSubmit = new java.util.concurrent.atomic.AtomicLong(), sSpecialItemForm = new java.util.concurrent.atomic.AtomicLong();
	/** Диаг-счётчики кэша квадов (BUG-106 №4): extract'ы рендер-объектов / реальные пересборки / кэш-хиты. */
	public static final java.util.concurrent.atomic.AtomicLong sQuadExtracts = new java.util.concurrent.atomic.AtomicLong(), sQuadBuilds = new java.util.concurrent.atomic.AtomicLong(), sQuadCacheHits = new java.util.concurrent.atomic.AtomicLong();

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

	/** Сброс кэша квадов у всех MTE секции (зовёт MixinLevelRenderer из setSectionDirty, main-thread). */
	public static void onSectionDirty(net.minecraft.client.multiplayer.ClientLevel aLevel, int aSectionX, int aSectionY, int aSectionZ) {
		net.minecraft.world.level.chunk.LevelChunk tChunk = gregapi.util.WD.chunkNow(aLevel, aSectionX, aSectionZ);
		if (tChunk == null) return;
		int tMinY = aSectionY << 4, tMaxY = tMinY + 15;
		for (java.util.Map.Entry<BlockPos, net.minecraft.world.level.block.entity.BlockEntity> tEntry : tChunk.getBlockEntities().entrySet()) {
			int tY = tEntry.getKey().getY();
			if (tY >= tMinY && tY <= tMaxY && tEntry.getValue() instanceof TileEntityBase01Root tBE) tBE.mQuadCacheEpoch = Long.MIN_VALUE;
		}
	}

	/** Полный сброс (зовёт MixinLevelRenderer из allChanged, main-thread). */
	public static void onRenderAllChanged() {sQuadEpoch++;}
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
				sSpecialItemForm.incrementAndGet();
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
		sQuadExtracts.incrementAndGet();
		// BUG-106 №4: кэш-хит — облик с кадра построения не менялся (сигнала setSectionDirty не было, см. sQuadEpoch
		// выше). mRenderAABB остаётся от кадра построения, спец-рендер (аниматика TESR) идёт живым ниже, как всегда.
		if (aBE.mQuadCacheEpoch == sQuadEpoch) {
			aState.mQuads = aBE.mQuadCache;
			sQuadCacheHits.incrementAndGet();
		} else {
			aBE.mRenderAABB = new net.minecraft.world.phys.AABB(aBE.getBlockPos());
			BlockPos tPos = aBE.getBlockPos();
			GT6QuadBuilder tQB = new GT6QuadBuilder();
			try { GT6BlockModel.buildRendererQuads(tQB, tRenderer, tBlock, aBE.getLevel(), tPos.getX(), tPos.getY(), tPos.getZ()); } catch (Throwable e) {/* render-логика конкретного MTE не должна ронять кадр */}
			if (!tQB.isEmpty()) aState.mQuads = tQB.quads();
			// BUG-063: рамка = ФАКТИЧЕСКИ нарисованное этим BE (quads строятся в локальных координатах блока → сдвигаем в мир).
			float[] tDrawn = tQB.drawnBounds();
			if (tDrawn != null) aBE.mRenderAABB = new net.minecraft.world.phys.AABB(
				  tPos.getX() + Math.min(tDrawn[0], 0F), tPos.getY() + Math.min(tDrawn[1], 0F), tPos.getZ() + Math.min(tDrawn[2], 0F)
				, tPos.getX() + Math.max(tDrawn[3], 1F), tPos.getY() + Math.max(tDrawn[4], 1F), tPos.getZ() + Math.max(tDrawn[5], 1F));
			aBE.mQuadCache = aState.mQuads; // null = «квадов нет» — тоже кэшируется (валидность судит эпоха)
			aBE.mQuadCacheEpoch = sQuadEpoch;
			sQuadBuilds.incrementAndGet();
		}
		@SuppressWarnings("rawtypes") BlockEntityRenderer tSpecial = SPECIAL_RENDERERS.get(aBE.getClass());
		if (tSpecial != null) try {
			aState.mSpecialRenderer = tSpecial;
			aState.mSpecialState = (BlockEntityRenderState)tSpecial.createRenderState();
			tSpecial.extractRenderState(aBE, aState.mSpecialState, aPartialTicks, aCameraPos, aBreakProgress);
			sSpecialExtract.incrementAndGet();
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
			try {aState.mSpecialRenderer.submit(aState.mSpecialState, aPoseStack, aNodes, aCamera); sSpecialSubmit.incrementAndGet();} catch (Throwable e) {/* спец-рендер не должен ронять кадр */}
	}
}

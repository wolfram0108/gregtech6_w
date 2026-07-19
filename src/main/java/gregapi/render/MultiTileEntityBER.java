/**
 * Copyright (c) 2026 GregTech-6 Team
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

	// F3-render спец-рендеры (1.7.10 ClientRegistry.bindTileEntitySpecialRenderer = vanilla-диспетчер по КЛАССУ TE;
	// в neo BER регистрируется по BlockEntityType, а у всех MTE он ОДИН — MTE_TYPE) → диспетч по классу живёт здесь,
	// в едином BER: реестр класс→рендерер, extract/submit делегируются. Оба живых TESR GT6 (Chest/MassStorage) идут сюда.
	@SuppressWarnings("rawtypes")
	private static final java.util.Map<Class<?>, BlockEntityRenderer> SPECIAL_RENDERERS = new java.util.HashMap<>();
	public static void bindSpecialRenderer(Class<?> aTileEntityClass, @SuppressWarnings("rawtypes") BlockEntityRenderer aRenderer) {SPECIAL_RENDERERS.put(aTileEntityClass, aRenderer);}

	/** Диаг-счётчики судьи П2 (спец-рендер реально вызван движком). */
	public static final java.util.concurrent.atomic.AtomicLong sSpecialExtract = new java.util.concurrent.atomic.AtomicLong(), sSpecialSubmit = new java.util.concurrent.atomic.AtomicLong(), sSpecialItemForm = new java.util.concurrent.atomic.AtomicLong();

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
					if (tCont != null && tCont.mTileEntity != null && SPECIAL_RENDERERS.containsKey(tCont.mTileEntity.getClass())) return tCont.mTileEntity;
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
		if (aBE.getLevel() == null || !(aBE instanceof IRenderedBlockObject tRenderer) || !(tBlock instanceof MultiTileEntityBlock)) return;
		BlockPos tPos = aBE.getBlockPos();
		GT6QuadBuilder tQB = new GT6QuadBuilder();
		try { GT6BlockModel.buildRendererQuads(tQB, tRenderer, tBlock, aBE.getLevel(), tPos.getX(), tPos.getY(), tPos.getZ()); } catch (Throwable e) {/* render-логика конкретного MTE не должна ронять кадр */}
		if (!tQB.isEmpty()) aState.mQuads = tQB.quads();
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
		}
		if (aState.mSpecialRenderer != null && aState.mSpecialState != null)
			try {aState.mSpecialRenderer.submit(aState.mSpecialState, aPoseStack, aNodes, aCamera); sSpecialSubmit.incrementAndGet();} catch (Throwable e) {/* спец-рендер не должен ронять кадр */}
	}
}

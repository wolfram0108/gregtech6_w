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

	/** Снапшот геометрии, собранной на main-thread (thread-safe: submit его лишь читает). */
	public static class MTERenderState extends BlockEntityRenderState {
		public List<BakedQuad> mQuads;
	}

	@Override public MTERenderState createRenderState() {return new MTERenderState();}

	@Override
	public void extractRenderState(TileEntityBase01Root aBE, MTERenderState aState, float aPartialTicks, Vec3 aCameraPos, ModelFeatureRenderer.CrumblingOverlay aBreakProgress) {
		BlockEntityRenderer.super.extractRenderState(aBE, aState, aPartialTicks, aCameraPos, aBreakProgress); // база: blockPos/lightCoords/breakProgress
		aState.mQuads = null;
		Block tBlock = aBE.getBlockState().getBlock();
		// Только MTE-блоки с render-объектом: руды(PrefixBlock/PrefixBlockTileEntity) и стабы(TileEntityLoaderStub, render-данных нет) → baked/пусто.
		if (aBE.getLevel() == null || !(aBE instanceof IRenderedBlockObject tRenderer) || !(tBlock instanceof MultiTileEntityBlock)) return;
		BlockPos tPos = aBE.getBlockPos();
		GT6QuadBuilder tQB = new GT6QuadBuilder();
		try { GT6BlockModel.buildRendererQuads(tQB, tRenderer, tBlock, aBE.getLevel(), tPos.getX(), tPos.getY(), tPos.getZ()); } catch (Throwable e) {/* render-логика конкретного MTE не должна ронять кадр */}
		if (!tQB.isEmpty()) aState.mQuads = tQB.quads();
	}

	@Override
	public void submit(MTERenderState aState, PoseStack aPoseStack, SubmitNodeCollector aNodes, CameraRenderState aCamera) {
		final List<BakedQuad> tQuads = aState.mQuads;
		if (tQuads == null || tQuads.isEmpty()) return;
		final QuadInstance tQI = new QuadInstance(); // color=-1 (белый, не перетинтит baked-цвет quad'а); light из позиции блока
		tQI.setLightCoords(aState.lightCoords);
		// quads GT6QuadBuilder — уже в локальных координатах блока 0..1 (как baked-модель); PoseStack на submit уже в позиции блока.
		aNodes.submitCustomGeometry(aPoseStack, Sheets.cutoutBlockSheet(), (tPose, tBuffer) -> {
			for (BakedQuad tQuad : tQuads) tBuffer.putBakedQuad(tPose, tQuad, tQI);
		});
	}
}

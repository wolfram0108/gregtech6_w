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

import java.util.HashMap;
import java.util.Map;

import com.mojang.blaze3d.vertex.PoseStack;

import gregapi.block.multitileentity.example.MultiTileEntityChest;
import gregapi.data.MD;
import gregapi.util.UT;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.resources.ResourceLocation;

import static gregapi.data.CS.*;

/**
 * BUG-092 (дедикейт: «нет камней и палок»): спец-рендер сундука ВЫНЕСЕН из common-класса
 * {@link MultiTileEntityChest}. Клиентские типы во вложенных классах/полях/телах методов common-MTE
 * валили ЛИНКОВКУ класса на выделенном сервере ({@code NoClassDefFoundError:
 * net.minecraft.client.renderer.blockentity.BlockEntityRenderer} при {@code Class.newInstance} в
 * {@code MultiTileEntityClassContainer:52}) — обрывался ВЕСЬ {@code Loader_MultiTileEntities} на первой
 * же регистрации (Chest) → MTE-регистраций 0 → в мире ни камней/палок/родников/ульев, ни машин.
 * В 1.7.10 эти члены вырезал из серверного класса {@code @SideOnly(CLIENT)} — neo-эквивалент приёма:
 * клиентский код живёт в клиентском файле, common-класс зовёт его ленивыми invokestatic-мостами
 * ({@code bindFirst}/{@code bindTexture}) только из onRegistration*Client (клиент-only канал).
 * Содержимое рендера/модели — 1:1 перенос, ни одна величина не менялась.
 */
public class MTEChestRenderer implements BlockEntityRenderer<MultiTileEntityChest> {

	private static MTEChestRenderer RENDERER;

	/** Мост {@code MultiTileEntityChest.onRegistrationFirstClient} (клиент-only вызов): было
	 *  ClientRegistry.bindTileEntitySpecialRenderer → тот же диспетч по классу в едином GT6-BER. */
	public static void bindFirst(Class<?> aClass) {
		MultiTileEntityBER.bindSpecialRenderer(aClass, RENDERER = new MTEChestRenderer());
	}

	/** Мост {@code MultiTileEntityChest.onRegistrationClient}: регистрация пары текстур .colored/.plain. */
	public static void bindTexture(String aTextureName, String aRegistryNameInternal) {
		RENDERER.mResources.put(aTextureName, new ResourceLocation[] {ResourceLocation.fromNamespaceAndPath(MD.GT.mID, TEX_DIR_MODEL + aRegistryNameInternal + "/" + aTextureName + ".colored.png"), ResourceLocation.fromNamespaceAndPath(MD.GT.mID, TEX_DIR_MODEL + aRegistryNameInternal + "/" + aTextureName + ".plain.png")});
	}

	private static final MultiTileEntityModelChest sModel = new MultiTileEntityModelChest();
	public final Map<String, ResourceLocation[]> mResources = new HashMap<>();

	/** Ветка 1.20.1: BER однопараметрический и без render-state — сбор величин и отрисовка снова в одном вызове,
	 *  как в 1.7.10 {@code renderTileEntityAt}. */
	@Override
	public void render(MultiTileEntityChest aChest, float aPartialTick, PoseStack aPoseStack, MultiBufferSource aBuffer, int aLight, int aOverlay) {
		ResourceLocation[] tLocation = mResources.get(aChest.mTextureName);
		if (tLocation == null || tLocation.length < 2) return;
		// 1.7.10 renderTileEntityAt: интерполяция крышки + кубическая кривая — дословно.
		double tLidAngle = 1 - (aChest.oLidAngle + (aChest.mLidAngle - aChest.oLidAngle) * aPartialTick); tLidAngle = -(((1 - tLidAngle*tLidAngle*tLidAngle) * Math.PI) / 2);
		// BUG-078: для item-формы facing уже подставлен центром applyItemFacing (величина — getItemFacing)
		byte tFacing = aChest.mFacing;
		// матрицы 1:1 с 1.7.10 (translate(0,1,1)+scale(1,-1,-1) — модель и текстуры в перевёрнутой системе 1.7.10)
		aPoseStack.pushPose();
		aPoseStack.translate(0, 1, 1);
		aPoseStack.scale(1, -1, -1);
		aPoseStack.translate(0.5f, 0.5f, 0.5f);
		aPoseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(COMPASS_FROM_SIDE[tFacing] * 90 - 180));
		aPoseStack.translate(-0.5f, -0.5f, -0.5f);
		short[] tRGBa = UT.Code.getRGBaArray(aChest.mRGBa);
		// пасс 1: .colored.png с тинтом mRGBa; пасс 2: .plain.png белым (blend+alpha 1.7.10 → entityCutout)
		sModel.render(aBuffer, aPoseStack, tLocation[0], (float)tLidAngle, aLight, aOverlay, tRGBa[0] / 255F, tRGBa[1] / 255F, tRGBa[2] / 255F);
		sModel.render(aBuffer, aPoseStack, tLocation[1], (float)tLidAngle, aLight, aOverlay, 1F, 1F, 1F);
		aPoseStack.popPose();
	}

	/** Модель сундука 1:1 (боксы/rotationPoints/texOffs дословно из 1.7.10 ModelBase-версии; ModelPart — носитель ModelRenderer). */
	public static class MultiTileEntityModelChest {
		private final net.minecraft.client.model.geom.ModelPart mRoot, mLid, mKnob;

		public MultiTileEntityModelChest() {
			net.minecraft.client.model.geom.builders.MeshDefinition tMesh = new net.minecraft.client.model.geom.builders.MeshDefinition();
			net.minecraft.client.model.geom.builders.PartDefinition tRoot = tMesh.getRoot();
			tRoot.addOrReplaceChild("lid",    net.minecraft.client.model.geom.builders.CubeListBuilder.create().texOffs(0,  0).addBox( 0, -5, -14, 14,  5, 14), net.minecraft.client.model.geom.PartPose.offset(1, 7, 15));
			tRoot.addOrReplaceChild("knob",   net.minecraft.client.model.geom.builders.CubeListBuilder.create().texOffs(0,  0).addBox(-1, -2, -15,  2,  4,  1), net.minecraft.client.model.geom.PartPose.offset(8, 7, 15));
			tRoot.addOrReplaceChild("bottom", net.minecraft.client.model.geom.builders.CubeListBuilder.create().texOffs(0, 19).addBox( 0,  0,   0, 14, 10, 14), net.minecraft.client.model.geom.PartPose.offset(1, 6, 1));
			mRoot = net.minecraft.client.model.geom.builders.LayerDefinition.create(tMesh, 64, 64).bakeRoot();
			mLid  = mRoot.getChild("lid");
			mKnob = mRoot.getChild("knob");
		}

		public void render(MultiBufferSource aBuffer, PoseStack aPoseStack, ResourceLocation aTexture, float aLidAngle, int aLight, int aOverlay, float aR, float aG, float aB) {
			// BUG-059: угол крышки ставится ПЕРЕД самой отрисовкой. Модель одна (static) на все сундуки; в 1.7.10
			// рендер был immediate-mode (записал угол → тут же нарисовал) — в 1.20.1 отрисовка тоже немедленная
			// (BER пишет в буфер прямо здесь), поэтому семантика сохранена без отложенных лямбд 26.x.
			mKnob.xRot = mLid.xRot = aLidAngle;
			mRoot.render(aPoseStack, aBuffer.getBuffer(RenderType.entityCutout(aTexture)), aLight, aOverlay, aR, aG, aB, 1F);
		}
	}
}

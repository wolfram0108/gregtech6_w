/**
 * Copyright (c) 2022 GregTech-6 Team
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
 *
 * Modified in 2026 for the GregTech 6 NeoForge port
 * (https://github.com/wolfram0108/gregtech6_w): ported from Minecraft 1.7.10 / Forge
 * to Minecraft 26.1.2 / NeoForge.
 */

package gregtech.render;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.event.RenderPlayerEvent;

import java.util.Collection;

import static gregapi.data.CS.RES_PATH_MODEL;

/**
 * Ветка 1.20.1: восстановлена ФОРМА ОРИГИНАЛА — GT6 рисует СВОЙ плащ собственной геометрией поверх
 * игрока, а не подменяет ванильный. Оригинал ({@code gt6-original PlayerModelRenderer.java:76-111})
 * делал {@code glTranslatef(0,0,0.125)}, считал наклон по интерполированным {@code field_71091_bM..}
 * и звал {@code ModelBiped.renderCloak}. В 1.20.1 доступны ровно те же три составляющие:
 * {@code RenderPlayerEvent.Pre} несёт {@code PoseStack}/{@code MultiBufferSource}/{@code packedLight}
 * ({@code RenderPlayerEvent.java:65,73,83}), поля наклона плаща у игрока публичны
 * ({@code xCloak/yCloak/zCloak} и их {@code O}-версии), а {@code PlayerModel.renderCloak(PoseStack,
 * VertexConsumer,int,int)} публичен ({@code PlayerModel.java:90-92}). Формулы наклона взяты дословно
 * из движкового канона {@code CapeLayer.render} ({@code CapeLayer.java:26-59}) — он и есть тот же
 * расчёт, что стоял в оригинале построчно.
 *
 * <p>Условия оригинала сохранены: невидимость ({@code isInvisible}) и «скрыть плащ»
 * ({@code getHideCape()} → {@code isModelPartShown(PlayerModelPart.CAPE)}). Модель 26.x-ветки (подмена
 * {@code PlayerSkin} в render-state) снята вместе с типами, которых в 1.20.1 нет; вместе с ней ушло и
 * её осознанное отличие «свой плащ игрока побеждает GT6-шный» — теперь, как у Грегориуса, GT6-плащ
 * рисуется поверх.</p>
 */
public class PlayerModelRenderer {
	// neo ResourceLocation.assertValidPath запрещает заглавные в path (1.7.10 ResourceLocation их допускал) — имена
	// плащей-текстур приведены к lowercase (файлы переименованы синхронно). Порядок/логика выбора плаща 1:1.
	private final ResourceLocation[] mResources = new ResourceLocation[] {ResourceLocation.parse(RES_PATH_MODEL + "braintech.png"), ResourceLocation.parse(RES_PATH_MODEL + "silver.png"), ResourceLocation.parse(RES_PATH_MODEL + "mrbrain.png"), ResourceLocation.parse(RES_PATH_MODEL + "dev.png"), ResourceLocation.parse(RES_PATH_MODEL + "gold.png"), ResourceLocation.parse(RES_PATH_MODEL + "crazy.png"), ResourceLocation.parse(RES_PATH_MODEL + "sus.png")};
	private final Collection<String> mSupporterListSilver, mSupporterListGold;

	public PlayerModelRenderer(Collection<String> aSupporterListSilver, Collection<String> aSupporterListGold) {
		mSupporterListSilver = aSupporterListSilver;
		mSupporterListGold   = aSupporterListGold;
	}

	private ResourceLocation getResource(String aPlayer) {
		aPlayer = aPlayer.toLowerCase();
		// I sure as fuck won't make a Microsoft Account!
		if (aPlayer.startsWith("gregori"))            return mResources[6];
		// GT6 Team
		if (aPlayer.equalsIgnoreCase("GregoriusT"))   return mResources[6];
		if (aPlayer.equalsIgnoreCase("OvermindDL1"))  return mResources[3];
		// GT6U Team
		if (aPlayer.equalsIgnoreCase("jihuayu123"))   return mResources[3];
		if (aPlayer.equalsIgnoreCase("Yuesha_Kev14")) return mResources[3];
		if (aPlayer.equalsIgnoreCase("Evanvenir"))    return mResources[3];
		// This "special" Cape is totally just to mess with her. XD
		if (aPlayer.equalsIgnoreCase("CrazyJ1984"))   return mResources[5];
		// People who helped back in ancient GT Versions.
		if (aPlayer.equalsIgnoreCase("Mr_Brain"))     return mResources[2];
		if (aPlayer.equalsIgnoreCase("Friedi4321"))   return mResources[0];
		// Supporter Lists
		if (mSupporterListGold  .contains(aPlayer))   return mResources[4];
		if (mSupporterListSilver.contains(aPlayer))   return mResources[1];
		return null;
	}

	/** Плащ GT6 — своей геометрией поверх игрока, дословно как в оригинале (разбор в javadoc класса). */
	public void receiveRenderSpecialsEvent(RenderPlayerEvent.Pre aEvent) {
		try {
			if (!(aEvent.getEntity() instanceof net.minecraft.client.player.AbstractClientPlayer aPlayer)) return;
			if (aPlayer.isInvisible() || !aPlayer.isModelPartShown(net.minecraft.world.entity.player.PlayerModelPart.CAPE)) return;

			// имя — getScoreboardName(): у Player это имя профиля (приём проекта, EnchantmentEffect_Werewolf:56)
			ResourceLocation tResource = getResource(aPlayer.getScoreboardName());
			if (tResource == null) tResource = getResource(aPlayer.getUUID().toString());
			if (tResource == null) return;

			float aPartialTicks = aEvent.getPartialTick();
			com.mojang.blaze3d.vertex.PoseStack tPose = aEvent.getPoseStack();
			tPose.pushPose();
			tPose.translate(0.0F, 0.0F, 0.125F);
			double d0 = net.minecraft.util.Mth.lerp((double)aPartialTicks, aPlayer.xCloakO, aPlayer.xCloak) - net.minecraft.util.Mth.lerp((double)aPartialTicks, aPlayer.xo, aPlayer.getX());
			double d1 = net.minecraft.util.Mth.lerp((double)aPartialTicks, aPlayer.yCloakO, aPlayer.yCloak) - net.minecraft.util.Mth.lerp((double)aPartialTicks, aPlayer.yo, aPlayer.getY());
			double d2 = net.minecraft.util.Mth.lerp((double)aPartialTicks, aPlayer.zCloakO, aPlayer.zCloak) - net.minecraft.util.Mth.lerp((double)aPartialTicks, aPlayer.zo, aPlayer.getZ());
			float f6 = net.minecraft.util.Mth.rotLerp(aPartialTicks, aPlayer.yBodyRotO, aPlayer.yBodyRot);
			double d3 = net.minecraft.util.Mth.sin(f6 * ((float)Math.PI / 180F));
			double d4 = -net.minecraft.util.Mth.cos(f6 * ((float)Math.PI / 180F));
			float f7 = (float)d1 * 10.0F;
			f7 = net.minecraft.util.Mth.clamp(f7, -6.0F, 32.0F);
			float f8 = (float)(d0 * d3 + d2 * d4) * 100.0F;
			f8 = net.minecraft.util.Mth.clamp(f8, 0.0F, 150.0F);
			float f9 = (float)(d0 * d4 - d2 * d3) * 100.0F;
			f9 = net.minecraft.util.Mth.clamp(f9, -20.0F, 20.0F);
			float f10 = net.minecraft.util.Mth.lerp(aPartialTicks, aPlayer.oBob, aPlayer.bob);
			f7 += net.minecraft.util.Mth.sin(net.minecraft.util.Mth.lerp(aPartialTicks, aPlayer.walkDistO, aPlayer.walkDist) * 6.0F) * 32.0F * f10;
			if (aPlayer.isCrouching()) f7 += 25.0F;
			tPose.mulPose(com.mojang.math.Axis.XP.rotationDegrees(6.0F + f8 / 2.0F + f7));
			tPose.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(f9 / 2.0F));
			tPose.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180.0F - f9 / 2.0F));
			aEvent.getRenderer().getModel().renderCloak(tPose,
				aEvent.getMultiBufferSource().getBuffer(net.minecraft.client.renderer.RenderType.entitySolid(tResource)),
				aEvent.getPackedLight(), net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY);
			tPose.popPose();
		} catch (Throwable e) {e.printStackTrace(gregapi.data.CS.ERR);}
	}
}

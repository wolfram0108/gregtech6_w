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

import com.mojang.blaze3d.vertex.PoseStack;

import gregapi.tileentity.inventories.MultiTileEntityMassStorage;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;

import static gregapi.data.CS.*;

/**
 * BUG-092 (дедикейт: «нет камней и палок»): спец-рендер масс-хранилища ВЫНЕСЕН из common-класса
 * {@link MultiTileEntityMassStorage} — тот же класс дефекта и тот же приём, что {@link MTEChestRenderer}
 * (клиентские типы в common-MTE валили линковку класса на выделенном сервере и обрывали
 * Loader_MultiTileEntities; в 1.7.10 члены вырезал @SideOnly). Содержимое — 1:1 перенос.
 */
public class MTEMassStorageRenderer implements BlockEntityRenderer<MultiTileEntityMassStorage> {
	public static MTEMassStorageRenderer INSTANCE = new MTEMassStorageRenderer();

	/** Мост {@code MultiTileEntityMassStorage.onRegistrationFirstClient} (клиент-only вызов). */
	public static void bindFirst(Class<?> aClass) {
		MultiTileEntityBER.bindSpecialRenderer(aClass, INSTANCE);
	}

	/** Ветка 1.20.1: BER однопараметрический и без render-state — сбор величин и отрисовка в одном вызове, как в
	 *  1.7.10 {@code renderTileEntityAt}. Содержимое рисуется штатным {@code ItemRenderer.renderStatic} в контексте GUI
	 *  (BUG-015 v2: GUI-контекст = ИНВЕНТАРНАЯ иконка — носитель 1.7.10 renderItemIntoGUI-формы, «как в JEI/креативе»). */
	@Override
	public void render(MultiTileEntityMassStorage aStorage, float aPartialTick, PoseStack aPoseStack, MultiBufferSource aBuffer, int aLight, int aOverlay) {
		if (!aStorage.slotHas(1) || !aStorage.isFaceVisible()) return;
		// BUG-078: для item-формы facing уже подставлен центром applyItemFacing // BUG-038: item-форма (detached-TE) — калибруемый facing предмет-дисплея
		byte tFacing = aStorage.mFacing;
		// BUG-015: 1.7.10 рисовал GUI-предмет ОТ УГЛА (renderItemIntoGUI от (0,0), 16px * 1/32 = 0.5 блока),
		// поэтому точка трансляции была сдвинута на −0.25 вбок и стояла на верхнем краю (Y=0.625) — чтобы ЦЕНТР
		// предмета попал в (центр грани, Y=0.375). Движковая item-модель рисуется ЦЕНТРИРОВАННОЙ —
		// дословный перенос углового сдвига смещал предмет на четверть блока вбок и вверх (репорт игрока
		// «изображение предмета не в центре, а сбоку»). Транслируем сразу в ЦЕНТР предмета 1.7.10.
		aPoseStack.pushPose();
		aPoseStack.translate(0.5 + OFFX[tFacing]*0.502, 0.375, 0.5 + OFFZ[tFacing]*0.502);
		// BUG-015 v4: rotZ(180) из 1.7.10 компенсировал y-вниз-ориентацию GUI-рендера (renderItemIntoGUI рисовал
		// текстуру сверху-вниз); движковая модель уже y-вверх — дословный перенос переворачивал иконку вверх ногами.
		// ПОВОРОТ ЛИЦОМ НАРУЖУ. Содержимое рисуется в контексте GUI, а там модель обращена лицом к камере,
		// то есть в +Z (у ItemTransforms.GUI плоского предмета поворота нет вовсе). Значит нужен угол,
		// переводящий +Z в сторону грани: north 180°, south 0°, west 270°, east 90° — это `-toYRot()`.
		//
		// Прежняя формула `COMPASS_FROM_SIDE[tFacing]*90` = {north 0, south 180, west 270, east 90}: запад и
		// восток совпадали, а СЕВЕР и ЮГ были развёрнуты ИЗНАНКОЙ — плоская пластина показывала зеркальную
		// цифру, блок не ту сторону (скриншоты игрока 2026-08-09). Отсюда вся история BUG-075: общий доворот
		// 180° чинил север с югом и ровно так же ломал запад с востоком, поэтому симптом «переезжал».
		// Канон рамки (`180 - toYRot`, ItemFrameRenderer) сюда НЕ подходит: рамка рисует в контексте
		// FIXED с иной базовой ориентацией — проверено живьём, зеркальность осталась на всех гранях.
		// Вертикальные грани: наклон -90*step по X, как у рамки.
		net.minecraft.core.Direction tDir = net.minecraft.core.Direction.from3DDataValue(tFacing);
		if (tDir.getAxis().isHorizontal()) {
			aPoseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(-tDir.toYRot()));
		} else {
			aPoseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(-90 * tDir.getAxisDirection().getStep()));
		}
		// BUG-075. Витрина показывает GUI-ФОРМУ содержимого. Недостающая половина отражения (снятый в BUG-015 v4
		// поворот glRotatef(180,0,0,1) отражал СРАЗУ по двум осям, а сняли его по причине, касавшейся только Y)
		// возвращается как ПОВОРОТ вокруг Y на 180°, а не scale(-1,1,1): для плоской модели это та же зеркальность
		// по горизонтали, но поворот не меняет хиральность и не ломает отбраковку граней. Блочной модели тот же
		// доворот нужен по замеру игрока («блоки перевёрнуты на 180»), поэтому ветки не разделяются — величина одна
		// на всю витрину и калибруется одним числом.
		if (MASSSTORAGE_DISPLAY_YAW != 0) aPoseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(MASSSTORAGE_DISPLAY_YAW));
		aPoseStack.scale(0.5f, 0.5f, 0.0001f);
		net.minecraft.client.Minecraft.getInstance().getItemRenderer().renderStatic(aStorage.slot(1), net.minecraft.world.item.ItemDisplayContext.GUI,
			0xF000F0 /* fullbright 240/240, ориг setLightmapTextureCoords */, net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY,
			aPoseStack, aBuffer, aStorage.getLevel(), 0);
		aPoseStack.popPose();
	}
}

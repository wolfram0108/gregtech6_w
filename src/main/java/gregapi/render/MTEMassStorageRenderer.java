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

import com.mojang.blaze3d.vertex.PoseStack;

import gregapi.tileentity.inventories.MultiTileEntityMassStorage;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;

import static gregapi.data.CS.*;

/**
 * BUG-092 (дедикейт: «нет камней и палок»): спец-рендер масс-хранилища ВЫНЕСЕН из common-класса
 * {@link MultiTileEntityMassStorage} — тот же класс дефекта и тот же приём, что {@link MTEChestRenderer}
 * (клиентские типы в common-MTE валили линковку класса на выделенном сервере и обрывали
 * Loader_MultiTileEntities; в 1.7.10 члены вырезал @SideOnly). Содержимое — 1:1 перенос.
 */
public class MTEMassStorageRenderer implements BlockEntityRenderer<MultiTileEntityMassStorage, MTEMassStorageRenderer.MTEMassStorageRenderState> {
	public static MTEMassStorageRenderer INSTANCE = new MTEMassStorageRenderer();

	/** Мост {@code MultiTileEntityMassStorage.onRegistrationFirstClient} (клиент-only вызов). */
	public static void bindFirst(Class<?> aClass) {
		MultiTileEntityBER.bindSpecialRenderer(aClass, INSTANCE);
	}

	/** Состояние кадра спец-рендера (extract на main-thread, submit только читает). */
	public static class MTEMassStorageRenderState extends BlockEntityRenderState {
		public net.minecraft.client.renderer.item.ItemStackRenderState mItem; public byte mStorageFacing;
	}

	@Override
	public MTEMassStorageRenderState createRenderState() {
		return new MTEMassStorageRenderState();
	}

	@Override
	public void extractRenderState(MultiTileEntityMassStorage aStorage, MTEMassStorageRenderState aState, float aPartialTick, net.minecraft.world.phys.Vec3 aCameraPos, net.minecraft.client.renderer.feature.ModelFeatureRenderer.CrumblingOverlay aBreakProgress) {
		BlockEntityRenderer.super.extractRenderState(aStorage, aState, aPartialTick, aCameraPos, aBreakProgress);
		aState.mItem = null;
		if (!aStorage.slotHas(1) || !aStorage.isFaceVisible()) return;
		aState.mStorageFacing = aStorage.mFacing; // BUG-078: для item-формы facing уже подставлен центром applyItemFacing // BUG-038: item-форма (detached-TE) — калибруемый facing предмет-дисплея
		// BUG-015 v2: GUI-контекст (не FIXED) = ИНВЕНТАРНАЯ иконка — носитель 1.7.10 renderItemIntoGUI-формы
		// (блоки изометрией «как в JEI/креативе» — репорт игрока: «иконка ресурса не такая, как в JEI»)
		aState.mItem = new net.minecraft.client.renderer.item.ItemStackRenderState();
		net.minecraft.client.Minecraft.getInstance().getItemModelResolver().updateForTopItem(aState.mItem, aStorage.slot(1), net.minecraft.world.item.ItemDisplayContext.GUI, aStorage.getLevel(), null, 0);
	}

	@Override
	public void submit(MTEMassStorageRenderState aState, PoseStack aPoseStack, SubmitNodeCollector aNodes, CameraRenderState aCamera) {
		if (aState.mItem == null) return;
		byte tFacing = aState.mStorageFacing;
		// BUG-015: 1.7.10 рисовал GUI-предмет ОТ УГЛА (renderItemIntoGUI от (0,0), 16px * 1/32 = 0.5 блока),
		// поэтому точка трансляции была сдвинута на −0.25 вбок и стояла на верхнем краю (Y=0.625) — чтобы ЦЕНТР
		// предмета попал в (центр грани, Y=0.375). Neo ItemStackRenderState/FIXED рисует модель ЦЕНТРИРОВАННОЙ —
		// дословный перенос углового сдвига смещал предмет на четверть блока вбок и вверх (репорт игрока
		// «изображение предмета не в центре, а сбоку»). Транслируем сразу в ЦЕНТР предмета 1.7.10.
		aPoseStack.pushPose();
		aPoseStack.translate(0.5 + OFFX[tFacing]*0.502, 0.375, 0.5 + OFFZ[tFacing]*0.502);
		// BUG-015 v4: rotZ(180) из 1.7.10 компенсировал y-вниз-ориентацию GUI-рендера (renderItemIntoGUI рисовал
		// текстуру сверху-вниз); neo-модель уже y-вверх — дословный перенос переворачивал иконку вверх ногами.
		aPoseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(COMPASS_FROM_SIDE[tFacing] * 90));
		// BUG-075. Витрина показывает GUI-ФОРМУ содержимого, а её оба движка строят по-разному, причём
		// ПО-РАЗНОМУ ДЛЯ БЛОКА И ДЛЯ ПЛОСКОГО ПРЕДМЕТА. Факты 1.7.10 (внешняя цепочка витрины :723-729 и
		// RenderItem.renderItemIntoGUI):
		//   внешняя витрина        : glRotatef(180,Z) · glRotatef(compass*90,Y) · glScalef(1/32, 1/32, -0.0001)
		//   ветка 3D-блока (:36-38,49): glScalef(1,1,-1) · glRotatef(210,X) · glRotatef(45,Y) · glRotatef(-90,Y)
		//   ветка спрайта  (renderIcon): ни зеркала, ни поворотов — иконка 16x16 в экранной плоскости
		// Отсюда в оригинале: у БЛОКА внешнее зеркало по Z гасилось внутренним (два минуса), у ПЛОСКОГО
		// предмета внутреннего зеркала не было и он оставался ЗЕРКАЛЬНЫМ по Z.
		//
		// Порт потерял знак: стояло +0.0001 для обоих. У блоков это случайно совпало с оригиналом, у
		// предметов дало расхождение — репорт игрока 2026-07-28: «предметы отражены».
		// Знак восстановлен 1:1 и ровно там, где он был в оригинале, — только для плоских моделей.
		//
		// Признак блочности берётся у самой модели: usesBlockLight = gui_light "side"
		// (ModelRenderProperties:16 — getTopGuiLight().lightLikeBlock()), а НЕ instanceof BlockItem:
		// у факела, рельсов и растений модель плоская, и вести себя они должны как предметы.
		// ⛔ Ветка «зеркало по Z для плоских» СНЯТА как бездействующая: отражение вдоль оси взгляда не меняет
		// проекцию на экран (координаты x,y сохраняются), у сплющенной до 0.0001 модели это тождество.
		// Живая проверка игроком: знак Z вернули — «изменений не вижу, предметы так же отражены».
		//
		// Источник отражения — снятый в BUG-015 v4 (d502c870) поворот `glRotatef(180, 0,0,1)`. Вокруг Z он
		// разворачивает картинку в её же плоскости, то есть отражает СРАЗУ ПО ДВУМ осям — и по Y, и по X.
		// Снимали его по причине, относящейся только к Y («в 1.7.10 компенсировал y-вниз GUI-рендера, neo-модель
		// уже y-вверх» — верно, вертикаль после этого стала правильной), но вместе с Y ушло и отражение по X.
		// Горизонталь осталась зеркальной и всплыла позже, на несимметричных иконках.
		//
		// Возвращаем недостающую половину как ПОВОРОТ вокруг Y на 180°, а не как scale(-1,1,1): для плоской
		// модели это та же зеркальность по горизонтали, но поворот не меняет хиральность и не ломает
		// отбраковку граней. Блочной модели тот же доворот нужен по замеру игрока («блоки перевёрнуты на 180»),
		// поэтому ветки не разделяются — величина одна на всю витрину и калибруется одним числом.
		if (MASSSTORAGE_DISPLAY_YAW != 0) aPoseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(MASSSTORAGE_DISPLAY_YAW));
		aPoseStack.scale(0.5f, 0.5f, 0.0001f);
		aState.mItem.submit(aPoseStack, aNodes, 0xF000F0 /* fullbright 240/240, ориг setLightmapTextureCoords */, net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY, 0);
		aPoseStack.popPose();
	}
}

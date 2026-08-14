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

package gregapi.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.renderer.LevelRenderer;

import gregapi.render.MultiTileEntityBER;

/**
 * BUG-106 №4 — сброс кэша квадов BER тем же сигналом, которым движок помечает секции на перестройку.
 *
 * <p><b>Почему именно здесь.</b> В 1.7.10 геометрия MTE жила в мэше секции 16³ и обновлялась ТОЛЬКО по
 * {@code markBlockForUpdate} (recompSrc {@code RenderGlobal.markBlockForUpdate} → секции ±1 блока).
 * В neo та же воронка — {@code LevelRenderer.setSectionDirty(IIIZ)}: в неё сводятся ВСЕ пути «картинка
 * изменилась» ({@code sendBlockUpdated} → {@code blockChanged:1432}, прямой {@code setBlock},
 * {@code setBlocksDirty}, свет; {@code viewArea.setDirty} зовётся только отсюда — :1481). Каждый
 * receiveData*-диспетчер MTE ({@code MultiTileEntityBlock:265-325}) кончается {@code WD.update} →
 * {@code ClientLevel.sendBlockUpdated:701} → сюда. Инвалидация от этой воронки даёт кэшу гранулярность
 * секций 1.7.10 — залипание возможно лишь там, где залипал бы и мэш 1.7.10 (1:1 по следствию).</p>
 *
 * <p>{@code allChanged()} — полная перезагрузка рендера (F3+A, F3+T-перешив атласа, смена дистанции):
 * кэшированные квады держат UV СТАРОГО атласа, рвём все кэши разом эпохой.</p>
 *
 * <p>Правка централизована: одно место на весь мод, все 106 классов с рендер-состоянием обслуживаются
 * одной воронкой, а не пофайловым сбросом.</p>
 *
 * <p><b>Цена сигнала.</b> Для движка это дешёвый идемпотентный флаг, поэтому он бьёт по нему пачками:
 * {@code setBlockDirty} крутит ±1 по трём осям и зовёт сюда 27 раз на ОДНО изменение блока
 * ({@code LevelRenderer.java:2385-2390}), а приход чанка добавляет свет ({@code ClientPacketListener}
 * {@code enableChunkLight:718-728}, {@code readSectionList:2310-2318}). Обработчик обязан быть O(1) —
 * см. {@code MultiTileEntityBER.onSectionDirty}: он только ПЕЧАТАЕТ секцию, а сверку делает сам MTE в
 * момент рисования. Всё, что тяжелее инкремента, здесь недопустимо.</p>
 */
@Mixin(LevelRenderer.class)
public abstract class MixinLevelRenderer {

	@Inject(method = "setSectionDirty(IIIZ)V", at = @At("HEAD"))
	private void gt6$invalidateQuadCaches(int aSectionX, int aSectionY, int aSectionZ, boolean aPlayerChanged, CallbackInfo aCI) {
		MultiTileEntityBER.onSectionDirty(aSectionX, aSectionY, aSectionZ);
	}

	@Inject(method = "allChanged()V", at = @At("HEAD"))
	private void gt6$invalidateAllQuadCaches(CallbackInfo aCI) {
		MultiTileEntityBER.onRenderAllChanged();
	}
}

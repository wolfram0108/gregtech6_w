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

import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;

import java.util.Collection;

import static gregapi.data.CS.RES_PATH_MODEL;

/**
 * F3 superseded-render (GT6BlockModel/ItemModel пайплайн; старый getIcon/immediate-mode мёртв, 0 вызовов neo): 1.7.10 {@code RenderPlayer} (immediate-mode: GL11 push/pop-матрицы,
 * ручная интерполяция позиции игрока по SRG-полям {@code field_71091_bM}.., {@code ModelBiped.renderCloak})
 * — весь этот стек удалён в 26.1.2 (decisions/F3-render.md §1). Класс больше не наследует движковый
 * рендерер игрока (тип удалён без замены с той же формой) — держит только чистую бизнес-логику выбора
 * плаща по нику/UUID ({@link #getResource(String)}, БЕЗ ИЗМЕНЕНИЙ). Хук ретипирован на neo-эквивалент
 * {@code RenderPlayerEvent.Pre} (1:1 замена старого {@code RenderPlayerEvent.Specials.Pre}).
 * [Метка отложенности «заглушка хука» СНЯТА 2026-08-06.] Тело оригинала (:69-111) копировало ванильную
 * геометрию плаща руками (immediate-mode, мёртв) — в neo та же функция выражается ДАННЫМИ: подмена
 * {@code AvatarRenderState.skin} (public-поле стейта, пересобирается каждый кадр) на копию с GT6-плащом,
 * рисует сам движковый {@code CapeLayer.submit:44-68}. Условия оригинала несёт движок 1:1: невидимость —
 * {@code !state.isInvisible} (CapeLayer:45), настройка «скрыть плащ» ({@code getHideCape()} оригинала) —
 * {@code state.showCape} (CapeLayer:45); фолбэк выбора по UUID — как оригинал :78. Отличие, осознанное:
 * оригинал рисовал GT6-плащ ПОВЕРХ Mojang-плаща (два слоя друг на друге со сдвигом 0.125) — здесь
 * Mojang-плащ не перекрывается (свой плащ у игрока побеждает GT6-шный), двойного рисования нет.
 */
public class PlayerModelRenderer {
	// neo Identifier.assertValidPath запрещает заглавные в path (1.7.10 ResourceLocation их допускал) — имена
	// плащей-текстур приведены к lowercase (файлы переименованы синхронно). Порядок/логика выбора плаща 1:1.
	private final Identifier[] mResources = new Identifier[] {Identifier.parse(RES_PATH_MODEL + "braintech.png"), Identifier.parse(RES_PATH_MODEL + "silver.png"), Identifier.parse(RES_PATH_MODEL + "mrbrain.png"), Identifier.parse(RES_PATH_MODEL + "dev.png"), Identifier.parse(RES_PATH_MODEL + "gold.png"), Identifier.parse(RES_PATH_MODEL + "crazy.png"), Identifier.parse(RES_PATH_MODEL + "sus.png")};
	private final Collection<String> mSupporterListSilver, mSupporterListGold;

	public PlayerModelRenderer(Collection<String> aSupporterListSilver, Collection<String> aSupporterListGold) {
		mSupporterListSilver = aSupporterListSilver;
		mSupporterListGold   = aSupporterListGold;
	}

	private Identifier getResource(String aPlayer) {
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

	/** Плащ GT6 — данными движковому слою (разбор в class javadoc): свой выбор текстуры + ванильный
	 *  {@code CapeLayer}. Имя игрока — по entity id из стейта (в {@code AvatarRenderState} ника нет). */
	public void receiveRenderSpecialsEvent(RenderPlayerEvent.Pre<?> aEvent) {
		try {
			net.minecraft.client.renderer.entity.state.AvatarRenderState tState = aEvent.getRenderState();
			if (tState.skin == null || tState.skin.cape() != null) return;
			net.minecraft.client.multiplayer.ClientLevel tLevel = net.minecraft.client.Minecraft.getInstance().level;
			if (tLevel == null) return;
			if (!(tLevel.getEntity(tState.id) instanceof net.minecraft.world.entity.player.Player tPlayer)) return;
			// имя — getScoreboardName(): у Player это имя профиля (приём проекта, EnchantmentEffect_Werewolf:56)
			Identifier tCape = getResource(tPlayer.getScoreboardName());
			if (tCape == null) tCape = getResource(tPlayer.getUUID().toString());
			if (tCape == null) return;
			// ResourceTexture(id, texturePath) — 2-арг конструктор, путь прямой (без авто-«textures/…png»):
			// mResources уже несут полный путь вида gregtech:textures/model/<имя>.png (ClientAsset.java:19-27).
			tState.skin = net.minecraft.world.entity.player.PlayerSkin.insecure(
				tState.skin.body(), new net.minecraft.core.ClientAsset.ResourceTexture(tCape, tCape), tState.skin.elytra(), tState.skin.model());
		} catch (Throwable e) {e.printStackTrace(gregapi.data.CS.ERR);}
	}
}

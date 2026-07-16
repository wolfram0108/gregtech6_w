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
 * плаща по нику/UUID ({@link #getResource(String)}, БЕЗ ИЗМЕНЕНИЙ) + PORT-TODO-заглушку хука. Хук
 * ретипирован на реальный neo-эквивалент {@code net.neoforged.neoforge.client.event.RenderPlayerEvent.Pre}
 * (`neoforge-decompiled/net/neoforged/neoforge/client/event/RenderPlayerEvent.java:50-55` — «до рендера
 * игрока, для доп. эффектов», 1:1 замена старого {@code RenderPlayerEvent.Specials.Pre}); тело — no-op,
 * реальная перерисовка плаща — {@code SubmitNodeCollector}/BER-путь (§2.5).
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

	/** F3 superseded-render (GT6BlockModel/ItemModel пайплайн; старый getIcon/immediate-mode мёртв, 0 вызовов neo): было immediate-mode рисование плаща через {@code ModelBiped.renderCloak}
	 *  (см. class javadoc) — тело заглушка, {@link #getResource(String)} (реальный выбор текстуры) сохранён живым. */
	public void receiveRenderSpecialsEvent(RenderPlayerEvent.Pre<?> aEvent) {
		//
	}
}

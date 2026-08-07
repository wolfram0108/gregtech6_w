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

package gregapi.api;

/**
 * Переходник F1 (жизненный цикл). Событие переназначения ID модов GregTech6 поверх NeoForge.
 *
 * <p>В Forge 1.7.10 движок слал {@code FMLModIdMappingEvent} при смене числовых ID блоков/предметов
 * (например, при загрузке мира, сохранённого с другим набором модов). GregTech6 ловил его в
 * {@code GT_API.onIDChangingEvent} и рассылал по compat-классам ({@code ICompat.onIDChanging}), чтобы
 * они пересобрали закэшированные ссылки на предметы.</p>
 *
 * <p>В NeoForge числовых ID нет — идентификаторы стабильны ({@code ResourceLocation}), поэтому прямого
 * аналога нет. Тип сохранён как маркер, чтобы контракт {@code ICompat} остался 1:1; привязка к
 * neo-эквиваленту (если потребуется) — вопрос рантайм-парити, решается при доводке жизненного цикла.</p>
 */
public class FMLModIdMappingEvent {
	public FMLModIdMappingEvent() {}
}

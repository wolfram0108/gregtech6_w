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

import java.util.Map;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.google.common.collect.ImmutableMap;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;

/**
 * BUG-106 — снимок окружения для перестройки картинки копировал блок-сущности ЦЕЛОГО чанка в КАЖДУЮ секцию.
 *
 * <p><b>Что.</b> {@code SectionCopy.<init>} (26.1.2, строка 34) делает
 * {@code this.blockEntities = ImmutableMap.copyOf(levelChunk.getBlockEntities())} — карту всего чанка,
 * хотя объект представляет ОДНУ секцию. {@code RenderRegionCache.createRegion} строит на каждую
 * перестраиваемую секцию регион из 27 таких снимков, а в чанке 24 секции — карта дублируется десятками раз.</p>
 *
 * <p><b>Почему это бьёт именно по GT6.</b> В ванильном мире блок-сущностей в чанке единицы (сундук, печь),
 * копия ничего не стоит. GT6 держит материал руды и породы в блок-сущности — как и оригинал 1.7.10
 * ({@code PrefixBlock.java:511} оригинала пишет {@code mMetaData} именно туда), — поэтому их в чанке сотни.
 * Замер живой игры (2026-08-09, сессия пользователя, 6 минут): {@code PrefixBlockTileEntity} 1 890 997 шт,
 * записей этих копий {@code ImmutableMapEntry} 15 030 924 шт = 344 МБ, вместе с массивами и не-терминальными
 * записями около 750 МБ живых. Отсюда «память занята целиком и всё тормозит»: сборщик не успевает.
 * В 1.7.10 такого умножения не было — движок снимок окружения так не строил. То есть дефект возникает на
 * стыке «модель данных GT6 (1:1 с оригиналом) × устройство рендера neo», а не из-за отступления порта.</p>
 *
 * <p><b>Как.</b> Копируем только те блок-сущности, что лежат в СВОЕЙ секции снимка. Это ничего не меняет
 * в поведении: {@code RenderSectionRegion.getBlockEntity} (:75-84) выбирает снимок по координатам секции
 * самой позиции, то есть у снимка спрашивают исключительно его собственные блок-сущности, а соседей берут
 * из соседних снимков того же региона (регион 3×3×3 их и содержит). Чужие записи в карте не читались никогда.</p>
 *
 * <p>Правка централизована: одно место на весь мод, чинит ВСЕ блок-сущности GT6 (руда, порода, машины,
 * трубы), а не отдельный их вид.</p>
 */
// цель package-private (класс движка не public) — целимся по имени, как это и делается для таких классов
@Mixin(targets = "net.minecraft.client.renderer.chunk.SectionCopy")
public abstract class MixinSectionCopy {

	@Redirect(
		method = "<init>",
		at = @At(value = "INVOKE", target = "Lcom/google/common/collect/ImmutableMap;copyOf(Ljava/util/Map;)Lcom/google/common/collect/ImmutableMap;"))
	private ImmutableMap<BlockPos, BlockEntity> gt6$copyOnlyOwnSection(Map<? extends BlockPos, ? extends BlockEntity> aWholeChunk, LevelChunk aChunk, int aSectionIndex) {
		if (aWholeChunk.isEmpty()) return ImmutableMap.of();
		// индекс вне мира — своих блок-сущностей у такого снимка нет по определению (движок там и секцию не берёт)
		if (aSectionIndex < 0 || aSectionIndex >= aChunk.getSectionsCount()) return ImmutableMap.of();
		int tMinY = aChunk.getSectionYFromSectionIndex(aSectionIndex) << 4, tMaxY = tMinY + 15;
		ImmutableMap.Builder<BlockPos, BlockEntity> rOut = ImmutableMap.builder();
		for (Map.Entry<? extends BlockPos, ? extends BlockEntity> tEntry : aWholeChunk.entrySet()) {
			int tY = tEntry.getKey().getY();
			if (tY >= tMinY && tY <= tMaxY) rOut.put(tEntry.getKey(), tEntry.getValue());
		}
		return rOut.build();
	}
}

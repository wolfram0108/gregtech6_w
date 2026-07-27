/**
 * Copyright (c) 2023 GregTech-6 Team
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

package gregapi.data;

import java.util.concurrent.CompletableFuture;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

/**
 * F12-harvest — ПЕРЕНОС GT6-ДАННЫХ О ДОБЫЧЕ В ТЕГИ (единственный канал, который слушает neo).
 *
 * <p><b>Что изменил движок.</b> В 1.7.10 «каким инструментом добывается блок и какого уровня»
 * спрашивалось МЕТОДАМИ блока — {@code Block.getHarvestTool(meta)} / {@code getHarvestLevel(meta)}, — и GT6
 * их переопределял ({@code BlockBase:191-192}, {@code PrefixBlock:715,751}, MTE — своим полем инструмента).
 * Поэтому ванильная кирка вела себя на блоках GT6 как положено, и Waila показывала строки «Effective Tool»/
 * «Currently Harvestable» без единой строчки интеграции. В neo этих методов НЕТ вовсе: и скорость, и право
 * на дроп решают ТЕГИ ({@code minecraft:mineable/*}, {@code minecraft:needs_*_tool}). Порт честно записал
 * потерю как «деградация до no-op» ({@code GT_API:436-440,451-455}) — но следствие у неё живое: для ванильных
 * инструментов и для тултип-модов блоки GT6 «ничем не добываются».
 *
 * <p><b>Что делает этот провайдер.</b> На datagen обходит реестр блоков, спрашивает у каждого GT6-блока его
 * же {@code getHarvestTool}/{@code getHarvestLevel} — те самые методы, что и в 1.7.10, — и раскладывает блок
 * по ванильным тегам. Никаких новых правил не выдумывается: источник данных прежний, меняется лишь канал,
 * которым движок их читает.
 *
 * <p><b>Границы честности.</b> (1) Тег вешается на БЛОК, а уровень у {@code PrefixBlock} зависит от материала
 * (меты) — выразить per-meta в теге нельзя, поэтому берётся МИНИМАЛЬНЫЙ уровень блока: так ни один вариант,
 * добываемый в оригинале, не станет недобываемым (точную проверку всё равно делает GT6-логика инструмента,
 * {@code MultiItemTool.getDigSpeed}). (2) Инструменты, которых в ванили нет ({@code wrench}, {@code saw},
 * {@code knife}, {@code drill}, {@code sword}), в теги НЕ маппятся — придумывать им ванильный аналог значило
 * бы менять правила, а не переносить их.
 */
public class GT6HarvestTags extends TagsProvider<Block> {
	/** 1.7.10-уровень → ванильный тег «нужен инструмент не ниже». Шкала совпадает: 1=камень, 2=железо, 3=алмаз. */
	private static final TagKey<?>[] NEEDS_BY_LEVEL = {null, BlockTags.NEEDS_STONE_TOOL, BlockTags.NEEDS_IRON_TOOL, BlockTags.NEEDS_DIAMOND_TOOL};

	public GT6HarvestTags(PackOutput aOutput, CompletableFuture<HolderLookup.Provider> aLookup) {
		super(aOutput, BuiltInRegistries.BLOCK.key(), aLookup, MD.GAPI.mID);
	}

	@Override
	protected void addTags(HolderLookup.Provider aProvider) {
		int tTagged = 0, tSkipped = 0;
		for (Block tBlock : BuiltInRegistries.BLOCK) {
			net.minecraft.resources.Identifier tID = BuiltInRegistries.BLOCK.getKey(tBlock);
			if (tID == null || !(tID.getNamespace().equals(CS.ModIDs.GT) || tID.getNamespace().equals("gregtech"))) continue;
			String tTool = harvestToolOf(tBlock);
			TagKey<Block> tMineable = mineableTag(tTool);
			if (tMineable == null) {tSkipped++; continue;} // GT6-специфичный инструмент — ванильного тега нет, не выдумываем
			getOrCreateRawBuilder(tMineable).addElement(tID);
			int tLevel = harvestLevelOf(tBlock);
			if (tLevel > 0 && tLevel < NEEDS_BY_LEVEL.length && NEEDS_BY_LEVEL[tLevel] != null) {
				@SuppressWarnings("unchecked") TagKey<Block> tNeeds = (TagKey<Block>) NEEDS_BY_LEVEL[tLevel];
				getOrCreateRawBuilder(tNeeds).addElement(tID);
			}
			tTagged++;
		}
		CS.OUT.println("GT6 F12-harvest: размечено блоков тегами добычи " + tTagged + ", пропущено (не-ванильный инструмент) " + tSkipped);
	}

	/** Инструмент блока — ЕГО ЖЕ 1.7.10-метод; мета 0, потому что тег вешается на блок целиком. */
	private static String harvestToolOf(Block aBlock) {
		try {
			if (aBlock instanceof gregapi.block.prefixblock.PrefixBlock tP) return tP.getHarvestTool(0);
			if (aBlock instanceof gregapi.block.BlockBase tB) return tB.getHarvestTool(0);
			if (aBlock instanceof gregapi.block.multitileentity.MultiTileEntityBlock tM) return tM.getHarvestTool(0);
		} catch (Throwable e) {/* блок без канала — просто не размечаем */}
		return null;
	}

	/** Уровень блока — ЕГО ЖЕ 1.7.10-метод. Для семейств по мете берём минимальный (см. «Границы честности»). */
	private static int harvestLevelOf(Block aBlock) {
		try {
			if (aBlock instanceof gregapi.block.prefixblock.PrefixBlock tP) return tP.getHarvestLevel(0);
			if (aBlock instanceof gregapi.block.BlockBase tB) return tB.getHarvestLevel(0);
			if (aBlock instanceof gregapi.block.multitileentity.MultiTileEntityBlock tM) return tM.getHarvestLevel(0);
		} catch (Throwable e) {/* см. выше */}
		return 0;
	}

	private static TagKey<Block> mineableTag(String aTool) {
		if (aTool == null) return null;
		if (aTool.equals(CS.TOOL_pickaxe)) return BlockTags.MINEABLE_WITH_PICKAXE;
		if (aTool.equals(CS.TOOL_axe    )) return BlockTags.MINEABLE_WITH_AXE;
		if (aTool.equals(CS.TOOL_shovel )) return BlockTags.MINEABLE_WITH_SHOVEL;
		return null;
	}
}

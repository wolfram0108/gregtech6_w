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

package gregapi.loot;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.common.loot.LootModifier;

/**
 * Ветка 1.20.1 (задача A1, workspace/tasks/consolidation/OPEN-ITEMS.md): НОСИТЕЛЬ доставки GT-добавок
 * {@code gt6mirror.minecraftforge.common.ChestGenHooks} в 10 ванильных chest-категорий.
 *
 * <p>Прежний канал мутировал живую {@code LootTable} через {@code addPool} — Forge 1.20.1 замораживает
 * каждую таблицу СИНХРОННО в том же проходе, что сеет {@code LootTableLoadEvent} (окна на мутацию нет
 * вообще), а буфер GT6 ({@code ChestGenHooks.contents}) наполняется строго позже, на server-start —
 * поэтому канал падал на КАЖДОМ старте (10 {@code RuntimeException "Attempted to modify LootTable after
 * being finalized!"}, GT-лут никуда не попадал). Правильный канал — тот же приём, что уже несёт
 * {@link GT6BlockDropsModifier} для дропа чужих блоков: {@code IGlobalLootModifier.doApply}
 * ({@code net/minecraftforge/common/loot/LootModifier.java:68}) зовётся ПОСЛЕ {@code getRandomItemsRaw}
 * ({@code ForgeHooks.java:1187-1188}) и самой {@code LootTable} не касается — заморозка ему не мешает.
 *
 * <p><b>Гейт:</b> {@link LootContext#getQueriedLootTableId()} должен совпасть с одной из 10 ванильных
 * chest-таблиц {@code ChestGenHooks.NEO_TABLE} (обратный поиск — {@code ChestGenHooks.categoryForTable});
 * иначе no-op (лут сундуков, рыбалки, мобов и чужих блоков через эту дверь не идёт). Распределение (rolls,
 * веса GT-предметов, вес «пустого» слота под ванильную часть) строит {@code ChestGenHooks.buildPool} — ТОТ
 * ЖЕ центр, что нёс прежний канал; здесь только доставка.
 */
public class GT6ChestLootModifier extends LootModifier {
	public static final Codec<GT6ChestLootModifier> CODEC = RecordCodecBuilder.create(aInstance -> codecStart(aInstance).apply(aInstance, GT6ChestLootModifier::new));

	/** ТОТ ЖЕ реестр GLM-сериализаторов мода, что и у {@link GT6BlockDropsModifier} — «ЕДИНСТВЕННЫЙ реестр
	 *  GLM-сериализаторов мода» второго не заводим, добавляем сюда ещё одну запись. */
	public static final net.minecraftforge.registries.RegistryObject<Codec<GT6ChestLootModifier>> TYPE = GT6BlockDropsModifier.SERIALIZERS.register("chest_loot", () -> CODEC);

	/** Пустое тело — вызов нужен только чтобы гарантированно тронуть класс (проинициализировать {@link #TYPE})
	 *  до RegisterEvent; сам мод-бас несёт {@link GT6BlockDropsModifier#SERIALIZERS}, второй подписки не
	 *  требуется. */
	public static void register() {}

	public GT6ChestLootModifier(LootItemCondition[] aConditions) {super(aConditions);}

	@Override
	protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> aLoot, LootContext aContext) {
		ResourceLocation tTableId = aContext.getQueriedLootTableId();
		if (tTableId == null) return aLoot;
		String tCategory = gt6mirror.minecraftforge.common.ChestGenHooks.categoryForTable(tTableId);
		if (tCategory == null) return aLoot;
		LootPool tPool = gt6mirror.minecraftforge.common.ChestGenHooks.buildPool(tCategory);
		if (tPool == null) return aLoot;
		tPool.addRandomItems(LootTable.createStackSplitter(aContext.getLevel(), aLoot::add), aContext);
		return aLoot;
	}

	@Override
	public Codec<? extends IGlobalLootModifier> codec() {return CODEC;}
}

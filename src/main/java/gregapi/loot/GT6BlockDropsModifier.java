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
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.common.loot.LootModifier;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Ветка 1.20.1: НОСИТЕЛЬ обработки дропа блока для ЧУЖИХ блоков (ванильных и модовых).
 *
 * <p>Правило (унификация, конверсия дропа инструментом GT6, сбор в инвентарь, огненный аспект, listва и
 * прочее) живёт в одном месте — {@code gregapi.GT_API_Proxy#processBlockDrops}; здесь только доставка.
 * В 1.7.10 доставкой было событие {@code BlockEvent.HarvestDropsEvent}. В Forge 1.20.1 события со списком
 * дропов произвольного блока нет вовсе (ни {@code HarvestDropsEvent} — снят движком, ни
 * {@code BlockDropsEvent} — он только у NeoForge), а единственный хук, получающий этот список, —
 * глобальный модификатор лута: {@code LootModifier.doApply(ObjectArrayList<ItemStack>, LootContext)}
 * ({@code net/minecraftforge/common/loot/LootModifier.java:68}), который движок зовёт для КАЖДОЙ
 * сработавшей лут-таблицы ({@code ForgeHooks.java:1187-1188}).</p>
 *
 * <p><b>Гейт:</b> обрабатываем только дроп БЛОКА — по наличию {@code BLOCK_STATE} в контексте
 * ({@code LootContextParams.BLOCK_STATE}); лут сундуков, рыбалки и мобов через эту дверь не проходит.
 * Собственные блоки GT6 сюда не попадают (они не имеют лут-таблиц и отдают список сами) — они зовут тот
 * же центр напрямую из своего {@code getDrops}.</p>
 */
public class GT6BlockDropsModifier extends LootModifier {
	public static final Codec<GT6BlockDropsModifier> CODEC = RecordCodecBuilder.create(aInstance -> codecStart(aInstance).apply(aInstance, GT6BlockDropsModifier::new));

	/** ЕДИНСТВЕННЫЙ реестр GLM-сериализаторов мода. Регистрируется тем же мод-басом, что и остальные центры. */
	public static final DeferredRegister<Codec<? extends IGlobalLootModifier>> SERIALIZERS = DeferredRegister.create(ForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, gregapi.data.MD.GT.mID);
	public static final net.minecraftforge.registries.RegistryObject<Codec<GT6BlockDropsModifier>> TYPE = SERIALIZERS.register("block_drops", () -> CODEC);

	public static void register(IEventBus aModBus) {SERIALIZERS.register(aModBus);}

	public GT6BlockDropsModifier(LootItemCondition[] aConditions) {super(aConditions);}

	@Override
	protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> aLoot, LootContext aContext) {
		BlockState tState = aContext.getParamOrNull(LootContextParams.BLOCK_STATE);
		if (tState == null) return aLoot;
		net.minecraft.world.phys.Vec3 tOrigin = aContext.getParamOrNull(LootContextParams.ORIGIN);
		if (tOrigin == null) return aLoot;
		gregapi.GT_API_Proxy.processBlockDrops(aLoot, aContext.getLevel(), BlockPos.containing(tOrigin), tState, aContext.getParamOrNull(LootContextParams.THIS_ENTITY));
		return aLoot;
	}

	@Override
	public Codec<? extends IGlobalLootModifier> codec() {return CODEC;}
}

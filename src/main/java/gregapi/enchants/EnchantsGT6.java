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

package gregapi.enchants;

import com.mojang.serialization.MapCodec;

import gregapi.data.LH;
import gregapi.data.MD;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Util;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentTarget;
import net.minecraft.world.item.enchantment.effects.EnchantmentEntityEffect;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Центральный переходник F8/ENCHANT (жила «enchant-effect-dispatch», см. `DEFERRED-LEDGER.md`
 * метки {@code ENCHANT, effect-dispatch-engine}/{@code F8, enchant-registry}) — ЕДИНСТВЕННОЕ место
 * мода, которое (а) регистрирует кастомный тип {@link EnchantmentEntityEffect} 4 GT6-чаров в
 * {@code Registries.ENCHANTMENT_ENTITY_EFFECT_TYPE} и (б) собирает записи датапак-реестра
 * {@code Registries.ENCHANTMENT} для {@code werebane}/{@code dissolving}/{@code disjunction}/
 * {@code radioactivity}. Прежний императивный override {@code Enchantment.func_151367_b} удалён
 * движком — новый механизм: {@code Enchantment} = record с компонентами-эффектами
 * ({@code neo-decompiled/.../enchantment/Enchantment.java:60,299-327}), диспетчеризуемыми через
 * {@code EnchantmentHelper.doPostAttackEffectsWithItemSourceOnBreak}
 * ({@code .../enchantment/EnchantmentHelper.java:244-270}) САМИМ движком.
 *
 * <p>Референс сигнатур (НЕ выдумано):
 * <ul>
 * <li>{@code DeferredRegister.create(Registries.ENCHANTMENT_ENTITY_EFFECT_TYPE, MODID)} — дословный
 *     паттерн модовой регистрации кодек-реестра, {@code neoforge-decompiled/.../common/NeoForgeMod.java:386-389}
 *     ({@code ENTITY_PREDICATE_CODECS}, тот же класс реестра {@code Registry<MapCodec<? extends X>>}).</li>
 * <li>{@code MapCodec.unit(new X())} — паттерн нуль-арг record-эффекта, {@code neo-decompiled/.../
 *     client/renderer/item/EmptyModel.java:33} и 15+ аналогичных мест в дереве neo-decompiled.</li>
 * <li>{@code Enchantment.enchantment(definition).withEffect(EnchantmentEffectComponents.POST_ATTACK,
 *     EnchantmentTarget.ATTACKER, EnchantmentTarget.VICTIM, effect)} — дословный паттерн
 *     {@code BANE_OF_ARTHROPODS} (весовой/урон-чар с потенцированием жертвы),
 *     {@code neo-decompiled/.../enchantment/Enchantments.java:632-673,656-666}.</li>
 * <li>{@code EnchantmentTarget.ATTACKER} = чар физически на оружии в mainhand АТАКУЮЩЕГО (сканируется
 *     {@code runIterationOnItem(source, MAINHAND, attacker, ...)}); {@code EnchantmentTarget.VICTIM} =
 *     чар на ЛЮБОМ слоте ЖЕРТВЫ, включая броню ({@code runIterationOnEquipment(livingVictim, ...)}) —
 *     {@code EnchantmentHelper.java:244-270}. {@code affected=VICTIM} всегда (эффект достаётся жертве,
 *     как оригинальный {@code aHurtEntity} — инвариант независимо от стороны-носителя чара).</li>
 * <li>{@code Enchantment.EnchantmentDefinition.slots()} — необходимое условие срабатывания:
 *     {@code enchantment.value().matchingSlot(slot)} гейтит {@code runIterationOnItem}
 *     ({@code EnchantmentHelper.java:165}); чар должен содержать {@code MAINHAND} для срабатывания
 *     с оружия атакующего и/или {@code ARMOR} для срабатывания с брони жертвы.</li>
 * </ul>
 *
 * <p><b>Стык для интегратора (НЕ подключено этим заходом):</b>
 * <ol>
 * <li>{@link #bootstrap} нужно добавить в СУЩЕСТВУЮЩИЙ {@code RegistrySetBuilder} —
 *     {@code gregapi/worldgen/GT6WorldgenFeature.java:117-131}, добавить
 *     {@code .add(Registries.ENCHANTMENT, EnchantsGT6::bootstrap)} к цепочке {@code BUILDER}, чтобы
 *     запись ушла через УЖЕ существующий {@code DatapackBuiltinEntriesProvider} в
 *     {@code GT6WorldgenFeature.java:153-156} (та же точка датагена, без нового провайдера).</li>
 * <li>{@link #register(IEventBus)} нужно вызвать рядом с {@code GT6WorldgenFeature.register(aModBus)}
 *     в {@code GT_API.java:345} (та же центральная точка подписки на мод-шину).</li>
 * </ol>
 * Оба файла ({@code GT6WorldgenFeature.java}, {@code GT_API.java}) — вне зоны этого захода (стык,
 * решает интегратор), правка не внесена намеренно.
 */
public class EnchantsGT6 {

	/** Центральный DeferredRegister — ЕДИНСТВЕННОЕ место мода, которое регистрирует кастомные
	 *  {@link EnchantmentEntityEffect}-типы в neo. */
	private static final DeferredRegister<MapCodec<? extends EnchantmentEntityEffect>> EFFECT_TYPES =
		DeferredRegister.create(Registries.ENCHANTMENT_ENTITY_EFFECT_TYPE, MD.GAPI.mID);

	public static final DeferredHolder<MapCodec<? extends EnchantmentEntityEffect>, MapCodec<EnchantmentEffect_Werewolf>> WEREWOLF_EFFECT =
		EFFECT_TYPES.register("werewolf", () -> EnchantmentEffect_Werewolf.CODEC);
	public static final DeferredHolder<MapCodec<? extends EnchantmentEntityEffect>, MapCodec<EnchantmentEffect_Slime>> SLIME_EFFECT =
		EFFECT_TYPES.register("slime", () -> EnchantmentEffect_Slime.CODEC);
	public static final DeferredHolder<MapCodec<? extends EnchantmentEntityEffect>, MapCodec<EnchantmentEffect_Ender>> ENDER_EFFECT =
		EFFECT_TYPES.register("ender", () -> EnchantmentEffect_Ender.CODEC);
	public static final DeferredHolder<MapCodec<? extends EnchantmentEntityEffect>, MapCodec<EnchantmentEffect_Radioactivity>> RADIOACTIVITY_EFFECT =
		EFFECT_TYPES.register("radioactivity", () -> EnchantmentEffect_Radioactivity.CODEC);

	/**
	 * Bootstrap 4 GT6-чаров в датапак-реестр {@code Registries.ENCHANTMENT}, дословный паттерн
	 * {@code Enchantments.bootstrap} (`Enchantments.java:131-136` — сигнатура и `HolderGetter`-подготовка).
	 *
	 * <p>{@code weight}/{@code minCost}/{@code maxCost}/{@code anvilCost} — прежних 1.7.10-эквивалентов
	 * нет 1:1 (см. по-чарово ниже, что перенесено дословно, а что форс движка):
	 * <ul>
	 * <li>Werebane/Dissolving/Disjunction: {@code getMinEnchantability(lvl)=5+(lvl-1)*8},
	 *     {@code getMaxEnchantability(lvl)=min+20} (все 3 файла-оригинала идентичны) → дословно
	 *     {@code dynamicCost(5,8)}/{@code dynamicCost(25,8)} (те же коэффициенты 5/8/20→25,8);
	 *     {@code weight=2} (оригинальный 2-й аргумент конструктора {@code EnchantmentDamage}) дословно;
	 *     {@code getMaxLevel()=5} дословно.</li>
	 * <li>Radioactivity: {@code getMinEnchantability=Integer.MAX_VALUE}/{@code getMaxEnchantability=0}
	 *     → дословно {@code constantCost(Integer.MAX_VALUE)}/{@code constantCost(0)} (те же литералы,
	 *     переупакованы в {@code Cost}); исходный {@code weight=0} невозможен ({@code EnchantmentDefinition.CODEC}
	 *     требует {@code intRange(1,1024)}, `Enchantment.java:672`) — форс-минимум {@code weight=1}
	 *     (функционально безразлично: чар не в теге {@code IN_ENCHANTING_TABLE}, стол его не предложит
	 *     независимо от веса, и {@code minCost=MAX_VALUE} уже гарантирует недостижимость).</li>
	 * <li>{@code anvilCost} — структурно НОВОЕ обязательное поле {@code EnchantmentDefinition}
	 *     (1.7.10 не знал понятия "цена наковальни" для чаров); GT6-прообраза нет — минимальное
	 *     легальное значение {@code 1} у всех 4 (как у {@code PROTECTION}, `Enchantments.java:147`),
	 *     функционально безразлично по той же причине (нет табличного/наковального пути этих чаров).</li>
	 * <li>{@code slots}: {@code MAINHAND} у всех 4 (оружие атакующего) — обязательно для срабатывания
	 *     {@code EnchantmentTarget.ATTACKER}; Radioactivity ДОПОЛНИТЕЛЬНО {@code ARMOR} (оригинал
	 *     регистрирует материалы и на {@code addEnchantmentForArmors}, `Enchantment_Radioactivity.java:41-54`)
	 *     — обязательно для срабатывания {@code EnchantmentTarget.VICTIM} (самооблучение через ношенную
	 *     броню при получении урона, тот же {@code aHurtEntity}-инвариант оригинала).</li>
	 * <li>{@code supportedItems}: {@code ItemTags.WEAPON_ENCHANTABLE} (3 damage-чара, оригинал только
	 *     {@code addEnchantmentForDamage}) / {@code ItemTags.EQUIPPABLE_ENCHANTABLE} (Radioactivity,
	 *     оригинал — tools+damage+ranged+armors, широкий охват) — функционально не влияет на реальное
	 *     GT6-назначение чара предмету (материал-driven, см. F8-заметку в
	 *     {@code Enchantment_WerewolfDamage} и др.), только на легальность vanilla {@code /enchant}.</li>
	 * </ul>
	 */
	public static void bootstrap(BootstrapContext<Enchantment> context) {
		HolderGetter<Item> items = context.lookup(Registries.ITEM);

		register(context, Enchantment_WerewolfDamage.KEY,
			Enchantment.enchantment(
					Enchantment.definition(
						items.getOrThrow(ItemTags.WEAPON_ENCHANTABLE),
						2, 5,
						Enchantment.dynamicCost(5, 8), Enchantment.dynamicCost(25, 8),
						1,
						EquipmentSlotGroup.MAINHAND
					)
				)
				.withEffect(EnchantmentEffectComponents.POST_ATTACK, EnchantmentTarget.ATTACKER, EnchantmentTarget.VICTIM, new EnchantmentEffect_Werewolf())
		);
		LH.add(descriptionId(Enchantment_WerewolfDamage.KEY), "Werebane");

		register(context, Enchantment_SlimeDamage.KEY,
			Enchantment.enchantment(
					Enchantment.definition(
						items.getOrThrow(ItemTags.WEAPON_ENCHANTABLE),
						2, 5,
						Enchantment.dynamicCost(5, 8), Enchantment.dynamicCost(25, 8),
						1,
						EquipmentSlotGroup.MAINHAND
					)
				)
				.withEffect(EnchantmentEffectComponents.POST_ATTACK, EnchantmentTarget.ATTACKER, EnchantmentTarget.VICTIM, new EnchantmentEffect_Slime())
		);
		LH.add(descriptionId(Enchantment_SlimeDamage.KEY), "Dissolving");

		register(context, Enchantment_EnderDamage.KEY,
			Enchantment.enchantment(
					Enchantment.definition(
						items.getOrThrow(ItemTags.WEAPON_ENCHANTABLE),
						2, 5,
						Enchantment.dynamicCost(5, 8), Enchantment.dynamicCost(25, 8),
						1,
						EquipmentSlotGroup.MAINHAND
					)
				)
				.withEffect(EnchantmentEffectComponents.POST_ATTACK, EnchantmentTarget.ATTACKER, EnchantmentTarget.VICTIM, new EnchantmentEffect_Ender())
		);
		LH.add(descriptionId(Enchantment_EnderDamage.KEY), "Disjunction");

		register(context, Enchantment_Radioactivity.KEY,
			Enchantment.enchantment(
					Enchantment.definition(
						items.getOrThrow(ItemTags.EQUIPPABLE_ENCHANTABLE),
						1, 5,
						Enchantment.constantCost(Integer.MAX_VALUE), Enchantment.constantCost(0),
						1,
						EquipmentSlotGroup.MAINHAND, EquipmentSlotGroup.ARMOR
					)
				)
				// attacker-side: MT.addEnchantmentForTools/Damage/Ranged (оригинал ставит на оружие атакующего)
				.withEffect(EnchantmentEffectComponents.POST_ATTACK, EnchantmentTarget.ATTACKER, EnchantmentTarget.VICTIM, new EnchantmentEffect_Radioactivity())
				// victim-side: MT.addEnchantmentForArmors (оригинал ставит на броню — самооблучение при ударе)
				.withEffect(EnchantmentEffectComponents.POST_ATTACK, EnchantmentTarget.VICTIM, EnchantmentTarget.VICTIM, new EnchantmentEffect_Radioactivity())
		);
		LH.add(descriptionId(Enchantment_Radioactivity.KEY), "Radioactivity");
	}

	/** Modern-переводной ключ, который в реальности вычисляет {@code Enchantment.Builder.build}
	 *  ({@code Util.makeDescriptionId("enchantment", key.identifier())}, `Enchantment.java:636`,
	 *  `Util.java:133-135`) — используется, чтобы {@code LH.add} регистрировал английский текст под
	 *  ТЕМ ключом, который движок реально ищет (оригинальный плоский ключ {@code "enchantment.damage.*"}
	 *  структурно недостижим в data-driven модели — движко-шов, не потеря видимого результата: то же
	 *  английское имя чара, что и в оригинале). */
	private static String descriptionId(ResourceKey<Enchantment> key) {
		return Util.makeDescriptionId("enchantment", key.identifier());
	}

	private static void register(BootstrapContext<Enchantment> context, ResourceKey<Enchantment> key, Enchantment.Builder builder) {
		context.register(key, builder.build(key.identifier()));
	}

	/** F8/ENCHANT: центральная точка подписки для эффект-типов, вызывается ОДИН раз рядом с
	 *  {@code GT6WorldgenFeature.register(aModBus)} (тот же мод-бас) — см. javadoc класса, «Стык
	 *  для интегратора» п.2. */
	public static void register(IEventBus aModBus) {
		EFFECT_TYPES.register(aModBus);
	}
}

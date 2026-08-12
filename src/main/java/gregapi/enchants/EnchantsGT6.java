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


package gregapi.enchants;

import gregapi.data.MD;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Центральный переходник ENCHANT — ЕДИНСТВЕННОЕ место мода, которое регистрирует 4 GT6-чара
 * ({@code werebane}/{@code dissolving}/{@code disjunction}/{@code radioactivity}) в реестре
 * {@code ForgeRegistries.ENCHANTMENTS} ({@code forge-1201-decompiled/net/minecraftforge/registries/
 * ForgeRegistries.java:74}).
 *
 * <p>В 1.20.1 чары — снова обычные объекты реестра с императивными колбэками (как в 1.7.10), а не
 * датапак-записи с {@code EnchantmentEntityEffect}, как в 26.x. Поэтому сборка чара (стоимость,
 * уровни, слоты, эффект) вернулась В САМ класс чара — {@link Enchantment_WerewolfDamage} и соседи, —
 * и здесь остаётся только регистрация. Прежний {@code bootstrap(BootstrapContext)}, реестр
 * {@code ENCHANTMENT_ENTITY_EFFECT_TYPE} и {@code DeferredHolder} сняты: в 1.20.1 таких сущностей
 * нет вовсе.
 *
 * <p>{@code INSTANCE}-объекты создаются при инициализации своих классов (как оригинальный
 * {@code INSTANCE = this} в конструкторе, {@code gt6-original/.../Enchantment_WerewolfDamage.java:75}),
 * поэтому {@code MT.init()} может раздавать их материалам через {@code addEnchantmentFor*} независимо
 * от момента {@link #register(IEventBus)} — тот же порядок, что в оригинале.
 *
 * <p>Точка подписки — {@link #register(IEventBus)} из центрального @Mod-конструктора
 * {@code gregapi.GT_API#GT_API(IEventBus)} (тот же мод-бас, что {@code ITEMS}/{@code BLOCKS}).
 */
public class EnchantsGT6 {

	/** Центральный DeferredRegister чар — ЕДИНСТВЕННОЕ место мода, которое пишет в реестр чар. */
	private static final DeferredRegister<Enchantment> ENCHANTMENTS = DeferredRegister.create(ForgeRegistries.ENCHANTMENTS, MD.GAPI.mID);

	public static final RegistryObject<Enchantment> WEREBANE      = ENCHANTMENTS.register("werebane"     , () -> Enchantment_WerewolfDamage.INSTANCE);
	public static final RegistryObject<Enchantment> DISSOLVING    = ENCHANTMENTS.register("dissolving"   , () -> Enchantment_SlimeDamage   .INSTANCE);
	public static final RegistryObject<Enchantment> DISJUNCTION   = ENCHANTMENTS.register("disjunction"  , () -> Enchantment_EnderDamage   .INSTANCE);
	public static final RegistryObject<Enchantment> RADIOACTIVITY = ENCHANTMENTS.register("radioactivity", () -> Enchantment_Radioactivity .INSTANCE);

	/** ENCHANT: центральная точка подписки, вызывается ОДИН раз из {@code GT_API} тем же мод-басом. */
	public static void register(IEventBus aModBus) {
		ENCHANTMENTS.register(aModBus);
	}
}

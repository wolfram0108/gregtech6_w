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

package gregtech.entities;

import gregapi.data.CS.ModIDs;
import gregtech.entities.projectiles.EntityArrow_Material;
import gregtech.entities.projectiles.EntityArrow_Potion;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.eventbus.api.IEventBus;

import net.minecraftforge.registries.DeferredRegister;

/**
 * F12-entity (ЗАКРЫТО): ЦЕНТРАЛЬНАЯ регистрация EntityType мода gregtech — единая точка (тот же приём, что
 * gregapi.GT_API.ITEMS/BLOCKS/BLOCK_ENTITIES). Заменяет удалённый 1.7.10 {@code EntityRegistry.registerModEntity}
 * (GT6_Main: "GT_Entity_Arrow"/id 1/trackingRange 160/updateFreq 1; "GT_Entity_Arrow_Potion"/id 2/…) на neo
 * {@code DeferredRegister<EntityType<?>>}. Параметры билдера — 1:1 vanilla Arrow (sized 0.5/eyeHeight 0.13),
 * дальность/интервал — из registerModEntity (160 блоков = 10 чанков, updateInterval 1). Имена реестра
 * приведены к lowercase (neo Identifier запрещает заглавные): GT_Entity_Arrow → gt_entity_arrow.
 */
public class EntitiesGT {
	public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, ModIDs.GT);

	public static final net.minecraftforge.registries.RegistryObject<EntityType<EntityArrow_Material>> ARROW_MATERIAL =
		ENTITY_TYPES.register("gt_entity_arrow", () -> EntityType.Builder.<EntityArrow_Material>of(EntityArrow_Material::new, MobCategory.MISC)
			.sized(0.5F, 0.5F).clientTrackingRange(10).updateInterval(1)
			.build("gt_entity_arrow"));

	public static final net.minecraftforge.registries.RegistryObject<EntityType<EntityArrow_Potion>> ARROW_POTION =
		ENTITY_TYPES.register("gt_entity_arrow_potion", () -> EntityType.Builder.<EntityArrow_Potion>of(EntityArrow_Potion::new, MobCategory.MISC)
			.sized(0.5F, 0.5F).clientTrackingRange(10).updateInterval(1)
			.build("gt_entity_arrow_potion"));

	/** Подписка DeferredRegister на мод-шину (вызывается из конструктора GT6_Main, как остальные центральные реестры). */
	public static void register(IEventBus aModBus) {
		ENTITY_TYPES.register(aModBus);
		aModBus.addListener(EntitiesGT::onAttributeModification);
	}

	// F-entity-ai (КРИТ, крах входа в мир): GT_Proxy.onEntitySpawningEvent добавляет TemptGoal виллагеру (изумруд) и
	// оцелоту (банки). В MC26 TemptGoal.canUse читает атрибут minecraft:tempt_range (RangedAttribute, новый в neo) →
	// сущность БЕЗ него крашится при тике ('Can't find attribute minecraft:tempt_range'). Vanilla-виллагер этот атрибут
	// НЕ несёт (в vanilla он не temptable). Регистрируем tempt_range виллагеру и оцелоту через EntityAttributeModificationEvent
	// (mod-bus); guard has() — оцелот в vanilla уже temptable (не дублируем, add на существующий кинул бы).
	private static void onAttributeModification(net.minecraftforge.event.entity.EntityAttributeModificationEvent aEvent) {
		addTemptRange(aEvent, EntityType.VILLAGER);
		addTemptRange(aEvent, EntityType.OCELOT);
	}

	private static void addTemptRange(net.minecraftforge.event.entity.EntityAttributeModificationEvent aEvent, EntityType<? extends net.minecraft.world.entity.LivingEntity> aType) {
		// Ветка 1.20.1: атрибута minecraft:tempt_range не существует, TemptGoal этой версии радиус не спрашивает
		// (TemptGoal.java) — класса дефекта «Can't find attribute tempt_range» нет, регистрировать нечего.
	}
}

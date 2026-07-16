/**
 * Copyright (c) 2025 GregTech-6 Team
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
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

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

	public static final DeferredHolder<EntityType<?>, EntityType<EntityArrow_Material>> ARROW_MATERIAL =
		ENTITY_TYPES.register("gt_entity_arrow", rl -> EntityType.Builder.<EntityArrow_Material>of(EntityArrow_Material::new, MobCategory.MISC)
			.noLootTable().sized(0.5F, 0.5F).eyeHeight(0.13F).clientTrackingRange(10).updateInterval(1)
			.build(ResourceKey.create(Registries.ENTITY_TYPE, rl)));

	public static final DeferredHolder<EntityType<?>, EntityType<EntityArrow_Potion>> ARROW_POTION =
		ENTITY_TYPES.register("gt_entity_arrow_potion", rl -> EntityType.Builder.<EntityArrow_Potion>of(EntityArrow_Potion::new, MobCategory.MISC)
			.noLootTable().sized(0.5F, 0.5F).eyeHeight(0.13F).clientTrackingRange(10).updateInterval(1)
			.build(ResourceKey.create(Registries.ENTITY_TYPE, rl)));

	/** Подписка DeferredRegister на мод-шину (вызывается из конструктора GT6_Main, как остальные центральные реестры). */
	public static void register(IEventBus aModBus) {
		ENTITY_TYPES.register(aModBus);
	}
}

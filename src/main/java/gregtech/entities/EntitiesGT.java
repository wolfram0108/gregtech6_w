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

/** Central EntityType registration for the mod, replacing removed 1.7.10 EntityRegistry.registerModEntity with neo's
 *  DeferredRegister; builder params stay 1:1 with vanilla Arrow, registry names lowercased since neo forbids uppercase. */
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

	/** Subscribes the DeferredRegister to the mod bus, called from the constructor like every other central registry. */
	public static void register(IEventBus aModBus) {
		ENTITY_TYPES.register(aModBus);
		aModBus.addListener(EntitiesGT::onAttributeModification);
	}

	// Villagers get a TemptGoal for emeralds, but vanilla villagers lack the new minecraft:tempt_range
	// attribute that goal now requires, crashing on tick; registered via EntityAttributeModificationEvent, guarded for ocelot.
	private static void onAttributeModification(net.minecraftforge.event.entity.EntityAttributeModificationEvent aEvent) {
		addTemptRange(aEvent, EntityType.VILLAGER);
		addTemptRange(aEvent, EntityType.OCELOT);
	}

	private static void addTemptRange(net.minecraftforge.event.entity.EntityAttributeModificationEvent aEvent, EntityType<? extends net.minecraft.world.entity.LivingEntity> aType) {
		// On 1.20.1 there's no tempt_range attribute, and TemptGoal here never asks for a radius, so there's nothing to register.
	}
}

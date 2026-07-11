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

package gregapi.enchants;

import com.mojang.serialization.MapCodec;

import gregapi.util.UT;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.enchantment.EnchantedItemInUse;
import net.minecraft.world.item.enchantment.effects.EnchantmentEntityEffect;
import net.minecraft.world.phys.Vec3;

/**
 * @author Gregorius Techneticies
 *
 * 1:1-перенос игровой логики {@code Enchantment_EnderDamage.func_151367_b}
 * (`gregtech6/src/main/java/gregapi/enchants/Enchantment_EnderDamage.java:82-89`) — только
 * механизм-носитель сменён, см. {@link EnchantmentEffect_Werewolf}. Значения (Weakness+Poison,
 * амплитуда {@code bind(1,5,(5*lvl)/7)}, длительность {@code lvl*200}) не изменены; комментарии
 * оригинала (Weakness мешает телепорту эндермена; Poison облегчает убийство дракона) сохранены.
 */
public record EnchantmentEffect_Ender() implements EnchantmentEntityEffect {
	public static final MapCodec<EnchantmentEffect_Ender> CODEC = MapCodec.unit(new EnchantmentEffect_Ender());

	@Override
	public void apply(ServerLevel serverLevel, int enchantmentLevel, EnchantedItemInUse item, Entity entity, Vec3 position) {
		if (entity instanceof LivingEntity aHurtEntity && UT.Entities.isEnderCreature(aHurtEntity)) {
			// Weakness causes Endermen to not be able to teleport with GT being installed.
			aHurtEntity.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, enchantmentLevel * 200, (int)UT.Code.bind(1, 5, (5*enchantmentLevel) / 7)));
			// They also get Poisoned. If you have this Enchant on an Arrow, you can kill the Ender Dragon easier.
			aHurtEntity.addEffect(new MobEffectInstance(MobEffects.POISON, enchantmentLevel * 200, (int)UT.Code.bind(1, 5, (5*enchantmentLevel) / 7)));
		}
	}

	@Override
	public MapCodec<EnchantmentEffect_Ender> codec() {
		return CODEC;
	}
}

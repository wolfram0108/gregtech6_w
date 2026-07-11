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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.enchantment.EnchantedItemInUse;
import net.minecraft.world.item.enchantment.effects.EnchantmentEntityEffect;
import net.minecraft.world.phys.Vec3;

/**
 * @author Gregorius Techneticies
 *
 * 1:1-перенос игровой логики {@code Enchantment_Radioactivity.func_151367_b}
 * (`gregtech6/src/main/java/gregapi/enchants/Enchantment_Radioactivity.java:84-86`) — только
 * механизм-носитель сменён, см. {@link EnchantmentEffect_Werewolf}. Делегат {@code UT.Entities.
 * applyRadioactivity(aHurtEntity, aLevel, 1)} не изменён (сигнатура/аргументы дословны).
 */
public record EnchantmentEffect_Radioactivity() implements EnchantmentEntityEffect {
	public static final MapCodec<EnchantmentEffect_Radioactivity> CODEC = MapCodec.unit(new EnchantmentEffect_Radioactivity());

	@Override
	public void apply(ServerLevel serverLevel, int enchantmentLevel, EnchantedItemInUse item, Entity entity, Vec3 position) {
		UT.Entities.applyRadioactivity(entity, enchantmentLevel, 1);
	}

	@Override
	public MapCodec<EnchantmentEffect_Radioactivity> codec() {
		return CODEC;
	}
}

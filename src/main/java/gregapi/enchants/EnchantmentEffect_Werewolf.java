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

package gregapi.enchants;

import com.mojang.serialization.MapCodec;

import gregapi.data.CS.SFX;
import gregapi.util.ST;
import gregapi.util.UT;
import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantedItemInUse;
import net.minecraft.world.item.enchantment.effects.EnchantmentEntityEffect;
import net.minecraft.world.phys.Vec3;

import static gregapi.data.CS.RNGSUS;

/**
 * @author Gregorius Techneticies
 *
 * 1:1-перенос игровой логики {@code Enchantment_WerewolfDamage.func_151367_b}
 * (`gregtech6/src/main/java/gregapi/enchants/Enchantment_WerewolfDamage.java:91-111`) — только
 * механизм-носитель сменён: было переопределение виртуального метода vanilla {@code Enchantment},
 * стало record-тело {@code EnchantmentEntityEffect} ({@code neo-decompiled/.../enchantment/effects/
 * EnchantmentEntityEffect.java:13,36}, паттерн эффекта — {@code .../effects/ApplyMobEffect.java:21-56}).
 * Значения (амплитуда {@code bind(1,5,(10*lvl)/7)}, длительность {@code lvl*200}, порог случайного
 * дропа {@code i=-1..<lvl}) не изменены.
 *
 * <p>Замены API 1:1 (движко-шов, не потеря): {@code aHurtEntity.worldObj.isClientSide()} →
 * {@code level().isClientSide()} ({@code Level.java:163}); {@code getCommandSenderName()} →
 * {@code getScoreboardName()} ({@code Entity.java:3200}, у {@code Player} переопределён на имя
 * профиля — {@code Player.java:1760-1763} — тот же семантический контракт: имя для игрока, иначе
 * технический идентификатор); {@code inventory.mainInventory[]} → {@code Inventory.getNonEquipmentItems()}
 * (тот же 36-слотовый хотбар+инвентарь без брони/оффхенда, живая ссылка, не копия — {@code Inventory.java:55,90-92});
 * {@code tEntity.delayBeforeCanPickup=40} → {@code ItemEntity.setPickUpDelay(40)} ({@code ItemEntity.java:439});
 * {@code mainInventory[tSlot]=null} → {@code .set(tSlot, ItemStack.EMPTY)} (тот же слот, тот же живой массив).
 */
public record EnchantmentEffect_Werewolf() implements EnchantmentEntityEffect {
	public static final MapCodec<EnchantmentEffect_Werewolf> CODEC = MapCodec.unit(new EnchantmentEffect_Werewolf());

	@Override
	public void apply(ServerLevel serverLevel, int enchantmentLevel, EnchantedItemInUse item, Entity entity, Vec3 position) {
		if (!(entity instanceof LivingEntity aHurtEntity) || !UT.Entities.isWereCreature(aHurtEntity)) return;
		// Anti Bear Damage now works through the Quantum Suit too, just in a different way. XD
		if (!aHurtEntity.level().isClientSide() && aHurtEntity instanceof Player && "Bear989Sr".equalsIgnoreCase(aHurtEntity.getScoreboardName())) {
			UT.Sounds.send(SFX.MC_FIREWORK_LARGE, aHurtEntity);
			Inventory tInv = ((Player)aHurtEntity).getInventory();
			NonNullList<ItemStack> tMain = tInv.getNonEquipmentItems();
			for (int i = -1; i < enchantmentLevel; i++) {
				int tSlot = RNGSUS.nextInt(tMain.size());
				ItemStack tStack = tMain.get(tSlot);
				if (ST.valid(tStack)) {
					ItemEntity tEntity = ST.drop(aHurtEntity, ST.copy_(tStack));
					if (tEntity != null) {
						tEntity.setPickUpDelay(40);
						tMain.set(tSlot, ItemStack.EMPTY);
					}
				}
			}
		}
		aHurtEntity.addEffect(new MobEffectInstance(MobEffects.WITHER, enchantmentLevel * 200, (int)UT.Code.bind(1, 5, (10*enchantmentLevel) / 7)));
		aHurtEntity.addEffect(new MobEffectInstance(MobEffects.POISON, enchantmentLevel * 200, (int)UT.Code.bind(1, 5, (10*enchantmentLevel) / 7)));
	}

	@Override
	public MapCodec<EnchantmentEffect_Werewolf> codec() {
		return CODEC;
	}
}

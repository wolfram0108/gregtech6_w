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

import gregapi.data.CS.SFX;
import gregapi.util.ST;
import gregapi.util.UT;
import net.minecraft.core.NonNullList;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import static gregapi.data.CS.RNGSUS;

/**
 * @author Gregorius Techneticies
 *
 * Игровая логика чара {@code Enchantment_WerewolfDamage} (оригинал 1.7.10 —
 * {@code gregapi/enchants/Enchantment_WerewolfDamage.java:91-111}, метод {@code func_151367_b}).
 * В 26.x-версии порта тело жило в record-е {@code EnchantmentEntityEffect} (движок диспетчерил чары
 * data-driven); в 1.20.1 движковая модель чар снова императивная — {@code Enchantment.doPostHurt}
 * ({@code forge-1201-decompiled/.../enchantment/Enchantment.java:121}), поэтому носитель снова
 * обычный метод, вызываемый из {@link Enchantment_WerewolfDamage}. Значения не изменены.
 */
public final class EnchantmentEffect_Werewolf {
	private EnchantmentEffect_Werewolf() {}

	public static void apply(Entity aEntity, int aLevel) {
		if (!(aEntity instanceof LivingEntity aHurtEntity) || !UT.Entities.isWereCreature(aHurtEntity)) return;
		// Anti Bear Damage now works through the Quantum Suit too, just in a different way. XD
		if (!aHurtEntity.level().isClientSide() && aHurtEntity instanceof Player && "Bear989Sr".equalsIgnoreCase(aHurtEntity.getScoreboardName())) {
			UT.Sounds.send(SFX.MC_FIREWORK_LARGE, aHurtEntity);
			Inventory tInv = ((Player)aHurtEntity).getInventory();
			NonNullList<ItemStack> tMain = tInv.items;
			for (int i = -1; i < aLevel; i++) {
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
		aHurtEntity.addEffect(new MobEffectInstance(MobEffects.WITHER, aLevel * 200, (int)UT.Code.bind(1, 5, (10*aLevel) / 7)));
		aHurtEntity.addEffect(new MobEffectInstance(MobEffects.POISON, aLevel * 200, (int)UT.Code.bind(1, 5, (10*aLevel) / 7)));
	}
}

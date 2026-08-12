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
 *
 * Modified in 2026 for the GregTech 6 NeoForge port
 * (https://github.com/wolfram0108/gregtech6_w): ported from Minecraft 1.7.10 / Forge
 * to Minecraft 1.20.1 / Forge.
 */


package gregapi.enchants;

import gregapi.data.LH;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.DamageEnchantment;
import net.minecraft.world.item.enchantment.Enchantment;

/**
 * @author Gregorius Techneticies
 *
 * Модель чар в 1.20.1 — та же императивная, что в 1.7.10: {@code Enchantment} наследуется, эффект
 * висит на переопределённом {@code doPostHurt} ({@code forge-1201-decompiled/.../enchantment/
 * Enchantment.java:121} = 1.7.10 {@code func_151367_b}, {@code gt6-original/build/tmp/recompSrc/
 * .../Enchantment.java:182}). Поэтому оригинальная форма восстановлена дословно; data-driven
 * обвязка 26.x (датапак-реестр, {@code EnchantmentEntityEffect}) в 1.20.1 не существует и снята.
 *
 * <p>Диспетчер — СВОЙ у GT6: {@code UT.Enchantments.applyBullshitA(жертва, атакующий, оружие)}
 * зовёт {@code doPostHurt(жертва, атакующий, lvl)} по чарам брони жертвы И по чарам оружия
 * ({@code gt6-original/.../UT.java:2434-2456}); вызыватели — ToolStats, PrefixItemProjectile,
 * EntityArrow_Material, Behavior_Arrow, Behavior_Gun. Слоты в конструкторе нужны только ванильному
 * пути ({@code EnchantmentHelper.doPostHurtEffects}), GT6-путь их не гейтит.
 *
 * <p>{@code weight}=0 оригинального конструктора {@code EnchantmentDamage(id, weight, type)} →
 * {@code Rarity.VERY_RARE} (веса {@code Rarity}: COMMON 10 / UNCOMMON 5 / RARE 2 / VERY_RARE 1,
 * {@code Enchantment.java:158-170}). {@code type}={@code -1} сохранён дословно: он не индексирует
 * массивы родителя, потому что {@code getMinCost}/{@code getMaxCost} переопределены здесь (ровно
 * как в оригинале), а {@code getDamageBonus}/{@code doPostAttack} при {@code -1} дают 0/no-op
 * ({@code DamageEnchantment.java:39-46,57-64}).
 */
public class Enchantment_Radioactivity extends DamageEnchantment {
	public static final Enchantment_Radioactivity INSTANCE = new Enchantment_Radioactivity();
	
	public Enchantment_Radioactivity() {
		// ARMOR-слоты сверх MAINHAND: материалы навешивают Radioactivity и через addEnchantmentForArmors
		// (MT.init enchant-блок), оригинал ловил их своим диспетчером по getLastActiveItems.
		super(Enchantment.Rarity.VERY_RARE, -1, EquipmentSlot.MAINHAND, EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD);
		LH.add(getDescriptionId(), "Radioactivity");
	}
	
	@Override
	public int getMinCost(int aLevel) {
		return Integer.MAX_VALUE;
	}
	
	@Override
	public int getMaxCost(int aLevel) {
		return 0;
	}
	
	@Override
	public int getMaxLevel() {
		return 5;
	}
	
	@Override
	public boolean canEnchant(ItemStack aStack) {
		return false;
	}
	
	@Override
	public boolean isAllowedOnBooks() {
		return false;
	}
	
	@Override
	public void doPostHurt(LivingEntity aHurtEntity, Entity aDamagingEntity, int aLevel) {
		EnchantmentEffect_Radioactivity.apply(aHurtEntity, aLevel);
	}
	
	@Override
	public String getDescriptionId() {
		return "enchantment.damage.radioactivity";
	}
}

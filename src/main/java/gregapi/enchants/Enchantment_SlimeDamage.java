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

/** @author Gregorius Techneticies
 *  1.20.1's enchantment model is imperative, like 1.7.10: doPostHurt is overridden directly, no data-driven wrapper needed.
 *  GT6's own dispatcher calls doPostHurt for both victim-armor and weapon enchants; ctor slots only gate vanilla's path. */
public class Enchantment_SlimeDamage extends DamageEnchantment {
	public static final Enchantment_SlimeDamage INSTANCE = new Enchantment_SlimeDamage();
	
	public Enchantment_SlimeDamage() {
		super(Enchantment.Rarity.RARE, -1, EquipmentSlot.MAINHAND);
		LH.add(getDescriptionId(), "Dissolving");
	}
	
	@Override
	public int getMinCost(int aLevel) {
		return 5 + (aLevel - 1) * 8;
	}
	
	@Override
	public int getMaxCost(int aLevel) {
		return this.getMinCost(aLevel) + 20;
	}
	
	@Override
	public int getMaxLevel() {
		return 5;
	}
	
	@Override
	public void doPostHurt(LivingEntity aHurtEntity, Entity aDamagingEntity, int aLevel) {
		EnchantmentEffect_Slime.apply(aHurtEntity, aLevel);
	}
	
	@Override
	public String getDescriptionId() {
		return "enchantment.damage.slime";
	}
}

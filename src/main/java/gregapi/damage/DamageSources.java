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

package gregapi.damage;

import gregapi.util.UT;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragonPart;
import net.minecraft.world.entity.player.Player;
import net.minecraft.util.*;

import static gregapi.data.CS.F;
/**
 * @author Gregorius Techneticies
 */
public class DamageSources {
	public static DamageSource getElectricDamage() {
		try {return ic2.api.info.Info.DMG_ELECTRIC;} catch(Throwable e) {/**/}
		return getHeatDamage();
	}
	
	public static DamageSource getRadioactiveDamage() {
		try {return ic2.api.info.Info.DMG_RADIATION;} catch(Throwable e) {/**/}
		return getHeatDamage();
	}
	
	public static DamageSource getNukeExplosionDamage() {
		try {return ic2.api.info.Info.DMG_NUKE_EXPLOSION;} catch(Throwable e) {/**/}
		return getHeatDamage();
	}
	
	public static DamageSource getExplodingDamage() {
		return new DamageSourceExploding();
	}
	
	public static DamageSource getCombatDamage(String aType, LivingEntity aPlayer, Component aDeathMessage) {return getCombatDamage(aType, aPlayer, aDeathMessage, F);}
	public static DamageSource getCombatDamage(String aType, LivingEntity aPlayer, Component aDeathMessage, boolean aBeheading) {
		return new DamageSourceCombat(aType, aPlayer, aDeathMessage, aBeheading);
	}
	
	public static DamageSource getSpikeDamage() {
		return new DamageSourceSpike();
	}
	
	public static DamageSource getShredderDamage() {
		return new DamageSourceShredder();
	}
	
	public static DamageSource getCrusherDamage() {
		return new DamageSourceCrusher();
	}
	
	public static DamageSource getHeatDamage() {
		return new DamageSourceHeat();
	}
	
	public static DamageSource getFrostDamage() {
		return new DamageSourceFrost();
	}
	
	public static DamageSource getChemDamage() {
		return new DamageSourceChem();
	}
	
	public static DamageSource getBumbleDamage() {
		return new DamageSourceBumble();
	}
	
	public static DamageSource getAlcoholDamage() {
		return new DamageSourceAlcohol();
	}
	
	public static DamageSource getCaffeineDamage() {
		return new DamageSourceCaffeine();
	}
	
	public static DamageSource getDehydrationDamage() {
		return new DamageSourceDehydration();
	}
	
	public static DamageSource getSugarDamage() {
		return new DamageSourceSugar();
	}
	
	public static DamageSource getFatDamage() {
		return new DamageSourceFat();
	}
	
	public static Component getDeathMessage(LivingEntity aPlayer, Entity aEntity, String aMessage) {
		return getDeathMessage(aPlayer, aEntity, UT.Code.stringValidate(aPlayer.getCommandSenderName(), "Someone"), UT.Code.stringValidate(aEntity.getCommandSenderName(), "Someone"), aMessage);
	}
	
	public static Component getDeathMessage(LivingEntity aPlayer, Entity aEntity, String aNamePlayer, String aNameEntity, String aMessage) {
		if (UT.Code.stringInvalid(aNamePlayer) || UT.Code.stringInvalid(aEntity)) return new Component("Death Message lacks names of involved People");
		aNamePlayer = aNamePlayer.trim(); aNameEntity = aNameEntity.trim();
		if (aNamePlayer.equalsIgnoreCase("CrazyJ84") || aNamePlayer.equalsIgnoreCase("CrazyJ1984")) {
			if (aNameEntity.equalsIgnoreCase("Bear989jr")) return new Component("<"+ ChatFormatting.LIGHT_PURPLE+"Mrs. Crazy"+ChatFormatting.WHITE + "> Sorry "+ChatFormatting.RED+"Junior"+ChatFormatting.WHITE);
			if (aNameEntity.equalsIgnoreCase("Bear989Sr")) return new Component("<"+ChatFormatting.LIGHT_PURPLE+"Mrs. Crazy"+ChatFormatting.WHITE + "> Hush it!, "+ChatFormatting.RED+"Bear"+ChatFormatting.WHITE+"!");
		}
		if (aNamePlayer.equalsIgnoreCase("Bear989Sr") || aNamePlayer.equalsIgnoreCase("Bear989jr")) {
			//
		}
		
		if (UT.Code.stringValid(aMessage)) {
			return new Component(aMessage.replace("[KILLER]", ChatFormatting.GREEN+aNamePlayer+ChatFormatting.WHITE).replace("[VICTIM]", ChatFormatting.RED+aNameEntity+ChatFormatting.WHITE));
		} else if (aEntity instanceof LivingEntity) {
			return new DamageSource(aPlayer instanceof Player ? "player" : "mob", aPlayer).func_151519_b((LivingEntity)aEntity);
		} else if (aEntity instanceof EnderDragonPart) {
			return new DamageSource(aPlayer instanceof Player ? "player" : "mob", aPlayer).func_151519_b((LivingEntity)((EnderDragonPart)aEntity).entityDragonObj);
		}
		return new Component(ChatFormatting.GREEN+aNamePlayer+ChatFormatting.WHITE+" has killed "+ChatFormatting.RED+aNameEntity+ChatFormatting.WHITE);
	}
}

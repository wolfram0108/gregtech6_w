/**
 * Copyright (c) 2019 Gregorius Techneticies
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

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

/**
 * @author Gregorius Techneticies
 */
public class DamageSourceAlcohol extends DamageSource {
	public DamageSourceAlcohol() {
		super("alcohol");
		setDamageBypassesArmor();
		setDamageIsAbsolute();
	}
	
	@Override
	public Component func_151519_b(LivingEntity aTarget) {
		return new Component(ChatFormatting.RED+aTarget.getCommandSenderName()+ChatFormatting.WHITE + " died from alcohol poisoning");
	}
}

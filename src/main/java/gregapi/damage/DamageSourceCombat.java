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
 * to Minecraft 26.1.2 / NeoForge.
 */

package gregapi.damage;

import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import static gregapi.data.CS.F;

/**
 * @author Gregorius Techneticies
 */
public class DamageSourceCombat extends DamageSources.GregTechDamageSource {
	private Component mDeathMessage;
	public boolean mBeheadingDamage = F;

	public DamageSourceCombat(String aType, LivingEntity aPlayer, Component aDeathMessage) {this(aType, aPlayer, aDeathMessage, F);}
	public DamageSourceCombat(String aType, LivingEntity aPlayer, Component aDeathMessage, boolean aBeheadingDamage) {
		super(DamageSources.combatDefinition(aType), aPlayer);
		mBeheadingDamage = aBeheadingDamage;
		mDeathMessage = aDeathMessage;
	}

	@Override
	public Component getLocalizedDeathMessage(LivingEntity aTarget) {
		if (mDeathMessage != null) return mDeathMessage;
		Entity tEntity = getEntity();
		ItemStack tStack = tEntity instanceof LivingEntity ? ((LivingEntity)tEntity).getMainHandItem() : ItemStack.EMPTY;
		String tKey = "death.attack." + getMsgId();
		String tItemKey = tKey + ".item";
		return !tStack.isEmpty() && tStack.hasCustomHoverName() && Language.getInstance().has(tItemKey) ? Component.translatable(tItemKey, aTarget.getDisplayName(), tEntity.getDisplayName(), tStack.getDisplayName()) : Component.translatable(tKey, aTarget.getDisplayName(), tEntity.getDisplayName());
	}
}

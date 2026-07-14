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

package gregapi.item.multiitem.behaviors;

import static gregapi.data.CS.*;

import java.util.List;

import gregapi.data.LH;
import gregapi.item.multiitem.MultiItem;
import gregapi.item.multiitem.behaviors.IBehavior.AbstractBehaviorDefault;
import gregapi.util.UT;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.effect.MobEffectInstance;

public class Behavior_FeedChocolate extends AbstractBehaviorDefault {
	public static final Behavior_FeedChocolate INSTANCE = new Behavior_FeedChocolate();
	
	@Override
	public boolean onRightClickEntity(MultiItem aItem, ItemStack aStack, Player aPlayer, Entity aEntity) {
		if (aEntity instanceof TamableAnimal && ((TamableAnimal)aEntity).isTame()) {
			((LivingEntity)aEntity).addEffect(new MobEffectInstance(MobEffects.POISON, 120, 0));
			UT.Entities.consumeCurrentItem(aPlayer);
			return T;
		}
		if (aEntity instanceof Horse) {
			((LivingEntity)aEntity).addEffect(new MobEffectInstance(MobEffects.POISON, 120, 0));
			((Horse)aEntity).level().playSoundAtEntity(aEntity, "eating", 1.0F, 1.0F + RNGSUS.nextFloat() - RNGSUS.nextFloat() * 0.2F);
			UT.Entities.consumeCurrentItem(aPlayer);
			return T;
		}
		if (aEntity instanceof Animal) {
			((LivingEntity)aEntity).addEffect(new MobEffectInstance(MobEffects.POISON, 120, 0));
			UT.Entities.consumeCurrentItem(aPlayer);
			return T;
		}
		return F;
	}
	
	static {
		LH.add("gt.behaviour.feed.chocolate", "Do not feed this to Pets!");
	}
	
	@Override
	public List<String> getAdditionalToolTips(MultiItem aItem, List<String> aList, ItemStack aStack) {
		aList.add(LH.get("gt.behaviour.feed.chocolate"));
		return aList;
	}
}

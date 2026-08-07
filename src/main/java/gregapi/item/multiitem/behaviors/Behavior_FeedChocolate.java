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
 *
 * Modified in 2026 for the GregTech 6 NeoForge port
 * (https://github.com/wolfram0108/gregtech6_w): ported from Minecraft 1.7.10 / Forge
 * to Minecraft 26.1.2 / NeoForge.
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
			UT.Sounds.send("eating", 1.0F, 1.0F + RNGSUS.nextFloat() - RNGSUS.nextFloat() * 0.2F, aEntity); // F-sound: 1.7.10 world.playSoundAtEntity(entity,String,vol,pitch) -> центр UT.Sounds.send(String,vol,pitch,Entity) 1:1.
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

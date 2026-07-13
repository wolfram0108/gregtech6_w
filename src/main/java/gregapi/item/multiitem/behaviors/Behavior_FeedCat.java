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
import net.minecraft.entity.ai.EntityAITasks.EntityAITaskEntry;
import net.minecraft.entity.ai.EntityAITempt;
import net.minecraft.world.entity.animal.feline.Ocelot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class Behavior_FeedCat extends AbstractBehaviorDefault {
	public static final Behavior_FeedCat INSTANCE = new Behavior_FeedCat();
	
	@Override
	public boolean onRightClickEntity(MultiItem aItem, ItemStack aStack, Player aPlayer, Entity aEntity) {
		if (aEntity instanceof Ocelot) {
			for (Object tTask : ((Ocelot)aEntity).tasks.taskEntries) if (((EntityAITaskEntry)tTask).action instanceof EntityAITempt && ((EntityAITempt)((EntityAITaskEntry)tTask).action).isRunning()) {
				if (aPlayer.distanceToSqr(aEntity) < 9.0D) {
					UT.Entities.consumeCurrentItem(aPlayer);
					if (!aPlayer.level().isClientSide()) {
						if (RNGSUS.nextInt(3) == 0) {
							((Ocelot)aEntity).setTamed(T);
							((Ocelot)aEntity).setTameSkin(1 + RNGSUS.nextInt(3));
							((Ocelot)aEntity).func_152115_b(aPlayer.getUUID().toString());
							for (int i = 0; i < 7; ++i) aEntity.level().spawnParticle("heart", aEntity.getX() + (RNGSUS.nextFloat() * aEntity.getBbWidth() * 2.0F) - aEntity.getBbWidth(), aEntity.getY() + 0.5D + (RNGSUS.nextFloat() * aEntity.getBbHeight()), aEntity.getZ() + (RNGSUS.nextFloat() * aEntity.getBbWidth() * 2.0F) - aEntity.getBbWidth(), RNGSUS.nextGaussian() * 0.02D, RNGSUS.nextGaussian() * 0.02D, RNGSUS.nextGaussian() * 0.02D);
							((Ocelot)aEntity).level().setEntityState(aEntity, (byte)7);
						} else {
							for (int i = 0; i < 7; ++i) aEntity.level().spawnParticle("smoke", aEntity.getX() + (RNGSUS.nextFloat() * aEntity.getBbWidth() * 2.0F) - aEntity.getBbWidth(), aEntity.getY() + 0.5D + (RNGSUS.nextFloat() * aEntity.getBbHeight()), aEntity.getZ() + (RNGSUS.nextFloat() * aEntity.getBbWidth() * 2.0F) - aEntity.getBbWidth(), RNGSUS.nextGaussian() * 0.02D, RNGSUS.nextGaussian() * 0.02D, RNGSUS.nextGaussian() * 0.02D);
							((Ocelot)aEntity).level().setEntityState(aEntity, (byte)6);
						}
					}
				}
				return T;
			}
			if (((Ocelot)aEntity).isTamed() && ((Ocelot)aEntity).getGrowingAge() == 0 && !((Ocelot)aEntity).isInLove()) {
				UT.Entities.consumeCurrentItem(aPlayer);
				((Ocelot)aEntity).func_146082_f(aPlayer);
				return T;
			}
		}
		return F;
	}
	
	static {
		LH.add("gt.behaviour.feed.cat", "Is usable as Cat Food");
	}
	
	@Override
	public List<String> getAdditionalToolTips(MultiItem aItem, List<String> aList, ItemStack aStack) {
		aList.add(LH.get("gt.behaviour.feed.cat"));
		return aList;
	}
}

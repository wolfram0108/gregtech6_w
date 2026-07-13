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
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class Behavior_FeedGrass extends AbstractBehaviorDefault {
	public static final Behavior_FeedGrass INSTANCE = new Behavior_FeedGrass();
	
	@Override
	public boolean onRightClickEntity(MultiItem aItem, ItemStack aStack, Player aPlayer, Entity aEntity) {
		if (aEntity instanceof Cow || aEntity instanceof Sheep) {
			if (((Animal)aEntity).getGrowingAge() == 0 && !((Animal)aEntity).isInLove()) {
				UT.Entities.consumeCurrentItem(aPlayer);
				((Animal)aEntity).func_146082_f(aPlayer);
				return T;
			}
		}
		if (aEntity instanceof Horse) {
			boolean tConsume = F;
			if (((Horse)aEntity).getHealth() < ((Horse)aEntity).getMaxHealth()) {
				((Horse)aEntity).heal(1);
				tConsume = T;
			}
			if (!((Horse)aEntity).isAdultHorse()) {
				((Horse)aEntity).addGrowth(30);
				tConsume = T;
			}
			if (tConsume || !((Horse)aEntity).isTame()) {
				tConsume = T;
				((Horse)aEntity).increaseTemper(2);
			}
			if (tConsume) {
				((Horse)aEntity).level().playSoundAtEntity(aEntity, "eating", 1.0F, 1.0F + RNGSUS.nextFloat() - RNGSUS.nextFloat() * 0.2F);
				UT.Entities.consumeCurrentItem(aPlayer);
			}
			return tConsume;
		}
		return F;
	}
	
	static {
		LH.add("gt.behaviour.feed.grass", "Is usable as Cow, Sheep and Horse Food");
	}
	
	@Override
	public List<String> getAdditionalToolTips(MultiItem aItem, List<String> aList, ItemStack aStack) {
		aList.add(LH.get("gt.behaviour.feed.grass"));
		return aList;
	}
}

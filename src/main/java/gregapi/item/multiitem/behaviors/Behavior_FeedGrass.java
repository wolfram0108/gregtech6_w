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
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class Behavior_FeedGrass extends AbstractBehaviorDefault {
	public static final Behavior_FeedGrass INSTANCE = new Behavior_FeedGrass();
	
	@Override
	public boolean onRightClickEntity(MultiItem aItem, ItemStack aStack, Player aPlayer, Entity aEntity) {
		if (aEntity instanceof Cow || aEntity instanceof Sheep) {
			if (((Animal)aEntity).getAge() == 0 && !((Animal)aEntity).isInLove()) {
				UT.Entities.consumeCurrentItem(aPlayer);
				((Animal)aEntity).setInLove(aPlayer);
				return T;
			}
		}
		if (aEntity instanceof Horse) {
			boolean tConsume = F;
			if (((Horse)aEntity).getHealth() < ((Horse)aEntity).getMaxHealth()) {
				((Horse)aEntity).heal(1);
				tConsume = T;
			}
			if (((Horse)aEntity).isBaby()) { // 1.7.10 !isAdultHorse() == neo isBaby() (age<0, AgeableMob.java:245).
				((Horse)aEntity).ageUp(30); // addGrowth(30) and neo's ageUp(30) are the same operation (age += seconds*20, capped at 0).
				tConsume = T;
			}
			if (tConsume || !((Horse)aEntity).isTamed()) { // AbstractHorse does not extend TamableAnimal; it has its own isTamed(), not isTame().
				tConsume = T;
				((Horse)aEntity).modifyTemper(2); // 1.7.10 increaseTemper(2) == neo modifyTemper(2) (clamp temper+amount, AbstractHorse.java:248).
			}
			if (tConsume) {
				UT.Sounds.send("eating", 1.0F, 1.0F + RNGSUS.nextFloat() - RNGSUS.nextFloat() * 0.2F, aEntity); // Uses the central sound helper instead of the removed world.playSoundAtEntity call.
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

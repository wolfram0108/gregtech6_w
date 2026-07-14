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
// F-entity-identity: 1.7.10 EntityOcelot БЫЛ приручаемым котом (отдельного Cat до 1.14 не было) -> neo Cat
// (Cat extends TamableAnimal, Cat.java:69); neo Ocelot extends Animal — НЕ приручаем ("trust", не tame).
import net.minecraft.world.entity.animal.feline.Cat;
// F-entity-ai: 1.7.10 EntityAITasks/EntityAITaskEntry/EntityAITempt удалены -> neo GoalSelector/WrappedGoal/TemptGoal.
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class Behavior_FeedCat extends AbstractBehaviorDefault {
	public static final Behavior_FeedCat INSTANCE = new Behavior_FeedCat();

	@Override
	public boolean onRightClickEntity(MultiItem aItem, ItemStack aStack, Player aPlayer, Entity aEntity) {
		if (aEntity instanceof Cat) {
			Cat tCat = (Cat)aEntity;
			// 1.7.10: ((EntityOcelot)aEntity).tasks.taskEntries -> ищем активный EntityAITempt.
			// neo: goalSelector.getAvailableGoals() -> WrappedGoal, getGoal() instanceof TemptGoal (ловит CatTemptGoal, Cat.java:637) + isRunning() (WrappedGoal.java:76).
			for (WrappedGoal tTask : tCat.goalSelector.getAvailableGoals()) if (tTask.getGoal() instanceof TemptGoal && tTask.isRunning()) {
				if (aPlayer.distanceToSqr(aEntity) < 9.0D) {
					UT.Entities.consumeCurrentItem(aPlayer);
					if (!aPlayer.level().isClientSide()) {
						if (RNGSUS.nextInt(3) == 0) {
							tCat.setTame(T, true);
							// 1.7.10 setTameSkin(1+rng(3)) — int-скин 0-3. neo: модель сменилась на registry Holder<CatVariant>
							// (setVariant private, Cat.java:121) -> публичный путь Entity.setComponent (Entity.java:4064) +
							// случайный вариант из реестра (оригинал тоже рандомил скин при приручении).
							tCat.setComponent(net.minecraft.core.component.DataComponents.CAT_VARIANT, tCat.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.CAT_VARIANT).getRandom(tCat.getRandom()).orElseThrow());
							tCat.setOwner(aPlayer); // 1.7.10 func_152115_b(uuid)=setOwnerUUID(player.uuid); neo setOwner(LivingEntity) ставит владельца по сущности (TamableAnimal.java:163).
							for (int i = 0; i < 7; ++i) aEntity.level().addParticle(net.minecraft.core.particles.ParticleTypes.HEART, aEntity.getX() + (RNGSUS.nextFloat() * aEntity.getBbWidth() * 2.0F) - aEntity.getBbWidth(), aEntity.getY() + 0.5D + (RNGSUS.nextFloat() * aEntity.getBbHeight()), aEntity.getZ() + (RNGSUS.nextFloat() * aEntity.getBbWidth() * 2.0F) - aEntity.getBbWidth(), RNGSUS.nextGaussian() * 0.02D, RNGSUS.nextGaussian() * 0.02D, RNGSUS.nextGaussian() * 0.02D);
							tCat.level().broadcastEntityEvent(aEntity, (byte)7);
						} else {
							for (int i = 0; i < 7; ++i) aEntity.level().addParticle(net.minecraft.core.particles.ParticleTypes.SMOKE, aEntity.getX() + (RNGSUS.nextFloat() * aEntity.getBbWidth() * 2.0F) - aEntity.getBbWidth(), aEntity.getY() + 0.5D + (RNGSUS.nextFloat() * aEntity.getBbHeight()), aEntity.getZ() + (RNGSUS.nextFloat() * aEntity.getBbWidth() * 2.0F) - aEntity.getBbWidth(), RNGSUS.nextGaussian() * 0.02D, RNGSUS.nextGaussian() * 0.02D, RNGSUS.nextGaussian() * 0.02D);
							tCat.level().broadcastEntityEvent(aEntity, (byte)6);
						}
					}
				}
				return T;
			}
			if (tCat.isTame() && tCat.getAge() == 0 && !tCat.isInLove()) {
				UT.Entities.consumeCurrentItem(aPlayer);
				tCat.setInLove(aPlayer);
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

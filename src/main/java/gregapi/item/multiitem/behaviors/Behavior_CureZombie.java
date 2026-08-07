/**
 * Copyright (c) 2023 GregTech-6 Team
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

import gregapi.data.LH;
import gregapi.item.multiitem.MultiItem;
import gregapi.item.multiitem.behaviors.IBehavior.AbstractBehaviorDefault;
import gregapi.util.UT;
import net.minecraft.world.entity.Entity;
// F-entity-identity: 1.7.10 EntityZombie.isVillager() -> neo отдельный класс ZombieVillager (ZombieVillager.java:59).
import net.minecraft.world.entity.monster.zombie.ZombieVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.effect.MobEffects;

import java.util.List;

import static gregapi.data.CS.*;

public class Behavior_CureZombie extends AbstractBehaviorDefault {
	public final int mAverageCureTime;
	public final boolean mNeedsWeakness;
	
	public Behavior_CureZombie(int aAverageCureTime, boolean aNeedsWeakness) {
		mAverageCureTime = aAverageCureTime;
		mNeedsWeakness = aNeedsWeakness;
	}
	
	@Override
	public boolean onRightClickEntity(MultiItem aItem, ItemStack aStack, Player aPlayer, Entity aEntity) {
		if (aEntity instanceof ZombieVillager) {
			ZombieVillager tZombie = (ZombieVillager)aEntity;
			if (!mNeedsWeakness || tZombie.hasEffect(MobEffects.WEAKNESS)) {
				UT.Entities.consumeCurrentItem(aPlayer);
				if (!tZombie.level().isClientSide()) {
					int tCureTime = RNGSUS.nextInt(mAverageCureTime * 2) + 500;
					// F-entity-conversion (ADR: движок централизовал запуск конверсии в приватный ZombieVillager.startConverting).
					// Оригинал GT6 заводил конверсию через NBT-ключ "ConversionTime" (writeToNBT/readFromNBT) + вручную:
					// datawatcher-флаг 14 + removePotion(weakness) + addPotion(strength, tCureTime, min(diff-1,0)) + setEntityState(16).
					// neo: тот же ключ "ConversionTime" читается readAdditionalSaveData (ZombieVillager.java:118-121) -> startConverting,
					// который ЦЕНТРАЛИЗОВАННО делает ВСЁ ручное (флаг DATA_CONVERTING_ID + removeEffect(WEAKNESS) +
					// addEffect(STRENGTH, time, min(diff.getId()-1,0)) + broadcastEntityEvent(16), строки 200-207) — ручные строки
					// СНЯТЫ (движок их поглотил, 1:1 по эффекту, включая тот же min(diff-1,0)-амплитудный расчёт).
					// save/load через ValueOutput/ValueInput (NBT-рефактор) -> мост TagValueOutput/TagValueInput(CompoundTag).
					net.minecraft.world.level.storage.TagValueOutput tOut = net.minecraft.world.level.storage.TagValueOutput.createWithContext(net.minecraft.util.ProblemReporter.DISCARDING, tZombie.registryAccess());
					tZombie.saveWithoutId(tOut);
					CompoundTag tNBT = tOut.buildResult();
					tNBT.putInt("ConversionTime", tCureTime);
					tZombie.load(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING, tZombie.registryAccess(), tNBT));
				}
				return T;
			}
		}
		return F;
	}
	
	static {
		LH.add("gt.behaviour.cure.zombie.strong", "Can be used to cure strong Zombie Villagers");
		LH.add("gt.behaviour.cure.zombie.weak", "Can be used to cure weakened Zombie Villagers");
	}
	
	@Override
	public List<String> getAdditionalToolTips(MultiItem aItem, List<String> aList, ItemStack aStack) {
		aList.add(LH.get(mNeedsWeakness ? "gt.behaviour.cure.zombie.weak" : "gt.behaviour.cure.zombie.strong"));
		return aList;
	}
}

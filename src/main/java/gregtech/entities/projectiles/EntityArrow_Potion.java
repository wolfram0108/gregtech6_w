/**
 * Copyright (c) 2022 GregTech-6 Team
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

package gregtech.entities.projectiles;

import gregapi.util.UT;
import gregtech.entities.EntitiesGT;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;

import static gregapi.data.CS.*;

public class EntityArrow_Potion extends EntityArrow_Material {
	
	// F12-entity: тип-ctor = фабрика (EntitiesGT.ARROW_POTION, ссылка EntityArrow_Potion::new). Convenience-ctor'ы
	// передают ARROW_POTION в protected type-ctor'ы EntityArrow_Material (иначе потион-стрела получила бы ARROW_MATERIAL).
	public EntityArrow_Potion(EntityType<? extends EntityArrow_Potion> aType, Level aWorld) {
		super(aType, aWorld);
	}

	public EntityArrow_Potion(Level aWorld) {
		super(EntitiesGT.ARROW_POTION.get(), aWorld);
	}

	public EntityArrow_Potion(Level aWorld, double aX, double aY, double aZ) {
		super(EntitiesGT.ARROW_POTION.get(), aWorld, aX, aY, aZ);
	}

	public EntityArrow_Potion(Level aWorld, LivingEntity aEntity, float aSpeed) {
		super(EntitiesGT.ARROW_POTION.get(), aWorld, aEntity, aSpeed);
	}
	
	
	@Override public void addAdditionalSaveData(net.minecraft.world.level.storage.ValueOutput aNBT) {
		super.addAdditionalSaveData(aNBT);
		aNBT.putIntArray("mPotions", mPotions);
	}

	@Override public void readAdditionalSaveData(net.minecraft.world.level.storage.ValueInput aNBT) {
		super.readAdditionalSaveData(aNBT);
		setPotions(aNBT.getIntArray("mPotions").orElse(new int[0])); // neo ValueInput.getIntArray -> Optional<int[]>
	}
	
	@Override
	public boolean breaksOnImpact() {
		return T;
	}
	
	/**
	 * @param aPotions An Array of Potion Effects with %4==0 Elements as follows
	 * ID of a Potion Effect. 0 for none
	 * Duration of the Potion in Ticks
	 * Level of the Effect. [0, 1, 2] are for [I, II, III]
	 * The likelihood that this Potion Effect takes place upon being eaten [1 - 100]
	 */
	public void setPotions(int... aPotions) {
		if (aPotions != null) mPotions = aPotions;
	}
	
	public int[] getPotions() {
		return mPotions;
	}
	
	private int[] mPotions = new int[0];
	
	@Override
	public int[] onHitEntity(Entity aHitEntity, Entity aShootingEntity, ItemStack aArrow, int aRegularDamage, int aMagicDamage, int aKnockback, int aFireDamage, int aHitTimer) {
		if (aHitEntity instanceof LivingEntity) for (int i = 3; i < mPotions.length; i+=4) {
			if (RNGSUS.nextInt(100) < mPotions[i]) {
				UT.Entities.applyPotion(aHitEntity, mPotions[i-3], mPotions[i-2], mPotions[i-1], F);
			}
		}
		return super.onHitEntity(aHitEntity, aShootingEntity, aArrow, 1, aMagicDamage, aKnockback, aFireDamage, aHitTimer);
	}
}

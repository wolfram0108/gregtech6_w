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
 */

package gregapi.player;

import gregapi.code.ArrayListNoNulls;
import gregapi.damage.DamageSources;
import gregapi.util.UT;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.attachment.AttachmentType;

import static gregapi.data.CS.*;

/**
 * @author Gregorius Techneticies
 */
public class EntityFoodTracker implements AttachmentType {
	public static ArrayListNoNulls<EntityFoodTracker> TICK_LIST = new ArrayListNoNulls<>();
	
	public byte mAlcohol = 0, mCaffeine = 0, mDehydration = 0, mSugar = 0, mFat = 0, mRadiation = 0;
	public final LivingEntity mEntity;
	
	public EntityFoodTracker(LivingEntity aEntity) {
		mEntity = aEntity;
	}
	
	// @Override
	public void saveNBTData(CompoundTag aNBT) {
		CompoundTag tNBT = UT.NBT.make();
		if (mAlcohol     != 0) tNBT.putByte("a", mAlcohol    );
		if (mCaffeine    != 0) tNBT.putByte("c", mCaffeine   );
		if (mSugar       != 0) tNBT.putByte("s", mSugar      );
		if (mDehydration != 0) tNBT.putByte("d", mDehydration);
		if (mFat         != 0) tNBT.putByte("f", mFat        );
		if (mRadiation   != 0) tNBT.putByte("r", mRadiation  );
		if (tNBT.isEmpty()) aNBT.removeTag("gt.props.food"); else aNBT.put("gt.props.food", tNBT);
	}
	
	// @Override
	public void loadNBTData(CompoundTag aNBT) {
		CompoundTag tNBT = aNBT.getCompoundTag("gt.props.food");
		if (tNBT == null) return;
		mAlcohol     = tNBT.getByte("a");
		mCaffeine    = tNBT.getByte("c");
		mDehydration = tNBT.getByte("d");
		mSugar       = tNBT.getByte("s");
		mFat         = tNBT.getByte("f");
		mRadiation   = tNBT.getByte("r");
	}
	
	public void init(Entity aEntity, Level aWorld) {TICK_LIST.add(this);}
	public void changeAlcohol    (long aAmount) {mAlcohol     = UT.Code.bind7(mAlcohol     + aAmount);}
	public void changeCaffeine   (long aAmount) {mCaffeine    = UT.Code.bind7(mCaffeine    + aAmount);}
	public void changeDehydration(long aAmount) {mDehydration = UT.Code.bind7(mDehydration + aAmount);}
	public void changeSugar      (long aAmount) {mSugar       = UT.Code.bind7(mSugar       + aAmount);}
	public void changeFat        (long aAmount) {mFat         = UT.Code.bind7(mFat         + aAmount);}
	public void changeRadiation  (long aAmount) {mRadiation   = UT.Code.bind7(mRadiation   + aAmount);}
	
	public static void tick() {
		if (SERVER_TIME % 50 == 0) for (int i = 0; i < TICK_LIST.size(); i++) {
			EntityFoodTracker tTracker = TICK_LIST.get(i);
			if (tTracker.mEntity.isDead) {TICK_LIST.remove(i--); continue;}
			
			if (tTracker.mAlcohol >= 100) {
				if (FOOD_OVERDOSE_DEATH || tTracker.mEntity.getHealth() >= 2)
				tTracker.mEntity.attackEntityFrom(DamageSources.getAlcoholDamage(), FOOD_OVERDOSE_DEATH?2:1);
				UT.Entities.applyPotion(tTracker.mEntity, MobEffect.confusion, 1200, 2, F);
				UT.Entities.applyPotion(tTracker.mEntity, MobEffect.damageBoost, 300, 3, F);
			} else if (tTracker.mAlcohol >= 75) {
				UT.Entities.applyPotion(tTracker.mEntity, MobEffect.confusion, 1200, 1, F);
				UT.Entities.applyPotion(tTracker.mEntity, MobEffect.damageBoost, 300, 2, F);
			} else if (tTracker.mAlcohol >= 50) {
				UT.Entities.applyPotion(tTracker.mEntity, MobEffect.confusion, 1200, 0, F);
				UT.Entities.applyPotion(tTracker.mEntity, MobEffect.damageBoost, 300, 1, F);
			} else if (tTracker.mAlcohol >= 25) {
				UT.Entities.applyPotion(tTracker.mEntity, MobEffect.damageBoost, 300, 0, F);
			}
			
			if (tTracker.mCaffeine >= 100) {
				if (FOOD_OVERDOSE_DEATH || tTracker.mEntity.getHealth() >= 2)
				tTracker.mEntity.attackEntityFrom(DamageSources.getCaffeineDamage(), FOOD_OVERDOSE_DEATH?2:1);
				UT.Entities.applyPotion(tTracker.mEntity, MobEffect.weakness, 1200, 2, F);
				UT.Entities.applyPotion(tTracker.mEntity, MobEffect.digSpeed, 300, 3, F);
			} else if (tTracker.mCaffeine >= 75) {
				UT.Entities.applyPotion(tTracker.mEntity, MobEffect.weakness, 1200, 1, F);
				UT.Entities.applyPotion(tTracker.mEntity, MobEffect.digSpeed, 300, 2, F);
			} else if (tTracker.mCaffeine >= 50) {
				UT.Entities.applyPotion(tTracker.mEntity, MobEffect.weakness, 1200, 0, F);
				UT.Entities.applyPotion(tTracker.mEntity, MobEffect.digSpeed, 300, 1, F);
			} else if (tTracker.mCaffeine >= 25) {
				UT.Entities.applyPotion(tTracker.mEntity, MobEffect.digSpeed, 300, 0, F);
			}
			
			if (tTracker.mRadiation >= 100) {
				UT.Entities.applyPotion(tTracker.mEntity, PotionsGT.ID_RADIATION >= 0 ? PotionsGT.ID_RADIATION : MobEffect.wither.id, 100, 0, F);
				UT.Entities.applyPotion(tTracker.mEntity, MobEffect.confusion, 100, 2, F);
				UT.Entities.applyPotion(tTracker.mEntity, MobEffect.hunger, 100, 2, F);
				UT.Entities.applyPotion(tTracker.mEntity, MobEffect.moveSlowdown, 100, 2, F);
				UT.Entities.applyPotion(tTracker.mEntity, MobEffect.digSlowdown, 100, 2, F);
				UT.Entities.applyPotion(tTracker.mEntity, MobEffect.weakness, 100, 2, F);
			} else if (tTracker.mRadiation >= 75) {
				UT.Entities.applyPotion(tTracker.mEntity, PotionsGT.ID_RADIATION >= 0 ? PotionsGT.ID_RADIATION : MobEffect.poison.id, 100, 0, F);
				UT.Entities.applyPotion(tTracker.mEntity, MobEffect.confusion, 100, 1, F);
				UT.Entities.applyPotion(tTracker.mEntity, MobEffect.hunger, 100, 1, F);
				UT.Entities.applyPotion(tTracker.mEntity, MobEffect.moveSlowdown, 100, 1, F);
				UT.Entities.applyPotion(tTracker.mEntity, MobEffect.digSlowdown, 100, 1, F);
				UT.Entities.applyPotion(tTracker.mEntity, MobEffect.weakness, 100, 1, F);
			} else if (tTracker.mRadiation >= 50) {
				UT.Entities.applyPotion(tTracker.mEntity, PotionsGT.ID_RADIATION >= 0 ? PotionsGT.ID_RADIATION : MobEffect.poison.id, 100, 0, F);
				UT.Entities.applyPotion(tTracker.mEntity, MobEffect.confusion, 100, 0, F);
				UT.Entities.applyPotion(tTracker.mEntity, MobEffect.hunger, 100, 0, F);
				UT.Entities.applyPotion(tTracker.mEntity, MobEffect.moveSlowdown, 100, 0, F);
				UT.Entities.applyPotion(tTracker.mEntity, MobEffect.digSlowdown, 100, 0, F);
				UT.Entities.applyPotion(tTracker.mEntity, MobEffect.weakness, 100, 0, F);
			} else if (tTracker.mRadiation >= 25) {
				UT.Entities.applyPotion(tTracker.mEntity, PotionsGT.ID_RADIATION >= 0 ? PotionsGT.ID_RADIATION : MobEffect.poison.id, 100, 0, F);
			}
			
			if (NUTRITION_SYSTEM) {
				if (tTracker.mFat >= 100) {
					if (FOOD_OVERDOSE_DEATH || tTracker.mEntity.getHealth() >= 2)
					tTracker.mEntity.attackEntityFrom(DamageSources.getFatDamage(), FOOD_OVERDOSE_DEATH?2:1);
					UT.Entities.applyPotion(tTracker.mEntity, MobEffect.moveSlowdown, 1200, 2, F);
					UT.Entities.applyPotion(tTracker.mEntity, MobEffect.resistance, 300, 3, F);
				} else if (tTracker.mFat >= 75) {
					UT.Entities.applyPotion(tTracker.mEntity, MobEffect.moveSlowdown, 1200, 1, F);
					UT.Entities.applyPotion(tTracker.mEntity, MobEffect.resistance, 300, 2, F);
				} else if (tTracker.mFat >= 50) {
					UT.Entities.applyPotion(tTracker.mEntity, MobEffect.moveSlowdown, 1200, 0, F);
					UT.Entities.applyPotion(tTracker.mEntity, MobEffect.resistance, 300, 1, F);
				} else if (tTracker.mFat >= 25) {
					UT.Entities.applyPotion(tTracker.mEntity, MobEffect.resistance, 300, 0, F);
				}
				
				if (tTracker.mSugar >= 100) {
					if (FOOD_OVERDOSE_DEATH || tTracker.mEntity.getHealth() >= 2)
					tTracker.mEntity.attackEntityFrom(DamageSources.getSugarDamage(), FOOD_OVERDOSE_DEATH?2:1);
					UT.Entities.applyPotion(tTracker.mEntity, MobEffect.digSlowdown, 1200, 2, F);
					UT.Entities.applyPotion(tTracker.mEntity, MobEffect.moveSpeed, 300, 3, F);
					UT.Entities.applyPotion(tTracker.mEntity, MobEffect.jump, 300, 3, F);
				} else if (tTracker.mSugar >= 75) {
					UT.Entities.applyPotion(tTracker.mEntity, MobEffect.digSlowdown, 1200, 1, F);
					UT.Entities.applyPotion(tTracker.mEntity, MobEffect.moveSpeed, 300, 2, F);
					UT.Entities.applyPotion(tTracker.mEntity, MobEffect.jump, 300, 2, F);
				} else if (tTracker.mSugar >= 50) {
					UT.Entities.applyPotion(tTracker.mEntity, MobEffect.digSlowdown, 1200, 0, F);
					UT.Entities.applyPotion(tTracker.mEntity, MobEffect.moveSpeed, 300, 1, F);
					UT.Entities.applyPotion(tTracker.mEntity, MobEffect.jump, 300, 1, F);
				} else if (tTracker.mSugar >= 25) {
					UT.Entities.applyPotion(tTracker.mEntity, MobEffect.moveSpeed, 300, 0, F);
					UT.Entities.applyPotion(tTracker.mEntity, MobEffect.jump, 300, 0, F);
				}
				
				if (tTracker.mDehydration >= 100) {
					if (FOOD_OVERDOSE_DEATH || tTracker.mEntity.getHealth() >= 2)
					tTracker.mEntity.attackEntityFrom(DamageSources.getDehydrationDamage(), FOOD_OVERDOSE_DEATH?2:1);
					UT.Entities.applyPotion(tTracker.mEntity, PotionsGT.ID_DEHYDRATION >= 0 ? PotionsGT.ID_DEHYDRATION : MobEffect.hunger.id, 1200, 3, F);
				} else if (tTracker.mDehydration >= 75) {
					UT.Entities.applyPotion(tTracker.mEntity, PotionsGT.ID_DEHYDRATION >= 0 ? PotionsGT.ID_DEHYDRATION : MobEffect.hunger.id, 1200, 2, F);
				} else if (tTracker.mDehydration >= 50) {
					UT.Entities.applyPotion(tTracker.mEntity, PotionsGT.ID_DEHYDRATION >= 0 ? PotionsGT.ID_DEHYDRATION : MobEffect.hunger.id, 1200, 1, F);
				} else if (tTracker.mDehydration >= 25) {
					UT.Entities.applyPotion(tTracker.mEntity, PotionsGT.ID_DEHYDRATION >= 0 ? PotionsGT.ID_DEHYDRATION : MobEffect.hunger.id, 1200, 0, F);
				}
			}
			
			if (SERVER_TIME % 100 == 0) {
				if (tTracker.mAlcohol     > 0) tTracker.mAlcohol--;
				if (tTracker.mCaffeine    > 0) tTracker.mCaffeine--;
				if (tTracker.mDehydration > 0) tTracker.mDehydration--;
				if (tTracker.mSugar       > 0) tTracker.mSugar--;
				if (tTracker.mFat         > 0) tTracker.mFat--;
				//if (tTracker.mRadiation > 0) tTracker.mRadiation--; // The only one that does not decrease, so you will have to deal with it until you either die or get a Radaway,
			}
		}
	}
	
	public static void add(LivingEntity aEntity) {
		if (aEntity == null || aEntity.level().isClientSide()) return;
		aEntity.registerExtendedProperties("gt.props.food", new EntityFoodTracker(aEntity));
	}
	
	public static EntityFoodTracker get(Entity aEntity) {
		if (aEntity == null || aEntity.level().isClientSide()) return null;
		Object rTracker = aEntity.getExtendedProperties("gt.props.food");
		return rTracker instanceof EntityFoodTracker ? (EntityFoodTracker)rTracker : null;
	}
}

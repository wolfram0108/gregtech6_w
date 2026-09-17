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

package gregapi.player;

import gregapi.code.ArrayListNoNulls;
import gregapi.damage.DamageSources;
import gregapi.data.MD;
import gregapi.util.UT;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.Level;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.eventbus.api.IEventBus;

import static gregapi.data.CS.*;

/** @author Gregorius Techneticies
 *  1.7.10's IExtendedEntityProperties is gone; an entity capability fills the same role, with the engine itself
 *  handling persistence. The carrier attaches only to players, 1:1 with the original. */
public class EntityFoodTracker {
	public static ArrayListNoNulls<EntityFoodTracker> TICK_LIST = new ArrayListNoNulls<>();

	public byte mAlcohol = 0, mCaffeine = 0, mDehydration = 0, mSugar = 0, mFat = 0, mRadiation = 0;
	public final LivingEntity mEntity;

	public static final ResourceLocation ID = new ResourceLocation(MD.GAPI.mID, "food_tracker");
	public static final Capability<EntityFoodTracker> CAP = CapabilityManager.get(new CapabilityToken<EntityFoodTracker>() {});

	/** One record per player entity; persistence is the engine's own job, inside its own capability store. */
	private static final class Provider implements ICapabilitySerializable<CompoundTag> {
		private final EntityFoodTracker mData;
		private final LazyOptional<EntityFoodTracker> mOptional;
		private Provider(LivingEntity aEntity) {mData = new EntityFoodTracker(aEntity); mOptional = LazyOptional.of(() -> mData);}

		@Override public <T> LazyOptional<T> getCapability(Capability<T> aCapability, Direction aSide) {return aCapability == CAP ? mOptional.cast() : LazyOptional.empty();}
		@Override public CompoundTag serializeNBT() {CompoundTag rNBT = new CompoundTag(); mData.saveNBTData(rNBT); return rNBT;}
		@Override public void deserializeNBT(CompoundTag aNBT) {mData.loadNBTData(aNBT);}
	}

	/** The only subscription point for this carrier: capability declaration on the mod bus, attachment on the Forge bus. */
	public static void register(IEventBus aModBus) {
		aModBus.addListener((RegisterCapabilitiesEvent aEvent) -> aEvent.register(EntityFoodTracker.class));
		MinecraftForge.EVENT_BUS.addGenericListener(net.minecraft.world.entity.Entity.class, (AttachCapabilitiesEvent<net.minecraft.world.entity.Entity> aEvent) -> {
			if (aEvent.getObject() instanceof Player tPlayer) aEvent.addCapability(ID, new Provider(tPlayer));
		});
	}

	public EntityFoodTracker(LivingEntity aEntity) {
		mEntity = aEntity;
		// The constructor is verbatim original (assignment only); init() calls add() from the entity-join event, exactly as
		// the 1.7.10 framework called init right after registering the properties object.
	}

	/** 1:1 with the original: fields at their default value simply aren't written to NBT at all. */
	public void saveNBTData(CompoundTag aNBT) {
		if (mAlcohol     != 0) aNBT.putByte("a", mAlcohol    );
		if (mCaffeine    != 0) aNBT.putByte("c", mCaffeine   );
		if (mSugar       != 0) aNBT.putByte("s", mSugar      );
		if (mDehydration != 0) aNBT.putByte("d", mDehydration);
		if (mFat         != 0) aNBT.putByte("f", mFat        );
		if (mRadiation   != 0) aNBT.putByte("r", mRadiation  );
	}

	public void loadNBTData(CompoundTag aNBT) {
		mAlcohol     = aNBT.getByte("a");
		mCaffeine    = aNBT.getByte("c");
		mDehydration = aNBT.getByte("d");
		mSugar       = aNBT.getByte("s");
		mFat         = aNBT.getByte("f");
		mRadiation   = aNBT.getByte("r");
	}

	/** A capability carrier survives a dimension change (the same player instance re-enters), so joining the tick list is
	 *  guarded against duplicates, or every one of its effects would apply twice. */
	public void init(Entity aEntity, Level aWorld) {if (!TICK_LIST.contains(this)) TICK_LIST.add(this);}
	public void changeAlcohol    (long aAmount) {mAlcohol     = UT.Code.bind7(mAlcohol     + aAmount);}
	public void changeCaffeine   (long aAmount) {mCaffeine    = UT.Code.bind7(mCaffeine    + aAmount);}
	public void changeDehydration(long aAmount) {mDehydration = UT.Code.bind7(mDehydration + aAmount);}
	public void changeSugar      (long aAmount) {mSugar       = UT.Code.bind7(mSugar       + aAmount);}
	public void changeFat        (long aAmount) {mFat         = UT.Code.bind7(mFat         + aAmount);}
	public void changeRadiation  (long aAmount) {mRadiation   = UT.Code.bind7(mRadiation   + aAmount);}

	public static void tick() {
		if (SERVER_TIME % 50 == 0) for (int i = 0; i < TICK_LIST.size(); i++) {
			EntityFoodTracker tTracker = TICK_LIST.get(i);
			// Attachment deserialization replaces the whole stored value after construction, so a fresh instance can
			// be replaced by a disk-loaded one before it ticks; without this check the orphaned instance keeps ticking stale.
			if (tTracker.mEntity.isRemoved() || get(tTracker.mEntity) != tTracker) {TICK_LIST.remove(i--); continue;}

			if (tTracker.mAlcohol >= 100) {
				if (FOOD_OVERDOSE_DEATH || tTracker.mEntity.getHealth() >= 2)
				tTracker.mEntity.hurt(DamageSources.getAlcoholDamage(), FOOD_OVERDOSE_DEATH?2:1);
				UT.Entities.applyPotion(tTracker.mEntity, MobEffects.CONFUSION, 1200, 2, F);
				UT.Entities.applyPotion(tTracker.mEntity, MobEffects.DAMAGE_BOOST, 300, 3, F);
			} else if (tTracker.mAlcohol >= 75) {
				UT.Entities.applyPotion(tTracker.mEntity, MobEffects.CONFUSION, 1200, 1, F);
				UT.Entities.applyPotion(tTracker.mEntity, MobEffects.DAMAGE_BOOST, 300, 2, F);
			} else if (tTracker.mAlcohol >= 50) {
				UT.Entities.applyPotion(tTracker.mEntity, MobEffects.CONFUSION, 1200, 0, F);
				UT.Entities.applyPotion(tTracker.mEntity, MobEffects.DAMAGE_BOOST, 300, 1, F);
			} else if (tTracker.mAlcohol >= 25) {
				UT.Entities.applyPotion(tTracker.mEntity, MobEffects.DAMAGE_BOOST, 300, 0, F);
			}

			if (tTracker.mCaffeine >= 100) {
				if (FOOD_OVERDOSE_DEATH || tTracker.mEntity.getHealth() >= 2)
				tTracker.mEntity.hurt(DamageSources.getCaffeineDamage(), FOOD_OVERDOSE_DEATH?2:1);
				UT.Entities.applyPotion(tTracker.mEntity, MobEffects.WEAKNESS, 1200, 2, F);
				UT.Entities.applyPotion(tTracker.mEntity, MobEffects.DIG_SPEED, 300, 3, F);
			} else if (tTracker.mCaffeine >= 75) {
				UT.Entities.applyPotion(tTracker.mEntity, MobEffects.WEAKNESS, 1200, 1, F);
				UT.Entities.applyPotion(tTracker.mEntity, MobEffects.DIG_SPEED, 300, 2, F);
			} else if (tTracker.mCaffeine >= 50) {
				UT.Entities.applyPotion(tTracker.mEntity, MobEffects.WEAKNESS, 1200, 0, F);
				UT.Entities.applyPotion(tTracker.mEntity, MobEffects.DIG_SPEED, 300, 1, F);
			} else if (tTracker.mCaffeine >= 25) {
				UT.Entities.applyPotion(tTracker.mEntity, MobEffects.DIG_SPEED, 300, 0, F);
			}

			if (tTracker.mRadiation >= 100) {
				UT.Entities.applyPotion(tTracker.mEntity, PotionsGT.ID_RADIATION >= 0 ? PotionsGT.ID_RADIATION : UT.Entities.POTID_WITHER, 100, 0, F);
				UT.Entities.applyPotion(tTracker.mEntity, MobEffects.CONFUSION, 100, 2, F);
				UT.Entities.applyPotion(tTracker.mEntity, MobEffects.HUNGER, 100, 2, F);
				UT.Entities.applyPotion(tTracker.mEntity, MobEffects.MOVEMENT_SLOWDOWN, 100, 2, F);
				UT.Entities.applyPotion(tTracker.mEntity, MobEffects.DIG_SLOWDOWN, 100, 2, F);
				UT.Entities.applyPotion(tTracker.mEntity, MobEffects.WEAKNESS, 100, 2, F);
			} else if (tTracker.mRadiation >= 75) {
				UT.Entities.applyPotion(tTracker.mEntity, PotionsGT.ID_RADIATION >= 0 ? PotionsGT.ID_RADIATION : UT.Entities.POTID_POISON, 100, 0, F);
				UT.Entities.applyPotion(tTracker.mEntity, MobEffects.CONFUSION, 100, 1, F);
				UT.Entities.applyPotion(tTracker.mEntity, MobEffects.HUNGER, 100, 1, F);
				UT.Entities.applyPotion(tTracker.mEntity, MobEffects.MOVEMENT_SLOWDOWN, 100, 1, F);
				UT.Entities.applyPotion(tTracker.mEntity, MobEffects.DIG_SLOWDOWN, 100, 1, F);
				UT.Entities.applyPotion(tTracker.mEntity, MobEffects.WEAKNESS, 100, 1, F);
			} else if (tTracker.mRadiation >= 50) {
				UT.Entities.applyPotion(tTracker.mEntity, PotionsGT.ID_RADIATION >= 0 ? PotionsGT.ID_RADIATION : UT.Entities.POTID_POISON, 100, 0, F);
				UT.Entities.applyPotion(tTracker.mEntity, MobEffects.CONFUSION, 100, 0, F);
				UT.Entities.applyPotion(tTracker.mEntity, MobEffects.HUNGER, 100, 0, F);
				UT.Entities.applyPotion(tTracker.mEntity, MobEffects.MOVEMENT_SLOWDOWN, 100, 0, F);
				UT.Entities.applyPotion(tTracker.mEntity, MobEffects.DIG_SLOWDOWN, 100, 0, F);
				UT.Entities.applyPotion(tTracker.mEntity, MobEffects.WEAKNESS, 100, 0, F);
			} else if (tTracker.mRadiation >= 25) {
				UT.Entities.applyPotion(tTracker.mEntity, PotionsGT.ID_RADIATION >= 0 ? PotionsGT.ID_RADIATION : UT.Entities.POTID_POISON, 100, 0, F);
			}

			if (NUTRITION_SYSTEM) {
				if (tTracker.mFat >= 100) {
					if (FOOD_OVERDOSE_DEATH || tTracker.mEntity.getHealth() >= 2)
					tTracker.mEntity.hurt(DamageSources.getFatDamage(), FOOD_OVERDOSE_DEATH?2:1);
					UT.Entities.applyPotion(tTracker.mEntity, MobEffects.MOVEMENT_SLOWDOWN, 1200, 2, F);
					UT.Entities.applyPotion(tTracker.mEntity, MobEffects.DAMAGE_RESISTANCE, 300, 3, F);
				} else if (tTracker.mFat >= 75) {
					UT.Entities.applyPotion(tTracker.mEntity, MobEffects.MOVEMENT_SLOWDOWN, 1200, 1, F);
					UT.Entities.applyPotion(tTracker.mEntity, MobEffects.DAMAGE_RESISTANCE, 300, 2, F);
				} else if (tTracker.mFat >= 50) {
					UT.Entities.applyPotion(tTracker.mEntity, MobEffects.MOVEMENT_SLOWDOWN, 1200, 0, F);
					UT.Entities.applyPotion(tTracker.mEntity, MobEffects.DAMAGE_RESISTANCE, 300, 1, F);
				} else if (tTracker.mFat >= 25) {
					UT.Entities.applyPotion(tTracker.mEntity, MobEffects.DAMAGE_RESISTANCE, 300, 0, F);
				}

				if (tTracker.mSugar >= 100) {
					if (FOOD_OVERDOSE_DEATH || tTracker.mEntity.getHealth() >= 2)
					tTracker.mEntity.hurt(DamageSources.getSugarDamage(), FOOD_OVERDOSE_DEATH?2:1);
					UT.Entities.applyPotion(tTracker.mEntity, MobEffects.DIG_SLOWDOWN, 1200, 2, F);
					UT.Entities.applyPotion(tTracker.mEntity, MobEffects.MOVEMENT_SPEED, 300, 3, F);
					UT.Entities.applyPotion(tTracker.mEntity, MobEffects.JUMP, 300, 3, F);
				} else if (tTracker.mSugar >= 75) {
					UT.Entities.applyPotion(tTracker.mEntity, MobEffects.DIG_SLOWDOWN, 1200, 1, F);
					UT.Entities.applyPotion(tTracker.mEntity, MobEffects.MOVEMENT_SPEED, 300, 2, F);
					UT.Entities.applyPotion(tTracker.mEntity, MobEffects.JUMP, 300, 2, F);
				} else if (tTracker.mSugar >= 50) {
					UT.Entities.applyPotion(tTracker.mEntity, MobEffects.DIG_SLOWDOWN, 1200, 0, F);
					UT.Entities.applyPotion(tTracker.mEntity, MobEffects.MOVEMENT_SPEED, 300, 1, F);
					UT.Entities.applyPotion(tTracker.mEntity, MobEffects.JUMP, 300, 1, F);
				} else if (tTracker.mSugar >= 25) {
					UT.Entities.applyPotion(tTracker.mEntity, MobEffects.MOVEMENT_SPEED, 300, 0, F);
					UT.Entities.applyPotion(tTracker.mEntity, MobEffects.JUMP, 300, 0, F);
				}

				if (tTracker.mDehydration >= 100) {
					if (FOOD_OVERDOSE_DEATH || tTracker.mEntity.getHealth() >= 2)
					tTracker.mEntity.hurt(DamageSources.getDehydrationDamage(), FOOD_OVERDOSE_DEATH?2:1);
					UT.Entities.applyPotion(tTracker.mEntity, PotionsGT.ID_DEHYDRATION >= 0 ? PotionsGT.ID_DEHYDRATION : UT.Entities.POTID_HUNGER, 1200, 3, F);
				} else if (tTracker.mDehydration >= 75) {
					UT.Entities.applyPotion(tTracker.mEntity, PotionsGT.ID_DEHYDRATION >= 0 ? PotionsGT.ID_DEHYDRATION : UT.Entities.POTID_HUNGER, 1200, 2, F);
				} else if (tTracker.mDehydration >= 50) {
					UT.Entities.applyPotion(tTracker.mEntity, PotionsGT.ID_DEHYDRATION >= 0 ? PotionsGT.ID_DEHYDRATION : UT.Entities.POTID_HUNGER, 1200, 1, F);
				} else if (tTracker.mDehydration >= 25) {
					UT.Entities.applyPotion(tTracker.mEntity, PotionsGT.ID_DEHYDRATION >= 0 ? PotionsGT.ID_DEHYDRATION : UT.Entities.POTID_HUNGER, 1200, 0, F);
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

	/** The object itself is already built by the capability provider at entity construction; only the original's second
	 *  half survives here -- joining the tick list. */
	public static void add(LivingEntity aEntity) {
		if (aEntity == null || aEntity.level().isClientSide()) return;
		EntityFoodTracker tTracker = get(aEntity);
		if (tTracker != null) tTracker.init(aEntity, aEntity.level());
	}

	/** The capability is already typed by its own key now, so no instanceof cast is needed; a non-player entity still
	 *  returns null, the same semantics the original had. */
	public static EntityFoodTracker get(Entity aEntity) {
		if (aEntity == null || aEntity.level().isClientSide()) return null;
		return aEntity.getCapability(CAP).orElse(null);
	}
}

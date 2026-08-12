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
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import net.neoforged.neoforge.attachment.IAttachmentSerializer;

import net.minecraftforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import static gregapi.data.CS.*;

/**
 * @author Gregorius Techneticies
 *
 * F-attachment: 1.7.10 {@code IExtendedEntityProperties} (реализовывался этим же классом, "gt.props.food"
 * ключ через {@code Entity.registerExtendedProperties}/{@code getExtendedProperties}) удалён из движка
 * целиком. Neo-эквивалент — data attachment ({@code net.neoforged.neoforge.attachment.AttachmentType<T>},
 * verified {@code AttachmentType.java}/{@code IAttachmentHolder.java}/{@code IAttachmentSerializer.java}):
 * ЭТОТ класс — хранимые данные (носитель), НЕ дескриптор типа, поэтому {@code implements AttachmentType}
 * убран (AttachmentType — не интерфейс, а final-класс-дескриптор, см. AttachmentType.java:59); вместо
 * этого он РЕГИСТРИРУЕТСЯ как {@code AttachmentType<EntityFoodTracker>} через {@link #ATTACHMENTS}
 * (тот же паттерн, что {@code gregapi.fluid.FluidGT}/{@code gregapi.worldgen.GT6WorldgenFeature} —
 * {@code DeferredRegister.create(NeoForgeRegistries.Keys.X, MD.GAPI.mID)} + {@code .register(aModBus)}
 * из {@code GT_API}-конструктора).
 */
public class EntityFoodTracker {
	public static ArrayListNoNulls<EntityFoodTracker> TICK_LIST = new ArrayListNoNulls<>();

	public byte mAlcohol = 0, mCaffeine = 0, mDehydration = 0, mSugar = 0, mFat = 0, mRadiation = 0;
	public final LivingEntity mEntity;

	/** F-attachment: сериализатор NBT для трекера (см. saveNBTData/loadNBTData ниже). Контракт
	 *  IAttachmentSerializer.write() уже пишет в ValueOutput, СКОПИРОВАННЫЙ под ключ этого attachment-типа
	 *  (AttachmentHolder.serializeAttachments), поэтому обёртка "gt.props.food" из 1.7.10 больше не нужна —
	 *  сам контракт её обеспечивает. write()==false = "не сериализовывать" (1:1 эквивалент 1.7.10
	 *  aNBT.removeTag(...) при пустом наборе полей). */
	private static final IAttachmentSerializer<EntityFoodTracker> SERIALIZER = new IAttachmentSerializer<EntityFoodTracker>() {
		@Override
		public EntityFoodTracker read(IAttachmentHolder aHolder, ValueInput aInput) {
			EntityFoodTracker rTracker = new EntityFoodTracker((LivingEntity)aHolder);
			rTracker.loadNBTData(aInput);
			return rTracker;
		}
		@Override
		public boolean write(EntityFoodTracker aTracker, ValueOutput aOutput) {
			return aTracker.saveNBTData(aOutput);
		}
	};

	/** F-attachment: центральный DeferredRegister — ЕДИНСТВЕННОЕ место, где GT6 регистрирует Entity-
	 *  attachment-типы в neo. {@code .register(aModBus)} зовётся из центрального @Mod-конструктора
	 *  ({@code gregapi.GT_API#GT_API(IEventBus)}, тем же мод-басом, что FluidGT/GT6WorldgenFeature/…). */
	public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS = DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, MD.GAPI.mID);

	/** Дефолт-конструктор (для свежих сущностей, ни разу не сохранявшихся) И read()-конструктор (для
	 *  загруженных с диска) — ОБА идут через {@code new EntityFoodTracker(LivingEntity)}, который сам
	 *  регистрирует себя в {@link #TICK_LIST} (см. конструктор ниже) — не важно, каким из двух путей
	 *  экземпляр создан, он всегда попадёт в тик-лист ровно один раз. */
	public static final net.minecraftforge.registries.RegistryObject<AttachmentType<EntityFoodTracker>> TYPE = ATTACHMENTS.register("food_tracker",
		() -> AttachmentType.<EntityFoodTracker>builder(aHolder -> new EntityFoodTracker((LivingEntity)aHolder)).serialize(SERIALIZER).build());

	public EntityFoodTracker(LivingEntity aEntity) {
		mEntity = aEntity;
		// F-attachment: 1.7.10 ванильный фреймворк сам звал IExtendedEntityProperties.init(Entity,World)
		// сразу после registerExtendedProperties; у neo AttachmentType нет эквивалентного авто-хука
		// (defaultValueSupplier/IAttachmentSerializer только возвращают значение, ничего не зовут на нём) —
		// зовём явно здесь, чтобы КАЖДЫЙ сконструированный трекер (свежий через getData() ИЛИ
		// восстановленный из NBT через read()) попадал в TICK_LIST ровно один раз.
		init(aEntity, aEntity.level());
	}

	/** F-attachment: см. IAttachmentSerializer.write() выше — false = не сериализовывать (1:1 эквивалент
	 *  1.7.10 aNBT.removeTag("gt.props.food") при полностью нулевом наборе полей). */
	public boolean saveNBTData(ValueOutput aNBT) {
		boolean rAny = F;
		if (mAlcohol     != 0) {aNBT.putByte("a", mAlcohol    ); rAny = T;}
		if (mCaffeine    != 0) {aNBT.putByte("c", mCaffeine   ); rAny = T;}
		if (mSugar       != 0) {aNBT.putByte("s", mSugar      ); rAny = T;}
		if (mDehydration != 0) {aNBT.putByte("d", mDehydration); rAny = T;}
		if (mFat         != 0) {aNBT.putByte("f", mFat        ); rAny = T;}
		if (mRadiation   != 0) {aNBT.putByte("r", mRadiation  ); rAny = T;}
		return rAny;
	}

	public void loadNBTData(ValueInput aNBT) {
		mAlcohol     = aNBT.getByte("a");
		mCaffeine    = aNBT.getByte("c");
		mDehydration = aNBT.getByte("d");
		mSugar       = aNBT.getByte("s");
		mFat         = aNBT.getByte("f");
		mRadiation   = aNBT.getByte("r");
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
			// F-attachment: neo attachment-карта ЗАМЕНЯЕТ значение целиком (не мутирует на месте) при
			// десериализации из NBT (AttachmentHolder.deserializeAttachments -> raw Map.put), которая
			// идёт ПОСЛЕ конструирования сущности (Entity.load(...) вызывается отдельно от конструктора,
			// см. Entity.java:2090 vs Entity.java:327 EntityConstructing) — значит экземпляр, созданный
			// через add()/getData() в момент конструирования, может быть замещён в карте более новым
			// (восстановленным из диска) экземпляром ДО того, как этот успеет потикать. Без этой проверки
			// осиротевший (замещённый) экземпляр продолжал бы тикать со старыми (нулевыми) значениями,
			// а реальный (текущий) экземпляр — никогда. Самоочистка ниже — тот же приём, что уже был для
			// isRemoved() (не новая ветка управления, расширение существующей проверки).
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

	public static void add(LivingEntity aEntity) {
		if (aEntity == null || aEntity.level().isClientSide()) return;
		// F-attachment: было registerExtendedProperties("gt.props.food", new EntityFoodTracker(aEntity))
		// (1.7.10, безусловно НОВЫЙ объект) -> neo IAttachmentHolder.getData(AttachmentType<T>) (Entity
		// extends AttachmentHolder implements IAttachmentHolder, AttachmentHolder.java:74) — на СВЕЖЕЙ
		// (только что сконструированной) сущности карта аттачментов пуста, поэтому getData() тоже
		// безусловно уходит в defaultValueSupplier и строит новый EntityFoodTracker (см. TYPE выше);
		// getExistingDataOrNull() НЕ используется здесь намеренно (это create-точка, не read-точка).
		aEntity.getData(TYPE.get());
	}

	public static EntityFoodTracker get(Entity aEntity) {
		if (aEntity == null || aEntity.level().isClientSide()) return null;
		// было getExtendedProperties(String) (1.7.10, возвращал Object, требовал instanceof-проверку) ->
		// neo IAttachmentHolder.getExistingDataOrNull(AttachmentType<T>) (AttachmentHolder.java:87) — уже
		// статически типизирован под EntityFoodTracker (по самому ключу TYPE), instanceof-проверка снята
		// как ставшая невозможной (никакой другой тип под этим TYPE в принципе не хранится), и, что
		// критично, НЕ создаёт запись при отсутствии (get-or-null, не get-or-create — та же семантика,
		// что и раньше: null для сущностей, для которых add() не звался).
		return aEntity.getExistingDataOrNull(TYPE.get());
	}
}

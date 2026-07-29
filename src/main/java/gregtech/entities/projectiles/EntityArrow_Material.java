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

package gregtech.entities.projectiles;

import com.mojang.authlib.GameProfile;
import gregapi.item.IItemProjectile.EntityProjectile;
import gregtech.entities.EntitiesGT;
import net.minecraft.world.entity.EntityType;
import gregapi.oredict.OreDictItemData;
import gregapi.util.OM;
import gregapi.util.ST;
import gregapi.util.UT;
import gregapi.util.UT.Enchantments;
import gregapi.util.WD;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.block.Block;
import gregapi.block.Material;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayerFactory;

import java.util.List;
import java.util.UUID;

import static gregapi.data.CS.*;

public class EntityArrow_Material extends EntityProjectile {
	private int mHitBlockX = -1;
	private int mHitBlockY = -1;
	private int mHitBlockZ = -1;
	private Block mHitBlock = NB;
	private int mHitBlockMeta = 0;
	private boolean inGround = F;
	private int mTicksAlive = 0;
	private int ticksInAir = 0;
	// mKnockback — в общем предке EntityProjectile (F-arrow-enchants ЦЕНТР), там же, где его держал 1.7.10-предок.

	private ItemStack mArrow = null;

	// F12-entity: тип-ctor (EntityType,Level) = фабрика реестра (EntitiesGT.ARROW_MATERIAL, ссылка EntityArrow_Material::new);
	// public (как vanilla Arrow(EntityType,Level)) — доступна для method-reference из EntitiesGT. Позиционные/скоростные
	// type-ctor'ы protected — подкласс EntityArrow_Potion передаёт в них свой ARROW_POTION. Convenience-ctor'ы → ARROW_MATERIAL.
	public EntityArrow_Material(EntityType<? extends EntityArrow_Material> aType, Level aWorld) {
		super(aType, aWorld);
	}
	protected EntityArrow_Material(EntityType<? extends EntityArrow_Material> aType, Level aWorld, double aX, double aY, double aZ) {
		super(aType, aWorld, aX, aY, aZ);
	}
	protected EntityArrow_Material(EntityType<? extends EntityArrow_Material> aType, Level aWorld, LivingEntity aEntity, float aSpeed) {
		super(aType, aWorld, aEntity, aSpeed);
	}

	public EntityArrow_Material(Level aWorld) {
		this(EntitiesGT.ARROW_MATERIAL.get(), aWorld);
	}

	public EntityArrow_Material(Level aWorld, double aX, double aY, double aZ) {
		this(EntitiesGT.ARROW_MATERIAL.get(), aWorld, aX, aY, aZ);
	}

	public EntityArrow_Material(Level aWorld, LivingEntity aEntity, float aSpeed) {
		this(EntitiesGT.ARROW_MATERIAL.get(), aWorld, aEntity, aSpeed);
	}

	public EntityArrow_Material(Arrow aArrow, ItemStack aStack) {
		this(EntitiesGT.ARROW_MATERIAL.get(), aArrow.level());
		setOwner(aArrow.getOwner());
		// F-entity-nbt: neo save/load через ValueOutput/ValueInput (не CompoundTag) — мост TagValueOutput/TagValueInput (как ядро Behavior_CureZombie).
		net.minecraft.world.level.storage.TagValueOutput tOut = net.minecraft.world.level.storage.TagValueOutput.createWithContext(net.minecraft.util.ProblemReporter.DISCARDING, aArrow.registryAccess());
		aArrow.saveWithoutId(tOut);
		load(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING, registryAccess(), tOut.buildResult()));
		setProjectileStack(aStack);
	}

	@Override
	public void tick() {
		baseTick();
		if (mArrow == null && !level().isClientSide()) {
			discard();
			return;
		}

		Entity tShootingEntity = getOwner();

		if (xRotO == 0.0F && yRotO == 0.0F) {
			float f = (float)Math.sqrt(WD.motionX(this) * WD.motionX(this) + WD.motionZ(this) * WD.motionZ(this));
			yRotO = (float)(Math.atan2(WD.motionX(this), WD.motionZ(this)) * 180.0D / Math.PI); setYRot(yRotO);
			xRotO = (float)(Math.atan2(WD.motionY(this), f) * 180.0D / Math.PI); setXRot(xRotO);
		}

		if (mTicksAlive++ == 3000) discard();

		Block tBlock = WD.block(level(), mHitBlockX, mHitBlockY, mHitBlockZ);

		if (WD.getMaterial(tBlock) != Material.air) {
			VoxelShape tShape = tBlock.defaultBlockState().getCollisionShape(level(), new BlockPos(mHitBlockX, mHitBlockY, mHitBlockZ));
			if (!tShape.isEmpty() && tShape.bounds().move(mHitBlockX, mHitBlockY, mHitBlockZ).contains(getX(), getY(), getZ())) inGround = T;
		}

		if (shakeTime > 0) shakeTime--;

		if (inGround) {
			int j = WD.meta(level(), mHitBlockX, mHitBlockY, mHitBlockZ);
			if (tBlock != mHitBlock || j != mHitBlockMeta) {
				inGround = F;
				WD.setMotionX(this, WD.motionX(this) * (getRandom().nextFloat() * 0.2F));
				WD.setMotionY(this, WD.motionY(this) * (getRandom().nextFloat() * 0.2F));
				WD.setMotionZ(this, WD.motionZ(this) * (getRandom().nextFloat() * 0.2F));
				mTicksAlive = 0;
				ticksInAir = 0;
			}
		} else {
			ticksInAir++;
			Vec3 vec31 = new Vec3(getX(), getY(), getZ());
			Vec3 vec3 = new Vec3(getX() + WD.motionX(this), getY() + WD.motionY(this), getZ() + WD.motionZ(this));
			HitResult tVector = level().clip(new ClipContext(vec31, vec3, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
			vec31 = new Vec3(getX(), getY(), getZ());
			vec3 = new Vec3(getX() + WD.motionX(this), getY() + WD.motionY(this), getZ() + WD.motionZ(this));

			if (tVector != null && tVector.getType() != HitResult.Type.MISS) vec3 = new Vec3(tVector.getLocation().x, tVector.getLocation().y, tVector.getLocation().z);

			Entity tHitEntity = null;
			List<Entity> tAllPotentiallyHitEntities = level().getEntities(this, getBoundingBox().expandTowards(WD.motionX(this), WD.motionY(this), WD.motionZ(this)).inflate(1.0D, 1.0D, 1.0D));
			double tSmallestDistance = Double.MAX_VALUE;

			for (int i = 0; i < tAllPotentiallyHitEntities.size(); ++i) {
				Entity entity1 = tAllPotentiallyHitEntities.get(i);

				if (entity1.canBeCollidedWith(this) && (entity1 != tShootingEntity || ticksInAir >= 5)) {
					AABB axisalignedbb1 = entity1.getBoundingBox().inflate(0.3, 0.3, 0.3);
					java.util.Optional<Vec3> movingobjectposition1 = axisalignedbb1.clip(vec31, vec3);

					if (movingobjectposition1.isPresent()) {
						double tDistance = vec31.distanceTo(movingobjectposition1.get());

						if (tDistance < tSmallestDistance) {
							tHitEntity = entity1;
							tSmallestDistance = tDistance;
						}
					}
				}
			}

			if (tHitEntity != null) tVector = new EntityHitResult(tHitEntity);

			if (tVector != null && tHitEntity != null && tHitEntity instanceof Player) {
				if (((Player)tHitEntity).getAbilities().invulnerable || (tShootingEntity instanceof Player && !((Player)tShootingEntity).canHarmPlayer((Player)tHitEntity))) tVector = null;
			}

			if (tVector != null && tVector.getType() != HitResult.Type.MISS) {
				if (tHitEntity != null) {
					OreDictItemData tData = OM.anydata(mArrow);

					// To make Railcrafts Implosion Enchantment work...
					if (tShootingEntity instanceof Player) NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.entity.player.AttackEntityEvent((Player)tShootingEntity, tHitEntity));

					float
					tMagicDamage = tHitEntity instanceof LivingEntity?UT.Enchantments.getDamageBonusVsCreature(mArrow, tHitEntity):0,
					tDamage = UT.Code.roundUp((float)Math.sqrt(WD.motionX(this)*WD.motionX(this) + WD.motionY(this)*WD.motionY(this) + WD.motionZ(this)*WD.motionZ(this)) * (getBaseDamageGT() + Math.max(0, tData != null && tData.validMaterial() ? tData.mMaterial.mMaterial.mToolQuality-1 : 0)));

					if (isCritArrow()) tDamage += getRandom().nextInt((int)(tDamage / 2.0 + 2.0));

					int
					tImplosion  = UT.NBT.getEnchantmentLevelImplosion(mArrow),
					tFireDamage = (isOnFire()?5:0) + 4 * UT.NBT.getEnchantmentLevel(net.minecraft.world.item.enchantment.Enchantments.FIRE_ASPECT, mArrow),
					tKnockback  = mKnockback + UT.NBT.getEnchantmentLevel(net.minecraft.world.item.enchantment.Enchantments.KNOCKBACK, mArrow),
					tHitTimer   = -1;

					// Also work on Ghasts and such. But no double dipping on Anti Creeper Damage!
					if (tImplosion > 0 && UT.Entities.isExplosiveCreature(tHitEntity) && !Creeper.class.isInstance(tHitEntity)) tMagicDamage += 1.5F * tImplosion;

					int[] tDamages = onHitEntity(tHitEntity, tShootingEntity==null?this:tShootingEntity, mArrow==null?ST.make(Items.ARROW, 1, 0):mArrow, (int)(tDamage*2), (int)(tMagicDamage*2), tKnockback, tFireDamage, tHitTimer);

					if (tDamages != null) {
						tDamage      = tDamages[0] / 2.0F;
						tMagicDamage = tDamages[1] / 2.0F;
						tKnockback   = tDamages[2];
						tFireDamage  = tDamages[3];
						tHitTimer    = tDamages[4];

						if (tFireDamage > 0 && !(tHitEntity instanceof EnderMan)) tHitEntity.igniteForSeconds(tFireDamage);

						if (!(tHitEntity instanceof Player) && UT.NBT.getEnchantmentLevel(net.minecraft.world.item.enchantment.Enchantments.LOOTING, mArrow) > 0) {
							Player tPlayer = null;
							if (level() instanceof ServerLevel) tPlayer = FakePlayerFactory.get((ServerLevel)level(), new GameProfile(new UUID(0, 0), tShootingEntity instanceof LivingEntity?((LivingEntity)tShootingEntity).getName().getString():"Arrow"));
							if (tPlayer != null) {
								tPlayer.getInventory().setSelectedSlot(0);
								tPlayer.getInventory().setItem(0, getArrowItem());
								// Bypasses Twilight Forest Progression Checks. Yeah this is needed or else any Looting Arrow would do ZERO Damage.
								if (WD.dimTF(level())) tPlayer.getAbilities().instabuild = T;
								tShootingEntity = tPlayer;
								tPlayer.discard();
							}
						}

						// To make Looting work at all...
						DamageSource tDamageSource = damageSources().arrow(this, tShootingEntity==null?this:tShootingEntity);

						if (tDamage + tMagicDamage > 0 && tHitEntity.hurtOrSimulate(tDamageSource, (tDamage + tMagicDamage) * TFC_DAMAGE_MULTIPLIER)) {
							if (tHitEntity instanceof LivingEntity) {
								if (tHitTimer >= 0) tHitEntity.invulnerableTime = tHitTimer;

								if (tHitEntity instanceof Creeper && UT.NBT.getEnchantmentLevel(net.minecraft.world.item.enchantment.Enchantments.FIRE_ASPECT, mArrow) > 0 && tImplosion <= 0) ((Creeper)tHitEntity).ignite();

								LivingEntity tHitLivingEntity = (LivingEntity)tHitEntity;

								if (!level().isClientSide()) tHitLivingEntity.setArrowCount(tHitLivingEntity.getArrowCount() + 1);

								if (tKnockback > 0) {
									float tKnockbackDivider = (float)Math.sqrt(WD.motionX(this) * WD.motionX(this) + WD.motionZ(this) * WD.motionZ(this));
									if (tKnockbackDivider > 0.0F) tHitLivingEntity.push(WD.motionX(this) * tKnockback * 0.6000000238418579D / tKnockbackDivider, 0.1D, WD.motionZ(this) * tKnockback * 0.6000000238418579D / tKnockbackDivider);
								}

								Enchantments.applyBullshitA(tHitLivingEntity                                                                  , tShootingEntity==null?this:tShootingEntity, mArrow);
								Enchantments.applyBullshitB(tShootingEntity instanceof LivingEntity?(LivingEntity)tShootingEntity:null, tHitLivingEntity                          , mArrow);

								if (tShootingEntity != null && tHitLivingEntity != tShootingEntity && tHitLivingEntity instanceof Player && tShootingEntity instanceof ServerPlayer) {
									((ServerPlayer)tShootingEntity).connection.send(new net.minecraft.network.protocol.game.ClientboundGameEventPacket(net.minecraft.network.protocol.game.ClientboundGameEventPacket.PLAY_ARROW_HIT_SOUND, 0.0F));
								}
							}

							// F-enchant-crit-visual impossible-1:1: 1.7.10 Player.onEnchantmentCritical(entity) — клиент-визуал крит-энчант-частиц, удалён в neo без прямого аналога. GT6-урон сохранён 1:1, потерян лишь визуал.

							if (!(tHitEntity instanceof EnderMan) || ((EnderMan)tHitEntity).getEffect(MobEffects.WEAKNESS) != null) {
								if (tFireDamage > 0) tHitEntity.igniteForSeconds(tFireDamage);
								playSound(SoundEvents.ARROW_HIT, 1.0F, 1.2F / (getRandom().nextFloat() * 0.2F + 0.9F));
								discard();
							}
						} else {
							WD.setMotionX(this, WD.motionX(this) * -0.10000000149011612D);
							WD.setMotionY(this, WD.motionY(this) * -0.10000000149011612D);
							WD.setMotionZ(this, WD.motionZ(this) * -0.10000000149011612D);
							setYRot(getYRot() + 180.0F);
							yRotO += 180.0F;
							ticksInAir = 0;
						}
					}
				} else {
					BlockHitResult tBlockVector = (BlockHitResult)tVector;
					mHitBlockX = tBlockVector.getBlockPos().getX();
					mHitBlockY = tBlockVector.getBlockPos().getY();
					mHitBlockZ = tBlockVector.getBlockPos().getZ();
					mHitBlock = WD.block(level(), mHitBlockX, mHitBlockY, mHitBlockZ);
					mHitBlockMeta = WD.meta(level(), mHitBlockX, mHitBlockY, mHitBlockZ);
					WD.setMotionX(this, (float)(tVector.getLocation().x - getX()));
					WD.setMotionY(this, (float)(tVector.getLocation().y - getY()));
					WD.setMotionZ(this, (float)(tVector.getLocation().z - getZ()));
					float f2 = (float)Math.sqrt(WD.motionX(this) * WD.motionX(this) + WD.motionY(this) * WD.motionY(this) + WD.motionZ(this) * WD.motionZ(this));
					setPos(getX() - WD.motionX(this) / f2 * 0.05000000074505806D, getY() - WD.motionY(this) / f2 * 0.05000000074505806D, getZ() - WD.motionZ(this) / f2 * 0.05000000074505806D);
					playSound(SoundEvents.ARROW_HIT, 1.0F, 1.2F / (getRandom().nextFloat() * 0.2F + 0.9F));
					inGround = true;
					shakeTime = 7;
					setCritArrow(false);

					// F-block-entity-collide impossible-1:1: 1.7.10 Block.onEntityCollidedWithBlock(стрела попала в блок) — neo entityInside приватен и требует InsideBlockEffectApplier (авто-система collision); ручной 1:1-вызов недоступен, edge-case спец-блоков реагирующих на попадание стрелы.

					if (!level().isClientSide() && UT.NBT.getEnchantmentLevel(net.minecraft.world.item.enchantment.Enchantments.FIRE_ASPECT, mArrow) > 2) WD.burn(level(), mHitBlockX, mHitBlockY, mHitBlockZ, T, F);

					if (breaksOnImpact()) discard();
				}
			}

			if (isCritArrow()) for (int i = 0; i < 4; ++i) level().addParticle(ParticleTypes.CRIT, getX() + WD.motionX(this) * i / 4.0D, getY() + WD.motionY(this) * i / 4.0D, getZ() + WD.motionZ(this) * i / 4.0D, -WD.motionX(this), -WD.motionY(this) + 0.2D, -WD.motionZ(this));

			setPos(getX() + WD.motionX(this), getY() + WD.motionY(this), getZ() + WD.motionZ(this));

			setYRot((float)(Math.atan2(WD.motionX(this), WD.motionZ(this)) * 180.0D / Math.PI));

			setXRot((float)(Math.atan2(WD.motionY(this), (float)Math.sqrt(WD.motionX(this) * WD.motionX(this) + WD.motionZ(this) * WD.motionZ(this))) * 180.0D / Math.PI));
			while (getXRot() - xRotO  < -180.0F) xRotO -= 360.0F;
			while (getXRot() - xRotO >= 180.0F) xRotO += 360.0F;
			while (getYRot() - yRotO  < -180.0F) yRotO -= 360.0F;
			while (getYRot() - yRotO >= 180.0F) yRotO += 360.0F;

			setXRot(xRotO + (getXRot() - xRotO) * 0.2F);
			setYRot(yRotO + (getYRot() - yRotO) * 0.2F);
			float tFrictionMultiplier = 0.99F;

			if (isInWater()) {
				for (int l = 0; l < 4; ++l) level().addParticle(ParticleTypes.BUBBLE, getX() - WD.motionX(this) * 0.25, getY() - WD.motionY(this) * 0.25, getZ() - WD.motionZ(this) * 0.25, WD.motionX(this), WD.motionY(this), WD.motionZ(this));
				tFrictionMultiplier = 0.8F;
			}

			if (isInWaterOrRain()) extinguishFire();

			WD.setMotionX(this, WD.motionX(this) * tFrictionMultiplier);
			WD.setMotionY(this, WD.motionY(this) * tFrictionMultiplier);
			WD.setMotionZ(this, WD.motionZ(this) * tFrictionMultiplier - 0.05F);
			setPos(getX(), getY(), getZ());
			// F-entity: 1.7.10 func_145775_I() (doBlockCollisions) — neo делает block-collisions авто в move/tick, ручной вызов не нужен.
		}
	}

	@Override
	public void addAdditionalSaveData(net.minecraft.world.level.storage.ValueOutput aNBT) {
		super.addAdditionalSaveData(aNBT);
		aNBT.putShort("xTile", (short)mHitBlockX);
		aNBT.putShort("yTile", (short)mHitBlockY);
		aNBT.putShort("zTile", (short)mHitBlockZ);
		aNBT.putShort("life", (short)mTicksAlive);
		aNBT.putByte("inTile", (byte)net.minecraft.core.registries.BuiltInRegistries.BLOCK.getId(mHitBlock));
		aNBT.putByte("inData", (byte)mHitBlockMeta);
		aNBT.putByte("shake", (byte)shakeTime);
		aNBT.putByte("inGround", (byte)(inGround ? 1 : 0));
		aNBT.putByte("pickup", (byte)pickup.ordinal());
		aNBT.putDouble("damage", getBaseDamageGT());
		if (ST.valid(mArrow)) aNBT.store("mArrow", ItemStack.CODEC, mArrow);
	}

	@Override
	public void readAdditionalSaveData(net.minecraft.world.level.storage.ValueInput aNBT) {
		super.readAdditionalSaveData(aNBT);
		mHitBlockX = aNBT.getShortOr("xTile", (short)0);
		mHitBlockY = aNBT.getShortOr("yTile", (short)0);
		mHitBlockZ = aNBT.getShortOr("zTile", (short)0);
		mTicksAlive = aNBT.getShortOr("life", (short)0);
		mHitBlock = Block.stateById(aNBT.getByteOr("inTile", (byte)0) & 255).getBlock();
		mHitBlockMeta = aNBT.getByteOr("inData", (byte)0) & 255;
		shakeTime = aNBT.getByteOr("shake", (byte)0) & 255;
		inGround = aNBT.getByteOr("inGround", (byte)0) == 1;
		setBaseDamage(aNBT.getDoubleOr("damage", 0.0D));
		pickup = AbstractArrow.Pickup.byOrdinal(aNBT.getByteOr("pickup", (byte)0));
		mArrow = aNBT.read("mArrow", ItemStack.CODEC).orElse(null);
	}

	@Override
	public void playerTouch(Player aPlayer) {
		if (!level().isClientSide() && inGround && shakeTime <= 0 && pickup == AbstractArrow.Pickup.ALLOWED && aPlayer.getInventory().add(getArrowItem())) {
			playSound(SoundEvents.ITEM_PICKUP, 0.2F, ((getRandom().nextFloat() - getRandom().nextFloat()) * 0.7F + 1.0F) * 2.0F);
			aPlayer.take(this, 1);
			discard();
		}
	}

	/**
	 * @param aHitEntity the hit Entity
	 * @param aShootingEntity the shooting Entity
	 * @param aArrow the Arrow Item, might be a vanilla Arrow if the Client has not synched the Item.
	 * @param aRegularDamage Damage in Half Hearts
	 * @param aMagicDamage Magic Damage in Half Hearts
	 * @param aKnockback Knockback Level
	 * @param aFireDamage Fire Damage
	 * @return null if this is not damaging the Entity, otherwise see the return value below.
	 */
	public int[] onHitEntity(Entity aHitEntity, Entity aShootingEntity, ItemStack aArrow, int aRegularDamage, int aMagicDamage, int aKnockback, int aFireDamage, int aHitTimer) {
		return new int[] {aRegularDamage, aMagicDamage, aKnockback, aFireDamage, aHitTimer};
	}

	@Override
	public void setProjectileStack(ItemStack aStack) {
		mArrow = ST.update(ST.amount(1, aStack), this);
	}

	public ItemStack getArrowItem() {
		return ST.copy(mArrow);
	}

	public boolean breaksOnImpact() {
		return false;
	}

	// setKnockbackStrength(int) и getBaseDamageGT() — в общем предке EntityProjectile (F-arrow-enchants ЦЕНТР):
	// в 1.7.10 оба жили на движковом предке EntityArrow, а не в этом классе. Копии здесь сняты как дубль.
}

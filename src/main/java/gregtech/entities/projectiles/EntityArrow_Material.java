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
import gregapi.oredict.OreDictItemData;
import gregapi.util.OM;
import gregapi.util.ST;
import gregapi.util.UT;
import gregapi.util.UT.Enchantments;
import gregapi.util.WD;
import net.minecraft.world.level.block.Block;
import gregapi.block.Material;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundGameEventPacket;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.util.*;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.entity.player.AttackEntityEvent;

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
	private int mKnockback = 0;
	
	private ItemStack mArrow = null;
	
	public EntityArrow_Material(Level aWorld) {
		super(aWorld);
	}
	
	public EntityArrow_Material(Level aWorld, double aX, double aY, double aZ) {
		super(aWorld, aX, aY, aZ);
	}
	
	public EntityArrow_Material(Level aWorld, LivingEntity aEntity, float aSpeed) {
		super(aWorld, aEntity, aSpeed);
	}
	
	public EntityArrow_Material(Arrow aArrow, ItemStack aStack) {
		super(aArrow.level());
		shootingEntity = aArrow.shootingEntity;
		CompoundTag tNBT = UT.NBT.make();
		aArrow.writeToNBT(tNBT);
		readFromNBT(tNBT);
		setProjectileStack(aStack);
	}
	
	@Override
	public void onUpdate() {
		onEntityUpdate();
		if (mArrow == null && !level().isClientSide()) {
			setDead();
			return;
		}
		
		Entity tShootingEntity = shootingEntity;
		
		if (prevRotationPitch == 0.0F && prevRotationYaw == 0.0F) {
			float f = MathHelper.sqrt_double(motionX * motionX + motionZ * motionZ);
			prevRotationYaw = rotationYaw = (float)(Math.atan2(motionX, motionZ) * 180.0D / Math.PI);
			prevRotationPitch = rotationPitch = (float)(Math.atan2(motionY, f) * 180.0D / Math.PI);
		}
		
		if (mTicksAlive++ == 3000) setDead();
		
		Block tBlock = WD.block(level(), mHitBlockX, mHitBlockY, mHitBlockZ);
		
		if (WD.getMaterial(tBlock) != Material.air) {
			tBlock.setBlockBoundsBasedOnState(level(), mHitBlockX, mHitBlockY, mHitBlockZ);
			AxisAlignedBB axisalignedbb = tBlock.getCollisionBoundingBoxFromPool(level(), mHitBlockX, mHitBlockY, mHitBlockZ);
			if (axisalignedbb != null && axisalignedbb.isVecInside(Vec3.createVectorHelper(getX(), getY(), getZ()))) inGround = T;
		}
		
		if (arrowShake > 0) arrowShake--;
		
		if (inGround) {
			int j = WD.meta(level(), mHitBlockX, mHitBlockY, mHitBlockZ);
			if (tBlock != mHitBlock || j != mHitBlockMeta) {
				inGround = F;
				motionX *= (rand.nextFloat() * 0.2F);
				motionY *= (rand.nextFloat() * 0.2F);
				motionZ *= (rand.nextFloat() * 0.2F);
				mTicksAlive = 0;
				ticksInAir = 0;
			}
		} else {
			ticksInAir++;
			Vec3 vec31 = Vec3.createVectorHelper(getX(), getY(), getZ());
			Vec3 vec3 = Vec3.createVectorHelper(getX() + motionX, getY() + motionY, getZ() + motionZ);
			MovingObjectPosition tVector = level().func_147447_a(vec31, vec3, F, T, F);
			vec31 = Vec3.createVectorHelper(getX(), getY(), getZ());
			vec3 = Vec3.createVectorHelper(getX() + motionX, getY() + motionY, getZ() + motionZ);
			
			if (tVector != null) vec3 = Vec3.createVectorHelper(tVector.hitVec.x, tVector.hitVec.y, tVector.hitVec.z);
			
			Entity tHitEntity = null;
			@SuppressWarnings("rawtypes")
			List tAllPotentiallyHitEntities = level().getEntities(this, boundingBox.addCoord(motionX, motionY, motionZ).expand(1.0D, 1.0D, 1.0D));
			double tSmallestDistance = Double.MAX_VALUE;
			
			for (int i = 0; i < tAllPotentiallyHitEntities.size(); ++i) {
				Entity entity1 = (Entity)tAllPotentiallyHitEntities.get(i);
				
				if (entity1.canBeCollidedWith() && (entity1 != tShootingEntity || ticksInAir >= 5)) {
					AxisAlignedBB axisalignedbb1 = entity1.boundingBox.expand(0.3, 0.3, 0.3);
					MovingObjectPosition movingobjectposition1 = axisalignedbb1.calculateIntercept(vec31, vec3);
					
					if (movingobjectposition1 != null) {
						double tDistance = vec31.distanceTo(movingobjectposition1.hitVec);
						
						if (tDistance < tSmallestDistance) {
							tHitEntity = entity1;
							tSmallestDistance = tDistance;
						}
					}
				}
			}
			
			if (tHitEntity != null) tVector = new MovingObjectPosition(tHitEntity);
			
			if (tVector != null && tHitEntity != null && tHitEntity instanceof Player) {
				if (((Player)tHitEntity).capabilities.disableDamage || (tShootingEntity instanceof Player && !((Player)tShootingEntity).canAttackPlayer((Player)tHitEntity))) tVector = null;
			}
			
			if (tVector != null) {
				if (tHitEntity != null) {
					OreDictItemData tData = OM.anydata(mArrow);
					
					// To make Railcrafts Implosion Enchantment work...
					if (tShootingEntity instanceof Player) NeoForge.EVENT_BUS.post(new AttackEntityEvent((Player)tShootingEntity, tHitEntity));
					
					float
					tMagicDamage = tHitEntity instanceof LivingEntity?EnchantmentHelper.func_152377_a(mArrow, ((LivingEntity)tHitEntity).getCreatureAttribute()):0,
					tDamage = UT.Code.roundUp(MathHelper.sqrt_double(motionX*motionX + motionY*motionY + motionZ*motionZ) * (getDamage() + Math.max(0, tData != null && tData.validMaterial() ? tData.mMaterial.mMaterial.mToolQuality-1 : 0)));
					
					if (getIsCritical()) tDamage += rand.nextInt((int)(tDamage / 2.0 + 2.0));
					
					int
					tImplosion  = UT.NBT.getEnchantmentLevelImplosion(mArrow),
					tFireDamage = (isBurning()?5:0) + 4 * UT.NBT.getEnchantmentLevel(Enchantments.FIRE_ASPECT, mArrow),
					tKnockback  = mKnockback + UT.NBT.getEnchantmentLevel(Enchantments.KNOCKBACK, mArrow),
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
						
						if (tFireDamage > 0 && !(tHitEntity instanceof EnderMan)) tHitEntity.setFire(tFireDamage);
						
						if (!(tHitEntity instanceof Player) && UT.NBT.getEnchantmentLevel(Enchantments.LOOTING, mArrow) > 0) {
							Player tPlayer = null;
							if (level() instanceof ServerLevel) tPlayer = FakePlayerFactory.get((ServerLevel)level(), new GameProfile(new UUID(0, 0), tShootingEntity instanceof LivingEntity?((LivingEntity)tShootingEntity).getCommandSenderName():"Arrow"));
							if (tPlayer != null) {
								tPlayer.inventory.currentItem = 0;
								tPlayer.inventory.setInventorySlotContents(0, getArrowItem());
								// Bypasses Twilight Forest Progression Checks. Yeah this is needed or else any Looting Arrow would do ZERO Damage.
								if (WD.dimTF(level())) tPlayer.capabilities.isCreativeMode = T;
								tShootingEntity = tPlayer;
								tPlayer.setDead();
							}
						}
						
						// To make Looting work at all...
						DamageSource tDamageSource = DamageSource.causeArrowDamage(this, tShootingEntity==null?this:tShootingEntity);
						
						if (tDamage + tMagicDamage > 0 && tHitEntity.attackEntityFrom(tDamageSource, (tDamage + tMagicDamage) * TFC_DAMAGE_MULTIPLIER)) {
							if (tHitEntity instanceof LivingEntity) {
								if (tHitTimer >= 0) tHitEntity.hurtResistantTime = tHitTimer;
								
								if (tHitEntity instanceof Creeper && UT.NBT.getEnchantmentLevel(Enchantments.FIRE_ASPECT, mArrow) > 0 && tImplosion <= 0) ((Creeper)tHitEntity).func_146079_cb();
								
								LivingEntity tHitLivingEntity = (LivingEntity)tHitEntity;
								
								if (!level().isClientSide()) tHitLivingEntity.setArrowCountInEntity(tHitLivingEntity.getArrowCountInEntity() + 1);
								
								if (tKnockback > 0) {
									float tKnockbackDivider = MathHelper.sqrt_double(motionX * motionX + motionZ * motionZ);
									if (tKnockbackDivider > 0.0F) tHitLivingEntity.addVelocity(motionX * tKnockback * 0.6000000238418579D / tKnockbackDivider, 0.1D, motionZ * tKnockback * 0.6000000238418579D / tKnockbackDivider);
								}
								
								Enchantments.applyBullshitA(tHitLivingEntity                                                                  , tShootingEntity==null?this:tShootingEntity, mArrow);
								Enchantments.applyBullshitB(tShootingEntity instanceof LivingEntity?(LivingEntity)tShootingEntity:null, tHitLivingEntity                          , mArrow);
								
								if (tShootingEntity != null && tHitLivingEntity != tShootingEntity && tHitLivingEntity instanceof Player && tShootingEntity instanceof ServerPlayer) {
									((ServerPlayer)tShootingEntity).playerNetServerHandler.sendPacket(new ClientboundGameEventPacket(6, 0.0F));
								}
							}
							
							if (tShootingEntity instanceof Player && tMagicDamage > 0.0F) ((Player)tShootingEntity).onEnchantmentCritical(tHitEntity);
							
							if (!(tHitEntity instanceof EnderMan) || ((EnderMan)tHitEntity).getActivePotionEffect(MobEffect.weakness) != null) {
								if (tFireDamage > 0) tHitEntity.setFire(tFireDamage);
								playSound("random.bowhit", 1.0F, 1.2F / (rand.nextFloat() * 0.2F + 0.9F));
								setDead();
							}
						} else {
							motionX *= -0.10000000149011612D;
							motionY *= -0.10000000149011612D;
							motionZ *= -0.10000000149011612D;
							rotationYaw += 180.0F;
							prevRotationYaw += 180.0F;
							ticksInAir = 0;
						}
					}
				} else {
					mHitBlockX = tVector.getBlockPos().getX();
					mHitBlockY = tVector.getBlockPos().getY();
					mHitBlockZ = tVector.getBlockPos().getZ();
					mHitBlock = WD.block(level(), mHitBlockX, mHitBlockY, mHitBlockZ);
					mHitBlockMeta = WD.meta(level(), mHitBlockX, mHitBlockY, mHitBlockZ);
					motionX = ((float)(tVector.hitVec.x - getX()));
					motionY = ((float)(tVector.hitVec.y - getY()));
					motionZ = ((float)(tVector.hitVec.z - getZ()));
					float f2 = MathHelper.sqrt_double(motionX * motionX + motionY * motionY + motionZ * motionZ);
					posX -= motionX / f2 * 0.05000000074505806D;
					posY -= motionY / f2 * 0.05000000074505806D;
					posZ -= motionZ / f2 * 0.05000000074505806D;
					playSound("random.bowhit", 1.0F, 1.2F / (rand.nextFloat() * 0.2F + 0.9F));
					inGround = true;
					arrowShake = 7;
					setIsCritical(false);
					
					if (WD.getMaterial(mHitBlock) != Material.air) mHitBlock.onEntityCollidedWithBlock(level(), mHitBlockX, mHitBlockY, mHitBlockZ, this);
					
					if (!level().isClientSide() && UT.NBT.getEnchantmentLevel(Enchantments.FIRE_ASPECT, mArrow) > 2) WD.burn(level(), mHitBlockX, mHitBlockY, mHitBlockZ, T, F);
					
					if (breaksOnImpact()) setDead();
				}
			}
			
			if (getIsCritical()) for (int i = 0; i < 4; ++i) level().spawnParticle("crit", getX() + motionX * i / 4.0D, getY() + motionY * i / 4.0D, getZ() + motionZ * i / 4.0D, -motionX, -motionY + 0.2D, -motionZ);
			
			posX += motionX; posY += motionY; posZ += motionZ;
			
			rotationYaw = (float)(Math.atan2(motionX, motionZ) * 180.0D / Math.PI);
			
			for (rotationPitch = (float)(Math.atan2(motionY, MathHelper.sqrt_double(motionX * motionX + motionZ * motionZ)) * 180.0D / Math.PI); rotationPitch - prevRotationPitch < -180.0F; prevRotationPitch -= 360.0F) {/**/}
			
			while (rotationPitch    - prevRotationPitch >= 180.0F) prevRotationPitch += 360.0F;
			while (rotationYaw      - prevRotationYaw   < -180.0F) prevRotationYaw -= 360.0F;
			while (rotationYaw      - prevRotationYaw   >= 180.0F) prevRotationYaw += 360.0F;
			
			rotationPitch = prevRotationPitch + (rotationPitch - prevRotationPitch) * 0.2F;
			rotationYaw = prevRotationYaw + (rotationYaw - prevRotationYaw) * 0.2F;
			float tFrictionMultiplier = 0.99F;
			
			if (isInWater()) {
				for (int l = 0; l < 4; ++l) level().spawnParticle("bubble", getX() - motionX * 0.25, getY() - motionY * 0.25, getZ() - motionZ * 0.25, motionX, motionY, motionZ);
				tFrictionMultiplier = 0.8F;
			}
			
			if (isWet()) extinguish();
			
			motionX *= tFrictionMultiplier;
			motionY *= tFrictionMultiplier;
			motionZ *= tFrictionMultiplier;
			motionY -= 0.05F;
			setPosition(getX(), getY(), getZ());
			func_145775_I();
		}
	}
	
	@Override
	public void writeEntityToNBT(CompoundTag aNBT) {
		super.writeEntityToNBT(aNBT);
		aNBT.putShort("xTile", (short)mHitBlockX);
		aNBT.putShort("yTile", (short)mHitBlockY);
		aNBT.putShort("zTile", (short)mHitBlockZ);
		aNBT.putShort("life", (short)mTicksAlive);
		aNBT.putByte("inTile", (byte)Block.getIdFromBlock(mHitBlock));
		aNBT.putByte("inData", (byte)mHitBlockMeta);
		aNBT.putByte("shake", (byte)arrowShake);
		aNBT.putByte("inGround", (byte)(inGround ? 1 : 0));
		aNBT.putByte("pickup", (byte)canBePickedUp);
		aNBT.putDouble("damage", getDamage());
		aNBT.put("mArrow", ST.save(mArrow));
	}
	
	@Override
	public void readEntityFromNBT(CompoundTag aNBT) {
		super.readEntityFromNBT(aNBT);
		mHitBlockX = aNBT.getShort("xTile");
		mHitBlockY = aNBT.getShort("yTile");
		mHitBlockZ = aNBT.getShort("zTile");
		mTicksAlive = aNBT.getShort("life");
		mHitBlock = Block.getBlockById(aNBT.getByte("inTile") & 255);
		mHitBlockMeta = aNBT.getByte("inData") & 255;
		arrowShake = aNBT.getByte("shake") & 255;
		inGround = aNBT.getByte("inGround") == 1;
		setDamage(aNBT.getDouble("damage"));
		canBePickedUp = aNBT.getByte("pickup");
		mArrow = ST.load(aNBT, "mArrow");
	}
	
	@Override
	public void onCollideWithPlayer(Player aPlayer) {
		if (!level().isClientSide() && inGround && arrowShake <= 0 && canBePickedUp == 1 && aPlayer.inventory.addItemStackToInventory(getArrowItem())) {
			playSound("random.pop", 0.2F, ((rand.nextFloat() - rand.nextFloat()) * 0.7F + 1.0F) * 2.0F);
			aPlayer.onItemPickup(this, 1);
			setDead();
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
	
	@Override
	public void setKnockbackStrength(int aKnockback) {
		mKnockback = aKnockback;
	}
}

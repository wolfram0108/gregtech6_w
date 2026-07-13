/**
 * Copyright (c) 2025 GregTech-6 Team
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

package gregtech.items.behaviors;
import net.minecraft.world.level.block.PumpkinBlock;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.Block;

import com.mojang.authlib.GameProfile;
import gregapi.block.misc.BlockBaseBars;
import gregapi.block.misc.BlockBaseSpike;
import gregapi.code.ArrayListNoNulls;
import gregapi.code.TagData;
import gregapi.damage.DamageSources;
import gregapi.data.*;
import gregapi.enchants.Enchantment_EnderDamage;
import gregapi.item.IItemProjectile;
import gregapi.item.multiitem.MultiItem;
import gregapi.item.multiitem.MultiItemTool;
import gregapi.item.multiitem.behaviors.IBehavior.AbstractBehaviorDefault;
import gregapi.oredict.OreDictItemData;
import gregapi.oredict.OreDictMaterial;
import gregapi.oredict.OreDictMaterialStack;
import gregapi.util.OM;
import gregapi.util.ST;
import gregapi.util.UT;
import gregapi.util.WD;
import gregtech.tileentity.misc.MultiTileEntityGregOLantern;
import net.minecraft.block.*;
import gregapi.block.Material;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragonPart;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundGameEventPacket;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.phys.AABB;
import net.minecraft.core.BlockPos;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import twilightforest.entity.boss.EntityTFLich;

import java.util.List;
import java.util.UUID;

import static gregapi.data.CS.*;

public class Behavior_Gun extends AbstractBehaviorDefault {
	public static Behavior_Gun BULLETS_SMALL  = new Behavior_Gun(TD.Projectiles.BULLET_SMALL , 1.00F, 10000, 16);
	public static Behavior_Gun BULLETS_MEDIUM = new Behavior_Gun(TD.Projectiles.BULLET_MEDIUM, 2.00F, 17500,  8);
	public static Behavior_Gun BULLETS_LARGE  = new Behavior_Gun(TD.Projectiles.BULLET_LARGE , 3.00F, 25000,  4);
	
	public final TagData mBulletType;
	public final long mPower;
	public final float mMagic;
	public final byte mAmmoPerMag;
	
	public Behavior_Gun(TagData aBulletType, float aMagic, long aPower, long aAmmoPerMag) {
		mBulletType = aBulletType;
		mMagic = aMagic;
		mPower = aPower;
		mAmmoPerMag = UT.Code.bindStack(aAmmoPerMag);
	}
	
	public boolean shoot(ItemStack aGun, ItemStack aBullet, Player aPlayer) {
		// Making sure all Data is correct.
		aGun    = ST.update(aGun   , aPlayer);
		aBullet = ST.update(aBullet, aPlayer);
		// What's the Angle we are looking from and to?
		Vec3
		tDir = aPlayer.getLookAngle(),
		tPos = Vec3.createVectorHelper(aPlayer.getX(), aPlayer.getY() + aPlayer.getEyeHeight(), aPlayer.getZ()),
		tAim = tPos.addVector(tDir.x * 200, tDir.y * 200, tDir.z * 200);
		// List all the Blocks that are on the way.
		List<BlockPos> aCoords = WD.line(tPos, tAim);
		// Gather random Information about the first Block.
		BlockPos oCoord = aCoords.get(0), aCoord = oCoord, nCoord = oCoord;
		Block oBlock = NB, aBlock = oBlock = WD.block(aPlayer.level(), aCoord.getX(), aCoord.getY(), aCoord.getZ());
		byte  oMeta  =  0, aMeta  = oMeta  = WD.meta (aPlayer.level(), aCoord.getX(), aCoord.getY(), aCoord.getZ());
		// Are we shooting from under Water?
		boolean tWater = WD.liquid(aPlayer.level(), aCoord.getX(), aCoord.getY(), aCoord.getZ());
		// Bullet related Stats
		int tFireAspect = UT.NBT.getEnchantmentLevel(Enchantments.FLAME, aGun) + UT.NBT.getEnchantmentLevel(Enchantments.FIRE_ASPECT, aBullet);
		
		// Make a List of all possible Targets.
		List tEntities = aPlayer.level().getEntities(aPlayer, AABB.getBoundingBox(Math.min(tPos.x, tAim.x)-2, Math.min(tPos.y, tAim.y)-2, Math.min(tPos.z, tAim.z)-2, Math.max(tPos.x, tAim.x)+2, Math.max(tPos.y, tAim.y)+2, Math.max(tPos.z, tAim.z)+2));
		List<Entity> tTargets = new ArrayListNoNulls<>();
		for (Object tEntity : tEntities) if (tEntity instanceof Entity) {
			AABB tBox = ((Entity)tEntity).getBoundingBox();
			if (tBox != null) {
				if (tEntity instanceof EndCrystal) tBox = tBox.getOffsetBoundingBox(0, 1.3, 0);
				if (tBox.calculateIntercept(tPos, tAim) != null) tTargets.add((Entity)tEntity);
			}
		}
		
		// Actually do the shooting now!
		long tPower = mPower + 2000L*UT.NBT.getEnchantmentLevel(Enchantments.POWER, aGun);
		for (int i = 1, ii = aCoords.size()-1; i < ii; i++) {
			
			if (tPower<=0) {
				// TODO Maybe drop the Round as an Item at ***oCoord***.
				if (tFireAspect > 2) WD.burn(aPlayer.level(), oCoord, T, T);
				return T;
			}
			
			oCoord = aCoords.get(i-1);
			aCoord = aCoords.get(i  );
			nCoord = aCoords.get(i+1);
			
			oBlock = aBlock;
			oMeta  = aMeta;
			
			aBlock = WD.block(aPlayer.level(), aCoord.getX(), aCoord.getY(), aCoord.getZ());
			aMeta  = WD.meta (aPlayer.level(), aCoord.getX(), aCoord.getY(), aCoord.getZ());
			
			
			for (int j = 0; j < tTargets.size(); j++) {
				if (tTargets.get(j).distanceToSqr(aCoord.getX()+0.5, aCoord.getY()+0.5, aCoord.getZ()+0.5) < tTargets.get(j).distanceToSqr(nCoord.getX()+0.5, nCoord.getY()+0.5, nCoord.getZ()+0.5)) {
					if (hit(aGun, aBullet, aPlayer, tTargets.remove(j--), tPower, tDir)) {
						tPower-=10000;
						// If the bullet hits an Entity it should not possibly drop itself.
						if (tPower<=0) return T;
					}
				}
			}
			
			if (WD.liquid(aPlayer.level(), aCoord.getX(), aCoord.getY(), aCoord.getZ())) {
				if (!tWater) {
					tWater = T;
					UT.Sounds.send(SFX.MC_LIQUID_SPLASH, aPlayer.level(), aCoord);
					// if high velocity break entirely, otherwise half the remaining power.
					if (tPower>10000) tPower=0; else tPower/=2;
				}
				continue;
			}
			
			tWater = F;
			
			if (aBlock instanceof PumpkinBlock || WD.te(aPlayer.level(), aCoord, T) instanceof MultiTileEntityGregOLantern) {
				if (RNGSUS.nextInt(3) == 0) {
					ST.drop(aPlayer.level(), aCoord.getX()+0.2+RNGSUS.nextFloat()*0.6, aCoord.getY()+0.1+RNGSUS.nextFloat()*0.5, aCoord.getZ()+0.2+RNGSUS.nextFloat()*0.6, ST.make(Blocks.PUMPKIN, 1, 0));
				} else {
					ST.drop(aPlayer.level(), aCoord.getX()+0.2+RNGSUS.nextFloat()*0.6, aCoord.getY()+0.1+RNGSUS.nextFloat()*0.5, aCoord.getZ()+0.2+RNGSUS.nextFloat()*0.6, ST.make(Items.PUMPKIN_SEEDS, 1+RNGSUS.nextInt(3), 0));
				}
				WD.set(aPlayer.level(), aCoord.getX(), aCoord.getY(), aCoord.getZ(), NB, 0, 3);
				if (tFireAspect > 1) WD.fire(aPlayer.level(), aCoord, F);
				UT.Sounds.send(aBlock.defaultBlockState().getSoundType().getBreakSound(), aPlayer.level(), aCoord);
				tPower-=3000;
				continue;
			}
			if (aBlock == Blocks.MELON) {
				ST.drop(aPlayer.level(), aCoord.getX()+0.2+RNGSUS.nextFloat()*0.6, aCoord.getY()+0.1+RNGSUS.nextFloat()*0.5, aCoord.getZ()+0.2+RNGSUS.nextFloat()*0.6, ST.make(Items.MELON_SLICE      , 1+RNGSUS.nextInt(6), 0));
				ST.drop(aPlayer.level(), aCoord.getX()+0.2+RNGSUS.nextFloat()*0.6, aCoord.getY()+0.1+RNGSUS.nextFloat()*0.5, aCoord.getZ()+0.2+RNGSUS.nextFloat()*0.6, ST.make(Items.MELON_SEEDS, 1+RNGSUS.nextInt(3), 0));
				WD.set(aPlayer.level(), aCoord.getX(), aCoord.getY(), aCoord.getZ(), NB, 0, 3);
				if (tFireAspect > 1) WD.fire(aPlayer.level(), aCoord, F);
				UT.Sounds.send(aBlock.defaultBlockState().getSoundType().getBreakSound(), aPlayer.level(), aCoord);
				tPower-=3000;
				continue;
			}
			if (aBlock == Blocks.CACTUS) {
				ST.drop(aPlayer.level(), aCoord.getX()+0.2+RNGSUS.nextFloat()*0.6, aCoord.getY()+0.1+RNGSUS.nextFloat()*0.5, aCoord.getZ()+0.2+RNGSUS.nextFloat()*0.6, ST.make(Blocks.CACTUS, 1, 0));
				WD.set(aPlayer.level(), aCoord.getX(), aCoord.getY(), aCoord.getZ(), NB, 0, 3);
				if (tFireAspect > 1) WD.fire(aPlayer.level(), aCoord, F);
				UT.Sounds.send(aBlock.defaultBlockState().getSoundType().getBreakSound(), aPlayer.level(), aCoord);
				tPower-=3000;
				continue;
			}
			if (aBlock == Blocks.COCOA) {
				ST.drop(aPlayer.level(), aCoord.getX()+0.2+RNGSUS.nextFloat()*0.6, aCoord.getY()+0.1+RNGSUS.nextFloat()*0.5, aCoord.getZ()+0.2+RNGSUS.nextFloat()*0.6, IL.Dye_Cocoa.get(1));
				WD.set(aPlayer.level(), aCoord.getX(), aCoord.getY(), aCoord.getZ(), NB, 0, 3);
				if (tFireAspect > 1) WD.fire(aPlayer.level(), aCoord, F);
				UT.Sounds.send(aBlock.defaultBlockState().getSoundType().getBreakSound(), aPlayer.level(), aCoord);
				tPower-=2000;
				continue;
			}
			if (aBlock == Blocks.WHITE_WOOL || WD.getMaterial(aBlock) == Material.carpet) {
				if (tFireAspect > 1) {
					WD.set(aPlayer.level(), aCoord.getX(), aCoord.getY(), aCoord.getZ(), NB, 0, 3);
					WD.fire(aPlayer.level(), aCoord, F);
				}
				UT.Sounds.send(aBlock.defaultBlockState().getSoundType().getBreakSound(), aPlayer.level(), aCoord);
				tPower-=4000;
				continue;
			}
			if (WD.getMaterial(aBlock) == Material.glass || aBlock == Blocks.ICE || aBlock == Blocks.REDSTONE_LAMP || aBlock == Blocks.REDSTONE_LAMP) {
				OreDictItemData tData = OM.anydata(ST.make(aBlock, 1, aMeta));
				for (OreDictMaterialStack tMaterial : tData.getAllMaterialStacks()) {
					long tAmount = tMaterial.mAmount / OP.scrapGt.mAmount;
					while (tAmount-->0) {
						ST.drop(aPlayer.level(), aCoord.getX()+0.2+RNGSUS.nextFloat()*0.6, aCoord.getY()+0.1+RNGSUS.nextFloat()*0.5, aCoord.getZ()+0.2+RNGSUS.nextFloat()*0.6, OP.scrapGt.mat(tMaterial.mMaterial, 1));
					}
				}
				WD.set(aPlayer.level(), aCoord.getX(), aCoord.getY(), aCoord.getZ(), NB, 0, 3);
				UT.Sounds.send(aBlock.defaultBlockState().getSoundType().getBreakSound(), aPlayer.level(), aCoord);
				tPower-=2000;
				continue;
			}
			if (aBlock instanceof FenceBlock || aBlock instanceof FenceGateBlock || aBlock == Blocks.COBWEB || aBlock == Blocks.SPAWNER || aBlock instanceof IronBarsBlock || aBlock instanceof BaseRailBlock || aBlock instanceof TorchBlock || aBlock instanceof BlockBaseBars || aBlock instanceof BlockBaseSpike || WD.getMaterial(aBlock) == Material.cactus || WD.getMaterial(aBlock) == Material.fire || WD.getMaterial(aBlock) == Material.air || WD.getMaterial(aBlock) == Material.cloth || WD.getMaterial(aBlock) == Material.leaves || WD.getMaterial(aBlock) == Material.plants || WD.getMaterial(aBlock) == Material.vine) {
				// Just ignore or assume the Player shot through them.
				tPower-=200;
				continue;
			}
			if (aBlock instanceof StairBlock || WD.opq(aPlayer.level(), aCoord.getX(), aCoord.getY(), aCoord.getZ(), T, F)) {
				UT.Sounds.send(aBlock.defaultBlockState().getSoundType().getBreakSound(), aPlayer.level(), aCoord);
				tPower=0;
				continue;
			}
			if (aBlock.canCollideCheck(aMeta, F) || aBlock.canCollideCheck(aMeta, T)) {
				AABB tBox = aBlock.getCollisionBoundingBoxFromPool(aPlayer.level(), aCoord.getX(), aCoord.getY(), aCoord.getZ());
				if (tBox != null && tBox.calculateIntercept(tPos, tAim) != null) {
					UT.Sounds.send(aBlock.defaultBlockState().getSoundType().getBreakSound(), aPlayer.level(), aCoord);
					tPower=0;
					continue;
				}
			}
			// Well, just keep flying.
			tPower-=200;
		}
		return F;
	}
	
	public boolean hit(ItemStack aGun, ItemStack aBullet, Player aPlayer, Entity aTarget, long aPower, Vec3 aDir) {
		try {
		// In case the Entity is Invulnerable.
		if (aTarget.isEntityInvulnerable()) return F;
		// Player specific immunities, and I guess friendly fire prevention too.
		if (aTarget instanceof Player && (((Player)aTarget).getAbilities().invulnerable || !aPlayer.canAttackPlayer((Player)aTarget))) return F;
		// Endermen require Disjunction Enchantment on the Bullet, or having a Weakness Potion Effect on them.
		if (aTarget instanceof EnderMan && ((EnderMan)aTarget).getActivePotionEffect(MobEffect.weakness) == null && UT.NBT.getEnchantmentLevel(Enchantment_EnderDamage.INSTANCE, aBullet) <= 0) for (int i = 0; i < 64; ++i) if (((EnderMan)aTarget).teleportRandomly()) return F;
		// EntityLivingBase, Ender Dragon and End Crystals only.
		if (!(aTarget instanceof LivingEntity || aTarget instanceof EnderDragonPart || aTarget instanceof EndCrystal)) return F;
	//  // To make Railcrafts Damage Enchantments work... // I later figured I'd just hardcode it in.
	//  MinecraftForge.EVENT_BUS.post(new AttackEntityEvent(aPlayer, aTarget));
		
		OreDictItemData tData = OM.anydata(aBullet);
		OreDictMaterial tGunMat = MultiItemTool.getPrimaryMaterial(aGun, MT.Steel);
		
		float
		tMassFactor = (tData!=null&&tData.nonemptyMaterial() ? (float)tData.mMaterial.weight() / 50.0F : 1),
		tSpeedFactor = Math.min(2.0F, aPower/5000.0F),
		tMagicDamage = (aTarget instanceof LivingEntity ? EnchantmentHelper.func_152377_a(aBullet, ((LivingEntity)aTarget).getCreatureAttribute()) : aTarget instanceof EnderDragonPart ? UT.NBT.getEnchantmentLevel(Enchantment_EnderDamage.INSTANCE, aBullet) : 0),
		tDamage = tSpeedFactor * Math.max(0, tGunMat.mToolQuality*0.5F + tMassFactor);
		int
		tImplosion  =      UT.NBT.getEnchantmentLevelImplosion(aBullet),
		tFireDamage = 4 * (UT.NBT.getEnchantmentLevel(Enchantments.FLAME, aGun) + UT.NBT.getEnchantmentLevel(Enchantments.FIRE_ASPECT, aBullet)),
		tKnockback  =     (UT.NBT.getEnchantmentLevel(Enchantments.PUNCH, aGun) + UT.NBT.getEnchantmentLevel(Enchantments.KNOCKBACK , aBullet));
		
		if (tImplosion  > 0 && UT.Entities.isExplosiveCreature(aTarget)) tMagicDamage += 1.5F*tImplosion;
		if (tFireDamage > 0) aTarget.setFire(tFireDamage);
		
		Player tPlayer = aPlayer;
		
		if (aTarget instanceof Player) {
			// Guns are quite overkill against Players otherwise.
			tDamage /= 2; tMagicDamage /= 2;
		} else {
			// Bigger Bullets deal more Magic Damage just like they already do for Normal Damage, but not against Players.
			// The Reason I didn't just up the Enchantment Level like I did with Looting is because that would increase the Side Effects too.
			tMagicDamage *= mMagic;
			
			if (aPlayer.level() instanceof ServerLevel) {
				if (UT.NBT.getEnchantmentLevel(Enchantments.LOOTING, aBullet) > 0) {
					tPlayer = FakePlayerFactory.get((ServerLevel)aPlayer.level(), new GameProfile(new UUID(0, 0), ((LivingEntity)aPlayer).getCommandSenderName()));
					tPlayer.getInventory().currentItem = 0;
					tPlayer.getInventory().setInventorySlotContents(0, aBullet);
					tPlayer.setPositionAndRotation(aPlayer.getX(), aPlayer.getY(), aPlayer.getZ(), aPlayer.rotationYaw, aPlayer.getXRot());
					// Bypasses Twilight Forest Progression Checks. Yeah this is needed or else any Looting Bullet would do ZERO Damage.
					if (WD.dimTF(aPlayer.level())) tPlayer.getAbilities().instabuild = T;
					tPlayer.setDead();
				}
			}
		}
		
		// To make Looting work at all...
		DamageSource tDamageSource = DamageSources.getCombatDamage("player", tPlayer, DamageSources.getDeathMessage(aPlayer, aTarget, (tData!=null&&tData.validMaterial() ? "[VICTIM] got killed by [KILLER] shooting a Bullet made of " + tData.mMaterial.mMaterial.getLocal() : "[VICTIM] got shot by [KILLER]")), F).setProjectile();
		// Extremely Fast Bullets will penetrate Armor. You need a Rifle with the Power Enchantment for this. A Power 5 Carbine at point-blank could do too though.
		if (aPower > 25000) tDamageSource.setDamageBypassesArmor();
		// Smite Bullets will break one Lich Shield each, in order to make this somewhat beatable in Multiplayer.
		if (MD.TF.mLoaded && aTarget instanceof EntityTFLich && UT.NBT.getEnchantmentLevel(Enchantments.SMITE, aBullet) > 0) tDamageSource.setDamageBypassesArmor();
		
		if (aTarget.hurtOrSimulate(tDamageSource, (tDamage + tMagicDamage) * TFC_DAMAGE_MULTIPLIER)) {
			aTarget.invulnerableTime = (aTarget instanceof LivingEntity ? ((LivingEntity)aTarget).maxHurtResistantTime : 20);
			if (aTarget instanceof Creeper && tFireDamage > 0 && tImplosion <= 0) ((Creeper)aTarget).func_146079_cb();
			if (tKnockback > 0) aTarget.addVelocity(aDir.x * tKnockback * aPower / 50000.0, 0.05, aDir.z * tKnockback * aPower / 50000.0);
			if (aTarget instanceof LivingEntity)
			UT.Enchantments.applyBullshitA((LivingEntity)aTarget, aPlayer, aBullet);
			UT.Enchantments.applyBullshitB(                  aPlayer, aTarget, aBullet);
			if (aTarget instanceof Player && aPlayer instanceof ServerPlayer) ((ServerPlayer)aPlayer).playerNetServerHandler.sendPacket(new ClientboundGameEventPacket(6, 0.0F));
			if (tMagicDamage > 0.0F) aPlayer.onEnchantmentCritical(aTarget);
			return T;
		}
		// Print Errors to the Log and send a Chat Message informing about its existence.
		} catch(Throwable e) {e.printStackTrace(ERR); UT.Entities.sendchat(aPlayer, "See gregtech.log for details: " + e.toString()); aTarget.setDead(); return T;}
		// Just pretend we miss the Target if it was in its Invulnerability Frames, this will end up hitting whatever is behind the Target instead.
		if (aTarget.invulnerableTime > 0) return F;
		// It hits, but it doesn't seem to do anything.
		return T;
	}
	
//  @Override public boolean onRightClickEntity(MultiItem aItem, ItemStack aStack, EntityPlayer aPlayer, Entity aEntity) {onItemRightClick(aItem, aStack, aPlayer.worldObj, aPlayer); return T;}
	@Override public boolean onItemUse         (MultiItem aItem, ItemStack aStack, Player aPlayer, Level aWorld, int aX, int aY, int aZ, byte aSide, float hitX, float hitY, float hitZ) {onItemRightClick(aItem, aStack, aPlayer.level(), aPlayer); return T;}
	@Override public boolean onItemUseFirst    (MultiItem aItem, ItemStack aStack, Player aPlayer, Level aWorld, int aX, int aY, int aZ, byte aSide, float hitX, float hitY, float hitZ) {if (aWorld.isClientSide()) return F; onItemRightClick(aItem, aStack, aPlayer.level(), aPlayer); return T;}
	
	@Override
	public ItemStack onItemRightClick(MultiItem aItem, ItemStack aGun, Level aWorld, Player aPlayer) {
		// TODO Particles!
		if (!(aPlayer instanceof ServerPlayer)) return aGun;
		
		CompoundTag aNBT = UT.NBT.getOrCreate(aGun);
		ItemStack aBullet = ST.load(aNBT, NBT_AMMO);
		if (aPlayer.isShiftKeyDown()) {
			if (ST.invalid(aBullet) || aBullet.getCount() <= 0) {
				reloadGun(aGun, aPlayer, F);
				return aGun;
			}
			ST.give(aPlayer, aBullet);
			UT.Sounds.send(SFX.MC_CLICK, 16, aPlayer);
			ST.save(aNBT, NBT_AMMO, NI);
			UT.NBT.set(aGun, aNBT); // F8: detached-копия из getOrCreate — коммитим мутацию (см. ItemNBT.java)
			return aGun;
		}
		if (ST.invalid(aBullet) || aBullet.getCount() <= 0) {
			UT.Sounds.send(SFX.MC_CLICK, 16, aPlayer);
			ST.save(aNBT, NBT_AMMO, NI);
			UT.NBT.set(aGun, aNBT); // F8: detached-копия из getOrCreate — коммитим мутацию (см. ItemNBT.java)
			return aGun;
		}
		shoot(aGun, ST.amount(1, aBullet), aPlayer);
		UT.Sounds.send(SFX.MC_FIREWORK_BLAST_FAR, 128, aPlayer);
		if (!UT.Entities.hasInfiniteItems(aPlayer) && RNGSUS.nextInt(1+UT.NBT.getEnchantmentLevel(Enchantments.INFINITY, aGun)) == 0) {
			OreDictItemData tData = OM.anydata(aBullet);
			aBullet.setCount(aBullet.getCount()-1);
			ST.save(aNBT, NBT_AMMO, aBullet.getCount() > 0 ? aBullet : NI);
			UT.NBT.set(aGun, aNBT); // F8: коммит detached-копии ДО doDamage, иначе расход патрона потеряется (см. ItemNBT.java)
			for (OreDictMaterialStack tMat : tData.mByProducts) if (tMat.mAmount >= OP.scrapGt.mAmount && !tMat.mMaterial.containsAny(TD.Properties.EXPLOSIVE, TD.Properties.FLAMMABLE)) ST.give(aPlayer, OP.scrapGt.mat(tMat.mMaterial, tMat.mAmount/OP.scrapGt.mAmount));
		}
		((MultiItemTool)aItem).doDamage(aGun, 100, aPlayer, F);
		return aGun;
	}
	
	@Override
	public List<String> getAdditionalToolTips(MultiItem aItem, List<String> aList, ItemStack aStack) {
		aList.add(LH.Chat.CYAN + LH.get(LH.WEAPON_SNEAK_RIGHTCLICK_TO_RELOAD));
		ItemStack aBullet = ST.load(UT.NBT.getNBT(aStack), NBT_AMMO);
		if (ST.valid(aBullet)) aList.add(LH.Chat.YELLOW + aBullet.getDisplayName() + LH.Chat._WHITE + aBullet.getCount());
		return aList;
	}
	
	public boolean isProjectile(ItemStack aStack) {
		return ST.item(aStack) instanceof IItemProjectile && ((IItemProjectile)ST.item(aStack)).hasProjectile(mBulletType, aStack);
	}
	
	public boolean reloadGun(ItemStack aGun, Player aPlayer, boolean aOnlyCheckHeld) {
		CompoundTag aNBT = UT.NBT.getOrCreate(aGun);
		ItemStack aBullet = ST.load(aNBT, NBT_AMMO);
		if (ST.valid(aBullet) && aBullet.getCount() > 0) return F;
		if (isProjectile(aPlayer.getInventory().getItem(aPlayer.getInventory().currentItem))) {
			int tConsumed = Math.min(mAmmoPerMag, aPlayer.getInventory().getItem(aPlayer.getInventory().currentItem).getCount());
			UT.Sounds.send(SFX.MC_CLICK, 16, aPlayer);
			ST.save(aNBT, NBT_AMMO, ST.amount(tConsumed, aPlayer.getInventory().getItem(aPlayer.getInventory().currentItem)));
			UT.NBT.set(aGun, aNBT); // F8: коммит detached-копии из getOrCreate (см. ItemNBT.java)
			aPlayer.getInventory().decrStackSize(aPlayer.getInventory().currentItem, tConsumed);
			ST.update(aPlayer);
			return T;
		}
		if (aOnlyCheckHeld) return F;
		for (int i = 0; i < Inventory.INVENTORY_SIZE; i++) if (aPlayer.getInventory().getItem(i) == aGun) {
			if (i < 27 && isProjectile(aPlayer.getInventory().getItem(i+27))) {
			if (i < 18 && isProjectile(aPlayer.getInventory().getItem(i+18))) {
			if (i <  9 && isProjectile(aPlayer.getInventory().getItem(i+ 9))) {
				int tConsumed = Math.min(mAmmoPerMag, aPlayer.getInventory().getItem(i+ 9).getCount());
				UT.Sounds.send(SFX.MC_CLICK, 16, aPlayer);
				ST.save(aNBT, NBT_AMMO, ST.amount(tConsumed, aPlayer.getInventory().getItem(i+ 9)));
				UT.NBT.set(aGun, aNBT); // F8: коммит detached-копии из getOrCreate (см. ItemNBT.java)
				aPlayer.getInventory().decrStackSize(i+ 9, tConsumed);
				ST.update(aPlayer);
				return T;
			}
				int tConsumed = Math.min(mAmmoPerMag, aPlayer.getInventory().getItem(i+18).getCount());
				UT.Sounds.send(SFX.MC_CLICK, 16, aPlayer);
				ST.save(aNBT, NBT_AMMO, ST.amount(tConsumed, aPlayer.getInventory().getItem(i+18)));
				UT.NBT.set(aGun, aNBT); // F8: коммит detached-копии из getOrCreate (см. ItemNBT.java)
				aPlayer.getInventory().decrStackSize(i+18, tConsumed);
				ST.update(aPlayer);
				return T;
			}
				int tConsumed = Math.min(mAmmoPerMag, aPlayer.getInventory().getItem(i+27).getCount());
				UT.Sounds.send(SFX.MC_CLICK, 16, aPlayer);
				ST.save(aNBT, NBT_AMMO, ST.amount(tConsumed, aPlayer.getInventory().getItem(i+27)));
				UT.NBT.set(aGun, aNBT); // F8: коммит detached-копии из getOrCreate (см. ItemNBT.java)
				aPlayer.getInventory().decrStackSize(i+27, tConsumed);
				ST.update(aPlayer);
				return T;
			}
			break;
		}
		for (int i = Inventory.INVENTORY_SIZE-1; i >= 0; i--) if (isProjectile(aPlayer.getInventory().getItem(i))) {
			int tConsumed = Math.min(mAmmoPerMag, aPlayer.getInventory().getItem(i).getCount());
			UT.Sounds.send(SFX.MC_CLICK, 16, aPlayer);
			ST.save(aNBT, NBT_AMMO, ST.amount(tConsumed, aPlayer.getInventory().getItem(i)));
			aPlayer.getInventory().decrStackSize(i, tConsumed);
			ST.update(aPlayer);
			return T;
		}
		return F;
	}
}

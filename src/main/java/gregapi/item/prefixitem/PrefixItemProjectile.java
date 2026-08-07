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

package gregapi.item.prefixitem;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.enchantment.Enchantments;

import net.neoforged.fml.Logging;
import gregapi.code.ModData;
import gregapi.code.ObjectStack;
import gregapi.code.TagData;
import gregapi.data.LH;
import gregapi.data.MT;
import gregapi.item.IItemProjectile;
import gregapi.oredict.OreDictMaterial;
import gregapi.oredict.OreDictPrefix;
import gregapi.util.ST;
import gregapi.util.UT;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.Position;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

import java.util.List;

import static gregapi.data.CS.*;

/**
 * @author Gregorius Techneticies
 */
public class PrefixItemProjectile extends PrefixItem implements IItemProjectile {
	public final TagData mProjectileType;
	public final Class<? extends EntityProjectile> mEntityClass;
	public final float mSpeedMultiplier, mPrecision;
	public final boolean mStabbing, mIsBullet;
	
	public PrefixItemProjectile(ModData aMod, String aNameInternal, OreDictPrefix aPrefix, TagData aProjectileType, Class<? extends EntityProjectile> aEntityClass, float aSpeedMultiplier, float aPrecision, boolean aDispensable, boolean aStabbing, boolean aIsBullet, OreDictMaterial... aMaterialList) {
		this(aMod.mID, aMod.mID, aNameInternal, aPrefix, aProjectileType, aEntityClass, aSpeedMultiplier, aPrecision, aDispensable, aStabbing, aIsBullet, aMaterialList);
	}
	
	public PrefixItemProjectile(String aModIDOwner, String aModIDTextures, String aNameInternal, OreDictPrefix aPrefix, TagData aProjectileType, Class<? extends EntityProjectile> aEntityClass, float aSpeedMultiplier, float aPrecision, boolean aDispensable, boolean aStabbing, boolean aIsBullet, OreDictMaterial... aMaterialList) {
		super(aModIDOwner, aModIDTextures, aNameInternal, aPrefix, aMaterialList);
		mProjectileType = aProjectileType;
		mEntityClass = aEntityClass;
		mPrecision = aPrecision;
		mSpeedMultiplier = aSpeedMultiplier;
		mStabbing = aStabbing;
		mIsBullet = aIsBullet;
		if (aDispensable) DispenserBlock.registerBehavior(this, new MetaItemDispense()); // было dispenseBehaviorRegistry.putObject (DispenserBlock.java:61)
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public void addInformation(ItemStack aStack, Player aPlayer, List aList, boolean aF3_H) {
		if (mIsBullet) {
			OreDictMaterial tMat = getMaterial(ST.meta(aStack));
			int tDamage = (int)((tMat == null ? 1.0 : tMat.getWeight(getPrefix(ST.meta(aStack)).mAmount) / 50.0) * 2.0F * TFC_DAMAGE_MULTIPLIER)+1;
			aList.add(LH.Chat.WHITE + "Bullet Damage: " + LH.Chat.RED + tDamage/2.0F + (TFC_DAMAGE_MULTIPLIER>1?"":" Hearts"));
		}
		super.addInformation(aStack, aPlayer, aList, aF3_H);
	}
	
	@Override
	public boolean hasProjectile(TagData aProjectileType, ItemStack aStack) {
		return (mProjectileType == aProjectileType || aProjectileType == null) && UT.Code.exists(ST.meta_(aStack), mMaterialList) && mMaterialList[ST.meta_(aStack)] != MT.Empty;
	}
	
	@Override
	public EntityProjectile getProjectile(TagData aProjectileType, ItemStack aStack, Level aWorld, double aX, double aY, double aZ) {
		if (!hasProjectile(aProjectileType, aStack)) return null;
		try {
			EntityProjectile tProjectile = mEntityClass.getConstructor(Level.class, Double.TYPE, Double.TYPE, Double.TYPE).newInstance(aWorld, aX, aY, aZ);
			tProjectile.setProjectileStack(ST.amount(1, aStack));
			return tProjectile;
		} catch (Throwable e) {ERR.println(String.format("Problems with '%s'", mEntityClass.getName())); ERR.println(e.toString());}
		return null;
	}
	
	@Override
	public EntityProjectile getProjectile(TagData aProjectileType, ItemStack aStack, Level aWorld, LivingEntity aEntity, float aSpeed) {
		if (!hasProjectile(aProjectileType, aStack)) return null;
		try {
			EntityProjectile tProjectile = mEntityClass.getConstructor(Level.class, LivingEntity.class, Float.TYPE).newInstance(aWorld, aEntity, mSpeedMultiplier * aSpeed);
			tProjectile.setProjectileStack(ST.amount(1, aStack));
			return tProjectile;
		} catch (Throwable e) {ERR.println(String.format("Problems with '%s'", mEntityClass.getName())); ERR.println(e.toString());}
		return null;
	}
	
	@Override
	public boolean onLeftClickEntity(ItemStack aStack, Player aPlayer, Entity aEntity) {
		super.onLeftClickEntity(aStack, aPlayer, aEntity);
		if (aEntity instanceof LivingEntity) {
			if (mStabbing) {
				UT.Enchantments.applyBullshitA((LivingEntity)aEntity, aPlayer, aStack);
				UT.Enchantments.applyBullshitB(aPlayer, aEntity, aStack);
			}
			ST.use(aPlayer, aStack);
			return F;
		}
		return F;
	}
	
	public int mLootingMultiplier = 1;
	
	public PrefixItemProjectile setLootingMultiplier(int aLootingMultiplier) {
		mLootingMultiplier = aLootingMultiplier;
		return this;
	}
	
	@Override
	public void updateItemStack(ItemStack aStack) {
		super.updateItemStack(aStack);
		short aMetaData = ST.meta_(aStack);
		if (UT.Code.exists(aMetaData, mMaterialList) && !mMaterialList[aMetaData].mEnchantmentAmmo.isEmpty()) {
			CompoundTag tNBT = UT.NBT.getOrCreate(aStack);
			if (!tNBT.getBoolean("gt.u").orElse(false)) {
				tNBT.putBoolean("gt.u", T);
				// F8: getOrCreate → detached-копия; коммитим флаг "gt.u" ДО addEnchantment, иначе он не
				// долетит до стека и энчанты будут добавляться повторно (см. ItemNBT.java, паттерн Behavior_Arrow).
				UT.NBT.set(aStack, tNBT);
				for (ObjectStack<ResourceKey<Enchantment>> tEnchantment : mMaterialList[aMetaData].mEnchantmentAmmo) {
					UT.NBT.addEnchantment(aStack, tEnchantment.mObject, tEnchantment.mObject == Enchantments.LOOTING ? tEnchantment.mAmount * mLootingMultiplier : tEnchantment.mAmount);
				}
			}
		}
	}
	
	public ItemStack onDispense(BlockSource aSource, ItemStack aStack) {
		Level aWorld = aSource.level();
		Position tPosition = DispenserBlock.getDispensePosition(aSource);
		Direction tFacing = aSource.state().getValue(DispenserBlock.FACING); // было func_149937_b(getBlockMetadata()) -> BlockSource.state()/DispenserBlock.FACING, приём ItemArmorBase.java:187
		EntityProjectile tProjectile = getProjectile(mProjectileType, aStack, aWorld, tPosition.x(), tPosition.y(), tPosition.z()); // было Position.getX/getY/getZ() -> x()/y()/z() (Position.java:4-8)
		if (tProjectile != null) {
			tProjectile.shoot(tFacing.getStepX(), (tFacing.getStepY() + 0.1F), tFacing.getStepZ(), mSpeedMultiplier * 1.10F, mPrecision); // было setThrowableHeading(...)/Direction.getFrontOffsetX|Y|Z() -> Projectile.shoot(double,double,double,float,float) (Projectile.java:141), Direction.getStepX|Y|Z() (Direction.java:247-255)
			tProjectile.setProjectileStack(ST.amount(1, aStack));
			tProjectile.pickup = AbstractArrow.Pickup.ALLOWED; // было canBePickedUp=1 (int tri-state) -> AbstractArrow.Pickup enum, ALLOWED==ordinal 1 (AbstractArrow.java:72,746-749, LEGACY_CODEC подтверждает byOrdinal-соответствие)
			aWorld.addFreshEntity(tProjectile);
			if (aStack.getCount() < 100) aStack.setCount(aStack.getCount()-1);
			return aStack;
		}

		// Default Item Dropping.
		Direction enumfacing = aSource.state().getValue(DispenserBlock.FACING);
		Position iposition = DispenserBlock.getDispensePosition(aSource);
		ItemStack itemstack1 = aStack.split(1);
		DefaultDispenseItemBehavior.spawnItem(aSource.level(), itemstack1, 6, enumfacing, iposition); // было doDispense -> spawnItem (DefaultDispenseItemBehavior.java:30)
		return aStack;
	}

	/** F13 (документация neo item/armor-компонентной модели): было {@code extends BehaviorProjectileDispense} с
	 *  {@code getProjectileEntity(...)→null} (1.7.10, dead code) — neo {@code ProjectileDispenseBehavior} требует
	 *  реального {@code ProjectileItem} в конструкторе (ProjectileDispenseBehavior.java:16), этот Item им не
	 *  является — сведено к {@code DefaultDispenseItemBehavior}, приём уже принят {@code ItemArmorBase.java:203-208}. */
	public static class MetaItemDispense extends DefaultDispenseItemBehavior {
		@Override
		protected ItemStack execute(BlockSource aSource, ItemStack aStack) {return ((PrefixItemProjectile)aStack.getItem()).onDispense(aSource, aStack);}
	}
}

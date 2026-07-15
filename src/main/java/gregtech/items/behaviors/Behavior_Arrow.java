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

package gregtech.items.behaviors;

import static gregapi.data.CS.*;

import gregapi.code.TagData;
import gregapi.data.TD;
import gregapi.item.IItemProjectile.EntityProjectile;
import gregapi.item.multiitem.MultiItem;
import gregapi.item.multiitem.behaviors.IBehavior.AbstractBehaviorDefault;
import gregapi.util.UT;
import gregapi.util.UT.Enchantments;
import gregtech.entities.projectiles.EntityArrow_Material;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.Position;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

public class Behavior_Arrow extends AbstractBehaviorDefault {
	public static Behavior_Arrow DEFAULT_WOODEN  = new Behavior_Arrow(EntityArrow_Material.class, 1.00F, 6.0F);
	public static Behavior_Arrow DEFAULT_PLASTIC = new Behavior_Arrow(EntityArrow_Material.class, 1.50F, 6.0F);
	
	private final int mLevel;
	private final Enchantment mEnchantment;
	private final float mSpeedMultiplier, mPrecision;
	private final Class<? extends EntityArrow_Material> mArrow;
	
	public Behavior_Arrow(Class<? extends EntityArrow_Material> aArrow, float aSpeed, float aPrecision) {
		this(aArrow, aSpeed, aPrecision, null, 0);
	}
	
	public Behavior_Arrow(Class<? extends EntityArrow_Material> aArrow, float aSpeed, float aPrecision, Enchantment aEnchantment, int aLevel) {
		mArrow = aArrow;
		mSpeedMultiplier = aSpeed;
		mPrecision = aPrecision;
		mEnchantment = aEnchantment;
		mLevel = aLevel;
	}
	
	@Override
	public boolean onLeftClickEntity(MultiItem aItem, ItemStack aStack, Player aPlayer, Entity aEntity) {
		if (aEntity instanceof LivingEntity) {
			Enchantments.applyBullshitA((LivingEntity)aEntity, aPlayer, aStack);
			Enchantments.applyBullshitB(aPlayer, aEntity, aStack);
			if (!UT.Entities.hasInfiniteItems(aPlayer)) aStack.setCount(aStack.getCount()-1);
			if (aStack.getCount() <= 0) aPlayer.destroyCurrentEquippedItem();
			return F;
		}
		return F;
	}
	
	@Override
	public boolean isItemStackUsable(MultiItem aItem, ItemStack aStack) {
		if (mEnchantment != null && mLevel > 0) {
			CompoundTag tNBT = UT.NBT.getNBT(aStack);
			if (!tNBT.getBooleanOr("gt.u", false)) {
				tNBT.putBoolean("gt.u", T);
				UT.NBT.set(aStack, tNBT);
				UT.NBT.addEnchantment(aStack, mEnchantment, mLevel);
			}
		}
		return T;
	}
	
	@Override
	public boolean canDispense(MultiItem aItem, BlockSource aSource, ItemStack aStack) {
		return T;
	}
	
	@Override
	public ItemStack onDispense(MultiItem aItem, BlockSource aSource, ItemStack aStack) {
		Level aWorld = aSource.level();
		Position tPosition = DispenserBlock.getDispensePosition(aSource);
		Direction tFacing = aSource.state().getValue(net.minecraft.world.level.block.DispenserBlock.FACING);
		EntityProjectile tEntityArrow = getProjectile(aItem, TD.Projectiles.ARROW, aStack, aWorld, tPosition.getX(), tPosition.getY(), tPosition.getZ());
		if (tEntityArrow != null) {
			tEntityArrow.shoot(tFacing.getStepX(), (tFacing.getStepY() + 0.1F), tFacing.getStepZ(), mSpeedMultiplier * 1.10F, mPrecision);
			tEntityArrow.setProjectileStack(aStack);
			tEntityArrow.canBePickedUp = 1;
			aWorld.addFreshEntity(tEntityArrow);
			if (aStack.getCount() < 100) aStack.setCount(aStack.getCount()-1);
			return aStack;
		}
		return super.onDispense(aItem, aSource, aStack);
	}
	
	@Override
	public boolean hasProjectile(MultiItem aItem, TagData aProjectileType, ItemStack aStack) {
		return aProjectileType == TD.Projectiles.ARROW;
	}
	
	@Override
	public EntityProjectile getProjectile(MultiItem aItem, TagData aProjectileType, ItemStack aStack, Level aWorld, double aX, double aY, double aZ) {
		if (!hasProjectile(aItem, aProjectileType, aStack)) return null;
		EntityArrow_Material rArrow = (EntityArrow_Material)UT.Reflection.callConstructor(mArrow.getName(), -1, null, T, aWorld, aX, aY, aZ);
		rArrow.setProjectileStack(aStack);
		return rArrow;
	}
	
	@Override
	public EntityProjectile getProjectile(MultiItem aItem, TagData aProjectileType, ItemStack aStack, Level aWorld, LivingEntity aEntity, float aSpeed) {
		if (!hasProjectile(aItem, aProjectileType, aStack)) return null;
		EntityArrow_Material rArrow = (EntityArrow_Material)UT.Reflection.callConstructor(mArrow.getName(), -1, null, T, aWorld, aEntity, mSpeedMultiplier * aSpeed);
		rArrow.setProjectileStack(aStack);
		return rArrow;
	}
}

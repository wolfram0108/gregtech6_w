/**
 * Copyright (c) 2021 GregTech-6 Team
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

package gregapi.item.multiitem.energy;
import net.minecraft.world.inventory.AbstractContainerMenu;

import static gregapi.data.CS.*;

import java.util.Collection;

import gregapi.code.HashSetNoNulls;
import gregapi.code.ItemNBT;
import gregapi.code.TagData;
import gregapi.data.TD;
import gregapi.item.IItemEnergy;
import gregapi.util.ST;
import gregapi.util.UT;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;

public class EnergyStat implements IItemEnergy {
	public long mSize, mAmountIN, mAmountOUT, mCapacity;
	public boolean mCanCharge, mCanDecharge;
	public TagData mType;
	public ItemStack mEmptyItem, mHalfItem, mFullItem;
	
	public EnergyStat(TagData aEnergyType, long aCapacity, long aSize, long aAmountIN, long aAmountOUT, boolean aCanCharge, boolean aCanDecharge, ItemStack aEmptyItem, ItemStack aHalfItem, ItemStack aFullItem) {
		mSize = aSize;
		mAmountIN = Math.max(1, aAmountIN);
		mAmountOUT = Math.max(1, aAmountOUT);
		mCapacity = aCapacity;
		mType = aEnergyType;
		mCanCharge = aCanCharge;
		mCanDecharge = aCanDecharge;
		mEmptyItem = aEmptyItem;
		mHalfItem = aHalfItem;
		mFullItem = aFullItem;
	}
	
	public static IItemEnergy makeREBattery(TagData aEnergyType, long aCapacity, long aSize, long aAmountIN, long aAmountOUT, ItemStack aEmptyItem, ItemStack aHalfItem, ItemStack aFullItem) {return new EnergyStat(aEnergyType, aCapacity, aSize, aAmountIN, aAmountOUT, T, T, aEmptyItem, aHalfItem, aFullItem);}
	public static IItemEnergy makeREBattery(TagData aEnergyType, long aCapacity, long aSize, long aAmountIN, long aAmountOUT, ItemStack aItem) {return new EnergyStat(aEnergyType, aCapacity, aSize, aAmountIN, aAmountOUT, T, T, aItem, aItem, aItem);}
	public static IItemEnergy makeREBattery(TagData aEnergyType, long aCapacity, long aSize, long aAmount, ItemStack aEmptyItem, ItemStack aHalfItem, ItemStack aFullItem) {return new EnergyStat(aEnergyType, aCapacity, aSize, aAmount, aAmount, T, T, aEmptyItem, aHalfItem, aFullItem);}
	public static IItemEnergy makeREBattery(TagData aEnergyType, long aCapacity, long aSize, long aAmount, ItemStack aItem) {return new EnergyStat(aEnergyType, aCapacity, aSize, aAmount, aAmount, T, T, aItem, aItem, aItem);}
	public static IItemEnergy makeSUBattery(TagData aEnergyType, long aCapacity, long aSize, long aAmount, ItemStack aEmptyItem, ItemStack aHalfItem, ItemStack aFullItem) {return new EnergyStat(aEnergyType, aCapacity, aSize, aAmount, aAmount, F, T, aEmptyItem, aHalfItem, aFullItem);}
	public static IItemEnergy makeSUBattery(TagData aEnergyType, long aCapacity, long aSize, long aAmount, ItemStack aItem) {return new EnergyStat(aEnergyType, aCapacity, aSize, aAmount, aAmount, F, T, aItem, aItem, aItem);}
	public static IItemEnergy makeTool     (TagData aEnergyType, long aCapacity, long aSize, long aAmount, ItemStack aEmptyItem, ItemStack aHalfItem, ItemStack aFullItem) {return new EnergyStat(aEnergyType, aCapacity, aSize, aAmount, aAmount, T, F, aEmptyItem, aHalfItem, aFullItem);}
	public static IItemEnergy makeTool     (TagData aEnergyType, long aCapacity, long aSize, long aAmount, ItemStack aItem) {return new EnergyStat(aEnergyType, aCapacity, aSize, aAmount, aAmount, T, F, aItem, aItem, aItem);}
	
	@Override
	public long doEnergyInjection(TagData aEnergyType, ItemStack aStack, long aSize, long aAmount, AbstractContainerMenu aInventory, Level aWorld, int aX, int aY, int aZ, boolean aDoInject) {
		if (aAmount < 1 || mAmountIN < 1) return 0;
		if (!canEnergyInjection(aEnergyType, aStack, aSize = Math.abs(aSize))) return 0;
		long tStored = getEnergyStored(mType, aStack);
		if (tStored >= mCapacity) return 0;
		long rAmount = Math.min(mAmountIN, aAmount);
		while (rAmount > 1 && tStored + rAmount * aSize > mCapacity) rAmount--;
		if (aDoInject) setEnergyStored(mType, aStack, tStored + rAmount * aSize);
		return rAmount;
	}
	
	@Override
	public long doEnergyExtraction(TagData aEnergyType, ItemStack aStack, long aSize, long aAmount, AbstractContainerMenu aInventory, Level aWorld, int aX, int aY, int aZ, boolean aDoExtract) {
		if (aAmount < 1 || mAmountOUT < 1) return 0;
		if (!canEnergyExtraction(aEnergyType, aStack, aSize = Math.abs(aSize))) return 0;
		long tStored = getEnergyStored(mType, aStack);
		if (tStored < aSize) return 0;
		long rAmount = Math.min(mAmountOUT, aAmount);
		while (rAmount > 1 && tStored - rAmount * aSize < 0) rAmount--;
		if (aDoExtract) setEnergyStored(mType, aStack, tStored - rAmount * aSize);
		return rAmount;
	}
	
	public ItemStack rechargeFromPlayer(TagData aEnergyType, ItemStack aStack, LivingEntity aPlayer, AbstractContainerMenu aInventory, Level aWorld, int aX, int aY, int aZ) {
		if (COMPAT_EU_ITEM == null || !mCanCharge || aPlayer == null || aPlayer.level().isClientSide() || aEnergyType != mType || aEnergyType != TD.Energy.EU) return aStack;
		long tMinInput = getEnergySizeInputMin(aEnergyType, aStack), tCapacity = getEnergyCapacity(aEnergyType, aStack);
		boolean temp = F;
		try {for (int i = 1; i < 5; i++) {
			long tContent = getEnergyStored(aEnergyType, aStack);
			if (tContent >= tCapacity) return aStack;
			ItemStack tArmor = UT.Entities.getEquipmentInSlot(aPlayer, i);
			if (tArmor == aStack || ST.invalid(tArmor) || !COMPAT_EU_ITEM.is(tArmor) || VMAX[COMPAT_EU_ITEM.tier(tArmor)] < tMinInput || !COMPAT_EU_ITEM.provider(tArmor)) continue;
			setEnergyStored(aEnergyType, aStack, tContent+COMPAT_EU_ITEM.decharge(tArmor, tCapacity-tContent, T));
			temp = T;
		}} catch(Throwable e) {e.printStackTrace(ERR);}
		if (temp) ST.update(aPlayer);
		return aStack;
	}
	
	@Override
	public boolean useEnergy(TagData aEnergyType, ItemStack aStack, long aEnergyAmount, LivingEntity aPlayer, AbstractContainerMenu aInventory, Level aWorld, int aX, int aY, int aZ, boolean aDoUse) {
		if (aPlayer instanceof Player && ((Player)aPlayer).getAbilities().instabuild) return T;
		if (aEnergyType != mType && aEnergyType != null) return F;
		rechargeFromPlayer(mType, aStack, aPlayer, aInventory, aWorld, aX, aY, aZ);
		long tStored = getEnergyStored(mType, aStack);
		if (tStored >= aEnergyAmount) {
			if (aDoUse) {
				setEnergyStored(mType, aStack, tStored-aEnergyAmount);
				rechargeFromPlayer(mType, aStack, aPlayer, aInventory, aWorld, aX, aY, aZ);
			}
			return T;
		}
		if (aDoUse) {
			setEnergyStored(mType, aStack, 0);
			rechargeFromPlayer(mType, aStack, aPlayer, aInventory, aWorld, aX, aY, aZ);
		}
		return F;
	}
	
	@Override
	public ItemStack setEnergyStored(TagData aEnergyType, ItemStack aStack, long aAmount) {
		if ((aEnergyType != mType && aEnergyType != null) || ST.size(aStack) <= 0) return aStack;
		
		CompoundTag tNBT = ItemNBT.get(aStack);
		if (tNBT == null) tNBT = UT.NBT.make(); else tNBT.remove(NBT_ENERGY);
		
		if (aAmount > 0) {
			if (aAmount >= mCapacity) {
				if (mFullItem != null) ST.set(aStack, mFullItem, F, F);
			} else {
				if (mHalfItem != null) ST.set(aStack, mHalfItem, F, F);
			}
			UT.NBT.setNumber(tNBT, NBT_ENERGY, aAmount);
		} else {
			if (mEmptyItem == null) {
				aStack.setCount(aStack.getCount()-1);
			} else {
				ST.set(aStack, mEmptyItem, F, F);
			}
		}
		UT.NBT.set(aStack, tNBT);
		return ST.update_(aStack);
	}

	@Override
	public long getEnergyStored(TagData aEnergyType, ItemStack aStack) {
		if (aEnergyType != mType && aEnergyType != null) return 0;
		CompoundTag tNBT = ItemNBT.get(aStack);
		return tNBT==null?0:tNBT.getLong(NBT_ENERGY).orElse(0L);
	}
	
	@Override public long getEnergyCapacity             (TagData aEnergyType, ItemStack aStack) {return aEnergyType == mType || aEnergyType == null ? mCapacity : 0;}
	@Override public long getEnergySizeInputMin         (TagData aEnergyType, ItemStack aStack) {return aEnergyType == mType || aEnergyType == null ? mSize <= 8 ? 1 : mSize / 2 : 0;}
	@Override public long getEnergySizeOutputMin        (TagData aEnergyType, ItemStack aStack) {return aEnergyType == mType || aEnergyType == null ? mSize <= 8 ? 1 : mSize / 2 : 0;}
	@Override public long getEnergySizeInputRecommended (TagData aEnergyType, ItemStack aStack) {return aEnergyType == mType || aEnergyType == null ? mSize : 0;}
	@Override public long getEnergySizeOutputRecommended(TagData aEnergyType, ItemStack aStack) {return aEnergyType == mType || aEnergyType == null ? mSize : 0;}
	@Override public long getEnergySizeInputMax         (TagData aEnergyType, ItemStack aStack) {return aEnergyType == mType || aEnergyType == null ? mSize * 2 : 0;}
	@Override public long getEnergySizeOutputMax        (TagData aEnergyType, ItemStack aStack) {return aEnergyType == mType || aEnergyType == null ? mSize * 2 : 0;}
	@Override public Collection<TagData> getEnergyTypes(ItemStack aStack) {return new HashSetNoNulls<>(F, mType);}
	@Override public boolean isEnergyType(TagData aEnergyType, ItemStack aStack, boolean aEmitting) {return (aEnergyType == mType || aEnergyType == null) && (aEmitting ? mCanDecharge : mCanCharge);}
	@Override public boolean canEnergyInjection (TagData aEnergyType, ItemStack aStack, long aSize) {return mCanCharge   && (aEnergyType == mType || aEnergyType == null) && aStack.getCount() == 1 && aSize <= getEnergySizeInputMax (aEnergyType, aStack) && aSize >= getEnergySizeInputMin (aEnergyType, aStack);}
	@Override public boolean canEnergyExtraction(TagData aEnergyType, ItemStack aStack, long aSize) {return mCanDecharge && (aEnergyType == mType || aEnergyType == null) && aStack.getCount() == 1 && aSize <= getEnergySizeOutputMax(aEnergyType, aStack) && aSize >= getEnergySizeOutputMin(aEnergyType, aStack);}
}

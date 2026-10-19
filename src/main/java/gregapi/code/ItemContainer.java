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
 *
 * Modified in 2026 for the GregTech 6 NeoForge port
 * (https://github.com/wolfram0108/gregtech6_w): ported from Minecraft 1.7.10 / Forge
 * to Minecraft 26.1.2 / NeoForge; moved out of gregapi/data/IL.java so that addons keep their own entries alike.
 */

package gregapi.code;

import gregapi.api.Abstract_Mod;
import gregapi.item.IItemEnergy;
import gregapi.oredict.OreDictItemData;
import gregapi.util.OM;
import gregapi.util.ST;
import gregapi.util.UT;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;

import static gregapi.data.CS.*;

/**
 * @author Gregorius Techneticies
 * 
 * One non-OreDict Item, as an Entry of IL holds it. Addons use it for their own Entries.
 */
public class ItemContainer implements IItemContainer {
	public final String mName;
	
	public ItemContainer(String aName) {mName = aName;}
	
	private ItemStack mStack;
	private boolean mHasNotBeenSet = T;
	// An ItemStack cannot be built in preInit, and the item itself cannot be constructed until the registry
	// opens; a Supplier is stored instead and the stack is materialized lazily once components are bound.
	private java.util.function.Supplier<Item> mItemSupplier;
	/** Takes a Supplier (DeferredItem) instead of the item itself, so construction and ST.make are deferred until needed. */
	public IItemContainer set(java.util.function.Supplier<Item> aItemSupplier) {mHasNotBeenSet = F; mItemSupplier = aItemSupplier; return this;}
	/** Materializes mStack from the deferred supplier on first access, once components are bound at runtime. */
	private void ensureMaterialized() {
		if (mStack == null && mItemSupplier != null) {Item tItem = mItemSupplier.get(); if (tItem != null) mStack = ST.amount(1, ST.make(tItem, 1, 0));}
	}
	
	@Override
	public IItemContainer set(Item aItem) {
		mHasNotBeenSet = F;
		if (aItem == null) {
//          new Exception().printStackTrace(GT_Log.deb);
			return this;
		}
		mStack = ST.amount(1, ST.make(aItem, 1, 0));
		return this;
	}
	
	public IItemContainer set(Item aItem, long aMeta) {
		mHasNotBeenSet = F;
		if (aItem == null) {
//          new Exception().printStackTrace(GT_Log.deb);
			return this;
		}
		mStack = ST.amount(1, ST.make(aItem, 1, aMeta));
		return this;
	}
	
	@Override
	public IItemContainer set(ItemStack aStack) {
		mHasNotBeenSet = F;
		if (ST.invalid(aStack)) {
//          new Exception().printStackTrace(GT_Log.deb);
			return this;
		}
		mStack = ST.amount(1, aStack);
		return this;
	}
	
	public IItemContainer set(Item aItem, OreDictItemData aData, Object... aOreDict) {
		mHasNotBeenSet = F;
		if (aItem == null) {
//          new Exception().printStackTrace(GT_Log.deb);
			return this;
		}
		ItemStack aStack = ST.make(aItem, 1, 0);
		mStack = ST.amount(1, aStack);
		if (aData != null && !OM.reg(aData.toString(), ST.make(aItem, 1, W))) OM.data(ST.make(aItem, 1, W), aData);
		for (Object tOreDict : aOreDict) OM.reg(tOreDict, ST.make(aItem, 1, W));
		return this;
	}
	
	public IItemContainer set(ItemStack aStack, OreDictItemData aData, Object... aOreDict) {
		mHasNotBeenSet = F;
		if (ST.invalid(aStack)) {
//          new Exception().printStackTrace(DEB);
			return this;
		}
		mStack = ST.amount(1, aStack);
		if (ST.meta_(mStack) == W) ST.meta_(mStack, 0);
		if (aData != null && !OM.reg(aData.toString(), ST.amount(1, aStack))) OM.data(ST.amount(1, aStack), aData);
		for (Object tOreDict : aOreDict) OM.reg(tOreDict, ST.amount(1, aStack));
		return this;
	}
	
	@Override
	public Item item() {
		ensureMaterialized(); // Materializes the deferred item before mStack is read.
		if (mHasNotBeenSet && Abstract_Mod.sFinalized < Abstract_Mod.sModCountUsingGTAPI) ERR.println("The Enum '" + mName + "' has not been set to an Item at this time!");
		if (ST.invalid(mStack)) return null;
		return mStack.getItem();
	}
	
	@Override
	public Block block() {
		return ST.block(get(0));
	}
	
	@Override
	public boolean exists() {
		ensureMaterialized(); // F12-lazy
		return ST.valid(mStack);
	}
	
	@Override
	public final boolean hasBeenSet() {
		return !mHasNotBeenSet;
	}
	
	@Override
	public boolean equal(Object aStackOrBlock) {
		ensureMaterialized(); // F12-lazy
		return mStack != null && (aStackOrBlock instanceof Block ? aStackOrBlock != NB && ST.block_(mStack) == aStackOrBlock : equal(aStackOrBlock, F, F));
	}
	
	@Override
	public boolean equal(Object aStack, boolean aWildcard, boolean aIgnoreNBT) {
		ensureMaterialized(); // F12-lazy
		return mStack != null && (aWildcard ? ST.item((ItemStack)aStack) == ST.item_(mStack) : ST.equal((ItemStack)aStack, mStack, aIgnoreNBT));
	}
	
	@Override
	public ItemStack get(long aAmount, Object... aReplacements) {
		ensureMaterialized(); // Materializes the deferred item before mStack is read.
		if (mHasNotBeenSet && Abstract_Mod.sFinalized < Abstract_Mod.sModCountUsingGTAPI) ERR.println("The Enum '" + mName + "' has not been set to an Item at this time!");
		if (ST.invalid(mStack)) return ST.copyFirst(aReplacements);
		return ST.amount(aAmount, OM.get_(mStack));
	}
	
	@Override
	@SuppressWarnings("deprecation")
	public ItemStack getWildcard(long aAmount, Object... aReplacements) {
		ensureMaterialized(); // Materializes the deferred item before mStack is read.
		if (mHasNotBeenSet && Abstract_Mod.sFinalized < Abstract_Mod.sModCountUsingGTAPI) ERR.println("The Enum '" + mName + "' has not been set to an Item at this time!");
		if (ST.invalid(mStack)) return ST.copyFirst(aReplacements);
		return ST.copyAmountAndMeta(aAmount, W, OM.get_(mStack));
	}
	
	@Override
	public ItemStack wild(long aAmount, Object... aReplacements) {
		ensureMaterialized(); // Materializes the deferred item before mStack is read.
		if (mHasNotBeenSet && Abstract_Mod.sFinalized < Abstract_Mod.sModCountUsingGTAPI) ERR.println("The Enum '" + mName + "' has not been set to an Item at this time!");
		if (ST.invalid(mStack)) return ST.copyFirst(aReplacements);
		return ST.copyAmountAndMeta(aAmount, W, OM.get_(mStack));
	}
	
	@Override
	public ItemStack getUndamaged(long aAmount, Object... aReplacements) {
		ensureMaterialized(); // Materializes the deferred item before mStack is read.
		if (mHasNotBeenSet && Abstract_Mod.sFinalized < Abstract_Mod.sModCountUsingGTAPI) ERR.println("The Enum '" + mName + "' has not been set to an Item at this time!");
		if (ST.invalid(mStack)) return ST.copyFirst(aReplacements);
		return ST.copyAmountAndMeta(aAmount, 0, OM.get_(mStack));
	}
	
	@Override
	public ItemStack getAlmostBroken(long aAmount, Object... aReplacements) {
		ensureMaterialized(); // Materializes the deferred item before mStack is read.
		if (mHasNotBeenSet && Abstract_Mod.sFinalized < Abstract_Mod.sModCountUsingGTAPI) ERR.println("The Enum '" + mName + "' has not been set to an Item at this time!");
		if (ST.invalid(mStack)) return ST.copyFirst(aReplacements);
		return ST.copyAmountAndMeta(aAmount, mStack.getMaxDamage()-1, OM.get_(mStack));
	}
	
	@Override
	public ItemStack getWithName(long aAmount, String aDisplayName, Object... aReplacements) {
		ItemStack rStack = get(1, aReplacements);
		if (ST.invalid(rStack)) return null;
		ST.name_(rStack, aDisplayName); // Display-name setting is repointed through the central ST.name_ helper, not the stack method directly.
		return ST.amount(aAmount, rStack);
	}
	
	@Override
	public ItemStack getWithNameAndNBT(long aAmount, String aDisplayName, CompoundTag aNBT, Object... aReplacements) {
		ItemStack rStack = get(1, aReplacements);
		if (ST.invalid(rStack)) return null;
		UT.NBT.set(rStack, aNBT);
		if (aDisplayName != null) ST.name_(rStack, aDisplayName); // Display-name setting is repointed through the central ST.name_ helper, not the stack method directly.
		return ST.amount(aAmount, rStack);
	}
	
	@Override
	public ItemStack getWithCharge(long aAmount, long aEnergy, Object... aReplacements) {
		ItemStack rStack = get(1, aReplacements);
		if (ST.invalid(rStack)) return null;
		if (rStack.getItem() instanceof IItemEnergy) for (TagData tEnergyType : ((IItemEnergy)rStack.getItem()).getEnergyTypes(rStack)) ((IItemEnergy)rStack.getItem()).setEnergyStored(tEnergyType, rStack, aEnergy);
		return ST.amount(aAmount, rStack);
	}
	
	@Override
	public ItemStack getWithMeta(long aAmount, long aMetaValue, Object... aReplacements) {
		ensureMaterialized(); // Materializes the deferred item before mStack is read.
		if (mHasNotBeenSet && Abstract_Mod.sFinalized < Abstract_Mod.sModCountUsingGTAPI) ERR.println("The Enum '" + mName + "' has not been set to an Item at this time!");
		if (ST.invalid(mStack)) return ST.copyFirst(aReplacements);
		return ST.copyAmountAndMeta(aAmount, aMetaValue, OM.get_(mStack));
	}
	
	@Override
	public ItemStack getWithDamage(long aAmount, long aMetaValue, Object... aReplacements) {
		ensureMaterialized(); // Materializes the deferred item before mStack is read.
		if (mHasNotBeenSet && Abstract_Mod.sFinalized < Abstract_Mod.sModCountUsingGTAPI) ERR.println("The Enum '" + mName + "' has not been set to an Item at this time!");
		if (ST.invalid(mStack)) return ST.copyFirst(aReplacements);
		return ST.copyAmountAndMeta(aAmount, aMetaValue, OM.get_(mStack));
	}
	
	@Override
	public ItemStack getWithNBT(long aAmount, CompoundTag aNBT, Object... aReplacements) {
		ensureMaterialized(); // Materializes the deferred item before mStack is read.
		if (mHasNotBeenSet && Abstract_Mod.sFinalized < Abstract_Mod.sModCountUsingGTAPI) ERR.println("The Enum '" + mName + "' has not been set to an Item at this time!");
		if (ST.invalid(mStack)) return ST.copyFirst(aReplacements);
		ItemStack rStack = ST.amount(aAmount, OM.get_(mStack));
		UT.NBT.set(rStack, aNBT);
		return rStack;
	}
	
	@Override
	public IItemContainer registerOre(Object... aOreNames) {
		ensureMaterialized(); // Materializes the deferred item before mStack is read.
		if (mHasNotBeenSet && Abstract_Mod.sFinalized < Abstract_Mod.sModCountUsingGTAPI) ERR.println("The Enum '" + mName + "' has not been set to an Item at this time!");
		for (Object tOreName : aOreNames) OM.reg(tOreName, get(1));
		return this;
	}
	
	@Override
	public IItemContainer registerWildcardAsOre(Object... aOreNames) {
		ensureMaterialized(); // Materializes the deferred item before mStack is read.
		if (mHasNotBeenSet && Abstract_Mod.sFinalized < Abstract_Mod.sModCountUsingGTAPI) ERR.println("The Enum '" + mName + "' has not been set to an Item at this time!");
		for (Object tOreName : aOreNames) OM.reg(tOreName, wild(1));
		return this;
	}
	
	@SuppressWarnings("deprecation") @Override public Item getItem() {return item();}
	@SuppressWarnings("deprecation") @Override public Block getBlock() {return block();}
}

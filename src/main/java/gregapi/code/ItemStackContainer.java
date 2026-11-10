/**
 * Copyright (c) 2020 GregTech-6 Team
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

package gregapi.code;

import static gregapi.data.CS.*;

import gregapi.util.ST;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * @author Gregorius Techneticies
 */
public class ItemStackContainer {
	public final Item mItem;
	public final Block mBlock;
	public final byte mStackSize;
	public final short mMetaData;
	private static Item canonicalItem(Item aItem, long aMeta) {return aItem instanceof gregapi.item.IMetaItem tMeta ? aMeta == W ? tMeta.metaFamily() : tMeta.itemForMeta((short)aMeta) : aItem;}
	
	private final net.minecraft.world.item.alchemy.PotionContents mPotionContents;
	private static net.minecraft.world.item.alchemy.PotionContents potionContents(ItemStack aStack) {
		var tContents = aStack.get(net.minecraft.core.component.DataComponents.POTION_CONTENTS);
		return net.minecraft.world.item.alchemy.PotionContents.EMPTY.equals(tContents) ? null : tContents;
	}
	public ItemStackContainer(Item aItem, long aStackSize, long aMetaData) {
		ItemStack tPotion = ST.isPotion(aItem) ? ST.make(aItem, 1, aMetaData) : null;
		mItem = tPotion == null ? canonicalItem(aItem, aMetaData) : tPotion.getItem();
		mBlock = ST.block(mItem);
		mStackSize = (byte)aStackSize;
		mMetaData = tPotion == null ? (short)aMetaData : ST.meta_(tPotion);
		mPotionContents = tPotion == null ? null : potionContents(tPotion);
	}
	public ItemStackContainer(Block aBlock, long aStackSize, long aMetaData) {this(ST.item(aBlock), aStackSize, aMetaData);}
	public ItemStackContainer(long aID, long aStackSize, long aMetaData) {this(ST.item(aID), aStackSize, aMetaData);}
	public ItemStackContainer(ItemStack aStack) {this(aStack, aStack == null ? 0 : aStack.getCount(), ST.meta(aStack));}
	public ItemStackContainer(ItemStack aStack, long aMetaData) {this(aStack, aStack == null ? 0 : aStack.getCount(), aMetaData);}
	public ItemStackContainer(ItemStack aStack, long aStackSize, long aMetaData) {
		ItemStack tPotion = aStack != null && ST.isPotion(aStack.getItem()) ? ST.meta_(aStack) == aMetaData ? aStack : ST.make(aStack.getItem(), 1, aMetaData) : null;
		mItem = aStack == null ? null : canonicalItem(tPotion == null ? aStack.getItem() : tPotion.getItem(), aMetaData);
		mBlock = ST.block(mItem);
		mStackSize = aStack == null ? 0 : (byte)aStackSize;
		mMetaData = aStack == null ? 0 : tPotion == null ? (short)aMetaData : ST.meta_(tPotion);
		mPotionContents = tPotion == null ? null : potionContents(tPotion);
	}

	public ItemStackContainer(int aHashCode) {
		this(ST.toItem(aHashCode), 0, ST.toMeta(aHashCode));
	}
	
	public ItemStack toStack() {
		ItemStack rStack = mItem == null ? null : ST.make(mItem, 1, mMetaData);
		if (rStack != null && mPotionContents != null) rStack.set(net.minecraft.core.component.DataComponents.POTION_CONTENTS, mPotionContents);
		return rStack;
	}
	public boolean isStackEqual(ItemStack aStack) {return mPotionContents == null ? ST.equal(aStack, mItem, mMetaData) : ST.equal(aStack, toStack(), T);}
	public boolean isStackEqual(ItemStackContainer aStack) {return (aStack.mItem == mItem || (mMetaData == W || aStack.mMetaData == W) && Flattened.sameFamily(mItem, aStack.mItem)) && ST.equal(aStack.mMetaData, mMetaData) && (mMetaData == W || aStack.mMetaData == W || java.util.Objects.equals(mPotionContents, aStack.mPotionContents));}
	
	@Override public boolean equals(Object aStack) {return aStack == this || (aStack instanceof ItemStackContainer && ((ItemStackContainer)aStack).mItem == mItem && ((ItemStackContainer)aStack).mMetaData == mMetaData && java.util.Objects.equals(((ItemStackContainer)aStack).mPotionContents, mPotionContents));}
	@Override public int hashCode() {return mPotionContents == null ? ST.toInt(mItem, mMetaData) : 31 * ST.toInt(mItem, mMetaData) + mPotionContents.hashCode();}
}

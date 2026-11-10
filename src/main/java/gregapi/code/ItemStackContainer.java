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
	
	public ItemStackContainer(Item aItem, long aStackSize, long aMetaData) {
		mItem = canonicalItem(aItem, aMetaData);
		mBlock = ST.block(mItem);
		mStackSize = (byte)aStackSize;
		mMetaData = (short)aMetaData;
	}
	public ItemStackContainer(Block aBlock, long aStackSize, long aMetaData) {
		mBlock = aBlock;
		mItem = canonicalItem(ST.item(mBlock), aMetaData);
		mStackSize = (byte)aStackSize;
		mMetaData = (short)aMetaData;
	}
	public ItemStackContainer(long aID, long aStackSize, long aMetaData) {
		mItem = canonicalItem(ST.item(aID), aMetaData);
		mBlock = ST.block(mItem);
		mStackSize = (byte)aStackSize;
		mMetaData = (short)aMetaData;
	}
	public ItemStackContainer(ItemStack aStack) {
		if (aStack == null) {
			mItem = null;
			mBlock = NB;
			mStackSize = 0;
			mMetaData = 0;
		} else {
			mItem = canonicalItem(ST.item_(aStack), ST.meta_(aStack));
			mBlock = ST.block_(mItem);
			mStackSize = (byte)aStack.getCount();
			mMetaData = ST.meta_(aStack);
		}
	}
	public ItemStackContainer(ItemStack aStack, long aMetaData) {
		if (aStack == null) {
			mItem = null;
			mBlock = NB;
			mStackSize = 0;
			mMetaData = 0;
		} else {
			mItem = canonicalItem(ST.item_(aStack), aMetaData);
			mBlock = ST.block_(mItem);
			mStackSize = (byte)aStack.getCount();
			mMetaData = (short)aMetaData;
		}
	}
	public ItemStackContainer(ItemStack aStack, long aStackSize, long aMetaData) {
		if (aStack == null) {
			mItem = null;
			mBlock = NB;
			mStackSize = 0;
			mMetaData = 0;
		} else {
			mItem = canonicalItem(ST.item_(aStack), aMetaData);
			mBlock = ST.block_(mItem);
			mStackSize = (byte)aStackSize;
			mMetaData = (short)aMetaData;
		}
	}
	public ItemStackContainer(int aHashCode) {
		this(ST.toItem(aHashCode), 0, ST.toMeta(aHashCode));
	}
	
	public ItemStack toStack() {return mItem == null ? null : ST.make(mItem, 1, mMetaData);}
	public boolean isStackEqual(ItemStack aStack) {return ST.equal(aStack, mItem, mMetaData);}
	public boolean isStackEqual(ItemStackContainer aStack) {return aStack.mItem == mItem && ST.equal(aStack.mMetaData, mMetaData);}
	
	@Override public boolean equals(Object aStack) {return aStack == this || (aStack instanceof ItemStackContainer && ((ItemStackContainer)aStack).mItem == mItem && ((ItemStackContainer)aStack).mMetaData == mMetaData);}
	@Override public int hashCode() {return ST.toInt(mItem, mMetaData);}
}

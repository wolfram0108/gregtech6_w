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

package gregapi.gui;

import gregapi.code.ArrayListNoNulls;
import gregapi.data.LH;
import gregapi.tileentity.ITileEntityInventoryGUI;
import gregapi.util.ST;
import gregapi.util.UT;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.List;

import static gregapi.data.CS.*;

/** @author Gregorius Techneticies
 *  The engine renamed nearly every Slot hook to the same meaning under a new name (getStack->getItem,
 *  isItemValid->mayPlace, and so on); logic and ordering are untouched, only the symbols changed. */
public class Slot_Base extends Slot {
	private String[] mToolTips = ZL_STRING;
	private String[] mToolTipColors = ZL_STRING;

	public final int mIndex;
	public final ITileEntityInventoryGUI mInventory;

	public boolean mCanTake = T, mCanPut = T;

	protected Slot_Base(ITileEntityInventoryGUI aInventory, int aIndex, int aX, int aY) {
		super(aInventory instanceof Container ? (Container)aInventory : null, aIndex, aX, aY);
		mInventory = aInventory;
		mIndex = aIndex;
	}

	public List<String> getTooltip(Player aPlayer, boolean aF3_H) {
		ArrayListNoNulls<String> rList = new ArrayListNoNulls<>();
		for (int i = 0; i < mToolTips.length; i++) {
			if (mToolTipColors[i] == null) mToolTipColors[i] = LH.Chat.GRAY;
			rList.add(mToolTipColors[i] + LH.get(mToolTips[i]));
		}
		return rList;
	}

	@Override
	public ItemStack getItem() {
		ItemStack rStack = mInventory.getStackInSlotGUI(mIndex);
		if (rStack == null) return ST.nn(rStack); // neo's Slot requires non-null; null (GT6's empty) becomes ItemStack.EMPTY before the wildcard branch runs.
		return ST.meta(rStack) != W || ST.isGT(rStack) ? rStack : ST.name(ST.copyMeta(0, rStack), ST.regName(rStack) + ":Wildcard");
	}

	public Slot_Base setCanTake(boolean aCanTake) {
		mCanTake = aCanTake;
		return this;
	}

	public Slot_Base setCanPut(boolean aCanPut) {
		mCanPut = aCanPut;
		return this;
	}

	public Slot_Base setTooltip(String aTooltip, String aToolTipColor) {
		mToolTips = new String[] {aTooltip};
		mToolTipColors = new String[] {aToolTipColor};
		return this;
	}

	public Slot_Base setTooltips(String[] aTooltips, String[] aToolTipColors) {
		mToolTips = aTooltips;
		mToolTipColors = (aToolTipColors.length < mToolTips.length ? mToolTipColors = new String[mToolTips.length] : aToolTipColors);
		return this;
	}

	@Override public boolean mayPlace(ItemStack aStack) {return mCanPut && mInventory.isItemValidForSlotGUI(mIndex, aStack);}
	@Override public boolean mayPickup(Player aPlayer) {return mInventory.canTakeOutOfSlotGUI(mIndex) && (UT.Entities.isCreative(aPlayer) || (mCanTake && !ST.debug(getItem())));}
	/** Engine dropped isSlotInInventory from Slot entirely; kept as a plain method, its only caller unchanged. */
	public boolean isSlotInInventory(Container aInventory, int aIndex) {return aInventory == mInventory && aIndex == mIndex;}
	@Override public int getMaxStackSize() {return mInventory.getInventoryStackLimitGUI(mIndex);}
	@Override public void set(ItemStack aStack) {if (ST.size(aStack) > 64) ST.size_(64, aStack); mInventory.setInventorySlotContentsGUI(mIndex, ST.ni(aStack)); setChanged();} // The EMPTY singleton maps back to null; a meaningful non-singleton count==0 stack is not collapsed by isEmpty().
	@Override public ItemStack remove(int aAmount) {return ST.nn(mInventory.decrStackSizeGUI(mIndex, aAmount));} // null maps to EMPTY here, the same boundary {@link #getItem()} uses.
	@Override public void setChanged() {mInventory.markDirtyGUI();}
	@Override public boolean isActive() {return T;}
	@Override public void onQuickCraft(ItemStack aStack, ItemStack aStack2) {if (ST.equal(aStack, aStack2, T)) {int tDifference = aStack2.getCount() - aStack.getCount(); if (tDifference > 0) onQuickCraft(aStack, tDifference);}}
	@Override protected void onQuickCraft(ItemStack aStack, int aDifference) {/**/}
	@Override public void onTake(Player aPlayer, ItemStack aStack) {setChanged();}
	@Override public boolean hasItem() {return mInventory.getStackInSlotGUI(mIndex) != null;} // Raw GT6 null-check, matching the original getHasStack(); a count==0 stack still counts as occupied.
	@Override public int getSlotIndex() {return mIndex;}
}

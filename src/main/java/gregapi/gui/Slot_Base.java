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

/**
 * @author Gregorius Techneticies
 *
 * F-GUI (шов «GUI/меню», серверный центр): 1.7.10 {@code net.minecraft.inventory.Slot} переименовало
 * почти каждый хук движок neo — символы поменяны 1:1 на новые имена того же смысла (сверено,
 * {@code neo-decompiled/net/minecraft/world/inventory/Slot.java}), логика/порядок не тронуты:
 * {@code getStack()}→{@code getItem()} (Slot.java:48), {@code isItemValid}→{@code mayPlace} (:44),
 * {@code canTakeStack}→{@code mayPickup} (:89), {@code getSlotStackLimit()}→{@code getMaxStackSize()} (:73),
 * {@code putStack}→{@code set} (:64), {@code decrStackSize}→{@code remove} (:85), {@code onSlotChanged}→
 * {@code setChanged} (:69), {@code onPickupFromSlot}→{@code onTake} (:40), {@code getHasStack}→{@code hasItem} (:52),
 * {@code func_111238_b}→{@code isActive} (:93, тот же смысл — булев гейт «можно ли наводить/подсвечивать
 * слот», используется {@code AbstractContainerScreen.java:167,261,316}), {@code onSlotChange(ItemStack,ItemStack)}→
 * {@code onQuickCraft(ItemStack,ItemStack)} (:24-29, та же пара «picked,original»→«picked,count»).
 * {@code isSlotInInventory(IInventory,int)} движок убрал целиком (в {@code Slot.java} такого метода больше
 * нет) — оставлен НЕ-{@code @Override} собственным методом (используется только {@link ContainerCommon#getSlotFromInventory}).
 * {@code IInventory}(1.7.10)→{@code Container} (neo, `Slot.java:12` поле {@code container}).
 */
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
		if (rStack == null) return ST.nn(rStack); // F15-мост: neo Slot требует non-null, null (GT6-пусто) → ItemStack.EMPTY, ДО Wildcard-ветки
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
	/** Движок убрал {@code isSlotInInventory} из {@code Slot} целиком — оставлен как собственный (не {@code @Override}) метод, единственный вызыватель {@link ContainerCommon#getSlotFromInventory}. */
	public boolean isSlotInInventory(Container aInventory, int aIndex) {return aInventory == mInventory && aIndex == mIndex;}
	@Override public int getMaxStackSize() {return mInventory.getInventoryStackLimitGUI(mIndex);}
	@Override public void set(ItemStack aStack) {if (ST.size(aStack) > 64) ST.size_(64, aStack); mInventory.setInventorySlotContentsGUI(mIndex, ST.ni(aStack)); setChanged();} // F15-мост: EMPTY-синглтон→null; значимый count==0 (не-синглтон) сохраняется, не схлопывается isEmpty()
	@Override public ItemStack remove(int aAmount) {return ST.nn(mInventory.decrStackSizeGUI(mIndex, aAmount));} // F15-мост: null→EMPTY (см. getItem())
	@Override public void setChanged() {mInventory.markDirtyGUI();}
	@Override public boolean isActive() {return T;}
	@Override public void onQuickCraft(ItemStack aStack, ItemStack aStack2) {if (ST.equal(aStack, aStack2, T)) {int tDifference = aStack2.getCount() - aStack.getCount(); if (tDifference > 0) onQuickCraft(aStack, tDifference);}}
	@Override protected void onQuickCraft(ItemStack aStack, int aDifference) {/**/}
	@Override public void onTake(Player aPlayer, ItemStack aStack) {setChanged();}
	@Override public boolean hasItem() {return mInventory.getStackInSlotGUI(mIndex) != null;} // F15-мост: сырой GT6-null-чек (= оригинал getHasStack()=getStack()!=null); count==0 = ЗАНЯТ, НЕ isEmpty()
	@Override public int getSlotIndex() {return mIndex;}
}

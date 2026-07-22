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

package gregapi.tileentity.base;

import gregapi.block.multitileentity.IMultiTileEntity.IMTE_BreakBlock;
import gregapi.block.multitileentity.IMultiTileEntity.IMTE_OnBlockExploded;
import gregapi.block.multitileentity.MultiTileEntityRegistry;
import gregapi.code.ItemNBT;
import gregapi.tileentity.ITileEntityInventoryGUI;
import gregapi.util.OM;
import gregapi.util.ST;
import gregapi.util.UT;
import net.minecraft.world.entity.ContainerUser;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.level.Explosion;

import static gregapi.data.CS.*;

/**
 * @author Gregorius Techneticies
 */
public abstract class TileEntityBase05Inventories extends TileEntityBase04MultiTileEntities implements Container, ITileEntityInventoryGUI, IMTE_OnBlockExploded, IMTE_BreakBlock {
	private ItemStack[] mInventory = ZL_IS;
	
	public boolean mInventoryChanged = F;
	
	@Override
	public void readFromNBT2(CompoundTag aNBT) {
		// Standard readFromNBT process to load Inventory.
		mInventory = getDefaultInventory(aNBT);
		if (mInventory != null && mInventory.length > 0) {
			ListTag tList = aNBT.getListOrEmpty(NBT_INV_LIST);
			for (int i = 0; i < tList.size(); i++) {
				CompoundTag tNBT = tList.getCompound(i).orElse(UT.NBT.make());
				int tSlot = tNBT.getShort("s").orElse((short)0);
				if (tSlot >= 0 && tSlot < mInventory.length) mInventory[tSlot] = ST.load(tNBT, getDefaultStack(tSlot));
			}
		}
	}
	
	@Override
	public void writeToNBT2(CompoundTag aNBT) {
		if (mInventory != null && mInventory.length > 0) {
			ListTag tList = new ListTag();
			for (short tSlot = 0; tSlot < mInventory.length; tSlot++) if (ST.valid(mInventory[tSlot]) && canSave (tSlot)) tList.add(UT.NBT.makeShort(ST.save(mInventory[tSlot]), "s", tSlot));
			if (tList.size() > 0) aNBT.put(NBT_INV_LIST, tList);
		}
	}
	
	@Override
	public CompoundTag writeItemNBT(CompoundTag aNBT) {
		aNBT = super.writeItemNBT(aNBT);
		if (mInventory != null && mInventory.length > 0) {
			ListTag tList = new ListTag();
			for (short tSlot = 0; tSlot < mInventory.length; tSlot++) if (ST.valid(mInventory[tSlot]) && keepSlot(tSlot)) tList.add(UT.NBT.makeShort(ST.save(mInventory[tSlot]), "s", tSlot));
			if (tList.size() > 0) aNBT.put(NBT_INV_LIST, tList);
		}
		return aNBT;
	}
	
	public ItemStack[] getDefaultInventory(CompoundTag aNBT) {
		int tSize = Math.max(getMinimumInventorySize(), aNBT.getShort(NBT_INV_SIZE).orElse((short)0));
		return tSize > 0 ? new ItemStack[tSize] : ZL_IS;
	}
	
	@Override
	public void onTickResetChecks(long aTimer, boolean aIsServerSide) {
		super.onTickResetChecks(aTimer, aIsServerSide);
		mInventoryChanged = F;
	}
	
	@Override public final ItemStack slot(int aIndex, ItemStack aStack) {return mInventory[aIndex] = aStack;}
	@Override public final ItemStack slot(int aIndex) {return mInventory[aIndex];}
	@Override public final ItemStack slotTake(int aIndex) {ItemStack rStack = mInventory[aIndex]; mInventory[aIndex] = null; return rStack;}
	@Override public final boolean slotTrash(int aIndex) {return GarbageGT.trash(slotTake(aIndex)) > 0;}
	@Override public final boolean slotNull(int aIndex) {if (mInventory[aIndex] != null && mInventory[aIndex].getCount() <= 0) return slotKill(aIndex); return F;}
	@Override public final boolean slotKill(int aIndex) {mInventory[aIndex] = null; return T;}
	@Override public final boolean slotHas(int aIndex) {return mInventory[aIndex] != null;}
	@Override public final boolean invempty() {for (int i = 0; i < mInventory.length; i++) if (mInventory[i] != null) return F; return T;}
	@Override public final int invsize() {return mInventory.length;}
	@Override public final CompoundTag slotNBT(int aIndex) {return mInventory[aIndex] != null ? ItemNBT.get(mInventory[aIndex]) : null;}
	
	@Override public void updateTanks() {mInventoryChanged = T;}
	@Override public void updateInventory() {mInventoryChanged = T;}
	@Override public boolean stillValid(Player aPlayer) {return !isDead() && allowInteraction(aPlayer) && aPlayer.distanceToSqr(getBlockPos().getX() + 0.5D, getBlockPos().getY() + 0.5D, getBlockPos().getZ() + 0.5D) <= 64D;}
	public void openInventory () {/**/}
	public void closeInventory() {/**/}
	@Override public void startOpen(ContainerUser aContainerUser) {openInventory();}
	@Override public void stopOpen(ContainerUser aContainerUser) {closeInventory();}
	@Override public int getMaxStackSize() {return 64;}
	@Override public void setChanged() {super.setChanged(); updateInventory();}
	@Override public boolean isEmpty() {return invempty();}
	@Override public void clearContent() {for (int i = 0; i < mInventory.length; i++) slotKill(i);}
	// F15-size0 (BUG-015 v2): allowZeroStacks-слот («тип запомнен, штук 0», 1.7.10 stackSize=0) хранится как
	// ZEROSIZE-призрак (count=1+маркер, ST.size_(0)) — физический count=0 слеп для ВСЕХ neo-чтений (copy/getItem/equal).
	// Логический размер — ST.count; изъятие из призрака = пусто (1.7.10 возвращал size0-стек = «ноль предметов»).
	@Override public ItemStack removeItem(int aSlot, int aDecrement) {updateInventory(); if (mInventory[aSlot] == null || aDecrement <= 0 || ST.count(mInventory[aSlot]) <= 0) return NI; if (ST.count(mInventory[aSlot]) <= aDecrement) {ItemStack tStack = ST.copy(mInventory[aSlot]); if (allowZeroStacks(aSlot)) ST.size_(0, mInventory[aSlot]); else mInventory[aSlot] = NI; return tStack;} ItemStack rStack = mInventory[aSlot].split(aDecrement); if (mInventory[aSlot].getCount() <= 0 && !allowZeroStacks(aSlot)) mInventory[aSlot] = NI; return rStack;}
	@Override public ItemStack removeItemNoUpdate(int aSlot) {ItemStack rStack = mInventory[aSlot]; mInventory[aSlot] = null; return rStack;}
	@Override public ItemStack getItem(int aSlot) {return mInventory[aSlot];}

	/** F15/F-break (NPE игрока 2026-07-19 при ломании MTE): neo BlockEntity.preRemoveSideEffects (BlockEntity.java:264-268)
	 *  сам вытряхивает любой Container через Containers.dropContents — НОВОЕ поведение движка (в 1.7.10 дроп содержимого
	 *  делал ТОЛЬКО сам мод в breakBlock). GT6-инвентарь держит null-слоты (F15 null-модель) → vanilla dropItemStack NPE;
	 *  плюс двойной дроп с GT6-путём (IMTE_BreakBlock.breakBlock → MultiTileEntityBlock.breakBlock:205). Дроп владеет GT6. */
	@Override public void preRemoveSideEffects(net.minecraft.core.BlockPos aPos, net.minecraft.world.level.block.state.BlockState aState) {/* дроп — GT6 breakBlock, не vanilla Container-вытряхивание */}
	public String getInventoryName() {String rName = getCustomName(); if (UT.Code.stringValid(rName)) return rName; MultiTileEntityRegistry tRegistry = MultiTileEntityRegistry.getRegistry(getMultiTileEntityRegistryID()); return tRegistry==null?getClass().getName():tRegistry.getLocal(getMultiTileEntityID());}
	@Override public int getContainerSize() {return mInventory==null?0:mInventory.length;}
	@Override public void setItem(int aSlot, ItemStack aStack) {updateInventory(); mInventory[aSlot] = OM.get(aStack);}
	public boolean hasCustomInventoryName() {return getCustomName() != null;}
	@Override public boolean canPlaceItem(int aSlot, ItemStack aStack) {return T;}
	public int getMinimumInventorySize() {return 0;}
	public boolean allowZeroStacks(int aSlot) {return F;}
	public ItemStack[] getInventory() {return mInventory;}
	public void setInventory(ItemStack[] aInventory) {mInventory = aInventory;}
	public void removeAllDroppableNullStacks() {for (int i = 0; i < mInventory.length; i++) if (canDrop(i) && mInventory[i] != null && ST.count(mInventory[i]) <= 0) mInventory[i] = NI;} // F15-size0: ZEROSIZE-призрак (физ. count=1) — тоже «мёртвый» стек, НЕ дропать (иначе дюп при ломании)
	
	public abstract boolean canDrop  (int aSlot);
	public          boolean keepSlot (int aSlot) {return F;}
	public          boolean breakDrop(int aSlot) {return T;}
	public          boolean canSave  (int aSlot) {return T;}
	/** Returns a Stack to be put into that Slot in case of a Mod being uninstalled causing a Loading Error for the original ItemStack. For example Shelves just replace the missing Item with a normal Book instead. */
	public ItemStack getDefaultStack(int aSlot) {return null;}
	
	// These Functions are intentionally duplicates of the Functions above.
	@Override public int getSizeInventoryGUI() {return mInventory==null?0:mInventory.length;}
	@Override public ItemStack getStackInSlotGUI(int aSlot) {return mInventory[aSlot];}
	@Override public ItemStack decrStackSizeGUI(int aSlot, int aDecrement) {updateInventory(); if (mInventory[aSlot] == null || aDecrement <= 0 || ST.count(mInventory[aSlot]) <= 0) return NI; if (ST.count(mInventory[aSlot]) <= aDecrement) {ItemStack tStack = ST.copy(mInventory[aSlot]); if (allowZeroStacks(aSlot)) ST.size_(0, mInventory[aSlot]); else mInventory[aSlot] = NI; return tStack;} ItemStack rStack = mInventory[aSlot].split(aDecrement); if (mInventory[aSlot].getCount() <= 0 && !allowZeroStacks(aSlot)) mInventory[aSlot] = NI; return rStack;}
	@Override public ItemStack getStackInSlotOnClosingGUI(int aSlot) {ItemStack rStack = mInventory[aSlot]; mInventory[aSlot] = null; return rStack;}
	@Override public void setInventorySlotContentsGUI(int aSlot, ItemStack aStack) {updateInventory(); mInventory[aSlot] = OM.get(aStack);}
	@Override public String getInventoryNameGUI() {String rName = getCustomName(); if (UT.Code.stringValid(rName)) return rName; MultiTileEntityRegistry tRegistry = MultiTileEntityRegistry.getRegistry(getMultiTileEntityRegistryID()); return tRegistry==null?getClass().getName():tRegistry.getLocal(getMultiTileEntityID());}
	@Override public boolean hasCustomInventoryNameGUI() {return getCustomName() != null;}
	@Override public int getInventoryStackLimitGUI(int aSlot) {return getMaxStackSize();}
	@Override public void markDirtyGUI() {setChanged();}
	@Override public boolean isUseableByPlayerGUI(Player aPlayer) {return !isDead() && allowInteraction(aPlayer) && aPlayer.distanceToSqr(getBlockPos().getX() + 0.5D, getBlockPos().getY() + 0.5D, getBlockPos().getZ() + 0.5D) <= 64D;}
	@Override public void openInventoryGUI() {openInventory();}
	@Override public void closeInventoryGUI() {closeInventory();}
	@Override public boolean isItemValidForSlotGUI(int aSlot, ItemStack aStack) {return canPlaceItem(aSlot, aStack);}
	@Override public boolean canTakeOutOfSlotGUI(int aSlot) {return T;}
	
	@Override
	public void onExploded(Explosion aExplosion) {
		for (int i = 0; i < mInventory.length; i++) if (RNGSUS.nextInt(3) != 0) GarbageGT.trash(mInventory, i);
		setToAir();
	}
	
	@Override
	public boolean breakBlock() {
		if (isServerSide()) for (short i = 0; i < mInventory.length; i++) if (mInventory[i] != null && canDrop(i) && !ST.debug(mInventory[i]) && breakDrop(i)) {
			ItemStack tDumpedStack = ST.amount(UT.Code.bind_(0, 512L * Math.max(1, mInventory[i].getMaxStackSize()), mInventory[i].getCount()), mInventory[i]);
			int tMaxSize = Math.max(1, mInventory[i].getMaxStackSize());
			
			while (tDumpedStack.getCount() > tMaxSize) {
				ST.drop(level, getCoords(), ST.amount(tMaxSize, tDumpedStack));
				tDumpedStack.setCount(tDumpedStack.getCount()-(tMaxSize));
				mInventory[i].setCount(mInventory[i].getCount()-(tMaxSize));
			}
			if (tDumpedStack.getCount() > 0) {
				mInventory[i].setCount(mInventory[i].getCount()-(tDumpedStack.getCount()));
				ST.drop(level, getCoords(), ST.copy(tDumpedStack));
			}
			
			GarbageGT.trash(mInventory, i);
		}
		return F;
	}
	
	public boolean addStackToSlot(int aIndex, ItemStack aStack) {
		if (ST.invalid(aStack)) return T;
		if (aIndex < 0 || aIndex >= getContainerSize()) return F;
		ItemStack tStack = getItem(aIndex);
		if (ST.invalid(tStack)) {
			setItem(aIndex, aStack);
			return T;
		}
		aStack = OM.get_(aStack);
		if (ST.equal(tStack, aStack) && tStack.getCount() + aStack.getCount() <= Math.min(Math.max(1, tStack.getMaxStackSize()), getMaxStackSize())) {
			tStack.setCount(tStack.getCount()+(aStack.getCount()));
			updateInventory();
			return T;
		}
		return F;
	}
}

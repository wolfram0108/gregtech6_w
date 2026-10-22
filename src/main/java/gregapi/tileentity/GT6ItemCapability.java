/**
 * Copyright (c) 2026 wolfram0108
 *
 * Written in 2026 for the GregTech 6 NeoForge port
 * (https://github.com/wolfram0108/gregtech6_w). Not part of the original GregTech 6
 * by Gregorius Techneticies; distributed under the same licence as the work it extends.
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

package gregapi.tileentity;

import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.wrapper.InvWrapper;
import net.minecraftforge.items.wrapper.SidedInvWrapper;

/** Vanilla hoppers still read the inventory directly; modded transport needs ForgeCapabilities.ITEM_HANDLER instead,
 *  declared once at the BlockEntity root and wrapped by the stock SidedInvWrapper, which still asks GT6's own side rules. */
public final class GT6ItemCapability {
	private GT6ItemCapability() {}

	/** Whether there's anything to show externally: an empty inventory is not the same as having none at all. */
	public static boolean hasInventory(net.minecraft.world.level.block.entity.BlockEntity aBlockEntity) {
		try {
			return aBlockEntity instanceof Container tContainer && tContainer.getContainerSize() > 0;
		} catch (Throwable e) {return false;} // A specific TE's own logic must not crash another mod that simply asked for this capability.
	}

	/** The handler is bound to whichever side was requested; null means a sideless request, GT6's own SIDE_ANY. */
	public static IItemHandler handlerOf(net.minecraft.world.level.block.entity.BlockEntity aBlockEntity, Direction aSide) {
		if (aBlockEntity instanceof WorldlyContainer tWorldly) return new SidedInvWrapper(tWorldly, aSide);
		return new InvWrapper((Container)aBlockEntity);
	}

	// ==============================================================================================
	// The other way round: another mod's item storage as GT6 code meets it next to a GT6 block.
	// ==============================================================================================

	/**
	 * Another mod's item storage on a block entity's side, as GT6 code sees one: a WorldlyContainer, through ForeignContainer.
	 * 1.7.10's foreign storages were IInventory block entities, so every mod's chest was reached by GT6's hoppers, pipes and machines
	 * without a line of mod-specific code; Forge's storages need not be a Container, only to answer the item capability. A block that
	 * is a Container itself needs no adapter, and one without storage stays out. Not cached, like the fluid twin: Forge answers about
	 * as cheaply, and a kept answer could outlive its block, since a mod need not invalidate its own LazyOptional.
	 */
	public static ForeignContainer foreign(BlockEntity aTileEntity, byte aSide) {
		if (aTileEntity instanceof Container || aTileEntity.isRemoved()) return null;
		IItemHandler tHandler;
		try {tHandler = aTileEntity.getCapability(ForgeCapabilities.ITEM_HANDLER, gregapi.data.CS.FORGE_DIR[aSide]).orElse(null);} catch (Throwable e) {return null;}
		if (tHandler == null) return null;
		ForeignContainer rForeign = new ForeignContainer(tHandler);
		return rForeign.getContainerSize() > 0 ? rForeign : null;
	}

	/**
	 * Forge's item handler of another mod's block as the Container GT6's transfer code is written against. Stacks it hands out are
	 * snapshots (a foreign storage never lets its own be changed in place), every change goes through the handler, the rules of what
	 * can go where stay the storage's own, and a storage that fails moves nothing instead of erroring a GT6 tick.
	 */
	public static final class ForeignContainer implements WorldlyContainer {
		private final IItemHandler mHandler;
		private ForeignContainer(IItemHandler aHandler) {mHandler = aHandler;}

		@Override public int getContainerSize() {try {return mHandler.getSlots();} catch (Throwable e) {return 0;}}
		@Override public boolean isEmpty() {for (int i = 0, j = getContainerSize(); i < j; i++) if (!getItem(i).isEmpty()) return false; return true;}
		@Override public ItemStack getItem(int aSlot) {
			try {ItemStack tStack = mHandler.getStackInSlot(aSlot); return tStack == null || tStack.isEmpty() ? ItemStack.EMPTY : tStack.copy();} catch (Throwable e) {return ItemStack.EMPTY;}
		}
		@Override public ItemStack removeItem(int aSlot, int aCount) {
			if (aCount <= 0) return ItemStack.EMPTY;
			try {ItemStack rTaken = mHandler.extractItem(aSlot, aCount, false); return rTaken == null ? ItemStack.EMPTY : rTaken;} catch (Throwable e) {return ItemStack.EMPTY;}
		}
		@Override public ItemStack removeItemNoUpdate(int aSlot) {
			try {return removeItem(aSlot, Math.max(1, mHandler.getStackInSlot(aSlot).getCount()));} catch (Throwable e) {return ItemStack.EMPTY;}
		}
		/** Puts a stack into one slot and reports how much of it the storage took; the rest stays with the caller. */
		public int put(int aSlot, ItemStack aStack) {
			if (aStack == null || aStack.isEmpty()) return 0;
			try {
				ItemStack tRest = mHandler.insertItem(aSlot, aStack.copy(), false);
				return aStack.getCount() - (tRest == null ? 0 : tRest.getCount());
			} catch (Throwable e) {return 0;}
		}
		/** Only an empty slot takes a stack set into it, and what the storage refuses is reported, never dropped in silence. */
		@Override public void setItem(int aSlot, ItemStack aStack) {
			if (aStack == null || aStack.isEmpty()) {removeItemNoUpdate(aSlot); return;}
			int tPut = put(aSlot, aStack);
			if (tPut < aStack.getCount()) gregapi.data.CS.ERR.println("[GT6] a foreign item storage refused " + (aStack.getCount() - tPut) + " of " + aStack.getCount() + " items set into slot " + aSlot);
		}
		@Override public void setChanged() {/* the foreign storage marks its own changes */}
		@Override public boolean stillValid(Player aPlayer) {return false;}
		/** Never clears a storage that is not GT6's. */
		@Override public void clearContent() {}
		@Override public boolean canPlaceItem(int aSlot, ItemStack aStack) {
			try {
				// the question is asked of the storage itself: simulated, never committed
				ItemStack tOne = aStack.copy();
				tOne.setCount(1);
				if (!mHandler.isItemValid(aSlot, tOne)) return false;
				ItemStack tRest = mHandler.insertItem(aSlot, tOne, true);
				return tRest == null || tRest.isEmpty();
			} catch (Throwable e) {return false;}
		}

		@Override public int[] getSlotsForFace(Direction aSide) {return java.util.stream.IntStream.range(0, getContainerSize()).toArray();}
		@Override public boolean canPlaceItemThroughFace(int aSlot, ItemStack aStack, Direction aSide) {return canPlaceItem(aSlot, aStack);}
		@Override public boolean canTakeItemThroughFace(int aSlot, ItemStack aStack, Direction aSide) {
			try {ItemStack tOne = mHandler.extractItem(aSlot, 1, true); return tOne != null && !tOne.isEmpty();} catch (Throwable e) {return false;}
		}

		/** Adapters of one storage are one container, so a delegator holding one can tell its storage still exists. */
		@Override public boolean equals(Object aOther) {return aOther instanceof ForeignContainer tOther && tOther.mHandler == mHandler;}
		@Override public int hashCode() {return System.identityHashCode(mHandler);}
	}
}

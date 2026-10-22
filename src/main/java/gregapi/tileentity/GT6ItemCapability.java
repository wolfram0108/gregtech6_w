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
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.VanillaContainerWrapper;
import net.neoforged.neoforge.transfer.item.WorldlyContainerWrapper;
import net.neoforged.neoforge.transfer.transaction.Transaction;

/**
 * RESTORING THE STANDARD INVENTORY CHANNEL (the same loss class fluids had —
 * {@link gregapi.fluid.GT6FluidCapability}, fixed by the same technique).
 *
 * <p><b>What 1.7.10 had.</b> GT6's base TEs declared the vanilla {@code IInventory}/{@code ISidedInventory}
 * (original {@code TileEntityBase05Inventories:41}, {@code TileEntityBase06Covers:62}) — and that was enough:
 * a foreign hopper, pipe, sorter, or Waila read the machine's inventory without a single line of GT6-specific code.
 *
 * <p><b>What changed in neo.</b> The interfaces were ported 1:1 ({@code Container}/{@code WorldlyContainer}), but
 * from outside a block is only visible through a REGISTERED capability: {@code Capabilities.Item.BLOCK}.
 * No registration existed ({@code grep "Capabilities.Item"} across the port returned 0), so as far as a foreign
 * mod was concerned, GT6 machines simply had no inventory. Jade, for example, looks for content exactly this
 * way — {@code CommonProxy.findItemHandler} -> {@code level.getCapability(Capabilities.Item.BLOCK, ...)}
 * (Jade sources, branch 26.1-neoforge, {@code CommonProxy.java:290-297}).
 *
 * <p><b>There is no custom item-transfer logic here.</b> The vanilla inventory is wrapped by the engine's OWN
 * STANDARD wrappers: {@link WorldlyContainerWrapper} (side-aware — a direct analogue of 1.7.10's
 * {@code ISidedInventory}) and {@link VanillaContainerWrapper} for a foreign request with no side. The rules
 * "what can be taken from where" remain entirely GT6's own — set by its own
 * {@code getSlotsForFace/canPlaceItemThroughFace/canTakeItemThroughFace}.
 *
 * <p><b>Registration is BY BLOCK, not by {@code BlockEntityType}</b> — the same engine requirement fluids had:
 * {@code MTE_TYPE} is created with an empty {@code validBlocks}, and {@code registerBlockEntity} silently
 * does nothing for it (measurement MODCOMPAT-001 item 2).
 */
public class GT6ItemCapability {
	private GT6ItemCapability() {}

	/** Subscription on the mod bus — alongside the other central adapters in {@code GT_API.init}. */
	public static void register(IEventBus aModBus) {
		aModBus.addListener(GT6ItemCapability::onRegisterCapabilities);
	}

	private static void onRegisterCapabilities(RegisterCapabilitiesEvent aEvent) {
		net.minecraft.world.level.block.Block[] tBlocks = gregapi.block.multitileentity.MultiTileEntityBlock.allInRegistry();
		if (tBlocks.length == 0) {
			// A silent skip is not acceptable: it amounts to "no inventories from outside" with no trace in the log.
			gregapi.data.CS.ERR.println("GT6 item-capability: 0 MTE blocks in the registry — the inventory channel was NOT registered!");
			return;
		}
		aEvent.registerBlock(Capabilities.Item.BLOCK, GT6ItemCapability::handlerAt, tBlocks);
		gregapi.data.CS.OUT.println("GT6 item-capability: inventory channel registered for " + tBlocks.length + " MTE blocks (Capabilities.Item.BLOCK).");
	}

	/** The engine passes the BlockEntity itself (may be null if it does not exist yet). */
	private static ResourceHandler<ItemResource> handlerAt(net.minecraft.world.level.BlockGetter aLevel, net.minecraft.core.BlockPos aPos, net.minecraft.world.level.block.state.BlockState aState, net.minecraft.world.level.block.entity.BlockEntity aBlockEntity, Direction aSide) {
		try {
			// An empty inventory is not the same as its absence: null means "there is no channel here".
			if (aBlockEntity instanceof Container tContainer && tContainer.getContainerSize() > 0) {
				if (aBlockEntity instanceof WorldlyContainer tWorldly) {
					WorldlyContainerWrapper tWrapper = new WorldlyContainerWrapper(tWorldly, aSide);
					return aSide == null ? new SidelessView(tWrapper, tWorldly) : tWrapper;
				}
				return VanillaContainerWrapper.of(tContainer);
			}
		} catch (Throwable e) {/* a specific TE's logic must not crash a foreign mod that merely asked for the capability */}
		return null;
	}

	/**
	 * A REQUEST WITH NO SIDE ALSO FOLLOWS GT6'S OWN RULES (BUG-082).
	 *
	 * <p><b>What used to happen.</b> When {@code side == null} the channel returned {@code VanillaContainerWrapper.of(...)}
	 * — the WHOLE inventory array, bypassing the mod's filter. Service slots that GT6 does not count as items leaked
	 * outward: fluid displays ({@code MultiTileEntityBasicMachine:470-471} — {@code FL.display(...)}) and the pattern
	 * slot. Hence Jade's showcase displaying a machine's content twice: first as "items" (actually displays, in
	 * buckets), then as the real fluid list. Jade specifically asks with no side — {@code Jade/CommonProxy.java:290-297}.
	 *
	 * <p><b>Why this is a port defect, not an engine one.</b> GT6 has no concept of "an inventory with no side": it
	 * calls side 6 {@code SIDE_ANY} and answers for it itself ({@code MultiTileEntityBasicMachine.updateAccessibleSlots}
	 * fills {@code ACCESSIBLE[6]}, the default masks {@code 127} include the {@code SBIT_A=64} bit — {@code CS.java:646}).
	 * neo's {@code null} IS that side: {@code FORGE_DIR[6] = null} ({@code CS.java:687}), {@code UT.Code.side(null)=6}.
	 * The neighboring FLUID channel is already built this way — {@code GT6FluidCapability.handlerOf} reads {@code getTankInfo(null)}.
	 * The item channel was the one left out of this pair: the same task was solved in the mod by two different means.
	 *
	 * <p><b>What is here, and what is not.</b> Here there is ONLY the slot selection — taken from the mod itself
	 * ({@code getSlotsForFace(null)} -> {@code TileEntityBase06Covers:322} -> {@code getAccessibleSlotsFromSide2(6)},
	 * covers included). Transferring, transactions and stack limits remain the job of the standard {@link WorldlyContainerWrapper}
	 * — there is no private copy of the engine's mechanics here. The class's shape mirrors the engine's
	 * {@code RangedResourceHandler} ({@code size}/{@code convertIndex} plus both index-free methods by iteration),
	 * except the index set is not a range but the mod's own answer.
	 */
	private static final class SidelessView extends net.neoforged.neoforge.transfer.DelegatingResourceHandler<ItemResource> {
		private final WorldlyContainer mContainer;

		SidelessView(WorldlyContainerWrapper aDelegate, WorldlyContainer aContainer) {super(aDelegate); mContainer = aContainer;}

		/** Recomputed every time: the set of accessible slots changes live (machine rotation, an installed cover). */
		private int[] slots() {
			int[] rSlots = mContainer.getSlotsForFace(null);
			return rSlots == null ? new int[0] : rSlots;
		}

		@Override public int size() {return slots().length;}

		@Override protected int convertIndex(int aIndex) {
			int[] tSlots = slots();
			java.util.Objects.checkIndex(aIndex, tSlots.length);
			return tSlots[aIndex];
		}

		/**
		 * The engine's wrapper does NOT ask its own {@code canTakeItemThroughFace} when {@code side == null}
		 * ({@code WorldlyContainerWrapper:84-89} — the check is guarded by {@code side != null}), while GT6 requires
		 * it for side 6 too: that is also where the ban on handing out service items lives
		 * ({@code TileEntityBase06Covers:341} — {@code ST.debug(aStack) -> F}). We ask the mod's own contract, we do not copy a foreign policy.
		 */
		@Override public int extract(int aIndex, ItemResource aResource, int aAmount, net.neoforged.neoforge.transfer.transaction.TransactionContext aTransaction) {
			int tSlot = convertIndex(aIndex);
			if (!mContainer.canTakeItemThroughFace(tSlot, aResource.toStack(), null)) return 0;
			return super.extract(aIndex, aResource, aAmount, aTransaction);
		}

		@Override public int insert(ItemResource aResource, int aAmount, net.neoforged.neoforge.transfer.transaction.TransactionContext aTransaction) {
			int rInserted = 0;
			for (int i = 0, j = size(); i < j && rInserted < aAmount; i++) rInserted += insert(i, aResource, aAmount - rInserted, aTransaction);
			return rInserted;
		}

		@Override public int extract(ItemResource aResource, int aAmount, net.neoforged.neoforge.transfer.transaction.TransactionContext aTransaction) {
			int rExtracted = 0;
			for (int i = 0, j = size(); i < j && rExtracted < aAmount; i++) rExtracted += extract(i, aResource, aAmount - rExtracted, aTransaction);
			return rExtracted;
		}
	}

	// ==============================================================================================
	// The other way round: another mod's item storage as GT6 code meets it next to a GT6 block.
	// ==============================================================================================

	/** Cache of another mod's item storages per position and side, the same rules as for fluids (GT6ForeignCapabilityCache). */
	private static final GT6ForeignCapabilityCache<ResourceHandler<ItemResource>, ForeignContainer> FOREIGN = new GT6ForeignCapabilityCache<>(Capabilities.Item.BLOCK, ForeignContainer::new);

	/**
	 * Another mod's item storage on a block entity's side, as GT6 code sees one: a WorldlyContainer, through ForeignContainer.
	 * 1.7.10's foreign storages were IInventory block entities, so every mod's chest was reached by GT6's hoppers, pipes and machines
	 * without a line of mod-specific code; the engine's storages need not be a Container, only to answer the item capability. A block that
	 * is a Container itself needs no adapter, and one without storage stays out, at no cost.
	 */
	public static ForeignContainer foreign(BlockEntity aTileEntity, byte aSide) {
		if (aTileEntity instanceof Container) return null;
		ForeignContainer rForeign = FOREIGN.get(aTileEntity, aSide);
		return rForeign != null && rForeign.getContainerSize() > 0 ? rForeign : null;
	}

	/**
	 * The engine's item handler of another mod's block as the Container GT6's transfer code is written against. Stacks it hands out are
	 * snapshots (a foreign storage never lets its own be changed in place), every change goes through the handler in a transaction nested
	 * in the open one if there is one, the rules of what can go where stay the storage's own, and a storage that fails moves nothing
	 * instead of erroring a GT6 tick.
	 */
	public static final class ForeignContainer implements WorldlyContainer {
		private final ResourceHandler<ItemResource> mHandler;
		private ForeignContainer(ResourceHandler<ItemResource> aHandler) {mHandler = aHandler;}

		@Override public int getContainerSize() {try {return mHandler.size();} catch (Throwable e) {return 0;}}
		@Override public boolean isEmpty() {for (int i = 0, j = getContainerSize(); i < j; i++) if (!getItem(i).isEmpty()) return false; return true;}
		@Override public ItemStack getItem(int aSlot) {
			try {
				ItemResource tResource = mHandler.getResource(aSlot);
				int tAmount = mHandler.getAmountAsInt(aSlot);
				return tResource.isEmpty() || tAmount <= 0 ? ItemStack.EMPTY : tResource.toStack(tAmount);
			} catch (Throwable e) {return ItemStack.EMPTY;}
		}
		@Override public ItemStack removeItem(int aSlot, int aCount) {
			if (aCount <= 0) return ItemStack.EMPTY;
			try {
				ItemResource tResource = mHandler.getResource(aSlot);
				if (tResource.isEmpty()) return ItemStack.EMPTY;
				try (Transaction tTx = Transaction.open(Transaction.getCurrentOpenedTransaction())) {
					int tTaken = mHandler.extract(aSlot, tResource, aCount, tTx);
					if (tTaken <= 0) return ItemStack.EMPTY;
					tTx.commit();
					return tResource.toStack(tTaken);
				}
			} catch (Throwable e) {return ItemStack.EMPTY;}
		}
		@Override public ItemStack removeItemNoUpdate(int aSlot) {
			try {return removeItem(aSlot, Math.max(1, mHandler.getAmountAsInt(aSlot)));} catch (Throwable e) {return ItemStack.EMPTY;}
		}
		/** Puts a stack into one slot and reports how much of it the storage took; the rest stays with the caller. */
		public int put(int aSlot, ItemStack aStack) {
			if (aStack == null || aStack.isEmpty()) return 0;
			try (Transaction tTx = Transaction.open(Transaction.getCurrentOpenedTransaction())) {
				int rPut = mHandler.insert(aSlot, ItemResource.of(aStack), aStack.getCount(), tTx);
				if (rPut <= 0) return 0;
				tTx.commit();
				return rPut;
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
			try (Transaction tTx = Transaction.open(Transaction.getCurrentOpenedTransaction())) {
				ItemResource tResource = ItemResource.of(aStack);
				// the question is asked of the storage itself: simulated, never committed
				return mHandler.isValid(aSlot, tResource) && mHandler.insert(aSlot, tResource, 1, tTx) > 0;
			} catch (Throwable e) {return false;}
		}

		@Override public int[] getSlotsForFace(Direction aSide) {return java.util.stream.IntStream.range(0, getContainerSize()).toArray();}
		@Override public boolean canPlaceItemThroughFace(int aSlot, ItemStack aStack, Direction aSide) {return canPlaceItem(aSlot, aStack);}
		@Override public boolean canTakeItemThroughFace(int aSlot, ItemStack aStack, Direction aSide) {
			try (Transaction tTx = Transaction.open(Transaction.getCurrentOpenedTransaction())) {
				ItemResource tResource = mHandler.getResource(aSlot);
				return !tResource.isEmpty() && mHandler.extract(aSlot, tResource, 1, tTx) > 0;
			} catch (Throwable e) {return false;}
		}

		/** Adapters of one storage are one container, so a delegator holding one can tell its storage still exists. */
		@Override public boolean equals(Object aOther) {return aOther instanceof ForeignContainer tOther && tOther.mHandler == mHandler;}
		@Override public int hashCode() {return System.identityHashCode(mHandler);}
	}
}

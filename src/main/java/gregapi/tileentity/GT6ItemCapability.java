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

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.WorldlyContainer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.VanillaContainerWrapper;
import net.neoforged.neoforge.transfer.item.WorldlyContainerWrapper;

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
		List<net.minecraft.world.level.block.Block> tBlocks = new ArrayList<>();
		for (net.minecraft.world.level.block.Block tBlock : net.minecraft.core.registries.BuiltInRegistries.BLOCK) {
			if (tBlock instanceof gregapi.block.multitileentity.MultiTileEntityBlock || tBlock instanceof gregapi.block.multitileentity.MultiTileEntityBlockInternal) tBlocks.add(tBlock);
		}
		if (tBlocks.isEmpty()) {
			// A silent skip is not acceptable: it amounts to "no inventories from outside" with no trace in the log.
			gregapi.data.CS.ERR.println("GT6 item-capability: 0 MTE blocks in the registry — the inventory channel was NOT registered!");
			return;
		}
		aEvent.registerBlock(Capabilities.Item.BLOCK, GT6ItemCapability::handlerAt, tBlocks.toArray(new net.minecraft.world.level.block.Block[0]));
		gregapi.data.CS.OUT.println("GT6 item-capability: inventory channel registered for " + tBlocks.size() + " MTE blocks (Capabilities.Item.BLOCK).");
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
	 * The neighboring FLUID channel is already built this way — {@code TileEntityBase01Root.getFluidTanksForCapability:766}.
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
}

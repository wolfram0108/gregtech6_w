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

package gregapi.fluid;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.Direction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/**
 * MODCOMPAT-001 P2 — RESTORES THE STANDARD FLUID CHANNEL FOR BLOCKS (F5-capability).
 *
 * <p><b>What was lost during the port.</b> In 1.7.10, GT6's tanks were exposed externally through the
 * STANDARD Forge interface: five TE hierarchies declared {@code implements IFluidHandler}
 * ({@code tank/TileEntityBase08Barrel}, {@code connectors/MultiTileEntityPipeFluid},
 * {@code machines/MultiTileEntityBasicMachine}, {@code multiblocks/MultiTileEntityMultiBlockPart},
 * {@code tools/MultiTileEntityAdvancedCraftingTable}), and any foreign mod — a pump, a pipe, a tooltip mod —
 * read the contents WITHOUT a single line of GT6-specific code. In neo the interface on a BlockEntity no
 * longer means anything by itself: only what is REGISTERED as a capability is visible externally. The port
 * carried the interface over verbatim (the methods live in {@code TileEntityBase01Root:809-817}), but never
 * registered it — a {@code grep} for {@code RegisterCapabilitiesEvent} returned 0. Result: GT6's tanks don't
 * exist from the outside.
 *
 * <p><b>Why this is one spot, not five classes to patch.</b> The whole GT6 TE hierarchy lives under ONE
 * {@code BlockEntityType} — {@code TileEntityBase01Root.MTE_TYPE} (a shared placeholder type, F-tileentity-
 * construction). So registration is exactly one call too: the TE answers through its side-aware {@code getTankInfo},
 * {@code canFill}, {@code fill} and {@code drain}, which multiblock walls, pipes and extenders override.
 * Cover overrides ({@code TileEntityBase06Covers:375}) are honored automatically as a result.
 *
 * <p><b>Transactionality.</b> Every {@link FluidTankGT} hands out a {@code ResourceHandler<FluidResource>}
 * ({@link FluidTankGT#asResourceHandler}) with
 * neo's snapshot/rollback semantics, but a TE's own fill/drain is not transactional: {@link SidedTankView} sizes a move
 * with a simulated fill/drain and applies it once the root transaction commits, so an aborted one changes nothing.
 */
public class GT6FluidCapability {
	private GT6FluidCapability() {}

	/** Subscribes to the mod bus — next to the other central adapters in {@code GT_API.init}. */
	public static void register(IEventBus aModBus) {
		aModBus.addListener(GT6FluidCapability::onRegisterCapabilities);
	}

	/**
	 * Registration goes BY BLOCK, not by {@code BlockEntityType}, and that's not a stylistic choice but an
	 * engine requirement: {@code registerBlockEntity} hands the provider to blocks from
	 * {@code BlockEntityType.validBlocks}, and {@code MTE_TYPE} is created with an EMPTY set
	 * ({@code TileEntityBase01Root.createType()}: {@code java.util.Set.<Block>of()}) — it is the shared
	 * placeholder type of GT6's dynamic hierarchy. A measurement confirmed this: registration went through,
	 * the BE type matched {@code MTE_TYPE}, the tank was in place — yet the capability still resolved to
	 * {@code null}. So we enumerate the blocks themselves: everything whose BlockEntity is a GT6 root.
	 */
	private static void onRegisterCapabilities(RegisterCapabilitiesEvent aEvent) {
		net.minecraft.world.level.block.Block[] tBlocks = gregapi.block.multitileentity.MultiTileEntityBlock.allInRegistry();
		if (tBlocks.length == 0) {
			// Can't silently skip this: a silent skip means "no tanks exposed" with zero trace in the log.
			gregapi.data.CS.ERR.println("GT6 F5-capability: 0 MTE blocks in the registry — the fluid channel was NOT registered!");
			return;
		}
		aEvent.registerBlock(Capabilities.Fluid.BLOCK, GT6FluidCapability::handlerAt, tBlocks);
		gregapi.data.CS.OUT.println("GT6 F5-capability: fluid channel registered for " + tBlocks.length + " MTE blocks (Capabilities.Fluid.BLOCK).");
		// Second half of the same class: 1.7.10 ITEM-side interface IFluidContainerItem is alive 1:1 on the
		// items (BUG-045) but was never registered as Capabilities.Fluid.ITEM — container items looked empty
		// to JEI and other mods. One adapter bridges the GT6 channel; items enumerated by the same rule.
		List<net.minecraft.world.item.Item> tItems = new ArrayList<>();
		for (net.minecraft.world.item.Item tItem : net.minecraft.core.registries.BuiltInRegistries.ITEM)
			if (tItem instanceof gt6mirror.net.minecraftforge.fluids.IFluidContainerItem) tItems.add(tItem);
		if (!tItems.isEmpty()) {
			aEvent.registerItem(Capabilities.Fluid.ITEM,
				(aStack, aAccess) -> aStack.getItem() instanceof gt6mirror.net.minecraftforge.fluids.IFluidContainerItem ? new GT6ItemFluidHandler(aAccess) : null,
				tItems.toArray(new net.minecraft.world.level.ItemLike[0]));
			gregapi.data.CS.OUT.println("GT6 F5-capability: item fluid channel registered for " + tItems.size() + " items (Capabilities.Fluid.ITEM).");
		}
	}

	/** Item side: bridges the live 1.7.10 IFluidContainerItem bodies (getFluid/getCapacity/fill/drain mutate
	 *  the stack NBT) into the neo transactional contract — same "one adapter over the GT6 channel" approach
	 *  as handlerOf() above. update() rebuilds a stack via the GT6 channel so NBT stays the single format. */
	private static final class GT6ItemFluidHandler extends net.neoforged.neoforge.transfer.ItemAccessResourceHandler<FluidResource> {
		private GT6ItemFluidHandler(net.neoforged.neoforge.transfer.access.ItemAccess aAccess) {super(aAccess, 1);}

		private static net.neoforged.neoforge.fluids.FluidStack fluidOf(net.neoforged.neoforge.transfer.item.ItemResource aResource) {
			net.minecraft.world.item.ItemStack tStack = aResource.toStack(1);
			return tStack.getItem() instanceof gt6mirror.net.minecraftforge.fluids.IFluidContainerItem tItem ? tItem.getFluid(tStack) : null;
		}

		@Override protected FluidResource getResourceFrom(net.neoforged.neoforge.transfer.item.ItemResource aResource, int aIndex) {
			net.neoforged.neoforge.fluids.FluidStack tFluid = fluidOf(aResource);
			return tFluid == null || tFluid.isEmpty() ? FluidResource.EMPTY : FluidResource.of(tFluid);
		}

		@Override protected int getAmountFrom(net.neoforged.neoforge.transfer.item.ItemResource aResource, int aIndex) {
			net.neoforged.neoforge.fluids.FluidStack tFluid = fluidOf(aResource);
			return tFluid == null ? 0 : tFluid.getAmount();
		}

		@Override protected int getCapacity(int aIndex, FluidResource aResource) {
			net.minecraft.world.item.ItemStack tStack = itemAccess.getResource().toStack(1);
			return tStack.getItem() instanceof gt6mirror.net.minecraftforge.fluids.IFluidContainerItem tItem ? tItem.getCapacity(tStack) : 0;
		}

		@Override protected net.neoforged.neoforge.transfer.item.ItemResource update(net.neoforged.neoforge.transfer.item.ItemResource aResource, int aIndex, FluidResource aNewResource, int aNewAmount) {
			net.minecraft.world.item.ItemStack tStack = aResource.toStack(1);
			if (!(tStack.getItem() instanceof gt6mirror.net.minecraftforge.fluids.IFluidContainerItem tItem)) return null;
			tItem.drain(tStack, Integer.MAX_VALUE, true);
			if (aNewAmount > 0 && tItem.fill(tStack, aNewResource.toStack(aNewAmount), true) != aNewAmount) return null;
			return net.neoforged.neoforge.transfer.item.ItemResource.of(tStack);
		}
	}

	/** Block variant of the provider: the engine passes the BlockEntity itself (can be null if it doesn't exist yet). */
	private static ResourceHandler<FluidResource> handlerAt(net.minecraft.world.level.BlockGetter aLevel, net.minecraft.core.BlockPos aPos, net.minecraft.world.level.block.state.BlockState aState, net.minecraft.world.level.block.entity.BlockEntity aBlockEntity, Direction aSide) {
		return aBlockEntity instanceof gregapi.tileentity.base.TileEntityBase01Root tRoot ? handlerOf(tRoot, aSide) : null;
	}

	/**
	 * Tanks visible from the outside on the given side. {@code aSide == null} is a sideless request, GT6's
	 * native {@code SIDE_ANY} convention (the same one used by the inherited bridges in {@code TileEntityBase01Root}).
	 * A {@code null} return means "there's no capability here" — that's how neo tells a block with no tanks
	 * apart from an empty tank.
	 */
	private static ResourceHandler<FluidResource> handlerOf(gregapi.tileentity.base.TileEntityBase01Root aTileEntity, Direction aSide) {
		if (aTileEntity == null) return null;
		try {
			gregapi.fluid.FluidTankInfo[] tInfo = aTileEntity.getTankInfo(aSide);
			if (tInfo == null || tInfo.length <= 0) return null;
		} catch (Throwable e) {return null;} // a specific TE's logic must not crash a foreign mod that just asked for the capability
		return new SidedTankView(aTileEntity, aSide);
	}

	/** A side of a GT6 tile entity as 1.7.10's IFluidHandler showed it, and as the 1.20.1 branch's SidedTankView does: read
	 *  through getTankInfo, filled and drained only through the TE's own canFill/fill/drain. Moves wait for the root commit. */
	private static final class SidedTankView extends SnapshotJournal<List<FluidStack>> implements ResourceHandler<FluidResource> {
		private final gregapi.tileentity.base.TileEntityBase01Root mTileEntity;
		private final Direction mSide;
		/** Moves of the open transactions: a positive amount fills, a negative one drains. */
		private List<FluidStack> mPending = new ArrayList<>();

		SidedTankView(gregapi.tileentity.base.TileEntityBase01Root aTileEntity, Direction aSide) {mTileEntity = aTileEntity; mSide = aSide;}

		private gregapi.fluid.FluidTankInfo[] info() {
			try {gregapi.fluid.FluidTankInfo[] rInfo = mTileEntity.getTankInfo(mSide); return rInfo == null ? gregapi.data.CS.ZL_FLUIDTANKINFO : rInfo;} catch (Throwable e) {return gregapi.data.CS.ZL_FLUIDTANKINFO;}
		}
		private FluidStack stack(int aIndex) {
			gregapi.fluid.FluidTankInfo[] tInfo = info();
			return aIndex >= 0 && aIndex < tInfo.length && tInfo[aIndex] != null && tInfo[aIndex].fluid != null ? tInfo[aIndex].fluid : FluidStack.EMPTY;
		}
		/** What this transaction already moves of the fluid in one direction, so a second request is sized on top of it. */
		private int pending(FluidResource aResource, boolean aFill) {
			int r = 0;
			for (FluidStack tMove : mPending) if ((tMove.getAmount() > 0) == aFill && aResource.matches(tMove)) r += Math.abs(tMove.getAmount());
			return r;
		}

		@Override public int size() {return Math.max(1, info().length);}
		@Override public FluidResource getResource(int aIndex) {return FluidResource.of(stack(aIndex));}
		@Override public long getAmountAsLong(int aIndex) {return stack(aIndex).getAmount();}
		@Override public long getCapacityAsLong(int aIndex, FluidResource aResource) {gregapi.fluid.FluidTankInfo[] tInfo = info(); return aIndex >= 0 && aIndex < tInfo.length && tInfo[aIndex] != null ? tInfo[aIndex].capacity : 0;}
		@Override public boolean isValid(int aIndex, FluidResource aResource) {
			if (aResource == null || aResource.isEmpty()) return false;
			try {return mTileEntity.canFill(mSide, aResource.getFluid());} catch (Throwable e) {return false;}
		}

		@Override public int insert(int aIndex, FluidResource aResource, int aAmount, TransactionContext aTx) {
			if (aResource == null || aResource.isEmpty() || aAmount <= 0) return 0;
			int tBefore = pending(aResource, true), rTaken;
			try {rTaken = Math.min(aAmount, mTileEntity.fill(mSide, aResource.toStack(tBefore + aAmount), false) - tBefore);} catch (Throwable e) {return 0;}
			if (rTaken <= 0) return 0;
			updateSnapshots(aTx);
			mPending.add(aResource.toStack(rTaken));
			return rTaken;
		}
		@Override public int extract(int aIndex, FluidResource aResource, int aAmount, TransactionContext aTx) {
			if (aResource == null || aResource.isEmpty() || aAmount <= 0) return 0;
			int tBefore = pending(aResource, false), rTaken;
			try {FluidStack tDrained = mTileEntity.drain(mSide, aResource.toStack(tBefore + aAmount), false); rTaken = Math.min(aAmount, (tDrained == null ? 0 : tDrained.getAmount()) - tBefore);} catch (Throwable e) {return 0;}
			if (rTaken <= 0) return 0;
			updateSnapshots(aTx);
			FluidStack tMove = aResource.toStack(rTaken); tMove.setAmount(-rTaken);
			mPending.add(tMove);
			return rTaken;
		}

		@Override protected List<FluidStack> createSnapshot() {return new ArrayList<>(mPending);}
		@Override protected void revertToSnapshot(List<FluidStack> aSnapshot) {mPending = aSnapshot;}
		@Override protected void onRootCommit(List<FluidStack> aOriginal) {
			List<FluidStack> tMoves = mPending; mPending = new ArrayList<>();
			for (FluidStack tMove : tMoves) try {
				if (tMove.getAmount() > 0) mTileEntity.fill(mSide, tMove, true); else {FluidStack tDrain = tMove.copy(); tDrain.setAmount(-tMove.getAmount()); mTileEntity.drain(mSide, tDrain, true);}
			} catch (Throwable e) {e.printStackTrace(gregapi.data.CS.ERR);}
		}
	}

	// ==============================================================================================
	// The other way round: another mod's fluid storage as GT6 code meets it next to a GT6 block.
	// ==============================================================================================

	/** Answers per position and side for another mod's block entity, each kept until the engine reports that block's capabilities
	 *  changed (replaced, removed, unloaded, reconfigured), then dropped: one map lookup on the hot path, nothing held past its
	 *  block, no stale answer. Server thread only; any other thread asks the engine directly. */
	private static final java.util.Map<net.minecraft.world.level.Level, it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap<Foreign[]>> FOREIGN = new java.util.WeakHashMap<>();

	private static final class Foreign {
		private final net.neoforged.neoforge.capabilities.BlockCapabilityCache<ResourceHandler<FluidResource>, Direction> mCache;
		private ResourceHandler<FluidResource> mLast;
		private IFluidHandler mTank;
		private Foreign(net.minecraft.server.level.ServerLevel aLevel, net.minecraft.core.BlockPos aPos, byte aSide, Runnable aDrop) {
			mCache = net.neoforged.neoforge.capabilities.BlockCapabilityCache.create(Capabilities.Fluid.BLOCK, aLevel, aPos, gregapi.data.CS.FORGE_DIR[aSide], () -> true, aDrop);
		}
		/** The same adapter object while the storage stays the same, so a delegator holding it can tell it still exists. */
		private IFluidHandler get() {
			ResourceHandler<FluidResource> tCap = mCache.getCapability();
			if (tCap != mLast) {mLast = tCap; mTank = tCap == null ? null : IFluidHandler.of(tCap);}
			return mTank;
		}
	}

	/** Another mod's fluid storage on a block entity's side, as GT6 code sees one (an IFluidHandler, through NeoForge's own
	 *  adapter). 1.7.10's foreign tanks were IFluidHandler block entities too, so a block without one stays out, at no cost. */
	public static IFluidHandler foreign(net.minecraft.world.level.block.entity.BlockEntity aTileEntity, byte aSide) {
		net.minecraft.world.level.Level tLevel = aTileEntity.getLevel();
		if (tLevel == null || aTileEntity.isRemoved()) return null;
		if (!(tLevel instanceof net.minecraft.server.level.ServerLevel tServer) || !tServer.getServer().isSameThread()) {
			ResourceHandler<FluidResource> tCap = tLevel.getCapability(Capabilities.Fluid.BLOCK, aTileEntity.getBlockPos(), aTileEntity.getBlockState(), aTileEntity, gregapi.data.CS.FORGE_DIR[aSide]);
			return tCap == null ? null : IFluidHandler.of(tCap);
		}
		long tPos = aTileEntity.getBlockPos().asLong();
		int tSide = gregapi.data.CS.SIDES_VALID[aSide] ? aSide : 6;
		it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap<Foreign[]> tMap = FOREIGN.computeIfAbsent(tLevel, k -> new it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap<>());
		Foreign[] tSides = tMap.get(tPos);
		if (tSides == null) tMap.put(tPos, tSides = new Foreign[7]);
		Foreign tEntry = tSides[tSide];
		if (tEntry == null) {
			Foreign[] tOwner = tSides;
			tSides[tSide] = tEntry = new Foreign(tServer, aTileEntity.getBlockPos(), aSide, () -> drop(tMap, tPos, tOwner, tSide));
		}
		return tEntry.get();
	}

	private static void drop(it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap<Foreign[]> aMap, long aPos, Foreign[] aSides, int aSide) {
		aSides[aSide] = null;
		for (Foreign tEntry : aSides) if (tEntry != null) return;
		if (aMap.get(aPos) == aSides) aMap.remove(aPos);
	}
}

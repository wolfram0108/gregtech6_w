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
import net.neoforged.neoforge.fluids.IFluidTank;
import net.neoforged.neoforge.transfer.CombinedResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;

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
 * construction). So registration is exactly one call too, and which tanks to expose is decided by the TE
 * itself through its side-aware {@code getFluidTanks(side)} — the same code as the internal transfer path.
 * Cover overrides ({@code TileEntityBase06Covers:375}) are honored automatically as a result.
 *
 * <p><b>Transactionality is off-the-shelf, not homegrown.</b> Every {@link FluidTankGT} already knows how to
 * hand out a correct {@code ResourceHandler<FluidResource>} ({@link FluidTankGT#asResourceHandler}) with
 * neo's snapshot/rollback semantics; several of a side's tanks are glued together with the stock
 * {@link CombinedResourceHandler}. There is no custom transactional logic here — only tank selection.
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
		List<net.minecraft.world.level.block.Block> tBlocks = new ArrayList<>();
		for (net.minecraft.world.level.block.Block tBlock : net.minecraft.core.registries.BuiltInRegistries.BLOCK) {
			if (tBlock instanceof gregapi.block.multitileentity.MultiTileEntityBlock || tBlock instanceof gregapi.block.multitileentity.MultiTileEntityBlockInternal) tBlocks.add(tBlock);
		}
		if (tBlocks.isEmpty()) {
			// Can't silently skip this: a silent skip means "no tanks exposed" with zero trace in the log.
			gregapi.data.CS.ERR.println("GT6 F5-capability: 0 MTE blocks in the registry — the fluid channel was NOT registered!");
			return;
		}
		aEvent.registerBlock(Capabilities.Fluid.BLOCK, GT6FluidCapability::handlerAt, tBlocks.toArray(new net.minecraft.world.level.block.Block[0]));
		gregapi.data.CS.OUT.println("GT6 F5-capability: fluid channel registered for " + tBlocks.size() + " MTE blocks (Capabilities.Fluid.BLOCK).");
		// Second half of the same class: 1.7.10 ITEM-side interface IFluidContainerItem is alive 1:1 on the
		// items (BUG-045) but was never registered as Capabilities.Fluid.ITEM — container items looked empty
		// to JEI and other mods. One adapter bridges the GT6 channel; items enumerated by the same rule.
		List<net.minecraft.world.item.Item> tItems = new ArrayList<>();
		for (net.minecraft.world.item.Item tItem : net.minecraft.core.registries.BuiltInRegistries.ITEM)
			if (tItem instanceof net.minecraftforge.fluids.IFluidContainerItem) tItems.add(tItem);
		if (!tItems.isEmpty()) {
			aEvent.registerItem(Capabilities.Fluid.ITEM,
				(aStack, aAccess) -> aStack.getItem() instanceof net.minecraftforge.fluids.IFluidContainerItem ? new GT6ItemFluidHandler(aAccess) : null,
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
			return tStack.getItem() instanceof net.minecraftforge.fluids.IFluidContainerItem tItem ? tItem.getFluid(tStack) : null;
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
			return tStack.getItem() instanceof net.minecraftforge.fluids.IFluidContainerItem tItem ? tItem.getCapacity(tStack) : 0;
		}

		@Override protected net.neoforged.neoforge.transfer.item.ItemResource update(net.neoforged.neoforge.transfer.item.ItemResource aResource, int aIndex, FluidResource aNewResource, int aNewAmount) {
			net.minecraft.world.item.ItemStack tStack = aResource.toStack(1);
			if (!(tStack.getItem() instanceof net.minecraftforge.fluids.IFluidContainerItem tItem)) return null;
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
		IFluidTank[] tTanks;
		try {
			tTanks = aTileEntity.getFluidTanksForCapability(aSide);
		} catch (Throwable e) {return null;} // a specific TE's logic must not crash a foreign mod that just asked for the capability
		if (tTanks == null || tTanks.length <= 0) return null;
		List<ResourceHandler<FluidResource>> rHandlers = new ArrayList<>(tTanks.length);
		for (IFluidTank tTank : tTanks) if (tTank instanceof FluidTankGT tGT) rHandlers.add(tGT.asResourceHandler());
		if (rHandlers.isEmpty()) return null;
		return rHandlers.size() == 1 ? rHandlers.get(0) : new CombinedResourceHandler<>(rHandlers);
	}
}

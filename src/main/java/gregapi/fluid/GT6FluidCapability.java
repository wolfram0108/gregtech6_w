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

import gregapi.tileentity.base.TileEntityBase01Root;
import net.minecraft.core.Direction;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

/** Side comes back into the contract here, restoring 1.7.10's form rather than a new one: 1.7.10's IFluidHandler had
 *  six side-taking methods, 1.20.1's is sideless, so the requested capability's own side plays that role instead. */
public final class GT6FluidCapability {
	private GT6FluidCapability() {}

	/** Same predicate as before (no tanks means no capability), asked through getTankInfo(side) so overrides still apply. */
	public static boolean hasTanks(TileEntityBase01Root aTileEntity, Direction aSide) {
		if (aTileEntity == null) return false;
		try {
			gregapi.fluid.FluidTankInfo[] tInfo = aTileEntity.getTankInfo(aSide);
			return tInfo != null && tInfo.length > 0;
		} catch (Throwable e) {return false;} // A specific TE's own logic must not crash another mod that simply asked for this capability.
	}

	/** The handler is bound to whichever side was requested; null means a sideless request, GT6's own SIDE_ANY. */
	public static IFluidHandler handlerOf(TileEntityBase01Root aTileEntity, Direction aSide) {
		return new SidedTankView(aTileEntity, aSide);
	}

	/** This is the literal 1.20.1 equivalent of the 1.7.10 TE itself; exceptions are swallowed for the same reason as hasTanks. */
	private static final class SidedTankView implements IFluidHandler {
		private final TileEntityBase01Root mTileEntity;
		private final Direction mSide;

		SidedTankView(TileEntityBase01Root aTileEntity, Direction aSide) {mTileEntity = aTileEntity; mSide = aSide;}

		private gregapi.fluid.FluidTankInfo[] info() {
			try {
				gregapi.fluid.FluidTankInfo[] rInfo = mTileEntity.getTankInfo(mSide);
				return rInfo == null ? gregapi.data.CS.ZL_FLUIDTANKINFO : rInfo;
			} catch (Throwable e) {return gregapi.data.CS.ZL_FLUIDTANKINFO;}
		}

		@Override public int getTanks() {return info().length;}

		@Override public FluidStack getFluidInTank(int aTank) {
			gregapi.fluid.FluidTankInfo[] tInfo = info();
			return aTank >= 0 && aTank < tInfo.length && tInfo[aTank] != null && tInfo[aTank].fluid != null ? tInfo[aTank].fluid : FluidStack.EMPTY;
		}

		@Override public int getTankCapacity(int aTank) {
			gregapi.fluid.FluidTankInfo[] tInfo = info();
			return aTank >= 0 && aTank < tInfo.length && tInfo[aTank] != null ? tInfo[aTank].capacity : 0;
		}

		@Override public boolean isFluidValid(int aTank, FluidStack aFluid) {
			if (aFluid == null || aFluid.isEmpty()) return false;
			try {return mTileEntity.canFill(mSide, aFluid.getFluid());} catch (Throwable e) {return false;}
		}

		@Override public int fill(FluidStack aResource, FluidAction aAction) {
			if (aResource == null || aResource.isEmpty()) return 0;
			try {return mTileEntity.fill(mSide, aResource, aAction.execute());} catch (Throwable e) {return 0;}
		}

		/** 1.20.1's IFluidHandler.drain contract requires a non-null stack, so EMPTY stands in for null. */
		@Override public FluidStack drain(FluidStack aResource, FluidAction aAction) {
			if (aResource == null || aResource.isEmpty()) return FluidStack.EMPTY;
			try {FluidStack rDrained = mTileEntity.drain(mSide, aResource, aAction.execute()); return rDrained == null ? FluidStack.EMPTY : rDrained;} catch (Throwable e) {return FluidStack.EMPTY;}
		}

		@Override public FluidStack drain(int aMaxDrain, FluidAction aAction) {
			if (aMaxDrain <= 0) return FluidStack.EMPTY;
			try {FluidStack rDrained = mTileEntity.drain(mSide, aMaxDrain, aAction.execute()); return rDrained == null ? FluidStack.EMPTY : rDrained;} catch (Throwable e) {return FluidStack.EMPTY;}
		}
	}

	// ==============================================================================================
	// ITEM arm (BUG-145 mirror): the live 1.7.10 IFluidContainerItem contract of GT6 items exposed
	// as ForgeCapabilities.FLUID_HANDLER_ITEM — what JEI, Jade and other mods read off an ItemStack.
	// ==============================================================================================

	private static final net.minecraft.resources.ResourceLocation ITEM_CAP_ID =
		new net.minecraft.resources.ResourceLocation("gregapi", "fluid_container_item");

	/** One listener for the whole mod (same seam as EntityFoodTracker.register / PrefixBlockOreMap):
	 *  every stack whose item still carries the live 1.7.10 IFluidContainerItem contract gets the
	 *  1.20.1 item capability; NBT stays the only storage format, the adapter below owns no state. */
	public static void registerItemCapabilities() {
		net.minecraftforge.common.MinecraftForge.EVENT_BUS.addGenericListener(net.minecraft.world.item.ItemStack.class,
			(net.minecraftforge.event.AttachCapabilitiesEvent<net.minecraft.world.item.ItemStack> aEvent) -> {
				net.minecraft.world.item.ItemStack tStack = aEvent.getObject();
				if (tStack.getItem() instanceof gt6mirror.minecraftforge.fluids.IFluidContainerItem tItem)
					aEvent.addCapability(ITEM_CAP_ID, new ItemProvider(tStack, tItem));
			});
	}

	private static final class ItemProvider implements net.minecraftforge.common.capabilities.ICapabilityProvider {
		private final net.minecraftforge.common.util.LazyOptional<net.minecraftforge.fluids.capability.IFluidHandlerItem> mHandler;
		ItemProvider(net.minecraft.world.item.ItemStack aStack, gt6mirror.minecraftforge.fluids.IFluidContainerItem aItem) {
			mHandler = net.minecraftforge.common.util.LazyOptional.of(() -> new GT6ItemFluidHandler(aStack, aItem));
		}
		@Override public <T> net.minecraftforge.common.util.LazyOptional<T> getCapability(net.minecraftforge.common.capabilities.Capability<T> aCapability, Direction aSide) {
			return aCapability == net.minecraftforge.common.capabilities.ForgeCapabilities.FLUID_HANDLER_ITEM ? mHandler.cast() : net.minecraftforge.common.util.LazyOptional.empty();
		}
	}

	/** Thin bridge IFluidContainerItem -> IFluidHandlerItem: all tank logic stays with the GT6 item
	 *  (getFluid/getCapacity/fill/drain over the stack's NBT), 1.7.10 null becomes 1.20.1 EMPTY. */
	private static final class GT6ItemFluidHandler implements net.minecraftforge.fluids.capability.IFluidHandlerItem {
		private final net.minecraft.world.item.ItemStack mStack;
		private final gt6mirror.minecraftforge.fluids.IFluidContainerItem mItem;
		GT6ItemFluidHandler(net.minecraft.world.item.ItemStack aStack, gt6mirror.minecraftforge.fluids.IFluidContainerItem aItem) {mStack = aStack; mItem = aItem;}
		@Override public net.minecraft.world.item.ItemStack getContainer() {return mStack;}
		@Override public int getTanks() {return 1;}
		@Override public FluidStack getFluidInTank(int aTank) {
			try {FluidStack rFluid = mItem.getFluid(mStack); return rFluid == null ? FluidStack.EMPTY : rFluid.copy();} catch (Throwable e) {return FluidStack.EMPTY;}
		}
		@Override public int getTankCapacity(int aTank) {
			try {return mItem.getCapacity(mStack);} catch (Throwable e) {return 0;}
		}
		@Override public boolean isFluidValid(int aTank, FluidStack aFluid) {return aFluid != null && !aFluid.isEmpty();}
		@Override public int fill(FluidStack aResource, FluidAction aAction) {
			if (aResource == null || aResource.isEmpty()) return 0;
			try {return mItem.fill(mStack, aResource, aAction.execute());} catch (Throwable e) {return 0;}
		}
		@Override public FluidStack drain(FluidStack aResource, FluidAction aAction) {
			if (aResource == null || aResource.isEmpty()) return FluidStack.EMPTY;
			try {
				FluidStack tHeld = mItem.getFluid(mStack);
				if (tHeld == null || !tHeld.isFluidEqual(aResource)) return FluidStack.EMPTY;
				FluidStack rDrained = mItem.drain(mStack, aResource.getAmount(), aAction.execute());
				return rDrained == null ? FluidStack.EMPTY : rDrained;
			} catch (Throwable e) {return FluidStack.EMPTY;}
		}
		@Override public FluidStack drain(int aMaxDrain, FluidAction aAction) {
			if (aMaxDrain <= 0) return FluidStack.EMPTY;
			try {FluidStack rDrained = mItem.drain(mStack, aMaxDrain, aAction.execute()); return rDrained == null ? FluidStack.EMPTY : rDrained;} catch (Throwable e) {return FluidStack.EMPTY;}
		}
	}
}

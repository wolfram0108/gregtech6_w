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
 *
 * Modified in 2026 for the GregTech 6 NeoForge port
 * (https://github.com/wolfram0108/gregtech6_w): ported from Minecraft 1.7.10 / Forge
 * to Minecraft 26.1.2 / NeoForge.
 */

package gregtech.tileentity.extenders;

import gregapi.GT_API;
import gregapi.block.multitileentity.IMultiTileEntity.IMTE_AddToolTips;
import gregapi.block.multitileentity.IMultiTileEntity.IMTE_GetComparatorInputOverride;
import gregapi.block.multitileentity.MultiTileEntityRegistry;
import gregapi.data.FL;
import gregapi.data.LH;
import gregapi.data.LH.Chat;
import gregapi.old.Textures;
import gregapi.render.BlockTextureDefault;
import gregapi.render.BlockTextureMulti;
import gregapi.render.IIconContainer;
import gregapi.render.ITexture;
import gregapi.tileentity.ITileEntityAdjacentInventoryUpdatable;
import gregapi.tileentity.base.TileEntityBase07Paintable;
import gregapi.tileentity.connectors.ITileEntityItemPipe;
import gregapi.tileentity.data.ITileEntityProgress;
import gregapi.tileentity.delegate.DelegatorTileEntity;
import gregapi.tileentity.delegate.ITileEntityDelegating;
import gregapi.tileentity.machines.*;
import gregapi.util.UT;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.Container;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.core.Direction;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.fluids.FluidStack;
import gregapi.fluid.FluidTankInfo;
import net.minecraftforge.fluids.capability.IFluidHandler;

import java.util.List;

import static gregapi.data.CS.*;

/**
 * @author Gregorius Techneticies
 */
public class MultiTileEntityBridge extends TileEntityBase07Paintable implements ITileEntityDelegating, ITileEntityAdjacentInventoryUpdatable, IFluidHandler, IMTE_GetComparatorInputOverride, IMTE_AddToolTips {
	public byte mModes = 0;
	
	protected IIconContainer[] mTextures = L6_IICONCONTAINER;
	
	@Override
	public void readFromNBT2(CompoundTag aNBT) {
		super.readFromNBT2(aNBT);
		if (aNBT.contains(NBT_MODE)) mModes = aNBT.getByte(NBT_MODE);
		
		if (CODE_CLIENT) {
			if (GT_API.sBlockIcons == null && aNBT.contains(NBT_TEXTURE)) {
				String tTextureName = aNBT.getString(NBT_TEXTURE);
				mTextures = new IIconContainer[] {
				new Textures.BlockIcons.CustomIcon("machines/extenders/"+tTextureName+"/colored/side"),
				new Textures.BlockIcons.CustomIcon("machines/extenders/"+tTextureName+"/overlay/side")};
			} else {
				BlockEntity tCanonicalTileEntity = MultiTileEntityRegistry.getCanonicalTileEntity(getMultiTileEntityRegistryID(), getMultiTileEntityID());
				if (tCanonicalTileEntity instanceof MultiTileEntityBridge) {
					mTextures = ((MultiTileEntityBridge)tCanonicalTileEntity).mTextures;
				}
			}
		}
	}
	
	@Override
	public void addToolTips(List<String> aList, ItemStack aStack, boolean aF3_H) {
		if ((mModes & EXTENDER_ALL) == EXTENDER_ALL) {
			aList.add(Chat.CYAN + LH.get(TOOLTIP_EXTENDER_ALL));
		} else {
			if ((mModes & EXTENDER_INV     ) == EXTENDER_INV     ) aList.add(Chat.CYAN + LH.get(TOOLTIP_EXTENDER_INVENTORY));
			if ((mModes & EXTENDER_TANK    ) == EXTENDER_TANK    ) aList.add(Chat.CYAN + LH.get(TOOLTIP_EXTENDER_TANK));
			if ((mModes & EXTENDER_REDSTONE) == EXTENDER_REDSTONE) aList.add(Chat.CYAN + LH.get(TOOLTIP_EXTENDER_REDSTONE));
			if ((mModes & EXTENDER_CONTROL ) == EXTENDER_CONTROL ) aList.add(Chat.CYAN + LH.get(TOOLTIP_EXTENDER_CONTROL));
			if ((mModes & EXTENDER_OTHER   ) == EXTENDER_OTHER   ) aList.add(Chat.CYAN + LH.get(TOOLTIP_EXTENDER_OTHER));
		}
		aList.add(Chat.ORANGE + LH.get(TOOLTIP_EXTENDER_EXCLUSIVE));
		
	}
	
	@Override
	public void onTick2(long aTimer, boolean aIsServerSide) {
		if (aIsServerSide && mBlockUpdated && (mModes & EXTENDER_REDSTONE) == 0) causeBlockUpdate();
	}
	
	@Override
	public ITexture getTexture2(Block aBlock, int aRenderPass, byte aSide, boolean[] aShouldSideBeRendered) {
		if (!aShouldSideBeRendered[aSide]) return null;
		return BlockTextureMulti.get(BlockTextureDefault.get(mTextures[0], mRGBa), BlockTextureDefault.get(mTextures[1]));
	}
	
	@Override
	public void adjacentInventoryUpdated(byte aSide, Container aTileEntity) {
		if ((mModes & EXTENDER_INV) != 0) {
			DelegatorTileEntity<BlockEntity> tDelegate = getAdjacentTileEntity(getExtenderTargetSide(aSide), F, T);
			if (tDelegate.mTileEntity instanceof ITileEntityAdjacentInventoryUpdatable) ((ITileEntityAdjacentInventoryUpdatable)tDelegate.mTileEntity).adjacentInventoryUpdated(tDelegate.mSideOfTileEntity, aTileEntity);
		}
	}
	
	
	@Override public String getTileEntityName() {return "gt.multitileentity.bridge";}
	
	// Relay TileEntities
	
	@Override
	public DelegatorTileEntity<BlockEntity> getDelegateTileEntity(byte aSide) {
		if ((mModes & EXTENDER_ALL) == EXTENDER_ALL) return getAdjacentTileEntity(getExtenderTargetSide(aSide), F, T);
		if ((mModes & EXTENDER_INV) != 0) {
			DelegatorTileEntity<BlockEntity> rDelegator = getAdjacentTileEntity(getExtenderTargetSide(aSide), F, T);
			if (rDelegator.mTileEntity instanceof ITileEntityItemPipe) return rDelegator;
		}
		return delegator(aSide);
	}
	
	@Override
	public boolean isExtender(byte aSide) {
		return T;
	}
	
	// Relay Redstone
	
	@Override public int getComparatorInputOverride(byte aSide) {return (mModes & EXTENDER_REDSTONE) == 0 ? 0 : getComparatorIncoming(OPOS[aSide]);}
	@Override public byte isProvidingWeakPower2    (byte aSide) {return (mModes & EXTENDER_REDSTONE) == 0 ? 0 : getRedstoneIncoming  (OPOS[aSide]);}
	
	// Relay Inventories
	
	public byte mLastSide = SIDE_UNKNOWN;
	
	@Override
	public ItemStack removeItem(int aSlot, int aDecrement) {
		if ((mModes & EXTENDER_INV) != 0) {
			DelegatorTileEntity<Container> tTileEntity = getAdjacentInventory(getExtenderTargetSide(mLastSide), F, T);
			if (tTileEntity.mTileEntity != null) return tTileEntity.mTileEntity.removeItem(aSlot, aDecrement);
		}
		return null;
	}
	@Override
	public ItemStack removeItemNoUpdate(int aSlot) {
		if ((mModes & EXTENDER_INV) != 0) {
			DelegatorTileEntity<Container> tTileEntity = getAdjacentInventory(getExtenderTargetSide(mLastSide), F, T);
			if (tTileEntity.mTileEntity != null) return tTileEntity.mTileEntity.removeItemNoUpdate(aSlot);
		}
		return null;
	}
	@Override
	public ItemStack getItem(int aSlot) {
		if ((mModes & EXTENDER_INV) != 0) {
			DelegatorTileEntity<Container> tTileEntity = getAdjacentInventory(getExtenderTargetSide(mLastSide), F, T);
			if (tTileEntity.mTileEntity != null) return tTileEntity.mTileEntity.getItem(aSlot);
		}
		return null;
	}
	@Override
	public String getInventoryName() {
		if ((mModes & EXTENDER_INV) != 0) {
			DelegatorTileEntity<Container> tTileEntity = getAdjacentInventory(getExtenderTargetSide(mLastSide), F, T);
		}
		return super.getInventoryName();
	}
	@Override
	public int getContainerSize() {
		if ((mModes & EXTENDER_INV) != 0) {
			DelegatorTileEntity<Container> tTileEntity = getAdjacentInventory(getExtenderTargetSide(mLastSide), F, T);
			if (tTileEntity.mTileEntity != null) return tTileEntity.mTileEntity.getContainerSize();
		}
		return 0;
	}
	@Override
	public int getMaxStackSize() {
		if ((mModes & EXTENDER_INV) != 0) {
			DelegatorTileEntity<Container> tTileEntity = getAdjacentInventory(getExtenderTargetSide(mLastSide), F, T);
			if (tTileEntity.mTileEntity != null) return tTileEntity.mTileEntity.getMaxStackSize();
		}
		return 0;
	}
	@Override
	public void setItem(int aSlot, ItemStack aStack) {
		if ((mModes & EXTENDER_INV) != 0) {
			DelegatorTileEntity<Container> tTileEntity = getAdjacentInventory(getExtenderTargetSide(mLastSide), F, T);
			if (tTileEntity.mTileEntity != null) tTileEntity.mTileEntity.setItem(aSlot, aStack);
		}
	}
	@Override
	public boolean hasCustomInventoryName() {
		if ((mModes & EXTENDER_INV) != 0) {
			DelegatorTileEntity<Container> tTileEntity = getAdjacentInventory(getExtenderTargetSide(mLastSide), F, T);
		}
		return getCustomName() != null;
	}
	@Override
	public boolean canPlaceItem(int aSlot, ItemStack aStack) {
		if ((mModes & EXTENDER_INV) != 0) {
			DelegatorTileEntity<Container> tTileEntity = getAdjacentInventory(getExtenderTargetSide(mLastSide), F, T);
			if (tTileEntity.mTileEntity != null) return tTileEntity.mTileEntity.canPlaceItem(aSlot, aStack);
		}
		return F;
	}
	
	// Relay Sided Inventories
	
	@Override
	public int[] getAccessibleSlotsFromSide2(byte aSide) {
		mLastSide = aSide;
		if ((mModes & EXTENDER_INV) != 0) {
			DelegatorTileEntity<Container> tTileEntity = getAdjacentInventory(getExtenderTargetSide(mLastSide), F, T);
			if (tTileEntity.mTileEntity instanceof WorldlyContainer) return ((WorldlyContainer)tTileEntity.mTileEntity).getSlotsForFace(FORGE_DIR[tTileEntity.mSideOfTileEntity]);
			if (tTileEntity.mTileEntity != null) return UT.Code.getAscendingArray(tTileEntity.mTileEntity.getContainerSize());
		}
		return ZL_INTEGER;
	}
	@Override
	public boolean canInsertItem2(int aSlot, ItemStack aStack, byte aSide) {
		mLastSide = aSide;
		if ((mModes & EXTENDER_INV) != 0) {
			DelegatorTileEntity<Container> tTileEntity = getAdjacentInventory(getExtenderTargetSide(mLastSide), F, T);
			if (tTileEntity.mTileEntity instanceof WorldlyContainer) return ((WorldlyContainer)tTileEntity.mTileEntity).canPlaceItemThroughFace(aSlot, aStack, FORGE_DIR[tTileEntity.mSideOfTileEntity]);
			if (tTileEntity.mTileEntity != null) return T;
		}
		return F;
	}
	@Override
	public boolean canExtractItem2(int aSlot, ItemStack aStack, byte aSide) {
		mLastSide = aSide;
		if ((mModes & EXTENDER_INV) != 0) {
			DelegatorTileEntity<Container> tTileEntity = getAdjacentInventory(getExtenderTargetSide(mLastSide), F, T);
			if (tTileEntity.mTileEntity instanceof WorldlyContainer) return ((WorldlyContainer)tTileEntity.mTileEntity).canTakeItemThroughFace(aSlot, aStack, FORGE_DIR[tTileEntity.mSideOfTileEntity]);
			if (tTileEntity.mTileEntity != null) return T;
		}
		return F;
	}
	
	// Relay Tanks
	
	@Override
	public int fill(Direction aDirection, FluidStack aFluid, boolean doFill) {
		if ((mModes & EXTENDER_TANK) != 0) {
			byte aSide = UT.Code.side(aDirection);
			if (hasCovers() && SIDES_VALID[aSide] && mCovers.mBehaviours[aSide] != null && mCovers.mBehaviours[aSide].interceptFluidFill(aSide, mCovers, aSide, aFluid)) return 0;
			DelegatorTileEntity<IFluidHandler> tTileEntity = getAdjacentTank(getExtenderTargetSide(aSide), F, T);
			if (tTileEntity.mTileEntity != null) return UT.Code.bindInt(FL.fill(tTileEntity, aFluid, doFill));
		}
		return 0;
	}
	@Override
	public FluidStack drain(Direction aDirection, FluidStack aFluid, boolean doDrain) {
		if ((mModes & EXTENDER_TANK) != 0) {
			byte aSide = UT.Code.side(aDirection);
			if (hasCovers() && SIDES_VALID[aSide] && mCovers.mBehaviours[aSide] != null && mCovers.mBehaviours[aSide].interceptFluidDrain(aSide, mCovers, aSide, aFluid)) return null;
			DelegatorTileEntity<IFluidHandler> tTileEntity = getAdjacentTank(getExtenderTargetSide(aSide), F, T);
			if (tTileEntity.mTileEntity != null) return FL.drain(tTileEntity, aFluid, doDrain);
		}
		return null;
	}
	@Override
	public FluidStack drain(Direction aDirection, int maxDrain, boolean doDrain) {
		if ((mModes & EXTENDER_TANK) != 0) {
			byte aSide = UT.Code.side(aDirection);
			if (hasCovers() && SIDES_VALID[aSide] && mCovers.mBehaviours[aSide] != null && mCovers.mBehaviours[aSide].interceptFluidDrain(aSide, mCovers, aSide, null)) return null;
			DelegatorTileEntity<IFluidHandler> tTileEntity = getAdjacentTank(getExtenderTargetSide(aSide), F, T);
			if (tTileEntity.mTileEntity != null) return FL.drain(tTileEntity, maxDrain, doDrain);
		}
		return null;
	}
	@Override
	public boolean canFill(Direction aDirection, Fluid aFluid) {
		if ((mModes & EXTENDER_TANK) != 0) {
			byte aSide = UT.Code.side(aDirection);
			if (hasCovers() && SIDES_VALID[aSide] && mCovers.mBehaviours[aSide] != null && mCovers.mBehaviours[aSide].interceptFluidFill(aSide, mCovers, aSide, FL.make(aFluid, 1))) return F;
			DelegatorTileEntity<IFluidHandler> tTileEntity = getAdjacentTank(getExtenderTargetSide(aSide), F, T);
			if (tTileEntity.mTileEntity != null) return FL.canFill(tTileEntity, aFluid);
		}
		return F;
	}
	@Override
	public boolean canDrain(Direction aDirection, Fluid aFluid) {
		if ((mModes & EXTENDER_TANK) != 0) {
			byte aSide = UT.Code.side(aDirection);
			if (hasCovers() && SIDES_VALID[aSide] && mCovers.mBehaviours[aSide] != null && mCovers.mBehaviours[aSide].interceptFluidDrain(aSide, mCovers, aSide, FL.make(aFluid, 1))) return F;
			DelegatorTileEntity<IFluidHandler> tTileEntity = getAdjacentTank(getExtenderTargetSide(aSide), F, T);
			if (tTileEntity.mTileEntity != null) return FL.canDrain(tTileEntity, aFluid);
		}
		return F;
	}
	@Override
	public FluidTankInfo[] getTankInfo(Direction aDirection) {
		if ((mModes & EXTENDER_TANK) != 0) {
			DelegatorTileEntity<IFluidHandler> tTileEntity = getAdjacentTank(getExtenderTargetSide(UT.Code.side(aDirection)), F, T);
			// 1:1 с оригиналом :311 (getTankInfo(getForgeSideOfTileEntity())); сторону несёт центр шва FL:944.
			if (tTileEntity.mTileEntity != null) return FL.getTankInfo(tTileEntity.mTileEntity, tTileEntity.mSideOfTileEntity);
		}
		return ZL_FLUIDTANKINFO;
	}
	
	// Relay Control Covers and such
	
	public boolean getStateRunningPossible() {
		if ((mModes & EXTENDER_CONTROL) != 0) {
			DelegatorTileEntity<BlockEntity> tTileEntity = getAdjacentTileEntity(getExtenderTargetSide(SIDE_UNDEFINED), F, T);
			if (tTileEntity.mTileEntity instanceof ITileEntityRunningPossible) return ((ITileEntityRunningPossible)tTileEntity.mTileEntity).getStateRunningPossible();
		}
		return F;
	}
	
	public boolean getStateRunningPassively() {
		if ((mModes & EXTENDER_CONTROL) != 0) {
			DelegatorTileEntity<BlockEntity> tTileEntity = getAdjacentTileEntity(getExtenderTargetSide(SIDE_UNDEFINED), F, T);
			if (tTileEntity.mTileEntity instanceof ITileEntityRunningPassively) return ((ITileEntityRunningPassively)tTileEntity.mTileEntity).getStateRunningPassively();
		}
		return F;
	}
	
	public boolean getStateRunningActively() {
		if ((mModes & EXTENDER_CONTROL) != 0) {
			DelegatorTileEntity<BlockEntity> tTileEntity = getAdjacentTileEntity(getExtenderTargetSide(SIDE_UNDEFINED), F, T);
			if (tTileEntity.mTileEntity instanceof ITileEntityRunningActively) return ((ITileEntityRunningActively)tTileEntity.mTileEntity).getStateRunningActively();
		}
		return F;
	}
	
	public boolean getStateRunningSuccessfully() {
		if ((mModes & EXTENDER_CONTROL) != 0) {
			DelegatorTileEntity<BlockEntity> tTileEntity = getAdjacentTileEntity(getExtenderTargetSide(SIDE_UNDEFINED), F, T);
			if (tTileEntity.mTileEntity instanceof ITileEntityRunningSuccessfully) return ((ITileEntityRunningSuccessfully)tTileEntity.mTileEntity).getStateRunningSuccessfully();
		}
		return F;
	}
	
	public boolean getStateOnOff() {
		if ((mModes & EXTENDER_CONTROL) != 0) {
			DelegatorTileEntity<BlockEntity> tTileEntity = getAdjacentTileEntity(getExtenderTargetSide(SIDE_UNDEFINED), F, T);
			if (tTileEntity.mTileEntity instanceof ITileEntitySwitchableOnOff) return ((ITileEntitySwitchableOnOff)tTileEntity.mTileEntity).getStateOnOff();
		}
		return F;
	}
	
	public byte getStateMode() {
		if ((mModes & EXTENDER_CONTROL) != 0) {
			DelegatorTileEntity<BlockEntity> tTileEntity = getAdjacentTileEntity(getExtenderTargetSide(SIDE_UNDEFINED), F, T);
			if (tTileEntity.mTileEntity instanceof ITileEntitySwitchableMode) return ((ITileEntitySwitchableMode)tTileEntity.mTileEntity).getStateMode();
		}
		return 0;
	}
	
	public boolean setStateOnOff(boolean aOnOff) {
		if ((mModes & EXTENDER_CONTROL) != 0) {
			DelegatorTileEntity<BlockEntity> tTileEntity = getAdjacentTileEntity(getExtenderTargetSide(SIDE_UNDEFINED), F, T);
			if (tTileEntity.mTileEntity instanceof ITileEntitySwitchableOnOff) return ((ITileEntitySwitchableOnOff)tTileEntity.mTileEntity).setStateOnOff(aOnOff);
		}
		return F;
	}
	
	public byte setStateMode(byte aMode) {
		if ((mModes & EXTENDER_CONTROL) != 0) {
			DelegatorTileEntity<BlockEntity> tTileEntity = getAdjacentTileEntity(getExtenderTargetSide(SIDE_UNDEFINED), F, T);
			if (tTileEntity.mTileEntity instanceof ITileEntitySwitchableMode) return ((ITileEntitySwitchableMode)tTileEntity.mTileEntity).setStateMode(aMode);
		}
		return 0;
	}
	
	public long getProgressValue(byte aSide) {
		if ((mModes & EXTENDER_CONTROL) != 0) {
			DelegatorTileEntity<BlockEntity> tTileEntity = getAdjacentTileEntity(getExtenderTargetSide(aSide), F, T);
			if (tTileEntity.mTileEntity instanceof ITileEntityProgress) return ((ITileEntityProgress)tTileEntity.mTileEntity).getProgressValue(tTileEntity.mSideOfTileEntity);
		}
		return 0;
	}
	
	public long getProgressMax(byte aSide) {
		if ((mModes & EXTENDER_CONTROL) != 0) {
			DelegatorTileEntity<BlockEntity> tTileEntity = getAdjacentTileEntity(getExtenderTargetSide(aSide), F, T);
			if (tTileEntity.mTileEntity instanceof ITileEntityProgress) return ((ITileEntityProgress)tTileEntity.mTileEntity).getProgressMax(tTileEntity.mSideOfTileEntity);
		}
		return 0;
	}
	
	
	public byte getExtenderTargetSide(byte aSide) {return OPOS[aSide];}
	
	@Override public boolean stillValid(Player aPlayer) {return aPlayer.distanceToSqr(getBlockPos().getX()+0.5, getBlockPos().getY()+0.5, getBlockPos().getZ()+0.5) <= 64;}
	@Override public void startOpen(net.minecraft.world.entity.player.Player aUser) {/**/}
	@Override public void stopOpen(net.minecraft.world.entity.player.Player aUser) {/**/}
	@Override public boolean canDrop(int aInventorySlot) {return F;}
}

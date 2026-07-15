/**
 * Copyright (c) 2024 GregTech-6 Team
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

package gregtech.tileentity.portals;

import gregapi.block.multitileentity.IMultiTileEntity.*;
import gregapi.block.multitileentity.MultiTileEntityRegistry;
import gregapi.data.LH;
import gregapi.data.LH.Chat;
import gregapi.network.INetworkHandler;
import gregapi.network.IPacket;
import gregapi.render.ITexture;
import gregapi.tileentity.base.TileEntityBase04MultiTileEntities;
import gregapi.tileentity.data.ITileEntitySurface;
import gregapi.tileentity.delegate.DelegatorTileEntity;
import gregapi.tileentity.delegate.ITileEntityDelegating;
import gregapi.util.UT;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.Container;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.core.Direction;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import gregapi.fluid.FluidTankInfo;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import java.io.File;
import java.util.List;

import static gregapi.data.CS.*;

/**
 * @author Gregorius Techneticies
 * 
 * An example implementation of a Miniature Nether Portal with my MultiTileEntity System.
 */
public abstract class MultiTileEntityMiniPortal extends TileEntityBase04MultiTileEntities implements ITileEntitySurface, ITileEntityDelegating, IFluidHandler, WorldlyContainer, IMTE_OnServerStart, IMTE_OnServerStop, IMTE_OnServerLoad, IMTE_OnToolClick, IMTE_IsProvidingWeakPower, IMTE_GetComparatorInputOverride, IMTE_GetExplosionResistance, IMTE_GetBlockHardness, IMTE_GetLightOpacity, IMTE_AddToolTips, IMTE_SyncDataByte {
	protected boolean mActive = F;
	
	public MultiTileEntityMiniPortal mTarget = null;
	public final byte[] mRedstone = new byte[] {0,0,0,0,0,0}, mComparator = new byte[] {0,0,0,0,0,0}, xRedstone = new byte[] {0,0,0,0,0,0}, xComparator = new byte[] {0,0,0,0,0,0}, wRedstone = new byte[] {0,0,0,0,0,0}, wComparator = new byte[] {0,0,0,0,0,0};
	
	public abstract void findTargetPortal();
	public abstract void addThisPortalToLists();
	public abstract List<MultiTileEntityMiniPortal> getPortalListA();
	public abstract List<MultiTileEntityMiniPortal> getPortalListB();
	
	@Override
	public void readFromNBT2(CompoundTag aNBT) {
		super.readFromNBT2(aNBT);
		if (aNBT.contains(NBT_ACTIVE)) mActive = aNBT.getBooleanOr(NBT_ACTIVE, false);
	}
	
	@Override
	public void writeToNBT2(CompoundTag aNBT) {
		super.writeToNBT2(aNBT);
		UT.NBT.setBoolean(aNBT, NBT_ACTIVE, mActive);
	}
	
	private static final String
	sToolTipFunction1 = "gt.tileentity.portal.mini.tooltip.1",
	sToolTipFunction2 = "gt.tileentity.portal.mini.tooltip.2";
	
	static {
		LH.add(sToolTipFunction1, "Teleports Items, Fluids, Redstone, Comparator Signals, GT Energy and more!");
		LH.add(sToolTipFunction2, "Always teleports things to the closest active Portal in Range!");
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public void addToolTips(@SuppressWarnings("rawtypes") List aList, ItemStack aStack, boolean aF3_H) {
		aList.add(Chat.CYAN     + LH.get(sToolTipFunction1));
		aList.add(Chat.CYAN     + LH.get(sToolTipFunction2));
		addToolTips2(aList, aStack, aF3_H);
		aList.add(Chat.ORANGE   + LH.get(LH.REQUIREMENT_CHUNKLOADER));
	}
	
	public void addToolTips2(List<String> aList, ItemStack aStack, boolean aF3_H) {/**/}
	
	@Override
	public void onTickFirst(boolean aIsServerSide) {
		super.onTickFirst(aIsServerSide);
		if (aIsServerSide && mActive) {
			addThisPortalToLists();
			causeBlockUpdate();
		}
	}
	
	@Override
	public void onTickStart(long aTimer, boolean aIsServerSide) {
		super.onTickStart(aTimer, aIsServerSide);
		
		if (aIsServerSide) {
			for (byte tSide : ALL_SIDES_VALID) {
				if (mActive) {
					if (xRedstone[tSide] >= 0) {
						if (mRedstone[tSide] != xRedstone[tSide]) {
							mRedstone[tSide]  = xRedstone[tSide];
							causeBlockUpdate();
						}
						xRedstone[tSide] = -1;
						wRedstone[tSide] =  0;
					} else {
						if (wRedstone[tSide] >= 20) {
							if (mRedstone[tSide] != 0) {
								mRedstone[tSide]  = 0;
								causeBlockUpdate();
							}
						} else {
							wRedstone[tSide]++;
						}
					}
					
					if (xComparator[tSide] >= 0) {
						if (mComparator[tSide] != xComparator[tSide]) {
							mComparator[tSide]  = xComparator[tSide];
							causeBlockUpdate();
						}
						xComparator[tSide] = -1;
						wComparator[tSide] =  0;
					} else {
						if (wComparator[tSide] >= 20) {
							if (mComparator[tSide] != 0) {
								mComparator[tSide]  = 0;
								causeBlockUpdate();
							}
						} else {
							wComparator[tSide]++;
						}
					}
				} else {
					if (mRedstone[tSide] != 0) {
						mRedstone[tSide]  = 0;
						causeBlockUpdate();
					}
					if (mComparator[tSide] != 0) {
						mComparator[tSide]  = 0;
						causeBlockUpdate();
					}
				}
			}
		}
	}
	
	@Override
	public void onTick(long aTimer, boolean aIsServerSide) {
		super.onTick(aTimer, aIsServerSide);
		
		if (aIsServerSide) {
			// Check if Target is still valid, because you never know how Minecraft screws you over.
			if (mActive && (mTarget == null ? aTimer % 100 == 5 : mTarget.isDead())) findTargetPortal();
			
			// Scan Redstone
			if (mTarget != null) for (byte tSide : ALL_SIDES_VALID) {
				mTarget.xRedstone  [OPOS[tSide]] = (byte)UT.Code.bind_(mTarget.xRedstone  [OPOS[tSide]], 15, getRedstoneIncoming  (tSide));
				mTarget.xComparator[OPOS[tSide]] = (byte)UT.Code.bind_(mTarget.xComparator[OPOS[tSide]], 15, getComparatorIncoming(tSide));
			}
		}
	}
	
	public void setPortalActive() {if (!mActive) {mActive = T; addThisPortalToLists(); causeBlockUpdate(); updateClientData();}}
	public void setPortalInactive() {if (mActive) {disableThisPortal(); causeBlockUpdate(); updateClientData();}}
	
	public void removeThisPortalFromLists() {
		if (getPortalListA().remove(this)) for (MultiTileEntityMiniPortal tPortal : getPortalListB()) if (tPortal.mTarget == this) tPortal.findTargetPortal();
		if (getPortalListB().remove(this)) for (MultiTileEntityMiniPortal tPortal : getPortalListA()) if (tPortal.mTarget == this) tPortal.findTargetPortal();
	}
	
	@Override
	public void clearRemoved() {
		super.clearRemoved();
		if (mActive) addThisPortalToLists();
	}
	
	@Override
	public void setRemoved() {
		disableThisPortal();
		super.setRemoved();
	}
	
	@Override
	public void onChunkUnloaded() {
		disableThisPortal();
		super.onChunkUnloaded();
	}
	
	@Override public void onServerStart() {getPortalListA().clear(); getPortalListB().clear();}
	@Override public void onServerStop () {getPortalListA().clear(); getPortalListB().clear();}
	@Override public void onServerLoad(File aSaveLocation) {if (CODE_CLIENT) {getPortalListA().clear(); getPortalListB().clear();}}
	
	public void disableThisPortal() {
		mActive = F;
		for (byte tSide : ALL_SIDES_VALID) {
			mRedstone[tSide] = 0;
			mComparator[tSide] = 0;
			if (mTarget != null) {
				mTarget.xRedstone  [OPOS[tSide]] = 0;
				mTarget.xComparator[OPOS[tSide]] = 0;
			}
		}
		removeThisPortalFromLists();
		mTarget = null;
	}
	
	@Override
	public long onToolClick(String aTool, long aRemainingDurability, long aQuality, Entity aPlayer, List<String> aChatReturn, Container aPlayerInventory, boolean aSneaking, ItemStack aStack, byte aSide, float aHitX, float aHitY, float aHitZ) {
		if (isClientSide()) return 0;
		if (aTool.equals(TOOL_magnifyingglass)) {
			if (aChatReturn != null) {
				if (mTarget == null) {
					aChatReturn.add("No Target");
				} else {
					aChatReturn.add("X: " + mTarget.getBlockPos().getX() + "   Y: " + mTarget.getBlockPos().getY() + "   Z: " + mTarget.getBlockPos().getZ());
				}
			}
			return 1;
		}
		return 0;
	}
	
	@Override
	public IPacket getClientDataPacket(boolean aSendAll) {
		return getClientDataPacketByte(aSendAll, (byte)(mActive?1:0));
	}
	
	@Override
	public boolean receiveDataByte(byte aData, INetworkHandler aNetworkHandler) {
		if (mActive) {
			mActive = ((aData & 1) != 0);
		} else {
			mActive = ((aData & 1) != 0);
			if (mActive && mTimer > 20) UT.Sounds.play("portal.portal", 10, 1.0F, 1.0F, getCoords());
		}
		return T;
	}
	
	@Override public int getLightOpacity() {return LIGHT_OPACITY_LEAVES;}
	
	@Override public float getSurfaceSize(byte aSide) {return 1.0F;}
	@Override public float getSurfaceSizeAttachable(byte aSide) {return 0.75F;}
	@Override public float getSurfaceDistance(byte aSide) {return 0.0F;}
	@Override public boolean isSurfaceSolid(byte aSide) {return F;}
	@Override public boolean isSurfaceOpaque(byte aSide) {return mActive;}
	
	@Override public int getFireSpreadSpeed(byte aSide, boolean aDefault) {return 0;}
	@Override public int getFlammability(byte aSide, boolean aDefault) {return 0;}
	@Override public float getBlockHardness() {return Blocks.STONE.getBlockHardness(level, getBlockPos().getX(), getBlockPos().getY(), getBlockPos().getZ());}
	@Override public float getExplosionResistance2() {return Blocks.STONE.getExplosionResistance(null);}
	
	@Override
	public int getRenderPasses(Block aBlock, boolean[] aShouldSideBeRendered) {
		return 13;
	}
	
	@Override
	public boolean setBlockBounds(Block aBlock, int aRenderPass, boolean[] aShouldSideBeRendered) {
		return box(aBlock, sBlockBounds[aRenderPass]);
	}
	
	protected static float[][] sBlockBounds = {
		{PX_P[ 1], PX_P[ 1], PX_P[ 1], PX_N[ 1], PX_N[ 1], PX_N[ 1]},
		
		{PX_P[ 0], PX_P[ 0], PX_P[ 0], PX_N[ 0], PX_N[14], PX_N[14]},
		{PX_P[ 0], PX_P[ 2], PX_P[ 0], PX_N[14], PX_N[ 2], PX_N[14]},
		{PX_P[ 0], PX_P[ 0], PX_P[ 0], PX_N[14], PX_N[14], PX_N[ 0]},
		{PX_P[14], PX_P[ 0], PX_P[ 0], PX_N[ 0], PX_N[14], PX_N[ 0]},
		{PX_P[14], PX_P[ 2], PX_P[ 0], PX_N[ 0], PX_N[ 2], PX_N[14]},
		{PX_P[ 0], PX_P[14], PX_P[ 0], PX_N[14], PX_N[ 0], PX_N[ 0]},
		{PX_P[ 0], PX_P[14], PX_P[ 0], PX_N[ 0], PX_N[ 0], PX_N[14]},
		{PX_P[ 0], PX_P[ 2], PX_P[14], PX_N[14], PX_N[ 2], PX_N[ 0]},
		{PX_P[ 0], PX_P[ 0], PX_P[14], PX_N[ 0], PX_N[14], PX_N[ 0]},
		{PX_P[ 0], PX_P[14], PX_P[14], PX_N[ 0], PX_N[ 0], PX_N[ 0]},
		{PX_P[14], PX_P[ 2], PX_P[14], PX_N[ 0], PX_N[ 2], PX_N[ 0]},
		{PX_P[14], PX_P[14], PX_P[ 0], PX_N[ 0], PX_N[ 0], PX_N[ 0]}
	};
	
	protected static boolean[][] sRenderedSides = {
		{T,T,T,T,T,T},
		
		{T,T,T,T,F,F},
		{F,F,T,T,T,T},
		{T,T,F,F,T,T},
		{T,T,F,F,T,T},
		{F,F,T,T,T,T},
		{T,T,F,F,T,T},
		{T,T,T,T,F,F},
		{F,F,T,T,T,T},
		{T,T,T,T,F,F},
		{T,T,T,T,F,F},
		{F,F,T,T,T,T},
		{T,T,F,F,T,T}
	};
	
	@Override
	public ITexture getTexture(Block aBlock, int aRenderPass, byte aSide, boolean[] aShouldSideBeRendered) {
		return sRenderedSides[aRenderPass][aSide] ? aRenderPass == 0 ? mActive || level == null ? aShouldSideBeRendered[aSide] ? getPortalTexture() : null : getInactiveTexture() : getFrameTexture() : null;
	}
	
	public abstract ITexture getPortalTexture();
	public abstract ITexture getFrameTexture();
	public ITexture getInactiveTexture() {return null;}
	
	// Relay TileEntities
	
	@Override
	public DelegatorTileEntity<BlockEntity> getDelegateTileEntity(byte aSide) {
		if (mTarget == null) return delegator(aSide);
		return mTarget.getAdjacentTileEntity(OPOS[aSide]);
	}
	
	@Override
	public boolean isExtender(byte aSide) {
		return mTarget != null;
	}
	
	// Relay Redstone
	
	@Override
	public int getComparatorInputOverride(byte aSide) {
		return mComparator[aSide];
	}
	
	@Override
	public int isProvidingWeakPower(byte aOppositeSide) {
		return mRedstone[OPOS[aOppositeSide]];
	}
	
	// Relay Inventories
	
	public byte mLastSide = SIDE_UNKNOWN;
	
	@Override
	public ItemStack decrStackSize(int aSlot, int aDecrement) {
		if (mTarget != null) {
			DelegatorTileEntity<Container> tTileEntity = mTarget.getAdjacentInventory(OPOS[mLastSide]);
			if (tTileEntity.mTileEntity != null) return tTileEntity.mTileEntity.decrStackSize(aSlot, aDecrement);
		}
		return null;
	}
	@Override
	public ItemStack getStackInSlotOnClosing(int aSlot) {
		if (mTarget != null) {
			DelegatorTileEntity<Container> tTileEntity = mTarget.getAdjacentInventory(OPOS[mLastSide]);
			if (tTileEntity.mTileEntity != null) return tTileEntity.mTileEntity.removeItemNoUpdate(aSlot);
		}
		return null;
	}
	@Override
	public ItemStack getStackInSlot(int aSlot) {
		if (mTarget != null) {
			DelegatorTileEntity<Container> tTileEntity = mTarget.getAdjacentInventory(OPOS[mLastSide]);
			if (tTileEntity.mTileEntity != null) return tTileEntity.mTileEntity.getItem(aSlot);
		}
		return null;
	}
	@Override
	public String getInventoryName() {
		if (mTarget != null) {
			DelegatorTileEntity<Container> tTileEntity = mTarget.getAdjacentInventory(OPOS[mLastSide]);
			if (tTileEntity.mTileEntity != null) return tTileEntity.mTileEntity.getInventoryName();
		}
		String rName = getCustomName();
		if (UT.Code.stringValid(rName)) return rName;
		MultiTileEntityRegistry tRegistry = MultiTileEntityRegistry.getRegistry(getMultiTileEntityRegistryID());
		return tRegistry==null?getClass().getName():tRegistry.getLocal(getMultiTileEntityID());
	}
	@Override
	public int getSizeInventory() {
		if (mTarget != null) {
			DelegatorTileEntity<Container> tTileEntity = mTarget.getAdjacentInventory(OPOS[mLastSide]);
			if (tTileEntity.mTileEntity != null) return tTileEntity.mTileEntity.getContainerSize();
		}
		return 0;
	}
	@Override
	public int getInventoryStackLimit() {
		if (mTarget != null) {
			DelegatorTileEntity<Container> tTileEntity = mTarget.getAdjacentInventory(OPOS[mLastSide]);
			if (tTileEntity.mTileEntity != null) return tTileEntity.mTileEntity.getInventoryStackLimit();
		}
		return 0;
	}
	@Override
	public void setInventorySlotContents(int aSlot, ItemStack aStack) {
		if (mTarget != null) {
			DelegatorTileEntity<Container> tTileEntity = mTarget.getAdjacentInventory(OPOS[mLastSide]);
			if (tTileEntity.mTileEntity != null) tTileEntity.mTileEntity.setItem(aSlot, aStack);
		}
	}
	@Override
	public boolean hasCustomInventoryName() {
		if (mTarget != null) {
			DelegatorTileEntity<Container> tTileEntity = mTarget.getAdjacentInventory(OPOS[mLastSide]);
			if (tTileEntity.mTileEntity != null) return tTileEntity.mTileEntity.hasCustomInventoryName();
		}
		return getCustomName() != null;
	}
	@Override
	public boolean isItemValidForSlot(int aSlot, ItemStack aStack) {
		if (mTarget != null) {
			DelegatorTileEntity<Container> tTileEntity = mTarget.getAdjacentInventory(OPOS[mLastSide]);
			if (tTileEntity.mTileEntity != null) return tTileEntity.mTileEntity.isItemValidForSlot(aSlot, aStack);
		}
		return F;
	}
	
	// Relay Sided Inventories
	
	@Override
	public int[] getAccessibleSlotsFromSide(int aSide) {
		mLastSide = (byte)aSide;
		if (mTarget != null) {
			DelegatorTileEntity<Container> tTileEntity = mTarget.getAdjacentInventory(OPOS[mLastSide]);
			if (tTileEntity.mTileEntity instanceof WorldlyContainer) return ((WorldlyContainer)tTileEntity.mTileEntity).getAccessibleSlotsFromSide(tTileEntity.mSideOfTileEntity);
			if (tTileEntity.mTileEntity != null) {
				int[] tReturn = new int[tTileEntity.mTileEntity.getContainerSize()];
				for (int i = 0; i < tReturn.length; i++) tReturn[i] = i;
				return tReturn;
			}
		}
		return ZL_INTEGER;
	}
	@Override
	public boolean canInsertItem(int aSlot, ItemStack aStack, int aSide) {
		mLastSide = (byte)aSide;
		if (mTarget != null) {
			DelegatorTileEntity<Container> tTileEntity = mTarget.getAdjacentInventory(OPOS[mLastSide]);
			if (tTileEntity.mTileEntity instanceof WorldlyContainer) return ((WorldlyContainer)tTileEntity.mTileEntity).canInsertItem(aSlot, aStack, tTileEntity.mSideOfTileEntity);
			if (tTileEntity.mTileEntity != null) return T;
		}
		return F;
	}
	@Override
	public boolean canExtractItem(int aSlot, ItemStack aStack, int aSide) {
		mLastSide = (byte)aSide;
		if (mTarget != null) {
			DelegatorTileEntity<Container> tTileEntity = mTarget.getAdjacentInventory(OPOS[mLastSide]);
			if (tTileEntity.mTileEntity instanceof WorldlyContainer) return ((WorldlyContainer)tTileEntity.mTileEntity).canExtractItem(aSlot, aStack, tTileEntity.mSideOfTileEntity);
			if (tTileEntity.mTileEntity != null) return T;
		}
		return F;
	}
	
	// Relay Tanks
	
	@Override
	public int fill(Direction from, FluidStack resource, boolean doFill) {
		if (mTarget != null) {
			DelegatorTileEntity<IFluidHandler> tTileEntity = mTarget.getAdjacentTank(OPOS[UT.Code.side(from)]);
			if (tTileEntity.mTileEntity != null) return tTileEntity.mTileEntity.fill(tTileEntity.getForgeSideOfTileEntity(), resource, doFill);
		}
		return 0;
	}
	@Override
	public FluidStack drain(Direction from, FluidStack resource, boolean doDrain) {
		if (mTarget != null) {
			DelegatorTileEntity<IFluidHandler> tTileEntity = mTarget.getAdjacentTank(OPOS[UT.Code.side(from)]);
			if (tTileEntity.mTileEntity != null) return tTileEntity.mTileEntity.drain(tTileEntity.getForgeSideOfTileEntity(), resource, doDrain);
		}
		return null;
	}
	@Override
	public FluidStack drain(Direction from, int maxDrain, boolean doDrain) {
		if (mTarget != null) {
			DelegatorTileEntity<IFluidHandler> tTileEntity = mTarget.getAdjacentTank(OPOS[UT.Code.side(from)]);
			if (tTileEntity.mTileEntity != null) return tTileEntity.mTileEntity.drain(tTileEntity.getForgeSideOfTileEntity(), maxDrain, doDrain);
		}
		return null;
	}
	@Override
	public boolean canFill(Direction from, Fluid fluid) {
		if (mTarget != null) {
			DelegatorTileEntity<IFluidHandler> tTileEntity = mTarget.getAdjacentTank(OPOS[UT.Code.side(from)]);
			if (tTileEntity.mTileEntity != null) return tTileEntity.mTileEntity.canFill(tTileEntity.getForgeSideOfTileEntity(), fluid);
		}
		return F;
	}
	@Override
	public boolean canDrain(Direction from, Fluid fluid) {
		if (mTarget != null) {
			DelegatorTileEntity<IFluidHandler> tTileEntity = mTarget.getAdjacentTank(OPOS[UT.Code.side(from)]);
			if (tTileEntity.mTileEntity != null) return tTileEntity.mTileEntity.canDrain(tTileEntity.getForgeSideOfTileEntity(), fluid);
		}
		return F;
	}
	@Override
	public FluidTankInfo[] getTankInfo(Direction from) {
		if (mTarget != null) {
			DelegatorTileEntity<IFluidHandler> tTileEntity = mTarget.getAdjacentTank(OPOS[UT.Code.side(from)]);
			if (tTileEntity.mTileEntity != null) return tTileEntity.mTileEntity.getTankInfo(tTileEntity.getForgeSideOfTileEntity());
		}
		return ZL_FLUIDTANKINFO;
	}
	
	@Override public boolean isUseableByPlayer(Player aPlayer) {return aPlayer.distanceToSqr(getBlockPos().getX() + 0.5D, getBlockPos().getY() + 0.5D, getBlockPos().getZ() + 0.5D) <= 64D;}
	@Override public void openInventory() {/**/}
	@Override public void closeInventory() {/**/}
}

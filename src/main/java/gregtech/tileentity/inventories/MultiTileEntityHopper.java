/**
 * Copyright (c) 2022 GregTech-6 Team
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

package gregtech.tileentity.inventories;

import gregapi.block.multitileentity.MultiTileEntityContainer;
import gregapi.data.LH;
import gregapi.data.LH.Chat;
import gregapi.data.TD;
import gregapi.gui.ContainerClientDefault;
import gregapi.gui.ContainerCommonDefault;
import gregapi.old.Textures;
import gregapi.render.BlockTextureDefault;
import gregapi.render.BlockTextureMulti;
import gregapi.render.IIconContainer;
import gregapi.render.ITexture;
import gregapi.tileentity.ITileEntityAdjacentInventoryUpdatable;
import gregapi.tileentity.base.TileEntityBase09FacingSingle;
import gregapi.tileentity.connectors.ITileEntityConnector;
import gregapi.tileentity.delegate.DelegatorTileEntity;
import gregapi.util.ST;
import gregapi.util.UT;
import gregapi.util.WD;
import gregtech.tileentity.tools.MultiTileEntityAnvil;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.SnowGolem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.Level;

import java.util.List;

import static gregapi.data.CS.*;

/**
 * @author Gregorius Techneticies
 */
public class MultiTileEntityHopper extends TileEntityBase09FacingSingle implements ITileEntityAdjacentInventoryUpdatable {
	public boolean mExactMode = F, mLock = F;
	public byte mMode = 0, mCheck = 3;
	
	@Override
	public void readFromNBT2(CompoundTag aNBT) {
		super.readFromNBT2(aNBT);
		mMode = aNBT.getByte(NBT_MODE);
		mExactMode = aNBT.getBoolean(NBT_MODE+".a");
	}
	
	@Override
	public void writeToNBT2(CompoundTag aNBT) {
		super.writeToNBT2(aNBT);
		if (mMode != 0) aNBT.putByte(NBT_MODE, mMode);
		UT.NBT.setBoolean(aNBT, NBT_MODE+".a", mExactMode);
	}
	
	@Override
	public CompoundTag writeItemNBT2(CompoundTag aNBT) {
		if (mMode != 0) aNBT.putByte(NBT_MODE, mMode);
		UT.NBT.setBoolean(aNBT, NBT_MODE+".a", mExactMode);
		return super.writeItemNBT2(aNBT);
	}
	
	static {
		LH.add("gt.multitileentity.hopper.tooltip.1", "Slot Count: ");
		LH.add("gt.multitileentity.hopper.tooltip.2", "Specified Stacksize: ");
		LH.add("gt.multitileentity.hopper.tooltip.3", "Exact Insertion Mode");
	}
	
	@Override
	public void addToolTips(List<String> aList, ItemStack aStack, boolean aF3_H) {
		aList.add(Chat.CYAN     + LH.get("gt.multitileentity.hopper.tooltip.1") + invsize());
		if (mMode > 0)
		aList.add(Chat.CYAN     + LH.get("gt.multitileentity.hopper.tooltip.2") + mMode);
		if (mExactMode)
		aList.add(Chat.CYAN     + LH.get("gt.multitileentity.hopper.tooltip.3"));
		aList.add(Chat.DGRAY    + LH.get(LH.TOOL_TO_TOGGLE_SCREWDRIVER));
		aList.add(Chat.DGRAY    + LH.get(LH.TOOL_TO_TOGGLE_MONKEY_WRENCH));
		aList.add(Chat.DGRAY    + LH.get(LH.TOOL_TO_DETAIL_MAGNIFYINGGLASS));
		aList.add(Chat.DGRAY    + LH.get(LH.TOOL_TO_RESET_SOFT_HAMMER));
		super.addToolTips(aList, aStack, aF3_H);
	}
	
	@Override
	public boolean onBlockActivated3(Player aPlayer, byte aSide, float aHitX, float aHitY, float aHitZ) {
		if (isServerSide() && isUseableByPlayerGUI(aPlayer)) openGUI(aPlayer);
		return T;
	}
	
	@Override
	public boolean onPlaced(ItemStack aStack, Player aPlayer, MultiTileEntityContainer aMTEContainer, Level aWorld, int aX, int aY, int aZ, byte aSide, float aHitX, float aHitY, float aHitZ) {
		super.onPlaced(aStack, aPlayer, aMTEContainer, aWorld, aX, aY, aZ, aSide, aHitX, aHitY, aHitZ);
		if (isServerSide() && SIDES_BOTTOM_HORIZONTAL[mFacing]) {
			DelegatorTileEntity<BlockEntity> tDelegator = getAdjacentTileEntity(mFacing);
			if (tDelegator.mTileEntity instanceof ITileEntityConnector && SIDES_VALID[tDelegator.mSideOfTileEntity] && ((ITileEntityConnector)tDelegator.mTileEntity).allowInteraction(aPlayer) && UT.Code.haveOneCommonElement(((ITileEntityConnector)tDelegator.mTileEntity).getConnectorTypes(tDelegator.mSideOfTileEntity), TD.Connectors.ALL_ITEM_TRANSPORT)) {
				((ITileEntityConnector)tDelegator.mTileEntity).connect(tDelegator.mSideOfTileEntity, T);
			}
		}
		return T;
	}
	
	@Override
	public long onToolClick2(String aTool, long aRemainingDurability, long aQuality, Entity aPlayer, List<String> aChatReturn, Container aPlayerInventory, boolean aSneaking, ItemStack aStack, byte aSide, float aHitX, float aHitY, float aHitZ) {
		if (isClientSide()) return super.onToolClick2(aTool, aRemainingDurability, aQuality, aPlayer, aChatReturn, aPlayerInventory, aSneaking, aStack, aSide, aHitX, aHitY, aHitZ);
		if (aTool.equals(TOOL_screwdriver)) {
			if (aPlayer != null && aPlayer.isShiftKeyDown()) {
				if (--mMode <  0) mMode = 64;
			} else {
				if (++mMode > 64) mMode =  0;
			}
			if (aChatReturn != null) aChatReturn.add(mMode <= 0 ? (mExactMode ? "Emits up to 1 Stack" : "Emits up to 64 Items") : (mExactMode ? "Emits exact Stacksize of: " : "Emits divisible Stacksize of: ") + mMode);
			return 200;
		}
		if (aTool.equals(TOOL_monkeywrench)) {
			mExactMode = !mExactMode;
			if (aChatReturn != null) aChatReturn.add(mMode <= 0 ? (mExactMode ? "Emits up to 1 Stack" : "Emits up to 64 Items") : (mExactMode ? "Emits exact Stacksize of: " : "Emits divisible Stacksize of: ") + mMode);
			return 10000;
		}
		if (aTool.equals(TOOL_softhammer)) {
			mExactMode = F;
			mMode = 0;
			if (aChatReturn != null) aChatReturn.add(mMode <= 0 ? (mExactMode ? "Emits up to 1 Stack" : "Emits up to 64 Items") : (mExactMode ? "Emits exact Stacksize of: " : "Emits divisible Stacksize of: ") + mMode);
			return 10000;
		}
		if (aTool.equals(TOOL_magnifyingglass)) {
			if (aChatReturn != null) aChatReturn.add(mMode <= 0 ? (mExactMode ? "Emits up to 1 Stack" : "Emits up to 64 Items") : (mExactMode ? "Emits exact Stacksize of: " : "Emits divisible Stacksize of: ") + mMode);
			return 1;
		}
		return super.onToolClick2(aTool, aRemainingDurability, aQuality, aPlayer, aChatReturn, aPlayerInventory, aSneaking, aStack, aSide, aHitX, aHitY, aHitZ);
	}
	
	@Override
	public void onWalkOver2(LivingEntity aEntity) {
		if (isServerSide() && (aEntity.getClass() == SnowGolem.class || "EntityNewSnowGolem".equalsIgnoreCase(UT.Reflection.getLowercaseClass(aEntity)))) {
			int i = invsize(); while (--i>=0) if (addStackToSlot(i, ST.make(Items.SNOWBALL, 1, 0))) break;
		}
	}
	
	@Override
	@SuppressWarnings("rawtypes")
	public void onTick2(long aTimer, boolean aIsServerSide) {
		super.onTick2(aTimer, aIsServerSide);
		if (aIsServerSide) {
			int tMovedItems = 0;
			if (mCheck > 0) {
				mCheck--;
			} else if ((mCheck == 0 || mInventoryChanged || mBlockUpdated || SYNC_SECOND) && !hasRedstoneIncomingFromNonRail()) {
				if (!SIDES_TOP[mFacing] && !invempty()) {
					DelegatorTileEntity tDelegator = getAdjacentTileEntity(mFacing);
					if (tDelegator.getBlock() instanceof BaseRailBlock) {
						List tList = level.getEntities((net.minecraft.world.entity.Entity)null, tDelegator.box(0, 0, 0, 1, 1, 1), net.minecraft.world.entity.EntitySelector.CONTAINER_ENTITY_SELECTOR);
						if (tList != null && !tList.isEmpty()) tDelegator = new DelegatorTileEntity<>((Container)tList.get(0), tDelegator);
					}
					while (tMovedItems + (mMode<=0?1:mMode) <= 64) {
						mLock = T;
						int tMoved = ST.move(delegator(mFacing), tDelegator, null, F, F, F, T, 64, 1, mMode<=0?64-tMovedItems:mMode, mMode<=0?1:mMode);
						mLock = F;
						if (tMoved <= 0) break;
						tMovedItems += tMoved;
						if (mExactMode) break;
					}
				}
				DelegatorTileEntity tDelegator = getAdjacentTileEntity(SIDE_TOP);
				if (tDelegator.getBlock() instanceof BaseRailBlock) {
					List tList = level.getEntities((net.minecraft.world.entity.Entity)null, tDelegator.box(0, 0, 0, 1, 1, 1), net.minecraft.world.entity.EntitySelector.CONTAINER_ENTITY_SELECTOR);
					if (tList != null && !tList.isEmpty()) tDelegator = new DelegatorTileEntity<>((Container)tList.get(0), tDelegator);
				}
				if (tDelegator.mTileEntity != null && !(tDelegator.mTileEntity instanceof MultiTileEntityAnvil)) {
					tMovedItems += ST.move(tDelegator, delegator(SIDE_TOP));
				} else {
					if (!WD.visOpq(tDelegator.getWorld(), tDelegator.getX(), tDelegator.getY(), tDelegator.getZ(), F, T)) {
						int i = invsize();
						while (i-->0) if (!slotHas(i)) {
							slot(i, WD.suck(tDelegator));
							if (slotHas(i)) {
								tMovedItems += slot(i).getCount();
								updateInventory();
							}
							break;
						}
					}
				}
				if (tMovedItems > 0) {
					mCheck =  3;
				} else {
					mCheck = -1;
				}
				if (mInventoryChanged) {
					for (int i = 0, k = invsize(), l = getMaxStackSize(); i < k; i++) for (int j = i+1; j < k; j++) if (slotHas(j)) {
						int tMaxSize = Math.min(l, slot(j).getMaxStackSize());
						if (slotHas(i)) {
							if (slot(i).getCount() < tMaxSize && ST.equal(slot(i), slot(j))) {
								tMovedItems += ST.move(this, j, i);
								if (slot(i).getCount() >= tMaxSize) break;
							}
						} else {
							tMovedItems += ST.move(this, j, i);
							if (slotHas(i) && slot(i).getCount() >= tMaxSize) break;
						}
					}
				}
				if (tMovedItems > 0) {
					for (byte tSide : ALL_SIDES_BUT_TOP) if (tSide != mFacing) {
						DelegatorTileEntity<BlockEntity> tDelegatorUpdate = getAdjacentTileEntity(tSide);
						if (tDelegatorUpdate.mTileEntity instanceof ITileEntityAdjacentInventoryUpdatable) {
							((ITileEntityAdjacentInventoryUpdatable)tDelegatorUpdate.mTileEntity).adjacentInventoryUpdated(tDelegatorUpdate.mSideOfTileEntity, this);
						}
					}
				}
			}
		}
	}
	
	@Override public float getSurfaceDistance       (byte aSide) {return 0.0F;}
	@Override public float getSurfaceSize           (byte aSide) {return SIDES_TOP[aSide]?PX_N[0]:PX_N[8];}
	@Override public float getSurfaceSizeAttachable (byte aSide) {return SIDES_TOP[aSide]?PX_N[2]:PX_N[8];}
	@Override public boolean isSurfaceSolid         (byte aSide) {return SIDES_TOP[aSide];}
	@Override public boolean isSurfaceOpaque2       (byte aSide) {return SIDES_TOP[aSide];}
	@Override public boolean isSideSolid2           (byte aSide) {return SIDES_TOP[aSide];}
	@Override public boolean allowCovers            (byte aSide) {return SIDES_TOP[aSide];}
	
	@Override public int[] getAccessibleSlotsFromSide2(byte aSide) {return UT.Code.getAscendingArray(invsize());}
	@Override public boolean canInsertItem2(int aSlot, ItemStack aStack, byte aSide) {return aSide != mFacing;}
	@Override public boolean canExtractItem2(int aSlot, ItemStack aStack, byte aSide) {return mLock || aSide != mFacing;}
	@Override public int getMaxStackSize() {return mMode<=0?64:mMode*Math.max(1, 64/mMode);}
	@Override public int getInventoryStackLimitGUI(int aSlot) {return mMode<=0?64:mMode*Math.max(1, 64/mMode);}
	@Override public boolean canDrop(int aInventorySlot) {return T;}
	
	@Override public void adjacentInventoryUpdated(byte aSide, Container aTileEntity) {if (SIDES_TOP[aSide] || aSide == mFacing) if (mCheck < 0) mCheck = 0;}
	
	@Override public byte getDefaultSide() {return SIDE_BOTTOM;}
	@Override public boolean[] getValidSides() {return SIDES_VALID;}
	@Override public boolean useSidePlacementRotation       () {return T;}
	@Override public boolean useInversePlacementRotation    () {return T;}
	
	@Override public int getRenderPasses2(Block aBlock, boolean[] aShouldSideBeRendered) {return SIDES_TOP[mFacing] ? 2 : 3;}
	@Override public boolean usesRenderPass2(int aRenderPass, boolean[] aShouldSideBeRendered) {return T;}
	
	@Override
	public boolean setBlockBounds2(Block aBlock, int aRenderPass, boolean[] aShouldSideBeRendered) {
		switch(aRenderPass) {
		case 0: box(aBlock, PX_P[ 0], PX_P[10], PX_P[ 0], PX_N[ 0], PX_N[ 0], PX_N[ 0]); return T;
		case 1: box(aBlock, PX_P[ 4], PX_P[ 4], PX_P[ 4], PX_N[ 4], PX_N[ 6], PX_N[ 4]); return T;
		case 2:
			switch(mFacing) {
			case SIDE_Y_NEG: box(aBlock, PX_P[ 6], PX_P[ 0], PX_P[ 6], PX_N[ 6], PX_N[12], PX_N[ 6]); return T;
			case SIDE_Z_NEG: box(aBlock, PX_P[ 6], PX_P[ 4], PX_P[ 0], PX_N[ 6], PX_N[ 8], PX_N[12]); return T;
			case SIDE_Z_POS: box(aBlock, PX_P[ 6], PX_P[ 4], PX_P[12], PX_N[ 6], PX_N[ 8], PX_N[ 0]); return T;
			case SIDE_X_NEG: box(aBlock, PX_P[ 0], PX_P[ 4], PX_P[ 6], PX_N[12], PX_N[ 8], PX_N[ 6]); return T;
			case SIDE_X_POS: box(aBlock, PX_P[12], PX_P[ 4], PX_P[ 6], PX_N[ 0], PX_N[ 8], PX_N[ 6]); return T;
			}
		}
		return T;
	}
	
	@Override
	public ITexture getTexture2(Block aBlock, int aRenderPass, byte aSide, boolean[] aShouldSideBeRendered) {
		return (aRenderPass == 1 ? !SIDES_TOP[aSide] : aShouldSideBeRendered[aSide] || (aRenderPass == 0 && SIDES_BOTTOM[aSide]) || (aRenderPass == 2 && aSide != mFacing)) ? BlockTextureMulti.get(BlockTextureDefault.get(sColoreds[FACES_TBS[aSide]], mRGBa, mMaterial.contains(TD.Properties.GLOWING)), BlockTextureDefault.get(sOverlays[FACES_TBS[aSide]])) : null;
	}
	
	// Icons
	public static IIconContainer sColoreds[] = new IIconContainer[] {
		new Textures.BlockIcons.CustomIcon("machines/automation/hopper/colored/bottom"),
		new Textures.BlockIcons.CustomIcon("machines/automation/hopper/colored/top"),
		new Textures.BlockIcons.CustomIcon("machines/automation/hopper/colored/side"),
	}, sOverlays[] = new IIconContainer[] {
		new Textures.BlockIcons.CustomIcon("machines/automation/hopper/overlay/bottom"),
		new Textures.BlockIcons.CustomIcon("machines/automation/hopper/overlay/top"),
		new Textures.BlockIcons.CustomIcon("machines/automation/hopper/overlay/side"),
	};
	
	@Override public String getTileEntityName() {return "gt.multitileentity.hopper";}
	
	@Override public int getLightOpacity() {return LIGHT_OPACITY_WATER;}
	
	@Override public Object getGUIClient2(int aGUIID, Player aPlayer) {return new ContainerClientDefault(aPlayer.getInventory(), this, aGUIID);}
	@Override public Object getGUIServer2(int aGUIID, Player aPlayer) {return new ContainerCommonDefault(aPlayer.getInventory(), this, aGUIID);}
}

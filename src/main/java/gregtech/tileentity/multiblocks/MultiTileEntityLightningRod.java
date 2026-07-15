/**
 * Copyright (c) 2025 GregTech-6 Team
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

package gregtech.tileentity.multiblocks;

import gregapi.code.ArrayListNoNulls;
import gregapi.code.TagData;
import gregapi.data.LH;
import gregapi.data.LH.Chat;
import gregapi.data.TD;
import gregapi.tileentity.energy.ITileEntityEnergy;
import gregapi.tileentity.energy.ITileEntityEnergyDataCapacitor;
import gregapi.tileentity.multiblocks.IMultiBlockEnergy;
import gregapi.tileentity.multiblocks.ITileEntityMultiBlockController;
import gregapi.tileentity.multiblocks.MultiTileEntityMultiBlockPart;
import gregapi.tileentity.multiblocks.TileEntityBase10MultiBlockBase;
import gregapi.util.UT;
import gregapi.util.WD;
import net.minecraft.world.entity.Entity;
import net.minecraft.entity.effect.EntityLightningBolt;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.BlockPos;

import java.util.Collection;
import java.util.List;

import static gregapi.data.CS.*;

/**
 * @author Gregorius Techneticies
 */
public class MultiTileEntityLightningRod extends TileEntityBase10MultiBlockBase implements ITileEntityEnergy, ITileEntityEnergyDataCapacitor, IMultiBlockEnergy {
	public long mEnergy = 0, mCapacity = 0;
	public byte mSize = 0;
	public TagData mEnergyTypeEmitted = TD.Energy.EU;
	
	public static List<MultiTileEntityLightningRod> ALL_LIGHTNING_RODS = new ArrayListNoNulls<>();
	
	@Override
	public void readFromNBT2(CompoundTag aNBT) {
		super.readFromNBT2(aNBT);
		mEnergy = aNBT.getLongOr(NBT_ENERGY, 0L);
		mCapacity = aNBT.getLongOr(NBT_CAPACITY, 0L);
		if (aNBT.contains(NBT_ENERGY_EMITTED)) mEnergyTypeEmitted = TagData.createTagData(aNBT.getStringOr(NBT_ENERGY_EMITTED, ""));
	}
	
	@Override
	public void writeToNBT2(CompoundTag aNBT) {
		super.writeToNBT2(aNBT);
		UT.NBT.setNumber(aNBT, NBT_ENERGY, mEnergy);
	}
	
	@Override
	public boolean checkStructure2(BlockPos aCoordinates, Entity aPlayer, Container aInventory) {
		boolean tSuccess = T;
		mSize = 0;
		
		for (int i = -1; i < 2; i++) for (int j = -1; j < 2; j++) {
			if (!ITileEntityMultiBlockController.Util.checkAndSetTargetOffset(this, i, 0, j, 18004, getMultiTileEntityRegistryID(), 0, MultiTileEntityMultiBlockPart.NOTHING, aCoordinates, aPlayer, aInventory)) tSuccess = F;
			if (!ITileEntityMultiBlockController.Util.checkAndSetTargetOffset(this, i, 1, j, 18041, getMultiTileEntityRegistryID(), 0, MultiTileEntityMultiBlockPart.NOTHING, aCoordinates, aPlayer, aInventory)) tSuccess = F;
			if (!ITileEntityMultiBlockController.Util.checkAndSetTargetOffset(this, i, 2, j, 18004, getMultiTileEntityRegistryID(), 0, MultiTileEntityMultiBlockPart.NOTHING, aCoordinates, aPlayer, aInventory)) tSuccess = F;
			if (!ITileEntityMultiBlockController.Util.checkAndSetTargetOffset(this, i, 3, j, 18041, getMultiTileEntityRegistryID(), 0, MultiTileEntityMultiBlockPart.NOTHING, aCoordinates, aPlayer, aInventory)) tSuccess = F;
			if (!ITileEntityMultiBlockController.Util.checkAndSetTargetOffset(this, i, 4, j, 18004, getMultiTileEntityRegistryID(), 0, MultiTileEntityMultiBlockPart.NOTHING, aCoordinates, aPlayer, aInventory)) tSuccess = F;
		}
		
		if (tSuccess) while (ITileEntityMultiBlockController.Util.checkAndSetTargetOffset(this, 0, 5+mSize, 0, 18104, getMultiTileEntityRegistryID(), 0, MultiTileEntityMultiBlockPart.NOTHING, aCoordinates, aPlayer, aInventory)) mSize++;
		return tSuccess;
	}
	
	static {
		LH.add("gt.tooltip.multiblock.lightningrod.1", "Bottom: 3x3 of Tungsten Walls with Main at Center");
		LH.add("gt.tooltip.multiblock.lightningrod.2", "Then: Full 3x3 of Large Niobium-Titanium Coils");
		LH.add("gt.tooltip.multiblock.lightningrod.3", "Then: Full 3x3 of Tungsten Walls");
		LH.add("gt.tooltip.multiblock.lightningrod.4", "Then: Full 3x3 of Large Niobium-Titanium Coils");
		LH.add("gt.tooltip.multiblock.lightningrod.5", "Top: 3x3 of Tungsten Walls");
		LH.add("gt.tooltip.multiblock.lightningrod.6", "Centered Above: 1x1 Pillar of simple Lightning Rod Blocks");
		LH.add("gt.tooltip.multiblock.lightningrod.7", "The Tip of the Rod has to be at Y = 100 or above");
		LH.add("gt.tooltip.multiblock.lightningrod.8", "Optimum Efficiency at a Rod Length of 100m");
		LH.add("gt.tooltip.multiblock.lightningrod.9", "Reduced Efficiency if too close to another Lightning Rod (256m)");
	}
	
	@Override
	public void addToolTips(List<String> aList, ItemStack aStack, boolean aF3_H) {
		aList.add(Chat.CYAN     + LH.get(LH.STRUCTURE) + ":");
		aList.add(Chat.WHITE    + LH.get("gt.tooltip.multiblock.lightningrod.1"));
		aList.add(Chat.WHITE    + LH.get("gt.tooltip.multiblock.lightningrod.2"));
		aList.add(Chat.WHITE    + LH.get("gt.tooltip.multiblock.lightningrod.3"));
		aList.add(Chat.WHITE    + LH.get("gt.tooltip.multiblock.lightningrod.4"));
		aList.add(Chat.WHITE    + LH.get("gt.tooltip.multiblock.lightningrod.5"));
		aList.add(Chat.WHITE    + LH.get("gt.tooltip.multiblock.lightningrod.6"));
		aList.add(Chat.WHITE    + LH.get("gt.tooltip.multiblock.lightningrod.7"));
		aList.add(Chat.YELLOW   + LH.get("gt.tooltip.multiblock.lightningrod.8"));
		aList.add(Chat.ORANGE   + LH.get("gt.tooltip.multiblock.lightningrod.9"));
		aList.add(Chat.GREEN    + LH.get(LH.ENERGY_OUTPUT) + ": " + Chat.WHITE + VREC[6] + " " + mEnergyTypeEmitted.getLocalisedChatNameShort() + Chat.WHITE + "/p (up to 16 Amps)");
		aList.add(Chat.WHITE    + mCapacity + " " + mEnergyTypeEmitted.getLocalisedChatNameShort() + Chat.GRAY + " per Lightning Strike");
		super.addToolTips(aList, aStack, aF3_H);
	}
	
	@Override
	public boolean isInsideStructure(int aX, int aY, int aZ) {
		return aY >= getBlockPos().getY() && aX >= getBlockPos().getX() - 1 && aZ >= getBlockPos().getZ() - 1 && aX <= getBlockPos().getX() + 1 && aZ <= getBlockPos().getZ() + 1 && (aY < getBlockPos().getY() + 5 || (aX == getBlockPos().getX() && aZ == getBlockPos().getZ() && aY <= getBlockPos().getY() + mSize + 4));
	}
	
	@Override
	public void onTick2(long aTimer, boolean aIsServerSide) {
		super.onTick2(aTimer, aIsServerSide);
		if (aIsServerSide) {
			// Required to use force sometimes, because of variable Rod Size, and newly placed Rod Blocks don't cause Multiblock Updates due to Lag.
			if (checkStructure(mSize < 100 && aTimer % 1200 == 300 && (level.isRaining() || level.isThundering()))) {
				if (mEnergy >= VREC[6]) {
					mEnergy -= Math.max(1, ITileEntityEnergy.Util.emitEnergyToSide(mEnergyTypeEmitted, SIDE_BOTTOM, VREC[6], 16, this)) * VREC[6];
				} else {
					mEnergy = 0;
					if (mSize > 0 && getBlockPos().getY() + mSize >= 100 && rng(1000000) < Math.min(100, mSize) && (level.isThundering() || (level.isRaining() && rng(10) == 0))) {
						int tCount = 1;
						for (MultiTileEntityLightningRod tLightningRod : ALL_LIGHTNING_RODS) if (tLightningRod != this && tLightningRod.mSize > 0 && tLightningRod.getWorld() == level && Math.abs(tLightningRod.getBlockPos().getX() - getBlockPos().getX()) < 256 && Math.abs(tLightningRod.getBlockPos().getZ() - getBlockPos().getZ()) < 256) tCount++;
						if (rng(tCount) == 0) {
							boolean temp = T;
							for (int i = getBlockPos().getY() + mSize + 5, j = level.getHeight(); i < j; i++) {
								if (!WD.air(level, getBlockPos().getX(), i, getBlockPos().getZ())) {
									temp = F;
									break;
								}
							}
							if (temp) {
								level.addWeatherEffect(new EntityLightningBolt(level, getBlockPos().getX(), getBlockPos().getY()+mSize+4, getBlockPos().getZ()));
								mEnergy = mCapacity;
							}
						}
					}
				}
			} else {
				mEnergy = 0;
			}
		}
	}
	
	@Override
	public void onTickFirst2(boolean aIsServerSide) {
		super.onTickFirst2(aIsServerSide);
		if (aIsServerSide && !ALL_LIGHTNING_RODS.contains(this)) ALL_LIGHTNING_RODS.add(this);
	}
	
	@Override
	public void clearRemoved() {
		super.clearRemoved();
		if (isServerSide() && !ALL_LIGHTNING_RODS.contains(this)) ALL_LIGHTNING_RODS.add(this);
	}
	
	@Override
	public void setRemoved() {
		if (isServerSide()) ALL_LIGHTNING_RODS.remove(this);
		super.setRemoved();
	}
	
	@Override
	public void onChunkUnloaded() {
		if (isServerSide()) ALL_LIGHTNING_RODS.remove(this);
		super.onChunkUnloaded();
	}
	
	@Override public byte getDefaultSide() {return SIDE_BOTTOM;}
	@Override public boolean[] getValidSides() {return SIDES_BOTTOM;}
	
	@Override public boolean isEnergyType(TagData aEnergyType, byte aSide, boolean aEmitting) {return aEmitting && aEnergyType == mEnergyTypeEmitted;}
	@Override public boolean isEnergyEmittingTo(TagData aEnergyType, byte aSide, boolean aTheoretical) {return isEnergyType(aEnergyType, aSide, F);}
	@Override public boolean isEnergyCapacitorType(TagData aEnergyType, byte aSide) {return aEnergyType == mEnergyTypeEmitted;}
	@Override public long getEnergySizeOutputRecommended(TagData aEnergyType, byte aSide) {return 2048;}
	@Override public long getEnergySizeOutputMin(TagData aEnergyType, byte aSide) {return 1024;}
	@Override public long getEnergySizeOutputMax(TagData aEnergyType, byte aSide) {return 4096;}
	@Override public long getEnergyStored(TagData aEnergyType, byte aSide) {return aEnergyType == mEnergyTypeEmitted ? mEnergy : 0;}
	@Override public long getEnergyCapacity(TagData aEnergyType, byte aSide) {return aEnergyType == mEnergyTypeEmitted ? mCapacity : 0;}
	@Override public Collection<TagData> getEnergyTypes(byte aSide) {return mEnergyTypeEmitted.AS_LIST;}
	@Override public Collection<TagData> getEnergyCapacitorTypes(byte aSide) {return mEnergyTypeEmitted.AS_LIST;}
	
	@Override public boolean canDrop(int aInventorySlot) {return T;}
	
	@Override public String getTileEntityName() {return "gt.multitileentity.multiblock.lightningrod";}
}

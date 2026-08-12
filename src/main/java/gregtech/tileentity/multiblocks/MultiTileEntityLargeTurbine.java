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
 *
 * Modified in 2026 for the GregTech 6 NeoForge port
 * (https://github.com/wolfram0108/gregtech6_w): ported from Minecraft 1.7.10 / Forge
 * to Minecraft 26.1.2 / NeoForge.
 */

package gregtech.tileentity.multiblocks;

import gregapi.util.WD;
import gregapi.code.TagData;
import gregapi.old.Textures;
import gregapi.render.BlockTextureDefault;
import gregapi.render.IIconContainer;
import gregapi.render.ITexture;
import gregapi.tileentity.ITileEntityUnloadable;
import gregapi.tileentity.machines.ITileEntitySwitchableOnOff;
import gregapi.tileentity.multiblocks.IMultiBlockFluidHandler;
import gregapi.tileentity.multiblocks.ITileEntityMultiBlockController;
import gregapi.tileentity.multiblocks.MultiTileEntityMultiBlockPart;
import gregapi.tileentity.multiblocks.TileEntityBase11MultiBlockConverter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.Container;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraftforge.fluids.capability.IFluidHandler;

import static gregapi.data.CS.*;

/**
 * @author Gregorius Techneticies
 */
public abstract class MultiTileEntityLargeTurbine extends TileEntityBase11MultiBlockConverter implements IMultiBlockFluidHandler, IFluidHandler, ITileEntitySwitchableOnOff {
	public short mTurbineWalls = 18022;
	
	public static final IIconContainer mTextureActive   = new Textures.BlockIcons.CustomIcon("machines/multiblockmains/turbine_active");
	public static final IIconContainer mTextureInactive = new Textures.BlockIcons.CustomIcon("machines/multiblockmains/turbine");
	
	@Override
	public void readFromNBT2(CompoundTag aNBT) {
		super.readFromNBT2(aNBT);
		if (aNBT.contains(NBT_DESIGN)) mTurbineWalls = aNBT.getShortOr(NBT_DESIGN, (short)0);
	}
	
	@Override
	public boolean checkStructure2(BlockPos aCoordinates, Entity aPlayer, Container aInventory) {
		int
		tMinX = getBlockPos().getX()-(SIDE_X_NEG==mFacing?0:SIDE_X_POS==mFacing?3:1),
		tMinY = getBlockPos().getY()-(SIDE_Y_NEG==mFacing?0:SIDE_Y_POS==mFacing?3:1),
		tMinZ = getBlockPos().getZ()-(SIDE_Z_NEG==mFacing?0:SIDE_Z_POS==mFacing?3:1),
		tMaxX = getBlockPos().getX()+(SIDE_X_POS==mFacing?0:SIDE_X_NEG==mFacing?3:1),
		tMaxY = getBlockPos().getY()+(SIDE_Y_POS==mFacing?0:SIDE_Y_NEG==mFacing?3:1),
		tMaxZ = getBlockPos().getZ()+(SIDE_Z_POS==mFacing?0:SIDE_Z_NEG==mFacing?3:1),
		tOutX = getOffsetXN(mFacing, 3),
		tOutY = getOffsetYN(mFacing, 3),
		tOutZ = getOffsetZN(mFacing, 3);
		
		if (WD.exists(level, tMinX, tMinY, tMinZ) && WD.exists(level, tMaxX, tMaxY, tMaxZ)) {
			mEmitter = null;
			boolean tSuccess = T;
			for (int tX = tMinX; tX <= tMaxX; tX++) for (int tY = tMinY; tY <= tMaxY; tY++) for (int tZ = tMinZ; tZ <= tMaxZ; tZ++) {
				int tBits = 0;
				if (tX == tOutX && tY == tOutY && tZ == tOutZ) {
					tBits = MultiTileEntityMultiBlockPart.ONLY_ENERGY_OUT;
				} else {
					if ((SIDES_AXIS_X[mFacing] && tX == getBlockPos().getX()) || (SIDES_AXIS_Y[mFacing] && tY == getBlockPos().getY()) || (SIDES_AXIS_Z[mFacing] && tZ == getBlockPos().getZ())) {
						tBits = (tY == tMinY ? MultiTileEntityMultiBlockPart.ONLY_ITEM_FLUID     : MultiTileEntityMultiBlockPart.ONLY_ITEM_FLUID_IN);
					} else {
						tBits = (tY == tMinY ? MultiTileEntityMultiBlockPart.ONLY_ITEM_FLUID_OUT : MultiTileEntityMultiBlockPart.NOTHING);
					}
				}
				if (!ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX, tY, tZ, mTurbineWalls, getMultiTileEntityRegistryID(), tX == tOutX && tY == tOutY && tZ == tOutZ ? 3 : 0, tBits, aCoordinates, aPlayer, aInventory)) tSuccess = F;
			}
			return tSuccess;
		}
		return mStructureOkay;
	}
	
	@Override
	public boolean allowCovers(byte aSide) {
		return aSide != mFacing;
	}
	
	@Override
	public boolean isInsideStructure(int aX, int aY, int aZ) {
		return
		aX >= getBlockPos().getX()-(SIDE_X_NEG==mFacing?0:SIDE_X_POS==mFacing?3:1) &&
		aY >= getBlockPos().getY()-(SIDE_Y_NEG==mFacing?0:SIDE_Y_POS==mFacing?3:1) &&
		aZ >= getBlockPos().getZ()-(SIDE_Z_NEG==mFacing?0:SIDE_Z_POS==mFacing?3:1) &&
		aX <= getBlockPos().getX()+(SIDE_X_POS==mFacing?0:SIDE_X_NEG==mFacing?3:1) &&
		aY <= getBlockPos().getY()+(SIDE_Y_POS==mFacing?0:SIDE_Y_NEG==mFacing?3:1) &&
		aZ <= getBlockPos().getZ()+(SIDE_Z_POS==mFacing?0:SIDE_Z_NEG==mFacing?3:1);
	}
	
	@Override
	public int getRenderPasses2(Block aBlock, boolean[] aShouldSideBeRendered) {
		return mStructureOkay ? 2 : 1;
	}
	
	@Override
	public boolean setBlockBounds2(Block aBlock, int aRenderPass, boolean[] aShouldSideBeRendered) {
		if (aRenderPass == 1) switch(mFacing) {
		case SIDE_X_NEG: case SIDE_X_POS: return box(aBlock, -0.001, -0.999, -0.999,  1.001,  1.999,  1.999);
		case SIDE_Y_NEG: case SIDE_Y_POS: return box(aBlock, -0.999, -0.001, -0.999,  1.999,  1.001,  1.999);
		case SIDE_Z_NEG: case SIDE_Z_POS: return box(aBlock, -0.999, -0.999, -0.001,  1.999,  1.999,  1.001);
		}
		return F;
	}
	
	@Override
	public ITexture getTexture2(Block aBlock, int aRenderPass, byte aSide, boolean[] aShouldSideBeRendered) {
		return aRenderPass == 0 ? super.getTexture2(aBlock, aRenderPass, aSide, aShouldSideBeRendered) : aSide != mFacing ? null : BlockTextureDefault.get(mActivity.mState > 0 ? mTextureActive : mTextureInactive);
	}
	
	public ITileEntityUnloadable mEmitter = null;
	
	@Override public BlockEntity getEmittingTileEntity() {if (mEmitter == null || mEmitter.isDead()) {mEmitter = null; BlockEntity tTileEntity = getTileEntityAtSideAndDistance(OPOS[mFacing], 3); if (tTileEntity instanceof ITileEntityUnloadable) mEmitter = (ITileEntityUnloadable)tTileEntity;} return mEmitter == null ? this : (BlockEntity)mEmitter;}
	@Override public byte getEmittingSide() {return OPOS[mFacing];}
	@Override public boolean isInput (byte aSide) {return aSide == mFacing;}
	@Override public boolean isOutput(byte aSide) {return aSide == OPOS[mFacing];}
	
	@Override public byte getDefaultSide() {return SIDE_FRONT;}
	@Override public boolean[] getValidSides() {return SIDES_VALID;}
	
	@Override public boolean isEnergyType                   (TagData aEnergyType, byte aSide, boolean aEmitting) {return aEmitting && mEnergyOUT.isType(aEnergyType);}
	@Override public boolean isEnergyAcceptingFrom          (TagData aEnergyType, byte aSide, boolean aTheoretical) {return F;}
	
	@Override public boolean canDrop(int aInventorySlot) {return F;}
}

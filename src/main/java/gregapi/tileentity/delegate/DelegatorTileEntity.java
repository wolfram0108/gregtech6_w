/**
 * Copyright (c) 2021 GregTech-6 Team
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

package gregapi.tileentity.delegate;

import static gregapi.data.CS.*;

import gregapi.random.WorldAndCoords;
import gregapi.tileentity.ITileEntityUnloadable;
import gregapi.util.UT;
import gregapi.util.WD;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.core.Direction;

/**
 * @author Gregorius Techneticies
 */
public final class DelegatorTileEntity<T> extends WorldAndCoords {
	/** the TileEntity. This should be an instance of TileEntity. */
	public final T mTileEntity;
	/** the Side of the Delegate responsible for handling. So a TE-Tesseract alike can go a curve. */
	public final byte mSideOfTileEntity;
	
	public DelegatorTileEntity(DelegatorTileEntity<T> aDelegator) {
		super(aDelegator.mWorld, aDelegator.mX, aDelegator.mY, aDelegator.mZ);
		mTileEntity = aDelegator.mTileEntity;
		mSideOfTileEntity = aDelegator.mSideOfTileEntity;
	}
	
	public DelegatorTileEntity(T aTileEntity, byte aSideOfTileEntity) {
		super((BlockEntity)aTileEntity);
		mTileEntity = aTileEntity;
		mSideOfTileEntity = aSideOfTileEntity;
	}
	
	public DelegatorTileEntity(T aTileEntity, DelegatorTileEntity<?> aDelegator) {
		super(aDelegator.mWorld, aDelegator.mX, aDelegator.mY, aDelegator.mZ);
		mTileEntity = aTileEntity;
		mSideOfTileEntity = aDelegator.mSideOfTileEntity;
	}
	
	public DelegatorTileEntity(T aTileEntity, Level aWorld, int aX, int aY, int aZ, byte aSideOfTileEntity) {
		super(aWorld, aX, aY, aZ);
		mTileEntity = aTileEntity;
		mSideOfTileEntity = aSideOfTileEntity;
	}
	
	public DelegatorTileEntity(T aTileEntity, Level aWorld, BlockPos aCoords, byte aSideOfTileEntity) {
		super(aWorld, aCoords.getX(), aCoords.getY(), aCoords.getZ());
		mTileEntity = aTileEntity;
		mSideOfTileEntity = aSideOfTileEntity;
	}
	
	public AABB box(double aMinX, double aMinY, double aMinZ, double aMaxX, double aMaxY, double aMaxZ) {return new AABB(mX+aMinX, mY+aMinY, mZ+aMinZ, mX+aMaxX, mY+aMaxY, mZ+aMaxZ);}
	public AABB box(double[] aBox) {return new AABB(mX+aBox[0], mY+aBox[1], mZ+aBox[2], mX+aBox[3], mY+aBox[4], mZ+aBox[5]);}
	public AABB box(float[] aBox) {return new AABB(mX+aBox[0], mY+aBox[1], mZ+aBox[2], mX+aBox[3], mY+aBox[4], mZ+aBox[5]);}
	public AABB box() {return new AABB(mX, mY, mZ, mX+1, mY+1, mZ+1);}
	
	public Direction getForgeSideOfTileEntity() {return FORGE_DIR[mSideOfTileEntity];}
	public Block getBlock() {return WD.block(mWorld, mX, mY, mZ);}
	public byte getMetaData() {return UT.Code.bind4(WD.meta(mWorld, mX, mY, mZ));}
	public boolean setBlock(Block aBlock) {return WD.set(mWorld, mX, mY, mZ, aBlock, 0, 3);}
	public boolean setBlock(Block aBlock, int aMetaData) {return WD.set(mWorld, mX, mY, mZ, aBlock, UT.Code.bind4(aMetaData), 3);}
	public boolean setBlock(Block aBlock, int aMetaData, int aFlags) {return WD.set(mWorld, mX, mY, mZ, aBlock, UT.Code.bind4(aMetaData), aFlags);}
	public boolean setMetaData(int aMetaData) {return WD.set(mWorld, mX, mY, mZ, WD.block(mWorld, mX, mY, mZ), UT.Code.bind4(aMetaData), 3, F);}
	public boolean setMetaData(int aMetaData, int aFlags) {return WD.set(mWorld, mX, mY, mZ, WD.block(mWorld, mX, mY, mZ), UT.Code.bind4(aMetaData), aFlags, F);}
	
	public boolean hasCollisionBox() {return mWorld != null && WD.hasCollide(mWorld, mX, mY, mZ);}
	
	public boolean equalCoords(DelegatorTileEntity<?> aOther) {return aOther.mX == mX && aOther.mY == mY && aOther.mZ == mZ;}
	public boolean equalSideAndCoords(DelegatorTileEntity<?> aOther) {return aOther.mSideOfTileEntity == mSideOfTileEntity && equalCoords(aOther);}
	public boolean equalSideWorldAndCoords(DelegatorTileEntity<?> aOther) {return aOther.mWorld == mWorld && equalSideAndCoords(aOther);}
	public boolean equalSideTileEntityAndCoords(DelegatorTileEntity<?> aOther) {return aOther.mTileEntity == mTileEntity && equalSideAndCoords(aOther);}
	
	public boolean exists() {return mTileEntity instanceof ITileEntityUnloadable ? !((ITileEntityUnloadable)mTileEntity).isDead() : mTileEntity != null && !((BlockEntity)mTileEntity).isRemoved() && mWorld != null && WD.exists(mWorld, mX, mY, mZ);}
	
	@Override public Level getWorld() {return mWorld;}
	@Override public int getX() {return mX;}
	@Override public int getY() {return mY;}
	@Override public int getZ() {return mZ;}
	@Override public int getOffsetX (byte aSide) {return mX + OFFX[aSide];}
	@Override public int getOffsetY (byte aSide) {return mY + OFFY[aSide];}
	@Override public int getOffsetZ (byte aSide) {return mZ + OFFZ[aSide];}
	@Override public int getOffsetX (byte aSide, int aMultiplier) {return mX + OFFX[aSide] * aMultiplier;}
	@Override public int getOffsetY (byte aSide, int aMultiplier) {return mY + OFFY[aSide] * aMultiplier;}
	@Override public int getOffsetZ (byte aSide, int aMultiplier) {return mZ + OFFZ[aSide] * aMultiplier;}
	@Override public int getOffsetXN(byte aSide) {return mX - OFFX[aSide];}
	@Override public int getOffsetYN(byte aSide) {return mY - OFFY[aSide];}
	@Override public int getOffsetZN(byte aSide) {return mZ - OFFZ[aSide];}
	@Override public int getOffsetXN(byte aSide, int aMultiplier) {return mX - OFFX[aSide] * aMultiplier;}
	@Override public int getOffsetYN(byte aSide, int aMultiplier) {return mY - OFFY[aSide] * aMultiplier;}
	@Override public int getOffsetZN(byte aSide, int aMultiplier) {return mZ - OFFZ[aSide] * aMultiplier;}
	@Override public BlockPos getCoords() {return new BlockPos(mX, mY, mZ);}
	@Override public BlockPos getOffset (byte aSide, int aMultiplier) {return new BlockPos(getOffsetX (aSide, aMultiplier), getOffsetY (aSide, aMultiplier), getOffsetZ (aSide, aMultiplier));}
	@Override public BlockPos getOffsetN(byte aSide, int aMultiplier) {return new BlockPos(getOffsetXN(aSide, aMultiplier), getOffsetYN(aSide, aMultiplier), getOffsetZN(aSide, aMultiplier));}
	@Override public boolean isServerSide() {return mWorld == null ? cpw.mods.fml.common.FMLCommonHandler.instance().getEffectiveSide().isServer() : !mWorld.isClientSide();}
	@Override public boolean isClientSide() {return mWorld == null ? cpw.mods.fml.common.FMLCommonHandler.instance().getEffectiveSide().isClient() :  mWorld.isClientSide();}
	@Override public int rng(int aRange) {return RNGSUS.nextInt(aRange);}
	@Override public int getRandomNumber(int aRange) {return RNGSUS.nextInt(aRange);}
	@Override public BlockEntity getTileEntity   (int aX, int aY, int aZ) {return mWorld==null?null:WD.te(mWorld, aX, aY, aZ, T);}
	@Override public Block getBlock             (int aX, int aY, int aZ) {return mWorld==null?NB:WD.block(mWorld, aX, aY, aZ);}
	@Override public byte getMetaData           (int aX, int aY, int aZ) {return mWorld==null?0:UT.Code.bind4(WD.meta(mWorld, aX, aY, aZ));}
	@Override public byte getLightLevel         (int aX, int aY, int aZ) {return mWorld==null?0:UT.Code.bind4((long)WD.lightBrightness(mWorld, aX, aY, aZ)*15);}
	@Override public boolean getOpacity         (int aX, int aY, int aZ) {return mWorld!=null&&WD.opq(mWorld, aX, aY, aZ, T, F);}
	@Override public boolean getSky             (int aX, int aY, int aZ) {return mWorld==null||WD.canSeeSky(mWorld, aX, aY, aZ);}
	@Override public boolean getRain            (int aX, int aY, int aZ) {return mWorld==null||WD.precipitationHeight(mWorld, aX, aZ) <= aY;}
	@Override public boolean getAir             (int aX, int aY, int aZ) {return mWorld==null||WD.air(mWorld, aX, aY, aZ);}
	@Override public Biome getBiome      (int aX, int aZ) {return mWorld==null?null:WD.biome(mWorld, aX, aZ);}
	@Override public BlockEntity getTileEntity   (BlockPos aCoords) {return mWorld==null?null:WD.te(mWorld, aCoords.getX(), aCoords.getY(), aCoords.getZ(), T);}
	@Override public Block getBlock             (BlockPos aCoords) {return mWorld==null?NB:WD.block(mWorld, aCoords.getX(), aCoords.getY(), aCoords.getZ());}
	@Override public byte getMetaData           (BlockPos aCoords) {return mWorld==null?0:UT.Code.bind4(WD.meta(mWorld, aCoords.getX(), aCoords.getY(), aCoords.getZ()));}
	@Override public byte getLightLevel         (BlockPos aCoords) {return mWorld==null?0:UT.Code.bind4((long)WD.lightBrightness(mWorld, aCoords.getX(), aCoords.getY(), aCoords.getZ())*15);}
	@Override public boolean getOpacity         (BlockPos aCoords) {return mWorld!=null&&WD.opq(mWorld, aCoords.getX(), aCoords.getY(), aCoords.getZ(), T, F);}
	@Override public boolean getSky             (BlockPos aCoords) {return mWorld==null||WD.canSeeSky(mWorld, aCoords.getX(), aCoords.getY(), aCoords.getZ());}
	@Override public boolean getRain            (BlockPos aCoords) {return mWorld==null||WD.precipitationHeight(mWorld, aCoords.getX(), aCoords.getZ()) <= aCoords.getY();}
	@Override public boolean getAir             (BlockPos aCoords) {return mWorld==null||WD.air(mWorld, aCoords.getX(), aCoords.getY(), aCoords.getZ());}
	@Override public Biome getBiome      (BlockPos aCoords) {return mWorld==null?null:WD.biome(mWorld, aCoords.getX(), aCoords.getZ());}
}

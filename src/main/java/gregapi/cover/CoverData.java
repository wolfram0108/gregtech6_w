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
 */

package gregapi.cover;

import gregapi.code.ItemNBT;
import gregapi.tileentity.delegate.DelegatorTileEntity;
import gregapi.util.ST;
import gregapi.util.UT;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.phys.AABB;

import java.util.List;

import static gregapi.data.CS.*;

/**
 * @author Gregorius Techneticies
 */
public class CoverData {
	public short mIDs[], mMetas[], mVisuals[], mValues[];
	public boolean mVisualsToSync[] = new boolean[] {F,F,F,F,F,F}, mStopped = F;
	public CompoundTag mNBTs[];
	public ICover mBehaviours[] = new ICover[6];
	public ITileEntityCoverable mTileEntity;
	
	public CoverData(ITileEntityCoverable aTileEntity) {
		mIDs = new short[6]; mMetas = new short[6]; mVisuals = new short[6]; mValues = new short[6]; mNBTs = new CompoundTag[6];
		mTileEntity = aTileEntity;
	}
	
	public CoverData(short[] aIDs, short[] aMetas, short[] aVisuals, short[] aValues, CompoundTag[] aNBTs, boolean aStopped, ITileEntityCoverable aTileEntity) {
		mVisuals = aVisuals; mValues = aValues; mNBTs = aNBTs;
		for (int i = 0; i < mNBTs.length;i++) if (mNBTs[i] != null && mNBTs[i].isEmpty()) mNBTs[i] = null;
		setIDs(aIDs, aMetas);
		mStopped = aStopped;
		mTileEntity = aTileEntity;
		if (mTileEntity != null) try {for (byte tSide : ALL_SIDES_VALID) if (mBehaviours[tSide] != null) mBehaviours[tSide].onCoverLoaded(tSide, this);} catch(Throwable e) {e.printStackTrace(ERR); mTileEntity.setError("Cover Loaded:" + e);}
	}
	
	public CoverData(ITileEntityCoverable aTileEntity, CompoundTag aNBT) {
		this( new short[] {aNBT.getShort("a").orElse((short)0), aNBT.getShort("b").orElse((short)0), aNBT.getShort("c").orElse((short)0), aNBT.getShort("d").orElse((short)0), aNBT.getShort("e").orElse((short)0), aNBT.getShort("f").orElse((short)0)}
			, new short[] {aNBT.getShort("g").orElse((short)0), aNBT.getShort("h").orElse((short)0), aNBT.getShort("i").orElse((short)0), aNBT.getShort("j").orElse((short)0), aNBT.getShort("k").orElse((short)0), aNBT.getShort("l").orElse((short)0)}
			, new short[] {aNBT.getShort("m").orElse((short)0), aNBT.getShort("n").orElse((short)0), aNBT.getShort("o").orElse((short)0), aNBT.getShort("p").orElse((short)0), aNBT.getShort("q").orElse((short)0), aNBT.getShort("r").orElse((short)0)}
			, new short[] {aNBT.getShort("0").orElse((short)0), aNBT.getShort("1").orElse((short)0), aNBT.getShort("2").orElse((short)0), aNBT.getShort("3").orElse((short)0), aNBT.getShort("4").orElse((short)0), aNBT.getShort("5").orElse((short)0)}
			, new CompoundTag[] {aNBT.getCompoundOrEmpty("s"), aNBT.getCompoundOrEmpty("t"), aNBT.getCompoundOrEmpty("u"), aNBT.getCompoundOrEmpty("v"), aNBT.getCompoundOrEmpty("w"), aNBT.getCompoundOrEmpty("x")}
			, aNBT.getBoolean("y").orElse(false)
			, aTileEntity);
	}
	
	public CompoundTag writeToNBT() {return writeToNBT(UT.NBT.make(), T);}
	public CompoundTag writeToNBT(CompoundTag aNBT, boolean aIncludeVisuals) {
		byte i = 0;
		if (mIDs[  i] != 0) {
			aNBT.putShort("a", mIDs[i]);
			if (mMetas[i] != 0) aNBT.putShort("g", mMetas[i]);
			if (mValues[i] != 0) aNBT.putShort("0", mValues[i]);
			if (mNBTs[i] != null && !mNBTs[i].isEmpty()) aNBT.put("s", mNBTs[i]);
			if (mVisuals[i] != 0 && (aIncludeVisuals || (mBehaviours[i] != null && mBehaviours[i].needsVisualsSaved(i, this)))) aNBT.putShort("m", mVisuals[i]);
		}
		if (mIDs[++i] != 0) {
			aNBT.putShort("b", mIDs[i]);
			if (mMetas[i] != 0) aNBT.putShort("h", mMetas[i]);
			if (mValues[i] != 0) aNBT.putShort("1", mValues[i]);
			if (mNBTs[i] != null && !mNBTs[i].isEmpty()) aNBT.put("t", mNBTs[i]);
			if (mVisuals[i] != 0 && (aIncludeVisuals || (mBehaviours[i] != null && mBehaviours[i].needsVisualsSaved(i, this)))) aNBT.putShort("n", mVisuals[i]);
		}
		if (mIDs[++i] != 0) {
			aNBT.putShort("c", mIDs[i]);
			if (mMetas[i] != 0) aNBT.putShort("i", mMetas[i]);
			if (mValues[i] != 0) aNBT.putShort("2", mValues[i]);
			if (mNBTs[i] != null && !mNBTs[i].isEmpty()) aNBT.put("u", mNBTs[i]);
			if (mVisuals[i] != 0 && (aIncludeVisuals || (mBehaviours[i] != null && mBehaviours[i].needsVisualsSaved(i, this)))) aNBT.putShort("o", mVisuals[i]);
		}
		if (mIDs[++i] != 0) {
			aNBT.putShort("d", mIDs[i]);
			if (mMetas[i] != 0) aNBT.putShort("j", mMetas[i]);
			if (mValues[i] != 0) aNBT.putShort("3", mValues[i]);
			if (mNBTs[i] != null && !mNBTs[i].isEmpty()) aNBT.put("v", mNBTs[i]);
			if (mVisuals[i] != 0 && (aIncludeVisuals || (mBehaviours[i] != null && mBehaviours[i].needsVisualsSaved(i, this)))) aNBT.putShort("p", mVisuals[i]);
		}
		if (mIDs[++i] != 0) {
			aNBT.putShort("e", mIDs[i]);
			if (mMetas[i] != 0) aNBT.putShort("k", mMetas[i]);
			if (mValues[i] != 0) aNBT.putShort("4", mValues[i]);
			if (mNBTs[i] != null && !mNBTs[i].isEmpty()) aNBT.put("w", mNBTs[i]);
			if (mVisuals[i] != 0 && (aIncludeVisuals || (mBehaviours[i] != null && mBehaviours[i].needsVisualsSaved(i, this)))) aNBT.putShort("q", mVisuals[i]);
		}
		if (mIDs[++i] != 0) {
			aNBT.putShort("f", mIDs[i]); 
			if (mMetas[i] != 0) aNBT.putShort("l", mMetas[i]);
			if (mValues[i] != 0) aNBT.putShort("5", mValues[i]);
			if (mNBTs[i] != null && !mNBTs[i].isEmpty()) aNBT.put("x", mNBTs[i]);
			if (mVisuals[i] != 0 && (aIncludeVisuals || (mBehaviours[i] != null && mBehaviours[i].needsVisualsSaved(i, this)))) aNBT.putShort("r", mVisuals[i]);
		}
		if (mStopped) aNBT.putBoolean("y", mStopped);
		return aNBT;
	}
	
	public CoverData setIDs(short[] aIDs, short[] aMetas) {
		mIDs = aIDs; mMetas = aMetas;
		for (byte tSide : ALL_SIDES_VALID) if (mIDs[tSide] == 0) mBehaviours[tSide] = null; else mBehaviours[tSide] = CoverRegistry.get(mIDs[tSide], mMetas[tSide]);
		return this;
	}
	
	public CoverData set(byte aSide, ItemStack aStack) {
		return aStack == null ? set(aSide, (short)0, (short)0, null) : set(aSide, ST.id(aStack), ST.meta_(aStack), ItemNBT.get(aStack));
	}
	
	public CoverData set(byte aSide, short aID, short aMeta, CompoundTag aNBT) {
		mIDs[aSide] = aID; mMetas[aSide] = aMeta;
		if (aID == 0) mBehaviours[aSide] = null; else mBehaviours[aSide] = CoverRegistry.get(aID, aMeta);
		mNBTs[aSide] = (CompoundTag)(aNBT==null||aNBT.isEmpty()?null:aNBT.copy());
		return this;
	}
	
	public CoverData value(byte aSide, short aValue) {
		return value(aSide, aValue, F);
	}
	public CoverData value(byte aSide, short aValue, boolean aBlockUpdate) {
		if (mValues[aSide] != aValue) {
			mValues[aSide]  = aValue;
			if (aBlockUpdate) mTileEntity.sendBlockUpdateFromCover();
		}
		return this;
	}
	
	public CoverData visual(byte aSide, short aVisual) {
		return visual(aSide, aVisual, F);
	}
	public CoverData visual(byte aSide, short aVisual, boolean aBlockUpdate) {
		if (mVisuals[aSide] != aVisual) {
			mVisuals[aSide]  = aVisual;
			mVisualsToSync[aSide] = T;
			mTileEntity.updateCoverVisuals();
			if (aBlockUpdate) mTileEntity.sendBlockUpdateFromCover();
		}
		return this;
	}
	
	public boolean setStopped(boolean aStopped) {
		if (aStopped == mStopped) return F;
		mStopped = aStopped;
		for (byte tSide : ALL_SIDES_VALID) if (mBehaviours[tSide] != null) mBehaviours[tSide].onStoppedUpdate(tSide, this, mStopped);
		return T;
	}
	
	public boolean onBlockUpdate() {
		for (byte tSide : ALL_SIDES_VALID) if (mBehaviours[tSide] != null) mBehaviours[tSide].onBlockUpdate(tSide, this);
		return T;
	}
	
	public ItemStack getCoverItem(byte aSide) {
		return mIDs[aSide] == 0 ? null : mBehaviours[aSide] == null ? ST.make(mIDs[aSide], 1, mMetas[aSide], mNBTs[aSide]==null||mNBTs[aSide].isEmpty()?null:mNBTs[aSide]) : mBehaviours[aSide].getCoverItem(aSide, this);
	}
	
	public DelegatorTileEntity<ITileEntityCoverable> delegator(byte aSide) {
		return new DelegatorTileEntity<>(mTileEntity, aSide);
	}
	
	public boolean requiresSync() {
		return UT.Code.containsBoolean(T, mVisualsToSync);
	}
	
	public void resetSync() {
		for (int i = 0; i < mVisualsToSync.length; i++) mVisualsToSync[i] = F;
	}
	
	public void tickPre (long aTimer, boolean aIsServerSide, boolean aReceivedBlockUpdate, boolean aReceivedInventoryUpdate) {
		try {for (byte tSide : ALL_SIDES_VALID) if (mBehaviours[tSide] != null) mBehaviours[tSide].onTickPre (tSide, this, aTimer, aIsServerSide, aReceivedBlockUpdate, aReceivedInventoryUpdate);} catch(Throwable e) {e.printStackTrace(ERR); mTileEntity.setError("Cover Pre Tick - "  + (aIsServerSide?"Serverside: ":"Clientside: ") + e);}
	}
	
	public void tickPost(long aTimer, boolean aIsServerSide, boolean aReceivedBlockUpdate, boolean aReceivedInventoryUpdate) {
		try {for (byte tSide : ALL_SIDES_VALID) if (mBehaviours[tSide] != null) mBehaviours[tSide].onTickPost(tSide, this, aTimer, aIsServerSide, aReceivedBlockUpdate, aReceivedInventoryUpdate);} catch(Throwable e) {e.printStackTrace(ERR); mTileEntity.setError("Cover Post Tick - " + (aIsServerSide?"Serverside: ":"Clientside: ") + e);}
	}
	
	public AABB box(double aMinX, double aMinY, double aMinZ, double aMaxX, double aMaxY, double aMaxZ) {return AABB.getBoundingBox(mTileEntity.getX()+aMinX, mTileEntity.getY()+aMinY, mTileEntity.getZ()+aMinZ, mTileEntity.getX()+aMaxX, mTileEntity.getY()+aMaxY, mTileEntity.getZ()+aMaxZ);}
	public AABB box(double[] aBox) {return AABB.getBoundingBox(mTileEntity.getX()+aBox[0], mTileEntity.getY()+aBox[1], mTileEntity.getZ()+aBox[2], mTileEntity.getX()+aBox[3], mTileEntity.getY()+aBox[4], mTileEntity.getZ()+aBox[5]);}
	public AABB box(float[] aBox) {return AABB.getBoundingBox(mTileEntity.getX()+aBox[0], mTileEntity.getY()+aBox[1], mTileEntity.getZ()+aBox[2], mTileEntity.getX()+aBox[3], mTileEntity.getY()+aBox[4], mTileEntity.getZ()+aBox[5]);}
	public AABB box() {return AABB.getBoundingBox(mTileEntity.getX(), mTileEntity.getY(), mTileEntity.getZ(), mTileEntity.getX()+1, mTileEntity.getY()+1, mTileEntity.getZ()+1);}
	
	public boolean box(AABB aAABB, List<AABB> aList, double aMinX, double aMinY, double aMinZ, double aMaxX, double aMaxY, double aMaxZ) {
		AABB tBox = box(aMinX, aMinY, aMinZ, aMaxX, aMaxY, aMaxZ);
		return tBox.intersects(aAABB) && aList.add(tBox);
	}
	public boolean box(AABB aAABB, List<AABB> aList, double[] aBox) {
		AABB tBox = box(aBox[0], aBox[1], aBox[2], aBox[3], aBox[4], aBox[5]);
		return tBox.intersects(aAABB) && aList.add(tBox);
	}
	public boolean box(AABB aAABB, List<AABB> aList, float[] aBox) {
		AABB tBox = box(aBox[0], aBox[1], aBox[2], aBox[3], aBox[4], aBox[5]);
		return tBox.intersects(aAABB) && aList.add(tBox);
	}
	public boolean box(AABB aAABB, List<AABB> aList) {
		AABB tBox = box(0, 0, 0, 1, 1, 1);
		return tBox.intersects(aAABB) && aList.add(tBox);
	}
	public boolean box(AABB aBox, AABB aAABB, List<AABB> aList) {
		return aBox != null && aBox.intersects(aAABB) && aList.add(aBox);
	}
}

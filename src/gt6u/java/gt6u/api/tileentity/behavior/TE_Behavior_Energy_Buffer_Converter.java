/**
 * Copyright (c) 2020 GT6U-Team
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
 * Modified in 2026 for the GregTech 6 NeoForge port (https://github.com/wolfram0108/gregtech6_w):
 * ported from GregTech6-Unofficial for Minecraft 1.7.10 / Forge to an addon for Minecraft 26.1.2 / NeoForge.
 */

package gt6u.api.tileentity.behavior;

import gregapi.tileentity.behavior.TE_Behavior;
import gregapi.tileentity.behavior.TE_Behavior_Energy_Capacitor;
import gregapi.tileentity.behavior.TE_Behavior_Energy_Stats;

import gregapi.data.TD;
import gregapi.tileentity.energy.ITileEntityEnergy;
import gregapi.util.UT;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;

import static gregapi.data.CS.*;

/**
 * @author YueSha
 */
public class TE_Behavior_Energy_Buffer_Converter extends TE_Behavior {
	public TE_Behavior_Energy_Stats mEnergyIN, mEnergyOUT;
	public TE_Behavior_Energy_Capacitor mStorage;
	public boolean mWasteEnergy = F, mLimitConsumption = F, mOverloaded = F, mEmitsEnergy = F, mCanEmitEnergy = F, mSizeIrrelevant = F, mFast = F;
	public long mMultiplier = 1;
	public byte mFactor = 1;

	public TE_Behavior_Energy_Buffer_Converter(BlockEntity aTileEntity, CompoundTag aNBT, TE_Behavior_Energy_Capacitor aStorage, TE_Behavior_Energy_Stats aEnergyIN, TE_Behavior_Energy_Stats aEnergyOUT, long aMultiplier, boolean aWasteEnergy, boolean aNegativeOutput, boolean aLimitConsumption) {
		super(aTileEntity, aNBT);
		mStorage = aStorage;
		mEnergyIN = aEnergyIN;
		mEnergyOUT = aEnergyOUT;
		mMultiplier = aMultiplier;
		mWasteEnergy = aWasteEnergy;
		mSizeIrrelevant = TD.Energy.ALL_SIZE_IRRELEVANT.contains(mEnergyOUT.mType);
		mLimitConsumption = aLimitConsumption;
		if (aNegativeOutput) mFactor = -1;
	}

	@Override
	public void load(CompoundTag aNBT) {
		mEmitsEnergy = aNBT.contains(NBT_ACTIVE_ENERGY) ? aNBT.getBoolean(NBT_ACTIVE_ENERGY) : false;
		mCanEmitEnergy = aNBT.contains(NBT_CAN_ENERGY) ? aNBT.getBoolean(NBT_CAN_ENERGY) : false;
//		if (aNBT.contains(NBT_MULTIPLIER)) mMultiplier = aNBT.getLongOr(NBT_MULTIPLIER, 0L);
//		if (aNBT.contains(NBT_OUTPUT_MAX)) mEnergyOUT.mMax = aNBT.getLongOr(NBT_OUTPUT_MAX, 0L);
//		if (aNBT.contains(NBT_OUTPUT)) mEnergyOUT.mRec = aNBT.getLongOr(NBT_OUTPUT, 0L);
//		if (aNBT.contains(NBT_OUTPUT_MIN)) mEnergyOUT.mMin = aNBT.getLongOr(NBT_OUTPUT_MIN, 0L);
//		if (aNBT.contains(NBT_INPUT_MAX)) mEnergyIN.mMax = aNBT.getLongOr(NBT_INPUT_MAX, 0L);
//		if (aNBT.contains(NBT_INPUT)) mEnergyIN.mRec = aNBT.getLongOr(NBT_INPUT, 0L);
//		if (aNBT.contains(NBT_INPUT_MIN)) mEnergyIN.mMin = aNBT.getLongOr(NBT_INPUT_MIN, 0L);
	}

	@Override
	public void save(CompoundTag aNBT) {
		UT.NBT.setBoolean(aNBT, NBT_ACTIVE_ENERGY, mEmitsEnergy);
		UT.NBT.setBoolean(aNBT, NBT_CAN_ENERGY, mCanEmitEnergy);
		UT.NBT.setNumber(aNBT, NBT_MULTIPLIER, mMultiplier);
		UT.NBT.setNumber(aNBT, NBT_OUTPUT_MAX, mEnergyOUT.mMax);
		UT.NBT.setNumber(aNBT, NBT_OUTPUT, mEnergyOUT.mRec);
		UT.NBT.setNumber(aNBT, NBT_OUTPUT_MIN, mEnergyOUT.mMin);
		UT.NBT.setNumber(aNBT, NBT_INPUT_MAX, mEnergyIN.mMax);
		UT.NBT.setNumber(aNBT, NBT_INPUT, mEnergyIN.mRec);
		UT.NBT.setNumber(aNBT, NBT_INPUT_MIN, mEnergyIN.mMin);
	}

	public boolean onTickEnergy(long aTimer, BlockEntity aEmitter, byte aSide, byte aMode, boolean aNegative) {
		long tOutput = mEnergyOUT.mRec;
		long tEmittedPackets;
		mCanEmitEnergy = mStorage.mEnergy >= tOutput;
		mEmitsEnergy = F;
		if (mCanEmitEnergy) {

			if (SIDES_VALID[aSide]) {
				tEmittedPackets = ITileEntityEnergy.Util.emitEnergyToNetwork(mEnergyOUT.mType, aNegative ? -tOutput*mFactor : tOutput*mFactor, mMultiplier, (ITileEntityEnergy)aEmitter);
			} else {
				tEmittedPackets = ITileEntityEnergy.Util.emitEnergyToSide   (mEnergyOUT.mType, aSide, aNegative ? -tOutput*mFactor : tOutput*mFactor, mMultiplier, aEmitter);
			}

			if (tEmittedPackets > 0) {
				mStorage.mEnergy = Math.max(0, mStorage.mEnergy - UT.Code.units(tEmittedPackets * tOutput * mFactor, mEnergyOUT.mRec * mMultiplier, mEnergyIN.mRec, T));
				mEmitsEnergy = T;
			}

		}


		return mCanEmitEnergy;
	}
}

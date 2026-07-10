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

package gregapi.fluid;

import gregapi.data.FL;
import gregapi.recipes.Recipe.RecipeMap;
import gregapi.util.UT;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.IFluidTank;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.FluidStacksResourceHandler;

import java.util.HashMap;
import java.util.Map;

import static gregapi.data.CS.F;
import static gregapi.data.CS.T;

/**
 * F5 центральный переходник — танк GT6. Внутренняя логика (fill/drain/capacity/voidExcess/
 * preventDraining/адаптивная ёмкость по {@code RecipeMap.mMinInputTankSizes}) сохранена 1:1
 * (`decisions/F5-fluids.md` §4) — меняется ТОЛЬКО внешний фасад:
 * <ul>
 * <li>{@link IFluidTank} — реальный (хоть и {@code @Deprecated(forRemoval=true)}) интерфейс neo
 *     26.1.2 ({@code net.neoforged.neoforge.fluids.IFluidTank.java}), максимально совместимый по
 *     форме с legacy-потребителями; сигнатуры {@code fill(FluidStack,FluidAction)}/
 *     {@code drain(int,FluidAction)}/{@code drain(FluidStack,FluidAction)} — из декомпила.</li>
 * <li>{@link #asResourceHandler()} — современный фасад, {@code ResourceHandler<FluidResource>} через
 *     {@code Capabilities.Fluid.BLOCK/ITEM}, ровно как решение F5 §4 и образец AE2
 *     {@code SkyStoneTankBlockEntity.java:27,56} требуют. Реализация — не самодельная транзакционная
 *     логика (риск незаметно сломать rollback), а обёртка над готовым, транзакционно-безопасным
 *     {@link FluidStacksResourceHandler} (referenced, не изобретено) с двусторонней синхронизацией
 *     против {@link #mFluid}/{@link #mAmount}.</li>
 * </ul>
 *
 * <p>Трение long-vs-int (`decisions/F5-fluids.md` §4, §8): {@link #mAmount} остаётся {@code long}
 * (GT6 объёмы огромны), на границе с {@link FluidStack}/{@link FluidResource} (оба {@code int})
 * клампится через {@link UT.Code#bindInt}, ровно как раньше.
 */
public class FluidTankGT implements IFluidTank {
	public final FluidTankGT[] AS_ARRAY = new FluidTankGT[] {this};

	private FluidStack mFluid;
	private long mCapacity = 0, mAmount = 0;
	private boolean mPreventDraining = F, mVoidExcess = F, mChangedFluids = F;
	/** HashMap of adjustable Tank Sizes based on Fluids if needed. */
	private Map<String, Long> mAdjustableCapacity = null;
	private long mAdjustableMultiplier = 1;
	/** Gives you a Tank Index in case there is multiple Tanks on a TileEntity that cares. */
	public int mIndex = 0;

	/** Современный facade-объект (см. javadoc класса) — построен один раз, живёт с танком. */
	private FluidStacksResourceHandler mCapabilityView;
	private boolean mSyncingCapability = F;

	public FluidTankGT() {mCapacity = Long.MAX_VALUE;}
	public FluidTankGT(long aCapacity) {mCapacity = aCapacity;}
	public FluidTankGT(FluidStack aFluid) {mFluid = aFluid; if (aFluid != null) {mCapacity = aFluid.getAmount(); mAmount = aFluid.getAmount();}}
	public FluidTankGT(FluidStack aFluid, long aCapacity) {mFluid = aFluid; mCapacity = aCapacity; mAmount = (aFluid == null ? 0 : aFluid.getAmount());}
	public FluidTankGT(FluidStack aFluid, long aAmount, long aCapacity) {mFluid = aFluid; mCapacity = aCapacity; mAmount = (aFluid == null ? 0 : aAmount);}
	public FluidTankGT(Fluid aFluid, long aAmount) {this(FL.make(aFluid, aAmount)); mAmount = aAmount;}
	public FluidTankGT(Fluid aFluid, long aAmount, long aCapacity) {this(FL.make(aFluid, aAmount), aCapacity); mAmount = aAmount;}
	public FluidTankGT(CompoundTag aNBT, long aCapacity) {mCapacity = aCapacity; if (aNBT != null && !aNBT.isEmpty()) {mFluid = FL.load_(aNBT); mAmount = (isEmpty() ? 0 : aNBT.contains("LAmount") ? aNBT.getLongOr("LAmount", 0L) : mFluid.getAmount());}}
	public FluidTankGT(CompoundTag aNBT, String aKey, long aCapacity) {this(aNBT.contains(aKey) ? aNBT.getCompoundOrEmpty(aKey) : null, aCapacity);}

	public FluidTankGT readFromNBT(CompoundTag aNBT, String aKey) {
		if (aNBT.contains(aKey)) {
			CompoundTag tNBT = aNBT.getCompoundOrEmpty(aKey);
			if (!tNBT.isEmpty()) {
				mFluid = FL.load_(tNBT);
				mAmount = (isEmpty() ? 0 : tNBT.contains("LAmount") ? tNBT.getLongOr("LAmount", 0L) : mFluid.getAmount());
				syncCapabilityView();
			}
		}
		return this;
	}

	public CompoundTag writeToNBT(CompoundTag aNBT, String aKey) {
		if (mFluid != null && (mPreventDraining || mAmount > 0)) {
			CompoundTag tNBT = FL.save(FL.amount(mFluid, UT.Code.bindInt(mAmount)));
			if (tNBT == null) tNBT = UT.NBT.make();
			if (mAmount > Integer.MAX_VALUE) tNBT.putLong("LAmount", mAmount);
			aNBT.put(aKey, tNBT);
		} else {
			aNBT.remove(aKey);
		}
		return aNBT;
	}

	public CompoundTag writeToNBT(String aKey) {
		CompoundTag aNBT = UT.NBT.make();
		return writeToNBT(aNBT, aKey);
	}

	public static CompoundTag writeToNBT(String aKey, FluidStack aFluid) {
		CompoundTag rNBT = UT.NBT.make();
		if (aFluid != null && aFluid.getAmount() > 0) {
			CompoundTag tNBT = FL.save(aFluid);
			if (tNBT != null) rNBT.put(aKey, tNBT);
		}
		return rNBT;
	}

	public static CompoundTag writeToNBT(CompoundTag aNBT, String aKey, FluidStack aFluid) {
		if (aFluid != null && aFluid.getAmount() > 0) {
			CompoundTag tNBT = FL.save(aFluid);
			if (tNBT != null) aNBT.put(aKey, tNBT);
		} else {
			aNBT.remove(aKey);
		}
		return aNBT;
	}

	public FluidStack drain(int aDrained) {return drain(aDrained, T);}
	public FluidStack drain(int aDrained, boolean aDoDrain) {
		if (isEmpty() || aDrained <= 0) return null;
		if (mAmount < aDrained) aDrained = (int)mAmount;
		FluidStack rFluid = mFluid.copyWithAmount(aDrained);
		if (aDoDrain) {
			mAmount -= aDrained;
			if (mAmount <= 0) {
				if (mPreventDraining) {
					mAmount = 0;
				} else {
					setEmpty();
				}
			}
			syncCapabilityView();
		}
		return rFluid;
	}
	@Override public FluidStack drain(int aDrained, FluidAction aAction) {return drain(aDrained, aAction.execute());}
	@Override public FluidStack drain(FluidStack aResource, FluidAction aAction) {
		if (aResource == null || aResource.isEmpty() || !contains(aResource)) return FluidStack.EMPTY;
		FluidStack rFluid = drain(aResource.getAmount(), aAction.execute());
		return rFluid == null ? FluidStack.EMPTY : rFluid;
	}

	public boolean drainAll(long aDrained) {
		if (isEmpty() || mAmount < aDrained) return F;
		mAmount -= aDrained;
		if (mAmount <= 0) {
			if (mPreventDraining) {
				mAmount = 0;
			} else {
				setEmpty();
			}
		}
		syncCapabilityView();
		return T;
	}

	public long remove(long aDrained) {
		if (isEmpty() || mAmount <= 0 || aDrained <= 0) return 0;
		if (mAmount < aDrained) aDrained = mAmount;
		mAmount -= aDrained;
		if (mAmount <= 0) {
			if (mPreventDraining) {
				mAmount = 0;
			} else {
				setEmpty();
			}
		}
		syncCapabilityView();
		return aDrained;
	}

	public long add(long aFilled) {
		if (isEmpty() || aFilled <= 0) return 0;
		long tCapacity = capacity();
		if (mAmount + aFilled > tCapacity) {
			if (!mVoidExcess) aFilled = tCapacity - mAmount;
			mAmount = tCapacity;
			syncCapabilityView();
			return aFilled;
		}
		mAmount += aFilled;
		syncCapabilityView();
		return aFilled;
	}

	public long add(long aFilled, FluidStack aFluid) {
		if (aFluid == null || aFilled <= 0) return 0;
		if (isEmpty()) {
			mFluid = aFluid.copy();
			mChangedFluids = T;
			mAmount = Math.min(capacity(aFluid), aFilled);
			syncCapabilityView();
			return mVoidExcess ? aFilled : mAmount;
		}
		return contains(aFluid) ? add(aFilled) : 0;
	}

	public int fill(FluidStack aFluid) {return fill(aFluid, T);}
	public int fill(FluidStack aFluid, boolean aDoFill) {
		if (aFluid == null) return 0;
		if (aDoFill) {
			if (isEmpty()) {
				mFluid = aFluid.copy();
				mChangedFluids = T;
				mAmount = Math.min(capacity(aFluid), aFluid.getAmount());
				syncCapabilityView();
				return mVoidExcess ? aFluid.getAmount() : (int)mAmount;
			}
			if (!contains(aFluid)) return 0;
			long tCapacity = capacity(aFluid), tFilled = tCapacity - mAmount;
			if (aFluid.getAmount() < tFilled) {
				mAmount += aFluid.getAmount();
				tFilled = aFluid.getAmount();
			} else mAmount = tCapacity;
			syncCapabilityView();
			return mVoidExcess ? aFluid.getAmount() : (int)tFilled;
		}
		return UT.Code.bindInt(isEmpty() ? mVoidExcess ? aFluid.getAmount() : Math.min(capacity(aFluid), aFluid.getAmount()) : contains(aFluid) ? mVoidExcess ? aFluid.getAmount() : Math.min(capacity(aFluid) - mAmount, aFluid.getAmount()) : 0);
	}
	@Override public int fill(FluidStack aResource, FluidAction aAction) {return fill(aResource, aAction.execute());}

	public boolean canFillAll(FluidStack aFluid) {return aFluid == null || aFluid.getAmount() <= 0 || (isEmpty() ? mVoidExcess || aFluid.getAmount() <= capacity(aFluid) : contains(aFluid) && (mVoidExcess || mAmount + aFluid.getAmount() <= capacity(aFluid)));}
	public boolean canFillAll(long aAmount) {return aAmount <= 0 || mVoidExcess || mAmount + aAmount <= capacity();}

	public boolean fillAll(FluidStack aFluid) {
		if (aFluid == null || aFluid.getAmount() <= 0) return T;
		if (isEmpty()) {
			long tCapacity = capacity(aFluid);
			if (aFluid.getAmount() <= tCapacity || mVoidExcess) {
				mFluid = aFluid.copy();
				mChangedFluids = T;
				mAmount = aFluid.getAmount();
				if (mAmount > tCapacity) mAmount = tCapacity;
				syncCapabilityView();
				return T;
			}
			return F;
		}
		if (contains(aFluid)) {
			if (mAmount + aFluid.getAmount() <= capacity()) {
				mAmount += aFluid.getAmount();
				syncCapabilityView();
				return T;
			}
			if (mVoidExcess) {
				mAmount = capacity();
				syncCapabilityView();
				return T;
			}
		}
		return F;
	}

	public boolean fillAll(FluidStack aFluid, long aMultiplier) {
		if (aMultiplier <= 0) return T;
		if (aMultiplier == 1) return fillAll(aFluid);
		if (aFluid == null || aFluid.getAmount() <= 0) return T;
		if (isEmpty()) {
			long tCapacity = capacity(aFluid);
			if (aFluid.getAmount() * aMultiplier <= tCapacity || mVoidExcess) {
				mFluid = aFluid.copy();
				mChangedFluids = T;
				mAmount = aFluid.getAmount() * aMultiplier;
				if (mAmount > tCapacity) mAmount = tCapacity;
				syncCapabilityView();
				return T;
			}
			return F;
		}
		if (contains(aFluid)) {
			if (mAmount + aFluid.getAmount() * aMultiplier <= capacity()) {
				mAmount += aFluid.getAmount() * aMultiplier;
				syncCapabilityView();
				return T;
			}
			if (mVoidExcess) {
				mAmount = capacity();
				syncCapabilityView();
				return T;
			}
		}
		return F;
	}

	/** Resets Tank Contents entirely */
	public FluidTankGT setEmpty() {
		if (mFluid != null) mChangedFluids = T;
		mFluid  = null;
		mAmount = 0;
		syncCapabilityView();
		return this;
	}
	/** Sets Fluid Content, taking Amount from the Fluid Parameter  */
	public FluidTankGT setFluid(FluidStack aFluid) {
		if (aFluid == null) return setEmpty();
		if (!FL.equal(mFluid, aFluid)) mChangedFluids = T;
		mFluid  = aFluid;
		mAmount = mFluid.getAmount();
		syncCapabilityView();
		return this;
	}
	/** Sets Fluid Content and Amount */
	public FluidTankGT setFluid(FluidStack aFluid, long aAmount) {
		if (aFluid == null) return setEmpty();
		if (!FL.equal(mFluid, aFluid)) mChangedFluids = T;
		mFluid  = aFluid;
		mAmount = aAmount;
		syncCapabilityView();
		return this;
	}
	/** Sets Fluid Content, taking Amount from the Tank Parameter  */
	public FluidTankGT setFluid(FluidTankGT aTank) {
		if (aTank == null || aTank.mFluid == null) return setEmpty();
		if (!FL.equal(mFluid, aTank.mFluid)) mChangedFluids = T;
		mFluid  = FL.amount(aTank.mFluid, aTank.mAmount);
		mAmount = aTank.mAmount;
		syncCapabilityView();
		return this;
	}
	/** Sets the Tank Index for easier Reverse Mapping. */
	public FluidTankGT setIndex(int aIndex) {mIndex = aIndex; return this;}
	/** Sets the Capacity, and yes it accepts 63 Bit Numbers */
	public FluidTankGT setCapacity(long aCapacity) {if (aCapacity >= 0) mCapacity = aCapacity; return this;}
	/** Always keeps at least 0 Liters of Fluid instead of setting it to null */
	public FluidTankGT setPreventDraining() {return setPreventDraining(T);}
	/** Always keeps at least 0 Liters of Fluid instead of setting it to null */
	public FluidTankGT setPreventDraining(boolean aPrevent) {mPreventDraining = aPrevent; return this;}
	/** Voids any Overlow */
	public FluidTankGT setVoidExcess() {return setVoidExcess(T);}
	/** Voids any Overlow */
	public FluidTankGT setVoidExcess(boolean aVoidExcess) {mVoidExcess = aVoidExcess; return this;}
	/** Sets Tank capacity Map, should it be needed. */
	public FluidTankGT setCapacity(RecipeMap aMap, long aCapacityMultiplier) {mAdjustableCapacity = aMap.mMinInputTankSizes; mAdjustableMultiplier = aCapacityMultiplier; return this;}
	/** Sets Tank capacity Map, should it be needed. */
	public FluidTankGT setCapacity(Map<String, Long> aMap, long aCapacityMultiplier) {mAdjustableCapacity = aMap; mAdjustableMultiplier = aCapacityMultiplier; return this;}
	/** Adds a custom capacity to the Tank capacity Map. */
	public FluidTankGT setCapacity(FluidStack aFluid) {return setCapacity(aFluid.getFluid(), aFluid.getAmount());}
	/** Adds a custom capacity to the Tank capacity Map. */
	public FluidTankGT setCapacity(Fluid aFluid, long aCapacity) {return setCapacity(FluidGT.nameOf(aFluid), aCapacity);}
	/** Adds a custom capacity to the Tank capacity Map. */
	public FluidTankGT setCapacity(String aFluid, long aCapacity) {if (mAdjustableCapacity == null) mAdjustableCapacity = new HashMap<>(); mAdjustableCapacity.put(aFluid, aCapacity); return this;}

	public boolean isEmpty  () {return mFluid == null;}
	public boolean isFull   () {return mAmount     >= capacity();}
	public boolean isHalf   () {return mAmount * 2 >= capacity();}
	public boolean overHalf () {return mAmount * 2 >  capacity();}
	public boolean underHalf() {return mAmount * 2 <  capacity();}

	public boolean contains(Fluid aFluid) {return mFluid != null && mFluid.getFluid() == aFluid;}
	public boolean contains(FluidStack aFluid) {return FL.equal(mFluid, aFluid);}

	public boolean has(long aAmount) {return mAmount >= aAmount;}
	public boolean has() {return mAmount > 0;}

	public boolean check() {if (mChangedFluids) {mChangedFluids = F; return T;} return F;}
	public boolean update() {return mChangedFluids = T;}
	public boolean changed() {return mChangedFluids;}

	public long amount() {return isEmpty() ? 0 : mAmount;}
	public long amount(long aMax) {return isEmpty() || aMax <= 0 ? 0 : Math.min(mAmount, aMax);}

	public long capacity (                 ) {return mAdjustableCapacity == null ? mCapacity : capacity_(mFluid);}
	public long capacity (FluidStack aFluid) {return mAdjustableCapacity == null ? mCapacity : capacity_(aFluid);}
	public long capacity (Fluid      aFluid) {return mAdjustableCapacity == null ? mCapacity : capacity_(aFluid);}
	public long capacity (String     aFluid) {return mAdjustableCapacity == null ? mCapacity : capacity_(aFluid);}
	public long capacity_(FluidStack aFluid) {return aFluid == null ? mCapacity : capacity_(aFluid.getFluid());}
	public long capacity_(Fluid      aFluid) {return aFluid == null ? mCapacity : capacity_(FluidGT.nameOf(aFluid));}
	public long capacity_(String     aFluid) {
		if (aFluid == null) return mCapacity;
		Long tSize = mAdjustableCapacity.get(aFluid);
		return tSize == null ? Math.max(mAmount, mCapacity) : Math.max(tSize * mAdjustableMultiplier, Math.max(mAmount, mCapacity));
	}

	public String name() {return mFluid == null ? null : FluidGT.nameOf(mFluid.getFluid());}
	public String name(boolean aLocalised) {return FL.name(mFluid, aLocalised);}

	public String content() {return content("Empty");}
	public String content(String aEmptyMessage) {return  mFluid == null ? aEmptyMessage                     : UT.Code.makeString(amount()) + " L of " + name(T) + " (" + (FL.gas(mFluid) ? "Gaseous" : "Liquid") + ")";}
	public String contentcap() {return mFluid == null ? "Capacity: " + UT.Code.makeString(mCapacity) + " L" : UT.Code.makeString(amount()) + " L of " + name(T) + " (" + (FL.gas(mFluid) ? "Gaseous" : "Liquid") + "); Max: "+UT.Code.makeString(capacity())+" L)";}

	public Fluid fluid() {return mFluid == null ? null : mFluid.getFluid();}

	public FluidStack make(int aAmount) {return FL.make(fluid(), aAmount);}

	public FluidStack get() {return mFluid;}
	public FluidStack get(long aMax) {return isEmpty() || aMax <= 0 ? null : mFluid.copyWithAmount(UT.Code.bindInt(mAmount < aMax ? mAmount : aMax));}

	@Override public FluidStack getFluid() {if (mFluid != null) mFluid.setAmount(UT.Code.bindInt(mAmount)); return mFluid;}
	@Override public int getFluidAmount() {return UT.Code.bindInt(mAmount);}
	@Override public int getCapacity() {return UT.Code.bindInt(capacity());}
	@Override public boolean isFluidValid(FluidStack aStack) {return T;} // GT6 1.7.10 танк исторически не фильтровал жидкости на этом уровне (фильтрация — на уровне RecipeMap/ковера)
	/** @deprecated см. {@link FluidTankInfo} — форж-1.7.10 совместимость, не часть текущего IFluidTank. */
	@Deprecated public FluidTankInfo getInfo() {return new FluidTankInfo(isEmpty() ? null : mFluid.copy(), UT.Code.bindInt(capacity()));}

	// ======================== Современный фасад: ResourceHandler<FluidResource> ========================

	/**
	 * {@code ResourceHandler<FluidResource>} — то, что консьюмер регистрирует через
	 * {@code Capabilities.Fluid.BLOCK/ITEM} в своём {@code RegisterCapabilitiesEvent}
	 * (AE2 {@code InitCapabilityProviders.java:99,150,161}). Транзакционная семантика — из
	 * {@link FluidStacksResourceHandler} (NeoForge, не самодельная), содержимое зеркалируется на
	 * {@link #mFluid}/{@link #mAmount} через {@link #syncCapabilityView()} (push) и
	 * {@link FluidStacksResourceHandler#onContentsChanged} (pull), с защитой от реентрантности.
	 */
	public ResourceHandler<FluidResource> asResourceHandler() {
		if (mCapabilityView == null) {
			mCapabilityView = new FluidStacksResourceHandler(NonNullList.withSize(1, FluidStack.EMPTY), UT.Code.bindInt(capacity())) {
				@Override protected int getCapacity(int aIndex, FluidResource aResource) {return UT.Code.bindInt(capacity());}
				@Override protected void onContentsChanged(int aIndex, FluidStack aPreviousContents) {pullFromCapabilityView();}
			};
			syncCapabilityView();
		}
		return mCapabilityView;
	}

	/** GT6-состояние ({@link #mFluid}/{@link #mAmount}) -> capability-слот. */
	private void syncCapabilityView() {
		if (mCapabilityView == null || mSyncingCapability) return;
		mSyncingCapability = T;
		try {
			FluidStack tView = isEmpty() ? FluidStack.EMPTY : mFluid.copyWithAmount(UT.Code.bindInt(mAmount));
			mCapabilityView.set(0, FluidResource.of(tView), tView.getAmount());
		} finally {
			mSyncingCapability = F;
		}
	}

	/** capability-слот -> GT6-состояние ({@link #mFluid}/{@link #mAmount}); вызывается NeoForge при
	 *  изменении слота ИЗВНЕ (сторонний мод/труба через {@link ResourceHandler}). */
	private void pullFromCapabilityView() {
		if (mSyncingCapability) return;
		mSyncingCapability = T;
		try {
			FluidResource tResource = mCapabilityView.getResource(0);
			int tAmount = mCapabilityView.getAmountAsInt(0);
			if (tResource == null || tResource.isEmpty() || tAmount <= 0) {
				if (mFluid != null) mChangedFluids = T;
				mFluid = null;
				mAmount = 0;
			} else {
				FluidStack tStack = tResource.toStack(tAmount);
				if (!FL.equal(mFluid, tStack)) mChangedFluids = T;
				mFluid = tStack;
				mAmount = tAmount;
			}
		} finally {
			mSyncingCapability = F;
		}
	}
}

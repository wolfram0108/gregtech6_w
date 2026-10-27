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

package gregapi.recipes.handlers;

import gregapi.code.ICondition;
import gregapi.data.FL;
import gregapi.data.OP;
import gregapi.data.TD;
import gregapi.oredict.OreDictItemData;
import gregapi.oredict.OreDictMaterial;
import gregapi.oredict.OreDictPrefix;
import gregapi.recipes.IRecipeMapHandler.RecipeMapHandler;
import gregapi.recipes.Recipe.RecipeMap;
import gregapi.util.OM;
import gregapi.util.ST;
import gregapi.util.UT;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.fluids.FluidStack;

import static gregapi.data.CS.*;

/**
 * @author Gregorius Techneticies
 */
@SuppressWarnings("rawtypes")
public class RecipeMapHandlerPrefix extends RecipeMapHandler {
	protected ICondition mCondition;
	protected final OreDictPrefix[] mInputPrefixes, mOutputPrefixes;
	protected final byte[] mInputAmounts, mOutputAmounts;
	protected final ItemStack mAdditionalInput, mAdditionalOutput;
	protected final FluidStack mFluidInputPerUnit, mFluidOutputPerUnit;
	protected final long mUnitsInputted, mUnitsOutputted, mEUt, mDuration, mMultiplier;
	protected final boolean mAllowToGenerateAllRecipesAtOnce;
	
	protected long[] mChances = ZL_LONG;
	
	protected boolean mAlreadyAddedAllRecipes = F, mOutputPulverizedRemains = F, mFlatFluidCosts = F;
	
	public RecipeMapHandlerPrefix(OreDictPrefix aInputPrefix, long aInputAmount, FluidStack aFluidInputPerUnit, long aEUt, long aDuration, long aMultiplier, FluidStack aFluidOutputPerUnit, OreDictPrefix aOutputPrefix, long aOutputAmount, ItemStack aAdditionalInput, ItemStack aAdditionalOutput, boolean aAllowToGenerateAllRecipesAtOnce, boolean aOutputPulverizedRemains, boolean aFlatFluidCosts, ICondition aCondition) {
		mAllowToGenerateAllRecipesAtOnce = aAllowToGenerateAllRecipesAtOnce;
		mFlatFluidCosts     = aFlatFluidCosts;
		mFluidInputPerUnit  = aFluidInputPerUnit;
		mFluidOutputPerUnit = aFluidOutputPerUnit;
		mCondition          = (aCondition == null ? ICondition.TRUE : aCondition);
		mInputPrefixes      = new OreDictPrefix[] {aInputPrefix};
		mInputAmounts       = new byte[] {UT.Code.bindStack(aInputAmount)};
		mOutputPrefixes     = aOutputPrefix == null ? ZL_OREDICTPREFIX : new OreDictPrefix[] {aOutputPrefix};
		mOutputAmounts      = new byte[] {UT.Code.bindStack(aOutputAmount)};
		mAdditionalOutput   = aAdditionalOutput;
		mAdditionalInput    = aAdditionalInput;
		mMultiplier         = aMultiplier;
		mDuration           = aDuration;
		mEUt                = aEUt;
		long
		tUnitsProcessed     = 0;
		for (int i = 0; i < mInputPrefixes.length; i++) tUnitsProcessed += mInputPrefixes[i].mAmount * mInputAmounts[i];
		mUnitsInputted      = tUnitsProcessed;
		tUnitsProcessed     = 0;
		for (int i = 0; i < mOutputPrefixes.length; i++) tUnitsProcessed += mOutputPrefixes[i].mAmount * mOutputAmounts[i];
		mUnitsOutputted     = tUnitsProcessed;
		mOutputPulverizedRemains = (aOutputPulverizedRemains && mUnitsInputted-mUnitsOutputted >= OP.dustDiv72.mAmount);
	}
	
	public RecipeMapHandlerPrefix(OreDictPrefix aInputPrefix1, long aInputAmount1, OreDictPrefix aInputPrefix2, long aInputAmount2, FluidStack aFluidInputPerUnit, long aEUt, long aDuration, long aMultiplier, FluidStack aFluidOutputPerUnit, OreDictPrefix aOutputPrefix1, long aOutputAmount1, OreDictPrefix aOutputPrefix2, long aOutputAmount2, ItemStack aAdditionalInput, ItemStack aAdditionalOutput, boolean aAllowToGenerateAllRecipesAtOnce, boolean aOutputPulverizedRemains, boolean aFlatFluidCosts, ICondition aCondition) {
		mAllowToGenerateAllRecipesAtOnce = aAllowToGenerateAllRecipesAtOnce;
		mFlatFluidCosts     = aFlatFluidCosts;
		mFluidInputPerUnit  = aFluidInputPerUnit;
		mFluidOutputPerUnit = aFluidOutputPerUnit;
		mCondition          = (aCondition == null ? ICondition.TRUE : aCondition);
		mInputPrefixes      = aInputPrefix2 == null ? new OreDictPrefix[] {aInputPrefix1} : new OreDictPrefix[] {aInputPrefix1, aInputPrefix2};
		mInputAmounts       = aInputPrefix2 == null ? new byte[] {UT.Code.bindStack(aInputAmount1)} : new byte[] {UT.Code.bindStack(aInputAmount1), UT.Code.bindStack(aInputAmount2)};
		mOutputPrefixes     = aOutputPrefix2 == null ? aOutputPrefix1 == null ? ZL_OREDICTPREFIX : new OreDictPrefix[] {aOutputPrefix1} : new OreDictPrefix[] {aOutputPrefix1, aOutputPrefix2};
		mOutputAmounts      = aOutputPrefix2 == null ? new byte[] {UT.Code.bindStack(aOutputAmount1)} : new byte[] {UT.Code.bindStack(aOutputAmount1), UT.Code.bindStack(aOutputAmount2)};
		mAdditionalOutput   = aAdditionalOutput;
		mAdditionalInput    = aAdditionalInput;
		mMultiplier         = aMultiplier;
		mDuration           = aDuration;
		mEUt                = aEUt;
		long
		tUnitsProcessed     = 0;
		for (int i = 0; i < mInputPrefixes.length; i++) tUnitsProcessed += mInputPrefixes[i].mAmount * mInputAmounts[i];
		mUnitsInputted      = tUnitsProcessed;
		tUnitsProcessed     = 0;
		for (int i = 0; i < mOutputPrefixes.length; i++) tUnitsProcessed += mOutputPrefixes[i].mAmount * mOutputAmounts[i];
		mUnitsOutputted     = tUnitsProcessed;
		mOutputPulverizedRemains = (aOutputPulverizedRemains && mUnitsInputted-mUnitsOutputted >= OP.dustDiv72.mAmount);
	}
	
	public RecipeMapHandlerPrefix(OreDictPrefix[] aInputPrefixes, long[] aInputAmount, FluidStack aFluidInputPerUnit, long aEUt, long aDuration, long aMultiplier, FluidStack aFluidOutputPerUnit, OreDictPrefix[] aOutputPrefixes, long[] aOutputAmount, ItemStack aAdditionalInput, ItemStack aAdditionalOutput, boolean aAllowToGenerateAllRecipesAtOnce, boolean aOutputPulverizedRemains, boolean aFlatFluidCosts, ICondition aCondition) {
		mAllowToGenerateAllRecipesAtOnce = aAllowToGenerateAllRecipesAtOnce;
		mFlatFluidCosts     = aFlatFluidCosts;
		mFluidInputPerUnit  = aFluidInputPerUnit;
		mFluidOutputPerUnit = aFluidOutputPerUnit;
		mCondition          = (aCondition == null ? ICondition.TRUE : aCondition);
		mInputPrefixes      = aInputPrefixes;
		mInputAmounts       = new byte[mInputPrefixes.length];
		for (int i = 0; i < mInputAmounts.length; i++) mInputAmounts[i] = UT.Code.bindStack(aInputAmount[i]);
		mOutputPrefixes     = aOutputPrefixes;
		mOutputAmounts      = new byte[mOutputPrefixes.length];
		for (int i = 0; i < mOutputAmounts.length; i++) mOutputAmounts[i] = UT.Code.bindStack(aOutputAmount[i]);
		mAdditionalOutput   = aAdditionalOutput;
		mAdditionalInput    = aAdditionalInput;
		mMultiplier         = aMultiplier;
		mDuration           = aDuration;
		mEUt                = aEUt;
		long
		tUnitsProcessed     = 0;
		for (int i = 0; i < mInputPrefixes.length; i++) tUnitsProcessed += mInputPrefixes[i].mAmount * mInputAmounts[i];
		mUnitsInputted      = tUnitsProcessed;
		tUnitsProcessed     = 0;
		for (int i = 0; i < mOutputPrefixes.length; i++) tUnitsProcessed += mOutputPrefixes[i].mAmount * mOutputAmounts[i];
		mUnitsOutputted     = tUnitsProcessed;
		mOutputPulverizedRemains = (aOutputPulverizedRemains && mUnitsInputted-mUnitsOutputted >= OP.dustDiv72.mAmount);
	}
	
	private static boolean matchesPrefix(ItemStack aStack, OreDictItemData aData, OreDictPrefix[] aPrefixes) {
		if (UT.Code.contains(aData.mPrefix, aPrefixes)) return T;
		// Canonical targets can retain the primary data of another equal-mass prefix.
		for (OreDictPrefix tPrefix : aPrefixes) if (aData.mMaterial.mAmount == tPrefix.mAmount && ST.equal(aStack, tPrefix.mat(aData.mMaterial.mMaterial, 1), T)) return T;
		return F;
	}

	@Override
	public boolean addRecipesUsing(RecipeMap aMap, boolean aNEI, ItemStack aStack, OreDictItemData aData) {
		if (isDone()) return F;
		if (ST.equal(aStack, mAdditionalInput)) return aNEI && mAllowToGenerateAllRecipesAtOnce && addAllRecipesInternal(aMap);
		return aData != null && aData.validData() && matchesPrefix(aStack, aData, mInputPrefixes) && addRecipeForMaterial(aMap, aData.mMaterial.mMaterial);
	}
	
	@Override
	public boolean addRecipesProducing(RecipeMap aMap, boolean aNEI, ItemStack aStack, OreDictItemData aData) {
		if (isDone()) return F;
		if (ST.equal(aStack, mAdditionalOutput)) return aNEI && mAllowToGenerateAllRecipesAtOnce && addAllRecipesInternal(aMap);
		return aData != null && aData.validData() && (matchesPrefix(aStack, aData, mOutputPrefixes) || (mOutputPulverizedRemains && aData.mPrefix == OP.dust)) && addRecipeForMaterial(aMap, aData.mMaterial.mMaterial);
	}
	
	@Override
	public boolean addRecipesUsing(RecipeMap aMap, boolean aNEI, Fluid aFluid) {
		if (isDone()) return F;
		return aNEI && mAllowToGenerateAllRecipesAtOnce && mFluidInputPerUnit != null && mFluidInputPerUnit.getFluid() == aFluid && addAllRecipesInternal(aMap);
	}
	
	@Override
	public boolean addRecipesProducing(RecipeMap aMap, boolean aNEI, Fluid aFluid) {
		if (isDone()) return F;
		return aNEI && mAllowToGenerateAllRecipesAtOnce && mFluidOutputPerUnit != null && mFluidOutputPerUnit.getFluid() == aFluid && addAllRecipesInternal(aMap);
	}
	
	@Override
	public boolean containsInput(RecipeMap aMap, Fluid aFluid) {
		return mFluidInputPerUnit != null && mFluidInputPerUnit.getFluid() == aFluid;
	}
	
	@Override
	public boolean addAllRecipes(RecipeMap aMap) {
		return mAllowToGenerateAllRecipesAtOnce && addAllRecipesInternal(aMap);
	}
	
	public boolean addAllRecipesInternal(RecipeMap aMap) {
		if (isDone()) return F;
		aMap.mConfigFile.mSaveOnEdit = F;
		for (OreDictMaterial tMaterial : mInputPrefixes[0].mRegisteredMaterials) addRecipeForMaterial(aMap, tMaterial);
		mAlreadyAddedAllRecipes = T;
		aMap.mConfigFile.mSaveOnEdit = T;
		aMap.mConfigFile.mConfig.save();
		return T;
	}
	
	public RecipeMapHandlerPrefix chances(long... aChances) {
		mChances = aChances;
		return this;
	}
	
	/** Makes the same Recipes: same inputs and outputs per unit, energy and time. The condition is left out, it is built anew each time. */
	public boolean sameAs(RecipeMapHandlerPrefix aOther) {
		return aOther != null && java.util.Arrays.equals(mInputPrefixes, aOther.mInputPrefixes) && java.util.Arrays.equals(mOutputPrefixes, aOther.mOutputPrefixes)
			&& java.util.Arrays.equals(mInputAmounts, aOther.mInputAmounts) && java.util.Arrays.equals(mOutputAmounts, aOther.mOutputAmounts)
			&& (mAdditionalInput   == null ? aOther.mAdditionalInput   == null : ST.equal(mAdditionalInput  , aOther.mAdditionalInput  ))
			&& (mAdditionalOutput  == null ? aOther.mAdditionalOutput  == null : ST.equal(mAdditionalOutput , aOther.mAdditionalOutput ))
			&& (mFluidInputPerUnit  == null ? aOther.mFluidInputPerUnit  == null : FL.equal(mFluidInputPerUnit , aOther.mFluidInputPerUnit ))
			&& (mFluidOutputPerUnit == null ? aOther.mFluidOutputPerUnit == null : FL.equal(mFluidOutputPerUnit, aOther.mFluidOutputPerUnit))
			&& mUnitsInputted == aOther.mUnitsInputted && mUnitsOutputted == aOther.mUnitsOutputted && mEUt == aOther.mEUt && mDuration == aOther.mDuration && mMultiplier == aOther.mMultiplier;
	}
	
	@Override
	public boolean isDone() {
		return mAlreadyAddedAllRecipes;
	}
	
	@Override
	public boolean onAddedToMap(RecipeMap aMap) {
		if (mFluidInputPerUnit != null) {
			aMap.mMaxFluidInputSize = Math.max(mFlatFluidCosts ? mFluidInputPerUnit.getAmount() : mFluidInputPerUnit.getAmount() * 4, aMap.mMaxFluidInputSize);
			Long tSize = aMap.mMinInputTankSizes.get(FL.regName(mFluidInputPerUnit.getFluid()));
			if (tSize == null || tSize < mFluidInputPerUnit.getAmount()) aMap.mMinInputTankSizes.put(FL.regName(mFluidInputPerUnit.getFluid()), (long)mFluidInputPerUnit.getAmount());
		}
		if (mFluidOutputPerUnit != null) {
			aMap.mMaxFluidOutputSize = Math.max(mFluidOutputPerUnit.getAmount() * 16, aMap.mMaxFluidOutputSize);
			Long tSize = aMap.mMaxOutputTankSizes.get(FL.regName(mFluidOutputPerUnit.getFluid()));
			if (tSize == null || tSize < mFluidOutputPerUnit.getAmount() * 16L) aMap.mMaxOutputTankSizes.put(FL.regName(mFluidOutputPerUnit.getFluid()), mFluidOutputPerUnit.getAmount() * 16L);
		}
		return T;
	}
	
	@SuppressWarnings("unchecked")
	public boolean addRecipeForMaterial(RecipeMap aMap, OreDictMaterial aMaterial) {
		if (!mCondition.isTrue(aMaterial) || aMaterial.contains(TD.Properties.INVALID_MATERIAL)) return F;
		
		ItemStack[] tInputs = new ItemStack[mInputPrefixes.length + (ST.valid(mAdditionalInput) ? 1 : 0)];
		if (ST.valid(mAdditionalInput)) tInputs[tInputs.length-1] = mAdditionalInput;
		for (int i = 0; i < mInputPrefixes.length; i++) if (ST.invalid(tInputs[i] = mInputPrefixes[i].mat(aMaterial, mInputAmounts[i]))) return F;
		
		OreDictMaterial tMaterial = getOutputMaterial(aMaterial);
		
		ItemStack[] tOutputs = new ItemStack[mOutputPrefixes.length + (mOutputPulverizedRemains ? 1 : 0) + (ST.valid(mAdditionalOutput) ? 1 : 0)];
		for (int i = 0; i < mOutputPrefixes.length; i++) if (ST.invalid(tOutputs[i] = mOutputPrefixes[i].mat(tMaterial, mOutputAmounts[i]))) return F;
		if (mOutputPulverizedRemains) tOutputs[mOutputPrefixes.length] = OM.pulverize(aMaterial, mUnitsInputted-mUnitsOutputted);
		if (ST.valid(mAdditionalOutput)) tOutputs[tOutputs.length-1] = mAdditionalOutput;
		
		return aMap.addRecipeX(F,T,F,F, !aMaterial.contains(TD.Properties.UNUSED_MATERIAL), mEUt, mDuration<=0?Math.max(1, getCosts(aMaterial)):mDuration, mChances, tInputs, mFlatFluidCosts ? mFluidInputPerUnit : FL.mul(mFluidInputPerUnit, aMaterial.mToolQuality+1), mFlatFluidCosts ? mFluidOutputPerUnit : FL.mul(mFluidOutputPerUnit, aMaterial.mToolQuality+1), tOutputs) != null;
	}
	
	public OreDictMaterial getOutputMaterial(OreDictMaterial aMaterial) {
		return aMaterial;
	}
	
	public long getCosts(OreDictMaterial aMaterial) {
		return UT.Code.units(Math.max(mUnitsInputted, mUnitsOutputted), U, mMultiplier+mMultiplier*aMaterial.mToolQuality, T);
	}
}

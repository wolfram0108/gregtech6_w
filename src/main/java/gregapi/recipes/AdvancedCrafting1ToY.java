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

package gregapi.recipes;

import static gregapi.data.CS.*;

import java.util.List;

import gregapi.code.ICondition;
import gregapi.data.MT;
import gregapi.oredict.OreDictItemData;
import gregapi.oredict.OreDictMaterial;
import gregapi.oredict.OreDictPrefix;
import gregapi.util.CR;
import gregapi.util.OM;
import gregapi.util.ST;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import gregapi.recipes.ShapedOreRecipe;
import gregapi.recipes.ShapelessOreRecipe;

/**
 * @author Gregorius Techneticies
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public class AdvancedCrafting1ToY implements ICraftingRecipeGT {
	public final ICondition mCondition;
	public final OreDictPrefix mInput, mOutput;
	public final boolean mAutoCraftable;
	public final int mOutputCount, mEmpty;
	
	public AdvancedCrafting1ToY(OreDictPrefix aInput, OreDictPrefix aOutput, int aOutputCount, boolean aAutoCraftable) {this(aInput, aOutput, aOutputCount, aAutoCraftable, MT.NULL.NOT);}
	public AdvancedCrafting1ToY(OreDictPrefix aInput, OreDictPrefix aOutput, int aOutputCount, boolean aAutoCraftable, ICondition aCondition) {
		mAutoCraftable = aAutoCraftable;
		mCondition = aCondition;
		mInput = aInput;
		mEmpty = mInput.mShapelessManagersSingle.size();
		mOutput = aOutput;
		mOutputCount = aOutputCount;
		mInput.mShapelessManagersSingle.add(this);
		
		List<ICraftingRecipeGT> tRecipeList = CR.list();
		try {for (int i = 0; i < tRecipeList.size(); i++) {
			ICraftingRecipeGT tRecipe = tRecipeList.get(i);
			if (tRecipe == null) {tRecipeList.remove(i--); continue;}
			int tCount = 0;
			
			if (tRecipeList.get(i) instanceof ICraftingRecipeGT) {
				// NOTHING
			} else if (tRecipe instanceof ShapedOreRecipe) {
				Object[] tInputs = ((ShapedOreRecipe)tRecipe).getInput();
				
				if (tInputs != null) for (Object tObject : tInputs) if (tObject != null) {
					if (++tCount > 1) {
						tCount = 0;
						break;
					}
					if (tObject instanceof ItemStack) {
						OreDictItemData tData = OM.data((ItemStack)tObject);
						if (tData == null || tData.mPrefix != mInput || tData.mMaterial == null || !mCondition.isTrue(tData.mMaterial.mMaterial)) {
							tCount = 0;
							break;
						}
					} else if (tObject instanceof List) {
						if (((List)tObject).isEmpty()) {
							tCount = 0;
							break;
						}
						if (((List)tObject).get(0) instanceof ItemStack) {
							OreDictItemData tData = OM.data((ItemStack)((List)tObject).get(0));
							if (tData == null || tData.mPrefix != mInput || tData.mMaterial == null || !mCondition.isTrue(tData.mMaterial.mMaterial)) {
								tCount = 0;
								break;
							}
						}
					} else {
						tCount = 0;
						break;
					}
				}
			} else if (tRecipe instanceof ShapelessOreRecipe) {
				List tInputs = ((ShapelessOreRecipe)tRecipe).getInput();
				
				if (tInputs != null && tInputs.size() == 1) for (Object tObject : tInputs) if (tObject != null) {
					tCount++;
					if (tObject instanceof ItemStack) {
						OreDictItemData tData = OM.data((ItemStack)tObject);
						if (tData == null || tData.mPrefix != mInput || tData.mMaterial == null || !mCondition.isTrue(tData.mMaterial.mMaterial)) {
							tCount = 0;
							break;
						}
					} else if (tObject instanceof List) {
						if (((List)tObject).isEmpty()) {
							tCount = 0;
							break;
						}
						if (((List)tObject).get(0) instanceof ItemStack) {
							OreDictItemData tData = OM.data((ItemStack)((List)tObject).get(0));
							if (tData == null || tData.mPrefix != mInput || tData.mMaterial == null || !mCondition.isTrue(tData.mMaterial.mMaterial)) {
								tCount = 0;
								break;
							}
						}
					} else {
						tCount = 0;
						break;
					}
				}
			}
			// F11 (ADR §7): ветки скана ЧУЖИХ ванильных neo-рецептов (ShapedRecipe/ShapelessRecipe) удалены —
			// в neo рецепты грузятся датапаком на СТАРТЕ СЕРВЕРА, на mod-init их нет; скан/удаление чужих
			// рецептов отложено централизованно (F10-compat). Дедуп СВОИХ GT-рецептов (ветки выше) — 1:1.
			
			if (tCount == 1) {
				OreDictItemData tData = OM.data(tRecipe.getRecipeOutput());
				if (tData != null && tData.mPrefix == mOutput) {
					tRecipeList.remove(i--);
				}
			}
			
		}} catch(Throwable e) {e.printStackTrace(ERR);}
	}
	
	@Override
	public boolean matches(CraftingInput aGrid, Level aWorld) {
		ItemStack tStack = null;
		OreDictMaterial rMaterial = null;
		
		int tInventorySize = aGrid.size(), tCounter = 0, tEmpty = 0; // F11: сетка neo подрезана до габарита — семантику пустых слотов сверить под parity (ADR §7)
		if (tInventorySize < 1+mEmpty) return F;
		for (int i = 0; i < tInventorySize; i++) {
			tStack = aGrid.getItem(i);
			if (ST.valid(tStack)) {
				OreDictItemData tData = OM.anydata_(tStack);
				if (tData == null || tData.mPrefix != mInput) return F;
				if (rMaterial == null) rMaterial = tData.mMaterial.mMaterial; else if (rMaterial != tData.mMaterial.mMaterial) return F;
				tCounter++;
				continue;
			}
			if (tCounter == 0) tEmpty++;
			if (i-tCounter+1+mEmpty-tEmpty >= tInventorySize) return F;
		}
		return rMaterial != null && tCounter == 1 && tEmpty % mInput.mShapelessManagersSingle.size() == mEmpty && hasOutputFor(rMaterial);
	}
	
	@Override
	public ItemStack getCraftingResult(CraftingInput aGrid) {
		for (int i = 0, j = aGrid.size(); i < j; i++) {
			OreDictItemData tData = OM.anydata(aGrid.getItem(i));
			if (tData == null || tData.mMaterial == null || !mCondition.isTrue(tData.mMaterial.mMaterial)) continue;
			return mOutput.mat(tData.mMaterial.mMaterial, mOutputCount);
		}
		return ERROR_OUTPUT.copy();
	}
	
	public boolean hasOutputFor(OreDictMaterial aMaterial) {
		return ST.valid(mOutput.mat(aMaterial, mOutputCount)) && mCondition.isTrue(aMaterial);
	}
	
	@Override public boolean isRemovableByGT() {return F;}
	@Override public boolean isAutocraftableByGT() {return mAutoCraftable;}
	@Override public int getRecipeSize() {return 1;}
	@Override public ItemStack getRecipeOutput() {return ERROR_OUTPUT.copy();}
}

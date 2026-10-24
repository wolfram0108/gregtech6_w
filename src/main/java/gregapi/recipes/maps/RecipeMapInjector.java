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

package gregapi.recipes.maps;

import java.util.Collection;

import gregapi.data.FL;
import gregapi.data.IL;
import gregapi.data.MD;
import gregapi.random.IHasWorldAndCoords;
import gregapi.recipes.Recipe;
import gregapi.recipes.Recipe.RecipeMap;
import gregapi.util.ST;
import net.neoforged.neoforge.fluids.FluidStack;
import net.minecraft.world.item.ItemStack;

import static gregapi.data.CS.*;

/**
 * The injector with the recharge of Railcraft's firestones: in 1.7.10 the wear of a firestone was its meta, and the recipes named two of its values (4999 and 5000);
 * in Reborn the wear is the damage of the stack, which the comparison of GT6's stacks does not see, so the recharge is found here for any wear instead:
 * lava costs 1000 mB per point of damage, hot IC2 coolant 4000, volcanic lava a fixed 312500, exactly the quantities of the two original recipes.
 */
public class RecipeMapInjector extends RecipeMap {
	public RecipeMapInjector(Collection<Recipe> aRecipeList, String aUnlocalizedName, String aNameLocal, String aNameNEI, long aProgressBarDirection, long aProgressBarAmount, String aNEIGUIPath, long aInputItemsCount, long aOutputItemsCount, long aMinimalInputItems, long aInputFluidCount, long aOutputFluidCount, long aMinimalInputFluids, long aMinimalInputs, long aPower, String aNEISpecialValuePre, long aNEISpecialValueMultiplier, String aNEISpecialValuePost, boolean aShowVoltageAmperageInNEI, boolean aNEIAllowed, boolean aConfigAllowed, boolean aNeedsOutputs, boolean aCombinePower, boolean aUseBucketSizeIn, boolean aUseBucketSizeOut) {
		super(aRecipeList, aUnlocalizedName, aNameLocal, aNameNEI, aProgressBarDirection, aProgressBarAmount, aNEIGUIPath, aInputItemsCount, aOutputItemsCount, aMinimalInputItems, aInputFluidCount, aOutputFluidCount, aMinimalInputFluids, aMinimalInputs, aPower, aNEISpecialValuePre, aNEISpecialValueMultiplier, aNEISpecialValuePost, F, aShowVoltageAmperageInNEI, aNEIAllowed, aConfigAllowed, aNeedsOutputs, aCombinePower, aUseBucketSizeIn, aUseBucketSizeOut);
	}

	@Override
	public Recipe findRecipe(IHasWorldAndCoords aTileEntity, Recipe aRecipe, boolean aNotUnificated, long aSize, ItemStack aSpecialSlot, FluidStack[] aFluids, ItemStack... aInputs) {
		Recipe rRecipe = super.findRecipe(aTileEntity, aRecipe, aNotUnificated, aSize, aSpecialSlot, aFluids, aInputs);
		if (rRecipe != null || !MD.RC.mLoaded || aInputs == null || aFluids == null || aFluids.length < 1 || aFluids[0] == null) return rRecipe;
		for (ItemStack aInput : aInputs) if (ST.valid(aInput) && aInput.getDamageValue() > 0) {
			boolean tRefined = IL.RC_Firestone_Refined.equal(aInput, T, T), tCracked = IL.RC_Firestone_Cracked.equal(aInput, T, T);
			if (!tRefined && !tCracked) continue;
			int tDamage = aInput.getDamageValue();
			ItemStack tFresh = (tRefined ? IL.RC_Firestone_Refined : IL.RC_Firestone_Cracked).get(1);
			if (ST.invalid(tFresh)) return null;
			if (FL.Lava.is(aFluids[0])) return new Recipe(F, F, F, ST.array(ST.amount(1, aInput)), ST.array(tFresh), null, null, FL.array(FL.Lava.make(1000L * tDamage)), FL.array(FL.Lava_Pahoehoe.make(1000L * tDamage)), tRefined ? 2048 : 4096, 16, 0);
			if (FL.Coolant_IC2_Hot.is(aFluids[0])) return new Recipe(F, F, F, ST.array(ST.amount(1, aInput)), ST.array(tFresh), null, null, FL.array(FL.Coolant_IC2_Hot.make(4000L * tDamage)), FL.array(FL.Coolant_IC2.make(4000L * tDamage)), tRefined ? 4096 : 8192, 16, 0);
			if (FL.Lava_Volcanic.exists() && FL.Lava_Volcanic.is(aFluids[0])) return new Recipe(F, F, F, ST.array(ST.amount(1, aInput)), ST.array(tFresh), null, null, FL.array(FL.Lava_Volcanic.make(312500)), FL.array(FL.Lava_Pahoehoe.make(312500)), tRefined ? 128 : 256, 16, 0);
			return null;
		}
		return null;
	}
}

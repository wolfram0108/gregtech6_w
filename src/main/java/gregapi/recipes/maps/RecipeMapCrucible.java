/**
 * Copyright (c) 2024 GregTech-6 Team
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

import gregapi.code.ArrayListNoNulls;
import gregapi.code.HashSetNoNulls;
import gregapi.data.OP;
import gregapi.data.TD;
import gregapi.code.ItemStackContainer;
import gregapi.oredict.OreDictItemData;
import gregapi.oredict.OreDictMaterial;
import gregapi.oredict.OreDictMaterialStack;
import gregapi.oredict.OreDictPrefix;
import gregapi.recipes.Recipe;
import gregapi.util.OM;
import gregapi.util.ST;
import gregapi.util.UT;
import net.minecraft.world.item.ItemStack;

import java.util.Collection;
import java.util.List;

import static gregapi.data.CS.*;
import static gregapi.data.TD.Processing.MELTING;

/**
 * @author Gregorius Techneticies
 */
public class RecipeMapCrucible extends RecipeMapSpecialSingleInput {
	public RecipeMapCrucible(Collection<Recipe> aRecipeList, String aUnlocalizedName, String aNameLocal, String aNameNEI, long aProgressBarDirection, long aProgressBarAmount, String aNEIGUIPath, long aInputItemsCount, long aOutputItemsCount, long aMinimalInputItems, long aInputFluidCount, long aOutputFluidCount, long aMinimalInputFluids, long aMinimalInputs, long aPower, String aNEISpecialValuePre, long aNEISpecialValueMultiplier, String aNEISpecialValuePost, boolean aShowVoltageAmperageInNEI, boolean aNEIAllowed, boolean aConfigAllowed, boolean aNeedsOutputs, boolean aCombinePower, boolean aUseBucketSizeIn, boolean aUseBucketSizeOut) {
		super(aRecipeList, aUnlocalizedName, aNameLocal, aNameNEI, aProgressBarDirection, aProgressBarAmount, aNEIGUIPath, aInputItemsCount, aOutputItemsCount, aMinimalInputItems, aInputFluidCount, aOutputFluidCount, aMinimalInputFluids, aMinimalInputs, aPower, aNEISpecialValuePre, aNEISpecialValueMultiplier, aNEISpecialValuePost, aShowVoltageAmperageInNEI, aNEIAllowed, aConfigAllowed, aNeedsOutputs, aCombinePower, aUseBucketSizeIn, aUseBucketSizeOut);
	}
	
	private List<Recipe> mBufferedDynamicRecipes = null;

	/** F11-JEI, класс «динамическая мапа исчезла из витрины»: рецепты плавки не хранятся, а ПОРОЖДАЮТСЯ
	 *  {@link #getRecipeFor} (1:1 оригинала). NEI 1.7.10 это устраивало — он спрашивал по клику на предмет
	 *  ({@code NEI_RecipeMap.loadCraftingRecipes/loadUsageRecipes:571,580}); JEI регистрирует категории заранее и
	 *  берёт список у {@code getNEIAllRecipes} ({@code GT6_JEI_Plugin:142-143}), поэтому пустой список = категории
	 *  НЕТ ВООБЩЕ (симптом игрока: «в JEI нет ни одного крафта тигля, хотя чёрный песок плавится»).
	 *  Приём восстановления тот же, что уже применён у {@code RecipeMapHammer:54} — материалы берутся из реестров
	 *  префиксов, стеки из самого материала, отбор делает сам {@code getRecipeFor} (не плавится → null → выпадает).
	 *  Так же ловятся и предметы-БЛОКИ: чёрный песок зарегистрирован как {@code blockDust} Магнетита
	 *  ({@code BlockSands:51-53}), то есть лежит в {@code mRegisteredItems} своего материала. */
	@Override
	public List<Recipe> getNEIAllRecipes() {
		List<Recipe> rList = super.getNEIAllRecipes();
		if (mBufferedDynamicRecipes == null) {
			mBufferedDynamicRecipes = new ArrayListNoNulls<>();
			HashSetNoNulls<OreDictMaterial> tMaterials = new HashSetNoNulls<>();
			// Префиксы — ровно те, что перечисляет getNEIRecipes ниже: набор входов тигля один, в двух местах не расходится.
			for (OreDictPrefix tPrefix : new OreDictPrefix[] {OP.ingot, OP.blockIngot, OP.gem, OP.blockGem, OP.dust, OP.blockDust
			, OP.crushed, OP.crushedPurified, OP.crushedCentrifuged, OP.chunk, OP.rubble, OP.pebbles, OP.cluster
			, OP.cleanGravel, OP.dirtyGravel, OP.crystalline, OP.reduced}) tMaterials.addAll(tPrefix.mRegisteredMaterials);
			for (OreDictMaterial tMaterial : tMaterials) for (ItemStackContainer tThing : tMaterial.mRegisteredItems) {
				Recipe tRecipe = getRecipeFor(tThing.toStack());
				if (tRecipe != null) mBufferedDynamicRecipes.add(tRecipe);
			}
		}
		rList.addAll(mBufferedDynamicRecipes);
		return rList;
	}

	@Override
	public List<Recipe> getNEIRecipes(ItemStack... aOutputs) {
		List<Recipe> rList = super.getNEIRecipes(aOutputs);
		HashSetNoNulls<OreDictMaterial> tSet = new HashSetNoNulls<>();
		for (ItemStack aOutput : aOutputs) {
			OreDictItemData aData = OM.anydata(aOutput);
			if (aData == null || !aData.validData() || !aData.mPrefix.contains(TD.Prefix.INGOT_BASED)) continue;
			for (OreDictMaterial tMat : aData.mMaterial.mMaterial.mTargetedSmelting) if (tSet.add(tMat) && !tMat.contains(TD.Properties.INVALID_MATERIAL) && tMat.mTargetSmelting.mMaterial == aData.mMaterial.mMaterial) {
				if (tMat != aData.mMaterial.mMaterial) {
				rList.add(getRecipeFor(OP.ingot             .mat(tMat, 1)));
				rList.add(getRecipeFor(OP.blockIngot        .mat(tMat, 1)));
				rList.add(getRecipeFor(OP.gem               .mat(tMat, 1)));
				rList.add(getRecipeFor(OP.blockGem          .mat(tMat, 1)));
				}
				rList.add(getRecipeFor(OP.dust              .mat(tMat, 1)));
				rList.add(getRecipeFor(OP.blockDust         .mat(tMat, 1)));
				rList.add(getRecipeFor(OP.crushed           .mat(tMat, 1)));
				rList.add(getRecipeFor(OP.crushedPurified   .mat(tMat, 1)));
				rList.add(getRecipeFor(OP.crushedCentrifuged.mat(tMat, 1)));
				rList.add(getRecipeFor(OP.chunk             .mat(tMat, 1)));
				rList.add(getRecipeFor(OP.rubble            .mat(tMat, 1)));
				rList.add(getRecipeFor(OP.pebbles           .mat(tMat, 1)));
				rList.add(getRecipeFor(OP.cluster           .mat(tMat, 1)));
				rList.add(getRecipeFor(OP.cleanGravel       .mat(tMat, 1)));
				rList.add(getRecipeFor(OP.dirtyGravel       .mat(tMat, 1)));
				rList.add(getRecipeFor(OP.crystalline       .mat(tMat, 1)));
				rList.add(getRecipeFor(OP.reduced           .mat(tMat, 1)));
			}
		}
		return rList;
	}
	
	@Override
	public Recipe getRecipeFor(ItemStack aInput) {
		OreDictItemData aData = OM.anydata(aInput);
		if (aData == null) return null;
		
		List<OreDictMaterialStack> tMaterialList = new ArrayListNoNulls<>();
		for (OreDictMaterialStack tMaterial : aData.getAllMaterialStacks()) if (tMaterial.mMaterial.mTargetSmelting.mAmount > 0 && tMaterial.mMaterial.contains(MELTING)) OM.stack(UT.Code.units(tMaterial.mAmount, U, tMaterial.mMaterial.mTargetSmelting.mAmount, F), tMaterial.mMaterial.mTargetSmelting.mMaterial).addToList(tMaterialList);
		if (tMaterialList.isEmpty()) return null;
		
		ArrayListNoNulls<ItemStack> tOutputList = ST.arraylist();
		for (OreDictMaterialStack tMaterial : tMaterialList) tOutputList.add(OM.ingotOrDust(tMaterial.mMaterial, tMaterial.mAmount));
		
		ItemStack[] tOutputs = tOutputList.toArray(ZL_IS);
		if (!ST.hasValid(tOutputs)) return null;
		
		return new Recipe(F, F, F, ST.array(ST.amount(1, aInput)), tOutputs, null, null, ZL_FS, ZL_FS, 0, 0, aData.mMaterial.mMaterial.mMeltingPoint);
	}
}

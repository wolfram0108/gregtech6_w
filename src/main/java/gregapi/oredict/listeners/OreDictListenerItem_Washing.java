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

package gregapi.oredict.listeners;
import gregapi.util.WD;

import gregapi.code.ArrayListNoNulls;
import gregapi.data.LH;
import gregapi.lang.LanguageHandler;
import gregapi.oredict.OreDictMaterial;
import gregapi.oredict.OreDictPrefix;
import gregapi.oredict.listeners.IOreDictListenerItem.OreDictListenerItem;
import gregapi.util.ST;
import gregapi.util.UT;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CauldronBlock;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;

import static gregapi.data.CS.RNGSUS;

/**
 * @author Gregorius Techneticies
 */
public class OreDictListenerItem_Washing extends OreDictListenerItem {
	private final OreDictPrefix mItemToGet, mByProductPrefixes[];
	private final int mChance;
	
	/**
	 * @param aItemToGet the Main Item you get from washing this Prefix.
	 * @param aChance the Chance of getting a secondary Output.
	 * @param aByProductPrefixes the Prefixes it can select from, if Items of those Prefixes exist. You can add the same Prefix multiple times to weight it.
	 */
	public OreDictListenerItem_Washing(OreDictPrefix aItemToGet, int aChance, OreDictPrefix... aByProductPrefixes) {
		LH.add("gt.behaviour.washing", "Throw into Cauldron to clean this Item");
		mByProductPrefixes = aByProductPrefixes;
		mItemToGet = aItemToGet;
		mChance = aChance;
	}
	
	@Override
	public ItemStack onTickWorld(OreDictPrefix aPrefix, OreDictMaterial aMaterial, ItemStack aStack, ItemEntity aItem) {
		if (aMaterial != null) {
			int tX = UT.Code.roundDown(aItem.getX()), tY = UT.Code.roundDown(aItem.getY()-0.25), tZ = UT.Code.roundDown(aItem.getZ());
			Block tBlock = WD.block(aItem.level(), tX, tY, tZ);
			// F-cauldron: 1.7.10 водяной котёл = CauldronBlock с metadata-уровнем (1-3); neo = LayeredCauldronBlock со
			// BlockState-свойством LEVEL (LayeredCauldronBlock.java:39). Уровень читаем из состояния (WD.meta не отражает — метаданных нет).
			net.minecraft.core.BlockPos tCauldronPos = new net.minecraft.core.BlockPos(tX, tY, tZ);
			net.minecraft.world.level.block.state.BlockState tCauldronState = aItem.level().getBlockState(tCauldronPos);

			if (tBlock instanceof net.minecraft.world.level.block.LayeredCauldronBlock && tCauldronState.getValue(net.minecraft.world.level.block.LayeredCauldronBlock.LEVEL) > 0) {
				ItemStack tStack = mItemToGet.mat(aMaterial, 1);
				if (tStack != null) {
					net.minecraft.world.level.block.LayeredCauldronBlock.lowerFillLevel(tCauldronState, aItem.level(), tCauldronPos); // 1.7.10 func_150024_a(...,meta-1) = декремент уровня на 1 (LayeredCauldronBlock.java:104).
					if (mByProductPrefixes.length > 0 && RNGSUS.nextInt(mChance) > 0) {
						ArrayListNoNulls<ItemStack> tStacks = ST.arraylist();
						for (OreDictPrefix tPrefix : mByProductPrefixes) tStacks.add(tPrefix.mat(UT.Code.select(aMaterial, aMaterial.mByProducts), 1));
						if (tStacks.size() > 0) ST.drop(aItem.level(), aItem.getX(), aItem.getY(), aItem.getZ(), tStacks.get(RNGSUS.nextInt(tStacks.size())));
					}
					ST.drop(aItem.level(), aItem.getX(), aItem.getY(), aItem.getZ(), tStack);
					aItem.setDeltaMovement(0, 0, 0); // 1.7.10 motionX=motionY=motionZ=0 (поля удалены) -> neo setDeltaMovement(0,0,0) (Entity.java:3672).
					aItem.setPos(tX+0.5, tY+0.9, tZ+0.5); // setPosition -> neo setPos (Entity.java:471).
					return aStack.getCount() > 1 ? ST.amount(aStack.getCount() - 1, aStack) : null;
				}
			}
		}
		return aStack;
	}
	
	@Override
	public String getListenerToolTip(OreDictPrefix aPrefix, OreDictMaterial aMaterial, ItemStack aStack) {
		return LanguageHandler.translate("gt.behaviour.washing", "Throw into Cauldron to clean this Item");
	}
}

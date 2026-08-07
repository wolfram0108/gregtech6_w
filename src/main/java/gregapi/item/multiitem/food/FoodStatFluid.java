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
 *
 * Modified in 2026 for the GregTech 6 NeoForge port
 * (https://github.com/wolfram0108/gregtech6_w): ported from Minecraft 1.7.10 / Forge
 * to Minecraft 26.1.2 / NeoForge.
 */

package gregapi.item.multiitem.food;

import static gregapi.data.CS.*;

import java.util.List;

import gregapi.data.CS.DrinksGT;
import gregapi.data.FL;
import gregapi.data.MD;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;

public class FoodStatFluid implements IFoodStat {
	public static final FoodStatFluid INSTANCE = new FoodStatFluid();
	
	@Override
	public int getFoodLevel(Item aItem, ItemStack aStack, Player aPlayer) {
		IFoodStat rStats = null;
		FluidStack tFluid = FL.getFluid(aStack, T);
		if (tFluid != null) rStats = DrinksGT.REGISTER.get(FL.regName(tFluid.getFluid()));
		if (rStats == null) return 0;
		return rStats.getFoodLevel(aItem, aStack, aPlayer);
	}
	
	@Override
	public float getSaturation(Item aItem, ItemStack aStack, Player aPlayer) {
		IFoodStat rStats = null;
		FluidStack tFluid = FL.getFluid(aStack, T);
		if (tFluid != null) rStats = DrinksGT.REGISTER.get(FL.regName(tFluid.getFluid()));
		if (rStats == null) return 0;
		return rStats.getSaturation(aItem, aStack, aPlayer);
	}
	
	@Override
	public float getHydration(Item aItem, ItemStack aStack, Player aPlayer) {
		IFoodStat rStats = null;
		FluidStack tFluid = FL.getFluid(aStack, T);
		if (tFluid != null) rStats = DrinksGT.REGISTER.get(FL.regName(tFluid.getFluid()));
		if (rStats == null) return 0;
		return rStats.getHydration(aItem, aStack, aPlayer);
	}
	
	@Override
	public float getTemperature(Item aItem, ItemStack aStack, Player aPlayer) {
		IFoodStat rStats = null;
		FluidStack tFluid = FL.getFluid(aStack, T);
		if (tFluid != null) rStats = DrinksGT.REGISTER.get(FL.regName(tFluid.getFluid()));
		if (rStats == null) return DEF_ENV_TEMP;
		return rStats.getTemperature(aItem, aStack, aPlayer);
	}
	
	@Override
	public float getTemperatureEffect(Item aItem, ItemStack aStack, Player aPlayer) {
		IFoodStat rStats = null;
		FluidStack tFluid = FL.getFluid(aStack, T);
		if (tFluid != null) rStats = DrinksGT.REGISTER.get(FL.regName(tFluid.getFluid()));
		if (rStats == null) return 0;
		return rStats.getTemperatureEffect(aItem, aStack, aPlayer);
	}
	
	@Override
	public boolean alwaysEdible(Item aItem, ItemStack aStack, Player aPlayer) {
		IFoodStat rStats = null;
		FluidStack tFluid = FL.getFluid(aStack, T);
		if (tFluid != null) rStats = DrinksGT.REGISTER.get(FL.regName(tFluid.getFluid()));
		if (rStats == null) return DRINKS_ALWAYS_DRINKABLE || MD.ENVM.mLoaded;
		return rStats.alwaysEdible(aItem, aStack, aPlayer);
	}
	
	@Override
	public boolean isRotten(Item aItem, ItemStack aStack, Player aPlayer) {
		IFoodStat rStats = null;
		FluidStack tFluid = FL.getFluid(aStack, T);
		if (tFluid != null) rStats = DrinksGT.REGISTER.get(FL.regName(tFluid.getFluid()));
		if (rStats == null) return F;
		return rStats.isRotten(aItem, aStack, aPlayer);
	}
	
	@Override
	public ItemUseAnimation getFoodAction(Item aItem, ItemStack aStack) {
		IFoodStat rStats = null;
		FluidStack tFluid = FL.getFluid(aStack, T);
		if (tFluid != null) rStats = DrinksGT.REGISTER.get(FL.regName(tFluid.getFluid()));
		if (rStats == null) return ItemUseAnimation.DRINK; // было ItemUseAnimation.drink (1.7.10 enum-конвенция) -> UPPER_CASE (ItemUseAnimation.java:17)
		return rStats.getFoodAction(aItem, aStack);
	}
	
	@Override
	public boolean useAppleCoreFunctionality(Item aItem, ItemStack aStack, Player aPlayer) {
		IFoodStat rStats = null;
		FluidStack tFluid = FL.getFluid(aStack, T);
		if (tFluid != null) rStats = DrinksGT.REGISTER.get(FL.regName(tFluid.getFluid()));
		if (rStats == null) return F;
		return rStats.useAppleCoreFunctionality(aItem, aStack, aPlayer);
	}
	
	@Override
	@SuppressWarnings("deprecation")
	public void onEaten(Item aItem, ItemStack aStack, Player aPlayer, boolean aConsumeItem) {
		IFoodStat rStats = null;
		FluidStack tFluid = FL.getFluid(aStack, T);
		if (tFluid != null) rStats = DrinksGT.REGISTER.get(FL.regName(tFluid.getFluid()));
		if (rStats == null) return;
		rStats.onEaten(aItem, aStack, aPlayer, aConsumeItem, T);
	}
	
	@Override
	public void onEaten(Item aItem, ItemStack aStack, Player aPlayer, boolean aConsumeItem, boolean aMakeSound) {
		IFoodStat rStats = null;
		FluidStack tFluid = FL.getFluid(aStack, T);
		if (tFluid != null) rStats = DrinksGT.REGISTER.get(FL.regName(tFluid.getFluid()));
		if (rStats == null) return;
		rStats.onEaten(aItem, aStack, aPlayer, aConsumeItem, aMakeSound);
	}
	
	@Override
	public void addAdditionalToolTips(Item aItem, List<String> aList, ItemStack aStack, boolean aF3_H) {
		IFoodStat rStats = null;
		FluidStack tFluid = FL.getFluid(aStack, T);
		if (tFluid != null) rStats = DrinksGT.REGISTER.get(FL.regName(tFluid.getFluid()));
		if (rStats == null) return;
		rStats.addAdditionalToolTips(aItem, aList, aStack, aF3_H);
	}
}

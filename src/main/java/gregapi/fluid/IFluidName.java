/**
 * Copyright (c) 2026 wolfram0108
 *
 * Written in 2026 for the GregTech 6 NeoForge port
 * (https://github.com/wolfram0108/gregtech6_w). Not part of the original GregTech 6
 * by Gregorius Techneticies; distributed under the same licence as the work it extends.
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

import java.util.Collection;
import java.util.List;

import gregapi.code.ArrayListNoNulls;
import gregapi.data.FL;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidTank;

import static gregapi.data.CS.*;

/** A GT fluid by its name, as the constants of FL name them: what a constant does with its fluid lives here once, for FL and for
 *  an addon's own list of fluids alike. */
public interface IFluidName {
	/** The name the fluid is registered and listed under. */
	String fluidName();
	/** Every name the fluid answers to, older ones included. */
	default String[] allNames() {return new String[] {fluidName()};}

	/** Puts aName into each of the fluid lists a constant names. */
	@SafeVarargs
	static void addTo(String aName, Collection<String>... aFluidSets) {for (Collection<String> aFluidSet : aFluidSets) aFluidSet.add(aName);}

	/** Returns the real registry position (stable once registered); the numbering itself is not semantically
	 *  meaningful, unlike the old global fluid id. */
	default int id() {Fluid tFluid = fluid(); return tFluid == null ? -1 : BuiltInRegistries.FLUID.getId(tFluid);}
	default Fluid fluid() {return FL.fluid_(fluidName());}
	default boolean exists() {return fluid() != null;}
	default ItemStack display() {return FL.display(make(0), F, F);}
	default ItemStack display(long aAmount) {return FL.display(make(aAmount), aAmount, F, F);}

	default FluidStack make (long aAmount) {return FL.make (fluidName(), aAmount);}
	default FluidStack make_(long aAmount) {return FL.make_(fluidName(), aAmount);}
	default FluidStack make (long aAmount, String aReplacement) {return FL.make (fluidName(), aAmount, aReplacement);}
	default FluidStack make_(long aAmount, String aReplacement) {return FL.make_(fluidName(), aAmount, aReplacement);}
	default FluidStack make (long aAmount, String aReplacement, long aReplacementAmount) {return FL.make (fluidName(), aAmount, aReplacement, aReplacementAmount);}
	default FluidStack make_(long aAmount, String aReplacement, long aReplacementAmount) {return FL.make_(fluidName(), aAmount, aReplacement, aReplacementAmount);}

	default boolean is(IFluidTank aTank) {return is(aTank.getFluid());}
	default boolean is(FluidStack aFluid) {return aFluid != null && is(aFluid.getFluid());}
	default boolean is(Fluid aFluid) {return aFluid != null && is(FluidGT.nameOf(aFluid));}
	default boolean is(String aFluidName) {return fluidName().equalsIgnoreCase(aFluidName);}
	default boolean is(Collection<String> aFluidSet) {return aFluidSet.contains(fluidName());}

	default ItemStack fill(ItemStack aStack) {return FL.fill(make(Integer.MAX_VALUE), aStack, F, T, T, F);}

	default List<FluidStack> list(long aAmount) {
		List<FluidStack> rList = new ArrayListNoNulls<>();
		for (String tName : allNames()) rList.add(FL.make(tName, aAmount));
		return rList;
	}
}

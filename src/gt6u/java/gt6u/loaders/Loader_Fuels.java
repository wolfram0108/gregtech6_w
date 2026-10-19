/**
 * Copyright (c) 2020 Gregtech6-Unofficial Team
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
 * the GregTech6-Unofficial entries of gregtech/loaders/b/Loader_Fuels.java moved into an addon registry.
 */

package gt6u.loaders;


import static gregapi.data.CS.*;
import gregapi.data.CS.FluidsGT;
import gregapi.data.FL;
import gregapi.data.FM;
import gregapi.data.MT;
import gregapi.data.OP;
import gregapi.oredict.OreDictMaterial;
import gregapi.util.OM;
import gregapi.util.UT;
import net.minecraftforge.fluids.FluidStack;
import gt6u.GT6U_Recipes;

/** GregTech6-Unofficial's edits of gregtech/loaders/b/Loader_Fuels.java, as statements: what GT6U added runs in run(), what it dropped is taken back in
 *  takeBack(): restated as GT6 makes them, their Recipes are taken out of GT6's maps again. */
public class Loader_Fuels {
	public static void run() {
		run_run();
	}
	
	public static void takeBack() {
		takeBack_run();
	}
	
	/** GT6U's additions to Loader_Fuels.run (GT6U gregtech/loaders/b/Loader_Fuels.java). */
	static void run_run() {
		FM.Plasma .addRecipe0(T, - 4096, 2, FL.make("heliumplasma", 1) , FL.make("helium", 1), ZL_IS);
		FM.Plasma .addRecipe0(T, - 4096, 4, FL.make("nitrogenplasma", 1) , FL.make("nitrogen", 1), ZL_IS);
		FM.Plasma .addRecipe0(T, - 8192, 2, FL.make("oxygenplasma", 1) , FL.make("oxygen", 1), ZL_IS);
		FM.Plasma .addRecipe0(T, - 8192, 4, FL.make("ironplasma", 1) , MT.Fe.fluid(U1000*1, F), ZL_IS);
		FM.Engine .addRecipe0(T, - 64, 7, FL.make("lpg", 1) , FL.make("carbondioxide", 1), ZL_IS);
		FM.Burn .addRecipe0(T, - 64, 4, FL.make("gasoil", 1) , FL.make("carbondioxide", 1), ZL_IS);
		FM.Engine .addRecipe0(T, - 64, 6, FL.make("gasoil", 1) , FL.make("carbondioxide", 1), ZL_IS);
		FM.Burn .addRecipe0(T, - 64, 5, FL.make("naphtha", 1) , FL.make("carbondioxide", 1), ZL_IS);
		FM.Engine .addRecipe0(T, - 64, 7, FL.make("naphtha", 1) , FL.make("carbondioxide", 1), ZL_IS);
		if (FL.LPG.exists()) {
			FM.Burn.addRecipe0(T, -64, 42, FL.LPG.make(7), FL.Water.make(7), FL.make("carbondioxide", 6));
		}
	}

	/** GT6's statements in Loader_Fuels.run that GT6U dropped, as the port has them: their Recipes are taken back. */
	static void takeBack_run() {
		GT6U_Recipes.takeBack("Loader_Fuels.run", () -> {
			if (FL.LPG.exists()) {
				for (FluidStack tFuel : FL.LPG.list(7)) {
					FM.Burn .addRecipe0(T, - 64, 42, tFuel , FL.Water.make(7), FL.CarbonDioxide.make(6));
				}
			}
		});
	}
}

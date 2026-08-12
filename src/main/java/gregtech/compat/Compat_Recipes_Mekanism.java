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

package gregtech.compat;

import static gregapi.data.CS.*;

import gregapi.api.FMLPostInitializationEvent;
import gregapi.api.Abstract_Mod;
import gregapi.code.ModData;
import gregapi.compat.CompatMods;
import gregapi.data.FL;
import gregapi.data.MD;
import gregapi.data.RM;
import gregapi.util.CR;
import gregapi.util.ST;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

public class Compat_Recipes_Mekanism extends CompatMods {
	public Compat_Recipes_Mekanism(ModData aMod, Abstract_Mod aGTMod) {super(aMod, aGTMod);}
	
	@Override public void onPostLoad(FMLPostInitializationEvent aInitEvent) {OUT.println("GT_Mod: Doing Mekanism Recipes.");
		ItemStack x = ST.make(MD.Mek, "Salt", 1, 0);
		CR.remove(x, x, NI, x, x);
		
		for (int i = 0; i < 16; i++) for (FluidStack tDye : DYE_FLUIDS[i]) {
			RM.Bath.addRecipe1(T, 0, 16, ST.make(MD.Mek, "Balloon"               , 1, W), FL.mul(tDye, 1,16, T), NF, ST.make(MD.Mek, "Balloon"               , 1, i));
			RM.Bath.addRecipe1(T, 0, 16, ST.make(MD.Mek, "PlasticFence"          , 1, W), FL.mul(tDye, 1,16, T), NF, ST.make(MD.Mek, "PlasticFence"          , 1, i));
			RM.Bath.addRecipe1(T, 0, 16, ST.make(MD.Mek, "GlowPanel"             , 1, W), FL.mul(tDye, 1,16, T), NF, ST.make(MD.Mek, "GlowPanel"             , 1, i));
			RM.Bath.addRecipe1(T, 0, 16, ST.make(MD.Mek, "RoadPlasticBlock"      , 1, W), FL.mul(tDye, 1,16, T), NF, ST.make(MD.Mek, "RoadPlasticBlock"      , 1, i));
			RM.Bath.addRecipe1(T, 0, 16, ST.make(MD.Mek, "PlasticBlock"          , 1, W), FL.mul(tDye, 1,16, T), NF, ST.make(MD.Mek, "PlasticBlock"          , 1, i));
			RM.Bath.addRecipe1(T, 0, 16, ST.make(MD.Mek, "SlickPlasticBlock"     , 1, W), FL.mul(tDye, 1,16, T), NF, ST.make(MD.Mek, "SlickPlasticBlock"     , 1, i));
			RM.Bath.addRecipe1(T, 0, 16, ST.make(MD.Mek, "GlowPlasticBlock"      , 1, W), FL.mul(tDye, 1,16, T), NF, ST.make(MD.Mek, "GlowPlasticBlock"      , 1, i));
			RM.Bath.addRecipe1(T, 0, 16, ST.make(MD.Mek, "ReinforcedPlasticBlock", 1, W), FL.mul(tDye, 1,16, T), NF, ST.make(MD.Mek, "ReinforcedPlasticBlock", 1, i));
		}
	}
}

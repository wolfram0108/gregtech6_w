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
 * the GregTech6-Unofficial entries of gregtech/compat/Compat_Recipes_IndustrialCraft.java moved into an addon registry.
 */

package gt6u.compat;

import gregapi.api.Abstract_Mod;
import gregapi.api.FMLPostInitializationEvent;
import gregapi.code.ModData;
import gregapi.compat.CompatMods;
import gregapi.data.IL;
import gregapi.old.GT_BaseCrop;

/** GregTech6-Unofficial's part of gregtech/compat/Compat_Recipes_IndustrialCraft.java: its Coconut crop, made only with IC2 loaded. */
public class Compat_Recipes_IndustrialCraft extends CompatMods {
	public Compat_Recipes_IndustrialCraft(ModData aMod, Abstract_Mod aGTMod) {super(aMod, aGTMod);}
	
	@Override public void onPostLoad(FMLPostInitializationEvent aInitEvent) {
		new GT_BaseCrop("Coconut" , "Duck_egg" , IL.Food_Coconut.get(1) , null , IL.Food_Coconut.get(4) , 3, 4, 0, 1, 4, 1, 4, 0, 2, 4, new String[] {"Bush" , "Food", "Nut"});
	}
}

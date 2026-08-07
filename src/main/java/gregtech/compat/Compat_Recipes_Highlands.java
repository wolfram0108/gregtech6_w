/**
 * Copyright (c) 2019 Gregorius Techneticies
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
import gregapi.data.MD;
import gregapi.data.MT;
import gregapi.data.RM;
import gregapi.util.OM;
import gregapi.util.ST;
import net.minecraft.world.item.Items;

public class Compat_Recipes_Highlands extends CompatMods {
	public Compat_Recipes_Highlands(ModData aMod, Abstract_Mod aGTMod) {super(aMod, aGTMod);}
	
	@Override public void onPostLoad(FMLPostInitializationEvent aInitEvent) {OUT.println("GT_Mod: Doing Highlands Recipes.");
		RM.Squeezer.addRecipe1(T, 16, 16, ST.make(MD.HiL, "tile.hl_blueFlower"  , 1, W), NF, DYE_FLUIDS_FLOWER[DYE_INDEX_Cyan], ST.make(Items.INK_SAC, 1, DYE_INDEX_Cyan));
		RM.Squeezer.addRecipe1(T, 16, 16, ST.make(MD.HiL, "tile.hl_lavender"    , 1, W), NF, DYE_FLUIDS_FLOWER[DYE_INDEX_Purple], ST.make(Items.INK_SAC, 1, DYE_INDEX_Purple));
		RM.Squeezer.addRecipe1(T, 16, 16, ST.make(MD.HiL, "tile.hl_whiteFlower" , 1, W), NF, DYE_FLUIDS_FLOWER[DYE_INDEX_White], OM.dust(MT.White));
		RM.Squeezer.addRecipe1(T, 16, 16, ST.make(MD.HiL, "tile.hl_cotton"      , 1, W), ST.make(Items.STRING, 2, 0));
		
		RM.Juicer.addRecipe1(T, 16, 16, ST.make(MD.HiL, "tile.hl_blueFlower"    , 1, W), NF, DYE_FLUIDS_FLOWER[DYE_INDEX_Cyan], ST.make(Items.INK_SAC, 1, DYE_INDEX_Cyan));
		RM.Juicer.addRecipe1(T, 16, 16, ST.make(MD.HiL, "tile.hl_lavender"      , 1, W), NF, DYE_FLUIDS_FLOWER[DYE_INDEX_Purple], ST.make(Items.INK_SAC, 1, DYE_INDEX_Purple));
		RM.Juicer.addRecipe1(T, 16, 16, ST.make(MD.HiL, "tile.hl_whiteFlower"   , 1, W), NF, DYE_FLUIDS_FLOWER[DYE_INDEX_White], OM.dust(MT.White));
		RM.Juicer.addRecipe1(T, 16, 16, ST.make(MD.HiL, "tile.hl_cotton"        , 1, W), ST.make(Items.STRING, 1, 0));
		
		if (ENABLE_ADDING_IC2_EXTRACTOR_RECIPES) {
		RM.ic2_extractor(ST.make(MD.HiL, "tile.hl_blueFlower"   , 1, W), ST.make(Items.INK_SAC, 2, DYE_INDEX_Cyan));
		RM.ic2_extractor(ST.make(MD.HiL, "tile.hl_lavender"     , 1, W), ST.make(Items.INK_SAC, 2, DYE_INDEX_Purple));
		RM.ic2_extractor(ST.make(MD.HiL, "tile.hl_whiteFlower"  , 1, W), OM.dust(MT.White, U*2));
		RM.ic2_extractor(ST.make(MD.HiL, "tile.hl_cotton"       , 1, W), ST.make(Items.STRING, 2, 0));
		}
	}
}

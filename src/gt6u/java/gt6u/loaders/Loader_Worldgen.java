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
 * Modified in 2026 for the GregTech 6 NeoForge port (https://github.com/wolfram0108/gregtech6_w):
 * GregTech6-Unofficial's ores of a stone layer of gregtech/loaders/b/Loader_Worldgen.java, added to GT6's layer by an addon.
 */

package gt6u.loaders;

import gregapi.data.MT;
import gregapi.worldgen.StoneLayer;
import gregapi.worldgen.StoneLayerOres;

import static gregapi.data.CS.*;

/** GT6U adds copper and the tin ores to the stone layer of jaspers and tiger eyes (GT6U Loader_Worldgen.java:129-132). */
public class Loader_Worldgen {
	public static void run() {
		for (StoneLayer tLayer : StoneLayer.LAYERS) if (tLayer.mOresDeclared.stream().anyMatch(tOre -> tOre.mMaterial == MT.TigerIron)) {
			tLayer.addOres(
			  new StoneLayerOres(MT.Cu                      , U12, 32, 80, BIOMES_MOUNTAINS)
			, new StoneLayerOres(MT.OREMATS.Cassiterite     , U16, 40, 80)
			, new StoneLayerOres(MT.OREMATS.Stannite        , U16, 40, 82)
			, new StoneLayerOres(MT.OREMATS.Kesterite       , U16, 40, 82)
			);
			return;
		}
		ERR.println("GT6U: the stone layer of jaspers and tiger eyes was not found, its ores are not added");
	}
}

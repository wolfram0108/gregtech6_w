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
 * the GregTech6-Unofficial additions of gregapi/data/OP.java moved into an addon class.
 */

package gt6u.data;

import gregapi.data.OP;
import gregapi.data.TC;
import gregapi.oredict.OreDictPrefix;

import static gregapi.data.CS.*;
import static gregapi.data.TD.Prefix.*;
import static gt6u.data.TD_GT6U.*;

/** The prefixes GregTech6-Unofficial added to OP (GT6U OP.java:298-329, :551), made with OP's own helpers. */
public class OP_GT6U extends OP {
	public static final OreDictPrefix
	wireLPGt16                  = create("wireLPGt16"                     , "16x Wires"                       , "16x "                            , " Wire"                           ).setMaterialStats(U * 8)     .setCondition(OPTICALFIBERS)                                                                                        .add(UNIFICATABLE, BURNABLE, UNIFICATABLE_RECIPES, RECYCLABLE, SIMPLIFIABLE, SCANNABLE, EXTRUDER_FODDER).setMinStacksize( 2).aspects(TC.ELECTRUM, 1),
	wireLPGt12                  = create("wireLPGt12"                     , "12x Wires"                       , "12x "                            , " Wire"                           ).setMaterialStats(U * 6)     .setCondition(OPTICALFIBERS)                                                                                        .add(UNIFICATABLE, BURNABLE, UNIFICATABLE_RECIPES, RECYCLABLE, SIMPLIFIABLE, SCANNABLE, EXTRUDER_FODDER).setMinStacksize( 2).aspects(TC.ELECTRUM, 1),
	wireLPGt08                  = create("wireLPGt08"                     , "8x Wires"                        , "8x "                             , " Wire"                           ).setMaterialStats(U * 4)     .setCondition(OPTICALFIBERS)                                                                                        .add(UNIFICATABLE, BURNABLE, UNIFICATABLE_RECIPES, RECYCLABLE, SIMPLIFIABLE, SCANNABLE, EXTRUDER_FODDER).setMinStacksize( 2).aspects(TC.ELECTRUM, 1),
	wireLPGt04                  = create("wireLPGt04"                     , "4x Wires"                        , "4x "                             , " Wire"                           ).setMaterialStats(U * 2)     .setCondition(OPTICALFIBERS)                                                                                        .add(UNIFICATABLE, BURNABLE, UNIFICATABLE_RECIPES, RECYCLABLE, SIMPLIFIABLE, SCANNABLE, EXTRUDER_FODDER).setMinStacksize( 4).aspects(TC.ELECTRUM, 1),
	wireLPGt02                  = create("wireLPGt02"                     , "2x Wires"                        , "2x "                             , " Wire"                           ).setMaterialStats(U    )     .setCondition(OPTICALFIBERS)                                                                                        .add(UNIFICATABLE, BURNABLE, UNIFICATABLE_RECIPES, RECYCLABLE, SIMPLIFIABLE, SCANNABLE, EXTRUDER_FODDER).setMinStacksize( 8).aspects(TC.ELECTRUM, 1),
	wireLPGt01                  = create("wireLPGt01"                     , "1x Wires"                        , "1x "                             , " Wire"                           ).setMaterialStats(U2   )     .setCondition(OPTICALFIBERS)                                                                                        .add(UNIFICATABLE, BURNABLE, UNIFICATABLE_RECIPES, RECYCLABLE, SIMPLIFIABLE, SCANNABLE, EXTRUDER_FODDER).setMinStacksize(16).aspects(TC.ELECTRUM, 1),
	cableLPGt16                   = create("cableLPGt16"                    , "16x Optical Fiber"                      , "16x "                            , "  Optical Fiber"                          ).setMaterialStats(U * 8)     .setCondition(OPTICALFIBERS)                                                                                        .add(UNIFICATABLE, BURNABLE, UNIFICATABLE_RECIPES, RECYCLABLE, SIMPLIFIABLE).setMinStacksize( 1).aspects(TC.ELECTRUM, 1),
	cableLPGt12                   = create("cableLPGt12"                    , "12x Optical Fiber"                      , "12x "                            , "  Optical Fiber"                          ).setMaterialStats(U * 6)     .setCondition(OPTICALFIBERS)                                                                                        .add(UNIFICATABLE, BURNABLE, UNIFICATABLE_RECIPES, RECYCLABLE, SIMPLIFIABLE).setMinStacksize( 2).aspects(TC.ELECTRUM, 1),
	cableLPGt08                   = create("cableLPGt08"                    , "8x Optical Fiber"                       , "8x "                             , "  Optical Fiber"                          ).setMaterialStats(U * 4)     .setCondition(OPTICALFIBERS)                                                                                        .add(UNIFICATABLE, BURNABLE, UNIFICATABLE_RECIPES, RECYCLABLE, SIMPLIFIABLE).setMinStacksize( 2).aspects(TC.ELECTRUM, 1),
	cableLPGt04                   = create("cableLPGt04"                    , "4x Optical Fiber"                       , "4x "                             , "  Optical Fiber"                          ).setMaterialStats(U * 2)     .setCondition(OPTICALFIBERS)                                                                                        .add(UNIFICATABLE, BURNABLE, UNIFICATABLE_RECIPES, RECYCLABLE, SIMPLIFIABLE).setMinStacksize( 4).aspects(TC.ELECTRUM, 1),
	cableLPGt02                   = create("cableLPGt02"                    , "2x Optical Fiber"                       , "2x "                             , "  Optical Fiber"                          ).setMaterialStats(U    )     .setCondition(OPTICALFIBERS)                                                                                        .add(UNIFICATABLE, BURNABLE, UNIFICATABLE_RECIPES, RECYCLABLE, SIMPLIFIABLE).setMinStacksize( 8).aspects(TC.ELECTRUM, 1),
	cableLPGt01                   = create("cableLPGt01"                    , "1x Optical Fiber"                       , "1x "                             , "  Optical Fiber"                          ).setMaterialStats(U2   )     .setCondition(OPTICALFIBERS)                                                                                        .add(UNIFICATABLE, BURNABLE, UNIFICATABLE_RECIPES, RECYCLABLE, SIMPLIFIABLE).setMinStacksize(16).aspects(TC.ELECTRUM, 1);
	
	public static final OreDictPrefix wireLPGt[] = {wireLPGt01, wireLPGt02, wireLPGt04, wireLPGt08, wireLPGt12, wireLPGt16};
}

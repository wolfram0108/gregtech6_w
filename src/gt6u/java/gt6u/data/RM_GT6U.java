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
 * the GregTech6-Unofficial additions of gregapi/data/RM.java moved into an addon class.
 */

package gt6u.data;

import gregapi.recipes.Recipe.RecipeMap;
import gt6u.api.recipes.maps.RecipeMapFuelCleaner;

import static gregapi.data.CS.*;

/** The recipe maps GregTech6-Unofficial added to RM (GT6U RM.java:153-164); its redefinitions of GT6's maps are in GT6U_Definitions. */
public class RM_GT6U {
	public static final RecipeMap
	  OilCleaner                = new RecipeMapFuelCleaner          (null, "gt.recipe.oilcleaner"                , "Oil Cleaner"                     , null, 0, 1, RES_PATH_GUI+"machines/Mixer"                     ,/*IN-OUT-MIN-ITEM=*/ 6, 1, 0,/*IN-OUT-MIN-FLUID=*/ 6, 2, 0,/*MIN*/ 2,/*AMP=*/ 1, ""                    ,    1, ""      , T, T, T, T, F, F, F)
	, Fluidsolidifier           = new RecipeMap                     (null, "gt.recipe.fluidsolidifier"              , "Fluid Solidifier"                , null, 0, 1, RES_PATH_GUI+"machines/Generifier"                ,/*IN-OUT-MIN-ITEM=*/ 1, 1, 0,/*IN-OUT-MIN-FLUID=*/ 1, 1, 0,/*MIN*/ 1,/*AMP=*/ 1, ""                    ,    1, ""      , T, T, T, T, F, F, F)
	, CrackingTower             = new RecipeMap                     (null, "gt.recipe.crackingtower"                , "Cracking Tower"                  , null, 0, 1, RES_PATH_GUI+"machines/SteamCracking"             ,/*IN-OUT-MIN-ITEM=*/ 1, 3, 0,/*IN-OUT-MIN-FLUID=*/ 2, 9, 1,/*MIN*/ 2,/*AMP=*/ 1, ""                    ,    1, ""      , T, T, T, T, F, F, F)
	, ParticleCollider          = new RecipeMap                     (null, "gt.recipe.particlecollider"             , "Particle Collider"               , null, 0, 1, RES_PATH_GUI+"machines/Fusion"                    ,/*IN-OUT-MIN-ITEM=*/ 2, 6, 1,/*IN-OUT-MIN-FLUID=*/ 2, 6, 0,/*MIN*/ 2,/*AMP=*/ 1, "Start: "             ,    1, " EU"   , T, T, T, T, F, F, F)
	, Ionizer                   = new RecipeMap                     (null, "gt.recipe.ionizer"                      , "Ionizer"                         , null, 0, 1, RES_PATH_GUI+"machines/Ionizer"                   ,/*IN-OUT-MIN-ITEM=*/ 1, 1, 0,/*IN-OUT-MIN-FLUID=*/ 1, 1, 0,/*MIN*/ 1,/*AMP=*/ 1, ""                    ,    1, ""      , T, T, T, T, F, F, F)
	, RPP                       = new RecipeMap                     (null, "gt.recipe.radiationprocessing"          , "radiationprocessing"             , null, 0, 1, RES_PATH_GUI+"machines/Lightning"                 ,/*IN-OUT-MIN-ITEM=*/ 6, 6, 0,/*IN-OUT-MIN-FLUID=*/ 6, 6, 0,/*MIN*/ 2,/*AMP=*/ 1, ""                    ,    1, ""      , T, T, T, T, F, F, F)
	, MaskAligner               = new RecipeMap                     (null, "gt.recipe.maskaligner"                  , "Mask Aligner"                    , null, 0, 1, RES_PATH_GUI+"machines/MaskAligner"               ,/*IN-OUT-MIN-ITEM=*/ 9, 1, 2,/*IN-OUT-MIN-FLUID=*/ 1, 0, 0,/*MIN*/ 2,/*AMP=*/ 1, ""                    ,    1, ""      , T, T, T, T, F, F, F)
	, LaserCutter               = new RecipeMap                     (null, "gt.recipe.lasercutter"                  , "Laser Cutter"                    , null, 0, 1, RES_PATH_GUI+"machines/LaserCutter"               ,/*IN-OUT-MIN-ITEM=*/ 1, 3, 1,/*IN-OUT-MIN-FLUID=*/ 0, 0, 0,/*MIN*/ 0,/*AMP=*/ 1, ""                    ,    1, ""      , T, T, T, T, F, F, F)
	, Incubator                 = new RecipeMap                     (null, "gt.recipe.incubator"                    , "Incubator"                       , null, 0, 1, RES_PATH_GUI+"machines/Incubator"                 ,/*IN-OUT-MIN-ITEM=*/ 3, 3, 1,/*IN-OUT-MIN-FLUID=*/ 3, 3, 0,/*MIN*/ 1,/*AMP=*/ 1, ""                    ,    1, ""      , T, T, T, T, F, F, F)
	, Well                      = new RecipeMap                     (null, "gt.recipe.well"                         , "Well"                            , null, 0, 1, RES_PATH_GUI+"machines/Generifier"                ,/*IN-OUT-MIN-ITEM=*/ 1, 1, 1,/*IN-OUT-MIN-FLUID=*/ 1, 1, 0,/*MIN*/ 1,/*AMP=*/ 1, ""                    ,    1, ""      , T, T, T, T, F, F, F)
	, BioLab                    = new RecipeMap                     (null, "gt.recipe.biolab"                       , "Biochemical Research Lab"        , null, 0, 1, RES_PATH_GUI+"machines/BioLab"                    ,/*IN-OUT-MIN-ITEM=*/ 6, 6, 0,/*IN-OUT-MIN-FLUID=*/ 6, 6, 0,/*MIN*/ 1,/*AMP=*/ 1, "Disinfection: "      ,    1, " LU"   , T, T, T, T, F, F, F)
	, AquaticFarm               = new RecipeMap                     (null, "gt.recipe.aquaticfarm"                  , "Aquatic Farm"                    , null, 0, 1, RES_PATH_GUI+"machines/Sluice"                    ,/*IN-OUT-MIN-ITEM=*/ 1, 9, 1,/*IN-OUT-MIN-FLUID=*/ 1, 1, 1,/*MIN*/ 2,/*AMP=*/ 1, ""                    ,    1, ""      , T, T, T, T, F, T, T);
}

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
 * the GregTech6-Unofficial additions of gregapi/data/FL.java moved into an addon class.
 */

package gt6u.data;

import java.util.Collection;

import gregapi.fluid.IFluidName;

import static gregapi.data.CS.FluidsGT.*;

/** The fluid names GregTech6-Unofficial added to FL (GT6U FL.java); what a constant does with its fluid is IFluidName's, as for FL. */
public enum FL_GT6U implements IFluidName {
	  HeliumP                   ("heliumplasma"                                             , PLASMA)
	, NitrogenP                 ("nitrogenplasma"                                           , PLASMA)
	, OxygenP                   ("oxygenplasma"                                             , PLASMA)
	, IronP                     ("ironplasma"                                               , PLASMA)
	, protonP                   ("protonplasma"                                             , PLASMA)
	, antiprotonP               ("antiprotonplasma"                                         , PLASMA)
	, electronP                 ("electronplasma"                                           , PLASMA)
	, positronP                 ("positronplasma"                                           , PLASMA)
	, HydrogenI                 ("ionizedhydrogen", GAS)
	, HeliumI                   ("ionizedhelium", GAS)
	, Iron                      ("iron"                                                  , LIQUID)
	, AntiNitrogen              ("antinitrogen"                                                 , GAS)
	, AntiHydrogen              ("antihydrogen"                                                 , GAS)
	, AntiDeuterium             ("antideuterium"                                                , GAS)
	, AntiTritium               ("antitritium"                                                  , GAS)
	, AntiHelium                ("antihelium"                                                   , GAS)
	, AntiHelium_3              ("antihelium-3"                                                 , GAS)
	, AntiCarbon_13             ("anticarbon-13"                                                , LIQUID)
	, Gasoil                    ("gasoil"                                                   , SIMPLE, LIQUID)
	, Naphtha                   ("naphtha"                                                  , SIMPLE, LIQUID)
	, benzene                   ("benzene"                                                  , SIMPLE, LIQUID)
	, phenol                    ("phenol"                                                   , SIMPLE, LIQUID)
	, acetone                   ("acetone"                                                  , SIMPLE, LIQUID)
	, bisphenol                 ("bisphenol"                                                , SIMPLE, LIQUID)
	, epichlorohydrin           ("epichlorohydrin"                                          , SIMPLE, LIQUID)
	, Isoprene                  ("isoprene"                                                 , SIMPLE, LIQUID)
	, Dichloroisopropanol       ("dichloroisopropanol"                                      , SIMPLE, LIQUID)
	, Cumene                    ("cumene"                                                   , SIMPLE, LIQUID)
	, Silane                    ("silane"                                                   , SIMPLE, LIQUID)
	, Allyl_Chloride            ("allylchloride"                                            , SIMPLE, LIQUID)
	, Acrylic_Acid              ("acrylicacid"                                              , SIMPLE, LIQUID)
	, Ethyl_Acrylate            ("ethylacrylate"                                            , SIMPLE, LIQUID)
	, Styrene                   ("styrene"                                                  , SIMPLE, LIQUID)
	, Vinyl_Chloride            ("vinylchloride"                                            , SIMPLE, LIQUID)
	, Ethyne                    ("ethyne"                                                   , SIMPLE, LIQUID)
	, Photoresist               ("photoresist"                                              , SIMPLE, LIQUID)
	, Monochlorodifluoromethane ("monochlorodifluoromethane"                                , SIMPLE, GAS)
	, Trichloromethane          ("trichloromethane"                                         , SIMPLE, GAS)
	, Tetrafluoroethylene       ("tetrafluoroethylene"                                      , SIMPLE, GAS)
	, SolutionRichPtPa          ("solutionrichptpa"                                         , SIMPLE, LIQUID)
	, SolutionRichPa            ("solutionrichpa"                                           , SIMPLE, LIQUID)
	, MoltenSodiumBisulfate     ("moltensodiumbisulfate"                                    , SIMPLE, LIQUID)
	, RhodiumSulfideSolution    ("rhodiumsulfidesolution"                                   , SIMPLE, LIQUID)
	, PregnantSolutionOsRu      ("pregnantsolutionosru"                                     , SIMPLE, LIQUID)
	, AbsorptionLiquidOsRu      ("absorptionliquidosru"                                     , SIMPLE, LIQUID)
	, AbsorptionLiquidRu        ("absorptionliquidru"                                       , SIMPLE, LIQUID)
	, RutheniumTetroxide        ("rutheniumtetroxide"                                       , SIMPLE, GAS)
	, OsmiumTetroxide           ("osmiumtetroxide"                                          , SIMPLE, GAS)
	, ChloroRhodiumAcid         ("chlororhodiumacid"                                        , SIMPLE, LIQUID)
	, EvaporatedliquidOs        ("evaporatedliquidos"                                       , SIMPLE, LIQUID)
	, SodiumOsmiumHexachloride  ("sodiumosmiumhexachloride"                                 , SIMPLE, LIQUID)
	, MSCBasalMedium            ("mscbasalmedium"                                           , SIMPLE, LIQUID)
	, MSCSupplement             ("mscsupplement"                                            , SIMPLE, LIQUID)
	, MSC                       ("msc"                                                      , SIMPLE, LIQUID)
	, BoneMarrow                ("bonemarrow"                                               , SIMPLE, LIQUID)
	, SPSS                      ("spss"                                                     , SIMPLE, LIQUID)
	, AmylumSolution            ("amylumsolution"                                           , SIMPLE, LIQUID)
	, VitaminSolution           ("vitaminsolution"                                          , SIMPLE, LIQUID)
	, HormoneSolution           ("hormonesolution"                                          , SIMPLE, LIQUID)
	, DifferentiationPromotingProteinNeuron("differentiationpromotingproteinneuron"         , SIMPLE, LIQUID)
	, Methanol                  ("methanol"                                                 , SIMPLE, LIQUID)
	, CoconutOil                ("coconutoil"                                               , SIMPLE, LIQUID)
	, MethylLaurate             ("methyllaurate"                                            , SIMPLE, LIQUID)
	, MethylHydrolaurate        ("methylhydrolaurate"                                       , SIMPLE, LIQUID)
	, Tris_HCl                  ("tris_hcl"                                                 , SIMPLE, LIQUID)
	, AceticAcid                ("aceticacid"                                               , SIMPLE, LIQUID)
	, EDTA                      ("edta"                                                     , SIMPLE, LIQUID)
	, AnimalTissueHomogenate    ("animaltissuehomogenate"                                   , SIMPLE, LIQUID)
	, DNAExtract                ("dnaextract"                                               , SIMPLE, LIQUID)
	, DNASolution               ("dnasolution"                                              , SIMPLE, LIQUID)
	, Bacteria_DifferentiationPromotingProteinNeuron("bacteria_differentiationpromotingproteinneuron", SIMPLE, LIQUID)
	, Protein                   ("protein"                                                  , SIMPLE, LIQUID)
	, Dodecane                  ("dodecane"                                                 , SIMPLE, LIQUID)
	, Dodecane_Chloride         ("dodecanechloride"                                         , SIMPLE, LIQUID)
	, Monochlorodecane          ("monochlorodecane"                                         , SIMPLE, LIQUID)
	, Deoxyribonucleotide       ("deoxyribonucleotide"                                      , SIMPLE, LIQUID)
	, Primer                    ("primer"                                                   , SIMPLE, LIQUID)
	, taqDNAPolymerase          ("taqdnapolymerase"                                         , SIMPLE, LIQUID)
	, EscherichiaColi           ("escherichiacoli"                                          , SIMPLE, LIQUID)
	, RestrictionEndonuclease   ("restrictionendonuclease"                                  , SIMPLE, LIQUID)
	, DifferentiationPromotingProtein_NerveCell_Gene("differentiationpromotingprotein_nervecell_gene", SIMPLE, LIQUID)
	, LaurylAlcohol             ("laurylalcohol"                                            , SIMPLE, LIQUID)
	, Biowaste                  ("biowaste"                                                 , SIMPLE, LIQUID)
	, Agar                      ("agar"                                                     , SIMPLE, LIQUID)
	, BiomassFlora              ("biomass_flora"                                            , SIMPLE, LIQUID)
	, DilutedBiomass            ("biomass_diluted"                                          , SIMPLE, LIQUID)
	, BacteriaMedium            ("bacteria_medium"                                          , SIMPLE, LIQUID)
	, Yeast                     ("yeast"                                                    , SIMPLE, LIQUID)
	, Methanogens               ("methanogens"                                              , SIMPLE, LIQUID)
	, Bacillus                  ("bacillus"                                                 , SIMPLE, LIQUID)
	, Clostridia                ("clostridia"                                               , SIMPLE, LIQUID)
	, MixedEster                ("mixedester"                                               , SIMPLE, LIQUID)
	, AquaticWaste              ("aquaticwaste"                                           , SIMPLE, LIQUID)
	, AquaticNutrition          ("aquaticnutrition"                                       , SIMPLE, LIQUID)
	, Polyethylene              ("polyethylene"                                             , SIMPLE, LIQUID)
	, polytetrafluoroethylene   ("polytetrafluoroethylene"                                  , SIMPLE, LIQUID)
	, polyvinylchloride         ("polyvinylchloride"                                        , SIMPLE, LIQUID)
	, epoxid                    ("epoxid"                                                   , SIMPLE, LIQUID)
	, SFuel                     ("sfuel"                                                    , SIMPLE, LIQUID)
	, SDiesel                   ("sdiesel"                                                  , SIMPLE, LIQUID)
	, SKerosine                 ("skerosine"                                                , SIMPLE, LIQUID)
	, SPetrol                   ("spetrol"                                                  , SIMPLE, LIQUID)
	, SNaphtha                  ("snaphtha"                                                 , SIMPLE, LIQUID)
	, SGasoil                   ("sgasoil"                                                  , SIMPLE, LIQUID)
	;

	public final String mName;

	@SafeVarargs
	FL_GT6U(String aName, Collection<String>... aFluidSets) {
		mName = aName;
		IFluidName.addTo(mName, aFluidSets);
	}

	@Override public String fluidName() {return mName;}
}

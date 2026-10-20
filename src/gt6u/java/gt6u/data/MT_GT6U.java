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
 * the GregTech6-Unofficial additions of gregapi/data/MT.java moved into an addon class.
 */

package gt6u.data;

import gregapi.data.AM;
import gregapi.data.MD;
import gregapi.data.MT;
import gregapi.data.TC;
import gregapi.oredict.OreDictItemData;
import gregapi.oredict.OreDictMaterial;
import gregapi.util.OM;

import static gregapi.data.CS.*;
import static gregapi.data.TD.Atomic.*;
import static gregapi.data.TD.Compounds.*;
import static gregapi.data.TD.ItemGenerator.*;
import static gregapi.data.TD.Processing.*;
import static gregapi.data.TD.Properties.*;
import static gregapi.render.TextureSet.*;

/** GregTech6-Unofficial's materials (GT6U MT.java, class MT.GT6U, ids 15000-16000) made with MT's own helpers, and its edits of GT6's materials. */
public class MT_GT6U extends MT {
	public static final OreDictMaterial
		Nb2Ti3C4                = alloymachine  (15000, "Titanium Niobium Carbide"                      , 153, 153, 204     ).put(MOLTEN).qual(3, 16.0, 7680, 4)                                                                                                            .uumMcfg( 0, NiobiumTitanium               , 6*U, C                , 4*U)                                                                          .aspects(TC.METALLUM, 2, TC.GELUM, 2).heat(4196).alloyCentrifuge(),
	
		H3PO4                   = lqudaciddcmp  (15200, "Phosphoric Acid"                               , 255, 255,  61,     255).put(LIQUID)                                                                                                                                   .uumMcfg( 0, H              , 3*U, P                , 1*U, O                , 4*U)                                                                          .heat( 200,  400), PhosphoricAcid = H3PO4,
		Naphtha                 = lquddcmp      (15201, "Naphtha"                                       , 255, 255, 102, 255).put(LIQUID, EXPLOSIVE, FLAMMABLE)                                                                                                                                                                                                                                                                         .heat( 100,  400).aspects(TC.MORTUUS, 1, TC.POTENTIA, 1),
		H_Ion                   = lquddcmp      (15202, "Ionized Hydrogen"                              , 255, 255, 102, 255).put(PLASMA).heat(1, 12),
		He_Ion                  = lquddcmp      (15203, "Ionized Helium"                                , 255, 255, 102, 255).put(PLASMA).heat(2, 8),
		ElectroEtchingSolution  = lqudacidcent  (15204, "Electro-Etching Solution"                      , 193, 255, 193, 255).put(LIQUID)                                                                                                                                   .uumMcfg( 0, H2O           , 3*U, HCl              , 1*U, H2O2              , 2*U)                                                                                                  .heat( 200,  400),
		H2S2O8                  = lqudaciddcmp  (15205, "Peroxydisulfuric Acid"                         , 255, 250, 205, 255).put(LIQUID)                                                                                                                                   .uumMcfg( 0, H              , 2*U, S                , 2*U, O                , 8*U)                                                                          .heat( 200,  400),
		PF5                     = gaschemelec   (15206, "Phosphorus Pentafluoride"                      , 241, 220, 207, 100).put(GASSES)                                                                                                                                    .uumMcfg( 0, P              , 1*U, F                , 5*U)                                                                                                  ,
		COCl2                   = gaschemelec   (15207, "Phosgen"                                       , 241, 220, 207, 100).put(GASSES).uumMcfg( 0, C , 1*U, O  , 1*U,  Cl,  2*U),
		C5H10O3                 = lqudchemelec  (15208, "Diethyl Carbonate"                             , 241, 220, 207, 100).put(LIQUID).uumMcfg( 0, C , 5*U, H  , 10*U,  O,  3*U),
		C4H8O3                  = lqudchemelec  (15209, "Ethyl Methyl Carbonate"                        , 241, 200, 207, 100).put(LIQUID).uumMcfg( 0, C , 4*U, H  , 8*U,   O,  3*U),
		C3H6O3                  = lqudchemelec  (15210, "Dimethyl Carbonate"                            , 241, 180, 207, 100).put(LIQUID).uumMcfg( 0, C , 3*U, H  , 6*U,   O,  3*U),
	
	
		PTFE                    = create        (15400, "Polytetrafluoroethylene", SET_DULL             , 250, 250, 250, 255).put(G_INGOT_MACHINE, APPROXIMATE, FLAMMABLE, EXTRUDER, EXTRUDER_SIMPLE, WIRES, MORTAR, BOUNCY, BRITTLE, FURNACE)                      .uumMcfg( 0, C              , 2*U, F                , 4*U)                                                                                                  .aspects(TC.MOTUS, 2).heat(423).setBurning(Ash, U9).setSmelting(null, 2*U3).qual(1, 3.0, 32, 1),
		PE                      = create        (15401, "Polyethylene"          , SET_DULL              , 200, 200, 200, 255).put(G_INGOT_MACHINE, APPROXIMATE, FLAMMABLE, EXTRUDER, EXTRUDER_SIMPLE, WIRES, MORTAR, BOUNCY, BRITTLE, FURNACE)                      .uumMcfg( 0, C              , 1*U, H                , 2*U)                                                                                                  .aspects(TC.MOTUS, 2).heat(423).setBurning(Ash, U9).setSmelting(null, 2*U3).qual(1, 3.0, 32, 1),
		PVC                     = create        (15402, "Polyvinylchloride"     , SET_DULL              , 144, 238, 144, 255).put(G_INGOT_MACHINE, APPROXIMATE, FLAMMABLE, EXTRUDER, EXTRUDER_SIMPLE, WIRES, MORTAR, BOUNCY, BRITTLE, FURNACE)                      .uumMcfg( 0, C              , 2*U, H                , 3*U, Cl, 1*U)                                                                                                  .aspects(TC.MOTUS, 2).heat(600).setBurning(Ash, U9).setSmelting(null, 2*U3).qual(1, 3.0, 32, 1),
		Epoxid                  = create        (15403, "Epoxid"                , SET_DULL              , 192, 255,  62, 255).put(G_INGOT_MACHINE, APPROXIMATE, FLAMMABLE, EXTRUDER, EXTRUDER_SIMPLE, WIRES, MORTAR, BOUNCY, BRITTLE, FURNACE)                      .uumMcfg( 0, C              , 11*U, H                , 12*U, O, 3*U)                                                                                                  .aspects(TC.MOTUS, 2).heat(423).setBurning(Ash, U9).setSmelting(null, 2*U3).qual(1, 3.0, 32, 1),
	
	
		Na2CrO4                 = dustdcmp      (15600, "Sodium Chromate"       , SET_FINE              , 255, 255,   0, 255).put(MELTING).setMcfg( 0, Cr, 1*U, O, 4*U, Na, 2*U),
		Na2Cr2O7                = dustdcmp      (15601, "Sodium Dichromate"     , SET_FINE              , 255,  69,   0, 255).put(MELTING).setMcfg( 0, Cr, 2*U, O, 7*U, Na, 2*U),
		Cr2O3                   = dustdcmp      (15602, "Chromium Oxide"        , SET_FINE              ,   0, 139,   0, 255).put(MELTING).setMcfg( 0, Cr, 2*U, O, 3*U),
		AlCl3                   = dustdcmp      (15603, "Aluminium Chloride"    , SET_FINE              , 255, 250, 205, 255).put(MELTING).setMcfg( 0, Al              , 1*U, Cl                , 3*U),
		P2O5                    = dustdcmp      (15604, "Phosphorus Pentoxide"  , SET_DULL              , 255, 250, 205, 255)             .setMcfg( 0, P               , 2*U, O                 , 5*U),
		LiF                     = dustdcmp      (15605, "Lithium Fluoride"      , SET_CUBE              , 204, 218, 218, 255).put(MELTING).setMcfg( 0, Li              , 1*U, F                 , 1*U),
		LiPF6                   = dustdcmp      (15606, "Lithium Hexafluorophosphate"       , SET_FINE  , 229, 209, 226, 255)             .setMcfg( 0, Li, 1*U, P, 1*U, F, 6*U),
		CaO                     = dustdcmp      (15607, "Calcium Oxide"         , SET_FINE              , 255, 255, 255, 255).put(MELTING).setMcfg( 0, Ca              , 1*U, O                 , 1*U),
	
	// GT6U gives these GT6 ids (MT.java:370, :901); in the addon they take ids of GT6U's own range, GT6's stay free for GT6.
	HiggsBoson              = create(15800, "Higgs-Boson").setStatsElement(0,0,0,0,0).heat(0,0,0).setRGBa(255, 255, 255, 255).setTextures(SET_NONE).put(PARTICLE).tooltip("HB").hide(),
	Coconut                 = dustfood      (15801, "Coconut"                                       , 240, 150, 140, 255).put(TICKS_PER_SMELT/ 2)                                                                                                                                                                                                                                                                                   .aspects(TC.FAMES, 1).setOriginalMod(MD.BINNIE);
	
	/** Optical fibre cables by tier, as GT6U keeps its circuit lists (GT6U MT.java:3024-3048, class DATA). */
	public static final OreDictItemData[]
	FIBERCABLES_04 = {OP_GT6U.cableLPGt04.dat(MT.Superconductor), OP_GT6U.cableLPGt04.dat(MT.Superconductor), OP_GT6U.cableLPGt04.dat(MT.Superconductor), OP_GT6U.cableLPGt04.dat(MT.Superconductor), OP_GT6U.cableLPGt04.dat(MT.Superconductor), OP_GT6U.cableLPGt04.dat(MT.Superconductor), OP_GT6U.cableLPGt04.dat(MT.Superconductor), OP_GT6U.cableLPGt04.dat(MT.Superconductor), OP_GT6U.cableLPGt04.dat(MT.Superconductor), OP_GT6U.cableLPGt04.dat(MT.Superconductor)},
	FIBERCABLES_01 = {OP_GT6U.cableLPGt01.dat(MT.Superconductor), OP_GT6U.cableLPGt01.dat(MT.Superconductor), OP_GT6U.cableLPGt01.dat(MT.Superconductor), OP_GT6U.cableLPGt01.dat(MT.Superconductor), OP_GT6U.cableLPGt01.dat(MT.Superconductor), OP_GT6U.cableLPGt01.dat(MT.Superconductor), OP_GT6U.cableLPGt01.dat(MT.Superconductor), OP_GT6U.cableLPGt01.dat(MT.Superconductor), OP_GT6U.cableLPGt01.dat(MT.Superconductor), OP_GT6U.cableLPGt01.dat(MT.Superconductor)};
	
	/** GT6U's definitions of GT6 materials, asked by MT while it defines them (GT6U_Definitions.material). Its own subclass of MT:
	 *  MT is still being initialised then, and reaching MT's helpers through MT_GT6U would initialise MT_GT6U, which reads MT's
	 *  materials before they exist. */
	public static final class InMT extends MT {
		private InMT() {}
		
		/** GT6U MT.java:904 makes Sand with create, not dust (no dust prefixes, no mortar); GT6 6.17's later aspects and stats stay. */
		public static OreDictMaterial sand() {return create(8100, "Sand", SET_SAND, 250, 250, 200, 255, FURNACE, UNRECYCLABLE).aspects(TC.TERRA, 1).stealStatsElement(SiO2).setSmelting(Glass, U);}
		/** GT6U MT.java:907 makes Sluice Sand an ore dust; GT6 6.17's later aspects and stats stay. */
		public static OreDictMaterial sluiceSand() {return oredust(8102, "Sluice Sand", SET_SAND, 165, 165, 120, 255).aspects(TC.TERRA, 1, TC.LUCRUM, 1).stealStatsElement(SiO2);}
		/** GT6U MT.java:539 makes Neutronium an element with GT6's id 3800; in the addon it takes an id of GT6U's own range. */
		/** GT6U MT.java:524, :526 give Elemental Tritanium and Duranium lower melting and boiling points, set at creation so that the
		 *  alloys and compounds made of them, which MT defines afterwards, take them over. */
		public static OreDictMaterial tritanium() {return element( 1250, "TritaniumElemental"    , "Tn"      , 125, 198, 1500, 2500, 25.0     , SET_DULL     ,  55, 155, 155, 255, G_DUST_ORES, UUM, METAL, SMITHABLE, MELTING, MOLTEN, EXTRUDER, "Unbipentium").aspects(TC.METALLUM, 2, TC.TUTAMEN, 2, TC.VITREUS, 1).setLocal("Elemental Tritanium").put(FUSION);}
		public static OreDictMaterial duranium() {return element( 1450, "DuraniumElemental"     , "Dn"      , 145, 190, 1200, 2100, 20.0, SET_DULL, 75, 175, 175, 255, G_DUST_ORES, UUM, METAL, SMITHABLE, MELTING, MOLTEN, EXTRUDER, "Unquadpentium").aspects(TC.METALLUM, 2, TC.TUTAMEN, 1).setLocal("Elemental Duranium").put(FUSION);}
		public static OreDictMaterial neutronium() {return element(15802, "Neutronium", "Nt", 0, 1, 3000, 100000, 100000, SET_SHINY, 255, 255, 255, 255).qual(3, 1000000000.0, 1000000000, 15).put(FUSION, MOLTEN, VALUABLE, UNBURNABLE, MAGICAL, WITHER_PROOF, ENDER_DRAGON_PROOF, GLOWING, LIGHTING, MAGNETIC_ACTIVE).aspects(TC.POTENTIA, 10, TC.NEBRISUM, 10, TC.PERMUTATIO, 10, TC.MAGNETO, 10, TC.PRAECANTIO, 10, TC.AURAM, 10);}
	}
	
	/** GT6U's edits of GT6's own materials (GT6U MT.java), made before GT6 generates items, fluids and recipes from them. */
	public static void changeGT6() {
		for (OreDictMaterial tMat : new OreDictMaterial[] {MT.y, MT.v, MT.n, MT.p, MT.e}) tMat.setTextures(SET_NONE);
		MT.p.put(FUSION);
		for (OreDictMaterial tMat : new OreDictMaterial[] {MT.Li_6, MT.Be_7, MT.Y, MT.Ru, MT.Rh, MT.Hf, MT.Ta, MT.U_238, MT.U_235, MT.Pu, MT.Pu_241, MT.Pu_243, MT.Pu_239, MT.Am, MT.Am_241}) tMat.put(MOLTEN);
		for (OreDictMaterial tMat : new OreDictMaterial[] {MT.W, MT.Vb, MT.Ad}) tMat.remove(UNBURNABLE);
		for (OreDictMaterial tMat : new OreDictMaterial[] {MT.Tn, MT.Ke, MT.Dn, MT.Vb, MT.Nq, MT.Nq_528, MT.Nq_522, MT.An, MT.Cor, MT.Dr, MT.Etx, MT.Atl, MT.Ad, MT.Mcg, MT.Gt}) tMat.put(FUSION);
		for (OreDictMaterial tMat : new OreDictMaterial[] {AM.H, AM.D, AM.T}) tMat.put(FUSION); // GT6U AM.java
		MT.Al2O3.setRGBa(248, 248, 255, 255);
		MT.OREMATS.Chromite.mByProducts.clear();
		MT.OREMATS.Chromite.ores(MT.MnO2, MT.Cr, MT.Mg, MT.OREMATS.Bromargyrite);
		// GT6U dropped GT6's setSmelting(Cr, 2*U9): present in v6.14.17 MT.java:3316 and 6.17.08 MT.java:3750, absent in
		// GT6U MT.java:3111. That leaves the default 1:1 self target and drops the PROCESSING.MELTING the call put; the
		// Blacklisted Smelter marker, carried by both bases, is what keeps Chromite out of the Smelter, not an empty target.
		// The state is restored directly, not re-set: setSmelting(null, U) would add a second reverse link to Chromite's own
		// mTargetedSmelting (the one its construction made is out of reach for OreDictMaterial.java:812-814, mHashID being
		// assigned after the sets, OreDictMaterial.java:218,338), while GT6U carries exactly the construction's one link.
		MT.OREMATS.Chromite.mTargetSmelting = OM.stack(MT.OREMATS.Chromite, U);
		MT.Cr.mTargetedSmelting.remove(MT.OREMATS.Chromite);
		MT.OREMATS.Chromite.remove(MELTING);
		MT.Nb.ores(MT.OREMATS.Tantalite, MT.TiO2);
		// GT6U lists Phosphorus' by-products a second time (GT6U MT.java:2442, :2446).
		MT.Phosphorus.ores(MT.Phosphorite, MT.Apatite, MT.FluoriteYellow, MT.PO4);
	}
}

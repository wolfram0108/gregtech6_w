/**
 * Copyright (c) 2026 GregTech-6 Team
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

import gregapi.api.FMLPostInitializationEvent;
import gregapi.api.Abstract_Mod;
import gregapi.code.ModData;
import gregapi.compat.CompatMods;
import gregapi.data.*;
import gregapi.oredict.OreDictMaterial;
import gregapi.oredict.event.IOreDictListenerEvent;
import gregapi.oredict.event.OreDictListenerEvent_Names;
import gregapi.util.CR;
import gregapi.util.OM;
import gregapi.util.ST;
import net.minecraft.world.level.block.Blocks;

import static gregapi.data.CS.*;

public class Compat_Recipes_AppliedEnergistics extends CompatMods {
	public Compat_Recipes_AppliedEnergistics(ModData aMod, Abstract_Mod aGTMod) {super(aMod, aGTMod);}
	
	@Override public void onPostLoad(FMLPostInitializationEvent aInitEvent) {OUT.println("GT_Mod: Doing AE Recipes.");
		// Three grinder-recipe calls removed: AE2 on 1.20.1 has no quartz grinder machine.

		RM.DidYouKnow.addFakeRecipe(F, ST.array(IL.AE_Cutter_Certus.wild(1), OP.ingot.mat(MT.Fe, 1)), ST.array(ST.make(MD.AE, "item.ItemMultiMaterial", 0, 21)), null, ZL_LONG, ZL_FS, ZL_FS, 0, 0, 0);
		RM.DidYouKnow.addFakeRecipe(F, ST.array(IL.AE_Cutter_Quartz.wild(1), OP.ingot.mat(MT.Fe, 1)), ST.array(ST.make(MD.AE, "item.ItemMultiMaterial", 0, 21)), null, ZL_LONG, ZL_FS, ZL_FS, 0, 0, 0);
		
		CR.shaped(ST.make(MD.AE, "tile.BlockQuartzGlass", 4, 0), CR.DEF_REM_REV_NCC, "QGQ", "GQG", "QGQ", 'G', OD.blockGlassColorless, 'Q', OP.dust.dat(ANY.Quartz));
		CR.shaped(ST.make(MD.AE, "tile.BlockQuartzLamp" , 1, 0), CR.DEF_REM_REV_NCC, "GQG", 'G', OP.dust.dat(ANY.Glowstone), 'Q', ST.make(MD.AE, "tile.BlockQuartzGlass", 1, 0));
		
		RM.Press        .addRecipe2(T, 16,   64, ST.make(MD.AE, "item.ItemMultiMaterial", 0, 13), ST.make(MD.AE, "item.ItemMultiMaterial", 1, 10), ST.make(MD.AE, "item.ItemMultiMaterial", 1, 16));
		RM.Press        .addRecipe2(T, 16,   64, ST.make(MD.AE, "item.ItemMultiMaterial", 0, 13), OP.plateGem.mat(MT.CertusQuartz            , 1), ST.make(MD.AE, "item.ItemMultiMaterial", 1, 16));
		for (OreDictMaterial tMat : ANY.Diamond.mToThis)
		RM.Press        .addRecipe2(T, 16,   64, ST.make(MD.AE, "item.ItemMultiMaterial", 0, 14), OP.plateGem.mat(tMat                       , 1), ST.make(MD.AE, "item.ItemMultiMaterial", 1, 17));
		RM.Press        .addRecipe2(T, 16,   64, ST.make(MD.AE, "item.ItemMultiMaterial", 0, 15), OP.plate   .mat(MT.Au                      , 1), ST.make(MD.AE, "item.ItemMultiMaterial", 1, 18));
		RM.Press        .addRecipe2(T, 16,   64, ST.make(MD.AE, "item.ItemMultiMaterial", 0, 19), OP.plate   .mat(MT.Si                      , 1), ST.make(MD.AE, "item.ItemMultiMaterial", 1, 20));
		RM.Press        .addRecipe2(T, 16,   64, ST.make(MD.AE, "item.ItemMultiMaterial", 0, 19), OP.plateGem.mat(MT.Si                      , 1), ST.make(MD.AE, "item.ItemMultiMaterial", 1, 20));
		
		for (OreDictMaterial tMat : ANY.Iron.mToThis) if (tMat != MT.Enori) {
		RM.Press        .addRecipe2(T, 16,   64, ST.make(MD.AE, "item.ItemMultiMaterial", 0, 13), OP.blockSolid.mat(tMat, 1), ST.make(MD.AE, "item.ItemMultiMaterial", 1, 13));
		RM.Press        .addRecipe2(T, 16,   64, ST.make(MD.AE, "item.ItemMultiMaterial", 0, 14), OP.blockSolid.mat(tMat, 1), ST.make(MD.AE, "item.ItemMultiMaterial", 1, 14));
		RM.Press        .addRecipe2(T, 16,   64, ST.make(MD.AE, "item.ItemMultiMaterial", 0, 15), OP.blockSolid.mat(tMat, 1), ST.make(MD.AE, "item.ItemMultiMaterial", 1, 15));
		RM.Press        .addRecipe2(T, 16,   64, ST.make(MD.AE, "item.ItemMultiMaterial", 0, 19), OP.blockSolid.mat(tMat, 1), ST.make(MD.AE, "item.ItemMultiMaterial", 1, 19));
		
		RM.sawing(16, 16, F, 10, OP.ingot.mat(tMat     , 1), ST.make(MD.AE, "item.ItemMultiPart", 3, 120));
		}
		for (OreDictMaterial tMat : ANY.Cu.mToThis)
		RM.sawing(16, 16, F, 10, OP.ingot.mat(tMat     , 1), ST.make(MD.AE, "item.ItemMultiPart", 3, 120));
		RM.sawing(16, 16, F, 10, OP.ingot.mat(MT.Sn    , 1), ST.make(MD.AE, "item.ItemMultiPart", 3, 120));
		RM.sawing(16, 16, F, 10, OP.ingot.mat(MT.Pb    , 1), ST.make(MD.AE, "item.ItemMultiPart", 3, 120));
		RM.sawing(16, 16, F, 10, OP.ingot.mat(MT.Ag    , 1), ST.make(MD.AE, "item.ItemMultiPart", 3, 120));
		RM.sawing(16, 16, F, 10, OP.ingot.mat(MT.Ni    , 1), ST.make(MD.AE, "item.ItemMultiPart", 3, 120));
		RM.sawing(16, 16, F, 10, OP.ingot.mat(MT.Al    , 1), ST.make(MD.AE, "item.ItemMultiPart", 3, 120));
		RM.sawing(16, 16, F, 10, OP.ingot.mat(MT.Brass , 1), ST.make(MD.AE, "item.ItemMultiPart", 3, 120));
		RM.sawing(16, 16, F, 10, OP.ingot.mat(MT.Bronze, 1), ST.make(MD.AE, "item.ItemMultiPart", 3, 120));
		RM.sawing(16, 16, F, 10, OP.ingot.mat(MT.Invar , 1), ST.make(MD.AE, "item.ItemMultiPart", 3, 120));
		
		RM.Press        .addRecipeX(T, 16,   64, ST.array(ST.make(MD.AE, "item.ItemMultiMaterial", 1, 16), OM.dust(MT.Redstone), ST.make(MD.AE, "item.ItemMultiMaterial", 1, 20)), ST.make(MD.AE, "item.ItemMultiMaterial", 1, 23));
		RM.Press        .addRecipeX(T, 16,   64, ST.array(ST.make(MD.AE, "item.ItemMultiMaterial", 1, 17), OM.dust(MT.Redstone), ST.make(MD.AE, "item.ItemMultiMaterial", 1, 20)), ST.make(MD.AE, "item.ItemMultiMaterial", 1, 24));
		RM.Press        .addRecipeX(T, 16,   64, ST.array(ST.make(MD.AE, "item.ItemMultiMaterial", 1, 18), OM.dust(MT.Redstone), ST.make(MD.AE, "item.ItemMultiMaterial", 1, 20)), ST.make(MD.AE, "item.ItemMultiMaterial", 1, 22));
		
		RM.Autoclave    .addRecipe2(T,  0, 1500, ST.make(MD.AE, "item.ItemCrystalSeed", 1,    0), ST.tag(2), FL.Steam.make(48000), FL.DistW.make(225), ST.make(MD.AE, "item.ItemMultiMaterial", 1, 10));
		RM.Autoclave    .addRecipe2(T,  0, 1500, ST.make(MD.AE, "item.ItemCrystalSeed", 1,  600), ST.tag(2), FL.Steam.make(48000), FL.DistW.make(225), ST.make(MD.AE, "item.ItemMultiMaterial", 1, 11));
		RM.Autoclave    .addRecipe2(T,  0, 1500, ST.make(MD.AE, "item.ItemCrystalSeed", 1, 1200), ST.tag(2), FL.Steam.make(48000), FL.DistW.make(225), ST.make(MD.AE, "item.ItemMultiMaterial", 1, 12));
		
		RM.Compressor   .addRecipe1(T, 16,   16, OP.gem.mat(MT.CertusQuartz                 , 4), ST.make(MD.AE, "tile.BlockQuartz", 1, 0));
		RM.Compressor   .addRecipe1(T, 16,   16, ST.make(MD.AE, "item.ItemMultiMaterial", 8, 10), ST.make(MD.AE, "tile.BlockQuartz", 1, 0));
		RM.Compressor   .addRecipe1(T, 16,   16, OP.gem.mat(MT.Fluix                        , 4), ST.make(MD.AE, "tile.BlockFluix", 1, 0));
		RM.Compressor   .addRecipe1(T, 16,   16, ST.make(MD.AE, "item.ItemMultiMaterial", 8, 12), ST.make(MD.AE, "tile.BlockFluix", 1, 0));
		RM.Compressor   .addRecipe1(T, 16,   16, ST.make(MD.AE, "item.ItemMultiMaterial", 8, 11), ST.make(Blocks.QUARTZ_BLOCK, 1, 0));
		
		RM.Mixer        .addRecipe2(T, 16,   16, OM.dust(MT.CertusQuartz), ST.make(Blocks.SAND, 1, W), ST.make(MD.AE, "item.ItemCrystalSeed", 2,    0));
		RM.Mixer        .addRecipe2(T, 16,   16, OM.dust(MT.NetherQuartz), ST.make(Blocks.SAND, 1, W), ST.make(MD.AE, "item.ItemCrystalSeed", 2,  600));
		RM.Mixer        .addRecipe2(T, 16,   16, OM.dust(MT.Fluix       ), ST.make(Blocks.SAND, 1, W), ST.make(MD.AE, "item.ItemCrystalSeed", 2, 1200));
		if (IL.AETHER_Sand.exists()) {
		RM.Mixer        .addRecipe2(T, 16,   16, OM.dust(MT.CertusQuartz), IL.AETHER_Sand     .get(1), ST.make(MD.AE, "item.ItemCrystalSeed", 2,    0));
		RM.Mixer        .addRecipe2(T, 16,   16, OM.dust(MT.NetherQuartz), IL.AETHER_Sand     .get(1), ST.make(MD.AE, "item.ItemCrystalSeed", 2,  600));
		RM.Mixer        .addRecipe2(T, 16,   16, OM.dust(MT.Fluix       ), IL.AETHER_Sand     .get(1), ST.make(MD.AE, "item.ItemCrystalSeed", 2, 1200));
		}
		
		RM.smash(ST.make(MD.AE, "tile.BlockQuartz"              , 1, W), OP.gem.mat(MT.CertusQuartz, 4));
		RM.smash(ST.make(MD.AE, "tile.BlockQuartzPillar"        , 1, W), OP.gem.mat(MT.CertusQuartz, 4));
		RM.smash(ST.make(MD.AE, "tile.BlockQuartzChiseled"      , 1, W), OP.gem.mat(MT.CertusQuartz, 4));
		RM.smash(ST.make(MD.AE, "tile.BlockFluix"               , 1, W), OP.gem.mat(MT.Fluix, 4));
		RM.smash(ST.make(MD.AE, "tile.QuartzStairBlock"         , 1, W), OP.gem.mat(MT.CertusQuartz, 6));
		RM.smash(ST.make(MD.AE, "tile.QuartzPillarStairBlock"   , 1, W), OP.gem.mat(MT.CertusQuartz, 6));
		RM.smash(ST.make(MD.AE, "tile.ChiseledQuartzStairBlock" , 1, W), OP.gem.mat(MT.CertusQuartz, 6));
		RM.smash(ST.make(MD.AE, "tile.FluixStairBlock"          , 1, W), OP.gem.mat(MT.Fluix, 6));
		RM.smash(ST.make(MD.AE, "tile.QuartzSlabBlock"          , 1, W), OP.gem.mat(MT.CertusQuartz, 2));
		RM.smash(ST.make(MD.AE, "tile.QuartzPillarSlabBlock"    , 1, W), OP.gem.mat(MT.CertusQuartz, 2));
		RM.smash(ST.make(MD.AE, "tile.ChiseledQuartzSlabBlock"  , 1, W), OP.gem.mat(MT.CertusQuartz, 2));
		RM.smash(ST.make(MD.AE, "tile.FluixSlabBlock"           , 1, W), OP.gem.mat(MT.Fluix, 2));
		
		
		RM.mortarize( 18, ST.make(MD.AE, "tile.BlockSkyStone", 1, W), OP.blockDust.mat(MT.STONES.SkyStone, 1));
		RM.mortarize(144, ST.make(MD.AE, "tile.BlockSkyChest", 1, W), OP.blockDust.mat(MT.STONES.SkyStone, 8));
		
		RM.stonetypes(MT.STONES.SkyStone, T, OP.rockGt.mat(MT.STONES.SkyStone, 4), OP.blockDust.mat(MT.STONES.SkyStone, 1)
		, RM.stoneshapes(MT.STONES.SkyStone, F, ST.make(MD.AE, "tile.BlockSkyStone", 1, 0), ST.make(MD.AE, "tile.SkyStoneStairBlock"          , 1, 0), ST.make(MD.AE, "tile.SkyStoneSlabBlock"          , 1, 0), NI, NI)
		, NI
		, RM.stoneshapes(MT.STONES.SkyStone, F, ST.make(MD.AE, "tile.BlockSkyStone", 1, 2), ST.make(MD.AE, "tile.SkyStoneBrickStairBlock"     , 1, 0), ST.make(MD.AE, "tile.SkyStoneBrickSlabBlock"     , 1, 0), NI, NI)
		, NI
		, NI
		, RM.stoneshapes(MT.STONES.SkyStone, F, ST.make(MD.AE, "tile.BlockSkyStone", 1, 1), ST.make(MD.AE, "tile.SkyStoneBlockStairBlock"     , 1, 0), ST.make(MD.AE, "tile.SkyStoneBlockSlabBlock"     , 1, 0), NI, NI)
		, NI
		, RM.stoneshapes(MT.STONES.SkyStone, F, ST.make(MD.AE, "tile.BlockSkyStone", 1, 3), ST.make(MD.AE, "tile.SkyStoneSmallBrickStairBlock", 1, 0), ST.make(MD.AE, "tile.SkyStoneSmallBrickSlabBlock", 1, 0), NI, NI)
		);
		
		
		new OreDictListenerEvent_Names() {@Override public void addAllListeners() {
		// Twenty listeners whose only job was calling AE2's grinder are removed with it.
		// Four lens listeners stay, since they feed GT6's own laser engraver.
		addListener(DYE_OREDICTS_LENS[DYE_INDEX_White], new IOreDictListenerEvent() {@Override public void onOreRegistration(OreDictRegistrationContainer aEvent) {
			for (OreDictMaterial tMat : ANY.Fe.mToThis) if (tMat != MT.Enori)
			RM.LaserEngraver.addRecipe2(T,512,512, OP.blockSolid.mat(tMat, 1), ST.amount(0, aEvent.mStack), ST.make(MD.AE, "item.ItemMultiMaterial", 1, 13));
		}});
		addListener(DYE_OREDICTS_LENS[DYE_INDEX_Cyan], new IOreDictListenerEvent() {@Override public void onOreRegistration(OreDictRegistrationContainer aEvent) {
			for (OreDictMaterial tMat : ANY.Fe.mToThis) if (tMat != MT.Enori)
			RM.LaserEngraver.addRecipe2(T,512,512, OP.blockSolid.mat(tMat, 1), ST.amount(0, aEvent.mStack), ST.make(MD.AE, "item.ItemMultiMaterial", 1, 14));
		}});
		addListener(DYE_OREDICTS_LENS[DYE_INDEX_Yellow], new IOreDictListenerEvent() {@Override public void onOreRegistration(OreDictRegistrationContainer aEvent) {
			for (OreDictMaterial tMat : ANY.Fe.mToThis) if (tMat != MT.Enori)
			RM.LaserEngraver.addRecipe2(T,512,512, OP.blockSolid.mat(tMat, 1), ST.amount(0, aEvent.mStack), ST.make(MD.AE, "item.ItemMultiMaterial", 1, 15));
		}});
		addListener(DYE_OREDICTS_LENS[DYE_INDEX_Purple], new IOreDictListenerEvent() {@Override public void onOreRegistration(OreDictRegistrationContainer aEvent) {
			for (OreDictMaterial tMat : ANY.Fe.mToThis) if (tMat != MT.Enori)
			RM.LaserEngraver.addRecipe2(T,512,512, OP.blockSolid.mat(tMat, 1), ST.amount(0, aEvent.mStack), ST.make(MD.AE, "item.ItemMultiMaterial", 1, 19));
		}});
		}};

		// ================================================================================================
		// Suppresses AE2's own datapack recipes wherever GT6's machines already do the same matter transformation;
		// movement, storage, and ME-network mechanics are left alone, since those aren't duplicated by anything GT6 has.
		// ================================================================================================
		gregapi.GT_API.deferRecipeScan(() -> {
			java.util.Set<net.minecraft.resources.ResourceLocation> tSuppress = new java.util.HashSet<>();

			// The inscriber node is removed entirely, machine included.
			// Every one of its outputs is reachable by a GT6 machine instead.
			if (ConfigsGT.GREGTECH.get("ae2", "DisableAllInscriberRecipes", T)) {
				suppressAE(tSuppress, "inscriber/certus_quartz_dust");
				suppressAE(tSuppress, "inscriber/fluix_dust");
				suppressAE(tSuppress, "inscriber/ender_dust");
				suppressAE(tSuppress, "inscriber/sky_stone_dust");
				suppressAE(tSuppress, "inscriber/calculation_processor_press");
				suppressAE(tSuppress, "inscriber/engineering_processor_press");
				suppressAE(tSuppress, "inscriber/logic_processor_press");
				suppressAE(tSuppress, "inscriber/silicon_press");
				suppressAE(tSuppress, "inscriber/calculation_processor_print");
				suppressAE(tSuppress, "inscriber/engineering_processor_print");
				suppressAE(tSuppress, "inscriber/logic_processor_print");
				suppressAE(tSuppress, "inscriber/silicon_print");
				suppressAE(tSuppress, "inscriber/calculation_processor");
				suppressAE(tSuppress, "inscriber/engineering_processor");
				suppressAE(tSuppress, "inscriber/logic_processor");
				suppressAE(tSuppress, "network/blocks/inscribers");
			}

			// GT6 already produces charged certus by other means.
			// The charger block itself stays, since it also charges AE2's own powered items.
			if (ConfigsGT.GREGTECH.get("ae2", "DisableAllChargerCrystalRecipes", T)) {
				suppressAE(tSuppress, "charger/charged_certus_quartz_crystal");
			}

			// Crystal growth and synthesis duplicate GT6 production; the budding chain and singularities are left untouched.
			if (ConfigsGT.GREGTECH.get("ae2", "DisableAllTransformCrystalRecipes", T)) {
				suppressAE(tSuppress, "transform/certus_quartz_crystals");
				suppressAE(tSuppress, "transform/fluix_crystal");
				suppressAE(tSuppress, "transform/fluix_crystals");
				suppressAE(tSuppress, "transform/damaged_budding_quartz");
				suppressAE(tSuppress, "transform/chipped_budding_quartz");
				suppressAE(tSuppress, "transform/flawed_budding_quartz");
			}

			// All 14 certus/quartz tools are suppressed as duplicates of GT6's own unified tool item, including both wrench
			// keys, so the network tool's crafting input is repointed to GT6's own wrench via a bundled recipe pack.
			if (AE2_KILL_QUARTZ_TOOLS) {
				suppressAE(tSuppress, "tools/certus_quartz_axe");
				suppressAE(tSuppress, "tools/certus_quartz_hoe");
				suppressAE(tSuppress, "tools/certus_quartz_pickaxe");
				suppressAE(tSuppress, "tools/certus_quartz_spade");
				suppressAE(tSuppress, "tools/certus_quartz_sword");
				suppressAE(tSuppress, "tools/certus_quartz_wrench");
				suppressAE(tSuppress, "tools/certus_quartz_cutting_knife");
				suppressAE(tSuppress, "tools/nether_quartz_axe");
				suppressAE(tSuppress, "tools/nether_quartz_hoe");
				suppressAE(tSuppress, "tools/nether_quartz_pickaxe");
				suppressAE(tSuppress, "tools/nether_quartz_spade");
				suppressAE(tSuppress, "tools/nether_quartz_sword");
				suppressAE(tSuppress, "tools/nether_quartz_wrench");
				suppressAE(tSuppress, "tools/nether_quartz_cutting_knife");
			}

			// The five fluix tools and their sole-purpose smithing template are suppressed for the same reason as the quartz set.
			if (ConfigsGT.GREGTECH.get("ae2", "DisableAllFluixToolRecipes", T)) {
				suppressAE(tSuppress, "tools/fluix_axe");
				suppressAE(tSuppress, "tools/fluix_hoe");
				suppressAE(tSuppress, "tools/fluix_pickaxe");
				suppressAE(tSuppress, "tools/fluix_shovel");
				suppressAE(tSuppress, "tools/fluix_sword");
				suppressAE(tSuppress, "tools/fluix_upgrade_smithing_template");
			}

			// GT6's own storage is far richer, and a plain sky-stone chest isn't ME-network storage.
			// Unlike ME Chest/Drive, it's suppressed.
			if (ConfigsGT.GREGTECH.get("ae2", "DisableAllSkyStoneContainerRecipes", T)) {
				suppressAE(tSuppress, "misc/chests_sky_stone");
				suppressAE(tSuppress, "misc/chests_smooth_sky_stone");
				suppressAE(tSuppress, "misc/tank_sky_stone");
			}

			// GT6 has its own explosives tied to its own mining mechanics, so this node is suppressed.
			if (ConfigsGT.GREGTECH.get("ae2", "DisableAllTinyTNTRecipes", T)) {
				suppressAE(tSuppress, "misc/tiny_tnt");
			}

			// Crafting and all ten operations are suppressed together: heating/cooling is GT6 machine work.
			// A machine with no operations left is clutter.
			if (ConfigsGT.GREGTECH.get("ae2", "DisableAllEntropyManipulatorRecipes", T)) {
				suppressAE(tSuppress, "tools/misctools_entropy_manipulator");
				suppressAE(tSuppress, "entropy/heat/cobblestone_stone");
				suppressAE(tSuppress, "entropy/heat/ice_water");
				suppressAE(tSuppress, "entropy/heat/snow_water");
				suppressAE(tSuppress, "entropy/heat/water_air");
				suppressAE(tSuppress, "entropy/cool/flowing_water_snowball");
				suppressAE(tSuppress, "entropy/cool/grass_block_dirt");
				suppressAE(tSuppress, "entropy/cool/lava_obsidian");
				suppressAE(tSuppress, "entropy/cool/stone_bricks_cracked_stone_bricks");
				suppressAE(tSuppress, "entropy/cool/stone_cobblestone");
				suppressAE(tSuppress, "entropy/cool/water_ice");
			}

			// GT6 already replaces this exact output itself, from an ingot via an earlier-tier tool, and the knife it needs
			// is suppressed by the tool node anyway, so it would be half-orphaned without this.
			if (ConfigsGT.GREGTECH.get("ae2", "DisableAllCableAnchorRecipes", T)) {
				suppressAE(tSuppress, "network/parts/cable_anchor");
			}

			// Not part of the original canon (1.7.10 never had a dust-to-block furnace path).
			// A shortcut here would undercut GT6's own mixer/crucible/mold route.
			if (ConfigsGT.GREGTECH.get("ae2", "DisableAllSkyStoneDustSmeltingRecipes", T)) {
				suppressAE(tSuppress, "blasting/sky_stone_block");
			}

			// Both AE2 power generators duplicate GT6's own generators; the FE bridge lets AE2 draw power from GT6 machines instead.
			// (appeng/blockentity/powersink/AEBasePoweredBlockEntity + ForgeEnergyAdapter).
			if (ConfigsGT.GREGTECH.get("ae2", "DisableAllEnergyGeneratorRecipes", T)) {
				suppressAE(tSuppress, "network/blocks/energy_vibration_chamber");
				suppressAE(tSuppress, "network/crystal_resonance_generator");
			}

			// The charger stopped being irreplaceable once GT6's own chargers could power AE2 items via the energy bridge.
			// Only its crafting recipe is suppressed here; blocks already placed in the world keep working.
			if (ConfigsGT.GREGTECH.get("ae2", "DisableAllChargerAndCrankRecipes", T)) {
				suppressAE(tSuppress, "network/blocks/crystal_processing_charger");
				suppressAE(tSuppress, "network/blocks/crank");
			}

			// Not its own node, just a companion to the meteorite generation switch: no meteorites means no need for a compass.
			if (AE2_REPLACE_METEORITE_GENERATION) {
				suppressAE(tSuppress, "charger/meteorite_compass");
			}

			// GT6's own hammer-based path qualifies, but this defaults to false since 1.7.10 kept these recipes rather than
			// removing them; only the four blocks GT6 has its own breakdown for are ever listed.
			if (ConfigsGT.GREGTECH.get("ae2", "DisableAllCrystalDeconstructionRecipes", F)) {
				suppressAE(tSuppress, "misc/deconstruction_certus_quartz_block");
				suppressAE(tSuppress, "misc/deconstruction_certus_quartz_pillar");
				suppressAE(tSuppress, "misc/deconstruction_chiseled_certus_quartz");
				suppressAE(tSuppress, "misc/deconstruction_fluix_block");
			}

			OUT.println("GT_Mod: AE Duplicate Recipes to suppress: " + tSuppress.size() + " " + tSuppress);
			gregapi.GT_API.removeDatapackRecipes(gregapi.GT_API.sCurrentServerForRecipeScan, tSuppress);
		});
	}

	/** Builds the datapack recipe's key from its file path, using the central mod-id constant rather than a literal string. */
	private static void suppressAE(java.util.Set<net.minecraft.resources.ResourceLocation> aSet, String aPath) {
		aSet.add(new net.minecraft.resources.ResourceLocation(MD.AE.mID, aPath));
	}
}

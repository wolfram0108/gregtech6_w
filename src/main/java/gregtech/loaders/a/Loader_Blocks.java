/**
 * Copyright (c) 2023 GregTech-6 Team
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
 */

package gregtech.loaders.a;

import gregapi.GT_API;
import net.neoforged.fml.InterModComms;
import gregapi.block.MaterialGas;
import gregapi.block.MaterialOil;
import gregapi.block.fluid.BlockBaseFluid;
import gregapi.data.*;
import gregapi.old.Textures;
import gregapi.util.CR;
import gregapi.util.ST;
import gregapi.util.UT;
import gregtech.blocks.*;
import gregtech.blocks.fluids.BlockOcean;
import gregtech.blocks.fluids.BlockRiver;
import gregtech.blocks.fluids.BlockSwamp;
import gregtech.blocks.plants.BlockFlowersA;
import gregtech.blocks.plants.BlockFlowersB;
import gregtech.blocks.plants.BlockGlowtus;
import gregtech.blocks.stone.BlockCrystalOres;
import gregtech.blocks.stone.BlockRockOres;
import gregtech.blocks.stone.BlockVanillaOresA;
import gregtech.blocks.tool.*;
import gregtech.experiments.BlockRiverAdvanced;
import net.minecraft.world.level.block.Block;
import gregapi.block.Material;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.world.effect.MobEffect;

import static gregapi.data.CS.*;

public class Loader_Blocks implements Runnable {
	@Override
	public void run() {
		GT_API.registerBlockLazy(gregapi.data.CS.ModIDs.GAPI, "gt.block.cfoam", () -> {BlockCFoam b = new BlockCFoam("gt.block.cfoam"); BlocksGT.CFoam = b; VISUALLY_OPAQUE_BLOCKS.add(b); return b;});
		GT_API.registerBlockLazy(gregapi.data.CS.ModIDs.GAPI, "gt.block.cfoam.fresh", () -> {BlockCFoamFresh b = new BlockCFoamFresh("gt.block.cfoam.fresh"); BlocksGT.CFoamFresh = b; return b;});
		if (COMPAT_TC != null) COMPAT_TC.registerThaumcraftAspectsToItem(ST.make(BlocksGT.CFoam, 1, W), F, TC.stack(TC.TERRA, 1), TC.stack(TC.TUTAMEN, 1));
		if (COMPAT_TC != null) COMPAT_TC.registerThaumcraftAspectsToItem(ST.make(BlocksGT.CFoamFresh, 1, W), F, TC.stack(TC.TERRA, 1), TC.stack(TC.TUTAMEN, 1));
		
		GT_API.registerBlockLazy(gregapi.data.CS.ModIDs.GAPI, "gt.block.asphalt", () -> {BlockAsphalt b = new BlockAsphalt("gt.block.asphalt"); BlocksGT.Asphalt = b; VISUALLY_OPAQUE_BLOCKS.add(b); return b;});
		if (COMPAT_TC != null) COMPAT_TC.registerThaumcraftAspectsToItem(ST.make(BlocksGT.Asphalt, 1, W), F, TC.stack(TC.TERRA, 1), TC.stack(TC.ITER, 1));
		ItemsGT.addNEIRedirects(BlocksGT.Asphalt);
		
		GT_API.registerBlockLazy(gregapi.data.CS.ModIDs.GAPI, "gt.block.concrete", () -> {BlockConcrete b = new BlockConcrete("gt.block.concrete"); BlocksGT.Concrete = b; VISUALLY_OPAQUE_BLOCKS.add(b); return b;});
		if (COMPAT_TC != null) COMPAT_TC.registerThaumcraftAspectsToItem(ST.make(BlocksGT.Concrete, 1, W), F, TC.stack(TC.TERRA, 1), TC.stack(TC.FABRICO, 1));
		ItemsGT.addNEIRedirects(BlocksGT.Concrete);
		
		GT_API.registerBlockLazy(gregapi.data.CS.ModIDs.GAPI, "gt.block.concrete.reinforced", () -> {BlockConcreteReinforced b = new BlockConcreteReinforced("gt.block.concrete.reinforced"); BlocksGT.ConcreteReinforced = b; VISUALLY_OPAQUE_BLOCKS.add(b); return b;});
		if (COMPAT_TC != null) COMPAT_TC.registerThaumcraftAspectsToItem(ST.make(BlocksGT.ConcreteReinforced, 1, W), F, TC.stack(TC.TERRA, 1), TC.stack(TC.FABRICO, 1), TC.stack(TC.TUTAMEN, 1));
		ItemsGT.addNEIRedirects(BlocksGT.ConcreteReinforced);
		for (byte i = 0; i < 16; i++) CR.shaped(ST.make(BlocksGT.ConcreteReinforced, 1, i), CR.DEF_MIR, "Se", "X ", 'X', ST.make(BlocksGT.Concrete, 1, i), 'S', OP.stick.dat(ANY.Iron));
		
		GT_API.registerBlockLazy(gregapi.data.CS.ModIDs.GAPI, "gt.block.glass", () -> {BlockGlassClear b = new BlockGlassClear("gt.block.glass"); BlocksGT.Glass = b; return b;});
		GT_API.registerBlockLazy(gregapi.data.CS.ModIDs.GAPI, "gt.block.glass.glow", () -> {BlockGlassGlow b = new BlockGlassGlow("gt.block.glass.glow"); BlocksGT.GlowGlass = b; return b;});
		if (COMPAT_TC != null) COMPAT_TC.registerThaumcraftAspectsToItem(ST.make(BlocksGT.Glass    , 1, W), F, TC.stack(TC.VITREUS, 2));
		if (COMPAT_TC != null) COMPAT_TC.registerThaumcraftAspectsToItem(ST.make(BlocksGT.GlowGlass, 1, W), F, TC.stack(TC.VITREUS, 2), TC.stack(TC.LUX, 2));
		
		GT_API.registerBlockLazy(gregapi.data.CS.ModIDs.GAPI, "gt.block.diggable", () -> {BlockDiggable b = new BlockDiggable("gt.block.diggable"); BlocksGT.Diggables = b; VISUALLY_OPAQUE_BLOCKS.add(b); return b;});
		if (COMPAT_TC != null) COMPAT_TC.registerThaumcraftAspectsToItem(ST.make(BlocksGT.Diggables, 1, W), F, TC.stack(TC.TERRA, 2));
		
		GT_API.registerBlockLazy(gregapi.data.CS.ModIDs.GAPI, "gt.block.grass", () -> {BlockGrass b = new BlockGrass("gt.block.grass"); BlocksGT.Grass = b; VISUALLY_OPAQUE_BLOCKS.add(b); return b;});
		if (COMPAT_TC != null) COMPAT_TC.registerThaumcraftAspectsToItem(ST.make(BlocksGT.Grass, 1, W), F, TC.stack(TC.TERRA, 2), TC.stack(TC.HERBA, 2));
		
		GT_API.registerBlockLazy(gregapi.data.CS.ModIDs.GAPI, "gt.block.paths", () -> {BlockPath b = new BlockPath("gt.block.paths"); BlocksGT.Paths = b; return b;});
		if (COMPAT_TC != null) COMPAT_TC.registerThaumcraftAspectsToItem(ST.make(BlocksGT.Paths, 1, W), F, TC.stack(TC.TERRA, 2));
		
		GT_API.registerBlockLazy(gregapi.data.CS.ModIDs.GAPI, "gt.block.rockores", () -> {BlockRockOres b = new BlockRockOres("gt.block.rockores"); BlocksGT.RockOres = b; VISUALLY_OPAQUE_BLOCKS.add(b); return b;});
		GT_API.registerBlockLazy(gregapi.data.CS.ModIDs.GAPI, "gt.block.crystalores", () -> {BlockCrystalOres b = new BlockCrystalOres("gt.block.crystalores"); BlocksGT.CrystalOres = b; VISUALLY_OPAQUE_BLOCKS.add(b); return b;});
		GT_API.registerBlockLazy(gregapi.data.CS.ModIDs.GAPI, "gt.block.vanillaores.a", () -> {BlockVanillaOresA b = new BlockVanillaOresA("gt.block.vanillaores.a"); BlocksGT.VanillaOresA = b; VISUALLY_OPAQUE_BLOCKS.add(b); return b;});
		
		GT_API.registerBlockLazy(gregapi.data.CS.ModIDs.GAPI, "gt.block.sands", () -> {BlockSands b = new BlockSands("gt.block.sands"); BlocksGT.Sands = b; VISUALLY_OPAQUE_BLOCKS.add(b); return b;});
		if (COMPAT_TC != null) COMPAT_TC.registerThaumcraftAspectsToItem(ST.make(BlocksGT.Sands, 1, W), F, TC.stack(TC.TERRA, 1));
		
		
		GT_API.registerBlockLazy(gregapi.data.CS.ModIDs.GAPI, "gt.block.spikes.sharp", () -> {BlockSpikeSharp b = new BlockSpikeSharp("gt.block.spikes.sharp"); BlocksGT.Spikes_Sharp = b; return b;});
		GT_API.registerBlockLazy(gregapi.data.CS.ModIDs.GAPI, "gt.block.spikes.steel", () -> {BlockSpikeSteel b = new BlockSpikeSteel("gt.block.spikes.steel"); BlocksGT.Spikes_Steel = b; return b;});
		GT_API.registerBlockLazy(gregapi.data.CS.ModIDs.GAPI, "gt.block.spikes.super", () -> {BlockSpikeSuper b = new BlockSpikeSuper("gt.block.spikes.super"); BlocksGT.Spikes_Super = b; return b;});
		GT_API.registerBlockLazy(gregapi.data.CS.ModIDs.GAPI, "gt.block.spikes.metal", () -> {BlockSpikeMetal b = new BlockSpikeMetal("gt.block.spikes.metal"); BlocksGT.Spikes_Metal = b; return b;});
		GT_API.registerBlockLazy(gregapi.data.CS.ModIDs.GAPI, "gt.block.spikes.fancy", () -> {BlockSpikeFancy b = new BlockSpikeFancy("gt.block.spikes.fancy"); BlocksGT.Spikes_Fancy = b; return b;});
		if (COMPAT_TC != null) COMPAT_TC.registerThaumcraftAspectsToItem(ST.make(BlocksGT.Spikes_Sharp, 1, W), F, TC.stack(TC.VINCULUM, 4), TC.stack(TC.METALLUM, 4), TC.stack(TC.TELUM, 4));
		if (COMPAT_TC != null) COMPAT_TC.registerThaumcraftAspectsToItem(ST.make(BlocksGT.Spikes_Steel, 1, W), F, TC.stack(TC.VINCULUM, 4), TC.stack(TC.METALLUM, 4), TC.stack(TC.TELUM, 4));
		if (COMPAT_TC != null) COMPAT_TC.registerThaumcraftAspectsToItem(ST.make(BlocksGT.Spikes_Super, 1, W), F, TC.stack(TC.VINCULUM, 8), TC.stack(TC.METALLUM, 4), TC.stack(TC.FABRICO, 4));
		if (COMPAT_TC != null) COMPAT_TC.registerThaumcraftAspectsToItem(ST.make(BlocksGT.Spikes_Metal, 1, W), F, TC.stack(TC.VINCULUM, 4), TC.stack(TC.METALLUM, 4), TC.stack(TC.VENEMUM, 4));
		if (COMPAT_TC != null) COMPAT_TC.registerThaumcraftAspectsToItem(ST.make(BlocksGT.Spikes_Fancy, 1, W), F, TC.stack(TC.VINCULUM, 4), TC.stack(TC.METALLUM, 4), TC.stack(TC.LUCRUM, 4));
		
		
		GT_API.registerBlockLazy(gregapi.data.CS.ModIDs.GAPI, "gt.block.bars.wood", () -> {BlockBarsWood b = new BlockBarsWood("gt.block.bars.wood"); BlocksGT.Bars_Wood = b; return b;});
		GT_API.registerBlockLazy(gregapi.data.CS.ModIDs.GAPI, "gt.block.bars.brass", () -> {BlockBarsBrass b = new BlockBarsBrass("gt.block.bars.brass"); BlocksGT.Bars_Brass = b; return b;});
		GT_API.registerBlockLazy(gregapi.data.CS.ModIDs.GAPI, "gt.block.bars.steel", () -> {BlockBarsSteel b = new BlockBarsSteel("gt.block.bars.steel"); BlocksGT.Bars_Steel = b; return b;});
		GT_API.registerBlockLazy(gregapi.data.CS.ModIDs.GAPI, "gt.block.bars.titanium", () -> {BlockBarsTitanium b = new BlockBarsTitanium("gt.block.bars.titanium"); BlocksGT.Bars_Titanium = b; return b;});
		GT_API.registerBlockLazy(gregapi.data.CS.ModIDs.GAPI, "gt.block.bars.tungstensteel", () -> {BlockBarsTungstenSteel b = new BlockBarsTungstenSteel("gt.block.bars.tungstensteel"); BlocksGT.Bars_TungstenSteel = b; return b;});
		GT_API.registerBlockLazy(gregapi.data.CS.ModIDs.GAPI, "gt.block.bars.adamantium", () -> {BlockBarsAdamantium b = new BlockBarsAdamantium("gt.block.bars.adamantium"); BlocksGT.Bars_Adamantium = b; return b;});
		if (COMPAT_TC != null) COMPAT_TC.registerThaumcraftAspectsToItem(ST.make(BlocksGT.Bars_Wood         , 1, W), F, TC.stack(TC.VINCULUM, 2), TC.stack(TC.ARBOR, 4));
		if (COMPAT_TC != null) COMPAT_TC.registerThaumcraftAspectsToItem(ST.make(BlocksGT.Bars_Brass        , 1, W), F, TC.stack(TC.VINCULUM, 3), TC.stack(TC.METALLUM, 2));
		if (COMPAT_TC != null) COMPAT_TC.registerThaumcraftAspectsToItem(ST.make(BlocksGT.Bars_Steel        , 1, W), F, TC.stack(TC.VINCULUM, 4), TC.stack(TC.METALLUM, 3));
		if (COMPAT_TC != null) COMPAT_TC.registerThaumcraftAspectsToItem(ST.make(BlocksGT.Bars_Titanium     , 1, W), F, TC.stack(TC.VINCULUM, 6), TC.stack(TC.METALLUM, 5));
		if (COMPAT_TC != null) COMPAT_TC.registerThaumcraftAspectsToItem(ST.make(BlocksGT.Bars_TungstenSteel, 1, W), F, TC.stack(TC.VINCULUM, 8), TC.stack(TC.METALLUM, 7));
		if (COMPAT_TC != null) COMPAT_TC.registerThaumcraftAspectsToItem(ST.make(BlocksGT.Bars_Adamantium   , 1, W), F, TC.stack(TC.VINCULUM,10), TC.stack(TC.METALLUM,10));
		
		
		GT_API.registerBlockLazy(gregapi.data.CS.ModIDs.GAPI, "gt.block.flower.a", () -> {BlockFlowersA b = new BlockFlowersA("gt.block.flower.a"); BlocksGT.FlowersA = b; return b;});
		GT_API.registerBlockLazy(gregapi.data.CS.ModIDs.GAPI, "gt.block.flower.b", () -> {BlockFlowersB b = new BlockFlowersB("gt.block.flower.b"); BlocksGT.FlowersB = b; return b;});
		if (COMPAT_TC != null) COMPAT_TC.registerThaumcraftAspectsToItem(ST.make((Block)BlocksGT.FlowersA, 1, W), F, TC.stack(TC.HERBA, 2));
		if (COMPAT_TC != null) COMPAT_TC.registerThaumcraftAspectsToItem(ST.make((Block)BlocksGT.FlowersB, 1, W), F, TC.stack(TC.HERBA, 2));
		
		GT_API.registerBlockLazy(gregapi.data.CS.ModIDs.GAPI, "gt.block.lilypad.glowtus", () -> {BlockGlowtus b = new BlockGlowtus("gt.block.lilypad.glowtus"); BlocksGT.Glowtus = b; return b;});
		if (COMPAT_TC != null) COMPAT_TC.registerThaumcraftAspectsToItem(ST.make(BlocksGT.Glowtus        , 1, W), F, TC.stack(TC.HERBA, 2), TC.stack(TC.LUX, 2));
		
		GT_API.registerBlockLazy(gregapi.data.CS.ModIDs.GAPI, "gt.block.bale.grass", () -> {BlockBaleGrass b = new BlockBaleGrass("gt.block.bale.grass"); BlocksGT.BalesGrass = b; VISUALLY_OPAQUE_BLOCKS.add(b); return b;});
		GT_API.registerBlockLazy(gregapi.data.CS.ModIDs.GAPI, "gt.block.bale.crop", () -> {BlockBaleCrop b = new BlockBaleCrop("gt.block.bale.crop"); BlocksGT.BalesCrop = b; VISUALLY_OPAQUE_BLOCKS.add(b); return b;});
		if (COMPAT_TC != null) COMPAT_TC.registerThaumcraftAspectsToItem(ST.make(BlocksGT.BalesGrass     , 1, W), F, TC.stack(TC.MESSIS, 4));
		if (COMPAT_TC != null) COMPAT_TC.registerThaumcraftAspectsToItem(ST.make(BlocksGT.BalesCrop      , 1, W), F, TC.stack(TC.MESSIS, 4));
		
		if (EXPERIMENTS)
		GT_API.registerBlockLazy(gregapi.data.CS.ModIDs.GAPI, "gt.block.river.adv", () -> {BlockRiverAdvanced b = new BlockRiverAdvanced("gt.block.river.adv", FL.Soda       .fluid()); BlocksGT.RiverAdvanced = b; return b;});
		GT_API.registerBlockLazy(gregapi.data.CS.ModIDs.GAPI, "gt.block.river", () -> {BlockRiver b = new BlockRiver("gt.block.river"    , FL.River_Water.fluid()); BlocksGT.River = b; return b;});
		GT_API.registerBlockLazy(gregapi.data.CS.ModIDs.GAPI, "gt.block.ocean", () -> {BlockOcean b = new BlockOcean("gt.block.ocean"    , FL.Ocean      .fluid()); BlocksGT.Ocean = b; return b;});
		GT_API.registerBlockLazy(gregapi.data.CS.ModIDs.GAPI, "gt.block.swamp", () -> {net.minecraft.world.level.block.Block b = new BlockSwamp("gt.block.swamp"    , FL.Dirty_Water.fluid()).addEffect(/*hunger*/17, 300, 0).addEffect(/*confusion*/9, 120, 0); BlocksGT.Swamp = b; return b;});
		if (COMPAT_TC != null) COMPAT_TC.registerThaumcraftAspectsToItem(ST.make(BlocksGT.River          , 1, W), F, TC.stack(TC.AQUA, 3), TC.stack(TC.MOTUS, 3));
		if (COMPAT_TC != null) COMPAT_TC.registerThaumcraftAspectsToItem(ST.make(BlocksGT.Ocean          , 1, W), F, TC.stack(TC.AQUA, 3), TC.stack(TC.TEMPESTAS, 3));
		if (COMPAT_TC != null) COMPAT_TC.registerThaumcraftAspectsToItem(ST.make(BlocksGT.Swamp          , 1, W), F, TC.stack(TC.AQUA, 3), TC.stack(TC.VENEMUM, 1));
		ListTag tNBTList = new ListTag();
		tNBTList.add(new StringTag(ST.regName(BlocksGT.River)));
		tNBTList.add(new StringTag(ST.regName(BlocksGT.Ocean)));
		InterModComms.sendTo(MD.IC2C.mID, "watergen", () -> UT.NBT.make("blocks", tNBTList));
		
		GT_API.registerBlockLazy(gregapi.data.CS.ModIDs.GAPI, "gt.block.fluid.water.geothermal", () -> {BlockBaseFluid b = new BlockBaseFluid("gt.block.fluid.water.geothermal"  , FL.Water_Geothermal,    0, Material.water      ).setLighterThanWater().addEffectBathing(/*regeneration*/10, 100, 0).addEffectBathing(/*resistance*/11, 2400, 2); BlocksGT.WaterGeothermal = b; return b;});
		if (COMPAT_TC != null) COMPAT_TC.registerThaumcraftAspectsToItem(ST.make(BlocksGT.WaterGeothermal, 1, W), F, TC.stack(TC.AQUA, 3), TC.stack(TC.SANO, 3));
		
		GT_API.registerBlockLazy(gregapi.data.CS.ModIDs.GAPI, "gt.block.fluid.oil.extraheavy", () -> {BlockBaseFluid b = new BlockBaseFluid("gt.block.fluid.oil.extraheavy"    , FL.Oil_ExtraHeavy  , 1000, MaterialOil.instance).setLighterThanWater().addEffectBreathing(/*poison*/19, 300, 0).addEffectBreathing(/*confusion*/9, 120, 0).addEffectBathing(PotionsGT.ID_FLAMMABLE, 300, 1).addEffectBathing(PotionsGT.ID_STICKY  , 300, 1).addEffectBathing(/*blindness*/15, 60, 1).setWeb(); BlocksGT.OilExtraHeavy = b; return b;});
		GT_API.registerBlockLazy(gregapi.data.CS.ModIDs.GAPI, "gt.block.fluid.oil.heavy", () -> {BlockBaseFluid b = new BlockBaseFluid("gt.block.fluid.oil.heavy"         , FL.Oil_Heavy       , 1000, MaterialOil.instance).setLighterThanWater().addEffectBreathing(/*poison*/19, 300, 0).addEffectBreathing(/*confusion*/9, 120, 0).addEffectBathing(PotionsGT.ID_FLAMMABLE, 300, 1).addEffectBathing(PotionsGT.ID_STICKY  , 300, 1).addEffectBathing(/*blindness*/15, 60, 1).setWeb(); BlocksGT.OilHeavy = b; return b;});
		GT_API.registerBlockLazy(gregapi.data.CS.ModIDs.GAPI, "gt.block.fluid.oil.medium", () -> {BlockBaseFluid b = new BlockBaseFluid("gt.block.fluid.oil.medium"        , FL.Oil_Medium      , 1000, MaterialOil.instance).setLighterThanWater().addEffectBreathing(/*poison*/19, 300, 0).addEffectBreathing(/*confusion*/9, 120, 0).addEffectBathing(PotionsGT.ID_FLAMMABLE, 300, 1).addEffectBathing(PotionsGT.ID_SLIPPERY, 300, 1).addEffectBathing(/*blindness*/15, 60, 1); BlocksGT.OilMedium = b; return b;});
		GT_API.registerBlockLazy(gregapi.data.CS.ModIDs.GAPI, "gt.block.fluid.oil.light", () -> {BlockBaseFluid b = new BlockBaseFluid("gt.block.fluid.oil.light"         , FL.Oil_Light       , 1000, MaterialOil.instance).setLighterThanWater().addEffectBreathing(/*poison*/19, 300, 0).addEffectBreathing(/*confusion*/9, 120, 0).addEffectBathing(PotionsGT.ID_FLAMMABLE, 300, 1).addEffectBathing(PotionsGT.ID_SLIPPERY, 300, 1).addEffectBathing(/*blindness*/15, 60, 1); BlocksGT.OilLight = b; return b;});
		GT_API.registerBlockLazy(gregapi.data.CS.ModIDs.GAPI, "gt.block.fluid.gas.natural", () -> {BlockBaseFluid b = new BlockBaseFluid("gt.block.fluid.gas.natural"       , FL.Gas_Natural     , 1000, MaterialGas.instance).setLighterThanWater().addEffectBreathing(/*poison*/19, 300, 0).addEffectBreathing(/*confusion*/9, 120, 0); BlocksGT.GasNatural = b; return b;});
		if (COMPAT_TC != null) COMPAT_TC.registerThaumcraftAspectsToItem(ST.make(BlocksGT.OilExtraHeavy  , 1, W), F, TC.stack(TC.AQUA, 1), TC.stack(TC.IGNIS, 1), TC.stack(TC.POTENTIA, 3));
		if (COMPAT_TC != null) COMPAT_TC.registerThaumcraftAspectsToItem(ST.make(BlocksGT.OilHeavy       , 1, W), F, TC.stack(TC.AQUA, 1), TC.stack(TC.IGNIS, 1), TC.stack(TC.POTENTIA, 2));
		if (COMPAT_TC != null) COMPAT_TC.registerThaumcraftAspectsToItem(ST.make(BlocksGT.OilMedium      , 1, W), F, TC.stack(TC.AQUA, 1), TC.stack(TC.IGNIS, 1), TC.stack(TC.POTENTIA, 1));
		if (COMPAT_TC != null) COMPAT_TC.registerThaumcraftAspectsToItem(ST.make(BlocksGT.OilLight       , 1, W), F, TC.stack(TC.AQUA, 1), TC.stack(TC.IGNIS, 1), TC.stack(TC.LUX, 1));
		if (COMPAT_TC != null) COMPAT_TC.registerThaumcraftAspectsToItem(ST.make(BlocksGT.GasNatural     , 1, W), F, TC.stack(TC.AER , 1), TC.stack(TC.IGNIS, 1), TC.stack(TC.LUX, 1));
		
		GT_API.registerBlockLazy(gregapi.data.CS.ModIDs.GAPI, "gt.block.longdistwire.01", () -> {BlockLongDistWire b = new BlockLongDistWire("gt.block.longdistwire.01", Textures.BlockIcons.LONG_DIST_WIRES_01, new byte[] {4, 4, 5, 6, 6, 6, 6, 6, 7, 7, 7, 7, 8, 8, 8, 8}); BlocksGT.LongDistWire01 = b; VISUALLY_OPAQUE_BLOCKS.add(b); return b;});
		
		CR.shaped(ST.make(BlocksGT.LongDistWire01, 1, 0), CR.DEF_REV_NCC, "RSR", "PWP", "RSR", 'R', OP.plate.dat(ANY.Rubber), 'P', OP.plateCurved.dat(ANY.Cu), 'S', OP.plateCurved.dat(MT.Al), 'W', OP.wireGt16.dat(MT.Sn));
		CR.shaped(ST.make(BlocksGT.LongDistWire01, 1, 1), CR.DEF_REV_NCC, "RSR", "PWP", "RSR", 'R', OP.plate.dat(ANY.Rubber), 'P', OP.plateCurved.dat(ANY.Cu), 'S', OP.plateCurved.dat(MT.Al), 'W', OP.wireGt16.dat(MT.Pb));
		CR.shaped(ST.make(BlocksGT.LongDistWire01, 1, 2), CR.DEF_REV_NCC, "RSR", "PWP", "RSR", 'R', OP.plate.dat(ANY.Rubber), 'P', OP.plateCurved.dat(ANY.Cu), 'S', OP.plateCurved.dat(MT.Al), 'W', OP.wireGt16.dat(ANY.Cu));
		CR.shaped(ST.make(BlocksGT.LongDistWire01, 1, 3), CR.DEF_REV_NCC, "RSR", "PWP", "RSR", 'R', OP.plate.dat(ANY.Rubber), 'P', OP.plateCurved.dat(ANY.Cu), 'S', OP.plateCurved.dat(MT.Al), 'W', OP.wireGt16.dat(MT.Ag));
		CR.shaped(ST.make(BlocksGT.LongDistWire01, 1, 4), CR.DEF_REV_NCC, "RSR", "PWP", "RSR", 'R', OP.plate.dat(ANY.Rubber), 'P', OP.plateCurved.dat(ANY.Cu), 'S', OP.plateCurved.dat(MT.Al), 'W', OP.wireGt16.dat(MT.Au));
		CR.shaped(ST.make(BlocksGT.LongDistWire01, 1, 5), CR.DEF_REV_NCC, "RSR", "PWP", "RSR", 'R', OP.plate.dat(ANY.Rubber), 'P', OP.plateCurved.dat(ANY.Cu), 'S', OP.plateCurved.dat(MT.Al), 'W', OP.wireGt16.dat(MT.Electrum));
		CR.shaped(ST.make(BlocksGT.LongDistWire01, 1, 6), CR.DEF_REV_NCC, "RSR", "PWP", "RSR", 'R', OP.plate.dat(ANY.Rubber), 'P', OP.plateCurved.dat(ANY.Cu), 'S', OP.plateCurved.dat(MT.Al), 'W', OP.wireGt16.dat(MT.BlueAlloy));
		CR.shaped(ST.make(BlocksGT.LongDistWire01, 1, 7), CR.DEF_REV_NCC, "RSR", "PWP", "RSR", 'R', OP.plate.dat(ANY.Rubber), 'P', OP.plateCurved.dat(ANY.Cu), 'S', OP.plateCurved.dat(MT.Al), 'W', OP.wireGt16.dat(MT.ElectrotineAlloy));
		CR.shaped(ST.make(BlocksGT.LongDistWire01, 1, 8), CR.DEF_REV_NCC, "RSR", "PWP", "RSR", 'R', OP.plate.dat(ANY.Rubber), 'P', OP.plateCurved.dat(ANY.Cu), 'S', OP.plateCurved.dat(MT.Al), 'W', OP.wireGt16.dat(ANY.Steel));
		CR.shaped(ST.make(BlocksGT.LongDistWire01, 1, 9), CR.DEF_REV_NCC, "RSR", "PWP", "RSR", 'R', OP.plate.dat(ANY.Rubber), 'P', OP.plateCurved.dat(ANY.Cu), 'S', OP.plateCurved.dat(MT.Al), 'W', OP.wireGt16.dat(MT.Al));
		CR.shaped(ST.make(BlocksGT.LongDistWire01, 1,10), CR.DEF_REV_NCC, "RSR", "PWP", "RSR", 'R', OP.plate.dat(ANY.Rubber), 'P', OP.plateCurved.dat(ANY.Cu), 'S', OP.plateCurved.dat(MT.Al), 'W', OP.wireGt16.dat(ANY.W));
		CR.shaped(ST.make(BlocksGT.LongDistWire01, 1,11), CR.DEF_REV_NCC, "RSR", "PWP", "RSR", 'R', OP.plate.dat(ANY.Rubber), 'P', OP.plateCurved.dat(ANY.Cu), 'S', OP.plateCurved.dat(MT.Al), 'W', OP.wireGt16.dat(MT.TungstenSteel));
		CR.shaped(ST.make(BlocksGT.LongDistWire01, 1,12), CR.DEF_REV_NCC, "RSR", "PWP", "RSR", 'R', OP.plate.dat(ANY.Rubber), 'P', OP.plateCurved.dat(ANY.Cu), 'S', OP.plateCurved.dat(MT.Al), 'W', OP.wireGt16.dat(MT.Os));
		CR.shaped(ST.make(BlocksGT.LongDistWire01, 1,13), CR.DEF_REV_NCC, "RSR", "PWP", "RSR", 'R', OP.plate.dat(ANY.Rubber), 'P', OP.plateCurved.dat(ANY.Cu), 'S', OP.plateCurved.dat(MT.Al), 'W', OP.wireGt16.dat(MT.Pt));
		CR.shaped(ST.make(BlocksGT.LongDistWire01, 1,14), CR.DEF_REV_NCC, "RSR", "PWP", "RSR", 'R', OP.plate.dat(ANY.Rubber), 'P', OP.plateCurved.dat(ANY.Cu), 'S', OP.plateCurved.dat(MT.Al), 'W', OP.wireGt16.dat(MT.Nq));
		CR.shaped(ST.make(BlocksGT.LongDistWire01, 1,15), CR.DEF_REV_NCC, "RSR", "PWP", "RSR", 'R', OP.plate.dat(ANY.Rubber), 'P', OP.plateCurved.dat(ANY.Cu), 'S', OP.plateCurved.dat(MT.Al), 'W', OP.wireGt16.dat(MT.Graphene));
		
		GT_API.registerBlockLazy(gregapi.data.CS.ModIDs.GAPI, "gt.block.longdistpipe.01", () -> {BlockLongDistPipe b = new BlockLongDistPipe("gt.block.longdistpipe.01", Textures.BlockIcons.LONG_DIST_PIPES_01, new long[] {-1, MT.StainlessSteel.mMeltingPoint, MT.W.mMeltingPoint, MT.Ad.mMeltingPoint, MT.Draconium.mMeltingPoint, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0}); BlocksGT.LongDistPipe01 = b; VISUALLY_OPAQUE_BLOCKS.add(b); return b;});
		
		CR.shaped(ST.make(BlocksGT.LongDistPipe01, 1, 0), CR.DEF_REV_NCC, "SPS", "PwP", "SPS", 'P', OP.pipeMedium.dat(MT.Electrum       ), 'S', OP.plate.dat(ANY.Plastic));
		CR.shaped(ST.make(BlocksGT.LongDistPipe01, 1, 1), CR.DEF_REV_NCC, "SPS", "PwP", "SPS", 'P', OP.pipeMedium.dat(MT.StainlessSteel ), 'S', OP.plate.dat(ANY.Plastic));
		CR.shaped(ST.make(BlocksGT.LongDistPipe01, 1, 2), CR.DEF_REV_NCC, "SPS", "PwP", "SPS", 'P', OP.pipeMedium.dat(ANY.W             ), 'S', OP.plate.dat(ANY.Plastic));
		CR.shaped(ST.make(BlocksGT.LongDistPipe01, 1, 3), CR.DEF_REV_NCC, "SPS", "PwP", "SPS", 'P', OP.pipeMedium.dat(MT.Ad             ), 'S', OP.plate.dat(ANY.Plastic));
		CR.shaped(ST.make(BlocksGT.LongDistPipe01, 1, 4), CR.DEF_REV_NCC, "SPS", "PwP", "SPS", 'P', OP.pipeMedium.dat(MT.Draconium      ), 'S', OP.plate.dat(ANY.Plastic));
		
		/*
		OM.reg(OP.blockGlass    .toString()                     , ST.make(BlocksGT.Glass, 1, W));
		for (byte i = 0; i < 16; i++) {
		OM.reg(OP.stainedGlass  .toString()+DYE_OREDICTS_POST[i], ST.make(BlocksGT.Glass, 1, i));
		OM.reg(OP.blockGlass    .toString()+DYE_OREDICTS_POST[i], ST.make(BlocksGT.Glass, 1, i));
		}*/
	}
}

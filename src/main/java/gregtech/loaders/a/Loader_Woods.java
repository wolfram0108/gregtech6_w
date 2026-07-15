/**
 * Copyright (c) 2025 GregTech-6 Team
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
import gregapi.block.metatype.BlockMetaType;
import gregapi.data.*;
import gregapi.oredict.OreDictManager;
import gregapi.util.CR;
import gregapi.util.OM;
import gregapi.util.ST;
import gregtech.blocks.tree.*;
import gregtech.blocks.wood.*;
import net.minecraft.world.level.block.Blocks;

import static gregapi.data.CS.*;

public class Loader_Woods implements Runnable {
	@Override
	@SuppressWarnings("deprecation")
	public void run() {
		// F12-followup (block-split): 28 конструкций деревьев → registerBlockLazy (конструкция на RegisterEvent<Block>,
		// реестр разморожен); поле BlocksGT.X и VISUALLY_OPAQUE_BLOCKS.add — внутри supplier (нужен инстанс). Пост-настройка
		// (ST.make/CR.shaped/OM/OreDict, ниже) — deferItemInit (server-start): поля заселены RegisterEvent'ом + компоненты связаны.
		GT_API.registerBlockLazy(gregapi.data.CS.ModIDs.GT, "gt.block.log.1"            , () -> {BlockTreeLog1             b = new BlockTreeLog1            ("gt.block.log.1"            ); BlocksGT.Log1             = b; VISUALLY_OPAQUE_BLOCKS.add(b); return b;});
		GT_API.registerBlockLazy(gregapi.data.CS.ModIDs.GT, "gt.block.log.1.fireproof"  , () -> {BlockTreeLog1FireProof    b = new BlockTreeLog1FireProof   ("gt.block.log.1.fireproof"  ); BlocksGT.Log1FireProof    = b; VISUALLY_OPAQUE_BLOCKS.add(b); return b;});
		GT_API.registerBlockLazy(gregapi.data.CS.ModIDs.GT, "gt.block.log.a"            , () -> {BlockTreeLogA             b = new BlockTreeLogA            ("gt.block.log.a"            ); BlocksGT.LogA             = b; VISUALLY_OPAQUE_BLOCKS.add(b); return b;});
		GT_API.registerBlockLazy(gregapi.data.CS.ModIDs.GT, "gt.block.log.a.fireproof"  , () -> {BlockTreeLogAFireProof    b = new BlockTreeLogAFireProof   ("gt.block.log.a.fireproof"  ); BlocksGT.LogAFireProof    = b; VISUALLY_OPAQUE_BLOCKS.add(b); return b;});
		GT_API.registerBlockLazy(gregapi.data.CS.ModIDs.GT, "gt.block.log.b"            , () -> {BlockTreeLogB             b = new BlockTreeLogB            ("gt.block.log.b"            ); BlocksGT.LogB             = b; VISUALLY_OPAQUE_BLOCKS.add(b); return b;});
		GT_API.registerBlockLazy(gregapi.data.CS.ModIDs.GT, "gt.block.log.b.fireproof"  , () -> {BlockTreeLogBFireProof    b = new BlockTreeLogBFireProof   ("gt.block.log.b.fireproof"  ); BlocksGT.LogBFireProof    = b; VISUALLY_OPAQUE_BLOCKS.add(b); return b;});
		GT_API.registerBlockLazy(gregapi.data.CS.ModIDs.GT, "gt.block.log.c"            , () -> {BlockTreeLogC             b = new BlockTreeLogC            ("gt.block.log.c"            ); BlocksGT.LogC             = b; VISUALLY_OPAQUE_BLOCKS.add(b); return b;});
		GT_API.registerBlockLazy(gregapi.data.CS.ModIDs.GT, "gt.block.log.c.fireproof"  , () -> {BlockTreeLogCFireProof    b = new BlockTreeLogCFireProof   ("gt.block.log.c.fireproof"  ); BlocksGT.LogCFireProof    = b; VISUALLY_OPAQUE_BLOCKS.add(b); return b;});
	//  VISUALLY_OPAQUE_BLOCKS.add(BlocksGT.LogD             = new BlockTreeLogD            ("gt.block.log.d"));
	//  VISUALLY_OPAQUE_BLOCKS.add(BlocksGT.LogDFireProof    = new BlockTreeLogDFireProof   ("gt.block.log.d.fireproof"));
		GT_API.registerBlockLazy(gregapi.data.CS.ModIDs.GT, "gt.block.beam.a"           , () -> {BlockTreeBeamA            b = new BlockTreeBeamA           ("gt.block.beam.a"           ); BlocksGT.BeamA            = b; VISUALLY_OPAQUE_BLOCKS.add(b); return b;});
		GT_API.registerBlockLazy(gregapi.data.CS.ModIDs.GT, "gt.block.beam.a.fireproof" , () -> {BlockTreeBeamAFireProof   b = new BlockTreeBeamAFireProof  ("gt.block.beam.a.fireproof" ); BlocksGT.BeamAFireProof   = b; VISUALLY_OPAQUE_BLOCKS.add(b); return b;});
		GT_API.registerBlockLazy(gregapi.data.CS.ModIDs.GT, "gt.block.beam.b"           , () -> {BlockTreeBeamB            b = new BlockTreeBeamB           ("gt.block.beam.b"           ); BlocksGT.BeamB            = b; VISUALLY_OPAQUE_BLOCKS.add(b); return b;});
		GT_API.registerBlockLazy(gregapi.data.CS.ModIDs.GT, "gt.block.beam.b.fireproof" , () -> {BlockTreeBeamBFireProof   b = new BlockTreeBeamBFireProof  ("gt.block.beam.b.fireproof" ); BlocksGT.BeamBFireProof   = b; VISUALLY_OPAQUE_BLOCKS.add(b); return b;});
		GT_API.registerBlockLazy(gregapi.data.CS.ModIDs.GT, "gt.block.beam.c"           , () -> {BlockTreeBeamC            b = new BlockTreeBeamC           ("gt.block.beam.c"           ); BlocksGT.BeamC            = b; VISUALLY_OPAQUE_BLOCKS.add(b); return b;});
		GT_API.registerBlockLazy(gregapi.data.CS.ModIDs.GT, "gt.block.beam.c.fireproof" , () -> {BlockTreeBeamCFireProof   b = new BlockTreeBeamCFireProof  ("gt.block.beam.c.fireproof" ); BlocksGT.BeamCFireProof   = b; VISUALLY_OPAQUE_BLOCKS.add(b); return b;});
	//  VISUALLY_OPAQUE_BLOCKS.add(BlocksGT.BeamD            = new BlockTreeBeamD           ("gt.block.beam.d"));
	//  VISUALLY_OPAQUE_BLOCKS.add(BlocksGT.BeamDFireProof   = new BlockTreeBeamDFireProof   ("gt.block.beam.d.fireproof"));
		GT_API.registerBlockLazy(gregapi.data.CS.ModIDs.GT, "gt.block.beam.1"           , () -> {BlockTreeBeam1            b = new BlockTreeBeam1           ("gt.block.beam.1"           ); BlocksGT.Beam1            = b; VISUALLY_OPAQUE_BLOCKS.add(b); return b;});
		GT_API.registerBlockLazy(gregapi.data.CS.ModIDs.GT, "gt.block.beam.1.fireproof" , () -> {BlockTreeBeam1FireProof   b = new BlockTreeBeam1FireProof  ("gt.block.beam.1.fireproof" ); BlocksGT.Beam1FireProof   = b; VISUALLY_OPAQUE_BLOCKS.add(b); return b;});
		GT_API.registerBlockLazy(gregapi.data.CS.ModIDs.GT, "gt.block.beam.2"           , () -> {BlockTreeBeam2            b = new BlockTreeBeam2           ("gt.block.beam.2"           ); BlocksGT.Beam2            = b; VISUALLY_OPAQUE_BLOCKS.add(b); return b;});
		GT_API.registerBlockLazy(gregapi.data.CS.ModIDs.GT, "gt.block.beam.2.fireproof" , () -> {BlockTreeBeam2FireProof   b = new BlockTreeBeam2FireProof  ("gt.block.beam.2.fireproof" ); BlocksGT.Beam2FireProof   = b; VISUALLY_OPAQUE_BLOCKS.add(b); return b;});
		GT_API.registerBlockLazy(gregapi.data.CS.ModIDs.GT, "gt.block.beam.3"           , () -> {BlockTreeBeam3            b = new BlockTreeBeam3           ("gt.block.beam.3"           ); BlocksGT.Beam3            = b; VISUALLY_OPAQUE_BLOCKS.add(b); return b;});
		GT_API.registerBlockLazy(gregapi.data.CS.ModIDs.GT, "gt.block.beam.3.fireproof" , () -> {BlockTreeBeam3FireProof   b = new BlockTreeBeam3FireProof  ("gt.block.beam.3.fireproof" ); BlocksGT.Beam3FireProof   = b; VISUALLY_OPAQUE_BLOCKS.add(b); return b;});
		GT_API.registerBlockLazy(gregapi.data.CS.ModIDs.GT, "gt.block.planks"           , () -> {BlockTreePlanks           b = new BlockTreePlanks          ("gt.block.planks"           ); BlocksGT.Planks           = b; VISUALLY_OPAQUE_BLOCKS.add(b); return b;});
		GT_API.registerBlockLazy(gregapi.data.CS.ModIDs.GT, "gt.block.planks.fireproof" , () -> {BlockTreePlanksFireProof  b = new BlockTreePlanksFireProof ("gt.block.planks.fireproof" ); BlocksGT.PlanksFireProof  = b; VISUALLY_OPAQUE_BLOCKS.add(b); return b;});
		GT_API.registerBlockLazy(gregapi.data.CS.ModIDs.GT, "gt.block.planks2"          , () -> {BlockTreePlanks2          b = new BlockTreePlanks2         ("gt.block.planks2"          ); BlocksGT.Planks2          = b; VISUALLY_OPAQUE_BLOCKS.add(b); return b;});
		GT_API.registerBlockLazy(gregapi.data.CS.ModIDs.GT, "gt.block.planks2.fireproof", () -> {BlockTreePlanks2FireProof b = new BlockTreePlanks2FireProof("gt.block.planks2.fireproof"); BlocksGT.Planks2FireProof = b; VISUALLY_OPAQUE_BLOCKS.add(b); return b;});

		GT_API.registerBlockLazy(gregapi.data.CS.ModIDs.GT, "gt.block.sapling"          , () -> {BlockTreeSaplingAB b = new BlockTreeSaplingAB("gt.block.sapling"   ); BlocksGT.Saplings_AB = b; BlocksGT.Sapling = b; return b;});
		GT_API.registerBlockLazy(gregapi.data.CS.ModIDs.GT, "gt.block.sapling.cd"       , () -> {BlockTreeSaplingCD b = new BlockTreeSaplingCD("gt.block.sapling.cd"); BlocksGT.Saplings_CD = b; return b;});
		GT_API.registerBlockLazy(gregapi.data.CS.ModIDs.GT, "gt.block.leaves"           , () -> {BlockTreeLeavesAB  b = new BlockTreeLeavesAB ("gt.block.leaves"   , BlocksGT.Saplings_AB); BlocksGT.Leaves_AB = b; BlocksGT.Leaves = b; return b;});
		GT_API.registerBlockLazy(gregapi.data.CS.ModIDs.GT, "gt.block.leaves.cd"        , () -> {BlockTreeLeavesCD  b = new BlockTreeLeavesCD ("gt.block.leaves.cd", BlocksGT.Saplings_CD); BlocksGT.Leaves_CD = b; return b;});

		gregapi.GT_API.deferItemInit(() -> {
		IL.Plank_Stairs       .set(ST.make(Blocks.OAK_STAIRS, 1, 0));
		IL.Plank_Slab         .set(ST.make(((BlockMetaType)BlocksGT.Planks).mSlabs[0], 1, 9));
		IL.Plank              .set(ST.make(BlocksGT.Planks, 1,  9));
		IL.Treated_Planks_Slab.set(ST.make(((BlockMetaType)BlocksGT.Planks).mSlabs[0], 1, 10));
		IL.Treated_Planks     .set(ST.make(BlocksGT.Planks, 1, 10));
		IL.Beam               .set(ST.make(BlocksGT.Beam2 , 1,  3));
		IL.Crate              .set(ST.make(BlocksGT.Planks, 1, 11), null, OD.crateGtEmpty);
		IL.Crate_Fireproof    .set(ST.make(BlocksGT.PlanksFireProof, 1, 11), null, OD.crateGtEmpty);
		
		CR.shaped(IL.Crate.get(1), CR.DEF_NCC, "Ts", "Pd", 'P', OD.plankAnyWood, 'T', OP.screw.dat(MT.HSLA));
		CR.shaped(IL.Crate.get(1), CR.DEF_NCC, "Ts", "Pd", 'P', OD.plankAnyWood, 'T', OP.screw.dat(ANY.Iron));
		CR.shaped(IL.Crate.get(1), CR.DEF_NCC, "Ts", "Pd", 'P', OD.plankAnyWood, 'T', OP.screw.dat(ANY.WoodPlastic));
		
		OM.reg(OP.plate, MT.WoodTreated, ST.make(BlocksGT.PlanksFireProof, 1, 10));
		OreDictManager.INSTANCE.setTarget(OP.plate, MT.Wood       , IL.Plank.get(1));
		OreDictManager.INSTANCE.setTarget(OP.plate, MT.WoodTreated, ST.make(BlocksGT.Planks, 1, 10));
		
		if (COMPAT_TC != null) {
		COMPAT_TC.registerThaumcraftAspectsToItem(ST.make(BlocksGT.Log1          , 1, W), F, TC.stack(TC.ARBOR, 2));
		COMPAT_TC.registerThaumcraftAspectsToItem(ST.make(BlocksGT.Log1FireProof , 1, W), F, TC.stack(TC.ARBOR, 2), TC.stack(TC.GELUM, 1));
		COMPAT_TC.registerThaumcraftAspectsToItem(ST.make(BlocksGT.Beam1         , 1, W), F, TC.stack(TC.ARBOR, 4));
		COMPAT_TC.registerThaumcraftAspectsToItem(ST.make(BlocksGT.Beam1FireProof, 1, W), F, TC.stack(TC.ARBOR, 4), TC.stack(TC.GELUM, 1));
		COMPAT_TC.registerThaumcraftAspectsToItem(ST.make(BlocksGT.Beam2         , 1, W), F, TC.stack(TC.ARBOR, 4));
		COMPAT_TC.registerThaumcraftAspectsToItem(ST.make(BlocksGT.Beam2FireProof, 1, W), F, TC.stack(TC.ARBOR, 4), TC.stack(TC.GELUM, 1));
		COMPAT_TC.registerThaumcraftAspectsToItem(ST.make(BlocksGT.Beam3         , 1, W), F, TC.stack(TC.ARBOR, 4));
		COMPAT_TC.registerThaumcraftAspectsToItem(ST.make(BlocksGT.Beam3FireProof, 1, W), F, TC.stack(TC.ARBOR, 4), TC.stack(TC.GELUM, 1));
		COMPAT_TC.registerThaumcraftAspectsToItem(ST.make(BlocksGT.BeamA         , 1, W), F, TC.stack(TC.ARBOR, 4));
		COMPAT_TC.registerThaumcraftAspectsToItem(ST.make(BlocksGT.BeamAFireProof, 1, W), F, TC.stack(TC.ARBOR, 4), TC.stack(TC.GELUM, 1));
		COMPAT_TC.registerThaumcraftAspectsToItem(ST.make(BlocksGT.BeamB         , 1, W), F, TC.stack(TC.ARBOR, 4));
		COMPAT_TC.registerThaumcraftAspectsToItem(ST.make(BlocksGT.BeamBFireProof, 1, W), F, TC.stack(TC.ARBOR, 4), TC.stack(TC.GELUM, 1));
		COMPAT_TC.registerThaumcraftAspectsToItem(ST.make(BlocksGT.BeamC         , 1, W), F, TC.stack(TC.ARBOR, 4));
		COMPAT_TC.registerThaumcraftAspectsToItem(ST.make(BlocksGT.BeamCFireProof, 1, W), F, TC.stack(TC.ARBOR, 4), TC.stack(TC.GELUM, 1));
		COMPAT_TC.registerThaumcraftAspectsToItem(ST.make(BlocksGT.BeamD         , 1, W), F, TC.stack(TC.ARBOR, 4));
		COMPAT_TC.registerThaumcraftAspectsToItem(ST.make(BlocksGT.BeamDFireProof, 1, W), F, TC.stack(TC.ARBOR, 4), TC.stack(TC.GELUM, 1));
		}
		
		// Those typically get overridden, but in case of a fuck up it is at least worth some Wood.
		OM.data(ST.make(BlocksGT.Log1          , 1, W), ANY.Wood, U*4);
		OM.data(ST.make(BlocksGT.Log1FireProof , 1, W), ANY.Wood, U*4);
		OM.data(ST.make(BlocksGT.Beam1         , 1, W), ANY.Wood, U*8);
		OM.data(ST.make(BlocksGT.Beam1FireProof, 1, W), ANY.Wood, U*8);
		OM.data(ST.make(BlocksGT.Beam2         , 1, W), ANY.Wood, U*8);
		OM.data(ST.make(BlocksGT.Beam2FireProof, 1, W), ANY.Wood, U*8);
		OM.data(ST.make(BlocksGT.Beam3         , 1, W), ANY.Wood, U*8);
		OM.data(ST.make(BlocksGT.Beam3FireProof, 1, W), ANY.Wood, U*8);
		OM.data(ST.make(BlocksGT.BeamA         , 1, W), ANY.Wood, U*8);
		OM.data(ST.make(BlocksGT.BeamAFireProof, 1, W), ANY.Wood, U*8);
		OM.data(ST.make(BlocksGT.BeamB         , 1, W), ANY.Wood, U*8);
		OM.data(ST.make(BlocksGT.BeamBFireProof, 1, W), ANY.Wood, U*8);
		OM.data(ST.make(BlocksGT.BeamC         , 1, W), ANY.Wood, U*8);
		OM.data(ST.make(BlocksGT.BeamCFireProof, 1, W), ANY.Wood, U*8);
		OM.data(ST.make(BlocksGT.BeamD         , 1, W), ANY.Wood, U*8);
		OM.data(ST.make(BlocksGT.BeamDFireProof, 1, W), ANY.Wood, U*8);
		});
	}
}

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
 */

package gregtech.loaders.a;

import gregapi.GT_API;

import net.minecraft.world.level.block.SoundType;
import static gregapi.data.CS.*;

import com.cricketcraft.chisel.api.carving.CarvingUtils;

import gregapi.block.behaviors.Drops;
import gregapi.block.behaviors.Drops_SmallOre;
import gregapi.block.metatype.BlockStones;
import gregapi.block.prefixblock.PrefixBlock;
import gregapi.block.prefixblock.PrefixBlock_;
import gregapi.code.ItemStackContainer;
import gregapi.code.ItemStackSet;
import gregapi.data.CS.BlocksGT;
import gregapi.data.CS.ItemsGT;
import gregapi.data.IL;
import gregapi.data.MD;
import gregapi.data.MT;
import gregapi.data.OP;
import gregapi.data.RM;
import gregapi.oredict.OreDictMaterial;
import gregapi.render.BlockTextureDefault;
import gregapi.util.CR;
import gregapi.util.ST;
import gregtech.blocks.stone.BlockStonesGT;
import net.minecraft.world.level.block.Block;
import gregapi.block.Material;
import team.chisel.carving.Carving;

public class Loader_Rocks implements Runnable {
	@Override
	@SuppressWarnings("unchecked")
	public void run() {
		final BlockStones[] tStoneH = new BlockStones[1];
		
		int n = 0;
		GT_API.registerBlockLazy(gregapi.data.CS.ModIDs.GT, "gt.stone.granite.black", () -> {BlockStonesGT s = new BlockStonesGT("gt.stone.granite.black", MT.STONES.GraniteBlack                                                                                                                , 6.00F, 3.00F,  3, T); BlocksGT.stones[0]=s; BlocksGT.GraniteBlack=s; tStoneH[0]=s; return s;});
		GT_API.registerBlockLazy(MD.GT.mID, "gt.meta.ore.normal.blackgranite", () -> {gregapi.block.prefixblock.PrefixBlock_ o = new PrefixBlock_(MD.GT, "gt.meta.ore.normal.blackgranite", OP.oreBlackgranite        , null                                  , BlockTextureDefault.get(((BlockStonesGT)BlocksGT.stones[0]).mIcons[0]), Material.rock, SoundType.STONE, TOOL_pickaxe  , 3.00F, 6.00F,  0, ((BlockStonesGT)BlocksGT.stones[0]).mHarvestLevel  , F,F, OreDictMaterial.MATERIAL_ARRAY); BlocksGT.ores_normal[0]=o; return (net.minecraft.world.level.block.Block)o;});
		GT_API.registerBlockLazy(MD.GT.mID, "gt.meta.ore.broken.blackgranite", () -> {gregapi.block.prefixblock.PrefixBlock_ o = new PrefixBlock_(MD.GT, "gt.meta.ore.broken.blackgranite", OP.oreBlackgranite        , null                                  , BlockTextureDefault.get(((BlockStonesGT)BlocksGT.stones[0]).mIcons[1]), Material.rock, SoundType.STONE, TOOL_pickaxe  , 1.50F, 3.00F, -1, ((BlockStonesGT)BlocksGT.stones[0]).mHarvestLevel-1, T,F, OreDictMaterial.MATERIAL_ARRAY); BlocksGT.ores_broken[0]=o; return (net.minecraft.world.level.block.Block)o;});
		GT_API.registerBlockLazy(MD.GT.mID, "gt.meta.ore.small.blackgranite", () -> {gregapi.block.prefixblock.PrefixBlock_ o = new PrefixBlock_(MD.GT, "gt.meta.ore.small.blackgranite", OP.oreSmall               , new Drops_SmallOre(((BlockStonesGT)BlocksGT.stones[0]).mMaterial)  , BlockTextureDefault.get(((BlockStonesGT)BlocksGT.stones[0]).mIcons[0]), Material.rock, SoundType.STONE, TOOL_pickaxe  , 3.00F, 6.00F, -1, ((BlockStonesGT)BlocksGT.stones[0]).mHarvestLevel  , F,F, OreDictMaterial.MATERIAL_ARRAY); BlocksGT.ores_small[0]=o; return (net.minecraft.world.level.block.Block)o;});
		GT_API.registerBlockLazy(gregapi.data.CS.ModIDs.GT, "gt.stone.granite.red", () -> {BlockStonesGT s = new BlockStonesGT("gt.stone.granite.red", MT.STONES.GraniteRed                                                                                                                  , 6.00F, 3.00F,  3, T); BlocksGT.stones[1]=s; BlocksGT.GraniteRed=s; tStoneH[0]=s; return s;});
		GT_API.registerBlockLazy(MD.GT.mID, "gt.meta.ore.normal.redgranite", () -> {gregapi.block.prefixblock.PrefixBlock_ o = new PrefixBlock_(MD.GT, "gt.meta.ore.normal.redgranite", OP.oreRedgranite          , null                                  , BlockTextureDefault.get(((BlockStonesGT)BlocksGT.stones[1]).mIcons[0]), Material.rock, SoundType.STONE, TOOL_pickaxe  , 3.00F, 6.00F,  0, ((BlockStonesGT)BlocksGT.stones[1]).mHarvestLevel  , F,F, OreDictMaterial.MATERIAL_ARRAY); BlocksGT.ores_normal[1]=o; return (net.minecraft.world.level.block.Block)o;});
		GT_API.registerBlockLazy(MD.GT.mID, "gt.meta.ore.broken.redgranite", () -> {gregapi.block.prefixblock.PrefixBlock_ o = new PrefixBlock_(MD.GT, "gt.meta.ore.broken.redgranite", OP.oreRedgranite          , null                                  , BlockTextureDefault.get(((BlockStonesGT)BlocksGT.stones[1]).mIcons[1]), Material.rock, SoundType.STONE, TOOL_pickaxe  , 1.50F, 3.00F, -1, ((BlockStonesGT)BlocksGT.stones[1]).mHarvestLevel-1, T,F, OreDictMaterial.MATERIAL_ARRAY); BlocksGT.ores_broken[1]=o; return (net.minecraft.world.level.block.Block)o;});
		GT_API.registerBlockLazy(MD.GT.mID, "gt.meta.ore.small.redgranite", () -> {gregapi.block.prefixblock.PrefixBlock_ o = new PrefixBlock_(MD.GT, "gt.meta.ore.small.redgranite", OP.oreSmall               , new Drops_SmallOre(((BlockStonesGT)BlocksGT.stones[1]).mMaterial)  , BlockTextureDefault.get(((BlockStonesGT)BlocksGT.stones[1]).mIcons[0]), Material.rock, SoundType.STONE, TOOL_pickaxe  , 3.00F, 6.00F, -1, ((BlockStonesGT)BlocksGT.stones[1]).mHarvestLevel  , F,F, OreDictMaterial.MATERIAL_ARRAY); BlocksGT.ores_small[1]=o; return (net.minecraft.world.level.block.Block)o;});
		GT_API.registerBlockLazy(gregapi.data.CS.ModIDs.GT, "gt.stone.basalt", () -> {BlockStonesGT s = new BlockStonesGT("gt.stone.basalt", MT.STONES.Basalt                                                                                                                      , 3.00F, 2.00F,  2, F); BlocksGT.stones[2]=s; BlocksGT.Basalt=s; tStoneH[0]=s; return s;});
		GT_API.registerBlockLazy(MD.GT.mID, "gt.meta.ore.normal.basalt", () -> {gregapi.block.prefixblock.PrefixBlock_ o = new PrefixBlock_(MD.GT, "gt.meta.ore.normal.basalt", OP.oreBasalt              , null                                  , BlockTextureDefault.get(((BlockStonesGT)BlocksGT.stones[2]).mIcons[0]), Material.rock, SoundType.STONE, TOOL_pickaxe  , 2.00F, 3.00F,  0, ((BlockStonesGT)BlocksGT.stones[2]).mHarvestLevel  , F,F, OreDictMaterial.MATERIAL_ARRAY); BlocksGT.ores_normal[2]=o; return (net.minecraft.world.level.block.Block)o;});
		GT_API.registerBlockLazy(MD.GT.mID, "gt.meta.ore.broken.basalt", () -> {gregapi.block.prefixblock.PrefixBlock_ o = new PrefixBlock_(MD.GT, "gt.meta.ore.broken.basalt", OP.oreBasalt              , null                                  , BlockTextureDefault.get(((BlockStonesGT)BlocksGT.stones[2]).mIcons[1]), Material.rock, SoundType.STONE, TOOL_pickaxe  , 1.00F, 1.50F, -1, ((BlockStonesGT)BlocksGT.stones[2]).mHarvestLevel-1, T,F, OreDictMaterial.MATERIAL_ARRAY); BlocksGT.ores_broken[2]=o; return (net.minecraft.world.level.block.Block)o;});
		GT_API.registerBlockLazy(MD.GT.mID, "gt.meta.ore.small.basalt", () -> {gregapi.block.prefixblock.PrefixBlock_ o = new PrefixBlock_(MD.GT, "gt.meta.ore.small.basalt", OP.oreSmall               , new Drops_SmallOre(((BlockStonesGT)BlocksGT.stones[2]).mMaterial)  , BlockTextureDefault.get(((BlockStonesGT)BlocksGT.stones[2]).mIcons[0]), Material.rock, SoundType.STONE, TOOL_pickaxe  , 2.00F, 3.00F, -1, ((BlockStonesGT)BlocksGT.stones[2]).mHarvestLevel  , F,F, OreDictMaterial.MATERIAL_ARRAY); BlocksGT.ores_small[2]=o; return (net.minecraft.world.level.block.Block)o;});
		GT_API.registerBlockLazy(gregapi.data.CS.ModIDs.GT, "gt.stone.marble", () -> {BlockStonesGT s = new BlockStonesGT("gt.stone.marble", MT.STONES.Marble                                                                                                                      , 0.75F, 0.50F,  0, F); BlocksGT.stones[3]=s; BlocksGT.Marble=s; tStoneH[0]=s; return s;});
		GT_API.registerBlockLazy(MD.GT.mID, "gt.meta.ore.normal.marble", () -> {gregapi.block.prefixblock.PrefixBlock_ o = new PrefixBlock_(MD.GT, "gt.meta.ore.normal.marble", OP.oreMarble              , null                                  , BlockTextureDefault.get(((BlockStonesGT)BlocksGT.stones[3]).mIcons[0]), Material.rock, SoundType.STONE, TOOL_pickaxe  , 0.50F, 0.75F,  0, ((BlockStonesGT)BlocksGT.stones[3]).mHarvestLevel  , F,F, OreDictMaterial.MATERIAL_ARRAY); BlocksGT.ores_normal[3]=o; return (net.minecraft.world.level.block.Block)o;});
		GT_API.registerBlockLazy(MD.GT.mID, "gt.meta.ore.broken.marble", () -> {gregapi.block.prefixblock.PrefixBlock_ o = new PrefixBlock_(MD.GT, "gt.meta.ore.broken.marble", OP.oreMarble              , null                                  , BlockTextureDefault.get(((BlockStonesGT)BlocksGT.stones[3]).mIcons[1]), Material.rock, SoundType.STONE, TOOL_pickaxe  , 0.25F, 0.37F, -1, ((BlockStonesGT)BlocksGT.stones[3]).mHarvestLevel-1, T,F, OreDictMaterial.MATERIAL_ARRAY); BlocksGT.ores_broken[3]=o; return (net.minecraft.world.level.block.Block)o;});
		GT_API.registerBlockLazy(MD.GT.mID, "gt.meta.ore.small.marble", () -> {gregapi.block.prefixblock.PrefixBlock_ o = new PrefixBlock_(MD.GT, "gt.meta.ore.small.marble", OP.oreSmall               , new Drops_SmallOre(((BlockStonesGT)BlocksGT.stones[3]).mMaterial)  , BlockTextureDefault.get(((BlockStonesGT)BlocksGT.stones[3]).mIcons[0]), Material.rock, SoundType.STONE, TOOL_pickaxe  , 0.50F, 0.75F, -1, ((BlockStonesGT)BlocksGT.stones[3]).mHarvestLevel  , F,F, OreDictMaterial.MATERIAL_ARRAY); BlocksGT.ores_small[3]=o; return (net.minecraft.world.level.block.Block)o;});
		GT_API.registerBlockLazy(gregapi.data.CS.ModIDs.GT, "gt.stone.limestone", () -> {BlockStonesGT s = new BlockStonesGT("gt.stone.limestone", MT.STONES.Limestone                                                                                                                   , 0.75F, 0.50F,  0, F); BlocksGT.stones[4]=s; BlocksGT.Limestone=s; tStoneH[0]=s; return s;});
		GT_API.registerBlockLazy(MD.GT.mID, "gt.meta.ore.normal.limestone", () -> {gregapi.block.prefixblock.PrefixBlock_ o = new PrefixBlock_(MD.GT, "gt.meta.ore.normal.limestone", OP.oreLimestone           , null                                  , BlockTextureDefault.get(((BlockStonesGT)BlocksGT.stones[4]).mIcons[0]), Material.rock, SoundType.STONE, TOOL_pickaxe  , 0.50F, 0.75F,  0, ((BlockStonesGT)BlocksGT.stones[4]).mHarvestLevel  , F,F, OreDictMaterial.MATERIAL_ARRAY); BlocksGT.ores_normal[4]=o; return (net.minecraft.world.level.block.Block)o;});
		GT_API.registerBlockLazy(MD.GT.mID, "gt.meta.ore.broken.limestone", () -> {gregapi.block.prefixblock.PrefixBlock_ o = new PrefixBlock_(MD.GT, "gt.meta.ore.broken.limestone", OP.oreLimestone           , null                                  , BlockTextureDefault.get(((BlockStonesGT)BlocksGT.stones[4]).mIcons[1]), Material.rock, SoundType.STONE, TOOL_pickaxe  , 0.25F, 0.37F, -1, ((BlockStonesGT)BlocksGT.stones[4]).mHarvestLevel-1, T,F, OreDictMaterial.MATERIAL_ARRAY); BlocksGT.ores_broken[4]=o; return (net.minecraft.world.level.block.Block)o;});
		GT_API.registerBlockLazy(MD.GT.mID, "gt.meta.ore.small.limestone", () -> {gregapi.block.prefixblock.PrefixBlock_ o = new PrefixBlock_(MD.GT, "gt.meta.ore.small.limestone", OP.oreSmall               , new Drops_SmallOre(((BlockStonesGT)BlocksGT.stones[4]).mMaterial)  , BlockTextureDefault.get(((BlockStonesGT)BlocksGT.stones[4]).mIcons[0]), Material.rock, SoundType.STONE, TOOL_pickaxe  , 0.50F, 0.75F, -1, ((BlockStonesGT)BlocksGT.stones[4]).mHarvestLevel  , F,F, OreDictMaterial.MATERIAL_ARRAY); BlocksGT.ores_small[4]=o; return (net.minecraft.world.level.block.Block)o;});
		GT_API.registerBlockLazy(gregapi.data.CS.ModIDs.GT, "gt.stone.granite", () -> {BlockStonesGT s = new BlockStonesGT("gt.stone.granite", MT.STONES.Granite                                                                                                                     , 2.00F, 1.00F,  1, F); BlocksGT.stones[5]=s; BlocksGT.Granite=s; tStoneH[0]=s; return s;});
		GT_API.registerBlockLazy(MD.GT.mID, "gt.meta.ore.normal.granite", () -> {gregapi.block.prefixblock.PrefixBlock_ o = new PrefixBlock_(MD.GT, "gt.meta.ore.normal.granite", OP.oreVanillagranite      , null                                  , BlockTextureDefault.get(((BlockStonesGT)BlocksGT.stones[5]).mIcons[0]), Material.rock, SoundType.STONE, TOOL_pickaxe  , 1.00F, 1.50F,  0, ((BlockStonesGT)BlocksGT.stones[5]).mHarvestLevel  , F,F, OreDictMaterial.MATERIAL_ARRAY); BlocksGT.ores_normal[5]=o; return (net.minecraft.world.level.block.Block)o;});
		GT_API.registerBlockLazy(MD.GT.mID, "gt.meta.ore.broken.granite", () -> {gregapi.block.prefixblock.PrefixBlock_ o = new PrefixBlock_(MD.GT, "gt.meta.ore.broken.granite", OP.oreVanillagranite      , null                                  , BlockTextureDefault.get(((BlockStonesGT)BlocksGT.stones[5]).mIcons[1]), Material.rock, SoundType.STONE, TOOL_pickaxe  , 0.50F, 0.75F, -1, ((BlockStonesGT)BlocksGT.stones[5]).mHarvestLevel-1, T,F, OreDictMaterial.MATERIAL_ARRAY); BlocksGT.ores_broken[5]=o; return (net.minecraft.world.level.block.Block)o;});
		GT_API.registerBlockLazy(MD.GT.mID, "gt.meta.ore.small.granite", () -> {gregapi.block.prefixblock.PrefixBlock_ o = new PrefixBlock_(MD.GT, "gt.meta.ore.small.granite", OP.oreSmall               , new Drops_SmallOre(((BlockStonesGT)BlocksGT.stones[5]).mMaterial)  , BlockTextureDefault.get(((BlockStonesGT)BlocksGT.stones[5]).mIcons[0]), Material.rock, SoundType.STONE, TOOL_pickaxe  , 1.00F, 1.50F, -1, ((BlockStonesGT)BlocksGT.stones[5]).mHarvestLevel  , F,F, OreDictMaterial.MATERIAL_ARRAY); BlocksGT.ores_small[5]=o; return (net.minecraft.world.level.block.Block)o;});
		GT_API.registerBlockLazy(gregapi.data.CS.ModIDs.GT, "gt.stone.diorite", () -> {BlockStonesGT s = new BlockStonesGT("gt.stone.diorite", MT.STONES.Diorite                                                                                                                     , 0.75F, 0.50F,  0, F); BlocksGT.stones[6]=s; BlocksGT.Diorite=s; tStoneH[0]=s; return s;});
		GT_API.registerBlockLazy(MD.GT.mID, "gt.meta.ore.normal.diorite", () -> {gregapi.block.prefixblock.PrefixBlock_ o = new PrefixBlock_(MD.GT, "gt.meta.ore.normal.diorite", OP.oreDiorite             , null                                  , BlockTextureDefault.get(((BlockStonesGT)BlocksGT.stones[6]).mIcons[0]), Material.rock, SoundType.STONE, TOOL_pickaxe  , 0.50F, 0.75F,  0, ((BlockStonesGT)BlocksGT.stones[6]).mHarvestLevel  , F,F, OreDictMaterial.MATERIAL_ARRAY); BlocksGT.ores_normal[6]=o; return (net.minecraft.world.level.block.Block)o;});
		GT_API.registerBlockLazy(MD.GT.mID, "gt.meta.ore.broken.diorite", () -> {gregapi.block.prefixblock.PrefixBlock_ o = new PrefixBlock_(MD.GT, "gt.meta.ore.broken.diorite", OP.oreDiorite             , null                                  , BlockTextureDefault.get(((BlockStonesGT)BlocksGT.stones[6]).mIcons[1]), Material.rock, SoundType.STONE, TOOL_pickaxe  , 0.25F, 0.37F, -1, ((BlockStonesGT)BlocksGT.stones[6]).mHarvestLevel-1, T,F, OreDictMaterial.MATERIAL_ARRAY); BlocksGT.ores_broken[6]=o; return (net.minecraft.world.level.block.Block)o;});
		GT_API.registerBlockLazy(MD.GT.mID, "gt.meta.ore.small.diorite", () -> {gregapi.block.prefixblock.PrefixBlock_ o = new PrefixBlock_(MD.GT, "gt.meta.ore.small.diorite", OP.oreSmall               , new Drops_SmallOre(((BlockStonesGT)BlocksGT.stones[6]).mMaterial)  , BlockTextureDefault.get(((BlockStonesGT)BlocksGT.stones[6]).mIcons[0]), Material.rock, SoundType.STONE, TOOL_pickaxe  , 0.50F, 0.75F, -1, ((BlockStonesGT)BlocksGT.stones[6]).mHarvestLevel  , F,F, OreDictMaterial.MATERIAL_ARRAY); BlocksGT.ores_small[6]=o; return (net.minecraft.world.level.block.Block)o;});
		GT_API.registerBlockLazy(gregapi.data.CS.ModIDs.GT, "gt.stone.andesite", () -> {BlockStonesGT s = new BlockStonesGT("gt.stone.andesite", MT.STONES.Andesite                                                                                                                    , 0.75F, 0.50F,  0, F); BlocksGT.stones[7]=s; BlocksGT.Andesite=s; tStoneH[0]=s; return s;});
		GT_API.registerBlockLazy(MD.GT.mID, "gt.meta.ore.normal.andesite", () -> {gregapi.block.prefixblock.PrefixBlock_ o = new PrefixBlock_(MD.GT, "gt.meta.ore.normal.andesite", OP.oreAndesite            , null                                  , BlockTextureDefault.get(((BlockStonesGT)BlocksGT.stones[7]).mIcons[0]), Material.rock, SoundType.STONE, TOOL_pickaxe  , 0.50F, 0.75F,  0, ((BlockStonesGT)BlocksGT.stones[7]).mHarvestLevel  , F,F, OreDictMaterial.MATERIAL_ARRAY); BlocksGT.ores_normal[7]=o; return (net.minecraft.world.level.block.Block)o;});
		GT_API.registerBlockLazy(MD.GT.mID, "gt.meta.ore.broken.andesite", () -> {gregapi.block.prefixblock.PrefixBlock_ o = new PrefixBlock_(MD.GT, "gt.meta.ore.broken.andesite", OP.oreAndesite            , null                                  , BlockTextureDefault.get(((BlockStonesGT)BlocksGT.stones[7]).mIcons[1]), Material.rock, SoundType.STONE, TOOL_pickaxe  , 0.25F, 0.37F, -1, ((BlockStonesGT)BlocksGT.stones[7]).mHarvestLevel-1, T,F, OreDictMaterial.MATERIAL_ARRAY); BlocksGT.ores_broken[7]=o; return (net.minecraft.world.level.block.Block)o;});
		GT_API.registerBlockLazy(MD.GT.mID, "gt.meta.ore.small.andesite", () -> {gregapi.block.prefixblock.PrefixBlock_ o = new PrefixBlock_(MD.GT, "gt.meta.ore.small.andesite", OP.oreSmall               , new Drops_SmallOre(((BlockStonesGT)BlocksGT.stones[7]).mMaterial)  , BlockTextureDefault.get(((BlockStonesGT)BlocksGT.stones[7]).mIcons[0]), Material.rock, SoundType.STONE, TOOL_pickaxe  , 0.50F, 0.75F, -1, ((BlockStonesGT)BlocksGT.stones[7]).mHarvestLevel  , F,F, OreDictMaterial.MATERIAL_ARRAY); BlocksGT.ores_small[7]=o; return (net.minecraft.world.level.block.Block)o;});
		GT_API.registerBlockLazy(gregapi.data.CS.ModIDs.GT, "gt.stone.komatiite", () -> {BlockStonesGT s = new BlockStonesGT("gt.stone.komatiite", MT.STONES.Komatiite                                                                                                                   , 3.00F, 2.00F,  2, F); BlocksGT.stones[8]=s; BlocksGT.Komatiite=s; tStoneH[0]=s; return s;});
		GT_API.registerBlockLazy(MD.GT.mID, "gt.meta.ore.normal.komatiite", () -> {gregapi.block.prefixblock.PrefixBlock_ o = new PrefixBlock_(MD.GT, "gt.meta.ore.normal.komatiite", OP.oreKomatiite           , null                                  , BlockTextureDefault.get(((BlockStonesGT)BlocksGT.stones[8]).mIcons[0]), Material.rock, SoundType.STONE, TOOL_pickaxe  , 2.00F, 3.00F,  0, ((BlockStonesGT)BlocksGT.stones[8]).mHarvestLevel  , F,F, OreDictMaterial.MATERIAL_ARRAY); BlocksGT.ores_normal[8]=o; return (net.minecraft.world.level.block.Block)o;});
		GT_API.registerBlockLazy(MD.GT.mID, "gt.meta.ore.broken.komatiite", () -> {gregapi.block.prefixblock.PrefixBlock_ o = new PrefixBlock_(MD.GT, "gt.meta.ore.broken.komatiite", OP.oreKomatiite           , null                                  , BlockTextureDefault.get(((BlockStonesGT)BlocksGT.stones[8]).mIcons[1]), Material.rock, SoundType.STONE, TOOL_pickaxe  , 1.00F, 1.50F, -1, ((BlockStonesGT)BlocksGT.stones[8]).mHarvestLevel-1, T,F, OreDictMaterial.MATERIAL_ARRAY); BlocksGT.ores_broken[8]=o; return (net.minecraft.world.level.block.Block)o;});
		GT_API.registerBlockLazy(MD.GT.mID, "gt.meta.ore.small.komatiite", () -> {gregapi.block.prefixblock.PrefixBlock_ o = new PrefixBlock_(MD.GT, "gt.meta.ore.small.komatiite", OP.oreSmall               , new Drops_SmallOre(((BlockStonesGT)BlocksGT.stones[8]).mMaterial)  , BlockTextureDefault.get(((BlockStonesGT)BlocksGT.stones[8]).mIcons[0]), Material.rock, SoundType.STONE, TOOL_pickaxe  , 2.00F, 3.00F, -1, ((BlockStonesGT)BlocksGT.stones[8]).mHarvestLevel  , F,F, OreDictMaterial.MATERIAL_ARRAY); BlocksGT.ores_small[8]=o; return (net.minecraft.world.level.block.Block)o;});
		GT_API.registerBlockLazy(gregapi.data.CS.ModIDs.GT, "gt.stone.greenschist", () -> {BlockStonesGT s = new BlockStonesGT("gt.stone.greenschist", MT.STONES.Greenschist                                                                                                                 , 0.75F, 0.50F,  0, F); BlocksGT.stones[9]=s; BlocksGT.SchistGreen=s; tStoneH[0]=s; return s;});
		GT_API.registerBlockLazy(MD.GT.mID, "gt.meta.ore.normal.greenschist", () -> {gregapi.block.prefixblock.PrefixBlock_ o = new PrefixBlock_(MD.GT, "gt.meta.ore.normal.greenschist", OP.oreGreenschist         , null                                  , BlockTextureDefault.get(((BlockStonesGT)BlocksGT.stones[9]).mIcons[0]), Material.rock, SoundType.STONE, TOOL_pickaxe  , 0.50F, 0.75F,  0, ((BlockStonesGT)BlocksGT.stones[9]).mHarvestLevel  , F,F, OreDictMaterial.MATERIAL_ARRAY); BlocksGT.ores_normal[9]=o; return (net.minecraft.world.level.block.Block)o;});
		GT_API.registerBlockLazy(MD.GT.mID, "gt.meta.ore.broken.greenschist", () -> {gregapi.block.prefixblock.PrefixBlock_ o = new PrefixBlock_(MD.GT, "gt.meta.ore.broken.greenschist", OP.oreGreenschist         , null                                  , BlockTextureDefault.get(((BlockStonesGT)BlocksGT.stones[9]).mIcons[1]), Material.rock, SoundType.STONE, TOOL_pickaxe  , 0.25F, 0.37F, -1, ((BlockStonesGT)BlocksGT.stones[9]).mHarvestLevel-1, T,F, OreDictMaterial.MATERIAL_ARRAY); BlocksGT.ores_broken[9]=o; return (net.minecraft.world.level.block.Block)o;});
		GT_API.registerBlockLazy(MD.GT.mID, "gt.meta.ore.small.greenschist", () -> {gregapi.block.prefixblock.PrefixBlock_ o = new PrefixBlock_(MD.GT, "gt.meta.ore.small.greenschist", OP.oreSmall               , new Drops_SmallOre(((BlockStonesGT)BlocksGT.stones[9]).mMaterial)  , BlockTextureDefault.get(((BlockStonesGT)BlocksGT.stones[9]).mIcons[0]), Material.rock, SoundType.STONE, TOOL_pickaxe  , 0.50F, 0.75F, -1, ((BlockStonesGT)BlocksGT.stones[9]).mHarvestLevel  , F,F, OreDictMaterial.MATERIAL_ARRAY); BlocksGT.ores_small[9]=o; return (net.minecraft.world.level.block.Block)o;});
		GT_API.registerBlockLazy(gregapi.data.CS.ModIDs.GT, "gt.stone.blueschist", () -> {BlockStonesGT s = new BlockStonesGT("gt.stone.blueschist", MT.STONES.Blueschist                                                                                                                  , 0.75F, 0.50F,  0, F); BlocksGT.stones[10]=s; BlocksGT.SchistBlue=s; tStoneH[0]=s; return s;});
		GT_API.registerBlockLazy(MD.GT.mID, "gt.meta.ore.normal.blueschist", () -> {gregapi.block.prefixblock.PrefixBlock_ o = new PrefixBlock_(MD.GT, "gt.meta.ore.normal.blueschist", OP.oreBlueschist          , null                                  , BlockTextureDefault.get(((BlockStonesGT)BlocksGT.stones[10]).mIcons[0]), Material.rock, SoundType.STONE, TOOL_pickaxe  , 0.50F, 0.75F,  0, ((BlockStonesGT)BlocksGT.stones[10]).mHarvestLevel  , F,F, OreDictMaterial.MATERIAL_ARRAY); BlocksGT.ores_normal[10]=o; return (net.minecraft.world.level.block.Block)o;});
		GT_API.registerBlockLazy(MD.GT.mID, "gt.meta.ore.broken.blueschist", () -> {gregapi.block.prefixblock.PrefixBlock_ o = new PrefixBlock_(MD.GT, "gt.meta.ore.broken.blueschist", OP.oreBlueschist          , null                                  , BlockTextureDefault.get(((BlockStonesGT)BlocksGT.stones[10]).mIcons[1]), Material.rock, SoundType.STONE, TOOL_pickaxe  , 0.25F, 0.37F, -1, ((BlockStonesGT)BlocksGT.stones[10]).mHarvestLevel-1, T,F, OreDictMaterial.MATERIAL_ARRAY); BlocksGT.ores_broken[10]=o; return (net.minecraft.world.level.block.Block)o;});
		GT_API.registerBlockLazy(MD.GT.mID, "gt.meta.ore.small.blueschist", () -> {gregapi.block.prefixblock.PrefixBlock_ o = new PrefixBlock_(MD.GT, "gt.meta.ore.small.blueschist", OP.oreSmall               , new Drops_SmallOre(((BlockStonesGT)BlocksGT.stones[10]).mMaterial)  , BlockTextureDefault.get(((BlockStonesGT)BlocksGT.stones[10]).mIcons[0]), Material.rock, SoundType.STONE, TOOL_pickaxe  , 0.50F, 0.75F, -1, ((BlockStonesGT)BlocksGT.stones[10]).mHarvestLevel  , F,F, OreDictMaterial.MATERIAL_ARRAY); BlocksGT.ores_small[10]=o; return (net.minecraft.world.level.block.Block)o;});
		GT_API.registerBlockLazy(gregapi.data.CS.ModIDs.GT, "gt.stone.kimberlite", () -> {BlockStonesGT s = new BlockStonesGT("gt.stone.kimberlite", MT.STONES.Kimberlite                                                                                                                  , 3.00F, 2.00F,  2, F); BlocksGT.stones[11]=s; BlocksGT.Kimberlite=s; tStoneH[0]=s; return s;});
		GT_API.registerBlockLazy(MD.GT.mID, "gt.meta.ore.normal.kimberlite", () -> {gregapi.block.prefixblock.PrefixBlock_ o = new PrefixBlock_(MD.GT, "gt.meta.ore.normal.kimberlite", OP.oreKimberlite          , null                                  , BlockTextureDefault.get(((BlockStonesGT)BlocksGT.stones[11]).mIcons[0]), Material.rock, SoundType.STONE, TOOL_pickaxe  , 2.00F, 3.00F,  0, ((BlockStonesGT)BlocksGT.stones[11]).mHarvestLevel  , F,F, OreDictMaterial.MATERIAL_ARRAY); BlocksGT.ores_normal[11]=o; return (net.minecraft.world.level.block.Block)o;});
		GT_API.registerBlockLazy(MD.GT.mID, "gt.meta.ore.broken.kimberlite", () -> {gregapi.block.prefixblock.PrefixBlock_ o = new PrefixBlock_(MD.GT, "gt.meta.ore.broken.kimberlite", OP.oreKimberlite          , null                                  , BlockTextureDefault.get(((BlockStonesGT)BlocksGT.stones[11]).mIcons[1]), Material.rock, SoundType.STONE, TOOL_pickaxe  , 1.00F, 1.50F, -1, ((BlockStonesGT)BlocksGT.stones[11]).mHarvestLevel-1, T,F, OreDictMaterial.MATERIAL_ARRAY); BlocksGT.ores_broken[11]=o; return (net.minecraft.world.level.block.Block)o;});
		GT_API.registerBlockLazy(MD.GT.mID, "gt.meta.ore.small.kimberlite", () -> {gregapi.block.prefixblock.PrefixBlock_ o = new PrefixBlock_(MD.GT, "gt.meta.ore.small.kimberlite", OP.oreSmall               , new Drops_SmallOre(((BlockStonesGT)BlocksGT.stones[11]).mMaterial)  , BlockTextureDefault.get(((BlockStonesGT)BlocksGT.stones[11]).mIcons[0]), Material.rock, SoundType.STONE, TOOL_pickaxe  , 2.00F, 3.00F, -1, ((BlockStonesGT)BlocksGT.stones[11]).mHarvestLevel  , F,F, OreDictMaterial.MATERIAL_ARRAY); BlocksGT.ores_small[11]=o; return (net.minecraft.world.level.block.Block)o;});
		GT_API.registerBlockLazy(gregapi.data.CS.ModIDs.GT, "gt.stone.quartzite", () -> {BlockStonesGT s = new BlockStonesGT("gt.stone.quartzite", MT.STONES.Quartzite                                                                                                                   , 0.75F, 0.50F,  0, F); BlocksGT.stones[12]=s; BlocksGT.Quartzite=s; tStoneH[0]=s; return s;});
		GT_API.registerBlockLazy(MD.GT.mID, "gt.meta.ore.normal.quartzite", () -> {gregapi.block.prefixblock.PrefixBlock_ o = new PrefixBlock_(MD.GT, "gt.meta.ore.normal.quartzite", OP.oreQuartzite           , null                                  , BlockTextureDefault.get(((BlockStonesGT)BlocksGT.stones[12]).mIcons[0]), Material.rock, SoundType.STONE, TOOL_pickaxe  , 0.50F, 0.75F,  0, ((BlockStonesGT)BlocksGT.stones[12]).mHarvestLevel  , F,F, OreDictMaterial.MATERIAL_ARRAY); BlocksGT.ores_normal[12]=o; return (net.minecraft.world.level.block.Block)o;});
		GT_API.registerBlockLazy(MD.GT.mID, "gt.meta.ore.broken.quartzite", () -> {gregapi.block.prefixblock.PrefixBlock_ o = new PrefixBlock_(MD.GT, "gt.meta.ore.broken.quartzite", OP.oreQuartzite           , null                                  , BlockTextureDefault.get(((BlockStonesGT)BlocksGT.stones[12]).mIcons[1]), Material.rock, SoundType.STONE, TOOL_pickaxe  , 0.25F, 0.37F, -1, ((BlockStonesGT)BlocksGT.stones[12]).mHarvestLevel-1, T,F, OreDictMaterial.MATERIAL_ARRAY); BlocksGT.ores_broken[12]=o; return (net.minecraft.world.level.block.Block)o;});
		GT_API.registerBlockLazy(MD.GT.mID, "gt.meta.ore.small.quartzite", () -> {gregapi.block.prefixblock.PrefixBlock_ o = new PrefixBlock_(MD.GT, "gt.meta.ore.small.quartzite", OP.oreSmall               , new Drops_SmallOre(((BlockStonesGT)BlocksGT.stones[12]).mMaterial)  , BlockTextureDefault.get(((BlockStonesGT)BlocksGT.stones[12]).mIcons[0]), Material.rock, SoundType.STONE, TOOL_pickaxe  , 0.50F, 0.75F, -1, ((BlockStonesGT)BlocksGT.stones[12]).mHarvestLevel  , F,F, OreDictMaterial.MATERIAL_ARRAY); BlocksGT.ores_small[12]=o; return (net.minecraft.world.level.block.Block)o;});
		GT_API.registerBlockLazy(gregapi.data.CS.ModIDs.GT, "gt.stone.prismarine.light", () -> {BlockStonesGT s = new BlockStonesGT("gt.stone.prismarine.light", MT.PrismarineLight                                                                                                                    , 0.75F, 0.50F,  0, F); BlocksGT.stones[13]=s; BlocksGT.PrismarineLight=s; tStoneH[0]=s; return s;});
		GT_API.registerBlockLazy(MD.GT.mID, "gt.meta.ore.normal.prismarine.light", () -> {gregapi.block.prefixblock.PrefixBlock_ o = new PrefixBlock_(MD.GT, "gt.meta.ore.normal.prismarine.light", OP.oreLightprismarine     , null                                  , BlockTextureDefault.get(((BlockStonesGT)BlocksGT.stones[13]).mIcons[0]), Material.rock, SoundType.STONE, TOOL_pickaxe  , 0.50F, 0.75F,  0, ((BlockStonesGT)BlocksGT.stones[13]).mHarvestLevel  , F,F, OreDictMaterial.MATERIAL_ARRAY); BlocksGT.ores_normal[13]=o; return (net.minecraft.world.level.block.Block)o;});
		GT_API.registerBlockLazy(MD.GT.mID, "gt.meta.ore.broken.prismarine.light", () -> {gregapi.block.prefixblock.PrefixBlock_ o = new PrefixBlock_(MD.GT, "gt.meta.ore.broken.prismarine.light", OP.oreLightprismarine     , null                                  , BlockTextureDefault.get(((BlockStonesGT)BlocksGT.stones[13]).mIcons[1]), Material.rock, SoundType.STONE, TOOL_pickaxe  , 0.25F, 0.37F, -1, ((BlockStonesGT)BlocksGT.stones[13]).mHarvestLevel-1, T,F, OreDictMaterial.MATERIAL_ARRAY); BlocksGT.ores_broken[13]=o; return (net.minecraft.world.level.block.Block)o;});
		GT_API.registerBlockLazy(MD.GT.mID, "gt.meta.ore.small.prismarine.light", () -> {gregapi.block.prefixblock.PrefixBlock_ o = new PrefixBlock_(MD.GT, "gt.meta.ore.small.prismarine.light", OP.oreSmall               , new Drops_SmallOre(((BlockStonesGT)BlocksGT.stones[13]).mMaterial)  , BlockTextureDefault.get(((BlockStonesGT)BlocksGT.stones[13]).mIcons[0]), Material.rock, SoundType.STONE, TOOL_pickaxe  , 0.50F, 0.75F, -1, ((BlockStonesGT)BlocksGT.stones[13]).mHarvestLevel  , F,F, OreDictMaterial.MATERIAL_ARRAY); BlocksGT.ores_small[13]=o; return (net.minecraft.world.level.block.Block)o;});
		GT_API.registerBlockLazy(gregapi.data.CS.ModIDs.GT, "gt.stone.prismarine.dark", () -> {BlockStonesGT s = new BlockStonesGT("gt.stone.prismarine.dark", MT.PrismarineDark                                                                                                                     , 0.75F, 0.50F,  1, F); BlocksGT.stones[14]=s; BlocksGT.PrismarineDark=s; tStoneH[0]=s; return s;});
		GT_API.registerBlockLazy(MD.GT.mID, "gt.meta.ore.normal.prismarine.dark", () -> {gregapi.block.prefixblock.PrefixBlock_ o = new PrefixBlock_(MD.GT, "gt.meta.ore.normal.prismarine.dark", OP.oreDarkprismarine      , null                                  , BlockTextureDefault.get(((BlockStonesGT)BlocksGT.stones[14]).mIcons[0]), Material.rock, SoundType.STONE, TOOL_pickaxe  , 0.50F, 0.75F,  0, ((BlockStonesGT)BlocksGT.stones[14]).mHarvestLevel  , F,F, OreDictMaterial.MATERIAL_ARRAY); BlocksGT.ores_normal[14]=o; return (net.minecraft.world.level.block.Block)o;});
		GT_API.registerBlockLazy(MD.GT.mID, "gt.meta.ore.broken.prismarine.dark", () -> {gregapi.block.prefixblock.PrefixBlock_ o = new PrefixBlock_(MD.GT, "gt.meta.ore.broken.prismarine.dark", OP.oreDarkprismarine      , null                                  , BlockTextureDefault.get(((BlockStonesGT)BlocksGT.stones[14]).mIcons[1]), Material.rock, SoundType.STONE, TOOL_pickaxe  , 0.25F, 0.37F, -1, ((BlockStonesGT)BlocksGT.stones[14]).mHarvestLevel-1, T,F, OreDictMaterial.MATERIAL_ARRAY); BlocksGT.ores_broken[14]=o; return (net.minecraft.world.level.block.Block)o;});
		GT_API.registerBlockLazy(MD.GT.mID, "gt.meta.ore.small.prismarine.dark", () -> {gregapi.block.prefixblock.PrefixBlock_ o = new PrefixBlock_(MD.GT, "gt.meta.ore.small.prismarine.dark", OP.oreSmall               , new Drops_SmallOre(((BlockStonesGT)BlocksGT.stones[14]).mMaterial)  , BlockTextureDefault.get(((BlockStonesGT)BlocksGT.stones[14]).mIcons[0]), Material.rock, SoundType.STONE, TOOL_pickaxe  , 0.50F, 0.75F, -1, ((BlockStonesGT)BlocksGT.stones[14]).mHarvestLevel  , F,F, OreDictMaterial.MATERIAL_ARRAY); BlocksGT.ores_small[14]=o; return (net.minecraft.world.level.block.Block)o;});
		GT_API.registerBlockLazy(gregapi.data.CS.ModIDs.GT, "gt.stone.slate", () -> {BlockStonesGT s = new BlockStonesGT("gt.stone.slate", MT.STONES.Slate                                                                                                                       , 0.75F, 0.50F,  1, F); BlocksGT.stones[15]=s; BlocksGT.Slate=s; tStoneH[0]=s; return s;});
		GT_API.registerBlockLazy(MD.GT.mID, "gt.meta.ore.normal.slate", () -> {gregapi.block.prefixblock.PrefixBlock_ o = new PrefixBlock_(MD.GT, "gt.meta.ore.normal.slate", OP.oreSlate               , null                                  , BlockTextureDefault.get(((BlockStonesGT)BlocksGT.stones[15]).mIcons[0]), Material.rock, SoundType.STONE, TOOL_pickaxe  , 0.50F, 0.75F,  0, ((BlockStonesGT)BlocksGT.stones[15]).mHarvestLevel  , F,F, OreDictMaterial.MATERIAL_ARRAY); BlocksGT.ores_normal[15]=o; return (net.minecraft.world.level.block.Block)o;});
		GT_API.registerBlockLazy(MD.GT.mID, "gt.meta.ore.broken.slate", () -> {gregapi.block.prefixblock.PrefixBlock_ o = new PrefixBlock_(MD.GT, "gt.meta.ore.broken.slate", OP.oreSlate               , null                                  , BlockTextureDefault.get(((BlockStonesGT)BlocksGT.stones[15]).mIcons[1]), Material.rock, SoundType.STONE, TOOL_pickaxe  , 0.25F, 0.37F, -1, ((BlockStonesGT)BlocksGT.stones[15]).mHarvestLevel-1, T,F, OreDictMaterial.MATERIAL_ARRAY); BlocksGT.ores_broken[15]=o; return (net.minecraft.world.level.block.Block)o;});
		GT_API.registerBlockLazy(MD.GT.mID, "gt.meta.ore.small.slate", () -> {gregapi.block.prefixblock.PrefixBlock_ o = new PrefixBlock_(MD.GT, "gt.meta.ore.small.slate", OP.oreSmall               , new Drops_SmallOre(((BlockStonesGT)BlocksGT.stones[15]).mMaterial)  , BlockTextureDefault.get(((BlockStonesGT)BlocksGT.stones[15]).mIcons[0]), Material.rock, SoundType.STONE, TOOL_pickaxe  , 0.50F, 0.75F, -1, ((BlockStonesGT)BlocksGT.stones[15]).mHarvestLevel  , F,F, OreDictMaterial.MATERIAL_ARRAY); BlocksGT.ores_small[15]=o; return (net.minecraft.world.level.block.Block)o;});
		GT_API.registerBlockLazy(gregapi.data.CS.ModIDs.GT, "gt.stone.shale", () -> {BlockStonesGT s = new BlockStonesGT("gt.stone.shale", MT.STONES.Shale                                                                                                                       , 0.75F, 0.50F,  0, F); BlocksGT.stones[16]=s; BlocksGT.Shale=s; tStoneH[0]=s; return s;});
		GT_API.registerBlockLazy(MD.GT.mID, "gt.meta.ore.normal.shale", () -> {gregapi.block.prefixblock.PrefixBlock_ o = new PrefixBlock_(MD.GT, "gt.meta.ore.normal.shale", OP.oreShale               , null                                  , BlockTextureDefault.get(((BlockStonesGT)BlocksGT.stones[16]).mIcons[0]), Material.rock, SoundType.STONE, TOOL_pickaxe  , 0.50F, 0.75F,  0, ((BlockStonesGT)BlocksGT.stones[16]).mHarvestLevel  , F,F, OreDictMaterial.MATERIAL_ARRAY); BlocksGT.ores_normal[16]=o; return (net.minecraft.world.level.block.Block)o;});
		GT_API.registerBlockLazy(MD.GT.mID, "gt.meta.ore.broken.shale", () -> {gregapi.block.prefixblock.PrefixBlock_ o = new PrefixBlock_(MD.GT, "gt.meta.ore.broken.shale", OP.oreShale               , null                                  , BlockTextureDefault.get(((BlockStonesGT)BlocksGT.stones[16]).mIcons[1]), Material.rock, SoundType.STONE, TOOL_pickaxe  , 0.25F, 0.37F, -1, ((BlockStonesGT)BlocksGT.stones[16]).mHarvestLevel-1, T,F, OreDictMaterial.MATERIAL_ARRAY); BlocksGT.ores_broken[16]=o; return (net.minecraft.world.level.block.Block)o;});
		GT_API.registerBlockLazy(MD.GT.mID, "gt.meta.ore.small.shale", () -> {gregapi.block.prefixblock.PrefixBlock_ o = new PrefixBlock_(MD.GT, "gt.meta.ore.small.shale", OP.oreSmall               , new Drops_SmallOre(((BlockStonesGT)BlocksGT.stones[16]).mMaterial)  , BlockTextureDefault.get(((BlockStonesGT)BlocksGT.stones[16]).mIcons[0]), Material.rock, SoundType.STONE, TOOL_pickaxe  , 0.50F, 0.75F, -1, ((BlockStonesGT)BlocksGT.stones[16]).mHarvestLevel  , F,F, OreDictMaterial.MATERIAL_ARRAY); BlocksGT.ores_small[16]=o; return (net.minecraft.world.level.block.Block)o;});
		// F12-followup (block-split): пост-регистрационная настройка (mDrops/stoneToOres/silk/generify) использует
		// сконструированные блоки (RegisterEvent) + зарегистрированные prefix-items + стеки (ST.make) → отложена на server-start.
		gregapi.GT_API.deferItemInit(() -> {
		for (int i = 0; i < BlocksGT.stones.length; i++) {
			VISUALLY_OPAQUE_BLOCKS.add(BlocksGT.stones[i]);
			((PrefixBlock)BlocksGT.ores_normal[i]).mDrops = new Drops(BlocksGT.ores_broken[i], BlocksGT.ores_normal[i], OP.oreRaw.mRegisteredPrefixItems.get(0), 0, Math.max(1, BlocksGT.stones[16].getHarvestLevel(0)));
			BlocksGT.stoneToNormalOres.put(new ItemStackContainer(BlocksGT.stones[i], 1, 0), BlocksGT.ores_normal[i]);
			BlocksGT.stoneToBrokenOres.put(new ItemStackContainer(BlocksGT.stones[i], 1, 0), BlocksGT.ores_broken[i]);
			BlocksGT.stoneToSmallOres .put(new ItemStackContainer(BlocksGT.stones[i], 1, 0), BlocksGT.ores_small [i]);
			BlocksGT.stoneOverridable.add(BlocksGT.ores_normal[i]); BlocksGT.drillableDynamite.add(BlocksGT.ores_normal[i]);
			BlocksGT.stoneOverridable.add(BlocksGT.ores_broken[i]); BlocksGT.drillableDynamite.add(BlocksGT.ores_broken[i]);
			BlocksGT.stoneOverridable.add(BlocksGT.ores_small [i]); BlocksGT.drillableDynamite.add(BlocksGT.ores_small [i]);
		}
		
		RM.generify(IL.CHSL_Granite             .get(1), ST.make(BlocksGT.stones[ 5], 1, 0));
		RM.generify(IL.CHSL_Diorite             .get(1), ST.make(BlocksGT.stones[ 6], 1, 0));
		RM.generify(IL.CHSL_Andesite            .get(1), ST.make(BlocksGT.stones[ 7], 1, 0));
		RM.generify(IL.CHSL_Granite_Smooth      .get(1), ST.make(BlocksGT.stones[ 5], 1, 7));
		RM.generify(IL.CHSL_Diorite_Smooth      .get(1), ST.make(BlocksGT.stones[ 6], 1, 7));
		RM.generify(IL.CHSL_Andesite_Smooth     .get(1), ST.make(BlocksGT.stones[ 7], 1, 7));
		RM.generify(IL.EtFu_Granite             .get(1), ST.make(BlocksGT.stones[ 5], 1, 0));
		RM.generify(IL.EtFu_Diorite             .get(1), ST.make(BlocksGT.stones[ 6], 1, 0));
		RM.generify(IL.EtFu_Andesite            .get(1), ST.make(BlocksGT.stones[ 7], 1, 0));
		RM.generify(IL.EtFu_Granite_Smooth      .get(1), ST.make(BlocksGT.stones[ 5], 1, 7));
		RM.generify(IL.EtFu_Diorite_Smooth      .get(1), ST.make(BlocksGT.stones[ 6], 1, 7));
		RM.generify(IL.EtFu_Andesite_Smooth     .get(1), ST.make(BlocksGT.stones[ 7], 1, 7));
		RM.generify(IL.GaSu_Granite             .get(1), ST.make(BlocksGT.stones[ 5], 1, 0));
		RM.generify(IL.GaSu_Diorite             .get(1), ST.make(BlocksGT.stones[ 6], 1, 0));
		RM.generify(IL.GaSu_Andesite            .get(1), ST.make(BlocksGT.stones[ 7], 1, 0));
		RM.generify(IL.GaSu_Granite_Smooth      .get(1), ST.make(BlocksGT.stones[ 5], 1, 7));
		RM.generify(IL.GaSu_Diorite_Smooth      .get(1), ST.make(BlocksGT.stones[ 6], 1, 7));
		RM.generify(IL.GaSu_Andesite_Smooth     .get(1), ST.make(BlocksGT.stones[ 7], 1, 7));
		RM.generify(IL.BOTA_Granite             .get(1), ST.make(BlocksGT.stones[ 5], 1, 0));
		RM.generify(IL.BOTA_Diorite             .get(1), ST.make(BlocksGT.stones[ 6], 1, 0));
		RM.generify(IL.BOTA_Andesite            .get(1), ST.make(BlocksGT.stones[ 7], 1, 0));
		RM.generify(IL.BOTA_Granite_Smooth      .get(1), ST.make(BlocksGT.stones[ 5], 1, 7));
		RM.generify(IL.BOTA_Diorite_Smooth      .get(1), ST.make(BlocksGT.stones[ 6], 1, 7));
		RM.generify(IL.BOTA_Andesite_Smooth     .get(1), ST.make(BlocksGT.stones[ 7], 1, 7));
		RM.generify(IL.BOTA_Granite_Bricks      .get(1), ST.make(BlocksGT.stones[ 5], 1, 3));
		RM.generify(IL.BOTA_Diorite_Bricks      .get(1), ST.make(BlocksGT.stones[ 6], 1, 3));
		RM.generify(IL.BOTA_Andesite_Bricks     .get(1), ST.make(BlocksGT.stones[ 7], 1, 3));
		RM.generify(IL.BOTA_Granite_Chiseled    .get(1), ST.make(BlocksGT.stones[ 5], 1, 6));
		RM.generify(IL.BOTA_Diorite_Chiseled    .get(1), ST.make(BlocksGT.stones[ 6], 1, 6));
		RM.generify(IL.BOTA_Andesite_Chiseled   .get(1), ST.make(BlocksGT.stones[ 7], 1, 6));
		RM.generify(IL.BOTA_Prismarine          .get(1), ST.make(BlocksGT.stones[13], 1, 0));
		RM.generify(IL.BOTA_Prismarine_Bricks   .get(1), ST.make(BlocksGT.stones[13], 1, 3));
		RM.generify(IL.BOTA_Prismarine_Dark     .get(1), ST.make(BlocksGT.stones[14], 1,11));
		
		ItemsGT.addNEIRedirects(ST.make(BlocksGT.stones[ 5], 1, 0), IL.CHSL_Granite.get(1), IL.GaSu_Granite.get(1), IL.EtFu_Granite.get(1), IL.BOTA_Granite.get(1));
		ItemsGT.addNEIRedirects(ST.make(BlocksGT.stones[ 6], 1, 0), IL.CHSL_Diorite.get(1), IL.GaSu_Diorite.get(1), IL.EtFu_Diorite.get(1), IL.BOTA_Diorite.get(1));
		ItemsGT.addNEIRedirects(ST.make(BlocksGT.stones[ 7], 1, 0), IL.CHSL_Andesite.get(1), IL.GaSu_Andesite.get(1), IL.EtFu_Andesite.get(1), IL.BOTA_Andesite.get(1));
		ItemsGT.addNEIRedirects(ST.make(BlocksGT.stones[ 5], 1, 7), IL.CHSL_Granite_Smooth.get(1), IL.GaSu_Granite_Smooth.get(1), IL.EtFu_Granite_Smooth.get(1), IL.BOTA_Granite_Smooth.get(1));
		ItemsGT.addNEIRedirects(ST.make(BlocksGT.stones[ 6], 1, 7), IL.CHSL_Diorite_Smooth.get(1), IL.GaSu_Diorite_Smooth.get(1), IL.EtFu_Diorite_Smooth.get(1), IL.BOTA_Diorite_Smooth.get(1));
		ItemsGT.addNEIRedirects(ST.make(BlocksGT.stones[ 7], 1, 7), IL.CHSL_Andesite_Smooth.get(1), IL.GaSu_Andesite_Smooth.get(1), IL.EtFu_Andesite_Smooth.get(1), IL.BOTA_Andesite_Smooth.get(1));
		ItemsGT.addNEIRedirects(ST.make(BlocksGT.stones[ 5], 1, 3), IL.BOTA_Granite_Bricks.get(1));
		ItemsGT.addNEIRedirects(ST.make(BlocksGT.stones[ 6], 1, 3), IL.BOTA_Diorite_Bricks.get(1));
		ItemsGT.addNEIRedirects(ST.make(BlocksGT.stones[ 7], 1, 3), IL.BOTA_Andesite_Bricks.get(1));
		ItemsGT.addNEIRedirects(ST.make(BlocksGT.stones[ 5], 1, 6), IL.BOTA_Granite_Chiseled.get(1));
		ItemsGT.addNEIRedirects(ST.make(BlocksGT.stones[ 6], 1, 6), IL.BOTA_Diorite_Chiseled.get(1));
		ItemsGT.addNEIRedirects(ST.make(BlocksGT.stones[ 7], 1, 6), IL.BOTA_Andesite_Chiseled.get(1));
		ItemsGT.addNEIRedirects(ST.make(BlocksGT.stones[13], 1, 0), IL.BOTA_Prismarine.get(1));
		ItemsGT.addNEIRedirects(ST.make(BlocksGT.stones[13], 1, 3), IL.BOTA_Prismarine_Bricks.get(1));
		ItemsGT.addNEIRedirects(ST.make(BlocksGT.stones[14], 1,11), IL.BOTA_Prismarine_Dark.get(1));
		
		((BlockStones)BlocksGT.stones[ 5]).mEqualBlocks[ 0].add(IL.CHSL_Granite           .get(1));
		((BlockStones)BlocksGT.stones[ 6]).mEqualBlocks[ 0].add(IL.CHSL_Diorite           .get(1));
		((BlockStones)BlocksGT.stones[ 7]).mEqualBlocks[ 0].add(IL.CHSL_Andesite          .get(1));
		((BlockStones)BlocksGT.stones[ 5]).mEqualBlocks[ 0].add(IL.EtFu_Granite           .get(1));
		((BlockStones)BlocksGT.stones[ 6]).mEqualBlocks[ 0].add(IL.EtFu_Diorite           .get(1));
		((BlockStones)BlocksGT.stones[ 7]).mEqualBlocks[ 0].add(IL.EtFu_Andesite          .get(1));
		((BlockStones)BlocksGT.stones[ 5]).mEqualBlocks[ 0].add(IL.GaSu_Granite           .get(1));
		((BlockStones)BlocksGT.stones[ 6]).mEqualBlocks[ 0].add(IL.GaSu_Diorite           .get(1));
		((BlockStones)BlocksGT.stones[ 7]).mEqualBlocks[ 0].add(IL.GaSu_Andesite          .get(1));
		((BlockStones)BlocksGT.stones[ 5]).mEqualBlocks[ 0].add(IL.BOTA_Granite           .get(1));
		((BlockStones)BlocksGT.stones[ 6]).mEqualBlocks[ 0].add(IL.BOTA_Diorite           .get(1));
		((BlockStones)BlocksGT.stones[ 7]).mEqualBlocks[ 0].add(IL.BOTA_Andesite          .get(1));
		((BlockStones)BlocksGT.stones[ 5]).mEqualBlocks[ 7].add(IL.CHSL_Granite_Smooth    .get(1));
		((BlockStones)BlocksGT.stones[ 6]).mEqualBlocks[ 7].add(IL.CHSL_Diorite_Smooth    .get(1));
		((BlockStones)BlocksGT.stones[ 7]).mEqualBlocks[ 7].add(IL.CHSL_Andesite_Smooth   .get(1));
		((BlockStones)BlocksGT.stones[ 5]).mEqualBlocks[ 7].add(IL.EtFu_Granite_Smooth    .get(1));
		((BlockStones)BlocksGT.stones[ 6]).mEqualBlocks[ 7].add(IL.EtFu_Diorite_Smooth    .get(1));
		((BlockStones)BlocksGT.stones[ 7]).mEqualBlocks[ 7].add(IL.EtFu_Andesite_Smooth   .get(1));
		((BlockStones)BlocksGT.stones[ 5]).mEqualBlocks[ 7].add(IL.GaSu_Granite_Smooth    .get(1));
		((BlockStones)BlocksGT.stones[ 6]).mEqualBlocks[ 7].add(IL.GaSu_Diorite_Smooth    .get(1));
		((BlockStones)BlocksGT.stones[ 7]).mEqualBlocks[ 7].add(IL.GaSu_Andesite_Smooth   .get(1));
		((BlockStones)BlocksGT.stones[ 5]).mEqualBlocks[ 7].add(IL.BOTA_Granite_Smooth    .get(1));
		((BlockStones)BlocksGT.stones[ 6]).mEqualBlocks[ 7].add(IL.BOTA_Diorite_Smooth    .get(1));
		((BlockStones)BlocksGT.stones[ 7]).mEqualBlocks[ 7].add(IL.BOTA_Andesite_Smooth   .get(1));
		((BlockStones)BlocksGT.stones[ 5]).mEqualBlocks[ 3].add(IL.BOTA_Granite_Bricks    .get(1));
		((BlockStones)BlocksGT.stones[ 6]).mEqualBlocks[ 3].add(IL.BOTA_Diorite_Bricks    .get(1));
		((BlockStones)BlocksGT.stones[ 7]).mEqualBlocks[ 3].add(IL.BOTA_Andesite_Bricks   .get(1));
		((BlockStones)BlocksGT.stones[ 5]).mEqualBlocks[ 6].add(IL.BOTA_Granite_Chiseled  .get(1));
		((BlockStones)BlocksGT.stones[ 6]).mEqualBlocks[ 6].add(IL.BOTA_Diorite_Chiseled  .get(1));
		((BlockStones)BlocksGT.stones[ 7]).mEqualBlocks[ 6].add(IL.BOTA_Andesite_Chiseled .get(1));
		((BlockStones)BlocksGT.stones[13]).mEqualBlocks[ 0].add(IL.BOTA_Prismarine        .get(1));
		((BlockStones)BlocksGT.stones[13]).mEqualBlocks[ 3].add(IL.BOTA_Prismarine_Bricks .get(1));
		((BlockStones)BlocksGT.stones[14]).mEqualBlocks[11].add(IL.BOTA_Prismarine_Dark   .get(1));
		
		if (MD.CHSL.mLoaded) {
		CR.shapeless(IL.CHSL_Granite           .get(1), CR.DEF_NCC, new Object[] {ST.make(BlocksGT.stones[5], 1, 0)});
		CR.shapeless(IL.CHSL_Diorite           .get(1), CR.DEF_NCC, new Object[] {ST.make(BlocksGT.stones[6], 1, 0)});
		CR.shapeless(IL.CHSL_Andesite          .get(1), CR.DEF_NCC, new Object[] {ST.make(BlocksGT.stones[7], 1, 0)});
		CR.shapeless(IL.CHSL_Granite_Smooth    .get(1), CR.DEF_NCC, new Object[] {ST.make(BlocksGT.stones[5], 1, 7)});
		CR.shapeless(IL.CHSL_Diorite_Smooth    .get(1), CR.DEF_NCC, new Object[] {ST.make(BlocksGT.stones[6], 1, 7)});
		CR.shapeless(IL.CHSL_Andesite_Smooth   .get(1), CR.DEF_NCC, new Object[] {ST.make(BlocksGT.stones[7], 1, 7)});
		CR.shapeless(ST.make(BlocksGT.stones[5], 1, 0), CR.DEF_NCC, new Object[] {IL.CHSL_Granite           .get(1)});
		CR.shapeless(ST.make(BlocksGT.stones[6], 1, 0), CR.DEF_NCC, new Object[] {IL.CHSL_Diorite           .get(1)});
		CR.shapeless(ST.make(BlocksGT.stones[7], 1, 0), CR.DEF_NCC, new Object[] {IL.CHSL_Andesite          .get(1)});
		CR.shapeless(ST.make(BlocksGT.stones[5], 1, 7), CR.DEF_NCC, new Object[] {IL.CHSL_Granite_Smooth    .get(1)});
		CR.shapeless(ST.make(BlocksGT.stones[6], 1, 7), CR.DEF_NCC, new Object[] {IL.CHSL_Diorite_Smooth    .get(1)});
		CR.shapeless(ST.make(BlocksGT.stones[7], 1, 7), CR.DEF_NCC, new Object[] {IL.CHSL_Andesite_Smooth   .get(1)});
		
		try {
			for (int i = 0; i < 16; i++) if (BlockStones.JUSTSTONE[i]) {
				for (ItemStackContainer tStack : (ItemStackSet<ItemStackContainer>)(((BlockStones)BlocksGT.stones[ 5]).mEqualBlocks[i])) if (!MD.CHSL.owns(tStack.toStack()))
				Carving.chisel.getGroup(IL.CHSL_Granite .block(), 0).addVariation(CarvingUtils.getDefaultVariationFor(tStack.mBlock, tStack.mMetaData, 1111+i));
				for (ItemStackContainer tStack : (ItemStackSet<ItemStackContainer>)(((BlockStones)BlocksGT.stones[ 6]).mEqualBlocks[i])) if (!MD.CHSL.owns(tStack.toStack()))
				Carving.chisel.getGroup(IL.CHSL_Diorite .block(), 0).addVariation(CarvingUtils.getDefaultVariationFor(tStack.mBlock, tStack.mMetaData, 1111+i));
				for (ItemStackContainer tStack : (ItemStackSet<ItemStackContainer>)(((BlockStones)BlocksGT.stones[ 7]).mEqualBlocks[i])) if (!MD.CHSL.owns(tStack.toStack()))
				Carving.chisel.getGroup(IL.CHSL_Andesite.block(), 0).addVariation(CarvingUtils.getDefaultVariationFor(tStack.mBlock, tStack.mMetaData, 1111+i));
			}
		} catch(Throwable e) {
			e.printStackTrace(ERR);
		}
		
		}
		
		BlocksGT.blockToDrop.put(IL.CHSL_Granite            , ST.make(BlocksGT.Granite , 1, 1));
		BlocksGT.blockToDrop.put(IL.EtFu_Granite            , ST.make(BlocksGT.Granite , 1, 1));
		BlocksGT.blockToDrop.put(IL.GaSu_Granite            , ST.make(BlocksGT.Granite , 1, 1));
		BlocksGT.blockToDrop.put(IL.BOTA_Granite            , ST.make(BlocksGT.Granite , 1, 1));
		BlocksGT.blockToDrop.put(IL.CHSL_Granite_Smooth     , ST.make(BlocksGT.Granite , 1, 7));
		BlocksGT.blockToDrop.put(IL.EtFu_Granite_Smooth     , ST.make(BlocksGT.Granite , 1, 7));
		BlocksGT.blockToDrop.put(IL.GaSu_Granite_Smooth     , ST.make(BlocksGT.Granite , 1, 7));
		BlocksGT.blockToDrop.put(IL.BOTA_Granite_Smooth     , ST.make(BlocksGT.Granite , 1, 7));
		BlocksGT.blockToDrop.put(IL.BOTA_Granite_Bricks     , ST.make(BlocksGT.Granite , 1, 3));
		BlocksGT.blockToDrop.put(IL.BOTA_Granite_Chiseled   , ST.make(BlocksGT.Granite , 1, 6));
		BlocksGT.blockToDrop.put(IL.CHSL_Diorite            , ST.make(BlocksGT.Diorite , 1, 1));
		BlocksGT.blockToDrop.put(IL.EtFu_Diorite            , ST.make(BlocksGT.Diorite , 1, 1));
		BlocksGT.blockToDrop.put(IL.GaSu_Diorite            , ST.make(BlocksGT.Diorite , 1, 1));
		BlocksGT.blockToDrop.put(IL.BOTA_Diorite            , ST.make(BlocksGT.Diorite , 1, 1));
		BlocksGT.blockToDrop.put(IL.CHSL_Diorite_Smooth     , ST.make(BlocksGT.Diorite , 1, 7));
		BlocksGT.blockToDrop.put(IL.EtFu_Diorite_Smooth     , ST.make(BlocksGT.Diorite , 1, 7));
		BlocksGT.blockToDrop.put(IL.GaSu_Diorite_Smooth     , ST.make(BlocksGT.Diorite , 1, 7));
		BlocksGT.blockToDrop.put(IL.BOTA_Diorite_Smooth     , ST.make(BlocksGT.Diorite , 1, 7));
		BlocksGT.blockToDrop.put(IL.BOTA_Diorite_Bricks     , ST.make(BlocksGT.Diorite , 1, 3));
		BlocksGT.blockToDrop.put(IL.BOTA_Diorite_Chiseled   , ST.make(BlocksGT.Diorite , 1, 6));
		BlocksGT.blockToDrop.put(IL.CHSL_Andesite           , ST.make(BlocksGT.Andesite, 1, 1));
		BlocksGT.blockToDrop.put(IL.EtFu_Andesite           , ST.make(BlocksGT.Andesite, 1, 1));
		BlocksGT.blockToDrop.put(IL.GaSu_Andesite           , ST.make(BlocksGT.Andesite, 1, 1));
		BlocksGT.blockToDrop.put(IL.BOTA_Andesite           , ST.make(BlocksGT.Andesite, 1, 1));
		BlocksGT.blockToDrop.put(IL.CHSL_Andesite_Smooth    , ST.make(BlocksGT.Andesite, 1, 7));
		BlocksGT.blockToDrop.put(IL.EtFu_Andesite_Smooth    , ST.make(BlocksGT.Andesite, 1, 7));
		BlocksGT.blockToDrop.put(IL.GaSu_Andesite_Smooth    , ST.make(BlocksGT.Andesite, 1, 7));
		BlocksGT.blockToDrop.put(IL.BOTA_Andesite_Smooth    , ST.make(BlocksGT.Andesite, 1, 7));
		BlocksGT.blockToDrop.put(IL.BOTA_Andesite_Bricks    , ST.make(BlocksGT.Andesite, 1, 3));
		BlocksGT.blockToDrop.put(IL.BOTA_Andesite_Chiseled  , ST.make(BlocksGT.Andesite, 1, 6));
		
		BlocksGT.blockToSilk.put(IL.CHSL_Granite            , ST.make(BlocksGT.Granite , 1, 0));
		BlocksGT.blockToSilk.put(IL.EtFu_Granite            , ST.make(BlocksGT.Granite , 1, 0));
		BlocksGT.blockToSilk.put(IL.GaSu_Granite            , ST.make(BlocksGT.Granite , 1, 0));
		BlocksGT.blockToSilk.put(IL.BOTA_Granite            , ST.make(BlocksGT.Granite , 1, 0));
		BlocksGT.blockToSilk.put(IL.CHSL_Granite_Smooth     , ST.make(BlocksGT.Granite , 1, 7));
		BlocksGT.blockToSilk.put(IL.EtFu_Granite_Smooth     , ST.make(BlocksGT.Granite , 1, 7));
		BlocksGT.blockToSilk.put(IL.GaSu_Granite_Smooth     , ST.make(BlocksGT.Granite , 1, 7));
		BlocksGT.blockToSilk.put(IL.BOTA_Granite_Smooth     , ST.make(BlocksGT.Granite , 1, 7));
		BlocksGT.blockToSilk.put(IL.BOTA_Granite_Bricks     , ST.make(BlocksGT.Granite , 1, 3));
		BlocksGT.blockToSilk.put(IL.BOTA_Granite_Chiseled   , ST.make(BlocksGT.Granite , 1, 6));
		BlocksGT.blockToSilk.put(IL.CHSL_Diorite            , ST.make(BlocksGT.Diorite , 1, 0));
		BlocksGT.blockToSilk.put(IL.EtFu_Diorite            , ST.make(BlocksGT.Diorite , 1, 0));
		BlocksGT.blockToSilk.put(IL.GaSu_Diorite            , ST.make(BlocksGT.Diorite , 1, 0));
		BlocksGT.blockToSilk.put(IL.BOTA_Diorite            , ST.make(BlocksGT.Diorite , 1, 0));
		BlocksGT.blockToSilk.put(IL.CHSL_Diorite_Smooth     , ST.make(BlocksGT.Diorite , 1, 7));
		BlocksGT.blockToSilk.put(IL.EtFu_Diorite_Smooth     , ST.make(BlocksGT.Diorite , 1, 7));
		BlocksGT.blockToSilk.put(IL.GaSu_Diorite_Smooth     , ST.make(BlocksGT.Diorite , 1, 7));
		BlocksGT.blockToSilk.put(IL.BOTA_Diorite_Smooth     , ST.make(BlocksGT.Diorite , 1, 7));
		BlocksGT.blockToSilk.put(IL.BOTA_Diorite_Bricks     , ST.make(BlocksGT.Diorite , 1, 3));
		BlocksGT.blockToSilk.put(IL.BOTA_Diorite_Chiseled   , ST.make(BlocksGT.Diorite , 1, 6));
		BlocksGT.blockToSilk.put(IL.CHSL_Andesite           , ST.make(BlocksGT.Andesite, 1, 0));
		BlocksGT.blockToSilk.put(IL.EtFu_Andesite           , ST.make(BlocksGT.Andesite, 1, 0));
		BlocksGT.blockToSilk.put(IL.GaSu_Andesite           , ST.make(BlocksGT.Andesite, 1, 0));
		BlocksGT.blockToSilk.put(IL.BOTA_Andesite           , ST.make(BlocksGT.Andesite, 1, 0));
		BlocksGT.blockToSilk.put(IL.CHSL_Andesite_Smooth    , ST.make(BlocksGT.Andesite, 1, 7));
		BlocksGT.blockToSilk.put(IL.EtFu_Andesite_Smooth    , ST.make(BlocksGT.Andesite, 1, 7));
		BlocksGT.blockToSilk.put(IL.GaSu_Andesite_Smooth    , ST.make(BlocksGT.Andesite, 1, 7));
		BlocksGT.blockToSilk.put(IL.BOTA_Andesite_Smooth    , ST.make(BlocksGT.Andesite, 1, 7));
		BlocksGT.blockToSilk.put(IL.BOTA_Andesite_Bricks    , ST.make(BlocksGT.Andesite, 1, 3));
		BlocksGT.blockToSilk.put(IL.BOTA_Andesite_Chiseled  , ST.make(BlocksGT.Andesite, 1, 6));
		}); // конец отложенной пост-регистрационной настройки Loader_Rocks
	}
}

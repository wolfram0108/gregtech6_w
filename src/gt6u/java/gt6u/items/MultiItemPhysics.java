/**
 * Copyright (c) 2020 GregTech-6U Team
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
 * ported from GregTech6-Unofficial for Minecraft 1.7.10 / Forge to an addon for Minecraft 26.1.2 / NeoForge.
 */

package gt6u.items;

import gt6u.data.IL_GT6U;

import static gregapi.data.CS.*;

import gregapi.code.ItemStackContainer;
import gregapi.cover.covers.*;
import gregapi.data.*;
import gregapi.data.CS.BooksGT;
import gregapi.data.CS.ItemsGT;
import gregapi.item.CreativeTab;
import gregapi.item.multiitem.MultiItemRandom;
import gregapi.item.multiitem.behaviors.Behavior_Turn_Into;
import gregapi.oredict.OreDictItemData;
import gregapi.oredict.OreDictMaterial;
import gregapi.util.CR;
import gregapi.util.OM;
import gregapi.util.ST;
import gregtech.items.behaviors.Behavior_DataStorage;
import gregtech.items.behaviors.Behavior_DataStorage16;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import gt6mirror.net.minecraftforge.fluids.FluidContainerRegistry.FluidContainerData;

public class MultiItemPhysics extends MultiItemRandom{

    public MultiItemPhysics() {
        super(gt6u.GT6U_Main.MOD_ID, "gt.multiitem.physics");
        new CreativeTab(getUnlocalizedName(), "GregTech: Physics", this, (short)1008);
    }

    @Override
    public void addItems() {
        IL_GT6U.Proton.set(addItem(1000, "Proton", "A subatomic particle. Can be produced in particle collider."));
        IL_GT6U.Anti_Proton.set(addItem(1001, "Anti Proton", "A subatomic particle. Can be produced in particle collider."));
        IL_GT6U.Electron.set(addItem(1002, "Electron", "A subatomic particle. Can be produced in particle collider."));
        IL_GT6U.Positron.set(addItem(1003, "Positron (Anti Electron)", "A subatomic particle. Can be produced in particle collider."));
        IL_GT6U.Neutron.set(addItem(1004, "Neutron", "A subatomic particle. Can be produced in particle collider."));
        IL_GT6U.Neutrino.set(addItem(1005, "Neutrino", "A subatomic particle. Can be produced in particle collider."));
        IL_GT6U.Anti_Neutrino.set(addItem(1006, "Anti Neutrino", "A subatomic particle. Can be produced in particle collider."));
        IL_GT6U.Alpha_Particle.set(addItem(1007, "Alpha Particle", "The nucleus of helium. "));
        IL_GT6U.Higgs_Boson.set(addItem(1008, "Higgs-Boson", "A Standard Model particle. Origin of mass. "));
        IL_GT6U.Kerr_Blackhole.set(addItem(1009, "Kerr Blackhole", "An extremely rare tiny blackhole that can be manually produced in particle collider "));

        //No usage yet
    }

}

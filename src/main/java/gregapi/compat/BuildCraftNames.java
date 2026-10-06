/**
 * Copyright (c) 2026 wolfram0108
 *
 * Written in 2026 for the GregTech 6 NeoForge port
 * (https://github.com/wolfram0108/gregtech6_w). Not part of the original GregTech 6
 * by Gregorius Techneticies; distributed under the same licence as the work it extends.
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

package gregapi.compat;

import java.util.function.Consumer;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;

import gregapi.data.MD;

/** BuildCraft's table in ForeignNames: 7.1.23 item names (reference/mods/BuildCraft-7.1.23-1.7.10) onto the ids of
 *  BuildCraft (Unofficial) 2026.2.0 (reference/mods/BuildCraft-Unofficial-2026.2.0-br2), whose sub-mods share one mod id. */
public final class BuildCraftNames {
	private BuildCraftNames() {}

	public static final ForeignNames TABLE = new ForeignNames(MD.BC);
	/** Whether the pair has a carrier in the BuildCraft of this branch; callers skip their entry when not. */
	public static boolean has(String aName, long aMeta) {return TABLE.has(aName, aMeta);}
	/** BuildCraft's crude oil and light fuel as fluids: 26.1 registers heat tier 0 under the bare base name (BCEnergyFluids:212). */
	public static final String OIL = MD.BC_ENERGY.mID + ":oil", FUEL = MD.BC_ENERGY.mID + ":fuel_light";
	/** The heat exchanger, which the refinery chain needs and which has no 7.1.23 name to map from. */
	public static final String HEAT_EXCHANGER = "heat_exchange";

	private static final String
	  NO_CHIPSET = "BuildCraft 26.1 registers five chipsets only (BCSiliconItems: redstone, iron, gold, diamond, quartz); 7.1.23 ItemRedstoneChipset.Chipset also had PULSATING(4), COMP(6) and EMERALD(7)"
	, NO_BOARD   = "BuildCraft 26.1 has no redstone board item: no registration and no lang key for it (7.1.23 robotics ItemRedstoneBoard «redstone_board»)"
	;

	/** BuildCraft 26.1 keeps a painted pipe's colour in its {@code pipe_colour} component (BCTransportItems:36-37), 7.1.23 in meta. */
	@SuppressWarnings("unchecked")
	private static Consumer<ItemStack> colour(int aWool) {
		return aStack -> {
			DataComponentType<?> tType = BuiltInRegistries.DATA_COMPONENT_TYPE.getValue(Identifier.fromNamespaceAndPath(MD.BC.mID, "pipe_colour"));
			if (tType != null) aStack.set((DataComponentType<DyeColor>)tType, DyeColor.byId(aWool));
		};
	}

	static {
		// 7.1.23 BuildCraftCore «woodenGearItem» … «diamondGearItem», «wrenchItem», «mapLocation», «list»;
		// 26.1 BCCoreItems gear_wood … gear_diamond, wrench, map_location, list.
		TABLE.map("woodenGearItem" , "gear_wood");
		TABLE.map("stoneGearItem"  , "gear_stone");
		TABLE.map("ironGearItem"   , "gear_iron");
		TABLE.map("goldGearItem"   , "gear_gold");
		TABLE.map("diamondGearItem", "gear_diamond");
		TABLE.map("wrenchItem"     , "wrench");
		TABLE.map("mapLocation"    , "map_location");
		TABLE.map("list"           , "list");

		// 7.1.23 chipset meta = enum RED, IRON, GOLD, DIAMOND, PULSATING, QUARTZ, COMP, EMERALD (ItemRedstoneChipset:30-39);
		// 26.1 has one item per chipset. W takes the redstone one, which every other chipset is pressed from in GT6.
		TABLE.map ("redstoneChipset", 0, "chipset_redstone");
		TABLE.map ("redstoneChipset", 1, "chipset_iron");
		TABLE.map ("redstoneChipset", 2, "chipset_gold");
		TABLE.map ("redstoneChipset", 3, "chipset_diamond");
		TABLE.gone("redstoneChipset", 4, NO_CHIPSET);
		TABLE.map ("redstoneChipset", 5, "chipset_quartz");
		TABLE.gone("redstoneChipset", 6, NO_CHIPSET);
		TABLE.gone("redstoneChipset", 7, NO_CHIPSET);
		TABLE.map ("redstoneChipset", gregapi.data.CS.W, "chipset_redstone");

		// Transport: 7.1.23 «pipeWaterproof» («Pipe Sealant»), «pipePlug», «gateCopier», the void pipes;
		// 26.1 waterproof («Pipe Sealant»), plug_blocker («Pipe Plug»), gate_copier, pipe_void_item / pipe_void_fluid.
		TABLE.map("pipeWaterproof"                       , "waterproof");
		TABLE.map("pipePlug"                             , "plug_blocker");
		TABLE.map("gateCopier"                           , "gate_copier");
		TABLE.map("item.buildcraftPipe.pipeitemsvoid"    , "pipe_void_item");
		TABLE.map("item.buildcraftPipe.pipefluidsvoid"   , "pipe_void_fluid");

		// Pipe wire: 7.1.23 PipeWire enum RED, BLUE, GREEN, YELLOW (PipeWire.java:18) is the item meta;
		// 26.1 registers one item per colour, «wire_» + colour name (BCTransportItems:66).
		TABLE.map("pipeWire", 0, "wire_red");
		TABLE.map("pipeWire", 1, "wire_blue");
		TABLE.map("pipeWire", 2, "wire_green");
		TABLE.map("pipeWire", 3, "wire_yellow");

		// 7.1.23 meta 0 is the unpainted pipe, 1..16 painted in wool colour meta-1 (ItemPipe:158); 26.1 pipe_cobble_item
		// and pipe_structure carry the paint in a component.
		TABLE.map("item.buildcraftPipe.pipeitemscobblestone"    , 0, "pipe_cobble_item");
		TABLE.map("item.buildcraftPipe.pipestructurecobblestone", 0, "pipe_structure");
		for (int i = 0; i < 16; i++) {
			TABLE.map("item.buildcraftPipe.pipeitemscobblestone"    , i + 1, "pipe_cobble_item", colour(i));
			TABLE.map("item.buildcraftPipe.pipestructurecobblestone", i + 1, "pipe_structure"  , colour(i));
		}
		TABLE.map("item.buildcraftPipe.pipeitemscobblestone"    , gregapi.data.CS.W, "pipe_cobble_item");
		TABLE.map("item.buildcraftPipe.pipestructurecobblestone", gregapi.data.CS.W, "pipe_structure");

		// Factory and builders: 7.1.23 «tankBlock», «autoWorkbenchBlock», «blueprintItem», «templateItem»;
		// 26.1 tank («Tank»), autoworkbench_item («Auto Workbench»), blueprint_clean / template_clean (the blank ones).
		TABLE.map("tankBlock"         , "tank");
		// 7.1.23 «refineryBlock» turned oil into fuel (BuildCraftFactory:163); in 26.1 the distiller does (BCFactoryBlocks:56).
		TABLE.map("refineryBlock"     , "distiller");
		TABLE.map("autoWorkbenchBlock", "autoworkbench_item");
		TABLE.map("blueprintItem"     , "blueprint_clean");
		TABLE.map("templateItem"      , "template_clean");

		TABLE.gone("redstone_board", NO_BOARD);
	}
}

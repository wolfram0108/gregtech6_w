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

import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;

import gregapi.data.MD;

/** BuildCraft's table in ForeignNames: 7.1.23 item names (reference/mods/BuildCraft-7.1.23-1.7.10) onto the ids of
 *  BuildCraft: Community Edition 7.99.25, whose sub-mods keep their own namespaces, so each path names its own. */
public final class BuildCraftNames {
	private BuildCraftNames() {}

	public static final ForeignNames TABLE = new ForeignNames(MD.BC, MD.BC_SILICON, MD.BC_TRANSPORT, MD.BC_FACTORY, MD.BC_ENERGY, MD.BC_BUILDERS, MD.BC_ROBOTICS);
	/** Whether the pair has a carrier in the BuildCraft of this branch; callers skip their entry when not. */
	public static boolean has(String aName, long aMeta) {return TABLE.has(aName, aMeta);}
	/** BuildCraft's crude oil as a fluid: 7.99 registers every heat tier with a «_heat_N» suffix (BCEnergyFluids.defineFluids). */
	public static final String OIL = MD.BC_ENERGY.mID + ":oil_heat_0";
	/** The heat exchanger, which the refinery chain needs and which has no 7.1.23 name to map from. */
	public static final String HEAT_EXCHANGER = "heat_exchange";

	private static final String
	  NO_CHIPSET = "BuildCraft 7.99 registers five chipsets only (BCSiliconItems:46-50: redstone, iron, gold, quartz, diamond); 7.1.23 ItemRedstoneChipset.Chipset also had PULSATING(4), COMP(6) and EMERALD(7)"
	;

	/** BuildCraft 7.99 keeps a wire's colour in the {@code colour} tag by dye name (ColourUtil:450-461), 7.1.23 in meta. */
	private static Consumer<ItemStack> colour(DyeColor aDye) {
		return aStack -> aStack.getOrCreateTag().putString("colour", aDye.getName());
	}

	static {
		// Gears and core items: 7.1.23 BuildCraftCore «woodenGearItem» … «diamondGearItem», «wrenchItem», «mapLocation»,
		// «list»; 7.99 BCCore tags item.gear.* → gear_wood … gear_diamond, wrench, map_location, list (BCCore:143-159).
		TABLE.map("woodenGearItem" , "buildcraftcore:gear_wood");
		TABLE.map("stoneGearItem"  , "buildcraftcore:gear_stone");
		TABLE.map("ironGearItem"   , "buildcraftcore:gear_iron");
		TABLE.map("goldGearItem"   , "buildcraftcore:gear_gold");
		TABLE.map("diamondGearItem", "buildcraftcore:gear_diamond");
		TABLE.map("wrenchItem"     , "buildcraftcore:wrench");
		TABLE.map("mapLocation"    , "buildcraftcore:map_location");
		TABLE.map("list"           , "buildcraftcore:list");

		// 7.1.23 chipset meta = enum RED, IRON, GOLD, DIAMOND, PULSATING, QUARTZ, COMP, EMERALD (ItemRedstoneChipset:30-39);
		// 7.99 has one item per chipset. W takes the redstone one, which every other chipset is pressed from in GT6.
		TABLE.map ("redstoneChipset", 0, "buildcraftsilicon:chipset_redstone");
		TABLE.map ("redstoneChipset", 1, "buildcraftsilicon:chipset_iron");
		TABLE.map ("redstoneChipset", 2, "buildcraftsilicon:chipset_gold");
		TABLE.map ("redstoneChipset", 3, "buildcraftsilicon:chipset_diamond");
		TABLE.gone("redstoneChipset", 4, NO_CHIPSET);
		TABLE.map ("redstoneChipset", 5, "buildcraftsilicon:chipset_quartz");
		TABLE.gone("redstoneChipset", 6, NO_CHIPSET);
		TABLE.gone("redstoneChipset", 7, NO_CHIPSET);
		TABLE.map ("redstoneChipset", gregapi.data.CS.W, "buildcraftsilicon:chipset_redstone");

		// 7.1.23 «pipeWaterproof», «pipePlug», «gateCopier», void pipes; 7.99 waterproof, plug_blocker, gate_copier, and every
		// pipe registered colourless plus 16 painted («_colorless» / «_<dye>», ItemPipeHolder:76-78).
		TABLE.map("pipeWaterproof"                       , "buildcrafttransport:waterproof");
		TABLE.map("pipePlug"                             , "buildcrafttransport:plug_blocker");
		TABLE.map("gateCopier"                           , "buildcraftsilicon:gate_copier");
		TABLE.map("item.buildcraftPipe.pipeitemsvoid"    , "buildcrafttransport:pipe_items_void_colorless");
		TABLE.map("item.buildcraftPipe.pipefluidsvoid"   , "buildcrafttransport:pipe_fluids_void_colorless");

		// Pipe wire: 7.1.23 PipeWire enum RED, BLUE, GREEN, YELLOW (PipeWire.java:18) is the item meta; 7.99 has one
		// «wire» item (BCTransportItems:152) coloured by its tag.
		TABLE.map("pipeWire", 0, "buildcrafttransport:wire", colour(DyeColor.RED));
		TABLE.map("pipeWire", 1, "buildcrafttransport:wire", colour(DyeColor.BLUE));
		TABLE.map("pipeWire", 2, "buildcrafttransport:wire", colour(DyeColor.GREEN));
		TABLE.map("pipeWire", 3, "buildcrafttransport:wire", colour(DyeColor.YELLOW));

		// Cobblestone transport and structure pipes: in 7.1.23 meta 0 is unpainted and meta 1..16 is the pipe painted
		// in wool colour (meta - 1) (ItemPipe.java:158 with ColorUtils WOOL_TO_NAME); 7.99 registers a separate item per paint.
		TABLE.map("item.buildcraftPipe.pipeitemscobblestone"    , 0, "buildcrafttransport:pipe_items_cobblestone_colorless");
		TABLE.map("item.buildcraftPipe.pipestructurecobblestone", 0, "buildcrafttransport:pipe_structure_cobblestone_colorless");
		for (int i = 0; i < 16; i++) {
			TABLE.map("item.buildcraftPipe.pipeitemscobblestone"    , i + 1, "buildcrafttransport:pipe_items_cobblestone_"     + DyeColor.byId(i).getName());
			TABLE.map("item.buildcraftPipe.pipestructurecobblestone", i + 1, "buildcrafttransport:pipe_structure_cobblestone_" + DyeColor.byId(i).getName());
		}
		TABLE.map("item.buildcraftPipe.pipeitemscobblestone"    , gregapi.data.CS.W, "buildcrafttransport:pipe_items_cobblestone_colorless");
		TABLE.map("item.buildcraftPipe.pipestructurecobblestone", gregapi.data.CS.W, "buildcrafttransport:pipe_structure_cobblestone_colorless");

		// 7.99 tank, autoworkbench_item, snapshot_blueprint/_template (blank until written); boards are one item per program,
		// the blank one board_robot_empty (RedstoneBoardRobotEmptyNBT:49).
		TABLE.map("tankBlock"         , "buildcraftfactory:tank");
		// 7.1.23 «refineryBlock» turned oil into fuel (BuildCraftFactory:163); in 7.99 the distiller does (BCFactoryBlocks:71).
		TABLE.map("refineryBlock"     , "buildcraftfactory:distiller");
		TABLE.map("autoWorkbenchBlock", "buildcraftfactory:autoworkbench_item");
		TABLE.map("blueprintItem"     , "buildcraftbuilders:snapshot_blueprint");
		TABLE.map("templateItem"      , "buildcraftbuilders:snapshot_template");
		TABLE.map("redstone_board"    , "buildcraftrobotics:board_robot_empty");
	}
}

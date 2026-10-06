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

/**
 * BuildCraft's table in the foreign item name centre ({@link ForeignNames}): BuildCraft 7.1.23 item names, the ones
 * GT6 1.7.10 addresses, onto the ids BuildCraft: Community Edition 7.99.25 registers. Its sub-mods keep their own mod
 * ids and namespaces (buildcraftcore, buildcraftsilicon, …), so the table answers for every {@code MD.BC*} and each
 * path names its namespace.
 *
 * <p>Left side: {@code reference/mods/BuildCraft-7.1.23-1.7.10} (registry names and meta enums). Right side:
 * {@code reference/mods/BuildCraft-CE-7.99.25-1.20.1} — the registering class, its registry tag
 * ({@code registerTag(...).reg(...)}) and the item model shipped in the jar.
 */
public final class BuildCraftNames {
	private BuildCraftNames() {}

	public static final ForeignNames TABLE = new ForeignNames(MD.BC, MD.BC_SILICON, MD.BC_TRANSPORT, MD.BC_FACTORY, MD.BC_ENERGY, MD.BC_BUILDERS, MD.BC_ROBOTICS);

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

		// Chipsets: 7.1.23 ItemRedstoneChipset meta = Chipset enum order RED, IRON, GOLD, DIAMOND, PULSATING, QUARTZ,
		// COMP, EMERALD (ItemRedstoneChipset.java:30-39); 7.99 has one item per chipset. W («any chipset») takes the
		// redstone one, the base every other chipset is pressed from in GT6's own recipes.
		TABLE.map ("redstoneChipset", 0, "buildcraftsilicon:chipset_redstone");
		TABLE.map ("redstoneChipset", 1, "buildcraftsilicon:chipset_iron");
		TABLE.map ("redstoneChipset", 2, "buildcraftsilicon:chipset_gold");
		TABLE.map ("redstoneChipset", 3, "buildcraftsilicon:chipset_diamond");
		TABLE.gone("redstoneChipset", 4, NO_CHIPSET);
		TABLE.map ("redstoneChipset", 5, "buildcraftsilicon:chipset_quartz");
		TABLE.gone("redstoneChipset", 6, NO_CHIPSET);
		TABLE.gone("redstoneChipset", 7, NO_CHIPSET);
		TABLE.map ("redstoneChipset", gregapi.data.CS.W, "buildcraftsilicon:chipset_redstone");

		// Transport: 7.1.23 «pipeWaterproof», «pipePlug», «gateCopier», the void pipes; 7.99 BCTransportItems:88 waterproof,
		// :145 plug_blocker, BCSiliconItems:54 gate_copier, and every pipe registered colourless plus 16 painted
		// («_colorless» / «_<dye>», ItemPipeHolder:76-78).
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

		// Factory, builders, robotics: 7.1.23 «tankBlock», «autoWorkbenchBlock», «blueprintItem», «templateItem»,
		// «redstone_board»; 7.99 BCFactory tank / autoworkbench_item, BCBuildersItems:24-25 snapshot_blueprint /
		// snapshot_template (blank until written), and BCRoboticsItems:39-42 one item per board program, the blank
		// one being board_robot_empty (RedstoneBoardRobotEmptyNBT:49).
		TABLE.map("tankBlock"         , "buildcraftfactory:tank");
		TABLE.map("autoWorkbenchBlock", "buildcraftfactory:autoworkbench_item");
		TABLE.map("blueprintItem"     , "buildcraftbuilders:snapshot_blueprint");
		TABLE.map("templateItem"      , "buildcraftbuilders:snapshot_template");
		TABLE.map("redstone_board"    , "buildcraftrobotics:board_robot_empty");
	}
}

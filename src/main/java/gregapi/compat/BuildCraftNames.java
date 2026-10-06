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

/**
 * BuildCraft's table in the foreign item name centre ({@link ForeignNames}): BuildCraft 7.1.23 item names, the ones
 * GT6 1.7.10 addresses, onto the ids BuildCraft (Unofficial) 2026.2.0 registers. All BuildCraft sub-mods share one
 * mod id there, so one table answers for {@code MD.BC} and every {@code MD.BC_*}.
 *
 * <p>Left side: {@code reference/mods/BuildCraft-7.1.23-1.7.10} (registry names and meta enums). Right side:
 * {@code reference/mods/BuildCraft-Unofficial-2026.2.0-br2} — the registering class and the item model shipped in the
 * jar ({@code assets/buildcraftunofficial/items/<id>.json}); human-readable names from {@code lang/en_us.json}.
 */
public final class BuildCraftNames {
	private BuildCraftNames() {}

	public static final ForeignNames TABLE = new ForeignNames(MD.BC);

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
		// Gears and core items: 7.1.23 BuildCraftCore registers «woodenGearItem» … «diamondGearItem», «wrenchItem»,
		// «mapLocation», «list»; 26.1 BCCoreItems registers gear_wood … gear_diamond («Wood Gear» … «Diamond Gear»),
		// wrench («Wrench»), map_location («Map Location»), list («List»).
		TABLE.map("woodenGearItem" , "gear_wood");
		TABLE.map("stoneGearItem"  , "gear_stone");
		TABLE.map("ironGearItem"   , "gear_iron");
		TABLE.map("goldGearItem"   , "gear_gold");
		TABLE.map("diamondGearItem", "gear_diamond");
		TABLE.map("wrenchItem"     , "wrench");
		TABLE.map("mapLocation"    , "map_location");
		TABLE.map("list"           , "list");

		// Chipsets: 7.1.23 ItemRedstoneChipset meta = Chipset enum order RED, IRON, GOLD, DIAMOND, PULSATING, QUARTZ,
		// COMP, EMERALD (ItemRedstoneChipset.java:30-39); 26.1 has one item per chipset. W («any chipset») takes the
		// redstone one, the base every other chipset is pressed from in GT6's own recipes.
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

		// Cobblestone transport and structure pipes: in 7.1.23 meta 0 is unpainted and meta 1..16 is the pipe painted
		// in wool colour (meta - 1) (ItemPipe.java:158 with ColorUtils WOOL_TO_NAME); 26.1 pipe_cobble_item
		// («Cobblestone Transport Pipe») and pipe_structure («Cobblestone Structure Pipe») carry the paint in a component.
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
		TABLE.map("autoWorkbenchBlock", "autoworkbench_item");
		TABLE.map("blueprintItem"     , "blueprint_clean");
		TABLE.map("templateItem"      , "template_clean");

		TABLE.gone("redstone_board", NO_BOARD);
	}
}

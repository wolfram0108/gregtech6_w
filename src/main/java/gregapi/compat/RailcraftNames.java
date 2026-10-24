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

import gregapi.data.MD;

/** Railcraft's table in ForeignNames: 1.7.10 item names onto Railcraft Reborn 1.4.6 ids (reference/mods/Railcraft-Reborn). */
public final class RailcraftNames {
	private RailcraftNames() {}

	public static final ForeignNames TABLE = new ForeignNames(MD.RC);

	private static final String
	  NO_DARK_ORE = "Railcraft Reborn has no abyssal diamond, emerald or lapis ore (1.7.10 ore metas 2, 3 and 4)"
	, NO_CELL = "Railcraft Reborn has no creosote cell, only creosote_bucket"
	, NO_CREOSOTE_CONTAINER = "Railcraft Reborn has no bottle, can, wax capsule or refractory capsule of creosote, only creosote_bucket"
	, NO_CONCRETE = "Railcraft Reborn has no concrete block (1.7.10 cube meta 1), only bag_of_cement"
	, NO_CREOSOTE_WOOD = "Railcraft Reborn has no creosote-soaked wood block (1.7.10 cube meta 8): creosote is only a fluid there"
	, NO_SHUNTING_WIRE = "Railcraft Reborn has no shunting wire (1.7.10 machine.delta meta 0)"
	, NO_ADMIN_ANCHOR = "Railcraft Reborn has no admin anchor (1.7.10 machine.alpha meta 4)"
	, NO_TRADE_STATION = "Railcraft Reborn has no trade station (1.7.10 machine.alpha meta 6)"
	, NO_STEAM_TRAP = "Railcraft Reborn has no steam trap, manual or automatic (1.7.10 machine.alpha metas 9 and 10)"
	, NO_PASSIVE_ANCHOR = "Railcraft Reborn has no passive anchor (1.7.10 machine.alpha meta 13)"
	, NO_STEAM_ENGINE = "Railcraft Reborn has no steam engine, hobby, low or high pressure (1.7.10 machine.beta metas 7, 8 and 9)"
	, NO_SENTINEL = "Railcraft Reborn has no sentinel (1.7.10 machine.beta meta 10)"
	, NO_METALS_CHEST = "Railcraft Reborn has no metals chest (1.7.10 machine.beta meta 12)"
	, NO_ELECTRIC_FEEDER = "Railcraft Reborn has no electric feeder (1.7.10 machine.epsilon meta 0)"
	, NO_ENGRAVING_BENCH = "Railcraft Reborn has no engraving bench (1.7.10 machine.epsilon meta 5)"
	, NO_POST_PLATFORM = "Railcraft Reborn has no platform item: a platform is a state of a post under a rail (1.7.10 post meta 6 and post.metal.platform)"
	, NO_MAGIC_CROWBAR = "Railcraft Reborn crowbars are iron, steel, diamond and seasons only; the Thaumcraft crowbars (tool.crowbar.magic and .void) have no counterpart"
	, NO_NOTEPAD = "Railcraft Reborn has no notepad"
	, NO_MAGNIFYING_GLASS = "Railcraft Reborn has no magnifying glass"
	, NO_EMBLEM = "Railcraft Reborn has no emblem item and no engraving bench"
	, NO_WOOD_TNT_CART = "Railcraft Reborn has no TNT cart of its own (1.7.10 cart.tnt.wood), only the vanilla one"
	, NO_BLEACHED_CLAY = "Railcraft Reborn has no bleached clay";

	/** The 16 colours in the 1.7.10 EnumColor order (id 0 black ... 15 white), which is also the GT6 dye order (CS.DYE_INDEX_*). */
	private static final String[] COLORS = {"black", "red", "green", "brown", "blue", "purple", "cyan", "light_gray", "gray", "pink", "lime", "yellow", "light_blue", "magenta", "orange", "white"};

	static {
		TABLE.map("cube", 0, "railcraft:coal_coke_block");
		TABLE.map("fuel.coke", "railcraft:coal_coke");
		TABLE.map("firestone.raw", "railcraft:raw_firestone");
		TABLE.map("ore", 0, "railcraft:sulfur_ore");
		TABLE.map("ore", 1, "railcraft:saltpeter_ore");
		TABLE.gone("ore", 2, NO_DARK_ORE);
		TABLE.gone("ore", 3, NO_DARK_ORE);
		TABLE.gone("ore", 4, NO_DARK_ORE);
		TABLE.map("ore", 5, "railcraft:firestone_ore");
		TABLE.map("part.plate", 0, "railcraft:iron_plate");
		TABLE.map("part.plate", 1, "railcraft:steel_plate");
		TABLE.map("part.plate", 2, "railcraft:tin_plate");
		TABLE.map("part.plate", 3, "railcraft:copper_plate");
		TABLE.map("part.plate", 4, "railcraft:lead_plate");
		TABLE.gone("fluid.creosote.cell", 1, NO_CELL);

		// LoaderItemList
		TABLE.gone("machine.delta", 0, NO_SHUNTING_WIRE);
		TABLE.map("frame", "railcraft:frame");
		// Reborn posts are always coloured; the post its rolling machine makes from iron is the black one, which stands for the unpainted post of 1.7.10.
		TABLE.map("post", 2, "railcraft:black_post");
		TABLE.gone("post", 6, NO_POST_PLATFORM);
		TABLE.gone("cube", 1, NO_CONCRETE);
		TABLE.map("cube", 4, "railcraft:crushed_obsidian");
		TABLE.map("cube", 6, "railcraft:abyssal_stone");
		TABLE.map("cube", 7, "railcraft:quarried_stone");
		TABLE.gone("cube", 8, NO_CREOSOTE_WOOD);
		TABLE.map("part.rail", 0, "railcraft:standard_rail");
		TABLE.map("part.rail", 1, "railcraft:advanced_rail");
		TABLE.map("part.rail", 2, "railcraft:wooden_rail");
		TABLE.map("part.rail", 3, "railcraft:high_speed_rail");
		TABLE.map("part.rail", 4, "railcraft:reinforced_rail");
		TABLE.map("part.rail", 5, "railcraft:electric_rail");
		TABLE.map("part.tie", 0, "railcraft:wooden_tie");
		TABLE.map("part.tie", 1, "railcraft:stone_tie");
		TABLE.map("part.railbed", 0, "railcraft:wooden_railbed");
		TABLE.map("part.railbed", 1, "railcraft:stone_railbed");
		TABLE.map("part.rebar", "railcraft:rebar");
		TABLE.map("firestone.cut", "railcraft:cut_firestone");
		TABLE.map("firestone.refined", "railcraft:refined_firestone");
		TABLE.map("firestone.cracked", "railcraft:cracked_firestone");
		TABLE.map("tool.crowbar", "railcraft:iron_crowbar");
		TABLE.map("tool.crowbar.reinforced", "railcraft:steel_crowbar");
		TABLE.gone("tool.crowbar.magic", NO_MAGIC_CROWBAR);
		TABLE.gone("tool.crowbar.void", NO_MAGIC_CROWBAR);
		TABLE.map("tool.steel.sword", "railcraft:steel_sword");
		TABLE.map("tool.steel.pickaxe", "railcraft:steel_pickaxe");
		TABLE.map("tool.steel.shovel", "railcraft:steel_shovel");
		TABLE.map("tool.steel.axe", "railcraft:steel_axe");
		TABLE.map("tool.steel.hoe", "railcraft:steel_hoe");
		TABLE.gone("fluid.creosote.bottle", NO_CREOSOTE_CONTAINER);
		TABLE.map("fluid.creosote.bucket", "railcraft:creosote_bucket");
		TABLE.gone("fluid.creosote.cell", 0, NO_CELL);
		TABLE.gone("fluid.creosote.can", NO_CREOSOTE_CONTAINER);
		TABLE.gone("fluid.creosote.wax", NO_CREOSOTE_CONTAINER);
		TABLE.gone("fluid.creosote.refactory", NO_CREOSOTE_CONTAINER);

		// LoaderItemData
		TABLE.map("anvil", 0, "railcraft:steel_anvil");
		TABLE.map("anvil", 1, "railcraft:chipped_steel_anvil");
		TABLE.map("anvil", 2, "railcraft:damaged_steel_anvil");
		TABLE.map("routing.table", "railcraft:routing_table_book");
		TABLE.map("part.gear", 0, "railcraft:gold_gear");
		TABLE.map("part.gear", 1, "railcraft:iron_gear");
		TABLE.map("part.gear", 2, "railcraft:steel_gear");
		TABLE.map("part.gear", 3, "railcraft:bushing_gear");

		// Compat_Recipes_Railcraft
		TABLE.map("part.circuit", 0, "railcraft:controller_circuit");
		TABLE.map("part.circuit", 1, "railcraft:receiver_circuit");
		TABLE.map("part.circuit", 2, "railcraft:signal_circuit");
		TABLE.gone("cart.tnt.wood", NO_WOOD_TNT_CART);
		TABLE.gone("tool.notepad", NO_NOTEPAD);
		TABLE.gone("tool.magnifying.glass", NO_MAGNIFYING_GLASS);
		// Reborn colours every tank wall, gauge and valve; white is the colour its plain recipes produce (iron_tank_wall.json and the like).
		TABLE.map("machine.beta", 0, "railcraft:white_iron_tank_wall");
		TABLE.map("machine.beta", 1, "railcraft:white_iron_tank_gauge");
		TABLE.map("machine.beta", 2, "railcraft:white_iron_tank_valve");
		TABLE.map("machine.beta", 3, "railcraft:low_pressure_steam_boiler_tank");
		TABLE.map("machine.beta", 4, "railcraft:high_pressure_steam_boiler_tank");
		TABLE.map("machine.beta", 5, "railcraft:solid_fueled_firebox");
		TABLE.map("machine.beta", 6, "railcraft:fluid_fueled_firebox");
		TABLE.gone("machine.beta", 7, NO_STEAM_ENGINE);
		TABLE.gone("machine.beta", 8, NO_STEAM_ENGINE);
		TABLE.gone("machine.beta", 9, NO_STEAM_ENGINE);
		TABLE.gone("machine.beta", 10, NO_SENTINEL);
		TABLE.map("machine.beta", 11, "railcraft:void_chest");
		TABLE.gone("machine.beta", 12, NO_METALS_CHEST);
		TABLE.map("machine.beta", 13, "railcraft:white_steel_tank_wall");
		TABLE.map("machine.beta", 14, "railcraft:white_steel_tank_gauge");
		TABLE.map("machine.beta", 15, "railcraft:white_steel_tank_valve");
		TABLE.map("machine.alpha", 0, "railcraft:world_spike");
		TABLE.map("machine.alpha", 1, "railcraft:steam_turbine");
		TABLE.map("machine.alpha", 2, "railcraft:personal_world_spike");
		TABLE.map("machine.alpha", 3, "railcraft:steam_oven");
		TABLE.gone("machine.alpha", 4, NO_ADMIN_ANCHOR);
		TABLE.map("machine.alpha", 5, "railcraft:chimney");
		TABLE.gone("machine.alpha", 6, NO_TRADE_STATION);
		TABLE.map("machine.alpha", 8, "railcraft:manual_rolling_machine");
		TABLE.gone("machine.alpha", 9, NO_STEAM_TRAP);
		TABLE.gone("machine.alpha", 10, NO_STEAM_TRAP);
		TABLE.map("machine.alpha", 11, "railcraft:feed_station");
		TABLE.gone("machine.alpha", 13, NO_PASSIVE_ANCHOR);
		TABLE.map("machine.alpha", 14, "railcraft:water_tank_siding");
		TABLE.map("machine.alpha", 15, "railcraft:crusher");
		TABLE.gone("machine.epsilon", 0, NO_ELECTRIC_FEEDER);
		TABLE.gone("machine.epsilon", 5, NO_ENGRAVING_BENCH);
		TABLE.map("tool.whistle.tuner", "railcraft:whistle_tuner");
		TABLE.map("part.turbine.blade", "railcraft:turbine_blade");
		TABLE.map("part.turbine.disk", "railcraft:turbine_disk");
		TABLE.map("part.turbine.rotor", "railcraft:turbine_rotor");
		TABLE.map("borehead.iron", "railcraft:iron_tunnel_bore_head");
		TABLE.map("borehead.steel", "railcraft:steel_tunnel_bore_head");
		TABLE.map("borehead.diamond", "railcraft:diamond_tunnel_bore_head");
		TABLE.map("cart.loco.steam.solid", "railcraft:steam_locomotive");
		TABLE.map("cart.loco.electric", "railcraft:electric_locomotive");
		TABLE.map("cart.bore", "railcraft:tunnel_bore");
		// 1.7.10 registers every cube as a custom stack named by its tag too (ModuleCore -> RailcraftRegistry.register(ItemStack)): this one is cube meta 4.
		TABLE.map("cube.crushed.obsidian", "railcraft:crushed_obsidian");
		TABLE.gone("part.bleached.clay", NO_BLEACHED_CLAY);
		// 1.7.10: ItemPostMetal names a stack by EnumColor.fromId(damage), ItemStrengthGlass by EnumColor.fromId(15 - damage).
		for (int i = 0; i < 16; i++) TABLE.map("glass", i, "railcraft:" + COLORS[15 - i] + "_strengthened_glass");
		for (int i = 0; i < 16; i++) TABLE.map("post.metal", i, "railcraft:" + COLORS[i] + "_post");
		TABLE.gone("post.metal.platform", NO_POST_PLATFORM);

		// LoaderBookList
		TABLE.map("routing.ticket", "railcraft:ticket");
		TABLE.map("routing.ticket.gold", "railcraft:golden_ticket");
		TABLE.gone("emblem", NO_EMBLEM);
		TABLE.map("tool.signal.label", "railcraft:signal_label");
	}

	/** The colour of a Reborn strengthened glass or post, as the 1.7.10 colour id (0 black … 15 white); -1 for anything else. */
	public static int colour(net.minecraft.world.item.ItemStack aStack) {
		if (aStack == null || aStack.isEmpty() || !MD.RC.mLoaded) return -1;
		net.minecraft.resources.Identifier tID = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(aStack.getItem());
		if (!MD.RC.mID.equals(tID.getNamespace())) return -1;
		String tPath = tID.getPath();
		String tName = tPath.endsWith("_post") ? tPath.substring(0, tPath.length() - 5) : tPath.endsWith("_strengthened_glass") ? tPath.substring(0, tPath.length() - 19) : null;
		for (int i = 0; tName != null && i < COLORS.length; i++) if (COLORS[i].equals(tName)) return i;
		return -1;
	}

	/** The same kind of item in another colour (a post stays a post, a glass a glass), or null. */
	public static net.minecraft.world.item.ItemStack recoloured(net.minecraft.world.item.ItemStack aStack, int aColour) {
		if (colour(aStack) < 0 || aColour < 0 || aColour >= COLORS.length) return null;
		String tPath = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(aStack.getItem()).getPath();
		String tKind = tPath.endsWith("_post") ? "_post" : "_strengthened_glass";
		net.minecraft.world.item.Item tItem = net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.fromNamespaceAndPath(MD.RC.mID, COLORS[aColour] + tKind));
		return tItem == null || tItem == net.minecraft.world.item.Items.AIR ? null : new net.minecraft.world.item.ItemStack(tItem);
	}

	/** The colour chlorine bleaches to: the glass is white, the post is the black one the rolling machine makes. */
	public static int bleached(net.minecraft.world.item.ItemStack aStack) {
		net.minecraft.resources.Identifier tID = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(aStack.getItem());
		return tID.getPath().endsWith("_post") ? 0 : 15;
	}
}

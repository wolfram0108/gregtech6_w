package gregapi.compat;

import gregapi.data.MD;

/** EFR 2.5.0 identifiers backed by modern vanilla items and blocks. */
public final class EFRNames {
	private EFRNames() {}
	public static final ForeignNames TABLE = new ForeignNames(MD.EtFu).carrier(MD.MC);
	static {
		TABLE.map("copper_block", 0, "minecraft:copper_block");
		TABLE.map("copper_block", 4, "minecraft:cut_copper");
		TABLE.map("copper_block", 8, "minecraft:waxed_copper_block");
		TABLE.map("copper_block", 12, "minecraft:waxed_cut_copper");
		TABLE.map("cut_copper_slab", 0, "minecraft:cut_copper_slab");
		TABLE.map("cut_copper_slab", 4, "minecraft:waxed_cut_copper_slab");
		TABLE.map("cut_copper_stairs", "minecraft:cut_copper_stairs");
		TABLE.map("waxed_cut_copper_stairs", "minecraft:waxed_cut_copper_stairs");
		TABLE.map("copper_block", 1, "minecraft:exposed_copper");
		TABLE.map("copper_block", 5, "minecraft:exposed_cut_copper");
		TABLE.map("copper_block", 9, "minecraft:waxed_exposed_copper");
		TABLE.map("copper_block", 13, "minecraft:waxed_exposed_cut_copper");
		TABLE.map("cut_copper_slab", 1, "minecraft:exposed_cut_copper_slab");
		TABLE.map("cut_copper_slab", 5, "minecraft:waxed_exposed_cut_copper_slab");
		TABLE.map("exposed_cut_copper_stairs", "minecraft:exposed_cut_copper_stairs");
		TABLE.map("waxed_exposed_cut_copper_stairs", "minecraft:waxed_exposed_cut_copper_stairs");
		TABLE.map("copper_block", 2, "minecraft:weathered_copper");
		TABLE.map("copper_block", 6, "minecraft:weathered_cut_copper");
		TABLE.map("copper_block", 10, "minecraft:waxed_weathered_copper");
		TABLE.map("copper_block", 14, "minecraft:waxed_weathered_cut_copper");
		TABLE.map("cut_copper_slab", 2, "minecraft:weathered_cut_copper_slab");
		TABLE.map("cut_copper_slab", 6, "minecraft:waxed_weathered_cut_copper_slab");
		TABLE.map("weathered_cut_copper_stairs", "minecraft:weathered_cut_copper_stairs");
		TABLE.map("waxed_weathered_cut_copper_stairs", "minecraft:waxed_weathered_cut_copper_stairs");
		TABLE.map("copper_block", 3, "minecraft:oxidized_copper");
		TABLE.map("copper_block", 7, "minecraft:oxidized_cut_copper");
		TABLE.map("copper_block", 11, "minecraft:waxed_oxidized_copper");
		TABLE.map("copper_block", 15, "minecraft:waxed_oxidized_cut_copper");
		TABLE.map("cut_copper_slab", 3, "minecraft:oxidized_cut_copper_slab");
		TABLE.map("cut_copper_slab", 7, "minecraft:waxed_oxidized_cut_copper_slab");
		TABLE.map("oxidized_cut_copper_stairs", "minecraft:oxidized_cut_copper_stairs");
		TABLE.map("waxed_oxidized_cut_copper_stairs", "minecraft:waxed_oxidized_cut_copper_stairs");
		TABLE.map("copper_ingot", "minecraft:copper_ingot");
		TABLE.map("copper_ore", "minecraft:copper_ore");
		TABLE.map("deepslate_copper_ore", "minecraft:deepslate_copper_ore");
		TABLE.map("raw_ore", 0, "minecraft:raw_copper");
		TABLE.map("raw_ore_block", 0, "minecraft:raw_copper_block");
		TABLE.map("raw_ore", 1, "minecraft:raw_iron");
		TABLE.map("raw_ore_block", 1, "minecraft:raw_iron_block");
		TABLE.map("raw_ore", 2, "minecraft:raw_gold");
		TABLE.map("raw_ore_block", 2, "minecraft:raw_gold_block");
		TABLE.map("stone", 0, "minecraft:stone");
		TABLE.map("stone", 1, "minecraft:granite");
		TABLE.map("stone", 2, "minecraft:polished_granite");
		TABLE.map("stone", 3, "minecraft:diorite");
		TABLE.map("stone", 4, "minecraft:polished_diorite");
		TABLE.map("stone", 5, "minecraft:andesite");
		TABLE.map("stone", 6, "minecraft:polished_andesite");
	}
}

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

package gregapi.oredict;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.common.MinecraftForge;

import gregapi.util.ST;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Replaces the removed Forge OreDictionary: only its flat name-to-stack storage was ever really Forge's (GT6 already
 *  owns the rich semantics above it), moved in unchanged, including the original's exact hash-bucket dedup rule. */
public class OreDictionary {
	private OreDictionary() {/* Static-only, like the Forge class it replaces. */}

	/** Forge's "any metadata" wildcard marker, same value as Short.MAX_VALUE. */
	public static final int WILDCARD_VALUE = Short.MAX_VALUE;

	/** Flat name-to-stack storage; LinkedHashMap keeps sweep order deterministic. The value type is ArrayList,
	 *  not just List, so the 1-arg getOres(name) can return that exact original type without a cast. */
	private static final Map<String, ArrayList<ItemStack>> sOres = new LinkedHashMap<>();
	private static final List<ItemStack> EMPTY = Collections.emptyList();

	/** Matches Forge's registerOreImpl 1:1: dedups an already-registered physical stack, then fires the registration event. */
	public static void registerOre(String aName, ItemStack aStack) {
		ArrayList<ItemStack> tList = sOres.computeIfAbsent(aName, k -> new ArrayList<>());
		for (ItemStack tExisting : tList) if (sameHashBucket(tExisting, aStack)) return;
		ItemStack tOre = aStack.copy();
		tList.add(tOre);
		MinecraftForge.EVENT_BUS.post(new OreRegisterEvent(aName, tOre));
	}

	/** Matches Forge's exact hash-bucket rule (meta only mixed in when it isn't the wildcard value), not the
	 *  general ST.equal, which treats a wildcard as matching any meta and would wrongly collapse the two buckets. */
	private static boolean sameHashBucket(ItemStack aExisting, ItemStack aStack) {
		return aExisting.getItem() == aStack.getItem() && ST.meta_(aExisting) == ST.meta_(aStack);
	}

	/** Forge's 1-arg getOres(name) auto-creates the entry and returns the live list; the return type matches
	 *  the original ArrayList<ItemStack> since some callers declare their variable with that exact type. */
	public static ArrayList<ItemStack> getOres(String aName) {
		return sOres.computeIfAbsent(aName, k -> new ArrayList<>());
	}

	/** Forge's 2-arg getOres: returns the live list, or a shared empty one when false and the entry is absent. */
	public static List<ItemStack> getOres(String aName, boolean aAlwaysCreateEntry) {
		if (aAlwaysCreateEntry) return sOres.computeIfAbsent(aName, k -> new ArrayList<>());
		List<ItemStack> tList = sOres.get(aName);
		return tList == null ? EMPTY : tList;
	}

	/** Forge's getOreNames(): every registered name. */
	public static String[] getOreNames() {
		return sOres.keySet().toArray(new String[0]);
	}

	private static boolean sHasInit = false;

	/** Restores the startup set of vanilla dictionary entries Forge itself seeded before mods loaded (the new engine has
	 *  none); newer vanilla content is added under the same rule, using only Greg's existing names, never invented ones. */
	public static void initVanillaEntries() {
		if (sHasInit) return;
		sHasInit = true;

		// Wood: 1.7.10 had one block per part (log/planks/slab/sapling/leaves) with a species meta.
		for (net.minecraft.world.level.block.Block tLog : new net.minecraft.world.level.block.Block[]{Blocks.OAK_LOG, Blocks.SPRUCE_LOG, Blocks.BIRCH_LOG, Blocks.JUNGLE_LOG, Blocks.ACACIA_LOG, Blocks.DARK_OAK_LOG}) registerOre("logWood", ST.make(tLog, 1, 0));
		for (net.minecraft.world.level.block.Block tPlank : new net.minecraft.world.level.block.Block[]{Blocks.OAK_PLANKS, Blocks.SPRUCE_PLANKS, Blocks.BIRCH_PLANKS, Blocks.JUNGLE_PLANKS, Blocks.ACACIA_PLANKS, Blocks.DARK_OAK_PLANKS}) registerOre("plankWood", ST.make(tPlank, 1, 0));
		for (net.minecraft.world.level.block.Block tSlab : new net.minecraft.world.level.block.Block[]{Blocks.OAK_SLAB, Blocks.SPRUCE_SLAB, Blocks.BIRCH_SLAB, Blocks.JUNGLE_SLAB, Blocks.ACACIA_SLAB, Blocks.DARK_OAK_SLAB}) registerOre("slabWood", ST.make(tSlab, 1, 0));
		for (net.minecraft.world.level.block.Block tStair : new net.minecraft.world.level.block.Block[]{Blocks.OAK_STAIRS, Blocks.SPRUCE_STAIRS, Blocks.BIRCH_STAIRS, Blocks.JUNGLE_STAIRS, Blocks.ACACIA_STAIRS, Blocks.DARK_OAK_STAIRS}) registerOre("stairWood", ST.make(tStair, 1, 0));
		registerOre("stickWood", ST.make(net.minecraft.world.item.Items.STICK, 1, 0));
		for (net.minecraft.world.level.block.Block tSap : new net.minecraft.world.level.block.Block[]{Blocks.OAK_SAPLING, Blocks.SPRUCE_SAPLING, Blocks.BIRCH_SAPLING, Blocks.JUNGLE_SAPLING, Blocks.ACACIA_SAPLING, Blocks.DARK_OAK_SAPLING}) registerOre("treeSapling", ST.make(tSap, 1, 0));
		for (net.minecraft.world.level.block.Block tLeaf : new net.minecraft.world.level.block.Block[]{Blocks.OAK_LEAVES, Blocks.SPRUCE_LEAVES, Blocks.BIRCH_LEAVES, Blocks.JUNGLE_LEAVES, Blocks.ACACIA_LEAVES, Blocks.DARK_OAK_LEAVES}) registerOre("treeLeaves", ST.make(tLeaf, 1, 0));

		// Ores and storage blocks (1.7.10's quartz_ore was the Nether variety).
		registerOre("oreGold"      , ST.make(Blocks.GOLD_ORE        , 1, 0));
		registerOre("oreIron"      , ST.make(Blocks.IRON_ORE        , 1, 0));
		registerOre("oreLapis"     , ST.make(Blocks.LAPIS_ORE       , 1, 0));
		registerOre("oreDiamond"   , ST.make(Blocks.DIAMOND_ORE     , 1, 0));
		registerOre("oreRedstone"  , ST.make(Blocks.REDSTONE_ORE    , 1, 0));
		registerOre("oreEmerald"   , ST.make(Blocks.EMERALD_ORE     , 1, 0));
		registerOre("oreQuartz"    , ST.make(Blocks.NETHER_QUARTZ_ORE, 1, 0));
		registerOre("oreCoal"      , ST.make(Blocks.COAL_ORE        , 1, 0));
		// Ore families are split into several blocks on this engine version, not one as in 1.7.10, and the engine itself declares
		// the family via block tags; new members simply register under the same name the first member already uses.
		registerOre("oreGold"      , ST.make(Blocks.DEEPSLATE_GOLD_ORE    , 1, 0));
		registerOre("oreGold"      , ST.make(Blocks.NETHER_GOLD_ORE       , 1, 0));
		registerOre("oreIron"      , ST.make(Blocks.DEEPSLATE_IRON_ORE    , 1, 0));
		registerOre("oreLapis"     , ST.make(Blocks.DEEPSLATE_LAPIS_ORE   , 1, 0));
		registerOre("oreDiamond"   , ST.make(Blocks.DEEPSLATE_DIAMOND_ORE , 1, 0));
		registerOre("oreRedstone"  , ST.make(Blocks.DEEPSLATE_REDSTONE_ORE, 1, 0));
		registerOre("oreEmerald"   , ST.make(Blocks.DEEPSLATE_EMERALD_ORE , 1, 0));
		registerOre("oreCoal"      , ST.make(Blocks.DEEPSLATE_COAL_ORE    , 1, 0));
		// Ancient debris is a nether ore, the same as nether quartz above; Greg's own material for it is MT.AncientDebris.
		// (LanguageHandler.java «Ancient Debris»/«Netherite Scrap»).
		registerOre("oreAncientDebris", ST.make(Blocks.ANCIENT_DEBRIS, 1, 0));
		registerOre("blockGold"    , ST.make(Blocks.GOLD_BLOCK      , 1, 0));
		registerOre("blockIron"    , ST.make(Blocks.IRON_BLOCK      , 1, 0));
		registerOre("blockLapis"   , ST.make(Blocks.LAPIS_BLOCK     , 1, 0));
		registerOre("blockDiamond" , ST.make(Blocks.DIAMOND_BLOCK   , 1, 0));
		registerOre("blockRedstone", ST.make(Blocks.REDSTONE_BLOCK  , 1, 0));
		registerOre("blockEmerald" , ST.make(Blocks.EMERALD_BLOCK   , 1, 0));
		registerOre("blockQuartz"  , ST.make(Blocks.QUARTZ_BLOCK    , 1, 0));
		registerOre("blockCoal"    , ST.make(Blocks.COAL_BLOCK      , 1, 0));
		// New vanilla storage blocks continue this list under the prefix+material rule, matching Greg's own 9-unit block
		// capacity; amethyst_block is excluded since its real recipe is 4 shards, not 9, misstating Greg's blockGem name.
		registerOre("blockNetherite", ST.make(Blocks.NETHERITE_BLOCK, 1, 0));
		registerOre("blockRawIron"  , ST.make(Blocks.RAW_IRON_BLOCK , 1, 0));
		registerOre("blockRawGold"  , ST.make(Blocks.RAW_GOLD_BLOCK , 1, 0));

		// Glass: colorless plus the full color range (1.7.10 used one wildcard entry for all colors).
		registerOre("blockGlassColorless", ST.make(Blocks.GLASS     , 1, 0));
		registerOre("blockGlass"         , ST.make(Blocks.GLASS     , 1, 0));
		for (int i = 0; i < gregapi.data.CS.Flattened.STAINED_GLASS.length; i++) registerOre("blockGlass", ST.make(gregapi.data.CS.Flattened.STAINED_GLASS[i], 1, 0));
		registerOre("paneGlassColorless" , ST.make(Blocks.GLASS_PANE, 1, 0));
		registerOre("paneGlass"          , ST.make(Blocks.GLASS_PANE, 1, 0));
		for (int i = 0; i < gregapi.data.CS.Flattened.STAINED_GLASS_PANE.length; i++) registerOre("paneGlass", ST.make(gregapi.data.CS.Flattened.STAINED_GLASS_PANE[i], 1, 0));

		// Ingots/nuggets/gems/dusts/food (1.7.10's gemLapis was a dye meta).
		registerOre("ingotIron"      , ST.make(net.minecraft.world.item.Items.IRON_INGOT    , 1, 0));
		registerOre("ingotGold"      , ST.make(net.minecraft.world.item.Items.GOLD_INGOT    , 1, 0));
		registerOre("ingotBrick"     , ST.make(net.minecraft.world.item.Items.BRICK         , 1, 0));
		registerOre("ingotBrickNether", ST.make(net.minecraft.world.item.Items.NETHER_BRICK , 1, 0));
		registerOre("nuggetGold"     , ST.make(net.minecraft.world.item.Items.GOLD_NUGGET   , 1, 0));
		// New vanilla material items continue the list under Greg's existing naming rule; copper is deliberately excluded,
		// since a separate vanilla-recipe loader already targets Greg's own copper ingot and this would race that choice.
		registerOre("nuggetIron"     , ST.make(net.minecraft.world.item.Items.IRON_NUGGET   , 1, 0));
		registerOre("gemAmethyst"    , ST.make(net.minecraft.world.item.Items.AMETHYST_SHARD, 1, 0));
		registerOre("ingotNetherite" , ST.make(net.minecraft.world.item.Items.NETHERITE_INGOT, 1, 0));
		registerOre("ingotAncientDebris", ST.make(net.minecraft.world.item.Items.NETHERITE_SCRAP, 1, 0));
		registerOre("oreRawIron"     , ST.make(net.minecraft.world.item.Items.RAW_IRON      , 1, 0));
		registerOre("oreRawGold"     , ST.make(net.minecraft.world.item.Items.RAW_GOLD      , 1, 0));
		registerOre("gemDiamond"     , ST.make(net.minecraft.world.item.Items.DIAMOND       , 1, 0));
		registerOre("gemEmerald"     , ST.make(net.minecraft.world.item.Items.EMERALD       , 1, 0));
		registerOre("gemQuartz"      , ST.make(net.minecraft.world.item.Items.QUARTZ        , 1, 0));
		registerOre("dustRedstone"   , ST.make(net.minecraft.world.item.Items.REDSTONE      , 1, 0));
		registerOre("dustGlowstone"  , ST.make(net.minecraft.world.item.Items.GLOWSTONE_DUST, 1, 0));
		registerOre("gemLapis"       , ST.make(net.minecraft.world.item.Items.LAPIS_LAZULI  , 1, 0));
		registerOre("slimeball"      , ST.make(net.minecraft.world.item.Items.SLIME_BALL    , 1, 0));
		registerOre("glowstone"      , ST.make(Blocks.GLOWSTONE     , 1, 0));
		registerOre("cropWheat"      , ST.make(net.minecraft.world.item.Items.WHEAT         , 1, 0));
		registerOre("cropPotato"     , ST.make(net.minecraft.world.item.Items.POTATO        , 1, 0));
		registerOre("cropCarrot"     , ST.make(net.minecraft.world.item.Items.CARROT        , 1, 0));
		registerOre("stone"          , ST.make(Blocks.STONE         , 1, 0));
		registerOre("cobblestone"    , ST.make(Blocks.COBBLESTONE   , 1, 0));
		// 1.7.10 sandstone meta covered smooth/chiseled/plain; sand meta covered plain plus red.
		registerOre("sandstone"      , ST.make(Blocks.SANDSTONE         , 1, 0));
		registerOre("sandstone"      , ST.make(Blocks.CHISELED_SANDSTONE, 1, 0));
		registerOre("sandstone"      , ST.make(Blocks.CUT_SANDSTONE     , 1, 0));
		registerOre("sand"           , ST.make(Blocks.SAND              , 1, 0));
		registerOre("sand"           , ST.make(Blocks.RED_SAND          , 1, 0));

		// Music discs (1.7.10 record_*, now music_disc_*).
		for (net.minecraft.world.item.Item tRecord : new net.minecraft.world.item.Item[]{
			net.minecraft.world.item.Items.MUSIC_DISC_13, net.minecraft.world.item.Items.MUSIC_DISC_CAT, net.minecraft.world.item.Items.MUSIC_DISC_BLOCKS, net.minecraft.world.item.Items.MUSIC_DISC_CHIRP,
			net.minecraft.world.item.Items.MUSIC_DISC_FAR, net.minecraft.world.item.Items.MUSIC_DISC_MALL, net.minecraft.world.item.Items.MUSIC_DISC_MELLOHI, net.minecraft.world.item.Items.MUSIC_DISC_STAL,
			net.minecraft.world.item.Items.MUSIC_DISC_STRAD, net.minecraft.world.item.Items.MUSIC_DISC_WARD, net.minecraft.world.item.Items.MUSIC_DISC_11, net.minecraft.world.item.Items.MUSIC_DISC_WAIT}) registerOre("record", ST.make(tRecord, 1, 0));

		// Dye and color-named rows follow Forge's dye order; glass/panes run the opposite direction.
		final String[] tDyes = {"Black", "Red", "Green", "Brown", "Blue", "Purple", "Cyan", "LightGray", "Gray", "Pink", "Lime", "Yellow", "LightBlue", "Magenta", "Orange", "White"};
		for (int i = 0; i < 16; i++) {
			registerOre("dye"                , ST.make(gregapi.data.CS.Flattened.DYE[i], 1, 0));
			registerOre("dye"       + tDyes[i], ST.make(gregapi.data.CS.Flattened.DYE[i], 1, 0));
			registerOre("blockGlass"+ tDyes[i], ST.make(gregapi.data.CS.Flattened.STAINED_GLASS     [15-i], 1, 0));
			registerOre("paneGlass" + tDyes[i], ST.make(gregapi.data.CS.Flattened.STAINED_GLASS_PANE[15-i], 1, 0));
		}
		// The four post-1.13 pure dyes (1.14 split: black/blue/brown/white_dye). 1.7.10 had no such items, but
		// the engine hands them to the player as the PRIMARY dye source (cornflower -> blue_dye), and keeping
		// them outside the dye* lists made every GT6 recipe with a dye ingredient reject them (player report
		// 2026-08-29: crowbar refused vanilla blue dye). Explicit adaptation on the player's order; the
		// historical carriers (ink_sac/lapis/cocoa/bone_meal) stay registered above, ore lists only widen.
		registerOre("dye"     , ST.make(net.minecraft.world.item.Items.BLACK_DYE, 1, 0));
		registerOre("dyeBlack", ST.make(net.minecraft.world.item.Items.BLACK_DYE, 1, 0));
		registerOre("dye"     , ST.make(net.minecraft.world.item.Items.BLUE_DYE , 1, 0));
		registerOre("dyeBlue" , ST.make(net.minecraft.world.item.Items.BLUE_DYE , 1, 0));
		registerOre("dye"     , ST.make(net.minecraft.world.item.Items.BROWN_DYE, 1, 0));
		registerOre("dyeBrown", ST.make(net.minecraft.world.item.Items.BROWN_DYE, 1, 0));
		registerOre("dye"     , ST.make(net.minecraft.world.item.Items.WHITE_DYE, 1, 0));
		registerOre("dyeWhite", ST.make(net.minecraft.world.item.Items.WHITE_DYE, 1, 0));
	}

	private static boolean sHasReplacedRecipes = false;

	/** Replaces the second half of Forge's own setup: re-registering vanilla recipes as ore-recipes wherever an
	 *  input matched a replaceable item; the vanilla recipe stays (data-driven), the ore version added as a superset. */
	public static void initVanillaRecipeReplacements(net.minecraft.server.MinecraftServer aServer) {
		if (sHasReplacedRecipes || aServer == null) return;
		sHasReplacedRecipes = true;

		// Replacement map matches Forge's own list verbatim; split families are listed the same way as above.
		final Map<net.minecraft.world.item.Item, String> tReplacements = new java.util.HashMap<>();
		tReplacements.put(net.minecraft.world.item.Items.STICK, "stickWood");
		for (net.minecraft.world.level.block.Block tPlank : new net.minecraft.world.level.block.Block[]{Blocks.OAK_PLANKS, Blocks.SPRUCE_PLANKS, Blocks.BIRCH_PLANKS, Blocks.JUNGLE_PLANKS, Blocks.ACACIA_PLANKS, Blocks.DARK_OAK_PLANKS}) tReplacements.put(tPlank.asItem(), "plankWood");
		tReplacements.put(Blocks.STONE.asItem()           , "stone");
		tReplacements.put(Blocks.COBBLESTONE.asItem()     , "cobblestone");
		tReplacements.put(net.minecraft.world.item.Items.GOLD_INGOT    , "ingotGold");
		tReplacements.put(net.minecraft.world.item.Items.IRON_INGOT    , "ingotIron");
		tReplacements.put(net.minecraft.world.item.Items.DIAMOND       , "gemDiamond");
		tReplacements.put(net.minecraft.world.item.Items.EMERALD       , "gemEmerald");
		tReplacements.put(net.minecraft.world.item.Items.REDSTONE      , "dustRedstone");
		tReplacements.put(net.minecraft.world.item.Items.GLOWSTONE_DUST, "dustGlowstone");
		tReplacements.put(Blocks.GLOWSTONE.asItem()       , "glowstone");
		tReplacements.put(net.minecraft.world.item.Items.SLIME_BALL    , "slimeball");
		tReplacements.put(Blocks.GLASS.asItem()           , "blockGlassColorless");
		final String[] tDyes = {"Black", "Red", "Green", "Brown", "Blue", "Purple", "Cyan", "LightGray", "Gray", "Pink", "Lime", "Yellow", "LightBlue", "Magenta", "Orange", "White"};
		for (int i = 0; i < 16; i++) {
			tReplacements.put(gregapi.data.CS.Flattened.DYE[i]                              , "dye"        + tDyes[i]);
			tReplacements.put(gregapi.data.CS.Flattened.STAINED_GLASS     [15-i].asItem()   , "blockGlass" + tDyes[i]);
			tReplacements.put(gregapi.data.CS.Flattened.STAINED_GLASS_PANE[15-i].asItem()   , "paneGlass"  + tDyes[i]);
		}
		// New neo dyes needed by datapack dyeing recipes, not the older ink/lapis/cocoa/bone-meal items.
		tReplacements.put(net.minecraft.world.item.Items.BLACK_DYE, "dyeBlack");
		tReplacements.put(net.minecraft.world.item.Items.BLUE_DYE , "dyeBlue");
		tReplacements.put(net.minecraft.world.item.Items.BROWN_DYE, "dyeBrown");
		tReplacements.put(net.minecraft.world.item.Items.WHITE_DYE, "dyeWhite");

		// Exclusion list matches Forge's own verbatim, remapped to the current block names (e.g. the 1.7.10 stone
		// stairs recipe used cobblestone, so it maps to neo's separate cobblestone_stairs, not the new stone_stairs).
		final java.util.Set<net.minecraft.world.item.Item> tExclusions = new java.util.HashSet<>();
		tExclusions.add(Blocks.LAPIS_BLOCK.asItem());
		tExclusions.add(net.minecraft.world.item.Items.COOKIE);
		tExclusions.add(Blocks.STONE_BRICKS.asItem());
		for (net.minecraft.world.level.block.Block tSlab : new net.minecraft.world.level.block.Block[]{Blocks.STONE_SLAB, Blocks.SMOOTH_STONE_SLAB, Blocks.SANDSTONE_SLAB, Blocks.PETRIFIED_OAK_SLAB, Blocks.COBBLESTONE_SLAB, Blocks.BRICK_SLAB, Blocks.STONE_BRICK_SLAB, Blocks.NETHER_BRICK_SLAB, Blocks.QUARTZ_SLAB}) tExclusions.add(tSlab.asItem());
		tExclusions.add(Blocks.COBBLESTONE_STAIRS.asItem());
		tExclusions.add(Blocks.COBBLESTONE_WALL.asItem());
		for (net.minecraft.world.level.block.Block tStair : new net.minecraft.world.level.block.Block[]{Blocks.OAK_STAIRS, Blocks.SPRUCE_STAIRS, Blocks.BIRCH_STAIRS, Blocks.JUNGLE_STAIRS, Blocks.ACACIA_STAIRS, Blocks.DARK_OAK_STAIRS}) tExclusions.add(tStair.asItem());
		tExclusions.add(Blocks.GLASS_PANE.asItem());

		// Without this rule an ore-recipe substitution could erase which wood species or color a vanilla recipe
		// produces; any pattern group whose members disagree on output is skipped, keeping the vanilla result intact.
		final java.util.Map<String, java.util.List<gregapi.recipes.ICraftingRecipeGT>> tBySignature = new java.util.LinkedHashMap<>();
		final java.util.Map<String, java.util.Set<net.minecraft.world.item.Item>> tOutputsBySignature = new java.util.HashMap<>();

		int tReplaced = 0;
		// On 1.20.1 there's no recipe+id wrapper; getRecipes() returns the recipes themselves, each already carrying its own id.
		for (net.minecraft.world.item.crafting.Recipe<?> tAny : aServer.getRecipeManager().getRecipes()) {
			try {
				if (tAny instanceof net.minecraft.world.item.crafting.ShapedRecipe tShaped) {
					ItemStack tOutput = tShaped.getResultItem(aServer.registryAccess()); // The nominal output, equivalent to 1.7.10's getRecipeOutput.
					if (ST.invalid(tOutput) || tExclusions.contains(tOutput.getItem())) continue;
					// Pattern width/height come through the Forge IShapedRecipe channel, since those fields are package-private on the class.
					int tWidth = tShaped.getRecipeWidth(), tHeight = tShaped.getRecipeHeight();
					StringBuilder tSignature = new StringBuilder(tWidth + "x" + tHeight + ":");
					Object[] tCells = replaceIngredients(tShaped.getIngredients(), tReplacements, tSignature);
					if (tCells == null) continue; // No replacement matched and no custom ingredient is present, so this recipe is not a candidate.
					// Builds the Forge-style constructor arguments (pattern rows plus symbol-to-ingredient pairs).
					java.util.List<Object> tArgs = new java.util.ArrayList<>();
					String[] tRows = new String[tHeight];
					char tChar = 'A';
					for (int y = 0; y < tHeight; y++) {
						StringBuilder tRow = new StringBuilder();
						for (int x = 0; x < tWidth; x++) {
							Object tCell = tCells[x + y * tWidth];
							if (tCell == null) {tRow.append(' '); continue;}
							tRow.append(tChar);
							tArgs.add(tChar);
							tArgs.add(tCell);
							tChar++;
						}
						tRows[y] = tRow.toString();
					}
					tArgs.add(0, tRows);
					gregapi.recipes.ShapedOreRecipe tRecipe = new gregapi.recipes.ShapedOreRecipe(tOutput, tArgs.toArray());
					tRecipe.mVanillaReplacement = true; // Marks this as a Forge-style vanilla replacement for the recipe-replacement scan to pick up.
					tRecipe.mSourceId = tAny.getId();
					tBySignature.computeIfAbsent(tSignature.toString(), k -> new java.util.ArrayList<>()).add(tRecipe);
					tOutputsBySignature.computeIfAbsent(tSignature.toString(), k -> new java.util.HashSet<>()).add(tOutput.getItem());
				} else if (tAny instanceof net.minecraft.world.item.crafting.ShapelessRecipe tShapeless) {
					ItemStack tOutput = tShapeless.getResultItem(aServer.registryAccess());
					if (ST.invalid(tOutput) || tExclusions.contains(tOutput.getItem())) continue;
					// On 1.20.1 there's no PlacementInfo wrapper; the recipe itself returns its own ingredient list.
					StringBuilder tRawSignature = new StringBuilder();
					Object[] tCells = replaceIngredients(tShapeless.getIngredients(), tReplacements, tRawSignature);
					if (tCells == null) continue;
					gregapi.recipes.ShapelessOreRecipe tRecipe = new gregapi.recipes.ShapelessOreRecipe(tOutput, tCells);
					tRecipe.mVanillaReplacement = true;
					tRecipe.mSourceId = tAny.getId();
					// Cell order does not matter for a shapeless recipe, so the signature is sorted; otherwise equivalent
					// grids would land in different groups and the collision would go unnoticed.
					String[] tParts = tRawSignature.toString().split("\\|", -1);
					java.util.Arrays.sort(tParts);
					String tSignature = "shapeless:" + String.join("|", tParts);
					tBySignature.computeIfAbsent(tSignature, k -> new java.util.ArrayList<>()).add(tRecipe);
					tOutputsBySignature.computeIfAbsent(tSignature, k -> new java.util.HashSet<>()).add(tOutput.getItem());
				}
			} catch(Throwable e) {e.printStackTrace(gregapi.data.CS.ERR);}
		}
		// A signature group with more than one distinct output is dropped entirely, since the ore version there
		// would swap the wood species or color rather than being a safe superset of the vanilla recipe.
		int tCollided = 0;
		for (Map.Entry<String, java.util.List<gregapi.recipes.ICraftingRecipeGT>> tEntry : tBySignature.entrySet()) {
			if (tOutputsBySignature.get(tEntry.getKey()).size() > 1) {tCollided += tEntry.getValue().size(); continue;}
			for (gregapi.recipes.ICraftingRecipeGT tRecipe : tEntry.getValue()) {gregapi.util.CR.BUFFER.add(tRecipe); tReplaced++;}
		}
		gregapi.data.CS.OUT.println("GT_API: Vanilla recipe replacements (F4 role-C): " + tReplaced + " ore-versions added to CR.BUFFER, " + tCollided + " skipped as ambiguous (output family collision).");
	}

	/** Builds the cells for an ore recipe from the vanilla recipe's ingredients: the replaced item becomes the
	 *  live ore list, everything else stays as its stack or list of stacks; null means this recipe is not a candidate. */
	private static Object[] replaceIngredients(java.util.List<net.minecraft.world.item.crafting.Ingredient> aIngredients, Map<net.minecraft.world.item.Item, String> aReplacements, StringBuilder aSignature) {
		Object[] rCells = new Object[aIngredients.size()];
		boolean tAnyReplaced = false;
		for (int i = 0; i < rCells.length; i++) {
			if (i > 0) aSignature.append('|');
			// An empty pattern cell is Ingredient.EMPTY here, not Optional.empty; a foreign ingredient is Forge's own isVanilla flag.
			net.minecraft.world.item.crafting.Ingredient tIn = aIngredients.get(i);
			if (tIn == null || tIn.isEmpty()) continue;
			if (!tIn.isVanilla()) return null;
			java.util.LinkedHashSet<net.minecraft.world.item.Item> tSet = new java.util.LinkedHashSet<>();
			for (ItemStack tStack : tIn.getItems()) if (!tStack.isEmpty()) tSet.add(tStack.getItem());
			java.util.List<net.minecraft.world.item.Item> tItems = new java.util.ArrayList<>(tSet);
			if (tItems.isEmpty()) return null;
			String tOreName = null;
			for (net.minecraft.world.item.Item tItem : tItems) if ((tOreName = aReplacements.get(tItem)) != null) break;
			if (tOreName != null) {
				rCells[i] = getOres(tOreName);
				tAnyReplaced = true;
				aSignature.append(tOreName); // The cell after replacement is the ore key itself; ambiguity is judged by that key.
			} else if (tItems.size() == 1) {
				rCells[i] = new ItemStack(tItems.get(0));
				aSignature.append(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(tItems.get(0)));
			} else {
				java.util.List<ItemStack> tAlts = new java.util.ArrayList<>();
				java.util.List<String> tKeys = new java.util.ArrayList<>();
				for (net.minecraft.world.item.Item tItem : tItems) {tAlts.add(new ItemStack(tItem)); tKeys.add(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(tItem).toString());}
				rCells[i] = tAlts;
				java.util.Collections.sort(tKeys);
				aSignature.append(String.join(",", tKeys));
			}
		}
		return tAnyReplaced ? rCells : null;
	}

	/** Simple GT6 carrier class replacing Forge's OreDictionary.OreRegisterEvent; nested here so the import
	 *  path stays unchanged. */
	public static class OreRegisterEvent extends Event {
		public final String Name;
		public final ItemStack Ore;

		public OreRegisterEvent(String aName, ItemStack aOre) {
			Name = aName;
			Ore = aOre;
		}
	}
}

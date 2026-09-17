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

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import gregapi.code.ModData;
import gregapi.data.MD;
import gregapi.util.ST;

import static gregapi.data.CS.*;

/** Central AE2 address table: the only place that translates Applied Energistics 2 rv2 (1.7.10) item
 *  names into flat 15.4.10 ids. One funnel (ST.make and friends) calls it, so GT6's own addressing code stays verbatim. */
public final class AE2Names {
	private AE2Names() {}

	/** Carrier exists: "name rv2" or "name rv2#meta" -> path id in the ae2 namespace. */
	private static final Map<String, String> MAPPED = new LinkedHashMap<>();
	/** No carrier: "name rv2" or "name rv2#meta" -> the reason, printed by the caller/stand. */
	private static final Map<String, String> GONE = new LinkedHashMap<>();
	/** Names whose 1.7.10 meta was a SUBTYPE: look up by "name#meta" first, and don't set meta on the
	 *  match found. */
	private static final Set<String> SPLIT = new LinkedHashSet<>();

	private static void map (String aName,             String aPath  ) {MAPPED.put(aName, aPath);}
	private static void map (String aName, long aMeta, String aPath  ) {SPLIT.add(aName); MAPPED.put(key(aName, aMeta), aPath);}
	private static void gone(String aName,             String aReason) {GONE .put(aName, aReason);}
	private static void gone(String aName, long aMeta, String aReason) {SPLIT.add(aName); GONE .put(key(aName, aMeta), aReason);}

	private static String key(String aName, long aMeta) {return aName + "#" + aMeta;}

	// ================================================================================================
	// One line per reason a mapping is missing, so wording doesn't drift between causes.
	// ================================================================================================
	private static final String
	  NO_SEED     = "семян кристаллов в AE2 15.4.10 нет: рост заменён budding-блоками (AEBlockIds.java:39-42 flawless/flawed/chipped/damaged_budding_quartz + :44-46 small/medium/large_quartz_bud), предмета-затравки в реестре нет. Меты GT6 — это стадии роста rv3: ItemCrystalSeed.java:62-64 объявляет CERTUS=0, NETHER=600, FLUIX=1200"
	, NO_ORE      = "рудного блока сертуса в AE2 15.4.10 нет: сертус даёт budding-цепь метеорита, ни OreQuartz, ни OreQuartzCharged в AEBlockIds не объявлены (в rv3 блоки были: appeng/block/solids/OreQuartz.java, OreQuartzCharged.java)"
	, NO_ORE_CASE = "имени с такой раскладкой букв не существовало и в AE2 rv3: реестровое имя блока там — «tile.» + простое имя КЛАССА (AEBlockFeatureHandler:72), а классы зовутся OreQuartz/OreQuartzCharged с большой буквы. Плюс самого рудного блока в 15.4.10 нет — см. соседнюю строку"
	, NO_PURIFIED = "«очищенный» кристалл объявлен (AEItemIds.java:203-205), но НЕ зарегистрирован: AEItems.java на PURIFIED_* не ссылается — предмета в реестре нет"
	, NO_BIOMETRIC= "биометрической карты в AE2 15.4.10 нет: греп «biometric» по appeng/**/*.java пуст (в rv3 предмет был: ToolBiometricCard, tools/network.recipe)"
	;

	static {
		// ============================================================================================
		// Blocks: only two 1.7.10 names carried a subtype in meta; the rest map the whole block, meta passes
		// through unchanged. quartz_block proven by matching rv3/15.4.10 registry name and recipe role.
		//   4 × crystalCertusQuartz (2×2) → BlockQuartz ⟺ 15.4.10 decorative/quartz_block.json 4 × certus_quartz_crystal.
		map("tile.BlockQuartz"                  , "quartz_block");
		// quartz_pillar and chiseled_quartz_block proven the same way: matching rv3/15.4.10 names and recipes.
		map("tile.BlockQuartzPillar"            , "quartz_pillar");
		map("tile.BlockQuartzChiseled"          , "chiseled_quartz_block");
		// fluix_block proven the same way: rv3's 4-crystalFluix craft matches 15.4.10's fluix_block.json.
		map("tile.BlockFluix"                   , "fluix_block");
		// quartz_glass proven cell-for-cell: rv3's dustQuartz+glass pattern matches 15.4.10's recipe exactly.
		map("tile.BlockQuartzGlass"             , "quartz_glass");
		// quartz_vibrant_glass proven the same way, by matching rv3/15.4.10 names.
		//   «glowstone_dust BlockQuartzGlass glowstone_dust» ⟺ 15.4.10 quartz_vibrant_glass.json «aba».
		map("tile.BlockQuartzLamp"              , "quartz_vibrant_glass");
		// quartz_fixture proven by matching name and recipe output count between rv3 and 15.4.10.
		//   iron_ingot → 2 BlockQuartzTorch» ⟺ 15.4.10 quartz_fixture.json → count 2.
		map("tile.BlockQuartzTorch"             , "quartz_fixture");
		// Stairs and slabs: names come from rv3's block classes/parameter; human-readable names match 15.4.10
		// verbatim, same input/output counts.
		map("tile.QuartzStairBlock"             , "quartz_stairs");
		map("tile.QuartzPillarStairBlock"       , "quartz_pillar_stairs");
		map("tile.ChiseledQuartzStairBlock"     , "chiseled_quartz_stairs");
		map("tile.FluixStairBlock"              , "fluix_stairs");
		map("tile.QuartzSlabBlock"              , "quartz_slab");
		map("tile.QuartzPillarSlabBlock"        , "quartz_pillar_slab");
		map("tile.ChiseledQuartzSlabBlock"      , "chiseled_quartz_slab");
		map("tile.FluixSlabBlock"               , "fluix_slab");
		// Sky stone stairs/slabs: rv3 assigns each meta explicitly to a specific stair/slab class; names
		// match 15.4.10 verbatim.
		//   en_us.json:178/193/174/180.
		map("tile.SkyStoneStairBlock"           , "sky_stone_stairs");
		map("tile.SkyStoneBlockStairBlock"      , "smooth_sky_stone_stairs");
		map("tile.SkyStoneBrickStairBlock"      , "sky_stone_brick_stairs");
		map("tile.SkyStoneSmallBrickStairBlock" , "sky_stone_small_brick_stairs");
		map("tile.SkyStoneSlabBlock"            , "sky_stone_slab");
		map("tile.SkyStoneBlockSlabBlock"       , "smooth_sky_stone_slab");
		map("tile.SkyStoneBrickSlabBlock"       , "sky_stone_brick_slab");
		map("tile.SkyStoneSmallBrickSlabBlock"  , "sky_stone_small_brick_slab");

		// tile.BlockSkyStone carried its subtype in meta (four stones in one 1.7.10 block); rv3 names each
		// meta explicitly. W ("any") isn't a subtype: it resolves to the family's base stone, the same convention as CS.Flattened.
		map("tile.BlockSkyStone",  0, "sky_stone_block");
		map("tile.BlockSkyStone",  1, "smooth_sky_stone_block");
		map("tile.BlockSkyStone",  2, "sky_stone_brick");
		map("tile.BlockSkyStone",  3, "sky_stone_small_brick");
		map("tile.BlockSkyStone",  W, "sky_stone_block");

		// tile.BlockSkyChest, second family collapsed by meta: rv3 lang strings and recipes match 15.4.10's
		// two chest ids and recipes exactly.
		map("tile.BlockSkyChest",  0, "sky_stone_chest");
		map("tile.BlockSkyChest",  1, "smooth_sky_stone_chest");
		map("tile.BlockSkyChest",  W, "sky_stone_chest");

		// Certus ore has no 15.4.10 carrier at all; GT6 calls it under four different spellings, so all four
		// are listed, leaving no silent null.
		gone("tile.OreQuartz"                   , NO_ORE);
		gone("tile.oreQuartz"                   , NO_ORE_CASE);
		gone("tile.OreQuartzCharged"            , NO_ORE);
		gone("tile.oreQuartzCharged"            , NO_ORE_CASE);

		// ============================================================================================
		// Standalone items. Quartz tools: rv3 builds names from the tool class plus a Certus/Nether sub-name;
		// human-readable names match 15.4.10 verbatim.
		//   «Certus Quartz Wrench» (en_US.lang) ⟺ en_us.json:736 certus_quartz_wrench (AEItemIds:172, AEItems:108);
		//   «Nether Quartz Wrench» ⟺ :862 (AEItemIds:180, AEItems:120); «Certus Quartz Cutting Knife» ⟺ :730
		//   (AEItemIds:173, AEItems:109); «Nether Quartz Cutting Knife» ⟺ :857 (AEItemIds:181, AEItems:121).
		map("item.ToolCertusQuartzWrench"       , "certus_quartz_wrench");
		map("item.ToolNetherQuartzWrench"       , "nether_quartz_wrench");
		map("item.ToolCertusQuartzCuttingKnife" , "certus_quartz_cutting_knife");
		map("item.ToolNetherQuartzCuttingKnife" , "nether_quartz_cutting_knife");
		// Wireless Terminal / Memory Card / View Cell proven by matching rv3 and 15.4.10 names verbatim.
		map("item.ToolWirelessTerminal"         , "wireless_terminal");
		map("item.ToolMemoryCard"               , "memory_card");
		map("item.ItemViewCell"                 , "view_cell");
		// rv3 had exactly one portable cell, and its recipe's closest living 15.4.10 descendant is the 1k
		// item cell (three of four recipe inputs match).
		//   (LoaderBookList:372).
		map("item.ToolPortableCell"             , "portable_item_cell_1k");
		// 1k-64k storage cells proven by matching rv3/15.4.10 names (word "Item" added in 15.4.10 for the
		// new fluid-cell family) and recipes.
		map("item.ItemBasicStorageCell.1k"      , "item_storage_cell_1k");
		map("item.ItemBasicStorageCell.4k"      , "item_storage_cell_4k");
		map("item.ItemBasicStorageCell.16k"     , "item_storage_cell_16k");
		map("item.ItemBasicStorageCell.64k"     , "item_storage_cell_64k");
		// The one line where the two AE2 lines disagree by VALUE, not just citation number: 15.4.10 has no
		// "creative_storage_cell" id at all, its role is now creative_item_cell, proven the same way as storage cells above.
		map("item.ItemCreativeStorageCell"      , "creative_item_cell");
		// 2/16/128-cubed spatial storage cells proven by matching rv3/15.4.10 names verbatim, three lines.
		map("item.ItemSpatialStorageCell.2Cubed"  , "spatial_storage_cell_2");
		map("item.ItemSpatialStorageCell.16Cubed" , "spatial_storage_cell_16");
		map("item.ItemSpatialStorageCell.128Cubed", "spatial_storage_cell_128");
		// No 15.4.10 carrier: the rv3 Biometric Card existed, but "biometric" doesn't appear anywhere in
		// 15.4.10's AE2 sources.
		gone("item.ToolBiometricCard"           , NO_BIOMETRIC);
		// No 15.4.10 carrier: rv3's three crystal-seed metas match GT6's own values exactly, but 15.4.10
		// replaced seed growth with budding blocks.
		gone("item.ItemCrystalSeed"             , NO_SEED);

		// ============================================================================================
		// item.ItemMultiPart, meta 120 = cable anchor: meta is declared explicitly in rv3's PartType enum,
		// and name plus recipe output count both match 15.4.10.
		// ============================================================================================
		map("item.ItemMultiPart", 120, "cable_anchor");

		// ============================================================================================
		// item.ItemMultiMaterial: a materials grab-bag, meta = specific material. Each meta's meaning below
		// is the one declared in rv3's MaterialType enum, not guessed; meta 1 matches name and GT6's own unification target.
		map("item.ItemMultiMaterial",  1, "charged_certus_quartz_crystal");
		// Meta 10/11/12 have no 15.4.10 carrier: these are rv3's "purified" crystals, proven by GT6's own
		// autoclave/compressor recipes matching rv3's, but declared in 15.4.10's ids and never registered.
		gone("item.ItemMultiMaterial", 10, NO_PURIFIED);
		gone("item.ItemMultiMaterial", 11, NO_PURIFIED);
		gone("item.ItemMultiMaterial", 12, NO_PURIFIED);
		// Meta 13/14/15/19 = the four inscriber presses, proven by matching rv3/15.4.10 names and the same
		// self-multiplying recipe GT6 already uses.
		map("item.ItemMultiMaterial", 13, "calculation_processor_press");
		map("item.ItemMultiMaterial", 14, "engineering_processor_press");
		map("item.ItemMultiMaterial", 15, "logic_processor_press");
		map("item.ItemMultiMaterial", 19, "silicon_press");
		// Meta 16/17/18/20 = the matching four printed circuits, proven the same way, cell for cell with
		// GT6's own four stampings.
		map("item.ItemMultiMaterial", 16, "printed_calculation_processor");
		map("item.ItemMultiMaterial", 17, "printed_engineering_processor");
		map("item.ItemMultiMaterial", 18, "printed_logic_processor");
		map("item.ItemMultiMaterial", 20, "printed_silicon");
		// Meta 21 = name_press, proven by matching rv3/15.4.10 names verbatim.
		map("item.ItemMultiMaterial", 21, "name_press");
		// Meta 22/23/24 = the three processors, proven by matching names and the same redstone+print+
		// silicon-print recipe GT6 already uses.
		map("item.ItemMultiMaterial", 22, "logic_processor");
		map("item.ItemMultiMaterial", 23, "calculation_processor");
		map("item.ItemMultiMaterial", 24, "engineering_processor");
		// Meta 39 = storage-cell housing; 15.4.10 split it into item/fluid housings, and three independent
		// recipe/history checks point to the item variant as its true descendant.
		map("item.ItemMultiMaterial", 39, "item_cell_housing");
		// Meta 45 = sky_dust, proven by matching rv3/15.4.10 names and GT6's own unification target.
		map("item.ItemMultiMaterial", 45, "sky_dust");
	}

	/** Whether this center knows this (mod, name) pair -- i.e. whether the ST.make funnel should defer to it. */
	public static boolean owns(ModData aMod, String aName) {
		if (aMod == null || aName == null || !MD.AE.mID.equals(aMod.mID)) return F;
		return SPLIT.contains(aName) || MAPPED.containsKey(aName) || GONE.containsKey(aName);
	}

	/** 15.4.10 id path for an (rv2 name, meta) pair; null means no carrier or the name isn't ours. */
	public static String path(String aName, long aMeta) {
		if (aName == null) return null;
		return SPLIT.contains(aName) ? MAPPED.get(key(aName, aMeta)) : MAPPED.get(aName);
	}

	/** Whether the pair has a 15.4.10 carrier at all; callers use this to decide whether to register their own entry. */
	public static boolean has(String aName, long aMeta) {
		return path(aName, aMeta) != null;
	}

	/** The REASON a carrier is missing; null means either a carrier exists or the name isn't ours. */
	public static String reason(String aName, long aMeta) {
		if (aName == null || path(aName, aMeta) != null) return null;
		if (!SPLIT.contains(aName)) return GONE.get(aName);
		String rReason = GONE.get(key(aName, aMeta));
		return rReason != null ? rReason : "мета " + aMeta + " у «" + aName + "» центру неизвестна (GT6 её не зовёт)";
	}

	/** Sole way to get an AE2 stack from a 1.7.10 name; called only from the ST.make funnel, itself
	 *  reaching the registry only through ST.findItem. Split names' subtype is already in the item, so meta isn't set on them. */
	public static ItemStack make(String aName, long aSize, long aMeta) {
		if (!MD.AE.mLoaded || !GAPI_POST.mStartedPreInit) return null;
		String tPath = path(aName, aMeta);
		if (tPath == null) return null;
		Item tItem = ST.findItem(MD.AE.mID, tPath);
		if (tItem == null) return null;
		return ST.make_(tItem, aSize, SPLIT.contains(aName) && aMeta != W ? 0 : aMeta);
	}
}

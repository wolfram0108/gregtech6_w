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

/**
 * AE2 ADDRESSING CENTRE (stage E2 of the GT6 ↔ AE2 compatibility layer) — the ONLY place that translates
 * Applied Energistics 2 rv2 item names (Minecraft 1.7.10, «item.ItemMultiMaterial» + meta) into
 * flat AE2 26.1 ids («ae2:calculation_processor_press»).
 *
 * <p><b>Why a centre, not spot fixes.</b> GT6 addresses the foreign mod in 124 places across six files
 * ({@code LoaderItemList}, {@code LoaderItemData}, {@code LoaderBookList}, {@code LoaderUnificationTargets},
 * {@code Loader_Recipes_Replace}, {@code Compat_Recipes_AppliedEnergistics}), and ALL of them converge into one
 * funnel — {@code ST.make(ModData, String, long aSize, long aMeta)}: the same funnel is fed by
 * {@code ST.block(ModData,String)} (ST:194), {@code OM.data(ModData,…)} (OM:73), {@code ST.item(ModData,String)}
 * (ST:185), {@code OreDictManager.setTarget(…,ModData,name,meta)} (OreDictManager:520),
 * {@code ItemStackMap.put(ModData,…)} (ItemStackMap:189) and {@code ItemStackSet.add(ModData,…)} (ItemStackSet:78).
 * So the table is declared HERE exactly once, and exactly one caller invokes it — that same funnel;
 * the GT6 source at the addressing sites stays verbatim-1:1. This is the same technique the port already
 * used to solve vanilla meta flattening — {@code CS.Flattened} (tables in one place, substituted in {@code ST.make_}).
 *
 * <p><b>Three verdicts, no silent null.</b> A «name + meta» pair gets one of three answers:
 * <ol>
 * <li><b>carrier exists</b> — {@link #path} returns the 26.1 id path;</li>
 * <li><b>no carrier</b> — {@link #reason} returns the REASON (item removed from AE2, correspondence not
 *     proven); the caller must silently skip its registration rather than feed null into a recipe;</li>
 * <li><b>not our name</b> — {@link #owns} = F, the funnel takes the old path.</li>
 * </ol>
 *
 * <p><b>Where the correspondences come from.</b> Three primary sources, not a single guess:
 * <ol>
 * <li><b>left side — AE2 rv3 sources for 1.7.10</b> ({@code reference/mods/AE2-rv3-1.7.10}), the very
 *     version GT6 worked against. The name GT6 calls an item by is AE2's REGISTRY name, and there it is
 *     built mechanically: a block — {@code "tile." + simple class name} ({@code AEBlockFeatureHandler:72}),
 *     an item — {@code "item." + simple class name} ({@code ItemFeatureHandler:92}); the classes
 *     {@code ItemMultiMaterial}/{@code ItemMultiPart} keep their full name in the registry but are called
 *     {@code ItemMaterial}/{@code ItemPart} in resources ({@code ItemFeatureHandler:83-90},
 *     {@code FeatureNameExtractor:45-51}). The meaning of every meta is declared explicitly: {@code MaterialType.java}
 *     (meta in parentheses next to the constant), {@code PartType.java}, {@code ItemCrystalSeed.java:62-64},
 *     {@code BlockSkyStone.java:143-161}, {@code ApiBlocks.java:231-251};</li>
 * <li><b>right side — the 26.1 registry</b>: {@code appeng/api/ids/AEItemIds.java}, {@code AEBlockIds.java},
 *     {@code AEPartIds.java}, with the FACT of registration coming from references to these constants in
 *     {@code appeng/core/definitions/AEItems.java}, {@code AEBlocks.java}, {@code AEParts.java} (an id
 *     declared in Ids but not referenced from definitions does NOT make it into the registry — that is
 *     the case for the «purified» crystals);</li>
 * <li><b>bridge between the sides</b> — a match of ROLE: the rv3 recipe
 *     ({@code assets/appliedenergistics2/recipes/**}) against the 26.1 recipe
 *     ({@code src/generated/resources/data/ae2/recipe/**}) cell for cell, and/or a match of the
 *     human-readable name: {@code lang/en_US.lang} in rv3 against {@code lang/en_us.json} in 26.1.</li>
 * </ol>
 * Each line below states exactly what proves it.
 */
public final class AE2Names {
	private AE2Names() {}

	/** Carrier exists: «rv2 name» or «rv2 name#meta» → id path in the {@code ae2} namespace. */
	private static final Map<String, String> MAPPED = new LinkedHashMap<>();
	/** No carrier: «rv2 name» or «rv2 name#meta» → the reason printed by the caller/stand. */
	private static final Map<String, String> GONE = new LinkedHashMap<>();
	/** Names whose 1.7.10 meta was a SUBTYPE: look up by «name#meta» first, and don't stamp meta on the found subtype. */
	private static final Set<String> SPLIT = new LinkedHashSet<>();

	private static void map (String aName,             String aPath  ) {MAPPED.put(aName, aPath);}
	private static void map (String aName, long aMeta, String aPath  ) {SPLIT.add(aName); MAPPED.put(key(aName, aMeta), aPath);}
	private static void gone(String aName,             String aReason) {GONE .put(aName, aReason);}
	private static void gone(String aName, long aMeta, String aReason) {SPLIT.add(aName); GONE .put(key(aName, aMeta), aReason);}

	private static String key(String aName, long aMeta) {return aName + "#" + aMeta;}

	// ================================================================================================
	// Reasons for the missing carrier — one line per class, so wordings don't drift apart.
	// ================================================================================================
	private static final String
	  NO_SEED     = "AE2 26.1 has no crystal seeds: growth was replaced by budding blocks (AEBlockIds: flawless/flawed/chipped/damaged_budding_quartz + *_quartz_bud), no seed item in the registry. GT6 metas are the rv3 growth stages: ItemCrystalSeed.java:62-64 declares CERTUS=0, NETHER=600, FLUIX=1200"
	, NO_ORE      = "AE2 26.1 has no certus ore block: certus comes from the meteorite budding chain, and neither OreQuartz nor OreQuartzCharged is declared in AEBlockIds (in rv3 the blocks existed: appeng/block/solids/OreQuartz.java, OreQuartzCharged.java)"
	, NO_ORE_CASE = "a name spelled this way never existed even in AE2 rv3: the registry name there is «tile.» plus the simple CLASS name (AEBlockFeatureHandler:72), and the classes are OreQuartz/OreQuartzCharged with capitals. On top of that the ore block itself is gone in 26.1 — see the line next to this one"
	, NO_PURIFIED = "the «purified» crystal is declared (AEItemIds.java:203-205) but NOT registered: AEItems.java never references PURIFIED_* — the item is absent from the registry"
	, NO_BIOMETRIC= "AE2 26.1 has no biometric card: grepping «biometric» over appeng/**/*.java returns nothing (in rv3 the item existed: ToolBiometricCard, tools/network.recipe)"
	;

	static {
		// ============================================================================================
		// BLOCKS. The 1.7.10 meta was a subtype for exactly two names — tile.BlockSkyStone and tile.BlockSkyChest
		// (both families are expanded below); for all others the name addresses the whole block and meta passes through.
		// ============================================================================================
		// [PROVEN FROM THE PRIMARY SOURCE: rv3 appeng/block/solids/BlockQuartz.java → registry name «tile.BlockQuartz»,
		//   en_US.lang:22 «Certus Quartz Block»] → 26.1 AEBlockIds:109 quartz_block, en_us.json:151 «Certus Quartz
		//   Block» — name for name. Role matches: rv3 decorative/crystals.recipe 4 × crystalCertusQuartz (2×2) →
		//   BlockQuartz ⟺ 26.1 decorative/quartz_block.json 4 × ae2:certus_quartz_crystal (2×2) → quartz_block.
		map("tile.BlockQuartz"                  , "quartz_block");
		// [PROVEN FROM THE PRIMARY SOURCE: rv3 BlockQuartzPillar / BlockQuartzChiseled, en_US.lang:26 «Certus Quartz
		//   Pillar» and :23 «Chiseled Certus Quartz Block»] → 26.1 AEBlockIds:113/114 quartz_pillar /
		//   chiseled_quartz_block, en_us.json:159/104 — the same two lines verbatim.
		map("tile.BlockQuartzPillar"            , "quartz_pillar");
		map("tile.BlockQuartzChiseled"          , "chiseled_quartz_block");
		// [PROVEN FROM THE PRIMARY SOURCE: rv3 BlockFluix, en_US.lang:21 «Fluix Block»] → 26.1 AEBlockIds:115
		//   fluix_block, en_us.json:131 «Fluix Block». Role: rv3 4 × crystalFluix (2×2) ⟺ 26.1 fluix_block.json.
		map("tile.BlockFluix"                   , "fluix_block");
		// [PROVEN FROM THE PRIMARY SOURCE: rv3 BlockQuartzGlass, en_US.lang:24 «Quartz Glass»] → 26.1 AEBlockIds:120
		//   quartz_glass, en_us.json:158 «Quartz Glass». Role cell for cell: rv3 decorative/quartzglass.recipe
		//   «dustQuartz glass dustQuartz / glass dustQuartz glass / dustQuartz glass dustQuartz» → 4 pcs ⟺
		//   26.1 quartz_glass.json «aba/bab/aba», a = #ae2:all_quartz_dust, b = #c:glass_blocks/cheap → 4 pcs.
		map("tile.BlockQuartzGlass"             , "quartz_glass");
		// [PROVEN FROM THE PRIMARY SOURCE: rv3 BlockQuartzLamp, en_US.lang:25 «Vibrant Quartz Glass»] → 26.1
		//   AEBlockIds:121 quartz_vibrant_glass, en_us.json:165 «Vibrant Quartz Glass». Role: rv3
		//   «glowstone_dust BlockQuartzGlass glowstone_dust» ⟺ 26.1 quartz_vibrant_glass.json «aba».
		map("tile.BlockQuartzLamp"              , "quartz_vibrant_glass");
		// [PROVEN FROM THE PRIMARY SOURCE: rv3 BlockQuartzTorch, en_US.lang:27 «Charged Quartz Fixture»] → 26.1
		//   AEBlockIds:51 quartz_fixture, en_us.json:157 «Charged Quartz Fixture» — name for name. Role matches
		//   together with the OUTPUT COUNT: rv3 decorative/fixtures.recipe «CertusQuartzCrystalCharged + iron_ingot
		//   → 2 BlockQuartzTorch» ⟺ 26.1 quartz_fixture.json «ab» → count 2. (In E2 this was inferred indirectly
		//   from OD.blockTorch at LoaderItemList:1780; now proven directly.)
		map("tile.BlockQuartzTorch"             , "quartz_fixture");
		// Stairs and slabs. [PROVEN FROM THE PRIMARY SOURCE] rv3 names are set by classes (appeng/block/stair/*.java)
		//   and the string parameter of AEBaseSlabBlock (ApiBlocks.java:244-251), and the rv3/26.1 human-readable
		//   names match verbatim: «Certus Quartz Stairs/Slabs» (en_US.lang:58/68) ⟺ en_us.json:164/163,
		//   «Certus Quartz Pillar…» :57/67 ⟺ :161/160, «Chiseled Certus Quartz…» :55/65 ⟺ :106/105,
		//   «Fluix Stairs/Slabs» :56/66 ⟺ :133/132. The rv3 input/output count (3 blocks → 6 slabs; a stair → 4)
		//   matches the vanilla 26.1 forms.
		map("tile.QuartzStairBlock"             , "quartz_stairs");
		map("tile.QuartzPillarStairBlock"       , "quartz_pillar_stairs");
		map("tile.ChiseledQuartzStairBlock"     , "chiseled_quartz_stairs");
		map("tile.FluixStairBlock"              , "fluix_stairs");
		map("tile.QuartzSlabBlock"              , "quartz_slab");
		map("tile.QuartzPillarSlabBlock"        , "quartz_pillar_slab");
		map("tile.ChiseledQuartzSlabBlock"      , "chiseled_quartz_slab");
		map("tile.FluixSlabBlock"               , "fluix_slab");
		// [PROVEN FROM THE PRIMARY SOURCE] Sky stone shapes. Which meta each stair and slab is pinned to
		//   is declared EXPLICITLY in rv3: ApiBlocks.java:231-234 «new SkyStoneStairBlock(skyStone, 0)»,
		//   «SkyStoneBlockStairBlock(skyStone, 1)», «SkyStoneBrickStairBlock(skyStone, 2)»,
		//   «SkyStoneSmallBrickStairBlock(skyStone, 3)»; ApiBlocks.java:244-247 — the same four metas for slabs.
		//   Human-readable names match 26.1 verbatim: «Sky Stone Stairs» (en_US.lang:62) ⟺ en_us.json:178
		//   sky_stone_stairs; «Sky Stone Block Stairs» (:59) ⟺ :189 smooth_sky_stone_stairs; «Sky Stone Brick
		//   Stairs» (:60) ⟺ :170; «Sky Stone Small Brick Stairs» (:61) ⟺ :176. Slabs — the same:
		//   «Sky Stone Block Slabs» (:69) ⟺ en_us.json:188 «Sky Stone Block Slab» smooth_sky_stone_slab.
		map("tile.SkyStoneStairBlock"           , "sky_stone_stairs");
		map("tile.SkyStoneBlockStairBlock"      , "smooth_sky_stone_stairs");
		map("tile.SkyStoneBrickStairBlock"      , "sky_stone_brick_stairs");
		map("tile.SkyStoneSmallBrickStairBlock" , "sky_stone_small_brick_stairs");
		map("tile.SkyStoneSlabBlock"            , "sky_stone_slab");
		map("tile.SkyStoneBlockSlabBlock"       , "smooth_sky_stone_slab");
		map("tile.SkyStoneBrickSlabBlock"       , "sky_stone_brick_slab");
		map("tile.SkyStoneSmallBrickSlabBlock"  , "sky_stone_small_brick_slab");

		// tile.BlockSkyStone — a block name whose meta was a SUBTYPE (four stones in one 1.7.10 block).
		// [PROVEN FROM THE PRIMARY SOURCE] The meaning of every meta is declared in rv3 BlockSkyStone.java:143-161: meta 1 →
		//   suffix «.Block», 2 → «.Brick», 3 → «.SmallBrick», anything else → the base name. Through en_US.lang:38-41 this
		//   gives: 0 «Sky Stone», 1 «Sky Stone Block», 2 «Sky Stone Brick», 3 «Sky Stone Small Brick». Exactly these
		//   four lines are in 26.1: en_us.json:167 sky_stone_block «Sky Stone», :186 smooth_sky_stone_block
		//   «Sky Stone Block», :168 sky_stone_brick, :174 sky_stone_small_brick. The reprocessing chain is the same:
		//   rv3 decorative/skystone.recipe «smelt 0 → 1; 1 → 2; 2 → 3» ⟺ 26.1 smelting/smooth_sky_stone_block.json
		//   (sky_stone_block → smooth), decorative/sky_stone_brick.json (smooth → brick),
		//   decorative/sky_stone_small_brick.json (brick → small).
		// W («any») is not a subtype — we return the family's base stone, same as CS.Flattened.
		map("tile.BlockSkyStone",  0, "sky_stone_block");
		map("tile.BlockSkyStone",  1, "smooth_sky_stone_block");
		map("tile.BlockSkyStone",  2, "sky_stone_brick");
		map("tile.BlockSkyStone",  3, "sky_stone_small_brick");
		map("tile.BlockSkyStone",  W, "sky_stone_block");

		// tile.BlockSkyChest — the second family, collapsed by meta. [PROVEN FROM THE PRIMARY SOURCE] rv3 en_US.lang:42-43:
		//   meta 0 «Sky Stone Chest», meta 1 «Sky Stone Block Chest»; rv3 misc/chests.recipe assembles the first from
		//   8 × BlockSkyStone:0, the second from 8 × BlockSkyStone:1. In 26.1 both lines are verbatim:
		//   en_us.json:172 sky_stone_chest «Sky Stone Chest», :187 smooth_sky_stone_chest «Sky Stone Block Chest»,
		//   and the recipes are the same — misc/chests_sky_stone.json (8 × sky_stone_block) and chests_smooth_sky_stone.json
		//   (8 × smooth_sky_stone_block). GT6 only calls W (Compat:116, mortarizing a chest into 8 blockDust) —
		//   W is not a subtype, we return the family's base member.
		map("tile.BlockSkyChest",  0, "sky_stone_chest");
		map("tile.BlockSkyChest",  1, "smooth_sky_stone_chest");
		map("tile.BlockSkyChest",  W, "sky_stone_chest");

		// Certus ores — no carrier. GT6 calls them with four different spellings, so all four are in the table:
		// no silent null is left for any of them. [PROVEN FROM THE PRIMARY SOURCE] the rv3 registry names are only
		// «tile.OreQuartz» and «tile.OreQuartzCharged» (classes appeng/block/solids/OreQuartz.java,
		// OreQuartzCharged.java + the AEBlockFeatureHandler:72 rule); the lowercase «o» spellings GT6 uses to
		// call them at LoaderItemList:589-590 never resolved even in 1.7.10.
		gone("tile.OreQuartz"                   , NO_ORE);
		gone("tile.oreQuartz"                   , NO_ORE_CASE);
		gone("tile.OreQuartzCharged"            , NO_ORE);
		gone("tile.oreQuartzCharged"            , NO_ORE_CASE);

		// ============================================================================================
		// STANDALONE ITEMS.
		// ============================================================================================
		// [PROVEN FROM THE PRIMARY SOURCE] Quartz tools. rv3 names are built by FeatureNameExtractor:60-67: class
		//   ToolQuartzWrench/ToolQuartzCuttingKnife + subname CertusQuartzTools|NetherQuartzTools → «Quartz»
		//   is replaced with «CertusQuartz»/«NetherQuartz». Human-readable names match 26.1 verbatim:
		//   «Certus Quartz Wrench» (en_US.lang) ⟺ en_us.json:749 certus_quartz_wrench; «Nether Quartz Wrench»
		//   ⟺ :875; «Certus Quartz Cutting Knife» ⟺ :743; «Nether Quartz Cutting Knife» ⟺ :870.
		map("item.ToolCertusQuartzWrench"       , "certus_quartz_wrench");
		map("item.ToolNetherQuartzWrench"       , "nether_quartz_wrench");
		map("item.ToolCertusQuartzCuttingKnife" , "certus_quartz_cutting_knife");
		map("item.ToolNetherQuartzCuttingKnife" , "nether_quartz_cutting_knife");
		// [PROVEN FROM THE PRIMARY SOURCE] rv3 «Wireless Terminal» / «Memory Card» / «View Cell» (en_US.lang:493/491/373)
		//   ⟺ 26.1 en_us.json:956 wireless_terminal, :864 memory_card, :944 view_cell — name for name.
		map("item.ToolWirelessTerminal"         , "wireless_terminal");
		map("item.ToolMemoryCard"               , "memory_card");
		map("item.ItemViewCell"                 , "view_cell");
		// [PROVEN FROM THE PRIMARY SOURCE] rv3 had EXACTLY ONE portable terminal — «ae2:ToolPortableCell»
		//   (en_US.lang:486 «Portable Cell»), and its recipe tools/network.recipe: «BlockChest + Cell1kPart +
		//   BlockEnergyCell». In 26.1 the family has split into 10 (item/fluid × 5 tiers), but there is exactly
		//   one direct heir to that same recipe: tools/portable_item_cell_1k.json = «ae2:me_chest + ae2:cell_component_1k
		//   + ae2:energy_cell + ae2:item_cell_housing» (AEItems.java:166). Three of the four inputs are the same
		//   items; the fourth is a housing that did not exist in the rv3 recipe (rv3 did not require it). The other
		//   nine members of the heir family have no rv3 counterpart at all — they did not exist there, and no GT6
		//   name points at them. GT6 calls this name once, from a book tooltip (LoaderBookList:372).
		map("item.ToolPortableCell"             , "portable_item_cell_1k");
		// [PROVEN FROM THE PRIMARY SOURCE] rv3 «1k/4k/16k/64k ME Storage Cell» (en_US.lang:367-370) ⟺ 26.1
		//   «1k…64k ME Item Storage Cell» (en_us.json:825/827/824/828): in 26.1 the word Item was added to the name
		//   because fluid cells appeared, which did not exist in rv3 (grepping «FluidCell» over rv3 is empty).
		//   Same recipe: rv3 network/cells/storage.recipe «QuartzGlass dustRedstone QuartzGlass / dustRedstone
		//   Cell1kPart dustRedstone / iron iron iron» ⟺ 26.1 network/cells/item_storage_cell_1k.json
		//   «aba/bcb/ded» with the same layout. What is actually registered is ITEM_CELL_*
		//   («item_storage_cell_1k», AEItems.java); the declared STORAGE_CELL_* do not make it into the registry.
		map("item.ItemBasicStorageCell.1k"      , "item_storage_cell_1k");
		map("item.ItemBasicStorageCell.4k"      , "item_storage_cell_4k");
		map("item.ItemBasicStorageCell.16k"     , "item_storage_cell_16k");
		map("item.ItemBasicStorageCell.64k"     , "item_storage_cell_64k");
		// [PROVEN FROM THE PRIMARY SOURCE] rv3 «Creative ME Storage Cell» (en_US.lang:371) ⟺ 26.1 en_us.json:757
		//   creative_storage_cell «Creative ME Storage Cell» (AEItems.java:257 CREATIVE_CELL) — name for name.
		map("item.ItemCreativeStorageCell"      , "creative_storage_cell");
		// [PROVEN FROM THE PRIMARY SOURCE] rv3 «2³/16³/128³ Spatial Storage Cell» (en_US.lang:465-467) ⟺ 26.1
		//   en_us.json:937/936/935 spatial_storage_cell_2/16/128 (AEItems.java:272-274) — three lines verbatim.
		map("item.ItemSpatialStorageCell.2Cubed"  , "spatial_storage_cell_2");
		map("item.ItemSpatialStorageCell.16Cubed" , "spatial_storage_cell_16");
		map("item.ItemSpatialStorageCell.128Cubed", "spatial_storage_cell_128");
		// [PROVEN FROM THE PRIMARY SOURCE, no carrier] the item existed in rv3 (ToolBiometricCard, tools/network.recipe,
		//   en_US.lang:494); in 26.1 the word «biometric» is absent from all of appeng/**.
		gone("item.ToolBiometricCard"           , NO_BIOMETRIC);
		// [PROVEN FROM THE PRIMARY SOURCE, no carrier] rv3 ItemCrystalSeed.java:62-64 sets the seed metas: CERTUS=0,
		//   NETHER=SINGLE_OFFSET=600, FLUIX=1200 — exactly the three numbers GT6 calls them with (Compat:82-84,
		//   92-98). In 26.1 there are no seeds: growth was replaced by budding blocks (AEBlockIds:39-42).
		gone("item.ItemCrystalSeed"             , NO_SEED);

		// ============================================================================================
		// item.ItemMultiPart — «cable bus part»; the meta was the part number.
		// [PROVEN FROM THE PRIMARY SOURCE: rv3 PartType.java:111 «CableAnchor( 120, … PartCableAnchor.class )»] —
		//   meta 120 is declared explicitly, nothing to guess. Name: en_US.lang:438 «Cable Anchor» ⟺ 26.1 en_us.json:729
		//   cable_anchor «Cable Anchor» (AEParts.java, AEPartIds.CABLE_ANCHOR). Role matches together with the count:
		//   rv3 network/parts/cable-anchor.recipe «metalIngots knife → 3 ItemPart.CableAnchor» ⟺ GT6 RM.sawing
		//   gives exactly 3 (Compat:67-78); 26.1 network/parts/cable_anchor.json — knife + #ae2:metal_ingots.
		// ============================================================================================
		map("item.ItemMultiPart", 120, "cable_anchor");

		// ============================================================================================
		// item.ItemMultiMaterial — rv3's catch-all item, meta = material. Eighteen metas that GT6 calls.
		// THE MEANING OF EVERY META is declared in the primary source as an enum: rv3 appeng/items/materials/MaterialType.java,
		// the number in parentheses next to the constant. Below, each line states its constant and that file's line.
		// ============================================================================================
		// [PROVEN FROM THE PRIMARY SOURCE: MaterialType.java:46 CertusQuartzCrystalCharged(1)] → 26.1 AEItemIds:196
		//   charged_certus_quartz_crystal (AEItems.java:204). Name for name: rv3 en_US.lang:398 «Charged Certus
		//   Quartz Crystal» ⟺ 26.1 en_us.json:750. Role also matches on the GT6 side: setTarget(OP.gem, ChargedCertusQuartz,
		//   meta 1) (LoaderUnificationTargets:832), OM.reg(OD.itemCertusQuartz, meta 1) (LoaderItemData:52).
		map("item.ItemMultiMaterial",  1, "charged_certus_quartz_crystal");
		// [PROVEN FROM THE PRIMARY SOURCE, no carrier: MaterialType.java:62-64 PurifiedCertusQuartzCrystal(10),
		//   PurifiedNetherQuartzCrystal(11), PurifiedFluixCrystal(12)] — these are «purified» crystals, NOT the plain ones.
		//   The primary source also confirms both GT6 chains: (a) rv3 ItemCrystalSeed.java:124-141 yields
		//   exactly purifiedCertus/Nether/Fluix from a grown seed — the same thing GT6's autoclave does (Compat:82-84);
		//   (b) rv3 decorative/crystals.recipe assembles BlockQuartz from 8 × PurifiedCertusQuartzCrystal — exactly
		//   the Compressor squeeze «8 × meta 10 → tile.BlockQuartz» that GT6 has (Compat:87). In 26.1 all three
		//   are declared (AEItemIds.java:203-205) but not registered: AEItems.java never references PURIFIED_*.
		gone("item.ItemMultiMaterial", 10, NO_PURIFIED);
		gone("item.ItemMultiMaterial", 11, NO_PURIFIED);
		gone("item.ItemMultiMaterial", 12, NO_PURIFIED);
		// [PROVEN FROM THE PRIMARY SOURCE: MaterialType.java:66-68 CalcProcessorPress(13), EngProcessorPress(14),
		//   LogicProcessorPress(15); :74 SiliconPress(19)] — four inscriber press shapes. Names are verbatim:
		//   rv3 en_US.lang:426/424/425 «Inscriber Calculation/Engineering/Logic Press», :430 «Inscriber Silicon
		//   Press» ⟺ 26.1 en_us.json:734/774/853/928 calculation_/engineering_/logic_processor_press,
		//   silicon_press. Same role: rv3 materials/presses.recipe «blockIron + press → the same press» ⟺ 26.1
		//   inscriber/*_press.json, and GT6 has this same duplication (Compat:62-65).
		map("item.ItemMultiMaterial", 13, "calculation_processor_press");
		map("item.ItemMultiMaterial", 14, "engineering_processor_press");
		map("item.ItemMultiMaterial", 15, "logic_processor_press");
		map("item.ItemMultiMaterial", 19, "silicon_press");
		// [PROVEN FROM THE PRIMARY SOURCE: MaterialType.java:70-72 CalcProcessorPrint(16), EngProcessorPrint(17),
		//   LogicProcessorPrint(18); :75 SiliconPrint(20)] — prints. Names are verbatim: rv3 en_US.lang:428/427/429
		//   «Printed Calculation/Engineering/Logic Circuit», :431 «Printed Silicon» ⟺ 26.1 en_us.json:903/904/
		//   905/906. Same role cell for cell: rv3 materials/circuits.recipe «PurifiedCertus + CalcPress →
		//   CalcPrint; gemDiamond + EngPress → EngPrint; ingotGold + LogicPress → LogicPrint; itemSilicon +
		//   SiliconPress → SiliconPrint» ⟺ 26.1 inscriber/*_print.json; the same four stampings appear in GT6
		//   (Compat:53-59), with a Greg plate instead of a crystal/ingot.
		map("item.ItemMultiMaterial", 16, "printed_calculation_processor");
		map("item.ItemMultiMaterial", 17, "printed_engineering_processor");
		map("item.ItemMultiMaterial", 18, "printed_logic_processor");
		map("item.ItemMultiMaterial", 20, "printed_silicon");
		// [PROVEN FROM THE PRIMARY SOURCE: MaterialType.java:77 NamePress(21)] → 26.1 AEItems.java:219 NAME_PRESS.
		//   Name for name: rv3 en_US.lang:432 «Inscriber Name Press» ⟺ 26.1 en_us.json:868.
		map("item.ItemMultiMaterial", 21, "name_press");
		// [PROVEN FROM THE PRIMARY SOURCE: MaterialType.java:79-81 LogicProcessor(22), CalcProcessor(23),
		//   EngProcessor(24)] — processors. Names are verbatim: rv3 en_US.lang «Logic/Calculation/Engineering
		//   Processor» ⟺ 26.1 en_us.json:852/733/773. Same role: rv3 materials/processors.recipe «dustRedstone
		//   + <type>Print + SiliconPrint → <type>Processor» ⟺ 26.1 inscriber/*_processor.json; GT6 has exactly
		//   these three triples (Compat:80-82).
		map("item.ItemMultiMaterial", 22, "logic_processor");
		map("item.ItemMultiMaterial", 23, "calculation_processor");
		map("item.ItemMultiMaterial", 24, "engineering_processor");
		// [PROVEN FROM THE PRIMARY SOURCE: MaterialType.java:101 EmptyStorageCell(39)] — «ME Storage Housing»
		//   (rv3 en_US.lang:400), a cell's housing. In 26.1 the entity SPLIT into item_cell_housing and
		//   fluid_cell_housing (AEItemIds:237-238, AEItems.java:242-243), and the ITEM one was chosen as the
		//   carrier on three independent grounds:
		//   (a) the recipe is inherited cell for cell by it specifically: rv3 network/cells/empty.recipe
		//       «QuartzGlass dustRedstone QuartzGlass / dustRedstone _ dustRedstone / ingotIron ingotIron
		//       ingotIron» ⟺ 26.1 network/cells/item_cell_housing.json «aba / b b / cdc», c = #c:ingots/iron;
		//       fluid_cell_housing.json's bottom row is «ccc» — solid copper, no iron at all;
		//   (b) fluid cells did not exist at all in rv3 (grepping «FluidCell» over appeng/** is empty), so
		//       fluid_cell_housing has nothing to inherit from — it is a new 26.1 entity;
		//   (c) everything rv3 did with the housing, 26.1 does with the item one: rv3 network/cells/storage.recipe
		//       «Cell1kPart + EmptyStorageCell → ItemBasicStorageCell.1k», rv3 network/cells/view.recipe
		//       «certusCrystal + EmptyStorageCell → ItemViewCell» ⟺ 26.1 tools/portable_item_cell_1k.json and
		//       the item_storage_cell_* family, whose fourth input is specifically ae2:item_cell_housing.
		//   GT6's role does not contradict this: the only mention is a book tooltip for category 46 «AE Cells»
		//   (LoaderBookList:385), and the item cell's housing is exactly an item of that category.
		map("item.ItemMultiMaterial", 39, "item_cell_housing");
		// [PROVEN FROM THE PRIMARY SOURCE: MaterialType.java:111 SkyDust(45)] → 26.1 AEItems.java:248 SKY_DUST.
		//   Name for name: rv3 en_US.lang:433 «Sky Stone Dust» ⟺ 26.1 en_us.json:930. Role also matches on the
		//   GT6 side: setTarget(OP.dust, MT.STONES.SkyStone, meta 45) (LoaderUnificationTargets:831); in rv3 the
		//   same dust comes from milling BlockSkyStone:0 (processing/grind.recipe).
		map("item.ItemMultiMaterial", 45, "sky_dust");
	}

	/** Whether the centre knows this (mod, name) pair — i.e. whether the {@code ST.make} funnel should hand it off. */
	public static boolean owns(ModData aMod, String aName) {
		if (aMod == null || aName == null || !MD.AE.mID.equals(aMod.mID)) return F;
		return SPLIT.contains(aName) || MAPPED.containsKey(aName) || GONE.containsKey(aName);
	}

	/** The 26.1 id path for a «rv2 name + meta» pair; {@code null} = no carrier, or not our name. */
	public static String path(String aName, long aMeta) {
		if (aName == null) return null;
		return SPLIT.contains(aName) ? MAPPED.get(key(aName, aMeta)) : MAPPED.get(aName);
	}

	/** Whether the pair has a carrier in AE2 26.1. The caller uses this to decide whether to register its entry. */
	public static boolean has(String aName, long aMeta) {
		return path(aName, aMeta) != null;
	}

	/** The REASON the carrier is missing; {@code null} = carrier exists, or not our name. */
	public static String reason(String aName, long aMeta) {
		if (aName == null || path(aName, aMeta) != null) return null;
		if (!SPLIT.contains(aName)) return GONE.get(aName);
		String rReason = GONE.get(key(aName, aMeta));
		return rReason != null ? rReason : "meta " + aMeta + " of «" + aName + "» is unknown to the centre (GT6 never asks for it)";
	}

	/**
	 * The ONLY way to get an AE2 stack from a 1.7.10 name. Called from the
	 * {@code ST.make(ModData, String, long, long)} funnel; itself hits the registry only through {@code ST.findItem}.
	 *
	 * <p>Meta: for split names the subtype is already expressed by the item ITSELF, so meta is not stamped on it
	 * (the {@code CS.Flattened} doctrine); {@code W} is not a subtype but «any», and survives the resolve as-is.
	 * For non-split names the meta passes through unchanged — exactly as the old path did.
	 */
	public static ItemStack make(String aName, long aSize, long aMeta) {
		if (!MD.AE.mLoaded || !GAPI_POST.mStartedPreInit) return null;
		String tPath = path(aName, aMeta);
		if (tPath == null) return null;
		Item tItem = ST.findItem(MD.AE.mID, tPath);
		if (tItem == null) return null;
		return ST.make_(tItem, aSize, SPLIT.contains(aName) && aMeta != W ? 0 : aMeta);
	}
}

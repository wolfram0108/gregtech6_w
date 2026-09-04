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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.Tags;

import gregapi.data.CS;
import gregapi.data.MD;
import gregapi.data.MT;
import gregapi.data.OD;
import gregapi.data.OP;
import gregapi.util.ST;
import gregapi.util.UT;

/**
 * F4/F1-b TAG BRIDGE — the ONLY place where the GT6 dictionary talks to the {@code c:} convention-tag
 * system (`decisions/F4-oredictionary.md` §4.4, `F1-item-metadata-model.md` §6.4 — the bridge is declared
 * and deferred there, built here).
 *
 * <p><b>What the engine changed.</b> In 1.7.10, inter-mod exchange went through a flat Forge dictionary: a foreign
 * mod called {@code OreDictionary.registerOre("ingotCopper", stack)}, Forge broadcast an {@code OreRegisterEvent},
 * and GT6 caught it and decided what to do with the item itself ({@code OreDictManager.onOreRegistration1}). In
 * 26.1.2 there is no Forge dictionary at all; the mods' common language is the {@code c:} namespace TAGS
 * ({@code Tags.java:1051} — {@code ItemTags.create(Identifier.fromNamespaceAndPath("c", name))}). The port had
 * already internalized the dictionary's storage ({@link OreDictionary}, F4 role A); what was missing was exactly
 * the TRANSPORT to foreign mods — this class provides it.
 *
 * <p><b>The bridge is two-way, but the sides are NOT symmetric — that is how the engine is built, not our choice.</b></p>
 *
 * <p><b>1. Inbound (full).</b> {@link #importFromTags()} reads the REAL tag registry
 * ({@code Registry.getTags()}, {@code Registry.java:143}), translates each convention tag into a GT6 oredict
 * name via the table below, and feeds the content into {@link OreDictionary#registerOre} — the SAME entry point
 * foreign mods used to arrive through in 1.7.10. From there the UNTOUCHED Gregorius machinery does everything: dedup,
 * broadcasting {@code OreRegisterEvent}, name parsing, {@code setTarget_} (unification), {@code triggerVisibility},
 * the {@code LoaderOreDictReRegistrations} re-registrations. There is not a single new semantic branch here.
 *
 * <p><b>2. Outbound (limited by the engine).</b> A tag is hung on a REGISTRY ENTRY ({@code Item}), while under the F1
 * decision (model B) GT6 has ONE {@code Item} per prefix, with the material living in a stack component
 * ({@code PrefixItem.java:118} {@code ST.make(this, 1, i)}, {@code ST.java:206} {@code meta_} = the
 * {@code SUBTYPE} component). So a per-material tag like {@code c:ingots/tin} has NOTHING to hang on: tagging
 * {@code gt.meta.ingot} would declare an iridium ingot to be tin. So outbound we give exactly what can be
 * expressed HONESTLY:
 * <ul>
 * <li>MATERIAL-AGNOSTIC groups ({@code c:ingots}, {@code c:dusts}, {@code c:gems}, {@code c:nuggets},
 *     {@code c:rods}, {@code c:ores}, {@code c:storage_blocks}, {@code c:raw_materials}) — for these
 *     "one Item per prefix" is an EXACT statement, not an approximation; they are emitted by the datagen provider
 *     {@code gregapi.data.GT6ConventionTags}, which reads the table FROM HERE;</li>
 * <li>per-material tags fall out ON THEIR OWN, through Gregorius's own unification: the moment the inbound side
 *     feeds a foreign/vanilla item under the name {@code ingotCopper}, {@code setTarget_} makes IT
 *     the material's unification target — so the item the player finds in the world is exactly that one, already
 *     sitting in {@code c:ingots/copper} thanks to its own owner's data. That is exactly how GT6 worked in 1.7.10
 *     (copper/iron/certus were foreign items).</li>
 * </ul>
 * The remainder (a material nobody but GT6 has) cannot be expressed by a per-material tag — this is an
 * engine-forced F1-b boundary, not a forgotten branch.
 *
 * <p><b>The naming rule lives here, and only here.</b> A GT6 name is {@code prefix.mNameInternal +
 * material.mNameInternal} ({@code OreDictManager.java:527,752} — verbatim 1:1 with the original
 * {@code gt6-original/.../OreDictManager.java:527,752}). A convention name is {@code c:<group>/<material>}.
 * So exactly two things are needed: a "group <-> prefix" table and an "material name <-> snake_case" function
 * ({@link #snake}/{@link #camel}). Anything this rule cannot describe lives in the EXPLICIT list
 * {@link #sExceptions}, not scattered across the code.
 *
 * <p><b>Convention names are never invented.</b> Every table row is confirmed by a file on disk:
 * {@code neoforge-decompiled/.../common/Tags.java} (:511 dusts, :660 gems, :686 ingots, :703 nuggets,
 * :744/:201 ores, :798 raw_materials, :805 rods, :855/:280 storage_blocks) and
 * {@code reference/mods/Applied-Energistics-2/.../ConventionTags.java} (:61 {@code c:silicon}, :70/:73
 * certus, :81/:82 fluix, :99 sky_stone, :101 rods/wooden, :106 glass_blocks/cheap). The only row WITHOUT
 * disk confirmation is the {@code plates} group: it was added by spec requirement and only works INBOUND
 * (we read the tag if someone declared it); {@code c:plates} is NOT written outbound, so as not to invent a name
 * whose existence cannot be proven.
 */
public class OreDictTags {
	private OreDictTags() {/* static-only, like the dictionary's other centers */}

	/** The convention tag namespace — {@code Tags.java:1051-1053}. */
	public static final String CONVENTION = "c";

	/** The SINGLE "convention tag group -> GT6 prefix" table. Both sides of the bridge read IT. */
	private static Map<String, OreDictPrefix> sGroupToPrefix = null;
	/** The reverse table (outbound side): prefix -> group. Built from the same map, not a copy of it. */
	private static Map<OreDictPrefix, String> sPrefixToGroup = null;
	/** The EXPLICIT exception list: a tag's convention path (without {@code c:}) -> the READY GT6 oredict name. */
	private static Map<String, String> sExceptions = null;

	/** Lazy build: the table references {@link OP}, which forbids creating prefixes after Init
	 *  ({@code OreDictPrefix.java:105}) — this class's static initializer could trigger {@code OP.<clinit>}
	 *  in the wrong phase. Deferred building removes the class-ordering question entirely. */
	private static void build() {
		if (sGroupToPrefix != null) return;
		Map<String, OreDictPrefix> tGroups = new LinkedHashMap<>();
		// Tags.java:686 c:ingots/*        · AE2 ConventionTags:84,87,90 (copper/gold/iron)
		tGroups.put("ingots"        , OP.ingot );
		// Tags.java:703 c:nuggets/*       · AE2's Matter Cannon ammo — 67 materials
		tGroups.put("nuggets"       , OP.nugget);
		// Tags.java:511 c:dusts/*         · AE2 ConventionTags:73,81,93,94,97,99
		tGroups.put("dusts"         , OP.dust  );
		// Tags.java:660 c:gems/*          · AE2 ConventionTags:70,82,92
		tGroups.put("gems"          , OP.gem   );
		// Tags.java:805 c:rods/*          · AE2 ConventionTags:101
		tGroups.put("rods"          , OP.stick );
		// Tags.java:744 c:ores/*          · Tags.java:201 (the block half)
		tGroups.put("ores"          , OP.ore   );
		// Tags.java:798 c:raw_materials/* — "raw ore" from a future vanilla version, in GT6 this is OP.oreRaw (OP.java:137)
		tGroups.put("raw_materials" , OP.oreRaw);
		// Tags.java:855 c:storage_blocks/* — takes the GENERIC block prefix (OP.java:376), not blockSolid/blockGem/
		// blockDust: breaking the material down by the concrete block kind is Gregorius's own job — LoaderOreDictReRegistrations:230-241
		// ("blockCertusQuartz" -> "blockGemCertusQuartz"). The bridge speaks his language rather than deciding for him.
		tGroups.put("storage_blocks", OP.block );
		// plates — WITHOUT disk confirmation (no such group in Tags.java, AE2 does not ask for it). INBOUND only: if
		// nobody declared the tag, the branch stays silent; c:plates is not written outbound (see the class javadoc).
		tGroups.put("plates"        , OP.plate );
		sGroupToPrefix = tGroups;

		Map<OreDictPrefix, String> tBack = new LinkedHashMap<>();
		for (Map.Entry<String, OreDictPrefix> tEntry : tGroups.entrySet()) tBack.putIfAbsent(tEntry.getValue(), tEntry.getKey());
		sPrefixToGroup = tBack;

		Map<String, String> tExceptions = new LinkedHashMap<>();
		// c:silicon — a tag WITHOUT a group (AE2 ConventionTags:61, the inscriber's input). The GT6 name for the same
		// substance is OD.itemSilicon (OD.java:235), which is also what it was called in 1.7.10.
		tExceptions.put("silicon"           , OD.itemSilicon.toString());
		// c:rods/wooden (Tags.java:812) — "wooden" is not a GT6 material name; Gregorius's wooden stick is stickWood
		// (OD.java, an entry in the vanilla dictionary OreDictionary.java:165).
		tExceptions.put("rods/wooden"       , OD.stickWood.toString());
		// c:glass_blocks/cheap (Tags.java:673; AE2 ConventionTags:106 — recipe inputs hang off it) — "cheap" is not a
		// material either; for Gregorius it is blockGlass (OreDictionary.java:189-190: colorless + the whole colored
		// range). We deliberately take the "cheap glass" SUBTAG, not the PARENT c:glass_blocks (Tags.java:668): that one
		// is wider by tinted_glass, which did not exist in 1.7.10 and is not glass in the sense of GT6 recipes — the
		// bridge has no right to widen blockGlass's membership beyond the original.
		tExceptions.put("glass_blocks/cheap", OD.blockGlass.toString());
		// c:ingots/redstone_alloy — by the naming rule this would be RedstoneAlloy, EnderIO's alloy (MT.java:1753: Si +
		// Redstone, unification target LoaderUnificationTargets.java:876). But the tag is populated by More Red, and it
		// smelts its ingot from COPPER — its own tags/item/red_alloyable_ingots.json is #c:ingots/copper — meaning by
		// composition this is Gregorius's RedAlloy (MT.java:1749: Cu + Redstone), RedPower's alloy, whose successor the
		// mod is. The tag's name and its actual substance diverged for that tag's author; without this line the copper
		// alloy would be filed as silicon. The condition is on the mod, not the bare name: while nobody populates that tag, the naming rule is correct on its own.
		if (MD.MR.mLoaded) tExceptions.put("ingots/redstone_alloy", OP.ingot.mNameInternal + MT.RedAlloy.mNameInternal);
		sExceptions = tExceptions;
	}

	// ================================================================================================
	// NAMING RULE — one place, both sides
	// ================================================================================================

	/** {@code CertusQuartz} -> {@code certus_quartz}. The inverse of {@link #camel}. */
	public static String snake(String aCamel) {
		if (UT.Code.stringInvalid(aCamel)) return "";
		StringBuilder rName = new StringBuilder(aCamel.length() + 4);
		for (int i = 0; i < aCamel.length(); i++) {
			char tChar = aCamel.charAt(i);
			if (i > 0 && Character.isUpperCase(tChar)) rName.append('_');
			rName.append(Character.toLowerCase(tChar));
		}
		return rName.toString();
	}

	/** {@code certus_quartz} -> {@code CertusQuartz}. The inverse of {@link #snake}. */
	public static String camel(String aSnake) {
		if (UT.Code.stringInvalid(aSnake)) return "";
		StringBuilder rName = new StringBuilder(aSnake.length());
		for (String tPart : aSnake.split("_")) rName.append(UT.Code.capitalise(tPart));
		return rName.toString();
	}

	/** Outbound name: a GT6 pair -> the full convention tag name ({@code c:ingots/iron}), or {@code null}
	 *  when either this prefix has no convention group OR the material name does not survive the round trip
	 *  {@link #snake}->{@link #camel} (in which case no convention name exists for it, and inventing
	 *  one is forbidden). */
	public static String tagName(OreDictPrefix aPrefix, OreDictMaterial aMaterial) {
		build();
		if (aPrefix == null || aMaterial == null) return null;
		String tGroup = sPrefixToGroup.get(aPrefix);
		if (tGroup == null) return null;
		String tPath = snake(aMaterial.mNameInternal);
		if (!camel(tPath).equals(aMaterial.mNameInternal)) return null;
		return CONVENTION + ":" + tGroup + "/" + tPath;
	}

	/** Inbound name: a convention tag -> a GT6 oredict name ({@code c:ingots/iron} -> {@code ingotIron}), or
	 *  {@code null} when the tag is not a convention one or its group is unknown to the bridge. */
	public static String oreName(Identifier aTag) {
		build();
		if (aTag == null || !CONVENTION.equals(aTag.getNamespace())) return null;
		String tPath = aTag.getPath();
		String tException = sExceptions.get(tPath);
		if (tException != null) return tException;
		int tSlash = tPath.indexOf('/');
		if (tSlash <= 0 || tSlash + 1 >= tPath.length()) return null;
		OreDictPrefix tPrefix = sGroupToPrefix.get(tPath.substring(0, tSlash));
		if (tPrefix == null) return null;
		String tMaterial = camel(tPath.substring(tSlash + 1));
		if (tMaterial.isEmpty()) return null;
		return tPrefix.mNameInternal + tMaterial;
	}

	// ================================================================================================
	// OUTBOUND SIDE — material-agnostic groups (what is expressible on a registry entry)
	// ================================================================================================

	/** Prefix -> the material-agnostic ITEM tag, or {@code null}. There must be no second copy of this link
	 *  in the tree: both the datagen provider and the stand judge read it. */
	public static TagKey<Item> groupItemTag(OreDictPrefix aPrefix) {
		if (aPrefix == null) return null;
		if (aPrefix == OP.ingot ) return Tags.Items.INGOTS;         // Tags.java:686
		if (aPrefix == OP.nugget) return Tags.Items.NUGGETS;        // Tags.java:703
		if (aPrefix == OP.dust  ) return Tags.Items.DUSTS;          // Tags.java:511
		if (aPrefix == OP.gem   ) return Tags.Items.GEMS;           // Tags.java:660
		if (aPrefix == OP.stick ) return Tags.Items.RODS;           // Tags.java:805
		if (aPrefix == OP.oreRaw) return Tags.Items.RAW_MATERIALS;  // Tags.java:798
		if (isStorageBlock(aPrefix)) return Tags.Items.STORAGE_BLOCKS; // Tags.java:855
		if (isOre(aPrefix)) return Tags.Items.ORES;                 // Tags.java:744
		return null;
	}

	/** The same for BLOCK: {@code c:ores} and {@code c:storage_blocks} also exist in the block half
	 *  ({@code Tags.java:201,280}), while ingots/dusts/nuggets are never blocks. */
	public static TagKey<Block> groupBlockTag(OreDictPrefix aPrefix) {
		if (aPrefix == null) return null;
		if (isStorageBlock(aPrefix)) return Tags.Blocks.STORAGE_BLOCKS; // Tags.java:280
		if (isOre(aPrefix)) return Tags.Blocks.ORES;                    // Tags.java:201
		return null;
	}

	/** GT6 storage blocks — by an EXPLICIT list (OP.java:349,352,355 and neighbors), not by the prefix's tag data:
	 *  {@code STORAGE_BASED} also covers crates ({@code crateGtRaw}, OP.java:336), which are not storage in the
	 *  {@code c:storage_blocks} sense. */
	private static boolean isStorageBlock(OreDictPrefix aPrefix) {
		return aPrefix == OP.blockSolid || aPrefix == OP.blockGem || aPrefix == OP.blockDust || aPrefix == OP.blockIngot || aPrefix == OP.blockRaw;
	}

	/** GT6 ore prefixes — the same explicit list as in the ore loader ({@code Loader_Ores.java:52-71}). */
	private static boolean isOre(OreDictPrefix aPrefix) {
		return aPrefix == OP.ore || aPrefix == OP.oreSmall || aPrefix == OP.oreVanillastone || aPrefix == OP.oreSandstone
			|| aPrefix == OP.oreNetherrack || aPrefix == OP.oreEndstone || aPrefix == OP.oreGravel || aPrefix == OP.oreSand
			|| aPrefix == OP.oreRedSand || aPrefix == OP.oreBedrock;
	}

	// ================================================================================================
	// INBOUND SIDE — foreign items from tags into the GT6 dictionary
	// ================================================================================================

	/** How many entries the bridge fed into the dictionary on the last {@link #importFromTags} call (for the stand judge). */
	public static int sImportedEntries = 0;
	/** How many convention tags the bridge recognized on the last call (for the stand judge). */
	public static int sImportedTags = 0;
	/** How many VANILLA entries the bridge skipped, leaving them to role B ({@link OreDictionary#initVanillaEntries}) — for the stand judge. */
	public static int sSkippedVanilla = 0;

	/**
	 * Reads the REAL tag registry and feeds foreign items into the GT6 dictionary under GT6's own names.
	 *
	 * <p>Call timing is the start of the {@code GT_API.runDeferredItemInit} window, right after
	 * {@link OreDictionary#initVanillaEntries} and BEFORE GT6's own items register. This is not a matter of
	 * convenience but a semantic requirement: {@code setTarget_(..., aOverwrite=F)} ({@code OreDictManager.java:471})
	 * keeps the FIRST-registered item as the unification target — so a foreign item must arrive before ours,
	 * exactly as mods that loaded before GT6 used to in 1.7.10. Tags are already bound at this point:
	 * {@code WorldLoader.java:80} ({@code updateComponentsAndStaticRegistryTags}) runs BEFORE
	 * {@code MinecraftServer.loadLevel} ({@code MinecraftServer.java:403-411}), inside which {@code LevelEvent.Load} fires.
	 *
	 * <p>GT6's own items are NOT fed from tags: their material lives in a stack component, while a tag only gives a
	 * bare registry entry — a stack with meta-0 would mean {@code MT.Empty} and lie to the dictionary. GT6's own
	 * items arrive through their own channel ({@code PrefixItem.runDeferred}). Filtering uses the GT6 criterion
	 * {@code ST.isGT} ({@code ST.java:143}).
	 *
	 * <p><b>VANILLA items from tags are not fed in either — and that is the same role split, just seen from the
	 * other side.</b> Vanilla content is carried by role B of this very same adapter
	 * ({@link OreDictionary#initVanillaEntries}): there it is listed by name, using GREGORIUS'S names and with
	 * deliberate omissions. The tag bridge exists FOR FOREIGN mods — exactly the stream that used to reach GT6
	 * through the Forge dictionary in 1.7.10. Two mechanisms registering the same thing is not "more reliable", it
	 * is a race, and the bridge wins it: it runs at the start of the window, so its entry claims both the
	 * unification target ({@code setTarget_(..., aOverwrite=F)}, {@code OreDictManager.java:471}) and the item's
	 * passport ({@code addItemData_} — first-wins, {@code OreDictManager.java:667-670}) ahead of the side that
	 * decided deliberately: ADAPT-014 keeps GREGORIUS'S own ingot as the copper material's target and hands the
	 * vanilla copper ore the passport {@code oreVanillastone}/{@code oreDeepslate}
	 * ({@code Loader_Recipes_Vanilla.java:1100-1106,1181-1183}). On top of that the bridge speaks in TAG names, not
	 * Gregorius's names, which shows immediately on vanilla: {@code c:ores/netherite_scrap} would give
	 * "oreNetheriteScrap" where Gregorius calls it {@code oreAncientDebris}, {@code c:rods/breeze} would give
	 * "stickBreeze" for a material that does not exist, and {@code c:gems/prismarine} would give "gemPrismarine" for
	 * exactly the item role B REFUSED to list. Vanilla content is recognized the same way Gregorius himself
	 * distinguishes it everywhere — {@link gregapi.data.MD#MC}{@code .owns} (cf. {@code OreDictPrefix.java:491}) —
	 * not by a separate exception list: the rule here is semantic ("vanilla is not the tag bridge's territory"), the
	 * named list is role B's job.
	 *
	 * <p>Idempotent: a repeat call is absorbed by {@link OreDictionary#registerOre}'s dedup (the same hash bucket
	 * Forge used), so re-entering a world does not duplicate entries.
	 */
	public static int importFromTags() {
		build();
		int tTags = 0, tEntries = 0, tSkippedGT = 0, tSkippedMC = 0;
		Map<String, Integer> tByGroup = new TreeMap<>();
		try {
			for (HolderSet.Named<Item> tTag : BuiltInRegistries.ITEM.getTags().toList()) {
				Identifier tID = tTag.key().location();
				String tOreName = oreName(tID);
				if (tOreName == null) continue;
				tTags++;
				String tGroup = tID.getPath().indexOf('/') > 0 ? tID.getPath().substring(0, tID.getPath().indexOf('/')) : tID.getPath();
				for (Holder<Item> tHolder : tTag) {
					try {
						Item tItem = tHolder.value();
						if (tItem == null || tItem == Items.AIR) continue;
						if (ST.isGT(tItem)) {tSkippedGT++; continue;}
						// vanilla is not the tag bridge's territory, role B carries it (see the method javadoc)
						if (MD.MC.owns(tItem)) {tSkippedMC++; continue;}
						ItemStack tStack = ST.make(tItem, 1, 0);
						if (ST.invalid(tStack)) continue;
						OreDictionary.registerOre(tOreName, tStack);
						tEntries++;
						tByGroup.merge(tGroup, 1, Integer::sum);
					} catch(Throwable e) {e.printStackTrace(CS.ERR);}
				}
			}
		} catch(Throwable e) {e.printStackTrace(CS.ERR);}
		sImportedTags = tTags;
		sImportedEntries = tEntries;
		sSkippedVanilla = tSkippedMC;
		CS.OUT.println("GT6 F1-b tag bridge (inbound): convention tags recognized " + tTags + ", entries fed into the dictionary " + tEntries
			+ ", GT6 items skipped (material lives in the component, arrive via their own channel) " + tSkippedGT
			+ ", vanilla items skipped (carried by role B OreDictionary.initVanillaEntries) " + tSkippedMC + "; by group " + tByGroup);
		return tEntries;
	}
}

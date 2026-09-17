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

import com.mojang.datafixers.util.Pair;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.common.Tags;

import gregapi.data.CS;
import gregapi.data.MD;
import gregapi.data.MT;
import gregapi.data.OD;
import gregapi.data.OP;
import gregapi.util.ST;
import gregapi.util.UT;

/** The only place GT6's dictionary talks to Forge's forge: convention tags, replacing 1.7.10's flat OreDictionary as
 *  the cross-mod language here; the bridge is two-way but asymmetric, since GT6 can only honestly tag by group outward. */
public class OreDictTags {
	private OreDictTags() {/* Static-only, like the dictionary's other centers. */}

	/** The convention-tag namespace on this engine version is forge:, confirmed by a live mod of the same version. */
	public static final String CONVENTION = "forge";

	/** The only table mapping a convention-tag group to a GT6 prefix; both directions of the bridge read this same table. */
	private static Map<String, OreDictPrefix> sGroupToPrefix = null;
	/** The reverse table (prefix to group), built from the same map rather than kept as a separate copy. */
	private static Map<OreDictPrefix, String> sPrefixToGroup = null;
	/** An explicit exceptions list for tag paths that don't follow the group/prefix rule below. */
	private static Map<String, String> sExceptions = null;

	/** Built lazily because the table references OP, which forbids new prefixes after Init; a static initializer here
	 *  could trigger OP's own class-init too early, so building is deferred instead of risking that ordering. */
	private static void build() {
		if (sGroupToPrefix != null) return;
		Map<String, OreDictPrefix> tGroups = new LinkedHashMap<>();
		// Tags.java:310 forge:ingots/*   · AE2 ConventionTags:76 (copper)
		tGroups.put("ingots"        , OP.ingot );
		// forge:nuggets/* is also AE2's Matter Cannon ammunition, across 67 materials.
		tGroups.put("nuggets"       , OP.nugget);
		// Tags.java:220 forge:dusts/*    · AE2 ConventionTags:64,73 (certus_quartz, fluix)
		tGroups.put("dusts"         , OP.dust  );
		// Tags.java:256 forge:gems/*     · AE2 ConventionTags:61,74 (certus_quartz, fluix)
		tGroups.put("gems"          , OP.gem   );
		// Tags.java:376 forge:rods/*     · AE2 ConventionTags:93 (rods/wooden)
		tGroups.put("rods"          , OP.stick );
		// forge:ores/* has a block-side counterpart in the same tag file.
		tGroups.put("ores"          , OP.ore   );
		// forge:raw_materials/* is vanilla's newer 'raw ore' concept; GT6's own equivalent prefix is OP.oreRaw.
		tGroups.put("raw_materials" , OP.oreRaw);
		// Takes the general block prefix, not a specific one like blockGem: sorting a material into its exact block kind is
		// Greg's own job elsewhere, and the bridge only needs to speak his language, not decide it.
		tGroups.put("storage_blocks", OP.block );
		// This group has no on-disk confirmation anywhere, so it's input-only: read if someone declares it, never written outward.
		tGroups.put("plates"        , OP.plate );
		sGroupToPrefix = tGroups;

		Map<OreDictPrefix, String> tBack = new LinkedHashMap<>();
		for (Map.Entry<String, OreDictPrefix> tEntry : tGroups.entrySet()) tBack.putIfAbsent(tEntry.getValue(), tEntry.getKey());
		sPrefixToGroup = tBack;

		Map<String, String> tExceptions = new LinkedHashMap<>();
		// forge:silicon has no group of its own; GT6's name for the same substance, OD.itemSilicon, is unchanged since 1.7.10.
		tExceptions.put("silicon"           , OD.itemSilicon.toString());
		// 'wooden' isn't a GT6 material name; Greg's own name for the same wooden stick is stickWood.
		tExceptions.put("rods/wooden"       , OD.stickWood.toString());
		// The tag's name would resolve to a silicon alloy by the naming rule, but the mod that actually fills it makes this
		// ingot from copper -- it's really Greg's own RedAlloy, and without this exception it would be mislabeled.
		if (MD.MR.mLoaded) tExceptions.put("ingots/redstone_alloy", OP.ingot.mNameInternal + MT.RedAlloy.mNameInternal);
		sExceptions = tExceptions;
	}

	// ================================================================================================
	// The naming rule lives here, in one place read by both directions of the bridge.
	// ================================================================================================

	/** CertusQuartz becomes certus_quartz; the inverse of camel below. */
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

	/** certus_quartz becomes CertusQuartz; the inverse of snake above. */
	public static String camel(String aSnake) {
		if (UT.Code.stringInvalid(aSnake)) return "";
		StringBuilder rName = new StringBuilder(aSnake.length());
		for (String tPart : aSnake.split("_")) rName.append(UT.Code.capitalise(tPart));
		return rName.toString();
	}

	/** Outgoing name: a GT6 pair to its full convention tag, or null if the prefix has no group or the material's name
	 *  doesn't survive the snake/camel round trip -- meaning no honest convention name exists for it at all. */
	public static String tagName(OreDictPrefix aPrefix, OreDictMaterial aMaterial) {
		build();
		if (aPrefix == null || aMaterial == null) return null;
		String tGroup = sPrefixToGroup.get(aPrefix);
		if (tGroup == null) return null;
		String tPath = snake(aMaterial.mNameInternal);
		if (!camel(tPath).equals(aMaterial.mNameInternal)) return null;
		return CONVENTION + ":" + tGroup + "/" + tPath;
	}

	/** Incoming name: a convention tag to its GT6 oredict name, or null if the tag isn't conventional or its group is unknown. */
	public static String oreName(ResourceLocation aTag) {
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
	// Outgoing side: material-agnostic groups only, the only thing a registry entry can honestly express.
	// ================================================================================================

	/** Prefix to material-agnostic item tag, or null; both the datagen provider and the stand judge read this same link,
	 *  so a second copy of it shouldn't exist anywhere else. */
	public static TagKey<Item> groupItemTag(OreDictPrefix aPrefix) {
		if (aPrefix == null) return null;
		if (aPrefix == OP.ingot ) return Tags.Items.INGOTS;         // Tags.java:310
		if (aPrefix == OP.nugget) return Tags.Items.NUGGETS;        // Tags.java:321
		if (aPrefix == OP.dust  ) return Tags.Items.DUSTS;          // Tags.java:220
		if (aPrefix == OP.gem   ) return Tags.Items.GEMS;           // Tags.java:256
		if (aPrefix == OP.stick ) return Tags.Items.RODS;           // Tags.java:376
		if (aPrefix == OP.oreRaw) return Tags.Items.RAW_MATERIALS;  // Tags.java:372
		if (isStorageBlock(aPrefix)) return Tags.Items.STORAGE_BLOCKS; // Tags.java:395
		if (isOre(aPrefix)) return Tags.Items.ORES;                 // Tags.java:349
		return null;
	}

	/** Same for the block side: forge:ores and forge:storage_blocks exist there too, but ingots/dusts/nuggets never are blocks. */
	public static TagKey<Block> groupBlockTag(OreDictPrefix aPrefix) {
		if (aPrefix == null) return null;
		if (isStorageBlock(aPrefix)) return Tags.Blocks.STORAGE_BLOCKS; // Tags.java:159
		if (isOre(aPrefix)) return Tags.Blocks.ORES;                    // Tags.java:127
		return null;
	}

	/** Listed explicitly rather than derived from prefix tag data: STORAGE_BASED also covers crates, which aren't
	 *  storage blocks in the forge:storage_blocks sense at all. */
	private static boolean isStorageBlock(OreDictPrefix aPrefix) {
		return aPrefix == OP.blockSolid || aPrefix == OP.blockGem || aPrefix == OP.blockDust || aPrefix == OP.blockIngot || aPrefix == OP.blockRaw;
	}

	/** GT6's ore prefixes, kept as the same explicit list the ore loader itself already uses. */
	private static boolean isOre(OreDictPrefix aPrefix) {
		return aPrefix == OP.ore || aPrefix == OP.oreSmall || aPrefix == OP.oreVanillastone || aPrefix == OP.oreSandstone
			|| aPrefix == OP.oreNetherrack || aPrefix == OP.oreEndstone || aPrefix == OP.oreGravel || aPrefix == OP.oreSand
			|| aPrefix == OP.oreRedSand || aPrefix == OP.oreBedrock;
	}

	// ================================================================================================
	// Incoming side: foreign items from tags into GT6's own dictionary.
	// ================================================================================================

	/** Entry count the bridge fed into the dictionary on its last call, read by the stand judge. */
	public static int sImportedEntries = 0;
	/** Tag count the bridge recognized on its last call, read by the stand judge. */
	public static int sImportedTags = 0;
	/** Vanilla entry count the bridge skipped, leaving them to the other role, read by the stand judge. */
	public static int sSkippedVanilla = 0;

	/** Runs at the very start of deferred item-init, before GT6's own items register, because unification keeps the FIRST
	 *  registrant as its target -- a foreign item must win that race, exactly as early-loading mods did back in 1.7.10. */
	public static int importFromTags() {
		build();
		int tTags = 0, tEntries = 0, tSkippedGT = 0, tSkippedMC = 0;
		Map<String, Integer> tByGroup = new TreeMap<>();
		try {
			for (Pair<TagKey<Item>, HolderSet.Named<Item>> tPair : BuiltInRegistries.ITEM.getTags().toList()) {
				ResourceLocation tID = tPair.getFirst().location();
				HolderSet.Named<Item> tTag = tPair.getSecond();
				String tOreName = oreName(tID);
				if (tOreName == null) continue;
				tTags++;
				String tGroup = tID.getPath().indexOf('/') > 0 ? tID.getPath().substring(0, tID.getPath().indexOf('/')) : tID.getPath();
				for (Holder<Item> tHolder : tTag) {
					try {
						Item tItem = tHolder.value();
						if (tItem == null || tItem == Items.AIR) continue;
						if (ST.isGT(tItem)) {tSkippedGT++; continue;}
						// Vanilla content isn't this bridge's territory; it's handled by the dictionary's other role instead.
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
		CS.OUT.println("GT6 F1-b тег-мост (внутрь): конвенционных тегов опознано " + tTags + ", записей подано в словарь " + tEntries
			+ ", предметов GT6 пропущено (материал в компоненте, приходят своим каналом) " + tSkippedGT
			+ ", ванильных пропущено (ведёт роль-B OreDictionary.initVanillaEntries) " + tSkippedMC + "; по группам " + tByGroup);
		return tEntries;
	}
}

/**
 * Copyright (c) 2026 wolfram0108
 *
 * COMPILE-TIME STAND-IN — NOT THIRD-PARTY CODE.
 *
 * This declaration was written from scratch for the GregTech 6 NeoForge port
 * (https://github.com/wolfram0108/gregtech6_w). It contains no code from the project
 * that owns this package name, and no part of it was copied or decompiled from that
 * project: it declares only the members GregTech 6 itself implements or calls, so that
 * the port compiles while integration with that mod stays deferred.
 *
 * The original package name is kept deliberately, because GregTech 6 implements these
 * types verbatim and the port does not alter the code Gregorius Techneticies wrote.
 * Removing these classes from the build is not possible: 66 classes of the mod extend
 * or implement them, and the JVM requires the type to load the implementing class.
 *
 * All names, trademarks and rights in the project this package belongs to remain with
 * its authors. See src/compat-mirror/README.md and NOTICE.
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

// Package is gt6mirror.minecraftforge.common, not net.minecraftforge.common: the real forge module and
// gregtech6 would otherwise export the same package (JPMS split); repackaged, not deleted, since runtime uses it.
package gt6mirror.minecraftforge.common;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.EmptyLootItem;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;
import net.minecraftforge.server.ServerLifecycleHooks;

/** Sole center of the loot-table adaptation: 1.7.10's ChestGenHooks/WeightedRandomChestContent are reproduced here,
 *  and field/method names must match 1.7.10-Forge exactly, since GT6 code reflects on them by name. */
public class ChestGenHooks {
	public static final String
		MINESHAFT_CORRIDOR       = "mineshaftCorridor",
		PYRAMID_DESERT_CHEST     = "pyramidDesertyChest",
		PYRAMID_JUNGLE_CHEST     = "pyramidJungleChest",
		PYRAMID_JUNGLE_DISPENSER = "pyramidJungleDispenser",
		STRONGHOLD_CORRIDOR      = "strongholdCorridor",
		STRONGHOLD_LIBRARY       = "strongholdLibrary",
		STRONGHOLD_CROSSING      = "strongholdCrossing",
		VILLAGE_BLACKSMITH       = "villageBlacksmith",
		BONUS_CHEST              = "bonusChest",
		DUNGEON_CHEST            = "dungeonChest";

	/** Maps each GT6 chest category to a neo vanilla loot table; villageBlacksmith maps to weaponsmith,
	 *  the closest match after neo split the village loot into twelve separate tables. */
	private static final Map<String, ResourceLocation> NEO_TABLE = new LinkedHashMap<>();
	/** The total weight of 1.7.10's vanilla chest contents per category, computed from the original
	 *  structure-generation data, used as the weight of an "empty" entry so GT items keep the exact 1.7.10 odds. */
	private static final Map<String, Integer> VANILLA_WEIGHT_1710 = new HashMap<>();
	static {
		NEO_TABLE.put(DUNGEON_CHEST           , new ResourceLocation("minecraft", "chests/simple_dungeon"));
		NEO_TABLE.put(MINESHAFT_CORRIDOR      , new ResourceLocation("minecraft", "chests/abandoned_mineshaft"));
		NEO_TABLE.put(STRONGHOLD_LIBRARY      , new ResourceLocation("minecraft", "chests/stronghold_library"));
		NEO_TABLE.put(STRONGHOLD_CROSSING     , new ResourceLocation("minecraft", "chests/stronghold_crossing"));
		NEO_TABLE.put(STRONGHOLD_CORRIDOR     , new ResourceLocation("minecraft", "chests/stronghold_corridor"));
		NEO_TABLE.put(PYRAMID_DESERT_CHEST    , new ResourceLocation("minecraft", "chests/desert_pyramid"));
		NEO_TABLE.put(PYRAMID_JUNGLE_CHEST    , new ResourceLocation("minecraft", "chests/jungle_temple"));
		NEO_TABLE.put(PYRAMID_JUNGLE_DISPENSER, new ResourceLocation("minecraft", "chests/jungle_temple_dispenser"));
		NEO_TABLE.put(VILLAGE_BLACKSMITH      , new ResourceLocation("minecraft", "chests/village/village_weaponsmith"));
		NEO_TABLE.put(BONUS_CHEST             , new ResourceLocation("minecraft", "chests/spawn_bonus_chest"));

		VANILLA_WEIGHT_1710.put(DUNGEON_CHEST           , 120);
		VANILLA_WEIGHT_1710.put(MINESHAFT_CORRIDOR      ,  80);
		VANILLA_WEIGHT_1710.put(STRONGHOLD_LIBRARY      ,  44);
		VANILLA_WEIGHT_1710.put(STRONGHOLD_CROSSING     ,  62);
		VANILLA_WEIGHT_1710.put(STRONGHOLD_CORRIDOR     ,  99);
		VANILLA_WEIGHT_1710.put(PYRAMID_DESERT_CHEST    ,  73);
		VANILLA_WEIGHT_1710.put(PYRAMID_JUNGLE_CHEST    ,  73);
		VANILLA_WEIGHT_1710.put(PYRAMID_JUNGLE_DISPENSER,  30);
		VANILLA_WEIGHT_1710.put(VILLAGE_BLACKSMITH      ,  94);
		VANILLA_WEIGHT_1710.put(BONUS_CHEST             ,  64);
	}

	// Field names and types match 1.7.10 exactly because ChestGenHooksChestReplacer finds them by reflection.
	private static final HashMap<String, ChestGenHooks> chestInfo = new HashMap<>();
	private static boolean hasInit = false;
	static {
		init();
	}

	private static void init() {
		if (hasInit) return;
		hasInit = true;
		// 1.7.10 filled these with vanilla contents too; here contents holds only GT additions, since
		// vanilla content now lives in the engine's data-driven tables, with the original counts kept 1:1.
		// ChestGenHooks.init:51-60).
		addInfo(MINESHAFT_CORRIDOR      ,  3,  7);
		addInfo(PYRAMID_DESERT_CHEST    ,  2,  7);
		addInfo(PYRAMID_JUNGLE_CHEST    ,  2,  7);
		addInfo(PYRAMID_JUNGLE_DISPENSER,  2,  2);
		addInfo(STRONGHOLD_CORRIDOR     ,  2,  4);
		addInfo(STRONGHOLD_LIBRARY      ,  1,  5);
		addInfo(STRONGHOLD_CROSSING     ,  1,  5);
		addInfo(VILLAGE_BLACKSMITH      ,  3,  9);
		addInfo(BONUS_CHEST             , 10, 10);
		addInfo(DUNGEON_CHEST           ,  8,  8);
	}

	private static void addInfo(String aCategory, int aMin, int aMax) {
		ChestGenHooks tHook = new ChestGenHooks(aCategory);
		tHook.countMin = aMin;
		tHook.countMax = aMax;
		chestInfo.put(aCategory, tHook);
	}

	public static ChestGenHooks getInfo(String aCategory) {
		if (!chestInfo.containsKey(aCategory)) {
			chestInfo.put(aCategory, new ChestGenHooks(aCategory));
		}
		return chestInfo.get(aCategory);
	}

	/** Refreshes the stack here because enchantment templates are created against the first world's
	 *  registry; opening a GUI with a stale Holder from another world crashes, and this is the one choke point for issuance. */
	public static ItemStack[] generateStacks(Random aRandom, ItemStack aSource, int aMin, int aMax) {
		int tCount = aMin + (aMax > aMin ? aRandom.nextInt(aMax - aMin + 1) : 0);
		ItemStack[] rStacks;
		if (aSource == null || aSource.isEmpty()) {
			rStacks = new ItemStack[0];
		} else if (tCount > aSource.getMaxStackSize()) {
			rStacks = new ItemStack[tCount];
			for (int i = 0; i < tCount; i++) {
				rStacks[i] = gregapi.util.UT.NBT.freshenEnchantments(aSource.copy());
				rStacks[i].setCount(1);
			}
		} else {
			rStacks = new ItemStack[1];
			rStacks[0] = gregapi.util.UT.NBT.freshenEnchantments(aSource.copy());
			rStacks[0].setCount(tCount);
		}
		return rStacks;
	}

	// static shortcuts 1:1
	public static WeightedRandomChestContent[] getItems(String aCategory, Random aRandom) {return getInfo(aCategory).getItems(aRandom);}
	public static int getCount(String aCategory, Random aRandom) {return getInfo(aCategory).getCount(aRandom);}
	public static void addItem(String aCategory, WeightedRandomChestContent aItem) {getInfo(aCategory).addItem(aItem);}
	public static void removeItem(String aCategory, ItemStack aItem) {getInfo(aCategory).removeItem(aItem);}
	public static ItemStack getOneItem(String aCategory, Random aRandom) {return getInfo(aCategory).getOneItem(aRandom);}

	private String category;
	private int countMin = 0;
	private int countMax = 0;
	private ArrayList<WeightedRandomChestContent> contents = new ArrayList<>();

	public ChestGenHooks(String aCategory) {
		category = aCategory;
	}

	public void addItem(WeightedRandomChestContent aItem) {
		if (aItem != null) contents.add(aItem);
	}

	/** 1.7.10 compared item and metadata (including wildcard meta); neo has no metadata, so this
	 *  compares by item only, following the block/item flattening the engine did. */
	public void removeItem(ItemStack aItem) {
		Iterator<WeightedRandomChestContent> tIterator = contents.iterator();
		while (tIterator.hasNext()) {
			WeightedRandomChestContent tContent = tIterator.next();
			if (tContent.theItemId != null && !tContent.theItemId.isEmpty() && aItem.getItem() == tContent.theItemId.getItem()) {
				tIterator.remove();
			}
		}
	}

	/** For vanilla categories the weighted list lives in the engine's data-driven table and cannot be
	 *  read without a LootContext, so this throws (PORT-TODO) instead of silently losing the vanilla part. */
	public WeightedRandomChestContent[] getItems(Random aRandom) {
		if (NEO_TABLE.containsKey(category)) throw new UnsupportedOperationException(
			"PORT-TODO(F-loot REPLACE): взвешенный список ванильной категории '" + category + "' живёт в data-driven LootTable (без LootContext не читается); ре-экспрессировать вызывателя в IGlobalLootModifier — decisions/F-loot-chestgen-map.md");
		return contents.toArray(new WeightedRandomChestContent[contents.size()]);
	}

	/** The max bound is exclusive here, matching how Forge 1.7.10 implemented this roll. */
	public int getCount(Random aRandom) {
		return countMin < countMax ? countMin + aRandom.nextInt(countMax - countMin) : countMin;
	}

	/** GT6 categories roll from contents directly; vanilla categories instead sample the resulting
	 *  neo table (vanilla plus injected GT pools) on a live server, returning null outside one. */
	public ItemStack getOneItem(Random aRandom) {
		if (NEO_TABLE.containsKey(category)) return getOneVanillaItem(aRandom);
		WeightedRandomChestContent tItem = WeightedRandomChestContent.getRandomItem(aRandom, getItems(aRandom));
		if (tItem == null) return null;
		ItemStack[] tStacks = generateStacks(aRandom, tItem.theItemId, tItem.theMinimumChanceToGenerateItem, tItem.theMaximumChanceToGenerateItem);
		return tStacks.length > 0 ? tStacks[0] : null;
	}

	private ItemStack getOneVanillaItem(Random aRandom) {
		try {
			MinecraftServer tServer = ServerLifecycleHooks.getCurrentServer();
			if (tServer == null) return null;
			net.minecraft.server.level.ServerLevel tLevel = tServer.overworld();
			if (tLevel == null) return null;
			LootTable tTable = tServer.getLootData().getLootTable(NEO_TABLE.get(category));
			LootParams tParams = new LootParams.Builder(tLevel)
				.withParameter(LootContextParams.ORIGIN, net.minecraft.world.phys.Vec3.ZERO)
				.create(LootContextParamSets.CHEST);
			it.unimi.dsi.fastutil.objects.ObjectArrayList<ItemStack> tItems = tTable.getRandomItems(tParams);
			return tItems.isEmpty() ? null : tItems.get(aRandom.nextInt(tItems.size()));
		} catch (Throwable e) {
			return null;
		}
	}

	public int getMin() {return countMin;}
	public int getMax() {return countMax;}
	public void setMin(int aValue) {countMin = aValue;}
	public void setMax(int aValue) {countMax = aValue;}


	/** Reverse lookup: neo table -> GT6 category. Its only caller is GT6ChestLootModifier (delivery lives there,
	 *  kept separate from compat-mirror); the mapping itself stays here, the one place it's defined. */
	public static String categoryForTable(ResourceLocation aTable) {
		for (Map.Entry<String, ResourceLocation> tEntry : NEO_TABLE.entrySet())
			if (tEntry.getValue().equals(aTable)) return tEntry.getKey();
		return null;
	}

	/** Builds a standalone LootPool (never attached to a table, never frozen) with the category's GT additions,
	 *  using the exact 1.7.10 weighted-list distribution; rebuilt fresh from the live buffer on every real loot roll. */
	public static LootPool buildPool(String aCategory) {
		ChestGenHooks tHook = chestInfo.get(aCategory);
		if (tHook == null || tHook.contents.isEmpty()) return null;
		Integer tVanillaWeight = VANILLA_WEIGHT_1710.get(aCategory);
		LootPool.Builder tPool = LootPool.lootPool().name("gregtech6:" + aCategory).setRolls(
			tHook.countMin < tHook.countMax ? UniformGenerator.between(tHook.countMin, tHook.countMax - 1) : ConstantValue.exactly(tHook.countMin));
		if (tVanillaWeight != null) tPool.add(EmptyLootItem.emptyItem().setWeight(tVanillaWeight));
		for (WeightedRandomChestContent tContent : tHook.contents) {
			if (tContent.theItemId == null || tContent.theItemId.isEmpty()) continue;
			net.minecraft.world.level.storage.loot.entries.LootPoolSingletonContainer.Builder<?> tItem =
				LootItem.lootTableItem(tContent.theItemId.getItem())
					.setWeight(Math.max(1, tContent.itemWeight))
					.apply(SetItemCountFunction.setCount(UniformGenerator.between(
						tContent.theMinimumChanceToGenerateItem, Math.max(tContent.theMinimumChanceToGenerateItem, tContent.theMaximumChanceToGenerateItem))));
			// GT6 item identity on this branch lives in the stack's NBT tag, while LootItem.lootTableItem only carries the type.
			// The tag is carried over with the vanilla SetNbtFunction; a damage-based loot function would corrupt the subtype.
			if (tContent.theItemId.getTag() != null)
				tItem.apply(net.minecraft.world.level.storage.loot.functions.SetNbtFunction.setTag(tContent.theItemId.getTag().copy()));
			tPool.add(tItem);
		}
		return tPool.build();
	}

}

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

package gt6mirror.net.minecraftforge.common;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
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
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.LootTableLoadEvent;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

/** Single adaptation point for 1.7.10's chest loot: its one weighted-list-per-category model is
 *  reconciled here with neo's data-driven loot tables, injecting GT6's additions into each vanilla table. */
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
	private static final Map<String, Identifier> NEO_TABLE = new LinkedHashMap<>();
	/** The total weight of 1.7.10's vanilla chest contents per category, computed from the original
	 *  structure-generation data, used as the weight of an "empty" entry so GT items keep the exact 1.7.10 odds. */
	private static final Map<String, Integer> VANILLA_WEIGHT_1710 = new HashMap<>();
	static {
		NEO_TABLE.put(DUNGEON_CHEST           , Identifier.withDefaultNamespace("chests/simple_dungeon"));
		NEO_TABLE.put(MINESHAFT_CORRIDOR      , Identifier.withDefaultNamespace("chests/abandoned_mineshaft"));
		NEO_TABLE.put(STRONGHOLD_LIBRARY      , Identifier.withDefaultNamespace("chests/stronghold_library"));
		NEO_TABLE.put(STRONGHOLD_CROSSING     , Identifier.withDefaultNamespace("chests/stronghold_crossing"));
		NEO_TABLE.put(STRONGHOLD_CORRIDOR     , Identifier.withDefaultNamespace("chests/stronghold_corridor"));
		NEO_TABLE.put(PYRAMID_DESERT_CHEST    , Identifier.withDefaultNamespace("chests/desert_pyramid"));
		NEO_TABLE.put(PYRAMID_JUNGLE_CHEST    , Identifier.withDefaultNamespace("chests/jungle_temple"));
		NEO_TABLE.put(PYRAMID_JUNGLE_DISPENSER, Identifier.withDefaultNamespace("chests/jungle_temple_dispenser"));
		NEO_TABLE.put(VILLAGE_BLACKSMITH      , Identifier.withDefaultNamespace("chests/village/village_weaponsmith"));
		NEO_TABLE.put(BONUS_CHEST             , Identifier.withDefaultNamespace("chests/spawn_bonus_chest"));

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
		NeoForge.EVENT_BUS.addListener(ChestGenHooks::onLootTableLoad);
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
			LootTable tTable = tServer.reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE, NEO_TABLE.get(category)));
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


	/** Channel 1: handles /reload and later resource reloads; on the very first load the buffer is
	 *  still empty, so channel 2 (injectAll) covers that case instead, with no race between them. */
	private static void onLootTableLoad(LootTableLoadEvent aEvent) {
		for (Map.Entry<String, Identifier> tEntry : NEO_TABLE.entrySet()) {
			if (!tEntry.getValue().equals(aEvent.getName())) continue;
			ChestGenHooks tHook = chestInfo.get(tEntry.getKey());
			if (tHook != null && !tHook.contents.isEmpty()) injectInto(tHook, aEvent.getTable());
			return;
		}
	}

	/** Channel 2: catches the first load by running right after deferred item init, once the buffer
	 *  is full, the server exists, and the loot tables are loaded. */
	public static void injectAll(MinecraftServer aServer) {
		if (aServer == null) return;
		for (Map.Entry<String, Identifier> tEntry : NEO_TABLE.entrySet()) {
			ChestGenHooks tHook = chestInfo.get(tEntry.getKey());
			if (tHook == null || tHook.contents.isEmpty()) continue;
			try {
				LootTable tTable = aServer.reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE, tEntry.getValue()));
				if (tTable != null && tTable != LootTable.EMPTY) injectInto(tHook, tTable);
			} catch (Throwable e) {
				e.printStackTrace();
			}
		}
	}

	/** Injects GT additions as one named pool with an "empty" entry weighted to match 1.7.10's vanilla
	 *  share, reproducing the exact per-slot odds of the original single weighted list; naming keeps it idempotent. */
	private static void injectInto(ChestGenHooks aHook, LootTable aTable) {
		String tPoolName = "gregtech6:" + aHook.category;
		if (aTable.getPool(tPoolName) != null) return;
		Integer tVanillaWeight = VANILLA_WEIGHT_1710.get(aHook.category);
		LootPool.Builder tPool = LootPool.lootPool().name(tPoolName).setRolls(
			aHook.countMin < aHook.countMax ? UniformGenerator.between(aHook.countMin, aHook.countMax - 1) : ConstantValue.exactly(aHook.countMin));
		if (tVanillaWeight != null) tPool.add(EmptyLootItem.emptyItem().setWeight(tVanillaWeight));
		for (WeightedRandomChestContent tContent : aHook.contents) {
			if (tContent.theItemId == null || tContent.theItemId.isEmpty()) continue;
			net.minecraft.world.level.storage.loot.entries.LootPoolSingletonContainer.Builder<?> tItem =
				LootItem.lootTableItem(tContent.theItemId.getItem())
					.setWeight(Math.max(1, tContent.itemWeight))
					.apply(SetItemCountFunction.setCount(UniformGenerator.between(
						tContent.theMinimumChanceToGenerateItem, Math.max(tContent.theMinimumChanceToGenerateItem, tContent.theMaximumChanceToGenerateItem))));
			// GT6 item identity lives in data components (meta, registry id), but LootItem.lootTableItem only
			// carries the base Item type, so the full component patch is copied via SetComponentsFunction to keep it.
			for (Map.Entry<net.minecraft.core.component.DataComponentType<?>, java.util.Optional<?>> tComp : tContent.theItemId.getComponentsPatch().entrySet()) {
				if (tComp.getValue().isEmpty()) continue; // a removed component cannot be expressed by a loot function; buffer stacks never have one
				applyComponent(tItem, tComp.getKey(), tComp.getValue().get());
			}
			tPool.add(tItem);
		}
		aTable.addPool(tPool.build());
	}

	@SuppressWarnings("unchecked")
	private static <T> void applyComponent(net.minecraft.world.level.storage.loot.entries.LootPoolSingletonContainer.Builder<?> aEntry, net.minecraft.core.component.DataComponentType<T> aType, Object aValue) {
		aEntry.apply(net.minecraft.world.level.storage.loot.functions.SetComponentsFunction.setComponent(aType, (T)aValue));
	}
}

package net.minecraftforge.common;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import net.minecraft.resources.Identifier;
import net.minecraft.util.WeightedRandomChestContent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.LootTableLoadEvent;

/** 1.7.10 {@code net.minecraftforge.common.ChestGenHooks} — реестр модификаций vanilla chest-loot.
 *  neo сделал loot data-driven (JSON LootTable). Централизованный порт (F11-паттерн буфер+диспетчер):
 *  контент буферизует добавления через {@link #addItem}, единый {@link #onLootTableLoad} инъектит их
 *  как {@link LootPool} в соответствующую neo-таблицу при загрузке (карта {@link #NEO_TABLE}).
 *  Категорийные String-константы 1:1 (сверены с референсом). REPLACE/read-путь (getItems/getOneItem) —
 *  требует рантайм-LootContext, недоступного на init → видимый PORT-TODO-сбой до re-экспрессии в
 *  IGlobalLootModifier. См. decisions/F-loot-chestgen-map.md. */
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

	/** GT6 chest-категория -> neo vanilla loot-таблица (Identifier). 9/10 1:1; village_blacksmith -> weaponsmith. */
	private static final Map<String, Identifier> NEO_TABLE = new LinkedHashMap<>();
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
	}

	private static final Map<String, ChestGenHooks> INFOS = new LinkedHashMap<>();
	private static boolean sListenerRegistered = false;

	private final String mCategory;
	private final List<WeightedRandomChestContent> mItems = new ArrayList<>();
	private int mMin = 0, mMax = 0;

	protected ChestGenHooks(String aCategory) {mCategory = aCategory;}

	public static ChestGenHooks getInfo(String aCategory) {
		if (!sListenerRegistered) {sListenerRegistered = true; NeoForge.EVENT_BUS.addListener(ChestGenHooks::onLootTableLoad);}
		ChestGenHooks tHook = INFOS.get(aCategory);
		if (tHook == null) {tHook = new ChestGenHooks(aCategory); INFOS.put(aCategory, tHook);}
		return tHook;
	}

	public void addItem(WeightedRandomChestContent aContent) {if (aContent != null) mItems.add(aContent);}
	public static void addItem(String aCategory, WeightedRandomChestContent aContent) {getInfo(aCategory).addItem(aContent);}
	public void setMin(int aMin) {mMin = aMin;}
	public void setMax(int aMax) {mMax = aMax;}
	public int getMin() {return mMin;}
	public int getMax() {return mMax;}

	/** REPLACE/read-путь — чтение лута требует рантайм-LootContext (нет на init). Re-экспрессировать в
	 *  IGlobalLootModifier (decisions/F-loot-chestgen-map.md §4). До неё — видимый сбой (PORT-TODO падает, не тихо). */
	public WeightedRandomChestContent[] getItems(Random aRandom) {throw porttodo();}
	public ItemStack getOneItem(Random aRandom) {throw porttodo();}
	public void removeItem(ItemStack aStack) {throw porttodo();}
	public int getCount(Random aRandom) {throw porttodo();}
	public static ItemStack getOneItem(String aCategory, Random aRandom) {throw porttodo();}
	private static UnsupportedOperationException porttodo() {
		return new UnsupportedOperationException("PORT-TODO(F-loot REPLACE): чтение лута требует neo LootContext (нет на init); ре-экспрессировать ChestReplacer/TwilightTreasureReplacer/Compat_IC2 в IGlobalLootModifier — decisions/F-loot-chestgen-map.md");
	}

	/** Единый диспетчер: при загрузке любой vanilla loot-таблицы инъектит буфер соответствующей категории как LootPool. */
	private static void onLootTableLoad(LootTableLoadEvent aEvent) {
		Identifier tName = aEvent.getName();
		for (ChestGenHooks tHook : INFOS.values()) {
			if (tHook.mItems.isEmpty()) continue;
			Identifier tTarget = NEO_TABLE.get(tHook.mCategory);
			if (tTarget == null || !tTarget.equals(tName)) continue;
			LootPool.Builder tPool = LootPool.lootPool().setRolls(
				tHook.mMax > 0 ? UniformGenerator.between(Math.max(0, tHook.mMin), tHook.mMax) : ConstantValue.exactly(1));
			for (WeightedRandomChestContent tContent : tHook.mItems) {
				if (tContent.theItemId == null || tContent.theItemId.isEmpty()) continue;
				tPool.add(LootItem.lootTableItem(tContent.theItemId.getItem())
					.setWeight(Math.max(1, tContent.itemWeight))
					.apply(SetItemCountFunction.setCount(UniformGenerator.between(
						tContent.theMinimumChanceToGenerateItem, Math.max(tContent.theMinimumChanceToGenerateItem, tContent.theMaximumChanceToGenerateItem)))));
			}
			aEvent.getTable().addPool(tPool.build());
		}
	}
}

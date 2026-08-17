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

// Пакет gt6mirror.minecraftforge.common (не net.minecraftforge.common): boot-краш ResolutionException —
// настоящий модуль forge 1.20.1 и модуль gregtech6 экспортировали бы один и тот же пакет net.minecraftforge.*
// (split-package), JPMS такое не резолвит; тип живой (используется рантаймом), поэтому переупакован, а не удалён.
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
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.LootTableLoadEvent;
import net.minecraftforge.server.ServerLifecycleHooks;

/** 1.7.10 {@code net.minecraftforge.common.ChestGenHooks} — реестр chest-лута. ЕДИНСТВЕННЫЙ центр адаптации
 *  F-loot (BUG-039): neo сделал лут data-driven (JSON LootTable), 1.7.10-механика «единый взвешенный список
 *  категории» воспроизводится здесь и только здесь; контент (Loader_Loot и потребители getOneItem) — verbatim.
 *
 *  <p>Контракт полей/методов — ТОЧНО 1.7.10-Forge ({@code chestInfo}/{@code contents}/{@code countMin}/{@code countMax}):
 *  на эти имена рефлексией завязан GT6-код ({@code ChestGenHooksChestReplacer} и «MineTweaker does Reflection
 *  the wrong way» — Loader_Loot). Формулы getCount/generateStacks/getOneItem — 1:1 из референса
 *  (gt6-oracle-dumper recompSrc ChestGenHooks.java:109-243).
 *
 *  <p>Два рантайм-пути:
 *  <ul><li><b>GT6-категории (gt.*)</b> — содержимое целиком в {@code contents}: getOneItem/getItems/getCount —
 *  формулы 1:1, никакого neo-контекста не нужно (мешки Bag_Loot_*, книги, Unboxinator, ST.generateLoot).</li>
 *  <li><b>Ванильные категории</b> — vanilla-часть живёт в data-driven таблице движка; GT-добавки из
 *  {@code contents} ИНЪЕКТИРУЮТСЯ в неё именованным {@link LootPool} с точным 1.7.10-распределением (см.
 *  {@link #injectInto}); getOneItem семплирует итоговую таблицу (vanilla+GT) на живом сервере.</li></ul>
 *
 *  <p>Тайминг (класс BUG-054): {@link LootTableLoadEvent} стреляет при загрузке серверных ресурсов
 *  (ReloadableServerRegistries.scheduleRegistryLoad:69) — РАНЬШЕ GT6 data-init (drain отложек на
 *  LevelEvent.Load) → на первой загрузке буфер пуст и событие no-op. Потому ДВА канала инъекции:
 *  {@link #onLootTableLoad} (покрывает /reload и все будущие перезагрузки) + догоняющий {@link #injectAll}
 *  из GT_API.onLevelLoadEarlyItemInit ПОСЛЕ runDeferredItemInit. Идемпотентность — именованный pool
 *  ({@code gregtech6:<категория>}, LootTable.getPool/addPool отвергает дубликат).
 *
 *  <p>REPLACE/read-путь ванильных категорий — ЗАКРЫТ РЕШЕНИЕМ (FORCED-ADAPTATION, BUG-039 v4): канал 1.7.10
 *  (подмена ванильного сундука на GT6-сундук В МОМЕНТ генерации его лута) в neo исчез — лут ленивый
 *  (наполнение при первом открытии), у генерации структур пер-сундук-хука нет, подмена при открытии ломала бы
 *  UX (сундук меняется под открытым GUI). Деградация принята: ванильные структурные сундуки остаются
 *  ванильными (GT-лут в них уже есть ADD-путём); trapped-фича GT6-сундуков в структурах не переносится.
 *  Compat_IC2 (iridium→scrapbox) — вернуться ТОЛЬКО при реальной интеграции IC2 (F10; мода для 26.1.2 нет).
 *  {@link #getItems(Random)} для ванильных категорий оставлен throw — СТРАЖ от нового использования мёртвого
 *  канала, не отложенность. См. decisions/F-loot-chestgen-map.md §6.9. */
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

	/** GT6 chest-категория -> neo vanilla loot-таблица. 9/10 1:1; villageBlacksmith -> weaponsmith (neo раздробил
	 *  village на 12 таблиц; кузнец=weaponsmith — ближайший 1:1 по содержимому, ADR §3). */
	private static final Map<String, ResourceLocation> NEO_TABLE = new LinkedHashMap<>();
	/** Суммарный вес vanilla-содержимого категории в 1.7.10 (массивы структур + Forge-init enchanted_book: +1 везде,
	 *  +2 library; кузнец/бонус/диспенсер — без книги). Выведено скриптом из референса (recompSrc:
	 *  WorldGenDungeons.field_111189_a=119, mineshaftChestContents=79, itemsToGenerateInTemple=72,
	 *  junglePyramidsChestContents=72, junglePyramidsDispenserContents=30, strongholdChestContents=98,
	 *  strongholdLibraryChestContents=42, strongholdRoomCrossingChestContents=61, villageBlacksmithChestContents=94,
	 *  WorldServer.bonusChestContent=64). Роль: вес «пустого» entry в инъектируемом pool — эмулирует «слот достался
	 *  ванили», давая GT-предметам ТОЧНО 1.7.10-вероятность на слот (vanilla-часть сундука генерит сам движок). */
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

	// Имя/тип полей — ТОЧНО 1.7.10 (chestInfo=HashMap, contents=ArrayList): рефлексия ChestGenHooksChestReplacer
	// и гейты Loader_Loot ищут именно их.
	private static final HashMap<String, ChestGenHooks> chestInfo = new HashMap<>();
	private static boolean hasInit = false;
	static {
		init();
	}

	private static void init() {
		if (hasInit) return;
		hasInit = true;
		// 1.7.10-Forge init() наполнял категории vanilla-массивами структур + counts. В neo vanilla-содержимое живёт
		// в data-driven таблицах движка (contents хранит только GT-добавки) — воспроизводим counts 1:1 (референс
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
		MinecraftForge.EVENT_BUS.addListener(ChestGenHooks::onLootTableLoad);
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

	/** 1:1 (референс :109-134): count стеков из source; больше maxStackSize — сплит по 1.
	 *  BUG-060 (класс BUG-002 «протухший Holder»): шаблоны contents создаются на data-init ПЕРВОГО мира сессии;
	 *  энчанты 1.21+ — пер-серверный datapack-реестр → Holder'ы шаблона протухают при смене мира → краш-энкод
	 *  container_set_content при открытии GUI с таким предметом. Freshen ЗДЕСЬ — единственная точка, через
	 *  которую идут ВСЕ выдачи шаблонов (getOneItem всех вызывателей: сейфы/мешки/Unboxinator/Twilight/stats-loot
	 *  + generateChestContents); повторный freshen в ST.generateLoot идемпотентен. */
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

	/** 1:1-эквивалент (референс :180-191): 1.7.10 сравнивал item+meta (и wildcard-мету); в neo меты нет —
	 *  сравнение по item (F4-flattening). */
	public void removeItem(ItemStack aItem) {
		Iterator<WeightedRandomChestContent> tIterator = contents.iterator();
		while (tIterator.hasNext()) {
			WeightedRandomChestContent tContent = tIterator.next();
			if (tContent.theItemId != null && !tContent.theItemId.isEmpty() && aItem.getItem() == tContent.theItemId.getItem()) {
				tIterator.remove();
			}
		}
	}

	/** GT6-категории (gt.*) — contents = полное содержимое, 1:1. Ванильные категории — их взвешенный список живёт
	 *  в data-driven таблице движка и без LootContext как список НЕ читается: отдать один contents = тихая потеря
	 *  vanilla-части (запрещено ADR) → видимый PORT-TODO-сбой до re-экспрессии REPLACE-пути в IGlobalLootModifier
	 *  (единственные вызыватели — ChestGenHooksChestReplacer.getItems и гейтованный MD.IC Compat_IC2). */
	public WeightedRandomChestContent[] getItems(Random aRandom) {
		if (NEO_TABLE.containsKey(category)) throw new UnsupportedOperationException(
			"PORT-TODO(F-loot REPLACE): взвешенный список ванильной категории '" + category + "' живёт в data-driven LootTable (без LootContext не читается); ре-экспрессировать вызывателя в IGlobalLootModifier — decisions/F-loot-chestgen-map.md");
		return contents.toArray(new WeightedRandomChestContent[contents.size()]);
	}

	/** 1:1 (референс :225-228; max ЭКСКЛЮЗИВНО — так в Forge 1.7.10, воспроизводим). */
	public int getCount(Random aRandom) {
		return countMin < countMax ? countMin + aRandom.nextInt(countMax - countMin) : countMin;
	}

	/** GT6-категории — 1:1 (weighted из contents + generateStacks, референс :237-243). Ванильные категории —
	 *  семпл итоговой neo-таблицы (vanilla + инъектированные GT-пулы) на живом сервере; вне сервера → null
	 *  (как оригинал вне мира). */
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

	// ------------------------------------------------------------------ neo-диспетчер инъекции (ADD-путь) ---

	/** Канал 1: /reload и все будущие (пере)загрузки серверных ресурсов. На ПЕРВОЙ загрузке буфер ещё пуст
	 *  (data-init GT6 отложен на LevelEvent.Load) — тогда работает канал 2 {@link #injectAll}. Событие приходит
	 *  на background-executor'е загрузки ресурсов; к этому моменту contents либо пуст (первая загрузка), либо
	 *  стабилен (data-init давно завершён) — гонки нет. */
	private static void onLootTableLoad(LootTableLoadEvent aEvent) {
		for (Map.Entry<String, ResourceLocation> tEntry : NEO_TABLE.entrySet()) {
			if (!tEntry.getValue().equals(aEvent.getName())) continue;
			ChestGenHooks tHook = chestInfo.get(tEntry.getKey());
			if (tHook != null && !tHook.contents.isEmpty()) injectInto(tHook, aEvent.getTable());
			return;
		}
	}

	/** Канал 2: догоняющая инъекция первой загрузки — вызывается из GT_API.onLevelLoadEarlyItemInit СРАЗУ после
	 *  runDeferredItemInit (буфер полон, сервер создан, таблицы загружены). Тот же приём, что пересбор
	 *  furnace-propertySet (BUG-054). */
	public static void injectAll(MinecraftServer aServer) {
		if (aServer == null) return;
		for (Map.Entry<String, ResourceLocation> tEntry : NEO_TABLE.entrySet()) {
			ChestGenHooks tHook = chestInfo.get(tEntry.getKey());
			if (tHook == null || tHook.contents.isEmpty()) continue;
			try {
				LootTable tTable = aServer.getLootData().getLootTable(tEntry.getValue());
				if (tTable != null && tTable != LootTable.EMPTY) injectInto(tHook, tTable);
			} catch (Throwable e) {
				e.printStackTrace();
			}
		}
	}

	/** Инъекция GT-добавок категории в ванильную таблицу ОДНИМ именованным pool'ом с точным 1.7.10-распределением:
	 *  rolls = 1.7.10-count категории (формула getCount 1:1), внутри — GT-entries с их весами + «пустой» entry с
	 *  весом vanilla-суммы 1.7.10 ({@link #VANILLA_WEIGHT_1710}). Каждый ролл: с вероятностью W_v/(W_v+ΣW_gt) слот
	 *  «достался ванили» (пусто — её генерит родная часть таблицы), иначе GT-предмет по весу → P(слот=GT-предмет i)
	 *  = w_i/(W_v+ΣW_gt) — ровно как в едином списке 1.7.10. Идемпотентно: pool именован, дубликат не ставится. */
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
			// Identity GT6-предметов в ветке 1.20.1 живёт в NBT-теге стека (мета MultiItem, реестр/ID
			// MultiTileEntity — монеты/мешки/книги/сундуки), а LootItem.lootTableItem несёт только тип Item: без
			// переноса выпадали бы «голые» дефолты. Переносим тег штатной SetNbtFunction (SetNbtFunction.java:34) —
			// это ровно тот же стек, что нёс WeightedRandomChestContent 1.7.10. Отдельная лут-функция под damage
			// НЕ нужна и была бы ВРЕДНА: подтип лежит ВНУТРИ того же тега (IForgeItem.setDamage:472 —
			// getOrCreateTag().putInt("Damage")), а SetItemDamageFunction трактует значение как ДОЛЮ от maxDamage
			// (SetItemDamageFunction.java:41) и у мета-предметов GT6 (maxDamage=0) обнулила бы подтип молча.
			if (tContent.theItemId.getTag() != null)
				tItem.apply(net.minecraft.world.level.storage.loot.functions.SetNbtFunction.setTag(tContent.theItemId.getTag().copy()));
			tPool.add(tItem);
		}
		aTable.addPool(tPool.build());
	}

}

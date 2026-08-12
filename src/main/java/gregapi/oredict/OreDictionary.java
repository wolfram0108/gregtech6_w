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
import net.neoforged.bus.api.Event;
import net.neoforged.neoforge.common.NeoForge;

import gregapi.util.ST;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * F4 ЦЕНТРАЛЬНЫЙ ПЕРЕХОДНИК — замена удалённого Forge-класса {@code net.minecraftforge.oredict.OreDictionary}.
 * См. {@code doc/missions/gt6-port/decisions/F4-oredictionary.md} §4.1 и {@code architecture/oredict.md}.
 *
 * <p>В Forge 1.7.10 этот класс держал плоское хранилище {@code имя -> список ItemStack} и рассылал
 * {@code OreRegisterEvent} по шине. GregTech6 уже владеет всей богатой семантикой поверх (унификация,
 * ассоциация, слушатели с реплеем — {@code OreDictManager}); единственное, чем реально владел Forge-класс,
 * это плоское хранилище (роль-A F4-ADR). По решению F4 оно интернализуется сюда БЕЗ изменения логики
 * вызывающих: точки {@code OreDictionary.registerOre/getOres/getOreNames/WILDCARD_VALUE} остаются
 * дословными (1:1), меняется только пакет в импорте (словарь движка F4).</p>
 *
 * <p><b>Событие + дедуп (R8, восстановлено 1:1; УЛИКА R8-2 доработка dedup-семантики).</b> Оригинал
 * Forge {@code registerOreImpl} (эталон
 * {@code gt6-oracle-dumper/build/tmp/recompSrc/net/minecraftforge/oredict/OreDictionary.java:517-562})
 * перед вставкой считал HASH из ID реестра предмета, к которому (только если {@code meta !=
 * WILDCARD_VALUE}) подмешивал {@code (meta+1)<<16} ({@code :543-546}) — то есть wildcard-запись
 * (meta == WILDCARD_VALUE) попадает в hash-бакет БЕЗ meta-битов и НИКОГДА не совпадает с
 * hash-бакетом конкретной meta того же предмета; проверял, не встречался ли уже {@code oreID} этого
 * имени в этом бакете ({@code ids.contains(oreID) return}, {@code :549-550}), и после реальной вставки
 * рассылал {@code MinecraftForge.EVENT_BUS.post(new OreRegisterEvent(name, ore))} ({@code :561}). NBT
 * в hash НЕ участвует (только Item+meta) — {@code ST.equal(a,b,true)} с {@code aIgnoreNBT=true}
 * корректно это отражал, НО его meta-компаратор {@code equal(long,long)}
 * ({@code aMeta1==aMeta2 || aMeta1==W || aMeta2==W}) СИММЕТРИЧНО-permissive: любой wildcard с ЛЮБОЙ
 * стороны считает мету равной — это НЕ Forge-семантика (у Forge wildcard-бакет и конкретный-meta-бакет
 * РАЗНЫЕ, схлопывания нет). Заменено точным hash-бакет-критерием Forge: тот же Item И (обе meta ==
 * {@link #WILDCARD_VALUE}, либо обе meta численно равны — без permissive-wildcard-стороны). Рассылка —
 * через {@code NeoForge.EVENT_BUS.post(...)}, на который {@code OreDictManager} подписан
 * ({@code @SubscribeEvent onOreRegistration1} + {@code NeoForge.EVENT_BUS.register(this)}, {@code :121}).
 * Свои ore GregTech6 ДОПОЛНИТЕЛЬНО прогоняет через РЕПЛЕЙ в конструкторе ({@code OreDictManager:118}) —
 * это покрывает записи, существовавшие ДО создания {@code OreDictManager}; сама рассылка здесь покрывает
 * все записи ПОСЛЕ (ровно как в оригинале, где реплей и живая {@code EVENT_BUS.post} — два разных
 * источника одного потока). Требование движка: {@code IEventBus.post(T)} принимает только
 * {@code T extends net.neoforged.bus.api.Event} (реально проверено в реализации шины —
 * {@code fml-decompiled/.../bus/EventBus.java:143}, кидает {@code IllegalArgumentException} на
 * {@code register(this)} для не-{@code Event} параметра {@code @SubscribeEvent}-метода) — поэтому
 * {@code OreRegisterEvent} здесь наследует {@code Event} (движко-форсированный тип-шов, не отсебятина).</p>
 */
public class OreDictionary {
	private OreDictionary() {/* только статика, как у Forge-класса */}

	/** Метка-джокер «любая metadata». Значение Forge (= {@link Short#MAX_VALUE}); матчинг — зона F1 (F4 §4.3). */
	public static final int WILDCARD_VALUE = Short.MAX_VALUE;

	/** Роль-A: плоское хранилище {@code имя -> список ItemStack}. LinkedHashMap — детерминированный порядок свипа ({@code :118}).
	 *  Тип значения — {@code ArrayList} (не просто {@code List}), чтобы 1-арг {@code getOres(name)} мог вернуть его
	 *  оригинальный тип {@code ArrayList<ItemStack>} без каста (см. ниже, R8-сигнатура). */
	private static final Map<String, ArrayList<ItemStack>> sOres = new LinkedHashMap<>();
	private static final List<ItemStack> EMPTY = Collections.emptyList();

	/** Forge {@code registerOre(name, stack)} == {@code registerOreImpl} 1:1: дедуп (пропускает уже
	 *  зарегистрированный под этим именем физический стек) + рассылка {@code OreRegisterEvent} после
	 *  реальной вставки копии (см. javadoc класса, R8). */
	public static void registerOre(String aName, ItemStack aStack) {
		ArrayList<ItemStack> tList = sOres.computeIfAbsent(aName, k -> new ArrayList<>());
		for (ItemStack tExisting : tList) if (sameHashBucket(tExisting, aStack)) return;
		ItemStack tOre = aStack.copy();
		tList.add(tOre);
		NeoForge.EVENT_BUS.post(new OreRegisterEvent(aName, tOre));
	}

	/** УЛИКА R8-2: Forge hash-бакет 1:1 ({@code registerOreImpl:543-546} — meta подмешивается в hash
	 *  ТОЛЬКО если {@code != WILDCARD_VALUE}), НЕ общий {@code ST.equal(...,true)} (тот трактует
	 *  wildcard permissive-симметрично и ошибочно схлопывает wildcard-запись с конкретной meta). */
	private static boolean sameHashBucket(ItemStack aExisting, ItemStack aStack) {
		return aExisting.getItem() == aStack.getItem() && ST.meta_(aExisting) == ST.meta_(aStack);
	}

	/** Forge 1-арг {@code getOres(name)}: авто-создаёт запись и возвращает ЖИВОЙ список (на него ссылается {@code OD.mItems}).
	 *  Тип возврата — {@code ArrayList<ItemStack>}, ровно как у оригинала
	 *  ({@code gt6-oracle-dumper/.../OreDictionary.java:376}); потребители ({@code Compat_Recipes_HarvestCraft} и др.)
	 *  объявляют переменные именно этим типом (R8-сигнатура). */
	public static ArrayList<ItemStack> getOres(String aName) {
		return sOres.computeIfAbsent(aName, k -> new ArrayList<>());
	}

	/** Forge 2-арг {@code getOres(name, alwaysCreateEntry)}: живой список либо (при {@code false} и отсутствии) общий пустой. */
	public static List<ItemStack> getOres(String aName, boolean aAlwaysCreateEntry) {
		if (aAlwaysCreateEntry) return sOres.computeIfAbsent(aName, k -> new ArrayList<>());
		List<ItemStack> tList = sOres.get(aName);
		return tList == null ? EMPTY : tList;
	}

	/** Forge {@code getOreNames()}: все зарегистрированные имена. */
	public static String[] getOreNames() {
		return sOres.keySet().toArray(new String[0]);
	}

	private static boolean sHasInit = false;

	/**
	 * Роль-B переходника F4: ВАНИЛЬНЫЕ записи словаря, которые в 1.7.10 заводил САМ Forge
	 * ({@code OreDictionary.initVanillaEntries}, эталон
	 * {@code gt6-oracle-dumper/build/tmp/recompSrc/net/minecraftforge/oredict/OreDictionary.java:59-190}) —
	 * ещё до загрузки модов. NeoForge словаря не имеет вовсе, поэтому вместе с хранилищем (роль-A) сюда
	 * переносится и этот стартовый набор: без него GT6 видит ПОЛУПУСТОЙ ванильный словарь.
	 *
	 * <p><b>Улики, что это не догадка, а недостающая часть.</b> Судья паритета (2026-07-26) показал ровно
	 * форму этой дыры: в порте нет {@code oreIron}, {@code blockQuartz} и по 16 записей
	 * {@code blockGlass<Цвет>} / {@code paneGlass<Цвет>} — все они рождались ИМЕННО здесь (остальные
	 * ванильные имена — {@code stickWood}, {@code cropWheat}, {@code record}, {@code stairWood}… — в порте
	 * есть, потому что их дублирует своими стеками сам GT6). Отсюда же счётчики префиксов
	 * {@code blockGlass/paneGlass} 16→0, {@code record} 12→0, {@code stair} 6→0.</p>
	 *
	 * <p><b>Перенос 1:1 с поправкой на расщепление семейств.</b> Там, где 1.7.10 обходился одной
	 * wildcard-записью ({@code new ItemStack(Blocks.log, 1, WILDCARD_VALUE)} = «любое бревно»), в neo нет
	 * ни меты, ни единого блока-семьи — значит члены перечисляются поимённо, тем же приёмом, что уже принят
	 * в {@code LoaderWoodDictionary:45-56}. Цветовые ряды берутся из центра {@code CS.Flattened} (объявлены
	 * там один раз), а {@code ST.make(глава, 1, мета)} сам подставит нужный вариант — карты общие, копий нет.
	 * Порядок цветов {@code dyes[]} и инверсия {@code 15 - i} для стекла — дословно из Forge-исходника
	 * ({@code :154-186}), не из памяти: у красителя и стекла ряды идут навстречу друг другу.</p>
	 *
	 * <p><b>Список 1.7.10 продолжен новым ванильным содержимым (Э1-bis), а не переосмыслен.</b> Forge-список
	 * перечислял ВСЁ, из чего в 1.7.10 состояла ванильная материальная часть: руды, блоки-хранилища, слитки,
	 * самородки, самоцветы. После 1.7.10 движок добавил в эти же ряды новых членов (deepslate- и незер-варианты
	 * руд, самородок железа, аметист, незерит, древние обломки, сырая руда и её блок), и без них словарь GT6
	 * остаётся замороженным на 2014 годе: до этой правки правильный ответ приходил только через тег-мост
	 * ({@link OreDictTags}), то есть ЗАВИСЕЛ от того, объявил ли кто-то конвенционный тег. Правило добавления
	 * жёсткое и проверяемое: (а) имя берётся ТОЛЬКО у Грега — либо это уже существующее имя семьи из списка выше
	 * (новый член вписывается в него), либо его собственное правило {@code префикс + mNameInternal материала}
	 * при УЖЕ существующих {@link gregapi.data.OP}-префиксе и {@link gregapi.data.MT}-материале; (б) новых
	 * префиксов и материалов не заводится; (в) ванильная медь сюда не попадает — её целиком ведёт ADAPT-014
	 * ({@code Loader_Recipes_Vanilla.copperAge/copperItemData}), включая осознанное решение оставить целью
	 * унификации грегский слиток. Ванильные сущности, для которых у Грега имени НЕТ (смола {@code resin_clump}
	 * и её блок, осколок и кристаллы призмарина, почкующийся аметист и друза, соты, эхо-осколок, окисленные и
	 * вощёные формы меди) либо чья ёмкость его префиксу противоречит ({@code amethyst_block} — четыре осколка
	 * против девяти у {@code blockGem}), не регистрируются вовсе — выдумывать имя запрещено.
	 *
	 * <p>Идемпотентно ({@code sHasInit}) — как {@code hasInit} у Forge; повторные и пересекающиеся с GT6
	 * записи дополнительно снимает дедуп {@link #registerOre} (тот же hash-бакет, что у оригинала).
	 * Блок {@code replacements}/{@code exclusions} Forge-оригинала ({@code :136-190}) — роль-C, перенесён
	 * отдельным методом {@link #initVanillaRecipeReplacements}: ему нужен полный {@code RecipeManager},
	 * которого в этом окне ещё нет.</p>
	 */
	public static void initVanillaEntries() {
		if (sHasInit) return;
		sHasInit = true;

		// дерево: 1.7.10 log/log2/planks/wooden_slab/sapling/leaves/leaves2 — по одному блоку с метой-породой
		for (net.minecraft.world.level.block.Block tLog : new net.minecraft.world.level.block.Block[]{Blocks.OAK_LOG, Blocks.SPRUCE_LOG, Blocks.BIRCH_LOG, Blocks.JUNGLE_LOG, Blocks.ACACIA_LOG, Blocks.DARK_OAK_LOG}) registerOre("logWood", ST.make(tLog, 1, 0));
		for (net.minecraft.world.level.block.Block tPlank : new net.minecraft.world.level.block.Block[]{Blocks.OAK_PLANKS, Blocks.SPRUCE_PLANKS, Blocks.BIRCH_PLANKS, Blocks.JUNGLE_PLANKS, Blocks.ACACIA_PLANKS, Blocks.DARK_OAK_PLANKS}) registerOre("plankWood", ST.make(tPlank, 1, 0));
		for (net.minecraft.world.level.block.Block tSlab : new net.minecraft.world.level.block.Block[]{Blocks.OAK_SLAB, Blocks.SPRUCE_SLAB, Blocks.BIRCH_SLAB, Blocks.JUNGLE_SLAB, Blocks.ACACIA_SLAB, Blocks.DARK_OAK_SLAB}) registerOre("slabWood", ST.make(tSlab, 1, 0));
		for (net.minecraft.world.level.block.Block tStair : new net.minecraft.world.level.block.Block[]{Blocks.OAK_STAIRS, Blocks.SPRUCE_STAIRS, Blocks.BIRCH_STAIRS, Blocks.JUNGLE_STAIRS, Blocks.ACACIA_STAIRS, Blocks.DARK_OAK_STAIRS}) registerOre("stairWood", ST.make(tStair, 1, 0));
		registerOre("stickWood", ST.make(net.minecraft.world.item.Items.STICK, 1, 0));
		for (net.minecraft.world.level.block.Block tSap : new net.minecraft.world.level.block.Block[]{Blocks.OAK_SAPLING, Blocks.SPRUCE_SAPLING, Blocks.BIRCH_SAPLING, Blocks.JUNGLE_SAPLING, Blocks.ACACIA_SAPLING, Blocks.DARK_OAK_SAPLING}) registerOre("treeSapling", ST.make(tSap, 1, 0));
		for (net.minecraft.world.level.block.Block tLeaf : new net.minecraft.world.level.block.Block[]{Blocks.OAK_LEAVES, Blocks.SPRUCE_LEAVES, Blocks.BIRCH_LEAVES, Blocks.JUNGLE_LEAVES, Blocks.ACACIA_LEAVES, Blocks.DARK_OAK_LEAVES}) registerOre("treeLeaves", ST.make(tLeaf, 1, 0));

		// руды и блоки-хранилища (1.7.10 quartz_ore = НЕЗЕРСКИЙ кварц)
		registerOre("oreGold"      , ST.make(Blocks.GOLD_ORE        , 1, 0));
		registerOre("oreIron"      , ST.make(Blocks.IRON_ORE        , 1, 0));
		registerOre("oreLapis"     , ST.make(Blocks.LAPIS_ORE       , 1, 0));
		registerOre("oreDiamond"   , ST.make(Blocks.DIAMOND_ORE     , 1, 0));
		registerOre("oreRedstone"  , ST.make(Blocks.REDSTONE_ORE    , 1, 0));
		registerOre("oreEmerald"   , ST.make(Blocks.EMERALD_ORE     , 1, 0));
		registerOre("oreQuartz"    , ST.make(Blocks.NETHER_QUARTZ_ORE, 1, 0));
		registerOre("oreCoal"      , ST.make(Blocks.COAL_ORE        , 1, 0));
		// РАСЩЕПЛЁННЫЕ СЕМЬИ РУД (движок, а не мод): в 1.7.10 у каждой руды был ОДИН блок, в 26.1 их несколько,
		// и семью объявляет САМ движок — data/minecraft/tags/block/iron_ores.json = {iron_ore, deepslate_iron_ore},
		// gold_ores.json = {gold_ore, nether_gold_ore, deepslate_gold_ore}, и так все восемь. Новые члены идут под
		// ТЕМ ЖЕ именем, что уже перечисленный выше первый член — тот же приём, что применён здесь к sandstone/sand
		// (:278-282) и к брёвнам/доскам/листве (:177-183), где 1.7.10 обходился одной wildcard-записью.
		// У префикса OP.ore своя ветка разбора: она НЕ ставит цель унификации, а лишь выдаёт паспорт
		// (OreDictManager.java:466-467 addItemData_), поэтому состав целей этими строками не меняется.
		registerOre("oreGold"      , ST.make(Blocks.DEEPSLATE_GOLD_ORE    , 1, 0));
		registerOre("oreGold"      , ST.make(Blocks.NETHER_GOLD_ORE       , 1, 0));
		registerOre("oreIron"      , ST.make(Blocks.DEEPSLATE_IRON_ORE    , 1, 0));
		registerOre("oreLapis"     , ST.make(Blocks.DEEPSLATE_LAPIS_ORE   , 1, 0));
		registerOre("oreDiamond"   , ST.make(Blocks.DEEPSLATE_DIAMOND_ORE , 1, 0));
		registerOre("oreRedstone"  , ST.make(Blocks.DEEPSLATE_REDSTONE_ORE, 1, 0));
		registerOre("oreEmerald"   , ST.make(Blocks.DEEPSLATE_EMERALD_ORE , 1, 0));
		registerOre("oreCoal"      , ST.make(Blocks.DEEPSLATE_COAL_ORE    , 1, 0));
		// Древние обломки — незер-руда, ровно как незерский кварц выше; у Грега это материал MT.AncientDebris
		// (MT.java:1837 «Ancient Debris», внутреннее имя = AncientDebris — пробелы снимает OreDictMaterial.sanitize:212;
		// put(MD.NePl, COMMON_ORE) — в 1.7.10 этот контент давал бэкпорт-мод Netherite Plus). Что руда/самородок/слиток
		// ИМЕННО этого материала — незеритовое семейство, Грег говорит сам: LanguageHandler.java:314-326.
		registerOre("oreAncientDebris", ST.make(Blocks.ANCIENT_DEBRIS, 1, 0));
		registerOre("blockGold"    , ST.make(Blocks.GOLD_BLOCK      , 1, 0));
		registerOre("blockIron"    , ST.make(Blocks.IRON_BLOCK      , 1, 0));
		registerOre("blockLapis"   , ST.make(Blocks.LAPIS_BLOCK     , 1, 0));
		registerOre("blockDiamond" , ST.make(Blocks.DIAMOND_BLOCK   , 1, 0));
		registerOre("blockRedstone", ST.make(Blocks.REDSTONE_BLOCK  , 1, 0));
		registerOre("blockEmerald" , ST.make(Blocks.EMERALD_BLOCK   , 1, 0));
		registerOre("blockQuartz"  , ST.make(Blocks.QUARTZ_BLOCK    , 1, 0));
		registerOre("blockCoal"    , ST.make(Blocks.COAL_BLOCK      , 1, 0));
		// НОВЫЕ ванильные блоки-хранилища (1.16-1.17) — продолжение ровно этого списка. Имена — то же правило
		// «префикс + материал»: blockNetherite и blockRaw<Материал> уже живут в языке Грега как ключи его же
		// пере-регистраций (LoaderOreDictReRegistrations.java:246 blockNetherite -> blockIngotNetherite;
		// OP.java:349 blockRaw = "New Vanilla Storage Block for Ores" — префикс заведён Грегом ИМЕННО под этот
		// ванильный блок из будущего). Ёмкость сходится с блочными префиксами Грега (9 единиц): ванильные
		// рецепты netherite_block и raw_*_block — 3x3 (data/minecraft/recipe/netherite_block.json,
		// raw_iron_block.json). ⛔ amethyst_block сюда НЕ входит: его ванильный рецепт 2x2 = ЧЕТЫРЕ осколка
		// (recipe/amethyst_block.json), а blockGem у Грега — девять (OP.java:350 setMaterialStats(U*9)); имя
		// blockAmethyst разбирается в blockGem+Amethyst и объявило бы неверную массу, попутно перехватив цель
		// унификации грегского «Block of Amethyst». Соответствия у Грега для «блока из четырёх» нет — не заводим.
		registerOre("blockNetherite", ST.make(Blocks.NETHERITE_BLOCK, 1, 0));
		registerOre("blockRawIron"  , ST.make(Blocks.RAW_IRON_BLOCK , 1, 0));
		registerOre("blockRawGold"  , ST.make(Blocks.RAW_GOLD_BLOCK , 1, 0));

		// стекло: бесцветное + весь цветной ряд (1.7.10 — одна wildcard-запись stained_glass:W)
		registerOre("blockGlassColorless", ST.make(Blocks.GLASS     , 1, 0));
		registerOre("blockGlass"         , ST.make(Blocks.GLASS     , 1, 0));
		for (int i = 0; i < gregapi.data.CS.Flattened.STAINED_GLASS.length; i++) registerOre("blockGlass", ST.make(gregapi.data.CS.Flattened.STAINED_GLASS[i], 1, 0));
		registerOre("paneGlassColorless" , ST.make(Blocks.GLASS_PANE, 1, 0));
		registerOre("paneGlass"          , ST.make(Blocks.GLASS_PANE, 1, 0));
		for (int i = 0; i < gregapi.data.CS.Flattened.STAINED_GLASS_PANE.length; i++) registerOre("paneGlass", ST.make(gregapi.data.CS.Flattened.STAINED_GLASS_PANE[i], 1, 0));

		// слитки/самородки/камни/пыли/еда (1.7.10 gemLapis = dye:4)
		registerOre("ingotIron"      , ST.make(net.minecraft.world.item.Items.IRON_INGOT    , 1, 0));
		registerOre("ingotGold"      , ST.make(net.minecraft.world.item.Items.GOLD_INGOT    , 1, 0));
		registerOre("ingotBrick"     , ST.make(net.minecraft.world.item.Items.BRICK         , 1, 0));
		registerOre("ingotBrickNether", ST.make(net.minecraft.world.item.Items.NETHER_BRICK , 1, 0));
		registerOre("nuggetGold"     , ST.make(net.minecraft.world.item.Items.GOLD_NUGGET   , 1, 0));
		// НОВЫЕ ванильные предметы-материалы (1.11-1.17) — продолжение того же списка теми же именами Грега:
		// nuggetIron рядом с nuggetGold, gemAmethyst рядом с gemDiamond/gemEmerald/gemQuartz, ingotNetherite
		// (ключ пере-регистрации LoaderOreDictReRegistrations.java:107), «Netherite Scrap» = ingot материала
		// AncientDebris (LanguageHandler.java:326 — Грег сам зовёт этим именем ИМЕННО ингот этого материала),
		// oreRaw<Материал> для сырой руды (OP.java:137 — «The Vanilla "Raw Ore" Item from the Future»).
		// Медь в этот список НЕ входит намеренно: ванильное медное семейство целиком ведёт ADAPT-014
		// (Loader_Recipes_Vanilla.java:1073-1105) — там оно и опознаётся, и СОЗНАТЕЛЬНО оставляет целью
		// унификации грегский слиток; дублировать его здесь значило бы перехватить паспорта первым.
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
		// 1.7.10 sandstone:W = гладкий/резной/обычный; sand:W = обычный + красный
		registerOre("sandstone"      , ST.make(Blocks.SANDSTONE         , 1, 0));
		registerOre("sandstone"      , ST.make(Blocks.CHISELED_SANDSTONE, 1, 0));
		registerOre("sandstone"      , ST.make(Blocks.CUT_SANDSTONE     , 1, 0));
		registerOre("sand"           , ST.make(Blocks.SAND              , 1, 0));
		registerOre("sand"           , ST.make(Blocks.RED_SAND          , 1, 0));

		// пластинки (1.7.10 record_*; в neo music_disc_*)
		for (net.minecraft.world.item.Item tRecord : new net.minecraft.world.item.Item[]{
			net.minecraft.world.item.Items.MUSIC_DISC_13, net.minecraft.world.item.Items.MUSIC_DISC_CAT, net.minecraft.world.item.Items.MUSIC_DISC_BLOCKS, net.minecraft.world.item.Items.MUSIC_DISC_CHIRP,
			net.minecraft.world.item.Items.MUSIC_DISC_FAR, net.minecraft.world.item.Items.MUSIC_DISC_MALL, net.minecraft.world.item.Items.MUSIC_DISC_MELLOHI, net.minecraft.world.item.Items.MUSIC_DISC_STAL,
			net.minecraft.world.item.Items.MUSIC_DISC_STRAD, net.minecraft.world.item.Items.MUSIC_DISC_WARD, net.minecraft.world.item.Items.MUSIC_DISC_11, net.minecraft.world.item.Items.MUSIC_DISC_WAIT}) registerOre("record", ST.make(tRecord, 1, 0));

		// красители и цвет-именованные ряды: порядок Forge (dye:0 = Black … dye:15 = White), стекло/панель — навстречу (15 - i)
		final String[] tDyes = {"Black", "Red", "Green", "Brown", "Blue", "Purple", "Cyan", "LightGray", "Gray", "Pink", "Lime", "Yellow", "LightBlue", "Magenta", "Orange", "White"};
		for (int i = 0; i < 16; i++) {
			registerOre("dye"                , ST.make(gregapi.data.CS.Flattened.DYE[i], 1, 0));
			registerOre("dye"       + tDyes[i], ST.make(gregapi.data.CS.Flattened.DYE[i], 1, 0));
			registerOre("blockGlass"+ tDyes[i], ST.make(gregapi.data.CS.Flattened.STAINED_GLASS     [15-i], 1, 0));
			registerOre("paneGlass" + tDyes[i], ST.make(gregapi.data.CS.Flattened.STAINED_GLASS_PANE[15-i], 1, 0));
		}
		// НОВЫЕ чистые красители neo (1.14: black/blue/brown/white_dye) в словарь НЕ регистрируются:
		// состав ore-списков остаётся 1:1 с 1.7.10 (их принимает сам ванильный датапак-рецепт, а
		// ore-версии роли-C дают GT-пыли — функция игрока полна; см. tReplacements роли-C).
	}

	private static boolean sHasReplacedRecipes = false;

	/**
	 * Роль-C переходника F4: ЗАМЕНА ванильных верстак-рецептов ore-версиями, которую в 1.7.10 делал САМ Forge —
	 * вторая половина {@code OreDictionary.initVanillaEntries} (эталон {@code gt6-oracle-dumper/.../OreDictionary.java:136-260}):
	 * обход {@code CraftingManager}, и каждый {@code Shaped/ShapelessRecipes}, чьи входы содержат предмет из карты
	 * {@code replacements} (палка/доски/камень/булыжник/слитки/алмаз/изумруд/редстоун/светопыль/слизь/стекло/красители/витражи),
	 * пере-регистрировался как {@code Shaped/ShapelessOreRecipe} с ore-списком вместо предмета. Именно так GT-палки,
	 * GT-доски и GT-красители работали в ВАНИЛЬНЫХ рецептах (лук, кровать, полка, витражи, факел...).
	 *
	 * <p><b>Улика, что это не догадка:</b> сверка {@code crafting.jsonl} против golden (2026-08-06) — в golden
	 * ровно 92 таких рецепта (cls {@code shapedorerecipe} 90 + {@code shapelessorerecipe} 2), в порте было 0;
	 * прежняя пометка роли-B «блок replacements не переносится» этим опровергнута.</p>
	 *
	 * <p><b>Адаптация к neo (движок иной — централизованно, на его уровне):</b></p>
	 * <ul>
	 * <li>ванильные рецепты живут в датапаке ({@code RecipeManager}), рантайм-удаления в neo нет
	 *     ({@code RecipeManager.java:74}) → ванильный рецепт ОСТАЁТСЯ, ore-версия добавляется в постоянный буфер GT6
	 *     ({@code CR.BUFFER}, читается диспетчером F11 живьём). Функция игрока та же, что в 1.7.10: ore-версия —
	 *     супермножество ванильной (тот же выход на тех же входах плюс ore-альтернативы);</li>
	 * <li>wildcard-мета семей 1.7.10 → члены поимённо из центра {@code CS.Flattened} — тот же приём, что роль-B выше;</li>
	 * <li>момент — server-start, ПОСЛЕ {@code runDeferredItemInit} (окно, куда F12 сдвинул весь stack-init):
	 *     {@code RecipeManager} полон датапаком, словарь полон и ванилью (роль-B), и GT6-стеками.</li>
	 * </ul>
	 *
	 * <p>Ore-списки кладутся ЖИВЫМИ ({@code getOres} возвращает живой список — 1:1 с Forge, который клал тот же
	 * живой {@code List} из своего словаря): предметы, зарегистрированные под ключом позже, подхватываются сами.</p>
	 */
	public static void initVanillaRecipeReplacements(net.minecraft.server.MinecraftServer aServer) {
		if (sHasReplacedRecipes || aServer == null) return;
		sHasReplacedRecipes = true;

		// Карта замен — дословно Forge :137-153,176-190; расщеплённые семьи перечислены тем же приёмом, что роль-B.
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
		// НОВЫЕ чистые красители neo (1.14) — датапак-рецепты покраски требуют их, не исторические
		// ink_sac/lapis/cocoa/bone_meal; ore-ключи те же (см. роль-B выше).
		tReplacements.put(net.minecraft.world.item.Items.BLACK_DYE, "dyeBlack");
		tReplacements.put(net.minecraft.world.item.Items.BLUE_DYE , "dyeBlue");
		tReplacements.put(net.minecraft.world.item.Items.BROWN_DYE, "dyeBrown");
		tReplacements.put(net.minecraft.world.item.Items.WHITE_DYE, "dyeWhite");

		// Исключения — дословно Forge :196-211. Карта АДРЕСОВ 1.7.10→neo: stone_slab расщеплён по подтипам
		// (recompSrc BlockStoneSlab.java:17 — stone/sand/wood/cobble/brick/smoothStoneBrick/netherBrick/quartz);
		// 1.7.10 minecraft:stone_stairs — лестница ИЗ БУЛЫЖНИКА → neo cobblestone_stairs (neo stone_stairs — новый
		// блок, его тут не было). SMOOTH_STONE_SLAB добавлен рядом со STONE_SLAB: подтип «stone» 1.7.10 несёт
		// обе роли, исключение шире на один блок безвредно (его рецепт из smooth_stone, которого нет в карте замен).
		final java.util.Set<net.minecraft.world.item.Item> tExclusions = new java.util.HashSet<>();
		tExclusions.add(Blocks.LAPIS_BLOCK.asItem());
		tExclusions.add(net.minecraft.world.item.Items.COOKIE);
		tExclusions.add(Blocks.STONE_BRICKS.asItem());
		for (net.minecraft.world.level.block.Block tSlab : new net.minecraft.world.level.block.Block[]{Blocks.STONE_SLAB, Blocks.SMOOTH_STONE_SLAB, Blocks.SANDSTONE_SLAB, Blocks.PETRIFIED_OAK_SLAB, Blocks.COBBLESTONE_SLAB, Blocks.BRICK_SLAB, Blocks.STONE_BRICK_SLAB, Blocks.NETHER_BRICK_SLAB, Blocks.QUARTZ_SLAB}) tExclusions.add(tSlab.asItem());
		tExclusions.add(Blocks.COBBLESTONE_STAIRS.asItem());
		tExclusions.add(Blocks.COBBLESTONE_WALL.asItem());
		for (net.minecraft.world.level.block.Block tStair : new net.minecraft.world.level.block.Block[]{Blocks.OAK_STAIRS, Blocks.SPRUCE_STAIRS, Blocks.BIRCH_STAIRS, Blocks.JUNGLE_STAIRS, Blocks.ACACIA_STAIRS, Blocks.DARK_OAK_STAIRS}) tExclusions.add(tStair.asItem());
		tExclusions.add(Blocks.GLASS_PANE.asItem());

		// F4 роль-C, правило коллизии (без него ore-версия СТИРАЕТ породу/цвет выхода — симптом игрока: из еловых
		// досок крафтилась АКАЦИЕВАЯ дверь). Замена ингредиента на ore-ключ делает сетку менее специфичной; если после
		// этого два разных ванильных рецепта становятся неразличимы по входу, а выходы у них разные, — крафт даёт
		// произвольный из них (побеждает первый в буфере). В 1.7.10 Грег гасил ровно это, но СПИСКОМ (tExclusions:
		// полублоки и лестницы — единственные породные семьи 1.7.10, OreDictionary:282-290). В neo таких семей на
		// порядок больше (двери, люки, заборы, ворота, кнопки, плиты, знаки, лодки…), поэтому список заменён ПРАВИЛОМ:
		// кандидаты группируются по сигнатуре сетки, и группа с несколькими разными выходами не попадает в буфер
		// вовсе — ванильный датапак-рецепт остаётся действовать как есть (порода сохраняется).
		final java.util.Map<String, java.util.List<gregapi.recipes.ICraftingRecipeGT>> tBySignature = new java.util.LinkedHashMap<>();
		final java.util.Map<String, java.util.Set<net.minecraft.world.item.Item>> tOutputsBySignature = new java.util.HashMap<>();

		int tReplaced = 0;
		for (net.minecraft.world.item.crafting.RecipeHolder<?> tHolder : aServer.getRecipeManager().getRecipes()) {
			try {
				if (tHolder.value() instanceof net.minecraft.world.item.crafting.ShapedRecipe tShaped) {
					ItemStack tOutput = tShaped.assemble(net.minecraft.world.item.crafting.CraftingInput.EMPTY); // result.create(), input не читается (референс ShapedRecipe.java:68-70)
					if (ST.invalid(tOutput) || tExclusions.contains(tOutput.getItem())) continue;
					int tWidth = tShaped.pattern.width(), tHeight = tShaped.pattern.height();
					StringBuilder tSignature = new StringBuilder(tWidth + "x" + tHeight + ":");
					Object[] tCells = replaceIngredients(tShaped.pattern.ingredients(), tReplacements, tSignature);
					if (tCells == null) continue; // ни одной замены либо custom-ингредиент — рецепт не наш (1:1 containsMatch)
					// Синтез Forge-формата (строки + пары символ→ингредиент) для конструктора ShapedOreRecipe.
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
					tRecipe.mVanillaReplacement = true; // статус Forge-замены 1.7.10: обрабатывается сканом Loader_Recipes_Replace
					tRecipe.mSourceId = (net.minecraft.resources.ResourceKey<net.minecraft.world.item.crafting.Recipe<?>>)(net.minecraft.resources.ResourceKey<?>)tHolder.id();
					tBySignature.computeIfAbsent(tSignature.toString(), k -> new java.util.ArrayList<>()).add(tRecipe);
					tOutputsBySignature.computeIfAbsent(tSignature.toString(), k -> new java.util.HashSet<>()).add(tOutput.getItem());
				} else if (tHolder.value() instanceof net.minecraft.world.item.crafting.ShapelessRecipe tShapeless) {
					ItemStack tOutput = tShapeless.assemble(net.minecraft.world.item.crafting.CraftingInput.EMPTY);
					if (ST.invalid(tOutput) || tExclusions.contains(tOutput.getItem())) continue;
					net.minecraft.world.item.crafting.PlacementInfo tPlacement = tShapeless.placementInfo();
					if (tPlacement.isImpossibleToPlace()) continue;
					java.util.List<java.util.Optional<net.minecraft.world.item.crafting.Ingredient>> tIngredients = new java.util.ArrayList<>();
					for (net.minecraft.world.item.crafting.Ingredient tIn : tPlacement.ingredients()) tIngredients.add(java.util.Optional.of(tIn));
					StringBuilder tRawSignature = new StringBuilder();
					Object[] tCells = replaceIngredients(tIngredients, tReplacements, tRawSignature);
					if (tCells == null) continue;
					gregapi.recipes.ShapelessOreRecipe tRecipe = new gregapi.recipes.ShapelessOreRecipe(tOutput, tCells);
					tRecipe.mVanillaReplacement = true;
					tRecipe.mSourceId = (net.minecraft.resources.ResourceKey<net.minecraft.world.item.crafting.Recipe<?>>)(net.minecraft.resources.ResourceKey<?>)tHolder.id();
					// shapeless: порядок ячеек значения не имеет — сигнатура нормализуется сортировкой, иначе
					// одинаковые по сути сетки разошлись бы по разным группам и коллизия осталась бы незамеченной.
					String[] tParts = tRawSignature.toString().split("\\|", -1);
					java.util.Arrays.sort(tParts);
					String tSignature = "shapeless:" + String.join("|", tParts);
					tBySignature.computeIfAbsent(tSignature, k -> new java.util.ArrayList<>()).add(tRecipe);
					tOutputsBySignature.computeIfAbsent(tSignature, k -> new java.util.HashSet<>()).add(tOutput.getItem());
				}
			} catch(Throwable e) {e.printStackTrace(gregapi.data.CS.ERR);}
		}
		// Правило коллизии: группа сигнатуры с более чем одним выходом отбрасывается целиком — ore-версия там не
		// «супермножество ванильной», а подмена породы/цвета. Ванильные датапак-рецепты этой группы не трогаются.
		int tCollided = 0;
		for (Map.Entry<String, java.util.List<gregapi.recipes.ICraftingRecipeGT>> tEntry : tBySignature.entrySet()) {
			if (tOutputsBySignature.get(tEntry.getKey()).size() > 1) {tCollided += tEntry.getValue().size(); continue;}
			for (gregapi.recipes.ICraftingRecipeGT tRecipe : tEntry.getValue()) {gregapi.util.CR.BUFFER.add(tRecipe); tReplaced++;}
		}
		gregapi.data.CS.OUT.println("GT_API: Vanilla recipe replacements (F4 role-C): " + tReplaced + " ore-versions added to CR.BUFFER, " + tCollided + " skipped as ambiguous (output family collision).");
	}

	/**
	 * Ячейки для ore-рецепта из ингредиентов ванильного: замещаемый предмет → ЖИВОЙ ore-список, прочие —
	 * {@code ItemStack}/{@code List<ItemStack>} членов как есть. {@code null}-возврат = «рецепт не кандидат»
	 * (ни одной замены — 1:1 с Forge {@code containsMatch}, — либо custom-ингредиент, которого в 1.7.10 не было).
	 */
	private static Object[] replaceIngredients(java.util.List<java.util.Optional<net.minecraft.world.item.crafting.Ingredient>> aIngredients, Map<net.minecraft.world.item.Item, String> aReplacements, StringBuilder aSignature) {
		Object[] rCells = new Object[aIngredients.size()];
		boolean tAnyReplaced = false;
		for (int i = 0; i < rCells.length; i++) {
			if (i > 0) aSignature.append('|');
			java.util.Optional<net.minecraft.world.item.crafting.Ingredient> tOpt = aIngredients.get(i);
			if (tOpt.isEmpty()) continue;
			net.minecraft.world.item.crafting.Ingredient tIn = tOpt.get();
			if (tIn.isCustom()) return null;
			java.util.List<net.minecraft.world.item.Item> tItems = tIn.items().map(net.minecraft.core.Holder::value).toList();
			if (tItems.isEmpty()) return null;
			String tOreName = null;
			for (net.minecraft.world.item.Item tItem : tItems) if ((tOreName = aReplacements.get(tItem)) != null) break;
			if (tOreName != null) {
				rCells[i] = getOres(tOreName);
				tAnyReplaced = true;
				aSignature.append(tOreName); // ячейка после замены — именно ore-ключ; по нему и считается неоднозначность
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

	/**
	 * F4 §4.2 — простой GT6-класс-носитель взамен Forge {@code OreDictionary.OreRegisterEvent}.
	 * Поля {@code Name}/{@code Ore} — ровно то, что читает {@code OreDictManager.onOreRegistration1}.
	 * Вложен в {@code OreDictionary}, чтобы импорт {@code OreDictionary.OreRegisterEvent} остался 1:1.
	 */
	public static class OreRegisterEvent extends Event {
		public final String Name;
		public final ItemStack Ore;

		public OreRegisterEvent(String aName, ItemStack aOre) {
			Name = aName;
			Ore = aOre;
		}
	}
}

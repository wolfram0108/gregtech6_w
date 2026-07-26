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
	 * <p>Идемпотентно ({@code sHasInit}) — как {@code hasInit} у Forge; повторные и пересекающиеся с GT6
	 * записи дополнительно снимает дедуп {@link #registerOre} (тот же hash-бакет, что у оригинала).
	 * Блок {@code replacements}/{@code exclusions} Forge-оригинала ({@code :136-190}) сюда НЕ переносится:
	 * он обслуживал автозамену ингредиентов в ванильных верстак-рецептах, а не словарь (роль-A/B), и в порте
	 * за крафт отвечает свой диспетчер (F11).</p>
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
		registerOre("blockGold"    , ST.make(Blocks.GOLD_BLOCK      , 1, 0));
		registerOre("blockIron"    , ST.make(Blocks.IRON_BLOCK      , 1, 0));
		registerOre("blockLapis"   , ST.make(Blocks.LAPIS_BLOCK     , 1, 0));
		registerOre("blockDiamond" , ST.make(Blocks.DIAMOND_BLOCK   , 1, 0));
		registerOre("blockRedstone", ST.make(Blocks.REDSTONE_BLOCK  , 1, 0));
		registerOre("blockEmerald" , ST.make(Blocks.EMERALD_BLOCK   , 1, 0));
		registerOre("blockQuartz"  , ST.make(Blocks.QUARTZ_BLOCK    , 1, 0));
		registerOre("blockCoal"    , ST.make(Blocks.COAL_BLOCK      , 1, 0));

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

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
import gregapi.data.OD;
import gregapi.data.OP;
import gregapi.util.ST;
import gregapi.util.UT;

/**
 * F4/F1-b ТЕГ-МОСТ — ЕДИНСТВЕННОЕ место, где словарь GT6 разговаривает с системой конвенционных тегов
 * {@code forge:} (`decisions/F4-oredictionary.md` §4.4, `F1-item-metadata-model.md` §6.4 — там мост объявлен
 * и отложен, здесь построен).
 *
 * <p><b>Что изменил движок.</b> В 1.7.10 межмодовый обмен шёл через плоский Forge-словарь: чужой мод звал
 * {@code OreDictionary.registerOre("ingotCopper", stack)}, Forge рассылал {@code OreRegisterEvent}, GT6 его
 * ловил и сам решал, что с предметом делать ({@code OreDictManager.onOreRegistration1}). На 1.20.1 Forge
 * плоского словаря уже НЕТ — общий язык модов стал ТЕГАМИ, и на этой версии их неймспейс до сих пор
 * {@code forge:}, а не более поздний нейтральный {@code c:} (проверено кодом, не памятью — оба свидетеля
 * дают один ответ): движковый {@code net.minecraftforge.common.Tags} строит каждый тег как
 * {@code new ResourceLocation("forge", name)} ({@code Tags.java:181,193,488,507,571}), и живой мод той же
 * версии, {@code Applied Energistics 2} (ветка {@code forge/1.20.1}), объявляет конвенционные теги буквально
 * {@code forge:ingots/copper} ({@code ConventionTags.java:76}) — нигде в его датагене неймспейса {@code c:}
 * нет вовсе. Хранилище словаря порт уже интернализовал ({@link OreDictionary}, роль-A F4); недоставало именно
 * ТРАНСПОРТА к чужим модам — его и даёт этот класс.
 *
 * <p><b>Мост двусторонний, но стороны НЕ симметричны — так устроен движок, а не наш выбор.</b></p>
 *
 * <p><b>1. Внутрь (полная).</b> {@link #importFromTags()} читает РЕАЛЬНЫЙ реестр тегов
 * ({@code Registry.getTags()}, {@code Registry.java:176} — на этой версии движка возвращает
 * {@code Stream<Pair<TagKey<T>, HolderSet.Named<T>>>}, не голый {@code HolderSet.Named}), переводит каждый
 * конвенционный тег в oredict-имя GT6 по таблице ниже и подаёт содержимое в {@link OreDictionary#registerOre}
 * — В ТОТ ЖЕ ВХОД, которым в 1.7.10 приходили чужие моды. Дальше всё делает НЕТРОНУТЫЙ механизм Грега: дедуп,
 * рассылка {@code OreRegisterEvent}, разбор имени, {@code setTarget_} (унификация, {@code OreDictManager.java:549}
 * — формула имени {@code prefix.mNameInternal + material.mNameInternal}), {@code triggerVisibility}, пере-
 * регистрации {@code LoaderOreDictReRegistrations}. Ни одной новой ветки семантики здесь нет.
 *
 * <p><b>2. Наружу (ограничена движком).</b> Тег вешается на ЗАПИСЬ РЕЕСТРА ({@code Item}), а по решению F1
 * (модель B) у GT6 ОДИН {@code Item} на префикс, материал — компонент стека
 * ({@code PrefixItem.java:118} {@code ST.make(this, 1, i)}, {@code ST.java:206} {@code meta_} = компонент
 * {@code SUBTYPE}). Значит по-материальный тег {@code forge:ingots/tin} повесить НЕ НА ЧТО: пометив
 * {@code gt.meta.ingot}, мы объявили бы иридиевый слиток оловянным. Поэтому наружу отдаётся ровно то, что
 * выразимо ЧЕСТНО:
 * <ul>
 * <li>МАТЕРИАЛ-АГНОСТИЧНЫЕ группы ({@code forge:ingots}, {@code forge:dusts}, {@code forge:gems},
 *     {@code forge:nuggets}, {@code forge:rods}, {@code forge:ores}, {@code forge:storage_blocks},
 *     {@code forge:raw_materials}) — для них «один Item на префикс» это ТОЧНОЕ утверждение, а не приближение;
 *     их выдаёт датаген-провайдер {@code gregapi.data.GT6ConventionTags}, читающий таблицу ОТСЮДА;</li>
 * <li>по-материальные теги получаются САМИ, через собственную унификацию Грега: как только входящая сторона
 *     подала чужой/ванильный предмет под именем {@code ingotCopper}, {@code setTarget_} делает ЕГО
 *     целью унификации материала — и в мире у игрока лежит именно он, уже лежащий в {@code forge:ingots/copper}
 *     данными своего владельца. Ровно так GT6 и работал в 1.7.10 (медь/железо/сертус — чужие предметы).</li>
 * </ul>
 * Остаток (материал, которого нет ни у кого, кроме GT6) по-материальным тегом не выражается — это
 * форсированная движком граница F1-b, а не забытая ветка.
 *
 * <p><b>Правило именования — здесь и только здесь.</b> GT6-имя это {@code prefix.mNameInternal +
 * material.mNameInternal} ({@code OreDictManager.java:549} — дословно 1:1 с оригиналом
 * {@code gt6-original/.../OreDictManager.java:527,752}). Конвенционное имя — {@code forge:<группа>/<материал>}.
 * Значит нужны ровно две вещи: таблица «группа ⇄ префикс» и функция «имя материала ⇄ snake_case»
 * ({@link #snake}/{@link #camel}). Всё, что этим правилом не описывается, живёт ЯВНЫМ списком
 * {@link #sExceptions}, а не разбросом по коду.
 *
 * <p><b>⛔ Конвенционные имена не выдумываются.</b> Каждая строка таблицы подтверждена файлом на диске:
 * {@code forge-1201-decompiled/.../common/Tags.java} (:220 dusts, :256 gems, :310 ingots, :321 nuggets,
 * :349/:127 ores, :372 raw_materials, :376 rods, :395/:159 storage_blocks) и
 * {@code reference/mods/Applied-Energistics-2-1.20.1/.../ConventionTags.java} (:52 {@code forge:silicon}, :61/:64
 * certus, :73/:74 fluix, :93 rods/wooden). Единственная строка БЕЗ подтверждения на диске — группа
 * {@code plates}: она внесена по требованию ТЗ и работает ТОЛЬКО на вход (читаем тег, если его кто-то
 * объявил); наружу {@code forge:plates} НЕ пишется, чтобы не породить имя, существование которого нечем
 * доказать. Исключение {@code glass_blocks/cheap}, которое было на ветке `main` (NeoForge 26.1.2), сюда
 * СОЗНАТЕЛЬНО не перенесено: ни {@code net.minecraftforge.common.Tags} 1.20.1, ни датаген AE2 1.20.1
 * такой тег-группы не знают вовсе (AE2 1.20.1 ссылается лишь на плоский {@code forge:glass},
 * {@code ConventionTags.java:98-99}) — это движковый шов версии, а не забытая строка.
 */
public class OreDictTags {
	private OreDictTags() {/* только статика, как у остальных центров словаря */}

	/** Пространство имён конвенционных тегов на этой версии движка — {@code Tags.java:181,193,488,507,571}
	 *  (каждая фабрика тега строит {@code new ResourceLocation("forge", name)}); подтверждено живым модом
	 *  {@code Applied-Energistics-2-1.20.1/.../ConventionTags.java:76} ({@code forge:ingots/copper}). */
	public static final String CONVENTION = "forge";

	/** ЕДИНСТВЕННАЯ таблица «группа конвенционного тега → префикс GT6». Обе стороны моста читают ЕЁ. */
	private static Map<String, OreDictPrefix> sGroupToPrefix = null;
	/** Обратная таблица (исходящая сторона): префикс → группа. Строится из той же карты, копией не является. */
	private static Map<OreDictPrefix, String> sPrefixToGroup = null;
	/** ЯВНЫЙ список исключений: конвенционный путь тега (без {@code forge:}) → ГОТОВОЕ oredict-имя GT6. */
	private static Map<String, String> sExceptions = null;

	/** Ленивая сборка: таблица ссылается на {@link OP}, а тот запрещает создание префиксов после Init
	 *  ({@code OreDictPrefix.java:105}) — статический инициализатор этого класса мог бы дёрнуть {@code OP.<clinit>}
	 *  в чужой фазе. Отложенная сборка снимает вопрос порядка классов целиком. */
	private static void build() {
		if (sGroupToPrefix != null) return;
		Map<String, OreDictPrefix> tGroups = new LinkedHashMap<>();
		// Tags.java:310 forge:ingots/*   · AE2 ConventionTags:76 (copper)
		tGroups.put("ingots"        , OP.ingot );
		// Tags.java:321 forge:nuggets/*  · AE2 боеприпас Matter Cannon — 67 материалов
		tGroups.put("nuggets"       , OP.nugget);
		// Tags.java:220 forge:dusts/*    · AE2 ConventionTags:64,73 (certus_quartz, fluix)
		tGroups.put("dusts"         , OP.dust  );
		// Tags.java:256 forge:gems/*     · AE2 ConventionTags:61,74 (certus_quartz, fluix)
		tGroups.put("gems"          , OP.gem   );
		// Tags.java:376 forge:rods/*     · AE2 ConventionTags:93 (rods/wooden)
		tGroups.put("rods"          , OP.stick );
		// Tags.java:349 forge:ores/*     · Tags.java:127 (блочная половина)
		tGroups.put("ores"          , OP.ore   );
		// Tags.java:372 forge:raw_materials/* — «сырая руда» из ванили будущего, у GT6 это OP.oreRaw (OP.java:137)
		tGroups.put("raw_materials" , OP.oreRaw);
		// Tags.java:395 forge:storage_blocks/* — берётся ОБЩИЙ префикс block (OP.java:376), а не blockSolid/blockGem/
		// blockDust: разложить материал по конкретному виду блока умеет сам Грег — LoaderOreDictReRegistrations
		// («blockCertusQuartz» → «blockGemCertusQuartz»). Мост говорит на его языке, а не решает за него.
		tGroups.put("storage_blocks", OP.block );
		// plates — БЕЗ подтверждения на диске (в Tags.java группы нет, AE2 её не спрашивает). Только на ВХОД: если
		// тега никто не объявил, ветка молчит; наружу forge:plates не пишется (см. javadoc класса).
		tGroups.put("plates"        , OP.plate );
		sGroupToPrefix = tGroups;

		Map<OreDictPrefix, String> tBack = new LinkedHashMap<>();
		for (Map.Entry<String, OreDictPrefix> tEntry : tGroups.entrySet()) tBack.putIfAbsent(tEntry.getValue(), tEntry.getKey());
		sPrefixToGroup = tBack;

		Map<String, String> tExceptions = new LinkedHashMap<>();
		// forge:silicon — тег БЕЗ группы (AE2 ConventionTags:52, вход инскрайбера). Имя GT6 для того же вещества —
		// OD.itemSilicon (OD.java:235), оно же было им и в 1.7.10.
		tExceptions.put("silicon"           , OD.itemSilicon.toString());
		// forge:rods/wooden (Tags.java:378; AE2 ConventionTags:93 читает его как WOOD_STICK) — «wooden» это не имя
		// материала GT6; деревянная палка у Грега это stickWood (OD.java, вход ванильного словаря).
		tExceptions.put("rods/wooden"       , OD.stickWood.toString());
		sExceptions = tExceptions;
	}

	// ================================================================================================
	// ПРАВИЛО ИМЕНОВАНИЯ — одно место, обе стороны
	// ================================================================================================

	/** {@code CertusQuartz} → {@code certus_quartz}. Обратна {@link #camel}. */
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

	/** {@code certus_quartz} → {@code CertusQuartz}. Обратна {@link #snake}. */
	public static String camel(String aSnake) {
		if (UT.Code.stringInvalid(aSnake)) return "";
		StringBuilder rName = new StringBuilder(aSnake.length());
		for (String tPart : aSnake.split("_")) rName.append(UT.Code.capitalise(tPart));
		return rName.toString();
	}

	/** Исходящее имя: пара GT6 → полное имя конвенционного тега ({@code forge:ingots/iron}), либо {@code null},
	 *  если для этого префикса конвенционной группы нет ЛИБО имя материала не переживает круг
	 *  {@link #snake}→{@link #camel} (тогда конвенционного имени у него попросту не существует, и выдумывать
	 *  его запрещено). */
	public static String tagName(OreDictPrefix aPrefix, OreDictMaterial aMaterial) {
		build();
		if (aPrefix == null || aMaterial == null) return null;
		String tGroup = sPrefixToGroup.get(aPrefix);
		if (tGroup == null) return null;
		String tPath = snake(aMaterial.mNameInternal);
		if (!camel(tPath).equals(aMaterial.mNameInternal)) return null;
		return CONVENTION + ":" + tGroup + "/" + tPath;
	}

	/** Входящее имя: конвенционный тег → oredict-имя GT6 ({@code forge:ingots/iron} → {@code ingotIron}), либо
	 *  {@code null}, если тег не конвенционный или его группа мосту неизвестна. */
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
	// ИСХОДЯЩАЯ СТОРОНА — материал-агностичные группы (то, что выразимо на записи реестра)
	// ================================================================================================

	/** Префикс → материал-агностичный тег ПРЕДМЕТА, либо {@code null}. Второй копии этой связи в дереве быть
	 *  не должно: её читает и датаген-провайдер, и стенд-судья. */
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

	/** То же для БЛОКА: {@code forge:ores} и {@code forge:storage_blocks} существуют и в блочной половине
	 *  ({@code Tags.java:127,159}), а слитки/пыли/самородки блоками не бывают. */
	public static TagKey<Block> groupBlockTag(OreDictPrefix aPrefix) {
		if (aPrefix == null) return null;
		if (isStorageBlock(aPrefix)) return Tags.Blocks.STORAGE_BLOCKS; // Tags.java:159
		if (isOre(aPrefix)) return Tags.Blocks.ORES;                    // Tags.java:127
		return null;
	}

	/** Блоки-хранилища GT6 — ЯВНЫМ списком (OP.java:349-355 и соседние), а не по тег-данным префикса:
	 *  {@code STORAGE_BASED} несут и ящики ({@code crateGtRaw}, OP.java:336), которые хранилищем в смысле
	 *  {@code forge:storage_blocks} не являются. */
	private static boolean isStorageBlock(OreDictPrefix aPrefix) {
		return aPrefix == OP.blockSolid || aPrefix == OP.blockGem || aPrefix == OP.blockDust || aPrefix == OP.blockIngot || aPrefix == OP.blockRaw;
	}

	/** Рудные префиксы GT6 — тем же явным списком, что и в загрузчике руд ({@code Loader_Ores.java}). */
	private static boolean isOre(OreDictPrefix aPrefix) {
		return aPrefix == OP.ore || aPrefix == OP.oreSmall || aPrefix == OP.oreVanillastone || aPrefix == OP.oreSandstone
			|| aPrefix == OP.oreNetherrack || aPrefix == OP.oreEndstone || aPrefix == OP.oreGravel || aPrefix == OP.oreSand
			|| aPrefix == OP.oreRedSand || aPrefix == OP.oreBedrock;
	}

	// ================================================================================================
	// ВХОДЯЩАЯ СТОРОНА — чужие предметы из тегов в словарь GT6
	// ================================================================================================

	/** Сколько записей мост подал в словарь за последний вызов {@link #importFromTags} (для стенда-судьи). */
	public static int sImportedEntries = 0;
	/** Сколько конвенционных тегов мост опознал за последний вызов (для стенда-судьи). */
	public static int sImportedTags = 0;
	/** Сколько ВАНИЛЬНЫХ записей мост пропустил, отдав их роли-B ({@link OreDictionary#initVanillaEntries}) — для стенда-судьи. */
	public static int sSkippedVanilla = 0;

	/**
	 * Читает РЕАЛЬНЫЙ реестр тегов и подаёт чужие предметы в словарь GT6 под его же именами.
	 *
	 * <p>Момент вызова — начало окна {@code GT_API.runDeferredItemInit}, сразу за
	 * {@link OreDictionary#initVanillaEntries} и ДО регистрации собственных предметов GT6. Это не выбор
	 * удобства, а требование семантики: {@code setTarget_(..., aOverwrite=F)} ({@code OreDictManager.java:471})
	 * оставляет целью унификации ПЕРВОГО зарегистрированного — значит чужой предмет должен успеть до нашего,
	 * ровно как в 1.7.10 успевали моды, грузившиеся раньше GT6. {@code runDeferredItemInit} на этой ветке
	 * зовётся из {@code GT_API.onLevelLoadEarlyItemInit} на {@code LevelEvent.Load} оверворлда — том же
	 * событии, на котором уже держится роль-B ({@code initVanillaEntries}) и роль-C
	 * ({@code initVanillaRecipeReplacements}); теги реестра к этому моменту уже привязаны движком (тот же
	 * порядок, каким пользуются обе соседние роли).
	 *
	 * <p>Предметы GT6 из тегов НЕ подаются: у них материал живёт в компоненте стека, а из тега приходит голая
	 * запись реестра — стек с мета-0 означал бы {@code MT.Empty} и наврал бы словарю. Свои предметы приходят
	 * своим каналом ({@code PrefixItem.runDeferred}). Отбор — GT6-критерием {@code ST.isGT}.
	 *
	 * <p><b>ВАНИЛЬНЫЕ предметы из тегов не подаются тоже — и это то же самое разделение ролей, только с другой
	 * стороны.</b> Ванильный контент ведёт роль-B этого же переходника ({@link OreDictionary#initVanillaEntries}):
	 * там он перечислен поимённо, ИМЕНАМИ ГРЕГА и с осознанными изъятиями. Тег-мост существует ради ЧУЖИХ модов —
	 * ровно того потока, который в 1.7.10 приходил в GT6 через Forge-словарь. Два механизма, регистрирующих одно
	 * и то же, — это не «надёжнее», а гонка, и мост в ней быстрее: он работает в начале окна, поэтому его запись
	 * забирает себе и цель унификации ({@code setTarget_(..., aOverwrite=F)}, {@code OreDictManager.java:471}), и
	 * паспорт предмета ({@code addItemData_} — first-wins, {@code OreDictManager.java:667-670}) у того, кто решал
	 * осознанно. Вдобавок мост говорит именами ТЕГОВ, а не именами Грега, и на ванили это сразу видно:
	 * {@code forge:gems/quartz} дал бы «gemQuartz» там, где у Грега это своё имя ванильного кварца. Ванильный
	 * контент опознаётся тем же способом, каким его везде отличает сам Грег — {@link gregapi.data.MD#MC}
	 * {@code .owns} — а не отдельным списком исключений: правило здесь смысловое («ваниль — не зона тег-моста»),
	 * поимённый список ведёт роль-B.
	 *
	 * <p>Идемпотентен: повторный вызов гасится дедупом {@link OreDictionary#registerOre} (тот же hash-бакет,
	 * что у Forge), поэтому перезаход в мир записей не удваивает.
	 */
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
						// ваниль — не зона тег-моста, её ведёт роль-B (см. javadoc метода)
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

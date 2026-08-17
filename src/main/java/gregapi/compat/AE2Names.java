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
 * ЦЕНТР АДРЕСАЦИИ AE2 (этап Э2 слоя совместимости GT6 ↔ AE2) — ЕДИНСТВЕННОЕ место, которое переводит
 * имена предметов Applied Energistics 2 rv2 (Minecraft 1.7.10, «item.ItemMultiMaterial» + мета) в
 * плоские id AE2 15.4.10 («ae2:calculation_processor_press»).
 *
 * <p><b>Зачем центр, а не правка мест.</b> GT6 адресует чужой мод в 183 местах шести файлов
 * ({@code LoaderItemList}, {@code LoaderItemData}, {@code LoaderBookList}, {@code LoaderUnificationTargets},
 * {@code Loader_Recipes_Replace}, {@code Compat_Recipes_AppliedEnergistics}), и ВСЕ они сходятся в одну
 * воронку — {@code ST.make(ModData, String, long aSize, long aMeta)}: туда же приводят
 * {@code ST.block(ModData,String)}, {@code OM.data(ModData,…)}, {@code ST.item(ModData,String)},
 * {@code OreDictManager.setTarget(…,ModData,name,meta)}, {@code ItemStackMap.put(ModData,…)} и
 * {@code ItemStackSet.add(ModData,…)}. Поэтому таблица объявлена ЗДЕСЬ ровно один раз, а зовёт её ровно
 * один вызыватель — та самая воронка; исходник GT6 в местах адресации остаётся verbatim-1:1. Это тот же
 * приём, которым порт уже решил ванильную развёртку меты — {@code CS.Flattened} (карты в одном доме,
 * подстановка в {@code ST.make_}).
 *
 * <p><b>Три вердикта, молчаливых null нет.</b> Пара «имя + мета» получает один из трёх ответов:
 * <ol>
 * <li><b>носитель есть</b> — {@link #path} отдаёт путь id 15.4.10;</li>
 * <li><b>носителя нет</b> — {@link #reason} отдаёт ПРИЧИНУ (предмет удалён из AE2, соответствие не
 *     доказано); вызыватель обязан тихо пропустить свою регистрацию, а не подавать null в рецепт;</li>
 * <li><b>имя не наше</b> — {@link #owns} = F, воронка идёт прежним путём.</li>
 * </ol>
 *
 * <p><b>Откуда соответствия.</b> Три первоисточника, ни одной догадки:
 * <ol>
 * <li><b>левая сторона — исходники AE2 rv3 для 1.7.10</b> ({@code reference/mods/AE2-rv3-1.7.10}), той самой
 *     версии, с которой работал GT6. Имя, которым GT6 зовёт предмет, — это РЕЕСТРОВОЕ имя AE2, и строится
 *     оно там механически: блок — {@code "tile." + простое имя класса} ({@code AEBlockFeatureHandler:72}),
 *     предмет — {@code "item." + простое имя класса} ({@code ItemFeatureHandler:92}), причём классы
 *     {@code ItemMultiMaterial}/{@code ItemMultiPart} в реестре сохраняют своё полное имя, а в ресурсах
 *     зовутся {@code ItemMaterial}/{@code ItemPart} ({@code ItemFeatureHandler:83-90},
 *     {@code FeatureNameExtractor:45-51}). Смысл каждой меты объявлен явно: {@code MaterialType.java}
 *     (мета в скобках у константы), {@code PartType.java}, {@code ItemCrystalSeed.java:62-64},
 *     {@code BlockSkyStone.java:143-161}, {@code ApiBlocks.java:231-251};</li>
 * <li><b>правая сторона — реестр 15.4.10</b> ({@code reference/mods/Applied-Energistics-2-1.20.1}, ветка
 *     {@code forge/1.20.1} на релизном теге {@code forge/v15.4.10}): {@code appeng/api/ids/AEItemIds.java},
 *     {@code AEBlockIds.java}, {@code AEPartIds.java}, а ФАКТ регистрации — по ссылкам на эти константы из
 *     {@code appeng/core/definitions/AEItems.java}, {@code AEBlocks.java}, {@code AEParts.java} (id,
 *     объявленный в Ids, но не упомянутый в definitions, в реестр НЕ попадает — так обстоит с «очищенными»
 *     кристаллами);</li>
 * <li><b>мост между сторонами</b> — совпадение РОЛИ: рецепт rv3
 *     ({@code assets/appliedenergistics2/recipes/**}) против рецепта 15.4.10
 *     ({@code src/generated/resources/data/ae2/recipes/**}) клетка в клетку, и/или совпадение
 *     человеческого имени: {@code lang/en_US.lang} rv3 против {@code lang/en_us.json} 15.4.10.</li>
 * </ol>
 * У каждой строки ниже приведено, чем именно она доказана.
 *
 * <p><b>⚠️ Номера строк правой стороны сняты МАШИНОЙ по дереву 15.4.10, а не перенесены с ветки main.</b>
 * Файлы {@code ids} между линейками AE2 сдвинулись: например {@code charged_certus_quartz_crystal}
 * регистрируется на {@code AEItems.java:194} (на 26.1 — {@code :204}), а {@code quartz_block} стоит в
 * {@code en_us.json:156} (на 26.1 — {@code :151}). Совпадение имени ещё не значит совпадения адреса цитаты.
 */
public final class AE2Names {
	private AE2Names() {}

	/** Носитель есть: «имя rv2» либо «имя rv2#мета» → путь id в пространстве {@code ae2}. */
	private static final Map<String, String> MAPPED = new LinkedHashMap<>();
	/** Носителя нет: «имя rv2» либо «имя rv2#мета» → причина, которую печатает вызыватель/стенд. */
	private static final Map<String, String> GONE = new LinkedHashMap<>();
	/** Имена, у которых мета 1.7.10 была ПОДТИПОМ: искать сперва по «имя#мета», а найденному подтипу мету не ставить. */
	private static final Set<String> SPLIT = new LinkedHashSet<>();

	private static void map (String aName,             String aPath  ) {MAPPED.put(aName, aPath);}
	private static void map (String aName, long aMeta, String aPath  ) {SPLIT.add(aName); MAPPED.put(key(aName, aMeta), aPath);}
	private static void gone(String aName,             String aReason) {GONE .put(aName, aReason);}
	private static void gone(String aName, long aMeta, String aReason) {SPLIT.add(aName); GONE .put(key(aName, aMeta), aReason);}

	private static String key(String aName, long aMeta) {return aName + "#" + aMeta;}

	// ================================================================================================
	// Причины отсутствия носителя — по одной строке на класс, чтобы не расползались формулировки.
	// ================================================================================================
	private static final String
	  NO_SEED     = "семян кристаллов в AE2 15.4.10 нет: рост заменён budding-блоками (AEBlockIds.java:39-42 flawless/flawed/chipped/damaged_budding_quartz + :44-46 small/medium/large_quartz_bud), предмета-затравки в реестре нет. Меты GT6 — это стадии роста rv3: ItemCrystalSeed.java:62-64 объявляет CERTUS=0, NETHER=600, FLUIX=1200"
	, NO_ORE      = "рудного блока сертуса в AE2 15.4.10 нет: сертус даёт budding-цепь метеорита, ни OreQuartz, ни OreQuartzCharged в AEBlockIds не объявлены (в rv3 блоки были: appeng/block/solids/OreQuartz.java, OreQuartzCharged.java)"
	, NO_ORE_CASE = "имени с такой раскладкой букв не существовало и в AE2 rv3: реестровое имя блока там — «tile.» + простое имя КЛАССА (AEBlockFeatureHandler:72), а классы зовутся OreQuartz/OreQuartzCharged с большой буквы. Плюс самого рудного блока в 15.4.10 нет — см. соседнюю строку"
	, NO_PURIFIED = "«очищенный» кристалл объявлен (AEItemIds.java:203-205), но НЕ зарегистрирован: AEItems.java на PURIFIED_* не ссылается — предмета в реестре нет"
	, NO_BIOMETRIC= "биометрической карты в AE2 15.4.10 нет: греп «biometric» по appeng/**/*.java пуст (в rv3 предмет был: ToolBiometricCard, tools/network.recipe)"
	;

	static {
		// ============================================================================================
		// БЛОКИ. Мета 1.7.10 подтипом была ровно у двух имён — tile.BlockSkyStone и tile.BlockSkyChest
		// (обе семьи раскрыты ниже); у всех прочих имя адресует блок целиком, мета проходит насквозь.
		// ============================================================================================
		// [ДОКАЗАНО ПЕРВОИСТОЧНИКОМ: rv3 appeng/block/solids/BlockQuartz.java → реестровое имя «tile.BlockQuartz»,
		//   en_US.lang:22 «Certus Quartz Block»] → 15.4.10 AEBlockIds:109 quartz_block (регистрация AEBlocks:135),
		//   en_us.json:156 «Certus Quartz Block» — имя в имя. Роль совпала: rv3 decorative/crystals.recipe
		//   4 × crystalCertusQuartz (2×2) → BlockQuartz ⟺ 15.4.10 decorative/quartz_block.json 4 × certus_quartz_crystal.
		map("tile.BlockQuartz"                  , "quartz_block");
		// [ДОКАЗАНО ПЕРВОИСТОЧНИКОМ: rv3 BlockQuartzPillar / BlockQuartzChiseled, en_US.lang:26 «Certus Quartz
		//   Pillar» и :23 «Chiseled Certus Quartz Block»] → 15.4.10 AEBlockIds:113/114 quartz_pillar /
		//   chiseled_quartz_block (AEBlocks:139/140), en_us.json:164/109 — те же две строки дословно.
		map("tile.BlockQuartzPillar"            , "quartz_pillar");
		map("tile.BlockQuartzChiseled"          , "chiseled_quartz_block");
		// [ДОКАЗАНО ПЕРВОИСТОЧНИКОМ: rv3 BlockFluix, en_US.lang:21 «Fluix Block»] → 15.4.10 AEBlockIds:115
		//   fluix_block (AEBlocks:147), en_us.json:137 «Fluix Block». Роль: rv3 4 × crystalFluix (2×2) ⟺ fluix_block.json.
		map("tile.BlockFluix"                   , "fluix_block");
		// [ДОКАЗАНО ПЕРВОИСТОЧНИКОМ: rv3 BlockQuartzGlass, en_US.lang:24 «Quartz Glass»] → 15.4.10 AEBlockIds:120
		//   quartz_glass (AEBlocks:142), en_us.json:163 «Quartz Glass». Роль клетка в клетку: rv3
		//   decorative/quartzglass.recipe «dustQuartz glass dustQuartz / glass dustQuartz glass / dustQuartz glass
		//   dustQuartz» → 4 шт ⟺ 15.4.10 quartz_glass.json «aba/bab/aba» → 4 шт.
		map("tile.BlockQuartzGlass"             , "quartz_glass");
		// [ДОКАЗАНО ПЕРВОИСТОЧНИКОМ: rv3 BlockQuartzLamp, en_US.lang:25 «Vibrant Quartz Glass»] → 15.4.10
		//   AEBlockIds:121 quartz_vibrant_glass (AEBlocks:143), en_us.json:170 «Vibrant Quartz Glass». Роль: rv3
		//   «glowstone_dust BlockQuartzGlass glowstone_dust» ⟺ 15.4.10 quartz_vibrant_glass.json «aba».
		map("tile.BlockQuartzLamp"              , "quartz_vibrant_glass");
		// [ДОКАЗАНО ПЕРВОИСТОЧНИКОМ: rv3 BlockQuartzTorch, en_US.lang:27 «Charged Quartz Fixture»] → 15.4.10
		//   AEBlockIds:51 quartz_fixture (AEBlocks:146), en_us.json:162 «Charged Quartz Fixture» — имя в имя.
		//   Роль совпала вместе с ВЫХОДНЫМ ЧИСЛОМ: rv3 decorative/fixtures.recipe «CertusQuartzCrystalCharged +
		//   iron_ingot → 2 BlockQuartzTorch» ⟺ 15.4.10 quartz_fixture.json → count 2.
		map("tile.BlockQuartzTorch"             , "quartz_fixture");
		// Лестницы и плиты. [ДОКАЗАНО ПЕРВОИСТОЧНИКОМ] Имена rv3 задаются классами (appeng/block/stair/*.java)
		//   и строкой-параметром AEBaseSlabBlock (ApiBlocks.java:244-251), а человеческие имена rv3 и 15.4.10
		//   совпадают дословно: «Certus Quartz Stairs/Slabs» (en_US.lang:58/68) ⟺ en_us.json:169/168,
		//   «Certus Quartz Pillar…» :57/67 ⟺ :166/165, «Chiseled Certus Quartz…» :55/65 ⟺ :111/110,
		//   «Fluix Stairs/Slabs» :56/66 ⟺ :139/138. Число вход/выход у rv3 (3 блока → 6 плит; лесенка → 4)
		//   то же, что у ванильных форм 15.4.10. Реестр: AEBlockIds:131/136/135/130 (лестницы, AEBlocks:211/216/215/210)
		//   и AEBlockIds:161/166/165/160 (плиты, AEBlocks:238/243/242/237).
		map("tile.QuartzStairBlock"             , "quartz_stairs");
		map("tile.QuartzPillarStairBlock"       , "quartz_pillar_stairs");
		map("tile.ChiseledQuartzStairBlock"     , "chiseled_quartz_stairs");
		map("tile.FluixStairBlock"              , "fluix_stairs");
		map("tile.QuartzSlabBlock"              , "quartz_slab");
		map("tile.QuartzPillarSlabBlock"        , "quartz_pillar_slab");
		map("tile.ChiseledQuartzSlabBlock"      , "chiseled_quartz_slab");
		map("tile.FluixSlabBlock"               , "fluix_slab");
		// [ДОКАЗАНО ПЕРВОИСТОЧНИКОМ] Формы небесного камня. К какой мете пришита каждая лестница и плита,
		//   объявлено в rv3 ЯВНО: ApiBlocks.java:231-234 «new SkyStoneStairBlock(skyStone, 0)»,
		//   «SkyStoneBlockStairBlock(skyStone, 1)», «SkyStoneBrickStairBlock(skyStone, 2)»,
		//   «SkyStoneSmallBrickStairBlock(skyStone, 3)»; ApiBlocks.java:244-247 — те же четыре меты у плит.
		//   Человеческие имена совпадают дословно с 15.4.10: «Sky Stone Stairs» (en_US.lang:62) ⟺ en_us.json:183
		//   sky_stone_stairs (AEBlockIds:126, AEBlocks:206); «Sky Stone Block Stairs» (:59) ⟺ :194
		//   smooth_sky_stone_stairs (AEBlockIds:127); «Sky Stone Brick Stairs» (:60) ⟺ :175 (AEBlockIds:128);
		//   «Sky Stone Small Brick Stairs» (:61) ⟺ :181 (AEBlockIds:129). Плиты — то же: AEBlockIds:156-159,
		//   en_us.json:178/193/174/180.
		map("tile.SkyStoneStairBlock"           , "sky_stone_stairs");
		map("tile.SkyStoneBlockStairBlock"      , "smooth_sky_stone_stairs");
		map("tile.SkyStoneBrickStairBlock"      , "sky_stone_brick_stairs");
		map("tile.SkyStoneSmallBrickStairBlock" , "sky_stone_small_brick_stairs");
		map("tile.SkyStoneSlabBlock"            , "sky_stone_slab");
		map("tile.SkyStoneBlockSlabBlock"       , "smooth_sky_stone_slab");
		map("tile.SkyStoneBrickSlabBlock"       , "sky_stone_brick_slab");
		map("tile.SkyStoneSmallBrickSlabBlock"  , "sky_stone_small_brick_slab");

		// tile.BlockSkyStone — блочное имя, у которого мета была ПОДТИПОМ (четыре камня в одном блоке 1.7.10).
		// [ДОКАЗАНО ПЕРВОИСТОЧНИКОМ] Смысл каждой меты объявлен в rv3 BlockSkyStone.java:143-161: мета 1 →
		//   суффикс «.Block», 2 → «.Brick», 3 → «.SmallBrick», прочее → базовое имя. Через en_US.lang:38-41 это
		//   даёт: 0 «Sky Stone», 1 «Sky Stone Block», 2 «Sky Stone Brick», 3 «Sky Stone Small Brick». Ровно эти
		//   четыре строки стоят в 15.4.10: en_us.json:172 sky_stone_block «Sky Stone» (AEBlockIds:116, AEBlocks:148),
		//   :191 smooth_sky_stone_block «Sky Stone Block» (:117/:150), :173 sky_stone_brick (:118/:151),
		//   :179 sky_stone_small_brick (:119/:152). Цепь переделки та же: rv3 decorative/skystone.recipe
		//   «smelt 0 → 1; 1 → 2; 2 → 3» ⟺ 15.4.10 smelting/smooth_sky_stone_block.json, decorative/sky_stone_brick.json,
		//   decorative/sky_stone_small_brick.json.
		// W («любой») подтипом не является — отдаём базовый камень семьи, как и CS.Flattened.
		map("tile.BlockSkyStone",  0, "sky_stone_block");
		map("tile.BlockSkyStone",  1, "smooth_sky_stone_block");
		map("tile.BlockSkyStone",  2, "sky_stone_brick");
		map("tile.BlockSkyStone",  3, "sky_stone_small_brick");
		map("tile.BlockSkyStone",  W, "sky_stone_block");

		// tile.BlockSkyChest — вторая семья, схлопнутая метой. [ДОКАЗАНО ПЕРВОИСТОЧНИКОМ] rv3 en_US.lang:42-43:
		//   мета 0 «Sky Stone Chest», мета 1 «Sky Stone Block Chest»; rv3 misc/chests.recipe собирает первый из
		//   8 × BlockSkyStone:0, второй — из 8 × BlockSkyStone:1. В 15.4.10 обе строки стоят дословно:
		//   en_us.json:177 sky_stone_chest «Sky Stone Chest» (AEBlockIds:52, AEBlocks:154), :192
		//   smooth_sky_stone_chest «Sky Stone Block Chest» (:53/:155), и рецепты те же — misc/chests_sky_stone.json
		//   (8 × sky_stone_block) и chests_smooth_sky_stone.json (8 × smooth_sky_stone_block).
		//   GT6 зовёт только W (Compat:120, mortarize сундука в 8 blockDust) — W подтипом не является,
		//   отдаём базового члена семьи.
		map("tile.BlockSkyChest",  0, "sky_stone_chest");
		map("tile.BlockSkyChest",  1, "smooth_sky_stone_chest");
		map("tile.BlockSkyChest",  W, "sky_stone_chest");

		// Руды сертуса — носителя нет. GT6 зовёт их четырьмя написаниями, поэтому в таблице все четыре:
		// молчаливого null не остаётся ни у одного. [ДОКАЗАНО ПЕРВОИСТОЧНИКОМ] реестровые имена rv3 — только
		// «tile.OreQuartz» и «tile.OreQuartzCharged» (классы appeng/block/solids/OreQuartz.java,
		// OreQuartzCharged.java + правило AEBlockFeatureHandler:72); написания со строчной «o», которыми GT6
		// зовёт их в LoaderItemList:593-594, не резолвились и в 1.7.10.
		gone("tile.OreQuartz"                   , NO_ORE);
		gone("tile.oreQuartz"                   , NO_ORE_CASE);
		gone("tile.OreQuartzCharged"            , NO_ORE);
		gone("tile.oreQuartzCharged"            , NO_ORE_CASE);

		// ============================================================================================
		// ПРЕДМЕТЫ-ОДИНОЧКИ.
		// ============================================================================================
		// [ДОКАЗАНО ПЕРВОИСТОЧНИКОМ] Инструменты кварца. Имена rv3 строит FeatureNameExtractor:60-67: класс
		//   ToolQuartzWrench/ToolQuartzCuttingKnife + подимя CertusQuartzTools|NetherQuartzTools → «Quartz»
		//   заменяется на «CertusQuartz»/«NetherQuartz». Человеческие имена совпадают дословно с 15.4.10:
		//   «Certus Quartz Wrench» (en_US.lang) ⟺ en_us.json:736 certus_quartz_wrench (AEItemIds:172, AEItems:108);
		//   «Nether Quartz Wrench» ⟺ :862 (AEItemIds:180, AEItems:120); «Certus Quartz Cutting Knife» ⟺ :730
		//   (AEItemIds:173, AEItems:109); «Nether Quartz Cutting Knife» ⟺ :857 (AEItemIds:181, AEItems:121).
		map("item.ToolCertusQuartzWrench"       , "certus_quartz_wrench");
		map("item.ToolNetherQuartzWrench"       , "nether_quartz_wrench");
		map("item.ToolCertusQuartzCuttingKnife" , "certus_quartz_cutting_knife");
		map("item.ToolNetherQuartzCuttingKnife" , "nether_quartz_cutting_knife");
		// [ДОКАЗАНО ПЕРВОИСТОЧНИКОМ] rv3 «Wireless Terminal» / «Memory Card» / «View Cell» (en_US.lang:493/491/373)
		//   ⟺ 15.4.10 en_us.json:943 wireless_terminal (AEItemIds:52, AEItems:139), :852 memory_card
		//   (AEItemIds:41, AEItems:175), :931 view_cell (AEItemIds:40, AEItems:249) — имя в имя.
		map("item.ToolWirelessTerminal"         , "wireless_terminal");
		map("item.ToolMemoryCard"               , "memory_card");
		map("item.ItemViewCell"                 , "view_cell");
		// [ДОКАЗАНО ПЕРВОИСТОЧНИКОМ] rv3 имел РОВНО ОДИН переносной терминал — «ae2:ToolPortableCell»
		//   (en_US.lang:486 «Portable Cell»), и его рецепт tools/network.recipe: «BlockChest + Cell1kPart +
		//   BlockEnergyCell». В 15.4.10 семья расщеплена (item/fluid × тиры), но прямой наследник того самого
		//   рецепта ровно один: tools/portable_item_cell_1k.json = «ae2:me_chest + ae2:cell_component_1k +
		//   ae2:energy_cell + ae2:item_cell_housing» (AEItemIds:80, AEItems:158, en_us.json:886 «1k Portable Item
		//   Cell»). Три из четырёх входов — те же предметы, четвёртый — корпус, которого в rv3-рецепте не было.
		//   Прочие члены семьи наследника в rv3 не имеют вовсе. GT6 зовёт это имя один раз, подсказкой-книгой
		//   (LoaderBookList:372).
		map("item.ToolPortableCell"             , "portable_item_cell_1k");
		// [ДОКАЗАНО ПЕРВОИСТОЧНИКОМ] rv3 «1k/4k/16k/64k ME Storage Cell» (en_US.lang:367-370) ⟺ 15.4.10
		//   «1k…64k ME Item Storage Cell» (en_us.json:813/815/812/816): в 15.4.10 к имени добавилось слово Item,
		//   потому что появились жидкостные ячейки, которых в rv3 не было (греп «FluidCell» по rv3 пуст).
		//   Рецепт тот же: rv3 network/cells/storage.recipe «QuartzGlass dustRedstone QuartzGlass / dustRedstone
		//   Cell1kPart dustRedstone / iron iron iron» ⟺ 15.4.10 network/cells/item_storage_cell_1k.json «aba/bcb/ded».
		//   Реально зарегистрированы именно ITEM_CELL_* (AEItemIds:65-68, AEItems:251-254).
		map("item.ItemBasicStorageCell.1k"      , "item_storage_cell_1k");
		map("item.ItemBasicStorageCell.4k"      , "item_storage_cell_4k");
		map("item.ItemBasicStorageCell.16k"     , "item_storage_cell_16k");
		map("item.ItemBasicStorageCell.64k"     , "item_storage_cell_64k");
		// ⚠️ ЕДИНСТВЕННАЯ СТРОКА, ГДЕ ЛИНЕЙКИ AE2 РАСХОДЯТСЯ ЗНАЧЕНИЕМ, А НЕ ТОЛЬКО НОМЕРОМ ЦИТАТЫ.
		//   На 26.1 творческая ячейка зовётся «creative_storage_cell»; в 15.4.10 такого id нет ВООБЩЕ (греп по
		//   appeng/** и по en_us.json пуст), а её место занимает «creative_item_cell» — AEItemIds.java:78
		//   ITEM_CELL_CREATIVE = id("creative_item_cell"), регистрация AEItems.java:247, en_us.json:745
		//   «Creative ME Item Cell». [ДОКАЗАНО ПЕРВОИСТОЧНИКОМ] Наследование то же и тем же приёмом, что у
		//   ячеек хранения выше: rv3 en_US.lang:371 «item.appliedenergistics2.ItemCreativeStorageCell.name=
		//   Creative ME Storage Cell» (класс appeng/items/storage/ItemCreativeStorageCell, регистрация
		//   ApiItems.java:143) — к имени добавилось слово Item, потому что рядом появилась жидкостная
		//   creative_fluid_cell (en_us.json:744), которой в rv3 не существовало. Роль (творческая ячейка) и
		//   единственность наследника не изменились. GT6 зовёт имя один раз, подсказкой-книгой (LoaderBookList:381).
		map("item.ItemCreativeStorageCell"      , "creative_item_cell");
		// [ДОКАЗАНО ПЕРВОИСТОЧНИКОМ] rv3 «2³/16³/128³ Spatial Storage Cell» (en_US.lang:465-467) ⟺ 15.4.10
		//   en_us.json:924/923/922 spatial_storage_cell_2/16/128 (AEItemIds:75-77, AEItems:263-265) — три строки дословно.
		map("item.ItemSpatialStorageCell.2Cubed"  , "spatial_storage_cell_2");
		map("item.ItemSpatialStorageCell.16Cubed" , "spatial_storage_cell_16");
		map("item.ItemSpatialStorageCell.128Cubed", "spatial_storage_cell_128");
		// [ДОКАЗАНО ПЕРВОИСТОЧНИКОМ, носителя нет] rv3 предмет был (ToolBiometricCard, tools/network.recipe,
		//   en_US.lang:494); в 15.4.10 слова «biometric» нет во всём appeng/**.
		gone("item.ToolBiometricCard"           , NO_BIOMETRIC);
		// [ДОКАЗАНО ПЕРВОИСТОЧНИКОМ, носителя нет] rv3 ItemCrystalSeed.java:62-64 задаёт меты семян: CERTUS=0,
		//   NETHER=SINGLE_OFFSET=600, FLUIX=1200 — ровно те три числа, которыми GT6 зовёт их (Compat:86-88).
		//   В 15.4.10 семян нет: рост заменён budding-блоками (AEBlockIds:39-42).
		gone("item.ItemCrystalSeed"             , NO_SEED);

		// ============================================================================================
		// item.ItemMultiPart — «часть кабельной шины»; мета была номером части.
		// [ДОКАЗАНО ПЕРВОИСТОЧНИКОМ: rv3 PartType.java:111 «CableAnchor( 120, … PartCableAnchor.class )»] —
		//   мета 120 объявлена явно, гадать не о чем. Имя: en_US.lang:438 «Cable Anchor» ⟺ 15.4.10
		//   en_us.json:716 cable_anchor (AEPartIds:235, AEParts:91). Роль совпала вместе с числом:
		//   rv3 network/parts/cable-anchor.recipe «metalIngots knife → 3 ItemPart.CableAnchor» ⟺ GT6 RM.sawing
		//   даёт ровно 3 (Compat:67-78); 15.4.10 network/parts/cable_anchor.json — нож + #ae2:metal_ingots.
		// ============================================================================================
		map("item.ItemMultiPart", 120, "cable_anchor");

		// ============================================================================================
		// item.ItemMultiMaterial — сборный предмет rv3, мета = материал. Восемнадцать мет, которые зовёт GT6.
		// СМЫСЛ КАЖДОЙ МЕТЫ объявлен в первоисточнике перечислением: rv3 appeng/items/materials/MaterialType.java,
		// число в скобках у константы. Ниже у каждой строки указана её константа и строка того файла.
		// ============================================================================================
		// [ДОКАЗАНО ПЕРВОИСТОЧНИКОМ: MaterialType.java:46 CertusQuartzCrystalCharged(1)] → 15.4.10 AEItemIds:196
		//   charged_certus_quartz_crystal (AEItems:194). Имя в имя: rv3 en_US.lang:398 «Charged Certus
		//   Quartz Crystal» ⟺ 15.4.10 en_us.json:737. Роль совпала и у GT6: setTarget(OP.gem, ChargedCertusQuartz,
		//   мета 1) (LoaderUnificationTargets:832), OM.reg(OD.itemCertusQuartz, мета 1) (LoaderItemData:52).
		map("item.ItemMultiMaterial",  1, "charged_certus_quartz_crystal");
		// [ДОКАЗАНО ПЕРВОИСТОЧНИКОМ, носителя нет: MaterialType.java:62-64 PurifiedCertusQuartzCrystal(10),
		//   PurifiedNetherQuartzCrystal(11), PurifiedFluixCrystal(12)] — это «очищенные» кристаллы, а НЕ обычные.
		//   Первоисточник подтверждает и обе цепочки GT6: (а) rv3 ItemCrystalSeed.java:124-141 выдаёт из
		//   доросшего семени ровно purifiedCertus/Nether/Fluix — то же, что делает автоклав GT6 (Compat:86-88);
		//   (б) rv3 decorative/crystals.recipe собирает BlockQuartz из 8 × PurifiedCertusQuartzCrystal — ровно
		//   то Compressor-сжатие «8 × мета 10 → tile.BlockQuartz», что стоит у GT6 (Compat:97). В 15.4.10 все три
		//   объявлены (AEItemIds.java:203-205), но не зарегистрированы: AEItems.java на PURIFIED_* не ссылается.
		gone("item.ItemMultiMaterial", 10, NO_PURIFIED);
		gone("item.ItemMultiMaterial", 11, NO_PURIFIED);
		gone("item.ItemMultiMaterial", 12, NO_PURIFIED);
		// [ДОКАЗАНО ПЕРВОИСТОЧНИКОМ: MaterialType.java:66-68 CalcProcessorPress(13), EngProcessorPress(14),
		//   LogicProcessorPress(15); :74 SiliconPress(19)] — четыре формы инскрайбера. Имена дословны:
		//   rv3 en_US.lang:426/424/425 «Inscriber Calculation/Engineering/Logic Press», :430 «Inscriber Silicon
		//   Press» ⟺ 15.4.10 en_us.json:721/762/841/915 (AEItemIds:206-208 и :212, AEItems:201-203 и :207).
		//   Роль та же: rv3 materials/presses.recipe «blockIron + пресс → тот же пресс» ⟺ 15.4.10
		//   inscriber/*_press.json, и это же размножение стоит у GT6 (Compat:62-65).
		map("item.ItemMultiMaterial", 13, "calculation_processor_press");
		map("item.ItemMultiMaterial", 14, "engineering_processor_press");
		map("item.ItemMultiMaterial", 15, "logic_processor_press");
		map("item.ItemMultiMaterial", 19, "silicon_press");
		// [ДОКАЗАНО ПЕРВОИСТОЧНИКОМ: MaterialType.java:70-72 CalcProcessorPrint(16), EngProcessorPrint(17),
		//   LogicProcessorPrint(18); :75 SiliconPrint(20)] — печати. Имена дословны: rv3 en_US.lang:428/427/429
		//   «Printed Calculation/Engineering/Logic Circuit», :431 «Printed Silicon» ⟺ 15.4.10 en_us.json:890/891/
		//   892/893 (AEItemIds:209-211 и :213, AEItems:204-206 и :208). Роль та же клетка в клетку: rv3
		//   materials/circuits.recipe «PurifiedCertus + CalcPress → CalcPrint; gemDiamond + EngPress → EngPrint;
		//   ingotGold + LogicPress → LogicPrint; itemSilicon + SiliconPress → SiliconPrint» ⟺ 15.4.10
		//   inscriber/*_print.json; те же четыре штамповки у GT6 (Compat:53-59), с грегской пластиной вместо кристалла.
		map("item.ItemMultiMaterial", 16, "printed_calculation_processor");
		map("item.ItemMultiMaterial", 17, "printed_engineering_processor");
		map("item.ItemMultiMaterial", 18, "printed_logic_processor");
		map("item.ItemMultiMaterial", 20, "printed_silicon");
		// [ДОКАЗАНО ПЕРВОИСТОЧНИКОМ: MaterialType.java:77 NamePress(21)] → 15.4.10 AEItemIds:214 name_press
		//   (AEItems:209). Имя в имя: rv3 en_US.lang:432 «Inscriber Name Press» ⟺ 15.4.10 en_us.json:855.
		map("item.ItemMultiMaterial", 21, "name_press");
		// [ДОКАЗАНО ПЕРВОИСТОЧНИКОМ: MaterialType.java:79-81 LogicProcessor(22), CalcProcessor(23),
		//   EngProcessor(24)] — процессоры. Имена дословны: rv3 en_US.lang «Logic/Calculation/Engineering
		//   Processor» ⟺ 15.4.10 en_us.json:840/720/761 (AEItemIds:215-217, AEItems:210-212). Роль та же: rv3
		//   materials/processors.recipe «dustRedstone + <тип>Print + SiliconPrint → <тип>Processor» ⟺ 15.4.10
		//   inscriber/*_processor.json; ровно эти три тройки стоят у GT6 (Compat:80-82).
		map("item.ItemMultiMaterial", 22, "logic_processor");
		map("item.ItemMultiMaterial", 23, "calculation_processor");
		map("item.ItemMultiMaterial", 24, "engineering_processor");
		// [ДОКАЗАНО ПЕРВОИСТОЧНИКОМ: MaterialType.java:101 EmptyStorageCell(39)] — «ME Storage Housing»
		//   (rv3 en_US.lang:400), корпус ячейки. В 15.4.10 сущность РАЗДЕЛИЛАСЬ на item_cell_housing и
		//   fluid_cell_housing (AEItemIds:237-238), и носителем выбран ПРЕДМЕТНЫЙ (AEItems:232) — по трём
		//   независимым доводам:
		//   (а) рецепт наследуется клетка в клетку именно им: rv3 network/cells/empty.recipe
		//       «QuartzGlass dustRedstone QuartzGlass / dustRedstone _ dustRedstone / ingotIron ingotIron
		//       ingotIron» ⟺ 15.4.10 network/cells/item_cell_housing.json «aba / b b / cdc», c = ingots/iron;
		//       у fluid_cell_housing.json нижний ряд — сплошная медь, железа нет вовсе;
		//   (б) жидкостных ячеек в rv3 не было вообще (греп «FluidCell» по appeng/** пуст), поэтому
		//       fluid_cell_housing наследовать нечему — это новая сущность;
		//   (в) всё, что rv3 делал корпусом, в 15.4.10 делает предметный: rv3 network/cells/storage.recipe
		//       «Cell1kPart + EmptyStorageCell → ItemBasicStorageCell.1k», rv3 network/cells/view.recipe
		//       «certusCrystal + EmptyStorageCell → ItemViewCell» ⟺ 15.4.10 tools/portable_item_cell_1k.json и
		//       семейство item_storage_cell_*, где четвёртым входом стоит именно ae2:item_cell_housing.
		//   Роль в GT6 этому не противоречит: единственное упоминание — подсказка-книга категории 46 «AE Cells»
		//   (LoaderBookList:385), а корпус предметной ячейки — как раз предмет этой категории.
		map("item.ItemMultiMaterial", 39, "item_cell_housing");
		// [ДОКАЗАНО ПЕРВОИСТОЧНИКОМ: MaterialType.java:111 SkyDust(45)] → 15.4.10 AEItemIds:243 sky_dust
		//   (AEItems:238). Имя в имя: rv3 en_US.lang:433 «Sky Stone Dust» ⟺ 15.4.10 en_us.json:917. Роль совпала
		//   и у GT6: setTarget(OP.dust, MT.STONES.SkyStone, мета 45) (LoaderUnificationTargets:831); в rv3 ту же
		//   пыль дают мельницы из BlockSkyStone:0 (processing/grind.recipe).
		map("item.ItemMultiMaterial", 45, "sky_dust");
	}

	/** Знает ли центр это (мод, имя) — то есть должна ли воронка {@code ST.make} отдать ответ ему. */
	public static boolean owns(ModData aMod, String aName) {
		if (aMod == null || aName == null || !MD.AE.mID.equals(aMod.mID)) return F;
		return SPLIT.contains(aName) || MAPPED.containsKey(aName) || GONE.containsKey(aName);
	}

	/** Путь id 15.4.10 для пары «имя rv2 + мета»; {@code null} = носителя нет либо имя не наше. */
	public static String path(String aName, long aMeta) {
		if (aName == null) return null;
		return SPLIT.contains(aName) ? MAPPED.get(key(aName, aMeta)) : MAPPED.get(aName);
	}

	/** Есть ли у пары носитель в AE2 15.4.10. Этим вызыватель решает, регистрировать ли свою запись. */
	public static boolean has(String aName, long aMeta) {
		return path(aName, aMeta) != null;
	}

	/** ПРИЧИНА отсутствия носителя; {@code null} = носитель есть либо имя не наше. */
	public static String reason(String aName, long aMeta) {
		if (aName == null || path(aName, aMeta) != null) return null;
		if (!SPLIT.contains(aName)) return GONE.get(aName);
		String rReason = GONE.get(key(aName, aMeta));
		return rReason != null ? rReason : "мета " + aMeta + " у «" + aName + "» центру неизвестна (GT6 её не зовёт)";
	}

	/**
	 * ЕДИНСТВЕННЫЙ способ получить стек AE2 по имени 1.7.10. Зовётся из воронки
	 * {@code ST.make(ModData, String, long, long)}; сам ходит в реестр только через {@code ST.findItem}.
	 *
	 * <p>Мета: у расщеплённых имён подтип уже выражен САМИМ предметом, поэтому мета ему не ставится
	 * (доктрина {@code CS.Flattened}); {@code W} — не подтип, а «любой», и переживает резолв как есть.
	 * У нерасщеплённых имён мета передаётся насквозь — ровно как делал прежний путь.
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

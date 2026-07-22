/**
 * Copyright (c) 2019 Gregorius Techneticies
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

package gregapi.item;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;

/**
 * F16-creative-tab (централизация 1:1): 1.7.10 {@code Block/Item.setCreativeTab(CreativeTabs.tabX)} удалён — neo наполняет
 * вкладки событием {@link BuildCreativeModeTabContentsEvent}. Единая точка на весь мод (как один разговор с движком): каждый
 * блок/предмет в ctor вызывает {@link #assign} с neo-ключом вкладки (маппинг ванильных вкладок 1.7.10→neo ниже), а один
 * хендлер добавляет их варианты в нужную вкладку. Перечисление вариантов — существующими {@code getSubItems/getSubBlocks}
 * (порт их сохранил), boot-safe (сбой перечисления одного предмета не рушит загрузку). Замена россыпи setCreativeTab —
 * ровно тем же приёмом, что у Грегориуса (централизованный разговор с движком).
 */
public final class CreativeTabsGT {
	private CreativeTabsGT() {}

	// Маппинг ванильных 1.7.10-вкладок → neo ResourceKey (сверено neo CreativeModeTabs):
	// tabBlock→BUILDING_BLOCKS, tabDecorations→FUNCTIONAL_BLOCKS, tabRedstone→REDSTONE_BLOCKS,
	// tabTransport→TOOLS_AND_UTILITIES, tabMisc→INGREDIENTS, tabCombat→COMBAT, tabFood→FOOD_AND_DRINKS.
	public static final ResourceKey<CreativeModeTab> BLOCK       = CreativeModeTabs.BUILDING_BLOCKS;
	public static final ResourceKey<CreativeModeTab> DECORATIONS = CreativeModeTabs.FUNCTIONAL_BLOCKS;
	public static final ResourceKey<CreativeModeTab> REDSTONE    = CreativeModeTabs.REDSTONE_BLOCKS;
	public static final ResourceKey<CreativeModeTab> TRANSPORT   = CreativeModeTabs.TOOLS_AND_UTILITIES;
	public static final ResourceKey<CreativeModeTab> MISC        = CreativeModeTabs.INGREDIENTS;
	public static final ResourceKey<CreativeModeTab> COMBAT      = CreativeModeTabs.COMBAT;
	public static final ResourceKey<CreativeModeTab> FOOD        = CreativeModeTabs.FOOD_AND_DRINKS;

	private static final List<Object[]> ASSIGNMENTS = new ArrayList<>(); // {ItemLike (Block|Item), ResourceKey<CreativeModeTab>}

	/** F16 собственные GT-вкладки (1:1 golden): 1.7.10 создавал свои CreativeTabs — 7 god-item-вкладок (Technology,
	 *  Nature&Foods, Cans, Bottles, Books, Bumblebees, Equipment) + per-prefix (mPrefix.mCreativeTab) + per-MTE-registry
	 *  (mCreativeTabs). Их нельзя свести в ванильные — отдельные вкладки с иконкой+заголовком. Вкладка РАЗДЕЛЯЕМА: несколько
	 *  предметов присоединяются к ней (setCreativeTab). Карта вкладка→члены; displayItems перечисляет getSubItems всех членов.
	 *  Регистрируем каждую как настоящий neo CreativeModeTab (RegisterEvent&lt;CreativeModeTab&gt;). */
	static final java.util.LinkedHashMap<String, CreativeTab>    OWN_TABS        = new java.util.LinkedHashMap<>(); // имя → вкладка (для регистрации)
	static final java.util.LinkedHashMap<String, List<Item>>    OWN_TAB_MEMBERS = new java.util.LinkedHashMap<>(); // имя → члены (getSubItems)
	static final java.util.LinkedHashMap<String, CreativeTab[]>  OWN_TAB_REF     = new java.util.LinkedHashMap<>(); // имя → holder инстанса (заполняется после super())

	/** F16: строит neo Builder для собственной GT-вкладки. Ключ — имя (доступно ДО super(), в отличие от this): displayItems
	 *  захватывает СПИСОК членов + holder инстанса вкладки (не this — стена лямбды-в-super()). Инстанс кладётся в holder
	 *  registerOwnTab'ом (после super). Реальная вкладка нужна MTE-члену: его getSubItems фильтрует варианты по mCreativeTabID.
	 *  Авто-джойнит создателя aItem. Иконка — ЖИВАЯ через holder: шелл (F16-shell, aItem=null) после бинда реальной вкладки
	 *  показывает её иконку (holder перезаписан registerOwnTab), а не вечный камень. */
	static CreativeModeTab.Builder builderFor(String aName, String aLocal, Item aItem, int aMeta) {
		List<Item> tMembers = OWN_TAB_MEMBERS.computeIfAbsent(aName, k -> new ArrayList<>());
		CreativeTab[] tRef = OWN_TAB_REF.computeIfAbsent(aName, k -> new CreativeTab[1]);
		if (aItem != null && !tMembers.contains(aItem)) tMembers.add(aItem);
		return CreativeModeTab.builder()
			.title(net.minecraft.network.chat.Component.literal(aLocal))
			.icon(() -> {
				CreativeTab tLive = tRef[0];
				Item tIcon = (tLive != null && tLive.mItem != null) ? tLive.mItem : aItem;
				int tIconMeta = (tLive != null && tLive.mItem != null) ? (tLive.mMetaData & 0xFFFF) : (aMeta & 0xFFFF);
				return tIcon == null ? new ItemStack(Items.STONE) : gregapi.util.ST.make(tIcon, 1, tIconMeta);
			})
			.displayItems((aParams, aOutput) -> populate(tMembers, tRef[0], aOutput));
	}

	/** F16-shell (тайминг vs замороженный реестр): вкладки MTE-машин порождаются генератором на server-start
	 *  (Loader_MultiTileEntities в deferItemInit — F12), а реестр CreativeModeTab замораживается на буте → напрямую они
	 *  в реестр не успевают. Адаптация БЕЗ новой сущности — существующий центр конфигов GT6 (ConfigsGT.GREGTECH):
	 *  на server-start {@link #writeShellCache} пишет СВОЙ список вкладок (кэш собственного генератора, не копия оригинала);
	 *  на буте {@link #createShellsFromCache} поднимает вкладки-шеллы из кэша ДО регистрации реестра. Реальная вкладка,
	 *  создаваясь на server-start, биндится к шеллу существующим механизмом (registerOwnTab перезаписывает holder,
	 *  члены доливаются в общий список) — содержимое/иконка/фильтр оживают без перерегистрации. Первый в жизни запуск
	 *  (кэша ещё нет) показывает вкладки со второго запуска; кэш самообновляется каждый server-start. */
	private static final String SHELL_CATEGORY = "creativetabshells";

	/** Читает кэш и создаёт шеллы (ctor CreativeTab сам кладёт их в OWN_TABS/REF). Зовётся в начале onRegisterTabs. */
	private static void createShellsFromCache() {
		try {
			gregapi.config.Config tCfg = gregapi.data.CS.ConfigsGT.GREGTECH;
			if (tCfg == null) return;
			int tShells = 0;
			for (java.util.Map.Entry<String, gregapi.config.ConfigValue> tE : tCfg.mConfig.getCategory(SHELL_CATEGORY).entrySet()) try {
				String tName = tE.getKey();
				if (OWN_TABS.containsKey(tName)) continue;
				String tVal = tE.getValue().getString();
				int tSplit = tVal.indexOf('|');
				if (tSplit < 0) continue;
				short tMeta = Short.parseShort(tVal.substring(0, tSplit));
				String tLocal = tVal.substring(tSplit + 1);
				new CreativeTab(tName, tLocal, null, tMeta); // ctor → registerOwnTab → попадёт в регистрацию ниже
				++tShells;
			} catch (Throwable e) {/* битая запись кэша не рушит остальные */}
			gregapi.data.CS.OUT.println("[GT6-F16] шеллов вкладок из кэша: " + tShells + " (итого к регистрации: " + OWN_TABS.size() + ")");
		} catch (Throwable e) {/* нет конфига — нет шеллов (первый запуск) */}
	}

	/** Пишет ПОЛНЫЙ текущий набор собственных вкладок в кэш (server-start, после deferred-init — генератор уже отработал). */
	public static void writeShellCache() {
		try {
			gregapi.config.Config tCfg = gregapi.data.CS.ConfigsGT.GREGTECH;
			if (tCfg == null) return;
			int tNew = 0;
			for (java.util.Map.Entry<String, CreativeTab> tE : OWN_TABS.entrySet()) {
				CreativeTab tTab = tE.getValue();
				String tLocal = gregapi.data.LH.get("itemGroup." + tE.getKey(), tE.getKey());
				java.util.Map<String, gregapi.config.ConfigValue> tCat = tCfg.mConfig.getCategory(SHELL_CATEGORY);
				if (!tCat.containsKey(tE.getKey())) tNew++;
				tCfg.mConfig.get(SHELL_CATEGORY, tE.getKey(), tTab.mMetaData + "|" + tLocal);
			}
			tCfg.mConfig.save();
			gregapi.data.CS.OUT.println("[GT6-F16] кэш creative-вкладок: всего=" + OWN_TABS.size() + " новых=" + tNew
				+ (tNew > 0 ? " (новые вкладки видимы со СЛЕДУЮЩЕГО запуска — реестр neo заморожен на буте)" : ""));
		} catch (Throwable e) { gregapi.data.CS.OUT.println("[GT6-F16] запись кэша вкладок упала: " + e); }
	}

	/** ПРОБА (диагностика «вкладки пусты», gated flag в вызывателе): по каждой собственной вкладке — члены/варианты
	 *  (тот же enumerate, что кормит displayItems) и ФАКТИЧЕСКИ построенное движком содержимое (getDisplayItems). */
	public static void probeOwnTabs() {
		int tEmpty = 0, tTotal = 0, tShown = 0;
		for (java.util.Map.Entry<String, CreativeTab> tE : OWN_TABS.entrySet()) {
			tTotal++;
			List<Item> tMembers = OWN_TAB_MEMBERS.get(tE.getKey());
			CreativeTab[] tRefH = OWN_TAB_REF.get(tE.getKey());
			CreativeTab tTab = tRefH == null ? null : tRefH[0];
			int tEnum = 0;
			if (tMembers != null) for (Item tItem : tMembers) try { tEnum += enumerate(tItem, tItem, tTab).size(); } catch (Throwable e) {}
			int tBuilt = -1;
			try { tBuilt = tE.getValue().getDisplayItems().size(); } catch (Throwable e) {}
			if (tBuilt <= 0) tEmpty++;
			boolean tIsMTE = tE.getKey().startsWith("gt.multitileentity");
			if (tIsMTE ? tShown < 8 : false) { // фокус диагностики: машинные вкладки + АНАТОМИЯ члена (ключ/класс/реестр)
				tShown++;
				StringBuilder tSB = new StringBuilder("[GT6-F16-PROBE] ").append(tE.getKey())
					.append(" members=").append(tMembers == null ? 0 : tMembers.size()).append(" enum=").append(tEnum).append(" built=").append(tBuilt);
				if (tMembers != null && !tMembers.isEmpty()) {
					Item tM = tMembers.get(0);
					tSB.append(" member0=").append(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(tM)).append(" cls=").append(tM.getClass().getSimpleName());
					if (tM instanceof gregapi.block.multitileentity.MultiTileEntityItemInternal tMI)
						try { tSB.append(" regs=").append(tMI.mBlock.mMultiTileEntityRegistry.mRegistrations.size()); } catch (Throwable e) { tSB.append(" regs=EXC"); }
				}
				gregapi.data.CS.OUT.println(tSB.toString());
				// ВСКРЫТИЕ (первая машинная вкладка): ручной повтор цикла getSubItems БЕЗ глотания исключений —
				// счётчик совпадений фильтра + ПЕРВОЕ исключение getItem (invokeSub глотает Throwable → пусто молча).
				if (tShown == 1 && tMembers != null && !tMembers.isEmpty() && tMembers.get(0) instanceof gregapi.block.multitileentity.MultiTileEntityItemInternal tMI2 && tTab != null) {
					int tMatch = 0, tAdded = 0; Throwable tFirst = null; String tFirstAt = "";
					for (gregapi.block.multitileentity.MultiTileEntityClassContainer tC : tMI2.mBlock.mMultiTileEntityRegistry.mRegistrations) {
						if (tC.mHidden) continue;
						if (tTab.mMetaData != tC.mCreativeTabID) continue;
						tMatch++;
						try { net.minecraft.world.item.ItemStack tS = tMI2.mBlock.mMultiTileEntityRegistry.getItem(tC.mID); if (tS != null && !tS.isEmpty()) tAdded++; }
						catch (Throwable e) { if (tFirst == null) { tFirst = e; tFirstAt = "getItem(" + tC.mID + ")"; } }
					}
					gregapi.data.CS.OUT.println("[GT6-F16-PROBE-DEEP] tab=" + tE.getKey() + " tabMeta=" + tTab.mMetaData + " filterMatch=" + tMatch + " addedOK=" + tAdded
						+ (tFirst != null ? (" FIRST-EXC@" + tFirstAt + ": " + tFirst) : ""));
					if (tFirst != null) tFirst.printStackTrace(gregapi.data.CS.ERR);
				}
			}
		}
		gregapi.data.CS.OUT.println("[GT6-F16-PROBE] вкладок=" + tTotal + " пустых(built<=0)=" + tEmpty);
		// N6-судья (инструменты в креативе): явный замер собственной вкладки инструментов (gt.metatool.*) — built = реальное
		// содержимое, построенное движком через displayItems-генератор. built>0 = инструменты подключены к креатив-каналу.
		CreativeTab tToolTab = null;
		for (java.util.Map.Entry<String, CreativeTab> tE : OWN_TABS.entrySet()) if (tE.getKey().contains("metatool")) { tToolTab = tE.getValue(); break; }
		if (tToolTab != null) {
			// enum = реальные варианты, которые displayItems-генератор выдаст в креатив (getDisplayItems-кэш в probe-момент
			// ещё пуст — строится при первом открытии креатива, потому built=0 у всех — артефакт замера). enum>0 = подключено.
			List<Item> tMem = OWN_TAB_MEMBERS.get(tToolTab.mName);
			int tEnum = 0;
			if (tMem != null) for (Item tItem : tMem) try { tEnum += enumerate(tItem, tItem, tToolTab).size(); } catch (Throwable e) {}
			gregapi.data.CS.OUT.println("[GT6-TOOL-PROBE] вкладка инструментов='" + tToolTab.mName + "' members=" + (tMem == null ? 0 : tMem.size()) + " enum=" + tEnum + " (enum>0 = инструменты перечисляются в креатив)");
		} else {
			gregapi.data.CS.OUT.println("[GT6-TOOL-PROBE] вкладка инструментов НЕ найдена в OWN_TABS (fix не сработал)");
		}
	}

	/** Вызывается из ctor {@link CreativeTab}: запоминает инстанс вкладки под её именем (реестр + holder для displayItems). */
	static void registerOwnTab(CreativeTab aTab) {
		if (aTab == null || aTab.mName == null) return;
		OWN_TABS.put(aTab.mName, aTab);
		OWN_TAB_REF.computeIfAbsent(aTab.mName, k -> new CreativeTab[1])[0] = aTab;
	}

	/** Присоединить предмет к собственной GT-вкладке (замена setCreativeTab(tab)). Дедуп: повторное присоединение игнорируется. */
	public static void joinOwnTab(Item aItem, CreativeModeTab aTab) {
		if (aItem == null || !(aTab instanceof CreativeTab tTab) || tTab.mName == null) return;
		List<Item> tMembers = OWN_TAB_MEMBERS.computeIfAbsent(tTab.mName, k -> new ArrayList<>());
		if (!tMembers.contains(aItem)) tMembers.add(aItem);
	}

	/** Вызывается из ctor блока/предмета (замена setCreativeTab). aOwner — сам блок или предмет (ItemLike). LAST-WINS: как
	 *  1.7.10 setCreativeTab (повторный вызов заменял вкладку) — subclass-ctor (позже super) переопределяет базовую. */
	public static void assign(ItemLike aOwner, ResourceKey<CreativeModeTab> aTab) {
		if (aOwner == null || aTab == null) return;
		for (Object[] tA : ASSIGNMENTS) if (tA[0] == aOwner) {tA[1] = aTab; return;}
		ASSIGNMENTS.add(new Object[]{aOwner, aTab});
	}

	/** Единая подписка на mod-bus (вызов из GT_API ctor). */
	public static void register(IEventBus aModBus) {
		aModBus.addListener(CreativeTabsGT::onBuildContents);
		aModBus.addListener(CreativeTabsGT::onRegisterTabs);
	}

	/** F16: регистрируем 7 собственных GT-вкладок в реестр CREATIVE_MODE_TAB. К моменту этого события (после ITEM) OWN_TABS
	 *  заполнен ctor'ами god-items. Каждая CreativeTab — валидный neo CreativeModeTab (super(builder) с icon+displayItems). */
	private static void onRegisterTabs(net.neoforged.neoforge.registries.RegisterEvent aEvent) {
		if (!aEvent.getRegistryKey().equals(net.minecraft.core.registries.Registries.CREATIVE_MODE_TAB)) return;
		createShellsFromCache(); // F16-shell: вкладки server-start-генератора (MTE) поднимаются из кэша ДО заморозки реестра
		for (java.util.Map.Entry<String, CreativeTab> tE : OWN_TABS.entrySet()) try {
			final CreativeTab tTab = tE.getValue();
			aEvent.register(net.minecraft.core.registries.Registries.CREATIVE_MODE_TAB,
				net.minecraft.resources.Identifier.fromNamespaceAndPath(gregapi.data.CS.ModIDs.GT, gregapi.GT_API.sanitizeRegName(tE.getKey())), () -> tTab);
		} catch (Throwable e) {/* boot-safe: сбой одной вкладки не рушит загрузку */}
	}

	/** F16: наполнение собственной GT-вкладки (client-only, из displayItems-генератора). Перечисляет getSubItems ВСЕХ членов
	 *  вкладки (несколько предметов могут делить одну вкладку — prefix/MTE), что и делал 1.7.10 (vanilla звал getSubItems
	 *  каждого предмета вкладки). */
	static void populate(List<Item> aMembers, CreativeModeTab aTab, CreativeModeTab.Output aOutput) {
		if (aMembers == null) return;
		for (Item tItem : aMembers) try {
			// BUG-010: ST.hidden — центральная замена мёртвого NEI-канала ST.hide (скрытые слэб-варианты и пр.)
			for (ItemStack tStack : enumerate(tItem, tItem, aTab)) if (tStack != null && !tStack.isEmpty() && !gregapi.util.ST.hidden(tStack)) aOutput.accept(tStack);
		} catch (Throwable e) {/* boot-safe */}
	}

	private static void onBuildContents(BuildCreativeModeTabContentsEvent aEvent) {
		for (Object[] tA : ASSIGNMENTS) {
			if (!tA[1].equals(aEvent.getTabKey())) continue;
			try {
				ItemLike tOwner = (ItemLike)tA[0];
				Item tItem = tOwner.asItem();
				if (tItem == null || tItem == Items.AIR) continue;
				for (ItemStack tStack : enumerate(tOwner, tItem, null)) if (tStack != null && !tStack.isEmpty() && !gregapi.util.ST.hidden(tStack)) aEvent.accept(tStack); // BUG-010: фильтр скрытых (ST.hide)
			} catch (Throwable e) {/* boot-safe: сбой одного назначения не рушит загрузку вкладок */}
		}
	}

	/** Варианты для вкладки: getSubItems(Item,CreativeModeTab,List) у предмета либо getSubBlocks(...) у блока (порт сохранил
	 *  эти методы); при отсутствии/пустоте — базовый стек. Рефлексия — потому что общего интерфейса нет (россыпь классов). */
	@SuppressWarnings({"rawtypes", "unchecked"})
	public static List<ItemStack> enumerate(ItemLike aOwner, Item aItem, CreativeModeTab aTab) {
		List<ItemStack> tList = new ArrayList<>();
		// MTE-предмет — ПРЯМОЙ вызов, не рефлексия: его класс несёт compat-интерфейсы (applecore/IC2), вырезанные из
		// рантайма (stripRunMirror) → Class.getMethod перечисляет методы класса → NoClassDefFoundError → invokeSub молча
		// отдавал пусто → ВСЕ вкладки машин пусты (вскрытие: ручной цикл filterMatch=121 addedOK=121 без исключений).
		// Прямой virtual-вызов посторонние интерфейсы не резолвит. Прецедент ловушки: PrefixItem.registerIcons(Object).
		if (aItem instanceof gregapi.block.multitileentity.MultiTileEntityItemInternal tMTE) {
			try { tMTE.getSubItems(aItem, aTab, tList); } catch (Throwable e) {/* boot-safe */}
			if (tList.isEmpty()) tList.add(new ItemStack(aItem));
			return tList;
		}
		// МАТЕРИАЛИЗАЦИЯ инструментов (порт-поверхность, НЕ 1:1-шов: у Грега sMetaTool в креативе отсутствовал — его
		// getSubItems-шаблоны БЕЗ материала (MT.NULL → качество 0, скорость 0) в 1.7.10 были display-only и в креатив
		// не попадали). Вкладка Tools добавлена по запросу игрока (N6) → её предметы обязаны РАБОТАТЬ: каждому шаблону
		// даём материал Steel/Steel полноценным getToolWithStats-NBT — та же фабрика, что у крафта (репорт игрока:
		// «креативный ключ не ломает ничего» — шаблон без материала давал нулевую скорость копания).
		if (aItem instanceof gregapi.item.multiitem.MultiItemTool tTool) {
			List<ItemStack> tTemplates = new ArrayList<>();
			try { tTool.getSubItems(aItem, aTab, tTemplates); } catch (Throwable e) {/* boot-safe */}
			for (ItemStack tTemplate : tTemplates) {
				ItemStack tReal = null;
				try { tReal = tTool.getToolWithStats(gregapi.util.ST.meta_(tTemplate), 1, gregapi.data.MT.Steel, gregapi.data.MT.Steel); } catch (Throwable e) {/* boot-safe */}
				tList.add(tReal != null && !tReal.isEmpty() ? tReal : tTemplate);
			}
			if (tList.isEmpty()) tList.add(new ItemStack(aItem));
			return tList;
		}
		invokeSub(aItem, "getSubItems", aItem, aTab, tList);
		if (tList.isEmpty() && aOwner instanceof net.minecraft.world.level.block.Block tBlock) invokeSub(tBlock, "getSubBlocks", aItem, aTab, tList);
		if (tList.isEmpty()) tList.add(new ItemStack(aItem));
		return tList;
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	private static void invokeSub(Object aTarget, String aMethod, Item aItem, CreativeModeTab aTab, List aList) {
		try {
			java.lang.reflect.Method m = aTarget.getClass().getMethod(aMethod, Item.class, CreativeModeTab.class, List.class);
			m.invoke(aTarget, aItem, aTab, aList); // передаём реальную вкладку: MTE getSubItems фильтрует варианты по mCreativeTabID
		} catch (Throwable ignored) {/* метод отсутствует/сигнатура иная — базовый стек */}
	}
}

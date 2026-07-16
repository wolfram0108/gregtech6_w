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
	 *  Авто-джойнит создателя aItem. */
	static CreativeModeTab.Builder builderFor(String aName, String aLocal, Item aItem, int aMeta) {
		List<Item> tMembers = OWN_TAB_MEMBERS.computeIfAbsent(aName, k -> new ArrayList<>());
		CreativeTab[] tRef = OWN_TAB_REF.computeIfAbsent(aName, k -> new CreativeTab[1]);
		if (aItem != null && !tMembers.contains(aItem)) tMembers.add(aItem);
		return CreativeModeTab.builder()
			.title(net.minecraft.network.chat.Component.literal(aLocal))
			.icon(() -> aItem == null ? new ItemStack(Items.STONE) : gregapi.util.ST.make(aItem, 1, aMeta & 0xFFFF))
			.displayItems((aParams, aOutput) -> populate(tMembers, tRef[0], aOutput));
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

	/** Вызывается из ctor блока/предмета (замена setCreativeTab). aOwner — сам блок или предмет (ItemLike). */
	public static void assign(ItemLike aOwner, ResourceKey<CreativeModeTab> aTab) {
		if (aOwner != null && aTab != null) ASSIGNMENTS.add(new Object[]{aOwner, aTab});
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
			for (ItemStack tStack : enumerate(tItem, tItem, aTab)) if (tStack != null && !tStack.isEmpty()) aOutput.accept(tStack);
		} catch (Throwable e) {/* boot-safe */}
	}

	private static void onBuildContents(BuildCreativeModeTabContentsEvent aEvent) {
		for (Object[] tA : ASSIGNMENTS) {
			if (!tA[1].equals(aEvent.getTabKey())) continue;
			try {
				ItemLike tOwner = (ItemLike)tA[0];
				Item tItem = tOwner.asItem();
				if (tItem == null || tItem == Items.AIR) continue;
				for (ItemStack tStack : enumerate(tOwner, tItem, null)) if (tStack != null && !tStack.isEmpty()) aEvent.accept(tStack);
			} catch (Throwable e) {/* boot-safe: сбой одного назначения не рушит загрузку вкладок */}
		}
	}

	/** Варианты для вкладки: getSubItems(Item,CreativeModeTab,List) у предмета либо getSubBlocks(...) у блока (порт сохранил
	 *  эти методы); при отсутствии/пустоте — базовый стек. Рефлексия — потому что общего интерфейса нет (россыпь классов). */
	@SuppressWarnings({"rawtypes", "unchecked"})
	private static List<ItemStack> enumerate(ItemLike aOwner, Item aItem, CreativeModeTab aTab) {
		List<ItemStack> tList = new ArrayList<>();
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

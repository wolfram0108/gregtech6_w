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

	/** F16 собственные GT-вкладки (1:1 golden): 1.7.10 создавал 7 своих CreativeTabs (по одной на god-item: Technology,
	 *  Nature&Foods, Cans, Bottles, Books, Bumblebees, Equipment). Их нельзя свести в ванильные — они отдельные вкладки с
	 *  иконкой+заголовком. Здесь регистрируем каждую как настоящий neo CreativeModeTab (RegisterEvent&lt;CreativeModeTab&gt;). */
	static final List<CreativeTab> OWN_TABS = new ArrayList<>();

	/** Вызывается из ctor {@link CreativeTab} (создаётся в ctor god-item — замена setCreativeTab(new CreativeTab(...))). */
	static void registerOwnTab(CreativeTab aTab) {if (aTab != null) OWN_TABS.add(aTab);}

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
		for (CreativeTab tTab : OWN_TABS) try {
			aEvent.register(net.minecraft.core.registries.Registries.CREATIVE_MODE_TAB,
				net.minecraft.resources.Identifier.fromNamespaceAndPath(gregapi.data.CS.ModIDs.GT, gregapi.GT_API.sanitizeRegName(tTab.mName)), () -> tTab);
		} catch (Throwable e) {/* boot-safe: сбой одной вкладки не рушит загрузку */}
	}

	/** F16: наполнение собственной GT-вкладки (client-only, вызывается из displayItems-генератора вкладки). Перечисление —
	 *  тем же getSubItems god-item, что и раньше (порт сохранил). */
	static void populate(Item aItem, CreativeModeTab.Output aOutput) {
		if (aItem == null) return;
		try {for (ItemStack tStack : enumerate(aItem, aItem)) if (tStack != null && !tStack.isEmpty()) aOutput.accept(tStack);}
		catch (Throwable e) {/* boot-safe */}
	}

	private static void onBuildContents(BuildCreativeModeTabContentsEvent aEvent) {
		for (Object[] tA : ASSIGNMENTS) {
			if (!tA[1].equals(aEvent.getTabKey())) continue;
			try {
				ItemLike tOwner = (ItemLike)tA[0];
				Item tItem = tOwner.asItem();
				if (tItem == null || tItem == Items.AIR) continue;
				for (ItemStack tStack : enumerate(tOwner, tItem)) if (tStack != null && !tStack.isEmpty()) aEvent.accept(tStack);
			} catch (Throwable e) {/* boot-safe: сбой одного назначения не рушит загрузку вкладок */}
		}
	}

	/** Варианты для вкладки: getSubItems(Item,CreativeModeTab,List) у предмета либо getSubBlocks(...) у блока (порт сохранил
	 *  эти методы); при отсутствии/пустоте — базовый стек. Рефлексия — потому что общего интерфейса нет (россыпь классов). */
	@SuppressWarnings({"rawtypes", "unchecked"})
	private static List<ItemStack> enumerate(ItemLike aOwner, Item aItem) {
		List<ItemStack> tList = new ArrayList<>();
		invokeSub(aItem, "getSubItems", aItem, tList);
		if (tList.isEmpty() && aOwner instanceof net.minecraft.world.level.block.Block tBlock) invokeSub(tBlock, "getSubBlocks", aItem, tList);
		if (tList.isEmpty()) tList.add(new ItemStack(aItem));
		return tList;
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	private static void invokeSub(Object aTarget, String aMethod, Item aItem, List aList) {
		try {
			java.lang.reflect.Method m = aTarget.getClass().getMethod(aMethod, Item.class, CreativeModeTab.class, List.class);
			m.invoke(aTarget, aItem, null, aList);
		} catch (Throwable ignored) {/* метод отсутствует/сигнатура иная — базовый стек */}
	}
}

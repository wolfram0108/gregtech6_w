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
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;

/** 1.7.10's per-item setCreativeTab is gone; neo fills tabs through an event instead, so this is the one
 *  central place items register their tab, reusing getSubItems/getSubBlocks so one failure can't break boot. */
public final class CreativeTabsGT {
	private CreativeTabsGT() {}

	// Mapping from 1.7.10 vanilla tabs to their neo ResourceKey equivalents.
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

	/** GT6's own seven creative tabs cannot be folded into vanilla tabs since they need their own icon and
	 *  title; a tab is shared by joining members into it, and its contents enumerate every member's getSubItems. */
	static final java.util.LinkedHashMap<String, CreativeTab>    OWN_TABS        = new java.util.LinkedHashMap<>(); // Keyed by tab name; this map is read back when registering each CreativeTab.
	static final java.util.LinkedHashMap<String, List<Item>>    OWN_TAB_MEMBERS = new java.util.LinkedHashMap<>(); // Keyed by tab name; members are enumerated later via getSubItems.
	static final java.util.LinkedHashMap<String, CreativeTab[]>  OWN_TAB_REF     = new java.util.LinkedHashMap<>(); // Keyed by tab name; the holder is filled in only after super() runs.

	/** Builds the neo Builder for an own GT tab, keyed by name since it is available before super(), unlike
	 *  this; the icon stays live through the holder so a shell later shows the real tab's icon once bound. */
	static CreativeModeTab.Builder builderFor(String aName, String aLocal, Item aItem, int aMeta) {
		List<Item> tMembers = OWN_TAB_MEMBERS.computeIfAbsent(aName, k -> new ArrayList<>());
		CreativeTab[] tRef = OWN_TAB_REF.computeIfAbsent(aName, k -> new CreativeTab[1]);
		if (aItem != null && !tMembers.contains(aItem)) tMembers.add(aItem);
		return CreativeModeTab.builder()
			// Registering the instance directly bypasses Builder.build(), the only place Forge sets its background default.
			// Without it the raw field stays null and crashes rendering on tab select, so the default is set explicitly here.
			.withBackgroundLocation(new net.minecraft.resources.ResourceLocation("textures/gui/container/creative_inventory/tab_items.png"))
			.title(net.minecraft.network.chat.Component.literal(aLocal))
			.icon(() -> {
				CreativeTab tLive = tRef[0];
				Item tIcon = (tLive != null && tLive.mItem != null) ? tLive.mItem : aItem;
				int tIconMeta = (tLive != null && tLive.mItem != null) ? (tLive.mMetaData & 0xFFFF) : (aMeta & 0xFFFF);
				return tIcon == null ? new ItemStack(Items.STONE) : gregapi.util.ST.make(tIcon, 1, tIconMeta);
			})
			.displayItems((aParams, aOutput) -> populate(tMembers, tRef[0], aOutput));
	}

	/** Machine tabs come from a generator that only runs at server start, after the tab registry has frozen,
	 *  so a cache written at server start lets shell tabs exist at boot and bind once the real tab exists. */
	private static final String SHELL_CATEGORY = "creativetabshells";

	/** Reads the cache and creates shell tabs; called at the start of onRegisterTabs. */
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
				new CreativeTab(tName, tLocal, null, tMeta); // Constructing a CreativeTab here calls registerOwnTab automatically, feeding the registration below.
				++tShells;
			} catch (Throwable e) {/* A corrupt cache entry does not break the others. */}
		} catch (Throwable e) {/* No config yet means no shells on the very first launch. */}
	}

	/** Writes the full current set of own tabs to the cache at server start, after deferred init has run. */
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
		} catch (Throwable e) {/* An unwritable cache is regenerated by the generator on the next launch. */}
	}

	/** Called from CreativeTab's constructor: remembers the instance under its name for registration and lookup. */
	static void registerOwnTab(CreativeTab aTab) {
		if (aTab == null || aTab.mName == null) return;
		OWN_TABS.put(aTab.mName, aTab);
		OWN_TAB_REF.computeIfAbsent(aTab.mName, k -> new CreativeTab[1])[0] = aTab;
	}

	/** Joins an item to an own GT tab, replacing setCreativeTab(tab); a repeat join is ignored. */
	public static void joinOwnTab(Item aItem, CreativeModeTab aTab) {
		if (aItem == null || !(aTab instanceof CreativeTab tTab) || tTab.mName == null) return;
		List<Item> tMembers = OWN_TAB_MEMBERS.computeIfAbsent(tTab.mName, k -> new ArrayList<>());
		if (!tMembers.contains(aItem)) tMembers.add(aItem);
	}

	/** Replaces setCreativeTab: last write wins, matching how the original setCreativeTab let a later
	 *  subclass constructor override an earlier assignment from its own super call. */
	public static void assign(ItemLike aOwner, ResourceKey<CreativeModeTab> aTab) {
		if (aOwner == null || aTab == null) return;
		for (Object[] tA : ASSIGNMENTS) if (tA[0] == aOwner) {tA[1] = aTab; return;}
		ASSIGNMENTS.add(new Object[]{aOwner, aTab});
	}

	/** Single mod-bus subscription point, called from the GT_API constructor. */
	public static void register(IEventBus aModBus) {
		aModBus.addListener(CreativeTabsGT::onBuildContents);
		aModBus.addListener(CreativeTabsGT::onRegisterTabs);
	}

	/** Registers the seven own GT tabs; by the time this event fires, every god-item constructor has already
	 *  populated OWN_TABS, and each entry is a valid neo CreativeModeTab. */
	private static void onRegisterTabs(net.minecraftforge.registries.RegisterEvent aEvent) {
		if (!aEvent.getRegistryKey().equals(net.minecraft.core.registries.Registries.CREATIVE_MODE_TAB)) return;
		createShellsFromCache(); // Server-start-generated machine tabs are raised from the cache before the registry freezes.
		for (java.util.Map.Entry<String, CreativeTab> tE : OWN_TABS.entrySet()) try {
			final CreativeTab tTab = tE.getValue();
			// Skips registering a tab whose members are all hidden or empty, since JEI otherwise flags it as having
			// no display items; both known empty tabs match the original exactly, not a defect introduced by the port.
			if (isTabEmpty(tE.getKey())) continue;
			aEvent.register(net.minecraft.core.registries.Registries.CREATIVE_MODE_TAB,
				new net.minecraft.resources.ResourceLocation(gregapi.data.CS.ModIDs.GT, gregapi.GT_API.sanitizeRegName(tE.getKey())), () -> tTab);
		} catch (Throwable e) {/* boot-safe: one tab failing does not break loading. */}
	}

	/** A tab counts as empty only if it has members but none produce a visible variant; an empty member list
	 *  means a machine shell still to be filled at server start, so that case is treated as not empty. */
	private static boolean isTabEmpty(String aName) {
		// Deferred item-init may still be queueing addItems calls; an empty getSubItems here means 'not yet', not 'no variants',
		// and self-hides the item when empty, so the tab is treated as 'fills in later' until that init has actually drained.
		if (!gregapi.GT_API.sDeferredItemInitDone) return false;
		List<Item> tMembers = OWN_TAB_MEMBERS.get(aName);
		if (tMembers == null || tMembers.isEmpty()) return false; // An empty shell with no members yet will be filled later, so it is not hidden here.
		for (Item tItem : tMembers) try {
			for (ItemStack tStack : enumerate(tItem, tItem, null))
				if (tStack != null && !tStack.isEmpty() && !gregapi.util.ST.hidden(tStack)) return false;
		} catch (Throwable e) {return false;/* An enumeration failure counts as not-empty so it is not hidden by mistake. */}
		return true;
	}

	/** Populates an own GT tab client-side by enumerating getSubItems across every joined member, the same
	 *  way vanilla called getSubItems on each tab's item in 1.7.10. */
	static void populate(List<Item> aMembers, CreativeModeTab aTab, CreativeModeTab.Output aOutput) {
		if (aMembers == null) return;
		// The vanilla tab-content setter throws on a duplicate entry and aborted the entire population, while
		// the old NEI-based channel tolerated duplicates; deduplicated here by the tab's own identity instead.
		java.util.Set<ItemStack> tSeen = net.minecraft.world.item.ItemStackLinkedSet.createTypeAndTagSet();
		for (Item tItem : aMembers) try {
			// ST.hidden replaces the dead NEI-only ST.hide channel for hiding variants such as slab halves.
			for (ItemStack tStack : enumerate(tItem, tItem, aTab)) if (tStack != null && !tStack.isEmpty() && !gregapi.util.ST.hidden(tStack) && tSeen.add(tStack)) aOutput.accept(tStack);
		} catch (Throwable e) {/* boot-safe */}
	}

	private static void onBuildContents(BuildCreativeModeTabContentsEvent aEvent) {
		// Same duplicate-identity dedup as populate(): one duplicate assignment used to drop the whole tab's contents.
		java.util.Set<ItemStack> tSeen = net.minecraft.world.item.ItemStackLinkedSet.createTypeAndTagSet();
		for (Object[] tA : ASSIGNMENTS) {
			if (!tA[1].equals(aEvent.getTabKey())) continue;
			try {
				ItemLike tOwner = (ItemLike)tA[0];
				Item tItem = tOwner.asItem();
				if (tItem == null || tItem == Items.AIR) continue;
				for (ItemStack tStack : enumerate(tOwner, tItem, null)) if (tStack != null && !tStack.isEmpty() && !gregapi.util.ST.hidden(tStack) && tSeen.add(tStack)) aEvent.accept(tStack); // Filters hidden variants (ST.hidden) the same way populate() does, replacing the old NEI ST.hide channel.
			} catch (Throwable e) {/* boot-safe: one assignment failing does not break tab loading. */}
		}
	}

	/** Falls back to getSubItems on the item or getSubBlocks on the block, whichever the port kept; reflection
	 *  is used because there is no shared interface across this mixed set of classes. */
	@SuppressWarnings({"rawtypes", "unchecked"})
	public static List<ItemStack> enumerate(ItemLike aOwner, Item aItem, CreativeModeTab aTab) {
		List<ItemStack> tList = new ArrayList<>();
		// A direct virtual call, not reflection: this class carries compat interfaces stripped from the runtime
		// jar, and Class.getMethod enumerating them threw a missing-class error that silently emptied every tab.
		if (aItem instanceof gregapi.block.multitileentity.MultiTileEntityItemInternal tMTE) {
			try { tMTE.getSubItems(aItem, aTab, tList); } catch (Throwable e) {/* boot-safe */}
			if (tList.isEmpty()) tList.add(new ItemStack(aItem));
			return tList;
		}
		// Tool templates with no material were display-only in the original and never reached the creative
		// inventory; since this tab is new, each template gets a working steel stack instead of a non-functional one.
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
		// The same stripped-interface trap as the MTE case above: getMethod on this legacy interface throws and
		// silently collapsed hundreds of fluid-display variants down to one base stack.
		if (aItem instanceof gregapi.item.ItemFluidDisplay tFluidDisplay) {
			try { tFluidDisplay.getSubItems(aItem, aTab, tList); } catch (Throwable e) {/* boot-safe */}
			if (tList.isEmpty()) tList.add(new ItemStack(aItem));
			return tList;
		}
		invokeSub(aItem, "getSubItems", aItem, aTab, tList);
		// Vanilla enumerated getSubBlocks on every block in a tab; the block is recovered from the owner or
		// through BlockItem, since an own-tab member is stored as an Item and may only expose "blockness" that way.
		if (tList.isEmpty()) {
			net.minecraft.world.level.block.Block tBlock =
				(aOwner instanceof net.minecraft.world.level.block.Block b) ? b :
				(aItem instanceof net.minecraft.world.item.BlockItem bi) ? bi.getBlock() : null;
			if (tBlock != null) invokeSub(tBlock, "getSubBlocks", aItem, aTab, tList);
		}
		if (tList.isEmpty()) tList.add(new ItemStack(aItem));
		return tList;
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	private static void invokeSub(Object aTarget, String aMethod, Item aItem, CreativeModeTab aTab, List aList) {
		try {
			java.lang.reflect.Method m = aTarget.getClass().getMethod(aMethod, Item.class, CreativeModeTab.class, List.class);
			m.invoke(aTarget, aItem, aTab, aList); // Passes the real tab through: MTE's getSubItems filters variants by mCreativeTabID.
		} catch (Throwable ignored) {/* Method missing or signature differs; falls back to the base stack. */}
	}
}

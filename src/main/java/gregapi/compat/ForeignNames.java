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
import java.util.function.Consumer;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import gregapi.code.ModData;
import gregapi.util.ST;

import static gregapi.data.CS.*;

/** The one place a foreign mod's 1.7.10 item name (+ meta) becomes the id that mod registers today; the ST.make
 *  funnel asks only here. Each mod keeps its table in its own class (AE2Names, BuildCraftNames); a path may name its namespace. */
public final class ForeignNames {
	private final String[] mModIDs;
	private final Map<String, String> mMapped = new LinkedHashMap<>();
	private final Map<String, String> mGone = new LinkedHashMap<>();
	/** Names whose 1.7.10 meta was a SUBTYPE: looked up by «name#meta», and the found item gets no meta stamped. */
	private final Set<String> mSplit = new LinkedHashSet<>();
	/** Subtypes a mod now keeps in a data component rather than in a separate item: the stamp writes it onto the stack. */
	private final Map<String, Consumer<ItemStack>> mStamps = new LinkedHashMap<>();

	/** A table answering for every given mod (ids compared, so sub-mods sharing one id share the table). */
	public ForeignNames(ModData... aMods) {
		mModIDs = new String[aMods.length];
		for (int i = 0; i < aMods.length; i++) mModIDs[i] = aMods[i].mID;
	}

	/** Every table the funnel knows. A new foreign mod joins by adding its table here. */
	private static ForeignNames[] all() {return new ForeignNames[] {AE2Names.TABLE, BuildCraftNames.TABLE};}
	// Built on first use: each table is a static field that itself constructs a ForeignNames, so an eager list
	// here would read those fields while their classes are still initialising.
	private static ForeignNames[] sAll = null;

	/** The table that knows this (mod, name) pair, or null — then the funnel keeps the plain registry lookup. */
	public static ForeignNames table(ModData aMod, String aName) {
		if (aMod == null || aName == null) return null;
		if (sAll == null) sAll = all();
		for (ForeignNames tTable : sAll) if (tTable.answersFor(aMod) && tTable.owns(aName)) return tTable;
		return null;
	}

	private boolean answersFor(ModData aMod) {
		for (String tID : mModIDs) if (tID.equals(aMod.mID)) return T;
		return F;
	}

	private static String key(String aName, long aMeta) {return aName + "#" + aMeta;}

	public ForeignNames map (String aName,             String aPath  ) {mMapped.put(aName, aPath); return this;}
	public ForeignNames map (String aName, long aMeta, String aPath  ) {mSplit.add(aName); mMapped.put(key(aName, aMeta), aPath); return this;}
	public ForeignNames map (String aName, long aMeta, String aPath, Consumer<ItemStack> aStamp) {map(aName, aMeta, aPath); mStamps.put(key(aName, aMeta), aStamp); return this;}
	public ForeignNames gone(String aName,             String aReason) {mGone.put(aName, aReason); return this;}
	public ForeignNames gone(String aName, long aMeta, String aReason) {mSplit.add(aName); mGone.put(key(aName, aMeta), aReason); return this;}

	/** Whether this table knows the name at all. */
	public boolean owns(String aName) {
		return aName != null && (mSplit.contains(aName) || mMapped.containsKey(aName) || mGone.containsKey(aName));
	}

	/** The current id path for a «1.7.10 name + meta» pair; {@code null} = no carrier, or not our name. */
	public String path(String aName, long aMeta) {
		if (aName == null) return null;
		return mSplit.contains(aName) ? mMapped.get(key(aName, aMeta)) : mMapped.get(aName);
	}

	/** Whether the pair has a carrier today. The caller uses this to decide whether to register its entry. */
	public boolean has(String aName, long aMeta) {return path(aName, aMeta) != null;}

	/** The REASON the carrier is missing; {@code null} = carrier exists, or not our name. */
	public String reason(String aName, long aMeta) {
		if (aName == null || path(aName, aMeta) != null) return null;
		if (!mSplit.contains(aName)) return mGone.get(aName);
		String rReason = mGone.get(key(aName, aMeta));
		return rReason != null ? rReason : "meta " + aMeta + " of «" + aName + "» is unknown to the centre (GT6 never asks for it)";
	}

	/** A foreign stack from its 1.7.10 name, through ST.findItem only; a split name's subtype is the item itself, so its
	 *  meta is not stamped (W, "any", survives), other names keep their meta as the plain path does. */
	public ItemStack make(ModData aMod, String aName, long aSize, long aMeta) {
		if (aMod == null || !aMod.mLoaded || !GAPI_POST.mStartedPreInit) return null;
		String tPath = path(aName, aMeta);
		if (tPath == null) return null;
		int tColon = tPath.indexOf(':');
		Item tItem = tColon < 0 ? ST.findItem(mModIDs[0], tPath) : ST.findItem(tPath.substring(0, tColon), tPath.substring(tColon + 1));
		if (tItem == null) return null;
		ItemStack rStack = ST.make_(tItem, aSize, mSplit.contains(aName) && aMeta != W ? 0 : aMeta);
		Consumer<ItemStack> tStamp = mStamps.get(key(aName, aMeta));
		if (tStamp != null) tStamp.accept(rStack);
		return rStack;
	}
}

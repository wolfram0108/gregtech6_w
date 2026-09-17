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

package gregapi.code;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

/** Central ItemStack<->NBT bridge, reverted to 1.7.10's own form: the raw tag lives on the stack itself
 *  again, but that tag now also emulates subtype and the zero-size marker, so every read/write here must filter them out. */
public final class ItemNBT {
	private ItemNBT() {/**/}

	/** Root-tag keys that 1.20.1 uses to emulate 1.7.10 stack fields -- not real "item NBT". Damage =
	 *  subtype/durability, gt.zerosize = the zero-count marker. */
	private static final String[] FIELDS = {ItemStack.TAG_DAMAGE, gregapi.data.CS.NBT_ZEROSIZE};

	/** A tag holding nothing but these emulated fields counts as "no tag", in 1.7.10 terms. */
	private static boolean fieldsOnly(CompoundTag aNBT) {
		if (aNBT == null || aNBT.isEmpty()) return true;
		int tFields = 0;
		for (String tKey : FIELDS) if (aNBT.contains(tKey)) tFields++;
		return aNBT.size() == tFields;
	}

	private static boolean hasField(CompoundTag aNBT) {
		if (aNBT == null) return false;
		for (String tKey : FIELDS) if (aNBT.contains(tKey)) return true;
		return false;
	}

	public static CompoundTag get(ItemStack aStack) {
		if (aStack == null || aStack.isEmpty()) return null;
		CompoundTag rNBT = aStack.getTag();
		if (fieldsOnly(rNBT)) return null;
		// No emulated fields in the tag: return the live tag itself, as 1.7.10 did (mutation persists on its own).
		if (!hasField(rNBT)) return rNBT;
		// Fields are present but don't belong in "item NBT": return a detached copy without them (the same
		// shape the 26.1.2 bridge already gave, so every commit call site downstream already matches it).
		rNBT = rNBT.copy();
		for (String tKey : FIELDS) rNBT.remove(tKey);
		return rNBT;
	}

	public static void set(ItemStack aStack, CompoundTag aNBT) {
		if (aStack == null || aStack.isEmpty()) return;
		CompoundTag tOld = aStack.getTag();
		CompoundTag rNBT = aNBT;
		// Emulated fields never travel with a foreign tag: only their own centers (ST.meta_/ST.size_) set them.
		if (rNBT != null && rNBT != tOld) for (String tKey : FIELDS) rNBT.remove(tKey);
		boolean tFields = hasField(tOld);
		// Nothing to store and no fields either: there's no tag at all (1.7.10 normalization: GT6 never keeps an empty tag).
		if ((rNBT == null || rNBT.isEmpty()) && !tFields) {aStack.setTag(null); return;}
		// Fields exist, so a tag must exist, and it has to be the object PASSED IN, not a fresh one: the
		// original set the tag and kept mutating that same object afterward, and that idiom has to keep working.
		if (rNBT == null) rNBT = new CompoundTag();
		if (tFields && rNBT != tOld) for (String tKey : FIELDS) {
			net.minecraft.nbt.Tag tField = tOld.get(tKey);
			if (tField != null) rNBT.put(tKey, tField);
		}
		aStack.setTag(rNBT);
	}

	public static boolean has(ItemStack aStack) {
		if (aStack == null || aStack.isEmpty()) return false;
		return !fieldsOnly(aStack.getTag());
	}

	/** Reads an emulated stack FIELD, bypassing the item-NBT filter above; the only caller is that field's own center. */
	public static boolean field(ItemStack aStack, String aKey) {
		if (aStack == null || aStack.isEmpty()) return false;
		CompoundTag tNBT = aStack.getTag();
		return tNBT != null && tNBT.getBoolean(aKey);
	}

	/** Writes an emulated stack field, leaving no empty tag behind once cleared. aFlag=false means "field
	 *  absent", which for a numeric field is its zero default -- the same branch ST.meta_ uses at meta 0. */
	public static void field(ItemStack aStack, String aKey, boolean aFlag) {
		if (aStack == null || aStack.isEmpty()) return;
		if (aFlag) {aStack.getOrCreateTag().putBoolean(aKey, true); return;}
		CompoundTag tNBT = aStack.getTag();
		if (tNBT == null || !tNBT.contains(aKey)) return;
		tNBT.remove(aKey);
		if (tNBT.isEmpty()) aStack.setTag(null);
	}
}

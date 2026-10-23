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

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;

import gregapi.GT_API;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.ByteArrayTag;
import net.minecraft.nbt.ByteTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NumericTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/** @author Gregorius Techneticies
 *  Central ItemStack<->NBT bridge: the stack-level tag lives in the GT6 component {@link GT_API#DATA} (the old mutable tag is now an
 *  immutable component value), so code that mutates a tag from {@link #get(ItemStack)} must write it back via {@link #set} or lose it.
 *  Stacks that still hold their data in the vanilla DataComponents.CUSTOM_DATA are read from there and moved on first {@link #get}. */
public final class ItemNBT {
	private ItemNBT() {/**/}

	/** Marks a string that must stay a string although it reads as another tag (or starts with the mark itself) in {@link #CODEC}'s text form. */
	private static final char TEXT_MARK = '\u001E';
	private static final TagParser<Tag> SNBT = TagParser.create(NbtOps.INSTANCE);

	/** Codec of {@link GT_API#DATA}. World saves (NBT) keep the tag as is. Every other format (JSON, FTB's Json5, ...) gets numbers and arrays as
	 *  SNBT strings: their readers narrow integers to Byte/Short and their packers reject those, so a tag must not depend on number types there. */
	public static final Codec<CustomData> CODEC = new Codec<CustomData>() {
		@Override public <T> DataResult<T> encode(CustomData aData, DynamicOps<T> aOps, T aPrefix) {
			CompoundTag tTag = aData.copyTag();
			return CompoundTag.CODEC.encode(isNBT(aOps) ? tTag : (CompoundTag)toText(tTag), aOps, aPrefix);
		}
		@Override public <T> DataResult<Pair<CustomData, T>> decode(DynamicOps<T> aOps, T aInput) {
			boolean tNBT = isNBT(aOps);
			return CompoundTag.CODEC.decode(aOps, aInput).map(aPair -> aPair.mapFirst(aTag -> CustomData.of(tNBT ? aTag : (CompoundTag)fromText(aTag))));
		}
	};

	private static boolean isNBT(DynamicOps<?> aOps) {
		Object tByte = aOps.createByte((byte)0);
		return tByte instanceof ByteTag;
	}

	private static Tag parseSNBT(String aText) {
		try {return SNBT.parseFully(aText);} catch (CommandSyntaxException | RuntimeException e) {return null;}
	}

	private static boolean marked(String aText) {
		return !aText.isEmpty() && aText.charAt(0) == TEXT_MARK;
	}

	private static Tag toText(Tag aTag) {
		if (aTag instanceof CompoundTag tCompound) {
			CompoundTag rTag = new CompoundTag();
			for (String tKey : tCompound.keySet()) rTag.put(tKey, toText(tCompound.get(tKey)));
			return rTag;
		}
		if (aTag instanceof ListTag tList) {
			ListTag rList = new ListTag(tList.size());
			for (Tag tElement : tList) rList.add(toText(tElement));
			return rList;
		}
		if (aTag instanceof StringTag tString) {
			String tValue = tString.value();
			if (marked(tValue)) return StringTag.valueOf(TEXT_MARK + tValue);
			Tag tParsed = parseSNBT(tValue);
			return tParsed == null || tParsed instanceof StringTag ? aTag : StringTag.valueOf(TEXT_MARK + tValue);
		}
		if (aTag instanceof NumericTag || aTag instanceof ByteArrayTag || aTag instanceof IntArrayTag || aTag instanceof LongArrayTag) return StringTag.valueOf(aTag.toString());
		return aTag;
	}

	private static Tag fromText(Tag aTag) {
		if (aTag instanceof CompoundTag tCompound) {
			CompoundTag rTag = new CompoundTag();
			for (String tKey : tCompound.keySet()) rTag.put(tKey, fromText(tCompound.get(tKey)));
			return rTag;
		}
		if (aTag instanceof ListTag tList) {
			ListTag rList = new ListTag(tList.size());
			for (Tag tElement : tList) rList.add(fromText(tElement));
			return rList;
		}
		if (aTag instanceof StringTag tString) {
			String tValue = tString.value();
			if (marked(tValue)) return StringTag.valueOf(tValue.substring(1));
			Tag tParsed = parseSNBT(tValue);
			return tParsed == null || tParsed instanceof StringTag ? aTag : tParsed;
		}
		return aTag;
	}

	public static CompoundTag get(ItemStack aStack) {
		if (aStack == null || aStack.isEmpty()) return null;
		CustomData tData = aStack.get(GT_API.DATA_TYPE);
		if (tData != null && !tData.isEmpty()) return tData.copyTag();
		CustomData tOld = aStack.get(DataComponents.CUSTOM_DATA);
		if (tOld == null || tOld.isEmpty()) return null;
		aStack.set(GT_API.DATA_TYPE, tOld);
		aStack.remove(DataComponents.CUSTOM_DATA);
		return tOld.copyTag();
	}

	public static void set(ItemStack aStack, CompoundTag aNBT) {
		if (aStack == null || aStack.isEmpty()) return;
		aStack.remove(DataComponents.CUSTOM_DATA);
		if (aNBT == null || aNBT.isEmpty()) {
			aStack.remove(GT_API.DATA_TYPE);
		} else {
			aStack.set(GT_API.DATA_TYPE, CustomData.of(aNBT));
		}
	}

	public static boolean has(ItemStack aStack) {
		if (aStack == null || aStack.isEmpty()) return false;
		CustomData tData = aStack.get(GT_API.DATA_TYPE);
		if (tData != null && !tData.isEmpty()) return true;
		CustomData tOld = aStack.get(DataComponents.CUSTOM_DATA);
		return tOld != null && !tOld.isEmpty();
	}
}

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

import java.util.ArrayList;
import java.util.List;

import gregapi.GT_API;
import gregapi.data.CS;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
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
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;

/** @author Gregorius Techneticies
 *  Central ItemStack<->NBT bridge: the stack-level tag lives in the GT6 component {@link GT_API#DATA} (the old mutable tag is now an
 *  immutable component value), so code that mutates a tag from {@link #get(ItemStack)} must write it back via {@link #set} or lose it.
 *  Stacks that still hold their data in the vanilla DataComponents.CUSTOM_DATA are read from there and moved on first {@link #get} ({@link #migrate} and
 *  its walkers do it ahead of time). Only GT6 items are moved or written there: the custom_data of any other item is read and written in place, never moved. */
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

	/** Whether the item is GT6 content (GT6 and its addons): only such items keep their data in {@link GT_API#DATA}; the custom_data of any other item is somebody else's. */
	private static boolean owned(ItemStack aStack) {
		var tKey = BuiltInRegistries.ITEM.getKey(aStack.getItem());
		return tKey != null && CS.ModIDs.isGregNamespace(tKey.getNamespace());
	}

	public static CompoundTag get(ItemStack aStack) {
		if (aStack == null || aStack.isEmpty()) return null;
		CustomData tData = aStack.get(GT_API.DATA_TYPE);
		if (tData != null && !tData.isEmpty()) return tData.copyTag();
		CustomData tOld = aStack.get(DataComponents.CUSTOM_DATA);
		if (tOld == null || tOld.isEmpty()) return null;
		if (owned(aStack)) {
			aStack.set(GT_API.DATA_TYPE, tOld);
			aStack.remove(DataComponents.CUSTOM_DATA);
		}
		return tOld.copyTag();
	}

	public static void set(ItemStack aStack, CompoundTag aNBT) {
		if (aStack == null || aStack.isEmpty()) return;
		boolean tOwned = owned(aStack);
		DataComponentType<CustomData> tType = tOwned ? GT_API.DATA_TYPE : DataComponents.CUSTOM_DATA;
		if (tOwned) aStack.remove(DataComponents.CUSTOM_DATA);
		if (aNBT == null || aNBT.isEmpty()) {
			aStack.remove(tType);
		} else {
			aStack.set(tType, CustomData.of(aNBT));
		}
	}

	public static boolean has(ItemStack aStack) {
		if (aStack == null || aStack.isEmpty()) return false;
		CustomData tData = aStack.get(GT_API.DATA_TYPE);
		if (tData != null && !tData.isEmpty()) return true;
		CustomData tOld = aStack.get(DataComponents.CUSTOM_DATA);
		return tOld != null && !tOld.isEmpty();
	}

	/** Moves the data of a GT6 stack from the old place to {@link GT_API#DATA} (the touch of {@link #get}), and the same inside the vanilla containers it holds
	 *  (shulker boxes, bundles). GT6's own stored stacks (NBT_INV_LIST) are restored through ST.load/{@link #set} and need no visit. Returns whether anything moved. */
	public static boolean migrate(ItemStack aStack) {
		if (aStack == null || aStack.isEmpty()) return false;
		boolean tHadOld = aStack.get(DataComponents.CUSTOM_DATA) != null;
		if (tHadOld) get(aStack);
		boolean rMoved = tHadOld && aStack.get(DataComponents.CUSTOM_DATA) == null;
		ItemContainerContents tContents = aStack.get(DataComponents.CONTAINER);
		if (tContents != null && tContents != ItemContainerContents.EMPTY) {
			List<ItemStack> tItems = new ArrayList<>(tContents.getSlots());
			boolean tChanged = false;
			for (int i = 0; i < tContents.getSlots(); i++) {
				ItemStack tItem = tContents.getStackInSlot(i);
				tChanged |= migrate(tItem);
				tItems.add(tItem);
			}
			if (tChanged) {aStack.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(tItems)); rMoved = true;}
		}
		BundleContents tBundle = aStack.get(DataComponents.BUNDLE_CONTENTS);
		if (tBundle != null && !tBundle.isEmpty()) {
			List<ItemStackTemplate> tTemplates = new ArrayList<>(tBundle.size());
			boolean tChanged = false;
			for (ItemStackTemplate tTemplate : tBundle.items()) {
				ItemStack tItem = tTemplate.components().isEmpty() ? ItemStack.EMPTY : tTemplate.create();
				if (migrate(tItem)) {tTemplates.add(ItemStackTemplate.fromNonEmptyStack(tItem)); tChanged = true;} else tTemplates.add(tTemplate);
			}
			if (tChanged) {aStack.set(DataComponents.BUNDLE_CONTENTS, new BundleContents(tTemplates)); rMoved = true;}
		}
		return rMoved;
	}

	/** The stacks of a container (live ones, so the move is in place); the container is told when something moved. */
	public static boolean migrateContainer(Container aContainer) {
		boolean rMoved = false;
		for (int i = 0; i < aContainer.getContainerSize(); i++) rMoved |= migrate(aContainer.getItem(i));
		if (rMoved) aContainer.setChanged();
		return rMoved;
	}

	/** Inventory with equipment, and the ender chest. */
	public static boolean migratePlayer(Player aPlayer) {
		return migrateContainer(aPlayer.getInventory()) | migrateContainer(aPlayer.getEnderChestInventory());
	}

	/** Every slot of an opened menu (the container and the player's inventory in it). */
	public static boolean migrateMenu(AbstractContainerMenu aMenu) {
		boolean rMoved = false;
		for (Slot tSlot : aMenu.slots) if (migrate(tSlot.getItem())) {tSlot.setChanged(); rMoved = true;}
		return rMoved;
	}

	/** The block entities of a loaded chunk that are containers. Other mods' storage behind capabilities is not reached: they hand out immutable resources. */
	public static boolean migrateChunk(LevelChunk aChunk) {
		boolean rMoved = false;
		for (BlockEntity tBlockEntity : new ArrayList<>(aChunk.getBlockEntities().values())) {
			if (tBlockEntity instanceof Container tContainer && migrateContainer(tContainer)) {tBlockEntity.setChanged(); rMoved = true;}
		}
		return rMoved;
	}

	/** What an entity holds: a dropped item, a framed item, its container (minecart with chest), its equipment. Players are visited by {@link #migratePlayer}. */
	public static boolean migrateEntity(Entity aEntity) {
		if (aEntity == null || aEntity instanceof Player || aEntity.level().isClientSide()) return false;
		boolean rMoved = false;
		if (aEntity instanceof ItemEntity tItemEntity) {
			ItemStack tItem = tItemEntity.getItem().copy();
			if (migrate(tItem)) {tItemEntity.setItem(tItem); rMoved = true;}
		}
		if (aEntity instanceof ItemFrame tFrame) rMoved |= migrate(tFrame.getItem());
		if (aEntity instanceof Container tContainer) rMoved |= migrateContainer(tContainer);
		if (aEntity instanceof LivingEntity tLiving) for (EquipmentSlot tSlot : EquipmentSlot.VALUES) rMoved |= migrate(tLiving.getItemBySlot(tSlot));
		return rMoved;
	}
}

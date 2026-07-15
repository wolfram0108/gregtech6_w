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

package gregtech.items.behaviors;

import java.util.List;

import gregapi.code.ItemNBT;
import gregapi.item.multiitem.MultiItem;
import gregapi.item.multiitem.behaviors.IBehavior.AbstractBehaviorDefault;
import gregapi.util.ST;
import gregapi.util.UT;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;

public class Behavior_DataOrb extends AbstractBehaviorDefault {
	@Override
	public List<String> getAdditionalToolTips(MultiItem aItem, List<String> aList, ItemStack aStack) {
		if (!getDataTitle(aStack).equals("")) {
			aList.add(getDataTitle(aStack));
			aList.add(getDataName(aStack));
		}
		return aList;
	}
	
	public static void copyInventory(ItemStack aInventory[], ItemStack aNewContent[], int aIndexlength) {
		for (int i = 0; i < aIndexlength; i++) {
			if (aNewContent[i] == null)
				aInventory[i] = null;
			else
				aInventory[i] = ST.copy(aNewContent[i]);
		}
	}
	
	public static String getDataName(ItemStack aStack) {
		CompoundTag tNBT = ItemNBT.get(aStack);
		if (tNBT == null) return "";
		return tNBT.getStringOr("mDataName", "");
	}
	
	public static String getDataTitle(ItemStack aStack) {
		CompoundTag tNBT = ItemNBT.get(aStack);
		if (tNBT == null) return "";
		return tNBT.getStringOr("mDataTitle", "");
	}
	
	public static CompoundTag setDataName(ItemStack aStack, String aDataName) {
		CompoundTag tNBT = ItemNBT.get(aStack);
		if (tNBT == null) tNBT = UT.NBT.make();
		tNBT.putString("mDataName", aDataName);
		UT.NBT.set(aStack, tNBT);
		return tNBT;
	}
	
	public static CompoundTag setDataTitle(ItemStack aStack, String aDataTitle) {
		CompoundTag tNBT = ItemNBT.get(aStack);
		if (tNBT == null) tNBT = UT.NBT.make();
		tNBT.putString("mDataTitle", aDataTitle);
		UT.NBT.set(aStack, tNBT);
		return tNBT;
	}
	
	public static ItemStack[] getNBTInventory(ItemStack aStack) {
		ItemStack[] tInventory = new ItemStack[256];
		CompoundTag tNBT = ItemNBT.get(aStack);
		if (tNBT == null) return tInventory;
		
		ListTag tNBT_ItemList = tNBT.getListOrEmpty("Inventory");
		for (int i = 0; i < tNBT_ItemList.size(); i++) {
			CompoundTag tag = tNBT_ItemList.getCompoundOrEmpty(i);
			byte slot = tag.getByteOr("Slot", (byte)0);
			if (slot >= 0 && slot < tInventory.length) {
				tInventory[slot] = ST.load(tag);
			}
		}
		return tInventory;
	}
	
	public static CompoundTag setNBTInventory(ItemStack aStack, ItemStack[] aInventory) {
		CompoundTag tNBT = ItemNBT.get(aStack);
		if (tNBT == null) tNBT = UT.NBT.make();
		
		ListTag tNBT_ItemList = new ListTag();
		for (int i = 0; i < aInventory.length; i++) {
			ItemStack stack = aInventory[i];
			if (stack != null) {
				CompoundTag tag = UT.NBT.make();
				tag.putByte("Slot", (byte) i);
				tNBT_ItemList.add(ST.save(stack));
			}
		}
		tNBT.put("Inventory", tNBT_ItemList);
		UT.NBT.set(aStack, tNBT);
		return tNBT;
	}
}

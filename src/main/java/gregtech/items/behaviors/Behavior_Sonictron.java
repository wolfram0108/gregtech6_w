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

import gregapi.code.ItemNBT;
import gregapi.item.multiitem.MultiItem;
import gregapi.item.multiitem.behaviors.IBehavior;
import gregapi.item.multiitem.behaviors.IBehavior.AbstractBehaviorDefault;
import gregapi.util.ST;
import gregapi.util.UT;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.level.Level;

public class Behavior_Sonictron extends AbstractBehaviorDefault {
	public static final IBehavior<MultiItem> INSTANCE = new Behavior_Sonictron();
	
	@Override
	public boolean onItemUseFirst(MultiItem aItem, ItemStack aStack, Player aPlayer, Level aWorld, int aX, int aY, int aZ, byte aSide, float hitX, float hitY, float hitZ) {
		/*if (!aWorld.isClientSide() && aWorld.getBlock(aX, aY, aZ) == GregTech_API.sBlockMachines && aWorld.getBlockMetadata(aX, aY, aZ) == 6) {
			
			GT_TileEntity_Sonictron tSonictron = (GT_TileEntity_Sonictron)WD.te(aWorld, aX, aY, aZ, T);
			if (tSonictron != null) {
				ItemStack[] tInventory = getNBTInventory(aStack);
				if (aPlayer.isSneaking()) {
					copyInventory(tSonictron.mInventory, tInventory, 64);
				} else {
					copyInventory(tInventory, tSonictron.mInventory, 64);
				}
				setNBTInventory(aStack, tInventory);
				tSonictron.sendClientData = true;
				return true;
			}
			
		}*/
		setCurrentIndex(aStack, -1);
		return false;
	}
	
	@Override
	public ItemStack onItemRightClick(MultiItem aItem, ItemStack aStack, Level aWorld, Player aPlayer) {
		setCurrentIndex(aStack, 0);
		return aStack;
	}
	
	@Override
	public void onUpdate(MultiItem aItem, ItemStack aStack, Level aWorld, Entity aPlayer, int aTimer, boolean aIsInHand) {
		int tTickTimer      = getTickTimer(aStack),
			tCurrentIndex   = getCurrentIndex(aStack);
			
		if (tTickTimer++%2==0&&tCurrentIndex>-1) {
			//ItemStack[] tInventory = getNBTInventory(aStack);
			//GT.doSonictronSound(tInventory[tCurrentIndex], aPlayer.worldObj, aPlayer.posX, aPlayer.posY, aPlayer.posZ);
			if (++tCurrentIndex>63) tCurrentIndex=-1;
		}
		
		setTickTimer(aStack, tTickTimer);
		setCurrentIndex(aStack, tCurrentIndex);
	}
	
	public static int getCurrentIndex(ItemStack aStack) {
		CompoundTag tNBTTagCompound = ItemNBT.get(aStack);
		if (tNBTTagCompound == null) tNBTTagCompound = UT.NBT.make();
		return tNBTTagCompound.getIntOr("mCurrentIndex", 0);
	}

	public static int getTickTimer(ItemStack aStack) {
		CompoundTag tNBTTagCompound = ItemNBT.get(aStack);
		if (tNBTTagCompound == null) tNBTTagCompound = UT.NBT.make();
		return tNBTTagCompound.getIntOr("mTickTimer", 0);
	}

	// PORT-TODO(F8, остаточный риск): оригинал НИГДЕ не вызывает setTagCompound для этого мутированного
	// тега (в 1.7.10 это был живой объект стека, мутация сохранялась сама), вызывающий код тоже не
	// коммитит возврат. Под мостом ItemNBT это становится no-op. См. ItemNBT.java, decisions/F8-nbt-data-components.md §7.
	public static CompoundTag setCurrentIndex(ItemStack aStack, int aIndex) {
		CompoundTag tNBTTagCompound = ItemNBT.get(aStack);
		if (tNBTTagCompound == null) tNBTTagCompound = UT.NBT.make();
		tNBTTagCompound.putInt("mCurrentIndex", aIndex);
		return tNBTTagCompound;
	}

	// PORT-TODO(F8, остаточный риск): см. setCurrentIndex выше — тот же паттерн без commit.
	public static CompoundTag setTickTimer(ItemStack aStack, int aTime) {
		CompoundTag tNBTTagCompound = ItemNBT.get(aStack);
		if (tNBTTagCompound == null) tNBTTagCompound = UT.NBT.make();
		tNBTTagCompound.putInt("mTickTimer", aTime);
		return tNBTTagCompound;
	}
	
	public static ItemStack[] getNBTInventory(ItemStack aStack) {
		ItemStack[] tInventory = new ItemStack[64];
		CompoundTag tNBT = ItemNBT.get(aStack);
		if (tNBT == null) return tInventory;
		
		ListTag tNBT_ItemList = tNBT.getTagList("Inventory", 10);
		for (int i = 0; i < tNBT_ItemList.tagCount(); i++) {
			CompoundTag tag = tNBT_ItemList.getCompoundTagAt(i);
			byte slot = tag.getByte("Slot");
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
	
	public static void copyInventory(ItemStack aInventory[], ItemStack aNewContent[], int aIndexlength) {
		for (int i = 0; i < aIndexlength; i++) {
			if (aNewContent[i] == null)
				aInventory[i] = null;
			else
				aInventory[i] = ST.copy(aNewContent[i]);
		}
	}
}

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
 *
 * Modified in 2026 for the GregTech 6 NeoForge port
 * (https://github.com/wolfram0108/gregtech6_w): ported from Minecraft 1.7.10 / Forge
 * to Minecraft 26.1.2 / NeoForge.
 */

package gregtech.tileentity.computer;

import static gregapi.data.CS.*;

import java.util.List;

import gregapi.code.ItemNBT;
import gregapi.data.LH;
import gregapi.data.LH.Chat;
import gregapi.gui.ContainerClientDefault;
import gregapi.gui.ContainerCommonDefault;
import gregapi.old.Textures;
import gregapi.render.BlockTextureDefault;
import gregapi.render.BlockTextureMulti;
import gregapi.render.IIconContainer;
import gregapi.render.ITexture;
import gregapi.tileentity.computer.TileEntityBase08DataSwitch;
import gregapi.util.OM;
import gregapi.util.UT;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;

/**
 * @author Gregorius Techneticies
 */
public class MultiTileEntityHDDSwitch extends TileEntityBase08DataSwitch {
	static {
		LH.add("gt.multitileentity.hdd.switch.tooltip", "Switches between the 16 Data Slots using Selector Covers");
	}
	
	@Override
	public void addToolTips(List<String> aList, ItemStack aStack, boolean aF3_H) {
		aList.add(Chat.CYAN + LH.get("gt.multitileentity.hdd.switch.tooltip"));
		super.addToolTips(aList, aStack, aF3_H);
	}
	
	@Override
	public CompoundTag getUSBData(byte aSide, int aUSBTier) {
		ItemStack tDrive = slot(0);
		if (OM.is(OD_USB_DRIVES[aUSBTier], tDrive) && ItemNBT.has(tDrive)) {
			CompoundTag tDriveData = ItemNBT.get(tDrive).getCompound(NBT_USB_DRIVE);
			if (tDriveData.getByte(NBT_USB_TIER+mMode) <= aUSBTier) return tDriveData.contains(NBT_USB_DATA+mMode) ? tDriveData.getCompound(NBT_USB_DATA+mMode) : null;
		}
		return null;
	}

	// F8: внешний тег захвачен ОДИН раз в tNBT (создан, если отсутствовал), вложенная tDriveData
	// мутируется на месте (вложенная мутация внутри одного и того же дерева тегов работает как раньше),
	// коммит единый ItemNBT.set в конце — иначе все правки тихо терялись бы (см. ItemNBT.java).
	@Override
	public boolean setUSBData(byte aSide, int aUSBTier, CompoundTag aData) {
		ItemStack tDrive = slot(0);
		if (OM.is(OD_USB_DRIVES[aUSBTier], tDrive)) {
			CompoundTag tNBT = ItemNBT.has(tDrive) ? ItemNBT.get(tDrive) : UT.NBT.make();
			CompoundTag tDriveData = tNBT.getCompound(NBT_USB_DRIVE);
			if (aData == null || aData.isEmpty()) {
				tDriveData.remove(NBT_USB_DATA+mMode);
				tDriveData.remove(NBT_USB_TIER+mMode);
			} else {
				tDriveData.put(NBT_USB_DATA+mMode, aData);
				tDriveData.putByte(NBT_USB_TIER+mMode, (byte)aUSBTier);
			}
			if (tDriveData.isEmpty()) {
				tNBT.remove(NBT_USB_DRIVE);
			} else {
				tNBT.put(NBT_USB_DRIVE, tDriveData);
			}
			ItemNBT.set(tDrive, tNBT.isEmpty() ? null : tNBT);
			return T;
		}
		return F;
	}
	
	@Override
	public ITexture getTexture2(Block aBlock, int aRenderPass, byte aSide, boolean[] aShouldSideBeRendered) {
		return aShouldSideBeRendered[aSide] ? BlockTextureMulti.get(BlockTextureDefault.get(sColoreds[(int)UT.Code.bind_(0, 2, aSide)], mRGBa), BlockTextureDefault.get(sOverlays[(int)UT.Code.bind_(0, 2, aSide)])) : null;
	}
	
	// Icons
	public static IIconContainer sColoreds[] = new IIconContainer[] {
		new Textures.BlockIcons.CustomIcon("machines/hdd/switch/colored/bottom"),
		new Textures.BlockIcons.CustomIcon("machines/hdd/switch/colored/top"),
		new Textures.BlockIcons.CustomIcon("machines/hdd/switch/colored/side"),
	}, sOverlays[] = new IIconContainer[] {
		new Textures.BlockIcons.CustomIcon("machines/hdd/switch/overlay/bottom"),
		new Textures.BlockIcons.CustomIcon("machines/hdd/switch/overlay/top"),
		new Textures.BlockIcons.CustomIcon("machines/hdd/switch/overlay/side"),
	};
	
	@Override public String getTileEntityName() {return "gt.multitileentity.hdd.switch";}
	
	@Override public ItemStack[] getDefaultInventory(CompoundTag aNBT) {return new ItemStack[1];}
	@Override public boolean canInsertItem2(int aSlot, ItemStack aStack, byte aSide) {return OM.is(OD_USB_DRIVES[0], aStack);}
	
	@Override public Object getGUIClient2(int aGUIID, Player aPlayer) {return new ContainerClientDefault(aPlayer.getInventory(), this, aGUIID, RES_PATH_GUI + "machines/HDDSwitch.png");}
	@Override public Object getGUIServer2(int aGUIID, Player aPlayer) {return new ContainerCommonDefault(aPlayer.getInventory(), this, aGUIID);}
}

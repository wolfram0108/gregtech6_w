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
public class MultiTileEntityUSBSwitch extends TileEntityBase08DataSwitch {
	static {
		LH.add("gt.multitileentity.usb.switch.tooltip", "Switches between 16 different USB Sticks using Selector Covers");
	}
	
	@Override
	public void addToolTips(List<String> aList, ItemStack aStack, boolean aF3_H) {
		aList.add(Chat.CYAN + LH.get("gt.multitileentity.usb.switch.tooltip"));
		super.addToolTips(aList, aStack, aF3_H);
	}
	
	@Override
	public CompoundTag getUSBData(byte aSide, int aUSBTier) {
		ItemStack tUSB = slot(mMode);
		if (OM.is(OD_USB_STICKS[aUSBTier], tUSB) && ItemNBT.has(tUSB) && ItemNBT.get(tUSB).getByteOr(NBT_USB_TIER, (byte)0) <= aUSBTier) {
			return ItemNBT.get(tUSB).getCompoundOrEmpty(NBT_USB_DATA);
		}
		return null;
	}

	// F8: тег захвачен ОДИН раз в tNBT (создан, если отсутствовал), все мутации идут в него,
	// коммит единый ItemNBT.set в конце — иначе все правки тихо терялись бы (см. ItemNBT.java).
	@Override
	public boolean setUSBData(byte aSide, int aUSBTier, CompoundTag aData) {
		ItemStack tUSB = slot(mMode);
		if (OM.is(OD_USB_STICKS[aUSBTier], tUSB)) {
			CompoundTag tNBT = ItemNBT.has(tUSB) ? ItemNBT.get(tUSB) : UT.NBT.make();
			if (aData == null || aData.isEmpty()) {
				tNBT.removeTag(NBT_USB_DATA);
				tNBT.removeTag(NBT_USB_TIER);
			} else {
				tNBT.put(NBT_USB_DATA, aData);
				tNBT.putByte(NBT_USB_TIER, (byte)aUSBTier);
			}
			ItemNBT.set(tUSB, tNBT.isEmpty() ? null : tNBT);
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
		new Textures.BlockIcons.CustomIcon("machines/usb/switch/colored/bottom"),
		new Textures.BlockIcons.CustomIcon("machines/usb/switch/colored/top"),
		new Textures.BlockIcons.CustomIcon("machines/usb/switch/colored/side"),
	}, sOverlays[] = new IIconContainer[] {
		new Textures.BlockIcons.CustomIcon("machines/usb/switch/overlay/bottom"),
		new Textures.BlockIcons.CustomIcon("machines/usb/switch/overlay/top"),
		new Textures.BlockIcons.CustomIcon("machines/usb/switch/overlay/side"),
	};
	
	@Override public String getTileEntityName() {return "gt.multitileentity.usb.hub";}
	
	@Override public ItemStack[] getDefaultInventory(CompoundTag aNBT) {return new ItemStack[16];}
	@Override public boolean canInsertItem2(int aSlot, ItemStack aStack, byte aSide) {return OM.is(OD_USB_STICKS[0], aStack);}
	
	@Override public Object getGUIClient2(int aGUIID, Player aPlayer) {return new ContainerClientDefault(aPlayer.inventory, this, aGUIID, RES_PATH_GUI + "machines/USBSwitch.png");}
	@Override public Object getGUIServer2(int aGUIID, Player aPlayer) {return new ContainerCommonDefault(aPlayer.inventory, this, aGUIID);}
}

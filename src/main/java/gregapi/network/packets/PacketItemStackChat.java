/**
 * Copyright (c) 2021 GregTech-6 Team
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

package gregapi.network.packets;

import static gregapi.data.CS.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;

import gregapi.GT_API;
import gregapi.network.INetworkHandler;
import gregapi.network.IPacket;
import gregapi.util.ST;
import gregapi.util.UT;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.BlockGetter;

/**
 * @author Gregorius Techneticies
 */
public class PacketItemStackChat implements IPacket {
	private ItemStack mStack;

	public PacketItemStackChat() {/**/}

	public PacketItemStackChat(ItemStack aStack) {
		mStack = aStack;
	}

	@Override
	public byte getPacketID() {
		return 125;
	}

	@Override
	public ByteArrayDataOutput encode() {
		ByteArrayDataOutput aData = ByteStreams.newDataOutput();
		aData.writeShort(ST.id_(mStack.getItem()));
		aData.writeByte(mStack.getCount());
		aData.writeShort(ST.meta_(mStack));
		CompoundTag tNBT = getCustomData(mStack);
		if (tNBT == null) aData.writeShort(-1); else {
			try {
				ByteArrayOutputStream tBuffer = new ByteArrayOutputStream();
				NbtIo.writeCompressed(tNBT, tBuffer);
				byte[] tData = tBuffer.toByteArray();
				aData.writeShort(tData.length);
				aData.write(tData);
			} catch (IOException e) {e.printStackTrace(ERR);}
		}
		return aData;
	}

	@Override
	public IPacket decode(ByteArrayDataInput aData) {
		return new PacketItemStackChat(setCustomData(ST.make(aData.readShort(), aData.readByte(), aData.readShort()), readNBTTagCompoundFromBuffer(aData)));
	}

	public CompoundTag readNBTTagCompoundFromBuffer(ByteArrayDataInput aData) {
		short tLength = aData.readShort();
		if (tLength <= 0) return null;
		byte[] tData = new byte[tLength];
		aData.readFully(tData);
		try {return NbtIo.readCompressed(new ByteArrayInputStream(tData), NbtAccounter.create(2097152L));} catch (IOException e) {e.printStackTrace(ERR);}
		return null;
	}

	private static CompoundTag getCustomData(ItemStack aStack) {
		CustomData tData = aStack.get(DataComponents.CUSTOM_DATA);
		return tData == null || tData.isEmpty() ? null : tData.copyTag();
	}

	private static ItemStack setCustomData(ItemStack aStack, CompoundTag aNBT) {
		if (aStack != null && aNBT != null) aStack.set(DataComponents.CUSTOM_DATA, CustomData.of(aNBT));
		return aStack;
	}

	@Override
	@SuppressWarnings("unchecked")
	public void process(BlockGetter aWorld, INetworkHandler aNetworkHandler) {
		if (mStack == null) return;
		Player tPlayer = GT_API.api_proxy.getThePlayer();
		DISPLAY_TEMP_TOOLTIP = F;
		List<Component> tList = mStack.getTooltipLines(Item.TooltipContext.of(tPlayer == null ? null : tPlayer.level(), tPlayer), tPlayer, TooltipFlag.NORMAL);
		DISPLAY_TEMP_TOOLTIP = T;
		if (tList != null && !tList.isEmpty()) {
			UT.Entities.chat(tPlayer, tList, F);
		} else {
			UT.Entities.chat(tPlayer, mStack.getHoverName());
		}
	}
}

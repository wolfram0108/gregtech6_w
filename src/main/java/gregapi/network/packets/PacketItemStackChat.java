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
import gregapi.code.ItemNBT;
import gregapi.network.INetworkHandler;
import gregapi.network.IPacket;
import gregapi.util.ST;
import gregapi.util.UT;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
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
		aData.writeShort(ST.id(mStack));
		aData.writeByte(mStack.getCount());
		aData.writeShort(ST.meta_(mStack));
		CompoundTag tNBT = ItemNBT.get(mStack); // F8 стык: было локальное aStack.get(CUSTOM_DATA)+copyTag — репойнт на центр ItemNBT.get
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
		return new PacketItemStackChat(ST.make(aData.readShort(), aData.readByte(), aData.readShort(), readNBTTagCompoundFromBuffer(aData)));
	}

	public CompoundTag readNBTTagCompoundFromBuffer(ByteArrayDataInput aData) {
		short tLength = aData.readShort();
		if (tLength <= 0) return null;
		byte[] tData = new byte[tLength];
		aData.readFully(tData);
		try {return NbtIo.readCompressed(new ByteArrayInputStream(tData), NbtAccounter.create(2097152L));} catch (IOException e) {e.printStackTrace(ERR);}
		return null;
	}

	@Override
	@SuppressWarnings("unchecked")
	public void process(BlockGetter aWorld, INetworkHandler aNetworkHandler) {
		DISPLAY_TEMP_TOOLTIP = F;
		List<Component> tList = mStack.getTooltipLines(Item.TooltipContext.of(null, GT_API.api_proxy.getThePlayer()), GT_API.api_proxy.getThePlayer(), TooltipFlag.NORMAL);
		DISPLAY_TEMP_TOOLTIP = T;
		if (tList != null && !tList.isEmpty()) {
			UT.Entities.chat(GT_API.api_proxy.getThePlayer(), tList, F);
		} else {
			UT.Entities.chat(GT_API.api_proxy.getThePlayer(), mStack.getHoverName());
		}
	}
}

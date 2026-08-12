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

package gregapi.network.packets;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;

import gregapi.block.prefixblock.PrefixBlockOreMap;
import gregapi.network.INetworkHandler;
import gregapi.network.IPacket;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;

/**
 * Ветка 1.20.1: карта материалов руды на чанк ({@link PrefixBlockOreMap}) едет клиенту ЭТИМ пакетом.
 *
 * <p>В 26.x носителем был neo-attachment, и синк вёл движок сам ({@code sync(STREAM_CODEC)} — при отправке
 * чанка игроку и при {@code chunk.syncData(TYPE)}). В Forge 1.20.1 капабилити чанка автосинка не имеет,
 * поэтому те же два момента обслуживает свой пакет — той же схемой, какой Грегориус слал всё остальное:
 * байт-ID в общей таблице канала {@code GAPI}, тело — сырой поток. Содержимое — вся карта чанка целиком,
 * ровно как отправлял attachment (он тоже слал значение целиком, а не дельту).</p>
 */
public class PacketOreMap implements IPacket {
	public final int mChunkX, mChunkZ;
	public final long[] mEntries;

	public PacketOreMap() {
		mChunkX = 0; mChunkZ = 0; mEntries = null;
	}

	public PacketOreMap(int aChunkX, int aChunkZ, long[] aEntries) {
		mChunkX = aChunkX; mChunkZ = aChunkZ; mEntries = aEntries;
	}

	@Override
	public byte getPacketID() {
		return 124;
	}

	@Override
	public ByteArrayDataOutput encode() {
		ByteArrayDataOutput rOut = ByteStreams.newDataOutput();
		rOut.writeInt(mChunkX);
		rOut.writeInt(mChunkZ);
		rOut.writeInt(mEntries.length);
		for (long tEntry : mEntries) rOut.writeLong(tEntry);
		return rOut;
	}

	@Override
	public IPacket decode(ByteArrayDataInput aData) {
		int tChunkX = aData.readInt(), tChunkZ = aData.readInt(), tSize = aData.readInt();
		long[] tEntries = new long[tSize];
		for (int i = 0; i < tSize; i++) tEntries[i] = aData.readLong();
		return new PacketOreMap(tChunkX, tChunkZ, tEntries);
	}

	@Override
	public void process(BlockGetter aWorld, INetworkHandler aNetworkHandler) {
		if (mEntries == null || !(aWorld instanceof Level tWorld)) return;
		// Чанк уже пришёл (пакет чанка идёт по тому же соединению РАНЬШЕ — момент отправки задан ChunkWatchEvent.Watch);
		// если его всё же нет, писать некуда — карта приедет заново со следующей отправкой чанка.
		net.minecraft.world.level.chunk.ChunkAccess tChunk = tWorld.getChunkSource().getChunk(mChunkX, mChunkZ, false);
		if (tChunk == null) return;
		PrefixBlockOreMap tMap = PrefixBlockOreMap.existing(tChunk);
		if (tMap == null) return;
		tMap.unpack(mEntries);
	}
}

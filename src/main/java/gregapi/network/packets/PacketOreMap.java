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

/** 1.20.1's chunk capability has no autosync like a newer-engine attachment would, so this packet covers the same two
 *  sync moments (chunk send, explicit resync) by hand, sending the whole per-chunk map each time, never a delta. */
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
		// The chunk packet always precedes this one on the same connection; if the chunk is still missing there's nowhere to
		// write the data, and it simply arrives again on the next chunk send.
		net.minecraft.world.level.chunk.ChunkAccess tChunk = tWorld.getChunkSource().getChunk(mChunkX, mChunkZ, false);
		if (tChunk == null) return;
		PrefixBlockOreMap tMap = PrefixBlockOreMap.existing(tChunk);
		if (tMap == null) return;
		// Without a dirty mark, a section tessellated before this data arrived never redraws, so the ore map on screen
		// goes stale forever though the data is correct; only changed positions get marked, via the shared WD.update path.
		long[] tOld = tMap.pack();
		tMap.unpack(mEntries);
		// On first delivery the client had no map yet, so nothing is 'changed' -- the column just arrived and isn't tessellated
		// yet, making a per-entry mark pointless; one mark per populated section matches what the engine itself does on arrival.
		if (tOld.length == 0) {markColumnDirty(tWorld, mEntries); return;}
		java.util.HashMap<Integer, Short> tOldMap = new java.util.HashMap<>(tOld.length * 2);
		for (long tEntry : tOld) tOldMap.put((int)(tEntry >>> 16), (short)(tEntry & 0xFFFFL));
		for (long tEntry : mEntries) {
			int tKey = (int)(tEntry >>> 16); short tMeta = (short)(tEntry & 0xFFFFL);
			Short tWas = tOldMap.remove(tKey);
			if (tWas == null || tWas.shortValue() != tMeta) markDirty(tWorld, tKey);
		}
		for (Integer tKey : tOldMap.keySet()) markDirty(tWorld, tKey);
	}

	/** One mark per section that actually holds entries, skipping empty ones, through the same WD.update bottleneck as
	 *  the per-entry branch -- since that call already marks the area around a block, covering the column fully. */
	private void markColumnDirty(Level aWorld, long[] aEntries) {
		java.util.HashSet<Integer> tSections = new java.util.HashSet<>();
		for (long tEntry : aEntries) {
			int tKey = (int)(tEntry >>> 16);
			int tY = (tKey >>> 8) - 2048;
			if (tSections.add(tY >> 4)) markDirty(aWorld, (tKey & 0xFF) | (((tY & ~15) + 8 + 2048) << 8));
		}
	}

	/** Inverse of the map's own key packing, back into a world position. */
	private void markDirty(Level aWorld, int aKey) {
		gregapi.util.WD.update(aWorld, (mChunkX << 4) | (aKey & 15), (aKey >>> 8) - 2048, (mChunkZ << 4) | ((aKey >>> 4) & 15));
	}
}

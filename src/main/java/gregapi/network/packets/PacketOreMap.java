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
		// ВРЕМЕННАЯ ДИАГНОСТИКА (цвет мира 2026-08-13): судьба первых 10 пакетов карты — ловим гонку «пакет раньше чанка».
		if (sDiagPackets < 10) {sDiagPackets++; gregapi.data.CS.OUT.println("[GT6-OREMAPDIAG] чанк " + mChunkX + "," + mChunkZ + " записей=" + (mEntries == null ? 0 : mEntries.length) + " чанк-на-клиенте=" + (tChunk != null) + " капа=" + (tChunk != null && PrefixBlockOreMap.existing(tChunk) != null));}
		if (tChunk == null) return;
		PrefixBlockOreMap tMap = PrefixBlockOreMap.existing(tChunk);
		if (tMap == null) return;
		// Класс «применили данные — не перерисовали» (репорт игрока: слиток при постановке белый, доложенный в
		// стопку невидим): секция была тесселирована ДО прихода карты, и без пометки dirty движок её больше не
		// перестраивает — данные лежат, картинка вечно старая. Помечаем ТОЛЬКО изменившиеся позиции — тем же
		// горлышком WD.update (= sendBlockUpdated → blockChanged, секции ±1), которым идёт весь остальной синк мода.
		long[] tOld = tMap.pack();
		tMap.unpack(mEntries);
		// ПЕРВАЯ ДОСТАВКА (у клиента карты ещё не было) — это НЕ «данные изменились», а «чанк приехал»: секции
		// его колонки к этому моменту не тесселированы, и метить их поштучно незачем. Поштучный путь давал здесь
		// ХУДШИЙ случай диффа — «изменились ВСЕ записи», а их в чанке сотни (перепись сейва GT6WGTest: медиана
		// 502, p90 1083, максимум 2750), и каждая через WD.update стоила 27 движковых пометок секций.
		// Каноничная форма момента «пришёл чанк» — одна пометка на колонку, ровно как это делает сам движок
		// (26.x ClientPacketListener.enableChunkLight:909 — один setSectionRangeDirty на приход чанка; в 1.20.1
		// такого метода нет, поэтому колонку метим тем же WD.update, но ОДИН раз на секцию, а не на запись).
		if (tOld.length == 0) {markColumnDirty(tWorld, mEntries); return;}
		java.util.HashMap<Integer, Short> tOldMap = new java.util.HashMap<>(tOld.length * 2);
		for (long tEntry : tOld) tOldMap.put((int)(tEntry >>> 16), (short)(tEntry & 0xFFFFL));
		for (long tEntry : mEntries) {
			int tKey = (int)(tEntry >>> 16); short tMeta = (short)(tEntry & 0xFFFFL);
			Short tWas = tOldMap.remove(tKey);
			if (tWas == null || tWas.shortValue() != tMeta) markDirty(tWorld, tKey);
		}
		for (Integer tKey : tOldMap.keySet()) markDirty(tWorld, tKey); // было и исчезло
	}

	/** Пометка колонки чанка — по одной на КАЖДУЮ секцию, где записи реально есть (пустые не трогаем). Пометка
	 *  ставится тем же горлышком WD.update, что и поштучная ветка, поэтому охват внутри чанка полный: WD.update
	 *  метит секции ±1 вокруг блока, а мы бьём в каждую населённую секцию колонки. */
	private void markColumnDirty(Level aWorld, long[] aEntries) {
		java.util.HashSet<Integer> tSections = new java.util.HashSet<>();
		for (long tEntry : aEntries) {
			int tKey = (int)(tEntry >>> 16);
			int tY = (tKey >>> 8) - 2048;
			if (tSections.add(tY >> 4)) markDirty(aWorld, (tKey & 0xFF) | (((tY & ~15) + 8 + 2048) << 8));
		}
	}

	private static int sDiagPackets = 0; // ВРЕМЕННАЯ ДИАГНОСТИКА — снять вместе с [GT6-OREMAPDIAG]

	/** Обратное преобразование ключа карты (PrefixBlockOreMap.key: ((y+2048)<<8) | ((z&15)<<4) | (x&15)) в мировую позицию. */
	private void markDirty(Level aWorld, int aKey) {
		gregapi.util.WD.update(aWorld, (mChunkX << 4) | (aKey & 15), (aKey >>> 8) - 2048, (mChunkZ << 4) | ((aKey >>> 4) & 15));
	}
}

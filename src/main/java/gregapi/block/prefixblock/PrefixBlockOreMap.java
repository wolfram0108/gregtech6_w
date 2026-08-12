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

package gregapi.block.prefixblock;

import java.util.ArrayList;
import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;

import it.unimi.dsi.fastutil.ints.Int2ShortOpenHashMap;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraftforge.eventbus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.minecraftforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * Правка №1 (BUG-106, решение пользователя 2026-08-09): материал руды/породы хранится НЕ в блок-сущности
 * на каждой позиции (~600 сущностей и ~47 КБ на чанк, 2 845 589 живых объектов по замеру), а в ОДНОЙ
 * компактной карте «позиция → ID материала» на чанк.
 *
 * <p><b>Семантика 1:1 с оригиналом:</b> значение — тот же глобальный ID материала, что и
 * {@code PrefixBlockTileEntity.mMetaData} у Грегориуса (индекс в {@code OreDictMaterial.MATERIAL_ARRAY});
 * меняется только контейнер. Блок-стейт невозможен: свойство на 32767 значений порождает у движка
 * квадратичные таблицы переходов (миллиарды ссылок на блок).</p>
 *
 * <p><b>Носитель — штатный NeoForge-attachment на чанке:</b> персист — {@code serialize(CODEC)}
 * (пишется/читается вместе с чанком, и на ProtoChunk ворлдгена, и на LevelChunk), синк клиенту —
 * {@code sync(STREAM_CODEC)} (движок сам шлёт при отправке чанка игроку, {@code AttachmentSync.onChunkSent};
 * точечные изменения — {@code chunk.syncData(TYPE)}).</p>
 *
 * <p><b>Потоки:</b> сервер пишет только в main-поток (или в свой ProtoChunk при генерации); клиентские
 * обновления синка заменяют объект карты целиком (StreamCodec-путь создаёт новый экземпляр) — рендер-потоки
 * видят либо старую, либо новую карту, никогда полу-перестроенную.</p>
 */
public final class PrefixBlockOreMap {
	/** Ключ позиции внутри чанка: (y+2048)&lt;&lt;8 | localZ&lt;&lt;4 | localX. Смещение +2048 покрывает
	 *  любые допустимые движком диапазоны высот, не завися от minY конкретного измерения. */
	public static int key(int aX, int aY, int aZ) {return ((aY + 2048) << 8) | ((aZ & 15) << 4) | (aX & 15);}

	private final Int2ShortOpenHashMap mMap;

	public PrefixBlockOreMap() {mMap = new Int2ShortOpenHashMap(); mMap.defaultReturnValue((short)0);}
	private PrefixBlockOreMap(Int2ShortOpenHashMap aMap) {mMap = aMap; mMap.defaultReturnValue((short)0);}

	/** 0 = на позиции нет записи (ровно как прежнее «нет сущности» в getMetaDataValue). */
	public short get(int aX, int aY, int aZ) {return mMap.get(key(aX, aY, aZ));}
	public void set(int aX, int aY, int aZ, short aMeta) {if (aMeta == 0) mMap.remove(key(aX, aY, aZ)); else mMap.put(key(aX, aY, aZ), aMeta);}
	public void remove(int aX, int aY, int aZ) {mMap.remove(key(aX, aY, aZ));}
	public boolean isEmpty() {return mMap.isEmpty();}
	public int size() {return mMap.size();}

	// Персист: список long, каждый = (ключ << 16) | (материал & 0xFFFF). Ключ ≤ 20 бит, материал ≤ 16 бит.
	private static final Codec<PrefixBlockOreMap> ENTRIES_CODEC = Codec.LONG.listOf().xmap(aList -> {
		Int2ShortOpenHashMap tMap = new Int2ShortOpenHashMap(aList.size());
		for (long tEntry : aList) tMap.put((int)(tEntry >>> 16), (short)(tEntry & 0xFFFFL));
		return new PrefixBlockOreMap(tMap);
	}, aMap -> {
		List<Long> rList = new ArrayList<>(aMap.mMap.size());
		for (it.unimi.dsi.fastutil.ints.Int2ShortMap.Entry tEntry : aMap.mMap.int2ShortEntrySet())
			rList.add(((long)tEntry.getIntKey() << 16) | (tEntry.getShortValue() & 0xFFFFL));
		return rList;
	});
	public static final MapCodec<PrefixBlockOreMap> CODEC = ENTRIES_CODEC.fieldOf("gt6_ore");

	public static final StreamCodec<RegistryFriendlyByteBuf, PrefixBlockOreMap> STREAM_CODEC = StreamCodec.of((aBuf, aMap) -> {
		aBuf.writeVarInt(aMap.mMap.size());
		for (it.unimi.dsi.fastutil.ints.Int2ShortMap.Entry tEntry : aMap.mMap.int2ShortEntrySet()) {
			aBuf.writeVarInt(tEntry.getIntKey());
			aBuf.writeShort(tEntry.getShortValue());
		}
	}, aBuf -> {
		int tSize = aBuf.readVarInt();
		Int2ShortOpenHashMap tMap = new Int2ShortOpenHashMap(tSize);
		for (int i = 0; i < tSize; i++) tMap.put(aBuf.readVarInt(), aBuf.readShort());
		return new PrefixBlockOreMap(tMap);
	});

	/** Центральный DeferredRegister attachment-типов GT6 — ЕДИНСТВЕННОЕ место (тот же приём, что
	 *  GT6WorldgenFeature.FEATURES для Feature-типов). */
	public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, gregapi.data.MD.GAPI.mID);
	public static final java.util.function.Supplier<AttachmentType<PrefixBlockOreMap>> TYPE = ATTACHMENTS.register("ore_map",
		() -> AttachmentType.builder(() -> new PrefixBlockOreMap()).serialize(CODEC, aMap -> !aMap.isEmpty()).sync(STREAM_CODEC).build());

	public static void register(IEventBus aModBus) {ATTACHMENTS.register(aModBus);}
}

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

import it.unimi.dsi.fastutil.ints.Int2ShortOpenHashMap;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.eventbus.api.IEventBus;

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
 * <p><b>Ветка 1.20.1 — носитель сменился: attachment → capability.</b> {@code AttachmentType} есть только
 * у NeoForge; в Forge 1.20.1 его роль занимает capability на самом чанке. Проверено по декомпилу:
 * {@code LevelChunk implements ICapabilityProviderImpl<LevelChunk>} ({@code LevelChunk.java:50}), а персист
 * ведёт сам движок — {@code ChunkSerializer.java:350} пишет тег {@code ForgeCaps}, {@code :162} читает его
 * обратно. Своих записей в файл чанка не заводится.</p>
 *
 * <p><b>ProtoChunk капабилити не несёт</b> (обе точки {@code ChunkSerializer} явно кастуют к
 * {@code LevelChunk}; {@code ProtoChunk extends ChunkAccess} без {@code ICapabilityProvider},
 * {@code ProtoChunk.java:39}). Поэтому фаза ворлдгена возвращается к ФОРМЕ ОРИГИНАЛА 1.7.10 — материал едет
 * в самой блок-сущности ({@code PrefixBlockTileEntity.mMetaData}), а существующая миграция
 * ({@code PrefixBlock.migrateChunkOres} на {@code ChunkEvent.Load}) переливает её в карту и снимает
 * сущность. Порядок доказан декомпилом: {@code ChunkMap.java:706} строит {@code LevelChunk} из прото-чанка
 * (перенося отложенные сущности, {@code LevelChunk.java:103-107}), {@code :715} зовёт {@code runPostLoad()}
 * — промоушен ВСЕХ отложенных сущностей ({@code LevelChunk.java:514-518}), {@code :720}
 * {@code registerAllBlockEntitiesAfterLevelLoad()}, и лишь {@code :722} шлёт {@code ChunkEvent.Load}. То
 * есть к моменту миграции сущности прото-фазы уже все на месте. Сущность-однодневка живёт от генерации до
 * первой загрузки чанка — постоянных объектов на рудах не появляется.</p>
 *
 * <p><b>Синк клиенту</b> в 26.x вёл движок ({@code sync(STREAM_CODEC)}); в 1.20.1 автосинка капабилити
 * чанка нет — карта уходит СВОИМ пакетом GT6 ({@code gregapi.network.packets.PacketOreMap}, тот же
 * байт-конверт и та же таблица ID, что у остальных пакетов мода). Моменты — те же два, что были у
 * attachment: отправка чанка игроку ({@code ChunkWatchEvent.Watch} — движок сам документирует его как точку
 * «дослать свои чанковые данные», {@code ChunkWatchEvent.java:64-73}) и точечная запись в живом мире
 * (бывший {@code chunk.syncData(TYPE)}).</p>
 *
 * <p><b>Потоки:</b> сервер пишет только в main-поток (или в блок-сущность своего ProtoChunk при генерации);
 * клиентский синк заменяет содержимое карты целиком — рендер-потоки видят либо старое, либо новое
 * состояние, никогда полу-перестроенное.</p>
 */
public final class PrefixBlockOreMap {
	/** Ключ позиции внутри чанка: (y+2048)&lt;&lt;8 | localZ&lt;&lt;4 | localX. Смещение +2048 покрывает
	 *  любые допустимые движком диапазоны высот, не завися от minY конкретного измерения. */
	public static int key(int aX, int aY, int aZ) {return ((aY + 2048) << 8) | ((aZ & 15) << 4) | (aX & 15);}

	private final Int2ShortOpenHashMap mMap;

	public PrefixBlockOreMap() {mMap = new Int2ShortOpenHashMap(); mMap.defaultReturnValue((short)0);}

	/** 0 = на позиции нет записи (ровно как прежнее «нет сущности» в getMetaDataValue). */
	public short get(int aX, int aY, int aZ) {return mMap.get(key(aX, aY, aZ));}
	public void set(int aX, int aY, int aZ, short aMeta) {if (aMeta == 0) mMap.remove(key(aX, aY, aZ)); else mMap.put(key(aX, aY, aZ), aMeta);}
	public void remove(int aX, int aY, int aZ) {mMap.remove(key(aX, aY, aZ));}
	public boolean isEmpty() {return mMap.isEmpty();}
	public int size() {return mMap.size();}

	// Упаковка: одна запись = (ключ << 16) | (материал & 0xFFFF). Ключ ≤ 20 бит, материал ≤ 16 бит.
	// Та же упаковка, что несли Codec/StreamCodec 26.x-ветки — и на диск, и в провод.
	public long[] pack() {
		long[] rEntries = new long[mMap.size()];
		int i = 0;
		for (it.unimi.dsi.fastutil.ints.Int2ShortMap.Entry tEntry : mMap.int2ShortEntrySet()) rEntries[i++] = ((long)tEntry.getIntKey() << 16) | (tEntry.getShortValue() & 0xFFFFL);
		return rEntries;
	}
	public void unpack(long[] aEntries) {
		mMap.clear();
		for (long tEntry : aEntries) mMap.put((int)(tEntry >>> 16), (short)(tEntry & 0xFFFFL));
	}

	/** Имя массива внутри собственного тега капабилити (сам тег движок кладёт в {@code ForgeCaps}). */
	public static final String NBT_KEY = "gt6_ore";
	public static final ResourceLocation ID = new ResourceLocation(gregapi.data.MD.GAPI.mID, "ore_map");

	public static final Capability<PrefixBlockOreMap> CAP = CapabilityManager.get(new CapabilityToken<PrefixBlockOreMap>() {});

	/** Провайдер-носитель: одна карта на чанк; персист ведёт движок (ChunkSerializer, тег ForgeCaps). */
	private static final class Provider implements ICapabilitySerializable<CompoundTag> {
		private final PrefixBlockOreMap mData = new PrefixBlockOreMap();
		private final LazyOptional<PrefixBlockOreMap> mOptional = LazyOptional.of(() -> mData);

		@Override public <T> LazyOptional<T> getCapability(Capability<T> aCapability, Direction aSide) {return aCapability == CAP ? mOptional.cast() : LazyOptional.empty();}
		@Override public CompoundTag serializeNBT() {CompoundTag rNBT = new CompoundTag(); if (!mData.isEmpty()) rNBT.putLongArray(NBT_KEY, mData.pack()); return rNBT;}
		@Override public void deserializeNBT(CompoundTag aNBT) {mData.unpack(aNBT.getLongArray(NBT_KEY));}
	}

	/** ЕДИНСТВЕННАЯ точка подписки носителя (та же роль, что была у attachment-реестра 26.x-ветки):
	 *  объявление капабилити — на мод-шине, прикрепление к чанку — на форж-шине. */
	public static void register(IEventBus aModBus) {
		aModBus.addListener((RegisterCapabilitiesEvent aEvent) -> aEvent.register(PrefixBlockOreMap.class));
		MinecraftForge.EVENT_BUS.addGenericListener(LevelChunk.class, (AttachCapabilitiesEvent<LevelChunk> aEvent) -> aEvent.addCapability(ID, new Provider()));
	}

	/** Карта чанка, если носитель есть (LevelChunk с прикреплённой капой); иначе null. Чтение не создаёт. */
	public static PrefixBlockOreMap existing(ChunkAccess aChunk) {
		if (!(aChunk instanceof LevelChunk tChunk)) return null;
		return tChunk.getCapability(CAP).orElse(null);
	}
}

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

/** Ore material moves from a per-position BlockEntity (thousands of live objects, tens of KB per chunk)
 *  to one compact position->material map per chunk; on 1.20.1 the carrier is a chunk capability, engine-persisted. */
public final class PrefixBlockOreMap {
	/** The +2048 offset covers every height range the engine allows, regardless of a given dimension's minY. */
	public static int key(int aX, int aY, int aZ) {return ((aY + 2048) << 8) | ((aZ & 15) << 4) | (aX & 15);}

	private final Int2ShortOpenHashMap mMap;

	public PrefixBlockOreMap() {mMap = new Int2ShortOpenHashMap(); mMap.defaultReturnValue((short)0);}

	/** 0 means no entry at this position, matching the old "no tile entity" meaning exactly. */
	public short get(int aX, int aY, int aZ) {return mMap.get(key(aX, aY, aZ));}
	public void set(int aX, int aY, int aZ, short aMeta) {if (aMeta == 0) mMap.remove(key(aX, aY, aZ)); else mMap.put(key(aX, aY, aZ), aMeta);}
	public void remove(int aX, int aY, int aZ) {mMap.remove(key(aX, aY, aZ));}
	public boolean isEmpty() {return mMap.isEmpty();}
	public int size() {return mMap.size();}

	// Packing: one entry = (key<<16 | material), key<=20 bits, material<=16 bits -- the same layout the
	// 26.x branch's Codec/StreamCodec used, both to disk and over the wire.
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

	/** Name of the array inside the capability's own tag (the engine itself nests that tag under ForgeCaps). */
	public static final String NBT_KEY = "gt6_ore";
	public static final ResourceLocation ID = new ResourceLocation(gregapi.data.MD.GAPI.mID, "ore_map");

	public static final Capability<PrefixBlockOreMap> CAP = CapabilityManager.get(new CapabilityToken<PrefixBlockOreMap>() {});

	/** The carrier: one map per chunk; persistence is handled by the engine itself (ChunkSerializer, tag ForgeCaps). */
	private static final class Provider implements ICapabilitySerializable<CompoundTag> {
		private final PrefixBlockOreMap mData = new PrefixBlockOreMap();
		private final LazyOptional<PrefixBlockOreMap> mOptional = LazyOptional.of(() -> mData);

		@Override public <T> LazyOptional<T> getCapability(Capability<T> aCapability, Direction aSide) {return aCapability == CAP ? mOptional.cast() : LazyOptional.empty();}
		@Override public CompoundTag serializeNBT() {CompoundTag rNBT = new CompoundTag(); if (!mData.isEmpty()) rNBT.putLongArray(NBT_KEY, mData.pack()); return rNBT;}
		@Override public void deserializeNBT(CompoundTag aNBT) {mData.unpack(aNBT.getLongArray(NBT_KEY));}
	}

	/** The one subscription point for this carrier (same role attachments played on 26.x): the capability
	 *  is declared on the mod bus, attached to chunks on the Forge bus. */
	public static void register(IEventBus aModBus) {
		aModBus.addListener((RegisterCapabilitiesEvent aEvent) -> aEvent.register(PrefixBlockOreMap.class));
		MinecraftForge.EVENT_BUS.addGenericListener(LevelChunk.class, (AttachCapabilitiesEvent<LevelChunk> aEvent) -> aEvent.addCapability(ID, new Provider()));
	}

	/** The chunk's map if a carrier exists (a LevelChunk with the capability attached), else null. Reading never creates one. */
	public static PrefixBlockOreMap existing(ChunkAccess aChunk) {
		if (!(aChunk instanceof LevelChunk tChunk)) return null;
		return tChunk.getCapability(CAP).orElse(null);
	}
}

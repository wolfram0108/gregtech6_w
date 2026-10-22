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

package gregapi.tileentity;

import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Function;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;

/**
 * Another mod's capability on a block entity's side, as GT6 code meets it next to a GT6 block: the engine's handler wrapped once in
 * GT6's own adapter, and both kept until the engine invalidates that block's capability. One instance per channel (fluids, items):
 * the rules below were written for fluids and are the same for every channel, so they live here once.
 *
 * <p>Kept per position and side: one map lookup per call, nothing held past its block, never stale. Server thread only; any other
 * thread asks the engine directly, since the cache is not synchronised.
 */
public final class GT6ForeignCapabilityCache<H, A> {
	private final BlockCapability<H, Direction> mCapability;
	private final Function<H, A> mAdapt;
	private final Map<Level, Long2ObjectOpenHashMap<Entry<H, A>[]>> mLevels = new WeakHashMap<>();

	public GT6ForeignCapabilityCache(BlockCapability<H, Direction> aCapability, Function<H, A> aAdapt) {
		mCapability = aCapability;
		mAdapt = aAdapt;
	}

	/** The adapted capability of a block entity on one side; null where the block offers none or its provider fails. */
	@SuppressWarnings("unchecked")
	public A get(BlockEntity aTileEntity, byte aSide) {
		Level tLevel = aTileEntity.getLevel();
		if (tLevel == null || aTileEntity.isRemoved()) return null;
		if (!(tLevel instanceof ServerLevel tServer) || !tServer.getServer().isSameThread()) {
			try {
				H tCap = tLevel.getCapability(mCapability, aTileEntity.getBlockPos(), aTileEntity.getBlockState(), aTileEntity, gregapi.data.CS.FORGE_DIR[aSide]);
				return tCap == null ? null : mAdapt.apply(tCap);
			} catch (Throwable e) {return null;}
		}
		long tPos = aTileEntity.getBlockPos().asLong();
		int tSide = gregapi.data.CS.SIDES_VALID[aSide] ? aSide : 6;
		Long2ObjectOpenHashMap<Entry<H, A>[]> tMap = mLevels.computeIfAbsent(tLevel, k -> new Long2ObjectOpenHashMap<>());
		Entry<H, A>[] tSides = tMap.get(tPos);
		if (tSides == null) tMap.put(tPos, tSides = new Entry[7]);
		Entry<H, A> tEntry = tSides[tSide];
		if (tEntry == null) {
			Entry<H, A>[] tOwner = tSides;
			tSides[tSide] = tEntry = new Entry<>(this, tServer, aTileEntity.getBlockPos(), aSide, () -> drop(tMap, tPos, tOwner, tSide));
		}
		return tEntry.get();
	}

	private static <H, A> void drop(Long2ObjectOpenHashMap<Entry<H, A>[]> aMap, long aPos, Entry<H, A>[] aSides, int aSide) {
		aSides[aSide] = null;
		for (Entry<H, A> tEntry : aSides) if (tEntry != null) return;
		if (aMap.get(aPos) == aSides) aMap.remove(aPos);
	}

	private static final class Entry<H, A> {
		private final GT6ForeignCapabilityCache<H, A> mOwner;
		private final BlockCapabilityCache<H, Direction> mCache;
		private H mLast;
		private A mAdapted;

		private Entry(GT6ForeignCapabilityCache<H, A> aOwner, ServerLevel aLevel, BlockPos aPos, byte aSide, Runnable aDrop) {
			mOwner = aOwner;
			mCache = BlockCapabilityCache.create(aOwner.mCapability, aLevel, aPos, gregapi.data.CS.FORGE_DIR[aSide], () -> true, aDrop);
		}

		/** The same adapter object while the storage stays the same, so a delegator holding it can tell it still exists. */
		private A get() {
			H tCap;
			try {tCap = mCache.getCapability();} catch (Throwable e) {return null;}
			if (tCap != mLast) {mLast = tCap; mAdapted = tCap == null ? null : mOwner.mAdapt.apply(tCap);}
			return mAdapted;
		}
	}
}

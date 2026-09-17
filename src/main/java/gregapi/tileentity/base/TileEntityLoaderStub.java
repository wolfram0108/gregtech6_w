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

package gregapi.tileentity.base;

import com.mojang.serialization.MapCodec;

import gregapi.util.UT;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;

import static gregapi.data.CS.*;

/** A concrete stub neo creates on world-load for GT6 machines whose class comes from saved NBT, unavailable
 *  to BlockEntityType.create; it only captures raw NBT, and the registry replaces it with the real MTE on ChunkEvent.Load. */
public class TileEntityLoaderStub extends TileEntityBase01Root {
	public CompoundTag mLoadedNBT = null;

	public TileEntityLoaderStub(BlockPos aPos, BlockState aState) {super(F, aPos, aState);}

	@Override public String getTileEntityName() {return "gt.te.loader";}

	// Captures raw NBT since the stub can't run a concrete class's readFromNBT.
	// super.load still runs, since it parses Forge's own chunk data.
	@Override public void load(CompoundTag aNBT) {
		super.load(aNBT);
		mLoadedNBT = aNBT.copy();
	}

	/** A chunk can save before the stub's reconstruction catches up, and writing only id/x/y/z through the normal
	 *  GT6 NBT bridge would erase all MTE data forever; the stub instead round-trips its captured NBT unchanged. */
	@Override protected void saveAdditional(CompoundTag aNBT) {
		if (mLoadedNBT == null) {super.saveAdditional(aNBT); return;}
		aNBT.merge(mLoadedNBT);
	}

	/** Same transparent-carrier trick as getUpdateTag, but reconstruction here is deferred with a quota, so the stub
	 *  itself is often what's still in place when the chunk packet builds; it must hand over its captured identity alone. */
	@Override protected void writeMTEIdentity(CompoundTag aNBT) {
		if (mLoadedNBT == null) return;
		if (mLoadedNBT.contains(NBT_MTE_REG)) aNBT.putShort(NBT_MTE_REG, mLoadedNBT.getShort(NBT_MTE_REG));
		if (mLoadedNBT.contains(NBT_MTE_ID )) aNBT.putShort(NBT_MTE_ID , mLoadedNBT.getShort(NBT_MTE_ID ));
		// The registry name travels with the identity: the number is a local item index and means nothing to a client.
		String tName = mLoadedNBT.getString(NBT_MTE_REGNAME);
		if (tName != null && !tName.isEmpty()) aNBT.putString(NBT_MTE_REGNAME, tName);
		else if (mLoadedNBT.contains(NBT_MTE_REG)) gregapi.block.multitileentity.MultiTileEntityRegistry.writeRegistryName(aNBT, mLoadedNBT.getShort(NBT_MTE_REG));
	}
}

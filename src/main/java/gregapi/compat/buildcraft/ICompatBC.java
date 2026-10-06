/**
 * Copyright (c) 2019 Gregorius Techneticies
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

package gregapi.compat.buildcraft;

import gregapi.compat.ICompat;
import gregapi.tileentity.base.TileEntityBase01Root;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.NonNullSupplier;


public interface ICompatBC extends ICompat {
	/** Rotates a block that declares BuildCraft's rotation contract, as 1.7.10 Block.rotateBlock did for BuildCraft blocks. */
	public boolean rotateBlock(Level aWorld, BlockPos aPos, BlockState aState, Direction aSide);
	/** What a GT6 block offers for a capability BuildCraft asks (MJ receiver, work state), or null when nothing on that side;
	 *  TileEntityBase01Root caches it per side like its own channels. */
	public NonNullSupplier<Object> capability(TileEntityBase01Root aRoot, Capability<?> aCapability, Direction aSide);
	/** RF into a BuildCraft MJ receiver at BuildCraft's MJ per RF; the RF it took, or -1 when the block takes no MJ. */
	public long insertRF(BlockEntity aReceiver, byte aSide, long aRF);
}

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
 *
 * Modified in 2026 for the GregTech 6 NeoForge port
 * (https://github.com/wolfram0108/gregtech6_w): ported from Minecraft 1.7.10 / Forge
 * to Minecraft 26.1.2 / NeoForge.
 */

package gregapi.block;

import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.BlockGetter;

/**
 * @author Gregorius Techneticies
 * 
 * This Interface is used for MultiBlocks in order to provide "stupid" Blocks, which have no TileEntity with the Main TileEntity of a MultiBlock.
 * Do not implement this Interface for real TileEntity Blocks, just for Blocks which should not have an own TileEntity in MC, but which have a TileEntity registered inside a Custom Handler.
 */
public interface IBlockTileEntity {
	/** can return a TileEntity for this Block even if it has no actual one, in order to provide an Interface to the Main-Block of a MultiBlock Contraption. */
	public BlockEntity getTileEntity(BlockGetter aWorld, int aX, int aY, int aZ);
}

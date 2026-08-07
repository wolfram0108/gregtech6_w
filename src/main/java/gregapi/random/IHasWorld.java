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

package gregapi.random;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;

/**
 * @author Gregorius Techneticies
 * 
 * Contains simple Utility Functions based just on the World of the Implementor.
 */
public interface IHasWorld {
	public Level getWorld();
	
	public boolean isServerSide();
	public boolean isClientSide();
	
	public int rng(int aRange);
	public int getRandomNumber(int aRange);
	
	public BlockEntity getTileEntity(int aX, int aY, int aZ);
	public BlockEntity getTileEntity(BlockPos aCoords);
	
	public Block getBlock(int aX, int aY, int aZ);
	public Block getBlock(BlockPos aCoords);
	
	public byte getMetaData(int aX, int aY, int aZ);
	public byte getMetaData(BlockPos aCoords);
	
	public byte getLightLevel(int aX, int aY, int aZ);
	public byte getLightLevel(BlockPos aCoords);
	
	public boolean getOpacity(int aX, int aY, int aZ);
	public boolean getOpacity(BlockPos aCoords);
	
	public boolean getSky(int aX, int aY, int aZ);
	public boolean getSky(BlockPos aCoords);
	
	public boolean getRain(int aX, int aY, int aZ);
	public boolean getRain(BlockPos aCoords);
	
	public boolean getAir(int aX, int aY, int aZ);
	public boolean getAir(BlockPos aCoords);
	
	public Biome getBiome(int aX, int aZ);
	public Biome getBiome(BlockPos aCoords);
}

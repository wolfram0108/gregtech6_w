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

package gregtech.api.interfaces.tileentity;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.Container;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

@Deprecated
/** Required to exist in GT6 because Immersive Engineering crashes otherwise. Also there is that GT5U+GT6 Mod that basically needs this for Compat. */
public interface IHasWorldObjectAndCoords {
	public Level getWorld();
	public int getXCoord();
	public short getYCoord();
	public int getZCoord();
	public boolean isServerSide();
	public boolean isClientSide();
	public int getRandomNumber(int aRange);
	public BlockEntity getTileEntity(int aX, int aY, int aZ);
	public BlockEntity getTileEntityOffset(int aX, int aY, int aZ);
	public BlockEntity getTileEntityAtSide(byte aSide);
	public BlockEntity getTileEntityAtSideAndDistance(byte aSide, int aDistance);
	public Container getIInventory(int aX, int aY, int aZ);
	public Container getIInventoryOffset(int aX, int aY, int aZ);
	public Container getIInventoryAtSide(byte aSide);
	public Container getIInventoryAtSideAndDistance(byte aSide, int aDistance);
	public IFluidHandler getITankContainer(int aX, int aY, int aZ);
	public IFluidHandler getITankContainerOffset(int aX, int aY, int aZ);
	public IFluidHandler getITankContainerAtSide(byte aSide);
	public IFluidHandler getITankContainerAtSideAndDistance(byte aSide, int aDistance);
//  public IGregTechTileEntity getIGregTechTileEntity(int aX, int aY, int aZ);
//  public IGregTechTileEntity getIGregTechTileEntityOffset(int aX, int aY, int aZ);
//  public IGregTechTileEntity getIGregTechTileEntityAtSide(byte aSide);
//  public IGregTechTileEntity getIGregTechTileEntityAtSideAndDistance(byte aSide, int aDistance);
	public Block getBlock(int aX, int aY, int aZ);
	public Block getBlockOffset(int aX, int aY, int aZ);
	public Block getBlockAtSide(byte aSide);
	public Block getBlockAtSideAndDistance(byte aSide, int aDistance);
	public byte getMetaID(int aX, int aY, int aZ);
	public byte getMetaIDOffset(int aX, int aY, int aZ);
	public byte getMetaIDAtSide(byte aSide);
	public byte getMetaIDAtSideAndDistance(byte aSide, int aDistance);
	public byte getLightLevel(int aX, int aY, int aZ);
	public byte getLightLevelOffset(int aX, int aY, int aZ);
	public byte getLightLevelAtSide(byte aSide);
	public byte getLightLevelAtSideAndDistance(byte aSide, int aDistance);
	public boolean getOpacity(int aX, int aY, int aZ);
	public boolean getOpacityOffset(int aX, int aY, int aZ);
	public boolean getOpacityAtSide(byte aSide);
	public boolean getOpacityAtSideAndDistance(byte aSide, int aDistance);
	public boolean getSky(int aX, int aY, int aZ);
	public boolean getSkyOffset(int aX, int aY, int aZ);
	public boolean getSkyAtSide(byte aSide);
	public boolean getSkyAtSideAndDistance(byte aSide, int aDistance);
	public boolean getAir(int aX, int aY, int aZ);
	public boolean getAirOffset(int aX, int aY, int aZ);
	public boolean getAirAtSide(byte aSide);
	public boolean getAirAtSideAndDistance(byte aSide, int aDistance);
	public Biome getBiome();
	public Biome getBiome(int aX, int aZ);
	public int   getOffsetX(byte aSide, int aMultiplier);
	public short getOffsetY(byte aSide, int aMultiplier);
	public int   getOffsetZ(byte aSide, int aMultiplier);
	public boolean isDead();
	public void sendBlockEvent(byte aID, byte aValue);
	public long getTimer();
	public void setLightValue(byte aLightValue);
	public boolean isInvalidTileEntity();
	public boolean openGUI(Player aPlayer, int aID);
	public boolean openGUI(Player aPlayer);
}

/**
 * Copyright (c) 2021 GregTech-6 Team
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
import net.minecraft.world.inventory.AbstractContainerMenu;
import gregapi.util.WD;

import static gregapi.data.CS.*;

import gregapi.tileentity.delegate.DelegatorTileEntity;
import gregapi.tileentity.delegate.ITileEntityCanDelegate;
import gregapi.tileentity.delegate.ITileEntityDelegating;
import gregapi.util.UT;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.Container;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraftforge.fluids.capability.IFluidHandler;

/**
 * @author Gregorius Techneticies
 * 
 * Contains simple Utility Functions based on the In-World-Coordinates
 */
public class WorldAndCoords implements IHasWorldAndCoords, Comparable<WorldAndCoords> {
	public final int mX, mY, mZ;
	public final Level mWorld;
	
	public WorldAndCoords(Level aWorld, int aX, int aY, int aZ) {mWorld = aWorld; mX = aX; mY = aY; mZ = aZ;}
	public WorldAndCoords(Level aWorld, BlockPos aCoords) {mWorld = aWorld; mX = aCoords.getX(); mY = aCoords.getY(); mZ = aCoords.getZ();}
	public WorldAndCoords(BlockEntity aTileEntity) {mWorld = aTileEntity.getLevel(); mX = aTileEntity.getBlockPos().getX(); mY = aTileEntity.getBlockPos().getY(); mZ = aTileEntity.getBlockPos().getZ();}
	
	@Override public Level getWorld() {return mWorld;}
	@Override public int getX() {return mX;}
	@Override public int getY() {return mY;}
	@Override public int getZ() {return mZ;}
	@Override public int getOffsetX (byte aSide) {return mX + OFFX[aSide];}
	@Override public int getOffsetY (byte aSide) {return mY + OFFY[aSide];}
	@Override public int getOffsetZ (byte aSide) {return mZ + OFFZ[aSide];}
	@Override public int getOffsetX (byte aSide, int aMultiplier) {return mX + OFFX[aSide] * aMultiplier;}
	@Override public int getOffsetY (byte aSide, int aMultiplier) {return mY + OFFY[aSide] * aMultiplier;}
	@Override public int getOffsetZ (byte aSide, int aMultiplier) {return mZ + OFFZ[aSide] * aMultiplier;}
	@Override public int getOffsetXN(byte aSide) {return mX - OFFX[aSide];}
	@Override public int getOffsetYN(byte aSide) {return mY - OFFY[aSide];}
	@Override public int getOffsetZN(byte aSide) {return mZ - OFFZ[aSide];}
	@Override public int getOffsetXN(byte aSide, int aMultiplier) {return mX - OFFX[aSide] * aMultiplier;}
	@Override public int getOffsetYN(byte aSide, int aMultiplier) {return mY - OFFY[aSide] * aMultiplier;}
	@Override public int getOffsetZN(byte aSide, int aMultiplier) {return mZ - OFFZ[aSide] * aMultiplier;}
	@Override public BlockPos getCoords() {return new BlockPos(mX, mY, mZ);}
	@Override public BlockPos getOffset (byte aSide, int aMultiplier) {return new BlockPos(getOffsetX (aSide, aMultiplier), getOffsetY (aSide, aMultiplier), getOffsetZ (aSide, aMultiplier));}
	@Override public BlockPos getOffsetN(byte aSide, int aMultiplier) {return new BlockPos(getOffsetXN(aSide, aMultiplier), getOffsetYN(aSide, aMultiplier), getOffsetZN(aSide, aMultiplier));}
	@Override public boolean isServerSide() {return mWorld == null ? net.minecraftforge.fml.util.thread.EffectiveSide.get().isServer() : !mWorld.isClientSide();}
	@Override public boolean isClientSide() {return mWorld == null ? net.minecraftforge.fml.util.thread.EffectiveSide.get().isClient() :  mWorld.isClientSide();}
	@Override public int rng(int aRange) {return RNGSUS.nextInt(aRange);}
	@Override public int getRandomNumber(int aRange) {return RNGSUS.nextInt(aRange);}
	@Override public BlockEntity getTileEntity   (int aX, int aY, int aZ) {return mWorld==null?null:WD.te(mWorld, aX, aY, aZ, T);}
	@Override public Block getBlock             (int aX, int aY, int aZ) {return mWorld==null?NB:WD.block(mWorld, aX, aY, aZ);}
	@Override public byte getMetaData           (int aX, int aY, int aZ) {return mWorld==null?0:UT.Code.bind4(WD.meta(mWorld, aX, aY, aZ));}
	@Override public byte getLightLevel         (int aX, int aY, int aZ) {return mWorld==null?0:UT.Code.bind4((long)WD.lightBrightness(mWorld, aX, aY, aZ)*15);}
	@Override public boolean getOpacity         (int aX, int aY, int aZ) {return mWorld!=null&&WD.opaque(WD.block(mWorld, aX, aY, aZ));}
	@Override public boolean getSky             (int aX, int aY, int aZ) {return mWorld==null||WD.canSeeSky(mWorld, aX, aY, aZ);}
	@Override public boolean getRain            (int aX, int aY, int aZ) {return mWorld==null||WD.precipitationHeight(mWorld, aX, aZ) <= aY;}
	@Override public boolean getAir             (int aX, int aY, int aZ) {return mWorld==null||WD.air(mWorld, aX, aY, aZ);}
	@Override public Biome getBiome() {return getBiome(mX, mZ);}
	@Override public Biome getBiome      (int aX, int aZ) {return mWorld==null?null:WD.biome(mWorld, aX, aZ);}
	@Override public Biome getBiome      (BlockPos aCoords) {return mWorld==null?null:WD.biome(mWorld, aCoords.getX(), aCoords.getZ());}
	@Override public BlockEntity getTileEntity   (BlockPos aCoords) {return mWorld==null?null:WD.te(mWorld, aCoords.getX(), aCoords.getY(), aCoords.getZ(), T);}
	@Override public Block getBlock             (BlockPos aCoords) {return mWorld==null?NB:WD.block(mWorld, aCoords.getX(), aCoords.getY(), aCoords.getZ());}
	@Override public byte getMetaData           (BlockPos aCoords) {return mWorld==null?0:UT.Code.bind4(WD.meta(mWorld, aCoords.getX(), aCoords.getY(), aCoords.getZ()));}
	@Override public byte getLightLevel         (BlockPos aCoords) {return mWorld==null?0:UT.Code.bind4((long)WD.lightBrightness(mWorld, aCoords.getX(), aCoords.getY(), aCoords.getZ())*15);}
	@Override public boolean getOpacity         (BlockPos aCoords) {return mWorld!=null&&WD.opaque(WD.block(mWorld, aCoords.getX(), aCoords.getY(), aCoords.getZ()));}
	@Override public boolean getSky             (BlockPos aCoords) {return mWorld==null||WD.canSeeSky(mWorld, aCoords.getX(), aCoords.getY(), aCoords.getZ());}
	@Override public boolean getRain            (BlockPos aCoords) {return mWorld==null||WD.precipitationHeight(mWorld, aCoords.getX(), aCoords.getZ()) <= aCoords.getY();}
	@Override public boolean getAir             (BlockPos aCoords) {return mWorld==null||WD.air(mWorld, aCoords.getX(), aCoords.getY(), aCoords.getZ());}
	@Override public Block getBlockOffset(int aX, int aY, int aZ) {return getBlock(mX+aX, mY+aY, mZ+aZ);}
	@Override public Block getBlockAtSide(byte aSide) {return getBlockAtSideAndDistance(aSide, 1);}
	@Override public Block getBlockAtSideAndDistance(byte aSide, int aDistance) {return getBlock(getOffsetX(aSide, aDistance), getOffsetY(aSide, aDistance), getOffsetZ(aSide, aDistance));}
	@Override public byte getMetaDataOffset(int aX, int aY, int aZ) {return getMetaData(mX+aX, mY+aY, mZ+aZ);}
	@Override public byte getMetaDataAtSide(byte aSide) {return getMetaDataAtSideAndDistance(aSide, 1);}
	@Override public byte getMetaDataAtSideAndDistance(byte aSide, int aDistance) {return getMetaData(getOffsetX(aSide, aDistance), getOffsetY(aSide, aDistance), getOffsetZ(aSide, aDistance));}
	@Override public byte getLightLevelOffset(int aX, int aY, int aZ) {return getLightLevel(mX+aX, mY+aY, mZ+aZ);}
	@Override public byte getLightLevelAtSide(byte aSide) {return getLightLevelAtSideAndDistance(aSide, 1);}
	@Override public byte getLightLevelAtSideAndDistance(byte aSide, int aDistance) {return getLightLevel(getOffsetX(aSide, aDistance), getOffsetY(aSide, aDistance), getOffsetZ(aSide, aDistance));}
	@Override public boolean getOpacityOffset(int aX, int aY, int aZ) {return getOpacity(mX+aX, mY+aY, mZ+aZ);}
	@Override public boolean getOpacityAtSide(byte aSide) {return getOpacityAtSideAndDistance(aSide, 1);}
	@Override public boolean getOpacityAtSideAndDistance(byte aSide, int aDistance) {return getOpacity(getOffsetX(aSide, aDistance), getOffsetY(aSide, aDistance), getOffsetZ(aSide, aDistance));}
	@Override public boolean getRainOffset(int aX, int aY, int aZ) {return getRain(mX+aX, mY+aY, mZ+aZ);}
	@Override public boolean getRainAtSide(byte aSide) {return getRainAtSideAndDistance(aSide, 1);}
	@Override public boolean getRainAtSideAndDistance(byte aSide, int aDistance) {return getRain(getOffsetX(aSide, aDistance), getOffsetY(aSide, aDistance), getOffsetZ(aSide, aDistance));}
	@Override public boolean getSkyOffset(int aX, int aY, int aZ) {return getSky(mX+aX, mY+aY, mZ+aZ);}
	@Override public boolean getSkyAtSide(byte aSide) {return getSkyAtSideAndDistance(aSide, 1);}
	@Override public boolean getSkyAtSideAndDistance(byte aSide, int aDistance) {return getSky(getOffsetX(aSide, aDistance), getOffsetY(aSide, aDistance), getOffsetZ(aSide, aDistance));}
	@Override public boolean getAirOffset(int aX, int aY, int aZ) {return getAir(mX+aX, mY+aY, mZ+aZ);}
	@Override public boolean getAirAtSide(byte aSide) {return getAirAtSideAndDistance(aSide, 1);}
	@Override public boolean getAirAtSideAndDistance(byte aSide, int aDistance) {return getAir(getOffsetX(aSide, aDistance), getOffsetY(aSide, aDistance), getOffsetZ(aSide, aDistance));}
	@Override public BlockEntity getTileEntityOffset(int aX, int aY, int aZ) {return getTileEntity(mX+aX, mY+aY, mZ+aZ);}
	@Override public BlockEntity getTileEntityAtSideAndDistance(byte aSide, int aDistance) {return getTileEntity(getOffsetX(aSide, aDistance), getOffsetY(aSide, aDistance), getOffsetZ(aSide, aDistance));}
	@Override public DelegatorTileEntity<BlockEntity         > getAdjacentTileEntity     (byte aSide) {return getAdjacentTileEntity(aSide, T, F);}
	@Override public DelegatorTileEntity<Container         > getAdjacentInventory      (byte aSide) {return getAdjacentInventory(aSide, T, F);}
	@Override public DelegatorTileEntity<WorldlyContainer    > getAdjacentSidedInventory (byte aSide) {return getAdjacentSidedInventory(aSide, T, F);}
	@Override public DelegatorTileEntity<IFluidHandler      > getAdjacentTank           (byte aSide) {return getAdjacentTank(aSide, T, F);}
	@Override public DelegatorTileEntity<Container         > getAdjacentInventory      (byte aSide, boolean aAllowDelegates, boolean aNotConnectToDelegators) {DelegatorTileEntity<BlockEntity> tDelegator = getAdjacentTileEntity(aSide, aAllowDelegates, aNotConnectToDelegators); return new DelegatorTileEntity<>(tDelegator.mTileEntity instanceof Container      ?(Container        )tDelegator.mTileEntity:null, tDelegator);}
	@Override public DelegatorTileEntity<WorldlyContainer    > getAdjacentSidedInventory (byte aSide, boolean aAllowDelegates, boolean aNotConnectToDelegators) {DelegatorTileEntity<BlockEntity> tDelegator = getAdjacentTileEntity(aSide, aAllowDelegates, aNotConnectToDelegators); return new DelegatorTileEntity<>(tDelegator.mTileEntity instanceof WorldlyContainer ?(WorldlyContainer   )tDelegator.mTileEntity:null, tDelegator);}
	@Override public DelegatorTileEntity<IFluidHandler      > getAdjacentTank           (byte aSide, boolean aAllowDelegates, boolean aNotConnectToDelegators) {DelegatorTileEntity<BlockEntity> tDelegator = getAdjacentTileEntity(aSide, aAllowDelegates, aNotConnectToDelegators); return new DelegatorTileEntity<>(tDelegator.mTileEntity instanceof IFluidHandler   ?(IFluidHandler     )tDelegator.mTileEntity:null, tDelegator);}
	
	@Override
	public DelegatorTileEntity<BlockEntity> getAdjacentTileEntity(byte aSide, boolean aAllowDelegates, boolean aNotConnectToDelegators) {
		BlockEntity tTileEntity = getTileEntityAtSideAndDistance(aSide, 1);
		if (tTileEntity == null) return new DelegatorTileEntity<>(null, mWorld, getOffsetX(aSide), getOffsetY(aSide), getOffsetZ(aSide), OPOS[aSide]);
		if (aNotConnectToDelegators && tTileEntity instanceof ITileEntityCanDelegate && ((ITileEntityCanDelegate)tTileEntity).isExtender(aSide)) return new DelegatorTileEntity<>(null, mWorld, getOffsetX(aSide), getOffsetY(aSide), getOffsetZ(aSide), OPOS[aSide]);
		if (aAllowDelegates && tTileEntity instanceof ITileEntityDelegating) return ((ITileEntityDelegating)tTileEntity).getDelegateTileEntity(OPOS[aSide]);
		return new DelegatorTileEntity<>(tTileEntity, tTileEntity.getLevel(), tTileEntity.getBlockPos().getX(), tTileEntity.getBlockPos().getY(), tTileEntity.getBlockPos().getZ(), OPOS[aSide]);
	}
	
	@Override
	public boolean hasRedstoneIncoming() {
		for (byte tSide : ALL_SIDES_VALID) if (getRedstoneIncoming(tSide) > 0) return T;
		return F;
	}
	
	@Override
	public byte getRedstoneIncoming(byte aSide) {
		if (SIDES_INVALID[aSide]) {
			byte rRedstone = 0;
			for (byte tSide : ALL_SIDES_VALID) {
				rRedstone = (byte)Math.max(rRedstone, mWorld.getSignal(new BlockPos(getOffsetX(tSide), getOffsetY(tSide), getOffsetZ(tSide)), FORGE_DIR[tSide]));
				if (rRedstone >= 15) return 15;
			}
			return rRedstone;
		}
		return UT.Code.bind4(mWorld.getSignal(new BlockPos(getOffsetX(aSide), getOffsetY(aSide), getOffsetZ(aSide)), FORGE_DIR[aSide]));
	}
	
	@Override
	public byte getComparatorIncoming(byte aSide) {
		// F-block: Block.hasComparatorInputOverride/getComparatorInputOverride(world,x,y,z,side) ->
		// BlockState.hasAnalogOutputSignal/getAnalogOutputSignal(Level,BlockPos,Direction) (BlockBehaviour:628/632).
		BlockPos tPos = new BlockPos(getOffsetX(aSide), getOffsetY(aSide), getOffsetZ(aSide));
		net.minecraft.world.level.block.state.BlockState tState = gregapi.util.WD.state(mWorld, tPos);
		return tState.hasAnalogOutputSignal()?UT.Code.bind4(tState.getAnalogOutputSignal(mWorld, tPos)):getRedstoneIncoming(aSide);
	}
	
	@Override public boolean equals(Object aObject) {return aObject instanceof WorldAndCoords && ((WorldAndCoords)aObject).mWorld == mWorld && ((WorldAndCoords)aObject).mX == mX && ((WorldAndCoords)aObject).mY == mY && ((WorldAndCoords)aObject).mZ == mZ;}
	@Override public int hashCode() {return mX + mZ << 8 + mY << 16;}
	@Override public int compareTo(WorldAndCoords aObject) {return mY == aObject.mY ? mZ == aObject.mZ ? mX - aObject.mX : mZ - aObject.mZ : mY - aObject.mY;}
	@Override public String toString() {return "Pos{x=" + mX + ", y=" + mY + ", z=" + mZ + '}';}
}

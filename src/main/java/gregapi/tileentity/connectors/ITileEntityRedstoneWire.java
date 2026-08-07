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

package gregapi.tileentity.connectors;

import static gregapi.data.CS.*;

import gregapi.code.HashSetNoNulls;
import gregapi.tileentity.delegate.DelegatorTileEntity;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * @author Gregorius Techneticies
 */
public interface ITileEntityRedstoneWire extends ITileEntityConnector {
	public static final long MAX_RANGE = Integer.MAX_VALUE;
	
	public boolean canEmitRedstoneToWire(byte aSide, int aRedstoneID);
	public boolean canAcceptRedstoneFromWire(byte aSide, int aRedstoneID);
	
	public boolean updateRedstone(int aRedstoneID);
	public long getRedstoneMinusLoss(byte aSide, int aRedstoneID);
	public long getRedstoneValue(byte aSide, int aRedstoneID);
	public long getRedstoneLoss(int aRedstoneID);
	
	public static class Util {
		public static void doRedstoneUpdate(ITileEntityRedstoneWire aTileEntity, int aRedstoneID) {
			HashSetNoNulls<ITileEntityRedstoneWire> tSetUpdating = new HashSetNoNulls<>(F, aTileEntity), tSetNext = new HashSetNoNulls<>();
			while (!tSetUpdating.isEmpty()) {
				for (ITileEntityRedstoneWire tTileEntity : tSetUpdating) for (byte tSide : ALL_SIDES_VALID) if (tTileEntity.canEmitRedstoneToWire(tSide, aRedstoneID)) {
					DelegatorTileEntity<BlockEntity> tDelegator = tTileEntity.getAdjacentTileEntity(tSide);
					if (tDelegator.mTileEntity instanceof ITileEntityRedstoneWire && ((ITileEntityRedstoneWire)tDelegator.mTileEntity).canAcceptRedstoneFromWire(tDelegator.mSideOfTileEntity, aRedstoneID) && ((ITileEntityRedstoneWire)tDelegator.mTileEntity).updateRedstone(aRedstoneID)) {
						tSetNext.add((ITileEntityRedstoneWire)tDelegator.mTileEntity);
					}
				}
				tSetUpdating.clear();
				tSetUpdating.addAll(tSetNext);
				tSetNext.clear();
			}
		}
	}
}

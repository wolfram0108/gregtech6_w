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

package gregapi.event;

import java.util.ArrayList;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.Level;

public class BlockScanningEvent extends net.neoforged.neoforge.event.level.LevelEvent implements net.neoforged.bus.api.ICancellableEvent {
	
	public final Player mPlayer;
	public final int mX, mY, mZ, mScanLevel;
	public final ArrayList<String> mList;
	public final byte mSide;
	public final float mClickX, mClickY, mClickZ;
	public final BlockEntity mTileEntity;
	public final Block mBlock;
	
	/**
	 * used to determine the amount of Energy this Scan is costing.
	 */
	public long mEUCost = 0;
	
	public BlockScanningEvent(Level aWorld, Player aPlayer, int aX, int aY, int aZ, byte aSide, int aScanLevel, Block aBlock, BlockEntity aTileEntity, ArrayList<String> aList, float aClickX, float aClickY, float aClickZ) {
		super(aWorld);
		mPlayer = aPlayer;
		mScanLevel = aScanLevel;
		mTileEntity = aTileEntity;
		mBlock = aBlock;
		mList = aList;
		mSide = aSide;
		mX = aX;
		mY = aY;
		mZ = aZ;
		mClickX = aClickX;
		mClickY = aClickY;
		mClickZ = aClickZ;
	}
}

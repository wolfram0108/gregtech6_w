/**
 * Copyright (c) 2025 GregTech-6 Team
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

package gregtech.items.behaviors;
import static gregapi.data.CS.FORGE_DIR;

import net.minecraft.core.BlockPos;

import gregapi.block.IBlockDecolorable;
import gregapi.code.ItemNBT;
import gregapi.data.CS.BlocksGT;
import gregapi.data.CS.SFX;
import gregapi.data.LH;
import gregapi.item.multiitem.MultiItem;
import gregapi.item.multiitem.behaviors.IBehavior.AbstractBehaviorDefault;
import gregapi.tileentity.ITileEntityDecolorable;
import gregapi.tileentity.delegate.DelegatorTileEntity;
import gregapi.util.ST;
import gregapi.util.UT;
import gregapi.util.WD;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.Level;

import java.util.List;

import static gregapi.data.CS.F;
import static gregapi.data.CS.T;

public class Behavior_Spray_Color_Remover extends AbstractBehaviorDefault {
	private final ItemStack mEmpty, mUsed, mFull;
	private final long mUses;
	
	public Behavior_Spray_Color_Remover(ItemStack aEmpty, ItemStack aUsed, ItemStack aFull, long aUses) {
		mEmpty = aEmpty;
		mUsed = aUsed;
		mFull = aFull;
		mUses = aUses * 10;
	}
	
	@Override
	public boolean onItemUseFirst(MultiItem aItem, ItemStack aStack, Player aPlayer, Level aWorld, int aX, int aY, int aZ, byte aSide, float hitX, float hitY, float hitZ) {
		if (aWorld.isClientSide() || aStack.getCount() != 1) return F;
		
		boolean rOutput = F;
		
		if (!(aPlayer).mayUseItemAt(new BlockPos(aX, aY, aZ), FORGE_DIR[aSide], aStack)) return F;
		
		CompoundTag tNBT = ItemNBT.get(aStack);
		if (tNBT == null) tNBT = UT.NBT.make();
		long tUses = tNBT.getLongOr("gt.remaining", 0L);
		
		if (ST.equal(aStack, mFull, T)) {
			ST.setItem(aStack, mUsed.getItem());
			ST.meta_(aStack, ST.meta_(mUsed));
			tUses = mUses;
		}
		if (ST.equal(aStack, mUsed, T)) {
			if (decolorize(aWorld, aX, aY, aZ, aSide)) {
				UT.Sounds.send(SFX.IC_SPRAY, aWorld, aX, aY, aZ);
				if (!UT.Entities.hasInfiniteItems(aPlayer)) tUses-=10;
				rOutput = T;
			}
		}
		tNBT.remove("gt.remaining");
		if (tUses > 0) UT.NBT.setNumber(tNBT, "gt.remaining", tUses);
		UT.NBT.set(aStack, tNBT);
		
		if (tUses <= 0) {
			if (mEmpty == null) {
				aStack.setCount(aStack.getCount()-1);
			} else {
				ST.setItem(aStack, mEmpty.getItem());
				ST.meta_(aStack, ST.meta_(mEmpty));
			}
		}
		return rOutput;
	}
	
	private static boolean decolorize(Level aWorld, int aX, int aY, int aZ, byte aSide) {
		DelegatorTileEntity<BlockEntity> aDelegator = WD.te(aWorld, aX, aY, aZ, aSide, T);
		if (aDelegator.mTileEntity instanceof ITileEntityDecolorable) return ((ITileEntityDecolorable)aDelegator.mTileEntity).removePaint(aDelegator.mSideOfTileEntity);
		Block aBlock = aDelegator.getBlock();
		if (aBlock instanceof IBlockDecolorable) return ((IBlockDecolorable)aBlock).removePaint(aWorld, aDelegator.mX, aDelegator.mY, aDelegator.mZ, aDelegator.mSideOfTileEntity);
		// F4-flatten: 1.7.10 сравнивал с блоком-семьёй (stained_glass = все 16 оттенков), в neo семья расщеплена —
		// сравниваем с ГЛАВОЙ семьи через центр CS.Flattened, иначе краска смывается только с белого варианта.
		Block tHead = gregapi.data.CS.Flattened.headOf(aBlock);
		if (aBlock == Blocks.WHITE_TERRACOTTA          || tHead == Blocks.WHITE_TERRACOTTA         ) return aDelegator.setBlock(Blocks.TERRACOTTA);
		if (aBlock == Blocks.WHITE_STAINED_GLASS_PANE  || tHead == Blocks.WHITE_STAINED_GLASS_PANE ) return aDelegator.setBlock(Blocks.GLASS_PANE);
		if (aBlock == Blocks.WHITE_STAINED_GLASS       || tHead == Blocks.WHITE_STAINED_GLASS      ) return aDelegator.setBlock(Blocks.GLASS);
		if (aBlock == BlocksGT.Grass) return aDelegator.setBlock(Blocks.GRASS_BLOCK);
		return F;
	}
	
	static {
		LH.add("gt.behaviour.paintremoverspray.tooltip", "Can Decolor things");
		LH.add("gt.behaviour.paintremoverspray.uses", "Remaining Uses:");
		LH.add("gt.behaviour.unstackable", "Not usable when stacked!");
	}
	
	@Override
	public List<String> getAdditionalToolTips(MultiItem aItem, List<String> aList, ItemStack aStack) {
		aList.add(LH.get("gt.behaviour.paintremoverspray.tooltip"));
		CompoundTag tNBT = ItemNBT.get(aStack);
		long tRemaining = (ST.equal(aStack, mFull, T)?mUses:tNBT==null?0:tNBT.getLongOr("gt.remaining", 0L));
		aList.add(LH.get("gt.behaviour.paintremoverspray.uses") + " " + (tRemaining / 10) + "." + (tRemaining % 10));
		aList.add(LH.get("gt.behaviour.unstackable"));
		return aList;
	}
}

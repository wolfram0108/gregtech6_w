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

package gregtech.items.behaviors;

import static gregapi.data.CS.*;

import java.util.List;

import gregapi.code.ItemNBT;
import gregapi.data.LH;
import gregapi.item.multiitem.MultiItem;
import gregapi.item.multiitem.behaviors.IBehavior;
import gregapi.item.multiitem.behaviors.IBehavior.AbstractBehaviorDefault;
import gregapi.tileentity.ITileEntityKeyInteractable;
import gregapi.tileentity.delegate.DelegatorTileEntity;
import gregapi.util.UT;
import gregapi.util.WD;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.Level;

public class Behavior_Key extends AbstractBehaviorDefault {
	public static final IBehavior<MultiItem> INSTANCE = new Behavior_Key();
	
	@Override
	public boolean onItemUseFirst(MultiItem aItem, ItemStack aStack, Player aPlayer, Level aWorld, int aX, int aY, int aZ, byte aSide, float hitX, float hitY, float hitZ) {
		if (aWorld.isClientSide()) return F;
		
		DelegatorTileEntity<BlockEntity> aTileEntity = WD.te(aWorld, aX, aY, aZ, aSide, T);
		if (aTileEntity.mTileEntity instanceof ITileEntityKeyInteractable) {
			CompoundTag tNBT = UT.NBT.getNBT(aStack);
			long tKeyID = tNBT.getLongOr(NBT_KEY, 0L);
			if (tKeyID != 0) return ((ITileEntityKeyInteractable)aTileEntity.mTileEntity).useKey(aPlayer, aSide, hitX, hitY, hitZ, tKeyID);
			tKeyID = ((ITileEntityKeyInteractable)aTileEntity.mTileEntity).getKeyID();
			if (tKeyID == 0) {
				tKeyID = 1+Math.max(RNGSUS.nextInt(1000000), System.nanoTime());
				UT.NBT.set(aStack, UT.NBT.setNumber(tNBT, NBT_KEY, tKeyID));
				return ((ITileEntityKeyInteractable)aTileEntity.mTileEntity).useKey(aPlayer, aSide, hitX, hitY, hitZ, tKeyID);
			}
			if (((ITileEntityKeyInteractable)aTileEntity.mTileEntity).canCloneKey(aPlayer, aSide, hitX, hitY, hitZ)) {
				UT.NBT.set(aStack, UT.NBT.setNumber(tNBT, NBT_KEY, tKeyID));
				return T;
			}
		}
		return F;
	}
	
	static {LH.add("gt.behaviour.key", "Can open certain regular Locks");}
	
	@Override
	public List<String> getAdditionalToolTips(MultiItem aItem, List<String> aList, ItemStack aStack) {
		aList.add(LH.get("gt.behaviour.key"));
		CompoundTag tNBT = ItemNBT.get(aStack);
		if (tNBT != null && tNBT.contains(NBT_KEY)) aList.add("Key ID: " + UT.Code.makeString(tNBT.getLongOr(NBT_KEY, 0L))); else aList.add("*BLANK*");
		return aList;
	}
}

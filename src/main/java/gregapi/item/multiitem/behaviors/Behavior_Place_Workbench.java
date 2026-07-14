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
 */

package gregapi.item.multiitem.behaviors;

import net.minecraft.core.BlockPos;

import static gregapi.data.CS.*;

import gregapi.data.OD;
import gregapi.item.multiitem.MultiItem;
import gregapi.item.multiitem.behaviors.IBehavior.AbstractBehaviorDefault;
import gregapi.util.ST;
import gregapi.util.UT;
import gregapi.util.WD;
import net.minecraft.world.level.block.Block;
import gregapi.block.Material;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class Behavior_Place_Workbench extends AbstractBehaviorDefault {
	public static final Behavior_Place_Workbench INSTANCE = new Behavior_Place_Workbench();
	
	@Override
	public boolean onItemUse(MultiItem aItem, ItemStack aStack, Player aPlayer, Level aWorld, int aX, int aY, int aZ, byte aSide, float aHitX, float aHitY, float aHitZ) {
		if (aWorld.isClientSide() || aPlayer == null || !(aPlayer).mayUseItemAt(new BlockPos(aX, aY, aZ), FORGE_DIR[aSide], aStack)) return F;
		
		Block aBlock = WD.block(aWorld, aX, aY, aZ);
		// Don't place Workbenches on Wood or Plants, since this Class is supposed to be used by Axes and Saws.
		if (WD.getMaterial(aBlock) == Material.wood || WD.getMaterial(aBlock) == Material.leaves || WD.getMaterial(aBlock) == Material.plants || WD.getMaterial(aBlock) == Material.vine || WD.getMaterial(aBlock) == Material.gourd || WD.getMaterial(aBlock) == Material.cactus) return F;
		if (WD.wood(aBlock, aWorld, aX, aY, aZ) || WD.leaves(aBlock, aWorld, aX, aY, aZ)) return F;
		// Scan Inventory for suitable Workbenches.
		for (int i = 0; i < net.minecraft.world.entity.player.Inventory.INVENTORY_SIZE; i++) {
			ItemStack tStack = aPlayer.getInventory().getItem(net.minecraft.world.entity.player.Inventory.INVENTORY_SIZE-i-1);
			if (!OD.craftingWorkBench.is(tStack)) continue;
			
			int tOldSize = tStack.getCount();
			if (UT.tryPlaceItemIntoWorld(tStack, aPlayer, aWorld, aX, aY, aZ, aSide, aHitX, aHitY, aHitZ)) {
				if (UT.Entities.hasInfiniteItems(aPlayer)) {
					tStack.setCount(tOldSize);
				} else {
					ST.use(aPlayer, T, tStack, 0);
				}
				return T;
			}
			return F;
		}
		return F;
	}
}

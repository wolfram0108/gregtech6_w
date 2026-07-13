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

import static gregapi.data.CS.*;

import gregapi.block.IPrefixBlock;
import gregapi.data.MD;
import gregapi.data.TD;
import gregapi.item.multiitem.MultiItem;
import gregapi.item.multiitem.behaviors.IBehavior.AbstractBehaviorDefault;
import gregapi.util.OM;
import gregapi.util.ST;
import gregapi.util.UT;
import gregapi.util.WD;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.InfestedBlock;
import net.minecraft.world.level.block.EntityBlock;
import gregapi.block.Material;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class Behavior_Plug_Leak extends AbstractBehaviorDefault {
	public static final Behavior_Plug_Leak INSTANCE = new Behavior_Plug_Leak();
	
	@Override
	public boolean onItemUse(MultiItem aItem, ItemStack aStack, Player aPlayer, Level aWorld, int aX, int aY, int aZ, byte aSide, float aHitX, float aHitY, float aHitZ) {
		if (aWorld.isClientSide() || aPlayer == null || !WD.mayEdit(aPlayer, aX, aY, aZ, aSide, aStack)) return F;
		for (byte tSide : ALL_SIDES) {
			// Only place right next to Liquids or inside of Liquids.
			if (!WD.liquid(WD.block(aWorld, aX+OFFX[aSide]+OFFX[tSide], aY+OFFY[aSide]+OFFY[tSide], aZ+OFFZ[aSide]+OFFZ[tSide]))) continue;
			// Scan Inventory for suitable Items.
			for (int i = 0; i < Inventory.INVENTORY_SIZE; i++) {
				ItemStack tStack = aPlayer.getInventory().getItem(Inventory.INVENTORY_SIZE-i-1);
				if (ST.invalid(tStack)) continue;
				Block tBlock = ST.block(tStack);
				// The Block has to be Opaque to ensure the Leak is plugged.
				if (tBlock == NB || !WD.opaque(tBlock)) continue;
				// No Bedrock, Obsidian or Black Granite!
				if (WD.bedrock(tBlock) || tBlock.getHarvestLevel(ST.meta(tStack) & 15) >= 3) continue;
				// Don't use any PrefixBlocks, TileEntities or Silverfish Blocks.
				if (tBlock instanceof IPrefixBlock || tBlock instanceof EntityBlock || tBlock instanceof InfestedBlock) continue;
				// Only use Blocks that are typically mined.
				if (WD.getMaterial(tBlock) != Material.rock && WD.getMaterial(tBlock) != Material.ground && WD.getMaterial(tBlock) != Material.sand && WD.getMaterial(tBlock) != Material.clay) continue;
				// Don't use frikkin Ore Blocks or Storage Blocks for this!
				if (OM.prefixcontainsany(OM.anydata(tStack), TD.Prefix.ORE, TD.Prefix.STORAGE_BASED)) continue;
				// No Thaumcraft Blocks!
				if (MD.TC.owns(tBlock)) continue;
				
				int tOldSize = tStack.getCount();
				if (tStack.tryPlaceItemIntoWorld(aPlayer, aWorld, aX, aY, aZ, aSide, aHitX, aHitY, aHitZ)) {
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
		return F;
	}
}

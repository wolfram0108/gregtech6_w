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

package gregtech.items.behaviors;

import net.minecraft.core.BlockPos;

import static gregapi.data.CS.*;

import java.util.List;

import gregapi.block.metatype.BlockStones;
import gregapi.code.ItemNBT;
import gregapi.data.CS.BlocksGT;
import gregapi.data.IL;
import gregapi.data.LH;
import gregapi.item.multiitem.MultiItem;
import gregapi.item.multiitem.MultiItemTool;
import gregapi.item.multiitem.behaviors.IBehavior.AbstractBehaviorDefault;
import gregapi.util.ST;
import gregapi.util.UT;
import gregapi.util.WD;
import gregapi.worldgen.StoneLayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;

public class Behavior_Place_Dynamite extends AbstractBehaviorDefault {
	public static final Behavior_Place_Dynamite INSTANCE = new Behavior_Place_Dynamite();
	
	@Override
	public boolean onItemUseFirst(MultiItem aItem, ItemStack aStack, Player aPlayer, Level aWorld, int aX, int aY, int aZ, byte aSide, float aHitX, float aHitY, float aHitZ) {
		if (aWorld.isClientSide() || aPlayer == null || !(aPlayer).mayUseItemAt(new BlockPos(aX, aY, aZ), FORGE_DIR[aSide], aStack)) return F;
		Block tBlock = WD.block(aWorld, aX, aY, aZ);
		if (tBlock.getBlockHardness(aWorld, aX, aY, aZ) < 0 && !BlocksGT.drillableDynamite.contains(tBlock)) return F;
		byte tMeta = (byte)WD.meta(aWorld, aX, aY, aZ);
		if (tBlock instanceof BlockStones) {if (tMeta >= 3) return F;} else
		if (!BlocksGT.drillableDynamite.contains(tBlock) && !StoneLayer.REPLACEABLE_BLOCKS.contains(tBlock) && !WD.ore_stone(tBlock, tMeta)) return F;
		
		for (int i = 0; i < Inventory.INVENTORY_SIZE; i++) {
			ItemStack tStack = aPlayer.inventory.getItem(Inventory.INVENTORY_SIZE-i-1);
			if (IL.Boomstick.equal(tStack, F, T) || IL.Dynamite.equal(tStack, F, T) || IL.Dynamite_Strong.equal(tStack, F, T)) {
				// F8: тег захвачен ОДИН раз в tOldTag (для восстановления) и ОДИН раз мутирован в tTempTag
				// (NBT_MODE=T), затем закоммичен единым ItemNBT.set — иначе setBoolean на свежем get()
				// потерялся бы до чтения tryPlaceItemIntoWorld (см. ItemNBT.java).
				CompoundTag tOldTag = ItemNBT.get(tStack);
				CompoundTag tTempTag = tOldTag != null ? (CompoundTag)tOldTag.copy() : UT.NBT.make();
				tTempTag.putBoolean(NBT_MODE, T);
				ItemNBT.set(tStack, tTempTag);
				int tOldSize = tStack.getCount();
				if (tStack.tryPlaceItemIntoWorld(aPlayer, aWorld, aX, aY, aZ, aSide, aHitX, aHitY, aHitZ)) {
					if (UT.Entities.hasInfiniteItems(aPlayer)) {
						tStack.setCount(tOldSize);
					} else {
						((MultiItemTool)aItem).doDamage(aStack, 100, aPlayer, F);
						ST.use(aPlayer, T, tStack, 0);
					}
					ItemNBT.set(tStack, tOldTag);
					// Add Dynamite Coords to Remote Activator if in Hotbar.
					for (int j = 0; j < Inventory.getHotbarSize(); j++) if (IL.Tool_Remote_Activator.equal(aPlayer.inventory.getItem(j), F, T)) {
						if (Behavior_Remote.addCoords(aPlayer.inventory.getItem(j), aPlayer, aWorld, aX+OFFX[aSide], aY+OFFY[aSide], aZ+OFFZ[aSide])) {
							break;
						}
					}
					return T;
				}
				ItemNBT.set(tStack, tOldTag);
			}
		}
		return F;
	}
	
	static {
		LH.add("gt.behaviour.placedynamite", "Places Dynamite and links it to Remote Activators in Hotbar");
	}
	
	@Override
	public List<String> getAdditionalToolTips(MultiItem aItem, List<String> aList, ItemStack aStack) {
		aList.add(LH.get("gt.behaviour.placedynamite"));
		return aList;
	}
}

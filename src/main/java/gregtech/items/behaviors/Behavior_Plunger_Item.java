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

package gregtech.items.behaviors;
import gregapi.util.WD;

import static gregapi.data.CS.*;

import java.util.List;

import gregapi.data.LH;
import gregapi.item.multiitem.MultiItem;
import gregapi.item.multiitem.behaviors.IBehavior.AbstractBehaviorDefault;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class Behavior_Plunger_Item extends AbstractBehaviorDefault {
//  private final int mCosts;
	
	public Behavior_Plunger_Item(int aCosts) {
//      mCosts = aCosts;
	}
	
	@Override
	public boolean onItemUseFirst(MultiItem aItem, ItemStack aStack, Player aPlayer, Level aWorld, int aX, int aY, int aZ, byte aSide, float hitX, float hitY, float hitZ) {
		if (aWorld.isClientSide()) return F;/*
		TileEntity aTileEntity = UT.Worlds.getTileEntity(aWorld, aX, aY, aZ, true);
		if (aTileEntity instanceof IGregTechTileEntity) {
			IMetaTileEntity tMetaTileEntity = ((IGregTechTileEntity)aTileEntity).getMetaTileEntity();
			if (tMetaTileEntity instanceof IMetaTileEntityItemPipe) {
				for (IMetaTileEntityItemPipe tTileEntity : UT.Code.sortByValuesAcending(IMetaTileEntityItemPipe.Util.scanPipes((IMetaTileEntityItemPipe)tMetaTileEntity, new HashMap<IMetaTileEntityItemPipe, Long>(), 0, false, true)).keySet()) {
					for (int i = 0, j = tTileEntity.getContainerSize(); i < j; i++) if (tTileEntity.isValidSlot(i)) {
						if (tTileEntity.getStackInSlot(i) != null) {
							if (aPlayer.getAbilities().instabuild || ((ItemMetaTool)aItem).doDamage(aStack, mCosts)) {
								ItemStack tStack = tTileEntity.decrStackSize(i, 64);
								if (tStack != null) {
									EntityItem tEntity = new EntityItem(aWorld, ((IGregTechTileEntity)aTileEntity).getOffsetX((byte)aSide, 1) + 0.5, ((IGregTechTileEntity)aTileEntity).getOffsetY((byte)aSide, 1) + 0.5, ((IGregTechTileEntity)aTileEntity).getOffsetZ((byte)aSide, 1) + 0.5, tStack);
									WD.setMotionX(tEntity, 0); WD.setMotionY(tEntity, 0); WD.setMotionZ(tEntity, 0);
									aWorld.addFreshEntity(tEntity);
									UT.Sounds.send(aWorld, GregTech_API.sSoundList.get(101), 1.0F, -1, aX, aY, aZ);
								}
								return T;
							}
						}
					}
				}
			}
		}*/
		return F;
	}
	
	static {
		LH.add("gt.behaviour.plunger.item", "Clears Items from Pipes");
	}
	
	@Override
	public List<String> getAdditionalToolTips(MultiItem aItem, List<String> aList, ItemStack aStack) {
		aList.add(LH.get("gt.behaviour.plunger.item"));
		return aList;
	}
}

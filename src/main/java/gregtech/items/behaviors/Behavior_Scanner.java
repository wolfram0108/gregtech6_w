/**
 * Copyright (c) 2022 GregTech-6 Team
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

import gregapi.code.ArrayListNoNulls;
import gregapi.data.CS.SFX;
import gregapi.data.LH;
import gregapi.data.TD;
import gregapi.item.multiitem.MultiItem;
import gregapi.item.multiitem.behaviors.IBehavior.AbstractBehaviorDefault;
import gregapi.util.UT;
import gregapi.util.WD;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

import static gregapi.data.CS.F;
import static gregapi.data.CS.T;

public class Behavior_Scanner extends AbstractBehaviorDefault {
	public final int mScanLevel;
	
	public Behavior_Scanner(int aScanLevel) {
		mScanLevel = aScanLevel;
	}
	
	@Override
	public boolean onItemUseFirst(MultiItem aItem, ItemStack aStack, Player aPlayer, Level aWorld, int aX, int aY, int aZ, byte aSide, float hitX, float hitY, float hitZ) {
		if (aPlayer instanceof ServerPlayer) {
			ArrayList<String> tList = new ArrayListNoNulls<>();
			if (aItem.useEnergy(TD.Energy.EU, aStack, WD.scan(tList, aPlayer, aWorld, mScanLevel, aX, aY, aZ, aSide, hitX, hitY, hitZ), aPlayer, aPlayer.getInventory(), aWorld, aX, aY, aZ, T)) UT.Entities.sendchat(aPlayer, tList, F);
			return T;
		}
		UT.Sounds.forActor(SFX.IC_SCANNER, 20, 1.0F, aPlayer, aX, aY, aZ); // звук ДЕЙСТВИЯ
		return aPlayer instanceof ServerPlayer;
	}
	
	@Override
	public boolean onLeftClickEntity(MultiItem aItem, ItemStack aStack, Player aPlayer, Entity aEntity) {
		if (mScanLevel > 100) {
			UT.Entities.sendchat(aPlayer, aEntity.getClass().getName());
			UT.Sounds.forActor(SFX.IC_SCANNER, 20, 1.0F, aPlayer, UT.Code.roundDown(aEntity.getX()), UT.Code.roundDown(aEntity.getY()), UT.Code.roundDown(aEntity.getZ())); // звук ДЕЙСТВИЯ
		}
		return T;
	}
	
	@Override
	public boolean onRightClickEntity(MultiItem aItem, ItemStack aStack, Player aPlayer, Entity aEntity) {
		if (mScanLevel > 100) {
			UT.Entities.sendchat(aPlayer, aEntity.getClass().getName());
			UT.Sounds.forActor(SFX.IC_SCANNER, 20, 1.0F, aPlayer, UT.Code.roundDown(aEntity.getX()), UT.Code.roundDown(aEntity.getY()), UT.Code.roundDown(aEntity.getZ())); // звук ДЕЙСТВИЯ
		}
		return T;
	}
	
	static {
		LH.add("gt.behaviour.scanning", "Can scan Blocks in World");
	}
	
	@Override
	public List<String> getAdditionalToolTips(MultiItem aItem, List<String> aList, ItemStack aStack) {
		aList.add(LH.get("gt.behaviour.scanning"));
		return aList;
	}
}

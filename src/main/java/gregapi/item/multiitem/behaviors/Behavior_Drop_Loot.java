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
 *
 * Modified in 2026 for the GregTech 6 NeoForge port
 * (https://github.com/wolfram0108/gregtech6_w): ported from Minecraft 1.7.10 / Forge
 * to Minecraft 26.1.2 / NeoForge.
 */

package gregapi.item.multiitem.behaviors;

import gregapi.item.multiitem.MultiItem;
import gregapi.item.multiitem.behaviors.IBehavior.AbstractBehaviorDefault;
import gregapi.util.ST;
import gregapi.util.UT;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import gt6mirror.minecraftforge.common.ChestGenHooks;

import java.util.List;

import static gregapi.data.CS.*;

public class Behavior_Drop_Loot extends AbstractBehaviorDefault {
	public String[] mLoots;
	
	public Behavior_Drop_Loot(String... aLoots) {
		mLoots = aLoots;
	}
	
	@Override
	public List<String> getAdditionalToolTips(MultiItem aItem, List<String> aList, ItemStack aStack) {
		aList.add("Rightclick this on a Block to loot");
		return aList;
	}
	
	@Override
	public boolean onItemUse(MultiItem aItem, ItemStack aStack, Player aPlayer, Level aWorld, int aX, int aY, int aZ, byte aSide, float aHitX, float aHitY, float aHitZ) {
		if (!aWorld.isClientSide() && ST.use(aPlayer, T, aStack)) {
			for (String tLoot : mLoots) ST.drop(aWorld, aX+OFFX[aSide]+0.5, aY+OFFY[aSide]+0.5, aZ+OFFZ[aSide]+0.5, ChestGenHooks.getOneItem(tLoot, RNGSUS));
			if (aPlayer != null) UT.Sounds.send(SFX.MC_DIG_CLOTH, aPlayer);
			return T;
		}
		return F;
	}
}

/**
 * Copyright (c) 2026 wolfram0108
 *
 * Written in 2026 for the GregTech 6 NeoForge port
 * (https://github.com/wolfram0108/gregtech6_w). Not part of the original GregTech 6
 * by Gregorius Techneticies; distributed under the same licence as the work it extends.
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

package gregapi.compat.buildcraft;

import buildcraft.api.tools.IToolWrench;
import gregapi.item.multiitem.MultiItemToolWithCompat;
import gregapi.item.multiitem.tools.IToolStats;
import gregapi.util.UT;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.HitResult;

import static gregapi.data.CS.*;

/**
 * The GT6 tool item as BuildCraft sees it: BuildCraft 7.99 recognises a wrench by instanceof IToolWrench on the item,
 * and the interface may only be declared when BuildCraft is installed, so Loader_Tools builds the tool from this class
 * by name when it is. The answers are the 1.7.10 ones (MultiItemToolWithCompat:78-89), asked for the stack BuildCraft
 * hands over instead of the player's current item.
 */
public class MultiItemToolWithCompatBC extends MultiItemToolWithCompat implements IToolWrench {
	public MultiItemToolWithCompatBC(String aModID, String aUnlocalized) {
		super(aModID, aUnlocalized);
	}
	
	@Override
	public boolean canWrench(Player aPlayer, InteractionHand aHand, ItemStack aStack, HitResult aRayTrace) {
		if (!isItemStackUsable(aStack)) return F;
		IToolStats tStats = getToolStats(aStack);
		return tStats != null && tStats.isWrench();
	}
	
	@Override
	public void wrenchUsed(Player aPlayer, InteractionHand aHand, ItemStack aStack, HitResult aRayTrace) {
		IToolStats tStats = getToolStats(aStack);
		if (tStats != null && !UT.Entities.hasInfiniteItems(aPlayer)) doDamage(aStack, 100, aPlayer, T);
	}
}

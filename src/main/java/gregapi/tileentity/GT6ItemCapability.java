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

package gregapi.tileentity;

import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.WorldlyContainer;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.wrapper.InvWrapper;
import net.minecraftforge.items.wrapper.SidedInvWrapper;

/** Vanilla hoppers still read the inventory directly; modded transport needs ForgeCapabilities.ITEM_HANDLER instead,
 *  declared once at the BlockEntity root and wrapped by the stock SidedInvWrapper, which still asks GT6's own side rules. */
public final class GT6ItemCapability {
	private GT6ItemCapability() {}

	/** Whether there's anything to show externally: an empty inventory is not the same as having none at all. */
	public static boolean hasInventory(net.minecraft.world.level.block.entity.BlockEntity aBlockEntity) {
		try {
			return aBlockEntity instanceof Container tContainer && tContainer.getContainerSize() > 0;
		} catch (Throwable e) {return false;} // A specific TE's own logic must not crash another mod that simply asked for this capability.
	}

	/** The handler is bound to whichever side was requested; null means a sideless request, GT6's own SIDE_ANY. */
	public static IItemHandler handlerOf(net.minecraft.world.level.block.entity.BlockEntity aBlockEntity, Direction aSide) {
		if (aBlockEntity instanceof WorldlyContainer tWorldly) return new SidedInvWrapper(tWorldly, aSide);
		return new InvWrapper((Container)aBlockEntity);
	}
}

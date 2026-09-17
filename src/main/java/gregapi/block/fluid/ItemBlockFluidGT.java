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

package gregapi.block.fluid;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

/** 1.7.10 injected names into a live translation table read by any caller; the port instead has every item override
 *  getName() from its own table, but fluid blocks kept registering plain BlockItem, bypassing that bridge entirely. */
public class ItemBlockFluidGT extends BlockItem {
	public ItemBlockFluidGT(Block aBlock) {
		// The item id is required and derived from the already-registered block's key, the same technique as ItemBlockBase.
		super(aBlock, new Item.Properties());
	}

	/** Reads the name from GT6's own table, like every other item in the mod; if it's missing there, engine behavior is
	 *  unchanged. */
	@Override
	public net.minecraft.network.chat.Component getName(ItemStack aStack) {
		String tName = nameOf(getBlock());
		return tName != null && !tName.isEmpty() ? net.minecraft.network.chat.Component.literal(tName) : super.getName(aStack);
	}

	/** The same source the block itself uses: getLocalizedName() on either fluid hierarchy. */
	public static String nameOf(Block aBlock) {
		try {
			if (aBlock instanceof BlockBaseFluid tFluid) return tFluid.getLocalizedName();
			if (aBlock instanceof gregtech.blocks.fluids.BlockWaterlike tWater) return tWater.getLocalizedName();
		} catch (Throwable e) {/* a missing name must never crash the tooltip */}
		return null;
	}
}

/**
 * Copyright (c) 2020 GregTech-6 Team
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

package gregapi.item;

import java.util.List;

import net.neoforged.api.distmarker.Dist;
import gregapi.data.CS.ItemsGT;
import gregapi.data.MD;
import gregapi.old.Textures;
import gregapi.util.ST;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.resources.ResourceLocation;

/**
 * @author Gregorius Techneticies
 */
public class ItemEmptySlot extends ItemBase {
	public ItemEmptySlot() {
		super(MD.GAPI.mID, "gt.empty_slot", "Empty Slot", "This Slot has to be left Empty");
		ItemsGT.ILLEGAL_DROPS.add(this);
		gregapi.GT_API.deferItemInit(() -> ST.hide(this)); // F1/F12/F16: ST.hide создаёт стек (ST.make) — отложить в setup (пост-freeze), т.к. конструкция @RegisterEvent (компоненты не привязаны)
	}
	
	@Override
	public ResourceLocation getIconFromDamage(int aMeta) {
		return Textures.ItemIcons.VOID.getIcon(0);
	}

	@Override
	public void registerIcons(Object aIconRegister) {
		// No Icons to register!
	}
	
	// @Override
	public final void getSubItems(Item var1, CreativeModeTab aCreativeTab, @SuppressWarnings("rawtypes") List aList) {
		//
	}
}

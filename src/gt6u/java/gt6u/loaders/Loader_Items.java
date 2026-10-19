/**
 * Copyright (c) 2020 Gregtech6-Unofficial Team
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
 * Modified in 2026 for the GregTech 6 NeoForge port (https://github.com/wolfram0108/gregtech6_w):
 * the GregTech6-Unofficial entries of gregtech/loaders/a/Loader_Items.java moved into an addon registry.
 */

package gt6u.loaders;

import gregapi.GT_API;
import gt6u.GT6U_Main;

import gt6u.items.MultiItemPhysics;


import gt6u.data.CS_GT6U;

import static gregapi.data.CS.*;
import gregapi.data.CS.ItemsGT;
import gregapi.data.CS.SFX;
import gregapi.data.MD;
import gregapi.data.MT;
import gregapi.data.OP;
import gregapi.data.TD;
import gregapi.item.prefixitem.PrefixItem;
import gregapi.item.prefixitem.PrefixItemProjectile;
import gregapi.item.prefixitem.PrefixItemRing;
import gregapi.util.ST;
import gregtech.entities.projectiles.EntityArrow_Material;
import gregtech.items.*;
import gt6u.items.MultiItemBiology;
import gt6u.GT6U_Recipes;

/** GregTech6-Unofficial's edits of gregtech/loaders/a/Loader_Items.java, as statements: what GT6U added runs in run(), what it dropped is taken back in
 *  takeBack(): restated as GT6 makes them, their Recipes are taken out of GT6's maps again. */
public class Loader_Items {
	public static void run() {
		run_run();
	}
	
	public static void takeBack() {
	}
	
	/** GT6U's additions to Loader_Items.run (GT6U gregtech/loaders/a/Loader_Items.java). */
	static void run_run() {
		// Made on RegisterEvent, as GT6 makes its own MultiItems (gregtech/loaders/a/Loader_Items.java).
		GT_API.registerItemLazy(GT6U_Main.MOD_ID, "gt.multiitem.physics", () -> CS_GT6U.PHYS = new MultiItemPhysics());
		GT_API.registerItemLazy(GT6U_Main.MOD_ID, "gt.multiitem.biology", () -> CS_GT6U.BIO = new MultiItemBiology());
		GT_API.registerItemLazy(GT6U_Main.MOD_ID, "gt.multiitem.technological", () -> CS_GT6U.TECH = new gt6u.items.MultiItemTechnological());
		// GT6U lists its MultiItems with GT6's ones (GT6U CS.java:1556); the array is GT6's, made longer for them once GT6 filled it.
		GT_API.deferItemInit(() -> {
			ItemsGT.ALL_MULTI_ITEMS = java.util.Arrays.copyOf(ItemsGT.ALL_MULTI_ITEMS, ItemsGT.ALL_MULTI_ITEMS.length + 3);
			ItemsGT.ALL_MULTI_ITEMS[ItemsGT.ALL_MULTI_ITEMS.length - 3] = CS_GT6U.PHYS;
			ItemsGT.ALL_MULTI_ITEMS[ItemsGT.ALL_MULTI_ITEMS.length - 2] = CS_GT6U.BIO;
			ItemsGT.ALL_MULTI_ITEMS[ItemsGT.ALL_MULTI_ITEMS.length - 1] = CS_GT6U.TECH;
		});
	}
}

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

package gregapi.data;

import java.util.concurrent.CompletableFuture;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;

import gregapi.item.IItemProjectile;

/** Carries GT6's ammo data into tags (same trick as GT6HarvestTags). In 1.7.10, firing a
 *  GT6 arrow from a vanilla bow relied on event order: the bow posted its event before checking for a vanilla arrow at all. */
public class GT6ItemTags extends TagsProvider<Item> {
	public GT6ItemTags(PackOutput aOutput, CompletableFuture<HolderLookup.Provider> aLookup) {
		super(aOutput, Registries.ITEM, aLookup, CS.ModIDs.GT, null);
	}

	@Override
	public String getName() {return "GT6 Item Tags (ammo)";}

	@Override
	protected void addTags(HolderLookup.Provider aProvider) {
		int tArrows = 0;
		for (Item tItem : BuiltInRegistries.ITEM) {
			if (!(tItem instanceof IItemProjectile tProjectile)) continue;
			net.minecraft.resources.ResourceLocation tID = BuiltInRegistries.ITEM.getKey(tItem);
			if (tID == null || !(tID.getNamespace().equals(CS.ModIDs.GT) || tID.getNamespace().equals("gregtech"))) continue;
			if (!isArrow(tProjectile, tID)) continue;
			getOrCreateRawBuilder(ItemTags.ARROWS).addElement(tID);
			tArrows++;
		}
		gregapi.data.CS.OUT.println("[GT6-DATAGEN] стрел GT6 внесено в minecraft:arrows: " + tArrows);
	}

	/** Asks the item its own projectile TYPE, not whether a specific stack qualifies, since ItemStack
	 *  can't be built at datagen time. Anything outside this hierarchy surfaces as a warning for a human, not a silent skip. */
	private static boolean isArrow(IItemProjectile aProjectile, net.minecraft.resources.ResourceLocation aID) {
		if (aProjectile instanceof gregapi.item.prefixitem.PrefixItemProjectile tPrefix)
			return tPrefix.mProjectileType == TD.Projectiles.ARROW;
		// Second carrier of the same contract, the multi-item: whether it's an arrow is decided by its
		// BEHAVIORS, asked directly from the registry without building a stack at all.
		if (aProjectile instanceof gregapi.item.multiitem.MultiItem tMulti) {
			for (java.util.ArrayList<gregapi.item.multiitem.behaviors.IBehavior<gregapi.item.multiitem.MultiItem>> tList : tMulti.mItemBehaviors.values())
				for (gregapi.item.multiitem.behaviors.IBehavior<gregapi.item.multiitem.MultiItem> tBehavior : tList) {
					try {
						if (tBehavior.hasProjectile(tMulti, TD.Projectiles.ARROW, null)) return true;
					} catch (Throwable e) {/* This provider only sees the Item, not a stack, so the projectile check can't apply here. */}
				}
			return false;
		}
		gregapi.data.CS.OUT.println("[GT6-DATAGEN] ⚠ носитель IItemProjectile вне известных иерархий: " + aID
			+ " — тип снаряда на фазе датагена не спросить (стек создавать нельзя), в minecraft:arrows НЕ внесён");
		return false;
	}
}

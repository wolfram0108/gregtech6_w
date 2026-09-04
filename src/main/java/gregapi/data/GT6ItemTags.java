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

/**
 * F12-ammo — MOVING GT6's AMMO DATA INTO TAGS (the same approach as {@link GT6HarvestTags} for mining).
 *
 * <p><b>What the engine changed.</b> In 1.7.10, GT6 arrow-shooting from a vanilla bow rested on call
 * order: {@code ItemBow.onPlayerStoppedUsing} posted {@code ArrowLooseEvent} FIRST
 * ({@code gregtech6/build/tmp/recompSrc/net/minecraft/item/ItemBow.java:39-40}), BEFORE any ammo
 * check, and GT6 intercepted the shot ({@code GT_API_Proxy.onArrowLooseEvent}), created its own
 * projectile and cancelled the vanilla branch. A vanilla arrow being present was not required — the
 * {@code inventory.hasItem(Items.arrow)} check came AFTER the event ({@code :49}).
 *
 * <p>In NeoForge 26.1.2 the order is reversed: {@code BowItem.releaseUsing} first asks
 * {@code player.getProjectile(bow)} and exits IMMEDIATELY on an empty result
 * ({@code neo-decompiled/net/minecraft/world/item/BowItem.java:30-33}) — no shot event is posted
 * at all. And ammo selection there is {@code ARROW_ONLY = itemStack.is(ItemTags.ARROWS)}
 * ({@code ProjectileWeaponItem.java:20}), i.e. purely tag-based.
 *
 * <p><b>Consequence for the port.</b> GT6 arrows were not listed in the vanilla tag, so the bow did not
 * see them as ammo — and all GT6 arrow shooting was silently dead (measurement: {@code getProjectile -> EMPTY}
 * with a green control on a vanilla arrow). This isn't "enchantments don't apply", it's a missing precondition.
 *
 * <p><b>A transfer, not a list.</b> Who counts as an arrow is known by the item itself — the
 * {@link IItemProjectile#hasProjectile} contract, the same one the mod asks when shooting
 * ({@code GT_API_Proxy.onArrowLooseEvent} via {@code ST.projectile}). The provider walks the registry and
 * asks the CONTRACT, so a new arrow lands in the tag by itself, with no data edit needed — GT6's property
 * of generating content procedurally is preserved.
 *
 * <p>Bullets ({@code TD.Projectiles.BULLET_*}) do NOT land here: even in 1.7.10 they were never shot from a bow —
 * {@code onArrowLooseEvent} looked strictly for {@code TD.Projectiles.ARROW}.
 */
public class GT6ItemTags extends TagsProvider<Item> {
	public GT6ItemTags(PackOutput aOutput, CompletableFuture<HolderLookup.Provider> aLookup) {
		super(aOutput, Registries.ITEM, aLookup, CS.ModIDs.GT);
	}

	@Override
	public String getName() {return "GT6 Item Tags (ammo)";}

	@Override
	protected void addTags(HolderLookup.Provider aProvider) {
		int tArrows = 0;
		for (Item tItem : BuiltInRegistries.ITEM) {
			if (!(tItem instanceof IItemProjectile tProjectile)) continue;
			net.minecraft.resources.Identifier tID = BuiltInRegistries.ITEM.getKey(tItem);
			if (tID == null || !(tID.getNamespace().equals(CS.ModIDs.GT) || tID.getNamespace().equals("gregtech"))) continue;
			if (!isArrow(tProjectile, tID)) continue;
			getOrCreateRawBuilder(ItemTags.ARROWS).addElement(tID);
			tArrows++;
		}
		gregapi.data.CS.OUT.println("[GT6-DATAGEN] GT6 arrows added to minecraft:arrows: " + tArrows);
	}

	/** Ask the item for ITS PROJECTILE TYPE, not "is this particular stack eligible".
	 *  <p>⛔ During the datagen phase an {@code ItemStack} MUST NOT be created: {@code Holder.Reference.components}
	 *  isn't bound yet and any {@code ST.make} fails with "Components not bound yet" (the same limitation is
	 *  recorded in {@code build.gradle:213}; the first version of this provider crashed {@code runData} exactly this way).
	 *  So selection goes by a field set in the item's constructor ({@code PrefixItemProjectile.java:68}) —
	 *  it doesn't depend on the phase. A contract carrier outside this hierarchy won't be lost silently: it falls
	 *  through to the warning below, and a human decides what to do about it, not a silent skip. */
	private static boolean isArrow(IItemProjectile aProjectile, net.minecraft.resources.Identifier aID) {
		if (aProjectile instanceof gregapi.item.prefixitem.PrefixItemProjectile tPrefix)
			return tPrefix.mProjectileType == TD.Projectiles.ARROW;
		// The second contract carrier — a multi-item: for it, whether "this is an arrow" is decided by BEHAVIORS
		// (MultiItem.java:103-106). Ask the behavior registry directly, without creating a stack:
		// Behavior_Arrow.hasProjectile doesn't look at the stack at all (Behavior_Arrow.java:114-115).
		if (aProjectile instanceof gregapi.item.multiitem.MultiItem tMulti) {
			for (java.util.ArrayList<gregapi.item.multiitem.behaviors.IBehavior<gregapi.item.multiitem.MultiItem>> tList : tMulti.mItemBehaviors.values())
				for (gregapi.item.multiitem.behaviors.IBehavior<gregapi.item.multiitem.MultiItem> tBehavior : tList) {
					try {
						if (tBehavior.hasProjectile(tMulti, TD.Projectiles.ARROW, null)) return true;
					} catch (Throwable e) {/* the behavior looks at the stack — can't ask during this phase, and it isn't an arrow anyway */}
				}
			return false;
		}
		gregapi.data.CS.OUT.println("[GT6-DATAGEN] WARNING: IItemProjectile carrier outside the known hierarchies: " + aID
			+ " — the projectile type cannot be asked for during datagen (no stack may be created), so it was NOT added to minecraft:arrows");
		return false;
	}
}

/**
 * Copyright (c) 2026 wolfram0108
 *
 * COMPILE-TIME STAND-IN — NOT THIRD-PARTY CODE.
 *
 * This declaration was written from scratch for the GregTech 6 NeoForge port
 * (https://github.com/wolfram0108/gregtech6_w). It contains no code from the project
 * that owns this package name, and no part of it was copied or decompiled from that
 * project: it declares only the members GregTech 6 itself implements or calls, so that
 * the port compiles while integration with that mod stays deferred.
 *
 * The original package name is kept deliberately, because GregTech 6 implements these
 * types verbatim and the port does not alter the code Gregorius Techneticies wrote.
 * Removing these classes from the build is not possible: 66 classes of the mod extend
 * or implement them, and the JVM requires the type to load the implementing class.
 *
 * All names, trademarks and rights in the project this package belongs to remain with
 * its authors. See src/compat-mirror/README.md and NOTICE.
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

// Package is gt6mirror.minecraftforge.common, not net.minecraftforge.common: the real forge module and
// gregtech6 would otherwise export the same package (JPMS split); repackaged, not deleted, since runtime uses it.
package gt6mirror.minecraftforge.common;

import java.util.Random;

import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

/** 1.7.10's WeightedRandomChestContent, a chest-loot data holder, lives under net.minecraftforge.common here
 *  since net.minecraft.* is a JPMS split-package owned by the real minecraft module; body matches 1:1. */
public class WeightedRandomChestContent {
	public ItemStack theItemId;
	public int theMinimumChanceToGenerateItem;
	public int theMaximumChanceToGenerateItem;
	public int itemWeight;

	public WeightedRandomChestContent(ItemStack aStack, int aMinChance, int aMaxChance, int aWeight) {
		theItemId = aStack;
		theMinimumChanceToGenerateItem = aMinChance;
		theMaximumChanceToGenerateItem = aMaxChance;
		itemWeight = aWeight;
	}

	/** Generates the stacks for one entry; the body is unchanged from 1.7.10. */
	protected ItemStack[] generateChestContent(Random aRandom, Container aInventory) {
		return ChestGenHooks.generateStacks(aRandom, theItemId, theMinimumChanceToGenerateItem, theMaximumChanceToGenerateItem);
	}

	/** Unchanged from 1.7.10: picks an entry by weight, generates its stacks, and places them into
	 *  random inventory slots, repeated aCount times. */
	public static void generateChestContents(Random aRandom, WeightedRandomChestContent[] aList, Container aInventory, int aCount) {
		for (int j = 0; j < aCount; ++j) {
			WeightedRandomChestContent tContent = getRandomItem(aRandom, aList);
			if (tContent == null) continue;
			ItemStack[] tStacks = tContent.generateChestContent(aRandom, aInventory);
			for (ItemStack tStack : tStacks) {
				aInventory.setItem(aRandom.nextInt(aInventory.getContainerSize()), tStack);
			}
		}
	}

	/** Reproduces 1.7.10's weighted-roulette selection here because neo reworked the WeightedRandom
	 *  class incompatibly, and this is the only place that needs the original formula. */
	public static WeightedRandomChestContent getRandomItem(Random aRandom, WeightedRandomChestContent[] aList) {
		int tTotal = 0;
		for (WeightedRandomChestContent tContent : aList) tTotal += tContent.itemWeight;
		if (tTotal <= 0) return null;
		int tRoll = aRandom.nextInt(tTotal);
		for (WeightedRandomChestContent tContent : aList) {
			tRoll -= tContent.itemWeight;
			if (tRoll < 0) return tContent;
		}
		return null;
	}
}

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

package thaumcraft.api;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.world.item.ItemStack;
import thaumcraft.api.aspects.AspectList;

/** Compile-only mirror of the Thaumcraft API: only what GregTech6 references; bodies stay empty
 *  since none of this runs without the real mod loaded. */
public class ThaumcraftApi {
	/** A stub map until real integration; the key is Object rather than String because CompatTC
	 *  stores a List(Item, Integer) here. */
	public static Map<Object, Object> objectTags = new HashMap<>();

	/** CompatTC.java: portableHoleBlackList.add(...). */
	public static List<Object> portableHoleBlackList = new ArrayList<>();

	/** Uses the same List(Item, Integer) key convention as objectTags, read by
	 *  Thaumcraft_AspectLagFix.getCachedItemHash. */
	public static Map<Object, int[]> groupedObjectTags = new HashMap<>();

	public static void registerEntityTag(String aEntityName, AspectList aAspects) {}

	public static void registerComplexObjectTag(ItemStack aStack, AspectList aAspects) {}

	/** CompatTC.java:354/359/375/387: addCrucibleRecipe(String, ItemStack, Object, AspectList). */
	public static Object addCrucibleRecipe(String aResearch, ItemStack aOutput, Object aInput, AspectList aAspects) {return null;}
}

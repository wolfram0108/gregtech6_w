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

// Package is gt6mirror.minecraftforge.fluids, not net.minecraftforge.fluids: the real forge module and
// gregtech6 would otherwise export the same package (JPMS split); repackaged, not deleted, since runtime uses it.
package gt6mirror.minecraftforge.fluids;

import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fluids.FluidStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** 1.7.10's FluidContainerRegistry has no neo equivalent (the whole class is gone); neo uses per-item
 *  BucketItem+capability instead of a global registry (PORT-TODO(F5): no auto full<->empty registry). */
public class FluidContainerRegistry {
	private static final List<FluidContainerData> DATA = new ArrayList<>();

	/** The "no empty container" marker is specifically a bucket, since consumers distinguish it by
	 *  item; it is built lazily because this class loads before vanilla registries exist. */
	private static ItemStack nullEmptyContainer() {return new ItemStack(net.minecraft.world.item.Items.BUCKET);}

	public static FluidContainerData registerFluidContainer(FluidContainerData aData) {
		if (aData != null) {
			DATA.add(aData);
			MinecraftForge.EVENT_BUS.post(new FluidContainerRegisterEvent(aData));
		}
		return aData;
	}

	public static List<FluidContainerData> getRegisteredFluidContainerData() {
		return Collections.unmodifiableList(DATA);
	}

	/** Mirrors the Forge 1.7.10 FluidContainerData fields exactly, checked against every GT6 call site. */
	public static class FluidContainerData {
		public FluidStack fluid;
		public ItemStack filledContainer;
		public ItemStack emptyContainer;

		public FluidContainerData(FluidStack aFluid, ItemStack aFilledContainer, ItemStack aEmptyContainer) {
			this(aFluid, aFilledContainer, aEmptyContainer, false);
		}

		public FluidContainerData(FluidStack aFluid, ItemStack aFilledContainer, ItemStack aEmptyContainer, boolean aAllowNullEmptyContainer) {
			fluid = aFluid;
			filledContainer = aFilledContainer;
			// When there is no empty container the field gets the bucket marker instead of null, because
			// Loader_Recipes_Foreign reads "empty == bucket" to source the empty side; null would drop those recipes.
			emptyContainer = aEmptyContainer == null ? nullEmptyContainer() : aEmptyContainer;
		}
	}

	/** Mirrors Forge 1.7.10's FluidContainerRegisterEvent; the data field is exactly what
	 *  OreDictManager.onFluidContainerRegistration reads. */
	public static class FluidContainerRegisterEvent extends Event {
		public final FluidContainerData data;

		public FluidContainerRegisterEvent(FluidContainerData aData) {
			data = aData;
		}
	}
}

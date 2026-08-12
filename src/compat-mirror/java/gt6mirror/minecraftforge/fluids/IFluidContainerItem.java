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

// Пакет gt6mirror.minecraftforge.fluids (не net.minecraftforge.fluids): boot-краш ResolutionException —
// настоящий модуль forge 1.20.1 и модуль gregtech6 экспортировали бы один и тот же пакет net.minecraftforge.*
// (split-package), JPMS такое не резолвит; тип живой (используется рантаймом), поэтому переупакован, а не удалён.
package gt6mirror.minecraftforge.fluids;

import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

/**
 * F5-мост (BUG-045), ЖИВОЙ контракт (не compile-only shim). 1.7.10 Forge
 * {@code net.minecraftforge.fluids.IFluidContainerItem} удалён в neo (заменён item-bound
 * capability {@code IFluidHandlerItem}/{@code Capabilities.Fluid.ITEM} — НЕ 1:1: singleton
 * {@code Item} не может отдать {@code getContainer()} без per-stack состояния).
 * Поверхность 1:1 с 1.7.10 (gregtech6/build/tmp/recompSrc/net/minecraftforge/fluids/
 * IFluidContainerItem.java), старый->neo {@code ItemStack}/{@code FluidStack}.
 * <p>
 * Null-семантика 1.7.10 сохранена: {@code getFluid}/{@code drain} возвращают {@code null}
 * (CS.NF) при пустом контейнере, НЕ {@code FluidStack.EMPTY}.
 * <p>
 * Реализуют (то же множество, что в оригинале): {@code ItemFluidDisplay},
 * {@code MultiTileEntityItemInternal} (делегат на TE), {@code TileEntityBase08FluidContainer},
 * {@code TileEntityBase08Barrel}. Распознают через {@code instanceof}: {@code FL.fill/contains/
 * getFluid/getEmpty}, {@code ST.ingredable/container}, {@code OreDictManager},
 * {@code RecipeMapFluidCanner}, {@code Behavior_Turn_Into}, {@code IItemRottable},
 * {@code MultiTileEntityFluidFunnel/CapNozzle}, {@code Behavior_Watering_Crops},
 * {@code GT_API_Proxy} — 1:1 карта вызывателей оригинала.
 */
public interface IFluidContainerItem {
	/** @return FluidStack representing the fluid in the container, null if the container is empty. */
	FluidStack getFluid(ItemStack container);

	/** @return Capacity of this fluid container. */
	int getCapacity(ItemStack container);

	/** @return Amount of fluid that was (or would have been, if simulated) filled into the container. */
	int fill(ItemStack container, FluidStack resource, boolean doFill);

	/** @return FluidStack representing fluid that was (or would have been, if simulated) drained from the container. */
	FluidStack drain(ItemStack container, int maxDrain, boolean doDrain);
}

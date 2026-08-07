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

package net.minecraftforge.fluids;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * F-fluid compile-only shim. 1.7.10 Forge {@code net.minecraftforge.fluids.IFluidBlock} удалён в neo —
 * fluid-блоки стали {@code LiquidBlock} + {@code FluidState}, слив через {@code BucketPickup.pickupBlock}.
 * Поверхность 1:1 с 1.7.10 (gregtech6/.../IFluidBlock.java), World->Level, старый->neo FluidStack.
 * <p>
 * НИ ОДИН GT6-блок его не реализует — используется только для {@code instanceof}/cast. Реальная детекция
 * fluid-блока и слив — контракт F-fluid (PORT-TODO): {@code instanceof IFluidBlock} -> neo
 * {@code state.getFluidState()}/{@code LiquidBlock}; drain -> neo pickup. Сейчас deferred (тип для сборки ядра).
 */
public interface IFluidBlock {
	Fluid getFluid();
	FluidStack drain(Level world, int x, int y, int z, boolean doDrain);
	boolean canDrain(Level world, int x, int y, int z);
	float getFilledPercentage(Level world, int x, int y, int z);
}

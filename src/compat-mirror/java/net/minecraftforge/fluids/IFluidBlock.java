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

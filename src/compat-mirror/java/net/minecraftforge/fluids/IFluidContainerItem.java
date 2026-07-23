package net.minecraftforge.fluids;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;

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

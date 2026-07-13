package net.minecraftforge.fluids;

import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.Event;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * F5 compile-only shim. 1.7.10 Forge {@code net.minecraftforge.fluids.FluidContainerRegistry} удалён в neo
 * целиком — весь пакет {@code net.minecraftforge} отсутствует на classpath (0 хитов во всех 3 корнях
 * референса neo/neoforge/fml, не split-package с реальным {@code net.minecraft}/{@code net.neoforged}), поэтому
 * зеркалирование в compat-mirror возможно (тот же приём, что уже применён для {@code net.minecraft.launchwrapper},
 * F2-shim). Глобальный авто-реестр full<->empty пар контейнеров (вёдра, банки...) неo-аналога не имеет — 26.1.2
 * использует per-item {@code BucketItem}+capability без единого auto-реестра (PORT-TODO(F5, авто-реестр
 * бакетов/канистр), decisions/F5-fluids.md §3,8, уже закреплено в {@code gregapi.data.FL} — владелец РЕГИСТРАЦИИ
 * остаётся no-op, эта точка НЕ меняет то решение). Здесь зеркалируется только структура Forge-класса (тип +
 * вложенные {@link FluidContainerData}/{@link FluidContainerRegisterEvent}), которую GT6 использует повсеместно
 * как ЛИТЕРАЛ item-метаданных (напр. {@code gregtech.items.MultiItemTechnological}), не только через сам реестр.
 * <p>
 * {@link #registerFluidContainer(FluidContainerData)} реализован честно (реальный локальный список + рассылка
 * события через {@code NeoForge.EVENT_BUS}, тот же приём, что {@code gregapi.oredict.OreDictionary.OreRegisterEvent}),
 * но в этом заходе его никто не зовёт: единственный вызыватель в GT6 — {@code gregapi.data.FL.reg/set(FluidContainerData,...)}
 * — остаётся no-op-заглушкой (F5-решение «не изобретать новый API» уже принято ДО этого захода) — реестр всегда
 * пуст в рантайме, {@link #getRegisteredFluidContainerData()} честно отражает это.
 */
public class FluidContainerRegistry {
	private static final List<FluidContainerData> DATA = new ArrayList<>();

	public static FluidContainerData registerFluidContainer(FluidContainerData aData) {
		if (aData != null) {
			DATA.add(aData);
			NeoForge.EVENT_BUS.post(new FluidContainerRegisterEvent(aData));
		}
		return aData;
	}

	public static List<FluidContainerData> getRegisteredFluidContainerData() {
		return Collections.unmodifiableList(DATA);
	}

	/** 1:1 зеркало полей Forge-1.7.10 {@code FluidContainerRegistry.FluidContainerData} (сверено по всем вызывающим GT6-местам: {@code .fluid}/{@code .filledContainer}/{@code .emptyContainer}). */
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
			emptyContainer = aAllowNullEmptyContainer && aEmptyContainer == null ? null : aEmptyContainer;
		}
	}

	/** 1:1 зеркало Forge-1.7.10 {@code FluidContainerRegistry.FluidContainerRegisterEvent} ({@code data}-поле — ровно то, что читает {@code OreDictManager.onFluidContainerRegistration}). */
	public static class FluidContainerRegisterEvent extends Event {
		public final FluidContainerData data;

		public FluidContainerRegisterEvent(FluidContainerData aData) {
			data = aData;
		}
	}
}

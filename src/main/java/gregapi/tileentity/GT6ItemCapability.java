/**
 * Copyright (c) 2023 GregTech-6 Team
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

package gregapi.tileentity;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.WorldlyContainer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.VanillaContainerWrapper;
import net.neoforged.neoforge.transfer.item.WorldlyContainerWrapper;

/**
 * ВОССТАНОВЛЕНИЕ СТАНДАРТНОГО КАНАЛА ИНВЕНТАРЯ (тот же класс потери, что был у жидкостей —
 * {@link gregapi.fluid.GT6FluidCapability}, и лечится тем же приёмом).
 *
 * <p><b>Что было в 1.7.10.</b> Базовые TE GT6 объявляли ванильные {@code IInventory}/{@code ISidedInventory}
 * (оригинал {@code TileEntityBase05Inventories:41}, {@code TileEntityBase06Covers:62}) — и этого хватало:
 * чужая воронка, труба, сортировщик и Waila читали инвентарь машины без единой строчки про GT6.
 *
 * <p><b>Что стало в neo.</b> Интерфейсы перенесены 1:1 ({@code Container}/{@code WorldlyContainer}), но
 * снаружи блок виден только через ЗАРЕГИСТРИРОВАННУЮ capability: {@code Capabilities.Item.BLOCK}.
 * Регистрации не было ({@code grep "Capabilities.Item"} по порту давал 0), поэтому для стороннего мода
 * инвентаря у машин GT6 просто не существовало. Jade, например, ищет содержимое именно так —
 * {@code CommonProxy.findItemHandler} → {@code level.getCapability(Capabilities.Item.BLOCK, …)}
 * (исходники Jade, ветка 26.1-neoforge, {@code CommonProxy.java:290-297}).
 *
 * <p><b>Своей логики переноса предметов здесь нет.</b> Ванильный инвентарь оборачивается ШТАТНЫМИ
 * обёртками движка: {@link WorldlyContainerWrapper} (учитывает стороны — прямой аналог
 * {@code ISidedInventory} 1.7.10) и {@link VanillaContainerWrapper} для стороннего запроса без стороны.
 * Правила «что откуда можно брать» остаются целиком на GT6 — их задают его же
 * {@code getSlotsForFace/canPlaceItemThroughFace/canTakeItemThroughFace}.
 *
 * <p><b>Регистрация ПО БЛОКАМ, а не по {@code BlockEntityType}</b> — то же требование движка, что и у
 * жидкостей: {@code MTE_TYPE} создан с пустым {@code validBlocks}, и {@code registerBlockEntity} для него
 * молча не работает (замер MODCOMPAT-001 П2).
 */
public class GT6ItemCapability {
	private GT6ItemCapability() {}

	/** Подписка на мод-шину — рядом с остальными центральными переходниками в {@code GT_API.init}. */
	public static void register(IEventBus aModBus) {
		aModBus.addListener(GT6ItemCapability::onRegisterCapabilities);
	}

	private static void onRegisterCapabilities(RegisterCapabilitiesEvent aEvent) {
		List<net.minecraft.world.level.block.Block> tBlocks = new ArrayList<>();
		for (net.minecraft.world.level.block.Block tBlock : net.minecraft.core.registries.BuiltInRegistries.BLOCK) {
			if (tBlock instanceof gregapi.block.multitileentity.MultiTileEntityBlock || tBlock instanceof gregapi.block.multitileentity.MultiTileEntityBlockInternal) tBlocks.add(tBlock);
		}
		if (tBlocks.isEmpty()) {
			// Молчаливый пропуск недопустим: он равен «инвентарей снаружи нет» без единого следа в логе.
			gregapi.data.CS.ERR.println("GT6 item-capability: MTE-блоков в реестре 0 — канал инвентаря НЕ зарегистрирован!");
			return;
		}
		aEvent.registerBlock(Capabilities.Item.BLOCK, GT6ItemCapability::handlerAt, tBlocks.toArray(new net.minecraft.world.level.block.Block[0]));
		gregapi.data.CS.OUT.println("GT6 item-capability: канал инвентаря зарегистрирован для " + tBlocks.size() + " MTE-блоков (Capabilities.Item.BLOCK).");
	}

	/** BlockEntity движок передаёт сам (может быть null, если его ещё нет). */
	private static ResourceHandler<ItemResource> handlerAt(net.minecraft.world.level.BlockGetter aLevel, net.minecraft.core.BlockPos aPos, net.minecraft.world.level.block.state.BlockState aState, net.minecraft.world.level.block.entity.BlockEntity aBlockEntity, Direction aSide) {
		try {
			// Пустой инвентарь — не то же самое, что его отсутствие: null означает «канала здесь нет».
			if (aBlockEntity instanceof Container tContainer && tContainer.getContainerSize() > 0) {
				if (aSide != null && aBlockEntity instanceof WorldlyContainer tWorldly) return new WorldlyContainerWrapper(tWorldly, aSide);
				return VanillaContainerWrapper.of(tContainer);
			}
		} catch (Throwable e) {/* логика конкретного TE не должна ронять чужой мод, который просто спросил капу */}
		return null;
	}
}

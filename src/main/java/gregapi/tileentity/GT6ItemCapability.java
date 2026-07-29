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
				if (aBlockEntity instanceof WorldlyContainer tWorldly) {
					WorldlyContainerWrapper tWrapper = new WorldlyContainerWrapper(tWorldly, aSide);
					return aSide == null ? new SidelessView(tWrapper, tWorldly) : tWrapper;
				}
				return VanillaContainerWrapper.of(tContainer);
			}
		} catch (Throwable e) {/* логика конкретного TE не должна ронять чужой мод, который просто спросил капу */}
		return null;
	}

	/**
	 * ЗАПРОС БЕЗ СТОРОНЫ — ТОЖЕ ПО ПРАВИЛАМ GT6 (BUG-082).
	 *
	 * <p><b>Что было.</b> При {@code side == null} канал отдавал {@code VanillaContainerWrapper.of(...)} — ВЕСЬ
	 * массив инвентаря, мимо фильтра мода. Наружу утекали служебные слоты, которые GT6 предметами не считает:
	 * дисплеи жидкостей ({@code MultiTileEntityBasicMachine:470-471} — {@code FL.display(...)}) и слот схемы.
	 * Отсюда витрина Jade, где содержимое машины показано дважды: сперва «предметами» (на деле дисплеи, в вёдрах),
	 * затем настоящим списком жидкостей. Jade спрашивает именно без стороны — {@code Jade/CommonProxy.java:290-297}.
	 *
	 * <p><b>Почему это дефект порта, а не движка.</b> У GT6 нет «инвентаря без стороны»: сторону 6 он называет
	 * {@code SIDE_ANY} и отвечает на неё сам ({@code MultiTileEntityBasicMachine.updateAccessibleSlots} заполняет
	 * {@code ACCESSIBLE[6]}, маски по умолчанию {@code 127} включают бит {@code SBIT_A=64} — {@code CS.java:646}).
	 * neo-{@code null} и есть эта сторона: {@code FORGE_DIR[6] = null} ({@code CS.java:687}), {@code UT.Code.side(null)=6}.
	 * Соседний канал ЖИДКОСТЕЙ так и сделан — {@code TileEntityBase01Root.getFluidTanksForCapability:766}. Предметный
	 * канал из этой пары выпадал: одна и та же задача решалась в моде двумя разными способами.
	 *
	 * <p><b>Что здесь есть и чего нет.</b> Здесь ТОЛЬКО выбор слотов — он берётся у самого мода
	 * ({@code getSlotsForFace(null)} → {@code TileEntityBase06Covers:322} → {@code getAccessibleSlotsFromSide2(6)},
	 * с учётом каверов). Перекладывание, транзакции и стек-лимиты остаются штатным {@link WorldlyContainerWrapper}
	 * — своей копии движковой механики тут нет. Форма класса — как у движкового {@code RangedResourceHandler}
	 * ({@code size}/{@code convertIndex} + оба безындексных метода перебором), только множество индексов не
	 * диапазон, а ответ мода.
	 */
	private static final class SidelessView extends net.neoforged.neoforge.transfer.DelegatingResourceHandler<ItemResource> {
		private final WorldlyContainer mContainer;

		SidelessView(WorldlyContainerWrapper aDelegate, WorldlyContainer aContainer) {super(aDelegate); mContainer = aContainer;}

		/** Каждый раз заново: набор доступного меняется живьём (поворот машины, надетый кавер). */
		private int[] slots() {
			int[] rSlots = mContainer.getSlotsForFace(null);
			return rSlots == null ? new int[0] : rSlots;
		}

		@Override public int size() {return slots().length;}

		@Override protected int convertIndex(int aIndex) {
			int[] tSlots = slots();
			java.util.Objects.checkIndex(aIndex, tSlots.length);
			return tSlots[aIndex];
		}

		/**
		 * Движковая обёртка при {@code side == null} свой {@code canTakeItemThroughFace} НЕ спрашивает
		 * ({@code WorldlyContainerWrapper:84-89} — проверка стоит под {@code side != null}), а GT6 её требует и для
		 * стороны 6: там же живёт запрет отдавать наружу служебные предметы ({@code TileEntityBase06Covers:341} —
		 * {@code ST.debug(aStack) → F}). Спрашиваем контракт самого мода, чужой политики не копируем.
		 */
		@Override public int extract(int aIndex, ItemResource aResource, int aAmount, net.neoforged.neoforge.transfer.transaction.TransactionContext aTransaction) {
			int tSlot = convertIndex(aIndex);
			if (!mContainer.canTakeItemThroughFace(tSlot, aResource.toStack(), null)) return 0;
			return super.extract(aIndex, aResource, aAmount, aTransaction);
		}

		@Override public int insert(ItemResource aResource, int aAmount, net.neoforged.neoforge.transfer.transaction.TransactionContext aTransaction) {
			int rInserted = 0;
			for (int i = 0, j = size(); i < j && rInserted < aAmount; i++) rInserted += insert(i, aResource, aAmount - rInserted, aTransaction);
			return rInserted;
		}

		@Override public int extract(ItemResource aResource, int aAmount, net.neoforged.neoforge.transfer.transaction.TransactionContext aTransaction) {
			int rExtracted = 0;
			for (int i = 0, j = size(); i < j && rExtracted < aAmount; i++) rExtracted += extract(i, aResource, aAmount - rExtracted, aTransaction);
			return rExtracted;
		}
	}
}

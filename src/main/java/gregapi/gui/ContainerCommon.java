/**
 * Copyright (c) 2025 GregTech-6 Team
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
 *
 * Modified in 2026 for the GregTech 6 NeoForge port
 * (https://github.com/wolfram0108/gregtech6_w): ported from Minecraft 1.7.10 / Forge
 * to Minecraft 26.1.2 / NeoForge.
 */

package gregapi.gui;

import gregapi.data.MD;
import gregapi.tileentity.ITileEntityGUI;
import gregapi.tileentity.ITileEntityInventoryGUI;
import gregapi.util.ST;
import gregapi.util.WD;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.ContainerListener;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.eventbus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;

import net.minecraftforge.registries.DeferredRegister;

import java.util.function.Supplier;

import static gregapi.data.CS.*;

/**
 * @author Gregorius Techneticies
 *
 * F-GUI (шов «GUI/меню», серверный центр закрыт этим файлом+{@link GT6MenuProvider}). 1.7.10
 * {@code player.openGui(mod,id,world,x,y,z)}→{@code IGuiHandler.getServerGuiElement}→{@code Container}
 * не существует в neo; движок-эквивалент — {@code player.openMenu(MenuProvider)}+{@code AbstractContainerMenu}
 * (см. {@link GT6MenuProvider}, {@code gregapi.tileentity.base.TileEntityBase01Root#openGUI}). Централизация
 * GT6 (маршрут id→{@code ITileEntityGUI.getGUIServer(id,player)}) СОХРАНЕНА 1:1 — и серверное открытие
 * ({@link GT6MenuProvider#createMenu}), и клиентская реконструкция контейнера ({@link #createFromNetwork})
 * зовут ТОТ ЖЕ {@code getGUIServer}, как было в оригинале (единственный центр маршрутизации).
 *
 * <p>Дословный перенос {@code transferStackInSlot}→{@link #quickMoveStack}, {@code canInteractWith}→
 * {@link #stillValid}, {@code mergeItemStack}→{@link #moveItemStackTo}, {@code slotClick}→{@link #clicked}
 * (движок сменил модель клика: {@code slotClick(index,mouse,shift,player)}→{@code clicked(index,button,
 * ContainerInput,player)}, `neo-decompiled/net/minecraft/world/inventory/AbstractContainerMenu.java:319`;
 * {@code ContainerInput} 1:1 повторяет старые int-коды {@code ClickType} — {@code PICKUP=0,QUICK_MOVE=1,
 * SWAP=2,CLONE=3,THROW=4,QUICK_CRAFT=5,PICKUP_ALL=6} (`ContainerInput.java:10-16`), поэтому
 * {@code aType.id()} = дословный старый {@code aShift}). Оригинальный {@code slotClick} вызывал
 * {@code super.slotClick(...)} и ВСЕГДА возвращался из try-блока без исключения — весь огромный
 * дублирующий ручной блок ниже (`aShift==0/1/2/3` реимплементация ванильного пикапа) был МЁРТВЫМ кодом
 * (срабатывал только при исключении из {@code super.slotClick}, которого 1.7.10 vanilla не бросает) —
 * не портируется (не потеря семантики: путь был недостижим), в новой модели его заменяет {@code super.clicked(...)}.
 */
public class ContainerCommon extends AbstractContainerMenu {

	// ==================================================================================================
	// F-GUI: единственный MenuType для ВСЕГО семейства ContainerCommon* (Default/Chest/BasicMachine) —
	// GT6 маршрутизирует по своему GUIID внутри одного Container-класса, а не по одному Menu на класс,
	// поэтому центр — ОДИН зарегистрированный тип (см. постановку задачи, п.3).
	// ==================================================================================================
	private static final DeferredRegister<MenuType<?>> MENU_TYPES = DeferredRegister.create(Registries.MENU, MD.GAPI.mID);

	/** ЕДИНСТВЕННЫЙ {@link MenuType} мода для {@link ContainerCommon} и его наследников (эталон паттерна
	 *  регистрации кодек/реестр-хелпера — {@code gregapi/enchants/EnchantsGT6.java:93-94}, тот же
	 *  {@code DeferredRegister.create(Registries.X, MD.GAPI.mID)}). {@code IMenuTypeExtension.create(IContainerFactory)}
	 *  — `neoforge-decompiled/net/neoforged/neoforge/common/extensions/IMenuTypeExtension.java:25-27`. */
	public static final net.minecraftforge.registries.RegistryObject<MenuType<ContainerCommon>> MENU_TYPE =
		MENU_TYPES.register("container_common", () -> IMenuTypeExtension.create(ContainerCommon::createFromNetwork));

	/** Стык для интегратора: вызвать рядом с {@code EnchantsGT6.register(aModBus)}/{@code GT6WorldgenFeature.register(aModBus)}
	 *  в {@code GT_API.java:345-348} (тот же мод-бас) — вне зоны этого захода, файл не тронут намеренно. */
	public static void register(IEventBus aModBus) {
		MENU_TYPES.register(aModBus);
	}

	/**
	 * Windowid-мост (форс движка): {@code AbstractContainerMenu.containerId} теперь {@code public final},
	 * задаётся ТОЛЬКО через {@code super(menuType, containerId)} в конструкторе (`AbstractContainerMenu.java:60,68`);
	 * 1.7.10 {@code Container.windowId} было мутируемым публичным полем, назначаемым ИЗВНЕ уже ПОСЛЕ
	 * конструирования ({@code EntityPlayerMP.openContainer}). Контракт {@code ITileEntityGUI.getGUIServer(id,player)}
	 * (сотни реализаций по всему моду, вне зоны этого захода) этот id не принимает и не может быть расширен
	 * без разрыва сотен файлов — вместо этого id передаётся через один статический слот на время ЕДИНСТВЕННОГО
	 * вызова {@code getGUIServer} из центральных точек входа ({@link #createFromNetwork}, {@link GT6MenuProvider#createMenu}),
	 * и подхватывается «легаси»-конструкторами ниже. Однопоточность: сервер выполняет открытие GUI в основном
	 * потоке (как и было в 1.7.10), реентерабельность не требуется.
	 */
	private static int sPendingWindowID = -1;

	public static <T> T withWindowID(int aWindowID, Supplier<T> aFactory) { // public: нужен и клиент-фабрике экранов (GT_API_Proxy_Client, containerId-мост)
		int tPrev = sPendingWindowID;
		sPendingWindowID = aWindowID;
		try {
			return aFactory.get();
		} finally {
			sPendingWindowID = tPrev;
		}
	}

	/**
	 * Клиентская реконструкция контейнера ({@link IContainerFactory#create}) — читает позицию TE + GUIID из
	 * буфера (записаны {@link GT6MenuProvider#createMenu} через {@code extraDataWriter}, см.
	 * {@code TileEntityBase01Root.openGUI}), находит TE через {@code WD.te(...)} и зовёт ТОТ ЖЕ
	 * {@code getGUIServer(guiId, player)}, что и серверное открытие — единый маршрут (постановка задачи, п.3).
	 */
	public static ContainerCommon createFromNetwork(int aWindowID, Inventory aInv, RegistryFriendlyByteBuf aData) {
		BlockPos tPos = aData.readBlockPos();
		int tGUIID = aData.readInt();
		BlockEntity tTileEntity = WD.te(aInv.player.level(), tPos, T);
		// null-BE (клиент ещё не получил синк — race server-place↔open): 1.7.10 getClientGuiElement=null тихо НЕ открывал
		// GUI; в neo null из фабрики = исключение пакет-хендлера = ДИСКОННЕКТ → не-null заглушка, экран закрывает себя сам.
		if (!(tTileEntity instanceof ITileEntityGUI)) return new ContainerCommon(aWindowID, aInv);
		final BlockEntity fTileEntity = tTileEntity;
		Object tGUI = withWindowID(aWindowID, () -> ((ITileEntityGUI)fTileEntity).getGUIServer(tGUIID, aInv.player));
		return tGUI instanceof ContainerCommon ? (ContainerCommon)tGUI : new ContainerCommon(aWindowID, aInv);
	}

	/** Заглушка null-реконструкции (см. {@link #createFromNetwork}): mTileEntity==null — маркер «GUI не открылся»,
	 *  {@link ContainerClient} закрывает такой экран на первом тике. */
	public ContainerCommon(int aWindowID, Inventory aInventoryPlayer) {
		super(MENU_TYPE.get(), aWindowID);
		mInventoryPlayer = aInventoryPlayer;
		mTileEntity = null;
		mSlotCount = 0; mOffset = 0; mGUIID = 0;
	}

	// Пакет-гейты заглушки: сервер-меню настоящее (N слотов) и шлёт content/data — у заглушки слотов 0, vanilla-приём
	// упал бы IndexOutOfBounds → «Network Protocol Error» (дисконнект). Глотаем до закрытия первым тиком экрана.
	@Override public void initializeContents(int aStateID, java.util.List<ItemStack> aItems, ItemStack aCarried) {
		if (mTileEntity == null && aItems.size() > slots.size()) return;
		super.initializeContents(aStateID, aItems, aCarried);
	}
	@Override public void setItem(int aSlot, int aStateID, ItemStack aStack) {
		if (mTileEntity == null && aSlot >= slots.size()) return;
		super.setItem(aSlot, aStateID, aStack);
	}
	@Override public void setData(int aID, int aValue) {
		if (mTileEntity == null) return;
		super.setData(aID, aValue);
	}

	public final int mOffset, mSlotCount, mGUIID;
	public ITileEntityInventoryGUI mTileEntity;
	public Inventory mInventoryPlayer;

	public ContainerCommon(Inventory aInventoryPlayer, ITileEntityInventoryGUI aTileEntity) {
		this(aInventoryPlayer, aTileEntity, 0);
	}
	public ContainerCommon(Inventory aInventoryPlayer, ITileEntityInventoryGUI aTileEntity, int aOffset, int aSlotCount) {
		this(aInventoryPlayer, aTileEntity, 0, aOffset, aSlotCount);
	}
	public ContainerCommon(Inventory aInventoryPlayer, ITileEntityInventoryGUI aTileEntity, int aGUIID) {
		this(aInventoryPlayer, aTileEntity, aGUIID, 0, aTileEntity.getSizeInventoryGUI());
	}
	/** Легаси-форма (без явного {@code MenuType}/{@code containerId}) — контракт, которым продолжают
	 *  пользоваться сотни {@code getGUIServer}-реализаций по всему моду (вне зоны этого захода); id
	 *  подхватывается из моста {@link #sPendingWindowID}. */
	public ContainerCommon(Inventory aInventoryPlayer, ITileEntityInventoryGUI aTileEntity, int aGUIID, int aOffset, int aSlotCount) {
		this(MENU_TYPE.get(), sPendingWindowID, aInventoryPlayer, aTileEntity, aGUIID, aOffset, aSlotCount);
	}
	/** Полная форма (постановка задачи, п.1): конструктор принимает {@code MenuType}+{@code containerId} и
	 *  зовёт {@code super(menuType, containerId)}. */
	public ContainerCommon(MenuType<?> aMenuType, int aWindowID, Inventory aInventoryPlayer, ITileEntityInventoryGUI aTileEntity, int aGUIID, int aOffset, int aSlotCount) {
		super(aMenuType, aWindowID);
		mInventoryPlayer = aInventoryPlayer;
		mTileEntity = aTileEntity;
		mTileEntity.openInventoryGUI();
		mSlotCount = aSlotCount;
		mOffset = aOffset;
		mGUIID = aGUIID;

		int tOffset = addSlots(aInventoryPlayer);
		if (doesBindPlayerInventory()) bindPlayerInventory(aInventoryPlayer, tOffset);
		broadcastChanges();
	}

	/**
	 * To add the Slots to your GUI
	 */
	public int addSlots(Inventory aPlayerInventory) {
		int i = mOffset;
		if (useDefaultSlots()) switch(mSlotCount) {
		case  1:
			addSlotToContainer(new Slot_Normal(mTileEntity, i++, 80, 35));
			break;
		case  2:
			addSlotToContainer(new Slot_Normal(mTileEntity, i++, 71, 35));
			addSlotToContainer(new Slot_Normal(mTileEntity, i++, 89, 35));
			break;
		case  3:
			addSlotToContainer(new Slot_Normal(mTileEntity, i++, 62, 35));
			addSlotToContainer(new Slot_Normal(mTileEntity, i++, 80, 35));
			addSlotToContainer(new Slot_Normal(mTileEntity, i++, 98, 35));
			break;
		case  4:
			addSlotToContainer(new Slot_Normal(mTileEntity, i++, 71, 26));
			addSlotToContainer(new Slot_Normal(mTileEntity, i++, 89, 26));
			addSlotToContainer(new Slot_Normal(mTileEntity, i++, 71, 44));
			addSlotToContainer(new Slot_Normal(mTileEntity, i++, 89, 44));
			break;
		case  5:
			addSlotToContainer(new Slot_Normal(mTileEntity, i++, 44, 35));
			addSlotToContainer(new Slot_Normal(mTileEntity, i++, 62, 35));
			addSlotToContainer(new Slot_Normal(mTileEntity, i++, 80, 35));
			addSlotToContainer(new Slot_Normal(mTileEntity, i++, 98, 35));
			addSlotToContainer(new Slot_Normal(mTileEntity, i++,116, 35));
			break;
		case  6:
			addSlotToContainer(new Slot_Normal(mTileEntity, i++, 62, 26));
			addSlotToContainer(new Slot_Normal(mTileEntity, i++, 80, 26));
			addSlotToContainer(new Slot_Normal(mTileEntity, i++, 98, 26));
			addSlotToContainer(new Slot_Normal(mTileEntity, i++, 62, 44));
			addSlotToContainer(new Slot_Normal(mTileEntity, i++, 80, 44));
			addSlotToContainer(new Slot_Normal(mTileEntity, i++, 98, 44));
			break;
		case  7:
			addSlotToContainer(new Slot_Normal(mTileEntity, i++, 26, 35));
			addSlotToContainer(new Slot_Normal(mTileEntity, i++, 44, 35));
			addSlotToContainer(new Slot_Normal(mTileEntity, i++, 62, 35));
			addSlotToContainer(new Slot_Normal(mTileEntity, i++, 80, 35));
			addSlotToContainer(new Slot_Normal(mTileEntity, i++, 98, 35));
			addSlotToContainer(new Slot_Normal(mTileEntity, i++,116, 35));
			addSlotToContainer(new Slot_Normal(mTileEntity, i++,134, 35));
			break;
		case  8:
			addSlotToContainer(new Slot_Normal(mTileEntity, i++, 53, 26));
			addSlotToContainer(new Slot_Normal(mTileEntity, i++, 71, 26));
			addSlotToContainer(new Slot_Normal(mTileEntity, i++, 89, 26));
			addSlotToContainer(new Slot_Normal(mTileEntity, i++,107, 26));
			addSlotToContainer(new Slot_Normal(mTileEntity, i++, 53, 44));
			addSlotToContainer(new Slot_Normal(mTileEntity, i++, 71, 44));
			addSlotToContainer(new Slot_Normal(mTileEntity, i++, 89, 44));
			addSlotToContainer(new Slot_Normal(mTileEntity, i++,107, 44));
			break;
		case  9:
			addSlotToContainer(new Slot_Normal(mTileEntity, i++, 62, 17));
			addSlotToContainer(new Slot_Normal(mTileEntity, i++, 80, 17));
			addSlotToContainer(new Slot_Normal(mTileEntity, i++, 98, 17));
			addSlotToContainer(new Slot_Normal(mTileEntity, i++, 62, 35));
			addSlotToContainer(new Slot_Normal(mTileEntity, i++, 80, 35));
			addSlotToContainer(new Slot_Normal(mTileEntity, i++, 98, 35));
			addSlotToContainer(new Slot_Normal(mTileEntity, i++, 62, 53));
			addSlotToContainer(new Slot_Normal(mTileEntity, i++, 80, 53));
			addSlotToContainer(new Slot_Normal(mTileEntity, i++, 98, 53));
			break;
		case 12:
			addSlotToContainer(new Slot_Normal(mTileEntity, i++, 35, 26));
			addSlotToContainer(new Slot_Normal(mTileEntity, i++, 53, 26));
			addSlotToContainer(new Slot_Normal(mTileEntity, i++, 71, 26));
			addSlotToContainer(new Slot_Normal(mTileEntity, i++, 89, 26));
			addSlotToContainer(new Slot_Normal(mTileEntity, i++,107, 26));
			addSlotToContainer(new Slot_Normal(mTileEntity, i++,125, 26));
			addSlotToContainer(new Slot_Normal(mTileEntity, i++, 35, 44));
			addSlotToContainer(new Slot_Normal(mTileEntity, i++, 53, 44));
			addSlotToContainer(new Slot_Normal(mTileEntity, i++, 71, 44));
			addSlotToContainer(new Slot_Normal(mTileEntity, i++, 89, 44));
			addSlotToContainer(new Slot_Normal(mTileEntity, i++,107, 44));
			addSlotToContainer(new Slot_Normal(mTileEntity, i++,125, 44));
			break;
		case 14:
			addSlotToContainer(new Slot_Normal(mTileEntity, i++, 26, 26));
			addSlotToContainer(new Slot_Normal(mTileEntity, i++, 44, 26));
			addSlotToContainer(new Slot_Normal(mTileEntity, i++, 62, 26));
			addSlotToContainer(new Slot_Normal(mTileEntity, i++, 80, 26));
			addSlotToContainer(new Slot_Normal(mTileEntity, i++, 98, 26));
			addSlotToContainer(new Slot_Normal(mTileEntity, i++,116, 26));
			addSlotToContainer(new Slot_Normal(mTileEntity, i++,134, 26));
			addSlotToContainer(new Slot_Normal(mTileEntity, i++, 26, 44));
			addSlotToContainer(new Slot_Normal(mTileEntity, i++, 44, 44));
			addSlotToContainer(new Slot_Normal(mTileEntity, i++, 62, 44));
			addSlotToContainer(new Slot_Normal(mTileEntity, i++, 80, 44));
			addSlotToContainer(new Slot_Normal(mTileEntity, i++, 98, 44));
			addSlotToContainer(new Slot_Normal(mTileEntity, i++,116, 44));
			addSlotToContainer(new Slot_Normal(mTileEntity, i++,134, 44));
			break;
		case 15:
			addSlotToContainer(new Slot_Normal(mTileEntity, i++, 44, 17));
			addSlotToContainer(new Slot_Normal(mTileEntity, i++, 62, 17));
			addSlotToContainer(new Slot_Normal(mTileEntity, i++, 80, 17));
			addSlotToContainer(new Slot_Normal(mTileEntity, i++, 98, 17));
			addSlotToContainer(new Slot_Normal(mTileEntity, i++,116, 17));
			addSlotToContainer(new Slot_Normal(mTileEntity, i++, 44, 35));
			addSlotToContainer(new Slot_Normal(mTileEntity, i++, 62, 35));
			addSlotToContainer(new Slot_Normal(mTileEntity, i++, 80, 35));
			addSlotToContainer(new Slot_Normal(mTileEntity, i++, 98, 35));
			addSlotToContainer(new Slot_Normal(mTileEntity, i++,116, 35));
			addSlotToContainer(new Slot_Normal(mTileEntity, i++, 44, 53));
			addSlotToContainer(new Slot_Normal(mTileEntity, i++, 62, 53));
			addSlotToContainer(new Slot_Normal(mTileEntity, i++, 80, 53));
			addSlotToContainer(new Slot_Normal(mTileEntity, i++, 98, 53));
			addSlotToContainer(new Slot_Normal(mTileEntity, i++,116, 53));
			break;
		case 16:
			addSlotToContainer(new Slot_Normal(mTileEntity, i++, 53,  8));
			addSlotToContainer(new Slot_Normal(mTileEntity, i++, 71,  8));
			addSlotToContainer(new Slot_Normal(mTileEntity, i++, 89,  8));
			addSlotToContainer(new Slot_Normal(mTileEntity, i++,107,  8));
			addSlotToContainer(new Slot_Normal(mTileEntity, i++, 53, 26));
			addSlotToContainer(new Slot_Normal(mTileEntity, i++, 71, 26));
			addSlotToContainer(new Slot_Normal(mTileEntity, i++, 89, 26));
			addSlotToContainer(new Slot_Normal(mTileEntity, i++,107, 26));
			addSlotToContainer(new Slot_Normal(mTileEntity, i++, 53, 44));
			addSlotToContainer(new Slot_Normal(mTileEntity, i++, 71, 44));
			addSlotToContainer(new Slot_Normal(mTileEntity, i++, 89, 44));
			addSlotToContainer(new Slot_Normal(mTileEntity, i++,107, 44));
			addSlotToContainer(new Slot_Normal(mTileEntity, i++, 53, 62));
			addSlotToContainer(new Slot_Normal(mTileEntity, i++, 71, 62));
			addSlotToContainer(new Slot_Normal(mTileEntity, i++, 89, 62));
			addSlotToContainer(new Slot_Normal(mTileEntity, i++,107, 62));
			break;
		case 18:
			addSlotToContainer(new Slot_Normal(mTileEntity, i++,  8, 26));
			addSlotToContainer(new Slot_Normal(mTileEntity, i++, 26, 26));
			addSlotToContainer(new Slot_Normal(mTileEntity, i++, 44, 26));
			addSlotToContainer(new Slot_Normal(mTileEntity, i++, 62, 26));
			addSlotToContainer(new Slot_Normal(mTileEntity, i++, 80, 26));
			addSlotToContainer(new Slot_Normal(mTileEntity, i++, 98, 26));
			addSlotToContainer(new Slot_Normal(mTileEntity, i++,116, 26));
			addSlotToContainer(new Slot_Normal(mTileEntity, i++,134, 26));
			addSlotToContainer(new Slot_Normal(mTileEntity, i++,152, 26));
			addSlotToContainer(new Slot_Normal(mTileEntity, i++,  8, 44));
			addSlotToContainer(new Slot_Normal(mTileEntity, i++, 26, 44));
			addSlotToContainer(new Slot_Normal(mTileEntity, i++, 44, 44));
			addSlotToContainer(new Slot_Normal(mTileEntity, i++, 62, 44));
			addSlotToContainer(new Slot_Normal(mTileEntity, i++, 80, 44));
			addSlotToContainer(new Slot_Normal(mTileEntity, i++, 98, 44));
			addSlotToContainer(new Slot_Normal(mTileEntity, i++,116, 44));
			addSlotToContainer(new Slot_Normal(mTileEntity, i++,134, 44));
			addSlotToContainer(new Slot_Normal(mTileEntity, i++,152, 44));
			break;
		case 27:
			addSlotToContainer(new Slot_Normal(mTileEntity, i++,  8, 17));
			addSlotToContainer(new Slot_Normal(mTileEntity, i++, 26, 17));
			addSlotToContainer(new Slot_Normal(mTileEntity, i++, 44, 17));
			addSlotToContainer(new Slot_Normal(mTileEntity, i++, 62, 17));
			addSlotToContainer(new Slot_Normal(mTileEntity, i++, 80, 17));
			addSlotToContainer(new Slot_Normal(mTileEntity, i++, 98, 17));
			addSlotToContainer(new Slot_Normal(mTileEntity, i++,116, 17));
			addSlotToContainer(new Slot_Normal(mTileEntity, i++,134, 17));
			addSlotToContainer(new Slot_Normal(mTileEntity, i++,152, 17));
			addSlotToContainer(new Slot_Normal(mTileEntity, i++,  8, 35));
			addSlotToContainer(new Slot_Normal(mTileEntity, i++, 26, 35));
			addSlotToContainer(new Slot_Normal(mTileEntity, i++, 44, 35));
			addSlotToContainer(new Slot_Normal(mTileEntity, i++, 62, 35));
			addSlotToContainer(new Slot_Normal(mTileEntity, i++, 80, 35));
			addSlotToContainer(new Slot_Normal(mTileEntity, i++, 98, 35));
			addSlotToContainer(new Slot_Normal(mTileEntity, i++,116, 35));
			addSlotToContainer(new Slot_Normal(mTileEntity, i++,134, 35));
			addSlotToContainer(new Slot_Normal(mTileEntity, i++,152, 35));
			addSlotToContainer(new Slot_Normal(mTileEntity, i++,  8, 53));
			addSlotToContainer(new Slot_Normal(mTileEntity, i++, 26, 53));
			addSlotToContainer(new Slot_Normal(mTileEntity, i++, 44, 53));
			addSlotToContainer(new Slot_Normal(mTileEntity, i++, 62, 53));
			addSlotToContainer(new Slot_Normal(mTileEntity, i++, 80, 53));
			addSlotToContainer(new Slot_Normal(mTileEntity, i++, 98, 53));
			addSlotToContainer(new Slot_Normal(mTileEntity, i++,116, 53));
			addSlotToContainer(new Slot_Normal(mTileEntity, i++,134, 53));
			addSlotToContainer(new Slot_Normal(mTileEntity, i++,152, 53));
			break;
		case 36:
			addSlotToContainer(new Slot_Normal(mTileEntity, i++,  8,  8));
			addSlotToContainer(new Slot_Normal(mTileEntity, i++, 26,  8));
			addSlotToContainer(new Slot_Normal(mTileEntity, i++, 44,  8));
			addSlotToContainer(new Slot_Normal(mTileEntity, i++, 62,  8));
			addSlotToContainer(new Slot_Normal(mTileEntity, i++, 80,  8));
			addSlotToContainer(new Slot_Normal(mTileEntity, i++, 98,  8));
			addSlotToContainer(new Slot_Normal(mTileEntity, i++,116,  8));
			addSlotToContainer(new Slot_Normal(mTileEntity, i++,134,  8));
			addSlotToContainer(new Slot_Normal(mTileEntity, i++,152,  8));
			addSlotToContainer(new Slot_Normal(mTileEntity, i++,  8, 26));
			addSlotToContainer(new Slot_Normal(mTileEntity, i++, 26, 26));
			addSlotToContainer(new Slot_Normal(mTileEntity, i++, 44, 26));
			addSlotToContainer(new Slot_Normal(mTileEntity, i++, 62, 26));
			addSlotToContainer(new Slot_Normal(mTileEntity, i++, 80, 26));
			addSlotToContainer(new Slot_Normal(mTileEntity, i++, 98, 26));
			addSlotToContainer(new Slot_Normal(mTileEntity, i++,116, 26));
			addSlotToContainer(new Slot_Normal(mTileEntity, i++,134, 26));
			addSlotToContainer(new Slot_Normal(mTileEntity, i++,152, 26));
			addSlotToContainer(new Slot_Normal(mTileEntity, i++,  8, 44));
			addSlotToContainer(new Slot_Normal(mTileEntity, i++, 26, 44));
			addSlotToContainer(new Slot_Normal(mTileEntity, i++, 44, 44));
			addSlotToContainer(new Slot_Normal(mTileEntity, i++, 62, 44));
			addSlotToContainer(new Slot_Normal(mTileEntity, i++, 80, 44));
			addSlotToContainer(new Slot_Normal(mTileEntity, i++, 98, 44));
			addSlotToContainer(new Slot_Normal(mTileEntity, i++,116, 44));
			addSlotToContainer(new Slot_Normal(mTileEntity, i++,134, 44));
			addSlotToContainer(new Slot_Normal(mTileEntity, i++,152, 44));
			addSlotToContainer(new Slot_Normal(mTileEntity, i++,  8, 62));
			addSlotToContainer(new Slot_Normal(mTileEntity, i++, 26, 62));
			addSlotToContainer(new Slot_Normal(mTileEntity, i++, 44, 62));
			addSlotToContainer(new Slot_Normal(mTileEntity, i++, 62, 62));
			addSlotToContainer(new Slot_Normal(mTileEntity, i++, 80, 62));
			addSlotToContainer(new Slot_Normal(mTileEntity, i++, 98, 62));
			addSlotToContainer(new Slot_Normal(mTileEntity, i++,116, 62));
			addSlotToContainer(new Slot_Normal(mTileEntity, i++,134, 62));
			addSlotToContainer(new Slot_Normal(mTileEntity, i++,152, 62));
			break;
		}
		return 84;
	}

	public boolean useDefaultSlots() {
		return F;
	}

	/**
	 * Amount of regular Slots in the GUI (so, non-HoloSlots)
	 */
	public int getSlotCount() {return useDefaultSlots() ? mSlotCount : 0;}

	/**
	 * Amount of ALL Slots in the GUI including HoloSlots and ArmorSlots, but excluding regular Player Slots
	 */
	protected final int getAllSlotCount() {
		return slots!=null?doesBindPlayerInventory()?slots.size()-36:slots.size():getSlotCount();
	}

	/**
	 * Start-Index of the usable Slots (the first non-HoloSlot)
	 */
	public int getStartIndex() {return 0;}

	public int getShiftClickStartIndex() {return getStartIndex();}

	/**
	 * Amount of Slots in the GUI the player can Shift-Click into. Uses also getSlotStartIndex
	 */
	public int getShiftClickSlotCount() {return useDefaultSlots() ? mSlotCount : 0;}

	/**
	 * Is Player-Inventory visible?
	 */
	public boolean doesBindPlayerInventory() {return T;}

	@Override public boolean stillValid(Player aPlayer) {return mTileEntity != null && mTileEntity.isUseableByPlayerGUI(aPlayer);}

	protected void bindPlayerInventory(Inventory aInventoryPlayer, int aOffset) {
		for (int i = 0; i < 3; i++) for (int j = 0; j < 9; j++) {
			addSlotToContainer(new Slot(aInventoryPlayer, j + i * 9 + 9, 8 + j * 18, aOffset + i * 18));
		}
		for (int i = 0; i < 9; i++) {
			addSlotToContainer(new Slot(aInventoryPlayer, i, 8 + i * 18, aOffset + 58));
		}
	}

	/**
	 * F-GUI: движок сменил модель клика — {@code slotClick(index,mouse,shift,player):ItemStack} (1.7.10)
	 * → {@code clicked(index,button,ContainerInput,player):void} (`AbstractContainerMenu.java:319`).
	 * {@code aType.id()} — дословный старый {@code aShift} (см. javadoc класса). Перехват GT6
	 * ({@code interceptClick}/{@code slotClick} на TE) и гейтинг Holo/Armor-слотов — 1:1 из оригинала;
	 * дальше маршрут отдаётся движку ({@code super.clicked}, делает то же, что раньше делал
	 * {@code super.slotClick} в единственном РЕАЛЬНО достижимом пути оригинала). Оригинал (`gregtech6/.../
	 * ContainerCommon.java:343-345`) возвращал результат {@code slotClick(...)} как значение ванильного
	 * {@code Container.slotClick} — это была СЕТЕВАЯ СВЕРКА транзакции, а НЕ команда курсору: сам курсор
	 * ({@code aPlayer.inventory.itemStack}) мутировался только явными {@code setItemStack(...)} ВНУТРИ {@code slotClick}.
	 * Поэтому neo-мост курсор из возврата НЕ выставляет (см. BUG-027 в {@link #clicked}); TE-{@code slotClick} сам
	 * зовёт {@code setCarried(...)} где надо, а {@code broadcastChanges()} синхронизирует его на клиент.
	 */
	@Override
	public void clicked(int aIndex, int aMouse, ContainerInput aType, Player aPlayer) {
		mTileEntity.markDirtyGUI();
		Slot aSlot = (aIndex >= 0 && aIndex < slots.size()) ? getSlot(aIndex) : null;
		int aShift = aType.id();

		try {
			if (aSlot instanceof Slot_Base && mTileEntity.interceptClick(mGUIID, (Slot_Base)aSlot, aIndex, aSlot.getSlotIndex(), aPlayer, aShift == 1, aMouse != 0, aMouse, aShift)) {
				// BUG-027: slotClick САМ мутирует курсор через setCarried там, где надо (1.7.10: aPlayer.inventory.setItemStack;
				// напр. MultiTileEntityAdvancedCraftingTable.slotClick:642/647). Его ВОЗВРАТ — 1.7.10 sync-значение ванильного
				// Container.slotClick (сетевая сверка транзакции), НЕ императив «поставить это в руку»: ветки, не менявшие курсор,
				// возвращали null = «курсор не трогали». Прежний setCarried(ST.nn(rStack)) для них делал setCarried(EMPTY) и
				// СТИРАЛ предмет с курсора (клик по выходу крафта на пустой сетке / по служебным кнопкам стола Грега, invSlot 31/32).
				mTileEntity.slotClick(mGUIID, (Slot_Base)aSlot, aIndex, aSlot.getSlotIndex(), aPlayer, aShift == 1, aMouse != 0, aMouse, aShift);
				broadcastChanges();
				return;
			}
		} catch (Throwable e) {e.printStackTrace(ERR); return;}

		if (aIndex >= 0) {
			if (aSlot == null || aSlot instanceof Slot_Holo) return;
			if (!(aSlot instanceof Slot_Armor)) if (aIndex < getAllSlotCount()) if (aIndex < getStartIndex() || aIndex >= getStartIndex() + getSlotCount()) return;
		}

		try {
			super.clicked(aIndex, aMouse, aType, aPlayer);
		} catch (Throwable e) {
			e.printStackTrace(ERR);
		}
		broadcastChanges();
	}

	/** F-GUI: {@code transferStackInSlot}→{@code quickMoveStack} (движок, абстрактный метод
	 *  `AbstractContainerMenu.java:310`); {@code null}→{@code ItemStack.EMPTY} (движок, ItemStack больше
	 *  не бывает null — `doClick` вызывающего кода проверяет {@code .isEmpty()}). */
	@Override
	public ItemStack quickMoveStack(Player aPlayer, int aIndex) {
		ItemStack rStack = ItemStack.EMPTY;
		Slot tSlot = slots.get(aIndex);

		mTileEntity.markDirtyGUI();

		// null checks and checks if the item can be stacked (maxStackSize > 1)
		if (getSlotCount() > 0 && tSlot != null && tSlot.hasItem() && !(tSlot instanceof Slot_Holo)) {
			ItemStack tStack = tSlot.getItem();
			rStack = ST.copy(tStack);

			// TileEntity -> Player
			if (aIndex < getAllSlotCount()) {
				if (doesBindPlayerInventory() && !moveItemStackTo(tStack, getAllSlotCount(), getAllSlotCount()+36, T)) {
					return ItemStack.EMPTY;
				}
			// Player -> TileEntity
			} else if (!moveItemStackTo(tStack, getShiftClickStartIndex(), getShiftClickStartIndex()+getShiftClickSlotCount(), F)) {
				return ItemStack.EMPTY;
			}

			if (tStack.getCount() == 0) tSlot.set(ItemStack.EMPTY); else tSlot.setChanged();
		}
		return rStack;
	}

	/** F-GUI: {@code mergeItemStack}→{@code moveItemStackTo} (движок переименовал, тот же протектед-хук,
	 *  `AbstractContainerMenu.java:648`); {@code isItemValid}→{@code mayPlace}, {@code getStack}→
	 *  {@code getItem}, {@code putStack}→{@code set}, {@code onSlotChanged}→{@code setChanged},
	 *  {@code getSlotStackLimit}→{@code getMaxStackSize} (движок, см. {@link Slot_Base}). Логика (Wildcard-мета
	 *  ранний выход, {@code markDirtyGUI}, исключение {@code Slot_Holo}, {@code ST.equal}-сравнение вместо
	 *  ванильного) — дословно из оригинала, это НЕ ванильный мёрдж, а GT6-кастом.
	 */
	@Override
	protected boolean moveItemStackTo(ItemStack aStack, int aStartIndex, int aSlotCount, boolean aReverse) {
		if (ST.meta(aStack) == W) return F;

		boolean rSuccess = F;
		int tIndex = aReverse?aSlotCount-1:aStartIndex;

		mTileEntity.markDirtyGUI();

		if (aStack.isStackable()) {
			while (aStack.getCount() > 0 && (aReverse ? tIndex >= aStartIndex : tIndex < aSlotCount)) {
				Slot tSlot = slots.get(tIndex);
				int tLimit = Math.min(aStack.getMaxStackSize(), tSlot.getMaxStackSize());
				ItemStack tStack = tSlot.getItem();
				if (!(tSlot instanceof Slot_Holo) && tSlot.mayPlace(aStack) && ST.meta(tStack) != W && ST.equal(aStack, tStack)) {
					int tSize = tStack.getCount() + aStack.getCount();
					if (tSize <= tLimit) {
						aStack.setCount(0);
						tStack.setCount(tSize);
						tSlot.setChanged();
						rSuccess = T;
					} else if (tStack.getCount() < tLimit) {
						aStack.setCount(aStack.getCount()-(tLimit - tStack.getCount()));
						tStack.setCount(tLimit);
						tSlot.setChanged();
						rSuccess = T;
					}
				}
				if (aReverse) tIndex--; else tIndex++;
			}
		}
		if (aStack.getCount() > 0) {
			if (aReverse) tIndex = aSlotCount - 1; else tIndex = aStartIndex;
			while (aReverse ? tIndex >= aStartIndex : tIndex < aSlotCount) {
				Slot tSlot = slots.get(tIndex);
				if (!(tSlot instanceof Slot_Holo) && tSlot.mayPlace(aStack)) {
					if (!tSlot.hasItem()) {
						ItemStack tStack = ST.amount(Math.min(aStack.getCount(), Math.min(aStack.getMaxStackSize(), tSlot.getMaxStackSize())), aStack);
						tSlot.set(tStack);
						tSlot.setChanged();
						aStack.setCount(aStack.getCount()-(tStack.getCount()));
						rSuccess = T;
						if (aStack.getCount() <= 0) break;
					}
				}
				if (aReverse) tIndex--; else tIndex++;
			}
		}
		return rSuccess;
	}

	/** Не {@code @Override} — движок больше не знает {@code addSlotToContainer} (переименован в
	 *  {@code addSlot}, `AbstractContainerMenu.java:124`); имя и null-гейт (используется
	 *  {@code ContainerCommonBasicMachine} для опциональных спец-слотов) сохранены дословно. */
	protected Slot addSlotToContainer(Slot aSlot) {
		if (aSlot == null) return null;
		return addSlot(aSlot);
	}

	/** F-GUI: {@code addCraftingToCrafters}→{@code addSlotListener}, {@code ICrafting}→{@code ContainerListener} (движок). */
	@Override
	public void addSlotListener(ContainerListener aListener) {
		try {
			super.addSlotListener(aListener);
		} catch(Throwable e) {
			e.printStackTrace(ERR);
		}
	}

	/** F-GUI: {@code getInventory():List<ItemStack>}→{@code getItems():NonNullList<ItemStack>} (движок переименовал+сузил тип). */
	@Override
	public NonNullList<ItemStack> getItems() {
		try {
			return super.getItems();
		} catch(Throwable e) {
			e.printStackTrace(ERR);
		}
		return null;
	}

	/** F-GUI: {@code removeCraftingFromCrafters}→{@code removeSlotListener}, {@code ICrafting}→{@code ContainerListener} (движок). */
	@Override
	public void removeSlotListener(ContainerListener aListener) {
		try {
			super.removeSlotListener(aListener);
		} catch(Throwable e) {
			e.printStackTrace(ERR);
		}
	}

	/** F-GUI: {@code detectAndSendChanges}→{@code broadcastChanges} (движок переименовал). GT6-кастом
	 *  «{@code needsToSyncEverything} → отправить ВСЕ слоты без сравнения» больше не портируем ручной
	 *  реимплементацией (движок спрятал {@code lastSlots}/{@code remoteSlots} — стали {@code private}),
	 *  а зовём готовый движковый эквивалент той же роли — {@code broadcastFullState()}
	 *  (`AbstractContainerMenu.java:216`, безусловно ре-триггерит слушателей для ВСЕХ слотов, как и
	 *  оригинал), иначе — обычный {@code super.broadcastChanges()} (change-detected путь, как раньше). */
	@Override
	public void broadcastChanges() {
		try {
			if (mTileEntity.needsToSyncEverything()) {
				broadcastFullState();
			} else {
				super.broadcastChanges();
			}
		} catch(Throwable e) {
			e.printStackTrace(ERR);
		}
	}

	/** F-GUI: {@code getSlotFromInventory(IInventory,int)} движок убрал целиком (заменил на
	 *  {@code findSlot(Container,int):OptionalInt}, работающий ТОЛЬКО через базовые поля {@code container}/
	 *  {@code getContainerSlot()} — непригодно для {@link Slot_Base}, чьи слоты сверяются через собственные
	 *  {@code mInventory}/{@code mIndex}, см. {@link Slot_Base#isSlotInInventory}). Не {@code @Override}
	 *  (цели в базе больше нет) — имя/структура сохранены, для {@link Slot_Base} зовём его собственный
	 *  {@code isSlotInInventory}, для обычных {@code Slot} (слоты игрока, {@link #bindPlayerInventory}) —
	 *  сравнение через {@code container}/{@code getContainerSlot()} (те самые поля, что раньше проверяла
	 *  базовая {@code Slot.isSlotInInventory}). {@code IInventory}(1.7.10)→{@code Container} (движок). */
	public Slot getSlotFromInventory(Container aInventory, int aIndex) {
		try {
			for (int j = 0; j < slots.size(); ++j) {
				Slot slot = slots.get(j);
				if (slot instanceof Slot_Base ? ((Slot_Base)slot).isSlotInInventory(aInventory, aIndex) : (slot.container == aInventory && slot.getContainerSlot() == aIndex)) return slot;
			}
		} catch(Throwable e) {
			e.printStackTrace(ERR);
		}
		return null;
	}

	@Override
	public Slot getSlot(int aIndex) {
		try {
			if (aIndex < slots.size()) return slots.get(aIndex);
		} catch(Throwable e) {
			e.printStackTrace(ERR);
		}
		return null;
	}

	/** F-GUI: {@code retrySlotClick} движок убрал целиком (двойной клик = PICKUP_ALL теперь обрабатывает
	 *  сам {@code doClick}, вызванный через {@code super.clicked(...)} в {@link #clicked}) — форс, нет цели
	 *  для переноса. */

	/** F-GUI (доработка R8): {@code onContainerClosed}→{@code removed} (движок переименовал,
	 *  `AbstractContainerMenu.java:598`). 1:1 перенос оригинала (`gregtech6/.../ContainerCommon.java:680-696`):
	 *  два ST.check-цикла (main+armor) + возврат/сброс «зажатого мышью» стека — {@code НЕ} зовём
	 *  {@code super.removed(player)} (тот делает place-back-или-drop без {@code ST.check}, ДРУГОЕ поведение,
	 *  исказило бы 1:1). Carried-стек: {@code InventoryPlayer.getItemStack()}→{@code getCarried()}
	 *  (`AbstractContainerMenu.java:783`), {@code setItemStack(null)}→{@code setCarried(ItemStack.EMPTY)}
	 *  (`:779`), {@code dropPlayerItemWithRandomChoice(stack,F)}→{@code Player.drop(ItemStack,boolean)}
	 *  (`neo-decompiled/net/minecraft/world/entity/player/Player.java:592`, тот же второй аргумент {@code F}).
	 *  Проверка присутствия carried — через F15-канон {@code ST.ni(getCarried()) != null}
	 *  (`gregapi/util/ST.java:154`), НЕ {@code getCarried().isEmpty()} — последний схлопнул бы значимый
	 *  {@code count==0} (не то же самое, что синглтон {@code ItemStack.EMPTY}) в «пусто», а оригинал
	 *  {@code getItemStack() != null} этот случай считал присутствующим.
	 *  Сканирование инвентаря на debug-предметы ({@code ST.check}) — 1:1, {@code mainInventory}→
	 *  {@code getInventory().items} (`Inventory.java:90`), {@code armorInventory}→
	 *  {@code getItemBySlot(EquipmentSlot)} по тому же порядку FEET/LEGS/CHEST/HEAD, что уже установлен
	 *  центром брони (`gregapi/GT_API_Proxy.java:994`). */
	private static final EquipmentSlot[] ARMOR_SLOTS = {EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD};

	@Override
	public void removed(Player aPlayer) {
		try {
			mTileEntity.closeInventoryGUI();
			for (ItemStack tStack : aPlayer.getInventory().items) {
				ST.check(aPlayer, tStack);
			}
			for (EquipmentSlot tArmorSlot : ARMOR_SLOTS) {
				ItemStack tStack = aPlayer.getItemBySlot(tArmorSlot);
				if (ST.valid(tStack)) ST.check(aPlayer, tStack);
			}
			if (ST.ni(getCarried()) != null) { // было getItemStack() != null; F15-канон ST.ni (не isEmpty() — тот схлопывает значимый count==0)
				ST.check(aPlayer, getCarried());
				aPlayer.drop(getCarried(), false); // было dropPlayerItemWithRandomChoice(carried, F)
				setCarried(ItemStack.EMPTY); // было setItemStack(null)
			}
		} catch(Throwable e) {
			e.printStackTrace(ERR);
		}
	}

	@Override
	public void slotsChanged(Container aInventory) {
		broadcastChanges();
	}

	@Override
	public boolean canDragTo(Slot aSlot) {
		return T;
	}

	/*
	 * F-GUI — форс-удаления (методы, у которых движок убрал целевой хук БЕЗ замены; тела оригинала были
	 * либо no-op, либо ровно совпадали с новым дефолтом движка — семантика не теряется):
	 *  - enchantItem(Player,int){return F;} — цели-хука в neo нет; movern-аналог clickMenuButton(Player,int)
	 *    (`AbstractContainerMenu.java:302`) уже по умолчанию возвращает false — то же поведение.
	 *  - func_94530_a(ItemStack,Slot){return T;} — 1.7.10 pickup-all-гейт; совпадает по роли и умолчанию
	 *    с новым canTakeItemForPickAll(ItemStack,Slot){return true;} (`:594`) — не переопределяем (то же).
	 *  - retrySlotClick(int,int,boolean,Player) — двойной клик (pickup-all) теперь целиком внутри движкового
	 *    doClick/ContainerInput.PICKUP_ALL (`:536-558`), вызываемого через super.clicked(...) в {@link #clicked}.
	 *  - putStackInSlot(int,ItemStack)/putStacksInSlots(ItemStack[]) — старый ручной bulk-sync при открытии
	 *    GUI; движок заменил на setItem(int,int,ItemStack)/initializeContents(int,List,ItemStack)
	 *    (`:628-640`), которые уже разумно реализованы в базе — по имени переопределять нечего.
	 *  - updateProgressBar(int,int) (пустой стаб на этом уровне) — движок заменил int-индексный прогресс-бар
	 *    на DataSlot+setData(int,int) (`:642-644`); ContainerCommonBasicMachine теперь переопределяет setData.
	 *  - getNextTransactionID(InventoryPlayer)/isPlayerNotUsingContainer(Player)/setPlayerIsPresent(Player,boolean)
	 *    — старая transaction-id/desync-схема сети 1.7.10 удалена движком целиком, замены нет и не нужно
	 *    (современная сеть синхронизирует через stateId/HashedStack, `:58,829-836`, внутри самого движка).
	 */
}

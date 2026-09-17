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
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ContainerListener;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.common.extensions.IForgeMenuType;

import net.minecraftforge.registries.DeferredRegister;

import java.util.function.Supplier;

import static gregapi.data.CS.*;

/** @author Gregorius Techneticies
 *  The old player.openGui/IGuiHandler path does not exist in neo; the equivalent is player.openMenu
 *  (MenuProvider) (see {@link GT6MenuProvider}), routed through the same GT6 id-based getGUIServer center. */
public class ContainerCommon extends AbstractContainerMenu {

	// ==================================================================================================
	// One registered MenuType covers the whole ContainerCommon family, since GT6 routes by its own GUI id
	// inside a single container class rather than using one menu type per class.
	// ==================================================================================================
	private static final DeferredRegister<MenuType<?>> MENU_TYPES = DeferredRegister.create(Registries.MENU, MD.GAPI.mID);

	/** The only MenuType for ContainerCommon and everything that extends it. */
	public static final net.minecraftforge.registries.RegistryObject<MenuType<ContainerCommon>> MENU_TYPE =
		MENU_TYPES.register("container_common", () -> IForgeMenuType.create(ContainerCommon::createFromNetwork));

	/** Must be called alongside EnchantsGT6.register and GT6WorldgenFeature.register on the same mod bus,
	 *  but is not wired in from GT_API yet. */
	public static void register(IEventBus aModBus) {
		MENU_TYPES.register(aModBus);
	}

	/** containerId is now assigned only through the constructor and can't be set from outside afterward;
	 *  passed instead through a static slot held open for the single getGUIServer call from the entry points. */
	private static int sPendingWindowID = -1;

	public static <T> T withWindowID(int aWindowID, Supplier<T> aFactory) { // Public because the client screen factory (GT_API_Proxy_Client) needs it too for the containerId bridge.
		int tPrev = sPendingWindowID;
		sPendingWindowID = aWindowID;
		try {
			return aFactory.get();
		} finally {
			sPendingWindowID = tPrev;
		}
	}

	/** Client-side reconstruction reads the tile entity position and GUI id from the network buffer and
	 *  calls the same getGUIServer route the server used, keeping one shared routing path on both sides. */
	public static ContainerCommon createFromNetwork(int aWindowID, Inventory aInv, FriendlyByteBuf aData) {
		BlockPos tPos = aData.readBlockPos();
		int tGUIID = aData.readInt();
		BlockEntity tTileEntity = WD.te(aInv.player.level(), tPos, T);
		// A null client-side factory result now throws in the packet handler and disconnects the player instead
		// of silently not opening the GUI, so a stub container is returned that closes itself instead.
		if (!(tTileEntity instanceof ITileEntityGUI)) return new ContainerCommon(aWindowID, aInv);
		final BlockEntity fTileEntity = tTileEntity;
		Object tGUI = withWindowID(aWindowID, () -> ((ITileEntityGUI)fTileEntity).getGUIServer(tGUIID, aInv.player));
		return tGUI instanceof ContainerCommon ? (ContainerCommon)tGUI : new ContainerCommon(aWindowID, aInv);
	}

	/** Stub for the null-reconstruction case: a null tile entity marks a GUI that failed to open. */
	public ContainerCommon(int aWindowID, Inventory aInventoryPlayer) {
		super(MENU_TYPE.get(), aWindowID);
		mInventoryPlayer = aInventoryPlayer;
		mTileEntity = null;
		mSlotCount = 0; mOffset = 0; mGUIID = 0;
	}

	// A zero-slot stub menu would otherwise throw IndexOutOfBounds against the real server-side item list
	// and disconnect the player; swallowed here until the client closes the screen on its first tick.
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
	/** Legacy form used by every existing getGUIServer implementation; id comes from the {@link #sPendingWindowID} bridge. */
	public ContainerCommon(Inventory aInventoryPlayer, ITileEntityInventoryGUI aTileEntity, int aGUIID, int aOffset, int aSlotCount) {
		this(MENU_TYPE.get(), sPendingWindowID, aInventoryPlayer, aTileEntity, aGUIID, aOffset, aSlotCount);
	}
	/** Full form taking an explicit MenuType and containerId. */
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

	/** The engine replaced slotClick(index,mouse,shift,player) with clicked(index,button,ContainerInput,player);
	 *  the original's return value was a network sync check, not a cursor command, so it is not applied to
	 *  the carried stack here, matching what the one reachable original code path actually did. */
	@Override
	public void clicked(int aIndex, int aMouse, ClickType aType, Player aPlayer) {
		mTileEntity.markDirtyGUI();
		Slot aSlot = (aIndex >= 0 && aIndex < slots.size()) ? getSlot(aIndex) : null;
		int aShift = aType.ordinal();

		try {
			if (aSlot instanceof Slot_Base && mTileEntity.interceptClick(mGUIID, (Slot_Base)aSlot, aIndex, aSlot.getSlotIndex(), aPlayer, aShift == 1, aMouse != 0, aMouse, aShift)) {
				// The original's return value was a sync check, not an instruction to set the cursor; applying it as if
				// it were one wiped the carried item on clicks that never meant to touch the cursor at all.
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

	/** Engine renamed transferStackInSlot to quickMoveStack and replaced null returns with ItemStack.EMPTY. */
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

	/** Engine renamed several Slot/Container methods; the merge logic itself (wildcard early exit, dirty
	 *  marking, Slot_Holo exclusion, ST.equal comparison) is GT6's own custom behavior, not a vanilla merge. */
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

	/** Not an override: the engine renamed addSlotToContainer to addSlot; kept under the old name and null guard. */
	protected Slot addSlotToContainer(Slot aSlot) {
		if (aSlot == null) return null;
		return addSlot(aSlot);
	}

	/** Engine renamed addCraftingToCrafters/ICrafting to addSlotListener/ContainerListener. */
	@Override
	public void addSlotListener(ContainerListener aListener) {
		try {
			super.addSlotListener(aListener);
		} catch(Throwable e) {
			e.printStackTrace(ERR);
		}
	}

	/** Engine renamed and narrowed getInventory():List<ItemStack> to getItems():NonNullList<ItemStack>. */
	@Override
	public NonNullList<ItemStack> getItems() {
		try {
			return super.getItems();
		} catch(Throwable e) {
			e.printStackTrace(ERR);
		}
		return null;
	}

	/** Engine renamed removeCraftingFromCrafters/ICrafting to removeSlotListener/ContainerListener. */
	@Override
	public void removeSlotListener(ContainerListener aListener) {
		try {
			super.removeSlotListener(aListener);
		} catch(Throwable e) {
			e.printStackTrace(ERR);
		}
	}

	/** Engine renamed detectAndSendChanges to broadcastChanges; the custom "resync everything" behavior now
	 *  calls the engine's own broadcastFullState() instead of reimplementing it against now-private fields. */
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

	/** The engine dropped this method for findSlot, which only works through base container fields unsuited
	 *  to Slot_Base's own bookkeeping; kept under the old name, routed to each slot type's own comparison. */
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

	/** Engine removed this entirely: double-click pickup-all is now handled inside doClick via super.clicked. */

	/** removed replaces the renamed onContainerClosed; the original's two ST.check scans and mouse-held-stack handling
	 *  are ported 1:1, without calling super.removed (which does its own place-back-or-drop without ST.check). */
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
			if (ST.ni(getCarried()) != null) { // Not isEmpty(): that would collapse a stack with a meaningful count of 0 into empty.
				ST.check(aPlayer, getCarried());
				aPlayer.drop(getCarried(), false);
				setCarried(ItemStack.EMPTY);
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

	/* Forced removals: hooks the engine dropped without a replacement, where the original body was already
	   either a no-op or matched the new engine default exactly, so nothing here is actually lost. */
}

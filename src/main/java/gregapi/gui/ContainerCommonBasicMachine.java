/**
 * Copyright (c) 2021 GregTech-6 Team
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

package gregapi.gui;

import static gregapi.data.CS.*;

import gregapi.data.LH;
import gregapi.recipes.Recipe.RecipeMap;
import gregapi.tileentity.ITileEntityInventoryGUI;
import gregapi.tileentity.machines.MultiTileEntityBasicMachine;
import gregapi.util.UT;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.DataSlot;

/**
 * F-GUI: {@code detectAndSendChanges}→{@code broadcastChanges}, старый int-индексный прогресс-бар
 * ({@code ContainerListener.sendProgressBarUpdate}, движок убрал целиком) → {@code DataSlot}
 * (`neo-decompiled/net/minecraft/world/inventory/AbstractContainerMenu.java:132-142,205-213` — добавляется
 * через {@code addDataSlot}, синхронизируется САМИМ движком внутри {@code super.broadcastChanges()}, ручная
 * рассылка по {@code crafters} не нужна). Клиентский приём значения — {@code updateProgressBar(int,int)}→
 * {@code setData(int,int)} (движок переименовал; `ClientPacketListener.java:1505` — реальный клиентский
 * вызыватель, `BeaconMenu.java:78`/`LecternMenu.java:47-86` — эталон того же паттерна оверрайда без
 * {@code @OnlyIn(CLIENT)}, метод общий для обеих сторон).
 */
public class ContainerCommonBasicMachine extends ContainerCommon {
	private RecipeMap mRecipes;

	/** F-GUI: движковый DataSlot заменяет ручную рассылку прогресс-бара, см. javadoc класса. */
	private final DataSlot mProgressBarSlot;

	public ContainerCommonBasicMachine(Inventory aInventoryPlayer, ITileEntityInventoryGUI aTileEntity, RecipeMap aRecipes, int aGUIID) {
		super(aInventoryPlayer, aTileEntity, aGUIID);
		mRecipes = aRecipes;
		mProgressBarSlot = addDataSlot(DataSlot.standalone());
	}
	
	@Override
	public int addSlots(Inventory aPlayerInventory) {
		mRecipes = ((MultiTileEntityBasicMachine)mTileEntity).mRecipes;
		int tIndex = 0;
		
		addSlotToContainer(mRecipes.getSpecialSlot(mTileEntity, mRecipes.mInputItemsCount + mRecipes.mOutputItemsCount, 80, 43));
		
		switch (mRecipes.mInputItemsCount) {
		case  0:
			break;
		case  1:
			addSlotToContainer(new Slot_Normal(mTileEntity, tIndex++, 53, mRecipes.mInputFluidCount>6? 7:25));
			break;
		case  2:
			addSlotToContainer(new Slot_Normal(mTileEntity, tIndex++, 35, mRecipes.mInputFluidCount>6? 7:25));
			addSlotToContainer(new Slot_Normal(mTileEntity, tIndex++, 53, mRecipes.mInputFluidCount>6? 7:25));
			break;
		case  3:
			addSlotToContainer(new Slot_Normal(mTileEntity, tIndex++, 17, mRecipes.mInputFluidCount>6? 7:25));
			addSlotToContainer(new Slot_Normal(mTileEntity, tIndex++, 35, mRecipes.mInputFluidCount>6? 7:25));
			addSlotToContainer(new Slot_Normal(mTileEntity, tIndex++, 53, mRecipes.mInputFluidCount>6? 7:25));
			break;
		case  4:
			addSlotToContainer(new Slot_Normal(mTileEntity, tIndex++, 35, mRecipes.mInputFluidCount>3? 7:16));
			addSlotToContainer(new Slot_Normal(mTileEntity, tIndex++, 53, mRecipes.mInputFluidCount>3? 7:16));
			addSlotToContainer(new Slot_Normal(mTileEntity, tIndex++, 35, mRecipes.mInputFluidCount>3?25:34));
			addSlotToContainer(new Slot_Normal(mTileEntity, tIndex++, 53, mRecipes.mInputFluidCount>3?25:34));
			break;
		case  5:
			addSlotToContainer(new Slot_Normal(mTileEntity, tIndex++, 17, mRecipes.mInputFluidCount>3? 7:16));
			addSlotToContainer(new Slot_Normal(mTileEntity, tIndex++, 35, mRecipes.mInputFluidCount>3? 7:16));
			addSlotToContainer(new Slot_Normal(mTileEntity, tIndex++, 53, mRecipes.mInputFluidCount>3? 7:16));
			addSlotToContainer(new Slot_Normal(mTileEntity, tIndex++, 35, mRecipes.mInputFluidCount>3?25:34));
			addSlotToContainer(new Slot_Normal(mTileEntity, tIndex++, 53, mRecipes.mInputFluidCount>3?25:34));
			break;
		case  6:
			addSlotToContainer(new Slot_Normal(mTileEntity, tIndex++, 17, mRecipes.mInputFluidCount>3? 7:16));
			addSlotToContainer(new Slot_Normal(mTileEntity, tIndex++, 35, mRecipes.mInputFluidCount>3? 7:16));
			addSlotToContainer(new Slot_Normal(mTileEntity, tIndex++, 53, mRecipes.mInputFluidCount>3? 7:16));
			addSlotToContainer(new Slot_Normal(mTileEntity, tIndex++, 17, mRecipes.mInputFluidCount>3?25:34));
			addSlotToContainer(new Slot_Normal(mTileEntity, tIndex++, 35, mRecipes.mInputFluidCount>3?25:34));
			addSlotToContainer(new Slot_Normal(mTileEntity, tIndex++, 53, mRecipes.mInputFluidCount>3?25:34));
			break;
		case  7:
			addSlotToContainer(new Slot_Normal(mTileEntity, tIndex++, 17,  7));
			addSlotToContainer(new Slot_Normal(mTileEntity, tIndex++, 35,  7));
			addSlotToContainer(new Slot_Normal(mTileEntity, tIndex++, 53,  7));
			addSlotToContainer(new Slot_Normal(mTileEntity, tIndex++, 17, 25));
			addSlotToContainer(new Slot_Normal(mTileEntity, tIndex++, 35, 25));
			addSlotToContainer(new Slot_Normal(mTileEntity, tIndex++, 53, 25));
			addSlotToContainer(new Slot_Normal(mTileEntity, tIndex++, 17, 43));
			break;
		case  8:
			addSlotToContainer(new Slot_Normal(mTileEntity, tIndex++, 17,  7));
			addSlotToContainer(new Slot_Normal(mTileEntity, tIndex++, 35,  7));
			addSlotToContainer(new Slot_Normal(mTileEntity, tIndex++, 53,  7));
			addSlotToContainer(new Slot_Normal(mTileEntity, tIndex++, 17, 25));
			addSlotToContainer(new Slot_Normal(mTileEntity, tIndex++, 35, 25));
			addSlotToContainer(new Slot_Normal(mTileEntity, tIndex++, 53, 25));
			addSlotToContainer(new Slot_Normal(mTileEntity, tIndex++, 17, 43));
			addSlotToContainer(new Slot_Normal(mTileEntity, tIndex++, 35, 43));
			break;
		case  9:
			addSlotToContainer(new Slot_Normal(mTileEntity, tIndex++, 17,  7));
			addSlotToContainer(new Slot_Normal(mTileEntity, tIndex++, 35,  7));
			addSlotToContainer(new Slot_Normal(mTileEntity, tIndex++, 53,  7));
			addSlotToContainer(new Slot_Normal(mTileEntity, tIndex++, 17, 25));
			addSlotToContainer(new Slot_Normal(mTileEntity, tIndex++, 35, 25));
			addSlotToContainer(new Slot_Normal(mTileEntity, tIndex++, 53, 25));
			addSlotToContainer(new Slot_Normal(mTileEntity, tIndex++, 17, 43));
			addSlotToContainer(new Slot_Normal(mTileEntity, tIndex++, 35, 43));
			addSlotToContainer(new Slot_Normal(mTileEntity, tIndex++, 53, 43));
			break;
		case 10:
			addSlotToContainer(new Slot_Normal(mTileEntity, tIndex++, 17,  7));
			addSlotToContainer(new Slot_Normal(mTileEntity, tIndex++, 35,  7));
			addSlotToContainer(new Slot_Normal(mTileEntity, tIndex++, 53,  7));
			addSlotToContainer(new Slot_Normal(mTileEntity, tIndex++, 17, 25));
			addSlotToContainer(new Slot_Normal(mTileEntity, tIndex++, 35, 25));
			addSlotToContainer(new Slot_Normal(mTileEntity, tIndex++, 53, 25));
			addSlotToContainer(new Slot_Normal(mTileEntity, tIndex++, 17, 43));
			addSlotToContainer(new Slot_Normal(mTileEntity, tIndex++, 35, 43));
			addSlotToContainer(new Slot_Normal(mTileEntity, tIndex++, 53, 43));
			addSlotToContainer(new Slot_Normal(mTileEntity, tIndex++, 53, 61));
			break;
		case 11:
			addSlotToContainer(new Slot_Normal(mTileEntity, tIndex++, 17,  7));
			addSlotToContainer(new Slot_Normal(mTileEntity, tIndex++, 35,  7));
			addSlotToContainer(new Slot_Normal(mTileEntity, tIndex++, 53,  7));
			addSlotToContainer(new Slot_Normal(mTileEntity, tIndex++, 17, 25));
			addSlotToContainer(new Slot_Normal(mTileEntity, tIndex++, 35, 25));
			addSlotToContainer(new Slot_Normal(mTileEntity, tIndex++, 53, 25));
			addSlotToContainer(new Slot_Normal(mTileEntity, tIndex++, 17, 43));
			addSlotToContainer(new Slot_Normal(mTileEntity, tIndex++, 35, 43));
			addSlotToContainer(new Slot_Normal(mTileEntity, tIndex++, 53, 43));
			addSlotToContainer(new Slot_Normal(mTileEntity, tIndex++, 35, 61));
			addSlotToContainer(new Slot_Normal(mTileEntity, tIndex++, 53, 61));
			break;
		default:
			addSlotToContainer(new Slot_Normal(mTileEntity, tIndex++, 17,  7));
			addSlotToContainer(new Slot_Normal(mTileEntity, tIndex++, 35,  7));
			addSlotToContainer(new Slot_Normal(mTileEntity, tIndex++, 53,  7));
			addSlotToContainer(new Slot_Normal(mTileEntity, tIndex++, 17, 25));
			addSlotToContainer(new Slot_Normal(mTileEntity, tIndex++, 35, 25));
			addSlotToContainer(new Slot_Normal(mTileEntity, tIndex++, 53, 25));
			addSlotToContainer(new Slot_Normal(mTileEntity, tIndex++, 17, 43));
			addSlotToContainer(new Slot_Normal(mTileEntity, tIndex++, 35, 43));
			addSlotToContainer(new Slot_Normal(mTileEntity, tIndex++, 53, 43));
			addSlotToContainer(new Slot_Normal(mTileEntity, tIndex++, 17, 61));
			addSlotToContainer(new Slot_Normal(mTileEntity, tIndex++, 35, 61));
			addSlotToContainer(new Slot_Normal(mTileEntity, tIndex++, 53, 61));
			break;
		}
		
		switch (mRecipes.mOutputItemsCount) {
		case  0:
			break;
		case  1:
			addSlotToContainer(new Slot_Normal(mTileEntity, tIndex++, 107, mRecipes.mOutputFluidCount>6? 7:25).setCanPut(F));
			break;
		case  2:
			addSlotToContainer(new Slot_Normal(mTileEntity, tIndex++, 107, mRecipes.mOutputFluidCount>6? 7:25).setCanPut(F));
			addSlotToContainer(new Slot_Normal(mTileEntity, tIndex++, 125, mRecipes.mOutputFluidCount>6? 7:25).setCanPut(F));
			break;
		case  3:
			addSlotToContainer(new Slot_Normal(mTileEntity, tIndex++, 107, mRecipes.mOutputFluidCount>6? 7:25).setCanPut(F));
			addSlotToContainer(new Slot_Normal(mTileEntity, tIndex++, 125, mRecipes.mOutputFluidCount>6? 7:25).setCanPut(F));
			addSlotToContainer(new Slot_Normal(mTileEntity, tIndex++, 143, mRecipes.mOutputFluidCount>6? 7:25).setCanPut(F));
			break;
		case  4:
			addSlotToContainer(new Slot_Normal(mTileEntity, tIndex++, 107, mRecipes.mOutputFluidCount>3? 7:16).setCanPut(F));
			addSlotToContainer(new Slot_Normal(mTileEntity, tIndex++, 125, mRecipes.mOutputFluidCount>3? 7:16).setCanPut(F));
			addSlotToContainer(new Slot_Normal(mTileEntity, tIndex++, 107, mRecipes.mOutputFluidCount>3?25:34).setCanPut(F));
			addSlotToContainer(new Slot_Normal(mTileEntity, tIndex++, 125, mRecipes.mOutputFluidCount>3?25:34).setCanPut(F));
			break;
		case  5:
			addSlotToContainer(new Slot_Normal(mTileEntity, tIndex++, 107, mRecipes.mOutputFluidCount>3? 7:16).setCanPut(F));
			addSlotToContainer(new Slot_Normal(mTileEntity, tIndex++, 125, mRecipes.mOutputFluidCount>3? 7:16).setCanPut(F));
			addSlotToContainer(new Slot_Normal(mTileEntity, tIndex++, 143, mRecipes.mOutputFluidCount>3? 7:16).setCanPut(F));
			addSlotToContainer(new Slot_Normal(mTileEntity, tIndex++, 107, mRecipes.mOutputFluidCount>3?25:34).setCanPut(F));
			addSlotToContainer(new Slot_Normal(mTileEntity, tIndex++, 125, mRecipes.mOutputFluidCount>3?25:34).setCanPut(F));
			break;
		case  6:
			addSlotToContainer(new Slot_Normal(mTileEntity, tIndex++, 107, mRecipes.mOutputFluidCount>3? 7:16).setCanPut(F));
			addSlotToContainer(new Slot_Normal(mTileEntity, tIndex++, 125, mRecipes.mOutputFluidCount>3? 7:16).setCanPut(F));
			addSlotToContainer(new Slot_Normal(mTileEntity, tIndex++, 143, mRecipes.mOutputFluidCount>3? 7:16).setCanPut(F));
			addSlotToContainer(new Slot_Normal(mTileEntity, tIndex++, 107, mRecipes.mOutputFluidCount>3?25:34).setCanPut(F));
			addSlotToContainer(new Slot_Normal(mTileEntity, tIndex++, 125, mRecipes.mOutputFluidCount>3?25:34).setCanPut(F));
			addSlotToContainer(new Slot_Normal(mTileEntity, tIndex++, 143, mRecipes.mOutputFluidCount>3?25:34).setCanPut(F));
			break;
		case  7:
			addSlotToContainer(new Slot_Normal(mTileEntity, tIndex++, 107,  7).setCanPut(F));
			addSlotToContainer(new Slot_Normal(mTileEntity, tIndex++, 125,  7).setCanPut(F));
			addSlotToContainer(new Slot_Normal(mTileEntity, tIndex++, 143,  7).setCanPut(F));
			addSlotToContainer(new Slot_Normal(mTileEntity, tIndex++, 107, 25).setCanPut(F));
			addSlotToContainer(new Slot_Normal(mTileEntity, tIndex++, 125, 25).setCanPut(F));
			addSlotToContainer(new Slot_Normal(mTileEntity, tIndex++, 143, 25).setCanPut(F));
			addSlotToContainer(new Slot_Normal(mTileEntity, tIndex++, 107, 43).setCanPut(F));
			break;
		case  8:
			addSlotToContainer(new Slot_Normal(mTileEntity, tIndex++, 107,  7).setCanPut(F));
			addSlotToContainer(new Slot_Normal(mTileEntity, tIndex++, 125,  7).setCanPut(F));
			addSlotToContainer(new Slot_Normal(mTileEntity, tIndex++, 143,  7).setCanPut(F));
			addSlotToContainer(new Slot_Normal(mTileEntity, tIndex++, 107, 25).setCanPut(F));
			addSlotToContainer(new Slot_Normal(mTileEntity, tIndex++, 125, 25).setCanPut(F));
			addSlotToContainer(new Slot_Normal(mTileEntity, tIndex++, 143, 25).setCanPut(F));
			addSlotToContainer(new Slot_Normal(mTileEntity, tIndex++, 107, 43).setCanPut(F));
			addSlotToContainer(new Slot_Normal(mTileEntity, tIndex++, 125, 43).setCanPut(F));
			break;
		case  9:
			addSlotToContainer(new Slot_Normal(mTileEntity, tIndex++, 107,  7).setCanPut(F));
			addSlotToContainer(new Slot_Normal(mTileEntity, tIndex++, 125,  7).setCanPut(F));
			addSlotToContainer(new Slot_Normal(mTileEntity, tIndex++, 143,  7).setCanPut(F));
			addSlotToContainer(new Slot_Normal(mTileEntity, tIndex++, 107, 25).setCanPut(F));
			addSlotToContainer(new Slot_Normal(mTileEntity, tIndex++, 125, 25).setCanPut(F));
			addSlotToContainer(new Slot_Normal(mTileEntity, tIndex++, 143, 25).setCanPut(F));
			addSlotToContainer(new Slot_Normal(mTileEntity, tIndex++, 107, 43).setCanPut(F));
			addSlotToContainer(new Slot_Normal(mTileEntity, tIndex++, 125, 43).setCanPut(F));
			addSlotToContainer(new Slot_Normal(mTileEntity, tIndex++, 143, 43).setCanPut(F));
			break;
		case 10:
			addSlotToContainer(new Slot_Normal(mTileEntity, tIndex++, 107,  7).setCanPut(F));
			addSlotToContainer(new Slot_Normal(mTileEntity, tIndex++, 125,  7).setCanPut(F));
			addSlotToContainer(new Slot_Normal(mTileEntity, tIndex++, 143,  7).setCanPut(F));
			addSlotToContainer(new Slot_Normal(mTileEntity, tIndex++, 107, 25).setCanPut(F));
			addSlotToContainer(new Slot_Normal(mTileEntity, tIndex++, 125, 25).setCanPut(F));
			addSlotToContainer(new Slot_Normal(mTileEntity, tIndex++, 143, 25).setCanPut(F));
			addSlotToContainer(new Slot_Normal(mTileEntity, tIndex++, 107, 43).setCanPut(F));
			addSlotToContainer(new Slot_Normal(mTileEntity, tIndex++, 125, 43).setCanPut(F));
			addSlotToContainer(new Slot_Normal(mTileEntity, tIndex++, 143, 43).setCanPut(F));
			addSlotToContainer(new Slot_Normal(mTileEntity, tIndex++, 143, 61).setCanPut(F));
			break;
		case 11:
			addSlotToContainer(new Slot_Normal(mTileEntity, tIndex++, 107,  7).setCanPut(F));
			addSlotToContainer(new Slot_Normal(mTileEntity, tIndex++, 125,  7).setCanPut(F));
			addSlotToContainer(new Slot_Normal(mTileEntity, tIndex++, 143,  7).setCanPut(F));
			addSlotToContainer(new Slot_Normal(mTileEntity, tIndex++, 107, 25).setCanPut(F));
			addSlotToContainer(new Slot_Normal(mTileEntity, tIndex++, 125, 25).setCanPut(F));
			addSlotToContainer(new Slot_Normal(mTileEntity, tIndex++, 143, 25).setCanPut(F));
			addSlotToContainer(new Slot_Normal(mTileEntity, tIndex++, 107, 43).setCanPut(F));
			addSlotToContainer(new Slot_Normal(mTileEntity, tIndex++, 125, 43).setCanPut(F));
			addSlotToContainer(new Slot_Normal(mTileEntity, tIndex++, 143, 43).setCanPut(F));
			addSlotToContainer(new Slot_Normal(mTileEntity, tIndex++, 125, 61).setCanPut(F));
			addSlotToContainer(new Slot_Normal(mTileEntity, tIndex++, 143, 61).setCanPut(F));
			break;
		default:
			addSlotToContainer(new Slot_Normal(mTileEntity, tIndex++, 107,  7).setCanPut(F));
			addSlotToContainer(new Slot_Normal(mTileEntity, tIndex++, 125,  7).setCanPut(F));
			addSlotToContainer(new Slot_Normal(mTileEntity, tIndex++, 143,  7).setCanPut(F));
			addSlotToContainer(new Slot_Normal(mTileEntity, tIndex++, 107, 25).setCanPut(F));
			addSlotToContainer(new Slot_Normal(mTileEntity, tIndex++, 125, 25).setCanPut(F));
			addSlotToContainer(new Slot_Normal(mTileEntity, tIndex++, 143, 25).setCanPut(F));
			addSlotToContainer(new Slot_Normal(mTileEntity, tIndex++, 107, 43).setCanPut(F));
			addSlotToContainer(new Slot_Normal(mTileEntity, tIndex++, 125, 43).setCanPut(F));
			addSlotToContainer(new Slot_Normal(mTileEntity, tIndex++, 143, 43).setCanPut(F));
			addSlotToContainer(new Slot_Normal(mTileEntity, tIndex++, 107, 61).setCanPut(F));
			addSlotToContainer(new Slot_Normal(mTileEntity, tIndex++, 125, 61).setCanPut(F));
			addSlotToContainer(new Slot_Normal(mTileEntity, tIndex++, 143, 61).setCanPut(F));
			break;
		}
		
		tIndex++;
		
		for (int i = 0; i < mRecipes.mInputFluidCount ; i++) addSlotToContainer(new Slot_Render(mTileEntity, tIndex++,  53 - (i%3)*18, 63 - (i/3)*18).setTooltip("Extract using a Tap or Nozzle", LH.Chat.WHITE));
		for (int i = 0; i < mRecipes.mOutputFluidCount; i++) addSlotToContainer(new Slot_Render(mTileEntity, tIndex++, 107 + (i%3)*18, 63 - (i/3)*18).setTooltip("Extract using a Tap or Nozzle", LH.Chat.WHITE));
		
		return super.addSlots(aPlayerInventory);
	}
	
	public short mProgressBar = 0;

	@Override
	public void broadcastChanges() {
		// F-GUI R8-фикс: ContainerCommon-конструктор зовёт broadcastChanges() (виртуальный диспетч попадает
		// сюда) ДО того, как addDataSlot(...) успел выполниться (он в теле ЭТОГО подкласса, после super(...))
		// — mProgressBarSlot ещё null на первом вызове; null-гейт (1.7.10 тут был примитив short, конструктора
		// не боялся); первый реальный синк прогресса произойдёт на следующем тике, поведение не теряется.
		if (mProgressBarSlot != null) {
			MultiTileEntityBasicMachine tTE = (MultiTileEntityBasicMachine)mTileEntity;
			if (tTE.mSuccessful) {
				mProgressBarSlot.set(Short.MAX_VALUE);
			} else if (tTE.mMaxProgress > 0) {
				mProgressBarSlot.set((short)UT.Code.units(Math.min(tTE.mMaxProgress, tTE.mProgress), tTE.mMaxProgress, Short.MAX_VALUE, T));
			} else {
				mProgressBarSlot.set(-1);
			}
		}
		super.broadcastChanges();
	}

	@Override
	public void setData(int aIndex, int aValue) {
		super.setData(aIndex, aValue);
		switch (aIndex) {
		case 0: mProgressBar = (short)aValue; break;
		}
	}

	@Override public int getStartIndex() {return 0;}
	@Override public int getSlotCount() {return mRecipes.mInputItemsCount + mRecipes.mOutputItemsCount + (mRecipes.getSpecialSlot(mTileEntity, 0, 80, 43)!=null?1:0);}
	@Override public int getShiftClickStartIndex() {return 0;}
	@Override public int getShiftClickSlotCount() {return mRecipes.mInputItemsCount + (mRecipes.getSpecialSlot(mTileEntity, 0, 80, 43)!=null?1:0);}
}

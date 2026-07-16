/**
 * Copyright (c) 2019 Gregorius Techneticies
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

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import gregapi.tileentity.ITileEntityInventoryGUI;
import net.minecraft.world.entity.player.Inventory;

/**
 * @author Gregorius Techneticies
 */
@OnlyIn(Dist.CLIENT)
public class ContainerClientDefault extends ContainerClient {
	public ContainerClientDefault(Inventory aInventoryPlayer, ITileEntityInventoryGUI aTileEntity) {
		this(aInventoryPlayer, aTileEntity, RES_PATH_GUI + "chests/" + aTileEntity.getSizeInventoryGUI() + ".png");
	}
	public ContainerClientDefault(Inventory aInventoryPlayer, ITileEntityInventoryGUI aTileEntity, String aBackgroundPath) {
		this(aInventoryPlayer, aTileEntity, 0, aBackgroundPath);
	}
	public ContainerClientDefault(Inventory aInventoryPlayer, ITileEntityInventoryGUI aTileEntity, int aGUIID) {
		this(aInventoryPlayer, aTileEntity, aGUIID, RES_PATH_GUI + "chests/" + aTileEntity.getSizeInventoryGUI() + ".png");
	}
	public ContainerClientDefault(Inventory aInventoryPlayer, ITileEntityInventoryGUI aTileEntity, int aGUIID, String aBackgroundPath) {
		super(new ContainerCommonDefault(aInventoryPlayer, aTileEntity, aGUIID), aBackgroundPath);
	}
	public ContainerClientDefault(ContainerCommonDefault aContainer) {
		super(aContainer, RES_PATH_GUI + "chests/" + aContainer.mSlotCount + ".png");
	}
	
	/** F3 superseded-render (GT6BlockModel/ItemModel пайплайн; старый getIcon/immediate-mode мёртв, 0 вызовов neo): было {@code FontRenderer.drawString} (метод удалён у {@code Font},
	 *  текст экрана теперь рисуется через {@code GuiGraphicsExtractor} в extract-фазе — см. javadoc
	 *  {@link ContainerClient} class). */
	@Override
	protected void drawGuiContainerForegroundLayer(int p_146979_1_, int p_146979_2_) {
		//
	}
}

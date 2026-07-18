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
import gregapi.tileentity.ITileEntityInventoryGUI;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.client.resources.language.I18n;

/**
 * @author Gregorius Techneticies
 */
public class ContainerClientChest extends ContainerClient {
	private int mRows;
	
	public ContainerClientChest(Inventory aInventoryPlayer, ITileEntityInventoryGUI aTileEntity, int aGUIID) {
		this(new ContainerCommonChest(aInventoryPlayer, aTileEntity, aGUIID));
	}
	
	public ContainerClientChest(ContainerCommonChest aContainer) {
		super(aContainer, RES_PATH_GUI + "chests/" + aContainer.mSlotCount + ".png");
		allowUserInput = F;
		mRows = aContainer.mSlotCount / 9 + (aContainer.mSlotCount % 9 == 0 ? 0 : 1);
		ySize = 114 + mRows * 18;
	}
	
	@Override
	protected void drawGuiContainerForegroundLayer(int p_146979_1_, int p_146979_2_) {
		drawString(fontRendererObj, mContainer.mTileEntity.getInventoryNameGUI(), 8, 6, 4210752);
		drawString(fontRendererObj, gregapi.lang.LanguageHandler.translate("container.inventory"), 8, ySize - 94, 4210752);
	}

	@Override
	protected void drawGuiContainerBackgroundLayer2(float par1, int par2, int par3) {
		// был GL11.glColor4f(1,1,1,1) — в neo цвет пер-blit (дефолт белый), сброс состояния не существует
		int k = (width - xSize) / 2;
		int l = (height - ySize) / 2;
		drawTexturedModalRect(k, l, 0, 0, xSize, mRows * 18 + 17);
		drawTexturedModalRect(k, l + mRows * 18 + 17, 0, 126, xSize, 96);
	}
}

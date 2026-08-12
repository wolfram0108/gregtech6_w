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
 *
 * Modified in 2026 for the GregTech 6 NeoForge port
 * (https://github.com/wolfram0108/gregtech6_w): ported from Minecraft 1.7.10 / Forge
 * to Minecraft 26.1.2 / NeoForge.
 */

package gregapi.gui;

import static gregapi.data.CS.*;

import net.minecraftforge.api.distmarker.Dist;
import gregapi.tileentity.ITileEntityInventoryGUI;
import net.minecraft.world.entity.player.Inventory;

/**
 * @author Gregorius Techneticies
 */
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
	
	@Override
	protected void drawGuiContainerForegroundLayer(int p_146979_1_, int p_146979_2_) {
		if (mContainer.mSlotCount != 16 && mContainer.mSlotCount <= 27) drawString(fontRendererObj, mContainer.mTileEntity.getInventoryNameGUI(), 8,  4, 4210752);
	}
}

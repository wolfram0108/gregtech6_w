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

import net.neoforged.api.distmarker.Dist;
import gregapi.data.LH;
import gregapi.recipes.Recipe.RecipeMap;
import gregapi.tileentity.ITileEntityInventoryGUI;
import gregapi.util.UT;
import net.minecraft.world.entity.player.Inventory;

public class ContainerClientBasicMachine extends ContainerClient {
	private RecipeMap mRecipes;
	
	public ContainerClientBasicMachine(Inventory aInventoryPlayer, ITileEntityInventoryGUI aTileEntity, RecipeMap aRecipes, int aGUIID, String aGUITexture) {
		super(new ContainerCommonBasicMachine(aInventoryPlayer, aTileEntity, aRecipes, aGUIID), aGUITexture);
		mRecipes = aRecipes;
		mNEI = mRecipes.mNameNEI;
	}
	
	@Override
	protected void drawGuiContainerForegroundLayer(int par1, int par2) {
		drawString(fontRendererObj, mContainer.mTileEntity.hasCustomInventoryNameGUI()?mContainer.mTileEntity.getInventoryNameGUI():LH.get(mRecipes.mNameInternal), 8,  4, 4210752);
	}

	/**
	 * Зона клика «показать рецепты» вокруг стрелки прогресса — взята ДОСЛОВНО ИЗ ОРИГИНАЛА, а не из
	 * собственной прикидки по отрисовке.
	 *
	 * <p>1.7.10, {@code gregapi/NEI_RecipeMap.java:70}:
	 * {@code transferRects.add(new RecipeTransferRect(new Rectangle(70-sOffsetX, 24-sOffsetY, 36, 18), …))}
	 * при {@code sOffsetX = 5, sOffsetY = 11} ({@code :66}) — то есть в координатах САМОГО GUI зона равна
	 * {@code (70, 24)} размером {@code 36×18}. Те же числа после вычета оффсета видны в проверках
	 * {@code :420}/{@code :425} как {@code Rectangle(65, 13, 36, 18)}.</p>
	 *
	 * <p>⚠️ Зона ШИРЕ самой стрелки: стрелка рисуется с {@code x+78} шириной 20
	 * ({@link #drawGuiContainerBackgroundLayer2}), а кликабельны 36 пикселей начиная с {@code x+70}.
	 * Первая моя редакция брала границы из отрисовки (78, 20 px) и была уже оригинальной — игрок,
	 * кликнув по краю зоны, рецептов бы не получил.</p>
	 */
	protected boolean isOverProgressBar(double aMouseX, double aMouseY) {
		int tX = leftPos + 70, tY = topPos + 24;
		return aMouseX >= tX && aMouseX < tX + 36 && aMouseY >= tY && aMouseY < tY + 18;
	}

	/**
	 * BUG-056 часть Б: клик по СТРЕЛКЕ ПРОГРЕССА открывает список рецептов этой машины — ровно тот жест,
	 * которым это делалось в 1.7.10 (там его обрабатывал оверлей мода NEI, см.
	 * {@link ContainerClient#openRecipesForThisGUI}). Клик обрабатывается только если попали в стрелку и
	 * экран рецептов реально открылся; иначе управление уходит дальше по штатной цепочке.
	 */
	@Override
	public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent aEvent, boolean aDoubleClick) {
		if (isOverProgressBar(aEvent.x(), aEvent.y()) && openRecipesForThisGUI()) return true;
		return super.mouseClicked(aEvent, aDoubleClick);
	}
	
	@Override
	protected void drawGuiContainerBackgroundLayer2(float par1, int par2, int par3) {
		int x = (width - xSize) / 2;
		int y = (height - ySize) / 2;
		drawTexturedModalRect(x, y, 0, 0, xSize, ySize);
		if (mContainer instanceof ContainerCommonBasicMachine) {
			if (((ContainerCommonBasicMachine)mContainer).mProgressBar >= 0) {
				int tSize = (mRecipes.mProgressBarDirection % 4 < 2 ? 20 : 18), tProgress = (int)UT.Code.scale(((ContainerCommonBasicMachine)mContainer).mProgressBar, 0, Short.MAX_VALUE, tSize * UT.Code.unsignB(mRecipes.mProgressBarAmount), F) % (tSize+1);
				
				switch (mRecipes.mProgressBarDirection) {
				case 0:                             drawTexturedModalRect(x + 78                    , y + 24                    , 176                   , 0                 , tProgress , 18        ); break;
				case 1:                             drawTexturedModalRect(x + 78 + 20 - tProgress   , y + 24                    , 176 + 20 - tProgress  , 0                 , tProgress , 18        ); break;
				case 2:                             drawTexturedModalRect(x + 78                    , y + 24                    , 176                   , 0                 , 20        , tProgress ); break;
				case 3:                             drawTexturedModalRect(x + 78                    , y + 24 + 18 - tProgress   , 176                   , 18 - tProgress    , 20        , tProgress ); break;
				case 4: tProgress = 20 - tProgress; drawTexturedModalRect(x + 78                    , y + 24                    , 176                   , 0                 , tProgress , 18        ); break;
				case 5: tProgress = 20 - tProgress; drawTexturedModalRect(x + 78 + 20 - tProgress   , y + 24                    , 176 + 20 - tProgress  , 0                 , tProgress , 18        ); break;
				case 6: tProgress = 18 - tProgress; drawTexturedModalRect(x + 78                    , y + 24                    , 176                   , 0                 , 20        , tProgress ); break;
				case 7: tProgress = 18 - tProgress; drawTexturedModalRect(x + 78                    , y + 24 + 18 - tProgress   , 176                   , 18 - tProgress    , 20        , tProgress ); break;
				}
			}
		}
	}
}

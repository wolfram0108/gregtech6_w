/**
 * Copyright (c) 2020 GregTech-6 Team
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
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;
import net.minecraft.resources.Identifier;

/**
 * @author Gregorius Techneticies
 *
 * F3 superseded-render (GT6BlockModel/ItemModel пайплайн; старый getIcon/immediate-mode мёртв, 0 вызовов neo): 1.7.10 {@code GuiContainer} (immediate-mode: {@code drawGuiContainerBackgroundLayer}
 * рисовал фон через {@code mc.renderEngine.bindTexture}+GL11, {@code drawTexturedModalRect} слал квады в
 * {@code Tessellator}, {@code drawScreen} каждый кадр перерисовывал tooltip) заменён {@code AbstractContainerScreen<T>}
 * — новый extract-render-state API 26.1.2 (`neo-decompiled/net/minecraft/client/gui/screens/inventory/AbstractContainerScreen.java:103-128`:
 * {@code extractRenderState(GuiGraphicsExtractor,...)}→{@code extractContents}→{@code extractLabels}/{@code extractSlots},
 * БЕЗ старого {@code drawGuiContainerXxxLayer(...)}/{@code drawScreen(...)}). Legacy-имена полей/методов
 * ({@code mc}, {@code fontRendererObj}, {@code xSize}/{@code ySize}, {@code drawTexturedModalRect}, {@code allowUserInput})
 * сохранены здесь КАК ОДИН нейтральный compile-only мост (централизация #3, единая точка для всей иерархии
 * {@code ContainerClientDefault/Chest/BasicMachine}) — их построчную адаптацию под подклассы делать не пришлось.
 * Реальная перерисовка — decisions/F3-render.md §2.7 (BER/{@code GuiGraphicsExtractor}-путь); тело каждого
 * рисующего метода ниже — no-op заглушка, сигнатуры сохранены 1:1.
 */
@OnlyIn(Dist.CLIENT)
public class ContainerClient extends AbstractContainerScreen<ContainerCommon> {

	public boolean mCrashed = F;

	public Identifier mBackground;

	public String mNEI = "";

	public ContainerCommon mContainer;

	/** F3 superseded-render (GT6BlockModel/ItemModel пайплайн; старый getIcon/immediate-mode мёртв, 0 вызовов neo): было поле {@code GuiScreen.mc} (переименовано в {@code Screen.minecraft}, см. class javadoc). */
	protected final Minecraft mc;
	/** F3 superseded-render (GT6BlockModel/ItemModel пайплайн; старый getIcon/immediate-mode мёртв, 0 вызовов neo): было поле {@code GuiScreen.fontRendererObj} (переименовано в {@code Screen.font}, см. class javadoc). */
	protected final Font fontRendererObj;
	/** F3 superseded-render (GT6BlockModel/ItemModel пайплайн; старый getIcon/immediate-mode мёртв, 0 вызовов neo): были мутируемые поля {@code GuiContainer.xSize/ySize}; в 26.1.2
	 *  {@code AbstractContainerScreen.imageWidth/imageHeight} — {@code final} (подклассы GT6 мутируют
	 *  {@code ySize} ПОСЛЕ {@code super(...)}, см. {@link ContainerClientChest}) — отдельный держатель, см. class javadoc. */
	protected int xSize, ySize;
	/** F3 superseded-render (GT6BlockModel/ItemModel пайплайн; старый getIcon/immediate-mode мёртв, 0 вызовов neo): было поле {@code GuiContainer.allowUserInput} (см. class javadoc). */
	protected boolean allowUserInput;

	public int getLeft() {return leftPos;}
	public int getTop() {return topPos;}

	public ContainerClient(ContainerCommon aContainer, String aBackgroundPath) {
		super(aContainer, aContainer.mInventoryPlayer, Component.empty());
		mContainer = aContainer;
		mBackground = Identifier.parse(aBackgroundPath);
		mc = minecraft;
		fontRendererObj = font;
		xSize = imageWidth;
		ySize = imageHeight;
	}

	/** F3 superseded-render (GT6BlockModel/ItemModel пайплайн; старый getIcon/immediate-mode мёртв, 0 вызовов neo): было immediate-mode рисование заголовка (см. class javadoc). */
	protected void drawGuiContainerForegroundLayer(int par1, int par2) {
		//
	}

	/** F3 superseded-render (GT6BlockModel/ItemModel пайплайн; старый getIcon/immediate-mode мёртв, 0 вызовов neo): было {@code mc.renderEngine.bindTexture}+GL11 (см. class javadoc). */
	protected void drawGuiContainerBackgroundLayer(float par1, int par2, int par3) {
		drawGuiContainerBackgroundLayer2(par1, par2, par3);
	}

	/** F3 superseded-render (GT6BlockModel/ItemModel пайплайн; старый getIcon/immediate-mode мёртв, 0 вызовов neo): было {@code drawTexturedModalRect} через Tessellator (см. class javadoc). */
	protected void drawGuiContainerBackgroundLayer2(float par1, int par2, int par3) {
		int x = (width - xSize) / 2;
		int y = (height - ySize) / 2;
		drawTexturedModalRect(x, y, 0, 0, xSize, ySize);
	}

	/** F3 superseded-render (GT6BlockModel/ItemModel пайплайн; старый getIcon/immediate-mode мёртв, 0 вызовов neo): было {@code GuiScreen.drawTexturedModalRect} (immediate-mode
	 *  квад в {@code Tessellator}, тип удалён, см. class javadoc). */
	protected void drawTexturedModalRect(int aX, int aY, int aU, int aV, int aW, int aH) {
		//
	}

	/** F3 superseded-render (GT6BlockModel/ItemModel пайплайн; старый getIcon/immediate-mode мёртв, 0 вызовов neo): было {@code GuiScreen.drawScreen(int,int,float)} — весь immediate-mode
	 *  цикл кадра, включая per-slot tooltip через {@code drawHoveringText} (метод/API удалены, см. class javadoc). */
	public void drawScreen(int aX, int aY, float par3) {
		//
	}

	protected boolean isMouseOverSlot(Slot aSlot, int aX, int aY) {return isHovering(aSlot.x, aSlot.y, 16, 16, aX, aY);}
}

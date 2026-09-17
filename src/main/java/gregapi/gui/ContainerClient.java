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
 *
 * Modified in 2026 for the GregTech 6 NeoForge port
 * (https://github.com/wolfram0108/gregtech6_w): ported from Minecraft 1.7.10 / Forge
 * to Minecraft 26.1.2 / NeoForge.
 */

package gregapi.gui;

import static gregapi.data.CS.*;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;
import net.minecraft.resources.ResourceLocation;

/** @author Gregorius Techneticies
 *  1.20.1's AbstractContainerScreen is an immediate-mode pipeline too, with the same background/labels/tooltip layers
 *  as 1.7.10's draw*Layer methods, so the legacy field/method names are kept as one neutral bridge for the hierarchy. */
public class ContainerClient extends AbstractContainerScreen<ContainerCommon> {

	public boolean mCrashed = F;

	public ResourceLocation mBackground;

	public String mNEI = "";

	public ContainerCommon mContainer;

	/** GuiScreen.mc is now Screen.minecraft; kept under the old name as part of the class-level bridge. */
	protected final Minecraft mc;
	/** GuiScreen.fontRendererObj is now Screen.font; kept under the old name as part of the class-level bridge. */
	protected final Font fontRendererObj;
	/** AbstractContainerScreen.imageWidth/imageHeight are final, but GT6 subclasses mutate xSize/ySize after
	 *  super(...), so this bridge keeps its own mutable holder instead. */
	protected int xSize, ySize;
	protected boolean allowUserInput;

	/** Valid only inside renderBg/renderLabels; see the class javadoc for the bridge this backs. */
	protected GuiGraphics mGraphics = null;

	/** Diagnostic counters proving the engine actually drew the background/text, not just queued it. */
	public static final java.util.concurrent.atomic.AtomicLong sBlitCalls = new java.util.concurrent.atomic.AtomicLong(), sTextCalls = new java.util.concurrent.atomic.AtomicLong();

	public int getLeft() {return leftPos;}
	public int getTop() {return topPos;}

	public ContainerClient(ContainerCommon aContainer, String aBackgroundPath) {
		super(aContainer, aContainer.mInventoryPlayer, Component.empty());
		mContainer = aContainer;
		// GT6 paths use uppercase letters, which neo's Identifier rejects; lowercased here the same way the
		// texture set already does for on-disk assets.
		mBackground = new ResourceLocation(aBackgroundPath.toLowerCase(java.util.Locale.ROOT));
		// Screen.minecraft/font are filled only in init(...); grabbing them in the constructor gave null and crashed on open.
		// The live instance already exists by the time the client builds this GUI, so it's fetched fresh here instead.
		mc = net.minecraft.client.Minecraft.getInstance();
		fontRendererObj = mc.font;
		xSize = imageWidth;
		ySize = imageHeight;
	}

	// neo's imageWidth/imageHeight are final while GT6 subclasses mutate xSize/ySize after super(...), so
	// the screen is centered on the GT6 fields to keep slots and background aligned.
	@Override protected void init() {
		super.init();
		leftPos = (width - xSize) / 2;
		topPos  = (height - ySize) / 2;
	}

	/** Restores a function 1.7.10 provided through the NEI overlay clicking the progress arrow, which JEI
	 *  does not replicate; reuses the same recipe-category lookup the icon path already uses. */
	public boolean openRecipesForThisGUI() {
		if (!NEI || !gregapi.util.UT.Code.stringValid(mNEI)) return F;
		return gregapi.jei.GT6_JEI_Plugin.showRecipeCategory(mNEI);
	}

	// A null tile entity after client reconstruction means the GUI failed to open, matching 1.7.10 behavior;
	// the screen closes itself on the first tick instead.
	@Override protected void containerTick() {
		super.containerTick();
		if (mContainer != null && mContainer.mTileEntity == null) onClose();
	}

	// This is verbatim vanilla ContainerScreen.render's frame order (dim, layers, tooltip); 1.7.10 did the same in drawScreen.
	@Override public void render(GuiGraphics aGraphics, int aMouseX, int aMouseY, float aPartial) {
		renderBackground(aGraphics);
		super.render(aGraphics, aMouseX, aMouseY, aPartial);
		renderTooltip(aGraphics, aMouseX, aMouseY);
	}

	// renderBg routes to the 1.7.10 background hook, still in screen coordinates with no translate, as 1.7.10 was.
	@Override protected void renderBg(GuiGraphics aGraphics, float aPartial, int aMouseX, int aMouseY) {
		mGraphics = aGraphics;
		try {drawGuiContainerBackgroundLayer(aPartial, aMouseX, aMouseY);} finally {mGraphics = null;}
	}

	// renderLabels routes to the 1.7.10 foreground hook, without super: 1.7.10's GuiContainer never drew labels itself.
	@Override protected void renderLabels(GuiGraphics aGraphics, int aMouseX, int aMouseY) {
		mGraphics = aGraphics;
		try {drawGuiContainerForegroundLayer(aMouseX, aMouseY);} finally {mGraphics = null;}
	}

	// Restores the original tooltip for an empty Slot_Base, keyed on the stack itself as before, not on
	// hasItem(); the engine's own tooltip handling now covers non-empty slots, including fluid displays.
	@Override protected void renderTooltip(GuiGraphics aGraphics, int aMouseX, int aMouseY) {
		super.renderTooltip(aGraphics, aMouseX, aMouseY);
		if (!(hoveredSlot instanceof Slot_Base tSlot)) return;
		if (gregapi.util.ST.n(hoveredSlot.getItem()) != null) return;   // This boundary turns EMPTY into null; a non-empty slot's tooltip remains the engine's own job.
		java.util.List<String> tTip = tSlot.getTooltip(minecraft.player, minecraft.options.advancedItemTooltips);
		if (tTip != null && !tTip.isEmpty()) {
			java.util.List<Component> tComps = new java.util.ArrayList<>();
			for (String tLine : tTip) if (tLine != null) tComps.add(Component.literal(tLine));
			aGraphics.renderTooltip(font, tComps, java.util.Optional.empty(), aMouseX, aMouseY);
		}
	}

	protected void drawGuiContainerForegroundLayer(int par1, int par2) {
		//
	}

	protected void drawGuiContainerBackgroundLayer(float par1, int par2, int par3) {
		drawGuiContainerBackgroundLayer2(par1, par2, par3);
	}

	protected void drawGuiContainerBackgroundLayer2(float par1, int par2, int par3) {
		int x = (width - xSize) / 2;
		int y = (height - ySize) / 2;
		drawTexturedModalRect(x, y, 0, 0, xSize, ySize);
	}

	/** 1.7.10's bound-texture draw becomes a single blit against GT6's fixed 256x256 atlas. */
	protected void drawTexturedModalRect(int aX, int aY, int aU, int aV, int aW, int aH) {
		if (mGraphics != null) {mGraphics.blit(mBackground, aX, aY, aU, aV, aW, aH); sBlitCalls.incrementAndGet();}
	}

	/** Bridges GuiScreen.drawString to neo's Font, which has no drawing methods of its own. */
	public void drawString(Font aFont, String aText, int aX, int aY, int aColor) {
		if (mGraphics != null && aText != null) {mGraphics.drawString(aFont, aText, aX, aY, (aColor & 0xFF000000) == 0 ? aColor | 0xFF000000 : aColor, F); sTextCalls.incrementAndGet();}
	}

	protected boolean isMouseOverSlot(Slot aSlot, int aX, int aY) {return isHovering(aSlot.x, aSlot.y, 16, 16, aX, aY);}
}

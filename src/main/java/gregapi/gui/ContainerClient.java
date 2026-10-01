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

import net.neoforged.api.distmarker.Dist;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;
import net.minecraft.resources.Identifier;

/** @author Gregorius Techneticies
 *  AbstractContainerScreen replaces the old immediate-mode GuiContainer render path with an extract-render-
 *  state API; legacy names are kept here as one neutral bridge for the whole GUI hierarchy. */
public class ContainerClient extends AbstractContainerScreen<ContainerCommon> {

	public boolean mCrashed = F;

	public Identifier mBackground;

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

	/** Valid only inside extractBackground/extractLabels; holds the current frame's extract context. */
	protected GuiGraphicsExtractor mGraphics = null;

	/** Diagnostic counters proving the engine actually drew the background/text, not just queued it. */
	public static final java.util.concurrent.atomic.AtomicLong sBlitCalls = new java.util.concurrent.atomic.AtomicLong(), sTextCalls = new java.util.concurrent.atomic.AtomicLong();

	public int getLeft() {return leftPos;}
	public int getTop() {return topPos;}

	public ContainerClient(ContainerCommon aContainer, String aBackgroundPath) {
		super(aContainer, aContainer.mInventoryPlayer, Component.empty());
		mContainer = aContainer;
		// GT6 paths use uppercase letters, which neo's Identifier rejects; lowercased here the same way the
		// texture set already does for on-disk assets.
		mBackground = Identifier.parse(aBackgroundPath.toLowerCase(java.util.Locale.ROOT));
		mc = minecraft;
		fontRendererObj = font;
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

	// neo's extract phase bridged to the 1.7.10 background hook; coordinates stay screen-relative as before.
	@Override public void extractBackground(GuiGraphicsExtractor aGraphics, int aMouseX, int aMouseY, float aPartial) {
		super.extractBackground(aGraphics, aMouseX, aMouseY, aPartial);
		mGraphics = aGraphics;
		try {drawGuiContainerBackgroundLayer(aPartial, aMouseX, aMouseY);} finally {mGraphics = null;}
	}

	// neo's extract phase bridged to the 1.7.10 foreground hook; titles are drawn by the subclass, not here,
	// matching how 1.7.10's GuiContainer never drew labels itself.
	@Override protected void extractLabels(GuiGraphicsExtractor aGraphics, int aMouseX, int aMouseY) {
		mGraphics = aGraphics;
		try {drawGuiContainerForegroundLayer(aMouseX, aMouseY);} finally {mGraphics = null;}
	}

	/** A size-0 stack (a layout copy, a remembered type) is no item: it shows "0" under the recipe book's ghost veil,
	 *  so it can't be mistaken for a real one. The engine's per-slot hook here; the 1.20.1 branch has it closed. */
	@Override protected void renderSlotContents(GuiGraphicsExtractor aGraphics, net.minecraft.world.item.ItemStack aStack, Slot aSlot, @org.jspecify.annotations.Nullable String aCount) {
		boolean tGhost = gregapi.util.ST.size(aStack) == 0 && aStack.getCount() > 0;
		super.renderSlotContents(aGraphics, aStack, aSlot, tGhost ? "0" : aCount);
		if (tGhost) aGraphics.fill(aSlot.x, aSlot.y, aSlot.x + 16, aSlot.y + 16, GHOST_VEIL);
	}
	/** The veil colour of the recipe book's ghost slots (GhostSlots:58). */
	private static final int GHOST_VEIL = 0x30FFFFFF;

	// Restores the original tooltip for an empty Slot_Base, keyed on the stack itself as before, not on
	// hasItem(); the engine's own tooltip handling now covers non-empty slots, including fluid displays.
	@Override protected void extractTooltip(GuiGraphicsExtractor aGraphics, int aMouseX, int aMouseY) {
		super.extractTooltip(aGraphics, aMouseX, aMouseY);
		if (!(hoveredSlot instanceof Slot_Base tSlot)) return;
		if (gregapi.util.ST.n(hoveredSlot.getItem()) != null) return;   // This boundary turns EMPTY into null; a non-empty slot's tooltip remains the engine's own job.
		java.util.List<String> tTip = tSlot.getTooltip(minecraft.player, minecraft.options.advancedItemTooltips);
		if (tTip != null && !tTip.isEmpty()) {
			java.util.List<Component> tComps = new java.util.ArrayList<>();
			for (String tLine : tTip) if (tLine != null) tComps.add(Component.literal(tLine));
			aGraphics.setTooltipForNextFrame(font, tComps, java.util.Optional.empty(), net.minecraft.world.item.ItemStack.EMPTY, aMouseX, aMouseY);
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
		if (mGraphics != null) {mGraphics.blit(RenderPipelines.GUI_TEXTURED, mBackground, aX, aY, aU, aV, aW, aH, 256, 256); sBlitCalls.incrementAndGet();}
	}

	/** Bridges GuiScreen.drawString to neo's Font, which has no drawing methods of its own. */
	public void drawString(Font aFont, String aText, int aX, int aY, int aColor) {
		if (mGraphics != null && aText != null) {mGraphics.text(aFont, aText, aX, aY, (aColor & 0xFF000000) == 0 ? aColor | 0xFF000000 : aColor, F); sTextCalls.incrementAndGet();}
	}

	protected boolean isMouseOverSlot(Slot aSlot, int aX, int aY) {return isHovering(aSlot.x, aSlot.y, 16, 16, aX, aY);}
}

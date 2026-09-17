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

package gregapi.tileentity.render;

import gregapi.tileentity.ITileEntityUnloadable;
import net.minecraftforge.client.event.RenderHighlightEvent;

/** 1.7.10's DrawBlockHighlightEvent, with direct player/held-item/partialTicks fields, is gone entirely in 26.1.2.
 *  The signature is retyped to the new event, which carries none of that; implementations below are compiling stubs. */
public interface ITileEntityOnDrawBlockHighlight extends ITileEntityUnloadable {
	/** Gets called Client Side, when you mouse over this TileEntity. return true to prevent other things from rendering. */
	public boolean onDrawBlockHighlight(RenderHighlightEvent.Block aEvent);
}

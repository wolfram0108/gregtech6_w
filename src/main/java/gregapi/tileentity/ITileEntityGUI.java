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

package gregapi.tileentity;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.minecraft.world.entity.player.Player;

/**
 * @author Gregorius Techneticies
 */
public interface ITileEntityGUI extends ITileEntityUnloadable {
	/** Gets the GUI Elements. Negative GUIIDs are internal Usage. For example -1, -2, -3, -4, -5 and -6 are the Covers on the Side -GUIID-1 */
	@OnlyIn(Dist.CLIENT)
	public Object getGUIClient(int aGUIID, Player aPlayer);
	/** Gets the GUI Elements. Negative GUIIDs are internal Usage. For example -1, -2, -3, -4, -5 and -6 are the Covers on the Side -GUIID-1 */
	public Object getGUIServer(int aGUIID, Player aPlayer);
	
	/** Opens the GUI with this ID of this TileEntity */
	public boolean openGUI(Player aPlayer, int aID);
	/** Opens the GUI with the ID = 0 of this TileEntity */
	public boolean openGUI(Player aPlayer);
}

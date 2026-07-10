/**
 * Copyright (c) 2023 GregTech-6 Team
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

package gregapi.cover.covers;

import gregapi.cover.CoverData;
import gregapi.render.ITexture;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.inventory.ContainerWorkbench;
import net.minecraft.network.play.server.S2DPacketOpenWindow;

import static gregapi.data.CS.F;
import static gregapi.data.CS.T;

/**
 * @author Gregorius Techneticies
 */
public class CoverCrafting extends CoverTextureMulti {
	public CoverCrafting(ITexture... aTextures) {
		super(T, T, aTextures);
	}
	
	public CoverCrafting(String aFolder, int aAmount) {
		super(T, T, aFolder, aAmount);
	}
	
	@Override
	public boolean onCoverClickedRight(byte aSide, CoverData aData, Entity aPlayer, byte aSideClicked, float aHitX, float aHitY, float aHitZ) {
		if (aPlayer instanceof ServerPlayer) {
			((ServerPlayer)aPlayer).getNextWindowId();
			((ServerPlayer)aPlayer).playerNetServerHandler.sendPacket(new S2DPacketOpenWindow(((ServerPlayer)aPlayer).currentWindowId, 1, "Crafting", 9, T));
			((ServerPlayer)aPlayer).openContainer = new ContainerWorkbench(((ServerPlayer)aPlayer).inventory, ((ServerPlayer)aPlayer).worldObj, aData.mTileEntity.getX(), aData.mTileEntity.getY(), aData.mTileEntity.getZ()) {@Override public boolean canInteractWith(Player par1EntityPlayer) {return T;}};
			((ServerPlayer)aPlayer).openContainer.windowId = ((ServerPlayer)aPlayer).currentWindowId;
			((ServerPlayer)aPlayer).openContainer.addCraftingToCrafters(((ServerPlayer)aPlayer));
		}
		return T;
	}
	
	@Override public boolean isSealable(byte aCoverSide, CoverData aData) {return F;}
	@Override public boolean isDecorative(byte aCoverSide, CoverData aData) {return T;}
	@Override public boolean showsConnectorFront(byte aCoverSide, CoverData aData) {return F;}
}

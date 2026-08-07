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
 *
 * Modified in 2026 for the GregTech 6 NeoForge port
 * (https://github.com/wolfram0108/gregtech6_w): ported from Minecraft 1.7.10 / Forge
 * to Minecraft 26.1.2 / NeoForge.
 */

package gregapi.cover.covers;

import gregapi.cover.CoverData;
import gregapi.render.ITexture;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
// F14: 1.7.10 ContainerWorkbench/S2DPacketOpenWindow удалены -> neo openMenu+CraftingMenu (см. onCoverClickedRight).

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
			// F14: 1.7.10 ручное открытие GUI (getNextWindowId + S2DPacketOpenWindow + set containerMenu +
			// addCraftingToCrafters) удалено — neo ServerPlayer.openMenu(MenuProvider) делает всё централизованно
			// (счётчик окна, пакет, привязка меню, синхронизация). ContainerWorkbench->CraftingMenu (CraftingMenu.java:38).
			// ContainerLevelAccess.NULL (ContainerLevelAccess.java:10) = stillValid всегда true — воспроизводит
			// оригинальный override canInteractWith->true (кавер не настоящий верстак, доступен всегда).
			((ServerPlayer)aPlayer).openMenu(new net.minecraft.world.SimpleMenuProvider((aId, aInv, aP) -> new net.minecraft.world.inventory.CraftingMenu(aId, aInv, net.minecraft.world.inventory.ContainerLevelAccess.NULL), net.minecraft.network.chat.Component.translatable("container.crafting")));
		}
		return T;
	}
	
	@Override public boolean isSealable(byte aCoverSide, CoverData aData) {return F;}
	@Override public boolean isDecorative(byte aCoverSide, CoverData aData) {return T;}
	@Override public boolean showsConnectorFront(byte aCoverSide, CoverData aData) {return F;}
}

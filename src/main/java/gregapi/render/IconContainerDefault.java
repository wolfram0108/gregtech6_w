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

package gregapi.render;

import static gregapi.data.CS.*;

// The block atlas belongs to the shared holder, not the client one: TextureAtlas is
// @OnlyIn(Dist.CLIENT), and loading it on a dedicated server crashes the class.
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.resources.ResourceLocation;

/** Constructors took IIcon (removed); they now take ResourceLocation as a neutral texture reference.
 *  Forward-compatible with a future Material(ResourceLocation). */
public class IconContainerDefault implements IIconContainer {
	public final ResourceLocation mTextureFile;
	public final ResourceLocation mIcon;
	public final short[] mRGBa;

	public IconContainerDefault(ResourceLocation aIcon, short[] aRGBa, ResourceLocation aTextureFile) {
		mIcon = aIcon; mRGBa = aRGBa; mTextureFile = aTextureFile;
	}

	public IconContainerDefault(ResourceLocation aIcon, short[] aRGBa, boolean aIsBlockTexture) {
		mIcon = aIcon; mRGBa = aRGBa; mTextureFile = (aIsBlockTexture ? InventoryMenu.BLOCK_ATLAS : gregapi.render.GT6QuadBuilder.LOCATION_ITEMS);
	}

	public IconContainerDefault(ResourceLocation aIcon, short[] aRGBa) {
		mIcon = aIcon; mRGBa = aRGBa; mTextureFile = InventoryMenu.BLOCK_ATLAS;
	}

	public IconContainerDefault(ResourceLocation aIcon, ResourceLocation aTextureFile) {
		mIcon = aIcon; mRGBa = UNCOLOURED; mTextureFile = aTextureFile;
	}

	public IconContainerDefault(ResourceLocation aIcon, boolean aIsBlockTexture) {
		mIcon = aIcon; mRGBa = UNCOLOURED; mTextureFile = (aIsBlockTexture ? InventoryMenu.BLOCK_ATLAS : gregapi.render.GT6QuadBuilder.LOCATION_ITEMS);
	}

	public IconContainerDefault(ResourceLocation aIcon) {
		mIcon = aIcon; mRGBa = UNCOLOURED; mTextureFile = InventoryMenu.BLOCK_ATLAS;
	}

	@Override
	public ResourceLocation getIcon(int aRenderPass) {
		return mIcon;
	}

	@Override
	public boolean isUsingColorModulation(int aRenderPass) {
		return mRGBa == UNCOLOURED;
	}

	@Override
	public short[] getIconColor(int aRenderPass) {
		return mRGBa;
	}

	@Override
	public int getIconPasses() {
		return 1;
	}

	@Override
	public ResourceLocation getTextureFile() {
		return mTextureFile;
	}

	@Override
	public void registerIcons(Object aIconRegister) {
		//
	}
}

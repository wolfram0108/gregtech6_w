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

package gregapi.render;

import static gregapi.data.CS.*;

import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.util.IIcon;
import net.minecraft.resources.Identifier;

public class IconContainerDefault implements IIconContainer {
	public final Identifier mTextureFile;
	public final IIcon mIcon;
	public final short[] mRGBa;
	
	public IconContainerDefault(IIcon aIcon, short[] aRGBa, Identifier aTextureFile) {
		mIcon = aIcon; mRGBa = aRGBa; mTextureFile = aTextureFile;
	}
	
	public IconContainerDefault(IIcon aIcon, short[] aRGBa, boolean aIsBlockTexture) {
		mIcon = aIcon; mRGBa = aRGBa; mTextureFile = (aIsBlockTexture ? TextureAtlas.locationBlocksTexture : TextureAtlas.locationItemsTexture);
	}
	
	public IconContainerDefault(IIcon aIcon, short[] aRGBa) {
		mIcon = aIcon; mRGBa = aRGBa; mTextureFile = TextureAtlas.locationBlocksTexture;
	}
	
	public IconContainerDefault(IIcon aIcon, Identifier aTextureFile) {
		mIcon = aIcon; mRGBa = UNCOLOURED; mTextureFile = aTextureFile;
	}
	
	public IconContainerDefault(IIcon aIcon, boolean aIsBlockTexture) {
		mIcon = aIcon; mRGBa = UNCOLOURED; mTextureFile = (aIsBlockTexture ? TextureAtlas.locationBlocksTexture : TextureAtlas.locationItemsTexture);
	}
	
	public IconContainerDefault(IIcon aIcon) {
		mIcon = aIcon; mRGBa = UNCOLOURED; mTextureFile = TextureAtlas.locationBlocksTexture;
	}
	
	@Override
	public IIcon getIcon(int aRenderPass) {
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
	public Identifier getTextureFile() {
		return mTextureFile;
	}
	
	@Override
	public void registerIcons(IIconRegister aIconRegister) {
		//
	}
}

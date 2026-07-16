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

import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.Identifier;

/**
 * F3 superseded-render (GT6BlockModel/ItemModel пайплайн; старый getIcon/immediate-mode мёртв, 0 вызовов neo): конструкторы принимали {@code IIcon} (тип удалён в 26.1.2) —
 * теперь принимают {@link Identifier} как нейтральный держатель ссылки на текстуру, форвард-совместимый
 * с будущим {@code Material(Identifier)} (decisions/F3-render.md §2.3).
 */
public class IconContainerDefault implements IIconContainer {
	public final Identifier mTextureFile;
	public final Identifier mIcon;
	public final short[] mRGBa;

	public IconContainerDefault(Identifier aIcon, short[] aRGBa, Identifier aTextureFile) {
		mIcon = aIcon; mRGBa = aRGBa; mTextureFile = aTextureFile;
	}

	public IconContainerDefault(Identifier aIcon, short[] aRGBa, boolean aIsBlockTexture) {
		mIcon = aIcon; mRGBa = aRGBa; mTextureFile = (aIsBlockTexture ? TextureAtlas.LOCATION_BLOCKS : TextureAtlas.LOCATION_ITEMS);
	}

	public IconContainerDefault(Identifier aIcon, short[] aRGBa) {
		mIcon = aIcon; mRGBa = aRGBa; mTextureFile = TextureAtlas.LOCATION_BLOCKS;
	}

	public IconContainerDefault(Identifier aIcon, Identifier aTextureFile) {
		mIcon = aIcon; mRGBa = UNCOLOURED; mTextureFile = aTextureFile;
	}

	public IconContainerDefault(Identifier aIcon, boolean aIsBlockTexture) {
		mIcon = aIcon; mRGBa = UNCOLOURED; mTextureFile = (aIsBlockTexture ? TextureAtlas.LOCATION_BLOCKS : TextureAtlas.LOCATION_ITEMS);
	}

	public IconContainerDefault(Identifier aIcon) {
		mIcon = aIcon; mRGBa = UNCOLOURED; mTextureFile = TextureAtlas.LOCATION_BLOCKS;
	}

	@Override
	public Identifier getIcon(int aRenderPass) {
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
	public void registerIcons(Object aIconRegister) {
		//
	}
}

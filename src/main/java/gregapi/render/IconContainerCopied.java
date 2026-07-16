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

import net.minecraft.world.level.block.Block;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.Identifier;

/**
 * @author Gregorius Techneticies
 *
 * Copies the Icon of another Block's Side+Meta (e.g. Dirt below Grass). PORT-TODO(F3, baked-рендер
 * клиента): {@code Block.getIcon(side,meta)} удалён в 26.1.2 (мёртвый immediate-mode метод, см.
 * REMAP-RULES §C2) — до baked-фазы {@link #getIcon(int)} отдаёт {@code null}-держатель.
 */
public class IconContainerCopied implements IIconContainer {
	private final Block mBlock;
	private final byte mSide, mMeta;
	public short[] mRGBa;

	public IconContainerCopied(Block aBlock, long aMeta, long aSide, short[] aRGBa) {
		mBlock = aBlock; mMeta = (byte)aMeta; mSide = (byte)aSide; mRGBa = aRGBa;
	}
	public IconContainerCopied(Block aBlock, long aMeta, long aSide) {
		mBlock = aBlock; mMeta = (byte)aMeta; mSide = (byte)aSide; mRGBa = UNCOLOURED;
	}

	@Override
	public Identifier getIcon(int aRenderPass) {
		// PORT-TODO(F3, baked-рендер клиента): было mBlock.getIcon(mSide, mMeta) — Block.getIcon удалён (neo BakedModel-рендер);
		// crash-only per /goal (F3-фаза заменит реальной моделью).
		throw new UnsupportedOperationException("PORT-TODO(F3, baked-рендер): neo BakedModel-рендер, 1.7.10 getIcon мёртв — crash-only per /goal");
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
		return TextureAtlas.LOCATION_BLOCKS;
	}

	@Override
	public void registerIcons(Object aIconRegister) {
		//
	}
}

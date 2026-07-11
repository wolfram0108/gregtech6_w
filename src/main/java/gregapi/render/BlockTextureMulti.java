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

/**
 * @author Gregorius Techneticies
 *
 * Lets Multiple ITextures Render overlay over each other.
 *
 * I should have done this much earlier...
 *
 * PORT-TODO(F3, baked-рендер клиента): {@code RenderBlocks} удалён в 26.1.2 — параметр заменён
 * нейтральным держателем {@code Object aRenderer} (см. {@link ITexture}).
 */
public class BlockTextureMulti implements ITexture {
	private final ITexture[] mTextures;
	
	public static BlockTextureMulti get(ITexture... aTextures) {
		return CODE_CLIENT||CODE_UNCHECKED?new BlockTextureMulti(aTextures):null;
	}
	
	public BlockTextureMulti(ITexture... aTextures) {
		mTextures = aTextures;
	}
	
	@Override
	public void renderXPos(Object aRenderer, Block aBlock, int aX, int aY, int aZ, int aBrightness, boolean aChangedBlockBounds) {
		for (ITexture tTexture : mTextures) if (tTexture != null && tTexture.isValidTexture()) tTexture.renderXPos(aRenderer, aBlock, aX, aY, aZ, aBrightness, aChangedBlockBounds);
	}
	
	@Override
	public void renderXNeg(Object aRenderer, Block aBlock, int aX, int aY, int aZ, int aBrightness, boolean aChangedBlockBounds) {
		for (ITexture tTexture : mTextures) if (tTexture != null && tTexture.isValidTexture()) tTexture.renderXNeg(aRenderer, aBlock, aX, aY, aZ, aBrightness, aChangedBlockBounds);
	}
	
	@Override
	public void renderYPos(Object aRenderer, Block aBlock, int aX, int aY, int aZ, int aBrightness, boolean aChangedBlockBounds) {
		for (ITexture tTexture : mTextures) if (tTexture != null && tTexture.isValidTexture()) tTexture.renderYPos(aRenderer, aBlock, aX, aY, aZ, aBrightness, aChangedBlockBounds);
	}
	
	@Override
	public void renderYNeg(Object aRenderer, Block aBlock, int aX, int aY, int aZ, int aBrightness, boolean aChangedBlockBounds) {
		for (ITexture tTexture : mTextures) if (tTexture != null && tTexture.isValidTexture()) tTexture.renderYNeg(aRenderer, aBlock, aX, aY, aZ, aBrightness, aChangedBlockBounds);
	}
	
	@Override
	public void renderZPos(Object aRenderer, Block aBlock, int aX, int aY, int aZ, int aBrightness, boolean aChangedBlockBounds) {
		for (ITexture tTexture : mTextures) if (tTexture != null && tTexture.isValidTexture()) tTexture.renderZPos(aRenderer, aBlock, aX, aY, aZ, aBrightness, aChangedBlockBounds);
	}
	
	@Override
	public void renderZNeg(Object aRenderer, Block aBlock, int aX, int aY, int aZ, int aBrightness, boolean aChangedBlockBounds) {
		for (ITexture tTexture : mTextures) if (tTexture != null && tTexture.isValidTexture()) tTexture.renderZNeg(aRenderer, aBlock, aX, aY, aZ, aBrightness, aChangedBlockBounds);
	}
	
	@Override
	public boolean isValidTexture() {
		return mTextures.length > 0;
	}
}

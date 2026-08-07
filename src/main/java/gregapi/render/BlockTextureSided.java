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

import net.minecraft.world.level.block.Block;

/**
 * @author Gregorius Techneticies
 *
 * F3 superseded-render (GT6BlockModel/ItemModel пайплайн; старый getIcon/immediate-mode мёртв, 0 вызовов neo): {@code RenderBlocks} удалён в 26.1.2 — параметр заменён
 * нейтральным держателем {@code Object aRenderer} (см. {@link ITexture}).
 */
public class BlockTextureSided implements ITexture {
	private final IIconContainer[] mIconContainers;
	
	/**
	 *  DO NOT MANIPULATE THE VALUES INSIDE THIS ARRAY!!!
	 *  
	 *  Just set this variable to another different Array instead.
	 *  Otherwise some coloured things will get Problems.
	 */
	public short[] mRGBa;
	
	private final boolean mAllowAlpha, mUseMaxBrightness;

	public static BlockTextureSided get(IIconContainer aIcon0, IIconContainer aIcon1, IIconContainer aIcon2, IIconContainer aIcon3, IIconContainer aIcon4, IIconContainer aIcon5, short[] aRGBa, boolean aAllowAlpha, boolean aUseMaxBrightness) {
		return CODE_CLIENT||CODE_UNCHECKED?new BlockTextureSided(aIcon0, aIcon1, aIcon2, aIcon3, aIcon4, aIcon5, aRGBa, aAllowAlpha, aUseMaxBrightness):null;
	}
	
	public static BlockTextureSided get(IIconContainer aBottom, IIconContainer aTop, IIconContainer aSides, short[] aRGBa, boolean aAllowAlpha, boolean aUseMaxBrightness) {
		return CODE_CLIENT||CODE_UNCHECKED?new BlockTextureSided(aBottom, aTop, aSides, aRGBa, aAllowAlpha, aUseMaxBrightness):null;
	}
	
	public BlockTextureSided(IIconContainer aIcon0, IIconContainer aIcon1, IIconContainer aIcon2, IIconContainer aIcon3, IIconContainer aIcon4, IIconContainer aIcon5, short[] aRGBa, boolean aAllowAlpha, boolean aUseMaxBrightness) {
		if (aRGBa.length != 4) throw new IllegalArgumentException("RGBa doesn't have 4 Values @ BlockTextureSided");
		mIconContainers = new IIconContainer[] {aIcon0, aIcon1, aIcon2, aIcon3, aIcon4, aIcon5};
		mUseMaxBrightness = aUseMaxBrightness;
		mAllowAlpha = aAllowAlpha;
		mRGBa = aRGBa;
	}
	
	public BlockTextureSided(IIconContainer aBottom, IIconContainer aTop, IIconContainer aSides, short[] aRGBa, boolean aAllowAlpha, boolean aUseMaxBrightness) {
		this(aBottom, aTop, aSides, aSides, aSides, aSides, aRGBa, aAllowAlpha, aUseMaxBrightness);
	}
	
	@Override
	public void renderXPos(Object aRenderer, Block aBlock, int aX, int aY, int aZ, int aBrightness, boolean aChangedBlockBounds) {
		final byte aSide = 5;
		for (int i = 0, j = mIconContainers[aSide].getIconPasses(); i < j; i++)
		ITexture.Util.renderZPos(mIconContainers[aSide].getIcon(0), mIconContainers[aSide].isUsingColorModulation(i)?mRGBa:mIconContainers[aSide].getIconColor(i), mAllowAlpha, mUseMaxBrightness, true, aRenderer, aBlock, aX, aY, aZ, mUseMaxBrightness?240:aBrightness, aChangedBlockBounds);
	}
	
	@Override
	public void renderXNeg(Object aRenderer, Block aBlock, int aX, int aY, int aZ, int aBrightness, boolean aChangedBlockBounds) {
		final byte aSide = 4;
		for (int i = 0, j = mIconContainers[aSide].getIconPasses(); i < j; i++)
		ITexture.Util.renderZPos(mIconContainers[aSide].getIcon(0), mIconContainers[aSide].isUsingColorModulation(i)?mRGBa:mIconContainers[aSide].getIconColor(i), mAllowAlpha, mUseMaxBrightness, true, aRenderer, aBlock, aX, aY, aZ, mUseMaxBrightness?240:aBrightness, aChangedBlockBounds);
	}
	
	@Override
	public void renderYPos(Object aRenderer, Block aBlock, int aX, int aY, int aZ, int aBrightness, boolean aChangedBlockBounds) {
		final byte aSide = 1;
		for (int i = 0, j = mIconContainers[aSide].getIconPasses(); i < j; i++)
		ITexture.Util.renderZPos(mIconContainers[aSide].getIcon(0), mIconContainers[aSide].isUsingColorModulation(i)?mRGBa:mIconContainers[aSide].getIconColor(i), mAllowAlpha, mUseMaxBrightness, true, aRenderer, aBlock, aX, aY, aZ, mUseMaxBrightness?240:aBrightness, aChangedBlockBounds);
	}
	
	@Override
	public void renderYNeg(Object aRenderer, Block aBlock, int aX, int aY, int aZ, int aBrightness, boolean aChangedBlockBounds) {
		final byte aSide = 0;
		for (int i = 0, j = mIconContainers[aSide].getIconPasses(); i < j; i++)
		ITexture.Util.renderZPos(mIconContainers[aSide].getIcon(0), mIconContainers[aSide].isUsingColorModulation(i)?mRGBa:mIconContainers[aSide].getIconColor(i), mAllowAlpha, mUseMaxBrightness, true, aRenderer, aBlock, aX, aY, aZ, mUseMaxBrightness?240:aBrightness, aChangedBlockBounds);
	}
	
	@Override
	public void renderZPos(Object aRenderer, Block aBlock, int aX, int aY, int aZ, int aBrightness, boolean aChangedBlockBounds) {
		final byte aSide = 3;
		for (int i = 0, j = mIconContainers[aSide].getIconPasses(); i < j; i++)
		ITexture.Util.renderZPos(mIconContainers[aSide].getIcon(0), mIconContainers[aSide].isUsingColorModulation(i)?mRGBa:mIconContainers[aSide].getIconColor(i), mAllowAlpha, mUseMaxBrightness, true, aRenderer, aBlock, aX, aY, aZ, mUseMaxBrightness?240:aBrightness, aChangedBlockBounds);
	}
	
	@Override
	public void renderZNeg(Object aRenderer, Block aBlock, int aX, int aY, int aZ, int aBrightness, boolean aChangedBlockBounds) {
		final byte aSide = 2;
		for (int i = 0, j = mIconContainers[aSide].getIconPasses(); i < j; i++)
		ITexture.Util.renderZPos(mIconContainers[aSide].getIcon(0), mIconContainers[aSide].isUsingColorModulation(i)?mRGBa:mIconContainers[aSide].getIconColor(i), mAllowAlpha, mUseMaxBrightness, true, aRenderer, aBlock, aX, aY, aZ, mUseMaxBrightness?240:aBrightness, aChangedBlockBounds);
	}
	
	public short[] getRGBA() {
		return mRGBa;
	}
	
	@Override
	public boolean isValidTexture() {
		return mIconContainers != null && mIconContainers[0] != null && mIconContainers[1] != null && mIconContainers[2] != null && mIconContainers[3] != null && mIconContainers[4] != null && mIconContainers[5] != null;
	}
}

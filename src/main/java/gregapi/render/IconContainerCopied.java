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
// The block atlas belongs to the shared holder, not the client one: TextureAtlas is
// @OnlyIn(Dist.CLIENT), and loading it on a dedicated server crashes the class.
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.resources.ResourceLocation;

/** @author Gregorius Techneticies
 *  Copies the icon of another block's side+meta (e.g. dirt below grass).
 *  Block.getIcon(side,meta) is gone in neo; the sprite comes from the copied block's baked BlockStateModel instead. */
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
	public ResourceLocation getIcon(int aRenderPass) {
		// Was mBlock.getIcon(mSide,mMeta), now resolved from the copied block's baked model via the centralized
		// resolveBlockFaceIcon; exceptions fall back to RENDERING_ERROR since models may not be ready outside the render tick.
		try {
			return GT6QuadBuilder.resolveBlockFaceIcon(mBlock, mSide, mMeta);
		} catch (Throwable e) {
			return gregapi.old.Textures.BlockIcons.RENDERING_ERROR.getIcon(0);
		}
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
		return InventoryMenu.BLOCK_ATLAS;
	}

	@Override
	public void registerIcons(Object aIconRegister) {
		//
	}
}

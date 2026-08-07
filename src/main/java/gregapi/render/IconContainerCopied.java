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
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.Identifier;

/**
 * @author Gregorius Techneticies
 *
 * Copies the Icon of another Block's Side+Meta (e.g. Dirt below Grass). F3 block-icon-data ЗАКРЫТ:
 * {@code Block.getIcon(side,meta)} удалён в 26.1.2 (baked-model рендер) — {@link #getIcon(int)} резолвит спрайт
 * грани копируемого блока из его baked {@code BlockStateModel} ({@link GT6QuadBuilder#resolveBlockFaceIcon}).
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
		// F3 block-icon-data: было mBlock.getIcon(mSide, mMeta) — Block.getIcon удалён (neo baked-model рендер);
		// спрайт грани копируемого блока резолвим из его baked BlockStateModel (централизованный §3
		// GT6QuadBuilder.resolveBlockFaceIcon), mMeta учтён (Flattening-варианты). catch→RENDERING_ERROR — модели могут быть не готовы вне рендер-тика.
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
	public Identifier getTextureFile() {
		return TextureAtlas.LOCATION_BLOCKS;
	}

	@Override
	public void registerIcons(Object aIconRegister) {
		//
	}
}

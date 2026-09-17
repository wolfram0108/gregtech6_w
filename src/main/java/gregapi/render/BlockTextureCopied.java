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

import gregapi.util.UT;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.resources.ResourceLocation;

/** @author Gregorius Techneticies
 *  Glow (fire/lava/glowstone/lit redstone lamp) is DATA ported 1:1; Block.getIcon(side,meta) is gone in neo, so
 *  copied-block face icons resolve from the baked BlockStateModel and render color from vanillaRenderColor below. */
public class BlockTextureCopied implements ITexture {
	private final Block mBlock;
	private final byte mSide, mMeta;

	/**
	 *  DO NOT MANIPULATE THE VALUES INSIDE THIS ARRAY!!!
	 *
	 *  Just set this variable to another different Array instead.
	 *  Otherwise some colored things will get Problems.
	 */
	public short[] mRGBa;

	private final boolean mAllowAlpha, mUseMaxBrightness, mUseConstantBrightness;

	public static BlockTextureCopied get(Block aBlock, int aSide, int aMeta, short[] aRGBa, boolean aAllowAlpha, boolean aUseMaxBrightness, boolean aUseConstantBrightness) {
		return (CODE_CLIENT||CODE_UNCHECKED)&&aBlock!=null&&aBlock!=NB?new BlockTextureCopied(aBlock, aSide, aMeta, aRGBa, aAllowAlpha, aUseMaxBrightness, aUseConstantBrightness):null;
	}

	public static BlockTextureCopied get(Block aBlock, int aSide, int aMeta, int aRGBa, boolean aAllowAlpha, boolean aUseMaxBrightness, boolean aUseConstantBrightness) {
		return (CODE_CLIENT||CODE_UNCHECKED)&&aBlock!=null&&aBlock!=NB?new BlockTextureCopied(aBlock, aSide, aMeta, aRGBa, aAllowAlpha, aUseMaxBrightness, aUseConstantBrightness):null;
	}

	public static BlockTextureCopied get(Block aBlock, int aSide, int aMeta) {
		return (CODE_CLIENT||CODE_UNCHECKED)&&aBlock!=null&&aBlock!=NB?new BlockTextureCopied(aBlock, aSide, aMeta):null;
	}

	public static BlockTextureCopied get(Block aBlock, int aMeta) {
		return (CODE_CLIENT||CODE_UNCHECKED)&&aBlock!=null&&aBlock!=NB?new BlockTextureCopied(aBlock, SIDE_ANY, aMeta):null;
	}

	public static BlockTextureCopied get(Block aBlock) {
		return (CODE_CLIENT||CODE_UNCHECKED)&&aBlock!=null&&aBlock!=NB?new BlockTextureCopied(aBlock, SIDE_ANY, 0):null;
	}

	public BlockTextureCopied(Block aBlock, int aSide, int aMeta, short[] aRGBa, boolean aAllowAlpha, boolean aUseMaxBrightness, boolean aUseConstantBrightness) {
		if (aRGBa.length != 4) throw new IllegalArgumentException("RGBa doesn't have 4 Values @ BlockTextureCopied");
		mBlock = aBlock;
		mRGBa = aRGBa;
		mSide = (byte)aSide;
		mMeta = (byte)aMeta;
		mAllowAlpha = aAllowAlpha;
		mUseMaxBrightness = aUseMaxBrightness;
		mUseConstantBrightness = aUseConstantBrightness;
	}

	public BlockTextureCopied(Block aBlock, int aSide, int aMeta, int aRGBa, boolean aAllowAlpha, boolean aUseMaxBrightness, boolean aUseConstantBrightness) {
		mBlock = aBlock;
		mRGBa = UT.Code.getRGBaArray(aRGBa);
		mSide = (byte)aSide;
		mMeta = (byte)aMeta;
		mAllowAlpha = aAllowAlpha;
		mUseMaxBrightness = aUseMaxBrightness;
		mUseConstantBrightness = aUseConstantBrightness;
	}

	public BlockTextureCopied(Block aBlock, int aSide, int aMeta, int aRGBa, boolean aAllowAlpha, boolean aGlow) {
		mBlock = aBlock;
		mRGBa = UT.Code.getRGBaArray(aRGBa);
		mSide = (byte)aSide;
		mMeta = (byte)aMeta;
		mAllowAlpha = aAllowAlpha;
		mUseMaxBrightness = aGlow;
		mUseConstantBrightness = aGlow;
	}

	public BlockTextureCopied(Block aBlock, int aSide, int aMeta) {
		// Glow is DATA (self-lit vanilla blocks); flowing lava is just Blocks.LAVA with a FluidState, already covered here.
		// Color comes from IBlock#getRenderColor for GT6 blocks, else vanillaRenderColor below (1.7.10 default: white).
		this(aBlock, aSide, aMeta
			, aBlock instanceof gregapi.block.IBlock tGT6 ? tGT6.getRenderColor(aMeta) : vanillaRenderColor(aBlock, aMeta), F
			, aBlock == Blocks.FIRE || aBlock == Blocks.LAVA || aBlock == Blocks.GLOWSTONE
			, aBlock == Blocks.FIRE || aBlock == Blocks.LAVA || aBlock == Blocks.GLOWSTONE);
	}

	/** Restores 1.7.10 Block.getRenderColor(meta) for vanilla blocks: a static color, independent of world or biome.
	 *  Data is every vanilla override from 1.7.10 (leaves, grass, vines, lily pad, stem); other mods get the default color. */
	private static int vanillaRenderColor(Block aBlock, int aMeta) {
		if (aBlock == Blocks.SPRUCE_LEAVES) return net.minecraft.world.level.FoliageColor.getEvergreenColor() & 0xFFFFFF;
		if (aBlock == Blocks.BIRCH_LEAVES)  return net.minecraft.world.level.FoliageColor.getBirchColor() & 0xFFFFFF;
		if (aBlock == Blocks.OAK_LEAVES || aBlock == Blocks.JUNGLE_LEAVES || aBlock == Blocks.ACACIA_LEAVES || aBlock == Blocks.DARK_OAK_LEAVES || aBlock == Blocks.VINE)
			return net.minecraft.world.level.FoliageColor.get(0.5, 1.0) & 0xFFFFFF;
		if (aBlock == Blocks.GRASS_BLOCK || aBlock == Blocks.GRASS || aBlock == Blocks.FERN)
			return net.minecraft.world.level.GrassColor.getDefaultColor() & 0xFFFFFF;
		if (aBlock == Blocks.LILY_PAD) return 2129968;
		if (aBlock == Blocks.PUMPKIN_STEM || aBlock == Blocks.MELON_STEM || aBlock == Blocks.ATTACHED_PUMPKIN_STEM || aBlock == Blocks.ATTACHED_MELON_STEM)
			return (aMeta * 32) << 16 | (255 - aMeta * 8) << 8 | aMeta * 4;
		return UT.Code.getRGBInt(UNCOLOURED);
	}

	private ResourceLocation getIcon(int aSide) {
		// Block.getIcon is gone in neo (baked-model rendering); the face sprite is resolved from the vanilla block's baked
		// BlockStateModel via the centralized GT6QuadBuilder.resolveBlockFaceIcon, meta included for Flattening variants.
		try {
			return GT6QuadBuilder.resolveBlockFaceIcon(mBlock, mSide == SIDE_ANY ? aSide : mSide, mMeta);
		} catch (Throwable e) {
			return gregapi.old.Textures.BlockIcons.RENDERING_ERROR.getIcon(0);
		}
	}

	@Override
	public void renderXPos(Object aRenderer, Block aBlock, int aX, int aY, int aZ, int aBrightness, boolean aChangedBlockBounds) {
		ITexture.Util.renderSide(SIDE_X_POS, getIcon(5), mRGBa, mAllowAlpha, mUseConstantBrightness, !mUseMaxBrightness, aRenderer, aBlock, aX, aY, aZ, mUseMaxBrightness?240:aBrightness, aChangedBlockBounds);
	}

	@Override
	public void renderXNeg(Object aRenderer, Block aBlock, int aX, int aY, int aZ, int aBrightness, boolean aChangedBlockBounds) {
		ITexture.Util.renderSide(SIDE_X_NEG, getIcon(4), mRGBa, mAllowAlpha, mUseConstantBrightness, !mUseMaxBrightness, aRenderer, aBlock, aX, aY, aZ, mUseMaxBrightness?240:aBrightness, aChangedBlockBounds);
	}

	@Override
	public void renderYPos(Object aRenderer, Block aBlock, int aX, int aY, int aZ, int aBrightness, boolean aChangedBlockBounds) {
		ITexture.Util.renderSide(SIDE_Y_POS, getIcon(1), mRGBa, mAllowAlpha, mUseConstantBrightness, !mUseMaxBrightness, aRenderer, aBlock, aX, aY, aZ, mUseMaxBrightness?240:aBrightness, aChangedBlockBounds);
	}

	@Override
	public void renderYNeg(Object aRenderer, Block aBlock, int aX, int aY, int aZ, int aBrightness, boolean aChangedBlockBounds) {
		ITexture.Util.renderSide(SIDE_Y_NEG, getIcon(0), mRGBa, mAllowAlpha, mUseConstantBrightness, !mUseMaxBrightness, aRenderer, aBlock, aX, aY, aZ, mUseMaxBrightness?240:aBrightness, aChangedBlockBounds);
	}

	@Override
	public void renderZPos(Object aRenderer, Block aBlock, int aX, int aY, int aZ, int aBrightness, boolean aChangedBlockBounds) {
		ITexture.Util.renderSide(SIDE_Z_POS, getIcon(3), mRGBa, mAllowAlpha, mUseConstantBrightness, !mUseMaxBrightness, aRenderer, aBlock, aX, aY, aZ, mUseMaxBrightness?240:aBrightness, aChangedBlockBounds);
	}

	@Override
	public void renderZNeg(Object aRenderer, Block aBlock, int aX, int aY, int aZ, int aBrightness, boolean aChangedBlockBounds) {
		ITexture.Util.renderSide(SIDE_Z_NEG, getIcon(2), mRGBa, mAllowAlpha, mUseConstantBrightness, !mUseMaxBrightness, aRenderer, aBlock, aX, aY, aZ, mUseMaxBrightness?240:aBrightness, aChangedBlockBounds);
	}

	@Override
	public boolean isValidTexture() {
		return mBlock != null && mBlock != NB;
	}
}

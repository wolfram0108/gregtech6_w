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

import gregapi.util.UT;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.resources.Identifier;

/**
 * @author Gregorius Techneticies
 *
 * glow-эвристика (fire/lava/flowing_lava/glowstone/lit_redstone_lamp) — это ДАННЫЕ (набор
 * самосветящихся ванильных блоков), перенесена 1:1 (REMAP-RULES §A: данные не гатим).
 * PORT-TODO(F3, block-icon-data): {@code Block.getIcon(side,meta)} и {@code Block.getRenderColor(meta)}
 * удалены из neo {@code Block} (REMAP-RULES §C2) — прямого 1:1-доступа к цвету/иконке блока нет;
 * до block-icon-data фазы держатель иконки — {@code null}, цвет — {@code UNCOLOURED} (заглушки
 * помечены явно, оригинальные строки сохранены — это не тихое обнуление данных).
 */
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
		// glow — ДАННЫЕ (самосветящиеся ванильные блоки). Оригинал (BlockTextureCopied.java:100):
		//   aBlock == Blocks.FIRE || Blocks.LAVA || Blocks.LAVA || Blocks.GLOWSTONE || Blocks.REDSTONE_LAMP
		// fire/lava/glowstone → neo 1:1 (REMAP-RULES §C блок-флэттен: lowercase→uppercase neo-константа).
		// PORT-TODO(F3/block-flatten): flowing_lava (в neo — та же Blocks.LAVA с FluidState, отдельного блока нет)
		// и lit_redstone_lamp (в neo — Blocks.REDSTONE_LAMP с blockstate-свойством LIT, отдельного блока нет)
		// block-идентичности не имеют — 2 токена glow-набора восстановятся при закрытии шва block-flatten.
		// PORT-TODO(F3, block-icon-data): 4-й аргумент был aBlock.getRenderColor(aMeta) (метод удалён из neo
		// Block, REMAP-RULES §C2) — цвет рендера блока 1:1-доступа не имеет; передаём UNCOLOURED-заглушку.
		this(aBlock, aSide, aMeta, UNCOLOURED, F
			, aBlock == Blocks.FIRE || aBlock == Blocks.LAVA || aBlock == Blocks.GLOWSTONE
			, aBlock == Blocks.FIRE || aBlock == Blocks.LAVA || aBlock == Blocks.GLOWSTONE);
	}

	private Identifier getIcon(int aSide) {
		// PORT-TODO(F3, block-icon-data): было (try) mSide==SIDE_ANY ? mBlock.getIcon(aSide, mMeta) : mBlock.getIcon(mSide, mMeta)
		// с fallback Textures.BlockIcons.RENDERING_ERROR.getIcon(0) — Block.getIcon удалён из neo (REMAP-RULES §C2), 1:1 нет.
		return null;
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

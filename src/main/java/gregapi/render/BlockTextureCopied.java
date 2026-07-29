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
 * F3 block-icon-data ЗАКРЫТ: {@code Block.getIcon(side,meta)} удалён из neo (baked-model рендер) — спрайт грани
 * копируемого ванильного блока резолвится из его baked {@code BlockStateModel} ({@link GT6QuadBuilder#resolveBlockFaceIcon}).
 * PORT-TODO(F3, block-render-color): {@code Block.getRenderColor(meta)} удалён из neo {@code Block} (REMAP-RULES §C2) —
 * 1:1-доступа к цвету рендера блока нет (для биом-тинта нужен neo {@code BlockColors}); тинт — {@code UNCOLOURED}
 * (заглушка помечена явно, оригинальная строка сохранена — не тихое обнуление; корректно для всех не-биом-тинт блоков).
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
		// Два оставшихся токена разобраны после закрытия шва block-flatten (BUG-080, CS.Flattened):
		//  · flowing_lava — в neo отдельного блока НЕТ, текучая лава это та же Blocks.LAVA с FluidState,
		//    то есть токен уже покрыт условием `aBlock == Blocks.LAVA` (дубль оригинала — у Грега LAVA стоит
		//    в списке дважды, ровно потому что там это были два разных блока);
		//  · lit_redstone_lamp — в neo это Blocks.REDSTONE_LAMP со свойством LIT, block-идентичности нет:
		//    различие живёт в BlockState, а сюда приходит Block. Вызывателей на лампу в дереве 0
		//    (греп BlockTextureCopied.get/new по FIRE|LAVA|GLOWSTONE|LAMP: только LAVA, GLOWSTONE, OBSIDIAN
		//    и портал Aether), поэтому расхождение ненаблюдаемо; при появлении вызывателя различие берётся
		//    из состояния позиции, а не из блок-идентичности. Долгом это не является — движковое расхождение.
		// Цвет: 4-й аргумент был aBlock.getRenderColor(aMeta). Канал восстановлен как контракт IBlock#getRenderColor
		// (общего Block-предка у иерархий GT6 нет) — спрашиваем ЕГО у GT6-блоков. У ванильных neo-блоков тинта нет
		// и не нужно: их цвет перенесён в САМИ блоки флэттенингом (white_wool/red_wool — свои текстуры), а дефолт
		// 1.7.10 Block.getRenderColor был 0xFFFFFF = UNCOLOURED, то есть для них поведение и так 1:1.
		this(aBlock, aSide, aMeta
			, aBlock instanceof gregapi.block.IBlock tGT6 ? tGT6.getRenderColor(aMeta) : UT.Code.getRGBInt(UNCOLOURED), F
			, aBlock == Blocks.FIRE || aBlock == Blocks.LAVA || aBlock == Blocks.GLOWSTONE
			, aBlock == Blocks.FIRE || aBlock == Blocks.LAVA || aBlock == Blocks.GLOWSTONE);
	}

	private Identifier getIcon(int aSide) {
		// F3 block-icon-data: было mBlock.getIcon(mSide==SIDE_ANY?aSide:mSide, mMeta) + catch→RENDERING_ERROR (1:1) —
		// Block.getIcon удалён из neo (baked-model рендер); спрайт грани резолвим из baked BlockStateModel ванильного
		// блока (централизованный GT6QuadBuilder.resolveBlockFaceIcon, §3). mMeta учтён (Flattening-варианты, см. резолвер).
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

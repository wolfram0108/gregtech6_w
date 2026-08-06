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
 * [Метка отложенности «block-render-color» СНЯТА 2026-08-06.] Прежняя её формулировка («для биом-тинта нужен
 * {@code BlockColors}») несла НЕВЕРНУЮ модель: 1.7.10 звал здесь {@code Block.getRenderColor(meta)} —
 * СТАТИЧЕСКИЙ цвет рендера БЕЗ мира и биома; биомный канал {@code colorMultiplier(world,x,y,z)} этот класс
 * не звал и в оригинале. Канал восстановлен методом {@link #vanillaRenderColor} — переопределения ванили
 * 1.7.10 перенесены как ДАННЫЕ (сверено по декомпилу {@code recompSrc}, тела в javadoc метода).
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
		// Цвет: 4-й аргумент был aBlock.getRenderColor(aMeta). Для GT6-блоков канал — контракт IBlock#getRenderColor
		// (общего Block-предка у иерархий GT6 нет); для ванильных — vanillaRenderColor ниже (переопределения
		// 1.7.10 как данные; дефолт 1.7.10 Block.getRenderColor = 0xFFFFFF = UNCOLOURED — для прочих 1:1 и так).
		this(aBlock, aSide, aMeta
			, aBlock instanceof gregapi.block.IBlock tGT6 ? tGT6.getRenderColor(aMeta) : vanillaRenderColor(aBlock, aMeta), F
			, aBlock == Blocks.FIRE || aBlock == Blocks.LAVA || aBlock == Blocks.GLOWSTONE
			, aBlock == Blocks.FIRE || aBlock == Blocks.LAVA || aBlock == Blocks.GLOWSTONE);
	}

	/** [снятие метки block-render-color 2026-08-06] Восстановленный канал 1.7.10 {@code Block.getRenderColor(meta)}
	 *  для ВАНИЛЬНЫХ блоков: статический цвет рендера, БЕЗ мира и биома (биомный {@code colorMultiplier} этот класс
	 *  не звал и в оригинале). Данные — ВСЕ переопределения ванили 1.7.10 (recompSrc, полный греп по
	 *  {@code net/minecraft/block}: Grass, Leaves, OldLeaf, LilyPad, Stem, TallGrass, Vine — 7 + дефолт):
	 *  · {@code BlockOldLeaf}: meta&3==1 (ель) → {@code getFoliageColorPine()} = 0x619961 — neo-константа
	 *    {@code FoliageColor.FOLIAGE_EVERGREEN} несёт ту же величину; ==2 (берёза) → 0x80A755 = {@code FOLIAGE_BIRCH};
	 *    прочее и {@code BlockLeaves}-база (дуб/джунгли/акация/тёмный дуб) → {@code getFoliageColorBasic()} =
	 *    colormap(0.5,1.0) — neo {@code FoliageColor.get(0.5,1.0)}, та же формула по тому же colormap;
	 *  · {@code BlockGrass}/{@code BlockTallGrass} (meta 1/2 → в neo SHORT_GRASS/FERN; meta 0 dead shrub →
	 *    в neo DEAD_BUSH, у него 1.7.10 давал белый) → {@code ColorizerGrass.getGrassColor(0.5,1.0)} —
	 *    neo {@code GrassColor.getDefaultColor()} = буквально {@code get(0.5,1.0)};
	 *  · {@code BlockVine} → foliage basic; · {@code BlockLilyPad} → константа 2129968;
	 *  · {@code BlockStem} → формула из меты 1:1 (attached-стебли — расщепление того же блока 1.7.10).
	 *  Листвы, которых в 1.7.10 нет (cherry/azalea/mangrove/pale_oak), канала не имели — дефолт, не выдумываем.
	 *  Модовые блоки: 1.7.10 диспатчил виртуально и чужие переопределения работали; в neo канала нет ни у кого —
	 *  восстановимы только ванильные данные, чужие получают дефолт (граница шва, честно). */
	private static int vanillaRenderColor(Block aBlock, int aMeta) {
		if (aBlock == Blocks.SPRUCE_LEAVES) return net.minecraft.world.level.FoliageColor.FOLIAGE_EVERGREEN & 0xFFFFFF;
		if (aBlock == Blocks.BIRCH_LEAVES)  return net.minecraft.world.level.FoliageColor.FOLIAGE_BIRCH & 0xFFFFFF;
		if (aBlock == Blocks.OAK_LEAVES || aBlock == Blocks.JUNGLE_LEAVES || aBlock == Blocks.ACACIA_LEAVES || aBlock == Blocks.DARK_OAK_LEAVES || aBlock == Blocks.VINE)
			return net.minecraft.world.level.FoliageColor.get(0.5, 1.0) & 0xFFFFFF;
		if (aBlock == Blocks.GRASS_BLOCK || aBlock == Blocks.SHORT_GRASS || aBlock == Blocks.FERN)
			return net.minecraft.world.level.GrassColor.getDefaultColor() & 0xFFFFFF;
		if (aBlock == Blocks.LILY_PAD) return 2129968;
		if (aBlock == Blocks.PUMPKIN_STEM || aBlock == Blocks.MELON_STEM || aBlock == Blocks.ATTACHED_PUMPKIN_STEM || aBlock == Blocks.ATTACHED_MELON_STEM)
			return (aMeta * 32) << 16 | (255 - aMeta * 8) << 8 | aMeta * 4;
		return UT.Code.getRGBInt(UNCOLOURED);
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

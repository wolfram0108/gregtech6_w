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

import gregapi.data.FL;
import gregapi.fluid.FluidGT;
import net.minecraft.world.level.block.Block;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.IFluidTank;

/**
 * @author Gregorius Techneticies
 *
 * В 1.7.10 отсюда читались {@code Fluid.getLuminosity/getBlock/getStillIcon/getColor} (Forge-API
 * кастомной жидкости, удалён в 26.1.2). Эти ДАННЫЕ уже живут в центре F5 ({@link FluidGT}) —
 * переиспользуем оттуда, НЕ дублируем и НЕ гатим (REMAP-RULES §A/§C4, философия: центр F5 —
 * единственный источник данных жидкости): свечение {@link FluidGT#getLuminosity()} (:153), цвет
 * {@link FluidGT#getRGBa()} (:155), ссылку-иконку {@link FluidGT#mTexture} (:98).
 * PORT-TODO(F3, baked-рендер клиента): САМ рендер жидкости (baked-геометрия по высоте потока) —
 * клиентская поздняя фаза (decisions/F3-render.md §2.1-2.2); здесь только держатель данных.
 */
public class BlockTextureFluid implements ITexture {
	private final boolean mAllowAlpha;
	private final int mLuminosity;
	private final Identifier mIcon;

	/**
	 *  DO NOT MANIPULATE THE VALUES INSIDE THIS ARRAY!!!
	 *
	 *  Just set this variable to another different Array instead.
	 *  Otherwise some coloured things will get Problems.
	 */
	public short[] mRGBa;

	public static BlockTextureFluid get(IFluidTank aTank, boolean aAllowAlpha) {
		return CODE_CLIENT?new BlockTextureFluid(aTank.getFluid(), aAllowAlpha):null;
	}
	public static BlockTextureFluid get(IFluidTank aTank) {
		return CODE_CLIENT?new BlockTextureFluid(aTank.getFluid()):null;
	}
	public static BlockTextureFluid get(FluidStack aFluidStack, boolean aAllowAlpha) {
		return CODE_CLIENT?new BlockTextureFluid(aFluidStack, aAllowAlpha):null;
	}
	public static BlockTextureFluid get(FluidStack aFluidStack) {
		return CODE_CLIENT?new BlockTextureFluid(aFluidStack):null;
	}
	public static BlockTextureFluid get(Fluid aFluid, boolean aAllowAlpha) {
		return CODE_CLIENT?new BlockTextureFluid(aFluid, aAllowAlpha):null;
	}
	public static BlockTextureFluid get(Fluid aFluid) {
		return CODE_CLIENT?new BlockTextureFluid(aFluid):null;
	}
	public static BlockTextureFluid get(FL aFluid) {
		return CODE_CLIENT?new BlockTextureFluid(aFluid.fluid()):null;
	}

	public BlockTextureFluid(FluidStack aFluid, boolean aAllowAlpha) {
		// Данные жидкости — из ЦЕНТРА F5 (FluidGT), не обнуляем/не дублируем. Оригинал (1.7.10):
		// mLuminosity = aFluid.getFluid().getLuminosity(aFluid) * 16; иконка/цвет — из блока жидкости
		// либо Fluid.getStillIcon()/getColor(). Всё это теперь хранит FluidGT (см. class javadoc).
		FluidGT tGT = (aFluid == null) ? null : FluidGT.of(aFluid.getFluid());
		if (tGT == null) {
			// PORT-TODO(F3, block-icon-data): жидкость не из центра F5 (нет FluidGT-переходника) — старый
			// Forge Fluid.getBlock()/getStillIcon()/getColor() (1.7.10) удалён, 1:1-ветки по блоку жидкости нет.
			mLuminosity = 0;
			mRGBa = UNCOLOURED;
			mIcon = null;
		} else {
			mLuminosity = tGT.getLuminosity() * 16; // *16 — как в оригинале (0..15 → шкала яркости 0..240).
			mRGBa = tGT.getRGBa();
			mIcon = (tGT.mTexture == null) ? null : tGT.mTexture.getIcon(0);
		}
		mAllowAlpha = aAllowAlpha;
	}

	public BlockTextureFluid(FluidStack aFluidStack) {
		this(aFluidStack, F);
	}

	public BlockTextureFluid(Fluid aFluid, boolean aAllowAlpha) {
		this(FL.make(aFluid, 0), aAllowAlpha);
	}

	public BlockTextureFluid(Fluid aFluid) {
		this(aFluid, F);
	}

	private Identifier getIcon(int aSide) {
		return mIcon;
	}

	@Override
	public void renderXPos(Object aRenderer, Block aBlock, int aX, int aY, int aZ, int aBrightness, boolean aChangedBlockBounds) {
		ITexture.Util.renderSide(SIDE_X_POS, getIcon(5), mRGBa, mAllowAlpha, mLuminosity > aBrightness, T, aRenderer, aBlock, aX, aY, aZ, Math.max(mLuminosity, aBrightness), aChangedBlockBounds);
	}

	@Override
	public void renderXNeg(Object aRenderer, Block aBlock, int aX, int aY, int aZ, int aBrightness, boolean aChangedBlockBounds) {
		ITexture.Util.renderSide(SIDE_X_NEG, getIcon(4), mRGBa, mAllowAlpha, mLuminosity > aBrightness, T, aRenderer, aBlock, aX, aY, aZ, Math.max(mLuminosity, aBrightness), aChangedBlockBounds);
	}

	@Override
	public void renderYPos(Object aRenderer, Block aBlock, int aX, int aY, int aZ, int aBrightness, boolean aChangedBlockBounds) {
		ITexture.Util.renderSide(SIDE_Y_POS, getIcon(1), mRGBa, mAllowAlpha, mLuminosity > aBrightness, T, aRenderer, aBlock, aX, aY, aZ, Math.max(mLuminosity, aBrightness), aChangedBlockBounds);
	}

	@Override
	public void renderYNeg(Object aRenderer, Block aBlock, int aX, int aY, int aZ, int aBrightness, boolean aChangedBlockBounds) {
		ITexture.Util.renderSide(SIDE_Y_NEG, getIcon(0), mRGBa, mAllowAlpha, mLuminosity > aBrightness, T, aRenderer, aBlock, aX, aY, aZ, Math.max(mLuminosity, aBrightness), aChangedBlockBounds);
	}

	@Override
	public void renderZPos(Object aRenderer, Block aBlock, int aX, int aY, int aZ, int aBrightness, boolean aChangedBlockBounds) {
		ITexture.Util.renderSide(SIDE_Z_POS, getIcon(3), mRGBa, mAllowAlpha, mLuminosity > aBrightness, T, aRenderer, aBlock, aX, aY, aZ, Math.max(mLuminosity, aBrightness), aChangedBlockBounds);
	}

	@Override
	public void renderZNeg(Object aRenderer, Block aBlock, int aX, int aY, int aZ, int aBrightness, boolean aChangedBlockBounds) {
		ITexture.Util.renderSide(SIDE_Z_NEG, getIcon(2), mRGBa, mAllowAlpha, mLuminosity > aBrightness, T, aRenderer, aBlock, aX, aY, aZ, Math.max(mLuminosity, aBrightness), aChangedBlockBounds);
	}

	@Override
	public boolean isValidTexture() {
		return mIcon != null;
	}
}

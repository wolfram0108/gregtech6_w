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

import gregapi.data.FL;
import gregapi.fluid.FluidGT;
import net.minecraft.world.level.block.Block;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidTank;

/**
 * @author Gregorius Techneticies
 *
 * В 1.7.10 отсюда читались {@code Fluid.getLuminosity/getBlock/getStillIcon/getColor} (Forge-API
 * кастомной жидкости, удалён в 26.1.2). Эти ДАННЫЕ уже живут в центре F5 ({@link FluidGT}) —
 * переиспользуем оттуда, НЕ дублируем и НЕ гатим (REMAP-RULES §A/§C4, философия: центр F5 —
 * единственный источник данных жидкости): свечение {@link FluidGT#getLuminosity()} (:153), цвет
 * {@link FluidGT#getRGBa()} (:155), ссылку-иконку {@link FluidGT#mTexture} (:98).
 * F3 superseded-render (GT6BlockModel/ItemModel пайплайн; старый getIcon/immediate-mode мёртв, 0 вызовов neo): САМ рендер жидкости (baked-геометрия по высоте потока) —
 * клиентская поздняя фаза (decisions/F3-render.md §2.1-2.2); здесь только держатель данных.
 */
public class BlockTextureFluid implements ITexture {
	private final boolean mAllowAlpha;
	private final int mLuminosity;
	private final ResourceLocation mIcon;

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
		ResourceLocation tIcon;
		if (aFluid == null) {
			mLuminosity = 0;
			mRGBa = UNCOLOURED;
			tIcon = null;
		} else if (tGT == null) {
			// BUG-049: 1:1-ветка «иконка самой жидкости» ВОССТАНОВЛЕНА через центр FL.stillIcon (ванильные
			// вода/лава были невидимы во всех ёмкостях — mIcon оставался null). Водный тинт — как у дисплеев
			// (ItemFluidDisplay.getColorFromItemStack: серый water_still без тинта бесцветен).
			mLuminosity = aFluid.getFluid().isSame(net.minecraft.world.level.material.Fluids.LAVA) ? 15 * 16 : 0;
			mRGBa = aFluid.getFluid().isSame(net.minecraft.world.level.material.Fluids.WATER) ? gregapi.util.UT.Code.getRGBaArray(0xFF3F76E4) : UNCOLOURED;
			tIcon = FL.stillIcon(aFluid.getFluid());
		} else {
			mLuminosity = tGT.getLuminosity() * 16; // *16 — как в оригинале (0..15 → шкала яркости 0..240).
			mRGBa = tGT.getRGBa();
			// BUG-049: GT6-жидкость БЕЗ своей текстуры — water_still из центра, красится mRGBa. С текстурой — она же.
			tIcon = FL.stillIcon(aFluid.getFluid());
		}
		// BUG-049: клиент-нормализация «текстура заявлена, но спрайта в атласе НЕТ» (seawater и др.: PNG
		// отсутствует и в ресурсах 1.7.10 — там рисовалась missing-шахматкой; резолвер GT6QuadBuilder такие
		// квады ПРОПУСКАЕТ → жидкость невидима) → water_still, красится mRGBa. Конструктор бежит только под
		// CODE_CLIENT (все get()-фабрики гейтованы) — client-класс GT6QuadBuilder сервером не линкуется.
		if (tIcon != null && GT6QuadBuilder.resolveSprite(tIcon) == null) tIcon = new ResourceLocation("minecraft", "block/water_still");
		mIcon = tIcon;
		mAllowAlpha = aAllowAlpha;
	}

	public BlockTextureFluid(FluidStack aFluidStack) {
		this(aFluidStack, F);
	}

	public BlockTextureFluid(Fluid aFluid, boolean aAllowAlpha) {
		// КРИТ (флюид-блоки прозрачные): amount=0 → neo FluidStack трактует как EMPTY → getFluid()=EMPTY →
		// FluidGT.of(EMPTY)=null → mIcon null → грань не рисовалась. amount текстуре не важен, даём ненулевой.
		this(FL.make(aFluid, 1000), aAllowAlpha);
	}

	public BlockTextureFluid(Fluid aFluid) {
		this(aFluid, F);
	}

	private ResourceLocation getIcon(int aSide) {
		return mIcon;
	}

	/** Иконка жидкости для fluid-меша ({@link RendererBlockFluid}): GT6-жидкость несёт ОДНУ текстуру
	 *  (FL.create → CustomIcon("fluids/имя")) — still==flowing, как Forge Fluid.setIcons(still) 1.7.10. */
	public ResourceLocation icon() {return mIcon;}

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

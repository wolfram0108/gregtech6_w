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

/** @author Gregorius Techneticies
 *  Forge's custom-fluid API (getLuminosity/getStillIcon/getColor) is gone; this data now lives in {@link FluidGT},
 *  reused from there instead of duplicated; the flowing-fluid mesh itself is built elsewhere, this is only a data holder. */
public class BlockTextureFluid implements ITexture {
	private final boolean mAllowAlpha;
	private final int mLuminosity;
	private final FluidStack mStack;

	/** Never cached: the client fluid extension is bound after the textures that are created at startup, so an
	 *  answer taken early would freeze to white for the rest of the session. */
	public short[] getRGBa() {
		return FL.tint(mStack);
	}

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
		// Sprite and tint come from the engine's fluid extension (FL centre); only luminosity, absent from it, is
		// taken from the GT6 fluid, with the lava value as the one vanilla exception.
		FluidGT tGT = (aFluid == null) ? null : FluidGT.of(aFluid.getFluid());
		mLuminosity = tGT != null ? tGT.getLuminosity() * 16 : (aFluid != null && aFluid.getFluid().isSame(net.minecraft.world.level.material.Fluids.LAVA) ? 15 * 16 : 0);
		mStack = aFluid;
		mAllowAlpha = aAllowAlpha;
	}

	public BlockTextureFluid(FluidStack aFluidStack) {
		this(aFluidStack, F);
	}

	public BlockTextureFluid(Fluid aFluid, boolean aAllowAlpha) {
		// amount=0 makes neo treat the FluidStack as EMPTY, so FluidGT.of() returns null and the face never draws; the
		// texture itself doesn't depend on amount, so a nonzero placeholder amount is used here.
		this(FL.make(aFluid, 1000), aAllowAlpha);
	}

	public BlockTextureFluid(Fluid aFluid) {
		this(aFluid, F);
	}

	/** Asked from the fluid extension on every build for the same reason as the tint: no early answer is kept. */
	private ResourceLocation getIcon(int aSide) {
		if (mStack == null) return null;
		ResourceLocation tIcon = FL.stillIcon(mStack.getFluid());
		return tIcon == null || GT6QuadBuilder.resolveSprite(tIcon) == null ? new ResourceLocation("minecraft", "block/water_still") : tIcon;
	}

	/** Fluid icon for the fluid mesh ({@link RendererBlockFluid}): a GT6 fluid has one texture, still==flowing,
	 *  matching Forge's 1.7.10 Fluid.setIcons(still). */
	public ResourceLocation icon() {return getIcon(0);}

	@Override
	public void renderXPos(Object aRenderer, Block aBlock, int aX, int aY, int aZ, int aBrightness, boolean aChangedBlockBounds) {
		ITexture.Util.renderSide(SIDE_X_POS, getIcon(5), getRGBa(), mAllowAlpha, mLuminosity > aBrightness, T, aRenderer, aBlock, aX, aY, aZ, Math.max(mLuminosity, aBrightness), aChangedBlockBounds);
	}

	@Override
	public void renderXNeg(Object aRenderer, Block aBlock, int aX, int aY, int aZ, int aBrightness, boolean aChangedBlockBounds) {
		ITexture.Util.renderSide(SIDE_X_NEG, getIcon(4), getRGBa(), mAllowAlpha, mLuminosity > aBrightness, T, aRenderer, aBlock, aX, aY, aZ, Math.max(mLuminosity, aBrightness), aChangedBlockBounds);
	}

	@Override
	public void renderYPos(Object aRenderer, Block aBlock, int aX, int aY, int aZ, int aBrightness, boolean aChangedBlockBounds) {
		ITexture.Util.renderSide(SIDE_Y_POS, getIcon(1), getRGBa(), mAllowAlpha, mLuminosity > aBrightness, T, aRenderer, aBlock, aX, aY, aZ, Math.max(mLuminosity, aBrightness), aChangedBlockBounds);
	}

	@Override
	public void renderYNeg(Object aRenderer, Block aBlock, int aX, int aY, int aZ, int aBrightness, boolean aChangedBlockBounds) {
		ITexture.Util.renderSide(SIDE_Y_NEG, getIcon(0), getRGBa(), mAllowAlpha, mLuminosity > aBrightness, T, aRenderer, aBlock, aX, aY, aZ, Math.max(mLuminosity, aBrightness), aChangedBlockBounds);
	}

	@Override
	public void renderZPos(Object aRenderer, Block aBlock, int aX, int aY, int aZ, int aBrightness, boolean aChangedBlockBounds) {
		ITexture.Util.renderSide(SIDE_Z_POS, getIcon(3), getRGBa(), mAllowAlpha, mLuminosity > aBrightness, T, aRenderer, aBlock, aX, aY, aZ, Math.max(mLuminosity, aBrightness), aChangedBlockBounds);
	}

	@Override
	public void renderZNeg(Object aRenderer, Block aBlock, int aX, int aY, int aZ, int aBrightness, boolean aChangedBlockBounds) {
		ITexture.Util.renderSide(SIDE_Z_NEG, getIcon(2), getRGBa(), mAllowAlpha, mLuminosity > aBrightness, T, aRenderer, aBlock, aX, aY, aZ, Math.max(mLuminosity, aBrightness), aChangedBlockBounds);
	}

	@Override
	public boolean isValidTexture() {
		return getIcon(0) != null;
	}
}

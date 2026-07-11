/**
 * Copyright (c) 2021 GregTech-6 Team
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

import net.minecraft.world.level.block.Block;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.BlockGetter;

/**
 * @author Gregorius Techneticies
 *
 * PORT-TODO(F3, baked-рендер клиента): в 1.7.10 диспетчер реализовывал
 * {@code ISimpleBlockRenderingHandler}+{@code IItemRenderer} и рисовал immediate-mode через
 * {@code RenderBlocks}/{@code Tesselator}/{@code GL11} — весь этот стек удалён в 26.1.2
 * (decisions/F3-render.md §1). Реальная замена диспетчера — {@code DynamicBlockStateModel}
 * (world-рендер, §2.1) + item-модель через {@code ItemStackRenderState}/{@code ItemModelResolver}
 * (инвентарь-рендер, §3 таблица "IItemRenderer"); регистрация — {@code RegisterBlockStateModels}/
 * {@code ModelEvent.RegisterStandalone}, НЕ {@code RenderingRegistry.registerBlockHandler}. Это
 * клиентская baked-фаза — здесь только серверная поверхность: поле {@code mRenderID}/{@code INSTANCE}
 * (используются в 11+ местах как "есть ли рендерер"/"id рендер-типа") и статические per-side
 * помощники, на которые опирается {@link IRenderedBlockObject.ErrorRenderer} — тело обоих гатится
 * до no-op.
 */
public class RendererBlockTextured {
	public final int mRenderID;
	public static RendererBlockTextured INSTANCE;
	public static CompoundTag mUsedNBT = null;

	public RendererBlockTextured(int aRenderID) {
		INSTANCE = this;
		mRenderID = aRenderID;
	}

	/** PORT-TODO(F3, baked-рендер клиента): было immediate-mode Tesselator/GL11 через RenderBlocks (см. class javadoc). */
	public static boolean renderNegativeYFacing(BlockGetter aWorld, Object aRenderer, Block aBlock, int aX, int aY, int aZ, ITexture aIcon, boolean aFullBlock, boolean aShouldSideBeRendered, Object aRenderedBlockObject) {
		if (aIcon == null || !aIcon.isValidTexture()) return F;
		if (aWorld != null && aFullBlock && !aShouldSideBeRendered) return F;
		aIcon.renderYNeg(aRenderer, aBlock, aX, aY, aZ, 240, !aFullBlock);
		return T;
	}

	/** PORT-TODO(F3, baked-рендер клиента): было immediate-mode Tesselator/GL11 через RenderBlocks (см. class javadoc). */
	public static boolean renderPositiveYFacing(BlockGetter aWorld, Object aRenderer, Block aBlock, int aX, int aY, int aZ, ITexture aIcon, boolean aFullBlock, boolean aShouldSideBeRendered, Object aRenderedBlockObject) {
		if (aIcon == null || !aIcon.isValidTexture()) return F;
		if (aWorld != null && aFullBlock && !aShouldSideBeRendered) return F;
		aIcon.renderYPos(aRenderer, aBlock, aX, aY, aZ, 240, !aFullBlock);
		return T;
	}

	/** PORT-TODO(F3, baked-рендер клиента): было immediate-mode Tesselator/GL11 через RenderBlocks (см. class javadoc). */
	public static boolean renderNegativeZFacing(BlockGetter aWorld, Object aRenderer, Block aBlock, int aX, int aY, int aZ, ITexture aIcon, boolean aFullBlock, boolean aShouldSideBeRendered, Object aRenderedBlockObject) {
		if (aIcon == null || !aIcon.isValidTexture()) return F;
		if (aWorld != null && aFullBlock && !aShouldSideBeRendered) return F;
		aIcon.renderZNeg(aRenderer, aBlock, aX, aY, aZ, 240, !aFullBlock);
		return T;
	}

	/** PORT-TODO(F3, baked-рендер клиента): было immediate-mode Tesselator/GL11 через RenderBlocks (см. class javadoc). */
	public static boolean renderPositiveZFacing(BlockGetter aWorld, Object aRenderer, Block aBlock, int aX, int aY, int aZ, ITexture aIcon, boolean aFullBlock, boolean aShouldSideBeRendered, Object aRenderedBlockObject) {
		if (aIcon == null || !aIcon.isValidTexture()) return F;
		if (aWorld != null && aFullBlock && !aShouldSideBeRendered) return F;
		aIcon.renderZPos(aRenderer, aBlock, aX, aY, aZ, 240, !aFullBlock);
		return T;
	}

	/** PORT-TODO(F3, baked-рендер клиента): было immediate-mode Tesselator/GL11 через RenderBlocks (см. class javadoc). */
	public static boolean renderNegativeXFacing(BlockGetter aWorld, Object aRenderer, Block aBlock, int aX, int aY, int aZ, ITexture aIcon, boolean aFullBlock, boolean aShouldSideBeRendered, Object aRenderedBlockObject) {
		if (aIcon == null || !aIcon.isValidTexture()) return F;
		if (aWorld != null && aFullBlock && !aShouldSideBeRendered) return F;
		aIcon.renderXNeg(aRenderer, aBlock, aX, aY, aZ, 240, !aFullBlock);
		return T;
	}

	/** PORT-TODO(F3, baked-рендер клиента): было immediate-mode Tesselator/GL11 через RenderBlocks (см. class javadoc). */
	public static boolean renderPositiveXFacing(BlockGetter aWorld, Object aRenderer, Block aBlock, int aX, int aY, int aZ, ITexture aIcon, boolean aFullBlock, boolean aShouldSideBeRendered, Object aRenderedBlockObject) {
		if (aIcon == null || !aIcon.isValidTexture()) return F;
		if (aWorld != null && aFullBlock && !aShouldSideBeRendered) return F;
		aIcon.renderXPos(aRenderer, aBlock, aX, aY, aZ, 240, !aFullBlock);
		return T;
	}
}

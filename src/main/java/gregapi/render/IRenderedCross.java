/**
 * Copyright (c) 2026 wolfram0108
 *
 * Written in 2026 for the GregTech 6 NeoForge port
 * (https://github.com/wolfram0108/gregtech6_w). Not part of the original GregTech 6
 * by Gregorius Techneticies; distributed under the same licence as the work it extends.
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

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;

/** Marks a block as a cross-model (plants/flowers: an X from two diagonal planes, not a cube); GT6BlockModel
 *  branches on it via {@link #getCrossIcon}, not the 6-face path; extends IRenderedBlock only for baking injection. */
public interface IRenderedCross extends IRenderedBlock {
	/** Cross-model icon for a position; when aWorld is null (item render), aX carries the stack's meta
	 *  instead, matching 1.7.10's renderBlockAsItem behavior. */
	ResourceLocation getCrossIcon(BlockGetter aWorld, int aX, int aY, int aZ);
	/** Cross-model tint (0..255 RGBa); null means white, i.e. no tint. */
	default short[] getCrossRGBa(BlockGetter aWorld, int aX, int aY, int aZ) {return null;}

	// IRenderedBlock's cubic defaults below are never called on the cross path; they only satisfy the interface.
	@Override default ITexture getTexture(int aRenderPass, byte aSide, ItemStack aStack) {return null;}
	@Override default ITexture getTexture(int aRenderPass, byte aSide, boolean[] aShouldSideBeRendered, BlockGetter aWorld, int aX, int aY, int aZ) {return null;}
	@Override default boolean usesRenderPass(int aRenderPass, ItemStack aStack) {return aRenderPass == 0;}
	@Override default boolean usesRenderPass(int aRenderPass, BlockGetter aWorld, int aX, int aY, int aZ, boolean[] aShouldSideBeRendered) {return aRenderPass == 0;}
	@Override default boolean setBlockBounds(int aRenderPass, ItemStack aStack) {return false;}
	@Override default boolean setBlockBounds(int aRenderPass, BlockGetter aWorld, int aX, int aY, int aZ, boolean[] aShouldSideBeRendered) {return false;}
	@Override default int getRenderPasses(ItemStack aStack) {return 1;}
	@Override default int getRenderPasses(BlockGetter aWorld, int aX, int aY, int aZ, boolean[] aShouldSideBeRendered) {return 1;}
	@Override default IRenderedBlockObject passRenderingToObject(ItemStack aStack) {return null;}
	@Override default IRenderedBlockObject passRenderingToObject(BlockGetter aWorld, int aX, int aY, int aZ) {return null;}
}

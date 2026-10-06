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

package gregapi.compat.buildcraft;

import buildcraft.api.core.render.ISprite;
import gregapi.render.GT6QuadBuilder;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;

/** A trigger icon looked up in the block atlas each time BuildCraft draws it (GuiIcon.draw: bind, then U/V), so it shows
 *  what the latest stitch holds; BuildCraft's own holders fill only at a stitch they were registered before. */
final class TriggerIconBC implements ISprite {
	private final ResourceLocation mIcon;
	
	TriggerIconBC(ResourceLocation aIcon) {mIcon = aIcon;}
	
	private TextureAtlasSprite sprite() {return GT6QuadBuilder.resolveSprite(mIcon, GT6QuadBuilder.ATLAS_ITEMS);}
	
	@Override public void bindTexture() {com.mojang.blaze3d.systems.RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_BLOCKS);}
	@Override public double getInterpU(double aU) {TextureAtlasSprite tSprite = sprite(); return tSprite == null ? aU : tSprite.getU0() + aU * (tSprite.getU1() - tSprite.getU0());}
	@Override public double getInterpV(double aV) {TextureAtlasSprite tSprite = sprite(); return tSprite == null ? aV : tSprite.getV0() + aV * (tSprite.getV1() - tSprite.getV0());}
}

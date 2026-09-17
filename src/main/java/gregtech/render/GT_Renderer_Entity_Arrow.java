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

package gregtech.render;

import static gregapi.data.CS.*;

import net.minecraft.client.renderer.entity.ArrowRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.resources.ResourceLocation;

/** The texture-by-instance contract is unchanged; with no intermediate render-state class here, the form is
 *  literally the 1.7.10 original again; real registration happens later through EntityRenderersEvent instead. */
public class GT_Renderer_Entity_Arrow extends ArrowRenderer<Arrow> {
	private final ResourceLocation mTexture;

	// Real EntityRendererProvider.Context: the renderer builds in EntityRenderersEvent.RegisterRenderers.
	// 1.7.10's RenderingRegistry handler is gone; registration is now keyed by EntityType.
	public GT_Renderer_Entity_Arrow(EntityRendererProvider.Context aContext, String aTextureName) {
		super(aContext);
		mTexture = new ResourceLocation(RES_PATH_ENTITY+aTextureName+".png");
	}

	@Override
	public ResourceLocation getTextureLocation(Arrow aArrow) {
		return mTexture;
	}
}

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

package gregtech.render;

import static gregapi.data.CS.*;

import net.minecraft.client.renderer.entity.ArrowRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.ArrowRenderState;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.resources.Identifier;

/**
 * F3 superseded-render (GT6BlockModel/ItemModel пайплайн; старый getIcon/immediate-mode мёртв, 0 вызовов neo): 1.7.10 {@code RenderArrow.getEntityTexture(Arrow)} (по инстансу
 * сущности) заменён в 26.1.2 на {@code ArrowRenderer<T,S>.getTextureLocation(S state)} (по render-state,
 * `neo-decompiled/net/minecraft/client/renderer/entity/ArrowRenderer.java:17-36`) — движко-шов, тот же
 * держатель {@code mTexture} возвращается независимо от параметра, семантика 1:1. Конструктор требует
 * {@code EntityRendererProvider.Context} (недоступен в этой legacy-точке регистрации через F10-зеркало
 * {@code RenderingRegistry}, decisions/F3-render.md §2.5/§6) — {@code null}, реальная регистрация
 * переезжает на {@code EntityRenderersEvent.RegisterRenderers}.
 */
public class GT_Renderer_Entity_Arrow extends ArrowRenderer<Arrow, ArrowRenderState> {
	private final Identifier mTexture;

	// F12-entity/F3-render (ЗАКРЫТО): реальный EntityRendererProvider.Context (не null) — рендерер строится в
	// EntityRenderersEvent.RegisterRenderers (GT_Client#registerClientRenderers), где Context доступен. 1.7.10
	// RenderingRegistry.registerEntityRenderingHandler(class, this) удалён — регистрация теперь по EntityType.
	public GT_Renderer_Entity_Arrow(EntityRendererProvider.Context aContext, String aTextureName) {
		super(aContext);
		mTexture = Identifier.parse(RES_PATH_ENTITY+aTextureName+".png");
	}

	@Override
	protected Identifier getTextureLocation(ArrowRenderState aState) {
		return mTexture;
	}

	@Override
	public ArrowRenderState createRenderState() {
		return new ArrowRenderState();
	}
}

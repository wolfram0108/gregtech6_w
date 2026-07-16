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

import java.util.List;

import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.model.pipeline.QuadBakingVertexConsumer;

/**
 * F3-render (client): единая item-модель ВСЕХ GT6-предметов (аналог блочной {@link GT6BlockModel} — та же централизация 1:1).
 * neo зовёт {@link #update} → берём per-meta иконку предмета (GT6 {@code getIconIndex(ItemStack)}/{@code getIconFromDamage}
 * возвращают {@link Identifier}, порт сохранил) → плоские front/back-quads из спрайта (стиль item/generated, упрощён до плоскости)
 * → в render-state. Регистрируется рантайм-инъекцией в {@code itemStackModels()} через {@code ModifyBakingResult} (без тысяч JSON,
 * процедурный мод). Икону резолвим рефлексией (общего интерфейса нет: MultiItem/PrefixItem/ItemBlock — россыпь), boot/render-safe.
 */
public class GT6ItemModel implements ItemModel {

	@Override
	public void update(ItemStackRenderState aOutput, ItemStack aItem, ItemModelResolver aResolver, ItemDisplayContext aCtx, net.minecraft.client.multiplayer.ClientLevel aLevel, net.minecraft.world.entity.ItemOwner aOwner, int aSeed) {
		aOutput.appendModelIdentityElement(this);
		try {
			Identifier tIcon = resolveIcon(aItem);
			if (tIcon == null) return;
			TextureAtlasSprite tSprite = GT6QuadBuilder.resolveSprite(tIcon);
			if (tSprite == null) return;
			ItemStackRenderState.LayerRenderState tLayer = aOutput.newLayer();
			List<BakedQuad> tQuads = tLayer.prepareQuadList();
			tQuads.add(flatFace(tSprite, true));   // front (+Z)
			tQuads.add(flatFace(tSprite, false));  // back  (-Z)
			tLayer.setParticleMaterial(new Material.Baked(tSprite, false));
		} catch (Throwable e) {/* render-safe: сбой одного предмета не рушит рендер */}
	}

	/** Икона предмета: GT6 {@code getIconIndex(ItemStack)} (PrefixItem/MultiItem) → Identifier; иначе {@code getIconFromDamage(int)}. */
	private static Identifier resolveIcon(ItemStack aItem) {
		Object tItem = aItem.getItem();
		Identifier r = tryIcon(tItem, "getIconIndex", ItemStack.class, aItem);
		if (r == null) r = tryIcon(tItem, "getIconFromDamage", int.class, aItem.getDamageValue());
		return r;
	}

	private static Identifier tryIcon(Object aTarget, String aMethod, Class<?> aArgType, Object aArg) {
		try {
			java.lang.reflect.Method m = aTarget.getClass().getMethod(aMethod, aArgType);
			Object o = m.invoke(aTarget, aArg);
			return o instanceof Identifier tId ? tId : null;
		} catch (Throwable ignored) {return null;}
	}

	/** Плоская грань предмета 16×16 (плоскость z=8/16) из спрайта, front (+Z) либо back (−Z). */
	private static BakedQuad flatFace(TextureAtlasSprite aSprite, boolean aFront) {
		Direction tDir = aFront ? Direction.SOUTH : Direction.NORTH;
		float z = 0.5f;
		float[][] c = aFront
			? new float[][]{{0,0,z, 0,16},{0,1,z, 0,0},{1,1,z, 16,0},{1,0,z, 16,16}}
			: new float[][]{{1,0,z, 16,16},{1,1,z, 16,0},{0,1,z, 0,0},{0,0,z, 0,16}};
		net.minecraft.world.phys.Vec3 n = tDir.getUnitVec3();
		QuadBakingVertexConsumer b = new QuadBakingVertexConsumer();
		b.setSprite(new Material.Baked(aSprite, false));
		b.setDirection(tDir);
		for (int i = 0; i < 4; i++) {
			b.addVertex(c[i][0], c[i][1], c[i][2]);
			b.setColor(255, 255, 255, 255);
			b.setNormal((float)n.x, (float)n.y, (float)n.z);
			b.setUv(aSprite.getU(c[i][3] / 16f), aSprite.getV(c[i][4] / 16f));
		}
		return b.bakeQuad();
	}
}

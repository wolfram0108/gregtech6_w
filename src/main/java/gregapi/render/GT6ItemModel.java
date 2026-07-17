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
			// item-иконки — в ITEMS-атласе (atlases/items.json, textures/items/**): материал-предметы берут item-версию
			// materialicons, а gt.multiitem.* (еда/книги/инструменты) иначе вообще не в атласе → пурпур. BLOCKS — fallback
			// для редких иконок, копирующих грань блока (IconContainerCopied/BlockTextureCopied).
			TextureAtlasSprite tSprite = GT6QuadBuilder.resolveSprite(tIcon, net.minecraft.data.AtlasIds.ITEMS);
			if (tSprite == null) tSprite = GT6QuadBuilder.resolveSprite(tIcon, net.minecraft.data.AtlasIds.BLOCKS);
			if (tSprite == null) return;
			ItemStackRenderState.LayerRenderState tLayer = aOutput.newLayer();
			List<BakedQuad> tQuads = tLayer.prepareQuadList();
			tQuads.add(flatFace(tSprite, true));   // front (+Z)
			tQuads.add(flatFace(tSprite, false));  // back  (-Z)
			tLayer.setParticleMaterial(new Material.Baked(tSprite, false));
		} catch (Throwable e) {/* render-safe: сбой одного предмета не рушит рендер */}
	}

	/** Икона предмета: GT6 {@code getIconIndex(ItemStack)} (PrefixItem/MultiItem) → Identifier; иначе {@code getIconFromDamage(int)}.
	 *  public — переиспользуется скан-оснасткой рендера (GT6RenderProbe) для приёмки «иконки не пурпур». */
	public static Identifier resolveIcon(ItemStack aItem) {
		Object tItem = aItem.getItem();
		Identifier r = tryIcon(tItem, "getIconIndex", ItemStack.class, aItem);
		if (r == null) r = tryIcon(tItem, "getIconFromDamage", int.class, aItem.getDamageValue());
		return r;
	}

	/**
	 * Приёмочный СКАН РЕНДЕРА (гейт ②, «текстуры не пурпур»): для всех GT6-предметов (не BlockItem) проверяет, что иконка
	 * резолвится в атласе (ITEMS, fallback BLOCKS) — не missing/пурпур. Пишет found/missing + примеры в gregtech.log.
	 * Зовётся один раз на первом client-tick (атлас стежен). Автоматизирует визуальный гейт, не заменяя, но давая механику.
	 */
	public static void probeItemIcons() {
		int tFound = 0, tMissing = 0, tNullIcon = 0, tTotal = 0;
		java.util.List<String> tNullSamples = new java.util.ArrayList<>(), tMissSamples = new java.util.ArrayList<>(), tFoundSamples = new java.util.ArrayList<>();
		java.util.Map<String,Integer> tNullByClass = new java.util.TreeMap<>();
		// КОРЕНЬ-ПРОВЕРКА (once): наполнен ли material.mTextureSetsItems + работает ли TextureSetIconItem.getIcon.
		try {
			gregapi.oredict.OreDictMaterial tFe = gregapi.data.MT.Fe;
			String tH = "Fe.mTextureSetsItems.size=" + tFe.mTextureSetsItems.size() + " INSTANCES_ITEM=" + gregapi.render.TextureSet.INSTANCES_ITEM.size();
			if (!tFe.mTextureSetsItems.isEmpty()) { Object ic0 = tFe.mTextureSetsItems.get(0); tH += " ic0=" + ic0.getClass().getSimpleName() + " getIcon0=" + ic0.getClass().getMethod("getIcon", int.class).invoke(ic0, 0); }
			gregapi.data.CS.OUT.println("[GT6-RENDER-PROBE] Fe-header: " + tH);
			// РЕАЛЬНЫЙ рендер-путь (валидная meta): dust iron → getIconFromDamageForRenderPass(Fe.mID) → resolveSprite → не пурпур?
			net.minecraft.world.item.Item tDust = net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.fromNamespaceAndPath("gregtech", "gt.meta.dust"));
			if (tDust instanceof gregapi.item.prefixitem.PrefixItem tDPI) {
				Object ic = tDPI.getClass().getMethod("getIconFromDamageForRenderPass", int.class, int.class).invoke(tDPI, (int)tFe.mID, 0);
				String tR = "dust-iron getIconFDR(Fe.mID=" + tFe.mID + ")=" + ic;
				if (ic instanceof Identifier idi) { TextureAtlasSprite sp = GT6QuadBuilder.resolveSprite(idi, net.minecraft.data.AtlasIds.ITEMS); if (sp == null) sp = GT6QuadBuilder.resolveSprite(idi, net.minecraft.data.AtlasIds.BLOCKS); tR += " → sprite=" + (sp != null ? "VALID(не-пурпур)" : "PURPLE!"); }
				gregapi.data.CS.OUT.println("[GT6-RENDER-PROBE] REAL-PATH: " + tR);
			}
		} catch (Throwable e) { gregapi.data.CS.OUT.println("[GT6-RENDER-PROBE] Fe-header EXC " + e); }
		for (net.minecraft.world.item.Item tItem : net.minecraft.core.registries.BuiltInRegistries.ITEM) {
			net.minecraft.resources.Identifier tKey = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(tItem);
			if (tKey == null || !(tKey.getNamespace().equals("gregtech") || tKey.getNamespace().equals("gregapi"))) continue;
			if (tItem instanceof net.minecraft.world.item.BlockItem) continue;
			tTotal++;
			Identifier tIcon = null; String tErr = "";
			try {
				// ВАЛИДНЫЙ вариант через getSubItems (meta=0 у PrefixItem/MultiItem = невалидный материал → ложный null);
				// берём первый creative-вариант (реальный dust=iron и т.п.), как его видит игрок.
				ItemStack tStack = null;
				try {
					java.util.List<ItemStack> tList = new java.util.ArrayList<>();
					java.lang.reflect.Method gsi = tItem.getClass().getMethod("getSubItems", net.minecraft.world.item.Item.class, net.minecraft.world.item.CreativeModeTab.class, java.util.List.class);
					gsi.invoke(tItem, tItem, null, tList);
					if (!tList.isEmpty()) tStack = tList.get(0);
				} catch (Throwable e) {/* нет getSubItems — single-variant */}
				if (tStack == null) tStack = new ItemStack(tItem);
				tIcon = resolveIcon(tStack);
			} catch (Throwable e) { tErr = "EXC:" + e.getClass().getSimpleName(); }
			String tCls = tItem.getClass().getSimpleName();
			// РЕШАЮЩАЯ проверка: retry с ЯВНО валидным материалом (iron) — если резолвится, дыры нет (probe meta=0 был артефакт).
			if (tIcon == null && tItem instanceof gregapi.item.prefixitem.PrefixItem) {
				try { tIcon = resolveIcon(gregapi.util.ST.make(tItem, 1, gregapi.data.MT.Fe.mID)); } catch (Throwable e) {}
			}
			if (tIcon == null) {
				tNullIcon++; tNullByClass.merge(tCls + tErr, 1, Integer::sum);
				if (tItem instanceof gregapi.item.prefixitem.PrefixItem tPI) {
					if (tNullSamples.size() < 6) try {
						gregapi.oredict.OreDictMaterial tFe = gregapi.data.MT.Fe;
						int tIdx = tPI.mPrefix.mIconIndexItem;
						String tD = "idx=" + tIdx + " FeTsiSz=" + tFe.mTextureSetsItems.size();
						if (tIdx >= 0 && tIdx < tFe.mTextureSetsItems.size()) {
							Object tIC = tFe.mTextureSetsItems.get(tIdx);
							Object ic = tIC.getClass().getMethod("getIcon", int.class).invoke(tIC, 0);
							tD += " IC=" + tIC.getClass().getSimpleName() + " getIcon0=" + ic;
						} else tD += " OOB!";
						tNullSamples.add("PFX:" + tKey.getPath() + "[" + tD + "]");
					} catch (Throwable e) { tNullSamples.add("PFX:" + tKey.getPath() + "[EXC " + e.getClass().getSimpleName() + ":" + e.getMessage() + "]"); }
				} else if (tNullSamples.size() < 8) tNullSamples.add(tKey.getPath() + "[" + tCls + "]" + tErr);
				continue;
			}
			TextureAtlasSprite tS = GT6QuadBuilder.resolveSprite(tIcon, net.minecraft.data.AtlasIds.ITEMS);
			if (tS == null) tS = GT6QuadBuilder.resolveSprite(tIcon, net.minecraft.data.AtlasIds.BLOCKS);
			if (tS != null) { tFound++; if (tFoundSamples.size() < 12) tFoundSamples.add(tKey.getPath() + "[" + tCls + "]→" + tIcon); }
			else { tMissing++; if (tMissSamples.size() < 12) tMissSamples.add(tKey.getPath() + "[" + tCls + "]→" + tIcon); }
		}
		gregapi.data.CS.OUT.println("[GT6-RENDER-PROBE] total=" + tTotal + " found=" + tFound + " missing(пурпур)=" + tMissing + " null-icon(не рисуется)=" + tNullIcon);
		gregapi.data.CS.OUT.println("[GT6-RENDER-PROBE] NULL-по-классам: " + tNullByClass);
		if (!tNullSamples.isEmpty()) gregapi.data.CS.OUT.println("[GT6-RENDER-PROBE] NULL-детали: " + tNullSamples);
		if (!tFoundSamples.isEmpty()) gregapi.data.CS.OUT.println("[GT6-RENDER-PROBE] FOUND примеры: " + tFoundSamples);
		if (!tMissSamples.isEmpty()) gregapi.data.CS.OUT.println("[GT6-RENDER-PROBE] MISSING-sprite примеры: " + tMissSamples);
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

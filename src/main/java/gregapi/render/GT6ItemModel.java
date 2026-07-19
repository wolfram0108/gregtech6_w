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
		// КРИТ: GUI кэширует ПИКСЕЛИ предмета по getModelIdentity() (GuiItemAtlas.getOrUpdate: identity совпал → слот READY →
		// перерисовки НЕТ). Канон Mojang (CuboidItemModelWrapper.update): identity обязан включать ВСЁ, от чего зависят пиксели —
		// каждый вычисленный тинт и foil добавляются appendModelIdentityElement. Поэтому: базовый ключ (item+GT6-meta) здесь, а
		// ПЕР-ПАССОВЫЕ спрайт+тинт+foil добавляют renderFlatItem/renderBlockInventory (у инструментов материал в NBT, не в meta —
		// без этого все инструменты одного типа делили ОДИН слот атласа и показывали первый отрисованный, Steel-серый без NBT).
		aOutput.appendModelIdentityElement(aItem.getItem());
		aOutput.appendModelIdentityElement((int) gregapi.util.ST.meta_(aItem));
		try {
			net.minecraft.world.item.Item tItem = aItem.getItem();
			// ЦЕНТР item-рендера, воспроизводит RendererBlockTextured.renderInventoryBlock (референс): предмет-БЛОК → 3D-геометрия блока
			// (canonical-TE/block-level, buildInventoryQuads); предмет-ПРЕДМЕТ (материал/MultiItem) → плоские иконки ПО РЕНДЕР-ПАССАМ с
			// per-pass тинтом (getColorFromItemStack) — как ванильный мульти-пасс item-icon (PrefixItem: 2 пасса, pass0 тинт материала).
			if (tItem instanceof net.minecraft.world.item.BlockItem tBI && tBI.getBlock() instanceof IRenderedBlock) {
				renderBlockInventory(aOutput, aItem, tBI.getBlock(), aCtx);
			} else {
				renderFlatItem(aOutput, aItem, tItem);
			}
		} catch (Throwable e) {/* render-safe: сбой одного предмета не рушит рендер */}
	}

	/** Предмет-форма БЛОКА: 3D-геометрия блока в инвентаре через {@link GT6BlockModel#buildInventoryQuads} (= renderInventoryBlock). */
	private static void renderBlockInventory(ItemStackRenderState aOutput, ItemStack aStack, net.minecraft.world.level.block.Block aBlock, net.minecraft.world.item.ItemDisplayContext aCtx) {
		GT6QuadBuilder tQB = new GT6QuadBuilder();
		try { GT6BlockModel.buildInventoryQuads(tQB, aBlock, aStack); } catch (Throwable e) {}
		List<BakedQuad> tBuilt = tQB.quads();
		if (tBuilt.isEmpty()) {
			// 1.7.10 renderItem TESR-классов (Chest/MassStorage): item-форму рисовал спец-рендер (renderTileEntityAt(this,0,0,0,0));
			// neo-носитель — special-model слой, тот же зарегистрированный рендерер, что и в мире (BER-диспетч по классу).
			net.minecraft.world.level.block.entity.BlockEntity tArg = MultiTileEntityBER.SPECIAL_ITEM_FORM.extractArgument(aStack);
			if (tArg != null) {
				aOutput.appendModelIdentityElement("mte-special:" + tArg.getClass().getName());
				ItemStackRenderState.LayerRenderState tSpLayer = aOutput.newLayer();
				net.minecraft.client.resources.model.cuboid.ItemTransforms tSpTr = blockGuiTransforms();
				if (tSpTr != null) tSpLayer.setItemTransform(tSpTr.getTransform(aCtx));
				tSpLayer.setupSpecialModel(MultiTileEntityBER.SPECIAL_ITEM_FORM, tArg);
				aOutput.setAnimated(); // спец-рендер per-frame (крышка/содержимое) — кэш GUI-атласа не для него
			}
			return;
		}
		// identity-вклад (канон, как в renderFlatItem): пиксели зависят от набора спрайтов квадов → в identity,
		// иначе стеки с одинаковыми item+meta, но разным видом (NBT/state) делят один кэш-слот GuiItemAtlas.
		java.util.TreeSet<String> tIdSpr = new java.util.TreeSet<>();
		boolean tAnimated = false;
		for (BakedQuad q : tBuilt) try {
			tIdSpr.add(q.materialInfo().sprite().contents().name().toString());
			if (q.materialInfo().sprite().contents().isAnimated()) tAnimated = true;
		} catch (Throwable e) {}
		for (String s : tIdSpr) aOutput.appendModelIdentityElement(s);
		// канон CuboidItemModelWrapper.update:100-102: анимированный спрайт грани → setAnimated (иначе GUI-атлас кэширует статику)
		if (tAnimated) aOutput.setAnimated();
		ItemStackRenderState.LayerRenderState tLayer = aOutput.newLayer();
		// КОРЕНЬ «блоки в инвентаре — плоская тёмная грань, не 3D-куб»: buildInventoryQuads даёт куб в 0..1, но без display-
		// трансформации neo рисует его фронтально (видна одна грань; ITEMS_3D-диффуз на неповёрнутой грани тёмный). В 1.7.10
		// изометрию блока-предмета применял движок (RenderBlocks.renderBlockAsItem), в neo — ItemTransforms модели. Берём
		// КАНОНИЧЕСКУЮ block-GUI трансформацию (изометрия 30/225, scale 0.625) ИЗ ДВИЖКА (block/block.json), не хардкодим.
		net.minecraft.client.resources.model.cuboid.ItemTransforms tTr = blockGuiTransforms();
		if (tTr != null) tLayer.setItemTransform(tTr.getTransform(aCtx));
		if (aStack.hasFoil()) {
			tLayer.setFoilType(ItemStackRenderState.FoilType.STANDARD);
			aOutput.setAnimated();
			aOutput.appendModelIdentityElement(ItemStackRenderState.FoilType.STANDARD);
		}
		tLayer.prepareQuadList().addAll(tBuilt);
		tLayer.setUsesBlockLight(true);
		try { tLayer.setParticleMaterial(new Material.Baked(tBuilt.get(0).materialInfo().sprite(), false)); } catch (Throwable e) {}
	}

	/** Предмет-ПРЕДМЕТ (материал/MultiItem): по РЕНДЕР-ПАССАМ getIcon(stack,pass) + тинт getColorFromItemStack(stack,pass). */
	private static void renderFlatItem(ItemStackRenderState aOutput, ItemStack aStack, net.minecraft.world.item.Item aItem) {
		int tPasses = itemRenderPasses(aItem, aStack);
		for (int tPass = 0; tPass < tPasses; tPass++) {
			Identifier tIcon = iconForPass(aItem, aStack, tPass);
			if (tIcon == null) { if (tPass == 0) return; else continue; }
			TextureAtlasSprite tSprite = GT6QuadBuilder.resolveSprite(tIcon, net.minecraft.data.AtlasIds.ITEMS);
			if (tSprite == null) tSprite = GT6QuadBuilder.resolveSprite(tIcon, net.minecraft.data.AtlasIds.BLOCKS);
			if (tSprite == null) continue;
			int tColor = itemColor(aItem, aStack, tPass);
			// identity-вклад пасса (канон CuboidItemModelWrapper.update:92): пиксели зависят от спрайта и тинта →
			// оба в identity, иначе GuiItemAtlas отдаёт чужой кэш-слот (инструменты: материал в NBT, meta одинаковая).
			aOutput.appendModelIdentityElement(tSprite.contents().name());
			aOutput.appendModelIdentityElement(tColor);
			// канон CuboidItemModelWrapper.update:100-102 (hasMaterialFlag(2)→setAnimated): анимированный спрайт (пчёлы,
			// жидкости) требует setAnimated — иначе GuiItemAtlas рисует слот один раз (READY) и анимация замирает статикой.
			if (tSprite.contents().isAnimated()) aOutput.setAnimated();
			ItemStackRenderState.LayerRenderState tLayer = aOutput.newLayer();
			tLayer.setUsesBlockLight(false); // эталон ItemModelGenerator=GuiLight.FRONT: плоский предмет в GUI full-bright; без этого слой block-shade'ится (SOUTH-грань ~0.8) → предмет «затемнён» и цвет искажён тенью
			if (aStack.hasFoil()) { // 1:1: GT6-1.7.10 рисует глинт по hasEffect (=isItemEnchanted) поверх пассов; канон neo — FoilType на слое + identity + animated (глинт скроллится)
				tLayer.setFoilType(ItemStackRenderState.FoilType.STANDARD);
				aOutput.setAnimated();
				aOutput.appendModelIdentityElement(ItemStackRenderState.FoilType.STANDARD);
			}
			List<BakedQuad> tQuads = tLayer.prepareQuadList();
			tQuads.add(flatFace(tSprite, true, tColor));
			tQuads.add(flatFace(tSprite, false, tColor));
			tLayer.setParticleMaterial(new Material.Baked(tSprite, false));
		}
	}

	// Каноническая block-GUI трансформация (изометрия) — кэш; читается ИЗ ДВИЖКА один раз (после bake атласа/моделей).
	private static net.minecraft.client.resources.model.cuboid.ItemTransforms sBlockGuiTransforms;
	private static boolean sBlockGuiTransformsTried = false;
	/** ItemTransforms ванильного {@code minecraft:block/block} (его {@code display.gui} — изометрия 30/225, scale 0.625),
	 *  взятые из движкового {@link net.minecraft.client.resources.model.ResolvedModel} — НЕ хардкод-константа (§«не выдумывать
	 *  константы»). 1.7.10 применял ту же изометрию в {@code RenderBlocks.renderBlockAsItem}; в neo носитель — ItemTransforms
	 *  модели. Публичного геттера нет → рефлексия приватного {@code ModelBakery.resolvedModels} (идиома проекта, как iconForPass). */
	private static net.minecraft.client.resources.model.cuboid.ItemTransforms blockGuiTransforms() {
		if (sBlockGuiTransformsTried) return sBlockGuiTransforms;
		sBlockGuiTransformsTried = true;
		try {
			net.minecraft.client.resources.model.ModelBakery tBakery = net.minecraft.client.Minecraft.getInstance().getModelManager().getModelBakery();
			java.lang.reflect.Field tF = net.minecraft.client.resources.model.ModelBakery.class.getDeclaredField("resolvedModels");
			tF.setAccessible(true);
			@SuppressWarnings("unchecked")
			java.util.Map<net.minecraft.resources.Identifier, net.minecraft.client.resources.model.ResolvedModel> tResolved =
				(java.util.Map<net.minecraft.resources.Identifier, net.minecraft.client.resources.model.ResolvedModel>) tF.get(tBakery);
			net.minecraft.client.resources.model.ResolvedModel tBlock = tResolved.get(net.minecraft.resources.Identifier.withDefaultNamespace("block/block"));
			if (tBlock != null) sBlockGuiTransforms = tBlock.getTopTransforms();
			gregapi.data.CS.OUT.println("[GT6-BLOCKITEM] block/block transforms=" + (sBlockGuiTransforms != null ? "OK gui=" + sBlockGuiTransforms.getTransform(net.minecraft.world.item.ItemDisplayContext.GUI) : "NULL(fallback NO_TRANSFORM)"));
		} catch (Throwable e) { gregapi.data.CS.OUT.println("[GT6-BLOCKITEM] transform-fetch упал: " + e); }
		return sBlockGuiTransforms;
	}

	/** Число рендер-пассов предмета (PrefixItem.getRenderPasses(int)=2). Нет метода → 1 пасс. */
	private static int itemRenderPasses(Object aItem, ItemStack aStack) {
		try { java.lang.reflect.Method m = aItem.getClass().getMethod("getRenderPasses", int.class); Object r = m.invoke(aItem, (int)gregapi.util.ST.meta_(aStack)); if (r instanceof Integer ri && ri > 0) return Math.min(ri, 8); } catch (Throwable e) {}
		return 1;
	}
	/** Иконка предмета на пасс: GT6 {@code getIcon(stack,pass)} (=getIconFromDamageForRenderPass); fallback pass0 getIconIndex/getIconFromDamage. */
	private static Identifier iconForPass(Object aItem, ItemStack aStack, int aPass) {
		try { java.lang.reflect.Method m = aItem.getClass().getMethod("getIcon", ItemStack.class, int.class); Object o = m.invoke(aItem, aStack, aPass); if (o instanceof Identifier id) return id; } catch (Throwable e) {}
		if (aPass == 0) { Identifier r = tryIcon(aItem, "getIconIndex", ItemStack.class, aStack); if (r == null) r = tryIcon(aItem, "getIconFromDamage", int.class, aStack.getDamageValue()); return r; }
		return null;
	}
	/** GT6 {@code getColorFromItemStack(stack,pass)} → 0xRRGGBB (pass0 = материал-тинт, иначе 0xFFFFFF белый). */
	private static int itemColor(Object aItem, ItemStack aStack, int aPass) {
		try { java.lang.reflect.Method m = aItem.getClass().getMethod("getColorFromItemStack", ItemStack.class, int.class); Object c = m.invoke(aItem, aStack, aPass); if (c instanceof Integer ci) return ci; } catch (Throwable e) {}
		return 0xFFFFFF;
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

	// ─────────────────────────────────────────────────────────────────────────────────────────────────────────────
	// ВИЗУАЛ-ПАРИТЕТ (Ф2, судья 1:1 с оригиналом). Заменяет недействительный probeItemIcons (140 предметов, метрика
	// «непурпур»). Снимает ПОЛНЫЙ render-дескриптор КАЖДОГО креатив-варианта тем же кодом, что рисует update()
	// (iconForPass/itemColor/resolveSprite) → реально резолвнутый спрайт+тинт per pass. НЕ судит сам («found») —
	// пишет ЧТО резолвится, чтобы компаратор сравнил с золотым дескриптором оригинала. Слепые зоны (throw) → "ERR", не "ok".
	/** Дамп порт-дескриптора ВСЕХ GT6 item-вариантов (реальный креатив-набор через getSubItems). Файл gt6dump/descriptor.port.item.jsonl. */
	public static void dumpItemDescriptors() {
		java.util.List<String> tLines = new java.util.ArrayList<>();
		for (net.minecraft.world.item.Item tItem : net.minecraft.core.registries.BuiltInRegistries.ITEM) {
			net.minecraft.resources.Identifier tKey = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(tItem);
			if (tKey == null || !(tKey.getNamespace().equals("gregtech") || tKey.getNamespace().equals("gregapi"))) continue;
			java.util.List<ItemStack> tVariants = new java.util.ArrayList<>();
			// MTE — прямой вызов: рефлексия по классу с вырезанными compat-интерфейсами кидает NoClassDefFoundError молча
			// (ловушка №3, см. CreativeTabsGT.enumerate) → машинные варианты не попадали в дамп вовсе.
			if (tItem instanceof gregapi.block.multitileentity.MultiTileEntityItemInternal tMTE) {
				try { tMTE.getSubItems(tItem, null, tVariants); } catch (Throwable e) {}
			} else try {
				java.lang.reflect.Method gsi = tItem.getClass().getMethod("getSubItems", net.minecraft.world.item.Item.class, net.minecraft.world.item.CreativeModeTab.class, java.util.List.class);
				gsi.invoke(tItem, tItem, null, tVariants);
			} catch (Throwable e) {/* нет getSubItems — одиночный вариант */}
			if (tVariants.isEmpty()) tVariants.add(new ItemStack(tItem));
			for (ItemStack tSt : tVariants) if (gregapi.util.ST.valid(tSt)) try { tLines.add(describeStack(tSt)); } catch (Throwable e) {}
		}
		java.util.Collections.sort(tLines);
		try {
			java.nio.file.Path tDir = java.nio.file.Path.of("gt6dump");
			java.nio.file.Files.createDirectories(tDir);
			java.nio.file.Files.write(tDir.resolve("descriptor.port.item.jsonl"), tLines, java.nio.charset.StandardCharsets.UTF_8);
			gregapi.data.CS.OUT.println("[GT6-VP] item-дескрипторов=" + tLines.size() + " → gt6dump/descriptor.port.item.jsonl");
		} catch (Throwable e) { gregapi.data.CS.OUT.println("[GT6-VP] запись дампа упала: " + e); }
	}

	/** Дамп render-дескриптора КОНКРЕТНЫХ стеков (для инжектора кандидата) в gt6dump/<aName> + в лог построчно. */
	public static void dumpStacks(java.util.List<ItemStack> aStacks, String aName) {
		java.util.List<String> tLines = new java.util.ArrayList<>();
		for (ItemStack tS : aStacks) if (gregapi.util.ST.valid(tS)) try { tLines.add(describeStack(tS)); } catch (Throwable e) {}
		try {
			java.nio.file.Path tDir = java.nio.file.Path.of("gt6dump");
			java.nio.file.Files.createDirectories(tDir);
			java.nio.file.Files.write(tDir.resolve(aName), tLines, java.nio.charset.StandardCharsets.UTF_8);
			gregapi.data.CS.OUT.println("[GT6-VP] дескрипторов кандидата=" + tLines.size() + " → gt6dump/" + aName);
			for (String tL : tLines) gregapi.data.CS.OUT.println("[GT6-VP-CAND] " + tL);
		} catch (Throwable e) { gregapi.data.CS.OUT.println("[GT6-VP] запись кандидата упала: " + e); }
	}

	/** Render-дескриптор одного стека: реально резолвнутый спрайт+тинт per pass (для flat) либо набор спрайтов (для block-item). */
	private static String describeStack(ItemStack aStack) {
		net.minecraft.resources.Identifier k = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(aStack.getItem());
		String tKey = (k == null ? "?" : k.toString()) + "#" + gregapi.util.ST.meta_(aStack);
		net.minecraft.world.item.Item tItem = aStack.getItem();
		StringBuilder sb = new StringBuilder(160).append("{\"k\":\"").append(jsonEsc(tKey)).append('"');
		try {
			if (tItem instanceof net.minecraft.world.item.BlockItem tBI && tBI.getBlock() instanceof IRenderedBlock) {
				sb.append(",\"path\":\"block\",\"spr\":[");
				GT6QuadBuilder tQB = new GT6QuadBuilder();
				try { GT6BlockModel.buildInventoryQuads(tQB, tBI.getBlock(), aStack); } catch (Throwable e) {}
				java.util.TreeSet<String> tSpr = new java.util.TreeSet<>();
				int tBlockTint = -1;
				for (BakedQuad q : tQB.quads()) try {
					tSpr.add(q.materialInfo().sprite().contents().name().toString());
					// тинт block-item = цвет первого НЕ-белого квада (colored-слой; golden пишет getRenderColor)
					if (tBlockTint == -1) { int c = q.bakedColors().color(0) & 0xFFFFFF; if (c != 0xFFFFFF) tBlockTint = c; }
				} catch (Throwable e) {}
				boolean tF = true; for (String s : tSpr) { if (!tF) sb.append(','); tF = false; sb.append('"').append(jsonEsc(s)).append('"'); }
				sb.append(']').append(",\"tint\":\"").append(String.format("%06x", (tBlockTint == -1 ? 0xFFFFFF : tBlockTint))).append('"');
			} else {
				sb.append(",\"path\":\"flat\",\"p\":[");
				int tPasses = itemRenderPasses(tItem, aStack);
				for (int p = 0; p < tPasses; p++) {
					Identifier tIcon = iconForPass(tItem, aStack, p);
					String tSprite;
					if (tIcon == null) tSprite = "null";
					else {
						TextureAtlasSprite s = GT6QuadBuilder.resolveSprite(tIcon, net.minecraft.data.AtlasIds.ITEMS);
						if (s == null) s = GT6QuadBuilder.resolveSprite(tIcon, net.minecraft.data.AtlasIds.BLOCKS);
						tSprite = (s == null) ? ("MISSING:" + tIcon) : s.contents().name().toString();
					}
					int tTint = itemColor(tItem, aStack, p);
					if (p > 0) sb.append(',');
					sb.append("{\"pass\":").append(p).append(",\"icon\":\"").append(jsonEsc(tIcon == null ? "null" : tIcon.toString()))
					  .append("\",\"sprite\":\"").append(jsonEsc(tSprite)).append("\",\"tint\":\"").append(String.format("%06x", tTint & 0xFFFFFF)).append("\"}");
				}
				sb.append(']');
			}
		} catch (Throwable e) { sb.append(",\"err\":\"").append(jsonEsc(e.getClass().getSimpleName())).append('"'); }
		return sb.append('}').toString();
	}

	// A/GAP-1 (система паритета 3D-объектов-В-МИРЕ): дескриптор рендера блока В МИРЕ (не item-форма). MTE-3D-объекты
	// (шкафы/трубы/провода/монетки) рисуются BER-путём buildRendererQuads на ЖИВОМ BE (getTexture по FACING/mActive/
	// mConnections — item-форма их НЕ покрывает, там mConnections=0/mActive=0). Тот же извлекатель спрайтов+тинта, что
	// describeStack (переиспользование). Ключ несёт TE-класс+meta+facing+connections — многомерность world-состояния.
	public static String describeWorldBlock(net.minecraft.world.level.BlockGetter aLevel, net.minecraft.core.BlockPos aPos) {
		net.minecraft.world.level.block.state.BlockState st = aLevel.getBlockState(aPos);
		net.minecraft.world.level.block.Block b = st.getBlock();
		net.minecraft.resources.Identifier k = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(b);
		net.minecraft.world.level.block.entity.BlockEntity be = aLevel.getBlockEntity(aPos);
		String teName = be == null ? "-" : be.getClass().getSimpleName();
		byte tFacing = -1, tConn = -1; boolean tActive = false;
		try { java.lang.reflect.Field fF = be == null ? null : findField(be.getClass(), "mFacing"); if (fF != null) tFacing = fF.getByte(be); } catch (Throwable e) {}
		try { java.lang.reflect.Field fC = be == null ? null : findField(be.getClass(), "mConnections"); if (fC != null) tConn = fC.getByte(be); } catch (Throwable e) {}
		try { java.lang.reflect.Field fA = be == null ? null : findField(be.getClass(), "mActive"); if (fA != null) tActive = fA.getBoolean(be); } catch (Throwable e) {}
		StringBuilder sb = new StringBuilder(200).append("{\"k\":\"").append(jsonEsc((k == null ? "?" : k.toString()) + "#" + teName))
			.append("\",\"path\":\"world\",\"facing\":").append(tFacing).append(",\"conn\":").append(tConn).append(",\"active\":").append(tActive).append(",\"spr\":[");
		// СИММЕТРИЯ с golden (DumpRenderBlocks): getTexture(block,pass,side,sides) НАПРЯМУЮ на BE + распаковка ITexture→Identifier
		// (не buildRendererQuads: он renderBlock-gate → пустые квады для машин/спец-BER; golden берёт getTexture напрямую).
		java.util.TreeSet<String> tSpr = new java.util.TreeSet<>();
		String tTintS = null;
		try {
			if (be instanceof gregapi.render.IRenderedBlockObject ro) {
				gregapi.render.IRenderedBlockObject ro2 = ro.passRenderingToObject(aLevel, aPos.getX(), aPos.getY(), aPos.getZ());
				if (ro2 != null) ro = ro2;
				boolean[] tAll = {true,true,true,true,true,true};
				int tPasses = Math.min(8, Math.max(1, ro.getRenderPasses(b, tAll)));
				for (int pass = 0; pass < tPasses; pass++) {
					if (!ro.usesRenderPass(pass, tAll)) continue;
					for (byte side = 0; side < 6; side++) try { tTintS = collectIconsWorld(ro.getTexture(b, pass, side, tAll), tSpr, tTintS); } catch (Throwable e) {}
				}
			}
		} catch (Throwable e) {}
		boolean tF = true; for (String s : tSpr) { if (!tF) sb.append(','); tF = false; sb.append('"').append(jsonEsc(s)).append('"'); }
		sb.append(']').append(",\"tint\":\"").append(tTintS == null ? "ffffff" : tTintS).append("\"}");
		return sb.toString();
	}

	/** Распаковка порт-ITexture → имена спрайтов (Identifier.toString, симметрично golden IIcon.getIconName) + первый не-белый тинт. */
	private static String collectIconsWorld(gregapi.render.ITexture tx, java.util.Set<String> out, String tint) {
		if (tx == null) return tint;
		try {
			if (tx instanceof gregapi.render.BlockTextureMulti tM) {
				java.lang.reflect.Field f = gregapi.render.BlockTextureMulti.class.getDeclaredField("mTextures"); f.setAccessible(true);
				Object[] arr = (Object[]) f.get(tM); if (arr != null) for (Object sub : arr) tint = collectIconsWorld((gregapi.render.ITexture) sub, out, tint);
				return tint;
			}
			if (tx instanceof gregapi.render.BlockTextureDefault tD) {
				java.lang.reflect.Field f = gregapi.render.BlockTextureDefault.class.getDeclaredField("mIconContainer"); f.setAccessible(true);
				Object cont = f.get(tD);
				if (cont instanceof gregapi.render.IIconContainer tIC) { net.minecraft.resources.Identifier ic = tIC.getIcon(0); if (ic != null) out.add(ic.toString()); }
				java.lang.reflect.Field fc = gregapi.render.BlockTextureDefault.class.getDeclaredField("fRGBa"); fc.setAccessible(true);
				short[] rgba = (short[]) fc.get(tD);
				if (tint == null && rgba != null && rgba.length >= 3 && !(rgba[0] == 255 && rgba[1] == 255 && rgba[2] == 255)) tint = String.format("%02x%02x%02x", rgba[0] & 0xFF, rgba[1] & 0xFF, rgba[2] & 0xFF);
				return tint;
			}
			if (tx instanceof gregapi.render.BlockTextureSided tS) {
				java.lang.reflect.Field f = gregapi.render.BlockTextureSided.class.getDeclaredField("mIconContainers"); f.setAccessible(true);
				Object[] arr = (Object[]) f.get(tS); if (arr != null) for (Object cont : arr) if (cont instanceof gregapi.render.IIconContainer tIC) { net.minecraft.resources.Identifier ic = tIC.getIcon(0); if (ic != null) out.add(ic.toString()); }
				return tint;
			}
			// Rock/Stick/MiniPortal используют BlockTextureCopied (copy текстуры ванильного блока) — симметрично golden collectIcons.
			if (tx instanceof gregapi.render.BlockTextureCopied tCp) {
				java.lang.reflect.Method m = gregapi.render.BlockTextureCopied.class.getDeclaredMethod("getIcon", int.class); m.setAccessible(true);
				for (int side = 0; side < 6; side++) { Object ic = m.invoke(tCp, side); if (ic instanceof net.minecraft.resources.Identifier id) out.add(id.toString()); }
				try { java.lang.reflect.Field fr = gregapi.render.BlockTextureCopied.class.getDeclaredField("mRGBa"); fr.setAccessible(true); short[] rgba = (short[]) fr.get(tCp);
					if (tint == null && rgba != null && rgba.length >= 3 && !(rgba[0] == 255 && rgba[1] == 255 && rgba[2] == 255)) tint = String.format("%02x%02x%02x", rgba[0] & 0xFF, rgba[1] & 0xFF, rgba[2] & 0xFF); } catch (Throwable e) {}
				return tint;
			}
		} catch (Throwable e) {}
		return tint;
	}

	// A/GAP-3 симметрия эталона: golden = canonical-TE (оракул без мира). Порт был живой-BE → 50 расхождений (facing/level-
	// дефолт). Тот же getTexture, но на canonical-TE (из реестра, level=null) — точная симметрия с golden DumpRenderBlocks.
	public static String describeWorldBlockCanonical(net.minecraft.world.level.block.entity.BlockEntity aBE, net.minecraft.world.level.block.Block aBlock) {
		net.minecraft.resources.Identifier k = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(aBlock);
		String teName = aBE == null ? "-" : aBE.getClass().getSimpleName();
		StringBuilder sb = new StringBuilder(200).append("{\"k\":\"").append(jsonEsc((k == null ? "?" : k.toString()) + "#" + teName)).append("\",\"path\":\"world\",\"spr\":[");
		java.util.TreeSet<String> tSpr = new java.util.TreeSet<>();
		String tTintS = null;
		try {
			if (aBE instanceof gregapi.render.IRenderedBlockObject ro) {
				boolean[] tAll = {true,true,true,true,true,true};
				int tPasses = Math.min(8, Math.max(1, ro.getRenderPasses(aBlock, tAll)));
				for (int pass = 0; pass < tPasses; pass++) {
					if (!ro.usesRenderPass(pass, tAll)) continue;
					for (byte side = 0; side < 6; side++) try { tTintS = collectIconsWorld(ro.getTexture(aBlock, pass, side, tAll), tSpr, tTintS); } catch (Throwable e) {}
				}
			}
		} catch (Throwable e) {}
		boolean tF = true; for (String s : tSpr) { if (!tF) sb.append(','); tF = false; sb.append('"').append(jsonEsc(s)).append('"'); }
		sb.append(']').append(",\"tint\":\"").append(tTintS == null ? "ffffff" : tTintS).append("\"}");
		return sb.toString();
	}

	// A/GAP-4 (варьирование соединений труб/проводов): connector-TE рендерит по mConnections (getTexture2 по соединениям
	// соседей). Дескриптор мерит дефолт mConnections=0 (изолированная труба); здесь ставим mConnections=aConn (рефлексия)
	// перед getTexture → спрайт по соединению. Ключ несёт @conn (симметрично порт↔golden). Восстановление 0 после — не нужно
	// (canonical-TE одноразовый для дампа).
	public static String describeWorldBlockConn(net.minecraft.world.level.block.entity.BlockEntity aBE, net.minecraft.world.level.block.Block aBlock, byte aConn) {
		try { java.lang.reflect.Field f = aBE == null ? null : findField(aBE.getClass(), "mConnections"); if (f != null) f.setByte(aBE, aConn); } catch (Throwable e) {}
		String tBase = describeWorldBlockCanonical(aBE, aBlock);
		return tBase.replaceFirst("(\\{\"k\":\"[^\"]*)\"", "$1@conn" + aConn + "\"");
	}

	private static java.lang.reflect.Field findField(Class<?> aClass, String aName) {
		for (Class<?> c = aClass; c != null && c != Object.class; c = c.getSuperclass()) try { java.lang.reflect.Field f = c.getDeclaredField(aName); f.setAccessible(true); return f; } catch (Throwable e) {}
		return null;
	}

	private static String jsonEsc(String s) { return s == null ? "" : s.replace("\\", "\\\\").replace("\"", "\\\""); }

	private static Identifier tryIcon(Object aTarget, String aMethod, Class<?> aArgType, Object aArg) {
		try {
			java.lang.reflect.Method m = aTarget.getClass().getMethod(aMethod, aArgType);
			Object o = m.invoke(aTarget, aArg);
			return o instanceof Identifier tId ? tId : null;
		} catch (Throwable ignored) {return null;}
	}

	/** Плоская грань предмета 16×16 (плоскость z=8/16) из спрайта, front (+Z) либо back (−Z), с тинтом aColor (0xRRGGBB). */
	private static BakedQuad flatFace(TextureAtlasSprite aSprite, boolean aFront, int aColor) {
		int r=(aColor>>16)&0xFF, g=(aColor>>8)&0xFF, b8=aColor&0xFF;
		Direction tDir = aFront ? Direction.SOUTH : Direction.NORTH;
		float z = aFront ? 8.5f/16f : 7.5f/16f; // разнести front/back на 1px (как ItemModelGenerator): обе на z=0.5 → z-fight, тёмная задняя грань проступает
		float[][] c = aFront
			? new float[][]{{0,0,z, 0,16},{0,1,z, 0,0},{1,1,z, 16,0},{1,0,z, 16,16}}
			: new float[][]{{1,0,z, 16,16},{1,1,z, 16,0},{0,1,z, 0,0},{0,0,z, 0,16}};
		net.minecraft.world.phys.Vec3 n = tDir.getUnitVec3();
		QuadBakingVertexConsumer b = new QuadBakingVertexConsumer();
		b.setSprite(new Material.Baked(aSprite, false));
		b.setDirection(tDir);
		b.setLightEmission(15); // full-bright: putBakedQuad берёт light=getLightCoordsWithEmission(lightEmission); GUI даёт тёмный lightCoords (даже без-тинтовая полоса тёмная) → форсим эмиссию 15 (как эталон плоского item-icon)
		for (int i = 3; i >= 0; i--) { // КОРЕНЬ затемнения: winding вершин был инвертирован vs канон Mojang (FaceInfo) → GPU backface-cull скрывал SOUTH-грань (яркая нормаль) и показывал NORTH-грань (тёмная под ITEMS_FLAT-диффузом). Реверс порядка (i=3→0) чинит winding — тот же приём, что уже в GT6QuadBuilder.boundedFace:130
			b.addVertex(c[i][0], c[i][1], c[i][2]);
			b.setColor(r, g, b8, 255); // тинт материала (белая проба подтвердила: цвет-механизм работает; корень — свет)
			b.setNormal((float)n.x, (float)n.y, (float)n.z);
			b.setUv(aSprite.getU(c[i][3] / 16f), aSprite.getV(c[i][4] / 16f));
		}
		return b.bakeQuad();
	}
}

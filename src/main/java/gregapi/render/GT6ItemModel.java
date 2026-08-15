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

import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.pipeline.QuadBakingVertexConsumer;

/**
 * F3-render (client): единая item-модель ВСЕХ GT6-предметов (аналог блочной {@link GT6BlockModel} — та же централизация 1:1).
 * Берём per-meta иконку предмета (GT6 {@code getIconIndex(ItemStack)}/{@code getIcon(stack,pass)} возвращают
 * {@link ResourceLocation}, порт сохранил) → плоские front/back-quads из спрайта (стиль item/generated) либо 3D-форму
 * блока ({@link GT6BlockModel#buildInventoryQuads} = {@code renderInventoryBlock}). Регистрируется рантайм-инъекцией
 * в {@code ModelEvent.ModifyBakingResult} (без тысяч JSON — мод процедурный). Икону резолвим рефлексией (общего
 * интерфейса нет: MultiItem/PrefixItem/ItemBlock — россыпь), boot/render-safe.
 *
 * <p><b>Ветка 1.20.1.</b> Здесь item-модель — {@link BakedModel} + {@link ItemOverrides}: движок сначала спрашивает
 * {@code getOverrides().resolve(model, stack, level, entity, seed)} ({@code ItemRenderer.getModel}), и ЭТА точка —
 * носитель «модель зависит от стека», которым в 26.x был {@code ItemModel.update(ItemStackRenderState, stack, ...)}.
 * Разрешённая модель отдаёт готовые квады, {@link ItemTransforms} и признаки gui3d/blockLight. Пиксельного кэша GUI
 * (26.x {@code GuiItemAtlas}) в 1.20.1 нет — вместе с ним исчезли {@code appendModelIdentityElement}/{@code setAnimated}:
 * это не потеря поведения, а исчезнувший в 1.20.1 класс дефекта. Глинт и свет ставит сам {@code ItemRenderer}.
 */
public class GT6ItemModel implements BakedModel {

	// ================================================================================================================
	// Правка №3 (BUG-106): КЭШ ГЕОМЕТРИИ ПРЕДМЕТОВ. Замер JFR (52 мин живой игры): ~40% ВСЕХ аллокаций клиента —
	// пересборка одних и тех же квадов каждый кадр (sideQuad 12.6%, boundedFace-семья ~27%). Плоская геометрия —
	// ЧИСТАЯ функция (спрайт, тинт, ободок): кэшируем глобально; BakedQuad иммутабелен — безопасно разделяется
	// между кадрами и слоями (движок сам так делает с ванильными baked-моделями). 3D-форма блока-предмета —
	// функция (блок, мета, NBT стека). Сброс — при перепечке моделей (onModifyBakingResult → invalidateCaches:
	// атлас пересоздан, старые спрайты мертвы). Отступление от 1.7.10 (там immediate-mode каждый кадр) одобрено
	// пользователем 2026-08-09: «оптимизация важнее 1:1, централизация обязательна».
	// ================================================================================================================
	private record FlatKey(TextureAtlasSprite mSprite, int mColor, boolean mSides) {}
	private record InvKey(net.minecraft.world.level.block.Block mBlock, short mMeta, CompoundTag mTag) {}
	/** Ключ разрешённой per-стек модели: тот же состав, что у 26.x-identity (предмет + GT6-мета + NBT). */
	private record ModelKey(net.minecraft.world.item.Item mItem, short mMeta, CompoundTag mTag) {}
	private static final java.util.concurrent.ConcurrentHashMap<FlatKey, List<BakedQuad>> sFlatCache = new java.util.concurrent.ConcurrentHashMap<>();
	private static final java.util.concurrent.ConcurrentHashMap<InvKey, List<BakedQuad>> sInvCache = new java.util.concurrent.ConcurrentHashMap<>();
	private static final java.util.concurrent.ConcurrentHashMap<ModelKey, Baked> sModelCache = new java.util.concurrent.ConcurrentHashMap<>();

	/** Сброс кэшей геометрии — зовётся из onModifyBakingResult при каждой перепечке моделей/атласов. */
	public static void invalidateCaches() {sFlatCache.clear(); sInvCache.clear(); sModelCache.clear(); sSideFaceCache.clear(); sVanillaTransforms.clear(); sVanillaTransformsTried.clear(); sBarOverlayIcons = null;}

	/** Правка №3: кэш рефлексивных Method — прежде getClass().getMethod(...) звался на КАЖДЫЙ пасс КАЖДОГО
	 *  видимого предмета каждый кадр (аллокации в reflection-машинерии видны в JFR). null-значение — «метода нет». */
	private static final java.util.concurrent.ConcurrentHashMap<String, java.lang.reflect.Method> sMethodCache = new java.util.concurrent.ConcurrentHashMap<>();
	private static final java.lang.reflect.Method NO_METHOD;
	static {java.lang.reflect.Method m = null; try {m = Object.class.getMethod("hashCode");} catch (Throwable e) {} NO_METHOD = m;}
	private static java.lang.reflect.Method cachedMethod(Class<?> aClass, String aName, Class<?>... aArgs) {
		String tKey = aClass.getName() + '#' + aName + '#' + aArgs.length + (aArgs.length > 0 ? aArgs[0].getSimpleName() : "");
		java.lang.reflect.Method rMethod = sMethodCache.computeIfAbsent(tKey, k -> {
			try {return aClass.getMethod(aName, aArgs);} catch (Throwable e) {return NO_METHOD;}
		});
		return rMethod == NO_METHOD ? null : rMethod;
	}

	/** Плоская геометрия предмета (front+back+опц. ободок) из кэша; сборка — только на промах. */
	private static List<BakedQuad> flatQuads(TextureAtlasSprite aSprite, int aColor, boolean aSides) {
		if (sFlatCache.size() > 16384) sFlatCache.clear(); // предохранитель размера (JEI листает тысячи предметов)
		return sFlatCache.computeIfAbsent(new FlatKey(aSprite, aColor, aSides), aKey -> {
			java.util.ArrayList<BakedQuad> rQuads = new java.util.ArrayList<>(aSides ? 10 : 2);
			rQuads.add(flatFace(aSprite, true, aColor));
			rQuads.add(flatFace(aSprite, false, aColor));
			if (aSides) addSideQuads(rQuads, aSprite, aColor);
			return java.util.List.copyOf(rQuads);
		});
	}

	// ==================== корневая модель: точка «модель зависит от стека» ====================

	private final ItemOverrides mOverrides = new ItemOverrides() {
		@Override
		public BakedModel resolve(BakedModel aModel, ItemStack aStack, ClientLevel aLevel, LivingEntity aEntity, int aSeed) {
			try {return modelFor(aStack);} catch (Throwable e) {return aModel;} // render-safe: сбой одного предмета не рушит рендер
		}
	};

	@Override public ItemOverrides getOverrides() {return mOverrides;}
	@Override public List<BakedQuad> getQuads(BlockState aState, Direction aSide, RandomSource aRandom) {return java.util.List.of();}
	@Override public boolean useAmbientOcclusion() {return false;}
	@Override public boolean isGui3d() {return false;}
	@Override public boolean usesBlockLight() {return false;}
	@Override public boolean isCustomRenderer() {return false;}
	/** BP-BUG-007, ПРЕДМЕТНОЕ ПЛЕЧО канала слоя. Спрашивают именно ЭТУ модель: {@code RenderTypeHelper
	 *  .getFallbackItemRenderType:63-68} для {@code BlockItem} зовёт {@code model.getRenderTypes(block
	 *  .defaultBlockState(), …)} у item-модели, а не у блочной. Без этого ответа дефолт
	 *  {@code IForgeBakedModel:85-88} уходил в {@code ItemBlockRenderTypes.getRenderLayers}, где блоков GT6 нет,
	 *  и стекло в GUI рисовалось CUTOUT — непрозрачным. Формула — общий центр, своей копии не заводим. */
	@Override public net.minecraftforge.client.ChunkRenderTypeSet getRenderTypes(BlockState aState, RandomSource aRandom, net.minecraftforge.client.model.data.ModelData aData) {
		return GT6BlockModel.renderTypesOf(aState, null);
	}
	@Override public TextureAtlasSprite getParticleIcon() {return GT6QuadBuilder.resolveSprite(gregapi.old.Textures.BlockIcons.CFOAM_HARDENED.getIcon(0));}
	@Override public ItemTransforms getTransforms() {return ItemTransforms.NO_TRANSFORMS;}

	/** ЦЕНТР item-рендера, воспроизводит {@code RendererBlockTextured.renderInventoryBlock} (референс): предмет-БЛОК →
	 *  3D-геометрия блока (canonical-TE/block-level, buildInventoryQuads); предмет-ПРЕДМЕТ (материал/MultiItem) →
	 *  плоские иконки ПО РЕНДЕР-ПАССАМ с per-pass тинтом (getColorFromItemStack) — как ванильный мульти-пасс item-icon
	 *  (PrefixItem: 2 пасса, pass0 тинт материала). */
	private static Baked modelFor(ItemStack aStack) {
		if (sModelCache.size() > 16384) sModelCache.clear();
		CompoundTag tTag = aStack.getTag();
		return sModelCache.computeIfAbsent(new ModelKey(aStack.getItem(), gregapi.util.ST.meta_(aStack), tTag == null ? null : tTag.copy()), aKey -> build(aStack));
	}

	private static Baked build(ItemStack aStack) {
		net.minecraft.world.item.Item tItem = aStack.getItem();
		if (tItem instanceof net.minecraft.world.item.BlockItem tRailBI && tRailBI.getBlock() instanceof gregapi.block.misc.BlockBaseRail tRail) {
			// Рельс — block-item без IRenderedBlock: flat-путь его иконку не резолвит (getIconIndex у ItemBlockBase нет).
			// Рисуем плоскую straight-иконку рельса (мета 0) напрямую, как ванильный item рельса.
			return buildRailItem(tRail);
		}
		if (tItem instanceof net.minecraft.world.item.BlockItem tBI && tBI.getBlock() instanceof IRenderedBlock) {
			return buildBlockInventory(aStack, tBI.getBlock());
		}
		return buildFlatItem(aStack, tItem);
	}

	// ==================== разрешённая per-стек модель ====================

	/** Готовая модель конкретного стека: квады + позы + признаки. {@code mWorldVariant} — та же модель без
	 *  GUI-only оверлеев (см. BUG-028), выбирается в {@link Baked#applyTransform} по контексту отрисовки. */
	private static final class Baked implements BakedModel {
		final List<BakedQuad> mQuads;
		final ItemTransforms mTransforms;
		final boolean mGui3d, mBlockLight;
		final TextureAtlasSprite mParticle;
		Baked mWorldVariant = this;
		/** 1.7.10 renderItem TESR-классов (Chest/MassStorage): item-форму рисовал спец-рендер. Носитель 1.20.1 —
		 *  {@code isCustomRenderer()==true} → движок уходит в {@code IClientItemExtensions.getCustomRenderer()}
		 *  ({@code ItemRenderer.render:...}), то есть в BEWLR {@link MultiTileEntityBER#SPECIAL_ITEM_FORM}. */
		boolean mCustomRenderer = false;

		Baked(List<BakedQuad> aQuads, ItemTransforms aTransforms, boolean aGui3d, boolean aBlockLight, TextureAtlasSprite aParticle) {
			mQuads = aQuads; mTransforms = aTransforms == null ? ItemTransforms.NO_TRANSFORMS : aTransforms;
			mGui3d = aGui3d; mBlockLight = aBlockLight; mParticle = aParticle;
		}

		@Override public List<BakedQuad> getQuads(BlockState aState, Direction aSide, RandomSource aRandom) {return aSide == null ? mQuads : java.util.List.of();}
		@Override public boolean useAmbientOcclusion() {return false;}
		@Override public boolean isGui3d() {return mGui3d;}
		@Override public boolean usesBlockLight() {return mBlockLight;}
		@Override public boolean isCustomRenderer() {return mCustomRenderer;}
		/** BP-BUG-007: ВТОРОЙ носитель предметного плеча — разрешённая per-стек модель. Движок спрашивает слой
		 *  именно у той модели, которую отдал {@code ItemOverrides}, поэтому ответ обязан быть и здесь; иначе
		 *  класс закрыт наполовину и стекло остаётся непрозрачным ровно у тех предметов, что идут через кэш. */
		@Override public net.minecraftforge.client.ChunkRenderTypeSet getRenderTypes(BlockState aState, RandomSource aRandom, net.minecraftforge.client.model.data.ModelData aData) {
			return GT6BlockModel.renderTypesOf(aState, null);
		}
		@Override public ItemOverrides getOverrides() {return ItemOverrides.EMPTY;}
		@Override public ItemTransforms getTransforms() {return mTransforms;}
		@Override public TextureAtlasSprite getParticleIcon() {return mParticle;}

		/** BUG-028: полоски прочности/заряда рисуются ТОЛЬКО в инвентаре/GUI. В 1.7.10 развилка «инвентарь vs предмет
		 *  в мире» жила в самом рендер-движке (getRenderPasses всегда отдаёт base+2), поэтому мост-адаптер порта
		 *  воспроизводит её здесь, централизованно. Носитель контекста в 1.20.1 — {@code applyTransform}: движок зовёт
		 *  его ПЕРЕД сбором квадов и рисует ВОЗВРАЩЁННУЮ модель ({@code ItemRenderer.render} →
		 *  {@code ForgeHooksClient.handleCameraTransforms}), то есть это штатная точка «модель зависит от контекста». */
		@Override
		public BakedModel applyTransform(ItemDisplayContext aContext, PoseStack aPoseStack, boolean aLeftHand) {
			mTransforms.getTransform(aContext).apply(aLeftHand, aPoseStack);
			return aContext == ItemDisplayContext.GUI ? this : mWorldVariant;
		}
	}

	// ==================== ветви сборки ====================

	/** Предмет-форма БЛОКА: 3D-геометрия блока в инвентаре через {@link GT6BlockModel#buildInventoryQuads} (= renderInventoryBlock). */
	private static Baked buildBlockInventory(ItemStack aStack, net.minecraft.world.level.block.Block aBlock) {
		// Правка №3: 3D-форма блока-предмета — функция (блок, мета, NBT стека); кэш глобальный, сборка на промах.
		if (sInvCache.size() > 16384) sInvCache.clear();
		CompoundTag tTag = aStack.getTag();
		List<BakedQuad> tBuilt = sInvCache.computeIfAbsent(new InvKey(aBlock, gregapi.util.ST.meta_(aStack), tTag == null ? null : tTag.copy()), aKey -> {
			GT6QuadBuilder tQB = new GT6QuadBuilder();
			try { GT6BlockModel.buildInventoryQuads(tQB, aBlock, aStack); } catch (Throwable e) {}
			return java.util.List.copyOf(tQB.quads());
		});
		// КОРЕНЬ «блоки в инвентаре — плоская тёмная грань, не 3D-куб»: buildInventoryQuads даёт куб в 0..1, но без display-
		// трансформации движок рисует его фронтально (видна одна грань; диффуз на неповёрнутой грани тёмный). В 1.7.10
		// изометрию блока-предмета применял движок (RenderBlocks.renderBlockAsItem), здесь — ItemTransforms модели. Берём
		// КАНОНИЧЕСКУЮ block-GUI трансформацию (изометрия 30/225, scale 0.625) ИЗ ДВИЖКА (block/block.json), не хардкодим.
		TextureAtlasSprite tParticle = tBuilt.isEmpty() ? null : tBuilt.get(0).getSprite();
		Baked rBaked = new Baked(tBuilt, blockGuiTransforms(), true, true, tParticle);
		// 1.7.10 renderItem TESR-классов (Chest/MassStorage) звал renderTileEntityAt(this,0,0,0,0) на canonical-TE
		// (данные из NBT стека). Носитель 1.20.1 — BEWLR: модель объявляет isCustomRenderer, движок уходит в
		// IClientItemExtensions.getCustomRenderer() → MultiTileEntityBER.SPECIAL_ITEM_FORM (тот же диспетч по классу TE).
		if (tBuilt.isEmpty() && MultiTileEntityBER.extractSpecialItemForm(aStack) != null) rBaked.mCustomRenderer = true;
		return rBaked;
	}

	/** Рельс в инвентаре: плоская straight-иконка (мета 0, primary) — как ванильный item рельса. Иконка рельса лежит
	 *  среди блок-текстур (iconsets/rail_*), потому резолв ITEMS→BLOCKS. Переиспользует flat-геометрию (front+back + ободок BUG-031). */
	private static Baked buildRailItem(gregapi.block.misc.BlockBaseRail aRail) {
		ResourceLocation tIcon = aRail.getIcon(0, 0);
		TextureAtlasSprite tSprite = tIcon == null ? null : GT6QuadBuilder.resolveSprite(tIcon, GT6QuadBuilder.ATLAS_ITEMS);
		if (tSprite == null && tIcon != null) tSprite = GT6QuadBuilder.resolveSprite(tIcon, GT6QuadBuilder.ATLAS_BLOCKS);
		if (tSprite == null) return new Baked(java.util.List.of(), ItemTransforms.NO_TRANSFORMS, false, false, null);
		// BUG-112: рельс — плоская иконка и НЕ full3D (в 1.7.10 его ItemBlock не звал setFull3D) → положение «плашмя»;
		// плоский предмет в GUI full-bright (эталон ItemModelGenerator = GuiLight.FRONT) → usesBlockLight=false.
		return new Baked(flatQuads(tSprite, -1, true), flatItemTransforms(false), false, false, tSprite);
	}

	/** Предмет-ПРЕДМЕТ (материал/MultiItem): по РЕНДЕР-ПАССАМ getIcon(stack,pass) + тинт getColorFromItemStack(stack,pass). */
	private static Baked buildFlatItem(ItemStack aStack, net.minecraft.world.item.Item aItem) {
		java.util.ArrayList<BakedQuad> tGui = new java.util.ArrayList<>();
		java.util.ArrayList<BakedQuad> tWorld = new java.util.ArrayList<>();
		TextureAtlasSprite tParticle = null;
		boolean tHasBars = false;
		int tPasses = itemRenderPasses(aItem, aStack);
		for (int tPass = 0; tPass < tPasses; tPass++) {
			ResourceLocation tIcon = iconForPass(aItem, aStack, tPass);
			if (tIcon == null) { if (tPass == 0) break; else continue; }
			TextureAtlasSprite tSprite = GT6QuadBuilder.resolveSprite(tIcon, GT6QuadBuilder.ATLAS_ITEMS);
			if (tSprite == null) tSprite = GT6QuadBuilder.resolveSprite(tIcon, GT6QuadBuilder.ATLAS_BLOCKS);
			if (tSprite == null) continue;
			int tColor = itemColor(aItem, aStack, tPass);
			// BUG-028: признак бар-оверлея — членство в ЦЕНТРАЛЬНОМ реестре иконок мода (НЕ index-математика «последние 2»:
			// у PrefixItem пасс 1 — легальный слой материала, бар-иконок не отдаёт → его не заденем).
			boolean tBar = isBarOverlayIcon(tIcon);
			// BUG-031: «толщина» — ободок 1px по контуру (бар-оверлею не строим, 1.7.10 рисовал бар плоско)
			List<BakedQuad> tQuads = flatQuads(tSprite, tColor, !tBar);
			tGui.addAll(tQuads);
			if (tBar) tHasBars = true; else tWorld.addAll(tQuads);
			if (tParticle == null) tParticle = tSprite;
		}
		// BUG-112: положение в руке/на земле/в рамке. Канал различия — тот же, что в 1.7.10: isFull3D().
		ItemTransforms tTransforms = flatItemTransforms(isFull3D(aItem));
		Baked rGui = new Baked(java.util.List.copyOf(tGui), tTransforms, false, false, tParticle);
		// вне GUI (первое/третье лицо, земля, рамка) бар-оверлейные пассы пропускаем — 1:1 с 1.7.10
		if (tHasBars) rGui.mWorldVariant = new Baked(java.util.List.copyOf(tWorld), tTransforms, false, false, tParticle);
		return rGui;
	}

	// Канонические трансформации ванильных моделей — кэш по пути модели; читаются ИЗ ДВИЖКА один раз (после bake).
	private static final java.util.Map<String, ItemTransforms> sVanillaTransforms = new java.util.concurrent.ConcurrentHashMap<>();
	private static final java.util.Set<String> sVanillaTransformsTried = java.util.concurrent.ConcurrentHashMap.newKeySet();
	/** ItemTransforms ванильного {@code minecraft:block/block} (его {@code display.gui} — изометрия 30/225, scale 0.625). */
	private static ItemTransforms blockGuiTransforms() {return vanillaTransforms("block/block");}

	/** Тот же канал, что и в 1.7.10: {@code Item.isFull3D()} (у GT6 его несут {@code MultiItemTool} — все инструменты и
	 *  мечи — и {@code ItemBase.setFull3D()} — спреи, паяльник от {@code GT_Tool_Item}). Спрашиваем КОНТРАКТ базового
	 *  класса предметов мода, а не иерархию инструментов. */
	private static boolean isFull3D(net.minecraft.world.item.Item aItem) {
		return aItem instanceof gregapi.item.ItemBase tBase && tBase.isFull3D();
	}

	/** BUG-112: положение ПЛОСКОГО предмета. В 1.7.10 его задавал сам движок и различал ровно два случая по
	 *  {@code Item.isFull3D()} — «как рукоять» (RenderPlayer:353-374: поворот -100/45, scale 0.625) для инструментов и
	 *  мечей, и «плашмя» для остальных иконок. Носитель этого различия — ItemTransforms модели: ванильные
	 *  {@code item/handheld} и {@code item/generated} несут ровно те же два положения. Потому берём их ИЗ ДВИЖКА тем же
	 *  приёмом, что и {@code block/block} выше, — углы не выдумываем. */
	private static ItemTransforms flatItemTransforms(boolean aFull3D) {
		return vanillaTransforms(aFull3D ? "item/handheld" : "item/generated");
	}

	/** ItemTransforms ванильной модели по её пути ({@code block/block} — изометрия 30/225 scale 0.625; {@code item/handheld}
	 *  и {@code item/generated} — два положения предмета в руке). Берутся из движкового {@code ModelBakery.getModel(rl)}
	 *  ({@code ModelBakery.java:229}, публичный) → {@code BlockModel.getTransforms()} ({@code BlockModel.java:270}),
	 *  НЕ хардкод-константами (§«не выдумывать константы»). В 26.x публичного геттера не было и приходилось лезть
	 *  отражением в {@code ModelBakery.resolvedModels}; в 1.20.1 канал открыт — отражение снято. */
	private static ItemTransforms vanillaTransforms(String aModelPath) {
		ItemTransforms rCached = sVanillaTransforms.get(aModelPath);
		if (rCached != null || sVanillaTransformsTried.contains(aModelPath)) return rCached;
		sVanillaTransformsTried.add(aModelPath);
		try {
			net.minecraft.client.resources.model.ModelBakery tBakery = net.minecraft.client.Minecraft.getInstance().getModelManager().getModelBakery();
			net.minecraft.client.resources.model.UnbakedModel tModel = tBakery.getModel(ResourceLocation.withDefaultNamespace(aModelPath));
			if (tModel instanceof net.minecraft.client.renderer.block.model.BlockModel tBlockModel) {
				ItemTransforms tTr = tBlockModel.getTransforms();
				if (tTr != null) sVanillaTransforms.put(aModelPath, tTr);
				return tTr;
			}
		} catch (Throwable e) {/* модель недоступна -> fallback NO_TRANSFORMS */}
		return null;
	}

	/** Число рендер-пассов предмета (PrefixItem.getRenderPasses(int)=2). Нет метода → 1 пасс. */
	private static int itemRenderPasses(Object aItem, ItemStack aStack) {
		try { java.lang.reflect.Method m = cachedMethod(aItem.getClass(), "getRenderPasses", int.class); if (m != null) { Object r = m.invoke(aItem, (int)gregapi.util.ST.meta_(aStack)); if (r instanceof Integer ri && ri > 0) return Math.min(ri, 8); } } catch (Throwable e) {}
		return 1;
	}
	/** Иконка предмета на пасс: GT6 {@code getIcon(stack,pass)} (=getIconFromDamageForRenderPass); fallback pass0 getIconIndex/getIconFromDamage. */
	private static ResourceLocation iconForPass(Object aItem, ItemStack aStack, int aPass) {
		try { java.lang.reflect.Method m = cachedMethod(aItem.getClass(), "getIcon", ItemStack.class, int.class); if (m != null) { Object o = m.invoke(aItem, aStack, aPass); if (o instanceof ResourceLocation id) return id; } } catch (Throwable e) {}
		if (aPass == 0) { ResourceLocation r = tryIcon(aItem, "getIconIndex", ItemStack.class, aStack); if (r == null) r = tryIcon(aItem, "getIconFromDamage", int.class, aStack.getDamageValue()); return r; }
		return null;
	}
	/** GT6 {@code getColorFromItemStack(stack,pass)} → 0xRRGGBB (pass0 = материал-тинт, иначе 0xFFFFFF белый). */
	private static int itemColor(Object aItem, ItemStack aStack, int aPass) {
		try { java.lang.reflect.Method m = cachedMethod(aItem.getClass(), "getColorFromItemStack", ItemStack.class, int.class); if (m != null) { Object c = m.invoke(aItem, aStack, aPass); if (c instanceof Integer ci) return ci; } } catch (Throwable e) {}
		return 0xFFFFFF;
	}

	// BUG-028: центральный набор бар-оверлейных иконок мода — Textures.ItemIcons.DURABILITY_BAR ∪ ENERGY_BAR (те же
	// IIconContainer'ы, что отдаёт MultiItemTool.getIcon на последних 2 пассах). Не хардкод-строки и не index-эвристика — опора
	// на существующий центральный реестр текстур. Ленивый кэш (иконки резолвятся лениво getIcon→run() после bake атласа; строим
	// при первом рендере); кэшируем только непустой (полностью резолвнутый) набор.
	private static java.util.Set<ResourceLocation> sBarOverlayIcons;
	private static java.util.Set<ResourceLocation> barOverlayIcons() {
		if (sBarOverlayIcons != null) return sBarOverlayIcons;
		java.util.HashSet<ResourceLocation> tSet = new java.util.HashSet<>();
		try {
			for (gregapi.render.IIconContainer c : gregapi.old.Textures.ItemIcons.DURABILITY_BAR) { ResourceLocation i = c.getIcon(0); if (i != null) tSet.add(i); }
			for (gregapi.render.IIconContainer c : gregapi.old.Textures.ItemIcons.ENERGY_BAR)     { ResourceLocation i = c.getIcon(0); if (i != null) tSet.add(i); }
		} catch (Throwable e) {}
		if (!tSet.isEmpty()) sBarOverlayIcons = tSet;
		return tSet;
	}
	/** BUG-028: иконка пасса — бар-оверлей прочности/заряда (GUI-only)? Мембершип по центральному реестру мода. */
	private static boolean isBarOverlayIcon(ResourceLocation aIcon) { return aIcon != null && barOverlayIcons().contains(aIcon); }

	/** Икона предмета: GT6 {@code getIconIndex(ItemStack)} (PrefixItem/MultiItem) → ResourceLocation; иначе {@code getIconFromDamage(int)}.
	 *  public — переиспользуется скан-оснасткой рендера (GT6RenderProbe) для приёмки «иконки не пурпур». */
	public static ResourceLocation resolveIcon(ItemStack aItem) {
		Object tItem = aItem.getItem();
		ResourceLocation r = tryIcon(tItem, "getIconIndex", ItemStack.class, aItem);
		if (r == null) r = tryIcon(tItem, "getIconFromDamage", int.class, aItem.getDamageValue());
		return r;
	}

	private static ResourceLocation tryIcon(Object aTarget, String aMethod, Class<?> aArgType, Object aArg) {
		try {
			java.lang.reflect.Method m = cachedMethod(aTarget.getClass(), aMethod, aArgType);
			if (m == null) return null;
			Object o = m.invoke(aTarget, aArg);
			return o instanceof ResourceLocation tId ? tId : null;
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
		org.joml.Vector3f n = tDir.step();
		QuadBakingVertexConsumer.Buffered b = new QuadBakingVertexConsumer.Buffered();
		b.setSprite(aSprite);
		b.setDirection(tDir);
		b.setTintIndex(-1); // тинт материала УЖЕ запечён в вершины; tintIndex по умолчанию 0 → движок домножил бы ещё раз через ItemColors
		b.setShade(false);  // плоский предмет — ровный свет (эталон ItemModelGenerator/GuiLight.FRONT), без направленного затенения
		for (int i = 3; i >= 0; i--) { // КОРЕНЬ затемнения: winding вершин был инвертирован vs канон Mojang (FaceInfo) → GPU backface-cull скрывал SOUTH-грань (яркая нормаль) и показывал NORTH-грань (тёмная под item-диффузом). Реверс порядка (i=3→0) чинит winding — тот же приём, что в GT6QuadBuilder.boundedFace
			b.vertex(c[i][0], c[i][1], c[i][2]);
			b.color(r, g, b8, 255); // тинт материала (белая проба подтвердила: цвет-механизм работает; корень — свет)
			b.normal(n.x(), n.y(), n.z());
			b.uv(aSprite.getU(c[i][3]), aSprite.getV(c[i][4])); // getU/getV 1.20.1 сами делят на 16 (шкала 0..16, канон 1.7.10) — см. GT6QuadBuilder.boundedFace
			b.endVertex();
		}
		return b.getQuad();
	}

	// ==================== BUG-031: боковой ободок «толщины» плоского предмета ====================
	// Дословная транскрипция движкового ItemModelGenerator.bakeSideFaces/getSideFaces/checkTransition/isTransparent:
	// для КАЖДОГО непрозрачного пикселя спрайта, у которого сосед прозрачен, строится боковая грань толщиной 1px
	// (z=7.5..8.5/16) по контуру силуэта — та самая «толщина», отличающая ванильный плоский предмет от плоской картинки.
	// Ваниль делает скан ОДИН раз при bake модели; здесь результат скана кэшируется по спрайту (сами quad'ы дешёвые).

	/** Канон SideDirection (ItemModelGenerator.SideDirection): UP→Direction.UP, DOWN→DOWN, LEFT→EAST, RIGHT→WEST. */
	private static final Direction[] SIDE_DIRS = {Direction.UP, Direction.DOWN, Direction.EAST, Direction.WEST};
	/** Кэш пиксельного скана: имя спрайта → список граней {dirIdx, x, y} (union по всем кадрам анимации, как ваниль). */
	private static final java.util.concurrent.ConcurrentHashMap<String, int[][]> sSideFaceCache = new java.util.concurrent.ConcurrentHashMap<>();

	private static void addSideQuads(List<BakedQuad> aOut, TextureAtlasSprite aSprite, int aColor) {
		int[][] tFaces = sideFacesOf(aSprite);
		net.minecraft.client.renderer.texture.SpriteContents tC = aSprite.contents();
		float tXScale = 16.0F / tC.width(), tYScale = 16.0F / tC.height(); // bakeSideFaces:117-118
		for (int[] tFace : tFaces) {
			int tDir = tFace[0]; float x = tFace[1], y = tFace[2];
			// UV (bakeSideFaces:124-135): подрез 0.1px от краёв пикселя; вертикальные грани — V перевёрнут
			float u0 = x + 0.1F, u1 = x + 1.0F - 0.1F, v0, v1;
			if (tDir <= 1) {v0 = y + 0.1F; v1 = y + 1.0F - 0.1F;} else {v0 = y + 1.0F - 0.1F; v1 = y + 0.1F;} // isHorizontal = UP|DOWN
			// Геометрия (bakeSideFaces:137-186): границы строки в пикселях → масштаб → flip Y текстуры (y вниз) в модель (y вверх)
			float tStartX = x, tStartY = y, tEndX = x, tEndY = y;
			switch (tDir) {
				case 0: tEndX = x + 1.0F; break;                                    // UP
				case 1: tEndX = x + 1.0F; tStartY = y + 1.0F; tEndY = y + 1.0F; break; // DOWN
				case 2: tEndY = y + 1.0F; break;                                    // LEFT (EAST)
				default: tStartX = x + 1.0F; tEndX = x + 1.0F; tEndY = y + 1.0F;    // RIGHT (WEST)
			}
			tStartX *= tXScale; tEndX *= tXScale; tStartY *= tYScale; tEndY *= tYScale;
			tStartY = 16.0F - tStartY; tEndY = 16.0F - tEndY;
			float[] tFrom, tTo;
			switch (tDir) {
				case 0:  tFrom = new float[]{tStartX, tStartY, 7.5F}; tTo = new float[]{tEndX,   tStartY, 8.5F}; break; // UP
				case 1:  tFrom = new float[]{tStartX, tEndY,   7.5F}; tTo = new float[]{tEndX,   tEndY,   8.5F}; break; // DOWN
				case 2:  tFrom = new float[]{tStartX, tStartY, 7.5F}; tTo = new float[]{tStartX, tEndY,   8.5F}; break; // LEFT
				default: tFrom = new float[]{tEndX,   tStartY, 7.5F}; tTo = new float[]{tEndX,   tEndY,   8.5F}; break; // RIGHT
			}
			aOut.add(sideQuad(aSprite, SIDE_DIRS[tDir], tFrom, tTo, u0 * tXScale, v0 * tYScale, u1 * tXScale, v1 * tYScale, aColor));
		}
	}

	/** Пиксельный скан контура (getSideFaces/checkTransition/isTransparent), кэш по спрайту. */
	private static int[][] sideFacesOf(TextureAtlasSprite aSprite) {
		net.minecraft.client.renderer.texture.SpriteContents tC = aSprite.contents();
		String tKey = tC.name().toString();
		int[][] tCached = sSideFaceCache.get(tKey);
		if (tCached != null) return tCached;
		java.util.LinkedHashSet<Integer> tSet = new java.util.LinkedHashSet<>();
		try {
			int w = tC.width(), h = tC.height();
			int[] tFrames = tC.getUniqueFrames().toArray(); // 1.20.1: getUniqueFrames отдаёт IntStream (SpriteContents.java:162)
			for (int f = 0; f < tFrames.length; f++) {
				int tFrame = tFrames[f];
				for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) {
					if (sideTransparent(tC, tFrame, x, y, w, h)) continue;
					// checkTransition: сосед (x-stepX, y-stepY) прозрачен → грань (шаги Direction: UP=(0,1) → сосед (x,y-1) и т.д.)
					if (sideTransparent(tC, tFrame, x,     y - 1, w, h)) tSet.add(sideKey(0, x, y)); // UP
					if (sideTransparent(tC, tFrame, x,     y + 1, w, h)) tSet.add(sideKey(1, x, y)); // DOWN
					if (sideTransparent(tC, tFrame, x - 1, y,     w, h)) tSet.add(sideKey(2, x, y)); // LEFT (EAST)
					if (sideTransparent(tC, tFrame, x + 1, y,     w, h)) tSet.add(sideKey(3, x, y)); // RIGHT (WEST)
				}
			}
		} catch (Throwable e) {tSet.clear();} // пиксели недоступны — предмет остаётся без ободка (перед/зад целы)
		int[][] rFaces = new int[tSet.size()][]; int i = 0;
		for (int tKey2 : tSet) rFaces[i++] = new int[]{tKey2 >>> 28, (tKey2 >>> 14) & 0x3FFF, tKey2 & 0x3FFF};
		sSideFaceCache.put(tKey, rFaces);
		return rFaces;
	}
	private static int sideKey(int aDir, int aX, int aY) {return (aDir << 28) | (aX << 14) | aY;}
	private static boolean sideTransparent(net.minecraft.client.renderer.texture.SpriteContents aC, int aFrame, int aX, int aY, int aW, int aH) {
		return aX < 0 || aY < 0 || aX >= aW || aY >= aH || aC.isTransparent(aFrame, aX, aY); // isTransparent (вне спрайта = прозрачно)
	}

	/** Боковой quad по канону FaceBakery: порядок вершин FaceInfo (MIN=from/MAX=to),
	 *  UV по индексу вершины (u→minU для 0,1 / maxU для 2,3; v→minV для 0,3 / maxV для 1,2).
	 *  Прямой FaceInfo-порядок даёт тот же winding, что реверс в {@link #flatFace} (сверено по SOUTH-циклу). */
	private static BakedQuad sideQuad(TextureAtlasSprite aSprite, Direction aDir, float[] aFrom, float[] aTo, float aMinU, float aMinV, float aMaxU, float aMaxV, int aColor) {
		int r = (aColor >> 16) & 0xFF, g = (aColor >> 8) & 0xFF, b8 = aColor & 0xFF;
		// FaceInfo: селектор from/to по осям для 4 вершин грани (1=to, 0=from)
		int[][] tSel;
		switch (aDir) {
			case UP:   tSel = new int[][]{{0,1,0},{0,1,1},{1,1,1},{1,1,0}}; break;
			case DOWN: tSel = new int[][]{{0,0,1},{0,0,0},{1,0,0},{1,0,1}}; break;
			case WEST: tSel = new int[][]{{0,1,0},{0,0,0},{0,0,1},{0,1,1}}; break;
			default:   tSel = new int[][]{{1,1,1},{1,0,1},{1,0,0},{1,1,0}}; break; // EAST
		}
		org.joml.Vector3f n = aDir.step();
		QuadBakingVertexConsumer.Buffered b = new QuadBakingVertexConsumer.Buffered();
		b.setSprite(aSprite);
		b.setDirection(aDir);
		b.setTintIndex(-1);
		b.setShade(false); // как flatFace: единая яркость модели плоского предмета
		for (int i = 0; i < 4; i++) {
			b.vertex((tSel[i][0] == 1 ? aTo[0] : aFrom[0]) / 16f, (tSel[i][1] == 1 ? aTo[1] : aFrom[1]) / 16f, (tSel[i][2] == 1 ? aTo[2] : aFrom[2]) / 16f);
			b.color(r, g, b8, 255);
			b.normal(n.x(), n.y(), n.z());
			b.uv(aSprite.getU(i == 0 || i == 1 ? aMinU : aMaxU), aSprite.getV(i == 0 || i == 3 ? aMinV : aMaxV)); // getU/getV 1.20.1 сами делят на 16
			b.endVertex();
		}
		return b.getQuad();
	}
}

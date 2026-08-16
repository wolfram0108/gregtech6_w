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

import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.ChunkRenderTypeSet;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.client.model.data.ModelProperty;
import net.minecraft.core.Direction;

/**
 * F3-render (client): единая динамическая модель ВСЕХ GT6-блоков (аналог одного {@code RendererBlockTextured} у Грегориуса —
 * централизация 1:1). Движок зовёт {@link #getModelData} (чанк-компиляция, {@code ChunkRenderDispatcher:631}) → берём
 * {@link IRenderedBlock} из блока → per pass×side зовём его {@code getTexture(...)} →
 * {@link ITexture}{@code .render<Side>(quadBuilder,...)} → {@link GT6QuadBuilder} аккумулирует quads → результат едет
 * в {@link #getQuads} через {@link ModelData}. GT6 per-side texture-логика переиспользуется без переписывания; заменён
 * лишь механизм отрисовки (immediate→baked).
 *
 * <p><b>Ветка 1.20.1.</b> Канал «модель видит мир» здесь — Forge {@code ModelData}/{@code ModelProperty}
 * ({@code IForgeBakedModel.getModelData(level,pos,state,data)} → {@code getQuads(state,side,rand,data,renderType)}) —
 * ровно тот, что описан планом F3-render.md §2.4 (в 26.x он был удалён, и ветка ходила через
 * {@code DynamicBlockStateModel.collectParts(level,pos,...)}). Порядок вызовов GT6 не изменился — сменился только
 * носитель контекста. Спрайты — из block-атласа в рантайме (динамика материал×префикс). Регистрация — рантайм-инъекция
 * в {@code ModelEvent.ModifyBakingResult} (GT_API_Proxy_Client). См. F3-render.md §2.
 */
public class GT6BlockModel implements BakedModel {
	/** Канал «мир → модель» (F3-render.md §2.4, эталон AE2 {@code CableBusRenderState.PROPERTY}). Один ключ на весь мод. */
	public static final ModelProperty<QuadContext> PROPERTY = new ModelProperty<>();

	/** Готовая геометрия одной позиции, собранная в {@link #getModelData}; {@link #getQuads} её только режет по сторонам. */
	public static final class QuadContext {
		final GT6QuadBuilder.QuadSet mQuads;
		QuadContext(GT6QuadBuilder.QuadSet aQuads) {mQuads = aQuads;}
	}

	/** Блок-владелец (инъекция ModifyBakingResult даёт per-state инстансы): нужен там, где контекста мира НЕТ —
	 *  breaking-оверлей движка и статический запрос модели (JourneyMap). null = куб-фолбэк. */
	private final Block mOwner;
	/** BlockState-владелец: в 1.20.1 {@code getParticleIcon(ModelData)} не получает ни state, ни pos — мету крошки
	 *  берём отсюда тем же центром {@code IBlockExtendedMetaData}, которым её берёт источник тинта GT6. */
	private final BlockState mOwnerState;
	/** Ленивый кэш безконтекстной формы (чистая функция владельца): breaking-оверлей и JourneyMap зовут её каждый кадр. */
	private volatile GT6QuadBuilder.QuadSet mContextFree;
	private volatile boolean mContextFreeBuilt = false;

	/** Путь ModelEvent.ModifyBakingResult. Ветка 1.20.1: спрайт-фолбэк резолвится ЛЕНИВО — событие приходит на
	 *  рабочем потоке до готовности {@code ModelManager} (см. его javadoc: «must therefore not be accessed»),
	 *  а {@code getParticleIcon} зовут уже в рантайме. */
	public GT6BlockModel() {this(null, null);}
	public GT6BlockModel(Block aOwner) {this(aOwner, null);}
	public GT6BlockModel(Block aOwner, BlockState aOwnerState) {mOwner = aOwner; mOwnerState = aOwnerState;}

	/** Фолбэк-спрайт крошки (1:1 прежнего particle всей модели): {@code gregtech:system/error}. sprite-id БЕЗ
	 *  префикса {@code blocks/} — atlas-source кладёт {@code textures/blocks/**} с {@code prefix:""}. */
	private static TextureAtlasSprite sErrorParticle;
	private static TextureAtlasSprite errorParticle() {
		if (sErrorParticle == null) sErrorParticle = GT6QuadBuilder.resolveSprite(ResourceLocation.fromNamespaceAndPath("gregtech", "system/error"));
		return sErrorParticle;
	}
	/** Сброс ленивого спрайта при перепечке атласа (зовёт onModifyBakingResult вместе с кэшами item-модели). */
	public static void invalidateParticle() {sErrorParticle = null;}

	// ==================== контракт BakedModel 1.20.1 ====================

	/** Точка, где модель ВИДИТ МИР (чанк-компиляция зовёт её прямо перед тесселяцией: {@code ChunkRenderDispatcher:631}).
	 *  Здесь и только здесь работает GT6-цепочка getRenderPasses/setBlockBounds/getTexture. */
	@Override
	public ModelData getModelData(BlockAndTintGetter aLevel, BlockPos aPos, BlockState aState, ModelData aModelData) {
		GT6QuadBuilder.QuadSet tQuads = collect(aLevel, aPos, aState);
		return tQuads == null ? aModelData : aModelData.derive().with(PROPERTY, new QuadContext(tQuads)).build();
	}

	@Override
	public List<BakedQuad> getQuads(BlockState aState, Direction aSide, RandomSource aRandom, ModelData aData, RenderType aRenderType) {
		QuadContext tCtx = aData == null ? null : aData.get(PROPERTY);
		return (tCtx != null ? tCtx.mQuads : contextFree()).getQuads(aSide);
	}

	/** Ванильная перегрузка без ModelData — сюда приходят потребители вне чанк-пути. */
	@Override
	public List<BakedQuad> getQuads(BlockState aState, Direction aSide, RandomSource aRandom) {
		return getQuads(aState, aSide, aRandom, ModelData.EMPTY, null);
	}

	/** ЕДИНАЯ ФОРМУЛА СЛОЯ ОТРИСОВКИ на ВСЕ модели GT6 (BP-BUG-007). 1.7.10-канал {@code getRenderBlockPass()}
	 *  (0 = alpha-test, 1 = блендинг), см. {@code IBlock}.
	 *
	 *  <p><b>Почему центр, а не метод одной модели.</b> Движок 1.20.1 сам слой не выбирает (в отличие от 26.1,
	 *  где мод об этом не знает вовсе), поэтому канал завела ветка — и у канала ДВА плеча, мировое и предметное.
	 *  Предметное спрашивает слой не у блочной модели, а у ITEM-модели: {@code RenderTypeHelper
	 *  .getFallbackItemRenderType:63-68} берёт {@code BlockItem} и зовёт {@code model.getRenderTypes(block
	 *  .defaultBlockState(), …)} У НЕЁ ЖЕ. Пока эту перегрузку несла только {@link GT6BlockModel}, item-плечо
	 *  падало в дефолт {@code ItemBlockRenderTypes.getRenderLayers} ({@code IForgeBakedModel:85-88}), где блоков
	 *  GT6 нет, и стекло в GUI выходило CUTOUT — без блендинга. Формула одна, потребителей три; второй таблицы
	 *  слоёв не заводим. Блок берётся из состояния (его и передаёт движок), {@code aFallback} — на случай null. */
	public static ChunkRenderTypeSet renderTypesOf(BlockState aState, Block aFallback) {
		return ChunkRenderTypeSet.of(renderTypeOf(aState == null ? aFallback : aState.getBlock()));
	}

	/** ТА ЖЕ ФОРМУЛА, ответ одним слоем — четвёртый потребитель центра (BP-BUG-015, поверхность жидкости).
	 *  Клетку роли {@code OWN_TAGGED_FLUID} движок рисует не моделью, а ЖИДКОСТНЫМ проходом, и слой берёт у
	 *  СВОЕЙ таблицы {@code ItemBlockRenderTypes.getRenderLayer(FluidState)} — где дефолт {@code solid}
	 *  ({@code ItemBlockRenderTypes.java:377-379,399-401}), то есть поверхность непрозрачна. Ваниль пишет туда
	 *  свою воду статически (:315-319); мод обязан записать свою — но НЕ второй таблицей слоёв: величина
	 *  остаётся 1.7.10-каналом {@code getRenderBlockPass()} того же блока (у обеих жидкостных семей это 1,
	 *  {@code BlockBaseFluid:473} = оригинал {@code BlockBaseFluid.java:366}). Регистрация — центр
	 *  {@code GT_API_Proxy_Client.registerFluidRenderLayers}. */
	public static RenderType renderTypeOf(Block aBlock) {
		int tPass = aBlock instanceof gregapi.block.IBlock tI ? tI.getRenderBlockPass() : 0;
		return tPass > 0 ? RenderType.translucent() : RenderType.cutout();
	}

	/** Слой отрисовки блока в МИРЕ — тем же центром, что и предметное плечо. */
	@Override
	public ChunkRenderTypeSet getRenderTypes(BlockState aState, RandomSource aRandom, ModelData aData) {
		return renderTypesOf(aState, mOwner);
	}

	@Override public boolean useAmbientOcclusion() {return true;}
	@Override public boolean isGui3d() {return true;}
	@Override public boolean usesBlockLight() {return true;}
	@Override public boolean isCustomRenderer() {return false;}
	@Override public ItemOverrides getOverrides() {return ItemOverrides.EMPTY;}
	@Override public ItemTransforms getTransforms() {return ItemTransforms.NO_TRANSFORMS;}
	@Override public TextureAtlasSprite getParticleIcon() {return errorParticle();}

	// ==================== GT6-цепочка ====================

	/** Сборка геометрии позиции. Отдельный метод, потому что в 1.20.1 её зовут ДО {@link #getQuads} (из getModelData). */
	private GT6QuadBuilder.QuadSet collect(BlockAndTintGetter aLevel, BlockPos aPos, BlockState aState) {
		// F-bounds-race: вся рендер-цепь — в bounds-контексте (BlockBase.RENDER_BOUNDS_CTX): пассовые setBlockBounds и
		// анти-протечка пишут потоко-локальную копию, НЕ общие поля Block (см. BlockBase.setBlockBounds).
		boolean[] tCtx = gregapi.block.BlockBase.RENDER_BOUNDS_CTX.get(); boolean tPrevCtx = tCtx[0]; tCtx[0] = true;
		try {
			return collectParts0(aLevel, aPos, aState);
		} finally {tCtx[0] = tPrevCtx;}
	}

	/** Безконтекстная форма блока (breaking-оверлей движка и статический запрос модели): чистая функция владельца. */
	private GT6QuadBuilder.QuadSet contextFree() {
		if (mContextFreeBuilt) return mContextFree;
		boolean[] tCtx = gregapi.block.BlockBase.RENDER_BOUNDS_CTX.get(); boolean tPrevCtx = tCtx[0]; tCtx[0] = true;
		try {mContextFree = buildContextFree();} catch (Throwable e) {mContextFree = new GT6QuadBuilder().build();} finally {tCtx[0] = tPrevCtx;}
		mContextFreeBuilt = true;
		return mContextFree;
	}

	/** F3-render ТРЕЩИНЫ (репорт игрока «нет текстуры трещин» + уточнение «в оригинале трещины ложились ПРЯМО
	 *  на поверхность трубы/камня/верёвки/куста») и статический запрос модели (MODCOMPAT-002).
	 *  <p>Ветка 1.20.1: контекста мира у {@code getQuads} нет вовсе (движок зовёт breaking-оверлей через
	 *  {@code LevelRenderer:1315} → {@code BlockRenderDispatcher.renderBreakingTexture} с {@code ModelData.EMPTY},
	 *  а JourneyMap и прочие читатели моделей вне мира — через ванильную перегрузку), поэтому та же форма строится
	 *  здесь, по владельцу. Диспатч по mOwner: MTE (обе иерархии) → ПУСТО, их геометрию (и трещины на ней) даёт
	 *  {@link MultiTileEntityBER}; куст/цветок (IRenderedCross) → крест; остальные (IBlock) → статические bounds
	 *  (полублок = полбокса); неизвестный владелец → куб. UV трещин пересчитывает {@code SheetedDecalTextureGenerator}
	 *  по ПОЗИЦИИ — спрайт не важен, важна ГЕОМЕТРИЯ.
	 *  <p>Первоисточник — ITEM-ФОРМА блока ({@link #buildInventoryQuads}: тот же центр, что 3D-иконка в инвентаре/JEI —
	 *  настоящие per-pass текстуры С КОЛОРИЗАЦИЕЙ; в 1.7.10 карта видела раскрашенный канал getIcon+colorMultiplier,
	 *  голая грейскейл-иконка руды теряла цвет — замер #BCBCBC). Фолбэк — куб/крест из иконки канала {@code IBlock.getIcon}
	 *  (CFOAM — 1:1-дефолт 1.7.10 BlockBase:103/MultiTileEntityBlock:293). */
	private GT6QuadBuilder.QuadSet buildContextFree() {
		GT6QuadBuilder tCrackQB = new GT6QuadBuilder();
		if (mOwner instanceof gregapi.block.multitileentity.MultiTileEntityBlock || mOwner instanceof gregapi.block.multitileentity.MultiTileEntityBlockInternal) return tCrackQB.build();
		if (mOwner != null) {
			net.minecraft.world.item.Item tOwnerItem = net.minecraft.world.item.Item.byBlock(mOwner);
			if (tOwnerItem != null && tOwnerItem != net.minecraft.world.item.Items.AIR) {
				try {buildInventoryQuads(tCrackQB, mOwner, new net.minecraft.world.item.ItemStack(tOwnerItem));} catch (Throwable e) {/* фолбэк ниже */}
			}
		}
		if (tCrackQB.isEmpty()) {
			net.minecraft.resources.ResourceLocation tCrackIcon = null;
			try {
				if (mOwner instanceof IRenderedCross tCross) tCrackIcon = tCross.getCrossIcon(null, 0, 0, 0); // контракт aWorld==null + мета в aX (см. buildInventoryQuads)
				else if (mOwner instanceof gregapi.block.IBlock tGT6) tCrackIcon = tGT6.getIcon(1, 0);
			} catch (Throwable e) {/* фолбэк ниже */}
			if (tCrackIcon == null) tCrackIcon = gregapi.old.Textures.BlockIcons.CFOAM_HARDENED.getIcon(0);
			if (mOwner instanceof IRenderedCross) {
				tCrackQB.crossFace(tCrackIcon, gregapi.data.CS.UNCOLOURED);
			} else {
				tCrackQB.setBounds(mOwner instanceof gregapi.block.IBlock tIB ? tIB.getRenderBounds() : null);
				for (byte tSide = 0; tSide < 6; tSide++) tCrackQB.putFace(tSide, tCrackIcon, gregapi.data.CS.UNCOLOURED);
			}
		}
		return tCrackQB.build();
	}

	private GT6QuadBuilder.QuadSet collectParts0(BlockAndTintGetter aLevel, BlockPos aPos, BlockState aState) {
		// F3-render рельсы: BlockBaseRail наследует vanilla BaseRailBlock (НЕ IRenderedBlock) — плоский рельс-quad по мете
		// (1:1 vanilla renderBlockRail), форма/иконка из меты. Отдельная ветка, минуя box-цепочку IRenderedBlock ниже.
		if (aState.getBlock() instanceof gregapi.block.misc.BlockBaseRail tRail) {
			GT6QuadBuilder tRailQB = new GT6QuadBuilder();
			RailRenderer.collectRailQuads(tRailQB, aLevel, aPos.getX(), aPos.getY(), aPos.getZ(), tRail);
			return tRailQB.build();
		}
		if (!(aState.getBlock() instanceof IRenderedBlock tRB)) return null;
		Block tBlock = aState.getBlock();
		int tX = aPos.getX(), tY = aPos.getY(), tZ = aPos.getZ();
		GT6QuadBuilder tQB = new GT6QuadBuilder();

		// F3-render cross-модель (растения/цветы, IRenderedCross): X-форма из 2 диагональных плоскостей, минуя кубическую цепочку.
		if (tRB instanceof IRenderedCross tCross) {
			tQB.crossFace(tCross.getCrossIcon(aLevel, tX, tY, tZ), tCross.getCrossRGBa(aLevel, tX, tY, tZ));
			return tQB.build();
		}

		// F3-fluid: жидкости-блоки (нефти/газ/гео-вода) — 1:1 порт RendererBlockFluid.renderWorldBlock:
		// кванта-высота, склоны угловых высот по соседям 3×3 (смыкают уровни без дыр), газ зеркально от потолка.
		// Вместо box-пути IRenderedBlock (тот оставлен для item-формы).
		if (tBlock instanceof gregapi.block.fluid.BlockBaseFluid tFluid) {
			RendererBlockFluid.collectFluidQuads(tQB, aLevel, tX, tY, tZ, tFluid);
			return tQB.build();
		}

		// F3-render: MTE-блоки рисует BER (MultiTileEntityBER), НЕ baked-модель. Причина (probe, окончательно): neo section-compile
		// регион (worker-снапшот) НЕ отдаёт MTE-BE (getBlockEntity=null 100%) → тут геометрию собрать нельзя. BER берёт живой BE на
		// main-thread. Пропускаем (пустой меш; часть MTE регион случайно захватывал — рисовали бы дважды с BER). Флюид/BlockBase — ниже (render на блоке).
		if (tBlock instanceof gregapi.block.multitileentity.MultiTileEntityBlock) return tQB.build();

		// 1:1-порт RendererBlockTextured.renderWorldBlock: двойной passRenderingToObject → ветвь блока / ветвь рендер-объекта.
		IRenderedBlockObject tRenderer = tRB.passRenderingToObject(aLevel, tX, tY, tZ);
		if (tRenderer != null) tRenderer = tRenderer.passRenderingToObject(aLevel, tX, tY, tZ);

		if (tRenderer == null) {
			// 1:1 RenderBlocks.renderBlockLog (диспетчер renderType==PILLAR_RENDER, RenderBlocks:350,4430): PILLAR-блоки
			// (брёвна/балки/тюки) поворачивают UV граней по оси укладки из меты: X(4)→низ/верх/север/юг, Z(8)→запад/восток.
			if (tBlock instanceof gregapi.block.BlockBase tBB && tBB.getRenderType() == gregapi.data.CS.PILLAR_RENDER) {
				int tAxis = gregapi.util.WD.meta(aLevel, tX, tY, tZ) & gregapi.data.CS.PILLAR_BITS;
				if (tAxis == gregapi.data.CS.PILLAR_X) tQB.setUVRotate(1, 1, 1, 1, 0, 0);
				else if (tAxis == gregapi.data.CS.PILLAR_Z) tQB.setUVRotate(0, 0, 0, 0, 1, 1);
			}
			boolean[] tSides = sides(tBlock, tRB instanceof IRenderedBlockObjectSideCheck ? (IRenderedBlockObjectSideCheck)tRB : null);
			// КОНТРАКТ setBlockBounds (1:1 renderWorldBlock RendererBlockTextured:121): true → перечитать bounds из блока;
			// false → ПОЛНЫЙ КУБ (сброс 0..1 в блок). Игнор return читал ПОСЛЕДНИЕ сохранённые bounds ОБЩЕГО Block-инстанса
			// → мини-бокс камешка протекал в машины/центры кустов на том же блоке (регресс d87e09e4, репорт игрока).
			boolean tNeedsToSetBounds = true;
			for (int i = 0, j = tRB.getRenderPasses(aLevel, tX, tY, tZ, tSides); i < j; i++) {
				if (!tRB.usesRenderPass(i, aLevel, tX, tY, tZ, tSides)) continue;
				if (tRB.setBlockBounds(i, aLevel, tX, tY, tZ, tSides)) {tNeedsToSetBounds = true;}
				else {if (tNeedsToSetBounds) gregapi.util.WD.setBlockBounds(tBlock, 0, 0, 0, 1, 1, 1); tNeedsToSetBounds = false;}
				applyBounds(tQB, tBlock);
				for (byte s = 0; s < 6; s++) face(tQB, tBlock, s, tRB.getTexture(i, s, tSides, aLevel, tX, tY, tZ), tX, tY, tZ);
			}
			if (tNeedsToSetBounds) gregapi.util.WD.setBlockBounds(tBlock, 0, 0, 0, 1, 1, 1); // анти-протечка общего блока (1:1 :132)
			tQB.clearUVRotate(); // 1:1 renderBlockLog: сброс uvRotate* после renderStandardBlock
		} else {
			buildRendererQuads(tQB, tRenderer, tBlock, aLevel, tX, tY, tZ);
		}
		return tQB.build();
	}

	/** F3-render: ветвь рендер-объекта (getRenderPasses→setBlockBounds→getTexture→quads). Общий код collectParts (baked) и
	 *  MultiTileEntityBER (BER, живой BE на main-thread). renderBlock=true → объект сам нарисовал, цикл не нужен. */
	public static void buildRendererQuads(GT6QuadBuilder aQB, IRenderedBlockObject aRenderer, Block aBlock, net.minecraft.world.level.BlockGetter aLevel, int aX, int aY, int aZ) {
		// F-bounds-race: скобки контекста и здесь — метод зовётся и напрямую (MultiTileEntityBER, main thread).
		boolean[] tCtx = gregapi.block.BlockBase.RENDER_BOUNDS_CTX.get(); boolean tPrevCtx = tCtx[0]; tCtx[0] = true;
		try {buildRendererQuads0(aQB, aRenderer, aBlock, aLevel, aX, aY, aZ);} finally {tCtx[0] = tPrevCtx;}
	}
	private static void buildRendererQuads0(GT6QuadBuilder aQB, IRenderedBlockObject aRenderer, Block aBlock, net.minecraft.world.level.BlockGetter aLevel, int aX, int aY, int aZ) {
		if (aRenderer.renderBlock(aBlock, aQB, aLevel, aX, aY, aZ)) return;
		boolean[] tSides = sides(aBlock, aRenderer instanceof IRenderedBlockObjectSideCheck ? (IRenderedBlockObjectSideCheck)aRenderer : null);
		// КОНТРАКТ setBlockBounds (1:1 renderWorldBlock, ветвь рендер-объекта :146): false → полный куб, не стухшие bounds.
		boolean tNeedsToSetBounds = true;
		for (int i = 0, j = aRenderer.getRenderPasses(aBlock, tSides); i < j; i++) {
			if (!aRenderer.usesRenderPass(i, tSides)) continue;
			if (aRenderer.setBlockBounds(aBlock, i, tSides)) {tNeedsToSetBounds = true;}
			else {if (tNeedsToSetBounds) gregapi.util.WD.setBlockBounds(aBlock, 0, 0, 0, 1, 1, 1); tNeedsToSetBounds = false;}
			applyBounds(aQB, aBlock);
			for (byte s = 0; s < 6; s++) face(aQB, aBlock, s, aRenderer.getTexture(aBlock, i, s, tSides), aX, aY, aZ);
		}
		if (tNeedsToSetBounds) gregapi.util.WD.setBlockBounds(aBlock, 0, 0, 0, 1, 1, 1); // анти-протечка общего блока (1:1 :158)
	}

	/** F3-render item-форма блока (3D-иконка в инвентаре) — ДОСЛОВНОЕ воспроизведение {@code RendererBlockTextured.renderInventoryBlock}
	 *  (референс gregtech6): либо TE-ветка через {@code passRenderingToObject(ItemStack)}→canonical-TE (MTE; level=null → дефолт-рендер),
	 *  либо block-level ветка {@code getRenderPasses(stack)/getTexture(pass,side,stack)} (руды/простые). SIDES_ITEM_RENDER = все грани true.
	 *  Тот же {@link #face}/{@link GT6QuadBuilder} — один центр рендера, как один RendererBlockTextured у Грегориуса. */
	public static void buildInventoryQuads(GT6QuadBuilder aQB, Block aBlock, net.minecraft.world.item.ItemStack aStack) {
		// F-bounds-race: item-форма блока строится на Render thread — тоже в bounds-контексте.
		boolean[] tCtx = gregapi.block.BlockBase.RENDER_BOUNDS_CTX.get(); boolean tPrevCtx = tCtx[0]; tCtx[0] = true;
		try {buildInventoryQuads0(aQB, aBlock, aStack);} finally {tCtx[0] = tPrevCtx;}
	}
	private static void buildInventoryQuads0(GT6QuadBuilder aQB, Block aBlock, net.minecraft.world.item.ItemStack aStack) {
		if (!(aBlock instanceof IRenderedBlock tRB)) return;
		// F3-render (приёмка 2026-07-30, «цветок в инвентаре без иконки»): item-форма cross-блока — те же две
		// скрещенные плоскости (1.7.10 renderBlockAsItem case 1 = drawCrossedSquares), а кубические каналы у
		// IRenderedCross — контрактные null (IRenderedCross:40-45) и давали ПУСТОЙ набор квадов. aWorld==null +
		// мета стека в aX — контракт getCrossIcon.
		if (tRB instanceof IRenderedCross tCross) {
			int tMeta = gregapi.util.ST.meta_(aStack);
			aQB.crossFace(tCross.getCrossIcon(null, tMeta, 0, 0), tCross.getCrossRGBa(null, tMeta, 0, 0));
			return;
		}
		boolean[] tSides = {true, true, true, true, true, true}; // SIDES_ITEM_RENDER (без соседей → все грани)
		IRenderedBlockObject tRenderer = tRB.passRenderingToObject(aStack);
		if (tRenderer != null) tRenderer = tRenderer.passRenderingToObject(aStack);
		// КОНТРАКТ setBlockBounds (1:1 renderInventoryBlock RendererBlockTextured:67/81): false → полный куб + анти-протечка.
		boolean tNeedsToSetBounds = true;
		if (tRenderer != null) {
			for (int i = 0, j = tRenderer.getRenderPasses(aBlock, tSides); i < j; i++) {
				if (!tRenderer.usesRenderPass(i, tSides)) continue;
				if (tRenderer.setBlockBounds(aBlock, i, tSides)) {tNeedsToSetBounds = true;}
				else {if (tNeedsToSetBounds) gregapi.util.WD.setBlockBounds(aBlock, 0, 0, 0, 1, 1, 1); tNeedsToSetBounds = false;}
				applyBounds(aQB, aBlock);
				for (byte s = 0; s < 6; s++) face(aQB, aBlock, s, tRenderer.getTexture(aBlock, i, s, tSides), 0, 0, 0);
			}
		} else {
			for (int i = 0, j = tRB.getRenderPasses(aStack); i < j; i++) {
				if (!tRB.usesRenderPass(i, aStack)) continue;
				if (tRB.setBlockBounds(i, aStack)) {tNeedsToSetBounds = true;}
				else {if (tNeedsToSetBounds) gregapi.util.WD.setBlockBounds(aBlock, 0, 0, 0, 1, 1, 1); tNeedsToSetBounds = false;}
				applyBounds(aQB, aBlock);
				for (byte s = 0; s < 6; s++) face(aQB, aBlock, s, tRB.getTexture(i, s, aStack), 0, 0, 0);
			}
		}
		if (tNeedsToSetBounds) gregapi.util.WD.setBlockBounds(aBlock, 0, 0, 0, 1, 1, 1); // 1:1 :92
	}

	/** tSides: у SideCheck-объекта — renderFullBlockSide; иначе все true (соседнее скрытие делает neo через addCulledFace). */
	private static boolean[] sides(Block aBlock, IRenderedBlockObjectSideCheck aCheck) {
		boolean[] r = {true, true, true, true, true, true};
		if (aCheck != null) for (byte s = 0; s < 6; s++) r[s] = aCheck.renderFullBlockSide(aBlock, null, s);
		return r;
	}

	/** Перенести текущие render-bounds блока (после setBlockBounds) в quad-builder (было RenderBlocks.setRenderBoundsFromBlock).
	 *  Чтение — через общий контракт IBlock.getRenderBounds: Block-иерархий GT6 ШЕСТЬ (BlockBase/BlockFluidBaseGT/
	 *  MultiTileEntityBlock/MultiTileEntityBlockInternal/BlockBaseRail/PrefixBlock, общего предка нет) — instanceof-цепочка
	 *  по классам теряла MTE (под-боксы пассов схлопывались в полный куб: LIVE-DEFECTS №2/№7). */
	private static void applyBounds(GT6QuadBuilder aQB, Block aBlock) {
		aQB.setBounds(aBlock instanceof gregapi.block.IBlock tI ? tI.getRenderBounds() : null);
	}

	/** Один per-side вызов ITexture (диспетчер по стороне) → GT6QuadBuilder аккумулирует грань. */
	private static void face(GT6QuadBuilder aQB, Block aBlock, byte aSide, ITexture aTex, int aX, int aY, int aZ) {
		if (aTex == null || !aTex.isValidTexture()) return;
		switch (aSide) {
		case 0: aTex.renderYNeg(aQB, aBlock, aX, aY, aZ, 240, false); break;
		case 1: aTex.renderYPos(aQB, aBlock, aX, aY, aZ, 240, false); break;
		case 2: aTex.renderZNeg(aQB, aBlock, aX, aY, aZ, 240, false); break;
		case 3: aTex.renderZPos(aQB, aBlock, aX, aY, aZ, 240, false); break;
		case 4: aTex.renderXNeg(aQB, aBlock, aX, aY, aZ, 240, false); break;
		case 5: aTex.renderXPos(aQB, aBlock, aX, aY, aZ, 240, false); break;
		}
	}

	/** Партиклы разрушения/удара (репорт игрока: ВСЕ GT-блоки крошатся error-текстурой) и MODCOMPAT-002
	 *  (JourneyMap при пустых квадах падает в particle-канал). Единая модель на весь мод отдавала статичный
	 *  mParticle=system/error; резолвим 1:1 с 1.7.10 EntityDiggingFX ({@code block.getIcon(0, meta)}): родной канал
	 *  getIcon у BlockBase-иерархии, текстура жидкости у fluid-блоков (обе жидкостные иерархии — BlockBaseFluid со
	 *  своей текстурой и BlockWaterlike с ванильной водой — отвечают по тому же КОНТРАКТУ IBlock.getIcon, поэтому
	 *  ветка одна, а не по иерархиям); MTE/нет иконки → CFOAM (1:1-дефолт 1.7.10 BlockBase.getIcon:103 и
	 *  MultiTileEntityBlock.getIcon:293 — серая CFoam-крошка, НЕ error-текстура).
	 *  <p>Ветка 1.20.1: движок спрашивает иконку крошки БЕЗ позиции ({@code TerrainParticle:26} →
	 *  {@code BlockModelShaper.getParticleIcon(state)} → {@code getParticleIcon(ModelData.EMPTY)}), поэтому мету берём
	 *  из BlockState владельца — тем же центром {@code IBlockExtendedMetaData}, которым её берёт источник тинта GT6
	 *  ({@code GT_API_Proxy_Client.GT6BlockTint}), а не своим разбором свойств. */
	@Override
	public TextureAtlasSprite getParticleIcon(ModelData aData) {
		try {
			if (mOwner instanceof gregapi.block.IBlock tGT6) {
				int tMeta = mOwnerState != null && mOwner instanceof gregapi.block.IBlockExtendedMetaData tMetaBlock ? tMetaBlock.getExtendedMetaData(mOwnerState) : 0;
				ResourceLocation tIcon = tGT6.getIcon(0, tMeta);
				if (tIcon == null) tIcon = gregapi.old.Textures.BlockIcons.CFOAM_HARDENED.getIcon(0);
				if (tIcon != null) {
					TextureAtlasSprite tSprite = GT6QuadBuilder.resolveSprite(tIcon);
					if (tSprite != null) return tSprite;
				}
			}
		} catch (Throwable e) {/* партикл не рушит рендер */}
		return errorParticle();
	}
}

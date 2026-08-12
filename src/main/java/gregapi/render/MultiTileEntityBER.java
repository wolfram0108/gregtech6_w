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
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;

import gregapi.block.multitileentity.MultiTileEntityBlock;
import gregapi.tileentity.base.TileEntityBase01Root;

/**
 * F3-render КОРЕНЬ прозрачности MTE (машины/трубы/декор): geometry MTE-блоков живёт на клиент-BE (IRenderedBlockObject:
 * getRenderPasses/getTexture/box), а регион чанк-компиляции (worker-снапшот) BE НЕ отдаёт (доказано probe:
 * getBlockEntity=null 100%). Потому MTE рисуются НЕ через baked {@link GT6BlockModel} (он их пропускает), а через
 * ЭТОТ {@link BlockEntityRenderer}: {@link #render} на main-thread берёт ЖИВОЙ BE и строит quads той же логикой GT6
 * ({@link GT6BlockModel#buildRendererQuads}, переиспользование 1:1). Один generic BER на весь {@code MTE_TYPE} —
 * централизация 1:1 (аналог единого GT6-рендерера). Руды ({@code PrefixBlockTileEntity}) и стабы отсеиваются гейтом
 * (их рисует baked-модель).
 *
 * <p><b>Ветка 1.20.1.</b> BER здесь однопараметрический и БЕЗ промежуточного render-state: движок зовёт
 * {@code render(be, partialTick, PoseStack, MultiBufferSource, light, overlay)} ({@code BlockEntityRenderer.java:12}),
 * то есть сбор геометрии и её выдача снова в одном вызове, как в 1.7.10 TESR. Два следствия:
 * <ul>
 *   <li>рамка отсечения объявляется не рендерером, а САМИМ BE — {@code IForgeBlockEntity.getRenderBoundingBox()}
 *       ({@code IForgeBlockEntity.java:105}); BUG-063 живёт там ({@code TileEntityBase01Root.getRenderBoundingBox});</li>
 *   <li>трещины на живой геометрии эмитить вручную НЕ нужно: движок сам оборачивает буфер BE декалью
 *       ({@code LevelRenderer.java:1246-1257} — {@code SheetedDecalTextureGenerator} для типов с
 *       {@code affectsCrumbling()}, а {@code Sheets.cutoutBlockSheet()} = {@code entityCutout}, у которого этот
 *       флаг {@code true}: {@code RenderType.java:43-46}). Класса дефекта «трещин нет» здесь не существует.</li>
 * </ul>
 */
public class MultiTileEntityBER implements BlockEntityRenderer<TileEntityBase01Root> {

	public MultiTileEntityBER(BlockEntityRendererProvider.Context aContext) {/* per-BE геометрия строится в render; ресурсы контекста тут не нужны */}

	/** F3-render дистанция (репорт игрока: MTE «пропадают вдалеке»): дефолт BER = 64 блока, но в 1.7.10 MTE были
	 *  chunk-геометрией и рисовались на ВСЮ дистанцию прорисовки. 1:1 по следствию: радиус = renderDistance чанков. */
	@Override public int getViewDistance() {
		return net.minecraft.client.Minecraft.getInstance().options.renderDistance().get() * 16;
	}

	// F3-render спец-рендеры (1.7.10 ClientRegistry.bindTileEntitySpecialRenderer = vanilla-диспетчер по КЛАССУ TE;
	// здесь BER регистрируется по BlockEntityType, а у всех MTE он ОДИН — MTE_TYPE) → диспетч по классу живёт здесь,
	// в едином BER: реестр класс→рендерер, render делегируется. Оба живых TESR GT6 (Chest/MassStorage) идут сюда.
	@SuppressWarnings("rawtypes")
	private static final java.util.Map<Class<?>, BlockEntityRenderer> SPECIAL_RENDERERS = new java.util.HashMap<>();
	public static void bindSpecialRenderer(Class<?> aTileEntityClass, @SuppressWarnings("rawtypes") BlockEntityRenderer aRenderer) {SPECIAL_RENDERERS.put(aTileEntityClass, aRenderer);}

	/** Диаг-счётчики судьи П2 (спец-рендер реально вызван движком). */
	public static final java.util.concurrent.atomic.AtomicLong sSpecialSubmit = new java.util.concurrent.atomic.AtomicLong(), sSpecialItemForm = new java.util.concurrent.atomic.AtomicLong();
	/** Диаг-счётчики кэша квадов (BUG-106 №4): вызовы рендера рендер-объектов / реальные пересборки / кэш-хиты. */
	public static final java.util.concurrent.atomic.AtomicLong sQuadExtracts = new java.util.concurrent.atomic.AtomicLong(), sQuadBuilds = new java.util.concurrent.atomic.AtomicLong(), sQuadCacheHits = new java.util.concurrent.atomic.AtomicLong();

	/** BUG-106 №4 — кэш квадов BER. Эпоха рендера: {@code allChanged()} (перешив атласа/моделей — F3+T, F3+A,
	 *  смена дистанции; кэшированные квады держат UV СТАРОГО атласа) рвёт ВСЕ кэши разом, O(1). Точечный сброс —
	 *  {@link #onSectionDirty}: та же воронка, которой движок помечает секции на перестройку, то есть ровно тот
	 *  сигнал, по которому в 1.7.10 пересобирался мэш с геометрией MTE (recompSrc RenderGlobal.markBlockForUpdate →
	 *  markBlockRangeForRenderUpdate ±1). Всё общение клиента о смене облика уже проходит через неё:
	 *  каждый receiveData*-диспетчер ({@code MultiTileEntityBlock:265-325}) кончается {@code WD.update} →
	 *  {@code ClientLevel.sendBlockUpdated} → {@code LevelRenderer.blockChanged} → setSectionDirty;
	 *  прямые setBlock и свет — туда же. Залипание кэша возможно лишь там, где залипал бы и мэш 1.7.10 — 1:1 по следствию. */
	public static long sQuadEpoch = 0;

	/** Сброс кэша квадов у всех MTE секции (зовёт MixinLevelRenderer из setSectionDirty, main-thread). */
	public static void onSectionDirty(net.minecraft.client.multiplayer.ClientLevel aLevel, int aSectionX, int aSectionY, int aSectionZ) {
		net.minecraft.world.level.chunk.LevelChunk tChunk = gregapi.util.WD.chunkNow(aLevel, aSectionX, aSectionZ);
		if (tChunk == null) return;
		int tMinY = aSectionY << 4, tMaxY = tMinY + 15;
		for (java.util.Map.Entry<BlockPos, net.minecraft.world.level.block.entity.BlockEntity> tEntry : tChunk.getBlockEntities().entrySet()) {
			int tY = tEntry.getKey().getY();
			if (tY >= tMinY && tY <= tMaxY && tEntry.getValue() instanceof TileEntityBase01Root tBE) tBE.mQuadCacheEpoch = Long.MIN_VALUE;
		}
	}

	/** Полный сброс (зовёт MixinLevelRenderer из allChanged, main-thread). */
	public static void onRenderAllChanged() {sQuadEpoch++;}

	public static boolean hasSpecialRenderer(Class<?> aTileEntityClass) {return SPECIAL_RENDERERS.containsKey(aTileEntityClass);}

	/** Item-форма TESR-классов: 1.7.10 renderItem звал renderTileEntityAt(this,0,0,0,0) на canonical-TE (данные из NBT
	 *  стека); носитель 1.20.1 — {@code BlockEntityWithoutLevelRenderer}, куда движок уходит, когда item-модель
	 *  объявила {@code isCustomRenderer()} ({@code ItemRenderer.render} → {@code IClientItemExtensions.getCustomRenderer()}).
	 *  Внутри — тот же зарегистрированный спец-рендерер (диспетч по классу), рисующий detached-BE. */
	/** Мост из common-класса предмета MTE ({@code MultiTileEntityItemInternal.initializeClient}) — тот же приём, что
	 *  {@code MTEChestRenderer.bindFirst}: клиентский код живёт в клиентском файле (BUG-092), common-класс зовёт его
	 *  ленивым invokestatic. Через этот канал движок и находит BEWLR item-формы. */
	public static void bindItemExtensions(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> aConsumer) {
		aConsumer.accept(new net.minecraftforge.client.extensions.common.IClientItemExtensions() {
			@Override public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() {return specialItemForm();}
		});
	}

	private static SpecialItemForm sItemForm;
	/** Ленивая выдача BEWLR: конструктор {@code BlockEntityWithoutLevelRenderer} требует живых узлов Minecraft,
	 *  а класс BER грузится раньше их готовности — инстанс создаём при первом обращении с клиента. */
	public static SpecialItemForm specialItemForm() {
		if (sItemForm == null) sItemForm = new SpecialItemForm();
		return sItemForm;
	}

	/** BEWLR item-формы MTE со спец-рендером. Отдельный класс, потому что 1.20.1 требует наследника
	 *  {@link net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer}, а не анонимного интерфейса. */
	public static final class SpecialItemForm extends net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer {
		private SpecialItemForm() {
			super(net.minecraft.client.Minecraft.getInstance().getBlockEntityRenderDispatcher(), net.minecraft.client.Minecraft.getInstance().getEntityModels());
		}

		@Override
		@SuppressWarnings("unchecked")
		public void renderByItem(net.minecraft.world.item.ItemStack aStack, net.minecraft.world.item.ItemDisplayContext aContext, PoseStack aPoseStack, MultiBufferSource aBuffer, int aLight, int aOverlay) {
			net.minecraft.world.level.block.entity.BlockEntity tBE = extractSpecialItemForm(aStack);
			if (tBE == null) return;
			@SuppressWarnings("rawtypes") BlockEntityRenderer tRenderer = SPECIAL_RENDERERS.get(tBE.getClass());
			if (tRenderer == null) return;
			try {
				tRenderer.render(tBE, 0F, aPoseStack, aBuffer, aLight, aOverlay);
				sSpecialItemForm.incrementAndGet();
			} catch (Throwable e) {/* item-форма не должна ронять рендер */}
		}

	}

	/** Стек → canonical-TE со спец-рендером (или null). Тот же центр рождения detached-TE, что у обычного item-рендера.
	 *  Статический: item-модель спрашивает «нужен ли спец-рендер» ДО того, как BEWLR понадобится. */
	public static net.minecraft.world.level.block.entity.BlockEntity extractSpecialItemForm(net.minecraft.world.item.ItemStack aStack) {
		try {
				if (aStack.getItem() instanceof gregapi.block.multitileentity.MultiTileEntityItemInternal tMTE) {
					gregapi.block.multitileentity.MultiTileEntityContainer tCont = tMTE.mBlock.mMultiTileEntityRegistry.getNewTileEntityContainer(aStack);
					// BUG-078: ВТОРОЙ путь рождения detached-TE (предметы со своим рендерером: сундук, масстораж).
					// Компенсация item-facing берётся из того же центра, что и у обычного item-рендера.
					if (tCont != null && tCont.mTileEntity != null && SPECIAL_RENDERERS.containsKey(tCont.mTileEntity.getClass())) return gregapi.block.multitileentity.MultiTileEntityRegistry.applyItemFacing(tCont.mTileEntity);
			}
		} catch (Throwable e) {/**/}
		return null;
	}

	@Override
	public void render(TileEntityBase01Root aBE, float aPartialTicks, PoseStack aPoseStack, MultiBufferSource aBuffer, int aLight, int aOverlay) {
		Block tBlock = aBE.getBlockState().getBlock();
		// Только MTE-блоки с render-объектом: руды(PrefixBlock/PrefixBlockTileEntity) и стабы(TileEntityLoaderStub,
		// render-данных нет) → baked/пусто. BUG-063: рамку отсечения гейт-отсеянным ставим ЗДЕСЬ — иначе она
		// навсегда осталась бы неизвестной, а неизвестная = «не отсекать». Их геометрия — обычный куб блока.
		if (aBE.getLevel() == null || !(aBE instanceof IRenderedBlockObject tRenderer) || !(tBlock instanceof MultiTileEntityBlock)) {
			aBE.mRenderAABB = new net.minecraft.world.phys.AABB(aBE.getBlockPos());
			return;
		}
		sQuadExtracts.incrementAndGet();
		List<BakedQuad> tQuads;
		// BUG-106 №4: кэш-хит — облик с кадра построения не менялся (сигнала setSectionDirty не было, см. sQuadEpoch
		// выше). mRenderAABB остаётся от кадра построения, спец-рендер (аниматика TESR) идёт живым ниже, как всегда.
		if (aBE.mQuadCacheEpoch == sQuadEpoch) {
			tQuads = aBE.mQuadCache;
			sQuadCacheHits.incrementAndGet();
		} else {
			aBE.mRenderAABB = new net.minecraft.world.phys.AABB(aBE.getBlockPos());
			BlockPos tPos = aBE.getBlockPos();
			GT6QuadBuilder tQB = new GT6QuadBuilder();
			try { GT6BlockModel.buildRendererQuads(tQB, tRenderer, tBlock, aBE.getLevel(), tPos.getX(), tPos.getY(), tPos.getZ()); } catch (Throwable e) {/* render-логика конкретного MTE не должна ронять кадр */}
			tQuads = tQB.isEmpty() ? null : tQB.quads();
			// BUG-063: рамка = ФАКТИЧЕСКИ нарисованное этим BE (quads строятся в локальных координатах блока → сдвигаем в мир).
			float[] tDrawn = tQB.drawnBounds();
			if (tDrawn != null) aBE.mRenderAABB = new net.minecraft.world.phys.AABB(
				  tPos.getX() + Math.min(tDrawn[0], 0F), tPos.getY() + Math.min(tDrawn[1], 0F), tPos.getZ() + Math.min(tDrawn[2], 0F)
				, tPos.getX() + Math.max(tDrawn[3], 1F), tPos.getY() + Math.max(tDrawn[4], 1F), tPos.getZ() + Math.max(tDrawn[5], 1F));
			aBE.mQuadCache = tQuads; // null = «квадов нет» — тоже кэшируется (валидность судит эпоха)
			aBE.mQuadCacheEpoch = sQuadEpoch;
			sQuadBuilds.incrementAndGet();
		}
		if (tQuads != null && !tQuads.isEmpty()) {
			// quads GT6QuadBuilder — уже в локальных координатах блока 0..1 (как baked-модель); PoseStack на входе
			// уже сдвинут в позицию блока (LevelRenderer:1244). cutoutBlockSheet несёт affectsCrumbling=true —
			// трещины разрушения движок домешивает сам (см. javadoc класса), вручную их эмитить не нужно.
			VertexConsumer tConsumer = aBuffer.getBuffer(Sheets.cutoutBlockSheet());
			PoseStack.Pose tPose = aPoseStack.last();
			for (BakedQuad tQuad : tQuads) tConsumer.putBulkData(tPose, tQuad, 1F, 1F, 1F, aLight, aOverlay);
		}
		@SuppressWarnings("rawtypes") BlockEntityRenderer tSpecial = SPECIAL_RENDERERS.get(aBE.getClass());
		if (tSpecial != null) try {
			tSpecial.render(aBE, aPartialTicks, aPoseStack, aBuffer, aLight, aOverlay);
			sSpecialSubmit.incrementAndGet();
		} catch (Throwable e) {/* спец-рендер не должен ронять кадр */}
	}
}

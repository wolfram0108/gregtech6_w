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

import com.mojang.serialization.MapCodec;

import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.SimpleModelWrapper;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.client.resources.model.sprite.MaterialBaker;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.model.DynamicBlockStateModel;
import net.neoforged.neoforge.client.model.block.CustomUnbakedBlockStateModel;

/**
 * F3-render (client): единая динамическая модель ВСЕХ GT6-блоков (аналог одного {@code RendererBlockTextured} у Грегориуса —
 * централизация 1:1). neo зовёт {@link #collectParts} → берём {@link IRenderedBlock} из блока → per pass×side зовём его
 * {@code getTexture(...)} → {@link ITexture}{@code .render<Side>(quadBuilder,...)} → {@link GT6QuadBuilder} аккумулирует quads
 * → {@code parts.add(SimpleModelWrapper)}. GT6 per-side texture-логика переиспользуется без переписывания; заменён лишь
 * механизм отрисовки (immediate→baked). Спрайты — из block-атласа в рантайме (динамика материал×префикс). Регистрация типа —
 * {@code RegisterBlockStateModels} (GT_API_Proxy_Client); blockstate-JSON блоков ссылаются на этот тип (датаген). См. F3-render.md §2.
 */
public class GT6BlockModel implements DynamicBlockStateModel {
	private final Material.Baked mParticle;

	GT6BlockModel(MaterialBaker aBaker) {
		net.minecraft.client.resources.model.ModelDebugName tDebugName = getClass()::toString;
		// sprite-id без blocks/ префикса: atlas-source (assets/minecraft/atlases/blocks.json) кладёт textures/blocks/** с prefix:"" → gregtech:system/error.
		mParticle = aBaker.get(new Material(Identifier.fromNamespaceAndPath("gregtech", "system/error")), tDebugName);
	}

	/** Путь ModelEvent.ModifyBakingResult: particle из готового спрайта (событие даёт textureGetter, не MaterialBaker). */
	public GT6BlockModel(Material.Baked aParticle) {mParticle = aParticle;}

	@Override
	public void collectParts(BlockAndTintGetter aLevel, BlockPos aPos, BlockState aState, RandomSource aRandom, List<BlockStateModelPart> aParts) {
		if (!(aState.getBlock() instanceof IRenderedBlock tRB)) return;
		Block tBlock = aState.getBlock();
		int tX = aPos.getX(), tY = aPos.getY(), tZ = aPos.getZ();
		GT6QuadBuilder tQB = new GT6QuadBuilder();

		// F3-render cross-модель (растения/цветы, IRenderedCross): X-форма из 2 диагональных плоскостей, минуя кубическую цепочку.
		if (tRB instanceof IRenderedCross tCross) {
			tQB.crossFace(tCross.getCrossIcon(aLevel, tX, tY, tZ), tCross.getCrossRGBa(aLevel, tX, tY, tZ));
			aParts.add(new SimpleModelWrapper(tQB.build(), true, mParticle));
			return;
		}

		// F3-fluid: жидкости-блоки (нефти/газ/гео-вода) — 1:1 порт RendererBlockFluid.renderWorldBlock:
		// кванта-высота, склоны угловых высот по соседям 3×3 (смыкают уровни без дыр), газ зеркально от потолка.
		// Вместо box-пути IRenderedBlock (тот оставлен для item-формы).
		if (tBlock instanceof gregapi.block.fluid.BlockBaseFluid tFluid) {
			RendererBlockFluid.collectFluidQuads(tQB, aLevel, tX, tY, tZ, tFluid);
			aParts.add(new SimpleModelWrapper(tQB.build(), true, mParticle));
			return;
		}

		// F3-render: MTE-блоки рисует BER (MultiTileEntityBER), НЕ baked-модель. Причина (probe, окончательно): neo section-compile
		// регион (worker-снапшот) НЕ отдаёт MTE-BE (getBlockEntity=null 100%) → тут геометрию собрать нельзя. BER берёт живой BE на
		// main-thread. Пропускаем (пустой меш; часть MTE регион случайно захватывал — рисовали бы дважды с BER). Флюид/BlockBase — ниже (render на блоке).
		if (tBlock instanceof gregapi.block.multitileentity.MultiTileEntityBlock) {aParts.add(new SimpleModelWrapper(tQB.build(), true, mParticle)); return;}

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
			for (int i = 0, j = tRB.getRenderPasses(aLevel, tX, tY, tZ, tSides); i < j; i++) {
				if (!tRB.usesRenderPass(i, aLevel, tX, tY, tZ, tSides)) continue;
				tRB.setBlockBounds(i, aLevel, tX, tY, tZ, tSides);
				applyBounds(tQB, tBlock);
				for (byte s = 0; s < 6; s++) face(tQB, tBlock, s, tRB.getTexture(i, s, tSides, aLevel, tX, tY, tZ), tX, tY, tZ);
			}
			tQB.clearUVRotate(); // 1:1 renderBlockLog: сброс uvRotate* после renderStandardBlock
		} else {
			buildRendererQuads(tQB, tRenderer, tBlock, aLevel, tX, tY, tZ);
		}
		aParts.add(new SimpleModelWrapper(tQB.build(), true, mParticle));
	}

	/** F3-render: ветвь рендер-объекта (getRenderPasses→setBlockBounds→getTexture→quads). Общий код collectParts (baked) и
	 *  MultiTileEntityBER (BER, живой BE на main-thread). renderBlock=true → объект сам нарисовал, цикл не нужен. */
	public static void buildRendererQuads(GT6QuadBuilder aQB, IRenderedBlockObject aRenderer, Block aBlock, net.minecraft.world.level.BlockGetter aLevel, int aX, int aY, int aZ) {
		if (aRenderer.renderBlock(aBlock, aQB, aLevel, aX, aY, aZ)) return;
		boolean[] tSides = sides(aBlock, aRenderer instanceof IRenderedBlockObjectSideCheck ? (IRenderedBlockObjectSideCheck)aRenderer : null);
		for (int i = 0, j = aRenderer.getRenderPasses(aBlock, tSides); i < j; i++) {
			if (!aRenderer.usesRenderPass(i, tSides)) continue;
			aRenderer.setBlockBounds(aBlock, i, tSides);
			applyBounds(aQB, aBlock);
			for (byte s = 0; s < 6; s++) face(aQB, aBlock, s, aRenderer.getTexture(aBlock, i, s, tSides), aX, aY, aZ);
		}
	}

	/** F3-render item-форма блока (3D-иконка в инвентаре) — ДОСЛОВНОЕ воспроизведение {@code RendererBlockTextured.renderInventoryBlock}
	 *  (референс gregtech6): либо TE-ветка через {@code passRenderingToObject(ItemStack)}→canonical-TE (MTE; level=null → дефолт-рендер),
	 *  либо block-level ветка {@code getRenderPasses(stack)/getTexture(pass,side,stack)} (руды/простые). SIDES_ITEM_RENDER = все грани true.
	 *  Тот же {@link #face}/{@link GT6QuadBuilder} — один центр рендера, как один RendererBlockTextured у Грегориуса. */
	public static void buildInventoryQuads(GT6QuadBuilder aQB, Block aBlock, net.minecraft.world.item.ItemStack aStack) {
		if (!(aBlock instanceof IRenderedBlock tRB)) return;
		boolean[] tSides = {true, true, true, true, true, true}; // SIDES_ITEM_RENDER (без соседей → все грани)
		IRenderedBlockObject tRenderer = tRB.passRenderingToObject(aStack);
		if (tRenderer != null) tRenderer = tRenderer.passRenderingToObject(aStack);
		if (tRenderer != null) {
			for (int i = 0, j = tRenderer.getRenderPasses(aBlock, tSides); i < j; i++) {
				if (!tRenderer.usesRenderPass(i, tSides)) continue;
				tRenderer.setBlockBounds(aBlock, i, tSides);
				applyBounds(aQB, aBlock);
				for (byte s = 0; s < 6; s++) face(aQB, aBlock, s, tRenderer.getTexture(aBlock, i, s, tSides), 0, 0, 0);
			}
		} else {
			for (int i = 0, j = tRB.getRenderPasses(aStack); i < j; i++) {
				if (!tRB.usesRenderPass(i, aStack)) continue;
				tRB.setBlockBounds(i, aStack);
				applyBounds(aQB, aBlock);
				for (byte s = 0; s < 6; s++) face(aQB, aBlock, s, tRB.getTexture(i, s, aStack), 0, 0, 0);
			}
		}
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

	@Override
	public Material.Baked particleMaterial() {return mParticle;}

	@Override
	public int materialFlags() {return 0;}

	/** Unbaked-тип модели для регистрации (RegisterBlockStateModels). blockstate-JSON: {@code {"model":{"type":"gregtech:gt6block"}}}. */
	public record Unbaked() implements CustomUnbakedBlockStateModel {
		public static final Identifier ID = Identifier.fromNamespaceAndPath("gregtech", "gt6block");
		public static final MapCodec<Unbaked> MAP_CODEC = MapCodec.unit(Unbaked::new);
		@Override public BlockStateModel bake(ModelBaker aBaker) {return new GT6BlockModel(aBaker.materials());}
		@Override public void resolveDependencies(Resolver aResolver) {}
		@Override public MapCodec<Unbaked> codec() {return MAP_CODEC;}
	}
}

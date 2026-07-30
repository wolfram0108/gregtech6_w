/**
 * Copyright (c) 2021 GregTech-6 Team
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

package gregapi.block.metatype;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.Direction;

import static gregapi.data.CS.*;

import java.util.List;
import java.util.Random;

import net.neoforged.api.distmarker.Dist;
import gregapi.block.BlockBaseMeta;
import gregapi.data.OP;
import gregapi.data.RM;
import gregapi.oredict.OreDictMaterial;
import gregapi.render.IIconContainer;
import gregapi.util.CR;
import gregapi.util.ST;
import gregapi.util.UT;
import gregapi.util.WD;
import net.minecraft.world.level.block.Block;
import gregapi.block.Material;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;

public class BlockMetaType extends BlockBaseMeta implements net.minecraft.world.level.block.SimpleWaterloggedBlock {
	public final float mHardnessMultiplier, mResistanceMultiplier;
	public final int mHarvestLevel;
	public final byte mSide, mOctantcount;
	public final boolean mIsWall, mIsSlab, mIsStair, mIsPrimary;
	public final BlockMetaType mBlock;
	public final BlockMetaType[] mSlabs;
	/** BUG-010-мелочь (реестр отложенного №3): WATERLOGGED должен быть ТОЛЬКО у слэбов, но property добавляется в
	 *  createBlockStateDefinition, вызываемом из super() ДО инициализации полей (mIsSlab ещё неизвестен). Флаг-контекст
	 *  выставляется вокруг конструирования слэбов (mSlabs-массив ниже) — единственный путь создания слэбов. */
	private static final ThreadLocal<boolean[]> SLAB_CTOR_CTX = ThreadLocal.withInitial(() -> new boolean[1]);
	
	public BlockMetaType(Class<? extends BlockItem> aItemClass, Material aVanillaMaterial, SoundType aSoundType, String aNameInternal, String aDefaultLocalised, OreDictMaterial aMaterial, float aResistanceMultiplier, float aHardnessMultiplier, int aHarvestLevel, int aCount, IIconContainer[] aIcons) {
		super(aItemClass == null ? ItemBlockMetaType.class : aItemClass, aNameInternal, aVanillaMaterial, aSoundType, aCount, aIcons);
		if (aItemClass == null) aItemClass = ItemBlockMetaType.class;
		onBlockCreation(aItemClass, aVanillaMaterial, aSoundType, aNameInternal, aDefaultLocalised, aMaterial, aResistanceMultiplier, aHardnessMultiplier, aHarvestLevel, aCount, aIcons);
		// F12-hardness: 1.7.10 setHardness/setResistance (runtime мутаторы после super) — neo Properties.strength неизменяема.
		// Заменены: getBlockHardness (WD.hardness(STONE)*mHardnessMultiplier) → neo через BlockBase.getDestroyProgress;
		// getExplosionResistance(byte) → neo через BlockBase.getExplosionResistance(BlockState,...). Оба подключены 1:1, значение активно.
		gregapi.item.CreativeTabsGT.assign(this, gregapi.item.CreativeTabsGT.BLOCK);
		mIsWall = F;
		mIsSlab = F;
		mIsStair = F;
		mIsPrimary = T;
		mBlock = this;
		mOctantcount = 8;
		mSide = SIDE_UNKNOWN;
		mHarvestLevel = aHarvestLevel;
		mHardnessMultiplier = aHardnessMultiplier;
		mResistanceMultiplier = aResistanceMultiplier;
		boolean[] tSlabCtx = SLAB_CTOR_CTX.get(); tSlabCtx[0] = true; // WATERLOGGED — только слэбам (см. SLAB_CTOR_CTX)
		try {
			mSlabs = new BlockMetaType[] {
			  makeSlab(aItemClass, aVanillaMaterial, aSoundType, aNameInternal, aDefaultLocalised, aMaterial, aResistanceMultiplier / 2, aHardnessMultiplier / 2, aHarvestLevel, maxMeta(), aIcons, SIDE_DOWN    , this)
			, makeSlab(aItemClass, aVanillaMaterial, aSoundType, aNameInternal, aDefaultLocalised, aMaterial, aResistanceMultiplier / 2, aHardnessMultiplier / 2, aHarvestLevel, maxMeta(), aIcons, SIDE_UP      , this)
			, makeSlab(aItemClass, aVanillaMaterial, aSoundType, aNameInternal, aDefaultLocalised, aMaterial, aResistanceMultiplier / 2, aHardnessMultiplier / 2, aHarvestLevel, maxMeta(), aIcons, SIDE_NORTH   , this)
			, makeSlab(aItemClass, aVanillaMaterial, aSoundType, aNameInternal, aDefaultLocalised, aMaterial, aResistanceMultiplier / 2, aHardnessMultiplier / 2, aHarvestLevel, maxMeta(), aIcons, SIDE_SOUTH   , this)
			, makeSlab(aItemClass, aVanillaMaterial, aSoundType, aNameInternal, aDefaultLocalised, aMaterial, aResistanceMultiplier / 2, aHardnessMultiplier / 2, aHarvestLevel, maxMeta(), aIcons, SIDE_WEST    , this)
			, makeSlab(aItemClass, aVanillaMaterial, aSoundType, aNameInternal, aDefaultLocalised, aMaterial, aResistanceMultiplier / 2, aHardnessMultiplier / 2, aHarvestLevel, maxMeta(), aIcons, SIDE_EAST    , this)
			, null};
		} finally {tSlabCtx[0] = false;}
		mSlabs[SIDE_INVALID] = mSlabs[SIDE_DOWN];
		// F12-followup (block-split): слэбы созданы ВНУТРИ конструктора (makeSlab), а не через call-site registerBlockLazy —
		// их neo-Block-реестр никто не регистрирует → «intrusive holders were not registered» на freeze. Регистрируем каждый
		// уникальный слэб напрямую в реестр (конструкция идёт на RegisterEvent<Block>, реестр разморожен); ключ = setId слэба
		// (GAPI:санитизированное имя). BlockItem слэба уже зарегистрирован его BlockBase-конструктором (registerItemLazy).
		{
			java.util.Set<Object> tSeenSlabs = new java.util.HashSet<>();
			for (BlockMetaType tSlab : mSlabs) if (tSlab != null && tSeenSlabs.add(tSlab)) {
				net.minecraft.core.Registry.register(net.minecraft.core.registries.BuiltInRegistries.BLOCK, net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.BLOCK, net.minecraft.resources.Identifier.fromNamespaceAndPath(gregapi.data.CS.ModIDs.GT, gregapi.GT_API.sanitizeRegName(tSlab.mNameInternal))), tSlab);
			}
		}
		// F12-followup (block-split): конструкция блока идёт на RegisterEvent (реестр разморожен), но ST.hide/ST.make/рецепты
		// создают ItemStack → компоненты связаны только на server-start. Откладываем эту дата-часть в deferItemInit (1:1 порядок).
		gregapi.GT_API.deferItemInit(() -> {
		ST.hide(mSlabs[SIDE_UP]);
		ST.hide(mSlabs[SIDE_NORTH]);
		ST.hide(mSlabs[SIDE_SOUTH]);
		ST.hide(mSlabs[SIDE_WEST]);
		ST.hide(mSlabs[SIDE_EAST]);
		for (byte i = 0; i < 16; i++) {
			CR.shaped(ST.make(this, 1, i), CR.DEF, "X", "X", 'X', ST.make(mSlabs[0], 1, i));
			// Avoid duplicating Recipes that are added by the Wood Dictionary anyways.
			if (!(this instanceof BlockBasePlanks)) {
				RM.sawing(16, 16, F, 5, ST.make(this, 1, i), ST.make(mSlabs[0], 2, i));
				CR.shaped(ST.make(mSlabs[0], 2, i), CR.DEF, "sX", 'X', ST.make(this, 1, i));
			}
		}
		if (COMPAT_FR != null) gregapi.GT_API.deferItemInit(() -> COMPAT_FR.addToBackpacks("builder", ST.make(this, 1, W)));
		});
	}
	
	protected BlockMetaType makeSlab(Class<? extends BlockItem> aItemClass, Material aVanillaMaterial, SoundType aVanillaSoundType, String aName, String aDefaultLocalised, OreDictMaterial aMaterial, float aResistanceMultiplier, float aHardnessMultiplier, int aHarvestLevel, int aCount, IIconContainer[] aIcons, byte aSlabType, BlockMetaType aBlock) {
		return new BlockMetaType(aItemClass, aVanillaMaterial, aVanillaSoundType, aName, aDefaultLocalised, aMaterial, aResistanceMultiplier, aHardnessMultiplier, aHarvestLevel, aCount, aIcons, aSlabType, aBlock);
	}
	
	protected BlockMetaType(Class<? extends BlockItem> aItemClass, Material aVanillaMaterial, SoundType aSoundType, String aName, String aDefaultLocalised, OreDictMaterial aMaterial, float aResistanceMultiplier, float aHardnessMultiplier, int aHarvestLevel, int aCount, IIconContainer[] aIcons, byte aSlabType, BlockMetaType aBlock) {
		super(aItemClass == null ? ItemBlockMetaType.class : aItemClass, aName+".slab."+aSlabType, aVanillaMaterial, aSoundType, aCount, aIcons);
		if (aItemClass == null) aItemClass = ItemBlockMetaType.class;
		onSlabCreation(aItemClass, aVanillaMaterial, aSoundType, aName, aDefaultLocalised, aMaterial, aResistanceMultiplier, aHardnessMultiplier, aHarvestLevel, aCount, aIcons, aSlabType, aBlock);
		// F12-hardness: см. основной конструктор выше — getBlockHardness/getExplosionResistance подключены к neo (getDestroyProgress), 1:1.
		mIsWall = F;
		mIsSlab = T;
		mIsStair = F;
		mIsPrimary = (aSlabType == 0);
		mBlock = aBlock;
		mOctantcount = 4;
		mSide = aSlabType;
		mHarvestLevel = aHarvestLevel;
		mHardnessMultiplier = aHardnessMultiplier;
		mResistanceMultiplier = aResistanceMultiplier;
		mSlabs = null;
		registerDefaultState(defaultBlockState().setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED, Boolean.FALSE)); // BUG-010: явный дефолт (any() не гарантирует false)
		setBlockBounds(
		mSide == SIDE_X_POS ? 0.5F : 0.0F,
		mSide == SIDE_Y_POS ? 0.5F : 0.0F,
		mSide == SIDE_Z_POS ? 0.5F : 0.0F,
		mSide == SIDE_X_NEG ? 0.5F : 1.0F,
		mSide == SIDE_Y_NEG ? 0.5F : 1.0F,
		mSide == SIDE_Z_NEG ? 0.5F : 1.0F
		);
		// F12-followup (block-split): ST.make (ItemStack) — компоненты только на server-start → deferItemInit.
		gregapi.GT_API.deferItemInit(() -> {if (COMPAT_FR != null) COMPAT_FR.addToBackpacks("builder", ST.make(this, 1, W));});
	}
	
	// BUG-010 (слэб «иногда полный куб», гоночный): bounds слэба СТАТИЧНЫ (формула конструктора :137-144), но
	// setBlockBounds(pass)=true у BlockBaseMeta заявлял «bounds выставлены», НИЧЕГО не выставляя, а анти-протечка
	// рендера (GT6BlockModel:142/164/195 — 1:1 контракт «false → сброс в полный куб» ОБЩЕГО Block-инстанса) сбрасывает
	// поля после КАЖДОГО мешинга → следующий мешинг рисует слэб кубом (замер gt6slabprobe: те же 6 state'ов в одних
	// прогонах полублоки, в других — кубы). В 1.7.10 поля Block никто не сбрасывал (статичный конструкторный bounds
	// жил вечно). Фикс: слэб ПЕРЕВЫСТАВЛЯЕТ свои статичные bounds на каждом рендер-пассе (блок- и item-форма).
	private void setSlabBounds() {
		setBlockBounds(
		mSide == SIDE_X_POS ? 0.5F : 0.0F,
		mSide == SIDE_Y_POS ? 0.5F : 0.0F,
		mSide == SIDE_Z_POS ? 0.5F : 0.0F,
		mSide == SIDE_X_NEG ? 0.5F : 1.0F,
		mSide == SIDE_Y_NEG ? 0.5F : 1.0F,
		mSide == SIDE_Z_NEG ? 0.5F : 1.0F
		);
	}
	// BUG-010 (живой тест игрока: «коллизия целого блока»): shape-мосты BlockBase (getShape/getCollisionShape) читают
	// гоночное поле mRenderBounds напрямую → слэбу нужна СТАТИЧНАЯ форма, не зависящая от полей вовсе.
	private net.minecraft.world.phys.shapes.VoxelShape mSlabShape = null;
	private net.minecraft.world.phys.shapes.VoxelShape slabShape() {
		if (mSlabShape == null) mSlabShape = net.minecraft.world.phys.shapes.Shapes.create(new net.minecraft.world.phys.AABB(
			mSide == SIDE_X_POS ? 0.5 : 0.0,
			mSide == SIDE_Y_POS ? 0.5 : 0.0,
			mSide == SIDE_Z_POS ? 0.5 : 0.0,
			mSide == SIDE_X_NEG ? 0.5 : 1.0,
			mSide == SIDE_Y_NEG ? 0.5 : 1.0,
			mSide == SIDE_Z_NEG ? 0.5 : 1.0));
		return mSlabShape;
	}
	@Override protected net.minecraft.world.phys.shapes.VoxelShape getShape(net.minecraft.world.level.block.state.BlockState aState, net.minecraft.world.level.BlockGetter aWorld, net.minecraft.core.BlockPos aPos, net.minecraft.world.phys.shapes.CollisionContext aContext) {
		return mIsSlab ? slabShape() : super.getShape(aState, aWorld, aPos, aContext);
	}
	@Override protected net.minecraft.world.phys.shapes.VoxelShape getCollisionShape(net.minecraft.world.level.block.state.BlockState aState, net.minecraft.world.level.BlockGetter aWorld, net.minecraft.core.BlockPos aPos, net.minecraft.world.phys.shapes.CollisionContext aContext) {
		return mIsSlab ? slabShape() : super.getCollisionShape(aState, aWorld, aPos, aContext);
	}

	// BUG-010 (запрос игрока, согласованное отклонение от 1.7.10: в 1.7.10 waterlogging не существовал): слэбы ведут
	// себя как современные ванильные — ставятся В воду (вода остаётся, WATERLOGGED), ведро вынимает/заливает
	// (дефолты SimpleWaterloggedBlock по property), вода тикает через updateShape. Property получают ТОЛЬКО слэбы
	// (флаг-контекст SLAB_CTOR_CTX — mIsSlab на этом этапе ещё не присвоен); полные блоки — без лишнего property.
	@Override protected void createBlockStateDefinition(net.minecraft.world.level.block.state.StateDefinition.Builder<net.minecraft.world.level.block.Block, net.minecraft.world.level.block.state.BlockState> aBuilder) {
		super.createBlockStateDefinition(aBuilder);
		if (SLAB_CTOR_CTX.get()[0]) aBuilder.add(net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED);
	}
	@Override public boolean canPlaceLiquid(net.minecraft.world.entity.LivingEntity aUser, net.minecraft.world.level.BlockGetter aWorld, net.minecraft.core.BlockPos aPos, net.minecraft.world.level.block.state.BlockState aState, net.minecraft.world.level.material.Fluid aFluid) {
		return mIsSlab && net.minecraft.world.level.block.SimpleWaterloggedBlock.super.canPlaceLiquid(aUser, aWorld, aPos, aState, aFluid);
	}
	@Override protected net.minecraft.world.level.material.FluidState getFluidState(net.minecraft.world.level.block.state.BlockState aState) {
		return aState.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED) && aState.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED)
			? net.minecraft.world.level.material.Fluids.WATER.getSource(false) : super.getFluidState(aState);
	}
	@Override protected net.minecraft.world.level.block.state.BlockState updateShape(net.minecraft.world.level.block.state.BlockState aState, net.minecraft.world.level.LevelReader aWorld, net.minecraft.world.level.ScheduledTickAccess aTicks, net.minecraft.core.BlockPos aPos, net.minecraft.core.Direction aDir, net.minecraft.core.BlockPos aNeighbourPos, net.minecraft.world.level.block.state.BlockState aNeighbourState, net.minecraft.util.RandomSource aRandom) {
		if (aState.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED) && aState.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED))
			aTicks.scheduleTick(aPos, net.minecraft.world.level.material.Fluids.WATER, net.minecraft.world.level.material.Fluids.WATER.getTickDelay(aWorld));
		return super.updateShape(aState, aWorld, aTicks, aPos, aDir, aNeighbourPos, aNeighbourState, aRandom);
	}

	// Рендер слэба вообще не должен зависеть от гоночных общих полей: bounds статичны → отдаём их напрямую.
	private float[] mSlabRenderBounds = null;
	@Override public float[] getRenderBounds() {
		if (!mIsSlab) return super.getRenderBounds();
		if (mSlabRenderBounds == null) mSlabRenderBounds = new float[] {
			mSide == SIDE_X_POS ? 0.5F : 0.0F,
			mSide == SIDE_Y_POS ? 0.5F : 0.0F,
			mSide == SIDE_Z_POS ? 0.5F : 0.0F,
			mSide == SIDE_X_NEG ? 0.5F : 1.0F,
			mSide == SIDE_Y_NEG ? 0.5F : 1.0F,
			mSide == SIDE_Z_NEG ? 0.5F : 1.0F};
		return mSlabRenderBounds;
	}
	@Override public boolean setBlockBounds(int aRenderPass, net.minecraft.world.item.ItemStack aStack) {
		if (mIsSlab) setSlabBounds();
		return super.setBlockBounds(aRenderPass, aStack);
	}
	@Override public boolean setBlockBounds(int aRenderPass, net.minecraft.world.level.BlockGetter aWorld, int aX, int aY, int aZ, boolean[] aShouldSideBeRendered) {
		if (mIsSlab) setSlabBounds();
		return super.setBlockBounds(aRenderPass, aWorld, aX, aY, aZ, aShouldSideBeRendered);
	}

	public void onBlockCreation(Class<? extends BlockItem> aItemClass, Material aVanillaMaterial, SoundType aSoundType, String aName, String aDefaultLocalised, OreDictMaterial aMaterial, float aResistanceMultiplier, float aHardnessMultiplier, int aHarvestLevel, int aCount, IIconContainer[] aIcons) {
		//
	}
	
	public void onSlabCreation(Class<? extends BlockItem> aItemClass, Material aVanillaMaterial, SoundType aSoundType, String aName, String aDefaultLocalised, OreDictMaterial aMaterial, float aResistanceMultiplier, float aHardnessMultiplier, int aHarvestLevel, int aCount, IIconContainer[] aIcons, byte aSlabType, BlockMetaType aBlock) {
		//
	}
	
	// Подключение канала «клик по блоку» (2026-07-30, реестр мёртвых каналов). Канал СМЕСТИЛСЯ: в 1.7.10
	// движок звал Block.onBlockActivated, в neo — BlockBehaviour.useItemOn (с предметом) и useWithoutItem
	// (пустая рука). Мост тот же, что у брата MultiTileEntityBlock:365-381, включая порядок диспетчеризации.
	// Мост стоит ЗДЕСЬ, а не в корне BlockBase: в оригинале правило живёт у BlockMetaType:142 и
	// BlockStones:557, в базе его нет; BlockStones наследует этот класс, поэтому один мост покрывает оба.
	// Без моста тело ниже не звалось никем: половинка GT6 не собиралась в целый блок кликом второй половинки,
	// и правило BlockStones (:573) тоже молчало.
	@Override protected net.minecraft.world.InteractionResult useItemOn(net.minecraft.world.item.ItemStack aStack, net.minecraft.world.level.block.state.BlockState aState, Level aWorld, net.minecraft.core.BlockPos aPos, Player aPlayer, net.minecraft.world.InteractionHand aHand, net.minecraft.world.phys.BlockHitResult aHit) {
		if (aHand == net.minecraft.world.InteractionHand.MAIN_HAND && bridgeBlockActivated(aWorld, aPos, aPlayer, aHit))
			return aWorld.isClientSide() ? net.minecraft.world.InteractionResult.SUCCESS : net.minecraft.world.InteractionResult.SUCCESS_SERVER;
		return net.minecraft.world.InteractionResult.TRY_WITH_EMPTY_HAND;
	}
	@Override protected net.minecraft.world.InteractionResult useWithoutItem(net.minecraft.world.level.block.state.BlockState aState, Level aWorld, net.minecraft.core.BlockPos aPos, Player aPlayer, net.minecraft.world.phys.BlockHitResult aHit) {
		if (bridgeBlockActivated(aWorld, aPos, aPlayer, aHit))
			return aWorld.isClientSide() ? net.minecraft.world.InteractionResult.SUCCESS : net.minecraft.world.InteractionResult.SUCCESS_SERVER;
		return net.minecraft.world.InteractionResult.PASS;
	}
	private boolean bridgeBlockActivated(Level aWorld, net.minecraft.core.BlockPos aPos, Player aPlayer, net.minecraft.world.phys.BlockHitResult aHit) {
		net.minecraft.world.phys.Vec3 tHitVec = aHit.getLocation();
		return onBlockActivated(aWorld, aPos.getX(), aPos.getY(), aPos.getZ(), aPlayer, aHit.getDirection().get3DDataValue(),
			(float)(tHitVec.x - aPos.getX()), (float)(tHitVec.y - aPos.getY()), (float)(tHitVec.z - aPos.getZ()));
	}

	// @Override
	public boolean onBlockActivated(Level aWorld, int aX, int aY, int aZ, Player aPlayer, int aSide, float aHitX, float aHitY, float aHitZ) {
		if (mBlock == this || aSide != OPOS[mSide] || (WD.hasCollide(aWorld, aX, aY, aZ, mBlock) && !WD.noEntityCollision(aWorld, WD.collisionBox(aWorld, aX, aY, aZ, mBlock)))) return F;
		ItemStack aStack = aPlayer.getMainHandItem();
		byte aMetaData = WD.meta(aWorld, aX, aY, aZ);
		if (ST.equal(aStack, mBlock.mSlabs[0], aMetaData)) {
			WD.set(aWorld, aX, aY, aZ, mBlock, aMetaData, 3);
			WD.playStepSound(aWorld, aX + 0.5F, aY + 0.5F, aZ + 0.5F, mBlock);
			if (!UT.Entities.hasInfiniteItems(aPlayer)) aStack.setCount(aStack.getCount()-1);
			return T;
		}
		return F;
	}
	
	// было shouldSideBeRendered(IBlockAccess,x,y,z,side) -> BlockBehaviour.skipRendering(BlockState,BlockState,Direction)
	// [BlockBehaviour.java:160], семантика ИНВЕРТИРОВАНА (shouldRender -> skipRendering). Позиция(aX,aY,aZ) в исходнике
	// была позицией СОСЕДА (стандартная 1.7.10-семантика shouldSideBeRendered) -> aNeighbor.getBlock() эквивалентен
	// WD.block(aWorld,aX,aY,aZ) без потерь, доп. world/pos не требовались.
	/**
	 * ЦЕНТР «рисовать ли грань к соседу» — то, чем в 1.7.10 был {@code shouldSideBeRendered(world,x,y,z,side)}.
	 *
	 * <p><b>Почему контракт переведён на СОСТОЯНИЯ.</b> neo спрашивает видимость грани через
	 * {@code BlockBehaviour.skipRendering(BlockState, BlockState, Direction)} — мира и координат там нет.
	 * Потомки, переопределившие 1.7.10-сигнатуру, остались без вызывателей: их правило просто выпадало.
	 * Живой случай (найден игроком сверкой с 1.7.10): два блока стекла GT6 рядом рисовали между собой
	 * стенку, тогда как в оригинале одинаковые стёкла сливаются — правило жило в
	 * {@code BlockGlassClear.shouldSideBeRendered}, которое движок не звал.
	 *
	 * <p>Поэтому вопрос задаётся ЗДЕСЬ, в общем предке семейства, и ровно один раз; потомки переопределяют
	 * этот метод, а не мёртвую сигнатуру. Возврат — как в 1.7.10: {@code true} = грань рисовать.
	 *
	 * <p>Само объявление контракта — в корне иерархии ({@link gregapi.block.BlockBase}); здесь только вызов,
	 * потому что этот мост переопределяет предковый.
	 */
	@Override
	protected boolean skipRendering(BlockState aState, BlockState aNeighbor, Direction aDir) {
		byte aSide = UT.Code.side(aDir);
		// сперва — правило семьи (стёкла/дорожка): «не рисовать» у них выражается через контракт выше
		if (!shouldSideBeRendered(aState, aNeighbor, aSide)) return T;
		if (aSide == OPOS[mSide]) return F;
		if (aSide != mSide && SIDES_VALID[mSide]) {
			Block aBlock = aNeighbor.getBlock();
			// было aBlock.getRenderBlockPass() (1.7.10 vanilla Block, overridable) - neo Block не несёт эту
			// override-точку generic-но; BlockMetaType-семейство сама её нигде не переопределяет (грепом по
			// оригиналу - только этот единственный вызов), т.е. в 1.7.10 всегда резолвился в vanilla-дефолт 0 ->
			// GT6-own reintroduced константный метод ниже (тот же приём, что BlockBaseFluid/PrefixBlock/
			// MultiTileEntityBlock уже применяют для этого имени). Восстановлено "!= 0" (было инвертировано на
			// "== 0" при предыдущем порте - 1:1 с оригиналом gregtech6/.../BlockMetaType.java:161).
			if (aBlock instanceof BlockMetaType && ((BlockMetaType)aBlock).mSide == mSide) return ((BlockMetaType)aBlock).getRenderBlockPass() != 0;
		}
		return super.skipRendering(aState, aNeighbor, aDir);
	}
	public int getRenderBlockPass() {return 0;}
	
	@Override public String getHarvestTool(int aMeta) {return TOOL_pickaxe;}
	@Override public int getHarvestLevel(int aMeta) {return mHarvestLevel;}
	@Override public float getBlockHardness(Level aWorld, int aX, int aY, int aZ) {return WD.hardness(Blocks.STONE, aWorld, aX, aY, aZ) * mHardnessMultiplier;}
	// было Block.getExplosionResistance(Entity) (1.7.10, вызов с null-Entity) -> Block.getExplosionResistance() [Block.java:453],
	// тот же no-arg-fallback, что уже принят для аналогичного generic-вызова в WD.scan (WD.java:1123).
	@Override public float getExplosionResistance(byte aMeta) {return Blocks.STONE.getExplosionResistance() * mResistanceMultiplier;}
	@Override public boolean isSideSolid(int aMeta, byte aSide) {return mBlock == this || mSide == aSide;}
	@Override public boolean isNormalCube(BlockGetter aWorld, int aX, int aY, int aZ)  {return mBlock == this;}
	public boolean isNormalCube() {return mBlock == this;}
	@Override public boolean isOpaqueCube() {return mBlock == this;}
	@Override public boolean renderAsNormalBlock() {return mBlock == this;}
	@Override public boolean doesPistonPush(byte aMeta) {return T;}
	@Override public int getLightOpacity() {return mBlock == this ? LIGHT_OPACITY_MAX : LIGHT_OPACITY_WATER;}
	@Override public int getItemStackLimit(ItemStack aStack) {return UT.Code.bindStack(OP.stone.mDefaultStackSize * (mBlock.mBlock == mBlock ? 1 : 2));}
	@Override public Item getItemDropped(int par1, Random par2Random, int par3) {return Item.byBlock(mIsSlab ? mBlock.mSlabs[0] : mBlock);}
	@Override public void getSubBlocks(Item aItem, CreativeModeTab aTab, @SuppressWarnings("rawtypes") List aList) {if (mIsPrimary) super.getSubBlocks(aItem, aTab, aList);}
	@Override public Item getItem(Level aWorld, int aX, int aY, int aZ) {return Item.byBlock(mIsSlab ? mBlock.mSlabs[0] : mBlock);}
}

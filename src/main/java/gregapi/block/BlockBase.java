/**
 * Copyright (c) 2023 GregTech-6 Team
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

package gregapi.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.Explosion;

import gregapi.data.LH;
import gregapi.data.OP;
import gregapi.util.ST;
import gregapi.util.UT;
import gregapi.util.WD;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FallingBlock;
import gregapi.block.Material;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.resources.Identifier;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.IPlantable;
import net.minecraft.core.Direction;

import java.util.List;
import java.util.ArrayList;
import java.util.Random;

import static gregapi.data.CS.*;

/**
 * @author Gregorius Techneticies
 */
public abstract class BlockBase extends Block implements IBlockBase {
	public final String mNameInternal;
	/** F-bounds: последние заданные bounds (1.7.10 мутировал Block.mBoundingBox); рендер-использование
	 *  отложено на F3-клиент-проход. Хранит форму {minX,minY,minZ,maxX,maxY,maxZ}. */
	protected float[] mRenderBounds = {0, 0, 0, 1, 1, 1};
	/** F-bounds-race (системный фикс гонки рендера): 1.7.10 был однопоточен — мутация общих полей Block в рендер-цикле
	 *  была безопасна. neo мешает чанки НЕСКОЛЬКИМИ worker-потоками с ОДНИМ Block-инстансом: пассовые setBlockBounds
	 *  разных потоков и анти-протечка (сброс в куб) гонялись по одному полю → недетерминированные полные кубы у
	 *  полу-форм (репорт игрока по слэбам) и порча shape-канала. Канал развязан: рендер-цепь (GT6BlockModel/BER —
	 *  скобки {@code RENDER_BOUNDS_CTX}) пишет и читает ТОЛЬКО потоко-локальную копию; общие поля мутируются лишь вне
	 *  рендер-контекста (конструкторы, серверная логика — 1:1 с 1.7.10) и питают shape-мосты. Реализации
	 *  setBlockBounds(pass,...) сотен блоков не изменялись — центр один. */
	public static final ThreadLocal<boolean[]> RENDER_BOUNDS_CTX = ThreadLocal.withInitial(() -> new boolean[1]);
	private final ThreadLocal<float[]> mRenderBoundsTL = ThreadLocal.withInitial(() -> mRenderBounds.clone());
	@Override public void setBlockBounds(float aMinX, float aMinY, float aMinZ, float aMaxX, float aMaxY, float aMaxZ) {
		float[] tBounds = new float[] {aMinX, aMinY, aMinZ, aMaxX, aMaxY, aMaxZ};
		mRenderBoundsTL.set(tBounds);
		if (!RENDER_BOUNDS_CTX.get()[0]) mRenderBounds = tBounds;
	}
	/** F3-render: текущие render-bounds {minX,minY,minZ,maxX,maxY,maxZ} для GT6BlockModel (было RenderBlocks.setRenderBoundsFromBlock).
	 *  Читает потоко-локальную копию (см. F-bounds-race выше) — в рендер-потоке это значения ЕГО пассов, не чужих. */
	public float[] getRenderBounds() {return mRenderBoundsTL.get();}
	// F-shape (класс «канал движка сместился»; зеркало моста MTE-иерархий MultiTileEntityBlock:296): 1.7.10-коллизия
	// шла через vanilla-поверхность Block.addCollisionBoxesToList/getCollisionBoundingBoxFromPool (дефолт = статические
	// bounds setBlockBounds + pos; подклассы переопределяли: Bars/Spike/LilyPad/Path/Leaves/Sapling/CFoamFresh).
	// Оригинальный BlockBase их НЕ переопределял — поверхность жила на vanilla Block; в neo она УДАЛЕНА (VoxelShape)
	// → дефолты восстановлены ЗДЕСЬ в корне иерархии, override-цепь подклассов работает как в 1.7.10.
	/** 1:1-порт vanilla-дефолта Block.getCollisionBoundingBoxFromPool (recompSrc 1.7.10: статические bounds + pos). */
	public AABB getCollisionBoundingBoxFromPool(Level aWorld, int aX, int aY, int aZ) {
		float[] tB = mRenderBounds;
		return new AABB(aX+tB[0], aY+tB[1], aZ+tB[2], aX+tB[3], aY+tB[4], aZ+tB[5]);
	}
	/** 1:1-порт vanilla-дефолта Block.addCollisionBoxesToList (recompSrc 1.7.10 Block.java:661-669: pool + intersects). */
	@SuppressWarnings({"unchecked", "rawtypes"})
	public void addCollisionBoxesToList(Level aWorld, int aX, int aY, int aZ, AABB aAABB, List aList, Entity aEntity) {
		AABB tBox = getCollisionBoundingBoxFromPool(aWorld, aX, aY, aZ);
		if (tBox != null && aAABB.intersects(tBox)) aList.add(tBox);
	}
	/** 1:1-порт vanilla-дефолта Block.setBlockBoundsBasedOnState (no-op: bounds статичны); подклассы переопределяют (Bars:186/LilyPad:128/Path:151). */
	public void setBlockBoundsBasedOnState(BlockGetter aWorld, int aX, int aY, int aZ) {/**/}
	/**
	 * BUG-076 — ФОРМА ИЗ СОСТОЯНИЯ. Хук для семей, у которых геометрия зависит от подтипа блока
	 * (решётки: биты соединений; шипы: сторона крепления). Такие семьи в 1.7.10 читали мету ИЗ МИРА
	 * (`setBlockBoundsBasedOnState`/`getCollisionBoundingBoxFromPool` принимали World+координаты), и порт
	 * это сохранил дословно. В neo этого мало: движок строит BlockState-кэш формы ОДИН раз на
	 * {@code EmptyBlockGetter}/{@code BlockPos.ZERO} ({@code BlockBehaviour:916}), где мира нет — мостами
	 * ниже это уходило в статические {@code mRenderBounds} = полный куб, и тонкая решётка снаружи вела
	 * себя как сплошной блок (замер: 11 классов из 13 в этой ветке).
	 *
	 * <p>Мета при этом ДОСТУПНА и без мира — она живёт в самом {@code BlockState}
	 * ({@code IBlockExtendedMetaData.getExtendedMetaData(BlockState)}, F13-снимок, заведён для BUG-016/047).
	 * Поэтому семья возвращает форму отсюда, и кэш становится ВЕРНЫМ — в отличие от приёма брата
	 * ({@code MultiTileEntityBlock:165} гасит кэш через {@code dynamicShape()}, что там неизбежно: форма
	 * MTE живёт в BlockEntity, а его в кэш-контексте нет).
	 *
	 * @param aCollision {@code true} — коллизия (физическое препятствие), {@code false} — outline/прицел.
	 * @return форма в локальных координатах 0..1 либо {@code null} — «формы из состояния нет», мосты идут прежним путём.
	 */
	protected net.minecraft.world.phys.shapes.VoxelShape shapeFromState(BlockState aState, boolean aCollision) {return null;}

	// Мост neo №1: getCollisionShape ← addCollisionBoxesToList (список под-боксов, сущность из EntityCollisionContext —
	// лодка LilyPad и т.п.; pool=null у подкласса → пустая коллизия = проходим, 1:1). Гейт hasCollision — блоки с
	// Properties.noCollission не должны отвердеть. Кэш-ветка (EmptyBlockGetter при построении BlockState-кэша:
	// снег/isFaceSturdy/suffocation) — сначала форма ИЗ СОСТОЯНИЯ (shapeFromState), и лишь если её нет —
	// статические bounds: зеркало 1.7.10, где эти проверки тоже читали статический mBoundingBox.
	@Override protected net.minecraft.world.phys.shapes.VoxelShape getCollisionShape(BlockState aState, BlockGetter aWorld, BlockPos aPos, net.minecraft.world.phys.shapes.CollisionContext aContext) {
		if (!hasCollision) return net.minecraft.world.phys.shapes.Shapes.empty();
		net.minecraft.world.phys.shapes.VoxelShape tFromState = shapeFromState(aState, T);
		if (tFromState != null) return tFromState;
		if (aWorld instanceof Level tLevel) {
			List<AABB> tList = new java.util.ArrayList<>();
			addCollisionBoxesToList(tLevel, aPos.getX(), aPos.getY(), aPos.getZ(), new AABB(aPos.getX()-1, aPos.getY()-1, aPos.getZ()-1, aPos.getX()+2, aPos.getY()+2, aPos.getZ()+2), tList, aContext instanceof net.minecraft.world.phys.shapes.EntityCollisionContext tEntityContext ? tEntityContext.getEntity() : null);
			net.minecraft.world.phys.shapes.VoxelShape rShape = net.minecraft.world.phys.shapes.Shapes.empty();
			for (AABB tBox : tList) if (tBox != null) rShape = net.minecraft.world.phys.shapes.Shapes.or(rShape, net.minecraft.world.phys.shapes.Shapes.create(tBox.move(-aPos.getX(), -aPos.getY(), -aPos.getZ())));
			return rShape;
		}
		float[] tB = mRenderBounds;
		return tB[0] <= 0 && tB[1] <= 0 && tB[2] <= 0 && tB[3] >= 1 && tB[4] >= 1 && tB[5] >= 1 ? super.getCollisionShape(aState, aWorld, aPos, aContext) : net.minecraft.world.phys.shapes.Shapes.create(new AABB(tB[0], tB[1], tB[2], tB[3], tB[4], tB[5]));
	}
	// Мост neo №2: getShape (outline/таргетинг/raytrace) = 1:1 семантика 1.7.10 Block.collisionRayTrace (recompSrc:
	// СНАЧАЛА setBlockBoundsBasedOnState, ЗАТЕМ статические bounds). Пустой результат (гонка render-мутации bounds)
	// → полный куб, не empty: empty-outline делает блок неприцеливаемым.
	@Override protected net.minecraft.world.phys.shapes.VoxelShape getShape(BlockState aState, BlockGetter aWorld, BlockPos aPos, net.minecraft.world.phys.shapes.CollisionContext aContext) {
		// BUG-076: форма из состояния — единственный путь, который верен и в кэше (мира нет), и в живом мире.
		// Семьи, которым нужна ещё и мировая логика (у решётки — «игрок держит такой же блок в руке → полный
		// куб для удобства достройки»), решают это внутри своей реализации хука.
		net.minecraft.world.phys.shapes.VoxelShape tFromState = shapeFromState(aState, F);
		if (tFromState != null) return tFromState.isEmpty() ? net.minecraft.world.phys.shapes.Shapes.block() : tFromState;
		try { setBlockBoundsBasedOnState(aWorld, aPos.getX(), aPos.getY(), aPos.getZ()); } catch (Throwable e) {/*чужой BlockGetter/гонка — статические bounds ниже*/}
		float[] tB = mRenderBounds;
		if (tB[0] <= 0 && tB[1] <= 0 && tB[2] <= 0 && tB[3] >= 1 && tB[4] >= 1 && tB[5] >= 1) return super.getShape(aState, aWorld, aPos, aContext);
		net.minecraft.world.phys.shapes.VoxelShape rShape = net.minecraft.world.phys.shapes.Shapes.create(new AABB(tB[0], tB[1], tB[2], tB[3], tB[4], tB[5]));
		return rShape.isEmpty() ? net.minecraft.world.phys.shapes.Shapes.block() : rShape;
	}
	/** F3-render диспетчер-канал 1.7.10 (RenderBlocks.renderBlockByRenderType): vanilla Block.getRenderType()==0 —
	 *  стандартный куб; PILLAR-классы (BlockBaseBeam/Log/Bale) возвращают PILLAR_RENDER=31 → GT6BlockModel
	 *  применяет поворот UV по оси укладки (1:1 renderBlockLog). Метод существовал на подклассах и до этого —
	 *  без базового дефолта модель не могла его диспетчеризовать. */
	public int getRenderType() {return 0;}
	/** F-light: 1.7.10 Block.setLightLevel(float) мутировал эмиссию. neo эмиссия — Properties.lightLevel(ToIntFunction<BlockState>),
	 *  выставляется при ctor, но ВЫЧИСЛЯЕТСЯ лениво (initCache после регистрации) → функция читает mLightLevel через
	 *  state.getBlock() уже ПОСЛЕ setLightLevel подкласса. Мост подключён (lightOf ниже в mkProps). setLightLevel хранит поле. */
	protected float mLightLevel = 0.0F;
	public void setLightLevel(float aLightLevel) {mLightLevel = aLightLevel;}
	/** F-light мост: neo lightLevel-функция; читает mLightLevel инстанса через state.getBlock() в момент initCache (после setLightLevel). */
	private static int lightOf(net.minecraft.world.level.block.state.BlockState aState) {return aState.getBlock() instanceof BlockBase b ? (int)(15.0F * b.mLightLevel) : 0;}

	/** F9: gregapi Material (портированная 1.7.10-модель) хранится блоком — neo `WD.getMaterial(Block)` удалён. */
	protected final Material mMaterial;
	public Material getMaterial() {return mMaterial;}
	// F-harvest-tool (зеркало mkProps MTE/PrefixBlock — ТРЕТИЙ корень, семья BlockBase; согласовано с игроком
	// 2026-07-22): гейт «нужен ли инструмент для дропа» решает МАТЕРИАЛ (1.7.10 EntityPlayer.canHarvestBlock →
	// Material.isToolNotRequired). Породы/кирпичи (Material.rock) → только кирка (рука ломает /100 БЕЗ дропа);
	// дерево/ткань/земля (isToolNotRequired) — рука дропает /30. Без гейта ВСЯ семья дропалась рукой — щедрее канона.
	private static net.minecraft.world.level.block.state.BlockBehaviour.Properties mkProps(String aNameInternal, Material aMaterial, SoundType aSoundType) {
		net.minecraft.world.level.block.state.BlockBehaviour.Properties p = net.minecraft.world.level.block.state.BlockBehaviour.Properties.of().sound(aSoundType).lightLevel(BlockBase::lightOf).setId(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.BLOCK, net.minecraft.resources.Identifier.fromNamespaceAndPath(gregapi.data.CS.ModIDs.GT, gregapi.GT_API.sanitizeRegName(aNameInternal))));
		if (aMaterial != null && !aMaterial.isToolNotRequired()) p = p.requiresCorrectToolForDrops();
		p = mapColorOf(p, aMaterial);
		return p;
	}

	/**
	 * MODCOMPAT-002 (блоки GT6 невидимы на карте). В 1.7.10 цвет блока на карте приходил САМ СОБОЙ: ванильный
	 * {@code Block.getMapColor(int)} возвращал {@code getMaterial().getMaterialMapColor()}
	 * (`recompSrc/net/minecraft/block/Block.java:232-235`), и GT6 его нигде не переопределял, кроме
	 * {@code MultiTileEntityBlock:155}. В neo дефолт другой — {@code state -> MapColor.NONE}
	 * (`BlockBehaviour.java:970`), то есть «пропустить блок», отчего руды/камни/растения и все жидкости GT6
	 * пропадали и с ванильной карты, и с миникарт. Возвращаем ровно 1.7.10-дефолт: цвет берётся из того же
	 * материала тем же мостом {@code MapColor.toNeo()} (F9-bridge), что уже используют MTE-блоки — приём и
	 * источник переиспользованы, не заведены заново.
	 */
	public static net.minecraft.world.level.block.state.BlockBehaviour.Properties mapColorOf(net.minecraft.world.level.block.state.BlockBehaviour.Properties aProps, Material aMaterial) {
		if (aMaterial == null) return aProps;
		gregapi.block.MapColor tColor = aMaterial.getMaterialMapColor();
		return tColor == null ? aProps : aProps.mapColor(tColor.toNeo());
	}
	public BlockBase(Class<? extends BlockItem> aItemClass, String aNameInternal, Material aMaterial, SoundType aSoundType) {
		// F16/F9 форс движка: neo `Block` immutable (данные в Properties ДО super). setStepSound встроен в Properties.sound;
		// setBlockName удалён (имя через реестр — ST.register ниже); setCreativeTab(tabBlock) → CreativeTabsGT.assign(BLOCK) ниже
		// (last-wins: subclass-ctor переопределит). Light ПОДКЛЮЧЁН (lightLevel(lightOf) — ленивая функция читает
		// mLightLevel). Твёрдость/mapColor per-meta варьируются → динамические override'ы (getDestroyProgress/getMapColor), не Properties.
		// F12-followup (block-split): setId в Properties (иначе «Block id not set»); namespace=GAPI (совпадает с реестром BLOCKS,
		// куда ST.register клал блок), ключ санитизирован. Конструкция — на RegisterEvent через registerBlockLazy на call-site.
		super(mkProps(aNameInternal, aMaterial, aSoundType));
		mMaterial = aMaterial;
		mNameInternal = aNameInternal;
		gregapi.item.CreativeTabsGT.assign(this, gregapi.item.CreativeTabsGT.BLOCK); // F16 1:1: 1.7.10 setCreativeTab(tabBlock); last-wins → subclass переопределит
		// F12-followup (block-split): блок регистрирует registerBlockLazy на call-site; ЗДЕСЬ (RegisterEvent<Block>, ITEMS открыт)
		// регистрируем ТОЛЬКО BlockItem через supplier. Было: ST.register(this,...) (регистрировало блок эагер→freeze + BlockItem).
		final Class<? extends BlockItem> tItemClass = aItemClass==null?gregapi.block.ItemBlockBase.class:aItemClass;
		gregapi.GT_API.registerItemLazy(gregapi.data.CS.ModIDs.GT, mNameInternal, () -> (BlockItem)gregapi.util.UT.Reflection.callConstructor(tItemClass, 0, null, T, this));
		LH.add(mNameInternal+"."+W, "Any Sub-Block of this one");
	}
	
	public final String getUnlocalizedName() {return mNameInternal;}
	public String getLocalizedName() {return gregapi.lang.LanguageHandler.get(mNameInternal);}
	public String getHarvestTool(int aMeta) {return TOOL_pickaxe;}
	public int getHarvestLevel(int aMeta) {return 0;}
	public boolean canSilkHarvest() {return canSilkHarvest((byte)0);}
	public boolean canSilkHarvest(Level aWorld, Player aPlayer, int aX, int aY, int aZ, int aMeta) {return canSilkHarvest(UT.Code.bind4(aMeta));}
	public boolean isToolEffective(String aType, int aMeta) {return getHarvestTool(aMeta).equals(aType);}
	public boolean canBeReplacedByLeaves(BlockGetter aWorld, int aX, int aY, int aZ) {return F;}
	public boolean isNormalCube(BlockGetter aWorld, int aX, int aY, int aZ)  {return T;}
	public boolean renderAsNormalBlock() {return T;}
	public boolean isOpaqueCube() {return T;}
	public boolean func_149730_j() {return isOpaqueCube();}
	// F-occlusion МОСТ (репорт игрока: кувшинка/слаб рядом с блоком делает его прозрачным): 1.7.10-канал
	// isOpaqueCube() портирован per-класс (LilyPad/Bars/Spike/Sapling/Leaves/Path/Glass=F, слабы BlockMetaType=
	// mBlock==this), но ОСИРОТЕЛ — neo вырезает грани соседей по occlusion-форме состояния (canOcclude +
	// getOcclusionShape; дефолт = ПОЛНЫЙ куб → не-полные блоки глушили рендер за собой). Мост: не-opaque →
	// occlusion-форма ПУСТА (сосед рисуется) и свет проходит (1.7.10 lightOpacity = isOpaqueCube?255:0).
	// Кэш состояний строится ПОСЛЕ ctor (initCache) → override и per-класс isOpaqueCube резолвятся корректно.
	@Override protected net.minecraft.world.phys.shapes.VoxelShape getOcclusionShape(BlockState aState) {
		return isOpaqueCube() ? super.getOcclusionShape(aState) : net.minecraft.world.phys.shapes.Shapes.empty();
	}
	@Override protected boolean propagatesSkylightDown(BlockState aState) {
		return !isOpaqueCube() || super.propagatesSkylightDown(aState);
	}
	public boolean isSideSolid(BlockGetter aWorld, int aX, int aY, int aZ, Direction aDirection) {return isSideSolid(WD.meta(aWorld, aX, aY, aZ), UT.Code.side(aDirection));}
	// было shouldSideBeRendered(IBlockAccess,x,y,z,side) -> BlockBehaviour.skipRendering(BlockState,BlockState,Direction)
	// [BlockBehaviour.java:160], семантика ИНВЕРТИРОВАНА (shouldRender -> skipRendering) И новая сигнатура не
	// передаёт World/BlockPos - для isOpaqueCube()==true ветка (константный результат от THIS-блока, позиция
	// не нужна) переносится напрямую с инверсией; для else-ветки используем ванильный дефолт (position-lost).
	/**
	 * ЦЕНТР «рисовать ли грань к соседу» — то, чем в 1.7.10 был {@code shouldSideBeRendered(world,x,y,z,side)}.
	 *
	 * <p><b>Почему контракт по СОСТОЯНИЯМ.</b> neo спрашивает видимость грани через
	 * {@code BlockBehaviour.skipRendering(BlockState, BlockState, Direction)} — мира и координат там нет.
	 * Потомки, у которых правило осталось в 1.7.10-сигнатуре, вызывателей не имели, и их логика выпадала.
	 * Живой случай (найден игроком сверкой с 1.7.10): два блока стекла GT6 рядом рисовали между собой
	 * стенку, хотя одинаковые стёкла должны сливаться.
	 *
	 * <p>Вопрос задаётся здесь, в КОРНЕ иерархии, и оттуда его получают обе ветки — и {@code BlockMetaType}
	 * (стёкла), и прямые наследники {@code BlockBaseMeta} (дорожка). Возврат как в 1.7.10: {@code true} = рисовать.
	 */
	public boolean shouldSideBeRendered(BlockState aState, BlockState aNeighbor, byte aSide) {return T;}

	@Override protected boolean skipRendering(BlockState aState, BlockState aNeighbor, Direction aDir) {if (!shouldSideBeRendered(aState, aNeighbor, UT.Code.side(aDir))) return T; return isOpaqueCube() ? WD.visOpq(aNeighbor.getBlock()) : super.skipRendering(aState, aNeighbor, aDir);}
	public int damageDropped(int aMeta) {return aMeta;}
	public int quantityDropped(int aMeta, int aFortune, Random aRandom) {return 1;}
	public ItemStack createStackedBlock(int aMeta) {return ST.make(this, 1, damageDropped(aMeta));}

	// BUG-066 (репорт игрока: «любая балка это oak, хотя текстура и дроп от правильного дерева»): в neo предмет
	// «этого блока» — единый канал getCloneItemStack (IBlockExtension), и от него зависит ВСЁ, что показывают о
	// блоке: имя и иконка в тултипе/Jade, средний клик. Его дефолт — `new ItemStack(this)`, то есть подтип 0
	// (BlockBehaviour:393), поэтому балка любой породы представлялась дубовой, хотя в мире мета верная (оттого и
	// текстура с дропом были правильными). В 1.7.10 этот канал существовал и был подтип-зависимым —
	// `createStackedBlock(meta)` через `damageDropped` (оригинал BlockBase:82,84); тело перенесено 1:1 (строка выше),
	// но мост в движок к нему подключён не был. Подключаем ЗДЕСЬ, в корне иерархии — одним местом на брёвна, балки,
	// доски, плиты, камни и листву (тот же приём, что уже применён точечно: BlockBaseSpike:137, MultiTileEntityBlock:506).
	@Override public ItemStack getCloneItemStack(net.minecraft.world.level.LevelReader aLevel, net.minecraft.core.BlockPos aPos, BlockState aState, boolean aIncludeData, Player aPlayer) {
		ItemStack rStack = createStackedBlock(WD.meta(aLevel, aPos.getX(), aPos.getY(), aPos.getZ()));
		return rStack == null || rStack.isEmpty() ? super.getCloneItemStack(aLevel, aPos, aState, aIncludeData, aPlayer) : rStack;
	}

	public int getDamageValue(Level aWorld, int aX, int aY, int aZ) {return WD.meta(aWorld, aX, aY, aZ);}
	public int getLightOpacity() {return LIGHT_OPACITY_MAX;}

	// F3 light-opacity МОСТ (корень иерархии BlockBase — сюда сходятся BlockBaseSealable/Meta/Tree/MetaType,
	// стекло, листва, саженцы, решётки, шипы, кувшинки, дорожки). В 1.7.10 движок спрашивал getLightOpacity()
	// у блока; в neo затухание берётся из состояния — LightEngine.getOpacity:85-87 читает
	// state.getLightDampening(). Без моста значения GT6 до движка не доходили: он подставлял свой дефолт
	// (BlockBehaviour:290-295), из-за чего листва гасила 0 вместо 1, дорожка 0/1 вместо 3 и т.д.
	// Значение НЕ дублируется — берётся из того же getLightOpacity(), перевод шкалы 1.7.10→neo в одном месте
	// (CS.lightDampening). Момент вызова безопасен: initCache идёт ПОСЛЕ регистрации блоков
	// (neo-decompiled/.../Blocks.java:7221-7228), поэтому поля потомков уже заполнены.
	@Override protected int getLightDampening(net.minecraft.world.level.block.state.BlockState aState) {return gregapi.data.CS.lightDampening(getLightOpacity());}

	// F3 shade МОСТ (репорт игрока сверкой с 1.7.10: камень ПОД стеклом GT6 заметно темнеет, в оригинале
	// стекло на камне почти незаметно). Затенение соседних граней в 1.7.10 задавал getAmbientOcclusionLightValue()
	// = isBlockNormalCube() ? 0.2 : 1.0 (Block.java:1334-1337, 502-504), и GT6 читал его у всех шести соседей
	// собственным AO-рендером (gregapi/render/ITexture.java:386-527). В neo то же значение спрашивается каналом
	// getShadeBrightness (BlockModelLighter:50-128), но ПРИЗНАК другой — isCollisionShapeFullBlock
	// (BlockBehaviour:306-308). Признаки расходятся ровно на блоках GT6 с полной коллизией и
	// renderAsNormalBlock()==F: стёкла, дорожки, половинки — в 1.7.10 они не затемняли ничего, а neo-дефолт
	// тушил ими соседей до 0.2. Мост задаёт 1.7.10-признак; величины не дублируются — перевод в CS.shadeBrightness.
	@Override protected float getShadeBrightness(BlockState aState, BlockGetter aWorld, BlockPos aPos) {return gregapi.data.CS.shadeBrightness(isBlockNormalCube());}

	/** 1.7.10 {@code Block.isBlockNormalCube()} ({@code Block.java:502-504}) — признак «нормальный куб» для
	 *  затенения соседей. Тело 1:1; {@code renderAsNormalBlock()} виртуален, поэтому переопределения потомков
	 *  (стёкла, листва, дорожки, половинки {@code BlockMetaType}) учитываются так же, как в оригинале. */
	public boolean isBlockNormalCube() {return mMaterial.blocksMovement() && renderAsNormalBlock();}
	public Item getItemDropped(int aMeta, Random aRandom, int aFortune) {return Item.byBlock(this);}

	// BUG-006: GT6 simple-блоки (логи/камни/листва/руды/трава/стекло/путь/cfoam) НЕ имеют loot-table → neo-дефолт
	// getDrops(loot) отдавал ПУСТО → блоки не дропались НИЧЕМ. Мост neo getDrops(state,params) → GT6-хук
	// getDrops(Level,x,y,z,meta,fortune) (тот же приём, что MTE.playerDestroy→harvestBlock, но через neo getDrops:
	// сохраняет dropResources+BlockDropsEvent+onBlockHarvestingEvent — это 1:1 порт 1.7.10 HarvestDropsEvent:
	// unification/leafdecay/silk/fortune). Блок уже air на этом хуке → мету берём из СНИМКА aState (WD.meta(BlockState),
	// фикс BUG-016/BUG-026), а НЕ из мира: WD.meta(мир) вернул бы 0 и вся BlockBaseMeta-семья дропала бы вариант .0.
	@Override protected List<ItemStack> getDrops(BlockState aState, net.minecraft.world.level.storage.loot.LootParams.Builder aParams) {
		net.minecraft.server.level.ServerLevel tLevel = aParams.getLevel();
		net.minecraft.world.phys.Vec3 tOrigin = aParams.getOptionalParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN);
		if (tOrigin == null) return super.getDrops(aState, aParams);
		// BUG-024: гейт дропа от взрыва — ЦЕНТР WD.explosionDropDenied (консолидация, копии искоренены).
		if (WD.explosionDropDenied(aParams)) return java.util.Collections.emptyList();
		int tX = net.minecraft.util.Mth.floor(tOrigin.x), tY = net.minecraft.util.Mth.floor(tOrigin.y), tZ = net.minecraft.util.Mth.floor(tOrigin.z);
		int tFortune = 0;
		net.minecraft.world.entity.Entity tEntity = aParams.getOptionalParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.THIS_ENTITY);
		if (tEntity instanceof net.minecraft.world.entity.LivingEntity tLiving) tFortune = net.minecraft.world.item.enchantment.EnchantmentHelper.getEnchantmentLevel(tLevel.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT).getOrThrow(net.minecraft.world.item.enchantment.Enchantments.FORTUNE), tLiving);
		// BUG-026 (тот же F13-класс, что BUG-016): сухое/заплесневелое/сгнившее сено давало мокрый Grass Bale (вариант .0),
		// т.к. WD.meta(мир) читал уже-air = 0. Мета из снимка aState — тот же готовый мост WD.meta(BlockState) (WD.java:828).
		ArrayList<ItemStack> rDrops = getDrops(tLevel, tX, tY, tZ, WD.meta(aState), tFortune);
		return rDrops == null ? java.util.Collections.emptyList() : rDrops;
	}
	// 1.7.10 Block.getDrops(World,x,y,z,meta,fortune) дефолт: quantityDropped копий ST(getItemDropped,1,damageDropped).
	// Наследники (BlockBaseLeaves/Stones/RockOres/Grass/Path/...) переопределяют; базовый = блок роняет себя (лог/камень/земля).
	public ArrayList<ItemStack> getDrops(Level aWorld, int aX, int aY, int aZ, int aMeta, int aFortune) {
		ArrayList<ItemStack> rDrops = ST.arraylist();
		Item tItem = getItemDropped(aMeta, RNGSUS, aFortune);
		if (tItem != null) for (int i = 0, j = quantityDropped(aMeta, aFortune, RNGSUS); i < j; i++) rDrops.add(ST.make(tItem, 1, damageDropped(aMeta)));
		return rDrops;
	}
	public Item getItem(Level aWorld, int aX, int aY, int aZ) {return Item.byBlock(this);}
	public void registerBlockIcons(Object aIconRegister) {/**/}
	public boolean canSustainPlant(BlockGetter aWorld, int aX, int aY, int aZ, Direction aSide, IPlantable aPlant) {return F;}
	public boolean canCreatureSpawn(MobCategory type, BlockGetter aWorld, int aX, int aY, int aZ) {byte aMeta = WD.meta(aWorld, aX, aY, aZ); return canCreatureSpawn(aMeta) && isSideSolid(aMeta, SIDE_TOP);}
	public boolean isFireSource(Level aWorld, int aX, int aY, int aZ, Direction aSide) {return isFireSource(WD.meta(aWorld, aX, aY, aZ));}
	public boolean isFlammable(BlockGetter aWorld, int aX, int aY, int aZ, Direction aSide) {return isFlammable(WD.meta(aWorld, aX, aY, aZ));}
	public int getFlammability(BlockGetter aWorld, int aX, int aY, int aZ, Direction aSide) {return getFlammability(WD.meta(aWorld, aX, aY, aZ));}
	public int getFireSpreadSpeed(BlockGetter aWorld, int aX, int aY, int aZ, Direction aSide) {return getFireSpreadSpeed(WD.meta(aWorld, aX, aY, aZ));}
	// было getExplosionResistance(Entity,World,x,y,z,eX,eY,eZ) -> IBlockExtension.getExplosionResistance
	// (BlockState,BlockGetter,BlockPos,Explosion) [IBlockExtension.java:333]
	@Override public float getExplosionResistance(BlockState aState, BlockGetter aWorld, BlockPos aPos, Explosion aExplosion) {return getExplosionResistance(WD.meta(aWorld, aPos.getX(), aPos.getY(), aPos.getZ()));}
	public float getExplosionResistance(Entity aEntity) {return getExplosionResistance((byte)0);}
	// F12/F9-hardness: getDestroySpeed(BlockGetter,BlockPos) возвращает лишь запечённый Properties.destroyTime (не зовёт Block,
	// neo Properties immutable → runtime setHardness невозможен), НО getDestroyProgress(state,player,world,pos) — overridable
	// динамический хук. Подключаем GT6-getBlockHardness (субклассы дают vanilla/GT6-значения) по vanilla-формуле — 1:1.
	@Override protected float getDestroyProgress(net.minecraft.world.level.block.state.BlockState aState, net.minecraft.world.entity.player.Player aPlayer, net.minecraft.world.level.BlockGetter aWorld, net.minecraft.core.BlockPos aPos) {
		if (!(aWorld instanceof Level tLevel)) return super.getDestroyProgress(aState, aPlayer, aWorld, aPos);
		return WD.destroyProgress(getBlockHardness(tLevel, aPos.getX(), aPos.getY(), aPos.getZ()), aPlayer, aState, aWorld, aPos); // vanilla-формула — ЦЕНТР WD.destroyProgress
	}
	public float getBlockHardness(Level aWorld, int aX, int aY, int aZ) {return 1;}
	@Override public Block getBlock() {return this;}
	@Override public byte maxMeta() {return 1;}
	public final void onNeighborBlockChange(Level aWorld, int aX, int aY, int aZ, Block aBlock) {if (useGravity(WD.meta(aWorld, aX, aY, aZ))) aWorld.scheduleTick(new BlockPos(aX, aY, aZ), this, 2); onNeighborBlockChange2(aWorld, aX, aY, aZ, aBlock);}
	// F-neighbor (канал сместился): 1.7.10 World.notifyBlocksOfNeighborChange звал Block.onNeighborBlockChange; neo-вход —
	// BlockBehaviour.neighborChanged. Мост по образцу BlockFluidBaseGT:154; GT6-канал (гравитация + onNeighborBlockChange2) цел.
	@Override protected void neighborChanged(BlockState aState, Level aWorld, BlockPos aPos, Block aBlock, net.minecraft.world.level.redstone.Orientation aOrientation, boolean aMovedByPiston) {
		onNeighborBlockChange(aWorld, aPos.getX(), aPos.getY(), aPos.getZ(), aBlock);
	}
	// было onBlockAdded(World,x,y,z) -> BlockBehaviour.onPlace(BlockState,Level,BlockPos,BlockState,boolean) [BlockBehaviour.java:167]
	@Override protected final void onPlace(BlockState aState, Level aWorld, BlockPos aPos, BlockState aOldState, boolean aMovedByPiston) {if (useGravity(WD.meta(aWorld, aPos.getX(), aPos.getY(), aPos.getZ()))) aWorld.scheduleTick(aPos, this, 2); onBlockAdded2(aWorld, aPos.getX(), aPos.getY(), aPos.getZ());}
	public Identifier getIcon(BlockGetter aWorld, int aX, int aY, int aZ, int aSide) {return getIcon(aSide, WD.meta(aWorld, aX, aY, aZ));}
	// F3 superseded-render (GT6BlockModel/ItemModel пайплайн; старый getIcon/immediate-mode мёртв, 0 вызовов neo): было наследуемое vanilla Block.getIcon(int,int) (1.7.10, удалено в 26.1.2
	// целиком вместе со всем IIcon-атласом) — GT6 полагался на полиморфную диспетчеризацию к этому методу движка.
	// Восстановлено локально (тот же приём, что уже принят в BlockBaseMeta.getIcon), чтобы вызов выше и переопределения
	// в наследниках (BlockBaseSpike/BlockBaseBars/...) имели общую точку. Держатель ссылки — Identifier (см. IIconContainer).
	/** Дефолт корня иерархии: в оригинале собственного тела у BlockBase НЕТ — {@code getIcon(side,meta)} приходил
	 *  от ванильного Block ({@code blockIcon}), которого в neo не существует. Отдаём {@code null} = «канал иконки
	 *  не заведён» (контракт {@link gregapi.block.IBlock#getIcon}); потребитель уходит на штатный baked-путь.
	 *  Потомки со своими спрайтами (BlockBaseMeta/Log/Beam/Leaves/Sapling/Flower/LilyPad/Bale/Rail/Grass/Path)
	 *  перекрывают его, как перекрывали в 1.7.10. */
	public Identifier getIcon(int aSide, int aMeta) {return null;}
	
	@Override public String name(byte aMeta) {return aMeta == W ? mNameInternal : mNameInternal + "." + aMeta;}
	@Override public boolean useGravity(byte aMeta) {return F;}
	@Override public boolean doesWalkSpeed(byte aMeta) {return F;}
	@Override public boolean doesPistonPush(byte aMeta) {return F;}
	@Override public boolean canSilkHarvest(byte aMeta) {return T;}
	@Override public boolean canCreatureSpawn(byte aMeta) {return F;}
	@Override public boolean isSealable(byte aMeta, byte aSide) {return isSideSolid(aMeta, aSide);}
	@Override public boolean isFireSource(byte aMeta) {return F;}
	@Override public boolean isFlammable(byte aMeta) {return getFlammability(aMeta) > 0;}
	@Override public void addInformation(ItemStack aStack, byte aMeta, Player aPlayer, List<String> aList, boolean aF3_H) {/**/}
	@Override public float getExplosionResistance(byte aMeta) {return 10.0F;}
	@Override public int getFlammability(byte aMeta) {return 0;}
	@Override public int getFireSpreadSpeed(byte aMeta) {return 0;}
	@Override public int getItemStackLimit(ItemStack aStack) {return UT.Code.bindStack(OP.block.mDefaultStackSize);}
	@Override public ItemStack onItemRightClick(ItemStack aStack, Level aWorld, Player aPlayer) {return aStack;}
	
	public boolean checkNoEntityCollision(Level aWorld, int aX, int aY, int aZ, byte aMeta, Entity aExceptThisOne) {return WD.noEntityCollision(aWorld, new AABB(aX, aY, aZ, aX+1, aY+1, aZ+1), aExceptThisOne);}

	// F-block-placement: 1.7.10/Forge Block.canReplace(World,x,y,z,side,stack) и Block.onBlockPlaced(...,meta)
	// удалены из neo (размещение перестроено на BlockPlaceContext/getStateForPlacement). Воспроизводим Forge-дефолты
	// как GT6-хелперы: canReplace=T (реальная проверка заменяемости — WD.replaceable в onItemUse выше); onBlockPlaced
	// возвращает мету без изменений (facing-подклассы переопределяют). ItemBlock.placeBlockAt/World.canPlaceEntityOnSide
	// — заменены прямым WD.set + checkNoEntityCollision в onItemUse (см. ниже).
	public boolean canReplace(Level aWorld, int aX, int aY, int aZ, int aSide, ItemStack aStack) {return T;}
	public byte onBlockPlaced(Level aWorld, int aX, int aY, int aZ, int aSide, float aHitX, float aHitY, float aHitZ, byte aMeta) {return aMeta;}
	public boolean isSideSolid(int aMeta, byte aSide) {return T;}
	public void updateTick2(Level aWorld, int aX, int aY, int aZ, Random aRandom) {/**/}
	public void onNeighborBlockChange2(Level aWorld, int aX, int aY, int aZ, Block aBlock) {/**/}
	public void onBlockAdded2(Level aWorld, int aX, int aY, int aZ) {/**/}
	
	// BUG-005: neo scheduled-tick канал = tick(BlockState,ServerLevel,BlockPos,RandomSource) (образец BlockFluidBaseGT:142).
	// updateTick был СИРОТОЙ (1.7.10-сигнатура «// @Override», никто не звал) → распад листвы/рост саженцев/гравитация/
	// мшистость всей семьи BlockBase были МЕРТВЫ (scheduleTick бил в неперекрытый neo tick()). Мост 1:1: java.util.Random из RandomSource.
	@Override protected void tick(BlockState aState, net.minecraft.server.level.ServerLevel aWorld, BlockPos aPos, net.minecraft.util.RandomSource aRandom) {
		updateTick(aWorld, aPos.getX(), aPos.getY(), aPos.getZ(), UT.Code.random(aRandom)); // конвертер — ЦЕНТР UT.Code.random
	}
	public final void updateTick(Level aWorld, int aX, int aY, int aZ, Random aRandom) {
		if (aWorld.isClientSide() || checkGravity(aWorld, aX, aY, aZ)) return;
		updateTick2(aWorld, aX, aY, aZ, aRandom);
	}
	
	public boolean checkGravity(Level aWorld, int aX, int aY, int aZ) {
		byte aMeta = WD.meta(aWorld, aX, aY, aZ);
		if (aY > 0 && useGravity(aMeta) && FallingBlock.isFree(WD.block(aWorld, aX, aY - 1, aZ).defaultBlockState())) {
			// было BlockFalling.fallInstantly (1.7.10 static-поле, дефолт false, не найден ни в одном из 3 корней
			// референса) -> дефолтное значение "T" (=!false), тот же эффект без движкового поля.
			// было World.checkChunksExist(x0,y0,z0,x1,y1,z1) (симметричный диапазон ±32) -> ILevelReaderExtension.
			// isAreaLoaded(BlockPos,int) [ILevelReaderExtension.java:19], тот же приём, что уже принят для
			// doChunksNearChunkExist в block-behavior 2-м проходе (decisions/DEFERRED-LEDGER.md §B2).
			if (T && aWorld.isAreaLoaded(new BlockPos(aX, aY, aZ), 32)) {
				// было new FallingBlockEntity(World,x,y,z,Block,meta) + addFreshEntity (1.7.10-форма, приватный
				// конструктор в neo) -> FallingBlockEntity.fall(Level,BlockPos,BlockState) [FallingBlockEntity.java:91],
				// единственный публичный neo-путь спавна (сам заменяет исходный блок на fluid-state и вызывает addFreshEntity).
				if (!aWorld.isClientSide()) FallingBlockEntity.fall(aWorld, new BlockPos(aX, aY, aZ), aWorld.getBlockState(new BlockPos(aX, aY, aZ)));
			} else {
				WD.set(aWorld, aX, aY, aZ, NB, 0, 3);
				while (FallingBlock.isFree(WD.block(aWorld, aX, aY-1, aZ).defaultBlockState()) && aY > 0) --aY;
				if (aY > 0) WD.set(aWorld, aX, aY, aZ, this, aMeta, 2);
			}
			return T;
		}
		return F;
	}
	
	@Override public boolean onItemUseFirst(ItemBlockBase aItem, ItemStack aStack, Player aPlayer, Level aWorld, int aX, int aY, int aZ, int aSide, float aHitX, float aHitY, float aHitZ) {return F;}
	
	@Override
	public boolean onItemUse(ItemBlockBase aItem, ItemStack aStack, Player aPlayer, Level aWorld, int aX, int aY, int aZ, int aSide, float aHitX, float aHitY, float aHitZ) {
		if (aStack.getCount() == 0) return F;
		
		Block tBlock = WD.block(aWorld, aX, aY, aZ);
		if (tBlock == Blocks.SNOW && (WD.meta(aWorld, aX, aY, aZ) & 7) < 1) {
			aSide = SIDE_UP;
		} else if (tBlock != Blocks.VINE && tBlock != Blocks.DEAD_BUSH && tBlock != Blocks.DEAD_BUSH && !WD.replaceable(tBlock, aWorld, aX, aY, aZ)) {
			aX += OFFX[aSide]; aY += OFFY[aSide]; aZ += OFFZ[aSide];
		}

		if (!WD.replaceable(WD.block(aWorld, aX, aY, aZ), aWorld, aX, aY, aZ)) return F;
		if (!canReplace(aWorld, aX, aY, aZ, aSide, aStack)) return F;
		byte aMeta = UT.Code.bind4(aItem.getMetadata(ST.meta(aStack)));
		if (!checkNoEntityCollision(aWorld, aX, aY, aZ, aMeta, null)) return F;
		// canPlaceEntityOnSide восстановлен 1:1 через ЦЕНТР WD.canPlaceEntityOnSide (Forge-хук удалён по ИМЕНИ, способность
		// адаптирована централизованно — коллизия формы с исключением размещающего + заменяемость цели, WD.java).
		if (!(aPlayer).mayUseItemAt(new BlockPos(aX, aY, aZ), FORGE_DIR[aSide], aStack) || (aY == 255 && getMaterial().isSolid()) || !WD.canPlaceEntityOnSide(aWorld, this, aX, aY, aZ, F, aSide, aPlayer, aStack)) return F;

		// 1:1 vanilla ItemBlock.onItemUse: завершение установки — КАНАЛ aItem.placeBlockAt (подклассы item'а его
		// переопределяют: ItemBlockMetaType выбирает слэб-вариант по wrenching-стороне клика). Дефолт placeBlockAt =
		// WD.set(getBlock(), aMeta, 3) — прежний итог для всех остальных. Прямой WD.set(this) здесь ОБХОДИЛ канал →
		// оверрайд-выбор варианта был сиротой, слэб ставился всегда DOWN (BUG-010, живой тест игрока).
		if (aItem.placeBlockAt(aStack, aPlayer, aWorld, aX, aY, aZ, aSide, aHitX, aHitY, aHitZ, onBlockPlaced(aWorld, aX, aY, aZ, aSide, aHitX, aHitY, aHitZ, aMeta))) {
			WD.playStepSound(aWorld, aX+0.5F, aY+0.5F, aZ+0.5F, this);
			aStack.setCount(aStack.getCount()-1);
		}
		return T;
	}
	
	public final int quantityDropped(Random aRandom) {return quantityDropped(0, 0, aRandom);}
}

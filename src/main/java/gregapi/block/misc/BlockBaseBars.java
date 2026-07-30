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

package gregapi.block.misc;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.Direction;

import static gregapi.data.CS.*;

import java.util.List;
import java.util.Random;

import gregapi.block.BlockBaseSealable;
import gregapi.block.ItemBlockBase;
import gregapi.data.OP;
import gregapi.oredict.OreDictMaterial;
import gregapi.render.IRenderedBlock;
import gregapi.render.IRenderedBlockObject;
import gregapi.render.ITexture;
import gregapi.render.RendererBlockTextured;
import gregapi.util.CR;
import gregapi.util.ST;
import gregapi.util.UT;
import gregapi.util.WD;
import net.minecraft.world.level.block.Block;
import gregapi.block.Material;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;

public abstract class BlockBaseBars extends BlockBaseSealable implements IRenderedBlock, gregapi.block.IBlockExtendedMetaData {
	public final OreDictMaterial mMat;

	// F13 (заход данжей #39, живой тест «заборы не повёрнуты»): мета баров = битмаска горизонтальных соединений
	// (1=N,2=S,4=W,8=E — весь класс оперирует ею через WD.set/WD.meta, рендер BarRenderer(mMeta)); хранение —
	// BlockState-свойство META, маршрутизация get/setExtendedMetaData — дефолты IBlockExtendedMetaData
	// (консолидация захода #39: прежнее дословное зеркало BlockBaseMeta удалено, один код на всех носителей).
	@Override protected void createBlockStateDefinition(net.minecraft.world.level.block.state.StateDefinition.Builder<net.minecraft.world.level.block.Block, net.minecraft.world.level.block.state.BlockState> aBuilder) {super.createBlockStateDefinition(aBuilder); aBuilder.add(gregapi.block.BlockBaseMeta.META);}

	public BlockBaseBars(String aNameInternal, OreDictMaterial aMat, Material aVanillaMaterial, SoundType aSoundType) {
		super(null, aNameInternal, aVanillaMaterial, aSoundType);
		registerDefaultState(getStateDefinition().any().setValue(gregapi.block.BlockBaseMeta.META, 0)); // после super (createBlockStateDefinition отработал в Block-конструкторе)
		gregapi.item.CreativeTabsGT.assign(this, gregapi.item.CreativeTabsGT.REDSTONE);
		if (COMPAT_FR != null) gregapi.GT_API.deferItemInit(() -> COMPAT_FR.addToBackpacks("builder", ST.make(this, 1, W)));
		mMat = aMat;
		
		gregapi.GT_API.deferItemInit(() -> CR.shaped(ST.make(this, 3, 0), CR.DEF_REV_NCC_MIR, "BBB", aVanillaMaterial == Material.wood ? "r v" : "h w", "BBB", 'B', OP.stick.dat(mMat)));
		
		if (CODE_CLIENT) {
			mRenderers[ 0] = new BarRendererItem(aMat);
			mRenderers[ 1] = new BarRenderer(aMat,  1);
			mRenderers[ 2] = new BarRenderer(aMat,  2);
			mRenderers[ 3] = new BarRenderer(aMat,  3);
			mRenderers[ 4] = new BarRenderer(aMat,  4);
			mRenderers[ 5] = new BarRenderer(aMat,  5);
			mRenderers[ 6] = new BarRenderer(aMat,  6);
			mRenderers[ 7] = new BarRenderer(aMat,  7);
			mRenderers[ 8] = new BarRenderer(aMat,  8);
			mRenderers[ 9] = new BarRenderer(aMat,  9);
			mRenderers[10] = new BarRenderer(aMat, 10);
			mRenderers[11] = new BarRenderer(aMat, 11);
			mRenderers[12] = new BarRenderer(aMat, 12);
			mRenderers[13] = new BarRenderer(aMat, 13);
			mRenderers[14] = new BarRenderer(aMat, 14);
			mRenderers[15] = new BarRenderer(aMat, 15);
		}
	}
	
	@Override
	public boolean onItemUseFirst(ItemBlockBase aItem, ItemStack aStack, Player aPlayer, Level aWorld, int aX, int aY, int aZ, int aSide, float aHitX, float aHitY, float aHitZ) {
		if (aStack.getCount() == 0 || aWorld.isClientSide()) return F;
		if (!aPlayer.isShiftKeyDown()) {
			for (int i = 0; i < 2; i++) {
				if (i == 1) {aX += OFFX[aSide]; aY += OFFY[aSide]; aZ += OFFZ[aSide];}
				if (!(aPlayer).mayUseItemAt(new BlockPos(aX, aY, aZ), FORGE_DIR[aSide], aStack)) return F;
				Block aBlock = WD.block(aWorld, aX, aY, aZ);
				byte  aMeta  = WD.meta (aWorld, aX, aY, aZ);
				if (aBlock == this) {
					byte tMeta = (byte)(FACE_CONNECTION_COUNT[aMeta] == 3 ? 15-aMeta : aHitX < aHitZ ? aHitX + aHitZ < 1 ? 4 : 2 : aHitX + aHitZ < 1 ? 1 : 8);
					if ((aMeta & tMeta) != 0 || SIDES_HORIZONTAL[aSide]) tMeta = (byte)(SIDES_AXIS_X[aSide] ? aHitZ < 0.5 ? 1 : 2 : aHitX < 0.5 ? 4 : 8);
					if ((aMeta & tMeta) != 0 && SIDES_HORIZONTAL[aSide]) tMeta = (byte)(SBIT[aSide] >> 2);
					if ((aMeta & tMeta) == 0 && tMeta != 0) {
						if (WD.set(aWorld, aX, aY, aZ, this, aMeta | tMeta, 3)) {
							WD.playStepSound(aWorld, aX+0.5, aY+0.5, aZ+0.5, this);
							if (!UT.Entities.hasInfiniteItems(aPlayer)) aStack.setCount(aStack.getCount()-1);
						}
						return T;
					}
					if (aMeta != 15) return F;
				}
			}
			aX -= OFFX[aSide]; aY -= OFFY[aSide]; aZ -= OFFZ[aSide];
		}
		
		Block aBlock = WD.block(aWorld, aX, aY, aZ);
		
		if (aBlock == Blocks.SNOW && (WD.meta(aWorld, aX, aY, aZ) & 7) < 1) {
			aSide = SIDE_UP;
		} else if (aBlock != Blocks.VINE && aBlock != Blocks.DEAD_BUSH && aBlock != Blocks.DEAD_BUSH && !WD.replaceable(aBlock, aWorld, aX, aY, aZ)) {
			aX += OFFX[aSide]; aY += OFFY[aSide]; aZ += OFFZ[aSide];
			aBlock = WD.block(aWorld, aX, aY, aZ);
		}

		if (!WD.replaceable(aBlock, aWorld, aX, aY, aZ)) return F;
		
		// if used in conjunction with << 2 , these Meta Values return the Side Bits perfectly.
		// Z- = 1, Z+ = 2, X- = 4, X+ = 8
		if (aItem.placeBlockAt(aStack, aPlayer, aWorld, aX, aY, aZ, aSide, aHitX, aHitY, aHitZ, (SIDES_HORIZONTAL[aSide] ? SIDES_AXIS_X[aSide] ? aHitZ < 0.5 ? 1 : 2 : aHitX < 0.5 ? 4 : 8 : aHitX < aHitZ ? aHitX + aHitZ < 1 ? 4 : 2 : aHitX + aHitZ < 1 ? 1 : 8))) {
			WD.playStepSound(aWorld, aX+0.5, aY+0.5, aZ+0.5, this);
			if (!UT.Entities.hasInfiniteItems(aPlayer)) aStack.setCount(aStack.getCount()-1);
			return T;
		}
		return F;
	}
	
	@Override public boolean onItemUse(ItemBlockBase aItem, ItemStack aStack, Player aPlayer, Level aWorld, int aX, int aY, int aZ, int aSide, float aHitX, float aHitY, float aHitZ) {return F;}
	@Override public String getHarvestTool(int aMeta) {return getMaterial() == Material.wood ? TOOL_axe : TOOL_pickaxe;}
	@Override public int getHarvestLevel(int aMeta) {return mMat.mToolQuality;}
	@Override public int getLightOpacity() {return LIGHT_OPACITY_NONE;}
	@Override public int damageDropped(int aMeta) {return 0;}
	@Override public int quantityDropped(int aMeta, int aFortune, Random aRandom) {return aMeta == 0 ? 1 : FACE_CONNECTION_COUNT[aMeta];}
	@Override public byte maxMeta() {return 1;}
	@Override public float getBlockHardness(Level aWorld, int aX, int aY, int aZ) {return 5;}
	@Override public float getExplosionResistance(byte aMeta) {return 5;}
	@Override public boolean doesPistonPush(byte aMeta) {return T;}
	@Override public boolean renderAsNormalBlock() {return F;}
	@Override public boolean isOpaqueCube() {return F;}
	@Override public boolean isNormalCube(BlockGetter aWorld, int aX, int aY, int aZ) {return F;}
	@Override public boolean isSideSolid(int aMeta, byte aSide) {return F;}
	@Override public boolean isSealable(byte aMeta, byte aSide) {return F;}
	// было shouldSideBeRendered(IBlockAccess,x,y,z,side) -> BlockBehaviour.skipRendering(BlockState,BlockState,Direction)
	// [BlockBehaviour.java:160]; исходное тело - константа T (всегда рендерить) не зависела от позиции/соседа,
	// переносится без потерь с инверсией (shouldRender=T -> skipRendering=F).
	@Override public boolean skipRendering(BlockState aState, BlockState aNeighbor, Direction aDir) {return F;}
	@SuppressWarnings("unchecked") public void getSubBlocks(Item aItem, CreativeModeTab aTab, @SuppressWarnings("rawtypes") List aList) {aList.add(ST.make(aItem, 1, 0));}

	// F13: getPickBlock возвращал ST.make(this,1,0) = ровно то, что даёт neo-дефолт getCloneItemStack (block-item meta0) →
	// отдельный override не нужен, поведение 1:1 покрыто дефолтом. GT6-метод сохранён для внутренних вызовов.
	public ItemStack getPickBlock(HitResult aTarget, Level aWorld, int aX, int aY, int aZ, Player aPlayer) {return ST.make(this, 1, 0);}
	
	public AABB getCollisionBoundingBoxFromPool(Level aWorld, int aX, int aY, int aZ) {return null;}

	/**
	 * BUG-076: форма решётки ИЗ СОСТОЯНИЯ (мета соединений 1=Z-,2=Z+,4=X-,8=X+ живёт в BlockState-свойстве META).
	 *
	 * <p>Тот же набор боксов, что у 1.7.10-каналов ниже, — ЕДИНСТВЕННЫЙ источник геометрии на класс:
	 * {@code addCollisionBoxesToList} и {@code setBlockBoundsBasedOnState} тоже читают его, поэтому вторая
	 * копия координат не заводится. Коллизия — пластины 2px по занятым сторонам (1:1 оригинал `:197-200`),
	 * outline — 1px-пластина при одиночном соединении и полный куб иначе (1:1 оригинал `:165-169`).</p>
	 *
	 * <p>Мировую оговорку оригинала («игрок держит такой же блок в руке ближе 5 блоков → полный куб, чтобы
	 * было куда целиться при достройке», `:158-162`) здесь воспроизвести нельзя — мира в кэш-контексте нет.
	 * Она осталась в {@link #setBlockBoundsBasedOnState}, но для outline теперь не применяется: цена ей —
	 * удобство прицеливания, тогда как ошибка формы ломает саму проходимость. Осознанное отклонение.</p>
	 */
	@Override protected net.minecraft.world.phys.shapes.VoxelShape shapeFromState(net.minecraft.world.level.block.state.BlockState aState, boolean aCollision) {
		byte tMeta = UT.Code.bind4(getExtendedMetaData(aState));
		if (aCollision) {
			net.minecraft.world.phys.shapes.VoxelShape rShape = net.minecraft.world.phys.shapes.Shapes.empty();
			for (AABB tBox : collisionBoxes(tMeta)) rShape = net.minecraft.world.phys.shapes.Shapes.or(rShape, net.minecraft.world.phys.shapes.Shapes.create(tBox));
			return rShape;
		}
		switch (tMeta) {
		case  1: return net.minecraft.world.phys.shapes.Shapes.create(new AABB(0        , 0, 0        , 1        , 1, PX_P[ 1]));
		case  2: return net.minecraft.world.phys.shapes.Shapes.create(new AABB(0        , 0, PX_P[15] , 1        , 1, 1       ));
		case  4: return net.minecraft.world.phys.shapes.Shapes.create(new AABB(0        , 0, 0        , PX_P[ 1] , 1, 1       ));
		case  8: return net.minecraft.world.phys.shapes.Shapes.create(new AABB(PX_P[15] , 0, 0        , 1        , 1, 1       ));
		default: return net.minecraft.world.phys.shapes.Shapes.block();
		}
	}

	/** ЕДИНСТВЕННЫЙ источник боксов коллизии решётки, локальные координаты 0..1 (1:1 оригинал `:197-200`). */
	private static java.util.List<AABB> collisionBoxes(byte aMeta) {
		java.util.List<AABB> rList = new java.util.ArrayList<>(4);
		if ((aMeta & 1) != 0) rList.add(new AABB(0        , 0, 0        , 1        , 1, PX_P[ 2]));
		if ((aMeta & 2) != 0) rList.add(new AABB(0        , 0, PX_P[14] , 1        , 1, 1       ));
		if ((aMeta & 4) != 0) rList.add(new AABB(0        , 0, 0        , PX_P[ 2] , 1, 1       ));
		if ((aMeta & 8) != 0) rList.add(new AABB(PX_P[14] , 0, 0        , 1        , 1, 1       ));
		return rList;
	}
	
	// ⚠️ КАНАЛ ИЗБЫТОЧЕН — роль закрыта shapeFromState:187 (BUG-076): форма выделения выводится из состояния
	// и спрашивается движком через getShape в корне BlockBase. Мировая оговорка оригинала («игрок держит
	// такой же блок ближе 5 блоков → полный куб») в outline не воспроизводится — это ОСОЗНАННОЕ отклонение,
	// обоснованное в javadoc выше (:182-185): в кэш-контексте формы мира нет, а цена оговорке — удобство
	// прицеливания, тогда как ошибка формы ломает проходимость. Метод оставлен точкой сверки с оригиналом.
	// @Override
	public AABB getSelectedBoundingBoxFromPool(Level aWorld, int aX, int aY, int aZ) {
		// было World.loadedEntityList (1.7.10 public-поле) -> neo EntityGetter.players() [EntityGetter.java:27]
		// (уже отфильтрованный список игроков, instanceof-проверка больше не нужна).
		for (Player tEntity : aWorld.players()) {
			if (ST.equal(tEntity.getMainHandItem(), this) && tEntity.distanceToSqr(aX, aY, aZ) <= 25) {
				return  new AABB(aX         , aY, aZ         , aX+1       , aY+1, aZ+1       );
			}
		}
		switch (WD.meta(aWorld, aX, aY, aZ)) {
		case  1: return new AABB(aX         , aY, aZ         , aX+1       , aY+1, aZ+PX_P[ 1]);
		case  2: return new AABB(aX         , aY, aZ+PX_P[15], aX+1       , aY+1, aZ+1       );
		case  4: return new AABB(aX         , aY, aZ         , aX+PX_P[ 1], aY+1, aZ+1       );
		case  8: return new AABB(aX+PX_P[15], aY, aZ         , aX+1       , aY+1, aZ+1       );
		default: return new AABB(aX         , aY, aZ         , aX+1       , aY+1, aZ+1       );
		}
	}
	
	// @Override
	public void setBlockBoundsBasedOnState(BlockGetter aWorld, int aX, int aY, int aZ) {
		// было World.loadedEntityList (1.7.10 public-поле) -> neo EntityGetter.players() [EntityGetter.java:27]
		if (aWorld instanceof Level) for (Player tEntity : ((Level)aWorld).players()) {
			if (ST.equal(tEntity.getMainHandItem(), this) && tEntity.distanceToSqr(aX, aY, aZ) <= 25) {
				setBlockBounds(0, 0, 0, 1, 1, 1);
				return;
			}
		}
		switch (WD.meta(aWorld, aX, aY, aZ)) {
		case  1: setBlockBounds(0, 0, 0, 1, 1, PX_P[ 1]); return;
		case  2: setBlockBounds(0, 0, PX_P[15], 1, 1, 1); return;
		case  4: setBlockBounds(0, 0, 0, PX_P[ 1], 1, 1); return;
		case  8: setBlockBounds(PX_P[15], 0, 0, 1, 1, 1); return;
		default: setBlockBounds(0, 0, 0, 1, 1, 1); return;
		}
	}
	
	// @Override
	@SuppressWarnings("unchecked")
	public void addCollisionBoxesToList(Level aWorld, int aX, int aY, int aZ, AABB aAABB, @SuppressWarnings("rawtypes") List aList, Entity aEntity) {
		if (aEntity instanceof ItemEntity || aEntity instanceof ExperienceOrb || aEntity instanceof Projectile) return;
		// BUG-076: боксы берутся из ЕДИНСТВЕННОГО источника collisionBoxes(мета) — того же, что питает
		// shapeFromState; здесь они лишь переносятся в мировые координаты. Z- = 1, Z+ = 2, X- = 4, X+ = 8.
		byte tMeta = WD.meta(aWorld, aX, aY, aZ);
		for (AABB tLocal : collisionBoxes(tMeta)) {
			AABB tBox = tLocal.move(aX, aY, aZ);
			if (aAABB.intersects(tBox)) aList.add(tBox);
		}
	}
	
	public int getRenderType() {return RendererBlockTextured.INSTANCE==null?23:RendererBlockTextured.INSTANCE.mRenderID;}
	// F3 superseded-render (GT6BlockModel/ItemModel пайплайн; старый getIcon/immediate-mode мёртв, 0 вызовов neo): было Blocks.IRON_BARS.getIcon(2,0) (vanilla Block.getIcon удалён в 26.1.2 целиком).
	/** 1:1 оригинала (:204): {@code Blocks.iron_bars.getIcon(2, 0)} — спрайт ВАНИЛЬНЫХ решёток. neo-резолв
	 *  ванильного блока в {@link Identifier} уже централизован в {@link gregapi.render.GT6QuadBuilder#resolveBlockFaceIcon}
	 *  — спрашиваем его, второго резолвера не заводим. Только клиент: атлас существует лишь там. */
	// ⛔ Сторону НЕЛЬЗЯ определять по CS.CODE_CLIENT: в одиночной игре его выставляют ОБА прокси —
	// GT_API_Proxy_Client:93 ставит T, GT_API_Proxy_Server:32 ставит F, и побеждает тот, кто
	// инициализировался последним. Из-за этого канал молча отдавал null, рендер подставлял серую
	// CFoam-заглушку, и крошка решёток/шипов оставалась серой (найдено живым тестом игрока).
	// Физическая сторона — FMLEnvironment.getDist().isClient(), тот же канон, что в GT_API:378.
	// Гейт обязателен: resolveBlockFaceIcon трогает Minecraft.getInstance(), а на выделенном сервере
	// этого класса нет (класс дефекта BUG-084 — клиентский тип в общем коде).
	public Identifier getIcon(int aSide, int aMeta) {return net.neoforged.fml.loading.FMLEnvironment.getDist().isClient() ? gregapi.render.GT6QuadBuilder.resolveBlockFaceIcon(net.minecraft.world.level.block.Blocks.IRON_BARS, 2, 0) : null;}
	@Override public ITexture getTexture(int aRenderPass, byte aSide, ItemStack aStack) {return null;}
	@Override public ITexture getTexture(int aRenderPass, byte aSide, boolean[] aShouldSideBeRendered, BlockGetter aWorld, int aX, int aY, int aZ) {return null;}
	@Override public boolean usesRenderPass(int aRenderPass, ItemStack aStack) {return F;}
	@Override public boolean usesRenderPass(int aRenderPass, BlockGetter aWorld, int aX, int aY, int aZ, boolean[] aShouldSideBeRendered) {return F;}
	@Override public boolean setBlockBounds(int aRenderPass, ItemStack aStack) {return F;}
	@Override public boolean setBlockBounds(int aRenderPass, BlockGetter aWorld, int aX, int aY, int aZ, boolean[] aShouldSideBeRendered) {return F;}
	@Override public int getRenderPasses(ItemStack aStack) {return 0;}
	@Override public int getRenderPasses(BlockGetter aWorld, int aX, int aY, int aZ, boolean[] aShouldSideBeRendered) {return 0;}
	@Override public IRenderedBlockObject passRenderingToObject(ItemStack aStack) {return mRenderers[0];}
	@Override public IRenderedBlockObject passRenderingToObject(BlockGetter aWorld, int aX, int aY, int aZ) {return mRenderers[WD.meta(aWorld, aX, aY, aZ)];}
	
	public BarRendererBase[] mRenderers = new BarRendererBase[16];
	
	public static abstract class BarRendererBase implements IRenderedBlockObject {
		public ITexture mTexture;
		public BarRendererBase(OreDictMaterial aMat) {mTexture = aMat.getTextureSmooth();}
		
		@Override public int getRenderPasses(Block aBlock, boolean[] aShouldSideBeRendered) {return 20;}
		@Override public ITexture getTexture(Block aBlock, int aRenderPass, byte aSide, boolean[] aShouldSideBeRendered) {return mTexture;}
		@Override public boolean usesRenderPass(int aRenderPass, boolean[] aShouldSideBeRendered) {return T;}
		// F3 superseded-render (GT6BlockModel/ItemModel пайплайн; старый getIcon/immediate-mode мёртв, 0 вызовов neo): было RenderBlocks aRenderer (тип удалён в 26.1.2) — параметр Object,
		// тот же нейтральный держатель, что gregapi.render.IRenderedBlockObject#renderItem/renderBlock.
		@Override public boolean renderItem (Block aBlock, Object aRenderer) {return F;}
		@Override public boolean renderBlock(Block aBlock, Object aRenderer, BlockGetter aWorld, int aX, int aY, int aZ) {return F;}
		@Override public IRenderedBlockObject passRenderingToObject(ItemStack aStack) {return this;}
		@Override public IRenderedBlockObject passRenderingToObject(BlockGetter aWorld, int aX, int aY, int aZ) {return this;}
	}
	
	public static class BarRendererItem extends BarRendererBase {
		public BarRendererItem(OreDictMaterial aMat) {super(aMat);}
		@Override public int getRenderPasses(Block aBlock, boolean[] aShouldSideBeRendered) {return 6;}
		@Override
		public boolean setBlockBounds(Block aBlock, int aRenderPass, boolean[] aShouldSideBeRendered) {
			switch(aRenderPass) {
			default: WD.setBlockBounds(aBlock, PX_P[ 0], PX_P[ 0], PX_P[ 7], PX_P[ 1], PX_P[16], PX_P[ 8]); return T;
			case  1: WD.setBlockBounds(aBlock, PX_P[15], PX_P[ 0], PX_P[ 7], PX_P[16], PX_P[16], PX_P[ 8]); return T;
			case  2: WD.setBlockBounds(aBlock, PX_P[ 1], PX_P[ 0], PX_P[ 7], PX_P[15], PX_P[ 1], PX_P[ 8]); return T;
			case  3: WD.setBlockBounds(aBlock, PX_P[ 1], PX_P[15], PX_P[ 7], PX_P[15], PX_P[16], PX_P[ 8]); return T;
			case  4: WD.setBlockBounds(aBlock, PX_P[ 5], PX_P[ 1], PX_P[ 7], PX_P[ 6], PX_P[15], PX_P[ 8]); return T;
			case  5: WD.setBlockBounds(aBlock, PX_P[10], PX_P[ 1], PX_P[ 7], PX_P[11], PX_P[15], PX_P[ 8]); return T;
			}
		}
	}
	
	public static class BarRenderer extends BarRendererBase {
		public byte mMeta;
		public BarRenderer(OreDictMaterial aMat, int aMeta) {super(aMat); mMeta = (byte)aMeta;}
		@Override
		public boolean usesRenderPass(int aRenderPass, boolean[] aShouldSideBeRendered) {
			switch(aRenderPass) {
			default: return (mMeta &  5) != 0;
			case  1: return (mMeta &  9) != 0;
			case  2: return (mMeta &  6) != 0;
			case  3: return (mMeta & 10) != 0;
			case  4: case  8: case 12: case 16: return (mMeta &  1) != 0;
			case  5: case  9: case 13: case 17: return (mMeta &  4) != 0;
			case  6: case 10: case 14: case 18: return (mMeta &  2) != 0;
			case  7: case 11: case 15: case 19: return (mMeta &  8) != 0;
			}
		}
		
		@Override
		public boolean setBlockBounds(Block aBlock, int aRenderPass, boolean[] aShouldSideBeRendered) {
			// Z- = 1, Z+ = 2, X- = 4, X+ = 8
			switch(aRenderPass) {
			// Vertical Corner Bars
			default: WD.setBlockBounds(aBlock, PX_P[ 0], PX_P[ 0], PX_P[ 0], PX_P[ 1], PX_P[16], PX_P[ 1]); return T;
			case  1: WD.setBlockBounds(aBlock, PX_P[15], PX_P[ 0], PX_P[ 0], PX_P[16], PX_P[16], PX_P[ 1]); return T;
			case  2: WD.setBlockBounds(aBlock, PX_P[ 0], PX_P[ 0], PX_P[15], PX_P[ 1], PX_P[16], PX_P[16]); return T;
			case  3: WD.setBlockBounds(aBlock, PX_P[15], PX_P[ 0], PX_P[15], PX_P[16], PX_P[16], PX_P[16]); return T;
			// Horizontal Bottom Bars
			case  4: WD.setBlockBounds(aBlock, PX_P[ 1], PX_P[ 0], PX_P[ 0], PX_P[15], PX_P[ 1], PX_P[ 1]); return T;
			case  5: WD.setBlockBounds(aBlock, PX_P[ 0], PX_P[ 0], PX_P[ 1], PX_P[ 1], PX_P[ 1], PX_P[15]); return T;
			case  6: WD.setBlockBounds(aBlock, PX_P[ 1], PX_P[ 0], PX_P[15], PX_P[15], PX_P[ 1], PX_P[16]); return T;
			case  7: WD.setBlockBounds(aBlock, PX_P[15], PX_P[ 0], PX_P[ 1], PX_P[16], PX_P[ 1], PX_P[15]); return T;
			// Horizontal Top Bars
			case  8: WD.setBlockBounds(aBlock, PX_P[ 1], PX_P[15], PX_P[ 0], PX_P[15], PX_P[16], PX_P[ 1]); return T;
			case  9: WD.setBlockBounds(aBlock, PX_P[ 0], PX_P[15], PX_P[ 1], PX_P[ 1], PX_P[16], PX_P[15]); return T;
			case 10: WD.setBlockBounds(aBlock, PX_P[ 1], PX_P[15], PX_P[15], PX_P[15], PX_P[16], PX_P[16]); return T;
			case 11: WD.setBlockBounds(aBlock, PX_P[15], PX_P[15], PX_P[ 1], PX_P[16], PX_P[16], PX_P[15]); return T;
			// Vertical Middle Bars
			case 12: WD.setBlockBounds(aBlock, PX_P[ 5], PX_P[ 1], PX_P[ 0], PX_P[ 6], PX_P[15], PX_P[ 1]); return T;
			case 13: WD.setBlockBounds(aBlock, PX_P[ 0], PX_P[ 1], PX_P[ 5], PX_P[ 1], PX_P[15], PX_P[ 6]); return T;
			case 14: WD.setBlockBounds(aBlock, PX_P[ 5], PX_P[ 1], PX_P[15], PX_P[ 6], PX_P[15], PX_P[16]); return T;
			case 15: WD.setBlockBounds(aBlock, PX_P[15], PX_P[ 1], PX_P[ 5], PX_P[16], PX_P[15], PX_P[ 6]); return T;
			case 16: WD.setBlockBounds(aBlock, PX_P[10], PX_P[ 1], PX_P[ 0], PX_P[11], PX_P[15], PX_P[ 1]); return T;
			case 17: WD.setBlockBounds(aBlock, PX_P[ 0], PX_P[ 1], PX_P[10], PX_P[ 1], PX_P[15], PX_P[11]); return T;
			case 18: WD.setBlockBounds(aBlock, PX_P[10], PX_P[ 1], PX_P[15], PX_P[11], PX_P[15], PX_P[16]); return T;
			case 19: WD.setBlockBounds(aBlock, PX_P[15], PX_P[ 1], PX_P[10], PX_P[16], PX_P[15], PX_P[11]); return T;
			}
		}
	}
}

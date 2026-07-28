/**
 * Copyright (c) 2025 GregTech-6 Team
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
import gregapi.util.WD;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.state.BlockState;

import gregapi.block.BlockBaseSealable;
import gregapi.block.IBlockOnWalkOver;
import gregapi.block.IBlockToolable;
import gregapi.block.ToolCompat;
import gregapi.data.CS.*;
import gregapi.data.OP;
import gregapi.old.Textures;
import gregapi.oredict.OreDictMaterial;
import gregapi.render.*;
import gregapi.util.*;
import net.minecraft.world.level.block.Block;
import gregapi.block.Material;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.core.Direction;

import java.util.List;

import static gregapi.data.CS.*;

public abstract class BlockBaseSpike extends BlockBaseSealable implements IBlockOnWalkOver, IBlockToolable, IRenderedBlock, gregapi.block.IBlockExtendedMetaData {
	public final OreDictMaterial mMat1, mMat2;

	// BUG-072 (тот же класс, что BUG-071): у шипа ВСЯ его суть живёт в мете 0..15 — младшие биты это сторона
	// установки (onBlockPlaced ниже), бит 8 это ВТОРОЙ материал (рецепты :72-73, OM.data :84, getHarvestLevel ниже).
	// Канала меты у этой иерархии не было вовсе: BlockBase контракт IBlockExtendedMetaData не реализует, поэтому
	// WD.meta(...) отдавал 0 ВСЕГДА — шип вёл себя как подтип 0 (ориентация «низ», дроп первого варианта, уровень
	// добычи от первого материала). Приём взят готовый — тот же, которым канал возвращён решёткам
	// (BlockBaseBars:62,69): хранение в BlockState-свойстве META, маршрутизация get/setExtendedMetaData — дефолты
	// интерфейса. Своей сущности не заводим.
	@Override protected void createBlockStateDefinition(net.minecraft.world.level.block.state.StateDefinition.Builder<net.minecraft.world.level.block.Block, net.minecraft.world.level.block.state.BlockState> aBuilder) {super.createBlockStateDefinition(aBuilder); aBuilder.add(gregapi.block.BlockBaseMeta.META);}

	public BlockBaseSpike(String aNameInternal, OreDictMaterial aMat1, OreDictMaterial aMat2) {
		super(null, aNameInternal, Material.iron, SoundType.METAL);
		registerDefaultState(getStateDefinition().any().setValue(gregapi.block.BlockBaseMeta.META, 0)); // после super — как BlockBaseBars:73
		gregapi.item.CreativeTabsGT.assign(this, gregapi.item.CreativeTabsGT.REDSTONE);
		mMat1 = aMat1; mMat2 = aMat2;
		// F12-followup (block-split): рецепты/OM.data используют ST.make → server-start → deferItemInit.
		gregapi.GT_API.deferItemInit(() -> {
		CR.shaped(ST.make(this, 1, 0), CR.DEF_NCC, "BTB", "TPT", "BTB", 'B', OP.toolHeadSword.dat(mMat1), 'P', OP.plate.dat(mMat1), 'T', OP.screw.dat(mMat1));
		CR.shaped(ST.make(this, 1, 6), CR.DEF_NCC, "TBT", "BPB", "TBT", 'B', OP.toolHeadSword.dat(mMat1), 'P', OP.plate.dat(mMat1), 'T', OP.screw.dat(mMat1));
		CR.shaped(ST.make(this, 1, 8), CR.DEF_NCC, "BTB", "TPT", "BTB", 'B', OP.toolHeadSword.dat(mMat2), 'P', OP.plate.dat(mMat2), 'T', OP.screw.dat(mMat2));
		CR.shaped(ST.make(this, 1,14), CR.DEF_NCC, "TBT", "BPB", "TBT", 'B', OP.toolHeadSword.dat(mMat2), 'P', OP.plate.dat(mMat2), 'T', OP.screw.dat(mMat2));
		
		CR.shapeless(ST.make(this, 1, 7), CR.DEF_NCC, new Object[] {ST.make(this, 1, 6)});
		CR.shapeless(ST.make(this, 1,15), CR.DEF_NCC, new Object[] {ST.make(this, 1,14)});
		
		CR.shapeless(ST.make(this, 1, 6), CR.DEF_NCC, new Object[] {ST.make(this, 1, 7)});
		CR.shapeless(ST.make(this, 1,14), CR.DEF_NCC, new Object[] {ST.make(this, 1,15)});
		
		OM.data(ST.make(this, 1, 0), aMat1, U*9);
		OM.data(ST.make(this, 1, 6), aMat1, U*9);
		OM.data(ST.make(this, 1, 7), aMat1, U*9);
		OM.data(ST.make(this, 1, 8), aMat2, U*9);
		OM.data(ST.make(this, 1,14), aMat2, U*9);
		OM.data(ST.make(this, 1,15), aMat2, U*9);
		});

		if (CODE_CLIENT) {
			mRenderers[ 0] = new SpikeRendererYNeg(aMat1);
			mRenderers[ 1] = new SpikeRendererYPos(aMat1);
			mRenderers[ 2] = new SpikeRendererZNeg(aMat1);
			mRenderers[ 3] = new SpikeRendererZPos(aMat1);
			mRenderers[ 4] = new SpikeRendererXNeg(aMat1);
			mRenderers[ 5] = new SpikeRendererXPos(aMat1);
			mRenderers[ 6] = mRenderers[ 7] = new SpikeRendererOmni(aMat1);
			mRenderers[ 8] = new SpikeRendererYNeg(aMat2);
			mRenderers[ 9] = new SpikeRendererYPos(aMat2);
			mRenderers[10] = new SpikeRendererZNeg(aMat2);
			mRenderers[11] = new SpikeRendererZPos(aMat2);
			mRenderers[12] = new SpikeRendererXNeg(aMat2);
			mRenderers[13] = new SpikeRendererXPos(aMat2);
			mRenderers[14] = mRenderers[15] = new SpikeRendererOmni(aMat2);
		}
		
		if (COMPAT_FR != null) gregapi.GT_API.deferItemInit(() -> COMPAT_FR.addToBackpacks("builder", ST.make(this, 1, W)));
	}
	
	// было Entity.motionX/motionZ (1.7.10 public мутируемые поля) -> neo Vec3 (getDeltaMovement()) immutable ->
	// Entity.setDeltaMovement(double,double,double) [Entity.java:3672], тот же эффект.
	@Override public void onWalkOver(LivingEntity aEntity, Level aWorld, int aX, int aY, int aZ) {if ((WD.meta(aWorld, aX, aY, aZ) & 7) != SIDE_UP) {Vec3 tMotion = aEntity.getDeltaMovement(); aEntity.setDeltaMovement(tMotion.x * 0.1, tMotion.y, tMotion.z * 0.1);}}
	public int onBlockPlaced(Level aWorld, int aX, int aY, int aZ, int aSide, float aHitX, float aHitY, float aHitZ, int aMeta) {return (aMeta & 7) < 6 ? (aMeta & 8) | OPOS[aSide] : aMeta;}
	@Override public void onBlockAdded2(Level aWorld, int aX, int aY, int aZ) {if (useGravity(WD.meta(aWorld, aX, aY, aZ))) UT.Sounds.send(SFX.MC_ANVIL_LAND, 1, 2, aWorld, aX, aY, aZ);}
	
	@Override public String getHarvestTool(int aMeta) {return TOOL_pickaxe;}
	@Override public int getHarvestLevel(int aMeta) {return aMeta < 8 ? mMat1.mToolQuality : mMat2.mToolQuality;}
	@Override public int getLightOpacity() {return LIGHT_OPACITY_NONE;}
	@Override public int damageDropped(int aMeta) {return (aMeta & 7) < 6 ? aMeta & 8 : aMeta;}
	@Override public byte maxMeta() {return 16;}
	@Override public float getBlockHardness(Level aWorld, int aX, int aY, int aZ) {return 30;}
	@Override public float getExplosionResistance(byte aMeta) {return 5;}
	@Override public boolean isSideSolid(int aMeta, byte aSide) {return (aMeta & 7) < 6 && aMeta == aSide;}
	@Override public boolean isNormalCube(BlockGetter aWorld, int aX, int aY, int aZ)  {return F;}
	@Override public boolean renderAsNormalBlock() {return F;}
	@Override public boolean isOpaqueCube() {return F;}
	@Override public boolean useGravity(byte aMeta) {return (aMeta & 7) == 7;}
	@Override public boolean doesWalkSpeed(byte aMeta) {return T;}
	@Override public boolean doesPistonPush(byte aMeta) {return T;}
	@Override public boolean isSealable(byte aMeta, byte aSide) {return F;}
	// было shouldSideBeRendered(IBlockAccess,x,y,z,side) -> BlockBehaviour.skipRendering(BlockState,BlockState,Direction)
	// [BlockBehaviour.java:160]; исходное тело - константа T (всегда рендерить), не зависела от позиции/соседа,
	// переносится без потерь с инверсией (shouldRender=T -> skipRendering=F).
	@Override public boolean skipRendering(BlockState aState, BlockState aNeighbor, Direction aDir) {return F;}
	@SuppressWarnings("unchecked") public void getSubBlocks(Item aItem, CreativeModeTab aTab, @SuppressWarnings("rawtypes") List aList) {aList.add(ST.make(aItem, 1, 0)); aList.add(ST.make(aItem, 1, 6)); aList.add(ST.make(aItem, 1, 7)); aList.add(ST.make(aItem, 1, 8)); aList.add(ST.make(aItem, 1, 14)); aList.add(ST.make(aItem, 1, 15));}

	// F13: neo middle-click через IBlockExtension.getCloneItemStack — делегируем в GT6-getPickBlock (meta-specific), 1:1.
	@Override public ItemStack getCloneItemStack(net.minecraft.world.level.LevelReader aLevel, net.minecraft.core.BlockPos aPos, net.minecraft.world.level.block.state.BlockState aState, boolean aIncludeData, Player aPlayer) {
		int aMeta = WD.meta(aLevel, aPos.getX(), aPos.getY(), aPos.getZ());
		return ST.make(this, 1, (aMeta & 7) < 6 ? aMeta & 8 : aMeta);
	}
	public ItemStack getPickBlock(HitResult aTarget, Level aWorld, int aX, int aY, int aZ, Player aPlayer) {
		int aMeta = WD.meta(aWorld, aX, aY, aZ);
		return ST.make(this, 1, (aMeta & 7) < 6 ? aMeta & 8 : aMeta);
	}
	
	// @Override
	public boolean rotateBlock(Level aWorld, int aX, int aY, int aZ, Direction aAxis) {
		int aMeta = WD.meta(aWorld, aX, aY, aZ);
		return (aMeta & 7) < 6 && WD.set(aWorld, aX, aY, aZ, this, (aMeta & 8) | (((aMeta & 7) + 1) % 6), 3);
	}
	
	// @Override
	// было ForgeDirection.VALID_DIRECTIONS (1.7.10, все 6 реальных направлений, без UNKNOWN) -> neo Direction
	// не имеет UNKNOWN-константы вовсе (Direction.java:33-38, ровно 6 значений) -> Direction.values() 1:1.
	public Direction[] getValidRotations(Level aWorld, int aX, int aY, int aZ) {
		return (WD.meta(aWorld, aX, aY, aZ) & 7) < 6 ? Direction.values() : null;
	}
	
	@Override
	public long onToolClick(String aTool, long aRemainingDurability, long aQuality, Entity aPlayer, List<String> aChatReturn, Container aPlayerInventory, boolean aSneaking, ItemStack aStack, Level aWorld, byte aSide, int aX, int aY, int aZ, float aHitX, float aHitY, float aHitZ) {
		if (aTool.equals(TOOL_wrench) || aTool.equals(TOOL_rotator)) {
			if (aWorld.isClientSide()) return 0;
			int aMeta = WD.meta(aWorld, aX, aY, aZ);
			if ((aMeta & 7) >= 6) return 0;
			byte tSide = UT.Code.getSideWrenching(aSide, aHitX, aHitY, aHitZ);
			return (aMeta & 7) != tSide && WD.set(aWorld, aX, aY, aZ, this, (aMeta & 8) | tSide, 3) ? 2000 : 0;
		}
		return ToolCompat.onToolClick(this, aTool, aRemainingDurability, aQuality, aPlayer, aChatReturn, aPlayerInventory, aSneaking, aStack, aWorld, aSide, aX, aY, aZ, aHitX, aHitY, aHitZ);
	}
	
	/**
	 * BUG-076: форма шипа ИЗ СОСТОЯНИЯ (сторона крепления — младшие 3 бита меты, живут в BlockState-свойстве META).
	 *
	 * <p>Причина та же, что у решёток: neo строит BlockState-кэш формы на {@code EmptyBlockGetter}
	 * ({@code BlockBehaviour:916}), где мира нет, а 1.7.10-канал {@link #getCollisionBoundingBoxFromPool}
	 * читает мету ИЗ МИРА — в кэше это давало полный куб (замер: все 5 классов шипов). Координаты берутся
	 * из {@link #localBox(byte)} — единственного источника на класс, его же использует мировой канал.</p>
	 *
	 * <p>Outline оригинала — всегда полный куб (`:201` {@code getSelectedBoundingBoxFromPool}), поэтому из
	 * состояния отдаётся форма только для коллизии; для прицела возвращается {@code null} и мосты идут
	 * прежним путём (полный куб), как в 1.7.10.</p>
	 */
	@Override protected net.minecraft.world.phys.shapes.VoxelShape shapeFromState(net.minecraft.world.level.block.state.BlockState aState, boolean aCollision) {
		if (!aCollision) return null;
		return net.minecraft.world.phys.shapes.Shapes.create(localBox((byte)(getExtendedMetaData(aState) & 7)));
	}

	/** ЕДИНСТВЕННЫЙ источник формы шипа, локальные координаты 0..1 (1:1 оригинал `:182-190`). */
	private static AABB localBox(byte aSide) {
		switch (aSide) {
		case SIDE_X_POS: return new AABB(0.4  , 0    , 0    , 1    , 1    , 1    );
		case SIDE_Y_POS: return new AABB(0    , 0.4  , 0    , 1    , 1    , 1    );
		case SIDE_Z_POS: return new AABB(0    , 0    , 0.4  , 1    , 1    , 1    );
		case SIDE_X_NEG: return new AABB(0    , 0    , 0    , 0.6  , 1    , 1    );
		case SIDE_Y_NEG: return new AABB(0    , 0    , 0    , 1    , 0.6  , 1    );
		case SIDE_Z_NEG: return new AABB(0    , 0    , 0    , 1    , 1    , 0.6  );
		default        : return new AABB(0.125, 0.125, 0.125, 0.875, 0.875, 0.875);
		}
	}

	// @Override
	public AABB getCollisionBoundingBoxFromPool(Level aWorld, int aX, int aY, int aZ) {
		// BUG-076: координаты — из общего localBox(сторона), здесь только перенос в мировые.
		return localBox((byte)(WD.meta(aWorld, aX, aY, aZ) & 7)).move(aX, aY, aZ);
	}


	// @Override
	// было super.addCollisionBoxesToList(...) (1.7.10 Block, УДАЛЁН из neo целиком). Дефолт inline-порт 1:1 вместо
	// super-вызова (Block.java:661-669 recompSrc), тот же приём, что уже принят в MultiTileEntityBlock/BlockBaseLilyPad.
	public void addCollisionBoxesToList(Level aWorld, int aX, int aY, int aZ, AABB aAABB, @SuppressWarnings("rawtypes") List aList, Entity aEntity) {
		if (aEntity instanceof ItemEntity || aEntity instanceof ExperienceOrb || aEntity instanceof Projectile) return;
		AABB tBox = getCollisionBoundingBoxFromPool(aWorld, aX, aY, aZ);
		if (tBox != null && aAABB.intersects(tBox)) aList.add(tBox);
	}
	
	public AABB getSelectedBoundingBoxFromPool(Level aWorld, int aX, int aY, int aZ) {return new AABB(aX, aY, aZ, aX+1, aY+1, aZ+1);}
	public int getRenderType() {return RendererBlockTextured.INSTANCE==null?23:RendererBlockTextured.INSTANCE.mRenderID;}
	// F3 superseded-render (GT6BlockModel/ItemModel пайплайн; старый getIcon/immediate-mode мёртв, 0 вызовов neo): было Blocks.IRON_BARS.getIcon(2,0) (vanilla Block.getIcon удалён в 26.1.2 целиком).
	public Identifier getIcon(int aSide, int aMeta) {throw new UnsupportedOperationException("F3 dead-interface: 1.7.10 Block.getIcon(side,meta) удалён из neo (НЕ @Override, движок не зовёт). Рендер — через GT6BlockModel (IRenderedBlock.getTexture). Defensive throw.");}
	@Override public ITexture getTexture(int aRenderPass, byte aSide, ItemStack aStack) {return null;}
	@Override public ITexture getTexture(int aRenderPass, byte aSide, boolean[] aShouldSideBeRendered, BlockGetter aWorld, int aX, int aY, int aZ) {return null;}
	@Override public boolean usesRenderPass(int aRenderPass, ItemStack aStack) {return F;}
	@Override public boolean usesRenderPass(int aRenderPass, BlockGetter aWorld, int aX, int aY, int aZ, boolean[] aShouldSideBeRendered) {return F;}
	@Override public boolean setBlockBounds(int aRenderPass, ItemStack aStack) {return F;}
	@Override public boolean setBlockBounds(int aRenderPass, BlockGetter aWorld, int aX, int aY, int aZ, boolean[] aShouldSideBeRendered) {return F;}
	@Override public int getRenderPasses(ItemStack aStack) {return 0;}
	@Override public int getRenderPasses(BlockGetter aWorld, int aX, int aY, int aZ, boolean[] aShouldSideBeRendered) {return 0;}
	@Override public IRenderedBlockObject passRenderingToObject(ItemStack aStack) {return mRenderers[ST.meta_(aStack) & 15];}
	@Override public IRenderedBlockObject passRenderingToObject(BlockGetter aWorld, int aX, int aY, int aZ) {return mRenderers[WD.meta(aWorld, aX, aY, aZ)];}
	
	public SpikeRendererBase[] mRenderers = new SpikeRendererBase[16];
	
	public static abstract class SpikeRendererBase implements IRenderedBlockObject {
		public ITexture mTextureNormal, mTextureUsed;
		public SpikeRendererBase(OreDictMaterial aMat) {mTextureUsed = mTextureNormal = aMat.getTextureSmooth();}
		
		@Override public int getRenderPasses(Block aBlock, boolean[] aShouldSideBeRendered) {return APRIL_FOOLS ? 5 : 13;}
		@Override public ITexture getTexture(Block aBlock, int aRenderPass, byte aSide, boolean[] aShouldSideBeRendered) {return mTextureUsed;}
		@Override public boolean usesRenderPass(int aRenderPass, boolean[] aShouldSideBeRendered) {return T;}
		// F3 superseded-render (GT6BlockModel/ItemModel пайплайн; старый getIcon/immediate-mode мёртв, 0 вызовов neo): было RenderBlocks aRenderer (тип удалён в 26.1.2) — параметр Object,
		// тот же нейтральный держатель, что gregapi.render.IRenderedBlockObject#renderItem/renderBlock.
		@Override public boolean renderItem (Block aBlock, Object aRenderer) {return F;}
		@Override public boolean renderBlock(Block aBlock, Object aRenderer, BlockGetter aWorld, int aX, int aY, int aZ) {return F;}
		@Override public IRenderedBlockObject passRenderingToObject(ItemStack aStack) {mTextureUsed = mTextureNormal; return this;}
		@Override public IRenderedBlockObject passRenderingToObject(BlockGetter aWorld, int aX, int aY, int aZ) {mTextureUsed = (APRIL_FOOLS ? BlockTextureDefault.get(Textures.BlockIcons.CFOAM_HARDENED, RAINBOW_ARRAY[WD.random(42069, aX, aY, aZ, 12) * 2]) : mTextureNormal); return this;}
	}
	
	public static class SpikeRendererXPos extends SpikeRendererBase {
		public SpikeRendererXPos(OreDictMaterial aMat) {super(aMat);}
		@Override public ITexture getTexture(Block aBlock, int aRenderPass, byte aSide, boolean[] aShouldSideBeRendered) {return SIDE_X_POS == aSide && (aRenderPass != 0 || !aShouldSideBeRendered[aSide]) ? null : super.getTexture(aBlock, aRenderPass, aSide, aShouldSideBeRendered);}
		@Override
		public boolean setBlockBounds(Block aBlock, int aRenderPass, boolean[] aShouldSideBeRendered) {
			if (APRIL_FOOLS) switch(aRenderPass) {
			case  1: WD.setBlockBounds(aBlock, PX_P[ 5], PX_P[ 2], PX_P[ 2], PX_N[ 1], PX_P[ 7], PX_P[ 7]); return T;
			case  2: WD.setBlockBounds(aBlock, PX_P[ 5], PX_P[ 2], PX_P[ 9], PX_N[ 1], PX_P[ 7], PX_P[14]); return T;
			case  3: WD.setBlockBounds(aBlock, PX_P[ 5], PX_P[ 9], PX_P[ 2], PX_N[ 1], PX_P[14], PX_P[ 7]); return T;
			case  4: WD.setBlockBounds(aBlock, PX_P[ 5], PX_P[ 9], PX_P[ 9], PX_N[ 1], PX_P[14], PX_P[14]); return T;
			default: WD.setBlockBounds(aBlock, PX_P[ 8], PX_P[ 0], PX_P[ 0], PX_N[ 0], PX_N[ 0], PX_N[ 0]); return T;
			}
			switch(aRenderPass) {
			case  1: WD.setBlockBounds(aBlock, PX_P[ 1], PX_P[ 4], PX_P[ 4], PX_N[ 1], PX_P[ 5], PX_P[ 5]); return T;
			case  2: WD.setBlockBounds(aBlock, PX_P[ 1], PX_P[ 4], PX_P[11], PX_N[ 1], PX_P[ 5], PX_P[12]); return T;
			case  3: WD.setBlockBounds(aBlock, PX_P[ 1], PX_P[11], PX_P[ 4], PX_N[ 1], PX_P[12], PX_P[ 5]); return T;
			case  4: WD.setBlockBounds(aBlock, PX_P[ 1], PX_P[11], PX_P[11], PX_N[ 1], PX_P[12], PX_P[12]); return T;
			case  5: WD.setBlockBounds(aBlock, PX_P[ 5], PX_P[ 3], PX_P[ 3], PX_N[ 1], PX_P[ 6], PX_P[ 6]); return T;
			case  6: WD.setBlockBounds(aBlock, PX_P[ 5], PX_P[ 3], PX_P[10], PX_N[ 1], PX_P[ 6], PX_P[13]); return T;
			case  7: WD.setBlockBounds(aBlock, PX_P[ 5], PX_P[10], PX_P[ 3], PX_N[ 1], PX_P[13], PX_P[ 6]); return T;
			case  8: WD.setBlockBounds(aBlock, PX_P[ 5], PX_P[10], PX_P[10], PX_N[ 1], PX_P[13], PX_P[13]); return T;
			case  9: WD.setBlockBounds(aBlock, PX_P[ 9], PX_P[ 2], PX_P[ 2], PX_N[ 1], PX_P[ 7], PX_P[ 7]); return T;
			case 10: WD.setBlockBounds(aBlock, PX_P[ 9], PX_P[ 2], PX_P[ 9], PX_N[ 1], PX_P[ 7], PX_P[14]); return T;
			case 11: WD.setBlockBounds(aBlock, PX_P[ 9], PX_P[ 9], PX_P[ 2], PX_N[ 1], PX_P[14], PX_P[ 7]); return T;
			case 12: WD.setBlockBounds(aBlock, PX_P[ 9], PX_P[ 9], PX_P[ 9], PX_N[ 1], PX_P[14], PX_P[14]); return T;
			default: WD.setBlockBounds(aBlock, PX_P[14], PX_P[ 0], PX_P[ 0], PX_N[ 0], PX_N[ 0], PX_N[ 0]); return T;
			}
		}
	}
	public static class SpikeRendererXNeg extends SpikeRendererBase {
		public SpikeRendererXNeg(OreDictMaterial aMat) {super(aMat);}
		@Override public ITexture getTexture(Block aBlock, int aRenderPass, byte aSide, boolean[] aShouldSideBeRendered) {return SIDE_X_NEG == aSide && (aRenderPass != 0 || !aShouldSideBeRendered[aSide]) ? null : super.getTexture(aBlock, aRenderPass, aSide, aShouldSideBeRendered);}
		@Override
		public boolean setBlockBounds(Block aBlock, int aRenderPass, boolean[] aShouldSideBeRendered) {
			if (APRIL_FOOLS) switch(aRenderPass) {
			case  1: WD.setBlockBounds(aBlock, PX_P[ 1], PX_P[ 2], PX_P[ 2], PX_N[ 5], PX_P[ 7], PX_P[ 7]); return T;
			case  2: WD.setBlockBounds(aBlock, PX_P[ 1], PX_P[ 2], PX_P[ 9], PX_N[ 5], PX_P[ 7], PX_P[14]); return T;
			case  3: WD.setBlockBounds(aBlock, PX_P[ 1], PX_P[ 9], PX_P[ 2], PX_N[ 5], PX_P[14], PX_P[ 7]); return T;
			case  4: WD.setBlockBounds(aBlock, PX_P[ 1], PX_P[ 9], PX_P[ 9], PX_N[ 5], PX_P[14], PX_P[14]); return T;
			default: WD.setBlockBounds(aBlock, PX_P[ 0], PX_P[ 0], PX_P[ 0], PX_N[ 8], PX_N[ 0], PX_N[ 0]); return T;
			}
			switch(aRenderPass) {
			case  1: WD.setBlockBounds(aBlock, PX_P[ 1], PX_P[ 4], PX_P[ 4], PX_N[ 1], PX_P[ 5], PX_P[ 5]); return T;
			case  2: WD.setBlockBounds(aBlock, PX_P[ 1], PX_P[ 4], PX_P[11], PX_N[ 1], PX_P[ 5], PX_P[12]); return T;
			case  3: WD.setBlockBounds(aBlock, PX_P[ 1], PX_P[11], PX_P[ 4], PX_N[ 1], PX_P[12], PX_P[ 5]); return T;
			case  4: WD.setBlockBounds(aBlock, PX_P[ 1], PX_P[11], PX_P[11], PX_N[ 1], PX_P[12], PX_P[12]); return T;
			case  5: WD.setBlockBounds(aBlock, PX_P[ 1], PX_P[ 3], PX_P[ 3], PX_N[ 5], PX_P[ 6], PX_P[ 6]); return T;
			case  6: WD.setBlockBounds(aBlock, PX_P[ 1], PX_P[ 3], PX_P[10], PX_N[ 5], PX_P[ 6], PX_P[13]); return T;
			case  7: WD.setBlockBounds(aBlock, PX_P[ 1], PX_P[10], PX_P[ 3], PX_N[ 5], PX_P[13], PX_P[ 6]); return T;
			case  8: WD.setBlockBounds(aBlock, PX_P[ 1], PX_P[10], PX_P[10], PX_N[ 5], PX_P[13], PX_P[13]); return T;
			case  9: WD.setBlockBounds(aBlock, PX_P[ 1], PX_P[ 2], PX_P[ 2], PX_N[ 9], PX_P[ 7], PX_P[ 7]); return T;
			case 10: WD.setBlockBounds(aBlock, PX_P[ 1], PX_P[ 2], PX_P[ 9], PX_N[ 9], PX_P[ 7], PX_P[14]); return T;
			case 11: WD.setBlockBounds(aBlock, PX_P[ 1], PX_P[ 9], PX_P[ 2], PX_N[ 9], PX_P[14], PX_P[ 7]); return T;
			case 12: WD.setBlockBounds(aBlock, PX_P[ 1], PX_P[ 9], PX_P[ 9], PX_N[ 9], PX_P[14], PX_P[14]); return T;
			default: WD.setBlockBounds(aBlock, PX_P[ 0], PX_P[ 0], PX_P[ 0], PX_N[14], PX_N[ 0], PX_N[ 0]); return T;
			}
		}
	}
	public static class SpikeRendererYPos extends SpikeRendererBase {
		public SpikeRendererYPos(OreDictMaterial aMat) {super(aMat);}
		@Override public ITexture getTexture(Block aBlock, int aRenderPass, byte aSide, boolean[] aShouldSideBeRendered) {return SIDE_Y_POS == aSide && (aRenderPass != 0 || !aShouldSideBeRendered[aSide]) ? null : super.getTexture(aBlock, aRenderPass, aSide, aShouldSideBeRendered);}
		@Override
		public boolean setBlockBounds(Block aBlock, int aRenderPass, boolean[] aShouldSideBeRendered) {
			if (APRIL_FOOLS) switch(aRenderPass) {
			case  1: WD.setBlockBounds(aBlock, PX_P[ 2], PX_P[ 5], PX_P[ 2], PX_P[ 7], PX_N[ 1], PX_P[ 7]); return T;
			case  2: WD.setBlockBounds(aBlock, PX_P[ 2], PX_P[ 5], PX_P[ 9], PX_P[ 7], PX_N[ 1], PX_P[14]); return T;
			case  3: WD.setBlockBounds(aBlock, PX_P[ 9], PX_P[ 5], PX_P[ 2], PX_P[14], PX_N[ 1], PX_P[ 7]); return T;
			case  4: WD.setBlockBounds(aBlock, PX_P[ 9], PX_P[ 5], PX_P[ 9], PX_P[14], PX_N[ 1], PX_P[14]); return T;
			default: WD.setBlockBounds(aBlock, PX_P[ 0], PX_P[ 8], PX_P[ 0], PX_N[ 0], PX_N[ 0], PX_N[ 0]); return T;
			}
			switch(aRenderPass) {
			case  1: WD.setBlockBounds(aBlock, PX_P[ 4], PX_P[ 1], PX_P[ 4], PX_P[ 5], PX_N[ 1], PX_P[ 5]); return T;
			case  2: WD.setBlockBounds(aBlock, PX_P[ 4], PX_P[ 1], PX_P[11], PX_P[ 5], PX_N[ 1], PX_P[12]); return T;
			case  3: WD.setBlockBounds(aBlock, PX_P[11], PX_P[ 1], PX_P[ 4], PX_P[12], PX_N[ 1], PX_P[ 5]); return T;
			case  4: WD.setBlockBounds(aBlock, PX_P[11], PX_P[ 1], PX_P[11], PX_P[12], PX_N[ 1], PX_P[12]); return T;
			case  5: WD.setBlockBounds(aBlock, PX_P[ 3], PX_P[ 5], PX_P[ 3], PX_P[ 6], PX_N[ 1], PX_P[ 6]); return T;
			case  6: WD.setBlockBounds(aBlock, PX_P[ 3], PX_P[ 5], PX_P[10], PX_P[ 6], PX_N[ 1], PX_P[13]); return T;
			case  7: WD.setBlockBounds(aBlock, PX_P[10], PX_P[ 5], PX_P[ 3], PX_P[13], PX_N[ 1], PX_P[ 6]); return T;
			case  8: WD.setBlockBounds(aBlock, PX_P[10], PX_P[ 5], PX_P[10], PX_P[13], PX_N[ 1], PX_P[13]); return T;
			case  9: WD.setBlockBounds(aBlock, PX_P[ 2], PX_P[ 9], PX_P[ 2], PX_P[ 7], PX_N[ 1], PX_P[ 7]); return T;
			case 10: WD.setBlockBounds(aBlock, PX_P[ 2], PX_P[ 9], PX_P[ 9], PX_P[ 7], PX_N[ 1], PX_P[14]); return T;
			case 11: WD.setBlockBounds(aBlock, PX_P[ 9], PX_P[ 9], PX_P[ 2], PX_P[14], PX_N[ 1], PX_P[ 7]); return T;
			case 12: WD.setBlockBounds(aBlock, PX_P[ 9], PX_P[ 9], PX_P[ 9], PX_P[14], PX_N[ 1], PX_P[14]); return T;
			default: WD.setBlockBounds(aBlock, PX_P[ 0], PX_P[14], PX_P[ 0], PX_N[ 0], PX_N[ 0], PX_N[ 0]); return T;
			}
		}
	}
	public static class SpikeRendererYNeg extends SpikeRendererBase {
		public SpikeRendererYNeg(OreDictMaterial aMat) {super(aMat);}
		@Override public ITexture getTexture(Block aBlock, int aRenderPass, byte aSide, boolean[] aShouldSideBeRendered) {return SIDE_Y_NEG == aSide && (aRenderPass != 0 || !aShouldSideBeRendered[aSide]) ? null : super.getTexture(aBlock, aRenderPass, aSide, aShouldSideBeRendered);}
		@Override
		public boolean setBlockBounds(Block aBlock, int aRenderPass, boolean[] aShouldSideBeRendered) {
			if (APRIL_FOOLS) switch(aRenderPass) {
			case  1: WD.setBlockBounds(aBlock, PX_P[ 2], PX_P[ 1], PX_P[ 2], PX_P[ 7], PX_N[ 5], PX_P[ 7]); return T;
			case  2: WD.setBlockBounds(aBlock, PX_P[ 2], PX_P[ 1], PX_P[ 9], PX_P[ 7], PX_N[ 5], PX_P[14]); return T;
			case  3: WD.setBlockBounds(aBlock, PX_P[ 9], PX_P[ 1], PX_P[ 2], PX_P[14], PX_N[ 5], PX_P[ 7]); return T;
			case  4: WD.setBlockBounds(aBlock, PX_P[ 9], PX_P[ 1], PX_P[ 9], PX_P[14], PX_N[ 5], PX_P[14]); return T;
			default: WD.setBlockBounds(aBlock, PX_P[ 0], PX_P[ 0], PX_P[ 0], PX_N[ 0], PX_N[ 8], PX_N[ 0]); return T;
			}
			switch(aRenderPass) {
			case  1: WD.setBlockBounds(aBlock, PX_P[ 4], PX_P[ 1], PX_P[ 4], PX_P[ 5], PX_N[ 1], PX_P[ 5]); return T;
			case  2: WD.setBlockBounds(aBlock, PX_P[ 4], PX_P[ 1], PX_P[11], PX_P[ 5], PX_N[ 1], PX_P[12]); return T;
			case  3: WD.setBlockBounds(aBlock, PX_P[11], PX_P[ 1], PX_P[ 4], PX_P[12], PX_N[ 1], PX_P[ 5]); return T;
			case  4: WD.setBlockBounds(aBlock, PX_P[11], PX_P[ 1], PX_P[11], PX_P[12], PX_N[ 1], PX_P[12]); return T;
			case  5: WD.setBlockBounds(aBlock, PX_P[ 3], PX_P[ 1], PX_P[ 3], PX_P[ 6], PX_N[ 5], PX_P[ 6]); return T;
			case  6: WD.setBlockBounds(aBlock, PX_P[ 3], PX_P[ 1], PX_P[10], PX_P[ 6], PX_N[ 5], PX_P[13]); return T;
			case  7: WD.setBlockBounds(aBlock, PX_P[10], PX_P[ 1], PX_P[ 3], PX_P[13], PX_N[ 5], PX_P[ 6]); return T;
			case  8: WD.setBlockBounds(aBlock, PX_P[10], PX_P[ 1], PX_P[10], PX_P[13], PX_N[ 5], PX_P[13]); return T;
			case  9: WD.setBlockBounds(aBlock, PX_P[ 2], PX_P[ 1], PX_P[ 2], PX_P[ 7], PX_N[ 9], PX_P[ 7]); return T;
			case 10: WD.setBlockBounds(aBlock, PX_P[ 2], PX_P[ 1], PX_P[ 9], PX_P[ 7], PX_N[ 9], PX_P[14]); return T;
			case 11: WD.setBlockBounds(aBlock, PX_P[ 9], PX_P[ 1], PX_P[ 2], PX_P[14], PX_N[ 9], PX_P[ 7]); return T;
			case 12: WD.setBlockBounds(aBlock, PX_P[ 9], PX_P[ 1], PX_P[ 9], PX_P[14], PX_N[ 9], PX_P[14]); return T;
			default: WD.setBlockBounds(aBlock, PX_P[ 0], PX_P[ 0], PX_P[ 0], PX_N[ 0], PX_N[14], PX_N[ 0]); return T;
			}
		}
	}
	public static class SpikeRendererZPos extends SpikeRendererBase {
		public SpikeRendererZPos(OreDictMaterial aMat) {super(aMat);}
		@Override public ITexture getTexture(Block aBlock, int aRenderPass, byte aSide, boolean[] aShouldSideBeRendered) {return SIDE_Z_POS == aSide && (aRenderPass != 0 || !aShouldSideBeRendered[aSide]) ? null : super.getTexture(aBlock, aRenderPass, aSide, aShouldSideBeRendered);}
		@Override
		public boolean setBlockBounds(Block aBlock, int aRenderPass, boolean[] aShouldSideBeRendered) {
			if (APRIL_FOOLS) switch(aRenderPass) {
			case  1: WD.setBlockBounds(aBlock, PX_P[ 2], PX_P[ 2], PX_P[ 5], PX_P[ 7], PX_P[ 7], PX_N[ 1]); return T;
			case  2: WD.setBlockBounds(aBlock, PX_P[ 2], PX_P[ 9], PX_P[ 5], PX_P[ 7], PX_P[14], PX_N[ 1]); return T;
			case  3: WD.setBlockBounds(aBlock, PX_P[ 9], PX_P[ 2], PX_P[ 5], PX_P[14], PX_P[ 7], PX_N[ 1]); return T;
			case  4: WD.setBlockBounds(aBlock, PX_P[ 9], PX_P[ 9], PX_P[ 5], PX_P[14], PX_P[14], PX_N[ 1]); return T;
			default: WD.setBlockBounds(aBlock, PX_P[ 0], PX_P[ 0], PX_P[ 8], PX_N[ 0], PX_N[ 0], PX_N[ 0]); return T;
			}
			switch(aRenderPass) {
			case  1: WD.setBlockBounds(aBlock, PX_P[ 4], PX_P[ 4], PX_P[ 1], PX_P[ 5], PX_P[ 5], PX_N[ 1]); return T;
			case  2: WD.setBlockBounds(aBlock, PX_P[ 4], PX_P[11], PX_P[ 1], PX_P[ 5], PX_P[12], PX_N[ 1]); return T;
			case  3: WD.setBlockBounds(aBlock, PX_P[11], PX_P[ 4], PX_P[ 1], PX_P[12], PX_P[ 5], PX_N[ 1]); return T;
			case  4: WD.setBlockBounds(aBlock, PX_P[11], PX_P[11], PX_P[ 1], PX_P[12], PX_P[12], PX_N[ 1]); return T;
			case  5: WD.setBlockBounds(aBlock, PX_P[ 3], PX_P[ 3], PX_P[ 5], PX_P[ 6], PX_P[ 6], PX_N[ 1]); return T;
			case  6: WD.setBlockBounds(aBlock, PX_P[ 3], PX_P[10], PX_P[ 5], PX_P[ 6], PX_P[13], PX_N[ 1]); return T;
			case  7: WD.setBlockBounds(aBlock, PX_P[10], PX_P[ 3], PX_P[ 5], PX_P[13], PX_P[ 6], PX_N[ 1]); return T;
			case  8: WD.setBlockBounds(aBlock, PX_P[10], PX_P[10], PX_P[ 5], PX_P[13], PX_P[13], PX_N[ 1]); return T;
			case  9: WD.setBlockBounds(aBlock, PX_P[ 2], PX_P[ 2], PX_P[ 9], PX_P[ 7], PX_P[ 7], PX_N[ 1]); return T;
			case 10: WD.setBlockBounds(aBlock, PX_P[ 2], PX_P[ 9], PX_P[ 9], PX_P[ 7], PX_P[14], PX_N[ 1]); return T;
			case 11: WD.setBlockBounds(aBlock, PX_P[ 9], PX_P[ 2], PX_P[ 9], PX_P[14], PX_P[ 7], PX_N[ 1]); return T;
			case 12: WD.setBlockBounds(aBlock, PX_P[ 9], PX_P[ 9], PX_P[ 9], PX_P[14], PX_P[14], PX_N[ 1]); return T;
			default: WD.setBlockBounds(aBlock, PX_P[ 0], PX_P[ 0], PX_P[14], PX_N[ 0], PX_N[ 0], PX_N[ 0]); return T;
			}
		}
	}
	public static class SpikeRendererZNeg extends SpikeRendererBase {
		public SpikeRendererZNeg(OreDictMaterial aMat) {super(aMat);}
		@Override public ITexture getTexture(Block aBlock, int aRenderPass, byte aSide, boolean[] aShouldSideBeRendered) {return SIDE_Z_NEG == aSide && (aRenderPass != 0 || !aShouldSideBeRendered[aSide]) ? null : super.getTexture(aBlock, aRenderPass, aSide, aShouldSideBeRendered);}
		@Override
		public boolean setBlockBounds(Block aBlock, int aRenderPass, boolean[] aShouldSideBeRendered) {
			if (APRIL_FOOLS) switch(aRenderPass) {
			case  1: WD.setBlockBounds(aBlock, PX_P[ 2], PX_P[ 2], PX_P[ 1], PX_P[ 7], PX_P[ 7], PX_N[ 5]); return T;
			case  2: WD.setBlockBounds(aBlock, PX_P[ 2], PX_P[ 9], PX_P[ 1], PX_P[ 7], PX_P[14], PX_N[ 5]); return T;
			case  3: WD.setBlockBounds(aBlock, PX_P[ 9], PX_P[ 2], PX_P[ 1], PX_P[14], PX_P[ 7], PX_N[ 5]); return T;
			case  4: WD.setBlockBounds(aBlock, PX_P[ 9], PX_P[ 9], PX_P[ 1], PX_P[14], PX_P[14], PX_N[ 5]); return T;
			default: WD.setBlockBounds(aBlock, PX_P[ 0], PX_P[ 0], PX_P[ 0], PX_N[ 0], PX_N[ 0], PX_N[ 8]); return T;
			}
			switch(aRenderPass) {
			case  1: WD.setBlockBounds(aBlock, PX_P[ 4], PX_P[ 4], PX_P[ 1], PX_P[ 5], PX_P[ 5], PX_N[ 1]); return T;
			case  2: WD.setBlockBounds(aBlock, PX_P[ 4], PX_P[11], PX_P[ 1], PX_P[ 5], PX_P[12], PX_N[ 1]); return T;
			case  3: WD.setBlockBounds(aBlock, PX_P[11], PX_P[ 4], PX_P[ 1], PX_P[12], PX_P[ 5], PX_N[ 1]); return T;
			case  4: WD.setBlockBounds(aBlock, PX_P[11], PX_P[11], PX_P[ 1], PX_P[12], PX_P[12], PX_N[ 1]); return T;
			case  5: WD.setBlockBounds(aBlock, PX_P[ 3], PX_P[ 3], PX_P[ 1], PX_P[ 6], PX_P[ 6], PX_N[ 5]); return T;
			case  6: WD.setBlockBounds(aBlock, PX_P[ 3], PX_P[10], PX_P[ 1], PX_P[ 6], PX_P[13], PX_N[ 5]); return T;
			case  7: WD.setBlockBounds(aBlock, PX_P[10], PX_P[ 3], PX_P[ 1], PX_P[13], PX_P[ 6], PX_N[ 5]); return T;
			case  8: WD.setBlockBounds(aBlock, PX_P[10], PX_P[10], PX_P[ 1], PX_P[13], PX_P[13], PX_N[ 5]); return T;
			case  9: WD.setBlockBounds(aBlock, PX_P[ 2], PX_P[ 2], PX_P[ 1], PX_P[ 7], PX_P[ 7], PX_N[ 9]); return T;
			case 10: WD.setBlockBounds(aBlock, PX_P[ 2], PX_P[ 9], PX_P[ 1], PX_P[ 7], PX_P[14], PX_N[ 9]); return T;
			case 11: WD.setBlockBounds(aBlock, PX_P[ 9], PX_P[ 2], PX_P[ 1], PX_P[14], PX_P[ 7], PX_N[ 9]); return T;
			case 12: WD.setBlockBounds(aBlock, PX_P[ 9], PX_P[ 9], PX_P[ 1], PX_P[14], PX_P[14], PX_N[ 9]); return T;
			default: WD.setBlockBounds(aBlock, PX_P[ 0], PX_P[ 0], PX_P[ 0], PX_N[ 0], PX_N[ 0], PX_N[14]); return T;
			}
		}
	}
	public static class SpikeRendererOmni extends SpikeRendererBase {
		public SpikeRendererOmni(OreDictMaterial aMat) {super(aMat);}
		@Override public int getRenderPasses(Block aBlock, boolean[] aShouldSideBeRendered) {return 13;}
		@Override
		public boolean setBlockBounds(Block aBlock, int aRenderPass, boolean[] aShouldSideBeRendered) {
			switch(aRenderPass) {
			case  1: WD.setBlockBounds(aBlock, PX_P[ 0], PX_P[ 5], PX_P[ 5], PX_N[ 0], PX_P[ 6], PX_P[ 6]); return T;
			case  2: WD.setBlockBounds(aBlock, PX_P[ 0], PX_P[ 5], PX_P[10], PX_N[ 0], PX_P[ 6], PX_P[11]); return T;
			case  3: WD.setBlockBounds(aBlock, PX_P[ 0], PX_P[10], PX_P[ 5], PX_N[ 0], PX_P[11], PX_P[ 6]); return T;
			case  4: WD.setBlockBounds(aBlock, PX_P[ 0], PX_P[10], PX_P[10], PX_N[ 0], PX_P[11], PX_P[11]); return T;
			case  5: WD.setBlockBounds(aBlock, PX_P[ 5], PX_P[ 0], PX_P[ 5], PX_P[ 6], PX_N[ 0], PX_P[ 6]); return T;
			case  6: WD.setBlockBounds(aBlock, PX_P[ 5], PX_P[ 0], PX_P[10], PX_P[ 6], PX_N[ 0], PX_P[11]); return T;
			case  7: WD.setBlockBounds(aBlock, PX_P[10], PX_P[ 0], PX_P[ 5], PX_P[11], PX_N[ 0], PX_P[ 6]); return T;
			case  8: WD.setBlockBounds(aBlock, PX_P[10], PX_P[ 0], PX_P[10], PX_P[11], PX_N[ 0], PX_P[11]); return T;
			case  9: WD.setBlockBounds(aBlock, PX_P[ 5], PX_P[ 5], PX_P[ 0], PX_P[ 6], PX_P[ 6], PX_N[ 0]); return T;
			case 10: WD.setBlockBounds(aBlock, PX_P[ 5], PX_P[10], PX_P[ 0], PX_P[ 6], PX_P[11], PX_N[ 0]); return T;
			case 11: WD.setBlockBounds(aBlock, PX_P[10], PX_P[ 5], PX_P[ 0], PX_P[11], PX_P[ 6], PX_N[ 0]); return T;
			case 12: WD.setBlockBounds(aBlock, PX_P[10], PX_P[10], PX_P[ 0], PX_P[11], PX_P[11], PX_N[ 0]); return T;
			default: WD.setBlockBounds(aBlock, PX_P[ 4], PX_P[ 4], PX_P[ 4], PX_N[ 4], PX_N[ 4], PX_N[ 4]); return T;
			}
		}
	}
}

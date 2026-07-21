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

package gregapi.block.multitileentity;

import static gregapi.data.CS.*;

import gregapi.block.IBlock;
import gregapi.block.IBlockPlacable;
import gregapi.block.multitileentity.IMultiTileEntity.IMTE_HasMultiBlockMachineRelevantData;
import gregapi.block.multitileentity.IMultiTileEntity.IMTE_RegisterIcons;
import gregapi.item.IItemGT;
import gregapi.render.IRenderedBlock;
import gregapi.render.IRenderedBlockObject;
import gregapi.render.ITexture;
import gregapi.render.RendererBlockTextured;
import gregapi.tileentity.ITileEntity;
import gregapi.tileentity.ITileEntityMachineBlockUpdateable;
import gregapi.util.WD;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import gregapi.block.Material;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;

/**
 * @author Gregorius Techneticies
 */
public class MultiTileEntityBlockInternal extends Block implements IBlock, IItemGT, IRenderedBlock, IBlockPlacable {
	/** Диаг-счётчики П5 (mismatch-сироты): откаты placeBlock по гейтам «блок не встал». */
	public static final java.util.concurrent.atomic.AtomicLong sPlaceAbort1 = new java.util.concurrent.atomic.AtomicLong(), sPlaceAbort2 = new java.util.concurrent.atomic.AtomicLong();
	public MultiTileEntityRegistry mMultiTileEntityRegistry;

	public MultiTileEntityBlockInternal(String aNameInternal) {
		// F12-followup (block-split, MTE): setId в Properties (neo Block требует id, иначе «Block id not set»); namespace=GT
		// (gt.multitileentity — контент GT6, golden = gregtech:; совпадает с реестром ST.register→registerBlock). Конструкция на RegisterEvent через GT_API.deferBlockInit (call-site).
		// F-shape: dynamicShape() ОБЯЗАТЕЛЕН — иначе neo кэширует getCollisionShape (EmptyBlockGetter/ZERO) → per-BE
		// shape-мост (ниже) игнорируется, снег/коллизия из статического кэша = полный куб. См. MultiTileEntityBlock:165.
		super(net.minecraft.world.level.block.state.BlockBehaviour.Properties.of().dynamicShape().setId(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.BLOCK, net.minecraft.resources.Identifier.fromNamespaceAndPath(gregapi.data.CS.ModIDs.GT, gregapi.GT_API.sanitizeRegName(aNameInternal)))));
	}

	/** F-bounds (тот же приём, что BlockBase.java/MultiTileEntityBlock.java): последние заданные bounds, neo bounds
	 *  immutable -> храним сами, рендер-использование отложено на F3-клиент-проход. IBlock-обязательный метод. */
	protected float[] mRenderBounds = {0, 0, 0, 1, 1, 1};
	@Override public void setBlockBounds(float aMinX, float aMinY, float aMinZ, float aMaxX, float aMaxY, float aMaxZ) {
		mRenderBounds = new float[] {aMinX, aMinY, aMinZ, aMaxX, aMaxY, aMaxZ};
	}
	@Override public float[] getRenderBounds() {return mRenderBounds;}

	@Override public ITexture getTexture(int aRenderPass, byte aSide, ItemStack aStack) {return null;}
	@Override public ITexture getTexture(int aRenderPass, byte aSide, boolean[] aShouldSideBeRendered, BlockGetter aWorld, int aX, int aY, int aZ) {return null;}
	@Override public boolean setBlockBounds(int aRenderPass, ItemStack aStack) {return F;}
	@Override public boolean setBlockBounds(int aRenderPass, BlockGetter aWorld, int aX, int aY, int aZ, boolean[] aShouldSideBeRendered) {return F;}
	@Override public int getRenderPasses(ItemStack aStack) {return 0;}
	@Override public int getRenderPasses(BlockGetter aWorld, int aX, int aY, int aZ, boolean[] aShouldSideBeRendered) {return 0;}
	@Override public boolean usesRenderPass(int aRenderPass, ItemStack aStack) {return T;}
	@Override public boolean usesRenderPass(int aRenderPass, BlockGetter aWorld, int aX, int aY, int aZ, boolean[] aShouldSideBeRendered) {return T;}
	
	@Override
	public IRenderedBlockObject passRenderingToObject(BlockGetter aWorld, int aX, int aY, int aZ) {
		return null;
	}
	
	@Override
	public IRenderedBlockObject passRenderingToObject(ItemStack aStack) {
		BlockEntity tTileEntity = mMultiTileEntityRegistry.getNewTileEntity(aStack);
		return tTileEntity instanceof IRenderedBlockObject ? (IRenderedBlockObject)tTileEntity : null;
	}
	
	// @Override
	public void registerBlockIcons(Object aIconRegister) {
		for (MultiTileEntityClassContainer tClassContainer : mMultiTileEntityRegistry.mRegistry.values()) if (tClassContainer.mCanonicalTileEntity instanceof IMTE_RegisterIcons) ((IMTE_RegisterIcons)tClassContainer.mCanonicalTileEntity).registerIcons(aIconRegister);
	}
	
	public final int getRenderBlockPass() {return ITexture.Util.MC_ALPHA_BLENDING?1:0;}
	// F3-render (отложенная фаза): super.getRenderType() удалён из neo (рендер data-driven) -> -1; см. MultiTileEntityBlock:436.
	public final int getRenderType() {return RendererBlockTextured.INSTANCE==null?-1:RendererBlockTextured.INSTANCE.mRenderID;}
	@Override public final Block getBlock() {return this;}
	public final String getUnlocalizedName() {return mMultiTileEntityRegistry.mNameInternal;}
	public final String getLocalizedName() {return gregapi.lang.LanguageHandler.get(mMultiTileEntityRegistry.mNameInternal);}
	
	@Override
	public boolean placeBlock(net.minecraft.world.level.LevelAccessor aWorld, int aX, int aY, int aZ, byte aSide, short aMetaData, CompoundTag aNBT, boolean aCauseBlockUpdates, boolean aForcePlacement) {
		// F6-worldgen: приёмник расширен до LevelAccessor (контракт IBlockPlacable). Блок+BE ставятся через WD.set/WD.te (оба
		// на LevelAccessor: setBlock — LevelWriter, BE — ChunkAccess.setBlockEntity при worldgen) — MTE (флюид-спринги, резин-холы)
		// ГЕНЕРИРУЮТСЯ worldgen'ом, значит размещение обязано работать и на WorldGenLevel, а не только gameplay-Level. Level-специфичная
		// РЕАКТИВНОСТЬ (causeMachineUpdate/updateNeighborsAt — уведомление машин/соседей) при worldgen не нужна и невозможна → под гейтом instanceof Level.
		MultiTileEntityContainer aMTEContainer = mMultiTileEntityRegistry.getNewTileEntityContainer(aWorld, aX, aY, aZ, aMetaData, aNBT);
		if (aMTEContainer == null) return F;
		
		Block tReplacedBlock = WD.block(aWorld, aX, aY, aZ);
		
		
		// That is some complicated Bullshit I have to do to make my MTEs work right.
		// Set Block with reverse MetaData first.
		WD.set(aWorld, aX, aY, aZ, aMTEContainer.mBlock, 15-aMTEContainer.mBlockMetaData, 2);
		// Make sure the Block has been set, yes I know setBlock has a true/false return value, but guess what, it is not reliable in 0.0001% of cases!
		if (WD.block(aWorld, aX, aY, aZ) != aMTEContainer.mBlock) {sPlaceAbort1.incrementAndGet(); WD.set(aWorld, aX, aY, aZ, NB, 0, 0); return F;}
		// TileEntity should not refresh yet!
		((IMultiTileEntity)aMTEContainer.mTileEntity).setShouldRefresh(F);
		// Fake-Set the TileEntity first, bypassing a lot of checks.
		WD.te (aWorld, aX, aY, aZ, aMTEContainer.mTileEntity, F);
		// Now set the Block with the REAL MetaData.
		WD.set(aWorld, aX, aY, aZ, aMTEContainer.mBlock, aMTEContainer.mBlockMetaData, 0, F);
		// When the TileEntity is set now it SHOULD refresh!
		((IMultiTileEntity)aMTEContainer.mTileEntity).setShouldRefresh(T);
		// But make sure again that the Block we have set was actually set properly, because 0.0001%!
		// (диагноз mismatch-сирот: при откате BE уже приклеен fake-set'ом выше — снимаем, иначе BE-сирота в сейве)
		if (WD.block(aWorld, aX, aY, aZ) != aMTEContainer.mBlock) {sPlaceAbort2.incrementAndGet(); try {aWorld.getChunk(aX >> 4, aZ >> 4).removeBlockEntity(new BlockPos(aX, aY, aZ));} catch (Throwable e) {/**/} WD.set(aWorld, aX, aY, aZ, NB, 0, 0); return F;}
		// And finally properly set the TileEntity for real!
		WD.te (aWorld, aX, aY, aZ, aMTEContainer.mTileEntity, aCauseBlockUpdates);
		// Yep, all this just to set one Block and its TileEntity properly...
		// (кросс-чанк BE-персист регистрируется централизованно в WD.te — единственной точке привязки MTE-BE)
		
		
		try {
			// causeMachineUpdate — Level-only реактивность (уведомление многоблок-машин); при worldgen (не-Level) пропускаем.
			if (aWorld instanceof Level tLevelMU && aMTEContainer.mTileEntity instanceof IMTE_HasMultiBlockMachineRelevantData) {
				if (((IMTE_HasMultiBlockMachineRelevantData)aMTEContainer.mTileEntity).hasMultiBlockMachineRelevantData()) ITileEntityMachineBlockUpdateable.Util.causeMachineUpdate(tLevelMU, aX, aY, aZ, aMTEContainer.mBlock, aMTEContainer.mBlockMetaData, F);
			}
		} catch(Throwable e) {e.printStackTrace(ERR);}
		try {
			// updateNeighborsAt — Level-only; при worldgen (не-Level) уведомление соседей не нужно/невозможно (регион ещё генерится).
			if (aWorld instanceof Level tLevelNU && !tLevelNU.isClientSide() && aCauseBlockUpdates) {
				// было World.notifyBlockChange(x,y,z,Block) -> тело делегировало notifyBlocksOfNeighborChange (recompSrc
				// World.java:695-698) -> Level.updateNeighborsAt(BlockPos,Block,Orientation) [Level.java:338], тот же
				// форс-эквивалент, что уже принят для соседнего func_147453_f ниже (см. decisions/DEFERRED-LEDGER.md §B).
				tLevelNU.updateNeighborsAt(new BlockPos(aX, aY, aZ), tReplacedBlock, null);
				// было World.func_147453_f(x,y,z,Block) -> Level.updateNeighborsAt(BlockPos,Block,Orientation) [Level.java:338]
				tLevelNU.updateNeighborsAt(new BlockPos(aX, aY, aZ), aMTEContainer.mBlock, null);
			}
		} catch(Throwable e) {e.printStackTrace(ERR);}
		try {
			if (aMTEContainer.mTileEntity instanceof ITileEntity) {
				((ITileEntity)aMTEContainer.mTileEntity).onTileEntityPlaced();
			}
		} catch(Throwable e) {e.printStackTrace(ERR);}
		try {
			// было World.func_147451_t(x,y,z) (пересчёт Sky+Block light в позиции, recompSrc World.java:3268-3279) ->
			// neo Level.getLightEngine().checkBlock(BlockPos) [LevelLightEngine.java:32], тот же приём (пересчёт обоих
			// типов света для позиции), движковая замена per-type updateLightByType-цикла.
			aWorld.getLightEngine().checkBlock(new BlockPos(aX, aY, aZ));
		} catch(Throwable e) {e.printStackTrace(ERR);}
		return T;
	}

	// F-shape (см. MultiTileEntityBlock:265 — тот же приём для ВТОРОЙ MTE-блок-иерархии Internal; обе extends vanilla Block,
	// общего GT6-предка нет → мост дублируется, как useOn-мост на корнях-предметах). BE-AABB (абсолютная, box()=pos+bounds) →
	// относительный VoxelShape; null коллизия (MTE-Rock) → empty (снег не ляжет, камешек проходим). getShape — маленький outline.
	@Override protected net.minecraft.world.phys.shapes.VoxelShape getCollisionShape(net.minecraft.world.level.block.state.BlockState aState, net.minecraft.world.level.BlockGetter aWorld, BlockPos aPos, net.minecraft.world.phys.shapes.CollisionContext aContext) {
		if (aWorld instanceof Level tLevel) {
			BlockEntity tBE = WD.te(tLevel, aPos.getX(), aPos.getY(), aPos.getZ(), T);
			// Порядок диспатча = 1:1 с 1.7.10 addCollisionBoxesToList: СНАЧАЛА список под-боксов (леса/верёвка = проходимая
			// рама, трубы = рукава), фолбэк — broad-phase бокс. Зеркало MultiTileEntityBlock.getCollisionShape.
			if (tBE instanceof gregapi.block.multitileentity.IMultiTileEntity.IMTE_AddCollisionBoxesToList tMulti) {
				java.util.List<net.minecraft.world.phys.AABB> tList = new java.util.ArrayList<>();
				tMulti.addCollisionBoxesToList(new net.minecraft.world.phys.AABB(aPos.getX()-1, aPos.getY()-1, aPos.getZ()-1, aPos.getX()+2, aPos.getY()+2, aPos.getZ()+2), tList, aContext instanceof net.minecraft.world.phys.shapes.EntityCollisionContext tEntityContext ? tEntityContext.getEntity() : null);
				net.minecraft.world.phys.shapes.VoxelShape rShape = net.minecraft.world.phys.shapes.Shapes.empty();
				for (net.minecraft.world.phys.AABB tBox : tList) if (tBox != null) rShape = net.minecraft.world.phys.shapes.Shapes.or(rShape, net.minecraft.world.phys.shapes.Shapes.create(tBox.move(-aPos.getX(), -aPos.getY(), -aPos.getZ())));
				return rShape;
			}
			if (tBE instanceof gregapi.block.multitileentity.IMultiTileEntity.IMTE_GetCollisionBoundingBoxFromPool tC) {
				net.minecraft.world.phys.AABB tBox = tC.getCollisionBoundingBoxFromPool();
				return tBox == null ? net.minecraft.world.phys.shapes.Shapes.empty() : net.minecraft.world.phys.shapes.Shapes.create(tBox.move(-aPos.getX(), -aPos.getY(), -aPos.getZ()));
			}
		}
		return super.getCollisionShape(aState, aWorld, aPos, aContext);
	}
	@Override protected net.minecraft.world.phys.shapes.VoxelShape getShape(net.minecraft.world.level.block.state.BlockState aState, net.minecraft.world.level.BlockGetter aWorld, BlockPos aPos, net.minecraft.world.phys.shapes.CollisionContext aContext) {
		if (aWorld instanceof Level tLevel) {
			BlockEntity tBE = WD.te(tLevel, aPos.getX(), aPos.getY(), aPos.getZ(), T);
			if (tBE instanceof gregapi.block.multitileentity.IMultiTileEntity.IMTE_GetSelectedBoundingBoxFromPool tS) {
				net.minecraft.world.phys.AABB tBox = tS.getSelectedBoundingBoxFromPool();
				if (tBox != null) return net.minecraft.world.phys.shapes.Shapes.create(tBox.move(-aPos.getX(), -aPos.getY(), -aPos.getZ()));
			}
		}
		return super.getShape(aState, aWorld, aPos, aContext);
	}
}

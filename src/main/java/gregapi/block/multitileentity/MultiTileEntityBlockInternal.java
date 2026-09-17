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
 *
 * Modified in 2026 for the GregTech 6 NeoForge port
 * (https://github.com/wolfram0108/gregtech6_w): ported from Minecraft 1.7.10 / Forge
 * to Minecraft 26.1.2 / NeoForge.
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
	/** Diagnostic counters tracking how often block placement aborts because the block never actually landed. */
	public static final java.util.concurrent.atomic.AtomicLong sPlaceAbort1 = new java.util.concurrent.atomic.AtomicLong(), sPlaceAbort2 = new java.util.concurrent.atomic.AtomicLong();
	public MultiTileEntityRegistry mMultiTileEntityRegistry;

	public MultiTileEntityBlockInternal(String aNameInternal) {
		// dynamicShape() is mandatory, or neo caches an empty-world collision shape and every per-tile shape bridge below
		// is ignored; map color is also restored explicitly here, since neo's own default shows no color at all.
		super(gregapi.block.BlockBase.mapColorOf(net.minecraft.world.level.block.state.BlockBehaviour.Properties.of().dynamicShape(), gregapi.block.Material.anvil));
	}

	/** Same bounds-storage technique as BlockBase and MultiTileEntityBlock, since neo's bounds are immutable and render use is
	 *  deferred to a later pass. */
	protected float[] mRenderBounds = {0, 0, 0, 1, 1, 1};
	@Override public void setBlockBounds(float aMinX, float aMinY, float aMinZ, float aMaxX, float aMaxY, float aMaxZ) {
		mRenderBounds = new float[] {aMinX, aMinY, aMinZ, aMaxX, aMaxY, aMaxZ};
	}
	@Override public float[] getRenderBounds() {return mRenderBounds;}

	/** Own material field instead of the removed vanilla Block.blockMaterial, the same technique
	 *  already used by BlockBaseRail and BlockBaseFlower, carrying the same anvil material passed to mapColorOf above. */
	protected final gregapi.block.Material mMaterial = gregapi.block.Material.anvil;
	public gregapi.block.Material getMaterial() {return mMaterial;}

	// In 1.7.10 this counted as a normal cube and shaded neighbors; neo's shape is dynamic (driven
	// by the tile entity), so its own collision-based default would wrongly give full brightness instead.
	@Override public float getShadeBrightness(net.minecraft.world.level.block.state.BlockState aState, BlockGetter aWorld, net.minecraft.core.BlockPos aPos) {return gregapi.data.CS.shadeBrightness(isBlockNormalCube());}

	/** Body 1:1 with 1.7.10's Block.isBlockNormalCube; see BlockBase for the same method. */
	public boolean isBlockNormalCube() {return mMaterial.blocksMovement() && renderAsNormalBlock();}

	/** The original never overrode this; it is just vanilla 1.7.10's own default, carried over explicitly. */
	public boolean renderAsNormalBlock() {return T;}

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
		// The single center compensating item-facing for all MTE content: 1.7.10 rotated every rendered item's geometry
		// uniformly, which neo's baked-quad pipeline can't do, so this substitutes facing only where texture depends on it.
		MultiTileEntityRegistry.applyItemFacing(tTileEntity);
		return tTileEntity instanceof IRenderedBlockObject ? (IRenderedBlockObject)tTileEntity : null;
	}
	
	// @Override
	public void registerBlockIcons(Object aIconRegister) {
		for (MultiTileEntityClassContainer tClassContainer : mMultiTileEntityRegistry.mRegistry.values()) if (tClassContainer.mCanonicalTileEntity instanceof IMTE_RegisterIcons) ((IMTE_RegisterIcons)tClassContainer.mCanonicalTileEntity).registerIcons(aIconRegister);
	}
	
	public final int getRenderBlockPass() {return ITexture.Util.MC_ALPHA_BLENDING?1:0;}
	// super.getRenderType() no longer exists since neo's rendering is data-driven, so this returns -1 instead.
	public final int getRenderType() {return RendererBlockTextured.INSTANCE==null?-1:RendererBlockTextured.INSTANCE.mRenderID;}
	@Override public final Block getBlock() {return this;}
	public final String getUnlocalizedName() {return mMultiTileEntityRegistry.mNameInternal;}
	public final String getLocalizedName() {return gregapi.lang.LanguageHandler.get(mMultiTileEntityRegistry.mNameInternal);}
	
	@Override
	public boolean placeBlock(net.minecraft.world.level.LevelAccessor aWorld, int aX, int aY, int aZ, byte aSide, short aMetaData, CompoundTag aNBT, boolean aCauseBlockUpdates, boolean aForcePlacement) {
		// Widened to accept LevelAccessor, not just Level, since some MTE content is placed by worldgen on a WorldGenLevel;
		// machine/neighbor-update reactivity is gated behind an instanceof Level check since it makes no sense during generation.
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
		// Removes the tile entity a fake set already attached before rolling back, or it would be orphaned in the save.
		if (WD.block(aWorld, aX, aY, aZ) != aMTEContainer.mBlock) {sPlaceAbort2.incrementAndGet(); try {aWorld.getChunk(aX >> 4, aZ >> 4).removeBlockEntity(new BlockPos(aX, aY, aZ));} catch (Throwable e) {/**/} WD.set(aWorld, aX, aY, aZ, NB, 0, 0); return F;}
		// And finally properly set the TileEntity for real!
		WD.te (aWorld, aX, aY, aZ, aMTEContainer.mTileEntity, aCauseBlockUpdates);
		// Yep, all this just to set one Block and its TileEntity properly...
		// Cross-chunk tile-entity persistence is registered centrally in WD.te, the single attachment point for MTE tile entities.
		
		
		try {
			// Multi-block machine notification only makes sense on a real Level, so it is skipped during worldgen.
			if (aWorld instanceof Level tLevelMU && aMTEContainer.mTileEntity instanceof IMTE_HasMultiBlockMachineRelevantData) {
				if (((IMTE_HasMultiBlockMachineRelevantData)aMTEContainer.mTileEntity).hasMultiBlockMachineRelevantData()) ITileEntityMachineBlockUpdateable.Util.causeMachineUpdate(tLevelMU, aX, aY, aZ, aMTEContainer.mBlock, aMTEContainer.mBlockMetaData, F);
			}
		} catch(Throwable e) {e.printStackTrace(ERR);}
		try {
			// Neighbor notification is Level-only too, since it is neither needed nor possible while the region is still generating.
			if (aWorld instanceof Level tLevelNU && !tLevelNU.isClientSide() && aCauseBlockUpdates) {
				// Was World.notifyBlockChange(x,y,z,Block) -> Level.updateNeighborsAt(BlockPos,Block), the same
				// force-equivalent already accepted for the neighboring func_147453_f call below.
				tLevelNU.updateNeighborsAt(new BlockPos(aX, aY, aZ), tReplacedBlock);
				// Was World.func_147453_f(x,y,z,Block) -> Level.updateNeighborsAt(BlockPos,Block).
				tLevelNU.updateNeighborsAt(new BlockPos(aX, aY, aZ), aMTEContainer.mBlock);
			}
		} catch(Throwable e) {e.printStackTrace(ERR);}
		try {
			if (aMTEContainer.mTileEntity instanceof ITileEntity) {
				((ITileEntity)aMTEContainer.mTileEntity).onTileEntityPlaced();
			}
		} catch(Throwable e) {e.printStackTrace(ERR);}
		try {
			// neo's light engine checkBlock replaces the old sky+block light recalculation, recomputing both light types for a
			// position the same way.
			aWorld.getLightEngine().checkBlock(new BlockPos(aX, aY, aZ));
		} catch(Throwable e) {e.printStackTrace(ERR);}
		return T;
	}

	// Bridged per-tile-entity hardness into the IBlock contract, mirroring MultiTileEntityBlock; without it,
	// the default baked destroy time of 0 would mean zero tool wear.
	public float getBlockHardness(Level aWorld, int aX, int aY, int aZ) {BlockEntity tBE = WD.te(aWorld, aX, aY, aZ, T); return tBE instanceof gregapi.block.multitileentity.IMultiTileEntity.IMTE_GetBlockHardness tH ? tH.getBlockHardness() : 1.0F;}
	// Mirrors MultiTileEntityBlock.getDestroyProgress; without it the baked destroy time defaults to
	// 0 and the block breaks instantly by hand, instead of reading per-tile-entity hardness from NBT.
	@Override public float getDestroyProgress(net.minecraft.world.level.block.state.BlockState aState, net.minecraft.world.entity.player.Player aPlayer, net.minecraft.world.level.BlockGetter aWorld, BlockPos aPos) {
		BlockEntity tBE = WD.te(aWorld, aPos.getX(), aPos.getY(), aPos.getZ(), T);
		float tHardness = tBE instanceof gregapi.block.multitileentity.IMultiTileEntity.IMTE_GetBlockHardness tH ? tH.getBlockHardness() : 1.0F;
		float tOriginal = WD.destroyProgress(tHardness, aPlayer, aState, aWorld, aPos); // the vanilla formula lives in the single center WD.destroyProgress
		return tBE instanceof gregapi.block.multitileentity.IMultiTileEntity.IMTE_GetPlayerRelativeBlockHardness tP ? tP.getPlayerRelativeBlockHardness(aPlayer, tOriginal) : tOriginal;
	}
	// Duplicated here rather than shared, since this is the second MTE block hierarchy and both extend vanilla Block directly
	// with no common GT6 ancestor; a null collision box (rock pebbles) becomes an empty shape, so they stay passable.
	@Override public net.minecraft.world.phys.shapes.VoxelShape getCollisionShape(net.minecraft.world.level.block.state.BlockState aState, net.minecraft.world.level.BlockGetter aWorld, BlockPos aPos, net.minecraft.world.phys.shapes.CollisionContext aContext) {
		// Deliberately has no instanceof Level gate, unlike its mirror: the engine also calls this with a
		// chunk-scoped BlockGetter, and gating it used to fall back to a full cube, pushing entities out of passable MTE blocks.
		if (aWorld != null) {
			BlockEntity tBE = WD.te(aWorld, aPos.getX(), aPos.getY(), aPos.getZ(), T);
			// Checks the per-tile sub-box list first (scaffolding, pipes), falling back to the broad-phase box,
			// matching 1.7.10's dispatch order exactly, the same as MultiTileEntityBlock's own mirror.
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
	@Override public net.minecraft.world.phys.shapes.VoxelShape getShape(net.minecraft.world.level.block.state.BlockState aState, net.minecraft.world.level.BlockGetter aWorld, BlockPos aPos, net.minecraft.world.phys.shapes.CollisionContext aContext) {
		if (aWorld instanceof Level tLevel) {
			BlockEntity tBE = WD.te(tLevel, aPos.getX(), aPos.getY(), aPos.getZ(), T);
			if (tBE instanceof gregapi.block.multitileentity.IMultiTileEntity.IMTE_GetSelectedBoundingBoxFromPool tS) {
				net.minecraft.world.phys.AABB tBox = tS.getSelectedBoundingBoxFromPool();
				if (tBox != null) return net.minecraft.world.phys.shapes.Shapes.create(tBox.move(-aPos.getX(), -aPos.getY(), -aPos.getZ()));
			}
		}
		return super.getShape(aState, aWorld, aPos, aContext);
	}

	/** Harvest permission is judged by the center WD.canHarvestBlock; this is only the call site.
	 *  The rule moved here from PlayerEvent.HarvestCheck, which in 1.20.1 carries neither world nor position. */
	@Override public boolean canHarvestBlock(net.minecraft.world.level.block.state.BlockState aState, net.minecraft.world.level.BlockGetter aWorld, net.minecraft.core.BlockPos aPos, net.minecraft.world.entity.player.Player aPlayer) {
		return gregapi.util.WD.canHarvestBlock(aState, aWorld, aPos, aPlayer);
	}

}

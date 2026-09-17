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
 *
 * Modified in 2026 for the GregTech 6 NeoForge port
 * (https://github.com/wolfram0108/gregtech6_w): ported from Minecraft 1.7.10 / Forge
 * to Minecraft 26.1.2 / NeoForge.
 */

package gregapi.block.metatype;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.Direction;

import static gregapi.data.CS.*;

import java.util.List;
import java.util.Random;

import net.minecraftforge.api.distmarker.Dist;
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
	/** WATERLOGGED must apply only to slabs, but the property is added from super() before mIsSlab
	 *  is even known, so a thread-local flag is set only around slab construction to decide it correctly. */
	private static final ThreadLocal<boolean[]> SLAB_CTOR_CTX = ThreadLocal.withInitial(() -> new boolean[1]);
	
	public BlockMetaType(Class<? extends BlockItem> aItemClass, Material aVanillaMaterial, SoundType aSoundType, String aNameInternal, String aDefaultLocalised, OreDictMaterial aMaterial, float aResistanceMultiplier, float aHardnessMultiplier, int aHarvestLevel, int aCount, IIconContainer[] aIcons) {
		super(aItemClass == null ? ItemBlockMetaType.class : aItemClass, aNameInternal, aVanillaMaterial, aSoundType, aCount, aIcons);
		if (aItemClass == null) aItemClass = ItemBlockMetaType.class;
		onBlockCreation(aItemClass, aVanillaMaterial, aSoundType, aNameInternal, aDefaultLocalised, aMaterial, aResistanceMultiplier, aHardnessMultiplier, aHarvestLevel, aCount, aIcons);
		// neo's Properties.strength is immutable, unlike 1.7.10's post-construction setHardness/
		// setResistance mutators, so hardness and explosion resistance are wired through BlockBase's overridable hooks instead.
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
		boolean[] tSlabCtx = SLAB_CTOR_CTX.get(); tSlabCtx[0] = true; // WATERLOGGED only for slabs; see SLAB_CTOR_CTX
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
		// Slabs are built inside the constructor (makeSlab), not through a registerBlockLazy call site, so
		// nothing registers their neo Block; each unique slab is registered through the center GT_API.registerBlockOnly instead.
		{
			java.util.Set<Object> tSeenSlabs = new java.util.HashSet<>();
			for (BlockMetaType tSlab : mSlabs) if (tSlab != null && tSeenSlabs.add(tSlab)) {
				gregapi.GT_API.registerBlockOnly(tSlab, tSlab.mNameInternal);
			}
		}
		// Block construction runs during the registry event, but building an ItemStack needs components
		// that only exist at server start, so this data part is deferred to deferItemInit instead, keeping the original order.
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
		// Same hardness/explosion-resistance wiring as the main constructor above.
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
		registerDefaultState(defaultBlockState().setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED, Boolean.FALSE)); // explicit default, since any() does not guarantee false
		setBlockBounds(
		mSide == SIDE_X_POS ? 0.5F : 0.0F,
		mSide == SIDE_Y_POS ? 0.5F : 0.0F,
		mSide == SIDE_Z_POS ? 0.5F : 0.0F,
		mSide == SIDE_X_NEG ? 0.5F : 1.0F,
		mSide == SIDE_Y_NEG ? 0.5F : 1.0F,
		mSide == SIDE_Z_NEG ? 0.5F : 1.0F
		);
		// Deferred to server start, since building this ItemStack needs components that don't exist yet.
		gregapi.GT_API.deferItemInit(() -> {if (COMPAT_FR != null) COMPAT_FR.addToBackpacks("builder", ST.make(this, 1, W));});
	}
	
	// The slab's bounds are static, but the render anti-leak logic resets the shared Block instance's
	// fields after every meshing pass, so the next pass drew a full cube; the fix re-asserts the static bounds on every pass.
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
	// BlockBase's shape bridges read the racy mRenderBounds field directly, so the slab needs its
	// own shape that doesn't depend on those fields at all.
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
	@Override public net.minecraft.world.phys.shapes.VoxelShape getShape(net.minecraft.world.level.block.state.BlockState aState, net.minecraft.world.level.BlockGetter aWorld, net.minecraft.core.BlockPos aPos, net.minecraft.world.phys.shapes.CollisionContext aContext) {
		return mIsSlab ? slabShape() : super.getShape(aState, aWorld, aPos, aContext);
	}
	@Override public net.minecraft.world.phys.shapes.VoxelShape getCollisionShape(net.minecraft.world.level.block.state.BlockState aState, net.minecraft.world.level.BlockGetter aWorld, net.minecraft.core.BlockPos aPos, net.minecraft.world.phys.shapes.CollisionContext aContext) {
		return mIsSlab ? slabShape() : super.getCollisionShape(aState, aWorld, aPos, aContext);
	}

	// Deliberate deviation from 1.7.10, which had no waterlogging: slabs now behave like modern
	// vanilla ones, holding water via the WATERLOGGED property, which only slabs get, gated by the same flag context.
	@Override protected void createBlockStateDefinition(net.minecraft.world.level.block.state.StateDefinition.Builder<net.minecraft.world.level.block.Block, net.minecraft.world.level.block.state.BlockState> aBuilder) {
		super.createBlockStateDefinition(aBuilder);
		if (SLAB_CTOR_CTX.get()[0]) aBuilder.add(net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED);
	}
	@Override public boolean canPlaceLiquid(net.minecraft.world.level.BlockGetter aWorld, net.minecraft.core.BlockPos aPos, net.minecraft.world.level.block.state.BlockState aState, net.minecraft.world.level.material.Fluid aFluid) {
		return mIsSlab && net.minecraft.world.level.block.SimpleWaterloggedBlock.super.canPlaceLiquid(aWorld, aPos, aState, aFluid);
	}
	@Override public net.minecraft.world.level.material.FluidState getFluidState(net.minecraft.world.level.block.state.BlockState aState) {
		return aState.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED) && aState.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED)
			? net.minecraft.world.level.material.Fluids.WATER.getSource(false) : super.getFluidState(aState);
	}
	// 1.20.1: updateShape schedules its tick directly through LevelAccessor, since there's no separate
	// ScheduledTickAccess type yet, matching vanilla's own SlabBlock on this version.
	@Override public net.minecraft.world.level.block.state.BlockState updateShape(net.minecraft.world.level.block.state.BlockState aState, net.minecraft.core.Direction aDir, net.minecraft.world.level.block.state.BlockState aNeighbourState, net.minecraft.world.level.LevelAccessor aWorld, net.minecraft.core.BlockPos aPos, net.minecraft.core.BlockPos aNeighbourPos) {
		if (aState.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED) && aState.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED))
			aWorld.scheduleTick(aPos, net.minecraft.world.level.material.Fluids.WATER, net.minecraft.world.level.material.Fluids.WATER.getTickDelay(aWorld));
		return super.updateShape(aState, aDir, aNeighbourState, aWorld, aPos, aNeighbourPos);
	}

	// Slab rendering must not depend on the racy shared fields at all, since its bounds are static;
	// they are returned directly instead.
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
	
	// 1.20.1 has one click channel, BlockBehaviour.use, matching 1.7.10's onBlockActivated exactly (no split
	// into useItemOn/useWithoutItem); without this bridge two GT6 half-slabs never combined into a whole block on click.
	@Override public net.minecraft.world.InteractionResult use(net.minecraft.world.level.block.state.BlockState aState, Level aWorld, net.minecraft.core.BlockPos aPos, Player aPlayer, net.minecraft.world.InteractionHand aHand, net.minecraft.world.phys.BlockHitResult aHit) {
		if (aHand == net.minecraft.world.InteractionHand.MAIN_HAND && bridgeBlockActivated(aWorld, aPos, aPlayer, aHit))
			return net.minecraft.world.InteractionResult.SUCCESS;
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
	
	// neo's skipRendering drops the neighbor's position, which this branch never actually needed,
	// since the neighbor block itself is enough here.
	/** neo asks face visibility through a pair of BlockState with no world, orphaning descendants that kept the 1.7.10
	 *  signature -- adjacent GT6 glass drew a seam between panes that should merge, fixed by asking this once here instead. */
	@Override
	public boolean skipRendering(BlockState aState, BlockState aNeighbor, Direction aDir) {
		byte aSide = UT.Code.side(aDir);
		// checks the family rule (glass, paths) first, expressed through the contract above
		if (!shouldSideBeRendered(aState, aNeighbor, aSide)) return T;
		if (aSide == OPOS[mSide]) return F;
		if (aSide != mSide && SIDES_VALID[mSide]) {
			Block aBlock = aNeighbor.getBlock();
			// getRenderBlockPass never had a real override in 1.7.10, so it always resolved to vanilla's default 0.
			// The original meant true=draw; here the contract is inverted (true=hide), so the check must be "== 0", not "!= 0".
			if (aBlock instanceof BlockMetaType && ((BlockMetaType)aBlock).mSide == mSide) return ((BlockMetaType)aBlock).getRenderBlockPass() == 0;
		}
		return super.skipRendering(aState, aNeighbor, aDir);
	}
	public int getRenderBlockPass() {return 0;}
	
	@Override public String getHarvestTool(int aMeta) {return TOOL_pickaxe;}
	@Override public int getHarvestLevel(int aMeta) {return mHarvestLevel;}
	@Override public float getBlockHardness(Level aWorld, int aX, int aY, int aZ) {return WD.hardness(Blocks.STONE, aWorld, aX, aY, aZ) * mHardnessMultiplier;}
	// Calling with a null Entity is replaced by the no-arg overload, the same fallback already used for the equivalent generic
	// call in WD.scan.
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

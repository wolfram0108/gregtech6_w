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
import net.neoforged.api.distmarker.OnlyIn;
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

public class BlockMetaType extends BlockBaseMeta {
	public final float mHardnessMultiplier, mResistanceMultiplier;
	public final int mHarvestLevel;
	public final byte mSide, mOctantcount;
	public final boolean mIsWall, mIsSlab, mIsStair, mIsPrimary;
	public final BlockMetaType mBlock;
	public final BlockMetaType[] mSlabs;
	
	public BlockMetaType(Class<? extends BlockItem> aItemClass, Material aVanillaMaterial, SoundType aSoundType, String aNameInternal, String aDefaultLocalised, OreDictMaterial aMaterial, float aResistanceMultiplier, float aHardnessMultiplier, int aHarvestLevel, int aCount, IIconContainer[] aIcons) {
		super(aItemClass == null ? ItemBlockMetaType.class : aItemClass, aNameInternal, aVanillaMaterial, aSoundType, aCount, aIcons);
		if (aItemClass == null) aItemClass = ItemBlockMetaType.class;
		onBlockCreation(aItemClass, aVanillaMaterial, aSoundType, aNameInternal, aDefaultLocalised, aMaterial, aResistanceMultiplier, aHardnessMultiplier, aHarvestLevel, aCount, aIcons);
		// PORT-TODO(F12, block-property-runtime-mutator): Block.setHardness(float)/setResistance(float) (1.7.10
		// runtime мутаторы, вызов ПОСЛЕ super()) удалены - neo BlockBehaviour.Properties.strength(...) неизменяема,
		// задаётся ТОЛЬКО ДО super() [BlockBehaviour.java:1127 окрестность]; тот же класс уже открыт GT_API.java:734
		// (block-property-runtime-mutator) - ретроактивная мутация недостижима, деградация до no-op (getBlockHardness/
		// getExplosionResistance(byte) ниже уже несут mHardnessMultiplier/mResistanceMultiplier как GT6-own
		// не-движковые методы, значение не теряется для GT6-внутренних потребителей).
		/* PORT-TODO(F16) setCreativeTab */;
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
		mSlabs = new BlockMetaType[] {
		  makeSlab(aItemClass, aVanillaMaterial, aSoundType, aNameInternal, aDefaultLocalised, aMaterial, aResistanceMultiplier / 2, aHardnessMultiplier / 2, aHarvestLevel, maxMeta(), aIcons, SIDE_DOWN    , this)
		, makeSlab(aItemClass, aVanillaMaterial, aSoundType, aNameInternal, aDefaultLocalised, aMaterial, aResistanceMultiplier / 2, aHardnessMultiplier / 2, aHarvestLevel, maxMeta(), aIcons, SIDE_UP      , this)
		, makeSlab(aItemClass, aVanillaMaterial, aSoundType, aNameInternal, aDefaultLocalised, aMaterial, aResistanceMultiplier / 2, aHardnessMultiplier / 2, aHarvestLevel, maxMeta(), aIcons, SIDE_NORTH   , this)
		, makeSlab(aItemClass, aVanillaMaterial, aSoundType, aNameInternal, aDefaultLocalised, aMaterial, aResistanceMultiplier / 2, aHardnessMultiplier / 2, aHarvestLevel, maxMeta(), aIcons, SIDE_SOUTH   , this)
		, makeSlab(aItemClass, aVanillaMaterial, aSoundType, aNameInternal, aDefaultLocalised, aMaterial, aResistanceMultiplier / 2, aHardnessMultiplier / 2, aHarvestLevel, maxMeta(), aIcons, SIDE_WEST    , this)
		, makeSlab(aItemClass, aVanillaMaterial, aSoundType, aNameInternal, aDefaultLocalised, aMaterial, aResistanceMultiplier / 2, aHardnessMultiplier / 2, aHarvestLevel, maxMeta(), aIcons, SIDE_EAST    , this)
		, null};
		mSlabs[SIDE_INVALID] = mSlabs[SIDE_DOWN];
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
		
		if (COMPAT_FR != null) COMPAT_FR.addToBackpacks("builder", ST.make(this, 1, W));
	}
	
	protected BlockMetaType makeSlab(Class<? extends BlockItem> aItemClass, Material aVanillaMaterial, SoundType aVanillaSoundType, String aName, String aDefaultLocalised, OreDictMaterial aMaterial, float aResistanceMultiplier, float aHardnessMultiplier, int aHarvestLevel, int aCount, IIconContainer[] aIcons, byte aSlabType, BlockMetaType aBlock) {
		return new BlockMetaType(aItemClass, aVanillaMaterial, aVanillaSoundType, aName, aDefaultLocalised, aMaterial, aResistanceMultiplier, aHardnessMultiplier, aHarvestLevel, aCount, aIcons, aSlabType, aBlock);
	}
	
	protected BlockMetaType(Class<? extends BlockItem> aItemClass, Material aVanillaMaterial, SoundType aSoundType, String aName, String aDefaultLocalised, OreDictMaterial aMaterial, float aResistanceMultiplier, float aHardnessMultiplier, int aHarvestLevel, int aCount, IIconContainer[] aIcons, byte aSlabType, BlockMetaType aBlock) {
		super(aItemClass == null ? ItemBlockMetaType.class : aItemClass, aName+".slab."+aSlabType, aVanillaMaterial, aSoundType, aCount, aIcons);
		if (aItemClass == null) aItemClass = ItemBlockMetaType.class;
		onSlabCreation(aItemClass, aVanillaMaterial, aSoundType, aName, aDefaultLocalised, aMaterial, aResistanceMultiplier, aHardnessMultiplier, aHarvestLevel, aCount, aIcons, aSlabType, aBlock);
		// PORT-TODO(F12, block-property-runtime-mutator): см. основной конструктор выше — тот же класс, деградация до no-op.
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
		setBlockBounds(
		mSide == SIDE_X_POS ? 0.5F : 0.0F,
		mSide == SIDE_Y_POS ? 0.5F : 0.0F,
		mSide == SIDE_Z_POS ? 0.5F : 0.0F,
		mSide == SIDE_X_NEG ? 0.5F : 1.0F,
		mSide == SIDE_Y_NEG ? 0.5F : 1.0F,
		mSide == SIDE_Z_NEG ? 0.5F : 1.0F
		);
		
		if (COMPAT_FR != null) COMPAT_FR.addToBackpacks("builder", ST.make(this, 1, W));
	}
	
	public void onBlockCreation(Class<? extends BlockItem> aItemClass, Material aVanillaMaterial, SoundType aSoundType, String aName, String aDefaultLocalised, OreDictMaterial aMaterial, float aResistanceMultiplier, float aHardnessMultiplier, int aHarvestLevel, int aCount, IIconContainer[] aIcons) {
		//
	}
	
	public void onSlabCreation(Class<? extends BlockItem> aItemClass, Material aVanillaMaterial, SoundType aSoundType, String aName, String aDefaultLocalised, OreDictMaterial aMaterial, float aResistanceMultiplier, float aHardnessMultiplier, int aHarvestLevel, int aCount, IIconContainer[] aIcons, byte aSlabType, BlockMetaType aBlock) {
		//
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
	@Override
	@OnlyIn(Dist.CLIENT)
	protected boolean skipRendering(BlockState aState, BlockState aNeighbor, Direction aDir) {
		byte aSide = UT.Code.side(aDir);
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

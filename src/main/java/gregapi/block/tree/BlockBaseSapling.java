/**
 * Copyright (c) 2024 GregTech-6 Team
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

package gregapi.block.tree;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.TallGrassBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;

import gregapi.api.Optional;
import gregapi.block.BlockBaseMeta;
import gregapi.data.CS.*;
import gregapi.data.MD;
import gregapi.data.OP;
import gregapi.render.IIconContainer;
import gregapi.util.ST;
import gregapi.util.UT;
import gregapi.util.WD;
import micdoodle8.mods.galacticraft.api.block.IOxygenReliantBlock;
import mods.railcraft.common.carts.EntityTunnelBore;

import gregapi.block.Material;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.PlantType;
import net.minecraftforge.common.IPlantable;
import net.minecraft.core.Direction;

import java.util.Random;

import static gregapi.data.CS.*;
import static net.minecraftforge.common.PlantType.PLAINS;

/**
 * @author Gregorius Techneticies
 */
@Optional.InterfaceList(value = {
	@Optional.Interface(iface = "micdoodle8.mods.galacticraft.api.block.IOxygenReliantBlock", modid = ModIDs.GC)
})
// Without this, the sapling rendered as a solid cube instead of a cross, since 1.7.10's render
// type 1 was the only signal telling the renderer to draw crossed squares; same contract as BlockBaseFlower.
public abstract class BlockBaseSapling extends BlockBaseMeta implements IPlantable, BonemealableBlock, IOxygenReliantBlock, gregapi.render.IRenderedCross {
	public BlockBaseSapling(Class<? extends BlockItem> aItemClass, String aNameInternal, Material aMaterial, SoundType aSoundType, long aMaxMeta, IIconContainer[] aIcons) {
		super(aItemClass, aNameInternal, aMaterial, aSoundType, Math.min(8, aMaxMeta), aIcons);
		setBlockBounds(0.1F, 0.0F, 0.1F, 0.9F, 0.8F, 0.9F);
		gregapi.item.CreativeTabsGT.assign(this, gregapi.item.CreativeTabsGT.DECORATIONS);
		// setTickRandomly's old runtime-mutator call is replaced by overriding isRandomlyTicking below,
		// since unlike setHardness/setResistance this override point actually exists on neo's Block.
		if (MD.RC.mLoaded) try {EntityTunnelBore.addMineableBlock(this);} catch(Throwable e) {e.printStackTrace(ERR);}
		if (COMPAT_FR != null) gregapi.GT_API.deferItemInit(() -> COMPAT_FR.addToBackpacks("forester", ST.make(this, 1, W)));
	}

	// replaces the old setTickRandomly(true) call; see the constructor's comment above
	@Override public boolean isRandomlyTicking(BlockState aState) {return T;}

	public abstract boolean grow(net.minecraft.world.level.LevelAccessor aWorld, int aX, int aY, int aZ, byte aMeta, Random aRandom);

	// Routes neo's performBonemeal into GT6's own grow(), centralized here in the sapling base so it covers every sapling
	// family.
	@Override public void performBonemeal(net.minecraft.server.level.ServerLevel aWorld, net.minecraft.util.RandomSource aRandom, BlockPos aPos, BlockState aState) {
		grow(aWorld, aPos.getX(), aPos.getY(), aPos.getZ(), WD.meta(aWorld, aPos.getX(), aPos.getY(), aPos.getZ()), UT.Code.random(aRandom)); // the RandomSource-to-Random converter lives in the single center UT.Code.random
	}
	@Override public boolean isBonemealSuccess(Level aWorld, net.minecraft.util.RandomSource aRandom, BlockPos aPos, BlockState aState) {return aRandom.nextFloat() < 0.45F;} // vanilla's own sapling-growth chance
	@Override public boolean isValidBonemealTarget(net.minecraft.world.level.LevelReader aWorld, BlockPos aPos, BlockState aState, boolean aIsClient) {return T;} // a sapling is always a valid bonemeal target
	
	@Override public String getHarvestTool(int aMeta) {return TOOL_sword;}
	@Override public int damageDropped(int aMeta) {return aMeta & 7;}
	@Override public int getDamageValue(Level aWorld, int aX, int aY, int aZ) {return WD.meta(aWorld, aX, aY, aZ) & 7;}
	@Override public float getBlockHardness(Level aWorld, int aX, int aY, int aZ) {return WD.hardness(Blocks.OAK_SAPLING, aWorld, aX, aY, aZ);}
	@Override public float getExplosionResistance(byte aMeta) {return Blocks.OAK_SAPLING.getExplosionResistance();}
	@Override public boolean checkNoEntityCollision(Level aWorld, int aX, int aY, int aZ, byte aMeta, Entity aExceptThisOne) {return T;}
	@Override public boolean canBeReplacedByLeaves(BlockGetter aWorld, int aX, int aY, int aZ) {return T;}
	@Override public boolean renderAsNormalBlock() {return F;}
	@Override public boolean isNormalCube(BlockGetter aWorld, int aX, int aY, int aZ)  {return F;}
	@Override public boolean isOpaqueCube() {return F;}
	@Override public boolean isSealable(byte aMeta, byte aSide) {return F;}
	@Override public boolean isSideSolid(int aMeta, byte aSide) {return F;}
	@Override public int getLightOpacity() {return LIGHT_OPACITY_LEAVES;}
	@Override public int getItemStackLimit(ItemStack aStack) {return UT.Code.bindStack(OP.treeSapling.mDefaultStackSize);}
	@Override public ResourceLocation getIcon(int aSide, int aMeta) {return mIcons[aMeta & 15].getIcon(0);}
	// Same per-meta icon lookup as getIcon above; aWorld==null means an item render, where aX carries the stack's own meta,
	// the same contract as BlockBaseFlower.
	@Override public ResourceLocation getCrossIcon(BlockGetter aWorld, int aX, int aY, int aZ) {
		if (mIcons == null || mIcons.length == 0) return null;
		gregapi.render.IIconContainer tIcon = mIcons[(aWorld == null ? aX : WD.meta(aWorld, aX, aY, aZ)) & 15];
		return tIcon == null ? null : tIcon.getIcon(0);
	}
	// Goes through the shared WD.canSustainPlant center; an earlier copy here collapsed its result to always true, while a
	// descendant used a stale always-false version instead, knocking saplings off grass on the very first tick.
	public boolean canBlockStay(Level aWorld, int aX, int aY, int aZ) {return WD.canSustainPlant(aWorld, aX, aY - 1, aZ, Direction.UP, Blocks.OAK_SAPLING);}
	public AABB getCollisionBoundingBoxFromPool(Level aWorld, int aX, int aY, int aZ) {return null;}
	public int getRenderType() {return 1;}
	public void onOxygenAdded(Level aWorld, int aX, int aY, int aZ) {/**/}
	public void onOxygenRemoved(Level aWorld, int aX, int aY, int aZ) {if (!aWorld.isClientSide() && !WD.oxygen(aWorld, aX, aY, aZ)) {WD.set(aWorld, aX, aY, aZ, Blocks.DEAD_BUSH, 0, 3); return;}}
	
	@Override
	public void onBlockAdded2(Level aWorld, int aX, int aY, int aZ) {
		if (!aWorld.isClientSide() && !WD.oxygen(aWorld, aX, aY, aZ)) {WD.set(aWorld, aX, aY, aZ, Blocks.DEAD_BUSH, 0, 3); return;}
	}
	
	@Override
	public void updateTick2(Level aWorld, int aX, int aY, int aZ, Random aRandom) {
		if (!aWorld.isClientSide() && !WD.oxygen(aWorld, aX, aY, aZ)) {WD.set(aWorld, aX, aY, aZ, Blocks.DEAD_BUSH, 0, 3); return;}
		// neo's getMaxLocalRawBrightness replaces the old combined block+sky light lookup, with the same meaning.
		if (aWorld.isClientSide() || checkAndDropBlock(aWorld, aX, aY, aZ) || aWorld.getMaxLocalRawBrightness(new BlockPos(aX, aY+1, aZ)) < 9 || aRandom.nextInt(7) != 0) return;
		tryGrow(aWorld, aX, aY, aZ, aRandom);
	}

	public boolean tryGrow(Level aWorld, int aX, int aY, int aZ, Random aRandom) {
		if (!aWorld.isClientSide() && !WD.oxygen(aWorld, aX, aY, aZ)) {WD.set(aWorld, aX, aY, aZ, Blocks.DEAD_BUSH, 0, 3); return F;}
		if (TREE_GROWTH_TIME > 1 && RNGSUS.nextInt(TREE_GROWTH_TIME) > 0) return F;
		byte aMeta = WD.meta(aWorld, aX, aY, aZ);
		if (aMeta < 8) {
			WD.set(aWorld, aX, aY, aZ, WD.block(aWorld, aX, aY, aZ), aMeta | 8, 2, F);
			return F;
		}
		// 1.7.10's tree-growth veto event has a real neo equivalent, ForgeEventFactory.blockGrowFeature, the
		// same path vanilla's own AbstractTreeGrower uses; GT6 grows imperatively, so only the cancellation result matters.
		return net.minecraftforge.event.ForgeEventFactory.blockGrowFeature(aWorld, aWorld.getRandom(), new net.minecraft.core.BlockPos(aX, aY, aZ), null).getResult() != net.minecraftforge.eventbus.api.Event.Result.DENY && grow(aWorld, aX, aY, aZ, aMeta, aRandom);
	}
	
	public int getMaxHeight(net.minecraft.world.level.LevelAccessor aWorld, int aX, int aY, int aZ, int aMaxTreeHeight) {
		aMaxTreeHeight--;
		int rMaxHeight = 0;
		while (rMaxHeight++ < aMaxTreeHeight) if (aY+rMaxHeight >= WD.topY(aWorld) || !canPlaceTree(aWorld, aX, aY+rMaxHeight, aZ)) return rMaxHeight-1; // the ceiling now comes from the shared Y-scale center, not getHeight(), which returns the total count, not the top
		return rMaxHeight;
	}
	
	public boolean placeTree(net.minecraft.world.level.LevelAccessor aWorld, int aX, int aY, int aZ, Block aBlock, int aMeta) {
		return canPlaceTree(aWorld, aX, aY, aZ) && WD.set(aWorld, aX, aY, aZ, aBlock, aMeta, 3);
	}
	
	public boolean canPlaceTree(net.minecraft.world.level.LevelAccessor aWorld, int aX, int aY, int aZ) {
		Block tBlock = WD.block(aWorld, aX, aY, aZ);
		// neo's TallGrassBlock/LeavesBlock replace the old generic vanilla base classes, covering the same variants through
		// instanceof.
		return tBlock == this || tBlock instanceof TallGrassBlock || tBlock instanceof SnowLayerBlock || tBlock instanceof LeavesBlock || canBeReplacedByLeavesOf(tBlock, aWorld, aX, aY, aZ);
	}

	// neo's Block has no generic override point for this, so it's wired through an instanceof dispatcher across every
	// GT6 override; the default checks the same isSolidRender flag as WD.visOpq, matching vanilla's isOpaqueCube.
	private static boolean canBeReplacedByLeavesOf(Block aBlock, net.minecraft.world.level.LevelAccessor aWorld, int aX, int aY, int aZ) {
		if (aBlock instanceof gregapi.block.BlockBase) return ((gregapi.block.BlockBase)aBlock).canBeReplacedByLeaves(aWorld, aX, aY, aZ);
		if (aBlock instanceof gregapi.block.prefixblock.PrefixBlock) return ((gregapi.block.prefixblock.PrefixBlock)aBlock).canBeReplacedByLeaves(aWorld, aX, aY, aZ);
		if (aBlock instanceof gregapi.block.misc.BlockBaseRail) return ((gregapi.block.misc.BlockBaseRail)aBlock).canBeReplacedByLeaves(aWorld, aX, aY, aZ);
		if (aBlock instanceof gregapi.block.misc.BlockBaseFlower) return ((gregapi.block.misc.BlockBaseFlower)aBlock).canBeReplacedByLeaves(aWorld, aX, aY, aZ);
		if (aBlock instanceof gregapi.block.multitileentity.MultiTileEntityBlock) return ((gregapi.block.multitileentity.MultiTileEntityBlock)aBlock).canBeReplacedByLeaves(aWorld, aX, aY, aZ);
		return !aBlock.defaultBlockState().isSolidRender(aWorld, new net.minecraft.core.BlockPos(aX, aY, aZ));
	}
	
	// 1.7.10 canPlaceBlockAt override (original :133 = isReplaceable(target) && canBlockStay) is expressed by the
	// engine channel: target replaceability is clause (2) of WD.canPlaceEntityOnSide, support is canSurvive below —
	// same bridge as BlockBaseFlower:235. Removal on lost support stays the GT6 checkAndDropBlock channel (1:1).
	@Override public boolean canSurvive(net.minecraft.world.level.block.state.BlockState aState, net.minecraft.world.level.LevelReader aWorld, net.minecraft.core.BlockPos aPos) {
		return aWorld instanceof Level tLevel ? canBlockStay(tLevel, aPos.getX(), aPos.getY(), aPos.getZ()) : super.canSurvive(aState, aWorld, aPos);
	}
	
	@Override
	public void onNeighborBlockChange2(Level aWorld, int aX, int aY, int aZ, Block aBlock) {
		checkAndDropBlock(aWorld, aX, aY, aZ);
	}
	
	public boolean checkAndDropBlock(Level aWorld, int aX, int aY, int aZ) {
		if (canBlockStay(aWorld, aX, aY, aZ)) return F;
		WD.dropBlockAsItem(aWorld, aX, aY, aZ, WD.meta(aWorld, aX, aY, aZ), 0);
		WD.set(aWorld, aX, aY, aZ, NB, 0, 2);
		return T;
	}
	
	// The real IPlantable signature is (BlockGetter,BlockPos); getPlant follows the real BushBlock.getPlant
	// pattern (state by position, else default). getPlantMetadata is removed, since the real interface has no such method.
	@Override public PlantType getPlantType(BlockGetter aWorld, BlockPos aPos) {return PLAINS;}
	@Override public BlockState getPlant(BlockGetter aWorld, BlockPos aPos) {BlockState tState = aWorld.getBlockState(aPos); return tState.getBlock() != this ? defaultBlockState() : tState;}
	public boolean func_149851_a(Level aWorld, int aX, int aY, int aZ, boolean aIsRemote) {return T;}
	public boolean func_149852_a(Level aWorld, Random aRandom, int aX, int aY, int aZ) {return aRandom.nextFloat() < 0.45;}
	public void func_149853_b(Level aWorld, Random aRandom, int aX, int aY, int aZ) {tryGrow(aWorld, aX, aY, aZ, aRandom);}
}

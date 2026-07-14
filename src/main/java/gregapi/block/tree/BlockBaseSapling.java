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
 */

package gregapi.block.tree;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.SnowLayerBlock;

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
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.EnumPlantType;
import net.minecraftforge.common.IPlantable;
import net.minecraft.core.Direction;
import net.minecraftforge.event.terraingen.TerrainGen;

import java.util.Random;

import static gregapi.data.CS.*;
import static net.minecraftforge.common.EnumPlantType.Plains;

/**
 * @author Gregorius Techneticies
 */
@Optional.InterfaceList(value = {
	@Optional.Interface(iface = "micdoodle8.mods.galacticraft.api.block.IOxygenReliantBlock", modid = ModIDs.GC)
})
public abstract class BlockBaseSapling extends BlockBaseMeta implements IPlantable, BonemealableBlock, IOxygenReliantBlock {
	public BlockBaseSapling(Class<? extends BlockItem> aItemClass, String aNameInternal, Material aMaterial, SoundType aSoundType, long aMaxMeta, IIconContainer[] aIcons) {
		super(aItemClass, aNameInternal, aMaterial, aSoundType, Math.min(8, aMaxMeta), aIcons);
		setBlockBounds(0.1F, 0.0F, 0.1F, 0.9F, 0.8F, 0.9F);
		/* PORT-TODO(F16) setCreativeTab */;
		setTickRandomly(T);
		setHardness(0);
		if (MD.RC.mLoaded) try {EntityTunnelBore.addMineableBlock(this);} catch(Throwable e) {e.printStackTrace(ERR);}
		if (COMPAT_FR != null) COMPAT_FR.addToBackpacks("forester", ST.make(this, 1, W));
	}
	
	public abstract boolean grow(Level aWorld, int aX, int aY, int aZ, byte aMeta, Random aRandom);
	
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
	@Override public Identifier getIcon(int aSide, int aMeta) {return mIcons[aMeta & 15].getIcon(0);}
	// было Block.canSustainPlant(IBlockAccess,x,y,z,side,IPlantable) (1.7.10) -> IBlockExtension.canSustainPlant(BlockState,
	// BlockGetter,BlockPos,Direction,BlockState) [IBlockExtension.java:424], TriState вместо boolean; тот же приём, что
	// уже принят в BlockBaseFlower.canBlockStay - toBoolean(T) как дефолт для TriState.DEFAULT.
	public boolean canBlockStay(Level aWorld, int aX, int aY, int aZ) {BlockPos tBelow = new BlockPos(aX, aY-1, aZ); return WD.block(aWorld, aX, aY - 1, aZ).canSustainPlant(aWorld.getBlockState(tBelow), aWorld, tBelow, Direction.UP, Blocks.OAK_SAPLING.defaultBlockState()).toBoolean(T);}
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
		if (aWorld.isClientSide() || checkAndDropBlock(aWorld, aX, aY, aZ) || aWorld.getBlockLightValue(aX, aY+1, aZ) < 9 || aRandom.nextInt(7) != 0) return;
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
		return TerrainGen.saplingGrowTree(aWorld, aRandom, aX, aY, aZ) && grow(aWorld, aX, aY, aZ, aMeta, aRandom);
	}
	
	public int getMaxHeight(Level aWorld, int aX, int aY, int aZ, int aMaxTreeHeight) {
		aMaxTreeHeight--;
		int rMaxHeight = 0;
		while (rMaxHeight++ < aMaxTreeHeight) if (aY+rMaxHeight >= aWorld.getHeight() || !canPlaceTree(aWorld, aX, aY+rMaxHeight, aZ)) return rMaxHeight-1;
		return rMaxHeight;
	}
	
	public boolean placeTree(Level aWorld, int aX, int aY, int aZ, Block aBlock, int aMeta) {
		return canPlaceTree(aWorld, aX, aY, aZ) && WD.set(aWorld, aX, aY, aZ, aBlock, aMeta, 3);
	}
	
	public boolean canPlaceTree(Level aWorld, int aX, int aY, int aZ) {
		Block tBlock = WD.block(aWorld, aX, aY, aZ);
		return tBlock == this || tBlock instanceof BlockTallGrass || tBlock instanceof SnowLayerBlock || tBlock instanceof BlockLeavesBase || tBlock.canBeReplacedByLeaves(aWorld, aX, aY, aZ);
	}
	
	// было Block.canPlaceBlockAt(World,x,y,z) (1.7.10, дефолт world.getBlock(x,y,z).isReplaceable(...), Block.java:1046-1049)
	// удалён из neo целиком - inline-порт вместо super через уже-существующий центр WD.replaceable, тот же приём, что
	// BlockBaseLilyPad.canPlaceBlockAt уже использует.
	public boolean canPlaceBlockAt(Level aWorld, int aX, int aY, int aZ) {return WD.replaceable(WD.block(aWorld, aX, aY, aZ), aWorld, aX, aY, aZ) && canBlockStay(aWorld, aX, aY, aZ);}
	
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
	
	public EnumPlantType getPlantType(BlockGetter aWorld, int aX, int aY, int aZ) {return Plains;}
	public Block getPlant(BlockGetter aWorld, int aX, int aY, int aZ) {return this;}
	public int getPlantMetadata(BlockGetter aWorld, int aX, int aY, int aZ) {return WD.meta(aWorld, aX, aY, aZ);}
	public boolean func_149851_a(Level aWorld, int aX, int aY, int aZ, boolean aIsRemote) {return T;}
	public boolean func_149852_a(Level aWorld, Random aRandom, int aX, int aY, int aZ) {return aRandom.nextFloat() < 0.45;}
	public void func_149853_b(Level aWorld, Random aRandom, int aX, int aY, int aZ) {tryGrow(aWorld, aX, aY, aZ, aRandom);}
}

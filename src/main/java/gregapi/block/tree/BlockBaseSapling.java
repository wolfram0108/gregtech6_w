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
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.EnumPlantType;
import net.minecraftforge.common.IPlantable;
import net.minecraft.core.Direction;

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
		gregapi.item.CreativeTabsGT.assign(this, gregapi.item.CreativeTabsGT.DECORATIONS);
		// было setTickRandomly(true) (1.7.10 runtime мутатор, вызов ПОСЛЕ super()) -> перенесено на реальную
		// override-точку BlockBehaviour.isRandomlyTicking(BlockState) [BlockBehaviour.java:382-384] ниже (в отличие
		// от setHardness/setResistance у этой точки ЕСТЬ override, не no-op).
		// F12-hardness: 1.7.10 setHardness(0) заменён getBlockHardness ниже (OAK_SAPLING) → подключён к neo через
		// BlockBase.getDestroyProgress (централизованно, 1:1). Runtime-мутатор не нужен.
		if (MD.RC.mLoaded) try {EntityTunnelBore.addMineableBlock(this);} catch(Throwable e) {e.printStackTrace(ERR);}
		if (COMPAT_FR != null) gregapi.GT_API.deferItemInit(() -> COMPAT_FR.addToBackpacks("forester", ST.make(this, 1, W)));
	}

	// было setTickRandomly(true) — см. комментарий в конструкторе выше.
	@Override protected boolean isRandomlyTicking(BlockState aState) {return T;}

	public abstract boolean grow(Level aWorld, int aX, int aY, int aZ, byte aMeta, Random aRandom);

	// neo BonemealableBlock.performBonemeal — маршрут в GT6 grow() (централизовано в базе сапплингов, покрывает AB/CD).
	@Override public void performBonemeal(net.minecraft.server.level.ServerLevel aWorld, net.minecraft.util.RandomSource aRandom, BlockPos aPos, BlockState aState) {
		grow(aWorld, aPos.getX(), aPos.getY(), aPos.getZ(), WD.meta(aWorld, aPos.getX(), aPos.getY(), aPos.getZ()), new Random(aRandom.nextLong()));
	}
	@Override public boolean isBonemealSuccess(Level aWorld, net.minecraft.util.RandomSource aRandom, BlockPos aPos, BlockState aState) {return aRandom.nextFloat() < 0.45F;} // ванильный шанс сапплинга (SaplingBlock)
	@Override public boolean isValidBonemealTarget(net.minecraft.world.level.LevelReader aWorld, BlockPos aPos, BlockState aState) {return T;} // сапплинг всегда bonemeal-цель
	
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
		// было World.getBlockLightValue(x,y,z) (комбинированный блок+небо свет, recompSrc World.java:864-922) ->
		// LevelReader.getMaxLocalRawBrightness(BlockPos) [LevelReader.java:163], тот же комбинированный смысл.
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
		// 1.7.10 TerrainGen.saplingGrowTree(World,Random,x,y,z) — veto-событие роста дерева — СПОСОБНОСТЬ ЕСТЬ в neo:
		// EventHooks.fireBlockGrowFeature(LevelAccessor,RandomSource,BlockPos,@Nullable Holder<ConfiguredFeature>)
		// (neoforge EventHooks.java:770; тот же путь, что vanilla TreeGrower). GT6 растит императивно (свой grow(),
		// ConfiguredFeature нет) -> holder=null (@Nullable допускает); интересует только отмена (isCanceled), ровно как
		// оригинал возвращал allow/veto. RandomSource = aWorld.getRandom() (aRandom тут java.util.Random). Реальный порт.
		return !net.neoforged.neoforge.event.EventHooks.fireBlockGrowFeature(aWorld, aWorld.getRandom(), new net.minecraft.core.BlockPos(aX, aY, aZ), null).isCanceled() && grow(aWorld, aX, aY, aZ, aMeta, aRandom);
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
		// было BlockTallGrass/BlockLeavesBase (1.7.10 vanilla generic-базы) -> TallGrassBlock [TallGrassBlock.java:15,
		// Blocks.java:707-732 - SHORT_GRASS/FERN оба instanceof TallGrassBlock, тот же класс что раньше нёс оба meta-
		// варианта единого BlockTallGrass] / LeavesBlock [LeavesBlock.java:29 - абстрактная база всех vanilla-листьев].
		return tBlock == this || tBlock instanceof TallGrassBlock || tBlock instanceof SnowLayerBlock || tBlock instanceof LeavesBlock || canBeReplacedByLeavesOf(tBlock, aWorld, aX, aY, aZ);
	}

	// PORT-TODO(F13, block-canBeReplacedByLeaves-generic-lost): 1.7.10 vanilla Block.canBeReplacedByLeaves(World,x,y,z)
	// (recompSrc Block.java) - overridable generic vanilla-хук, у neo Block такой generic override-точки нет (не
	// найдена ни в одном из 3 корней референса). Центральный аналог WD.wood/WD.leaves (instanceof-диспетчер,
	// WD.java:473-484) сюда не распространён — WD god-класс, отдельный F-шов вне периметра этой задачи. Локальный
	// instanceof-диспетчер по известным GT6-переопределениям (BlockBase/PrefixBlock/BlockBaseRail/BlockBaseFlower/
	// MultiTileEntityBlock — грепом "canBeReplacedByLeaves" по gregapi/block), дефолт F = vanilla Block-дефолт.
	private static boolean canBeReplacedByLeavesOf(Block aBlock, Level aWorld, int aX, int aY, int aZ) {
		if (aBlock instanceof gregapi.block.BlockBase) return ((gregapi.block.BlockBase)aBlock).canBeReplacedByLeaves(aWorld, aX, aY, aZ);
		if (aBlock instanceof gregapi.block.prefixblock.PrefixBlock) return ((gregapi.block.prefixblock.PrefixBlock)aBlock).canBeReplacedByLeaves(aWorld, aX, aY, aZ);
		if (aBlock instanceof gregapi.block.misc.BlockBaseRail) return ((gregapi.block.misc.BlockBaseRail)aBlock).canBeReplacedByLeaves(aWorld, aX, aY, aZ);
		if (aBlock instanceof gregapi.block.misc.BlockBaseFlower) return ((gregapi.block.misc.BlockBaseFlower)aBlock).canBeReplacedByLeaves(aWorld, aX, aY, aZ);
		if (aBlock instanceof gregapi.block.multitileentity.MultiTileEntityBlock) return ((gregapi.block.multitileentity.MultiTileEntityBlock)aBlock).canBeReplacedByLeaves(aWorld, aX, aY, aZ);
		return F;
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

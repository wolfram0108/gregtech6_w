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

package gregapi.item.multiitem.behaviors;
import net.minecraft.world.phys.BlockHitResult;

import static gregapi.data.CS.*;

import gregapi.data.CS.BlocksGT;
import gregapi.data.FL;
import gregapi.data.MD;
import gregapi.item.multiitem.MultiItem;
import gregapi.item.multiitem.behaviors.IBehavior.AbstractBehaviorDefault;
import gregapi.util.ST;
import gregapi.util.UT;
import gregapi.util.WD;
import iguanaman.hungeroverhaul.config.IguanaConfig;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CauldronBlock;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.core.BlockSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.animal.MushroomCow;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.level.Level;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidBlock;

public class Behavior_Bucket_Simple extends AbstractBehaviorDefault {
	public static final IBehavior<MultiItem> INSTANCE = new Behavior_Bucket_Simple(NI);
	
	public ItemStack mDefaultFullBucket;
	
	public Behavior_Bucket_Simple(ItemStack aDefault) {
		mDefaultFullBucket = aDefault;
	}
	
	@Override public boolean canDispense(MultiItem aItem, BlockSource aSource, ItemStack aStack) {return T;}
	
	@Override
	public ItemStack onDispense(MultiItem aItem, BlockSource aSource, ItemStack aStack) {
		if (aStack.getCount() > 1) return super.onDispense(aItem, aSource, aStack);
		FluidStack mFluid = FL.getFluid(aStack, T);
		ItemStack tBucket = ST.make(Items.BUCKET, 1, 0);
		
		Direction aFacing = aSource.getBlockState().getValue(DispenserBlock.FACING); // F-dispenser: func_149937_b(metadata) -> facing из BlockState (BlockSource=record, pos()/state())
		Level aWorld = aSource.getLevel();
		int aX = aSource.getPos().getX() + aFacing.getStepX(), aY = aSource.getPos().getY() + aFacing.getStepY(), aZ = aSource.getPos().getZ() + aFacing.getStepZ(); // getXInt/getFrontOffsetX -> getPos().getX()/getStepX() (BlockSource.java:14)
		
		if (mFluid == null) {
			Block tFluidBlock = WD.block(aWorld, aX, aY, aZ);
			if (tFluidBlock == BlocksGT.River) {
				tBucket = FL.fill(FL.Water.make(1000), aStack, F, T, F, T);
				return tBucket == null ? aStack : tBucket;
			}
			if (tFluidBlock == BlocksGT.Ocean) {
				tBucket = FL.fill(FL.Ocean.make(1000), aStack, F, T, F, T);
				return tBucket == null ? aStack : tBucket;
			}
			if (tFluidBlock == BlocksGT.Swamp) {
				tBucket = FL.fill(FL.Dirty_Water.make(1000), aStack, F, T, F, T);
				return tBucket == null ? aStack : tBucket;
			}
			if (tFluidBlock == Blocks.LAVA || tFluidBlock == Blocks.LAVA) {
				if (WD.meta(aWorld, aX, aY, aZ) != 0) return super.onDispense(aItem, aSource, aStack);
				tBucket = FL.fill(FL.Lava.make(1000), aStack, F, T, F, T);
				return tBucket == null ? aStack : WD.set(aWorld, aX, aY, aZ, NB, 0, 3) ? tBucket : aStack;
			}
			if (tFluidBlock == Blocks.WATER || tFluidBlock == Blocks.WATER) {
				if (WD.meta(aWorld, aX, aY, aZ) != 0) return super.onDispense(aItem, aSource, aStack);
				tBucket = FL.fill(FL.Water.make(1000), aStack, F, T, F, T);
				return tBucket == null ? aStack : WD.set(aWorld, aX, aY, aZ, NB, 0, 3) ? tBucket : aStack;
			}
			if (tFluidBlock instanceof IFluidBlock) {
				FluidStack tFluid = FL.drainable(aWorld, new net.minecraft.core.BlockPos(aX, aY, aZ)); // F5 §6.2 — центр
				if (tFluid != null) {
					tBucket = FL.fill(tFluid, aStack, F, T, F, T);
					if (ST.valid(tBucket)) {
						FL.drainCell(aWorld, new net.minecraft.core.BlockPos(aX, aY, aZ)); // F5 §6.2 — центр
						return tBucket == null ? aStack : tBucket;
					}
					return super.onDispense(aItem, aSource, aStack);
				}
			}
		} else {
			if (ST.valid(mDefaultFullBucket)) {
				tBucket = ST.copy(mDefaultFullBucket);
			} else {
				if (ST.invalid(tBucket = FL.fill(mFluid, tBucket, F, T, F, T))) return super.onDispense(aItem, aSource, aStack);
			}
			// F-item-use: 1.7.10 BucketItem.tryPlaceContainedLiquid(world,x,y,z) -> neo BucketItem.emptyContents(LivingEntity,
			// Level,BlockPos,BlockHitResult) (BucketItem.java). Диспенсер без игрока/хита -> null,null; ставит жидкость в (aX,aY,aZ).
			if (ST.item_(tBucket) instanceof BucketItem tBucketItem && tBucketItem.emptyContents(null, aWorld, new net.minecraft.core.BlockPos(aX, aY, aZ), null)) {
				return processBucket(ST.make(Items.BUCKET, 1, 0), aStack, T);
			}
		}
		return super.onDispense(aItem, aSource, aStack);
	}
	
	@Override
	public ItemStack onItemRightClick(MultiItem aItem, ItemStack aStack, Level aWorld, Player aPlayer) {
		FluidStack mFluid = FL.getFluid(aStack, T);
		HitResult aTarget = WD.getMOP(aWorld, aPlayer, mFluid == null);
		if (aTarget == null || aTarget.getType() != HitResult.Type.BLOCK) return aStack;
		int aX = ((BlockHitResult)aTarget).getBlockPos().getX(), aY = ((BlockHitResult)aTarget).getBlockPos().getY(), aZ = ((BlockHitResult)aTarget).getBlockPos().getZ();
		ItemStack tBucket = ST.make(Items.BUCKET, 1, 0);
		
		if (mFluid == null) {
			Block tFluidBlock = WD.block(aWorld, aX, aY, aZ);
			if (tFluidBlock == BlocksGT.River) {
				tBucket = FL.fill(FL.Water.make(1000), aStack, F, T, F, T);
				return tBucket == null ? aStack : tBucket;
			}
			if (tFluidBlock == BlocksGT.Ocean) {
				tBucket = FL.fill(FL.Ocean.make(1000), aStack, F, T, F, T);
				return tBucket == null ? aStack : tBucket;
			}
			if (tFluidBlock == BlocksGT.Swamp) {
				tBucket = FL.fill(FL.Dirty_Water.make(1000), aStack, F, T, F, T);
				return tBucket == null ? aStack : tBucket;
			}
			if (tFluidBlock == Blocks.LAVA || tFluidBlock == Blocks.LAVA || tFluidBlock == Blocks.WATER || tFluidBlock == Blocks.WATER) {
				// F-item-use: 1.7.10 vanilla Bucket.onItemRightClick(empty) заполнял бакет из источника по рейкасту -> neo use()
				// работает по held-item и не возвращает стек. Воспроизводим ИТОГ напрямую: source (meta==0) -> vanilla полный
				// бакет + удаление источника (WD.set NB). Ровно то, что делал vanilla-бакет.
				if (WD.meta(aWorld, aX, aY, aZ) == 0) {
					tBucket = ST.make(tFluidBlock == Blocks.LAVA ? Items.LAVA_BUCKET : Items.WATER_BUCKET, 1, 0);
					WD.set(aWorld, aX, aY, aZ, NB, 0, 3);
				}
			} else
			if (tFluidBlock instanceof IFluidBlock) {
				FluidStack tFluid = FL.drainable(aWorld, new net.minecraft.core.BlockPos(aX, aY, aZ)); // F5 §6.2 — центр
				if (tFluid != null) {
					// F-item-use: vanilla Bucket.onItemRightClick(tBucket) не заполняется от modded IFluidBlock (mirror, мод не загружен);
					// GT6-бакет уже заполняется FL.fill(aStack) в общем потоке — vestigial vanilla-делегат убран, tBucket без изменений.
					FL.fill(tFluid, aStack, F, T, F, T);
					if (FL.milk(tFluid) && tFluid.getAmount() >= 1000) tBucket = ST.make(Items.MILK_BUCKET, 1, 0);
				}
			}
		} else {
			if (ST.valid(mDefaultFullBucket)) {
				tBucket = ST.copy(mDefaultFullBucket);
				// F-item-use: onItemRightClick(full bucket)=опустошить (поставить жидкость) -> neo BucketItem.emptyContents(
				// player,level,pos,hit); итог — пустой бакет. pos = соседний к клику блок (getBlockPos().relative(direction)).
				if (ST.item_(tBucket) instanceof BucketItem tBucketItem) {tBucketItem.emptyContents(aPlayer, aWorld, ((BlockHitResult)aTarget).getBlockPos().relative(((BlockHitResult)aTarget).getDirection()), (BlockHitResult)aTarget); tBucket = ST.make(Items.BUCKET, 1, 0);}
			} else {
				if (ST.invalid(tBucket = FL.fill(mFluid, tBucket, F, T, F, T))) return aStack;
				// F-item-use: onItemRightClick(full bucket)=опустошить (поставить жидкость) -> neo BucketItem.emptyContents(
				// player,level,pos,hit); итог — пустой бакет. pos = соседний к клику блок (getBlockPos().relative(direction)).
				if (ST.item_(tBucket) instanceof BucketItem tBucketItem) {tBucketItem.emptyContents(aPlayer, aWorld, ((BlockHitResult)aTarget).getBlockPos().relative(((BlockHitResult)aTarget).getDirection()), (BlockHitResult)aTarget); tBucket = ST.make(Items.BUCKET, 1, 0);}
			}
		}
		aPlayer.stopUsingItem(); // 1.7.10 clearItemInUse -> neo LivingEntity.stopUsingItem()
		return processBucket(tBucket, aStack, mFluid != null);
	}
	
	@Override
	public boolean onRightClickEntity(MultiItem aItem, ItemStack aStack, Player aPlayer, Entity aEntity) {
		if (FL.getFluid(aStack, T) == null && aEntity instanceof LivingEntity && !((LivingEntity)aEntity).isBaby()) {
			if (aPlayer.level().isClientSide()) return T;
			if (aEntity.getClass() == Cow.class || aEntity.getClass() == MushroomCow.class) {
				if (MD.HO.mLoaded && IguanaConfig.milkedTimeout > 0 && !UT.Entities.hasInfiniteItems(aPlayer)) {
					CompoundTag tNBT = aEntity.getPersistentData();
					if (tNBT.contains("Milked")) return T;
					tNBT.putInt("Milked", IguanaConfig.milkedTimeout * 60);
				}
				ST.set(aStack, FL.fill(FL.Milk.make(Integer.MAX_VALUE), aStack, F, T, T, T));
			}
			return T;
		}
		return F;
	}
	
	@Override
	public boolean onItemUseFirst(MultiItem aItem, ItemStack aStack, Player aPlayer, Level aWorld, int aX, int aY, int aZ, byte aSide, float hitX, float hitY, float hitZ) {
		if (aPlayer.level().isClientSide()) return F;
		FluidStack mFluid = FL.getFluid(aStack, T);
		if (mFluid == null) return F;
		if (FL.water(mFluid) && mFluid.getAmount() >= 1000) {
			Block aBlock = WD.block(aWorld, aX, aY, aZ);
			// F-cauldron: 1.7.10 CauldronBlock c metadata-уровнем (0-3) + func_150024_a(...,3)=залить водой доверху. neo:
			// пустой котёл = CauldronBlock, водяной = LayeredCauldronBlock со BlockState LEVEL (макс MAX_FILL_LEVEL=3).
			// Заливка бакетом воды: ставим WATER_CAULDRON с LEVEL=MAX, если текущий уровень не полон (empty=0). Уровень из
			// состояния (метаданных нет). AbstractCauldronBlock покрывает и пустой (CauldronBlock), и водяной (Layered).
			if (aBlock instanceof net.minecraft.world.level.block.AbstractCauldronBlock) {
				net.minecraft.core.BlockPos tCauldronPos = new net.minecraft.core.BlockPos(aX, aY, aZ);
				int tLevel = aBlock instanceof net.minecraft.world.level.block.LayeredCauldronBlock ? aWorld.getBlockState(tCauldronPos).getValue(net.minecraft.world.level.block.LayeredCauldronBlock.LEVEL) : 0;
				if (tLevel < net.minecraft.world.level.block.LayeredCauldronBlock.MAX_FILL_LEVEL) {
					aWorld.setBlockAndUpdate(tCauldronPos, Blocks.WATER_CAULDRON.defaultBlockState().setValue(net.minecraft.world.level.block.LayeredCauldronBlock.LEVEL, net.minecraft.world.level.block.LayeredCauldronBlock.MAX_FILL_LEVEL));
					ST.set(aStack, ST.container(aStack, T));
					return T;
				}
				return F;
			}
		}
		return F;
	}
	
	protected ItemStack processBucket(ItemStack aBucket, ItemStack aStack, boolean aWasFull) {
		if (aBucket == null) return aStack;
		if (aWasFull) {
			if (aBucket.getItem() == Items.BUCKET) {
				aBucket = ST.container(aStack, F);
				if (aBucket == null) aStack.setCount(0); else aStack = aBucket;
				return aStack;
			}
		} else {
			FluidStack tFluid = FL.getFluid(aBucket, T);
			if (tFluid != null) {
				aBucket = FL.fill(tFluid, aStack, F, T, F, T);
				if (aBucket == null) aStack.setCount(0); else aStack = aBucket;
				return aStack;
			}
		}
		return aStack;
	}
}

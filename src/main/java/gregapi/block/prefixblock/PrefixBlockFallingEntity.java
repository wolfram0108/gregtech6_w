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

package gregapi.block.prefixblock;
import gregapi.util.WD;

import static gregapi.data.CS.*;

import com.mojang.serialization.MapCodec;

import gregapi.block.IBlockPlacable;
import gregapi.code.ArrayListNoNulls;
import gregapi.code.ItemNBT;
import gregapi.util.ST;
import gregapi.util.UT;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * @author Gregorius Techneticies
 */
public class PrefixBlockFallingEntity extends FallingBlockEntity {
	protected IBlockPlacable mBlock;
	protected ItemStack mStack;
	protected CompoundTag mBlockNBT;

	/** The real falling block is read straight from the base class's own private field, with no separate copy, since
	 *  the author deliberately made the public getter show gravel while internal logic reads fallingBlock() instead. */
	protected Block fallingBlock() {return super.getBlockState().getBlock();}

	/** Writes the real falling block into the base's private field through its own normal channel --
	 *  reading NBT. 1.20.1's entity NBT is CompoundTag again, so no intermediate wrapper is needed. */
	private void initFallingBlock(Level aWorld, net.minecraft.world.level.block.state.BlockState aState) {
		CompoundTag tNBT = UT.NBT.make();
		tNBT.put("BlockState", net.minecraft.nbt.NbtUtils.writeBlockState(aState));
		super.readAdditionalSaveData(tNBT);
	}

	/** The engine's entity factory, called both on client-side spawn and when loading from disk. */
	public PrefixBlockFallingEntity(EntityType<? extends PrefixBlockFallingEntity> aType, Level aWorld) {
		super(aType, aWorld);
	}

	public PrefixBlockFallingEntity(Level aWorld) {
		super(gregapi.GT_API.METABLOCK_FALLING.get(), aWorld);
	}

	public PrefixBlockFallingEntity(Level aWorld, double aX, double aY, double aZ, IBlockPlacable aBlock, ItemStack aStack) {
		super(gregapi.GT_API.METABLOCK_FALLING.get(), aWorld);
		setPos(aX, aY, aZ);
		setStartPos(blockPosition()); // matches the private neo constructor exactly, or the starting point would be null
		mBlock = aBlock;
		mStack = aStack;
		mBlockNBT = ItemNBT.get(aStack);
		initFallingBlock(aWorld, ((Block)aBlock).defaultBlockState()); // matches the original: (Block)aBlock, meta 0
	}

	/** Deliberately returns gravel, not the real block: engine and renderer only ever see this getter (also driving the
	 *  spawn packet), while internal logic reads the real block through fallingBlock(), exactly matching 1.7.10's split. */
	@Override
	public net.minecraft.world.level.block.state.BlockState getBlockState() {
		return Blocks.GRAVEL.defaultBlockState();
	}

	@Override
	public void tick() {
		xo = getX();
		yo = getY();
		zo = getZ();
		++time;
		setDeltaMovement(getDeltaMovement().add(0.0D, -0.03999999910593033D, 0.0D));
		move(MoverType.SELF, getDeltaMovement());
		setDeltaMovement(getDeltaMovement().scale(0.9800000190734863D));
		if (!level().isClientSide()) {
			int aX = UT.Code.roundDown(getX());
			int aY = UT.Code.roundDown(getY());
			int aZ = UT.Code.roundDown(getZ());
			if (time == 1) {
				if (WD.block(level(), aX, aY, aZ) != fallingBlock()) { // checks against the real block, not the gravel visual, matching the original
					discard();
					return;
				}
				WD.set(level(), aX, aY, aZ, Blocks.AIR, 0, 3);
			}
			if (onGround()) {
				Vec3 v = getDeltaMovement();
				setDeltaMovement(v.x * 0.699999988079071D, v.y * -0.5D, v.z * 0.699999988079071D);
				if (WD.block(level(), aX, aY, aZ) != Blocks.MOVING_PISTON) {
					discard();
					// Checks placement legality through the shared WD.canPlaceEntityOnSide center; the previous check asked whether
					// the falling block itself was opaque first, making solid ore unconditionally "occupied" and dropped as an item.
					if (!WD.canPlaceEntityOnSide(level(), fallingBlock(), aX, aY, aZ, T, 1, null, mStack) || FallingBlock.isFree(WD.block(level(), aX, aY - 1, aZ).defaultBlockState()) || mBlock == null || !mBlock.placeBlock(level(), aX, aY, aZ, (byte)1, ST.meta_(mStack), ItemNBT.get(mStack), T, T)) {
						if (dropItem) if (mBlock instanceof PrefixBlock) {for (ItemStack tStack : ((PrefixBlock)mBlock).mDrops.getDrops((PrefixBlock)mBlock, level(), aX, aY, aZ, ST.meta_(mStack), null, 0, F)) {spawnAtLocation(tStack);}} else {spawnAtLocation(mStack);}
					}
				}
			} else if (time > 100 && !level().isClientSide() && (aY < WD.minY(level())+1 || aY > WD.topY(level())) || time > 600) { // world bounds now come from the shared Y-scale center, not the old fixed [1, 256] range
				if (dropItem) if (mBlock instanceof PrefixBlock) {for (ItemStack tStack : ((PrefixBlock)mBlock).mDrops.getDrops((PrefixBlock)mBlock, level(), aX, aY, aZ, ST.meta_(mStack), null, 0, F)) {spawnAtLocation(tStack);}} else {spawnAtLocation(mStack);}
				discard();
			}
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public boolean causeFallDamage(float aFallDistance, float aDamageModifier, DamageSource aDamageSource) {
		int i = Mth.ceil(aFallDistance - 1.0F);
		if (i > 0) for (Entity tEntity : new ArrayListNoNulls<Entity>(level().getEntities(this, getBoundingBox()))) {
			if (tEntity instanceof LivingEntity) tEntity.hurt(damageSources().fallingBlock(this), TFC_DAMAGE_MULTIPLIER * Math.min(Mth.floor((float)i * 2), 40));// neo's damageSources().fallingBlock replaces the old static DamageSource field.
		}
		return false;
	}

	/** 1.20.1: entity NBT is CompoundTag again, so this is 1.7.10's writeEntityToNBT/readEntityFromNBT form
	 *  verbatim; the ValueOutput/ValueInput bridge is gone along with those 26.x types. super runs first. */
	@Override
	protected void addAdditionalSaveData(CompoundTag aNBT) {
		super.addAdditionalSaveData(aNBT);
		aNBT.putShort("MetaData", ST.meta_(mStack));
		if (mBlockNBT != null) aNBT.put("TileEntityData", mBlockNBT);
		// The base class itself persists the real falling block, exactly as 1.7.10's own base save method did; no separate key is
		// needed.
	}

	@Override
	protected void readAdditionalSaveData(CompoundTag aNBT) {
		super.readAdditionalSaveData(aNBT);
		// Rebuilds from the real block the base class already loaded from NBT one line above; a non-GT6 or corrupted block leaves
		// mBlock null, which the landing logic then treats as "just drop an item" instead of crashing on a bad cast.
		mBlock = fallingBlock() instanceof IBlockPlacable tPlacable ? tPlacable : null;
		mStack = ST.make(fallingBlock(), 1, aNBT.getShort("MetaData"));
		mBlockNBT = aNBT.contains("TileEntityData") ? aNBT.getCompound("TileEntityData") : null;
		ItemNBT.set(mStack, mBlockNBT);
	}
}

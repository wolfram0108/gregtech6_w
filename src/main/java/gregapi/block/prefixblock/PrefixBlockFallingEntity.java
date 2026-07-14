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
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/**
 * @author Gregorius Techneticies
 */
public class PrefixBlockFallingEntity extends FallingBlockEntity {
	protected IBlockPlacable mBlock;
	protected ItemStack mStack;
	protected CompoundTag mBlockNBT;

	public PrefixBlockFallingEntity(Level aWorld) {
		super(EntityType.FALLING_BLOCK, aWorld); // PORT-TODO(F12-entity, decisions/F12-registration-lifecycle.md): плейсхолдер EntityType — своя EntityType-регистрация (DeferredRegister<EntityType>, см. GT_API.java:901-907) ещё не заведена ADR'ом
	}

	public PrefixBlockFallingEntity(Level aWorld, double aX, double aY, double aZ, IBlockPlacable aBlock, ItemStack aStack) {
		super(EntityType.FALLING_BLOCK, aWorld); // PORT-TODO(F12-entity, decisions/F12-registration-lifecycle.md): плейсхолдер EntityType, см. выше
		setPos(aX, aY, aZ);
		mBlock = aBlock;
		mStack = aStack;
		mBlockNBT = ItemNBT.get(aStack);
		// PORT-TODO(F12-entity, decisions/F12-registration-lifecycle.md): blockState (визуал падающего блока) остаётся дефолтным
		// Blocks.SAND.defaultBlockState() — приватный конструктор FallingBlockEntity(Level,x,y,z,BlockState) (FallingBlockEntity.java:79)
		// недоступен вне пакета neo, публичного сеттера нет.
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
				if (WD.block(level(), aX, aY, aZ) != getBlockState().getBlock()) {
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
					if (WD.hasCollide(level(), aX, aY, aZ, getBlockState().getBlock()) || FallingBlock.isFree(WD.block(level(), aX, aY - 1, aZ).defaultBlockState()) || !mBlock.placeBlock(level(), aX, aY, aZ, (byte)1, ST.meta_(mStack), ItemNBT.get(mStack), T, T)) {
						if (dropItem) if (mBlock instanceof PrefixBlock) {for (ItemStack tStack : ((PrefixBlock)mBlock).mDrops.getDrops((PrefixBlock)mBlock, level(), aX, aY, aZ, ST.meta_(mStack), null, 0, F)) {if (level() instanceof ServerLevel tServerLevel) spawnAtLocation(tServerLevel, tStack);}} else {if (level() instanceof ServerLevel tServerLevel) spawnAtLocation(tServerLevel, mStack);}
					}
				}
			} else if (time > 100 && !level().isClientSide() && (aY < 1 || aY > 256) || time > 600) {
				if (dropItem) if (mBlock instanceof PrefixBlock) {for (ItemStack tStack : ((PrefixBlock)mBlock).mDrops.getDrops((PrefixBlock)mBlock, level(), aX, aY, aZ, ST.meta_(mStack), null, 0, F)) {if (level() instanceof ServerLevel tServerLevel) spawnAtLocation(tServerLevel, tStack);}} else {if (level() instanceof ServerLevel tServerLevel) spawnAtLocation(tServerLevel, mStack);}
				discard();
			}
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public boolean causeFallDamage(double aFallDistance, float aDamageModifier, DamageSource aDamageSource) {
		int i = Mth.ceil(aFallDistance - 1.0D);
		if (i > 0) for (Entity tEntity : new ArrayListNoNulls<Entity>(level().getEntities(this, getBoundingBox()))) {
			if (tEntity instanceof LivingEntity) tEntity.hurt(damageSources().fallingBlock(this), TFC_DAMAGE_MULTIPLIER * Math.min(Mth.floor((float)i * 2), 40));// было DamageSource.fallingBlock (1.7.10 статик удалён) -> neo damageSources().fallingBlock(Entity=падающий блок=this)
		}
		return false;
	}

	/**
	 * F8 (шов «NBT-персистенс Entity», тот же приём моста CompoundTag<->ValueIO, что и F8-TE в
	 * {@code TileEntityBase01Root.saveAdditional/loadAdditional}): neo зовёт
	 * {@code addAdditionalSaveData(ValueOutput)}/{@code readAdditionalSaveData(ValueInput)}
	 * (`neo-decompiled/net/minecraft/world/entity/Entity.java:2121,2123`), а не GT6/1.7.10-модель
	 * {@code writeEntityToNBT}/{@code readEntityFromNBT}(NBTTagCompound). super.addAdditionalSaveData
	 * вызывается первым, чтобы сохранить neo-собственные данные FallingBlockEntity (Time/DropItem/
	 * BlockState/TileEntityData/…, см. FallingBlockEntity.java:290-302).
	 */
	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		CompoundTag aNBT = UT.NBT.make();
		aNBT.putShort("MetaData", ST.meta_(mStack));
		if (mBlockNBT != null) aNBT.put("TileEntityData", mBlockNBT);
		output.store(aNBT);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		CompoundTag aNBT = input.read(MapCodec.assumeMapUnsafe(CompoundTag.CODEC)).orElseGet(UT.NBT::make);
		mBlock = (IBlockPlacable)getBlockState().getBlock();
		mStack = ST.make(getBlockState().getBlock(), 1, aNBT.getShortOr("MetaData", (short)0));
		mBlockNBT = aNBT.getCompound("TileEntityData").orElse(null);
		ItemNBT.set(mStack, mBlockNBT);
	}
}

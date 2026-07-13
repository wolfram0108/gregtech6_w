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

import gregapi.block.IBlockPlacable;
import gregapi.code.ArrayListNoNulls;
import gregapi.code.ItemNBT;
import gregapi.util.ST;
import gregapi.util.UT;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;

/**
 * @author Gregorius Techneticies
 */
public class PrefixBlockFallingEntity extends FallingBlockEntity {
	protected IBlockPlacable mBlock;
	protected ItemStack mStack;
	
	public PrefixBlockFallingEntity(Level aWorld) {
		super(aWorld);
	}
	
	public PrefixBlockFallingEntity(Level aWorld, double aX, double aY, double aZ, IBlockPlacable aBlock, ItemStack aStack) {
		super(aWorld, aX, aY, aZ, (Block)aBlock, 0);
		mBlock = aBlock;
		mStack = aStack;
		field_145810_d = ItemNBT.get(aStack);
	}
	
	// @Override
	public void onUpdate() {
		prevPosX = getX();
		prevPosY = getY();
		prevPosZ = getZ();
		++field_145812_b;
		motionY -= 0.03999999910593033D;
		moveEntity(motionX, motionY, motionZ);
		motionX *= 0.9800000190734863D;
		motionY *= 0.9800000190734863D;
		motionZ *= 0.9800000190734863D;
		if (!level().isClientSide()) {
			int aX = UT.Code.roundDown(getX());
			int aY = UT.Code.roundDown(getY());
			int aZ = UT.Code.roundDown(getZ());
			if (field_145812_b == 1) {
				if (WD.block(level(), aX, aY, aZ) != super.func_145805_f()) {
					setDead();
					return;
				}
				level().setBlockToAir(aX, aY, aZ);
			}
			if (onGround) {
				motionX *= 0.699999988079071D;
				motionZ *= 0.699999988079071D;
				motionY *= -0.5D;
				if (WD.block(level(), aX, aY, aZ) != Blocks.MOVING_PISTON) {
					setDead();
					if (!level().canPlaceEntityOnSide(super.func_145805_f(), aX, aY, aZ, T, 1, null, mStack) || FallingBlock.func_149831_e(level(), aX, aY - 1, aZ) || !mBlock.placeBlock(level(), aX, aY, aZ, (byte)1, ST.meta_(mStack), ItemNBT.get(mStack), T, T)) {
						if (field_145813_c) if (mBlock instanceof PrefixBlock) {for (ItemStack tStack : ((PrefixBlock)mBlock).mDrops.getDrops((PrefixBlock)mBlock, level(), aX, aY, aZ, ST.meta_(mStack), null, 0, F)) entityDropItem(tStack, 0.0F);} else entityDropItem(mStack, 0.0F);
					}
				}
			} else if (field_145812_b > 100 && !level().isClientSide() && (aY < 1 || aY > 256) || field_145812_b > 600) {
				if (field_145813_c) if (mBlock instanceof PrefixBlock) {for (ItemStack tStack : ((PrefixBlock)mBlock).mDrops.getDrops((PrefixBlock)mBlock, level(), aX, aY, aZ, ST.meta_(mStack), null, 0, F)) entityDropItem(tStack, 0.0F);} else entityDropItem(mStack, 0.0F);
				setDead();
			}
		}
	}
	
	// @Override
	@SuppressWarnings("unchecked")
	protected void fall(float p_70069_1_) {
		int i = Mth.ceiling_float_int(p_70069_1_ - 1.0F);
		if (i > 0) for (Entity tEntity : new ArrayListNoNulls<Entity>(level().getEntities(this, boundingBox))) {
			if (tEntity instanceof LivingEntity) tEntity.hurt(damageSources().fallingBlock(this), TFC_DAMAGE_MULTIPLIER * Math.min(Mth.floor_float((float)i * 2), 40));// было DamageSource.fallingBlock (1.7.10 статик удалён) -> neo damageSources().fallingBlock(Entity=падающий блок=this)
		}
	}
	
	// @Override
	protected void writeEntityToNBT(CompoundTag aNBT) {
		super.writeEntityToNBT(aNBT);
		aNBT.putShort("MetaData", ST.meta_(mStack));
	}
	
	// @Override
	protected void readEntityFromNBT(CompoundTag aNBT) {
		super.readEntityFromNBT(aNBT);
		mBlock = (IBlockPlacable)super.func_145805_f();
		mStack = ST.make(super.func_145805_f(), 1, aNBT.getShort("MetaData"));
		ItemNBT.set(mStack, field_145810_d);
	}
	
	// @Override
	public Block func_145805_f() {
		return Blocks.GRAVEL;
	}
}

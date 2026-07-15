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

package gregtech.entities.ai;

import static gregapi.data.CS.*;

import gregapi.util.ST;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.entity.ai.EntityAIAttackOnCollide;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.ItemStack;
import net.minecraft.pathfinding.PathEntity;
import net.minecraft.pathfinding.PathPoint;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;

// Started off as a refactored copy of `EntityAIAttackOnCollide`
public class EntityAIBetterAttackOnCollide extends EntityAIBase {
	public Level mWorld;
	public PathEntity mPath;
	public Class<?> mTargetClass;
	public PathfinderMob mCreature;
	public int mAttackCoolDown, mPathCoolDown, mFailedPathFindingPenalty;
	public double mX, mY, mZ, mSpeedToTarget;
	public boolean mLastingMemory;
	
	public EntityAIBetterAttackOnCollide(EntityAIAttackOnCollide orig) {
		mTargetClass = orig.classTarget;
		mCreature = orig.attacker;
		mWorld = mCreature.level();
		mSpeedToTarget = orig.speedTowardsTarget;
		mLastingMemory = orig.longMemory;
		setMutexBits(3);
	}
	
	@Override
	public boolean shouldExecute() {
		LivingEntity entitylivingbase = mCreature.getAttackTarget();
		if (entitylivingbase == null) return F;
		if (!entitylivingbase.isAlive()) return F;
		if (mTargetClass != null && !mTargetClass.isAssignableFrom(entitylivingbase.getClass())) return F;
		
		if (--mPathCoolDown <= 0) {
			mPath = mCreature.getNavigator().getPathToEntityLiving(entitylivingbase);
			mPathCoolDown = 4 + mCreature.getRNG().nextInt(7);
			return mPath != null;
		}
		return T;
	}
	
	@Override
	public boolean continueExecuting() {
		LivingEntity tTarget = mCreature.getAttackTarget();
		return tTarget != null && tTarget.isAlive() && (!mLastingMemory ? !mCreature.getNavigator().noPath() : mCreature.isWithinHomeDistance(Mth.floor_double(tTarget.getX()), Mth.floor_double(tTarget.getY()), Mth.floor_double(tTarget.getZ())));
	}
	
	@Override
	public void startExecuting() {
		mCreature.getNavigator().setPath(mPath, mSpeedToTarget);
		mPathCoolDown = 0;
	}
	
	@Override
	public void resetTask() {
		mCreature.getNavigator().clearPathEntity();
	}
	
	@Override
	public void updateTask() {
		LivingEntity tTarget = mCreature.getAttackTarget();
		mCreature.getLookHelper().setLookPositionWithEntity(tTarget, 30, 30);
		double tTargetDistance = mCreature.distanceToSqr(tTarget.getX(), tTarget.getBoundingBox().minY, tTarget.getZ());
		double tLookRadius = mCreature.width * mCreature.width * 4 + tTarget.width;
		mPathCoolDown--;
		if ((mLastingMemory || mCreature.getEntitySenses().canSee(tTarget)) && mPathCoolDown <= 0 && ((mX == 0 && mY == 0 && mZ == 0) || tTarget.distanceToSqr(mX, mY, mZ) >= 1 || mCreature.getRNG().nextFloat() < 0.05F)) {
			mX = tTarget.getX(); mY = tTarget.getBoundingBox().minY; mZ = tTarget.getZ();
			
			mPathCoolDown = mFailedPathFindingPenalty + 4 + mCreature.getRNG().nextInt(7);
			if (mCreature.getNavigator().getPath() != null) {
				PathPoint tPathPoint = mCreature.getNavigator().getPath().getFinalPathPoint();
				if (tPathPoint != null && tTarget.distanceToSqr(tPathPoint.x, tPathPoint.y, tPathPoint.z) < 1) {
					mFailedPathFindingPenalty = 0;
				} else {
					mFailedPathFindingPenalty += 10;
				}
			} else {
				mFailedPathFindingPenalty += 10;
			}
			
			if (tTargetDistance > 1024) {
				mPathCoolDown += 10;
			} else if (tTargetDistance > 256) {
				mPathCoolDown += 5;
			}
			
			if (!mCreature.getNavigator().tryMoveToEntityLiving(tTarget, mSpeedToTarget)) {
				mPathCoolDown += 15;
			}
		}
		
		mAttackCoolDown = Math.max(mAttackCoolDown - 1, 0);
		if (tTargetDistance <= tLookRadius && mAttackCoolDown <= 0) {
			mAttackCoolDown = 5;
			
			boolean tAttacking = T;
			ItemStack tHeld = ST.valisize(mCreature.getHeldItem());
			
			if (tHeld != null) {
				mCreature.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
				if (ZOMBIES_IGNITE_HELD_TNT && ST.equal_(tHeld, Blocks.TNT)) {
					mAttackCoolDown = 20;
					tAttacking = F;
					
					tHeld.setCount(tHeld.getCount()-1);
					if (tHeld.getCount() <= 0) mCreature.setCurrentItemOrArmor(0, NI);
					
					if (!mWorld.isClientSide()) {
						PrimedTnt entitytntprimed = new PrimedTnt(mWorld, mCreature.getX(), mCreature.getY(), mCreature.getZ(), mCreature);
						mWorld.addFreshEntity(entitytntprimed);
						mWorld.playSoundAtEntity(entitytntprimed, "game.tnt.primed", 1, 1);
					}
				} else
				if (ZOMBIES_DIG_WITH_TOOLS) {
					// TODO: Handle tools to break things
					// 1. figure out what the `held` item can work on.
					// 2. Get nearby block that the tool works on adjust up/down or even if the target is high or low in comparison, skip TE's and such perhaps with ZOMBIES_DIG_TILEENTITIES.
					// 3. Use up some of the `held` tool and break that block then set `attackTick` to something hig based on toughness of that block or so (or add another counter to take 'time' to break something?)
				}
			}
			
			if (tAttacking) mCreature.attackEntityAsMob(tTarget);
			
			// TODO: playSound("creeper.primed", 1, 0.5);
		}
	}
}

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

package gregapi.random;

import static gregapi.data.CS.*;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import gregapi.data.CS.SFX;
import gregapi.util.UT;
import net.minecraft.world.level.block.Block;
import gregapi.block.Material;
import net.minecraft.enchantment.EnchantmentProtection;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.enderdragon.EnderDragonPart;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.network.protocol.game.ClientboundExplodePacket;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;

/**
 * @author Gregorius Techneticies
 */
public class ExplosionGT extends Explosion {
	public static ExplosionGT explode(Level aWorld, Entity aEntity, double aX, double aY, double aZ, float aPower, boolean aFlaming, boolean aSmoking) {
		ExplosionGT tExplosion = new ExplosionGT(aWorld, aEntity, aX, aY, aZ, aPower);
		tExplosion.isFlaming = aFlaming;
		tExplosion.isSmoking = aSmoking;
		if (net.neoforged.neoforge.event.EventHooks.onExplosionStart(aWorld, tExplosion)) return tExplosion;
		tExplosion.doExplosionA();
		if (aWorld instanceof ServerLevel) {
			tExplosion.doExplosionB(F);
			if (!aSmoking) tExplosion.affectedBlockPositions.clear();
			@SuppressWarnings("rawtypes")
			Iterator tIterator = aWorld.playerEntities.iterator();
			while (tIterator.hasNext()) {
				Player tPlayer = (Player)tIterator.next();
				if (tPlayer.getDistanceSq(aX, aY, aZ) < 4096) {
					((ServerPlayer)tPlayer).playerNetServerHandler.sendPacket(new ClientboundExplodePacket(aX, aY, aZ, aPower, tExplosion.affectedBlockPositions, (Vec3)tExplosion.func_77277_b().get(tPlayer)));
				}
			}
		} else {
			tExplosion.doExplosionB(T);
		}
		return tExplosion;
	}
	
	public ExplosionGT(Level aWorld, Entity aEntity, double aX, double aY, double aZ, float aPower) {
		super(aWorld, aEntity, aX, aY, aZ, aPower);
		mWorld = aWorld;
	}
	
	private Level mWorld;
	@SuppressWarnings("rawtypes")
	private Map field_77288_k = new HashMap<>();
	
	@Override
	@SuppressWarnings("unchecked")
	public void doExplosionA() {
		float tSize = explosionSize;
		HashSet<BlockPos> tPositions = new HashSet<>();
		for (int i = 0; i < 16; ++i) for (int j = 0; j < 16; ++j) for (int k = 0; k < 16; ++k) {
			if (i == 0 || i == 15 || j == 0 || j == 15 || k == 0 || k == 15) {
				double tIncX = i / 7.5F - 1, tIncY = j / 7.5F - 1, tIncZ = k / 7.5F - 1;
				double tDist = Math.sqrt(tIncX * tIncX + tIncY * tIncY + tIncZ * tIncZ);
				tIncX /= tDist; tIncY /= tDist; tIncZ /= tDist;
				float tPow = tSize * (0.7F + mWorld.rand.nextFloat() * 0.6F);
				double tX = explosionX, tY = explosionY, tZ = explosionZ;
				for (float tMul = 0.3F; tPow > 0; tPow -= tMul * 0.75F) {
					int tFloorX = UT.Code.roundDown(tX), tFloorY = UT.Code.roundDown(tY), tFloorZ = UT.Code.roundDown(tZ);
					Block tBlock = mWorld.getBlock(tFloorX, tFloorY, tFloorZ);
					if (tBlock.getMaterial() != Material.air) {
						float f3 = exploder != null ? exploder.func_145772_a(this, mWorld, tFloorX, tFloorY, tFloorZ, tBlock) : tBlock.getExplosionResistance(exploder, mWorld, tFloorX, tFloorY, tFloorZ, explosionX, explosionY, explosionZ);
						tPow -= (f3 + 0.3F) * tMul;
					}
					if (tPow > 0 && (exploder == null || exploder.func_145774_a(this, mWorld, tFloorX, tFloorY, tFloorZ, tBlock, tPow))) {
						tPositions.add(new BlockPos(tFloorX, tFloorY, tFloorZ));
					}
					tX += tIncX * tMul; tY += tIncY * tMul; tZ += tIncZ * tMul;
				}
			}
		}
		affectedBlockPositions.addAll(tPositions);
		tSize *= 2;
		@SuppressWarnings("rawtypes")
		List tEntities = mWorld.getEntitiesWithinAABBExcludingEntity(exploder, AABB.getBoundingBox(UT.Code.roundDown(explosionX - tSize - 1), UT.Code.roundDown(explosionY - tSize - 1), UT.Code.roundDown(explosionZ - tSize - 1), UT.Code.roundDown(explosionX + tSize + 1), UT.Code.roundDown(explosionY + tSize + 1), UT.Code.roundDown(explosionZ + tSize + 1)));
		net.neoforged.neoforge.event.EventHooks.onExplosionDetonate(mWorld, this, tEntities, tSize);
		Vec3 tVec3 = Vec3.createVectorHelper(explosionX, explosionY, explosionZ);
		for (int i1 = 0; i1 < tEntities.size(); ++i1) {
			Entity tEntity = (Entity)tEntities.get(i1);
			double tEntityDist = tEntity.getDistance(explosionX, explosionY, explosionZ) / tSize;
			if (tEntityDist <= 1 && !(tEntity instanceof WitherBoss || tEntity instanceof EnderDragon || tEntity instanceof EnderDragonPart || tEntity.getClass().getName().toLowerCase().contains("boss"))) {
				double tKnockX = tEntity.getX() - explosionX, tKnockY = tEntity.getY() + tEntity.getEyeHeight() - explosionY, tKnockZ = tEntity.getZ() - explosionZ;
				double tDist = Mth.sqrt_double(tKnockX * tKnockX + tKnockY * tKnockY + tKnockZ * tKnockZ);
				if (tDist > 0) {
					tKnockX /= tDist;
					tKnockY /= tDist;
					tKnockZ /= tDist;
					double tKnockback = (1 - tEntityDist) * mWorld.getBlockDensity(tVec3, tEntity.boundingBox);
					tEntity.attackEntityFrom(DamageSource.setExplosionSource(this), ((int)((tKnockback * tKnockback + tKnockback) * 4 * tSize + 1)) * TFC_DAMAGE_MULTIPLIER);
					double tBlastProtection = EnchantmentProtection.func_92092_a(tEntity, tKnockback);
					tEntity.motionX += tKnockX * tBlastProtection;
					tEntity.motionY += tKnockY * tBlastProtection;
					tEntity.motionZ += tKnockZ * tBlastProtection;
					
					if (tEntity instanceof Player) field_77288_k.put(tEntity, Vec3.createVectorHelper(tKnockX * tKnockback, tKnockY * tKnockback, tKnockZ * tKnockback));
				}
			}
		}
	}
	
	@Override
	public void doExplosionB(boolean aEffects) {
		mWorld.playSoundEffect(explosionX, explosionY, explosionZ, SFX.MC_EXPLODE, 4, (1 + (mWorld.rand.nextFloat() - mWorld.rand.nextFloat()) * 0.2F) * 0.7F);
		mWorld.spawnParticle(explosionSize >= 2 && isSmoking ? "hugeexplosion" : "largeexplode", explosionX, explosionY, explosionZ, 1, 0, 0);
		if (isSmoking) {
			@SuppressWarnings("rawtypes")
			Iterator tIterator = affectedBlockPositions.iterator();
			while (tIterator.hasNext()) {
				final BlockPos tPos = (BlockPos)tIterator.next();
				final Block tBlock = mWorld.getBlock(tPos.chunkPosX, tPos.chunkPosY, tPos.chunkPosZ);
				if (aEffects) {
					double d0 = (tPos.chunkPosX + mWorld.rand.nextFloat());
					double d1 = (tPos.chunkPosY + mWorld.rand.nextFloat());
					double d2 = (tPos.chunkPosZ + mWorld.rand.nextFloat());
					double d3 = d0 - explosionX;
					double d4 = d1 - explosionY;
					double d5 = d2 - explosionZ;
					double d6 = Mth.sqrt_double(d3 * d3 + d4 * d4 + d5 * d5);
					d3 /= d6;
					d4 /= d6;
					d5 /= d6;
					double d7 = 0.5D / (d6 / explosionSize + 0.1D);
					d7 *= (mWorld.rand.nextFloat() * mWorld.rand.nextFloat() + 0.3F);
					d3 *= d7;
					d4 *= d7;
					d5 *= d7;
					mWorld.spawnParticle("explode", (d0 + explosionX) / 2, (d1 + explosionY) / 2, (d2 + explosionZ) / 2, d3, d4, d5);
					mWorld.spawnParticle("smoke", d0, d1, d2, d3, d4, d5);
				}
				if (tBlock.getMaterial() != Material.air) {
					if (tBlock.canDropFromExplosion(this)) tBlock.dropBlockAsItemWithChance(mWorld, tPos.chunkPosX, tPos.chunkPosY, tPos.chunkPosZ, mWorld.getBlockMetadata(tPos.chunkPosX, tPos.chunkPosY, tPos.chunkPosZ), 1 / explosionSize, 0);
					tBlock.onBlockExploded(mWorld, tPos.chunkPosX, tPos.chunkPosY, tPos.chunkPosZ, this);
				}
			}
		}
		if (isFlaming) {
			@SuppressWarnings("rawtypes")
			Iterator tIterator = affectedBlockPositions.iterator();
			while (tIterator.hasNext()) {
				final BlockPos tPos = (BlockPos)tIterator.next();
				final Block tBlock = mWorld.getBlock(tPos.chunkPosX, tPos.chunkPosY, tPos.chunkPosZ), tAbove = mWorld.getBlock(tPos.chunkPosX, tPos.chunkPosY - 1, tPos.chunkPosZ);
				if (tBlock.getMaterial() == Material.air && tAbove.func_149730_j() && RNGSUS.nextInt(3) == 0) {
					mWorld.setBlock(tPos.chunkPosX, tPos.chunkPosY, tPos.chunkPosZ, Blocks.fire);
				}
			}
		}
	}
	
	@SuppressWarnings("rawtypes")
	@Override public Map func_77277_b() {return field_77288_k;}
	@Override public LivingEntity getExplosivePlacedBy() {return exploder == null ? null : (exploder instanceof PrimedTnt ? ((PrimedTnt)exploder).getTntPlacedBy() : (exploder instanceof LivingEntity ? (LivingEntity)exploder : null));}
}

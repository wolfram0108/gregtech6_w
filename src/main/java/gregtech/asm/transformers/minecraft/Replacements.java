/**
 * Copyright (c) 2022 GregTech-6 Team
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

package gregtech.asm.transformers.minecraft;

import gregapi.util.WD;
import java.util.Random;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import gregapi.block.Material;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.Level;
import net.minecraft.core.Direction;

/* This is a separate file so it class loads *while* minecraft loads,
   if we accessed world in the main transformer then we can miss out
   on the transformations.  Not an issue when accessing MC classes
   while transforming other mods though.
 */
public class Replacements {

	/** Zombies convert their Victim. */
	public static void EntityZombie_onKillEntity(Object aZombie, Object aVictim) {
		// Just ALWAYS convert Villagers, not only sometimes or when the stupid Difficulty Setting is right.
		if (aVictim instanceof Villager) {
			Villager aVillager = (Villager)aVictim;
			Level aWorld = aVillager.level();
			// neo: villager-зомби — отдельный класс ZombieVillager (1.7.10 Zombie.setVillager(true) удалён). F-ASM: метод мёртв в neo (коремод не применяется) → Mixin, тело портируется на neo-символы для компиляции.
			net.minecraft.world.entity.monster.ZombieVillager tZombieVillager = new net.minecraft.world.entity.monster.ZombieVillager(net.minecraft.world.entity.EntityType.ZOMBIE_VILLAGER, aWorld);
			tZombieVillager.copyPosition(aVillager); // было copyLocationAndAnglesFrom
			// onSpawnWithEgg/finalizeSpawn (инициализация конверсии) движок делает сам — в neo требует ServerLevelAccessor+EntitySpawnReason, опущено в мёртвом ASM-теле.
			tZombieVillager.setCanPickUpLoot(false);
			tZombieVillager.setPersistenceRequired(); // было func_110163_bv (не деспавнить)
			tZombieVillager.setBaby(aVillager.isBaby()); // было setChild
			tZombieVillager.setCustomName(aVillager.getCustomName()); // было setCustomNameTag/getCustomNameTag (Component, не String)
			aVillager.setCustomName(null);
			aWorld.addFreshEntity(tZombieVillager);
			aWorld.levelEvent(1016, tZombieVillager.blockPosition(), 0); // было playAuxSFXAtEntity(null,1016,x,y,z,0)
			aVillager.discard(); // было removeEntity
		}
	}
	
	public static void BlockStaticLiquid_updateTick(LiquidBlock self, Level world, int x, int y, int z, Random rand) {
		if (WD.getMaterial(self) == Material.lava)
		{
			int l = rand.nextInt(3);
			int i1;

			for (i1 = 0; i1 < l; ++i1)
			{
				x += rand.nextInt(3) - 1;
				++y;
				z += rand.nextInt(3) - 1;
				Block block = WD.block(world, x, y, z);

				if (WD.getMaterial(block) == Material.air)
				{
					if (
						BlockStaticLiquid_isFlammable(world, x - 1, y, z, Direction.EAST) ||
						BlockStaticLiquid_isFlammable(world, x + 1, y, z, Direction.WEST) ||
						BlockStaticLiquid_isFlammable(world, x, y, z - 1, Direction.SOUTH) ||
						BlockStaticLiquid_isFlammable(world, x, y, z + 1, Direction.NORTH) ||
						BlockStaticLiquid_isFlammable(world, x, y - 1, z, Direction.UP) ||
						BlockStaticLiquid_isFlammable(world, x, y + 1, z, Direction.DOWN))
					{
						WD.set(world, x, y, z, Blocks.FIRE, 0, 3);
						return;
					}
				}
				else if (WD.getMaterial(block).blocksMovement())
				{
					return;
				}
			}

			if (l == 0)
			{
				i1 = x;
				int k1 = z;

				for (int j1 = 0; j1 < 3; ++j1)
				{
					x = i1 + rand.nextInt(3) - 1;
					z = k1 + rand.nextInt(3) - 1;

					if (world.isEmptyBlock(new net.minecraft.core.BlockPos(x, y + 1, z)) && BlockStaticLiquid_isFlammable(world, x, y, z, Direction.UP))
					{
						WD.set(world, x, y + 1, z, Blocks.FIRE, 0, 3);
					}
				}
			}
		}
	}

	public static boolean BlockStaticLiquid_isFlammable(Level world, int x, int y, int z, Direction dir) {
		net.minecraft.core.BlockPos p = new net.minecraft.core.BlockPos(x, y, z);
		return WD.block(world, x, y, z).isFlammable(world.getBlockState(p), world, p, dir); // neo IBlockExtension.isFlammable(BlockState,BlockGetter,BlockPos,Direction)
	}

	public static boolean BlockStaticLiquid_isFlammable(Level world, int x, int y, int z) {
		net.minecraft.core.BlockPos p = new net.minecraft.core.BlockPos(x, y, z);
		return WD.block(world, x, y, z).isFlammable(world.getBlockState(p), world, p, Direction.UP); // было ForgeDirection.UNKNOWN (нет в neo) → UP-дефолт
	}

	public static boolean EntityAICreeperSwell_shouldExecute(Creeper swellingCreeper) {
		LivingEntity target = swellingCreeper.getTarget();
		if(swellingCreeper.getSwellDir() > 0) return true;
		if(target == null) return false;
		double distSq = swellingCreeper.distanceToSqr(target);
		if(distSq >= 9.0) return false;
		// Do this last since it's the most 'work'
		double facing = new Vec3(target.getX(), target.getY(), target.getZ())
				.subtract(new Vec3(swellingCreeper.getX(), swellingCreeper.getY(), swellingCreeper.getZ()))
				.normalize()
				.dot(target.getLookAngle());
		return facing >= -0.F;
	}
}

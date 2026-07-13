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
 */

package gregtech.asm.transformers.minecraft;

import java.util.Random;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import gregapi.block.Material;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.entity.IEntityLivingData;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.entity.passive.EntityVillager;
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
		if (aVictim instanceof EntityVillager) {
			EntityVillager aVillager = (EntityVillager)aVictim;
			Level aWorld = aVillager.level();
			// Yep, new Zombie Object.
			Zombie tZombieVillager = new Zombie(aWorld);
			// Location and Head need to point to the right places.
			tZombieVillager.copyLocationAndAnglesFrom(aVillager);
			// Do normal spawning Stuff.
			tZombieVillager.onSpawnWithEgg((IEntityLivingData)null);
			// Converting Villager Zombies back to Villagers would make it impossible to retrieve the Items, so don't pick up in the first place!
			tZombieVillager.setCanPickUpLoot(false);
			// Well yes, he clearly is a Villager Zombie.
			tZombieVillager.setVillager(true);
			// STAY THERE! DONT DESPAWN!
			tZombieVillager.func_110163_bv();
			// Convert to Child
			tZombieVillager.setChild(aVillager.isChild());
			// Transfer Name Tag
			tZombieVillager.setCustomNameTag(aVillager.getCustomNameTag());
			// Prevent duping Name Tags
			aVillager.setCustomNameTag("");
			// And put the new Zombie into the World!
			aWorld.addFreshEntity(tZombieVillager);
			// With Sound ofcourse!
			aWorld.playAuxSFXAtEntity(null, 1016, (int)tZombieVillager.getX(), (int)tZombieVillager.getY(), (int)tZombieVillager.getZ(), 0);
			// Villager? What Villager?
			aWorld.removeEntity(aVillager);
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
						world.setBlock(x, y, z, Blocks.FIRE);
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

					if (world.isAirBlock(x, y + 1, z) && BlockStaticLiquid_isFlammable(world, x, y, z, Direction.UP))
					{
						world.setBlock(x, y + 1, z, Blocks.FIRE);
					}
				}
			}
		}
	}

	public static boolean BlockStaticLiquid_isFlammable(Level world, int x, int y, int z, Direction dir) {
		return WD.block(world, x, y, z).isFlammable(world, x, y, z, dir);
	}

	public static boolean BlockStaticLiquid_isFlammable(Level world, int x, int y, int z) {
		return WD.block(world, x, y, z).isFlammable(world, x, y, z, Direction.UNKNOWN);
	}

	public static boolean EntityAICreeperSwell_shouldExecute(Creeper swellingCreeper) {
		LivingEntity target = swellingCreeper.getAttackTarget();
		if(swellingCreeper.getCreeperState() > 0) return true;
		if(target == null) return false;
		double distSq = swellingCreeper.distanceToSqr(target);
		if(distSq >= 9.0) return false;
		// Do this last since it's the most 'work'
		double facing = Vec3
				.createVectorHelper(target.getX(), target.getY(), target.getZ())
				.subtract(Vec3.createVectorHelper(swellingCreeper.getX(), swellingCreeper.getY(), swellingCreeper.getZ()))
				.normalize()
				.dotProduct(target.getLookAngle());
		return facing >= -0.F;
	}
}

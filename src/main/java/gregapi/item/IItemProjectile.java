/**
 * Copyright (c) 2019 Gregorius Techneticies
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

package gregapi.item;

import gregapi.code.TagData;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * @author Gregorius Techneticies
 */
public interface IItemProjectile {
	/** @return if this Item has an Arrow Entity */
	public boolean hasProjectile(TagData aProjectileType, ItemStack aStack);
	/** @return an Arrow Entity to be spawned. If null then this is not an Arrow. Note: Other Projectiles still extend EntityArrow */
	public EntityProjectile getProjectile(TagData aProjectileType, ItemStack aStack, Level aWorld, double aX, double aY, double aZ);
	/** @return an Arrow Entity to be spawned. If null then this is not an Arrow. Note: Other Projectiles still extend EntityArrow */
	public EntityProjectile getProjectile(TagData aProjectileType, ItemStack aStack, Level aWorld, LivingEntity aEntity, float aSpeed);
	
	/** Class for being able to set the ItemStack when launching the Projectile. And for de-obfuscation of Parameters. */
	public static abstract class EntityProjectile extends Arrow {
		public EntityProjectile(Level aWorld) {
			super(aWorld);
		}
		public EntityProjectile(Level aWorld, double aX, double aY, double aZ) {
			super(aWorld, aX, aY, aZ);
		}
		public EntityProjectile(Level aWorld, LivingEntity aShootingEntity, LivingEntity aWhateverThatIs, float aSpeed, float aPrecision) {
			super(aWorld, aShootingEntity, aWhateverThatIs, aSpeed, aPrecision);
		}
		public EntityProjectile(Level aWorld, LivingEntity aShootingEntity, float aSpeed) {
			super(aWorld, aShootingEntity, aSpeed);
		}
		
		public abstract void setProjectileStack(ItemStack aStack);
	}
}

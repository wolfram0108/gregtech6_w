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
 *
 * Modified in 2026 for the GregTech 6 NeoForge port
 * (https://github.com/wolfram0108/gregtech6_w): ported from Minecraft 1.7.10 / Forge
 * to Minecraft 26.1.2 / NeoForge.
 */

package gregapi.item;

import gregapi.code.TagData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Arrow;
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
		// F-entity-construction (ЗАКРЫТО): РЕАЛЬНЫЙ EntityType протягивается сюда подклассами (EntityArrow_Material/
		// _Potion → gregtech.entities.EntitiesGT.ARROW_*), заменяя прежний плейсхолдер EntityType.ARROW. neo Arrow
		// позиционные ctor'ы (Level,x,y,z,…)/(Level,shooter,…) внутри ХАРДКОДЯТ EntityType.ARROW (neo-decompiled
		// Arrow.java:29,34), поэтому строим через тип-ctor super(aType,aWorld) + setPos / shootFromRotation.
		// Скоростной ctor воспроизводит 1.7.10 EntityArrow(World,shooter,speed): позиция у глаз стрелка, скорость от
		// взгляда (vanilla setThrowableHeading(..., speed*1.5, 1.0) ≡ neo shootFromRotation(shooter,pitch,yaw,0,speed*1.5,1.0)).
		protected EntityProjectile(EntityType<? extends Arrow> aType, Level aWorld) {
			super(aType, aWorld);
		}
		protected EntityProjectile(EntityType<? extends Arrow> aType, Level aWorld, double aX, double aY, double aZ) {
			super(aType, aWorld);
			setPos(aX, aY, aZ);
		}
		protected EntityProjectile(EntityType<? extends Arrow> aType, Level aWorld, LivingEntity aShootingEntity, float aSpeed) {
			super(aType, aWorld);
			setPos(aShootingEntity.getX(), aShootingEntity.getEyeY() - 0.1, aShootingEntity.getZ());
			setOwner(aShootingEntity);
			shootFromRotation(aShootingEntity, aShootingEntity.getXRot(), aShootingEntity.getYRot(), 0.0F, aSpeed * 1.5F, 1.0F);
		}

		// F-arrow-enchants ЦЕНТР: в 1.7.10 оба метода жили на ДВИЖКОВОМ предке EntityArrow
		// (setKnockbackStrength(int) / getDamage()), поэтому обработчик выстрела применял чары лука к любому
		// снаряду GT6 без различения типа. neo AbstractArrow оставил только setBaseDamage: своего knockback у
		// стрелы нет вовсе (движок считает его из приватного firedFromWeapon, задаваемого лишь конструктором
		// Arrow(Level,…,weapon), который хардкодит EntityType.ARROW — снарядам GT6 со своим EntityType недоступен),
		// а baseDamage private без геттера. Держим оба здесь, на общем предке снарядов GT6 — том же уровне,
		// на котором их держал оригинал, чтобы приём не расползался по подклассам.
		protected int mKnockback = 0;

		/** 1.7.10 {@code EntityArrow.setKnockbackStrength(int)}: величина отбрасывания, применяется при попадании. */
		public void setKnockbackStrength(int aKnockback) {mKnockback = aKnockback;}

		/** 1.7.10 {@code EntityArrow.getDamage()}: neo {@code AbstractArrow.baseDamage} private и без геттера
		 *  (есть только сеттер, AbstractArrow.java:671) — единственное место чтения на весь мод. */
		public double getBaseDamageGT() {
			try {
				java.lang.reflect.Field tField = net.minecraft.world.entity.projectile.AbstractArrow.class.getDeclaredField("baseDamage");
				tField.setAccessible(true);
				return tField.getDouble(this);
			} catch (Throwable e) {return 2.0;}
		}

		public abstract void setProjectileStack(ItemStack aStack);
	}
}

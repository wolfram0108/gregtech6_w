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

package gregapi.item.multiitem.behaviors;

import static gregapi.data.CS.*;

import java.util.List;

import gregapi.code.TagData;
import gregapi.item.IItemProjectile.EntityProjectile;
import gregapi.item.multiitem.MultiItem;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.core.BlockSource;
import net.minecraft.core.Position;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

/**
 * @author Gregorius Techneticies
 */
public interface IBehavior<E extends Item> {
	public boolean onLeftClickEntity(E aItem, ItemStack aStack, Player aPlayer, Entity aEntity);
	public boolean onRightClickEntity(E aItem, ItemStack aStack, Player aPlayer, Entity aEntity);
	public boolean onItemUse(E aItem, ItemStack aStack, Player aPlayer, Level aWorld, int aX, int aY, int aZ, byte aSide, float hitX, float hitY, float hitZ);
	public boolean onItemUseFirst(E aItem, ItemStack aStack, Player aPlayer, Level aWorld, int aX, int aY, int aZ, byte aSide, float hitX, float hitY, float hitZ);
	public ItemStack onItemRightClick(E aItem, ItemStack aStack, Level aWorld, Player aPlayer);
	public List<String> getAdditionalToolTips(E aItem, List<String> aList, ItemStack aStack);
	public void onUpdate(E aItem, ItemStack aStack, Level aWorld, Entity aPlayer, int aTimer, boolean aIsInHand);
	public boolean isItemStackUsable(E aItem, ItemStack aStack);
	public boolean canDispense(E aItem, BlockSource aSource, ItemStack aStack);
	public ItemStack onDispense(E aItem, BlockSource aSource, ItemStack aStack);
	public boolean hasProjectile(E aItem, TagData aProjectileType, ItemStack aStack);
	public EntityProjectile getProjectile(E aItem, TagData aProjectileType, ItemStack aStack, Level aWorld, double aX, double aY, double aZ);
	public EntityProjectile getProjectile(E aItem, TagData aProjectileType, ItemStack aStack, Level aWorld, LivingEntity aEntity, float aSpeed);
	
	public abstract class AbstractBehaviorDefault implements IBehavior<MultiItem> {
		@Override public boolean onLeftClickEntity(MultiItem aItem, ItemStack aStack, Player aPlayer, Entity aEntity) {return F;}
		@Override public boolean onRightClickEntity(MultiItem aItem, ItemStack aStack, Player aPlayer, Entity aEntity) {return F;}
		@Override public boolean onItemUse(MultiItem aItem, ItemStack aStack, Player aPlayer, Level aWorld, int aX, int aY, int aZ, byte aSide, float hitX, float hitY, float hitZ) {return F;}
		@Override public boolean onItemUseFirst(MultiItem aItem, ItemStack aStack, Player aPlayer, Level aWorld, int aX, int aY, int aZ, byte aSide, float hitX, float hitY, float hitZ) {return F;}
		@Override public ItemStack onItemRightClick(MultiItem aItem, ItemStack aStack, Level aWorld, Player aPlayer) {return aStack;}
		@Override public List<String> getAdditionalToolTips(MultiItem aItem, List<String> aList, ItemStack aStack) {return aList;}
		@Override public void onUpdate(MultiItem aItem, ItemStack aStack, Level aWorld, Entity aPlayer, int aTimer, boolean aIsInHand) {/**/}
		@Override public boolean isItemStackUsable(MultiItem aItem, ItemStack aStack) {return T;}
		@Override public boolean canDispense(MultiItem aItem, BlockSource aSource, ItemStack aStack) {return F;}
		@Override public boolean hasProjectile(MultiItem aItem, TagData aProjectileType, ItemStack aStack) {return F;}
		@Override public EntityProjectile getProjectile(MultiItem aItem, TagData aProjectileType, ItemStack aStack, Level aWorld, double aX, double aY, double aZ) {return null;}
		@Override public EntityProjectile getProjectile(MultiItem aItem, TagData aProjectileType, ItemStack aStack, Level aWorld, LivingEntity aEntity, float aSpeed) {return null;}
		
		@Override
		public ItemStack onDispense(MultiItem aItem, BlockSource aSource, ItemStack aStack) {
			Direction enumfacing = aSource.getBlockState().getValue(DispenserBlock.FACING); // F-dispenser: func_149937_b(metadata) -> facing в BlockState (BlockSource=record, DispenserBlock.java:50).
			Position iposition = DispenserBlock.getDispensePosition(aSource);
			ItemStack itemstack1 = aStack.split(1);
			DefaultDispenseItemBehavior.spawnItem(aSource.getLevel(), itemstack1, 6, enumfacing, iposition); // F-dispenser: doDispense -> spawnItem (DefaultDispenseItemBehavior.java:30).
			return aStack;
		}
	}
	
	@Deprecated public abstract class Behaviour_None extends AbstractBehaviorDefault {/**/}
}

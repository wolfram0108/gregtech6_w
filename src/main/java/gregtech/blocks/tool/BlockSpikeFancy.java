/**
 * Copyright (c) 2023 GregTech-6 Team
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

package gregtech.blocks.tool;

import static gregapi.data.CS.*;

import java.util.List;

import gregapi.block.misc.BlockBaseSpike;
import gregapi.damage.DamageSources;
import gregapi.data.LH;
import gregapi.data.MT;
import gregapi.util.UT;
import gregapi.util.WD;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.monster.skeleton.Skeleton;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;

public class BlockSpikeFancy extends BlockBaseSpike {
	public BlockSpikeFancy(String aNameInternal) {
		super(aNameInternal, MT.Au, MT.Ag);
		LH.add(getUnlocalizedName()+ ".0", "Gold Wall Spike");
		LH.add(getUnlocalizedName()+ ".1", "Gold Wall Spike");
		LH.add(getUnlocalizedName()+ ".2", "Gold Wall Spike");
		LH.add(getUnlocalizedName()+ ".3", "Gold Wall Spike");
		LH.add(getUnlocalizedName()+ ".4", "Gold Wall Spike");
		LH.add(getUnlocalizedName()+ ".5", "Gold Wall Spike");
		LH.add(getUnlocalizedName()+ ".6", "Gold Block Spike");
		LH.add(getUnlocalizedName()+ ".7", "Falling Gold Spike Block");
		LH.add(getUnlocalizedName()+ ".8", "Silver Wall Spike");
		LH.add(getUnlocalizedName()+ ".9", "Silver Wall Spike");
		LH.add(getUnlocalizedName()+".10", "Silver Wall Spike");
		LH.add(getUnlocalizedName()+".11", "Silver Wall Spike");
		LH.add(getUnlocalizedName()+".12", "Silver Wall Spike");
		LH.add(getUnlocalizedName()+".13", "Silver Wall Spike");
		LH.add(getUnlocalizedName()+".14", "Silver Block Spike");
		LH.add(getUnlocalizedName()+".15", "Falling Silver Spike Block");
	}
	
	@Override
	public void addInformation(ItemStack aStack, byte aMeta, Player aPlayer, List<String> aList, boolean aF3_H) {
		if (aMeta < 8) {
			aList.add(LH.Chat.ORANGE + "Deals huge Damage to any Undead touching it!");
			aList.add(LH.Chat.ORANGE + "Does very low Damage to anything else!");
			aList.add(LH.Chat.ORANGE + "Doesn't work on Slimes and Iron Golems.");
		} else {
			aList.add(LH.Chat.ORANGE + "Deals huge Damage to any Enderman, Werewolf or Bear989Sr touching it!");
			aList.add(LH.Chat.ORANGE + "Does very low Damage to anything else!");
			aList.add(LH.Chat.ORANGE + "Doesn't work on Skeletons, Slimes and Iron Golems.");
		}
		if ((aMeta & 7) >= 6) {
			aList.add(LH.Chat.CYAN + "Works in all Directions, but only does half the Wall Spikes Damage!");
		}
	}
	
	@Override
	public void onEntityCollidedWithBlock(Level aWorld, int aX, int aY, int aZ, Entity aEntity) {
		int aMeta = WD.meta(aWorld, aX, aY, aZ);
		if (aEntity instanceof LivingEntity) {
			if (aMeta < 8) {
				if (((LivingEntity)aEntity).getType().builtInRegistryHolder().is(EntityTypeTags.UNDEAD))
				aEntity.hurt(DamageSources.getSpikeDamage(), TFC_DAMAGE_MULTIPLIER * ((aMeta & 7) < 6 ? 20.0F : 10.0F));
				else if (!(aEntity instanceof IronGolem || aEntity instanceof Skeleton || aEntity instanceof Slime))
				aEntity.hurt(DamageSources.getSpikeDamage(), TFC_DAMAGE_MULTIPLIER * ((aMeta & 7) < 6 ?  2.0F :  1.0F));
			} else {
				if (UT.Entities.isEnderCreature((LivingEntity)aEntity) || UT.Entities.isWereCreature((LivingEntity)aEntity))
				aEntity.hurt(DamageSources.getSpikeDamage(), TFC_DAMAGE_MULTIPLIER * ((aMeta & 7) < 6 ? 20.0F : 10.0F));
				else if (!(aEntity instanceof IronGolem || aEntity instanceof Skeleton || aEntity instanceof Slime))
				aEntity.hurt(DamageSources.getSpikeDamage(), TFC_DAMAGE_MULTIPLIER * ((aMeta & 7) < 6 ?  2.0F :  1.0F));
			}
		}
	}
	
	@Override
	public boolean canEntityDestroy(BlockGetter aWorld, int aX, int aY, int aZ, Entity aEntity) {
		return WD.meta(aWorld, aX, aY, aZ) < 8 ? !(aEntity instanceof WitherBoss) : !(aEntity instanceof EnderDragon);
	}
}

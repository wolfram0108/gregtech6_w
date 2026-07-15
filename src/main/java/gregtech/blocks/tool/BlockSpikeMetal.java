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

import gregapi.block.misc.BlockBaseSpike;
import gregapi.damage.DamageSources;
import gregapi.data.ANY;
import gregapi.data.LH;
import gregapi.data.MT;
import gregapi.util.UT;
import gregapi.util.WD;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.monster.skeleton.Skeleton;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.List;

import static gregapi.data.CS.TFC_DAMAGE_MULTIPLIER;

public class BlockSpikeMetal extends BlockBaseSpike {
	public BlockSpikeMetal(String aNameInternal) {
		super(aNameInternal, ANY.Cu, MT.Pb);
		LH.add(getUnlocalizedName()+ ".0" , "Copper Wall Spike");
		LH.add(getUnlocalizedName()+ ".1" , "Copper Wall Spike");
		LH.add(getUnlocalizedName()+ ".2" , "Copper Wall Spike");
		LH.add(getUnlocalizedName()+ ".3" , "Copper Wall Spike");
		LH.add(getUnlocalizedName()+ ".4" , "Copper Wall Spike");
		LH.add(getUnlocalizedName()+ ".5" , "Copper Wall Spike");
		LH.add(getUnlocalizedName()+ ".6" , "Copper Block Spike");
		LH.add(getUnlocalizedName()+ ".7" , "Falling Copper Spike Block");
		LH.add(getUnlocalizedName()+ ".8" , "Lead Wall Spike");
		LH.add(getUnlocalizedName()+ ".9" , "Lead Wall Spike");
		LH.add(getUnlocalizedName()+ ".10", "Lead Wall Spike");
		LH.add(getUnlocalizedName()+ ".11", "Lead Wall Spike");
		LH.add(getUnlocalizedName()+ ".12", "Lead Wall Spike");
		LH.add(getUnlocalizedName()+ ".13", "Lead Wall Spike");
		LH.add(getUnlocalizedName()+ ".14", "Lead Block Spike");
		LH.add(getUnlocalizedName()+ ".15", "Falling Lead Spike Block");
	}
	
	@Override
	public void addInformation(ItemStack aStack, byte aMeta, Player aPlayer, List<String> aList, boolean aF3_H) {
		if (aMeta < 8) {
			aList.add(LH.Chat.ORANGE + "Deals huge Damage to any Slime touching it!");
			aList.add(LH.Chat.ORANGE + "Does very low Damage to anything else!");
			aList.add(LH.Chat.ORANGE + "Doesn't work on Skeletons and Iron Golems.");
		} else {
			aList.add(LH.Chat.ORANGE + "Deals huge Damage to any Arthropod touching it!");
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
				if (UT.Entities.isSlimeCreature((LivingEntity)aEntity))
				aEntity.hurt(DamageSources.getSpikeDamage(), TFC_DAMAGE_MULTIPLIER * ((aMeta & 7) < 6 ? 20.0F : 10.0F));
				else if (!(aEntity instanceof IronGolem || aEntity instanceof Skeleton))
				aEntity.hurt(DamageSources.getSpikeDamage(), TFC_DAMAGE_MULTIPLIER * ((aMeta & 7) < 6 ?  2.0F :  1.0F));
			} else {
				if (((LivingEntity)aEntity).getType().is(EntityTypeTags.ARTHROPOD))
				aEntity.hurt(DamageSources.getSpikeDamage(), TFC_DAMAGE_MULTIPLIER * ((aMeta & 7) < 6 ? 20.0F : 10.0F));
				else if (!(aEntity instanceof IronGolem || aEntity instanceof Skeleton || aEntity instanceof Slime))
				aEntity.hurt(DamageSources.getSpikeDamage(), TFC_DAMAGE_MULTIPLIER * ((aMeta & 7) < 6 ?  2.0F :  1.0F));
			}
		}
	}
}

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

package gregtech.items.tools.early;

import gregapi.block.metatype.BlockStones;
import gregapi.data.IL;
import gregapi.data.MT;
import gregapi.data.OP;
import gregapi.item.multiitem.MultiItemTool;
import gregapi.old.Textures;
import gregapi.render.IIconContainer;
import gregapi.util.OM;
import gregapi.util.ST;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.ItemStack;
import net.minecraft.stats.AchievementList;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.event.level.BlockEvent;

import java.util.List;

import static gregapi.data.CS.*;

public class GT_Tool_Club extends GT_Tool_HardHammer {
	@Override public int getToolDamagePerBlockBreak()                                       {return  50;}
	@Override public int getToolDamagePerDropConversion()                                   {return 100;}
	@Override public int getToolDamagePerContainerCraft()                                   {return 800;}
	@Override public int getToolDamagePerEntityAttack()                                     {return  50;}
	@Override public int getHurtResistanceTime(int aOriginalHurtResistance, Entity aEntity) {return (aOriginalHurtResistance * 3) / 2;}
	@Override public float getSpeedMultiplier()                                             {return 0.5F;}
	@Override public String getCraftingSound()                                              {return SFX.MC_DIG_ROCK;}
	@Override public String getBreakingSound()                                              {return SFX.MC_BREAK;}
	@Override public boolean canBlock()                                                     {return T;}
	@Override public boolean isWeapon()                                                     {return T;}
	@Override public boolean isMiningTool()                                                 {return F;}
	
	@Override
	public int convertBlockDrops(List<ItemStack> aDrops, ItemStack aStack, Player aPlayer, Block aBlock, long aAvailableDurability, int aX, int aY, int aZ, byte aMetaData, int aFortune, boolean aSilkTouch, BlockEvent.HarvestDropsEvent aEvent) {
		Block aDrop = aDrops.size() == 1 ? ST.block(aDrops.get(0)) : NB;
		if (aDrop == NB) aDrop = aBlock;
		if (aDrop == Blocks.STONE || aDrop == Blocks.COBBLESTONE || aDrop == Blocks.MOSSY_COBBLESTONE || aDrop == Blocks.STONE_BRICKS || aDrop == Blocks.STONE_BRICK_STAIRS || aDrop == Blocks.COBBLESTONE_WALL || aDrop == Blocks.STONE_BUTTON || aDrop == Blocks.STONE_PRESSURE_PLATE) {
			aDrops.clear();
			aDrops.add(OP.rockGt.mat(MT.Stone, 1+RNGSUS.nextInt(4)));
			return 0;
		}
		if (aDrop == Blocks.NETHER_BRICKS || aDrop == Blocks.NETHER_BRICK_STAIRS || aDrop == Blocks.NETHER_BRICK_FENCE) {
			aDrops.clear();
			aDrops.add(OP.rockGt.mat(MT.NetherBrick, 1+RNGSUS.nextInt(4)));
			return 0;
		}
		if (aDrop == Blocks.NETHERRACK) {
			aDrops.clear();
			aDrops.add(OP.rockGt.mat(MT.Netherrack, 1+RNGSUS.nextInt(4)));
			return 0;
		}
		if (aDrop == Blocks.END_STONE) {
			aDrops.clear();
			aDrops.add(OP.rockGt.mat(MT.Endstone, 1+RNGSUS.nextInt(4)));
			return 0;
		}
		if (aDrop == Blocks.OBSIDIAN || IL.NeLi_Obsidian.equal(aDrop) || IL.NePl_Obsidian.equal(aDrop) || IL.EtFu_Obsidian.equal(aDrop)) {
			aDrops.clear();
			aDrops.add(OP.rockGt.mat(MT.Obsidian, 1+RNGSUS.nextInt(4)));
			return 0;
		}
		if (IL.NeLi_Basalt.equal(aDrop) || IL.NeLi_Basalt_Polished.equal(aDrop) || IL.NePl_Basalt.equal(aDrop) || IL.NePl_Basalt_Polished.equal(aDrop) || IL.GaSu_Basalt.equal(aDrop) || IL.GaSu_Basalt_Smooth.equal(aDrop) || IL.BOTA_Basalt.equal(aDrop) || IL.BOTA_Basalt_Bricks.equal(aDrop) || IL.BOTA_Basalt_Chiseled.equal(aDrop) || IL.BOTA_Basalt_Smooth.equal(aDrop)) {
			aDrops.clear();
			aDrops.add(OP.rockGt.mat(MT.STONES.Basalt, 1+RNGSUS.nextInt(4)));
			return 0;
		}
		if (IL.NeLi_Blackstone.equal(aDrop) || IL.NeLi_Blackstone_Bricks.equal(aDrop) || IL.NeLi_Blackstone_Chiseled.equal(aDrop) || IL.NeLi_Blackstone_Cracked.equal(aDrop) || IL.NeLi_Blackstone_Polished.equal(aDrop) || IL.NePl_Blackstone.equal(aDrop) || IL.NePl_Blackstone_Bricks.equal(aDrop) || IL.NePl_Blackstone_Chiseled.equal(aDrop) || IL.NePl_Blackstone_Cracked.equal(aDrop) || IL.NePl_Blackstone_Polished.equal(aDrop)) {
			aDrops.clear();
			aDrops.add(OP.rockGt.mat(MT.STONES.Blackstone, 1+RNGSUS.nextInt(4)));
			return 0;
		}
		if (aBlock instanceof BlockStones && BlockStones.JUSTSTONE[aMetaData]) {
			aDrops.clear();
			aDrops.add(OP.rockGt.mat(((BlockStones)aBlock).mMaterial, 1+RNGSUS.nextInt(((BlockStones)aBlock).mOctantcount/2)));
			return 0;
		}
		if (aBlock == Blocks.REDSTONE_ORE || aBlock == Blocks.REDSTONE_ORE || aBlock == BlocksGT.EtFu_Deepslate_Redstone_Ore  || aBlock == BlocksGT.EtFu_Deepslate_Lit_Redstone_Ore || OM.is("oreRedstone", ST.make(aBlock, 1, aMetaData))) {
			aDrops.clear();
			aDrops.add(OP.gemChipped.mat(MT.OREMATS.Cinnabar, 1+RNGSUS.nextInt(4)));
			return 0;
		}
		return super.convertBlockDrops(aDrops, aStack, aPlayer, aBlock, aAvailableDurability, aX, aY, aZ, aMetaData, aFortune, aSilkTouch, aEvent);
	}
	
	@Override
	public IIconContainer getIcon(boolean aIsToolHead, ItemStack aStack) {
		return !aIsToolHead ? Textures.ItemIcons.CLUB : Textures.ItemIcons.VOID;
	}
	
	@Override
	public short[] getRGBa(boolean aIsToolHead, ItemStack aStack) {
		return !aIsToolHead ? MultiItemTool.getPrimaryMaterial(aStack, MT.Steel).mRGBaSolid : UNCOLOURED;
	}
	
	@Override
	public void onToolCrafted(ItemStack aStack, Player aPlayer) {
		super.onToolCrafted(aStack, aPlayer);
		aPlayer.triggerAchievement(AchievementList.buildSword);
	}
	
	@Override
	public void onStatsAddedToTool(MultiItemTool aItem, int aID) {
		// No Behaviors
	}
	
	@Override
	public String getDeathMessage() {
		return "[VICTIM] got welcomed into the club by [KILLER]";
	}
	
	@Override
	public Component getDeathMessage(LivingEntity aPlayer, LivingEntity aEntity, String aNamePlayer, String aNameEntity) {
		return aNamePlayer.equalsIgnoreCase("Bear989Sr") ? new Component(ChatFormatting.RED+aNameEntity+ChatFormatting.WHITE+" got clubbed by a Bear!") : super.getDeathMessage(aPlayer, aEntity, aNamePlayer, aNameEntity);
	}
}

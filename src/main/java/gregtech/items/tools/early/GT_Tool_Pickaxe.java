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
 *
 * Modified in 2026 for the GregTech 6 NeoForge port
 * (https://github.com/wolfram0108/gregtech6_w): ported from Minecraft 1.7.10 / Forge
 * to Minecraft 26.1.2 / NeoForge.
 */

package gregtech.items.tools.early;
import gregapi.util.ST;

import gregapi.util.WD;
import gregapi.data.MT;
import gregapi.data.OP;
import gregapi.item.multiitem.MultiItemTool;
import gregapi.item.multiitem.behaviors.Behavior_Place_Torch;
import gregapi.item.multiitem.behaviors.Behavior_Plug_Leak;
import gregapi.item.multiitem.tools.ToolStats;
import gregapi.render.IIconContainer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.InfestedBlock;
import gregapi.block.Material;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.ItemStack;
import gt6mirror.minecraftforge.common.AchievementList;

import static gregapi.data.CS.T;
import static gregapi.data.CS.TOOL_pickaxe;

public class GT_Tool_Pickaxe extends ToolStats {
	@Override public int getToolDamagePerBlockBreak()                                       {return  25;}
	@Override public int getToolDamagePerEntityAttack()                                     {return 200;}
	@Override public float getBaseDamage()                                                  {return 3.0F;}
	@Override public boolean canPenetrate()                                                 {return T;}
	@Override public boolean isMiningTool()                                                 {return T;}
	
	@Override
	public int getHurtResistanceTime(int aOriginalHurtResistance, Entity aEntity) {
		return aOriginalHurtResistance * 2;
	}
	
	@Override
	public boolean isMinableBlock(Block aBlock, byte aMetaData) {
		return TOOL_pickaxe.equalsIgnoreCase(WD.harvestTool(aBlock, aMetaData)) || aBlock instanceof InfestedBlock || WD.getMaterial(aBlock) == Material.rock || WD.getMaterial(aBlock) == Material.iron || WD.getMaterial(aBlock) == Material.anvil || WD.getMaterial(aBlock) == Material.glass || WD.getMaterial(aBlock) == Material.packedIce || WD.getMaterial(aBlock) == Material.ice || aBlock == Blocks.FLOWER_POT;
	}
	
	@Override
	public IIconContainer getIcon(boolean aIsToolHead, ItemStack aStack) {
		return aIsToolHead ? MultiItemTool.getPrimaryMaterial(aStack, MT.Steel).mTextureSetsItems.get(OP.toolHeadPickaxe.mIconIndexItem) : MultiItemTool.getSecondaryMaterial(aStack, MT.WOODS.Spruce).mTextureSetsItems.get(OP.stick.mIconIndexItem);
	}
	
	@Override
	public short[] getRGBa(boolean aIsToolHead, ItemStack aStack) {
		return aIsToolHead ? MultiItemTool.getPrimaryMaterial(aStack, MT.Steel).mRGBaSolid : MultiItemTool.getSecondaryMaterial(aStack, MT.WOODS.Spruce).mRGBaSolid;
	}
	
	@Override
	public void onStatsAddedToTool(MultiItemTool aItem, int aID) {
		aItem.addItemBehavior(aID, Behavior_Plug_Leak.INSTANCE);
		aItem.addItemBehavior(aID, Behavior_Place_Torch.INSTANCE);
	}
	
	@Override
	public void onToolCrafted(ItemStack aStack, Player aPlayer) {
		super.onToolCrafted(aStack, aPlayer);
		ST.achieve(aPlayer, AchievementList.buildPickaxe);
		ST.achieve(aPlayer, AchievementList.buildBetterPickaxe);
	}
	
	@Override
	public String getDeathMessage() {
		return "[VICTIM] got mined by [KILLER]";
	}
}

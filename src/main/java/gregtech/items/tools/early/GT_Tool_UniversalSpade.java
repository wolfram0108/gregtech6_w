/**
 * Copyright (c) 2025 GregTech-6 Team
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

import gregapi.util.WD;
import gregapi.block.MaterialAdventure;
import gregapi.data.MT;
import gregapi.data.OP;
import gregapi.data.RM;
import gregapi.item.multiitem.MultiItemTool;
import gregapi.item.multiitem.behaviors.*;
import gregapi.item.multiitem.tools.ToolStats;
import gregapi.recipes.Recipe;
import gregapi.render.IIconContainer;
import gregapi.util.ST;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BaseRailBlock;
import gregapi.block.Material;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import gt6mirror.minecraftforge.common.AchievementList;
import net.minecraftforge.event.level.BlockEvent;

import java.util.List;

import static gregapi.data.CS.*;

public class GT_Tool_UniversalSpade extends ToolStats {
	@Override
	public int getToolDamagePerBlockBreak() {
		return 100;
	}
	
	@Override
	public int getToolDamagePerDropConversion() {
		return 100;
	}
	
	@Override
	public int getToolDamagePerContainerCraft() {
		return 400;
	}
	
	@Override
	public int getToolDamagePerEntityAttack() {
		return 100;
	}
	
	@Override
	public int getBaseQuality() {
		return 0;
	}
	
	@Override
	public float getBaseDamage() {
		return 3.0F;
	}
	
	@Override
	public float getSpeedMultiplier() {
		return 0.75F;
	}
	
	@Override
	public float getMaxDurabilityMultiplier() {
		return 1.0F;
	}
	
	@Override public boolean canCollect()                                                   {return T;}
	@Override public boolean canBlock()                                                     {return T;}
	@Override public boolean isCrowbar()                                                    {return T;}
	@Override public boolean isWeapon()                                                     {return T;}
	
	@Override
	public boolean isMinableBlock(Block aBlock, byte aMetaData) {
		if (aBlock instanceof BaseRailBlock || BlocksGT.openableCrowbar.contains(aBlock)) return T;
		String tTool = WD.harvestTool(aBlock, aMetaData);
		return (tTool != null && (tTool.equalsIgnoreCase(TOOL_shovel) || tTool.equalsIgnoreCase(TOOL_axe) || tTool.equalsIgnoreCase(TOOL_saw) || tTool.equalsIgnoreCase(TOOL_sword) || tTool.equalsIgnoreCase(TOOL_crowbar))) || WD.getMaterial(aBlock) == Material.sand || WD.getMaterial(aBlock) == Material.grass || WD.getMaterial(aBlock) == Material.ground || WD.getMaterial(aBlock) == Material.snow || WD.getMaterial(aBlock) == Material.craftedSnow || WD.getMaterial(aBlock) == Material.clay  || WD.getMaterial(aBlock) == Material.leaves || WD.getMaterial(aBlock) == Material.vine || WD.getMaterial(aBlock) == Material.wood || WD.getMaterial(aBlock) == MaterialAdventure.WOOD || WD.getMaterial(aBlock) == Material.cactus || WD.getMaterial(aBlock) == Material.circuits || WD.getMaterial(aBlock) == Material.gourd || WD.getMaterial(aBlock) == Material.web || WD.getMaterial(aBlock) == Material.cloth || WD.getMaterial(aBlock) == Material.carpet || WD.getMaterial(aBlock) == Material.plants || WD.getMaterial(aBlock) == Material.cake || WD.getMaterial(aBlock) == Material.tnt || WD.getMaterial(aBlock) == Material.fire || WD.getMaterial(aBlock) == Material.sponge;
	}
	
	@Override
	public int convertBlockDrops(List<ItemStack> aDrops, ItemStack aStack, Player aPlayer, Block aBlock, long aAvailableDurability, int aX, int aY, int aZ, byte aMetaData, int aFortune, boolean aSilkTouch) {
		if (BlocksGT.openableCrowbar.contains(aBlock)) {
			List<ItemStack> tDrops = ST.arraylist();
			for (int i = 0; i < aDrops.size(); i++) {
				Recipe tRecipe = RM.Unboxinator.findRecipe(null, null, T, Integer.MAX_VALUE, NI, ZL_FS, ST.amount(1, aDrops.get(i)));
				if (tRecipe != null) {
					int tStackSize = aDrops.get(i).getCount();
					aDrops.remove(i--);
					if (tRecipe.mOutputs.length > 0) for (int j = 0; j < tStackSize; j++) {
						ItemStack[] tOutput = tRecipe.getOutputs();
						for (int k = 0; k < tOutput.length; k++) tDrops.add(tOutput[k]);
					}
				}
			}
			aDrops.addAll(tDrops);
		}
		return 0;
	}
	
	@Override
	public IIconContainer getIcon(boolean aIsToolHead, ItemStack aStack) {
		return aIsToolHead ? MultiItemTool.getPrimaryMaterial(aStack, MT.Steel).mTextureSetsItems.get(OP.toolHeadUniversalSpade.mIconIndexItem) : MultiItemTool.getSecondaryMaterial(aStack, MT.Steel).mTextureSetsItems.get(OP.stick.mIconIndexItem);
	}
	
	@Override
	public short[] getRGBa(boolean aIsToolHead, ItemStack aStack) {
		return aIsToolHead ? MultiItemTool.getPrimaryMaterial(aStack, MT.Steel).mRGBaSolid : MultiItemTool.getSecondaryMaterial(aStack, MT.Steel).mRGBaSolid;
	}
	
	@Override
	public void onStatsAddedToTool(MultiItemTool aItem, int aID) {
		aItem.addItemBehavior(aID, Behavior_Plug_Leak.INSTANCE);
		aItem.addItemBehavior(aID, new Behavior_Place_Path(50));
		aItem.addItemBehavior(aID, new Behavior_Place_Paddy(50));
		aItem.addItemBehavior(aID, new Behavior_Tool(TOOL_crowbar, SFX.MC_BREAK, 200, F, SFX.RANDOM_PITCH));
		aItem.addItemBehavior(aID, new Behavior_Tool(TOOL_shovel, SFX.MC_DIG_GRAVEL, 100, F, SFX.RANDOM_PITCH));
		aItem.addItemBehavior(aID, Behavior_Place_Torch.INSTANCE);
	}
	
	@Override
	public void onToolCrafted(ItemStack aStack, Player aPlayer) {
		super.onToolCrafted(aStack, aPlayer);
		ST.achieve(aPlayer, AchievementList.buildSword);
	}
	
	@Override
	public String getDeathMessage() {
		return "[VICTIM] has been digged by [KILLER]";
	}
}

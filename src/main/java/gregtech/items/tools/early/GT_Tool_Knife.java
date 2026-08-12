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
import gregapi.util.ST;

import gregapi.data.CS.SFX;
import gregapi.item.multiitem.MultiItemTool;
import gregapi.item.multiitem.behaviors.Behavior_Tool;
import gregapi.old.Textures;
import gregapi.render.IIconContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import gt6mirror.minecraftforge.common.AchievementList;

import static gregapi.data.CS.T;
import static gregapi.data.CS.TOOL_knife;

public class GT_Tool_Knife extends GT_Tool_Sword {
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
		return 100;
	}
	
	@Override
	public int getToolDamagePerEntityAttack() {
		return 200;
	}
	
	@Override
	public float getBaseDamage() {
		return 2.0F;
	}
	
	@Override
	public float getSpeedMultiplier() {
		return 0.5F;
	}
	
	@Override
	public float getMaxDurabilityMultiplier() {
		return 1.0F;
	}
	
	@Override public boolean canCollect()                                                   {return T;}
	
	@Override
	public IIconContainer getIcon(boolean aIsToolHead, ItemStack aStack) {
		return !aIsToolHead ? Textures.ItemIcons.KNIFE : Textures.ItemIcons.VOID;
	}
	
	@Override
	public String getDeathMessage() {
		return "<[VICTIM]> [KILLER] what are you doing?, [KILLER]?!? STAHP!!!";
	}
	
	@Override
	public void onStatsAddedToTool(MultiItemTool aItem, int aID) {
		aItem.addItemBehavior(aID, new Behavior_Tool(TOOL_knife, SFX.MC_DIG_CLOTH, getToolDamagePerContainerCraft(), !canBlock(), SFX.RANDOM_PITCH));
	}
	
	@Override
	public void onToolCrafted(ItemStack aStack, Player aPlayer) {
		super.onToolCrafted(aStack, aPlayer);
		ST.achieve(aPlayer, AchievementList.buildSword);
	}
}

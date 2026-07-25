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
 */

package gregtech.items.tools.electric;

import gregapi.util.WD;
import gregapi.block.MaterialAdventure;
import gregapi.data.MD;
import gregapi.data.MT;
import gregapi.data.OP;
import gregapi.item.multiitem.MultiItemTool;
import gregapi.item.multiitem.behaviors.Behavior_Place_Sapling;
import gregapi.item.multiitem.behaviors.Behavior_Place_Workbench;
import gregapi.item.multiitem.behaviors.Behavior_Tool;
import gregapi.old.Textures;
import gregapi.render.IIconContainer;
import gregapi.util.ST;
import gregtech.items.tools.early.GT_Tool_Axe;
import net.minecraft.world.level.block.Block;
import gregapi.block.Material;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.AchievementList;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.IShearable;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;

import java.util.ArrayList;
import java.util.List;

import static gregapi.data.CS.*;

public class GT_Tool_Chainsaw_LV extends GT_Tool_Axe {
	@Override public int getBaseQuality                () {return 1;}
	@Override public int getToolDamagePerContainerCraft() {return 200;}
	@Override public int getToolDamagePerEntityAttack  () {return super.getToolDamagePerEntityAttack() * 4;}
	@Override public float getSpeedMultiplier          () {return 2.0F;}
	@Override public String getCraftingSound           () {return SFX.IC_CHAINSAW_01;}
	@Override public String getEntityHitSound          () {return SFX.IC_CHAINSAW_02;}
	@Override public String getMiningSound             () {return SFX.IC_CHAINSAW_01;}
	
	@Override
	public int getHurtResistanceTime(int aOriginalHurtResistance, Entity aEntity) {
		if (aEntity instanceof Creeper) return aOriginalHurtResistance / 3;
		return aOriginalHurtResistance;
	}
	
	@Override
	public DamageSource getDamageSource(LivingEntity aPlayer, Entity aEntity) {
		if (MD.IC2.mLoaded && aPlayer instanceof Player && aEntity instanceof Creeper) try {
		ST.achieve(aPlayer, AchievementList.acquireIron);
		ic2.core.IC2.achievements.issueAchievement((Player)aPlayer, "buildCable");
		ic2.core.IC2.achievements.issueAchievement((Player)aPlayer, "buildGenerator");
		ic2.core.IC2.achievements.issueAchievement((Player)aPlayer, "buildBatBox");
		ic2.core.IC2.achievements.issueAchievement((Player)aPlayer, "buildChainsaw");
		ic2.core.IC2.achievements.issueAchievement((Player)aPlayer, "killCreeperChainsaw");
		} catch(Throwable e) {e.printStackTrace(ERR);}
		return super.getDamageSource(aPlayer, aEntity);
	}
	
	@Override
	public boolean isMinableBlock(Block aBlock, byte aMetaData) {
		String tTool = WD.harvestTool(aBlock, aMetaData);
		return (tTool != null && (tTool.equalsIgnoreCase(TOOL_axe) || tTool.equalsIgnoreCase(TOOL_saw))) || WD.getMaterial(aBlock) == Material.wood || WD.getMaterial(aBlock) == MaterialAdventure.WOOD || WD.getMaterial(aBlock) == Material.cactus || WD.getMaterial(aBlock) == Material.leaves || WD.getMaterial(aBlock) == Material.vine || WD.getMaterial(aBlock) == Material.plants || WD.getMaterial(aBlock) == Material.gourd || WD.getMaterial(aBlock) == Material.ice || WD.getMaterial(aBlock) == Material.packedIce || WD.getMaterial(aBlock) == Material.coral;
	}
	
	@Override
	public int convertBlockDrops(List<ItemStack> aDrops, ItemStack aStack, Player aPlayer, Block aBlock, long aAvailableDurability, int aX, int aY, int aZ, byte aMetaData, int aFortune, boolean aSilkTouch, BlockDropsEvent aEvent) {
		if (WD.getMaterial(aBlock) == Material.leaves && aBlock instanceof IShearable) {
			WD.set(aPlayer.level(), aX, aY, aZ, aBlock, aMetaData, 0);
			if (((IShearable)aBlock).isShearable(aStack, aPlayer.level(), aX, aY, aZ)) {
				ArrayList<ItemStack> tDrops = ((IShearable)aBlock).onSheared(aStack, aPlayer.level(), aX, aY, aZ, aFortune);
				aDrops.clear();
				aDrops.addAll(tDrops);
				/*neo: BlockDropsEvent getDrops() падают всегда; dropChance убран*/;
			}
			WD.set(aPlayer.level(), aX, aY, aZ, NB, 0, 0);
			return 0;
		}
		if ((WD.getMaterial(aBlock) == Material.ice || WD.getMaterial(aBlock) == Material.packedIce) && aDrops.isEmpty()) {
			aDrops.add(ST.make(aBlock, 1, aMetaData));
			WD.set(aPlayer.level(), aX, aY, aZ, NB, 0, 3);
			/*neo: BlockDropsEvent getDrops() падают всегда; dropChance убран*/;
			return 0;
		}
		return super.convertBlockDrops(aDrops, aStack, aPlayer, aBlock, aAvailableDurability, aX, aY, aZ, aMetaData, aFortune, aSilkTouch, aEvent);
	}
	
	@Override
	public float getMiningSpeed(Block aBlock, byte aMetaData, float aDefault, Player aPlayer, Level aWorld, int aX, int aY, int aZ) {
		return WD.getMaterial(aBlock) == Material.leaves || WD.getMaterial(aBlock) == Material.vine || WD.getMaterial(aBlock) == Material.plants || WD.getMaterial(aBlock) == Material.gourd ? aDefault : super.getMiningSpeed(aBlock, aMetaData, aDefault, aPlayer, aWorld, aX, aY, aZ);
	}
	
	@Override
	public IIconContainer getIcon(boolean aIsToolHead, ItemStack aStack) {
		return aIsToolHead ? Textures.ItemIcons.POWER_UNIT_LV : ST.meta(aStack) % 2 != 0 ? Textures.ItemIcons.VOID : MultiItemTool.getPrimaryMaterial(aStack, MT.Steel).mTextureSetsItems.get(OP.toolHeadChainsaw.mIconIndexItem);
	}
	
	@Override
	public short[] getRGBa(boolean aIsToolHead, ItemStack aStack) {
		return aIsToolHead ? MultiItemTool.getSecondaryMaterial(aStack, MT.StainlessSteel).mRGBaSolid : MultiItemTool.getPrimaryMaterial(aStack, MT.Steel).mRGBaSolid;
	}
	
	@Override
	public void onToolCrafted(ItemStack aStack, Player aPlayer) {
		super.onToolCrafted(aStack, aPlayer);
		if (MD.IC2.mLoaded) try {
		ST.achieve(aPlayer, AchievementList.buildPickaxe);
		ST.achieve(aPlayer, AchievementList.buildFurnace);
		ST.achieve(aPlayer, AchievementList.acquireIron);
		ic2.core.IC2.achievements.issueAchievement(aPlayer, "buildCable");
		ic2.core.IC2.achievements.issueAchievement(aPlayer, "buildGenerator");
		ic2.core.IC2.achievements.issueAchievement(aPlayer, "buildBatBox");
		ic2.core.IC2.achievements.issueAchievement(aPlayer, "buildChainsaw");
		} catch(Throwable e) {e.printStackTrace(ERR);}
	}
	
	@Override
	public void onStatsAddedToTool(MultiItemTool aItem, int aID) {
		aItem.addItemBehavior(aID, new Behavior_Tool(TOOL_saw, SFX.MC_DIG_WOOD, getToolDamagePerContainerCraft(), F, SFX.RANDOM_PITCH));
		aItem.addItemBehavior(aID, Behavior_Place_Sapling.INSTANCE);
		aItem.addItemBehavior(aID, Behavior_Place_Workbench.INSTANCE);
	}
	
	@Override
	public String getDeathMessage() {
		return "[VICTIM] was massacred by [KILLER]";
	}
}

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

package gregtech.items.tools.early;

import gregapi.block.MaterialAdventure;
import gregapi.block.tree.BlockBaseBeam;
import gregapi.data.MD;
import gregapi.data.MT;
import gregapi.data.OP;
import gregapi.item.multiitem.MultiItemTool;
import gregapi.item.multiitem.behaviors.Behavior_Place_Sapling;
import gregapi.item.multiitem.behaviors.Behavior_Place_Workbench;
import gregapi.item.multiitem.behaviors.Behavior_Tool;
import gregapi.item.multiitem.tools.ToolStats;
import gregapi.render.IIconContainer;
import gregapi.util.ST;
import gregapi.util.UT;
import gregapi.util.WD;
import gregapi.wooddict.WoodDictionary;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HugeMushroomBlock;
import gregapi.block.Material;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.level.BlockEvent;

import java.util.List;

import static gregapi.data.CS.*;

public class GT_Tool_Axe extends ToolStats {
	@Override
	public int getToolDamagePerBlockBreak() {
		return 50;
	}
	
	@Override
	public int getToolDamagePerEntityAttack() {
		return 200;
	}
	
	@Override
	public int getToolDamagePerContainerCraft() {
		return 100;
	}
	
	@Override
	public int getToolDamagePerDropConversion() {
		return 1;
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
		return 1.0F;
	}
	
	@Override
	public float getMaxDurabilityMultiplier() {
		return 1.0F;
	}
	
	@Override
	public boolean isWeapon() {
		return T;
	}
	
	@Override
	public boolean isMinableBlock(Block aBlock, byte aMeta) {
		return TOOL_axe.equalsIgnoreCase(WD.harvestTool(aBlock, aMeta)) || aBlock instanceof HugeMushroomBlock || WD.getMaterial(aBlock) == Material.wood || WD.getMaterial(aBlock) == MaterialAdventure.WOOD || WD.getMaterial(aBlock) == Material.cactus || WD.getMaterial(aBlock) == Material.leaves || WD.getMaterial(aBlock) == Material.vine || WD.getMaterial(aBlock) == Material.plants || WD.getMaterial(aBlock) == Material.gourd || WD.getMaterial(aBlock) == Material.coral;
	}
	
	private static boolean LOCK = T;
	
	@Override
	public int convertBlockDrops(List<ItemStack> aDrops, ItemStack aStack, Player aPlayer, Block aBlock, long aAvailableDurability, int aX, int aY, int aZ, byte aMeta, int aFortune, boolean aSilkTouch, BlockEvent.HarvestDropsEvent aEvent) {
		int rAmount = 0;
		if (LOCK && !MD.TreeCap.mLoaded && !aPlayer.level().isClientSide() && !aPlayer.isShiftKeyDown() && !aBlock.getClass().getName().startsWith("com.ferreusveritas.dynamictrees") && (aBlock instanceof HugeMushroomBlock || WD.wood(aBlock, aPlayer.level(), aX, aY, aZ) || OP.log.contains(ST.make(aBlock, 1, aMeta)) || WoodDictionary.WOODS.containsKey(aBlock, aMeta, T))) {
			LOCK = F;
			try {
				int tY = aY, tH = aPlayer.level().getHeight(), tCount = 0, tIncrement = UT.Code.roundUp(WD.hardness(aBlock, aPlayer.level(), aX, aY, aZ) * getToolDamagePerBlockBreak());
				// Checking...
				while (++tY < tH) {
					if (WD.block(aPlayer.level(), aX, tY, aZ) != aBlock) break;
					if (rAmount >= aAvailableDurability) continue;
					rAmount+= ++tIncrement;
					tCount++;
				}
				// Harvesting...
				while (--tY > aY && tCount-->0 && aPlayer.level().func_147480_a(aX, tY, aZ, T)) {
					if (FAST_LEAF_DECAY) WD.leafdecay(aPlayer.level(), aX, tY, aZ, null, T, T);
				}
				if (FAST_LEAF_DECAY) WD.leafdecay(aPlayer.level(), aX, aY, aZ, null, T, T);
			} catch(Throwable e) {e.printStackTrace(ERR);}
			LOCK = T;
		}
		harvestStick(aDrops, aStack, aPlayer, aBlock, aAvailableDurability, aX, aY, aZ, aMeta, aFortune, aSilkTouch, aEvent);
		return rAmount;
	}
	
	@Override
	public float getMiningSpeed(Block aBlock, byte aMeta, float aDefault, Player aPlayer, Level aWorld, int aX, int aY, int aZ) {
		if (aBlock instanceof BlockBaseBeam) return 2.0F * aDefault;
		if (aBlock.getClass().getName().startsWith("com.ferreusveritas.dynamictrees")) return aDefault;
		if (aBlock instanceof HugeMushroomBlock || WD.wood(aBlock, aPlayer.level(), aX, aY, aZ) || OP.log.contains(ST.make(aBlock, 1, aMeta)) || WoodDictionary.WOODS.containsKey(aBlock, aMeta, T)) {
			float rAmount = 1.0F, tIncrement = 1.0F;
			if (!aPlayer.isShiftKeyDown() && !MD.TreeCap.mLoaded) for (int tY = aY+1, tH = aPlayer.level().getHeight(); tY < tH; tY++) if (WD.block(aPlayer.level(), aX, tY, aZ) == aBlock) {tIncrement+=0.1F; rAmount+=tIncrement;} else break;
			if (rAmount > 2.0F && (aBlock instanceof HugeMushroomBlock || MD.NeLi.owns(aBlock))) return aDefault / (4.0F * rAmount);
			return 2.0F * aDefault / rAmount;
		}
		return WD.getMaterial(aBlock) == Material.leaves || WD.getMaterial(aBlock) == Material.vine || WD.getMaterial(aBlock) == Material.plants || WD.getMaterial(aBlock) == Material.gourd ? aDefault / 4.0F : aDefault;
	}
	
	@Override
	public void afterDealingDamage(float aNormalDamage, float aMagicDamage, int aFireAspect, boolean aCriticalHit, Entity aEntity, ItemStack aStack, Player aPlayer) {
		super.afterDealingDamage(aNormalDamage, aMagicDamage, aFireAspect, aCriticalHit, aEntity, aStack, aPlayer);
		if (aEntity.level().isClientSide() || aNormalDamage < 2) return;
		if ("EntityEnt".equalsIgnoreCase(UT.Reflection.getLowercaseClass(aEntity))) ST.drop(aEntity, Blocks.OAK_LOG, UT.Code.bindStack((int)(aNormalDamage / 2)), 0);
	}
	
	@Override
	public IIconContainer getIcon(boolean aIsToolHead, ItemStack aStack) {
		return aIsToolHead ? MultiItemTool.getPrimaryMaterial(aStack, MT.Steel).mTextureSetsItems.get(OP.toolHeadAxe.mIconIndexItem) : MultiItemTool.getSecondaryMaterial(aStack, MT.WOODS.Spruce).mTextureSetsItems.get(OP.stick.mIconIndexItem);
	}
	
	@Override
	public short[] getRGBa(boolean aIsToolHead, ItemStack aStack) {
		return aIsToolHead ? MultiItemTool.getPrimaryMaterial(aStack, MT.Steel).mRGBaSolid : MultiItemTool.getSecondaryMaterial(aStack, MT.WOODS.Spruce).mRGBaSolid;
	}
	
	@Override
	public void onStatsAddedToTool(MultiItemTool aItem, int aID) {
		aItem.addItemBehavior(aID, new Behavior_Tool(TOOL_axe, SFX.MC_DIG_WOOD, getToolDamagePerContainerCraft(), F, SFX.RANDOM_PITCH));
		aItem.addItemBehavior(aID, Behavior_Place_Sapling.INSTANCE);
		aItem.addItemBehavior(aID, Behavior_Place_Workbench.INSTANCE);
	}
	
	@Override
	public String getDeathMessage() {
		return "[VICTIM] has been chopped by [KILLER]";
	}
}

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
import gregapi.block.misc.BlockBaseBars;
import gregapi.data.IL;
import gregapi.data.MD;
import gregapi.data.MT;
import gregapi.data.OP;
import gregapi.item.multiitem.MultiItemTool;
import gregapi.item.multiitem.behaviors.Behavior_Place_Sapling;
import gregapi.item.multiitem.behaviors.Behavior_Place_Workbench;
import gregapi.item.multiitem.behaviors.Behavior_Tool;
import gregapi.item.multiitem.tools.ToolStats;
import gregapi.old.Textures;
import gregapi.render.IIconContainer;
import gregapi.util.ST;
import gregapi.util.UT;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.IronBarsBlock;
import gregapi.block.Material;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.IShearable;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;

import java.util.ArrayList;
import java.util.List;

import static gregapi.data.CS.*;

public class GT_Tool_Saw extends ToolStats {
	@Override
	public int getToolDamagePerBlockBreak() {
		return 50;
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
	public int getBaseQuality() {
		return 0;
	}
	
	@Override
	public float getBaseDamage() {
		return 1.75F;
	}
	
	@Override
	public float getSpeedMultiplier() {
		return 1.0F;
	}
	
	@Override
	public float getMaxDurabilityMultiplier() {
		return 1.0F;
	}
	
	@Override public boolean canCollect()                                                   {return T;}
	
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
		if (IL.AETHER_Skyroot_Leaves_Gold.equal(aBlock)) {
			aDrops.clear();
			aDrops.add(IL.AETHER_Skyroot_Leaves_Gold.get(1));
			WD.set(aPlayer.level(), aX, aY, aZ, NB, 0, 3);
			/*neo: BlockDropsEvent getDrops() падают всегда; dropChance убран*/;
			return 0;
		}
		if (IL.AETHER_Skyroot_Leaves_Green.equal(aBlock)) {
			aDrops.clear();
			aDrops.add(IL.AETHER_Skyroot_Leaves_Green.get(1));
			WD.set(aPlayer.level(), aX, aY, aZ, NB, 0, 3);
			/*neo: BlockDropsEvent getDrops() падают всегда; dropChance убран*/;
			return 0;
		}
		if (IL.AETHER_Skyroot_Leaves_Blue.equal(aBlock)) {
			aDrops.clear();
			aDrops.add(IL.AETHER_Skyroot_Leaves_Blue.get(1));
			WD.set(aPlayer.level(), aX, aY, aZ, NB, 0, 3);
			/*neo: BlockDropsEvent getDrops() падают всегда; dropChance убран*/;
			return 0;
		}
		if (IL.AETHER_Skyroot_Leaves_Dark.equal(aBlock)) {
			aDrops.clear();
			aDrops.add(IL.AETHER_Skyroot_Leaves_Dark.get(1));
			WD.set(aPlayer.level(), aX, aY, aZ, NB, 0, 3);
			/*neo: BlockDropsEvent getDrops() падают всегда; dropChance убран*/;
			return 0;
		}
		if (IL.AETHER_Skyroot_Leaves_Purple.equal(aBlock)) {
			aDrops.clear();
			aDrops.add(IL.AETHER_Skyroot_Leaves_Purple.get(1));
			WD.set(aPlayer.level(), aX, aY, aZ, NB, 0, 3);
			/*neo: BlockDropsEvent getDrops() падают всегда; dropChance убран*/;
			return 0;
		}
		if (IL.AETHER_Skyroot_Leaves_Apple.equal(aBlock)) {
			aDrops.clear();
			aDrops.add(IL.AETHER_Skyroot_Leaves_Apple.get(1));
			WD.set(aPlayer.level(), aX, aY, aZ, NB, 0, 3);
			/*neo: BlockDropsEvent getDrops() падают всегда; dropChance убран*/;
			return 0;
		}
		if (aBlock == Blocks.BOOKSHELF) {
			aDrops.clear();
			aDrops.add(ST.make(Blocks.BOOKSHELF, 1, 0));
			/*neo: BlockDropsEvent getDrops() падают всегда; dropChance убран*/;
			return 0;
		}
		if ((WD.getMaterial(aBlock) == Material.ice || WD.getMaterial(aBlock) == Material.packedIce) && aDrops.isEmpty()) {
			aDrops.add(ST.make(aBlock, 1, aMetaData));
			/*neo: BlockDropsEvent getDrops() падают всегда; dropChance убран*/;
			return 0;
		}
		return 0;
	}
	
	@Override
	public boolean isMinableBlock(Block aBlock, byte aMetaData) {
		String tTool = WD.harvestTool(aBlock, aMetaData);
		return (tTool != null && (tTool.equalsIgnoreCase(TOOL_axe) || tTool.equalsIgnoreCase(TOOL_saw))) || aBlock instanceof BlockBaseBars || (aBlock instanceof IronBarsBlock && WD.getMaterial(aBlock) == Material.iron) || WD.getMaterial(aBlock) == Material.leaves || WD.getMaterial(aBlock) == Material.vine || WD.getMaterial(aBlock) == Material.plants || WD.getMaterial(aBlock) == Material.gourd || WD.getMaterial(aBlock) == Material.wood || WD.getMaterial(aBlock) == MaterialAdventure.WOOD || WD.getMaterial(aBlock) == Material.cactus || WD.getMaterial(aBlock) == Material.ice || WD.getMaterial(aBlock) == Material.packedIce || WD.getMaterial(aBlock) == Material.coral || MD.CARP.owns(aBlock);
	}
	
	@Override
	public float getMiningSpeed(Block aBlock, byte aMetaData, float aDefault, Player aPlayer, Level aWorld, int aX, int aY, int aZ) {
		if (WD.wood(aBlock, aPlayer.level(), aX, aY, aZ) || OP.log.contains(ST.make(aBlock, 1, aMetaData))) return aDefault / 2;
		if (WD.getMaterial(aBlock) == Material.wood || OP.plank.contains(ST.make(aBlock, 1, aMetaData))) return aDefault * 2;
		return WD.getMaterial(aBlock) == Material.vine || WD.getMaterial(aBlock) == Material.plants || WD.getMaterial(aBlock) == Material.gourd ? aDefault / 4 : aDefault;
	}
	
	@Override
	public void afterDealingDamage(float aNormalDamage, float aMagicDamage, int aFireAspect, boolean aCriticalHit, Entity aEntity, ItemStack aStack, Player aPlayer) {
		super.afterDealingDamage(aNormalDamage, aMagicDamage, aFireAspect, aCriticalHit, aEntity, aStack, aPlayer);
		if (aEntity.level().isClientSide() || aNormalDamage < 3) return;
		if ("EntityEnt".equalsIgnoreCase(UT.Reflection.getLowercaseClass(aEntity))) ST.drop(aEntity, Blocks.OAK_LOG, UT.Code.bindStack((int)(aNormalDamage / 3)), 0);
	}
	
	@Override
	public IIconContainer getIcon(boolean aIsToolHead, ItemStack aStack) {
		return aIsToolHead ? MultiItemTool.getPrimaryMaterial(aStack, MT.Steel).mTextureSetsItems.get(OP.toolHeadSaw.mIconIndexItem) : Textures.ItemIcons.HANDLE_SAW;
	}
	
	@Override
	public short[] getRGBa(boolean aIsToolHead, ItemStack aStack) {
		return aIsToolHead ? MultiItemTool.getPrimaryMaterial(aStack, MT.Steel).mRGBaSolid : MultiItemTool.getSecondaryMaterial(aStack, MT.WOODS.Spruce).mRGBaSolid;
	}
	
	@Override
	public void onStatsAddedToTool(MultiItemTool aItem, int aID) {
		aItem.addItemBehavior(aID, new Behavior_Tool(TOOL_saw, SFX.MC_DIG_WOOD, getToolDamagePerContainerCraft(), F, SFX.RANDOM_PITCH));
		aItem.addItemBehavior(aID, Behavior_Place_Sapling.INSTANCE);
		aItem.addItemBehavior(aID, Behavior_Place_Workbench.INSTANCE);
	}
	
	@Override
	public String getDeathMessage() {
		return "[KILLER] failed to perform the 'sawing a woman in half' trick on [VICTIM]";
	}
}

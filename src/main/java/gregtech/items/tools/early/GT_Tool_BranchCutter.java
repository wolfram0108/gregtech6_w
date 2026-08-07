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

import gregapi.util.WD;
import gregapi.block.tree.BlockBaseLeaves;
import gregapi.data.IL;
import gregapi.data.MT;
import gregapi.item.multiitem.MultiItemTool;
import gregapi.item.multiitem.tools.ToolStats;
import gregapi.old.Textures;
import gregapi.render.IIconContainer;
import gregapi.util.ST;
import gregapi.util.UT;
import net.minecraft.world.level.block.Block;
import gregapi.block.Material;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;

import java.util.List;

import static gregapi.data.CS.*;

public class GT_Tool_BranchCutter extends ToolStats {
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
		return 100;
	}
	
	@Override
	public float getBaseDamage() {
		return 2.5F;
	}
	
	@Override
	public float getSpeedMultiplier() {
		return 0.25F;
	}
	
	@Override
	public float getMaxDurabilityMultiplier() {
		return 0.25F;
	}
	
	@Override public boolean canCollect()                                                   {return T;}
	@Override public boolean isGrafter()                                                    {return T;}
	
	@Override
	public int convertBlockDrops(List<ItemStack> aDrops, ItemStack aStack, Player aPlayer, Block aBlock, long aAvailableDurability, int aX, int aY, int aZ, byte aMetaData, int aFortune, boolean aSilkTouch, BlockDropsEvent aEvent) {
		// F-harvest: 1.7.10 HarvestDropsEvent.dropChance (шанс выпадения ванильного дропа) удалён — neo BlockDropsEvent
		// роняет getDrops() всегда (dropChance=1.0 эквивалент), а кастомный дроп задаётся aDrops ниже -> буст-строка no-op.
		// 1.7.10: `Blocks.leaves` = дуб/ель/берёза/джунгли (мета 0-3), `Blocks.leaves2` = акация/тёмный дуб.
		// В neo обе семьи расщеплены на отдельные блоки, а мета блока в мире больше не несёт породу — сравнение
		// с одним членом ловило только дуб/акацию и всегда роняло ДУБОВЫЙ саженец. Породу берём из положения
		// блока в семье (центр CS.Flattened), им же `ST.make` подставит саженец нужной породы.
		Block tLeavesHead = gregapi.data.CS.Flattened.headOf(aBlock);
		if (tLeavesHead == Blocks.OAK_LEAVES) {
			aDrops.clear();
			int tWood = gregapi.data.CS.Flattened.metaOf(aBlock);
			if (tWood == 0 && RNGSUS.nextInt(9) <= aFortune * 2) aDrops.add(IL.Food_Apple_Red.get(1)); else aDrops.add(ST.make(Blocks.OAK_SAPLING, 1, tWood));
		} else if (tLeavesHead == Blocks.ACACIA_LEAVES) {
			aDrops.clear();
			aDrops.add(ST.make(Blocks.OAK_SAPLING, 1, gregapi.data.CS.Flattened.metaOf(aBlock) + 4));
		} else if (aBlock == Blocks.VINE) {
			aDrops.clear();
			aDrops.add(ST.make(Blocks.VINE, 1, 0));
		} else if (aBlock instanceof BlockBaseLeaves) {
			aDrops.clear();
			aDrops.addAll(((BlockBaseLeaves)aBlock).getDrops(aPlayer.level(), aX, aY, aZ, aMetaData, aFortune)); // было getItemDropped(meta,rng,fortune)+damageDropped(meta) (1.7.10) -> GT6-leaves getDrops-центр

		} else if (IL.IC2_Leaves_Rubber.equal(aBlock)) {
			aDrops.clear();
			aDrops.add(IL.IC2_Sapling_Rubber.get(1));
		} else if (IL.AETHER_Skyroot_Leaves_Gold.equal(aBlock)) {
			aDrops.clear();
			aDrops.add(IL.AETHER_Skyroot_Sapling_Gold.get(1));
		} else if (IL.AETHER_Skyroot_Leaves_Green.equal(aBlock)) {
			aDrops.clear();
			aDrops.add(IL.AETHER_Skyroot_Sapling_Green.get(1));
		} else if (IL.AETHER_Skyroot_Leaves_Blue.equal(aBlock)) {
			aDrops.clear();
			aDrops.add(IL.AETHER_Skyroot_Sapling_Blue.get(1));
		} else if (IL.AETHER_Skyroot_Leaves_Dark.equal(aBlock)) {
			aDrops.clear();
			aDrops.add(IL.AETHER_Skyroot_Sapling_Dark.get(1));
		} else if (IL.AETHER_Skyroot_Leaves_Purple.equal(aBlock)) {
			aDrops.clear();
			aDrops.add(IL.AETHER_Skyroot_Sapling_Purple.get(1));
		} else if (IL.AETHER_Skyroot_Leaves_Apple.equal(aBlock)) {
			if (RNGSUS.nextInt(9) <= aFortune * 2) aDrops.add(IL.AETHER_Apple.get(1)); else aDrops.add(IL.AETHER_Skyroot_Sapling_Purple.get(1));
		}
		return 0;
	}
	
	@Override
	public boolean isMinableBlock(Block aBlock, byte aMetaData) {
		return "grafter".equalsIgnoreCase(WD.harvestTool(aBlock, aMetaData)) || aBlock == Blocks.VINE || WD.getMaterial(aBlock) == Material.leaves || IL.TF_Mazehedge.equal(aBlock);
	}
	
	@Override
	public IIconContainer getIcon(boolean aIsToolHead, ItemStack aStack) {
		return aIsToolHead ? Textures.ItemIcons.GRAFTER : Textures.ItemIcons.VOID;
	}
	
	@Override
	public short[] getRGBa(boolean aIsToolHead, ItemStack aStack) {
		return aIsToolHead ? MultiItemTool.getPrimaryMaterial(aStack, MT.Steel).mRGBaSolid : UNCOLOURED;
	}
	
	@Override
	public String getDeathMessage() {
		return "[VICTIM] has been trimmed by [KILLER]";
	}
}

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

package gregtech.items.behaviors;

import net.minecraft.core.BlockPos;

import gregapi.code.ItemNBT;
import gregapi.data.IL;
import gregapi.data.LH;
import gregapi.item.multiitem.MultiItem;
import gregapi.item.multiitem.behaviors.IBehavior.AbstractBehaviorDefault;
import gregapi.util.ST;
import gregapi.util.UT;
import gregapi.util.WD;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static gregapi.data.CS.*;

public class Behavior_Spray_Color extends AbstractBehaviorDefault {
	private final ItemStack mEmpty, mUsed, mFull;
	private final long mUses;
	private final byte mColor;
	
	public Behavior_Spray_Color(ItemStack aEmpty, ItemStack aUsed, ItemStack aFull, long aUses, byte aColor) {
		mEmpty = aEmpty;
		mUsed = aUsed;
		mFull = aFull;
		mUses = aUses * 10;
		mColor = UT.Code.bind4(aColor);
		LH.add("gt.behaviour.paintspray."+mColor+".tooltip", "Can Color things in " + DYE_NAMES[mColor]);
	}
	
	@Override
	public boolean onItemUseFirst(MultiItem aItem, ItemStack aStack, Player aPlayer, Level aWorld, int aX, int aY, int aZ, byte aSide, float hitX, float hitY, float hitZ) {
		if (aWorld.isClientSide() || aStack.getCount() != 1) return F;
		
		boolean rOutput = F;
		
		if (!(aPlayer).mayUseItemAt(new BlockPos(aX, aY, aZ), FORGE_DIR[aSide], aStack)) return F;
		
		CompoundTag tNBT = UT.NBT.getNBT(aStack);
		long tUses = tNBT.getLong("gt.remaining");
		
		if (ST.equal(aStack, mFull, T)) {
			ST.setItem(aStack, mUsed.getItem());
			ST.meta_(aStack, ST.meta_(mUsed));
			tUses = mUses;
		}
		if (ST.equal(aStack, mUsed, T)) {
			if (colorize(aWorld, aX, aY, aZ, aSide)) {
				UT.Sounds.send(SFX.IC_SPRAY, aWorld, aX, aY, aZ);
				if (!UT.Entities.hasInfiniteItems(aPlayer)) tUses-=10;
				rOutput = T;
			}
		}
		
		UT.NBT.set(aStack, UT.NBT.setPosNum(tNBT, "gt.remaining", tUses));
		
		if (tUses <= 0) {
			if (mEmpty == null) {
				aStack.setCount(aStack.getCount()-1);
			} else {
				ST.setItem(aStack, mEmpty.getItem());
				ST.meta_(aStack, ST.meta_(mEmpty));
			}
		}
		return rOutput;
	}
	
	@Override
	public boolean onRightClickEntity(MultiItem aItem, ItemStack aStack, Player aPlayer, Entity aEntity) {
		if (aStack.getCount() != 1) return F;
		
		boolean rUsed = F;
		
		if (aEntity instanceof Sheep && !((Sheep)aEntity).isSheared() ) {
			if (((Sheep)aEntity).getColor().getId() != (~mColor & 15)) {
				((Sheep)aEntity).setColor(net.minecraft.world.item.DyeColor.byId(~mColor & 15));
				if (aEntity.level().isClientSide()) return T;
				rUsed = T;
			}
		}
		if (aEntity instanceof Wolf && ((Wolf)aEntity).isTame()) {
			if (((Wolf)aEntity).getCollarColor().getId() != (~mColor & 15)) {
				UT.Reflection.callMethod((Wolf)aEntity, "setCollarColor", true, false, true, net.minecraft.world.item.DyeColor.byId(~mColor & 15));
				if (aEntity.level().isClientSide()) return T;
				rUsed = T;
			}
		}
		
		if (rUsed) {
			CompoundTag tNBT = UT.NBT.getNBT(aStack);
			long tUses = tNBT.getLong("gt.remaining");
			
			if (ST.equal(aStack, mFull, T)) {
				ST.setItem(aStack, mUsed.getItem());
				ST.meta_(aStack, ST.meta_(mUsed));
				tUses = mUses;
			}
			if (ST.equal(aStack, mUsed, T) && !UT.Entities.hasInfiniteItems(aPlayer)) tUses-=50;
			
			UT.NBT.set(aStack, UT.NBT.setPosNum(tNBT, "gt.remaining", tUses));
			
			if (tUses <= 0) {
				if (mEmpty == null) {
					aStack.setCount(aStack.getCount()-1);
				} else {
					ST.setItem(aStack, mEmpty.getItem());
					ST.meta_(aStack, ST.meta_(mEmpty));
				}
			}
			
			return T;
		}
		return F;
	}
	
	private final Collection<Block> mAllowedVanillaBlocks = Arrays.asList(Blocks.GRASS_BLOCK, Blocks.GLASS, Blocks.GLASS_PANE, Blocks.WHITE_STAINED_GLASS, Blocks.WHITE_STAINED_GLASS_PANE, Blocks.WHITE_CARPET, Blocks.TERRACOTTA, Blocks.WHITE_TERRACOTTA);
	
	private boolean colorize(Level aWorld, int aX, int aY, int aZ, byte aSide) {
		Block aBlock = WD.block(aWorld, aX, aY, aZ);
		// F4-flatten: список выше перечисляет ОДИН блок на семью, потому что в 1.7.10 он и был всей семьёй
		// (stained_glass = все 16 оттенков). После расщепления «перекрасить уже цветное» перестало проходить
		// проверку — поймано живым стендом GT6-FLATTENPROBE (REPAINT красное→жёлтое: осталось красным).
		// Спрашиваем принадлежность цветовой семье у центра CS.Flattened, а не перечисляем 16 оттенков здесь.
		if (aBlock != NB && (mAllowedVanillaBlocks.contains(aBlock) || gregapi.data.CS.Flattened.isColored(aBlock) || aBlock.defaultBlockState().is(net.minecraft.tags.BlockTags.WOOL) || IL.TE_Rockwool.block() == aBlock || aBlock == BlocksGT.Grass)) {
			if (aBlock == Blocks.TERRACOTTA  ) return WD.set(aWorld, aX, aY, aZ, Blocks.WHITE_TERRACOTTA, ~mColor & 15, 3);
			if (aBlock == Blocks.GLASS_PANE     ) return WD.set(aWorld, aX, aY, aZ, Blocks.WHITE_STAINED_GLASS_PANE   , ~mColor & 15, 3);
			if (aBlock == Blocks.GLASS          ) return WD.set(aWorld, aX, aY, aZ, Blocks.WHITE_STAINED_GLASS        , ~mColor & 15, 3);
			
			if (aBlock == Blocks.GRASS_BLOCK || aBlock == BlocksGT.Grass) {
				switch(mColor) {
				case DYE_INDEX_Green    : return WD.set(aWorld, aX, aY, aZ, BlocksGT.Grass, 0, 3);
				case DYE_INDEX_Lime     : return WD.set(aWorld, aX, aY, aZ, BlocksGT.Grass, 1, 3);
				case DYE_INDEX_Black    : return WD.set(aWorld, aX, aY, aZ, BlocksGT.Grass, 2, 3);
				case DYE_INDEX_LightGray: return WD.set(aWorld, aX, aY, aZ, BlocksGT.Grass, 3, 3);
				case DYE_INDEX_Yellow   : return WD.set(aWorld, aX, aY, aZ, BlocksGT.Grass, 4, 3);
				case DYE_INDEX_Brown    : return WD.set(aWorld, aX, aY, aZ, BlocksGT.Grass, 5, 3);
				default: return F;
				}
			}
			return WD.meta(aWorld, aX, aY, aZ) != (~mColor & 15) && WD.set(aWorld, aX, aY, aZ, WD.block(aWorld, aX, aY, aZ), ~mColor & 15, 3, F);
		}
		// F-block-recolor: было aBlock.recolourBlock(world,x,y,z,FORGE_DIR[aSide],~mColor&15) — 1.7.10 Forge-метод на
		// ЛЮБОМ Block (дефолт false) удалён из neo. GT6-блоки, что его переопределяли, несут recolourBlock как СВОЙ метод
		// (BlockColored; MultiTileEntityBlock → делегирует IMTE_RecolourBlock тайла) — зовём его; прочие блоки не
		// перекрашиваются (1:1 с forge-дефолтом false). Это единственная точка вызова (централизация §3).
		if (aBlock instanceof gregapi.block.metatype.BlockColored tBC) return tBC.recolourBlock(aWorld, aX, aY, aZ, FORGE_DIR[aSide], ~mColor & 15);
		if (aBlock instanceof gregapi.block.multitileentity.MultiTileEntityBlock tMTE) return tMTE.recolourBlock(aWorld, aX, aY, aZ, FORGE_DIR[aSide], ~mColor & 15);
		return F;
	}
	
	static {
		LH.add("gt.behaviour.paintspray.uses", "Remaining Uses:");
		LH.add("gt.behaviour.unstackable", "Not usable when stacked!");
	}
	
	@Override
	public List<String> getAdditionalToolTips(MultiItem aItem, List<String> aList, ItemStack aStack) {
		aList.add(LH.get("gt.behaviour.paintspray."+mColor+".tooltip"));
		CompoundTag tNBT = ItemNBT.get(aStack);
		long tRemaining = (ST.equal(aStack, mFull, T)?mUses:tNBT==null?0:tNBT.getLong("gt.remaining"));
		aList.add(LH.get("gt.behaviour.paintspray.uses") + " " + (tRemaining / 10) + "." + (tRemaining % 10));
		aList.add(LH.get("gt.behaviour.unstackable"));
		return aList;
	}
}

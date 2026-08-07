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

package gregapi.item.multiitem.behaviors;
import gregapi.util.WD;

import gregapi.data.CS.SFX;
import gregapi.data.LH;
import gregapi.item.multiitem.MultiItem;
import gregapi.item.multiitem.MultiItemTool;
import gregapi.item.multiitem.behaviors.IBehavior.AbstractBehaviorDefault;
import gregapi.util.UT;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

import static gregapi.data.CS.F;
import static gregapi.data.CS.T;

public class Behavior_TripwireCutting extends AbstractBehaviorDefault {
	private final int mCosts;
	
	public Behavior_TripwireCutting(int aCosts) {
		mCosts = aCosts;
	}
	
	@Override
	public boolean onItemUseFirst(MultiItem aItem, ItemStack aStack, Player aPlayer, Level aWorld, int aX, int aY, int aZ, byte aSide, float hitX, float hitY, float hitZ) {
		if (aPlayer.level().isClientSide()) return F;
		if (WD.block(aWorld, aX, aY, aZ) == Blocks.TRIPWIRE) {
			if (((MultiItemTool)aItem).doDamage(aStack, mCosts, aPlayer, F)) {
				int aMeta = WD.meta(aWorld, aX, aY, aZ) | 8;
				WD.set(aWorld, aX, aY, aZ, WD.block(aWorld, aX, aY, aZ), aMeta, 4, F);
				// было removedByPlayer(World,EntityPlayer,x,y,z,willHarvest) -> IBlockExtension.onDestroyedByPlayer
				// (BlockState,Level,BlockPos,Player,ItemStack,boolean,FluidState) [IBlockExtension.java:238]
				BlockPos aBlockPos = new BlockPos(aX, aY, aZ);
				BlockState aBlockState = aWorld.getBlockState(aBlockPos);
				if (Blocks.TRIPWIRE.onDestroyedByPlayer(aBlockState, aWorld, aBlockPos, aPlayer, aPlayer.getMainHandItem(), T, aWorld.getFluidState(aBlockPos))) {
					// F13 (1:1): 1.7.10 harvestBlock(World,Player,x,y,z,meta) ронял дроп tripwire (струну). Восстановлено ниже через
					// neo Block.dropResources(state,level,pos,be,breaker,tool) [Block.java:398] — loot по инструменту игрока.
					// onDestroyedByPlayer уже снёс блок в air; дропаем по захваченному aBlockState. Дроп восстановлен 1:1,
					// звук остаётся.
					net.minecraft.world.level.block.Block.dropResources(aBlockState, aWorld, aBlockPos, null, aPlayer, aPlayer.getMainHandItem());
					UT.Sounds.send(SFX.MC_SHEARS, aWorld, aX, aY, aZ);
				}
			}
			return T;
		}
		return F;
	}
	
	static {
		LH.add("gt.behaviour.tripwirecutting", "Can cut Tripwires by Rightclick");
	}
	
	@Override
	public List<String> getAdditionalToolTips(MultiItem aItem, List<String> aList, ItemStack aStack) {
		aList.add(LH.get("gt.behaviour.tripwirecutting"));
		return aList;
	}
}

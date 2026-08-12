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
import static gregapi.data.CS.FORGE_DIR;

import net.minecraft.core.BlockPos;

import gregapi.data.CS.SFX;
import gregapi.data.FL;
import gregapi.item.multiitem.MultiItem;
import gregapi.item.multiitem.behaviors.IBehavior;
import gregapi.item.multiitem.behaviors.IBehavior.AbstractBehaviorDefault;
import gregapi.util.UT;
import gregapi.util.WD;
import ic2.api.crops.ICropTile;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.Level;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidContainerItem;

import static gregapi.data.CS.F;
import static gregapi.data.CS.T;

public class Behavior_Watering_Crops extends AbstractBehaviorDefault {
	public static final IBehavior<MultiItem> INSTANCE = new Behavior_Watering_Crops();
	
	@Override
	public boolean onItemUseFirst(MultiItem aItem, ItemStack aStack, Player aPlayer, Level aWorld, int aX, int aY, int aZ, byte aSide, float hitX, float hitY, float hitZ) {
		if (aWorld.isClientSide() || aPlayer == null || !(aPlayer).mayUseItemAt(new BlockPos(aX, aY, aZ), FORGE_DIR[aSide], aStack)) return F;
		FluidStack mFluid = ((IFluidContainerItem)aItem).getFluid(aStack); // F5/BUG-045 (1:1): восстановленный IFluidContainerItem (оригинал :46)
		if (FL.water(mFluid)) {
			BlockEntity tTileEntity = WD.te(aWorld, aX, aY, aZ, F);
			try {if (tTileEntity instanceof ICropTile) {
				int tHydration = ((ICropTile)tTileEntity).getHydrationStorage();
				int tDrained = Math.min((200-tHydration)/10, mFluid.getAmount());
				if (tDrained > 0) {
					((IFluidContainerItem)aItem).drain(aStack, tDrained, T); // F5/BUG-045 (1:1): восстановленный IFluidContainerItem.drain (оригинал :53); FluidUtil-путь был мёртв (capability не регистрируется)
					((ICropTile)tTileEntity).setHydrationStorage(tHydration + tDrained*10);
					UT.Sounds.send(SFX.MC_LIQUID_WATER, aWorld, aX, aY, aZ);
				}
				return T;
			}} catch(Throwable e) {/**/}
		}
		return F;
	}
}

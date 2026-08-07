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

package gregapi.compat.forestry;
import gregapi.util.WD;

import net.neoforged.fml.event.lifecycle.FMLLoadCompleteEvent;
import forestry.api.farming.Farmables;
import forestry.api.farming.ICrop;
import forestry.api.farming.IFarmable;
import forestry.api.storage.BackpackManager;
import forestry.core.utils.vect.Vect;
import forestry.farming.logic.CropBlock;
import gregapi.block.ItemBlockBase;
import gregapi.block.tree.BlockBaseSapling;
import gregapi.code.ItemStackContainer;
import gregapi.code.ItemStackSet;
import gregapi.compat.CompatBase;
import gregapi.data.MD;
import gregapi.util.ST;
import gregapi.util.UT;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import static gregapi.data.CS.*;

public class CompatFR extends CompatBase implements ICompatFR, IFarmable {
	public ItemStackSet<ItemStackContainer> mWindfalls = ST.hashset();
	
	// @Override
	public void onPostLoad(FMLLoadCompleteEvent aEvent) {
		Farmables.farmables.get("farmArboreal").add(this);
	}
	
	@Override
	public void addToBackpacks(String aType, ItemStack aStack) {
		if (MD.FR.mLoaded) try {
			BackpackManager.definitions.get(aType).addValidItem(aStack);
		} catch(Throwable e) {
			e.printStackTrace(ERR);
		}
	}
	
	// @Override
	public boolean isSaplingAt(Level aWorld, int aX, int aY, int aZ) {
		return WD.block(aWorld, aX, aY, aZ) instanceof BlockBaseSapling;
	}
	
	// @Override
	public ICrop getCropAt(Level aWorld, int aX, int aY, int aZ) {
		Block aBlock = WD.block(aWorld, aX, aY, aZ);
		return WD.wood(aBlock, aWorld, aX, aY, aZ) ? new CropBlock(aWorld, aBlock, WD.meta(aWorld, aX, aY, aZ), new Vect(aX, aY, aZ)) : null;
	}
	
	// @Override
	public boolean isGermling(ItemStack aStack) {
		return aStack.getItem() instanceof ItemBlockBase && ((ItemBlockBase)aStack.getItem()).mPlaceable instanceof BlockBaseSapling;
	}
	
	@Override
	public void addWindfall(ItemStack aStack) {mWindfalls.add(aStack);}
	
	// @Override
	public boolean isWindfall(ItemStack aStack) {
		return mWindfalls.contains(aStack, T);
	}
	
	// @Override
	public boolean plantSaplingAt(Player aPlayer, ItemStack aSeed, Level aWorld, int aX, int aY, int aZ) {
		return UT.tryPlaceItemIntoWorld(aSeed.copy(), aPlayer, aWorld, aX, aY - 1, aZ, SIDE_UP, 0, 0, 0);
	}
}

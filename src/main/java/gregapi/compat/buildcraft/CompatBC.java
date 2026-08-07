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

package gregapi.compat.buildcraft;
import gregapi.util.WD;

import buildcraft.api.core.BuildCraftAPI;
import buildcraft.core.properties.WorldPropertyIsWood;
import net.neoforged.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import gregapi.code.TagData;
import gregapi.compat.CompatBase;
import gregapi.data.OP;
import gregapi.data.TD;
import gregapi.util.ST;
import gregapi.wooddict.WoodDictionary;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HugeMushroomBlock;
import net.minecraft.world.level.BlockGetter;

import static gregapi.data.CS.T;


public class CompatBC extends CompatBase implements ICompatBC {
	public CompatBC() {
		TriggerBC_Energy_Capacity_Empty.class.getCanonicalName();
		TriggerBC_Energy_Capacity_Partial.class.getCanonicalName();
		TriggerBC_Energy_Capacity_NotFull.class.getCanonicalName();
		TriggerBC_Energy_Capacity_Full.class.getCanonicalName();
		WorldPropertyIsWood.class.getCanonicalName();
		BuildCraftAPI.class.getCanonicalName();
	}
	
	// @Override
	public void onPostLoad(FMLLoadCompleteEvent aEvent) {
		for (TagData tEnergyType : TD.Energy.ALL) {
			new TriggerBC_Energy_Capacity_Empty(tEnergyType);
			new TriggerBC_Energy_Capacity_Partial(tEnergyType);
			new TriggerBC_Energy_Capacity_NotFull(tEnergyType);
			new TriggerBC_Energy_Capacity_Full(tEnergyType);
		}
	}
	
	@Override
	public void onServerStarting(ServerStartingEvent aEvent) {
		BuildCraftAPI.registerWorldProperty("wood", new WorldPropertyIsLog());
	}
	
	public static class WorldPropertyIsLog extends WorldPropertyIsWood {
		// @Override
		public boolean get(BlockGetter aWorld, Block aBlock, int aMeta, int aX, int aY, int aZ) {
			return aBlock instanceof HugeMushroomBlock || WD.wood(aBlock, aWorld, aX, aY, aZ) || OP.log.contains(ST.make(aBlock, 1, aMeta)) || WoodDictionary.WOODS.containsKey(aBlock, aMeta, T);
		}
	}
}

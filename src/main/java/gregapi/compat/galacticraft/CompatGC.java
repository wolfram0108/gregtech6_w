/**
 * Copyright (c) 2019 Gregorius Techneticies
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

package gregapi.compat.galacticraft;

import static gregapi.data.CS.*;

import net.neoforged.bus.api.SubscribeEvent;
import gregapi.compat.CompatBase;
import gregapi.worldgen.GT6WorldGenerator;
import micdoodle8.mods.galacticraft.api.event.wgen.GCCoreEventPopulate;
import micdoodle8.mods.galacticraft.api.power.EnergySource;
import micdoodle8.mods.galacticraft.api.power.EnergySource.EnergySourceAdjacent;
import micdoodle8.mods.galacticraft.api.power.IEnergyHandlerGC;
import micdoodle8.mods.galacticraft.api.transmission.NetworkType;
import micdoodle8.mods.galacticraft.api.transmission.tile.IConnector;
import micdoodle8.mods.galacticraft.core.energy.EnergyConfigHandler;
import net.neoforged.neoforge.common.NeoForge;

public class CompatGC extends CompatBase implements ICompatGC {
	public final EnergySourceAdjacent[] ENERGY_DIR = {new EnergySourceAdjacent(FORGE_DIR[0]), new EnergySourceAdjacent(FORGE_DIR[1]), new EnergySourceAdjacent(FORGE_DIR[2]), new EnergySourceAdjacent(FORGE_DIR[3]), new EnergySourceAdjacent(FORGE_DIR[4]), new EnergySourceAdjacent(FORGE_DIR[5]), new EnergySourceAdjacent(FORGE_DIR[6])};
	
	public CompatGC() {
		NetworkType.POWER.toString();
		IConnector.class.getCanonicalName();
		IEnergyHandlerGC.class.getCanonicalName();
		EnergySource.EnergySourceAdjacent.class.getCanonicalName();
		EnergyConfigHandler.class.getCanonicalName();
		NeoForge.EVENT_BUS.register(this);
	}
	
	@Override public Object dir(byte aSide) {return ENERGY_DIR[aSide];}
	
	@SubscribeEvent
	public void populate(GCCoreEventPopulate.Post aEvent) {
		// F6/F10-DEFERRED (GC worldgen): GT6WorldGenerator.generate теперь работает по WorldGenLevel (data-driven Feature-путь
		// стадии FEATURES — чистая архитектура вместо серверно-тикового хака). GC-populate — ЛЕГАСИ Forge-событие, отдаёт полный
		// Level (worldObj), а не WorldGenLevel региона → напрямую скормить нельзя. Интеграция GC-worldgen требует СОБСТВЕННОГО
		// GC-Feature (как у ванильных измерений), что относится к порту самого Galacticraft — foreign-gated, MD.GC не портирован,
		// событие в текущем состоянии не приходит. Ноль-действие корректно, пока GC отсутствует; при порте GC — завести GC-Feature.
		if (aEvent.worldObj instanceof net.minecraft.world.level.WorldGenLevel tWGL) GT6WorldGenerator.generate(tWGL, aEvent.chunkX, aEvent.chunkZ, T);
	}
}

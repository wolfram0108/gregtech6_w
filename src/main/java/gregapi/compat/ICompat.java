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

package gregapi.compat;

import gregapi.api.FMLPreInitializationEvent;
import gregapi.api.FMLInitializationEvent;
import gregapi.api.FMLPostInitializationEvent;
import gregapi.api.FMLModIdMappingEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import gregapi.code.ArrayListNoNulls;

import java.util.Collection;


public interface ICompat {
	public static final Collection<ICompat> COMPAT_CLASSES = new ArrayListNoNulls<>();
	
	public void onPreLoad       (FMLPreInitializationEvent aEvent);
	public void onLoad          (FMLInitializationEvent aEvent);
	public void onPostLoad      (FMLPostInitializationEvent aEvent);
	public void onServerStarting(ServerStartingEvent aEvent);
	public void onServerStarted (ServerStartedEvent aEvent);
	public void onServerStopping(ServerStoppingEvent aEvent);
	public void onServerStopped (ServerStoppedEvent aEvent);
	public void onIDChanging    (FMLModIdMappingEvent aEvent);
}

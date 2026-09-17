/**
 * Copyright (c) 2021 GregTech-6 Team
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

package gregapi.api;

import gregapi.api.FMLInitializationEvent;
import gregapi.api.FMLPostInitializationEvent;
import gregapi.api.FMLPreInitializationEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;

/**
 * @author Gregorius Techneticies
 * 
 * Base Proxy used for all my Mods.
 */
public abstract class Abstract_Proxy {
	/** Registers proxy @SubscribeEvent methods on MinecraftForge.EVENT_BUS one by one via addListener,
	 *  working around neo's ban on register(this) when handlers live on a supertype, not the instance's own class. */
	protected final void registerSubscribeEvents() {
		for (java.lang.reflect.Method tMethod : getClass().getMethods()) {
			net.minecraftforge.eventbus.api.SubscribeEvent tAnnotation = tMethod.getAnnotation(net.minecraftforge.eventbus.api.SubscribeEvent.class);
			if (tAnnotation == null || tMethod.getParameterCount() != 1) continue;
			Class<?> tParameter = tMethod.getParameterTypes()[0];
			if (!net.minecraftforge.eventbus.api.Event.class.isAssignableFrom(tParameter)) continue;
			// Mod-bus events (IModBusEvent) can't be listened on the common MinecraftForge.EVENT_BUS -- neo throws on registration.
			// They register on the mod bus separately (registerClientModels/RegisterEvent handlers). Only game-bus events belong here.
			if (net.minecraftforge.fml.event.IModBusEvent.class.isAssignableFrom(tParameter)) continue;
			java.util.function.Consumer<net.minecraftforge.eventbus.api.Event> tDispatch = aEvent -> {
				try {tMethod.invoke(this, aEvent);}
				catch (ReflectiveOperationException e) {throw new RuntimeException("Abstract_Proxy: сбой диспетчеризации события " + tMethod, e);}
			};
			if (java.lang.reflect.Modifier.isAbstract(tParameter.getModifiers())) {
				for (Class<?> tSub : tParameter.getDeclaredClasses()) {
					if (!java.lang.reflect.Modifier.isAbstract(tSub.getModifiers()) && tParameter.isAssignableFrom(tSub)) {
						@SuppressWarnings({"unchecked", "rawtypes"})
						Class<net.minecraftforge.eventbus.api.Event> tSubType = (Class) tSub;
						net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener(tAnnotation.priority(), tAnnotation.receiveCanceled(), tSubType, tDispatch);
					}
				}
			} else {
				@SuppressWarnings({"unchecked", "rawtypes"})
				Class<net.minecraftforge.eventbus.api.Event> tEventType = (Class) tParameter;
				net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener(tAnnotation.priority(), tAnnotation.receiveCanceled(), tEventType, tDispatch);
			}
		}
	}

	public void onProxyBeforePreInit        (Abstract_Mod aMod, FMLPreInitializationEvent   aEvent) {/**/}
	public void onProxyBeforeInit           (Abstract_Mod aMod, FMLInitializationEvent      aEvent) {/**/}
	public void onProxyBeforePostInit       (Abstract_Mod aMod, FMLPostInitializationEvent  aEvent) {/**/}
	/** NEVER DO ANYTHING LAGGY HERE!!! */
	public void onProxyBeforeServerStarting (Abstract_Mod aMod, ServerStartingEvent      aEvent) {/**/}
	/** NEVER DO ANYTHING LAGGY HERE!!! */
	public void onProxyBeforeServerStarted  (Abstract_Mod aMod, ServerStartedEvent       aEvent) {/**/}
	public void onProxyBeforeServerStopping (Abstract_Mod aMod, ServerStoppingEvent      aEvent) {/**/}
	public void onProxyBeforeServerStopped  (Abstract_Mod aMod, ServerStoppedEvent       aEvent) {/**/}
	
	public void onProxyAfterPreInit         (Abstract_Mod aMod, FMLPreInitializationEvent   aEvent) {/**/}
	public void onProxyAfterInit            (Abstract_Mod aMod, FMLInitializationEvent      aEvent) {/**/}
	public void onProxyAfterPostInit        (Abstract_Mod aMod, FMLPostInitializationEvent  aEvent) {/**/}
	/** NEVER DO ANYTHING LAGGY HERE!!! */
	public void onProxyAfterServerStarting  (Abstract_Mod aMod, ServerStartingEvent      aEvent) {/**/}
	/** NEVER DO ANYTHING LAGGY HERE!!! */
	public void onProxyAfterServerStarted   (Abstract_Mod aMod, ServerStartedEvent       aEvent) {/**/}
	public void onProxyAfterServerStopping  (Abstract_Mod aMod, ServerStoppingEvent      aEvent) {/**/}
	public void onProxyAfterServerStopped   (Abstract_Mod aMod, ServerStoppedEvent       aEvent) {/**/}
}

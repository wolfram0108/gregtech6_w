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
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;

/**
 * @author Gregorius Techneticies
 * 
 * Base Proxy used for all my Mods.
 */
public abstract class Abstract_Proxy {
	/** F7 (centralized, "one place"): registers the proxy's {@code @SubscribeEvent} methods on
	 *  {@code NeoForge.EVENT_BUS} via a per-method {@code addListener} — works around neo's ban on
	 *  {@code register(this)} when the handlers live on a SUPERtype (the base proxy holds them
	 *  centrally, while the instance is the Server/Client subclass; otherwise {@code EventBus.checkSupertypes}
	 *  throws IllegalArgumentException). {@code getClass().getMethods()} takes the RUNTIME type → catches base+subclass
	 *  (including client-only ones on *_Client) with no silent skip. An abstract event class (ServerTickEvent etc.,
	 *  a 1.7.10-style phase) unfolds into its concrete nested subclasses — the method takes the base, instanceof inside
	 *  sorts it out. Called from the CONCRETE proxy's constructor (GT_API_Proxy/GT_Proxy). */
	protected final void registerSubscribeEvents() {
		for (java.lang.reflect.Method tMethod : getClass().getMethods()) {
			net.neoforged.bus.api.SubscribeEvent tAnnotation = tMethod.getAnnotation(net.neoforged.bus.api.SubscribeEvent.class);
			if (tAnnotation == null || tMethod.getParameterCount() != 1) continue;
			Class<?> tParameter = tMethod.getParameterTypes()[0];
			if (!net.neoforged.bus.api.Event.class.isAssignableFrom(tParameter)) continue;
			// F7 bus split (engine force): mod-bus events (IModBusEvent, e.g. TextureAtlasStitchedEvent/ModelEvent/
			// RegisterEvent) MUST NOT be hung on the shared NeoForge.EVENT_BUS — neo throws "IModBusEvent not allowed on the
			// common bus" on registration (crashed runData/runClient during mod construction). They are registered on the mod bus
			// separately (registerClientModels/RegisterEvent handlers). Here — only game-bus @SubscribeEvent.
			if (net.neoforged.fml.event.IModBusEvent.class.isAssignableFrom(tParameter)) continue;
			java.util.function.Consumer<net.neoforged.bus.api.Event> tDispatch = aEvent -> {
				try {tMethod.invoke(this, aEvent);}
				catch (ReflectiveOperationException e) {throw new RuntimeException("Abstract_Proxy: event dispatch failure " + tMethod, e);}
			};
			if (java.lang.reflect.Modifier.isAbstract(tParameter.getModifiers())) {
				for (Class<?> tSub : tParameter.getDeclaredClasses()) {
					if (!java.lang.reflect.Modifier.isAbstract(tSub.getModifiers()) && tParameter.isAssignableFrom(tSub)) {
						@SuppressWarnings({"unchecked", "rawtypes"})
						Class<net.neoforged.bus.api.Event> tSubType = (Class) tSub;
						net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(tAnnotation.priority(), tAnnotation.receiveCanceled(), tSubType, tDispatch);
					}
				}
			} else {
				@SuppressWarnings({"unchecked", "rawtypes"})
				Class<net.neoforged.bus.api.Event> tEventType = (Class) tParameter;
				net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(tAnnotation.priority(), tAnnotation.receiveCanceled(), tEventType, tDispatch);
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

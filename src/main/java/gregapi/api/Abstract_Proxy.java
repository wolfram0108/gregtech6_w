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
	/** F7 (централизованно, «одно место»): регистрация {@code @SubscribeEvent}-методов прокси на
	 *  {@code NeoForge.EVENT_BUS} через per-method {@code addListener} — обходит запрет neo на
	 *  {@code register(this)}, когда обработчики лежат на СУПЕРтипе (base-прокси держит их
	 *  централизованно, а инстанс — Server/Client-подкласс; {@code EventBus.checkSupertypes} иначе бьёт
	 *  IllegalArgumentException). {@code getClass().getMethods()} берёт РАНТАЙМ-тип → ловит base+подкласс
	 *  (включая клиентские у *_Client) без тихого пропуска. Абстрактный event-класс (ServerTickEvent и т.п.,
	 *  фаза 1.7.10) раскладывается на конкретные вложенные подклассы — метод берёт базу, instanceof внутри
	 *  разрулит. Зовётся из конструктора КОНКРЕТНОГО прокси (GT_API_Proxy/GT_Proxy). */
	protected final void registerSubscribeEvents() {
		for (java.lang.reflect.Method tMethod : getClass().getMethods()) {
			net.minecraftforge.eventbus.api.SubscribeEvent tAnnotation = tMethod.getAnnotation(net.minecraftforge.eventbus.api.SubscribeEvent.class);
			if (tAnnotation == null || tMethod.getParameterCount() != 1) continue;
			Class<?> tParameter = tMethod.getParameterTypes()[0];
			if (!net.neoforged.bus.api.Event.class.isAssignableFrom(tParameter)) continue;
			// F7 bus-раздел (форс движка): mod-bus события (IModBusEvent, напр. TextureAtlasStitchedEvent/ModelEvent/
			// RegisterEvent) НЕЛЬЗЯ вешать на общую NeoForge.EVENT_BUS — neo бросает "IModBusEvent not allowed on the
			// common bus" при регистрации (крашило runData/runClient на конструкции мода). Они регистрируются на mod-шине
			// отдельно (registerClientModels/RegisterEvent-хендлеры). Здесь — только game-bus @SubscribeEvent.
			if (net.minecraftforge.fml.event.IModBusEvent.class.isAssignableFrom(tParameter)) continue;
			java.util.function.Consumer<net.neoforged.bus.api.Event> tDispatch = aEvent -> {
				try {tMethod.invoke(this, aEvent);}
				catch (ReflectiveOperationException e) {throw new RuntimeException("Abstract_Proxy: сбой диспетчеризации события " + tMethod, e);}
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

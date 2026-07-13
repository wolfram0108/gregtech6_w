package ic2.api.info;

import ic2.api.item.IWrenchHandler;

/** F10 ЗЕРКАЛО (compile-only) чужого API. Сверено javap ic2:IC2Classic:1.2.1.8-dev
 *  (ic2.api.info.IC2Classic). Реально используется — CompatIC2C.java:35:
 *  registerWrenchHandler(IWrenchHandler):void. Остальные статические поля/методы реального
 *  IC2Classic (windNetwork, getLoadedIC2Type, isIc2ExpLoaded, …) в GT6-исходнике не
 *  используются (греп 0) — не добавлены. */
public class IC2Classic {
	public static void registerWrenchHandler(IWrenchHandler aHandler) {/**/}
}

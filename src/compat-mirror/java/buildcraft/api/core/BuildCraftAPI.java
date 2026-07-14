package buildcraft.api.core;

/** F10 ЗЕРКАЛО (compile-only) чужого API BuildCraft. GT6 зовёт статический registerWorldProperty в
 *  CompatBC.onServerStarting (javap buildcraft-7.1.23-dev: (String, IWorldProperty)). Реальный мод не грузится
 *  (guard MD.BC), интеграция отложена -> no-op. См. compat-mirror/README.md. */
public class BuildCraftAPI {
	public static void registerWorldProperty(String name, IWorldProperty property) {}
}

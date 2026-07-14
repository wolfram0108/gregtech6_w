package buildcraft.api.statements;

/** F10 ЗЕРКАЛО (compile-only) чужого API BuildCraft. GT6 зовёт статические registerStatement/
 *  registerTriggerProvider в TriggerBC-регистрации (javap buildcraft-7.1.23-dev). Реальный мод не грузится
 *  (guard MD.BC), интеграция отложена -> no-op. См. compat-mirror/README.md. */
public class StatementManager {
	public static void registerStatement(IStatement statement) {}
	public static void registerTriggerProvider(ITriggerProvider provider) {}
}

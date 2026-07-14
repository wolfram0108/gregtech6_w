package buildcraft.api.statements;

/** F10 ЗЕРКАЛО (compile-only) чужого API BuildCraft. extends IStatement — как в реальном BC
 *  (ITriggerExternal->ITrigger->IStatement), чтобы TriggerBC (implements ITriggerExternal) годился аргументом
 *  StatementManager.registerStatement(IStatement). Реальный мод не грузится. См. compat-mirror/README.md. */
public interface ITriggerExternal extends IStatement {}

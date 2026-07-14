package buildcraft.api.core;

/** F10 ЗЕРКАЛО (compile-only) чужого API BuildCraft. Маркер-интерфейс мировых свойств; GT6 регистрирует
 *  WorldPropertyIsLog (extends WorldPropertyIsWood implements IWorldProperty) через BuildCraftAPI.
 *  registerWorldProperty. Реальный мод не грузится. См. compat-mirror/README.md. */
public interface IWorldProperty {}

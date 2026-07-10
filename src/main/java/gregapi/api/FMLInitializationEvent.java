package gregapi.api;

/**
 * Переходник F1 (жизненный цикл). Событие фазы Init GregTech6 поверх жизненного цикла NeoForge.
 *
 * <p>Сохраняет трёхфазный контракт GregTech6 (Pre/Init/Post). neo-точка входа
 * ({@code gregtech6.GregTech6}) конструирует его и передаёт в {@code Abstract_Mod.onModInit(...)}.
 * По всему исходнику GregTech6 с этого события ничего не читается — оно служит маркером фазы.</p>
 */
public class FMLInitializationEvent {
	public FMLInitializationEvent() {}
}

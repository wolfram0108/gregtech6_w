package forestry.api.recipes;

/** Forestry-mirror (compile-only; настоящий Forestry инициализирует эти менеджеры в рантайме).
 *  GT6 обращается только под {@code MD.FR.mLoaded}. */
public class RecipeManagers {
	public static ICentrifugeManager centrifugeManager;
	public static ISqueezerManager   squeezerManager;
}

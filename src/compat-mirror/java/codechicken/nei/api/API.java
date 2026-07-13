package codechicken.nei.api;

/** F10 ЗЕРКАЛО (compile-only) чужого API — NEI. Минимум для ST.hide/NEI_RecipeMap/NEI_GT_API_Config. */
public class API {
    public static void hideItem(Object aStack) {}
    public static void registerRecipeHandler(Object aHandler) {}
    public static void registerUsageHandler(Object aHandler) {}
    public static void registerGuiOverlay(Class<?> aGuiClass, String aOverlayId, int aX, int aY) {}
    public static void registerGuiOverlayHandler(Class<?> aGuiClass, Object aHandler, String aOverlayId) {}
}

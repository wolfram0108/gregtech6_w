package ic2.core;

/** F10 ЗЕРКАЛО (compile-only) чужого API. Минимум для компиляции ядра; члены добираются
 *  компилятором. Реальная зависимость — при возврате к интеграции. См. compat-mirror/README.md. */
public class AdvRecipe {
	public Object[] input; // реально читается GregTech (Loader_Recipes_Replace: .input как массив ингредиентов)
}

package ic2.api.recipe;

/** F10 ЗЕРКАЛО (compile-only) чужого API. Сверено javap ic2:IC2Classic:1.2.1.8-dev
 *  (ic2.api.recipe.RecipeInputOreDict): {@code implements IRecipeInput},
 *  ctor(String,int) — реально вызывается CompatIC2.java:156.
 *  Поля/остальные ctor/методы реального API не используются (греп 0) — не добавлены. */
public class RecipeInputOreDict implements IRecipeInput {
	public RecipeInputOreDict(String aOreDict, int aAmount) {/**/}
}

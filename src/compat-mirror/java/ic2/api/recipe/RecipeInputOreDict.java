package ic2.api.recipe;

/** F10 ЗЕРКАЛО (compile-only) чужого API. Сверено javap ic2:IC2Classic:1.2.1.8-dev
 *  (ic2.api.recipe.RecipeInputOreDict): {@code implements IRecipeInput},
 *  ctor(String,int) — реально вызывается CompatIC2.java:156.
 *  Поля/остальные ctor/методы реального API не используются (греп 0) — не добавлены. */
public class RecipeInputOreDict implements IRecipeInput {
	public String input; // реально читается GregTech (Loader_Recipes_Replace: .input как String OreDict)
	public RecipeInputOreDict(String aOreDict, int aAmount) {input = aOreDict;}
	@Override public boolean matches(net.minecraft.world.item.ItemStack aStack) {return false;} // compile-only мирор, мод не грузится.
}

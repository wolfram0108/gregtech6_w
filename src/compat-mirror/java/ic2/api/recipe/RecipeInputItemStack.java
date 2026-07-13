package ic2.api.recipe;

import net.minecraft.world.item.ItemStack;

/** F10 ЗЕРКАЛО (compile-only) чужого API. Сверено javap ic2:IC2Classic:1.2.1.8-dev
 *  (ic2.api.recipe.RecipeInputItemStack): {@code implements IRecipeInput},
 *  ctor(ItemStack,int) — реально вызывается CompatIC2.java:155.
 *  Поля/остальные ctor/методы реального API не используются (греп 0) — не добавлены. */
public class RecipeInputItemStack implements IRecipeInput {
	public RecipeInputItemStack(ItemStack aInput, int aAmount) {/**/}
}

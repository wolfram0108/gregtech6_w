package ic2.api.recipe;

import net.minecraft.world.item.ItemStack;

/** F10 ЗЕРКАЛО (compile-only) чужого API IC2. GT6 ВЫЗЫВАЕТ matches(ItemStack) в UT (перебор рецептов), НЕ
 *  реализует -> сигнатура matches под neo ItemStack (реальный ic2.api.recipe.IRecipeInput.matches сверен javap
 *  IC2Classic:1.2.1.8-dev, старый пакет net.minecraft.item.ItemStack ремаплен на neo). getAmount/getInputs реального
 *  API не вызываются -> не добавлены. Реальный мод не грузится, интеграция отложена. См. compat-mirror/README.md. */
public interface IRecipeInput {
	boolean matches(ItemStack stack);
}

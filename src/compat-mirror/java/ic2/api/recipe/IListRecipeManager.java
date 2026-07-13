package ic2.api.recipe;

import net.minecraft.world.item.ItemStack;

/** F10 ЗЕРКАЛО (compile-only) чужого API — НОВЫЙ файл (реальный тип полей Recipes.recyclerBlacklist/
 *  recyclerWhitelist). Сверено javap ic2:IC2Classic:1.2.1.8-dev (ic2.api.recipe.IListRecipeManager).
 *  Реально используются: add(IRecipeInput), contains(ItemStack), isEmpty() — CompatIC2.java:60,61,
 *  72,115,116,141. Метод getInputs() и {@code extends Iterable} реального API нигде не используются
 *  (греп 0 — ни один foreach по blacklist/whitelist) — не добавлены. */
public interface IListRecipeManager {
	void add(IRecipeInput aInput);
	boolean contains(ItemStack aStack);
	boolean isEmpty();
}

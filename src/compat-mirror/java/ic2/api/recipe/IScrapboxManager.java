package ic2.api.recipe;

import net.minecraft.world.item.ItemStack;

/** F10 ЗЕРКАЛО (compile-only) чужого API — НОВЫЙ файл (реальный тип поля Recipes.scrapboxDrops).
 *  Сверено javap ic2:IC2Classic:1.2.1.8-dev (ic2.api.recipe.IScrapboxManager).
 *  Реально используются: getDrop(ItemStack,boolean):ItemStack, addDrop(ItemStack,float) —
 *  CompatIC2.java:59,121,132. Метод getDrops() реального API не используется (греп 0) — не добавлен. */
public interface IScrapboxManager {
	ItemStack getDrop(ItemStack aInput, boolean aRemove);
	void addDrop(ItemStack aStack, float aChance);
}

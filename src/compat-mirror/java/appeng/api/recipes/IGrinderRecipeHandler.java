package appeng.api.recipes;

import java.util.Collection;

import net.minecraft.world.item.ItemStack;

/** F10 ЗЕРКАЛО (compile-only) чужого API — AppliedEnergistics2. НОВЫЙ файл, конец цепочки
 *  {@code AEApi.instance().registries().grinder()...} — GT_API_Post.java:733 (getRecipes().clear());
 *  RM.java:967-969 (addRecipe, 3 оверлоада компилятором вскрыты сверх исходной спеки — греп
 *  {@code grinder().addRecipe} по gregtech6_w/src/main): addRecipe(ItemStack,ItemStack,int),
 *  addRecipe(ItemStack,ItemStack,ItemStack,float,int), addRecipe(ItemStack,ItemStack,ItemStack,
 *  float,ItemStack,float,int). Пакет — вывод из цепочки вызова, реальный jar недоступен для
 *  проверки (build.gradle:218, не разрешён в gradle-кэше). */
public interface IGrinderRecipeHandler {
	Collection<?> getRecipes();
	void addRecipe(ItemStack aInput, ItemStack aOutput, int aTurns);
	void addRecipe(ItemStack aInput, ItemStack aOutput, ItemStack aOutput2, float aChance2, int aTurns);
	void addRecipe(ItemStack aInput, ItemStack aOutput, ItemStack aOutput2, float aChance2, ItemStack aOutput3, float aChance3, int aTurns);
}

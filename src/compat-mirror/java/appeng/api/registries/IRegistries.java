package appeng.api.registries;

import appeng.api.recipes.IGrinderRecipeHandler;

/** F10 ЗЕРКАЛО (compile-only) чужого API — AppliedEnergistics2. НОВЫЙ файл, звено цепочки
 *  {@code AEApi.instance().registries().grinder()...} — GT_API_Post.java:733. Пакет — вывод из
 *  цепочки вызова (см. AEApi.java), реальный jar недоступен для проверки (build.gradle:218,
 *  не разрешён в gradle-кэше). */
public interface IRegistries {
	IGrinderRecipeHandler grinder();
}

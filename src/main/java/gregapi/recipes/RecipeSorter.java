/**
 * Copyright (c) 2020 GregTech-6 Team
 *
 * This file is part of GregTech.
 *
 * GregTech is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * GregTech is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with GregTech. If not, see <http://www.gnu.org/licenses/>.
 */

package gregapi.recipes;

/**
 * F11 ПУСТЫШКА-ПЕРЕХОДНИК — замена Forge {@code net.minecraftforge.oredict.RecipeSorter}, которого в neo НЕТ
 * (см. {@code decisions/F11-crafting-recipe.md} §4.3). Его роль — задавать приоритет GT-рецептов в ванильном
 * списке крафта — в модели диспетчера не нужна: порядок GT6-рецептов задаёт сам GT6 при переборе {@code CR.BUFFER}.
 * Держит {@code GT_API} компилируемым 1:1; вызовы {@code register(...)} — no-op.
 */
public final class RecipeSorter {
	private RecipeSorter() {}

	/** Категория рецепта Forge-сортировщика; в neo эффекта не имеет, сохранена для дословности вызовов GT6. */
	public enum Category {UNKNOWN, SHAPELESS, SHAPED}

	/** No-op: в neo нет сортировщика рецептов; порядок задаёт перебор буфера GT6. */
	public static void register(String aName, Class<?> aRecipeClass, Category aCategory, String aOrdering) {/* no-op (F11) */}
}

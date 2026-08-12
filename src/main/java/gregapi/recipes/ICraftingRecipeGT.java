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
 *
 * Modified in 2026 for the GregTech 6 NeoForge port
 * (https://github.com/wolfram0108/gregtech6_w): ported from Minecraft 1.7.10 / Forge
 * to Minecraft 26.1.2 / NeoForge.
 */

package gregapi.recipes;

import gregapi.util.ST;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.level.Level;

/**
 * @author Gregorius Techneticies
 *
 * F11 (decisions/F11-crafting-recipe.md): СОБСТВЕННЫЙ крафт-контракт GT6 над {@code CraftingContainer}/{@code Level}.
 * НЕ ванильный {@code Recipe}: экземпляры этого контракта живут в собственном буфере GT6 ({@code CR.BUFFER}) и в
 * ванильный верстак пробрасываются ОДНИМ диспетчером-{@code CustomRecipe}, а не регистрируются в
 * {@code RecipeManager} по-отдельности (рантайм-add удалён движком). Логика {@code matches}/{@code getCraftingResult}
 * — 1:1 из 1.7.10, лишь тип сетки {@code InventoryCrafting}→{@code CraftingContainer} (прямой наследник:
 * {@code getWidth}/{@code getHeight}/{@code getItems}, сетка приходит ЦЕЛИКОМ), {@code World}→{@code Level}.
 */
public interface ICraftingRecipeGT {
	/** Used for Recipes as an Error Indicator. */
	public static final ItemStack ERROR_OUTPUT = ST.make(Items.EGG, 0, 0, "Error: Please Report used Ingredients to GregTech!");

	/** @return true, если раскладка сетки собирает этот рецепт. */
	public boolean matches(CraftingContainer aGrid, Level aWorld);
	/** @return результат крафта для данной сетки (с GT6-пост-обработкой: NBT/заряд/зачар/динам. материал). */
	public ItemStack getCraftingResult(CraftingContainer aGrid);
	/** Число входных слотов рецепта (для сортировки/приоритета в GT6). */
	public int getRecipeSize();
	/** Номинальный выход рецепта (для отображения/скана). */
	public ItemStack getRecipeOutput();

	/** this is basically just needed so I don't accidentally remove my own Recipes. */
	public boolean isRemovableByGT();
	/** return false to make GT Autocrafting Tables not produce this Recipe. */
	public boolean isAutocraftableByGT();
}

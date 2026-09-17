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

/** @author Gregorius Techneticies
 *  GT6's own crafting contract, not the vanilla Recipe: instances live in GT6's own buffer and reach the bench through
 *  one dispatcher, since runtime recipe registration is gone; matches/getCraftingResult logic is 1:1 from 1.7.10. */
public interface ICraftingRecipeGT {
	/** Used for Recipes as an Error Indicator. */
	public static final ItemStack ERROR_OUTPUT = ST.make(Items.EGG, 0, 0, "Error: Please Report used Ingredients to GregTech!");

	/** @return true if this grid layout assembles this recipe. */
	public boolean matches(CraftingContainer aGrid, Level aWorld);
	/** @return the crafting result for this grid, with GT6 post-processing (NBT/charge/enchant/dynamic material). */
	public ItemStack getCraftingResult(CraftingContainer aGrid);
	/** Number of input slots this recipe uses, for GT6's own sorting/priority. */
	public int getRecipeSize();
	/** The recipe's nominal output, for display/scanning purposes. */
	public ItemStack getRecipeOutput();

	/** this is basically just needed so I don't accidentally remove my own Recipes. */
	public boolean isRemovableByGT();
	/** return false to make GT Autocrafting Tables not produce this Recipe. */
	public boolean isAutocraftableByGT();
}

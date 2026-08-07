/**
 * Copyright (c) 2026 wolfram0108
 *
 * COMPILE-TIME STAND-IN — NOT THIRD-PARTY CODE.
 *
 * This declaration was written from scratch for the GregTech 6 NeoForge port
 * (https://github.com/wolfram0108/gregtech6_w). It contains no code from the project
 * that owns this package name, and no part of it was copied or decompiled from that
 * project: it declares only the members GregTech 6 itself implements or calls, so that
 * the port compiles while integration with that mod stays deferred.
 *
 * The original package name is kept deliberately, because GregTech 6 implements these
 * types verbatim and the port does not alter the code Gregorius Techneticies wrote.
 * Removing these classes from the build is not possible: 66 classes of the mod extend
 * or implement them, and the JVM requires the type to load the implementing class.
 *
 * All names, trademarks and rights in the project this package belongs to remain with
 * its authors. See src/compat-mirror/README.md and NOTICE.
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

package codechicken.nei.recipe;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.world.item.ItemStack;
import codechicken.nei.PositionedStack;

/** F10 ЗЕРКАЛО (compile-only) чужого API — NEI. Поля/методы, используемые GregTech6
 *  (NEI_RecipeMap extends TemplateRecipeHandler; большинство методов в оригинале не @Override —
 *  маркер-контракт, кроме {@code loadCraftingRecipes(String,Object...)}, реально зовущегося
 *  через {@code super.}). См. compat-mirror/README.md. */
public class TemplateRecipeHandler {
	public List<CachedRecipe> arecipes = new ArrayList<>();
	public List<RecipeTransferRect> transferRects = new ArrayList<>();
	public int cycleticks;

	public TemplateRecipeHandler newInstance() {return null;}

	protected List<PositionedStack> getCycledIngredients(int aCycle, List<PositionedStack> aIngredients) {return aIngredients;}

	public void loadCraftingRecipes(String aOutputId, Object... aResults) {}
	public void loadCraftingRecipes(ItemStack aResult) {}
	public void loadUsageRecipes(ItemStack aInput) {}
	public String getOverlayIdentifier() {return null;}
	public void drawBackground(int aRecipe) {}
	public int recipiesPerPage() {return 1;}
	public String getRecipeName() {return null;}
	public String getGuiTexture() {return null;}
	public List<String> handleItemTooltip(GuiRecipe aGui, ItemStack aStack, List<String> aCurrentTip, int aRecipeIndex) {return aCurrentTip;}
	public void drawExtras(int aRecipeIndex) {}
}

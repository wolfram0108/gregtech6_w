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

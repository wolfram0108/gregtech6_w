package codechicken.nei.recipe;

import java.util.ArrayList;
import java.util.List;

/** F10 ЗЕРКАЛО (compile-only) чужого API — NEI. Минимум для RecipeMap.openNEI/guiRecipes,
 *  NEI_RecipeMap.init/NEI_GT_API_Config, GT_RectHandler.transferRect (возврат — boolean,
 *  используется как условие). См. compat-mirror/README.md. */
public class GuiCraftingRecipe {
    public static List<TemplateRecipeHandler> craftinghandlers = new ArrayList<>();

    public static boolean openRecipeGui(String aRecipeName, Object... aOutputs) {return false;}
}

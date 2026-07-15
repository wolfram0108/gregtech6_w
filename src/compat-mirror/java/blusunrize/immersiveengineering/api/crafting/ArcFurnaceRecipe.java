package blusunrize.immersiveengineering.api.crafting;

/** F10 ЗЕРКАЛО (compile-only) чужого API. Минимум для компиляции ядра; члены добираются
 *  компилятором. Реальная зависимость — при возврате к интеграции. См. compat-mirror/README.md. */
public class ArcFurnaceRecipe {
	public static java.util.List<ArcFurnaceRecipe> recipeList = new java.util.ArrayList<>();
	public static ArcFurnaceRecipe addRecipe(net.minecraft.world.item.ItemStack aOutput, Object aInput, net.minecraft.world.item.ItemStack aSlag, int aTime, int aEnergyPerTick, Object... aAdditives) {return new ArcFurnaceRecipe();}
	public ArcFurnaceRecipe setSpecialRecipeType(String aType) {return this;}
}

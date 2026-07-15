package blusunrize.immersiveengineering.api.crafting;

/** F10 ЗЕРКАЛО (compile-only) чужого API. Минимум для компиляции ядра; члены добираются
 *  компилятором. Реальная зависимость — при возврате к интеграции. См. compat-mirror/README.md. */
public class CrusherRecipe {
	public static java.util.List<CrusherRecipe> recipeList = new java.util.ArrayList<>();
	public static CrusherRecipe addRecipe(net.minecraft.world.item.ItemStack aOutput, Object aInput, int aEnergy) {return new CrusherRecipe();}
}

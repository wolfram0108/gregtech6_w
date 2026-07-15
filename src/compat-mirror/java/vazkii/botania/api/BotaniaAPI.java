package vazkii.botania.api;

/** F10 ЗЕРКАЛО (compile-only) чужого API. Минимум для компиляции ядра; члены добираются
 *  компилятором. Реальная зависимость — при возврате к интеграции. См. compat-mirror/README.md. */
public class BotaniaAPI {
    /** Реально используется GregTech (Compat_Recipes_Botania: registerManaInfusionRecipe(output, inputOreDict, mana)). */
    public static Object registerManaInfusionRecipe(net.minecraft.world.item.ItemStack aOutput, String aInput, int aMana) {return null;}
}

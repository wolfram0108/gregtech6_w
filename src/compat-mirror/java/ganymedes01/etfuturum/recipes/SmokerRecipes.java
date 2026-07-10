package ganymedes01.etfuturum.recipes;

import net.minecraft.world.item.ItemStack;

/** F10 ЗЕРКАЛО (compile-only) чужого API — Et Futurum. Минимум для вызовов RM/GT_API_Proxy. */
public class SmokerRecipes {
    public SmeltingBlacklist smeltingBlacklist;

    public static SmokerRecipes smelting() {
        return null;
    }

    public void addRecipe(ItemStack aInput, ItemStack aOutput, float aExperience) {}

    public void removeRecipe(ItemStack aInput) {}

    public ItemStack getSmeltingResult(ItemStack aInput) {
        return null;
    }

    public static class SmeltingBlacklist {
        public void add(ItemStack aInput) {}
    }
}

package squeek.applecore.api.food;

import net.minecraft.world.item.ItemStack;

/** F10 ЗЕРКАЛО (compile-only) чужого API — AppleCore. Метод, который переопределяет GT6. */
public interface IEdible {
    FoodValues getFoodValues(ItemStack aStack);
}

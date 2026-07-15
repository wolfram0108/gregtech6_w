package forestry.api.recipes;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;

/** Forestry-mirror (compile-only; настоящий Forestry предоставляет реализацию в рантайме,
 *  вызов гейтится {@code MD.FR.mLoaded}). */
public interface ISqueezerRecipe {
	ItemStack[] getResources();
	ItemStack getRemnants();
	float getRemnantsChance();
	FluidStack getFluidOutput();
	int getProcessingTime();
}

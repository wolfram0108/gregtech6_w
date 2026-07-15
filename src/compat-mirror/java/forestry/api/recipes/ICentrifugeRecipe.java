package forestry.api.recipes;

import java.util.Map;
import net.minecraft.world.item.ItemStack;

/** Forestry-mirror (compile-only; настоящий Forestry предоставляет реализацию в рантайме,
 *  вызов гейтится {@code MD.FR.mLoaded} — тело здесь никогда не исполняется). */
public interface ICentrifugeRecipe {
	ItemStack getInput();
	Map<ItemStack, Float> getAllProducts();
	int getProcessingTime();
}

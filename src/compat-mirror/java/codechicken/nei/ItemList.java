package codechicken.nei;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** F10 ЗЕРКАЛО (compile-only) чужого API — NEI. Только поле, используемое GregTech6
 *  (NEI_RecipeMap.FixedPositionedStack.generatePermutations: ItemList.itemMap.get(Item)).
 *  См. compat-mirror/README.md. */
public class ItemList {
	public static Map<Item, List<ItemStack>> itemMap = new LinkedHashMap<>();
}

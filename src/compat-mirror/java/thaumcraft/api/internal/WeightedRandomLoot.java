package thaumcraft.api.internal;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.world.item.ItemStack;

/** F10 ЗЕРКАЛО (compile-only) чужого API Thaumcraft. Только объявления, используемые GregTech6
 *  (CompatTC.onServerStarting: перебор lootBagCommon, чтение .item). См. compat-mirror/README.md. */
public class WeightedRandomLoot {
	public ItemStack item;

	public static List<WeightedRandomLoot> lootBagCommon = new ArrayList<>();
}

package forestry.api.storage;

import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.world.item.ItemStack;

/** F10 ЗЕРКАЛО (compile-only) чужого API — Forestry. Только статика, используемая
 *  GregTech6 (CompatFR.addToBackpacks: definitions.get(aType).addValidItem(aStack)),
 *  вызов уже обёрнут в try/catch(Throwable) в оригинале. См. compat-mirror/README.md. */
public class BackpackManager {
	public static Map<String, Definition> definitions = new LinkedHashMap<>();

	public static class Definition {
		public void addValidItem(ItemStack aStack) {}
	}
}

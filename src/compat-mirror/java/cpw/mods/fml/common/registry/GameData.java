package cpw.mods.fml.common.registry;

import java.util.Collections;

import net.minecraft.world.item.Item;

/** F10 ЗЕРКАЛО (compile-only) — legacy Forge/FML 1.7.10 (пакет cpw.mods.fml не существует
 *  на neo-classpath). Только используемое GregTech6 (Behavior_Unlock_Item_Aspects:
 *  GameData.getItemRegistry().iterator()). См. compat-mirror/README.md. */
public class GameData {
	public static Iterable<Item> getItemRegistry() {return Collections.emptyList();}
}

package ic2.core;

import net.minecraft.world.item.ItemStack;

/** F10 ЗЕРКАЛО (compile-only) чужого API. Сверено javap ic2:IC2Classic:1.2.1.8-dev (ic2.core.Ic2Items,
 *  850+ статических полей ItemStack). Реально используется одно — CompatIC2.java:82: coffeeBeans
 *  (записываемое, static, ItemStack). Остальные поля реального Ic2Items в GT6-исходнике не
 *  используются (греп 0) — не добавлены. */
public class Ic2Items {
	public static ItemStack coffeeBeans;
}

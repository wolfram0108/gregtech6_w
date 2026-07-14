package squeek.applecore.api.food;

import net.minecraft.world.item.Item;

/** F10 ЗЕРКАЛО (compile-only) чужого API — AppleCore. GT6 конструирует this через reflection
 *  (UT.Reflection.callConstructor, конструктор #0, единственный аргумент — свой Item), оборачивая
 *  свой предмет под AppleCore-мост. Единственный сайт-потребитель, что раньше кастовал результат в
 *  1.7.10 ItemFood и звал FoodStats.addStats(ItemFood,ItemStack) (SRG func_151686_a) — этого метода в
 *  neo FoodData нет вовсе (компонентная FoodProperties-модель), см. отложенную метку F10 (AppleCore
 *  ItemFoodProxy addStats) в gregapi/item/multiitem/MultiItemRandom.java и
 *  gregapi/tileentity/tank/TileEntityBase08FluidContainer.java.
 *  Мост оставлен минимальным (под сигнатуру reflection-конструктора), на будущее, когда AppleCore будет
 *  реально портирован — decisions/F10-external-mod-compat.md. */
public class ItemFoodProxy {
	public final Item item;

	public ItemFoodProxy(Item aItem) {
		item = aItem;
	}
}

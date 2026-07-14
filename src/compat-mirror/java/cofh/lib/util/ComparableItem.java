package cofh.lib.util;

import net.minecraft.world.item.Item;

/** F10 ЗЕРКАЛО (compile-only) чужого API. Минимум для компиляции ядра; члены добираются
 *  компилятором. Реальная зависимость — при возврате к интеграции. См. compat-mirror/README.md.
 *  GT6 (GT_API_Proxy) кастует объекты из чужих Map/Set к ComparableItem и читает/пишет поля
 *  {@code item}/{@code metadata} — зеркалим их публичными полями (типы выведены из ST.item_:172=Item,
 *  ST.meta_→int). Конструктор GT6 не использует (кастует уже существующие объекты). */
public class ComparableItem {
	public Item item;
	public int  metadata;
}

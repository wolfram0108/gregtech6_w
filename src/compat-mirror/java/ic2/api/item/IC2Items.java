package ic2.api.item;

import net.minecraft.world.item.ItemStack;

/** F10 ЗЕРКАЛО (compile-only) чужого API IC2. GT6 зовёт статический IC2Items.getItem(String). Реальный мод
 *  не грузится (guard MD.IC2.mLoaded) -> возвращаем null = штатный путь «IC2 не установлен» (вызыватель ST.mkic
 *  уже обрабатывает null, печатает предупреждение). Честная отложенность F10, не тихий стаб (neo-пути к IC2 нет —
 *  мод не портирован). См. compat-mirror/README.md. */
public interface IC2Items {
	static ItemStack getItem(String name) {return null;}
}

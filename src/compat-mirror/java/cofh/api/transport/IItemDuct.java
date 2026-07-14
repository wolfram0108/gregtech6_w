package cofh.api.transport;

import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;

/** F10 ЗЕРКАЛО (compile-only) чужого API CoFH. GT6 ВЫЗЫВАЕТ (кастует внешний TE), НЕ реализует —
 *  сигнатура под neo-тип сайта вызова в ST.java (getForgeSideOfTileEntity()->Direction). Реальный мод не
 *  грузится в рантайме, интеграция отложена. См. compat-mirror/README.md. */
public interface IItemDuct {
	ItemStack insertItem(Direction from, ItemStack item);
}

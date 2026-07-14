package buildcraft.api.transport;

import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;

/** F10 ЗЕРКАЛО (compile-only) чужого API BuildCraft. GT6 ВЫЗЫВАЕТ этот интерфейс (кастует внешний TE),
 *  НЕ реализует — сигнатуры под neo-типы сайтов вызова в ST.java (getForgeSideOfTileEntity()->Direction;
 *  оригинальный ForgeDirection/EnumColor удалены движком, цвет=Object т.к. всегда null). Реальный мод не
 *  грузится в рантайме (guard BC_PIPES), интеграция отложена. См. compat-mirror/README.md. */
public interface IInjectable {
	boolean canInjectItems(Direction from);
	int injectItem(ItemStack stack, boolean doAdd, Direction from, Object color);
}

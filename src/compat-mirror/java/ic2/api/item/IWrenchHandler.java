package ic2.api.item;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** F10 ЗЕРКАЛО (compile-only) чужого API. Сверено javap ic2:IC2Classic:1.2.1.8-dev
 *  (ic2.api.item.IWrenchHandler, оригинал EntityPlayer 1.7.10 → neo Player, как во всём порту).
 *  Реализуется CompatIC2C.java:32,38-40 (3 метода реального интерфейса, точное соответствие). */
public interface IWrenchHandler {
	boolean supportsItem(ItemStack aWrench);
	boolean canWrench(ItemStack aWrench, int aX, int aY, int aZ, Player aPlayer);
	void useWrench(ItemStack aWrench, int aX, int aY, int aZ, Player aPlayer);
}

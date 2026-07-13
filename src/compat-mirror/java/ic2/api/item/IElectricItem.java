package ic2.api.item;

import net.minecraft.world.item.ItemStack;

/** F10 ЗЕРКАЛО (compile-only) чужого API. Сверено javap ic2:IC2Classic:1.2.1.8-dev
 *  (ic2.api.item.IElectricItem). Реально используются (CompatIC2EUItem.java:39,53,66,68,69,70,72):
 *  getTier(ItemStack):int, canProvideEnergy(ItemStack):boolean, getMaxCharge(ItemStack):double.
 *  Методы getChargedItem/getEmptyItem/getTransferLimit реального API нигде не вызываются
 *  (греп 0) — не добавлены. */
public interface IElectricItem {
	int getTier(ItemStack aStack);
	boolean canProvideEnergy(ItemStack aStack);
	double getMaxCharge(ItemStack aStack);
}

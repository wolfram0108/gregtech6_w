package ic2.api.item;

import net.minecraft.world.item.ItemStack;

/** F10 ЗЕРКАЛО (compile-only) чужого API. Сверено javap ic2:IC2Classic:1.2.1.8-dev
 *  (ic2.api.item.IElectricItemManager). Реально используются на {@code ElectricItem.manager}
 *  (CompatIC2EUItem.java:43,57,64,65,71): charge(ItemStack,double,int,boolean,boolean):double,
 *  discharge(ItemStack,double,int,boolean,boolean,boolean):double — точное соответствие
 *  реальному API (long-аргументы вызывающей стороны неявно расширяются до double).
 *  Методы getCharge/canUse/use/chargeFromArmor/getToolTip реального API не используются
 *  (греп 0) — не добавлены. */
public interface IElectricItemManager {
	double charge(ItemStack aStack, double aAmount, int aTier, boolean aIgnoreTransferLimit, boolean aSimulate);
	double discharge(ItemStack aStack, double aAmount, int aTier, boolean aIgnoreTransferLimit, boolean aSimulate, boolean aExternally);
}

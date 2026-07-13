package ic2.api.energy;

/** F10 ЗЕРКАЛО (compile-only) чужого API. Было {@code class EnergyNet {}} — сломано:
 *  {@code EnergyNet.instance} вызывается как объект-менеджер (.getTileEntity(...)) —
 *  EnergyCompat.java:119,226. Сверено javap ic2:IC2Classic:1.2.1.8-dev (ic2.api.energy.EnergyNet):
 *  {@code public static IEnergyNet instance}. */
public final class EnergyNet {
	public static IEnergyNet instance;
}

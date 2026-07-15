package buildcraft.api.power;

/** F10 ЗЕРКАЛО (compile-only) чужого API. Минимум для компиляции ядра; члены добираются
 *  компилятором. Реальная зависимость — при возврате к интеграции. См. compat-mirror/README.md. */
public interface ILaserTarget {
	boolean requiresLaserEnergy();
	void receiveLaserEnergy(int aEnergy);
}

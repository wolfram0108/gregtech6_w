package ic2.api.energy;

/** F10 ЗЕРКАЛО (compile-only) чужого API. Минимум для компиляции ядра; члены добираются
 *  компилятором. Реальная зависимость — при возврате к интеграции. См. compat-mirror/README.md. */
public class NodeStats {
	/** IC2-mirror (compile-only; настоящий IC2 предоставляет реализацию в рантайме). */
	public double getEnergyIn() {return 0;}
	public double getEnergyOut() {return 0;}
	public double getStorage() {return 0;}
}

package micdoodle8.mods.galacticraft.core.energy;

/** F10 ЗЕРКАЛО (compile-only) чужого API. Минимум для компиляции ядра; члены добираются
 *  компилятором. Реальная зависимость — при возврате к интеграции. См. compat-mirror/README.md. */
public interface EnergyConfigHandler {
	/** Galacticraft EU-конверсия. Compile-only (dead-path MD.GC.mLoaded); value 1 (non-zero, без div-by-zero даже если достигнут). PORT-TODO(F10): реальное значение при интеграции GC. */
	float IC2_RATIO = 1;
}

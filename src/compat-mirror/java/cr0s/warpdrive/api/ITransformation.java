package cr0s.warpdrive.api;

/** F10 ЗЕРКАЛО (compile-only) чужого API WarpDrive. GT6 ВЫЗЫВАЕТ getRotationSteps в CompatWD.rotate, НЕ реализует.
 *  Реальный мод не грузится, интеграция отложена. См. compat-mirror/README.md. */
public interface ITransformation {
	int getRotationSteps();
}

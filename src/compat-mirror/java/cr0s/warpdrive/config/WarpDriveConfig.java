package cr0s.warpdrive.config;

/** F10 ЗЕРКАЛО (compile-only) чужого API WarpDrive. GT6 зовёт статический registerBlockTransformer в CompatWD-ctor;
 *  реальный мод не грузится (guard MD.WD.mLoaded, CompatWD активируется только при загруженном WarpDrive) -> no-op.
 *  Интеграция отложена. См. compat-mirror/README.md. */
public class WarpDriveConfig {
	public static void registerBlockTransformer(String modId, cr0s.warpdrive.api.IBlockTransformer transformer) {}
}

package codechicken.nei.api;

/** F10 ЗЕРКАЛО (compile-only) чужого API — NEI. Контракт, реализуемый NEI_GT_API_Config
 *  (loadConfig не @Override в оригинале, но getName/getVersion — часть реального интерфейса).
 *  См. compat-mirror/README.md. */
public interface IConfigureNEI {
	void loadConfig();
	String getName();
	String getVersion();
}

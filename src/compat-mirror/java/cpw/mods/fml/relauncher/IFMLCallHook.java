package cpw.mods.fml.relauncher;

import java.util.Map;

/** F10 ЗЕРКАЛО (compile-only) — legacy Forge/FML 1.7.10 (пакет cpw.mods.fml не существует
 *  на neo-classpath). Только используемое GregTech6 (GT_ASM.Setup implements IFMLCallHook).
 *  См. compat-mirror/README.md. */
public interface IFMLCallHook {
	void injectData(Map<String, Object> aData);
	Void call() throws Exception;
}

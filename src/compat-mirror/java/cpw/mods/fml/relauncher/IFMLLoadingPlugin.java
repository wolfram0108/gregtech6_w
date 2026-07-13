package cpw.mods.fml.relauncher;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Map;

/** F10 ЗЕРКАЛО (compile-only) — legacy Forge/FML 1.7.10 coremod-контракт (пакет cpw.mods.fml
 *  не существует на neo-classpath; ASM-coremod-система в neo — иная модель, отдельный шов вне
 *  зоны F10). Только используемое GregTech6 (GT_ASM implements IFMLLoadingPlugin).
 *  См. compat-mirror/README.md. */
public interface IFMLLoadingPlugin {
	String[] getASMTransformerClass();
	String getModContainerClass();
	String getSetupClass();
	void injectData(Map<String, Object> data);
	String getAccessTransformerClass();

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.TYPE)
	@interface MCVersion {String value();}

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.TYPE)
	@interface Name {String value();}

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.TYPE)
	@interface SortingIndex {int value();}

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.TYPE)
	@interface TransformerExclusions {String[] value();}
}

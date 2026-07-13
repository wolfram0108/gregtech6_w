package cpw.mods.fml.common;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** F10 ЗЕРКАЛО (compile-only) — legacy Forge/FML 1.7.10 (пакет cpw.mods.fml не существует
 *  на neo-classpath). {@code @Mod(...)} сам по себе закомментирован в Example_Mod.java (не
 *  компилируется), но вложенная {@code @Mod.EventHandler} реально используется 7 раз.
 *  См. compat-mirror/README.md. */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Mod {
	String modid() default "";
	String name() default "";
	String version() default "";
	String dependencies() default "";

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.METHOD)
	@interface EventHandler {}
}

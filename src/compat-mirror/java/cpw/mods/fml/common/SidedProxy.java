package cpw.mods.fml.common;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** F10 ЗЕРКАЛО (compile-only) — legacy Forge/FML 1.7.10 (пакет cpw.mods.fml не существует
 *  на neo-classpath). Только используемое GregTech6 (Example_Mod: @SidedProxy(modId=...,
 *  clientSide=..., serverSide=...) на поле PROXY). См. compat-mirror/README.md. */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface SidedProxy {
	String modId() default "";
	String clientSide() default "";
	String serverSide() default "";
}

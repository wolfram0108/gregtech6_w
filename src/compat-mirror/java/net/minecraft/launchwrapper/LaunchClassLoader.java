package net.minecraft.launchwrapper;

/**
 * F2 compile-only shim. 1.7.10 FML launchwrapper (пакет {@code net.minecraft.launchwrapper} — старый FML,
 * НЕ Mojang, отсутствует на neo-classpath целиком, 0 хитов во всех 3 корнях референса) — см. javadoc
 * {@link IClassTransformer} (тот же F2-шов, ADR {@code decisions/F2-coremod-mixin.md}). {@code extends
 * ClassLoader} — обязателен для узкого каста {@code (LaunchClassLoader)Thread.currentThread().
 * getContextClassLoader()} в {@code GT_ASM.injectData} (JLS 5.5 — narrowing reference conversion между
 * классами требует subtype-отношения). {@link #registerTransformer(String)} — единственный вызываемый
 * GT6-код метод; в рантайме этот путь недостижим (GT_ASM не регистрируется как coremod, ADR §6), поэтому
 * тело честно no-op, а не имитирует реальную ASM-регистрацию.
 */
public class LaunchClassLoader extends ClassLoader {
	public void registerTransformer(String transformerClassName) {
	}
}

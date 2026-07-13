package net.minecraft.launchwrapper;

/**
 * F2 compile-only shim. 1.7.10 FML launchwrapper (пакет {@code net.minecraft.launchwrapper} — старый FML,
 * НЕ Mojang, отсутствует на neo-classpath целиком, 0 хитов во всех 3 корнях референса neo/neoforge/fml) —
 * NeoForge использует Mixin+Access Transformer вместо ручного ASM-coremod (ADR
 * {@code decisions/F2-coremod-mixin.md} §1-3). Зеркалирован ТОЛЬКО контракт, нужный {@code gregtech.asm.*}
 * (GT_ASM + ~14 IClassTransformer-реализаций) для КОМПИЛЯЦИИ; сам coremod-механизм в рантайме НЕ активен
 * (GT_ASM больше никем не регистрируется — реальная замена на Mixin отложена, PORT-TODO(F2, mixin-cycle),
 * ADR §6 «Этап 9»). Логика трансформеров (байткод-патчи) СОХРАНЕНА нетронутой для будущего Mixin-порта.
 */
public interface IClassTransformer {
	byte[] transform(String name, String transformedName, byte[] basicClass);
}

package appeng.api;

import appeng.api.registries.IRegistries;

/** F10 ЗЕРКАЛО (compile-only) чужого API — AppliedEnergistics2. Было {@code interface AEApi {}} —
 *  сломано: GT_API_Post.java:733 делает {@code AEApi.instance().registries().grinder().getRecipes().clear()}
 *  (цепочка вызовов, статика + 2 инстанс-метода). Пакеты {@code appeng.api.registries}/
 *  {@code appeng.api.recipes} у реального AE2 rv3-beta-6 (build.gradle:218, jar не разрешён в
 *  gradle-кэше — недоступен для javap) НЕ подтверждены грепом по трём каноничным корням
 *  (не neo/neoforge/fml API) — размещение по аналогии с реальной структурой AE2
 *  (api/registries/*, api/recipes/*), помечено как вывод из цепочки вызова, не факт. */
public final class AEApi {
	public static AEApi instance() {return null;}
	public IRegistries registries() {return null;}
}

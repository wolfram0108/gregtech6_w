package ic2.core;

import ic2.api.recipe.IRecipeInput;

/** F10 ЗЕРКАЛО (compile-only) чужого API. Сверено javap ic2:IC2Classic:1.2.1.8-dev (ic2.core.IC2).
 *  Реально используется один статический оверлоад из 6 — CompatIC2.java:103:
 *  addValuableOre(IRecipeInput,int):void. Остальные оверлоады/сотни статических полей реального
 *  IC2 (валюта конфигов, кэши тиков, версии…) в GT6-исходнике не используются (греп 0) —
 *  не добавлены. */
public class IC2 {
	public static void addValuableOre(IRecipeInput aInput, int aValue) {/**/}
}

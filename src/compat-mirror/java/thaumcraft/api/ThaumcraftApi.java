package thaumcraft.api;

/** F10 ЗЕРКАЛО (compile-only) чужого API. Минимум для компиляции ядра; члены добираются
 *  компилятором. Реальная зависимость — при возврате к интеграции. См. compat-mirror/README.md. */
public interface ThaumcraftApi {
	/** Добрано компилятором (gregapi/GT_API.java: {@code ThaumcraftApi.objectTags.isEmpty()},
	 *  MD.TC.mLoaded-гейт) — пустая заглушка-карта, реальный тип/содержимое из внешнего мода
	 *  вне 3 корней референса, недостижимо до интеграции Thaumcraft. */
	java.util.Map<String, Object> objectTags = new java.util.HashMap<>();
}

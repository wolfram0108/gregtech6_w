package ic2.api.info;

/** F10 ЗЕРКАЛО (compile-only) чужого API. Минимум для компиляции ядра; члены добираются
 *  компилятором. Реальная зависимость — при возврате к интеграции. См. compat-mirror/README.md.
 *  Поле добрано компилятором (gregapi/GT_API.java: {@code Info.POTION_RADIATION.id}, MD.IC2.mLoaded-гейт)
 *  — тот же PotionRef-паттерн, что enviromine/EnviroPotion.java (id-заглушка внешнего Potion). */
public class Info {
	public static PotionRef POTION_RADIATION;

	public static class PotionRef {
		public int id;
	}
}

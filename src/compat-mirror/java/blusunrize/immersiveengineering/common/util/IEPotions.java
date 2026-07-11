package blusunrize.immersiveengineering.common.util;

/** F10 ЗЕРКАЛО (compile-only) чужого API. Минимум для компиляции ядра; члены добираются
 *  компилятором. Реальная зависимость — при возврате к интеграции. См. compat-mirror/README.md.
 *  Поля добраны компилятором (gregapi/GT_API.java: {@code IEPotions.flammable/slippery/conductive/
 *  sticky.id}, MD.IE.mLoaded-гейт) — тот же PotionRef-паттерн, что enviromine/EnviroPotion.java. */
public class IEPotions {
	public static PotionRef flammable;
	public static PotionRef slippery;
	public static PotionRef conductive;
	public static PotionRef sticky;

	public static class PotionRef {
		public int id;
	}
}

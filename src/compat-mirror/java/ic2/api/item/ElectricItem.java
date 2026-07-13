package ic2.api.item;

/** F10 ЗЕРКАЛО (compile-only) чужого API. Было {@code class ElectricItem {}} — сломано:
 *  CompatIC2EUItem.java обращается к статическому полю {@code manager} (null-проверка +
 *  .charge()/.discharge()) — CompatIC2EUItem.java:38,43,52,57,64,65,71. Сверено javap
 *  ic2:IC2Classic:1.2.1.8-dev (ic2.api.item.ElectricItem): {@code public static IElectricItemManager manager}.
 *  Поле rawManager и метод registerBackupManager реального API нигде не используются (греп 0) —
 *  не добавлены. */
public final class ElectricItem {
	public static IElectricItemManager manager;
}

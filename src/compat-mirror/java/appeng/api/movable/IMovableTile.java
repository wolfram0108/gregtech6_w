package appeng.api.movable;

/**
 * F10 ЗЕРКАЛО (compile-only) чужого API — Applied Energistics 2.
 * См. {@code src/compat-mirror/README.md} и {@code decisions/F10-external-mod-compat.md} §3.2.
 *
 * <p>Минимальная декларация: только методы, которые GregTech6 переопределяет
 * ({@code TileEntityBase01Root#prepareToMove}, {@code #doneMoving}). НЕ полный API AE2 —
 * ровно столько, сколько нужно ядру для компиляции. Реальная зависимость подключается при
 * возврате к интеграции AE2 (тогда этот файл удаляется).</p>
 */
public interface IMovableTile {
	boolean prepareToMove();
	void doneMoving();
}

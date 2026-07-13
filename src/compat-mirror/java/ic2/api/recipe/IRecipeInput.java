package ic2.api.recipe;

/** F10 ЗЕРКАЛО (compile-only) чужого API. Минимум для компиляции ядра; члены добираются
 *  компилятором. Реальная зависимость — при возврате к интеграции. См. compat-mirror/README.md.
 *  Маркер-интерфейс: методы {@code matches/getAmount/getInputs} реального IC2 (сверено javap
 *  ic2:IC2Classic:1.2.1.8-dev, ic2.api.recipe.IRecipeInput) нигде в GT6-исходнике не вызываются
 *  (греп 0 — только приведение типа/аргумент {@code IListRecipeManager.add|contains}), не добавлены. */
public interface IRecipeInput {}

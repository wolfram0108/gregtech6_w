package squeek.applecore.api.food;

/** F10 ЗЕРКАЛО (compile-only) чужого API — AppleCore. GT6 конструирует this под IEdible.getFoodValues()
 *  (см. IEdible.java) с сигнатурой (int hunger, float saturationModifier) — минимум под сайты вызова
 *  (gregapi/block/multitileentity/IMultiTileEntity.java IMTE_GetFoodValues-реализации,
 *  gregapi/item/multiitem/MultiItemRandomWithCompat.java, gregtech/tileentity/food/MultiTileEntitySandwich.java). */
public class FoodValues {
	public final int hunger;
	public final float saturationModifier;

	public FoodValues(int aHunger, float aSaturationModifier) {
		hunger = aHunger;
		saturationModifier = aSaturationModifier;
	}
}

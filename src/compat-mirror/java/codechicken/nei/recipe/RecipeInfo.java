package codechicken.nei.recipe;

/** F10 ЗЕРКАЛО (compile-only) чужого API — NEI. Только статика, используемая GT_RectHandler
 *  (GuiContainer-тип параметра — 1.7.10-остаток, вне зоны F10; здесь Object, вызывающая сторона
 *  уже блокирована отсутствующим {@code net.minecraft.client.gui.inventory.GuiContainer}).
 *  См. compat-mirror/README.md. */
public class RecipeInfo {
	public static int[] getGuiOffset(Object aGui) {return new int[] {0, 0};}
}

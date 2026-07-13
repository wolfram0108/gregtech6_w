package codechicken.nei;

import net.minecraft.world.item.ItemStack;

/** F10 ЗЕРКАЛО (compile-only) чужого API — NEI. Поля/методы, используемые GregTech6
 *  (NEI_RecipeMap.FixedPositionedStack extends PositionedStack, GuiDraw-подсказки).
 *  См. compat-mirror/README.md. */
public class PositionedStack {
	public int relx, rely;
	/** Текущий отображаемый представитель (может совпадать с одним из {@link #items}). */
	public ItemStack item;
	/** Полный набор перестановок (wildcard-мета разворачивается сюда). */
	public ItemStack[] items = new ItemStack[0];

	public PositionedStack(Object aItem, int aX, int aY) {this(aItem, aX, aY, false);}

	public PositionedStack(Object aItem, int aX, int aY, boolean aPermutated) {
		relx = aX;
		rely = aY;
	}

	public void generatePermutations() {}

	public void setPermutationToRender(int aIndex) {}
}

package codechicken.lib.gui;

import java.awt.Point;

/** F10 ЗЕРКАЛО (compile-only) чужого API — CodeChickenLib. Только статика, используемая
 *  GregTech6 (NEI_RecipeMap: drawBackground/GT_RectHandler). См. compat-mirror/README.md. */
public class GuiDraw {
	public static void changeTexture(String aTexture) {}
	public static void drawTexturedModalRect(int aX, int aY, int aU, int aV, int aWidth, int aHeight) {}
	public static Point getMousePosition() {return new Point(0, 0);}
}

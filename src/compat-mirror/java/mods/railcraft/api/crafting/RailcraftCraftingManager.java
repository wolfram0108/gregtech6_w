package mods.railcraft.api.crafting;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;

/** F10 ЗЕРКАЛО (compile-only) чужого API — Railcraft. Было {@code class RailcraftCraftingManager {}} —
 *  сломано: компилятор вскрыл сверх исходной спеки (греп {@code RailcraftCraftingManager\.} по
 *  gregtech6_w/src/main) 4 статических поля-менеджера — GT_API_Proxy.java:339-342, RM.java:1002:
 *  blastFurnace/cokeOven — .getRecipes() перебирается foreach как {@code Object} (реальный элемент
 *  читается рефлексией по полю "output" — UT.Reflection.getFieldContent), сюда достаточно
 *  {@code Iterable<Object>}; rockCrusher — .getRecipes() аналогично Object, плюс
 *  .createNewRecipe(ItemStack,boolean,boolean):IRockCrusherRecipe; rollingMachine —
 *  .getRecipeList():Iterable&lt;Recipe&gt; (реальный тип потребителя — vanilla Recipe, дальнейший
 *  {@code tRecipe.getRecipeOutput()} — legacy-API вне compat-mirror, не наш охват F10). */
public class RailcraftCraftingManager {
	public static final Manager blastFurnace = new Manager();
	public static final Manager cokeOven = new Manager();
	public static final RockCrusher rockCrusher = new RockCrusher();
	public static final RollingMachine rollingMachine = new RollingMachine();

	public static class Manager {
		public Iterable<Object> getRecipes() {return null;}
	}

	public static class RockCrusher {
		public Iterable<Object> getRecipes() {return null;}
		public IRockCrusherRecipe createNewRecipe(ItemStack aInput, boolean aUseOreDict, boolean aOverwrite) {return null;}
	}

	@SuppressWarnings("rawtypes")
	public static class RollingMachine {
		// Raw Recipe (не Recipe<?>) — потребитель GT_API_Proxy.java:342 объявляет raw {@code Recipe tRecipe}.
		public Iterable<Recipe> getRecipeList() {return null;}
	}
}

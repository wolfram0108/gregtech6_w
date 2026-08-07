/**
 * Copyright (c) 2026 wolfram0108
 *
 * COMPILE-TIME STAND-IN — NOT THIRD-PARTY CODE.
 *
 * This declaration was written from scratch for the GregTech 6 NeoForge port
 * (https://github.com/wolfram0108/gregtech6_w). It contains no code from the project
 * that owns this package name, and no part of it was copied or decompiled from that
 * project: it declares only the members GregTech 6 itself implements or calls, so that
 * the port compiles while integration with that mod stays deferred.
 *
 * The original package name is kept deliberately, because GregTech 6 implements these
 * types verbatim and the port does not alter the code Gregorius Techneticies wrote.
 * Removing these classes from the build is not possible: 66 classes of the mod extend
 * or implement them, and the JVM requires the type to load the implementing class.
 *
 * All names, trademarks and rights in the project this package belongs to remain with
 * its authors. See src/compat-mirror/README.md and NOTICE.
 *
 * This file is part of GregTech.
 *
 * GregTech is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * GregTech is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with GregTech. If not, see <http://www.gnu.org/licenses/>.
 */

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

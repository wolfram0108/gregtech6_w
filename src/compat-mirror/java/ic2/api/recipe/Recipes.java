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

package ic2.api.recipe;

/** F10 ЗЕРКАЛО (compile-only) чужого API. Было {@code class Recipes {}} — сломано: 13 полей
 *  вызываются как объекты-менеджеры (.getRecipes()/.add()/.contains()/.getDrop()/.addDrop()),
 *  Object бы не прошёл компиляцию. Типы сверены javap ic2:IC2Classic:1.2.1.8-dev
 *  (ic2.api.recipe.Recipes) — точное соответствие реальному API.
 *  Греп полей по всему gregtech6_w/src/main (не только из спеки — добраны oreWashing/cannerBottle/
 *  metalformerCutting/metalformerRolling/matterAmplifier, использованные в GT_API_Proxy.java:326-335
 *  и RM.java, но отсутствовавшие в исходном инвентаре): scrapboxDrops, recyclerBlacklist,
 *  recyclerWhitelist, compressor, extractor, macerator, centrifuge, metalformerExtruding, oreWashing,
 *  cannerBottle, metalformerCutting, metalformerRolling, matterAmplifier.
 *  Остальные поля реального Recipes (blockcutter, blastfurance, recycler, advRecipes,
 *  semiFluidGenerator, FluidHeatGenerator, liquidCooldownManager, liquidHeatupManager,
 *  cannerEnrich) в GT6-исходнике не используются (греп 0) — не добавлены. */
public class Recipes {
	public static IScrapboxManager scrapboxDrops;
	public static IListRecipeManager recyclerBlacklist;
	public static IListRecipeManager recyclerWhitelist;
	public static IMachineRecipeManager compressor;
	public static IMachineRecipeManager extractor;
	public static IMachineRecipeManager macerator;
	public static IMachineRecipeManager centrifuge;
	public static IMachineRecipeManager metalformerExtruding;
	public static IMachineRecipeManager oreWashing;
	public static IMachineRecipeManager cannerBottle;
	public static IMachineRecipeManager metalformerCutting;
	public static IMachineRecipeManager metalformerRolling;
	public static IMachineRecipeManager matterAmplifier;
}

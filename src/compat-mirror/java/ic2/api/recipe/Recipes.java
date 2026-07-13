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

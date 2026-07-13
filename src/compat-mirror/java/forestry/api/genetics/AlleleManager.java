package forestry.api.genetics;

import net.minecraft.world.item.ItemStack;

/** F10 ЗЕРКАЛО (compile-only) чужого API — Forestry. Только статика, используемая
 *  GregTech6 (RecipeMapPlantalyzer/RecipeMapBumblelyzer: AlleleManager.alleleRegistry.getIndividual(aStack)).
 *  См. compat-mirror/README.md. */
public class AlleleManager {
	public static IAlleleRegistry alleleRegistry = new IAlleleRegistry();

	public static class IAlleleRegistry {
		public Object getIndividual(ItemStack aStack) {return null;}
	}
}

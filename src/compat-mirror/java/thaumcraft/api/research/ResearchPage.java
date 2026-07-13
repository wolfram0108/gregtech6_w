package thaumcraft.api.research;

import net.minecraft.world.item.crafting.Recipe;
import thaumcraft.api.crafting.CrucibleRecipe;
import thaumcraft.api.crafting.IArcaneRecipe;
import thaumcraft.api.crafting.InfusionEnchantmentRecipe;
import thaumcraft.api.crafting.InfusionRecipe;

/** F10 ЗЕРКАЛО (compile-only) чужого API Thaumcraft. Только объявления, используемые GregTech6
 *  (CompatTC.addResearch: набор конструкторов ResearchPage(...)). См. compat-mirror/README.md. */
public class ResearchPage {
	public ResearchPage(String aText) {}
	@SuppressWarnings("rawtypes")
	public ResearchPage(Recipe aRecipe) {}
	public ResearchPage(IArcaneRecipe aRecipe) {}
	public ResearchPage(CrucibleRecipe aRecipe) {}
	public ResearchPage(InfusionRecipe aRecipe) {}
	public ResearchPage(InfusionEnchantmentRecipe aRecipe) {}
}

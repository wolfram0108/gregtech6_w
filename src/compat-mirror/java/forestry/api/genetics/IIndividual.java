package forestry.api.genetics;

import net.minecraft.nbt.CompoundTag;

/** F10 ЗЕРКАЛО (compile-only) чужого API — Forestry. Методы, реально используемые
 *  GregTech6 (RecipeMapPlantalyzer/RecipeMapBumblelyzer: analyze()/writeToNBT(CompoundTag)).
 *  См. compat-mirror/README.md. */
public interface IIndividual {
	boolean analyze();
	void writeToNBT(CompoundTag aNBT);
}

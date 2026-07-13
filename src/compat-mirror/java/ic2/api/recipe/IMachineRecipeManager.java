package ic2.api.recipe;

import java.util.Map;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

/** F10 ЗЕРКАЛО (compile-only) чужого API. Сверено javap ic2:IC2Classic:1.2.1.8-dev
 *  (ic2.api.recipe.IMachineRecipeManager). Реально используются:
 *  addRecipe(IRecipeInput,NBTTagCompound,ItemStack...) — UT.java:3406/3420 (addSimpleIC2MachineRecipe);
 *  getRecipes():Map&lt;IRecipeInput,RecipeOutput&gt; — GT_API_Post.java:726+, GT_API_Proxy.java:326+,
 *  RM.java (removeSimpleIC2MachineRecipe принимает {@code Map} — сырой тип, совместимо).
 *  Метод getOutputFor реального API нигде не вызывается (греп 0) — не добавлен. */
public interface IMachineRecipeManager {
	void addRecipe(IRecipeInput aInput, CompoundTag aMetadata, ItemStack... aOutput);
	Map<IRecipeInput, RecipeOutput> getRecipes();
}

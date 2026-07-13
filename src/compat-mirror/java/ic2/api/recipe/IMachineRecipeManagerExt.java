package ic2.api.recipe;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

/** F10 ЗЕРКАЛО (compile-only) чужого API. Сверено javap ic2:IC2Classic:1.2.1.8-dev
 *  (ic2.api.recipe.IMachineRecipeManagerExt extends IMachineRecipeManager):
 *  addRecipe(IRecipeInput,NBTTagCompound,boolean,ItemStack...):boolean — UT.java:3398/3400
 *  (addSimpleIC2MachineRecipe, {@code instanceof IMachineRecipeManagerExt} ветка). */
public interface IMachineRecipeManagerExt extends IMachineRecipeManager {
	boolean addRecipe(IRecipeInput aInput, CompoundTag aMetadata, boolean aOverwrite, ItemStack... aOutput);
}

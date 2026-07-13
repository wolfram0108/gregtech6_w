package ic2.api.recipe;

import java.util.Arrays;
import java.util.List;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

/** F10 ЗЕРКАЛО (compile-only) чужого API. Было {@code interface RecipeOutput {}} — сломано:
 *  CompatIC2.java:157 делает {@code new RecipeOutput(aNBT, aStacks)} (нужен класс + ctor),
 *  GT_API_Proxy.java:326-335 читает {@code tRecipe.items} (List&lt;ItemStack&gt;) со значений getRecipes().
 *  Сверено javap ic2:IC2Classic:1.2.1.8-dev (ic2.api.recipe.RecipeOutput):
 *  {@code public final class RecipeOutput}, поля {@code items: List<ItemStack>, metadata: NBTTagCompound},
 *  ctor(NBTTagCompound, ItemStack...). Поле metadata не читается нигде в GT6-исходнике (греп 0) —
 *  оставлено ради ctor-параметра, наружу не используется. */
public final class RecipeOutput {
	public final List<ItemStack> items;
	public final CompoundTag metadata;

	public RecipeOutput(CompoundTag aMetadata, ItemStack... aItems) {
		this.metadata = aMetadata;
		this.items = aItems == null ? null : Arrays.asList(aItems);
	}
}

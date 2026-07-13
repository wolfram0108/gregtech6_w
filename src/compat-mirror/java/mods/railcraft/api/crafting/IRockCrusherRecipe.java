package mods.railcraft.api.crafting;

import net.minecraft.world.item.ItemStack;

/** F10 ЗЕРКАЛО (compile-only) чужого API — Railcraft. Реально используется — RM.java:1003,1004:
 *  addOutput(ItemStack,float):void (createNewRecipe(...) возвращает этот тип, RM.java:1002). */
public interface IRockCrusherRecipe {
	void addOutput(ItemStack aStack, float aChance);
}

package thaumcraft.common.lib.crafting;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import thaumcraft.common.tiles.TileArcaneWorkbench;

/** F10 ЗЕРКАЛО (compile-only) чужого API Thaumcraft. Только ContainerArcaneWorkbenchFixed
 *  (@Deprecated путь, не исполняется без реального TC). См. compat-mirror/README.md. */
public class ThaumcraftCraftingManager {
	public static Object findMatchingArcaneRecipeAspects(TileArcaneWorkbench aTile, Player aPlayer) {return null;}
	public static ItemStack findMatchingArcaneRecipe(TileArcaneWorkbench aTile, Player aPlayer) {return ItemStack.EMPTY;}
}

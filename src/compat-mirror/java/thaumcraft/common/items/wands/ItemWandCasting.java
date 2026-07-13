package thaumcraft.common.items.wands;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** F10 ЗЕРКАЛО (compile-only) чужого API Thaumcraft. Только используется в instanceof/
 *  ContainerArcaneWorkbenchFixed (@Deprecated путь). См. compat-mirror/README.md. */
public class ItemWandCasting extends Item {
	public ItemWandCasting() {super(new Item.Properties());}

	public boolean consumeAllVisCrafting(ItemStack aStack, Player aPlayer, Object aAspects, boolean aSimulate) {return false;}
}

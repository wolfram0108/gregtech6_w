package openmods.utils;

import net.minecraft.world.entity.player.Player;

/** F10 ЗЕРКАЛО (compile-only) чужого API — OpenModsLib. Только статика, используемая
 *  GregTech6 (CoverDrain: getPlayerXP/addPlayerXP). См. compat-mirror/README.md. */
public class EnchantmentUtils {
	public static int getPlayerXP(Player aPlayer) {return 0;}
	public static void addPlayerXP(Player aPlayer, int aXP) {}
}

package micdoodle8.mods.galacticraft.core.blocks;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/** F10 ЗЕРКАЛО (compile-only) чужого API — Galacticraft. Реально используются —
 *  ToolCompat.java:315: onSneakUseWrench, onUseWrench. */
public interface BlockAdvanced {
	boolean onSneakUseWrench(Level aWorld, int aX, int aY, int aZ, Player aEntityPlayer, byte aSide, float aHitX, float aHitY, float aHitZ);
	boolean onUseWrench(Level aWorld, int aX, int aY, int aZ, Player aEntityPlayer, byte aSide, float aHitX, float aHitY, float aHitZ);
}

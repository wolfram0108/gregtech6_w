package ic2.api.tile;

import net.minecraft.world.level.block.Block;

/** F10 ЗЕРКАЛО (compile-only) чужого API. Сверено javap ic2:IC2Classic:1.2.1.8-dev
 *  (ic2.api.tile.ExplosionWhitelist). Реально используются — CompatIC2.java:147,152:
 *  isBlockWhitelisted(Block):boolean, addWhitelistedBlock(Block):void.
 *  Метод removeWhitelistedBlock реального API не используется (греп 0) — не добавлен. */
public final class ExplosionWhitelist {
	public static boolean isBlockWhitelisted(Block aBlock) {return false;}
	public static void addWhitelistedBlock(Block aBlock) {/**/}
}

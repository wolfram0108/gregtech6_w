package mods.railcraft.common.carts;

import net.minecraft.world.level.block.Block;

/** F10 ЗЕРКАЛО (compile-only) чужого API — Railcraft. Реально используются два оверлоада
 *  (найдено компилятором сверх исходной спеки — греп {@code addMineableBlock} по всему
 *  gregtech6_w/src/main): addMineableBlock(Block):void — 12 вызовов (BlockBaseTree.java:43 и др.);
 *  addMineableBlock(Block,int):void (мета-специфичный) — BlockStones.java:206-208,
 *  Compat_Recipes_Railcraft.java:172,183. Остальная поверхность реального EntityTunnelBore
 *  (класс сущности-вагонетки) не используется (греп 0) — не добавлена. */
public final class EntityTunnelBore {
	public static void addMineableBlock(Block aBlock) {/**/}
	public static void addMineableBlock(Block aBlock, int aMeta) {/**/}
}

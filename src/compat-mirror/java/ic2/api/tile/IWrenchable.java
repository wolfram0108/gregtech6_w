package ic2.api.tile;

import net.minecraft.world.entity.player.Player;

/** F10 ЗЕРКАЛО (compile-only) чужого API. Сверено javap ic2:IC2Classic:1.2.1.8-dev
 *  (ic2.api.tile.IWrenchable, оригинал EntityPlayer 1.7.10 → neo Player, как во всём порту).
 *  Реально используются — WD.java:1237: getFacing, wrenchCanRemove, getWrenchDropRate.
 *  Методы wrenchCanSetFacing/setFacing/getWrenchDrop реального API не используются
 *  (греп 0) — не добавлены. */
public interface IWrenchable {
	short getFacing();
	boolean wrenchCanRemove(Player aPlayer);
	float getWrenchDropRate();
}

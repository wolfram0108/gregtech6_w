package com.rwtema.funkylocomotion.blocks;

import net.minecraft.core.Direction;

/** F10 ЗЕРКАЛО (compile-only) чужого API — FunkyLocomotion. Было {@code class TileBooster {}} —
 *  сломано той же причиной, что TilePusher (см. его комментарий). receiveEnergy — реально
 *  используется EnergyCompat.java:164. */
public interface TileBooster {
	int receiveEnergy(Direction aFrom, int aMaxReceive, boolean aSimulate);
}

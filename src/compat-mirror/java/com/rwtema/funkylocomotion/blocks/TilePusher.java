package com.rwtema.funkylocomotion.blocks;

import net.minecraft.core.Direction;

/** F10 ЗЕРКАЛО (compile-only) чужого API — FunkyLocomotion. Было {@code class TilePusher {}} —
 *  сломано: EnergyCompat.java:110,163 делает instanceof/cast против aTarget:BlockEntity, а класс
 *  не связан с BlockEntity ("incompatible types" — та же причина, что у appeng.tile.powersink.IC2,
 *  см. его комментарий); interface обходит эту проверку компилятора. receiveEnergy — реально
 *  используется EnergyCompat.java:163. */
public interface TilePusher {
	int receiveEnergy(Direction aFrom, int aMaxReceive, boolean aSimulate);
}

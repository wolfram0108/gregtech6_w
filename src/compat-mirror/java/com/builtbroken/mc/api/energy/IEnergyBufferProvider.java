package com.builtbroken.mc.api.energy;

import net.minecraft.core.Direction;

/** F10 ЗЕРКАЛО (compile-only) чужого API — Voltz/UniversalElectricity (builtbroken). Реально
 *  используется — EnergyCompat.java:116,203: getEnergyBuffer (результат присваивается в Object,
 *  код сразу кастует к IEnergyBuffer — сигнатура возврата взята из места вызова). */
public interface IEnergyBufferProvider {
	Object getEnergyBuffer(Direction aSide);
}

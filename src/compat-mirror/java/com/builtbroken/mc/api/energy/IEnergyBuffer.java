package com.builtbroken.mc.api.energy;

/** F10 ЗЕРКАЛО (compile-only) чужого API — Voltz/UniversalElectricity (builtbroken). Реально
 *  используется — EnergyCompat.java:206: addEnergyToStorage. */
public interface IEnergyBuffer {
	long addEnergyToStorage(long aEnergy, boolean aSimulate);
}

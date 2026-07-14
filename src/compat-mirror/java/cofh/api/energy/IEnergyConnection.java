package cofh.api.energy;

import net.minecraft.core.Direction;

/** F10 ЗЕРКАЛО (compile-only) чужого API — CoFH RF-API (энергия). Реально используется —
 *  EnergyCompat.java:125,212,257: canConnectEnergy. Методы getEnergyStored/getMaxEnergyStored
 *  реального API не используются в EnergyCompat/ToolCompat (греп 0) — не добавлены. */
public interface IEnergyConnection {
	boolean canConnectEnergy(Direction aFrom);
}

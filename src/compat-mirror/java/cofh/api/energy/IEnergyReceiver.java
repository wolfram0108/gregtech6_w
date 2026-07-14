package cofh.api.energy;

import net.minecraft.core.Direction;

/** F10 ЗЕРКАЛО (compile-only) чужого API — CoFH RF-API (энергия, "новая" версия — флаг
 *  EnergyCompat.RF_ENERGY_NEW: некоторые моды не включают этот файл). Реально используется —
 *  EnergyCompat.java:213,258: receiveEnergy. Методы extractEnergy/getEnergyStored/
 *  getMaxEnergyStored реального API не используются в EnergyCompat/ToolCompat (греп 0) —
 *  не добавлены. */
public interface IEnergyReceiver {
	int receiveEnergy(Direction aFrom, int aMaxReceive, boolean aSimulate);
}

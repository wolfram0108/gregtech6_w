package cofh.api.energy;

import net.minecraft.core.Direction;

/** F10 ЗЕРКАЛО (compile-only) чужого API — CoFH RF-API (энергия, "старая" версия — некоторые
 *  моды объявляют receiveEnergy прямо в IEnergyHandler, а не только в отдельном IEnergyReceiver,
 *  см. комментарий EnergyCompat.checkAvailabilities). Реально используется — EnergyCompat.java:
 *  125,214,259: receiveEnergy (через instanceof IEnergyHandler). Методы extractEnergy/
 *  getEnergyStored/getMaxEnergyStored реального API не используются (греп 0) — не добавлены. */
public interface IEnergyHandler {
	int receiveEnergy(Direction aFrom, int aMaxReceive, boolean aSimulate);
}

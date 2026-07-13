package ic2.api.tile;

import net.minecraft.core.Direction;

/** F10 ЗЕРКАЛО (compile-only) чужого API. Сверено javap ic2:IC2Classic:1.2.1.8-dev
 *  (ic2.api.tile.IEnergyStorage, оригинал ForgeDirection → neo Direction). Реально
 *  используются — WD.java:1254,1255: getStored, getCapacity, isTeleporterCompatible.
 *  Методы setStored/addEnergy/getOutput/getOutputEnergyUnitsPerTick реального API не
 *  используются (греп 0) — не добавлены. */
public interface IEnergyStorage {
	int getStored();
	int getCapacity();
	boolean isTeleporterCompatible(Direction aSide);
}

package micdoodle8.mods.galacticraft.api.power;

/** F10 ЗЕРКАЛО (compile-only) чужого API — Galacticraft. Реально используются —
 *  EnergyCompat.java:183-189: getEnergyStoredGC, getMaxEnergyStoredGC, receiveEnergyGC. */
public interface IEnergyHandlerGC {
	float getEnergyStoredGC(EnergySource aDir);
	float getMaxEnergyStoredGC(EnergySource aDir);
	float receiveEnergyGC(EnergySource aDir, float aReceive, boolean aSimulate);
}

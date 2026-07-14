package cr0s.warpdrive.block;

import net.minecraft.core.Direction;

/** F10 ЗЕРКАЛО (compile-only) чужого API — WarpDrive. Было {@code class TileEntityAbstractEnergy {}} —
 *  сломано той же причиной, что appeng.tile.powersink.IC2 (см. его комментарий) — interface
 *  обходит "incompatible types" при instanceof/cast против aTarget:BlockEntity. Реально
 *  используются — EnergyCompat.java:112,169,170,173: energy_canInput, energy_getEnergyStored,
 *  energy_getMaxStorage, energy_consume. */
public interface TileEntityAbstractEnergy {
	boolean energy_canInput(Direction aSide);
	long energy_getEnergyStored();
	long energy_getMaxStorage();
	void energy_consume(long aAmount);
}

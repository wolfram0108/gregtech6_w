package ic2.api.energy.tile;

/** F10 ЗЕРКАЛО (compile-only) чужого API. Сверено javap ic2:IC2Classic:1.2.1.8-dev
 *  (ic2.api.energy.tile.IEnergyConductor). Реально используется — WD.java:1250: getConductionLoss().
 *  Методы getInsulationEnergyAbsorption/getInsulationBreakdownEnergy/getConductorBreakdownEnergy/
 *  removeInsulation/removeConductor реального API не используются (греп 0) — не добавлены. */
public interface IEnergyConductor {
	double getConductionLoss();
}

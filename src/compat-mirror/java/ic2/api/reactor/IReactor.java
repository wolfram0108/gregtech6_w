package ic2.api.reactor;

/** F10 ЗЕРКАЛО (compile-only) чужого API. Сверено javap ic2:IC2Classic:1.2.1.8-dev
 *  (ic2.api.reactor.IReactor). Реально используются — WD.java:1232,1233:
 *  getHeat, getMaxHeat, getHeatEffectModifier, getReactorEUEnergyOutput.
 *  Огромная остальная поверхность реального IReactor (getPosition/getWorld/setHeat/addHeat/
 *  setMaxHeat/addEmitHeat/setHeatEffectModifier/getReactorEnergyOutput/addOutput/getItemAt/
 *  setItemAt/explode/getTickRate/produceEnergy/setRedstoneSignal/isFluidCooled) в GT6-исходнике
 *  не используется (греп 0) — не добавлена. */
public interface IReactor {
	int getHeat();
	int getMaxHeat();
	float getHeatEffectModifier();
	double getReactorEUEnergyOutput();
}

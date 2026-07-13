package ic2.api.energy.tile;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;

/** F10 ЗЕРКАЛО (compile-only) чужого API. Сверено javap ic2:IC2Classic:1.2.1.8-dev
 *  (ic2.api.energy.tile.IEnergySource extends IEnergyEmitter; emitsEnergyTo унаследован
 *  оттуда — здесь объявлен напрямую, как в IEnergySink). Реально используются —
 *  EnergyCompat.java:121, WD.java:1246: emitsEnergyTo, getSourceTier.
 *  Методы getOfferedEnergy/drawEnergy реального API не используются (греп 0) — не добавлены. */
public interface IEnergySource {
	boolean emitsEnergyTo(BlockEntity aReceiver, Direction aSide);
	int getSourceTier();
}

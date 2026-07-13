package ic2.api.energy.tile;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;

/** F10 ЗЕРКАЛО (compile-only) чужого API. Сверено javap ic2:IC2Classic:1.2.1.8-dev
 *  (ic2.api.energy.tile.IEnergySink extends IEnergyAcceptor; acceptsEnergyFrom унаследован
 *  оттуда — здесь объявлен напрямую, компилятору нужна только сигнатура на месте вызова, не
 *  иерархия). Оригинал (net.minecraft.tileentity.TileEntity, net.minecraftforge.common.util.
 *  ForgeDirection) → neo (BlockEntity, Direction), как везде в порту (FORGE_DIR = Direction[]).
 *  Реально используются — EnergyCompat.java:120,227,229,231, WD.java:1239-1242:
 *  acceptsEnergyFrom, getDemandedEnergy, injectEnergy, getSinkTier. */
public interface IEnergySink {
	boolean acceptsEnergyFrom(BlockEntity aEmitter, Direction aSide);
	double getDemandedEnergy();
	int getSinkTier();
	double injectEnergy(Direction aDirectionFrom, double aAmount, double aVoltage);
}

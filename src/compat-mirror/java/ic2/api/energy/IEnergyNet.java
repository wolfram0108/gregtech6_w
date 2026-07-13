package ic2.api.energy;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/** F10 ЗЕРКАЛО (compile-only) чужого API — НОВЫЙ файл (реальный тип поля EnergyNet.instance).
 *  Сверено javap ic2:IC2Classic:1.2.1.8-dev (ic2.api.energy.IEnergyNet). Реально используется —
 *  EnergyCompat.java:119,226: getTileEntity(World,int,int,int):TileEntity → neo Level/BlockEntity.
 *  Методы getNeighbor/getTotalEnergyEmitted/getTotalEnergySunken/getNodeStats/getPowerFromTier/
 *  getTierFromPower реального API не используются (греп 0) — не добавлены. */
public interface IEnergyNet {
	BlockEntity getTileEntity(Level aWorld, int aX, int aY, int aZ);
}

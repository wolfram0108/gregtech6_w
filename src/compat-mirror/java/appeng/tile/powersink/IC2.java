package appeng.tile.powersink;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;

/** F10 ЗЕРКАЛО (compile-only) чужого API — AppliedEnergistics2 (мост IC2-энергии AE2, тип
 *  так и называется {@code IC2} в реальном AE2 rv3-beta-6, jar недоступен для javap — build.gradle:218,
 *  не разрешён в gradle-кэше). Было {@code class IC2 {}} — сломано: EnergyCompat.java:108,151
 *  делает {@code aTarget instanceof appeng.tile.powersink.IC2} где aTarget:BlockEntity — класс,
 *  не связанный с BlockEntity, ловится компилятором как "incompatible types" (instanceof против
 *  двух несвязанных class-типов запрещён; interface обходит проверку — то же решение, что и у
 *  остальных {@code implements}-мостов F10, IMovableTile и т.п.). Интерфейс — точнее отражает
 *  реальный AE2-паттерн (TileEntity его implements). Методы acceptsEnergyFrom/getDemandedEnergy/
 *  injectEnergy — та же тройка, что у ic2.api.energy.tile.IEnergySink (AE2 реализует
 *  IC2-энергетический контракт), выведены из места вызова EnergyCompat.java:108,151,152,155. */
public interface IC2 {
	boolean acceptsEnergyFrom(BlockEntity aEmitter, Direction aSide);
	double getDemandedEnergy();
	double injectEnergy(Direction aDirectionFrom, double aAmount, double aVoltage);
}

/**
 * Copyright (c) 2026 wolfram0108
 *
 * COMPILE-TIME STAND-IN — NOT THIRD-PARTY CODE.
 *
 * This declaration was written from scratch for the GregTech 6 NeoForge port
 * (https://github.com/wolfram0108/gregtech6_w). It contains no code from the project
 * that owns this package name, and no part of it was copied or decompiled from that
 * project: it declares only the members GregTech 6 itself implements or calls, so that
 * the port compiles while integration with that mod stays deferred.
 *
 * The original package name is kept deliberately, because GregTech 6 implements these
 * types verbatim and the port does not alter the code Gregorius Techneticies wrote.
 * Removing these classes from the build is not possible: 66 classes of the mod extend
 * or implement them, and the JVM requires the type to load the implementing class.
 *
 * All names, trademarks and rights in the project this package belongs to remain with
 * its authors. See src/compat-mirror/README.md and NOTICE.
 *
 * This file is part of GregTech.
 *
 * GregTech is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * GregTech is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with GregTech. If not, see <http://www.gnu.org/licenses/>.
 */

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

/**
 * Copyright (c) 2022 GregTech-6 Team
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
 *
 * Modified in 2026 for the GregTech 6 NeoForge port
 * (https://github.com/wolfram0108/gregtech6_w): ported from Minecraft 1.7.10 / Forge
 * to Minecraft 26.1.2 / NeoForge.
 */

package gregapi.log;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.spi.AbstractLogger;

/**
 * F2-coremod (отложенная фаза): GT6 подсовывает «пустой» Logger в ASM-трансформеры (gregtech.asm.transformers.
 * MicroBlock_FixLoggerCrash / MultiPart_FixLoggerCrash — {@code Logger FAKE_LOGGER = new LoggerFML(...)}), чтобы
 * подавить чужие лог-краши. Реализация — тотальный no-op: {@code isEnabled(...)} везде false, значит ни один
 * лог не пишется (1:1 с прежним «пустым логгером»).
 *
 * Прежняя версия реализовывала весь интерфейс {@code org.apache.logging.log4j.Logger} вручную (~150 no-op
 * методов). Log4j 2.25.2 (текущий на neo-classpath) расширил интерфейс десятками overload'ов (warn/error/…
 * ×0-10 Object, Supplier/MessageSupplier-варианты) — ручной список хрупок к каждому version-bump и не собирался.
 * Переведено на {@code extends AbstractLogger}: базовый класс даёт ВСЕ convenience-методы, оставляя абстрактными
 * лишь семейство {@code isEnabled(...)} + ядро {@code logMessage(...)} (из ExtendedLogger) + {@code getLevel()}.
 * Их и реализуем no-op — устойчиво к будущим версиям Log4j.
 */
public class LoggerFML extends AbstractLogger {
	private static final long serialVersionUID = 1L;
	public String mName = "";

	public LoggerFML(Object aName) {
		super(aName == null ? "" : aName.toString());
		if (aName != null) mName = aName.toString();
	}

	@Override public Level getLevel() {return Level.OFF;}

	@Override public void logMessage(String aFQCN, Level aLevel, Marker aMarker, Message aMsg, Throwable aThrowable) {/* no-op logger */}

	@Override public boolean isEnabled(Level aLevel, Marker aMarker, Message aMsg, Throwable aThrowable) {return false;}
	@Override public boolean isEnabled(Level aLevel, Marker aMarker, CharSequence aMsg, Throwable aThrowable) {return false;}
	@Override public boolean isEnabled(Level aLevel, Marker aMarker, Object aMsg, Throwable aThrowable) {return false;}
	@Override public boolean isEnabled(Level aLevel, Marker aMarker, String aMsg, Throwable aThrowable) {return false;}
	@Override public boolean isEnabled(Level aLevel, Marker aMarker, String aMsg) {return false;}
	@Override public boolean isEnabled(Level aLevel, Marker aMarker, String aMsg, Object... aParams) {return false;}
	@Override public boolean isEnabled(Level aLevel, Marker aMarker, String aMsg, Object p0) {return false;}
	@Override public boolean isEnabled(Level aLevel, Marker aMarker, String aMsg, Object p0, Object p1) {return false;}
	@Override public boolean isEnabled(Level aLevel, Marker aMarker, String aMsg, Object p0, Object p1, Object p2) {return false;}
	@Override public boolean isEnabled(Level aLevel, Marker aMarker, String aMsg, Object p0, Object p1, Object p2, Object p3) {return false;}
	@Override public boolean isEnabled(Level aLevel, Marker aMarker, String aMsg, Object p0, Object p1, Object p2, Object p3, Object p4) {return false;}
	@Override public boolean isEnabled(Level aLevel, Marker aMarker, String aMsg, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5) {return false;}
	@Override public boolean isEnabled(Level aLevel, Marker aMarker, String aMsg, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6) {return false;}
	@Override public boolean isEnabled(Level aLevel, Marker aMarker, String aMsg, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6, Object p7) {return false;}
	@Override public boolean isEnabled(Level aLevel, Marker aMarker, String aMsg, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6, Object p7, Object p8) {return false;}
	@Override public boolean isEnabled(Level aLevel, Marker aMarker, String aMsg, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6, Object p7, Object p8, Object p9) {return false;}
}
